package tests.mobile;

import io.qameta.allure.Description;
import org.mobile.interfaces.ArticleScreen;
import org.mobile.interfaces.MainScreen;
import org.mobile.interfaces.ReadingListDetailScreen;
import org.testng.Assert;
import org.testng.annotations.Test;


public class ReadingListTest extends BaseMobileTest {

    private static final String ARTICLE_SEARCH_TERM = "Artificial Intelligence";
    private static final String ARTICLE_TITLE = "Artificial intelligence";
    private static final String LIST_NAME = "AI Research";

    @Test(groups = {"mobile"})
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
