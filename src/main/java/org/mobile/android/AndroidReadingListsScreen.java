package org.mobile.android;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.mobile.interfaces.ReadingListDetailScreen;
import org.mobile.interfaces.ReadingListsScreen;
import org.openqa.selenium.By;

/** The Saved tab: every reading list, with a filter for finding one by name. */
@Slf4j
public class AndroidReadingListsScreen extends AndroidScreenBase implements ReadingListsScreen {

    private static final By listsRecycler = id("recycler_view");
    private static final By filterButton = id("menu_search_lists");

    /** The filter reuses the app's shared search field, the same id the article search uses. */
    private static final By filterInput = id("search_src_text");

    AndroidReadingListsScreen awaitLoaded() {
        awaitPastPromos(listsRecycler, "reading lists");
        return this;
    }

    @Override
    @Step("Find the reading list '{0}'")
    public AndroidReadingListsScreen filterBy(String listName) {
        tap(filterButton, "Filter my lists");
        type(filterInput, listName, "List filter field");
        wait.visible(listNamed(listName));
        return this;
    }

    @Override
    @Step("Open the reading list '{0}'")
    public ReadingListDetailScreen openList(String listName) {
        tap(listNamed(listName), "reading list '" + listName + "'");
        return new AndroidReadingListDetailScreen().awaitLoaded();
    }

    private static By listNamed(String listName) {
        return idWithText("item_title", listName);
    }
}
