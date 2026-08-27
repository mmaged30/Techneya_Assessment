package org.api.controllers;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.api.constants.ApiConstants;
import org.utils.ConfigManager;
import org.utils.ExecutionFootprint;

/**
 * Provides the common request setup, including base URI, content type,
 * Allure logging, and thread-safe request specifications.
 * Service classes use request() to build their requests.
 */
@Slf4j
public class BaseController {
    // One spec per thread to prevent request configuration from leaking between parallel tests.
    private static final ThreadLocal<RequestSpecification> threadLocalSpec = new ThreadLocal<>();
    protected static RequestSpecification getBaseSpec() {
        if (threadLocalSpec.get() == null) {
            String baseUri = ConfigManager.getProperty("api.base.uri");
            log.info("Connecting to Endpoint: {}", baseUri);
            RequestSpecification spec = new RequestSpecBuilder()
                    .setBaseUri(baseUri)
                    .setContentType(ApiConstants.CONTENT_TYPE_JSON)
                    // Attaches request + response to the Allure step on every call.
                    .addFilter(new AllureRestAssured())
                    .build();
            threadLocalSpec.set(spec);
        }
        return threadLocalSpec.get();
    }


    /**
     * Returns a request that can be completed with .when().get(), .queryParam(), etc.
     * <p>
     * The footprint is recorded here rather than in getBaseSpec() so it reflects a request
     * actually being built, not a spec merely being cached for the thread. AllureEnvironmentWriter
     * reads it to decide whether the run touched the API at all.
     */
    protected static RequestSpecification request() {
        ExecutionFootprint.recordApiExercised();
        return RestAssured.given().spec(getBaseSpec());
    }
}
