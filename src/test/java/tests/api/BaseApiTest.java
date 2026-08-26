package tests.api;

import io.restassured.response.Response;
import org.api.controllers.ZippopotamController;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;


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

    protected void assertMatchesSchema(Response response, String schemaPath) {
        response.then().body(matchesJsonSchemaInClasspath(schemaPath));
    }
}
