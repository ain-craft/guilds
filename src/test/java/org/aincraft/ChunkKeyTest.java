package org.aincraft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for ChunkKey record.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChunkKey")
class ChunkKeyTest {

    @Mock private Chunk chunk;
    @Mock private World world;

    @Test
    @DisplayName("should create from chunk")
    void shouldCreateFromChunk() {
        when(chunk.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(chunk.getX()).thenReturn(5);
        when(chunk.getZ()).thenReturn(-3);

        ChunkKey key = ChunkKey.from(chunk);

        assertThat(key.world()).isEqualTo("world");
        assertThat(key.x()).isEqualTo(5);
        assertThat(key.z()).isEqualTo(-3);
    }

    @Test
    @DisplayName("should be equal when components match")
    void shouldBeEqualWhenComponentsMatch() {
        ChunkKey key1 = new ChunkKey("world", 5, 10);
        ChunkKey key2 = new ChunkKey("world", 5, 10);

        assertThat(key1).isEqualTo(key2);
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when world differs")
    void shouldNotBeEqualWhenWorldDiffers() {
        ChunkKey key1 = new ChunkKey("world", 5, 10);
        ChunkKey key2 = new ChunkKey("nether", 5, 10);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("should not be equal when x differs")
    void shouldNotBeEqualWhenXDiffers() {
        ChunkKey key1 = new ChunkKey("world", 5, 10);
        ChunkKey key2 = new ChunkKey("world", 6, 10);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("should not be equal when z differs")
    void shouldNotBeEqualWhenZDiffers() {
        ChunkKey key1 = new ChunkKey("world", 5, 10);
        ChunkKey key2 = new ChunkKey("world", 5, 11);

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("should handle negative coordinates")
    void shouldHandleNegativeCoordinates() {
        ChunkKey key = new ChunkKey("world", -100, -200);

        assertThat(key.x()).isEqualTo(-100);
        assertThat(key.z()).isEqualTo(-200);
    }
}
