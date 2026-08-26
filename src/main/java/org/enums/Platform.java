package org.enums;

import java.util.Arrays;

/**
 * Mobile platforms the framework can drive.
 * Using an enum prevents an invalid {@code -Dplatform} value from reaching Appium as a
 * capability and failing with an opaque server-side error.
 */
public enum Platform {
    ANDROID,
    IOS;

    /** Accepts any casing or surrounding whitespace, e.g. android / Android / ANDROID. */
    public static Platform fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("❌ CRITICAL: No platform was supplied. "
                    + "Set 'platform' in config/global.properties or pass -Dplatform=android");
        }
        try {
            return Platform.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("❌ CRITICAL: Unsupported platform '" + value
                    + "'. Supported platforms are: " + Arrays.toString(values()), e);
        }
    }
}
