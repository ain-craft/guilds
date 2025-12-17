package org.aincraft.commands;

import net.kyori.adventure.text.Component;

/**
 * Centralized error and info messages for guild commands.
 * Reduces duplication of message strings across command components.
 */
public enum GuildMessages {
    // Error messages
    NOT_IN_GUILD("You are not in a guild"),
    GUILD_NOT_FOUND("Guild not found: %s"),
    NO_PERMISSION("You don't have permission to %s"),
    ALREADY_IN_GUILD("You are already in a guild"),
    PLAYER_ONLY("Only players can use this command"),
    PLAYER_NOT_FOUND("Player not found: %s"),
    NOT_GUILD_OWNER("Only the guild owner can do this"),
    GUILD_FULL("Guild is full"),
    CHUNK_NOT_OWNED("This chunk is not owned by your guild"),
    CANNOT_UNCLAIM_HOMEBLOCK("Cannot unclaim the homeblock"),

    // Success messages
    GUILD_CREATED("Guild created successfully!"),
    GUILD_DISBANDED("Your guild has been disbanded!"),
    LEFT_GUILD("You have left your guild!"),
    JOINED_GUILD("Successfully joined guild: %s"),
    PLAYER_KICKED("Player kicked from guild!"),
    CHUNK_CLAIMED("Claimed chunk at %s, %s"),
    CHUNK_UNCLAIMED("Unclaimed chunk at %s, %s"),
    SPAWN_SET("Guild spawn set!"),
    SPAWN_CLEARED("Guild spawn cleared!");

    private final String message;

    GuildMessages(String message) {
        this.message = message;
    }

    /**
     * Gets the raw message template.
     *
     * @return the message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Formats the message with optional arguments.
     *
     * @param args arguments to insert into the message
     * @return formatted string
     */
    public String format(Object... args) {
        if (args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }

    /**
     * Formats the message as an error Component.
     *
     * @param args arguments to insert into the message
     * @return formatted error Component
     */
    public Component formatError(Object... args) {
        return MessageFormatter.format(MessageFormatter.ERROR, format(args));
    }

    /**
     * Formats the message as a success Component.
     *
     * @param args arguments to insert into the message
     * @return formatted success Component
     */
    public Component formatSuccess(Object... args) {
        return MessageFormatter.deserialize("<green>" + format(args) + "</green>");
    }

    /**
     * Formats the message as an info Component.
     *
     * @param args arguments to insert into the message
     * @return formatted info Component
     */
    public Component formatInfo(Object... args) {
        return MessageFormatter.deserialize("<gray>" + format(args) + "</gray>");
    }
}
