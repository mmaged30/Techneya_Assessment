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
 * What every Android screen shares: how an element is addressed, how a long press is performed,
 * and which interruptions have to be cleared.
 * <p>
 * Everything portable is inherited from {@link MobileScreen}; what is added here is Android and
 * only Android, which is why it lives in the Android package rather than in a base class an iOS
 * screen would also have to extend.
 */
@Slf4j
public abstract class AndroidScreenBase extends MobileScreen {

    /**
     * How long a press is held for the app to treat it as a long press. Android's own threshold
     * is 500 ms; this leaves room above it for a device that is busy.
     */
    private static final Duration LONG_PRESS_DURATION = Duration.ofMillis(1200);

    /**
     * The promotional overlays a freshly installed Wikipedia build shows.
     * <p>
     * Each mobile scenario starts from cleared app data, which is what keeps scenarios
     * independent - but it also means the app treats every run as a first run and interrupts at
     * points that are not tied to any one screen. Every entry was observed on the app under
     * test, not assumed.
     */
    private static final List<By> TRANSIENT_PROMOS = List.of(
            bottomSheetClose(),     // "A Faster way to Search" - the search-widget advert
            id("closeButton"),      // "Introducing Wikipedia games"
            id("buttonView"),       // tooltip balloons - "Share this reading list" and others
            text("Got it"),         // the same balloons when the id does not resolve
            id("negativeButton"));  // "Discover articles picked just for you" - "No thanks"

    // --- Locators ---

    /**
     * An Android resource id, qualified with the package from configuration rather than
     * hard-coded, so the same screens work against a debug or beta build of the app.
     */
    protected static By id(String resourceId) {
        return AppiumBy.id(qualify(resourceId));
    }

    /**
     * An element identified by both its id and its text - a row of a list, in practice.
     * <p>
     * Expressed as a UiSelector rather than an XPath on purpose. XPath makes UiAutomator build
     * a full snapshot of the accessibility tree on every evaluation, and on a screen whose main
     * thread is busy - an article's WebView, most of all - that snapshot times out before it can
     * be produced. A UiSelector is matched by the device without that round trip.
     */
    protected static By idWithText(String resourceId, String text) {
        return uiSelector("new UiSelector().resourceId(\"" + qualify(resourceId)
                + "\").text(\"" + text + "\")");
    }

    /** A framework-owned id such as an AlertDialog button, which is never app-scoped. */
    protected static By androidId(String resourceId) {
        return AppiumBy.id("android:id/" + resourceId);
    }

    protected static By text(String value) {
        return uiSelector("new UiSelector().text(\"" + value + "\")");
    }

    protected static By uiSelector(String expression) {
        return AppiumBy.androidUIAutomator(expression);
    }

    /**
     * The close control of a promotional bottom sheet, scoped to the sheet rather than matched
     * on the description alone, so it can never pick up a "Close" belonging to something else.
     */
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

    /**
     * Delegated to the driver's own gesture endpoint rather than assembled from W3C pointer
     * actions. A hand-built sequence expresses the hold as a pause between pointer-down and
     * pointer-up, and that pause is not always honoured: the emulator collapsed it often enough
     * that the press arrived as an ordinary tap, which on a list row opens the article instead
     * of its menu - a failure that looks like a wrong locator and is not one.
     */
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
