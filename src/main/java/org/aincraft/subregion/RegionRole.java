package org.aincraft.subregion;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a role scoped to a specific region.
 * Region roles have lower priority than guild roles in the permission hierarchy.
 */
public class RegionRole {
    private final String id;
    private final String regionId;
    private String name;
    private int permissions;
    private final long createdAt;
    private final UUID createdBy;

    /**
     * Creates a new region role.
     */
    public RegionRole(String regionId, String name, int permissions, UUID createdBy) {
        this.id = UUID.randomUUID().toString();
        this.regionId = Objects.requireNonNull(regionId, "Region ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.permissions = permissions;
        this.createdAt = System.currentTimeMillis();
        this.createdBy = Objects.requireNonNull(createdBy, "Creator cannot be null");
    }

    /**
     * Constructor for loading from database.
     */
    public RegionRole(String id, String regionId, String name, int permissions, long createdAt, UUID createdBy) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.regionId = Objects.requireNonNull(regionId, "Region ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.permissions = permissions;
        this.createdAt = createdAt;
        this.createdBy = Objects.requireNonNull(createdBy, "Creator cannot be null");
    }

    public String getId() {
        return id;
    }

    public String getRegionId() {
        return regionId;
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

    /**
     * Checks if this role has a specific permission.
     */
    public boolean hasPermission(int permissionBit) {
        return (permissions & permissionBit) != 0;
    }

    /**
     * Grants a permission to this role.
     */
    public void grantPermission(int permissionBit) {
        permissions |= permissionBit;
    }

    /**
     * Revokes a permission from this role.
     */
    public void revokePermission(int permissionBit) {
        permissions &= ~permissionBit;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegionRole that = (RegionRole) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RegionRole{" +
                "id='" + id + '\'' +
                ", regionId='" + regionId + '\'' +
                ", name='" + name + '\'' +
                ", permissions=" + permissions +
                '}';
    }
}
