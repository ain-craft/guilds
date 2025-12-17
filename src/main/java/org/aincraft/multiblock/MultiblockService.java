package org.aincraft.multiblock;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.aincraft.ChunkKey;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service layer coordinating multiblock detection and instance tracking.
 * Tracks instances in-memory only; consumers handle their own persistence.
 */
@Singleton
public class MultiblockService {
    private static final int CHUNK_SHIFT = 4; // Block coordinates to chunk coordinates (right shift 4 = divide by 16)

    private final MultiblockRegistry registry;
    private final MultiblockDetector detector;

    // Active instances: patternId -> (instanceId -> instance)
    private final Map<String, Map<String, MultiblockInstance>> activeInstances = new ConcurrentHashMap<>();

    // Spatial index for fast lookup: chunk -> instance IDs
    private final Map<ChunkKey, Set<String>> chunkIndex = new ConcurrentHashMap<>();

    @Inject
    public MultiblockService(MultiblockRegistry registry) {
        this.registry = registry;
        this.detector = new MultiblockDetector();
    }

    /**
     * Checks if block placement completes any multiblock.
     *
     * @param block the placed block
     * @return list of newly formed multiblocks
     */
    public List<MultiblockInstance> checkFormation(Block block) {
        Material material = block.getType();
        List<MultiblockInstance> formed = new ArrayList<>();

        // Only check patterns that use this material
        for (MultiblockPattern pattern : registry.getPatternsForMaterial(material)) {
            detector.detect(pattern, block).ifPresent(instance -> {
                if (!isAlreadyTracked(instance)) {
                    formed.add(instance);
                }
            });
        }
        return formed;
    }

    /**
     * Checks if block break destroys any tracked multiblock.
     *
     * @param block the block being broken
     * @return list of multiblocks that will be broken
     */
    public List<MultiblockInstance> checkBreaking(Block block) {
        Location loc = block.getLocation();
        List<MultiblockInstance> breaking = new ArrayList<>();

        for (Map<String, MultiblockInstance> instances : activeInstances.values()) {
            for (MultiblockInstance instance : instances.values()) {
                if (instance.containsBlock(loc)) {
                    breaking.add(instance);
                }
            }
        }
        return breaking;
    }

    /**
     * Registers a formed multiblock (called after event is not cancelled).
     *
     * @param instance the instance to track
     */
    public void trackInstance(MultiblockInstance instance) {
        activeInstances
                .computeIfAbsent(instance.patternId(), k -> new ConcurrentHashMap<>())
                .put(instance.instanceId(), instance);

        // Update chunk index
        for (Location loc : instance.blockLocations()) {
            if (loc.getWorld() != null) {
                ChunkKey chunk = new ChunkKey(
                        loc.getWorld().getName(),
                        loc.getBlockX() >> CHUNK_SHIFT,
                        loc.getBlockZ() >> CHUNK_SHIFT
                );
                chunkIndex.computeIfAbsent(chunk, k -> ConcurrentHashMap.newKeySet())
                        .add(instance.instanceId());
            }
        }
    }

    /**
     * Unregisters a broken multiblock.
     *
     * @param instance the instance to untrack
     */
    public void untrackInstance(MultiblockInstance instance) {
        Map<String, MultiblockInstance> instances = activeInstances.get(instance.patternId());
        if (instances != null) {
            instances.remove(instance.instanceId());
        }

        for (Location loc : instance.blockLocations()) {
            if (loc.getWorld() != null) {
                ChunkKey chunk = new ChunkKey(
                        loc.getWorld().getName(),
                        loc.getBlockX() >> CHUNK_SHIFT,
                        loc.getBlockZ() >> CHUNK_SHIFT
                );
                Set<String> chunkInstances = chunkIndex.get(chunk);
                if (chunkInstances != null) {
                    chunkInstances.remove(instance.instanceId());
                }
            }
        }
    }

    /**
     * Gets a multiblock instance at the given location, if any.
     *
     * @param loc the location to check
     * @return the instance, or empty if none found
     */
    public Optional<MultiblockInstance> getInstanceAt(Location loc) {
        for (Map<String, MultiblockInstance> instances : activeInstances.values()) {
            for (MultiblockInstance instance : instances.values()) {
                if (instance.containsBlock(loc)) {
                    return Optional.of(instance);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Gets a multiblock instance by its ID.
     *
     * @param instanceId the instance ID
     * @return the instance, or empty if not found
     */
    public Optional<MultiblockInstance> getInstance(String instanceId) {
        for (Map<String, MultiblockInstance> instances : activeInstances.values()) {
            MultiblockInstance instance = instances.get(instanceId);
            if (instance != null) {
                return Optional.of(instance);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets all instances of a specific pattern type.
     *
     * @param patternId the pattern ID
     * @return unmodifiable collection of instances
     */
    public Collection<MultiblockInstance> getInstances(String patternId) {
        Map<String, MultiblockInstance> instances = activeInstances.get(patternId);
        if (instances == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(instances.values());
    }

    /**
     * Gets all tracked instances across all pattern types.
     *
     * @return unmodifiable collection of all instances
     */
    public Collection<MultiblockInstance> getAllInstances() {
        List<MultiblockInstance> all = new ArrayList<>();
        for (Map<String, MultiblockInstance> instances : activeInstances.values()) {
            all.addAll(instances.values());
        }
        return Collections.unmodifiableCollection(all);
    }

    /**
     * Gets the multiblock registry.
     *
     * @return the registry
     */
    public MultiblockRegistry getRegistry() {
        return registry;
    }

    private boolean isAlreadyTracked(MultiblockInstance newInstance) {
        Map<String, MultiblockInstance> instances = activeInstances.get(newInstance.patternId());
        if (instances == null) {
            return false;
        }

        // Check if same origin already tracked
        for (MultiblockInstance existing : instances.values()) {
            if (locationsEqual(existing.origin(), newInstance.origin())) {
                return true;
            }
        }
        return false;
    }

    private boolean locationsEqual(Location a, Location b) {
        if (a.getWorld() == null || b.getWorld() == null) {
            return false;
        }
        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ()
                && a.getWorld().getName().equals(b.getWorld().getName());
    }
}
