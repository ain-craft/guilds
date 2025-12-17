package org.aincraft.multiblock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Stateless multiblock detection logic.
 * Handles pattern matching with rotation and mirroring support.
 */
public class MultiblockDetector {

    /**
     * Detects if a multiblock pattern exists around a changed block.
     * Checks all possible origins within the pattern's bounding box.
     *
     * @param pattern the pattern to detect
     * @param changedBlock the block that was placed/changed
     * @return detected instance, or empty if no match
     */
    public Optional<MultiblockInstance> detect(MultiblockPattern pattern, Block changedBlock) {
        Location loc = changedBlock.getLocation();
        Vector bbox = pattern.getBoundingBox();

        // The changed block could be ANY block in the pattern.
        // Check all potential origin positions.
        for (int dx = -bbox.getBlockX(); dx <= bbox.getBlockX(); dx++) {
            for (int dy = -bbox.getBlockY(); dy <= bbox.getBlockY(); dy++) {
                for (int dz = -bbox.getBlockZ(); dz <= bbox.getBlockZ(); dz++) {
                    Location potentialOrigin = loc.clone().add(dx, dy, dz);

                    Optional<MultiblockInstance> result = checkAtOrigin(pattern, potentialOrigin);
                    if (result.isPresent()) {
                        return result;
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if pattern matches at exact origin with any rotation/mirror combination.
     */
    private Optional<MultiblockInstance> checkAtOrigin(MultiblockPattern pattern, Location origin) {
        Rotation[] rotations = pattern.supportsRotation()
                ? Rotation.values()
                : new Rotation[]{Rotation.NONE};

        boolean[] mirrorOptions = pattern.supportsMirroring()
                ? new boolean[]{false, true}
                : new boolean[]{false};

        for (Rotation rotation : rotations) {
            for (boolean mirrored : mirrorOptions) {
                if (matchesPattern(pattern, origin, rotation, mirrored)) {
                    Set<Location> blockLocs = getBlockLocations(pattern, origin, rotation, mirrored);
                    return Optional.of(new MultiblockInstance(
                            pattern.getId(), origin, rotation, mirrored, blockLocs
                    ));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if all blocks match the pattern at given transformation.
     */
    private boolean matchesPattern(MultiblockPattern pattern, Location origin,
                                   Rotation rotation, boolean mirrored) {
        World world = origin.getWorld();
        if (world == null) {
            return false;
        }

        for (Map.Entry<Vector, Set<Material>> entry : pattern.getBlocks().entrySet()) {
            Vector relPos = entry.getKey();
            Set<Material> validMaterials = entry.getValue();

            // Apply transformations
            Vector transformed = rotation.rotate(relPos);
            if (mirrored) {
                transformed = Rotation.mirror(transformed);
            }

            Location blockLoc = origin.clone().add(transformed);
            Block block = world.getBlockAt(blockLoc);

            // Early exit on mismatch
            if (!validMaterials.contains(block.getType())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets all block locations for a matched pattern.
     */
    private Set<Location> getBlockLocations(MultiblockPattern pattern, Location origin,
                                            Rotation rotation, boolean mirrored) {
        Set<Location> locations = new HashSet<>();

        for (Vector relPos : pattern.getBlocks().keySet()) {
            Vector transformed = rotation.rotate(relPos);
            if (mirrored) {
                transformed = Rotation.mirror(transformed);
            }
            locations.add(origin.clone().add(transformed));
        }
        return locations;
    }

    /**
     * Validates an existing multiblock structure is still intact.
     *
     * @param pattern the pattern to validate against
     * @param instance the instance to validate
     * @return true if the structure is still complete
     */
    public boolean isIntact(MultiblockPattern pattern, MultiblockInstance instance) {
        return matchesPattern(pattern, instance.origin(), instance.rotation(), instance.mirrored());
    }
}
