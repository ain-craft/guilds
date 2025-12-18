package org.aincraft.vault.gui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.aincraft.commands.MessageFormatter;
import org.aincraft.vault.VaultRepository;
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
 * Uses transactional slot operations to prevent duplication exploits
 * when multiple players view the vault simultaneously.
 */
@Singleton
public class VaultGUIListener implements Listener {
    private final VaultService vaultService;
    private final VaultRepository vaultRepository;

    @Inject
    public VaultGUIListener(VaultService vaultService, VaultRepository vaultRepository) {
        this.vaultService = vaultService;
        this.vaultRepository = vaultRepository;
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

        String vaultId = vaultGUI.getVault().getId();
        int slot = event.getSlot();

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
                    // Validate against DB state before allowing
                    ItemStack currentItem = event.getCurrentItem();
                    ItemStack dbItem = vaultRepository.getSlot(vaultId, slot);

                    if (!itemStacksEqual(currentItem, dbItem)) {
                        // Slot state changed - refresh and cancel
                        event.setCancelled(true);
                        refreshInventory(player, vaultGUI);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.WARNING,
                                "Vault contents changed, refreshing..."));
                        return;
                    }

                    // Calculate what will remain after pickup
                    ItemStack remaining = calculateRemainingAfterPickup(event, currentItem);

                    // Atomically update DB
                    if (!vaultRepository.compareAndSetSlot(vaultId, slot, dbItem, remaining)) {
                        event.setCancelled(true);
                        refreshInventory(player, vaultGUI);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.WARNING,
                                "Vault contents changed, refreshing..."));
                        return;
                    }

                    logWithdraw(vaultGUI, player, currentItem, event);
                }
            }
            case PLACE_ALL, PLACE_ONE, PLACE_SOME -> {
                // Placing in vault = deposit
                // Deposits don't need strict CAS - they don't cause duplication
                // Only withdrawals can dupe items, so we just permission check here
                if (isVaultInventory && !vaultGUI.canDeposit()) {
                    event.setCancelled(true);
                    player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                            "You don't have permission to deposit into the vault"));
                    return;
                }
                if (isVaultInventory) {
                    logDeposit(vaultGUI, player, event.getCursor());
                    // Sync to DB after event completes
                    scheduleSync(player, vaultGUI);
                }
            }
            case MOVE_TO_OTHER_INVENTORY -> {
                // Shift-click: direction depends on which inventory was clicked
                if (isPlayerInventory) {
                    // Shift-click from player inventory = deposit
                    // Deposits don't need strict CAS - they don't cause duplication
                    if (!vaultGUI.canDeposit()) {
                        event.setCancelled(true);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                                "You don't have permission to deposit into the vault"));
                        return;
                    }
                    ItemStack movingItem = event.getCurrentItem();
                    if (movingItem != null && !movingItem.getType().isAir()) {
                        logDeposit(vaultGUI, player, movingItem);
                        scheduleSync(player, vaultGUI);
                    }
                } else if (isVaultInventory) {
                    // Shift-click from vault = withdraw
                    if (!vaultGUI.canWithdraw()) {
                        event.setCancelled(true);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.ERROR,
                                "You don't have permission to withdraw from the vault"));
                        return;
                    }

                    ItemStack currentItem = event.getCurrentItem();
                    ItemStack dbItem = vaultRepository.getSlot(vaultId, slot);

                    if (!itemStacksEqual(currentItem, dbItem)) {
                        event.setCancelled(true);
                        refreshInventory(player, vaultGUI);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.WARNING,
                                "Vault contents changed, refreshing..."));
                        return;
                    }

                    // Shift-click removes entire stack
                    if (!vaultRepository.compareAndSetSlot(vaultId, slot, dbItem, null)) {
                        event.setCancelled(true);
                        refreshInventory(player, vaultGUI);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.WARNING,
                                "Vault contents changed, refreshing..."));
                        return;
                    }

                    logWithdraw(vaultGUI, player, currentItem, event);
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

                    // Need CAS for withdraw portion to prevent duping
                    ItemStack currentItem = event.getCurrentItem();
                    ItemStack dbItem = vaultRepository.getSlot(vaultId, slot);

                    if (!itemStacksEqual(currentItem, dbItem)) {
                        event.setCancelled(true);
                        refreshInventory(player, vaultGUI);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.WARNING,
                                "Vault contents changed, refreshing..."));
                        return;
                    }

                    ItemStack cursorItem = event.getCursor();
                    if (!vaultRepository.compareAndSetSlot(vaultId, slot, dbItem, cloneItem(cursorItem))) {
                        event.setCancelled(true);
                        refreshInventory(player, vaultGUI);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.WARNING,
                                "Vault contents changed, refreshing..."));
                        return;
                    }

                    logDeposit(vaultGUI, player, cursorItem);
                    logWithdraw(vaultGUI, player, currentItem, event);
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

                    ItemStack currentItem = event.getCurrentItem();
                    ItemStack dbItem = vaultRepository.getSlot(vaultId, slot);

                    if (!itemStacksEqual(currentItem, dbItem)) {
                        event.setCancelled(true);
                        refreshInventory(player, vaultGUI);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.WARNING,
                                "Vault contents changed, refreshing..."));
                        return;
                    }

                    ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
                    if (!vaultRepository.compareAndSetSlot(vaultId, slot, dbItem, cloneItem(hotbarItem))) {
                        event.setCancelled(true);
                        refreshInventory(player, vaultGUI);
                        player.sendMessage(MessageFormatter.format(MessageFormatter.WARNING,
                                "Vault contents changed, refreshing..."));
                        return;
                    }

                    // Log both transactions
                    if (currentItem != null && !currentItem.getType().isAir()) {
                        logWithdraw(vaultGUI, player, currentItem, event);
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
            // Drag deposits don't need CAS - they don't cause duplication
            ItemStack newItems = event.getOldCursor().clone();
            int remaining = event.getCursor() != null ? event.getCursor().getAmount() : 0;
            newItems.setAmount(newItems.getAmount() - remaining);
            if (newItems.getAmount() > 0) {
                logDeposit(vaultGUI, player, newItems);
            }
            scheduleSync(player, vaultGUI);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof VaultGUI vaultGUI)) {
            return;
        }

        // Final sync on close to catch any edge cases
        syncInventoryToDb(vaultGUI);
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

        // Open the vault GUI with fresh DB contents
        VaultGUI gui = new VaultGUI(result.vault(), result.canDeposit(), result.canWithdraw());
        // Ensure we have latest DB state
        ItemStack[] freshContents = vaultRepository.getFreshContents(result.vault().getId());
        if (freshContents != null) {
            gui.getInventory().setContents(freshContents);
        }
        event.getPlayer().openInventory(gui.getInventory());
    }

    /**
     * Refreshes player's view with current DB state.
     */
    private void refreshInventory(Player player, VaultGUI gui) {
        ItemStack[] freshContents = vaultRepository.getFreshContents(gui.getVault().getId());
        if (freshContents != null) {
            gui.getInventory().setContents(freshContents);
            player.updateInventory();
        }
    }

    /**
     * Syncs current inventory state to DB.
     */
    private void syncInventoryToDb(VaultGUI gui) {
        vaultService.updateVaultContents(
                gui.getVault().getId(),
                gui.getInventory().getContents()
        );
    }

    /**
     * Schedules a sync to DB after the current tick completes.
     */
    private void scheduleSync(Player player, VaultGUI gui) {
        player.getServer().getScheduler().runTask(
                player.getServer().getPluginManager().getPlugin("Guilds"),
                () -> syncInventoryToDb(gui)
        );
    }

    /**
     * Calculates what remains in slot after a pickup action.
     */
    private ItemStack calculateRemainingAfterPickup(InventoryClickEvent event, ItemStack current) {
        if (current == null || current.getType().isAir()) {
            return null;
        }

        return switch (event.getAction()) {
            case PICKUP_ALL -> null;
            case PICKUP_HALF -> {
                int remaining = current.getAmount() / 2;
                if (remaining <= 0) yield null;
                ItemStack result = current.clone();
                result.setAmount(remaining);
                yield result;
            }
            case PICKUP_ONE -> {
                int remaining = current.getAmount() - 1;
                if (remaining <= 0) yield null;
                ItemStack result = current.clone();
                result.setAmount(remaining);
                yield result;
            }
            case PICKUP_SOME -> {
                // PICKUP_SOME happens when cursor has items and can't pick all
                // The amount picked is limited by max stack size
                int maxStack = current.getMaxStackSize();
                int cursorAmount = event.getCursor() != null ? event.getCursor().getAmount() : 0;
                int canPick = maxStack - cursorAmount;
                int remaining = current.getAmount() - canPick;
                if (remaining <= 0) yield null;
                ItemStack result = current.clone();
                result.setAmount(remaining);
                yield result;
            }
            default -> current;
        };
    }

    /**
     * Calculates slot contents after a place action.
     */
    private ItemStack calculateAfterPlace(InventoryClickEvent event, ItemStack current) {
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            return current;
        }

        return switch (event.getAction()) {
            case PLACE_ALL -> {
                if (current == null || current.getType().isAir()) {
                    yield cursor.clone();
                }
                ItemStack result = current.clone();
                result.setAmount(current.getAmount() + cursor.getAmount());
                yield result;
            }
            case PLACE_ONE -> {
                if (current == null || current.getType().isAir()) {
                    ItemStack result = cursor.clone();
                    result.setAmount(1);
                    yield result;
                }
                ItemStack result = current.clone();
                result.setAmount(current.getAmount() + 1);
                yield result;
            }
            case PLACE_SOME -> {
                // PLACE_SOME happens when placing would exceed max stack
                int maxStack = cursor.getMaxStackSize();
                int currentAmount = current != null ? current.getAmount() : 0;
                ItemStack result = cursor.clone();
                result.setAmount(maxStack);
                yield result;
            }
            default -> current;
        };
    }

    private ItemStack cloneItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        return item.clone();
    }

    private boolean itemStacksEqual(ItemStack a, ItemStack b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.getType().isAir() && b.getType().isAir()) return true;
        return a.isSimilar(b) && a.getAmount() == b.getAmount();
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

    private void logWithdraw(VaultGUI gui, Player player, ItemStack item, InventoryClickEvent event) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        // Calculate actual amount withdrawn based on action
        int amount = switch (event.getAction()) {
            case PICKUP_ALL, MOVE_TO_OTHER_INVENTORY -> item.getAmount();
            case PICKUP_HALF -> (item.getAmount() + 1) / 2; // rounds up
            case PICKUP_ONE -> 1;
            case PICKUP_SOME -> {
                int maxStack = item.getMaxStackSize();
                int cursorAmount = event.getCursor() != null ? event.getCursor().getAmount() : 0;
                yield Math.min(item.getAmount(), maxStack - cursorAmount);
            }
            default -> item.getAmount();
        };

        VaultTransaction transaction = new VaultTransaction(
                gui.getVault().getId(),
                player.getUniqueId(),
                VaultTransaction.TransactionType.WITHDRAW,
                item.getType(),
                amount
        );
        vaultService.logTransaction(transaction);
    }
}
