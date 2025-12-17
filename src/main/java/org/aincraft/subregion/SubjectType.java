package org.aincraft.subregion;

/**
 * Type of permission subject (player, role, or guild relationship).
 */
public enum SubjectType {
    /**
     * Permission applies to a specific player.
     */
    PLAYER,

    /**
     * Permission applies to all members with a specific role.
     */
    ROLE,

    /**
     * Permission applies to all members of allied guilds.
     */
    GUILD_ALLY,

    /**
     * Permission applies to all members of enemy guilds.
     */
    GUILD_ENEMY,

    /**
     * Permission applies to all players not in the guild and without a relationship.
     */
    GUILD_OUTSIDER
}
