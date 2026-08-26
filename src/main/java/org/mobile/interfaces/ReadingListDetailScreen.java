package org.mobile.interfaces;

/** One reading list and the articles in it. */
public interface ReadingListDetailScreen {

    boolean contains(String articleTitle);

    /**
     * Whether the article settles at exactly this many rows.
     * <p>
     * Waits rather than samples: counting straight after an action races the list's own
     * re-render, so the app is given the full timeout to produce a duplicate before the
     * absence of one is accepted as a result.
     */
    boolean containsExactly(int expectedCopies, String articleTitle);

    ReadingListDetailScreen removeArticle(String articleTitle);

    boolean awaitAbsenceOf(String articleTitle);

    boolean isEmpty();
}
