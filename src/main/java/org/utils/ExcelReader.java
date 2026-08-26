package org.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Reads a sheet of an {@code .xlsx} workbook into rows a TestNG {@code @DataProvider} can feed
 * straight to a test.
 * <p>
 * Rows are keyed by the header cell above them rather than by position, so a test reads
 * {@code row.get("postalCode")} and stays correct when someone reorders or inserts a column.
 * <p>
 * Every value is read as text through {@link DataFormatter}. Postal codes are the reason: Excel
 * stores {@code 01067} as the number 1067 and would drop the leading zero, and a cell typed as
 * numeric would otherwise arrive as {@code 90210.0}. The API takes strings, so text is both
 * safer and closer to what is actually sent.
 */
public final class ExcelReader {

    private ExcelReader() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * @param resource  classpath-relative workbook, e.g. {@code data/api-data.xlsx}
     * @param sheetName the sheet to read; its first row is treated as the header
     * @return one map per data row, in sheet order
     */
    public static List<Map<String, String>> rows(String resource, String sheetName) {
        // Created per read rather than shared in a static field: DataFormatter caches parsed
        // formats in a plain HashMap and carries mutable locale state, so one instance shared
        // between concurrently-invoked data providers would be a race. Constructing one per
        // sheet costs nothing next to opening the workbook.
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

            List<String> columns = readHeader(header, formatter);
            List<Map<String, String>> rows = new ArrayList<>();

            for (int i = header.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (isBlank(row, columns.size(), formatter)) {
                    // A trailing blank row is how a spreadsheet ends, not a data row.
                    continue;
                }
                rows.add(readRow(row, columns, formatter));
            }

            if (rows.isEmpty()) {
                throw new IllegalStateException("❌ CRITICAL: sheet '" + sheetName + "' in '"
                        + resource + "' has a header but no data rows.");
            }
            return rows;

        } catch (IOException e) {
            throw new IllegalStateException("❌ CRITICAL: could not read workbook: " + resource, e);
        }
    }

    /**
     * The same rows shaped for a {@code @DataProvider}: one map per test invocation.
     * <p>
     * Each row is a single argument rather than one argument per column, so a seven-column data
     * set does not become a seven-parameter test method whose call site nobody can read.
     */
    public static Object[][] dataProvider(String resource, String sheetName) {
        return rows(resource, sheetName).stream()
                .map(row -> new Object[]{row})
                .toArray(Object[][]::new);
    }

    private static List<String> readHeader(Row header, DataFormatter formatter) {
        List<String> columns = new ArrayList<>();
        for (int c = 0; c < header.getLastCellNum(); c++) {
            columns.add(text(header.getCell(c), formatter));
        }
        return columns;
    }

    private static Map<String, String> readRow(Row row, List<String> columns,
                                               DataFormatter formatter) {
        // Linked so the map iterates in column order, which makes a failure message readable.
        Map<String, String> values = new LinkedHashMap<>();
        for (int c = 0; c < columns.size(); c++) {
            String column = columns.get(c);
            if (!column.isEmpty()) {
                values.put(column, text(row.getCell(c), formatter));
            }
        }
        return values;
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
