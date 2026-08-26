package org.mobile.android;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.mobile.interfaces.ArticleScreen;
import org.mobile.interfaces.SaveToReadingListSheet;
import org.openqa.selenium.By;

/**
 * An article, with the save control in the bottom action bar.
 * <p>
 * The save button behaves differently depending on what is already saved, and the difference
 * matters to the tests:
 * <ul>
 *     <li>on an unsaved article, tapping it saves straight into the default "Saved" list;</li>
 *     <li>on an article that is already saved, tapping it opens a menu instead of un-saving.</li>
 * </ul>
 * Both were confirmed against the app rather than assumed, and {@link #addToReadingList()}
 * relies on the second: it is the only route to the list chooser.
 */
@Slf4j
public class AndroidArticleScreen extends AndroidScreenBase implements ArticleScreen {

    /**
     * The bottom action bar, not the WebView, is what says the article is ready to work with.
     * It is a plain Android view that appears once the page is loaded, whereas the WebView is
     * a moving target that can be present while the article is still rendering - and the save
     * button inside this bar is the next thing every scenario touches.
     */
    private static final By actionBar = id("page_actions_tab_layout");
    private static final By saveButton = id("page_save");

    /** Rows of the menu the save button opens once the article is already saved. */
    private static final By addToAnotherList = menuItem("Add to another reading list");

    /**
     * The message the app shows when an article is added to a list that already holds it.
     * <p>
     * It is a snackbar: it animates in, lives for a few seconds and animates out. UiAutomator
     * waits for the screen to be idle before answering, and the snackbar is never on screen
     * while idle - so this is read through a source query that tolerates a busy screen, and it
     * is treated as supporting evidence. The assertion that actually decides the duplicate
     * scenario is the count of rows in the list, which is not time-sensitive at all.
     */
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
