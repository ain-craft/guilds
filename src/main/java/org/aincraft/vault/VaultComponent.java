package org.aincraft.vault;

import org.aincraft.commands.MessageFormatter;
import org.aincraft.vault.gui.VaultGUI;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Command component for vault operations.
 * Handles: /g vault [open|info|log|destroy]
 */
public class VaultComponent {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm");
    private static final int TRANSACTION_PAGE_SIZE = 10;
    private static final int UUID_DISPLAY_LENGTH = 8;

    private final VaultService vaultService;

    public VaultComponent(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "Only players can use vault commands"));
            return true;
        }

        if (args.length < 2) {
            // Default to open
            return handleOpen(player);
        }

        String subCommand = args[1].toLowerCase();

        return switch (subCommand) {
            case "open" -> handleOpen(player);
            case "info" -> handleInfo(player);
            case "log" -> handleLog(player, args);
            case "destroy" -> handleDestroy(player, args);
            default -> {
                showHelp(player);
                yield true;
            }
        };
    }

    private void showHelp(Player player) {
        player.sendMessage(MessageFormatter.format(MessageFormatter.HEADER, "Vault Commands", ""));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g vault", "Open the guild vault"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g vault info", "Show vault information"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g vault log [page]", "View recent transactions"));
        player.sendMessage(MessageFormatter.format(MessageFormatter.USAGE, "/g vault destroy confirm", "Destroy the vault (owner only)"));
    }

    private boolean handleOpen(Player player) {
        VaultService.VaultAccessResult result = vaultService.openVault(player);

        if (!result.success()) {
            player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, result.errorMessage()));
            return true;
        }

        VaultGUI gui = new VaultGUI(result.vault(), result.canDeposit(), result.canWithdraw());
        player.openInventory(gui.getInventory());

        // Show permission info
        StringBuilder perms = new StringBuilder();
        if (result.canDeposit()) perms.append("deposit");
        if (result.canDeposit() && result.canWithdraw()) perms.append(", ");
        if (result.canWithdraw()) perms.append("withdraw");

        player.sendMessage(MessageFormatter.deserialize(
                "<gray>Vault opened. Permissions: <gold>" + perms + "</gold></gray>"));

        return true;
    }

    private boolean handleInfo(Player player) {
        Optional<Vault> vaultOpt = vaultService.getGuildVault(player.getUniqueId());

        if (vaultOpt.isEmpty()) {
            player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "Your guild does not have a vault"));
            player.sendMessage(MessageFormatter.deserialize(
                    "<gray>Build a 3x3x3 iron block structure with a chest in the center inside a bank subregion.</gray>"));
            return true;
        }

        Vault vault = vaultOpt.get();

        player.sendMessage(MessageFormatter.format(MessageFormatter.HEADER, "Guild Vault", ""));
        player.sendMessage(MessageFormatter.format(MessageFormatter.INFO, "Location",
                vault.getWorld() + " at " + vault.getOriginX() + ", " + vault.getOriginY() + ", " + vault.getOriginZ()));
        player.sendMessage(MessageFormatter.format(MessageFormatter.INFO, "Created",
                new Date(vault.getCreatedAt()).toString()));
        player.sendMessage(MessageFormatter.format(MessageFormatter.INFO, "Storage",
                Vault.STORAGE_SIZE + " slots"));

        // Count items in vault
        int itemCount = 0;
        if (vault.getContents() != null) {
            for (var item : vault.getContents()) {
                if (item != null && !item.getType().isAir()) {
                    itemCount++;
                }
            }
        }
        player.sendMessage(MessageFormatter.format(MessageFormatter.INFO, "Used Slots",
                itemCount + "/" + Vault.STORAGE_SIZE));

        return true;
    }

    private boolean handleLog(Player player, String[] args) {
        Optional<Vault> vaultOpt = vaultService.getGuildVault(player.getUniqueId());

        if (vaultOpt.isEmpty()) {
            player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "Your guild does not have a vault"));
            return true;
        }

        Vault vault = vaultOpt.get();
        int page = 1;
        if (args.length >= 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        int offset = (page - 1) * TRANSACTION_PAGE_SIZE;

        List<VaultTransaction> transactions = vaultService.getRecentTransactions(vault.getId(), TRANSACTION_PAGE_SIZE * page);

        if (transactions.isEmpty()) {
            player.sendMessage(MessageFormatter.format(MessageFormatter.WARNING, "No transactions recorded"));
            return true;
        }

        player.sendMessage(MessageFormatter.format(MessageFormatter.HEADER, "Vault Transactions", " (Page " + page + ")"));

        int shown = 0;
        for (int i = offset; i < transactions.size() && shown < TRANSACTION_PAGE_SIZE; i++) {
            VaultTransaction tx = transactions.get(i);
            String playerName = org.bukkit.Bukkit.getOfflinePlayer(tx.playerId()).getName();
            if (playerName == null) playerName = tx.playerId().toString().substring(0, UUID_DISPLAY_LENGTH);

            String action = tx.action() == VaultTransaction.TransactionType.DEPOSIT ? "<green>+</green>" : "<red>-</red>";
            String itemName = tx.itemType().name().toLowerCase().replace("_", " ");
            String time = DATE_FORMAT.format(new Date(tx.timestamp()));

            player.sendMessage(MessageFormatter.deserialize(
                    "<gray>" + time + " " + action + " <gold>" + tx.amount() + "x " + itemName + "</gold> by " + playerName + "</gray>"));
            shown++;
        }

        if (transactions.size() > page * TRANSACTION_PAGE_SIZE) {
            player.sendMessage(MessageFormatter.deserialize(
                    "<gray>Use <yellow>/g vault log " + (page + 1) + "</yellow> for more</gray>"));
        }

        return true;
    }

    private boolean handleDestroy(Player player, String[] args) {
        Optional<Vault> vaultOpt = vaultService.getGuildVault(player.getUniqueId());

        if (vaultOpt.isEmpty()) {
            player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "Your guild does not have a vault"));
            return true;
        }

        Vault vault = vaultOpt.get();

        if (!vaultService.canDestroyVault(player.getUniqueId(), vault)) {
            player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR, "Only the guild owner can destroy the vault"));
            return true;
        }

        // Require confirmation
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            player.sendMessage(MessageFormatter.format(MessageFormatter.WARNING,
                    "This will destroy the vault and drop all items!"));
            player.sendMessage(MessageFormatter.deserialize(
                    "<gray>Type <yellow>/g vault destroy confirm</yellow> to confirm.</gray>"));
            return true;
        }

        // Drop items
        Location dropLoc = new Location(
                org.bukkit.Bukkit.getWorld(vault.getWorld()),
                vault.getOriginX(),
                vault.getOriginY(),
                vault.getOriginZ()
        );

        if (dropLoc.getWorld() != null && vault.getContents() != null) {
            for (var item : vault.getContents()) {
                if (item != null && !item.getType().isAir()) {
                    dropLoc.getWorld().dropItemNaturally(dropLoc, item);
                }
            }
        }

        vaultService.destroyVault(vault);
        player.sendMessage(MessageFormatter.format(MessageFormatter.SUCCESS,
                "Vault destroyed. All items have been dropped at the vault location."));

        return true;
    }
}
