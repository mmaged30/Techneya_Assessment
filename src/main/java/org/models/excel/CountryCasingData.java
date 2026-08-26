package org.models.excel;

import lombok.Data;
import org.utils.CustomAnnotations.ExcelColumn;


@Data
public class CountryCasingData {

    @ExcelColumn("country")
    private String country;

    @ExcelColumn("postalCode")
    private String postalCode;

    @ExcelColumn("expectedCountryName")
    private String expectedCountryName;

    @ExcelColumn("expectedAbbreviation")
    private String expectedAbbreviation;
}
