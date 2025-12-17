package org.aincraft.multiblock;

import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Fluent builder for creating multiblock patterns.
 * Supports layer-by-layer definition for intuitive structure design.
 */
public class MultiblockPatternBuilder {
    private final String id;
    private String displayName;
    private final Map<Vector, Set<Material>> blocks = new HashMap<>();
    private boolean supportsRotation = true;
    private boolean supportsMirroring = false;

    private MultiblockPatternBuilder(String id) {
        this.id = Objects.requireNonNull(id, "Pattern ID cannot be null");
        this.displayName = id;
    }

    /**
     * Creates a new pattern builder with the given ID.
     *
     * @param id unique identifier for the pattern
     */
    public static MultiblockPatternBuilder create(String id) {
        return new MultiblockPatternBuilder(id);
    }

    /**
     * Sets the display name for this pattern.
     */
    public MultiblockPatternBuilder displayName(String name) {
        this.displayName = Objects.requireNonNull(name);
        return this;
    }

    /**
     * Adds a block at relative position with multiple valid materials.
     *
     * @param x x offset from origin
     * @param y y offset from origin
     * @param z z offset from origin
     * @param materials acceptable materials at this position
     */
    public MultiblockPatternBuilder block(int x, int y, int z, Material... materials) {
        if (materials.length == 0) {
            throw new IllegalArgumentException("At least one material required");
        }
        blocks.put(new Vector(x, y, z), Set.of(materials));
        return this;
    }

    /**
     * Defines a layer (Y-slice) using a 2D character map.
     * Characters in the legend map to materials. Space characters are ignored.
     *
     * @param y the Y coordinate for this layer
     * @param legend maps characters to materials
     * @param rows the pattern rows (Z varies per row, X varies per character)
     */
    public MultiblockPatternBuilder layer(int y, Map<Character, Material> legend, String... rows) {
        for (int z = 0; z < rows.length; z++) {
            String row = rows[z];
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c != ' ' && legend.containsKey(c)) {
                    blocks.put(new Vector(x, y, z), Set.of(legend.get(c)));
                }
            }
        }
        return this;
    }

    /**
     * Enables or disables rotation detection (default: enabled).
     */
    public MultiblockPatternBuilder rotation(boolean enabled) {
        this.supportsRotation = enabled;
        return this;
    }

    /**
     * Enables or disables mirror detection (default: disabled).
     */
    public MultiblockPatternBuilder mirroring(boolean enabled) {
        this.supportsMirroring = enabled;
        return this;
    }

    /**
     * Builds the multiblock pattern.
     *
     * @throws IllegalStateException if no blocks have been defined
     */
    public MultiblockPattern build() {
        if (blocks.isEmpty()) {
            throw new IllegalStateException("Pattern must have at least one block");
        }
        return new SimpleMultiblockPattern(id, displayName, blocks, supportsRotation, supportsMirroring);
    }
}
