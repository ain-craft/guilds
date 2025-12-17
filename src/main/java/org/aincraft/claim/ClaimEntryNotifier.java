package org.aincraft.claim;

import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.aincraft.claim.events.PlayerEnterClaimEvent;
import org.aincraft.subregion.SubregionTypeRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Objects;

/**
 * Displays action bar notifications when players enter/exit guild claims.
 * Shows ownership changes, type changes, and transitions between guilds/wilderness.
 */
public class ClaimEntryNotifier implements Listener {
    private final SubregionTypeRegistry typeRegistry;

    @Inject
    public ClaimEntryNotifier(SubregionTypeRegistry typeRegistry) {
        this.typeRegistry = Objects.requireNonNull(typeRegistry, "Type registry cannot be null");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClaimEnter(PlayerEnterClaimEvent event) {
        Player player = event.getPlayer();
        ClaimState newState = event.getNewState();
        ClaimState previousState = event.getPreviousState();

        // Build message based on transition type
        Component message = buildMessage(newState, previousState);
        if (message != null) {
            player.sendActionBar(message);
        }
    }

    /**
     * Constructs message based on ownership/type change.
     * Returns null if no meaningful change detected.
     */
    private Component buildMessage(ClaimState newState, ClaimState previousState) {
        boolean ownershipChanged = newState.ownershipChangedFrom(previousState);
        boolean typeChanged = newState.typeChangedFrom(previousState);

        if (!ownershipChanged && !typeChanged) {
            return null; // No relevant change
        }

        // Both ownership and type changed
        if (ownershipChanged && typeChanged) {
            return buildDualChangeMessage(newState, previousState);
        }

        // Only ownership changed
        if (ownershipChanged) {
            return buildOwnershipChangeMessage(newState, previousState);
        }

        // Only type changed (entered/exited subregion of same guild)
        return buildTypeChangeMessage(newState);
    }

    /**
     * Builds message for ownership change only.
     * Format: "Entering GuildName" or "Entering Wilderness"
     */
    private Component buildOwnershipChangeMessage(ClaimState newState, ClaimState previousState) {
        if (newState.guildId() == null) {
            // Exiting to wilderness
            return Component.text("Entering ", NamedTextColor.GRAY)
                    .append(Component.text("Wilderness", NamedTextColor.GRAY, TextDecoration.BOLD));
        } else {
            // Entering guild territory
            return Component.text("Entering ", NamedTextColor.GRAY)
                    .append(Component.text(newState.displayName(), NamedTextColor.GOLD, TextDecoration.BOLD));
        }
    }

    /**
     * Builds message for type change only.
     * Format: "Entering [Type]: GuildName"
     */
    private Component buildTypeChangeMessage(ClaimState newState) {
        if (newState.subregionType() == null) {
            return null; // Exiting subregion - less critical
        }

        // Get type display name
        String typeName = typeRegistry.getType(newState.subregionType())
                .map(t -> t.getDisplayName())
                .orElse(newState.subregionType());

        return Component.text("Entering ", NamedTextColor.GRAY)
                .append(Component.text("[" + typeName + "]", NamedTextColor.AQUA))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(newState.displayName(), NamedTextColor.YELLOW));
    }

    /**
     * Builds message for both ownership and type changes.
     * Format: "GuildA → GuildB [Type]"
     */
    private Component buildDualChangeMessage(ClaimState newState, ClaimState previousState) {
        Component result;

        // Show previous state
        if (previousState != null && previousState.guildId() != null) {
            result = Component.text(previousState.displayName(), NamedTextColor.DARK_RED);
        } else if (previousState != null) {
            result = Component.text("Wilderness", NamedTextColor.DARK_RED);
        } else {
            result = Component.empty();
        }

        // Add arrow
        result = result.append(Component.text(" → ", NamedTextColor.GRAY));

        // Show new state
        if (newState.guildId() == null) {
            result = result.append(Component.text("Wilderness", NamedTextColor.GRAY, TextDecoration.BOLD));
        } else {
            result = result.append(Component.text(newState.displayName(), NamedTextColor.GOLD, TextDecoration.BOLD));
        }

        // Add type if present
        if (newState.subregionType() != null) {
            String typeName = typeRegistry.getType(newState.subregionType())
                    .map(t -> t.getDisplayName())
                    .orElse(newState.subregionType());
            result = result.append(Component.text(" [", NamedTextColor.GRAY));
            result = result.append(Component.text(typeName, NamedTextColor.AQUA));
            result = result.append(Component.text("]", NamedTextColor.GRAY));
        }

        return result;
    }
}
