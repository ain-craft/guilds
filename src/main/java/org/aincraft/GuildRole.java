package org.aincraft;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a role within a guild with associated permissions.
 * Roles are guild-specific and identified by name + permission bitfield.
 */
public final class GuildRole {
    public static final String DEFAULT_ROLE_NAME = "Member";

    private final String id;
    private final String guildId;
    private String name;
    private int permissions;
    private int priority;

    /**
     * Creates a new GuildRole.
     *
     * @param guildId the guild this role belongs to
     * @param name the role name
     * @param permissions the permission bitfield
     * @param priority the role priority (higher = more authority)
     */
    public GuildRole(String guildId, String name, int permissions, int priority) {
        this.id = UUID.randomUUID().toString();
        this.guildId = Objects.requireNonNull(guildId, "Guild ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.permissions = permissions;
        this.priority = priority;
    }

    /**
     * Creates a new GuildRole with default priority (0).
     *
     * @param guildId the guild this role belongs to
     * @param name the role name
     * @param permissions the permission bitfield
     */
    public GuildRole(String guildId, String name, int permissions) {
        this(guildId, name, permissions, 0);
    }

    /**
     * Creates a GuildRole with an existing ID (for database restoration).
     */
    public GuildRole(String id, String guildId, String name, int permissions, int priority) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.guildId = Objects.requireNonNull(guildId, "Guild ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.permissions = permissions;
        this.priority = priority;
    }

    /**
     * Creates a GuildRole with an existing ID and default priority (for database restoration).
     */
    public GuildRole(String id, String guildId, String name, int permissions) {
        this(id, guildId, name, permissions, 0);
    }

    /**
     * Creates the default "Member" role with basic permissions.
     */
    public static GuildRole createDefault(String guildId) {
        return new GuildRole(guildId, DEFAULT_ROLE_NAME, GuildPermission.defaultPermissions());
    }

    public String getId() {
        return id;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
    }

    public int getPermissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Checks if this role has a specific permission.
     */
    public boolean hasPermission(GuildPermission permission) {
        return (permissions & permission.getBit()) != 0;
    }

    /**
     * Grants a permission to this role.
     */
    public void grantPermission(GuildPermission permission) {
        permissions |= permission.getBit();
    }

    /**
     * Revokes a permission from this role.
     */
    public void revokePermission(GuildPermission permission) {
        permissions &= ~permission.getBit();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GuildRole that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Checks if this role has higher priority than another role.
     */
    public boolean hasHigherPriorityThan(GuildRole other) {
        return this.priority > other.priority;
    }

    @Override
    public String toString() {
        return "GuildRole{" +
                "id='" + id + '\'' +
                ", guildId='" + guildId + '\'' +
                ", name='" + name + '\'' +
                ", permissions=" + permissions +
                ", priority=" + priority +
                '}';
    }
}
