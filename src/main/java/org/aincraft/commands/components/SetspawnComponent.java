package org.aincraft.commands.components;

import org.aincraft.Guild;
import org.aincraft.GuildService;
import org.aincraft.GuildPermission;
import org.aincraft.commands.GuildCommand;
import org.aincraft.commands.MessageFormatter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command component for setting a guild's spawn location.
 * Usage: /g setspawn
 */
public class SetspawnComponent implements GuildCommand {
    private final GuildService guildService;

    public SetspawnComponent(GuildService guildService) {
        this.guildService = guildService;
    }

    @Override
    public String getName() {
        return "setspawn";
    }

    @Override
    public String getPermission() {
        return "guilds.setspawn";
    }

    @Override
    public String getUsage() {
        return "/g setspawn";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "Only players can use this command"));
            return true;
        }

        if (!player.hasPermission(getPermission())) {
            player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "You don't have permission to set guild spawn"));
            return true;
        }

        // Get player's guild
        Guild guild = guildService.getPlayerGuild(player.getUniqueId());
        if (guild == null) {
            player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "✗ You are not in a guild"));
            return true;
        }

        // Check if player has MANAGE_SPAWN permission
        if (!guildService.hasPermission(guild.getId(), player.getUniqueId(), GuildPermission.MANAGE_SPAWN)) {
            player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "✗ You don't have permission to set guild spawn"));
            return true;
        }

        // Attempt to set spawn
        org.bukkit.Location originalLoc = player.getLocation();
        if (guildService.setGuildSpawn(guild.getId(), player.getUniqueId(), originalLoc)) {
            // Check if location was adjusted
            org.bukkit.Location actual = guildService.getGuildSpawnLocation(guild.getId());
            if (actual != null && !isSameLocation(originalLoc, actual)) {
                player.sendMessage(MessageFormatter.deserialize(
                    "<yellow>⚠ Spawn was moved to homeblock at <gold>" +
                    String.format("%.1f, %.1f, %.1f", actual.getX(), actual.getY(), actual.getZ()) +
                    "</gold></yellow>"
                ));
            } else {
                player.sendMessage(MessageFormatter.deserialize(
                    "<green>✓ Guild spawn set at <gold>" +
                    String.format("%.1f, %.1f, %.1f", originalLoc.getX(), originalLoc.getY(), originalLoc.getZ()) +
                    "</gold>!</green>"
                ));
            }
            return true;
        }

        player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
            "✗ Failed to set spawn. Guild must have a homeblock (claim a chunk first)"));
        return true;
    }

    private boolean isSameLocation(org.bukkit.Location loc1, org.bukkit.Location loc2) {
        return Math.abs(loc1.getX() - loc2.getX()) < 0.1 &&
               Math.abs(loc1.getY() - loc2.getY()) < 0.1 &&
               Math.abs(loc1.getZ() - loc2.getZ()) < 0.1;
    }
}
