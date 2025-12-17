package org.aincraft.commands.components;

import org.aincraft.Guild;
import org.aincraft.GuildService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CreateComponent command.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CreateComponent")
class CreateComponentTest {

    @Mock private GuildService guildService;
    @Mock private Player player;

    private CreateComponent createComponent;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        createComponent = new CreateComponent(guildService);
        playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
    }

    @Test
    @DisplayName("should have correct name")
    void shouldHaveCorrectName() {
        assertThat(createComponent.getName()).isEqualTo("create");
    }

    @Test
    @DisplayName("should have correct permission")
    void shouldHaveCorrectPermission() {
        assertThat(createComponent.getPermission()).isEqualTo("guilds.create");
    }

    @Nested
    @DisplayName("execute")
    class Execute {

        @Test
        @DisplayName("should create guild with name only")
        void shouldCreateGuildWithNameOnly() {
            when(player.hasPermission("guilds.create")).thenReturn(true);
            Guild guild = new Guild("TestGuild", null, playerId);
            when(guildService.createGuild(eq("TestGuild"), isNull(), eq(playerId)))
                    .thenReturn(guild);

            boolean result = createComponent.execute(player, new String[]{"create", "TestGuild"});

            assertThat(result).isTrue();
            verify(guildService).createGuild("TestGuild", null, playerId);
        }

        @Test
        @DisplayName("should create guild with name and description")
        void shouldCreateGuildWithNameAndDescription() {
            when(player.hasPermission("guilds.create")).thenReturn(true);
            Guild guild = new Guild("TestGuild", "A cool guild", playerId);
            when(guildService.createGuild(eq("TestGuild"), eq("A cool guild"), eq(playerId)))
                    .thenReturn(guild);

            boolean result = createComponent.execute(player,
                    new String[]{"create", "TestGuild", "A", "cool", "guild"});

            assertThat(result).isTrue();
            verify(guildService).createGuild("TestGuild", "A cool guild", playerId);
        }

        @Test
        @DisplayName("should return false when missing name argument")
        void shouldReturnFalseWhenMissingNameArgument() {
            when(player.hasPermission("guilds.create")).thenReturn(true);

            boolean result = createComponent.execute(player, new String[]{"create"});

            assertThat(result).isFalse();
            verify(guildService, never()).createGuild(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("should deny when player lacks permission")
        void shouldDenyWhenPlayerLacksPermission() {
            when(player.hasPermission("guilds.create")).thenReturn(false);

            boolean result = createComponent.execute(player, new String[]{"create", "TestGuild"});

            assertThat(result).isTrue(); // Command handled, but denied
            verify(guildService, never()).createGuild(anyString(), anyString(), any());
            verify(player, atLeastOnce()).sendMessage(any(net.kyori.adventure.text.Component.class));
        }

        @Test
        @DisplayName("should send error when creation fails")
        void shouldSendErrorWhenCreationFails() {
            when(player.hasPermission("guilds.create")).thenReturn(true);
            when(guildService.createGuild(anyString(), any(), any())).thenReturn(null);

            boolean result = createComponent.execute(player, new String[]{"create", "Duplicate"});

            assertThat(result).isTrue();
            verify(player, atLeastOnce()).sendMessage(any(net.kyori.adventure.text.Component.class));
        }
    }
}
