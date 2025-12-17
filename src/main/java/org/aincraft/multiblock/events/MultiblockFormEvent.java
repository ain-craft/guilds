package org.aincraft.multiblock.events;

import org.aincraft.multiblock.MultiblockInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

/**
 * Fired when a multiblock structure is completed by placing the final block.
 * If cancelled, the multiblock will not be registered/tracked.
 */
public class MultiblockFormEvent extends BlockEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final MultiblockInstance instance;
    private final Player player;
    private boolean cancelled = false;

    public MultiblockFormEvent(Block triggerBlock, MultiblockInstance instance, Player player) {
        super(triggerBlock);
        this.instance = instance;
        this.player = player;
    }

    /**
     * Gets the formed multiblock instance.
     */
    public MultiblockInstance getInstance() {
        return instance;
    }

    /**
     * Gets the pattern ID of the formed multiblock.
     */
    public String getPatternId() {
        return instance.patternId();
    }

    /**
     * Gets the player who placed the final block.
     */
    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
