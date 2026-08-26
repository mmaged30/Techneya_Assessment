package org.mobile.base;

import org.enums.Platform;
import org.mobile.android.AndroidOnboardingScreen;
import org.mobile.interfaces.OnboardingScreen;
import org.utils.ConfigManager;

/**
 * The one place a platform decides which screen implementations a run uses.
 * <p>
 * Only the entry point needs resolving. Once a test holds a screen, every later screen is
 * produced by the one it navigates from - an Android screen builds Android screens and returns
 * them as interfaces - so this stays a single method rather than a catalogue of every screen.
 * <p>
 * The switch is exhaustive over {@link Platform}: adding a platform will not compile until it is
 * handled here, which fails earlier and more loudly than a registry that returns null at runtime.
 */
public final class Screens {

    private Screens() {
        throw new IllegalStateException("Utility class");
    }

    /** The first screen of a scenario: the app's first-run flow on the active platform. */
    public static OnboardingScreen onboarding() {
        Platform platform = Platform.fromString(ConfigManager.getProperty("platform"));

        return switch (platform) {
            case ANDROID -> new AndroidOnboardingScreen();
            case IOS -> throw new UnsupportedOperationException("""
                    iOS screens are not implemented.

                    The architecture is ready for them: IosScreenBase supplies the gestures, the
                    bundle id and the locator helpers, and IosSearchScreen is a worked reference
                    for what a screen looks like. What is missing is the screens themselves -
                    IosOnboardingScreen, IosMainScreen, IosArticleScreen,
                    IosSaveToReadingListSheet, IosReadingListsScreen and
                    IosReadingListDetailScreen - each implementing the interface of the same name
                    in org.mobile.interfaces, plus returning IosOnboardingScreen from here.

                    They need a macOS machine with Xcode to write against and verify, which is
                    why they are absent rather than guessed.""");
        };
    }
}
