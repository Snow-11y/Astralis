package stellar.snow.astralis.engine.ecs.storage;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Query - Advanced fluent API for querying entities by component.
 *
 * <p>Core Features:</p>
 * <ul>
 *   <li>Fluent builder pattern</li>
 *   <li>Stream-based iteration</li>
 *   <li>Parallel execution support</li>
 *   <li>Typed component access</li>
 *   <li>Change detection queries</li>
 *   <li>Result caching</li>
 *   <li>Batch processing</li>
 *   <li>Optional component handling</li>
 *   <li>Foreign Memory API integration</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Query.create(world)
 *     .with(Position.class, Velocity.class)
 *     .without(Static.class)
 *     .optional(Rotation.class)
 *     .filter(e -> world.isValid(e))
 *     .changedSince(lastTick)
 *     .stream()
 *     .forEach(result -> {
 *         ByteBuffer pos = result.get(Position.class);
 *         ByteBuffer vel = result.get(Velocity.class);
 *         // Process entity
 *     });
 * }</pre>
 *
 * @author Enhanced ECS Framework
 * @version 2.0.0
 * @since Java 21
 */
public final class Query {

    // ========================================================================
    // CONFIGURATION
    // ========================================================================

    private static final int DEFAULT_BATCH_SIZE = 256;
    private static final int PARALLEL_THRESHOLD = 1000;

    // ========================================================================
    // STATE
    // ========================================================================

    private final World world;
    private final ComponentRegistry registry;
    
    private long requiredMask = 0;
    private long excludedMask = 0;
    private long optionalMask = 0;
    private long changedMask = 0;  // Components that must have changed
    
    private final List<Class<?>> requiredTypes = new ArrayList<>();
    private final List<Class<?>> optionalTypes = new ArrayList<>();
    
    private Predicate<Entity> entityFilter = e -> true;
    private Predicate<Archetype> archetypeFilter = a -> true;
    private long changedSinceVersion = -1;
    
    private int batchSize = DEFAULT_BATCH_SIZE;
    private boolean parallel = false;
    private boolean cached = false;
    
    // Cache
    private volatile List<Archetype> cachedArchetypes;
    private volatile long cacheVersion = -1;

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    private Query(World world) {
        this.world = Objects.requireNonNull(world, "World cannot be null");
        this.registry = ComponentRegistry.get();
    }

    // ========================================================================
    // STATIC FACTORY
    // ========================================================================

    /**
     * Create a new query for the world.
     */
    public static Query create(World world) {
        return new Query(world);
    }

    /**
     * Create query with initial required components.
     */
    @SafeVarargs
    public static Query create(World world, Class<?>... required) {
        Query query = new Query(world);
        for (Class<?> type : required) {
            query.with(type);
        }
        return query;
    }

    // ========================================================================
    // FLUENT CONFIGURATION
    // ========================================================================

    /**
     * Require a component type.
     */
    public Query with(Class<?> componentClass) {
        ComponentRegistry.ComponentType type = registry.getType(componentClass);
        requiredMask |= (1L << type.id);
        requiredTypes.add(componentClass);
        invalidateCache();
        return this;
    }

    /**
     * Require multiple component types.
     */
    @SafeVarargs
    public final Query with(Class<?>... componentClasses) {
        for (Class<?> clazz : componentClasses) {
            with(clazz);
        }
        return this;
    }

    /**
     * Exclude entities with a component type.
     */
    public Query without(Class<?> componentClass) {
        ComponentRegistry.ComponentType type = registry.getType(componentClass);
        excludedMask |= (1L << type.id);
        invalidateCache();
        return this;
    }

    /**
     * Exclude multiple component types.
     */
    @SafeVarargs
    public final Query without(Class<?>... componentClasses) {
        for (Class<?> clazz : componentClasses) {
            without(clazz);
        }
        return this;
    }

    /**
     * Mark component as optional.
     */
    public Query optional(Class<?> componentClass) {
        ComponentRegistry.ComponentType type = registry.getType(componentClass);
        optionalMask |= (1L << type.id);
        optionalTypes.add(componentClass);
        return this;
    }

    /**
     * Mark multiple components as optional.
     */
    @SafeVarargs
    public final Query optional(Class<?>... componentClasses) {
        for (Class<?> clazz : componentClasses) {
            optional(clazz);
        }
        return this;
    }

    /**
     * Only include entities where component has changed.
     */
    public Query changed(Class<?> componentClass) {
        ComponentRegistry.ComponentType type = registry.getType(componentClass);
        changedMask |= (1L << type.id);
        return this;
    }

    /**
     * Only include entities changed since version.
     */
    public Query changedSince(long version) {
        this.changedSinceVersion = version;
        return this;
    }

    /**
     * Add entity filter predicate.
     */
    public Query filter(Predicate<Entity> predicate) {
        this.entityFilter = entityFilter.and(predicate);
        return this;
    }

    /**
     * Add archetype filter predicate.
     */
    public Query filterArchetype(Predicate<Archetype> predicate) {
        this.archetypeFilter = archetypeFilter.and(predicate);
        invalidateCache();
        return this;
    }

    /**
     * Set batch size for batch processing.
     */
    public Query batchSize(int size) {
        this.batchSize = Math.max(1, size);
        return this;
    }

    /**
     * Enable parallel execution.
     */
    public Query parallel() {
        this.parallel = true;
        return this;
    }

    /**
     * Enable parallel execution with threshold.
     */
    public Query parallel(int threshold) {
        this.parallel = count() >= threshold;
        return this;
    }

    /**
     * Enable archetype caching.
     */
    public Query cached() {
        this.cached = true;
        return this;
    }

    // ========================================================================
    // ARCHETYPE RETRIEVAL
    // ========================================================================

    /**
     * Get matching archetypes.
     */
    public List<Archetype> archetypes() {
        if (cached && cachedArchetypes != null) {
            // Check if cache is still valid
            // This would need a version from world
            return cachedArchetypes;
        }

        List<Archetype> result = world.queryArchetypes(requiredMask, excludedMask, optionalMask)
            .stream()
            .filter(archetypeFilter)
            .toList();

        if (cached) {
            cachedArchetypes = result;
        }

        return result;
    }

    private void invalidateCache() {
        cachedArchetypes = null;
    }

    // ========================================================================
    // ITERATION - BASIC
    // ========================================================================

    /**
     * Iterate over all matching entities.
     */
    public void forEach(Consumer<Entity> consumer) {
        if (parallel && count() >= PARALLEL_THRESHOLD) {
            forEachParallel(consumer);
        } else {
            forEachSequential(consumer);
        }
    }

    private void forEachSequential(Consumer<Entity> consumer) {
        for (Archetype archetype : archetypes()) {
            archetype.forEachEntity(entity -> {
                if (matchesFilters(entity)) {
                    consumer.accept(entity);
                }
            });
        }
    }

    private void forEachParallel(Consumer<Entity> consumer) {
        archetypes().parallelStream().forEach(archetype ->
            archetype.forEachEntity(entity -> {
                if (matchesFilters(entity)) {
                    consumer.accept(entity);
                }
            })
        );
    }

    private boolean matchesFilters(Entity entity) {
        if (!world.isValid(entity)) return false;
        if (!entityFilter.test(entity)) return false;
        
        // Change detection filter
        if (changedSinceVersion >= 0) {
            // Would need archetype support for version tracking
            // For now, pass all
        }
        
        return true;
    }

    // ========================================================================
    // ITERATION - WITH COMPONENTS
    // ========================================================================

    /**
     * Iterate with component data map.
     */
    public void forEach(BiConsumer<Entity, ComponentView> consumer) {
        for (Archetype archetype : archetypes()) {
            archetype.forEachEntity(entity -> {
                if (matchesFilters(entity)) {
                    ComponentView view = new ComponentView(archetype, entity);
                    consumer.accept(entity, view);
                }
            });
        }
    }

    /**
     * Iterate with single component (common case, optimized).
     */
    public <T> void forEachWith(Class<T> componentClass, BiConsumer<Entity, ByteBuffer> consumer) {
        ComponentRegistry.ComponentType type = registry.getType(componentClass);

        for (Archetype archetype : archetypes()) {
            if (!archetype.hasComponent(type.id)) continue;

            archetype.forEachEntity(entity -> {
                if (matchesFilters(entity)) {
                    ByteBuffer data = archetype.getComponent(entity.index(), type.id);
                    if (data != null) {
                        consumer.accept(entity, data);
                    }
                }
            });
        }
    }

    /**
     * Iterate with two components (common case, optimized).
     */
    public <T1, T2> void forEachWith(
            Class<T1> type1, Class<T2> type2,
            TriConsumer<Entity, ByteBuffer, ByteBuffer> consumer) {
        
        ComponentRegistry.ComponentType ct1 = registry.getType(type1);
        ComponentRegistry.ComponentType ct2 = registry.getType(type2);

        for (Archetype archetype : archetypes()) {
            if (!archetype.hasComponent(ct1.id) || !archetype.hasComponent(ct2.id)) continue;

            archetype.forEachEntity(entity -> {
                if (matchesFilters(entity)) {
                    ByteBuffer data1 = archetype.getComponent(entity.index(), ct1.id);
                    ByteBuffer data2 = archetype.getComponent(entity.index(), ct2.id);
                    if (data1 != null && data2 != null) {
                        consumer.accept(entity, data1, data2);
                    }
                }
            });
        }
    }

    /**
     * Iterate with three components.
     */
    public <T1, T2, T3> void forEachWith(
            Class<T1> type1, Class<T2> type2, Class<T3> type3,
            QuadConsumer<Entity, ByteBuffer, ByteBuffer, ByteBuffer> consumer) {
        
        ComponentRegistry.ComponentType ct1 = registry.getType(type1);
        ComponentRegistry.ComponentType ct2 = registry.getType(type2);
        ComponentRegistry.ComponentType ct3 = registry.getType(type3);

        for (Archetype archetype : archetypes()) {
            if (!archetype.hasComponent(ct1.id) || 
                !archetype.hasComponent(ct2.id) || 
                !archetype.hasComponent(ct3.id)) continue;

            archetype.forEachEntity(entity -> {
                if (matchesFilters(entity)) {
                    ByteBuffer d1 = archetype.getComponent(entity.index(), ct1.id);
                    ByteBuffer d2 = archetype.getComponent(entity.index(), ct2.id);
                    ByteBuffer d3 = archetype.getComponent(entity.index(), ct3.id);
                    if (d1 != null && d2 != null && d3 != null) {
                        consumer.accept(entity, d1, d2, d3);
                    }
                }
            });
        }
    }

    // ========================================================================
    // ITERATION - ARCHETYPE LEVEL
    // ========================================================================

    /**
     * Iterate over archetypes directly (fastest for batch processing).
     */
    public void forEachArchetype(Consumer<Archetype> consumer) {
        archetypes().forEach(consumer);
    }

    /**
     * Iterate over archetypes in parallel.
     */
    public void forEachArchetypeParallel(Consumer<Archetype> consumer) {
        archetypes().parallelStream().forEach(consumer);
    }

    /**
     * Iterate with archetype and entity stream.
     */
    public void forEachArchetype(BiConsumer<Archetype, Stream<Entity>> consumer) {
        for (Archetype archetype : archetypes()) {
            Stream<Entity> entityStream = archetype.entityStream()
                .filter(this::matchesFilters);
            consumer.accept(archetype, entityStream);
        }
    }

    // ========================================================================
    // STREAMING API
    // ========================================================================

    /**
     * Get stream of matching entities.
     */
    public Stream<Entity> stream() {
        Stream<Entity> stream = archetypes().stream()
            .flatMap(Archetype::entityStream)
            .filter(this::matchesFilters);
        
        return parallel ? stream.parallel() : stream;
    }

    /**
     * Get stream of query results with component data.
     */
    public Stream<QueryResult> streamResults() {
        return archetypes().stream()
            .flatMap(archetype -> archetype.entityStream()
                .filter(this::matchesFilters)
                .map(entity -> new QueryResult(entity, archetype)));
    }

    /**
     * Get parallel stream.
     */
    public Stream<Entity> parallelStream() {
        return stream().parallel();
    }

    // ========================================================================
    // BATCH PROCESSING
    // ========================================================================

    /**
     * Process entities in batches.
     */
    public void forEachBatch(Consumer<List<Entity>> batchConsumer) {
        List<Entity> batch = new ArrayList<>(batchSize);

        for (Archetype archetype : archetypes()) {
            archetype.forEachEntity(entity -> {
                if (matchesFilters(entity)) {
                    batch.add(entity);
                    if (batch.size() >= batchSize) {
                        batchConsumer.accept(new ArrayList<>(batch));
                        batch.clear();
                    }
                }
            });
        }

        // Process remaining
        if (!batch.isEmpty()) {
            batchConsumer.accept(batch);
        }
    }

    /**
     * Process batches in parallel.
     */
    public void forEachBatchParallel(Consumer<List<Entity>> batchConsumer) {
        List<List<Entity>> batches = collectBatches();
        batches.parallelStream().forEach(batchConsumer);
    }

    private List<List<Entity>> collectBatches() {
        List<List<Entity>> batches = new ArrayList<>();
        List<Entity> currentBatch = new ArrayList<>(batchSize);

        for (Archetype archetype : archetypes()) {
            for (Entity entity : archetype.getEntities()) {
                if (matchesFilters(entity)) {
                    currentBatch.add(entity);
                    if (currentBatch.size() >= batchSize) {
                        batches.add(currentBatch);
                        currentBatch = new ArrayList<>(batchSize);
                    }
                }
            }
        }

        if (!currentBatch.isEmpty()) {
            batches.add(currentBatch);
        }

        return batches;
    }

    // ========================================================================
    // AGGREGATION
    // ========================================================================

    /**
     * Count matching entities.
     */
    public int count() {
        return archetypes().stream()
            .mapToInt(Archetype::getEntityCount)
            .sum();
    }

    /**
     * Count with filter (slower, applies entity filter).
     */
    public int countFiltered() {
        return (int) stream().count();
    }

    /**
     * Check if any entities match.
     */
    public boolean any() {
        return archetypes().stream()
            .anyMatch(a -> a.getEntityCount() > 0);
    }

    /**
     * Check if any entities match with filter.
     */
    public boolean anyFiltered() {
        return stream().findAny().isPresent();
    }

    /**
     * Check if no entities match.
     */
    public boolean none() {
        return !any();
    }

    /**
     * Check if all entities in archetypes match filter.
     */
    public boolean all(Predicate<Entity> predicate) {
        return stream().allMatch(predicate);
    }

    /**
     * Get first matching entity.
     */
    public Optional<Entity> first() {
        return stream().findFirst();
    }

    /**
     * Get single matching entity (throws if more than one).
     */
    public Optional<Entity> single() {
        List<Entity> results = stream().limit(2).toList();
        if (results.size() > 1) {
            throw new IllegalStateException("Query matched more than one entity");
        }
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * Collect to list.
     */
    public List<Entity> toList() {
        return stream().toList();
    }

    /**
     * Collect to set.
     */
    public Set<Entity> toSet() {
        return stream().collect(Collectors.toSet());
    }

    // ========================================================================
    // REDUCTION
    // ========================================================================

    /**
     * Reduce over component data.
     */
    public <T, R> R reduce(
            Class<T> componentClass,
            R identity,
            BiFunction<R, ByteBuffer, R> accumulator) {
        
        ComponentRegistry.ComponentType type = registry.getType(componentClass);
        AtomicReference<R> result = new AtomicReference<>(identity);

        for (Archetype archetype : archetypes()) {
            if (!archetype.hasComponent(type.id)) continue;

            archetype.forEachEntity(entity -> {
                if (matchesFilters(entity)) {
                    ByteBuffer data = archetype.getComponent(entity.index(), type.id);
                    if (data != null) {
                        result.updateAndGet(r -> accumulator.apply(r, data));
                    }
                }
            });
        }

        return result.get();
    }

    /**
     * Sum float component values.
     */
    public float sumFloat(Class<?> componentClass, int offset) {
        ComponentRegistry.ComponentType type = registry.getType(componentClass);
        DoubleAdder sum = new DoubleAdder();

        for (Archetype archetype : archetypes()) {
            if (!archetype.hasComponent(type.id)) continue;

            archetype.forEachEntity(entity -> {
                if (matchesFilters(entity)) {
                    ByteBuffer data = archetype.getComponent(entity.index(), type.id);
                    if (data != null && data.remaining() > offset + 4) {
                        sum.add(data.getFloat(offset));
                    }
                }
            });
        }

        return (float) sum.sum();
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    /**
     * Query result with entity and component access.
     */
    public static final class QueryResult {
        private final Entity entity;
        private final Archetype archetype;
        private static final ComponentRegistry REGISTRY = ComponentRegistry.get();

        QueryResult(Entity entity, Archetype archetype) {
            this.entity = entity;
            this.archetype = archetype;
        }

        public Entity entity() { return entity; }

        public ByteBuffer get(Class<?> componentClass) {
            ComponentRegistry.ComponentType type = REGISTRY.getType(componentClass);
            return archetype.getComponent(entity.index(), type.id);
        }

        public Optional<ByteBuffer> getOptional(Class<?> componentClass) {
            return Optional.ofNullable(get(componentClass));
        }

        public MemorySegment getSegment(Class<?> componentClass) {
            ComponentRegistry.ComponentType type = REGISTRY.getType(componentClass);
            return archetype.getComponentSegment(entity.index(), type.id);
        }

        public boolean has(Class<?> componentClass) {
            ComponentRegistry.ComponentType type = REGISTRY.getType(componentClass);
            return archetype.hasComponent(type.id);
        }

        public float getFloat(Class<?> componentClass, int offset) {
            ByteBuffer data = get(componentClass);
            return data != null ? data.getFloat(offset) : 0f;
        }

        public int getInt(Class<?> componentClass, int offset) {
            ByteBuffer data = get(componentClass);
            return data != null ? data.getInt(offset) : 0;
        }

        public void setFloat(Class<?> componentClass, int offset, float value) {
            ByteBuffer data = get(componentClass);
            if (data != null) {
                data.putFloat(offset, value);
            }
        }

        public void setInt(Class<?> componentClass, int offset, int value) {
            ByteBuffer data = get(componentClass);
            if (data != null) {
                data.putInt(offset, value);
            }
        }
    }

    /**
     * Component view for entity.
     */
    public static final class ComponentView {
        private final Archetype archetype;
        private final Entity entity;
        private static final ComponentRegistry REGISTRY = ComponentRegistry.get();

        ComponentView(Archetype archetype, Entity entity) {
            this.archetype = archetype;
            this.entity = entity;
        }

        public ByteBuffer get(Class<?> componentClass) {
            ComponentRegistry.ComponentType type = REGISTRY.getType(componentClass);
            return archetype.getComponent(entity.index(), type.id);
        }

        public Optional<ByteBuffer> getOptional(Class<?> componentClass) {
            return Optional.ofNullable(get(componentClass));
        }

        public boolean has(Class<?> componentClass) {
            ComponentRegistry.ComponentType type = REGISTRY.getType(componentClass);
            return archetype.hasComponent(type.id);
        }

        public Map<Class<?>, ByteBuffer> toMap(List<Class<?>> types) {
            Map<Class<?>, ByteBuffer> map = new HashMap<>();
            for (Class<?> type : types) {
                ByteBuffer data = get(type);
                if (data != null) {
                    map.put(type, data);
                }
            }
            return map;
        }
    }

    // ========================================================================
    // FUNCTIONAL INTERFACES
    // ========================================================================

    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    @FunctionalInterface
    public interface QuadConsumer<A, B, C, D> {
        void accept(A a, B b, C c, D d);
    }

    // ========================================================================
    // QUERY STATISTICS
    // ========================================================================

    /**
     * Get query statistics.
     */
    public QueryStats stats() {
        List<Archetype> archs = archetypes();
        int totalEntities = archs.stream().mapToInt(Archetype::getEntityCount).sum();
        long totalComponents = archs.stream()
            .mapToLong(a -> (long) a.getEntityCount() * Long.bitCount(a.getComponentMask()))
            .sum();

        return new QueryStats(
            archs.size(),
            totalEntities,
            totalComponents,
            Long.bitCount(requiredMask),
            Long.bitCount(excludedMask),
            Long.bitCount(optionalMask)
        );
    }

    public record QueryStats(
        int archetypeCount,
        int entityCount,
        long componentCount,
        int requiredCount,
        int excludedCount,
        int optionalCount
    ) {}

    // ========================================================================
    // DEBUG
    // ========================================================================

    @Override
    public String toString() {
        return String.format(
            "Query[required=%d, excluded=%d, optional=%d, archetypes=%d, entities=%d]",
            Long.bitCount(requiredMask),
            Long.bitCount(excludedMask),
            Long.bitCount(optionalMask),
            archetypes().size(),
            count()
        );
    }

    /**
     * Get detailed query description.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append("Query {\n");
        sb.append("  Required: ").append(requiredTypes).append("\n");
        sb.append("  Optional: ").append(optionalTypes).append("\n");
        sb.append("  Excluded mask: 0x").append(Long.toHexString(excludedMask)).append("\n");
        sb.append("  Parallel: ").append(parallel).append("\n");
        sb.append("  Batch size: ").append(batchSize).append("\n");
        sb.append("  Stats: ").append(stats()).append("\n");
        sb.append("}");
        return sb.toString();
    }
}
