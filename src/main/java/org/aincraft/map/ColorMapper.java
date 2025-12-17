package org.aincraft.map;

/**
 * Interface for mapping guilds to display colors.
 * Abstracts the color assignment logic for testability and extensibility.
 */
public interface ColorMapper {

    /**
     * Gets the display color for a guild.
     *
     * @param guildId the guild ID
     * @param guildColor the guild's configured color (may be null)
     * @return the color to use for display (hex format like "#RRGGBB")
     */
    String getColorForGuild(String guildId, String guildColor);

    /**
     * Gets the display color for a guild using only the guild ID.
     * Uses a deterministic algorithm to generate a color.
     *
     * @param guildId the guild ID
     * @return the generated color (hex format like "#RRGGBB")
     */
    String getGeneratedColor(String guildId);

    /**
     * Clears any cached colors for a guild.
     *
     * @param guildId the guild ID
     */
    void clearCache(String guildId);
}
