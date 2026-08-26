package org.utils;


import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

/**
 * The single gateway to every configurable value.
 * <p>
 * Resolution order, highest first:
 * <ol>
 *     <li>{@code -Dkey=value} on the command line</li>
 *     <li>the platform overlay, e.g. {@code config/android.properties}</li>
 *     <li>{@code config/global.properties}</li>
 * </ol>
 * The overlay exists so a mobile capability lives with the platform it belongs to instead of
 * being prefixed into one flat file - and so adding iOS is a new file, not an edit to an
 * existing one. It is optional: an API-only run never needs it.
 */
@Slf4j
public final class ConfigManager {

    /** Classpath-relative, under src/test/resources. */
    private static final String BASE_CONFIG_FILE = "config/global.properties";

    /**
     * Per-platform overlay, resolved from the {@code platform} key. Loaded on top of the base
     * file so a mobile-only value never has to be duplicated for the platform that ignores it.
     */
    private static final String PLATFORM_CONFIG_TEMPLATE = "config/%s.properties";

    /**
     * Per-target overlay, resolved from the {@code execution.target} key. Loaded on top of the
     * platform file so a remote grid's device defaults win over a local emulator's, while a
     * {@code -D} flag still beats both.
     */
    private static final String TARGET_CONFIG_TEMPLATE = "config/%s.properties";

    private static final Properties masterProperties = new Properties();

    private static volatile boolean loaded;

    private ConfigManager() {
        throw new IllegalStateException("Utility class");
    }

    /** The explicit wait window used by every {@link Waits} instance. */
    public static Duration getTimeout() {
        return Duration.ofSeconds(getIntProperty("timeout.explicit"));
    }

    public static String getProperty(String key) {
        String value = resolve(key);

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("❌ CRITICAL: Property key '" + key
                    + "' is not defined in the configuration files and no -D override supplied it!");
        }

        return value.trim();
    }

    /** Fallback instead of failing. For genuinely optional keys only. */
    public static String getProperty(String key, String defaultValue) {
        String value = resolve(key);
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    public static int getIntProperty(String key) {
        String value = getProperty(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("❌ CRITICAL: Property '" + key
                    + "' must be a whole number but was '" + value + "'", e);
        }
    }

    public static boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(getProperty(key));
    }

    // --- Loading ---

    private static void loadRequired(String resource) {
        try (InputStream stream = open(resource)) {
            if (stream == null) {
                throw new IllegalStateException("❌ CRITICAL: Configuration file '" + resource
                        + "' was not found on the classpath. It belongs under src/test/resources/.");
            }
            masterProperties.load(stream);
            log.info("🚀 Configuration loaded from [{}]", resource);
        } catch (IOException e) {
            throw new IllegalStateException("❌ FATAL: Failed to read the configuration file: " + resource, e);
        }
    }

    /**
     * Absent overlay files are not an error - an API-only run has no platform. A file that
     * exists but cannot be read is, because that is a broken setup rather than an absent one.
     */
    private static void loadPlatformOverlay() {
        // resolveRaw, not resolve: this runs inside ensureLoaded and must not re-enter it.
        String platform = resolveRaw("platform");
        if (platform == null || platform.isBlank()) {
            return;
        }

        String resource = String.format(PLATFORM_CONFIG_TEMPLATE,
                platform.trim().toLowerCase());

        try (InputStream stream = open(resource)) {
            if (stream == null) {
                throw new IllegalStateException("❌ CRITICAL: platform is set to '" + platform
                        + "' but '" + resource + "' does not exist on the classpath.");
            }
            masterProperties.load(stream);
            log.info("🚀 Platform configuration loaded from [{}]", resource);
        } catch (IOException e) {
            throw new IllegalStateException("❌ FATAL: Failed to read the platform configuration: " + resource, e);
        }
    }

    /**
     * Absent for a local run - there is nothing a local Appium server needs that
     * global.properties does not already hold - and required for any remote target, because a
     * missing hub URL there is a broken setup rather than an absent one.
     */
    private static void loadTargetOverlay() {
        String target = resolveRaw("execution.target");
        if (target == null || target.isBlank() || "local".equalsIgnoreCase(target.trim())) {
            return;
        }

        String resource = String.format(TARGET_CONFIG_TEMPLATE, target.trim().toLowerCase());

        try (InputStream stream = open(resource)) {
            if (stream == null) {
                throw new IllegalStateException("❌ CRITICAL: execution.target is set to '" + target
                        + "' but '" + resource + "' does not exist on the classpath.");
            }
            masterProperties.load(stream);
            log.info("🚀 Target configuration loaded from [{}]", resource);
        } catch (IOException e) {
            throw new IllegalStateException("❌ FATAL: Failed to read the target configuration: "
                    + resource, e);
        }
    }

    private static InputStream open(String resource) {
        return ConfigManager.class.getClassLoader().getResourceAsStream(resource);
    }

    /**
     * Loads on first use rather than in a static initialiser.
     * <p>
     * A static initialiser that throws produces {@code ExceptionInInitializerError} once and
     * {@code NoClassDefFoundError} - with the original cause gone - on every access after it.
     * A missing configuration file is a routine setup mistake and deserves to say so every
     * time it is hit, not only the first.
     */
    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (ConfigManager.class) {
            if (loaded) {
                return;
            }
            loadRequired(BASE_CONFIG_FILE);
            loadPlatformOverlay();
            loadTargetOverlay();
            loaded = true;
        }
    }

    private static String resolve(String key) {
        ensureLoaded();
        return resolveRaw(key);
    }

    /** Reads without triggering a load, for the lookups that happen while loading. */
    private static String resolveRaw(String key) {
        // isBlank, not null-only: a blank -Dkey= must fall through to the file.
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }
        return masterProperties.getProperty(key);
    }
}
