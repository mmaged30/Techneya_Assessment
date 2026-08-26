package org.models.excel;

import lombok.Data;
import org.utils.CustomAnnotations.ExcelColumn;


@Data
public class RejectedLookupData {

    @ExcelColumn("reason")
    private String reason;

    @ExcelColumn("country")
    private String country;

    @ExcelColumn("postalCode")
    private String postalCode;
}
