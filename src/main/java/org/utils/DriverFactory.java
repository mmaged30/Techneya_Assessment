package org.utils;


import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.enums.ExecutionTarget;
import org.enums.Platform;
import org.openqa.selenium.SessionNotCreatedException;

/**
 * Builds the Appium session for the configured platform - the driver-side counterpart of
 * {@code org.mobile.interfaces.Screens}.
 * <p>
 * Capability-driven rather than class-per-platform: the two builders differ only in the options
 * object they populate and three setters, and every value they read comes from the platform
 * overlay ({@code config/android.properties} / {@code config/ios.properties}). The switch is
 * exhaustive over {@link Platform}, so adding a platform is a compile error here until it is
 * handled - which is a stronger guarantee than a registry that fails at runtime.
 * <p>
 * Two axes, resolved independently: <b>which platform</b> ({@link Platform}) and <b>where the
 * session runs</b> ({@link ExecutionTarget}). Local is the default, so an emulator plus a local
 * Appium server needs no flags. A remote target adds its own capability block and skips the
 * capabilities that only mean something on this machine.
 */
@Slf4j
public final class DriverFactory {

    /**
     * Counts the sessions this run has started, and doubles as the per-session offset for the
     * device-side helper port: two parallel sessions on one host would otherwise fight over it.
     */
    private static final AtomicInteger SESSION_COUNTER = new AtomicInteger(0);

    private DriverFactory() {
        throw new IllegalStateException("Utility class");
    }

    /** Called once per test by {@code BaseMobileTest}. */
    public static AppiumDriver create(String sessionName) {
        Platform platform = Platform.fromString(ConfigManager.getProperty("platform"));
        ExecutionTarget target = ExecutionTarget.fromString(
                ConfigManager.getProperty("execution.target", "local"));
        URL serverUrl = serverUrl(target);

        log.info("Starting an Appium session | platform=[{}] target=[{}] server=[{}] thread=[{}]",
                platform, target, serverUrl, Thread.currentThread().getName());

        try {
            AppiumDriver driver = switch (platform) {
                case ANDROID -> new AndroidDriver(serverUrl, androidOptions(target, sessionName));
                case IOS -> new IOSDriver(serverUrl, iosOptions(target, sessionName));
            };
            applySessionSettings(driver);
            ExecutionFootprint.recordMobileExercised();
            return driver;

        } catch (SessionNotCreatedException e) {
            throw new IllegalStateException(sessionCreationHelp(target, serverUrl), e);
        }
    }

    /**
     * The advice differs enough between targets to be worth branching on: "start Appium and
     * check adb" is useless when the session was refused by a grid a thousand miles away.
     */
    private static String sessionCreationHelp(ExecutionTarget target, URL serverUrl) {
        if (target.isRemote()) {
            return "❌ CRITICAL: " + target + " refused to create a session at " + serverUrl
                    + ". Check that BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY are set and "
                    + "valid, that the device name and OS version exist on the grid, that the app "
                    + "has been uploaded and 'browserstack.app' names its bs:// id, and that your "
                    + "plan's parallel-session limit is not already used up. The grid's own reason "
                    + "follows.";
        }
        return "❌ CRITICAL: Appium refused to create a session at " + serverUrl
                + ". Check that the Appium server is running ('appium'), that a device or emulator "
                + "is attached ('adb devices'), and that the app under test is installed. Appium's "
                + "own reason follows.";
    }

    // --- Capability builders ---

    private static UiAutomator2Options androidOptions(ExecutionTarget target, String sessionName) {
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
                .setNewCommandTimeout(newCommandTimeout());

        // systemPort keeps two sessions on ONE host from fighting over the device-side helper
        // port. On a grid each session owns its own machine, so the capability is meaningless
        // there and some grids reject it outright.
        if (!target.isRemote()) {
            options.setSystemPort(nextHelperPort());
        }

        applyOptionalPlatformVersion(options::setPlatformVersion);
        applyApp(target, options::setApp);
        applyResetBehaviour(options::setNoReset, options::setFullReset);
        applyRemoteOptions(target, sessionName, options::setCapability);
        return options;
    }

    /**
     * <strong>Unverified:</strong> an XCUITest session needs macOS with Xcode, and none has ever
     * been started from this project. These are the shape an iOS run takes, not values confirmed
     * against a running simulator.
     */
    private static XCUITestOptions iosOptions(ExecutionTarget target, String sessionName) {
        XCUITestOptions options = new XCUITestOptions()
                .setAutomationName("XCUITest")
                .setDeviceName(ConfigManager.getProperty("device.name"))
                .setBundleId(ConfigManager.getProperty("app.bundle.id"))
                .setNewCommandTimeout(newCommandTimeout());

        // WebDriverAgent's port - the iOS counterpart of Android's system port, and local-only
        // for the same reason.
        if (!target.isRemote()) {
            options.setWdaLocalPort(nextHelperPort());
        }

        applyOptionalPlatformVersion(options::setPlatformVersion);
        applyApp(target, options::setApp);
        applyResetBehaviour(options::setNoReset, options::setFullReset);
        applyRemoteOptions(target, sessionName, options::setCapability);
        return options;
    }

    // --- Remote grid ---

    /**
     * Adds the vendor capability block a remote grid needs, and nothing at all for a local run.
     * <p>
     * Credentials come from the environment, never from a properties file: {@code config/} is
     * committed, and an access key in it would be published the moment the repository is shared.
     */
    private static void applyRemoteOptions(ExecutionTarget target, String sessionName,
                                           java.util.function.BiConsumer<String, Object> setter) {
        if (!target.isRemote()) {
            return;
        }

        Map<String, Object> vendor = new HashMap<>();
        vendor.put("userName", requiredEnv("BROWSERSTACK_USERNAME"));
        vendor.put("accessKey", requiredEnv("BROWSERSTACK_ACCESS_KEY"));
        vendor.put("projectName", ConfigManager.getProperty("browserstack.project.name", "Automation"));
        vendor.put("buildName", ConfigManager.getProperty("browserstack.build.name", "local-build"));
        vendor.put("sessionName", sessionName);

        // Optional niceties: off by default because each one slows a session down.
        vendor.put("debug", ConfigManager.getProperty("browserstack.debug", "false"));
        vendor.put("networkLogs", ConfigManager.getProperty("browserstack.network.logs", "false"));

        setter.accept("bstack:options", vendor);
    }

    /** A credential that has no safe default - failing loudly beats a confusing 401 from the grid. */
    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("❌ CRITICAL: environment variable '" + name
                    + "' is not set. A remote run needs it, and it must come from the environment "
                    + "rather than a committed properties file.");
        }
        return value;
    }

    // --- Capabilities both platforms accept ---

    private static java.time.Duration newCommandTimeout() {
        return java.time.Duration.ofSeconds(ConfigManager.getIntProperty("appium.new.command.timeout"));
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
     * What "the app" means depends on where the session runs.
     * <p>
     * Locally, {@code app.path} is an optional classpath-relative file; left unset, Appium
     * launches the already-installed package, which is the normal case here because the app is
     * installed from the store rather than shipped with the repository. On a grid there is no
     * local filesystem to point at - the app is uploaded ahead of the run and referenced by the
     * {@code bs://} id the upload returns.
     */
    private static void applyApp(ExecutionTarget target, Consumer<String> setter) {
        if (target.isRemote()) {
            String uploadedApp = ConfigManager.getProperty("browserstack.app", "");
            if (!uploadedApp.isBlank()) {
                setter.accept(uploadedApp);
            }
            return;
        }

        String appPath = ConfigManager.getProperty("app.path", "");
        if (!appPath.isBlank()) {
            setter.accept(onClasspath(appPath).toAbsolutePath().toString());
        }
    }

    /**
     * Reading-list scenarios mutate persistent app state, so each scenario must start from a
     * known one. {@code app.no.reset=false} lets Appium clear the app data between sessions,
     * which is what keeps a list created by one scenario from being seen by the next.
     */
    private static void applyResetBehaviour(Consumer<Boolean> noReset, Consumer<Boolean> fullReset) {
        noReset.accept(ConfigManager.getBooleanProperty("app.no.reset"));
        fullReset.accept(ConfigManager.getBooleanProperty("app.full.reset"));
    }

    /**
     * Shortens how long the automation engine waits for the screen to go fully idle before
     * answering.
     * <p>
     * The default is generous enough that a snackbar can never be observed: it animates in,
     * lives a few seconds and animates out, so the screen is not idle while it is up and a
     * query for one comes back empty however quickly it is made. Shortening the wait is what
     * lets the duplicate-save message be read at all - and it takes a fixed delay off every
     * other interaction as a side benefit.
     * <p>
     * Shortened rather than switched off. At zero, taps land during a dialog entry animation
     * and are silently swallowed; a few hundred milliseconds is enough for a view to settle
     * while still being far shorter than the life of a snackbar.
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

    /** The local Appium server, or the grid's hub, depending on the target. */
    private static URL serverUrl(ExecutionTarget target) {
        String key = target.isRemote() ? "browserstack.hub.url" : "appium.server.url";
        String url = ConfigManager.getProperty(key);
        try {
            return URI.create(url).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("❌ CRITICAL: '" + key + "' is not a usable URL: " + url, e);
        }
    }

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
