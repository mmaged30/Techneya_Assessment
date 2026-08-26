package tests.mobile;

import io.qameta.allure.Description;
import org.models.excel.SearchTermData;
import org.mobile.interfaces.SearchScreen;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.utils.CustomAnnotations.ExcelDataSource;
import org.utils.DataProviderSource;
import tests.TestResources;

/** Article search on the Wikipedia app. */
public class SearchTest extends BaseMobileTest {

    @ExcelDataSource(workbook = TestResources.MOBILE_WORKBOOK,
            sheetName = TestResources.SEARCH_TERMS_SHEET,
            pojoClass = SearchTermData.class)
    @Test(dataProvider = "ExcelFeed", dataProviderClass = DataProviderSource.class, groups = {"mobile"})
    @Description("Searching surfaces the expected article")
    public void searchingSurfacesTheExpectedArticle(SearchTermData data) {
        SearchScreen search = launchApp()
                .openSearch()
                .searchFor(data.getTerm());

        Assert.assertTrue(search.resultsInclude(data.getArticle()),
                "Searching did not return an article titled '" + data.getArticle() + "'.");
    }
}
