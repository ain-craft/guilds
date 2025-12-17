package org.aincraft.subregion;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing region-scoped roles.
 */
public interface RegionRoleRepository {
    /**
     * Saves a region role (insert or update).
     */
    void save(RegionRole role);

    /**
     * Deletes a role by ID.
     */
    void delete(String roleId);

    /**
     * Finds a role by its ID.
     */
    Optional<RegionRole> findById(String roleId);

    /**
     * Finds all roles for a specific region.
     */
    List<RegionRole> findByRegion(String regionId);

    /**
     * Finds a role by region and name.
     */
    Optional<RegionRole> findByRegionAndName(String regionId, String name);

    /**
     * Deletes all roles for a region (cleanup when region is deleted).
     */
    void deleteAllByRegion(String regionId);
}
