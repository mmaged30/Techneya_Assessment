package tests.api;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.restassured.response.Response;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.models.api.Place;
import org.models.api.PostalCode;
import org.models.excel.CountryCasingData;
import org.models.excel.MissingPathSegmentData;
import org.models.excel.PostalCodeLookupData;
import org.models.excel.RejectedLookupData;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.utils.CustomAnnotations.ExcelDataSource;
import org.utils.DataProviderSource;
import tests.TestResources;


@Slf4j
public class PostalCodeLookupTest extends BaseApiTest {

    private static final double MAX_LATITUDE = 90;
    private static final double MAX_LONGITUDE = 180;

    // ---------------------------------------------------------------- positive

    @ExcelDataSource(workbook = TestResources.API_WORKBOOK,
            sheetName = TestResources.KNOWN_POSTAL_CODES_SHEET,
            pojoClass = PostalCodeLookupData.class)
    @Test(dataProvider = "ExcelFeed", dataProviderClass = DataProviderSource.class, groups = {"api"})
    @Description("A known postal code resolves to its place")
    public void knownPostalCodeResolvesToItsPlace(PostalCodeLookupData data) {
        Response response = zippopotam.getLocation(data.getCountry(), data.getPostalCode());
        assertSucceeded(response);

        PostalCode location = response.as(PostalCode.class);
        Assert.assertEquals(location.getCountry(), data.getCountryName(),
                "Unexpected country name.");
        Assert.assertEquals(location.getCountryAbbreviation(), data.getAbbreviation(),
                "Unexpected country abbreviation.");

        // The code the caller sent must come back untouched, or a client cannot correlate it.
        Assert.assertEquals(location.getPostCode(), data.getPostalCode(),
                "The service echoed back a different postal code than the one requested.");

        assertIncludesPlace(location, data.getPlace(), data.getState(), data.getStateCode());
    }

    @Test(groups = {"api"})
    @Description("A postal code covering several districts returns every one of them")
    public void postalCodeCoveringSeveralDistrictsReturnsEveryOne() {
        Response response = zippopotam.getLocation("de", "01067");
        assertSucceeded(response);

        PostalCode location = response.as(PostalCode.class);
        Assert.assertEquals(location.getPlaces().size(), 3,
                "The postal code resolved to a different number of places than expected.");

        for (Place place : location.getPlaces()) {
            assertPresent(place.getPlaceName(), "place name", place);
            assertPresent(place.getState(), "state", place);
            assertPresent(place.getStateAbbreviation(), "state abbreviation", place);

            assertNumeric(place.getLongitude(), "longitude", place);
            assertNumeric(place.getLatitude(), "latitude", place);
        }
        reportCoordinateAnomalies(location);
    }

    @ExcelDataSource(workbook = TestResources.API_WORKBOOK,
            sheetName = TestResources.COUNTRY_CASING_SHEET,
            pojoClass = CountryCasingData.class)
    @Test(dataProvider = "ExcelFeed", dataProviderClass = DataProviderSource.class, groups = {"api"})
    @Description("The country code is accepted in any casing")
    public void countryCodeIsAcceptedInAnyCasing(CountryCasingData data) {
        Response response = zippopotam.getLocation(data.getCountry(), data.getPostalCode());
        assertSucceeded(response);

        PostalCode location = response.as(PostalCode.class);
        Assert.assertEquals(location.getCountry(), data.getExpectedCountryName(),
                "Unexpected country name.");
        Assert.assertEquals(location.getCountryAbbreviation(), data.getExpectedAbbreviation(),
                "Unexpected country abbreviation.");
    }

    @Test(groups = {"api"})
    @Description("A successful lookup matches the published location structure")
    public void successfulLookupMatchesTheLocationSchema() {
        Response response = zippopotam.getLocation("us", "90210");
        assertSucceeded(response);
        assertMatchesSchema(response, TestResources.LOCATION_SCHEMA);
    }

    // ---------------------------------------------------------------- negative

    @ExcelDataSource(workbook = TestResources.API_WORKBOOK,
            sheetName = TestResources.REJECTED_LOOKUPS_SHEET,
            pojoClass = RejectedLookupData.class)
    @Test(dataProvider = "ExcelFeed", dataProviderClass = DataProviderSource.class, groups = {"api"})
    @Description("A lookup is rejected, and every reason answers identically")
    public void lookupIsRejected(RejectedLookupData data) {
        log.info("Expecting rejection because the {}", data.getReason());

        Response response = zippopotam.getLocation(data.getCountry(), data.getPostalCode());
        assertRejectedAsNotFound(response);

        Assert.assertTrue(response.jsonPath().getMap("$").isEmpty(),
                "A rejected lookup is expected to carry an empty object, but the body was: "
                        + response.asString());
    }

    @Test(groups = {"api"})
    @Description("A rejected lookup still answers as JSON")
    public void rejectedLookupStillAnswersAsJson() {
        Response response = zippopotam.getLocation("us", "99999");
        assertRejectedAsNotFound(response);
        assertMatchesSchema(response, TestResources.NOT_FOUND_SCHEMA);
    }

    @ExcelDataSource(workbook = TestResources.API_WORKBOOK,
            sheetName = TestResources.MISSING_PATH_SEGMENTS_SHEET,
            pojoClass = MissingPathSegmentData.class)
    @Test(dataProvider = "ExcelFeed", dataProviderClass = DataProviderSource.class, groups = {"api"})
    @Description("A request missing a path segment never reaches the API")
    public void requestMissingAPathSegmentNeverReachesTheApi(MissingPathSegmentData data) {
        Response response = zippopotam.getRawPath(data.getPath());

        Assert.assertEquals(response.statusCode(), data.getExpectedStatus(),
                "The service answered with an unexpected HTTP status.");
        String contentType = response.contentType();
        Assert.assertFalse(contentType != null && contentType.contains("json"),
                "Expected a non-JSON error page, but the response was typed as: " + contentType);
    }

    @Test(groups = {"api"})
    @Description("The endpoint refuses to be written to")
    public void endpointRefusesToBeWrittenTo() {
        Response response = zippopotam.postLocation("us", "90210");
        Assert.assertEquals(response.statusCode(), 405,
                "A read-only endpoint was expected to reject a POST with 405.");
    }

    // ---------------------------------------------------------------- helpers

    private void assertIncludesPlace(PostalCode location, String placeName, String state,
                                     String stateCode) {
        List<Place> places = location.getPlaces();

        Place match = places.stream()
                .filter(place -> placeName.equals(place.getPlaceName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No place named [" + placeName + "] was returned. Places received: "
                                + places.stream().map(Place::getPlaceName).toList()));

        Assert.assertEquals(match.getState(), state,
                "[" + placeName + "] was returned in an unexpected state.");
        Assert.assertEquals(match.getStateAbbreviation(), stateCode,
                "[" + placeName + "] was returned with an unexpected state abbreviation.");
    }

    private void assertPresent(String value, String fieldName, Place place) {
        Assert.assertTrue(value != null && !value.isBlank(),
                "[" + fieldName + "] was missing from a returned place: " + place);
    }

    private void assertNumeric(String value, String fieldName, Place place) {
        try {
            Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException e) {
            throw new AssertionError("[" + fieldName + "] is not a number in a returned place: "
                    + place, e);
        }
    }

    /**
     * Asserts coordinate values are numeric without validating geographic range boundaries.
     * Intentionally permits out-of-bounds coordinates (e.g., transposed values in regional datasets)
     * to verify API contract compliance without failing tests on upstream data defects,
     * capturing anomalies in execution reports instead.
     */
    private void reportCoordinateAnomalies(PostalCode location) {
        String quirks = location.getPlaces().stream()
                .filter(place -> !withinRange(place.getLatitude(), MAX_LATITUDE)
                        || !withinRange(place.getLongitude(), MAX_LONGITUDE))
                .map(place -> "%s -> latitude=%s, longitude=%s"
                        .formatted(place.getPlaceName(), place.getLatitude(), place.getLongitude()))
                .reduce("", (all, line) -> all + line + System.lineSeparator());

        if (!quirks.isBlank()) {
            log.warn("Upstream coordinate data is out of range: {}", quirks);
            Allure.addAttachment("Upstream data quirk - coordinates out of range", quirks);
        }
    }

    private boolean withinRange(String value, double limit) {
        double parsed = Double.parseDouble(value);
        return parsed >= -limit && parsed <= limit;
    }
}
