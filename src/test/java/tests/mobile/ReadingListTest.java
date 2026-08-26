package tests.mobile;

import io.qameta.allure.Description;
import org.mobile.interfaces.ArticleScreen;
import org.mobile.interfaces.MainScreen;
import org.mobile.interfaces.ReadingListDetailScreen;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Saving articles into named reading lists, and what the app does with duplicates.
 * <p>
 * Each test gets its own app session with cleared data, so a list created here is never visible
 * to the next test. That is why nothing cleans up after itself: the isolation is a property of
 * the session, not of a teardown step that could itself fail.
 * <p>
 * Neither test is data-driven, deliberately. They exercise one behaviour each, and repeating
 * them with a different list name would add runtime rather than coverage.
 */
public class ReadingListTest extends BaseMobileTest {

    private static final String ARTICLE_SEARCH_TERM = "Artificial Intelligence";
    private static final String ARTICLE_TITLE = "Artificial intelligence";
    private static final String LIST_NAME = "AI Research";

    @Test(groups = {"mobile", "smoke"})
    @Description("An article saved to a new reading list can be found there and removed")
    public void articleSavedToANewListCanBeFoundThereAndRemoved() {
        MainScreen main = launchApp();

        // Mirrors what a reader actually does: save the article, then file it into a list of
        // their own. The app only offers the list chooser once an article is already saved.
        main.openSearch()
                .searchFor(ARTICLE_SEARCH_TERM)
                .openArticle(ARTICLE_TITLE)
                .save()
                .addToReadingList()
                .createList(LIST_NAME);

        ReadingListDetailScreen list = main.returnHere()
                .openSavedLists()
                .filterBy(LIST_NAME)
                .openList(LIST_NAME);

        Assert.assertTrue(list.contains(ARTICLE_TITLE),
                "'" + ARTICLE_TITLE + "' was not found in the reading list.");

        list.removeArticle(ARTICLE_TITLE);

        Assert.assertTrue(list.awaitAbsenceOf(ARTICLE_TITLE),
                "'" + ARTICLE_TITLE + "' is still in the reading list after being removed.");
        Assert.assertTrue(list.isEmpty(),
                "The reading list still shows articles after its last one was removed.");
    }

    /**
     * Duplicate prevention is verified two independent ways, because the app handles it in a way
     * worth pinning down. Adding an article to a list that already holds it is not blocked in
     * the UI - the list is still offered in the chooser - and the app instead detects the
     * duplicate after the choice and answers with a snackbar. So this asserts both that message
     * and that the list still holds exactly one copy, waited on rather than sampled.
     */
    @Test(groups = {"mobile"})
    @Description("Saving the same article twice leaves only one copy in the list")
    public void savingTheSameArticleTwiceLeavesOnlyOneCopy() {
        MainScreen main = launchApp();

        ArticleScreen article = main.openSearch()
                .searchFor(ARTICLE_SEARCH_TERM)
                .openArticle(ARTICLE_TITLE)
                .save()
                .addToReadingList()
                .createList(LIST_NAME);

        article = article.addToReadingList().chooseList(LIST_NAME);

        String message = article.saveMessage();
        Assert.assertTrue(message.contains(LIST_NAME) && message.contains(ARTICLE_TITLE),
                "Expected the app to say that '" + LIST_NAME + "' already contains '"
                        + ARTICLE_TITLE + "', but the message shown was: '" + message + "'");

        ReadingListDetailScreen list = main.returnHere()
                .openSavedLists()
                .filterBy(LIST_NAME)
                .openList(LIST_NAME);

        Assert.assertTrue(list.containsExactly(1, ARTICLE_TITLE),
                "The reading list was expected to hold exactly 1 copy of '" + ARTICLE_TITLE
                        + "', which would mean the app refused to duplicate it.");
    }
}
