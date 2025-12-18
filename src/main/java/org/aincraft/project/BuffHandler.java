package org.aincraft.project;

import org.aincraft.GuildsPlugin;
import org.bukkit.event.Listener;

/**
 * Interface for handlers that apply buff effects.
 * Handlers are registered and managed by BuffCategoryRegistry.
 * Extend this interface and implement Listener to handle buff-related events.
 */
public interface BuffHandler extends Listener {
    /**
     * Gets the buff category this handler applies.
     *
     * @return the buff category
     */
    RegisterableBuffCategory getCategory();

    /**
     * Called when the handler is registered.
     * Default implementation registers this handler as an event listener.
     * Override to customize registration behavior.
     *
     * @param plugin the Guilds plugin instance
     */
    default void onRegister(GuildsPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Called when the handler is unregistered.
     * Default implementation is empty. Override to handle cleanup.
     */
    default void onUnregister() {
    }
}
