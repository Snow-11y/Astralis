package stellar.snow.astralis.engine.ecs.storage;

import stellar.snow.astralis.Astralis;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * ComponentArray - High-performance SoA storage using Foreign Memory API.
 *
 * <p>Core Features:</p>
 * <ul>
 *   <li>Foreign Memory API for off-heap, cache-friendly storage</li>
 *   <li>Lock-free sparse set indexing</li>
 *   <li>GPU buffer integration with persistent mapping</li>
 *   <li>Change detection with versioning</li>
 *   <li>SIMD-friendly memory layout</li>
 *   <li>Batch operations for bulk updates</li>
 *   <li>Memory pooling for reduced allocations</li>
 *   <li>Zero-copy MemorySegment access</li>
 *   <li>Automatic capacity management</li>
 *   <li>Thread-safe concurrent access</li>
 * </ul>
 *
 * @author Enhanced ECS Framework
 * @version 2.0.0
 * @since Java 21
 */
public final class ComponentArray implements AutoCloseable {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    private static final int DEFAULT_INITIAL_CAPACITY = 256;
    private static final int DEFAULT_ENTITY_CAPACITY = 4096;
    private static final float GROWTH_FACTOR = 1.5f;
    private static final int CACHE_LINE_SIZE = 64;

    private static final int INVALID_INDEX = -1;

    // ========================================================================
    // CONFIGURATION
    // ========================================================================

    /**
     * Storage configuration.
     */
    public record Config(
        int initialCapacity,
        int entityCapacity,
        boolean useOffHeap,
        boolean trackChanges,
        boolean enableGpu,
        int alignment
    ) {
        public static Config defaults() {
            return new Config(
                DEFAULT_INITIAL_CAPACITY,
                DEFAULT_ENTITY_CAPACITY,
                true,   // Off-heap by default
                true,   // Track changes
                false,  // GPU disabled by default
                CACHE_LINE_SIZE
            );
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int initialCapacity = DEFAULT_INITIAL_CAPACITY;
            private int entityCapacity = DEFAULT_ENTITY_CAPACITY;
            private boolean useOffHeap = true;
            private boolean trackChanges = true;
            private boolean enableGpu = false;
            private int alignment = CACHE_LINE_SIZE;

            public Builder initialCapacity(int val) { initialCapacity = val; return this; }
            public Builder entityCapacity(int val) { entityCapacity = val; return this; }
            public Builder useOffHeap(boolean val) { useOffHeap = val; return this; }
            public Builder trackChanges(boolean val) { trackChanges = val; return this; }
            public Builder enableGpu(boolean val) { enableGpu = val; return this; }
            public Builder alignment(int val) { alignment = val; return this; }

            public Config build() {
                return new Config(initialCapacity, entityCapacity, useOffHeap, 
                    trackChanges, enableGpu, alignment);
            }
        }
    }

    // ========================================================================
    // CORE STATE
    // ========================================================================

    private final ComponentRegistry.ComponentType type;
    private final int componentSize;
    private final int alignment;
    private final Config config;

    // Memory arena for off-heap allocation
    private final Arena arena;

    // Raw data storage (off-heap)
    private MemorySegment data;
    private volatile int capacity;
    private final AtomicInteger count = new AtomicInteger(0);

    // Sparse set: entity index -> dense index
    private MemorySegment entityToIndex;  // int[]
    private volatile int entityCapacity;

    // Dense array: dense index -> entity index
    private MemorySegment indexToEntity;  // int[]

    // Change tracking
    private final AtomicLong version = new AtomicLong(0);
    private MemorySegment changeVersions;  // long[] per entity
    private final boolean trackChanges;

    // Dirty tracking for GPU sync
    private final AtomicBoolean gpuDirty = new AtomicBoolean(true);
    private volatile int dirtyRangeStart = 0;
    private volatile int dirtyRangeEnd = 0;

    // GPU buffer
    private volatile long gpuBuffer;
    private volatile MemorySegment gpuMappedSegment;

    // Concurrency
    private final StampedLock lock = new StampedLock();
    private final ReadWriteLock dataLock = new ReentrantReadWriteLock();

    // Statistics
    private final LongAdder addCount = new LongAdder();
    private final LongAdder removeCount = new LongAdder();
    private final LongAdder updateCount = new LongAdder();
    private final LongAdder resizeCount = new LongAdder();

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    /**
     * Create component array with type and default config.
     */
    public ComponentArray(ComponentRegistry.ComponentType type) {
        this(type, Config.defaults());
    }

    /**
     * Create component array with type and initial capacity.
     */
    public ComponentArray(ComponentRegistry.ComponentType type, int initialCapacity) {
        this(type, Config.builder().initialCapacity(initialCapacity).build());
    }

    /**
     * Create component array with full configuration.
     */
    public ComponentArray(ComponentRegistry.ComponentType type, Config config) {
        this.type = Objects.requireNonNull(type, "Component type cannot be null");
        this.config = Objects.requireNonNull(config, "Config cannot be null");
        this.componentSize = Math.max(type.sizeBytes(), 1);  // Minimum 1 byte for tags
        this.alignment = Math.max(config.alignment(), type.alignment());
        this.trackChanges = config.trackChanges();

        // Initialize arena
        this.arena = config.useOffHeap() ? Arena.ofShared() : Arena.ofConfined();

        // Allocate initial storage
        this.capacity = config.initialCapacity();
        this.entityCapacity = config.entityCapacity();

        allocateStorage();

        Astralis.LOGGER.debug("[ECS] Created ComponentArray for {} (size={}, capacity={})",
            type.name(), componentSize, capacity);
    }

    // ========================================================================
    // MEMORY ALLOCATION
    // ========================================================================

    private void allocateStorage() {
        // Data storage (aligned)
        long dataSize = alignUp((long) componentSize * capacity, alignment);
        this.data = arena.allocate(dataSize, alignment);

        // Entity to index mapping
        this.entityToIndex = arena.allocate(ValueLayout.JAVA_INT, entityCapacity);
        fillSegment(entityToIndex, INVALID_INDEX);

        // Index to entity mapping
        this.indexToEntity = arena.allocate(ValueLayout.JAVA_INT, capacity);
        fillSegment(indexToEntity, INVALID_INDEX);

        // Change tracking
        if (trackChanges) {
            this.changeVersions = arena.allocate(ValueLayout.JAVA_LONG, capacity);
            changeVersions.fill((byte) 0);
        }
    }

    private void ensureCapacity(int required) {
        if (required <= capacity) return;

        int newCapacity = Math.max(
            (int) (capacity * GROWTH_FACTOR),
            required
        );

        long stamp = lock.writeLock();
        try {
            if (required <= capacity) return; // Double-check

            // Allocate new data segment
            long newDataSize = alignUp((long) componentSize * newCapacity, alignment);
            MemorySegment newData = arena.allocate(newDataSize, alignment);

            // Copy existing data
            MemorySegment.copy(data, 0, newData, 0, (long) count.get() * componentSize);

            // Allocate new index to entity mapping
            MemorySegment newIndexToEntity = arena.allocate(ValueLayout.JAVA_INT, newCapacity);
            MemorySegment.copy(indexToEntity, ValueLayout.JAVA_INT, 0,
                newIndexToEntity, ValueLayout.JAVA_INT, 0,
                Math.min(capacity, newCapacity));
            fillSegmentRange(newIndexToEntity, capacity, newCapacity, INVALID_INDEX);

            // Allocate new change versions if tracking
            MemorySegment newChangeVersions = null;
            if (trackChanges) {
                newChangeVersions = arena.allocate(ValueLayout.JAVA_LONG, newCapacity);
                MemorySegment.copy(changeVersions, ValueLayout.JAVA_LONG, 0,
                    newChangeVersions, ValueLayout.JAVA_LONG, 0,
                    Math.min(capacity, newCapacity));
            }

            // Update references
            data = newData;
            indexToEntity = newIndexToEntity;
            if (trackChanges) {
                changeVersions = newChangeVersions;
            }
            capacity = newCapacity;

            resizeCount.increment();
            gpuDirty.set(true);

        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private void ensureEntityCapacity(int required) {
        if (required <= entityCapacity) return;

        int newCapacity = Math.max(
            (int) (entityCapacity * GROWTH_FACTOR),
            required
        );

        long stamp = lock.writeLock();
        try {
            if (required <= entityCapacity) return;

            MemorySegment newEntityToIndex = arena.allocate(ValueLayout.JAVA_INT, newCapacity);
            MemorySegment.copy(entityToIndex, ValueLayout.JAVA_INT, 0,
                newEntityToIndex, ValueLayout.JAVA_INT, 0,
                entityCapacity);
            fillSegmentRange(newEntityToIndex, entityCapacity, newCapacity, INVALID_INDEX);

            entityToIndex = newEntityToIndex;
            entityCapacity = newCapacity;

        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // ========================================================================
    // ADD/UPDATE OPERATIONS
    // ========================================================================

    /**
     * Add or update component data for an entity.
     */
    public void add(int entityIndex, ByteBuffer componentData) {
        Objects.requireNonNull(componentData, "Component data cannot be null");
        ensureEntityCapacity(entityIndex + 1);

        long stamp = lock.writeLock();
        try {
            int existingIndex = getEntityIndex(entityIndex);

            if (existingIndex != INVALID_INDEX) {
                // Update existing
                updateAtIndex(existingIndex, componentData);
                updateCount.increment();
            } else {
                // Add new
                ensureCapacity(count.get() + 1);
                int newIndex = count.getAndIncrement();

                setEntityMapping(entityIndex, newIndex);
                setIndexMapping(newIndex, entityIndex);
                updateAtIndex(newIndex, componentData);

                addCount.increment();
            }

            if (trackChanges) {
                markChanged(entityIndex);
            }
            markDirty(entityIndex);

        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Add component from MemorySegment (zero-copy for off-heap).
     */
    public void add(int entityIndex, MemorySegment componentData) {
        ensureEntityCapacity(entityIndex + 1);

        long stamp = lock.writeLock();
        try {
            int existingIndex = getEntityIndex(entityIndex);

            if (existingIndex != INVALID_INDEX) {
                // Update existing
                copyToIndex(existingIndex, componentData);
                updateCount.increment();
            } else {
                // Add new
                ensureCapacity(count.get() + 1);
                int newIndex = count.getAndIncrement();

                setEntityMapping(entityIndex, newIndex);
                setIndexMapping(newIndex, entityIndex);
                copyToIndex(newIndex, componentData);

                addCount.increment();
            }

            if (trackChanges) {
                markChanged(entityIndex);
            }
            markDirty(entityIndex);

        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Add component with float array data.
     */
    public void addFloats(int entityIndex, float... values) {
        MemorySegment temp = arena.allocate(ValueLayout.JAVA_FLOAT, values.length);
        MemorySegment.copy(values, 0, temp, ValueLayout.JAVA_FLOAT, 0, values.length);
        add(entityIndex, temp);
    }

    /**
     * Add component with int array data.
     */
    public void addInts(int entityIndex, int... values) {
        MemorySegment temp = arena.allocate(ValueLayout.JAVA_INT, values.length);
        MemorySegment.copy(values, 0, temp, ValueLayout.JAVA_INT, 0, values.length);
        add(entityIndex, temp);
    }

    /**
     * Batch add components.
     */
    public void addBatch(int[] entityIndices, ByteBuffer[] componentDataArray) {
        if (entityIndices.length != componentDataArray.length) {
            throw new IllegalArgumentException("Array lengths must match");
        }

        long stamp = lock.writeLock();
        try {
            for (int i = 0; i < entityIndices.length; i++) {
                add(entityIndices[i], componentDataArray[i]);
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // ========================================================================
    // REMOVE OPERATIONS
    // ========================================================================

    /**
     * Remove component from entity.
     */
    public boolean remove(int entityIndex) {
        if (entityIndex >= entityCapacity) return false;

        long stamp = lock.writeLock();
        try {
            int removedIndex = getEntityIndex(entityIndex);
            if (removedIndex == INVALID_INDEX) return false;

            int lastIndex = count.get() - 1;

            if (removedIndex != lastIndex) {
                // Swap with last element
                int lastEntityIndex = getIndexEntity(lastIndex);

                // Copy last element data to removed position
                MemorySegment.copy(
                    data, (long) lastIndex * componentSize,
                    data, (long) removedIndex * componentSize,
                    componentSize
                );

                // Update mappings
                setEntityMapping(lastEntityIndex, removedIndex);
                setIndexMapping(removedIndex, lastEntityIndex);

                // Copy change version if tracking
                if (trackChanges) {
                    long lastVersion = changeVersions.getAtIndex(ValueLayout.JAVA_LONG, lastIndex);
                    changeVersions.setAtIndex(ValueLayout.JAVA_LONG, removedIndex, lastVersion);
                }
            }

            // Clear removed entity's mapping
            setEntityMapping(entityIndex, INVALID_INDEX);
            setIndexMapping(lastIndex, INVALID_INDEX);

            count.decrementAndGet();
            removeCount.increment();
            gpuDirty.set(true);

            return true;

        } finally {
            lock.unlockWrite(stamp);
        }
    }

    /**
     * Batch remove components.
     */
    public int removeBatch(int[] entityIndices) {
        int removed = 0;
        long stamp = lock.writeLock();
        try {
            for (int entityIndex : entityIndices) {
                if (remove(entityIndex)) removed++;
            }
        } finally {
            lock.unlockWrite(stamp);
        }
        return removed;
    }

    /**
     * Clear all components.
     */
    public void clear() {
        long stamp = lock.writeLock();
        try {
            fillSegment(entityToIndex, INVALID_INDEX);
            fillSegment(indexToEntity, INVALID_INDEX);
            count.set(0);
            version.incrementAndGet();
            gpuDirty.set(true);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // ========================================================================
    // QUERY OPERATIONS
    // ========================================================================

    /**
     * Check if entity has this component.
     */
    public boolean has(int entityIndex) {
        if (entityIndex < 0 || entityIndex >= entityCapacity) return false;
        
        long stamp = lock.tryOptimisticRead();
        int index = getEntityIndex(entityIndex);
        
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                index = getEntityIndex(entityIndex);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        return index != INVALID_INDEX;
    }

    /**
     * Get component data as ByteBuffer (read-only view).
     */
    public ByteBuffer get(int entityIndex) {
        if (!has(entityIndex)) return null;

        long stamp = lock.readLock();
        try {
            int index = getEntityIndex(entityIndex);
            if (index == INVALID_INDEX) return null;

            return data.asSlice((long) index * componentSize, componentSize)
                .asByteBuffer()
                .order(ByteOrder.nativeOrder())
                .asReadOnlyBuffer();

        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Get component data as MemorySegment (zero-copy).
     */
    public MemorySegment getSegment(int entityIndex) {
        if (!has(entityIndex)) return null;

        long stamp = lock.readLock();
        try {
            int index = getEntityIndex(entityIndex);
            if (index == INVALID_INDEX) return null;

            return data.asSlice((long) index * componentSize, componentSize);

        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Get mutable component data for in-place modification.
     */
    public MemorySegment getMutable(int entityIndex) {
        if (!has(entityIndex)) return null;

        long stamp = lock.readLock();
        try {
            int index = getEntityIndex(entityIndex);
            if (index == INVALID_INDEX) return null;

            if (trackChanges) {
                markChanged(entityIndex);
            }
            markDirty(entityIndex);

            return data.asSlice((long) index * componentSize, componentSize);

        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Get raw data segment for batch processing.
     */
    public MemorySegment getRawData() {
        return data.asSlice(0, (long) count.get() * componentSize);
    }

    /**
     * Get mutable data segment for batch updates.
     */
    public MemorySegment getMutableData() {
        gpuDirty.set(true);
        return data.asSlice(0, (long) count.get() * componentSize);
    }

    /**
     * Get raw data as ByteBuffer.
     */
    public ByteBuffer getRawDataAsBuffer() {
        return getRawData()
            .asByteBuffer()
            .order(ByteOrder.nativeOrder())
            .asReadOnlyBuffer();
    }

    // ========================================================================
    // TYPED ACCESSORS
    // ========================================================================

    /**
     * Get float value at offset.
     */
    public float getFloat(int entityIndex, int offset) {
        MemorySegment segment = getSegment(entityIndex);
        if (segment == null) return 0f;
        return segment.get(ValueLayout.JAVA_FLOAT, offset);
    }

    /**
     * Set float value at offset.
     */
    public void setFloat(int entityIndex, int offset, float value) {
        MemorySegment segment = getMutable(entityIndex);
        if (segment != null) {
            segment.set(ValueLayout.JAVA_FLOAT, offset, value);
        }
    }

    /**
     * Get int value at offset.
     */
    public int getInt(int entityIndex, int offset) {
        MemorySegment segment = getSegment(entityIndex);
        if (segment == null) return 0;
        return segment.get(ValueLayout.JAVA_INT, offset);
    }

    /**
     * Set int value at offset.
     */
    public void setInt(int entityIndex, int offset, int value) {
        MemorySegment segment = getMutable(entityIndex);
        if (segment != null) {
            segment.set(ValueLayout.JAVA_INT, offset, value);
        }
    }

    /**
     * Get long value at offset.
     */
    public long getLong(int entityIndex, int offset) {
        MemorySegment segment = getSegment(entityIndex);
        if (segment == null) return 0L;
        return segment.get(ValueLayout.JAVA_LONG, offset);
    }

    /**
     * Set long value at offset.
     */
    public void setLong(int entityIndex, int offset, long value) {
        MemorySegment segment = getMutable(entityIndex);
        if (segment != null) {
            segment.set(ValueLayout.JAVA_LONG, offset, value);
        }
    }

    /**
     * Get double value at offset.
     */
    public double getDouble(int entityIndex, int offset) {
        MemorySegment segment = getSegment(entityIndex);
        if (segment == null) return 0.0;
        return segment.get(ValueLayout.JAVA_DOUBLE, offset);
    }

    /**
     * Set double value at offset.
     */
    public void setDouble(int entityIndex, int offset, double value) {
        MemorySegment segment = getMutable(entityIndex);
        if (segment != null) {
            segment.set(ValueLayout.JAVA_DOUBLE, offset, value);
        }
    }

    // ========================================================================
    // ITERATION
    // ========================================================================

    /**
     * Iterate over all components with their entity indices.
     */
    public void forEach(BiConsumer<Integer, MemorySegment> consumer) {
        long stamp = lock.readLock();
        try {
            int currentCount = count.get();
            for (int i = 0; i < currentCount; i++) {
                int entityIndex = getIndexEntity(i);
                MemorySegment componentData = data.asSlice((long) i * componentSize, componentSize);
                consumer.accept(entityIndex, componentData);
            }
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Iterate with ByteBuffer (legacy compatibility).
     */
    public void forEachBuffer(BiConsumer<Integer, ByteBuffer> consumer) {
        forEach((entityIndex, segment) -> {
            ByteBuffer buffer = segment.asByteBuffer().order(ByteOrder.nativeOrder());
            consumer.accept(entityIndex, buffer);
        });
    }

    /**
     * Parallel iteration.
     */
    public void forEachParallel(BiConsumer<Integer, MemorySegment> consumer) {
        long stamp = lock.readLock();
        try {
            int currentCount = count.get();
            IntStream.range(0, currentCount).parallel().forEach(i -> {
                int entityIndex = getIndexEntity(i);
                MemorySegment componentData = data.asSlice((long) i * componentSize, componentSize);
                consumer.accept(entityIndex, componentData);
            });
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Stream entity indices.
     */
    public IntStream entityIndexStream() {
        int currentCount = count.get();
        return IntStream.range(0, currentCount)
            .map(this::getIndexEntity);
    }

    /**
     * Stream component data.
     */
    public Stream<MemorySegment> componentStream() {
        int currentCount = count.get();
        return IntStream.range(0, currentCount)
            .mapToObj(i -> data.asSlice((long) i * componentSize, componentSize));
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
     * Check if component has changed since version.
     */
    public boolean hasChangedSince(int entityIndex, long sinceVersion) {
        if (!trackChanges || !has(entityIndex)) return false;

        int index = getEntityIndex(entityIndex);
        if (index == INVALID_INDEX) return false;

        long componentVersion = changeVersions.getAtIndex(ValueLayout.JAVA_LONG, index);
        return componentVersion > sinceVersion;
    }

    /**
     * Get entities changed since version.
     */
    public int[] getChangedEntities(long sinceVersion) {
        if (!trackChanges) return new int[0];

        long stamp = lock.readLock();
        try {
            int currentCount = count.get();
            IntStream.Builder builder = IntStream.builder();

            for (int i = 0; i < currentCount; i++) {
                long componentVersion = changeVersions.getAtIndex(ValueLayout.JAVA_LONG, i);
                if (componentVersion > sinceVersion) {
                    builder.add(getIndexEntity(i));
                }
            }

            return builder.build().toArray();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    private void markChanged(int entityIndex) {
        if (!trackChanges) return;

        int index = getEntityIndex(entityIndex);
        if (index != INVALID_INDEX) {
            long newVersion = version.incrementAndGet();
            changeVersions.setAtIndex(ValueLayout.JAVA_LONG, index, newVersion);
        }
    }

    // ========================================================================
    // GPU INTEGRATION
    // ========================================================================

    /**
     * Check if GPU buffer needs update.
     */
    public boolean isGpuDirty() {
        return gpuDirty.get();
    }

    /**
     * Clear GPU dirty flag.
     */
    public void clearGpuDirty() {
        gpuDirty.set(false);
        dirtyRangeStart = 0;
        dirtyRangeEnd = 0;
    }

    /**
     * Get dirty range for partial updates.
     */
    public int[] getDirtyRange() {
        return new int[]{dirtyRangeStart, dirtyRangeEnd};
    }

    /**
     * Set GPU buffer for direct mapping.
     */
    public void setGpuBuffer(long buffer, MemorySegment mapped) {
        this.gpuBuffer = buffer;
        this.gpuMappedSegment = mapped;
    }

    /**
     * Get GPU buffer handle.
     */
    public long getGpuBuffer() {
        return gpuBuffer;
    }

    /**
     * Sync data to GPU buffer (full sync).
     */
    public void syncToGpu() {
        if (!gpuDirty.get() || gpuMappedSegment == null) return;

        long stamp = lock.readLock();
        try {
            long dataSize = (long) count.get() * componentSize;
            MemorySegment.copy(data, 0, gpuMappedSegment, 0, dataSize);
            clearGpuDirty();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Sync dirty range only (partial sync).
     */
    public void syncDirtyRangeToGpu() {
        if (!gpuDirty.get() || gpuMappedSegment == null) return;
        if (dirtyRangeStart >= dirtyRangeEnd) {
            syncToGpu();
            return;
        }

        long stamp = lock.readLock();
        try {
            long startOffset = (long) dirtyRangeStart * componentSize;
            long endOffset = Math.min((long) dirtyRangeEnd * componentSize, 
                (long) count.get() * componentSize);
            long size = endOffset - startOffset;

            if (size > 0) {
                MemorySegment.copy(data, startOffset, gpuMappedSegment, startOffset, size);
            }
            clearGpuDirty();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    private void markDirty(int entityIndex) {
        gpuDirty.set(true);
        int index = getEntityIndex(entityIndex);
        if (index != INVALID_INDEX) {
            dirtyRangeStart = Math.min(dirtyRangeStart, index);
            dirtyRangeEnd = Math.max(dirtyRangeEnd, index + 1);
        }
    }

    // ========================================================================
    // ACCESSORS
    // ========================================================================

    /**
     * Get component count.
     */
    public int getCount() {
        return count.get();
    }

    /**
     * Get capacity.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Get component type.
     */
    public ComponentRegistry.ComponentType getType() {
        return type;
    }

    /**
     * Get component size in bytes.
     */
    public int getComponentSize() {
        return componentSize;
    }

    /**
     * Get total data size in bytes.
     */
    public long getDataSize() {
        return (long) count.get() * componentSize;
    }

    /**
     * Get total allocated size in bytes.
     */
    public long getAllocatedSize() {
        return (long) capacity * componentSize;
    }

    /**
     * Get memory efficiency (used/allocated ratio).
     */
    public float getMemoryEfficiency() {
        return capacity > 0 ? (float) count.get() / capacity : 0f;
    }

    /**
     * Check if empty.
     */
    public boolean isEmpty() {
        return count.get() == 0;
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get array statistics.
     */
    public ArrayStats getStats() {
        return new ArrayStats(
            type.name(),
            count.get(),
            capacity,
            entityCapacity,
            componentSize,
            getDataSize(),
            getAllocatedSize(),
            addCount.sum(),
            removeCount.sum(),
            updateCount.sum(),
            resizeCount.sum(),
            version.get()
        );
    }

    public record ArrayStats(
        String typeName,
        int count,
        int capacity,
        int entityCapacity,
        int componentSize,
        long dataSize,
        long allocatedSize,
        long adds,
        long removes,
        long updates,
        long resizes,
        long version
    ) {
        public float efficiency() {
            return allocatedSize > 0 ? (float) dataSize / allocatedSize : 0f;
        }
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private int getEntityIndex(int entityIndex) {
        if (entityIndex < 0 || entityIndex >= entityCapacity) return INVALID_INDEX;
        return entityToIndex.getAtIndex(ValueLayout.JAVA_INT, entityIndex);
    }

    private void setEntityMapping(int entityIndex, int denseIndex) {
        entityToIndex.setAtIndex(ValueLayout.JAVA_INT, entityIndex, denseIndex);
    }

    private int getIndexEntity(int denseIndex) {
        return indexToEntity.getAtIndex(ValueLayout.JAVA_INT, denseIndex);
    }

    private void setIndexMapping(int denseIndex, int entityIndex) {
        indexToEntity.setAtIndex(ValueLayout.JAVA_INT, denseIndex, entityIndex);
    }

    private void updateAtIndex(int index, ByteBuffer source) {
        source.rewind();
        int size = Math.min(source.remaining(), componentSize);
        MemorySegment.copy(
            MemorySegment.ofBuffer(source), ValueLayout.JAVA_BYTE, 0,
            data, ValueLayout.JAVA_BYTE, (long) index * componentSize,
            size
        );
    }

    private void copyToIndex(int index, MemorySegment source) {
        long size = Math.min(source.byteSize(), componentSize);
        MemorySegment.copy(source, 0, data, (long) index * componentSize, size);
    }

    private static void fillSegment(MemorySegment segment, int value) {
        long count = segment.byteSize() / ValueLayout.JAVA_INT.byteSize();
        for (long i = 0; i < count; i++) {
            segment.setAtIndex(ValueLayout.JAVA_INT, i, value);
        }
    }

    private static void fillSegmentRange(MemorySegment segment, int start, int end, int value) {
        for (int i = start; i < end; i++) {
            segment.setAtIndex(ValueLayout.JAVA_INT, i, value);
        }
    }

    private static long alignUp(long value, int alignment) {
        return (value + alignment - 1) & ~(alignment - 1);
    }

    // ========================================================================
    // AUTOCLOSEABLE
    // ========================================================================

    @Override
    public void close() {
        if (arena.scope().isAlive()) {
            Astralis.LOGGER.debug("[ECS] Closing ComponentArray for {} (count={})",
                type.name(), count.get());
            arena.close();
        }
    }

    // ========================================================================
    // DEBUG
    // ========================================================================

    @Override
    public String toString() {
        return String.format("ComponentArray[%s, count=%d/%d, size=%d]",
            type.name(), count.get(), capacity, componentSize);
    }

    /**
     * Get detailed description.
     */
    public String describe() {
        ArrayStats stats = getStats();
        return String.format("""
            ComponentArray {
              Type: %s
              Count: %d / %d (%.1f%% full)
              Component Size: %d bytes
              Data Size: %d bytes
              Allocated: %d bytes
              Efficiency: %.1f%%
              Operations: +%d -%d ~%d
              Resizes: %d
              Version: %d
              GPU Dirty: %s
            }""",
            stats.typeName(),
            stats.count(), stats.capacity(),
            stats.count() * 100.0 / Math.max(1, stats.capacity()),
            stats.componentSize(),
            stats.dataSize(),
            stats.allocatedSize(),
            stats.efficiency() * 100,
            stats.adds(), stats.removes(), stats.updates(),
            stats.resizes(),
            stats.version(),
            gpuDirty.get()
        );
    }
}
