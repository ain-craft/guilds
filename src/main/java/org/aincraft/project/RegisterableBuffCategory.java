package org.aincraft.project;

import org.bukkit.Material;

/**
 * Interface for buff categories that can be registered at runtime.
 * Allows both built-in and custom buff types to be treated uniformly.
 */
public interface RegisterableBuffCategory {
    /**
     * Gets the unique ID for this buff category.
     * Should match the enum name for built-in categories (e.g., "XP_MULTIPLIER").
     *
     * @return the category ID
     */
    String getId();

    /**
     * Gets the human-readable display name for this buff.
     *
     * @return the display name
     */
    String getDisplayName();

    /**
     * Gets the type of buff (scope of application).
     *
     * @return the buff type (ECONOMY or TERRITORY)
     */
    BuffType getBuffType();

    /**
     * Gets the material icon for this buff category (for GUIs, etc.).
     * Default is NETHER_STAR.
     *
     * @return the icon material
     */
    default Material getIcon() {
        return Material.NETHER_STAR;
    }
}
