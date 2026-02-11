package stellar.snow.astralis.engine.gpu.compute;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.*;
import net.minecraft.tileentity.*;
import net.minecraft.world.World;

import stellar.snow.astralis.engine.gpu.compute.CullingTier.*;

import java.lang.foreign.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * CullingManager — centralized culling oracle for every cullable object in the world.
 *
 * <h3>Responsibilities:</h3>
 * <ul>
 *   <li>Calculates and caches {@link CullingTier} per entity with hysteresis</li>
 *   <li>Resolves {@link CullingPolicy} per entity based on tier + category</li>
 *   <li>Resolves {@link HudCullingPolicy} per HUD element based on player context</li>
 *   <li>Maintains frame-level statistics for profiling / debug overlay</li>
 *   <li>Supports bulk processing for high-entity-count worlds</li>
 *   <li>Temporal smoothing prevents tier flickering at boundaries</li>
 * </ul>
 */
public class CullingManager {
    private static final CullingManager INSTANCE = new CullingManager();

    // ========================================================================
    // CACHE CONFIGURATION
    // ========================================================================

    /** Ticks before a cached tier expires and must be recalculated. */
    private static final int CACHE_DURATION = 5;

    /** Maximum cache entries before forced eviction. */
    private static final int MAX_CACHE_SIZE = 16_384;

    /** Hysteresis band: entity must move this many blocks² past a tier boundary
     *  before transitioning, preventing flicker at edges. */
    private static final double HYSTERESIS_BAND_SQ = 4.0 * 4.0; // 4 blocks

    /** How many consecutive ticks an entity must be in a new tier before transition. */
    private static final int TIER_TRANSITION_TICKS = 3;

    // ========================================================================
    // CACHE STATE
    // ========================================================================

    /** Per-entity cached tier + metadata. */
    private final ConcurrentHashMap<UUID, CachedTier> tierCache = new ConcurrentHashMap<>(4096, 0.75f, 4);

    /** Current HUD context — set once per frame by the input/state system. */
    private volatile HudContext currentHudContext = HudContext.EXPLORATION;

    /** Configurable tier thresholds — can be adjusted at runtime for dynamic quality. */
    private final CullingTier.TierThresholds thresholds = new CullingTier.TierThresholds();

    /** Per-frame statistics accumulator. */
    private final CullingTier.CullingStats frameStats = new CullingTier.CullingStats();

    /** Monotonic tick counter for cache freshness. */
    private final AtomicInteger globalTick = new AtomicInteger();

    /** Eviction generation counter — bumped when cache exceeds MAX_CACHE_SIZE. */
    private final AtomicLong evictionGeneration = new AtomicLong();

    // ========================================================================
    // STATISTICS COUNTERS
    // ========================================================================

    private final LongAdder cacheHits       = new LongAdder();
    private final LongAdder cacheMisses     = new LongAdder();
    private final LongAdder evictions       = new LongAdder();
    private final LongAdder tierTransitions = new LongAdder();
    private final LongAdder totalQueries    = new LongAdder();

    // ========================================================================
    // SINGLETON
    // ========================================================================

    public static CullingManager getInstance() {
        return INSTANCE;
    }

    // ========================================================================
    // CACHED TIER RECORD
    // ========================================================================

    /**
     * Enhanced cached tier with hysteresis state and temporal smoothing.
     */
    private record CachedTier(
            CullingTier tier,
            CullingTier pendingTier,
            int tickCalculated,
            int pendingTicks,
            double lastDistSq,
            CullableCategory category
    ) {
        /** Create initial entry. */
        static CachedTier initial(CullingTier tier, int tick, double distSq, CullableCategory cat) {
            return new CachedTier(tier, tier, tick, 0, distSq, cat);
        }

        /** Create updated entry with potential tier transition. */
        CachedTier withNewDistance(CullingTier newTier, int tick, double distSq) {
            if (newTier == tier) {
                // Same tier — reset pending
                return new CachedTier(tier, tier, tick, 0, distSq, category);
            }
            if (newTier == pendingTier) {
                // Same pending tier — increment counter
                int newPending = pendingTicks + 1;
                if (newPending >= TIER_TRANSITION_TICKS) {
                    // Transition confirmed
                    return new CachedTier(newTier, newTier, tick, 0, distSq, category);
                }
                return new CachedTier(tier, newTier, tick, newPending, distSq, category);
            }
            // Different pending tier — restart counter
            return new CachedTier(tier, newTier, tick, 1, distSq, category);
        }

        boolean isExpired(int currentTick) {
            return (currentTick - tickCalculated) >= CACHE_DURATION;
        }
    }

    // ========================================================================
    // CORE API — TIER CALCULATION
    // ========================================================================

    /**
     * Calculate the culling tier for an entity, with caching and hysteresis.
     * This is the primary entry point — same API, massively improved internals.
     */
    public CullingTier calculateTier(Entity entity, World world) {
        totalQueries.increment();
        UUID id = entity.getPersistentID();
        int currentTick = globalTick.get();

        // Fast path: cache hit
        CachedTier cached = tierCache.get(id);
        if (cached != null && !cached.isExpired(currentTick)) {
            cacheHits.increment();
            return cached.tier();
        }

        cacheMisses.increment();

        // Calculate distance to nearest player with FMA precision
        double distSq = calculateNearestPlayerDistSq(entity, world);

        // Determine raw tier
        CullingTier rawTier = thresholds.tierFromDistance((float) Math.sqrt(distSq));

        // Determine entity category
        CullableCategory category = classifyEntity(entity);

        // Apply hysteresis
        CachedTier newCached;
        if (cached != null) {
            // Check hysteresis band — require meaningful distance change
            double tierBoundary = getTierBoundarySq(cached.tier());
            double distFromBoundary = Math.abs(distSq - tierBoundary);

            if (distFromBoundary < HYSTERESIS_BAND_SQ && rawTier != cached.tier()) {
                // Within hysteresis band — keep current tier
                newCached = new CachedTier(
                        cached.tier(), cached.pendingTier(),
                        currentTick, cached.pendingTicks(),
                        distSq, category
                );
            } else {
                // Outside band — apply temporal smoothing
                newCached = cached.withNewDistance(rawTier, currentTick, distSq);
                if (newCached.tier() != cached.tier()) {
                    tierTransitions.increment();
                }
            }
        } else {
            newCached = CachedTier.initial(rawTier, currentTick, distSq, category);
        }

        // Store (with size guard)
        if (tierCache.size() >= MAX_CACHE_SIZE) {
            evictStaleEntries(currentTick);
        }
        tierCache.put(id, newCached);

        return newCached.tier();
    }

    /**
     * Clear all cached tiers. Call on dimension change, teleport, or world unload.
     */
    public void clearCache() {
        tierCache.clear();
        cacheHits.reset();
        cacheMisses.reset();
        evictions.reset();
    }

    // ========================================================================
    // EXTENDED API — POLICY RESOLUTION
    // ========================================================================

    /**
     * Get the full culling policy for an entity (tier + category → policy).
     * Most systems should use this instead of raw tier.
     */
    public CullingPolicy getPolicy(Entity entity, World world) {
        CullingTier tier = calculateTier(entity, world);
        CullableCategory category = classifyEntity(entity);
        return tier.policy(category);
    }

    /**
     * Get culling policy for a specific category at a specific distance.
     * Use for non-entity cullables (particles, sounds, etc.).
     */
    public CullingPolicy getPolicyForDistance(double distSq, CullableCategory category) {
        totalQueries.increment();
        return CullingTier.policyFor(distSq, category);
    }

    /**
     * Get HUD culling policy for a specific element.
     * Uses the current HUD context set by {@link #setHudContext}.
     */
    public HudCullingPolicy getHudPolicy(HudElement element) {
        return CullingTier.hudPolicy(element, currentHudContext);
    }

    /**
     * Get all HUD policies for the current context.
     */
    public Map<HudElement, HudCullingPolicy> getAllHudPolicies() {
        return CullingTier.allHudPolicies(currentHudContext);
    }

    /**
     * Quick check: should this HUD element render right now?
     */
    public boolean isHudVisible(HudElement element) {
        return CullingTier.isHudVisible(element, currentHudContext);
    }

    /**
     * Set the current HUD context. Call once per frame from the player state system.
     */
    public void setHudContext(HudContext context) {
        this.currentHudContext = Objects.requireNonNull(context);
    }

    /**
     * Get the current HUD context.
     */
    public HudContext getHudContext() {
        return currentHudContext;
    }

    // ========================================================================
    // BULK PROCESSING
    // ========================================================================

    /**
     * Calculate tiers for all entities in a collection.
     * Returns a map of entity UUID → tier for batch consumption.
     */
    public Map<UUID, CullingTier> calculateTiersBulk(Collection<Entity> entities, World world) {
        Map<UUID, CullingTier> results = new HashMap<>(entities.size());
        for (Entity entity : entities) {
            results.put(entity.getPersistentID(), calculateTier(entity, world));
        }
        return results;
    }

    /**
     * Calculate policies for all entities in a collection.
     */
    public Map<UUID, CullingPolicy> calculatePoliciesBulk(Collection<Entity> entities, World world) {
        Map<UUID, CullingPolicy> results = new HashMap<>(entities.size());
        for (Entity entity : entities) {
            results.put(entity.getPersistentID(), getPolicy(entity, world));
        }
        return results;
    }

    /**
     * Process entities through a tier-aware consumer.
     * Skips fully-culled entities entirely.
     */
    public void processVisible(Collection<Entity> entities, World world,
                               BiConsumer<Entity, CullingPolicy> consumer) {
        for (Entity entity : entities) {
            CullingPolicy policy = getPolicy(entity, world);
            if (!policy.isFullyCulled()) {
                consumer.accept(entity, policy);
            }
        }
    }

    /**
     * Filter entities to only those that should render at their current distance.
     */
    public List<Entity> filterRenderable(Collection<Entity> entities, World world) {
        List<Entity> renderable = new ArrayList<>(entities.size() / 2);
        for (Entity entity : entities) {
            CullingPolicy policy = getPolicy(entity, world);
            if (policy.shouldRender()) {
                renderable.add(entity);
            }
        }
        return renderable;
    }

    /**
     * Partition entities by tier. Useful for LOD batching in the render pipeline.
     */
    public EnumMap<CullingTier, List<Entity>> partitionByTier(Collection<Entity> entities, World world) {
        EnumMap<CullingTier, List<Entity>> partitions = new EnumMap<>(CullingTier.class);
        for (CullingTier tier : CullingTier.values()) {
            partitions.put(tier, new ArrayList<>());
        }
        for (Entity entity : entities) {
            CullingTier tier = calculateTier(entity, world);
            partitions.get(tier).add(entity);
        }
        return partitions;
    }

    // ========================================================================
    // TICK MANAGEMENT
    // ========================================================================

    /**
     * Advance the internal tick counter. Call once per server/client tick.
     */
    public void tick() {
        globalTick.incrementAndGet();
    }

    /**
     * Full tick: advance counter + periodic cache maintenance.
     * Call this instead of {@link #tick()} if you want automatic eviction.
     */
    public void tickWithMaintenance() {
        int tick = globalTick.incrementAndGet();

        // Evict every 200 ticks (10 seconds)
        if (tick % 200 == 0 && tierCache.size() > MAX_CACHE_SIZE / 2) {
            evictStaleEntries(tick);
        }
    }

    // ========================================================================
    // ENTITY REMOVAL
    // ========================================================================

    /**
     * Remove a specific entity from the cache (call on entity death/unload).
     */
    public void removeEntity(UUID entityId) {
        tierCache.remove(entityId);
    }

    /**
     * Remove a specific entity from the cache.
     */
    public void removeEntity(Entity entity) {
        tierCache.remove(entity.getPersistentID());
    }

    /**
     * Remove all entities not present in the given set (world unload cleanup).
     */
    public void retainOnly(Set<UUID> activeEntityIds) {
        tierCache.keySet().retainAll(activeEntityIds);
    }

    // ========================================================================
    // DYNAMIC QUALITY SCALING
    // ========================================================================

    /**
     * Get the mutable tier thresholds for runtime quality adjustment.
     */
    public CullingTier.TierThresholds getThresholds() {
        return thresholds;
    }

    /**
     * Scale all culling distances by a multiplier.
     * Call with values &lt; 1.0 when GPU-bound, &gt; 1.0 when CPU-bound.
     */
    public void scaleQuality(float multiplier) {
        thresholds.scale(multiplier);
        clearCache(); // Invalidate all cached tiers
    }

    /**
     * Reset quality to defaults.
     */
    public void resetQuality() {
        thresholds.reset();
        clearCache();
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Snapshot and reset per-frame culling statistics.
     */
    public CullingTier.CullingStatsSnapshot snapshotFrameStats() {
        return frameStats.snapshotAndReset();
    }

    /**
     * Get cache statistics.
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
                tierCache.size(),
                MAX_CACHE_SIZE,
                cacheHits.sum(),
                cacheMisses.sum(),
                evictions.sum(),
                tierTransitions.sum(),
                totalQueries.sum()
        );
    }

    public record CacheStats(
            int currentSize,
            int maxSize,
            long hits,
            long misses,
            long evictions,
            long tierTransitions,
            long totalQueries
    ) {
        public double hitRate() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0.0;
        }

        public double fillRatio() {
            return maxSize > 0 ? (double) currentSize / maxSize : 0.0;
        }
    }

    /**
     * Get tier distribution — how many cached entities are at each tier.
     */
    public EnumMap<CullingTier, Integer> getTierDistribution() {
        EnumMap<CullingTier, Integer> dist = new EnumMap<>(CullingTier.class);
        for (CullingTier tier : CullingTier.values()) dist.put(tier, 0);
        for (CachedTier cached : tierCache.values()) {
            dist.merge(cached.tier(), 1, Integer::sum);
        }
        return dist;
    }

    /**
     * Get category distribution — how many cached entities per category.
     */
    public EnumMap<CullableCategory, Integer> getCategoryDistribution() {
        EnumMap<CullableCategory, Integer> dist = new EnumMap<>(CullableCategory.class);
        for (CullableCategory cat : CullableCategory.values()) dist.put(cat, 0);
        for (CachedTier cached : tierCache.values()) {
            dist.merge(cached.category(), 1, Integer::sum);
        }
        return dist;
    }

    // ========================================================================
    // PRIVATE — DISTANCE CALCULATION
    // ========================================================================

    /**
     * Calculate squared distance to the nearest player.
     * Uses FMA for precision and checks ALL players (not just the closest
     * reported by World, which may be inaccurate for multi-player).
     */
    private double calculateNearestPlayerDistSq(Entity entity, World world) {
        double minDistSq = Double.MAX_VALUE;

        @SuppressWarnings("unchecked")
        List<EntityPlayer> players = world.playerEntities;

        if (players.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double ex = entity.posX;
        double ey = entity.posY;
        double ez = entity.posZ;

        for (int i = 0, size = players.size(); i < size; i++) {
            EntityPlayer player = players.get(i);
            if (player.isSpectator()) continue;

            double dx = ex - player.posX;
            double dy = ey - player.posY;
            double dz = ez - player.posZ;

            // FMA chain for precision: dx² + (dy² + dz²)
            double distSq = Math.fma(dx, dx, Math.fma(dy, dy, dz * dz));

            if (distSq < minDistSq) {
                minDistSq = distSq;

                // Early out: if we're in FULL range, no need to check further players
                if (distSq < 32.0 * 32.0) {
                    return distSq;
                }
            }
        }

        return minDistSq;
    }

    // ========================================================================
    // PRIVATE — ENTITY CLASSIFICATION
    // ========================================================================

    /**
     * Classify entity into a CullableCategory based on its class hierarchy.
     */
    private CullableCategory classifyEntity(Entity entity) {
        // Most specific first for correct classification
        if (entity instanceof EntityPlayer)       return CullableCategory.PLAYER;
        if (entity instanceof EntityArrow)        return CullableCategory.PROJECTILE;
        if (entity instanceof EntityThrowable)    return CullableCategory.PROJECTILE;
        if (entity instanceof EntityFireball)     return CullableCategory.PROJECTILE;
        if (entity instanceof EntityItem)         return CullableCategory.ITEM_ENTITY;
        if (entity instanceof EntityXPOrb)        return CullableCategory.PARTICLE;
        if (entity instanceof EntityMinecart)     return CullableCategory.ENTITY;
        if (entity instanceof EntityBoat)         return CullableCategory.ENTITY;
        if (entity instanceof EntityPainting)     return CullableCategory.BLOCK_ENTITY;
        if (entity instanceof EntityItemFrame)    return CullableCategory.BLOCK_ENTITY;
        if (entity instanceof EntityArmorStand)   return CullableCategory.BLOCK_ENTITY;
        if (entity instanceof EntityMob)          return CullableCategory.ENTITY;
        if (entity instanceof EntityAnimal)       return CullableCategory.ENTITY;
        if (entity instanceof EntityAmbientCreature) return CullableCategory.ENTITY;

        // Fallback
        return CullableCategory.ENTITY;
    }

    // ========================================================================
    // PRIVATE — CACHE MANAGEMENT
    // ========================================================================

    /**
     * Evict stale entries from the cache. Uses a two-pass strategy:
     * 1. Remove all expired entries.
     * 2. If still over limit, remove AGGRESSIVE-tier entries first (least important).
     */
    private void evictStaleEntries(int currentTick) {
        long gen = evictionGeneration.incrementAndGet();
        int beforeSize = tierCache.size();

        // Pass 1: remove expired
        tierCache.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTick));

        // Pass 2: if still too large, cull by tier priority (AGGRESSIVE first)
        if (tierCache.size() > MAX_CACHE_SIZE * 3 / 4) {
            CullingTier[] sacrificeOrder = {
                    CullingTier.AGGRESSIVE,
                    CullingTier.MODERATE,
                    CullingTier.MINIMAL
            };

            for (CullingTier sacrifice : sacrificeOrder) {
                if (tierCache.size() <= MAX_CACHE_SIZE / 2) break;
                tierCache.entrySet().removeIf(entry -> entry.getValue().tier() == sacrifice);
            }
        }

        int removed = beforeSize - tierCache.size();
        evictions.add(removed);
    }

    /**
     * Get the squared distance boundary for a tier (for hysteresis calculation).
     */
    private double getTierBoundarySq(CullingTier tier) {
        float maxBlocks = switch (tier) {
            case FULL       -> thresholds.getFullMaxBlocks();
            case MINIMAL    -> thresholds.getMinimalMaxBlocks();
            case MODERATE   -> thresholds.getModerateMaxBlocks();
            case AGGRESSIVE -> Float.MAX_VALUE;
        };
        return (double) maxBlocks * maxBlocks;
    }

    // ========================================================================
    // DEBUG
    // ========================================================================

    /**
     * Full diagnostic dump.
     */
    public String describe() {
        CacheStats stats = getCacheStats();
        EnumMap<CullingTier, Integer> tierDist = getTierDistribution();
        EnumMap<CullableCategory, Integer> catDist = getCategoryDistribution();

        StringBuilder sb = new StringBuilder(2048);
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  CullingManager Diagnostics\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  HUD Context: ").append(currentHudContext).append("\n");
        sb.append("  Global Tick: ").append(globalTick.get()).append("\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  Cache:\n");
        sb.append("    Size:        ").append(stats.currentSize()).append(" / ").append(stats.maxSize()).append("\n");
        sb.append("    Hit Rate:    ").append(String.format("%.1f%%", stats.hitRate() * 100)).append("\n");
        sb.append("    Hits:        ").append(stats.hits()).append("\n");
        sb.append("    Misses:      ").append(stats.misses()).append("\n");
        sb.append("    Evictions:   ").append(stats.evictions()).append("\n");
        sb.append("    Transitions: ").append(stats.tierTransitions()).append("\n");
        sb.append("    Queries:     ").append(stats.totalQueries()).append("\n");
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  Tier Distribution:\n");
        for (var entry : tierDist.entrySet()) {
            sb.append("    ").append(String.format("%-12s", entry.getKey())).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  Category Distribution:\n");
        for (var entry : catDist.entrySet()) {
            if (entry.getValue() > 0) {
                sb.append("    ").append(String.format("%-15s", entry.getKey())).append(": ").append(entry.getValue()).append("\n");
            }
        }
        sb.append("───────────────────────────────────────────────────────────────\n");
        sb.append("  Thresholds (blocks):\n");
        sb.append("    FULL:     ").append(thresholds.getFullMaxBlocks()).append("\n");
        sb.append("    MINIMAL:  ").append(thresholds.getMinimalMaxBlocks()).append("\n");
        sb.append("    MODERATE: ").append(thresholds.getModerateMaxBlocks()).append("\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("CullingManager[cached=%d, tick=%d, hud=%s, hitRate=%.0f%%]",
                tierCache.size(), globalTick.get(), currentHudContext,
                getCacheStats().hitRate() * 100);
    }
}
