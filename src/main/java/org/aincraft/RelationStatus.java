package org.aincraft;

/**
 * Status of a guild relationship.
 */
public enum RelationStatus {
    /**
     * Alliance request sent but not yet accepted.
     */
    PENDING,

    /**
     * Relationship is active (accepted alliance or active enemy declaration).
     */
    ACTIVE,

    /**
     * Alliance request was rejected.
     */
    REJECTED,

    /**
     * Relationship was ended/cancelled.
     */
    CANCELLED
}
