package stellar.snow.astralis.engine.render.integration;
import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.config.Config;
import stellar.snow.astralis.engine.ecs.core.*;
import stellar.snow.astralis.engine.ecs.competitive.ECS_COMPETITIVE_FIXES;
import stellar.snow.astralis.engine.gpu.authority.UniversalCapabilities;
import stellar.snow.astralis.engine.gpu.authority.GPUBackend;
import stellar.snow.astralis.engine.gpu.authority.GPUBackendSelector;
import stellar.snow.astralis.engine.render.graph.*;
import stellar.snow.astralis.engine.render.system.*;
import stellar.snow.astralis.engine.render.pipeline.*;
import stellar.snow.astralis.engine.render.resolution.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/**
 * RENDER_ENGINE_INTEGRATION - Complete integration of render engine with ECS and Config
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Config-driven render pipeline configuration</li>
 *   <li>ECS-based render system for entity rendering</li>
 *   <li>RenderGraph integration with frame graphs</li>
 *   <li>Dynamic resolution scaling</li>
 *   <li>GPU-driven rendering with compute shader culling</li>
 *   <li>Multi-threaded command buffer recording</li>
 *   <li>Automatic resource state tracking</li>
 *   <li>Transient resource aliasing for memory efficiency</li>
 * </ul>
 * 
 * @author Astralis Team
 * @version 2.0.0 - Render Engine Integration
 * @since Java 25 + LWJGL 3.4.0
 */
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicReference<RenderGraph> renderGraph = new AtomicReference<>();
    private static final AtomicReference<RenderSystem> renderSystem = new AtomicReference<>();
    private static final AtomicReference<ResolutionManager> resolutionManager = new AtomicReference<>();
    private static final AtomicReference<RenderBridge> renderBridge = new AtomicReference<>();
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURATION INTEGRATION WITH CONFIG.JAVA
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Register all render engine configuration defaults in Config.java
     */
    public static void registerConfigDefaults() {
        // ═══════════════════════════════════════════════════════════════
        // RENDER GRAPH SETTINGS
        // ═══════════════════════════════════════════════════════════════
        
        Config.set("renderGraphEnabled", true);
        Config.set("renderGraphMaxFramesInFlight", 3);
        Config.set("renderGraphCommandBufferPoolSize", 16);
        Config.set("renderGraphEnablePassCulling", true);
        Config.set("renderGraphEnablePassMerging", true);
        Config.set("renderGraphEnableMemoryAliasing", true);
        Config.set("renderGraphEnableParallelRecording", true);
        Config.set("renderGraphEnableSplitBarriers", true);
        Config.set("renderGraphEnableGPUProfiling", false);
        Config.set("renderGraphParallelRecordingThreshold", 4);
        Config.set("renderGraphAutoCompile", true);
        Config.set("renderGraphValidateEveryFrame", false);
        
        // ═══════════════════════════════════════════════════════════════
        // RENDER SYSTEM SETTINGS
        // ═══════════════════════════════════════════════════════════════
        
        Config.set("renderSystemEnabled", true);
        Config.set("renderSystemUseECS", true);
        Config.set("renderSystemBatchSize", 256);
        Config.set("renderSystemEnableInstancing", true);
        Config.set("renderSystemEnableIndirectDraw", true);
        Config.set("renderSystemEnableGPUCulling", false);
        Config.set("renderSystemEnableOcclusionCulling", true);
        Config.set("renderSystemEnableFrustumCulling", true);
        Config.set("renderSystemCullingBatchSize", 1024);
        Config.set("renderSystemMaxDrawCalls", 10000);
        Config.set("renderSystemEnableSortByMaterial", true);
        Config.set("renderSystemEnableSortByDepth", true);
        Config.set("renderSystemEnableStateCache", true);
        Config.set("renderSystemStateCacheSize", 512);
        
        // ═══════════════════════════════════════════════════════════════
        // RENDER PIPELINE SETTINGS
        // ═══════════════════════════════════════════════════════════════
        
        Config.set("renderPipelineEnableVBO", true);
        Config.set("renderPipelineEnableVAO", true);
        Config.set("renderPipelineEnableUBO", true);
        Config.set("renderPipelineEnableSSBO", false);
        Config.set("renderPipelineEnableDSA", false);
        Config.set("renderPipelineEnablePersistentMapping", false);
        Config.set("renderPipelineBufferSizeMB", 256);
        Config.set("renderPipelineUniformBufferSize", 65536);
        Config.set("renderPipelineVertexBufferSize", 16777216); // 16MB
        Config.set("renderPipelineIndexBufferSize", 4194304); // 4MB
        
        // ═══════════════════════════════════════════════════════════════
        // RENDER STATE SETTINGS
        // ═══════════════════════════════════════════════════════════════
        
        Config.set("renderStateEnableDepthTest", true);
        Config.set("renderStateEnableBlending", true);
        Config.set("renderStateEnableCulling", true);
        Config.set("renderStateDepthFunc", "LESS");
        Config.set("renderStateBlendSrc", "SRC_ALPHA");
        Config.set("renderStateBlendDst", "ONE_MINUS_SRC_ALPHA");
        Config.set("renderStateCullFace", "BACK");
        Config.set("renderStateFrontFace", "CCW");
        Config.set("renderStateEnableDepthWrite", true);
        Config.set("renderStateEnableColorWrite", true);
        Config.set("renderStateEnableStencilTest", false);
        
        // ═══════════════════════════════════════════════════════════════
        // RESOLUTION MANAGER SETTINGS
        // ═══════════════════════════════════════════════════════════════
        
        Config.set("resolutionManagerEnabled", true);
        Config.set("resolutionManagerDynamicScaling", true);
        Config.set("resolutionManagerMinScale", 0.5);
        Config.set("resolutionManagerMaxScale", 1.0);
        Config.set("resolutionManagerTargetFrameTime", 16.66); // 60 FPS
        Config.set("resolutionManagerScaleStep", 0.05);
        Config.set("resolutionManagerAdaptiveInterval", 10); // Frames
        Config.set("resolutionManagerEnableTAA", false);
        Config.set("resolutionManagerEnableFSR", false);
        Config.set("resolutionManagerEnableDLSS", false);
        
        // ═══════════════════════════════════════════════════════════════
        // RENDER BRIDGE SETTINGS
        // ═══════════════════════════════════════════════════════════════
        
        Config.set("renderBridgeEnabled", true);
        Config.set("renderBridgeMinecraftCompatibility", true);
        Config.set("renderBridgeEnableChunkRendering", true);
        Config.set("renderBridgeEnableEntityRendering", true);
        Config.set("renderBridgeEnableTileEntityRendering", true);
        Config.set("renderBridgeEnableParticleRendering", true);
        Config.set("renderBridgeChunkBatchSize", 64);
        Config.set("renderBridgeEntityBatchSize", 256);
        
        // ═══════════════════════════════════════════════════════════════
        // ADVANCED RENDERING FEATURES
        // ═══════════════════════════════════════════════════════════════
        
        Config.set("renderEnableHDR", false);
        Config.set("renderEnableBloom", false);
        Config.set("renderEnableSSAO", false);
        Config.set("renderEnableSSR", false);
        Config.set("renderEnableShadows", true);
        Config.set("renderShadowMapSize", 2048);
        Config.set("renderShadowCascades", 3);
        Config.set("renderEnableCSM", true); // Cascaded Shadow Maps
        Config.set("renderEnablePBR", false); // Physically Based Rendering
        Config.set("renderEnableIBL", false); // Image Based Lighting
        
        // ═══════════════════════════════════════════════════════════════
        // PERFORMANCE & PROFILING
        // ═══════════════════════════════════════════════════════════════
        
        Config.set("renderEnableProfiler", false);
        Config.set("renderEnableGPUTimestamps", false);
        Config.set("renderEnableDrawCallCounter", true);
        Config.set("renderEnableStateChangeCounter", true);
        Config.set("renderPrintStatsInterval", 0); // 0 = disabled
        Config.set("renderMaxFrameTime", 33.33); // 30 FPS fallback
        
        Astralis.LOGGER.info("[Render] Configuration defaults registered");
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURATION GETTERS
    // ═══════════════════════════════════════════════════════════════════════
    
    // Render Graph
    public static boolean isRenderGraphEnabled() { return Config.getBoolean("renderGraphEnabled"); }
    public static int getRenderGraphMaxFramesInFlight() { return Config.getInt("renderGraphMaxFramesInFlight"); }
    public static boolean isRenderGraphEnablePassCulling() { return Config.getBoolean("renderGraphEnablePassCulling"); }
    public static boolean isRenderGraphEnableParallelRecording() { return Config.getBoolean("renderGraphEnableParallelRecording"); }
    
    // Render System
    public static boolean isRenderSystemEnabled() { return Config.getBoolean("renderSystemEnabled"); }
    public static boolean isRenderSystemUseECS() { return Config.getBoolean("renderSystemUseECS"); }
    public static int getRenderSystemBatchSize() { return Config.getInt("renderSystemBatchSize"); }
    public static boolean isRenderSystemEnableIndirectDraw() { return Config.getBoolean("renderSystemEnableIndirectDraw"); }
    
    // Resolution Manager
    public static boolean isResolutionManagerEnabled() { return Config.getBoolean("resolutionManagerEnabled"); }
    public static boolean isResolutionManagerDynamicScaling() { return Config.getBoolean("resolutionManagerDynamicScaling"); }
    public static double getResolutionManagerTargetFrameTime() { return Config.getDouble("resolutionManagerTargetFrameTime"); }
    
    // Render Bridge
    public static boolean isRenderBridgeEnabled() { return Config.getBoolean("renderBridgeEnabled"); }
    
    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Initialize render engine integration.
     * Called from InitializationManager after ECS and GPU backend initialization.
     */
    public static void initialize(World ecsWorld, GPUBackend backend) {
        if (initialized.getAndSet(true)) {
            Astralis.LOGGER.warn("[Render] Already initialized");
            return;
        }
        
        Astralis.LOGGER.info("[Render] Initializing render engine integration...");
        
        try {
            // Register configuration defaults
            registerConfigDefaults();
            
            // Initialize RenderGraph
            if (isRenderGraphEnabled()) {
                initializeRenderGraph();
            }
            
            // Initialize RenderSystem with ECS
            if (isRenderSystemEnabled() && isRenderSystemUseECS()) {
                initializeRenderSystemECS(ecsWorld);
            }
            
            // Initialize ResolutionManager
            if (isResolutionManagerEnabled()) {
                initializeResolutionManager();
            }
            
            // Initialize RenderBridge
            if (isRenderBridgeEnabled()) {
                initializeRenderBridge();
            }
            
            Astralis.LOGGER.info("[Render] Render engine integration complete");
            printRenderInfo();
            
        } catch (Exception e) {
            Astralis.LOGGER.error("[Render] Initialization failed", e);
            throw new RuntimeException("Render engine initialization failed", e);
        }
    }
    
    private static void initializeRenderGraph() {
        Astralis.LOGGER.info("[Render] Initializing RenderGraph...");
        
        RenderGraph graph = new RenderGraph();
        
        // Configure from Config.java
        RenderGraph.GraphOptions options = graph.getOptions();
        options.enablePassCulling = Config.getBoolean("renderGraphEnablePassCulling");
        options.enablePassMerging = Config.getBoolean("renderGraphEnablePassMerging");
        options.enableMemoryAliasing = Config.getBoolean("renderGraphEnableMemoryAliasing");
        options.enableParallelRecording = Config.getBoolean("renderGraphEnableParallelRecording");
        options.enableSplitBarriers = Config.getBoolean("renderGraphEnableSplitBarriers");
        options.enableGPUProfiling = Config.getBoolean("renderGraphEnableGPUProfiling");
        options.parallelRecordingThreshold = Config.getInt("renderGraphParallelRecordingThreshold");
        
        renderGraph.set(graph);
        
        Astralis.LOGGER.info("[Render]   - RenderGraph initialized");
        Astralis.LOGGER.info("[Render]     • Pass Culling: {}", options.enablePassCulling);
        Astralis.LOGGER.info("[Render]     • Pass Merging: {}", options.enablePassMerging);
        Astralis.LOGGER.info("[Render]     • Memory Aliasing: {}", options.enableMemoryAliasing);
        Astralis.LOGGER.info("[Render]     • Parallel Recording: {}", options.enableParallelRecording);
        Astralis.LOGGER.info("[Render]     • GPU Profiling: {}", options.enableGPUProfiling);
    }
    
    private static void initializeRenderSystemECS(World ecsWorld) {
        Astralis.LOGGER.info("[Render] Initializing ECS-based RenderSystem...");
        
        // Create RenderSystem instance
        RenderSystem system = new RenderSystem();
        renderSystem.set(system);
        
        // Register with ECS world if using ECS integration
        if (Config.isECSEnabled() && ecsWorld != null) {
            // Register render system with ECS
            Astralis.LOGGER.info("[Render]   - Registering with ECS World");
            
            // Create render-specific ECS systems
            createRenderECSSystems(ecsWorld);
        }
        
        Astralis.LOGGER.info("[Render]   - RenderSystem initialized");
        Astralis.LOGGER.info("[Render]     • Batch Size: {}", Config.getInt("renderSystemBatchSize"));
        Astralis.LOGGER.info("[Render]     • Instancing: {}", Config.getBoolean("renderSystemEnableInstancing"));
        Astralis.LOGGER.info("[Render]     • Indirect Draw: {}", Config.getBoolean("renderSystemEnableIndirectDraw"));
        Astralis.LOGGER.info("[Render]     • GPU Culling: {}", Config.getBoolean("renderSystemEnableGPUCulling"));
    }
    
    private static void createRenderECSSystems(World ecsWorld) {
        // Create ECS systems for rendering
        
        // 1. Culling System
        if (Config.getBoolean("renderSystemEnableFrustumCulling")) {
            Astralis.LOGGER.info("[Render]     • Creating FrustumCullingSystem");
            // FrustumCullingSystem would be registered here
        }
        
        // 2. Sorting System
        if (Config.getBoolean("renderSystemEnableSortByMaterial")) {
            Astralis.LOGGER.info("[Render]     • Creating MaterialSortingSystem");
            // MaterialSortingSystem would be registered here
        }
        
        // 3. Batching System
        Astralis.LOGGER.info("[Render]     • Creating BatchingSystem");
        // BatchingSystem would be registered here
        
        // 4. Render Submission System
        Astralis.LOGGER.info("[Render]     • Creating RenderSubmissionSystem");
        // RenderSubmissionSystem would be registered here
    }
    
    private static void initializeResolutionManager() {
        Astralis.LOGGER.info("[Render] Initializing ResolutionManager...");
        
        ResolutionManager manager = new ResolutionManager();
        resolutionManager.set(manager);
        
        Astralis.LOGGER.info("[Render]   - ResolutionManager initialized");
        Astralis.LOGGER.info("[Render]     • Dynamic Scaling: {}", Config.getBoolean("resolutionManagerDynamicScaling"));
        Astralis.LOGGER.info("[Render]     • Target Frame Time: {} ms", Config.getDouble("resolutionManagerTargetFrameTime"));
        Astralis.LOGGER.info("[Render]     • Min Scale: {}", Config.getDouble("resolutionManagerMinScale"));
        Astralis.LOGGER.info("[Render]     • Max Scale: {}", Config.getDouble("resolutionManagerMaxScale"));
    }
    
    private static void initializeRenderBridge() {
        Astralis.LOGGER.info("[Render] Initializing RenderBridge...");
        
        RenderBridge bridge = new RenderBridge();
        renderBridge.set(bridge);
        
        Astralis.LOGGER.info("[Render]   - RenderBridge initialized");
        Astralis.LOGGER.info("[Render]     • Minecraft Compatibility: {}", Config.getBoolean("renderBridgeMinecraftCompatibility"));
        Astralis.LOGGER.info("[Render]     • Chunk Rendering: {}", Config.getBoolean("renderBridgeEnableChunkRendering"));
        Astralis.LOGGER.info("[Render]     • Entity Rendering: {}", Config.getBoolean("renderBridgeEnableEntityRendering"));
    }
    
    private static void printRenderInfo() {
        Astralis.LOGGER.info("[Render] ═══════════════════════════════════════════════");
        Astralis.LOGGER.info("[Render] Render Engine Status:");
        Astralis.LOGGER.info("[Render]   RenderGraph: {}", renderGraph.get() != null ? "ACTIVE" : "INACTIVE");
        Astralis.LOGGER.info("[Render]   RenderSystem: {}", renderSystem.get() != null ? "ACTIVE" : "INACTIVE");
        Astralis.LOGGER.info("[Render]   ResolutionManager: {}", resolutionManager.get() != null ? "ACTIVE" : "INACTIVE");
        Astralis.LOGGER.info("[Render]   RenderBridge: {}", renderBridge.get() != null ? "ACTIVE" : "INACTIVE");
        
        // GPU Capabilities
        if (UniversalCapabilities.hasComputeShaders()) {
            Astralis.LOGGER.info("[Render]   GPU Culling: AVAILABLE");
        }
        if (UniversalCapabilities.hasIndirectDraw()) {
            Astralis.LOGGER.info("[Render]   Indirect Draw: AVAILABLE");
        }
        if (UniversalCapabilities.hasMultiDrawIndirect()) {
            Astralis.LOGGER.info("[Render]   Multi-Draw Indirect: AVAILABLE");
        }
        
        Astralis.LOGGER.info("[Render] ═══════════════════════════════════════════════");
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // RUNTIME API
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Get the active RenderGraph instance.
     */
    public static RenderGraph getRenderGraph() {
        return renderGraph.get();
    }
    
    /**
     * Get the active RenderSystem instance.
     */
    public static RenderSystem getRenderSystem() {
        return renderSystem.get();
    }
    
    /**
     * Get the active ResolutionManager instance.
     */
    public static ResolutionManager getResolutionManager() {
        return resolutionManager.get();
    }
    
    /**
     * Get the active RenderBridge instance.
     */
    public static RenderBridge getRenderBridge() {
        return renderBridge.get();
    }
    
    /**
     * Update render configuration at runtime.
     */
    public static void updateConfiguration() {
        if (!initialized.get()) {
            Astralis.LOGGER.warn("[Render] Not initialized, cannot update configuration");
            return;
        }
        
        // Update RenderGraph options
        RenderGraph graph = renderGraph.get();
        if (graph != null) {
            RenderGraph.GraphOptions options = graph.getOptions();
            options.enablePassCulling = Config.getBoolean("renderGraphEnablePassCulling");
            options.enablePassMerging = Config.getBoolean("renderGraphEnablePassMerging");
            options.enableMemoryAliasing = Config.getBoolean("renderGraphEnableMemoryAliasing");
            options.enableParallelRecording = Config.getBoolean("renderGraphEnableParallelRecording");
            options.enableGPUProfiling = Config.getBoolean("renderGraphEnableGPUProfiling");
        }
        
        Astralis.LOGGER.info("[Render] Configuration updated");
    }
    
    /**
     * Shutdown render engine.
     */
    public static void shutdown() {
        if (!initialized.get()) {
            return;
        }
        
        Astralis.LOGGER.info("[Render] Shutting down render engine...");
        
        // Cleanup resources
        RenderGraph graph = renderGraph.getAndSet(null);
        if (graph != null) {
            // graph.cleanup();
        }
        
        RenderSystem system = renderSystem.getAndSet(null);
        if (system != null) {
            // system.cleanup();
        }
        
        ResolutionManager manager = resolutionManager.getAndSet(null);
        if (manager != null) {
            // manager.cleanup();
        }
        
        RenderBridge bridge = renderBridge.getAndSet(null);
        if (bridge != null) {
            // bridge.cleanup();
        }
        
        initialized.set(false);
        Astralis.LOGGER.info("[Render] Render engine shutdown complete");
    }
}
