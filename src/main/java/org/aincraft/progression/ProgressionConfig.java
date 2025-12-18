package org.aincraft.progression;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.aincraft.GuildsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration loader for guild progression settings.
 * Single Responsibility: Load and provide access to progression configuration.
 */
@Singleton
public class ProgressionConfig {
    private final GuildsPlugin plugin;

    // Progression settings
    private int maxLevel;
    private long baseXp;
    private double growthFactor;

    // XP sources
    private boolean mobKillEnabled;
    private long mobKillBaseXp;
    private Map<String, Double> mobKillMultipliers;

    private boolean blockMiningEnabled;
    private long blockMiningBaseXp;
    private Map<String, Double> blockMiningMultipliers;

    private boolean playtimeEnabled;
    private long playtimeXpPerMinute;
    private int playtimeCheckInterval;

    // Rewards
    private int membersPerLevel;
    private int chunksPerLevel;
    private int baseMaxMembers;
    private int baseMaxChunks;

    // Material costs - no config needed, using API registration

    @Inject
    public ProgressionConfig(GuildsPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        // Progression settings
        maxLevel = plugin.getConfig().getInt("progression.max-level", 100);
        baseXp = plugin.getConfig().getLong("progression.base-xp", 1000L);
        growthFactor = plugin.getConfig().getDouble("progression.growth-factor", 1.15);

        // XP sources - mob kills
        ConfigurationSection mobKillSection = plugin.getConfig().getConfigurationSection("progression.xp-sources.mob-kill");
        if (mobKillSection != null) {
            mobKillEnabled = mobKillSection.getBoolean("enabled", true);
            mobKillBaseXp = mobKillSection.getLong("base-xp", 10L);
            mobKillMultipliers = loadMultipliers(mobKillSection.getConfigurationSection("multipliers"));
        } else {
            mobKillEnabled = true;
            mobKillBaseXp = 10L;
            mobKillMultipliers = getDefaultMobMultipliers();
        }

        // XP sources - block mining
        ConfigurationSection miningSection = plugin.getConfig().getConfigurationSection("progression.xp-sources.block-mining");
        if (miningSection != null) {
            blockMiningEnabled = miningSection.getBoolean("enabled", true);
            blockMiningBaseXp = miningSection.getLong("base-xp", 1L);
            blockMiningMultipliers = loadMultipliers(miningSection.getConfigurationSection("multipliers"));
        } else {
            blockMiningEnabled = true;
            blockMiningBaseXp = 1L;
            blockMiningMultipliers = getDefaultMiningMultipliers();
        }

        // XP sources - playtime
        ConfigurationSection playtimeSection = plugin.getConfig().getConfigurationSection("progression.xp-sources.playtime");
        if (playtimeSection != null) {
            playtimeEnabled = playtimeSection.getBoolean("enabled", true);
            playtimeXpPerMinute = playtimeSection.getLong("xp-per-minute", 5L);
            playtimeCheckInterval = playtimeSection.getInt("check-interval-seconds", 60);
        } else {
            playtimeEnabled = true;
            playtimeXpPerMinute = 5L;
            playtimeCheckInterval = 60;
        }

        // Rewards
        ConfigurationSection rewardsSection = plugin.getConfig().getConfigurationSection("progression.rewards");
        if (rewardsSection != null) {
            membersPerLevel = rewardsSection.getInt("members-per-level", 2);
            chunksPerLevel = rewardsSection.getInt("chunks-per-level", 5);
            baseMaxMembers = rewardsSection.getInt("base-max-members", 100);
            baseMaxChunks = rewardsSection.getInt("base-max-chunks", 50);
        } else {
            membersPerLevel = 2;
            chunksPerLevel = 5;
            baseMaxMembers = 100;
            baseMaxChunks = 50;
        }

        plugin.getLogger().info("Progression system configured - Max level: " + maxLevel + " (Procedural costs with probability weighting)");
    }

    private Map<String, Double> loadMultipliers(ConfigurationSection section) {
        Map<String, Double> multipliers = new HashMap<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                multipliers.put(key.toUpperCase(), section.getDouble(key, 1.0));
            }
        }
        return multipliers;
    }


    private Map<String, Double> getDefaultMobMultipliers() {
        Map<String, Double> defaults = new HashMap<>();
        defaults.put("ZOMBIE", 1.0);
        defaults.put("SKELETON", 1.0);
        defaults.put("CREEPER", 1.2);
        defaults.put("ENDERMAN", 2.0);
        defaults.put("ENDER_DRAGON", 100.0);
        return defaults;
    }

    private Map<String, Double> getDefaultMiningMultipliers() {
        Map<String, Double> defaults = new HashMap<>();
        defaults.put("DIAMOND_ORE", 10.0);
        defaults.put("DEEPSLATE_DIAMOND_ORE", 10.0);
        defaults.put("EMERALD_ORE", 15.0);
        defaults.put("DEEPSLATE_EMERALD_ORE", 15.0);
        defaults.put("ANCIENT_DEBRIS", 20.0);
        return defaults;
    }


    public void reload() {
        loadConfig();
    }

    // Getters

    public int getMaxLevel() {
        return maxLevel;
    }

    public long getBaseXp() {
        return baseXp;
    }

    public double getGrowthFactor() {
        return growthFactor;
    }

    public boolean isMobKillEnabled() {
        return mobKillEnabled;
    }

    public long getMobKillBaseXp() {
        return mobKillBaseXp;
    }

    public double getMobKillMultiplier(String entityType) {
        return mobKillMultipliers.getOrDefault(entityType.toUpperCase(), 1.0);
    }

    public boolean isBlockMiningEnabled() {
        return blockMiningEnabled;
    }

    public long getBlockMiningBaseXp() {
        return blockMiningBaseXp;
    }

    public double getBlockMiningMultiplier(String blockType) {
        return blockMiningMultipliers.getOrDefault(blockType.toUpperCase(), 1.0);
    }

    public boolean isPlaytimeEnabled() {
        return playtimeEnabled;
    }

    public long getPlaytimeXpPerMinute() {
        return playtimeXpPerMinute;
    }

    public int getPlaytimeCheckInterval() {
        return playtimeCheckInterval;
    }

    public int getMembersPerLevel() {
        return membersPerLevel;
    }

    public int getChunksPerLevel() {
        return chunksPerLevel;
    }

    public int getBaseMaxMembers() {
        return baseMaxMembers;
    }

    public int getBaseMaxChunks() {
        return baseMaxChunks;
    }
}
