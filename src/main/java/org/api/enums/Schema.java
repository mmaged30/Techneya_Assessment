package org.api.enums;

import java.util.Arrays;

/**
 * The JSON Schema contracts this API is asserted against.
 * <p>
 * Gherkin names a schema by what it means ("location"); the file it lives in is an
 * implementation detail. Modelling that pairing as an enum rather than a map of strings means
 * an unknown name fails with the list of known ones, and adding a contract is one line here
 * instead of an edit to a step class.
 */
public enum Schema {

    LOCATION("location", "schemas/location-schema.json"),
    NOT_FOUND("not found", "schemas/not-found-schema.json");

    private final String gherkinName;
    private final String classpathPath;

    Schema(String gherkinName, String classpathPath) {
        this.gherkinName = gherkinName;
        this.classpathPath = classpathPath;
    }

    /** @param gherkinName the name as it is written in a feature file */
    public static Schema fromGherkinName(String gherkinName) {
        return Arrays.stream(values())
                .filter(schema -> schema.gherkinName.equalsIgnoreCase(gherkinName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown schema '" + gherkinName
                        + "'. Known schemas are: "
                        + Arrays.stream(values()).map(Schema::gherkinName).toList()));
    }

    public String gherkinName() {
        return gherkinName;
    }

    public String classpathPath() {
        return classpathPath;
    }
}
