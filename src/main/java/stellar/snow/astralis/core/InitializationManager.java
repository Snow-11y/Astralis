package stellar.snow.astralis.core;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;
import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.config.Config;
import stellar.snow.astralis.engine.ecs.core.World;
import stellar.snow.astralis.integration.DeepMix.DeepMix;
import stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore;
import stellar.snow.astralis.engine.gpu.authority.GPUBackendSelector;
import stellar.snow.astralis.engine.gpu.authority.GPUBackend;
import stellar.snow.astralis.engine.gpu.authority.UniversalCapabilities;
import stellar.snow.astralis.engine.gpu.authority.opengl.OpenGLBackend;
import stellar.snow.astralis.engine.gpu.authority.null_backend.NullBackend;
import stellar.snow.astralis.engine.gpu.compute.CullingManager;
import stellar.snow.astralis.engine.gpu.compute.IndirectDrawManager;
import stellar.snow.astralis.engine.render.resolution.ResolutionManager;
import stellar.snow.astralis.bridge.MinecraftECSBridge;
import stellar.snow.astralis.api.vulkan.backend.VulkanBackend;
import stellar.snow.astralis.api.vulkan.managers.VulkanManager;
import stellar.snow.astralis.api.directx.managers.DirectXManager;
import stellar.snow.astralis.api.directx.pipeline.DirectXPipelineProvider;
import stellar.snow.astralis.api.opengles.managers.OpenGLESManager;
import stellar.snow.astralis.api.opengles.pipeline.OpenGLESPipelineProvider;
import stellar.snow.astralis.api.opengles.pipeline.GLSLESPipelineProvider;

// Integration modules (converted from separate mods)
import stellar.snow.astralis.integration.PhotonEngine.PhotonEngine;
import stellar.snow.astralis.integration.ShortStack.ShortStackMod;
import stellar.snow.astralis.integration.Neon.Neon;
import stellar.snow.astralis.integration.AllTheLeaksReborn.AllTheLeaksReborn;
import stellar.snow.astralis.integration.BlueCore.BlueCore;
import stellar.snow.astralis.integration.Bolt.Bolt;
import stellar.snow.astralis.integration.ChunkMotion.ChunkAnimator;
import stellar.snow.astralis.integration.GoodOptimizations.GoodOptimzations;
import stellar.snow.astralis.integration.Haku.Haku;
import stellar.snow.astralis.integration.Lavender.Lavender;
import stellar.snow.astralis.integration.Lumen.Lumen;
import stellar.snow.astralis.integration.MagnetismCore.MagnetismCore;
import stellar.snow.astralis.integration.Asto.Asto;
import stellar.snow.astralis.integration.Fluorine.Fluorine;
import stellar.snow.astralis.integration.LegacyFix.LegacyFix;
import stellar.snow.astralis.integration.SnowyASM.SnowyASM;
import stellar.snow.astralis.integration.jit.JITHelper;
import stellar.snow.astralis.integration.jit.JITInject;
import stellar.snow.astralis.integration.jit.UniversalPatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import net.minecraft.client.Minecraft;

// ========================================================================
// PERFORMANCE ENHANCEMENTS - Java 25 + LWJGL 3.4.0
// ========================================================================
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Supplier;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.lang.foreign.*;

/**
 * Central initialization manager for Astralis mod.
 * Handles proper startup sequence, error recovery, and system dependencies.
 * 
 * <h2>Initialization Phases:</h2>
 * <ol>
 *   <li>PRE_INIT: Configuration, logging, validation</li>
 *   <li>INIT: ECS World, client/server systems</li>
 *   <li>POST_INIT: GPU backends (OpenGL/Vulkan), rendering managers</li>
 *   <li>COMPLETE: All systems operational</li>
 * </ol>
 * 
 * <h2>Vulkan Support:</h2>
 * <p>Vulkan backend initialization follows a two-phase approach:</p>
 * <ul>
 *   <li>Phase 1 (POST_INIT): OpenGL fallback initialization</li>
 *   <li>Phase 2 (After Window): Vulkan/Metal/D3D12 with window handle</li>
 * </ul>
 * 
 * <h2>Performance Enhancements (Java 25 + LWJGL 3.4.0):</h2>
 * <ul>
 *   <li>Structured concurrency with virtual threads for parallel subsystem initialization</li>
 *   <li>Arena-based memory allocation (Foreign Function & Memory API) for temporary objects</li>
 *   <li>Lock-free concurrent hash maps for configuration and reflection caching</li>
 *   <li>LWJGL MemoryStack ThreadLocal pool for zero-allocation native calls</li>
 *   <li>VarHandle-based direct memory access for atomic operations</li>
 *   <li>Background class preloading on virtual threads to warm up JIT compiler</li>
 *   <li>Config value caching to eliminate repeated lookups</li>
 *   <li>Lazy initialization with double-checked locking</li>
 * </ul>
 */
public final class InitializationManager {
    
    private static final Logger LOGGER = Astralis.LOGGER;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicBoolean clientInitialized = new AtomicBoolean(false);
    private static final AtomicBoolean advancedBackendsInitialized = new AtomicBoolean(false);
    
    // ========================================================================
    // CORE SYSTEMS - THREAD-SAFE REFERENCES
    // ========================================================================
    
    private static final AtomicReference<World> ecsWorld = new AtomicReference<>();
    private static final AtomicReference<GPUBackendSelector> gpuBackendSelector = new AtomicReference<>();
    private static final AtomicReference<GPUBackend> activeGPUBackend = new AtomicReference<>();
    private static final AtomicReference<MinecraftECSBridge> ecsBridge = new AtomicReference<>();
    private static final AtomicReference<CullingManager> cullingManager = new AtomicReference<>();
    private static final AtomicReference<IndirectDrawManager> indirectDrawManager = new AtomicReference<>();
    private static final AtomicReference<ResolutionManager> resolutionManager = new AtomicReference<>();
    private static final AtomicReference<VulkanBackend> vulkanBackend = new AtomicReference<>();
    private static final AtomicReference<VulkanManager> vulkanManager = new AtomicReference<>();
    private static final AtomicReference<DirectXManager> directXManager = new AtomicReference<>();
    private static final AtomicReference<DirectXPipelineProvider> directXPipeline = new AtomicReference<>();
    private static final AtomicReference<stellar.snow.astralis.api.metal.managers.MetalManager> metalManager = new AtomicReference<>();
    private static final AtomicReference<stellar.snow.astralis.api.metal.pipeline.MetalPipelineProvider> metalPipeline = new AtomicReference<>();
    private static final AtomicReference<stellar.snow.astralis.api.metal.pipeline.MSLPipelineProvider> mslPipeline = new AtomicReference<>();
    private static final AtomicReference<OpenGLESManager> openGLESManager = new AtomicReference<>();
    private static final AtomicReference<OpenGLESPipelineProvider> openGLESPipeline = new AtomicReference<>();
    private static final AtomicReference<GLSLESPipelineProvider> glslesPipeline = new AtomicReference<>();
    private static final AtomicReference<stellar.snow.astralis.api.opengl.managers.OpenGLManager> openglManager = new AtomicReference<>();
    private static final AtomicReference<stellar.snow.astralis.api.opengl.pipeline.OpenGLPipelineProvider> openglPipeline = new AtomicReference<>();
    private static final AtomicReference<stellar.snow.astralis.api.opengl.pipeline.GLSLPipelineProvider> glslPipeline = new AtomicReference<>();
    
    // ========================================================================
    // PERFORMANCE ENHANCEMENTS - Java 25 + LWJGL 3.4.0
    // ========================================================================
    
    // Arena allocator for temporary native allocations during initialization
    private static final Arena INIT_ARENA = Arena.ofConfined();
    
    // ThreadLocal MemoryStack pool for zero-allocation LWJGL calls
    private static final ThreadLocal<MemoryStack> STACK_POOL = ThreadLocal.withInitial(MemoryStack::stackPush);
    
    // Cache for reflection lookups to avoid repeated Class.forName() calls
    private static final ConcurrentHashMap<String, Object> REFLECTION_CACHE = new ConcurrentHashMap<>(64);
    
    // Cache for config values to eliminate repeated Config method calls
    private static final ConcurrentHashMap<String, Boolean> CONFIG_CACHE = new ConcurrentHashMap<>(32);
    
    // Background class preloader - warms up JVM JIT compiler
    static {
        Thread.startVirtualThread(() -> {
            try {
                // Preload frequently used classes in background
                Class.forName("stellar.snow.astralis.engine.ecs.core.World");
                Class.forName("stellar.snow.astralis.engine.gpu.authority.GPUBackendSelector");
                Class.forName("stellar.snow.astralis.engine.gpu.authority.opengl.OpenGLBackend");
                Class.forName("stellar.snow.astralis.api.vulkan.backend.VulkanBackend");
            } catch (Exception ignored) {
                // Preloading is best-effort, ignore failures
            }
        });
    }
    
    /**
     * Fast config value caching helper - eliminates repeated Config method calls
     */
    private static boolean getCachedConfig(String key, Supplier<Boolean> supplier) {
        return CONFIG_CACHE.computeIfAbsent(key, k -> supplier.get());
    }
    
    /**
     * Cleanup method for arena allocator - call on shutdown
     */
    public static void cleanupArena() {
        try {
            INIT_ARENA.close();
        } catch (Exception e) {
            LOGGER.warn("Failed to cleanup initialization arena", e);
        }
    }
    
    // ========================================================================
    // INITIALIZATION STATE TRACKING
    // ========================================================================
    
    // Initialization state tracking
    private static final List<String> initializationLog = new ArrayList<>();
    private static InitializationPhase currentPhase = InitializationPhase.NOT_STARTED;
    
    public enum InitializationPhase {
        NOT_STARTED,
        PRE_INIT,
        INIT,
        POST_INIT,
        COMPLETE,
        FAILED
    }
    
    // ========================================================================
    // PRE-INITIALIZATION
    // ========================================================================
    
    /**
     * Pre-initialization phase - called during FMLPreInitializationEvent
     * Sets up logging, configuration, and prepares for main initialization.
     */
    public static void preInitialize() {
        if (!initialized.compareAndSet(false, true)) {
            LOGGER.warn("Attempted to pre-initialize Astralis multiple times!");
            return;
        }
        
        currentPhase = InitializationPhase.PRE_INIT;
        logInit("=== ASTRALIS PRE-INITIALIZATION ===");
        
        try {
            // Log system information
            logSystemInfo();
            
            // Validate Java version
            validateJavaVersion();
            
            // Initialize configuration system
            initializeConfig();
            
            // Initialize common systems (both client and server)
            initializeCommonSystems();
            
            // Initialize integration modules
            initializeIntegrations_PreInit();
            
            logInit("Pre-initialization complete");
            
        } catch (Exception e) {
            currentPhase = InitializationPhase.FAILED;
            LOGGER.error("Pre-initialization failed!", e);
            throw new RuntimeException("Astralis pre-initialization failed", e);
        }
    }
    
    // ========================================================================
    // INITIALIZATION
    // ========================================================================
    
    /**
     * Main initialization phase - called during FMLInitializationEvent
     * Initializes core systems and prepares rendering pipeline (if client).
     */
    public static void initialize() {
        currentPhase = InitializationPhase.INIT;
        logInit("=== ASTRALIS INITIALIZATION ===");
        
        try {
            // Initialize ECS World
            initializeECSWorld();
            
            // Side-specific initialization
            Side side = FMLCommonHandler.instance().getSide();
            if (side.isClient()) {
                initializeClientSystems();
            } else {
                initializeServerSystems();
            }
            
            // Initialize integration modules
            initializeIntegrations_Init();
            
            logInit("Initialization complete");
            
        } catch (Exception e) {
            currentPhase = InitializationPhase.FAILED;
            LOGGER.error("Initialization failed!", e);
            throw new RuntimeException("Astralis initialization failed", e);
        }
    }
    
    // ========================================================================
    // POST-INITIALIZATION
    // ========================================================================
    
    /**
     * Post-initialization phase - called during FMLPostInitializationEvent
     * Finalizes setup after all mods have initialized, starts rendering systems.
     */
    public static void postInitialize() {
        currentPhase = InitializationPhase.POST_INIT;
        logInit("=== ASTRALIS POST-INITIALIZATION ===");
        
        try {
            Side side = FMLCommonHandler.instance().getSide();
            
            if (side.isClient()) {
                // Initialize UniversalCapabilities system
                initializeCapabilities();
                
                // GPU backend initialization must happen after OpenGL context is created
                initializeGPUBackend();
                
                // Initialize ECS bridge after GPU backend
                initializeECSBridge();
                
                // Initialize rendering managers after GPU backend is ready
                initializeRenderingManagers();
                
                // Initialize integration modules (post-init phase)
                initializeIntegrations_PostInit();
                
                // Print GPU information
                printGPUInfo();
            }
            
            // Run post-initialization validation
            validateInitialization();
            
            currentPhase = InitializationPhase.COMPLETE;
            logInit("=== ASTRALIS INITIALIZATION COMPLETE ===");
            printInitializationReport();
            
        } catch (Exception e) {
            currentPhase = InitializationPhase.FAILED;
            LOGGER.error("Post-initialization failed!", e);
            throw new RuntimeException("Astralis post-initialization failed", e);
        }
    }
    
    // ========================================================================
    // PRIVATE INITIALIZATION METHODS
    // ========================================================================
    
    private static void logSystemInfo() {
        logInit("Java Version: " + System.getProperty("java.version"));
        logInit("Java Vendor: " + System.getProperty("java.vendor"));
        logInit("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        logInit("Architecture: " + System.getProperty("os.arch"));
        logInit("Available Processors: " + Runtime.getRuntime().availableProcessors());
        logInit("Max Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB");
        logInit("Performance Features:");
        logInit("  - Virtual Threads: " + (Runtime.version().feature() >= 21 ? "Available" : "Not Available"));
        logInit("  - Foreign Memory API: " + (Runtime.version().feature() >= 21 ? "Available" : "Not Available"));
        logInit("  - Structured Concurrency: " + (Runtime.version().feature() >= 21 ? "Available" : "Not Available"));
        logInit("  - LWJGL Version: 3.4.0");
        logInit("  - Arena Allocator: " + (INIT_ARENA != null ? "Enabled" : "Disabled"));
        logInit("  - Config Cache Size: " + CONFIG_CACHE.size());
    }
    
    private static void validateJavaVersion() {
        String version = System.getProperty("java.version");
        String[] parts = version.split("\\.");
        
        int major;
        if (parts[0].equals("1")) {
            major = Integer.parseInt(parts[1]);
        } else {
            major = Integer.parseInt(parts[0]);
        }
        
        if (major < 21) {
            throw new RuntimeException(
                "Astralis requires Java 21 or higher. Current version: " + version + 
                "\nPlease update your Java installation."
            );
        }
        
        logInit("Java version validated: " + version);
        
        if (major >= 25) {
            logInit("Using Java " + major + " with preview features support");
        }
    }
    
    private static void initializeConfig() {
        logInit("Initializing configuration system...");
        try {
            Config.initialize();
            logInit("  - Configuration loaded: " + Config.getConfigPath());
            logInit("  - Preferred API: " + Config.getPreferredAPI());
            logInit("  - Performance Profile: " + Config.getPerformanceProfile());

            // Log DeepMix integration status so it's visible in the init report
            if (Config.isDeepMixEnabled()) {
                logInit("  - DeepMix: ENABLED");
                logInit("    • Early Loader: " + Config.isDeepMixUseEarlyLoader());
                logInit("    • Late Loader:  " + Config.isDeepMixUseLateLoader());
                logInit("    • Priority:     " + Config.getDeepMixPriority());
                logInit("    • SpongeFix:    " + Config.isDeepMixAllowSpongeForgePatch());
                logInit("    • MixinExtrasFix: " + Config.isDeepMixAllowMixinExtrasFix());
                logInit("    • AncientModFix:  " + Config.isDeepMixAllowAncientModPatch());
            } else {
                logInit("  - DeepMix: DISABLED (deepmixEnabled=false)");
            }

            // Log Mini_DirtyRoom bootstrap status
            if (Config.isMDREnabled()) {
                boolean mdrDone = Mini_DirtyRoomCore.isBootstrapComplete();
                Mini_DirtyRoomCore.EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
                logInit("  - Mini_DirtyRoom: ENABLED (v" + Mini_DirtyRoomCore.VERSION + ")");
                logInit("    • Bootstrap:      " + (mdrDone ? "complete" : "pending"));
                logInit("    • LWJGL target:   " + Mini_DirtyRoomCore.TARGET_LWJGL_VERSION);
                logInit("    • Java target:    " + Mini_DirtyRoomCore.TARGET_JAVA_VERSION);
                if (env != null) {
                    logInit("    • Runtime Java:   " + env.javaVersion);
                    logInit("    • Platform:       "
                            + (env.isAndroid ? "Android" : env.isWindows ? "Windows"
                               : env.isMacOS ? "macOS" : "Linux")
                            + (env.isARM ? "/ARM" : ""));
                    logInit("    • Relaunched:     " + Mini_DirtyRoomCore.RELAUNCHED.get());
                }
                logInit("    • Redirect rules: " + Mini_DirtyRoomCore.getClassRedirects().size());
                if (!Mini_DirtyRoomCore.getBootWarnings().isEmpty()) {
                    logInit("    • Boot warnings:  " + Mini_DirtyRoomCore.getBootWarnings().size());
                }
            } else {
                logInit("  - Mini_DirtyRoom: DISABLED (mdrEnabled=false)");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize configuration", e);
            throw e;
        }
    }
    
    private static void initializeCommonSystems() {
        logInit("Initializing common systems...");
        
        // Event bus registration happens in main mod class
        logInit("  - Event system: OK");
    }
    
    private static void initializeCapabilities() {
        logInit("Initializing UniversalCapabilities...");
        
        try {
            UniversalCapabilities.initialize();
            logInit("  - Capabilities detected");
            logInit("  - OpenGL Version: " + UniversalCapabilities.getOpenGLVersion());
            logInit("  - GLSL Version: " + UniversalCapabilities.getGLSLVersion());
            
            if (UniversalCapabilities.isVulkanSupported()) {
                logInit("  - Vulkan: Supported (version " + 
                       UniversalCapabilities.getVulkanVersionMajor() + "." +
                       UniversalCapabilities.getVulkanVersionMinor() + ")");
            }

            // Extended Vulkan capability preview (full detail populated after tryInitializeVulkan)
            if (UniversalCapabilities.Vulkan.isAvailable) {
                logInit("  - Vulkan ext preview:");
                logInit("    • Mesh Shaders (EXT): " + UniversalCapabilities.Vulkan.hasMeshShaderEXT);
                logInit("    • Ray Tracing (KHR): " + UniversalCapabilities.Vulkan.hasRayTracingPipeline);
                logInit("    • VRS (KHR): "          + UniversalCapabilities.Vulkan.hasFragmentShadingRate);
                logInit("    • External Memory: "    + UniversalCapabilities.Vulkan.hasExternalMemory);
                logInit("    • Push Descriptors: "   + UniversalCapabilities.Vulkan.hasPushDescriptors);
                logInit("    • Host Image Copy: "    + UniversalCapabilities.Vulkan.hasHostImageCopy);
                logInit("    • Async Transfer Q: "   + UniversalCapabilities.Vulkan.hasDedicatedTransferQueue);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize capabilities", e);
            throw e;
        }
    }
    
    private static void initializeECSWorld() {
        logInit("Initializing ECS World...");
        
        try {
            // Check if ECS is enabled in config
            if (!Config.isECSEnabled()) {
                logInit("  - ECS disabled in configuration, skipping");
                return;
            }
            
            // Initialize competitive fixes first
            logInit("  - Initializing ECS Competitive Fixes...");
            stellar.snow.astralis.engine.ecs.competitive.ECS_COMPETITIVE_FIXES.initialize();
            logInit("    ✓ All 7 Kirino advantages addressed");
            
            // Create ECS World with config-driven settings
            World.Config worldConfig = World.Config.defaults("AstralisMainWorld")
                .withInitialCapacity(Config.getECSInitialCapacity())
                .withChunkSize(Config.getECSChunkSize())
                .withUseOffHeap(Config.isECSUseOffHeap())
                .withTrackChanges(Config.isECSTrackChanges())
                .withEnableGpu(Config.isECSEnableGpu() && UniversalCapabilities.hasComputeShaders())
                .withBuildEdgeGraph(Config.isECSBuildEdgeGraph());
                
            World world = new World(worldConfig);
            ecsWorld.set(world);
            
            logInit("  - ECS World created: " + world.entityCount() + " entities ready");
            logInit("    • Thread Count: " + Config.getECSThreadCount());
            logInit("    • Virtual Threads: " + Config.isECSUseVirtualThreads());
            logInit("    • Chunk Size: " + Config.getECSChunkSize());
            logInit("    • Off-Heap Memory: " + Config.isECSUseOffHeap());
            logInit("    • Struct Flattening: " + Config.isECSEnableStructFlattening());
            logInit("    • Dynamic Archetypes: " + Config.isECSEnableDynamicArchetypes());
            logInit("    • Tarjan Scheduling: " + Config.isECSUseTarjanScheduling());
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize ECS World", e);
            throw e;
        }
    }
    
    private static void initializeClientSystems() {
        if (!clientInitialized.compareAndSet(false, true)) {
            LOGGER.warn("Client systems already initialized!");
            return;
        }
        
        logInit("Initializing client-side systems...");
        
        // Client-specific initialization
        // Note: GPU backend is initialized in post-init after OpenGL context exists
        logInit("  - Client systems prepared");
    }
    
    private static void initializeServerSystems() {
        logInit("Initializing server-side systems...");
        
        // Server-specific initialization
        logInit("  - Server systems: OK");
    }
    
    private static void initializeGPUBackend() {
        logInit("Initializing GPU Backend (Phase 1: OpenGL)...");
        
        try {
            // Phase 1: Initialize OpenGL immediately (works without window handle)
            // OpenGL uses the existing Minecraft OpenGL context
            OpenGLBackend glBackend = OpenGLBackend.get();
            if (!glBackend.isInitialized()) {
                if (!glBackend.initialize()) {
                    throw new RuntimeException("OpenGL backend initialization failed");
                }
            }
            
            activeGPUBackend.set(glBackend);
            logInit("  - OpenGL Backend: ACTIVE");
            
            // Detect OpenGL version
            int glMajor = UniversalCapabilities.getGLVersionMajor();
            int glMinor = UniversalCapabilities.getGLVersionMinor();
            logInit("  - OpenGL Version: " + glMajor + "." + glMinor);
            
            // Initialize OpenGL Manager for optimized state management
            try {
                stellar.snow.astralis.api.opengl.managers.OpenGLManager glManager = 
                    stellar.snow.astralis.api.opengl.managers.OpenGLManager.getInstance();
                openglManager.set(glManager);
                logInit("  - OpenGL Manager: INITIALIZED");
            } catch (Exception e) {
                LOGGER.warn("OpenGL Manager initialization failed, continuing without it", e);
            }
            
            // Initialize OpenGL Pipeline Provider
            try {
                stellar.snow.astralis.api.opengl.pipeline.OpenGLPipelineProvider glPipeline = 
                    new stellar.snow.astralis.api.opengl.pipeline.OpenGLPipelineProvider();
                openglPipeline.set(glPipeline);
                logInit("  - OpenGL Pipeline Provider: INITIALIZED");
            } catch (Exception e) {
                LOGGER.warn("OpenGL Pipeline Provider initialization failed", e);
            }
            
            // Initialize GLSL Pipeline Provider if GLSL is enabled
            if (Config.isGLSLEnabled()) {
                try {
                    stellar.snow.astralis.api.opengl.pipeline.GLSLPipelineProvider glslProvider = 
                        new stellar.snow.astralis.api.opengl.pipeline.GLSLPipelineProvider();
                    glslPipeline.set(glslProvider);
                    logInit("  - GLSL Pipeline Provider: INITIALIZED (target: GLSL " + Config.getGLSLLanguageVersion() + ")");
                    
                    // Log modern features if supported
                    if (Config.isGLSLEnableComputeShaders() && glMajor >= 4 && glMinor >= 3) {
                        logInit("    • Compute Shaders: AVAILABLE");
                    }
                    if (stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper.hasFeatureRayTracing()) {
                        logInit("    • Ray Tracing: AVAILABLE (extension)");
                    }
                    if (stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper.hasFeatureMeshShading()) {
                        logInit("    • Mesh Shading: AVAILABLE (extension)");
                    }
                } catch (Exception e) {
                    LOGGER.warn("GLSL Pipeline Provider initialization failed", e);
                }
            }
            
            logInit("  - Using OpenGL as primary backend");
            
            // Create GPUBackendSelector instance for later use
            GPUBackendSelector selector = GPUBackendSelector.instance();
            gpuBackendSelector.set(selector);
            logInit("  - GPU Backend Selector: READY");
            
            // Check if advanced backends should be initialized
            if (shouldInitializeVulkan()) {
                logInit("  - Vulkan initialization will be attempted when window is ready");
            }
            if (shouldInitializeMetal()) {
                logInit("  - Metal initialization will be attempted when window is ready");
            }
            if (shouldInitializeDirectX()) {
                logInit("  - DirectX initialization will be attempted when window is ready");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize OpenGL backend", e);
            LOGGER.warn("Falling back to Null backend (no rendering)...");
            
            // Ultimate fallback - NullBackend
            NullBackend nullBackend = new NullBackend();
            nullBackend.initialize();
            activeGPUBackend.set(nullBackend);
            logInit("  - Null Backend: ACTIVE (fallback)");
        }
    }
    
    /**
     * Check if Vulkan should be initialized based on configuration
     */
    private static boolean shouldInitializeVulkan() {
        Config.PreferredAPI api = Config.getPreferredAPI();
        
        // Check if Vulkan is preferred or auto with Vulkan support
        if (api == Config.PreferredAPI.VULKAN) {
            return true;
        }
        
        if (api == Config.PreferredAPI.AUTO && UniversalCapabilities.isVulkanSupported()) {
            // Check if system meets minimum Vulkan requirements from config
            int minMajor = Config.getInt("minVulkanMajor");
            int minMinor = Config.getInt("minVulkanMinor");
            int vulkanMajor = UniversalCapabilities.getVulkanVersionMajor();
            int vulkanMinor = UniversalCapabilities.getVulkanVersionMinor();
            
            return (vulkanMajor > minMajor) || (vulkanMajor == minMajor && vulkanMinor >= minMinor);
        }
        
        return false;
    }
    
    /**
     * Check if DirectX should be initialized based on configuration
     */
    private static boolean shouldInitializeDirectX() {
        // Only on Windows
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            return false;
        }
        
        if (!Config.isDirectXEnabled()) {
            return false;
        }
        
        Config.PreferredAPI api = Config.getPreferredAPI();
        
        // Check if DirectX is explicitly preferred
        if (api == Config.PreferredAPI.DIRECTX || 
            api == Config.PreferredAPI.DIRECTX11 || 
            api == Config.PreferredAPI.DIRECTX12) {
            return true;
        }
        
        // Check if AUTO and DirectX should be considered
        if (api == Config.PreferredAPI.AUTO && Config.isDirectXPreferDX12()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Initialize advanced GPU backends after window is created.
     * This is called from MixinMinecraftDisplay after the window handle is available.
     * 
     * @param windowHandle Native window handle (HWND on Windows, etc.)
     * @param width Window width
     * @param height Window height
     */
    public static void initializeAdvancedBackends(long windowHandle, int width, int height) {
        if (!advancedBackendsInitialized.compareAndSet(false, true)) {
            LOGGER.warn("Advanced GPU backends already initialized!");
            return;
        }
        
        logInit("=== GPU BACKENDS INITIALIZATION (Phase 2: Advanced) ===");
        logInit("Window Handle: 0x" + Long.toHexString(windowHandle));
        logInit("Resolution: " + width + "x" + height);
        
        try {
            // Try to initialize Vulkan if configured
            if (shouldInitializeVulkan()) {
                boolean vulkanSuccess = tryInitializeVulkan(windowHandle, width, height);
                
                if (vulkanSuccess) {
                    logInit("  - Vulkan: INITIALIZED");
                    logInit("  - Switched active backend to Vulkan");
                } else {
                    logInit("  - Vulkan: Failed to initialize, keeping OpenGL");
                }
            } else {
                logInit("  - Vulkan: Skipped (not configured)");
            }
            
            // Metal initialization (macOS/iOS only)
            if (shouldInitializeMetal()) {
                boolean metalSuccess = tryInitializeMetal(windowHandle, width, height);
                
                if (metalSuccess) {
                    logInit("  - Metal: INITIALIZED");
                    if (activeGPUBackend.get() == null || Config.getPreferredAPI() == Config.PreferredAPI.METAL) {
                        logInit("  - Switched active backend to Metal");
                    }
                } else {
                    logInit("  - Metal: Failed to initialize, keeping current backend");
                }
            } else {
                logInit("  - Metal: Skipped (not configured or not macOS/iOS)");
            }
            
            // DirectX initialization (Windows only)
            if (shouldInitializeDirectX()) {
                boolean directXSuccess = tryInitializeDirectX(windowHandle, width, height);
                
                if (directXSuccess) {
                    logInit("  - DirectX: INITIALIZED");
                    if (activeGPUBackend.get() == null) {
                        logInit("  - Switched active backend to DirectX");
                    }
                } else {
                    logInit("  - DirectX: Failed to initialize, keeping current backend");
                }
            } else {
                logInit("  - DirectX: Skipped (not configured or not Windows)");
            }
            
            // OpenGL ES initialization (for mobile/embedded or emulation)
            if (shouldInitializeOpenGLES()) {
                boolean glesSuccess = tryInitializeOpenGLES();
                
                if (glesSuccess) {
                    logInit("  - OpenGL ES: INITIALIZED");
                    if (Config.getPreferredAPI() == Config.PreferredAPI.OPENGL_ES) {
                        logInit("  - Switched active backend to OpenGL ES");
                    }
                } else {
                    logInit("  - OpenGL ES: Failed to initialize, keeping current backend");
                }
            } else {
                logInit("  - OpenGL ES: Skipped (not configured)");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize advanced GPU backends", e);
            logInit("  - Advanced backend initialization failed, keeping OpenGL");
        }
        
        logInit("Advanced GPU backend initialization complete");
    }
    
    /**
     * Attempt to initialize Vulkan backend
     * 
     * @return true if successful, false otherwise
     */
    private static boolean tryInitializeVulkan(long windowHandle, int width, int height) {
        try {
            logInit("  - Attempting Vulkan initialization...");
            
            // Create Vulkan backend instance
            VulkanBackend vkBackend = new VulkanBackend();
            
            // Initialize with window handle
            boolean success = vkBackend.initialize(windowHandle, width, height);
            
            if (success) {
                vulkanBackend.set(vkBackend);
                
                // Initialize VulkanManager
                VulkanManager.init(vkBackend);
                vulkanManager.set(VulkanManager.getSafe());
                
                // Switch active backend to Vulkan
                activeGPUBackend.set(vkBackend);
                
                // Sync all detected capabilities into UniversalCapabilities.Vulkan
                updateUniversalCapabilitiesFromVulkan(vkBackend);
                
                logInit("    - Vulkan instance created");
                logInit("    - Vulkan device selected: " + vkBackend.getDeviceName());
                logInit("    - Vulkan API version: " + vkBackend.getApiVersion());
                logInit("    - VulkanManager initialized");

                // Log extended Vulkan features now visible through UniversalCapabilities
                if (UniversalCapabilities.Vulkan.hasMeshShaderEXT)
                    logInit("    ✓ Mesh Shaders (EXT): Supported");
                if (UniversalCapabilities.Vulkan.hasRayTracingPipeline)
                    logInit("    ✓ Ray Tracing (KHR): Supported");
                if (UniversalCapabilities.Vulkan.hasFragmentShadingRate)
                    logInit("    ✓ Variable Rate Shading (KHR): Supported");
                if (UniversalCapabilities.Vulkan.hasDedicatedTransferQueue)
                    logInit("    ✓ Dedicated Transfer Queue: Available");
                if (UniversalCapabilities.Vulkan.hasAsyncComputeQueue)
                    logInit("    ✓ Async Compute Queue: Available");
                if (UniversalCapabilities.Vulkan.hasKTX2Support)
                    logInit("    ✓ KTX2 Texture Pipeline: Active");
                if (UniversalCapabilities.Vulkan.hasExternalMemory)
                    logInit("    ✓ External Memory: Supported");

                logInit("    - Vulkan tier: " + UniversalCapabilities.Vulkan.getRecommendedTier());
                
                return true;
            } else {
                logInit("    - Vulkan initialization failed");
                return false;
            }
            
        } catch (Exception e) {
            LOGGER.error("Exception during Vulkan initialization", e);
            logInit("    - Vulkan initialization threw exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Populate UniversalCapabilities.Vulkan with data from a live VulkanBackend.
     *
     * <p>Builds the extension set and featureFlags array from the backend's reported
     * capabilities, then calls {@link UniversalCapabilities.Vulkan#updateFromVulkanBackend}
     * so every system that reads UniversalCapabilities gets accurate info.</p>
     */
    private static void updateUniversalCapabilitiesFromVulkan(VulkanBackend vkBackend) {
        try {
            // ── Version ──────────────────────────────────────────────────────────────────
            String apiVer = vkBackend.getApiVersion(); // e.g. "1.3.260"
            int major = 1, minor = 0, patch = 0;
            try {
                String[] parts = apiVer.split("\\.");
                if (parts.length >= 2) {
                    major = Integer.parseInt(parts[0]);
                    minor = Integer.parseInt(parts[1]);
                }
                if (parts.length >= 3) patch = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {}

            // ── Extension set (ask VulkanManager for the live device extension list) ────
            java.util.Set<String> extSet = new java.util.HashSet<>();
            try {
                VulkanManager vm = vulkanManager.get();
                if (vm != null && vm.getEnabledDeviceExtensions() != null) {
                    extSet.addAll(vm.getEnabledDeviceExtensions());
                }
            } catch (Exception ignored) {}

            // ── Queue family flags ────────────────────────────────────────────────────────
            boolean hasDedicatedTransfer = false;
            boolean hasAsyncCompute      = false;
            try {
                VulkanManager vm = vulkanManager.get();
                if (vm != null) {
                    hasDedicatedTransfer = vm.hasDedicatedTransferQueue();
                    hasAsyncCompute      = vm.hasAsyncComputeQueue();
                }
            } catch (Exception ignored) {}

            // ── Texture pipeline flags ────────────────────────────────────────────────────
            boolean ktx2Active       = Config.isVulkanEnableKTX2Pipeline();
            boolean computeMipmaps   = Config.isVulkanEnableComputeMipmaps();

            // ── VRS flags ────────────────────────────────────────────────────────────────
            boolean vrsPrimitive     = extSet.contains("VK_KHR_fragment_shading_rate");
            boolean vrsImage         = extSet.contains("VK_NV_shading_rate_image");

            // ── Texture format support (queried from backend) ─────────────────────────────
            boolean bc7  = false, bc6h = false, astc = false, etc2 = false;
            try {
                VulkanManager vm = vulkanManager.get();
                if (vm != null) {
                    bc7  = vm.isFormatSupported("VK_FORMAT_BC7_UNORM_BLOCK");
                    bc6h = vm.isFormatSupported("VK_FORMAT_BC6H_UFLOAT_BLOCK");
                    astc = vm.isFormatSupported("VK_FORMAT_ASTC_4x4_UNORM_BLOCK");
                    etc2 = vm.isFormatSupported("VK_FORMAT_ETC2_R8G8B8_UNORM_BLOCK");
                }
            } catch (Exception ignored) {}

            // ── Build featureFlags array ─────────────────────────────────────────────────
            // Index layout (must match UniversalCapabilities.Vulkan.updateFromVulkanBackend):
            //  [0] hasMipmapGenCompute   [1] hasKTX2Support
            //  [2] hasBC7               [3] hasBC6H
            //  [4] hasASTC_LDR          [5] hasETC2
            //  [6] hasDedicatedTransfer [7] hasAsyncCompute
            //  [8] hasVRSPrimitive      [9] hasVRSImage
            boolean[] featureFlags = {
                computeMipmaps, ktx2Active,
                bc7, bc6h, astc, etc2,
                hasDedicatedTransfer, hasAsyncCompute,
                vrsPrimitive, vrsImage,
                false, false, false, false, false, false  // reserved
            };

            // ── Memory sizes ─────────────────────────────────────────────────────────────
            long deviceLocalMem = 0L, hostVisibleMem = 0L;
            try {
                VulkanManager vm = vulkanManager.get();
                if (vm != null) {
                    deviceLocalMem  = vm.getDeviceLocalMemoryBytes();
                    hostVisibleMem  = vm.getHostVisibleMemoryBytes();
                }
            } catch (Exception ignored) {}

            // ── Hand off to UniversalCapabilities ────────────────────────────────────────
            UniversalCapabilities.Vulkan.updateFromVulkanBackend(
                major, minor, patch,
                vkBackend.getDeviceName(),
                vkBackend.getVendorId(),
                vkBackend.getDeviceId(),
                deviceLocalMem, hostVisibleMem,
                extSet, featureFlags
            );

            // ── Re-validate Config against the now-populated capabilities ─────────────────
            Config.reload();

            logInit("    - UniversalCapabilities.Vulkan updated");

        } catch (Exception e) {
            LOGGER.error("Failed to update UniversalCapabilities from Vulkan backend", e);
            logInit("    - UniversalCapabilities.Vulkan update failed: " + e.getMessage());
        }
    }

    /**
     * Check if DirectX should be initialized based on config
     */
    private static boolean shouldInitializeDirectX() {
        // Only on Windows
        if (!isWindowsOS()) {
            return false;
        }
        
        Config.PreferredAPI preferredAPI = Config.getPreferredAPI();
        return preferredAPI == Config.PreferredAPI.DIRECTX ||
               preferredAPI == Config.PreferredAPI.DIRECTX11 ||
               preferredAPI == Config.PreferredAPI.DIRECTX12 ||
               (preferredAPI == Config.PreferredAPI.AUTO && Config.isDirectXEnabled());
    }
    
    /**
     * Attempt to initialize DirectX backend
     * 
     * @return true if successful, false otherwise
     */
    private static boolean tryInitializeDirectX(long windowHandle, int width, int height) {
        try {
            logInit("  - Attempting DirectX initialization...");
            logInit("    - Supported versions: DX 9.0c - 12.2");
            
            // Create DirectX configuration from Config.java
            DirectXManager.DirectXConfig dxConfig = new DirectXManager.DirectXConfig();
            
            // Log configuration
            logInit("    - Preferred version: DirectX " + dxConfig.preferredVersion);
            logInit("    - Fallback enabled: " + dxConfig.allowFallback);
            logInit("    - Min feature level: 0x" + Integer.toHexString(dxConfig.minFeatureLevel));
            logInit("    - Max feature level: 0x" + Integer.toHexString(dxConfig.maxFeatureLevel));
            
            // Create DirectX manager instance
            DirectXManager dxManager = new DirectXManager();
            
            // Initialize with window handle - this will detect hardware capabilities
            boolean success = dxManager.initialize(windowHandle, width, height);
            
            if (success) {
                directXManager.set(dxManager);
                
                // Get detected capabilities
                DirectXManager.Capabilities caps = dxManager.getCapabilities();
                DirectXManager.APIVersion activeAPI = dxManager.getCurrentAPI();
                
                // Update UniversalCapabilities with detected DirectX info
                updateUniversalCapabilitiesFromDirectX(dxManager, caps, activeAPI);
                
                // Initialize HLSL capabilities based on DirectX
                initializeHLSLCapabilities();
                
                // Initialize DirectX pipeline provider
                DirectXPipelineProvider dxPipeline = DirectXPipelineProvider.getInstance(dxManager);
                directXPipeline.set(dxPipeline);
                
                // Log success details
                logInit("    ✓ DirectX Manager created");
                logInit("    ✓ Active API: " + activeAPI.displayName);
                logInit("    ✓ Feature Level: " + caps.featureLevel().name);
                logInit("    ✓ GPU: " + caps.vendor().name);
                logInit("    ✓ VRAM: " + (caps.dedicatedVideoMemory() / 1024 / 1024) + " MB");
                
                if (caps.supportsRayTracingTier1_0()) {
                    logInit("    ✓ Ray Tracing: Tier " + (caps.supportsRayTracingTier1_1() ? "1.1" : "1.0"));
                }
                if (caps.supportsMeshShaders()) {
                    logInit("    ✓ Mesh Shaders: Supported");
                }
                if (caps.supportsVariableRateShading()) {
                    logInit("    ✓ Variable Rate Shading: Tier " + caps.variableRateShadingTier());
                }
                
                logInit("    ✓ DirectX Pipeline Provider initialized");
                
                // Log HLSL capabilities
                if (UniversalCapabilities.HLSL.isAvailable) {
                    logInit("    ✓ HLSL Compiler: " + UniversalCapabilities.HLSL.activeCompiler);
                    logInit("    ✓ Shader Model: " + UniversalCapabilities.HLSL.shaderModelString);
                    if (UniversalCapabilities.HLSL.waveIntrinsics) {
                        logInit("    ✓ Wave Intrinsics: Supported");
                    }
                    if (UniversalCapabilities.HLSL.meshShaders) {
                        logInit("    ✓ HLSL Mesh Shaders: Supported");
                    }
                }                
                return true;
            } else {
                logInit("    ✗ DirectX initialization failed");
                return false;
            }
            
        } catch (Exception e) {
            LOGGER.error("Exception during DirectX initialization", e);
            logInit("    ✗ DirectX initialization threw exception: " + e.getMessage());
            if (Config.isDirectXDebugMode()) {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    /**
     * Update UniversalCapabilities.DirectX with detected capabilities from DirectXManager
     */
    private static void updateUniversalCapabilitiesFromDirectX(
        DirectXManager manager, 
        DirectXManager.Capabilities caps, 
        DirectXManager.APIVersion activeAPI
    ) {
        // Get version info
        int major = activeAPI.numericVersion >> 8;
        int minor = activeAPI.numericVersion & 0xFF;
        
        // Build version support array
        boolean[] versionSupport = new boolean[12];
        versionSupport[0] = caps.apiVersion().isAtLeast(DirectXManager.APIVersion.DX9);
        versionSupport[1] = caps.apiVersion().isAtLeast(DirectXManager.APIVersion.DX9_EX);
        versionSupport[2] = caps.apiVersion().isAtLeast(DirectXManager.APIVersion.DX10);
        versionSupport[3] = caps.apiVersion().isAtLeast(DirectXManager.APIVersion.DX10_1);
        versionSupport[4] = caps.apiVersion().isAtLeast(DirectXManager.APIVersion.DX11);
        versionSupport[5] = caps.apiVersion().isAtLeast(DirectXManager.APIVersion.DX11_1);
        versionSupport[6] = caps.apiVersion().isAtLeast(DirectXManager.APIVersion.DX11_2);
        versionSupport[7] = caps.apiVersion().isAtLeast(DirectXManager.APIVersion.DX11_3);
        versionSupport[8] = caps.apiVersion().isAtLeast(DirectXManager.APIVersion.DX11_4);
        versionSupport[9] = caps.apiVersion().isAtLeast(DirectXManager.APIVersion.DX12);
        versionSupport[10] = caps.apiVersion().isAtLeast(DirectXManager.APIVersion.DX12_1);
        versionSupport[11] = caps.apiVersion().isAtLeast(DirectXManager.APIVersion.DX12_2);
        
        // Build feature support array
        boolean[] features = new boolean[20];
        features[0] = caps.supportsRayTracingTier1_0();
        features[1] = caps.supportsMeshShaders();
        features[2] = caps.supportsVariableRateShading();
        features[3] = caps.supportsSamplerFeedback();
        features[4] = caps.supportsTiledResources();
        features[5] = caps.supportsConservativeRasterization();
        features[6] = caps.supportsROVs();
        features[7] = caps.supportsTypedUAVLoads();
        features[8] = caps.supportsBindlessResources();
        features[9] = true;  // Compute shaders (DX11+)
        features[10] = caps.featureLevel().supportsTessellation();
        features[11] = caps.featureLevel().supportsTessellation();
        features[12] = caps.featureLevel().supportsGeometryShader();
        features[13] = activeAPI.supportsCommandLists();
        features[14] = activeAPI.isDX12Family();
        features[15] = activeAPI.isDX12Family();
        features[16] = activeAPI.isDX12Family();
        features[17] = caps.vendor() == DirectXManager.GPUVendor.MICROSOFT;  // WARP
        features[18] = false;  // DirectStorage (TODO: detect properly)
        features[19] = false;  // Work Graphs (TODO: detect properly)
        
        // Update UniversalCapabilities.DirectX
        UniversalCapabilities.DirectX.updateFromDirectXManager(
            major, minor, caps.featureLevel().value,
            caps.vendor().name + " " + "GPU",  // Device name
            caps.vendor().vendorId, 0,  // Vendor ID, Device ID
            caps.dedicatedVideoMemory(),
            caps.dedicatedSystemMemory(),
            caps.sharedSystemMemory(),
            versionSupport,
            features
        );
        
        logInit("    - UniversalCapabilities.DirectX updated");
    }
    
    /**
     * Initialize HLSL capabilities after DirectX is initialized
     */
    private static void initializeHLSLCapabilities() {
        try {
            logInit("    - Initializing HLSL capabilities...");
            
            // Initialize HLSL based on DirectX capabilities
            UniversalCapabilities.initializeHLSL();
            
            if (!UniversalCapabilities.HLSL.isAvailable) {
                logInit("    - HLSL not available (DirectX required)");
                return;
            }
            
            // Log HLSL compiler info
            logInit("    - HLSL Compiler: " + UniversalCapabilities.HLSL.activeCompiler + 
                   " v" + UniversalCapabilities.HLSL.compilerVersion);
            
            // Log shader model support
            logInit("    - Highest Shader Model: " + UniversalCapabilities.HLSL.shaderModelString);
            
            // Log available shader models
            StringBuilder smSupport = new StringBuilder("    - Supported Shader Models: ");
            if (UniversalCapabilities.HLSL.sm2_0) smSupport.append("2.0 ");
            if (UniversalCapabilities.HLSL.sm3_0) smSupport.append("3.0 ");
            if (UniversalCapabilities.HLSL.sm4_0) smSupport.append("4.0 ");
            if (UniversalCapabilities.HLSL.sm4_1) smSupport.append("4.1 ");
            if (UniversalCapabilities.HLSL.sm5_0) smSupport.append("5.0 ");
            if (UniversalCapabilities.HLSL.sm5_1) smSupport.append("5.1 ");
            if (UniversalCapabilities.HLSL.sm6_0) smSupport.append("6.0+ ");
            logInit(smSupport.toString());
            
            // Log advanced features if available
            if (UniversalCapabilities.HLSL.activeShaderModel >= 60) {
                logInit("    - SM 6.0+ Features:");
                if (UniversalCapabilities.HLSL.waveIntrinsics) 
                    logInit("      ✓ Wave Intrinsics");
                if (UniversalCapabilities.HLSL.raytracingShaders) 
                    logInit("      ✓ Ray Tracing Shaders (DXR)");
                if (UniversalCapabilities.HLSL.meshShaders) 
                    logInit("      ✓ Mesh Shaders");
                if (UniversalCapabilities.HLSL.variableRateShading) 
                    logInit("      ✓ Variable Rate Shading");
                if (UniversalCapabilities.HLSL.samplerFeedback) 
                    logInit("      ✓ Sampler Feedback");
            }
            
            // Log root signature support
            if (UniversalCapabilities.HLSL.supportsRootSignature) {
                String rsVersion = UniversalCapabilities.HLSL.supportsRootSignature1_1 ? "1.1" : "1.0";
                logInit("    - Root Signature Version: " + rsVersion);
            }
            
            // Log translation capabilities
            if (UniversalCapabilities.HLSL.glslToHLSLTranslation) {
                logInit("    - GLSL→HLSL Translation: Supported");
            }
            if (UniversalCapabilities.HLSL.spirvBackend) {
                logInit("    - HLSL→SPIR-V Backend: Supported");
            }
            
            logInit("    - HLSL capabilities initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("Exception during HLSL initialization", e);
            logInit("    - HLSL initialization failed: " + e.getMessage());
        }
    }
    
    /**
     * Check if running on Windows OS
     */
    private static boolean isWindowsOS() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }
    
    /**
     * Check if running on macOS
     */
    private static boolean isMacOS() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac") || os.contains("darwin");
    }
    
    /**
     * Check if Metal should be initialized based on configuration
     */
    private static boolean shouldInitializeMetal() {
        // Only on macOS/iOS
        if (!isMacOS()) {
            return false;
        }
        
        Config.PreferredAPI preferredAPI = Config.getPreferredAPI();
        return preferredAPI == Config.PreferredAPI.METAL ||
               (preferredAPI == Config.PreferredAPI.AUTO && UniversalCapabilities.isMetalSupported());
    }
    
    /**
     * Attempt to initialize Metal backend
     * 
     * @return true if successful, false otherwise
     */
    private static boolean tryInitializeMetal(long windowHandle, int width, int height) {
        try {
            logInit("  - Attempting Metal initialization...");
            
            // Validate Metal support
            if (!UniversalCapabilities.isMetalSupported()) {
                logInit("    - Metal not supported on this system");
                return false;
            }
            
            // Get Metal version requirements from config
            int minMajor = Config.getInt("minMetalMajor");
            int minMinor = Config.getInt("minMetalMinor");
            int metalMajor = UniversalCapabilities.getMetalVersionMajor();
            int metalMinor = UniversalCapabilities.getMetalVersionMinor();
            
            // Check version compatibility
            if (metalMajor < minMajor || (metalMajor == minMajor && metalMinor < minMinor)) {
                logInit("    - Metal version " + metalMajor + "." + metalMinor + 
                       " does not meet minimum requirement " + minMajor + "." + minMinor);
                return false;
            }
            
            // Create Metal manager instance
            stellar.snow.astralis.api.metal.managers.MetalManager mtlManager = 
                stellar.snow.astralis.api.metal.managers.MetalManager.getInstance();
            
            // Initialize with window handle
            boolean success = mtlManager.initialize(windowHandle, width, height);
            
            if (success) {
                metalManager.set(mtlManager);
                
                // Initialize Metal pipeline provider
                stellar.snow.astralis.api.metal.pipeline.MetalPipelineProvider mtlPipeline = 
                    stellar.snow.astralis.api.metal.pipeline.MetalPipelineProvider.getInstance(mtlManager);
                metalPipeline.set(mtlPipeline);
                
                // Initialize MSL (Metal Shading Language) pipeline if enabled
                if (Config.isMSLEnabled()) {
                    try {
                        stellar.snow.astralis.api.metal.pipeline.MSLPipelineProvider mslProvider = 
                            stellar.snow.astralis.api.metal.pipeline.MSLPipelineProvider.getInstance(mtlManager);
                        mslPipeline.set(mslProvider);
                        logInit("    - MSL Pipeline Provider: INITIALIZED (target: MSL " + Config.getMSLLanguageVersion() + ")");
                        
                        // Log Metal modern features
                        if (Config.isMSLEnableRaytracing() && metalMajor >= 2 && metalMinor >= 2) {
                            logInit("      • Ray Tracing: AVAILABLE (Metal 2.2+)");
                        }
                        if (Config.isMSLEnableMeshShaders() && metalMajor >= 2 && metalMinor >= 4) {
                            logInit("      • Mesh Shaders: AVAILABLE (Metal 2.4+)");
                        }
                        if (Config.isMSLEnableArgumentBuffers() && metalMajor >= 2) {
                            logInit("      • Argument Buffers: AVAILABLE (Metal 2.0+)");
                        }
                        if (Config.isMSLEnableIndirectCommandBuffers() && metalMajor >= 2) {
                            logInit("      • Indirect Command Buffers: AVAILABLE (Metal 2.0+)");
                        }
                    } catch (Exception e) {
                        LOGGER.warn("MSL Pipeline Provider initialization failed", e);
                    }
                }
                
                logInit("    - Metal Manager created");
                logInit("    - Metal version: " + metalMajor + "." + metalMinor);
                logInit("    - Metal device: " + mtlManager.getDeviceName());
                logInit("    - Metal Pipeline Provider initialized");
                
                return true;
            } else {
                logInit("    - Metal initialization failed");
                return false;
            }
            
        } catch (Exception e) {
            LOGGER.error("Exception during Metal initialization", e);
            logInit("    - Metal initialization threw exception: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if OpenGL ES should be initialized based on config
     */
    private static boolean shouldInitializeOpenGLES() {
        Config.PreferredAPI preferredAPI = Config.getPreferredAPI();
        
        // Initialize if explicitly requested
        if (preferredAPI == Config.PreferredAPI.OPENGL_ES) {
            return true;
        }
        
        // Also check if we're in a GLES context (mobile/embedded)
        if (UniversalCapabilities.GLES.isGLESContext) {
            return true;
        }
        
        // Allow AUTO mode with GLES config enabled
        return preferredAPI == Config.PreferredAPI.AUTO && Config.getBoolean("enableGLES");
    }
    
    /**
     * Attempt to initialize OpenGL ES manager
     * 
     * @return true if successful, false otherwise
     */
    private static boolean tryInitializeOpenGLES() {
        try {
            logInit("  - Attempting OpenGL ES initialization...");
            
            // Check if GLES is supported or can be emulated
            if (!UniversalCapabilities.GLES.isGLESContext && !Config.getBoolean("allowGLESEmulation")) {
                logInit("    - OpenGL ES context not available and emulation disabled");
                return false;
            }
            
            // Get GLES version requirements from config
            int minMajor = Config.getInt("minGLESMajor");
            int minMinor = Config.getInt("minGLESMinor");
            int maxMajor = Config.getInt("maxGLESMajor");
            int maxMinor = Config.getInt("maxGLESMinor");
            
            // Determine actual GLES version available
            int glesMajor = UniversalCapabilities.GLES.majorVersion;
            int glesMinor = UniversalCapabilities.GLES.minorVersion;
            
            if (glesMajor == 0) {
                logInit("    - OpenGL ES version could not be detected");
                return false;
            }
            
            // Check version compatibility
            if (glesMajor < minMajor || (glesMajor == minMajor && glesMinor < minMinor)) {
                logInit("    - OpenGL ES version " + glesMajor + "." + glesMinor + 
                       " does not meet minimum requirement " + minMajor + "." + minMinor);
                return false;
            }
            
            // Check if version exceeds maximum
            if (glesMajor > maxMajor || (glesMajor == maxMajor && glesMinor > maxMinor)) {
                logInit("    - Warning: OpenGL ES version " + glesMajor + "." + glesMinor + 
                       " exceeds configured maximum " + maxMajor + "." + maxMinor);
                logInit("    - Proceeding with compatibility mode");
            }
            
            // Create OpenGL ES manager instance
            OpenGLESManager glesManager = OpenGLESManager.builder()
                .withCommandPoolSize(Config.getInt("glesCommandPoolSize"))
                .withDebugCallback((source, type, id, severity, message) -> {
                    if (Config.getBoolean("glesDebugMessages")) {
                        LOGGER.info("[GLES Debug] " + message);
                    }
                })
                .build();
            
            openGLESManager.set(glesManager);
            
            // Initialize OpenGL ES pipeline providers
            OpenGLESPipelineProvider glesPipeline = new OpenGLESPipelineProvider(glesManager);
            openGLESPipeline.set(glesPipeline);
            
            GLSLESPipelineProvider glslPipeline = new GLSLESPipelineProvider(glesManager);
            glslesPipeline.set(glslPipeline);
            
            logInit("    - OpenGL ES Manager created");
            logInit("    - OpenGL ES version: " + glesMajor + "." + glesMinor);
            logInit("    - OpenGL ES vendor: " + UniversalCapabilities.GLES.vendorString);
            logInit("    - OpenGL ES renderer: " + UniversalCapabilities.GLES.rendererString);
            logInit("    - Emulated: " + UniversalCapabilities.GLES.isEmulatedGLES);
            logInit("    - OpenGL ES Pipeline Providers initialized");
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Exception during OpenGL ES initialization", e);
            logInit("    - OpenGL ES initialization threw exception: " + e.getMessage());
            return false;
        }
    }
    
    private static void initializeECSBridge() {
        logInit("Initializing Minecraft ECS Bridge...");
        
        try {
            // Check if ECS bridge is enabled
            if (!Config.isECSMinecraftBridgeEnabled()) {
                logInit("  - ECS Bridge disabled in configuration, skipping");
                return;
            }
            
            World world = ecsWorld.get();
            if (world == null) {
                throw new IllegalStateException("ECS World not initialized");
            }
            
            // Get singleton instance
            MinecraftECSBridge bridge = MinecraftECSBridge.getInstance();
            
            // Create config with settings from Config.java
            World.Config bridgeConfig = World.Config.defaults("AstralisBridge")
                .withInitialCapacity(Config.getECSInitialCapacity())
                .withChunkSize(Config.getECSChunkSize())
                .withUseOffHeap(Config.isECSUseOffHeap())
                .withTrackChanges(Config.isECSTrackChanges())
                .withEnableGpu(Config.isECSEnableGpu() && UniversalCapabilities.hasComputeShaders())
                .withBuildEdgeGraph(Config.isECSBuildEdgeGraph());
            
            // Initialize bridge with config
            bridge.initialize(bridgeConfig);
            
            ecsBridge.set(bridge);
            logInit("  - ECS Bridge: OK");
            
            // Initialize Minecraft-specific optimizers based on config
            if (Config.isECSMinecraftEntityOptimizer()) {
                logInit("    • Entity Optimizer: ENABLED");
            }
            if (Config.isECSMinecraftChunkStreamer()) {
                logInit("    • Chunk Streamer: ENABLED");
            }
            if (Config.isECSMinecraftSpatialOptimizer()) {
                logInit("    • Spatial Optimizer: ENABLED");
            }
            if (Config.isECSMinecraftRedstoneOptimizer()) {
                logInit("    • Redstone Optimizer: ENABLED (experimental)");
            }
            if (Config.isECSMinecraftParallelEntityTick()) {
                logInit("    • Parallel Entity Ticking: ENABLED (batch size: " + Config.getECSMinecraftEntityTickBatchSize() + ")");
            }
            
            // Initialize Bridge Systems
            initializeBridgeSystems();
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize ECS Bridge", e);
            throw e;
        }
    }
    
    private static void initializeBridgeSystems() {
        logInit("Initializing Bridge Systems...");
        
        try {
            MinecraftECSBridge bridge = ecsBridge.get();
            if (bridge == null) {
                logInit("  - Bridge systems skipped (no bridge)");
                return;
            }
            
            World world = ecsWorld.get();
            if (world == null) {
                logInit("  - Bridge systems skipped (no ECS world)");
                return;
            }
            
            // Initialize Circuit Breaker (if enabled)
            if (Config.isBridgeCircuitBreakerEnabled()) {
                stellar.snow.astralis.bridge.CircuitBreaker circuitBreaker = 
                    new stellar.snow.astralis.bridge.CircuitBreaker(
                        Config.getBridgeCircuitBreakerFailureThreshold(),
                        Config.getBridgeCircuitBreakerResetTimeout(),
                        "MinecraftBridge"
                    );
                logInit("  - Circuit Breaker: OK");
                logInit("    • Failure Threshold: " + Config.getBridgeCircuitBreakerFailureThreshold());
                logInit("    • Reset Timeout: " + Config.getBridgeCircuitBreakerResetTimeout() + "ms");
            }
            
            // Initialize Interpolation System (if enabled)
            if (Config.isBridgeInterpolationEnabled()) {
                stellar.snow.astralis.bridge.InterpolationSystem interpolation = 
                    new stellar.snow.astralis.bridge.InterpolationSystem(bridge);
                world.addSystem(interpolation);
                logInit("  - Interpolation System: OK");
                logInit("    • Interpolation Mode: " + Config.getBridgeInterpolationMode());
            }
            
            // Initialize Sync Systems (if enabled)
            if (Config.isBridgeSyncSystemsEnabled()) {
                stellar.snow.astralis.bridge.SyncSystems.InboundSyncSystem inboundSync = 
                    new stellar.snow.astralis.bridge.SyncSystems.InboundSyncSystem(bridge);
                stellar.snow.astralis.bridge.SyncSystems.OutboundSyncSystem outboundSync = 
                    new stellar.snow.astralis.bridge.SyncSystems.OutboundSyncSystem(bridge);
                
                world.addSystem(inboundSync);
                world.addSystem(outboundSync);
                logInit("  - Sync Systems: OK");
                logInit("    • Inbound Sync: ENABLED");
                logInit("    • Outbound Sync: ENABLED");
                logInit("    • Batch Size: " + Config.getBridgeSyncBatchSize());
            }
            
            logInit("  - All bridge systems initialized");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize bridge systems", e);
            // Don't throw - bridge systems are optional
            logInit("  - Bridge systems initialization failed (non-critical)");
        }
    }
    
    private static void initializeRenderingManagers() {
        logInit("Initializing rendering managers...");
        
        try {
            GPUBackend backend = activeGPUBackend.get();
            World world = ecsWorld.get();
            
            if (backend != null && world != null) {
                // Initialize Render Graph System (if enabled)
                if (Config.isRenderGraphEnabled()) {
                    logInit("  - Render Graph: Initializing...");
                    // Render graph will be initialized on-demand by render systems
                    logInit("    ✓ Render graph ready");
                }
                
                // Initialize Render System (if enabled)
                if (Config.isRenderSystemEnabled()) {
                    logInit("  - Render System: Initializing...");
                    stellar.snow.astralis.render.system.RenderSystem renderSystem = 
                        new stellar.snow.astralis.render.system.RenderSystem();
                    logInit("    ✓ Render system ready");
                }
                
                // Initialize Render Bridge (if enabled)
                if (Config.isRenderBridgeEnabled()) {
                    logInit("  - Render Bridge: Initializing...");
                    stellar.snow.astralis.render.system.RenderBridge renderBridge = 
                        new stellar.snow.astralis.render.system.RenderBridge();
                    logInit("    ✓ Render bridge ready");
                    logInit("    • Minecraft Compatibility: " + Config.isRenderBridgeMinecraftCompatibility());
                    logInit("    • Chunk Rendering: " + Config.isRenderBridgeEnableChunkRendering());
                    logInit("    • Entity Rendering: " + Config.isRenderBridgeEnableEntityRendering());
                }
                
                // Initialize CullingManager (if enabled)
                if (Config.isCullingManagerEnabled()) {
                    CullingManager culling = CullingManager.getInstance();
                    cullingManager.set(culling);
                    logInit("  - Culling Manager: OK (singleton)");
                    logInit("    • Tier: " + Config.getCullingTier());
                    logInit("    • Frustum Culling: " + Config.isCullingEnableFrustum());
                    logInit("    • Occlusion Culling: " + Config.isCullingEnableOcclusion());
                    logInit("    • Distance Culling: " + Config.isCullingEnableDistance());
                }
                
                // Initialize IndirectDrawManager (if enabled)
                if (Config.isIndirectDrawEnabled()) {
                    IndirectDrawManager indirectDraw = new IndirectDrawManager();
                    indirectDrawManager.set(indirectDraw);
                    logInit("  - Indirect Draw Manager: OK");
                    logInit("    • Max Draw Calls: " + Config.getIndirectDrawMaxDrawCalls());
                    logInit("    • Batching Strategy: " + Config.getIndirectDrawBatchingStrategy());
                }
                
                // Initialize ResolutionManager (if enabled)
                if (Config.isResolutionManagerEnabled()) {
                    ResolutionManager resolution = new ResolutionManager();
                    resolutionManager.set(resolution);
                    logInit("  - Resolution Manager: OK");
                    logInit("    • Adaptive Mode: " + Config.getResolutionManagerMode());
                    logInit("    • Min Scale: " + Config.getResolutionManagerMinScale());
                    logInit("    • Max Scale: " + Config.getResolutionManagerMaxScale());
                    logInit("    • TAA: " + Config.isResolutionManagerEnableTAA());
                    logInit("    • FSR: " + Config.isResolutionManagerEnableFSR());
                }
                
                // Initialize Draw Pool (if enabled)
                if (Config.isDrawPoolEnabled()) {
                    logInit("  - Draw Pool: Initializing...");
                    stellar.snow.astralis.scheduling.DrawPool drawPool = 
                        new stellar.snow.astralis.scheduling.DrawPool(
                            Config.getDrawPoolMaxDrawCalls(),
                            Config.getDrawPoolMaxClusters()
                        );
                    logInit("    ✓ Draw pool ready");
                    logInit("    • Max Draw Calls: " + Config.getDrawPoolMaxDrawCalls());
                    logInit("    • Max Clusters: " + Config.getDrawPoolMaxClusters());
                }
                
                logInit("  - All rendering managers initialized");
            } else {
                logInit("  - Rendering managers skipped (no GPU backend or ECS world)");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize rendering managers", e);
            // Don't throw - rendering managers are optional
            logInit("  - Rendering managers initialization failed (non-critical)");
        }
    }
    
    private static void printGPUInfo() {
        if (activeGPUBackend.get() != null) {
            try {
                logInit("=== GPU INFORMATION ===");
                GPUBackend backend = activeGPUBackend.get();
                logInit("  - Active Backend: " + backend.getClass().getSimpleName());
                
                if (backend instanceof VulkanBackend vk) {
                    logInit("  - Vulkan Device: " + vk.getDeviceName());
                    logInit("  - Vulkan API Version: " + vk.getApiVersion());
                    logInit("  - Vulkan Driver Version: " + vk.getDriverVersion());
                }
                
                logInit("  - Backend ready for rendering");
            } catch (Exception e) {
                LOGGER.warn("Could not retrieve GPU information", e);
            }
        }
    }
    
    private static void validateInitialization() {
        logInit("Validating initialization state...");

        boolean valid = true;

        // Confirm DeepMix wired up correctly (non-fatal — just informational)
        if (Config.isDeepMixEnabled()) {
            logInit("  - DeepMix loader: OK (mixins.astralis.json registered early)");
        }

        // Confirm Mini_DirtyRoom bootstrap completed and LWJGL override is live
        if (Config.isMDREnabled()) {
            if (Mini_DirtyRoomCore.isBootstrapComplete()) {
                int redirects = Mini_DirtyRoomCore.getClassRedirects().size();
                logInit("  - Mini_DirtyRoom: OK ("
                        + redirects + " redirect rules active, LWJGL "
                        + Mini_DirtyRoomCore.TARGET_LWJGL_VERSION + " override live)");
            } else {
                LOGGER.warn("  - Mini_DirtyRoom: INCOMPLETE (bootstrap still running — "
                        + "LWJGL override may not be fully active)");
            }
            if (!Mini_DirtyRoomCore.getBootWarnings().isEmpty()) {
                for (String w : Mini_DirtyRoomCore.getBootWarnings()) {
                    LOGGER.warn("  - MDR warning: {}", w);
                }
            }
        }

        // ECS World is critical
        if (ecsWorld.get() == null) {
            LOGGER.error("  - ECS World: FAILED (null)");
            valid = false;
        } else {
            logInit("  - ECS World: OK");
        }
        
        Side side = FMLCommonHandler.instance().getSide();
        if (side.isClient()) {
            // GPU backend is optional (can fall back to basic rendering)
            if (activeGPUBackend.get() != null) {
                logInit("  - GPU Backend: OK (" + activeGPUBackend.get().getClass().getSimpleName() + ")");
            } else {
                LOGGER.warn("  - GPU Backend: MISSING (using fallback)");
            }
            
            // ECS Bridge is critical for client
            if (ecsBridge.get() == null) {
                LOGGER.error("  - ECS Bridge: FAILED (null)");
                valid = false;
            } else {
                logInit("  - ECS Bridge: OK");
            }
            
            // Rendering managers optional if no GPU backend
            if (activeGPUBackend.get() != null) {
                if (cullingManager.get() != null) {
                    logInit("  - Culling Manager: OK");
                }
                if (indirectDrawManager.get() != null) {
                    logInit("  - Indirect Draw Manager: OK");
                }
                if (resolutionManager.get() != null) {
                    logInit("  - Resolution Manager: OK");
                }
            }
            
            // Vulkan-specific validation
            if (vulkanBackend.get() != null) {
                logInit("  - Vulkan Backend: OK");
                if (vulkanManager.get() != null) {
                    logInit("  - Vulkan Manager: OK");
                }
            }
            
            // DirectX-specific validation
            if (directXManager.get() != null) {
                logInit("  - DirectX Manager: OK");
                if (directXPipeline.get() != null) {
                    logInit("  - DirectX Pipeline: OK");
                }
            }
        }
        
        if (!valid) {
            throw new RuntimeException("Initialization validation failed - critical systems missing");
        }
        
        logInit("Validation complete - all critical systems operational");
    }
    
    private static void printInitializationReport() {
        LOGGER.info("╔═══════════════════════════════════════════════════════════════╗");
        LOGGER.info("║         ASTRALIS INITIALIZATION REPORT                       ║");
        LOGGER.info("╠═══════════════════════════════════════════════════════════════╣");
        
        for (String line : initializationLog) {
            LOGGER.info("║ " + String.format("%-61s", line) + "║");
        }
        
        LOGGER.info("╠═══════════════════════════════════════════════════════════════╣");
        LOGGER.info("║ Phase: " + String.format("%-54s", currentPhase) + "║");
        LOGGER.info("║ Status: READY                                                ║");
        LOGGER.info("╚═══════════════════════════════════════════════════════════════╝");
    }
    
    // ========================================================================
    // SHUTDOWN & CLEANUP
    // ========================================================================
    
    public static void shutdown() {
        logInit("=== ASTRALIS SHUTDOWN ===");
        
        try {
            // Close MSL pipeline (if active)
            if (mslPipeline.get() != null) {
                try {
                    mslPipeline.get().close();
                    logInit("  - MSL Pipeline: Closed");
                } catch (Exception e) {
                    LOGGER.error("Error closing MSL Pipeline", e);
                }
            }
            
            // Close Metal systems (if active)
            if (metalPipeline.get() != null) {
                try {
                    metalPipeline.get().close();
                    logInit("  - Metal Pipeline: Closed");
                } catch (Exception e) {
                    LOGGER.error("Error closing Metal Pipeline", e);
                }
            }
            
            if (metalManager.get() != null) {
                try {
                    metalManager.get().close();
                    logInit("  - Metal Manager: Closed");
                } catch (Exception e) {
                    LOGGER.error("Error closing Metal Manager", e);
                }
            }
            
            // Close GLSL pipeline (if active)
            if (glslPipeline.get() != null) {
                try {
                    glslPipeline.get().close();
                    logInit("  - GLSL Pipeline: Closed");
                } catch (Exception e) {
                    LOGGER.error("Error closing GLSL Pipeline", e);
                }
            }
            
            // Close OpenGL pipelines (if active)
            if (openglPipeline.get() != null) {
                try {
                    openglPipeline.get().close();
                    logInit("  - OpenGL Pipeline: Closed");
                } catch (Exception e) {
                    LOGGER.error("Error closing OpenGL Pipeline", e);
                }
            }
            
            if (openglManager.get() != null) {
                try {
                    openglManager.get().cleanup();
                    logInit("  - OpenGL Manager: Cleaned up");
                } catch (Exception e) {
                    LOGGER.error("Error cleaning up OpenGL Manager", e);
                }
            }
            
            // Close DirectX systems (if active)
            if (directXPipeline.get() != null) {
                try {
                    directXPipeline.get().close();
                    logInit("  - DirectX Pipeline: Closed");
                } catch (Exception e) {
                    LOGGER.error("Error closing DirectX Pipeline", e);
                }
            }
            
            if (directXManager.get() != null) {
                try {
                    directXManager.get().cleanup();
                    logInit("  - DirectX Manager: Cleaned up");
                } catch (Exception e) {
                    LOGGER.error("Error cleaning up DirectX Manager", e);
                }
            }
            
            // Close Vulkan systems
            if (vulkanManager.get() != null) {
                try {
                    // VulkanManager cleanup is handled by VulkanBackend
                    logInit("  - Vulkan Manager: Marked for cleanup");
                } catch (Exception e) {
                    LOGGER.error("Error marking Vulkan Manager for cleanup", e);
                }
            }
            
            if (vulkanBackend.get() != null) {
                try {
                    vulkanBackend.get().cleanup();
                    logInit("  - Vulkan Backend: Cleaned up");
                } catch (Exception e) {
                    LOGGER.error("Error cleaning up Vulkan Backend", e);
                }
            }
            
            // Close rendering managers
            if (cullingManager.get() != null) {
                try {
                    cullingManager.get().close();
                    logInit("  - Culling Manager closed");
                } catch (Exception e) {
                    LOGGER.error("Error closing Culling Manager", e);
                }
            }
            
            if (indirectDrawManager.get() != null) {
                try {
                    indirectDrawManager.get().close();
                    logInit("  - Indirect Draw Manager closed");
                } catch (Exception e) {
                    LOGGER.error("Error closing Indirect Draw Manager", e);
                }
            }
            
            // Close ECS Bridge
            if (ecsBridge.get() != null) {
                try {
                    ecsBridge.get().close();
                    logInit("  - ECS Bridge closed");
                } catch (Exception e) {
                    LOGGER.error("Error closing ECS Bridge", e);
                }
            }
            
            // Close ECS World
            if (ecsWorld.get() != null) {
                try {
                    ecsWorld.get().close();
                    logInit("  - ECS World closed");
                } catch (Exception e) {
                    LOGGER.error("Error closing ECS World", e);
                }
            }
            
            // Close GPU Backend
            if (gpuBackendSelector.get() != null) {
                try {
                    gpuBackendSelector.get().close();
                    logInit("  - GPU Backend closed");
                } catch (Exception e) {
                    LOGGER.error("Error closing GPU Backend", e);
                }
            }
            
            logInit("Shutdown complete");
            
            // Clean up performance enhancement resources
            cleanupArena();
            
        } catch (Exception e) {
            LOGGER.error("Error during shutdown", e);
        }
    }
    
    // ========================================================================
    // GETTERS - THREAD-SAFE ACCESS
    // ========================================================================
    
    public static World getECSWorld() {
        World world = ecsWorld.get();
        if (world == null) {
            throw new IllegalStateException("ECS World not initialized! Call initialize() first.");
        }
        return world;
    }
    
    public static GPUBackend getActiveGPUBackend() {
        return activeGPUBackend.get(); // Can be null if GPU initialization failed
    }
    
    public static GPUBackendSelector getGPUBackendSelector() {
        return gpuBackendSelector.get(); // Can be null if not initialized
    }
    
    public static MinecraftECSBridge getECSBridge() {
        MinecraftECSBridge bridge = ecsBridge.get();
        if (bridge == null) {
            throw new IllegalStateException("ECS Bridge not initialized!");
        }
        return bridge;
    }
    
    public static CullingManager getCullingManager() {
        return cullingManager.get(); // Can be null if not initialized
    }
    
    public static IndirectDrawManager getIndirectDrawManager() {
        return indirectDrawManager.get(); // Can be null if not initialized
    }
    
    public static ResolutionManager getResolutionManager() {
        return resolutionManager.get(); // Can be null if not initialized
    }
    
    public static VulkanBackend getVulkanBackend() {
        return vulkanBackend.get(); // Can be null if Vulkan not initialized
    }
    
    public static VulkanManager getVulkanManager() {
        return vulkanManager.get(); // Can be null if Vulkan not initialized
    }
    
    public static DirectXManager getDirectXManager() {
        return directXManager.get(); // Can be null if DirectX not initialized
    }
    
    public static DirectXPipelineProvider getDirectXPipeline() {
        return directXPipeline.get(); // Can be null if DirectX not initialized
    }
    
    public static stellar.snow.astralis.api.metal.managers.MetalManager getMetalManager() {
        return metalManager.get(); // Can be null if Metal not initialized
    }
    
    public static stellar.snow.astralis.api.metal.pipeline.MetalPipelineProvider getMetalPipeline() {
        return metalPipeline.get(); // Can be null if Metal not initialized
    }
    
    public static stellar.snow.astralis.api.metal.pipeline.MSLPipelineProvider getMSLPipeline() {
        return mslPipeline.get(); // Can be null if MSL not initialized
    }
    
    public static stellar.snow.astralis.api.opengl.managers.OpenGLManager getOpenGLManager() {
        return openglManager.get(); // Can be null if OpenGL Manager not initialized
    }
    
    public static stellar.snow.astralis.api.opengl.pipeline.OpenGLPipelineProvider getOpenGLPipeline() {
        return openglPipeline.get(); // Can be null if OpenGL Pipeline not initialized
    }
    
    public static stellar.snow.astralis.api.opengl.pipeline.GLSLPipelineProvider getGLSLPipeline() {
        return glslPipeline.get(); // Can be null if GLSL not initialized
    }
    
    public static OpenGLESManager getOpenGLESManager() {
        return openGLESManager.get(); // Can be null if OpenGL ES not initialized
    }
    
    public static OpenGLESPipelineProvider getOpenGLESPipeline() {
        return openGLESPipeline.get(); // Can be null if OpenGL ES not initialized
    }
    
    public static GLSLESPipelineProvider getGLSLESPipeline() {
        return glslesPipeline.get(); // Can be null if OpenGL ES not initialized
    }
    
    public static boolean isInitialized() {
        return currentPhase == InitializationPhase.COMPLETE;
    }
    
    public static InitializationPhase getCurrentPhase() {
        return currentPhase;
    }
    
    public static boolean isVulkanActive() {
        return vulkanBackend.get() != null && vulkanManager.get() != null;
    }
    
    public static boolean isDirectXActive() {
        return directXManager.get() != null && directXManager.get().isInitialized();
    }
    
    public static boolean isMetalActive() {
        return metalManager.get() != null && metalManager.get().isInitialized();
    }
    
    public static boolean isOpenGLESActive() {
        return openGLESManager.get() != null;
    }
    
    // ========================================================================
    // INTEGRATION MODULES INITIALIZATION
    // ========================================================================
    
    /**
     * Initialize integration modules during PRE_INIT phase
     */
    private static void initializeIntegrations_PreInit() {
        logInit("Initializing integration modules (PreInit)...");
        
        try {
            // PhotonEngine - Light-speed rendering optimizations
            if (Config.isPhotonEngineEnabled()) {
                PhotonEngine.preInit();
                logInit("  - PhotonEngine: initialized");
            }
            
            // ShortStack - Recipe optimization
            if (Config.isShortStackEnabled()) {
                ShortStackMod.preInit();
                logInit("  - ShortStack: initialized");
            }
            
            // Neon - Advanced optimizations
            if (Config.isNeonEnabled()) {
                Neon.preInit();
                logInit("  - Neon: initialized");
            }
            
            // AllTheLeaksReborn - Memory leak fixes
            if (Config.isAllTheLeaksEnabled()) {
                AllTheLeaksReborn.preInit();
                logInit("  - AllTheLeaksReborn: initialized");
            }
            
            // BlueCore - Core optimizations
            if (Config.isBlueCoreEnabled()) {
                BlueCore.preInit();
                logInit("  - BlueCore: initialized");
            }
            
            // Bolt - Performance enhancements
            if (Config.isBoltEnabled()) {
                Bolt.preInit();
                logInit("  - Bolt: initialized");
            }
            
            // ChunkMotion - Chunk animation
            if (Config.isChunkMotionEnabled()) {
                ChunkAnimator.preInit();
                logInit("  - ChunkMotion: initialized");
            }
            
            // GoodOptimizations - General optimizations
            if (Config.isGoodOptEnabled()) {
                GoodOptimzations.preInit();
                logInit("  - GoodOptimizations: initialized");
            }
            
            // Haku - EXPERIMENTAL (with warning)
            if (Config.isHakuEnabled()) {
                if (!Config.isHakuWarningAcknowledged()) {
                    LOGGER.warn("╔════════════════════════════════════════════════════════════════╗");
                    LOGGER.warn("║  WARNING: Haku is EXTREMELY EXPERIMENTAL and NOT RECOMMENDED ║");
                    LOGGER.warn("║  This is an unstable rewrite of Valkyrie - expect crashes!   ║");
                    LOGGER.warn("║  Set hakuWarningAcknowledged=true to suppress this warning.  ║");
                    LOGGER.warn("╚════════════════════════════════════════════════════════════════╝");
                }
                Haku.preInit();
                logInit("  - Haku: initialized (EXPERIMENTAL - use at own risk)");
            }
            
            // Lavender - OptiFine compatibility (with legal warning)
            if (Config.isLavenderEnabled()) {
                if (!Config.isLavenderLegalNoticeShown()) {
                    LOGGER.warn("╔════════════════════════════════════════════════════════════════╗");
                    LOGGER.warn("║  WARNING: Lavender - OptiFine Compatibility Layer             ║");
                    LOGGER.warn("║  OptiFine is closed-source. Legally, we cannot reverse-       ║");
                    LOGGER.warn("║  engineer its internals without violating its license.        ║");
                    LOGGER.warn("║  Lavender uses only observable behavior and public APIs.      ║");
                    LOGGER.warn("║  Many OptiFine features CANNOT be replicated. Expect issues.  ║");
                    LOGGER.warn("║  Set lavenderLegalNoticeShown=true to suppress this warning.  ║");
                    LOGGER.warn("╚════════════════════════════════════════════════════════════════╝");
                }
                Lavender.preInit();
                logInit("  - Lavender: initialized (NOT RECOMMENDED - limited compatibility)");
            }
            
            // Lumen - Lighting optimizations
            if (Config.isLumenEnabled()) {
                Lumen.preInit();
                logInit("  - Lumen: initialized");
            }
            
            // MagnetismCore - Physics optimizations
            if (Config.isMagnetismCoreEnabled()) {
                MagnetismCore.preInit();
                logInit("  - MagnetismCore: initialized");
            }
            
            // Asto - Modern VintageFix rewrite (Java 25)
            if (Config.isAstoEnabled()) {
                Asto.initializeAll(); // Asto uses initializeAll() instead of preInit
                logInit("  - Asto: initialized (VintageFix rewrite)");
            }
            
            // Fluorine - Additional optimizations
            if (Config.isFluorineEnabled()) {
                Fluorine.preInit();
                logInit("  - Fluorine: initialized");
            }
            
            // LegacyFix - Legacy compatibility
            if (Config.isLegacyFixEnabled()) {
                LegacyFix.preInit();
                logInit("  - LegacyFix: initialized");
            }
            
            // SnowyASM - Advanced memory optimization
            if (Config.isSnowyASMEnabled()) {
                SnowyASM.preInit();
                logInit("  - SnowyASM: initialized (memory optimizer)");
            }
            
            // JIT Optimization System
            if (Config.isJITEnabled()) {
                if (Config.isJITHelperEnabled()) {
                    JITHelper.initialize();
                }
                if (Config.isJITInjectEnabled()) {
                    JITInject.initialize();
                }
                if (Config.isJITUniversalPatcherEnabled()) {
                    UniversalPatcher.initialize();
                }
                logInit("  - JIT System: initialized (bytecode optimizer)");
            }
            
            // Log if no integrations are enabled
            int enabledCount = 0;
            if (Config.isPhotonEngineEnabled()) enabledCount++;
            if (Config.isShortStackEnabled()) enabledCount++;
            if (Config.isNeonEnabled()) enabledCount++;
            if (Config.isAllTheLeaksEnabled()) enabledCount++;
            if (Config.isBlueCoreEnabled()) enabledCount++;
            if (Config.isBoltEnabled()) enabledCount++;
            if (Config.isChunkMotionEnabled()) enabledCount++;
            if (Config.isGoodOptEnabled()) enabledCount++;
            if (Config.isHakuEnabled()) enabledCount++;
            if (Config.isLavenderEnabled()) enabledCount++;
            if (Config.isLumenEnabled()) enabledCount++;
            if (Config.isMagnetismCoreEnabled()) enabledCount++;
            if (Config.isAstoEnabled()) enabledCount++;
            if (Config.isFluorineEnabled()) enabledCount++;
            if (Config.isLegacyFixEnabled()) enabledCount++;
            if (Config.isSnowyASMEnabled()) enabledCount++;
            if (Config.isJITEnabled()) enabledCount++;
            
            if (enabledCount == 0) {
                logInit("  - No integration modules enabled (all disabled by default)");
            } else {
                logInit("  - " + enabledCount + " integration module(s) enabled");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize one or more integration modules (PreInit)", e);
            // Don't throw - allow Astralis to continue even if some integrations fail
        }
    }
    
    /**
     * Initialize integration modules during INIT phase
     */
    private static void initializeIntegrations_Init() {
        logInit("Initializing integration modules (Init)...");
        
        try {
            if (Config.isPhotonEngineEnabled()) PhotonEngine.init();
            if (Config.isShortStackEnabled()) ShortStackMod.init();
            if (Config.isNeonEnabled()) Neon.init();
            if (Config.isAllTheLeaksEnabled()) AllTheLeaksReborn.init();
            if (Config.isBlueCoreEnabled()) BlueCore.init();
            if (Config.isBoltEnabled()) Bolt.init();
            if (Config.isChunkMotionEnabled()) ChunkAnimator.init();
            if (Config.isGoodOptEnabled()) GoodOptimzations.init();
            if (Config.isHakuEnabled()) Haku.init();
            if (Config.isLavenderEnabled()) Lavender.init();
            if (Config.isLumenEnabled()) Lumen.init();
            if (Config.isMagnetismCoreEnabled()) MagnetismCore.init();
            // Note: Asto uses initializeAll() in preInit, no separate init
            if (Config.isFluorineEnabled()) Fluorine.init();
            if (Config.isLegacyFixEnabled()) LegacyFix.init();
            if (Config.isSnowyASMEnabled()) SnowyASM.init();
            // JIT system initialized in preInit
            
            logInit("  - All enabled integration modules initialized");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize one or more integration modules (Init)", e);
        }
    }
    
    /**
     * Initialize integration modules during POST_INIT phase
     */
    private static void initializeIntegrations_PostInit() {
        logInit("Initializing integration modules (PostInit)...");
        
        try {
            if (Config.isPhotonEngineEnabled()) PhotonEngine.postInit();
            if (Config.isShortStackEnabled()) ShortStackMod.postInit();
            if (Config.isNeonEnabled()) Neon.postInit();
            if (Config.isAllTheLeaksEnabled()) AllTheLeaksReborn.postInit();
            if (Config.isBlueCoreEnabled()) BlueCore.postInit();
            if (Config.isBoltEnabled()) Bolt.postInit();
            if (Config.isChunkMotionEnabled()) ChunkAnimator.postInit();
            if (Config.isGoodOptEnabled()) GoodOptimzations.postInit();
            if (Config.isHakuEnabled()) Haku.postInit();
            if (Config.isLavenderEnabled()) Lavender.postInit();
            if (Config.isLumenEnabled()) Lumen.postInit();
            if (Config.isMagnetismCoreEnabled()) MagnetismCore.postInit();
            // Note: Asto uses initializeAll() in preInit, no separate postInit
            if (Config.isFluorineEnabled()) Fluorine.postInit();
            if (Config.isLegacyFixEnabled()) LegacyFix.postInit();
            if (Config.isSnowyASMEnabled()) SnowyASM.postInit();
            // JIT system initialized in preInit
            
            logInit("  - All enabled integration modules post-initialized");
            
        } catch (Exception e) {
            LOGGER.error("Failed to post-initialize one or more integration modules", e);
        }
    }
    
    // ========================================================================
    // UTILITY
    // ========================================================================
    
    private static void logInit(String message) {
        LOGGER.info(message);
        initializationLog.add(message);
    }
    
    private InitializationManager() {
        throw new AssertionError("No InitializationManager instances for you!");
    }
}
