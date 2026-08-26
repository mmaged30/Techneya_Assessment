package org.utils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * What this run actually exercised.
 * <p>
 * Exists to invert a dependency. Reporting previously asked the API and mobile layers what they
 * had done - a low-level concern reaching upward into two feature layers, and forcing both to
 * carry a counter that only the report cared about. Now each layer <em>records</em> here and
 * reporting <em>reads</em>, so neither layer knows a report exists.
 * <p>
 * Static because a footprint is genuinely run-scoped: one JVM is one run, and threading an
 * instance through every factory would be ceremony for a value that has exactly one meaning.
 */
public final class ExecutionFootprint {

    private static final AtomicBoolean apiExercised = new AtomicBoolean();
    private static final AtomicBoolean mobileExercised = new AtomicBoolean();

    private ExecutionFootprint() {
        throw new IllegalStateException("Utility class");
    }

    public static void recordApiExercised() {
        apiExercised.set(true);
    }

    public static void recordMobileExercised() {
        mobileExercised.set(true);
    }

    public static boolean wasApiExercised() {
        return apiExercised.get();
    }

    public static boolean wasMobileExercised() {
        return mobileExercised.get();
    }
}
