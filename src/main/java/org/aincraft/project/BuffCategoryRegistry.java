package org.aincraft.project;

import org.aincraft.GuildsPlugin;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for buff categories and their handlers.
 * Manages both built-in and custom buff types registered by external plugins.
 * Follows the same pattern as SubregionTypeRegistry for consistency.
 */
public class BuffCategoryRegistry {
    private final Map<String, RegisterableBuffCategory> categories = new ConcurrentHashMap<>();
    private final Map<String, BuffHandler> handlers = new ConcurrentHashMap<>();
    private final Set<String> builtInIds = new HashSet<>();

    public BuffCategoryRegistry() {
        // Built-in categories will be registered via registerBuiltInBuffs() in GuildsPlugin
    }

    /**
     * Registers a built-in buff category.
     * Built-in categories cannot be unregistered.
     *
     * @param category the buff category to register
     * @throws IllegalArgumentException if a category with the same ID already exists
     */
    public void registerBuiltIn(RegisterableBuffCategory category) {
        Objects.requireNonNull(category, "Category cannot be null");
        if (categories.containsKey(category.getId())) {
            throw new IllegalArgumentException("Category already registered: " + category.getId());
        }
        categories.put(category.getId(), category);
        builtInIds.add(category.getId());
    }

    /**
     * Registers a custom buff category.
     *
     * @param category the buff category to register
     * @throws IllegalArgumentException if a category with the same ID already exists
     */
    public void register(RegisterableBuffCategory category) {
        Objects.requireNonNull(category, "Category cannot be null");
        if (categories.containsKey(category.getId())) {
            throw new IllegalArgumentException("Category already registered: " + category.getId());
        }
        categories.put(category.getId(), category);
    }

    /**
     * Unregisters a custom buff category.
     * Built-in categories cannot be unregistered.
     *
     * @param categoryId the category ID to unregister
     * @return true if the category was removed, false if not found or is built-in
     */
    public boolean unregister(String categoryId) {
        if (categoryId == null || builtInIds.contains(categoryId)) {
            return false;
        }
        handlers.remove(categoryId);
        return categories.remove(categoryId) != null;
    }

    /**
     * Registers a handler for a buff category.
     * The handler's onRegister method is called immediately.
     *
     * @param handler the handler to register
     * @throws IllegalArgumentException if the handler's category is not registered
     */
    public void registerHandler(BuffHandler handler) {
        Objects.requireNonNull(handler, "Handler cannot be null");
        String categoryId = handler.getCategory().getId();

        if (!categories.containsKey(categoryId)) {
            throw new IllegalArgumentException("Category not registered: " + categoryId);
        }

        handlers.put(categoryId, handler);
    }

    /**
     * Unregisters a handler for a buff category.
     * The handler's onUnregister method is called.
     *
     * @param categoryId the category ID to unregister the handler for
     * @return true if the handler was removed, false if not found
     */
    public boolean unregisterHandler(String categoryId) {
        Objects.requireNonNull(categoryId, "Category ID cannot be null");
        BuffHandler handler = handlers.remove(categoryId);
        if (handler != null) {
            handler.onUnregister();
            return true;
        }
        return false;
    }

    /**
     * Gets a buff category by ID.
     *
     * @param categoryId the category ID
     * @return the category, or empty if not found
     */
    public Optional<RegisterableBuffCategory> getCategory(String categoryId) {
        if (categoryId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(categories.get(categoryId));
    }

    /**
     * Gets a handler for a buff category.
     *
     * @param categoryId the category ID
     * @return the handler, or empty if not found
     */
    public Optional<BuffHandler> getHandler(String categoryId) {
        if (categoryId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlers.get(categoryId));
    }

    /**
     * Checks if a category is registered.
     *
     * @param categoryId the category ID
     * @return true if registered
     */
    public boolean isRegistered(String categoryId) {
        return categoryId != null && categories.containsKey(categoryId);
    }

    /**
     * Checks if a category is a built-in type.
     *
     * @param categoryId the category ID
     * @return true if built-in
     */
    public boolean isBuiltIn(String categoryId) {
        return categoryId != null && builtInIds.contains(categoryId);
    }

    /**
     * Checks if a handler is registered for a category.
     *
     * @param categoryId the category ID
     * @return true if a handler is registered
     */
    public boolean hasHandler(String categoryId) {
        return categoryId != null && handlers.containsKey(categoryId);
    }

    /**
     * Gets all registered buff categories.
     *
     * @return unmodifiable collection of all categories
     */
    public Collection<RegisterableBuffCategory> getAllCategories() {
        return Collections.unmodifiableCollection(categories.values());
    }

    /**
     * Gets all category IDs.
     *
     * @return unmodifiable set of category IDs
     */
    public Set<String> getCategoryIds() {
        return Collections.unmodifiableSet(categories.keySet());
    }

    /**
     * Gets the number of registered categories.
     *
     * @return category count
     */
    public int size() {
        return categories.size();
    }
}
