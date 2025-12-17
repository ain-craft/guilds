package org.aincraft;

/**
 * Interface for objects that have permission bitfields.
 * Provides a standardized way to check permissions using bitfield operations.
 */
public interface Permissible {

    /**
     * Gets the permission bitfield.
     *
     * @return the permissions as a bitfield
     */
    int getPermissions();

    /**
     * Checks if the permissible has a specific permission.
     *
     * @param permissionBit the permission bit to check
     * @return true if the permission is set
     */
    default boolean hasPermission(int permissionBit) {
        return (getPermissions() & permissionBit) != 0;
    }

    /**
     * Checks if the permissible has a specific GuildPermission.
     *
     * @param permission the permission to check
     * @return true if the permission is set
     */
    default boolean hasPermission(GuildPermission permission) {
        return hasPermission(permission.getBit());
    }

    /**
     * Checks if the permissible has all the specified permissions.
     *
     * @param permissionBits the permission bits to check
     * @return true if all permissions are set
     */
    default boolean hasAllPermissions(int permissionBits) {
        return (getPermissions() & permissionBits) == permissionBits;
    }

    /**
     * Checks if the permissible has any of the specified permissions.
     *
     * @param permissionBits the permission bits to check
     * @return true if any permission is set
     */
    default boolean hasAnyPermission(int permissionBits) {
        return (getPermissions() & permissionBits) != 0;
    }
}
