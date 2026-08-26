package org.utils;

import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * The only place a {@code WebDriverWait} is built, so {@code timeout.explicit} is read in
 * one spot and no page object invents its own wait - or a {@code Thread.sleep}.
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

    public WebDriverWait getDriverWait() {
        return this.wait;
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
     * Polls until element match count stabilizes at {@code expected} within the active timeout.
     * Prevents race conditions against list re-renders by verifying the UI count has settled
     * before asserting, ensuring accurate validation for dynamic elements like duplicates.
     * @param expected total expected element count to wait for
     * @return {@code true} if match count reaches {@code expected} before timeout; {@code false} otherwise
     */
    public boolean countSettlesAt(By locator, int expected) {
        try {
            return wait.until(ExpectedConditions.numberOfElementsToBe(locator, expected)) != null;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Polls for element presence within the configured timeout, returning a boolean status instead of throwing.
     * Distinguishes true absence ("not there") from initial loading delays ("not there yet"),
     * preventing false-negative checks on asynchronous or network-dependent UI state.
     * @return {@code true} if the element becomes visible before timeout; {@code false} otherwise
     */
    public boolean appears(By locator) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

}
