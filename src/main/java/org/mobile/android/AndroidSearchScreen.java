package org.mobile.android;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.mobile.interfaces.ArticleScreen;
import org.mobile.interfaces.SearchScreen;
import org.openqa.selenium.By;

/** Page object for the Search card and activity.*/
@Slf4j
public class AndroidSearchScreen extends AndroidScreenBase implements SearchScreen {

    private static final By searchCard = id("search_card");
    private static final By searchInput = id("search_src_text");
    private static final By resultsContainer = id("fragment_search_results");

    /**
     * Clears search-widget ad overlays dynamically within waits
     * to handle delayed modal arrivals.
     */
    AndroidSearchScreen awaitLoaded() {
        awaitPastPromos(searchCard, "Search card");
        return this;
    }

    @Override
    @Step("Search for '{0}'")
    public AndroidSearchScreen searchFor(String term) {
        tap(searchCard, "Search card");
        type(searchInput, term, "Search field");
        return this;
    }

    @Override
    @Step("Check whether the results include '{0}'")
    public boolean resultsInclude(String articleTitle) {
        wait.visible(resultsContainer);
        return wait.appears(resultAt(articleTitle));
    }

    @Override
    @Step("Open the '{0}' article from the results")
    public ArticleScreen openArticle(String articleTitle) {
        tap(resultAt(articleTitle), "search result '" + articleTitle + "'");
        return new AndroidArticleScreen().awaitLoaded();
    }

    /** Matches result rows by exact title (scoped to {@code TextView}
     *  to avoid search field false-positives).
     */
    private static By resultAt(String articleTitle) {
        return uiSelector(
                "new UiSelector().className(\"android.widget.TextView\").text(\"" + articleTitle + "\")");
    }
}
