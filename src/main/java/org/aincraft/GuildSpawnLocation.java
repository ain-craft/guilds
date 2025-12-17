package org.aincraft;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Immutable value object representing a guild's spawn location.
 * Encapsulates all spawn-related coordinates and world information.
 */
public record GuildSpawnLocation(String world, double x, double y, double z, float yaw, float pitch) {

    /**
     * Creates a GuildSpawnLocation from a Bukkit Location.
     *
     * @param location the Bukkit location
     * @return a new GuildSpawnLocation, or null if world is null
     */
    public static GuildSpawnLocation from(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }

        return new GuildSpawnLocation(
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );
    }

    /**
     * Converts this GuildSpawnLocation to a Bukkit Location.
     *
     * @return the Bukkit location, or null if the world is not loaded
     */
    public Location toBukkit() {
        World w = Bukkit.getWorld(world);
        if (w == null) {
            return null;
        }

        return new Location(w, x, y, z, yaw, pitch);
    }

    /**
     * Checks if this location is valid (world is loaded).
     *
     * @return true if the world exists
     */
    public boolean isValid() {
        return Bukkit.getWorld(world) != null;
    }

    /**
     * Gets the distance to another location in the same world.
     * Returns -1 if worlds differ.
     *
     * @param other the other location
     * @return the distance, or -1 if different worlds
     */
    public double distance(GuildSpawnLocation other) {
        if (other == null || !world.equals(other.world)) {
            return -1;
        }

        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public String toString() {
        return String.format("GuildSpawnLocation{world='%s', x=%.2f, y=%.2f, z=%.2f, yaw=%.1f, pitch=%.1f}",
            world, x, y, z, yaw, pitch);
    }
}
