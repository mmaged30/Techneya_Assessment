package org.models.excel;

import lombok.Data;
import org.utils.CustomAnnotations.ExcelColumn;


@Data
public class PostalCodeLookupData {

    @ExcelColumn("country")
    private String country;

    @ExcelColumn("postalCode")
    private String postalCode;

    @ExcelColumn("countryName")
    private String countryName;

    @ExcelColumn("abbreviation")
    private String abbreviation;

    @ExcelColumn("place")
    private String place;

    @ExcelColumn("state")
    private String state;

    @ExcelColumn("stateCode")
    private String stateCode;
}
