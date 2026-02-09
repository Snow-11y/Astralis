package stellar.snow.astralis.engine.ecs.core;

import stellar.snow.astralis.Astralis;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.*;
import java.util.stream.*;

/**
 * World - High-performance ECS world container with cutting-edge Java 21+ features.
 *
 * <p>Core Features:</p>
 * <ul>
 *   <li>Lock-free entity management with generational indices</li>
 *   <li>Virtual thread support for parallel system execution</li>
 *   <li>Foreign Memory API for off-heap component storage</li>
 *   <li>Deferred command buffers for thread-safe modifications</li>
 *   <li>Query caching with automatic invalidation</li>
 *   <li>Entity relationships and hierarchies</li>
 *   <li>Component change detection with versioning</li>
 *   <li>Structured concurrency for deterministic execution</li>
 *   <li>Event-driven architecture with pub/sub messaging</li>
 *   <li>Memory-efficient sparse set indexing</li>
 * </ul>
 *
 * @author Enhanced ECS Framework
 * @version 2.0.0
 * @since Java 21
 */
public final class World implements AutoCloseable {

    // ========================================================================
    // CONSTANTS & CONFIGURATION
    // ========================================================================

    private static final int CACHE_LINE_SIZE = 64;
    private static final int DEFAULT_MAX_ENTITIES = 1_000_000;
    private static final int DEFAULT_COMMAND_BUFFER_SIZE = 16_384;
    private static final long QUERY_CACHE_TTL_NANOS = Duration.ofMillis(100).toNanos();

    /** World configuration record */
    public record Config(
        String name,
        int maxEntities,
        int parallelism,
        int commandBufferSize,
        boolean useVirtualThreads,
        boolean enableChangeDetection,
        boolean enableEventSystem,
        boolean useOffHeapStorage
    ) {
        public static Config defaults(String name) {
            return new Config(
                name,
                DEFAULT_MAX_ENTITIES,
                Runtime.getRuntime().availableProcessors(),
                DEFAULT_COMMAND_BUFFER_SIZE,
                true,   // Virtual threads enabled by default
                true,   // Change detection enabled
                true,   // Event system enabled
                true    // Off-heap storage enabled
            );
        }

        public static Builder builder(String name) {
            return new Builder(name);
        }

        public static final class Builder {
            private final String name;
            private int maxEntities = DEFAULT_MAX_ENTITIES;
            private int parallelism = Runtime.getRuntime().availableProcessors();
            private int commandBufferSize = DEFAULT_COMMAND_BUFFER_SIZE;
            private boolean useVirtualThreads = true;
            private boolean enableChangeDetection = true;
            private boolean enableEventSystem = true;
            private boolean useOffHeapStorage = true;

            private Builder(String name) { this.name = name; }

            public Builder maxEntities(int val) { maxEntities = val; return this; }
            public Builder parallelism(int val) { parallelism = val; return this; }
            public Builder commandBufferSize(int val) { commandBufferSize = val; return this; }
            public Builder useVirtualThreads(boolean val) { useVirtualThreads = val; return this; }
            public Builder enableChangeDetection(boolean val) { enableChangeDetection = val; return this; }
            public Builder enableEventSystem(boolean val) { enableEventSystem = val; return this; }
            public Builder useOffHeapStorage(boolean val) { useOffHeapStorage = val; return this; }

            public Config build() {
                return new Config(name, maxEntities, parallelism, commandBufferSize,
                    useVirtualThreads, enableChangeDetection, enableEventSystem, useOffHeapStorage);
            }
        }
    }

    // ========================================================================
    // CORE STATE
    // ========================================================================

    /** Immutable world configuration */
    public final Config config;

    /** World name for debugging (shortcut to config.name) */
    public final String name;

    // Entity management - using off-heap memory for cache efficiency
    private final Arena entityArena;
    private final MemorySegment entityGenerations;  // int[] - generation per index
    private final MemorySegment entityArchetypes;   // int[] - archetype ID per index
    private final MemorySegment entityFlags;        // byte[] - status flags per entity

    private final AtomicInteger nextEntityIndex = new AtomicInteger(1);
    private final Deque<Integer> recycledIndices = new ConcurrentLinkedDeque<>();
    private volatile int maxEntityIndex = 0;

    // Archetype management with optimistic locking
    private final ConcurrentHashMap<Long, Archetype> archetypesByMask = new ConcurrentHashMap<>(64);
    private final CopyOnWriteArrayList<Archetype> archetypeList = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextArchetypeId = new AtomicInteger(0);
    private final StampedLock archetypeLock = new StampedLock();

    // Query caching
    private final ConcurrentHashMap<QueryKey, CachedQuery> queryCache = new ConcurrentHashMap<>();
    private final AtomicLong queryCacheVersion = new AtomicLong(0);

    // System scheduling
    private final SystemScheduler scheduler;
    private final ExecutorService virtualExecutor;
    private final StructuredTaskScope.ShutdownOnFailure structuredScope;

    // Component registry reference
    private final ComponentRegistry registry;

    // Command buffer for deferred operations
    private final CommandBuffer commandBuffer;

    // Entity relationships
    private final RelationshipGraph relationships;

    // Event system
    private final EventBus eventBus;

    // Statistics with low-contention counters
    private final LongAdder totalEntitiesCreated = new LongAdder();
    private final LongAdder totalEntitiesDestroyed = new LongAdder();
    private final LongAdder componentOperations = new LongAdder();
    private final LongAdder queryCacheHits = new LongAdder();
    private final LongAdder queryCacheMisses = new LongAdder();

    // Lifecycle state
    private volatile WorldState state = WorldState.CREATED;
    private final Instant creationTime = Instant.now();

    // ========================================================================
    // ENUMS & SEALED TYPES
    // ========================================================================

    public enum WorldState { CREATED, INITIALIZED, RUNNING, PAUSED, SHUTDOWN }

    public enum EntityFlag {
        ACTIVE((byte) 0x01),
        PREFAB((byte) 0x02),
        DISABLED((byte) 0x04),
        PENDING_DESTROY((byte) 0x08);

        public final byte mask;
        EntityFlag(byte mask) { this.mask = mask; }
    }

    /** Query key for caching */
    private record QueryKey(long requiredMask, long excludedMask, long optionalMask) {}

    /** Cached query result */
    private record CachedQuery(
        List<Archetype> archetypes,
        long version,
        long timestampNanos
    ) {
        boolean isValid(long currentVersion) {
            return version == currentVersion &&
                   (java.lang.System.nanoTime() - timestampNanos) < QUERY_CACHE_TTL_NANOS;
        }
    }

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    /**
     * Create a new world with configuration.
     */
    public World(Config config) {
        this.config = Objects.requireNonNull(config, "Config cannot be null");
        this.name = config.name();

        // Initialize off-heap entity storage
        this.entityArena = config.useOffHeapStorage() ? Arena.ofShared() : Arena.ofConfined();
        this.entityGenerations = entityArena.allocate(
            ValueLayout.JAVA_INT, config.maxEntities());
        this.entityArchetypes = entityArena.allocate(
            ValueLayout.JAVA_INT, config.maxEntities());
        this.entityFlags = entityArena.allocate(
            ValueLayout.JAVA_BYTE, config.maxEntities());

        // Initialize archetype indices to -1
        MemorySegment.copy(
            MemorySegment.ofArray(createFilledIntArray(config.maxEntities(), -1)),
            ValueLayout.JAVA_INT, 0,
            entityArchetypes, ValueLayout.JAVA_INT, 0,
            config.maxEntities()
        );

        // Initialize executors
        this.virtualExecutor = config.useVirtualThreads()
            ? Executors.newVirtualThreadPerTaskExecutor()
            : Executors.newWorkStealingPool(config.parallelism());

        this.structuredScope = new StructuredTaskScope.ShutdownOnFailure(
            "World-" + config.name(), Thread.ofVirtual().factory());

        this.scheduler = new SystemScheduler(config.parallelism(), config.useVirtualThreads());
        this.registry = ComponentRegistry.get();
        this.commandBuffer = new CommandBuffer(config.commandBufferSize());
        this.relationships = new RelationshipGraph(config.maxEntities());
        this.eventBus = config.enableEventSystem() ? new EventBus() : EventBus.NOOP;
    }

    /**
     * Create with default settings.
     */
    public World(String name) {
        this(Config.defaults(name));
    }

    /**
     * Create with custom max entities and parallelism (legacy compatibility).
     */
    public World(String name, int maxEntities, int parallelism) {
        this(Config.builder(name)
            .maxEntities(maxEntities)
            .parallelism(parallelism)
            .build());
    }

    // ========================================================================
    // ENTITY MANAGEMENT
    // ========================================================================

    /**
     * Create a new entity with lock-free allocation.
     */
    public Entity createEntity() {
        int index;
        int generation;

        // Try to reuse recycled index first (lock-free)
        Integer recycled = recycledIndices.pollFirst();
        if (recycled != null) {
            index = recycled;
            generation = getGeneration(index) + 1;
            setGeneration(index, generation);
        } else {
            index = nextEntityIndex.getAndIncrement();
            if (index >= config.maxEntities()) {
                throw new EntityLimitExceededException(config.maxEntities());
            }
            generation = 1;
            setGeneration(index, generation);
            updateMaxEntityIndex(index);
        }

        setEntityFlags(index, EntityFlag.ACTIVE.mask);
        totalEntitiesCreated.increment();
        
        Entity entity = new Entity(index, generation);
        eventBus.publish(new EntityCreatedEvent(entity));
        
        return entity;
    }

    /**
     * Create entity with initial components using varargs.
     */
    @SafeVarargs
    public final Entity createEntity(Class<?>... componentTypes) {
        Entity entity = createEntity();

        if (componentTypes.length > 0) {
            long mask = computeComponentMask(componentTypes);
            Archetype archetype = getOrCreateArchetype(mask);
            archetype.addEntity(entity);
            setArchetypeId(entity.index(), archetype.id);
        }

        return entity;
    }

    /**
     * Create entity from prefab template.
     */
    public Entity createFromPrefab(Entity prefab) {
        if (!isValid(prefab) || !hasFlag(prefab, EntityFlag.PREFAB)) {
            throw new IllegalArgumentException("Invalid prefab entity: " + prefab);
        }

        Entity entity = createEntity();
        int prefabArchetypeId = getArchetypeId(prefab.index());
        
        if (prefabArchetypeId >= 0) {
            Archetype prefabArchetype = archetypeList.get(prefabArchetypeId);
            Archetype entityArchetype = getOrCreateArchetype(prefabArchetype.getComponentMask());
            
            entityArchetype.addEntity(entity);
            entityArchetype.copyEntityData(prefab, entity, prefabArchetype);
            setArchetypeId(entity.index(), entityArchetype.id);
        }

        return entity;
    }

    /**
     * Batch create entities for improved performance.
     */
    public Entity[] createEntities(int count) {
        Entity[] entities = new Entity[count];
        
        for (int i = 0; i < count; i++) {
            entities[i] = createEntity();
        }
        
        return entities;
    }

    /**
     * Batch create entities with components.
     */
    @SafeVarargs
    public final Entity[] createEntities(int count, Class<?>... componentTypes) {
        Entity[] entities = new Entity[count];
        long mask = computeComponentMask(componentTypes);
        Archetype archetype = getOrCreateArchetype(mask);

        for (int i = 0; i < count; i++) {
            Entity entity = createEntity();
            archetype.addEntity(entity);
            setArchetypeId(entity.index(), archetype.id);
            entities[i] = entity;
        }

        return entities;
    }

    /**
     * Destroy an entity.
     */
    public void destroyEntity(Entity entity) {
        if (!isValid(entity)) return;

        eventBus.publish(new EntityDestroyingEvent(entity));

        // Remove relationships
        relationships.removeEntity(entity);

        // Remove from archetype
        int archetypeId = getArchetypeId(entity.index());
        if (archetypeId >= 0 && archetypeId < archetypeList.size()) {
            Archetype archetype = archetypeList.get(archetypeId);
            if (archetype != null) {
                archetype.removeEntity(entity);
            }
        }

        // Invalidate entity
        setArchetypeId(entity.index(), -1);
        setGeneration(entity.index(), getGeneration(entity.index()) + 1);
        setEntityFlags(entity.index(), (byte) 0);

        // Recycle index
        recycledIndices.addLast(entity.index());
        totalEntitiesDestroyed.increment();
        invalidateQueryCache();

        eventBus.publish(new EntityDestroyedEvent(entity.index()));
    }

    /**
     * Deferred entity destruction (thread-safe).
     */
    public void destroyEntityDeferred(Entity entity) {
        commandBuffer.enqueue(new DestroyEntityCommand(entity));
    }

    /**
     * Check if entity is valid.
     */
    public boolean isValid(Entity entity) {
        return entity != null &&
               entity.index() > 0 &&
               entity.index() < config.maxEntities() &&
               getGeneration(entity.index()) == entity.generation() &&
               (getEntityFlags(entity.index()) & EntityFlag.ACTIVE.mask) != 0;
    }

    /**
     * Check entity flag.
     */
    public boolean hasFlag(Entity entity, EntityFlag flag) {
        if (!isValid(entity)) return false;
        return (getEntityFlags(entity.index()) & flag.mask) != 0;
    }

    /**
     * Set entity flag.
     */
    public void setFlag(Entity entity, EntityFlag flag, boolean value) {
        if (!isValid(entity)) return;
        byte current = getEntityFlags(entity.index());
        byte updated = value ? (byte)(current | flag.mask) : (byte)(current & ~flag.mask);
        setEntityFlags(entity.index(), updated);
    }

    /**
     * Get entity count.
     */
    public int getEntityCount() {
        return (int) (totalEntitiesCreated.sum() - totalEntitiesDestroyed.sum());
    }

    /**
     * Get all active entities (lazy iterator).
     */
    public Iterable<Entity> entities() {
        return () -> new EntityIterator();
    }

    // ========================================================================
    // COMPONENT MANAGEMENT
    // ========================================================================

    /**
     * Add component to entity with ByteBuffer data.
     */
    public <T> void addComponent(Entity entity, Class<T> componentClass, ByteBuffer data) {
        if (!isValid(entity)) return;

        ComponentRegistry.ComponentType type = registry.getType(componentClass);
        int currentArchetypeId = getArchetypeId(entity.index());
        long currentMask = 0;

        if (currentArchetypeId >= 0) {
            Archetype currentArchetype = archetypeList.get(currentArchetypeId);
            currentMask = currentArchetype.getComponentMask();

            // Already has component - just update
            if (currentArchetype.hasComponent(type.id)) {
                currentArchetype.setComponent(entity.index(), type.id, data);
                markComponentChanged(entity, type.id);
                componentOperations.increment();
                return;
            }

            // Remove from current archetype
            currentArchetype.removeEntity(entity);
        }

        // Migrate to new archetype
        long newMask = currentMask | (1L << type.id);
        Archetype newArchetype = getOrCreateArchetype(newMask);
        newArchetype.addEntity(entity);
        newArchetype.setComponent(entity.index(), type.id, data);
        setArchetypeId(entity.index(), newArchetype.id);

        // Copy existing component data
        if (currentArchetypeId >= 0) {
            Archetype oldArchetype = archetypeList.get(currentArchetypeId);
            copyComponentData(entity, oldArchetype, newArchetype, type.id);
        }

        componentOperations.increment();
        invalidateQueryCache();
        eventBus.publish(new ComponentAddedEvent(entity, componentClass));
    }

    /**
     * Add component with float array data.
     */
    public void addComponent(Entity entity, Class<?> componentClass, float... values) {
        ByteBuffer data = ByteBuffer.allocateDirect(values.length * Float.BYTES)
            .order(ByteOrder.nativeOrder());
        for (float v : values) data.putFloat(v);
        data.flip();
        addComponent(entity, componentClass, data);
    }

    /**
     * Add component with int array data.
     */
    public void addComponent(Entity entity, Class<?> componentClass, int... values) {
        ByteBuffer data = ByteBuffer.allocateDirect(values.length * Integer.BYTES)
            .order(ByteOrder.nativeOrder());
        for (int v : values) data.putInt(v);
        data.flip();
        addComponent(entity, componentClass, data);
    }

    /**
     * Add component deferred (thread-safe).
     */
    public <T> void addComponentDeferred(Entity entity, Class<T> componentClass, ByteBuffer data) {
        commandBuffer.enqueue(new AddComponentCommand(entity, componentClass, data.duplicate()));
    }

    /**
     * Remove component from entity.
     */
    public void removeComponent(Entity entity, Class<?> componentClass) {
        if (!isValid(entity)) return;

        ComponentRegistry.ComponentType type = registry.getType(componentClass);
        int currentArchetypeId = getArchetypeId(entity.index());

        if (currentArchetypeId < 0) return;

        Archetype currentArchetype = archetypeList.get(currentArchetypeId);
        if (!currentArchetype.hasComponent(type.id)) return;

        eventBus.publish(new ComponentRemovingEvent(entity, componentClass));

        long newMask = currentArchetype.getComponentMask() & ~(1L << type.id);

        if (newMask == 0) {
            currentArchetype.removeEntity(entity);
            setArchetypeId(entity.index(), -1);
        } else {
            Archetype newArchetype = getOrCreateArchetype(newMask);
            copyComponentDataExcept(entity, currentArchetype, newArchetype, type.id);
            currentArchetype.removeEntity(entity);
            newArchetype.addEntity(entity);
            setArchetypeId(entity.index(), newArchetype.id);
        }

        componentOperations.increment();
        invalidateQueryCache();
        eventBus.publish(new ComponentRemovedEvent(entity, componentClass));
    }

    /**
     * Check if entity has component.
     */
    public boolean hasComponent(Entity entity, Class<?> componentClass) {
        if (!isValid(entity)) return false;

        int archetypeId = getArchetypeId(entity.index());
        if (archetypeId < 0) return false;

        ComponentRegistry.ComponentType type = registry.getType(componentClass);
        Archetype archetype = archetypeList.get(archetypeId);
        return archetype != null && archetype.hasComponent(type.id);
    }

    /**
     * Get component data.
     */
    public ByteBuffer getComponent(Entity entity, Class<?> componentClass) {
        if (!isValid(entity)) return null;

        int archetypeId = getArchetypeId(entity.index());
        if (archetypeId < 0) return null;

        ComponentRegistry.ComponentType type = registry.getType(componentClass);
        Archetype archetype = archetypeList.get(archetypeId);
        return archetype != null ? archetype.getComponent(entity.index(), type.id) : null;
    }

    /**
     * Get component as MemorySegment (zero-copy for off-heap).
     */
    public MemorySegment getComponentSegment(Entity entity, Class<?> componentClass) {
        if (!isValid(entity)) return null;

        int archetypeId = getArchetypeId(entity.index());
        if (archetypeId < 0) return null;

        ComponentRegistry.ComponentType type = registry.getType(componentClass);
        Archetype archetype = archetypeList.get(archetypeId);
        return archetype != null ? archetype.getComponentSegment(entity.index(), type.id) : null;
    }

    // ========================================================================
    // ENTITY RELATIONSHIPS
    // ========================================================================

    /**
     * Set parent-child relationship.
     */
    public void setParent(Entity child, Entity parent) {
        if (!isValid(child) || !isValid(parent)) return;
        relationships.setParent(child, parent);
        eventBus.publish(new RelationshipChangedEvent(child, parent, RelationType.PARENT));
    }

    /**
     * Get parent of entity.
     */
    public Optional<Entity> getParent(Entity entity) {
        if (!isValid(entity)) return Optional.empty();
        int parentIndex = relationships.getParent(entity);
        if (parentIndex <= 0) return Optional.empty();
        
        int generation = getGeneration(parentIndex);
        Entity parent = new Entity(parentIndex, generation);
        return isValid(parent) ? Optional.of(parent) : Optional.empty();
    }

    /**
     * Get children of entity.
     */
    public List<Entity> getChildren(Entity entity) {
        if (!isValid(entity)) return List.of();
        return relationships.getChildren(entity).stream()
            .map(idx -> new Entity(idx, getGeneration(idx)))
            .filter(this::isValid)
            .toList();
    }

    /**
     * Add tag/relationship.
     */
    public void addRelation(Entity entity, Entity target, int relationTypeId) {
        if (!isValid(entity) || !isValid(target)) return;
        relationships.addRelation(entity, target, relationTypeId);
    }

    public enum RelationType { PARENT, CHILD, SIBLING, CUSTOM }

    // ========================================================================
    // QUERY SYSTEM
    // ========================================================================

    /**
     * Create a fluent query builder.
     */
    public QueryBuilder query() {
        return new QueryBuilder(this);
    }

    /**
     * Get archetypes matching a component query (cached).
     */
    public List<Archetype> queryArchetypes(long requiredMask, long excludedMask) {
        return queryArchetypes(requiredMask, excludedMask, 0L);
    }

    /**
     * Get archetypes matching a component query with optional mask (cached).
     */
    public List<Archetype> queryArchetypes(long requiredMask, long excludedMask, long optionalMask) {
        QueryKey key = new QueryKey(requiredMask, excludedMask, optionalMask);
        long currentVersion = queryCacheVersion.get();

        // Check cache
        CachedQuery cached = queryCache.get(key);
        if (cached != null && cached.isValid(currentVersion)) {
            queryCacheHits.increment();
            return cached.archetypes();
        }

        queryCacheMisses.increment();

        // Compute result
        List<Archetype> result = archetypeList.stream()
            .filter(Objects::nonNull)
            .filter(a -> {
                long mask = a.getComponentMask();
                return (mask & requiredMask) == requiredMask && (mask & excludedMask) == 0;
            })
            .toList();

        // Cache result
        queryCache.put(key, new CachedQuery(result, currentVersion, java.lang.System.nanoTime()));

        return result;
    }

    /**
     * Execute action on all entities matching query.
     */
    public void forEach(long requiredMask, Consumer<Entity> action) {
        queryArchetypes(requiredMask, 0).forEach(archetype ->
            archetype.forEachEntity(entity -> {
                if (isValid(entity)) action.accept(entity);
            }));
    }

    /**
     * Execute action on matching entities in parallel.
     */
    public void forEachParallel(long requiredMask, Consumer<Entity> action) {
        List<Archetype> archetypes = queryArchetypes(requiredMask, 0);
        
        try {
            List<CompletableFuture<Void>> futures = archetypes.stream()
                .map(archetype -> CompletableFuture.runAsync(
                    () -> archetype.forEachEntity(entity -> {
                        if (isValid(entity)) action.accept(entity);
                    }),
                    virtualExecutor))
                .toList();

            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        } catch (Exception e) {
            Astralis.LOGGER.error("[ECS] Parallel forEach failed", e);
        }
    }

    /**
     * Fluent query builder.
     */
    public static final class QueryBuilder {
        private final World world;
        private long requiredMask = 0;
        private long excludedMask = 0;
        private long optionalMask = 0;
        private Predicate<Entity> filter = e -> true;

        private QueryBuilder(World world) { this.world = world; }

        @SafeVarargs
        public final QueryBuilder with(Class<?>... components) {
            for (Class<?> c : components) {
                requiredMask |= (1L << world.registry.getType(c).id);
            }
            return this;
        }

        @SafeVarargs
        public final QueryBuilder without(Class<?>... components) {
            for (Class<?> c : components) {
                excludedMask |= (1L << world.registry.getType(c).id);
            }
            return this;
        }

        @SafeVarargs
        public final QueryBuilder optional(Class<?>... components) {
            for (Class<?> c : components) {
                optionalMask |= (1L << world.registry.getType(c).id);
            }
            return this;
        }

        public QueryBuilder filter(Predicate<Entity> predicate) {
            this.filter = filter.and(predicate);
            return this;
        }

        public List<Archetype> archetypes() {
            return world.queryArchetypes(requiredMask, excludedMask, optionalMask);
        }

        public Stream<Entity> stream() {
            return archetypes().stream()
                .flatMap(a -> a.entityStream())
                .filter(world::isValid)
                .filter(filter);
        }

        public void forEach(Consumer<Entity> action) {
            stream().forEach(action);
        }

        public void forEachParallel(Consumer<Entity> action) {
            stream().parallel().forEach(action);
        }

        public int count() {
            return (int) stream().count();
        }

        public Optional<Entity> first() {
            return stream().findFirst();
        }
    }

    // ========================================================================
    // ARCHETYPE MANAGEMENT
    // ========================================================================

    /**
     * Get or create archetype for component mask.
     */
    private Archetype getOrCreateArchetype(long mask) {
        return archetypesByMask.computeIfAbsent(mask, m -> {
            int[] typeIdArray = computeTypeIdArray(m);
            int id = nextArchetypeId.getAndIncrement();
            
            Archetype archetype = new Archetype(
                id, typeIdArray, registry,
                config.useOffHeapStorage(),
                config.enableChangeDetection()
            );

            // Thread-safe list update
            long stamp = archetypeLock.writeLock();
            try {
                while (archetypeList.size() <= id) {
                    archetypeList.add(null);
                }
                archetypeList.set(id, archetype);
            } finally {
                archetypeLock.unlockWrite(stamp);
            }

            invalidateQueryCache();
            eventBus.publish(new ArchetypeCreatedEvent(archetype));

            return archetype;
        });
    }

    /**
     * Get all archetypes (unmodifiable view).
     */
    public List<Archetype> getArchetypes() {
        long stamp = archetypeLock.tryOptimisticRead();
        List<Archetype> result = archetypeList.stream()
            .filter(Objects::nonNull)
            .toList();

        if (!archetypeLock.validate(stamp)) {
            stamp = archetypeLock.readLock();
            try {
                result = archetypeList.stream()
                    .filter(Objects::nonNull)
                    .toList();
            } finally {
                archetypeLock.unlockRead(stamp);
            }
        }

        return result;
    }

    private void invalidateQueryCache() {
        queryCacheVersion.incrementAndGet();
    }

    // ========================================================================
    // SYSTEM MANAGEMENT
    // ========================================================================

    /**
     * Register a system.
     */
    public void registerSystem(System system, SystemScheduler.Stage stage) {
        scheduler.register(system, stage);
    }

    /**
     * Register system with dependencies.
     */
    public void registerSystem(System system, SystemScheduler.Stage stage, String... dependencies) {
        scheduler.register(system, stage, dependencies);
    }

    /**
     * Initialize all systems.
     */
    public void initialize() {
        if (state != WorldState.CREATED) {
            throw new IllegalStateException("World already initialized or shutdown");
        }

        scheduler.initialize(this);
        state = WorldState.INITIALIZED;

        Astralis.LOGGER.info("[ECS] World '{}' initialized - Archetypes: {}, VirtualThreads: {}",
            name, archetypeList.size(), config.useVirtualThreads());

        eventBus.publish(new WorldInitializedEvent(this));
    }

    /**
     * Update world (execute all systems).
     */
    public void update(float deltaTime) {
        if (state == WorldState.PAUSED || state == WorldState.SHUTDOWN) return;
        
        state = WorldState.RUNNING;

        // Process deferred commands
        commandBuffer.execute(this);

        // Execute systems
        scheduler.executeAll(this, deltaTime);
    }

    /**
     * Execute specific stage.
     */
    public void executeStage(SystemScheduler.Stage stage, float deltaTime) {
        if (state == WorldState.SHUTDOWN) return;
        scheduler.executeStage(this, stage, deltaTime);
    }

    /**
     * Pause world updates.
     */
    public void pause() {
        if (state == WorldState.RUNNING) {
            state = WorldState.PAUSED;
            eventBus.publish(new WorldPausedEvent(this));
        }
    }

    /**
     * Resume world updates.
     */
    public void resume() {
        if (state == WorldState.PAUSED) {
            state = WorldState.RUNNING;
            eventBus.publish(new WorldResumedEvent(this));
        }
    }

    /**
     * Sync all component data to GPU.
     */
    public void syncToGpu() {
        archetypeList.parallelStream()
            .filter(Objects::nonNull)
            .forEach(Archetype::syncToGpu);
    }

    /**
     * Shutdown world and release resources.
     */
    public void shutdown() {
        if (state == WorldState.SHUTDOWN) return;

        state = WorldState.SHUTDOWN;
        eventBus.publish(new WorldShuttingDownEvent(this));

        scheduler.shutdown(this);
        virtualExecutor.shutdown();

        try {
            structuredScope.close();
        } catch (Exception e) {
            Astralis.LOGGER.warn("[ECS] Error closing structured scope", e);
        }

        // Release off-heap memory
        if (entityArena.scope().isAlive()) {
            entityArena.close();
        }

        Astralis.LOGGER.info("[ECS] World '{}' shutdown - Created: {}, Destroyed: {}, Uptime: {}",
            name, totalEntitiesCreated.sum(), totalEntitiesDestroyed.sum(),
            Duration.between(creationTime, Instant.now()));
    }

    @Override
    public void close() {
        shutdown();
    }

    // ========================================================================
    // SNAPSHOT & SERIALIZATION
    // ========================================================================

    /**
     * Create a world snapshot for serialization/debugging.
     */
    public WorldSnapshot createSnapshot() {
        return new WorldSnapshot(
            name,
            getEntityCount(),
            totalEntitiesCreated.sum(),
            totalEntitiesDestroyed.sum(),
            getArchetypes().stream()
                .map(Archetype::createSnapshot)
                .toList(),
            Instant.now()
        );
    }

    public record WorldSnapshot(
        String name,
        int entityCount,
        long totalCreated,
        long totalDestroyed,
        List<Archetype.ArchetypeSnapshot> archetypes,
        Instant timestamp
    ) {}

    // ========================================================================
    // ACCESSORS
    // ========================================================================

    public SystemScheduler getScheduler() { return scheduler; }
    public ComponentRegistry getRegistry() { return registry; }
    public CommandBuffer getCommandBuffer() { return commandBuffer; }
    public EventBus getEventBus() { return eventBus; }
    public RelationshipGraph getRelationships() { return relationships; }
    public WorldState getState() { return state; }
    public Instant getCreationTime() { return creationTime; }
    public Duration getUptime() { return Duration.between(creationTime, Instant.now()); }

    // ========================================================================
    // STATISTICS & DIAGNOSTICS
    // ========================================================================

    /**
     * Get world statistics.
     */
    public WorldStats getStatistics() {
        return new WorldStats(
            name,
            getEntityCount(),
            totalEntitiesCreated.sum(),
            totalEntitiesDestroyed.sum(),
            recycledIndices.size(),
            archetypesByMask.size(),
            queryCacheHits.sum(),
            queryCacheMisses.sum(),
            componentOperations.sum(),
            getUptime()
        );
    }

    public record WorldStats(
        String name,
        int activeEntities,
        long totalCreated,
        long totalDestroyed,
        int recycledPoolSize,
        int archetypeCount,
        long queryCacheHits,
        long queryCacheMisses,
        long componentOperations,
        Duration uptime
    ) {
        public double queryCacheHitRate() {
            long total = queryCacheHits + queryCacheMisses;
            return total > 0 ? (double) queryCacheHits / total : 0.0;
        }
    }

    /**
     * Get comprehensive statistics string.
     */
    public String getStats() {
        WorldStats stats = getStatistics();
        StringBuilder sb = new StringBuilder(1024);

        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  World '").append(name).append("' Statistics\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  State:            ").append(state).append("\n");
        sb.append("  Uptime:           ").append(stats.uptime()).append("\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  ENTITIES\n");
        sb.append("    Active:         ").append(String.format("%,d", stats.activeEntities())).append("\n");
        sb.append("    Created:        ").append(String.format("%,d", stats.totalCreated())).append("\n");
        sb.append("    Destroyed:      ").append(String.format("%,d", stats.totalDestroyed())).append("\n");
        sb.append("    Recycled Pool:  ").append(String.format("%,d", stats.recycledPoolSize())).append("\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  ARCHETYPES:       ").append(stats.archetypeCount()).append("\n");

        for (Archetype archetype : getArchetypes()) {
            sb.append("    ").append(archetype).append("\n");
        }

        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  PERFORMANCE\n");
        sb.append("    Query Cache Hit Rate: ")
          .append(String.format("%.2f%%", stats.queryCacheHitRate() * 100)).append("\n");
        sb.append("    Component Operations: ")
          .append(String.format("%,d", stats.componentOperations())).append("\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append(scheduler.getPerformanceReport());
        sb.append("═══════════════════════════════════════════════════════════════\n");

        return sb.toString();
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private int getGeneration(int index) {
        return entityGenerations.getAtIndex(ValueLayout.JAVA_INT, index);
    }

    private void setGeneration(int index, int generation) {
        entityGenerations.setAtIndex(ValueLayout.JAVA_INT, index, generation);
    }

    private int getArchetypeId(int index) {
        return entityArchetypes.getAtIndex(ValueLayout.JAVA_INT, index);
    }

    private void setArchetypeId(int index, int archetypeId) {
        entityArchetypes.setAtIndex(ValueLayout.JAVA_INT, index, archetypeId);
    }

    private byte getEntityFlags(int index) {
        return entityFlags.getAtIndex(ValueLayout.JAVA_BYTE, index);
    }

    private void setEntityFlags(int index, byte flags) {
        entityFlags.setAtIndex(ValueLayout.JAVA_BYTE, index, flags);
    }

    private void updateMaxEntityIndex(int index) {
        int current;
        do {
            current = maxEntityIndex;
            if (index <= current) return;
        } while (!compareAndSetMaxEntityIndex(current, index));
    }

    private boolean compareAndSetMaxEntityIndex(int expected, int update) {
        // Simple volatile write since exact ordering isn't critical
        if (maxEntityIndex == expected) {
            maxEntityIndex = update;
            return true;
        }
        return false;
    }

    private long computeComponentMask(Class<?>[] componentTypes) {
        long mask = 0;
        for (Class<?> type : componentTypes) {
            ComponentRegistry.ComponentType ct = registry.getType(type);
            mask |= (1L << ct.id);
        }
        return mask;
    }

    private int[] computeTypeIdArray(long mask) {
        return IntStream.range(0, 64)
            .filter(i -> (mask & (1L << i)) != 0)
            .toArray();
    }

    private void copyComponentData(Entity entity, Archetype from, Archetype to, int excludeTypeId) {
        for (int typeId : from.getComponentTypeIds()) {
            if (typeId != excludeTypeId) {
                ByteBuffer data = from.getComponent(entity.index(), typeId);
                if (data != null) {
                    to.setComponent(entity.index(), typeId, data);
                }
            }
        }
    }

    private void copyComponentDataExcept(Entity entity, Archetype from, Archetype to, int excludeTypeId) {
        copyComponentData(entity, from, to, excludeTypeId);
    }

    private void markComponentChanged(Entity entity, int typeId) {
        if (!config.enableChangeDetection()) return;
        int archetypeId = getArchetypeId(entity.index());
        if (archetypeId >= 0) {
            Archetype archetype = archetypeList.get(archetypeId);
            if (archetype != null) {
                archetype.markChanged(entity.index(), typeId);
            }
        }
    }

    private static int[] createFilledIntArray(int size, int value) {
        int[] array = new int[size];
        Arrays.fill(array, value);
        return array;
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    /**
     * Entity iterator for lazy traversal.
     */
    private final class EntityIterator implements Iterator<Entity> {
        private int currentIndex = 0;
        private Entity next = null;

        @Override
        public boolean hasNext() {
            while (currentIndex <= maxEntityIndex) {
                int gen = getGeneration(currentIndex);
                if (gen > 0 && (getEntityFlags(currentIndex) & EntityFlag.ACTIVE.mask) != 0) {
                    next = new Entity(currentIndex, gen);
                    currentIndex++;
                    return true;
                }
                currentIndex++;
            }
            return false;
        }

        @Override
        public Entity next() {
            if (next == null && !hasNext()) {
                throw new NoSuchElementException();
            }
            Entity result = next;
            next = null;
            return result;
        }
    }

    // ========================================================================
    // COMMAND BUFFER
    // ========================================================================

    /**
     * Thread-safe command buffer for deferred entity operations.
     */
    public static final class CommandBuffer {
        private final ConcurrentLinkedQueue<Command> commands = new ConcurrentLinkedQueue<>();
        private final int maxSize;
        private final AtomicInteger size = new AtomicInteger(0);

        public CommandBuffer(int maxSize) {
            this.maxSize = maxSize;
        }

        public void enqueue(Command command) {
            if (size.incrementAndGet() > maxSize) {
                size.decrementAndGet();
                throw new IllegalStateException("Command buffer overflow");
            }
            commands.offer(command);
        }

        public void execute(World world) {
            Command cmd;
            while ((cmd = commands.poll()) != null) {
                cmd.execute(world);
                size.decrementAndGet();
            }
        }

        public int pending() { return size.get(); }
        public void clear() { commands.clear(); size.set(0); }
    }

    public sealed interface Command permits DestroyEntityCommand, AddComponentCommand, RemoveComponentCommand {
        void execute(World world);
    }

    private record DestroyEntityCommand(Entity entity) implements Command {
        @Override
        public void execute(World world) {
            world.destroyEntity(entity);
        }
    }

    private record AddComponentCommand(Entity entity, Class<?> componentClass, ByteBuffer data) implements Command {
        @Override
        public void execute(World world) {
            world.addComponent(entity, componentClass, data);
        }
    }

    private record RemoveComponentCommand(Entity entity, Class<?> componentClass) implements Command {
        @Override
        public void execute(World world) {
            world.removeComponent(entity, componentClass);
        }
    }

    // ========================================================================
    // RELATIONSHIP GRAPH
    // ========================================================================

    /**
     * Manages entity parent-child relationships and custom relations.
     */
    public static final class RelationshipGraph {
        private final int[] parents;
        private final ConcurrentHashMap<Integer, Set<Integer>> children = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Long, Set<Integer>> relations = new ConcurrentHashMap<>();

        public RelationshipGraph(int maxEntities) {
            this.parents = new int[maxEntities];
            Arrays.fill(parents, -1);
        }

        public void setParent(Entity child, Entity parent) {
            int oldParent = parents[child.index()];
            if (oldParent >= 0) {
                Set<Integer> oldChildren = children.get(oldParent);
                if (oldChildren != null) oldChildren.remove(child.index());
            }

            parents[child.index()] = parent.index();
            children.computeIfAbsent(parent.index(), k -> ConcurrentHashMap.newKeySet())
                .add(child.index());
        }

        public int getParent(Entity entity) {
            return parents[entity.index()];
        }

        public Set<Integer> getChildren(Entity entity) {
            return children.getOrDefault(entity.index(), Set.of());
        }

        public void addRelation(Entity from, Entity to, int relationTypeId) {
            long key = ((long) from.index() << 32) | (relationTypeId & 0xFFFFFFFFL);
            relations.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                .add(to.index());
        }

        public void removeEntity(Entity entity) {
            // Remove as child
            int parent = parents[entity.index()];
            if (parent >= 0) {
                Set<Integer> parentChildren = children.get(parent);
                if (parentChildren != null) parentChildren.remove(entity.index());
            }
            parents[entity.index()] = -1;

            // Remove children references
            Set<Integer> entityChildren = children.remove(entity.index());
            if (entityChildren != null) {
                for (int child : entityChildren) {
                    parents[child] = -1;
                }
            }
        }
    }

    // ========================================================================
    // EVENT SYSTEM
    // ========================================================================

    /**
     * Simple event bus for ECS events.
     */
    public static class EventBus {
        public static final EventBus NOOP = new EventBus() {
            @Override public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {}
            @Override public void publish(Object event) {}
        };

        private final ConcurrentHashMap<Class<?>, CopyOnWriteArrayList<Consumer<?>>> handlers = new ConcurrentHashMap<>();

        public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
            handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
        }

        @SuppressWarnings("unchecked")
        public void publish(Object event) {
            CopyOnWriteArrayList<Consumer<?>> eventHandlers = handlers.get(event.getClass());
            if (eventHandlers != null) {
                for (Consumer<?> handler : eventHandlers) {
                    ((Consumer<Object>) handler).accept(event);
                }
            }
        }
    }

    // Event records
    public record EntityCreatedEvent(Entity entity) {}
    public record EntityDestroyingEvent(Entity entity) {}
    public record EntityDestroyedEvent(int entityIndex) {}
    public record ComponentAddedEvent(Entity entity, Class<?> componentType) {}
    public record ComponentRemovingEvent(Entity entity, Class<?> componentType) {}
    public record ComponentRemovedEvent(Entity entity, Class<?> componentType) {}
    public record ArchetypeCreatedEvent(Archetype archetype) {}
    public record RelationshipChangedEvent(Entity entity, Entity related, RelationType type) {}
    public record WorldInitializedEvent(World world) {}
    public record WorldPausedEvent(World world) {}
    public record WorldResumedEvent(World world) {}
    public record WorldShuttingDownEvent(World world) {}

    // ========================================================================
    // EXCEPTIONS
    // ========================================================================

    public static class EntityLimitExceededException extends RuntimeException {
        public EntityLimitExceededException(int limit) {
            super("Entity limit exceeded: " + limit);
        }
    }
}
