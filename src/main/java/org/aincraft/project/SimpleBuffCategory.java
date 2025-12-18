package org.aincraft.project;

import org.bukkit.Material;

/**
 * Simple immutable implementation of RegisterableBuffCategory.
 * Use this for registering buff categories at runtime.
 */
public record SimpleBuffCategory(
    String id,
    String displayName,
    BuffType buffType,
    Material icon
) implements RegisterableBuffCategory {

    /**
     * Constructor with icon defaulting to NETHER_STAR.
     *
     * @param id the unique category ID
     * @param displayName the human-readable name
     * @param buffType the buff type (ECONOMY or TERRITORY)
     */
    public SimpleBuffCategory(String id, String displayName, BuffType buffType) {
        this(id, displayName, buffType, Material.NETHER_STAR);
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
    public BuffType getBuffType() {
        return buffType;
    }

    @Override
    public Material getIcon() {
        return icon;
    }
}
