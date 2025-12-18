package org.aincraft.vault.gui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.aincraft.vault.Vault;
import org.aincraft.vault.VaultRepository;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages shared vault inventories to prevent duplication exploits.
 * All players viewing the same vault share a single Inventory instance.
 */
@Singleton
public class SharedVaultInventoryManager {
    private static final String INVENTORY_TITLE = "Guild Vault";

    private final VaultRepository vaultRepository;
    private final Map<String, SharedVaultInventory> activeInventories = new ConcurrentHashMap<>();

    @Inject
    public SharedVaultInventoryManager(VaultRepository vaultRepository) {
        this.vaultRepository = vaultRepository;
    }

    /**
     * Gets or creates the shared inventory for a vault.
     * If already open by another player, returns the same instance.
     */
    public SharedVaultInventory getOrCreateInventory(Vault vault, boolean canDeposit, boolean canWithdraw) {
        return activeInventories.compute(vault.getId(), (id, existing) -> {
            if (existing != null) {
                // Return existing shared inventory
                return existing;
            }
            // Create new shared inventory with fresh DB contents
            Inventory inv = Bukkit.createInventory(
                    null, // We'll set holder after creation
                    Vault.STORAGE_SIZE,
                    Component.text(INVENTORY_TITLE).color(NamedTextColor.DARK_PURPLE)
            );
            inv.setContents(vaultRepository.getFreshContents(vault.getId()));
            return new SharedVaultInventory(vault, inv, canDeposit, canWithdraw);
        });
    }

    /**
     * Called when a player closes the vault.
     * Saves and removes the inventory if no viewers remain.
     */
    public void onPlayerClose(String vaultId) {
        activeInventories.computeIfPresent(vaultId, (id, shared) -> {
            // Check if any viewers remain
            if (shared.getInventory().getViewers().isEmpty()) {
                // Save to DB and remove from cache
                vaultRepository.updateContents(id, shared.getInventory().getContents());
                return null; // Remove from map
            }
            // Still has viewers, keep in map
            return shared;
        });
    }

    /**
     * Saves the current state of a vault inventory to DB.
     * Called periodically or on significant changes.
     */
    public void saveInventory(String vaultId) {
        SharedVaultInventory shared = activeInventories.get(vaultId);
        if (shared != null) {
            vaultRepository.updateContents(vaultId, shared.getInventory().getContents());
        }
    }

    /**
     * Saves all active vault inventories.
     * Called on plugin disable.
     */
    public void saveAllAndClear() {
        activeInventories.forEach((id, shared) -> {
            vaultRepository.updateContents(id, shared.getInventory().getContents());
        });
        activeInventories.clear();
    }

    /**
     * Gets the shared inventory for a vault if it exists.
     */
    public SharedVaultInventory getInventory(String vaultId) {
        return activeInventories.get(vaultId);
    }

    /**
     * Checks if a vault has an active shared inventory.
     */
    public boolean hasActiveInventory(String vaultId) {
        return activeInventories.containsKey(vaultId);
    }

    /**
     * Wrapper class holding the shared inventory and vault metadata.
     */
    public static class SharedVaultInventory implements InventoryHolder {
        private final Vault vault;
        private final Inventory inventory;
        private final boolean canDeposit;
        private final boolean canWithdraw;

        public SharedVaultInventory(Vault vault, Inventory inventory, boolean canDeposit, boolean canWithdraw) {
            this.vault = vault;
            this.inventory = inventory;
            this.canDeposit = canDeposit;
            this.canWithdraw = canWithdraw;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public Vault getVault() {
            return vault;
        }

        public boolean canDeposit() {
            return canDeposit;
        }

        public boolean canWithdraw() {
            return canWithdraw;
        }
    }
}
