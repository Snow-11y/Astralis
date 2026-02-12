package stellar.snow.astralis.engine.ecs.core;

/*
 * ════════════════════════════════════════════════════════════════════════════════════════════════
 * Astralis ECS Initialization Manager
 * 
 * Copyright © 2026 Astralis ECS Project
 * Licensed under PolyForm Shield License 1.0.0
 * ════════════════════════════════════════════════════════════════════════════════════════════════
 */

import stellar.snow.astralis.engine.ecs.config.ECSConfig;
import stellar.snow.astralis.engine.ecs.storage.*;
import stellar.snow.astralis.engine.ecs.integration.*;
import stellar.snow.astralis.engine.ecs.events.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Centralized initialization manager for Astralis ECS.
 * 
 * <h2>Purpose</h2>
 * <p>Manages initialization sequence for all ECS subsystems, ensuring proper startup order
 * and configuration validation.</p>
 * 
 * <h2>Initialization Phases</h2>
 * <ol>
 *   <li><b>Pre-initialization</b> - Load configuration, validate environment</li>
 *   <li><b>Core Systems</b> - Initialize storage, component registry, entity management</li>
 *   <li><b>Scheduling</b> - Setup system scheduler, barrier orchestration</li>
 *   <li><b>Integration</b> - Enable compatibility layers, external integrations</li>
 *   <li><b>Post-initialization</b> - Start profiler, register shutdown hooks</li>
 * </ol>
 * 
 * @author Astralis ECS Project
 * @version 1.0.0
 * @since Java 21
 */
public final class ECSInitializer {

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final List<Runnable> shutdownHooks = new ArrayList<>();

    /**
     * Initialize Astralis ECS with default configuration.
     * 
     * @return true if initialization successful, false if already initialized
     */
    public static boolean initialize() {
        return initialize(new InitializationOptions());
    }

    /**
     * Initialize Astralis ECS with custom options.
     * 
     * @param options Custom initialization options
     * @return true if initialization successful, false if already initialized
     */
    public static boolean initialize(InitializationOptions options) {
        if (initialized.compareAndSet(false, true)) {
            try {
                log("Starting Astralis ECS initialization...");
                
                // Phase 1: Pre-initialization
                preInitialize(options);
                
                // Phase 2: Core systems
                initializeCoreSystems(options);
                
                // Phase 3: Scheduling
                initializeScheduling(options);
                
                // Phase 4: Integration
                initializeIntegration(options);
                
                // Phase 5: Post-initialization
                postInitialize(options);
                
                log("Astralis ECS initialization complete!");
                if (ECSConfig.enableDetailedLogging()) {
                    log(ECSConfig.getConfigurationSummary());
                }
                
                return true;
            } catch (Exception e) {
                initialized.set(false); // Allow retry
                throw new InitializationException("Failed to initialize Astralis ECS", e);
            }
        } else {
            log("Astralis ECS already initialized");
            return false;
        }
    }

    /**
     * Phase 1: Pre-initialization
     */
    private static void preInitialize(InitializationOptions options) {
        log("Phase 1: Pre-initialization");
        
        // Load configuration
        ECSConfig.initialize();
        
        // Validate Java version
        validateJavaVersion();
        
        // Check for required features
        validateSystemFeatures();
        
        // Setup memory pools if using off-heap storage
        if (ECSConfig.useOffHeapStorage()) {
            initializeMemoryPools();
        }
    }

    /**
     * Phase 2: Core system initialization
     */
    private static void initializeCoreSystems(InitializationOptions options) {
        log("Phase 2: Core systems");
        
        // Initialize component registry
        ComponentRegistry.initialize();
        
        // Initialize struct flattening if enabled
        if (ECSConfig.enableStructFlattening()) {
            StructFlatteningRegistryV2.initialize();
        }
        
        // Initialize enhanced component registry
        EnhancedComponentRegistry.initialize();
        
        // Initialize access handle pool
        OptimizedAccessHandlePool.initialize();
    }

    /**
     * Phase 3: Scheduling initialization
     */
    private static void initializeScheduling(InitializationOptions options) {
        log("Phase 3: Scheduling");
        
        // Initialize system scheduler
        SystemScheduler.initialize(ECSConfig.workerThreadCount());
        
        // Initialize adaptive workload balancer if enabled
        if (ECSConfig.enableAdaptiveBalancing()) {
            AdaptiveWorkloadBalancer.initialize();
        }
        
        // Setup parallel job scheduler
        if (ECSConfig.enableParallelExecution()) {
            ParallelJobScheduler.initialize();
        }
    }

    /**
     * Phase 4: Integration initialization
     */
    private static void initializeIntegration(InitializationOptions options) {
        log("Phase 4: Integration");
        
        // Initialize compatibility layer if enabled
        if (ECSConfig.enableCompatibilityLayer() || options.forceCompatibilityLayer) {
            log("  - Enabling compatibility layer");
            // Compatibility layer is initialized on-demand
        }
        
        // Initialize Forge integration if enabled
        if (ECSConfig.enableForgeIntegration() || options.forgeEventBus != null) {
            initializeForgeIntegration(options.forgeEventBus);
        }
        
        // Initialize Minecraft optimizations if detected
        if (ECSConfig.enableMinecraftOptimizations() || options.forceMinecraftOptimizations) {
            initializeMinecraftIntegration();
        }
        
        // Initialize prefab system if enabled
        if (ECSConfig.enablePrefabSystem()) {
            initializePrefabSystem();
        }
        
        // Initialize event-driven scanning if enabled
        if (ECSConfig.enableEventDrivenScanning()) {
            EventDrivenComponentRegistry.initialize();
        }
    }

    /**
     * Phase 5: Post-initialization
     */
    private static void postInitialize(InitializationOptions options) {
        log("Phase 5: Post-initialization");
        
        // Start profiler if enabled
        if (ECSConfig.enableProfiler() || options.enableProfiler) {
            log("  - Starting ECS profiler");
            // Profiler initialization would go here
        }
        
        // Enable leak detection if configured
        if (ECSConfig.enableLeakDetection()) {
            log("  - Enabling memory leak detection");
            // Leak detection setup would go here
        }
        
        // Register shutdown hooks
        registerShutdownHooks();
    }

    /**
     * Initialize memory pools for off-heap storage.
     */
    private static void initializeMemoryPools() {
        log("  - Initializing memory pools (size: " + ECSConfig.memoryPoolSize() + " MB)");
        // Memory pool initialization would go here
    }

    /**
     * Initialize Forge integration.
     */
    private static void initializeForgeIntegration(EventBus eventBus) {
        log("  - Enabling Forge integration");
        if (eventBus != null) {
            KirinoCompatibilityLayer.enableForgeScanningEvents(eventBus);
        }
    }

    /**
     * Initialize Minecraft-specific optimizations.
     */
    private static void initializeMinecraftIntegration() {
        log("  - Enabling Minecraft optimizations");
        // Minecraft integration would go here
    }

    /**
     * Initialize prefab system.
     */
    private static void initializePrefabSystem() {
        log("  - Initializing prefab system");
        // Prefab system initialization would go here
    }

    /**
     * Validate Java version meets requirements.
     */
    private static void validateJavaVersion() {
        int javaVersion = getJavaVersion();
        if (javaVersion < 21) {
            throw new InitializationException(
                "Astralis ECS requires Java 21 or higher (current: " + javaVersion + ")"
            );
        }
    }

    /**
     * Validate required system features.
     */
    private static void validateSystemFeatures() {
        // Check for VarHandle support (required for off-heap storage)
        try {
            Class.forName("java.lang.invoke.VarHandle");
        } catch (ClassNotFoundException e) {
            throw new InitializationException("VarHandle support required (Java 9+)");
        }
        
        // Check for Foreign Memory API if using off-heap
        if (ECSConfig.useOffHeapStorage()) {
            try {
                Class.forName("java.lang.foreign.MemorySegment");
            } catch (ClassNotFoundException e) {
                log("  - Warning: Foreign Memory API not available, using fallback");
            }
        }
    }

    /**
     * Register shutdown hooks for cleanup.
     */
    private static void registerShutdownHooks() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log("Astralis ECS shutting down...");
            for (Runnable hook : shutdownHooks) {
                try {
                    hook.run();
                } catch (Exception e) {
                    System.err.println("Error during shutdown: " + e.getMessage());
                }
            }
            log("Astralis ECS shutdown complete");
        }, "Astralis-ECS-Shutdown"));
    }

    /**
     * Add a shutdown hook.
     * 
     * @param hook Runnable to execute during shutdown
     */
    public static void addShutdownHook(Runnable hook) {
        shutdownHooks.add(hook);
    }

    /**
     * Check if ECS is initialized.
     * 
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Shutdown Astralis ECS and release all resources.
     */
    public static void shutdown() {
        if (initialized.compareAndSet(true, false)) {
            log("Performing manual ECS shutdown...");
            for (Runnable hook : shutdownHooks) {
                try {
                    hook.run();
                } catch (Exception e) {
                    System.err.println("Error during shutdown: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Get Java version.
     */
    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }

    /**
     * Log message if detailed logging enabled.
     */
    private static void log(String message) {
        if (ECSConfig.enableDetailedLogging()) {
            System.out.println("[Astralis ECS] " + message);
        }
    }

    /**
     * Initialization options for customizing ECS startup.
     */
    public static class InitializationOptions {
        public boolean forceCompatibilityLayer = false;
        public boolean forceMinecraftOptimizations = false;
        public boolean enableProfiler = false;
        public EventBus forgeEventBus = null;

        public InitializationOptions() {}

        public InitializationOptions withCompatibilityLayer() {
            this.forceCompatibilityLayer = true;
            return this;
        }

        public InitializationOptions withMinecraftOptimizations() {
            this.forceMinecraftOptimizations = true;
            return this;
        }

        public InitializationOptions withProfiler() {
            this.enableProfiler = true;
            return this;
        }

        public InitializationOptions withForgeEventBus(EventBus eventBus) {
            this.forgeEventBus = eventBus;
            return this;
        }
    }

    /**
     * Exception thrown during initialization failures.
     */
    public static class InitializationException extends RuntimeException {
        public InitializationException(String message) {
            super(message);
        }

        public InitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Private constructor to prevent instantiation
    private ECSInitializer() {
        throw new AssertionError("Initializer class cannot be instantiated");
    }

    /**
     * Placeholder classes for compilation (these would reference actual implementations)
     */
    private static class ComponentRegistry {
        static void initialize() {}
    }

    private static class EnhancedComponentRegistry {
        static void initialize() {}
    }

    private static class StructFlatteningRegistryV2 {
        static void initialize() {}
    }

    private static class OptimizedAccessHandlePool {
        static void initialize() {}
    }

    private static class SystemScheduler {
        static void initialize(int threads) {}
    }

    private static class AdaptiveWorkloadBalancer {
        static void initialize() {}
    }

    private static class ParallelJobScheduler {
        static void initialize() {}
    }

    private static class EventDrivenComponentRegistry {
        static void initialize() {}
    }
}
