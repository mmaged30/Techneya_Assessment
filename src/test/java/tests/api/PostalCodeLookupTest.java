package tests.api;

import data.TestData;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.api.enums.Schema;
import org.api.models.Place;
import org.api.models.PostalCode;
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/**
 * The Zippopotam postal-code endpoint: {@code GET /{country}/{postalCode}}.
 * <p>
 * The service is read-only and unauthenticated, so no test has anything to set up first.
 * <p>
 * Two findings about the service are built into these tests rather than worked around:
 * <ol>
 *     <li><b>There is no error contract.</b> An unknown country, an uncovered country, a
 *         non-existent code and a malformed code all return the same empty {@code {}} with a
 *         404. {@link #lookupIsRejected} asserts that sameness, because it is what a client
 *         actually has to handle.</li>
 *     <li><b>The German dataset returns broken coordinates</b> - see
 *         {@link #reportCoordinateAnomalies}.</li>
 * </ol>
 */
@Slf4j
public class PostalCodeLookupTest extends BaseApiTest {

    private static final double MAX_LATITUDE = 90;
    private static final double MAX_LONGITUDE = 180;

    // ---------------------------------------------------------------- positive

    @Test(dataProvider = "knownPostalCodes", dataProviderClass = TestData.class,
            groups = {"api", "positive", "smoke"})
    @Description("A known postal code resolves to its place")
    public void knownPostalCodeResolvesToItsPlace(Map<String, String> row) {
        Response response = zippopotam.getLocation(row.get("country"), row.get("postalCode"));
        assertSucceeded(response);

        PostalCode location = response.as(PostalCode.class);
        Assert.assertEquals(location.getCountry(), row.get("countryName"),
                "Unexpected country name.");
        Assert.assertEquals(location.getCountryAbbreviation(), row.get("abbreviation"),
                "Unexpected country abbreviation.");

        // The code the caller sent must come back untouched, or a client cannot correlate it.
        Assert.assertEquals(location.getPostCode(), row.get("postalCode"),
                "The service echoed back a different postal code than the one requested.");

        assertIncludesPlace(location, row.get("place"), row.get("state"), row.get("stateCode"));
    }

    @Test(groups = {"api", "positive"})
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

    @Test(dataProvider = "countryCasing", dataProviderClass = TestData.class,
            groups = {"api", "positive"})
    @Description("The country code is accepted in any casing")
    public void countryCodeIsAcceptedInAnyCasing(Map<String, String> row) {
        Response response = zippopotam.getLocation(row.get("country"), "90210");
        assertSucceeded(response);

        PostalCode location = response.as(PostalCode.class);
        Assert.assertEquals(location.getCountry(), "United States", "Unexpected country name.");
        Assert.assertEquals(location.getCountryAbbreviation(), "US",
                "Unexpected country abbreviation.");
    }

    @Test(groups = {"api", "contract"})
    @Description("A successful lookup matches the published location structure")
    public void successfulLookupMatchesTheLocationSchema() {
        Response response = zippopotam.getLocation("us", "90210");
        assertSucceeded(response);
        response.then().body(matchesJsonSchemaInClasspath(Schema.LOCATION.classpathPath()));
    }

    // ---------------------------------------------------------------- negative

    @Test(dataProvider = "rejectedLookups", dataProviderClass = TestData.class,
            groups = {"api", "negative"})
    @Description("A lookup is rejected, and every reason answers identically")
    public void lookupIsRejected(Map<String, String> row) {
        log.info("Expecting rejection because the {}", row.get("reason"));

        Response response = zippopotam.getLocation(row.get("country"), row.get("postalCode"));
        assertRejectedAsNotFound(response);

        Assert.assertTrue(response.jsonPath().getMap("$").isEmpty(),
                "A rejected lookup is expected to carry an empty object, but the body was: "
                        + response.asString());
    }

    @Test(groups = {"api", "negative", "contract"})
    @Description("A rejected lookup still answers as JSON")
    public void rejectedLookupStillAnswersAsJson() {
        Response response = zippopotam.getLocation("us", "99999");
        assertRejectedAsNotFound(response);
        response.then().body(matchesJsonSchemaInClasspath(Schema.NOT_FOUND.classpathPath()));
    }

    /**
     * These fall through to the web framework's own error page rather than the API, so the
     * answer is an HTML document. Worth pinning: a client parsing every 404 as JSON breaks here.
     */
    @Test(dataProvider = "missingPathSegments", dataProviderClass = TestData.class,
            groups = {"api", "negative"})
    @Description("A request missing a path segment never reaches the API")
    public void requestMissingAPathSegmentNeverReachesTheApi(Map<String, String> row) {
        Response response = zippopotam.getRawPath(row.get("path"));

        Assert.assertEquals(response.statusCode(), 404,
                "The service answered with an unexpected HTTP status.");

        // Asserts the absence of JSON rather than the presence of HTML: the exact error page is
        // the web framework's business, but a client unable to parse this 404 as JSON is the
        // behaviour that would break them.
        String contentType = response.contentType();
        Assert.assertFalse(contentType != null && contentType.contains("json"),
                "Expected a non-JSON error page, but the response was typed as: " + contentType);
    }

    @Test(groups = {"api", "negative"})
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
     * Coordinates are asserted to be numeric, but NOT to fall inside the valid latitude and
     * longitude ranges - a deliberate, documented decision rather than an oversight.
     * <p>
     * The German dataset genuinely returns broken values: {@code de/01067} answers with
     * longitude 51.05 and latitude 14612, which are transposed and out of range. A range
     * assertion would therefore fail against correct, live service behaviour, and excluding
     * Germany to make it pass would quietly hide a real data defect.
     * <p>
     * The defect is surfaced into the report instead: the test stays green because the API met
     * its contract, and the bad data is still visible to whoever reads the run.
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
