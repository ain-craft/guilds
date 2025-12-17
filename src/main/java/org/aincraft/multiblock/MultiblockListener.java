package org.aincraft.multiblock;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.aincraft.multiblock.events.MultiblockBreakEvent;
import org.aincraft.multiblock.events.MultiblockFormEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

/**
 * Listens to block events and fires multiblock formation/break events.
 */
@Singleton
public class MultiblockListener implements Listener {
    private final MultiblockService service;

    @Inject
    public MultiblockListener(MultiblockService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        List<MultiblockInstance> formed = service.checkFormation(block);

        for (MultiblockInstance instance : formed) {
            MultiblockFormEvent formEvent = new MultiblockFormEvent(block, instance, player);
            Bukkit.getPluginManager().callEvent(formEvent);

            if (!formEvent.isCancelled()) {
                service.trackInstance(instance);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        List<MultiblockInstance> breaking = service.checkBreaking(block);

        for (MultiblockInstance instance : breaking) {
            MultiblockBreakEvent breakEvent = new MultiblockBreakEvent(block, instance, player);
            Bukkit.getPluginManager().callEvent(breakEvent);

            if (breakEvent.isCancelled()) {
                event.setCancelled(true);
                return;
            }

            service.untrackInstance(instance);
        }
    }
}
