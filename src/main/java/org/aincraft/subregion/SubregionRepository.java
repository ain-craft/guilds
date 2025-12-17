package org.aincraft.subregion;

import org.aincraft.ChunkKey;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for managing subregion persistence.
 */
public interface SubregionRepository {

    /**
     * Saves a subregion (insert or update).
     */
    void save(Subregion region);

    /**
     * Deletes a subregion by ID.
     */
    void delete(String regionId);

    /**
     * Deletes all subregions belonging to a guild.
     */
    void deleteAllByGuild(String guildId);

    /**
     * Finds a subregion by its ID.
     */
    Optional<Subregion> findById(String regionId);

    /**
     * Finds a subregion by guild and name.
     */
    Optional<Subregion> findByGuildAndName(String guildId, String name);

    /**
     * Finds all subregions belonging to a guild.
     */
    List<Subregion> findByGuild(String guildId);

    /**
     * Finds all subregions that contain a specific location.
     */
    List<Subregion> findByLocation(Location loc);

    /**
     * Finds all subregions that intersect with any of the given chunks.
     */
    List<Subregion> findOverlappingChunks(Set<ChunkKey> chunks);

    /**
     * Finds all subregions that intersect with a single chunk.
     */
    List<Subregion> findOverlappingChunk(ChunkKey chunk);

    /**
     * Gets the count of subregions owned by a guild.
     */
    int getCountByGuild(String guildId);

    /**
     * Gets the total volume of all subregions of a specific type for a guild.
     *
     * @param guildId the guild ID
     * @param typeId  the type ID
     * @return total volume in blocks, or 0 if none found
     */
    long getTotalVolumeByGuildAndType(String guildId, String typeId);
}
