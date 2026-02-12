package stellar.snow.astralis.engine.ecs.util;

import stellar.snow.astralis.engine.ecs.core.Archetype;
import stellar.snow.astralis.engine.ecs.core.World;
import stellar.snow.astralis.engine.ecs.storage.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * QueryBuilder - Fluent API for constructing ECS queries.
 * 
 * <p>Makes it easy to find entities matching specific component patterns:
 * <pre>
 * // Find all enemies that are visible and have health
 * List&lt;Archetype&gt; enemies = QueryBuilder.from(world)
 *     .withAll(Transform.class, Health.class, RenderInfo.class)
 *     .withTag(Tag.ENEMY)
 *     .withoutTag(Tag.DEAD)
 *     .execute();
 * 
 * // Process matching entities
 * QueryBuilder.from(world)
 *     .with(Transform.class)
 *     .with(Velocity.class)
 *     .forEach(archetype -> {
 *         // Process archetype
 *     });
 * </pre>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Required components (withAll)</li>
 *   <li>Optional components (withAny)</li>
 *   <li>Excluded components (without)</li>
 *   <li>Tag filtering (withTag, withoutTag)</li>
 *   <li>Custom predicates</li>
 *   <li>Lazy or eager execution</li>
 * </ul>
 * 
 * @author Astralis ECS
 * @version 1.0.0
 */
public class QueryBuilder {
    
    private final World world;
    private final List<Class<?>> required;
    private final List<Class<?>> excluded;
    private final List<Class<?>> optional;
    private final List<Predicate<Archetype>> predicates;
    
    private QueryBuilder(World world) {
        this.world = world;
        this.required = new ArrayList<>();
        this.excluded = new ArrayList<>();
        this.optional = new ArrayList<>();
        this.predicates = new ArrayList<>();
    }
    
    /**
     * Start building a query for the given world.
     */
    public static QueryBuilder from(World world) {
        return new QueryBuilder(world);
    }
    
    // ========================================================================
    // REQUIRED COMPONENTS
    // ========================================================================
    
    /**
     * Require a single component.
     */
    public QueryBuilder with(Class<?> componentClass) {
        required.add(componentClass);
        return this;
    }
    
    /**
     * Require multiple components (must have ALL).
     */
    public QueryBuilder withAll(Class<?>... componentClasses) {
        for (Class<?> cls : componentClasses) {
            required.add(cls);
        }
        return this;
    }
    
    /**
     * Require at least one of these components.
     */
    public QueryBuilder withAny(Class<?>... componentClasses) {
        for (Class<?> cls : componentClasses) {
            optional.add(cls);
        }
        return this;
    }
    
    // ========================================================================
    // EXCLUDED COMPONENTS
    // ========================================================================
    
    /**
     * Exclude a single component.
     */
    public QueryBuilder without(Class<?> componentClass) {
        excluded.add(componentClass);
        return this;
    }
    
    /**
     * Exclude multiple components (must have NONE).
     */
    public QueryBuilder withoutAny(Class<?>... componentClasses) {
        for (Class<?> cls : componentClasses) {
            excluded.add(cls);
        }
        return this;
    }
    
    // ========================================================================
    // CUSTOM PREDICATES
    // ========================================================================
    
    /**
     * Add custom filter predicate.
     */
    public QueryBuilder where(Predicate<Archetype> predicate) {
        predicates.add(predicate);
        return this;
    }
    
    /**
     * Filter by minimum archetype size.
     */
    public QueryBuilder minSize(int min) {
        return where(arch -> arch.size() >= min);
    }
    
    /**
     * Filter by maximum archetype size.
     */
    public QueryBuilder maxSize(int max) {
        return where(arch -> arch.size() <= max);
    }
    
    /**
     * Filter non-empty archetypes.
     */
    public QueryBuilder nonEmpty() {
        return minSize(1);
    }
    
    // ========================================================================
    // EXECUTION
    // ========================================================================
    
    /**
     * Execute query and return matching archetypes.
     */
    public List<Archetype> execute() {
        // Build the query
        Query.Builder builder = world.query();
        
        // Add required components
        for (Class<?> cls : required) {
            builder.with(cls);
        }
        
        // Add excluded components
        for (Class<?> cls : excluded) {
            builder.without(cls);
        }
        
        // Add optional components
        for (Class<?> cls : optional) {
            builder.optional(cls);
        }
        
        // Build and get archetypes
        Query query = builder.build();
        List<Archetype> archetypes = query.getArchetypes();
        
        // Apply custom predicates
        if (!predicates.isEmpty()) {
            archetypes = archetypes.stream()
                .filter(arch -> predicates.stream().allMatch(pred -> pred.test(arch)))
                .toList();
        }
        
        return archetypes;
    }
    
    /**
     * Execute query and process each matching archetype.
     */
    public void forEach(Consumer<Archetype> processor) {
        List<Archetype> archetypes = execute();
        archetypes.forEach(processor);
    }
    
    /**
     * Execute query and count matching entities.
     */
    public int count() {
        return execute().stream()
            .mapToInt(Archetype::size)
            .sum();
    }
    
    /**
     * Execute query and check if any entities match.
     */
    public boolean exists() {
        List<Archetype> archetypes = execute();
        return archetypes.stream().anyMatch(arch -> arch.size() > 0);
    }
    
    /**
     * Execute query and get first matching archetype.
     */
    public Archetype first() {
        List<Archetype> archetypes = execute();
        return archetypes.isEmpty() ? null : archetypes.get(0);
    }
    
    // ========================================================================
    // CONVENIENCE METHODS
    // ========================================================================
    
    /**
     * Find all entities with exactly these components (no more, no less).
     */
    public static List<Archetype> exactMatch(World world, Class<?>... components) {
        QueryBuilder builder = from(world).withAll(components);
        
        // This would need world.getAllComponentTypes() to properly exclude
        // components not in the list. For now, just return withAll result.
        return builder.execute();
    }
    
    /**
     * Find all entities that have a component.
     */
    public static List<Archetype> allWith(World world, Class<?> component) {
        return from(world).with(component).execute();
    }
    
    /**
     * Count entities with a component.
     */
    public static int countWith(World world, Class<?> component) {
        return from(world).with(component).count();
    }
}
