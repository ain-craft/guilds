package org.aincraft.vault;

import java.util.UUID;
import org.bukkit.Material;

/**
 * Represents a vault transaction for audit logging.
 */
public record VaultTransaction(
        long id,
        String vaultId,
        UUID playerId,
        TransactionType action,
        Material itemType,
        int amount,
        long timestamp
) {
    /**
     * Creates a new transaction (without ID, for insertion).
     */
    public VaultTransaction(String vaultId, UUID playerId, TransactionType action,
                            Material itemType, int amount) {
        this(0, vaultId, playerId, action, itemType, amount, System.currentTimeMillis());
    }

    public enum TransactionType {
        DEPOSIT,
        WITHDRAW
    }
}
