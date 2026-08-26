package org.utils;

import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.api.constants.FrameworkConstants;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Captures failure evidence for Allure and {@code target/screenshots}.
 * <p>
 * Written against {@link WebDriver}, not {@code AppiumDriver}, so a session of either kind is
 * photographed by the same code. Only the two places where the payload genuinely differs -
 * the page-source media type, and Android's current activity - inspect the concrete type.
 */
@Slf4j
public final class ScreenshotUtils {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private ScreenshotUtils() {
        throw new IllegalStateException("Utility class");
    }

    /** @param contextName names the artifact - the failing scenario, in practice */
    public static void captureFailure(String contextName) {
        if (!DriverManager.hasDriver()) {
            // An API scenario. There is nothing to photograph, and that is not an error.
            return;
        }

        byte[] screenshot = takeScreenshot();
        if (screenshot.length == 0) {
            return;
        }

        attachToAllure(contextName + " - failure screenshot", screenshot);
        writeToDisk(contextName, screenshot);
        attachPageSource();
        attachCurrentActivity();
    }

    private static byte[] takeScreenshot() {
        try {
            return ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.BYTES);
        } catch (Exception e) {
            log.warn("Could not capture a screenshot. The session may already be dead.", e);
            return new byte[0];
        }
    }

    private static void attachToAllure(String name, byte[] screenshot) {
        try {
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(screenshot), ".png");
        } catch (Exception e) {
            log.warn("Failed to attach the screenshot to the Allure report.", e);
        }
    }

    private static void writeToDisk(String contextName, byte[] screenshot) {
        try {
            Path directory = Paths.get(FrameworkConstants.SCREENSHOT_DIRECTORY);
            Files.createDirectories(directory);

            Path target = directory.resolve(uniqueFileName(contextName));

            Files.write(target, screenshot);
            log.info("Screenshot saved to: {}", target.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Failed to write the screenshot to disk.", e);
        }
    }

    private static String uniqueFileName(String contextName) {
        String safeName = contextName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "%s_%s_t%d_%d.png".formatted(
                safeName,
                LocalDateTime.now().format(TIMESTAMP),
                Thread.currentThread().threadId(),
                SEQUENCE.incrementAndGet());
    }

    /**
     * A mobile page source is an XML view hierarchy, not HTML. Attaching it with the wrong
     * media type makes Allure render it as unreadable markup, so the type follows the driver.
     */
    private static void attachPageSource() {
        try {
            WebDriver driver = DriverManager.getDriver();
            boolean isMobile = driver instanceof io.appium.java_client.AppiumDriver;

            Allure.addAttachment(
                    "Page source at failure",
                    isMobile ? "text/xml" : "text/html",
                    driver.getPageSource(),
                    isMobile ? ".xml" : ".html");
        } catch (Exception e) {
            log.debug("Failed to attach the page source.", e);
        }
    }

    /** The Android equivalent of "which page was I on" - the single most useful line of context. */
    private static void attachCurrentActivity() {
        WebDriver driver = DriverManager.getDriver();
        if (!(driver instanceof AndroidDriver androidDriver)) {
            return;
        }
        try {
            Allure.addAttachment("Activity at failure", androidDriver.currentActivity());
        } catch (Exception e) {
            log.debug("Failed to attach the current activity.", e);
        }
    }
}
