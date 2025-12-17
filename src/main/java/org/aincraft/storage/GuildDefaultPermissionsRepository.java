package org.aincraft.storage;

import org.aincraft.GuildDefaultPermissions;
import org.aincraft.subregion.SubjectType;

import java.util.Optional;

/**
 * Manages guild default permissions storage and retrieval.
 * Single Responsibility: Guild default permissions persistence only.
 */
public interface GuildDefaultPermissionsRepository {
    /**
     * Saves guild default permissions (insert or update).
     *
     * @param permissions the permissions to save
     */
    void save(GuildDefaultPermissions permissions);

    /**
     * Finds permissions for a specific guild.
     *
     * @param guildId the guild ID
     * @return Optional containing the permissions, or empty if not found
     */
    Optional<GuildDefaultPermissions> findByGuildId(String guildId);

    /**
     * Deletes permissions for a guild.
     *
     * @param guildId the guild ID
     */
    void delete(String guildId);

    /**
     * Gets permissions for a specific relationship type, returning defaults if not found.
     *
     * @param guildId the guild ID
     * @param subjectType the relationship type (GUILD_ALLY, GUILD_ENEMY, GUILD_OUTSIDER)
     * @return the permissions bitfield, or 0 if guild not found
     */
    int getPermissions(String guildId, SubjectType subjectType);
}
