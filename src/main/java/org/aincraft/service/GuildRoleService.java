package org.aincraft.service;

import com.google.inject.Inject;
import org.aincraft.Guild;
import org.aincraft.GuildRole;
import org.aincraft.MemberPermissions;
import org.aincraft.storage.GuildRepository;
import org.aincraft.storage.GuildRoleRepository;
import org.aincraft.storage.MemberRoleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing guild roles.
 * Single Responsibility: Role CRUD and assignment operations.
 */
public class GuildRoleService {
    private final GuildRoleRepository roleRepository;
    private final MemberRoleRepository memberRoleRepository;
    private final GuildRepository guildRepository;

    @Inject
    public GuildRoleService(GuildRoleRepository roleRepository,
                            MemberRoleRepository memberRoleRepository,
                            GuildRepository guildRepository) {
        this.roleRepository = Objects.requireNonNull(roleRepository, "RoleRepository cannot be null");
        this.memberRoleRepository = Objects.requireNonNull(memberRoleRepository, "MemberRoleRepository cannot be null");
        this.guildRepository = Objects.requireNonNull(guildRepository, "GuildRepository cannot be null");
    }

    /**
     * Creates a new role for a guild.
     *
     * @param guildId the guild ID
     * @param name the role name
     * @param permissions the permission bitfield
     * @param hasManageRolesPermission whether requester has MANAGE_ROLES permission
     * @return the created role, or null if failed
     */
    public GuildRole createRole(String guildId, String name, int permissions, boolean hasManageRolesPermission) {
        return createRole(guildId, name, permissions, 0, hasManageRolesPermission);
    }

    /**
     * Creates a new role for a guild with specified priority.
     *
     * @param guildId the guild ID
     * @param name the role name
     * @param permissions the permission bitfield
     * @param priority the role priority
     * @param hasManageRolesPermission whether requester has MANAGE_ROLES permission
     * @return the created role, or null if failed
     */
    public GuildRole createRole(String guildId, String name, int permissions, int priority, boolean hasManageRolesPermission) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");

        if (!hasManageRolesPermission) {
            return null;
        }

        if (roleRepository.findByGuildAndName(guildId, name).isPresent()) {
            return null;
        }

        GuildRole role = new GuildRole(guildId, name, permissions, priority);
        roleRepository.save(role);
        return role;
    }

    /**
     * Deletes a role from a guild.
     *
     * @param guildId the guild ID
     * @param roleId the role ID to delete
     * @param hasManageRolesPermission whether requester has MANAGE_ROLES permission
     * @return true if deleted successfully
     */
    public boolean deleteRole(String guildId, String roleId, boolean hasManageRolesPermission) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(roleId, "Role ID cannot be null");

        if (!hasManageRolesPermission) {
            return false;
        }

        Optional<GuildRole> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty() || !roleOpt.get().getGuildId().equals(guildId)) {
            return false;
        }

        memberRoleRepository.removeAllByRole(roleId);
        roleRepository.delete(roleId);
        return true;
    }

    /**
     * Updates a role's permissions.
     *
     * @param guildId the guild ID
     * @param roleId the role ID
     * @param permissions the new permission bitfield
     * @param hasManageRolesPermission whether requester has MANAGE_ROLES permission
     * @return true if updated successfully
     */
    public boolean updateRolePermissions(String guildId, String roleId, int permissions, boolean hasManageRolesPermission) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(roleId, "Role ID cannot be null");

        if (!hasManageRolesPermission) {
            return false;
        }

        Optional<GuildRole> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty() || !roleOpt.get().getGuildId().equals(guildId)) {
            return false;
        }

        GuildRole role = roleOpt.get();
        role.setPermissions(permissions);
        roleRepository.save(role);
        return true;
    }

    /**
     * Saves a role to the repository.
     *
     * @param role the role to save
     */
    public void saveRole(GuildRole role) {
        Objects.requireNonNull(role, "Role cannot be null");
        roleRepository.save(role);
    }

    /**
     * Renames a role.
     *
     * @param guildId the guild ID
     * @param roleId the role ID
     * @param newName the new role name
     * @param hasManageRolesPermission whether requester has MANAGE_ROLES permission
     * @return true if renamed successfully
     */
    public boolean renameRole(String guildId, String roleId, String newName, boolean hasManageRolesPermission) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(roleId, "Role ID cannot be null");
        Objects.requireNonNull(newName, "New name cannot be null");

        if (!hasManageRolesPermission) {
            return false;
        }

        Optional<GuildRole> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty() || !roleOpt.get().getGuildId().equals(guildId)) {
            return false;
        }

        if (roleRepository.findByGuildAndName(guildId, newName).isPresent()) {
            return false;
        }

        GuildRole role = roleOpt.get();
        role.setName(newName);
        roleRepository.save(role);
        return true;
    }

    /**
     * Gets all roles for a guild.
     *
     * @param guildId the guild ID
     * @return list of roles
     */
    public List<GuildRole> getGuildRoles(String guildId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        return roleRepository.findByGuildId(guildId);
    }

    /**
     * Gets a role by name in a guild.
     *
     * @param guildId the guild ID
     * @param name the role name
     * @return the role, or null if not found
     */
    public GuildRole getRoleByName(String guildId, String name) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(name, "Name cannot be null");
        return roleRepository.findByGuildAndName(guildId, name).orElse(null);
    }

    /**
     * Gets a role by ID.
     *
     * @param roleId the role ID
     * @return the role, or null if not found
     */
    public GuildRole getRoleById(String roleId) {
        Objects.requireNonNull(roleId, "Role ID cannot be null");
        return roleRepository.findById(roleId).orElse(null);
    }

    /**
     * Assigns a role to a member.
     *
     * @param guildId the guild ID
     * @param targetId the member to assign the role to
     * @param roleId the role ID
     * @param hasManageRolesPermission whether requester has MANAGE_ROLES permission
     * @return true if assigned successfully
     */
    public boolean assignRole(String guildId, UUID targetId, String roleId, boolean hasManageRolesPermission) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(targetId, "Target ID cannot be null");
        Objects.requireNonNull(roleId, "Role ID cannot be null");

        if (!hasManageRolesPermission) {
            return false;
        }

        Optional<Guild> guildOpt = guildRepository.findById(guildId);
        if (guildOpt.isEmpty() || !guildOpt.get().isMember(targetId)) {
            return false;
        }

        Optional<GuildRole> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty() || !roleOpt.get().getGuildId().equals(guildId)) {
            return false;
        }

        memberRoleRepository.assignRole(guildId, targetId, roleId);
        return true;
    }

    /**
     * Unassigns a role from a member.
     *
     * @param guildId the guild ID
     * @param targetId the member to unassign the role from
     * @param roleId the role ID
     * @param hasManageRolesPermission whether requester has MANAGE_ROLES permission
     * @return true if unassigned successfully
     */
    public boolean unassignRole(String guildId, UUID targetId, String roleId, boolean hasManageRolesPermission) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(targetId, "Target ID cannot be null");
        Objects.requireNonNull(roleId, "Role ID cannot be null");

        if (!hasManageRolesPermission) {
            return false;
        }

        memberRoleRepository.unassignRole(guildId, targetId, roleId);
        return true;
    }

    /**
     * Gets all roles assigned to a member.
     *
     * @param guildId the guild ID
     * @param playerId the member UUID
     * @return list of roles
     */
    public List<GuildRole> getMemberRoles(String guildId, UUID playerId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(playerId, "Player ID cannot be null");

        List<String> roleIds = memberRoleRepository.getMemberRoleIds(guildId, playerId);
        List<GuildRole> roles = new ArrayList<>();
        for (String roleId : roleIds) {
            roleRepository.findById(roleId).ifPresent(roles::add);
        }
        return roles;
    }

    /**
     * Gets the highest priority role for a member.
     *
     * @param guildId the guild ID
     * @param playerId the player UUID
     * @return the highest priority role, or empty if no roles assigned
     */
    public Optional<GuildRole> getHighestPriorityRole(String guildId, UUID playerId) {
        List<String> roleIds = memberRoleRepository.getMemberRoleIds(guildId, playerId);
        GuildRole highestRole = null;

        for (String roleId : roleIds) {
            Optional<GuildRole> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isPresent()) {
                GuildRole role = roleOpt.get();
                if (highestRole == null || role.getPriority() > highestRole.getPriority()) {
                    highestRole = role;
                }
            }
        }

        return Optional.ofNullable(highestRole);
    }

    /**
     * Computes effective permissions by OR-ing all assigned role permissions.
     *
     * @param guildId the guild ID
     * @param playerId the player UUID
     * @return the effective permissions bitfield
     */
    public int computeEffectivePermissions(String guildId, UUID playerId) {
        List<String> roleIds = memberRoleRepository.getMemberRoleIds(guildId, playerId);
        int permissions = 0;
        for (String roleId : roleIds) {
            Optional<GuildRole> role = roleRepository.findById(roleId);
            if (role.isPresent()) {
                permissions |= role.get().getPermissions();
            }
        }
        return permissions;
    }

    /**
     * Gets effective permissions for a member (OR of all assigned role permissions).
     *
     * @param guildId the guild ID
     * @param playerId the player UUID
     * @return the effective permissions as MemberPermissions
     */
    public MemberPermissions getEffectivePermissions(String guildId, UUID playerId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(playerId, "Player ID cannot be null");

        Optional<Guild> guildOpt = guildRepository.findById(guildId);
        if (guildOpt.isPresent() && guildOpt.get().isOwner(playerId)) {
            return MemberPermissions.all();
        }

        return MemberPermissions.fromBitfield(computeEffectivePermissions(guildId, playerId));
    }

    /**
     * Creates and assigns a default role for a new guild member.
     *
     * @param guildId the guild ID
     * @param playerId the new member's UUID
     */
    public void assignDefaultRole(String guildId, UUID playerId) {
        roleRepository.findByGuildAndName(guildId, GuildRole.DEFAULT_ROLE_NAME)
                .ifPresent(role -> memberRoleRepository.assignRole(guildId, playerId, role.getId()));
    }

    /**
     * Creates the default role for a new guild and assigns it to the owner.
     *
     * @param guild the new guild
     */
    public void createDefaultRoleForGuild(Guild guild) {
        GuildRole defaultRole = GuildRole.createDefault(guild.getId());
        roleRepository.save(defaultRole);
        memberRoleRepository.assignRole(guild.getId(), guild.getOwnerId(), defaultRole.getId());
    }

    /**
     * Removes all roles for a member when they leave a guild.
     *
     * @param guildId the guild ID
     * @param playerId the member's UUID
     */
    public void removeAllMemberRoles(String guildId, UUID playerId) {
        memberRoleRepository.removeAllMemberRoles(guildId, playerId);
    }

    /**
     * Deletes all roles for a guild when it's disbanded.
     *
     * @param guildId the guild ID
     */
    public void deleteAllGuildRoles(String guildId) {
        memberRoleRepository.removeAllByGuild(guildId);
        roleRepository.deleteAllByGuild(guildId);
    }
}
