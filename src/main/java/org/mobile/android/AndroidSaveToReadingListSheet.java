package org.mobile.android;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.mobile.interfaces.ArticleScreen;
import org.mobile.interfaces.SaveToReadingListSheet;
import org.openqa.selenium.By;

/**
 * The "Save to reading list" bottom sheet, and the create-list dialog it leads to.
 * <p>
 * Note that the sheet offers every list, including ones that already contain the article. The
 * app does not filter them out; it detects the duplicate after the choice is made.
 */
@Slf4j
public class AndroidSaveToReadingListSheet extends AndroidScreenBase implements SaveToReadingListSheet {

    private static final By sheetTitle = id("dialog_title");
    private static final By createNewButton = id("create_button");

    // The create-list dialog
    private static final By listNameInput = id("text_input");
    private static final By confirmButton = androidId("button1");

    AndroidSaveToReadingListSheet awaitLoaded() {
        wait.visible(sheetTitle);
        return this;
    }

    @Override
    @Step("Create a new reading list called '{0}'")
    public ArticleScreen createList(String listName) {
        tap(createNewButton, "Create new");
        type(listNameInput, listName, "List name field");
        tap(confirmButton, "OK");
        wait.invisible(listNameInput);
        log.info("Created the reading list [{}]", listName);
        return new AndroidArticleScreen();
    }

    @Override
    @Step("Choose the existing reading list '{0}'")
    public ArticleScreen chooseList(String listName) {
        tap(listNamed(listName), "reading list '" + listName + "'");
        wait.invisible(sheetTitle);
        return new AndroidArticleScreen();
    }

    private static By listNamed(String listName) {
        return idWithText("item_title", listName);
    }
}
