package org.mobile.android;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.mobile.interfaces.ArticleScreen;
import org.mobile.interfaces.SaveToReadingListSheet;
import org.openqa.selenium.By;

/**
 * Page element for an article's save control.
 * Tapping saves immediately if unsaved, but opens the list-chooser menu if already saved.
 * {@link #addToReadingList()} relies on this secondary menu state.
 */
@Slf4j
public class AndroidArticleScreen extends AndroidScreenBase implements ArticleScreen {

    private static final By actionBar = id("page_actions_tab_layout");
    private static final By saveButton = id("page_save");
    private static final By addToAnotherList = menuItem("Add to another reading list");
    private static final By snackbarText = id("snackbar_text");

    AndroidArticleScreen awaitLoaded() {
        awaitPastPromos(actionBar, "article action bar");
        return this;
    }

    @Override
    @Step("Save the article")
    public AndroidArticleScreen save() {
        tap(saveButton, "Save button");
        return this;
    }

    /**
     * Opens the list chooser for an already-saved article.
     * Returns the sheet so the caller decides between creating a list and picking one.
     */
    @Override
    @Step("Open the reading list chooser")
    public SaveToReadingListSheet addToReadingList() {
        tap(saveButton, "Save button");
        tap(addToAnotherList, "Add to another reading list");
        return new AndroidSaveToReadingListSheet().awaitLoaded();
    }

    @Override
    @Step("Read the message shown after saving")
    public String saveMessage() {
        return findAll(snackbarText).stream()
                .findFirst()
                .map(element -> element.getText().trim())
                .orElse("");
    }

    /** A row of the save menu, which the app renders as a generic titled list item. */
    private static By menuItem(String label) {
        return idWithText("title", label);
    }
}
