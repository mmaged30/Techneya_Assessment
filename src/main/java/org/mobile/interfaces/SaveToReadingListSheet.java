package org.mobile.interfaces;

/**
 * Choosing where an article is filed: an existing list, or a new one.
 * <p>
 * The sheet and the create-list dialog are modelled together because they are one decision from
 * the user's side, and splitting them would produce a screen object whose only job is to
 * forward a name to a text field.
 */
public interface SaveToReadingListSheet {

    ArticleScreen createList(String listName);

    ArticleScreen chooseList(String listName);
}
