package tests;

/** Where this project's test resources live, and how to reach them. */
public final class TestResources {

    private TestResources() {
        throw new IllegalStateException("Constants class");
    }

    // --- Workbooks (classpath-relative, under src/test/resources) ---
    public static final String API_WORKBOOK = "data/api-data.xlsx";
    public static final String MOBILE_WORKBOOK = "data/mobile-data.xlsx";

    // --- Excel sheet names ---
    public static final String KNOWN_POSTAL_CODES_SHEET = "KnownPostalCodes";
    public static final String COUNTRY_CASING_SHEET = "CountryCasing";
    public static final String REJECTED_LOOKUPS_SHEET = "RejectedLookups";
    public static final String MISSING_PATH_SEGMENTS_SHEET = "MissingPathSegments";
    public static final String SEARCH_TERMS_SHEET = "SearchTerms";

    // --- JSON Schemas ---
    public static final String LOCATION_SCHEMA = "schemas/location-schema.json";
    public static final String NOT_FOUND_SCHEMA = "schemas/not-found-schema.json";
}
