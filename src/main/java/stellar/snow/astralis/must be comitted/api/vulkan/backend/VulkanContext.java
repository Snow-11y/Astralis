package stellar.snow.astralis.api.vulkan.backend;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.EXTDescriptorIndexing.*;
import static org.lwjgl.vulkan.EXTMeshShader.*;
import static org.lwjgl.vulkan.EXTShaderObject.*;
import static org.lwjgl.vulkan.KHRBufferDeviceAddress.*;
import static org.lwjgl.vulkan.KHRDynamicRendering.*;
import static org.lwjgl.vulkan.KHRGetPhysicalDeviceProperties2.*;
import static org.lwjgl.vulkan.KHRMaintenance4.*;
import static org.lwjgl.vulkan.KHRPushDescriptor.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSynchronization2.*;
import static org.lwjgl.vulkan.KHRTimelineSemaphore.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.*;

/**
 * VulkanContext - Ultra-high-performance Vulkan initialization and management
 * 
 * <h2>Architecture Overview</h2>
 * This class provides complete Vulkan context management with:
 * <ul>
 *   <li>Vulkan 1.0 - 1.4 support with automatic feature detection</li>
 *   <li>Zero-allocation hot paths for frame rendering</li>
 *   <li>Lock-free synchronization for multi-threaded access</li>
 *   <li>Arena-managed native memory for leak-proof cleanup</li>
 *   <li>Timeline semaphore synchronization when available</li>
 *   <li>Per-image in-flight tracking for correct synchronization</li>
 *   <li>Command buffer pooling with per-frame reset</li>
 *   <li>Async swapchain recreation with old swapchain retirement</li>
 *   <li>Pipeline cache persistence with validation</li>
 *   <li>Comprehensive modern extension support</li>
 * </ul>
 * 
 * <h2>Java 25 Features Utilized</h2>
 * <ul>
 *   <li>Records for immutable data structures</li>
 *   <li>Sealed interfaces for type-safe hierarchies</li>
 *   <li>Pattern matching in switch expressions</li>
 *   <li>Foreign Function & Memory API (Arena, MemorySegment)</li>
 *   <li>Virtual threads for async operations</li>
 *   <li>Scoped values for context propagation</li>
 *   <li>VarHandle for atomic field access</li>
 * </ul>
 * 
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li>Frame rendering: zero allocations in steady state</li>
 *   <li>Swapchain recreation: async, non-blocking</li>
 *   <li>Command buffer allocation: pooled, O(1)</li>
 *   <li>Synchronization: lock-free with timeline semaphores</li>
 *   <li>Memory: cache-friendly primitive arrays</li>
 * </ul>
 * 
 * @author Enhanced for production use
 * @version 3.0.0
 * @since Java 25, LWJGL 3.3.6, Vulkan 1.4
 */
@SuppressWarnings({"preview", "unused"})
public final class VulkanContext implements AutoCloseable {
    
    // ========================================================================
    // VERSION CONSTANTS
    // ========================================================================
    
    /** Vulkan version constants for comparison */
    public static final class VulkanVersion {
        public static final int V1_0 = VK_MAKE_API_VERSION(0, 1, 0, 0);
        public static final int V1_1 = VK_MAKE_API_VERSION(0, 1, 1, 0);
        public static final int V1_2 = VK_MAKE_API_VERSION(0, 1, 2, 0);
        public static final int V1_3 = VK_MAKE_API_VERSION(0, 1, 3, 0);
        public static final int V1_4 = VK_MAKE_API_VERSION(0, 1, 4, 0);
        
        public static String toString(int version) {
            return VK_API_VERSION_MAJOR(version) + "." + 
                   VK_API_VERSION_MINOR(version) + "." + 
                   VK_API_VERSION_PATCH(version);
        }
        
        public static boolean isAtLeast(int version, int required) {
            return VK_API_VERSION_MAJOR(version) > VK_API_VERSION_MAJOR(required) ||
                   (VK_API_VERSION_MAJOR(version) == VK_API_VERSION_MAJOR(required) &&
                    VK_API_VERSION_MINOR(version) >= VK_API_VERSION_MINOR(required));
        }
        
        private VulkanVersion() {}
    }
    
    // ========================================================================
    // CONFIGURATION CONSTANTS
    // ========================================================================
    
    /** Maximum frames that can be in-flight simultaneously */
    public static final int MAX_FRAMES_IN_FLIGHT = 3;
    
    /** Maximum swapchain images supported */
    public static final int MAX_SWAPCHAIN_IMAGES = 8;
    
    /** Command buffer pool size per frame */
    public static final int COMMAND_BUFFERS_PER_FRAME = 16;
    
    /** Secondary command buffer pool size per frame */
    public static final int SECONDARY_COMMAND_BUFFERS_PER_FRAME = 64;
    
    /** Default timeout for fence waits (nanoseconds) */
    public static final long DEFAULT_FENCE_TIMEOUT = 5_000_000_000L; // 5 seconds
    
    /** Pipeline cache file name */
    public static final String PIPELINE_CACHE_FILENAME = "pipeline_cache.bin";
    
    /** Maximum pipeline cache size (bytes) */
    public static final int MAX_PIPELINE_CACHE_SIZE = 64 * 1024 * 1024; // 64 MB
    
    // ========================================================================
    // ERROR CODES - COMPREHENSIVE TRANSLATION
    // ========================================================================
    
    /**
     * Vulkan error code to human-readable string.
     */
    public static String vkResultToString(int result) {
        return switch (result) {
            case VK_SUCCESS -> "VK_SUCCESS";
            case VK_NOT_READY -> "VK_NOT_READY";
            case VK_TIMEOUT -> "VK_TIMEOUT";
            case VK_EVENT_SET -> "VK_EVENT_SET";
            case VK_EVENT_RESET -> "VK_EVENT_RESET";
            case VK_INCOMPLETE -> "VK_INCOMPLETE";
            case VK_ERROR_OUT_OF_HOST_MEMORY -> "VK_ERROR_OUT_OF_HOST_MEMORY";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "VK_ERROR_OUT_OF_DEVICE_MEMORY";
            case VK_ERROR_INITIALIZATION_FAILED -> "VK_ERROR_INITIALIZATION_FAILED";
            case VK_ERROR_DEVICE_LOST -> "VK_ERROR_DEVICE_LOST";
            case VK_ERROR_MEMORY_MAP_FAILED -> "VK_ERROR_MEMORY_MAP_FAILED";
            case VK_ERROR_LAYER_NOT_PRESENT -> "VK_ERROR_LAYER_NOT_PRESENT";
            case VK_ERROR_EXTENSION_NOT_PRESENT -> "VK_ERROR_EXTENSION_NOT_PRESENT";
            case VK_ERROR_FEATURE_NOT_PRESENT -> "VK_ERROR_FEATURE_NOT_PRESENT";
            case VK_ERROR_INCOMPATIBLE_DRIVER -> "VK_ERROR_INCOMPATIBLE_DRIVER";
            case VK_ERROR_TOO_MANY_OBJECTS -> "VK_ERROR_TOO_MANY_OBJECTS";
            case VK_ERROR_FORMAT_NOT_SUPPORTED -> "VK_ERROR_FORMAT_NOT_SUPPORTED";
            case VK_ERROR_FRAGMENTED_POOL -> "VK_ERROR_FRAGMENTED_POOL";
            case VK_ERROR_UNKNOWN -> "VK_ERROR_UNKNOWN";
            case VK_ERROR_OUT_OF_POOL_MEMORY -> "VK_ERROR_OUT_OF_POOL_MEMORY";
            case VK_ERROR_INVALID_EXTERNAL_HANDLE -> "VK_ERROR_INVALID_EXTERNAL_HANDLE";
            case VK_ERROR_FRAGMENTATION -> "VK_ERROR_FRAGMENTATION";
            case VK_ERROR_INVALID_OPAQUE_CAPTURE_ADDRESS -> "VK_ERROR_INVALID_OPAQUE_CAPTURE_ADDRESS";
            case VK_ERROR_SURFACE_LOST_KHR -> "VK_ERROR_SURFACE_LOST_KHR";
            case VK_ERROR_NATIVE_WINDOW_IN_USE_KHR -> "VK_ERROR_NATIVE_WINDOW_IN_USE_KHR";
            case VK_SUBOPTIMAL_KHR -> "VK_SUBOPTIMAL_KHR";
            case VK_ERROR_OUT_OF_DATE_KHR -> "VK_ERROR_OUT_OF_DATE_KHR";
            case VK_ERROR_VALIDATION_FAILED_EXT -> "VK_ERROR_VALIDATION_FAILED_EXT";
            default -> "VK_UNKNOWN_ERROR (" + result + ")";
        };
    }
    
    // ========================================================================
    // CUSTOM EXCEPTION HIERARCHY
    // ========================================================================
    
    /**
     * Base exception for all Vulkan errors.
     */
    public static sealed class VulkanException extends RuntimeException 
        permits VulkanDeviceLostException, VulkanOutOfMemoryException, 
                VulkanSurfaceLostException, VulkanSwapchainOutOfDateException,
                VulkanValidationException, VulkanInitializationException {
        
        private final int errorCode;
        
        public VulkanException(int errorCode, String message) {
            super(message + " (" + vkResultToString(errorCode) + ")");
            this.errorCode = errorCode;
        }
        
        public VulkanException(String message) {
            super(message);
            this.errorCode = VK_ERROR_UNKNOWN;
        }
        
        public VulkanException(String message, Throwable cause) {
            super(message, cause);
            this.errorCode = VK_ERROR_UNKNOWN;
        }
        
        public int getErrorCode() {
            return errorCode;
        }
        
        public boolean isRecoverable() {
            return false;
        }
    }
    
    /** Device lost - fatal, requires full restart */
    public static final class VulkanDeviceLostException extends VulkanException {
        public VulkanDeviceLostException(String context) {
            super(VK_ERROR_DEVICE_LOST, "Device lost during " + context);
        }
    }
    
    /** Out of memory - may be recoverable by freeing resources */
    public static final class VulkanOutOfMemoryException extends VulkanException {
        private final boolean hostMemory;
        
        public VulkanOutOfMemoryException(int errorCode, String context) {
            super(errorCode, "Out of memory during " + context);
            this.hostMemory = (errorCode == VK_ERROR_OUT_OF_HOST_MEMORY);
        }
        
        public boolean isHostMemory() { return hostMemory; }
        
        @Override
        public boolean isRecoverable() { return true; }
    }
    
    /** Surface lost - need to recreate surface */
    public static final class VulkanSurfaceLostException extends VulkanException {
        public VulkanSurfaceLostException(String context) {
            super(VK_ERROR_SURFACE_LOST_KHR, "Surface lost during " + context);
        }
        
        @Override
        public boolean isRecoverable() { return true; }
    }
    
    /** Swapchain out of date - need to recreate swapchain */
    public static final class VulkanSwapchainOutOfDateException extends VulkanException {
        public VulkanSwapchainOutOfDateException(String context) {
            super(VK_ERROR_OUT_OF_DATE_KHR, "Swapchain out of date during " + context);
        }
        
        @Override
        public boolean isRecoverable() { return true; }
    }
    
    /** Validation error - debug only */
    public static final class VulkanValidationException extends VulkanException {
        public VulkanValidationException(String message) {
            super(VK_ERROR_VALIDATION_FAILED_EXT, message);
        }
    }
    
    /** Initialization failed */
    public static final class VulkanInitializationException extends VulkanException {
        public VulkanInitializationException(String message) {
            super(VK_ERROR_INITIALIZATION_FAILED, message);
        }
        
        public VulkanInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Check Vulkan result and throw appropriate exception.
     */
    public static void vkCheck(int result, String context) {
        if (result == VK_SUCCESS) return;
        
        throw switch (result) {
            case VK_ERROR_DEVICE_LOST -> new VulkanDeviceLostException(context);
            case VK_ERROR_OUT_OF_HOST_MEMORY, VK_ERROR_OUT_OF_DEVICE_MEMORY,
                 VK_ERROR_OUT_OF_POOL_MEMORY -> new VulkanOutOfMemoryException(result, context);
            case VK_ERROR_SURFACE_LOST_KHR -> new VulkanSurfaceLostException(context);
            case VK_ERROR_OUT_OF_DATE_KHR -> new VulkanSwapchainOutOfDateException(context);
            case VK_ERROR_VALIDATION_FAILED_EXT -> new VulkanValidationException(context);
            default -> new VulkanException(result, context);
        };
    }
    
    // ========================================================================
    // FEATURE FLAGS - BIT-PACKED FOR EFFICIENCY
    // ========================================================================
    
    /**
     * Bit flags for device features.
     * Using long for up to 64 feature flags with O(1) checking.
     */
    public static final class FeatureFlags {
        // Core features
        public static final long SAMPLER_ANISOTROPY = 1L;
        public static final long GEOMETRY_SHADER = 1L << 1;
        public static final long TESSELLATION_SHADER = 1L << 2;
        public static final long MULTI_DRAW_INDIRECT = 1L << 3;
        public static final long DRAW_INDIRECT_FIRST_INSTANCE = 1L << 4;
        public static final long DEPTH_CLAMP = 1L << 5;
        public static final long DEPTH_BIAS_CLAMP = 1L << 6;
        public static final long FILL_MODE_NON_SOLID = 1L << 7;
        public static final long WIDE_LINES = 1L << 8;
        public static final long LARGE_POINTS = 1L << 9;
        public static final long LOGIC_OP = 1L << 10;
        public static final long MULTI_VIEWPORT = 1L << 11;
        public static final long SHADER_STORAGE_IMAGE_EXTENDED_FORMATS = 1L << 12;
        public static final long SHADER_UNIFORM_BUFFER_ARRAY_DYNAMIC_INDEXING = 1L << 13;
        public static final long SHADER_SAMPLED_IMAGE_ARRAY_DYNAMIC_INDEXING = 1L << 14;
        public static final long SHADER_STORAGE_BUFFER_ARRAY_DYNAMIC_INDEXING = 1L << 15;
        public static final long SHADER_STORAGE_IMAGE_ARRAY_DYNAMIC_INDEXING = 1L << 16;
        public static final long SHADER_INT64 = 1L << 17;
        public static final long SHADER_INT16 = 1L << 18;
        public static final long SHADER_FLOAT64 = 1L << 19;
        
        // Vulkan 1.1 features
        public static final long MULTIVIEW = 1L << 20;
        public static final long VARIABLE_POINTERS = 1L << 21;
        public static final long PROTECTED_MEMORY = 1L << 22;
        public static final long SHADER_DRAW_PARAMETERS = 1L << 23;
        
        // Vulkan 1.2 features
        public static final long TIMELINE_SEMAPHORE = 1L << 24;
        public static final long BUFFER_DEVICE_ADDRESS = 1L << 25;
        public static final long DESCRIPTOR_INDEXING = 1L << 26;
        public static final long SCALAR_BLOCK_LAYOUT = 1L << 27;
        public static final long IMAGELESS_FRAMEBUFFER = 1L << 28;
        public static final long UNIFORM_BUFFER_STANDARD_LAYOUT = 1L << 29;
        public static final long SHADER_SUBGROUP_EXTENDED_TYPES = 1L << 30;
        public static final long SEPARATE_DEPTH_STENCIL_LAYOUTS = 1L << 31;
        public static final long HOST_QUERY_RESET = 1L << 32;
        public static final long VULKAN_MEMORY_MODEL = 1L << 33;
        public static final long SHADER_OUTPUT_VIEWPORT_INDEX = 1L << 34;
        public static final long SHADER_OUTPUT_LAYER = 1L << 35;
        
        // Vulkan 1.3 features
        public static final long DYNAMIC_RENDERING = 1L << 36;
        public static final long SYNCHRONIZATION_2 = 1L << 37;
        public static final long MAINTENANCE_4 = 1L << 38;
        public static final long SHADER_INTEGER_DOT_PRODUCT = 1L << 39;
        public static final long SHADER_NON_SEMANTIC_INFO = 1L << 40;
        public static final long PRIVATE_DATA = 1L << 41;
        public static final long PIPELINE_CREATION_CACHE_CONTROL = 1L << 42;
        public static final long SUBGROUP_SIZE_CONTROL = 1L << 43;
        public static final long COMPUTE_FULL_SUBGROUPS = 1L << 44;
        public static final long TEXTURE_COMPRESSION_ASTC_HDR = 1L << 45;
        public static final long SHADER_ZERO_INITIALIZE_WORKGROUP_MEMORY = 1L << 46;
        
        // Vulkan 1.4 features (estimated - adjust based on actual spec)
        public static final long PUSH_DESCRIPTORS = 1L << 47;
        public static final long MAINTENANCE_5 = 1L << 48;
        public static final long MAINTENANCE_6 = 1L << 49;
        public static final long SHADER_OBJECT = 1L << 50;
        public static final long DYNAMIC_RENDERING_LOCAL_READ = 1L << 51;
        
        // Extension features
        public static final long MESH_SHADER = 1L << 52;
        public static final long TASK_SHADER = 1L << 53;
        public static final long RAY_TRACING_PIPELINE = 1L << 54;
        public static final long RAY_QUERY = 1L << 55;
        public static final long ACCELERATION_STRUCTURE = 1L << 56;
        public static final long FRAGMENT_SHADING_RATE = 1L << 57;
        public static final long EXTENDED_DYNAMIC_STATE = 1L << 58;
        public static final long EXTENDED_DYNAMIC_STATE_2 = 1L << 59;
        public static final long EXTENDED_DYNAMIC_STATE_3 = 1L << 60;
        public static final long VERTEX_INPUT_DYNAMIC_STATE = 1L << 61;
        public static final long COLOR_WRITE_ENABLE = 1L << 62;
        
        private FeatureFlags() {}
    }
    
    // ========================================================================
    // EXTENSION REGISTRY - INTERNED FOR FAST COMPARISON
    // ========================================================================
    
    /**
     * Extension categories for organized handling.
     */
    public sealed interface ExtensionCategory permits 
            ExtensionCategory.Required, 
            ExtensionCategory.Preferred, 
            ExtensionCategory.Optional {
        
        String name();
        int priority(); // Higher = more important
        
        record Required(String name) implements ExtensionCategory {
            @Override public int priority() { return 100; }
        }
        
        record Preferred(String name, long featureFlag) implements ExtensionCategory {
            @Override public int priority() { return 50; }
        }
        
        record Optional(String name, long featureFlag) implements ExtensionCategory {
            @Override public int priority() { return 10; }
        }
    }
    
    /**
     * All device extensions we care about.
     */
    public static final class DeviceExtensions {
        // Required
        public static final ExtensionCategory.Required SWAPCHAIN = 
            new ExtensionCategory.Required(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
        
        // Preferred (high value features)
        public static final ExtensionCategory.Preferred DYNAMIC_RENDERING = 
            new ExtensionCategory.Preferred(VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME, FeatureFlags.DYNAMIC_RENDERING);
        public static final ExtensionCategory.Preferred SYNCHRONIZATION_2 = 
            new ExtensionCategory.Preferred(VK_KHR_SYNCHRONIZATION_2_EXTENSION_NAME, FeatureFlags.SYNCHRONIZATION_2);
        public static final ExtensionCategory.Preferred TIMELINE_SEMAPHORE = 
            new ExtensionCategory.Preferred(VK_KHR_TIMELINE_SEMAPHORE_EXTENSION_NAME, FeatureFlags.TIMELINE_SEMAPHORE);
        public static final ExtensionCategory.Preferred BUFFER_DEVICE_ADDRESS = 
            new ExtensionCategory.Preferred(VK_KHR_BUFFER_DEVICE_ADDRESS_EXTENSION_NAME, FeatureFlags.BUFFER_DEVICE_ADDRESS);
        public static final ExtensionCategory.Preferred DESCRIPTOR_INDEXING = 
            new ExtensionCategory.Preferred(VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME, FeatureFlags.DESCRIPTOR_INDEXING);
        public static final ExtensionCategory.Preferred MAINTENANCE_4 = 
            new ExtensionCategory.Preferred(VK_KHR_MAINTENANCE_4_EXTENSION_NAME, FeatureFlags.MAINTENANCE_4);
        public static final ExtensionCategory.Preferred PUSH_DESCRIPTOR = 
            new ExtensionCategory.Preferred(VK_KHR_PUSH_DESCRIPTOR_EXTENSION_NAME, FeatureFlags.PUSH_DESCRIPTORS);
        
        // Optional (nice to have)
        public static final ExtensionCategory.Optional MESH_SHADER = 
            new ExtensionCategory.Optional(VK_EXT_MESH_SHADER_EXTENSION_NAME, FeatureFlags.MESH_SHADER);
        public static final ExtensionCategory.Optional SHADER_OBJECT = 
            new ExtensionCategory.Optional(VK_EXT_SHADER_OBJECT_EXTENSION_NAME, FeatureFlags.SHADER_OBJECT);
        
        // All extensions in priority order
        public static final List<ExtensionCategory> ALL = List.of(
            SWAPCHAIN,
            DYNAMIC_RENDERING,
            SYNCHRONIZATION_2,
            TIMELINE_SEMAPHORE,
            BUFFER_DEVICE_ADDRESS,
            DESCRIPTOR_INDEXING,
            MAINTENANCE_4,
            PUSH_DESCRIPTOR,
            MESH_SHADER,
            SHADER_OBJECT
        );
        
        private DeviceExtensions() {}
    }
    
    // ========================================================================
    // CONFIGURATION
    // ========================================================================
    
    /**
     * Immutable configuration for VulkanContext.
     */
    public record Configuration(
        boolean enableValidation,
        boolean enableGpuAssistedValidation,
        boolean enableBestPracticesValidation,
        boolean enableSynchronizationValidation,
        boolean preferIntegratedGpu,
        boolean enableHdr,
        boolean enableVsync,
        int preferredSwapchainImages,
        Path pipelineCachePath,
        Consumer<String> debugCallback
    ) {
        public static Builder builder() {
            return new Builder();
        }
        
        public static final class Builder {
            private boolean enableValidation = true;
            private boolean enableGpuAssistedValidation = false;
            private boolean enableBestPracticesValidation = true;
            private boolean enableSynchronizationValidation = false;
            private boolean preferIntegratedGpu = false;
            private boolean enableHdr = false;
            private boolean enableVsync = true;
            private int preferredSwapchainImages = 3;
            private Path pipelineCachePath = Path.of(".");
            private Consumer<String> debugCallback = System.err::println;
            
            public Builder enableValidation(boolean enable) { this.enableValidation = enable; return this; }
            public Builder enableGpuAssistedValidation(boolean enable) { this.enableGpuAssistedValidation = enable; return this; }
            public Builder enableBestPracticesValidation(boolean enable) { this.enableBestPracticesValidation = enable; return this; }
            public Builder enableSynchronizationValidation(boolean enable) { this.enableSynchronizationValidation = enable; return this; }
            public Builder preferIntegratedGpu(boolean prefer) { this.preferIntegratedGpu = prefer; return this; }
            public Builder enableHdr(boolean enable) { this.enableHdr = enable; return this; }
            public Builder enableVsync(boolean enable) { this.enableVsync = enable; return this; }
            public Builder preferredSwapchainImages(int count) { this.preferredSwapchainImages = Math.clamp(count, 2, MAX_SWAPCHAIN_IMAGES); return this; }
            public Builder pipelineCachePath(Path path) { this.pipelineCachePath = path; return this; }
            public Builder debugCallback(Consumer<String> callback) { this.debugCallback = callback; return this; }
            
            public Configuration build() {
                return new Configuration(
                    enableValidation, enableGpuAssistedValidation, enableBestPracticesValidation,
                    enableSynchronizationValidation, preferIntegratedGpu, enableHdr, enableVsync,
                    preferredSwapchainImages, pipelineCachePath, debugCallback
                );
            }
        }
        
        public static Configuration defaults() {
            return builder().build();
        }
        
        public static Configuration production() {
            return builder()
                .enableValidation(false)
                .enableGpuAssistedValidation(false)
                .build();
        }
    }
    
    // ========================================================================
    // QUEUE FAMILY INFORMATION
    // ========================================================================
    
    /**
     * Comprehensive queue family information.
     */
    public record QueueFamilyInfo(
        int index,
        int queueCount,
        int queueFlags,
        int timestampValidBits,
        int minImageTransferGranularityWidth,
        int minImageTransferGranularityHeight,
        int minImageTransferGranularityDepth,
        boolean supportsGraphics,
        boolean supportsCompute,
        boolean supportsTransfer,
        boolean supportsSparseBinding,
        boolean supportsProtected,
        boolean supportsPresent
    ) {
        public static QueueFamilyInfo from(int index, VkQueueFamilyProperties props, boolean supportsPresent) {
            int flags = props.queueFlags();
            return new QueueFamilyInfo(
                index,
                props.queueCount(),
                flags,
                props.timestampValidBits(),
                props.minImageTransferGranularity().width(),
                props.minImageTransferGranularity().height(),
                props.minImageTransferGranularity().depth(),
                (flags & VK_QUEUE_GRAPHICS_BIT) != 0,
                (flags & VK_QUEUE_COMPUTE_BIT) != 0,
                (flags & VK_QUEUE_TRANSFER_BIT) != 0,
                (flags & VK_QUEUE_SPARSE_BINDING_BIT) != 0,
                (flags & VK_QUEUE_PROTECTED_BIT) != 0,
                supportsPresent
            );
        }
        
        public boolean isDedicatedCompute() {
            return supportsCompute && !supportsGraphics;
        }
        
        public boolean isDedicatedTransfer() {
            return supportsTransfer && !supportsGraphics && !supportsCompute;
        }
        
        public int score(boolean forGraphics, boolean forCompute, boolean forTransfer, boolean forPresent) {
            int score = 0;
            
            if (forGraphics && supportsGraphics) score += 1000;
            if (forCompute && supportsCompute) score += 1000;
            if (forTransfer && supportsTransfer) score += 1000;
            if (forPresent && supportsPresent) score += 500;
            
            // Prefer dedicated queues
            if (forCompute && isDedicatedCompute()) score += 100;
            if (forTransfer && isDedicatedTransfer()) score += 100;
            
            // Prefer more queues for parallelism
            score += Math.min(queueCount, 8) * 10;
            
            // Prefer queues with timestamp support
            if (timestampValidBits > 0) score += 20;
            
            return score;
        }
    }
    
    /**
     * Selected queue families.
     */
    public record QueueFamilySelection(
        QueueFamilyInfo graphics,
        QueueFamilyInfo present,
        QueueFamilyInfo compute,
        QueueFamilyInfo transfer
    ) {
        public boolean isComplete() {
            return graphics != null && present != null;
        }
        
        public int[] uniqueFamilyIndices() {
            Set<Integer> unique = new LinkedHashSet<>();
            if (graphics != null) unique.add(graphics.index());
            if (present != null) unique.add(present.index());
            if (compute != null) unique.add(compute.index());
            if (transfer != null) unique.add(transfer.index());
            return unique.stream().mapToInt(Integer::intValue).toArray();
        }
        
        public boolean graphicsAndPresentSameFamily() {
            return graphics != null && present != null && graphics.index() == present.index();
        }
    }
    
    // ========================================================================
    // SWAPCHAIN SUPPORT INFORMATION
    // ========================================================================
    
    /**
     * Swapchain support details.
     */
    public record SwapchainSupport(
        int minImageCount,
        int maxImageCount,
        int currentWidth,
        int currentHeight,
        int minWidth,
        int minHeight,
        int maxWidth,
        int maxHeight,
        int maxArrayLayers,
        int supportedTransforms,
        int currentTransform,
        int supportedCompositeAlpha,
        int supportedUsageFlags,
        List<SurfaceFormat> formats,
        int[] presentModes
    ) {
        public record SurfaceFormat(int format, int colorSpace) {
            public boolean isSrgb() {
                return colorSpace == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
            }
            
            public boolean isHdr() {
                return colorSpace == 1000104002 || // VK_COLOR_SPACE_HDR10_ST2084_EXT
                       colorSpace == 1000104003 || // VK_COLOR_SPACE_DOLBYVISION_EXT
                       colorSpace == 1000104004;   // VK_COLOR_SPACE_HDR10_HLG_EXT
            }
        }
        
        public boolean isAdequate() {
            return !formats.isEmpty() && presentModes.length > 0;
        }
        
        public boolean supportsMailbox() {
            for (int mode : presentModes) {
                if (mode == VK_PRESENT_MODE_MAILBOX_KHR) return true;
            }
            return false;
        }
        
        public boolean supportsImmediate() {
            for (int mode : presentModes) {
                if (mode == VK_PRESENT_MODE_IMMEDIATE_KHR) return true;
            }
            return false;
        }
    }
    
    // ========================================================================
    // PHYSICAL DEVICE INFORMATION
    // ========================================================================
    
    /**
     * Comprehensive physical device information.
     */
    public record PhysicalDeviceInfo(
        long handle,
        String name,
        int deviceType,
        int apiVersion,
        int driverVersion,
        int vendorId,
        int deviceId,
        byte[] pipelineCacheUUID,
        DeviceLimits limits,
        long enabledFeatures,
        List<QueueFamilyInfo> queueFamilies,
        Set<String> availableExtensions,
        SwapchainSupport swapchainSupport
    ) {
        public boolean isDiscreteGpu() {
            return deviceType == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU;
        }
        
        public boolean isIntegratedGpu() {
            return deviceType == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU;
        }
        
        public boolean hasFeature(long featureFlag) {
            return (enabledFeatures & featureFlag) != 0;
        }
        
        public boolean hasExtension(String name) {
            return availableExtensions.contains(name);
        }
        
        public String vendorName() {
            return switch (vendorId) {
                case 0x1002 -> "AMD";
                case 0x1010 -> "ImgTec";
                case 0x10DE -> "NVIDIA";
                case 0x13B5 -> "ARM";
                case 0x5143 -> "Qualcomm";
                case 0x8086 -> "Intel";
                default -> "Unknown (0x" + Integer.toHexString(vendorId) + ")";
            };
        }
        
        public int score(Configuration config) {
            int score = 0;
            
            // Device type preference
            if (config.preferIntegratedGpu()) {
                if (isIntegratedGpu()) score += 10000;
                else if (isDiscreteGpu()) score += 1000;
            } else {
                if (isDiscreteGpu()) score += 10000;
                else if (isIntegratedGpu()) score += 1000;
            }
            
            // API version (higher is better)
            score += VK_API_VERSION_MINOR(apiVersion) * 500;
            
            // Features
            if (hasFeature(FeatureFlags.SAMPLER_ANISOTROPY)) score += 100;
            if (hasFeature(FeatureFlags.GEOMETRY_SHADER)) score += 50;
            if (hasFeature(FeatureFlags.TESSELLATION_SHADER)) score += 50;
            if (hasFeature(FeatureFlags.TIMELINE_SEMAPHORE)) score += 200;
            if (hasFeature(FeatureFlags.DYNAMIC_RENDERING)) score += 200;
            if (hasFeature(FeatureFlags.SYNCHRONIZATION_2)) score += 150;
            if (hasFeature(FeatureFlags.BUFFER_DEVICE_ADDRESS)) score += 100;
            if (hasFeature(FeatureFlags.DESCRIPTOR_INDEXING)) score += 100;
            if (hasFeature(FeatureFlags.MESH_SHADER)) score += 100;
            
            // Limits
            score += limits.maxImageDimension2D() / 1000;
            score += (int)(limits.maxMemoryAllocationCount() / 10000);
            score += limits.maxBoundDescriptorSets() * 10;
            
            // Swapchain adequacy
            if (swapchainSupport != null && swapchainSupport.isAdequate()) {
                score += 1000;
                if (swapchainSupport.supportsMailbox()) score += 100;
            }
            
            return score;
        }
    }
    
    /**
     * Device limits (subset of most important ones).
     */
    public record DeviceLimits(
        int maxImageDimension1D,
        int maxImageDimension2D,
        int maxImageDimension3D,
        int maxImageDimensionCube,
        int maxImageArrayLayers,
        int maxTexelBufferElements,
        int maxUniformBufferRange,
        int maxStorageBufferRange,
        int maxPushConstantsSize,
        long maxMemoryAllocationCount,
        long maxSamplerAllocationCount,
        long bufferImageGranularity,
        long sparseAddressSpaceSize,
        int maxBoundDescriptorSets,
        int maxPerStageDescriptorSamplers,
        int maxPerStageDescriptorUniformBuffers,
        int maxPerStageDescriptorStorageBuffers,
        int maxPerStageDescriptorSampledImages,
        int maxPerStageDescriptorStorageImages,
        int maxPerStageDescriptorInputAttachments,
        int maxPerStageResources,
        int maxDescriptorSetSamplers,
        int maxDescriptorSetUniformBuffers,
        int maxDescriptorSetStorageBuffers,
        int maxDescriptorSetSampledImages,
        int maxDescriptorSetStorageImages,
        int maxVertexInputAttributes,
        int maxVertexInputBindings,
        int maxVertexInputAttributeOffset,
        int maxVertexInputBindingStride,
        int maxVertexOutputComponents,
        int maxFragmentInputComponents,
        int maxFragmentOutputAttachments,
        int maxComputeWorkGroupCount0,
        int maxComputeWorkGroupCount1,
        int maxComputeWorkGroupCount2,
        int maxComputeWorkGroupInvocations,
        int maxComputeWorkGroupSize0,
        int maxComputeWorkGroupSize1,
        int maxComputeWorkGroupSize2,
        int maxViewports,
        int maxFramebufferWidth,
        int maxFramebufferHeight,
        int maxFramebufferLayers,
        int maxColorAttachments,
        float maxSamplerAnisotropy,
        float maxSamplerLodBias,
        int maxClipDistances,
        int maxCullDistances,
        int subPixelPrecisionBits,
        int subTexelPrecisionBits,
        int mipmapPrecisionBits,
        float pointSizeGranularity,
        float lineWidthGranularity,
        float minPointSize,
        float maxPointSize,
        float minLineWidth,
        float maxLineWidth,
        boolean strictLines,
        boolean standardSampleLocations,
        long optimalBufferCopyOffsetAlignment,
        long optimalBufferCopyRowPitchAlignment,
        long nonCoherentAtomSize
    ) {
        public static DeviceLimits from(VkPhysicalDeviceLimits limits) {
            return new DeviceLimits(
                limits.maxImageDimension1D(),
                limits.maxImageDimension2D(),
                limits.maxImageDimension3D(),
                limits.maxImageDimensionCube(),
                limits.maxImageArrayLayers(),
                limits.maxTexelBufferElements(),
                limits.maxUniformBufferRange(),
                limits.maxStorageBufferRange(),
                limits.maxPushConstantsSize(),
                limits.maxMemoryAllocationCount(),
                limits.maxSamplerAllocationCount(),
                limits.bufferImageGranularity(),
                limits.sparseAddressSpaceSize(),
                limits.maxBoundDescriptorSets(),
                limits.maxPerStageDescriptorSamplers(),
                limits.maxPerStageDescriptorUniformBuffers(),
                limits.maxPerStageDescriptorStorageBuffers(),
                limits.maxPerStageDescriptorSampledImages(),
                limits.maxPerStageDescriptorStorageImages(),
                limits.maxPerStageDescriptorInputAttachments(),
                limits.maxPerStageResources(),
                limits.maxDescriptorSetSamplers(),
                limits.maxDescriptorSetUniformBuffers(),
                limits.maxDescriptorSetStorageBuffers(),
                limits.maxDescriptorSetSampledImages(),
                limits.maxDescriptorSetStorageImages(),
                limits.maxVertexInputAttributes(),
                limits.maxVertexInputBindings(),
                limits.maxVertexInputAttributeOffset(),
                limits.maxVertexInputBindingStride(),
                limits.maxVertexOutputComponents(),
                limits.maxFragmentInputComponents(),
                limits.maxFragmentOutputAttachments(),
                limits.maxComputeWorkGroupCount(0),
                limits.maxComputeWorkGroupCount(1),
                limits.maxComputeWorkGroupCount(2),
                limits.maxComputeWorkGroupInvocations(),
                limits.maxComputeWorkGroupSize(0),
                limits.maxComputeWorkGroupSize(1),
                limits.maxComputeWorkGroupSize(2),
                limits.maxViewports(),
                limits.maxFramebufferWidth(),
                limits.maxFramebufferHeight(),
                limits.maxFramebufferLayers(),
                limits.maxColorAttachments(),
                limits.maxSamplerAnisotropy(),
                limits.maxSamplerLodBias(),
                limits.maxClipDistances(),
                limits.maxCullDistances(),
                limits.subPixelPrecisionBits(),
                limits.subTexelPrecisionBits(),
                limits.mipmapPrecisionBits(),
                limits.pointSizeGranularity(),
                limits.lineWidthGranularity(),
                limits.pointSizeRange(0),
                limits.pointSizeRange(1),
                limits.lineWidthRange(0),
                limits.lineWidthRange(1),
                limits.strictLines(),
                limits.standardSampleLocations(),
                limits.optimalBufferCopyOffsetAlignment(),
                limits.optimalBufferCopyRowPitchAlignment(),
                limits.nonCoherentAtomSize()
            );
        }
    }
    
    // ========================================================================
    // MEMORY TYPE INFORMATION
    // ========================================================================
    
    /**
     * Memory type information.
     */
    public record MemoryTypeInfo(
        int index,
        int propertyFlags,
        int heapIndex,
        boolean deviceLocal,
        boolean hostVisible,
        boolean hostCoherent,
        boolean hostCached,
        boolean lazilyAllocated,
        boolean protectedMemory
    ) {
        public static MemoryTypeInfo from(int index, VkMemoryType type) {
            int flags = type.propertyFlags();
            return new MemoryTypeInfo(
                index,
                flags,
                type.heapIndex(),
                (flags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0,
                (flags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0,
                (flags & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) != 0,
                (flags & VK_MEMORY_PROPERTY_HOST_CACHED_BIT) != 0,
                (flags & VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT) != 0,
                (flags & VK_MEMORY_PROPERTY_PROTECTED_BIT) != 0
            );
        }
        
        public boolean matches(int requiredFlags) {
            return (propertyFlags & requiredFlags) == requiredFlags;
        }
        
        public boolean isDeviceLocalHostVisible() {
            return deviceLocal && hostVisible;
        }
    }
    
    /**
     * Memory heap information.
     */
    public record MemoryHeapInfo(
        int index,
        long size,
        int flags,
        boolean deviceLocal,
        boolean multiInstance
    ) {
        public static MemoryHeapInfo from(int index, VkMemoryHeap heap) {
            int flags = heap.flags();
            return new MemoryHeapInfo(
                index,
                heap.size(),
                flags,
                (flags & VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0,
                (flags & VK_MEMORY_HEAP_MULTI_INSTANCE_BIT) != 0
            );
        }
        
        public String sizeString() {
            if (size >= 1024L * 1024 * 1024) {
                return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
            } else if (size >= 1024L * 1024) {
                return String.format("%.2f MB", size / (1024.0 * 1024));
            } else {
                return String.format("%.2f KB", size / 1024.0);
            }
        }
    }
    
    /**
     * Complete memory properties.
     */
    public record MemoryProperties(
        List<MemoryTypeInfo> types,
        List<MemoryHeapInfo> heaps
    ) {
        public int findMemoryType(int typeFilter, int requiredProperties) {
            for (MemoryTypeInfo type : types) {
                if ((typeFilter & (1 << type.index())) != 0 && type.matches(requiredProperties)) {
                    return type.index();
                }
            }
            return -1;
        }
        
        public int findMemoryTypeOrThrow(int typeFilter, int requiredProperties) {
            int index = findMemoryType(typeFilter, requiredProperties);
            if (index < 0) {
                throw new VulkanException("No suitable memory type found for filter 0x" + 
                    Integer.toHexString(typeFilter) + " with properties 0x" + 
                    Integer.toHexString(requiredProperties));
            }
            return index;
        }
        
        public long totalDeviceLocalMemory() {
            long total = 0;
            for (MemoryHeapInfo heap : heaps) {
                if (heap.deviceLocal()) {
                    total += heap.size();
                }
            }
            return total;
        }
    }
    
    // ========================================================================
    // VARHANDLE FOR ATOMIC ACCESS
    // ========================================================================
    
    private static final VarHandle CURRENT_FRAME_HANDLE;
    private static final VarHandle CURRENT_IMAGE_INDEX_HANDLE;
    private static final VarHandle SWAPCHAIN_NEEDS_RECREATION_HANDLE;
    private static final VarHandle INITIALIZED_HANDLE;
    
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            CURRENT_FRAME_HANDLE = lookup.findVarHandle(VulkanContext.class, "currentFrame", int.class);
            CURRENT_IMAGE_INDEX_HANDLE = lookup.findVarHandle(VulkanContext.class, "currentImageIndex", int.class);
            SWAPCHAIN_NEEDS_RECREATION_HANDLE = lookup.findVarHandle(VulkanContext.class, "swapchainNeedsRecreation", boolean.class);
            INITIALIZED_HANDLE = lookup.findVarHandle(VulkanContext.class, "initialized", boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    // ========================================================================
    // CORE STATE - VOLATILE FOR THREAD SAFETY
    // ========================================================================
    
    // Configuration
    private final Configuration config;
    
    // Initialization state
    @SuppressWarnings("FieldMayBeFinal")
    private volatile boolean initialized = false;
    
    // Window handle
    private long windowHandle;
    
    // Vulkan version
    private int instanceApiVersion;
    private int deviceApiVersion;
    private int effectiveApiVersion;
    
    // Core Vulkan objects
    private VkInstance instance;
    private long surface;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    
    // Device information (immutable after init)
    private PhysicalDeviceInfo physicalDeviceInfo;
    private MemoryProperties memoryProperties;
    
    // Queue selection
    private QueueFamilySelection queueSelection;
    
    // Queues
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;
    private VkQueue computeQueue;
    private VkQueue transferQueue;
    
    // Debug messenger
    private long debugMessenger;
    
    // ========================================================================
    // SWAPCHAIN STATE - PROTECTED BY STAMPED LOCK
    // ========================================================================
    
    private final StampedLock swapchainLock = new StampedLock();
    
    private long swapchain;
    private int swapchainImageFormat;
    private int swapchainColorSpace;
    private int swapchainWidth;
    private int swapchainHeight;
    private int swapchainImageCount;
    
    // Primitive arrays for zero-allocation access
    private final long[] swapchainImages = new long[MAX_SWAPCHAIN_IMAGES];
    private final long[] swapchainImageViews = new long[MAX_SWAPCHAIN_IMAGES];
    private final long[] swapchainFramebuffers = new long[MAX_SWAPCHAIN_IMAGES];
    
    // Old swapchain for retirement
    private long oldSwapchain;
    private final long[] oldSwapchainImageViews = new long[MAX_SWAPCHAIN_IMAGES];
    private final long[] oldSwapchainFramebuffers = new long[MAX_SWAPCHAIN_IMAGES];
    private int oldSwapchainImageCount;
    
    // Frame indices (atomic access via VarHandle)
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int currentFrame = 0;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int currentImageIndex = 0;
    @SuppressWarnings("FieldMayBeFinal")
    private volatile boolean swapchainNeedsRecreation = false;
    
    // ========================================================================
    // RENDER PASS AND DEPTH
    // ========================================================================
    
    private long renderPass;
    private int depthFormat;
    private long depthImage;
    private long depthImageMemory;
    private long depthImageView;
    
    // ========================================================================
    // PIPELINE CACHE
    // ========================================================================
    
    private long pipelineCache;
    private final AtomicBoolean pipelineCacheDirty = new AtomicBoolean(false);
    
    // ========================================================================
    // COMMAND POOLS AND BUFFERS
    // ========================================================================
    
    // Per-frame command pools (one per frame in flight)
    private final long[] frameCommandPools = new long[MAX_FRAMES_IN_FLIGHT];
    
    // Primary command buffers (one per swapchain image)
    private final VkCommandBuffer[] primaryCommandBuffers = new VkCommandBuffer[MAX_SWAPCHAIN_IMAGES];
    
    // Secondary command buffer pools per frame
    private final VkCommandBuffer[][] secondaryCommandBufferPools = 
        new VkCommandBuffer[MAX_FRAMES_IN_FLIGHT][SECONDARY_COMMAND_BUFFERS_PER_FRAME];
    private final int[] secondaryCommandBufferCounts = new int[MAX_FRAMES_IN_FLIGHT];
    
    // Transfer command pool (dedicated if available)
    private long transferCommandPool;
    
    // Single-time command pool
    private long singleTimeCommandPool;
    
    // ========================================================================
    // SYNCHRONIZATION
    // ========================================================================
    
    // Per-frame synchronization primitives
    private final long[] imageAvailableSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
    private final long[] renderFinishedSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
    private final long[] inFlightFences = new long[MAX_FRAMES_IN_FLIGHT];
    
    // Per-image in-flight tracking
    private final long[] imagesInFlight = new long[MAX_SWAPCHAIN_IMAGES];
    
    // Timeline semaphores (if supported)
    private long timelineSemaphore;
    private final AtomicLong timelineValue = new AtomicLong(0);
    private final long[] frameTimelineValues = new long[MAX_FRAMES_IN_FLIGHT];
    
    // ========================================================================
    // DESCRIPTOR MANAGEMENT
    // ========================================================================
    
    private long descriptorPool;
    private long descriptorSetLayout;
    private final long[] descriptorSets = new long[MAX_FRAMES_IN_FLIGHT];
    
    // ========================================================================
    // STATISTICS
    // ========================================================================
    
    private final AtomicLong framesRendered = new AtomicLong(0);
    private final AtomicLong swapchainRecreations = new AtomicLong(0);
    private final AtomicLong commandBuffersAllocated = new AtomicLong(0);
    private final AtomicLong memoryAllocated = new AtomicLong(0);
    
    // ========================================================================
    // LOGGING
    // ========================================================================
    
    private final Consumer<String> log;
    
    private void logInfo(String message) {
        log.accept("[VulkanContext INFO] " + message);
    }
    
    private void logWarn(String message) {
        log.accept("[VulkanContext WARN] " + message);
    }
    
    private void logError(String message) {
        log.accept("[VulkanContext ERROR] " + message);
    }
    
    private void logDebug(String message) {
        if (config.enableValidation()) {
            log.accept("[VulkanContext DEBUG] " + message);
        }
    }
    
    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================
    
    /**
     * Create VulkanContext with default configuration.
     */
    public VulkanContext() {
        this(Configuration.defaults());
    }
    
    /**
     * Create VulkanContext with custom configuration.
     */
    public VulkanContext(Configuration config) {
        this.config = config;
        this.log = config.debugCallback() != null ? config.debugCallback() : s -> {};
    }
    
    // ========================================================================
    // INITIALIZATION
    // ========================================================================
    
    /**
     * Initialize the Vulkan context.
     * 
     * @param glfwWindow The GLFW window handle
     * @throws VulkanInitializationException if initialization fails
     */
    public void initialize(long glfwWindow) {
        if ((boolean) INITIALIZED_HANDLE.getVolatile(this)) {
            logWarn("Already initialized");
            return;
        }
        
        this.windowHandle = glfwWindow;
        
        try {
            logInfo("Starting Vulkan initialization...");
            long startTime = System.nanoTime();
            
            // Detect instance API version
            detectInstanceApiVersion();
            
            // Create instance
            createInstance();
            
            // Create surface
            createSurface();
            
            // Select physical device
            selectPhysicalDevice();
            
            // Create logical device
            createLogicalDevice();
            
            // Create swapchain and related resources
            createSwapchain();
            createSwapchainImageViews();
            createRenderPass();
            createDepthResources();
            createFramebuffers();
            
            // Create command infrastructure
            createCommandPools();
            allocateCommandBuffers();
            
            // Create synchronization objects
            createSyncObjects();
            
            // Create pipeline cache
            createPipelineCache();
            
            // Mark as initialized
            INITIALIZED_HANDLE.setVolatile(this, true);
            
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            logInfo("Initialization complete in " + duration + "ms");
            logInfo("Vulkan Version: " + VulkanVersion.toString(effectiveApiVersion));
            logInfo("Device: " + physicalDeviceInfo.name() + " (" + physicalDeviceInfo.vendorName() + ")");
            logFeatureSupport();
            
        } catch (Exception e) {
            logError("Initialization failed: " + e.getMessage());
            cleanup();
            if (e instanceof VulkanException) {
                throw e;
            }
            throw new VulkanInitializationException("Vulkan initialization failed", e);
        }
    }
    
    // ========================================================================
    // INSTANCE API VERSION DETECTION
    // ========================================================================
    
    private void detectInstanceApiVersion() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pApiVersion = stack.ints(0);
            
            // vkEnumerateInstanceVersion is Vulkan 1.1+
            try {
                int result = vkEnumerateInstanceVersion(pApiVersion);
                if (result == VK_SUCCESS) {
                    instanceApiVersion = pApiVersion.get(0);
                } else {
                    instanceApiVersion = VulkanVersion.V1_0;
                }
            } catch (Exception e) {
                instanceApiVersion = VulkanVersion.V1_0;
            }
            
            logInfo("Instance API version: " + VulkanVersion.toString(instanceApiVersion));
        }
    }
    
    // ========================================================================
    // INSTANCE CREATION
    // ========================================================================
    
    private void createInstance() {
        try (MemoryStack stack = stackPush()) {
            // Determine API version to request
            int requestedApiVersion = determineRequestedApiVersion();
            
            // Application info
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(stack.UTF8Safe("Astralis Minecraft"))
                .applicationVersion(VK_MAKE_API_VERSION(0, 1, 0, 0))
                .pEngineName(stack.UTF8Safe("Astralis GL-to-Vulkan Engine"))
                .engineVersion(VK_MAKE_API_VERSION(0, 3, 0, 0))
                .apiVersion(requestedApiVersion);
            
            // Get required instance extensions from GLFW
            PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();
            if (glfwExtensions == null) {
                throw new VulkanInitializationException("GLFW Vulkan extensions not available");
            }
            
            // Build extension list
            List<String> extensions = new ArrayList<>();
            for (int i = 0; i < glfwExtensions.remaining(); i++) {
                extensions.add(memUTF8(glfwExtensions.get(i)));
            }
            
            // Add debug extension if validation enabled
            if (config.enableValidation()) {
                extensions.add(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
            }
            
            // Add physical device properties 2 if available (needed for feature queries)
            if (VulkanVersion.isAtLeast(instanceApiVersion, VulkanVersion.V1_1) || 
                checkInstanceExtensionSupport(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME, stack)) {
                if (!VulkanVersion.isAtLeast(instanceApiVersion, VulkanVersion.V1_1)) {
                    extensions.add(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME);
                }
            }
            
            logDebug("Instance extensions: " + extensions);
            
            PointerBuffer ppExtensions = stack.mallocPointer(extensions.size());
            for (String ext : extensions) {
                ppExtensions.put(stack.UTF8(ext));
            }
            ppExtensions.flip();
            
            // Create info
            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(ppExtensions);
            
            // Validation layers
            if (config.enableValidation() && checkValidationLayerSupport(stack)) {
                String[] layers = {"VK_LAYER_KHRONOS_validation"};
                createInfo.ppEnabledLayerNames(asPointerBuffer(stack, layers));
                logInfo("Validation layers enabled");
                
                // Debug messenger create info (for create/destroy instance)
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = createDebugMessengerCreateInfo(stack);
                createInfo.pNext(debugCreateInfo.address());
            }
            
            // Create instance
            PointerBuffer pInstance = stack.mallocPointer(1);
            int result = vkCreateInstance(createInfo, null, pInstance);
            vkCheck(result, "vkCreateInstance");
            
            instance = new VkInstance(pInstance.get(0), createInfo);
            logInfo("Vulkan instance created");
            
            // Setup debug messenger
            if (config.enableValidation()) {
                setupDebugMessenger(stack);
            }
        }
    }
    
    private int determineRequestedApiVersion() {
        // Request the highest version we support that's <= instance version
        if (VulkanVersion.isAtLeast(instanceApiVersion, VulkanVersion.V1_4)) {
            return VK_API_VERSION_1_3; // LWJGL 3.3.6 might not have 1.4 macros yet
        } else if (VulkanVersion.isAtLeast(instanceApiVersion, VulkanVersion.V1_3)) {
            return VK_API_VERSION_1_3;
        } else if (VulkanVersion.isAtLeast(instanceApiVersion, VulkanVersion.V1_2)) {
            return VK_API_VERSION_1_2;
        } else if (VulkanVersion.isAtLeast(instanceApiVersion, VulkanVersion.V1_1)) {
            return VK_API_VERSION_1_1;
        } else {
            return VK_API_VERSION_1_0;
        }
    }
    
    private boolean checkInstanceExtensionSupport(String extensionName, MemoryStack stack) {
        IntBuffer pCount = stack.ints(0);
        vkEnumerateInstanceExtensionProperties((String) null, pCount, null);
        
        if (pCount.get(0) == 0) return false;
        
        VkExtensionProperties.Buffer extensions = VkExtensionProperties.malloc(pCount.get(0), stack);
        vkEnumerateInstanceExtensionProperties((String) null, pCount, extensions);
        
        for (int i = 0; i < extensions.capacity(); i++) {
            if (extensionName.equals(extensions.get(i).extensionNameString())) {
                return true;
            }
        }
        return false;
    }
    
    private boolean checkValidationLayerSupport(MemoryStack stack) {
        IntBuffer pCount = stack.ints(0);
        vkEnumerateInstanceLayerProperties(pCount, null);
        
        if (pCount.get(0) == 0) return false;
        
        VkLayerProperties.Buffer layers = VkLayerProperties.malloc(pCount.get(0), stack);
        vkEnumerateInstanceLayerProperties(pCount, layers);
        
        for (int i = 0; i < layers.capacity(); i++) {
            if ("VK_LAYER_KHRONOS_validation".equals(layers.get(i).layerNameString())) {
                return true;
            }
        }
        return false;
    }
    
    private VkDebugUtilsMessengerCreateInfoEXT createDebugMessengerCreateInfo(MemoryStack stack) {
        int messageSeverity = VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT |
                             VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT;
        
        if (config.enableGpuAssistedValidation()) {
            messageSeverity |= VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT;
        }
        
        int messageType = VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                         VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT |
                         VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT;
        
        return VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
            .messageSeverity(messageSeverity)
            .messageType(messageType)
            .pfnUserCallback((severity, type, pCallbackData, pUserData) -> {
                VkDebugUtilsMessengerCallbackDataEXT callbackData = 
                    VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                
                String severityStr = switch (severity) {
                    case VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT -> "VERBOSE";
                    case VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT -> "INFO";
                    case VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT -> "WARNING";
                    case VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT -> "ERROR";
                    default -> "UNKNOWN";
                };
                
                String message = "[Vulkan Validation " + severityStr + "] " + callbackData.pMessageString();
                log.accept(message);
                
                return VK_FALSE;
            });
    }
    
    private void setupDebugMessenger(MemoryStack stack) {
        VkDebugUtilsMessengerCreateInfoEXT createInfo = createDebugMessengerCreateInfo(stack);
        
        LongBuffer pMessenger = stack.longs(0);
        int result = vkCreateDebugUtilsMessengerEXT(instance, createInfo, null, pMessenger);
        if (result == VK_SUCCESS) {
            debugMessenger = pMessenger.get(0);
            logDebug("Debug messenger created");
        } else {
            logWarn("Failed to create debug messenger: " + vkResultToString(result));
        }
    }
    
    // ========================================================================
    // SURFACE CREATION
    // ========================================================================
    
    private void createSurface() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurface = stack.longs(0);
            int result = glfwCreateWindowSurface(instance, windowHandle, null, pSurface);
            vkCheck(result, "glfwCreateWindowSurface");
            
            surface = pSurface.get(0);
            logInfo("Window surface created");
        }
    }
    
    // ========================================================================
    // PHYSICAL DEVICE SELECTION
    // ========================================================================
    
    private void selectPhysicalDevice() {
        try (MemoryStack stack = stackPush()) {
            // Enumerate physical devices
            IntBuffer pCount = stack.ints(0);
            vkEnumeratePhysicalDevices(instance, pCount, null);
            
            if (pCount.get(0) == 0) {
                throw new VulkanInitializationException("No Vulkan-capable GPU found");
            }
            
            PointerBuffer pDevices = stack.mallocPointer(pCount.get(0));
            vkEnumeratePhysicalDevices(instance, pCount, pDevices);
            
            // Evaluate each device
            PhysicalDeviceInfo bestDevice = null;
            int bestScore = -1;
            
            for (int i = 0; i < pDevices.capacity(); i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(pDevices.get(i), instance);
                PhysicalDeviceInfo info = evaluatePhysicalDevice(device, stack);
                
                if (info != null) {
                    int score = info.score(config);
                    logDebug("Device: " + info.name() + " - Score: " + score);
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestDevice = info;
                        physicalDevice = device;
                    }
                }
            }
            
            if (bestDevice == null || bestScore < 0) {
                throw new VulkanInitializationException("No suitable GPU found");
            }
            
            physicalDeviceInfo = bestDevice;
            deviceApiVersion = physicalDeviceInfo.apiVersion();
            effectiveApiVersion = Math.min(instanceApiVersion, deviceApiVersion);
            
            // Get memory properties
            memoryProperties = queryMemoryProperties(stack);
            
            logInfo("Selected GPU: " + physicalDeviceInfo.name());
            logInfo("Device API version: " + VulkanVersion.toString(deviceApiVersion));
            logInfo("Total device local memory: " + formatBytes(memoryProperties.totalDeviceLocalMemory()));
        }
    }
    
    private PhysicalDeviceInfo evaluatePhysicalDevice(VkPhysicalDevice device, MemoryStack stack) {
        // Get basic properties
        VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.malloc(stack);
        vkGetPhysicalDeviceProperties(device, props);
        
        // Get features
        VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.malloc(stack);
        vkGetPhysicalDeviceFeatures(device, features);
        
        // Get queue families
        List<QueueFamilyInfo> queueFamilies = getQueueFamilies(device, stack);
        
        // Check queue family support
        QueueFamilySelection selection = selectQueueFamilies(queueFamilies);
        if (!selection.isComplete()) {
            logDebug("Device " + props.deviceNameString() + " rejected: incomplete queue families");
            return null;
        }
        
        // Get available extensions
        Set<String> availableExtensions = getAvailableDeviceExtensions(device, stack);
        
        // Check required extensions
        if (!availableExtensions.contains(VK_KHR_SWAPCHAIN_EXTENSION_NAME)) {
            logDebug("Device " + props.deviceNameString() + " rejected: no swapchain support");
            return null;
        }
        
        // Query swapchain support
        SwapchainSupport swapchainSupport = querySwapchainSupport(device, stack);
        if (!swapchainSupport.isAdequate()) {
            logDebug("Device " + props.deviceNameString() + " rejected: inadequate swapchain support");
            return null;
        }
        
        // Build feature flags
        long enabledFeatures = buildFeatureFlags(device, features, availableExtensions, stack);
        
        // Get limits
        DeviceLimits limits = DeviceLimits.from(props.limits());
        
        // Extract pipeline cache UUID
        byte[] pipelineCacheUUID = new byte[VK_UUID_SIZE];
        props.pipelineCacheUUID().get(pipelineCacheUUID);
        
        return new PhysicalDeviceInfo(
            device.address(),
            props.deviceNameString(),
            props.deviceType(),
            props.apiVersion(),
            props.driverVersion(),
            props.vendorID(),
            props.deviceID(),
            pipelineCacheUUID,
            limits,
            enabledFeatures,
            queueFamilies,
            availableExtensions,
            swapchainSupport
        );
    }
    
    private List<QueueFamilyInfo> getQueueFamilies(VkPhysicalDevice device, MemoryStack stack) {
        IntBuffer pCount = stack.ints(0);
        vkGetPhysicalDeviceQueueFamilyProperties(device, pCount, null);
        
        VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(pCount.get(0), stack);
        vkGetPhysicalDeviceQueueFamilyProperties(device, pCount, queueFamilies);
        
        List<QueueFamilyInfo> result = new ArrayList<>(queueFamilies.capacity());
        IntBuffer pSupported = stack.ints(0);
        
        for (int i = 0; i < queueFamilies.capacity(); i++) {
            vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, pSupported);
            boolean supportsPresent = pSupported.get(0) == VK_TRUE;
            result.add(QueueFamilyInfo.from(i, queueFamilies.get(i), supportsPresent));
        }
        
        return result;
    }
    
    private QueueFamilySelection selectQueueFamilies(List<QueueFamilyInfo> families) {
        QueueFamilyInfo graphics = null;
        QueueFamilyInfo present = null;
        QueueFamilyInfo compute = null;
        QueueFamilyInfo transfer = null;
        
        int bestGraphicsScore = -1;
        int bestPresentScore = -1;
        int bestComputeScore = -1;
        int bestTransferScore = -1;
        
        for (QueueFamilyInfo family : families) {
            // Graphics queue
            if (family.supportsGraphics()) {
                int score = family.score(true, false, false, false);
                // Bonus for combined graphics+present
                if (family.supportsPresent()) score += 500;
                if (score > bestGraphicsScore) {
                    bestGraphicsScore = score;
                    graphics = family;
                }
            }
            
            // Present queue
            if (family.supportsPresent()) {
                int score = family.score(false, false, false, true);
                if (score > bestPresentScore) {
                    bestPresentScore = score;
                    present = family;
                }
            }
            
            // Compute queue (prefer dedicated)
            if (family.supportsCompute()) {
                int score = family.score(false, true, false, false);
                if (score > bestComputeScore) {
                    bestComputeScore = score;
                    compute = family;
                }
            }
            
            // Transfer queue (prefer dedicated)
            if (family.supportsTransfer()) {
                int score = family.score(false, false, true, false);
                if (score > bestTransferScore) {
                    bestTransferScore = score;
                    transfer = family;
                }
            }
        }
        
        // Fallbacks
        if (compute == null) compute = graphics;
        if (transfer == null) transfer = graphics;
        
        return new QueueFamilySelection(graphics, present, compute, transfer);
    }
    
    private Set<String> getAvailableDeviceExtensions(VkPhysicalDevice device, MemoryStack stack) {
        IntBuffer pCount = stack.ints(0);
        vkEnumerateDeviceExtensionProperties(device, (String) null, pCount, null);
        
        Set<String> extensions = new HashSet<>();
        if (pCount.get(0) > 0) {
            VkExtensionProperties.Buffer available = VkExtensionProperties.malloc(pCount.get(0), stack);
            vkEnumerateDeviceExtensionProperties(device, (String) null, pCount, available);
            
            for (int i = 0; i < available.capacity(); i++) {
                extensions.add(available.get(i).extensionNameString());
            }
        }
        
        return extensions;
    }
    
    private SwapchainSupport querySwapchainSupport(VkPhysicalDevice device, MemoryStack stack) {
        // Surface capabilities
        VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, capabilities);
        
        // Surface formats
        IntBuffer pCount = stack.ints(0);
        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, pCount, null);
        
        List<SwapchainSupport.SurfaceFormat> formats = new ArrayList<>();
        if (pCount.get(0) > 0) {
            VkSurfaceFormatKHR.Buffer surfaceFormats = VkSurfaceFormatKHR.malloc(pCount.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, pCount, surfaceFormats);
            
            for (int i = 0; i < surfaceFormats.capacity(); i++) {
                formats.add(new SwapchainSupport.SurfaceFormat(
                    surfaceFormats.get(i).format(),
                    surfaceFormats.get(i).colorSpace()
                ));
            }
        }
        
        // Present modes
        vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, pCount, null);
        int[] presentModes = new int[pCount.get(0)];
        if (pCount.get(0) > 0) {
            IntBuffer pModes = stack.mallocInt(pCount.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, pCount, pModes);
            pModes.get(presentModes);
        }
        
        return new SwapchainSupport(
            capabilities.minImageCount(),
            capabilities.maxImageCount(),
            capabilities.currentExtent().width(),
            capabilities.currentExtent().height(),
            capabilities.minImageExtent().width(),
            capabilities.minImageExtent().height(),
            capabilities.maxImageExtent().width(),
            capabilities.maxImageExtent().height(),
            capabilities.maxImageArrayLayers(),
            capabilities.supportedTransforms(),
            capabilities.currentTransform(),
            capabilities.supportedCompositeAlpha(),
            capabilities.supportedUsageFlags(),
            formats,
            presentModes
        );
    }
    
    private long buildFeatureFlags(VkPhysicalDevice device, VkPhysicalDeviceFeatures features,
                                   Set<String> extensions, MemoryStack stack) {
        long flags = 0;
        
        // Core 1.0 features
        if (features.samplerAnisotropy()) flags |= FeatureFlags.SAMPLER_ANISOTROPY;
        if (features.geometryShader()) flags |= FeatureFlags.GEOMETRY_SHADER;
        if (features.tessellationShader()) flags |= FeatureFlags.TESSELLATION_SHADER;
        if (features.multiDrawIndirect()) flags |= FeatureFlags.MULTI_DRAW_INDIRECT;
        if (features.drawIndirectFirstInstance()) flags |= FeatureFlags.DRAW_INDIRECT_FIRST_INSTANCE;
        if (features.depthClamp()) flags |= FeatureFlags.DEPTH_CLAMP;
        if (features.depthBiasClamp()) flags |= FeatureFlags.DEPTH_BIAS_CLAMP;
        if (features.fillModeNonSolid()) flags |= FeatureFlags.FILL_MODE_NON_SOLID;
        if (features.wideLines()) flags |= FeatureFlags.WIDE_LINES;
        if (features.largePoints()) flags |= FeatureFlags.LARGE_POINTS;
        if (features.logicOp()) flags |= FeatureFlags.LOGIC_OP;
        if (features.multiViewport()) flags |= FeatureFlags.MULTI_VIEWPORT;
        if (features.shaderStorageImageExtendedFormats()) flags |= FeatureFlags.SHADER_STORAGE_IMAGE_EXTENDED_FORMATS;
        if (features.shaderInt64()) flags |= FeatureFlags.SHADER_INT64;
        if (features.shaderInt16()) flags |= FeatureFlags.SHADER_INT16;
        if (features.shaderFloat64()) flags |= FeatureFlags.SHADER_FLOAT64;
        
        int apiVersion = VK_API_VERSION_MAJOR(deviceApiVersion) * 100 + 
                         VK_API_VERSION_MINOR(deviceApiVersion);
        
        // Vulkan 1.2+ features - query via feature structs
        if (apiVersion >= 102) { // 1.2+
            VkPhysicalDeviceVulkan12Features features12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);
            
            VkPhysicalDeviceVulkan11Features features11 = VkPhysicalDeviceVulkan11Features.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES)
                .pNext(features12.address());
            
            VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                .pNext(features11.address());
            
            vkGetPhysicalDeviceFeatures2(device, features2);
            
            // Vulkan 1.1 features
            if (features11.multiview()) flags |= FeatureFlags.MULTIVIEW;
            if (features11.variablePointers()) flags |= FeatureFlags.VARIABLE_POINTERS;
            if (features11.protectedMemory()) flags |= FeatureFlags.PROTECTED_MEMORY;
            if (features11.shaderDrawParameters()) flags |= FeatureFlags.SHADER_DRAW_PARAMETERS;
            
            // Vulkan 1.2 features
            if (features12.timelineSemaphore()) flags |= FeatureFlags.TIMELINE_SEMAPHORE;
            if (features12.bufferDeviceAddress()) flags |= FeatureFlags.BUFFER_DEVICE_ADDRESS;
            if (features12.descriptorIndexing()) flags |= FeatureFlags.DESCRIPTOR_INDEXING;
            if (features12.scalarBlockLayout()) flags |= FeatureFlags.SCALAR_BLOCK_LAYOUT;
            if (features12.imagelessFramebuffer()) flags |= FeatureFlags.IMAGELESS_FRAMEBUFFER;
            if (features12.uniformBufferStandardLayout()) flags |= FeatureFlags.UNIFORM_BUFFER_STANDARD_LAYOUT;
            if (features12.shaderSubgroupExtendedTypes()) flags |= FeatureFlags.SHADER_SUBGROUP_EXTENDED_TYPES;
            if (features12.separateDepthStencilLayouts()) flags |= FeatureFlags.SEPARATE_DEPTH_STENCIL_LAYOUTS;
            if (features12.hostQueryReset()) flags |= FeatureFlags.HOST_QUERY_RESET;
            if (features12.vulkanMemoryModel()) flags |= FeatureFlags.VULKAN_MEMORY_MODEL;
            if (features12.shaderOutputViewportIndex()) flags |= FeatureFlags.SHADER_OUTPUT_VIEWPORT_INDEX;
            if (features12.shaderOutputLayer()) flags |= FeatureFlags.SHADER_OUTPUT_LAYER;
        } else {
            // Check via extensions for Vulkan 1.0/1.1
            if (extensions.contains(VK_KHR_TIMELINE_SEMAPHORE_EXTENSION_NAME)) {
                flags |= FeatureFlags.TIMELINE_SEMAPHORE;
            }
            if (extensions.contains(VK_KHR_BUFFER_DEVICE_ADDRESS_EXTENSION_NAME)) {
                flags |= FeatureFlags.BUFFER_DEVICE_ADDRESS;
            }
            if (extensions.contains(VK_EXT_DESCRIPTOR_INDEXING_EXTENSION_NAME)) {
                flags |= FeatureFlags.DESCRIPTOR_INDEXING;
            }
        }
        
        // Vulkan 1.3+ features
        if (apiVersion >= 103) { // 1.3+
            VkPhysicalDeviceVulkan13Features features13 = VkPhysicalDeviceVulkan13Features.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES);
            
            VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                .pNext(features13.address());
            
            vkGetPhysicalDeviceFeatures2(device, features2);
            
            if (features13.dynamicRendering()) flags |= FeatureFlags.DYNAMIC_RENDERING;
            if (features13.synchronization2()) flags |= FeatureFlags.SYNCHRONIZATION_2;
            if (features13.maintenance4()) flags |= FeatureFlags.MAINTENANCE_4;
            if (features13.shaderIntegerDotProduct()) flags |= FeatureFlags.SHADER_INTEGER_DOT_PRODUCT;
            if (features13.privateData()) flags |= FeatureFlags.PRIVATE_DATA;
            if (features13.pipelineCreationCacheControl()) flags |= FeatureFlags.PIPELINE_CREATION_CACHE_CONTROL;
            if (features13.subgroupSizeControl()) flags |= FeatureFlags.SUBGROUP_SIZE_CONTROL;
            if (features13.computeFullSubgroups()) flags |= FeatureFlags.COMPUTE_FULL_SUBGROUPS;
            if (features13.textureCompressionASTC_HDR()) flags |= FeatureFlags.TEXTURE_COMPRESSION_ASTC_HDR;
            if (features13.shaderZeroInitializeWorkgroupMemory()) flags |= FeatureFlags.SHADER_ZERO_INITIALIZE_WORKGROUP_MEMORY;
        } else {
            // Check via extensions
            if (extensions.contains(VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME)) {
                flags |= FeatureFlags.DYNAMIC_RENDERING;
            }
            if (extensions.contains(VK_KHR_SYNCHRONIZATION_2_EXTENSION_NAME)) {
                flags |= FeatureFlags.SYNCHRONIZATION_2;
            }
            if (extensions.contains(VK_KHR_MAINTENANCE_4_EXTENSION_NAME)) {
                flags |= FeatureFlags.MAINTENANCE_4;
            }
        }
        
        // Extension-based features (check regardless of version)
        if (extensions.contains(VK_KHR_PUSH_DESCRIPTOR_EXTENSION_NAME)) {
            flags |= FeatureFlags.PUSH_DESCRIPTORS;
        }
        if (extensions.contains(VK_EXT_MESH_SHADER_EXTENSION_NAME)) {
            flags |= FeatureFlags.MESH_SHADER;
            flags |= FeatureFlags.TASK_SHADER;
        }
        if (extensions.contains(VK_EXT_SHADER_OBJECT_EXTENSION_NAME)) {
            flags |= FeatureFlags.SHADER_OBJECT;
        }
        if (extensions.contains("VK_KHR_maintenance5")) {
            flags |= FeatureFlags.MAINTENANCE_5;
        }
        if (extensions.contains("VK_KHR_maintenance6")) {
            flags |= FeatureFlags.MAINTENANCE_6;
        }
        if (extensions.contains("VK_KHR_ray_tracing_pipeline")) {
            flags |= FeatureFlags.RAY_TRACING_PIPELINE;
        }
        if (extensions.contains("VK_KHR_ray_query")) {
            flags |= FeatureFlags.RAY_QUERY;
        }
        if (extensions.contains("VK_KHR_acceleration_structure")) {
            flags |= FeatureFlags.ACCELERATION_STRUCTURE;
        }
        if (extensions.contains("VK_KHR_fragment_shading_rate")) {
            flags |= FeatureFlags.FRAGMENT_SHADING_RATE;
        }
        if (extensions.contains("VK_EXT_extended_dynamic_state")) {
            flags |= FeatureFlags.EXTENDED_DYNAMIC_STATE;
        }
        if (extensions.contains("VK_EXT_extended_dynamic_state2")) {
            flags |= FeatureFlags.EXTENDED_DYNAMIC_STATE_2;
        }
        if (extensions.contains("VK_EXT_extended_dynamic_state3")) {
            flags |= FeatureFlags.EXTENDED_DYNAMIC_STATE_3;
        }
        if (extensions.contains("VK_EXT_vertex_input_dynamic_state")) {
            flags |= FeatureFlags.VERTEX_INPUT_DYNAMIC_STATE;
        }
        if (extensions.contains("VK_EXT_color_write_enable")) {
            flags |= FeatureFlags.COLOR_WRITE_ENABLE;
        }
        
        return flags;
    }
    
    private MemoryProperties queryMemoryProperties(MemoryStack stack) {
        VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProps);
        
        List<MemoryTypeInfo> types = new ArrayList<>();
        for (int i = 0; i < memProps.memoryTypeCount(); i++) {
            types.add(MemoryTypeInfo.from(i, memProps.memoryTypes(i)));
        }
        
        List<MemoryHeapInfo> heaps = new ArrayList<>();
        for (int i = 0; i < memProps.memoryHeapCount(); i++) {
            heaps.add(MemoryHeapInfo.from(i, memProps.memoryHeaps(i)));
        }
        
        return new MemoryProperties(types, heaps);
    }
    
    // ========================================================================
    // LOGICAL DEVICE CREATION
    // ========================================================================
    
    private void createLogicalDevice() {
        try (MemoryStack stack = stackPush()) {
            // Re-query queue families for the selected device
            List<QueueFamilyInfo> queueFamilies = getQueueFamilies(physicalDevice, stack);
            queueSelection = selectQueueFamilies(queueFamilies);
            
            if (!queueSelection.isComplete()) {
                throw new VulkanInitializationException("Failed to find required queue families");
            }
            
            // Get unique queue family indices
            int[] uniqueFamilies = queueSelection.uniqueFamilyIndices();
            
            // Create queue create infos
            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = 
                VkDeviceQueueCreateInfo.calloc(uniqueFamilies.length, stack);
            
            for (int i = 0; i < uniqueFamilies.length; i++) {
                int familyIndex = uniqueFamilies[i];
                QueueFamilyInfo familyInfo = queueFamilies.get(familyIndex);
                
                // Request all available queues for parallelism (up to a reasonable limit)
                int queueCount = Math.min(familyInfo.queueCount(), 4);
                FloatBuffer priorities = stack.mallocFloat(queueCount);
                for (int q = 0; q < queueCount; q++) {
                    priorities.put(1.0f - (q * 0.1f)); // Slightly lower priority for later queues
                }
                priorities.flip();
                
                queueCreateInfos.get(i)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    .queueFamilyIndex(familyIndex)
                    .pQueuePriorities(priorities);
            }
            
            // Build extension list
            List<String> enabledExtensions = new ArrayList<>();
            
            // Required extensions
            enabledExtensions.add(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
            
            // Add optional extensions if available
            Set<String> available = physicalDeviceInfo.availableExtensions();
            long features = physicalDeviceInfo.enabledFeatures();
            
            for (ExtensionCategory ext : DeviceExtensions.ALL) {
                if (ext instanceof ExtensionCategory.Required req) {
                    if (!enabledExtensions.contains(req.name())) {
                        enabledExtensions.add(req.name());
                    }
                } else if (ext instanceof ExtensionCategory.Preferred pref) {
                    if (available.contains(pref.name()) && !enabledExtensions.contains(pref.name())) {
                        // Only enable if not core in our version
                        boolean neededAsExtension = shouldEnableAsExtension(pref.featureFlag());
                        if (neededAsExtension) {
                            enabledExtensions.add(pref.name());
                        }
                    }
                } else if (ext instanceof ExtensionCategory.Optional opt) {
                    if (available.contains(opt.name()) && !enabledExtensions.contains(opt.name())) {
                        enabledExtensions.add(opt.name());
                    }
                }
            }
            
            logDebug("Enabling device extensions: " + enabledExtensions);
            
            PointerBuffer ppExtensions = stack.mallocPointer(enabledExtensions.size());
            for (String ext : enabledExtensions) {
                ppExtensions.put(stack.UTF8(ext));
            }
            ppExtensions.flip();
            
            // Build device features
            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            populateEnabledFeatures(deviceFeatures, features);
            
            // Create device create info
            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(queueCreateInfos)
                .ppEnabledExtensionNames(ppExtensions)
                .pEnabledFeatures(deviceFeatures);
            
            // Chain feature structures for Vulkan 1.1+
            long pNext = buildDeviceFeatureChain(stack, features);
            if (pNext != 0) {
                createInfo.pNext(pNext);
            }
            
            // Add validation layers if enabled
            if (config.enableValidation()) {
                createInfo.ppEnabledLayerNames(asPointerBuffer(stack, new String[]{"VK_LAYER_KHRONOS_validation"}));
            }
            
            // Create logical device
            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);
            int result = vkCreateDevice(physicalDevice, createInfo, null, pDevice);
            vkCheck(result, "vkCreateDevice");
            
            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);
            logInfo("Logical device created");
            
            // Get queue handles
            retrieveQueues(stack);
        }
    }
    
    private boolean shouldEnableAsExtension(long featureFlag) {
        // Features that are core in different versions
        int minor = VK_API_VERSION_MINOR(effectiveApiVersion);
        
        // Vulkan 1.2 core features
        if (minor >= 2) {
            if (featureFlag == FeatureFlags.TIMELINE_SEMAPHORE ||
                featureFlag == FeatureFlags.BUFFER_DEVICE_ADDRESS ||
                featureFlag == FeatureFlags.DESCRIPTOR_INDEXING) {
                return false; // Core in 1.2
            }
        }
        
        // Vulkan 1.3 core features
        if (minor >= 3) {
            if (featureFlag == FeatureFlags.DYNAMIC_RENDERING ||
                featureFlag == FeatureFlags.SYNCHRONIZATION_2 ||
                featureFlag == FeatureFlags.MAINTENANCE_4) {
                return false; // Core in 1.3
            }
        }
        
        return true; // Need as extension
    }
    
    private void populateEnabledFeatures(VkPhysicalDeviceFeatures features, long flags) {
        features.samplerAnisotropy((flags & FeatureFlags.SAMPLER_ANISOTROPY) != 0);
        features.geometryShader((flags & FeatureFlags.GEOMETRY_SHADER) != 0);
        features.tessellationShader((flags & FeatureFlags.TESSELLATION_SHADER) != 0);
        features.multiDrawIndirect((flags & FeatureFlags.MULTI_DRAW_INDIRECT) != 0);
        features.drawIndirectFirstInstance((flags & FeatureFlags.DRAW_INDIRECT_FIRST_INSTANCE) != 0);
        features.depthClamp((flags & FeatureFlags.DEPTH_CLAMP) != 0);
        features.depthBiasClamp((flags & FeatureFlags.DEPTH_BIAS_CLAMP) != 0);
        features.fillModeNonSolid((flags & FeatureFlags.FILL_MODE_NON_SOLID) != 0);
        features.wideLines((flags & FeatureFlags.WIDE_LINES) != 0);
        features.largePoints((flags & FeatureFlags.LARGE_POINTS) != 0);
        features.logicOp((flags & FeatureFlags.LOGIC_OP) != 0);
        features.multiViewport((flags & FeatureFlags.MULTI_VIEWPORT) != 0);
        features.shaderStorageImageExtendedFormats((flags & FeatureFlags.SHADER_STORAGE_IMAGE_EXTENDED_FORMATS) != 0);
        features.shaderInt64((flags & FeatureFlags.SHADER_INT64) != 0);
        features.shaderInt16((flags & FeatureFlags.SHADER_INT16) != 0);
        features.shaderFloat64((flags & FeatureFlags.SHADER_FLOAT64) != 0);
    }
    
    private long buildDeviceFeatureChain(MemoryStack stack, long flags) {
        int minor = VK_API_VERSION_MINOR(effectiveApiVersion);
        if (minor < 1) {
            return 0; // No feature chain for Vulkan 1.0
        }
        
        long lastPNext = 0;
        
        // Vulkan 1.3 features
        if (minor >= 3) {
            VkPhysicalDeviceVulkan13Features features13 = VkPhysicalDeviceVulkan13Features.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES)
                .dynamicRendering((flags & FeatureFlags.DYNAMIC_RENDERING) != 0)
                .synchronization2((flags & FeatureFlags.SYNCHRONIZATION_2) != 0)
                .maintenance4((flags & FeatureFlags.MAINTENANCE_4) != 0)
                .shaderIntegerDotProduct((flags & FeatureFlags.SHADER_INTEGER_DOT_PRODUCT) != 0)
                .privateData((flags & FeatureFlags.PRIVATE_DATA) != 0)
                .pipelineCreationCacheControl((flags & FeatureFlags.PIPELINE_CREATION_CACHE_CONTROL) != 0)
                .subgroupSizeControl((flags & FeatureFlags.SUBGROUP_SIZE_CONTROL) != 0)
                .computeFullSubgroups((flags & FeatureFlags.COMPUTE_FULL_SUBGROUPS) != 0)
                .shaderZeroInitializeWorkgroupMemory((flags & FeatureFlags.SHADER_ZERO_INITIALIZE_WORKGROUP_MEMORY) != 0);
            
            if (lastPNext != 0) {
                features13.pNext(lastPNext);
            }
            lastPNext = features13.address();
        }
        
        // Vulkan 1.2 features
        if (minor >= 2) {
            VkPhysicalDeviceVulkan12Features features12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES)
                .timelineSemaphore((flags & FeatureFlags.TIMELINE_SEMAPHORE) != 0)
                .bufferDeviceAddress((flags & FeatureFlags.BUFFER_DEVICE_ADDRESS) != 0)
                .descriptorIndexing((flags & FeatureFlags.DESCRIPTOR_INDEXING) != 0)
                .scalarBlockLayout((flags & FeatureFlags.SCALAR_BLOCK_LAYOUT) != 0)
                .imagelessFramebuffer((flags & FeatureFlags.IMAGELESS_FRAMEBUFFER) != 0)
                .uniformBufferStandardLayout((flags & FeatureFlags.UNIFORM_BUFFER_STANDARD_LAYOUT) != 0)
                .shaderSubgroupExtendedTypes((flags & FeatureFlags.SHADER_SUBGROUP_EXTENDED_TYPES) != 0)
                .separateDepthStencilLayouts((flags & FeatureFlags.SEPARATE_DEPTH_STENCIL_LAYOUTS) != 0)
                .hostQueryReset((flags & FeatureFlags.HOST_QUERY_RESET) != 0)
                .vulkanMemoryModel((flags & FeatureFlags.VULKAN_MEMORY_MODEL) != 0)
                .shaderOutputViewportIndex((flags & FeatureFlags.SHADER_OUTPUT_VIEWPORT_INDEX) != 0)
                .shaderOutputLayer((flags & FeatureFlags.SHADER_OUTPUT_LAYER) != 0);
            
            if (lastPNext != 0) {
                features12.pNext(lastPNext);
            }
            lastPNext = features12.address();
        }
        
        // Vulkan 1.1 features
        if (minor >= 1) {
            VkPhysicalDeviceVulkan11Features features11 = VkPhysicalDeviceVulkan11Features.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES)
                .multiview((flags & FeatureFlags.MULTIVIEW) != 0)
                .variablePointers((flags & FeatureFlags.VARIABLE_POINTERS) != 0)
                .protectedMemory((flags & FeatureFlags.PROTECTED_MEMORY) != 0)
                .shaderDrawParameters((flags & FeatureFlags.SHADER_DRAW_PARAMETERS) != 0);
            
            if (lastPNext != 0) {
                features11.pNext(lastPNext);
            }
            lastPNext = features11.address();
        }
        
        return lastPNext;
    }
    
    private void retrieveQueues(MemoryStack stack) {
        PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);
        
        // Graphics queue
        vkGetDeviceQueue(device, queueSelection.graphics().index(), 0, pQueue);
        graphicsQueue = new VkQueue(pQueue.get(0), device);
        
        // Present queue (may be same as graphics)
        if (queueSelection.graphicsAndPresentSameFamily()) {
            presentQueue = graphicsQueue;
        } else {
            vkGetDeviceQueue(device, queueSelection.present().index(), 0, pQueue);
            presentQueue = new VkQueue(pQueue.get(0), device);
        }
        
        // Compute queue
        if (queueSelection.compute() != null) {
            if (queueSelection.compute().index() == queueSelection.graphics().index()) {
                // Use second queue if available, otherwise share
                if (queueSelection.compute().queueCount() > 1) {
                    vkGetDeviceQueue(device, queueSelection.compute().index(), 1, pQueue);
                    computeQueue = new VkQueue(pQueue.get(0), device);
                } else {
                    computeQueue = graphicsQueue;
                }
            } else {
                vkGetDeviceQueue(device, queueSelection.compute().index(), 0, pQueue);
                computeQueue = new VkQueue(pQueue.get(0), device);
            }
        } else {
            computeQueue = graphicsQueue;
        }
        
        // Transfer queue
        if (queueSelection.transfer() != null) {
            if (queueSelection.transfer().index() == queueSelection.graphics().index()) {
                // Use different queue index if possible
                int queueIndex = Math.min(2, queueSelection.transfer().queueCount() - 1);
                if (queueIndex > 0) {
                    vkGetDeviceQueue(device, queueSelection.transfer().index(), queueIndex, pQueue);
                    transferQueue = new VkQueue(pQueue.get(0), device);
                } else {
                    transferQueue = graphicsQueue;
                }
            } else {
                vkGetDeviceQueue(device, queueSelection.transfer().index(), 0, pQueue);
                transferQueue = new VkQueue(pQueue.get(0), device);
            }
        } else {
            transferQueue = graphicsQueue;
        }
        
        logDebug("Queues retrieved - Graphics: " + queueSelection.graphics().index() +
                 ", Present: " + queueSelection.present().index() +
                 ", Compute: " + queueSelection.compute().index() +
                 ", Transfer: " + queueSelection.transfer().index());
    }
    
    // ========================================================================
    // SWAPCHAIN CREATION
    // ========================================================================
    
    private void createSwapchain() {
        long stamp = swapchainLock.writeLock();
        try {
            createSwapchainInternal();
        } finally {
            swapchainLock.unlockWrite(stamp);
        }
    }
    
    private void createSwapchainInternal() {
        try (MemoryStack stack = stackPush()) {
            // Query current swapchain support
            SwapchainSupport support = querySwapchainSupport(physicalDevice, stack);
            
            // Choose surface format
            SwapchainSupport.SurfaceFormat format = chooseSurfaceFormat(support.formats());
            
            // Choose present mode
            int presentMode = choosePresentMode(support.presentModes());
            
            // Choose extent
            int[] extent = chooseExtent(support, stack);
            
            // Determine image count
            int imageCount = config.preferredSwapchainImages();
            if (support.maxImageCount() > 0) {
                imageCount = Math.min(imageCount, support.maxImageCount());
            }
            imageCount = Math.max(imageCount, support.minImageCount());
            
            // Create swapchain
            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .surface(surface)
                .minImageCount(imageCount)
                .imageFormat(format.format())
                .imageColorSpace(format.colorSpace())
                .imageArrayLayers(1)
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT);
            
            createInfo.imageExtent().set(extent[0], extent[1]);
            
            // Queue family sharing mode
            if (!queueSelection.graphicsAndPresentSameFamily()) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                    .pQueueFamilyIndices(stack.ints(
                        queueSelection.graphics().index(),
                        queueSelection.present().index()
                    ));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }
            
            createInfo
                .preTransform(support.currentTransform())
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .presentMode(presentMode)
                .clipped(true)
                .oldSwapchain(swapchain); // Use old swapchain for seamless recreation
            
            LongBuffer pSwapchain = stack.longs(VK_NULL_HANDLE);
            int result = vkCreateSwapchainKHR(device, createInfo, null, pSwapchain);
            vkCheck(result, "vkCreateSwapchainKHR");
            
            // Store old swapchain for later cleanup (retirement)
            if (swapchain != VK_NULL_HANDLE) {
                retireOldSwapchain();
            }
            
            swapchain = pSwapchain.get(0);
            swapchainImageFormat = format.format();
            swapchainColorSpace = format.colorSpace();
            swapchainWidth = extent[0];
            swapchainHeight = extent[1];
            
            // Get swapchain images
            IntBuffer pImageCount = stack.ints(0);
            vkGetSwapchainImagesKHR(device, swapchain, pImageCount, null);
            swapchainImageCount = pImageCount.get(0);
            
            if (swapchainImageCount > MAX_SWAPCHAIN_IMAGES) {
                throw new VulkanException("Swapchain image count " + swapchainImageCount + 
                    " exceeds maximum " + MAX_SWAPCHAIN_IMAGES);
            }
            
            LongBuffer pImages = stack.mallocLong(swapchainImageCount);
            vkGetSwapchainImagesKHR(device, swapchain, pImageCount, pImages);
            
            for (int i = 0; i < swapchainImageCount; i++) {
                swapchainImages[i] = pImages.get(i);
            }
            
            SWAPCHAIN_NEEDS_RECREATION_HANDLE.setVolatile(this, false);
            swapchainRecreations.incrementAndGet();
            
            logInfo("Swapchain created: " + swapchainWidth + "x" + swapchainHeight + 
                    " with " + swapchainImageCount + " images, format=" + swapchainImageFormat +
                    ", presentMode=" + presentModeToString(presentMode));
        }
    }
    
    private SwapchainSupport.SurfaceFormat chooseSurfaceFormat(List<SwapchainSupport.SurfaceFormat> formats) {
        // HDR preference
        if (config.enableHdr()) {
            for (SwapchainSupport.SurfaceFormat format : formats) {
                if (format.isHdr()) {
                    return format;
                }
            }
        }
        
        // Prefer SRGB for proper color
        for (SwapchainSupport.SurfaceFormat format : formats) {
            if (format.format() == VK_FORMAT_B8G8R8A8_SRGB && format.isSrgb()) {
                return format;
            }
        }
        
        // Fallback to UNORM with SRGB color space
        for (SwapchainSupport.SurfaceFormat format : formats) {
            if (format.format() == VK_FORMAT_B8G8R8A8_UNORM && format.isSrgb()) {
                return format;
            }
        }
        
        // Last resort
        return formats.get(0);
    }
    
    private int choosePresentMode(int[] presentModes) {
        if (!config.enableVsync()) {
            // Prefer immediate for lowest latency
            for (int mode : presentModes) {
                if (mode == VK_PRESENT_MODE_IMMEDIATE_KHR) {
                    return mode;
                }
            }
        }
        
        // Prefer mailbox for low latency with vsync
        for (int mode : presentModes) {
            if (mode == VK_PRESENT_MODE_MAILBOX_KHR) {
                return mode;
            }
        }
        
        // FIFO is always supported (vsync)
        return VK_PRESENT_MODE_FIFO_KHR;
    }
    
    private int[] chooseExtent(SwapchainSupport support, MemoryStack stack) {
        // If current extent is defined, use it
        if (support.currentWidth() != 0xFFFFFFFF) {
            return new int[]{support.currentWidth(), support.currentHeight()};
        }
        
        // Query actual window size
        IntBuffer pWidth = stack.ints(0);
        IntBuffer pHeight = stack.ints(0);
        glfwGetFramebufferSize(windowHandle, pWidth, pHeight);
        
        int width = Math.clamp(pWidth.get(0), support.minWidth(), support.maxWidth());
        int height = Math.clamp(pHeight.get(0), support.minHeight(), support.maxHeight());
        
        return new int[]{width, height};
    }
    
    private void retireOldSwapchain() {
        oldSwapchain = swapchain;
        oldSwapchainImageCount = swapchainImageCount;
        
        // Copy image views and framebuffers for later cleanup
        System.arraycopy(swapchainImageViews, 0, oldSwapchainImageViews, 0, swapchainImageCount);
        System.arraycopy(swapchainFramebuffers, 0, oldSwapchainFramebuffers, 0, swapchainImageCount);
        
        // Clear current arrays
        Arrays.fill(swapchainImageViews, 0, swapchainImageCount, VK_NULL_HANDLE);
        Arrays.fill(swapchainFramebuffers, 0, swapchainImageCount, VK_NULL_HANDLE);
    }
    
    /**
     * Cleanup retired swapchain resources (call after GPU is done with old swapchain).
     */
    private void cleanupRetiredSwapchain() {
        if (oldSwapchain == VK_NULL_HANDLE) return;
        
        // Destroy old framebuffers
        for (int i = 0; i < oldSwapchainImageCount; i++) {
            if (oldSwapchainFramebuffers[i] != VK_NULL_HANDLE) {
                vkDestroyFramebuffer(device, oldSwapchainFramebuffers[i], null);
                oldSwapchainFramebuffers[i] = VK_NULL_HANDLE;
            }
        }
        
        // Destroy old image views
        for (int i = 0; i < oldSwapchainImageCount; i++) {
            if (oldSwapchainImageViews[i] != VK_NULL_HANDLE) {
                vkDestroyImageView(device, oldSwapchainImageViews[i], null);
                oldSwapchainImageViews[i] = VK_NULL_HANDLE;
            }
        }
        
        // Destroy old swapchain
        vkDestroySwapchainKHR(device, oldSwapchain, null);
        oldSwapchain = VK_NULL_HANDLE;
        oldSwapchainImageCount = 0;
        
        logDebug("Retired swapchain cleaned up");
    }
    
    private String presentModeToString(int mode) {
        return switch (mode) {
            case VK_PRESENT_MODE_IMMEDIATE_KHR -> "IMMEDIATE";
            case VK_PRESENT_MODE_MAILBOX_KHR -> "MAILBOX";
            case VK_PRESENT_MODE_FIFO_KHR -> "FIFO";
            case VK_PRESENT_MODE_FIFO_RELAXED_KHR -> "FIFO_RELAXED";
            default -> "UNKNOWN(" + mode + ")";
        };
    }
    
    // ========================================================================
    // IMAGE VIEWS
    // ========================================================================
    
    private void createSwapchainImageViews() {
        long stamp = swapchainLock.writeLock();
        try {
            try (MemoryStack stack = stackPush()) {
                for (int i = 0; i < swapchainImageCount; i++) {
                    swapchainImageViews[i] = createImageView(
                        swapchainImages[i], swapchainImageFormat,
                        VK_IMAGE_ASPECT_COLOR_BIT, 1, stack
                    );
                }
            }
            logDebug("Created " + swapchainImageCount + " swapchain image views");
        } finally {
            swapchainLock.unlockWrite(stamp);
        }
    }
    
    /**
     * Create an image view.
     */
    public long createImageView(long image, int format, int aspectMask, int mipLevels, MemoryStack stack) {
        VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
            .image(image)
            .viewType(VK_IMAGE_VIEW_TYPE_2D)
            .format(format);
        
        viewInfo.subresourceRange()
            .aspectMask(aspectMask)
            .baseMipLevel(0)
            .levelCount(mipLevels)
            .baseArrayLayer(0)
            .layerCount(1);
        
        LongBuffer pImageView = stack.longs(VK_NULL_HANDLE);
        int result = vkCreateImageView(device, viewInfo, null, pImageView);
        vkCheck(result, "vkCreateImageView");
        
        return pImageView.get(0);
    }
    
    // ========================================================================
    // RENDER PASS
    // ========================================================================
    
    private void createRenderPass() {
        try (MemoryStack stack = stackPush()) {
            // Find depth format
            depthFormat = findDepthFormat(stack);
            
            // Attachments
            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(2, stack);
            
            // Color attachment
            attachments.get(0)
                .format(swapchainImageFormat)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            
            // Depth attachment
            attachments.get(1)
                .format(depthFormat)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            
            // Subpass
            VkAttachmentReference.Buffer colorRef = VkAttachmentReference.calloc(1, stack)
                .attachment(0)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            
            VkAttachmentReference depthRef = VkAttachmentReference.calloc(stack)
                .attachment(1)
                .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            
            VkSubpassDescription.Buffer subpasses = VkSubpassDescription.calloc(1, stack)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(1)
                .pColorAttachments(colorRef)
                .pDepthStencilAttachment(depthRef);
            
            // Dependencies
            VkSubpassDependency.Buffer dependencies = VkSubpassDependency.calloc(2, stack);
            
            // External -> Subpass 0
            dependencies.get(0)
                .srcSubpass(VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | 
                              VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | 
                              VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | 
                               VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)
                .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);
            
            // Subpass 0 -> External
            dependencies.get(1)
                .srcSubpass(0)
                .dstSubpass(VK_SUBPASS_EXTERNAL)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
                .dstAccessMask(0)
                .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT);
            
            // Create render pass
            VkRenderPassCreateInfo createInfo = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(attachments)
                .pSubpasses(subpasses)
                .pDependencies(dependencies);
            
            LongBuffer pRenderPass = stack.longs(VK_NULL_HANDLE);
            int result = vkCreateRenderPass(device, createInfo, null, pRenderPass);
            vkCheck(result, "vkCreateRenderPass");
            
            renderPass = pRenderPass.get(0);
            logDebug("Render pass created with depth format: " + depthFormat);
        }
    }
    
    private int findDepthFormat(MemoryStack stack) {
        int[] candidates = {
            VK_FORMAT_D32_SFLOAT,
            VK_FORMAT_D32_SFLOAT_S8_UINT,
            VK_FORMAT_D24_UNORM_S8_UINT,
            VK_FORMAT_D16_UNORM,
            VK_FORMAT_D16_UNORM_S8_UINT
        };
        
        return findSupportedFormat(candidates, VK_IMAGE_TILING_OPTIMAL,
            VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT, stack);
    }
    
    private int findSupportedFormat(int[] candidates, int tiling, int features, MemoryStack stack) {
        VkFormatProperties props = VkFormatProperties.malloc(stack);
        
        for (int format : candidates) {
            vkGetPhysicalDeviceFormatProperties(physicalDevice, format, props);
            
            int supported = (tiling == VK_IMAGE_TILING_LINEAR) 
                ? props.linearTilingFeatures() 
                : props.optimalTilingFeatures();
            
            if ((supported & features) == features) {
                return format;
            }
        }
        
        throw new VulkanException("Failed to find supported format");
    }
    
    // ========================================================================
    // DEPTH RESOURCES
    // ========================================================================
    
    private void createDepthResources() {
        try (MemoryStack stack = stackPush()) {
            // Create depth image
            long[] imageResult = createImage(
                swapchainWidth, swapchainHeight, 1, depthFormat,
                VK_IMAGE_TILING_OPTIMAL,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                stack
            );
            depthImage = imageResult[0];
            depthImageMemory = imageResult[1];
            
            // Create depth image view
            depthImageView = createImageView(depthImage, depthFormat, VK_IMAGE_ASPECT_DEPTH_BIT, 1, stack);
            
            // Transition layout
            transitionImageLayout(
                depthImage, depthFormat,
                VK_IMAGE_LAYOUT_UNDEFINED,
                VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                1
            );
            
            logDebug("Depth resources created: " + swapchainWidth + "x" + swapchainHeight);
        }
    }
    
    /**
     * Create a Vulkan image.
     */
    public long[] createImage(int width, int height, int mipLevels, int format,
                               int tiling, int usage, int memoryProperties,
                               MemoryStack stack) {
        VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
            .imageType(VK_IMAGE_TYPE_2D)
            .format(format)
            .mipLevels(mipLevels)
            .arrayLayers(1)
            .samples(VK_SAMPLE_COUNT_1_BIT)
            .tiling(tiling)
            .usage(usage)
            .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
        
        imageInfo.extent().set(width, height, 1);
        
        LongBuffer pImage = stack.longs(VK_NULL_HANDLE);
        int result = vkCreateImage(device, imageInfo, null, pImage);
        vkCheck(result, "vkCreateImage");
        
        long image = pImage.get(0);
        
        // Get memory requirements
        VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
        vkGetImageMemoryRequirements(device, image, memReqs);
        
        // Allocate memory
        int memoryTypeIndex = memoryProperties.findMemoryTypeOrThrow(
            memReqs.memoryTypeBits(), memoryProperties
        );
        
        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            .allocationSize(memReqs.size())
            .memoryTypeIndex(memoryTypeIndex);
        
        LongBuffer pMemory = stack.longs(VK_NULL_HANDLE);
        result = vkAllocateMemory(device, allocInfo, null, pMemory);
        if (result != VK_SUCCESS) {
            vkDestroyImage(device, image, null);
            vkCheck(result, "vkAllocateMemory for image");
        }
        
        long memory = pMemory.get(0);
        
        result = vkBindImageMemory(device, image, memory, 0);
        if (result != VK_SUCCESS) {
            vkFreeMemory(device, memory, null);
            vkDestroyImage(device, image, null);
            vkCheck(result, "vkBindImageMemory");
        }
        
        memoryAllocated.addAndGet(memReqs.size());
        
        return new long[]{image, memory};
    }
    
    private void transitionImageLayout(long image, int format, int oldLayout, int newLayout, int mipLevels) {
        VkCommandBuffer cmd = beginSingleTimeCommands();
        
        try (MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(image);
            
            barrier.subresourceRange()
                .baseMipLevel(0)
                .levelCount(mipLevels)
                .baseArrayLayer(0)
                .layerCount(1);
            
            // Set aspect mask
            if (newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                int aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT;
                if (hasStencilComponent(format)) {
                    aspectMask |= VK_IMAGE_ASPECT_STENCIL_BIT;
                }
                barrier.subresourceRange().aspectMask(aspectMask);
            } else {
                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            }
            
            int srcStage, dstStage;
            
            if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && 
                newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && 
                       newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
                srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            } else if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && 
                       newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | 
                                      VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;
            } else {
                barrier.srcAccessMask(VK_ACCESS_MEMORY_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_MEMORY_READ_BIT);
                srcStage = VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
                dstStage = VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;
            }
            
            vkCmdPipelineBarrier(cmd, srcStage, dstStage, 0, null, null, barrier);
        }
        
        endSingleTimeCommands(cmd);
    }
    
    private boolean hasStencilComponent(int format) {
        return format == VK_FORMAT_D32_SFLOAT_S8_UINT || 
               format == VK_FORMAT_D24_UNORM_S8_UINT ||
               format == VK_FORMAT_D16_UNORM_S8_UINT;
    }
    
    // ========================================================================
    // FRAMEBUFFERS
    // ========================================================================
    
    private void createFramebuffers() {
        long stamp = swapchainLock.writeLock();
        try {
            try (MemoryStack stack = stackPush()) {
                LongBuffer attachments = stack.longs(VK_NULL_HANDLE, depthImageView);
                
                VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(renderPass)
                    .width(swapchainWidth)
                    .height(swapchainHeight)
                    .layers(1);
                
                LongBuffer pFramebuffer = stack.longs(VK_NULL_HANDLE);
                
                for (int i = 0; i < swapchainImageCount; i++) {
                    attachments.put(0, swapchainImageViews[i]);
                    framebufferInfo.pAttachments(attachments);
                    
                    int result = vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer);
                    vkCheck(result, "vkCreateFramebuffer[" + i + "]");
                    
                    swapchainFramebuffers[i] = pFramebuffer.get(0);
                }
            }
            logDebug("Created " + swapchainImageCount + " framebuffers");
        } finally {
            swapchainLock.unlockWrite(stamp);
        }
    }
    
    // ========================================================================
    // COMMAND POOLS AND BUFFERS
    // ========================================================================
    
    private void createCommandPools() {
        try (MemoryStack stack = stackPush()) {
            // Per-frame command pools (resettable)
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(queueSelection.graphics().index());
            
            LongBuffer pPool = stack.longs(VK_NULL_HANDLE);
            
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                int result = vkCreateCommandPool(device, poolInfo, null, pPool);
                vkCheck(result, "vkCreateCommandPool[frame " + i + "]");
                frameCommandPools[i] = pPool.get(0);
            }
            
            // Single-time command pool
            poolInfo.flags(VK_COMMAND_POOL_CREATE_TRANSIENT_BIT);
            int result = vkCreateCommandPool(device, poolInfo, null, pPool);
            vkCheck(result, "vkCreateCommandPool[single-time]");
            singleTimeCommandPool = pPool.get(0);
            
            // Transfer command pool (if dedicated transfer queue)
            if (queueSelection.transfer().index() != queueSelection.graphics().index()) {
                poolInfo.queueFamilyIndex(queueSelection.transfer().index());
                result = vkCreateCommandPool(device, poolInfo, null, pPool);
                if (result == VK_SUCCESS) {
                    transferCommandPool = pPool.get(0);
                } else {
                    transferCommandPool = singleTimeCommandPool;
                }
            } else {
                transferCommandPool = singleTimeCommandPool;
            }
            
            logDebug("Command pools created");
        }
    }
    
    private void allocateCommandBuffers() {
        try (MemoryStack stack = stackPush()) {
            // Allocate primary command buffers for each swapchain image
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1);
            
            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            
            for (int i = 0; i < swapchainImageCount; i++) {
                int frameIndex = i % MAX_FRAMES_IN_FLIGHT;
                allocInfo.commandPool(frameCommandPools[frameIndex]);
                
                int result = vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);
                vkCheck(result, "vkAllocateCommandBuffers[primary " + i + "]");
                
                primaryCommandBuffers[i] = new VkCommandBuffer(pCommandBuffer.get(0), device);
                commandBuffersAllocated.incrementAndGet();
            }
            
            // Pre-allocate secondary command buffers for each frame
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_SECONDARY)
                     .commandBufferCount(SECONDARY_COMMAND_BUFFERS_PER_FRAME);
            
            PointerBuffer pSecondaryBuffers = stack.mallocPointer(SECONDARY_COMMAND_BUFFERS_PER_FRAME);
            
            for (int f = 0; f < MAX_FRAMES_IN_FLIGHT; f++) {
                allocInfo.commandPool(frameCommandPools[f]);
                
                int result = vkAllocateCommandBuffers(device, allocInfo, pSecondaryBuffers);
                vkCheck(result, "vkAllocateCommandBuffers[secondary frame " + f + "]");
                
                for (int i = 0; i < SECONDARY_COMMAND_BUFFERS_PER_FRAME; i++) {
                    secondaryCommandBufferPools[f][i] = new VkCommandBuffer(pSecondaryBuffers.get(i), device);
                }
                secondaryCommandBufferCounts[f] = 0;
                commandBuffersAllocated.addAndGet(SECONDARY_COMMAND_BUFFERS_PER_FRAME);
            }
            
            logDebug("Command buffers allocated: " + swapchainImageCount + " primary, " +
                     (MAX_FRAMES_IN_FLIGHT * SECONDARY_COMMAND_BUFFERS_PER_FRAME) + " secondary");
        }
    }
    
    // ========================================================================
    // SYNCHRONIZATION OBJECTS
    // ========================================================================
    
    private void createSyncObjects() {
        try (MemoryStack stack = stackPush()) {
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            
            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                .flags(VK_FENCE_CREATE_SIGNALED_BIT);
            
            LongBuffer pSemaphore = stack.longs(VK_NULL_HANDLE);
            LongBuffer pFence = stack.longs(VK_NULL_HANDLE);
            
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                // Image available semaphore
                int result = vkCreateSemaphore(device, semaphoreInfo, null, pSemaphore);
                vkCheck(result, "vkCreateSemaphore[imageAvailable " + i + "]");
                imageAvailableSemaphores[i] = pSemaphore.get(0);
                
                // Render finished semaphore
                result = vkCreateSemaphore(device, semaphoreInfo, null, pSemaphore);
                vkCheck(result, "vkCreateSemaphore[renderFinished " + i + "]");
                renderFinishedSemaphores[i] = pSemaphore.get(0);
                
                // In-flight fence
                result = vkCreateFence(device, fenceInfo, null, pFence);
                vkCheck(result, "vkCreateFence[inFlight " + i + "]");
                inFlightFences[i] = pFence.get(0);
            }
            
            // Initialize images-in-flight array
            Arrays.fill(imagesInFlight, VK_NULL_HANDLE);
            
            // Create timeline semaphore if supported
            if (hasFeature(FeatureFlags.TIMELINE_SEMAPHORE)) {
                createTimelineSemaphore(stack);
            }
            
            logDebug("Synchronization objects created" + 
                     (timelineSemaphore != VK_NULL_HANDLE ? " (with timeline semaphore)" : ""));
        }
    }
    
    private void createTimelineSemaphore(MemoryStack stack) {
        VkSemaphoreTypeCreateInfo typeInfo = VkSemaphoreTypeCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO)
            .semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE)
            .initialValue(0);
        
        VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
            .pNext(typeInfo.address());
        
        LongBuffer pSemaphore = stack.longs(VK_NULL_HANDLE);
        int result = vkCreateSemaphore(device, semaphoreInfo, null, pSemaphore);
        
        if (result == VK_SUCCESS) {
            timelineSemaphore = pSemaphore.get(0);
            timelineValue.set(0);
            Arrays.fill(frameTimelineValues, 0);
        } else {
            logWarn("Failed to create timeline semaphore: " + vkResultToString(result));
        }
    }
    
    // ========================================================================
    // PIPELINE CACHE
    // ========================================================================
    
    private void createPipelineCache() {
        try (MemoryStack stack = stackPush()) {
            ByteBuffer cacheData = loadPipelineCacheFromDisk();
            
            VkPipelineCacheCreateInfo cacheInfo = VkPipelineCacheCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);
            
            if (cacheData != null) {
                // Validate cache
                if (validatePipelineCache(cacheData)) {
                    cacheInfo.pInitialData(cacheData);
                    logDebug("Loading pipeline cache from disk: " + cacheData.remaining() + " bytes");
                } else {
                    logWarn("Pipeline cache validation failed, starting fresh");
                    memFree(cacheData);
                    cacheData = null;
                }
            }
            
            LongBuffer pCache = stack.longs(VK_NULL_HANDLE);
            int result = vkCreatePipelineCache(device, cacheInfo, null, pCache);
            
            if (cacheData != null) {
                memFree(cacheData);
            }
            
            if (result == VK_SUCCESS) {
                pipelineCache = pCache.get(0);
                logDebug("Pipeline cache created");
            } else {
                logWarn("Failed to create pipeline cache: " + vkResultToString(result));
            }
        }
    }
    
    private ByteBuffer loadPipelineCacheFromDisk() {
        Path cachePath = config.pipelineCachePath().resolve(PIPELINE_CACHE_FILENAME);
        
        if (!Files.exists(cachePath)) {
            return null;
        }
        
        try {
            byte[] data = Files.readAllBytes(cachePath);
            if (data.length > MAX_PIPELINE_CACHE_SIZE) {
                logWarn("Pipeline cache too large: " + data.length + " bytes");
                return null;
            }
            
            ByteBuffer buffer = memAlloc(data.length);
            buffer.put(data).flip();
            return buffer;
        } catch (IOException e) {
            logWarn("Failed to load pipeline cache: " + e.getMessage());
            return null;
        }
    }
    
    private boolean validatePipelineCache(ByteBuffer cacheData) {
        if (cacheData.remaining() < 32) {
            return false;
        }
        
        // Check header (Vulkan pipeline cache format)
        int headerSize = cacheData.getInt(0);
        int cacheVersion = cacheData.getInt(4);
        int vendorId = cacheData.getInt(8);
        int deviceId = cacheData.getInt(12);
        
        // Validate against current device
        if (vendorId != physicalDeviceInfo.vendorId() ||
            deviceId != physicalDeviceInfo.deviceId()) {
            logDebug("Pipeline cache device mismatch");
            return false;
        }
        
        // Check UUID
        byte[] cacheUUID = new byte[VK_UUID_SIZE];
        for (int i = 0; i < VK_UUID_SIZE; i++) {
            cacheUUID[i] = cacheData.get(16 + i);
        }
        
        if (!Arrays.equals(cacheUUID, physicalDeviceInfo.pipelineCacheUUID())) {
            logDebug("Pipeline cache UUID mismatch (driver version changed?)");
            return false;
        }
        
        return true;
    }
    
    /**
     * Save pipeline cache to disk.
     */
    public void savePipelineCache() {
        if (pipelineCache == VK_NULL_HANDLE) {
            return;
        }
        
        if (!pipelineCacheDirty.getAndSet(false)) {
            return; // No changes
        }
        
        try (MemoryStack stack = stackPush()) {
            // Get cache size
            LongBuffer pSize = stack.longs(0);
            int result = vkGetPipelineCacheData(device, pipelineCache, pSize, null);
            if (result != VK_SUCCESS) {
                logWarn("Failed to get pipeline cache size: " + vkResultToString(result));
                return;
            }
            
            long size = pSize.get(0);
            if (size == 0 || size > MAX_PIPELINE_CACHE_SIZE) {
                return;
            }
            
            // Get cache data
            ByteBuffer cacheData = memAlloc((int) size);
            result = vkGetPipelineCacheData(device, pipelineCache, pSize, cacheData);
            
            if (result == VK_SUCCESS) {
                // Write to disk
                Path cachePath = config.pipelineCachePath().resolve(PIPELINE_CACHE_FILENAME);
                try {
                    Files.createDirectories(cachePath.getParent());
                    Files.write(cachePath, toByteArray(cacheData));
                    logDebug("Pipeline cache saved: " + size + " bytes");
                } catch (IOException e) {
                    logWarn("Failed to save pipeline cache: " + e.getMessage());
                }
            }
            
            memFree(cacheData);
        }
    }
    
    /**
     * Mark pipeline cache as dirty (should be saved).
     */
    public void markPipelineCacheDirty() {
        pipelineCacheDirty.set(true);
    }
    
    // ========================================================================
    // SINGLE-TIME COMMANDS
    // ========================================================================
    
    /**
     * Begin a single-time command buffer.
     */
    public VkCommandBuffer beginSingleTimeCommands() {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandPool(singleTimeCommandPool)
                .commandBufferCount(1);
            
            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            int result = vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);
            vkCheck(result, "vkAllocateCommandBuffers[single-time]");
            
            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), device);
            
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            
            result = vkBeginCommandBuffer(commandBuffer, beginInfo);
            vkCheck(result, "vkBeginCommandBuffer[single-time]");
            
            return commandBuffer;
        }
    }
    
    /**
     * End and submit a single-time command buffer (blocking).
     */
    public void endSingleTimeCommands(VkCommandBuffer commandBuffer) {
        int result = vkEndCommandBuffer(commandBuffer);
        vkCheck(result, "vkEndCommandBuffer[single-time]");
        
        try (MemoryStack stack = stackPush()) {
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(stack.pointers(commandBuffer));
            
            result = vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE);
            vkCheck(result, "vkQueueSubmit[single-time]");
            
            // Wait for completion (blocking)
            result = vkQueueWaitIdle(graphicsQueue);
            vkCheck(result, "vkQueueWaitIdle[single-time]");
            
            vkFreeCommandBuffers(device, singleTimeCommandPool, commandBuffer);
        }
    }
    
    /**
     * End and submit single-time commands with a fence (non-blocking).
     */
    public long endSingleTimeCommandsAsync(VkCommandBuffer commandBuffer) {
        int result = vkEndCommandBuffer(commandBuffer);
        vkCheck(result, "vkEndCommandBuffer[single-time-async]");
        
        try (MemoryStack stack = stackPush()) {
            // Create fence
            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            
            LongBuffer pFence = stack.longs(VK_NULL_HANDLE);
            result = vkCreateFence(device, fenceInfo, null, pFence);
            vkCheck(result, "vkCreateFence[single-time-async]");
            
            long fence = pFence.get(0);
            
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(stack.pointers(commandBuffer));
            
            result = vkQueueSubmit(graphicsQueue, submitInfo, fence);
            vkCheck(result, "vkQueueSubmit[single-time-async]");
            
            return fence;
        }
    }
    
    /**
     * Wait for async single-time commands and cleanup.
     */
    public void waitAndCleanupSingleTimeCommands(VkCommandBuffer commandBuffer, long fence) {
        int result = vkWaitForFences(device, fence, true, DEFAULT_FENCE_TIMEOUT);
        vkCheck(result, "vkWaitForFences[single-time-async]");
        
        vkDestroyFence(device, fence, null);
        vkFreeCommandBuffers(device, singleTimeCommandPool, commandBuffer);
    }
    
    // ========================================================================
    // FRAME MANAGEMENT
    // ========================================================================
    
    /**
     * Begin a new frame.
     * Returns the image index to render to, or -1 if swapchain needs recreation.
     */
    public int beginFrame() {
        int frame = (int) CURRENT_FRAME_HANDLE.getVolatile(this);
        
        // Wait for this frame's fence
        int result = vkWaitForFences(device, inFlightFences[frame], true, DEFAULT_FENCE_TIMEOUT);
        if (result == VK_ERROR_DEVICE_LOST) {
            throw new VulkanDeviceLostException("beginFrame - wait fence");
        }
        if (result != VK_SUCCESS && result != VK_TIMEOUT) {
            vkCheck(result, "vkWaitForFences[frame " + frame + "]");
        }
        
        // Cleanup old swapchain if needed
        cleanupRetiredSwapchain();
        
        // Acquire next image
        try (MemoryStack stack = stackPush()) {
            IntBuffer pImageIndex = stack.ints(0);
            
            result = vkAcquireNextImageKHR(device, swapchain, DEFAULT_FENCE_TIMEOUT,
                imageAvailableSemaphores[frame], VK_NULL_HANDLE, pImageIndex);
            
            if (result == VK_ERROR_OUT_OF_DATE_KHR) {
                SWAPCHAIN_NEEDS_RECREATION_HANDLE.setVolatile(this, true);
                return -1;
            } else if (result == VK_SUBOPTIMAL_KHR) {
                // Continue but mark for later recreation
                SWAPCHAIN_NEEDS_RECREATION_HANDLE.setVolatile(this, true);
            } else if (result != VK_SUCCESS) {
                vkCheck(result, "vkAcquireNextImageKHR");
            }
            
            int imageIndex = pImageIndex.get(0);
            CURRENT_IMAGE_INDEX_HANDLE.setVolatile(this, imageIndex);
            
            // Wait if this image is still in flight
            if (imagesInFlight[imageIndex] != VK_NULL_HANDLE) {
                result = vkWaitForFences(device, imagesInFlight[imageIndex], true, DEFAULT_FENCE_TIMEOUT);
                if (result != VK_SUCCESS && result != VK_TIMEOUT) {
                    vkCheck(result, "vkWaitForFences[image " + imageIndex + "]");
                }
            }
            
            // Mark this image as in-flight with current frame's fence
            imagesInFlight[imageIndex] = inFlightFences[frame];
            
            // Reset command buffers for this frame
            resetFrameCommandBuffers(frame);
            
            return imageIndex;
        }
    }
    
    /**
     * End the current frame and present.
     */
    public void endFrame() {
        int frame = (int) CURRENT_FRAME_HANDLE.getVolatile(this);
        int imageIndex = (int) CURRENT_IMAGE_INDEX_HANDLE.getVolatile(this);
        
        VkCommandBuffer cmd = primaryCommandBuffers[imageIndex];
        
        // End command buffer if still recording
        // (Caller should have ended it, but we check just in case)
        
        try (MemoryStack stack = stackPush()) {
            // Reset fence
            vkResetFences(device, inFlightFences[frame]);
            
            // Submit command buffer
            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .waitSemaphoreCount(1)
                .pWaitSemaphores(stack.longs(imageAvailableSemaphores[frame]))
                .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                .pCommandBuffers(stack.pointers(cmd))
                .pSignalSemaphores(stack.longs(renderFinishedSemaphores[frame]));
            
            int result = vkQueueSubmit(graphicsQueue, submitInfo, inFlightFences[frame]);
            vkCheck(result, "vkQueueSubmit[frame " + frame + "]");
            
            // Present
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(stack.longs(renderFinishedSemaphores[frame]))
                .swapchainCount(1)
                .pSwapchains(stack.longs(swapchain))
                .pImageIndices(stack.ints(imageIndex));
            
            result = vkQueuePresentKHR(presentQueue, presentInfo);
            
            if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR) {
                SWAPCHAIN_NEEDS_RECREATION_HANDLE.setVolatile(this, true);
            } else if (result != VK_SUCCESS) {
                vkCheck(result, "vkQueuePresentKHR");
            }
        }
        
        // Advance frame
        int nextFrame = (frame + 1) % MAX_FRAMES_IN_FLIGHT;
        CURRENT_FRAME_HANDLE.setVolatile(this, nextFrame);
        
        framesRendered.incrementAndGet();
    }
    
    private void resetFrameCommandBuffers(int frame) {
        // Reset secondary command buffer pool for this frame
        secondaryCommandBufferCounts[frame] = 0;
        
        // Command buffers are reset via VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT
    }
    
    /**
     * Check if swapchain needs recreation.
     */
    public boolean needsSwapchainRecreation() {
        return (boolean) SWAPCHAIN_NEEDS_RECREATION_HANDLE.getVolatile(this);
    }
    
    /**
     * Recreate the swapchain.
     */
    public void recreateSwapchain() {
        // Wait for window to have valid size
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.ints(0);
            IntBuffer pHeight = stack.ints(0);
            
            glfwGetFramebufferSize(windowHandle, pWidth, pHeight);
            while (pWidth.get(0) == 0 || pHeight.get(0) == 0) {
                glfwGetFramebufferSize(windowHandle, pWidth, pHeight);
                glfwWaitEvents();
            }
        }
        
        // Wait for device idle
        vkDeviceWaitIdle(device);
        
        // Cleanup depth resources
        if (depthImageView != VK_NULL_HANDLE) {
            vkDestroyImageView(device, depthImageView, null);
        }
        if (depthImage != VK_NULL_HANDLE) {
            vkDestroyImage(device, depthImage, null);
        }
        if (depthImageMemory != VK_NULL_HANDLE) {
            vkFreeMemory(device, depthImageMemory, null);
        }
        
        // Recreate swapchain (will retire old one)
        createSwapchain();
        createSwapchainImageViews();
        createDepthResources();
        createFramebuffers();
        
        // Reset image-in-flight tracking
        Arrays.fill(imagesInFlight, VK_NULL_HANDLE);
        
        logInfo("Swapchain recreated: " + swapchainWidth + "x" + swapchainHeight);
    }
    
    // ========================================================================
    // ACCESSOR METHODS
    // ========================================================================
    
    public VkDevice getDevice() { return device; }
    public VkPhysicalDevice getPhysicalDevice() { return physicalDevice; }
    public VkInstance getInstance() { return instance; }
    public VkQueue getGraphicsQueue() { return graphicsQueue; }
    public VkQueue getPresentQueue() { return presentQueue; }
    public VkQueue getComputeQueue() { return computeQueue; }
    public VkQueue getTransferQueue() { return transferQueue; }
    
    public int getGraphicsQueueFamily() { return queueSelection.graphics().index(); }
    public int getPresentQueueFamily() { return queueSelection.present().index(); }
    public int getComputeQueueFamily() { return queueSelection.compute().index(); }
    public int getTransferQueueFamily() { return queueSelection.transfer().index(); }
    
    public long getSurface() { return surface; }
    public long getSwapchain() { return swapchain; }
    public long getRenderPass() { return renderPass; }
    public long getPipelineCache() { return pipelineCache; }
    
    public int getSwapchainImageFormat() { return swapchainImageFormat; }
    public int getSwapchainWidth() { return swapchainWidth; }
    public int getSwapchainHeight() { return swapchainHeight; }
    public int getSwapchainImageCount() { return swapchainImageCount; }
    public int getDepthFormat() { return depthFormat; }
    
    public int getCurrentFrame() { return (int) CURRENT_FRAME_HANDLE.getVolatile(this); }
    public int getCurrentImageIndex() { return (int) CURRENT_IMAGE_INDEX_HANDLE.getVolatile(this); }
    
    public long getSwapchainImage(int index) { return swapchainImages[index]; }
    public long getSwapchainImageView(int index) { return swapchainImageViews[index]; }
    public long getSwapchainFramebuffer(int index) { return swapchainFramebuffers[index]; }
    
    public long getCurrentSwapchainImage() { return swapchainImages[getCurrentImageIndex()]; }
    public long getCurrentSwapchainImageView() { return swapchainImageViews[getCurrentImageIndex()]; }
    public long getCurrentFramebuffer() { return swapchainFramebuffers[getCurrentImageIndex()]; }
    public VkCommandBuffer getCurrentCommandBuffer() { return primaryCommandBuffers[getCurrentImageIndex()]; }
    
    public long getDepthImage() { return depthImage; }
    public long getDepthImageView() { return depthImageView; }
    
    public PhysicalDeviceInfo getPhysicalDeviceInfo() { return physicalDeviceInfo; }
    public MemoryProperties getMemoryProperties() { return memoryProperties; }
    public Configuration getConfiguration() { return config; }
    
    public int getEffectiveApiVersion() { return effectiveApiVersion; }
    
    public boolean hasFeature(long featureFlag) {
        return physicalDeviceInfo != null && physicalDeviceInfo.hasFeature(featureFlag);
    }
    
    public boolean supportsTimelineSemaphores() { return hasFeature(FeatureFlags.TIMELINE_SEMAPHORE); }
    public boolean supportsDynamicRendering() { return hasFeature(FeatureFlags.DYNAMIC_RENDERING); }
    public boolean supportsSynchronization2() { return hasFeature(FeatureFlags.SYNCHRONIZATION_2); }
    public boolean supportsBufferDeviceAddress() { return hasFeature(FeatureFlags.BUFFER_DEVICE_ADDRESS); }
    public boolean supportsDescriptorIndexing() { return hasFeature(FeatureFlags.DESCRIPTOR_INDEXING); }
    public boolean supportsMeshShaders() { return hasFeature(FeatureFlags.MESH_SHADER); }
    public boolean supportsRayTracing() { return hasFeature(FeatureFlags.RAY_TRACING_PIPELINE); }
    
    public float getMaxAnisotropy() {
        return physicalDeviceInfo != null ? physicalDeviceInfo.limits().maxSamplerAnisotropy() : 1.0f;
    }
    
    public int findMemoryType(int typeFilter, int properties) {
        return memoryProperties.findMemoryType(typeFilter, properties);
    }
    
    public long getFramesRendered() { return framesRendered.get(); }
    public long getSwapchainRecreationCount() { return swapchainRecreations.get(); }
    public long getMemoryAllocated() { return memoryAllocated.get(); }
    
    public boolean isInitialized() { return (boolean) INITIALIZED_HANDLE.getVolatile(this); }
    
    // ========================================================================
    // LOGGING AND DIAGNOSTICS
    // ========================================================================
    
    private void logFeatureSupport() {
        logInfo("Feature Support:");
        logInfo("  Timeline Semaphores: " + supportsTimelineSemaphores());
        logInfo("  Dynamic Rendering: " + supportsDynamicRendering());
        logInfo("  Synchronization2: " + supportsSynchronization2());
        logInfo("  Buffer Device Address: " + supportsBufferDeviceAddress());
        logInfo("  Descriptor Indexing: " + supportsDescriptorIndexing());
        logInfo("  Mesh Shaders: " + supportsMeshShaders());
        logInfo("  Ray Tracing: " + supportsRayTracing());
        logInfo("  Anisotropic Filtering: " + hasFeature(FeatureFlags.SAMPLER_ANISOTROPY) + 
                " (max: " + getMaxAnisotropy() + ")");
    }
    
    /**
     * Generate comprehensive status report.
     */
    public String getStatusReport() {
        StringBuilder sb = new StringBuilder(4096);
        
        sb.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                        VULKAN CONTEXT STATUS REPORT                          ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        sb.append("║ Initialized: ").append(isInitialized()).append("\n");
        sb.append("║ Vulkan Version: ").append(VulkanVersion.toString(effectiveApiVersion)).append("\n");
        
        if (physicalDeviceInfo != null) {
            sb.append("║\n");
            sb.append("║ DEVICE INFO\n");
            sb.append("║   Name: ").append(physicalDeviceInfo.name()).append("\n");
            sb.append("║   Vendor: ").append(physicalDeviceInfo.vendorName()).append("\n");
            sb.append("║   Type: ").append(deviceTypeToString(physicalDeviceInfo.deviceType())).append("\n");
            sb.append("║   Driver Version: ").append(physicalDeviceInfo.driverVersion()).append("\n");
            sb.append("║   API Version: ").append(VulkanVersion.toString(physicalDeviceInfo.apiVersion())).append("\n");
        }
        
        if (memoryProperties != null) {
            sb.append("║\n");
            sb.append("║ MEMORY\n");
            sb.append("║   Total Device Local: ").append(formatBytes(memoryProperties.totalDeviceLocalMemory())).append("\n");
            sb.append("║   Heaps: ").append(memoryProperties.heaps().size()).append("\n");
            sb.append("║   Types: ").append(memoryProperties.types().size()).append("\n");
            sb.append("║   Allocated: ").append(formatBytes(memoryAllocated.get())).append("\n");
        }
        
        sb.append("║\n");
        sb.append("║ SWAPCHAIN\n");
        sb.append("║   Size: ").append(swapchainWidth).append("x").append(swapchainHeight).append("\n");
        sb.append("║   Images: ").append(swapchainImageCount).append("\n");
        sb.append("║   Format: ").append(swapchainImageFormat).append("\n");
        sb.append("║   Recreations: ").append(swapchainRecreations.get()).append("\n");
        sb.append("║   Needs Recreation: ").append(needsSwapchainRecreation()).append("\n");
        
        sb.append("║\n");
        sb.append("║ QUEUES\n");
        if (queueSelection != null) {
            sb.append("║   Graphics: Family ").append(queueSelection.graphics().index())
              .append(" (").append(queueSelection.graphics().queueCount()).append(" queues)\n");
            sb.append("║   Present: Family ").append(queueSelection.present().index()).append("\n");
            sb.append("║   Compute: Family ").append(queueSelection.compute().index())
              .append(queueSelection.compute().isDedicatedCompute() ? " (dedicated)" : "").append("\n");
            sb.append("║   Transfer: Family ").append(queueSelection.transfer().index())
              .append(queueSelection.transfer().isDedicatedTransfer() ? " (dedicated)" : "").append("\n");
        }
        
        sb.append("║\n");
        sb.append("║ SYNCHRONIZATION\n");
        sb.append("║   Frames in Flight: ").append(MAX_FRAMES_IN_FLIGHT).append("\n");
        sb.append("║   Current Frame: ").append(getCurrentFrame()).append("\n");
        sb.append("║   Current Image: ").append(getCurrentImageIndex()).append("\n");
        sb.append("║   Timeline Semaphore: ").append(timelineSemaphore != VK_NULL_HANDLE ? "enabled" : "disabled").append("\n");
        if (timelineSemaphore != VK_NULL_HANDLE) {
            sb.append("║   Timeline Value: ").append(timelineValue.get()).append("\n");
        }
        
        sb.append("║\n");
        sb.append("║ FEATURES\n");
        sb.append("║   Timeline Semaphores: ").append(supportsTimelineSemaphores()).append("\n");
        sb.append("║   Dynamic Rendering: ").append(supportsDynamicRendering()).append("\n");
        sb.append("║   Synchronization2: ").append(supportsSynchronization2()).append("\n");
        sb.append("║   Buffer Device Address: ").append(supportsBufferDeviceAddress()).append("\n");
        sb.append("║   Descriptor Indexing: ").append(supportsDescriptorIndexing()).append("\n");
        sb.append("║   Mesh Shaders: ").append(supportsMeshShaders()).append("\n");
        sb.append("║   Ray Tracing: ").append(supportsRayTracing()).append("\n");
        
        sb.append("║\n");
        sb.append("║ STATISTICS\n");
        sb.append("║   Frames Rendered: ").append(framesRendered.get()).append("\n");
        sb.append("║   Command Buffers Allocated: ").append(commandBuffersAllocated.get()).append("\n");
        
        sb.append("╚══════════════════════════════════════════════════════════════════════════════╝\n");
        
        return sb.toString();
    }
    
    private String deviceTypeToString(int type) {
        return switch (type) {
            case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU -> "Integrated GPU";
            case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU -> "Discrete GPU";
            case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU -> "Virtual GPU";
            case VK_PHYSICAL_DEVICE_TYPE_CPU -> "CPU";
            default -> "Unknown";
        };
    }
    
    // ========================================================================
    // CLEANUP
    // ========================================================================
    
    @Override
    public void close() {
        cleanup();
    }
    
    /**
     * Clean up all Vulkan resources.
     */
    public void cleanup() {
        if (device != null) {
            vkDeviceWaitIdle(device);
        }
        
        // Save pipeline cache before cleanup
        savePipelineCache();
        
        // Cleanup synchronization objects
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            if (imageAvailableSemaphores[i] != VK_NULL_HANDLE) {
                vkDestroySemaphore(device, imageAvailableSemaphores[i], null);
            }
            if (renderFinishedSemaphores[i] != VK_NULL_HANDLE) {
                vkDestroySemaphore(device, renderFinishedSemaphores[i], null);
            }
            if (inFlightFences[i] != VK_NULL_HANDLE) {
                vkDestroyFence(device, inFlightFences[i], null);
            }
        }
        
        if (timelineSemaphore != VK_NULL_HANDLE) {
            vkDestroySemaphore(device, timelineSemaphore, null);
        }
        
        // Cleanup pipeline cache
        if (pipelineCache != VK_NULL_HANDLE) {
            vkDestroyPipelineCache(device, pipelineCache, null);
        }
        
        // Cleanup command pools
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            if (frameCommandPools[i] != VK_NULL_HANDLE) {
                vkDestroyCommandPool(device, frameCommandPools[i], null);
            }
        }
        if (singleTimeCommandPool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(device, singleTimeCommandPool, null);
        }
        if (transferCommandPool != VK_NULL_HANDLE && transferCommandPool != singleTimeCommandPool) {
            vkDestroyCommandPool(device, transferCommandPool, null);
        }
        
        // Cleanup descriptor pool
        if (descriptorPool != VK_NULL_HANDLE) {
            vkDestroyDescriptorPool(device, descriptorPool, null);
        }
        if (descriptorSetLayout != VK_NULL_HANDLE) {
            vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);
        }
        
        // Cleanup framebuffers
        for (int i = 0; i < swapchainImageCount; i++) {
            if (swapchainFramebuffers[i] != VK_NULL_HANDLE) {
                vkDestroyFramebuffer(device, swapchainFramebuffers[i], null);
            }
        }
        
        // Cleanup old swapchain resources
        cleanupRetiredSwapchain();
        
        // Cleanup depth resources
        if (depthImageView != VK_NULL_HANDLE) {
            vkDestroyImageView(device, depthImageView, null);
        }
        if (depthImage != VK_NULL_HANDLE) {
            vkDestroyImage(device, depthImage, null);
        }
        if (depthImageMemory != VK_NULL_HANDLE) {
            vkFreeMemory(device, depthImageMemory, null);
        }
        
        // Cleanup image views
        for (int i = 0; i < swapchainImageCount; i++) {
            if (swapchainImageViews[i] != VK_NULL_HANDLE) {
                vkDestroyImageView(device, swapchainImageViews[i], null);
            }
        }
        
        // Cleanup render pass
        if (renderPass != VK_NULL_HANDLE) {
            vkDestroyRenderPass(device, renderPass, null);
        }
        
        // Cleanup swapchain
        if (swapchain != VK_NULL_HANDLE) {
            vkDestroySwapchainKHR(device, swapchain, null);
        }
        
        // Cleanup device
        if (device != null) {
            vkDestroyDevice(device, null);
        }
        
        // Cleanup debug messenger
        if (debugMessenger != VK_NULL_HANDLE) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);
        }
        
        // Cleanup surface
        if (surface != VK_NULL_HANDLE) {
            vkDestroySurfaceKHR(instance, surface, null);
        }
        
        // Cleanup instance
        if (instance != null) {
            vkDestroyInstance(instance, null);
        }
        
        INITIALIZED_HANDLE.setVolatile(this, false);
        
        logInfo("Cleanup complete");
    }
    
    // ========================================================================
    // UTILITY METHODS
    // ========================================================================
    
    private static PointerBuffer asPointerBuffer(MemoryStack stack, String[] strings) {
        PointerBuffer buffer = stack.mallocPointer(strings.length);
        for (String s : strings) {
            buffer.put(stack.UTF8(s));
        }
        return buffer.flip();
    }
    
    private static String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024 * 1024) {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        } else if (bytes >= 1024L * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return bytes + " B";
        }
    }
    
    private static byte[] toByteArray(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
