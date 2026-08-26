package org.mobile.interfaces;

/** An open article and the saving actions available on it. */
public interface ArticleScreen {

    /** Saves into the default list. Only valid on an article that is not yet saved. */
    ArticleScreen save();

    /** Opens the list chooser, which the app only offers once an article is already saved. */
    SaveToReadingListSheet addToReadingList();

    /** The message shown after a save, or an empty string if none was on screen. */
    String saveMessage();
}
