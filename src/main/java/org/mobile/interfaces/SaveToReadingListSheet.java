package org.mobile.interfaces;

/**
 * Handles target list selection and inline creation dialogs for saving articles.
 * Models both components together to simplify the article-saving flow without single-purpose wrappers.
 */
public interface SaveToReadingListSheet {

    ArticleScreen createList(String listName);

    ArticleScreen chooseList(String listName);
}
