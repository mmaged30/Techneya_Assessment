package tests.api;

import io.restassured.response.Response;
import org.api.controllers.ZippopotamController;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

/**
 * Shared setup for API tests: the controller, and the response checks more than one test makes.
 * <p>
 * The controller is built once per class rather than per method. It holds no per-request state -
 * a fresh {@code RequestSpecification} is derived for every call - so sharing one across the
 * methods of a class is safe even when they run in parallel.
 */
public abstract class BaseApiTest {

    protected ZippopotamController zippopotam;

    @BeforeClass(alwaysRun = true)
    public void initClient() {
        zippopotam = new ZippopotamController();
    }

    /** A successful lookup: 200 with a JSON body. */
    protected void assertSucceeded(Response response) {
        Assert.assertEquals(response.statusCode(), 200,
                "The service answered with an unexpected HTTP status.");
        assertJsonContentType(response);
    }

    /**
     * A rejected lookup: 404 with a JSON body.
     * <p>
     * The body is deliberately not asserted. This API answers every unknown country, unknown
     * code and malformed code with an empty object and no error description, so expecting a
     * message would be asserting behaviour the service does not have.
     */
    protected void assertRejectedAsNotFound(Response response) {
        Assert.assertEquals(response.statusCode(), 404,
                "A rejected lookup was expected to answer 404.");
        assertJsonContentType(response);
    }

    private void assertJsonContentType(Response response) {
        String contentType = response.contentType();
        Assert.assertTrue(contentType != null && contentType.contains("json"),
                "Expected a JSON response but the content type was: " + contentType);
    }
}
