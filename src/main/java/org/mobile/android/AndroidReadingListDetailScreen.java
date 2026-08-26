package org.mobile.android;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.mobile.interfaces.ReadingListDetailScreen;
import org.openqa.selenium.By;

/**
 * One reading list and the articles in it.
 * <p>
 * Article rows are the only nodes carrying {@code page_list_item_title}, so counting them is
 * how this screen answers both "is the article here" and "is it here exactly once".
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
     * Opening a list is where the app shows its share tooltip, and that tooltip is its own
     * window: while it is up the list beneath it is not in the hierarchy at all. Waiting past
     * promotions rather than waiting and then clearing them is what handles that.
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
     * The remove option sits below the fold of the action sheet, and the rows underneath it are
     * not merely off-screen but absent from the hierarchy until the sheet is expanded. So the
     * sheet is dragged up once, and only when the option is not already reachable.
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
