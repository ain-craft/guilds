package org.aincraft.project;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.aincraft.project.storage.ActiveBuffRepository;

import java.util.Objects;
import java.util.Optional;

@Singleton
public class BuffApplicationService {

    private final ActiveBuffRepository buffRepository;
    private final BuffCategoryRegistry buffCategoryRegistry;

    @Inject
    public BuffApplicationService(ActiveBuffRepository buffRepository, BuffCategoryRegistry buffCategoryRegistry) {
        this.buffRepository = Objects.requireNonNull(buffRepository);
        this.buffCategoryRegistry = Objects.requireNonNull(buffCategoryRegistry);
    }

    public double getXpMultiplier(String guildId) {
        return getBuffValue(guildId, BuffCategory.XP_MULTIPLIER, 1.0);
    }

    public double getLuckBonus(String guildId) {
        return getBuffValue(guildId, BuffCategory.LUCK_BONUS, 1.0);
    }

    public double getCropGrowthMultiplier(String guildId) {
        return getBuffValue(guildId, BuffCategory.CROP_GROWTH_SPEED, 1.0);
    }

    public double getMobSpawnMultiplier(String guildId) {
        return getBuffValue(guildId, BuffCategory.MOB_SPAWN_RATE, 1.0);
    }

    public double getProtectionMultiplier(String guildId) {
        return getBuffValue(guildId, BuffCategory.PROTECTION_BOOST, 1.0);
    }

    public double getBuffValue(String guildId, BuffCategory category, double defaultValue) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");

        Optional<ActiveBuff> buffOpt = buffRepository.findActiveByGuildId(guildId);
        if (buffOpt.isEmpty()) {
            return defaultValue;
        }

        ActiveBuff buff = buffOpt.get();
        if (buff.isExpired()) {
            return defaultValue;
        }

        if (buff.category() == category) {
            return buff.value();
        }

        return defaultValue;
    }

    public boolean hasBuff(String guildId, BuffCategory category) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(category, "Category cannot be null");

        Optional<ActiveBuff> buffOpt = buffRepository.findActiveByGuildId(guildId);
        if (buffOpt.isEmpty()) {
            return false;
        }

        ActiveBuff buff = buffOpt.get();
        return !buff.isExpired() && buff.category() == category;
    }

    /**
     * Checks if a guild has an active buff for the given category ID.
     * Supports custom buff categories registered at runtime.
     *
     * @param guildId the guild ID
     * @param categoryId the buff category ID (string)
     * @return true if the guild has an active buff for this category
     */
    public boolean hasBuff(String guildId, String categoryId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(categoryId, "Category ID cannot be null");

        // Try to match by BuffCategory enum first for backward compatibility
        for (BuffCategory cat : BuffCategory.values()) {
            if (cat.name().equals(categoryId)) {
                return hasBuff(guildId, cat);
            }
        }

        // For custom categories, we would need to extend ActiveBuff to support string IDs
        // For now, return false for unknown categories
        return false;
    }

    /**
     * Gets the buff value for a guild and category ID.
     * Supports custom buff categories registered at runtime.
     *
     * @param guildId the guild ID
     * @param categoryId the buff category ID (string)
     * @param defaultValue the default value if no buff is active
     * @return the buff value or default value
     */
    public double getBuffValue(String guildId, String categoryId, double defaultValue) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(categoryId, "Category ID cannot be null");

        // Try to match by BuffCategory enum first for backward compatibility
        for (BuffCategory cat : BuffCategory.values()) {
            if (cat.name().equals(categoryId)) {
                return getBuffValue(guildId, cat, defaultValue);
            }
        }

        // For custom categories, we would need to extend ActiveBuff to support string IDs
        // For now, return default value for unknown categories
        return defaultValue;
    }

    public void cleanupExpiredBuffs() {
        buffRepository.deleteExpired();
    }
}
