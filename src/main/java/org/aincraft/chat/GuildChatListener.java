package org.aincraft.chat;

import com.google.inject.Inject;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.aincraft.Guild;
import org.aincraft.GuildPermission;
import org.aincraft.GuildService;
import org.aincraft.RelationshipService;
import org.aincraft.chat.ChatModeService.ChatMode;
import org.aincraft.commands.MessageFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for handling guild and ally chat.
 * Intercepts chat events and routes messages based on player's chat mode.
 */
public class GuildChatListener implements Listener {
    private final ChatModeService chatModeService;
    private final GuildService guildService;
    private final RelationshipService relationshipService;

    @Inject
    public GuildChatListener(ChatModeService chatModeService,
                            GuildService guildService,
                            RelationshipService relationshipService) {
        this.chatModeService = Objects.requireNonNull(chatModeService);
        this.guildService = Objects.requireNonNull(guildService);
        this.relationshipService = Objects.requireNonNull(relationshipService);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        ChatMode mode = chatModeService.getMode(sender.getUniqueId());

        // Don't intercept public chat
        if (mode == ChatMode.PUBLIC) {
            return;
        }

        // Get sender's guild
        Guild guild = guildService.getPlayerGuild(sender.getUniqueId());
        if (guild == null) {
            sender.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                "You are not in a guild. Chat mode reset to public."));
            chatModeService.setMode(sender.getUniqueId(), ChatMode.PUBLIC);
            return;
        }

        // Check permission (owners bypass)
        if (!guild.isOwner(sender.getUniqueId()) &&
            !guildService.hasPermission(guild.getId(), sender.getUniqueId(), GuildPermission.CHAT_GUILD)) {
            sender.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                "You don't have permission to use guild chat."));
            event.setCancelled(true);
            return;
        }

        // Cancel the default chat event
        event.setCancelled(true);

        // Extract message text
        String message = ((net.kyori.adventure.text.TextComponent) event.message()).content();

        // Route based on mode
        if (mode == ChatMode.GUILD) {
            handleGuildChat(sender, guild, message);
        } else if (mode == ChatMode.ALLY) {
            handleAllyChat(sender, guild, message);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        chatModeService.clearMode(event.getPlayer().getUniqueId());
    }

    /**
     * Handles guild chat - sends message to all guild members.
     */
    private void handleGuildChat(Player sender, Guild guild, String message) {
        Component chatMessage = formatGuildChatMessage(sender, guild, message);

        // Send to all online guild members
        Bukkit.getOnlinePlayers().stream()
            .filter(p -> {
                Guild playerGuild = guildService.getPlayerGuild(p.getUniqueId());
                return playerGuild != null && playerGuild.getId().equals(guild.getId());
            })
            .forEach(p -> p.sendMessage(chatMessage));
    }

    /**
     * Handles ally chat - sends message to all members of allied guilds (transitive).
     */
    private void handleAllyChat(Player sender, Guild guild, String message) {
        Component chatMessage = formatAllyChatMessage(sender, guild, message);

        // Get transitive allies
        Set<String> allyGuildIds = getTransitiveAllies(guild.getId());
        allyGuildIds.add(guild.getId()); // Include sender's guild

        // Send to all members of allied guilds
        Bukkit.getOnlinePlayers().stream()
            .filter(p -> {
                Guild playerGuild = guildService.getPlayerGuild(p.getUniqueId());
                return playerGuild != null && allyGuildIds.contains(playerGuild.getId());
            })
            .forEach(p -> p.sendMessage(chatMessage));
    }

    /**
     * Formats a guild chat message.
     * Format: [G] [GuildName] PlayerName: message
     */
    private Component formatGuildChatMessage(Player sender, Guild guild, String message) {
        Component prefix = Component.text("[G] ", NamedTextColor.GREEN);
        Component guildTag = Component.text("[" + guild.getName() + "]", NamedTextColor.GOLD)
            .hoverEvent(HoverEvent.showText(Component.text("Guild: " + guild.getName(), NamedTextColor.YELLOW)));
        Component playerName = Component.text(sender.getName() + ": ", NamedTextColor.WHITE);
        Component msg = Component.text(message, NamedTextColor.GRAY);

        return Component.empty()
            .append(prefix)
            .append(guildTag)
            .append(Component.space())
            .append(playerName)
            .append(msg);
    }

    /**
     * Formats an ally chat message.
     * Format: [A] [GuildName] PlayerName: message
     */
    private Component formatAllyChatMessage(Player sender, Guild guild, String message) {
        Component prefix = Component.text("[A] ", NamedTextColor.AQUA);
        Component guildTag = Component.text("[" + guild.getName() + "]", NamedTextColor.GOLD)
            .hoverEvent(HoverEvent.showText(Component.text("Guild: " + guild.getName(), NamedTextColor.YELLOW)));
        Component playerName = Component.text(sender.getName() + ": ", NamedTextColor.WHITE);
        Component msg = Component.text(message, NamedTextColor.GRAY);

        return Component.empty()
            .append(prefix)
            .append(guildTag)
            .append(Component.space())
            .append(playerName)
            .append(msg);
    }

    /**
     * Gets all transitively allied guilds using BFS.
     * Handles circular alliances via visited set.
     *
     * @param guildId the starting guild ID
     * @return set of all transitively allied guild IDs (excluding the starting guild)
     */
    private Set<String> getTransitiveAllies(String guildId) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(guildId);
        visited.add(guildId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<String> allies = relationshipService.getAllies(current);

            for (String ally : allies) {
                if (!visited.contains(ally)) {
                    visited.add(ally);
                    queue.add(ally);
                }
            }
        }

        visited.remove(guildId); // Remove starting guild
        return visited;
    }
}
