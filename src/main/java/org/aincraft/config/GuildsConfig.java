package org.aincraft.config;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.aincraft.GuildsPlugin;
import org.aincraft.subregion.SubjectType;
import org.bukkit.configuration.ConfigurationSection;

@Singleton
public class GuildsConfig {
    private final GuildsPlugin plugin;
    private int claimBufferDistance;
    private final Map<SubjectType, String> defaultRoleAssignments = new HashMap<>();

    @Inject
    public GuildsConfig(GuildsPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin);
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        claimBufferDistance = plugin.getConfig().getInt("claim.buffer-distance", 4);

        if (claimBufferDistance < 0) {
            plugin.getLogger().warning("Invalid claim.buffer-distance: " + claimBufferDistance + ". Using default: 4");
            claimBufferDistance = 4;
        }

        plugin.getLogger().info("Claim buffer distance set to: " + claimBufferDistance + " chunks");

        loadDefaultRoleAssignments();
    }

    private void loadDefaultRoleAssignments() {
        defaultRoleAssignments.clear();

        ConfigurationSection assignmentsSection = plugin.getConfig().getConfigurationSection("default-role-assignments");
        if (assignmentsSection == null) {
            plugin.getLogger().fine("No default-role-assignments section found in config.yml");
            return;
        }

        for (String key : assignmentsSection.getKeys(false)) {
            try {
                SubjectType subjectType = SubjectType.valueOf(key.toUpperCase());
                String roleName = assignmentsSection.getString(key);
                if (roleName != null && !roleName.isEmpty()) {
                    defaultRoleAssignments.put(subjectType, roleName);
                    plugin.getLogger().fine("Loaded default role assignment: " + subjectType + " -> " + roleName);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown subject type '" + key + "' in default-role-assignments");
            }
        }
    }

    public int getClaimBufferDistance() {
        return claimBufferDistance;
    }

    public GuildsPlugin getPlugin() {
        return plugin;
    }

    public Map<SubjectType, String> getDefaultRoleAssignments() {
        return Collections.unmodifiableMap(defaultRoleAssignments);
    }

    public String getDefaultRoleForSubjectType(SubjectType subjectType) {
        return defaultRoleAssignments.get(subjectType);
    }

    public void reload() {
        loadConfig();
    }
}
