package org.utils;

import java.lang.reflect.Method;
import java.util.Iterator;
import org.testng.annotations.DataProvider;
import org.utils.CustomAnnotations.ExcelDataSource;

/**
 * Shared data provider for the test suite.
 * Resolves the workbook, sheet, and row type from {@code @ExcelDataSource}
 * and provides the corresponding test data.
 */
public class DataProviderSource {

    @DataProvider(name = "ExcelFeed")
    public static Iterator<Object> excelFeed(Method method) {
        ExcelDataSource source = method.getAnnotation(ExcelDataSource.class);
        if (source == null) {
            throw new IllegalStateException("❌ CRITICAL: " + method.getName()
                    + " uses the ExcelFeed provider but is missing @ExcelDataSource.");
        }

        return ExcelReader.rows(source.pojoClass(), source.workbook(), source.sheetName())
                .iterator();
    }
}
