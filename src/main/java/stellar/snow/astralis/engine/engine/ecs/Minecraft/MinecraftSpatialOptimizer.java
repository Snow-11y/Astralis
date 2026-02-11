package stellar.snow.astralis.engine.ecs.Minecraft;

import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.engine.ecs.core.Archetype;
import stellar.snow.astralis.engine.ecs.core.Entity;
import stellar.snow.astralis.engine.ecs.core.System;
import stellar.snow.astralis.engine.ecs.storage.ComponentRegistry;

import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * MinecraftSpatialOptimizer - ECS optimizations tailored for Minecraft's voxel-based world.
 *
 * <h2>Minecraft-Specific Performance Challenges</h2>
 * <p>Vanilla Minecraft and traditional ECS struggle with Minecraft's unique constraints:
 * 
 * <pre>
 * Challenge 1: Chunk-Aligned Spatial Queries
 * - Entities are spatially distributed across 16x16x256 chunks
 * - Naive "get all entities in radius" does full world scan (catastrophic)
 * - Need chunk-aware spatial indexing for O(chunks_in_range) not O(total_entities)
 * 
 * Challenge 2: Block Update Cascades
 * - Redstone, water, sand updates trigger neighbor updates
 * - Sequential updates cause frame drops (400+ blocks/tick in farms)
 * - Need batched block updates with deduplication
 * 
 * Challenge 3: Entity Streaming by View Distance
 * - Client render distance: 2-32 chunks (configurable)
 * - Server simulation distance: different from render distance
 * - Need hierarchical entity activation zones
 * 
 * Challenge 4: Voxel Cache Locality
 * - Block queries scattered across chunk sections (16³ blocks each)
 * - Cache misses dominate lighting, collision, rendering
 * - Need section-aligned component storage
 * </pre>
 *
 * <h2>Optimization Strategies</h2>
 * <ul>
 *   <li><b>Chunk-Aware Archetype Splitting:</b> Store entities per chunk for spatial locality</li>
 *   <li><b>Block Update Batching:</b> Deduplicate and merge cascading updates within tick</li>
 *   <li><b>Hierarchical Activation:</b> Enable/disable entity processing by distance tier</li>
 *   <li><b>Section-Aligned Storage:</b> 16³ block components stored contiguously</li>
 *   <li><b>Chunk Border Caching:</b> Pre-compute neighbor lookups for cross-chunk queries</li>
 *   <li><b>Y-Level Slicing:</b> Separate sky/surface/cave entities for vertical culling</li>
 * </ul>
 *
 * <h2>Integration Pattern</h2>
 * <pre>
 * // System declares Minecraft awareness
 * {@code @ChunkAware}
 * {@code @ActivationDistance(chunks = 8)}
 * public class MobAISystem extends System {
 *     protected void update(ExecutionContext ctx, Archetype archetype) {
 *         ChunkRegion region = spatialOptimizer.getActiveRegion(ctx);
 *         
 *         for (ChunkCoord chunk : region.getLoadedChunks()) {
 *             List&lt;Entity&gt; entities = spatialOptimizer.getEntitiesInChunk(chunk);
 *             // Process entities with guaranteed chunk locality
 *         }
 *     }
 * }
 * </pre>
 *
 * @author Enhanced ECS Framework (Minecraft Edition)
 * @version 1.0.0
 * @since Java 21
 */
public final class MinecraftSpatialOptimizer {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    /** Minecraft chunk size (X/Z) */
    public static final int CHUNK_SIZE = 16;

    /** Minecraft chunk section height */
    public static final int SECTION_HEIGHT = 16;

    /** World height (blocks) - configurable per world */
    private static final int DEFAULT_WORLD_HEIGHT = 384;
    private static final int DEFAULT_MIN_Y = -64;

    /** Chunk section count */
    private static final int SECTIONS_PER_CHUNK = DEFAULT_WORLD_HEIGHT / SECTION_HEIGHT;

    /** Maximum render distance (chunks) */
    private static final int MAX_RENDER_DISTANCE = 32;

    /** Block update batch size before flush */
    private static final int BLOCK_UPDATE_BATCH_SIZE = 512;

    // ========================================================================
    // ANNOTATIONS
    // ========================================================================

    /**
     * Mark system as chunk-aware (entities processed per chunk).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ChunkAware {
        /** Whether to sort chunks by distance before processing */
        boolean sortByDistance() default false;
        /** Whether system needs neighbor chunk data */
        boolean needsNeighbors() default false;
    }

    /**
     * Declare activation distance for entity processing.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ActivationDistance {
        /** Activation radius in chunks */
        int chunks() default 8;
        /** Whether to use squared distance (cheaper) */
        boolean squared() default true;
    }

    /**
     * Mark component as requiring section-aligned storage.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface SectionAligned {
        /** Section size (default 16³ blocks) */
        int size() default SECTION_HEIGHT;
    }

    /**
     * Mark system as processing block updates.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface BlockUpdateProcessor {
        /** Update priority (lower = earlier) */
        int priority() default 0;
        /** Whether updates can be batched */
        boolean batchable() default true;
    }

    /**
     * Mark component as Y-level dependent (for vertical slicing).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface YLevelDependent {
        /** Minimum Y level */
        int minY() default DEFAULT_MIN_Y;
        /** Maximum Y level */
        int maxY() default DEFAULT_MIN_Y + DEFAULT_WORLD_HEIGHT;
    }

    // ========================================================================
    // CORE STATE
    // ========================================================================

    /** Chunk-to-entities spatial index */
    private final ConcurrentHashMap<ChunkCoord, ChunkEntityIndex> chunkIndex = new ConcurrentHashMap<>();

    /** Block update batch queue per chunk */
    private final ConcurrentHashMap<ChunkCoord, BlockUpdateBatch> blockUpdates = new ConcurrentHashMap<>();

    /** Active chunk regions (per player/camera) */
    private final ConcurrentHashMap<UUID, ActiveRegion> activeRegions = new ConcurrentHashMap<>();

    /** Y-level sliced entity lists */
    private final ConcurrentHashMap<YSlice, List<Entity>> ySlices = new ConcurrentHashMap<>();

    /** Chunk border cache (for cross-chunk queries) */
    private final ConcurrentHashMap<ChunkCoord, ChunkBorderCache> borderCaches = new ConcurrentHashMap<>();

    /** Section-aligned component storage */
    private final ConcurrentHashMap<ChunkSection, SectionStorage> sectionStorage = new ConcurrentHashMap<>();

    // Statistics
    private final LongAdder spatialQueries = new LongAdder();
    private final LongAdder chunkLoads = new LongAdder();
    private final LongAdder chunkUnloads = new LongAdder();
    private final LongAdder blockUpdateBatches = new LongAdder();
    private final LongAdder borderCacheHits = new LongAdder();
    private final LongAdder borderCacheMisses = new LongAdder();

    // ========================================================================
    // COORDINATE RECORDS
    // ========================================================================

    /**
     * Chunk coordinate (X, Z).
     */
    public record ChunkCoord(int x, int z) implements Comparable<ChunkCoord> {
        /**
         * Get chunk coordinate from world position.
         */
        public static ChunkCoord fromWorld(double worldX, double worldZ) {
            return new ChunkCoord(
                (int) Math.floor(worldX / CHUNK_SIZE),
                (int) Math.floor(worldZ / CHUNK_SIZE)
            );
        }

        /**
         * Get squared distance to another chunk.
         */
        public int distanceSquared(ChunkCoord other) {
            int dx = x - other.x;
            int dz = z - other.z;
            return dx * dx + dz * dz;
        }

        /**
         * Get Chebyshev distance (max of abs deltas).
         */
        public int chebyshevDistance(ChunkCoord other) {
            return Math.max(Math.abs(x - other.x), Math.abs(z - other.z));
        }

        /**
         * Get neighbor chunk in direction.
         */
        public ChunkCoord neighbor(Direction direction) {
            return new ChunkCoord(x + direction.dx, z + direction.dz);
        }

        /**
         * Get all 8 neighbors.
         */
        public List<ChunkCoord> getNeighbors() {
            List<ChunkCoord> neighbors = new ArrayList<>(8);
            for (Direction dir : Direction.values()) {
                neighbors.add(neighbor(dir));
            }
            return neighbors;
        }

        @Override
        public int compareTo(ChunkCoord other) {
            int cmp = Integer.compare(x, other.x);
            return cmp != 0 ? cmp : Integer.compare(z, other.z);
        }
    }

    /**
     * Chunk section coordinate (X, Y, Z).
     */
    public record ChunkSection(int chunkX, int sectionY, int chunkZ) {
        /**
         * Get section from world position.
         */
        public static ChunkSection fromWorld(double worldX, double worldY, double worldZ) {
            return new ChunkSection(
                (int) Math.floor(worldX / CHUNK_SIZE),
                (int) Math.floor((worldY - DEFAULT_MIN_Y) / SECTION_HEIGHT),
                (int) Math.floor(worldZ / CHUNK_SIZE)
            );
        }

        /**
         * Get chunk coordinate.
         */
        public ChunkCoord getChunk() {
            return new ChunkCoord(chunkX, chunkZ);
        }

        /**
         * Get world Y coordinate.
         */
        public int getWorldY() {
            return sectionY * SECTION_HEIGHT + DEFAULT_MIN_Y;
        }
    }

    /**
     * Block position (X, Y, Z).
     */
    public record BlockPos(int x, int y, int z) implements Comparable<BlockPos> {
        /**
         * Get chunk coordinate.
         */
        public ChunkCoord getChunk() {
            return ChunkCoord.fromWorld(x, z);
        }

        /**
         * Get section coordinate.
         */
        public ChunkSection getSection() {
            return ChunkSection.fromWorld(x, y, z);
        }

        /**
         * Get relative position within chunk section.
         */
        public int getRelativeIndex() {
            int localX = Math.floorMod(x, CHUNK_SIZE);
            int localY = Math.floorMod(y - DEFAULT_MIN_Y, SECTION_HEIGHT);
            int localZ = Math.floorMod(z, CHUNK_SIZE);
            
            return localY * (CHUNK_SIZE * CHUNK_SIZE) + localZ * CHUNK_SIZE + localX;
        }

        @Override
        public int compareTo(BlockPos other) {
            int cmp = Integer.compare(y, other.y);
            if (cmp != 0) return cmp;
            cmp = Integer.compare(z, other.z);
            return cmp != 0 ? cmp : Integer.compare(x, other.x);
        }
    }

    /**
     * Y-level slice for vertical culling.
     */
    public record YSlice(int minY, int maxY) {
        /**
         * Check if Y coordinate is in slice.
         */
        public boolean contains(int y) {
            return y >= minY && y < maxY;
        }

        /**
         * Get slice for world Y.
         */
        public static YSlice forY(int y) {
            int sliceSize = 64;  // 4 sections per slice
            int sliceIndex = (y - DEFAULT_MIN_Y) / sliceSize;
            int minY = DEFAULT_MIN_Y + sliceIndex * sliceSize;
            return new YSlice(minY, minY + sliceSize);
        }
    }

    /**
     * Cardinal directions for chunk neighbors.
     */
    public enum Direction {
        NORTH(0, -1),
        SOUTH(0, 1),
        EAST(1, 0),
        WEST(-1, 0),
        NORTH_EAST(1, -1),
        NORTH_WEST(-1, -1),
        SOUTH_EAST(1, 1),
        SOUTH_WEST(-1, 1);

        public final int dx;
        public final int dz;

        Direction(int dx, int dz) {
            this.dx = dx;
            this.dz = dz;
        }

        public boolean isDiagonal() {
            return dx != 0 && dz != 0;
        }
    }

    // ========================================================================
    // SPATIAL INDEX STRUCTURES
    // ========================================================================

    /**
     * Entity index for a single chunk.
     */
    private static final class ChunkEntityIndex {
        final ChunkCoord coord;
        final ConcurrentHashMap<Entity, EntityPosition> entities = new ConcurrentHashMap<>();
        final AtomicInteger count = new AtomicInteger(0);
        volatile long lastAccessTime;
        volatile boolean dirty = true;

        ChunkEntityIndex(ChunkCoord coord) {
            this.coord = coord;
            this.lastAccessTime = java.lang.System.nanoTime();
        }

        void addEntity(Entity entity, double x, double y, double z) {
            EntityPosition pos = new EntityPosition(x, y, z);
            if (entities.putIfAbsent(entity, pos) == null) {
                count.incrementAndGet();
                dirty = true;
            }
            lastAccessTime = java.lang.System.nanoTime();
        }

        void removeEntity(Entity entity) {
            if (entities.remove(entity) != null) {
                count.decrementAndGet();
                dirty = true;
            }
        }

        void updatePosition(Entity entity, double x, double y, double z) {
            entities.compute(entity, (e, oldPos) -> new EntityPosition(x, y, z));
            dirty = true;
            lastAccessTime = java.lang.System.nanoTime();
        }

        List<Entity> getEntities() {
            return new ArrayList<>(entities.keySet());
        }

        List<Entity> getEntitiesInYRange(int minY, int maxY) {
            return entities.entrySet().stream()
                .filter(entry -> {
                    double y = entry.getValue().y;
                    return y >= minY && y < maxY;
                })
                .map(Map.Entry::getKey)
                .toList();
        }

        int getCount() {
            return count.get();
        }

        boolean isEmpty() {
            return count.get() == 0;
        }
    }

    /**
     * Cached entity position.
     */
    private record EntityPosition(double x, double y, double z) {}

    /**
     * Active region around player/camera.
     */
    public static final class ActiveRegion {
        final UUID ownerId;
        final ChunkCoord center;
        final int renderDistance;
        final int simulationDistance;
        final Set<ChunkCoord> loadedChunks;
        final Set<ChunkCoord> activeChunks;
        volatile long lastUpdate;

        ActiveRegion(UUID ownerId, ChunkCoord center, int renderDistance, int simulationDistance) {
            this.ownerId = ownerId;
            this.center = center;
            this.renderDistance = renderDistance;
            this.simulationDistance = simulationDistance;
            this.loadedChunks = computeLoadedChunks(center, renderDistance);
            this.activeChunks = computeLoadedChunks(center, simulationDistance);
            this.lastUpdate = java.lang.System.nanoTime();
        }

        private Set<ChunkCoord> computeLoadedChunks(ChunkCoord center, int radius) {
            Set<ChunkCoord> chunks = new HashSet<>();
            
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Use Chebyshev distance (square render area)
                    if (Math.abs(dx) <= radius && Math.abs(dz) <= radius) {
                        chunks.add(new ChunkCoord(center.x + dx, center.z + dz));
                    }
                }
            }
            
            return chunks;
        }

        public Set<ChunkCoord> getLoadedChunks() {
            return Collections.unmodifiableSet(loadedChunks);
        }

        public Set<ChunkCoord> getActiveChunks() {
            return Collections.unmodifiableSet(activeChunks);
        }

        public boolean isChunkLoaded(ChunkCoord chunk) {
            return loadedChunks.contains(chunk);
        }

        public boolean isChunkActive(ChunkCoord chunk) {
            return activeChunks.contains(chunk);
        }
    }

    /**
     * Chunk border cache for cross-chunk queries.
     */
    private static final class ChunkBorderCache {
        final ChunkCoord coord;
        final Map<Direction, List<Entity>> borderEntities = new EnumMap<>(Direction.class);
        volatile long lastUpdate;
        volatile boolean dirty = true;

        ChunkBorderCache(ChunkCoord coord) {
            this.coord = coord;
            this.lastUpdate = java.lang.System.nanoTime();
        }

        void cache(Direction direction, List<Entity> entities) {
            borderEntities.put(direction, new ArrayList<>(entities));
            dirty = false;
            lastUpdate = java.lang.System.nanoTime();
        }

        List<Entity> get(Direction direction) {
            return borderEntities.getOrDefault(direction, Collections.emptyList());
        }

        boolean isValid(long maxAgeNanos) {
            return !dirty && (java.lang.System.nanoTime() - lastUpdate) < maxAgeNanos;
        }

        void invalidate() {
            dirty = true;
        }
    }

    /**
     * Section-aligned storage for block components.
     */
    private static final class SectionStorage {
        final ChunkSection section;
        final byte[] data;  // 16³ = 4096 blocks per section
        volatile long version;

        SectionStorage(ChunkSection section) {
            this.section = section;
            this.data = new byte[SECTION_HEIGHT * SECTION_HEIGHT * SECTION_HEIGHT];
            this.version = 0;
        }

        void set(int relativeIndex, byte value) {
            if (relativeIndex >= 0 && relativeIndex < data.length) {
                data[relativeIndex] = value;
                version++;
            }
        }

        byte get(int relativeIndex) {
            return (relativeIndex >= 0 && relativeIndex < data.length) ? data[relativeIndex] : 0;
        }

        long getVersion() {
            return version;
        }
    }

    /**
     * Block update batch for deduplication.
     */
    private static final class BlockUpdateBatch {
        final ChunkCoord chunk;
        final Map<BlockPos, BlockUpdate> updates = new ConcurrentHashMap<>();
        final AtomicInteger priority = new AtomicInteger(Integer.MAX_VALUE);
        volatile long creationTime;

        BlockUpdateBatch(ChunkCoord chunk) {
            this.chunk = chunk;
            this.creationTime = java.lang.System.nanoTime();
        }

        void add(BlockPos pos, BlockUpdate update) {
            updates.merge(pos, update, (existing, newUpdate) -> {
                // Keep highest priority update
                return existing.priority() < newUpdate.priority() ? existing : newUpdate;
            });
            
            // Update batch priority to highest update priority
            priority.updateAndGet(current -> Math.min(current, update.priority()));
        }

        List<BlockUpdate> drain() {
            List<BlockUpdate> result = new ArrayList<>(updates.values());
            updates.clear();
            return result;
        }

        int size() {
            return updates.size();
        }

        boolean isEmpty() {
            return updates.isEmpty();
        }

        boolean shouldFlush() {
            return size() >= BLOCK_UPDATE_BATCH_SIZE ||
                   (java.lang.System.nanoTime() - creationTime) > 50_000_000;  // 50ms
        }
    }

    /**
     * Block update record.
     */
    public record BlockUpdate(BlockPos pos, int updateType, int priority, long timestamp) {
        /**
         * Create block update.
         */
        public static BlockUpdate create(BlockPos pos, int updateType, int priority) {
            return new BlockUpdate(pos, updateType, priority, java.lang.System.nanoTime());
        }
    }

    // ========================================================================
    // ENTITY TRACKING
    // ========================================================================

    /**
     * Add entity to spatial index.
     */
    public void trackEntity(Entity entity, double x, double y, double z) {
        ChunkCoord chunk = ChunkCoord.fromWorld(x, z);
        
        chunkIndex.computeIfAbsent(chunk, ChunkEntityIndex::new)
            .addEntity(entity, x, y, z);

        // Also add to Y-slice
        YSlice slice = YSlice.forY((int) y);
        ySlices.computeIfAbsent(slice, k -> new CopyOnWriteArrayList<>())
            .add(entity);

        Astralis.LOGGER.trace("[MinecraftSpatial] Tracked entity {} at chunk {}", 
            entity.getId(), chunk);
    }

    /**
     * Remove entity from spatial index.
     */
    public void untrackEntity(Entity entity, double x, double z) {
        ChunkCoord chunk = ChunkCoord.fromWorld(x, z);
        
        ChunkEntityIndex index = chunkIndex.get(chunk);
        if (index != null) {
            index.removeEntity(entity);
            
            if (index.isEmpty()) {
                chunkIndex.remove(chunk);
                chunkUnloads.increment();
            }
        }
    }

    /**
     * Update entity position in spatial index.
     */
    public void updateEntityPosition(Entity entity, double oldX, double oldZ, 
                                     double newX, double newY, double newZ) {
        ChunkCoord oldChunk = ChunkCoord.fromWorld(oldX, oldZ);
        ChunkCoord newChunk = ChunkCoord.fromWorld(newX, newZ);

        if (!oldChunk.equals(newChunk)) {
            // Entity crossed chunk boundary
            untrackEntity(entity, oldX, oldZ);
            trackEntity(entity, newX, newY, newZ);
            
            // Invalidate border caches
            invalidateChunkBorder(oldChunk);
            invalidateChunkBorder(newChunk);
        } else {
            // Update within same chunk
            ChunkEntityIndex index = chunkIndex.get(newChunk);
            if (index != null) {
                index.updatePosition(entity, newX, newY, newZ);
            }
        }
    }

    // ========================================================================
    // SPATIAL QUERIES
    // ========================================================================

    /**
     * Get all entities in chunk.
     */
    public List<Entity> getEntitiesInChunk(ChunkCoord chunk) {
        spatialQueries.increment();
        
        ChunkEntityIndex index = chunkIndex.get(chunk);
        return index != null ? index.getEntities() : Collections.emptyList();
    }

    /**
     * Get entities in chunk within Y range.
     */
    public List<Entity> getEntitiesInChunk(ChunkCoord chunk, int minY, int maxY) {
        spatialQueries.increment();
        
        ChunkEntityIndex index = chunkIndex.get(chunk);
        return index != null ? index.getEntitiesInYRange(minY, maxY) : Collections.emptyList();
    }

    /**
     * Get entities in radius around position.
     */
    public List<Entity> getEntitiesInRadius(double centerX, double centerZ, int radiusChunks) {
        spatialQueries.increment();
        
        ChunkCoord center = ChunkCoord.fromWorld(centerX, centerZ);
        List<Entity> result = new ArrayList<>();

        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                ChunkCoord chunk = new ChunkCoord(center.x + dx, center.z + dz);
                result.addAll(getEntitiesInChunk(chunk));
            }
        }

        return result;
    }

    /**
     * Get entities at chunk border (for cross-chunk queries).
     */
    public List<Entity> getEntitiesAtBorder(ChunkCoord chunk, Direction direction) {
        spatialQueries.increment();
        
        ChunkBorderCache cache = borderCaches.get(chunk);
        
        if (cache != null && cache.isValid(1_000_000_000)) {  // 1 second cache
            borderCacheHits.increment();
            return cache.get(direction);
        }

        borderCacheMisses.increment();
        
        // Compute border entities
        ChunkCoord neighbor = chunk.neighbor(direction);
        List<Entity> entities = getEntitiesInChunk(neighbor);
        
        // Cache result
        borderCaches.computeIfAbsent(chunk, ChunkBorderCache::new)
            .cache(direction, entities);

        return entities;
    }

    // ========================================================================
    // ACTIVE REGION MANAGEMENT
    // ========================================================================

    /**
     * Register active region for player/camera.
     */
    public void registerActiveRegion(UUID ownerId, double centerX, double centerZ, 
                                     int renderDistance, int simulationDistance) {
        ChunkCoord center = ChunkCoord.fromWorld(centerX, centerZ);
        
        ActiveRegion region = new ActiveRegion(
            ownerId,
            center,
            Math.min(renderDistance, MAX_RENDER_DISTANCE),
            Math.min(simulationDistance, MAX_RENDER_DISTANCE)
        );

        activeRegions.put(ownerId, region);
        
        // Mark chunks as loaded
        for (ChunkCoord chunk : region.getLoadedChunks()) {
            chunkIndex.computeIfAbsent(chunk, ChunkEntityIndex::new);
            chunkLoads.increment();
        }

        Astralis.LOGGER.debug("[MinecraftSpatial] Registered active region for {} (render: {}, sim: {})",
            ownerId, renderDistance, simulationDistance);
    }

    /**
     * Update active region center.
     */
    public void updateActiveRegion(UUID ownerId, double centerX, double centerZ) {
        ChunkCoord newCenter = ChunkCoord.fromWorld(centerX, centerZ);
        
        activeRegions.computeIfPresent(ownerId, (id, region) -> {
            if (!region.center.equals(newCenter)) {
                // Re-create region with new center
                return new ActiveRegion(
                    region.ownerId,
                    newCenter,
                    region.renderDistance,
                    region.simulationDistance
                );
            }
            return region;
        });
    }

    /**
     * Unregister active region.
     */
    public void unregisterActiveRegion(UUID ownerId) {
        activeRegions.remove(ownerId);
        Astralis.LOGGER.debug("[MinecraftSpatial] Unregistered active region for {}", ownerId);
    }

    /**
     * Get active region for owner.
     */
    public Optional<ActiveRegion> getActiveRegion(UUID ownerId) {
        return Optional.ofNullable(activeRegions.get(ownerId));
    }

    /**
     * Get all loaded chunks across all active regions.
     */
    public Set<ChunkCoord> getAllLoadedChunks() {
        return activeRegions.values().stream()
            .flatMap(region -> region.getLoadedChunks().stream())
            .collect(Collectors.toSet());
    }

    // ========================================================================
    // BLOCK UPDATE BATCHING
    // ========================================================================

    /**
     * Queue block update for batched processing.
     */
    public void queueBlockUpdate(BlockPos pos, int updateType, int priority) {
        ChunkCoord chunk = pos.getChunk();
        
        BlockUpdateBatch batch = blockUpdates.computeIfAbsent(
            chunk,
            BlockUpdateBatch::new
        );

        batch.add(pos, BlockUpdate.create(pos, updateType, priority));
    }

    /**
     * Flush block updates for chunk.
     */
    public List<BlockUpdate> flushBlockUpdates(ChunkCoord chunk) {
        BlockUpdateBatch batch = blockUpdates.remove(chunk);
        
        if (batch != null && !batch.isEmpty()) {
            blockUpdateBatches.increment();
            List<BlockUpdate> updates = batch.drain();
            
            Astralis.LOGGER.trace("[MinecraftSpatial] Flushed {} block updates for chunk {}",
                updates.size(), chunk);
            
            return updates;
        }

        return Collections.emptyList();
    }

    /**
     * Flush all pending block updates.
     */
    public Map<ChunkCoord, List<BlockUpdate>> flushAllBlockUpdates() {
        Map<ChunkCoord, List<BlockUpdate>> result = new HashMap<>();
        
        blockUpdates.forEach((chunk, batch) -> {
            if (!batch.isEmpty()) {
                result.put(chunk, batch.drain());
            }
        });

        blockUpdates.clear();
        return result;
    }

    /**
     * Auto-flush batches that exceed size or age threshold.
     */
    public Map<ChunkCoord, List<BlockUpdate>> autoFlushBlockUpdates() {
        Map<ChunkCoord, List<BlockUpdate>> result = new HashMap<>();
        
        blockUpdates.forEach((chunk, batch) -> {
            if (batch.shouldFlush()) {
                result.put(chunk, batch.drain());
            }
        });

        return result;
    }

    // ========================================================================
    // SECTION-ALIGNED STORAGE
    // ========================================================================

    /**
     * Get or create section storage.
     */
    public SectionStorage getSectionStorage(ChunkSection section) {
        return sectionStorage.computeIfAbsent(section, SectionStorage::new);
    }

    /**
     * Set block data in section.
     */
    public void setBlockData(BlockPos pos, byte data) {
        ChunkSection section = pos.getSection();
        int relativeIndex = pos.getRelativeIndex();
        
        getSectionStorage(section).set(relativeIndex, data);
    }

    /**
     * Get block data from section.
     */
    public byte getBlockData(BlockPos pos) {
        ChunkSection section = pos.getSection();
        int relativeIndex = pos.getRelativeIndex();
        
        SectionStorage storage = sectionStorage.get(section);
        return storage != null ? storage.get(relativeIndex) : 0;
    }

    /**
     * Unload section storage.
     */
    public void unloadSection(ChunkSection section) {
        sectionStorage.remove(section);
    }

    // ========================================================================
    // CACHE INVALIDATION
    // ========================================================================

    /**
     * Invalidate chunk border caches.
     */
    public void invalidateChunkBorder(ChunkCoord chunk) {
        ChunkBorderCache cache = borderCaches.get(chunk);
        if (cache != null) {
            cache.invalidate();
        }

        // Also invalidate neighbors
        for (ChunkCoord neighbor : chunk.getNeighbors()) {
            ChunkBorderCache neighborCache = borderCaches.get(neighbor);
            if (neighborCache != null) {
                neighborCache.invalidate();
            }
        }
    }

    /**
     * Clear all border caches.
     */
    public void clearBorderCaches() {
        borderCaches.clear();
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get optimizer statistics.
     */
    public SpatialStats getStats() {
        int totalEntities = chunkIndex.values().stream()
            .mapToInt(ChunkEntityIndex::getCount)
            .sum();

        int totalBlockUpdates = blockUpdates.values().stream()
            .mapToInt(BlockUpdateBatch::size)
            .sum();

        return new SpatialStats(
            chunkIndex.size(),
            totalEntities,
            activeRegions.size(),
            totalBlockUpdates,
            sectionStorage.size(),
            spatialQueries.sum(),
            borderCacheHits.sum(),
            borderCacheMisses.sum(),
            chunkLoads.sum(),
            chunkUnloads.sum(),
            blockUpdateBatches.sum()
        );
    }

    public record SpatialStats(
        int loadedChunks,
        int totalEntities,
        int activeRegions,
        int pendingBlockUpdates,
        int loadedSections,
        long spatialQueries,
        long borderCacheHits,
        long borderCacheMisses,
        long chunkLoads,
        long chunkUnloads,
        long blockUpdateBatches
    ) {
        public double averageEntitiesPerChunk() {
            return loadedChunks > 0 ? (double) totalEntities / loadedChunks : 0.0;
        }

        public double borderCacheHitRate() {
            long total = borderCacheHits + borderCacheMisses;
            return total > 0 ? (double) borderCacheHits / total : 0.0;
        }
    }

    // ========================================================================
    // DEBUG
    // ========================================================================

    /**
     * Describe spatial optimizer state.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  Minecraft Spatial Optimizer\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");

        SpatialStats stats = getStats();
        sb.append("  Loaded Chunks: ").append(stats.loadedChunks()).append("\n");
        sb.append("  Total Entities: ").append(stats.totalEntities()).append("\n");
        sb.append("  Avg Entities/Chunk: ").append(String.format("%.1f", stats.averageEntitiesPerChunk())).append("\n");
        sb.append("  Active Regions: ").append(stats.activeRegions()).append("\n");
        sb.append("  Pending Block Updates: ").append(stats.pendingBlockUpdates()).append("\n");
        sb.append("  Loaded Sections: ").append(stats.loadedSections()).append("\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  Performance:\n");
        sb.append("    Spatial Queries: ").append(stats.spatialQueries()).append("\n");
        sb.append("    Border Cache Hit Rate: ").append(String.format("%.2f%%", stats.borderCacheHitRate() * 100)).append("\n");
        sb.append("    Chunk Loads: ").append(stats.chunkLoads()).append("\n");
        sb.append("    Chunk Unloads: ").append(stats.chunkUnloads()).append("\n");
        sb.append("    Block Update Batches: ").append(stats.blockUpdateBatches()).append("\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("MinecraftSpatialOptimizer[chunks=%d, entities=%d, regions=%d]",
            chunkIndex.size(),
            chunkIndex.values().stream().mapToInt(ChunkEntityIndex::getCount).sum(),
            activeRegions.size());
    }
}
