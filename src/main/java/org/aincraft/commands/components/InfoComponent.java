package org.aincraft.commands.components;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.aincraft.Guild;
import org.aincraft.GuildService;
import org.aincraft.RelationshipService;
import org.aincraft.RelationType;
import org.aincraft.RelationStatus;
import org.aincraft.GuildRelationship;
import org.aincraft.commands.GuildCommand;
import org.aincraft.commands.MessageFormatter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Component for viewing guild information.
 */
public class InfoComponent implements GuildCommand {
    private final GuildService guildService;
    private final RelationshipService relationshipService;

    public InfoComponent(GuildService guildService, RelationshipService relationshipService) {
        this.guildService = guildService;
        this.relationshipService = relationshipService;
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getPermission() {
        return "guilds.info";
    }

    @Override
    public String getUsage() {
        return "/g info [guild-name]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "Only players can use this command"));
            return true;
        }

        if (!player.hasPermission(getPermission())) {
            sender.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "You don't have permission to view guild info"));
            return true;
        }

        Guild guild = null;

        if (args.length >= 2) {
            // Look up guild by name
            guild = guildService.getGuildByName(args[1]);
        } else {
            // Use player's current guild
            guild = guildService.getPlayerGuild(player.getUniqueId());
        }

        if (guild == null) {
            if (args.length >= 2) {
                player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "✗ Guild '" + args[1] + "' not found"));
            } else {
                player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "✗ You are not in a guild"));
            }
            return true;
        }

        displayGuildInfo(player, guild);
        return true;
    }

    /**
     * Displays formatted guild information.
     *
     * @param player the player to send the info to
     * @param guild the guild to display
     */
    private void displayGuildInfo(Player player, Guild guild) {
        player.sendMessage(MessageFormatter.format(MessageFormatter.HEADER, "Guild Information", ""));
        player.sendMessage(MessageFormatter.format(MessageFormatter.INFO, "Name", guild.getName()));

        if (guild.getDescription() != null && !guild.getDescription().isEmpty()) {
            player.sendMessage(MessageFormatter.format(MessageFormatter.INFO, "Description", guild.getDescription()));
        }

        // Owner with hover
        Component ownerLine = Component.text()
            .append(Component.text("Owner", NamedTextColor.YELLOW))
            .append(Component.text(": ", NamedTextColor.WHITE))
            .append(createHoverablePlayerName(guild.getOwnerId()))
            .build();
        player.sendMessage(ownerLine);

        player.sendMessage(MessageFormatter.deserialize("<yellow>Members<reset>: <white>" +
            guild.getMemberCount() + "<gray>/<white>" + guild.getMaxMembers()));

        // Created date with hover
        Component createdLine = Component.text()
            .append(Component.text("Created", NamedTextColor.YELLOW))
            .append(Component.text(": ", NamedTextColor.WHITE))
            .append(createHoverableCreatedDate(guild.getCreatedAt()))
            .build();
        player.sendMessage(createdLine);

        // Display toggles
        displayToggles(player, guild);

        // Display relationships
        displayRelationships(player, guild);
    }

    /**
     * Creates a hoverable player name component.
     *
     * @param playerId the UUID of the player
     * @return Component with hover showing player info
     */
    private Component createHoverablePlayerName(UUID playerId) {
        var offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerId);
        String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";

        // Build hover tooltip
        Component tooltip = Component.text()
            .append(Component.text("Player: ", NamedTextColor.YELLOW))
            .append(Component.text(playerName, NamedTextColor.WHITE))
            .append(Component.newline())
            .append(Component.text("UUID: ", NamedTextColor.YELLOW))
            .append(Component.text(playerId.toString(), NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("Status: ", NamedTextColor.YELLOW))
            .append(Component.text(offlinePlayer.isOnline() ? "Online" : "Offline",
                offlinePlayer.isOnline() ? NamedTextColor.GREEN : NamedTextColor.RED))
            .build();

        return Component.text(playerName, NamedTextColor.WHITE)
            .hoverEvent(tooltip);
    }

    /**
     * Creates a hoverable created date component.
     *
     * @param timestamp the creation timestamp
     * @return Component with hover showing detailed date/time
     */
    private Component createHoverableCreatedDate(long timestamp) {
        String dateOnly = new SimpleDateFormat("yyyy-MM-dd").format(new Date(timestamp));
        String fullDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(new Date(timestamp));
        long daysAgo = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24);

        // Build hover tooltip
        Component tooltip = Component.text()
            .append(Component.text("Created: ", NamedTextColor.YELLOW))
            .append(Component.text(fullDateTime, NamedTextColor.WHITE))
            .append(Component.newline())
            .append(Component.text("Days ago: ", NamedTextColor.YELLOW))
            .append(Component.text(String.valueOf(daysAgo), NamedTextColor.GRAY))
            .build();

        return Component.text(dateOnly, NamedTextColor.WHITE)
            .hoverEvent(tooltip);
    }

    /**
     * Displays guild toggle settings and chunk information.
     *
     * @param player the player to send the info to
     * @param guild the guild to display toggles for
     */
    private void displayToggles(Player player, Guild guild) {
        String explosions = guild.isExplosionsAllowed() ? "<green>Enabled</green>" : "<red>Disabled</red>";
        String fire = guild.isFireAllowed() ? "<green>Enabled</green>" : "<red>Disabled</red>";
        String isPublic = guild.isPublic() ? "<green>Public</green>" : "<red>Private</red>";

        player.sendMessage(MessageFormatter.deserialize("<yellow>Explosions<reset>: " + explosions));
        player.sendMessage(MessageFormatter.deserialize("<yellow>Fire Spread<reset>: " + fire));
        player.sendMessage(MessageFormatter.deserialize("<yellow>Access<reset>: " + isPublic));

        int claimedChunks = guildService.getGuildChunkCount(guild.getId());
        int maxChunks = guild.getMaxChunks();
        player.sendMessage(MessageFormatter.deserialize("<yellow>Chunks<reset>: <white>" +
            claimedChunks + "<gray>/<white>" + maxChunks));
    }

    /**
     * Displays guild relationships (allies and enemies).
     *
     * @param player the player to send the info to
     * @param guild the guild to display relationships for
     */
    private void displayRelationships(Player player, Guild guild) {
        List<String> allies = relationshipService.getAllies(guild.getId());
        List<String> enemies = relationshipService.getEnemies(guild.getId());

        // Display allies
        if (!allies.isEmpty()) {
            player.sendMessage(MessageFormatter.deserialize("<yellow>Allies<reset>: <green>" + allies.size()));
            for (String allyGuildId : allies) {
                Guild allyGuild = guildService.getGuildById(allyGuildId);
                if (allyGuild != null) {
                    player.sendMessage(MessageFormatter.deserialize("  <gray>• <green>" + allyGuild.getName()));
                }
            }
        } else {
            player.sendMessage(MessageFormatter.deserialize("<yellow>Allies<reset>: <gray>None"));
        }

        // Display enemies
        if (!enemies.isEmpty()) {
            player.sendMessage(MessageFormatter.deserialize("<yellow>Enemies<reset>: <red>" + enemies.size()));
            for (String enemyGuildId : enemies) {
                Guild enemyGuild = guildService.getGuildById(enemyGuildId);
                if (enemyGuild != null) {
                    player.sendMessage(MessageFormatter.deserialize("  <gray>• <red>" + enemyGuild.getName()));
                }
            }
        } else {
            player.sendMessage(MessageFormatter.deserialize("<yellow>Enemies<reset>: <gray>None"));
        }
    }
}
