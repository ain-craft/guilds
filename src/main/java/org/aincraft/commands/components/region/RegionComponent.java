package org.aincraft.commands.components.region;

import com.google.inject.Inject;
import org.aincraft.Guild;
import org.aincraft.commands.GuildCommand;
import org.aincraft.commands.MessageFormatter;
import org.aincraft.service.GuildMemberService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Router component for region management commands.
 * Routes subcommands to specialized component handlers.
 * Single Responsibility: Command routing and help display.
 */
public class RegionComponent implements GuildCommand {
    private final RegionSelectionComponent selectionComponent;
    private final RegionBasicComponent basicComponent;
    private final RegionTypeComponent typeComponent;
    private final RegionOwnerComponent ownerComponent;
    private final RegionPermissionComponent permissionComponent;
    private final GuildMemberService memberService;

    @Inject
    public RegionComponent(RegionSelectionComponent selectionComponent, RegionBasicComponent basicComponent,
                          RegionTypeComponent typeComponent, RegionOwnerComponent ownerComponent,
                          RegionPermissionComponent permissionComponent, GuildMemberService memberService) {
        this.selectionComponent = selectionComponent;
        this.basicComponent = basicComponent;
        this.typeComponent = typeComponent;
        this.ownerComponent = ownerComponent;
        this.permissionComponent = permissionComponent;
        this.memberService = memberService;
    }

    @Override
    public String getName() {
        return "region";
    }

    @Override
    public String getPermission() {
        return "guilds.region";
    }

    @Override
    public String getUsage() {
        return "/g region <pos1|pos2|create|cancel|delete|list|info|types|settype|addowner|removeowner|setperm|removeperm|listperms|role|limit>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "Only players can use region commands"));
            return true;
        }

        if (args.length < 2) {
            showHelp(player);
            return true;
        }

        String subCommand = args[1].toLowerCase();

        // Selection workflow commands
        return switch (subCommand) {
            case "pos1" -> selectionComponent.handlePos1(player);
            case "pos2" -> selectionComponent.handlePos2(player);
            case "create" -> selectionComponent.handleCreate(player, args);
            case "cancel" -> selectionComponent.handleCancel(player);

            // Basic CRUD commands
            case "delete" -> basicComponent.handleDelete(player, args);
            case "list" -> basicComponent.handleList(player);
            case "info" -> basicComponent.handleInfo(player, args);
            case "visualize", "show" -> basicComponent.handleVisualize(player, args);

            // Type management commands
            case "types" -> typeComponent.handleTypes(player);
            case "settype" -> typeComponent.handleSetType(player, args);
            case "limit" -> typeComponent.handleLimit(player, args);

            // Owner management commands
            case "addowner" -> ownerComponent.handleAddOwner(player, args);
            case "removeowner" -> ownerComponent.handleRemoveOwner(player, args);

            // Permission management commands
            case "setperm" -> permissionComponent.handleSetPerm(player, args);
            case "removeperm" -> permissionComponent.handleRemovePerm(player, args);
            case "listperms" -> permissionComponent.handleListPerms(player, args);
            case "role" -> {
                Guild guild = memberService.getPlayerGuild(player.getUniqueId());
                if (guild == null) {
                    player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "You are not in a guild"));
                    yield true;
                }
                yield permissionComponent.handleRole(player, guild, args);
            }

            default -> {
                showHelp(player);
                yield true;
            }
        };
    }

    /**
     * Displays help information for region commands.
     */
    private void showHelp(Player player) {
        player.sendMessage(MessageFormatter.format(MessageFormatter.HEADER, "Region Commands", ""));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region create <name> [type]", "Start creating a region"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region pos1", "Set first corner (during creation)"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region pos2", "Set second corner (during creation)"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region cancel", "Cancel region creation"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region delete <name>", "Delete a region"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region list", "List your guild's regions"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region info <name>", "Show region details"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region types", "List available region types"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region settype <name> <type>", "Change region type"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region addowner <region> <player>", "Add region owner"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region removeowner <region> <player>", "Remove region owner"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region setperm <region> player <player> <perms>", "Set player permissions"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region setperm <region> role <role> <perms>", "Set role permissions"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region removeperm <region> player <player>", "Remove player permissions"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region removeperm <region> role <role>", "Remove role permissions"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region listperms <region>", "List region permissions"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region role create <region> <name> <perms>", "Create region role"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region role delete <region> <name>", "Delete region role"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region role list <region>", "List region roles"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region role assign <region> <role> <player>", "Assign player to role"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region role unassign <region> <role> <player>", "Unassign player from role"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region role members <region> <role>", "List role members"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region limit", "List all type volume limits"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region limit <type>", "Show limit and usage for a type"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region limit <type> <volume>", "Set volume limit (op only)"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g region limit <type> remove", "Remove limit (op only)"));
    }
}
