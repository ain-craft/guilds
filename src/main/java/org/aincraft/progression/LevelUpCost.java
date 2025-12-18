package org.aincraft.progression;

import org.bukkit.Material;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the material costs required to level up a guild.
 * Immutable record-style class.
 */
public final class LevelUpCost {
    private final Map<Material, Integer> materials;

    /**
     * Creates a new level-up cost requirement.
     *
     * @param materials map of materials to required amounts (cannot be null)
     */
    public LevelUpCost(Map<Material, Integer> materials) {
        this.materials = Collections.unmodifiableMap(
            Objects.requireNonNull(materials, "Materials map cannot be null")
        );
    }

    /**
     * Gets the required materials and their amounts.
     *
     * @return unmodifiable map of materials to amounts
     */
    public Map<Material, Integer> getMaterials() {
        return materials;
    }

    /**
     * Checks if the cost contains a specific material.
     *
     * @param material the material to check
     * @return true if the material is required
     */
    public boolean contains(Material material) {
        return materials.containsKey(material);
    }

    /**
     * Gets the amount required for a specific material.
     *
     * @param material the material
     * @return the required amount, or 0 if not required
     */
    public int getAmount(Material material) {
        return materials.getOrDefault(material, 0);
    }

    /**
     * Checks if the cost is empty (no materials required).
     *
     * @return true if no materials are required
     */
    public boolean isEmpty() {
        return materials.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LevelUpCost)) return false;
        LevelUpCost that = (LevelUpCost) o;
        return Objects.equals(materials, that.materials);
    }

    @Override
    public int hashCode() {
        return Objects.hash(materials);
    }

    @Override
    public String toString() {
        return "LevelUpCost{" +
                "materials=" + materials +
                '}';
    }
}
