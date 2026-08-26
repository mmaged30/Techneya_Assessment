package data;

import org.testng.annotations.DataProvider;
import org.utils.ExcelReader;

/**
 * Every {@code @DataProvider} in the suite, and the only place a workbook or sheet is named.
 * <p>
 * Each provider hands the test one {@code Map<String,String>} per row, keyed by the sheet's
 * header. Adding a case is a row in Excel; nothing here or in a test changes.
 * <p>
 * Only the data sets that genuinely vary live in a workbook. A test with a single set of values
 * keeps them inline, because a one-row spreadsheet is a file to open, not information.
 * <p>
 * None of these are {@code @DataProvider(parallel = true)}. Concurrency comes from the suite
 * XML instead - {@code parallel="methods"} on the API {@code <test>} block - which runs
 * different {@code @Test} methods on different threads. A data-driven method's own rows still
 * run one after another on whichever thread that method landed on; only the methods overlap.
 */
public final class TestData {

    private static final String API_WORKBOOK = "data/api-data.xlsx";
    private static final String MOBILE_WORKBOOK = "data/mobile-data.xlsx";

    private TestData() {
        throw new IllegalStateException("Utility class");
    }

    /** Four countries with genuinely different postal-code formats. */
    @DataProvider(name = "knownPostalCodes")
    public static Object[][] knownPostalCodes() {
        return ExcelReader.dataProvider(API_WORKBOOK, "KnownPostalCodes");
    }

    @DataProvider(name = "countryCasing")
    public static Object[][] countryCasing() {
        return ExcelReader.dataProvider(API_WORKBOOK, "CountryCasing");
    }

    /** One row per distinct reason a lookup can fail, not one row per typo. */
    @DataProvider(name = "rejectedLookups")
    public static Object[][] rejectedLookups() {
        return ExcelReader.dataProvider(API_WORKBOOK, "RejectedLookups");
    }

    @DataProvider(name = "missingPathSegments")
    public static Object[][] missingPathSegments() {
        return ExcelReader.dataProvider(API_WORKBOOK, "MissingPathSegments");
    }

    /**
     * Serial, unlike the API providers: one emulator hosts one session.
     * <p>
     * The term and the article title are separate columns because Wikipedia normalises titles -
     * searching "Artificial Intelligence" returns "Artificial intelligence". Asserting the term
     * back would have quietly tested nothing.
     */
    @DataProvider(name = "searchTerms")
    public static Object[][] searchTerms() {
        return ExcelReader.dataProvider(MOBILE_WORKBOOK, "SearchTerms");
    }
}
