package org.aincraft.claim.events;

import org.aincraft.claim.ClaimState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Event fired when a player exits a guild claim or enters wilderness.
 * Fired for both ownership changes and subregion type changes.
 */
public class PlayerExitClaimEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final ClaimState previousState;
    private final ClaimState newState;
    private final Location from;
    private final Location to;

    public PlayerExitClaimEvent(Player player, ClaimState previousState,
                                ClaimState newState, Location from, Location to) {
        super(player);
        this.previousState = previousState;
        this.newState = newState;
        this.from = from;
        this.to = to;
    }

    /**
     * Gets the previous claim state before the transition.
     */
    public ClaimState getPreviousState() {
        return previousState;
    }

    /**
     * Gets the new claim state after the transition.
     */
    public ClaimState getNewState() {
        return newState;
    }

    /**
     * Gets the location the player is coming from.
     */
    public Location getFrom() {
        return from;
    }

    /**
     * Gets the location the player is moving to.
     */
    public Location getTo() {
        return to;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
