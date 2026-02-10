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
 *   <li>Performance Thresholds</li>
 *   <li>Compatibility Modes</li>
 *   <li>Logging Settings</li>
 * </ul>
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

        // DirectX Version Limits (9-12.2 supported)
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
        defaults.put("hlslMinShaderModel", "5_0"); // Minimum shader model (fallback)
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
