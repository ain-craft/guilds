package org.aincraft.progression.storage;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.aincraft.progression.GuildProgression;

import java.sql.*;
import java.util.*;

/**
 * SQLite implementation of GuildProgressionRepository.
 * Persists guild progression and contribution data.
 */
public class SQLiteGuildProgressionRepository implements GuildProgressionRepository {
    private final String connectionString;

    @Inject
    public SQLiteGuildProgressionRepository(@Named("databasePath") String dbPath) {
        this.connectionString = "jdbc:sqlite:" + dbPath;
        initializeDatabase();
    }

    private void initializeDatabase() {
        String createProgressionTableSQL = """
            CREATE TABLE IF NOT EXISTS guild_progression (
                guild_id TEXT PRIMARY KEY,
                level INTEGER NOT NULL DEFAULT 1,
                current_xp BIGINT NOT NULL DEFAULT 0,
                total_xp_earned BIGINT NOT NULL DEFAULT 0,
                last_levelup_time INTEGER,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            );
            """;

        String createContributionsTableSQL = """
            CREATE TABLE IF NOT EXISTS guild_xp_contributions (
                guild_id TEXT NOT NULL,
                player_id TEXT NOT NULL,
                total_xp_contributed BIGINT NOT NULL DEFAULT 0,
                last_contribution_time INTEGER,
                PRIMARY KEY (guild_id, player_id),
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            );
            """;

        String createIndexSQL = """
            CREATE INDEX IF NOT EXISTS idx_contributions_guild ON guild_xp_contributions(guild_id);
            CREATE INDEX IF NOT EXISTS idx_contributions_player ON guild_xp_contributions(player_id);
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             Statement stmt = conn.createStatement()) {

            stmt.execute(createProgressionTableSQL);
            stmt.execute(createContributionsTableSQL);

            for (String sql : createIndexSQL.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql.trim());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize progression database", e);
        }
    }

    @Override
    public void save(GuildProgression progression) {
        Objects.requireNonNull(progression, "Progression cannot be null");

        String sql = """
            INSERT OR REPLACE INTO guild_progression
            (guild_id, level, current_xp, total_xp_earned, last_levelup_time)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, progression.getGuildId());
            pstmt.setInt(2, progression.getLevel());
            pstmt.setLong(3, progression.getCurrentXp());
            pstmt.setLong(4, progression.getTotalXpEarned());

            if (progression.getLastLevelupTime() != null) {
                pstmt.setLong(5, progression.getLastLevelupTime());
            } else {
                pstmt.setNull(5, Types.BIGINT);
            }

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save guild progression", e);
        }
    }

    @Override
    public Optional<GuildProgression> findByGuildId(String guildId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");

        String sql = "SELECT * FROM guild_progression WHERE guild_id = ?";

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, guildId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRowToProgression(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find guild progression", e);
        }

        return Optional.empty();
    }

    @Override
    public void delete(String guildId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");

        String sql = "DELETE FROM guild_progression WHERE guild_id = ?";

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, guildId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete guild progression", e);
        }
    }

    @Override
    public void recordContribution(String guildId, UUID playerId, long xpAmount) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(playerId, "Player ID cannot be null");

        if (xpAmount <= 0) {
            return; // Don't record zero or negative contributions
        }

        String sql = """
            INSERT INTO guild_xp_contributions (guild_id, player_id, total_xp_contributed, last_contribution_time)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(guild_id, player_id) DO UPDATE SET
                total_xp_contributed = total_xp_contributed + excluded.total_xp_contributed,
                last_contribution_time = excluded.last_contribution_time
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, guildId);
            pstmt.setString(2, playerId.toString());
            pstmt.setLong(3, xpAmount);
            pstmt.setLong(4, System.currentTimeMillis());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to record XP contribution", e);
        }
    }

    @Override
    public Map<UUID, Long> getTopContributors(String guildId, int limit) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");

        String sql = """
            SELECT player_id, total_xp_contributed
            FROM guild_xp_contributions
            WHERE guild_id = ?
            ORDER BY total_xp_contributed DESC
            LIMIT ?
            """;

        Map<UUID, Long> contributors = new LinkedHashMap<>();

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, guildId);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("player_id"));
                long xp = rs.getLong("total_xp_contributed");
                contributors.put(playerId, xp);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get top contributors", e);
        }

        return contributors;
    }

    @Override
    public long getPlayerContribution(String guildId, UUID playerId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(playerId, "Player ID cannot be null");

        String sql = """
            SELECT total_xp_contributed
            FROM guild_xp_contributions
            WHERE guild_id = ? AND player_id = ?
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, guildId);
            pstmt.setString(2, playerId.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getLong("total_xp_contributed");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get player contribution", e);
        }

        return 0L;
    }

    @Override
    public void deleteContributions(String guildId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");

        String sql = "DELETE FROM guild_xp_contributions WHERE guild_id = ?";

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, guildId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete contributions", e);
        }
    }

    private GuildProgression mapRowToProgression(ResultSet rs) throws SQLException {
        String guildId = rs.getString("guild_id");
        int level = rs.getInt("level");
        long currentXp = rs.getLong("current_xp");
        long totalXpEarned = rs.getLong("total_xp_earned");
        long lastLevelupTime = rs.getLong("last_levelup_time");
        Long lastLevelup = rs.wasNull() ? null : lastLevelupTime;

        return new GuildProgression(guildId, level, currentXp, totalXpEarned, lastLevelup);
    }
}
