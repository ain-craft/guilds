package org.aincraft.multiblock;

import org.bukkit.Material;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for multiblock patterns.
 * Open for extension (register custom patterns), closed for modification of built-ins.
 */
public class MultiblockRegistry {
    private final Map<String, MultiblockPattern> patterns = new ConcurrentHashMap<>();
    private final Set<String> builtInIds = new HashSet<>();

    // Cache: material -> pattern IDs that use it (for efficient lookup)
    private final Map<Material, Set<String>> materialIndex = new ConcurrentHashMap<>();

    public MultiblockRegistry() {
        // Built-in patterns will be registered by the vault module
    }

    /**
     * Registers a built-in pattern that cannot be unregistered.
     *
     * @param pattern the pattern to register
     */
    public void registerBuiltIn(MultiblockPattern pattern) {
        Objects.requireNonNull(pattern, "Pattern cannot be null");
        if (patterns.containsKey(pattern.getId())) {
            throw new IllegalArgumentException("Pattern already registered: " + pattern.getId());
        }
        patterns.put(pattern.getId(), pattern);
        builtInIds.add(pattern.getId());
        indexPattern(pattern);
    }

    /**
     * Registers a custom pattern.
     *
     * @param pattern the pattern to register
     * @throws IllegalArgumentException if a pattern with the same ID already exists
     */
    public void register(MultiblockPattern pattern) {
        Objects.requireNonNull(pattern, "Pattern cannot be null");
        if (patterns.containsKey(pattern.getId())) {
            throw new IllegalArgumentException("Pattern already registered: " + pattern.getId());
        }
        patterns.put(pattern.getId(), pattern);
        indexPattern(pattern);
    }

    /**
     * Unregisters a custom pattern.
     * Built-in patterns cannot be unregistered.
     *
     * @param patternId the pattern ID to unregister
     * @return true if the pattern was removed
     */
    public boolean unregister(String patternId) {
        if (patternId == null || builtInIds.contains(patternId)) {
            return false;
        }
        MultiblockPattern removed = patterns.remove(patternId);
        if (removed != null) {
            unindexPattern(removed);
            return true;
        }
        return false;
    }

    private void indexPattern(MultiblockPattern pattern) {
        for (Material mat : pattern.getTriggerMaterials()) {
            materialIndex.computeIfAbsent(mat, k -> ConcurrentHashMap.newKeySet())
                    .add(pattern.getId());
        }
    }

    private void unindexPattern(MultiblockPattern pattern) {
        for (Material mat : pattern.getTriggerMaterials()) {
            Set<String> ids = materialIndex.get(mat);
            if (ids != null) {
                ids.remove(pattern.getId());
            }
        }
    }

    /**
     * Gets a pattern by ID.
     *
     * @param patternId the pattern ID
     * @return the pattern, or empty if not found
     */
    public Optional<MultiblockPattern> getPattern(String patternId) {
        return Optional.ofNullable(patterns.get(patternId));
    }

    /**
     * Gets all registered patterns.
     *
     * @return unmodifiable collection of all patterns
     */
    public Collection<MultiblockPattern> getAllPatterns() {
        return Collections.unmodifiableCollection(patterns.values());
    }

    /**
     * Gets patterns that use this material - efficient for block event handling.
     *
     * @param material the material to look up
     * @return list of patterns that use this material
     */
    public List<MultiblockPattern> getPatternsForMaterial(Material material) {
        Set<String> ids = materialIndex.get(material);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .map(patterns::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Checks if a pattern is registered.
     *
     * @param patternId the pattern ID
     * @return true if registered
     */
    public boolean isRegistered(String patternId) {
        return patternId != null && patterns.containsKey(patternId);
    }

    /**
     * Checks if a pattern is a built-in pattern.
     *
     * @param patternId the pattern ID
     * @return true if built-in
     */
    public boolean isBuiltIn(String patternId) {
        return patternId != null && builtInIds.contains(patternId);
    }

    /**
     * Gets the number of registered patterns.
     *
     * @return pattern count
     */
    public int size() {
        return patterns.size();
    }
}
