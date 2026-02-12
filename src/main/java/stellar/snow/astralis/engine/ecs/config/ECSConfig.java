package stellar.snow.astralis.engine.ecs.config;

/*
 * ════════════════════════════════════════════════════════════════════════════════════════════════
 * Astralis ECS Configuration System
 * 
 * Copyright © 2026 Astralis ECS Project
 * Licensed under PolyForm Shield License 1.0.0
 * ════════════════════════════════════════════════════════════════════════════════════════════════
 */

import stellar.snow.astralis.config.Config;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Comprehensive configuration system for Astralis ECS.
 * 
 * <h2>Purpose</h2>
 * <p>Centralized configuration management for all ECS subsystems including storage,
 * scheduling, compatibility layers, and performance tuning.</p>
 * 
 * <h2>Configuration Categories</h2>
 * <ul>
 *   <li><b>Storage Configuration</b> - Component storage, archetype management, memory allocation</li>
 *   <li><b>System Scheduling</b> - Parallel execution, dependency resolution, barrier orchestration</li>
 *   <li><b>Compatibility Layers</b> - API interoperability, migration support</li>
 *   <li><b>Performance Tuning</b> - Cache optimization, prefetching, SIMD operations</li>
 *   <li><b>Development Tools</b> - Profiling, debugging, validation</li>
 * </ul>
 * 
 * @author Astralis ECS Project
 * @version 1.0.0
 * @since Java 21
 */
public final class ECSConfig {

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    // ════════════════════════════════════════════════════════════════════════
    // STORAGE CONFIGURATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Enable off-heap component storage for reduced GC pressure.
     * Default: true
     */
    public static boolean useOffHeapStorage() {
        ensureInitialized();
        return Config.getBoolean("ecs.storage.offHeap", true);
    }

    /**
     * Initial capacity for component arrays (per archetype).
     * Default: 256
     */
    public static int componentArrayInitialCapacity() {
        ensureInitialized();
        return Config.getInt("ecs.storage.initialCapacity", 256);
    }

    /**
     * Growth factor for component array resizing.
     * Default: 1.5
     */
    public static double componentArrayGrowthFactor() {
        ensureInitialized();
        return Config.getDouble("ecs.storage.growthFactor", 1.5);
    }

    /**
     * Enable struct flattening for nested component optimization.
     * Default: true
     */
    public static boolean enableStructFlattening() {
        ensureInitialized();
        return Config.getBoolean("ecs.storage.structFlattening", true);
    }

    /**
     * Maximum depth for struct flattening recursion.
     * Default: 4
     */
    public static int structFlatteningMaxDepth() {
        ensureInitialized();
        return Config.getInt("ecs.storage.flatteningDepth", 4);
    }

    // ════════════════════════════════════════════════════════════════════════
    // SYSTEM SCHEDULING CONFIGURATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Enable parallel system execution.
     * Default: true
     */
    public static boolean enableParallelExecution() {
        ensureInitialized();
        return Config.getBoolean("ecs.scheduling.parallel", true);
    }

    /**
     * Number of worker threads for system execution.
     * Default: Runtime.getRuntime().availableProcessors() - 1
     */
    public static int workerThreadCount() {
        ensureInitialized();
        int defaultThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        return Config.getInt("ecs.scheduling.workerThreads", defaultThreads);
    }

    /**
     * Enable automatic dependency resolution.
     * Default: true
     */
    public static boolean autoResolveDependencies() {
        ensureInitialized();
        return Config.getBoolean("ecs.scheduling.autoDependencies", true);
    }

    /**
     * Enable adaptive workload balancing.
     * Default: true
     */
    public static boolean enableAdaptiveBalancing() {
        ensureInitialized();
        return Config.getBoolean("ecs.scheduling.adaptiveBalance", true);
    }

    /**
     * Minimum entities per task for parallel execution.
     * Default: 100
     */
    public static int minEntitiesPerTask() {
        ensureInitialized();
        return Config.getInt("ecs.scheduling.minTaskSize", 100);
    }

    // ════════════════════════════════════════════════════════════════════════
    // COMPATIBILITY LAYER CONFIGURATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Enable compatibility layer for third-party ECS integration.
     * Default: false (opt-in)
     */
    public static boolean enableCompatibilityLayer() {
        ensureInitialized();
        return Config.getBoolean("ecs.compatibility.enabled", false);
    }

    /**
     * Enable Forge EventBus integration.
     * Default: false (auto-detected in Forge environment)
     */
    public static boolean enableForgeIntegration() {
        ensureInitialized();
        return Config.getBoolean("ecs.compatibility.forge", false);
    }

    /**
     * Enable strict API compatibility mode (may reduce performance).
     * Default: false
     */
    public static boolean strictCompatibilityMode() {
        ensureInitialized();
        return Config.getBoolean("ecs.compatibility.strict", false);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PERFORMANCE TUNING
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Enable SIMD operations for component processing.
     * Default: true
     */
    public static boolean enableSIMD() {
        ensureInitialized();
        return Config.getBoolean("ecs.performance.simd", true);
    }

    /**
     * Enable prefetching for component access.
     * Default: true
     */
    public static boolean enablePrefetching() {
        ensureInitialized();
        return Config.getBoolean("ecs.performance.prefetch", true);
    }

    /**
     * Cache line size for alignment optimization.
     * Default: 64 bytes
     */
    public static int cacheLineSize() {
        ensureInitialized();
        return Config.getInt("ecs.performance.cacheLineSize", 64);
    }

    /**
     * Enable component change tracking.
     * Default: false (opt-in for reactive systems)
     */
    public static boolean enableChangeTracking() {
        ensureInitialized();
        return Config.getBoolean("ecs.performance.changeTracking", false);
    }

    // ════════════════════════════════════════════════════════════════════════
    // DEVELOPMENT TOOLS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Enable ECS profiler.
     * Default: false (development only)
     */
    public static boolean enableProfiler() {
        ensureInitialized();
        return Config.getBoolean("ecs.dev.profiler", false);
    }

    /**
     * Enable validation checks (bounds, null checks, etc).
     * Default: false (significant performance impact)
     */
    public static boolean enableValidation() {
        ensureInitialized();
        return Config.getBoolean("ecs.dev.validation", false);
    }

    /**
     * Enable detailed logging.
     * Default: false
     */
    public static boolean enableDetailedLogging() {
        ensureInitialized();
        return Config.getBoolean("ecs.dev.detailedLogs", false);
    }

    /**
     * Log system execution order.
     * Default: false
     */
    public static boolean logExecutionOrder() {
        ensureInitialized();
        return Config.getBoolean("ecs.dev.logExecutionOrder", false);
    }

    // ════════════════════════════════════════════════════════════════════════
    // MEMORY MANAGEMENT
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Enable automatic memory cleanup.
     * Default: true
     */
    public static boolean enableAutoCleanup() {
        ensureInitialized();
        return Config.getBoolean("ecs.memory.autoCleanup", true);
    }

    /**
     * Memory pool size for component allocation (in MB).
     * Default: 64 MB
     */
    public static int memoryPoolSize() {
        ensureInitialized();
        return Config.getInt("ecs.memory.poolSizeMB", 64);
    }

    /**
     * Enable memory leak detection.
     * Default: false (development only)
     */
    public static boolean enableLeakDetection() {
        ensureInitialized();
        return Config.getBoolean("ecs.memory.leakDetection", false);
    }

    // ════════════════════════════════════════════════════════════════════════
    // INTEGRATION FEATURES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Enable Minecraft-specific optimizations.
     * Default: auto-detected
     */
    public static boolean enableMinecraftOptimizations() {
        ensureInitialized();
        return Config.getBoolean("ecs.integration.minecraft", isMinecraftEnvironment());
    }

    /**
     * Enable entity prefab system.
     * Default: true
     */
    public static boolean enablePrefabSystem() {
        ensureInitialized();
        return Config.getBoolean("ecs.integration.prefabs", true);
    }

    /**
     * Enable event-driven component scanning.
     * Default: true
     */
    public static boolean enableEventDrivenScanning() {
        ensureInitialized();
        return Config.getBoolean("ecs.integration.eventScanning", true);
    }

    // ════════════════════════════════════════════════════════════════════════
    // INITIALIZATION AND UTILITIES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Initialize ECS configuration system.
     * Called automatically on first access.
     */
    public static void initialize() {
        if (initialized.compareAndSet(false, true)) {
            // Configuration is loaded from main Config system
            // This just marks ECS config as initialized
            if (enableDetailedLogging()) {
                System.out.println("[Astralis ECS] Configuration initialized");
            }
        }
    }

    /**
     * Ensure configuration is initialized.
     */
    private static void ensureInitialized() {
        if (!initialized.get()) {
            initialize();
        }
    }

    /**
     * Detect if running in Minecraft environment.
     */
    private static boolean isMinecraftEnvironment() {
        try {
            Class.forName("net.minecraft.client.Minecraft");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Reload configuration from file.
     */
    public static void reload() {
        Config.reload();
        if (enableDetailedLogging()) {
            System.out.println("[Astralis ECS] Configuration reloaded");
        }
    }

    /**
     * Get summary of current configuration.
     */
    public static String getConfigurationSummary() {
        ensureInitialized();
        return String.format("""
            Astralis ECS Configuration:
            ═══════════════════════════════════════════
            Storage: %s (capacity: %d, growth: %.2f)
            Scheduling: %s (%d threads)
            Compatibility: %s
            Performance: SIMD=%s, Prefetch=%s
            Development: Profiler=%s, Validation=%s
            ═══════════════════════════════════════════
            """,
            useOffHeapStorage() ? "Off-Heap" : "On-Heap",
            componentArrayInitialCapacity(),
            componentArrayGrowthFactor(),
            enableParallelExecution() ? "Parallel" : "Sequential",
            workerThreadCount(),
            enableCompatibilityLayer() ? "Enabled" : "Disabled",
            enableSIMD(),
            enablePrefetching(),
            enableProfiler(),
            enableValidation()
        );
    }

    // Private constructor to prevent instantiation
    private ECSConfig() {
        throw new AssertionError("Configuration class cannot be instantiated");
    }
}
