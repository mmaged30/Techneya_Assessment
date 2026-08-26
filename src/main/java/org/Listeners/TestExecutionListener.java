package org.listeners;

import org.testng.ITestListener;
import org.testng.ITestResult;
import org.utils.ConfigManager;
import org.utils.ScreenshotUtils;

/**
 * Captures failure evidence via {@link ITestListener} before the driver quits in {@code @AfterMethod}.
 * Safe for API tests: returns immediately if no driver is bound to the thread.
 */
public class TestExecutionListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        if (ConfigManager.getBooleanProperty("screenshot.on.failure")) {
            ScreenshotUtils.captureFailure(result.getName());
        }
    }
}
