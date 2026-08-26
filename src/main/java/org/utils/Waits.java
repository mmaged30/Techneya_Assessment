package org.utils;

import java.time.Duration;
import java.util.function.Function;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The only place a {@code WebDriverWait} is built, so {@code timeout.explicit} is read in
 * one spot and no screen object invents its own wait - or a {@code Thread.sleep}.
 * <p>
 * Driver-agnostic on purpose: {@code AppiumDriver} implements {@link WebDriver}, so the same
 * conditions serve a mobile session unchanged.
 */
public class Waits {

    /** Poll interval for every wait the framework performs. */
    private static final Duration POLLING_INTERVAL = Duration.ofMillis(500);

    private final WebDriverWait wait;

    public Waits(WebDriver driver) {
        this.wait = new WebDriverWait(
                driver,
                ConfigManager.getTimeout(),
                POLLING_INTERVAL
        );
    }

    /** Element is rendered and has a non-zero size. */
    public WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /** Element is visible AND enabled. */
    public WebElement clickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /** Element is hidden or gone. The condition for spinners, overlays and dismissed dialogs. */
    public boolean invisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Waits until the number of matches settles on {@code expected} and returns whether it did.
     * <p>
     * This is the condition a duplicate check needs: asserting a count immediately after an
     * action races the list's own re-render, so "count == 1" could pass on a list that has not
     * finished adding the second entry yet. Waiting for the count makes the assertion honest
     * in both directions - it gives the app the full timeout to produce a duplicate before
     * concluding that none appeared.
     */
    public boolean countSettlesAt(By locator, int expected) {
        try {
            return wait.until(ExpectedConditions.numberOfElementsToBe(locator, expected)) != null;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits for an element and reports whether it arrived, instead of throwing.
     * <p>
     * For assertions that need to distinguish "not there" from "not there yet". A plain
     * presence check would answer immediately and so could report a result list as empty purely
     * because the request behind it had not come back.
     */
    public boolean appears(By locator) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits on a caller-supplied condition, using the framework's single timeout and polling
     * interval. Exists so a screen that needs something these methods do not express still does
     * not build a {@code WebDriverWait} of its own.
     * <p>
     * The description is required rather than optional: a lambda has no readable name, so
     * without one a timeout reports the condition as {@code Lambda$$0x00007f...} and tells
     * whoever reads the failure nothing at all.
     * <p>
     * The description is attached by rethrowing rather than through {@code withMessage}, which
     * mutates the wait it is called on. Sharing one wait per screen, that leaks: the message set
     * here would still be attached to every later timeout on the same screen, so an unrelated
     * failure would be reported under this description and send the reader to the wrong place.
     */
    public <T> T until(String description, Function<WebDriver, T> condition) {
        try {
            return wait.until(condition);
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new org.openqa.selenium.TimeoutException("Timed out waiting for " + description, e);
        }
    }

}
