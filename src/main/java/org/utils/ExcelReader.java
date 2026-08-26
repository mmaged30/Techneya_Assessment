package org.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.utils.CustomAnnotations.ExcelColumn;

/**
 * Maps Excel sheet rows to typed objects using {@link ExcelColumn}.
 * Columns are matched by header name to prevent issues when columns are reordered.
 * Cells are read as text and converted to the declared field type.
 */
@Slf4j
public final class ExcelReader {

    private ExcelReader() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * @param rowType   the type each row is mapped onto; needs a no-argument constructor
     * @param resource  classpath-relative workbook, e.g. {@code data/api-data.xlsx}
     * @param sheetName the sheet to read; its first row is the header
     * @return one instance of {@code rowType} per data row, in sheet order
     */
    public static List<Object> rows(Class<?> rowType, String resource, String sheetName) {
        // Created per read to avoid sharing DataFormatter's mutable state across threads.
        DataFormatter formatter = new DataFormatter();

        try (InputStream stream = open(resource);
             Workbook workbook = new XSSFWorkbook(stream)) {

            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalStateException("❌ CRITICAL: workbook '" + resource
                        + "' has no sheet named '" + sheetName + "'. Sheets present: "
                        + sheetNames(workbook));
            }

            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                throw new IllegalStateException("❌ CRITICAL: sheet '" + sheetName + "' in '"
                        + resource + "' is empty - the first row must be a header.");
            }

            Map<String, Integer> columns = headerIndex(header, formatter);
            List<Field> fields = boundFieldsOf(rowType, columns, sheetName, resource);

            List<Object> rows = new ArrayList<>();
            for (int i = header.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isBlank(row, columns.size(), formatter)) {
                    // A trailing blank row is how a spreadsheet ends, not a data row.
                    continue;
                }
                rows.add(toRowObject(rowType, fields, columns, row, formatter));
            }

            if (rows.isEmpty()) {
                throw new IllegalStateException("❌ CRITICAL: sheet '" + sheetName + "' in '"
                        + resource + "' has a header but no data rows.");
            }

            log.info("Loaded {} row(s) from [{}!{}] as {}",
                    rows.size(), resource, sheetName, rowType.getSimpleName());
            return rows;

        } catch (IOException e) {
            throw new IllegalStateException("❌ CRITICAL: could not read workbook: " + resource, e);
        }
    }

    /** Header name to column index. Linked so a failure message lists them in sheet order. */
    private static Map<String, Integer> headerIndex(Row header, DataFormatter formatter) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            String name = text(header.getCell(c), formatter);
            if (!name.isEmpty()) {
                columns.put(name, c);
            }
        }
        return columns;
    }

    /**
     * Annotated fields validated against the sheet headers before processing rows.
     * Fails fast on missing or misspelled column names.
     */
    private static List<Field> boundFieldsOf(Class<?> rowType, Map<String, Integer> columns,
                                             String sheetName, String resource) {
        List<Field> fields = Arrays.stream(rowType.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ExcelColumn.class))
                .toList();

        if (fields.isEmpty()) {
            throw new IllegalStateException("❌ CRITICAL: " + rowType.getSimpleName()
                    + " has no @ExcelColumn fields, so no row could be mapped onto it.");
        }

        List<String> missing = fields.stream()
                .map(field -> field.getAnnotation(ExcelColumn.class).value())
                .filter(name -> !columns.containsKey(name))
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalStateException("❌ CRITICAL: " + rowType.getSimpleName()
                    + " expects column(s) " + missing + " but sheet '" + sheetName + "' in '"
                    + resource + "' has " + columns.keySet());
        }
        return fields;
    }

    /** A fresh instance per row, so two rows can never overwrite one object. */
    private static Object toRowObject(Class<?> rowType, List<Field> fields,
                                      Map<String, Integer> columns, Row row,
                                      DataFormatter formatter) {
        try {
            Object instance = rowType.getDeclaredConstructor().newInstance();
            for (Field field : fields) {
                int columnIndex = columns.get(field.getAnnotation(ExcelColumn.class).value());
                field.setAccessible(true);
                field.set(instance, valueAs(field, text(row.getCell(columnIndex), formatter)));
            }
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("❌ CRITICAL: could not map row " + (row.getRowNum() + 1)
                    + " onto " + rowType.getSimpleName()
                    + ". The class needs a no-argument constructor.", e);
        }
    }

    /**
     * Converts cell text to the declared field type.
     * Supports String, int, and boolean; unsupported types fail explicitly.
     */
    private static Object valueAs(Field field, String cellValue) {
        Class<?> type = field.getType();

        if (type == String.class) {
            return cellValue;
        }
        if (type == int.class || type == Integer.class) {
            return parseInt(field, cellValue);
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(cellValue);
        }
        throw new IllegalStateException("❌ CRITICAL: @ExcelColumn supports String, int and boolean"
                + " fields only, but '" + field.getName() + "' is a " + type.getSimpleName());
    }

    private static int parseInt(Field field, String cellValue) {
        if (cellValue.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(cellValue);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("❌ CRITICAL: column '"
                    + field.getAnnotation(ExcelColumn.class).value() + "' feeds '"
                    + field.getName() + "', which is a whole number, but the cell held '"
                    + cellValue + "'", e);
        }
    }

    private static boolean isBlank(Row row, int width, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int c = 0; c < width; c++) {
            if (!text(row.getCell(c), formatter).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Never null, always trimmed - an absent cell and an empty one mean the same thing here. */
    private static String text(Cell cell, DataFormatter formatter) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private static List<String> sheetNames(Workbook workbook) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            names.add(workbook.getSheetName(i));
        }
        return names;
    }

    private static InputStream open(String resource) {
        InputStream stream = ExcelReader.class.getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("❌ CRITICAL: workbook '" + resource
                    + "' was not found on the classpath. It belongs under src/test/resources/.");
        }
        return stream;
    }
}
