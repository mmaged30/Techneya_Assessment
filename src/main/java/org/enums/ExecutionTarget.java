package org.enums;

import java.util.Arrays;

/**
 * Where a mobile session actually runs.
 * <p>
 * Selected with {@code execution.target}, which defaults to {@link #LOCAL} so a developer with
 * an emulator and a local Appium server needs no flags at all. Everything a remote grid needs
 * that a local run does not - credentials, a hub URL, a session name - is added only when the
 * target is remote, and every capability that is meaningless remotely is skipped.
 * <p>
 * An enum rather than a boolean: a second grid (Sauce Labs, LambdaTest, a self-hosted Selenium
 * Grid) becomes a constant here and one branch, not an inverted flag nobody can read.
 */
public enum ExecutionTarget {

    LOCAL,
    BROWSERSTACK;

    /** Accepts any casing or surrounding whitespace, e.g. local / Local / BROWSERSTACK. */
    public static ExecutionTarget fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("❌ CRITICAL: No execution target was supplied. "
                    + "Set 'execution.target' in config/global.properties or pass "
                    + "-Dexecution.target=local");
        }
        try {
            return ExecutionTarget.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("❌ CRITICAL: Unsupported execution target '" + value
                    + "'. Supported targets are: " + Arrays.toString(values()), e);
        }
    }

    /** Whether the session is created on someone else's machine. */
    public boolean isRemote() {
        return this != LOCAL;
    }
}
