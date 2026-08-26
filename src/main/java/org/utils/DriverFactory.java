package org.utils;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.enums.Platform;
import org.openqa.selenium.SessionNotCreatedException;

/**
 * Builds an Appium session for the configured platform using platform-specific capabilities.
 * Platform settings are loaded from the corresponding configuration file.
 */
@Slf4j
public final class DriverFactory {

    /**
     * Tracks session launch counts and calculates dynamic port offsets for parallel runs.
     * Prevents port collision when running concurrent Appium driver instances on a single host.
     */
    private static final AtomicInteger SESSION_COUNTER = new AtomicInteger(0);

    private DriverFactory() {
        throw new IllegalStateException("Utility class");
    }

    /** Called once per test by {@code BaseMobileTest}. */
    public static AppiumDriver create(String sessionName) {
        Platform platform = Platform.fromString(ConfigManager.getProperty("platform"));
        URL serverUrl = AppiumServer.resolveUrl();

        log.info("Starting an Appium session | platform=[{}] server=[{}] test=[{}] thread=[{}]",
                platform, serverUrl, sessionName, Thread.currentThread().getName());

        try {
            AppiumDriver driver = switch (platform) {
                case ANDROID -> new AndroidDriver(serverUrl, androidOptions());
                case IOS -> new IOSDriver(serverUrl, iosOptions());
            };
            applySessionSettings(driver);
            ExecutionFootprint.recordMobileExercised();
            return driver;

        } catch (SessionNotCreatedException e) {
            throw new IllegalStateException("❌ CRITICAL: Appium refused to create a session at "
                    + serverUrl + ". Check that the Appium server is running ('appium'), that a "
                    + "device or emulator is attached ('adb devices'), and that the app under test "
                    + "is installed. Appium's own reason follows.", e);
        }
    }

    // --- Capability builders ---

    private static UiAutomator2Options androidOptions() {
        UiAutomator2Options options = new UiAutomator2Options()
                .setAutomationName("UiAutomator2")
                .setDeviceName(ConfigManager.getProperty("device.name"))
                .setAppPackage(ConfigManager.getProperty("app.package"))
                .setAppActivity(ConfigManager.getProperty("app.activity"))
                // Grants runtime permissions up front so no OS dialog can interrupt a scenario.
                .setAutoGrantPermissions(true)
                // Turns off the device window animations for the session. Less work for an
                // emulator to do, and one less source of a tap landing mid-transition.
                .setDisableWindowAnimation(true)
                .setNewCommandTimeout(newCommandTimeout())
                // A device-side helper port of its own, so two sessions on this host cannot
                // fight over one.
                .setSystemPort(nextHelperPort());

        applyOptionalPlatformVersion(options::setPlatformVersion);
        applyOptionalApp(options::setApp);
        applyResetBehaviour(options::setNoReset, options::setFullReset);
        return options;
    }

    /**
     * Defines execution options for iOS driver sessions.
     * <p>
     * <strong>Unverified:</strong> structural configuration for XCUITest, pending validation on a
     * macOS/Xcode runtime environment.
     */
    private static XCUITestOptions iosOptions() {
        XCUITestOptions options = new XCUITestOptions()
                .setAutomationName("XCUITest")
                .setDeviceName(ConfigManager.getProperty("device.name"))
                .setBundleId(ConfigManager.getProperty("app.bundle.id"))
                .setNewCommandTimeout(newCommandTimeout())
                // WebDriverAgent's port - the iOS counterpart of Android's system port.
                .setWdaLocalPort(nextHelperPort());

        applyOptionalPlatformVersion(options::setPlatformVersion);
        applyOptionalApp(options::setApp);
        applyResetBehaviour(options::setNoReset, options::setFullReset);
        return options;
    }

    // --- Capabilities both platforms accept ---

    private static Duration newCommandTimeout() {
        return Duration.ofSeconds(ConfigManager.getIntProperty("appium.new.command.timeout"));
    }

    private static int nextHelperPort() {
        return ConfigManager.getIntProperty("appium.system.port") + SESSION_COUNTER.getAndIncrement();
    }

    /**
     * Only sent when configured. An unset platform version lets Appium bind to whatever single
     * device is attached, which is what a developer running one emulator actually wants.
     */
    private static void applyOptionalPlatformVersion(Consumer<String> setter) {
        String version = ConfigManager.getProperty("device.platform.version", "");
        if (!version.isBlank()) {
            setter.accept(version);
        }
    }

    /**
     * {@code app.path} is optional and classpath-relative when present. Left unset, Appium
     * launches the already-installed app - the normal case here, because the app under test is
     * installed from the store rather than shipped with the repository.
     */
    private static void applyOptionalApp(Consumer<String> setter) {
        String appPath = ConfigManager.getProperty("app.path", "");
        if (!appPath.isBlank()) {
            setter.accept(onClasspath(appPath).toAbsolutePath().toString());
        }
    }

    /**
     * Configures session-level app data resetting to enforce scenario isolation.
     * Sets {@code app.no.reset=false} to clear persistent application state between sessions,
     * preventing test data pollution across scenarios.
     */
    private static void applyResetBehaviour(Consumer<Boolean> noReset, Consumer<Boolean> fullReset) {
        noReset.accept(ConfigManager.getBooleanProperty("app.no.reset"));
        fullReset.accept(ConfigManager.getBooleanProperty("app.full.reset"));
    }

    /**
     * Tunes driver idle-wait thresholds to capture transient UI elements without missing tap inputs.
     * Shortens animation idle delays so short-lived elements like snackbars can be queried,
     * while maintaining a minimal delay to prevent taps during view transition animations.
     */
    private static void applySessionSettings(AppiumDriver driver) {
        int idleTimeout = ConfigManager.getIntProperty("appium.wait.for.idle.millis");
        try {
            driver.setSetting("waitForIdleTimeout", idleTimeout);
            log.info("Idle wait set to {} ms", idleTimeout);
        } catch (Exception e) {
            // A driver that does not support the setting is workable, just slower.
            log.warn("Could not tune the idle wait. Interactions will be slower.", e);
        }
    }

    // --- Resolution ---

    /** Resolves a classpath resource to an absolute path, never via {@code user.dir}. */
    private static Path onClasspath(String resourcePath) {
        URL resource = DriverFactory.class.getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("❌ CRITICAL: 'app.path' is set to '" + resourcePath
                    + "' but no such resource is on the classpath.");
        }
        try {
            return Paths.get(resource.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("❌ CRITICAL: 'app.path' resolved to an unusable "
                    + "location: " + resource, e);
        }
    }
}
