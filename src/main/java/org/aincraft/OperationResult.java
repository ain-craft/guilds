package org.aincraft;

import java.util.Objects;

/**
 * Generic base class for operation results.
 * Provides a standardized way to represent success/failure with optional reason.
 *
 * @param <T> the status enum type
 */
public class OperationResult<T extends Enum<T>> {
    private final T status;
    private final String reason;

    protected OperationResult(T status, String reason) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.reason = reason;
    }

    /**
     * Gets the status of this operation.
     *
     * @return the status
     */
    public T getStatus() {
        return status;
    }

    /**
     * Gets the reason for failure (if any).
     *
     * @return the reason, or null if successful
     */
    public String getReason() {
        return reason;
    }

    /**
     * Checks if this operation succeeded.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return reason == null;
    }

    /**
     * Creates a successful result.
     *
     * @param status the status enum value
     * @param <T> the status type
     * @return a successful result
     */
    public static <T extends Enum<T>> OperationResult<T> success(T status) {
        return new OperationResult<>(status, null);
    }

    /**
     * Creates a failed result.
     *
     * @param status the status enum value
     * @param reason the failure reason
     * @param <T> the status type
     * @return a failed result
     */
    public static <T extends Enum<T>> OperationResult<T> failure(T status, String reason) {
        return new OperationResult<>(status, Objects.requireNonNull(reason, "Reason cannot be null"));
    }

    @Override
    public String toString() {
        return "OperationResult{" +
                "status=" + status +
                ", reason='" + reason + '\'' +
                ", success=" + isSuccess() +
                '}';
    }
}
