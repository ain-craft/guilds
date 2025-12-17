package org.aincraft.multiblock.patterns;

import java.util.Map;
import org.aincraft.multiblock.MultiblockPattern;
import org.aincraft.multiblock.MultiblockPatternBuilder;
import org.bukkit.Material;

/**
 * 3x3x3 iron block vault with chest in center.
 *
 * <pre>
 * Layer 0 (bottom):   Layer 1 (middle):   Layer 2 (top):
 * [I] [I] [I]         [I] [I] [I]         [I] [I] [I]
 * [I] [I] [I]         [I] [C] [I]         [I] [I] [I]
 * [I] [I] [I]         [I] [I] [I]         [I] [I] [I]
 *
 * I = IRON_BLOCK (26 total)
 * C = CHEST (interaction point / origin marker)
 * </pre>
 */
public final class GuildVaultPattern {
    public static final String PATTERN_ID = "guild_vault";

    private GuildVaultPattern() {
    }

    public static MultiblockPattern create() {
        Map<Character, Material> legend = Map.of(
                'I', Material.IRON_BLOCK,
                'C', Material.CHEST
        );

        return MultiblockPatternBuilder.create(PATTERN_ID)
                .displayName("Guild Vault")
                .layer(0, legend,
                        "III",
                        "III",
                        "III"
                )
                .layer(1, legend,
                        "III",
                        "ICI",
                        "III"
                )
                .layer(2, legend,
                        "III",
                        "III",
                        "III"
                )
                .rotation(true)
                .mirroring(false)
                .build();
    }
}
