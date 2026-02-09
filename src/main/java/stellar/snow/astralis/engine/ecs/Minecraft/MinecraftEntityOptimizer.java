package stellar.snow.astralis.engine.ecs.minecraft;

import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.engine.ecs.core.Entity;

import java.lang.annotation.*;
import java.lang.ref.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.zip.*;

/**
 * MinecraftEntityOptimizer - Entity-specific optimizations for Minecraft's patterns.
 *
 * <h2>Minecraft Entity Performance Challenges</h2>
 * <p>Minecraft has unique entity patterns that generic ECS doesn't handle well:
 * 
 * <pre>
 * Challenge 1: Entity Type Pooling
 * - Hundreds of arrows, items, XP orbs spawn/despawn every second
 * - Naive allocation causes GC pressure (30+ MB/sec)
 * - Need type-specific object pools with generational recycling
 * 
 * Challenge 2: NBT Data Bloat
 * - Entities store arbitrary NBT data (inventory, custom names, potion effects)
 * - Shulker box with items: 10+ KB per entity
 * - Need delta compression for network sync, sparse storage for disk
 * 
 * Challenge 3: Network Delta Encoding
 * - Position updates every tick (20 Hz) for all tracked entities
 * - Vanilla sends full state (16+ bytes per entity per tick)
 * - Need delta encoding: only send changed fields
 * 
 * Challenge 4: Tick Skipping / Lazy Updates
 * - Mobs far from players don't need full AI every tick
 * - Passive mobs can skip pathfinding when stationary
 * - Need adaptive tick scheduling based on entity state
 * 
 * Challenge 5: Entity Clusters
 * - Item drops cluster (64+ items in single block)
 * - Need spatial merging for physics/rendering
 * - Mob farms create entity hotspots
 * </pre>
 *
 * <h2>Optimization Strategies</h2>
 * <ul>
 *   <li><b>Type-Specific Pools:</b> Separate pools for arrows, items, XP orbs</li>
 *   <li><b>NBT Delta Compression:</b> Only store/sync changed NBT tags</li>
 *   <li><b>Network Delta Encoding:</b> Bit-packed field changes, not full state</li>
 *   <li><b>Adaptive Tick Scheduling:</b> Skip ticks based on distance, activity, importance</li>
 *   <li><b>Cluster Batching:</b> Merge nearby items/XP for single physics update</li>
 *   <li><b>Generational Recycling:</b> Reuse entity slots with generation tracking</li>
 * </ul>
 *
 * <h2>Integration Pattern</h2>
 * <pre>
 * // Register entity type for pooling
 * entityOptimizer.registerPool(EntityType.ARROW, 256);
 * entityOptimizer.registerPool(EntityType.ITEM, 1024);
 * 
 * // Create entity from pool
 * Entity arrow = entityOptimizer.acquire(EntityType.ARROW);
 * 
 * // Configure tick skipping
 * entityOptimizer.setTickStrategy(arrow, TickStrategy.adaptive(8, 32));
 * 
 * // Return to pool when done
 * entityOptimizer.release(arrow);
 * </pre>
 *
 * @author Enhanced ECS Framework (Minecraft Edition)
 * @version 1.0.0
 * @since Java 21
 */
public final class MinecraftEntityOptimizer {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    /** Default pool size per entity type */
    private static final int DEFAULT_POOL_SIZE = 128;

    /** Maximum pool size to prevent unbounded growth */
    private static final int MAX_POOL_SIZE = 4096;

    /** NBT compression threshold (compress if larger) */
    private static final int NBT_COMPRESSION_THRESHOLD = 256;

    /** Network delta buffer size */
    private static final int DELTA_BUFFER_SIZE = 64;

    /** Tick skip tiers (distance-based) */
    private static final int[] TICK_SKIP_DISTANCES = {8, 16, 32, 64};
    private static final int[] TICK_SKIP_INTERVALS = {1, 2, 4, 8};

    // ========================================================================
    // ANNOTATIONS
    // ========================================================================

    /**
     * Mark entity type as poolable.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Poolable {
        /** Initial pool size */
        int initialSize() default DEFAULT_POOL_SIZE;
        /** Maximum pool size */
        int maxSize() default MAX_POOL_SIZE;
        /** Whether to pre-allocate pool */
        boolean preAllocate() default false;
    }

    /**
     * Mark component as having NBT data.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface HasNBT {
        /** Whether to compress NBT data */
        boolean compress() default true;
        /** Whether to use delta encoding */
        boolean deltaEncode() default true;
    }

    /**
     * Mark entity type as supporting tick skipping.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface TickSkippable {
        /** Base tick interval */
        int baseInterval() default 1;
        /** Maximum skip interval */
        int maxInterval() default 8;
        /** Whether to use adaptive scheduling */
        boolean adaptive() default true;
    }

    /**
     * Mark entity as clusterable (can merge with nearby entities).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Clusterable {
        /** Cluster radius in blocks */
        double radius() default 1.0;
        /** Maximum entities per cluster */
        int maxSize() default 64;
    }

    /**
     * Mark component for network delta encoding.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DeltaEncoded {
        /** Fields to track for changes */
        String[] fields() default {};
        /** Whether to use bit packing */
        boolean bitPacked() default true;
    }

    // ========================================================================
    // ENTITY TYPE ENUM
    // ========================================================================

    /**
     * Minecraft entity types (subset for common pooled types).
     */
    public enum EntityType {
        // Projectiles
        ARROW("arrow", true, true),
        SPECTRAL_ARROW("spectral_arrow", true, true),
        SNOWBALL("snowball", true, true),
        EGG("egg", true, true),
        ENDER_PEARL("ender_pearl", true, true),
        FIREWORK_ROCKET("firework_rocket", true, false),
        
        // Items
        ITEM("item", true, true),
        EXPERIENCE_ORB("experience_orb", true, true),
        
        // Particles/Effects
        AREA_EFFECT_CLOUD("area_effect_cloud", true, false),
        
        // Vehicles
        MINECART("minecart", false, true),
        BOAT("boat", false, true),
        
        // Passive Mobs
        COW("cow", false, true),
        SHEEP("sheep", false, true),
        PIG("pig", false, true),
        CHICKEN("chicken", false, true),
        
        // Hostile Mobs
        ZOMBIE("zombie", false, true),
        SKELETON("skeleton", false, true),
        CREEPER("creeper", false, true),
        SPIDER("spider", false, true),
        
        // Special
        FALLING_BLOCK("falling_block", true, false),
        PRIMED_TNT("tnt", true, false);

        public final String name;
        public final boolean poolable;
        public final boolean tickSkippable;

        EntityType(String name, boolean poolable, boolean tickSkippable) {
            this.name = name;
            this.poolable = poolable;
            this.tickSkippable = tickSkippable;
        }
    }

    // ========================================================================
    // CORE STATE
    // ========================================================================

    /** Entity pools per type */
    private final ConcurrentHashMap<EntityType, EntityPool> pools = new ConcurrentHashMap<>();

    /** NBT data storage (sparse) */
    private final ConcurrentHashMap<Entity, NBTData> nbtData = new ConcurrentHashMap<>();

    /** Network delta trackers */
    private final ConcurrentHashMap<Entity, DeltaTracker> deltaTrackers = new ConcurrentHashMap<>();

    /** Tick strategies per entity */
    private final ConcurrentHashMap<Entity, TickStrategy> tickStrategies = new ConcurrentHashMap<>();

    /** Entity clusters */
    private final ConcurrentHashMap<ClusterKey, EntityCluster> clusters = new ConcurrentHashMap<>();

    /** Entity type mapping */
    private final ConcurrentHashMap<Entity, EntityType> entityTypes = new ConcurrentHashMap<>();

    // Statistics
    private final LongAdder poolAcquisitions = new LongAdder();
    private final LongAdder poolReleases = new LongAdder();
    private final LongAdder nbtCompressions = new LongAdder();
    private final LongAdder nbtDecompressions = new LongAdder();
    private final LongAdder deltaSyncs = new LongAdder();
    private final LongAdder ticksSkipped = new LongAdder();
    private final LongAdder clustersCreated = new LongAdder();

    // ========================================================================
    // ENTITY POOL
    // ========================================================================

    /**
     * Object pool for entity recycling.
     */
    private static final class EntityPool {
        final EntityType type;
        final int maxSize;
        final Deque<Entity> available = new ConcurrentLinkedDeque<>();
        final AtomicInteger nextIndex = new AtomicInteger(1);
        final AtomicInteger activeCount = new AtomicInteger(0);
        final AtomicInteger totalCreated = new AtomicInteger(0);

        EntityPool(EntityType type, int initialSize, int maxSize, boolean preAllocate) {
            this.type = type;
            this.maxSize = maxSize;

            if (preAllocate) {
                for (int i = 0; i < initialSize; i++) {
                    available.offer(createNew());
                }
            }
        }

        Entity acquire() {
            Entity entity = available.poll();
            
            if (entity == null) {
                // Pool exhausted - create new
                entity = createNew();
            }

            activeCount.incrementAndGet();
            return entity;
        }

        void release(Entity entity) {
            // Reset to next generation for safety
            Entity recycled = entity.nextGeneration();
            
            if (available.size() < maxSize) {
                available.offer(recycled);
            }
            // else: discard, pool full
            
            activeCount.decrementAndGet();
        }

        private Entity createNew() {
            int index = nextIndex.getAndIncrement();
            totalCreated.incrementAndGet();
            return Entity.of(index);
        }

        int getActiveCount() {
            return activeCount.get();
        }

        int getAvailableCount() {
            return available.size();
        }

        int getTotalCreated() {
            return totalCreated.get();
        }
    }

    // ========================================================================
    // NBT DATA STORAGE
    // ========================================================================

    /**
     * NBT data with compression and delta tracking.
     */
    private static final class NBTData {
        final Map<String, byte[]> tags = new ConcurrentHashMap<>();
        final AtomicLong version = new AtomicLong(0);
        volatile byte[] compressedSnapshot;
        volatile long snapshotVersion;

        void set(String key, byte[] value) {
            tags.put(key, value);
            version.incrementAndGet();
        }

        byte[] get(String key) {
            return tags.get(key);
        }

        void remove(String key) {
            tags.remove(key);
            version.incrementAndGet();
        }

        Set<String> keys() {
            return tags.keySet();
        }

        /**
         * Get changed tags since version.
         */
        Map<String, byte[]> getDeltaSince(long sinceVersion) {
            if (sinceVersion < snapshotVersion) {
                // Full sync needed
                return new HashMap<>(tags);
            }

            // Return delta (in real impl, track per-tag versions)
            return new HashMap<>(tags);
        }

        /**
         * Compress NBT data.
         */
        byte[] compress() {
            if (compressedSnapshot != null && version.get() == snapshotVersion) {
                return compressedSnapshot;
            }

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DeflaterOutputStream deflater = new DeflaterOutputStream(baos);
                
                // Write tag count
                deflater.write(tags.size());
                
                // Write each tag
                for (Map.Entry<String, byte[]> entry : tags.entrySet()) {
                    byte[] keyBytes = entry.getKey().getBytes();
                    byte[] value = entry.getValue();
                    
                    // Write key length + key
                    deflater.write(keyBytes.length);
                    deflater.write(keyBytes);
                    
                    // Write value length + value
                    ByteBuffer lengthBuf = ByteBuffer.allocate(4);
                    lengthBuf.putInt(value.length);
                    deflater.write(lengthBuf.array());
                    deflater.write(value);
                }
                
                deflater.close();
                
                compressedSnapshot = baos.toByteArray();
                snapshotVersion = version.get();
                
                return compressedSnapshot;
                
            } catch (Exception e) {
                Astralis.LOGGER.error("[EntityOptimizer] NBT compression failed", e);
                return new byte[0];
            }
        }

        int getUncompressedSize() {
            return tags.values().stream().mapToInt(arr -> arr.length).sum();
        }

        int getCompressedSize() {
            return compressedSnapshot != null ? compressedSnapshot.length : 0;
        }
    }

    // ========================================================================
    // NETWORK DELTA TRACKING
    // ========================================================================

    /**
     * Delta tracker for network synchronization.
     */
    private static final class DeltaTracker {
        final Map<String, FieldSnapshot> snapshots = new ConcurrentHashMap<>();
        final AtomicLong version = new AtomicLong(0);
        final AtomicInteger dirtyFieldMask = new AtomicInteger(0);

        void trackField(String name, Object value, int bitIndex) {
            FieldSnapshot snapshot = new FieldSnapshot(value, version.get());
            FieldSnapshot old = snapshots.put(name, snapshot);
            
            if (old == null || !Objects.equals(old.value, value)) {
                // Field changed - mark as dirty
                dirtyFieldMask.updateAndGet(mask -> mask | (1 << bitIndex));
                version.incrementAndGet();
            }
        }

        /**
         * Get delta packet (bit mask + changed values).
         */
        DeltaPacket getDelta() {
            int dirtyMask = dirtyFieldMask.getAndSet(0);
            
            if (dirtyMask == 0) {
                return DeltaPacket.EMPTY;
            }

            Map<String, Object> changes = new HashMap<>();
            int bitIndex = 0;
            
            for (Map.Entry<String, FieldSnapshot> entry : snapshots.entrySet()) {
                if ((dirtyMask & (1 << bitIndex)) != 0) {
                    changes.put(entry.getKey(), entry.getValue().value);
                }
                bitIndex++;
            }

            return new DeltaPacket(dirtyMask, changes);
        }

        record FieldSnapshot(Object value, long version) {}
    }

    /**
     * Network delta packet.
     */
    public record DeltaPacket(int dirtyMask, Map<String, Object> changes) {
        static final DeltaPacket EMPTY = new DeltaPacket(0, Collections.emptyMap());
        
        public boolean isEmpty() {
            return dirtyMask == 0;
        }

        public int getSize() {
            // Rough estimate: 4 bytes (mask) + 8 bytes per field
            return 4 + (changes.size() * 8);
        }
    }

    // ========================================================================
    // TICK SCHEDULING
    // ========================================================================

    /**
     * Adaptive tick scheduling strategy.
     */
    public static final class TickStrategy {
        final int baseInterval;
        final int maxInterval;
        final boolean adaptive;
        final AtomicInteger currentInterval = new AtomicInteger(1);
        final AtomicLong lastTickTime = new AtomicLong(0);
        final AtomicInteger stationaryTicks = new AtomicInteger(0);

        private TickStrategy(int baseInterval, int maxInterval, boolean adaptive) {
            this.baseInterval = baseInterval;
            this.maxInterval = maxInterval;
            this.adaptive = adaptive;
            this.currentInterval.set(baseInterval);
        }

        /**
         * Create fixed interval strategy.
         */
        public static TickStrategy fixed(int interval) {
            return new TickStrategy(interval, interval, false);
        }

        /**
         * Create adaptive strategy.
         */
        public static TickStrategy adaptive(int baseInterval, int maxInterval) {
            return new TickStrategy(baseInterval, maxInterval, true);
        }

        /**
         * Check if entity should tick this frame.
         */
        public boolean shouldTick(long currentTick) {
            int interval = currentInterval.get();
            return (currentTick % interval) == 0;
        }

        /**
         * Notify entity is stationary (increase interval).
         */
        public void onStationary() {
            if (!adaptive) return;

            int ticks = stationaryTicks.incrementAndGet();
            
            // Gradually increase interval when stationary
            if (ticks > 20 && currentInterval.get() < maxInterval) {
                currentInterval.updateAndGet(i -> Math.min(i * 2, maxInterval));
            }
        }

        /**
         * Notify entity is active (reset to base interval).
         */
        public void onActive() {
            if (!adaptive) return;

            stationaryTicks.set(0);
            currentInterval.set(baseInterval);
        }

        /**
         * Adjust interval based on distance.
         */
        public void adjustForDistance(double distance) {
            if (!adaptive) return;

            for (int i = 0; i < TICK_SKIP_DISTANCES.length; i++) {
                if (distance < TICK_SKIP_DISTANCES[i]) {
                    currentInterval.set(Math.max(baseInterval, TICK_SKIP_INTERVALS[i]));
                    return;
                }
            }

            // Very far - maximum interval
            currentInterval.set(maxInterval);
        }

        public int getCurrentInterval() {
            return currentInterval.get();
        }
    }

    // ========================================================================
    // ENTITY CLUSTERING
    // ========================================================================

    /**
     * Cluster key for spatial grouping.
     */
    private record ClusterKey(int blockX, int blockY, int blockZ, EntityType type) {
        static ClusterKey of(double x, double y, double z, EntityType type) {
            return new ClusterKey(
                (int) Math.floor(x),
                (int) Math.floor(y),
                (int) Math.floor(z),
                type
            );
        }
    }

    /**
     * Entity cluster for batched processing.
     */
    private static final class EntityCluster {
        final ClusterKey key;
        final Set<Entity> entities = ConcurrentHashMap.newKeySet();
        final AtomicInteger count = new AtomicInteger(0);
        volatile long lastUpdate;

        EntityCluster(ClusterKey key) {
            this.key = key;
            this.lastUpdate = java.lang.System.nanoTime();
        }

        void add(Entity entity) {
            if (entities.add(entity)) {
                count.incrementAndGet();
                lastUpdate = java.lang.System.nanoTime();
            }
        }

        void remove(Entity entity) {
            if (entities.remove(entity)) {
                count.decrementAndGet();
            }
        }

        Set<Entity> getEntities() {
            return Collections.unmodifiableSet(entities);
        }

        int getCount() {
            return count.get();
        }

        boolean isEmpty() {
            return count.get() == 0;
        }
    }

    // ========================================================================
    // POOL MANAGEMENT
    // ========================================================================

    /**
     * Register entity pool.
     */
    public void registerPool(EntityType type, int initialSize, int maxSize, boolean preAllocate) {
        if (!type.poolable) {
            Astralis.LOGGER.warn("[EntityOptimizer] Type {} is not poolable", type.name);
            return;
        }

        EntityPool pool = new EntityPool(type, initialSize, maxSize, preAllocate);
        pools.put(type, pool);

        Astralis.LOGGER.info("[EntityOptimizer] Registered pool for {} (initial: {}, max: {})",
            type.name, initialSize, maxSize);
    }

    /**
     * Register entity pool with defaults.
     */
    public void registerPool(EntityType type) {
        registerPool(type, DEFAULT_POOL_SIZE, MAX_POOL_SIZE, false);
    }

    /**
     * Acquire entity from pool.
     */
    public Entity acquire(EntityType type) {
        EntityPool pool = pools.get(type);
        
        if (pool == null) {
            Astralis.LOGGER.warn("[EntityOptimizer] No pool for type {}, creating entity directly", type.name);
            return Entity.of(1);
        }

        Entity entity = pool.acquire();
        entityTypes.put(entity, type);
        poolAcquisitions.increment();

        return entity;
    }

    /**
     * Release entity back to pool.
     */
    public void release(Entity entity) {
        EntityType type = entityTypes.remove(entity);
        
        if (type == null) {
            Astralis.LOGGER.warn("[EntityOptimizer] Cannot release entity {}: type unknown", entity);
            return;
        }

        EntityPool pool = pools.get(type);
        if (pool != null) {
            pool.release(entity);
            poolReleases.increment();
        }

        // Clean up associated data
        nbtData.remove(entity);
        deltaTrackers.remove(entity);
        tickStrategies.remove(entity);
    }

    // ========================================================================
    // NBT MANAGEMENT
    // ========================================================================

    /**
     * Set NBT tag.
     */
    public void setNBT(Entity entity, String key, byte[] value) {
        NBTData data = nbtData.computeIfAbsent(entity, e -> new NBTData());
        data.set(key, value);
    }

    /**
     * Get NBT tag.
     */
    public byte[] getNBT(Entity entity, String key) {
        NBTData data = nbtData.get(entity);
        return data != null ? data.get(key) : null;
    }

    /**
     * Compress NBT data.
     */
    public byte[] compressNBT(Entity entity) {
        NBTData data = nbtData.get(entity);
        
        if (data == null) {
            return new byte[0];
        }

        if (data.getUncompressedSize() < NBT_COMPRESSION_THRESHOLD) {
            // Too small to benefit from compression
            return new byte[0];
        }

        nbtCompressions.increment();
        return data.compress();
    }

    /**
     * Get NBT delta since version.
     */
    public Map<String, byte[]> getNBTDelta(Entity entity, long sinceVersion) {
        NBTData data = nbtData.get(entity);
        return data != null ? data.getDeltaSince(sinceVersion) : Collections.emptyMap();
    }

    // ========================================================================
    // NETWORK DELTA TRACKING
    // ========================================================================

    /**
     * Track field change for delta encoding.
     */
    public void trackChange(Entity entity, String fieldName, Object value, int bitIndex) {
        DeltaTracker tracker = deltaTrackers.computeIfAbsent(entity, e -> new DeltaTracker());
        tracker.trackField(fieldName, value, bitIndex);
    }

    /**
     * Get network delta packet.
     */
    public DeltaPacket getDelta(Entity entity) {
        DeltaTracker tracker = deltaTrackers.get(entity);
        
        if (tracker == null) {
            return DeltaPacket.EMPTY;
        }

        DeltaPacket packet = tracker.getDelta();
        
        if (!packet.isEmpty()) {
            deltaSyncs.increment();
        }

        return packet;
    }

    // ========================================================================
    // TICK SCHEDULING
    // ========================================================================

    /**
     * Set tick strategy for entity.
     */
    public void setTickStrategy(Entity entity, TickStrategy strategy) {
        tickStrategies.put(entity, strategy);
    }

    /**
     * Check if entity should tick.
     */
    public boolean shouldTick(Entity entity, long currentTick) {
        EntityType type = entityTypes.get(entity);
        
        if (type == null || !type.tickSkippable) {
            return true;  // Always tick if not registered for skipping
        }

        TickStrategy strategy = tickStrategies.get(entity);
        
        if (strategy == null) {
            return true;  // No strategy - tick every frame
        }

        boolean shouldTick = strategy.shouldTick(currentTick);
        
        if (!shouldTick) {
            ticksSkipped.increment();
        }

        return shouldTick;
    }

    /**
     * Notify entity is stationary.
     */
    public void markStationary(Entity entity) {
        TickStrategy strategy = tickStrategies.get(entity);
        if (strategy != null) {
            strategy.onStationary();
        }
    }

    /**
     * Notify entity is active.
     */
    public void markActive(Entity entity) {
        TickStrategy strategy = tickStrategies.get(entity);
        if (strategy != null) {
            strategy.onActive();
        }
    }

    /**
     * Adjust tick rate based on distance.
     */
    public void adjustTickRateForDistance(Entity entity, double distance) {
        TickStrategy strategy = tickStrategies.get(entity);
        if (strategy != null) {
            strategy.adjustForDistance(distance);
        }
    }

    // ========================================================================
    // CLUSTERING
    // ========================================================================

    /**
     * Add entity to cluster.
     */
    public void addToCluster(Entity entity, double x, double y, double z, EntityType type) {
        ClusterKey key = ClusterKey.of(x, y, z, type);
        
        EntityCluster cluster = clusters.computeIfAbsent(key, k -> {
            clustersCreated.increment();
            return new EntityCluster(k);
        });

        cluster.add(entity);
    }

    /**
     * Remove entity from cluster.
     */
    public void removeFromCluster(Entity entity, double x, double y, double z, EntityType type) {
        ClusterKey key = ClusterKey.of(x, y, z, type);
        
        EntityCluster cluster = clusters.get(key);
        if (cluster != null) {
            cluster.remove(entity);
            
            if (cluster.isEmpty()) {
                clusters.remove(key);
            }
        }
    }

    /**
     * Get cluster at position.
     */
    public Optional<EntityCluster> getCluster(double x, double y, double z, EntityType type) {
        ClusterKey key = ClusterKey.of(x, y, z, type);
        return Optional.ofNullable(clusters.get(key));
    }

    /**
     * Get all entities in clusters near position.
     */
    public Set<Entity> getClusteredEntities(double x, double y, double z, EntityType type, int radius) {
        Set<Entity> result = new HashSet<>();
        
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    ClusterKey key = new ClusterKey(blockX + dx, blockY + dy, blockZ + dz, type);
                    EntityCluster cluster = clusters.get(key);
                    
                    if (cluster != null) {
                        result.addAll(cluster.getEntities());
                    }
                }
            }
        }

        return result;
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get optimizer statistics.
     */
    public EntityOptimizerStats getStats() {
        int totalActive = pools.values().stream()
            .mapToInt(EntityPool::getActiveCount)
            .sum();

        int totalAvailable = pools.values().stream()
            .mapToInt(EntityPool::getAvailableCount)
            .sum();

        int totalNBTEntities = nbtData.size();
        long totalNBTSize = nbtData.values().stream()
            .mapToLong(NBTData::getUncompressedSize)
            .sum();

        long totalNBTCompressed = nbtData.values().stream()
            .mapToLong(NBTData::getCompressedSize)
            .sum();

        return new EntityOptimizerStats(
            pools.size(),
            totalActive,
            totalAvailable,
            poolAcquisitions.sum(),
            poolReleases.sum(),
            totalNBTEntities,
            totalNBTSize,
            totalNBTCompressed,
            nbtCompressions.sum(),
            deltaSyncs.sum(),
            ticksSkipped.sum(),
            clusters.size(),
            clustersCreated.sum()
        );
    }

    public record EntityOptimizerStats(
        int registeredPools,
        int activeEntities,
        int availableEntities,
        long poolAcquisitions,
        long poolReleases,
        int entitiesWithNBT,
        long totalNBTSize,
        long totalNBTCompressed,
        long nbtCompressions,
        long deltaSyncs,
        long ticksSkipped,
        int activeClusters,
        long clustersCreated
    ) {
        public double poolUtilization() {
            int total = activeEntities + availableEntities;
            return total > 0 ? (double) activeEntities / total : 0.0;
        }

        public double nbtCompressionRatio() {
            return totalNBTSize > 0 ? (double) totalNBTCompressed / totalNBTSize : 0.0;
        }

        public long nbtSavings() {
            return totalNBTSize - totalNBTCompressed;
        }
    }

    // ========================================================================
    // POOL STATISTICS PER TYPE
    // ========================================================================

    /**
     * Get pool statistics for entity type.
     */
    public PoolStats getPoolStats(EntityType type) {
        EntityPool pool = pools.get(type);
        
        if (pool == null) {
            return null;
        }

        return new PoolStats(
            type,
            pool.getActiveCount(),
            pool.getAvailableCount(),
            pool.getTotalCreated(),
            pool.maxSize
        );
    }

    public record PoolStats(
        EntityType type,
        int active,
        int available,
        int totalCreated,
        int maxSize
    ) {
        public int total() {
            return active + available;
        }

        public double utilization() {
            return total() > 0 ? (double) active / total() : 0.0;
        }
    }

    // ========================================================================
    // DEBUG
    // ========================================================================

    /**
     * Describe optimizer state.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  Minecraft Entity Optimizer\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");

        EntityOptimizerStats stats = getStats();
        sb.append("  Registered Pools: ").append(stats.registeredPools()).append("\n");
        sb.append("  Active Entities: ").append(stats.activeEntities()).append("\n");
        sb.append("  Available (Pooled): ").append(stats.availableEntities()).append("\n");
        sb.append("  Pool Utilization: ").append(String.format("%.1f%%", stats.poolUtilization() * 100)).append("\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  NBT Compression:\n");
        sb.append("    Entities with NBT: ").append(stats.entitiesWithNBT()).append("\n");
        sb.append("    Uncompressed Size: ").append(formatBytes(stats.totalNBTSize())).append("\n");
        sb.append("    Compressed Size: ").append(formatBytes(stats.totalNBTCompressed())).append("\n");
        sb.append("    Compression Ratio: ").append(String.format("%.1f%%", stats.nbtCompressionRatio() * 100)).append("\n");
        sb.append("    Savings: ").append(formatBytes(stats.nbtSavings())).append("\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  Performance:\n");
        sb.append("    Delta Syncs: ").append(stats.deltaSyncs()).append("\n");
        sb.append("    Ticks Skipped: ").append(stats.ticksSkipped()).append("\n");
        sb.append("    Active Clusters: ").append(stats.activeClusters()).append("\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  Pool Breakdown:\n");

        for (EntityType type : EntityType.values()) {
            PoolStats poolStats = getPoolStats(type);
            if (poolStats != null) {
                sb.append(String.format("    %-20s: %4d active, %4d available (%.1f%% util)\n",
                    type.name,
                    poolStats.active(),
                    poolStats.available(),
                    poolStats.utilization() * 100
                ));
            }
        }

        sb.append("═══════════════════════════════════════════════════════════════\n");
        return sb.toString();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @Override
    public String toString() {
        return String.format("MinecraftEntityOptimizer[pools=%d, active=%d, clusters=%d]",
            pools.size(),
            pools.values().stream().mapToInt(EntityPool::getActiveCount).sum(),
            clusters.size());
    }
}
