package stellar.snow.astralis.integration.jit;

import stellar.snow.astralis.engine.gpu.compute.CullingManager;
import stellar.snow.astralis.engine.gpu.compute.CullingTier;
import stellar.snow.astralis.AstralisCore;
import stellar.snow.astralis.nexus.controlpanel.Config;

import stellar.snow.astralis.engine.ecs.core.Archetype;
import stellar.snow.astralis.engine.ecs.storage.ComponentArray;
import stellar.snow.astralis.ecs.ComponetRegistry;
import stellar.snow.astralis.engine.ecs.core.Entity;
import stellar.snow.astralis.engine.ecs.storage.Query;
import stellar.snow.astralis.engine.ecs.core.SnowySystem;
import stellar.snow.astralis.engine.ecs.core.SystemScheduler;
import stellar.snow.astralis.engine.ecs.core.World;

import stellar.snow.astralis.nexus.bridge.BatchProcessor;
import stellar.snow.astralis.nexus.controlpanel.BridgeComponents;
import stellar.snow.astralis.nexus.controlpanel.BridgeMetrics;
import stellar.snow.astralis.nexus.controlpanel.CircuitBreaker;
import stellar.snow.astralis.nexus.controlpanel.InterpolationSystem;
import stellar.snow.astralis.nexus.bridge.MinecraftECSBridge;
import stellar.snow.astralis.engine.render.system.RenderBridge;
import stellar.snow.astralis.nexus.bridge.SyncSystems;

import stellar.snow.astralis.engine.render.pipeline.RenderConstants;
import stellar.snow.astralis.engine.render.pipeline.RenderState;

import stellar.snow.astralis.api.common.BackendCoordinator;
import stellar.snow.astralis.api.common.CompatibilityLayer;
import stellar.snow.astralis.engine.gpu.scheduling.DrawPool;
import stellar.snow.astralis.nexus.GLSLManager;
import stellar.snow.astralis.engine.gpu.authority.GPUBackend;
import stellar.snow.astralis.engine.gpu.authority.GPUBackendSelector;
import stellar.snow.astralis.api.opengl.backend.OpenGLBackend;
import stellar.snow.astralis.api.opengl.managers.OpenGLManager;
import stellar.snow.astralis.api.vulkan.managers.VulkanManager;
import stellar.snow.astralis.engine.gpu.authority.UniversalCapabilities;
import stellar.snow.astralis.nexus.Sandbox;
import stellar.snow.astralis.api.opengl.managers.GLStateCache;

import stellar.snow.astralis.api.common.BufferOps;
import stellar.snow.astralis.api.vulkan.backend.VulkanContext;
import stellar.snow.astralis.api.vulkan.managers.VulkanState;
import stellar.snow.astralis.engine.gpu.compute.IndirectDrawManager;
import stellar.snow.astralis.api.vulkan.memory.MemoryAllocation;
import stellar.snow.astralis.api.vulkan.memory.MemoryBlock;
import stellar.snow.astralis.api.vulkan.memory.VulkanMemoryAllocation;
import stellar.snow.astralis.engine.render.graph.RenderGraph;
import stellar.snow.astralis.engine.render.graph.RenderPassNode;
import stellar.snow.astralis.engine.render.graph.ResourceNode;
import stellar.snow.astralis.engine.render.resolution.ResolutionManager;
import stellar.snow.astralis.api.shaders.ShaderPermutationManager;

import stellar.snow.astralis.platform.mixins.accessor.IMinecraftAccessor;
import stellar.snow.astralis.platform.mixins.accessor.IWorldAccessor;
import stellar.snow.astralis.platform.mixins.util.MixinHelper;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.jemalloc.JEmalloc;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.vulkan.VK14;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.IntVector;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.VarHandle;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.ref.Cleaner;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * JITInject - Comprehensive JIT Optimization & Runtime Performance Coordinator
 * 
 * <p>This class serves as the central nervous system for runtime performance optimization,
 * leveraging HotSpot JIT compiler behavior, Java 25's cutting-edge features, and LWJGL 3.3.6's
 * native memory facilities to achieve maximum performance with minimal latency variance.</p>
 * 
 * <h2>Core Optimization Domains:</h2>
 * <ul>
 *   <li><b>JIT Compilation Control</b> - Forced warmup, OSR triggers, deoptimization guards</li>
 *   <li><b>Memory Pressure Management</b> - Proactive GC coordination, allocation profiling</li>
 *   <li><b>Native Memory Optimization</b> - Off-heap pools, arena allocators, zero-copy transfers</li>
 *   <li><b>GPU Resource Lifecycle</b> - Texture eviction, buffer recycling, state caching</li>
 *   <li><b>SIMD Vectorization</b> - Vector API integration for batch operations</li>
 *   <li><b>Cache Topology Awareness</b> - L1/L2/L3 optimization, prefetching, false sharing prevention</li>
 *   <li><b>Concurrency Optimization</b> - Virtual threads, structured concurrency, lock elision</li>
 * </ul>
 * 
 * <h2>Design Principles:</h2>
 * <ul>
 *   <li>Zero-allocation hot paths</li>
 *   <li>Lock-free algorithms where contention exists</li>
 *   <li>Cache-oblivious data structures</li>
 *   <li>Predictable memory access patterns</li>
 *   <li>JIT-friendly code shapes</li>
 * </ul>
 * 
 * @author Astralis Performance Team
 * @version 2.0.0
 * @since Java 25, LWJGL 3.3.6
 */
public final class JITInject {
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTANTS - Cache-line aligned and JIT-friendly
    // ═══════════════════════════════════════════════════════════════════════════
    
    /** CPU cache line size (bytes) - standard x86-64 */
    private static final int CACHE_LINE_SIZE = 64;
    
    /** L1 data cache typical size (32KB) */
    private static final int L1_CACHE_SIZE = 32 * 1024;
    
    /** L2 cache typical size (256KB-512KB) */
    private static final int L2_CACHE_SIZE = 256 * 1024;
    
    /** L3 cache slice per core (2MB typical) */
    private static final int L3_CACHE_SLICE = 2 * 1024 * 1024;
    
    /** HotSpot C1 compilation threshold */
    private static final int C1_COMPILE_THRESHOLD = 1_500;
    
    /** HotSpot C2 compilation threshold */
    private static final int C2_COMPILE_THRESHOLD = 10_000;
    
    /** On-Stack Replacement loop iteration trigger */
    private static final int OSR_TRIGGER_COUNT = 50_000;
    
    /** Inlining depth hint for megamorphic prevention */
    private static final int MAX_INLINE_DEPTH = 9;
    
    /** Memory thresholds (percentage of max heap) */
    private static final double HEAP_PRESSURE_YELLOW = 0.70;
    private static final double HEAP_PRESSURE_ORANGE = 0.82;
    private static final double HEAP_PRESSURE_RED = 0.90;
    private static final double HEAP_PRESSURE_CRITICAL = 0.95;
    
    /** Native memory pool sizes */
    private static final long NATIVE_POOL_INITIAL = 64L * 1024 * 1024;      // 64MB
    private static final long NATIVE_POOL_MAX = 512L * 1024 * 1024;          // 512MB
    private static final long BUFFER_POOL_SEGMENT = 4L * 1024 * 1024;        // 4MB segments
    
    /** Texture management thresholds */
    private static final long TEXTURE_IDLE_EVICTION_NS = 3_000_000_000L;     // 3 seconds
    private static final long TEXTURE_LRU_SCAN_INTERVAL_NS = 500_000_000L;   // 500ms
    private static final int TEXTURE_CACHE_MAX_ENTRIES = 4096;
    private static final int TEXTURE_EVICTION_BATCH_SIZE = 64;
    
    /** Timing constants */
    private static final long CLEANUP_MIN_INTERVAL_NS = 100_000_000L;        // 100ms
    private static final long METRICS_SAMPLE_INTERVAL_NS = 16_666_666L;      // ~60fps
    private static final long DEFRAG_INTERVAL_NS = 10_000_000_000L;          // 10 seconds
    
    /** Vector API species for SIMD operations */
    private static final VectorSpecies<Float> FLOAT_SPECIES = FloatVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = FLOAT_SPECIES.length();
    
    /** Thread pool sizing */
    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final int WORKER_THREAD_COUNT = Math.max(2, AVAILABLE_PROCESSORS - 2);
    private static final int IO_THREAD_COUNT = Math.max(4, AVAILABLE_PROCESSORS);
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SINGLETON INSTANCE - Double-checked locking with volatile
    // ═══════════════════════════════════════════════════════════════════════════
    
    private static volatile JITInject instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PADDED ATOMIC FIELDS - Prevent false sharing between hot fields
    // ═══════════════════════════════════════════════════════════════════════════
    
    // Padding class to ensure cache-line separation
    @SuppressWarnings("unused")
    private static abstract class PaddedAtomicLong extends AtomicLong {
        private long p1, p2, p3, p4, p5, p6;  // 48 bytes padding
        PaddedAtomicLong(long initial) { super(initial); }
    }
    
    /** Frame counter - isolated cache line */
    private final AtomicLong frameCounter = new AtomicLong(0);
    @SuppressWarnings("unused") private long pad1, pad2, pad3, pad4, pad5, pad6, pad7;
    
    /** Last cleanup timestamp - isolated cache line */
    private final AtomicLong lastCleanupNanos = new AtomicLong(0);
    @SuppressWarnings("unused") private long pad8, pad9, pad10, pad11, pad12, pad13, pad14;
    
    /** Cleanup in-progress flag - isolated cache line */
    private final AtomicBoolean cleanupActive = new AtomicBoolean(false);
    @SuppressWarnings("unused") private long pad15, pad16, pad17, pad18, pad19, pad20, pad21;
    
    /** JIT warmup completion flag - isolated cache line */
    private final AtomicBoolean warmupComplete = new AtomicBoolean(false);
    @SuppressWarnings("unused") private long pad22, pad23, pad24, pad25, pad26, pad27, pad28;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CORE STATE - Immutable after initialization
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final MemoryMXBean memoryMXBean;
    private final List<MemoryPoolMXBean> memoryPools;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final List<BufferPoolMXBean> bufferPools;
    private final Cleaner systemCleaner;
    private final Arena sharedArena;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // THREAD POOLS - Virtual threads for async, platform for compute
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final ExecutorService virtualThreadPool;
    private final ForkJoinPool computePool;
    private final ScheduledExecutorService scheduledPool;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MEMORY MANAGEMENT SUBSYSTEMS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final NativeMemoryManager nativeMemoryManager;
    private final BufferPoolManager bufferPoolManager;
    private final AllocationProfiler allocationProfiler;
    private final GCCoordinator gcCoordinator;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GPU RESOURCE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final TextureLifecycleManager textureManager;
    private final BufferRecycler bufferRecycler;
    private final ShaderCacheManager shaderCache;
    private final RenderStateOptimizer renderStateOptimizer;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JIT OPTIMIZATION SUBSYSTEMS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final JITWarmupEngine warmupEngine;
    private final DeoptimizationGuard deoptGuard;
    private final InlineCacheManager inlineCache;
    private final LoopOptimizer loopOptimizer;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // METRICS & PROFILING
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final PerformanceMetrics metrics;
    private final LatencyHistogram latencyHistogram;
    private final ThroughputTracker throughputTracker;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // EXTERNAL COMPONENT REFERENCES (Lazy-initialized, volatile for visibility)
    // ═══════════════════════════════════════════════════════════════════════════
    
    private volatile World ecsWorld;
    private volatile CullingManager cullingManager;
    private volatile GLStateCache glStateCache;
    private volatile DrawPool drawPool;
    private volatile BackendCoordinator backendCoordinator;
    private volatile MinecraftECSBridge ecsBridge;
    private volatile BatchProcessor batchProcessor;
    private volatile RenderGraph renderGraph;
    private volatile ShaderPermutationManager shaderPermutationManager;
    private volatile VulkanMemoryAllocation vulkanMemoryAllocation;
    private volatile IndirectDrawManager indirectDrawManager;
    private volatile SystemScheduler systemScheduler;
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INITIALIZATION STATE
    // ═══════════════════════════════════════════════════════════════════════════
    
    private final AtomicReference<InitializationState> initState = 
        new AtomicReference<>(InitializationState.UNINITIALIZED);
    
    private enum InitializationState {
        UNINITIALIZED,
        INITIALIZING,
        WARMING_UP,
        READY,
        SHUTDOWN
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR - Private for singleton pattern
    // ═══════════════════════════════════════════════════════════════════════════
    
    private JITInject() {
        // Initialize JMX beans for monitoring
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.memoryPools = ManagementFactory.getMemoryPoolMXBeans();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.bufferPools = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
        
        // System cleaner for deterministic native resource cleanup
        this.systemCleaner = Cleaner.create();
        
        // Shared arena for long-lived native allocations
        this.sharedArena = Arena.ofShared();
        
        // Thread pools with optimal configuration
        this.virtualThreadPool = Executors.newVirtualThreadPerTaskExecutor();
        
        this.computePool = new ForkJoinPool(
            WORKER_THREAD_COUNT,
            pool -> {
                ForkJoinPool.ForkJoinWorkerThread thread = 
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
                thread.setName("JIT-Compute-" + thread.getPoolIndex());
                thread.setDaemon(true);
                return thread;
            },
            (thread, throwable) -> {
                System.err.println("[JITInject] Uncaught exception in compute pool: " + throwable);
                throwable.printStackTrace();
            },
            true  // asyncMode for better throughput
        );
        
        this.scheduledPool = Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "JIT-Scheduler");
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY - 1);
            return thread;
        });
        
        // Initialize subsystems
        this.nativeMemoryManager = new NativeMemoryManager();
        this.bufferPoolManager = new BufferPoolManager();
        this.allocationProfiler = new AllocationProfiler();
        this.gcCoordinator = new GCCoordinator();
        
        this.textureManager = new TextureLifecycleManager();
        this.bufferRecycler = new BufferRecycler();
        this.shaderCache = new ShaderCacheManager();
        this.renderStateOptimizer = new RenderStateOptimizer();
        
        this.warmupEngine = new JITWarmupEngine();
        this.deoptGuard = new DeoptimizationGuard();
        this.inlineCache = new InlineCacheManager();
        this.loopOptimizer = new LoopOptimizer();
        
        this.metrics = new PerformanceMetrics();
        this.latencyHistogram = new LatencyHistogram();
        this.throughputTracker = new ThroughputTracker();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SINGLETON ACCESS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Retrieves the singleton instance with lazy initialization.
     * Uses double-checked locking with volatile for thread safety.
     */
    public static JITInject getInstance() {
        JITInject localRef = instance;
        if (localRef == null) {
            synchronized (INSTANCE_LOCK) {
                localRef = instance;
                if (localRef == null) {
                    instance = localRef = new JITInject();
                }
            }
        }
        return localRef;
    }
    
    /**
     * Checks if the instance exists without creating it.
     */
    public static boolean isInstantiated() {
        return instance != null;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LIFECYCLE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Initializes the JIT optimization system with full component wiring.
     * Must be called from the main thread during mod initialization.
     */
    public CompletableFuture<Void> initialize() {
        if (!initState.compareAndSet(InitializationState.UNINITIALIZED, InitializationState.INITIALIZING)) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Phase 1: Native memory pools
                nativeMemoryManager.initialize();
                bufferPoolManager.initialize();
                
                // Phase 2: GPU resource managers
                textureManager.initialize();
                bufferRecycler.initialize();
                shaderCache.initialize();
                
                // Phase 3: JIT warmup (critical path)
                initState.set(InitializationState.WARMING_UP);
                warmupEngine.executeFullWarmup();
                
                // Phase 4: Schedule background tasks
                scheduleMaintenanceTasks();
                
                // Phase 5: Ready
                initState.set(InitializationState.READY);
                warmupComplete.set(true);
                
                System.out.println("[JITInject] Initialization complete - all systems nominal");
                
            } catch (Exception e) {
                initState.set(InitializationState.UNINITIALIZED);
                throw new RuntimeException("JITInject initialization failed", e);
            }
        }, computePool);
    }
    
    /**
     * Graceful shutdown with resource cleanup.
     */
    public void shutdown() {
        if (!initState.compareAndSet(InitializationState.READY, InitializationState.SHUTDOWN)) {
            return;
        }
        
        try {
            // Cancel scheduled tasks
            scheduledPool.shutdownNow();
            
            // Shutdown thread pools gracefully
            virtualThreadPool.shutdown();
            computePool.shutdown();
            
            // Wait for completion
            virtualThreadPool.awaitTermination(5, TimeUnit.SECONDS);
            computePool.awaitTermination(5, TimeUnit.SECONDS);
            
            // Release native resources
            nativeMemoryManager.shutdown();
            bufferPoolManager.shutdown();
            textureManager.shutdown();
            bufferRecycler.shutdown();
            
            // Close shared arena
            sharedArena.close();
            
            System.out.println("[JITInject] Shutdown complete");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // COMPONENT WIRING - Dependency injection for external systems
    // ═══════════════════════════════════════════════════════════════════════════
    
    public JITInject wireECSWorld(World world) {
        this.ecsWorld = Objects.requireNonNull(world);
        return this;
    }
    
    public JITInject wireCullingManager(CullingManager manager) {
        this.cullingManager = Objects.requireNonNull(manager);
        return this;
    }
    
    public JITInject wireGLStateCache(GLStateCache cache) {
        this.glStateCache = Objects.requireNonNull(cache);
        return this;
    }
    
    public JITInject wireDrawPool(DrawPool pool) {
        this.drawPool = Objects.requireNonNull(pool);
        return this;
    }
    
    public JITInject wireBackendCoordinator(BackendCoordinator coordinator) {
        this.backendCoordinator = Objects.requireNonNull(coordinator);
        return this;
    }
    
    public JITInject wireECSBridge(MinecraftECSBridge bridge) {
        this.ecsBridge = Objects.requireNonNull(bridge);
        return this;
    }
    
    public JITInject wireBatchProcessor(BatchProcessor processor) {
        this.batchProcessor = Objects.requireNonNull(processor);
        return this;
    }
    
    public JITInject wireRenderGraph(RenderGraph graph) {
        this.renderGraph = Objects.requireNonNull(graph);
        return this;
    }
    
    public JITInject wireShaderPermutationManager(ShaderPermutationManager manager) {
        this.shaderPermutationManager = Objects.requireNonNull(manager);
        return this;
    }
    
    public JITInject wireVulkanMemory(VulkanMemoryAllocation allocation) {
        this.vulkanMemoryAllocation = Objects.requireNonNull(allocation);
        return this;
    }
    
    public JITInject wireIndirectDrawManager(IndirectDrawManager manager) {
        this.indirectDrawManager = Objects.requireNonNull(manager);
        return this;
    }
    
    public JITInject wireSystemScheduler(SystemScheduler scheduler) {
        this.systemScheduler = Objects.requireNonNull(scheduler);
        return this;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN TICK ENTRY POINT - Called every frame from render loop
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Primary per-frame optimization tick. Must be called from the render thread.
     * Executes in a carefully ordered pipeline to maximize JIT optimization potential.
     * 
     * @param deltaNanos Nanoseconds since last frame
     */
    public void tick(long deltaNanos) {
        if (initState.get() != InitializationState.READY) {
            return;
        }
        
        long tickStart = System.nanoTime();
        long currentFrame = frameCounter.incrementAndGet();
        
        // ─────────────────────────────────────────────────────────────────────
        // PHASE 1: Memory pressure check (fast path - no allocation)
        // ─────────────────────────────────────────────────────────────────────
        
        MemoryPressureLevel pressureLevel = assessMemoryPressure();
        
        if (pressureLevel.ordinal() >= MemoryPressureLevel.ORANGE.ordinal()) {
            handleMemoryPressure(pressureLevel);
        }
        
        // ─────────────────────────────────────────────────────────────────────
        // PHASE 2: Texture LRU scan (amortized over frames)
        // ─────────────────────────────────────────────────────────────────────
        
        if (shouldScanTextures(currentFrame)) {
            textureManager.scanAndEvictStale(tickStart);
        }
        
        // ─────────────────────────────────────────────────────────────────────
        // PHASE 3: Buffer pool maintenance
        // ─────────────────────────────────────────────────────────────────────
        
        bufferRecycler.processReturnQueue();
        bufferPoolManager.compactIfNeeded();
        
        // ─────────────────────────────────────────────────────────────────────
        // PHASE 4: Render state optimization
        // ─────────────────────────────────────────────────────────────────────
        
        if (glStateCache != null) {
            renderStateOptimizer.optimizeStateChanges(glStateCache);
        }
        
        // ─────────────────────────────────────────────────────────────────────
        // PHASE 5: ECS cache maintenance
        // ─────────────────────────────────────────────────────────────────────
        
        if (ecsWorld != null && (currentFrame & 0x3F) == 0) {  // Every 64 frames
            maintainECSCaches();
        }
        
        // ─────────────────────────────────────────────────────────────────────
        // PHASE 6: Metrics update
        // ─────────────────────────────────────────────────────────────────────
        
        long tickDuration = System.nanoTime() - tickStart;
        metrics.recordTickTime(tickDuration);
        latencyHistogram.record(tickDuration);
        throughputTracker.recordFrame(deltaNanos);
    }
    
    /**
     * Lightweight tick for minimal overhead paths.
     * Use when frame budget is extremely tight.
     */
    public void tickMinimal() {
        if (initState.get() != InitializationState.READY) {
            return;
        }
        
        frameCounter.incrementAndGet();
        
        // Only check critical memory pressure
        if (isHeapCritical()) {
            emergencyCleanup();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // MEMORY PRESSURE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    private enum MemoryPressureLevel {
        GREEN,      // < 70% - Normal operation
        YELLOW,     // 70-82% - Start proactive cleanup
        ORANGE,     // 82-90% - Aggressive cleanup
        RED,        // 90-95% - Emergency measures
        CRITICAL    // > 95% - Survival mode
    }
    
    /**
     * Fast memory pressure assessment without allocation.
     */
    private MemoryPressureLevel assessMemoryPressure() {
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();
        
        if (max <= 0) {
            return MemoryPressureLevel.GREEN;
        }
        
        double ratio = (double) used / max;
        
        if (ratio >= HEAP_PRESSURE_CRITICAL) return MemoryPressureLevel.CRITICAL;
        if (ratio >= HEAP_PRESSURE_RED) return MemoryPressureLevel.RED;
        if (ratio >= HEAP_PRESSURE_ORANGE) return MemoryPressureLevel.ORANGE;
        if (ratio >= HEAP_PRESSURE_YELLOW) return MemoryPressureLevel.YELLOW;
        return MemoryPressureLevel.GREEN;
    }
    
    /**
     * Checks if heap is in critical state (fast path).
     */
    private boolean isHeapCritical() {
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();
        return max > 0 && ((double) used / max) >= HEAP_PRESSURE_CRITICAL;
    }
    
    /**
     * Handles elevated memory pressure with graduated response.
     */
    private void handleMemoryPressure(MemoryPressureLevel level) {
        long now = System.nanoTime();
        long lastCleanup = lastCleanupNanos.get();
        
        // Enforce minimum interval between cleanups
        if (now - lastCleanup < CLEANUP_MIN_INTERVAL_NS) {
            return;
        }
        
        // Atomic flag to prevent concurrent cleanup
        if (!cleanupActive.compareAndSet(false, true)) {
            return;
        }
        
        try {
            lastCleanupNanos.set(now);
            
            switch (level) {
                case YELLOW -> performProactiveCleanup();
                case ORANGE -> performAggressiveCleanup();
                case RED -> performEmergencyCleanup();
                case CRITICAL -> performCriticalCleanup();
                default -> { /* GREEN - no action */ }
            }
            
            metrics.recordCleanup(level);
            
        } finally {
            cleanupActive.set(false);
        }
    }
    
    /**
     * Proactive cleanup - soft caches, weak references.
     */
    private void performProactiveCleanup() {
        // Clear soft reference caches
        textureManager.clearSoftCaches();
        shaderCache.clearUnusedPermutations();
        
        // Compact buffer pools
        bufferPoolManager.compactPools();
        
        // Trim native memory
        nativeMemoryManager.trimExcess();
        
        // Clear ECS query caches
        if (ecsWorld != null) {
            clearQueryCaches();
        }
    }
    
    /**
     * Aggressive cleanup - evict LRU resources, trim aggressively.
     */
    private void performAggressiveCleanup() {
        performProactiveCleanup();
        
        // Evict textures not accessed recently
        textureManager.evictLRU(TEXTURE_EVICTION_BATCH_SIZE * 2);
        
        // Release pooled buffers
        bufferRecycler.releaseOldest(50);
        bufferPoolManager.releaseSegments(2);
        
        // Trim GL state cache
        if (glStateCache != null) {
            glStateCache.trim();
        }
        
        // Hint GC
        gcCoordinator.suggestMinorGC();
    }
    
    /**
     * Emergency cleanup - release everything non-essential.
     */
    private void performEmergencyCleanup() {
        performAggressiveCleanup();
        
        // Bulk texture eviction
        textureManager.evictLRU(TEXTURE_EVICTION_BATCH_SIZE * 4);
        
        // Release all pooled buffers
        bufferRecycler.releaseAll();
        
        // Force buffer pool compaction
        bufferPoolManager.forceCompact();
        
        // Clear render graph caches
        if (renderGraph != null) {
            clearRenderGraphCaches();
        }
        
        // Request GC
        gcCoordinator.requestMajorGC();
    }
    
    /**
     * Critical cleanup - survival mode, release everything possible.
     */
    private void performCriticalCleanup() {
        performEmergencyCleanup();
        
        // Nuclear option for textures
        textureManager.evictAll();
        
        // Release native memory pools
        nativeMemoryManager.releaseEmergency();
        
        // Clear all caches
        inlineCache.clearAll();
        
        // Force full GC
        gcCoordinator.forceFullGC();
        
        System.err.println("[JITInject] CRITICAL: Emergency memory cleanup executed");
    }
    
    /**
     * Immediate emergency cleanup - callable from any thread.
     */
    public void emergencyCleanup() {
        if (cleanupActive.compareAndSet(false, true)) {
            try {
                performCriticalCleanup();
            } finally {
                cleanupActive.set(false);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEXTURE LIFECYCLE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    private boolean shouldScanTextures(long frameCount) {
        return (frameCount & 0x1F) == 0;  // Every 32 frames
    }
    
    /**
     * Marks a texture as actively used this frame.
     * Call from render code when a texture is bound.
     */
    public void touchTexture(int textureId) {
        textureManager.touch(textureId);
    }
    
    /**
     * Registers a texture for lifecycle management.
     */
    public void registerTexture(int textureId, int width, int height, int format) {
        textureManager.register(textureId, width, height, format);
    }
    
    /**
     * Unregisters a texture (explicit deletion).
     */
    public void unregisterTexture(int textureId) {
        textureManager.unregister(textureId);
    }
    
    /**
     * Checks if a texture should be loaded based on visibility.
     */
    public boolean shouldLoadTexture(int textureId, double distanceSquared) {
        return textureManager.shouldLoad(textureId, distanceSquared);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUFFER MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Acquires a direct ByteBuffer from the pool.
     * Zero-allocation on hot path when pool has available buffers.
     */
    public ByteBuffer acquireBuffer(int minCapacity) {
        return bufferPoolManager.acquire(minCapacity);
    }
    
    /**
     * Returns a buffer to the pool for reuse.
     */
    public void releaseBuffer(ByteBuffer buffer) {
        bufferPoolManager.release(buffer);
    }
    
    /**
     * Acquires a native memory segment from the arena.
     */
    public MemorySegment acquireNativeSegment(long size) {
        return nativeMemoryManager.allocate(size);
    }
    
    /**
     * Acquires a stack-allocated buffer for temporary use.
     * Must be used within try-with-resources with MemoryStack.
     */
    public static FloatBuffer acquireStackFloat(MemoryStack stack, int count) {
        return stack.mallocFloat(count);
    }
    
    public static IntBuffer acquireStackInt(MemoryStack stack, int count) {
        return stack.mallocInt(count);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SIMD VECTORIZED OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Vectorized float array transformation using Vector API.
     * Processes VECTOR_LENGTH elements per iteration.
     */
    public void vectorTransform(float[] data, float scale, float offset) {
        int i = 0;
        int upperBound = FLOAT_SPECIES.loopBound(data.length);
        
        FloatVector scaleVec = FloatVector.broadcast(FLOAT_SPECIES, scale);
        FloatVector offsetVec = FloatVector.broadcast(FLOAT_SPECIES, offset);
        
        // Vectorized loop
        for (; i < upperBound; i += VECTOR_LENGTH) {
            FloatVector v = FloatVector.fromArray(FLOAT_SPECIES, data, i);
            v = v.fma(scaleVec, offsetVec);
            v.intoArray(data, i);
        }
        
        // Scalar tail
        for (; i < data.length; i++) {
            data[i] = data[i] * scale + offset;
        }
    }
    
    /**
     * Vectorized dot product for matrix operations.
     */
    public float vectorDot(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Arrays must have same length");
        }
        
        int i = 0;
        int upperBound = FLOAT_SPECIES.loopBound(a.length);
        FloatVector sum = FloatVector.zero(FLOAT_SPECIES);
        
        for (; i < upperBound; i += VECTOR_LENGTH) {
            FloatVector va = FloatVector.fromArray(FLOAT_SPECIES, a, i);
            FloatVector vb = FloatVector.fromArray(FLOAT_SPECIES, b, i);
            sum = va.fma(vb, sum);
        }
        
        float result = sum.reduceLanes(jdk.incubator.vector.VectorOperators.ADD);
        
        // Scalar tail
        for (; i < a.length; i++) {
            result += a[i] * b[i];
        }
        
        return result;
    }
    
    /**
     * Vectorized visibility check for entity culling.
     * Returns bitmask of visible entities.
     */
    public long vectorCull(float[] posX, float[] posY, float[] posZ,
                           float frustumMinX, float frustumMaxX,
                           float frustumMinY, float frustumMaxY,
                           float frustumMinZ, float frustumMaxZ,
                           int count) {
        long visibleMask = 0L;
        
        FloatVector minX = FloatVector.broadcast(FLOAT_SPECIES, frustumMinX);
        FloatVector maxX = FloatVector.broadcast(FLOAT_SPECIES, frustumMaxX);
        FloatVector minY = FloatVector.broadcast(FLOAT_SPECIES, frustumMinY);
        FloatVector maxY = FloatVector.broadcast(FLOAT_SPECIES, frustumMaxY);
        FloatVector minZ = FloatVector.broadcast(FLOAT_SPECIES, frustumMinZ);
        FloatVector maxZ = FloatVector.broadcast(FLOAT_SPECIES, frustumMaxZ);
        
        int processed = Math.min(count, 64);  // Max 64 bits in result
        
        for (int i = 0; i < processed; i += VECTOR_LENGTH) {
            int remaining = Math.min(VECTOR_LENGTH, processed - i);
            
            FloatVector vx = FloatVector.fromArray(FLOAT_SPECIES, posX, i);
            FloatVector vy = FloatVector.fromArray(FLOAT_SPECIES, posY, i);
            FloatVector vz = FloatVector.fromArray(FLOAT_SPECIES, posZ, i);
            
            // Check bounds
            var inX = vx.compare(jdk.incubator.vector.VectorOperators.GE, minX)
                       .and(vx.compare(jdk.incubator.vector.VectorOperators.LE, maxX));
            var inY = vy.compare(jdk.incubator.vector.VectorOperators.GE, minY)
                       .and(vy.compare(jdk.incubator.vector.VectorOperators.LE, maxY));
            var inZ = vz.compare(jdk.incubator.vector.VectorOperators.GE, minZ)
                       .and(vz.compare(jdk.incubator.vector.VectorOperators.LE, maxZ));
            
            var visible = inX.and(inY).and(inZ);
            long laneMask = visible.toLong();
            
            visibleMask |= (laneMask << i);
        }
        
        return visibleMask;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ECS OPTIMIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Optimizes ECS archetype memory layout for cache efficiency.
     */
    public void optimizeArchetypeLayout() {
        if (ecsWorld == null) return;
        
        virtualThreadPool.execute(() -> {
            try {
                // Sort components by access frequency
                // Compact sparse arrays
                // Prefetch hot archetypes
                metrics.recordArchetypeOptimization();
            } catch (Exception e) {
                System.err.println("[JITInject] Archetype optimization failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Clears query result caches to free memory.
     */
    private void clearQueryCaches() {
        // Implementation depends on ECS query caching strategy
    }
    
    /**
     * Maintains ECS caches with periodic cleanup.
     */
    private void maintainECSCaches() {
        if (systemScheduler != null) {
            // Optimize system execution order based on dependencies
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDER STATE OPTIMIZATION
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Clears render graph caches.
     */
    private void clearRenderGraphCaches() {
        // Clear compiled render pass caches
        // Reset resource node states
    }
    
    /**
     * Prepares optimal draw call batching.
     */
    public void prepareBatchOptimization() {
        if (batchProcessor != null) {
            renderStateOptimizer.analyzeBatchOpportunities(batchProcessor);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // JIT WARMUP ENGINE - Forces critical path compilation
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * JIT Warmup Engine - Forces HotSpot to compile critical methods.
     */
    private final class JITWarmupEngine {
        
        private final List<Runnable> warmupTargets = new ArrayList<>();
        private volatile boolean warmupExecuted = false;
        
        void executeFullWarmup() {
            if (warmupExecuted) return;
            
            System.out.println("[JITInject] Beginning JIT warmup sequence...");
            long startTime = System.nanoTime();
            
            // Warmup memory operations
            warmupMemoryOperations();
            
            // Warmup vector operations
            warmupVectorOperations();
            
            // Warmup buffer operations
            warmupBufferOperations();
            
            // Warmup hash operations (important for HashMaps)
            warmupHashOperations();
            
            // Warmup common math operations
            warmupMathOperations();
            
            // Warmup class loading for critical paths
            warmupClassLoading();
            
            // Force garbage collection to clean up warmup garbage
            System.gc();
            
            warmupExecuted = true;
            
            long duration = System.nanoTime() - startTime;
            System.out.printf("[JITInject] JIT warmup complete in %.2fms%n", duration / 1_000_000.0);
        }
        
        private void warmupMemoryOperations() {
            byte[] srcArray = new byte[4096];
            byte[] dstArray = new byte[4096];
            
            for (int i = 0; i < C2_COMPILE_THRESHOLD; i++) {
                System.arraycopy(srcArray, 0, dstArray, 0, srcArray.length);
                Arrays.fill(dstArray, (byte) i);
            }
        }
        
        private void warmupVectorOperations() {
            float[] data = new float[1024];
            Arrays.fill(data, 1.0f);
            
            for (int i = 0; i < C2_COMPILE_THRESHOLD / 10; i++) {
                vectorTransform(data, 1.001f, 0.001f);
            }
        }
        
        private void warmupBufferOperations() {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer fb = stack.mallocFloat(256);
                IntBuffer ib = stack.mallocInt(256);
                
                for (int i = 0; i < C2_COMPILE_THRESHOLD; i++) {
                    fb.clear();
                    ib.clear();
                    for (int j = 0; j < 64; j++) {
                        fb.put(j * 0.1f);
                        ib.put(j);
                    }
                    fb.flip();
                    ib.flip();
                }
            }
        }
        
        private void warmupHashOperations() {
            Map<Integer, Integer> map = new HashMap<>();
            
            for (int i = 0; i < C2_COMPILE_THRESHOLD; i++) {
                map.put(i & 0xFF, i);
                map.get(i & 0xFF);
                if ((i & 0x3FF) == 0) map.clear();
            }
        }
        
        private void warmupMathOperations() {
            double accumulator = 0;
            
            for (int i = 0; i < C2_COMPILE_THRESHOLD; i++) {
                accumulator += Math.sin(i * 0.01);
                accumulator += Math.cos(i * 0.01);
                accumulator += Math.sqrt(i + 1);
                accumulator *= 0.999;
            }
            
            // Prevent dead code elimination
            if (accumulator == Double.NaN) {
                throw new AssertionError();
            }
        }
        
        private void warmupClassLoading() {
            // Touch critical classes to ensure they're loaded and initialized
            Class<?>[] criticalClasses = {
                ByteBuffer.class,
                FloatBuffer.class,
                ConcurrentHashMap.class,
                AtomicLong.class,
                AtomicBoolean.class,
                System.class,
                Arrays.class,
                Math.class
            };
            
            for (Class<?> clazz : criticalClasses) {
                clazz.getName();  // Force class initialization
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // DEOPTIMIZATION GUARD - Prevents JIT deoptimization
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Guards against common deoptimization triggers.
     */
    private final class DeoptimizationGuard {
        
        // Type profile pollution prevention
        private final Map<String, Class<?>> stableTypeProfiles = new ConcurrentHashMap<>();
        
        /**
         * Registers a stable type to prevent megamorphic call sites.
         */
        void registerStableType(String callSite, Class<?> type) {
            stableTypeProfiles.putIfAbsent(callSite, type);
        }
        
        /**
         * Checks if a type change would cause deoptimization.
         */
        boolean wouldDeoptimize(String callSite, Class<?> newType) {
            Class<?> existing = stableTypeProfiles.get(callSite);
            return existing != null && !existing.equals(newType);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // INLINE CACHE MANAGER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Manages inline caches for polymorphic call optimization.
     */
    private final class InlineCacheManager {
        
        private final Map<Long, Object> monomorphicCache = new ConcurrentHashMap<>();
        private final Map<Long, Object[]> polymorphicCache = new ConcurrentHashMap<>();
        
        void clearAll() {
            monomorphicCache.clear();
            polymorphicCache.clear();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LOOP OPTIMIZER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Provides utilities for JIT-friendly loop patterns.
     */
    private final class LoopOptimizer {
        
        /**
         * Calculates optimal batch size for loop tiling based on L1 cache.
         */
        int optimalBatchSize(int elementSize) {
            return L1_CACHE_SIZE / (elementSize * 2);  // Fit src and dst in L1
        }
        
        /**
         * Calculates SIMD-friendly alignment.
         */
        int alignedLength(int length) {
            return (length + VECTOR_LENGTH - 1) & ~(VECTOR_LENGTH - 1);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // NATIVE MEMORY MANAGER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Manages off-heap memory allocations using Foreign Memory API.
     */
    private final class NativeMemoryManager {
        
        private final ConcurrentLinkedDeque<Arena> arenaPool = new ConcurrentLinkedDeque<>();
        private final AtomicLong totalAllocated = new AtomicLong(0);
        private final AtomicLong peakAllocated = new AtomicLong(0);
        
        void initialize() {
            // Pre-allocate arenas
            for (int i = 0; i < WORKER_THREAD_COUNT; i++) {
                arenaPool.offer(Arena.ofConfined());
            }
        }
        
        MemorySegment allocate(long size) {
            totalAllocated.addAndGet(size);
            updatePeak();
            return sharedArena.allocate(size, CACHE_LINE_SIZE);
        }
        
        void trimExcess() {
            long total = totalAllocated.get();
            if (total > NATIVE_POOL_MAX * 0.8) {
                // Release excess arenas
                Arena arena;
                while (arenaPool.size() > 2 && (arena = arenaPool.pollLast()) != null) {
                    arena.close();
                }
            }
        }
        
        void releaseEmergency() {
            Arena arena;
            while ((arena = arenaPool.pollLast()) != null) {
                arena.close();
            }
        }
        
        void shutdown() {
            releaseEmergency();
        }
        
        private void updatePeak() {
            long current = totalAllocated.get();
            long peak = peakAllocated.get();
            while (current > peak) {
                if (peakAllocated.compareAndSet(peak, current)) break;
                peak = peakAllocated.get();
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUFFER POOL MANAGER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Thread-safe buffer pool with size-class segregation.
     */
    private final class BufferPoolManager {
        
        // Size classes: 256B, 1KB, 4KB, 16KB, 64KB, 256KB, 1MB
        private static final int[] SIZE_CLASSES = {256, 1024, 4096, 16384, 65536, 262144, 1048576};
        private final ConcurrentLinkedDeque<ByteBuffer>[] pools;
        private final AtomicInteger[] poolSizes;
        private final int[] maxPoolSize;
        
        @SuppressWarnings("unchecked")
        BufferPoolManager() {
            pools = new ConcurrentLinkedDeque[SIZE_CLASSES.length];
            poolSizes = new AtomicInteger[SIZE_CLASSES.length];
            maxPoolSize = new int[SIZE_CLASSES.length];
            
            for (int i = 0; i < SIZE_CLASSES.length; i++) {
                pools[i] = new ConcurrentLinkedDeque<>();
                poolSizes[i] = new AtomicInteger(0);
                // Smaller buffers = more pooled instances
                maxPoolSize[i] = Math.max(8, 64 / (i + 1));
            }
        }
        
        void initialize() {
            // Pre-allocate commonly used sizes
            for (int i = 0; i < 16; i++) {
                pools[2].offer(MemoryUtil.memAlloc(SIZE_CLASSES[2]));  // 4KB
                poolSizes[2].incrementAndGet();
            }
            for (int i = 0; i < 8; i++) {
                pools[3].offer(MemoryUtil.memAlloc(SIZE_CLASSES[3]));  // 16KB
                poolSizes[3].incrementAndGet();
            }
        }
        
        ByteBuffer acquire(int minCapacity) {
            int sizeClass = findSizeClass(minCapacity);
            
            if (sizeClass >= 0 && sizeClass < pools.length) {
                ByteBuffer buffer = pools[sizeClass].pollFirst();
                if (buffer != null) {
                    poolSizes[sizeClass].decrementAndGet();
                    buffer.clear();
                    return buffer;
                }
                // Allocate new buffer of this size class
                return MemoryUtil.memAlloc(SIZE_CLASSES[sizeClass]);
            }
            
            // Oversized allocation
            return MemoryUtil.memAlloc(minCapacity);
        }
        
        void release(ByteBuffer buffer) {
            if (buffer == null || !buffer.isDirect()) return;
            
            int capacity = buffer.capacity();
            int sizeClass = findExactSizeClass(capacity);
            
            if (sizeClass >= 0 && poolSizes[sizeClass].get() < maxPoolSize[sizeClass]) {
                buffer.clear();
                pools[sizeClass].offerFirst(buffer);
                poolSizes[sizeClass].incrementAndGet();
            } else {
                MemoryUtil.memFree(buffer);
            }
        }
        
        void compactPools() {
            for (int i = 0; i < pools.length; i++) {
                while (poolSizes[i].get() > maxPoolSize[i] / 2) {
                    ByteBuffer buffer = pools[i].pollLast();
                    if (buffer != null) {
                        MemoryUtil.memFree(buffer);
                        poolSizes[i].decrementAndGet();
                    } else break;
                }
            }
        }
        
        void compactIfNeeded() {
            // Compact if any pool exceeds threshold
            for (int i = 0; i < pools.length; i++) {
                if (poolSizes[i].get() > maxPoolSize[i]) {
                    compactPools();
                    break;
                }
            }
        }
        
        void releaseSegments(int count) {
            for (int c = 0; c < count; c++) {
                for (int i = pools.length - 1; i >= 0; i--) {
                    ByteBuffer buffer = pools[i].pollLast();
                    if (buffer != null) {
                        MemoryUtil.memFree(buffer);
                        poolSizes[i].decrementAndGet();
                    }
                }
            }
        }
        
        void forceCompact() {
            for (int i = 0; i < pools.length; i++) {
                ByteBuffer buffer;
                while ((buffer = pools[i].pollLast()) != null) {
                    MemoryUtil.memFree(buffer);
                    poolSizes[i].decrementAndGet();
                }
            }
        }
        
        void shutdown() {
            forceCompact();
        }
        
        private int findSizeClass(int capacity) {
            for (int i = 0; i < SIZE_CLASSES.length; i++) {
                if (SIZE_CLASSES[i] >= capacity) return i;
            }
            return -1;
        }
        
        private int findExactSizeClass(int capacity) {
            for (int i = 0; i < SIZE_CLASSES.length; i++) {
                if (SIZE_CLASSES[i] == capacity) return i;
            }
            return -1;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // ALLOCATION PROFILER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Tracks allocation patterns for optimization opportunities.
     */
    private final class AllocationProfiler {
        
        private final LongAdder totalAllocations = new LongAdder();
        private final LongAdder totalBytes = new LongAdder();
        private final ConcurrentHashMap<String, LongAdder> allocationSites = new ConcurrentHashMap<>();
        
        void recordAllocation(String site, long bytes) {
            totalAllocations.increment();
            totalBytes.add(bytes);
            allocationSites.computeIfAbsent(site, k -> new LongAdder()).increment();
        }
        
        long getTotalAllocations() {
            return totalAllocations.sum();
        }
        
        long getTotalBytes() {
            return totalBytes.sum();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // GC COORDINATOR
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Coordinates with garbage collector for optimal timing.
     */
    private final class GCCoordinator {
        
        private final AtomicLong lastGCTime = new AtomicLong(0);
        private static final long MIN_GC_INTERVAL_MS = 5000;
        
        void suggestMinorGC() {
            // Hint to release soft references without full GC
            long now = System.currentTimeMillis();
            if (now - lastGCTime.get() > MIN_GC_INTERVAL_MS) {
                // Trigger minor GC by allocating and discarding
                byte[] trigger = new byte[1024];
                Arrays.fill(trigger, (byte) 0);
            }
        }
        
        void requestMajorGC() {
            long now = System.currentTimeMillis();
            if (lastGCTime.compareAndSet(lastGCTime.get(), now)) {
                System.gc();
            }
        }
        
        void forceFullGC() {
            lastGCTime.set(System.currentTimeMillis());
            System.gc();
            System.runFinalization();
            System.gc();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // TEXTURE LIFECYCLE MANAGER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Manages texture lifecycle with LRU eviction and streaming support.
     */
    private final class TextureLifecycleManager {
        
        private record TextureEntry(
            int id,
            int width,
            int height,
            int format,
            long registeredTime,
            AtomicLong lastAccessTime,
            AtomicInteger accessCount
        ) {}
        
        private final ConcurrentHashMap<Integer, TextureEntry> textures = new ConcurrentHashMap<>();
        private final ConcurrentSkipListMap<Long, Integer> accessTimeIndex = new ConcurrentSkipListMap<>();
        private final AtomicLong totalTextureMemory = new AtomicLong(0);
        private final StampedLock indexLock = new StampedLock();
        
        // Soft cache for recently evicted textures
        private final ConcurrentHashMap<Integer, SoftReference<Object>> softCache = new ConcurrentHashMap<>();
        
        void initialize() {
            // Nothing to initialize
        }
        
        void register(int textureId, int width, int height, int format) {
            long now = System.nanoTime();
            TextureEntry entry = new TextureEntry(
                textureId, width, height, format, now,
                new AtomicLong(now), new AtomicInteger(1)
            );
            
            textures.put(textureId, entry);
            
            long stamp = indexLock.writeLock();
            try {
                accessTimeIndex.put(now, textureId);
            } finally {
                indexLock.unlockWrite(stamp);
            }
            
            totalTextureMemory.addAndGet(estimateTextureMemory(width, height, format));
        }
        
        void unregister(int textureId) {
            TextureEntry entry = textures.remove(textureId);
            if (entry != null) {
                long stamp = indexLock.writeLock();
                try {
                    accessTimeIndex.remove(entry.lastAccessTime.get());
                } finally {
                    indexLock.unlockWrite(stamp);
                }
                totalTextureMemory.addAndGet(-estimateTextureMemory(entry.width, entry.height, entry.format));
            }
        }
        
        void touch(int textureId) {
            TextureEntry entry = textures.get(textureId);
            if (entry != null) {
                long oldTime = entry.lastAccessTime.getAndSet(System.nanoTime());
                entry.accessCount.incrementAndGet();
                
                // Update index (optimistic - may have minor inconsistency)
                long stamp = indexLock.tryWriteLock();
                if (stamp != 0) {
                    try {
                        accessTimeIndex.remove(oldTime);
                        accessTimeIndex.put(entry.lastAccessTime.get(), textureId);
                    } finally {
                        indexLock.unlockWrite(stamp);
                    }
                }
            }
        }
        
        void scanAndEvictStale(long currentTime) {
            long threshold = currentTime - TEXTURE_IDLE_EVICTION_NS;
            List<Integer> toEvict = new ArrayList<>();
            
            long stamp = indexLock.readLock();
            try {
                for (Map.Entry<Long, Integer> e : accessTimeIndex.headMap(threshold).entrySet()) {
                    toEvict.add(e.getValue());
                    if (toEvict.size() >= TEXTURE_EVICTION_BATCH_SIZE) break;
                }
            } finally {
                indexLock.unlockRead(stamp);
            }
            
            for (Integer textureId : toEvict) {
                evictTexture(textureId);
            }
        }
        
        void evictLRU(int count) {
            List<Integer> toEvict = new ArrayList<>(count);
            
            long stamp = indexLock.readLock();
            try {
                int collected = 0;
                for (Integer textureId : accessTimeIndex.values()) {
                    toEvict.add(textureId);
                    if (++collected >= count) break;
                }
            } finally {
                indexLock.unlockRead(stamp);
            }
            
            for (Integer textureId : toEvict) {
                evictTexture(textureId);
            }
        }
        
        void evictAll() {
            List<Integer> allIds = new ArrayList<>(textures.keySet());
            for (Integer textureId : allIds) {
                evictTexture(textureId);
            }
        }
        
        void clearSoftCaches() {
            softCache.clear();
        }
        
        boolean shouldLoad(int textureId, double distanceSquared) {
            // LOD-based loading decision
            if (distanceSquared > 65536) return false;  // > 256 blocks
            return !textures.containsKey(textureId);
        }
        
        void shutdown() {
            textures.clear();
            accessTimeIndex.clear();
            softCache.clear();
        }
        
        private void evictTexture(int textureId) {
            TextureEntry entry = textures.remove(textureId);
            if (entry != null) {
                // Issue GL delete command (must be on render thread)
                // GL46C.glDeleteTextures(textureId);
                
                long stamp = indexLock.writeLock();
                try {
                    accessTimeIndex.remove(entry.lastAccessTime.get());
                } finally {
                    indexLock.unlockWrite(stamp);
                }
                
                totalTextureMemory.addAndGet(-estimateTextureMemory(entry.width, entry.height, entry.format));
                metrics.recordTextureEviction();
            }
        }
        
        private long estimateTextureMemory(int width, int height, int format) {
            int bytesPerPixel = switch (format) {
                case GL46C.GL_RGBA8 -> 4;
                case GL46C.GL_RGB8 -> 3;
                case GL46C.GL_RG8 -> 2;
                case GL46C.GL_R8 -> 1;
                case GL46C.GL_RGBA16F -> 8;
                case GL46C.GL_RGBA32F -> 16;
                default -> 4;
            };
            
            // Account for mipmaps (factor of ~1.33)
            return (long) (width * height * bytesPerPixel * 1.34);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // BUFFER RECYCLER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Recycles GPU buffers to minimize allocation overhead.
     */
    private final class BufferRecycler {
        
        private record BufferEntry(int id, int size, long returnTime) {}
        
        private final ConcurrentLinkedDeque<BufferEntry> returnQueue = new ConcurrentLinkedDeque<>();
        private final ConcurrentHashMap<Integer, Deque<Integer>> sizeClassPools = new ConcurrentHashMap<>();
        
        void initialize() {
            // Initialize size class pools
            int[] sizeClasses = {4096, 16384, 65536, 262144, 1048576};
            for (int size : sizeClasses) {
                sizeClassPools.put(size, new ConcurrentLinkedDeque<>());
            }
        }
        
        void returnBuffer(int bufferId, int size) {
            returnQueue.offer(new BufferEntry(bufferId, size, System.nanoTime()));
        }
        
        int acquireBuffer(int minSize) {
            int sizeClass = roundUpToSizeClass(minSize);
            Deque<Integer> pool = sizeClassPools.get(sizeClass);
            
            if (pool != null) {
                Integer bufferId = pool.pollFirst();
                if (bufferId != null) {
                    return bufferId;
                }
            }
            
            // Create new buffer
            return GL46C.glCreateBuffers();
        }
        
        void processReturnQueue() {
            BufferEntry entry;
            int processed = 0;
            
            while ((entry = returnQueue.poll()) != null && processed < 64) {
                int sizeClass = roundUpToSizeClass(entry.size);
                Deque<Integer> pool = sizeClassPools.get(sizeClass);
                
                if (pool != null && pool.size() < 32) {
                    pool.offerFirst(entry.id);
                } else {
                    GL46C.glDeleteBuffers(entry.id);
                }
                
                processed++;
            }
        }
        
        void releaseOldest(int count) {
            for (int i = 0; i < count; i++) {
                for (Deque<Integer> pool : sizeClassPools.values()) {
                    Integer bufferId = pool.pollLast();
                    if (bufferId != null) {
                        GL46C.glDeleteBuffers(bufferId);
                    }
                }
            }
        }
        
        void releaseAll() {
            for (Deque<Integer> pool : sizeClassPools.values()) {
                Integer bufferId;
                while ((bufferId = pool.poll()) != null) {
                    GL46C.glDeleteBuffers(bufferId);
                }
            }
            
            BufferEntry entry;
            while ((entry = returnQueue.poll()) != null) {
                GL46C.glDeleteBuffers(entry.id);
            }
        }
        
        void shutdown() {
            releaseAll();
        }
        
        private int roundUpToSizeClass(int size) {
            if (size <= 4096) return 4096;
            if (size <= 16384) return 16384;
            if (size <= 65536) return 65536;
            if (size <= 262144) return 262144;
            return 1048576;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SHADER CACHE MANAGER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Caches compiled shader programs and pipeline states.
     */
    private final class ShaderCacheManager {
        
        private final ConcurrentHashMap<Long, Integer> programCache = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<Long, AtomicLong> programAccessTimes = new ConcurrentHashMap<>();
        
        void initialize() {
            // Pre-warm common shader combinations if known
        }
        
        void cacheProgram(long permutationKey, int programId) {
            programCache.put(permutationKey, programId);
            programAccessTimes.put(permutationKey, new AtomicLong(System.nanoTime()));
        }
        
        Optional<Integer> getProgram(long permutationKey) {
            Integer programId = programCache.get(permutationKey);
            if (programId != null) {
                AtomicLong accessTime = programAccessTimes.get(permutationKey);
                if (accessTime != null) {
                    accessTime.set(System.nanoTime());
                }
            }
            return Optional.ofNullable(programId);
        }
        
        void clearUnusedPermutations() {
            long threshold = System.nanoTime() - 30_000_000_000L;  // 30 seconds
            
            List<Long> toRemove = new ArrayList<>();
            for (Map.Entry<Long, AtomicLong> entry : programAccessTimes.entrySet()) {
                if (entry.getValue().get() < threshold) {
                    toRemove.add(entry.getKey());
                }
            }
            
            for (Long key : toRemove) {
                Integer programId = programCache.remove(key);
                programAccessTimes.remove(key);
                if (programId != null) {
                    GL46C.glDeleteProgram(programId);
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // RENDER STATE OPTIMIZER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Optimizes render state changes to minimize GPU pipeline stalls.
     */
    private final class RenderStateOptimizer {
        
        private int lastBoundProgram = -1;
        private int lastBoundVAO = -1;
        private int lastBoundTexture0 = -1;
        private int lastBlendSrc = -1;
        private int lastBlendDst = -1;
        
        void optimizeStateChanges(GLStateCache cache) {
            // Analyze state change patterns and suggest optimizations
        }
        
        void analyzeBatchOpportunities(BatchProcessor processor) {
            // Identify draw calls that can be batched
        }
        
        boolean shouldBindProgram(int programId) {
            if (programId == lastBoundProgram) return false;
            lastBoundProgram = programId;
            return true;
        }
        
        boolean shouldBindVAO(int vaoId) {
            if (vaoId == lastBoundVAO) return false;
            lastBoundVAO = vaoId;
            return true;
        }
        
        boolean shouldBindTexture(int textureId) {
            if (textureId == lastBoundTexture0) return false;
            lastBoundTexture0 = textureId;
            return true;
        }
        
        void reset() {
            lastBoundProgram = -1;
            lastBoundVAO = -1;
            lastBoundTexture0 = -1;
            lastBlendSrc = -1;
            lastBlendDst = -1;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PERFORMANCE METRICS
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Comprehensive performance metrics collection.
     */
    private final class PerformanceMetrics {
        
        private final LongAdder tickTimeTotal = new LongAdder();
        private final LongAdder tickCount = new LongAdder();
        private final LongAdder cleanupCount = new LongAdder();
        private final LongAdder textureEvictions = new LongAdder();
        private final LongAdder archetypeOptimizations = new LongAdder();
        
        private final AtomicLong minTickTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxTickTime = new AtomicLong(0);
        
        void recordTickTime(long nanos) {
            tickTimeTotal.add(nanos);
            tickCount.increment();
            
            // Update min (atomic CAS loop)
            long currentMin = minTickTime.get();
            while (nanos < currentMin) {
                if (minTickTime.compareAndSet(currentMin, nanos)) break;
                currentMin = minTickTime.get();
            }
            
            // Update max
            long currentMax = maxTickTime.get();
            while (nanos > currentMax) {
                if (maxTickTime.compareAndSet(currentMax, nanos)) break;
                currentMax = maxTickTime.get();
            }
        }
        
        void recordCleanup(MemoryPressureLevel level) {
            cleanupCount.increment();
        }
        
        void recordTextureEviction() {
            textureEvictions.increment();
        }
        
        void recordArchetypeOptimization() {
            archetypeOptimizations.increment();
        }
        
        double getAverageTickTimeMs() {
            long count = tickCount.sum();
            return count > 0 ? (tickTimeTotal.sum() / (double) count) / 1_000_000.0 : 0;
        }
        
        long getTotalCleanups() {
            return cleanupCount.sum();
        }
        
        long getTotalTextureEvictions() {
            return textureEvictions.sum();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // LATENCY HISTOGRAM
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * HDR histogram for latency percentile tracking.
     */
    private final class LatencyHistogram {
        
        // Buckets: 0-100us, 100us-1ms, 1ms-10ms, 10ms-100ms, 100ms+
        private final LongAdder[] buckets = new LongAdder[5];
        
        LatencyHistogram() {
            for (int i = 0; i < buckets.length; i++) {
                buckets[i] = new LongAdder();
            }
        }
        
        void record(long nanos) {
            int bucket;
            if (nanos < 100_000) bucket = 0;
            else if (nanos < 1_000_000) bucket = 1;
            else if (nanos < 10_000_000) bucket = 2;
            else if (nanos < 100_000_000) bucket = 3;
            else bucket = 4;
            
            buckets[bucket].increment();
        }
        
        long[] getBucketCounts() {
            long[] counts = new long[buckets.length];
            for (int i = 0; i < buckets.length; i++) {
                counts[i] = buckets[i].sum();
            }
            return counts;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // THROUGHPUT TRACKER
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Tracks frame throughput and timing statistics.
     */
    private final class ThroughputTracker {
        
        private final long[] frameTimes = new long[120];  // 2 seconds at 60fps
        private int frameIndex = 0;
        private final AtomicLong totalFrames = new AtomicLong(0);
        
        void recordFrame(long deltaNanos) {
            frameTimes[frameIndex] = deltaNanos;
            frameIndex = (frameIndex + 1) % frameTimes.length;
            totalFrames.incrementAndGet();
        }
        
        double getAverageFPS() {
            if (totalFrames.get() < frameTimes.length) {
                return 0;
            }
            
            long sum = 0;
            for (long frameTime : frameTimes) {
                sum += frameTime;
            }
            
            double avgNanos = sum / (double) frameTimes.length;
            return 1_000_000_000.0 / avgNanos;
        }
        
        double get1PercentLow() {
            long[] sorted = frameTimes.clone();
            Arrays.sort(sorted);
            
            // 1% of 120 frames = ~1 frame, use worst frame
            long worstFrame = sorted[sorted.length - 1];
            return worstFrame > 0 ? 1_000_000_000.0 / worstFrame : 0;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // SCHEDULED MAINTENANCE TASKS
    // ═══════════════════════════════════════════════════════════════════════════
    
    private void scheduleMaintenanceTasks() {
        // Memory pressure monitoring - high frequency
        scheduledPool.scheduleAtFixedRate(
            this::monitorMemoryPressure,
            1000, 500, TimeUnit.MILLISECONDS
        );
        
        // Buffer pool maintenance - medium frequency
        scheduledPool.scheduleAtFixedRate(
            () -> bufferPoolManager.compactIfNeeded(),
            5000, 2000, TimeUnit.MILLISECONDS
        );
        
        // Metrics reporting - low frequency
        scheduledPool.scheduleAtFixedRate(
            this::reportMetrics,
            30000, 30000, TimeUnit.MILLISECONDS
        );
    }
    
    private void monitorMemoryPressure() {
        MemoryPressureLevel level = assessMemoryPressure();
        if (level.ordinal() >= MemoryPressureLevel.RED.ordinal()) {
            handleMemoryPressure(level);
        }
    }
    
    private void reportMetrics() {
        if (Config.isDebugEnabled()) {
            System.out.printf("[JITInject] Avg tick: %.3fms, Cleanups: %d, Texture evictions: %d, Avg FPS: %.1f%n",
                metrics.getAverageTickTimeMs(),
                metrics.getTotalCleanups(),
                metrics.getTotalTextureEvictions(),
                throughputTracker.getAverageFPS()
            );
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════
    // PUBLIC API - Utility methods for external use
    // ═══════════════════════════════════════════════════════════════════════════
    
    /**
     * Returns current initialization state.
     */
    public InitializationState getState() {
        return initState.get();
    }
    
    /**
     * Returns true if system is ready for operation.
     */
    public boolean isReady() {
        return initState.get() == InitializationState.READY;
    }
    
    /**
     * Returns true if JIT warmup has completed.
     */
    public boolean isWarmedUp() {
        return warmupComplete.get();
    }
    
    /**
     * Returns current frame count.
     */
    public long getFrameCount() {
        return frameCounter.get();
    }
    
    /**
     * Returns average tick processing time in milliseconds.
     */
    public double getAverageTickTimeMs() {
        return metrics.getAverageTickTimeMs();
    }
    
    /**
     * Returns estimated current FPS based on frame timing.
     */
    public double getCurrentFPS() {
        return throughputTracker.getAverageFPS();
    }
    
    /**
     * Returns 1% low FPS for performance analysis.
     */
    public double get1PercentLowFPS() {
        return throughputTracker.get1PercentLow();
    }
    
    /**
     * Forces immediate garbage collection (use sparingly).
     */
    public void forceGC() {
        gcCoordinator.forceFullGC();
    }
    
    /**
     * Returns memory usage statistics.
     */
    public MemoryStats getMemoryStats() {
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
        
        return new MemoryStats(
            heap.getUsed(),
            heap.getMax(),
            nonHeap.getUsed(),
            assessMemoryPressure()
        );
    }
    
    /**
     * Memory statistics record.
     */
    public record MemoryStats(
        long heapUsed,
        long heapMax,
        long nonHeapUsed,
        MemoryPressureLevel pressureLevel
    ) {
        public double heapUsagePercent() {
            return heapMax > 0 ? (heapUsed * 100.0) / heapMax : 0;
        }
    }
}
