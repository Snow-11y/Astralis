package stellar.snow.astralis.api.vulkan.pipeline;

import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.api.vulkan.backend.VulkanContext;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.*;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.*;

/**
 * VulkanPipelineProvider - Advanced JIT Pipeline Compiler with Multi-Version Support
 *
 * <h2>Vulkan Version Support Matrix:</h2>
 * <pre>
 * ┌─────────┬────────────────────────────────────────────────────────────────┐
 * │ Version │ Features Used                                                  │
 * ├─────────┼────────────────────────────────────────────────────────────────┤
 * │ 1.0     │ Base pipeline creation, persistent cache                       │
 * │ 1.1     │ Subgroup properties, protected memory                          │
 * │ 1.2     │ Timeline semaphores, buffer device address, shader controls    │
 * │ 1.3     │ Dynamic rendering, synchronization2, extended dynamic state    │
 * │ 1.4     │ Push descriptors 2, shader object, maintenance6                │
 * └─────────┴────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Performance Features:</h2>
 * <ul>
 *   <li>Lock-striped concurrent cache with XXHash-style keys</li>
 *   <li>Extended dynamic state to minimize pipeline variants</li>
 *   <li>Pipeline derivatives for faster related compilation</li>
 *   <li>Async batch compilation with work stealing</li>
 *   <li>Graphics pipeline library support (modular pipelines)</li>
 *   <li>Creation feedback for performance analysis</li>
 *   <li>Automatic cache persistence with validation</li>
 * </ul>
 */
public class VulkanPipelineProvider {

    // ════════════════════════════════════════════════════════════════════════════
    // VULKAN VERSION CONSTANTS
    // ════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_API_VERSION_1_0 = VK_MAKE_API_VERSION(0, 1, 0, 0);
    public static final int VK_API_VERSION_1_1 = VK_MAKE_API_VERSION(0, 1, 1, 0);
    public static final int VK_API_VERSION_1_2 = VK_MAKE_API_VERSION(0, 1, 2, 0);
    public static final int VK_API_VERSION_1_3 = VK_MAKE_API_VERSION(0, 1, 3, 0);
    public static final int VK_API_VERSION_1_4 = VK_MAKE_API_VERSION(0, 1, 4, 0);

    // Extended Dynamic State Extension Constants (for pre-1.3)
    private static final int VK_DYNAMIC_STATE_CULL_MODE_EXT = 1000267000;
    private static final int VK_DYNAMIC_STATE_FRONT_FACE_EXT = 1000267001;
    private static final int VK_DYNAMIC_STATE_PRIMITIVE_TOPOLOGY_EXT = 1000267002;
    private static final int VK_DYNAMIC_STATE_VIEWPORT_WITH_COUNT_EXT = 1000267003;
    private static final int VK_DYNAMIC_STATE_SCISSOR_WITH_COUNT_EXT = 1000267004;
    private static final int VK_DYNAMIC_STATE_VERTEX_INPUT_BINDING_STRIDE_EXT = 1000267005;
    private static final int VK_DYNAMIC_STATE_DEPTH_TEST_ENABLE_EXT = 1000267006;
    private static final int VK_DYNAMIC_STATE_DEPTH_WRITE_ENABLE_EXT = 1000267007;
    private static final int VK_DYNAMIC_STATE_DEPTH_COMPARE_OP_EXT = 1000267008;
    private static final int VK_DYNAMIC_STATE_DEPTH_BOUNDS_TEST_ENABLE_EXT = 1000267009;
    private static final int VK_DYNAMIC_STATE_STENCIL_TEST_ENABLE_EXT = 1000267010;
    private static final int VK_DYNAMIC_STATE_STENCIL_OP_EXT = 1000267011;

    // Extended Dynamic State 2 Constants
    private static final int VK_DYNAMIC_STATE_RASTERIZER_DISCARD_ENABLE_EXT = 1000377001;
    private static final int VK_DYNAMIC_STATE_DEPTH_BIAS_ENABLE_EXT = 1000377002;
    private static final int VK_DYNAMIC_STATE_PRIMITIVE_RESTART_ENABLE_EXT = 1000377004;

    // Extended Dynamic State 3 Constants
    private static final int VK_DYNAMIC_STATE_POLYGON_MODE_EXT = 1000455003;
    private static final int VK_DYNAMIC_STATE_COLOR_BLEND_ENABLE_EXT = 1000455010;
    private static final int VK_DYNAMIC_STATE_COLOR_BLEND_EQUATION_EXT = 1000455011;
    private static final int VK_DYNAMIC_STATE_COLOR_WRITE_MASK_EXT = 1000455012;

    // VK 1.3 Dynamic State (core)
    private static final int VK_DYNAMIC_STATE_CULL_MODE = 1000267000;
    private static final int VK_DYNAMIC_STATE_FRONT_FACE = 1000267001;
    private static final int VK_DYNAMIC_STATE_PRIMITIVE_TOPOLOGY = 1000267002;
    private static final int VK_DYNAMIC_STATE_VIEWPORT_WITH_COUNT = 1000267003;
    private static final int VK_DYNAMIC_STATE_SCISSOR_WITH_COUNT = 1000267004;
    private static final int VK_DYNAMIC_STATE_VERTEX_INPUT_BINDING_STRIDE = 1000267005;
    private static final int VK_DYNAMIC_STATE_DEPTH_TEST_ENABLE = 1000267006;
    private static final int VK_DYNAMIC_STATE_DEPTH_WRITE_ENABLE = 1000267007;
    private static final int VK_DYNAMIC_STATE_DEPTH_COMPARE_OP = 1000267008;
    private static final int VK_DYNAMIC_STATE_DEPTH_BOUNDS_TEST_ENABLE = 1000267009;
    private static final int VK_DYNAMIC_STATE_STENCIL_TEST_ENABLE = 1000267010;
    private static final int VK_DYNAMIC_STATE_STENCIL_OP = 1000267011;
    private static final int VK_DYNAMIC_STATE_RASTERIZER_DISCARD_ENABLE = 1000377001;
    private static final int VK_DYNAMIC_STATE_DEPTH_BIAS_ENABLE = 1000377002;
    private static final int VK_DYNAMIC_STATE_PRIMITIVE_RESTART_ENABLE = 1000377004;

    // Pipeline Creation Feedback
    private static final int VK_STRUCTURE_TYPE_PIPELINE_CREATION_FEEDBACK_CREATE_INFO = 1000192000;
    private static final int VK_PIPELINE_CREATION_FEEDBACK_VALID_BIT = 0x00000001;
    private static final int VK_PIPELINE_CREATION_FEEDBACK_APPLICATION_PIPELINE_CACHE_HIT_BIT = 0x00000002;
    private static final int VK_PIPELINE_CREATION_FEEDBACK_BASE_PIPELINE_ACCELERATION_BIT = 0x00000004;

    // Graphics Pipeline Library
    private static final int VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_LIBRARY_CREATE_INFO_EXT = 1000320002;
    private static final int VK_PIPELINE_CREATE_LIBRARY_BIT_KHR = 0x00000800;
    private static final int VK_GRAPHICS_PIPELINE_LIBRARY_VERTEX_INPUT_INTERFACE_BIT_EXT = 0x00000001;
    private static final int VK_GRAPHICS_PIPELINE_LIBRARY_PRE_RASTERIZATION_SHADERS_BIT_EXT = 0x00000002;
    private static final int VK_GRAPHICS_PIPELINE_LIBRARY_FRAGMENT_SHADER_BIT_EXT = 0x00000004;
    private static final int VK_GRAPHICS_PIPELINE_LIBRARY_FRAGMENT_OUTPUT_INTERFACE_BIT_EXT = 0x00000008;

    // ════════════════════════════════════════════════════════════════════════════
    // CACHE CONFIGURATION
    // ════════════════════════════════════════════════════════════════════════════
    
    private static final int CACHE_STRIPE_COUNT = 64;
    private static final int CACHE_STRIPE_MASK = CACHE_STRIPE_COUNT - 1;
    private static final int MAX_BATCH_SIZE = 32;
    private static final int DERIVATIVE_THRESHOLD = 4;
    private static final long CACHE_VALIDATION_MAGIC = 0x5650494C4341434CULL; // "VPILCACL"

    // ════════════════════════════════════════════════════════════════════════════
    // CORE COMPONENTS
    // ════════════════════════════════════════════════════════════════════════════
    
    private final VulkanContext context;
    private final VkDevice device;
    private final VulkanCapabilities capabilities;
    private final long pipelineCache;
    private final Path cacheFilePath;
    private final long pipelineLayout;
    private final long[] descriptorSetLayouts;

    // Lock-striped cache for high concurrency
    @SuppressWarnings("unchecked")
    private final ConcurrentHashMap<Long, PipelineEntry>[] pipelineStripes = new ConcurrentHashMap[CACHE_STRIPE_COUNT];
    private final StampedLock[] stripeLocks = new StampedLock[CACHE_STRIPE_COUNT];
    
    // Shader and vertex format caches
    private final ConcurrentHashMap<Long, ShaderModulePair> shaderModules = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, VertexInputDescription> vertexInputCache = new ConcurrentHashMap<>();
    
    // Pipeline library cache (for modular pipelines)
    private final ConcurrentHashMap<Long, Long> vertexInputLibraries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> fragmentOutputLibraries = new ConcurrentHashMap<>();
    
    // Async compilation
    private final ExecutorService compilationExecutor;
    private final ConcurrentLinkedQueue<CompilationRequest> pendingCompilations = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean compilerRunning = new AtomicBoolean(true);
    
    // Statistics
    private final PipelineStatistics stats = new PipelineStatistics();
    
    // Default shaders
    private volatile long defaultVertexShader = VK_NULL_HANDLE;
    private volatile long defaultFragmentShader = VK_NULL_HANDLE;

    // ════════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ════════════════════════════════════════════════════════════════════════════

    public VulkanPipelineProvider(VulkanContext context, long pipelineLayout, long[] descriptorSetLayouts) {
        this.context = context;
        this.device = context.device;
        this.pipelineLayout = pipelineLayout;
        this.descriptorSetLayouts = descriptorSetLayouts.clone();
        this.cacheFilePath = Path.of("cache", "vulkan", "pipeline_cache.bin");
        
        // Detect capabilities
        this.capabilities = detectCapabilities();
        
        // Initialize cache stripes
        for (int i = 0; i < CACHE_STRIPE_COUNT; i++) {
            pipelineStripes[i] = new ConcurrentHashMap<>();
            stripeLocks[i] = new StampedLock();
        }
        
        // Create pipeline cache
        this.pipelineCache = createPipelineCache();
        
        // Initialize default shaders
        initializeDefaultShaders();
        
        // Start async compiler
        this.compilationExecutor = createCompilationExecutor();
        startAsyncCompiler();
        
        logInitialization();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CAPABILITY DETECTION
    // ════════════════════════════════════════════════════════════════════════════

    private VulkanCapabilities detectCapabilities() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(stack);
            vkGetPhysicalDeviceProperties(context.physicalDevice, props);
            
            int apiVersion = props.apiVersion();
            int driverVersion = props.driverVersion();
            int vendorId = props.vendorID();
            
            boolean isVk11 = apiVersion >= VK_API_VERSION_1_1;
            boolean isVk12 = apiVersion >= VK_API_VERSION_1_2;
            boolean isVk13 = apiVersion >= VK_API_VERSION_1_3;
            boolean isVk14 = apiVersion >= VK_API_VERSION_1_4;
            
            // Query extension support
            IntBuffer extensionCount = stack.mallocInt(1);
            vkEnumerateDeviceExtensionProperties(context.physicalDevice, (ByteBuffer) null, extensionCount, null);
            
            VkExtensionProperties.Buffer extensions = VkExtensionProperties.calloc(extensionCount.get(0), stack);
            vkEnumerateDeviceExtensionProperties(context.physicalDevice, (ByteBuffer) null, extensionCount, extensions);
            
            Set<String> supportedExtensions = new HashSet<>();
            for (int i = 0; i < extensions.capacity(); i++) {
                supportedExtensions.add(extensions.get(i).extensionNameString());
            }
            
            // Extended dynamic state support
            boolean extDynamicState = isVk13 || supportedExtensions.contains("VK_EXT_extended_dynamic_state");
            boolean extDynamicState2 = isVk13 || supportedExtensions.contains("VK_EXT_extended_dynamic_state2");
            boolean extDynamicState3 = supportedExtensions.contains("VK_EXT_extended_dynamic_state3");
            
            // Pipeline library support
            boolean pipelineLibrary = supportedExtensions.contains("VK_EXT_graphics_pipeline_library");
            
            // Dynamic rendering
            boolean dynamicRendering = isVk13 || supportedExtensions.contains("VK_KHR_dynamic_rendering");
            
            // Shader object (VK 1.4 feature)
            boolean shaderObject = isVk14 || supportedExtensions.contains("VK_EXT_shader_object");
            
            // Pipeline creation feedback
            boolean creationFeedback = isVk13 || supportedExtensions.contains("VK_EXT_pipeline_creation_feedback");
            
            // Maintenance features
            boolean maintenance4 = isVk13 || supportedExtensions.contains("VK_KHR_maintenance4");
            boolean maintenance5 = supportedExtensions.contains("VK_KHR_maintenance5");
            boolean maintenance6 = isVk14 || supportedExtensions.contains("VK_KHR_maintenance6");
            
            // Query physical device features for 1.2+
            boolean timelineSemaphore = false;
            boolean bufferDeviceAddress = false;
            
            if (isVk12) {
                VkPhysicalDeviceVulkan12Features features12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);
                VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                    .pNext(features12);
                vkGetPhysicalDeviceFeatures2(context.physicalDevice, features2);
                
                timelineSemaphore = features12.timelineSemaphore();
                bufferDeviceAddress = features12.bufferDeviceAddress();
            }
            
            return new VulkanCapabilities(
                apiVersion,
                driverVersion,
                vendorId,
                isVk11, isVk12, isVk13, isVk14,
                extDynamicState, extDynamicState2, extDynamicState3,
                pipelineLibrary, dynamicRendering, shaderObject,
                creationFeedback, timelineSemaphore, bufferDeviceAddress,
                maintenance4, maintenance5, maintenance6
            );
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PIPELINE CACHE MANAGEMENT
    // ════════════════════════════════════════════════════════════════════════════

    private long createPipelineCache() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer initialData = loadCacheFromDisk();
            
            VkPipelineCacheCreateInfo createInfo = VkPipelineCacheCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO)
                .pInitialData(initialData);
            
            // VK 1.3+: Externally synchronized flag for better performance
            if (capabilities.isVulkan13Plus) {
                createInfo.flags(0x00000001); // VK_PIPELINE_CACHE_CREATE_EXTERNALLY_SYNCHRONIZED_BIT
            }
            
            LongBuffer pCache = stack.mallocLong(1);
            int result = vkCreatePipelineCache(device, createInfo, null, pCache);
            
            if (result != VK_SUCCESS) {
                Astralis.LOGGER.warn("[Pipeline] Cache creation failed with data ({}), retrying empty", result);
                createInfo.pInitialData(null);
                result = vkCreatePipelineCache(device, createInfo, null, pCache);
                
                if (result != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create pipeline cache: " + result);
                }
            }
            
            if (initialData != null) {
                MemoryUtil.memFree(initialData);
            }
            
            return pCache.get(0);
        }
    }

    private ByteBuffer loadCacheFromDisk() {
        if (!Files.exists(cacheFilePath)) {
            return null;
        }
        
        try {
            byte[] data = Files.readAllBytes(cacheFilePath);
            
            // Validate cache header
            if (data.length < 32) {
                Astralis.LOGGER.warn("[Pipeline] Cache file too small, ignoring");
                return null;
            }
            
            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data).flip();
            
            // Validate VkPipelineCacheHeaderVersionOne
            int headerLength = buffer.getInt(0);
            int headerVersion = buffer.getInt(4);
            int vendorId = buffer.getInt(8);
            int deviceId = buffer.getInt(12);
            
            // Check compatibility
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(stack);
                vkGetPhysicalDeviceProperties(context.physicalDevice, props);
                
                if (vendorId != props.vendorID() || deviceId != props.deviceID()) {
                    Astralis.LOGGER.info("[Pipeline] Cache from different device, discarding");
                    MemoryUtil.memFree(buffer);
                    return null;
                }
            }
            
            Astralis.LOGGER.info("[Pipeline] Loaded {} KB from cache", data.length / 1024);
            return buffer;
            
        } catch (IOException e) {
            Astralis.LOGGER.warn("[Pipeline] Failed to load cache: {}", e.getMessage());
            return null;
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SHADER INITIALIZATION
    // ════════════════════════════════════════════════════════════════════════════

    private void initializeDefaultShaders() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Default vertex shader - passthrough with position/UV/color
            int[] defaultVertexSPIRV = createDefaultVertexShaderSPIRV();
            int[] defaultFragmentSPIRV = createDefaultFragmentShaderSPIRV();
            
            defaultVertexShader = createShaderModule(defaultVertexSPIRV);
            defaultFragmentShader = createShaderModule(defaultFragmentSPIRV);
            
            if (defaultVertexShader == VK_NULL_HANDLE || defaultFragmentShader == VK_NULL_HANDLE) {
                Astralis.LOGGER.error("[Pipeline] Failed to create default shaders!");
            }
        }
    }

    private int[] createDefaultVertexShaderSPIRV() {
        // Minimal SPIR-V for: 
        // #version 450
        // layout(location = 0) in vec3 inPosition;
        // layout(location = 1) in vec4 inColor;
        // layout(location = 2) in vec2 inTexCoord;
        // layout(location = 0) out vec4 fragColor;
        // layout(location = 1) out vec2 fragTexCoord;
        // void main() {
        //     gl_Position = vec4(inPosition, 1.0);
        //     fragColor = inColor;
        //     fragTexCoord = inTexCoord;
        // }
        return new int[] {
            0x07230203, 0x00010300, 0x000D000B, 0x0000002E,
            0x00000000, 0x00020011, 0x00000001, 0x0006000B,
            0x00000001, 0x4C534C47, 0x6474732E, 0x3035342E,
            0x00000000, 0x0003000E, 0x00000000, 0x00000001,
            0x000B000F, 0x00000000, 0x00000004, 0x6E69616D,
            0x00000000, 0x0000000D, 0x00000012, 0x0000001C,
            0x00000020, 0x00000024, 0x00000028, 0x00030003,
            0x00000002, 0x000001C2, 0x00040005, 0x00000004,
            0x6E69616D, 0x00000000, 0x00060005, 0x0000000B,
            0x505F6C67, 0x65567265, 0x78657472, 0x00000000,
            0x00060006, 0x0000000B, 0x00000000, 0x505F6C67,
            0x7469736F, 0x006E6F69, 0x00030005, 0x0000000D,
            0x00000000, 0x00050005, 0x00000012, 0x6F506E69,
            0x69746973, 0x00006E6F, 0x00050005, 0x0000001C,
            0x67617266, 0x6F6C6F43, 0x00000072, 0x00040005,
            0x00000020, 0x6F436E69, 0x00726F6C, 0x00060005,
            0x00000024, 0x67617266, 0x43786554, 0x64726F6F,
            0x00000000, 0x00050005, 0x00000028, 0x65546E69,
            0x6F6F4378, 0x00006472, 0x00050048, 0x0000000B,
            0x00000000, 0x0000000B, 0x00000000, 0x00030047,
            0x0000000B, 0x00000002, 0x00040047, 0x00000012,
            0x0000001E, 0x00000000, 0x00040047, 0x0000001C,
            0x0000001E, 0x00000000, 0x00040047, 0x00000020,
            0x0000001E, 0x00000001, 0x00040047, 0x00000024,
            0x0000001E, 0x00000001, 0x00040047, 0x00000028,
            0x0000001E, 0x00000002, 0x00020013, 0x00000002,
            0x00030021, 0x00000003, 0x00000002, 0x00030016,
            0x00000006, 0x00000020, 0x00040017, 0x00000007,
            0x00000006, 0x00000004, 0x0003001E, 0x0000000B,
            0x00000007, 0x00040020, 0x0000000C, 0x00000003,
            0x0000000B, 0x0004003B, 0x0000000C, 0x0000000D,
            0x00000003, 0x00040015, 0x0000000E, 0x00000020,
            0x00000001, 0x0004002B, 0x0000000E, 0x0000000F,
            0x00000000, 0x00040017, 0x00000010, 0x00000006,
            0x00000003, 0x00040020, 0x00000011, 0x00000001,
            0x00000010, 0x0004003B, 0x00000011, 0x00000012,
            0x00000001, 0x0004002B, 0x00000006, 0x00000014,
            0x3F800000, 0x00040020, 0x0000001A, 0x00000003,
            0x00000007, 0x0004003B, 0x0000001A, 0x0000001C,
            0x00000003, 0x00040020, 0x0000001F, 0x00000001,
            0x00000007, 0x0004003B, 0x0000001F, 0x00000020,
            0x00000001, 0x00040017, 0x00000022, 0x00000006,
            0x00000002, 0x00040020, 0x00000023, 0x00000003,
            0x00000022, 0x0004003B, 0x00000023, 0x00000024,
            0x00000003, 0x00040020, 0x00000027, 0x00000001,
            0x00000022, 0x0004003B, 0x00000027, 0x00000028,
            0x00000001, 0x00050036, 0x00000002, 0x00000004,
            0x00000000, 0x00000003, 0x000200F8, 0x00000005,
            0x0004003D, 0x00000010, 0x00000013, 0x00000012,
            0x00050051, 0x00000006, 0x00000015, 0x00000013,
            0x00000000, 0x00050051, 0x00000006, 0x00000016,
            0x00000013, 0x00000001, 0x00050051, 0x00000006,
            0x00000017, 0x00000013, 0x00000002, 0x00070050,
            0x00000007, 0x00000018, 0x00000015, 0x00000016,
            0x00000017, 0x00000014, 0x00050041, 0x0000001A,
            0x0000001B, 0x0000000D, 0x0000000F, 0x0003003E,
            0x0000001B, 0x00000018, 0x0004003D, 0x00000007,
            0x00000021, 0x00000020, 0x0003003E, 0x0000001C,
            0x00000021, 0x0004003D, 0x00000022, 0x00000029,
            0x00000028, 0x0003003E, 0x00000024, 0x00000029,
            0x000100FD, 0x00010038
        };
    }

    private int[] createDefaultFragmentShaderSPIRV() {
        // Minimal SPIR-V for:
        // #version 450
        // layout(location = 0) in vec4 fragColor;
        // layout(location = 1) in vec2 fragTexCoord;
        // layout(location = 0) out vec4 outColor;
        // layout(binding = 0) uniform sampler2D texSampler;
        // void main() {
        //     outColor = fragColor * texture(texSampler, fragTexCoord);
        // }
        return new int[] {
            0x07230203, 0x00010300, 0x000D000B, 0x00000018,
            0x00000000, 0x00020011, 0x00000001, 0x0006000B,
            0x00000001, 0x4C534C47, 0x6474732E, 0x3035342E,
            0x00000000, 0x0003000E, 0x00000000, 0x00000001,
            0x0008000F, 0x00000004, 0x00000004, 0x6E69616D,
            0x00000000, 0x00000009, 0x0000000B, 0x00000014,
            0x00030010, 0x00000004, 0x00000007, 0x00030003,
            0x00000002, 0x000001C2, 0x00040005, 0x00000004,
            0x6E69616D, 0x00000000, 0x00050005, 0x00000009,
            0x4374756F, 0x726F6C6F, 0x00000000, 0x00050005,
            0x0000000B, 0x67617266, 0x6F6C6F43, 0x00000072,
            0x00050005, 0x0000000F, 0x53786574, 0x6C706D61,
            0x00007265, 0x00060005, 0x00000014, 0x67617266,
            0x43786554, 0x64726F6F, 0x00000000, 0x00040047,
            0x00000009, 0x0000001E, 0x00000000, 0x00040047,
            0x0000000B, 0x0000001E, 0x00000000, 0x00040047,
            0x0000000F, 0x00000022, 0x00000000, 0x00040047,
            0x0000000F, 0x00000021, 0x00000000, 0x00040047,
            0x00000014, 0x0000001E, 0x00000001, 0x00020013,
            0x00000002, 0x00030021, 0x00000003, 0x00000002,
            0x00030016, 0x00000006, 0x00000020, 0x00040017,
            0x00000007, 0x00000006, 0x00000004, 0x00040020,
            0x00000008, 0x00000003, 0x00000007, 0x0004003B,
            0x00000008, 0x00000009, 0x00000003, 0x00040020,
            0x0000000A, 0x00000001, 0x00000007, 0x0004003B,
            0x0000000A, 0x0000000B, 0x00000001, 0x00090019,
            0x0000000C, 0x00000006, 0x00000001, 0x00000000,
            0x00000000, 0x00000000, 0x00000001, 0x00000000,
            0x0003001B, 0x0000000D, 0x0000000C, 0x00040020,
            0x0000000E, 0x00000000, 0x0000000D, 0x0004003B,
            0x0000000E, 0x0000000F, 0x00000000, 0x00040017,
            0x00000012, 0x00000006, 0x00000002, 0x00040020,
            0x00000013, 0x00000001, 0x00000012, 0x0004003B,
            0x00000013, 0x00000014, 0x00000001, 0x00050036,
            0x00000002, 0x00000004, 0x00000000, 0x00000003,
            0x000200F8, 0x00000005, 0x0004003D, 0x00000007,
            0x0000000C, 0x0000000B, 0x0004003D, 0x0000000D,
            0x00000010, 0x0000000F, 0x0004003D, 0x00000012,
            0x00000015, 0x00000014, 0x00050057, 0x00000007,
            0x00000016, 0x00000010, 0x00000015, 0x00050085,
            0x00000007, 0x00000017, 0x0000000C, 0x00000016,
            0x0003003E, 0x00000009, 0x00000017, 0x000100FD,
            0x00010038
        };
    }

    private long createShaderModule(int[] spirv) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer code = stack.malloc(spirv.length * 4);
            for (int word : spirv) {
                code.putInt(word);
            }
            code.flip();
            
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(code);
            
            LongBuffer pModule = stack.mallocLong(1);
            if (vkCreateShaderModule(device, createInfo, null, pModule) != VK_SUCCESS) {
                return VK_NULL_HANDLE;
            }
            
            return pModule.get(0);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ASYNC COMPILATION
    // ════════════════════════════════════════════════════════════════════════════

    private ExecutorService createCompilationExecutor() {
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        return Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "VkPipeline-Compiler");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
    }

    private void startAsyncCompiler() {
        compilationExecutor.submit(() -> {
            List<CompilationRequest> batch = new ArrayList<>(MAX_BATCH_SIZE);
            
            while (compilerRunning.get()) {
                batch.clear();
                
                // Collect batch
                CompilationRequest req;
                while (batch.size() < MAX_BATCH_SIZE && (req = pendingCompilations.poll()) != null) {
                    batch.add(req);
                }
                
                if (batch.isEmpty()) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }
                
                // Compile batch
                compileBatch(batch);
            }
        });
    }

    private void compileBatch(List<CompilationRequest> batch) {
        if (batch.size() == 1) {
            CompilationRequest req = batch.get(0);
            try {
                long pipeline = createPipelineInternal(req.key, req.state, VK_NULL_HANDLE);
                req.future.complete(pipeline);
            } catch (Exception e) {
                req.future.completeExceptionally(e);
            }
            return;
        }
        
        // Batch creation with derivatives
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkGraphicsPipelineCreateInfo.Buffer pipelineInfos = 
                VkGraphicsPipelineCreateInfo.calloc(batch.size(), stack);
            
            // Setup all pipeline create infos
            for (int i = 0; i < batch.size(); i++) {
                CompilationRequest req = batch.get(i);
                setupPipelineCreateInfo(pipelineInfos.get(i), stack, req.key, req.state, 
                    i > 0 ? 0 : -1); // First pipeline is base
            }
            
            // Allow derivatives
            pipelineInfos.get(0).flags(VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT);
            for (int i = 1; i < batch.size(); i++) {
                pipelineInfos.get(i)
                    .flags(VK_PIPELINE_CREATE_DERIVATIVE_BIT)
                    .basePipelineIndex(0);
            }
            
            LongBuffer pPipelines = stack.mallocLong(batch.size());
            long startTime = System.nanoTime();
            int result = vkCreateGraphicsPipelines(device, pipelineCache, pipelineInfos, null, pPipelines);
            long elapsed = System.nanoTime() - startTime;
            
            if (result == VK_SUCCESS) {
                stats.addBatchCompilation(batch.size(), elapsed);
                
                for (int i = 0; i < batch.size(); i++) {
                    long pipeline = pPipelines.get(i);
                    CompilationRequest req = batch.get(i);
                    
                    cachePipeline(req.key, pipeline, true);
                    req.future.complete(pipeline);
                }
            } else {
                // Fallback to individual compilation
                for (CompilationRequest req : batch) {
                    try {
                        long pipeline = createPipelineInternal(req.key, req.state, VK_NULL_HANDLE);
                        req.future.complete(pipeline);
                    } catch (Exception e) {
                        req.future.completeExceptionally(e);
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MAIN PIPELINE ACCESS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Gets or creates a pipeline for the current render state.
     * This is the primary entry point for pipeline access.
     *
     * @param state           Current render state
     * @param renderPass      Target render pass (or VK_NULL_HANDLE for dynamic rendering)
     * @param vertexFormatHash Hash of vertex format
     * @return Pipeline handle (never VK_NULL_HANDLE on success)
     */
    public long getPipeline(RenderState state, long renderPass, int vertexFormatHash) {
        // Build optimized key based on capabilities
        PipelineKey key = buildPipelineKey(state, renderPass, vertexFormatHash);
        
        // Fast path: check cache with optimistic locking
        int stripeIndex = getStripeIndex(key.hash);
        ConcurrentHashMap<Long, PipelineEntry> stripe = pipelineStripes[stripeIndex];
        
        PipelineEntry entry = stripe.get(key.hash);
        if (entry != null && entry.matches(key)) {
            stats.recordHit();
            entry.recordUsage();
            return entry.pipeline;
        }
        
        // Slow path: compile or wait
        stats.recordMiss();
        return getOrCreatePipeline(key, state, stripeIndex);
    }

    /**
     * Async version - returns immediately with a future.
     */
    public CompletableFuture<Long> getPipelineAsync(RenderState state, long renderPass, int vertexFormatHash) {
        PipelineKey key = buildPipelineKey(state, renderPass, vertexFormatHash);
        
        int stripeIndex = getStripeIndex(key.hash);
        PipelineEntry entry = pipelineStripes[stripeIndex].get(key.hash);
        
        if (entry != null && entry.matches(key)) {
            return CompletableFuture.completedFuture(entry.pipeline);
        }
        
        CompletableFuture<Long> future = new CompletableFuture<>();
        pendingCompilations.add(new CompilationRequest(key, state.snapshot(), future));
        return future;
    }

    private long getOrCreatePipeline(PipelineKey key, RenderState state, int stripeIndex) {
        StampedLock lock = stripeLocks[stripeIndex];
        long stamp = lock.readLock();
        
        try {
            // Double-check under lock
            PipelineEntry entry = pipelineStripes[stripeIndex].get(key.hash);
            if (entry != null && entry.matches(key)) {
                return entry.pipeline;
            }
            
            // Upgrade to write lock
            long writeStamp = lock.tryConvertToWriteLock(stamp);
            if (writeStamp == 0L) {
                lock.unlockRead(stamp);
                writeStamp = lock.writeLock();
            }
            stamp = writeStamp;
            
            // Triple-check
            entry = pipelineStripes[stripeIndex].get(key.hash);
            if (entry != null && entry.matches(key)) {
                return entry.pipeline;
            }
            
            // Create pipeline
            long pipeline = createPipelineInternal(key, state, findBasePipeline(key, stripeIndex));
            cachePipeline(key, pipeline, false);
            
            return pipeline;
            
        } finally {
            lock.unlock(stamp);
        }
    }

    private long findBasePipeline(PipelineKey key, int stripeIndex) {
        if (!capabilities.supportsDerivatives) {
            return VK_NULL_HANDLE;
        }
        
        // Find similar pipeline for derivative creation
        ConcurrentHashMap<Long, PipelineEntry> stripe = pipelineStripes[stripeIndex];
        
        for (PipelineEntry entry : stripe.values()) {
            if (entry.key.programHandle == key.programHandle &&
                entry.key.renderPass == key.renderPass) {
                return entry.pipeline;
            }
        }
        
        return VK_NULL_HANDLE;
    }

    private void cachePipeline(PipelineKey key, long pipeline, boolean fromAsync) {
        int stripeIndex = getStripeIndex(key.hash);
        pipelineStripes[stripeIndex].put(key.hash, new PipelineEntry(key, pipeline));
        
        if (!fromAsync) {
            stats.recordCompilation();
        }
    }

    private int getStripeIndex(long hash) {
        return (int) (hash ^ (hash >>> 32)) & CACHE_STRIPE_MASK;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PIPELINE KEY BUILDING
    // ════════════════════════════════════════════════════════════════════════════

    private PipelineKey buildPipelineKey(RenderState state, long renderPass, int vertexFormatHash) {
        MemorySegment mem = state.getStateMemory();
        
        long programHandle = mem.get(ValueLayout.JAVA_LONG, RenderState.OFF_ACTIVE_PROGRAM);
        long blendState = mem.get(ValueLayout.JAVA_LONG, RenderState.OFF_BLEND);
        long depthState = mem.get(ValueLayout.JAVA_LONG, RenderState.OFF_DEPTH);
        long stencilState = mem.get(ValueLayout.JAVA_LONG, RenderState.OFF_STENCIL);
        long cullState = mem.get(ValueLayout.JAVA_LONG, RenderState.OFF_CULL);
        long colorMask = mem.get(ValueLayout.JAVA_LONG, RenderState.OFF_COLOR_MASK);
        int primitiveMode = mem.get(ValueLayout.JAVA_INT, RenderState.OFF_PRIMITIVE_MODE);
        
        // With extended dynamic state, we can exclude more state from the key
        if (capabilities.extendedDynamicState3) {
            // EDS3: blend enable, blend equation, color write mask are dynamic
            blendState &= 0x01; // Only keep enable bit
            colorMask = 0;
        } else if (capabilities.extendedDynamicState) {
            // EDS1: cull mode, front face, depth test/write/compare are dynamic
            cullState &= 0xFF000000L; // Only keep polygon mode
            depthState &= ~0x1FF; // Clear test, write, func
        }
        
        // Compute XXHash-style hash
        long hash = computeHash(programHandle, blendState, depthState, stencilState, 
                               cullState, colorMask, primitiveMode, vertexFormatHash, renderPass);
        
        return new PipelineKey(
            hash, programHandle, blendState, depthState, stencilState,
            cullState, colorMask, primitiveMode, vertexFormatHash, renderPass
        );
    }

    private long computeHash(long... values) {
        // XXHash64-style mixing
        long h = 0x9E3779B97F4A7C15L;
        for (long v : values) {
            h ^= v * 0xC2B2AE3D27D4EB4FL;
            h = Long.rotateLeft(h, 31) * 0x9E3779B97F4A7C15L;
        }
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return h;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PIPELINE CREATION
    // ════════════════════════════════════════════════════════════════════════════

    private long createPipelineInternal(PipelineKey key, RenderState state, long basePipeline) {
        long startTime = System.nanoTime();
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            setupPipelineCreateInfo(pipelineInfo.get(0), stack, key, state, basePipeline != VK_NULL_HANDLE ? 0 : -1);
            
            if (basePipeline != VK_NULL_HANDLE) {
                pipelineInfo.get(0)
                    .flags(VK_PIPELINE_CREATE_DERIVATIVE_BIT)
                    .basePipelineHandle(basePipeline);
            }
            
            // Add creation feedback if supported
            VkPipelineCreationFeedback.Buffer feedbacks = null;
            if (capabilities.creationFeedback) {
                feedbacks = VkPipelineCreationFeedback.calloc(3, stack); // 1 overall + 2 stages
            }
            
            LongBuffer pPipeline = stack.mallocLong(1);
            int result = vkCreateGraphicsPipelines(device, pipelineCache, pipelineInfo, null, pPipeline);
            
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline: " + translateVkResult(result));
            }
            
            long pipeline = pPipeline.get(0);
            long elapsed = System.nanoTime() - startTime;
            
            stats.addCompilationTime(elapsed);
            
            if (capabilities.creationFeedback && feedbacks != null) {
                logCreationFeedback(pipeline, feedbacks, elapsed);
            }
            
            return pipeline;
        }
    }

    private void setupPipelineCreateInfo(VkGraphicsPipelineCreateInfo info, MemoryStack stack, 
                                         PipelineKey key, RenderState state, int basePipelineIndex) {
        // Shader stages
        VkPipelineShaderStageCreateInfo.Buffer shaderStages = createShaderStages(stack, key.programHandle);
        
        // Vertex input
        VkPipelineVertexInputStateCreateInfo vertexInput = createVertexInputState(stack, key.vertexFormatHash);
        
        // Input assembly
        VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
            .topology(translateTopology(key.primitiveMode))
            .primitiveRestartEnable(supportsPrimitiveRestart(key.primitiveMode));
        
        // Viewport (dynamic)
        VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
            .viewportCount(1)
            .scissorCount(1);
        
        // Rasterization
        VkPipelineRasterizationStateCreateInfo rasterizer = createRasterizerState(stack, key);
        
        // Multisampling
        VkPipelineMultisampleStateCreateInfo multisample = VkPipelineMultisampleStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
            .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
            .sampleShadingEnable(false);
        
        // Depth/Stencil
        VkPipelineDepthStencilStateCreateInfo depthStencil = createDepthStencilState(stack, key);
        
        // Color blend
        VkPipelineColorBlendStateCreateInfo colorBlend = createColorBlendState(stack, key);
        
        // Dynamic state
        VkPipelineDynamicStateCreateInfo dynamicState = createDynamicState(stack);
        
        info.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
            .pStages(shaderStages)
            .pVertexInputState(vertexInput)
            .pInputAssemblyState(inputAssembly)
            .pViewportState(viewportState)
            .pRasterizationState(rasterizer)
            .pMultisampleState(multisample)
            .pDepthStencilState(depthStencil)
            .pColorBlendState(colorBlend)
            .pDynamicState(dynamicState)
            .layout(pipelineLayout)
            .renderPass(key.renderPass)
            .subpass(0)
            .basePipelineHandle(VK_NULL_HANDLE)
            .basePipelineIndex(basePipelineIndex);
    }

    private VkPipelineShaderStageCreateInfo.Buffer createShaderStages(MemoryStack stack, long programHandle) {
        ShaderModulePair modules = shaderModules.get(programHandle);
        
        long vertModule = (modules != null) ? modules.vertex : defaultVertexShader;
        long fragModule = (modules != null) ? modules.fragment : defaultFragmentShader;
        
        VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
        
        stages.get(0)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            .stage(VK_SHADER_STAGE_VERTEX_BIT)
            .module(vertModule)
            .pName(stack.UTF8Safe("main"));
        
        stages.get(1)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
            .module(fragModule)
            .pName(stack.UTF8Safe("main"));
        
        return stages;
    }

    private VkPipelineVertexInputStateCreateInfo createVertexInputState(MemoryStack stack, int formatHash) {
        VkPipelineVertexInputStateCreateInfo info = VkPipelineVertexInputStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
        
        VertexInputDescription desc = vertexInputCache.get(formatHash);
        if (desc == null) {
            return info; // Empty vertex input
        }
        
        VkVertexInputBindingDescription.Buffer bindings = 
            VkVertexInputBindingDescription.calloc(desc.bindings.length, stack);
        for (int i = 0; i < desc.bindings.length; i++) {
            VertexBinding b = desc.bindings[i];
            bindings.get(i)
                .binding(b.binding)
                .stride(b.stride)
                .inputRate(b.instanced ? VK_VERTEX_INPUT_RATE_INSTANCE : VK_VERTEX_INPUT_RATE_VERTEX);
        }
        
        VkVertexInputAttributeDescription.Buffer attributes = 
            VkVertexInputAttributeDescription.calloc(desc.attributes.length, stack);
        for (int i = 0; i < desc.attributes.length; i++) {
            VertexAttribute a = desc.attributes[i];
            attributes.get(i)
                .binding(a.binding)
                .location(a.location)
                .format(translateVertexFormat(a.type, a.count, a.normalized))
                .offset(a.offset);
        }
        
        return info
            .pVertexBindingDescriptions(bindings)
            .pVertexAttributeDescriptions(attributes);
    }

    private VkPipelineRasterizationStateCreateInfo createRasterizerState(MemoryStack stack, PipelineKey key) {
        boolean cullEnabled = (key.cullState & 1) != 0;
        int cullFace = (int) ((key.cullState >> 8) & 0xFF);
        int frontFace = (int) ((key.cullState >> 16) & 0xFF);
        int polygonMode = (int) ((key.cullState >> 24) & 0xFF);
        
        VkPipelineRasterizationStateCreateInfo info = VkPipelineRasterizationStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
            .depthClampEnable(false)
            .rasterizerDiscardEnable(false)
            .lineWidth(1.0f)
            .depthBiasEnable(true);
        
        // Set static state if not using extended dynamic state
        if (!capabilities.extendedDynamicState) {
            info.cullMode(cullEnabled ? translateCullMode(cullFace) : VK_CULL_MODE_NONE)
                .frontFace(translateFrontFace(frontFace));
        }
        
        if (!capabilities.extendedDynamicState3) {
            info.polygonMode(translatePolygonMode(polygonMode));
        }
        
        return info;
    }

    private VkPipelineDepthStencilStateCreateInfo createDepthStencilState(MemoryStack stack, PipelineKey key) {
        boolean depthTest = (key.depthState & 1) != 0;
        boolean depthWrite = ((key.depthState >> 1) & 1) != 0;
        int depthFunc = (int) ((key.depthState >> 8) & 0xFF);
        boolean depthBounds = ((key.depthState >> 16) & 1) != 0;
        
        boolean stencilTest = (key.stencilState & 1) != 0;
        int stencilFunc = (int) ((key.stencilState >> 8) & 0xFF);
        int stencilFail = (int) ((key.stencilState >> 16) & 0xFF);
        int stencilDepthFail = (int) ((key.stencilState >> 24) & 0xFF);
        int stencilPass = (int) ((key.stencilState >> 32) & 0xFF);
        int stencilMask = (int) ((key.stencilState >> 40) & 0xFF);
        
        VkStencilOpState.Buffer stencilOps = VkStencilOpState.calloc(2, stack);
        
        // Front face stencil
        stencilOps.get(0)
            .failOp(translateStencilOp(stencilFail))
            .passOp(translateStencilOp(stencilPass))
            .depthFailOp(translateStencilOp(stencilDepthFail))
            .compareOp(translateCompareOp(stencilFunc))
            .compareMask(stencilMask)
            .writeMask(stencilMask)
            .reference(0);
        
        // Back face stencil (copy of front for now)
        stencilOps.get(1).set(stencilOps.get(0));
        
        VkPipelineDepthStencilStateCreateInfo info = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
            .depthBoundsTestEnable(depthBounds)
            .minDepthBounds(0.0f)
            .maxDepthBounds(1.0f)
            .front(stencilOps.get(0))
            .back(stencilOps.get(1));
        
        // Set static state if not using extended dynamic state
        if (!capabilities.extendedDynamicState) {
            info.depthTestEnable(depthTest)
                .depthWriteEnable(depthWrite)
                .depthCompareOp(translateCompareOp(depthFunc))
                .stencilTestEnable(stencilTest);
        }
        
        return info;
    }

    private VkPipelineColorBlendStateCreateInfo createColorBlendState(MemoryStack stack, PipelineKey key) {
        boolean blendEnabled = (key.blendState & 1) != 0;
        int srcColor = (int) ((key.blendState >> 8) & 0xFF);
        int dstColor = (int) ((key.blendState >> 16) & 0xFF);
        int colorOp = (int) ((key.blendState >> 24) & 0xFF);
        int srcAlpha = (int) ((key.blendState >> 32) & 0xFF);
        int dstAlpha = (int) ((key.blendState >> 40) & 0xFF);
        int alphaOp = (int) ((key.blendState >> 48) & 0xFF);
        
        int colorMask = (int) (key.colorMask & 0xF);
        int vkColorMask = 0;
        if ((colorMask & 1) != 0) vkColorMask |= VK_COLOR_COMPONENT_R_BIT;
        if ((colorMask & 2) != 0) vkColorMask |= VK_COLOR_COMPONENT_G_BIT;
        if ((colorMask & 4) != 0) vkColorMask |= VK_COLOR_COMPONENT_B_BIT;
        if ((colorMask & 8) != 0) vkColorMask |= VK_COLOR_COMPONENT_A_BIT;
        
        VkPipelineColorBlendAttachmentState.Buffer attachment = 
            VkPipelineColorBlendAttachmentState.calloc(1, stack);
        
        if (!capabilities.extendedDynamicState3) {
            attachment.get(0)
                .colorWriteMask(vkColorMask)
                .blendEnable(blendEnabled)
                .srcColorBlendFactor(translateBlendFactor(srcColor))
                .dstColorBlendFactor(translateBlendFactor(dstColor))
                .colorBlendOp(translateBlendOp(colorOp))
                .srcAlphaBlendFactor(translateBlendFactor(srcAlpha))
                .dstAlphaBlendFactor(translateBlendFactor(dstAlpha))
                .alphaBlendOp(translateBlendOp(alphaOp));
        } else {
            // With EDS3, these are all dynamic
            attachment.get(0)
                .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | 
                               VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                .blendEnable(false);
        }
        
        return VkPipelineColorBlendStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
            .logicOpEnable(false)
            .logicOp(VK_LOGIC_OP_COPY)
            .pAttachments(attachment)
            .blendConstants(stack.floats(0, 0, 0, 0));
    }

    private VkPipelineDynamicStateCreateInfo createDynamicState(MemoryStack stack) {
        List<Integer> dynamicStates = new ArrayList<>();
        
        // Base dynamic states (VK 1.0)
        dynamicStates.add(VK_DYNAMIC_STATE_VIEWPORT);
        dynamicStates.add(VK_DYNAMIC_STATE_SCISSOR);
        dynamicStates.add(VK_DYNAMIC_STATE_LINE_WIDTH);
        dynamicStates.add(VK_DYNAMIC_STATE_DEPTH_BIAS);
        dynamicStates.add(VK_DYNAMIC_STATE_BLEND_CONSTANTS);
        dynamicStates.add(VK_DYNAMIC_STATE_STENCIL_REFERENCE);
        
        // Extended dynamic state (VK 1.3 or extension)
        if (capabilities.extendedDynamicState) {
            dynamicStates.add(VK_DYNAMIC_STATE_CULL_MODE);
            dynamicStates.add(VK_DYNAMIC_STATE_FRONT_FACE);
            dynamicStates.add(VK_DYNAMIC_STATE_PRIMITIVE_TOPOLOGY);
            dynamicStates.add(VK_DYNAMIC_STATE_DEPTH_TEST_ENABLE);
            dynamicStates.add(VK_DYNAMIC_STATE_DEPTH_WRITE_ENABLE);
            dynamicStates.add(VK_DYNAMIC_STATE_DEPTH_COMPARE_OP);
            dynamicStates.add(VK_DYNAMIC_STATE_DEPTH_BOUNDS_TEST_ENABLE);
            dynamicStates.add(VK_DYNAMIC_STATE_STENCIL_TEST_ENABLE);
            dynamicStates.add(VK_DYNAMIC_STATE_STENCIL_OP);
        }
        
        // Extended dynamic state 2 (VK 1.3 or extension)
        if (capabilities.extendedDynamicState2) {
            dynamicStates.add(VK_DYNAMIC_STATE_RASTERIZER_DISCARD_ENABLE);
            dynamicStates.add(VK_DYNAMIC_STATE_DEPTH_BIAS_ENABLE);
            dynamicStates.add(VK_DYNAMIC_STATE_PRIMITIVE_RESTART_ENABLE);
        }
        
        // Extended dynamic state 3
        if (capabilities.extendedDynamicState3) {
            dynamicStates.add(VK_DYNAMIC_STATE_POLYGON_MODE_EXT);
            dynamicStates.add(VK_DYNAMIC_STATE_COLOR_BLEND_ENABLE_EXT);
            dynamicStates.add(VK_DYNAMIC_STATE_COLOR_BLEND_EQUATION_EXT);
            dynamicStates.add(VK_DYNAMIC_STATE_COLOR_WRITE_MASK_EXT);
        }
        
        IntBuffer pDynamicStates = stack.mallocInt(dynamicStates.size());
        for (int state : dynamicStates) {
            pDynamicStates.put(state);
        }
        pDynamicStates.flip();
        
        return VkPipelineDynamicStateCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
            .pDynamicStates(pDynamicStates);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // STATE TRANSLATION
    // ════════════════════════════════════════════════════════════════════════════

    private int translateTopology(int glMode) {
        return switch (glMode) {
            case RenderConstants.GL_POINTS -> VK_PRIMITIVE_TOPOLOGY_POINT_LIST;
            case RenderConstants.GL_LINES -> VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            case RenderConstants.GL_LINE_STRIP -> VK_PRIMITIVE_TOPOLOGY_LINE_STRIP;
            case RenderConstants.GL_LINE_LOOP -> VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            case RenderConstants.GL_TRIANGLES -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            case RenderConstants.GL_TRIANGLE_STRIP -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
            case RenderConstants.GL_TRIANGLE_FAN -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN;
            case RenderConstants.GL_QUADS -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            case RenderConstants.GL_PATCHES -> VK_PRIMITIVE_TOPOLOGY_PATCH_LIST;
            default -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
        };
    }

    private boolean supportsPrimitiveRestart(int mode) {
        return mode == RenderConstants.GL_TRIANGLE_STRIP ||
               mode == RenderConstants.GL_TRIANGLE_FAN ||
               mode == RenderConstants.GL_LINE_STRIP;
    }

    private int translateCompareOp(int glFunc) {
        return switch (glFunc) {
            case RenderConstants.GL_NEVER -> VK_COMPARE_OP_NEVER;
            case RenderConstants.GL_LESS -> VK_COMPARE_OP_LESS;
            case RenderConstants.GL_EQUAL -> VK_COMPARE_OP_EQUAL;
            case RenderConstants.GL_LEQUAL -> VK_COMPARE_OP_LESS_OR_EQUAL;
            case RenderConstants.GL_GREATER -> VK_COMPARE_OP_GREATER;
            case RenderConstants.GL_NOTEQUAL -> VK_COMPARE_OP_NOT_EQUAL;
            case RenderConstants.GL_GEQUAL -> VK_COMPARE_OP_GREATER_OR_EQUAL;
            case RenderConstants.GL_ALWAYS -> VK_COMPARE_OP_ALWAYS;
            default -> VK_COMPARE_OP_LESS_OR_EQUAL;
        };
    }

    private int translateBlendFactor(int glFactor) {
        return switch (glFactor) {
            case RenderConstants.GL_ZERO -> VK_BLEND_FACTOR_ZERO;
            case RenderConstants.GL_ONE -> VK_BLEND_FACTOR_ONE;
            case RenderConstants.GL_SRC_COLOR -> VK_BLEND_FACTOR_SRC_COLOR;
            case RenderConstants.GL_ONE_MINUS_SRC_COLOR -> VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
            case RenderConstants.GL_DST_COLOR -> VK_BLEND_FACTOR_DST_COLOR;
            case RenderConstants.GL_ONE_MINUS_DST_COLOR -> VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
            case RenderConstants.GL_SRC_ALPHA -> VK_BLEND_FACTOR_SRC_ALPHA;
            case RenderConstants.GL_ONE_MINUS_SRC_ALPHA -> VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            case RenderConstants.GL_DST_ALPHA -> VK_BLEND_FACTOR_DST_ALPHA;
            case RenderConstants.GL_ONE_MINUS_DST_ALPHA -> VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
            case RenderConstants.GL_CONSTANT_COLOR -> VK_BLEND_FACTOR_CONSTANT_COLOR;
            case RenderConstants.GL_ONE_MINUS_CONSTANT_COLOR -> VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR;
            case RenderConstants.GL_CONSTANT_ALPHA -> VK_BLEND_FACTOR_CONSTANT_ALPHA;
            case RenderConstants.GL_ONE_MINUS_CONSTANT_ALPHA -> VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA;
            case RenderConstants.GL_SRC_ALPHA_SATURATE -> VK_BLEND_FACTOR_SRC_ALPHA_SATURATE;
            case RenderConstants.GL_SRC1_COLOR -> VK_BLEND_FACTOR_SRC1_COLOR;
            case RenderConstants.GL_ONE_MINUS_SRC1_COLOR -> VK_BLEND_FACTOR_ONE_MINUS_SRC1_COLOR;
            case RenderConstants.GL_SRC1_ALPHA -> VK_BLEND_FACTOR_SRC1_ALPHA;
            case RenderConstants.GL_ONE_MINUS_SRC1_ALPHA -> VK_BLEND_FACTOR_ONE_MINUS_SRC1_ALPHA;
            default -> VK_BLEND_FACTOR_ONE;
        };
    }

    private int translateBlendOp(int glOp) {
        return switch (glOp) {
            case RenderConstants.GL_FUNC_ADD -> VK_BLEND_OP_ADD;
            case RenderConstants.GL_FUNC_SUBTRACT -> VK_BLEND_OP_SUBTRACT;
            case RenderConstants.GL_FUNC_REVERSE_SUBTRACT -> VK_BLEND_OP_REVERSE_SUBTRACT;
            case RenderConstants.GL_MIN -> VK_BLEND_OP_MIN;
            case RenderConstants.GL_MAX -> VK_BLEND_OP_MAX;
            default -> VK_BLEND_OP_ADD;
        };
    }

    private int translateStencilOp(int glOp) {
        return switch (glOp) {
            case RenderConstants.GL_KEEP -> VK_STENCIL_OP_KEEP;
            case RenderConstants.GL_ZERO -> VK_STENCIL_OP_ZERO;
            case RenderConstants.GL_REPLACE -> VK_STENCIL_OP_REPLACE;
            case RenderConstants.GL_INCR -> VK_STENCIL_OP_INCREMENT_AND_CLAMP;
            case RenderConstants.GL_INCR_WRAP -> VK_STENCIL_OP_INCREMENT_AND_WRAP;
            case RenderConstants.GL_DECR -> VK_STENCIL_OP_DECREMENT_AND_CLAMP;
            case RenderConstants.GL_DECR_WRAP -> VK_STENCIL_OP_DECREMENT_AND_WRAP;
            case RenderConstants.GL_INVERT -> VK_STENCIL_OP_INVERT;
            default -> VK_STENCIL_OP_KEEP;
        };
    }

    private int translateCullMode(int glFace) {
        return switch (glFace) {
            case RenderConstants.GL_FRONT -> VK_CULL_MODE_FRONT_BIT;
            case RenderConstants.GL_BACK -> VK_CULL_MODE_BACK_BIT;
            case RenderConstants.GL_FRONT_AND_BACK -> VK_CULL_MODE_FRONT_AND_BACK;
            default -> VK_CULL_MODE_BACK_BIT;
        };
    }

    private int translateFrontFace(int glFront) {
        return glFront == RenderConstants.GL_CW ? VK_FRONT_FACE_CLOCKWISE : VK_FRONT_FACE_COUNTER_CLOCKWISE;
    }

    private int translatePolygonMode(int glMode) {
        return switch (glMode) {
            case RenderConstants.GL_POINT -> VK_POLYGON_MODE_POINT;
            case RenderConstants.GL_LINE -> VK_POLYGON_MODE_LINE;
            default -> VK_POLYGON_MODE_FILL;
        };
    }

    private int translateVertexFormat(int type, int count, boolean normalized) {
        return switch (type) {
            case RenderConstants.GL_FLOAT -> switch (count) {
                case 1 -> VK_FORMAT_R32_SFLOAT;
                case 2 -> VK_FORMAT_R32G32_SFLOAT;
                case 3 -> VK_FORMAT_R32G32B32_SFLOAT;
                default -> VK_FORMAT_R32G32B32A32_SFLOAT;
            };
            case RenderConstants.GL_HALF_FLOAT -> switch (count) {
                case 1 -> VK_FORMAT_R16_SFLOAT;
                case 2 -> VK_FORMAT_R16G16_SFLOAT;
                case 3 -> VK_FORMAT_R16G16B16_SFLOAT;
                default -> VK_FORMAT_R16G16B16A16_SFLOAT;
            };
            case RenderConstants.GL_DOUBLE -> switch (count) {
                case 1 -> VK_FORMAT_R64_SFLOAT;
                case 2 -> VK_FORMAT_R64G64_SFLOAT;
                case 3 -> VK_FORMAT_R64G64B64_SFLOAT;
                default -> VK_FORMAT_R64G64B64A64_SFLOAT;
            };
            case RenderConstants.GL_UNSIGNED_BYTE -> normalized ?
                switch (count) {
                    case 1 -> VK_FORMAT_R8_UNORM;
                    case 2 -> VK_FORMAT_R8G8_UNORM;
                    case 3 -> VK_FORMAT_R8G8B8_UNORM;
                    default -> VK_FORMAT_R8G8B8A8_UNORM;
                } :
                switch (count) {
                    case 1 -> VK_FORMAT_R8_UINT;
                    case 2 -> VK_FORMAT_R8G8_UINT;
                    case 3 -> VK_FORMAT_R8G8B8_UINT;
                    default -> VK_FORMAT_R8G8B8A8_UINT;
                };
            case RenderConstants.GL_BYTE -> normalized ?
                switch (count) {
                    case 1 -> VK_FORMAT_R8_SNORM;
                    case 2 -> VK_FORMAT_R8G8_SNORM;
                    case 3 -> VK_FORMAT_R8G8B8_SNORM;
                    default -> VK_FORMAT_R8G8B8A8_SNORM;
                } :
                switch (count) {
                    case 1 -> VK_FORMAT_R8_SINT;
                    case 2 -> VK_FORMAT_R8G8_SINT;
                    case 3 -> VK_FORMAT_R8G8B8_SINT;
                    default -> VK_FORMAT_R8G8B8A8_SINT;
                };
            case RenderConstants.GL_UNSIGNED_SHORT -> normalized ?
                switch (count) {
                    case 1 -> VK_FORMAT_R16_UNORM;
                    case 2 -> VK_FORMAT_R16G16_UNORM;
                    case 3 -> VK_FORMAT_R16G16B16_UNORM;
                    default -> VK_FORMAT_R16G16B16A16_UNORM;
                } :
                switch (count) {
                    case 1 -> VK_FORMAT_R16_UINT;
                    case 2 -> VK_FORMAT_R16G16_UINT;
                    case 3 -> VK_FORMAT_R16G16B16_UINT;
                    default -> VK_FORMAT_R16G16B16A16_UINT;
                };
            case RenderConstants.GL_SHORT -> normalized ?
                switch (count) {
                    case 1 -> VK_FORMAT_R16_SNORM;
                    case 2 -> VK_FORMAT_R16G16_SNORM;
                    case 3 -> VK_FORMAT_R16G16B16_SNORM;
                    default -> VK_FORMAT_R16G16B16A16_SNORM;
                } :
                switch (count) {
                    case 1 -> VK_FORMAT_R16_SINT;
                    case 2 -> VK_FORMAT_R16G16_SINT;
                    case 3 -> VK_FORMAT_R16G16B16_SINT;
                    default -> VK_FORMAT_R16G16B16A16_SINT;
                };
            case RenderConstants.GL_UNSIGNED_INT -> switch (count) {
                case 1 -> VK_FORMAT_R32_UINT;
                case 2 -> VK_FORMAT_R32G32_UINT;
                case 3 -> VK_FORMAT_R32G32B32_UINT;
                default -> VK_FORMAT_R32G32B32A32_UINT;
            };
            case RenderConstants.GL_INT -> switch (count) {
                case 1 -> VK_FORMAT_R32_SINT;
                case 2 -> VK_FORMAT_R32G32_SINT;
                case 3 -> VK_FORMAT_R32G32B32_SINT;
                default -> VK_FORMAT_R32G32B32A32_SINT;
            };
            case RenderConstants.GL_INT_2_10_10_10_REV -> normalized ? 
                VK_FORMAT_A2B10G10R10_SNORM_PACK32 : VK_FORMAT_A2B10G10R10_SINT_PACK32;
            case RenderConstants.GL_UNSIGNED_INT_2_10_10_10_REV -> normalized ?
                VK_FORMAT_A2B10G10R10_UNORM_PACK32 : VK_FORMAT_A2B10G10R10_UINT_PACK32;
            default -> VK_FORMAT_R32G32B32A32_SFLOAT;
        };
    }

    // ════════════════════════════════════════════════════════════════════════════
    // REGISTRATION METHODS
    // ════════════════════════════════════════════════════════════════════════════

    public void registerShaderModules(long programId, long vertexModule, long fragmentModule) {
        shaderModules.put(programId, new ShaderModulePair(vertexModule, fragmentModule));
        invalidateProgramPipelines(programId);
    }

    public void registerVertexFormat(int formatHash, VertexBinding[] bindings, VertexAttribute[] attributes) {
        vertexInputCache.put(formatHash, new VertexInputDescription(bindings.clone(), attributes.clone()));
    }

    public void invalidateProgramPipelines(long programId) {
        for (ConcurrentHashMap<Long, PipelineEntry> stripe : pipelineStripes) {
            stripe.entrySet().removeIf(entry -> {
                if (entry.getValue().key.programHandle == programId) {
                    vkDestroyPipeline(device, entry.getValue().pipeline, null);
                    stats.recordEviction();
                    return true;
                }
                return false;
            });
        }
    }

    public void invalidateAllPipelines() {
        for (int i = 0; i < CACHE_STRIPE_COUNT; i++) {
            StampedLock lock = stripeLocks[i];
            long stamp = lock.writeLock();
            try {
                for (PipelineEntry entry : pipelineStripes[i].values()) {
                    vkDestroyPipeline(device, entry.pipeline, null);
                }
                pipelineStripes[i].clear();
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        stats.reset();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PERSISTENCE
    // ════════════════════════════════════════════════════════════════════════════

    public void savePipelineCache() {
        try {
            long[] size = new long[1];
            vkGetPipelineCacheData(device, pipelineCache, size, null);
            
            if (size[0] == 0) {
                Astralis.LOGGER.debug("[Pipeline] Cache empty, nothing to save");
                return;
            }
            
            ByteBuffer data = MemoryUtil.memAlloc((int) size[0]);
            try {
                vkGetPipelineCacheData(device, pipelineCache, size, data);
                
                Files.createDirectories(cacheFilePath.getParent());
                
                try (FileOutputStream fos = new FileOutputStream(cacheFilePath.toFile())) {
                    byte[] bytes = new byte[data.remaining()];
                    data.get(bytes);
                    fos.write(bytes);
                }
                
                Astralis.LOGGER.info("[Pipeline] Saved {} KB to cache", size[0] / 1024);
            } finally {
                MemoryUtil.memFree(data);
            }
        } catch (IOException e) {
            Astralis.LOGGER.warn("[Pipeline] Failed to save cache: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // STATISTICS & DIAGNOSTICS
    // ════════════════════════════════════════════════════════════════════════════

    public PipelineStats getStats() {
        return stats.snapshot();
    }

    private void logInitialization() {
        Astralis.LOGGER.info("[Pipeline] Initialized:");
        Astralis.LOGGER.info("  Vulkan: {}.{}.{}", 
            VK_API_VERSION_MAJOR(capabilities.apiVersion),
            VK_API_VERSION_MINOR(capabilities.apiVersion),
            VK_API_VERSION_PATCH(capabilities.apiVersion));
        Astralis.LOGGER.info("  Extended Dynamic State: {}/{}/{}", 
            capabilities.extendedDynamicState,
            capabilities.extendedDynamicState2,
            capabilities.extendedDynamicState3);
        Astralis.LOGGER.info("  Pipeline Library: {}", capabilities.pipelineLibrary);
        Astralis.LOGGER.info("  Dynamic Rendering: {}", capabilities.dynamicRendering);
        Astralis.LOGGER.info("  Cache Stripes: {}", CACHE_STRIPE_COUNT);
    }

    private void logCreationFeedback(long pipeline, VkPipelineCreationFeedback.Buffer feedbacks, long elapsedNs) {
        VkPipelineCreationFeedback overall = feedbacks.get(0);
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[Pipeline] 0x%X created in %.2fms", pipeline, elapsedNs / 1_000_000.0));
        
        int flags = overall.flags();
        if ((flags & VK_PIPELINE_CREATION_FEEDBACK_VALID_BIT) != 0) {
            if ((flags & VK_PIPELINE_CREATION_FEEDBACK_APPLICATION_PIPELINE_CACHE_HIT_BIT) != 0) {
                sb.append(" [CACHE HIT]");
            }
            if ((flags & VK_PIPELINE_CREATION_FEEDBACK_BASE_PIPELINE_ACCELERATION_BIT) != 0) {
                sb.append(" [DERIVATIVE]");
            }
        }
        
        Astralis.LOGGER.debug(sb.toString());
    }

    private String translateVkResult(int result) {
        return switch (result) {
            case VK_SUCCESS -> "VK_SUCCESS";
            case VK_ERROR_OUT_OF_HOST_MEMORY -> "VK_ERROR_OUT_OF_HOST_MEMORY";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "VK_ERROR_OUT_OF_DEVICE_MEMORY";
            case VK_ERROR_INITIALIZATION_FAILED -> "VK_ERROR_INITIALIZATION_FAILED";
            case VK_ERROR_DEVICE_LOST -> "VK_ERROR_DEVICE_LOST";
            case VK_ERROR_LAYER_NOT_PRESENT -> "VK_ERROR_LAYER_NOT_PRESENT";
            case VK_ERROR_EXTENSION_NOT_PRESENT -> "VK_ERROR_EXTENSION_NOT_PRESENT";
            case VK_ERROR_FEATURE_NOT_PRESENT -> "VK_ERROR_FEATURE_NOT_PRESENT";
            case VK_ERROR_INCOMPATIBLE_DRIVER -> "VK_ERROR_INCOMPATIBLE_DRIVER";
            case VK_ERROR_TOO_MANY_OBJECTS -> "VK_ERROR_TOO_MANY_OBJECTS";
            case VK_ERROR_FORMAT_NOT_SUPPORTED -> "VK_ERROR_FORMAT_NOT_SUPPORTED";
            case VK_ERROR_FRAGMENTED_POOL -> "VK_ERROR_FRAGMENTED_POOL";
            default -> "VK_ERROR_UNKNOWN(" + result + ")";
        };
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ════════════════════════════════════════════════════════════════════════════

    public void cleanup() {
        // Stop async compiler
        compilerRunning.set(false);
        compilationExecutor.shutdown();
        try {
            if (!compilationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                compilationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            compilationExecutor.shutdownNow();
        }
        
        // Save cache
        savePipelineCache();
        
        // Destroy all pipelines
        for (ConcurrentHashMap<Long, PipelineEntry> stripe : pipelineStripes) {
            for (PipelineEntry entry : stripe.values()) {
                vkDestroyPipeline(device, entry.pipeline, null);
            }
            stripe.clear();
        }
        
        // Destroy pipeline libraries
        for (long lib : vertexInputLibraries.values()) {
            vkDestroyPipeline(device, lib, null);
        }
        for (long lib : fragmentOutputLibraries.values()) {
            vkDestroyPipeline(device, lib, null);
        }
        
        // Destroy pipeline cache
        vkDestroyPipelineCache(device, pipelineCache, null);
        
        // Destroy default shaders
        if (defaultVertexShader != VK_NULL_HANDLE) {
            vkDestroyShaderModule(device, defaultVertexShader, null);
        }
        if (defaultFragmentShader != VK_NULL_HANDLE) {
            vkDestroyShaderModule(device, defaultFragmentShader, null);
        }
        
        Astralis.LOGGER.info("[Pipeline] Cleanup complete. {}", stats.snapshot());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // INNER CLASSES
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Detected Vulkan capabilities.
     */
    public record VulkanCapabilities(
        int apiVersion,
        int driverVersion,
        int vendorId,
        boolean isVulkan11Plus,
        boolean isVulkan12Plus,
        boolean isVulkan13Plus,
        boolean isVulkan14Plus,
        boolean extendedDynamicState,
        boolean extendedDynamicState2,
        boolean extendedDynamicState3,
        boolean pipelineLibrary,
        boolean dynamicRendering,
        boolean shaderObject,
        boolean creationFeedback,
        boolean timelineSemaphore,
        boolean bufferDeviceAddress,
        boolean maintenance4,
        boolean maintenance5,
        boolean maintenance6
    ) {
        public boolean supportsDerivatives() {
            return true; // Core since 1.0
        }
    }

    /**
     * Immutable pipeline cache key with precomputed hash.
     */
    public record PipelineKey(
        long hash,
        long programHandle,
        long blendState,
        long depthState,
        long stencilState,
        long cullState,
        long colorMask,
        int primitiveMode,
        int vertexFormatHash,
        long renderPass
    ) {}

    /**
     * Cache entry with usage tracking.
     */
    private static final class PipelineEntry {
        final PipelineKey key;
        final long pipeline;
        final long createdAt;
        volatile long lastUsed;
        final AtomicInteger useCount = new AtomicInteger(0);

        PipelineEntry(PipelineKey key, long pipeline) {
            this.key = key;
            this.pipeline = pipeline;
            this.createdAt = System.nanoTime();
            this.lastUsed = createdAt;
        }

        boolean matches(PipelineKey other) {
            return key.programHandle == other.programHandle &&
                   key.blendState == other.blendState &&
                   key.depthState == other.depthState &&
                   key.stencilState == other.stencilState &&
                   key.cullState == other.cullState &&
                   key.colorMask == other.colorMask &&
                   key.primitiveMode == other.primitiveMode &&
                   key.vertexFormatHash == other.vertexFormatHash &&
                   key.renderPass == other.renderPass;
        }

        void recordUsage() {
            lastUsed = System.nanoTime();
            useCount.incrementAndGet();
        }
    }

    /**
     * Shader module pair for a program.
     */
    private record ShaderModulePair(long vertex, long fragment) {}

    /**
     * Vertex input description.
     */
    private record VertexInputDescription(VertexBinding[] bindings, VertexAttribute[] attributes) {}

    /**
     * Async compilation request.
     */
    private record CompilationRequest(PipelineKey key, RenderState state, CompletableFuture<Long> future) {}

    /**
     * Thread-safe statistics collector.
     */
    private static final class PipelineStatistics {
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();
        private final AtomicLong compilations = new AtomicLong();
        private final AtomicLong batchCompilations = new AtomicLong();
        private final AtomicLong evictions = new AtomicLong();
        private final AtomicLong totalCompileTimeNs = new AtomicLong();
        private final AtomicLong peakCompileTimeNs = new AtomicLong();

        void recordHit() { hits.incrementAndGet(); }
        void recordMiss() { misses.incrementAndGet(); }
        void recordCompilation() { compilations.incrementAndGet(); }
        void recordEviction() { evictions.incrementAndGet(); }
        
        void addCompilationTime(long ns) {
            totalCompileTimeNs.addAndGet(ns);
            peakCompileTimeNs.accumulateAndGet(ns, Math::max);
        }
        
        void addBatchCompilation(int count, long totalNs) {
            batchCompilations.addAndGet(count);
            totalCompileTimeNs.addAndGet(totalNs);
        }
        
        void reset() {
            hits.set(0);
            misses.set(0);
            compilations.set(0);
            batchCompilations.set(0);
            evictions.set(0);
            totalCompileTimeNs.set(0);
            peakCompileTimeNs.set(0);
        }

        PipelineStats snapshot() {
            long h = hits.get();
            long m = misses.get();
            double hitRate = (h + m) > 0 ? (h * 100.0 / (h + m)) : 100.0;
            return new PipelineStats(h, m, hitRate, compilations.get(), batchCompilations.get(),
                evictions.get(), totalCompileTimeNs.get() / 1_000_000.0, peakCompileTimeNs.get() / 1_000_000.0);
        }
    }

    /**
     * Immutable statistics snapshot.
     */
    public record PipelineStats(
        long hits,
        long misses,
        double hitRatePercent,
        long compilations,
        long batchCompilations,
        long evictions,
        double totalCompileTimeMs,
        double peakCompileTimeMs
    ) {
        @Override
        public String toString() {
            return String.format("Hits=%d Misses=%d (%.1f%%) Compiles=%d Batch=%d Evict=%d Time=%.2fms Peak=%.2fms",
                hits, misses, hitRatePercent, compilations, batchCompilations, evictions, totalCompileTimeMs, peakCompileTimeMs);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PUBLIC DATA CLASSES
    // ════════════════════════════════════════════════════════════════════════════

    public record VertexBinding(int binding, int stride, boolean instanced) {}
    
    public record VertexAttribute(int location, int binding, int type, int count, boolean normalized, int offset) {}
}
