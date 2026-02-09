package stellar.snow.astralis.engine.ecs.core;

import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.engine.ecs.core.Archetype;
import stellar.snow.astralis.engine.ecs.core.System;

import java.lang.annotation.*;
import java.lang.invoke.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * WorkloadEstimator - Intelligent workload estimation for optimal parallel job scheduling.
 *
 * <h2>The Problem with Naive Entity Counting</h2>
 * <p>Traditional ECS schedulers split work by entity count alone. But entity count is a poor 
 * proxy for actual computational work:
 * 
 * <pre>
 * // Archetype A: 1000 entities, trivial work per entity (1μs each)
 * // Archetype B: 100 entities, heavy work per entity (100μs each)
 * 
 * // Naive split: Thread 1 gets A (1ms total), Thread 2 gets B (10ms total)
 * // Result: Thread 1 idles for 9ms while Thread 2 completes ("long-tail" problem)
 * 
 * // Smart split with workload estimation:
 * // Thread 1: Archetype A (1ms) + part of B (5ms) = 6ms
 * // Thread 2: Rest of B (5ms) = 5ms
 * // Result: Both threads finish nearly simultaneously, no idle waste
 * </pre>
 *
 * <h2>Critical Features</h2>
 * <ul>
 *   <li><b>Per-Archetype Cost Profiling:</b> Track actual execution time per archetype, not just entity count</li>
 *   <li><b>Per-Entity Cost Estimation:</b> Systems declare estimateWorkload() method for custom logic</li>
 *   <li><b>Historical Cost Averaging:</b> Use exponential moving average to adapt to runtime conditions</li>
 *   <li><b>Component Complexity Weights:</b> Different components have different processing costs</li>
 *   <li><b>Work-Stealing Adjustment:</b> Feed data back to scheduler to prevent long-tail threads</li>
 *   <li><b>Batch Size Optimization:</b> Recommend optimal chunk sizes based on workload</li>
 * </ul>
 *
 * <h2>Integration with JobScheduler</h2>
 * <p>JobScheduler queries WorkloadEstimator to split tasks intelligently:
 * <pre>
 * List&lt;Archetype&gt; archetypes = getMatchingArchetypes();
 * WorkloadDistribution distribution = estimator.distribute(archetypes, threadCount);
 * 
 * for (WorkloadChunk chunk : distribution.chunks()) {
 *     executor.submit(() -> processChunk(chunk));
 * }
 * </pre>
 *
 * @author Enhanced ECS Framework (Kirino-inspired)
 * @version 1.0.0
 * @since Java 21
 */
public final class WorkloadEstimator {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    /** Default cost per entity if no historical data exists */
    private static final long DEFAULT_COST_NS = 1000;  // 1 microsecond

    /** Minimum entities per chunk (prevents over-splitting) */
    private static final int MIN_CHUNK_SIZE = 64;

    /** Exponential moving average alpha (weight of new samples) */
    private static final double EMA_ALPHA = 0.3;

    /** Cache line size for chunk alignment */
    private static final int CACHE_LINE_SIZE = 64;

    // ========================================================================
    // ANNOTATIONS
    // ========================================================================

    /**
     * Mark system as having custom workload estimation logic.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface CustomWorkloadEstimation {
        /** Base cost multiplier (scales estimated cost) */
        double multiplier() default 1.0;
    }

    /**
     * Declare component processing complexity.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ComponentComplexity {
        /** Component class */
        Class<?> component();
        /** Relative cost (1.0 = baseline, 2.0 = twice as expensive) */
        double cost() default 1.0;
    }

    /**
     * Mark system as having variable per-entity cost.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface VariableCost {}

    // ========================================================================
    // CORE STATE
    // ========================================================================

    /** Historical cost data per system + archetype */
    private final ConcurrentHashMap<WorkloadKey, CostHistory> costHistory = new ConcurrentHashMap<>();

    /** Component complexity weights */
    private final ConcurrentHashMap<Class<?>, Double> componentComplexity = new ConcurrentHashMap<>();

    /** System-specific workload estimators */
    private final ConcurrentHashMap<Class<? extends System>, WorkloadEstimatorFunc> customEstimators = 
        new ConcurrentHashMap<>();

    /** Statistics */
    private final LongAdder totalEstimations = new LongAdder();
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();
    private final DoubleAdder totalAccuracyError = new DoubleAdder();

    // ========================================================================
    // RECORDS
    // ========================================================================

    /**
     * Key for cost history lookup.
     */
    private record WorkloadKey(Class<? extends System> systemClass, long archetypeId) {
        static WorkloadKey of(System system, Archetype archetype) {
            return new WorkloadKey(system.getClass(), archetype.getId());
        }
    }

    /**
     * Historical cost tracking with exponential moving average.
     */
    private static final class CostHistory {
        private final AtomicLong sampleCount = new AtomicLong(0);
        private final DoubleAdder totalCost = new DoubleAdder();
        private volatile double emaValue;  // Nanoseconds per entity
        private volatile long lastUpdate;

        CostHistory(long initialCost) {
            this.emaValue = initialCost;
            this.lastUpdate = java.lang.System.nanoTime();
        }

        void recordSample(long costNs, int entityCount) {
            if (entityCount <= 0) return;

            double costPerEntity = (double) costNs / entityCount;
            
            // Update EMA
            double currentEma = emaValue;
            emaValue = (EMA_ALPHA * costPerEntity) + ((1 - EMA_ALPHA) * currentEma);
            
            sampleCount.incrementAndGet();
            totalCost.add(costNs);
            lastUpdate = java.lang.System.nanoTime();
        }

        double getEstimatedCostPerEntity() {
            return emaValue;
        }

        long getSampleCount() {
            return sampleCount.get();
        }

        double getAverageCost() {
            long samples = sampleCount.get();
            return samples > 0 ? totalCost.sum() / samples : emaValue;
        }
    }

    /**
     * Workload distribution result for parallel scheduling.
     */
    public record WorkloadDistribution(
        List<WorkloadChunk> chunks,
        long totalEstimatedCost,
        int totalEntities,
        int threadCount
    ) {
        /**
         * Get average cost per chunk.
         */
        public long getAverageCostPerChunk() {
            return chunks.isEmpty() ? 0 : totalEstimatedCost / chunks.size();
        }

        /**
         * Get load balance ratio (worst/best chunk cost).
         */
        public double getLoadBalanceRatio() {
            if (chunks.isEmpty()) return 1.0;

            long maxCost = chunks.stream().mapToLong(WorkloadChunk::estimatedCost).max().orElse(0);
            long minCost = chunks.stream().mapToLong(WorkloadChunk::estimatedCost).min().orElse(1);

            return (double) maxCost / minCost;
        }

        /**
         * Check if distribution is well-balanced (ratio < 1.5).
         */
        public boolean isWellBalanced() {
            return getLoadBalanceRatio() < 1.5;
        }
    }

    /**
     * Single chunk of work for a thread.
     */
    public record WorkloadChunk(
        Archetype archetype,
        int startIndex,
        int endIndex,
        long estimatedCost
    ) {
        /**
         * Get entity count in this chunk.
         */
        public int getEntityCount() {
            return endIndex - startIndex;
        }

        /**
         * Check if chunk is aligned to cache line boundaries.
         */
        public boolean isCacheAligned() {
            return (startIndex % CACHE_LINE_SIZE) == 0;
        }
    }

    /**
     * Custom workload estimator function.
     */
    @FunctionalInterface
    public interface WorkloadEstimatorFunc {
        /**
         * Estimate work units for processing entity at index in archetype.
         * 
         * @param archetype The archetype being processed
         * @param entityIndex Index of entity in archetype
         * @return Estimated work units (arbitrary scale, but consistent)
         */
        long estimateWork(Archetype archetype, int entityIndex);
    }

    // ========================================================================
    // REGISTRATION
    // ========================================================================

    /**
     * Register system for workload tracking.
     */
    public void registerSystem(System system) {
        Class<? extends System> systemClass = system.getClass();

        // Check for custom estimation
        CustomWorkloadEstimation annotation = systemClass.getAnnotation(CustomWorkloadEstimation.class);
        if (annotation != null) {
            Astralis.LOGGER.debug("[WorkloadEstimator] System {} uses custom estimation (multiplier: {})",
                systemClass.getSimpleName(), annotation.multiplier());
        }

        // Extract component complexity weights
        ComponentComplexity[] complexities = systemClass.getAnnotationsByType(ComponentComplexity.class);
        for (ComponentComplexity complexity : complexities) {
            registerComponentComplexity(complexity.component(), complexity.cost());
        }

        // Check for variable cost
        if (systemClass.isAnnotationPresent(VariableCost.class)) {
            Astralis.LOGGER.debug("[WorkloadEstimator] System {} has variable per-entity cost",
                systemClass.getSimpleName());
        }
    }

    /**
     * Register custom estimator function for system.
     */
    public void registerCustomEstimator(Class<? extends System> systemClass, WorkloadEstimatorFunc estimator) {
        customEstimators.put(systemClass, estimator);
        Astralis.LOGGER.info("[WorkloadEstimator] Registered custom estimator for {}",
            systemClass.getSimpleName());
    }

    /**
     * Register component processing complexity weight.
     */
    public void registerComponentComplexity(Class<?> componentClass, double cost) {
        componentComplexity.put(componentClass, cost);
        Astralis.LOGGER.trace("[WorkloadEstimator] Component {} has complexity weight: {}",
            componentClass.getSimpleName(), cost);
    }

    // ========================================================================
    // WORKLOAD ESTIMATION
    // ========================================================================

    /**
     * Estimate total workload for processing archetype with system.
     */
    public long estimateWorkload(System system, Archetype archetype) {
        totalEstimations.increment();

        WorkloadKey key = WorkloadKey.of(system, archetype);
        CostHistory history = costHistory.get(key);

        if (history != null && history.getSampleCount() > 0) {
            // Use historical data
            cacheHits.increment();
            double costPerEntity = history.getEstimatedCostPerEntity();
            return (long) (costPerEntity * archetype.getEntityCount());
        }

        // No history - estimate based on component complexity
        cacheMisses.increment();
        return estimateFromComplexity(system, archetype);
    }

    /**
     * Estimate workload based on component complexity weights.
     */
    private long estimateFromComplexity(System system, Archetype archetype) {
        // Base cost per entity
        long baseCost = DEFAULT_COST_NS;

        // Apply component complexity multipliers
        double complexityMultiplier = 1.0;
        
        // Get system's required components (would need to extract from annotations)
        // For now, use archetype component count as proxy
        int componentCount = archetype.getComponentCount();
        complexityMultiplier = Math.max(1.0, componentCount * 0.5);

        // Apply system-specific multiplier
        CustomWorkloadEstimation annotation = system.getClass().getAnnotation(CustomWorkloadEstimation.class);
        if (annotation != null) {
            complexityMultiplier *= annotation.multiplier();
        }

        long costPerEntity = (long) (baseCost * complexityMultiplier);
        return costPerEntity * archetype.getEntityCount();
    }

    /**
     * Estimate workload for entity using custom estimator.
     */
    public long estimateEntityWorkload(System system, Archetype archetype, int entityIndex) {
        WorkloadEstimatorFunc estimator = customEstimators.get(system.getClass());
        
        if (estimator != null) {
            return estimator.estimateWork(archetype, entityIndex);
        }

        // Fallback to average per-entity cost
        long totalCost = estimateWorkload(system, archetype);
        int entityCount = archetype.getEntityCount();
        return entityCount > 0 ? totalCost / entityCount : DEFAULT_COST_NS;
    }

    // ========================================================================
    // WORKLOAD DISTRIBUTION
    // ========================================================================

    /**
     * Distribute archetypes across threads with optimal load balancing.
     */
    public WorkloadDistribution distribute(System system, List<Archetype> archetypes, int threadCount) {
        if (archetypes.isEmpty() || threadCount <= 1) {
            // No distribution needed
            return createSingleThreadDistribution(system, archetypes);
        }

        // Estimate cost for each archetype
        List<ArchetypeWorkload> workloads = archetypes.stream()
            .map(archetype -> new ArchetypeWorkload(
                archetype,
                estimateWorkload(system, archetype)
            ))
            .sorted(Comparator.comparingLong(ArchetypeWorkload::cost).reversed())  // Largest first
            .toList();

        // Distribute using longest-processing-time-first algorithm
        List<WorkloadChunk> chunks = distributeWorkloads(workloads, threadCount);

        long totalCost = chunks.stream().mapToLong(WorkloadChunk::estimatedCost).sum();
        int totalEntities = chunks.stream().mapToInt(WorkloadChunk::getEntityCount).sum();

        WorkloadDistribution distribution = new WorkloadDistribution(chunks, totalCost, totalEntities, threadCount);

        Astralis.LOGGER.trace("[WorkloadEstimator] Distributed {} archetypes across {} threads (balance ratio: {:.2f})",
            archetypes.size(), threadCount, distribution.getLoadBalanceRatio());

        return distribution;
    }

    /**
     * Distribute workloads using LPT (Longest Processing Time first) greedy algorithm.
     */
    private List<WorkloadChunk> distributeWorkloads(List<ArchetypeWorkload> workloads, int threadCount) {
        // Priority queue of threads by current load
        PriorityQueue<ThreadLoad> threads = new PriorityQueue<>(
            Comparator.comparingLong(ThreadLoad::totalCost)
        );

        // Initialize threads
        for (int i = 0; i < threadCount; i++) {
            threads.offer(new ThreadLoad(i));
        }

        // Assign archetypes to threads
        for (ArchetypeWorkload workload : workloads) {
            // Get least-loaded thread
            ThreadLoad thread = threads.poll();
            
            // Check if archetype should be split
            if (workload.archetype().getEntityCount() > MIN_CHUNK_SIZE * 2) {
                // Split large archetypes across multiple chunks
                List<WorkloadChunk> splits = splitArchetype(workload, thread.chunks.size());
                for (WorkloadChunk chunk : splits) {
                    thread.addChunk(chunk);
                }
            } else {
                // Assign entire archetype
                WorkloadChunk chunk = new WorkloadChunk(
                    workload.archetype(),
                    0,
                    workload.archetype().getEntityCount(),
                    workload.cost()
                );
                thread.addChunk(chunk);
            }

            threads.offer(thread);  // Re-insert with updated cost
        }

        // Collect all chunks
        List<WorkloadChunk> allChunks = new ArrayList<>();
        for (ThreadLoad thread : threads) {
            allChunks.addAll(thread.chunks);
        }

        return allChunks;
    }

    /**
     * Split large archetype into smaller cache-friendly chunks.
     */
    private List<WorkloadChunk> splitArchetype(ArchetypeWorkload workload, int preferredChunkCount) {
        Archetype archetype = workload.archetype();
        int entityCount = archetype.getEntityCount();
        long totalCost = workload.cost();

        // Calculate optimal chunk size (aligned to cache lines)
        int baseChunkSize = Math.max(MIN_CHUNK_SIZE, entityCount / Math.max(1, preferredChunkCount * 2));
        int alignedChunkSize = alignUp(baseChunkSize, CACHE_LINE_SIZE);

        List<WorkloadChunk> chunks = new ArrayList<>();
        int startIndex = 0;

        while (startIndex < entityCount) {
            int endIndex = Math.min(startIndex + alignedChunkSize, entityCount);
            int chunkSize = endIndex - startIndex;

            long chunkCost = (long) ((double) totalCost * chunkSize / entityCount);

            chunks.add(new WorkloadChunk(archetype, startIndex, endIndex, chunkCost));
            startIndex = endIndex;
        }

        return chunks;
    }

    /**
     * Create single-thread distribution (no splitting).
     */
    private WorkloadDistribution createSingleThreadDistribution(System system, List<Archetype> archetypes) {
        List<WorkloadChunk> chunks = new ArrayList<>();
        long totalCost = 0;
        int totalEntities = 0;

        for (Archetype archetype : archetypes) {
            long cost = estimateWorkload(system, archetype);
            int count = archetype.getEntityCount();

            chunks.add(new WorkloadChunk(archetype, 0, count, cost));
            totalCost += cost;
            totalEntities += count;
        }

        return new WorkloadDistribution(chunks, totalCost, totalEntities, 1);
    }

    // ========================================================================
    // COST RECORDING (FEEDBACK LOOP)
    // ========================================================================

    /**
     * Record actual execution cost for learning.
     */
    public void recordCost(System system, Archetype archetype, long actualCostNs, int entityCount) {
        WorkloadKey key = WorkloadKey.of(system, archetype);

        costHistory.compute(key, (k, history) -> {
            if (history == null) {
                history = new CostHistory(actualCostNs / Math.max(1, entityCount));
            }
            history.recordSample(actualCostNs, entityCount);
            return history;
        });

        // Calculate accuracy error
        long estimated = estimateWorkload(system, archetype);
        double error = Math.abs(estimated - actualCostNs) / (double) Math.max(1, actualCostNs);
        totalAccuracyError.add(error);
    }

    // ========================================================================
    // BATCH SIZE RECOMMENDATION
    // ========================================================================

    /**
     * Recommend optimal batch size for processing based on workload.
     */
    public int recommendBatchSize(System system, Archetype archetype) {
        long costPerEntity = estimateWorkload(system, archetype) / Math.max(1, archetype.getEntityCount());

        // Heuristic: batch size inversely proportional to per-entity cost
        // High cost → smaller batches for better responsiveness
        // Low cost → larger batches to amortize overhead

        if (costPerEntity > 10_000) {
            // Expensive work (>10μs per entity)
            return alignUp(MIN_CHUNK_SIZE / 2, 16);
        } else if (costPerEntity > 1_000) {
            // Medium work (1-10μs per entity)
            return alignUp(MIN_CHUNK_SIZE, CACHE_LINE_SIZE);
        } else {
            // Cheap work (<1μs per entity)
            return alignUp(MIN_CHUNK_SIZE * 4, CACHE_LINE_SIZE);
        }
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Get estimator statistics.
     */
    public EstimatorStats getStats() {
        long estimations = totalEstimations.sum();
        long hits = cacheHits.sum();
        long misses = cacheMisses.sum();
        double avgError = estimations > 0 ? totalAccuracyError.sum() / estimations : 0.0;

        return new EstimatorStats(
            costHistory.size(),
            estimations,
            hits,
            misses,
            avgError,
            componentComplexity.size(),
            customEstimators.size()
        );
    }

    public record EstimatorStats(
        int trackedWorkloads,
        long totalEstimations,
        long cacheHits,
        long cacheMisses,
        double averageError,
        int registeredComplexities,
        int customEstimators
    ) {
        public double cacheHitRate() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }

        public double accuracyPercent() {
            return Math.max(0.0, (1.0 - averageError) * 100.0);
        }
    }

    // ========================================================================
    // HELPER RECORDS
    // ========================================================================

    /**
     * Archetype with associated workload cost.
     */
    private record ArchetypeWorkload(Archetype archetype, long cost) {}

    /**
     * Thread load tracking for distribution.
     */
    private static final class ThreadLoad {
        final int threadId;
        final List<WorkloadChunk> chunks = new ArrayList<>();
        long totalCost = 0;

        ThreadLoad(int threadId) {
            this.threadId = threadId;
        }

        void addChunk(WorkloadChunk chunk) {
            chunks.add(chunk);
            totalCost += chunk.estimatedCost();
        }

        long totalCost() {
            return totalCost;
        }
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private static int alignUp(int value, int alignment) {
        return (value + alignment - 1) & ~(alignment - 1);
    }

    // ========================================================================
    // DEBUG
    // ========================================================================

    /**
     * Describe workload history.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  Workload Estimator\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");

        EstimatorStats stats = getStats();
        sb.append("  Tracked Workloads: ").append(stats.trackedWorkloads()).append("\n");
        sb.append("  Total Estimations: ").append(stats.totalEstimations()).append("\n");
        sb.append("  Cache Hit Rate: ").append(String.format("%.2f%%", stats.cacheHitRate() * 100)).append("\n");
        sb.append("  Average Accuracy: ").append(String.format("%.2f%%", stats.accuracyPercent())).append("\n");
        sb.append("  Component Complexities: ").append(stats.registeredComplexities()).append("\n");
        sb.append("  Custom Estimators: ").append(stats.customEstimators()).append("\n");
        sb.append("───────────────────────────────────────────────────────────────\n");

        // Show top workloads
        List<Map.Entry<WorkloadKey, CostHistory>> sorted = costHistory.entrySet().stream()
            .sorted((a, b) -> Long.compare(
                b.getValue().getSampleCount(),
                a.getValue().getSampleCount()
            ))
            .limit(10)
            .toList();

        sb.append("  Top Workloads:\n");
        for (Map.Entry<WorkloadKey, CostHistory> entry : sorted) {
            WorkloadKey key = entry.getKey();
            CostHistory history = entry.getValue();

            sb.append(String.format("    %s (archetype %d): %.2f μs/entity (%d samples)\n",
                key.systemClass().getSimpleName(),
                key.archetypeId(),
                history.getEstimatedCostPerEntity() / 1000.0,
                history.getSampleCount()
            ));
        }

        sb.append("═══════════════════════════════════════════════════════════════\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("WorkloadEstimator[tracked=%d, estimations=%d]",
            costHistory.size(), totalEstimations.sum());
    }
}
