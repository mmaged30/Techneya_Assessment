package org.api.controllers;

import io.qameta.allure.Step;
import io.restassured.response.Response;

import static org.api.constants.ApiConstants.POSTAL_CODE_ENDPOINT;

/**
 * Client for the Zippopotam API ({@code GET /{country}/{postalCode}}).
 * Returns raw {@link Response} without assertions to support negative test scenarios.
 */
public class ZippopotamController extends BaseController {

    @Step("GET the location for country '{0}' and postal code '{1}'")
    public Response getLocation(String country, String postalCode) {
        return request()
                .pathParam("country", country)
                .pathParam("postalCode", postalCode)
                .when()
                .get(POSTAL_CODE_ENDPOINT);
    }

    /** Bypasses {@link #getLocation} to send raw paths with missing segments.*/
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
                .post(POSTAL_CODE_ENDPOINT);
    }
}
