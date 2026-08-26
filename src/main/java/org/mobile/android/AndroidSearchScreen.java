package org.mobile.android;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.mobile.interfaces.ArticleScreen;
import org.mobile.interfaces.SearchScreen;
import org.openqa.selenium.By;

/**
 * Search: a card on the Search tab that opens a dedicated search activity.
 * <p>
 * The results list is rendered with Jetpack Compose and carries no resource ids at all, so its
 * rows can only be addressed by their text. The locator is scoped by class so a query that
 * happens to equal a result's title cannot match the input field instead.
 */
@Slf4j
public class AndroidSearchScreen extends AndroidScreenBase implements SearchScreen {

    private static final By searchCard = id("search_card");
    private static final By searchInput = id("search_src_text");
    private static final By resultsContainer = id("fragment_search_results");

    /**
     * The search-widget advert is modal: while it is showing, the search card is not merely
     * covered but absent from the view hierarchy. It also does not always arrive before the tab
     * beneath it has rendered, so the advert is cleared from inside the wait rather than once
     * before it.
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

    /**
     * Waits for the result rather than asking whether it is there yet: results arrive over the
     * network, so an immediate check would report an empty list as "no such article".
     */
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

    /**
     * Matches a result row by its exact title.
     * <p>
     * The class is part of the match, not decoration: the search field holds the typed term and
     * would otherwise match a row whose title is spelled the same way - and being higher up the
     * tree, it would win. It is an {@code AutoCompleteTextView}, so pinning the class to
     * {@code TextView} excludes it.
     * <p>
     * The matched node is the title itself, which Compose renders as non-clickable. Tapping it
     * still works, because the tap lands at its centre - inside the clickable row that wraps it.
     */
    private static By resultAt(String articleTitle) {
        return uiSelector(
                "new UiSelector().className(\"android.widget.TextView\").text(\"" + articleTitle + "\")");
    }
}
