package org.aincraft.vault;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for vault transaction logging.
 */
public interface VaultTransactionRepository {

    /**
     * Logs a transaction.
     */
    void log(VaultTransaction transaction);

    /**
     * Finds recent transactions for a vault.
     *
     * @param vaultId the vault ID
     * @param limit maximum number of transactions to return
     * @return transactions ordered by timestamp descending
     */
    List<VaultTransaction> findByVaultId(String vaultId, int limit);

    /**
     * Finds recent transactions by a player.
     *
     * @param playerId the player UUID
     * @param limit maximum number of transactions to return
     * @return transactions ordered by timestamp descending
     */
    List<VaultTransaction> findByPlayer(UUID playerId, int limit);

    /**
     * Deletes all transactions for a vault.
     */
    void deleteByVaultId(String vaultId);
}
