package org.aincraft.vault;

import java.util.Optional;
import org.bukkit.inventory.ItemStack;

/**
 * Repository interface for vault persistence.
 */
public interface VaultRepository {

    /**
     * Saves a vault (insert or update).
     */
    void save(Vault vault);

    /**
     * Deletes a vault by ID.
     */
    void delete(String vaultId);

    /**
     * Finds a vault by its ID.
     */
    Optional<Vault> findById(String vaultId);

    /**
     * Finds a vault by guild ID.
     * Each guild can only have one vault.
     */
    Optional<Vault> findByGuildId(String guildId);

    /**
     * Finds a vault by its origin location.
     */
    Optional<Vault> findByLocation(String world, int x, int y, int z);

    /**
     * Updates only the storage contents of a vault.
     */
    void updateContents(String vaultId, ItemStack[] contents);

    /**
     * Checks if a guild already has a vault.
     */
    boolean existsForGuild(String guildId);

    /**
     * Gets the current item at a specific slot.
     * Used for validating slot state before operations.
     */
    ItemStack getSlot(String vaultId, int slot);

    /**
     * Atomically updates a single slot if it matches expected state.
     * Returns true if update succeeded, false if slot state changed.
     */
    boolean compareAndSetSlot(String vaultId, int slot, ItemStack expected, ItemStack newItem);

    /**
     * Gets fresh contents from database (bypasses any caching).
     */
    ItemStack[] getFreshContents(String vaultId);
}
