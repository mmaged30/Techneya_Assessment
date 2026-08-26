package org.utils;


import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.api.constants.FrameworkConstants;

/**
 * Central gateway for configurable values.
 * Resolution order: system property → platform config → global config.
 * Platform-specific settings are kept in separate optional overlay files.
 */
@Slf4j
public final class ConfigManager {



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
        String platform = resolveRaw("platform");
        if (platform == null || platform.isBlank()) {
            return;
        }

        String resource = String.format(FrameworkConstants.PLATFORM_CONFIG_TEMPLATE,
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

    private static InputStream open(String resource) {
        return ConfigManager.class.getClassLoader().getResourceAsStream(resource);
    }

    /**
     * Lazy-loads configuration on first use rather than inside a static initializer.
     * Prevents initialization errors from masking underlying configuration failures
     * under generic {@code NoClassDefFoundError} exceptions on subsequent access.
     */
    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (ConfigManager.class) {
            if (loaded) {
                return;
            }
            loadRequired(FrameworkConstants.BASE_CONFIG_FILE);
            loadPlatformOverlay();
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
