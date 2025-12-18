package org.aincraft.progression;

import java.util.Objects;

/**
 * Result pattern for level-up attempts.
 * Indicates success or specific failure reason.
 */
public final class LevelUpResult {
    private final boolean success;
    private final String errorMessage;
    private final int newLevel;

    private LevelUpResult(boolean success, String errorMessage, int newLevel) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.newLevel = newLevel;
    }

    /**
     * Creates a successful level-up result.
     *
     * @param newLevel the new level after level-up
     * @return success result
     */
    public static LevelUpResult success(int newLevel) {
        return new LevelUpResult(true, null, newLevel);
    }

    /**
     * Creates a failure result with error message.
     *
     * @param errorMessage the error message
     * @return failure result
     */
    public static LevelUpResult failure(String errorMessage) {
        Objects.requireNonNull(errorMessage, "Error message cannot be null");
        return new LevelUpResult(false, errorMessage, 0);
    }

    /**
     * Creates a failure result for insufficient XP.
     *
     * @param current the current XP
     * @param required the required XP
     * @return failure result
     */
    public static LevelUpResult insufficientXp(long current, long required) {
        return failure(String.format("Insufficient XP: %d / %d", current, required));
    }

    /**
     * Creates a failure result for insufficient materials.
     *
     * @param material the missing material
     * @param current the current amount
     * @param required the required amount
     * @return failure result
     */
    public static LevelUpResult insufficientMaterials(String material, int current, int required) {
        return failure(String.format("Insufficient %s: %d / %d", material, current, required));
    }

    /**
     * Creates a failure result for no permission.
     *
     * @return failure result
     */
    public static LevelUpResult noPermission() {
        return failure("You don't have permission to level up the guild");
    }

    /**
     * Creates a failure result for max level reached.
     *
     * @return failure result
     */
    public static LevelUpResult maxLevelReached() {
        return failure("Guild has reached maximum level");
    }

    /**
     * Creates a failure result for no vault.
     *
     * @return failure result
     */
    public static LevelUpResult noVault() {
        return failure("Guild must have a vault to level up");
    }

    /**
     * Checks if the level-up was successful.
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Gets the error message if failed.
     *
     * @return the error message, or null if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the new level if successful.
     *
     * @return the new level, or 0 if failed
     */
    public int getNewLevel() {
        return newLevel;
    }

    @Override
    public String toString() {
        if (success) {
            return "LevelUpResult{success=true, newLevel=" + newLevel + "}";
        } else {
            return "LevelUpResult{success=false, error='" + errorMessage + "'}";
        }
    }
}
