package org.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for the data-driven annotations. Import the nested types directly so usages read as
 * {@code @ExcelColumn("postalCode")}.
 */
public final class CustomAnnotations {

    private CustomAnnotations() {
        throw new IllegalStateException("Annotation container");
    }

    /**
     * Binds a field to an Excel column <em>by its header name</em>.
     * <p>
     * By name rather than by index on purpose. A positional binding silently re-points every
     * field after any column someone inserts in the spreadsheet - {@code title} starts reading
     * what {@code description} held - and that failure surfaces as a baffling assertion three
     * lines away from its cause. A header name survives reordering and insertion, and a name
     * that no longer exists fails immediately with the list of headers that do.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ExcelColumn {
        String value();
    }

    /**
     * Declares where a test's rows come from: which workbook, which sheet, and the type each row
     * is mapped onto.
     * <p>
     * Sitting on the test method itself is the point - the data source is visible where the test
     * is read, and adding a data-driven test never means writing another {@code @DataProvider}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ExcelDataSource {

        /** Classpath path to the workbook, e.g. {@code data/api-data.xlsx}. */
        String workbook();

        String sheetName();

        /** The type each row is mapped onto. Needs a no-argument constructor. */
        Class<?> pojoClass();
    }
}
