package org.api.controllers;

import io.qameta.allure.Step;
import io.restassured.response.Response;

/**
 * The Zippopotam postal-code service: {@code GET /{country}/{postalCode}} is its whole surface.
 * <p>
 * Returns the raw {@link Response} rather than asserting. A client that asserted would force
 * every caller to expect success, which is exactly what the negative scenarios must not do.
 */
public class ZippopotamController extends BaseController {

    /**
     * Kept as a template rather than concatenated at the call site so REST Assured encodes the
     * segments - which is what makes a deliberately malformed input reach the server as sent.
     */
    private static final String POSTAL_CODE_PATH = "/{country}/{postalCode}";

    @Step("GET the location for country '{0}' and postal code '{1}'")
    public Response getLocation(String country, String postalCode) {
        return request()
                .pathParam("country", country)
                .pathParam("postalCode", postalCode)
                .when()
                .get(POSTAL_CODE_PATH);
    }

    /**
     * Sends a raw path instead of the two-segment template, which is the only way to express
     * a request that is missing a segment entirely - something {@link #getLocation} cannot do,
     * because a path parameter always produces a segment.
     */
    @Step("GET the raw path '{0}'")
    public Response getRawPath(String path) {
        return request()
                .when()
                .get(path);
    }

    /** For asserting that a read-only endpoint rejects a write. */
    @Step("POST to the location endpoint for country '{0}' and postal code '{1}'")
    public Response postLocation(String country, String postalCode) {
        return request()
                .pathParam("country", country)
                .pathParam("postalCode", postalCode)
                .when()
                .post(POSTAL_CODE_PATH);
    }
}
