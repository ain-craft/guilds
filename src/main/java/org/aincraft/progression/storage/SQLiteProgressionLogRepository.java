package org.aincraft.progression.storage;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.aincraft.progression.ProgressionLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * SQLite-based implementation of ProgressionLogRepository.
 */
public class SQLiteProgressionLogRepository implements ProgressionLogRepository {
    private final String connectionString;

    @Inject
    public SQLiteProgressionLogRepository(@Named("databasePath") String dbPath) {
        this.connectionString = "jdbc:sqlite:" + dbPath;
        initializeDatabase();
    }

    private void initializeDatabase() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS progression_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                guild_id TEXT NOT NULL,
                player_id TEXT,
                action TEXT NOT NULL,
                amount BIGINT NOT NULL,
                details TEXT,
                timestamp INTEGER NOT NULL
            );
            CREATE INDEX IF NOT EXISTS idx_progression_log_guild ON progression_logs(guild_id);
            CREATE INDEX IF NOT EXISTS idx_progression_log_player ON progression_logs(player_id);
            CREATE INDEX IF NOT EXISTS idx_progression_log_time ON progression_logs(timestamp);
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             Statement stmt = conn.createStatement()) {
            for (String sql : createTableSQL.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql.trim());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize progression_logs table", e);
        }
    }

    @Override
    public void log(ProgressionLog entry) {
        Objects.requireNonNull(entry, "Log entry cannot be null");

        String insertSQL = """
            INSERT INTO progression_logs
            (guild_id, player_id, action, amount, details, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, entry.guildId());

            if (entry.playerId() != null) {
                pstmt.setString(2, entry.playerId().toString());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }

            pstmt.setString(3, entry.action().name());
            pstmt.setLong(4, entry.amount());
            pstmt.setString(5, entry.details());
            pstmt.setLong(6, entry.timestamp());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to log progression action", e);
        }
    }

    @Override
    public List<ProgressionLog> findByGuild(String guildId, int limit) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");

        String selectSQL = """
            SELECT * FROM progression_logs
            WHERE guild_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        List<ProgressionLog> logs = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setString(1, guildId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                logs.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find progression logs by guild ID", e);
        }

        return logs;
    }

    @Override
    public void deleteByGuildId(String guildId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");

        String deleteSQL = "DELETE FROM progression_logs WHERE guild_id = ?";

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
            pstmt.setString(1, guildId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete progression logs", e);
        }
    }

    private ProgressionLog mapResultSet(ResultSet rs) throws SQLException {
        String playerIdStr = rs.getString("player_id");
        UUID playerId = playerIdStr != null ? UUID.fromString(playerIdStr) : null;

        return new ProgressionLog(
                rs.getLong("id"),
                rs.getString("guild_id"),
                playerId,
                ProgressionLog.ActionType.valueOf(rs.getString("action")),
                rs.getLong("amount"),
                rs.getString("details"),
                rs.getLong("timestamp")
        );
    }
}
