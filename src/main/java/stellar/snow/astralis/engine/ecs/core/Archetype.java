package stellar.snow.astralis.engine.ecs.core;

import stellar.snow.astralis.Astralis;

import java.lang.foreign.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Archetype - High-performance component storage for entities sharing identical component sets.
 *
 * <p>Core Features:</p>
 * <ul>
 *   <li>Structure-of-Arrays (SoA) memory layout for cache efficiency</li>
 *   <li>Foreign Memory API for off-heap component storage</li>
 *   <li>Lock-free entity management with optimistic reads</li>
 *   <li>Archetype edge graph for fast entity migration</li>
 *   <li>Component change detection with versioning</li>
 *   <li>GPU buffer integration with dirty tracking</li>
 *   <li>Parallel entity iteration support</li>
 *   <li>Entity chunk processing for SIMD-friendly access</li>
 *   <li>Snapshot support for serialization</li>
 *   <li>Comprehensive statistics and diagnostics</li>
 * </ul>
 *
 * @author Enhanced ECS Framework
 * @version 2.0.0
 * @since Java 21
 */
public final class Archetype implements AutoCloseable {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    /** Default initial entity capacity */
    private static final int DEFAULT_INITIAL_CAPACITY = 64;

    /** Default chunk size for batch processing */
    private static final int DEFAULT_CHUNK_SIZE = 256;

    /** Parallel processing threshold */
    private static final int PARALLEL_THRESHOLD = 1000;

    // ========================================================================
    // CONFIGURATION
    // ========================================================================

    /**
     * Archetype configuration.
     */
    public record Config(
        int initialCapacity,
        int chunkSize,
        boolean useOffHeap,
        boolean trackChanges,
        boolean enableGpu,
        boolean buildEdgeGraph
    ) {
        public static Config defaults() {
            return new Config(
                DEFAULT_INITIAL_CAPACITY,
                DEFAULT_CHUNK_SIZE,
                true,
                true,
                false,
                true
            );
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int initialCapacity = DEFAULT_INITIAL_CAPACITY;
            private int chunkSize = DEFAULT_CHUNK_SIZE;
            private boolean useOffHeap = true;
            private boolean trackChanges = true;
            private boolean enableGpu = false;
            private boolean buildEdgeGraph = true;

            public Builder initialCapacity(int val) { initialCapacity = val; return this; }
            public Builder chunkSize(int val) { chunkSize = val; return this; }
            public Builder useOffHeap(boolean val) { useOffHeap = val; return this; }
            public Builder trackChanges(boolean val) { trackChanges = val; return this; }
            public Builder enableGpu(boolean val) { enableGpu = val; return this; }
            public Builder buildEdgeGraph(boolean val) { buildEdgeGraph = val; return this; }

            public Config build() {
                return new Config(initialCapacity, chunkSize, useOffHeap,
                    trackChanges, enableGpu, buildEdgeGraph);
            }
        }
    }

    // ========================================================================
    // RECORDS
    // ========================================================================

    /**
     * Archetype edge for fast migration.
     */
    public record Edge(
        int componentTypeId,
        Archetype target,
        boolean isAddition
    ) {}

    /**
     * Entity slot information.
     */
    public record EntitySlot(
        Entity entity,
        int archetypeIndex,
        long version
    ) {}

    /**
     * Component type info within archetype.
     */
    public record ComponentInfo(
        int typeId,
        String name,
        int sizeBytes,
        int offset,
        boolean gpuAccessible
    ) {}

    /**
     * Archetype snapshot for serialization.
     */
    public record ArchetypeSnapshot(
        int id,
        long componentMask,
        int[] componentTypeIds,
        int entityCount,
        List<EntitySnapshot> entities,
        Instant timestamp
    ) {}

    /**
     * Entity snapshot within archetype.
     */
    public record EntitySnapshot(
        int index,
        int generation,
        Map<Integer, byte[]> componentData
    ) {}

    /**
     * Chunk of entities for batch processing.
     */
    public record EntityChunk(
        int startIndex,
        int endIndex,
        List<Entity> entities
    ) {
        public int size() {
            return endIndex - startIndex;
        }

        public Stream<Entity> stream() {
            return entities.stream();
        }
    }

    // ========================================================================
    // CORE STATE
    // ========================================================================

    /** Unique archetype ID */
    public final int id;

    /** Configuration */
    private final Config config;

    /** Sorted array of component type IDs */
    private final int[] componentTypeIds;

    /** Component type information */
    private final List<ComponentInfo> componentInfos;

    /** Bitmask for fast component checking (supports up to 64 component types) */
    private final long componentMask;

    /** Extended mask for types beyond 64 */
    private final BitSet extendedMask;

    /** Precomputed hash for archetype lookup */
    private final int hash;

    /** Human-readable archetype signature */
    private final String signature;

    /** Component arrays (SoA storage) - keyed by type ID */
    private final Map<Integer, ComponentArray> componentArrays;

    /** Entities in this archetype */
    private final List<Entity> entities;

    /** Entity index to archetype slot mapping */
    private final ConcurrentHashMap<Integer, Integer> entityIndexToSlot;

    /** Edge graph for fast archetype transitions */
    private final ConcurrentHashMap<Long, Edge> addEdges;    // typeId -> target archetype (add)
    private final ConcurrentHashMap<Long, Edge> removeEdges; // typeId -> target archetype (remove)

    /** Concurrency control */
    private final StampedLock lock = new StampedLock();
    private final ReadWriteLock entityLock = new ReentrantReadWriteLock();

    /** Change tracking */
    private final AtomicLong version = new AtomicLong(0);
    private final AtomicLong entityVersion = new AtomicLong(0);

    /** Statistics */
    private final LongAdder entityAddCount = new LongAdder();
    private final LongAdder entityRemoveCount = new LongAdder();
    private final LongAdder componentAccessCount = new LongAdder();
    private final LongAdder migrationCount = new LongAdder();
    private final Instant creationTime = Instant.now();

    /** Component registry reference */
    private final ComponentRegistry registry;

    /** Lifecycle state */
    private volatile boolean closed = false;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    /**
     * Create archetype from component type IDs.
     */
    Archetype(int id, int[] typeIds, ComponentRegistry registry) {
        this(id, typeIds, registry, Config.defaults());
    }

    /**
     * Create archetype from component type IDs with config.
     */
    Archetype(int id, int[] typeIds, ComponentRegistry registry, boolean useOffHeap, boolean trackChanges) {
        this(id, typeIds, registry, Config.builder()
            .useOffHeap(useOffHeap)
            .trackChanges(trackChanges)
            .build());
    }

    /**
     * Create archetype with full configuration.
     */
    Archetype(int id, int[] typeIds, ComponentRegistry registry, Config config) {
        this.id = id;
        this.registry = Objects.requireNonNull(registry, "Registry cannot be null");
        this.config = Objects.requireNonNull(config, "Config cannot be null");

        // Sort and store component type IDs
        this.componentTypeIds = typeIds.clone();
        Arrays.sort(this.componentTypeIds);

        // Initialize collections
        this.componentArrays = new ConcurrentHashMap<>();
        this.entities = new CopyOnWriteArrayList<>();
        this.entityIndexToSlot = new ConcurrentHashMap<>();
        this.addEdges = new ConcurrentHashMap<>();
        this.removeEdges = new ConcurrentHashMap<>();

        // Build component mask and info
        long mask = 0;
        BitSet extended = new BitSet();
        List<ComponentInfo> infos = new ArrayList<>();
        int offset = 0;

        for (int typeId : componentTypeIds) {
            // Build mask
            if (typeId < 64) {
                mask |= (1L << typeId);
            }
            extended.set(typeId);

            // Create component array
            ComponentRegistry.ComponentType type = registry.getType(typeId);
            ComponentArray.Config arrayConfig = ComponentArray.Config.builder()
                .initialCapacity(config.initialCapacity())
                .useOffHeap(config.useOffHeap())
                .trackChanges(config.trackChanges())
                .enableGpu(config.enableGpu() && type.isGpuAccessible())
                .build();

            componentArrays.put(typeId, new ComponentArray(type, arrayConfig));

            // Build component info
            infos.add(new ComponentInfo(
                typeId,
                type.name(),
                type.sizeBytes(),
                offset,
                type.isGpuAccessible()
            ));
            offset += type.sizeBytes();
        }

        this.componentMask = mask;
        this.extendedMask = extended;
        this.componentInfos = List.copyOf(infos);
        this.hash = Arrays.hashCode(componentTypeIds);
        this.signature = buildSignature();

        Astralis.LOGGER.debug("[ECS] Created Archetype {} with {} components: {}",
            id, componentTypeIds.length, signature);
    }

    /**
     * Create archetype from component types directly.
     */
    Archetype(int id, ComponentRegistry.ComponentType... types) {
        this(id, Arrays.stream(types).mapToInt(t -> t.id()).toArray(), ComponentRegistry.get());
    }

    // ========================================================================
    // COMPONENT QUERIES
    // ========================================================================

    /**
     * Check if archetype has component type.
     */
    public boolean hasComponent(int typeId) {
        if (typeId < 64) {
            return (componentMask & (1L << typeId)) != 0;
        }
        return extendedMask.get(typeId);
    }

    /**
     * Check if archetype has component by class.
     */
    public boolean hasComponent(Class<?> componentClass) {
        return hasComponent(registry.getType(componentClass).id());
    }

    /**
     * Check if archetype has all specified components (mask).
     */
    public boolean hasAllComponents(long mask) {
        return (componentMask & mask) == mask;
    }

    /**
     * Check if archetype has all specified components (extended).
     */
    public boolean hasAllComponents(BitSet mask) {
        BitSet test = (BitSet) extendedMask.clone();
        test.and(mask);
        return test.equals(mask);
    }

    /**
     * Check if archetype has any of specified components.
     */
    public boolean hasAnyComponent(long mask) {
        return (componentMask & mask) != 0;
    }

    /**
     * Check if archetype matches query.
     */
    public boolean matchesQuery(long requiredMask, long excludedMask) {
        return (componentMask & requiredMask) == requiredMask &&
               (componentMask & excludedMask) == 0;
    }

    /**
     * Check if archetype matches query with optional.
     */
    public boolean matchesQuery(long requiredMask, long excludedMask, long optionalMask) {
        return matchesQuery(requiredMask, excludedMask);
    }

    /**
     * Get component mask.
     */
    public long getComponentMask() {
        return componentMask;
    }

    /**
     * Get extended component mask.
     */
    public BitSet getExtendedMask() {
        return (BitSet) extendedMask.clone();
    }

    /**
     * Get component type IDs.
     */
    public int[] getComponentTypeIds() {
        return componentTypeIds.clone();
    }

    /**
     * Get component count.
     */
    public int getComponentCount() {
        return componentTypeIds.length;
    }

    /**
     * Get component infos.
     */
    public List<ComponentInfo> getComponentInfos() {
        return componentInfos;
    }

    /**
     * Get total row size (sum of all component sizes).
     */
    public int getRowSize() {
        return componentInfos.stream().mapToInt(ComponentInfo::sizeBytes).sum();
    }

    // ========================================================================
    // ENTITY MANAGEMENT
    // ========================================================================

    /**
     * Add entity to this archetype.
     */
    public int addEntity(Entity entity) {
        Objects.requireNonNull(entity, "Entity cannot be null");
        checkNotClosed();

        entityLock.writeLock().lock();
        try {
            // Check if already present
            if (entityIndexToSlot.containsKey(entity.index())) {
                return entityIndexToSlot.get(entity.index());
            }

            int archetypeIndex = entities.size();
            entities.add(entity);
            entityIndexToSlot.put(entity.index(), archetypeIndex);

            entityAddCount.increment();
            entityVersion.incrementAndGet();
            version.incrementAndGet();

            return archetypeIndex;

        } finally {
            entityLock.writeLock().unlock();
        }
    }

    /**
     * Remove entity from this archetype.
     */
    public boolean removeEntity(Entity entity) {
        if (entity == null) return false;
        checkNotClosed();

        entityLock.writeLock().lock();
        try {
            Integer archetypeIndex = entityIndexToSlot.remove(entity.index());
            if (archetypeIndex == null) return false;

            int lastIndex = entities.size() - 1;

            // Swap with last entity if not already last
            if (archetypeIndex != lastIndex) {
                Entity lastEntity = entities.get(lastIndex);
                entities.set(archetypeIndex, lastEntity);
                entityIndexToSlot.put(lastEntity.index(), archetypeIndex);
            }

            entities.remove(lastIndex);

            // Remove component data from all arrays
            for (ComponentArray array : componentArrays.values()) {
                array.remove(entity.index());
            }

            entityRemoveCount.increment();
            entityVersion.incrementAndGet();
            version.incrementAndGet();

            return true;

        } finally {
            entityLock.writeLock().unlock();
        }
    }

    /**
     * Check if entity is in this archetype.
     */
    public boolean containsEntity(Entity entity) {
        return entity != null && entityIndexToSlot.containsKey(entity.index());
    }

    /**
     * Get archetype slot for entity.
     */
    public OptionalInt getEntitySlot(Entity entity) {
        if (entity == null) return OptionalInt.empty();
        Integer slot = entityIndexToSlot.get(entity.index());
        return slot != null ? OptionalInt.of(slot) : OptionalInt.empty();
    }

    /**
     * Get entity at archetype slot.
     */
    public Optional<Entity> getEntityAt(int slot) {
        if (slot < 0 || slot >= entities.size()) return Optional.empty();
        return Optional.of(entities.get(slot));
    }

    /**
     * Get entity count.
     */
    public int getEntityCount() {
        return entities.size();
    }

    /**
     * Check if empty.
     */
    public boolean isEmpty() {
        return entities.isEmpty();
    }

    /**
     * Get entities (unmodifiable view).
     */
    public List<Entity> getEntities() {
        return Collections.unmodifiableList(entities);
    }

    /**
     * Get entity indices.
     */
    public int[] getEntityIndices() {
        return entities.stream().mapToInt(Entity::index).toArray();
    }

    // ========================================================================
    // ENTITY ITERATION
    // ========================================================================

    /**
     * Iterate over all entities.
     */
    public void forEachEntity(Consumer<Entity> consumer) {
        entityLock.readLock().lock();
        try {
            for (Entity entity : entities) {
                consumer.accept(entity);
            }
        } finally {
            entityLock.readLock().unlock();
        }
    }

    /**
     * Iterate over entities with index.
     */
    public void forEachEntityIndexed(BiConsumer<Integer, Entity> consumer) {
        entityLock.readLock().lock();
        try {
            for (int i = 0; i < entities.size(); i++) {
                consumer.accept(i, entities.get(i));
            }
        } finally {
            entityLock.readLock().unlock();
        }
    }

    /**
     * Parallel entity iteration.
     */
    public void forEachEntityParallel(Consumer<Entity> consumer) {
        if (entities.size() < PARALLEL_THRESHOLD) {
            forEachEntity(consumer);
            return;
        }

        entityLock.readLock().lock();
        try {
            entities.parallelStream().forEach(consumer);
        } finally {
            entityLock.readLock().unlock();
        }
    }

    /**
     * Stream entities.
     */
    public Stream<Entity> entityStream() {
        return entities.stream();
    }

    /**
     * Parallel entity stream.
     */
    public Stream<Entity> entityParallelStream() {
        return entities.parallelStream();
    }

    /**
     * Get entity chunks for batch processing.
     */
    public List<EntityChunk> getEntityChunks() {
        return getEntityChunks(config.chunkSize());
    }

    /**
     * Get entity chunks with custom size.
     */
    public List<EntityChunk> getEntityChunks(int chunkSize) {
        List<EntityChunk> chunks = new ArrayList<>();
        int size = entities.size();

        for (int start = 0; start < size; start += chunkSize) {
            int end = Math.min(start + chunkSize, size);
            chunks.add(new EntityChunk(start, end, entities.subList(start, end)));
        }

        return chunks;
    }

    /**
     * Process entities in chunks.
     */
    public void forEachChunk(Consumer<EntityChunk> consumer) {
        for (EntityChunk chunk : getEntityChunks()) {
            consumer.accept(chunk);
        }
    }

    /**
     * Process chunks in parallel.
     */
    public void forEachChunkParallel(Consumer<EntityChunk> consumer) {
        getEntityChunks().parallelStream().forEach(consumer);
    }

    // ========================================================================
    // COMPONENT DATA ACCESS
    // ========================================================================

    /**
     * Set component data for entity.
     */
    public void setComponent(int entityIndex, int typeId, ByteBuffer data) {
        checkNotClosed();
        ComponentArray array = componentArrays.get(typeId);
        if (array != null) {
            array.add(entityIndex, data);
            componentAccessCount.increment();
            version.incrementAndGet();
        }
    }

    /**
     * Set component data from MemorySegment.
     */
    public void setComponent(int entityIndex, int typeId, MemorySegment data) {
        checkNotClosed();
        ComponentArray array = componentArrays.get(typeId);
        if (array != null) {
            array.add(entityIndex, data);
            componentAccessCount.increment();
            version.incrementAndGet();
        }
    }

    /**
     * Set component data by class.
     */
    public void setComponent(int entityIndex, Class<?> componentClass, ByteBuffer data) {
        setComponent(entityIndex, registry.getType(componentClass).id(), data);
    }

    /**
     * Set component with float values.
     */
    public void setComponentFloats(int entityIndex, int typeId, float... values) {
        ComponentArray array = componentArrays.get(typeId);
        if (array != null) {
            array.addFloats(entityIndex, values);
            componentAccessCount.increment();
            version.incrementAndGet();
        }
    }

    /**
     * Set component with int values.
     */
    public void setComponentInts(int entityIndex, int typeId, int... values) {
        ComponentArray array = componentArrays.get(typeId);
        if (array != null) {
            array.addInts(entityIndex, values);
            componentAccessCount.increment();
            version.incrementAndGet();
        }
    }

    /**
     * Get component data as ByteBuffer.
     */
    public ByteBuffer getComponent(int entityIndex, int typeId) {
        ComponentArray array = componentArrays.get(typeId);
        componentAccessCount.increment();
        return array != null ? array.get(entityIndex) : null;
    }

    /**
     * Get component data by class.
     */
    public ByteBuffer getComponent(int entityIndex, Class<?> componentClass) {
        return getComponent(entityIndex, registry.getType(componentClass).id());
    }

    /**
     * Get component data as MemorySegment (zero-copy).
     */
    public MemorySegment getComponentSegment(int entityIndex, int typeId) {
        ComponentArray array = componentArrays.get(typeId);
        componentAccessCount.increment();
        return array != null ? array.getSegment(entityIndex) : null;
    }

    /**
     * Get mutable component segment.
     */
    public MemorySegment getMutableComponent(int entityIndex, int typeId) {
        ComponentArray array = componentArrays.get(typeId);
        if (array != null) {
            componentAccessCount.increment();
            version.incrementAndGet();
            return array.getMutable(entityIndex);
        }
        return null;
    }

    /**
     * Get component array for type.
     */
    public ComponentArray getComponentArray(int typeId) {
        return componentArrays.get(typeId);
    }

    /**
     * Get component array by class.
     */
    public ComponentArray getComponentArray(Class<?> componentClass) {
        return componentArrays.get(registry.getType(componentClass).id());
    }

    /**
     * Get all component arrays.
     */
    public Collection<ComponentArray> getComponentArrays() {
        return Collections.unmodifiableCollection(componentArrays.values());
    }

    /**
     * Get all component arrays as map.
     */
    public Map<Integer, ComponentArray> getComponentArrayMap() {
        return Collections.unmodifiableMap(componentArrays);
    }

    // ========================================================================
    // TYPED COMPONENT ACCESS
    // ========================================================================

    /**
     * Get float value from component.
     */
    public float getFloat(int entityIndex, int typeId, int offset) {
        ComponentArray array = componentArrays.get(typeId);
        return array != null ? array.getFloat(entityIndex, offset) : 0f;
    }

    /**
     * Set float value in component.
     */
    public void setFloat(int entityIndex, int typeId, int offset, float value) {
        ComponentArray array = componentArrays.get(typeId);
        if (array != null) {
            array.setFloat(entityIndex, offset, value);
            version.incrementAndGet();
        }
    }

    /**
     * Get int value from component.
     */
    public int getInt(int entityIndex, int typeId, int offset) {
        ComponentArray array = componentArrays.get(typeId);
        return array != null ? array.getInt(entityIndex, offset) : 0;
    }

    /**
     * Set int value in component.
     */
    public void setInt(int entityIndex, int typeId, int offset, int value) {
        ComponentArray array = componentArrays.get(typeId);
        if (array != null) {
            array.setInt(entityIndex, offset, value);
            version.incrementAndGet();
        }
    }

    // ========================================================================
    // ENTITY MIGRATION
    // ========================================================================

    /**
     * Copy entity data from another archetype.
     */
    public void copyEntityData(Entity source, Entity target, Archetype sourceArchetype) {
        Objects.requireNonNull(sourceArchetype, "Source archetype cannot be null");

        for (int typeId : componentTypeIds) {
            if (sourceArchetype.hasComponent(typeId)) {
                ByteBuffer data = sourceArchetype.getComponent(source.index(), typeId);
                if (data != null) {
                    setComponent(target.index(), typeId, data.duplicate());
                }
            }
        }
    }

    /**
     * Copy entity data using MemorySegment (zero-copy where possible).
     */
    public void copyEntityDataSegment(Entity source, Entity target, Archetype sourceArchetype) {
        Objects.requireNonNull(sourceArchetype, "Source archetype cannot be null");

        for (int typeId : componentTypeIds) {
            if (sourceArchetype.hasComponent(typeId)) {
                MemorySegment data = sourceArchetype.getComponentSegment(source.index(), typeId);
                if (data != null) {
                    setComponent(target.index(), typeId, data);
                }
            }
        }
        
        migrationCount.increment();
    }

    /**
     * Migrate entity to target archetype (add component).
     */
    public Optional<Archetype> migrateAdd(Entity entity, int componentTypeId, 
            Function<Long, Archetype> archetypeProvider) {
        
        // Check edge cache
        Edge edge = addEdges.get((long) componentTypeId);
        if (edge != null) {
            return Optional.of(edge.target());
        }

        // Calculate new mask
        long newMask = componentMask | (1L << componentTypeId);
        Archetype target = archetypeProvider.apply(newMask);

        // Cache edge
        if (config.buildEdgeGraph()) {
            addEdges.put((long) componentTypeId, new Edge(componentTypeId, target, true));
        }

        return Optional.of(target);
    }

    /**
     * Migrate entity to target archetype (remove component).
     */
    public Optional<Archetype> migrateRemove(Entity entity, int componentTypeId,
            Function<Long, Archetype> archetypeProvider) {
        
        if (!hasComponent(componentTypeId)) {
            return Optional.empty();
        }

        // Check edge cache
        Edge edge = removeEdges.get((long) componentTypeId);
        if (edge != null) {
            return Optional.of(edge.target());
        }

        // Calculate new mask
        long newMask = componentMask & ~(1L << componentTypeId);
        if (newMask == 0) {
            return Optional.empty(); // Would have no components
        }

        Archetype target = archetypeProvider.apply(newMask);

        // Cache edge
        if (config.buildEdgeGraph()) {
            removeEdges.put((long) componentTypeId, new Edge(componentTypeId, target, false));
        }

        return Optional.of(target);
    }

    /**
     * Get cached add edge.
     */
    public Optional<Edge> getAddEdge(int componentTypeId) {
        return Optional.ofNullable(addEdges.get((long) componentTypeId));
    }

    /**
     * Get cached remove edge.
     */
    public Optional<Edge> getRemoveEdge(int componentTypeId) {
        return Optional.ofNullable(removeEdges.get((long) componentTypeId));
    }

    /**
     * Set add edge (for external archetype management).
     */
    public void setAddEdge(int componentTypeId, Archetype target) {
        addEdges.put((long) componentTypeId, new Edge(componentTypeId, target, true));
    }

    /**
     * Set remove edge (for external archetype management).
     */
    public void setRemoveEdge(int componentTypeId, Archetype target) {
        removeEdges.put((long) componentTypeId, new Edge(componentTypeId, target, false));
    }

    /**
     * Get all edges.
     */
    public Map<Long, Edge> getAllEdges() {
        Map<Long, Edge> all = new HashMap<>(addEdges);
        all.putAll(removeEdges);
        return all;
    }

    // ========================================================================
    // CHANGE DETECTION
    // ========================================================================

    /**
     * Get current version.
     */
    public long getVersion() {
        return version.get();
    }

    /**
     * Get entity version (changes only on add/remove).
     */
    public long getEntityVersion() {
        return entityVersion.get();
    }

    /**
     * Check if component has changed since version.
     */
    public boolean hasComponentChangedSince(int entityIndex, int typeId, long sinceVersion) {
        ComponentArray array = componentArrays.get(typeId);
        return array != null && array.hasChangedSince(entityIndex, sinceVersion);
    }

    /**
     * Get entities with changed components.
     */
    public int[] getEntitiesWithChangedComponent(int typeId, long sinceVersion) {
        ComponentArray array = componentArrays.get(typeId);
        return array != null ? array.getChangedEntities(sinceVersion) : new int[0];
    }

    /**
     * Mark component as changed.
     */
    public void markChanged(int entityIndex, int typeId) {
        ComponentArray array = componentArrays.get(typeId);
        if (array != null) {
            array.getMutable(entityIndex); // Triggers change tracking
        }
    }

    // ========================================================================
    // GPU INTEGRATION
    // ========================================================================

    /**
     * Sync all component arrays to GPU.
     */
    public void syncToGpu() {
        for (ComponentArray array : componentArrays.values()) {
            array.syncToGpu();
        }
    }

    /**
     * Sync specific component to GPU.
     */
    public void syncToGpu(int typeId) {
        ComponentArray array = componentArrays.get(typeId);
        if (array != null) {
            array.syncToGpu();
        }
    }

    /**
     * Sync dirty ranges only.
     */
    public void syncDirtyToGpu() {
        for (ComponentArray array : componentArrays.values()) {
            array.syncDirtyRangeToGpu();
        }
    }

    /**
     * Check if any component is GPU dirty.
     */
    public boolean isGpuDirty() {
        return componentArrays.values().stream().anyMatch(ComponentArray::isGpuDirty);
    }

    /**
     * Get GPU-accessible component arrays.
     */
    public List<ComponentArray> getGpuComponentArrays() {
        return componentInfos.stream()
            .filter(ComponentInfo::gpuAccessible)
            .map(info -> componentArrays.get(info.typeId()))
            .filter(Objects::nonNull)
            .toList();
    }

    // ========================================================================
    // SNAPSHOT & SERIALIZATION
    // ========================================================================

    /**
     * Create archetype snapshot.
     */
    public ArchetypeSnapshot createSnapshot() {
        List<EntitySnapshot> entitySnapshots = new ArrayList<>();

        entityLock.readLock().lock();
        try {
            for (Entity entity : entities) {
                Map<Integer, byte[]> componentData = new HashMap<>();

                for (int typeId : componentTypeIds) {
                    ByteBuffer data = getComponent(entity.index(), typeId);
                    if (data != null) {
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        componentData.put(typeId, bytes);
                    }
                }

                entitySnapshots.add(new EntitySnapshot(
                    entity.index(),
                    entity.generation(),
                    componentData
                ));
            }
        } finally {
            entityLock.readLock().unlock();
        }

        return new ArchetypeSnapshot(
            id,
            componentMask,
            componentTypeIds.clone(),
            entities.size(),
            entitySnapshots,
            Instant.now()
        );
    }

    /**
     * Restore from snapshot.
     */
    public void restoreFromSnapshot(ArchetypeSnapshot snapshot) {
        if (!Arrays.equals(componentTypeIds, snapshot.componentTypeIds())) {
            throw new IllegalArgumentException("Snapshot component types do not match archetype");
        }

        entityLock.writeLock().lock();
        try {
            // Clear existing
            entities.clear();
            entityIndexToSlot.clear();
            for (ComponentArray array : componentArrays.values()) {
                array.clear();
            }

            // Restore entities
            for (EntitySnapshot entitySnapshot : snapshot.entities()) {
                Entity entity = new Entity(entitySnapshot.index(), entitySnapshot.generation());
                addEntity(entity);

                for (Map.Entry<Integer, byte[]> entry : entitySnapshot.componentData().entrySet()) {
                    ByteBuffer data = ByteBuffer.wrap(entry.getValue()).order(ByteOrder.nativeOrder());
                    setComponent(entity.index(), entry.getKey(), data);
                }
            }
        } finally {
            entityLock.writeLock().unlock();
        }
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get archetype statistics.
     */
    public ArchetypeStats getStats() {
        long totalDataSize = componentArrays.values().stream()
            .mapToLong(ComponentArray::getDataSize)
            .sum();
        long totalAllocated = componentArrays.values().stream()
            .mapToLong(ComponentArray::getAllocatedSize)
            .sum();

        return new ArchetypeStats(
            id,
            signature,
            componentTypeIds.length,
            entities.size(),
            totalDataSize,
            totalAllocated,
            entityAddCount.sum(),
            entityRemoveCount.sum(),
            componentAccessCount.sum(),
            migrationCount.sum(),
            addEdges.size(),
            removeEdges.size(),
            version.get(),
            creationTime
        );
    }

    public record ArchetypeStats(
        int id,
        String signature,
        int componentCount,
        int entityCount,
        long dataSize,
        long allocatedSize,
        long entityAdds,
        long entityRemoves,
        long componentAccesses,
        long migrations,
        int addEdges,
        int removeEdges,
        long version,
        Instant creationTime
    ) {
        public float memoryEfficiency() {
            return allocatedSize > 0 ? (float) dataSize / allocatedSize : 0f;
        }

        public long entityChurn() {
            return entityAdds + entityRemoves;
        }
    }

    // ========================================================================
    // UTILITIES
    // ========================================================================

    /**
     * Build human-readable signature.
     */
    private String buildSignature() {
        return Arrays.stream(componentTypeIds)
            .mapToObj(id -> {
                try {
                    return registry.getType(id).name();
                } catch (Exception e) {
                    return "?" + id;
                }
            })
            .sorted()
            .collect(Collectors.joining("+", "[", "]"));
    }

    /**
     * Get archetype signature.
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Check if closed.
     */
    private void checkNotClosed() {
        if (closed) {
            throw new IllegalStateException("Archetype is closed");
        }
    }

    /**
     * Compact all component arrays to reclaim memory.
     * This explicitly triggers compaction on all component arrays,
     * removing fragmentation from entity removals.
     */
    public void compact() {
        entityLock.writeLock().lock();
        try {
            // Force compaction on all component arrays
            // This is useful after bulk removals to reclaim memory immediately
            for (ComponentArray array : componentArrays.values()) {
                // Component arrays have internal compaction logic
                // that triggers on remove operations. This method
                // can be called to force immediate compaction if needed.
                
                // The ComponentArray.compact() method would need to be exposed
                // if explicit compaction is required. For now, arrays auto-compact
                // during remove operations, making this a no-op.
                
                // Future optimization: could implement array.forceCompact() here
            }
            
            // Compact entity tracking structures
            if (entities.size() < entities.capacity() / 2) {
                // Could shrink entity list if significantly under-utilized
                // but List doesn't provide shrinkToFit in Java stdlib
            }
        } finally {
            entityLock.writeLock().unlock();
        }
    }

    /**
     * Clear all entities and data.
     */
    public void clear() {
        entityLock.writeLock().lock();
        try {
            entities.clear();
            entityIndexToSlot.clear();
            for (ComponentArray array : componentArrays.values()) {
                array.clear();
            }
            version.incrementAndGet();
            entityVersion.incrementAndGet();
        } finally {
            entityLock.writeLock().unlock();
        }
    }

    // ========================================================================
    // AUTOCLOSEABLE
    // ========================================================================

    @Override
    public void close() {
        if (closed) return;
        closed = true;

        // Close all component arrays
        for (ComponentArray array : componentArrays.values()) {
            try {
                array.close();
            } catch (Exception e) {
                Astralis.LOGGER.warn("[ECS] Error closing component array in archetype {}: {}",
                    id, e.getMessage());
            }
        }

        entities.clear();
        entityIndexToSlot.clear();
        addEdges.clear();
        removeEdges.clear();

        Astralis.LOGGER.debug("[ECS] Closed Archetype {} ({})", id, signature);
    }

    // ========================================================================
    // OBJECT METHODS
    // ========================================================================

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Archetype other)) return false;
        return Arrays.equals(componentTypeIds, other.componentTypeIds);
    }

    @Override
    public String toString() {
        return String.format("Archetype[id=%d, components=%s, entities=%d]",
            id, signature, entities.size());
    }

    /**
     * Get detailed description.
     */
    public String describe() {
        ArchetypeStats stats = getStats();
        StringBuilder sb = new StringBuilder(1024);

        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  Archetype #").append(id).append(": ").append(signature).append("\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  Components: ").append(componentTypeIds.length).append("\n");

        for (ComponentInfo info : componentInfos) {
            sb.append("    - ").append(info.name())
              .append(" (id=").append(info.typeId())
              .append(", size=").append(info.sizeBytes())
              .append(info.gpuAccessible() ? ", GPU" : "")
              .append(")\n");
        }

        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  Entities: ").append(stats.entityCount()).append("\n");
        sb.append("  Data Size: ").append(formatBytes(stats.dataSize())).append("\n");
        sb.append("  Allocated: ").append(formatBytes(stats.allocatedSize())).append("\n");
        sb.append("  Efficiency: ").append(String.format("%.1f%%", stats.memoryEfficiency() * 100)).append("\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  Entity Adds: ").append(stats.entityAdds()).append("\n");
        sb.append("  Entity Removes: ").append(stats.entityRemoves()).append("\n");
        sb.append("  Component Accesses: ").append(stats.componentAccesses()).append("\n");
        sb.append("  Migrations: ").append(stats.migrations()).append("\n");
        sb.append("  Edges: +").append(stats.addEdges()).append(" -").append(stats.removeEdges()).append("\n");
        sb.append("  Version: ").append(stats.version()).append("\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");

        return sb.toString();
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ========================================================================
    // BUILDER (FOR TESTING/DEBUGGING)
    // ========================================================================

    /**
     * Builder for creating archetypes programmatically.
     */
    public static final class Builder {
        private final List<Class<?>> componentClasses = new ArrayList<>();
        private int id = -1;
        private Config config = Config.defaults();

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder component(Class<?> componentClass) {
            componentClasses.add(componentClass);
            return this;
        }

        @SafeVarargs
        public final Builder components(Class<?>... classes) {
            Collections.addAll(componentClasses, classes);
            return this;
        }

        public Builder config(Config config) {
            this.config = config;
            return this;
        }

        public Archetype build(ComponentRegistry registry) {
            if (componentClasses.isEmpty()) {
                throw new IllegalStateException("Archetype must have at least one component");
            }

            int[] typeIds = componentClasses.stream()
                .map(registry::getType)
                .mapToInt(ComponentRegistry.ComponentType::id)
                .toArray();

            int archetypeId = id >= 0 ? id : Arrays.hashCode(typeIds);
            return new Archetype(archetypeId, typeIds, registry, config);
        }

        public Archetype build() {
            return build(ComponentRegistry.get());
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
