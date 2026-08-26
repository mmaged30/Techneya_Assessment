package org.mobile.ios;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.mobile.interfaces.ArticleScreen;
import org.mobile.interfaces.SearchScreen;
import org.openqa.selenium.By;

/**
 * Reference implementation and template for iOS screen objects, extending {@link IosScreenBase}.
 * Defines architecture and implements {@link SearchScreen}, but contains placeholder locators
 * and is withheld from runtime instantiation pending live iOS inspection.
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
     * Matches result rows by text label using element-type predicates on iOS.
     * Scopes locators by cell type to prevent false-positive matches against the search input field.
     */
    private static By resultAt(String articleTitle) {
        return staticTextWithLabel(articleTitle);
    }
}
