package tests.mobile;

import data.TestData;
import io.qameta.allure.Description;
import java.util.Map;
import org.mobile.interfaces.SearchScreen;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Article search on the Wikipedia app. */
public class SearchTest extends BaseMobileTest {

    @Test(dataProvider = "searchTerms", dataProviderClass = TestData.class,
            groups = {"mobile", "search"})
    @Description("Searching surfaces the expected article")
    public void searchingSurfacesTheExpectedArticle(Map<String, String> row) {
        SearchScreen search = launchApp()
                .openSearch()
                .searchFor(row.get("term"));

        Assert.assertTrue(search.resultsInclude(row.get("article")),
                "Searching did not return an article titled '" + row.get("article") + "'.");
    }
}
