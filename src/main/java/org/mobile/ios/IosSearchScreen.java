package org.mobile.ios;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.mobile.interfaces.ArticleScreen;
import org.mobile.interfaces.SearchScreen;
import org.openqa.selenium.By;

/**
 * Reference implementation: what an iOS screen in this framework looks like.
 * <p>
 * <strong>UNVERIFIED, AND NOT YET REACHABLE AT RUNTIME.</strong> This class exists as the
 * worked template for whoever completes iOS on a macOS machine. Two things are true about it:
 * <ul>
 *     <li>the <em>structure</em> is real - it extends {@link IosScreenBase}, implements the same
 *         {@link SearchScreen} interface the Android screen does, returns interfaces rather than
 *         concrete types, and keeps its locators private and static, exactly as its Android
 *         counterpart does;</li>
 *     <li>the <em>identifiers</em> below are placeholders. They were not read from a running
 *         app, because no such app could be run here. Every one of them must be replaced by
 *         inspecting the real iOS build with Appium Inspector.</li>
 * </ul>
 * Nothing constructs this class yet: there is no {@code IosMainScreen} to navigate from. That
 * is deliberate - resolving half a flow would fail deep inside a scenario instead of at the
 * boundary, with a message about a missing element rather than a missing implementation.
 */
@Slf4j
public class IosSearchScreen extends IosScreenBase implements SearchScreen {

    // PLACEHOLDERS - replace with identifiers read from the real iOS build.
    private static final By searchField = identifier("search_field");
    private static final By resultsTable = identifier("search_results_table");

    IosSearchScreen awaitLoaded() {
        awaitPastPromos(searchField, "search field");
        return this;
    }

    @Override
    @Step("Search for '{0}'")
    public IosSearchScreen searchFor(String term) {
        tap(searchField, "Search field");
        type(searchField, term, "Search field");
        return this;
    }

    @Override
    @Step("Check whether the results include '{0}'")
    public boolean resultsInclude(String articleTitle) {
        wait.visible(resultsTable);
        return wait.appears(resultAt(articleTitle));
    }

    @Override
    @Step("Open the '{0}' article from the results")
    public ArticleScreen openArticle(String articleTitle) {
        tap(resultAt(articleTitle), "search result '" + articleTitle + "'");
        throw new UnsupportedOperationException(
                "IosArticleScreen has not been written yet. See Screens.onboarding() for what iOS still needs.");
    }

    /**
     * The iOS counterpart of matching a Compose row by its text: a predicate on the cell's
     * label. Scoped by element type for the same reason the Android locator pins a class - so
     * the search field, which also holds the typed term, cannot win the match.
     */
    private static By resultAt(String articleTitle) {
        return staticTextWithLabel(articleTitle);
    }
}
