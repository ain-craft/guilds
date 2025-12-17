package org.aincraft.storage;

import org.aincraft.GuildRelationship;
import org.aincraft.RelationType;
import org.aincraft.RelationStatus;

import java.util.List;
import java.util.Optional;

/**
 * Manages guild relationship storage and retrieval.
 * Single Responsibility: Guild relationship persistence only.
 */
public interface GuildRelationshipRepository {
    /**
     * Saves a guild relationship (insert or update).
     *
     * @param relationship the relationship to save
     */
    void save(GuildRelationship relationship);

    /**
     * Deletes a relationship by ID.
     *
     * @param relationshipId the relationship ID
     */
    void delete(String relationshipId);

    /**
     * Finds a relationship by its ID.
     *
     * @param id the relationship ID
     * @return Optional containing the relationship, or empty if not found
     */
    Optional<GuildRelationship> findById(String id);

    /**
     * Finds an active relationship between two guilds (bidirectional).
     * Checks both source→target and target→source.
     *
     * @param guildId1 first guild ID
     * @param guildId2 second guild ID
     * @return Optional containing the relationship, or empty if not found
     */
    Optional<GuildRelationship> findRelationship(String guildId1, String guildId2);

    /**
     * Finds all relationships involving a guild (as source or target).
     *
     * @param guildId the guild ID
     * @return list of relationships
     */
    List<GuildRelationship> findAllByGuild(String guildId);

    /**
     * Finds relationships of a specific type and status for a guild.
     *
     * @param guildId the guild ID
     * @param type the relation type
     * @param status the relation status
     * @return list of matching relationships
     */
    List<GuildRelationship> findByType(String guildId, RelationType type, RelationStatus status);

    /**
     * Deletes all relationships involving a guild.
     *
     * @param guildId the guild ID
     */
    void deleteAllByGuild(String guildId);
}
