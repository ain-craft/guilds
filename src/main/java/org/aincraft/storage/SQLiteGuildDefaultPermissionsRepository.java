package org.aincraft.storage;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;
import org.aincraft.GuildDefaultPermissions;
import org.aincraft.subregion.SubjectType;

/**
 * SQLite implementation of GuildDefaultPermissionsRepository.
 * Single Responsibility: Guild default permissions persistence using SQLite.
 */
public class SQLiteGuildDefaultPermissionsRepository implements GuildDefaultPermissionsRepository {
    private final String connectionString;

    // Default permissions (bitfield values)
    private static final int DEFAULT_ALLY_PERMISSIONS = 4; // INTERACT only
    private static final int DEFAULT_ENEMY_PERMISSIONS = 0;
    private static final int DEFAULT_OUTSIDER_PERMISSIONS = 0;

    @Inject
    public SQLiteGuildDefaultPermissionsRepository(@Named("databasePath") String dbPath) {
        this.connectionString = "jdbc:sqlite:" + dbPath;
        initializeDatabase();
    }

    private void initializeDatabase() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS guild_default_permissions (
                guild_id TEXT PRIMARY KEY,
                ally_permissions INTEGER NOT NULL DEFAULT 4,
                enemy_permissions INTEGER NOT NULL DEFAULT 0,
                outsider_permissions INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (guild_id) REFERENCES guilds(id) ON DELETE CASCADE
            )
            """;

        String createIndexSQL = """
            CREATE INDEX IF NOT EXISTS idx_guild_default_permissions_guild_id
            ON guild_default_permissions(guild_id)
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createIndexSQL);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize guild default permissions database", e);
        }
    }

    @Override
    public void save(GuildDefaultPermissions permissions) {
        Objects.requireNonNull(permissions, "Permissions cannot be null");

        String sql = """
            INSERT OR REPLACE INTO guild_default_permissions
            (guild_id, ally_permissions, enemy_permissions, outsider_permissions, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, permissions.getGuildId());
            pstmt.setInt(2, permissions.getAllyPermissions());
            pstmt.setInt(3, permissions.getEnemyPermissions());
            pstmt.setInt(4, permissions.getOutsiderPermissions());
            pstmt.setLong(5, permissions.getCreatedAt());
            pstmt.setLong(6, permissions.getUpdatedAt());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save guild default permissions", e);
        }
    }

    @Override
    public Optional<GuildDefaultPermissions> findByGuildId(String guildId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");

        String sql = "SELECT * FROM guild_default_permissions WHERE guild_id = ?";

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, guildId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSet(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find guild default permissions", e);
        }
    }

    @Override
    public void delete(String guildId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");

        String sql = "DELETE FROM guild_default_permissions WHERE guild_id = ?";

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, guildId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete guild default permissions", e);
        }
    }

    @Override
    public int getPermissions(String guildId, SubjectType subjectType) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(subjectType, "Subject type cannot be null");

        Optional<GuildDefaultPermissions> perms = findByGuildId(guildId);

        if (perms.isEmpty()) {
            // Return global defaults if guild not found
            return getDefaultPermissions(subjectType);
        }

        return switch (subjectType) {
            case GUILD_ALLY -> perms.get().getAllyPermissions();
            case GUILD_ENEMY -> perms.get().getEnemyPermissions();
            case GUILD_OUTSIDER -> perms.get().getOutsiderPermissions();
            default -> throw new IllegalArgumentException("Invalid relationship subject type: " + subjectType);
        };
    }

    private int getDefaultPermissions(SubjectType subjectType) {
        return switch (subjectType) {
            case GUILD_ALLY -> DEFAULT_ALLY_PERMISSIONS;
            case GUILD_ENEMY -> DEFAULT_ENEMY_PERMISSIONS;
            case GUILD_OUTSIDER -> DEFAULT_OUTSIDER_PERMISSIONS;
            default -> 0;
        };
    }

    private GuildDefaultPermissions mapResultSet(ResultSet rs) throws SQLException {
        return new GuildDefaultPermissions(
            rs.getString("guild_id"),
            rs.getInt("ally_permissions"),
            rs.getInt("enemy_permissions"),
            rs.getInt("outsider_permissions")
        );
    }
}
