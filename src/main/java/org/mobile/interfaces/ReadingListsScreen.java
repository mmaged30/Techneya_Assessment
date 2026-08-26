package org.mobile.interfaces;

/** Every reading list, with a filter for finding one by name. */
public interface ReadingListsScreen {

    ReadingListsScreen filterBy(String listName);

    ReadingListDetailScreen openList(String listName);
}
