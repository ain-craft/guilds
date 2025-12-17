package org.aincraft;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for GuildPermission enum.
 */
@DisplayName("GuildPermission")
class GuildPermissionTest {

    @Test
    @DisplayName("should have unique bit values")
    void shouldHaveUniqueBitValues() {
        int combined = 0;
        for (GuildPermission perm : GuildPermission.values()) {
            // Check no overlap with previously seen permissions
            assertThat(combined & perm.getBit())
                    .as("Permission %s should have unique bit", perm.name())
                    .isZero();
            combined |= perm.getBit();
        }
    }

    @Test
    @DisplayName("should have power of 2 bit values")
    void shouldHavePowerOf2BitValues() {
        for (GuildPermission perm : GuildPermission.values()) {
            int bit = perm.getBit();
            assertThat(bit > 0 && (bit & (bit - 1)) == 0)
                    .as("Permission %s bit %d should be power of 2", perm.name(), bit)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("should combine all permissions correctly")
    void shouldCombineAllPermissionsCorrectly() {
        int all = GuildPermission.all();

        for (GuildPermission perm : GuildPermission.values()) {
            assertThat(all & perm.getBit())
                    .as("all() should include %s", perm.name())
                    .isEqualTo(perm.getBit());
        }
    }

    @Test
    @DisplayName("should have correct default permissions")
    void shouldHaveCorrectDefaultPermissions() {
        int defaults = GuildPermission.defaultPermissions();

        assertThat(defaults & GuildPermission.BUILD.getBit()).isPositive();
        assertThat(defaults & GuildPermission.DESTROY.getBit()).isPositive();
        assertThat(defaults & GuildPermission.INTERACT.getBit()).isPositive();

        // These should NOT be in defaults
        assertThat(defaults & GuildPermission.MANAGE_ROLES.getBit()).isZero();
        assertThat(defaults & GuildPermission.CLAIM.getBit()).isZero();
    }

    @Test
    @DisplayName("should verify specific bit values")
    void shouldVerifySpecificBitValues() {
        assertThat(GuildPermission.BUILD.getBit()).isEqualTo(1);
        assertThat(GuildPermission.DESTROY.getBit()).isEqualTo(2);
        assertThat(GuildPermission.INTERACT.getBit()).isEqualTo(4);
        assertThat(GuildPermission.CLAIM.getBit()).isEqualTo(8);
        assertThat(GuildPermission.UNCLAIM.getBit()).isEqualTo(16);
        assertThat(GuildPermission.INVITE.getBit()).isEqualTo(32);
        assertThat(GuildPermission.KICK.getBit()).isEqualTo(64);
        assertThat(GuildPermission.MANAGE_ROLES.getBit()).isEqualTo(128);
        assertThat(GuildPermission.MANAGE_REGIONS.getBit()).isEqualTo(256);
    }
}
