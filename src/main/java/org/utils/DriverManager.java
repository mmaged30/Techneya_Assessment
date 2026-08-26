package org.utils;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;

@Slf4j
public final class DriverManager {

    private static final ThreadLocal<WebDriver> threadLocalDriver = new ThreadLocal<>();

    private DriverManager() {
        throw new IllegalStateException("Utility class");
    }

    public static void setDriver(WebDriver driver) {
        threadLocalDriver.set(driver);
    }

    /** Fails fast with a readable message rather than an NPE deeper in a screen object. */
    public static WebDriver getDriver() {
        WebDriver driver = threadLocalDriver.get();
        if (driver == null) {
            throw new IllegalStateException("❌ CRITICAL: No driver is bound to thread '"
                    + Thread.currentThread().getName()
                    + "'. A @mobile scenario must run through MobileHooks, which starts the session.");
        }
        return driver;
    }

    /** For components shared with API scenarios, which have no driver to photograph. */
    public static boolean hasDriver() {
        return threadLocalDriver.get() != null;
    }

    /** Ends the session. The remove() matters: without it the thread pool leaks drivers. */
    public static void unloadDriver() {
        WebDriver driver = threadLocalDriver.get();
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
            log.info("Mobile session closed for thread [{}]", Thread.currentThread().getName());
        } catch (Exception e) {
            log.warn("Session was already dead on quit(). Continuing teardown.", e);
        } finally {
            threadLocalDriver.remove();
        }
    }
}
