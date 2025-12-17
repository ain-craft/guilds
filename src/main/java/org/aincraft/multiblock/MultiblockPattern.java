package org.aincraft.multiblock;

import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.util.Vector;

/**
 * Defines a multiblock structure pattern.
 * Patterns are defined relative to an origin (0,0,0).
 */
public interface MultiblockPattern {

    /**
     * Unique identifier for this pattern type.
     */
    String getId();

    /**
     * Human-readable display name.
     */
    String getDisplayName();

    /**
     * Block positions relative to origin with required materials.
     *
     * @return map of relative positions to acceptable materials at that position
     */
    Map<Vector, Set<Material>> getBlocks();

    /**
     * Materials that can trigger detection when placed/broken.
     * Optimization: only check pattern when these materials change.
     */
    Set<Material> getTriggerMaterials();

    /**
     * Whether this pattern supports rotation detection.
     */
    default boolean supportsRotation() {
        return true;
    }

    /**
     * Whether this pattern supports mirror detection.
     */
    default boolean supportsMirroring() {
        return false;
    }

    /**
     * Gets the bounding box dimensions for spatial queries.
     * Returns the maximum extent in each direction from the origin.
     */
    Vector getBoundingBox();
}
