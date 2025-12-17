package org.aincraft.claim;

/**
 * Immutable state representing a player's auto-claim configuration.
 * Uses a tristate mode: OFF, AUTO_CLAIM, or AUTO_UNCLAIM.
 */
public record AutoClaimState(AutoClaimMode mode) {

    /**
     * Creates an OFF state (no auto action).
     */
    public static AutoClaimState off() {
        return new AutoClaimState(AutoClaimMode.OFF);
    }

    /**
     * Creates an AUTO_CLAIM state (automatically claim wilderness chunks).
     */
    public static AutoClaimState autoClaim() {
        return new AutoClaimState(AutoClaimMode.AUTO_CLAIM);
    }

    /**
     * Creates an AUTO_UNCLAIM state (automatically unclaim owned chunks).
     */
    public static AutoClaimState autoUnclaim() {
        return new AutoClaimState(AutoClaimMode.AUTO_UNCLAIM);
    }
}
