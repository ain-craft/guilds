package org.aincraft.util;

import java.util.Objects;

/**
 * Utility class for common validation operations.
 * Reduces boilerplate for null checks and other validation patterns.
 */
public final class ValidationUtils {

    private ValidationUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Requires that the specified object is not null.
     *
     * @param obj the object to check
     * @param paramName the parameter name for the error message
     * @param <T> the type of the object
     * @return the object if not null
     * @throws NullPointerException if obj is null
     */
    public static <T> T requireNonNull(T obj, String paramName) {
        return Objects.requireNonNull(obj, paramName + " cannot be null");
    }

    /**
     * Requires that the specified string is not null and not blank.
     *
     * @param str the string to check
     * @param paramName the parameter name for the error message
     * @return the string if valid
     * @throws NullPointerException if str is null
     * @throws IllegalArgumentException if str is blank
     */
    public static String requireNotBlank(String str, String paramName) {
        requireNonNull(str, paramName);
        if (str.isBlank()) {
            throw new IllegalArgumentException(paramName + " cannot be blank");
        }
        return str;
    }

    /**
     * Requires that the specified number is positive.
     *
     * @param value the value to check
     * @param paramName the parameter name for the error message
     * @return the value if positive
     * @throws IllegalArgumentException if value is not positive
     */
    public static int requirePositive(int value, String paramName) {
        if (value <= 0) {
            throw new IllegalArgumentException(paramName + " must be positive");
        }
        return value;
    }

    /**
     * Requires that the specified number is non-negative.
     *
     * @param value the value to check
     * @param paramName the parameter name for the error message
     * @return the value if non-negative
     * @throws IllegalArgumentException if value is negative
     */
    public static int requireNonNegative(int value, String paramName) {
        if (value < 0) {
            throw new IllegalArgumentException(paramName + " cannot be negative");
        }
        return value;
    }
}
