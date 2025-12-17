package org.aincraft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GuildManager.
 * Uses Mockito to mock GuildService and Player objects.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GuildManager")
class GuildManagerTest {

    @Mock private GuildService guildService;
    @Mock private Player player;
    @Mock private Location location;
    @Mock private Chunk chunk;
    @Mock private World world;

    private GuildManager guildManager;
    private UUID playerId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        guildManager = new GuildManager(guildService);
        playerId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("createGuild")
    class CreateGuild {

        @Test
        @DisplayName("should create guild successfully")
        void shouldCreateGuildSuccessfully() {
            Guild expected = new Guild("TestGuild", "desc", ownerId);
            when(guildService.createGuild("TestGuild", "desc", ownerId)).thenReturn(expected);

            Guild result = guildManager.createGuild("TestGuild", "desc", ownerId);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw when service returns null")
        void shouldThrowWhenServiceReturnsNull() {
            when(guildService.createGuild("Existing", "desc", ownerId)).thenReturn(null);

            assertThatThrownBy(() -> guildManager.createGuild("Existing", "desc", ownerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("creation failed");
        }
    }

    @Nested
    @DisplayName("deleteGuild")
    class DeleteGuild {

        @Test
        @DisplayName("should delegate to service")
        void shouldDelegateToService() {
            when(guildService.deleteGuild("guildId", ownerId)).thenReturn(true);

            boolean result = guildManager.deleteGuild("guildId", ownerId);

            assertThat(result).isTrue();
            verify(guildService).deleteGuild("guildId", ownerId);
        }
    }

    @Nested
    @DisplayName("joinGuild")
    class JoinGuild {

        @Test
        @DisplayName("should delegate to service")
        void shouldDelegateToService() {
            when(guildService.joinGuild("guildId", playerId)).thenReturn(true);

            boolean result = guildManager.joinGuild("guildId", playerId);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("leaveGuild")
    class LeaveGuild {

        @Test
        @DisplayName("should leave guild when player is in one")
        void shouldLeaveGuildWhenPlayerIsInOne() {
            Guild guild = new Guild("Test", null, ownerId);
            guild.joinGuild(playerId);

            when(guildService.getPlayerGuild(playerId)).thenReturn(guild);
            when(guildService.leaveGuild(guild.getId(), playerId)).thenReturn(LeaveResult.success());

            boolean result = guildManager.leaveGuild(playerId);

            assertThat(result).isTrue();
            verify(guildService).leaveGuild(guild.getId(), playerId);
        }

        @Test
        @DisplayName("should return false when player not in guild")
        void shouldReturnFalseWhenPlayerNotInGuild() {
            when(guildService.getPlayerGuild(playerId)).thenReturn(null);

            boolean result = guildManager.leaveGuild(playerId);

            assertThat(result).isFalse();
            verify(guildService, never()).leaveGuild(anyString(), any());
        }
    }

    @Nested
    @DisplayName("kickMember")
    class KickMember {

        @Test
        @DisplayName("should kick member by guild ID")
        void shouldKickMemberByGuildId() {
            when(guildService.kickMember("guildId", ownerId, playerId)).thenReturn(true);

            boolean result = guildManager.kickMember("guildId", ownerId, playerId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should kick member using kicker's guild")
        void shouldKickMemberUsingKickersGuild() {
            Guild guild = new Guild("Test", null, ownerId);
            when(guildService.getPlayerGuild(ownerId)).thenReturn(guild);
            when(guildService.kickMember(guild.getId(), ownerId, playerId)).thenReturn(true);

            boolean result = guildManager.kickMember(ownerId, playerId);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when kicker not in guild")
        void shouldReturnFalseWhenKickerNotInGuild() {
            when(guildService.getPlayerGuild(ownerId)).thenReturn(null);

            boolean result = guildManager.kickMember(ownerId, playerId);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getAllGuilds")
    class GetAllGuilds {

        @Test
        @DisplayName("should return all guilds")
        void shouldReturnAllGuilds() {
            List<Guild> guilds = List.of(
                    new Guild("Guild1", null, UUID.randomUUID()),
                    new Guild("Guild2", null, UUID.randomUUID())
            );
            when(guildService.listAllGuilds()).thenReturn(guilds);

            List<Guild> result = guildManager.getAllGuilds();

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getGuildById")
    class GetGuildById {

        @Test
        @DisplayName("should return optional with guild")
        void shouldReturnOptionalWithGuild() {
            Guild guild = new Guild("Test", null, ownerId);
            when(guildService.getGuildById(guild.getId())).thenReturn(guild);

            Optional<Guild> result = guildManager.getGuildById(guild.getId());

            assertThat(result).contains(guild);
        }

        @Test
        @DisplayName("should return empty optional when not found")
        void shouldReturnEmptyOptionalWhenNotFound() {
            when(guildService.getGuildById("nonexistent")).thenReturn(null);

            Optional<Guild> result = guildManager.getGuildById("nonexistent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPlayerGuild")
    class GetPlayerGuild {

        @Test
        @DisplayName("should return optional with player's guild")
        void shouldReturnOptionalWithPlayersGuild() {
            Guild guild = new Guild("Test", null, ownerId);
            when(guildService.getPlayerGuild(ownerId)).thenReturn(guild);

            Optional<Guild> result = guildManager.getPlayerGuild(ownerId);

            assertThat(result).contains(guild);
        }
    }

    @Nested
    @DisplayName("claimChunk")
    class ClaimChunk {

        @BeforeEach
        void setUpPlayerMocks() {
            when(player.getUniqueId()).thenReturn(playerId);
            when(player.getLocation()).thenReturn(location);
            when(location.getChunk()).thenReturn(chunk);
            when(chunk.getWorld()).thenReturn(world);
            when(world.getName()).thenReturn("world");
            when(chunk.getX()).thenReturn(5);
            when(chunk.getZ()).thenReturn(10);
        }

        @Test
        @DisplayName("should claim chunk when player is in guild")
        void shouldClaimChunkWhenPlayerIsInGuild() {
            Guild guild = new Guild("Test", null, playerId);
            when(guildService.getPlayerGuild(playerId)).thenReturn(guild);
            when(guildService.claimChunk(eq(guild.getId()), eq(playerId), any(ChunkKey.class)))
                    .thenReturn(ClaimResult.success());

            ClaimResult result = guildManager.claimChunk(player);

            assertThat(result.isSuccess()).isTrue();
            verify(guildService).claimChunk(eq(guild.getId()), eq(playerId), argThat(
                    key -> key.x() == 5 && key.z() == 10 && key.world().equals("world")
            ));
        }

        @Test
        @DisplayName("should return failure when player not in guild")
        void shouldReturnFailureWhenPlayerNotInGuild() {
            when(guildService.getPlayerGuild(playerId)).thenReturn(null);

            ClaimResult result = guildManager.claimChunk(player);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getStatus()).isEqualTo(ClaimResult.Status.FAILURE);
            verify(guildService, never()).claimChunk(anyString(), any(), any());
        }
    }

    @Nested
    @DisplayName("unclaimChunk")
    class UnclaimChunk {

        @BeforeEach
        void setUpPlayerMocks() {
            when(player.getUniqueId()).thenReturn(playerId);
            when(player.getLocation()).thenReturn(location);
            when(location.getChunk()).thenReturn(chunk);
            when(chunk.getWorld()).thenReturn(world);
            when(world.getName()).thenReturn("world");
            when(chunk.getX()).thenReturn(5);
            when(chunk.getZ()).thenReturn(10);
        }

        @Test
        @DisplayName("should unclaim chunk when player is in guild")
        void shouldUnclaimChunkWhenPlayerIsInGuild() {
            Guild guild = new Guild("Test", null, playerId);
            when(guildService.getPlayerGuild(playerId)).thenReturn(guild);
            when(guildService.unclaimChunk(eq(guild.getId()), eq(playerId), any(ChunkKey.class)))
                    .thenReturn(true);

            boolean result = guildManager.unclaimChunk(player);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when player not in guild")
        void shouldReturnFalseWhenPlayerNotInGuild() {
            when(guildService.getPlayerGuild(playerId)).thenReturn(null);

            boolean result = guildManager.unclaimChunk(player);

            assertThat(result).isFalse();
        }
    }
}
