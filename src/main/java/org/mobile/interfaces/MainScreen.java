package org.mobile.interfaces;

/** The host screen every other screen is reached from. */
public interface MainScreen {

    /** Brings the app back to this screen from wherever the scenario has got to. */
    MainScreen returnHere();

    SearchScreen openSearch();

    ReadingListsScreen openSavedLists();
}
