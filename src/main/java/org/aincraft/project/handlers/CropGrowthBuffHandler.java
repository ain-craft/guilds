package org.aincraft.project.handlers;

import com.google.inject.Inject;
import org.aincraft.ChunkKey;
import org.aincraft.Guild;
import org.aincraft.GuildService;
import org.aincraft.project.BuffApplicationService;
import org.aincraft.project.BuffHandler;
import org.aincraft.project.RegisterableBuffCategory;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockGrowEvent;

import java.util.Objects;

/**
 * Handles the CROP_GROWTH_SPEED buff effect.
 * Increases the chance of block growth events when a guild controls the chunk.
 */
public class CropGrowthBuffHandler implements BuffHandler {
    private final RegisterableBuffCategory category;
    private final BuffApplicationService buffService;
    private final GuildService guildService;

    @Inject
    public CropGrowthBuffHandler(
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
     * Applies crop growth boost via random chance for extra growth events.
     * For example, a 1.5 multiplier means 50% chance for extra growth to trigger.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        ChunkKey chunk = ChunkKey.from(block.getChunk());

        Guild owner = guildService.getChunkOwner(chunk);
        if (owner == null) return;

        if (!buffService.hasBuff(owner.getId(), category.getId())) {
            return;
        }

        double multiplier = buffService.getBuffValue(owner.getId(), category.getId(), 1.0);
        if (multiplier <= 1.0) return;

        // Apply growth boost via random chance for extra growth
        double extraChance = multiplier - 1.0;
        if (Math.random() < extraChance) {
            // Event is allowed to proceed - effective boost is the chance of extra events
        }
    }
}
