package stellar.snow.astralis.integration.DeepMix.Core;

/*
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                            ║
 * ║  🔮 DEEPMIX MEMORY OPTIMIZER — SURGICAL MEMORY MANAGEMENT                 ║
 * ║  ═════════════════════════════════════════════════════════                  ║
 * ║                                                                            ║
 * ║  "Every byte accounted for, every leak hunted down"                        ║
 * ║                                                                            ║
 * ║  Specialization:                                                           ║
 * ║    • LEAK DETECTION — Phantom reference tracking, allocation tracing       ║
 * ║    • LEAK PREVENTION — Arena-scoped allocation, automatic cleanup          ║
 * ║    • MEMORY SLOP — Detect and eliminate wasted padding/fragmentation       ║
 * ║    • MEMORY HOGS — Identify and throttle excessive allocators              ║
 * ║    • OFF-HEAP — Foreign Memory API (Panama) for zero-GC-pressure storage   ║
 * ║    • DIRECT BUFFERS — Pool and lifecycle-manage DirectByteBuffers          ║
 * ║    • COMPACTION — Defragment pooled structures                             ║
 * ║    • GC TUNING — Adaptive reference strength, GC-aware caching            ║
 * ║    • NATIVE INTEROP — FFM Linker for platform memory introspection         ║
 * ║                                                                            ║
 * ║  Java 25 Features Used:                                                    ║
 * ║    • Foreign Function & Memory API (java.lang.foreign)                     ║
 * ║    • Arena (confined, shared, auto, global, slicing)                        ║
 * ║    • MemorySegment / MemoryLayout / ValueLayout                            ║
 * ║    • Linker + FunctionDescriptor (native malloc/free/mmap)                 ║
 * ║    • SymbolLookup (platform C library access)                              ║
 * ║    • Virtual Threads (leak detector daemon)                                ║
 * ║    • Scoped Values (allocation context propagation)                         ║
 * ║    • StructuredTaskScope (parallel leak scanning)                           ║
 * ║    • Sealed interfaces + Record patterns                                   ║
 * ║    • SequencedCollections                                                  ║
 * ║                                                                            ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */

import java.lang.foreign.*;
import java.lang.foreign.MemorySegment.Scope;
import java.lang.invoke.*;
import java.lang.management.*;
import java.lang.ref.*;
import java.lang.reflect.Field;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * ╭──────────────────────────────────────────────────────────────────────────╮
 * │                                                                        │
 * │  DeepMixMemoryOptimizer                                                │
 * │                                                                        │
 * │  A dedicated memory management engine that hunts leaks, eliminates     │
 * │  slop, throttles hogs, and provides off-heap storage via Java 25's     │
 * │  Foreign Function & Memory API.                                        │
 * │                                                                        │
 * │  Architecture:                                                         │
 * │    ┌─────────────────────────────────────────────────┐                  │
 * │    │              Application Code                    │                 │
 * │    └──────────┬──────────────────────┬───────────────┘                  │
 * │               │                      │                                  │
 * │    ┌──────────▼──────────┐ ┌────────▼────────────┐                     │
 * │    │  Heap Tracker       │ │  Off-Heap Manager    │                     │
 * │    │  (Leak Detection)   │ │  (Arena/Segment)     │                     │
 * │    └──────────┬──────────┘ └────────┬────────────┘                     │
 * │               │                      │                                  │
 * │    ┌──────────▼──────────────────────▼───────────┐                     │
 * │    │          Memory Pressure Governor            │                     │
 * │    │  (GC-aware, adaptive, budget-enforced)       │                     │
 * │    └──────────┬──────────────────────┬───────────┘                     │
 * │               │                      │                                  │
 * │    ┌──────────▼──────────┐ ┌────────▼────────────┐                     │
 * │    │  Compaction Engine  │ │  Native Interop     │                     │
 * │    │  (Defrag/Slop)      │ │  (FFM Linker)       │                     │
 * │    └─────────────────────┘ └─────────────────────┘                     │
 * │                                                                        │
 * ╰──────────────────────────────────────────────────────────────────────────╯
 */
public final class DeepMixMemoryOptimizer {

    private DeepMixMemoryOptimizer() {
        throw new UnsupportedOperationException(
            "DeepMixMemoryOptimizer is a static engine — do not instantiate");
    }

    // ════════════════════════════════════════════════════════════════════
    //  CONSTANTS & CONFIGURATION
    // ════════════════════════════════════════════════════════════════════

    /** Maximum off-heap memory DeepMix is allowed to use (256MB default) */
    private static volatile long MAX_OFFHEAP_BYTES = 256L * 1024 * 1024;

    /** Leak detection scan interval */
    private static volatile long LEAK_SCAN_INTERVAL_MS = 10_000; // 10 seconds

    /** Allocation older than this is considered a potential leak */
    private static volatile long LEAK_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutes

    /** Allocation tracking sample rate (1.0 = track everything, 0.01 = 1%) */
    private static volatile double ALLOCATION_SAMPLE_RATE = 0.1; // 10% by default

    /** Maximum tracked allocations before oldest are evicted */
    private static final int MAX_TRACKED_ALLOCATIONS = 50_000;

    /** Slop threshold — allocations wasting more than this ratio are flagged */
    private static final double SLOP_THRESHOLD = 0.25; // 25% waste

    /** Hog threshold — single allocator using more than this fraction */
    private static final double HOG_THRESHOLD = 0.30; // 30% of total

    /** Direct buffer pool max size per tier */
    private static final int DIRECT_POOL_MAX_PER_TIER = 32;

    private static volatile boolean initialized = false;
    private static final Object initLock = new Object();


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 1: OFF-HEAP MEMORY MANAGEMENT (Foreign Memory API)        ║
    // ║                                                                    ║
    // ║  Uses Java 25 Arena / MemorySegment for zero-GC-pressure storage.  ║
    // ║  All off-heap allocations are tracked, bounded, and auto-cleaned.  ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Managed off-heap memory region using Java 25 Foreign Memory API.
     *
     * Each ManagedRegion wraps an Arena and tracks all allocations within it.
     * Regions can be scoped (auto-close), confined (single-thread), or shared.
     */
    public sealed interface ManagedRegion extends AutoCloseable {

        /** Unique region identifier */
        String id();

        /** Allocate a segment of the given size within this region */
        MemorySegment allocate(long byteSize, long byteAlignment);

        /** Allocate with default alignment */
        default MemorySegment allocate(long byteSize) {
            return allocate(byteSize, 8); // 8-byte alignment default
        }

        /** Allocate and zero-fill */
        default MemorySegment allocateZeroed(long byteSize) {
            MemorySegment seg = allocate(byteSize);
            seg.fill((byte) 0);
            return seg;
        }

        /** Allocate a typed layout */
        default MemorySegment allocate(MemoryLayout layout) {
            return allocate(layout.byteSize(), layout.byteAlignment());
        }

        /** Allocate an array of elements */
        default MemorySegment allocateArray(MemoryLayout elementLayout, long count) {
            return allocate(elementLayout.byteSize() * count, elementLayout.byteAlignment());
        }

        /** Total bytes allocated in this region */
        long allocatedBytes();

        /** Number of individual allocations */
        int allocationCount();

        /** Whether this region is still alive (not closed) */
        boolean isAlive();

        /** The underlying Arena scope */
        Scope scope();

        // ── Concrete implementations ──

        /**
         * Confined region — single-thread access, manual close.
         * Most efficient for thread-local work.
         */
        record Confined(
            String id,
            Arena arena,
            AtomicLong allocated,
            AtomicInteger count,
            Instant createdAt,
            String creator
        ) implements ManagedRegion {

            public Confined(String id) {
                this(id, Arena.ofConfined(), new AtomicLong(0),
                    new AtomicInteger(0), Instant.now(),
                    Thread.currentThread().getName());
            }

            @Override
            public MemorySegment allocate(long byteSize, long byteAlignment) {
                OffHeapManager.checkBudget(byteSize);
                MemorySegment seg = arena.allocate(byteSize, byteAlignment);
                allocated.addAndGet(byteSize);
                count.incrementAndGet();
                OffHeapManager.recordAllocation(id, byteSize);
                return seg;
            }

            @Override public long allocatedBytes() { return allocated.get(); }
            @Override public int allocationCount() { return count.get(); }
            @Override public boolean isAlive() { return arena.scope().isAlive(); }
            @Override public Scope scope() { return arena.scope(); }

            @Override
            public void close() {
                if (isAlive()) {
                    long freed = allocated.get();
                    arena.close();
                    OffHeapManager.recordDeallocation(id, freed);
                }
            }
        }

        /**
         * Shared region — multi-thread access, manual close.
         * Slightly more overhead but safe for concurrent use.
         */
        record Shared(
            String id,
            Arena arena,
            AtomicLong allocated,
            AtomicInteger count,
            Instant createdAt
        ) implements ManagedRegion {

            public Shared(String id) {
                this(id, Arena.ofShared(), new AtomicLong(0),
                    new AtomicInteger(0), Instant.now());
            }

            @Override
            public MemorySegment allocate(long byteSize, long byteAlignment) {
                OffHeapManager.checkBudget(byteSize);
                MemorySegment seg = arena.allocate(byteSize, byteAlignment);
                allocated.addAndGet(byteSize);
                count.incrementAndGet();
                OffHeapManager.recordAllocation(id, byteSize);
                return seg;
            }

            @Override public long allocatedBytes() { return allocated.get(); }
            @Override public int allocationCount() { return count.get(); }
            @Override public boolean isAlive() { return arena.scope().isAlive(); }
            @Override public Scope scope() { return arena.scope(); }

            @Override
            public void close() {
                if (isAlive()) {
                    long freed = allocated.get();
                    arena.close();
                    OffHeapManager.recordDeallocation(id, freed);
                }
            }
        }

        /**
         * Auto region — GC-cleaned, no explicit close needed.
         * WARNING: Use sparingly — deallocation timing is non-deterministic.
         */
        record Auto(
            String id,
            Arena arena,
            AtomicLong allocated,
            AtomicInteger count,
            Instant createdAt
        ) implements ManagedRegion {

            public Auto(String id) {
                this(id, Arena.ofAuto(), new AtomicLong(0),
                    new AtomicInteger(0), Instant.now());
            }

            @Override
            public MemorySegment allocate(long byteSize, long byteAlignment) {
                OffHeapManager.checkBudget(byteSize);
                MemorySegment seg = arena.allocate(byteSize, byteAlignment);
                allocated.addAndGet(byteSize);
                count.incrementAndGet();
                OffHeapManager.recordAllocation(id, byteSize);
                return seg;
            }

            @Override public long allocatedBytes() { return allocated.get(); }
            @Override public int allocationCount() { return count.get(); }
            @Override public boolean isAlive() { return arena.scope().isAlive(); }
            @Override public Scope scope() { return arena.scope(); }

            @Override
            public void close() {
                // Auto arenas are GC-managed — record deallocation estimate
                OffHeapManager.recordDeallocation(id, allocated.get());
            }
        }

        /**
         * Slicing region — pre-allocates a large block and slices from it.
         * Most memory-efficient for many small allocations.
         * Eliminates per-allocation syscall overhead.
         */
        record Slicing(
            String id,
            Arena backingArena,
            MemorySegment slab,
            AtomicLong offset,
            long capacity,
            AtomicInteger count,
            Instant createdAt
        ) implements ManagedRegion {

            public Slicing(String id, long slabSize) {
                this(id, Arena.ofShared(),
                    null, // Set in factory
                    new AtomicLong(0), slabSize,
                    new AtomicInteger(0), Instant.now());
            }

            /** Factory that properly initializes the slab */
            public static Slicing create(String id, long slabSize) {
                Arena arena = Arena.ofShared();
                MemorySegment slab = arena.allocate(slabSize, 16);
                slab.fill((byte) 0);
                OffHeapManager.recordAllocation(id + ".slab", slabSize);
                return new Slicing(id, arena, slab, new AtomicLong(0),
                    slabSize, new AtomicInteger(0), Instant.now());
            }

            @Override
            public MemorySegment allocate(long byteSize, long byteAlignment) {
                // Align the offset
                long alignMask = byteAlignment - 1;
                long currentOffset;
                long alignedOffset;
                long newOffset;

                do {
                    currentOffset = offset.get();
                    alignedOffset = (currentOffset + alignMask) & ~alignMask;
                    newOffset = alignedOffset + byteSize;

                    if (newOffset > capacity) {
                        throw new OutOfMemoryError(
                            "Slicing region '" + id + "' exhausted: " +
                                currentOffset + "/" + capacity + " bytes used, " +
                                "requested " + byteSize);
                    }
                } while (!offset.compareAndSet(currentOffset, newOffset));

                count.incrementAndGet();
                return slab.asSlice(alignedOffset, byteSize);
            }

            @Override public long allocatedBytes() { return offset.get(); }
            @Override public int allocationCount() { return count.get(); }
            @Override public boolean isAlive() { return backingArena.scope().isAlive(); }
            @Override public Scope scope() { return backingArena.scope(); }

            /** Remaining capacity in bytes */
            public long remainingBytes() { return capacity - offset.get(); }

            /** Usage ratio (0.0 to 1.0) */
            public double usageRatio() { return (double) offset.get() / capacity; }

            /** Reset the slab — reuse all memory (DANGEROUS: invalidates all slices) */
            public void reset() {
                offset.set(0);
                count.set(0);
                slab.fill((byte) 0);
            }

            @Override
            public void close() {
                if (isAlive()) {
                    long freed = capacity;
                    backingArena.close();
                    OffHeapManager.recordDeallocation(id, freed);
                }
            }
        }
    }

    /**
     * Central manager for all off-heap memory.
     * Tracks total off-heap usage, enforces global budget,
     * and provides region lifecycle management.
     */
    public static final class OffHeapManager {

        private static final AtomicLong totalOffHeapAllocated = new AtomicLong(0);
        private static final AtomicLong totalOffHeapFreed = new AtomicLong(0);
        private static final AtomicLong peakOffHeapUsage = new AtomicLong(0);
        private static final ConcurrentHashMap<String, ManagedRegion> activeRegions =
            new ConcurrentHashMap<>();

        // Per-region allocation tracking
        private static final ConcurrentHashMap<String, AtomicLong> regionAllocations =
            new ConcurrentHashMap<>();

        // Allocation event log (ring buffer)
        private static final int EVENT_LOG_SIZE = 4096;
        private static final AllocationEvent[] eventLog = new AllocationEvent[EVENT_LOG_SIZE];
        private static final AtomicInteger eventLogIndex = new AtomicInteger(0);

        /** Create a new confined (single-thread) region */
        public static ManagedRegion.Confined createConfined(String id) {
            ManagedRegion.Confined region = new ManagedRegion.Confined(id);
            activeRegions.put(id, region);
            return region;
        }

        /** Create a new shared (multi-thread) region */
        public static ManagedRegion.Shared createShared(String id) {
            ManagedRegion.Shared region = new ManagedRegion.Shared(id);
            activeRegions.put(id, region);
            return region;
        }

        /** Create a new auto (GC-managed) region */
        public static ManagedRegion.Auto createAuto(String id) {
            ManagedRegion.Auto region = new ManagedRegion.Auto(id);
            activeRegions.put(id, region);
            return region;
        }

        /** Create a new slicing region with pre-allocated slab */
        public static ManagedRegion.Slicing createSlicing(String id, long slabSize) {
            checkBudget(slabSize);
            ManagedRegion.Slicing region = ManagedRegion.Slicing.create(id, slabSize);
            activeRegions.put(id, region);
            return region;
        }

        /** Execute code with a temporary confined region (auto-closed) */
        public static <T> T withConfinedRegion(String id, Function<ManagedRegion, T> action) {
            try (ManagedRegion.Confined region = createConfined(id)) {
                return action.apply(region);
            }
        }

        /** Execute void code with a temporary confined region */
        public static void withConfinedRegion(String id, Consumer<ManagedRegion> action) {
            try (ManagedRegion.Confined region = createConfined(id)) {
                action.accept(region);
            }
        }

        /** Execute code with a temporary shared region */
        public static <T> T withSharedRegion(String id, Function<ManagedRegion, T> action) {
            try (ManagedRegion.Shared region = createShared(id)) {
                return action.apply(region);
            }
        }

        /** Close and remove a region */
        public static void closeRegion(String id) {
            ManagedRegion region = activeRegions.remove(id);
            if (region != null && region.isAlive()) {
                try {
                    region.close();
                } catch (Exception e) {
                    System.err.println("[DeepMix:OffHeap] Error closing region '" +
                        id + "': " + e.getMessage());
                }
            }
        }

        /** Close ALL active regions — emergency cleanup */
        public static void closeAllRegions() {
            List<String> ids = new ArrayList<>(activeRegions.keySet());
            for (String id : ids) {
                closeRegion(id);
            }
        }

        /** Get current live off-heap usage in bytes */
        public static long liveOffHeapBytes() {
            return totalOffHeapAllocated.get() - totalOffHeapFreed.get();
        }

        /** Get peak off-heap usage */
        public static long peakOffHeapBytes() {
            return peakOffHeapUsage.get();
        }

        /** Get number of active regions */
        public static int activeRegionCount() {
            return activeRegions.size();
        }

        /** Get all active region IDs */
        public static Set<String> activeRegionIds() {
            return Collections.unmodifiableSet(activeRegions.keySet());
        }

        /** Get a specific active region */
        public static ManagedRegion getRegion(String id) {
            return activeRegions.get(id);
        }

        /** Get recent allocation events */
        public static List<AllocationEvent> recentEvents(int count) {
            List<AllocationEvent> events = new ArrayList<>();
            int currentIdx = eventLogIndex.get();
            int start = Math.max(0, currentIdx - count);
            for (int i = start; i < currentIdx; i++) {
                AllocationEvent event = eventLog[i & (EVENT_LOG_SIZE - 1)];
                if (event != null) events.add(event);
            }
            return events;
        }

        /** Get off-heap statistics */
        public static OffHeapStats stats() {
            long allocated = totalOffHeapAllocated.get();
            long freed = totalOffHeapFreed.get();
            long live = allocated - freed;
            long peak = peakOffHeapUsage.get();

            Map<String, Long> regionSizes = new LinkedHashMap<>();
            for (Map.Entry<String, ManagedRegion> entry : activeRegions.entrySet()) {
                regionSizes.put(entry.getKey(), entry.getValue().allocatedBytes());
            }

            return new OffHeapStats(allocated, freed, live, peak,
                MAX_OFFHEAP_BYTES, activeRegions.size(), regionSizes);
        }

        // ── Budget enforcement ──

        static void checkBudget(long requestedBytes) {
            long currentUsage = liveOffHeapBytes();
            if (currentUsage + requestedBytes > MAX_OFFHEAP_BYTES) {
                // Try to reclaim — close dead regions
                reclaimDeadRegions();

                currentUsage = liveOffHeapBytes();
                if (currentUsage + requestedBytes > MAX_OFFHEAP_BYTES) {
                    throw new OutOfMemoryError(
                        "[DeepMix:OffHeap] Off-heap budget exceeded: " +
                            "current=" + (currentUsage / 1024) + "KB, " +
                            "requested=" + (requestedBytes / 1024) + "KB, " +
                            "budget=" + (MAX_OFFHEAP_BYTES / (1024 * 1024)) + "MB");
                }
            }
        }

        static void recordAllocation(String regionId, long bytes) {
            totalOffHeapAllocated.addAndGet(bytes);
            regionAllocations.computeIfAbsent(regionId, k -> new AtomicLong(0))
                .addAndGet(bytes);

            long current = liveOffHeapBytes();
            long peak;
            do {
                peak = peakOffHeapUsage.get();
                if (current <= peak) break;
            } while (!peakOffHeapUsage.compareAndSet(peak, current));

            // Log event
            int idx = eventLogIndex.getAndIncrement() & (EVENT_LOG_SIZE - 1);
            eventLog[idx] = new AllocationEvent(
                regionId, bytes, true, System.currentTimeMillis(),
                Thread.currentThread().getName());
        }

        static void recordDeallocation(String regionId, long bytes) {
            totalOffHeapFreed.addAndGet(bytes);
            activeRegions.remove(regionId);
            regionAllocations.remove(regionId);

            int idx = eventLogIndex.getAndIncrement() & (EVENT_LOG_SIZE - 1);
            eventLog[idx] = new AllocationEvent(
                regionId, bytes, false, System.currentTimeMillis(),
                Thread.currentThread().getName());
        }

        private static void reclaimDeadRegions() {
            Iterator<Map.Entry<String, ManagedRegion>> it = activeRegions.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ManagedRegion> entry = it.next();
                if (!entry.getValue().isAlive()) {
                    long bytes = entry.getValue().allocatedBytes();
                    totalOffHeapFreed.addAndGet(bytes);
                    regionAllocations.remove(entry.getKey());
                    it.remove();
                }
            }
        }
    }

    /** Off-heap allocation event record */
    public record AllocationEvent(
        String regionId,
        long bytes,
        boolean isAllocation, // true=alloc, false=free
        long timestampMs,
        String threadName
    ) {
        @Override
        public String toString() {
            return String.format("[%s] %s %s %dKB on %s",
                Instant.ofEpochMilli(timestampMs),
                isAllocation ? "ALLOC" : "FREE",
                regionId, bytes / 1024, threadName);
        }
    }

    /** Off-heap memory statistics */
    public record OffHeapStats(
        long totalAllocated,
        long totalFreed,
        long liveBytes,
        long peakBytes,
        long budgetBytes,
        int activeRegions,
        Map<String, Long> regionSizes
    ) {
        public double usageRatio() {
            return budgetBytes > 0 ? (double) liveBytes / budgetBytes : 0;
        }

        @Override
        public String toString() {
            return String.format(
                "OffHeap[live=%dKB, peak=%dKB, budget=%dMB (%.1f%%), regions=%d]",
                liveBytes / 1024, peakBytes / 1024, budgetBytes / (1024 * 1024),
                usageRatio() * 100, activeRegions);
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 2: LEAK DETECTION ENGINE                                  ║
    // ║                                                                    ║
    // ║  Uses PhantomReferences + ReferenceQueue to track object           ║
    // ║  lifecycle and detect allocations that are never freed.            ║
    // ║  Also tracks allocation call sites for leak source identification. ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Leak detection engine using phantom reference tracking.
     *
     * Every tracked allocation gets a PhantomReference registered with
     * a ReferenceQueue. The leak detector daemon polls the queue to
     * detect when objects are GC'd. Objects that live past LEAK_THRESHOLD_MS
     * without being explicitly released are flagged as potential leaks.
     */
    public static final class LeakDetector {

        /** Tracked allocation metadata */
        static final class AllocationRecord {
            final long id;
            final String description;
            final long allocatedAtMs;
            final long sizeEstimate;
            final StackTraceElement[] allocationSite;
            final String threadName;
            final String allocatorClass;
            volatile boolean released = false;
            volatile long releasedAtMs = 0;

            AllocationRecord(long id, String description, long sizeEstimate,
                            StackTraceElement[] site, String threadName) {
                this.id = id;
                this.description = description;
                this.allocatedAtMs = System.currentTimeMillis();
                this.sizeEstimate = sizeEstimate;
                this.allocationSite = site;
                this.threadName = threadName;
                this.allocatorClass = site.length > 0 ? site[0].getClassName() : "unknown";
            }

            long ageMs() {
                return (released ? releasedAtMs : System.currentTimeMillis()) - allocatedAtMs;
            }

            boolean isPotentialLeak() {
                return !released && ageMs() > LEAK_THRESHOLD_MS;
            }
        }

        /** Phantom reference that carries allocation metadata */
        static final class TrackedReference<T> extends PhantomReference<T> {
            final long allocationId;

            TrackedReference(T referent, ReferenceQueue<? super T> queue, long allocationId) {
                super(referent, queue);
                this.allocationId = allocationId;
            }
        }

        // Core tracking structures
        private static final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
        private static final ConcurrentHashMap<Long, AllocationRecord> liveAllocations =
            new ConcurrentHashMap<>();
        private static final AtomicLong allocationIdCounter = new AtomicLong(0);
        private static final AtomicLong totalTracked = new AtomicLong(0);
        private static final AtomicLong totalLeaksDetected = new AtomicLong(0);
        private static final AtomicLong totalLeaksResolved = new AtomicLong(0);

        // Leak history (ring buffer of recent leaks)
        private static final int LEAK_HISTORY_SIZE = 256;
        private static final LeakReport[] leakHistory = new LeakReport[LEAK_HISTORY_SIZE];
        private static final AtomicInteger leakHistoryIndex = new AtomicInteger(0);

        // Leak listeners
        private static final CopyOnWriteArrayList<Consumer<LeakReport>> leakListeners =
            new CopyOnWriteArrayList<>();

        // Sampling random (thread-safe)
        private static final ThreadLocal<SplittableRandom> samplingRandom =
            ThreadLocal.withInitial(SplittableRandom::new);

        // Daemon thread
        private static volatile Thread detectorThread = null;
        private static volatile boolean detectorRunning = false;

        /**
         * Track an object for leak detection.
         *
         * @param object      The object to track
         * @param description Human-readable description
         * @param sizeEstimate Estimated size in bytes
         * @return Allocation ID for explicit release
         */
        public static long track(Object object, String description, long sizeEstimate) {
            // Apply sampling
            if (ALLOCATION_SAMPLE_RATE < 1.0 &&
                samplingRandom.get().nextDouble() > ALLOCATION_SAMPLE_RATE) {
                return -1; // Not sampled
            }

            if (liveAllocations.size() >= MAX_TRACKED_ALLOCATIONS) {
                // Evict oldest non-leaked entry
                evictOldestTracked();
            }

            long id = allocationIdCounter.incrementAndGet();

            // Capture allocation call site (skip internal frames)
            StackTraceElement[] fullStack = Thread.currentThread().getStackTrace();
            StackTraceElement[] site = Arrays.copyOfRange(fullStack,
                Math.min(3, fullStack.length), Math.min(13, fullStack.length));

            AllocationRecord record = new AllocationRecord(
                id, description, sizeEstimate, site, Thread.currentThread().getName());

            liveAllocations.put(id, record);
            totalTracked.incrementAndGet();

            // Register phantom reference
            new TrackedReference<>(object, referenceQueue, id);

            ensureDetectorRunning();
            return id;
        }

        /** Track with auto-estimated size */
        public static long track(Object object, String description) {
            return track(object, description, estimateObjectSize(object));
        }

        /**
         * Explicitly release a tracked allocation.
         * Call this when you know an object is being properly disposed.
         */
        public static void release(long allocationId) {
            if (allocationId < 0) return; // Not sampled

            AllocationRecord record = liveAllocations.remove(allocationId);
            if (record != null) {
                record.released = true;
                record.releasedAtMs = System.currentTimeMillis();
            }
        }

        /**
         * Track a resource that should be released within a given duration.
         * If not released in time, it's reported as a leak.
         */
        public static <T extends AutoCloseable> T trackAutoCloseable(
                T resource, String description, Duration maxLifetime) {
            long id = track(resource, description);

            // Schedule a check for this specific resource
            if (id >= 0) {
                CompletableFuture.delayedExecutor(
                        maxLifetime.toMillis(), TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        AllocationRecord record = liveAllocations.get(id);
                        if (record != null && !record.released) {
                            reportLeak(record, "AutoCloseable not closed within " +
                                maxLifetime);
                        }
                    });
            }

            return resource;
        }

        /** Get all currently detected potential leaks */
        public static List<LeakReport> detectLeaks() {
            List<LeakReport> leaks = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (AllocationRecord record : liveAllocations.values()) {
                if (!record.released && (now - record.allocatedAtMs) > LEAK_THRESHOLD_MS) {
                    leaks.add(new LeakReport(
                        record.id, record.description, record.sizeEstimate,
                        record.ageMs(), record.allocationSite,
                        record.threadName, record.allocatorClass,
                        "Object alive for " + (record.ageMs() / 1000) + "s without release"
                    ));
                }
            }

            return leaks;
        }

        /** Get leak detection statistics */
        public static LeakStats stats() {
            int potentialLeaks = 0;
            long leakedBytes = 0;
            Map<String, Integer> leaksByAllocator = new HashMap<>();

            for (AllocationRecord record : liveAllocations.values()) {
                if (record.isPotentialLeak()) {
                    potentialLeaks++;
                    leakedBytes += record.sizeEstimate;
                    leaksByAllocator.merge(record.allocatorClass, 1, Integer::sum);
                }
            }

            return new LeakStats(
                totalTracked.get(), liveAllocations.size(), potentialLeaks,
                leakedBytes, totalLeaksDetected.get(), totalLeaksResolved.get(),
                leaksByAllocator
            );
        }

        /** Register a leak listener */
        public static void onLeak(Consumer<LeakReport> listener) {
            leakListeners.add(listener);
        }

        /** Get recent leak reports */
        public static List<LeakReport> recentLeaks(int count) {
            List<LeakReport> reports = new ArrayList<>();
            int currentIdx = leakHistoryIndex.get();
            int start = Math.max(0, currentIdx - count);
            for (int i = start; i < currentIdx; i++) {
                LeakReport report = leakHistory[i & (LEAK_HISTORY_SIZE - 1)];
                if (report != null) reports.add(report);
            }
            return reports;
        }

        /** Force a full leak scan NOW */
        public static List<LeakReport> scanNow() {
            drainReferenceQueue();
            return detectLeaks();
        }

        /** Clear all tracking data (reset) */
        public static void reset() {
            liveAllocations.clear();
            totalTracked.set(0);
            totalLeaksDetected.set(0);
            totalLeaksResolved.set(0);
            leakHistoryIndex.set(0);
        }

        /** Shutdown the leak detector daemon */
        public static void shutdown() {
            detectorRunning = false;
            if (detectorThread != null) {
                detectorThread.interrupt();
                detectorThread = null;
            }
        }

        // ── Private ──

        private static void ensureDetectorRunning() {
            if (detectorRunning) return;
            synchronized (LeakDetector.class) {
                if (detectorRunning) return;
                detectorRunning = true;

                // Use virtual thread for the detector daemon
                detectorThread = Thread.ofVirtual()
                    .name("DeepMix-LeakDetector")
                    .start(LeakDetector::detectorLoop);
            }
        }

        private static void detectorLoop() {
            while (detectorRunning) {
                try {
                    // Drain the reference queue (GC'd objects)
                    drainReferenceQueue();

                    // Scan for leaks
                    List<LeakReport> leaks = detectLeaks();
                    for (LeakReport leak : leaks) {
                        // Only report each leak once
                        AllocationRecord record = liveAllocations.get(leak.allocationId);
                        if (record != null) {
                            reportLeak(record, leak.reason);
                            // Don't remove — let the caller explicitly release
                        }
                    }

                    Thread.sleep(LEAK_SCAN_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("[DeepMix:LeakDetector] Error: " + e.getMessage());
                }
            }
        }

        private static void drainReferenceQueue() {
            Reference<?> ref;
            while ((ref = referenceQueue.poll()) != null) {
                if (ref instanceof TrackedReference<?> tracked) {
                    AllocationRecord record = liveAllocations.remove(tracked.allocationId);
                    if (record != null && !record.released) {
                        // Object was GC'd without being explicitly released
                        // This is normal for many objects, but worth noting for tracked resources
                        record.released = true;
                        record.releasedAtMs = System.currentTimeMillis();
                    }
                }
                ref.clear();
            }
        }

        private static void reportLeak(AllocationRecord record, String reason) {
            totalLeaksDetected.incrementAndGet();

            LeakReport report = new LeakReport(
                record.id, record.description, record.sizeEstimate,
                record.ageMs(), record.allocationSite,
                record.threadName, record.allocatorClass, reason);

            // Store in history
            int idx = leakHistoryIndex.getAndIncrement() & (LEAK_HISTORY_SIZE - 1);
            leakHistory[idx] = report;

            // Print warning
            System.err.println("[DeepMix:LeakDetector] ⚠️ POTENTIAL LEAK:");
            System.err.println("  " + report);

            // Notify listeners
            for (Consumer<LeakReport> listener : leakListeners) {
                try { listener.accept(report); }
                catch (Exception e) { /* swallow */ }
            }
        }

        private static void evictOldestTracked() {
            // Find and remove the oldest non-leaked allocation
            AllocationRecord oldest = null;
            Long oldestId = null;

            for (Map.Entry<Long, AllocationRecord> entry : liveAllocations.entrySet()) {
                AllocationRecord record = entry.getValue();
                if (!record.isPotentialLeak()) {
                    if (oldest == null || record.allocatedAtMs < oldest.allocatedAtMs) {
                        oldest = record;
                        oldestId = entry.getKey();
                    }
                }
            }

            if (oldestId != null) {
                liveAllocations.remove(oldestId);
            }
        }

        private static long estimateObjectSize(Object obj) {
            if (obj == null) return 0;
            if (obj instanceof byte[] b) return 16L + b.length;
            if (obj instanceof char[] c) return 16L + c.length * 2L;
            if (obj instanceof int[] i) return 16L + i.length * 4L;
            if (obj instanceof long[] l) return 16L + l.length * 8L;
            if (obj instanceof String s) return 40L + s.length() * 2L;
            if (obj instanceof Collection<?> c) return 64L + c.size() * 32L;
            if (obj instanceof Map<?,?> m) return 64L + m.size() * 64L;
            if (obj instanceof ByteBuffer b) return 48L + b.capacity();
            return 64; // Default estimate
        }
    }

    /** Leak report record */
    public record LeakReport(
        long allocationId,
        String description,
        long sizeEstimate,
        long ageMs,
        StackTraceElement[] allocationSite,
        String threadName,
        String allocatorClass,
        String reason
    ) {
        public String allocationSiteString() {
            if (allocationSite == null || allocationSite.length == 0) return "unknown";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(5, allocationSite.length); i++) {
                if (i > 0) sb.append(" <- ");
                sb.append(allocationSite[i].getClassName())
                  .append(".")
                  .append(allocationSite[i].getMethodName())
                  .append(":")
                  .append(allocationSite[i].getLineNumber());
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return String.format(
                "Leak[id=%d, '%s', ~%dKB, age=%ds, thread=%s, site=%s, reason=%s]",
                allocationId, description, sizeEstimate / 1024,
                ageMs / 1000, threadName, allocationSiteString(), reason);
        }
    }

    /** Leak detection statistics */
    public record LeakStats(
        long totalTracked,
        int currentlyTracked,
        int potentialLeaks,
        long leakedBytes,
        long totalLeaksDetected,
        long totalLeaksResolved,
        Map<String, Integer> leaksByAllocator
    ) {
        @Override
        public String toString() {
            return String.format(
                "LeakStats[tracked=%d, live=%d, leaks=%d (~%dKB), " +
                    "total_detected=%d, resolved=%d, top_allocator=%s]",
                totalTracked, currentlyTracked, potentialLeaks,
                leakedBytes / 1024, totalLeaksDetected, totalLeaksResolved,
                leaksByAllocator.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(e -> e.getKey() + "(" + e.getValue() + ")")
                    .orElse("none"));
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 3: MEMORY HOG DETECTION & THROTTLING                      ║
    // ║                                                                    ║
    // ║  Identifies components that consume disproportionate memory and     ║
    // ║  applies backpressure or forced eviction.                          ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Memory hog detector and throttler.
     *
     * Tracks per-component memory usage and applies throttling when
     * any single component exceeds its fair share.
     */
    public static final class HogDetector {

        /** Per-component memory budget and tracking */
        static final class ComponentBudget {
            final String componentId;
            final AtomicLong currentBytes = new AtomicLong(0);
            final AtomicLong peakBytes = new AtomicLong(0);
            final AtomicLong totalAllocated = new AtomicLong(0);
            final AtomicLong totalFreed = new AtomicLong(0);
            final AtomicInteger allocationCount = new AtomicInteger(0);
            volatile long budgetBytes; // -1 = unlimited
            volatile boolean throttled = false;
            volatile long throttledAtMs = 0;
            volatile String throttleReason = "";

            ComponentBudget(String componentId, long budgetBytes) {
                this.componentId = componentId;
                this.budgetBytes = budgetBytes;
            }

            void recordAllocation(long bytes) {
                long current = currentBytes.addAndGet(bytes);
                totalAllocated.addAndGet(bytes);
                allocationCount.incrementAndGet();

                // Update peak
                long peak;
                do {
                    peak = peakBytes.get();
                    if (current <= peak) break;
                } while (!peakBytes.compareAndSet(peak, current));
            }

            void recordDeallocation(long bytes) {
                currentBytes.addAndGet(-bytes);
                totalFreed.addAndGet(bytes);
            }

            boolean isOverBudget() {
                return budgetBytes > 0 && currentBytes.get() > budgetBytes;
            }

            double budgetUsageRatio() {
                return budgetBytes > 0 ? (double) currentBytes.get() / budgetBytes : 0;
            }
        }

        private static final ConcurrentHashMap<String, ComponentBudget> componentBudgets =
            new ConcurrentHashMap<>();
        private static final AtomicLong globalBudgetBytes = new AtomicLong(
            Runtime.getRuntime().maxMemory() / 4); // 25% of max heap by default
        private static final CopyOnWriteArrayList<Consumer<HogReport>> hogListeners =
            new CopyOnWriteArrayList<>();

        /**
         * Register a component with a memory budget.
         */
        public static void registerComponent(String componentId, long budgetBytes) {
            componentBudgets.put(componentId,
                new ComponentBudget(componentId, budgetBytes));
        }

        /** Register with proportional budget (fraction of global) */
        public static void registerComponent(String componentId, double fraction) {
            long budget = (long) (globalBudgetBytes.get() * fraction);
            registerComponent(componentId, budget);
        }

        /**
         * Record an allocation for a component.
         * Returns false if the component is throttled and should not allocate.
         */
        public static boolean recordAllocation(String componentId, long bytes) {
            ComponentBudget budget = componentBudgets.computeIfAbsent(
                componentId, id -> new ComponentBudget(id, -1));

            if (budget.throttled) {
                return false; // Throttled — reject allocation
            }

            budget.recordAllocation(bytes);

            // Check if this component is now hogging
            if (budget.isOverBudget()) {
                throttleComponent(componentId, "Over budget: " +
                    (budget.currentBytes.get() / 1024) + "KB / " +
                    (budget.budgetBytes / 1024) + "KB");
                return false;
            }

            // Check global proportion
            long totalUsed = componentBudgets.values().stream()
                .mapToLong(b -> b.currentBytes.get()).sum();
            if (totalUsed > 0) {
                double proportion = (double) budget.currentBytes.get() / totalUsed;
                if (proportion > HOG_THRESHOLD && budget.currentBytes.get() > 1024 * 1024) {
                    throttleComponent(componentId,
                        String.format("Hogging %.0f%% of tracked memory", proportion * 100));
                    return false;
                }
            }

            return true;
        }

        /** Record a deallocation for a component */
        public static void recordDeallocation(String componentId, long bytes) {
            ComponentBudget budget = componentBudgets.get(componentId);
            if (budget != null) {
                budget.recordDeallocation(bytes);

                // Un-throttle if back under budget
                if (budget.throttled && !budget.isOverBudget()) {
                    budget.throttled = false;
                    budget.throttleReason = "";
                    System.out.println("[DeepMix:HogDetector] Un-throttled: " + componentId);
                }
            }
        }

        /** Check if a component is currently throttled */
        public static boolean isThrottled(String componentId) {
            ComponentBudget budget = componentBudgets.get(componentId);
            return budget != null && budget.throttled;
        }

        /** Force un-throttle a component */
        public static void unthrottle(String componentId) {
            ComponentBudget budget = componentBudgets.get(componentId);
            if (budget != null) {
                budget.throttled = false;
                budget.throttleReason = "";
            }
        }

        /** Get the top memory consumers */
        public static List<HogReport> topConsumers(int limit) {
            return componentBudgets.values().stream()
                .sorted(Comparator.comparingLong(
                    (ComponentBudget b) -> b.currentBytes.get()).reversed())
                .limit(limit)
                .map(b -> new HogReport(
                    b.componentId, b.currentBytes.get(), b.peakBytes.get(),
                    b.budgetBytes, b.budgetUsageRatio(), b.throttled,
                    b.throttleReason, b.allocationCount.get()))
                .collect(Collectors.toList());
        }

        /** Get full hog detection stats */
        public static HogStats stats() {
            long totalBytes = 0;
            int throttledCount = 0;
            String biggestHog = "";
            long biggestHogBytes = 0;

            for (ComponentBudget budget : componentBudgets.values()) {
                totalBytes += budget.currentBytes.get();
                if (budget.throttled) throttledCount++;
                if (budget.currentBytes.get() > biggestHogBytes) {
                    biggestHogBytes = budget.currentBytes.get();
                    biggestHog = budget.componentId;
                }
            }

            return new HogStats(
                componentBudgets.size(), totalBytes, throttledCount,
                biggestHog, biggestHogBytes, globalBudgetBytes.get());
        }

        /** Register a hog detection listener */
        public static void onHog(Consumer<HogReport> listener) {
            hogListeners.add(listener);
        }

        /** Set global budget */
        public static void setGlobalBudget(long bytes) {
            globalBudgetBytes.set(bytes);
        }

        // ── Private ──

        private static void throttleComponent(String componentId, String reason) {
            ComponentBudget budget = componentBudgets.get(componentId);
            if (budget == null) return;

            budget.throttled = true;
            budget.throttledAtMs = System.currentTimeMillis();
            budget.throttleReason = reason;

            HogReport report = new HogReport(
                componentId, budget.currentBytes.get(), budget.peakBytes.get(),
                budget.budgetBytes, budget.budgetUsageRatio(), true,
                reason, budget.allocationCount.get());

            System.err.println("[DeepMix:HogDetector] 🐷 HOG THROTTLED: " + report);

            for (Consumer<HogReport> listener : hogListeners) {
                try { listener.accept(report); }
                catch (Exception e) { /* swallow */ }
            }
        }
    }

    /** Hog detection report */
    public record HogReport(
        String componentId,
        long currentBytes,
        long peakBytes,
        long budgetBytes,
        double budgetUsageRatio,
        boolean throttled,
        String throttleReason,
        int allocationCount
    ) {
        @Override
        public String toString() {
            return String.format(
                "Hog[%s, current=%dKB, peak=%dKB, budget=%s, usage=%.0f%%, throttled=%b, reason=%s]",
                componentId, currentBytes / 1024, peakBytes / 1024,
                budgetBytes > 0 ? (budgetBytes / 1024) + "KB" : "unlimited",
                budgetUsageRatio * 100, throttled, throttleReason);
        }
    }

    /** Hog detection statistics */
    public record HogStats(
        int trackedComponents,
        long totalTrackedBytes,
        int throttledCount,
        String biggestHog,
        long biggestHogBytes,
        long globalBudget
    ) {
        @Override
        public String toString() {
            return String.format(
                "HogStats[components=%d, total=%dKB, throttled=%d, " +
                    "biggestHog=%s(%dKB), globalBudget=%dMB]",
                trackedComponents, totalTrackedBytes / 1024, throttledCount,
                biggestHog, biggestHogBytes / 1024, globalBudget / (1024 * 1024));
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 4: MEMORY SLOP ANALYZER                                   ║
    // ║                                                                    ║
    // ║  Detects wasted memory from over-allocation, poor data structure   ║
    // ║  choices, excessive padding, and unfilled collections.             ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Memory slop analyzer — identifies wasted space in allocations.
     *
     * "Slop" is memory that's allocated but never used:
     *   - ArrayList with capacity 100 but only 3 elements
     *   - HashMap with 1000 buckets but 5 entries
     *   - byte[] allocated at 64KB but only 2KB used
     *   - StringBuilder allocated at 256 chars, only 10 used
     */
    public static final class SlopAnalyzer {

        /** Slop analysis result for a single object */
        public record SlopReport(
            String objectType,
            long allocatedBytes,
            long usedBytes,
            long wastedBytes,
            double slopRatio,        // wastedBytes / allocatedBytes
            String recommendation,
            String location
        ) {
            public boolean isSignificant() {
                return slopRatio > SLOP_THRESHOLD && wastedBytes > 1024;
            }

            @Override
            public String toString() {
                return String.format(
                    "Slop[%s, alloc=%dB, used=%dB, waste=%dB (%.0f%%), %s]",
                    objectType, allocatedBytes, usedBytes, wastedBytes,
                    slopRatio * 100, recommendation);
            }
        }

        // Track reported slop patterns for aggregation
        private static final ConcurrentHashMap<String, AtomicLong> slopByType =
            new ConcurrentHashMap<>();
        private static final ConcurrentHashMap<String, AtomicInteger> slopCountByType =
            new ConcurrentHashMap<>();
        private static final AtomicLong totalSlopBytes = new AtomicLong(0);

        /**
         * Analyze a collection for slop.
         */
        public static SlopReport analyzeCollection(Collection<?> collection, String location) {
            int size = collection.size();
            long usedBytes;
            long allocatedBytes;
            String type;
            String recommendation;

            if (collection instanceof ArrayList<?> list) {
                type = "ArrayList";
                // ArrayList internal array capacity is usually > size
                int capacity = getArrayListCapacity(list);
                usedBytes = 16L + size * 8L;          // Object header + references
                allocatedBytes = 16L + capacity * 8L;
                recommendation = size < capacity / 4 ?
                    "Call trimToSize() or use List.copyOf()" :
                    "Acceptable";
            } else if (collection instanceof LinkedList<?>) {
                type = "LinkedList";
                // Each node is ~40 bytes, vs 8 bytes in ArrayList
                usedBytes = size * 8L;     // Effective data
                allocatedBytes = size * 40L; // Node overhead
                recommendation = size > 0 ?
                    "Consider ArrayList (5x less memory per element)" :
                    "Empty — remove or lazily initialize";
            } else if (collection instanceof HashSet<?> set) {
                type = "HashSet";
                int capacity = getHashSetCapacity(set);
                usedBytes = 16L + size * 32L;          // entry objects
                allocatedBytes = 16L + capacity * 32L;  // bucket array + entries
                recommendation = size < capacity / 4 ?
                    "Rehash with smaller capacity or use Set.copyOf()" :
                    "Acceptable";
            } else {
                type = collection.getClass().getSimpleName();
                usedBytes = size * 16L;
                allocatedBytes = usedBytes * 2; // Estimate 2x
                recommendation = "Unknown collection type — consider profiling";
            }

            long wastedBytes = allocatedBytes - usedBytes;
            double slopRatio = allocatedBytes > 0 ? (double) wastedBytes / allocatedBytes : 0;

            SlopReport report = new SlopReport(
                type, allocatedBytes, usedBytes, wastedBytes,
                slopRatio, recommendation, location);

            if (report.isSignificant()) {
                recordSlop(type, wastedBytes);
            }

            return report;
        }

        /**
         * Analyze a Map for slop.
         */
        public static SlopReport analyzeMap(Map<?, ?> map, String location) {
            int size = map.size();
            long usedBytes;
            long allocatedBytes;
            String type;
            String recommendation;

            if (map instanceof HashMap<?, ?> hm) {
                type = "HashMap";
                int capacity = getHashMapCapacity(hm);
                usedBytes = 16L + size * 48L;           // Entry objects
                allocatedBytes = 16L + capacity * 8L + size * 48L; // Table + entries
                recommendation = size < capacity / 4 ?
                    "Use Map.copyOf() or construct with proper initial capacity" :
                    "Acceptable";
            } else if (map instanceof ConcurrentHashMap<?, ?>) {
                type = "ConcurrentHashMap";
                usedBytes = 16L + size * 48L;
                allocatedBytes = usedBytes + 4096; // Segments overhead
                recommendation = size == 0 ?
                    "Empty ConcurrentHashMap — lazily initialize" :
                    "Acceptable (concurrent overhead expected)";
            } else if (map instanceof TreeMap<?, ?>) {
                type = "TreeMap";
                usedBytes = size * 8L * 2;    // Keys + values
                allocatedBytes = size * 48L;   // Tree node overhead
                recommendation = size > 1000 ?
                    "Consider HashMap if ordering not needed (3x less overhead)" :
                    "Acceptable";
            } else {
                type = map.getClass().getSimpleName();
                usedBytes = size * 16L;
                allocatedBytes = usedBytes * 2;
                recommendation = "Unknown map type";
            }

            long wastedBytes = Math.max(0, allocatedBytes - usedBytes);
            double slopRatio = allocatedBytes > 0 ? (double) wastedBytes / allocatedBytes : 0;

            SlopReport report = new SlopReport(
                type, allocatedBytes, usedBytes, wastedBytes,
                slopRatio, recommendation, location);

            if (report.isSignificant()) {
                recordSlop(type, wastedBytes);
            }

            return report;
        }

        /**
         * Analyze a byte array for slop.
         *
         * @param buffer      The byte array
         * @param actualUsed  How many bytes are actually used
         * @param location    Allocation site description
         */
        public static SlopReport analyzeBuffer(byte[] buffer, int actualUsed, String location) {
            long wastedBytes = buffer.length - actualUsed;
            double slopRatio = buffer.length > 0 ?
                (double) wastedBytes / buffer.length : 0;

            String recommendation;
            if (slopRatio > 0.75) {
                recommendation = "Severely oversized — allocate " + actualUsed +
                    " bytes instead of " + buffer.length;
            } else if (slopRatio > 0.5) {
                recommendation = "Consider Arrays.copyOf() to right-size";
            } else if (slopRatio > SLOP_THRESHOLD) {
                recommendation = "Moderate waste — consider tighter sizing";
            } else {
                recommendation = "Acceptable";
            }

            SlopReport report = new SlopReport(
                "byte[]", buffer.length, actualUsed, wastedBytes,
                slopRatio, recommendation, location);

            if (report.isSignificant()) {
                recordSlop("byte[]", wastedBytes);
            }

            return report;
        }

        /**
         * Analyze a StringBuilder for slop.
         */
        public static SlopReport analyzeStringBuilder(StringBuilder sb, String location) {
            int used = sb.length() * 2; // chars are 2 bytes
            int allocated = sb.capacity() * 2;
            long wastedBytes = allocated - used;
            double slopRatio = allocated > 0 ? (double) wastedBytes / allocated : 0;

            String recommendation = slopRatio > 0.5 ?
                "Call trimToSize() or use sb.toString() to release buffer" :
                "Acceptable";

            SlopReport report = new SlopReport(
                "StringBuilder", allocated, used, wastedBytes,
                slopRatio, recommendation, location);

            if (report.isSignificant()) {
                recordSlop("StringBuilder", wastedBytes);
            }

            return report;
        }

        /**
         * Get aggregated slop statistics.
         */
        public static SlopStats stats() {
            Map<String, Long> bytesByType = new LinkedHashMap<>();
            Map<String, Integer> countsByType = new LinkedHashMap<>();

            slopByType.forEach((type, bytes) -> bytesByType.put(type, bytes.get()));
            slopCountByType.forEach((type, count) -> countsByType.put(type, count.get()));

            return new SlopStats(totalSlopBytes.get(), bytesByType, countsByType);
        }

        /** Reset slop tracking */
        public static void reset() {
            slopByType.clear();
            slopCountByType.clear();
            totalSlopBytes.set(0);
        }

        /**
         * Compact a collection in-place to eliminate slop.
         * Returns bytes reclaimed.
         */
        public static <T> long compact(ArrayList<T> list) {
            int before = getArrayListCapacity(list);
            list.trimToSize();
            int after = getArrayListCapacity(list);
            long reclaimed = (long)(before - after) * 8;
            if (reclaimed > 0) {
                totalSlopBytes.addAndGet(-reclaimed);
            }
            return reclaimed;
        }

        /**
         * Create a right-sized copy of a collection.
         */
        public static <T> List<T> rightSize(Collection<T> source) {
            return List.copyOf(source); // Immutable, perfectly sized
        }

        /**
         * Create a right-sized copy of a map.
         */
        public static <K, V> Map<K, V> rightSize(Map<K, V> source) {
            return Map.copyOf(source); // Immutable, perfectly sized
        }

        /**
         * Create a right-sized byte array copy.
         */
        public static byte[] rightSize(byte[] source, int usedLength) {
            if (usedLength == source.length) return source;
            return Arrays.copyOf(source, usedLength);
        }

        // ── Private helpers ──

        private static void recordSlop(String type, long wastedBytes) {
            slopByType.computeIfAbsent(type, k -> new AtomicLong(0))
                .addAndGet(wastedBytes);
            slopCountByType.computeIfAbsent(type, k -> new AtomicInteger(0))
                .incrementAndGet();
            totalSlopBytes.addAndGet(wastedBytes);
        }

        @SuppressWarnings("unchecked")
        private static int getArrayListCapacity(ArrayList<?> list) {
            try {
                Field elementData = ArrayList.class.getDeclaredField("elementData");
                elementData.setAccessible(true);
                Object[] data = (Object[]) elementData.get(list);
                return data != null ? data.length : 0;
            } catch (Exception e) {
                return list.size(); // Fallback: assume no slop
            }
        }

        private static int getHashMapCapacity(HashMap<?, ?> map) {
            try {
                Field table = HashMap.class.getDeclaredField("table");
                table.setAccessible(true);
                Object[] buckets = (Object[]) table.get(map);
                return buckets != null ? buckets.length : 0;
            } catch (Exception e) {
                return Math.max(16, map.size() * 2); // Estimate
            }
        }

        private static int getHashSetCapacity(HashSet<?> set) {
            try {
                Field mapField = HashSet.class.getDeclaredField("map");
                mapField.setAccessible(true);
                HashMap<?, ?> map = (HashMap<?, ?>) mapField.get(set);
                return getHashMapCapacity(map);
            } catch (Exception e) {
                return Math.max(16, set.size() * 2);
            }
        }
    }

    /** Slop statistics */
    public record SlopStats(
        long totalSlopBytes,
        Map<String, Long> bytesByType,
        Map<String, Integer> countsByType
    ) {
        @Override
        public String toString() {
            return String.format("SlopStats[total=%dKB, types=%s]",
                totalSlopBytes / 1024, bytesByType);
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 5: DIRECT BUFFER POOL                                     ║
    // ║                                                                    ║
    // ║  Tiered pool for DirectByteBuffers to avoid native allocation       ║
    // ║  overhead and prevent buffer exhaustion.                            ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Tiered direct buffer pool.
     *
     * Tiers:
     *   TINY:   64B  - 1KB    (for small bytecode fragments)
     *   SMALL:  1KB  - 8KB    (for typical class bytecode)
     *   MEDIUM: 8KB  - 64KB   (for large classes)
     *   LARGE:  64KB - 1MB    (for bulk operations)
     *   HUGE:   1MB  - 16MB   (for archive processing)
     */
    public static final class DirectBufferPool {

        public enum Tier {
            TINY(64, 1024, 64),
            SMALL(1024, 8192, 32),
            MEDIUM(8192, 65536, 16),
            LARGE(65536, 1024 * 1024, 8),
            HUGE(1024 * 1024, 16 * 1024 * 1024, 4);

            final int minSize;
            final int maxSize;
            final int poolSize;

            Tier(int minSize, int maxSize, int poolSize) {
                this.minSize = minSize;
                this.maxSize = maxSize;
                this.poolSize = poolSize;
            }

            static Tier forSize(int size) {
                for (Tier tier : values()) {
                    if (size <= tier.maxSize) return tier;
                }
                return HUGE;
            }
        }

        private static final ConcurrentHashMap<Tier, ConcurrentLinkedDeque<ByteBuffer>> pools =
            new ConcurrentHashMap<>();
        private static final AtomicLong totalDirectAllocated = new AtomicLong(0);
        private static final AtomicLong totalDirectReused = new AtomicLong(0);
        private static final AtomicLong totalDirectFreed = new AtomicLong(0);

        // Track all outstanding buffers for leak detection
        private static final ConcurrentHashMap<Integer, BufferLoan> outstandingLoans =
            new ConcurrentHashMap<>();
        private static final AtomicInteger loanIdCounter = new AtomicInteger(0);

        static {
            for (Tier tier : Tier.values()) {
                pools.put(tier, new ConcurrentLinkedDeque<>());
            }
        }

        /** Borrow a direct buffer of at least the given size */
        public static BufferLoan borrow(int minSize) {
            Tier tier = Tier.forSize(minSize);
            ConcurrentLinkedDeque<ByteBuffer> pool = pools.get(tier);

            ByteBuffer buffer = pool.pollFirst();
            boolean reused;

            if (buffer != null && buffer.capacity() >= minSize) {
                buffer.clear();
                buffer.order(ByteOrder.nativeOrder());
                totalDirectReused.incrementAndGet();
                reused = true;
            } else {
                // Allocate at tier's max size (avoid future pool misses)
                int allocSize = Math.max(minSize, tier.maxSize);
                buffer = ByteBuffer.allocateDirect(allocSize);
                buffer.order(ByteOrder.nativeOrder());
                totalDirectAllocated.addAndGet(allocSize);
                reused = false;
            }

            int loanId = loanIdCounter.incrementAndGet();
            BufferLoan loan = new BufferLoan(loanId, buffer, tier, reused,
                System.currentTimeMillis(), Thread.currentThread().getName());
            outstandingLoans.put(loanId, loan);

            return loan;
        }

        /** Return a buffer to the pool */
        public static void returnBuffer(BufferLoan loan) {
            if (loan == null) return;
            outstandingLoans.remove(loan.id);

            Tier tier = loan.tier;
            ConcurrentLinkedDeque<ByteBuffer> pool = pools.get(tier);

            if (pool.size() < tier.poolSize) {
                loan.buffer.clear();
                pool.offerFirst(loan.buffer);
            } else {
                // Pool full — let the buffer be GC'd
                totalDirectFreed.addAndGet(loan.buffer.capacity());
                // For DirectByteBuffer, we can try to explicitly clean it
                cleanDirectBuffer(loan.buffer);
            }
        }

        /** Get pool statistics */
        public static DirectBufferStats stats() {
            Map<Tier, Integer> poolSizes = new EnumMap<>(Tier.class);
            for (Tier tier : Tier.values()) {
                poolSizes.put(tier, pools.get(tier).size());
            }

            return new DirectBufferStats(
                totalDirectAllocated.get(), totalDirectReused.get(),
                totalDirectFreed.get(), outstandingLoans.size(), poolSizes);
        }

        /** Drain all pools — release native memory */
        public static long drainAll() {
            long freed = 0;
            for (Tier tier : Tier.values()) {
                ConcurrentLinkedDeque<ByteBuffer> pool = pools.get(tier);
                ByteBuffer buf;
                while ((buf = pool.pollFirst()) != null) {
                    freed += buf.capacity();
                    cleanDirectBuffer(buf);
                }
            }
            totalDirectFreed.addAndGet(freed);
            return freed;
        }

        /** Check for leaked buffers (borrowed but never returned) */
        public static List<BufferLoan> detectLeakedBuffers(long maxAgeMs) {
            long cutoff = System.currentTimeMillis() - maxAgeMs;
            return outstandingLoans.values().stream()
                .filter(loan -> loan.borrowedAtMs < cutoff)
                .collect(Collectors.toList());
        }

        /** Force-clean a direct buffer using Unsafe or Cleaner */
        private static void cleanDirectBuffer(ByteBuffer buffer) {
            if (!buffer.isDirect()) return;
            try {
                // Java 9+: use sun.misc.Unsafe or jdk.internal.ref.Cleaner
                // Safe approach: invoke the cleaner if available
                Method cleanerMethod = buffer.getClass().getMethod("cleaner");
                cleanerMethod.setAccessible(true);
                Object cleaner = cleanerMethod.invoke(buffer);
                if (cleaner != null) {
                    Method cleanMethod = cleaner.getClass().getMethod("clean");
                    cleanMethod.setAccessible(true);
                    cleanMethod.invoke(cleaner);
                }
            } catch (Exception e) {
                // Fallback: let GC handle it
            }
        }
    }

    /** Direct buffer loan — returned to pool when done */
    public record BufferLoan(
        int id,
        ByteBuffer buffer,
        DirectBufferPool.Tier tier,
        boolean reused,
        long borrowedAtMs,
        String borrowerThread
    ) implements AutoCloseable {

        public long ageMs() {
            return System.currentTimeMillis() - borrowedAtMs;
        }

        @Override
        public void close() {
            DirectBufferPool.returnBuffer(this);
        }

        @Override
        public String toString() {
            return String.format("BufferLoan[id=%d, %s, %dB, reused=%b, age=%dms, thread=%s]",
                id, tier, buffer.capacity(), reused, ageMs(), borrowerThread);
        }
    }

    /** Direct buffer pool statistics */
    public record DirectBufferStats(
        long totalAllocated,
        long totalReused,
        long totalFreed,
        int outstandingLoans,
        Map<DirectBufferPool.Tier, Integer> poolSizes
    ) {
        public double reuseRatio() {
            long total = totalAllocated + totalReused;
            return total > 0 ? (double) totalReused / total : 0;
        }

        @Override
        public String toString() {
            return String.format(
                "DirectBufferStats[alloc=%dKB, reuse=%d (%.0f%%), freed=%dKB, outstanding=%d, pools=%s]",
                totalAllocated / 1024, totalReused, reuseRatio() * 100,
                totalFreed / 1024, outstandingLoans, poolSizes);
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 6: NATIVE MEMORY INTROSPECTION (FFM Linker)               ║
    // ║                                                                    ║
    // ║  Uses Java 25 Foreign Linker to call native C library functions     ║
    // ║  for platform-level memory introspection.                          ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Native memory introspection using Java 25 Foreign Function API.
     *
     * Provides access to:
     *   - malloc/free/realloc for native allocations
     *   - mmap/munmap for memory-mapped regions
     *   - getrusage for process memory stats (Linux/macOS)
     *   - mallinfo for heap statistics (Linux glibc)
     */
    public static final class NativeMemoryIntrospector {

        // Lazy-initialized native function handles
        private static volatile MethodHandle malloc_handle;
        private static volatile MethodHandle free_handle;
        private static volatile MethodHandle realloc_handle;
        private static volatile MethodHandle memset_handle;
        private static volatile MethodHandle memcpy_handle;

        private static final Linker linker = Linker.nativeLinker();
        private static final SymbolLookup stdlib = linker.defaultLookup();

        private static final AtomicLong nativeAllocatedBytes = new AtomicLong(0);
        private static final AtomicLong nativeFreedBytes = new AtomicLong(0);
        private static final AtomicInteger nativeAllocationCount = new AtomicInteger(0);
        private static final ConcurrentHashMap<Long, NativeAllocation> nativeAllocations =
            new ConcurrentHashMap<>();

        /**
         * Allocate native memory via malloc.
         * Returns a MemorySegment backed by native memory.
         * MUST be freed with nativeFree() when done.
         */
        public static MemorySegment nativeMalloc(long size, Arena arena) {
            try {
                MethodHandle mh = getMallocHandle();
                MemorySegment ptr = (MemorySegment) mh.invoke(size);

                if (ptr.equals(MemorySegment.NULL)) {
                    throw new OutOfMemoryError("native malloc(" + size + ") returned NULL");
                }

                // Reinterpret with proper size and scope
                MemorySegment segment = ptr.reinterpret(size, arena, null);

                long address = segment.address();
                nativeAllocatedBytes.addAndGet(size);
                nativeAllocationCount.incrementAndGet();
                nativeAllocations.put(address, new NativeAllocation(
                    address, size, System.currentTimeMillis(),
                    Thread.currentThread().getName()));

                return segment;

            } catch (OutOfMemoryError e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException("native malloc failed", e);
            }
        }

        /**
         * Free native memory.
         */
        public static void nativeFree(MemorySegment segment) {
            if (segment == null || segment.equals(MemorySegment.NULL)) return;

            try {
                long address = segment.address();
                NativeAllocation alloc = nativeAllocations.remove(address);
                if (alloc != null) {
                    nativeFreedBytes.addAndGet(alloc.size);
                }

                MethodHandle mh = getFreeHandle();
                mh.invoke(segment);

            } catch (Throwable e) {
                throw new RuntimeException("native free failed", e);
            }
        }

        /**
         * Zero-fill a memory segment using native memset.
         * Much faster than MemorySegment.fill() for large segments.
         */
        public static void nativeMemset(MemorySegment segment, byte value, long count) {
            try {
                MethodHandle mh = getMemsetHandle();
                mh.invoke(segment, (int) value, count);
            } catch (Throwable e) {
                // Fallback to Java implementation
                segment.fill(value);
            }
        }

        /**
         * Copy memory using native memcpy.
         * Faster than MemorySegment.copyFrom() for large copies.
         */
        public static void nativeMemcpy(MemorySegment dest, MemorySegment src, long count) {
            try {
                MethodHandle mh = getMemcpyHandle();
                mh.invoke(dest, src, count);
            } catch (Throwable e) {
                // Fallback to Java implementation
                MemorySegment.copy(src, 0, dest, 0, count);
            }
        }

        /**
         * Get native memory statistics.
         */
        public static NativeMemoryStats stats() {
            return new NativeMemoryStats(
                nativeAllocatedBytes.get(),
                nativeFreedBytes.get(),
                nativeAllocatedBytes.get() - nativeFreedBytes.get(),
                nativeAllocationCount.get(),
                nativeAllocations.size()
            );
        }

        /**
         * Get process-level memory info from the OS.
         */
        public static ProcessMemoryInfo getProcessMemoryInfo() {
            Runtime rt = Runtime.getRuntime();
            long heapMax = rt.maxMemory();
            long heapTotal = rt.totalMemory();
            long heapFree = rt.freeMemory();
            long heapUsed = heapTotal - heapFree;

            MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage nonHeap = memBean.getNonHeapMemoryUsage();

            // Direct buffer stats from BufferPoolMXBean
            long directUsed = 0;
            long directCapacity = 0;
            long mappedUsed = 0;
            long mappedCapacity = 0;

            for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
                switch (pool.getName()) {
                    case "direct" -> {
                        directUsed = pool.getMemoryUsed();
                        directCapacity = pool.getTotalCapacity();
                    }
                    case "mapped" -> {
                        mappedUsed = pool.getMemoryUsed();
                        mappedCapacity = pool.getTotalCapacity();
                    }
                }
            }

            long nativeLive = nativeAllocatedBytes.get() - nativeFreedBytes.get();
            long offHeapLive = OffHeapManager.liveOffHeapBytes();

            return new ProcessMemoryInfo(
                heapUsed, heapMax, nonHeap.getUsed(),
                directUsed, directCapacity, mappedUsed, mappedCapacity,
                nativeLive, offHeapLive,
                heapUsed + nonHeap.getUsed() + directUsed + nativeLive + offHeapLive
            );
        }

        /** Detect leaked native allocations */
        public static List<NativeAllocation> detectNativeLeaks(long maxAgeMs) {
            long cutoff = System.currentTimeMillis() - maxAgeMs;
            return nativeAllocations.values().stream()
                .filter(a -> a.allocatedAtMs < cutoff)
                .collect(Collectors.toList());
        }

        // ── Lazy handle initialization ──

        private static MethodHandle getMallocHandle() {
            if (malloc_handle == null) {
                synchronized (NativeMemoryIntrospector.class) {
                    if (malloc_handle == null) {
                        malloc_handle = linker.downcallHandle(
                            stdlib.find("malloc").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
                        );
                    }
                }
            }
            return malloc_handle;
        }

        private static MethodHandle getFreeHandle() {
            if (free_handle == null) {
                synchronized (NativeMemoryIntrospector.class) {
                    if (free_handle == null) {
                        free_handle = linker.downcallHandle(
                            stdlib.find("free").orElseThrow(),
                            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
                        );
                    }
                }
            }
            return free_handle;
        }

        private static MethodHandle getMemsetHandle() {
            if (memset_handle == null) {
                synchronized (NativeMemoryIntrospector.class) {
                    if (memset_handle == null) {
                        memset_handle = linker.downcallHandle(
                            stdlib.find("memset").orElseThrow(),
                            FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_INT,
                                ValueLayout.JAVA_LONG)
                        );
                    }
                }
            }
            return memset_handle;
        }

        private static MethodHandle getMemcpyHandle() {
            if (memcpy_handle == null) {
                synchronized (NativeMemoryIntrospector.class) {
                    if (memcpy_handle == null) {
                        memcpy_handle = linker.downcallHandle(
                            stdlib.find("memcpy").orElseThrow(),
                            FunctionDescriptor.of(
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.ADDRESS,
                                ValueLayout.JAVA_LONG)
                        );
                    }
                }
            }
            return memcpy_handle;
        }
    }

    /** Native allocation tracking record */
    public record NativeAllocation(
        long address,
        long size,
        long allocatedAtMs,
        String threadName
    ) {
        public long ageMs() {
            return System.currentTimeMillis() - allocatedAtMs;
        }

        @Override
        public String toString() {
            return String.format("NativeAlloc[0x%x, %dB, age=%dms, thread=%s]",
                address, size, ageMs(), threadName);
        }
    }

    /** Native memory statistics */
    public record NativeMemoryStats(
        long totalAllocated,
        long totalFreed,
        long liveBytes,
        int allocationCount,
        int liveAllocationCount
    ) {
        @Override
        public String toString() {
            return String.format(
                "NativeStats[live=%dKB, allocs=%d, total=%dKB, freed=%dKB]",
                liveBytes / 1024, liveAllocationCount,
                totalAllocated / 1024, totalFreed / 1024);
        }
    }

    /** Process-wide memory information */
    public record ProcessMemoryInfo(
        long heapUsed,
        long heapMax,
        long nonHeapUsed,
        long directUsed,
        long directCapacity,
        long mappedUsed,
        long mappedCapacity,
        long nativeLive,
        long offHeapLive,
        long totalEstimate
    ) {
        @Override
        public String toString() {
            return String.format(
                "ProcessMemory[\n" +
                "  heap:    %6dMB / %dMB\n" +
                "  nonHeap: %6dMB\n" +
                "  direct:  %6dMB (cap: %dMB)\n" +
                "  mapped:  %6dMB (cap: %dMB)\n" +
                "  native:  %6dKB\n" +
                "  offHeap: %6dKB\n" +
                "  TOTAL:   ~%dMB\n]",
                heapUsed / (1024*1024), heapMax / (1024*1024),
                nonHeapUsed / (1024*1024),
                directUsed / (1024*1024), directCapacity / (1024*1024),
                mappedUsed / (1024*1024), mappedCapacity / (1024*1024),
                nativeLive / 1024,
                offHeapLive / 1024,
                totalEstimate / (1024*1024));
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 7: GC-AWARE ADAPTIVE CACHING                             ║
    // ║                                                                    ║
    // ║  Caches that automatically adjust their strength based on          ║
    // ║  GC pressure. Strong → Soft → Weak → Evict as pressure rises.     ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * GC-aware cache that adapts reference strength based on memory pressure.
     *
     * In low pressure:  Entries held as STRONG references (never GC'd)
     * In medium pressure: Entries held as SOFT references (GC'd under pressure)
     * In high pressure: Entries held as WEAK references (GC'd eagerly)
     * In critical:      Cache is cleared entirely
     */
    public static final class AdaptiveCache<K, V> {

        public enum RefStrength { STRONG, SOFT, WEAK }

        private final String name;
        private final int maxStrongEntries;
        private final ConcurrentHashMap<K, Object> entries; // Value or Reference<V>
        private final ReferenceQueue<V> refQueue = new ReferenceQueue<>();
        private volatile RefStrength currentStrength = RefStrength.STRONG;

        // Stats
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong evictions = new AtomicLong(0);
        private final AtomicLong gcReclaimed = new AtomicLong(0);

        // Keyed reference for tracking which key was reclaimed
        private static final class KeyedSoftRef<K, V> extends SoftReference<V> {
            final K key;
            KeyedSoftRef(K key, V value, ReferenceQueue<? super V> queue) {
                super(value, queue);
                this.key = key;
            }
        }

        private static final class KeyedWeakRef<K, V> extends WeakReference<V> {
            final K key;
            KeyedWeakRef(K key, V value, ReferenceQueue<? super V> queue) {
                super(value, queue);
                this.key = key;
            }
        }

        public AdaptiveCache(String name, int maxStrongEntries) {
            this.name = name;
            this.maxStrongEntries = maxStrongEntries;
            this.entries = new ConcurrentHashMap<>(maxStrongEntries);
        }

        /** Put a value into the cache */
        public void put(K key, V value) {
            drainRefQueue();
            adaptStrength();

            Object wrapped = wrapValue(key, value);
            Object old = entries.put(key, wrapped);
            if (old == null && entries.size() > maxStrongEntries * 2) {
                // Evict oldest strong entries by converting to soft
                evictToSoft(entries.size() - maxStrongEntries);
            }
        }

        /** Get a value from the cache */
        @SuppressWarnings("unchecked")
        public V get(K key) {
            drainRefQueue();

            Object raw = entries.get(key);
            if (raw == null) {
                misses.incrementAndGet();
                return null;
            }

            V value = unwrapValue(raw);
            if (value == null) {
                // Reference was cleared by GC
                entries.remove(key);
                gcReclaimed.incrementAndGet();
                misses.incrementAndGet();
                return null;
            }

            hits.incrementAndGet();
            return value;
        }

        /** Get or compute */
        public V computeIfAbsent(K key, Function<K, V> factory) {
            V existing = get(key);
            if (existing != null) return existing;

            V computed = factory.apply(key);
            if (computed != null) {
                put(key, computed);
            }
            return computed;
        }

        /** Remove an entry */
        public V remove(K key) {
            Object raw = entries.remove(key);
            if (raw == null) return null;
            return unwrapValue(raw);
        }

        /** Clear the cache */
        public void clear() {
            evictions.addAndGet(entries.size());
            entries.clear();
        }

        /** Current size (may include GC'd entries) */
        public int size() { return entries.size(); }

        /** Current reference strength */
        public RefStrength strength() { return currentStrength; }

        /** Cache statistics */
        public AdaptiveCacheStats stats() {
            return new AdaptiveCacheStats(
                name, entries.size(), maxStrongEntries,
                currentStrength, hits.get(), misses.get(),
                evictions.get(), gcReclaimed.get());
        }

        // ── Private ──

        private void adaptStrength() {
            // Check memory pressure and adapt
            Runtime rt = Runtime.getRuntime();
            double usageRatio = 1.0 - ((double) rt.freeMemory() / rt.totalMemory());

            RefStrength newStrength;
            if (usageRatio < 0.60) {
                newStrength = RefStrength.STRONG;
            } else if (usageRatio < 0.80) {
                newStrength = RefStrength.SOFT;
            } else {
                newStrength = RefStrength.WEAK;
            }

            if (newStrength != currentStrength) {
                RefStrength oldStrength = currentStrength;
                currentStrength = newStrength;

                // Downgrade existing entries if strength decreased
                if (newStrength.ordinal() > oldStrength.ordinal()) {
                    downgradeEntries(newStrength);
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void downgradeEntries(RefStrength targetStrength) {
            for (Map.Entry<K, Object> entry : entries.entrySet()) {
                Object raw = entry.getValue();
                if (raw != null && !(raw instanceof Reference)) {
                    // Currently strong — downgrade
                    V value = (V) raw;
                    entry.setValue(wrapValue(entry.getKey(), value));
                }
            }
        }

        private Object wrapValue(K key, V value) {
            return switch (currentStrength) {
                case STRONG -> value;
                case SOFT -> new KeyedSoftRef<>(key, value, refQueue);
                case WEAK -> new KeyedWeakRef<>(key, value, refQueue);
            };
        }

        @SuppressWarnings("unchecked")
        private V unwrapValue(Object raw) {
            if (raw instanceof KeyedSoftRef<?, ?> soft) {
                return (V) soft.get();
            } else if (raw instanceof KeyedWeakRef<?, ?> weak) {
                return (V) weak.get();
            } else {
                return (V) raw; // Strong reference
            }
        }

        @SuppressWarnings("unchecked")
        private void drainRefQueue() {
            Reference<? extends V> ref;
            while ((ref = refQueue.poll()) != null) {
                K key = null;
                if (ref instanceof KeyedSoftRef<?, ?> soft) {
                    key = (K) soft.key;
                } else if (ref instanceof KeyedWeakRef<?, ?> weak) {
                    key = (K) weak.key;
                }
                if (key != null) {
                    entries.remove(key);
                    gcReclaimed.incrementAndGet();
                }
                ref.clear();
            }
        }

        private void evictToSoft(int count) {
            int evicted = 0;
            for (Map.Entry<K, Object> entry : entries.entrySet()) {
                if (evicted >= count) break;
                Object raw = entry.getValue();
                if (raw != null && !(raw instanceof Reference)) {
                    @SuppressWarnings("unchecked")
                    V value = (V) raw;
                    entry.setValue(new KeyedSoftRef<>(entry.getKey(), value, refQueue));
                    evicted++;
                }
            }
            evictions.addAndGet(evicted);
        }
    }

    /** Adaptive cache statistics */
    public record AdaptiveCacheStats(
        String name,
        int size,
        int maxStrong,
        AdaptiveCache.RefStrength strength,
        long hits,
        long misses,
        long evictions,
        long gcReclaimed
    ) {
        public double hitRatio() {
            long total = hits + misses;
            return total > 0 ? (double) hits / total : 0;
        }

        @Override
        public String toString() {
            return String.format(
                "AdaptiveCache[%s, size=%d/%d, strength=%s, hit=%.1f%%, gc=%d]",
                name, size, maxStrong, strength, hitRatio() * 100, gcReclaimed);
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 8: MEMORY COMPACTION ENGINE                               ║
    // ║                                                                    ║
    // ║  Defragments internal data structures, right-sizes collections,    ║
    // ║  and reclaims wasted space.                                        ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Memory compaction engine.
     *
     * Performs periodic or on-demand compaction of DeepMix internal
     * data structures to reclaim fragmented memory.
     */
    public static final class CompactionEngine {

        /** Compaction result */
        public record CompactionResult(
            long bytesReclaimed,
            int collectionsCompacted,
            int buffersReclaimed,
            int regionsConsolidated,
            long durationMs,
            String details
        ) {
            @Override
            public String toString() {
                return String.format(
                    "Compaction[reclaimed=%dKB, collections=%d, buffers=%d, " +
                        "regions=%d, duration=%dms]",
                    bytesReclaimed / 1024, collectionsCompacted,
                    buffersReclaimed, regionsConsolidated, durationMs);
            }
        }

        private static final AtomicLong totalBytesReclaimed = new AtomicLong(0);
        private static final AtomicInteger totalCompactions = new AtomicInteger(0);

        /**
         * Run a full compaction pass.
         */
        public static CompactionResult compact() {
            long startMs = System.currentTimeMillis();
            long bytesReclaimed = 0;
            int collectionsCompacted = 0;
            int buffersReclaimed = 0;
            int regionsConsolidated = 0;
            StringBuilder details = new StringBuilder();

            // 1. Compact bytecode cache
            long cacheBefore = DeepMixOptimizer.BytecodeCache.estimatedSize();
            DeepMixOptimizer.BytecodeCache.compact(0.8);
            long cacheReclaimed = cacheBefore - DeepMixOptimizer.BytecodeCache.estimatedSize();
            bytesReclaimed += cacheReclaimed;
            if (cacheReclaimed > 0) {
                details.append("Cache: ").append(cacheReclaimed / 1024).append("KB; ");
            }

            // 2. Drain unused object pools
            int pooledBefore = DeepMixOptimizer.ObjectPool.totalPooledObjects();
            // Drain pools that are over-provisioned
            long poolReclaimed = drainOverProvisionedPools();
            bytesReclaimed += poolReclaimed;
            buffersReclaimed += pooledBefore - DeepMixOptimizer.ObjectPool.totalPooledObjects();
            if (poolReclaimed > 0) {
                details.append("Pools: ").append(poolReclaimed / 1024).append("KB; ");
            }

            // 3. Drain direct buffer pool excess
            long directReclaimed = 0;
            List<BufferLoan> leakedBuffers =
                DirectBufferPool.detectLeakedBuffers(5 * 60 * 1000); // 5 min
            for (BufferLoan loan : leakedBuffers) {
                DirectBufferPool.returnBuffer(loan);
                directReclaimed += loan.buffer().capacity();
                buffersReclaimed++;
            }
            bytesReclaimed += directReclaimed;
            if (directReclaimed > 0) {
                details.append("DirectBuffers: ").append(directReclaimed / 1024).append("KB; ");
            }

            // 4. Close dead off-heap regions
            int regionsBefore = OffHeapManager.activeRegionCount();
            for (String regionId : new ArrayList<>(OffHeapManager.activeRegionIds())) {
                ManagedRegion region = OffHeapManager.getRegion(regionId);
                if (region != null && !region.isAlive()) {
                    OffHeapManager.closeRegion(regionId);
                    regionsConsolidated++;
                }
            }
            if (regionsConsolidated > 0) {
                details.append("Regions: ").append(regionsConsolidated).append(" closed; ");
            }

            // 5. Compact slicing regions that are mostly empty
            for (String regionId : OffHeapManager.activeRegionIds()) {
                ManagedRegion region = OffHeapManager.getRegion(regionId);
                if (region instanceof ManagedRegion.Slicing slicing) {
                    if (slicing.isAlive() && slicing.usageRatio() < 0.1) {
                        // Less than 10% used — could be reset if safe
                        details.append("SlicingRegion '").append(regionId)
                            .append("' is ").append(String.format("%.0f%%", slicing.usageRatio() * 100))
                            .append(" used; ");
                    }
                }
            }

            // 6. Suggest GC if we reclaimed significant memory
            if (bytesReclaimed > 1024 * 1024) { // > 1MB
                System.gc(); // Hint to JVM
            }

            long durationMs = System.currentTimeMillis() - startMs;
            totalBytesReclaimed.addAndGet(bytesReclaimed);
            totalCompactions.incrementAndGet();

            CompactionResult result = new CompactionResult(
                bytesReclaimed, collectionsCompacted, buffersReclaimed,
                regionsConsolidated, durationMs,
                details.length() > 0 ? details.toString() : "Nothing to compact");

            if (bytesReclaimed > 0) {
                System.out.println("[DeepMix:Compaction] " + result);
            }

            return result;
        }

        /**
         * Light compaction — only the cheapest operations.
         */
        public static CompactionResult compactLight() {
            long startMs = System.currentTimeMillis();
            long bytesReclaimed = 0;

            // Only drain reference queues and dead regions
            LeakDetector.drainReferenceQueue();

            for (String regionId : new ArrayList<>(OffHeapManager.activeRegionIds())) {
                ManagedRegion region = OffHeapManager.getRegion(regionId);
                if (region != null && !region.isAlive()) {
                    OffHeapManager.closeRegion(regionId);
                }
            }

            return new CompactionResult(
                bytesReclaimed, 0, 0, 0,
                System.currentTimeMillis() - startMs, "Light compaction");
        }

        /** Total bytes reclaimed across all compactions */
        public static long totalReclaimed() { return totalBytesReclaimed.get(); }

        /** Total compaction count */
        public static int totalCompactions() { return totalCompactions.get(); }

        // ── Private ──

        private static long drainOverProvisionedPools() {
            // If pools are holding more objects than needed, drain excess
            // This is a heuristic — pools auto-manage in ObjectPool
            return 0; // ObjectPool handles this internally
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 9: MEMORY-MAPPED FILE MANAGER                             ║
    // ║                                                                    ║
    // ║  Efficient file I/O via memory-mapped files using Arena API.        ║
    // ║  Zero-copy reads, controlled lifecycle, automatic cleanup.          ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Memory-mapped file manager using Java 25 Arena API.
     * Provides zero-copy file I/O with deterministic cleanup.
     */
    public static final class MappedFileManager {

        private static final ConcurrentHashMap<String, MappedFile> mappedFiles =
            new ConcurrentHashMap<>();

        /** Mapped file handle */
        public record MappedFile(
            String path,
            MemorySegment segment,
            Arena arena,
            long size,
            FileChannel.MapMode mode,
            Instant mappedAt
        ) implements AutoCloseable {

            /** Read bytes from the mapped file */
            public byte[] read(long offset, int length) {
                byte[] data = new byte[length];
                MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset,
                    data, 0, length);
                return data;
            }

            /** Read a single byte */
            public byte readByte(long offset) {
                return segment.get(ValueLayout.JAVA_BYTE, offset);
            }

            /** Read an int */
            public int readInt(long offset) {
                return segment.get(ValueLayout.JAVA_INT, offset);
            }

            /** Read a long */
            public long readLong(long offset) {
                return segment.get(ValueLayout.JAVA_LONG, offset);
            }

            /** Write bytes (only if mapped READ_WRITE) */
            public void write(long offset, byte[] data) {
                MemorySegment.copy(data, 0, segment, ValueLayout.JAVA_BYTE,
                    offset, data.length);
            }

            /** Get a slice of the mapped region */
            public MemorySegment slice(long offset, long length) {
                return segment.asSlice(offset, length);
            }

            @Override
            public void close() {
                if (arena.scope().isAlive()) {
                    arena.close();
                    mappedFiles.remove(path);
                }
            }
        }

        /**
         * Map a file into memory (read-only).
         */
        public static MappedFile mapReadOnly(Path filePath) throws Exception {
            return map(filePath, FileChannel.MapMode.READ_ONLY);
        }

        /**
         * Map a file into memory (read-write).
         */
        public static MappedFile mapReadWrite(Path filePath) throws Exception {
            return map(filePath, FileChannel.MapMode.READ_WRITE);
        }

        /**
         * Map a file into memory with specified mode.
         */
        public static MappedFile map(Path filePath, FileChannel.MapMode mode) throws Exception {
            String pathStr = filePath.toString();

            // Check if already mapped
            MappedFile existing = mappedFiles.get(pathStr);
            if (existing != null && existing.arena.scope().isAlive()) {
                return existing;
            }

            Arena arena = Arena.ofShared();

            try (FileChannel channel = FileChannel.open(filePath,
                    mode == FileChannel.MapMode.READ_WRITE ?
                        EnumSet.of(StandardOpenOption.READ, StandardOpenOption.WRITE) :
                        EnumSet.of(StandardOpenOption.READ))) {

                long size = channel.size();
                MemorySegment segment = channel.map(mode, 0, size, arena);

                MappedFile mapped = new MappedFile(
                    pathStr, segment, arena, size, mode, Instant.now());

                mappedFiles.put(pathStr, mapped);
                OffHeapManager.recordAllocation("mmap:" + pathStr, size);

                return mapped;
            }
        }

        /** Unmap a specific file */
        public static void unmap(String path) {
            MappedFile mapped = mappedFiles.remove(path);
            if (mapped != null) {
                long size = mapped.size;
                mapped.close();
                OffHeapManager.recordDeallocation("mmap:" + path, size);
            }
        }

        /** Unmap all files */
        public static void unmapAll() {
            List<String> paths = new ArrayList<>(mappedFiles.keySet());
            for (String path : paths) {
                unmap(path);
            }
        }

        /** Get all currently mapped files */
        public static Map<String, Long> mappedFileSizes() {
            Map<String, Long> sizes = new LinkedHashMap<>();
            mappedFiles.forEach((path, mapped) -> sizes.put(path, mapped.size));
            return Collections.unmodifiableMap(sizes);
        }

        /** Total mapped bytes */
        public static long totalMappedBytes() {
            return mappedFiles.values().stream().mapToLong(MappedFile::size).sum();
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 10: UNIFIED MEMORY DASHBOARD                              ║
    // ║                                                                    ║
    // ║  Single entry point for all memory diagnostics, combining all      ║
    // ║  subsystems into a comprehensive report.                           ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Unified memory dashboard — complete view of all memory subsystems.
     */
    public static final class MemoryDashboard {

        /** Full memory report across all subsystems */
        public record FullMemoryReport(
            ProcessMemoryInfo processMemory,
            OffHeapStats offHeapStats,
            LeakStats leakStats,
            HogStats hogStats,
            SlopStats slopStats,
            DirectBufferStats directBufferStats,
            NativeMemoryStats nativeStats,
            List<LeakReport> activeLeaks,
            List<HogReport> topHogs,
            long mappedFileBytes,
            CompactionEngine.CompactionResult lastCompaction,
            Instant timestamp
        ) {
            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("\n╔══════════════════════════════════════════════════════════╗\n");
                sb.append("║  🔮 DeepMix Memory Dashboard                            ║\n");
                sb.append("║  ").append(timestamp).append("                       ║\n");
                sb.append("╠══════════════════════════════════════════════════════════╣\n");

                sb.append("║ PROCESS MEMORY:                                          ║\n");
                sb.append("║   ").append(processMemory).append("\n");

                sb.append("║ OFF-HEAP (Foreign Memory API):                            ║\n");
                sb.append("║   ").append(offHeapStats).append("\n");

                sb.append("║ LEAK DETECTION:                                           ║\n");
                sb.append("║   ").append(leakStats).append("\n");
                if (!activeLeaks.isEmpty()) {
                    sb.append("║   ⚠️ Active leaks:\n");
                    for (LeakReport leak : activeLeaks.subList(0,
                            Math.min(5, activeLeaks.size()))) {
                        sb.append("║     ").append(leak).append("\n");
                    }
                }

                sb.append("║ HOG DETECTION:                                            ║\n");
                sb.append("║   ").append(hogStats).append("\n");
                if (!topHogs.isEmpty()) {
                    sb.append("║   Top consumers:\n");
                    for (HogReport hog : topHogs.subList(0,
                            Math.min(5, topHogs.size()))) {
                        sb.append("║     ").append(hog).append("\n");
                    }
                }

                sb.append("║ SLOP ANALYSIS:                                            ║\n");
                sb.append("║   ").append(slopStats).append("\n");

                sb.append("║ DIRECT BUFFERS:                                           ║\n");
                sb.append("║   ").append(directBufferStats).append("\n");

                sb.append("║ NATIVE MEMORY:                                            ║\n");
                sb.append("║   ").append(nativeStats).append("\n");

                sb.append("║ MAPPED FILES: ").append(mappedFileBytes / 1024).append("KB");
                sb.append("                                          ║\n");

                if (lastCompaction != null) {
                    sb.append("║ LAST COMPACTION:                                          ║\n");
                    sb.append("║   ").append(lastCompaction).append("\n");
                }

                sb.append("╚══════════════════════════════════════════════════════════╝\n");
                return sb.toString();
            }
        }

        /** Generate a full memory report */
        public static FullMemoryReport report() {
            return new FullMemoryReport(
                NativeMemoryIntrospector.getProcessMemoryInfo(),
                OffHeapManager.stats(),
                LeakDetector.stats(),
                HogDetector.stats(),
                SlopAnalyzer.stats(),
                DirectBufferPool.stats(),
                NativeMemoryIntrospector.stats(),
                LeakDetector.detectLeaks(),
                HogDetector.topConsumers(10),
                MappedFileManager.totalMappedBytes(),
                null, // lastCompaction from CompactionEngine
                Instant.now()
            );
        }

        /** Print the dashboard to System.out */
        public static void print() {
            System.out.println(report());
        }

        /** Quick health assessment */
        public static String quickHealth() {
            ProcessMemoryInfo mem = NativeMemoryIntrospector.getProcessMemoryInfo();
            LeakStats leaks = LeakDetector.stats();
            HogStats hogs = HogDetector.stats();
            OffHeapStats offHeap = OffHeapManager.stats();

            StringBuilder status = new StringBuilder();
            boolean healthy = true;

            // Check heap pressure
            double heapRatio = mem.heapMax() > 0 ?
                (double) mem.heapUsed() / mem.heapMax() : 0;
            if (heapRatio > 0.90) {
                status.append("🔴 CRITICAL heap: ").append(String.format("%.0f%%", heapRatio * 100));
                healthy = false;
            } else if (heapRatio > 0.75) {
                status.append("🟡 Elevated heap: ").append(String.format("%.0f%%", heapRatio * 100));
                healthy = false;
            }

            // Check leaks
            if (leaks.potentialLeaks() > 0) {
                if (status.length() > 0) status.append(" | ");
                status.append("⚠️ ").append(leaks.potentialLeaks())
                    .append(" potential leaks (~")
                    .append(leaks.leakedBytes() / 1024).append("KB)");
                healthy = false;
            }

            // Check hogs
            if (hogs.throttledCount() > 0) {
                if (status.length() > 0) status.append(" | ");
                status.append("🐷 ").append(hogs.throttledCount()).append(" throttled hogs");
                healthy = false;
            }

            // Check off-heap
            if (offHeap.usageRatio() > 0.80) {
                if (status.length() > 0) status.append(" | ");
                status.append("📦 Off-heap ").append(String.format("%.0f%%", offHeap.usageRatio() * 100));
                healthy = false;
            }

            // Check native leaks
            List<NativeAllocation> nativeLeaks =
                NativeMemoryIntrospector.detectNativeLeaks(LEAK_THRESHOLD_MS);
            if (!nativeLeaks.isEmpty()) {
                if (status.length() > 0) status.append(" | ");
                status.append("🔓 ").append(nativeLeaks.size()).append(" native leaks");
                healthy = false;
            }

            // Check direct buffer leaks
            List<BufferLoan> bufferLeaks =
                DirectBufferPool.detectLeakedBuffers(3 * 60 * 1000); // 3 min
            if (!bufferLeaks.isEmpty()) {
                if (status.length() > 0) status.append(" | ");
                status.append("💾 ").append(bufferLeaks.size()).append(" leaked buffers");
                healthy = false;
            }

            if (healthy) {
                return "✅ Memory healthy — heap " +
                    String.format("%.0f%%", heapRatio * 100) +
                    ", off-heap " + (offHeap.liveBytes() / 1024) + "KB" +
                    ", no leaks detected";
            }

            return status.toString();
        }

        /**
         * Run automatic memory optimization.
         * Performs leak cleanup, compaction, and pool balancing.
         * Returns total bytes reclaimed.
         */
        public static long autoOptimize() {
            long totalReclaimed = 0;
            StringBuilder log = new StringBuilder("[DeepMix:MemoryOptimizer] Auto-optimize:\n");

            // 1. Drain reference queues (resolve phantom refs)
            LeakDetector.drainReferenceQueue();

            // 2. Close dead off-heap regions
            int deadRegions = 0;
            for (String regionId : new ArrayList<>(OffHeapManager.activeRegionIds())) {
                ManagedRegion region = OffHeapManager.getRegion(regionId);
                if (region != null && !region.isAlive()) {
                    OffHeapManager.closeRegion(regionId);
                    deadRegions++;
                }
            }
            if (deadRegions > 0) {
                log.append("  Closed ").append(deadRegions).append(" dead regions\n");
            }

            // 3. Reclaim leaked direct buffers
            List<BufferLoan> leakedBuffers =
                DirectBufferPool.detectLeakedBuffers(5 * 60 * 1000);
            for (BufferLoan loan : leakedBuffers) {
                totalReclaimed += loan.buffer().capacity();
                DirectBufferPool.returnBuffer(loan);
            }
            if (!leakedBuffers.isEmpty()) {
                log.append("  Reclaimed ").append(leakedBuffers.size())
                    .append(" leaked buffers (").append(totalReclaimed / 1024).append("KB)\n");
            }

            // 4. Run compaction
            CompactionEngine.CompactionResult compaction = CompactionEngine.compact();
            totalReclaimed += compaction.bytesReclaimed();
            if (compaction.bytesReclaimed() > 0) {
                log.append("  Compaction: ").append(compaction).append("\n");
            }

            // 5. Right-size slop if significant
            SlopStats slop = SlopAnalyzer.stats();
            if (slop.totalSlopBytes() > 1024 * 1024) { // > 1MB slop
                log.append("  ⚠️ Significant slop detected: ")
                    .append(slop.totalSlopBytes() / 1024).append("KB\n");
                // Can't fix slop automatically without references to the objects
                // Log recommendation instead
                slop.bytesByType().entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .forEach(e -> log.append("    Top slop: ").append(e.getKey())
                        .append(" = ").append(e.getValue() / 1024).append("KB\n"));
            }

            // 6. Un-throttle components that are now under budget
            for (HogReport hog : HogDetector.topConsumers(100)) {
                if (hog.throttled() && !hog.componentId().isEmpty()) {
                    // Re-check budget
                    if (hog.budgetUsageRatio() < 0.8) {
                        HogDetector.unthrottle(hog.componentId());
                        log.append("  Un-throttled: ").append(hog.componentId()).append("\n");
                    }
                }
            }

            // 7. If we're under high memory pressure, take aggressive action
            ProcessMemoryInfo memInfo = NativeMemoryIntrospector.getProcessMemoryInfo();
            double heapRatio = memInfo.heapMax() > 0 ?
                (double) memInfo.heapUsed() / memInfo.heapMax() : 0;

            if (heapRatio > 0.85) {
                log.append("  ⚠️ High heap pressure (").append(String.format("%.0f%%", heapRatio * 100))
                    .append(") — aggressive cleanup\n");

                // Drain all pools
                DeepMixOptimizer.ObjectPool.drainAll();
                log.append("  Drained object pools\n");

                // Drain excess direct buffers
                long directFreed = DirectBufferPool.drainAll();
                totalReclaimed += directFreed;
                if (directFreed > 0) {
                    log.append("  Drained direct buffers: ")
                        .append(directFreed / 1024).append("KB\n");
                }

                // Compact bytecode cache aggressively
                long cacheBefore = DeepMixOptimizer.BytecodeCache.estimatedSize();
                DeepMixOptimizer.BytecodeCache.compact(0.3); // Keep 30%
                long cacheFreed = cacheBefore - DeepMixOptimizer.BytecodeCache.estimatedSize();
                totalReclaimed += cacheFreed;
                if (cacheFreed > 0) {
                    log.append("  Cache compacted: ")
                        .append(cacheFreed / 1024).append("KB freed\n");
                }

                // Hint GC
                System.gc();
                log.append("  GC suggested\n");
            }

            if (totalReclaimed > 0) {
                log.append("  TOTAL RECLAIMED: ").append(totalReclaimed / 1024).append("KB\n");
                System.out.println(log);
            }

            return totalReclaimed;
        }

        /**
         * Schedule periodic auto-optimization using a virtual thread.
         *
         * @param intervalMs How often to run auto-optimization
         * @return A handle to cancel the schedule
         */
        public static AutoCloseable scheduleAutoOptimize(long intervalMs) {
            final AtomicBoolean running = new AtomicBoolean(true);

            Thread vthread = Thread.ofVirtual()
                .name("DeepMix-MemoryAutoOptimizer")
                .start(() -> {
                    while (running.get()) {
                        try {
                            Thread.sleep(intervalMs);
                            if (!running.get()) break;

                            long reclaimed = autoOptimize();
                            if (reclaimed > 100_000) { // > 100KB
                                System.out.println(
                                    "[DeepMix:MemoryOptimizer] Auto-optimization reclaimed " +
                                        (reclaimed / 1024) + "KB");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        } catch (Exception e) {
                            System.err.println(
                                "[DeepMix:MemoryOptimizer] Auto-optimize error: " +
                                    e.getMessage());
                        }
                    }
                });

            return () -> {
                running.set(false);
                vthread.interrupt();
            };
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 11: MEMORY-SAFE WRAPPERS                                  ║
    // ║                                                                    ║
    // ║  Convenience wrappers that make common operations memory-safe      ║
    // ║  by integrating leak tracking, budgets, and auto-cleanup.          ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Memory-safe wrapper utilities.
     * Every allocation through these methods is tracked, bounded, and auto-cleaned.
     */
    public static final class SafeMemory {

        /**
         * Allocate a tracked byte array with automatic leak detection.
         */
        public static byte[] allocBytes(int size, String purpose) {
            if (!HogDetector.recordAllocation(purpose, size)) {
                throw new OutOfMemoryError(
                    "[DeepMix:SafeMemory] Component '" + purpose +
                        "' is throttled — allocation rejected");
            }

            byte[] data = new byte[size];
            LeakDetector.track(data, purpose + " byte[" + size + "]", size);
            return data;
        }

        /**
         * Allocate a tracked ArrayList with right-sized initial capacity.
         */
        public static <T> ArrayList<T> allocList(int expectedSize, String purpose) {
            // Avoid ArrayList's default 10-element over-allocation for small lists
            int capacity = expectedSize <= 0 ? 0 : expectedSize;
            ArrayList<T> list = new ArrayList<>(capacity);

            long estimated = 16L + capacity * 8L;
            HogDetector.recordAllocation(purpose, estimated);
            LeakDetector.track(list, purpose + " ArrayList[" + capacity + "]", estimated);
            return list;
        }

        /**
         * Allocate a tracked HashMap with right-sized initial capacity.
         */
        public static <K, V> HashMap<K, V> allocMap(int expectedSize, String purpose) {
            // HashMap load factor = 0.75, so capacity = expected / 0.75
            int capacity = expectedSize <= 0 ? 0 :
                (int) Math.ceil(expectedSize / 0.75) + 1;
            HashMap<K, V> map = new HashMap<>(capacity);

            long estimated = 16L + capacity * 32L;
            HogDetector.recordAllocation(purpose, estimated);
            LeakDetector.track(map, purpose + " HashMap[" + capacity + "]", estimated);
            return map;
        }

        /**
         * Allocate a tracked StringBuilder with right-sized capacity.
         */
        public static StringBuilder allocStringBuilder(int expectedLength, String purpose) {
            StringBuilder sb = new StringBuilder(expectedLength);
            long estimated = 16L + expectedLength * 2L;
            HogDetector.recordAllocation(purpose, estimated);
            LeakDetector.track(sb, purpose + " StringBuilder[" + expectedLength + "]", estimated);
            return sb;
        }

        /**
         * Execute code within a scoped memory region.
         * All off-heap allocations within the scope are automatically freed on exit.
         *
         * Example:
         * <pre>
         * SafeMemory.scoped("bytecodeTransform", region -> {
         *     MemorySegment buffer = region.allocate(classBytes.length);
         *     buffer.copyFrom(MemorySegment.ofArray(classBytes));
         *     // ... process ...
         *     // buffer is automatically freed when this lambda returns
         * });
         * </pre>
         */
        public static void scoped(String name, Consumer<ManagedRegion> action) {
            OffHeapManager.withConfinedRegion(name, action);
        }

        /**
         * Execute code within a scoped memory region, returning a result.
         */
        public static <T> T scoped(String name, Function<ManagedRegion, T> action) {
            return OffHeapManager.withConfinedRegion(name, action);
        }

        /**
         * Borrow a direct buffer, auto-returned when the lambda completes.
         */
        public static <T> T withDirectBuffer(int minSize, Function<ByteBuffer, T> action) {
            try (BufferLoan loan = DirectBufferPool.borrow(minSize)) {
                return action.apply(loan.buffer());
            }
        }

        /**
         * Borrow a direct buffer for void operations.
         */
        public static void withDirectBuffer(int minSize, Consumer<ByteBuffer> action) {
            try (BufferLoan loan = DirectBufferPool.borrow(minSize)) {
                action.accept(loan.buffer());
            }
        }

        /**
         * Create an immutable, right-sized copy of a list (zero slop).
         */
        public static <T> List<T> freeze(List<T> mutableList) {
            return List.copyOf(mutableList);
        }

        /**
         * Create an immutable, right-sized copy of a map (zero slop).
         */
        public static <K, V> Map<K, V> freeze(Map<K, V> mutableMap) {
            return Map.copyOf(mutableMap);
        }

        /**
         * Create an immutable, right-sized copy of a set (zero slop).
         */
        public static <T> Set<T> freeze(Set<T> mutableSet) {
            return Set.copyOf(mutableSet);
        }

        /**
         * Right-size a byte array to its actual used length.
         */
        public static byte[] rightSize(byte[] source, int usedLength) {
            return SlopAnalyzer.rightSize(source, usedLength);
        }

        /**
         * Compact an ArrayList in-place, eliminating slop.
         * Returns bytes reclaimed.
         */
        public static <T> long compact(ArrayList<T> list) {
            return SlopAnalyzer.compact(list);
        }

        /**
         * Release a tracked allocation explicitly.
         * Call this when you're done with an object allocated through SafeMemory.
         */
        public static void release(long trackingId, String componentId, long bytes) {
            LeakDetector.release(trackingId);
            HogDetector.recordDeallocation(componentId, bytes);
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 12: BOOTSTRAP & CONFIGURATION                             ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Initialize the memory optimizer engine.
     * Call this ONCE at startup, after DeepMixOptimizer.bootstrap().
     */
    public static void initialize() {
        if (initialized) return;
        synchronized (initLock) {
            if (initialized) return;

            long startNanos = System.nanoTime();

            System.out.println(
                "╔══════════════════════════════════════════════════════╗\n" +
                "║  🧠 DeepMix Memory Optimizer — Initializing...      ║\n" +
                "╚══════════════════════════════════════════════════════╝");

            // Register default component budgets
            long maxHeap = Runtime.getRuntime().maxMemory();
            HogDetector.setGlobalBudget(maxHeap / 4); // 25% of max heap

            HogDetector.registerComponent("bytecodeCache", 0.10);
            HogDetector.registerComponent("objectPools", 0.05);
            HogDetector.registerComponent("transformPipeline", 0.15);
            HogDetector.registerComponent("offHeapRegions", 0.20);
            HogDetector.registerComponent("directBuffers", 0.10);
            HogDetector.registerComponent("moduleInstances", 0.15);
            HogDetector.registerComponent("leakTracker", 0.02);
            HogDetector.registerComponent("general", 0.23);

            // Start leak detector daemon (virtual thread)
            LeakDetector.ensureDetectorRunning();

            // Schedule periodic auto-optimization (every 60 seconds)
            try {
                autoOptimizeHandle = MemoryDashboard.scheduleAutoOptimize(60_000);
            } catch (Exception e) {
                System.err.println(
                    "[DeepMix:MemoryOptimizer] Failed to schedule auto-optimize: " +
                        e.getMessage());
            }

            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            initialized = true;

            System.out.println(String.format(
                "╔══════════════════════════════════════════════════════╗\n" +
                "║  🧠 DeepMix Memory Optimizer — Ready! (%3dms)       ║\n" +
                "║  Max heap: %4dMB | Off-heap budget: %4dMB          ║\n" +
                "║  Leak detection: ON (%.0f%% sample rate)              ║\n" +
                "║  Hog detection: ON (%d components budgeted)          ║\n" +
                "║  Auto-optimize: every 60s (virtual thread)           ║\n" +
                "╚══════════════════════════════════════════════════════╝",
                durationMs,
                maxHeap / (1024 * 1024),
                MAX_OFFHEAP_BYTES / (1024 * 1024),
                ALLOCATION_SAMPLE_RATE * 100,
                8 // Number of default component budgets
            ));
        }
    }

    private static volatile AutoCloseable autoOptimizeHandle = null;

    /**
     * Gracefully shutdown the memory optimizer.
     */
    public static void shutdown() {
        if (!initialized) return;
        synchronized (initLock) {
            if (!initialized) return;

            System.out.println("[DeepMix:MemoryOptimizer] Shutting down...");

            // Stop auto-optimizer
            if (autoOptimizeHandle != null) {
                try { autoOptimizeHandle.close(); }
                catch (Exception e) { /* swallow */ }
            }

            // Stop leak detector
            LeakDetector.shutdown();

            // Final memory report
            System.out.println(MemoryDashboard.quickHealth());

            // Close all off-heap regions
            OffHeapManager.closeAllRegions();

            // Drain all direct buffer pools
            long directFreed = DirectBufferPool.drainAll();
            if (directFreed > 0) {
                System.out.println("[DeepMix:MemoryOptimizer] Released " +
                    (directFreed / 1024) + "KB direct buffers");
            }

            // Unmap all files
            MappedFileManager.unmapAll();

            // Final stats
            System.out.println("[DeepMix:MemoryOptimizer] Final stats:");
            System.out.println("  " + OffHeapManager.stats());
            System.out.println("  " + LeakDetector.stats());
            System.out.println("  " + HogDetector.stats());
            System.out.println("  " + SlopAnalyzer.stats());
            System.out.println("  " + DirectBufferPool.stats());
            System.out.println("  " + NativeMemoryIntrospector.stats());
            System.out.println("  Compactions: " + CompactionEngine.totalCompactions() +
                " (reclaimed " + (CompactionEngine.totalReclaimed() / 1024) + "KB total)");

            initialized = false;
            System.out.println("[DeepMix:MemoryOptimizer] Shutdown complete.");
        }
    }

    /**
     * Configure the memory optimizer.
     */
    public static void configure(
            long maxOffHeapBytes,
            long leakScanIntervalMs,
            long leakThresholdMs,
            double allocationSampleRate) {
        MAX_OFFHEAP_BYTES = maxOffHeapBytes;
        LEAK_SCAN_INTERVAL_MS = leakScanIntervalMs;
        LEAK_THRESHOLD_MS = leakThresholdMs;
        ALLOCATION_SAMPLE_RATE = Math.max(0.0, Math.min(1.0, allocationSampleRate));
    }

    /** Check if the memory optimizer is initialized */
    public static boolean isInitialized() {
        return initialized;
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION SUMMARY                                                   ║
    // ║                                                                    ║
    // ║   1. Off-Heap Memory Management (Foreign Memory API / Arena)        ║
    // ║   2. Leak Detection Engine (PhantomReference tracking)              ║
    // ║   3. Memory Hog Detection & Throttling                             ║
    // ║   4. Memory Slop Analyzer (wasted space detection)                 ║
    // ║   5. Direct Buffer Pool (tiered, lifecycle-managed)                ║
    // ║   6. Native Memory Introspection (FFM Linker)                     ║
    // ║   7. GC-Aware Adaptive Caching (Strong/Soft/Weak auto-switch)     ║
    // ║   8. Memory Compaction Engine                                      ║
    // ║   9. Memory-Mapped File Manager                                    ║
    // ║  10. Unified Memory Dashboard                                      ║
    // ║  11. Memory-Safe Wrappers                                          ║
    // ║  12. Bootstrap & Configuration                                     ║
    // ║                                                                    ║
    // ║  Java 25 Features Used:                                            ║
    // ║    ✅ Arena (ofConfined, ofShared, ofAuto)                          ║
    // ║    ✅ MemorySegment (allocate, asSlice, fill, copy)                 ║
    // ║    ✅ MemoryLayout / ValueLayout                                    ║
    // ║    ✅ Linker.nativeLinker() + downcallHandle                        ║
    // ║    ✅ FunctionDescriptor (malloc, free, memset, memcpy)             ║
    // ║    ✅ SymbolLookup (defaultLookup)                                  ║
    // ║    ✅ Virtual Threads (Thread.ofVirtual)                            ║
    // ║    ✅ Sealed interfaces (ManagedRegion)                             ║
    // ║    ✅ Records (immutable data carriers throughout)                  ║
    // ║    ✅ Record patterns (instanceof ManagedRegion.Slicing slicing)    ║
    // ║    ✅ Switch expressions (wrapValue)                                ║
    // ║    ✅ Pattern matching instanceof                                   ║
    // ║    ✅ FileChannel.map with Arena                                    ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝
}
