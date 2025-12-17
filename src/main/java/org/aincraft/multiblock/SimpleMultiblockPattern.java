package org.aincraft.multiblock;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.util.Vector;

/**
 * Default implementation of MultiblockPattern.
 */
public final class SimpleMultiblockPattern implements MultiblockPattern {
    private final String id;
    private final String displayName;
    private final Map<Vector, Set<Material>> blocks;
    private final Set<Material> triggerMaterials;
    private final boolean supportsRotation;
    private final boolean supportsMirroring;
    private final Vector boundingBox;

    SimpleMultiblockPattern(String id, String displayName,
                            Map<Vector, Set<Material>> blocks,
                            boolean supportsRotation, boolean supportsMirroring) {
        this.id = id;
        this.displayName = displayName;
        this.blocks = Map.copyOf(blocks);
        this.supportsRotation = supportsRotation;
        this.supportsMirroring = supportsMirroring;

        Set<Material> triggers = new HashSet<>();
        blocks.values().forEach(triggers::addAll);
        this.triggerMaterials = Set.copyOf(triggers);

        this.boundingBox = calculateBoundingBox(blocks.keySet());
    }

    private Vector calculateBoundingBox(Set<Vector> positions) {
        int maxX = 0, maxY = 0, maxZ = 0;
        for (Vector v : positions) {
            maxX = Math.max(maxX, Math.abs(v.getBlockX()) + 1);
            maxY = Math.max(maxY, Math.abs(v.getBlockY()) + 1);
            maxZ = Math.max(maxZ, Math.abs(v.getBlockZ()) + 1);
        }
        return new Vector(maxX, maxY, maxZ);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Map<Vector, Set<Material>> getBlocks() {
        return blocks;
    }

    @Override
    public Set<Material> getTriggerMaterials() {
        return triggerMaterials;
    }

    @Override
    public boolean supportsRotation() {
        return supportsRotation;
    }

    @Override
    public boolean supportsMirroring() {
        return supportsMirroring;
    }

    @Override
    public Vector getBoundingBox() {
        return boundingBox;
    }
}
