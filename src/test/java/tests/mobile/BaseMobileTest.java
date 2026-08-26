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


@Slf4j
public abstract class BaseMobileTest {

    @BeforeMethod(alwaysRun = true)
    public void startSession(Method method) {
        DriverManager.setDriver(DriverFactory.create(method.getName()));
    }


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
