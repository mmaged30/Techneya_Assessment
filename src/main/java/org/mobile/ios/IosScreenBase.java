package org.mobile.ios;


import io.appium.java_client.AppiumBy;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.mobile.base.MobileScreen;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.utils.ConfigManager;

/**
 * What every iOS screen shares, mirroring {@link org.mobile.android.AndroidScreenBase}.
 * <p>
 * The mechanics here are XCUITest's and are correct regardless of which app is under test: the
 * long-press gesture, the bundle id as the app's identity, and the locator helpers. iOS has no
 * resource-id namespace to qualify against, so there is no package to thread through - what it
 * has instead is accessibility identifiers, which an iOS build must set explicitly, and
 * NSPredicate, which is the efficient way to match on attributes.
 * <p>
 * <strong>Unverified.</strong> Nothing in this package has ever been executed. An XCUITest
 * session needs macOS with Xcode, which this project was not built on.
 */
@Slf4j
public abstract class IosScreenBase extends MobileScreen {

    /** iOS treats roughly half a second as a long press; this leaves room for a busy device. */
    private static final Duration LONG_PRESS_DURATION = Duration.ofMillis(1200);

    // --- Locators ---

    /** The accessibility identifier an iOS build assigns to a view. The preferred locator. */
    protected static By identifier(String accessibilityIdentifier) {
        return AppiumBy.accessibilityId(accessibilityIdentifier);
    }

    /**
     * Matches on an element's attributes.
     * <p>
     * Preferred over XPath for the same reason a UiSelector is preferred on Android: XCUITest
     * evaluates a predicate against the element tree directly, while an XPath forces a full
     * snapshot to be serialised first.
     */
    protected static By predicate(String nsPredicate) {
        return AppiumBy.iOSNsPredicateString(nsPredicate);
    }

    /** A hierarchy query, for the cases a flat predicate cannot express. */
    protected static By classChain(String chain) {
        return AppiumBy.iOSClassChain(chain);
    }

    protected static By staticTextWithLabel(String value) {
        return predicate("type == 'XCUIElementTypeStaticText' AND label == '" + value + "'");
    }

    // --- Platform behaviour ---

    /** iOS identifies an app by its bundle id, where Android uses a package name. */
    @Override
    protected String appId() {
        return ConfigManager.getProperty("app.bundle.id");
    }

    /**
     * XCUITest's own press-and-hold. The Android driver's {@code mobile: longClickGesture} does
     * not exist here, which is precisely why this is a per-platform method rather than something
     * the shared base could branch on.
     */
    @Override
    @Step("Long press: {1}")
    protected void longPress(By locator, String elementName) {
        WebElement element = wait.visible(locator);

        ((JavascriptExecutor) driver).executeScript("mobile: touchAndHold", Map.of(
                "elementId", ((RemoteWebElement) element).getId(),
                "duration", LONG_PRESS_DURATION.toSeconds()));

        log.info("Long pressed [{}]", elementName);
    }

    /**
     * No-op until the iOS build's interruptions have been observed on a real simulator.
     * <p>
     * Deliberately empty rather than a guessed list. The Android list was built by watching the
     * app interrupt and noting what it took to clear each one; inventing an iOS equivalent would
     * produce locators that match nothing and a screen that looks handled and is not. An empty
     * sweep is honest: {@code awaitPastPromos} still works, it simply has nothing to clear yet.
     */
    @Override
    protected void dismissTransientPromos() {
        // Intentionally empty - see the Javadoc above.
    }
}
