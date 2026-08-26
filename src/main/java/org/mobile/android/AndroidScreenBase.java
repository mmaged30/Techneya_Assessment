package org.mobile.android;


import io.appium.java_client.AppiumBy;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.mobile.base.MobileScreen;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.utils.ConfigManager;

/**
 * Base class for Android-specific screen behaviors (element addressing, long presses, interruption handling).
 * Extends {@link MobileScreen} with Android-only implementations separate from iOS cross-platform logic.
 */
@Slf4j
public abstract class AndroidScreenBase extends MobileScreen {

    /**
     * Long-press duration in milliseconds.
     * Set above Android's default 500 ms threshold to accommodate slower or busy devices.
     */
    private static final Duration LONG_PRESS_DURATION = Duration.ofMillis(1200);

    /**
     * Handles app-wide promotional overlays shown after fresh installs.
     * Cleared test data triggers these first-run interruptions across various screens;
     * this class handles clearing them dynamically.
     */
    private static final List<By> TRANSIENT_PROMOS = List.of(
            bottomSheetClose(),     // "A Faster way to Search" - the search-widget advert
            id("closeButton"),      // "Introducing Wikipedia games"
            id("buttonView"),       // tooltip balloons - "Share this reading list" and others
            text("Got it"),         // the same balloons when the id does not resolve
            id("negativeButton"));  // "Discover articles picked just for you" - "No thanks"

    // --- Locators ---

    protected static By id(String resourceId) {
        return AppiumBy.id(qualify(resourceId));
    }
    protected static By idWithText(String resourceId, String text) {
        return uiSelector("new UiSelector().resourceId(\"" + qualify(resourceId)
                + "\").text(\"" + text + "\")");
    }
    protected static By androidId(String resourceId) {
        return AppiumBy.id("android:id/" + resourceId);
    }
    protected static By text(String value) {
        return uiSelector("new UiSelector().text(\"" + value + "\")");
    }
    protected static By uiSelector(String expression) {
        return AppiumBy.androidUIAutomator(expression);
    }

    private static By bottomSheetClose() {
        return uiSelector("new UiSelector().resourceId(\"" + qualify("design_bottom_sheet") + "\")"
                + ".childSelector(new UiSelector().description(\"Close\"))");
    }

    private static String qualify(String resourceId) {
        return ConfigManager.getProperty("app.package") + ":id/" + resourceId;
    }

    // --- Platform behaviour ---

    /** Android identifies an app by its package name, where iOS uses a bundle id. */
    @Override
    protected String appId() {
        return ConfigManager.getProperty("app.package");
    }

    @Override
    @Step("Long press: {1}")
    protected void longPress(By locator, String elementName) {
        WebElement element = wait.visible(locator);

        ((JavascriptExecutor) driver).executeScript("mobile: longClickGesture", Map.of(
                "elementId", ((RemoteWebElement) element).getId(),
                "duration", LONG_PRESS_DURATION.toMillis()));

        log.info("Long pressed [{}]", elementName);
    }

    /**
     * Checked with {@code findElements}, which returns immediately when nothing is showing, so
     * calling this from inside a wait costs nothing on the runs where the app behaves.
     */
    @Override
    protected void dismissTransientPromos() {
        for (By dismisser : TRANSIENT_PROMOS) {
            List<WebElement> found = driver.findElements(dismisser);
            if (!found.isEmpty()) {
                found.getFirst().click();
                log.info("Dismissed a first-run promotion via [{}]", dismisser);
            }
        }
    }
}
