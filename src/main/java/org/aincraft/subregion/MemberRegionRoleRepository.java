package org.aincraft.subregion;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing member-to-region-role assignments.
 */
public interface MemberRegionRoleRepository {
    /**
     * Assigns a region role to a member.
     */
    void assignRole(String regionId, UUID playerId, String roleId);

    /**
     * Unassigns a region role from a member.
     */
    void unassignRole(String regionId, UUID playerId, String roleId);

    /**
     * Gets all region role IDs assigned to a member in a region.
     */
    List<String> getMemberRoleIds(String regionId, UUID playerId);

    /**
     * Gets all member UUIDs that have a specific region role.
     */
    List<UUID> getMembersWithRole(String roleId);

    /**
     * Checks if a member has a specific region role assigned.
     */
    boolean hasMemberRole(String regionId, UUID playerId, String roleId);

    /**
     * Removes all region role assignments for a member in a region.
     */
    void removeAllMemberRoles(String regionId, UUID playerId);

    /**
     * Removes all assignments for a specific role (for role deletion).
     */
    void removeAllByRole(String roleId);

    /**
     * Removes all role assignments for a region (for region deletion).
     */
    void removeAllByRegion(String regionId);
}
