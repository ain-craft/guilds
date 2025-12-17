package org.aincraft.storage;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.aincraft.GuildRelationship;
import org.aincraft.RelationType;
import org.aincraft.RelationStatus;

import java.sql.*;
import java.util.*;

/**
 * SQLite implementation of GuildRelationshipRepository.
 * Single Responsibility: Guild relationship persistence using SQLite.
 */
public class SQLiteGuildRelationshipRepository implements GuildRelationshipRepository {
    private final String connectionString;

    @Inject
    public SQLiteGuildRelationshipRepository(@Named("databasePath") String dbPath) {
        this.connectionString = "jdbc:sqlite:" + dbPath;
        initializeDatabase();
    }

    private void initializeDatabase() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS guild_relationships (
                id TEXT PRIMARY KEY,
                source_guild_id TEXT NOT NULL,
                target_guild_id TEXT NOT NULL,
                relation_type TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                created_by TEXT NOT NULL,
                UNIQUE(source_guild_id, target_guild_id)
            )
            """;

        String createIndexSQL1 = """
            CREATE INDEX IF NOT EXISTS idx_guild_relationships_source
            ON guild_relationships(source_guild_id)
            """;

        String createIndexSQL2 = """
            CREATE INDEX IF NOT EXISTS idx_guild_relationships_target
            ON guild_relationships(target_guild_id)
            """;

        String createIndexSQL3 = """
            CREATE INDEX IF NOT EXISTS idx_guild_relationships_type_status
            ON guild_relationships(relation_type, status)
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createIndexSQL1);
            stmt.execute(createIndexSQL2);
            stmt.execute(createIndexSQL3);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize guild relationships database", e);
        }
    }

    @Override
    public void save(GuildRelationship relationship) {
        Objects.requireNonNull(relationship, "Relationship cannot be null");

        String sql = """
            INSERT OR REPLACE INTO guild_relationships
            (id, source_guild_id, target_guild_id, relation_type, status, created_at, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, relationship.getId());
            pstmt.setString(2, relationship.getSourceGuildId());
            pstmt.setString(3, relationship.getTargetGuildId());
            pstmt.setString(4, relationship.getRelationType().name());
            pstmt.setString(5, relationship.getStatus().name());
            pstmt.setLong(6, relationship.getCreatedAt());
            pstmt.setString(7, relationship.getCreatedBy().toString());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save guild relationship", e);
        }
    }

    @Override
    public void delete(String relationshipId) {
        Objects.requireNonNull(relationshipId, "Relationship ID cannot be null");

        String sql = "DELETE FROM guild_relationships WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, relationshipId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete guild relationship", e);
        }
    }

    @Override
    public Optional<GuildRelationship> findById(String id) {
        Objects.requireNonNull(id, "ID cannot be null");

        String sql = "SELECT * FROM guild_relationships WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSet(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find guild relationship by ID", e);
        }
    }

    @Override
    public Optional<GuildRelationship> findRelationship(String guildId1, String guildId2) {
        Objects.requireNonNull(guildId1, "Guild ID 1 cannot be null");
        Objects.requireNonNull(guildId2, "Guild ID 2 cannot be null");

        String sql = """
            SELECT * FROM guild_relationships
            WHERE ((source_guild_id = ? AND target_guild_id = ?)
               OR (source_guild_id = ? AND target_guild_id = ?))
            AND status = ?
            LIMIT 1
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, guildId1);
            pstmt.setString(2, guildId2);
            pstmt.setString(3, guildId2);
            pstmt.setString(4, guildId1);
            pstmt.setString(5, RelationStatus.ACTIVE.name());

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSet(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find relationship between guilds", e);
        }
    }

    @Override
    public List<GuildRelationship> findAllByGuild(String guildId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");

        String sql = """
            SELECT * FROM guild_relationships
            WHERE source_guild_id = ? OR target_guild_id = ?
            """;

        List<GuildRelationship> relationships = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, guildId);
            pstmt.setString(2, guildId);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                relationships.add(mapResultSet(rs));
            }

            return relationships;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find relationships for guild", e);
        }
    }

    @Override
    public List<GuildRelationship> findByType(String guildId, RelationType type, RelationStatus status) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");
        Objects.requireNonNull(type, "Relation type cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");

        String sql = """
            SELECT * FROM guild_relationships
            WHERE (source_guild_id = ? OR target_guild_id = ?)
            AND relation_type = ?
            AND status = ?
            """;

        List<GuildRelationship> relationships = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, guildId);
            pstmt.setString(2, guildId);
            pstmt.setString(3, type.name());
            pstmt.setString(4, status.name());

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                relationships.add(mapResultSet(rs));
            }

            return relationships;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find relationships by type", e);
        }
    }

    @Override
    public void deleteAllByGuild(String guildId) {
        Objects.requireNonNull(guildId, "Guild ID cannot be null");

        String sql = """
            DELETE FROM guild_relationships
            WHERE source_guild_id = ? OR target_guild_id = ?
            """;

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, guildId);
            pstmt.setString(2, guildId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete all relationships for guild", e);
        }
    }

    private GuildRelationship mapResultSet(ResultSet rs) throws SQLException {
        return new GuildRelationship(
            rs.getString("id"),
            rs.getString("source_guild_id"),
            rs.getString("target_guild_id"),
            RelationType.valueOf(rs.getString("relation_type")),
            RelationStatus.valueOf(rs.getString("status")),
            rs.getLong("created_at"),
            UUID.fromString(rs.getString("created_by"))
        );
    }
}
