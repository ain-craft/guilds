package org.aincraft.vault;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Material;

/**
 * SQLite-based implementation of VaultTransactionRepository.
 */
public class SQLiteVaultTransactionRepository implements VaultTransactionRepository {
    private final String connectionString;

    @Inject
    public SQLiteVaultTransactionRepository(@Named("databasePath") String dbPath) {
        this.connectionString = "jdbc:sqlite:" + dbPath;
        initializeDatabase();
    }

    private void initializeDatabase() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS vault_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                vault_id TEXT NOT NULL,
                player_id TEXT NOT NULL,
                action TEXT NOT NULL,
                item_type TEXT NOT NULL,
                item_amount INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            );
            CREATE INDEX IF NOT EXISTS idx_vault_tx_vault ON vault_transactions(vault_id);
            CREATE INDEX IF NOT EXISTS idx_vault_tx_player ON vault_transactions(player_id);
            CREATE INDEX IF NOT EXISTS idx_vault_tx_time ON vault_transactions(timestamp);
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             Statement stmt = conn.createStatement()) {
            for (String sql : createTableSQL.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql.trim());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize vault_transactions table", e);
        }
    }

    @Override
    public void log(VaultTransaction transaction) {
        Objects.requireNonNull(transaction, "Transaction cannot be null");

        String insertSQL = """
            INSERT INTO vault_transactions
            (vault_id, player_id, action, item_type, item_amount, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, transaction.vaultId());
            pstmt.setString(2, transaction.playerId().toString());
            pstmt.setString(3, transaction.action().name());
            pstmt.setString(4, transaction.itemType().name());
            pstmt.setInt(5, transaction.amount());
            pstmt.setLong(6, transaction.timestamp());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to log vault transaction", e);
        }
    }

    @Override
    public List<VaultTransaction> findByVaultId(String vaultId, int limit) {
        Objects.requireNonNull(vaultId, "Vault ID cannot be null");

        String selectSQL = """
            SELECT * FROM vault_transactions
            WHERE vault_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        List<VaultTransaction> transactions = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setString(1, vaultId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                transactions.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find transactions by vault ID", e);
        }

        return transactions;
    }

    @Override
    public List<VaultTransaction> findByPlayer(UUID playerId, int limit) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");

        String selectSQL = """
            SELECT * FROM vault_transactions
            WHERE player_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        List<VaultTransaction> transactions = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setString(1, playerId.toString());
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                transactions.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find transactions by player", e);
        }

        return transactions;
    }

    @Override
    public void deleteByVaultId(String vaultId) {
        Objects.requireNonNull(vaultId, "Vault ID cannot be null");

        String deleteSQL = "DELETE FROM vault_transactions WHERE vault_id = ?";

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            pstmt.setString(1, vaultId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete vault transactions", e);
        }
    }

    private VaultTransaction mapResultSet(ResultSet rs) throws SQLException {
        return new VaultTransaction(
                rs.getLong("id"),
                rs.getString("vault_id"),
                UUID.fromString(rs.getString("player_id")),
                VaultTransaction.TransactionType.valueOf(rs.getString("action")),
                Material.valueOf(rs.getString("item_type")),
                rs.getInt("item_amount"),
                rs.getLong("timestamp")
        );
    }
}
