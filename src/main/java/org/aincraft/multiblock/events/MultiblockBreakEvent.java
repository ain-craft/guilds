package org.aincraft.multiblock.events;

import org.aincraft.multiblock.MultiblockInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockEvent;

/**
 * Fired when a multiblock structure is about to be broken.
 * If cancelled, the block break is prevented and the multiblock remains intact.
 */
public class MultiblockBreakEvent extends BlockEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final MultiblockInstance instance;
    private final Player player;
    private boolean cancelled = false;

    public MultiblockBreakEvent(Block brokenBlock, MultiblockInstance instance, Player player) {
        super(brokenBlock);
        this.instance = instance;
        this.player = player;
    }

    /**
     * Gets the multiblock instance being broken.
     */
    public MultiblockInstance getInstance() {
        return instance;
    }

    /**
     * Gets the pattern ID of the multiblock being broken.
     */
    public String getPatternId() {
        return instance.patternId();
    }

    /**
     * Gets the player breaking the block.
     * May be null if broken by non-player means.
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
