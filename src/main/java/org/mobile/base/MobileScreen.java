package org.mobile.base;


import io.appium.java_client.AppiumBy;
import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.utils.DriverManager;
import org.utils.Waits;

/**
 * Cross-platform base class for core screen actions (find, tap, type, swipe, wait, restart).
 * Exposes template methods for platform-specific behaviors ({@link #longPress},
 * {@link #dismissTransientPromos}, and {@link #appId}) implemented by platform subclasses.
 */
@Slf4j
public abstract class MobileScreen {

    /** Gesture duration, not a wait: a swipe too fast to register is not a swipe. */
    private static final Duration SWIPE_DURATION = Duration.ofMillis(400);

    protected final WebDriver driver;
    protected final Waits wait;

    protected MobileScreen() {
        this.driver = DriverManager.getDriver();
        this.wait = new Waits(driver);
    }

    // --- Locators that mean the same thing on both platforms ---

    /** Cross-platform locator using {@code content-desc} on Android and accessibility identifier on iOS.*/
    protected static By accessibilityId(String value) {
        return AppiumBy.accessibilityId(value);
    }

    // --- Interactions ---

    @Step("Tap: {1}")
    protected void tap(By locator, String elementName) {
        wait.clickable(locator).click();
        log.info("Tapped [{}]", elementName);
    }

    @Step("Type '{1}' into: {2}")
    protected void type(By locator, String value, String elementName) {
        WebElement field = wait.clickable(locator);
        field.clear();
        field.sendKeys(value);
        log.info("Typed [{}] into [{}]", value, elementName);
    }

    /**
     * Immediate presence check without waiting, used for optional flow decisions.
     * Excluded from Allure reporting to avoid cluttering test logs during polling loops.
     */
    protected boolean isDisplayed(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    protected List<WebElement> findAll(By locator) {
        return driver.findElements(locator);
    }

    // --- Gestures ---

    /** Drags upward through the middle of the screen, which scrolls the panel under the finger. */
    @Step("Swipe up to reveal more of: {0}")
    protected void swipeUp(String contextName) {
        Dimension screen = driver.manage().window().getSize();
        int x = screen.getWidth() / 2;
        int startY = (int) (screen.getHeight() * 0.85);
        int endY = (int) (screen.getHeight() * 0.45);

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(SWIPE_DURATION, PointerInput.Origin.viewport(), x, endY))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        ((RemoteWebDriver) driver).perform(List.of(swipe));
        log.info("Swiped up on [{}]", contextName);
    }

    /**
     * Performs a press-and-hold action implemented by platform subclasses.
     * Enforces a strict hold duration to prevent the gesture from registering as a standard tap.
     */
    protected abstract void longPress(By locator, String elementName);

    // --- App lifecycle ---

    /** The package name or bundle id of the app under test, whichever this platform uses. */
    protected abstract String appId();

    /**
     * Restarts the app deterministically rather than navigating back via keypresses.
     * Avoids route-dependent back-button counts while confirming app data persistence (e.g., reading lists).
     */
    protected void restartApp() {
        InteractsWithApps app = (InteractsWithApps) driver;
        app.terminateApp(appId());
        app.activateApp(appId());
        log.info("Restarted [{}]", appId());
    }

    // --- Interruptions ---

    /**
     * Waits for an element while continuously clearing promotional overlays.
     * Folds promo dismissal directly into the polling loop to handle delayed
     * interruptions that appear mid-wait.
     */
    protected void awaitPastPromos(By locator, String elementName) {
        log.info("Waiting for [{}], clearing any promotion in the way", elementName);
        try {
            wait.getDriverWait().until(driver -> {
                dismissTransientPromos();
                List<WebElement> found = driver.findElements(locator);
                return !found.isEmpty() && found.getFirst().isDisplayed();
            });
        } catch (TimeoutException e) {
            throw new org.openqa.selenium.TimeoutException(
                    "Timed out waiting for " + elementName + " to be on screen with any promotion cleared", e);
        }

    }

    /** Clears whatever first-run interruptions this platform's build of the app shows. */
    protected abstract void dismissTransientPromos();
}
