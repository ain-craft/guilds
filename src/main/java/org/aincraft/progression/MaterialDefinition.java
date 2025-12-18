package org.aincraft.progression;

import org.bukkit.Material;

import java.util.Objects;

/**
 * Defines a material's eligibility for procedural cost generation.
 * Includes level range constraints and probability weighting.
 */
public class MaterialDefinition {
    private final Material material;
    private final int minLevel;
    private final int maxLevel;
    private final int baseAmount;
    private final double probability;

    /**
     * Creates a material definition.
     *
     * @param material the material type
     * @param minLevel minimum guild level for this material to appear (inclusive)
     * @param maxLevel maximum guild level for this material to appear (inclusive)
     * @param baseAmount base quantity before exponential scaling
     * @param probability selection probability weight (higher = more likely)
     */
    public MaterialDefinition(Material material, int minLevel, int maxLevel, int baseAmount, double probability) {
        Objects.requireNonNull(material, "Material cannot be null");
        if (minLevel < 1) {
            throw new IllegalArgumentException("Min level must be >= 1");
        }
        if (maxLevel < minLevel) {
            throw new IllegalArgumentException("Max level must be >= min level");
        }
        if (baseAmount < 1) {
            throw new IllegalArgumentException("Base amount must be >= 1");
        }
        if (probability <= 0) {
            throw new IllegalArgumentException("Probability must be > 0");
        }

        this.material = material;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.baseAmount = baseAmount;
        this.probability = probability;
    }

    /**
     * Checks if this material is eligible for a given level.
     *
     * @param level the guild level
     * @return true if level is within range
     */
    public boolean isEligibleForLevel(int level) {
        return level >= minLevel && level <= maxLevel;
    }

    public Material getMaterial() {
        return material;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getBaseAmount() {
        return baseAmount;
    }

    public double getProbability() {
        return probability;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaterialDefinition that = (MaterialDefinition) o;
        return material == that.material;
    }

    @Override
    public int hashCode() {
        return Objects.hash(material);
    }

    @Override
    public String toString() {
        return "MaterialDefinition{" +
                "material=" + material +
                ", minLevel=" + minLevel +
                ", maxLevel=" + maxLevel +
                ", baseAmount=" + baseAmount +
                ", probability=" + probability +
                '}';
    }
}
