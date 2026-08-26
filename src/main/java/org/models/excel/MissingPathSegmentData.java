package org.models.excel;

import lombok.Data;
import org.utils.CustomAnnotations.ExcelColumn;


@Data
public class MissingPathSegmentData {

    @ExcelColumn("path")
    private String path;

    @ExcelColumn("expectedStatus")
    private int expectedStatus;
}
