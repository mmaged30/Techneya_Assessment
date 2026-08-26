package org.Listeners;

import org.testng.ITestListener;
import org.testng.ITestResult;
import org.utils.ConfigManager;
import org.utils.ScreenshotUtils;

/**
 * Captures failure evidence while the session is still alive.
 * <p>
 * {@code onTestFailure} fires before the {@code @AfterMethod} that quits the driver, which is
 * exactly the ordering a screenshot needs. Under Cucumber this took a hook-ordering trick;
 * TestNG gives it for free, which is why this is an {@link ITestListener} rather than more
 * teardown code.
 * <p>
 * Harmless on API tests: there is no driver bound to the thread, and the capture returns
 * immediately when that is the case.
 */
public class FailureListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        if (ConfigManager.getBooleanProperty("screenshot.on.failure")) {
            ScreenshotUtils.captureFailure(result.getName());
        }
    }
}
