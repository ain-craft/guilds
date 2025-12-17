package org.aincraft.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.aincraft.ChunkKey;
import org.aincraft.Guild;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for in-memory repository implementations.
 * These tests verify the test doubles work correctly.
 */
@DisplayName("In-Memory Repositories")
class InMemoryRepositoryTest {

    @Nested
    @DisplayName("InMemoryGuildRepository")
    class GuildRepositoryTests {

        private InMemoryGuildRepository repository;
        private UUID ownerId;

        @BeforeEach
        void setUp() {
            repository = new InMemoryGuildRepository();
            ownerId = UUID.randomUUID();
        }

        @Test
        @DisplayName("should save and retrieve guild by ID")
        void shouldSaveAndRetrieveGuildById() {
            Guild guild = new Guild("TestGuild", "desc", ownerId);

            repository.save(guild);
            Optional<Guild> result = repository.findById(guild.getId());

            assertThat(result).contains(guild);
        }

        @Test
        @DisplayName("should find guild by name case-insensitive")
        void shouldFindGuildByNameCaseInsensitive() {
            Guild guild = new Guild("TestGuild", "desc", ownerId);
            repository.save(guild);

            Optional<Guild> result = repository.findByName("TESTGUILD");

            assertThat(result).contains(guild);
        }

        @Test
        @DisplayName("should return empty when guild not found")
        void shouldReturnEmptyWhenGuildNotFound() {
            Optional<Guild> result = repository.findById("nonexistent");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should delete guild")
        void shouldDeleteGuild() {
            Guild guild = new Guild("TestGuild", "desc", ownerId);
            repository.save(guild);

            repository.delete(guild.getId());

            assertThat(repository.findById(guild.getId())).isEmpty();
            assertThat(repository.findByName("TestGuild")).isEmpty();
        }

        @Test
        @DisplayName("should find all guilds")
        void shouldFindAllGuilds() {
            repository.save(new Guild("Guild1", null, UUID.randomUUID()));
            repository.save(new Guild("Guild2", null, UUID.randomUUID()));

            List<Guild> result = repository.findAll();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("should clear all guilds")
        void shouldClearAllGuilds() {
            repository.save(new Guild("Guild1", null, UUID.randomUUID()));
            repository.save(new Guild("Guild2", null, UUID.randomUUID()));

            repository.clear();

            assertThat(repository.size()).isZero();
        }
    }

    @Nested
    @DisplayName("InMemoryPlayerGuildMapping")
    class PlayerGuildMappingTests {

        private InMemoryPlayerGuildMapping mapping;
        private UUID playerId;

        @BeforeEach
        void setUp() {
            mapping = new InMemoryPlayerGuildMapping();
            playerId = UUID.randomUUID();
        }

        @Test
        @DisplayName("should add player to guild")
        void shouldAddPlayerToGuild() {
            mapping.addPlayerToGuild(playerId, "guild123");

            assertThat(mapping.isPlayerInGuild(playerId)).isTrue();
            assertThat(mapping.getPlayerGuildId(playerId)).contains("guild123");
        }

        @Test
        @DisplayName("should remove player from guild")
        void shouldRemovePlayerFromGuild() {
            mapping.addPlayerToGuild(playerId, "guild123");

            mapping.removePlayerFromGuild(playerId);

            assertThat(mapping.isPlayerInGuild(playerId)).isFalse();
            assertThat(mapping.getPlayerGuildId(playerId)).isEmpty();
        }

        @Test
        @DisplayName("should get players in guild")
        void shouldGetPlayersInGuild() {
            UUID player1 = UUID.randomUUID();
            UUID player2 = UUID.randomUUID();
            UUID player3 = UUID.randomUUID();

            mapping.addPlayerToGuild(player1, "guildA");
            mapping.addPlayerToGuild(player2, "guildA");
            mapping.addPlayerToGuild(player3, "guildB");

            assertThat(mapping.getPlayersInGuild("guildA")).containsExactlyInAnyOrder(player1, player2);
        }
    }

    @Nested
    @DisplayName("InMemoryChunkClaimRepository")
    class ChunkClaimRepositoryTests {

        private InMemoryChunkClaimRepository repository;
        private ChunkKey chunk;
        private UUID claimerId;

        @BeforeEach
        void setUp() {
            repository = new InMemoryChunkClaimRepository();
            chunk = new ChunkKey("world", 0, 0);
            claimerId = UUID.randomUUID();
        }

        @Test
        @DisplayName("should claim chunk successfully")
        void shouldClaimChunkSuccessfully() {
            boolean result = repository.claim(chunk, "guild123", claimerId);

            assertThat(result).isTrue();
            assertThat(repository.getOwner(chunk)).contains("guild123");
        }

        @Test
        @DisplayName("should prevent claiming already claimed chunk")
        void shouldPreventClaimingAlreadyClaimedChunk() {
            repository.claim(chunk, "guild123", claimerId);

            boolean result = repository.claim(chunk, "otherGuild", UUID.randomUUID());

            assertThat(result).isFalse();
            assertThat(repository.getOwner(chunk)).contains("guild123");
        }

        @Test
        @DisplayName("should unclaim chunk")
        void shouldUnclaimChunk() {
            repository.claim(chunk, "guild123", claimerId);

            boolean result = repository.unclaim(chunk, "guild123");

            assertThat(result).isTrue();
            assertThat(repository.getOwner(chunk)).isEmpty();
        }

        @Test
        @DisplayName("should not unclaim chunk owned by different guild")
        void shouldNotUnclaimChunkOwnedByDifferentGuild() {
            repository.claim(chunk, "guild123", claimerId);

            boolean result = repository.unclaim(chunk, "otherGuild");

            assertThat(result).isFalse();
            assertThat(repository.getOwner(chunk)).contains("guild123");
        }

        @Test
        @DisplayName("should unclaim all chunks for guild")
        void shouldUnclaimAllChunksForGuild() {
            repository.claim(new ChunkKey("world", 0, 0), "guild123", claimerId);
            repository.claim(new ChunkKey("world", 1, 0), "guild123", claimerId);
            repository.claim(new ChunkKey("world", 2, 0), "otherGuild", UUID.randomUUID());

            repository.unclaimAll("guild123");

            assertThat(repository.getChunkCount("guild123")).isZero();
            assertThat(repository.getChunkCount("otherGuild")).isEqualTo(1);
        }

        @Test
        @DisplayName("should get guild chunks")
        void shouldGetGuildChunks() {
            ChunkKey chunk1 = new ChunkKey("world", 0, 0);
            ChunkKey chunk2 = new ChunkKey("world", 1, 0);

            repository.claim(chunk1, "guild123", claimerId);
            repository.claim(chunk2, "guild123", claimerId);

            List<ChunkKey> result = repository.getGuildChunks("guild123");

            assertThat(result).containsExactlyInAnyOrder(chunk1, chunk2);
        }

        @Test
        @DisplayName("should get chunk count")
        void shouldGetChunkCount() {
            repository.claim(new ChunkKey("world", 0, 0), "guild123", claimerId);
            repository.claim(new ChunkKey("world", 1, 0), "guild123", claimerId);

            int count = repository.getChunkCount("guild123");

            assertThat(count).isEqualTo(2);
        }
    }
}
