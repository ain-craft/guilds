package org.aincraft.claim;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player auto-claim mode state.
 * Thread-safe using ConcurrentHashMap for concurrent player access.
 * Supports tristate mode: OFF, AUTO_CLAIM, or AUTO_UNCLAIM.
 */
public class AutoClaimManager {
    private final Map<UUID, AutoClaimMode> playerAutoMode = new ConcurrentHashMap<>();

    /**
     * Sets the auto-claim mode for a player.
     *
     * @param playerId the player's UUID
     * @param mode the mode to set
     */
    public void setMode(UUID playerId, AutoClaimMode mode) {
        playerAutoMode.put(playerId, mode);
    }

    /**
     * Gets the current auto-claim mode for a player.
     *
     * @param playerId the player's UUID
     * @return the current mode, or OFF if not set
     */
    public AutoClaimMode getMode(UUID playerId) {
        return playerAutoMode.getOrDefault(playerId, AutoClaimMode.OFF);
    }

    /**
     * Disables auto mode for a player (sets to OFF).
     * Used when auto operations encounter failures.
     *
     * @param playerId the player's UUID
     */
    public void disable(UUID playerId) {
        playerAutoMode.put(playerId, AutoClaimMode.OFF);
    }

    /**
     * Clears all tracked auto-claim state. Used for cleanup on plugin disable.
     */
    public void clearAll() {
        playerAutoMode.clear();
    }
}
