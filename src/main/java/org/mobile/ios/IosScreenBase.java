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
 * Base class for iOS-specific screen behaviors, mirroring {@link org.mobile.android.AndroidScreenBase}.
 * Provides XCUITest mechanics (accessibility IDs, NSPredicates, long press, bundle ID).
 * @note Unverified: Requires macOS/Xcode execution environment.
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

    protected static By predicate(String nsPredicate) {
        return AppiumBy.iOSNsPredicateString(nsPredicate);
    }

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
     * Dynamic interruption handling hook for iOS screens.
     * Intentionally left un-implemented pending direct validation against
     * live iOS promo overlays.
     */
    @Override
    protected void dismissTransientPromos() {
        // Intentionally empty - see the Javadoc above.
    }
}
