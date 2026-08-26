package org.utils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Run-scoped registry tracking test execution coverage across API and mobile layers.
 * Inverts reporting dependencies by allowing feature layers to publish metrics passively
 * without exposing reporting concerns to business logic.
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
