package org.api.constants;

public class ApiConstants {
    private ApiConstants() {
        throw new IllegalStateException("Constants class");
    }

    public static final String CONTENT_TYPE_JSON = "application/json";

    // --- Endpoints
    public static final String POSTAL_CODE_ENDPOINT = "/{country}/{postalCode}";
}
