package org.mobile.android;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.mobile.interfaces.ReadingListDetailScreen;
import org.openqa.selenium.By;

/**
 * Represents a reading list and its contained articles.
 * Uses {@code page_list_item_title} nodes to count article rows and verify presence and uniqueness.
 */
@Slf4j
public class AndroidReadingListDetailScreen extends AndroidScreenBase implements ReadingListDetailScreen {

    private static final By listRecycler = id("reading_list_recycler_view");

    /** Shown in place of the header artwork once the last article is removed. */
    private static final By emptyStateImage = id("reading_list_header_empty_image");

    // The sheet a long press on an article row opens
    private static final By removeRow = id("reading_list_item_remove");
    private static final By sheetTitle = id("reading_list_item_title");

    /**
     * Handles the share tooltip window that covers the reading list.
     * Waits out overlay animations directly so the underlying list re-enters
     * the view hierarchy before interaction.
     */
    AndroidReadingListDetailScreen awaitLoaded() {
        awaitPastPromos(listRecycler, "reading list contents");
        return this;
    }

    @Override
    @Step("Check whether the list contains '{0}'")
    public boolean contains(String articleTitle) {
        return wait.appears(articleNamed(articleTitle));
    }

    @Override
    @Step("Check that '{1}' appears exactly {0} time(s)")
    public boolean containsExactly(int expectedCopies, String articleTitle) {
        return wait.countSettlesAt(articleNamed(articleTitle), expectedCopies);
    }

    @Override
    @Step("Remove '{0}' from the list")
    public AndroidReadingListDetailScreen removeArticle(String articleTitle) {
        longPress(articleNamed(articleTitle), "article '" + articleTitle + "'");
        wait.visible(sheetTitle);
        revealRemoveOption();
        tap(removeRow, "Remove from this list");
        wait.invisible(sheetTitle);
        return this;
    }

    @Override
    @Step("Wait until '{0}' is gone from the list")
    public boolean awaitAbsenceOf(String articleTitle) {
        return wait.countSettlesAt(articleNamed(articleTitle), 0);
    }

    @Override
    public boolean isEmpty() {
        return isDisplayed(emptyStateImage);
    }

    /**
     * Ensures the "Remove" action sheet option is visible before selection.
     * Expands the sheet via drag if the option is not yet rendered in the view hierarchy.
     */
    private void revealRemoveOption() {
        if (isDisplayed(removeRow)) {
            return;
        }
        swipeUp("the article action sheet");
        wait.visible(removeRow);
    }

    private static By articleNamed(String articleTitle) {
        return idWithText("page_list_item_title", articleTitle);
    }
}
