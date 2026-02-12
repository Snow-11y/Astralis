/**
 * Config - Astralis Configuration Manager
 *
 * <h2>Purpose:</h2>
 * Central configuration system for Astralis Universal Patcher.
 * Manages all user-configurable settings with runtime validation
 * against hardware capabilities via UniversalCapabilities.
 *
 * <h2>Configuration Categories:</h2>
 * <ul>
 *   <li>Graphics API Selection (OpenGL/GLES/Vulkan/DirectX)</li>
 *   <li>Shader Engine Selection (GLSL/HLSL/SPIR-V)</li>
 *   <li>HLSL Shader Compilation (Shader Model, DXC/D3DCompiler, Optimization)</li>
 *   <li>DirectX Features (DX9-12, Ray Tracing, Mesh Shaders, VRS)</li>
 *   <li>Vulkan Zero-Overhead Safety System (Memory tracking, deadlock detection, validation)</li>
 *   <li>Performance Thresholds</li>
 *   <li>Compatibility Modes</li>
 *   <li>Logging Settings</li>
 * </ul>
 *
 * <h2>Vulkan Zero-Overhead Safety System:</h2>
 * <p>Controls VulkanCallMapper's safety checks via compile-time JIT optimization:</p>
 * <ul>
 *   <li><b>vulkanSafetyChecksEnabled:</b> Master switch (default: false for zero overhead)</li>
 *   <li><b>vulkanSafetyFFITracking:</b> Use FFI native tracking vs Java (~10-20ns vs ~50-100ns)</li>
 *   <li><b>vulkanMemorySourceTracking:</b> Prevent LWJGL/FFM memory API mixing (JVM crash protection)</li>
 *   <li><b>vulkanDeadlockDetection:</b> Monitor lock acquisitions with timeouts</li>
 *   <li><b>vulkanBindlessValidation:</b> Validate descriptor indices before GPU access</li>
 *   <li><b>vulkanLeakDetection:</b> Report unfreed memory at shutdown</li>
 * </ul>
 * <p>When safety checks are disabled (default production mode), the JIT compiler completely
 * eliminates all safety code through dead code elimination, resulting in TRUE zero overhead -
 * not "low overhead", but literally zero additional CPU cycles.</p>
 * 
 * <p>See {@link stellar.snow.astralis.api.vulkan.mapping.VulkanCallMapper} for implementation details
 * and {@link stellar.snow.astralis.core.InitializationManager#setupVulkanSafetySystem()} for
 * initialization sequence.</p>
 *
 * @author Astralis Team
 * @version 1.0.0 - Java 25 / LWJGL 3.3.6
 * @since Astralis 1.0
 */

package stellar.snow.astralis.config;

// ═══════════════════════════════════════════════════════════════════════════════════════
// Astralis Internal Imports
// ═══════════════════════════════════════════════════════════════════════════════════════
import stellar.snow.astralis.engine.gpu.authority.UniversalCapabilities;

// ═══════════════════════════════════════════════════════════════════════════════════════
// Java Imports
// ═══════════════════════════════════════════════════════════════════════════════════════
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Central configuration manager for Astralis
 */
public final class Config {

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 1: CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /** Configuration file name */
    private static final String CONFIG_FILE_NAME = "astralis.cfg";

    /** Configuration directory */
    private static final String CONFIG_DIRECTORY = "config";

    /** Configuration version for migration */
    private static final int CONFIG_VERSION = 1;

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 2: ENUMS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Preferred Graphics API
     */
    public enum PreferredAPI {
        AUTO("Automatic - Best available"),
        OPENGL("OpenGL - Desktop standard"),
        DIRECTX("DirectX - Windows specialized"),
        METAL("Metal - macOS/iOS specialized"),
        OPENGL_ES("OpenGL ES - Mobile/Embedded"),
        VULKAN("Vulkan - Modern low-level API");

        public final String description;

        PreferredAPI(String description) {
            this.description = description;
        }
    }

    /**
     * Preferred Shader Engine
     */
    public enum ShaderEngine {
        AUTO("Automatic - Best available"),
        GLSL("GLSL - OpenGL Shading Language"),
        GLSL_ES("GLSL ES - OpenGL ES Shading Language - Mobile/Embedded"),
        HLSL("HLSL - High Level Shading Language (DirectX)"),
        MSL("Metal Shading language - Apple's baby"),
        SPIRV("SPIR-V - Standard Portable Intermediate Representation");

        public final String description;

        ShaderEngine(String description) {
            this.description = description;
        }
    }

    /**
     * Renderer Override
     */
    public enum RendererOverride {
        AUTO("Automatic detection"),
        VANILLA("Force Vanilla renderer"),
        OPTIFINE("Force Optifine compatibility"),
        SODIUM("Force Sodium compatibility"),
        RUBIDIUM("Force Rubidium compatibility"),
        EMBEDDIUM("Force Embeddium compatibility"),
        CELERITAS("Force Celeritas compatibility"),
        NOTHIRIUM("Force Nothirium compatibility"),
        NEONIUM("Force Neonium compatibility"),
        RELICTIUM("Force Relictium compatibility"),
        VINTAGIUM("Force Vintagium compatibility"),
        KIRINO("Force Kirino compatibility"),
        SNOWIUM("Force Snowium compatibility");

        public final String description;

        RendererOverride(String description) {
            this.description = description;
        }
    }

    /**
     * Logging Level
     */
    public enum LoggingLevel {
        OFF("No logging"),
        MINIMAL("Errors only"),
        NORMAL("Important events"),
        VERBOSE("All calls"),
        DEBUG("Maximum detail");

        public final String description;

        LoggingLevel(String description) {
            this.description = description;
        }
    }

    /**
     * Performance Profile
     */
    public enum PerformanceProfile {
        POTATO("Minimal features, maximum compatibility"),
        LOW("Basic features, high compatibility"),
        BALANCED("Balance of features and performance"),
        HIGH("Modern features, good hardware required"),
        ULTRA("All features, best hardware required"),
        CUSTOM("User-defined settings");

        public final String description;

        PerformanceProfile(String description) {
            this.description = description;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 3: STATE
    // ═══════════════════════════════════════════════════════════════════════════════════

    /** Initialization flag */
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    /** Configuration file path */
    private static Path configPath;

    /** Configuration values storage */
    private static final ConcurrentHashMap<String, Object> values = new ConcurrentHashMap<>();

    /** Default values */
    private static final Map<String, Object> defaults = new LinkedHashMap<>();

    /** Change listeners */
    private static final ConcurrentHashMap<String, Runnable> changeListeners = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 4: DEFAULT VALUES
    // ═══════════════════════════════════════════════════════════════════════════════════

    static {
        // API Settings
        defaults.put("preferredAPI", PreferredAPI.AUTO);
        defaults.put("shaderEngine", ShaderEngine.AUTO);
        defaults.put("rendererOverride", RendererOverride.AUTO);

        // Performance Profile
        defaults.put("performanceProfile", PerformanceProfile.BALANCED);

        // Thresholds
        defaults.put("teleportThresholdSquared", 64.0);
        defaults.put("maxFrameTime", 0.25);
        defaults.put("fixedTimestep", 0.05); // 1/20
        defaults.put("aiThrottleMask", 1);
        defaults.put("batchSize", 256);

        // State Management
        defaults.put("enableStateCache", true);
        defaults.put("stateValidationInterval", 60);
        defaults.put("syncStateOnExternalChange", true);

        // Exception Safety
        defaults.put("enableExceptionSafety", true);
        defaults.put("maxStateStackDepth", 16);

        // Logging
        defaults.put("loggingLevel", LoggingLevel.NORMAL);
        defaults.put("logBufferSize", 1024);
        defaults.put("logFlushIntervalMs", 5000L);
        defaults.put("logCallDetails", true);
        defaults.put("logFallbacks", true);

        // Compatibility
        defaults.put("allowFallback", true);
        defaults.put("maxFallbackAttempts", 5);
        defaults.put("skipStateManagerOverwrite", false);
        defaults.put("deferStateChanges", false);

        // Engine Version Limits
        defaults.put("minGLMajor", 1);
        defaults.put("minGLMinor", 5);
        defaults.put("maxGLMajor", 4);
        defaults.put("maxGLMinor", 6);
        defaults.put("minGLESMajor", 2);
        defaults.put("minGLESMinor", 0);
        defaults.put("maxGLESMajor", 3);
        defaults.put("maxGLESMinor", 2);
        defaults.put("minVulkanMajor", 1);
        defaults.put("minVulkanMinor", 0);
        defaults.put("maxVulkanMajor", 1);
        defaults.put("maxVulkanMinor", 4);

        // Vulkan-Specific Settings
        defaults.put("vulkanValidationLayers", false); // Enable Vulkan validation layers
        defaults.put("vulkanDebugMode", false); // Enable Vulkan debug utilities
        defaults.put("vulkanGPUAssistedValidation", false); // GPU-assisted validation (slow)
        defaults.put("vulkanStrictMode", false); // Throw on unsupported features vs no-op
        defaults.put("vulkanEnableTimelineSemaphores", true); // Use timeline semaphores
        defaults.put("vulkanEnableDynamicRendering", true); // Use dynamic rendering (VK 1.3+)
        defaults.put("vulkanEnableSynchronization2", true); // Use sync2 barriers
        defaults.put("vulkanEnableBufferDeviceAddress", true); // Bindless vertex pulling
        defaults.put("vulkanEnableDescriptorIndexing", true); // Bindless textures
        defaults.put("vulkanEnableMeshShaders", false); // Mesh shaders (VK 1.4+)
        defaults.put("vulkanEnableGPUDrivenRendering", false); // GPU-driven draw submission
        defaults.put("vulkanEnableGPUCulling", false); // GPU-side culling
        defaults.put("vulkanEnableMultiDrawIndirect", true); // Multi-draw indirect
        defaults.put("vulkanEnableIndirectCount", true); // MDI with count (VK 1.2+)
        defaults.put("vulkanMaxFramesInFlight", 3); // Triple buffering
        defaults.put("vulkanCommandBatchSize", 256); // Commands per batch
        defaults.put("vulkanDescriptorPoolSize", 1024); // Pre-allocated descriptor sets
        defaults.put("vulkanPipelineCacheSize", 512); // Max cached pipelines
        defaults.put("vulkanStagingBufferSizeMB", 64); // Staging buffer size
        defaults.put("vulkanUseDedicatedAllocations", true); // Dedicated allocs for large resources
        defaults.put("vulkanUseMemoryBudget", true); // Track memory budget
        defaults.put("vulkanMaxDeviceMemoryMB", 0); // Max device memory (0 = unlimited)

        // ═══════════════════════════════════════════════════════════════════════════
        // ZERO-OVERHEAD SAFETY SYSTEM (VulkanCallMapper)
        // ═══════════════════════════════════════════════════════════════════════════
        // Controls compile-time safety checks via JIT dead code elimination.
        // When disabled (default), all safety code is completely eliminated by the JIT
        // for TRUE zero overhead - not "low overhead", but literally zero instructions.
        //
        // PRODUCTION MODE (Default - Zero Overhead):
        //   vulkanSafetyChecksEnabled = false
        //   Result: All safety checks eliminated by JIT compiler (0ns overhead)
        //
        // DEVELOPMENT MODE (Low Overhead):
        //   vulkanSafetyChecksEnabled = true
        //   vulkanSafetyFFITracking = false (default)
        //   Result: Java-based tracking (~50-100ns per operation)
        //
        // DEVELOPMENT MODE (Minimal Overhead):
        //   vulkanSafetyChecksEnabled = true
        //   vulkanSafetyFFITracking = true
        //   Result: FFI native tracking (~10-20ns per operation)
        //   Requires: Java 21+ with --enable-preview and --enable-native-access
        // ═══════════════════════════════════════════════════════════════════════════
        
        defaults.put("vulkanSafetyChecksEnabled", false); // Enable comprehensive safety checks (default: false for zero overhead)
        defaults.put("vulkanSafetyFFITracking", false);   // Use FFI native tracking instead of Java (requires Java 21+)
        defaults.put("vulkanSafetyStrictMode", true);     // Throw exceptions on safety violations (vs warnings only)
        
        // Individual Safety System Controls (only active when vulkanSafetyChecksEnabled = true)
        defaults.put("vulkanMemorySourceTracking", true);  // Track memory sources (LWJGL vs FFM/Panama) to prevent cross-API bugs
        defaults.put("vulkanDeadlockDetection", true);     // Monitor lock acquisitions with 5-second timeouts
        defaults.put("vulkanBindlessValidation", true);    // Validate bindless descriptor indices before GPU access
        defaults.put("vulkanLeakDetection", true);         // Report memory leaks on shutdown
        
        // Safety System Thresholds
        defaults.put("vulkanDeadlockTimeoutMs", 5000);     // Lock acquisition timeout in milliseconds
        defaults.put("vulkanDeadlockCheckIntervalMs", 1000); // How often to check for long-held locks
        defaults.put("vulkanFFIHashTableSize", 1048576);   // FFI native hash table size (entries, ~8MB at 1M)

        // Vulkan Extended Feature Flags (aligned with VulkanCallMapper capabilities)
        defaults.put("vulkanEnableExternalMemory", false);       // Import/export GPU memory cross-process (Win32/POSIX fd)
        defaults.put("vulkanEnableExternalMemoryWin32", false);  // Win32 HANDLE memory import/export
        defaults.put("vulkanEnableExternalMemoryFd", false);     // POSIX fd memory import/export

        // Variable Rate Shading
        defaults.put("vulkanEnableVRS", false);                  // Fragment shading rate (KHR)
        defaults.put("vulkanEnableShadingRateImage", false);     // Per-tile VRS image (NV ext, foveated rendering)
        defaults.put("vulkanShadingRateTileSize", 16);           // VRS tile size (8, 16, or 32)

        // Mesh / Task Shaders (EXT_mesh_shader)
        defaults.put("vulkanEnableMeshShaderEXT", false);        // EXT_mesh_shader (cross-vendor, VK 1.3+)
        defaults.put("vulkanEnableTaskShaders", false);          // Task shader stage with mesh pipeline

        // Ray Tracing Extended
        defaults.put("vulkanEnableRayTracing", false);           // KHR ray tracing pipeline
        defaults.put("vulkanEnableRayQuery", false);             // Inline ray query (any stage)
        defaults.put("vulkanEnableAccelerationStructure", false); // BVH acceleration structure
        defaults.put("vulkanEnableRayTracingPositionFetch", false); // VK_KHR_ray_tracing_position_fetch
        defaults.put("vulkanScratchBufferSizeMB", 32);           // BLAS/TLAS scratch buffer size

        // Async Streaming / Transfer Queue
        defaults.put("vulkanEnableAsyncTransfer", true);         // Dedicated transfer queue for async uploads
        defaults.put("vulkanEnableAsyncCompute", true);          // Separate compute queue
        defaults.put("vulkanAsyncStagingRingBufferMB", 64);      // Staging ring buffer for async streaming

        // KTX2 Texture Pipeline
        defaults.put("vulkanEnableKTX2Pipeline", true);          // Runtime KTX2 conversion and upload
        defaults.put("vulkanKTX2CacheDirectory", "cache/ktx2"); // KTX2 texture cache directory
        defaults.put("vulkanKTX2MaxCacheSizeMB", 512);          // Max KTX2 cache size

        // Push Descriptors (zero-alloc binding, VK 1.4 / KHR ext)
        defaults.put("vulkanEnablePushDescriptors", true);       // Push descriptor sets (no heap alloc)

        // Pipeline Library / Fast Compile
        defaults.put("vulkanEnablePipelineLibrary", false);      // VK_KHR_pipeline_library
        defaults.put("vulkanEnableGraphicsPipelineLibrary", false); // VK_EXT_graphics_pipeline_library

        // Compute-Shader Mipmap Generation
        defaults.put("vulkanEnableComputeMipmaps", true);        // Use compute shader for mipmap generation
        defaults.put("minDirectXMajor", 9);
        defaults.put("minDirectXMinor", 0);
        defaults.put("maxDirectXMajor", 12);
        defaults.put("maxDirectXMinor", 2);

        // DirectX Core Settings
        defaults.put("directXEnabled", true); // Enable DirectX support
        defaults.put("directXPreferredVersion", 12); // Preferred version (9, 10, 11, or 12)
        defaults.put("directXAllowFallback", true); // Allow fallback to older versions
        defaults.put("directXPreferDX12", true); // Prefer DX12 over DX11
        defaults.put("directXPreferDX11", true); // Prefer DX11 over DX10/9
        defaults.put("directXMinFeatureLevel", 0x9100); // Minimum feature level (9_1)
        defaults.put("directXMaxFeatureLevel", 0xC200); // Maximum feature level (12_2)
        
        // DirectX Debug/Validation
        defaults.put("directXValidation", false); // Enable D3D debug layer
        defaults.put("directXDebugMode", false); // Enable DirectX debug utilities
        defaults.put("directXGPUValidation", false); // GPU-based validation (slow)
        defaults.put("directXEnableDRED", false); // Device Removed Extended Data
        defaults.put("directXStrictMode", false); // Throw on unsupported features
        
        // DirectX 12 Advanced Features
        defaults.put("directXUseTiledResources", true); // Tiled resources
        defaults.put("directXUseResourceBarriers", true); // Resource state barriers
        defaults.put("directXUseDescriptorHeaps", true); // Descriptor heaps
        defaults.put("directXUseBundledCommands", true); // Bundle command lists
        defaults.put("directXUseRayTracing", false); // DXR ray tracing (DX12)
        defaults.put("directXUseMeshShaders", false); // Mesh shaders (DX12)
        defaults.put("directXUseVariableRateShading", false); // VRS (DX12)
        defaults.put("directXUseSamplerFeedback", false); // Sampler feedback (DX12)
        
        // DirectX Performance Settings
        defaults.put("directXMaxFrameLatency", 3); // Triple buffering
        defaults.put("directXCommandListPoolSize", 64); // Command list pool size
        defaults.put("directXUploadHeapSizeMB", 256); // Upload heap size
        defaults.put("directXDescriptorHeapSize", 1000000); // Descriptor heap entries
        defaults.put("directXUseStablePowerState", false); // Stable clock for profiling
        
        // DirectX Fallback/Compatibility
        defaults.put("directXEnableWARP", false); // Allow software fallback (Windows Advanced Rasterization Platform)
        defaults.put("directXEnableDirectStorage", false); // DirectStorage API
        defaults.put("directXEnableAutoStereo", false); // Auto-stereo for VR
        
        // DirectX 9-specific settings
        defaults.put("directX9UseHardwareVP", true); // Use hardware vertex processing (DX9)
        defaults.put("directX9EnableVSync", false); // Enable VSync (DX9)
        defaults.put("directX9AllowMultithreading", true); // Allow multithreaded rendering (DX9Ex)
        
        // DirectX 10/11 specific settings
        defaults.put("directX11UseDeferred Context", true); // Use deferred contexts (DX11)
        defaults.put("directX11EnableMultithreading", true); // Multi-threaded command submission

        // OpenGL ES Specific Settings
        defaults.put("enableGLES", false); // Enable OpenGL ES support
        defaults.put("allowGLESEmulation", true); // Allow GLES emulation via ANGLE/Zink
        defaults.put("glesCommandPoolSize", 64); // Command buffer pool size
        defaults.put("glesDebugMessages", false); // Enable GLES debug messages
        defaults.put("glesPreferNative", true); // Prefer native GLES over emulation
        defaults.put("glesEnableExtensions", true); // Automatically enable GLES extensions
        defaults.put("glesEnableProgramBinaryCache", true); // Cache compiled programs
        defaults.put("glesEnableShaderCache", true); // Cache shader binaries
        defaults.put("glesCacheDirectory", "cache/gles"); // GLES cache directory
        defaults.put("glesValidateShaders", true); // Validate shaders before compilation
        defaults.put("glesOptimizationLevel", 2); // 0=none, 1=basic, 2=full
        defaults.put("glesAutoTranspileShaders", true); // Auto-transpile GLSL to GLSL ES
        defaults.put("glesMaxTextureSize", 8192); // Maximum texture size
        defaults.put("glesMaxRenderbufferSize", 8192); // Maximum renderbuffer size
        defaults.put("glesMaxUniformBlockSize", 65536); // Maximum UBO size
        defaults.put("glesPreferPersistentMapping", false); // Use persistent buffer mapping (ES 3.0+)
        defaults.put("glesEnableComputeShaders", false); // Enable compute shaders (ES 3.1+)

        // ═══════════════════════════════════════════════════════════════════════════════════
        // HLSL Shader Compilation Settings (DirectX Shader Language)
        // ═══════════════════════════════════════════════════════════════════════════════════
        
        // HLSL Core Settings
        defaults.put("hlslEnabled", true); // Enable HLSL shader compilation
        defaults.put("hlslShaderModel", "6_0"); // Target shader model: 2_0, 3_0, 4_0, 4_1, 5_0, 5_1, 6_0-6_8
        defaults.put("hlslMinShaderModel", "5_0"); //  mum shader model (fallback)
        defaults.put("hlslMaxShaderModel", "6_8"); // Maximum shader model
        defaults.put("hlslPreferDXC", true); // Use DXC compiler (modern) vs D3DCompiler (legacy)
        defaults.put("hlslFallbackToD3DCompiler", true); // Fallback to D3DCompiler if DXC fails
        defaults.put("hlslAutoDetectShaderModel", true); // Auto-detect based on DirectX version
        
        // HLSL Compilation Options
        defaults.put("hlslOptimizationLevel", 3); // 0=none, 1=size, 2=speed, 3=max (default)
        defaults.put("hlslEnableDebugInfo", false); // Include debug symbols in shaders
        defaults.put("hlslDisableOptimizations", false); // Disable optimizations for debugging
        defaults.put("hlslWarningsAsErrors", false); // Treat warnings as errors
        defaults.put("hlslStrictMode", false); // Strict compilation mode
        defaults.put("hlslIEEEStrictness", false); // IEEE floating-point strictness
        defaults.put("hlslPreferFlowControl", false); // Prefer flow control over branching
        defaults.put("hlslAvoidFlowControl", false); // Avoid flow control (flatten branches)
        defaults.put("hlslEnableBackwardsCompatibility", false); // Enable D3D10 compatibility
        defaults.put("hlslPackMatrixRowMajor", false); // Pack matrices row-major (default col-major)
        defaults.put("hlslPackMatrixColumnMajor", true); // Pack matrices column-major
        defaults.put("hlslEnableUnboundedArrays", true); // SM 6.6+ unbounded descriptor arrays
        defaults.put("hlslEnableDynamicResources", true); // Dynamic resource indexing
        
        // HLSL Shader Model Features (auto-enabled based on target SM)
        defaults.put("hlslSM2_Enabled", true); // Shader Model 2.0 (DX9)
        defaults.put("hlslSM3_Enabled", true); // Shader Model 3.0 (DX9)
        defaults.put("hlslSM4_Enabled", true); // Shader Model 4.0 (DX10)
        defaults.put("hlslSM4_1_Enabled", true); // Shader Model 4.1 (DX10.1)
        defaults.put("hlslSM5_Enabled", true); // Shader Model 5.0 (DX11)
        defaults.put("hlslSM5_1_Enabled", true); // Shader Model 5.1 (DX11.3)
        defaults.put("hlslSM6_Enabled", true); // Shader Model 6.0+ (DX12)
        
        // HLSL Root Signature Settings (DX12)
        defaults.put("hlslAutoGenerateRootSignature", true); // Auto-generate from reflection
        defaults.put("hlslRootSignatureVersion", "1_1"); // 1_0 or 1_1
        defaults.put("hlslEnableRootConstants", true); // Use root constants for small data
        defaults.put("hlslEnableDescriptorTables", true); // Use descriptor tables
        defaults.put("hlslEnableStaticSamplers", true); // Use static samplers
        defaults.put("hlslMaxRootParameters", 64); // Max root signature parameters
        defaults.put("hlslRootSignatureFlags", 0); // Custom flags (0 = default)
        
        // HLSL Bytecode Caching
        defaults.put("hlslEnableBytecodeCache", true); // Cache compiled shaders
        defaults.put("hlslBytecodeCacheDir", "cache/hlsl"); // Cache directory
        defaults.put("hlslBytecodeCacheMaxSizeMB", 512); // Max cache size
        defaults.put("hlslEnableDiskCache", true); // Persist cache to disk
        defaults.put("hlslCacheCompressionEnabled", true); // Compress cached bytecode
        defaults.put("hlslValidateCachedShaders", true); // Validate shader hashes
        defaults.put("hlslCacheShaderReflection", true); // Cache reflection data
        defaults.put("hlslIncrementalCompilation", true); // Incremental shader compilation
        
        // HLSL Advanced Features (SM 6.0+)
        defaults.put("hlslEnableWaveIntrinsics", true); // SM 6.0+ wave ops
        defaults.put("hlslEnableFloat16", false); // SM 6.2+ half-precision floats
        defaults.put("hlslEnableInt16", false); // SM 6.2+ 16-bit integers
        defaults.put("hlslEnableInt64", false); // SM 6.0+ 64-bit integers
        defaults.put("hlslEnableViewInstancing", false); // SM 6.1+ view instancing
        defaults.put("hlslEnableBarycentrics", false); // SM 6.1+ barycentric coords
        defaults.put("hlslEnableRaytracingShaders", false); // SM 6.3+ DXR shaders
        defaults.put("hlslEnableMeshShaderSupport", false); // SM 6.5+ mesh shaders
        defaults.put("hlslEnableAmplificationShaders", false); // SM 6.5+ amplification shaders
        defaults.put("hlslEnableWorkGraphs", false); // SM 6.8+ work graphs
        defaults.put("hlslEnableSamplerFeedback", false); // SM 6.5+ sampler feedback
        defaults.put("hlslEnableVariableRateShading", false); // SM 6.4+ VRS
        
        // HLSL Translation Settings (GLSL→HLSL)
        defaults.put("hlslEnableGLSLTranslation", true); // Enable GLSL→HLSL translation
        defaults.put("hlslTranslationCacheEnabled", true); // Cache translated shaders
        defaults.put("hlslPreserveGLSLSemantics", true); // Match GLSL behavior closely
        defaults.put("hlslEmulateGLSLBuiltins", true); // Emulate GLSL built-in functions
        defaults.put("hlslConvertLayoutQualifiers", true); // Convert layout qualifiers
        defaults.put("hlslHandleGLSLExtensions", true); // Handle GLSL extensions
        defaults.put("hlslTranslationQuality", 2); // 0=fast, 1=balanced, 2=accurate
        
        // HLSL SPIR-V Cross-Compilation
        defaults.put("hlslEnableSPIRVBackend", true); // Compile HLSL→SPIR-V
        defaults.put("hlslSPIRVOptimize", true); // Optimize SPIR-V output
        defaults.put("hlslSPIRVValidate", true); // Validate SPIR-V
        
        // HLSL Debugging & Profiling
        defaults.put("hlslEnablePIXMarkers", false); // PIX event markers
        defaults.put("hlslEnableShaderProfiling", false); // Shader profiling info
        defaults.put("hlslGenerateShaderASM", false); // Generate assembly output
        defaults.put("hlslVerboseCompilation", false); // Verbose compiler output
        defaults.put("hlslLogCompilationTimes", false); // Log shader compile times
        defaults.put("hlslPrintShaderErrors", true); // Print compilation errors
        defaults.put("hlslPrintShaderWarnings", true); // Print compilation warnings
        defaults.put("hlslDumpCompiledShaders", false); // Dump compiled bytecode to files

        // ═══════════════════════════════════════════════════════════════════════════════════
        // Metal (MSL) Shader Compilation Settings
        // ═══════════════════════════════════════════════════════════════════════════════════
        
        defaults.put("metalEnabled", true); // Enable Metal API support
        defaults.put("metalLanguageVersion", "3_0"); // Metal language version: 1_0, 1_1, 1_2, 2_0, 2_1, 2_2, 2_3, 2_4, 3_0, 3_1, 3_2
        defaults.put("metalOptimizationLevel", 2); // 0=none, 1=size, 2=default, 3=aggressive
        defaults.put("metalFastMathEnabled", true); // Enable fast math optimizations
        defaults.put("metalPreserveInvarianceEnabled", false); // Preserve invariance for precision
        
        // Metal Feature Support (auto-detected but can be forced)
        defaults.put("metalArgumentBuffersEnabled", true); // Metal 2.0+ argument buffers
        defaults.put("metalTier2ArgumentBuffers", true); // Tier 2 argument buffers
        defaults.put("metalIndirectCommandBuffers", true); // Indirect command buffers
        defaults.put("metalRasterOrderGroups", false); // Metal 2.0+ raster order groups
        defaults.put("metalImageBlocks", false); // Metal 2.0+ imageblock data
        defaults.put("metalTileShaders", false); // Tile shaders (iOS)
        defaults.put("metalRaytracing", false); // Metal 3.0+ raytracing
        defaults.put("metalMeshShaders", false); // Metal 3.0+ mesh/object shaders
        
        // Metal Shader Caching
        defaults.put("metalBinaryCacheEnabled", true); // Cache compiled Metal binaries
        defaults.put("metalBinaryCacheDir", "cache/metal"); // Cache directory
        defaults.put("metalBinaryCacheMaxSizeMB", 512); // Max cache size in MB
        defaults.put("metalCacheCompressionEnabled", true); // Compress cached binaries
        defaults.put("metalValidateCachedShaders", true); // Validate shader hashes
        
        // Metal Memory Management
        defaults.put("metalStorageModeShared", true); // Use shared storage (unified memory on Apple Silicon)
        defaults.put("metalStorageModePrivate", false); // Use private storage (discrete GPUs)
        defaults.put("metalStorageModeManaged", false); // Use managed storage (Intel Macs)
        defaults.put("metalResourceHeapEnabled", true); // Use resource heaps for memory management
        defaults.put("metalMemorylessTextures", false); // Use memoryless textures (iOS tile memory)
        
        // Metal Translation Settings (GLSL→MSL)
        defaults.put("metalGLSLTranslationEnabled", true); // Enable GLSL→MSL translation
        defaults.put("metalTranslationCacheEnabled", true); // Cache translated shaders
        defaults.put("metalPreserveGLSLSemantics", true); // Match GLSL behavior closely
        defaults.put("metalEmulateGLSLBuiltins", true); // Emulate GLSL built-in functions
        defaults.put("metalHandleGLSLExtensions", true); // Handle GLSL extensions
        defaults.put("metalFlipVertexY", true); // Flip Y coordinate for Metal's NDC
        
        // Metal Command Buffer Settings
        defaults.put("metalCommandBufferRetainedReferences", true); // Retain references in command buffers
        defaults.put("metalUnretainedReferences", false); // Use unretained references (advanced)
        defaults.put("metalMaxCommandBuffers", 3); // Max command buffers in flight
        defaults.put("metalCommandBufferErrorOptions", 1); // 0=none, 1=encode, 2=full
        
        // Metal Performance Settings
        defaults.put("metalThreadExecutionWidth", 32); // SIMD width (typically 32 on Apple GPUs)
        defaults.put("metalMaxTotalThreadsPerThreadgroup", 1024); // Max threads per threadgroup
        defaults.put("metalPreferredThreadgroupSizeMultiple", 32); // Preferred threadgroup multiple
        defaults.put("metalUseFenceForSynchronization", true); // Use Metal fences vs encoders
        
        // Metal Debugging & Profiling
        defaults.put("metalAPIValidationEnabled", false); // Enable Metal API validation layer
        defaults.put("metalShaderValidationEnabled", false); // Enable shader validation
        defaults.put("metalGPUFrameCaptureEnabled", false); // Allow GPU frame capture
        defaults.put("metalPIXMarkersEnabled", false); // Enable debug markers
        defaults.put("metalVerboseShaderCompilation", false); // Verbose compiler output
        defaults.put("metalLogCompilationTimes", false); // Log shader compile times

        // ═══════════════════════════════════════════════════════════════════════════════════
        // MSL (Metal Shading Language) Compiler Settings
        // ═══════════════════════════════════════════════════════════════════════════════════
        
        defaults.put("mslEnabled", true); // Enable MSL shader compilation
        defaults.put("mslLanguageVersion", "3_0"); // MSL version: 1_0, 1_1, 1_2, 2_0, 2_1, 2_2, 2_3, 2_4, 3_0, 3_1, 3_2
        defaults.put("mslOptimizationLevel", 2); // 0=none, 1=size, 2=balanced (default), 3=aggressive
        defaults.put("mslFastMathEnabled", true); // Enable fast math optimizations
        defaults.put("mslPreserveInvarianceEnabled", false); // IEEE precision vs performance
        defaults.put("mslEnableDebugInfo", false); // Include debug symbols in compiled libraries
        defaults.put("mslDisableOptimizations", false); // Disable optimizations for debugging
        defaults.put("mslWarningsAsErrors", false); // Treat warnings as compilation errors
        defaults.put("mslStrictMode", false); // Strict MSL compilation mode
        
        // MSL Feature Gates (Version-Dependent)
        defaults.put("mslEnableArgumentBuffers", true); // MSL 2.0+ argument buffers
        defaults.put("mslEnableTier2ArgumentBuffers", true); // Tier 2 argument buffers
        defaults.put("mslEnableIndirectCommandBuffers", true); // ICB support
        defaults.put("mslEnableRasterOrderGroups", false); // MSL 2.0+ raster order groups
        defaults.put("mslEnableImageBlocks", false); // MSL 2.0+ imageblock data (tile memory)
        defaults.put("mslEnableTileShaders", false); // Tile shader support (iOS primarily)
        defaults.put("mslEnableRaytracing", false); // MSL 2.2+ ray tracing support
        defaults.put("mslEnableRaytracingPrimitives", false); // MSL 2.3+ ray query primitives
        defaults.put("mslEnableMeshShaders", false); // MSL 2.4+ mesh/object shaders
        defaults.put("mslEnableFunctionPointers", false); // MSL 2.3+ function pointers
        defaults.put("mslEnableDynamicLibraries", false); // MSL 3.1+ dynamic libraries
        
        // MSL Function Constants
        defaults.put("mslUseFunctionConstants", true); // Enable function constants for specialization
        defaults.put("mslAutoInjectConstants", true); // Auto-inject common constants
        defaults.put("mslMaxFunctionConstants", 256); // Max function constants per shader
        
        // MSL Texture & Sampler Settings
        defaults.put("mslTextureUsageSampling", true); // Default texture usage: sampling
        defaults.put("mslTextureUsageRenderTarget", true); // Allow textures as render targets
        defaults.put("mslTextureUsageShaderWrite", false); // Allow compute shader writes
        defaults.put("mslDefaultSamplerFiltering", "linear"); // "nearest" or "linear"
        defaults.put("mslDefaultSamplerAddressMode", "clamp_to_edge"); // "clamp_to_edge", "repeat", "mirrored_repeat"
        defaults.put("mslMaxAnisotropy", 16); // Max anisotropic filtering (1, 2, 4, 8, 16)
        
        // MSL Bytecode Caching
        defaults.put("mslEnableBytecodeCache", true); // Cache compiled MSL binaries
        defaults.put("mslBytecodeCacheDir", "cache/msl"); // Cache directory
        defaults.put("mslBytecodeCacheMaxSizeMB", 512); // Max cache size
        defaults.put("mslEnableDiskCache", true); // Persist cache to disk
        defaults.put("mslCacheCompressionEnabled", true); // Compress cached bytecode
        defaults.put("mslValidateCachedShaders", true); // Validate shader hashes
        defaults.put("mslBinaryArchiveEnabled", false); // MSL 2.3+ binary archives
        
        // MSL Cross-Compilation (GLSL/HLSL/SPIR-V → MSL)
        defaults.put("mslEnableGLSLTranslation", true); // Enable GLSL→MSL cross-compilation
        defaults.put("mslEnableHLSLTranslation", true); // Enable HLSL→MSL cross-compilation
        defaults.put("mslEnableSPIRVTranslation", true); // Enable SPIR-V→MSL cross-compilation
        defaults.put("mslTranslationCacheEnabled", true); // Cache translated shaders
        defaults.put("mslPreserveSourceSemantics", true); // Match source language behavior closely
        defaults.put("mslEmulateSourceBuiltins", true); // Emulate source built-in functions
        defaults.put("mslConvertLayoutQualifiers", true); // Convert layout qualifiers
        defaults.put("mslHandleSourceExtensions", true); // Handle source language extensions
        defaults.put("mslFlipVertexY", true); // Flip Y coordinate for NDC differences
        defaults.put("mslAdjustClipSpace", true); // Adjust clip space Z (0..1 vs -1..1)
        
        // MSL SPIRV-Cross Options (when translating from SPIR-V)
        defaults.put("mslSPIRVCrossVersion", "auto"); // "auto", "latest", or specific version
        defaults.put("mslSPIRVArgumentBuffers", true); // Use argument buffers for SPIR-V descriptors
        defaults.put("mslSPIRVDiscreteDescriptorSets", false); // Discrete descriptor sets
        defaults.put("mslSPIRVTextureBufferNative", true); // Use native texture buffers
        defaults.put("mslSPIRVMultisampleImageArray", true); // Support multisample image arrays
        defaults.put("mslSPIRVStorageImageArray", true); // Support storage image arrays
        defaults.put("mslSPIRVPlatformIOS", false); // iOS-specific optimizations
        defaults.put("mslSPIRVPlatformMacOS", true); // macOS-specific optimizations
        
        // MSL Shader Variants & Specialization
        defaults.put("mslEnableShaderVariants", true); // Support shader variants
        defaults.put("mslAutoGenerateVariants", false); // Auto-generate common variants
        defaults.put("mslMaxVariantsPerShader", 32); // Max variants to cache per shader
        defaults.put("mslVariantSelectionStrategy", "static"); // "static", "dynamic", "hybrid"
        
        // MSL Buffer Management
        defaults.put("mslDefaultBufferAlignment", 256); // Default buffer alignment (bytes)
        defaults.put("mslArgumentBufferTier", 2); // Argument buffer tier (1 or 2)
        defaults.put("mslMaxBufferBindings", 31); // Max buffer bindings (0-30)
        defaults.put("mslMaxTextureBindings", 128); // Max texture bindings
        defaults.put("mslMaxSamplerBindings", 16); // Max sampler bindings
        defaults.put("mslUseResourceHeaps", true); // Use resource heaps for allocation
        
        // MSL Compute Shader Settings
        defaults.put("mslComputeThreadExecutionWidth", 32); // SIMD width (typically 32)
        defaults.put("mslComputeMaxThreadsPerThreadgroup", 1024); // Max threads per threadgroup
        defaults.put("mslComputeThreadgroupSizeMultiple", 32); // Preferred multiple
        defaults.put("mslComputeMemoryBarrierScope", "threadgroup"); // "device", "threadgroup", "texture"
        
        // MSL Debugging & Profiling
        defaults.put("mslEnablePIXMarkers", false); // Debug markers for Metal frame capture
        defaults.put("mslEnableShaderProfiling", false); // Shader profiling instrumentation
        defaults.put("mslGenerateShaderASM", false); // Generate assembly output
        defaults.put("mslVerboseCompilation", false); // Verbose compiler output
        defaults.put("mslLogCompilationTimes", false); // Log shader compile times
        defaults.put("mslCaptureShaderSource", false); // Capture shader source for debugging
        defaults.put("mslValidationMode", "none"); // "none", "basic", "full"
        
        // MSL Advanced Features
        defaults.put("mslEnableNonUniformResourceIndex", false); // MSL 2.2+ non-uniform indexing
        defaults.put("mslEnableQuadPermutations", true); // Quad-scoped permutations
        defaults.put("mslEnableSIMDGroupOperations", true); // SIMD-group operations
        defaults.put("mslEnableAtomicOperations", true); // Atomic operations support
        defaults.put("mslEnableHalf2/Half4", false); // Half-precision vector types
        defaults.put("mslEnableBFloat16", false); // Brain float 16 support (future)

        // ═══════════════════════════════════════════════════════════════════════════════════
        // GLSL (OpenGL Shading Language) Compiler Settings
        // ═══════════════════════════════════════════════════════════════════════════════════
        
        defaults.put("glslEnabled", true); // Enable GLSL shader compilation
        defaults.put("glslLanguageVersion", "460"); // GLSL version: 110, 120, 130, 140, 150, 330, 400, 410, 420, 430, 440, 450, 460
        defaults.put("glslOptimizationLevel", 2); // 0=none, 1=size, 2=balanced, 3=aggressive
        defaults.put("glslCoreProfile", true); // Use core profile (vs compatibility)
        defaults.put("glslEnableDebugInfo", false); // Include debug symbols
        defaults.put("glslDisableOptimizations", false); // Disable optimizations for debugging
        defaults.put("glslWarningsAsErrors", false); // Treat warnings as compilation errors
        defaults.put("glslStrictMode", false); // Strict GLSL compilation mode
        defaults.put("glslEnablePrecisionQualifiers", true); // Use precision qualifiers (ES compatibility)
        
        // GLSL Version-Specific Features
        defaults.put("glslEnableExplicitLocations", true); // GLSL 330+ explicit attribute/uniform locations
        defaults.put("glslEnableUniformBufferObjects", true); // GLSL 140+ UBOs
        defaults.put("glslEnableShaderStorageBuffers", true); // GLSL 430+ SSBOs
        defaults.put("glslEnableComputeShaders", true); // GLSL 430+ compute shaders
        defaults.put("glslEnableTessellationShaders", false); // GLSL 400+ tessellation
        defaults.put("glslEnableGeometryShaders", false); // GLSL 150+ geometry shaders
        defaults.put("glslEnableAtomicCounters", true); // GLSL 420+ atomic counters
        defaults.put("glslEnableImageLoadStore", true); // GLSL 420+ image load/store
        defaults.put("glslEnableSeparateShaderObjects", false); // GLSL 410+ program pipelines
        defaults.put("glslEnableSubroutines", false); // GLSL 400+ subroutines
        
        // GLSL Extension Support
        defaults.put("glslEnableARBExtensions", true); // ARB extensions
        defaults.put("glslEnableEXTExtensions", true); // EXT extensions
        defaults.put("glslEnableNVExtensions", false); // NVIDIA extensions
        defaults.put("glslEnableAMDExtensions", false); // AMD extensions
        defaults.put("glslEnableINTELExtensions", false); // Intel extensions
        defaults.put("glslEnableKHRExtensions", true); // Khronos extensions
        defaults.put("glslAutoEnableExtensions", true); // Auto-enable required extensions
        
        // GLSL Bytecode & Binary Caching
        defaults.put("glslEnableBinaryCache", true); // Cache compiled shader binaries
        defaults.put("glslBinaryCacheDir", "cache/glsl"); // Cache directory
        defaults.put("glslBinaryCacheMaxSizeMB", 512); // Max cache size in MB
        defaults.put("glslEnableDiskCache", true); // Persist cache to disk
        defaults.put("glslCacheCompressionEnabled", true); // Compress cached binaries
        defaults.put("glslValidateCachedShaders", true); // Validate shader hashes
        defaults.put("glslUseGetProgramBinary", true); // Use glGetProgramBinary if available
        
        // GLSL Preprocessor Settings
        defaults.put("glslEnablePreprocessor", true); // Enable custom preprocessor
        defaults.put("glslPreprocessorDefines", ""); // Comma-separated defines (e.g., "DEBUG,QUALITY_HIGH")
        defaults.put("glslEnableIncludeDirectives", true); // Support #include directives
        defaults.put("glslIncludePaths", "shaders/include"); // Include search paths
        defaults.put("glslMaxIncludeDepth", 32); // Maximum include nesting depth
        defaults.put("glslEnablePragmaDirectives", true); // Support #pragma directives
        defaults.put("glslEnableLineDirectives", true); // Preserve #line for error reporting
        
        // GLSL Cross-Compilation (HLSL/SPIR-V → GLSL)
        defaults.put("glslEnableHLSLTranslation", true); // Enable HLSL→GLSL cross-compilation
        defaults.put("glslEnableSPIRVTranslation", true); // Enable SPIR-V→GLSL cross-compilation
        defaults.put("glslTranslationCacheEnabled", true); // Cache translated shaders
        defaults.put("glslPreserveSourceSemantics", true); // Match source language behavior
        defaults.put("glslEmulateSourceBuiltins", true); // Emulate source built-in functions
        defaults.put("glslConvertSamplerTypes", true); // Convert sampler types correctly
        defaults.put("glslHandleSourceExtensions", true); // Handle source language extensions
        
        // GLSL SPIRV-Cross Options (when translating from SPIR-V)
        defaults.put("glslSPIRVCrossVersion", "auto"); // "auto", "latest", or specific version
        defaults.put("glslSPIRVTargetGLSL", true); // Target GLSL (vs GLSL ES)
        defaults.put("glslSPIRVVulkanSemantics", false); // Use Vulkan semantics
        defaults.put("glslSPIRVFlipVertexY", false); // Flip vertex Y coordinate
        defaults.put("glslSPIRVFixupClipspace", false); // Fix clip space differences
        defaults.put("glslSPIRVSeparateShaderObjects", false); // Use separate shader objects
        
        // GLSL Uniform & Attribute Settings
        defaults.put("glslAutoBindUniforms", true); // Auto-bind uniforms by name
        defaults.put("glslAutoBindAttributes", true); // Auto-bind vertex attributes
        defaults.put("glslMaxUniformLocations", 1024); // Max uniform locations
        defaults.put("glslMaxAttributeLocations", 16); // Max vertex attribute locations
        defaults.put("glslUseUniformBlocks", true); // Prefer uniform blocks over individual uniforms
        defaults.put("glslPackUniformBlocks", true); // Pack uniform blocks efficiently
        
        // GLSL Shader Variants & Specialization
        defaults.put("glslEnableShaderVariants", true); // Support shader variants
        defaults.put("glslAutoGenerateVariants", false); // Auto-generate common variants
        defaults.put("glslMaxVariantsPerShader", 32); // Max variants to cache per shader
        defaults.put("glslVariantSelectionStrategy", "static"); // "static", "dynamic", "uber"
        defaults.put("glslEnableUberShaders", false); // Generate uber-shaders with branches
        
        // GLSL Texture & Sampler Settings
        defaults.put("glslBindlessTexturesEnabled", false); // GLSL 450+ bindless textures
        defaults.put("glslSeparateTexturesSamplers", true); // Separate textures and samplers (modern)
        defaults.put("glslCombinedTextureSamplers", true); // Combined samplers (compatibility)
        defaults.put("glslMaxTextureUnits", 32); // Max texture image units
        defaults.put("glslMaxSamplerBindings", 16); // Max sampler bindings
        defaults.put("glslSamplerNameConvention", "modern"); // "modern" (sampler2D) or "legacy" (texture2D)
        
        // GLSL Buffer & Storage Settings
        defaults.put("glslMaxUniformBlockSize", 65536); // Max UBO size in bytes (64KB)
        defaults.put("glslMaxSSBOSize", 16777216); // Max SSBO size in bytes (16MB)
        defaults.put("glslPreferSSBO", true); // Prefer SSBOs over UBOs when available
        defaults.put("glslBufferAlignment", 256); // Buffer alignment in bytes
        defaults.put("glslStd140Layout", true); // Use std140 layout for UBOs
        defaults.put("glslStd430Layout", true); // Use std430 layout for SSBOs
        
        // GLSL Compute Shader Settings
        defaults.put("glslComputeMaxWorkGroupSize", 1024); // Max local work group size
        defaults.put("glslComputeMaxWorkGroupCountX", 65535); // Max work groups in X
        defaults.put("glslComputeMaxWorkGroupCountY", 65535); // Max work groups in Y
        defaults.put("glslComputeMaxWorkGroupCountZ", 65535); // Max work groups in Z
        defaults.put("glslComputeSharedMemorySize", 32768); // Shared memory size (32KB)
        
        // GLSL Debugging & Profiling
        defaults.put("glslEnableDebugMarkers", false); // Debug markers for profiling
        defaults.put("glslEnableShaderProfiling", false); // Shader profiling instrumentation
        defaults.put("glslGenerateShaderASM", false); // Generate GLSL assembly output
        defaults.put("glslVerboseCompilation", false); // Verbose compiler output
        defaults.put("glslLogCompilationTimes", false); // Log shader compile times
        defaults.put("glslCaptureShaderSource", false); // Capture shader source for debugging
        defaults.put("glslValidationMode", "none"); // "none", "basic", "full"
        defaults.put("glslEnableShaderErrorMessages", true); // Detailed shader error messages
        
        // GLSL Advanced Features
        defaults.put("glslEnableInvariantQualifier", false); // Force invariant on all outputs
        defaults.put("glslEnablePreciseQualifier", false); // Use precise qualifier
        defaults.put("glslEnableEarlyFragmentTests", true); // early_fragment_tests layout qualifier
        defaults.put("glslEnableDepthLayout", true); // depth_* layout qualifiers
        defaults.put("glslEnableOriginControl", true); // origin_upper_left layout qualifier
        defaults.put("glslEnablePixelCenterInteger", false); // pixel_center_integer layout
        defaults.put("glslEnableIntegerShaderOps", true); // Integer bitwise operations
        defaults.put("glslEnableFloat64Support", false); // Double precision floats
        defaults.put("glslEnableInt64Support", false); // 64-bit integers
        defaults.put("glslEnableFloat16Support", false); // Half-precision floats
        defaults.put("glslEnableInt16Support", false); // 16-bit integers
        defaults.put("glslEnableInt8Support", false); // 8-bit integers

        // Feature Toggles
        defaults.put("useVBO", true);
        defaults.put("useVAO", true);
        defaults.put("useInstancing", true);
        defaults.put("useDSA", true);
        defaults.put("usePersistentMapping", true);
        defaults.put("useComputeShaders", false);
        defaults.put("useSPIRV", false);

        // Wrapper-specific
        defaults.put("trustDriverVersion", true);
        defaults.put("enableWrapperQuirks", true);

        // Debug
        defaults.put("enableDebugOutput", false);
        defaults.put("validateOnEveryCall", false);
        
        // ═══════════════════════════════════════════════════════════════════════════════════
        // ECS (Entity Component System) Settings - Kirino-Competitive Edition
        // ═══════════════════════════════════════════════════════════════════════════════════
        
        // ECS Core Settings
        defaults.put("ecsEnabled", true); // Enable ECS system
        defaults.put("ecsThreadCount", Runtime.getRuntime().availableProcessors()); // Number of threads for parallel processing
        defaults.put("ecsUseVirtualThreads", true); // Use virtual threads (Java 21+)
        defaults.put("ecsChunkSize", 256); // Entities per chunk for batch processing
        defaults.put("ecsInitialCapacity", 1024); // Initial entity capacity
        defaults.put("ecsUseOffHeap", true); // Use off-heap memory (Foreign Memory API)
        defaults.put("ecsTrackChanges", true); // Track component changes
        defaults.put("ecsEnableGpu", false); // Enable GPU integration (computed from capabilities)
        defaults.put("ecsBuildEdgeGraph", true); // Build archetype edge graph
        defaults.put("ecsParallelThreshold", 1000); // Minimum entities for parallel processing
        
        // Struct Flattening (Kirino Advantage #1)
        defaults.put("ecsEnableStructFlattening", true); // Enable POJO → Array decomposition
        defaults.put("ecsStructFlatteningSizeHint", 256); // Default size hint for component arrays
        defaults.put("ecsStructFlatteningUseMethodHandles", true); // Use MethodHandles for zero-cost abstraction
        defaults.put("ecsStructFlatteningUseLambdaMetafactory", true); // Use LambdaMetafactory for optimal performance
        defaults.put("ecsStructFlatteningCacheAccessors", true); // Cache generated accessors
        
        // Component Discovery (Kirino Advantage #3)
        defaults.put("ecsAutoScanComponents", true); // Automatically scan for @Component classes
        defaults.put("ecsComponentScanPackages", new String[] { 
            "stellar.snow.astralis.engine.ecs.components",
            "stellar.snow.astralis.engine.ecs.Minecraft"
        }); // Packages to scan
        defaults.put("ecsComponentScanAtStartup", true); // Scan at startup vs on-demand
        defaults.put("ecsComponentScanParallel", true); // Parallel component scanning
        defaults.put("ecsComponentCacheResults", true); // Cache scan results
        
        // Dynamic Archetypes (Kirino Advantage #4)
        defaults.put("ecsEnableDynamicArchetypes", true); // Enable automatic archetype migration
        defaults.put("ecsArchetypePoolSize", 64); // Initial archetype pool size
        defaults.put("ecsArchetypeGrowthFactor", 1.5); // Growth factor when pool fills
        defaults.put("ecsArchetypeMigrationBatchSize", 128); // Entities per migration batch
        defaults.put("ecsArchetypeEdgeOptimization", true); // Cache archetype migration paths
        defaults.put("ecsArchetypeCompactionEnabled", true); // Compact fragmented archetypes
        defaults.put("ecsArchetypeCompactionThreshold", 0.3); // Compact when 30% empty
        
        // System Scheduling (Kirino Advantage #2)
        defaults.put("ecsUseTarjanScheduling", true); // Use Tarjan's algorithm for scheduling
        defaults.put("ecsDetectCycles", true); // Detect circular dependencies
        defaults.put("ecsThrowOnCycles", true); // Throw exception on cycles vs warn
        defaults.put("ecsUseDagOptimization", true); // Use DAG for parallel execution
        defaults.put("ecsMaxSystemDependencyDepth", 32); // Max dependency chain depth
        defaults.put("ecsSystemSchedulingStrategy", "DAG"); // "LINEAR", "DAG", or "WORK_STEALING"
        defaults.put("ecsEnableWorkStealing", true); // Enable work-stealing thread pool
        defaults.put("ecsWorkStealingQueueSize", 256); // Per-thread work queue size
        
        // Type-Safe Injection (Kirino Advantage #5)
        defaults.put("ecsEnableDataInjection", true); // Enable automatic data injection
        defaults.put("ecsInjectAtRegistration", true); // Inject at system registration vs lazy
        defaults.put("ecsValidateInjectionTypes", true); // Validate injection type safety
        defaults.put("ecsInjectReadOnlyArrays", true); // Inject read-only views for safety
        defaults.put("ecsCacheInjectionHandles", true); // Cache method handles
        
        // Performance & Profiling
        defaults.put("ecsEnableProfiler", false); // Enable ECS profiler
        defaults.put("ecsEnableJFR", false); // Enable JFR (Java Flight Recorder) events
        defaults.put("ecsProfilerSamplingInterval", 100); // Profiler sampling interval (ms)
        defaults.put("ecsProfilerRecordHistory", 1000); // Number of samples to keep
        defaults.put("ecsEnableMemoryStats", true); // Track memory usage statistics
        defaults.put("ecsEnableWorkloadEstimation", true); // Enable workload estimator
        defaults.put("ecsWorkloadEstimatorEMA", 0.3); // Exponential moving average alpha
        
        // Compatibility & Safety
        defaults.put("ecsCompatibilityMode", false); // Disable advanced features for compatibility
        defaults.put("ecsVanillaFallback", true); // Fallback to vanilla entity processing
        defaults.put("ecsSafeMode", false); // Extra validation (slower)
        defaults.put("ecsEnableAssertions", false); // Enable runtime assertions
        defaults.put("ecsMaxEntityCount", 1048576); // Maximum entities (1M)
        defaults.put("ecsMaxComponentTypes", 256); // Maximum component types
        defaults.put("ecsMaxSystemCount", 128); // Maximum systems
        
        // Multi-Mod Ecosystem (Kirino Advantage #6)
        defaults.put("ecsEnableModIsolation", true); // Isolate ECS instances per mod
        defaults.put("ecsForgeEventIntegration", true); // Integrate with Forge event bus
        defaults.put("ecsAllowModComponentRegistration", true); // Allow mods to register components
        defaults.put("ecsAllowModSystemRegistration", true); // Allow mods to register systems
        defaults.put("ecsModRegistrationTimeout", 10000); // Mod registration timeout (ms)
        
        // Debug & Development
        defaults.put("ecsDebugMode", false); // Enable debug logging
        defaults.put("ecsDebugPrintArchetypes", false); // Print archetype structure
        defaults.put("ecsDebugPrintDependencyGraph", false); // Print dependency graph
        defaults.put("ecsDebugValidateEveryFrame", false); // Validate ECS state every frame
        defaults.put("ecsDebugTrackAllocations", false); // Track allocations (very slow)
        defaults.put("ecsDebugDumpStatsInterval", 0); // Dump stats interval (0 = disabled)
        
        // Minecraft-Specific ECS Integration
        defaults.put("ecsMinecraftEntityOptimizer", true); // Optimize Minecraft entities
        defaults.put("ecsMinecraftChunkStreamer", true); // Chunk streaming optimization
        defaults.put("ecsMinecraftRedstoneOptimizer", false); // Redstone optimization (experimental)
        defaults.put("ecsMinecraftSpatialOptimizer", true); // Spatial partitioning
        defaults.put("ecsMinecraftBridgeEnabled", true); // Enable ECS bridge
        defaults.put("ecsMinecraftBatchEntityTick", true); // Batch entity ticking
        defaults.put("ecsMinecraftParallelEntityTick", true); // Parallel entity ticking
        defaults.put("ecsMinecraftEntityTickBatchSize", 64); // Entities per tick batch
        
        // Bridge Systems (Circuit Breaker, Interpolation, Sync)
        defaults.put("bridgeCircuitBreakerEnabled", true); // Enable circuit breaker
        defaults.put("bridgeCircuitBreakerFailureThreshold", 5); // Failures before tripping
        defaults.put("bridgeCircuitBreakerResetTimeout", 5000L); // Reset timeout in ms
        defaults.put("bridgeInterpolationEnabled", true); // Enable interpolation
        defaults.put("bridgeInterpolationMode", "LINEAR"); // LINEAR, CUBIC, HERMITE
        defaults.put("bridgeSyncSystemsEnabled", true); // Enable sync systems
        defaults.put("bridgeSyncBatchSize", 128); // Batch size for sync operations
        
        // Culling Manager Settings
        defaults.put("cullingManagerEnabled", true); // Enable culling manager
        defaults.put("cullingTier", "HIGH"); // LOW, MEDIUM, HIGH, ULTRA
        defaults.put("cullingEnableFrustum", true); // Frustum culling
        defaults.put("cullingEnableOcclusion", true); // Occlusion culling
        defaults.put("cullingEnableDistance", true); // Distance culling
        defaults.put("cullingMaxDistance", 256.0); // Max render distance
        defaults.put("cullingOcclusionQueryBatchSize", 64); // Queries per batch
        defaults.put("cullingUseHiZ", true); // Use hierarchical Z-buffer
        
        // Indirect Draw Manager Settings
        defaults.put("indirectDrawEnabled", true); // Enable indirect drawing
        defaults.put("indirectDrawMaxDrawCalls", 10000); // Max draw calls per frame
        defaults.put("indirectDrawBatchingStrategy", "MATERIAL"); // MATERIAL, MESH, DISTANCE
        defaults.put("indirectDrawEnableMultiDraw", true); // Use multi-draw indirect
        defaults.put("indirectDrawCommandBufferSizeMB", 4); // Command buffer size
        
        // Draw Pool Settings
        defaults.put("drawPoolEnabled", true); // Enable draw pool
        defaults.put("drawPoolMaxDrawCalls", 10000); // Max pooled draw calls
        defaults.put("drawPoolMaxClusters", 256); // Max draw clusters
        defaults.put("drawPoolClusteringStrategy", "SPATIAL"); // SPATIAL, MATERIAL, HYBRID
        
        // ═══════════════════════════════════════════════════════════════════════════════════
        // RENDER ENGINE SETTINGS
        // ═══════════════════════════════════════════════════════════════════════════════════
        
        // Render Graph Settings
        defaults.put("renderGraphEnabled", true);
        defaults.put("renderGraphMaxFramesInFlight", 3);
        defaults.put("renderGraphCommandBufferPoolSize", 16);
        defaults.put("renderGraphEnablePassCulling", true);
        defaults.put("renderGraphEnablePassMerging", true);
        defaults.put("renderGraphEnableMemoryAliasing", true);
        defaults.put("renderGraphEnableParallelRecording", true);
        defaults.put("renderGraphEnableSplitBarriers", true);
        defaults.put("renderGraphEnableGPUProfiling", false);
        defaults.put("renderGraphParallelRecordingThreshold", 4);
        defaults.put("renderGraphAutoCompile", true);
        defaults.put("renderGraphValidateEveryFrame", false);
        
        // Render System Settings
        defaults.put("renderSystemEnabled", true);
        defaults.put("renderSystemUseECS", true);
        defaults.put("renderSystemBatchSize", 256);
        defaults.put("renderSystemEnableInstancing", true);
        defaults.put("renderSystemEnableIndirectDraw", true);
        defaults.put("renderSystemEnableGPUCulling", false);
        defaults.put("renderSystemEnableOcclusionCulling", true);
        defaults.put("renderSystemEnableFrustumCulling", true);
        defaults.put("renderSystemCullingBatchSize", 1024);
        defaults.put("renderSystemMaxDrawCalls", 10000);
        defaults.put("renderSystemEnableSortByMaterial", true);
        defaults.put("renderSystemEnableSortByDepth", true);
        defaults.put("renderSystemEnableStateCache", true);
        defaults.put("renderSystemStateCacheSize", 512);
        
        // Render Pipeline Settings
        defaults.put("renderPipelineEnableVBO", true);
        defaults.put("renderPipelineEnableVAO", true);
        defaults.put("renderPipelineEnableUBO", true);
        defaults.put("renderPipelineEnableSSBO", false);
        defaults.put("renderPipelineEnableDSA", false);
        defaults.put("renderPipelineEnablePersistentMapping", false);
        defaults.put("renderPipelineBufferSizeMB", 256);
        defaults.put("renderPipelineUniformBufferSize", 65536);
        defaults.put("renderPipelineVertexBufferSize", 16777216); // 16MB
        defaults.put("renderPipelineIndexBufferSize", 4194304); // 4MB
        
        // Render State Settings
        defaults.put("renderStateEnableDepthTest", true);
        defaults.put("renderStateEnableBlending", true);
        defaults.put("renderStateEnableCulling", true);
        defaults.put("renderStateDepthFunc", "LESS");
        defaults.put("renderStateBlendSrc", "SRC_ALPHA");
        defaults.put("renderStateBlendDst", "ONE_MINUS_SRC_ALPHA");
        defaults.put("renderStateCullFace", "BACK");
        defaults.put("renderStateFrontFace", "CCW");
        defaults.put("renderStateEnableDepthWrite", true);
        defaults.put("renderStateEnableColorWrite", true);
        defaults.put("renderStateEnableStencilTest", false);
        
        // Resolution Manager Settings
        defaults.put("resolutionManagerEnabled", true);
        defaults.put("resolutionManagerMode", "ADAPTIVE"); // STATIC, ADAPTIVE, PERFORMANCE
        defaults.put("resolutionManagerDynamicScaling", true);
        defaults.put("resolutionManagerMinScale", 0.5);
        defaults.put("resolutionManagerMaxScale", 1.0);
        defaults.put("resolutionManagerTargetFrameTime", 16.66); // 60 FPS
        defaults.put("resolutionManagerScaleStep", 0.05);
        defaults.put("resolutionManagerAdaptiveInterval", 10); // Frames
        defaults.put("resolutionManagerEnableTAA", false);
        defaults.put("resolutionManagerEnableFSR", false);
        defaults.put("resolutionManagerEnableDLSS", false);
        
        // Render Bridge Settings
        defaults.put("renderBridgeEnabled", true);
        defaults.put("renderBridgeMinecraftCompatibility", true);
        defaults.put("renderBridgeEnableChunkRendering", true);
        defaults.put("renderBridgeEnableEntityRendering", true);
        defaults.put("renderBridgeEnableTileEntityRendering", true);
        defaults.put("renderBridgeEnableParticleRendering", true);
        defaults.put("renderBridgeChunkBatchSize", 64);
        defaults.put("renderBridgeEntityBatchSize", 256);
        
        // Advanced Rendering Features
        defaults.put("renderEnableHDR", false);
        defaults.put("renderEnableBloom", false);
        defaults.put("renderEnableSSAO", false);
        defaults.put("renderEnableSSR", false);
        defaults.put("renderEnableShadows", true);
        defaults.put("renderShadowMapSize", 2048);
        defaults.put("renderShadowCascades", 3);
        defaults.put("renderEnableCSM", true); // Cascaded Shadow Maps
        defaults.put("renderEnablePBR", false); // Physically Based Rendering
        defaults.put("renderEnableIBL", false); // Image Based Lighting
        
        // Performance & Profiling
        defaults.put("renderEnableProfiler", false);
        defaults.put("renderEnableGPUTimestamps", false);
        defaults.put("renderEnableDrawCallCounter", true);
        defaults.put("renderEnableStateChangeCounter", true);
        defaults.put("renderPrintStatsInterval", 0); // 0 = disabled
        defaults.put("renderMaxFrameTime", 33.33); // 30 FPS fallback
        
        // ═══════════════════════════════════════════════════════════════════════════════════
        // GPU BACKEND INTEGRATION SETTINGS
        // ═══════════════════════════════════════════════════════════════════════════════════
        
        // Backend Selection
        defaults.put("gpuBackendPreferred", "AUTO"); // AUTO, VULKAN, METAL, DIRECTX, OPENGL, OPENGLES, NULL
        defaults.put("gpuBackendAllowFallback", true);
        defaults.put("gpuBackendFallbackChain", new String[] {
            "VULKAN", "DIRECTX", "METAL", "OPENGL", "OPENGLES", "NULL"
        });
        defaults.put("gpuBackendRequireMinimumVersion", false);
        defaults.put("gpuBackendValidateCapabilities", true);
        
        // Platform-Specific Preferences
        defaults.put("gpuBackendWindowsPreferDX", true);
        defaults.put("gpuBackendMacOSPreferMetal", true);
        defaults.put("gpuBackendLinuxPreferVulkan", true);
        defaults.put("gpuBackendAndroidPreferVulkan", false); // Prefer GLES on Android
        
        // Feature Requirements
        defaults.put("gpuBackendRequireComputeShaders", false);
        defaults.put("gpuBackendRequireGeometryShaders", false);
        defaults.put("gpuBackendRequireTessellation", false);
        defaults.put("gpuBackendRequireMeshShaders", false);
        defaults.put("gpuBackendRequireRayTracing", false);
        defaults.put("gpuBackendRequireBindlessTextures", false);
        defaults.put("gpuBackendRequireMultiDrawIndirect", false);
        
        // Capability Overrides (for testing/debugging)
        defaults.put("gpuBackendOverrideVersion", false);
        defaults.put("gpuBackendForcedVersionMajor", 0);
        defaults.put("gpuBackendForcedVersionMinor", 0);
        defaults.put("gpuBackendDisableExtensions", false);
        defaults.put("gpuBackendDisabledExtensionList", new String[0]);
        
        // Backend-Specific Settings
        defaults.put("gpuBackendVulkanValidation", false);
        defaults.put("gpuBackendVulkanDebugUtils", false);
        defaults.put("gpuBackendMetalValidation", false);
        defaults.put("gpuBackendDirectXDebugLayer", false);
        defaults.put("gpuBackendOpenGLDebugContext", false);
        
        // Hot-Reload & Development
        defaults.put("gpuBackendEnableHotReload", false);
        defaults.put("gpuBackendAutoDetectChanges", false);
        defaults.put("gpuBackendReloadOnDriverUpdate", false);
        
        // Diagnostics
        defaults.put("gpuBackendEnableProfiling", false);
        defaults.put("gpuBackendLogSelectionProcess", true);
        defaults.put("gpuBackendLogCapabilities", true);
        defaults.put("gpuBackendDumpFullReport", false);

        // ─── DeepMix Integration ─────────────────────────────────────────────────────
        // Controls how Astralis interacts with the DeepMix mixin loading framework.
        // These are safe to leave at defaults unless you're debugging mixin conflicts.
        defaults.put("deepmixEnabled",                true);   // Master switch for DeepMix integration
        defaults.put("deepmixLogQueuedConfigs",       true);   // Log each config DeepMix queues
        defaults.put("deepmixLogHijacks",             true);   // Log if IMixinConfigHijacker fires
        defaults.put("deepmixUseEarlyLoader",         true);   // Register via IEarlyMixinLoader (required for GL hooks)
        defaults.put("deepmixUseLateLoader",          false);  // Register via ILateMixinLoader (optional compat mixins)
        defaults.put("deepmixAutoResolveConflicts",   true);   // Auto-resolve loader priority conflicts
        defaults.put("deepmixPriority",               1000);   // Our loader priority (shader-pack tier)
        defaults.put("deepmixAllowSpongeForgePatch",  true);   // Apply SpongeForgeFixer if SpongeForge is present
        defaults.put("deepmixAllowMixinExtrasFix",    true);   // Apply MixinExtrasFixer for ASM 5.0.x compat
        defaults.put("deepmixAllowAncientModPatch",   true);   // Patch mods that load mixins incorrectly
        defaults.put("deepmixLateConfigs",            new String[0]); // Extra configs for late loader (empty = none)

        // ─── Mini_DirtyRoom Integration ───────────────────────────────────────────────
        // Controls Astralis interaction with the Mini_DirtyRoom modernization layer.
        // MDR handles: LWJGL 3.4.0 override, Java 25 relaunch, Android compat, native extraction.
        defaults.put("mdrEnabled",                   true);    // Master switch: load MDR bootstrap
        defaults.put("mdrLogBootstrap",              true);    // Log MDR phase-by-phase boot output
        defaults.put("mdrAutoUpgradeJava",           true);    // Let MDR relaunch on Java 25 if needed
        defaults.put("mdrDownloadJRE",               true);    // Allow MDR to download JRE 25 if missing
        defaults.put("mdrDownloadLWJGL",             true);    // Allow MDR to download LWJGL 3.4.0 jars
        defaults.put("mdrVerifyChecksums",           true);    // SHA-256 verify downloaded artifacts
        defaults.put("mdrOverrideFilesOnDisk",       false);   // Nuclear option: overwrite launcher LWJGL jars
        defaults.put("mdrAndroidMode",               false);   // Force Android code paths even on desktop
        defaults.put("mdrDownloadTimeoutSecs",       60);      // Per-download HTTP timeout in seconds
        defaults.put("mdrNativeLoadRetries",         3);       // Retries for native library loading
        defaults.put("mdrBootstrapTimeoutMs",        10000);   // How long AstralisCore waits for MDR ready
        defaults.put("mdrLwjglMirrorUrl",            "");      // Custom LWJGL download mirror (blank = Maven Central)
        defaults.put("mdrJreMirrorUrl",              "");      // Custom JRE download mirror (blank = Adoptium)
        
        // ═══════════════════════════════════════════════════════════════════════════════════
        // Integration Modules - Performance Optimizations
        // ═══════════════════════════════════════════════════════════════════════════════════
        
        // PhotonEngine - Light-speed rendering optimizations (Java 25 FFM)
        defaults.put("photonEngineEnabled",              false);   // Master toggle for PhotonEngine
        defaults.put("photonEnhancedBatching",           true);    // Enhanced vertex batching
        defaults.put("photonFontAtlasResize",            true);    // Dynamic font atlas resizing
        defaults.put("photonMapAtlasGeneration",         true);    // Map atlas generation
        defaults.put("photonTextTranslucencySkip",       true);    // Skip text translucency checks
        defaults.put("photonFastTextLookup",             true);    // Fast text rendering lookup
        defaults.put("photonFramebufferOptimization",    true);    // Framebuffer switch elimination
        defaults.put("photonZeroCopyUpload",             true);    // Zero-copy buffer uploads (FFM)
        defaults.put("photonParallelBatching",           true);    // Parallel batch processing
        defaults.put("photonFixAppleGPUUpload",          true);    // Fix for Apple GPU uploads
        
        // ShortStack - Parallel recipe matching engine (Java 25)
        defaults.put("shortStackEnabled",                false);   // Master toggle for ShortStack
        defaults.put("shortStackParallelRecipes",        true);    // Parallel recipe matching
        defaults.put("shortStackRecipeCache",            true);    // Recipe result caching
        defaults.put("shortStackVirtualThreads",         true);    // Use virtual threads (Java 21+)
        defaults.put("shortStackOptimizationLevel",      2);       // 0=off, 1=basic, 2=aggressive
        
        // Neon - Advanced lighting optimizations (Java 25)
        defaults.put("neonEnabled",                      false);   // Master toggle for Neon
        defaults.put("neonNativeLightEngine",            true);    // Native light calculation engine
        defaults.put("neonSpatialIndexing",              true);    // Spatial light indexing
        defaults.put("neonTemporalSmoothing",            true);    // Temporal light smoothing
        defaults.put("neonAsyncLightUpdates",            true);    // Asynchronous light updates
        defaults.put("neonLumenBridge",                  true);    // Bridge to Lumen system
        
        // AllTheLeaksReborn - Memory leak detection & fixes
        defaults.put("allTheLeaksEnabled",               false);   // Master toggle for AllTheLeaks
        defaults.put("leaksMemoryMonitoring",            true);    // Monitor memory usage
        defaults.put("leaksLeakTracking",                true);    // Track potential leaks
        defaults.put("leaksAutoCleanup",                 true);    // Automatic cleanup
        defaults.put("leaksReportingLevel",              1);       // 0=off, 1=summary, 2=detailed
        
        // BlueCore - Core optimizations (Java 25)
        defaults.put("blueCoreEnabled",                  false);   // Master toggle for BlueCore
        defaults.put("blueCoreHighPerfLogging",          true);    // High-performance logging system
        defaults.put("blueCoreThreadOptimizations",      true);    // Thread-local optimizations
        defaults.put("blueCoreAllocationMinimization",   true);    // Minimize allocations
        
        // Bolt - Thread & performance enhancements (Java 25)
        defaults.put("boltEnabled",                      false);   // Master toggle for Bolt
        defaults.put("boltThreadOptimizations",          true);    // Thread pool optimizations
        defaults.put("boltVirtualThreads",               true);    // Use virtual threads (Java 21+)
        defaults.put("boltTaskScheduling",               true);    // Advanced task scheduling
        defaults.put("boltConcurrencyLevel",             2);       // 0=single, 1=moderate, 2=aggressive
        
        // ChunkMotion - Chunk animation system
        defaults.put("chunkMotionEnabled",               false);   // Master toggle for ChunkMotion
        defaults.put("chunkAnimationMode",               "hybrid");// Animation mode: below, above, hybrid, horizontal, fade
        defaults.put("chunkEasingFunction",              "cubic"); // Easing: linear, quad, cubic, quart, quint, sine, expo, circ, elastic, bounce
        defaults.put("chunkAnimationDuration",           1000);    // Animation duration in milliseconds
        defaults.put("chunkRenderOptimization",          true);    // Optimize chunk rendering during animation
        
        // GoodOptimizations - General optimizations
        defaults.put("goodOptEnabled",                   false);   // Master toggle for GoodOptimizations
        defaults.put("goodOptLightmapCache",             true);    // Lightmap caching
        defaults.put("goodOptRenderOptimizations",       true);    // General render optimizations
        defaults.put("goodOptMemoryOptimizations",       true);    // Memory usage optimizations
        
        // Haku - EXPERIMENTAL Valkyrie rewrite (NOT RECOMMENDED - EXTREMELY EXPERIMENTAL)
        // WARNING: Haku is an experimental rewrite of the Valkyrie mod and is NOT production-ready.
        // This module is highly unstable and may cause crashes, world corruption, or other issues.
        // Enable at your own risk - not worth using in most cases.
        defaults.put("hakuEnabled",                      false);   // Master toggle (KEEP DISABLED)
        defaults.put("hakuExperimentalFeatures",         false);   // Enable experimental features (DANGEROUS)
        defaults.put("hakuDebugMode",                    false);   // Debug mode for troubleshooting
        defaults.put("hakuWarningAcknowledged",          false);   // Must acknowledge risks to enable
        
        // Lavender - OptiFine compatibility layer (NOT RECOMMENDED - LEGAL CONCERNS)
        // WARNING: Lavender attempts OptiFine compatibility but faces significant limitations.
        // OptiFine is closed-source, and legally we cannot reverse-engineer its internals
        // without violating its license. Current features are based on observable behavior
        // and publicly documented APIs only. Many OptiFine features CANNOT be replicated.
        // Expect limited functionality and potential compatibility issues.
        defaults.put("lavenderEnabled",                  false);   // Master toggle (NOT RECOMMENDED)
        defaults.put("lavenderVisualizationMode",        true);    // Safe visualization-only features
        defaults.put("lavenderShaderEmulation",          false);   // EXPERIMENTAL shader emulation (limited)
        defaults.put("lavenderTextureOptimizations",     true);    // Safe texture optimizations
        defaults.put("lavenderCompatWarnings",           true);    // Show compatibility warnings
        defaults.put("lavenderLegalNoticeShown",         false);   // Legal notice acknowledgment
        
        // Lumen - Lighting engine optimizations
        defaults.put("lumenEnabled",                     false);   // Master toggle for Lumen
        defaults.put("lumenLightingEngine",              true);    // Advanced lighting engine
        defaults.put("lumenBlockLightOptimizations",     true);    // Block light optimizations
        defaults.put("lumenSkyLightOptimizations",       true);    // Sky light optimizations
        defaults.put("lumenAsyncLightUpdates",           true);    // Asynchronous light updates
        defaults.put("lumenCacheLightData",              true);    // Cache light calculations
        
        // MagnetismCore - Physics & memory optimizations (Java 25 FFM)
        defaults.put("magnetismCoreEnabled",             false);   // Master toggle for MagnetismCore
        defaults.put("magnetismNativeMemory",            true);    // Native memory management (FFM)
        defaults.put("magnetismPhysicsOptimizations",    true);    // Physics calculations optimization
        defaults.put("magnetismMemoryPooling",           true);    // Memory pooling system
        defaults.put("magnetismZeroCopyOperations",      true);    // Zero-copy operations (FFM)
        
        // Asto - Modern VintageFix rewrite (Java 25 + LWJGL 3.3.6)
        defaults.put("astoEnabled",                      false);   // Master toggle for Asto
        defaults.put("astoDynamicResourceLoading",       true);    // Parallel texture/model loading
        defaults.put("astoLazyAtlasStitching",           true);    // Lazy texture atlas stitching
        defaults.put("astoChunkOptimizations",           true);    // Chunk management optimizations
        defaults.put("astoBytecodeTransformCaching",     true);    // Transformer pipeline caching
        defaults.put("astoMemoryOptimizations",          true);    // Texture deduplication & memory opts
        defaults.put("astoConcurrentModelBaking",        true);    // Concurrent model baking
        defaults.put("astoJarDiscoveryCaching",          true);    // JAR discovery caching
        defaults.put("astoFastCollections",              true);    // Lock-free concurrent collections
        
        // Fluorine - Additional optimizations
        defaults.put("fluorineEnabled",                  false);   // Master toggle for Fluorine
        defaults.put("fluorineRenderOptimizations",      true);    // Render pipeline optimizations
        defaults.put("fluorineMemoryOptimizations",      true);    // Memory usage optimizations
        defaults.put("fluorineThreadOptimizations",      true);    // Threading optimizations
        
        // LegacyFix - Legacy compatibility fixes
        defaults.put("legacyFixEnabled",                 false);   // Master toggle for LegacyFix
        defaults.put("legacyCompatibilityMode",          true);    // Enable compatibility mode
        defaults.put("legacyFixCrashPrevention",         true);    // Crash prevention patches
        defaults.put("legacyFixMemoryLeaks",             true);    // Fix known memory leaks
        
        // SnowyASM - Advanced memory & performance optimization
        defaults.put("snowyASMEnabled",                  false);   // Master toggle for SnowyASM
        defaults.put("snowyStringDeduplication",         true);    // String deduplication (<50ns)
        defaults.put("snowyZeroAllocationPaths",         true);    // Zero-allocation rendering paths
        defaults.put("snowyLockFreeConcurrency",         true);    // Lock-free concurrent structures
        defaults.put("snowyMemoryReduction",             true);    // 30-50% memory reduction
        
        // JIT Optimization System
        defaults.put("jitEnabled",                       false);   // Master toggle for JIT optimizations
        defaults.put("jitHelperEnabled",                 true);    // JIT helper utilities
        defaults.put("jitInjectEnabled",                 true);    // JIT injection system
        defaults.put("jitUniversalPatcherEnabled",       true);    // Universal bytecode patcher
        defaults.put("jitAggressiveOptimization",        false);   // Aggressive JIT optimization (may be unstable)
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 5: INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Private constructor - utility class
     */
    private Config() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Initialize configuration system
     */
    public static void initialize() {
        if (initialized.getAndSet(true)) return;

        try {
            // Setup config path
            Path configDir = Paths.get(CONFIG_DIRECTORY);
            Files.createDirectories(configDir);
            configPath = configDir.resolve(CONFIG_FILE_NAME);

            // Load defaults
            values.putAll(defaults);

            // Load from file if exists
            if (Files.exists(configPath)) {
                load();
            } else {
                save();
            }

            // Validate against capabilities
            validateAgainstCapabilities();

        } catch (IOException e) {
            System.err.println("[Astralis Config] Initialization failed: " + e.getMessage());
            values.putAll(defaults);
        }
    }

    /**
     * Load configuration from file
     */
    public static void load() {
        if (!initialized.get()) initialize();

        try (BufferedReader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

                int eqIndex = line.indexOf('=');
                if (eqIndex <= 0) continue;

                String key = line.substring(0, eqIndex).trim();
                String valueStr = line.substring(eqIndex + 1).trim();

                if (defaults.containsKey(key)) {
                    Object defaultValue = defaults.get(key);
                    Object parsed = parseValue(valueStr, defaultValue.getClass());
                    if (parsed != null) {
                        values.put(key, parsed);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Astralis Config] Load failed: " + e.getMessage());
        }
    }

    /**
     * Save configuration to file
     */
    public static void save() {
        if (!initialized.get()) initialize();

        try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            
            writer.write("# Astralis Configuration\n");
            writer.write("# Version: " + CONFIG_VERSION + "\n");
            writer.write("# Generated: " + java.time.Instant.now() + "\n");
            writer.write("\n");

            // Group by category
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# API Settings\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "preferredAPI");
            writeValue(writer, "shaderEngine");
            writeValue(writer, "rendererOverride");
            writer.write("\n");

            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# Performance\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "performanceProfile");
            writeValue(writer, "teleportThresholdSquared");
            writeValue(writer, "maxFrameTime");
            writeValue(writer, "fixedTimestep");
            writeValue(writer, "aiThrottleMask");
            writeValue(writer, "batchSize");
            writer.write("\n");

            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# State Management\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "enableStateCache");
            writeValue(writer, "stateValidationInterval");
            writeValue(writer, "syncStateOnExternalChange");
            writeValue(writer, "enableExceptionSafety");
            writeValue(writer, "maxStateStackDepth");
            writer.write("\n");

            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# Logging\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "loggingLevel");
            writeValue(writer, "logBufferSize");
            writeValue(writer, "logFlushIntervalMs");
            writeValue(writer, "logCallDetails");
            writeValue(writer, "logFallbacks");
            writer.write("\n");

            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# Compatibility\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "allowFallback");
            writeValue(writer, "maxFallbackAttempts");
            writeValue(writer, "skipStateManagerOverwrite");
            writeValue(writer, "deferStateChanges");
            writer.write("\n");

            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# Feature Toggles\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "useVBO");
            writeValue(writer, "useVAO");
            writeValue(writer, "useInstancing");
            writeValue(writer, "useDSA");
            writeValue(writer, "usePersistentMapping");
            writeValue(writer, "useComputeShaders");
            writeValue(writer, "useSPIRV");
            writer.write("\n");

            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# Debug\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "enableDebugOutput");
            writeValue(writer, "validateOnEveryCall");
            writeValue(writer, "trustDriverVersion");
            writeValue(writer, "enableWrapperQuirks");

            writer.write("\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# DeepMix Integration\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "deepmixEnabled");
            writeValue(writer, "deepmixLogQueuedConfigs");
            writeValue(writer, "deepmixLogHijacks");
            writeValue(writer, "deepmixUseEarlyLoader");
            writeValue(writer, "deepmixUseLateLoader");
            writeValue(writer, "deepmixAutoResolveConflicts");
            writeValue(writer, "deepmixPriority");
            writeValue(writer, "deepmixAllowSpongeForgePatch");
            writeValue(writer, "deepmixAllowMixinExtrasFix");
            writeValue(writer, "deepmixAllowAncientModPatch");

            writer.write("\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# Mini_DirtyRoom Integration\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writeValue(writer, "mdrEnabled");
            writeValue(writer, "mdrLogBootstrap");
            writeValue(writer, "mdrAutoUpgradeJava");
            writeValue(writer, "mdrDownloadJRE");
            writeValue(writer, "mdrDownloadLWJGL");
            writeValue(writer, "mdrVerifyChecksums");
            writeValue(writer, "mdrOverrideFilesOnDisk");
            writeValue(writer, "mdrAndroidMode");
            writeValue(writer, "mdrDownloadTimeoutSecs");
            writeValue(writer, "mdrNativeLoadRetries");
            writeValue(writer, "mdrBootstrapTimeoutMs");
            writeValue(writer, "mdrLwjglMirrorUrl");
            writeValue(writer, "mdrJreMirrorUrl");
            
            writer.write("\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("# Integration Modules - Performance Optimizations\n");
            writer.write("# All modules are DISABLED by default for maximum compatibility\n");
            writer.write("# ═══════════════════════════════════════════════════════════════\n");
            writer.write("\n");
            
            writer.write("# PhotonEngine - Light-speed rendering optimizations (Java 25 FFM)\n");
            writeValue(writer, "photonEngineEnabled");
            writeValue(writer, "photonEnhancedBatching");
            writeValue(writer, "photonFontAtlasResize");
            writeValue(writer, "photonMapAtlasGeneration");
            writeValue(writer, "photonTextTranslucencySkip");
            writeValue(writer, "photonFastTextLookup");
            writeValue(writer, "photonFramebufferOptimization");
            writeValue(writer, "photonZeroCopyUpload");
            writeValue(writer, "photonParallelBatching");
            writeValue(writer, "photonFixAppleGPUUpload");
            writer.write("\n");
            
            writer.write("# ShortStack - Parallel recipe matching engine (Java 25)\n");
            writeValue(writer, "shortStackEnabled");
            writeValue(writer, "shortStackParallelRecipes");
            writeValue(writer, "shortStackRecipeCache");
            writeValue(writer, "shortStackVirtualThreads");
            writeValue(writer, "shortStackOptimizationLevel");
            writer.write("\n");
            
            writer.write("# Neon - Advanced lighting optimizations (Java 25)\n");
            writeValue(writer, "neonEnabled");
            writeValue(writer, "neonNativeLightEngine");
            writeValue(writer, "neonSpatialIndexing");
            writeValue(writer, "neonTemporalSmoothing");
            writeValue(writer, "neonAsyncLightUpdates");
            writeValue(writer, "neonLumenBridge");
            writer.write("\n");
            
            writer.write("# AllTheLeaksReborn - Memory leak detection & fixes\n");
            writeValue(writer, "allTheLeaksEnabled");
            writeValue(writer, "leaksMemoryMonitoring");
            writeValue(writer, "leaksLeakTracking");
            writeValue(writer, "leaksAutoCleanup");
            writeValue(writer, "leaksReportingLevel");
            writer.write("\n");
            
            writer.write("# BlueCore - Core optimizations (Java 25)\n");
            writeValue(writer, "blueCoreEnabled");
            writeValue(writer, "blueCoreHighPerfLogging");
            writeValue(writer, "blueCoreThreadOptimizations");
            writeValue(writer, "blueCoreAllocationMinimization");
            writer.write("\n");
            
            writer.write("# Bolt - Thread & performance enhancements (Java 25)\n");
            writeValue(writer, "boltEnabled");
            writeValue(writer, "boltThreadOptimizations");
            writeValue(writer, "boltVirtualThreads");
            writeValue(writer, "boltTaskScheduling");
            writeValue(writer, "boltConcurrencyLevel");
            writer.write("\n");
            
            writer.write("# ChunkMotion - Chunk animation system\n");
            writeValue(writer, "chunkMotionEnabled");
            writeValue(writer, "chunkAnimationMode");
            writeValue(writer, "chunkEasingFunction");
            writeValue(writer, "chunkAnimationDuration");
            writeValue(writer, "chunkRenderOptimization");
            writer.write("\n");
            
            writer.write("# GoodOptimizations - General optimizations\n");
            writeValue(writer, "goodOptEnabled");
            writeValue(writer, "goodOptLightmapCache");
            writeValue(writer, "goodOptRenderOptimizations");
            writeValue(writer, "goodOptMemoryOptimizations");
            writer.write("\n");
            
            writer.write("# ─────────────────────────────────────────────────────────────────\n");
            writer.write("# WARNING: Haku - EXTREMELY EXPERIMENTAL (NOT RECOMMENDED)\n");
            writer.write("# Haku is an experimental rewrite of Valkyrie - highly unstable!\n");
            writer.write("# May cause crashes, world corruption, or other serious issues.\n");
            writer.write("# Enable at your own risk - not worth using in most cases.\n");
            writer.write("# ─────────────────────────────────────────────────────────────────\n");
            writeValue(writer, "hakuEnabled");
            writeValue(writer, "hakuExperimentalFeatures");
            writeValue(writer, "hakuDebugMode");
            writeValue(writer, "hakuWarningAcknowledged");
            writer.write("\n");
            
            writer.write("# ─────────────────────────────────────────────────────────────────\n");
            writer.write("# WARNING: Lavender - OptiFine Compatibility (NOT RECOMMENDED)\n");
            writer.write("# Lavender attempts OptiFine compatibility but faces limitations.\n");
            writer.write("# OptiFine is closed-source - legally we cannot reverse-engineer\n");
            writer.write("# its internals without violating its license. Current features\n");
            writer.write("# are based on observable behavior and public APIs only.\n");
            writer.write("# Many OptiFine features CANNOT be replicated. Expect issues.\n");
            writer.write("# ─────────────────────────────────────────────────────────────────\n");
            writeValue(writer, "lavenderEnabled");
            writeValue(writer, "lavenderVisualizationMode");
            writeValue(writer, "lavenderShaderEmulation");
            writeValue(writer, "lavenderTextureOptimizations");
            writeValue(writer, "lavenderCompatWarnings");
            writeValue(writer, "lavenderLegalNoticeShown");
            writer.write("\n");
            
            writer.write("# Lumen - Lighting engine optimizations\n");
            writeValue(writer, "lumenEnabled");
            writeValue(writer, "lumenLightingEngine");
            writeValue(writer, "lumenBlockLightOptimizations");
            writeValue(writer, "lumenSkyLightOptimizations");
            writeValue(writer, "lumenAsyncLightUpdates");
            writeValue(writer, "lumenCacheLightData");
            writer.write("\n");
            
            writer.write("# MagnetismCore - Physics & memory optimizations (Java 25 FFM)\n");
            writeValue(writer, "magnetismCoreEnabled");
            writeValue(writer, "magnetismNativeMemory");
            writeValue(writer, "magnetismPhysicsOptimizations");
            writeValue(writer, "magnetismMemoryPooling");
            writeValue(writer, "magnetismZeroCopyOperations");
            writer.write("\n");
            
            writer.write("# Asto - Modern VintageFix rewrite (Java 25 + LWJGL 3.3.6)\n");
            writer.write("# 3-5x faster resource loading, 30-60% VRAM reduction\n");
            writeValue(writer, "astoEnabled");
            writeValue(writer, "astoDynamicResourceLoading");
            writeValue(writer, "astoLazyAtlasStitching");
            writeValue(writer, "astoChunkOptimizations");
            writeValue(writer, "astoBytecodeTransformCaching");
            writeValue(writer, "astoMemoryOptimizations");
            writeValue(writer, "astoConcurrentModelBaking");
            writeValue(writer, "astoJarDiscoveryCaching");
            writeValue(writer, "astoFastCollections");
            writer.write("\n");
            
            writer.write("# Fluorine - Additional optimizations\n");
            writeValue(writer, "fluorineEnabled");
            writeValue(writer, "fluorineRenderOptimizations");
            writeValue(writer, "fluorineMemoryOptimizations");
            writeValue(writer, "fluorineThreadOptimizations");
            writer.write("\n");
            
            writer.write("# LegacyFix - Legacy compatibility fixes\n");
            writeValue(writer, "legacyFixEnabled");
            writeValue(writer, "legacyCompatibilityMode");
            writeValue(writer, "legacyFixCrashPrevention");
            writeValue(writer, "legacyFixMemoryLeaks");
            writer.write("\n");
            
            writer.write("# SnowyASM - Advanced memory & performance (<50ns string dedup)\n");
            writeValue(writer, "snowyASMEnabled");
            writeValue(writer, "snowyStringDeduplication");
            writeValue(writer, "snowyZeroAllocationPaths");
            writeValue(writer, "snowyLockFreeConcurrency");
            writeValue(writer, "snowyMemoryReduction");
            writer.write("\n");
            
            writer.write("# JIT Optimization System - Bytecode optimization & patching\n");
            writeValue(writer, "jitEnabled");
            writeValue(writer, "jitHelperEnabled");
            writeValue(writer, "jitInjectEnabled");
            writeValue(writer, "jitUniversalPatcherEnabled");
            writeValue(writer, "jitAggressiveOptimization");

        } catch (IOException e) {
            System.err.println("[Astralis Config] Save failed: " + e.getMessage());
        }
    }

    private static void writeValue(BufferedWriter writer, String key) throws IOException {
        Object value = values.getOrDefault(key, defaults.get(key));
        String valueStr = value instanceof Enum<?> ? ((Enum<?>) value).name() : String.valueOf(value);
        writer.write(key + "=" + valueStr + "\n");
    }

    /**
     * Parse string value to appropriate type
     */
    @SuppressWarnings("unchecked")
    private static Object parseValue(String str, Class<?> type) {
        try {
            if (type == Boolean.class || type == boolean.class) {
                return Boolean.parseBoolean(str);
            } else if (type == Integer.class || type == int.class) {
                return Integer.parseInt(str);
            } else if (type == Long.class || type == long.class) {
                return Long.parseLong(str);
            } else if (type == Double.class || type == double.class) {
                return Double.parseDouble(str);
            } else if (type == Float.class || type == float.class) {
                return Float.parseFloat(str);
            } else if (type == String.class) {
                return str;
            } else if (type.isEnum()) {
                return Enum.valueOf((Class<Enum>) type, str.toUpperCase());
            }
        } catch (Exception e) {
            // Return null to use default
        }
        return null;
    }

    /**
     * Validate configuration against hardware capabilities
     */
    private static void validateAgainstCapabilities() {
        try {
            if (!UniversalCapabilities.isInitialized()) {
                UniversalCapabilities.detect();
            }

            // Validate API preference
            PreferredAPI api = getPreferredAPI();
            if (api == PreferredAPI.VULKAN && !UniversalCapabilities.Vulkan.isAvailable) {
                values.put("preferredAPI", PreferredAPI.OPENGL);
            }
            if (api == PreferredAPI.OPENGL_ES && !UniversalCapabilities.GLES.isGLESContext) {
                values.put("preferredAPI", PreferredAPI.OPENGL);
            }

            // Validate shader engine
            ShaderEngine shader = getPreferredShaderEngine();
            if (shader == ShaderEngine.SPIRV && !UniversalCapabilities.SPIRV.hasGLSPIRV) {
                values.put("shaderEngine", ShaderEngine.GLSL);
            }

            // Validate feature toggles against capabilities
            if (getBoolean("useDSA") && !UniversalCapabilities.Features.DSA) {
                values.put("useDSA", false);
            }
            if (getBoolean("usePersistentMapping") && !UniversalCapabilities.Features.persistentMapping) {
                values.put("usePersistentMapping", false);
            }
            if (getBoolean("useComputeShaders") && !UniversalCapabilities.Features.computeShaders) {
                values.put("useComputeShaders", false);
            }
            if (getBoolean("useSPIRV") && !UniversalCapabilities.SPIRV.hasGLSPIRV) {
                values.put("useSPIRV", false);
            }

            // ── Validate new Vulkan extended features ─────────────────────────────────
            if (UniversalCapabilities.Vulkan.isAvailable) {
                // External memory: need OS-matching extension
                if (getBoolean("vulkanEnableExternalMemoryWin32") &&
                        !UniversalCapabilities.Vulkan.hasExternalMemoryWin32) {
                    values.put("vulkanEnableExternalMemoryWin32", false);
                }
                if (getBoolean("vulkanEnableExternalMemoryFd") &&
                        !UniversalCapabilities.Vulkan.hasExternalMemoryFd) {
                    values.put("vulkanEnableExternalMemoryFd", false);
                }
                // VRS
                if (getBoolean("vulkanEnableVRS") &&
                        !UniversalCapabilities.Vulkan.hasFragmentShadingRate) {
                    values.put("vulkanEnableVRS", false);
                }
                if (getBoolean("vulkanEnableShadingRateImage") &&
                        !UniversalCapabilities.Vulkan.hasShadingRateImage) {
                    values.put("vulkanEnableShadingRateImage", false);
                }
                // Mesh shaders
                if (getBoolean("vulkanEnableMeshShaderEXT") &&
                        !UniversalCapabilities.Vulkan.hasMeshShaderEXT) {
                    values.put("vulkanEnableMeshShaderEXT", false);
                    values.put("vulkanEnableTaskShaders", false);
                }
                // Ray tracing
                if (getBoolean("vulkanEnableRayTracing") &&
                        !UniversalCapabilities.Vulkan.hasRayTracingPipeline) {
                    values.put("vulkanEnableRayTracing", false);
                    values.put("vulkanEnableAccelerationStructure", false);
                }
                if (getBoolean("vulkanEnableRayQuery") &&
                        !UniversalCapabilities.Vulkan.hasRayQuery) {
                    values.put("vulkanEnableRayQuery", false);
                }
                if (getBoolean("vulkanEnableRayTracingPositionFetch") &&
                        !UniversalCapabilities.Vulkan.hasRayTracingPositionFetch) {
                    values.put("vulkanEnableRayTracingPositionFetch", false);
                }
                // Push descriptors
                if (getBoolean("vulkanEnablePushDescriptors") &&
                        !UniversalCapabilities.Vulkan.hasPushDescriptors) {
                    values.put("vulkanEnablePushDescriptors", false);
                }
                // Pipeline library
                if (getBoolean("vulkanEnableGraphicsPipelineLibrary") &&
                        !UniversalCapabilities.Vulkan.hasGraphicsPipelineLibrary) {
                    values.put("vulkanEnableGraphicsPipelineLibrary", false);
                    values.put("vulkanEnablePipelineLibrary", false);
                }
                // Mesh shaders (old key, keep in sync)
                if (getBoolean("vulkanEnableMeshShaders") &&
                        !UniversalCapabilities.Vulkan.hasMeshShader) {
                    values.put("vulkanEnableMeshShaders", false);
                }
            }

        } catch (Throwable t) {
            System.err.println("[Astralis Config] Capability validation failed: " + t.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 6: GETTERS - API Settings
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static PreferredAPI getPreferredAPI() {
        if (!initialized.get()) initialize();
        return (PreferredAPI) values.getOrDefault("preferredAPI", defaults.get("preferredAPI"));
    }

    public static ShaderEngine getPreferredShaderEngine() {
        if (!initialized.get()) initialize();
        return (ShaderEngine) values.getOrDefault("shaderEngine", defaults.get("shaderEngine"));
    }

    public static RendererOverride getRendererOverride() {
        if (!initialized.get()) initialize();
        return (RendererOverride) values.getOrDefault("rendererOverride", defaults.get("rendererOverride"));
    }

    public static PerformanceProfile getPerformanceProfile() {
        if (!initialized.get()) initialize();
        return (PerformanceProfile) values.getOrDefault("performanceProfile", defaults.get("performanceProfile"));
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 7: GETTERS - Thresholds
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static double getTeleportThresholdSquared() {
        if (!initialized.get()) initialize();
        return getDouble("teleportThresholdSquared");
    }

    public static double getMaxFrameTime() {
        if (!initialized.get()) initialize();
        return getDouble("maxFrameTime");
    }

    public static double getFixedTimestep() {
        if (!initialized.get()) initialize();
        return getDouble("fixedTimestep");
    }

    public static int getAIThrottleMask() {
        if (!initialized.get()) initialize();
        return getInt("aiThrottleMask");
    }

    public static int getBatchSize() {
        if (!initialized.get()) initialize();
        return getInt("batchSize");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 8: GETTERS - State Management
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean isStateCacheEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("enableStateCache");
    }

    public static int getStateValidationInterval() {
        if (!initialized.get()) initialize();
        return getInt("stateValidationInterval");
    }

    public static boolean isSyncStateOnExternalChange() {
        if (!initialized.get()) initialize();
        return getBoolean("syncStateOnExternalChange");
    }

    public static boolean isExceptionSafetyEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("enableExceptionSafety");
    }

    public static int getMaxStateStackDepth() {
        if (!initialized.get()) initialize();
        return getInt("maxStateStackDepth");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 9: GETTERS - Logging
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static LoggingLevel getLoggingLevel() {
        if (!initialized.get()) initialize();
        return (LoggingLevel) values.getOrDefault("loggingLevel", defaults.get("loggingLevel"));
    }

    public static int getLogBufferSize() {
        if (!initialized.get()) initialize();
        return getInt("logBufferSize");
    }

    public static long getLogFlushIntervalMs() {
        if (!initialized.get()) initialize();
        return getLong("logFlushIntervalMs");
    }

    public static boolean isLogCallDetails() {
        if (!initialized.get()) initialize();
        return getBoolean("logCallDetails");
    }

    public static boolean isLogFallbacks() {
        if (!initialized.get()) initialize();
        return getBoolean("logFallbacks");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 10: GETTERS - Compatibility
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean isAllowFallback() {
        if (!initialized.get()) initialize();
        return getBoolean("allowFallback");
    }

    public static int getMaxFallbackAttempts() {
        if (!initialized.get()) initialize();
        return getInt("maxFallbackAttempts");
    }

    public static boolean isSkipStateManagerOverwrite() {
        if (!initialized.get()) initialize();
        return getBoolean("skipStateManagerOverwrite");
    }

    public static boolean isDeferStateChanges() {
        if (!initialized.get()) initialize();
        return getBoolean("deferStateChanges");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 11: GETTERS - Feature Toggles
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean isUseVBO() {
        if (!initialized.get()) initialize();
        return getBoolean("useVBO");
    }

    public static boolean isUseVAO() {
        if (!initialized.get()) initialize();
        return getBoolean("useVAO");
    }

    public static boolean isUseInstancing() {
        if (!initialized.get()) initialize();
        return getBoolean("useInstancing");
    }

    public static boolean isUseDSA() {
        if (!initialized.get()) initialize();
        return getBoolean("useDSA");
    }

    public static boolean isUsePersistentMapping() {
        if (!initialized.get()) initialize();
        return getBoolean("usePersistentMapping");
    }

    public static boolean isUseComputeShaders() {
        if (!initialized.get()) initialize();
        return getBoolean("useComputeShaders");
    }

    public static boolean isUseSPIRV() {
        if (!initialized.get()) initialize();
        return getBoolean("useSPIRV");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 12: GETTERS - Debug
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean isDebugOutputEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("enableDebugOutput");
    }

    public static boolean isValidateOnEveryCall() {
        if (!initialized.get()) initialize();
        return getBoolean("validateOnEveryCall");
    }

    public static boolean isTrustDriverVersion() {
        if (!initialized.get()) initialize();
        return getBoolean("trustDriverVersion");
    }

    public static boolean isEnableWrapperQuirks() {
        if (!initialized.get()) initialize();
        return getBoolean("enableWrapperQuirks");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 12.5: GETTERS - Vulkan Settings
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean isVulkanValidationLayers() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanValidationLayers");
    }

    public static boolean isVulkanDebugMode() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanDebugMode");
    }

    public static boolean isVulkanGPUAssistedValidation() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanGPUAssistedValidation");
    }

    public static boolean isVulkanStrictMode() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanStrictMode");
    }

    public static boolean isVulkanEnableTimelineSemaphores() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanEnableTimelineSemaphores");
    }

    public static boolean isVulkanEnableDynamicRendering() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanEnableDynamicRendering");
    }

    public static boolean isVulkanEnableSynchronization2() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanEnableSynchronization2");
    }

    public static boolean isVulkanEnableBufferDeviceAddress() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanEnableBufferDeviceAddress");
    }

    public static boolean isVulkanEnableDescriptorIndexing() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanEnableDescriptorIndexing");
    }

    public static boolean isVulkanEnableMeshShaders() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanEnableMeshShaders");
    }

    public static boolean isVulkanEnableGPUDrivenRendering() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanEnableGPUDrivenRendering");
    }

    public static boolean isVulkanEnableGPUCulling() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanEnableGPUCulling");
    }

    public static boolean isVulkanEnableMultiDrawIndirect() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanEnableMultiDrawIndirect");
    }

    public static boolean isVulkanEnableIndirectCount() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanEnableIndirectCount");
    }

    public static int getVulkanMaxFramesInFlight() {
        if (!initialized.get()) initialize();
        return getInt("vulkanMaxFramesInFlight");
    }

    public static int getVulkanCommandBatchSize() {
        if (!initialized.get()) initialize();
        return getInt("vulkanCommandBatchSize");
    }

    public static int getVulkanDescriptorPoolSize() {
        if (!initialized.get()) initialize();
        return getInt("vulkanDescriptorPoolSize");
    }

    public static int getVulkanPipelineCacheSize() {
        if (!initialized.get()) initialize();
        return getInt("vulkanPipelineCacheSize");
    }

    public static int getVulkanStagingBufferSizeMB() {
        if (!initialized.get()) initialize();
        return getInt("vulkanStagingBufferSizeMB");
    }

    public static boolean isVulkanUseDedicatedAllocations() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanUseDedicatedAllocations");
    }

    public static boolean isVulkanUseMemoryBudget() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanUseMemoryBudget");
    }

    public static long getVulkanMaxDeviceMemoryMB() {
        if (!initialized.get()) initialize();
        return getLong("vulkanMaxDeviceMemoryMB");
    }

    // ─── Vulkan Extended Features (VulkanCallMapper-aligned) ───
    public static boolean isVulkanEnableExternalMemory()            { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableExternalMemory"); }
    public static boolean isVulkanEnableExternalMemoryWin32()       { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableExternalMemoryWin32"); }
    public static boolean isVulkanEnableExternalMemoryFd()          { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableExternalMemoryFd"); }

    public static boolean isVulkanEnableVRS()                       { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableVRS"); }
    public static boolean isVulkanEnableShadingRateImage()          { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableShadingRateImage"); }
    public static int     getVulkanShadingRateTileSize()            { if (!initialized.get()) initialize(); return getInt("vulkanShadingRateTileSize"); }

    public static boolean isVulkanEnableMeshShaderEXT()             { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableMeshShaderEXT"); }
    public static boolean isVulkanEnableTaskShaders()               { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableTaskShaders"); }

    public static boolean isVulkanEnableRayTracing()                { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableRayTracing"); }
    public static boolean isVulkanEnableRayQuery()                  { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableRayQuery"); }
    public static boolean isVulkanEnableAccelerationStructure()     { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableAccelerationStructure"); }
    public static boolean isVulkanEnableRayTracingPositionFetch()   { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableRayTracingPositionFetch"); }
    public static int     getVulkanScratchBufferSizeMB()            { if (!initialized.get()) initialize(); return getInt("vulkanScratchBufferSizeMB"); }

    public static boolean isVulkanEnableAsyncTransfer()             { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableAsyncTransfer"); }
    public static boolean isVulkanEnableAsyncCompute()              { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableAsyncCompute"); }
    public static int     getVulkanAsyncStagingRingBufferMB()       { if (!initialized.get()) initialize(); return getInt("vulkanAsyncStagingRingBufferMB"); }

    public static boolean isVulkanEnableKTX2Pipeline()              { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableKTX2Pipeline"); }
    public static String  getVulkanKTX2CacheDirectory()             { if (!initialized.get()) initialize(); return getString("vulkanKTX2CacheDirectory"); }
    public static int     getVulkanKTX2MaxCacheSizeMB()             { if (!initialized.get()) initialize(); return getInt("vulkanKTX2MaxCacheSizeMB"); }

    public static boolean isVulkanEnablePushDescriptors()           { if (!initialized.get()) initialize(); return getBoolean("vulkanEnablePushDescriptors"); }
    public static boolean isVulkanEnablePipelineLibrary()           { if (!initialized.get()) initialize(); return getBoolean("vulkanEnablePipelineLibrary"); }
    public static boolean isVulkanEnableGraphicsPipelineLibrary()   { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableGraphicsPipelineLibrary"); }
    public static boolean isVulkanEnableComputeMipmaps()            { if (!initialized.get()) initialize(); return getBoolean("vulkanEnableComputeMipmaps"); }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 12.5A: GETTERS - Zero-Overhead Safety System
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if Vulkan safety checks are enabled.
     * When false (default), all safety code is eliminated by JIT for zero overhead.
     * When true, comprehensive safety checks catch bugs during development.
     * 
     * @return true if safety checks enabled, false for zero-overhead production mode
     */
    public static boolean isVulkanSafetyChecksEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanSafetyChecksEnabled");
    }
    
    /**
     * Check if FFI-based ultra-fast native tracking is enabled.
     * Only relevant when safety checks are enabled.
     * FFI mode: ~10-20ns overhead vs Java mode: ~50-100ns overhead
     * 
     * @return true if FFI native tracking enabled
     */
    public static boolean isVulkanSafetyFFITracking() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanSafetyFFITracking");
    }
    
    /**
     * Check if safety violations throw exceptions (strict mode).
     * When false, violations only log warnings.
     * 
     * @return true if strict mode enabled
     */
    public static boolean isVulkanSafetyStrictMode() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanSafetyStrictMode");
    }
    
    /**
     * Check if memory source tracking is enabled.
     * Tracks whether memory was allocated with LWJGL or FFM/Panama to prevent
     * dangerous cross-API frees that cause JVM crashes.
     * 
     * @return true if memory source tracking enabled
     */
    public static boolean isVulkanMemorySourceTracking() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanMemorySourceTracking");
    }
    
    /**
     * Check if deadlock detection is enabled.
     * Monitors lock acquisitions and enforces timeouts to prevent deadlocks.
     * 
     * @return true if deadlock detection enabled
     */
    public static boolean isVulkanDeadlockDetection() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanDeadlockDetection");
    }
    
    /**
     * Check if bindless descriptor validation is enabled.
     * Validates descriptor array indices before GPU access to prevent GPU hangs.
     * 
     * @return true if bindless validation enabled
     */
    public static boolean isVulkanBindlessValidation() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanBindlessValidation");
    }
    
    /**
     * Check if memory leak detection is enabled.
     * Reports unfreed memory on shutdown.
     * 
     * @return true if leak detection enabled
     */
    public static boolean isVulkanLeakDetection() {
        if (!initialized.get()) initialize();
        return getBoolean("vulkanLeakDetection");
    }
    
    /**
     * Get the deadlock detection timeout in milliseconds.
     * Lock acquisitions taking longer than this will be flagged.
     * 
     * @return timeout in milliseconds (default: 5000)
     */
    public static int getVulkanDeadlockTimeoutMs() {
        if (!initialized.get()) initialize();
        return getInt("vulkanDeadlockTimeoutMs");
    }
    
    /**
     * Get the deadlock check interval in milliseconds.
     * How often the detector scans for long-held locks.
     * 
     * @return interval in milliseconds (default: 1000)
     */
    public static int getVulkanDeadlockCheckIntervalMs() {
        if (!initialized.get()) initialize();
        return getInt("vulkanDeadlockCheckIntervalMs");
    }
    
    /**
     * Get the FFI native hash table size (number of entries).
     * Only used when FFI tracking is enabled.
     * 
     * @return hash table size in entries (default: 1048576 = ~8MB)
     */
    public static int getVulkanFFIHashTableSize() {
        if (!initialized.get()) initialize();
        return getInt("vulkanFFIHashTableSize");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 12.6: GETTERS - DirectX Settings (DX 9-12.2 Support)
    // ═══════════════════════════════════════════════════════════════════════════════════

    // DirectX Core Settings
    public static boolean isDirectXEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("directXEnabled");
    }

    public static int getDirectXPreferredVersion() {
        if (!initialized.get()) initialize();
        return getInt("directXPreferredVersion");
    }

    public static boolean isDirectXAllowFallback() {
        if (!initialized.get()) initialize();
        return getBoolean("directXAllowFallback");
    }

    public static boolean isDirectXPreferDX12() {
        if (!initialized.get()) initialize();
        return getBoolean("directXPreferDX12");
    }

    public static boolean isDirectXPreferDX11() {
        if (!initialized.get()) initialize();
        return getBoolean("directXPreferDX11");
    }

    public static int getDirectXMinFeatureLevel() {
        if (!initialized.get()) initialize();
        return getInt("directXMinFeatureLevel");
    }

    public static int getDirectXMaxFeatureLevel() {
        if (!initialized.get()) initialize();
        return getInt("directXMaxFeatureLevel");
    }

    // DirectX Debug/Validation
    public static boolean isDirectXValidation() {
        if (!initialized.get()) initialize();
        return getBoolean("directXValidation");
    }

    public static boolean isDirectXDebugMode() {
        if (!initialized.get()) initialize();
        return getBoolean("directXDebugMode");
    }

    public static boolean isDirectXGPUValidation() {
        if (!initialized.get()) initialize();
        return getBoolean("directXGPUValidation");
    }

    public static boolean isDirectXEnableDRED() {
        if (!initialized.get()) initialize();
        return getBoolean("directXEnableDRED");
    }

    public static boolean isDirectXStrictMode() {
        if (!initialized.get()) initialize();
        return getBoolean("directXStrictMode");
    }

    // DirectX 12 Advanced Features
    public static boolean isDirectXUseTiledResources() {
        if (!initialized.get()) initialize();
        return getBoolean("directXUseTiledResources");
    }

    public static boolean isDirectXUseResourceBarriers() {
        if (!initialized.get()) initialize();
        return getBoolean("directXUseResourceBarriers");
    }

    public static boolean isDirectXUseDescriptorHeaps() {
        if (!initialized.get()) initialize();
        return getBoolean("directXUseDescriptorHeaps");
    }

    public static boolean isDirectXUseBundledCommands() {
        if (!initialized.get()) initialize();
        return getBoolean("directXUseBundledCommands");
    }

    public static boolean isDirectXUseRayTracing() {
        if (!initialized.get()) initialize();
        return getBoolean("directXUseRayTracing");
    }

    public static boolean isDirectXUseMeshShaders() {
        if (!initialized.get()) initialize();
        return getBoolean("directXUseMeshShaders");
    }

    public static boolean isDirectXUseVariableRateShading() {
        if (!initialized.get()) initialize();
        return getBoolean("directXUseVariableRateShading");
    }

    public static boolean isDirectXUseSamplerFeedback() {
        if (!initialized.get()) initialize();
        return getBoolean("directXUseSamplerFeedback");
    }

    // DirectX Performance Settings
    public static int getDirectXMaxFrameLatency() {
        if (!initialized.get()) initialize();
        return getInt("directXMaxFrameLatency");
    }

    public static int getDirectXCommandListPoolSize() {
        if (!initialized.get()) initialize();
        return getInt("directXCommandListPoolSize");
    }

    public static int getDirectXUploadHeapSizeMB() {
        if (!initialized.get()) initialize();
        return getInt("directXUploadHeapSizeMB");
    }

    public static int getDirectXDescriptorHeapSize() {
        if (!initialized.get()) initialize();
        return getInt("directXDescriptorHeapSize");
    }

    public static boolean isDirectXUseStablePowerState() {
        if (!initialized.get()) initialize();
        return getBoolean("directXUseStablePowerState");
    }

    // DirectX Fallback/Compatibility
    public static boolean isDirectXEnableWARP() {
        if (!initialized.get()) initialize();
        return getBoolean("directXEnableWARP");
    }

    public static boolean isDirectXEnableDirectStorage() {
        if (!initialized.get()) initialize();
        return getBoolean("directXEnableDirectStorage");
    }

    public static boolean isDirectXEnableAutoStereo() {
        if (!initialized.get()) initialize();
        return getBoolean("directXEnableAutoStereo");
    }

    // DirectX 9-specific settings
    public static boolean isDirectX9UseHardwareVP() {
        if (!initialized.get()) initialize();
        return getBoolean("directX9UseHardwareVP");
    }

    public static boolean isDirectX9EnableVSync() {
        if (!initialized.get()) initialize();
        return getBoolean("directX9EnableVSync");
    }

    public static boolean isDirectX9AllowMultithreading() {
        if (!initialized.get()) initialize();
        return getBoolean("directX9AllowMultithreading");
    }

    // DirectX 10/11 specific settings
    public static boolean isDirectX11UseDeferredContext() {
        if (!initialized.get()) initialize();
        return getBoolean("directX11UseDeferredContext");
    }

    public static boolean isDirectX11EnableMultithreading() {
        if (!initialized.get()) initialize();
        return getBoolean("directX11EnableMultithreading");
    }

    // Legacy DirectX getters (for backward compatibility)
    @Deprecated
    public static boolean isDirectXEnableRayTracing() {
        return isDirectXUseRayTracing();
    }

    @Deprecated
    public static boolean isDirectXEnableMeshShaders() {
        return isDirectXUseMeshShaders();
    }

    @Deprecated
    public static boolean isDirectXEnableVariableRateShading() {
        return isDirectXUseVariableRateShading();
    }

    @Deprecated
    public static boolean isDirectXEnableSamplerFeedback() {
        return isDirectXUseSamplerFeedback();
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 12.5: HLSL SHADER CONFIGURATION GETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean isHLSLEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnabled");
    }

    public static String getHLSLShaderModel() {
        if (!initialized.get()) initialize();
        return getString("hlslShaderModel");
    }

    public static boolean isHLSLPreferDXC() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslPreferDXC");
    }

    public static int getHLSLOptimizationLevel() {
        if (!initialized.get()) initialize();
        return getInt("hlslOptimizationLevel");
    }

    public static boolean isHLSLEnableDebugInfo() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableDebugInfo");
    }

    public static boolean isHLSLDisableOptimizations() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslDisableOptimizations");
    }

    public static boolean isHLSLWarningsAsErrors() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslWarningsAsErrors");
    }

    public static boolean isHLSLStrictMode() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslStrictMode");
    }

    public static boolean isHLSLIEEEStrictness() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslIEEEStrictness");
    }

    public static boolean isHLSLPreferFlowControl() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslPreferFlowControl");
    }

    public static boolean isHLSLAvoidFlowControl() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslAvoidFlowControl");
    }

    public static boolean isHLSLEnableBackwardsCompatibility() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableBackwardsCompatibility");
    }

    public static boolean isHLSLPackMatrixRowMajor() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslPackMatrixRowMajor");
    }

    public static boolean isHLSLPackMatrixColumnMajor() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslPackMatrixColumnMajor");
    }

    // Root Signature Settings
    public static boolean isHLSLAutoGenerateRootSignature() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslAutoGenerateRootSignature");
    }

    public static String getHLSLRootSignatureVersion() {
        if (!initialized.get()) initialize();
        return getString("hlslRootSignatureVersion");
    }

    public static boolean isHLSLEnableRootConstants() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableRootConstants");
    }

    public static boolean isHLSLEnableDescriptorTables() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableDescriptorTables");
    }

    public static boolean isHLSLEnableStaticSamplers() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableStaticSamplers");
    }

    public static int getHLSLMaxRootParameters() {
        if (!initialized.get()) initialize();
        return getInt("hlslMaxRootParameters");
    }

    // Bytecode Caching
    public static boolean isHLSLEnableBytecodeCache() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableBytecodeCache");
    }

    public static String getHLSLBytecodeCacheDir() {
        if (!initialized.get()) initialize();
        return getString("hlslBytecodeCacheDir");
    }

    public static int getHLSLBytecodeCacheMaxSizeMB() {
        if (!initialized.get()) initialize();
        return getInt("hlslBytecodeCacheMaxSizeMB");
    }

    public static boolean isHLSLEnableDiskCache() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableDiskCache");
    }

    public static boolean isHLSLCacheCompressionEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslCacheCompressionEnabled");
    }

    public static boolean isHLSLValidateCachedShaders() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslValidateCachedShaders");
    }

    // Advanced Features
    public static boolean isHLSLEnableWaveIntrinsics() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableWaveIntrinsics");
    }

    public static boolean isHLSLEnableFloat16() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableFloat16");
    }

    public static boolean isHLSLEnableInt16() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableInt16");
    }

    public static boolean isHLSLEnableViewInstancing() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableViewInstancing");
    }

    public static boolean isHLSLEnableBarycentrics() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableBarycentrics");
    }

    public static boolean isHLSLEnableRaytracingShaders() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableRaytracingShaders");
    }

    public static boolean isHLSLEnableMeshShaderSupport() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableMeshShaderSupport");
    }

    public static boolean isHLSLEnableAmplificationShaders() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableAmplificationShaders");
    }

    public static boolean isHLSLEnableWorkGraphs() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableWorkGraphs");
    }

    // Translation Settings
    public static boolean isHLSLEnableGLSLTranslation() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableGLSLTranslation");
    }

    public static boolean isHLSLTranslationCacheEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslTranslationCacheEnabled");
    }

    public static boolean isHLSLPreserveGLSLSemantics() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslPreserveGLSLSemantics");
    }

    public static boolean isHLSLEmulateGLSLBuiltins() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEmulateGLSLBuiltins");
    }

    public static boolean isHLSLConvertLayoutQualifiers() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslConvertLayoutQualifiers");
    }

    public static boolean isHLSLHandleGLSLExtensions() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslHandleGLSLExtensions");
    }

    public static int getHLSLTranslationQuality() {
        if (!initialized.get()) initialize();
        return getInt("hlslTranslationQuality");
    }

    // Core Settings
    public static String getHLSLMinShaderModel() {
        if (!initialized.get()) initialize();
        return getString("hlslMinShaderModel");
    }

    public static String getHLSLMaxShaderModel() {
        if (!initialized.get()) initialize();
        return getString("hlslMaxShaderModel");
    }

    public static boolean isHLSLFallbackToD3DCompiler() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslFallbackToD3DCompiler");
    }

    public static boolean isHLSLAutoDetectShaderModel() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslAutoDetectShaderModel");
    }

    public static boolean isHLSLEnableUnboundedArrays() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableUnboundedArrays");
    }

    public static boolean isHLSLEnableDynamicResources() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableDynamicResources");
    }

    // Shader Model Flags
    public static boolean isHLSLSM2Enabled() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslSM2_Enabled");
    }

    public static boolean isHLSLSM3Enabled() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslSM3_Enabled");
    }

    public static boolean isHLSLSM4Enabled() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslSM4_Enabled");
    }

    public static boolean isHLSLSM4_1Enabled() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslSM4_1_Enabled");
    }

    public static boolean isHLSLSM5Enabled() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslSM5_Enabled");
    }

    public static boolean isHLSLSM5_1Enabled() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslSM5_1_Enabled");
    }

    public static boolean isHLSLSM6Enabled() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslSM6_Enabled");
    }

    // Additional Advanced Features
    public static boolean isHLSLEnableInt64() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableInt64");
    }

    public static boolean isHLSLEnableSamplerFeedback() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableSamplerFeedback");
    }

    public static boolean isHLSLEnableVariableRateShading() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableVariableRateShading");
    }

    // Bytecode Cache Advanced
    public static boolean isHLSLCacheShaderReflection() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslCacheShaderReflection");
    }

    public static boolean isHLSLIncrementalCompilation() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslIncrementalCompilation");
    }

    // Root Signature Advanced
    public static int getHLSLRootSignatureFlags() {
        if (!initialized.get()) initialize();
        return getInt("hlslRootSignatureFlags");
    }

    // SPIR-V Cross-Compilation
    public static boolean isHLSLEnableSPIRVBackend() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableSPIRVBackend");
    }

    public static boolean isHLSLSPIRVOptimize() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslSPIRVOptimize");
    }

    public static boolean isHLSLSPIRVValidate() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslSPIRVValidate");
    }

    // Debugging Advanced
    public static boolean isHLSLPrintShaderErrors() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslPrintShaderErrors");
    }

    public static boolean isHLSLPrintShaderWarnings() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslPrintShaderWarnings");
    }

    public static boolean isHLSLDumpCompiledShaders() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslDumpCompiledShaders");
    }

    // Debugging & Profiling
    public static boolean isHLSLEnablePIXMarkers() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnablePIXMarkers");
    }

    public static boolean isHLSLEnableShaderProfiling() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslEnableShaderProfiling");
    }

    public static boolean isHLSLGenerateShaderASM() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslGenerateShaderASM");
    }

    public static boolean isHLSLVerboseCompilation() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslVerboseCompilation");
    }

    public static boolean isHLSLLogCompilationTimes() {
        if (!initialized.get()) initialize();
        return getBoolean("hlslLogCompilationTimes");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 12B: MSL (METAL SHADING LANGUAGE) GETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    // Basic Settings
    public static boolean isMSLEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnabled");
    }

    public static String getMSLLanguageVersion() {
        if (!initialized.get()) initialize();
        return getString("mslLanguageVersion");
    }

    public static int getMSLOptimizationLevel() {
        if (!initialized.get()) initialize();
        return getInt("mslOptimizationLevel");
    }

    public static boolean isMSLFastMathEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("mslFastMathEnabled");
    }

    public static boolean isMSLPreserveInvarianceEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("mslPreserveInvarianceEnabled");
    }

    public static boolean isMSLEnableDebugInfo() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableDebugInfo");
    }

    public static boolean isMSLDisableOptimizations() {
        if (!initialized.get()) initialize();
        return getBoolean("mslDisableOptimizations");
    }

    public static boolean isMSLWarningsAsErrors() {
        if (!initialized.get()) initialize();
        return getBoolean("mslWarningsAsErrors");
    }

    public static boolean isMSLStrictMode() {
        if (!initialized.get()) initialize();
        return getBoolean("mslStrictMode");
    }

    // Feature Gates
    public static boolean isMSLEnableArgumentBuffers() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableArgumentBuffers");
    }

    public static boolean isMSLEnableTier2ArgumentBuffers() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableTier2ArgumentBuffers");
    }

    public static boolean isMSLEnableIndirectCommandBuffers() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableIndirectCommandBuffers");
    }

    public static boolean isMSLEnableRasterOrderGroups() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableRasterOrderGroups");
    }

    public static boolean isMSLEnableImageBlocks() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableImageBlocks");
    }

    public static boolean isMSLEnableTileShaders() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableTileShaders");
    }

    public static boolean isMSLEnableRaytracing() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableRaytracing");
    }

    public static boolean isMSLEnableMeshShaders() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableMeshShaders");
    }

    public static boolean isMSLEnableFunctionPointers() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableFunctionPointers");
    }

    public static boolean isMSLEnableDynamicLibraries() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableDynamicLibraries");
    }

    // Function Constants
    public static boolean isMSLUseFunctionConstants() {
        if (!initialized.get()) initialize();
        return getBoolean("mslUseFunctionConstants");
    }

    public static boolean isMSLAutoInjectConstants() {
        if (!initialized.get()) initialize();
        return getBoolean("mslAutoInjectConstants");
    }

    public static int getMSLMaxFunctionConstants() {
        if (!initialized.get()) initialize();
        return getInt("mslMaxFunctionConstants");
    }

    // Texture & Sampler Settings
    public static String getMSLDefaultSamplerFiltering() {
        if (!initialized.get()) initialize();
        return getString("mslDefaultSamplerFiltering");
    }

    public static String getMSLDefaultSamplerAddressMode() {
        if (!initialized.get()) initialize();
        return getString("mslDefaultSamplerAddressMode");
    }

    public static int getMSLMaxAnisotropy() {
        if (!initialized.get()) initialize();
        return getInt("mslMaxAnisotropy");
    }

    // Bytecode Caching
    public static boolean isMSLEnableBytecodeCache() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableBytecodeCache");
    }

    public static String getMSLBytecodeCacheDir() {
        if (!initialized.get()) initialize();
        return getString("mslBytecodeCacheDir");
    }

    public static int getMSLBytecodeCacheMaxSizeMB() {
        if (!initialized.get()) initialize();
        return getInt("mslBytecodeCacheMaxSizeMB");
    }

    public static boolean isMSLEnableDiskCache() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableDiskCache");
    }

    public static boolean isMSLCacheCompressionEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("mslCacheCompressionEnabled");
    }

    public static boolean isMSLValidateCachedShaders() {
        if (!initialized.get()) initialize();
        return getBoolean("mslValidateCachedShaders");
    }

    public static boolean isMSLBinaryArchiveEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("mslBinaryArchiveEnabled");
    }

    // Cross-Compilation
    public static boolean isMSLEnableGLSLTranslation() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableGLSLTranslation");
    }

    public static boolean isMSLEnableHLSLTranslation() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableHLSLTranslation");
    }

    public static boolean isMSLEnableSPIRVTranslation() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableSPIRVTranslation");
    }

    public static boolean isMSLTranslationCacheEnabled() {
        if (!initialized.get()) initialize();
        return getBoolean("mslTranslationCacheEnabled");
    }

    public static boolean isMSLPreserveSourceSemantics() {
        if (!initialized.get()) initialize();
        return getBoolean("mslPreserveSourceSemantics");
    }

    public static boolean isMSLEmulateSourceBuiltins() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEmulateSourceBuiltins");
    }

    public static boolean isMSLFlipVertexY() {
        if (!initialized.get()) initialize();
        return getBoolean("mslFlipVertexY");
    }

    public static boolean isMSLAdjustClipSpace() {
        if (!initialized.get()) initialize();
        return getBoolean("mslAdjustClipSpace");
    }

    // SPIRV-Cross Options
    public static String getMSLSPIRVCrossVersion() {
        if (!initialized.get()) initialize();
        return getString("mslSPIRVCrossVersion");
    }

    public static boolean isMSLSPIRVArgumentBuffers() {
        if (!initialized.get()) initialize();
        return getBoolean("mslSPIRVArgumentBuffers");
    }

    public static boolean isMSLSPIRVTextureBufferNative() {
        if (!initialized.get()) initialize();
        return getBoolean("mslSPIRVTextureBufferNative");
    }

    // Shader Variants
    public static boolean isMSLEnableShaderVariants() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableShaderVariants");
    }

    public static int getMSLMaxVariantsPerShader() {
        if (!initialized.get()) initialize();
        return getInt("mslMaxVariantsPerShader");
    }

    public static String getMSLVariantSelectionStrategy() {
        if (!initialized.get()) initialize();
        return getString("mslVariantSelectionStrategy");
    }

    // Buffer Management
    public static int getMSLDefaultBufferAlignment() {
        if (!initialized.get()) initialize();
        return getInt("mslDefaultBufferAlignment");
    }

    public static int getMSLArgumentBufferTier() {
        if (!initialized.get()) initialize();
        return getInt("mslArgumentBufferTier");
    }

    public static int getMSLMaxBufferBindings() {
        if (!initialized.get()) initialize();
        return getInt("mslMaxBufferBindings");
    }

    public static int getMSLMaxTextureBindings() {
        if (!initialized.get()) initialize();
        return getInt("mslMaxTextureBindings");
    }

    public static int getMSLMaxSamplerBindings() {
        if (!initialized.get()) initialize();
        return getInt("mslMaxSamplerBindings");
    }

    public static boolean isMSLUseResourceHeaps() {
        if (!initialized.get()) initialize();
        return getBoolean("mslUseResourceHeaps");
    }

    // Compute Shader Settings
    public static int getMSLComputeThreadExecutionWidth() {
        if (!initialized.get()) initialize();
        return getInt("mslComputeThreadExecutionWidth");
    }

    public static int getMSLComputeMaxThreadsPerThreadgroup() {
        if (!initialized.get()) initialize();
        return getInt("mslComputeMaxThreadsPerThreadgroup");
    }

    public static int getMSLComputeThreadgroupSizeMultiple() {
        if (!initialized.get()) initialize();
        return getInt("mslComputeThreadgroupSizeMultiple");
    }

    public static String getMSLComputeMemoryBarrierScope() {
        if (!initialized.get()) initialize();
        return getString("mslComputeMemoryBarrierScope");
    }

    // Debugging & Profiling
    public static boolean isMSLEnablePIXMarkers() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnablePIXMarkers");
    }

    public static boolean isMSLEnableShaderProfiling() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableShaderProfiling");
    }

    public static boolean isMSLGenerateShaderASM() {
        if (!initialized.get()) initialize();
        return getBoolean("mslGenerateShaderASM");
    }

    public static boolean isMSLVerboseCompilation() {
        if (!initialized.get()) initialize();
        return getBoolean("mslVerboseCompilation");
    }

    public static boolean isMSLLogCompilationTimes() {
        if (!initialized.get()) initialize();
        return getBoolean("mslLogCompilationTimes");
    }

    public static String getMSLValidationMode() {
        if (!initialized.get()) initialize();
        return getString("mslValidationMode");
    }

    // Advanced Features
    public static boolean isMSLEnableNonUniformResourceIndex() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableNonUniformResourceIndex");
    }

    public static boolean isMSLEnableQuadPermutations() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableQuadPermutations");
    }

    public static boolean isMSLEnableSIMDGroupOperations() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableSIMDGroupOperations");
    }

    public static boolean isMSLEnableAtomicOperations() {
        if (!initialized.get()) initialize();
        return getBoolean("mslEnableAtomicOperations");
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 12C: GLSL (OPENGL SHADING LANGUAGE) GETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean isGLSLEnabled() { if (!initialized.get()) initialize(); return getBoolean("glslEnabled"); }
    public static String getGLSLLanguageVersion() { if (!initialized.get()) initialize(); return getString("glslLanguageVersion"); }
    public static int getGLSLOptimizationLevel() { if (!initialized.get()) initialize(); return getInt("glslOptimizationLevel"); }
    public static boolean isGLSLCoreProfile() { if (!initialized.get()) initialize(); return getBoolean("glslCoreProfile"); }
    public static boolean isGLSLEnableDebugInfo() { if (!initialized.get()) initialize(); return getBoolean("glslEnableDebugInfo"); }
    public static boolean isGLSLEnableExplicitLocations() { if (!initialized.get()) initialize(); return getBoolean("glslEnableExplicitLocations"); }
    public static boolean isGLSLEnableUniformBufferObjects() { if (!initialized.get()) initialize(); return getBoolean("glslEnableUniformBufferObjects"); }
    public static boolean isGLSLEnableShaderStorageBuffers() { if (!initialized.get()) initialize(); return getBoolean("glslEnableShaderStorageBuffers"); }
    public static boolean isGLSLEnableComputeShaders() { if (!initialized.get()) initialize(); return getBoolean("glslEnableComputeShaders"); }
    public static boolean isGLSLEnableTessellationShaders() { if (!initialized.get()) initialize(); return getBoolean("glslEnableTessellationShaders"); }
    public static boolean isGLSLEnableGeometryShaders() { if (!initialized.get()) initialize(); return getBoolean("glslEnableGeometryShaders"); }
    public static boolean isGLSLEnableBinaryCache() { if (!initialized.get()) initialize(); return getBoolean("glslEnableBinaryCache"); }
    public static String getGLSLBinaryCacheDir() { if (!initialized.get()) initialize(); return getString("glslBinaryCacheDir"); }
    public static int getGLSLBinaryCacheMaxSizeMB() { if (!initialized.get()) initialize(); return getInt("glslBinaryCacheMaxSizeMB"); }
    public static boolean isGLSLEnableHLSLTranslation() { if (!initialized.get()) initialize(); return getBoolean("glslEnableHLSLTranslation"); }
    public static boolean isGLSLEnableSPIRVTranslation() { if (!initialized.get()) initialize(); return getBoolean("glslEnableSPIRVTranslation"); }
    public static boolean isGLSLTranslationCacheEnabled() { if (!initialized.get()) initialize(); return getBoolean("glslTranslationCacheEnabled"); }
    public static boolean isGLSLBindlessTexturesEnabled() { if (!initialized.get()) initialize(); return getBoolean("glslBindlessTexturesEnabled"); }
    public static int getGLSLMaxTextureUnits() { if (!initialized.get()) initialize(); return getInt("glslMaxTextureUnits"); }
    public static boolean isGLSLPreferSSBO() { if (!initialized.get()) initialize(); return getBoolean("glslPreferSSBO"); }
    public static int getGLSLMaxUniformBlockSize() { if (!initialized.get()) initialize(); return getInt("glslMaxUniformBlockSize"); }
    public static boolean isGLSLVerboseCompilation() { if (!initialized.get()) initialize(); return getBoolean("glslVerboseCompilation"); }
    public static String getGLSLValidationMode() { if (!initialized.get()) initialize(); return getString("glslValidationMode"); }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 12D: DEEPMIX INTEGRATION GETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /** Whether DeepMix integration is enabled at all. */
    public static boolean isDeepMixEnabled() {
        return getBoolean("deepmixEnabled");
    }

    /** Log each mixin config that DeepMix queues on our behalf. */
    public static boolean isDeepMixLogQueuedConfigs() {
        return getBoolean("deepmixLogQueuedConfigs");
    }

    /** Log when IMixinConfigHijacker intercepts a conflicting config. */
    public static boolean isDeepMixLogHijacks() {
        return getBoolean("deepmixLogHijacks");
    }

    /**
     * Register via {@code IEarlyMixinLoader} (coremod phase).
     * <p>Must be true — Astralis GL hooks require early registration.</p>
     */
    public static boolean isDeepMixUseEarlyLoader() {
        return getBoolean("deepmixUseEarlyLoader");
    }

    /**
     * Register via {@code ILateMixinLoader} (mod-construction phase).
     * <p>Only needed if we add optional late-phase compat mixins in the future.</p>
     */
    public static boolean isDeepMixUseLateLoader() {
        return getBoolean("deepmixUseLateLoader");
    }

    /** Automatically resolve loader priority conflicts with other mods. */
    public static boolean isDeepMixAutoResolveConflicts() {
        return getBoolean("deepmixAutoResolveConflicts");
    }

    /**
     * Our effective loader priority.
     * <p>1000 = shader-pack tier, above Sodium (750) and vanilla handlers (500).</p>
     */
    public static int getDeepMixPriority() {
        return getInt("deepmixPriority");
    }

    /** Allow DeepMix to apply SpongeForgeFixer if SpongeForge is detected. */
    public static boolean isDeepMixAllowSpongeForgePatch() {
        return getBoolean("deepmixAllowSpongeForgePatch");
    }

    /** Allow DeepMix to apply MixinExtrasFixer for ASM 5.0.x handle bugs. */
    public static boolean isDeepMixAllowMixinExtrasFix() {
        return getBoolean("deepmixAllowMixinExtrasFix");
    }

    /** Allow DeepMix to patch ancient mods that load mixins incorrectly. */
    public static boolean isDeepMixAllowAncientModPatch() {
        return getBoolean("deepmixAllowAncientModPatch");
    }

    /**
     * Extra mixin config files to register via the late loader.
     * <p>Empty by default — only populated if {@code deepmixUseLateLoader=true}
     * and late configs are explicitly listed in {@code astralis.cfg}.</p>
     */
    public static String[] getDeepMixLateConfigs() {
        Object raw = values.getOrDefault("deepmixLateConfigs", defaults.get("deepmixLateConfigs"));
        if (raw instanceof String[] arr) return arr;
        if (raw instanceof String s && !s.isEmpty()) return s.split(",");
        return new String[0];
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 12E: MINI_DIRTYROOM INTEGRATION GETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /** Master switch — false disables all MDR bootstrap logic. */
    public static boolean isMDREnabled() {
        return getBoolean("mdrEnabled");
    }

    /** Log each MDR bootstrap phase to the init report. */
    public static boolean isMDRLogBootstrap() {
        return getBoolean("mdrLogBootstrap");
    }

    /**
     * Allow MDR to relaunch the JVM on Java {@code TARGET_JAVA_VERSION} if the
     * current runtime is older.
     */
    public static boolean isMDRAutoUpgradeJava() {
        return getBoolean("mdrAutoUpgradeJava");
    }

    /** Allow MDR to download a JRE if no suitable local installation is found. */
    public static boolean isMDRDownloadJRE() {
        return getBoolean("mdrDownloadJRE");
    }

    /** Allow MDR to download LWJGL 3.4.0 jars from Maven Central if missing. */
    public static boolean isMDRDownloadLWJGL() {
        return getBoolean("mdrDownloadLWJGL");
    }

    /** Verify SHA-256 checksums of every downloaded artifact before use. */
    public static boolean isMDRVerifyChecksums() {
        return getBoolean("mdrVerifyChecksums");
    }

    /**
     * Nuclear option: overwrite launcher LWJGL jar files on disk.
     * <p>Android-only intent; dangerous on desktop. Off by default.</p>
     */
    public static boolean isMDROverrideFilesOnDisk() {
        return getBoolean("mdrOverrideFilesOnDisk");
    }

    /**
     * Force Android code paths even when running on desktop.
     * <p>Only useful for testing Android launcher compatibility on a PC.</p>
     */
    public static boolean isMDRAndroidMode() {
        return getBoolean("mdrAndroidMode");
    }

    /** Per-download HTTP timeout in seconds. */
    public static int getMDRDownloadTimeoutSecs() {
        return getInt("mdrDownloadTimeoutSecs");
    }

    /** How many times to retry native library loading before giving up. */
    public static int getMDRNativeLoadRetries() {
        return getInt("mdrNativeLoadRetries");
    }

    /**
     * Milliseconds {@code AstralisCore#onMixinConfigQueued} waits for MDR
     * bootstrap to complete before proceeding regardless.
     */
    public static int getMDRBootstrapTimeoutMs() {
        return getInt("mdrBootstrapTimeoutMs");
    }

    /**
     * Custom LWJGL download mirror URL.
     * <p>Empty string = use Maven Central ({@code repo1.maven.org}).</p>
     */
    public static String getMDRLwjglMirrorUrl() {
        return getString("mdrLwjglMirrorUrl");
    }

    /**
     * Custom JRE download mirror URL.
     * <p>Empty string = use Adoptium ({@code api.adoptium.net}).</p>
     */
    public static String getMDRJreMirrorUrl() {
        return getString("mdrJreMirrorUrl");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 12B: INTEGRATION MODULES CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    // PhotonEngine
    public static boolean isPhotonEngineEnabled() { return getBoolean("photonEngineEnabled"); }
    public static boolean isPhotonEnhancedBatchingEnabled() { return getBoolean("photonEnhancedBatching"); }
    public static boolean isPhotonFontAtlasResizeEnabled() { return getBoolean("photonFontAtlasResize"); }
    public static boolean isPhotonMapAtlasGenerationEnabled() { return getBoolean("photonMapAtlasGeneration"); }
    public static boolean isPhotonTextTranslucencySkipEnabled() { return getBoolean("photonTextTranslucencySkip"); }
    public static boolean isPhotonFastTextLookupEnabled() { return getBoolean("photonFastTextLookup"); }
    public static boolean isPhotonFramebufferOptimizationEnabled() { return getBoolean("photonFramebufferOptimization"); }
    public static boolean isPhotonZeroCopyUploadEnabled() { return getBoolean("photonZeroCopyUpload"); }
    public static boolean isPhotonParallelBatchingEnabled() { return getBoolean("photonParallelBatching"); }
    public static boolean isPhotonFixAppleGPUUploadEnabled() { return getBoolean("photonFixAppleGPUUpload"); }
    
    // ShortStack
    public static boolean isShortStackEnabled() { return getBoolean("shortStackEnabled"); }
    public static boolean isShortStackParallelRecipesEnabled() { return getBoolean("shortStackParallelRecipes"); }
    public static boolean isShortStackRecipeCacheEnabled() { return getBoolean("shortStackRecipeCache"); }
    public static boolean isShortStackVirtualThreadsEnabled() { return getBoolean("shortStackVirtualThreads"); }
    public static int getShortStackOptimizationLevel() { return getInt("shortStackOptimizationLevel"); }
    
    // Neon
    public static boolean isNeonEnabled() { return getBoolean("neonEnabled"); }
    public static boolean isNeonNativeLightEngineEnabled() { return getBoolean("neonNativeLightEngine"); }
    public static boolean isNeonSpatialIndexingEnabled() { return getBoolean("neonSpatialIndexing"); }
    public static boolean isNeonTemporalSmoothingEnabled() { return getBoolean("neonTemporalSmoothing"); }
    public static boolean isNeonAsyncLightUpdatesEnabled() { return getBoolean("neonAsyncLightUpdates"); }
    public static boolean isNeonLumenBridgeEnabled() { return getBoolean("neonLumenBridge"); }
    
    // AllTheLeaksReborn
    public static boolean isAllTheLeaksEnabled() { return getBoolean("allTheLeaksEnabled"); }
    public static boolean isLeaksMemoryMonitoringEnabled() { return getBoolean("leaksMemoryMonitoring"); }
    public static boolean isLeaksLeakTrackingEnabled() { return getBoolean("leaksLeakTracking"); }
    public static boolean isLeaksAutoCleanupEnabled() { return getBoolean("leaksAutoCleanup"); }
    public static int getLeaksReportingLevel() { return getInt("leaksReportingLevel"); }
    
    // BlueCore
    public static boolean isBlueCoreEnabled() { return getBoolean("blueCoreEnabled"); }
    public static boolean isBlueCoreHighPerfLoggingEnabled() { return getBoolean("blueCoreHighPerfLogging"); }
    public static boolean isBlueCoreThreadOptimizationsEnabled() { return getBoolean("blueCoreThreadOptimizations"); }
    public static boolean isBlueCoreAllocationMinimizationEnabled() { return getBoolean("blueCoreAllocationMinimization"); }
    
    // Bolt
    public static boolean isBoltEnabled() { return getBoolean("boltEnabled"); }
    public static boolean isBoltThreadOptimizationsEnabled() { return getBoolean("boltThreadOptimizations"); }
    public static boolean isBoltVirtualThreadsEnabled() { return getBoolean("boltVirtualThreads"); }
    public static boolean isBoltTaskSchedulingEnabled() { return getBoolean("boltTaskScheduling"); }
    public static int getBoltConcurrencyLevel() { return getInt("boltConcurrencyLevel"); }
    
    // ChunkMotion
    public static boolean isChunkMotionEnabled() { return getBoolean("chunkMotionEnabled"); }
    public static String getChunkAnimationMode() { return getString("chunkAnimationMode"); }
    public static String getChunkEasingFunction() { return getString("chunkEasingFunction"); }
    public static int getChunkAnimationDuration() { return getInt("chunkAnimationDuration"); }
    public static boolean isChunkRenderOptimizationEnabled() { return getBoolean("chunkRenderOptimization"); }
    
    // GoodOptimizations
    public static boolean isGoodOptEnabled() { return getBoolean("goodOptEnabled"); }
    public static boolean isGoodOptLightmapCacheEnabled() { return getBoolean("goodOptLightmapCache"); }
    public static boolean isGoodOptRenderOptimizationsEnabled() { return getBoolean("goodOptRenderOptimizations"); }
    public static boolean isGoodOptMemoryOptimizationsEnabled() { return getBoolean("goodOptMemoryOptimizations"); }
    
    // Haku - EXPERIMENTAL (NOT RECOMMENDED)
    public static boolean isHakuEnabled() { return getBoolean("hakuEnabled"); }
    public static boolean isHakuExperimentalFeaturesEnabled() { return getBoolean("hakuExperimentalFeatures"); }
    public static boolean isHakuDebugModeEnabled() { return getBoolean("hakuDebugMode"); }
    public static boolean isHakuWarningAcknowledged() { return getBoolean("hakuWarningAcknowledged"); }
    
    // Lavender - OptiFine compatibility (NOT RECOMMENDED - LEGAL CONCERNS)
    public static boolean isLavenderEnabled() { return getBoolean("lavenderEnabled"); }
    public static boolean isLavenderVisualizationModeEnabled() { return getBoolean("lavenderVisualizationMode"); }
    public static boolean isLavenderShaderEmulationEnabled() { return getBoolean("lavenderShaderEmulation"); }
    public static boolean isLavenderTextureOptimizationsEnabled() { return getBoolean("lavenderTextureOptimizations"); }
    public static boolean isLavenderCompatWarningsEnabled() { return getBoolean("lavenderCompatWarnings"); }
    public static boolean isLavenderLegalNoticeShown() { return getBoolean("lavenderLegalNoticeShown"); }
    
    // Lumen
    public static boolean isLumenEnabled() { return getBoolean("lumenEnabled"); }
    public static boolean isLumenLightingEngineEnabled() { return getBoolean("lumenLightingEngine"); }
    public static boolean isLumenBlockLightOptimizationsEnabled() { return getBoolean("lumenBlockLightOptimizations"); }
    public static boolean isLumenSkyLightOptimizationsEnabled() { return getBoolean("lumenSkyLightOptimizations"); }
    public static boolean isLumenAsyncLightUpdatesEnabled() { return getBoolean("lumenAsyncLightUpdates"); }
    public static boolean isLumenCacheLightDataEnabled() { return getBoolean("lumenCacheLightData"); }
    
    // MagnetismCore
    public static boolean isMagnetismCoreEnabled() { return getBoolean("magnetismCoreEnabled"); }
    public static boolean isMagnetismNativeMemoryEnabled() { return getBoolean("magnetismNativeMemory"); }
    public static boolean isMagnetismPhysicsOptimizationsEnabled() { return getBoolean("magnetismPhysicsOptimizations"); }
    public static boolean isMagnetismMemoryPoolingEnabled() { return getBoolean("magnetismMemoryPooling"); }
    public static boolean isMagnetismZeroCopyOperationsEnabled() { return getBoolean("magnetismZeroCopyOperations"); }
    
    // Asto - Modern VintageFix rewrite
    public static boolean isAstoEnabled() { return getBoolean("astoEnabled"); }
    public static boolean isAstoDynamicResourceLoadingEnabled() { return getBoolean("astoDynamicResourceLoading"); }
    public static boolean isAstoLazyAtlasStitchingEnabled() { return getBoolean("astoLazyAtlasStitching"); }
    public static boolean isAstoChunkOptimizationsEnabled() { return getBoolean("astoChunkOptimizations"); }
    public static boolean isAstoBytecodeTransformCachingEnabled() { return getBoolean("astoBytecodeTransformCaching"); }
    public static boolean isAstoMemoryOptimizationsEnabled() { return getBoolean("astoMemoryOptimizations"); }
    public static boolean isAstoConcurrentModelBakingEnabled() { return getBoolean("astoConcurrentModelBaking"); }
    public static boolean isAstoJarDiscoveryCachingEnabled() { return getBoolean("astoJarDiscoveryCaching"); }
    public static boolean isAstoFastCollectionsEnabled() { return getBoolean("astoFastCollections"); }
    
    // Fluorine
    public static boolean isFluorineEnabled() { return getBoolean("fluorineEnabled"); }
    public static boolean isFluorineRenderOptimizationsEnabled() { return getBoolean("fluorineRenderOptimizations"); }
    public static boolean isFluorineMemoryOptimizationsEnabled() { return getBoolean("fluorineMemoryOptimizations"); }
    public static boolean isFluorineThreadOptimizationsEnabled() { return getBoolean("fluorineThreadOptimizations"); }
    
    // LegacyFix
    public static boolean isLegacyFixEnabled() { return getBoolean("legacyFixEnabled"); }
    public static boolean isLegacyCompatibilityModeEnabled() { return getBoolean("legacyCompatibilityMode"); }
    public static boolean isLegacyFixCrashPreventionEnabled() { return getBoolean("legacyFixCrashPrevention"); }
    public static boolean isLegacyFixMemoryLeaksEnabled() { return getBoolean("legacyFixMemoryLeaks"); }
    
    // SnowyASM
    public static boolean isSnowyASMEnabled() { return getBoolean("snowyASMEnabled"); }
    public static boolean isSnowyStringDeduplicationEnabled() { return getBoolean("snowyStringDeduplication"); }
    public static boolean isSnowyZeroAllocationPathsEnabled() { return getBoolean("snowyZeroAllocationPaths"); }
    public static boolean isSnowyLockFreeConcurrencyEnabled() { return getBoolean("snowyLockFreeConcurrency"); }
    public static boolean isSnowyMemoryReductionEnabled() { return getBoolean("snowyMemoryReduction"); }
    
    // JIT Optimization System
    public static boolean isJITEnabled() { return getBoolean("jitEnabled"); }
    public static boolean isJITHelperEnabled() { return getBoolean("jitHelperEnabled"); }
    public static boolean isJITInjectEnabled() { return getBoolean("jitInjectEnabled"); }
    public static boolean isJITUniversalPatcherEnabled() { return getBoolean("jitUniversalPatcherEnabled"); }
    public static boolean isJITAggressiveOptimizationEnabled() { return getBoolean("jitAggressiveOptimization"); }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 13: GENERIC GETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static boolean getBoolean(String key) {
        Object value = values.getOrDefault(key, defaults.get(key));
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }

    public static int getInt(String key) {
        Object value = values.getOrDefault(key, defaults.get(key));
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (Exception e) { return 0; }
    }

    public static long getLong(String key) {
        Object value = values.getOrDefault(key, defaults.get(key));
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (Exception e) { return 0L; }
    }

    public static double getDouble(String key) {
        Object value = values.getOrDefault(key, defaults.get(key));
        if (value instanceof Number) return ((Number) value).doubleValue();
        try { return Double.parseDouble(String.valueOf(value)); } catch (Exception e) { return 0.0; }
    }

    public static String getString(String key) {
        return String.valueOf(values.getOrDefault(key, defaults.getOrDefault(key, "")));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T getEnum(String key, Class<T> enumClass) {
        Object value = values.getOrDefault(key, defaults.get(key));
        if (enumClass.isInstance(value)) return (T) value;
        try { return Enum.valueOf(enumClass, String.valueOf(value).toUpperCase()); } 
        catch (Exception e) { return null; }
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 14: SETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    public static void set(String key, Object value) {
        if (!initialized.get()) initialize();
        values.put(key, value);
        notifyChange(key);
    }

    public static void setPreferredAPI(PreferredAPI api) {
        set("preferredAPI", api);
    }

    public static void setPreferredShaderEngine(ShaderEngine engine) {
        set("shaderEngine", engine);
    }

    public static void setPerformanceProfile(PerformanceProfile profile) {
        set("performanceProfile", profile);
        applyPerformanceProfile(profile);
    }

    public static void setLoggingLevel(LoggingLevel level) {
        set("loggingLevel", level);
    }

    /**
     * Apply a performance profile preset
     */
    private static void applyPerformanceProfile(PerformanceProfile profile) {
        switch (profile) {
            case POTATO -> {
                values.put("useVBO", true);
                values.put("useVAO", false);
                values.put("useInstancing", false);
                values.put("useDSA", false);
                values.put("usePersistentMapping", false);
                values.put("useComputeShaders", false);
                values.put("useSPIRV", false);
                values.put("batchSize", 64);
            }
            case LOW -> {
                values.put("useVBO", true);
                values.put("useVAO", true);
                values.put("useInstancing", false);
                values.put("useDSA", false);
                values.put("usePersistentMapping", false);
                values.put("useComputeShaders", false);
                values.put("useSPIRV", false);
                values.put("batchSize", 128);
            }
            case BALANCED -> {
                values.put("useVBO", true);
                values.put("useVAO", true);
                values.put("useInstancing", true);
                values.put("useDSA", false);
                values.put("usePersistentMapping", false);
                values.put("useComputeShaders", false);
                values.put("useSPIRV", false);
                values.put("batchSize", 256);
            }
            case HIGH -> {
                values.put("useVBO", true);
                values.put("useVAO", true);
                values.put("useInstancing", true);
                values.put("useDSA", true);
                values.put("usePersistentMapping", true);
                values.put("useComputeShaders", false);
                values.put("useSPIRV", false);
                values.put("batchSize", 512);
            }
            case ULTRA -> {
                values.put("useVBO", true);
                values.put("useVAO", true);
                values.put("useInstancing", true);
                values.put("useDSA", true);
                values.put("usePersistentMapping", true);
                values.put("useComputeShaders", true);
                values.put("useSPIRV", true);
                values.put("batchSize", 1024);
            }
            case CUSTOM -> {
                // Don't modify - user settings
            }
        }
        validateAgainstCapabilities();
    }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 15: CHANGE LISTENERS
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Register a change listener for a specific key
     */
    public static void addChangeListener(String key, Runnable listener) {
        changeListeners.put(key, listener);
    }

    /**
     * Remove a change listener
     */
    public static void removeChangeListener(String key) {
        changeListeners.remove(key);
    }

    /**
     * Notify listeners of a change
     */
    private static void notifyChange(String key) {
        Runnable listener = changeListeners.get(key);
        if (listener != null) {
            try {
                listener.run();
            } catch (Throwable t) {
                System.err.println("[Astralis Config] Listener error for " + key + ": " + t.getMessage());
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // ECS CONFIGURATION GETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    // Core Settings
    public static boolean isECSEnabled() { if (!initialized.get()) initialize(); return getBoolean("ecsEnabled"); }
    public static int getECSThreadCount() { if (!initialized.get()) initialize(); return getInt("ecsThreadCount"); }
    public static boolean isECSUseVirtualThreads() { if (!initialized.get()) initialize(); return getBoolean("ecsUseVirtualThreads"); }
    public static int getECSChunkSize() { if (!initialized.get()) initialize(); return getInt("ecsChunkSize"); }
    public static int getECSInitialCapacity() { if (!initialized.get()) initialize(); return getInt("ecsInitialCapacity"); }
    public static boolean isECSUseOffHeap() { if (!initialized.get()) initialize(); return getBoolean("ecsUseOffHeap"); }
    public static boolean isECSTrackChanges() { if (!initialized.get()) initialize(); return getBoolean("ecsTrackChanges"); }
    public static boolean isECSEnableGpu() { if (!initialized.get()) initialize(); return getBoolean("ecsEnableGpu"); }
    public static boolean isECSBuildEdgeGraph() { if (!initialized.get()) initialize(); return getBoolean("ecsBuildEdgeGraph"); }
    public static int getECSParallelThreshold() { if (!initialized.get()) initialize(); return getInt("ecsParallelThreshold"); }
    
    // Struct Flattening
    public static boolean isECSEnableStructFlattening() { if (!initialized.get()) initialize(); return getBoolean("ecsEnableStructFlattening"); }
    public static int getECSStructFlatteningSizeHint() { if (!initialized.get()) initialize(); return getInt("ecsStructFlatteningSizeHint"); }
    public static boolean isECSStructFlatteningUseMethodHandles() { if (!initialized.get()) initialize(); return getBoolean("ecsStructFlatteningUseMethodHandles"); }
    public static boolean isECSStructFlatteningUseLambdaMetafactory() { if (!initialized.get()) initialize(); return getBoolean("ecsStructFlatteningUseLambdaMetafactory"); }
    public static boolean isECSStructFlatteningCacheAccessors() { if (!initialized.get()) initialize(); return getBoolean("ecsStructFlatteningCacheAccessors"); }
    
    // Component Discovery
    public static boolean isECSAutoScanComponents() { if (!initialized.get()) initialize(); return getBoolean("ecsAutoScanComponents"); }
    public static String[] getECSComponentScanPackages() { 
        if (!initialized.get()) initialize(); 
        Object packages = values.getOrDefault("ecsComponentScanPackages", defaults.get("ecsComponentScanPackages"));
        return packages instanceof String[] ? (String[]) packages : new String[0];
    }
    public static boolean isECSComponentScanAtStartup() { if (!initialized.get()) initialize(); return getBoolean("ecsComponentScanAtStartup"); }
    public static boolean isECSComponentScanParallel() { if (!initialized.get()) initialize(); return getBoolean("ecsComponentScanParallel"); }
    public static boolean isECSComponentCacheResults() { if (!initialized.get()) initialize(); return getBoolean("ecsComponentCacheResults"); }
    
    // Dynamic Archetypes
    public static boolean isECSEnableDynamicArchetypes() { if (!initialized.get()) initialize(); return getBoolean("ecsEnableDynamicArchetypes"); }
    public static int getECSArchetypePoolSize() { if (!initialized.get()) initialize(); return getInt("ecsArchetypePoolSize"); }
    public static double getECSArchetypeGrowthFactor() { if (!initialized.get()) initialize(); return getDouble("ecsArchetypeGrowthFactor"); }
    public static int getECSArchetypeMigrationBatchSize() { if (!initialized.get()) initialize(); return getInt("ecsArchetypeMigrationBatchSize"); }
    public static boolean isECSArchetypeEdgeOptimization() { if (!initialized.get()) initialize(); return getBoolean("ecsArchetypeEdgeOptimization"); }
    public static boolean isECSArchetypeCompactionEnabled() { if (!initialized.get()) initialize(); return getBoolean("ecsArchetypeCompactionEnabled"); }
    public static double getECSArchetypeCompactionThreshold() { if (!initialized.get()) initialize(); return getDouble("ecsArchetypeCompactionThreshold"); }
    
    // System Scheduling
    public static boolean isECSUseTarjanScheduling() { if (!initialized.get()) initialize(); return getBoolean("ecsUseTarjanScheduling"); }
    public static boolean isECSDetectCycles() { if (!initialized.get()) initialize(); return getBoolean("ecsDetectCycles"); }
    public static boolean isECSThrowOnCycles() { if (!initialized.get()) initialize(); return getBoolean("ecsThrowOnCycles"); }
    public static boolean isECSUseDagOptimization() { if (!initialized.get()) initialize(); return getBoolean("ecsUseDagOptimization"); }
    public static int getECSMaxSystemDependencyDepth() { if (!initialized.get()) initialize(); return getInt("ecsMaxSystemDependencyDepth"); }
    public static String getECSSystemSchedulingStrategy() { if (!initialized.get()) initialize(); return getString("ecsSystemSchedulingStrategy"); }
    public static boolean isECSEnableWorkStealing() { if (!initialized.get()) initialize(); return getBoolean("ecsEnableWorkStealing"); }
    public static int getECSWorkStealingQueueSize() { if (!initialized.get()) initialize(); return getInt("ecsWorkStealingQueueSize"); }
    
    // Type-Safe Injection
    public static boolean isECSEnableDataInjection() { if (!initialized.get()) initialize(); return getBoolean("ecsEnableDataInjection"); }
    public static boolean isECSInjectAtRegistration() { if (!initialized.get()) initialize(); return getBoolean("ecsInjectAtRegistration"); }
    public static boolean isECSValidateInjectionTypes() { if (!initialized.get()) initialize(); return getBoolean("ecsValidateInjectionTypes"); }
    public static boolean isECSInjectReadOnlyArrays() { if (!initialized.get()) initialize(); return getBoolean("ecsInjectReadOnlyArrays"); }
    public static boolean isECSCacheInjectionHandles() { if (!initialized.get()) initialize(); return getBoolean("ecsCacheInjectionHandles"); }
    
    // Performance & Profiling
    public static boolean isECSEnableProfiler() { if (!initialized.get()) initialize(); return getBoolean("ecsEnableProfiler"); }
    public static boolean isECSEnableJFR() { if (!initialized.get()) initialize(); return getBoolean("ecsEnableJFR"); }
    public static int getECSProfilerSamplingInterval() { if (!initialized.get()) initialize(); return getInt("ecsProfilerSamplingInterval"); }
    public static int getECSProfilerRecordHistory() { if (!initialized.get()) initialize(); return getInt("ecsProfilerRecordHistory"); }
    public static boolean isECSEnableMemoryStats() { if (!initialized.get()) initialize(); return getBoolean("ecsEnableMemoryStats"); }
    public static boolean isECSEnableWorkloadEstimation() { if (!initialized.get()) initialize(); return getBoolean("ecsEnableWorkloadEstimation"); }
    public static double getECSWorkloadEstimatorEMA() { if (!initialized.get()) initialize(); return getDouble("ecsWorkloadEstimatorEMA"); }
    
    // Compatibility & Safety
    public static boolean isECSCompatibilityMode() { if (!initialized.get()) initialize(); return getBoolean("ecsCompatibilityMode"); }
    public static boolean isECSVanillaFallback() { if (!initialized.get()) initialize(); return getBoolean("ecsVanillaFallback"); }
    public static boolean isECSSafeMode() { if (!initialized.get()) initialize(); return getBoolean("ecsSafeMode"); }
    public static boolean isECSEnableAssertions() { if (!initialized.get()) initialize(); return getBoolean("ecsEnableAssertions"); }
    public static int getECSMaxEntityCount() { if (!initialized.get()) initialize(); return getInt("ecsMaxEntityCount"); }
    public static int getECSMaxComponentTypes() { if (!initialized.get()) initialize(); return getInt("ecsMaxComponentTypes"); }
    public static int getECSMaxSystemCount() { if (!initialized.get()) initialize(); return getInt("ecsMaxSystemCount"); }
    
    // Multi-Mod Ecosystem
    public static boolean isECSEnableModIsolation() { if (!initialized.get()) initialize(); return getBoolean("ecsEnableModIsolation"); }
    public static boolean isECSForgeEventIntegration() { if (!initialized.get()) initialize(); return getBoolean("ecsForgeEventIntegration"); }
    public static boolean isECSAllowModComponentRegistration() { if (!initialized.get()) initialize(); return getBoolean("ecsAllowModComponentRegistration"); }
    public static boolean isECSAllowModSystemRegistration() { if (!initialized.get()) initialize(); return getBoolean("ecsAllowModSystemRegistration"); }
    public static int getECSModRegistrationTimeout() { if (!initialized.get()) initialize(); return getInt("ecsModRegistrationTimeout"); }
    
    // Debug & Development
    public static boolean isECSDebugMode() { if (!initialized.get()) initialize(); return getBoolean("ecsDebugMode"); }
    public static boolean isECSDebugPrintArchetypes() { if (!initialized.get()) initialize(); return getBoolean("ecsDebugPrintArchetypes"); }
    public static boolean isECSDebugPrintDependencyGraph() { if (!initialized.get()) initialize(); return getBoolean("ecsDebugPrintDependencyGraph"); }
    public static boolean isECSDebugValidateEveryFrame() { if (!initialized.get()) initialize(); return getBoolean("ecsDebugValidateEveryFrame"); }
    public static boolean isECSDebugTrackAllocations() { if (!initialized.get()) initialize(); return getBoolean("ecsDebugTrackAllocations"); }
    public static int getECSDebugDumpStatsInterval() { if (!initialized.get()) initialize(); return getInt("ecsDebugDumpStatsInterval"); }
    
    // Minecraft-Specific
    public static boolean isECSMinecraftEntityOptimizer() { if (!initialized.get()) initialize(); return getBoolean("ecsMinecraftEntityOptimizer"); }
    public static boolean isECSMinecraftChunkStreamer() { if (!initialized.get()) initialize(); return getBoolean("ecsMinecraftChunkStreamer"); }
    public static boolean isECSMinecraftRedstoneOptimizer() { if (!initialized.get()) initialize(); return getBoolean("ecsMinecraftRedstoneOptimizer"); }
    public static boolean isECSMinecraftSpatialOptimizer() { if (!initialized.get()) initialize(); return getBoolean("ecsMinecraftSpatialOptimizer"); }
    public static boolean isECSMinecraftBridgeEnabled() { if (!initialized.get()) initialize(); return getBoolean("ecsMinecraftBridgeEnabled"); }
    public static boolean isECSMinecraftBatchEntityTick() { if (!initialized.get()) initialize(); return getBoolean("ecsMinecraftBatchEntityTick"); }
    public static boolean isECSMinecraftParallelEntityTick() { if (!initialized.get()) initialize(); return getBoolean("ecsMinecraftParallelEntityTick"); }
    public static int getECSMinecraftEntityTickBatchSize() { if (!initialized.get()) initialize(); return getInt("ecsMinecraftEntityTickBatchSize"); }
    
    // Bridge Systems
    public static boolean isBridgeCircuitBreakerEnabled() { if (!initialized.get()) initialize(); return getBoolean("bridgeCircuitBreakerEnabled"); }
    public static int getBridgeCircuitBreakerFailureThreshold() { if (!initialized.get()) initialize(); return getInt("bridgeCircuitBreakerFailureThreshold"); }
    public static long getBridgeCircuitBreakerResetTimeout() { if (!initialized.get()) initialize(); return (long) values.getOrDefault("bridgeCircuitBreakerResetTimeout", 5000L); }
    public static boolean isBridgeInterpolationEnabled() { if (!initialized.get()) initialize(); return getBoolean("bridgeInterpolationEnabled"); }
    public static String getBridgeInterpolationMode() { if (!initialized.get()) initialize(); return getString("bridgeInterpolationMode"); }
    public static boolean isBridgeSyncSystemsEnabled() { if (!initialized.get()) initialize(); return getBoolean("bridgeSyncSystemsEnabled"); }
    public static int getBridgeSyncBatchSize() { if (!initialized.get()) initialize(); return getInt("bridgeSyncBatchSize"); }
    
    // Culling Manager
    public static boolean isCullingManagerEnabled() { if (!initialized.get()) initialize(); return getBoolean("cullingManagerEnabled"); }
    public static String getCullingTier() { if (!initialized.get()) initialize(); return getString("cullingTier"); }
    public static boolean isCullingEnableFrustum() { if (!initialized.get()) initialize(); return getBoolean("cullingEnableFrustum"); }
    public static boolean isCullingEnableOcclusion() { if (!initialized.get()) initialize(); return getBoolean("cullingEnableOcclusion"); }
    public static boolean isCullingEnableDistance() { if (!initialized.get()) initialize(); return getBoolean("cullingEnableDistance"); }
    public static double getCullingMaxDistance() { if (!initialized.get()) initialize(); return getDouble("cullingMaxDistance"); }
    public static int getCullingOcclusionQueryBatchSize() { if (!initialized.get()) initialize(); return getInt("cullingOcclusionQueryBatchSize"); }
    public static boolean isCullingUseHiZ() { if (!initialized.get()) initialize(); return getBoolean("cullingUseHiZ"); }
    
    // Indirect Draw Manager
    public static boolean isIndirectDrawEnabled() { if (!initialized.get()) initialize(); return getBoolean("indirectDrawEnabled"); }
    public static int getIndirectDrawMaxDrawCalls() { if (!initialized.get()) initialize(); return getInt("indirectDrawMaxDrawCalls"); }
    public static String getIndirectDrawBatchingStrategy() { if (!initialized.get()) initialize(); return getString("indirectDrawBatchingStrategy"); }
    public static boolean isIndirectDrawEnableMultiDraw() { if (!initialized.get()) initialize(); return getBoolean("indirectDrawEnableMultiDraw"); }
    public static int getIndirectDrawCommandBufferSizeMB() { if (!initialized.get()) initialize(); return getInt("indirectDrawCommandBufferSizeMB"); }
    
    // Draw Pool
    public static boolean isDrawPoolEnabled() { if (!initialized.get()) initialize(); return getBoolean("drawPoolEnabled"); }
    public static int getDrawPoolMaxDrawCalls() { if (!initialized.get()) initialize(); return getInt("drawPoolMaxDrawCalls"); }
    public static int getDrawPoolMaxClusters() { if (!initialized.get()) initialize(); return getInt("drawPoolMaxClusters"); }
    public static String getDrawPoolClusteringStrategy() { if (!initialized.get()) initialize(); return getString("drawPoolClusteringStrategy"); }
    
    // Resolution Manager Mode
    public static String getResolutionManagerMode() { 
        if (!initialized.get()) initialize(); 
        return values.getOrDefault("resolutionManagerMode", "ADAPTIVE").toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // RENDER ENGINE CONFIGURATION GETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    // Render Graph
    public static boolean isRenderGraphEnabled() { if (!initialized.get()) initialize(); return getBoolean("renderGraphEnabled"); }
    public static int getRenderGraphMaxFramesInFlight() { if (!initialized.get()) initialize(); return getInt("renderGraphMaxFramesInFlight"); }
    public static int getRenderGraphCommandBufferPoolSize() { if (!initialized.get()) initialize(); return getInt("renderGraphCommandBufferPoolSize"); }
    public static boolean isRenderGraphEnablePassCulling() { if (!initialized.get()) initialize(); return getBoolean("renderGraphEnablePassCulling"); }
    public static boolean isRenderGraphEnablePassMerging() { if (!initialized.get()) initialize(); return getBoolean("renderGraphEnablePassMerging"); }
    public static boolean isRenderGraphEnableMemoryAliasing() { if (!initialized.get()) initialize(); return getBoolean("renderGraphEnableMemoryAliasing"); }
    public static boolean isRenderGraphEnableParallelRecording() { if (!initialized.get()) initialize(); return getBoolean("renderGraphEnableParallelRecording"); }
    public static boolean isRenderGraphEnableSplitBarriers() { if (!initialized.get()) initialize(); return getBoolean("renderGraphEnableSplitBarriers"); }
    public static boolean isRenderGraphEnableGPUProfiling() { if (!initialized.get()) initialize(); return getBoolean("renderGraphEnableGPUProfiling"); }
    public static int getRenderGraphParallelRecordingThreshold() { if (!initialized.get()) initialize(); return getInt("renderGraphParallelRecordingThreshold"); }
    public static boolean isRenderGraphAutoCompile() { if (!initialized.get()) initialize(); return getBoolean("renderGraphAutoCompile"); }
    public static boolean isRenderGraphValidateEveryFrame() { if (!initialized.get()) initialize(); return getBoolean("renderGraphValidateEveryFrame"); }
    
    // Render System
    public static boolean isRenderSystemEnabled() { if (!initialized.get()) initialize(); return getBoolean("renderSystemEnabled"); }
    public static boolean isRenderSystemUseECS() { if (!initialized.get()) initialize(); return getBoolean("renderSystemUseECS"); }
    public static int getRenderSystemBatchSize() { if (!initialized.get()) initialize(); return getInt("renderSystemBatchSize"); }
    public static boolean isRenderSystemEnableInstancing() { if (!initialized.get()) initialize(); return getBoolean("renderSystemEnableInstancing"); }
    public static boolean isRenderSystemEnableIndirectDraw() { if (!initialized.get()) initialize(); return getBoolean("renderSystemEnableIndirectDraw"); }
    public static boolean isRenderSystemEnableGPUCulling() { if (!initialized.get()) initialize(); return getBoolean("renderSystemEnableGPUCulling"); }
    public static boolean isRenderSystemEnableOcclusionCulling() { if (!initialized.get()) initialize(); return getBoolean("renderSystemEnableOcclusionCulling"); }
    public static boolean isRenderSystemEnableFrustumCulling() { if (!initialized.get()) initialize(); return getBoolean("renderSystemEnableFrustumCulling"); }
    public static int getRenderSystemCullingBatchSize() { if (!initialized.get()) initialize(); return getInt("renderSystemCullingBatchSize"); }
    public static int getRenderSystemMaxDrawCalls() { if (!initialized.get()) initialize(); return getInt("renderSystemMaxDrawCalls"); }
    public static boolean isRenderSystemEnableSortByMaterial() { if (!initialized.get()) initialize(); return getBoolean("renderSystemEnableSortByMaterial"); }
    public static boolean isRenderSystemEnableSortByDepth() { if (!initialized.get()) initialize(); return getBoolean("renderSystemEnableSortByDepth"); }
    public static boolean isRenderSystemEnableStateCache() { if (!initialized.get()) initialize(); return getBoolean("renderSystemEnableStateCache"); }
    public static int getRenderSystemStateCacheSize() { if (!initialized.get()) initialize(); return getInt("renderSystemStateCacheSize"); }
    
    // Render Pipeline
    public static boolean isRenderPipelineEnableVBO() { if (!initialized.get()) initialize(); return getBoolean("renderPipelineEnableVBO"); }
    public static boolean isRenderPipelineEnableVAO() { if (!initialized.get()) initialize(); return getBoolean("renderPipelineEnableVAO"); }
    public static boolean isRenderPipelineEnableUBO() { if (!initialized.get()) initialize(); return getBoolean("renderPipelineEnableUBO"); }
    public static boolean isRenderPipelineEnableSSBO() { if (!initialized.get()) initialize(); return getBoolean("renderPipelineEnableSSBO"); }
    public static boolean isRenderPipelineEnableDSA() { if (!initialized.get()) initialize(); return getBoolean("renderPipelineEnableDSA"); }
    public static boolean isRenderPipelineEnablePersistentMapping() { if (!initialized.get()) initialize(); return getBoolean("renderPipelineEnablePersistentMapping"); }
    public static int getRenderPipelineBufferSizeMB() { if (!initialized.get()) initialize(); return getInt("renderPipelineBufferSizeMB"); }
    public static int getRenderPipelineUniformBufferSize() { if (!initialized.get()) initialize(); return getInt("renderPipelineUniformBufferSize"); }
    public static int getRenderPipelineVertexBufferSize() { if (!initialized.get()) initialize(); return getInt("renderPipelineVertexBufferSize"); }
    public static int getRenderPipelineIndexBufferSize() { if (!initialized.get()) initialize(); return getInt("renderPipelineIndexBufferSize"); }
    
    // Render State
    public static boolean isRenderStateEnableDepthTest() { if (!initialized.get()) initialize(); return getBoolean("renderStateEnableDepthTest"); }
    public static boolean isRenderStateEnableBlending() { if (!initialized.get()) initialize(); return getBoolean("renderStateEnableBlending"); }
    public static boolean isRenderStateEnableCulling() { if (!initialized.get()) initialize(); return getBoolean("renderStateEnableCulling"); }
    public static String getRenderStateDepthFunc() { if (!initialized.get()) initialize(); return getString("renderStateDepthFunc"); }
    public static String getRenderStateBlendSrc() { if (!initialized.get()) initialize(); return getString("renderStateBlendSrc"); }
    public static String getRenderStateBlendDst() { if (!initialized.get()) initialize(); return getString("renderStateBlendDst"); }
    public static String getRenderStateCullFace() { if (!initialized.get()) initialize(); return getString("renderStateCullFace"); }
    public static String getRenderStateFrontFace() { if (!initialized.get()) initialize(); return getString("renderStateFrontFace"); }
    public static boolean isRenderStateEnableDepthWrite() { if (!initialized.get()) initialize(); return getBoolean("renderStateEnableDepthWrite"); }
    public static boolean isRenderStateEnableColorWrite() { if (!initialized.get()) initialize(); return getBoolean("renderStateEnableColorWrite"); }
    public static boolean isRenderStateEnableStencilTest() { if (!initialized.get()) initialize(); return getBoolean("renderStateEnableStencilTest"); }
    
    // Resolution Manager
    public static boolean isResolutionManagerEnabled() { if (!initialized.get()) initialize(); return getBoolean("resolutionManagerEnabled"); }
    public static boolean isResolutionManagerDynamicScaling() { if (!initialized.get()) initialize(); return getBoolean("resolutionManagerDynamicScaling"); }
    public static double getResolutionManagerMinScale() { if (!initialized.get()) initialize(); return getDouble("resolutionManagerMinScale"); }
    public static double getResolutionManagerMaxScale() { if (!initialized.get()) initialize(); return getDouble("resolutionManagerMaxScale"); }
    public static double getResolutionManagerTargetFrameTime() { if (!initialized.get()) initialize(); return getDouble("resolutionManagerTargetFrameTime"); }
    public static double getResolutionManagerScaleStep() { if (!initialized.get()) initialize(); return getDouble("resolutionManagerScaleStep"); }
    public static int getResolutionManagerAdaptiveInterval() { if (!initialized.get()) initialize(); return getInt("resolutionManagerAdaptiveInterval"); }
    public static boolean isResolutionManagerEnableTAA() { if (!initialized.get()) initialize(); return getBoolean("resolutionManagerEnableTAA"); }
    public static boolean isResolutionManagerEnableFSR() { if (!initialized.get()) initialize(); return getBoolean("resolutionManagerEnableFSR"); }
    public static boolean isResolutionManagerEnableDLSS() { if (!initialized.get()) initialize(); return getBoolean("resolutionManagerEnableDLSS"); }
    
    // Render Bridge
    public static boolean isRenderBridgeEnabled() { if (!initialized.get()) initialize(); return getBoolean("renderBridgeEnabled"); }
    public static boolean isRenderBridgeMinecraftCompatibility() { if (!initialized.get()) initialize(); return getBoolean("renderBridgeMinecraftCompatibility"); }
    public static boolean isRenderBridgeEnableChunkRendering() { if (!initialized.get()) initialize(); return getBoolean("renderBridgeEnableChunkRendering"); }
    public static boolean isRenderBridgeEnableEntityRendering() { if (!initialized.get()) initialize(); return getBoolean("renderBridgeEnableEntityRendering"); }
    public static boolean isRenderBridgeEnableTileEntityRendering() { if (!initialized.get()) initialize(); return getBoolean("renderBridgeEnableTileEntityRendering"); }
    public static boolean isRenderBridgeEnableParticleRendering() { if (!initialized.get()) initialize(); return getBoolean("renderBridgeEnableParticleRendering"); }
    public static int getRenderBridgeChunkBatchSize() { if (!initialized.get()) initialize(); return getInt("renderBridgeChunkBatchSize"); }
    public static int getRenderBridgeEntityBatchSize() { if (!initialized.get()) initialize(); return getInt("renderBridgeEntityBatchSize"); }
    
    // Advanced Rendering Features
    public static boolean isRenderEnableHDR() { if (!initialized.get()) initialize(); return getBoolean("renderEnableHDR"); }
    public static boolean isRenderEnableBloom() { if (!initialized.get()) initialize(); return getBoolean("renderEnableBloom"); }
    public static boolean isRenderEnableSSAO() { if (!initialized.get()) initialize(); return getBoolean("renderEnableSSAO"); }
    public static boolean isRenderEnableSSR() { if (!initialized.get()) initialize(); return getBoolean("renderEnableSSR"); }
    public static boolean isRenderEnableShadows() { if (!initialized.get()) initialize(); return getBoolean("renderEnableShadows"); }
    public static int getRenderShadowMapSize() { if (!initialized.get()) initialize(); return getInt("renderShadowMapSize"); }
    public static int getRenderShadowCascades() { if (!initialized.get()) initialize(); return getInt("renderShadowCascades"); }
    public static boolean isRenderEnableCSM() { if (!initialized.get()) initialize(); return getBoolean("renderEnableCSM"); }
    public static boolean isRenderEnablePBR() { if (!initialized.get()) initialize(); return getBoolean("renderEnablePBR"); }
    public static boolean isRenderEnableIBL() { if (!initialized.get()) initialize(); return getBoolean("renderEnableIBL"); }
    
    // Render Performance & Profiling
    public static boolean isRenderEnableProfiler() { if (!initialized.get()) initialize(); return getBoolean("renderEnableProfiler"); }
    public static boolean isRenderEnableGPUTimestamps() { if (!initialized.get()) initialize(); return getBoolean("renderEnableGPUTimestamps"); }
    public static boolean isRenderEnableDrawCallCounter() { if (!initialized.get()) initialize(); return getBoolean("renderEnableDrawCallCounter"); }
    public static boolean isRenderEnableStateChangeCounter() { if (!initialized.get()) initialize(); return getBoolean("renderEnableStateChangeCounter"); }
    public static int getRenderPrintStatsInterval() { if (!initialized.get()) initialize(); return getInt("renderPrintStatsInterval"); }
    public static double getRenderMaxFrameTime() { if (!initialized.get()) initialize(); return getDouble("renderMaxFrameTime"); }
    
    // ═══════════════════════════════════════════════════════════════════════════════════
    // GPU BACKEND INTEGRATION CONFIGURATION GETTERS
    // ═══════════════════════════════════════════════════════════════════════════════════
    
    // Backend Selection
    public static String getGPUBackendPreferred() { if (!initialized.get()) initialize(); return getString("gpuBackendPreferred"); }
    public static boolean isGPUBackendAllowFallback() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendAllowFallback"); }
    public static String[] getGPUBackendFallbackChain() {
        if (!initialized.get()) initialize();
        Object chain = values.getOrDefault("gpuBackendFallbackChain", defaults.get("gpuBackendFallbackChain"));
        return chain instanceof String[] ? (String[]) chain : new String[0];
    }
    public static boolean isGPUBackendRequireMinimumVersion() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendRequireMinimumVersion"); }
    public static boolean isGPUBackendValidateCapabilities() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendValidateCapabilities"); }
    
    // Platform-Specific Preferences
    public static boolean isGPUBackendWindowsPreferDX() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendWindowsPreferDX"); }
    public static boolean isGPUBackendMacOSPreferMetal() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendMacOSPreferMetal"); }
    public static boolean isGPUBackendLinuxPreferVulkan() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendLinuxPreferVulkan"); }
    public static boolean isGPUBackendAndroidPreferVulkan() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendAndroidPreferVulkan"); }
    
    // Feature Requirements
    public static boolean isGPUBackendRequireComputeShaders() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendRequireComputeShaders"); }
    public static boolean isGPUBackendRequireGeometryShaders() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendRequireGeometryShaders"); }
    public static boolean isGPUBackendRequireTessellation() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendRequireTessellation"); }
    public static boolean isGPUBackendRequireMeshShaders() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendRequireMeshShaders"); }
    public static boolean isGPUBackendRequireRayTracing() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendRequireRayTracing"); }
    public static boolean isGPUBackendRequireBindlessTextures() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendRequireBindlessTextures"); }
    public static boolean isGPUBackendRequireMultiDrawIndirect() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendRequireMultiDrawIndirect"); }
    
    // Capability Overrides
    public static boolean isGPUBackendOverrideVersion() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendOverrideVersion"); }
    public static int getGPUBackendForcedVersionMajor() { if (!initialized.get()) initialize(); return getInt("gpuBackendForcedVersionMajor"); }
    public static int getGPUBackendForcedVersionMinor() { if (!initialized.get()) initialize(); return getInt("gpuBackendForcedVersionMinor"); }
    public static boolean isGPUBackendDisableExtensions() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendDisableExtensions"); }
    public static String[] getGPUBackendDisabledExtensionList() {
        if (!initialized.get()) initialize();
        Object list = values.getOrDefault("gpuBackendDisabledExtensionList", defaults.get("gpuBackendDisabledExtensionList"));
        return list instanceof String[] ? (String[]) list : new String[0];
    }
    
    // Backend-Specific Settings
    public static boolean isGPUBackendVulkanValidation() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendVulkanValidation"); }
    public static boolean isGPUBackendVulkanDebugUtils() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendVulkanDebugUtils"); }
    public static boolean isGPUBackendMetalValidation() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendMetalValidation"); }
    public static boolean isGPUBackendDirectXDebugLayer() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendDirectXDebugLayer"); }
    public static boolean isGPUBackendOpenGLDebugContext() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendOpenGLDebugContext"); }
    
    // Hot-Reload & Development
    public static boolean isGPUBackendEnableHotReload() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendEnableHotReload"); }
    public static boolean isGPUBackendAutoDetectChanges() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendAutoDetectChanges"); }
    public static boolean isGPUBackendReloadOnDriverUpdate() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendReloadOnDriverUpdate"); }
    
    // Diagnostics
    public static boolean isGPUBackendEnableProfiling() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendEnableProfiling"); }
    public static boolean isGPUBackendLogSelectionProcess() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendLogSelectionProcess"); }
    public static boolean isGPUBackendLogCapabilities() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendLogCapabilities"); }
    public static boolean isGPUBackendDumpFullReport() { if (!initialized.get()) initialize(); return getBoolean("gpuBackendDumpFullReport"); }

    // ═══════════════════════════════════════════════════════════════════════════════════
    // SECTION 16: UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════════════

    /**
     * Reset all settings to defaults
     */
    public static void resetToDefaults() {
        values.clear();
        values.putAll(defaults);
        validateAgainstCapabilities();
        save();
    }

    /**
     * Get all current values as a map
     */
    public static Map<String, Object> getAllValues() {
        return new LinkedHashMap<>(values);
    }

    /**
     * Check if a key exists
     */
    public static boolean hasKey(String key) {
        return values.containsKey(key) || defaults.containsKey(key);
    }

    /**
     * Get config file path
     */
    public static Path getConfigPath() {
        if (!initialized.get()) initialize();
        return configPath;
    }

    /**
     * Reload configuration from file
     */
    public static void reload() {
        load();
        validateAgainstCapabilities();
    }
}
// finish
