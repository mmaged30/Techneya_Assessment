package tests.mobile;

import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.mobile.base.Screens;
import org.mobile.interfaces.MainScreen;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.utils.DriverFactory;
import org.utils.DriverManager;

/**
 * Session lifecycle for mobile tests: one Appium session per test method, always ended.
 * <p>
 * A session per method rather than per class is deliberate. Reading lists are persistent app
 * state and the reset capability only takes effect when a session starts, so sharing a session
 * would let one test's list be visible to the next - the classic way a suite passes in isolation
 * and fails as a whole.
 * <p>
 * Extending this class is what gives a test a device. A test that does not extend it never
 * opens one, which is what replaced the tag-scoped hooks: the type system decides, not a string.
 */
@Slf4j
public abstract class BaseMobileTest {

    /**
     * TestNG injects the method about to run, which gives the session a name without any
     * plumbing - useful locally in the log, and the label BrowserStack shows on its dashboard.
     */
    @BeforeMethod(alwaysRun = true)
    public void startSession(Method method) {
        DriverManager.setDriver(DriverFactory.create(method.getName()));
    }

    /**
     * Failure evidence is NOT captured here - {@code listeners.FailureListener} does it, because
     * {@code onTestFailure} fires before this method and therefore while the session is still
     * alive. Teardown only has to end the session.
     */
    @AfterMethod(alwaysRun = true)
    public void endSession(ITestResult result) {
        DriverManager.unloadDriver();
        log.info("{} {}", result.isSuccess() ? "✅ PASSED" : "❌ FINISHED", result.getName());
    }

    /**
     * Gets the app past its first-run flow and onto the main screen.
     * <p>
     * {@link Screens#onboarding()} is the one place a platform is resolved. Everything a test
     * touches from here on is an interface, so no test knows whether it drives Android or iOS.
     */
    protected MainScreen launchApp() {
        return Screens.onboarding().completeFirstRun();
    }
}
