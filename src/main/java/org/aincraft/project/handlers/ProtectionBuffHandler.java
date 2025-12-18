package org.aincraft.project.handlers;

import com.google.inject.Inject;
import org.aincraft.ChunkKey;
import org.aincraft.Guild;
import org.aincraft.GuildService;
import org.aincraft.project.BuffApplicationService;
import org.aincraft.project.BuffHandler;
import org.aincraft.project.RegisterableBuffCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Objects;

/**
 * Handles the PROTECTION_BOOST buff effect.
 * Reduces damage taken by guild members in their own guild's territory.
 */
public class ProtectionBuffHandler implements BuffHandler {
    private final RegisterableBuffCategory category;
    private final BuffApplicationService buffService;
    private final GuildService guildService;

    @Inject
    public ProtectionBuffHandler(
        RegisterableBuffCategory category,
        BuffApplicationService buffService,
        GuildService guildService
    ) {
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.buffService = Objects.requireNonNull(buffService, "Buff service cannot be null");
        this.guildService = Objects.requireNonNull(guildService, "Guild service cannot be null");
    }

    @Override
    public RegisterableBuffCategory getCategory() {
        return category;
    }

    /**
     * Reduces damage taken by players in their own guild's territory.
     * Protection multiplier is typically < 1.0 (e.g., 0.85 for 15% damage reduction).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ChunkKey chunk = ChunkKey.from(player.getLocation().getChunk());
        Guild territoryOwner = guildService.getChunkOwner(chunk);
        if (territoryOwner == null) return;

        Guild playerGuild = guildService.getPlayerGuild(player.getUniqueId());
        if (playerGuild == null) return;

        // Only apply protection if player is in their own guild's territory
        if (!playerGuild.getId().equals(territoryOwner.getId())) return;

        if (!buffService.hasBuff(territoryOwner.getId(), category.getId())) {
            return;
        }

        double damageMultiplier = buffService.getBuffValue(territoryOwner.getId(), category.getId(), 1.0);
        // Protection multiplier is like 0.85 for 15% damage reduction
        if (damageMultiplier < 1.0) {
            event.setDamage(event.getDamage() * damageMultiplier);
        }
    }
}
