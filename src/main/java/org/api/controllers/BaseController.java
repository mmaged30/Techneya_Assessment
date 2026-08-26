package org.api.controllers;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.utils.ConfigManager;
import org.utils.ExecutionFootprint;

/**
 * How every API client in this framework connects: base URI from configuration, and the Allure
 * filter that attaches each request and response to the report.
 * <p>
 * One instance per scenario, built by Cucumber's dependency injection. That is what makes this
 * safe under parallel execution - two scenarios cannot share an instance they were never both
 * given - and it is why there is no thread-local specification here, and no teardown hook that
 * has to remember to clear one.
 * <p>
 * A base class rather than a collaborator because a client genuinely <em>is</em> one of these,
 * and because the state it holds is instance state. The earlier version of this idea was an
 * all-static class that subclasses extended purely to reach a helper; that bought nothing a
 * static import would not have, which is why it is gone.
 */
@Slf4j
public abstract class BaseController {

    private final RequestSpecification baseSpec;

    protected BaseController() {
        String baseUri = ConfigManager.getProperty("api.base.uri");
        log.info("Connecting to endpoint: {}", baseUri);

        this.baseSpec = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                // Attaches the full request and response to the Allure step on every call,
                // which is the API equivalent of a failure screenshot.
                .addFilter(new AllureRestAssured())
                .build();
    }

    /**
     * A request ready to be completed with {@code .when().get(...)}.
     * <p>
     * The footprint is recorded here rather than in the constructor, so the report reflects a
     * request actually being built rather than a client merely being instantiated.
     * <p>
     * No default content type is set: these calls carry no body, and sending a Content-Type on
     * a bodyless request would describe something that does not exist.
     */
    protected RequestSpecification request() {
        ExecutionFootprint.recordApiExercised();
        return RestAssured.given().spec(baseSpec);
    }
}
