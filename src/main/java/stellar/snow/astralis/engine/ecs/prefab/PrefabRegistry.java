package stellar.snow.astralis.engine.ecs.prefab;

import stellar.snow.astralis.engine.ecs.core.Entity;
import stellar.snow.astralis.engine.ecs.core.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * PrefabRegistry - Template system for reusable entity configurations.
 * 
 * <p>Prefabs are entity templates that can be instantiated multiple times.
 * They define a set of components and their default values.
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Register a prefab
 * registry.register("enemy", builder -> builder
 *     .withTransform(0, 0, 0)
 *     .withHealth(100)
 *     .withVelocity()
 *     .withTag(Tag.ENEMY)
 * );
 * 
 * // Instantiate prefab
 * Entity enemy1 = registry.instantiate(world, "enemy");
 * Entity enemy2 = registry.instantiate(world, "enemy", builder -> builder
 *     .withTransform(10, 0, 0) // Override position
 * );
 * 
 * // Batch instantiate
 * List&lt;Entity&gt; enemies = registry.instantiateMultiple(world, "enemy", 10);
 * </pre>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Reusable entity templates</li>
 *   <li>Component overrides</li>
 *   <li>Batch instantiation</li>
 *   <li>Prefab inheritance</li>
 *   <li>Thread-safe registration</li>
 * </ul>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public class PrefabRegistry {
    
    private final Map<String, Prefab> prefabs;
    
    /**
     * Create empty prefab registry.
     */
    public PrefabRegistry() {
        this.prefabs = new ConcurrentHashMap<>();
    }
    
    // ========================================================================
    // REGISTRATION
    // ========================================================================
    
    /**
     * Register a prefab with a builder function.
     * 
     * @param name Unique prefab name
     * @param builderFn Function that configures the entity builder
     */
    public void register(String name, Consumer<PrefabBuilder> builderFn) {
        PrefabBuilder builder = new PrefabBuilder();
        builderFn.accept(builder);
        prefabs.put(name, builder.build());
    }
    
    /**
     * Register a prefab from another prefab (inheritance).
     * 
     * @param name New prefab name
     * @param baseName Base prefab to inherit from
     * @param overrideFn Function to override/add components
     */
    public void registerExtending(String name, String baseName, Consumer<PrefabBuilder> overrideFn) {
        Prefab base = prefabs.get(baseName);
        if (base == null) {
            throw new IllegalArgumentException("Base prefab not found: " + baseName);
        }
        
        PrefabBuilder builder = new PrefabBuilder();
        
        // Copy base components
        base.components.forEach((type, data) -> builder.components.put(type, data));
        
        // Apply overrides
        overrideFn.accept(builder);
        
        prefabs.put(name, builder.build());
    }
    
    /**
     * Unregister a prefab.
     */
    public void unregister(String name) {
        prefabs.remove(name);
    }
    
    /**
     * Check if prefab exists.
     */
    public boolean has(String name) {
        return prefabs.containsKey(name);
    }
    
    /**
     * Get prefab (for inspection).
     */
    public Prefab get(String name) {
        return prefabs.get(name);
    }
    
    // ========================================================================
    // INSTANTIATION
    // ========================================================================
    
    /**
     * Instantiate a prefab.
     */
    public Entity instantiate(World world, String name) {
        Prefab prefab = prefabs.get(name);
        if (prefab == null) {
            throw new IllegalArgumentException("Prefab not found: " + name);
        }
        
        return prefab.instantiate(world);
    }
    
    /**
     * Instantiate a prefab with component overrides.
     */
    public Entity instantiate(World world, String name, Consumer<PrefabBuilder> overrideFn) {
        Prefab prefab = prefabs.get(name);
        if (prefab == null) {
            throw new IllegalArgumentException("Prefab not found: " + name);
        }
        
        // Create temporary builder with prefab components
        PrefabBuilder builder = new PrefabBuilder();
        prefab.components.forEach((type, data) -> builder.components.put(type, data));
        
        // Apply overrides
        overrideFn.accept(builder);
        
        // Instantiate
        return builder.build().instantiate(world);
    }
    
    /**
     * Instantiate multiple copies of a prefab.
     */
    public List<Entity> instantiateMultiple(World world, String name, int count) {
        Prefab prefab = prefabs.get(name);
        if (prefab == null) {
            throw new IllegalArgumentException("Prefab not found: " + name);
        }
        
        List<Entity> entities = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entities.add(prefab.instantiate(world));
        }
        return entities;
    }
    
    /**
     * Instantiate multiple copies with per-entity customization.
     */
    public List<Entity> instantiateMultiple(World world, String name, int count, 
                                           Consumer<PrefabBuilder> customizeFn) {
        List<Entity> entities = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entities.add(instantiate(world, name, customizeFn));
        }
        return entities;
    }
    
    // ========================================================================
    // UTILITIES
    // ========================================================================
    
    /**
     * Get all registered prefab names.
     */
    public Set<String> getPrefabNames() {
        return Collections.unmodifiableSet(prefabs.keySet());
    }
    
    /**
     * Get number of registered prefabs.
     */
    public int getPrefabCount() {
        return prefabs.size();
    }
    
    /**
     * Clear all prefabs.
     */
    public void clear() {
        prefabs.clear();
    }
    
    // ========================================================================
    // PREFAB CLASS
    // ========================================================================
    
    /**
     * Prefab definition.
     */
    public static class Prefab {
        private final Map<Class<?>, Object> components;
        
        Prefab(Map<Class<?>, Object> components) {
            this.components = new HashMap<>(components);
        }
        
        /**
         * Instantiate this prefab in a world.
         */
        public Entity instantiate(World world) {
            Entity entity = world.createEntity();
            
            for (Map.Entry<Class<?>, Object> entry : components.entrySet()) {
                world.addComponent(entity, entry.getKey(), entry.getValue());
            }
            
            return entity;
        }
        
        /**
         * Get component types in this prefab.
         */
        public Set<Class<?>> getComponentTypes() {
            return Collections.unmodifiableSet(components.keySet());
        }
        
        /**
         * Get component data.
         */
        public Object getComponent(Class<?> type) {
            return components.get(type);
        }
    }
    
    // ========================================================================
    // PREFAB BUILDER
    // ========================================================================
    
    /**
     * Builder for prefabs (similar to EntityBuilder but for templates).
     */
    public static class PrefabBuilder {
        private final Map<Class<?>, Object> components;
        
        PrefabBuilder() {
            this.components = new HashMap<>();
        }
        
        /**
         * Add a component to the prefab.
         */
        public <T> PrefabBuilder with(Class<T> componentClass, T data) {
            components.put(componentClass, data);
            return this;
        }
        
        /**
         * Build the prefab.
         */
        Prefab build() {
            return new Prefab(components);
        }
    }
}
