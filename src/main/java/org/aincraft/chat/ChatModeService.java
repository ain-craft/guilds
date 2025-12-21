package org.aincraft.chat;

import com.google.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for tracking player chat modes (PUBLIC, GUILD, ALLY).
 * Chat modes are session-specific and not persisted.
 */
@Singleton
public class ChatModeService {
    private final Map<UUID, ChatMode> playerChatModes = new ConcurrentHashMap<>();

    public enum ChatMode {
        PUBLIC, GUILD, ALLY, OFFICER
    }

    /**
     * Gets the current chat mode for a player.
     * Defaults to PUBLIC if not set.
     *
     * @param playerId the player UUID
     * @return the current chat mode
     */
    public ChatMode getMode(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        return playerChatModes.getOrDefault(playerId, ChatMode.PUBLIC);
    }

    /**
     * Sets the chat mode for a player.
     *
     * @param playerId the player UUID
     * @param mode the chat mode to set
     */
    public void setMode(UUID playerId, ChatMode mode) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(mode, "Chat mode cannot be null");

        if (mode == ChatMode.PUBLIC) {
            playerChatModes.remove(playerId);
        } else {
            playerChatModes.put(playerId, mode);
        }
    }

    /**
     * Toggles between PUBLIC and the target mode.
     * If currently in target mode, switches to PUBLIC.
     * If currently in PUBLIC or different mode, switches to target mode.
     *
     * @param playerId the player UUID
     * @param targetMode the mode to toggle to (GUILD or ALLY)
     * @return the new mode after toggling
     */
    public ChatMode toggleMode(UUID playerId, ChatMode targetMode) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(targetMode, "Target mode cannot be null");

        if (targetMode == ChatMode.PUBLIC) {
            throw new IllegalArgumentException("Cannot toggle to PUBLIC mode");
        }

        ChatMode currentMode = getMode(playerId);

        if (currentMode == targetMode) {
            // Already in target mode, switch to PUBLIC
            setMode(playerId, ChatMode.PUBLIC);
            return ChatMode.PUBLIC;
        } else {
            // Switch to target mode
            setMode(playerId, targetMode);
            return targetMode;
        }
    }

    /**
     * Clears the chat mode for a player.
     * Called on player logout.
     *
     * @param playerId the player UUID
     */
    public void clearMode(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        playerChatModes.remove(playerId);
    }
}
