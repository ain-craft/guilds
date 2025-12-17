package org.aincraft.multiblock;

import org.bukkit.Location;

import java.util.Set;
import java.util.UUID;

/**
 * Represents a detected multiblock instance in the world.
 * Immutable snapshot of the multiblock state at detection time.
 */
public record MultiblockInstance(
        String instanceId,
        String patternId,
        Location origin,
        Rotation rotation,
        boolean mirrored,
        Set<Location> blockLocations,
        long detectedAt
) {
    public MultiblockInstance {
        instanceId = instanceId != null ? instanceId : UUID.randomUUID().toString();
        blockLocations = Set.copyOf(blockLocations);
        detectedAt = detectedAt > 0 ? detectedAt : System.currentTimeMillis();
    }

    /**
     * Creates a new instance with generated ID and current timestamp.
     */
    public MultiblockInstance(String patternId, Location origin, Rotation rotation,
                              boolean mirrored, Set<Location> blockLocations) {
        this(null, patternId, origin, rotation, mirrored, blockLocations, 0);
    }

    /**
     * Checks if a location is part of this multiblock.
     *
     * @param loc the location to check
     * @return true if the location is part of this multiblock
     */
    public boolean containsBlock(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }
        String worldName = loc.getWorld().getName();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return blockLocations.stream().anyMatch(l ->
                l.getWorld() != null &&
                        l.getWorld().getName().equals(worldName) &&
                        l.getBlockX() == x &&
                        l.getBlockY() == y &&
                        l.getBlockZ() == z
        );
    }
}
