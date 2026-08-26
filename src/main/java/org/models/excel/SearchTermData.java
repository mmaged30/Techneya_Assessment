package org.models.excel;

import lombok.Data;
import org.utils.CustomAnnotations.ExcelColumn;


@Data
public class SearchTermData {

    @ExcelColumn("term")
    private String term;

    @ExcelColumn("article")
    private String article;
}
