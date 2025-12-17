package org.aincraft.vault.gui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.aincraft.commands.MessageFormatter;
import org.aincraft.vault.VaultService;
import org.aincraft.vault.VaultTransaction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Handles vault GUI interactions with permission checks.
 */
@Singleton
public class VaultGUIListener implements Listener {
    private final VaultService vaultService;

    @Inject
    public VaultGUIListener(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof VaultGUI vaultGUI)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();

        // Determine if this is a deposit or withdraw action
        boolean isVaultInventory = clickedInventory != null && clickedInventory.getHolder() instanceof VaultGUI;
        boolean isPlayerInventory = clickedInventory == player.getInventory();

        switch (event.getAction()) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME -> {
                // Taking from vault = withdraw
                if (isVaultInventory && !vaultGUI.canWithdraw()) {
                    event.setCancelled(true);
                    player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                            "You don't have permission to withdraw from the vault"));
                    return;
                }
                if (isVaultInventory) {
                    logWithdraw(vaultGUI, player, event.getCurrentItem());
                }
            }
            case PLACE_ALL, PLACE_ONE, PLACE_SOME -> {
                // Placing in vault = deposit
                if (isVaultInventory && !vaultGUI.canDeposit()) {
                    event.setCancelled(true);
                    player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                            "You don't have permission to deposit into the vault"));
                    return;
                }
                if (isVaultInventory) {
                    logDeposit(vaultGUI, player, event.getCursor());
                }
            }
            case MOVE_TO_OTHER_INVENTORY -> {
                // Shift-click: direction depends on which inventory was clicked
                if (isPlayerInventory) {
                    // Shift-click from player inventory = deposit
                    if (!vaultGUI.canDeposit()) {
                        event.setCancelled(true);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                                "You don't have permission to deposit into the vault"));
                        return;
                    }
                    logDeposit(vaultGUI, player, event.getCurrentItem());
                } else if (isVaultInventory) {
                    // Shift-click from vault = withdraw
                    if (!vaultGUI.canWithdraw()) {
                        event.setCancelled(true);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                                "You don't have permission to withdraw from the vault"));
                        return;
                    }
                    logWithdraw(vaultGUI, player, event.getCurrentItem());
                }
            }
            case SWAP_WITH_CURSOR -> {
                if (isVaultInventory) {
                    // Swapping requires both permissions
                    if (!vaultGUI.canDeposit() || !vaultGUI.canWithdraw()) {
                        event.setCancelled(true);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                                "You need both deposit and withdraw permissions to swap items"));
                        return;
                    }
                    logDeposit(vaultGUI, player, event.getCursor());
                    logWithdraw(vaultGUI, player, event.getCurrentItem());
                }
            }
            case HOTBAR_SWAP -> {
                if (isVaultInventory) {
                    // Hotbar swap also requires both permissions
                    if (!vaultGUI.canDeposit() || !vaultGUI.canWithdraw()) {
                        event.setCancelled(true);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                                "You need both deposit and withdraw permissions to swap items"));
                        return;
                    }
                    // Log both transactions
                    ItemStack currentItem = event.getCurrentItem();
                    ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                    if (currentItem != null && !currentItem.getType().isAir()) {
                        logWithdraw(vaultGUI, player, currentItem);
                    }
                    if (hotbarItem != null && !hotbarItem.getType().isAir()) {
                        logDeposit(vaultGUI, player, hotbarItem);
                    }
                }
            }
            default -> {
                // Other actions don't need special handling
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof VaultGUI vaultGUI)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Check if any slots are in the vault inventory
        boolean affectsVault = event.getRawSlots().stream()
                .anyMatch(slot -> slot < vaultGUI.getInventory().getSize());

        if (affectsVault && !vaultGUI.canDeposit()) {
            event.setCancelled(true);
            player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                    "You don't have permission to deposit into the vault"));
        } else if (affectsVault) {
            // Log deposit for dragged items
            ItemStack newItems = event.getOldCursor().clone();
            int remaining = event.getCursor() != null ? event.getCursor().getAmount() : 0;
            newItems.setAmount(newItems.getAmount() - remaining);
            if (newItems.getAmount() > 0) {
                logDeposit(vaultGUI, player, newItems);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof VaultGUI vaultGUI)) {
            return;
        }

        // Save vault contents to database
        vaultService.updateVaultContents(
                vaultGUI.getVault().getId(),
                event.getInventory().getContents()
        );
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock()) {
            return;
        }

        if (event.getClickedBlock().getType() != Material.CHEST) {
            return;
        }

        // Check if this is a vault chest
        VaultService.VaultAccessResult result = vaultService.openVaultByChest(
                event.getPlayer(),
                event.getClickedBlock().getLocation()
        );

        if (!result.isVault()) {
            // Not a vault chest, let normal behavior proceed
            return;
        }

        // Cancel the normal chest opening
        event.setCancelled(true);

        if (!result.success()) {
            event.getPlayer().sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                    result.errorMessage()));
            return;
        }

        // Open the vault GUI
        VaultGUI gui = new VaultGUI(result.vault(), result.canDeposit(), result.canWithdraw());
        event.getPlayer().openInventory(gui.getInventory());
    }

    private void logDeposit(VaultGUI gui, Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        VaultTransaction transaction = new VaultTransaction(
                gui.getVault().getId(),
                player.getUniqueId(),
                VaultTransaction.TransactionType.DEPOSIT,
                item.getType(),
                item.getAmount()
        );
        vaultService.logTransaction(transaction);
    }

    private void logWithdraw(VaultGUI gui, Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        VaultTransaction transaction = new VaultTransaction(
                gui.getVault().getId(),
                player.getUniqueId(),
                VaultTransaction.TransactionType.WITHDRAW,
                item.getType(),
                item.getAmount()
        );
        vaultService.logTransaction(transaction);
    }
}
