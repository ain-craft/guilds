package org.aincraft.commands.components;

import org.aincraft.Guild;
import org.aincraft.GuildService;
import org.aincraft.LeaveResult;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LeaveComponent command.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LeaveComponent")
class LeaveComponentTest {

    @Mock private GuildService guildService;
    @Mock private Player player;

    private LeaveComponent leaveComponent;
    private UUID playerId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        leaveComponent = new LeaveComponent(guildService);
        playerId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
    }

    @Test
    @DisplayName("should have correct name")
    void shouldHaveCorrectName() {
        assertThat(leaveComponent.getName()).isEqualTo("leave");
    }

    @Test
    @DisplayName("should leave guild successfully")
    void shouldLeaveGuildSuccessfully() {
        Guild guild = new Guild("TestGuild", null, ownerId);
        guild.joinGuild(playerId);

        when(player.hasPermission("guilds.leave")).thenReturn(true);
        when(guildService.getPlayerGuild(playerId)).thenReturn(guild);
        when(guildService.leaveGuild(guild.getId(), playerId)).thenReturn(LeaveResult.success());

        boolean result = leaveComponent.execute(player, new String[]{"leave"});

        assertThat(result).isTrue();
        verify(guildService).leaveGuild(guild.getId(), playerId);
    }

    @Test
    @DisplayName("should fail when not in guild")
    void shouldFailWhenNotInGuild() {
        when(player.hasPermission("guilds.leave")).thenReturn(true);
        when(guildService.getPlayerGuild(playerId)).thenReturn(null);

        boolean result = leaveComponent.execute(player, new String[]{"leave"});

        assertThat(result).isTrue();
        verify(guildService, never()).leaveGuild(anyString(), any());
    }

    @Test
    @DisplayName("should fail when player is owner")
    void shouldFailWhenPlayerIsOwner() {
        Guild guild = new Guild("TestGuild", null, playerId); // Player is owner

        when(player.hasPermission("guilds.leave")).thenReturn(true);
        when(guildService.getPlayerGuild(playerId)).thenReturn(guild);
        when(guildService.leaveGuild(guild.getId(), playerId)).thenReturn(LeaveResult.ownerCannotLeave());

        boolean result = leaveComponent.execute(player, new String[]{"leave"});

        assertThat(result).isTrue();
        verify(guildService).leaveGuild(guild.getId(), playerId);
    }

    @Test
    @DisplayName("should deny when player lacks permission")
    void shouldDenyWhenPlayerLacksPermission() {
        when(player.hasPermission("guilds.leave")).thenReturn(false);

        boolean result = leaveComponent.execute(player, new String[]{"leave"});

        assertThat(result).isTrue();
        verify(guildService, never()).leaveGuild(anyString(), any());
    }
}
