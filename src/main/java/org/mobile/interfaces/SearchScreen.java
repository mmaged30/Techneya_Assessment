package org.mobile.interfaces;

/** Article search and its results. */
public interface SearchScreen {

    SearchScreen searchFor(String term);

    /** Waits for results before answering, so "not yet" is never reported as "not there". */
    boolean resultsInclude(String articleTitle);

    ArticleScreen openArticle(String articleTitle);
}
