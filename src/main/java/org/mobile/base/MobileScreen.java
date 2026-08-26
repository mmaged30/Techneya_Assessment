package org.mobile.base;


import io.appium.java_client.AppiumBy;
import io.appium.java_client.InteractsWithApps;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.utils.DriverManager;
import org.utils.Waits;

/**
 * What every screen can do on every platform: find, tap, type, swipe, wait, restart.
 * <p>
 * Only genuinely portable behaviour lives here. The two things that are not portable are
 * declared abstract rather than branched on - the platform base class supplies them:
 * <ul>
 *     <li>{@link #longPress} - Android and iOS expose different driver gestures;</li>
 *     <li>{@link #dismissTransientPromos} - the interruptions are per-platform and per-app;</li>
 *     <li>{@link #appId} - a package name on Android, a bundle id on iOS.</li>
 * </ul>
 * That is a template method rather than a {@code Gestures} collaborator on purpose: exactly one
 * interaction differs, and an interface plus two implementations to carry a single method would
 * be more machinery than the difference is worth.
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

    /** Accessibility id: {@code content-desc} on Android, the accessibility identifier on iOS. */
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
     * An immediate presence check, deliberately without a wait - it answers "is this on screen
     * now", which is what an optional-flow decision needs.
     * <p>
     * Not an Allure step: it is called repeatedly inside loops, and recording each call buries
     * the steps that matter under dozens of empty ones.
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
     * A press-and-hold. Android and iOS reach this through different driver gestures, so the
     * platform base supplies it.
     * <p>
     * The hold duration is the gesture's own definition, not a wait for the app to catch up:
     * a press too short is a tap, and on a list row that opens the article instead of its menu.
     */
    protected abstract void longPress(By locator, String elementName);

    // --- App lifecycle ---

    /** The package name or bundle id of the app under test, whichever this platform uses. */
    protected abstract String appId();

    /**
     * Restarts the app.
     * <p>
     * Deliberately not done by pressing back. Each part of the app runs in its own activity or
     * view controller, so the number of presses varies with the route taken, and the presses are
     * not uniform either - back on a search screen may only close the keyboard, while back on
     * the main screen leaves the app altogether. Counting presses therefore either stops short
     * or walks out to the launcher, and both were observed before this replaced it.
     * <p>
     * Restarting is deterministic in one step and models something a user genuinely does. It
     * costs nothing in coverage: reading lists live in the app's database, so a list created
     * before the restart is still there afterwards - which this incidentally proves.
     */
    protected void restartApp() {
        InteractsWithApps app = (InteractsWithApps) driver;
        app.terminateApp(appId());
        app.activateApp(appId());
        log.info("Restarted [{}]", appId());
    }

    // --- Interruptions ---

    /**
     * Waits for an element, clearing interruptions as they appear.
     * <p>
     * Dismissing once up front is not enough: a promotion can arrive a moment after the screen
     * beneath it has already rendered, so a single sweep can run just before the thing it was
     * meant to clear. Folding the sweep into the polling means the wait handles a promotion
     * whenever it turns up inside the timeout.
     */
    protected void awaitPastPromos(By locator, String elementName) {
        log.info("Waiting for [{}], clearing any promotion in the way", elementName);
        wait.until(elementName + " to be on screen with any promotion cleared", ignored -> {
            dismissTransientPromos();
            List<WebElement> found = driver.findElements(locator);
            return !found.isEmpty() && found.getFirst().isDisplayed();
        });
    }

    /** Clears whatever first-run interruptions this platform's build of the app shows. */
    protected abstract void dismissTransientPromos();
}
