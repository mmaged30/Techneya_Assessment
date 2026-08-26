package org.mobile.android;


import io.qameta.allure.Step;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mobile.interfaces.MainScreen;
import org.mobile.interfaces.OnboardingScreen;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Handles the optional first-run introductory flow.
 * Safely bypassed or completed depending on whether the app triggers it
 * after the test suite clears user data.
 */
@Slf4j
public class AndroidOnboardingScreen extends AndroidScreenBase implements OnboardingScreen {

    /** Shown when clearing app data invalidates a session. Answered with "Cancel". */
    private static final By loggedOutCancel = androidId("button2");

    private static final By skipButton = text("Skip");
    private static final By forwardButton = accessibilityId("Forward");
    private static final By nextButton = accessibilityId("Next");

    /** Present only once onboarding is finished, so it is the signal that the app is usable. */
    private static final By mainNavigation = id("main_nav_tab_container");

    /**
     * Limits onboarding navigation iterations to avoid infinite loops and hung suites
     * if the introduction flow changes.
     */
    private static final int MAX_ONBOARDING_PAGES = 8;

    @Override
    @Step("Complete the first-run flow")
    public MainScreen completeFirstRun() {
        dismissLoggedOutPrompt();
        skipIntroduction();
        wait.visible(mainNavigation);
        log.info("The app is on the main screen and ready");
        return new AndroidMainScreen();
    }

    private void dismissLoggedOutPrompt() {
        List<WebElement> cancel = findAll(loggedOutCancel);
        if (!cancel.isEmpty()) {
            cancel.getFirst().click();
            log.info("Dismissed the logged-out prompt left behind by clearing app data");
        }
    }

    /**
     * Pages forward until Skip appears, then takes it. Skip is only offered on the last page,
     * so this is the shortest route through that does not depend on the page count staying at
     * four - a fifth page would simply be paged through.
     */
    private void skipIntroduction() {
        for (int page = 0; page < MAX_ONBOARDING_PAGES; page++) {
            if (isDisplayed(mainNavigation)) {
                return;
            }
            if (isDisplayed(skipButton)) {
                tap(skipButton, "Skip button");
                return;
            }
            if (isDisplayed(forwardButton)) {
                tap(forwardButton, "Forward button");
            } else if (isDisplayed(nextButton)) {
                tap(nextButton, "Next button");
            } else {
                // No onboarding on screen at all, which is a legitimate state.
                return;
            }
        }
        throw new IllegalStateException("❌ The onboarding flow did not finish after "
                + MAX_ONBOARDING_PAGES + " pages. The introduction may have changed shape.");
    }
}
