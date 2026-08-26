package org.mobile.android;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.mobile.interfaces.MainScreen;
import org.mobile.interfaces.ReadingListsScreen;
import org.mobile.interfaces.SearchScreen;
import org.openqa.selenium.By;

/**
 * The host screen: a bottom navigation bar over Home, Saved, Search, Activity and More.
 * Every other screen is reached from here, and returned to through {@link #returnHere()}.
 */
@Slf4j
public class AndroidMainScreen extends AndroidScreenBase implements MainScreen {

    private static final By bottomNavigation = id("main_nav_tab_container");
    private static final By searchTab = id("nav_tab_search");
    private static final By savedTab = id("nav_tab_reading_lists");

    @Override
    @Step("Return to the main screen")
    public AndroidMainScreen returnHere() {
        restartApp();

        // Arrival is judged by the bottom navigation, not by the activity name: launching the
        // app goes through its launcher alias, so the activity on top afterwards is reported
        // under the alias rather than as MainActivity. The navigation bar is what actually
        // defines this screen, and it is what the next step needs to be there anyway.
        awaitPastPromos(bottomNavigation, "bottom navigation");
        return this;
    }

    @Override
    @Step("Open the Search tab")
    public SearchScreen openSearch() {
        tap(searchTab, "Search tab");
        return new AndroidSearchScreen().awaitLoaded();
    }

    @Override
    @Step("Open the Saved tab")
    public ReadingListsScreen openSavedLists() {
        tap(savedTab, "Saved tab");
        return new AndroidReadingListsScreen().awaitLoaded();
    }
}
