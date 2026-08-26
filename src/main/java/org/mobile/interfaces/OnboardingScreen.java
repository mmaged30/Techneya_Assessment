package org.mobile.interfaces;

/**
 * The app's first-run flow, whatever shape a platform gives it.
 * <p>
 * Reached on every scenario because each one starts from cleared app data.
 */
public interface OnboardingScreen {

    /** Gets past the first-run flow, tolerating its absence, and lands on the main screen. */
    MainScreen completeFirstRun();
}
