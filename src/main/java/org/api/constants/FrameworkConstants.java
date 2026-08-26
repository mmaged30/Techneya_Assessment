package org.api.constants;

/** Values shared by BOTH the API and UI layers, so neither reaches into the other's. */
public class FrameworkConstants {
    private FrameworkConstants() {
        throw new IllegalStateException("Constants class");
    }

    // --- Configuration ---
    public static final String BASE_CONFIG_FILE = "config/global.properties";
    public static final String PLATFORM_CONFIG_TEMPLATE = "config/%s.properties";

    // --- Output Artifacts (kept under target/ so 'mvn clean' wipes them) ---
    public static final String SCREENSHOT_DIRECTORY = "target/screenshots";
    public static final String ALLURE_RESULTS_DIRECTORY = "target/allure-results";

}
