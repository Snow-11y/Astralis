package stellar.snow.astralis.api.vulkan.managers;

import stellar.snow.astralis.Config;
import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.api.vulkan.mapping.VulkanCallMapper;
import stellar.snow.astralis.nexus.vulkan.buffer.ops.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.*;

/**
 * VulkanManager - Ultra-High-Performance Vulkan Rendering Manager
 * 
 * <h2>Architecture Overview</h2>
 * <p>Central dispatcher for all Vulkan operations with these design principles:</p>
 * <ul>
 *   <li>Init-time pipeline selection ONLY - no per-call version branching</li>
 *   <li>Zero per-call allocations in hot paths</li>
 *   <li>MethodHandle dispatch for monomorphic JIT optimization</li>
 *   <li>Aggressive state caching to minimize Vulkan API calls</li>
 *   <li>No emulation - unsupported features throw or no-op based on config</li>
 * </ul>
 * 
 * <h2>Cutting-Edge Features</h2>
 * <ul>
 *   <li><b>GPU-Driven Rendering:</b> Indirect draw, GPU culling, MDI with count</li>
 *   <li><b>Meshlet Support:</b> Task/Mesh shader infrastructure (VK 1.4+)</li>
 *   <li><b>Timeline Semaphores:</b> Fine-grained CPU-GPU synchronization</li>
 *   <li><b>Dynamic Rendering:</b> Renderpass-less rendering (VK 1.3+)</li>
 *   <li><b>Buffer Device Address:</b> Bindless vertex pulling</li>
 *   <li><b>Descriptor Indexing:</b> Bindless texture arrays</li>
 *   <li><b>Synchronization2:</b> Enhanced barrier API</li>
 *   <li><b>Data-Oriented Design:</b> SoA batch operations, cache-friendly iteration</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>Single render thread assumed (standard for Minecraft). Command recording
 * must happen on the render thread. Some operations support async preparation.</p>
 * 
 * <h2>Version Support</h2>
 * <p>Vulkan 1.0 through 1.4 with automatic feature detection and graceful fallbacks.</p>
 * 
 * @author Astralis
 * @version 2.0
 */
public final class VulkanManager {

    // =========================================================================
    // SAFE PUBLICATION
    // =========================================================================
    
    /** Thread-safe published instance (volatile read) */
    private static volatile VulkanManager PUBLISHED;
    
    /** Fast-path instance (no volatile, use only after init confirmed) */
    private static VulkanManager FAST;
    
    /** Check if manager is initialized */
    public static boolean isInitialized() { 
        return PUBLISHED != null; 
    }
    
    /**
     * Safe accessor with single volatile read.
     * @throws IllegalStateException if not initialized
     */
    public static VulkanManager getSafe() {
        VulkanManager m = PUBLISHED;
        if (m == null) {
            throw new IllegalStateException(
                "VulkanManager not initialized. Call VulkanManager.init() after Vulkan context exists.");
        }
        return m;
    }
    
    /**
     * Fast accessor without volatile read.
     * <p><b>WARNING:</b> Only use after confirming init() succeeded!</p>
     */
    public static VulkanManager getFast() { 
        return FAST; 
    }

    // =========================================================================
    // VERSION CONSTANTS
    // =========================================================================
    
    public static final int VULKAN_1_0 = VK_MAKE_VERSION(1, 0, 0);
    public static final int VULKAN_1_1 = VK_MAKE_VERSION(1, 1, 0);
    public static final int VULKAN_1_2 = VK_MAKE_VERSION(1, 2, 0);
    public static final int VULKAN_1_3 = VK_MAKE_VERSION(1, 3, 0);
    public static final int VULKAN_1_4 = VK_MAKE_VERSION(1, 4, 0);

    // =========================================================================
    // VULKAN CONSTANTS (no LWJGL dependency in constants)
    // =========================================================================
    
    // Buffer usage flags
    public static final int VK_USAGE_VERTEX = 0x00000080;
    public static final int VK_USAGE_INDEX = 0x00000040;
    public static final int VK_USAGE_UNIFORM = 0x00000010;
    public static final int VK_USAGE_STORAGE = 0x00000020;
    public static final int VK_USAGE_INDIRECT = 0x00000100;
    public static final int VK_USAGE_TRANSFER_SRC = 0x00000001;
    public static final int VK_USAGE_TRANSFER_DST = 0x00000002;
    
    // Memory property flags
    public static final int VK_MEMORY_DEVICE_LOCAL = 0x00000001;
    public static final int VK_MEMORY_HOST_VISIBLE = 0x00000002;
    public static final int VK_MEMORY_HOST_COHERENT = 0x00000004;
    public static final int VK_MEMORY_HOST_CACHED = 0x00000008;
    
    // Pipeline bind points
    public static final int BIND_POINT_GRAPHICS = 0;
    public static final int BIND_POINT_COMPUTE = 1;
    
    // Primitive topologies
    public static final int TOPOLOGY_POINT_LIST = 0;
    public static final int TOPOLOGY_LINE_LIST = 1;
    public static final int TOPOLOGY_LINE_STRIP = 2;
    public static final int TOPOLOGY_TRIANGLE_LIST = 3;
    public static final int TOPOLOGY_TRIANGLE_STRIP = 4;
    public static final int TOPOLOGY_TRIANGLE_FAN = 5;

    // =========================================================================
    // SETTINGS (immutable after init)
    // =========================================================================
    
    /**
     * Immutable configuration snapshot taken at init time.
     * Uses reflection-friendly getters to avoid hard Config API dependency.
     */
    private static final class Settings {
        // Version limits
        final int maxVulkanVersion;      // Max Vulkan version to use (10, 11, 12, 13, 14)
        
        // Debug & validation
        final boolean debug;              // Enable debug logging
        final boolean validationLayers;   // Enable Vulkan validation layers
        final boolean gpuAssistedValidation; // GPU-assisted validation (slow but thorough)
        
        // Behavior modes
        final boolean strictNoEmulation;  // Throw on unsupported features vs silent no-op
        final boolean throwOnInitFailure; // Throw vs return false on init failure
        
        // State caching
        final boolean cacheBindings;      // Cache buffer/texture bindings
        final boolean cachePipelines;     // Cache pipeline state objects
        final boolean cacheDescriptors;   // Cache descriptor sets
        
        // Advanced features
        final boolean enableTimelineSemaphores;   // Use timeline semaphores if available
        final boolean enableDynamicRendering;     // Use dynamic rendering if available
        final boolean enableSynchronization2;     // Use sync2 barriers if available
        final boolean enableBufferDeviceAddress;  // Use BDA for bindless
        final boolean enableDescriptorIndexing;   // Use descriptor indexing for bindless
        final boolean enableMeshShaders;          // Use mesh shaders if available
        
        // GPU-driven rendering
        final boolean enableGPUDrivenRendering;   // Enable GPU-driven draw submission
        final boolean enableGPUCulling;           // Enable GPU-side culling
        final boolean enableMultiDrawIndirect;    // Use MDI commands
        final boolean enableIndirectCount;        // Use MDI with count (VK 1.2+)
        
        // Performance tuning
        final int maxFramesInFlight;      // Triple buffering by default
        final int commandBatchSize;       // Commands per batch before flush
        final int descriptorPoolSize;     // Pre-allocated descriptor sets
        final int pipelineCacheSize;      // Max cached pipelines
        final int stagingBufferSize;      // Staging buffer for uploads (MB)
        
        // Memory management
        final boolean useDedicatedAllocations;    // Dedicated allocs for large resources
        final boolean useMemoryBudget;            // Track memory budget
        final long maxDeviceMemory;               // Max device memory to use (0 = unlimited)
        
        Settings(Config cfg) {
            // Version
            this.maxVulkanVersion = getInt(cfg, "getMaxVulkanVersion", 14);
            
            // Debug
            this.debug = getBool(cfg, "getDebugMode", false);
            this.validationLayers = getBool(cfg, "getVulkanValidation", false);
            this.gpuAssistedValidation = getBool(cfg, "getGPUAssistedValidation", false);
            
            // Behavior
            this.strictNoEmulation = getBool(cfg, "getStrictNoEmulation", true);
            this.throwOnInitFailure = getBool(cfg, "getThrowOnInitFailure", false);
            
            // Caching
            this.cacheBindings = getBool(cfg, "getCacheBindings", true);
            this.cachePipelines = getBool(cfg, "getCachePipelines", true);
            this.cacheDescriptors = getBool(cfg, "getCacheDescriptors", true);
            
            // Advanced features (enabled by default if available)
            this.enableTimelineSemaphores = getBool(cfg, "getUseTimelineSemaphores", true);
            this.enableDynamicRendering = getBool(cfg, "getUseDynamicRendering", true);
            this.enableSynchronization2 = getBool(cfg, "getUseSynchronization2", true);
            this.enableBufferDeviceAddress = getBool(cfg, "getUseBufferDeviceAddress", true);
            this.enableDescriptorIndexing = getBool(cfg, "getUseDescriptorIndexing", true);
            this.enableMeshShaders = getBool(cfg, "getUseMeshShaders", true);
            
            // GPU-driven
            this.enableGPUDrivenRendering = getBool(cfg, "getUseGPUDrivenRendering", true);
            this.enableGPUCulling = getBool(cfg, "getUseGPUCulling", true);
            this.enableMultiDrawIndirect = getBool(cfg, "getUseMultiDrawIndirect", true);
            this.enableIndirectCount = getBool(cfg, "getUseIndirectCount", true);
            
            // Performance
            this.maxFramesInFlight = getInt(cfg, "getMaxFramesInFlight", 3);
            this.commandBatchSize = getInt(cfg, "getCommandBatchSize", 2048);
            this.descriptorPoolSize = getInt(cfg, "getDescriptorPoolSize", 1024);
            this.pipelineCacheSize = getInt(cfg, "getPipelineCacheSize", 256);
            this.stagingBufferSize = getInt(cfg, "getStagingBufferSizeMB", 64);
            
            // Memory
            this.useDedicatedAllocations = getBool(cfg, "getUseDedicatedAllocations", true);
            this.useMemoryBudget = getBool(cfg, "getUseMemoryBudget", true);
            this.maxDeviceMemory = getLong(cfg, "getMaxDeviceMemory", 0);
        }
        
        private static boolean getBool(Object cfg, String method, boolean def) {
            try {
                return (Boolean) cfg.getClass().getMethod(method).invoke(cfg);
            } catch (Throwable t) {
                return def;
            }
        }
        
        private static int getInt(Object cfg, String method, int def) {
            try {
                return (Integer) cfg.getClass().getMethod(method).invoke(cfg);
            } catch (Throwable t) {
                return def;
            }
        }
        
        private static long getLong(Object cfg, String method, long def) {
            try {
                return (Long) cfg.getClass().getMethod(method).invoke(cfg);
            } catch (Throwable t) {
                return def;
            }
        }
    }

    // =========================================================================
    // STATE CACHE (SoA layout for cache efficiency)
    // =========================================================================
    
    /**
     * High-performance state cache using Structure-of-Arrays layout.
     * <p>Designed for:</p>
     * <ul>
     *   <li>Predictable memory layout</li>
     *   <li>Cache-friendly iteration</li>
     *   <li>Minimal branching in hot paths</li>
     *   <li>Bitfield packing for boolean states</li>
     * </ul>
     */
    private static final class StateCache {
        
        // ---------------------------------------------------------------------
        // Buffer bindings (SoA)
        // ---------------------------------------------------------------------
        
        private static final int MAX_BUFFER_BINDINGS = 32;
        
        /** Buffer handles by binding slot */
        final long[] bufferHandles = new long[MAX_BUFFER_BINDINGS];
        
        /** Buffer offsets by binding slot */
        final long[] bufferOffsets = new long[MAX_BUFFER_BINDINGS];
        
        /** Buffer sizes by binding slot */
        final long[] bufferSizes = new long[MAX_BUFFER_BINDINGS];
        
        /** Binding validity bitmap (1 bit per binding) */
        int bufferBindingMask = 0;
        
        // ---------------------------------------------------------------------
        // Texture bindings (SoA)
        // ---------------------------------------------------------------------
        
        private static final int MAX_TEXTURE_UNITS = 32;
        
        /** Image view handles by unit */
        final long[] textureImageViews = new long[MAX_TEXTURE_UNITS];
        
        /** Sampler handles by unit */
        final long[] textureSamplers = new long[MAX_TEXTURE_UNITS];
        
        /** Binding validity bitmap */
        int textureBindingMask = 0;
        
        /** Currently active texture unit */
        int activeTextureUnit = 0;
        
        // ---------------------------------------------------------------------
        // Pipeline state (packed bitfield)
        // ---------------------------------------------------------------------
        
        /** Current graphics pipeline handle */
        long boundGraphicsPipeline = 0;
        
        /** Current compute pipeline handle */
        long boundComputePipeline = 0;
        
        /** Current pipeline layout */
        long currentPipelineLayout = 0;
        
        // ---------------------------------------------------------------------
        // Descriptor set bindings
        // ---------------------------------------------------------------------
        
        private static final int MAX_DESCRIPTOR_SETS = 8;
        
        /** Bound descriptor sets */
        final long[] descriptorSets = new long[MAX_DESCRIPTOR_SETS];
        
        /** Descriptor set validity bitmap */
        int descriptorSetMask = 0;
        
        // ---------------------------------------------------------------------
        // Dynamic state (frequently changed)
        // ---------------------------------------------------------------------
        
        // Viewport (packed into single cache line)
        float viewportX, viewportY, viewportWidth, viewportHeight;
        float viewportMinDepth = 0.0f, viewportMaxDepth = 1.0f;
        
        // Scissor
        int scissorX, scissorY, scissorWidth, scissorHeight;
        
        // Depth
        float depthBiasConstant, depthBiasClamp, depthBiasSlope;
        float depthBoundsMin, depthBoundsMax;
        
        // Stencil
        int stencilCompareMaskFront, stencilCompareMaskBack;
        int stencilWriteMaskFront, stencilWriteMaskBack;
        int stencilReferenceFront, stencilReferenceBack;
        
        // Blend
        final float[] blendConstants = new float[4];
        
        // Line
        float lineWidth = 1.0f;
        
        // ---------------------------------------------------------------------
        // Enable/disable state (packed bitfield)
        // ---------------------------------------------------------------------
        
        /** 
         * Packed capability bits:
         * Bit 0: depth test
         * Bit 1: depth write
         * Bit 2: stencil test
         * Bit 3: blend
         * Bit 4: cull face
         * Bit 5: scissor test
         * Bit 6: depth bounds test
         * Bit 7: depth bias
         * Bit 8: primitive restart
         * Bit 9: rasterizer discard
         * Bit 10-31: reserved
         */
        int capabilityBits = 0;
        
        static final int CAP_DEPTH_TEST = 1 << 0;
        static final int CAP_DEPTH_WRITE = 1 << 1;
        static final int CAP_STENCIL_TEST = 1 << 2;
        static final int CAP_BLEND = 1 << 3;
        static final int CAP_CULL_FACE = 1 << 4;
        static final int CAP_SCISSOR_TEST = 1 << 5;
        static final int CAP_DEPTH_BOUNDS = 1 << 6;
        static final int CAP_DEPTH_BIAS = 1 << 7;
        static final int CAP_PRIMITIVE_RESTART = 1 << 8;
        static final int CAP_RASTERIZER_DISCARD = 1 << 9;
        
        // Extended dynamic state (VK 1.3+)
        int cullMode = VK_CULL_MODE_BACK_BIT;
        int frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;
        int primitiveTopology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
        int depthCompareOp = VK_COMPARE_OP_LESS;
        
        // ---------------------------------------------------------------------
        // Frame management
        // ---------------------------------------------------------------------
        
        int currentFrameIndex = 0;
        long currentCommandBuffer = 0;
        boolean commandBufferRecording = false;
        
        // ---------------------------------------------------------------------
        // Dirty tracking (minimize redundant state sets)
        // ---------------------------------------------------------------------
        
        /** 
         * Dirty bits for state that needs flushing:
         * Bit 0: viewport
         * Bit 1: scissor
         * Bit 2: depth bias
         * Bit 3: blend constants
         * Bit 4: depth bounds
         * Bit 5: stencil compare mask
         * Bit 6: stencil write mask
         * Bit 7: stencil reference
         * Bit 8: line width
         * Bit 9: cull mode (VK 1.3+)
         * Bit 10: front face (VK 1.3+)
         * Bit 11: primitive topology (VK 1.3+)
         * Bit 12: depth test enable (VK 1.3+)
         * Bit 13: depth write enable (VK 1.3+)
         * Bit 14: depth compare op (VK 1.3+)
         */
        int dirtyBits = 0xFFFFFFFF; // All dirty initially
        
        static final int DIRTY_VIEWPORT = 1 << 0;
        static final int DIRTY_SCISSOR = 1 << 1;
        static final int DIRTY_DEPTH_BIAS = 1 << 2;
        static final int DIRTY_BLEND_CONSTANTS = 1 << 3;
        static final int DIRTY_DEPTH_BOUNDS = 1 << 4;
        static final int DIRTY_STENCIL_COMPARE = 1 << 5;
        static final int DIRTY_STENCIL_WRITE = 1 << 6;
        static final int DIRTY_STENCIL_REF = 1 << 7;
        static final int DIRTY_LINE_WIDTH = 1 << 8;
        static final int DIRTY_CULL_MODE = 1 << 9;
        static final int DIRTY_FRONT_FACE = 1 << 10;
        static final int DIRTY_TOPOLOGY = 1 << 11;
        static final int DIRTY_DEPTH_TEST = 1 << 12;
        static final int DIRTY_DEPTH_WRITE = 1 << 13;
        static final int DIRTY_DEPTH_COMPARE = 1 << 14;
        
        // ---------------------------------------------------------------------
        // Methods
        // ---------------------------------------------------------------------
        
        StateCache() {
            invalidateAll();
        }
        
        void invalidateAll() {
            // Clear all bindings
            bufferBindingMask = 0;
            textureBindingMask = 0;
            descriptorSetMask = 0;
            
            for (int i = 0; i < MAX_BUFFER_BINDINGS; i++) {
                bufferHandles[i] = 0;
                bufferOffsets[i] = 0;
                bufferSizes[i] = 0;
            }
            
            for (int i = 0; i < MAX_TEXTURE_UNITS; i++) {
                textureImageViews[i] = 0;
                textureSamplers[i] = 0;
            }
            
            for (int i = 0; i < MAX_DESCRIPTOR_SETS; i++) {
                descriptorSets[i] = 0;
            }
            
            // Clear pipeline bindings
            boundGraphicsPipeline = 0;
            boundComputePipeline = 0;
            currentPipelineLayout = 0;
            
            // Reset dynamic state
            viewportX = viewportY = 0;
            viewportWidth = viewportHeight = 0;
            viewportMinDepth = 0.0f;
            viewportMaxDepth = 1.0f;
            
            scissorX = scissorY = 0;
            scissorWidth = scissorHeight = 0;
            
            depthBiasConstant = depthBiasClamp = depthBiasSlope = 0;
            depthBoundsMin = 0.0f;
            depthBoundsMax = 1.0f;
            
            stencilCompareMaskFront = stencilCompareMaskBack = 0xFF;
            stencilWriteMaskFront = stencilWriteMaskBack = 0xFF;
            stencilReferenceFront = stencilReferenceBack = 0;
            
            blendConstants[0] = blendConstants[1] = blendConstants[2] = blendConstants[3] = 0;
            
            lineWidth = 1.0f;
            
            capabilityBits = CAP_DEPTH_WRITE; // Depth write on by default
            
            cullMode = VK_CULL_MODE_BACK_BIT;
            frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;
            primitiveTopology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            depthCompareOp = VK_COMPARE_OP_LESS;
            
            activeTextureUnit = 0;
            currentFrameIndex = 0;
            currentCommandBuffer = 0;
            commandBufferRecording = false;
            
            // Mark everything dirty
            dirtyBits = 0xFFFFFFFF;
        }
        
        void invalidateBufferBindings() {
            bufferBindingMask = 0;
        }
        
        void invalidateTextureBindings() {
            textureBindingMask = 0;
            activeTextureUnit = 0;
        }
        
        void invalidateDescriptorSets() {
            descriptorSetMask = 0;
        }
        
        void invalidatePipelines() {
            boundGraphicsPipeline = 0;
            boundComputePipeline = 0;
        }
        
        // Capability bit helpers
        boolean hasCapability(int cap) {
            return (capabilityBits & cap) != 0;
        }
        
        void setCapability(int cap, boolean enabled) {
            if (enabled) {
                capabilityBits |= cap;
            } else {
                capabilityBits &= ~cap;
            }
        }
        
        // Dirty bit helpers
        boolean isDirty(int bit) {
            return (dirtyBits & bit) != 0;
        }
        
        void markDirty(int bit) {
            dirtyBits |= bit;
        }
        
        void clearDirty(int bit) {
            dirtyBits &= ~bit;
        }
        
        void markAllDirty() {
            dirtyBits = 0xFFFFFFFF;
        }
    }

    // =========================================================================
    // BUFFER DISPATCH (MethodHandle-based zero-overhead routing)
    // =========================================================================
    
    /**
     * Buffer operations dispatcher using MethodHandles for JIT-friendly dispatch.
     * <p>Supports operations from VulkanBufferOps10 through VulkanBufferOps14.</p>
     */
    private static final class BufferDispatch {
        final Object impl;
        
        // Core operations (required)
        final MethodHandle mhGenBuffer;           // () -> long
        final MethodHandle mhGenBuffers;          // (int) -> long[] (optional)
        final MethodHandle mhDeleteBuffer;        // (long) -> void
        final MethodHandle mhDeleteBuffers;       // (long[]) -> void (optional)
        final MethodHandle mhBindBuffer;          // (int, long) -> void
        
        // Data operations
        final MethodHandle mhBufferData;          // (int, long, int) -> void
        final MethodHandle mhBufferDataBB;        // (int, ByteBuffer, int) -> void
        final MethodHandle mhBufferSubData;       // (int, long, ByteBuffer) -> void
        
        // Mapping operations
        final MethodHandle mhMapBuffer;           // (int, int, long) -> ByteBuffer
        final MethodHandle mhMapBufferRange;      // (int, long, long, int) -> ByteBuffer (optional)
        final MethodHandle mhUnmapBuffer;         // (int) -> boolean
        final MethodHandle mhFlushMappedRange;    // (int, long, long) -> void (optional)
        final MethodHandle mhInvalidateMappedRange; // (int, long, long) -> void (optional)
        
        // Copy operations
        final MethodHandle mhCopyBufferSubData;   // (int, int, long, long, long) -> void (optional)
        
        // Invalidation (VK 1.2+)
        final MethodHandle mhInvalidateBuffer;    // (long) -> void (optional)
        final MethodHandle mhInvalidateBufferSubData; // (long, long, long) -> void (optional)
        
        // Buffer device address (VK 1.2+)
        final MethodHandle mhGetBufferDeviceAddress; // (long) -> long (optional)
        
        // Persistent mapping (VK 1.1+)
        final MethodHandle mhMapBufferPersistent; // (long, long, long) -> ByteBuffer (optional)
        
        // Query
        final MethodHandle mhGetBufferSize;       // (long) -> long (optional)
        final MethodHandle mhGetBufferUsage;      // (long) -> int (optional)
        
        // Lifecycle
        final MethodHandle mhShutdown;            // () -> void (optional)
        
        // Capability flags
        final boolean hasGenBuffers;
        final boolean hasDeleteBuffers;
        final boolean hasMapBufferRange;
        final boolean hasFlushMappedRange;
        final boolean hasInvalidateMappedRange;
        final boolean hasCopyBufferSubData;
        final boolean hasInvalidateBuffer;
        final boolean hasInvalidateBufferSubData;
        final boolean hasBufferDeviceAddress;
        final boolean hasPersistentMapping;
        final boolean hasGetBufferSize;
        final boolean hasGetBufferUsage;
        final boolean hasShutdown;
        
        BufferDispatch(Object impl, MethodHandles.Lookup lookup) {
            this.impl = impl;
            
            // Required core methods
            mhGenBuffer = bindRequired(lookup, impl, "genBuffer", 
                MethodType.methodType(long.class));
            mhDeleteBuffer = bindRequired(lookup, impl, "deleteBuffer", 
                MethodType.methodType(void.class, long.class));
            mhBindBuffer = bindRequired(lookup, impl, "bindBuffer", 
                MethodType.methodType(void.class, int.class, long.class));
            
            mhBufferData = bindRequired(lookup, impl, "bufferData",
                MethodType.methodType(void.class, int.class, long.class, int.class));
            mhBufferDataBB = bindRequired(lookup, impl, "bufferData",
                MethodType.methodType(void.class, int.class, ByteBuffer.class, int.class));
            mhBufferSubData = bindRequired(lookup, impl, "bufferSubData",
                MethodType.methodType(void.class, int.class, long.class, ByteBuffer.class));
            
            // Map buffer - try 3-arg first, then 2-arg
            MethodHandle mapA = bindOptional(lookup, impl, "mapBuffer",
                MethodType.methodType(ByteBuffer.class, int.class, int.class, long.class));
            if (mapA != null) {
                mhMapBuffer = mapA;
            } else {
                MethodHandle mapB = bindOptional(lookup, impl, "mapBuffer",
                    MethodType.methodType(ByteBuffer.class, int.class, int.class));
                mhMapBuffer = requireNonNull(mapB, "mapBuffer");
            }
            
            // Unmap - try boolean return, then void
            MethodHandle unmapA = bindOptional(lookup, impl, "unmapBuffer",
                MethodType.methodType(boolean.class, int.class));
            if (unmapA != null) {
                mhUnmapBuffer = unmapA;
            } else {
                MethodHandle unmapB = bindOptional(lookup, impl, "unmapBuffer",
                    MethodType.methodType(void.class, int.class));
                // Wrap void to return true
                mhUnmapBuffer = requireNonNull(unmapB, "unmapBuffer");
            }
            
            // Optional batch operations
            MethodHandle genBatch = bindOptional(lookup, impl, "genBuffers",
                MethodType.methodType(long[].class, int.class));
            mhGenBuffers = genBatch;
            hasGenBuffers = (genBatch != null);
            
            MethodHandle delBatch = bindOptional(lookup, impl, "deleteBuffers",
                MethodType.methodType(void.class, long[].class));
            mhDeleteBuffers = delBatch;
            hasDeleteBuffers = (delBatch != null);
            
            // Optional advanced mapping
            mhMapBufferRange = bindOptional(lookup, impl, "mapBufferRange",
                MethodType.methodType(ByteBuffer.class, int.class, long.class, long.class, int.class));
            hasMapBufferRange = (mhMapBufferRange != null);
            
            mhFlushMappedRange = bindOptional(lookup, impl, "flushMappedBufferRange",
                MethodType.methodType(void.class, int.class, long.class, long.class));
            hasFlushMappedRange = (mhFlushMappedRange != null);
            
            mhInvalidateMappedRange = bindOptional(lookup, impl, "invalidateMappedBufferRange",
                MethodType.methodType(void.class, int.class, long.class, long.class));
            hasInvalidateMappedRange = (mhInvalidateMappedRange != null);
            
            // Copy
            mhCopyBufferSubData = bindOptional(lookup, impl, "copyBufferSubData",
                MethodType.methodType(void.class, int.class, int.class, long.class, long.class, long.class));
            hasCopyBufferSubData = (mhCopyBufferSubData != null);
            
            // Invalidation
            mhInvalidateBuffer = bindOptional(lookup, impl, "invalidateBufferData",
                MethodType.methodType(void.class, long.class));
            hasInvalidateBuffer = (mhInvalidateBuffer != null);
            
            mhInvalidateBufferSubData = bindOptional(lookup, impl, "invalidateBufferSubData",
                MethodType.methodType(void.class, long.class, long.class, long.class));
            hasInvalidateBufferSubData = (mhInvalidateBufferSubData != null);
            
            // Buffer device address
            mhGetBufferDeviceAddress = bindOptional(lookup, impl, "getBufferDeviceAddress",
                MethodType.methodType(long.class, long.class));
            hasBufferDeviceAddress = (mhGetBufferDeviceAddress != null);
            
            // Persistent mapping
            mhMapBufferPersistent = bindOptional(lookup, impl, "mapBufferPersistent",
                MethodType.methodType(ByteBuffer.class, long.class, long.class, long.class));
            hasPersistentMapping = (mhMapBufferPersistent != null);
            
            // Query
            mhGetBufferSize = bindOptional(lookup, impl, "getBufferSize",
                MethodType.methodType(long.class, long.class));
            hasGetBufferSize = (mhGetBufferSize != null);
            
            mhGetBufferUsage = bindOptional(lookup, impl, "getBufferUsage",
                MethodType.methodType(int.class, long.class));
            hasGetBufferUsage = (mhGetBufferUsage != null);
            
            // Lifecycle
            mhShutdown = bindOptional(lookup, impl, "shutdown",
                MethodType.methodType(void.class));
            hasShutdown = (mhShutdown != null);
        }
        
        // Execution methods with zero-allocation error handling
        
        long genBuffer() {
            try {
                return (long) mhGenBuffer.invokeExact();
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        long[] genBuffers(int count) {
            if (!hasGenBuffers) {
                long[] result = new long[count];
                for (int i = 0; i < count; i++) {
                    result[i] = genBuffer();
                }
                return result;
            }
            try {
                return (long[]) mhGenBuffers.invokeExact(count);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void deleteBuffer(long buffer) {
            try {
                mhDeleteBuffer.invokeExact(buffer);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void deleteBuffers(long[] buffers) {
            if (!hasDeleteBuffers) {
                for (long buffer : buffers) {
                    deleteBuffer(buffer);
                }
                return;
            }
            try {
                mhDeleteBuffers.invokeExact(buffers);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void bindBuffer(int target, long buffer) {
            try {
                mhBindBuffer.invokeExact(target, buffer);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void bufferData(int target, long size, int usage) {
            try {
                mhBufferData.invokeExact(target, size, usage);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void bufferData(int target, ByteBuffer data, int usage) {
            try {
                mhBufferDataBB.invokeExact(target, data, usage);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void bufferSubData(int target, long offset, ByteBuffer data) {
            try {
                mhBufferSubData.invokeExact(target, offset, data);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        ByteBuffer mapBuffer(int target, int access, long length) {
            try {
                return (ByteBuffer) mhMapBuffer.invokeExact(target, access, length);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        ByteBuffer mapBufferRange(int target, long offset, long length, int accessFlags) {
            if (!hasMapBufferRange) return null;
            try {
                return (ByteBuffer) mhMapBufferRange.invokeExact(target, offset, length, accessFlags);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        boolean unmapBuffer(int target) {
            try {
                Object result = mhUnmapBuffer.invoke(target);
                return result instanceof Boolean ? (Boolean) result : true;
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void flushMappedBufferRange(int target, long offset, long length) {
            if (!hasFlushMappedRange) return;
            try {
                mhFlushMappedRange.invokeExact(target, offset, length);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void copyBufferSubData(int readTarget, int writeTarget, long readOffset, 
                               long writeOffset, long size) {
            if (!hasCopyBufferSubData) return;
            try {
                mhCopyBufferSubData.invokeExact(readTarget, writeTarget, readOffset, writeOffset, size);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void invalidateBufferData(long buffer) {
            if (!hasInvalidateBuffer) return;
            try {
                mhInvalidateBuffer.invokeExact(buffer);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void invalidateBufferSubData(long buffer, long offset, long length) {
            if (!hasInvalidateBufferSubData) return;
            try {
                mhInvalidateBufferSubData.invokeExact(buffer, offset, length);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        long getBufferDeviceAddress(long buffer) {
            if (!hasBufferDeviceAddress) return 0;
            try {
                return (long) mhGetBufferDeviceAddress.invokeExact(buffer);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        ByteBuffer mapBufferPersistent(long buffer, long offset, long length) {
            if (!hasPersistentMapping) return null;
            try {
                return (ByteBuffer) mhMapBufferPersistent.invokeExact(buffer, offset, length);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        long getBufferSize(long buffer) {
            if (!hasGetBufferSize) return 0;
            try {
                return (long) mhGetBufferSize.invokeExact(buffer);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void shutdown() {
            if (!hasShutdown) return;
            try {
                mhShutdown.invokeExact();
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        // MethodHandle binding utilities
        
        private static MethodHandle bindRequired(MethodHandles.Lookup lookup, Object impl, 
                                                  String name, MethodType type) {
            MethodHandle mh = bindOptional(lookup, impl, name, type);
            return requireNonNull(mh, name + type);
        }
        
        private static MethodHandle bindOptional(MethodHandles.Lookup lookup, Object impl,
                                                  String name, MethodType type) {
            try {
                return MethodHandles.publicLookup()
                    .findVirtual(impl.getClass(), name, type)
                    .bindTo(impl);
            } catch (Throwable t) {
                return null;
            }
        }
        
        private static MethodHandle requireNonNull(MethodHandle mh, String what) {
            if (mh == null) {
                throw new IllegalStateException("Pipeline missing required method: " + what);
            }
            return mh;
        }
        
        private static RuntimeException rethrow(Throwable t) {
            if (t instanceof RuntimeException) return (RuntimeException) t;
            if (t instanceof Error) throw (Error) t;
            return new RuntimeException(t);
        }
    }

    // =========================================================================
    // DRAW DISPATCH (for draw commands)
    // =========================================================================
    
    /**
     * Draw command dispatcher with support for GPU-driven rendering.
     */
    private static final class DrawDispatch {
        final Object impl;
        
        // Basic draws
        final MethodHandle mhDraw;                    // (int, int, int, int) -> void
        final MethodHandle mhDrawIndexed;             // (int, int, int, int, int) -> void
        
        // Instanced draws
        final MethodHandle mhDrawInstanced;           // (int, int, int, int, int) -> void
        final MethodHandle mhDrawIndexedInstanced;    // (int, int, int, int, int, int) -> void
        
        // Indirect draws (VK 1.0+)
        final MethodHandle mhDrawIndirect;            // (long, long, int, int) -> void
        final MethodHandle mhDrawIndexedIndirect;     // (long, long, int, int) -> void
        
        // Multi-draw indirect with count (VK 1.2+)
        final MethodHandle mhDrawIndirectCount;       // (long, long, long, long, int, int) -> void
        final MethodHandle mhDrawIndexedIndirectCount; // (long, long, long, long, int, int) -> void
        
        // Mesh shaders (VK 1.4+ or extension)
        final MethodHandle mhDrawMeshTasks;           // (int, int, int) -> void
        final MethodHandle mhDrawMeshTasksIndirect;   // (long, long, int, int) -> void
        final MethodHandle mhDrawMeshTasksIndirectCount; // (long, long, long, long, int, int) -> void
        
        // Capability flags
        final boolean hasDrawIndirect;
        final boolean hasDrawIndirectCount;
        final boolean hasMeshShaders;
        
        DrawDispatch(Object impl, MethodHandles.Lookup lookup) {
            this.impl = impl;
            
            // Required basic draws
            mhDraw = bindRequired(lookup, impl, "draw",
                MethodType.methodType(void.class, int.class, int.class, int.class, int.class));
            mhDrawIndexed = bindRequired(lookup, impl, "drawIndexed",
                MethodType.methodType(void.class, int.class, int.class, int.class, int.class, int.class));
            
            // Instanced
            mhDrawInstanced = bindRequired(lookup, impl, "drawInstanced",
                MethodType.methodType(void.class, int.class, int.class, int.class, int.class, int.class));
            mhDrawIndexedInstanced = bindRequired(lookup, impl, "drawIndexedInstanced",
                MethodType.methodType(void.class, int.class, int.class, int.class, int.class, int.class, int.class));
            
            // Indirect
            mhDrawIndirect = bindOptional(lookup, impl, "drawIndirect",
                MethodType.methodType(void.class, long.class, long.class, int.class, int.class));
            mhDrawIndexedIndirect = bindOptional(lookup, impl, "drawIndexedIndirect",
                MethodType.methodType(void.class, long.class, long.class, int.class, int.class));
            hasDrawIndirect = (mhDrawIndirect != null);
            
            // Indirect count
            mhDrawIndirectCount = bindOptional(lookup, impl, "drawIndirectCount",
                MethodType.methodType(void.class, long.class, long.class, long.class, long.class, int.class, int.class));
            mhDrawIndexedIndirectCount = bindOptional(lookup, impl, "drawIndexedIndirectCount",
                MethodType.methodType(void.class, long.class, long.class, long.class, long.class, int.class, int.class));
            hasDrawIndirectCount = (mhDrawIndirectCount != null);
            
            // Mesh shaders
            mhDrawMeshTasks = bindOptional(lookup, impl, "drawMeshTasks",
                MethodType.methodType(void.class, int.class, int.class, int.class));
            mhDrawMeshTasksIndirect = bindOptional(lookup, impl, "drawMeshTasksIndirect",
                MethodType.methodType(void.class, long.class, long.class, int.class, int.class));
            mhDrawMeshTasksIndirectCount = bindOptional(lookup, impl, "drawMeshTasksIndirectCount",
                MethodType.methodType(void.class, long.class, long.class, long.class, long.class, int.class, int.class));
            hasMeshShaders = (mhDrawMeshTasks != null);
        }
        
        void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
            try {
                mhDraw.invokeExact(vertexCount, instanceCount, firstVertex, firstInstance);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void drawIndexed(int indexCount, int instanceCount, int firstIndex, 
                         int vertexOffset, int firstInstance) {
            try {
                mhDrawIndexed.invokeExact(indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void drawIndirect(long buffer, long offset, int drawCount, int stride) {
            if (!hasDrawIndirect) return;
            try {
                mhDrawIndirect.invokeExact(buffer, offset, drawCount, stride);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void drawIndexedIndirect(long buffer, long offset, int drawCount, int stride) {
            if (!hasDrawIndirect) return;
            try {
                mhDrawIndexedIndirect.invokeExact(buffer, offset, drawCount, stride);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void drawIndirectCount(long buffer, long offset, long countBuffer, long countOffset,
                               int maxDrawCount, int stride) {
            if (!hasDrawIndirectCount) return;
            try {
                mhDrawIndirectCount.invokeExact(buffer, offset, countBuffer, countOffset, maxDrawCount, stride);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void drawIndexedIndirectCount(long buffer, long offset, long countBuffer, long countOffset,
                                       int maxDrawCount, int stride) {
            if (!hasDrawIndirectCount) return;
            try {
                mhDrawIndexedIndirectCount.invokeExact(buffer, offset, countBuffer, countOffset, maxDrawCount, stride);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void drawMeshTasks(int groupCountX, int groupCountY, int groupCountZ) {
            if (!hasMeshShaders) return;
            try {
                mhDrawMeshTasks.invokeExact(groupCountX, groupCountY, groupCountZ);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        private static MethodHandle bindRequired(MethodHandles.Lookup lookup, Object impl,
                                                  String name, MethodType type) {
            MethodHandle mh = bindOptional(lookup, impl, name, type);
            if (mh == null) throw new IllegalStateException("Missing: " + name);
            return mh;
        }
        
        private static MethodHandle bindOptional(MethodHandles.Lookup lookup, Object impl,
                                                  String name, MethodType type) {
            try {
                return MethodHandles.publicLookup()
                    .findVirtual(impl.getClass(), name, type)
                    .bindTo(impl);
            } catch (Throwable t) {
                return null;
            }
        }
        
        private static RuntimeException rethrow(Throwable t) {
            if (t instanceof RuntimeException) return (RuntimeException) t;
            if (t instanceof Error) throw (Error) t;
            return new RuntimeException(t);
        }
    }

    // =========================================================================
    // COMPUTE DISPATCH
    // =========================================================================
    
    /**
     * Compute shader dispatcher.
     */
    private static final class ComputeDispatch {
        final Object impl;
        
        final MethodHandle mhDispatch;              // (int, int, int) -> void
        final MethodHandle mhDispatchIndirect;      // (long, long) -> void
        final MethodHandle mhDispatchBase;          // (int, int, int, int, int, int) -> void (VK 1.1+)
        
        final boolean hasDispatchIndirect;
        final boolean hasDispatchBase;
        
        ComputeDispatch(Object impl, MethodHandles.Lookup lookup) {
            this.impl = impl;
            
            mhDispatch = bindRequired(lookup, impl, "dispatch",
                MethodType.methodType(void.class, int.class, int.class, int.class));
            
            mhDispatchIndirect = bindOptional(lookup, impl, "dispatchIndirect",
                MethodType.methodType(void.class, long.class, long.class));
            hasDispatchIndirect = (mhDispatchIndirect != null);
            
            mhDispatchBase = bindOptional(lookup, impl, "dispatchBase",
                MethodType.methodType(void.class, int.class, int.class, int.class, int.class, int.class, int.class));
            hasDispatchBase = (mhDispatchBase != null);
        }
        
        void dispatch(int groupCountX, int groupCountY, int groupCountZ) {
            try {
                mhDispatch.invokeExact(groupCountX, groupCountY, groupCountZ);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void dispatchIndirect(long buffer, long offset) {
            if (!hasDispatchIndirect) return;
            try {
                mhDispatchIndirect.invokeExact(buffer, offset);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void dispatchBase(int baseGroupX, int baseGroupY, int baseGroupZ,
                          int groupCountX, int groupCountY, int groupCountZ) {
            if (!hasDispatchBase) {
                dispatch(groupCountX, groupCountY, groupCountZ);
                return;
            }
            try {
                mhDispatchBase.invokeExact(baseGroupX, baseGroupY, baseGroupZ, 
                                           groupCountX, groupCountY, groupCountZ);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        private static MethodHandle bindRequired(MethodHandles.Lookup lookup, Object impl,
                                                  String name, MethodType type) {
            MethodHandle mh = bindOptional(lookup, impl, name, type);
            if (mh == null) throw new IllegalStateException("Missing: " + name);
            return mh;
        }
        
        private static MethodHandle bindOptional(MethodHandles.Lookup lookup, Object impl,
                                                  String name, MethodType type) {
            try {
                return MethodHandles.publicLookup()
                    .findVirtual(impl.getClass(), name, type)
                    .bindTo(impl);
            } catch (Throwable t) {
                return null;
            }
        }
        
        private static RuntimeException rethrow(Throwable t) {
            if (t instanceof RuntimeException) return (RuntimeException) t;
            if (t instanceof Error) throw (Error) t;
            return new RuntimeException(t);
        }
    }

    // =========================================================================
    // SYNCHRONIZATION DISPATCH
    // =========================================================================
    
    /**
     * Synchronization operations dispatcher with timeline semaphore support.
     */
    private static final class SyncDispatch {
        final Object impl;
        
        // Basic sync
        final MethodHandle mhCreateFence;           // (boolean) -> long
        final MethodHandle mhDestroyFence;          // (long) -> void
        final MethodHandle mhWaitForFence;          // (long, long) -> boolean
        final MethodHandle mhResetFence;            // (long) -> void
        final MethodHandle mhGetFenceStatus;        // (long) -> boolean
        
        // Semaphores
        final MethodHandle mhCreateSemaphore;       // () -> long
        final MethodHandle mhDestroySemaphore;      // (long) -> void
        
        // Timeline semaphores (VK 1.2+)
        final MethodHandle mhCreateTimelineSemaphore; // (long) -> long
        final MethodHandle mhSignalSemaphore;       // (long, long) -> void
        final MethodHandle mhWaitSemaphore;         // (long, long, long) -> boolean
        final MethodHandle mhGetSemaphoreCounterValue; // (long) -> long
        
        // Barriers
        final MethodHandle mhMemoryBarrier;         // (int, int) -> void
        final MethodHandle mhMemoryBarrier2;        // (long, long, long, long) -> void (VK 1.3+)
        final MethodHandle mhPipelineBarrier;       // (int, int, int, ...) -> void
        final MethodHandle mhPipelineBarrier2;      // (VkDependencyInfo) -> void (VK 1.3+)
        
        final boolean hasTimelineSemaphores;
        final boolean hasSynchronization2;
        
        SyncDispatch(Object impl, MethodHandles.Lookup lookup) {
            this.impl = impl;
            
            // Basic fences
            mhCreateFence = bindRequired(lookup, impl, "createFence",
                MethodType.methodType(long.class, boolean.class));
            mhDestroyFence = bindRequired(lookup, impl, "destroyFence",
                MethodType.methodType(void.class, long.class));
            mhWaitForFence = bindRequired(lookup, impl, "waitForFence",
                MethodType.methodType(boolean.class, long.class, long.class));
            mhResetFence = bindRequired(lookup, impl, "resetFence",
                MethodType.methodType(void.class, long.class));
            mhGetFenceStatus = bindRequired(lookup, impl, "getFenceStatus",
                MethodType.methodType(boolean.class, long.class));
            
            // Semaphores
            mhCreateSemaphore = bindRequired(lookup, impl, "createSemaphore",
                MethodType.methodType(long.class));
            mhDestroySemaphore = bindRequired(lookup, impl, "destroySemaphore",
                MethodType.methodType(void.class, long.class));
            
            // Timeline semaphores
            mhCreateTimelineSemaphore = bindOptional(lookup, impl, "createTimelineSemaphore",
                MethodType.methodType(long.class, long.class));
            mhSignalSemaphore = bindOptional(lookup, impl, "signalSemaphore",
                MethodType.methodType(void.class, long.class, long.class));
            mhWaitSemaphore = bindOptional(lookup, impl, "waitSemaphore",
                MethodType.methodType(boolean.class, long.class, long.class, long.class));
            mhGetSemaphoreCounterValue = bindOptional(lookup, impl, "getSemaphoreCounterValue",
                MethodType.methodType(long.class, long.class));
            hasTimelineSemaphores = (mhCreateTimelineSemaphore != null);
            
            // Barriers
            mhMemoryBarrier = bindRequired(lookup, impl, "memoryBarrier",
                MethodType.methodType(void.class, int.class, int.class));
            mhMemoryBarrier2 = bindOptional(lookup, impl, "memoryBarrier2",
                MethodType.methodType(void.class, long.class, long.class, long.class, long.class));
            mhPipelineBarrier = bindRequired(lookup, impl, "pipelineBarrier",
                MethodType.methodType(void.class, int.class, int.class, int.class));
            mhPipelineBarrier2 = bindOptional(lookup, impl, "pipelineBarrier2",
                MethodType.methodType(void.class, long.class));
            hasSynchronization2 = (mhPipelineBarrier2 != null);
        }
        
        long createFence(boolean signaled) {
            try {
                return (long) mhCreateFence.invokeExact(signaled);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void destroyFence(long fence) {
            try {
                mhDestroyFence.invokeExact(fence);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        boolean waitForFence(long fence, long timeout) {
            try {
                return (boolean) mhWaitForFence.invokeExact(fence, timeout);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void resetFence(long fence) {
            try {
                mhResetFence.invokeExact(fence);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        long createSemaphore() {
            try {
                return (long) mhCreateSemaphore.invokeExact();
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void destroySemaphore(long semaphore) {
            try {
                mhDestroySemaphore.invokeExact(semaphore);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        long createTimelineSemaphore(long initialValue) {
            if (!hasTimelineSemaphores) return 0;
            try {
                return (long) mhCreateTimelineSemaphore.invokeExact(initialValue);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void signalSemaphore(long semaphore, long value) {
            if (!hasTimelineSemaphores) return;
            try {
                mhSignalSemaphore.invokeExact(semaphore, value);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        boolean waitSemaphore(long semaphore, long value, long timeout) {
            if (!hasTimelineSemaphores) return true;
            try {
                return (boolean) mhWaitSemaphore.invokeExact(semaphore, value, timeout);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        long getSemaphoreCounterValue(long semaphore) {
            if (!hasTimelineSemaphores) return 0;
            try {
                return (long) mhGetSemaphoreCounterValue.invokeExact(semaphore);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void memoryBarrier(int srcAccessMask, int dstAccessMask) {
            try {
                mhMemoryBarrier.invokeExact(srcAccessMask, dstAccessMask);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        void memoryBarrier2(long srcStageMask, long srcAccessMask, long dstStageMask, long dstAccessMask) {
            if (!hasSynchronization2) {
                memoryBarrier((int) srcAccessMask, (int) dstAccessMask);
                return;
            }
            try {
                mhMemoryBarrier2.invokeExact(srcStageMask, srcAccessMask, dstStageMask, dstAccessMask);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        
        private static MethodHandle bindRequired(MethodHandles.Lookup lookup, Object impl,
                                                  String name, MethodType type) {
            MethodHandle mh = bindOptional(lookup, impl, name, type);
            if (mh == null) throw new IllegalStateException("Missing: " + name);
            return mh;
        }
        
        private static MethodHandle bindOptional(MethodHandles.Lookup lookup, Object impl,
                                                  String name, MethodType type) {
            try {
                return MethodHandles.publicLookup()
                    .findVirtual(impl.getClass(), name, type)
                    .bindTo(impl);
            } catch (Throwable t) {
                return null;
            }
        }
        
        private static RuntimeException rethrow(Throwable t) {
            if (t instanceof RuntimeException) return (RuntimeException) t;
            if (t instanceof Error) throw (Error) t;
            return new RuntimeException(t);
        }
    }

    // =========================================================================
    // INSTANCE FIELDS (all final for JIT optimization)
    // =========================================================================
    
    private final Settings settings;
    private final Thread renderThread;
    private final boolean debug;
    
    // Version info
    private final int detectedVulkanVersion;  // Raw detected version
    private final int effectiveVulkanVersion; // Limited by config
    private final int bufferOpsVersion;       // 10, 11, 12, 13, or 14
    
    // Dispatchers
    private final BufferDispatch bufferDispatch;
    private final DrawDispatch drawDispatch;
    private final ComputeDispatch computeDispatch;
    private final SyncDispatch syncDispatch;
    
    // State
    private final StateCache stateCache;
    
    // Core handles
    private final VulkanContext context;
    private final VulkanState vulkanState;
    
    // Feature availability (detected at init)
    private final boolean hasTimelineSemaphores;
    private final boolean hasDynamicRendering;
    private final boolean hasSynchronization2;
    private final boolean hasBufferDeviceAddress;
    private final boolean hasDescriptorIndexing;
    private final boolean hasMeshShaders;
    private final boolean hasMultiDrawIndirect;
    private final boolean hasIndirectCount;
    
    // Version string for diagnostics
    private final String rawVersionString;
    
    // Frame management
    private final int maxFramesInFlight;
    private final AtomicInteger currentFrame = new AtomicInteger(0);
    private final AtomicLong frameCounter = new AtomicLong(0);
    
    // Timeline semaphore for frame sync
    private final long timelineSemaphore;
    private final AtomicLong timelineValue = new AtomicLong(0);
    
    // Command batching
    private final ConcurrentLinkedQueue<Runnable> deferredCommands = new ConcurrentLinkedQueue<>();
    private final int commandBatchSize;

    // =========================================================================
    // CONSTRUCTOR (private - use init())
    // =========================================================================
    
    private VulkanManager(
            Settings settings,
            int detectedVersion,
            int effectiveVersion,
            int opsVersion,
            BufferDispatch bufferDispatch,
            DrawDispatch drawDispatch,
            ComputeDispatch computeDispatch,
            SyncDispatch syncDispatch,
            StateCache stateCache,
            VulkanContext context,
            VulkanState vulkanState,
            boolean hasTimeline,
            boolean hasDynRender,
            boolean hasSync2,
            boolean hasBDA,
            boolean hasDescIdx,
            boolean hasMesh,
            boolean hasMDI,
            boolean hasIndCount,
            Thread renderThread,
            String rawVersionString,
            long timelineSem) {
        
        this.settings = settings;
        this.renderThread = renderThread;
        this.debug = settings.debug;
        
        this.detectedVulkanVersion = detectedVersion;
        this.effectiveVulkanVersion = effectiveVersion;
        this.bufferOpsVersion = opsVersion;
        
        this.bufferDispatch = bufferDispatch;
        this.drawDispatch = drawDispatch;
        this.computeDispatch = computeDispatch;
        this.syncDispatch = syncDispatch;
        
        this.stateCache = stateCache;
        this.context = context;
        this.vulkanState = vulkanState;
        
        this.hasTimelineSemaphores = hasTimeline;
        this.hasDynamicRendering = hasDynRender;
        this.hasSynchronization2 = hasSync2;
        this.hasBufferDeviceAddress = hasBDA;
        this.hasDescriptorIndexing = hasDescIdx;
        this.hasMeshShaders = hasMesh;
        this.hasMultiDrawIndirect = hasMDI;
        this.hasIndirectCount = hasIndCount;
        
        this.rawVersionString = rawVersionString;
        this.maxFramesInFlight = settings.maxFramesInFlight;
        this.commandBatchSize = settings.commandBatchSize;
        this.timelineSemaphore = timelineSem;
    }

    // =========================================================================
    // INITIALIZATION
    // =========================================================================
    
    /**
     * Initialize VulkanManager.
     * <p>Must be called after Vulkan context is created.</p>
     * 
     * @return true if initialization succeeded
     * @throws RuntimeException if throwOnInitFailure is true and init fails
     */
    public static boolean init() {
        if (PUBLISHED != null) return true;
        
        synchronized (VulkanManager.class) {
            if (PUBLISHED != null) return true;
            
            Settings s = null;
            try {
                // Get configuration
                Config cfg = Config.getInstance();
                s = new Settings(cfg);
                
                // Initialize the call mapper first
                VulkanCallMapper.initialize(VulkanContext.get());
                
                // Get context and state
                VulkanContext context = VulkanContext.get();
                VulkanState vulkanState = new VulkanState();
                
                // Detect Vulkan version
                int detectedVersion = context.vulkanVersion;
                String rawVersionString = formatVulkanVersion(detectedVersion);
                int effectiveVersion = clampVersion(detectedVersion, s.maxVulkanVersion);
                
                // Choose buffer ops version
                int opsVersion = chooseBufferOpsVersion(effectiveVersion);
                
                // Instantiate pipeline implementations
                Object bufferOpsImpl = instantiateBufferOps(opsVersion, context);
                Object drawOpsImpl = instantiateDrawOps(opsVersion, context);
                Object computeOpsImpl = instantiateComputeOps(opsVersion, context);
                Object syncOpsImpl = instantiateSyncOps(opsVersion, context);
                
                // Create dispatchers
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                BufferDispatch bufferDispatch = new BufferDispatch(bufferOpsImpl, lookup);
                DrawDispatch drawDispatch = new DrawDispatch(drawOpsImpl, lookup);
                ComputeDispatch computeDispatch = new ComputeDispatch(computeOpsImpl, lookup);
                SyncDispatch syncDispatch = new SyncDispatch(syncOpsImpl, lookup);
                
                // Detect features
                boolean hasTimeline = detectTimelineSemaphores(context, s, effectiveVersion);
                boolean hasDynRender = detectDynamicRendering(context, s, effectiveVersion);
                boolean hasSync2 = detectSynchronization2(context, s, effectiveVersion);
                boolean hasBDA = detectBufferDeviceAddress(context, s, effectiveVersion);
                boolean hasDescIdx = detectDescriptorIndexing(context, s, effectiveVersion);
                boolean hasMesh = detectMeshShaders(context, s, effectiveVersion);
                boolean hasMDI = detectMultiDrawIndirect(context, s);
                boolean hasIndCount = detectIndirectCount(context, s, effectiveVersion);
                
                // Create state cache
                StateCache stateCache = new StateCache();
                stateCache.invalidateAll();
                
                // Create timeline semaphore if supported
                long timelineSem = 0;
                if (hasTimeline && s.enableTimelineSemaphores) {
                    timelineSem = syncDispatch.createTimelineSemaphore(0);
                }
                
                // Get render thread
                Thread renderThread = Thread.currentThread();
                
                // Create manager instance
                VulkanManager mgr = new VulkanManager(
                    s, detectedVersion, effectiveVersion, opsVersion,
                    bufferDispatch, drawDispatch, computeDispatch, syncDispatch,
                    stateCache, context, vulkanState,
                    hasTimeline, hasDynRender, hasSync2, hasBDA, hasDescIdx, hasMesh, hasMDI, hasIndCount,
                    renderThread, rawVersionString, timelineSem
                );
                
                // Initialize GPU-driven systems if enabled
                if (s.enableGPUDrivenRendering) {
                    mgr.initializeGPUDrivenSystems();
                }
                
                // Publish
                PUBLISHED = mgr;
                FAST = mgr;
                
                // Log initialization
                if (s.debug) {
                    Astralis.LOGGER.info("[VulkanManager] Initialized successfully");
                    Astralis.LOGGER.info("[VulkanManager] {}", mgr.getConfigSummary());
                }
                
                return true;
                
            } catch (Throwable t) {
                Astralis.LOGGER.error("[VulkanManager] Initialization failed: {}", t.getMessage());
                t.printStackTrace();
                
                if (s != null && s.throwOnInitFailure) {
                    throw new RuntimeException("VulkanManager initialization failed", t);
                }
                return false;
            }
        }
    }
    
    /**
     * Initialize with error callback.
     */
    public static boolean init(Consumer<Throwable> errorCallback) {
        if (PUBLISHED != null) return true;
        
        synchronized (VulkanManager.class) {
            if (PUBLISHED != null) return true;
            
            try {
                return init();
            } catch (Throwable t) {
                if (errorCallback != null) {
                    errorCallback.accept(t);
                }
                return false;
            }
        }
    }

    // =========================================================================
    // VERSION DETECTION AND PIPELINE SELECTION
    // =========================================================================
    
    /**
     * Format Vulkan version to readable string.
     */
    private static String formatVulkanVersion(int version) {
        int major = VK_VERSION_MAJOR(version);
        int minor = VK_VERSION_MINOR(version);
        int patch = VK_VERSION_PATCH(version);
        return major + "." + minor + "." + patch;
    }
    
    /**
     * Clamp detected version to configured maximum.
     */
    private static int clampVersion(int detected, int maxVersion) {
        // maxVersion is in format 10, 11, 12, 13, 14
        int maxPacked;
        switch (maxVersion) {
            case 10: maxPacked = VULKAN_1_0; break;
            case 11: maxPacked = VULKAN_1_1; break;
            case 12: maxPacked = VULKAN_1_2; break;
            case 13: maxPacked = VULKAN_1_3; break;
            case 14: maxPacked = VULKAN_1_4; break;
            default: maxPacked = VULKAN_1_4; break;
        }
        return Math.min(detected, maxPacked);
    }
    
    /**
     * Choose buffer ops version based on effective Vulkan version.
     */
    private static int chooseBufferOpsVersion(int vulkanVersion) {
        if (vulkanVersion >= VULKAN_1_4) return 14;
        if (vulkanVersion >= VULKAN_1_3) return 13;
        if (vulkanVersion >= VULKAN_1_2) return 12;
        if (vulkanVersion >= VULKAN_1_1) return 11;
        return 10;
    }
    
    /**
     * Instantiate buffer operations implementation.
     */
    private static Object instantiateBufferOps(int version, VulkanContext ctx) {
        switch (version) {
            case 14: return new VulkanBufferOps14(ctx);
            case 13: return new VulkanBufferOps13(ctx);
            case 12: return new VulkanBufferOps12(ctx);
            case 11: return new VulkanBufferOps11(ctx);
            default: return new VulkanBufferOps10(ctx);
        }
    }
    
    /**
     * Instantiate draw operations implementation.
     */
    private static Object instantiateDrawOps(int version, VulkanContext ctx) {
        switch (version) {
            case 14: return new VulkanDrawOps14(ctx);
            case 13: return new VulkanDrawOps13(ctx);
            case 12: return new VulkanDrawOps12(ctx);
            case 11: return new VulkanDrawOps11(ctx);
            default: return new VulkanDrawOps10(ctx);
        }
    }
    
    /**
     * Instantiate compute operations implementation.
     */
    private static Object instantiateComputeOps(int version, VulkanContext ctx) {
        switch (version) {
            case 14: return new VulkanComputeOps14(ctx);
            case 13: return new VulkanComputeOps13(ctx);
            case 12: return new VulkanComputeOps12(ctx);
            case 11: return new VulkanComputeOps11(ctx);
            default: return new VulkanComputeOps10(ctx);
        }
    }
    
    /**
     * Instantiate synchronization operations implementation.
     */
    private static Object instantiateSyncOps(int version, VulkanContext ctx) {
        switch (version) {
            case 14: return new VulkanSyncOps14(ctx);
            case 13: return new VulkanSyncOps13(ctx);
            case 12: return new VulkanSyncOps12(ctx);
            case 11: return new VulkanSyncOps11(ctx);
            default: return new VulkanSyncOps10(ctx);
        }
    }

    // =========================================================================
    // FEATURE DETECTION
    // =========================================================================
    
    private static boolean detectTimelineSemaphores(VulkanContext ctx, Settings s, int version) {
        if (!s.enableTimelineSemaphores) return false;
        if (version >= VULKAN_1_2) return ctx.supportsTimelineSemaphores();
        return ctx.hasExtension("VK_KHR_timeline_semaphore");
    }
    
    private static boolean detectDynamicRendering(VulkanContext ctx, Settings s, int version) {
        if (!s.enableDynamicRendering) return false;
        if (version >= VULKAN_1_3) return ctx.supportsDynamicRendering();
        return ctx.hasExtension("VK_KHR_dynamic_rendering");
    }
    
    private static boolean detectSynchronization2(VulkanContext ctx, Settings s, int version) {
        if (!s.enableSynchronization2) return false;
        if (version >= VULKAN_1_3) return ctx.supportsSynchronization2();
        return ctx.hasExtension("VK_KHR_synchronization2");
    }
    
    private static boolean detectBufferDeviceAddress(VulkanContext ctx, Settings s, int version) {
        if (!s.enableBufferDeviceAddress) return false;
        if (version >= VULKAN_1_2) return ctx.supportsBufferDeviceAddress();
        return ctx.hasExtension("VK_KHR_buffer_device_address");
    }
    
    private static boolean detectDescriptorIndexing(VulkanContext ctx, Settings s, int version) {
        if (!s.enableDescriptorIndexing) return false;
        if (version >= VULKAN_1_2) return ctx.supportsDescriptorIndexing();
        return ctx.hasExtension("VK_EXT_descriptor_indexing");
    }
    
    private static boolean detectMeshShaders(VulkanContext ctx, Settings s, int version) {
        if (!s.enableMeshShaders) return false;
        // Mesh shaders require extension even in VK 1.4
        return ctx.hasExtension("VK_EXT_mesh_shader");
    }
    
    private static boolean detectMultiDrawIndirect(VulkanContext ctx, Settings s) {
        if (!s.enableMultiDrawIndirect) return false;
        return ctx.supportsMultiDrawIndirect();
    }
    
    private static boolean detectIndirectCount(VulkanContext ctx, Settings s, int version) {
        if (!s.enableIndirectCount) return false;
        if (version >= VULKAN_1_2) return true; // Core in 1.2
        return ctx.hasExtension("VK_KHR_draw_indirect_count");
    }

    // =========================================================================
    // GPU-DRIVEN RENDERING INFRASTRUCTURE
    // =========================================================================
    
    /**
     * GPU-driven rendering system state.
     * <p>Supports:</p>
     * <ul>
     *   <li>Indirect draw buffer management</li>
     *   <li>GPU culling compute shaders</li>
     *   <li>Meshlet data structures</li>
     *   <li>Instance data batching</li>
     * </ul>
     */
    private static final class GPUDrivenState {
        // Indirect command buffers (ring buffer for frames in flight)
        long[] indirectBuffers;
        long[] indirectBufferMemory;
        ByteBuffer[] indirectBufferMapped;
        int indirectBufferSize;
        
        // Draw count buffers (for MDI with count)
        long[] countBuffers;
        long[] countBufferMemory;
        ByteBuffer[] countBufferMapped;
        
        // Instance data buffers (SoA layout)
        long[] instanceBuffers;
        long[] instanceBufferMemory;
        ByteBuffer[] instanceBufferMapped;
        int maxInstances;
        
        // GPU culling resources
        long cullingPipeline;
        long cullingPipelineLayout;
        long cullingDescriptorSetLayout;
        long[] cullingDescriptorSets;
        
        // Meshlet resources (VK 1.4+ / mesh shader ext)
        long meshletBuffer;
        long meshletBufferMemory;
        long meshletDataBuffer;
        long meshletDataBufferMemory;
        int meshletCount;
        
        // Statistics
        final AtomicLong totalDrawCalls = new AtomicLong(0);
        final AtomicLong culledInstances = new AtomicLong(0);
        final AtomicLong visibleInstances = new AtomicLong(0);
        
        boolean initialized = false;
    }
    
    private GPUDrivenState gpuDrivenState;
    
    /**
     * Initialize GPU-driven rendering systems.
     */
    private void initializeGPUDrivenSystems() {
        gpuDrivenState = new GPUDrivenState();
        
        try {
            // Calculate buffer sizes
            int framesInFlight = maxFramesInFlight;
            int maxDrawsPerFrame = 65536; // 64K draws per frame
            int maxInstancesPerFrame = 1048576; // 1M instances per frame
            
            gpuDrivenState.indirectBufferSize = maxDrawsPerFrame * 20; // VkDrawIndexedIndirectCommand = 20 bytes
            gpuDrivenState.maxInstances = maxInstancesPerFrame;
            
            // Allocate indirect buffers
            gpuDrivenState.indirectBuffers = new long[framesInFlight];
            gpuDrivenState.indirectBufferMemory = new long[framesInFlight];
            gpuDrivenState.indirectBufferMapped = new ByteBuffer[framesInFlight];
            
            gpuDrivenState.countBuffers = new long[framesInFlight];
            gpuDrivenState.countBufferMemory = new long[framesInFlight];
            gpuDrivenState.countBufferMapped = new ByteBuffer[framesInFlight];
            
            gpuDrivenState.instanceBuffers = new long[framesInFlight];
            gpuDrivenState.instanceBufferMemory = new long[framesInFlight];
            gpuDrivenState.instanceBufferMapped = new ByteBuffer[framesInFlight];
            
            for (int i = 0; i < framesInFlight; i++) {
                // Create indirect buffer
                long[] result = createGPUBuffer(
                    gpuDrivenState.indirectBufferSize,
                    VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                );
                gpuDrivenState.indirectBuffers[i] = result[0];
                gpuDrivenState.indirectBufferMemory[i] = result[1];
                gpuDrivenState.indirectBufferMapped[i] = context.mapMemory(result[1], 0, gpuDrivenState.indirectBufferSize);
                
                // Create count buffer
                result = createGPUBuffer(
                    4, // Single uint32
                    VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                );
                gpuDrivenState.countBuffers[i] = result[0];
                gpuDrivenState.countBufferMemory[i] = result[1];
                gpuDrivenState.countBufferMapped[i] = context.mapMemory(result[1], 0, 4);
                
                // Create instance buffer (transform matrix + custom data per instance)
                int instanceDataSize = maxInstancesPerFrame * 80; // 64 bytes transform + 16 bytes custom
                result = createGPUBuffer(
                    instanceDataSize,
                    VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                );
                gpuDrivenState.instanceBuffers[i] = result[0];
                gpuDrivenState.instanceBufferMemory[i] = result[1];
                gpuDrivenState.instanceBufferMapped[i] = context.mapMemory(result[1], 0, instanceDataSize);
            }
            
            // Create GPU culling resources if enabled
            if (settings.enableGPUCulling) {
                initializeGPUCulling();
            }
            
            // Create meshlet resources if mesh shaders available
            if (hasMeshShaders && settings.enableMeshShaders) {
                initializeMeshletSystem();
            }
            
            gpuDrivenState.initialized = true;
            
            if (debug) {
                Astralis.LOGGER.info("[VulkanManager] GPU-driven systems initialized");
                Astralis.LOGGER.info("[VulkanManager] Max draws/frame: {}, Max instances/frame: {}", 
                    maxDrawsPerFrame, maxInstancesPerFrame);
            }
            
        } catch (Exception e) {
            Astralis.LOGGER.warn("[VulkanManager] Failed to initialize GPU-driven systems: {}", e.getMessage());
            gpuDrivenState.initialized = false;
        }
    }
    
    /**
     * Create a GPU buffer with specified properties.
     * @return array of [buffer handle, memory handle]
     */
    private long[] createGPUBuffer(long size, int usage, int memoryProperties) {
        LongBuffer pBuffer = memAllocLong(1);
        LongBuffer pMemory = memAllocLong(1);
        
        try {
            // Create buffer
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(size)
                .usage(usage)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            
            int result = vkCreateBuffer(context.device, bufferInfo, null, pBuffer);
            bufferInfo.free();
            
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create buffer: " + result);
            }
            
            long buffer = pBuffer.get(0);
            
            // Get memory requirements
            VkMemoryRequirements memReqs = VkMemoryRequirements.calloc();
            vkGetBufferMemoryRequirements(context.device, buffer, memReqs);
            
            // Allocate memory
            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memReqs.size())
                .memoryTypeIndex(context.findMemoryType(memReqs.memoryTypeBits(), memoryProperties));
            
            result = vkAllocateMemory(context.device, allocInfo, null, pMemory);
            memReqs.free();
            allocInfo.free();
            
            if (result != VK_SUCCESS) {
                vkDestroyBuffer(context.device, buffer, null);
                throw new RuntimeException("Failed to allocate memory: " + result);
            }
            
            long memory = pMemory.get(0);
            
            // Bind memory
            vkBindBufferMemory(context.device, buffer, memory, 0);
            
            return new long[] { buffer, memory };
            
        } finally {
            memFree(pBuffer);
            memFree(pMemory);
        }
    }
    
    /**
     * Initialize GPU culling compute pipeline.
     */
    private void initializeGPUCulling() {
        // Create descriptor set layout for culling
        VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(4);
        
        // Binding 0: Input instance buffer
        bindings.get(0)
            .binding(0)
            .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
            .descriptorCount(1)
            .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
        
        // Binding 1: Output indirect command buffer
        bindings.get(1)
            .binding(1)
            .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
            .descriptorCount(1)
            .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
        
        // Binding 2: Draw count buffer
        bindings.get(2)
            .binding(2)
            .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
            .descriptorCount(1)
            .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
        
        // Binding 3: Culling uniforms (frustum planes, etc.)
        bindings.get(3)
            .binding(3)
            .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
            .descriptorCount(1)
            .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
        
        VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
            .pBindings(bindings);
        
        LongBuffer pLayout = memAllocLong(1);
        vkCreateDescriptorSetLayout(context.device, layoutInfo, null, pLayout);
        gpuDrivenState.cullingDescriptorSetLayout = pLayout.get(0);
        
        bindings.free();
        layoutInfo.free();
        memFree(pLayout);
        
        // Create pipeline layout
        LongBuffer pSetLayouts = memAllocLong(1);
        pSetLayouts.put(0, gpuDrivenState.cullingDescriptorSetLayout);
        
        VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
            .pSetLayouts(pSetLayouts);
        
        LongBuffer pPipelineLayout = memAllocLong(1);
        vkCreatePipelineLayout(context.device, pipelineLayoutInfo, null, pPipelineLayout);
        gpuDrivenState.cullingPipelineLayout = pPipelineLayout.get(0);
        
        pipelineLayoutInfo.free();
        memFree(pSetLayouts);
        memFree(pPipelineLayout);
        
        // Create compute pipeline (shader would be loaded from resources)
        // For now, mark as initialized - actual shader creation would go here
        gpuDrivenState.cullingPipeline = VK_NULL_HANDLE; // TODO: Create actual pipeline
        
        // Allocate descriptor sets
        gpuDrivenState.cullingDescriptorSets = new long[maxFramesInFlight];
        
        if (debug) {
            Astralis.LOGGER.info("[VulkanManager] GPU culling system initialized");
        }
    }
    
    /**
     * Initialize meshlet rendering system.
     */
    private void initializeMeshletSystem() {
        // Meshlet buffer layout:
        // - Meshlet descriptors (32 bytes each): vertex offset, vertex count, primitive offset, primitive count, bounds
        // - Meshlet vertex data
        // - Meshlet primitive data (triangle indices packed)
        
        int maxMeshlets = 1048576; // 1M meshlets
        int meshletDescSize = maxMeshlets * 32;
        
        long[] result = createGPUBuffer(
            meshletDescSize,
            VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
        );
        gpuDrivenState.meshletBuffer = result[0];
        gpuDrivenState.meshletBufferMemory = result[1];
        
        // Meshlet data buffer (vertices and primitives)
        int meshletDataSize = 256 * 1024 * 1024; // 256MB for vertex/primitive data
        result = createGPUBuffer(
            meshletDataSize,
            VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
            VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
        );
        gpuDrivenState.meshletDataBuffer = result[0];
        gpuDrivenState.meshletDataBufferMemory = result[1];
        
        if (debug) {
            Astralis.LOGGER.info("[VulkanManager] Meshlet system initialized (max {} meshlets)", maxMeshlets);
        }
    }

    // =========================================================================
    // FRAME MANAGEMENT
    // =========================================================================
    
    /**
     * Begin a new frame.
     * <p>Must be called at the start of each frame before any rendering commands.</p>
     */
    public void beginFrame() {
        assertRenderThread();
        
        int frameIndex = currentFrame.get();
        
        // Wait for previous frame using this slot
        if (hasTimelineSemaphores && timelineSemaphore != 0) {
            // Use timeline semaphore
            long waitValue = timelineValue.get() - maxFramesInFlight + 1;
            if (waitValue > 0) {
                syncDispatch.waitSemaphore(timelineSemaphore, waitValue, Long.MAX_VALUE);
            }
        } else {
            // Use fence
            long fence = context.getFrameFence(frameIndex);
            syncDispatch.waitForFence(fence, Long.MAX_VALUE);
            syncDispatch.resetFence(fence);
        }
        
        // Update frame state
        stateCache.currentFrameIndex = frameIndex;
        stateCache.markAllDirty();
        
        // Begin command buffer
        VulkanCallMapper.beginFrame();
        stateCache.commandBufferRecording = true;
        
        // Flush any deferred commands
        flushDeferredCommands();
        
        if (debug) {
            Astralis.LOGGER.debug("[VulkanManager] Begin frame {}", frameCounter.get());
        }
    }
    
    /**
     * End the current frame.
     * <p>Submits all recorded commands and presents.</p>
     */
    public void endFrame() {
        assertRenderThread();
        
        if (!stateCache.commandBufferRecording) {
            if (debug) {
                Astralis.LOGGER.warn("[VulkanManager] endFrame called without beginFrame");
            }
            return;
        }
        
        // Flush any remaining commands
        flushDeferredCommands();
        
        // End command buffer and submit
        VulkanCallMapper.endFrame();
        stateCache.commandBufferRecording = false;
        
        // Signal timeline semaphore
        if (hasTimelineSemaphores && timelineSemaphore != 0) {
            long signalValue = timelineValue.incrementAndGet();
            syncDispatch.signalSemaphore(timelineSemaphore, signalValue);
        }
        
        // Advance frame index
        int nextFrame = (currentFrame.get() + 1) % maxFramesInFlight;
        currentFrame.set(nextFrame);
        frameCounter.incrementAndGet();
        
        if (debug) {
            Astralis.LOGGER.debug("[VulkanManager] End frame, next index: {}", nextFrame);
        }
    }
    
    /**
     * Flush any batched/deferred commands.
     */
    public void flushCommands() {
        assertRenderThread();
        VulkanCallMapper.flushCommands();
        flushDeferredCommands();
    }
    
    /**
     * Flush deferred command queue.
     */
    private void flushDeferredCommands() {
        Runnable cmd;
        int count = 0;
        while ((cmd = deferredCommands.poll()) != null && count < commandBatchSize) {
            cmd.run();
            count++;
        }
    }
    
    /**
     * Wait for GPU to complete all pending work.
     */
    public void finish() {
        assertRenderThread();
        VulkanCallMapper.finish();
    }
    
    /**
     * Get current frame index (0 to maxFramesInFlight-1).
     */
    public int getCurrentFrameIndex() {
        return currentFrame.get();
    }
    
    /**
     * Get total frames rendered.
     */
    public long getFrameCount() {
        return frameCounter.get();
    }
    
    /**
     * Get max frames in flight.
     */
    public int getMaxFramesInFlight() {
        return maxFramesInFlight;
    }

    // =========================================================================
    // RENDER GRAPH SUPPORT
    // =========================================================================
    
    /**
     * Render pass description for render graph.
     */
    public static final class RenderPassDesc {
        public final String name;
        public final int colorAttachmentCount;
        public final boolean hasDepth;
        public final boolean hasStencil;
        public final int loadOp;  // VK_ATTACHMENT_LOAD_OP_*
        public final int storeOp; // VK_ATTACHMENT_STORE_OP_*
        public final int[] clearColor;
        public final float clearDepth;
        public final int clearStencil;
        
        public RenderPassDesc(String name, int colorCount, boolean depth, boolean stencil,
                              int loadOp, int storeOp, int[] clearColor, float clearDepth, int clearStencil) {
            this.name = name;
            this.colorAttachmentCount = colorCount;
            this.hasDepth = depth;
            this.hasStencil = stencil;
            this.loadOp = loadOp;
            this.storeOp = storeOp;
            this.clearColor = clearColor;
            this.clearDepth = clearDepth;
            this.clearStencil = clearStencil;
        }
        
        public static RenderPassDesc color(String name) {
            return new RenderPassDesc(name, 1, false, false, 
                VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE,
                new int[]{0, 0, 0, 255}, 1.0f, 0);
        }
        
        public static RenderPassDesc colorDepth(String name) {
            return new RenderPassDesc(name, 1, true, false,
                VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE,
                new int[]{0, 0, 0, 255}, 1.0f, 0);
        }
        
        public static RenderPassDesc depthOnly(String name) {
            return new RenderPassDesc(name, 0, true, false,
                VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE,
                null, 1.0f, 0);
        }
    }
    
    /**
     * Begin a render pass using dynamic rendering (VK 1.3+) or traditional render pass.
     */
    public void beginRenderPass(RenderPassDesc desc, long framebuffer, 
                                 int x, int y, int width, int height) {
        assertRenderThread();
        ensureRecording();
        
        if (hasDynamicRendering) {
            beginDynamicRenderPass(desc, x, y, width, height);
        } else {
            beginLegacyRenderPass(desc, framebuffer, x, y, width, height);
        }
        
        if (debug) {
            Astralis.LOGGER.debug("[VulkanManager] Begin render pass: {}", desc.name);
        }
    }
    
    private void beginDynamicRenderPass(RenderPassDesc desc, int x, int y, int width, int height) {
        // Dynamic rendering implementation
        // Uses VK_KHR_dynamic_rendering or VK 1.3+ core
        VulkanCallMapper.beginDynamicRendering(
            x, y, width, height,
            desc.colorAttachmentCount,
            desc.hasDepth,
            desc.hasStencil,
            desc.loadOp,
            desc.storeOp,
            desc.clearColor,
            desc.clearDepth,
            desc.clearStencil
        );
    }
    
    private void beginLegacyRenderPass(RenderPassDesc desc, long framebuffer,
                                        int x, int y, int width, int height) {
        // Traditional render pass
        VulkanCallMapper.beginRenderPass(
            context.renderPass,
            framebuffer,
            x, y, width, height,
            desc.clearColor,
            desc.clearDepth,
            desc.clearStencil
        );
    }
    
    /**
     * End current render pass.
     */
    public void endRenderPass() {
        assertRenderThread();
        
        if (hasDynamicRendering) {
            VulkanCallMapper.endDynamicRendering();
        } else {
            VulkanCallMapper.endRenderPass();
        }
    }
    
    /**
     * Insert a pipeline barrier.
     */
    public void pipelineBarrier(int srcStage, int dstStage, int srcAccess, int dstAccess) {
        assertRenderThread();
        ensureRecording();
        
        if (hasSynchronization2) {
            // Use sync2 API
            VulkanCallMapper.pipelineBarrier2(
                translateStageToStage2(srcStage),
                srcAccess,
                translateStageToStage2(dstStage),
                dstAccess
            );
        } else {
            VulkanCallMapper.pipelineBarrier(srcStage, dstStage, srcAccess, dstAccess);
        }
    }
    
    /**
     * Translate VK 1.0 stage flags to VK 1.3 stage2 flags.
     */
    private long translateStageToStage2(int stage) {
        // VK_PIPELINE_STAGE_2_* has same values in low bits
        return stage & 0xFFFFFFFFL;
    }

    // =========================================================================
    // BINDLESS RESOURCE MANAGEMENT
    // =========================================================================
    
    /**
     * Bindless resource handle.
     */
    public static final class BindlessHandle {
        public final int index;      // Index in bindless array
        public final int type;       // Resource type (texture, buffer, etc.)
        public final long resource;  // Underlying Vulkan resource
        
        BindlessHandle(int index, int type, long resource) {
            this.index = index;
            this.type = type;
            this.resource = resource;
        }
        
        public static final int TYPE_TEXTURE = 0;
        public static final int TYPE_SAMPLER = 1;
        public static final int TYPE_STORAGE_IMAGE = 2;
        public static final int TYPE_STORAGE_BUFFER = 3;
        public static final int TYPE_UNIFORM_BUFFER = 4;
    }
    
    // Bindless resource tracking
    private static final int MAX_BINDLESS_TEXTURES = 65536;
    private static final int MAX_BINDLESS_BUFFERS = 16384;
    
    private final long[] bindlessTextureViews = new long[MAX_BINDLESS_TEXTURES];
    private final long[] bindlessSamplers = new long[MAX_BINDLESS_TEXTURES];
    private final long[] bindlessBuffers = new long[MAX_BINDLESS_BUFFERS];
    private final AtomicInteger nextBindlessTexture = new AtomicInteger(1); // 0 reserved for null
    private final AtomicInteger nextBindlessBuffer = new AtomicInteger(1);
    
    /**
     * Register a texture for bindless access.
     * @return bindless handle, or null if descriptor indexing not supported
     */
    public BindlessHandle registerBindlessTexture(long imageView, long sampler) {
        if (!hasDescriptorIndexing) return null;
        
        int index = nextBindlessTexture.getAndIncrement();
        if (index >= MAX_BINDLESS_TEXTURES) {
            nextBindlessTexture.decrementAndGet();
            throw new RuntimeException("Bindless texture limit exceeded");
        }
        
        bindlessTextureViews[index] = imageView;
        bindlessSamplers[index] = sampler;
        
        // Update descriptor set
        updateBindlessTextureDescriptor(index, imageView, sampler);
        
        return new BindlessHandle(index, BindlessHandle.TYPE_TEXTURE, imageView);
    }
    
    /**
     * Register a buffer for bindless access (via BDA).
     * @return buffer device address, or 0 if BDA not supported
     */
    public long registerBindlessBuffer(long buffer) {
        if (!hasBufferDeviceAddress) return 0;
        
        long address = bufferDispatch.getBufferDeviceAddress(buffer);
        
        int index = nextBindlessBuffer.getAndIncrement();
        if (index < MAX_BINDLESS_BUFFERS) {
            bindlessBuffers[index] = address;
        }
        
        return address;
    }
    
    /**
     * Unregister a bindless texture.
     */
    public void unregisterBindlessTexture(BindlessHandle handle) {
        if (handle == null || handle.type != BindlessHandle.TYPE_TEXTURE) return;
        
        bindlessTextureViews[handle.index] = 0;
        bindlessSamplers[handle.index] = 0;
        
        // Update descriptor to null (or default texture)
        updateBindlessTextureDescriptor(handle.index, 0, 0);
    }
    
    private void updateBindlessTextureDescriptor(int index, long imageView, long sampler) {
        // Update the bindless descriptor array
        // This would update a large descriptor set with VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT
        VulkanCallMapper.updateBindlessDescriptor(index, imageView, sampler);
    }

    // =========================================================================
    // THREAD SAFETY UTILITIES
    // =========================================================================
    
    /**
     * Assert we're on the render thread (debug only).
     */
    private void assertRenderThread() {
        if (!debug) return;
        if (Thread.currentThread() != renderThread) {
            throw new IllegalStateException(
                "VulkanManager called from wrong thread. Expected: " + 
                renderThread.getName() + ", Got: " + Thread.currentThread().getName());
        }
    }
    
    /**
     * Ensure command buffer is recording.
     */
    private void ensureRecording() {
        if (!stateCache.commandBufferRecording) {
            beginFrame();
        }
    }
    
    /**
     * Check if currently on render thread.
     */
    public boolean isRenderThread() {
        return Thread.currentThread() == renderThread;
    }
    
    /**
     * Get render thread reference.
     */
    public Thread getRenderThread() {
        return renderThread;
    }

    // =========================================================================
    // STATE INVALIDATION
    // =========================================================================
    
    /**
     * Invalidate all cached state.
     * <p>Call this if external code modifies Vulkan state directly.</p>
     */
    public void invalidateState() {
        assertRenderThread();
        stateCache.invalidateAll();
    }
    
    /**
     * Invalidate only buffer bindings.
     */
    public void invalidateBufferBindings() {
        assertRenderThread();
        stateCache.invalidateBufferBindings();
    }
    
    /**
     * Invalidate only texture bindings.
     */
    public void invalidateTextureBindings() {
        assertRenderThread();
        stateCache.invalidateTextureBindings();
    }
    
    /**
     * Invalidate only pipeline bindings.
     */
    public void invalidatePipelines() {
        assertRenderThread();
        stateCache.invalidatePipelines();
    }
    
    /**
     * Invalidate descriptor set bindings.
     */
    public void invalidateDescriptorSets() {
        assertRenderThread();
        stateCache.invalidateDescriptorSets();
    }

    // =========================================================================
    // STATISTICS AND PROFILING
    // =========================================================================
    
    /**
     * Frame statistics.
     */
    public static final class FrameStats {
        public long drawCalls;
        public long triangles;
        public long vertices;
        public long instances;
        public long culledInstances;
        public long pipelineBinds;
        public long descriptorBinds;
        public long bufferBinds;
        public long barriers;
        public double gpuTimeMs;
        public double cpuTimeMs;
        
        public void reset() {
            drawCalls = triangles = vertices = instances = 0;
            culledInstances = pipelineBinds = descriptorBinds = bufferBinds = barriers = 0;
            gpuTimeMs = cpuTimeMs = 0;
        }
    }
    
    private final FrameStats frameStats = new FrameStats();
    private final FrameStats[] frameStatsHistory = new FrameStats[maxFramesInFlight];
    
    /**
     * Get current frame statistics.
     */
    public FrameStats getFrameStats() {
        return frameStats;
    }
    
    /**
     * Get statistics for a previous frame.
     */
    public FrameStats getFrameStats(int framesAgo) {
        if (framesAgo < 0 || framesAgo >= maxFramesInFlight) return null;
        int index = (currentFrame.get() - framesAgo + maxFramesInFlight) % maxFramesInFlight;
        return frameStatsHistory[index];
    }
    
    /**
     * Reset frame statistics.
     */
    public void resetFrameStats() {
        // Save current to history
        int histIndex = currentFrame.get();
        if (frameStatsHistory[histIndex] == null) {
            frameStatsHistory[histIndex] = new FrameStats();
        }
        frameStatsHistory[histIndex].drawCalls = frameStats.drawCalls;
        frameStatsHistory[histIndex].triangles = frameStats.triangles;
        // ... copy other fields
        
        frameStats.reset();
    }

    // =========================================================================
    // BUFFER OPERATIONS - PUBLIC API
    // =========================================================================
    
    /**
     * Generate a new buffer.
     * @return buffer handle (Vulkan buffer wrapper ID)
     */
    public long genBuffer() {
        assertRenderThread();
        long buffer = bufferDispatch.genBuffer();
        
        if (debug) {
            Astralis.LOGGER.trace("[VulkanManager] genBuffer -> {}", buffer);
        }
        
        return buffer;
    }
    
    /**
     * Generate multiple buffers at once (batch operation).
     * <p>More efficient than calling genBuffer() in a loop.</p>
     * 
     * @param count number of buffers to generate
     * @return array of buffer handles
     */
    public long[] genBuffers(int count) {
        assertRenderThread();
        
        if (count <= 0) return new long[0];
        
        long[] buffers = bufferDispatch.genBuffers(count);
        
        if (debug) {
            Astralis.LOGGER.trace("[VulkanManager] genBuffers({}) -> {} buffers", count, buffers.length);
        }
        
        return buffers;
    }
    
    /**
     * Delete a buffer.
     * 
     * @param buffer buffer handle to delete
     */
    public void deleteBuffer(long buffer) {
        assertRenderThread();
        
        if (buffer == 0) return;
        
        bufferDispatch.deleteBuffer(buffer);
        
        // Invalidate from cache
        invalidateBufferFromCache(buffer);
        
        if (debug) {
            Astralis.LOGGER.trace("[VulkanManager] deleteBuffer({})", buffer);
        }
    }
    
    /**
     * Delete multiple buffers (batch operation).
     * 
     * @param buffers array of buffer handles to delete
     */
    public void deleteBuffers(long[] buffers) {
        assertRenderThread();
        
        if (buffers == null || buffers.length == 0) return;
        
        bufferDispatch.deleteBuffers(buffers);
        
        // Invalidate all from cache
        for (long buffer : buffers) {
            invalidateBufferFromCache(buffer);
        }
        
        if (debug) {
            Astralis.LOGGER.trace("[VulkanManager] deleteBuffers({} buffers)", buffers.length);
        }
    }
    
    /**
     * Invalidate a buffer from the state cache.
     */
    private void invalidateBufferFromCache(long buffer) {
        for (int i = 0; i < StateCache.MAX_BUFFER_BINDINGS; i++) {
            if (stateCache.bufferHandles[i] == buffer) {
                stateCache.bufferHandles[i] = 0;
                stateCache.bufferBindingMask &= ~(1 << i);
            }
        }
    }
    
    /**
     * Bind a buffer to a target.
     * <p>Uses state caching - redundant binds are elided.</p>
     * 
     * @param target buffer target (use GL constants for compatibility)
     * @param buffer buffer handle (0 to unbind)
     */
    public void bindBuffer(int target, long buffer) {
        assertRenderThread();
        
        int slot = targetToSlot(target);
        
        if (settings.cacheBindings && slot >= 0 && slot < StateCache.MAX_BUFFER_BINDINGS) {
            // Check cache
            if ((stateCache.bufferBindingMask & (1 << slot)) != 0 &&
                stateCache.bufferHandles[slot] == buffer) {
                // Already bound, skip
                return;
            }
            
            // Update cache
            if (buffer != 0) {
                stateCache.bufferHandles[slot] = buffer;
                stateCache.bufferBindingMask |= (1 << slot);
            } else {
                stateCache.bufferHandles[slot] = 0;
                stateCache.bufferBindingMask &= ~(1 << slot);
            }
        }
        
        // Perform actual bind
        bufferDispatch.bindBuffer(target, buffer);
        
        // Update VulkanState as well (for pipeline compatibility)
        vulkanState.bindBuffer(target, buffer);
        
        if (debug) {
            frameStats.bufferBinds++;
        }
    }
    
    /**
     * Force bind buffer, bypassing cache.
     * <p>Use when external code may have modified bindings.</p>
     */
    public void bindBufferForce(int target, long buffer) {
        assertRenderThread();
        
        int slot = targetToSlot(target);
        if (slot >= 0 && slot < StateCache.MAX_BUFFER_BINDINGS) {
            if (buffer != 0) {
                stateCache.bufferHandles[slot] = buffer;
                stateCache.bufferBindingMask |= (1 << slot);
            } else {
                stateCache.bufferHandles[slot] = 0;
                stateCache.bufferBindingMask &= ~(1 << slot);
            }
        }
        
        bufferDispatch.bindBuffer(target, buffer);
        vulkanState.bindBuffer(target, buffer);
    }
    
    /**
     * Map GL buffer target to cache slot index.
     */
    private static int targetToSlot(int target) {
        switch (target) {
            case 0x8892: return 0;  // GL_ARRAY_BUFFER
            case 0x8893: return 1;  // GL_ELEMENT_ARRAY_BUFFER
            case 0x8A11: return 2;  // GL_UNIFORM_BUFFER
            case 0x90D2: return 3;  // GL_SHADER_STORAGE_BUFFER
            case 0x8F3F: return 4;  // GL_DRAW_INDIRECT_BUFFER
            case 0x90EE: return 5;  // GL_DISPATCH_INDIRECT_BUFFER
            case 0x8F36: return 6;  // GL_COPY_READ_BUFFER
            case 0x8F37: return 7;  // GL_COPY_WRITE_BUFFER
            case 0x8C8E: return 8;  // GL_TRANSFORM_FEEDBACK_BUFFER
            case 0x8C2A: return 9;  // GL_TEXTURE_BUFFER
            case 0x88EB: return 10; // GL_PIXEL_PACK_BUFFER
            case 0x88EC: return 11; // GL_PIXEL_UNPACK_BUFFER
            case 0x9192: return 12; // GL_QUERY_BUFFER
            case 0x92C0: return 13; // GL_ATOMIC_COUNTER_BUFFER
            default: return -1;
        }
    }
    
    /**
     * Allocate buffer storage with size only (no data).
     * 
     * @param target buffer target
     * @param size size in bytes
     * @param usage usage hint (GL usage constant)
     */
    public void bufferData(int target, long size, int usage) {
        assertRenderThread();
        bufferDispatch.bufferData(target, size, usage);
    }
    
    /**
     * Allocate and initialize buffer with data.
     * 
     * @param target buffer target
     * @param data data to upload
     * @param usage usage hint
     */
    public void bufferData(int target, ByteBuffer data, int usage) {
        assertRenderThread();
        bufferDispatch.bufferData(target, data, usage);
    }
    
    /**
     * Allocate and initialize buffer with float data.
     */
    public void bufferData(int target, FloatBuffer data, int usage) {
        assertRenderThread();
        // Convert to ByteBuffer view
        ByteBuffer bb = memByteBuffer(memAddress(data), data.remaining() * 4);
        bufferDispatch.bufferData(target, bb, usage);
    }
    
    /**
     * Allocate and initialize buffer with int data.
     */
    public void bufferData(int target, IntBuffer data, int usage) {
        assertRenderThread();
        ByteBuffer bb = memByteBuffer(memAddress(data), data.remaining() * 4);
        bufferDispatch.bufferData(target, bb, usage);
    }
    
    /**
     * Update a portion of buffer data.
     * 
     * @param target buffer target
     * @param offset byte offset into buffer
     * @param data data to upload
     */
    public void bufferSubData(int target, long offset, ByteBuffer data) {
        assertRenderThread();
        bufferDispatch.bufferSubData(target, offset, data);
    }
    
    /**
     * Update buffer with float data.
     */
    public void bufferSubData(int target, long offset, FloatBuffer data) {
        assertRenderThread();
        ByteBuffer bb = memByteBuffer(memAddress(data), data.remaining() * 4);
        bufferDispatch.bufferSubData(target, offset, bb);
    }
    
    /**
     * Update buffer with int data.
     */
    public void bufferSubData(int target, long offset, IntBuffer data) {
        assertRenderThread();
        ByteBuffer bb = memByteBuffer(memAddress(data), data.remaining() * 4);
        bufferDispatch.bufferSubData(target, offset, bb);
    }
    
    /**
     * Map a buffer for CPU access.
     * 
     * @param target buffer target
     * @param access access mode (GL constant: GL_READ_ONLY, GL_WRITE_ONLY, GL_READ_WRITE)
     * @param length buffer size (required for Vulkan mapping)
     * @return mapped ByteBuffer, or null on failure
     */
    public ByteBuffer mapBuffer(int target, int access, long length) {
        assertRenderThread();
        return bufferDispatch.mapBuffer(target, access, length);
    }
    
    /**
     * Map a range of a buffer.
     * <p>Preferred over mapBuffer for partial updates.</p>
     * 
     * @param target buffer target
     * @param offset byte offset
     * @param length bytes to map
     * @param accessFlags GL_MAP_READ_BIT, GL_MAP_WRITE_BIT, etc.
     * @return mapped ByteBuffer, or null if unsupported/failed
     */
    public ByteBuffer mapBufferRange(int target, long offset, long length, int accessFlags) {
        assertRenderThread();
        
        ByteBuffer result = bufferDispatch.mapBufferRange(target, offset, length, accessFlags);
        
        if (result == null && settings.strictNoEmulation) {
            throw new UnsupportedOperationException(
                "mapBufferRange not supported by VulkanBufferOps" + bufferOpsVersion);
        }
        
        return result;
    }
    
    /**
     * Unmap a previously mapped buffer.
     * 
     * @param target buffer target
     * @return true if successful
     */
    public boolean unmapBuffer(int target) {
        assertRenderThread();
        return bufferDispatch.unmapBuffer(target);
    }
    
    /**
     * Flush a range of a mapped buffer.
     * <p>Required when mapping with GL_MAP_FLUSH_EXPLICIT_BIT.</p>
     */
    public void flushMappedBufferRange(int target, long offset, long length) {
        assertRenderThread();
        
        if (!bufferDispatch.hasFlushMappedRange && settings.strictNoEmulation) {
            throw new UnsupportedOperationException(
                "flushMappedBufferRange not supported by VulkanBufferOps" + bufferOpsVersion);
        }
        
        bufferDispatch.flushMappedBufferRange(target, offset, length);
    }
    
    /**
     * Copy data between buffers (GPU-side).
     * 
     * @param readTarget source buffer target
     * @param writeTarget destination buffer target
     * @param readOffset source offset
     * @param writeOffset destination offset
     * @param size bytes to copy
     */
    public void copyBufferSubData(int readTarget, int writeTarget, 
                                   long readOffset, long writeOffset, long size) {
        assertRenderThread();
        
        if (!bufferDispatch.hasCopyBufferSubData && settings.strictNoEmulation) {
            throw new UnsupportedOperationException(
                "copyBufferSubData not supported by VulkanBufferOps" + bufferOpsVersion);
        }
        
        bufferDispatch.copyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }
    
    /**
     * Invalidate buffer contents (optimization hint).
     * <p>Signals that previous buffer contents are no longer needed.</p>
     */
    public void invalidateBufferData(long buffer) {
        assertRenderThread();
        
        if (!bufferDispatch.hasInvalidateBuffer) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException(
                    "invalidateBufferData not supported by VulkanBufferOps" + bufferOpsVersion);
            }
            return;
        }
        
        bufferDispatch.invalidateBufferData(buffer);
    }
    
    /**
     * Invalidate a range of buffer contents.
     */
    public void invalidateBufferSubData(long buffer, long offset, long length) {
        assertRenderThread();
        
        if (!bufferDispatch.hasInvalidateBufferSubData) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException(
                    "invalidateBufferSubData not supported by VulkanBufferOps" + bufferOpsVersion);
            }
            return;
        }
        
        bufferDispatch.invalidateBufferSubData(buffer, offset, length);
    }
    
    /**
     * Get buffer device address for bindless vertex pulling.
     * <p>Requires Vulkan 1.2+ with bufferDeviceAddress feature.</p>
     * 
     * @param buffer buffer handle
     * @return device address, or 0 if not supported
     */
    public long getBufferDeviceAddress(long buffer) {
        assertRenderThread();
        
        if (!hasBufferDeviceAddress) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Buffer device address not supported");
            }
            return 0;
        }
        
        return bufferDispatch.getBufferDeviceAddress(buffer);
    }
    
    /**
     * Map buffer persistently (remains mapped until explicitly unmapped).
     * <p>Requires VK_EXT_external_memory_host or similar support.</p>
     */
    public ByteBuffer mapBufferPersistent(long buffer, long offset, long length) {
        assertRenderThread();
        
        if (!bufferDispatch.hasPersistentMapping) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Persistent mapping not supported");
            }
            return null;
        }
        
        return bufferDispatch.mapBufferPersistent(buffer, offset, length);
    }
    
    /**
     * Get buffer size.
     */
    public long getBufferSize(long buffer) {
        assertRenderThread();
        return bufferDispatch.getBufferSize(buffer);
    }

    // =========================================================================
    // TEXTURE OPERATIONS - PUBLIC API
    // =========================================================================
    
    /**
     * Generate a new texture.
     * @return texture handle
     */
    public long genTexture() {
        assertRenderThread();
        return VulkanCallMapper.genTexture();
    }
    
    /**
     * Generate multiple textures (batch operation).
     */
    public long[] genTextures(int count) {
        assertRenderThread();
        long[] textures = new long[count];
        VulkanCallMapper.genTextures(count, textures);
        return textures;
    }
    
    /**
     * Delete a texture.
     */
    public void deleteTexture(long texture) {
        assertRenderThread();
        if (texture == 0) return;
        
        VulkanCallMapper.deleteTexture(texture);
        invalidateTextureFromCache(texture);
    }
    
    /**
     * Delete multiple textures.
     */
    public void deleteTextures(long[] textures) {
        assertRenderThread();
        VulkanCallMapper.deleteTextures(textures.length, textures);
        
        for (long texture : textures) {
            invalidateTextureFromCache(texture);
        }
    }
    
    /**
     * Invalidate texture from cache.
     */
    private void invalidateTextureFromCache(long texture) {
        for (int i = 0; i < StateCache.MAX_TEXTURE_UNITS; i++) {
            if (stateCache.textureImageViews[i] == texture) {
                stateCache.textureImageViews[i] = 0;
                stateCache.textureSamplers[i] = 0;
                stateCache.textureBindingMask &= ~(1 << i);
            }
        }
    }
    
    /**
     * Set active texture unit.
     * 
     * @param unit texture unit (GL_TEXTURE0 + n)
     */
    public void activeTexture(int unit) {
        assertRenderThread();
        
        int unitIndex = unit - 0x84C0; // GL_TEXTURE0
        
        if (settings.cacheBindings && stateCache.activeTextureUnit == unitIndex) {
            return;
        }
        
        stateCache.activeTextureUnit = unitIndex;
        VulkanCallMapper.activeTexture(unit);
    }
    
    /**
     * Bind a texture to the current texture unit.
     * 
     * @param target texture target (GL_TEXTURE_2D, etc.)
     * @param texture texture handle
     */
    public void bindTexture(int target, long texture) {
        assertRenderThread();
        
        int unit = stateCache.activeTextureUnit;
        
        if (settings.cacheBindings && unit >= 0 && unit < StateCache.MAX_TEXTURE_UNITS) {
            // For simplicity, we cache by unit (not target) for 2D textures
            if (target == 0x0DE1) { // GL_TEXTURE_2D
                if ((stateCache.textureBindingMask & (1 << unit)) != 0 &&
                    stateCache.textureImageViews[unit] == texture) {
                    return;
                }
                
                if (texture != 0) {
                    stateCache.textureImageViews[unit] = texture;
                    stateCache.textureBindingMask |= (1 << unit);
                } else {
                    stateCache.textureImageViews[unit] = 0;
                    stateCache.textureBindingMask &= ~(1 << unit);
                }
            }
        }
        
        VulkanCallMapper.bindTexture(target, texture);
        vulkanState.bindTexture(unit, texture);
    }
    
    /**
     * Set texture parameter (integer).
     */
    public void texParameteri(int target, int pname, int param) {
        assertRenderThread();
        VulkanCallMapper.texParameteri(target, pname, param);
    }
    
    /**
     * Set texture parameter (float).
     */
    public void texParameterf(int target, int pname, float param) {
        assertRenderThread();
        VulkanCallMapper.texParameterf(target, pname, param);
    }

    // =========================================================================
    // SHADER AND PROGRAM OPERATIONS
    // =========================================================================
    
    /**
     * Create a shader.
     * 
     * @param type shader type (GL_VERTEX_SHADER, GL_FRAGMENT_SHADER, etc.)
     * @return shader handle
     */
    public long createShader(int type) {
        assertRenderThread();
        return VulkanCallMapper.createShader(type);
    }
    
    /**
     * Set shader source.
     */
    public void shaderSource(long shader, String source) {
        assertRenderThread();
        VulkanCallMapper.shaderSource(shader, source);
    }
    
    /**
     * Compile a shader.
     */
    public void compileShader(long shader) {
        assertRenderThread();
        VulkanCallMapper.compileShader(shader);
    }
    
    /**
     * Get shader compile status.
     */
    public boolean getShaderCompileStatus(long shader) {
        assertRenderThread();
        return VulkanCallMapper.getShaderiv(shader, 0x8B81) == 1; // GL_COMPILE_STATUS
    }
    
    /**
     * Get shader info log.
     */
    public String getShaderInfoLog(long shader) {
        assertRenderThread();
        return VulkanCallMapper.getShaderInfoLog(shader);
    }
    
    /**
     * Delete a shader.
     */
    public void deleteShader(long shader) {
        assertRenderThread();
        VulkanCallMapper.deleteShader(shader);
    }
    
    /**
     * Create a program.
     */
    public long createProgram() {
        assertRenderThread();
        return VulkanCallMapper.createProgram();
    }
    
    /**
     * Attach shader to program.
     */
    public void attachShader(long program, long shader) {
        assertRenderThread();
        VulkanCallMapper.attachShader(program, shader);
    }
    
    /**
     * Detach shader from program.
     */
    public void detachShader(long program, long shader) {
        assertRenderThread();
        VulkanCallMapper.detachShader(program, shader);
    }
    
    /**
     * Link a program.
     */
    public void linkProgram(long program) {
        assertRenderThread();
        VulkanCallMapper.linkProgram(program);
    }
    
    /**
     * Get program link status.
     */
    public boolean getProgramLinkStatus(long program) {
        assertRenderThread();
        return VulkanCallMapper.getProgramiv(program, 0x8B82) == 1; // GL_LINK_STATUS
    }
    
    /**
     * Get program info log.
     */
    public String getProgramInfoLog(long program) {
        assertRenderThread();
        return VulkanCallMapper.getProgramInfoLog(program);
    }
    
    /**
     * Use a program.
     * <p>Uses state caching - redundant binds are elided.</p>
     */
    public void useProgram(long program) {
        assertRenderThread();
        
        // Program binding affects pipeline, so we can't skip easily
        // But we can track for state queries
        VulkanCallMapper.useProgram(program);
        vulkanState.useProgram(program);
    }
    
    /**
     * Delete a program.
     */
    public void deleteProgram(long program) {
        assertRenderThread();
        VulkanCallMapper.deleteProgram(program);
    }
    
    /**
     * Get uniform location.
     */
    public int getUniformLocation(long program, String name) {
        assertRenderThread();
        return VulkanCallMapper.getUniformLocation(program, name);
    }
    
    // Uniform setters
    public void uniform1i(int location, int v) {
        assertRenderThread();
        VulkanCallMapper.uniform1i(location, v);
    }
    
    public void uniform1f(int location, float v) {
        assertRenderThread();
        VulkanCallMapper.uniform1f(location, v);
    }
    
    public void uniform2f(int location, float v0, float v1) {
        assertRenderThread();
        VulkanCallMapper.uniform2f(location, v0, v1);
    }
    
    public void uniform3f(int location, float v0, float v1, float v2) {
        assertRenderThread();
        VulkanCallMapper.uniform3f(location, v0, v1, v2);
    }
    
    public void uniform4f(int location, float v0, float v1, float v2, float v3) {
        assertRenderThread();
        VulkanCallMapper.uniform4f(location, v0, v1, v2, v3);
    }
    
    public void uniformMatrix4fv(int location, boolean transpose, float[] value) {
        assertRenderThread();
        VulkanCallMapper.uniformMatrix4fv(location, transpose, value);
    }

    // =========================================================================
    // VERTEX ARRAY OPERATIONS
    // =========================================================================
    
    /**
     * Generate a vertex array object.
     */
    public long genVertexArray() {
        assertRenderThread();
        return VulkanCallMapper.genVertexArray();
    }
    
    /**
     * Bind a vertex array object.
     */
    public void bindVertexArray(long vao) {
        assertRenderThread();
        VulkanCallMapper.bindVertexArray(vao);
        vulkanState.bindVertexArray(vao);
    }
    
    /**
     * Delete a vertex array object.
     */
    public void deleteVertexArray(long vao) {
        assertRenderThread();
        VulkanCallMapper.deleteVertexArray(vao);
    }
    
    /**
     * Set vertex attribute pointer.
     */
    public void vertexAttribPointer(int index, int size, int type, boolean normalized,
                                     int stride, long pointer) {
        assertRenderThread();
        VulkanCallMapper.vertexAttribPointer(index, size, type, normalized, stride, pointer);
    }
    
    /**
     * Enable vertex attribute array.
     */
    public void enableVertexAttribArray(int index) {
        assertRenderThread();
        VulkanCallMapper.enableVertexAttribArray(index);
    }
    
    /**
     * Disable vertex attribute array.
     */
    public void disableVertexAttribArray(int index) {
        assertRenderThread();
        VulkanCallMapper.disableVertexAttribArray(index);
    }
    
    /**
     * Set vertex attribute divisor (for instancing).
     */
    public void vertexAttribDivisor(int index, int divisor) {
        assertRenderThread();
        VulkanCallMapper.vertexAttribDivisor(index, divisor);
    }

    // =========================================================================
    // DRAW OPERATIONS - STANDARD
    // =========================================================================
    
    /**
     * Draw arrays.
     * 
     * @param mode primitive mode (GL_TRIANGLES, etc.)
     * @param first first vertex
     * @param count vertex count
     */
    public void drawArrays(int mode, int first, int count) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        VulkanCallMapper.drawArrays(mode, first, count);
        
        if (debug) {
            frameStats.drawCalls++;
            frameStats.vertices += count;
        }
    }
    
    /**
     * Draw elements (indexed).
     * 
     * @param mode primitive mode
     * @param count index count
     * @param type index type (GL_UNSIGNED_SHORT, GL_UNSIGNED_INT)
     * @param indices offset into bound element buffer
     */
    public void drawElements(int mode, int count, int type, long indices) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        VulkanCallMapper.drawElements(mode, count, type, indices);
        
        if (debug) {
            frameStats.drawCalls++;
            frameStats.vertices += count;
            if (mode == 0x0004) { // GL_TRIANGLES
                frameStats.triangles += count / 3;
            }
        }
    }
    
    /**
     * Draw arrays instanced.
     */
    public void drawArraysInstanced(int mode, int first, int count, int instanceCount) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        VulkanCallMapper.drawArraysInstanced(mode, first, count, instanceCount);
        
        if (debug) {
            frameStats.drawCalls++;
            frameStats.vertices += count * instanceCount;
            frameStats.instances += instanceCount;
        }
    }
    
    /**
     * Draw elements instanced.
     */
    public void drawElementsInstanced(int mode, int count, int type, long indices, int instanceCount) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        VulkanCallMapper.drawElementsInstanced(mode, count, type, indices, instanceCount);
        
        if (debug) {
            frameStats.drawCalls++;
            frameStats.vertices += count * instanceCount;
            frameStats.instances += instanceCount;
            if (mode == 0x0004) {
                frameStats.triangles += (count / 3) * instanceCount;
            }
        }
    }
    
    /**
     * Draw arrays instanced with base instance.
     */
    public void drawArraysInstancedBaseInstance(int mode, int first, int count,
                                                 int instanceCount, int baseInstance) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        VulkanCallMapper.drawArraysInstancedBaseInstance(mode, first, count, instanceCount, baseInstance);
        
        if (debug) {
            frameStats.drawCalls++;
            frameStats.vertices += count * instanceCount;
            frameStats.instances += instanceCount;
        }
    }
    
    /**
     * Draw elements instanced with base vertex and base instance.
     */
    public void drawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type,
                                                             long indices, int instanceCount,
                                                             int baseVertex, int baseInstance) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        VulkanCallMapper.drawElementsInstancedBaseVertexBaseInstance(
            mode, count, type, indices, instanceCount, baseVertex, baseInstance);
        
        if (debug) {
            frameStats.drawCalls++;
            frameStats.vertices += count * instanceCount;
            frameStats.instances += instanceCount;
        }
    }

    // =========================================================================
    // DRAW OPERATIONS - INDIRECT (GPU-DRIVEN)
    // =========================================================================
    
    /**
     * Draw arrays indirect.
     * <p>Draw parameters are read from a buffer.</p>
     * 
     * @param mode primitive mode
     * @param indirect buffer handle containing draw parameters
     * @param offset byte offset into buffer
     */
    public void drawArraysIndirect(int mode, long indirect, long offset) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        if (!hasMultiDrawIndirect) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Indirect drawing not supported");
            }
            return;
        }
        
        VulkanCallMapper.drawArraysIndirect(mode, indirect, offset);
        
        if (debug) {
            frameStats.drawCalls++;
        }
    }
    
    /**
     * Draw elements indirect.
     */
    public void drawElementsIndirect(int mode, int type, long indirect, long offset) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        if (!hasMultiDrawIndirect) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Indirect drawing not supported");
            }
            return;
        }
        
        VulkanCallMapper.drawElementsIndirect(mode, type, indirect, offset);
        
        if (debug) {
            frameStats.drawCalls++;
        }
    }
    
    /**
     * Multi-draw arrays indirect.
     * <p>Execute multiple indirect draws in a single call.</p>
     * 
     * @param mode primitive mode
     * @param indirect buffer containing draw commands
     * @param offset byte offset into buffer
     * @param drawCount number of draws
     * @param stride bytes between draw commands
     */
    public void multiDrawArraysIndirect(int mode, long indirect, long offset, 
                                         int drawCount, int stride) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        if (!hasMultiDrawIndirect) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Multi-draw indirect not supported");
            }
            return;
        }
        
        VulkanCallMapper.multiDrawArraysIndirect(mode, indirect, offset, drawCount, stride);
        
        if (debug) {
            frameStats.drawCalls += drawCount;
        }
    }
    
    /**
     * Multi-draw elements indirect.
     */
    public void multiDrawElementsIndirect(int mode, int type, long indirect, long offset,
                                           int drawCount, int stride) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        if (!hasMultiDrawIndirect) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Multi-draw indirect not supported");
            }
            return;
        }
        
        VulkanCallMapper.multiDrawElementsIndirect(mode, type, indirect, offset, drawCount, stride);
        
        if (debug) {
            frameStats.drawCalls += drawCount;
        }
    }
    
    /**
     * Multi-draw arrays indirect with count.
     * <p>Draw count is read from a buffer (GPU-determined draw count).</p>
     * <p>Requires Vulkan 1.2+ or VK_KHR_draw_indirect_count extension.</p>
     */
    public void multiDrawArraysIndirectCount(int mode, long indirect, long indirectOffset,
                                              long countBuffer, long countOffset,
                                              int maxDrawCount, int stride) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        if (!hasIndirectCount) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Indirect count not supported (requires VK 1.2+)");
            }
            return;
        }
        
        VulkanCallMapper.drawArraysIndirectCount(mode, indirect, indirectOffset,
            countBuffer, countOffset, maxDrawCount, stride);
        
        if (debug) {
            // Actual draw count unknown until GPU executes
            frameStats.drawCalls++;
        }
    }
    
    /**
     * Multi-draw elements indirect with count.
     */
    public void multiDrawElementsIndirectCount(int mode, int type, long indirect, long indirectOffset,
                                                long countBuffer, long countOffset,
                                                int maxDrawCount, int stride) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        if (!hasIndirectCount) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Indirect count not supported (requires VK 1.2+)");
            }
            return;
        }
        
        VulkanCallMapper.drawElementsIndirectCount(mode, type, indirect, indirectOffset,
            countBuffer, countOffset, maxDrawCount, stride);
        
        if (debug) {
            frameStats.drawCalls++;
        }
    }

    // =========================================================================
    // MESH SHADER DRAW OPERATIONS (VK 1.4+ / VK_EXT_mesh_shader)
    // =========================================================================
    
    /**
     * Draw mesh tasks.
     * <p>Dispatches mesh shader workgroups.</p>
     * 
     * @param groupCountX workgroup count X
     * @param groupCountY workgroup count Y
     * @param groupCountZ workgroup count Z
     */
    public void drawMeshTasks(int groupCountX, int groupCountY, int groupCountZ) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        if (!hasMeshShaders) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Mesh shaders not supported");
            }
            return;
        }
        
        drawDispatch.drawMeshTasks(groupCountX, groupCountY, groupCountZ);
        
        if (debug) {
            frameStats.drawCalls++;
        }
    }
    
    /**
     * Draw mesh tasks indirect.
     */
    public void drawMeshTasksIndirect(long indirect, long offset, int drawCount, int stride) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        if (!hasMeshShaders || !drawDispatch.hasMeshShaders) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Mesh shader indirect not supported");
            }
            return;
        }
        
        try {
            drawDispatch.mhDrawMeshTasksIndirect.invokeExact(indirect, offset, drawCount, stride);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        
        if (debug) {
            frameStats.drawCalls += drawCount;
        }
    }
    
    /**
     * Draw mesh tasks indirect with count.
     */
    public void drawMeshTasksIndirectCount(long indirect, long indirectOffset,
                                            long countBuffer, long countOffset,
                                            int maxDrawCount, int stride) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        if (!hasMeshShaders || !hasIndirectCount) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Mesh shader indirect count not supported");
            }
            return;
        }
        
        try {
            drawDispatch.mhDrawMeshTasksIndirectCount.invokeExact(
                indirect, indirectOffset, countBuffer, countOffset, maxDrawCount, stride);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        
        if (debug) {
            frameStats.drawCalls++;
        }
    }

    // =========================================================================
    // COMPUTE OPERATIONS
    // =========================================================================
    
    /**
     * Dispatch compute shader.
     * 
     * @param groupCountX workgroup count X
     * @param groupCountY workgroup count Y
     * @param groupCountZ workgroup count Z
     */
    public void dispatchCompute(int groupCountX, int groupCountY, int groupCountZ) {
        assertRenderThread();
        ensureRecording();
        
        computeDispatch.dispatch(groupCountX, groupCountY, groupCountZ);
    }
    
    /**
     * Dispatch compute shader indirect.
     * 
     * @param indirect buffer containing dispatch parameters
     * @param offset byte offset into buffer
     */
    public void dispatchComputeIndirect(long indirect, long offset) {
        assertRenderThread();
        ensureRecording();
        
        if (!computeDispatch.hasDispatchIndirect) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Indirect compute dispatch not supported");
            }
            return;
        }
        
        computeDispatch.dispatchIndirect(indirect, offset);
    }
    
    /**
     * Dispatch compute with base offsets (VK 1.1+).
     */
    public void dispatchComputeBase(int baseGroupX, int baseGroupY, int baseGroupZ,
                                     int groupCountX, int groupCountY, int groupCountZ) {
        assertRenderThread();
        ensureRecording();
        
        computeDispatch.dispatchBase(baseGroupX, baseGroupY, baseGroupZ,
                                      groupCountX, groupCountY, groupCountZ);
    }

    // =========================================================================
    // SYNCHRONIZATION OPERATIONS
    // =========================================================================
    
    /**
     * Create a fence sync object.
     * 
     * @param condition sync condition (GL_SYNC_GPU_COMMANDS_COMPLETE)
     * @param flags sync flags (must be 0)
     * @return sync object handle
     */
    public long fenceSync(int condition, int flags) {
        assertRenderThread();
        return VulkanCallMapper.fenceSync(condition, flags);
    }
    
    /**
     * Wait for a sync object on the client (CPU).
     * 
     * @param sync sync object
     * @param flags wait flags
     * @param timeout timeout in nanoseconds
     * @return wait status (GL_ALREADY_SIGNALED, GL_TIMEOUT_EXPIRED, GL_CONDITION_SATISFIED, GL_WAIT_FAILED)
     */
    public int clientWaitSync(long sync, int flags, long timeout) {
        assertRenderThread();
        return VulkanCallMapper.clientWaitSync(sync, flags, timeout);
    }
    
    /**
     * Wait for sync on the server (GPU).
     */
    public void waitSync(long sync, int flags, long timeout) {
        assertRenderThread();
        VulkanCallMapper.waitSync(sync, flags, timeout);
    }
    
    /**
     * Get sync object status.
     */
    public int getSyncStatus(long sync) {
        assertRenderThread();
        return VulkanCallMapper.getSyncStatus(sync);
    }
    
    /**
     * Delete a sync object.
     */
    public void deleteSync(long sync) {
        assertRenderThread();
        VulkanCallMapper.deleteSync(sync);
    }
    
    /**
     * Insert a memory barrier.
     * 
     * @param barriers barrier bits (GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT, etc.)
     */
    public void memoryBarrier(int barriers) {
        assertRenderThread();
        ensureRecording();
        
        VulkanCallMapper.memoryBarrier(barriers);
        
        if (debug) {
            frameStats.barriers++;
        }
    }
    
    /**
     * Create a Vulkan fence.
     */
    public long createFence(boolean signaled) {
        assertRenderThread();
        return syncDispatch.createFence(signaled);
    }
    
    /**
     * Destroy a Vulkan fence.
     */
    public void destroyFence(long fence) {
        assertRenderThread();
        syncDispatch.destroyFence(fence);
    }
    
    /**
     * Wait for a Vulkan fence.
     */
    public boolean waitForFence(long fence, long timeout) {
        assertRenderThread();
        return syncDispatch.waitForFence(fence, timeout);
    }
    
    /**
     * Reset a Vulkan fence.
     */
    public void resetFence(long fence) {
        assertRenderThread();
        syncDispatch.resetFence(fence);
    }
    
    /**
     * Create a timeline semaphore (VK 1.2+).
     * 
     * @param initialValue initial counter value
     * @return semaphore handle, or 0 if not supported
     */
    public long createTimelineSemaphore(long initialValue) {
        assertRenderThread();
        
        if (!hasTimelineSemaphores) {
            if (settings.strictNoEmulation) {
                throw new UnsupportedOperationException("Timeline semaphores not supported");
            }
            return 0;
        }
        
        return syncDispatch.createTimelineSemaphore(initialValue);
    }
    
    /**
     * Signal a timeline semaphore.
     */
    public void signalSemaphore(long semaphore, long value) {
        assertRenderThread();
        syncDispatch.signalSemaphore(semaphore, value);
    }
    
    /**
     * Wait on a timeline semaphore.
     */
    public boolean waitSemaphore(long semaphore, long value, long timeout) {
        assertRenderThread();
        return syncDispatch.waitSemaphore(semaphore, value, timeout);
    }
    
    /**
     * Get current timeline semaphore value.
     */
    public long getSemaphoreCounterValue(long semaphore) {
        assertRenderThread();
        return syncDispatch.getSemaphoreCounterValue(semaphore);
    }

    // =========================================================================
    // STATE MANAGEMENT - CAPABILITIES
    // =========================================================================
    
    /**
     * Enable a capability.
     */
    public void enable(int cap) {
        assertRenderThread();
        
        int capBit = capToBit(cap);
        if (capBit != 0) {
            if (settings.cacheBindings && stateCache.hasCapability(capBit)) {
                return; // Already enabled
            }
            stateCache.setCapability(capBit, true);
        }
        
        VulkanCallMapper.enable(cap);
        vulkanState.enable(cap);
    }
    
    /**
     * Disable a capability.
     */
    public void disable(int cap) {
        assertRenderThread();
        
        int capBit = capToBit(cap);
        if (capBit != 0) {
            if (settings.cacheBindings && !stateCache.hasCapability(capBit)) {
                return; // Already disabled
            }
            stateCache.setCapability(capBit, false);
        }
        
        VulkanCallMapper.disable(cap);
        vulkanState.disable(cap);
    }
    
    /**
     * Check if capability is enabled.
     */
    public boolean isEnabled(int cap) {
        assertRenderThread();
        
        if (settings.cacheBindings) {
            int capBit = capToBit(cap);
            if (capBit != 0) {
                return stateCache.hasCapability(capBit);
            }
        }
        
        return vulkanState.isEnabled(cap);
    }
    
    /**
     * Map GL capability to state cache bit.
     */
    private static int capToBit(int cap) {
        switch (cap) {
            case 0x0B71: return StateCache.CAP_DEPTH_TEST;      // GL_DEPTH_TEST
            case 0x0BE2: return StateCache.CAP_BLEND;            // GL_BLEND
            case 0x0B90: return StateCache.CAP_STENCIL_TEST;     // GL_STENCIL_TEST
            case 0x0B44: return StateCache.CAP_CULL_FACE;        // GL_CULL_FACE
            case 0x0C11: return StateCache.CAP_SCISSOR_TEST;     // GL_SCISSOR_TEST
            case 0x8037: return StateCache.CAP_DEPTH_BIAS;       // GL_POLYGON_OFFSET_FILL
            case 0x8F9D: return StateCache.CAP_PRIMITIVE_RESTART; // GL_PRIMITIVE_RESTART
            case 0x8C89: return StateCache.CAP_RASTERIZER_DISCARD; // GL_RASTERIZER_DISCARD
            default: return 0;
        }
    }

    // =========================================================================
    // STATE MANAGEMENT - DYNAMIC STATE
    // =========================================================================
    
    /**
     * Set viewport.
     */
    public void viewport(int x, int y, int width, int height) {
        assertRenderThread();
        
        if (settings.cacheBindings) {
            if (stateCache.viewportX == x && stateCache.viewportY == y &&
                stateCache.viewportWidth == width && stateCache.viewportHeight == height) {
                return;
            }
        }
        
        stateCache.viewportX = x;
        stateCache.viewportY = y;
        stateCache.viewportWidth = width;
        stateCache.viewportHeight = height;
        stateCache.markDirty(StateCache.DIRTY_VIEWPORT);
        
        VulkanCallMapper.viewport(x, y, width, height);
        vulkanState.setViewport(x, y, width, height);
    }
    
    /**
     * Set scissor rectangle.
     */
    public void scissor(int x, int y, int width, int height) {
        assertRenderThread();
        
        if (settings.cacheBindings) {
            if (stateCache.scissorX == x && stateCache.scissorY == y &&
                stateCache.scissorWidth == width && stateCache.scissorHeight == height) {
                return;
            }
        }
        
        stateCache.scissorX = x;
        stateCache.scissorY = y;
        stateCache.scissorWidth = width;
        stateCache.scissorHeight = height;
        stateCache.markDirty(StateCache.DIRTY_SCISSOR);
        
        VulkanCallMapper.scissor(x, y, width, height);
        vulkanState.setScissor(x, y, width, height);
    }
    
    /**
     * Set depth function.
     */
    public void depthFunc(int func) {
        assertRenderThread();
        VulkanCallMapper.depthFunc(func);
        vulkanState.setDepthFunc(func);
        
        if (effectiveVulkanVersion >= VULKAN_1_3) {
            stateCache.depthCompareOp = translateDepthFunc(func);
            stateCache.markDirty(StateCache.DIRTY_DEPTH_COMPARE);
        }
    }
    
    /**
     * Set depth mask.
     */
    public void depthMask(boolean flag) {
        assertRenderThread();
        
        int bit = flag ? StateCache.CAP_DEPTH_WRITE : 0;
        if (settings.cacheBindings && stateCache.hasCapability(StateCache.CAP_DEPTH_WRITE) == flag) {
            return;
        }
        
        stateCache.setCapability(StateCache.CAP_DEPTH_WRITE, flag);
        stateCache.markDirty(StateCache.DIRTY_DEPTH_WRITE);
        
        VulkanCallMapper.depthMask(flag);
        vulkanState.setDepthMask(flag);
    }
    
    /**
     * Set depth range.
     */
    public void depthRange(double near, double far) {
        assertRenderThread();
        
        stateCache.viewportMinDepth = (float) near;
        stateCache.viewportMaxDepth = (float) far;
        stateCache.markDirty(StateCache.DIRTY_VIEWPORT);
        
        VulkanCallMapper.depthRange(near, far);
        vulkanState.setDepthRange((float) near, (float) far);
    }
    
    /**
     * Set blend function.
     */
    public void blendFunc(int sfactor, int dfactor) {
        assertRenderThread();
        VulkanCallMapper.blendFunc(sfactor, dfactor);
        vulkanState.setBlendFunc(sfactor, dfactor);
    }
    
    /**
     * Set blend function separate.
     */
    public void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        assertRenderThread();
        VulkanCallMapper.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
        vulkanState.setBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }
    
    /**
     * Set blend equation.
     */
    public void blendEquation(int mode) {
        assertRenderThread();
        VulkanCallMapper.blendEquation(mode);
        vulkanState.setBlendEquation(mode);
    }
    
    /**
     * Set blend equation separate.
     */
    public void blendEquationSeparate(int modeRGB, int modeAlpha) {
        assertRenderThread();
        VulkanCallMapper.blendEquationSeparate(modeRGB, modeAlpha);
        vulkanState.setBlendEquationSeparate(modeRGB, modeAlpha);
    }
    
    /**
     * Set blend color.
     */
    public void blendColor(float r, float g, float b, float a) {
        assertRenderThread();
        
        stateCache.blendConstants[0] = r;
        stateCache.blendConstants[1] = g;
        stateCache.blendConstants[2] = b;
        stateCache.blendConstants[3] = a;
        stateCache.markDirty(StateCache.DIRTY_BLEND_CONSTANTS);
        
        VulkanCallMapper.blendColor(r, g, b, a);
        vulkanState.setBlendColor(r, g, b, a);
    }
    
    /**
     * Set cull face mode.
     */
    public void cullFace(int mode) {
        assertRenderThread();
        
        if (effectiveVulkanVersion >= VULKAN_1_3) {
            stateCache.cullMode = translateCullMode(mode);
            stateCache.markDirty(StateCache.DIRTY_CULL_MODE);
        }
        
        VulkanCallMapper.cullFace(mode);
        vulkanState.setCullFace(mode);
    }
    
    /**
     * Set front face winding.
     */
    public void frontFace(int mode) {
        assertRenderThread();
        
        if (effectiveVulkanVersion >= VULKAN_1_3) {
            stateCache.frontFace = translateFrontFace(mode);
            stateCache.markDirty(StateCache.DIRTY_FRONT_FACE);
        }
        
        VulkanCallMapper.frontFace(mode);
        vulkanState.setFrontFace(mode);
    }
    
    /**
     * Set polygon mode.
     */
    public void polygonMode(int face, int mode) {
        assertRenderThread();
        VulkanCallMapper.polygonMode(face, mode);
        vulkanState.setPolygonMode(face, mode);
    }
    
    /**
     * Set polygon offset.
     */
    public void polygonOffset(float factor, float units) {
        assertRenderThread();
        
        stateCache.depthBiasSlope = factor;
        stateCache.depthBiasConstant = units;
        stateCache.markDirty(StateCache.DIRTY_DEPTH_BIAS);
        
        VulkanCallMapper.polygonOffset(factor, units);
        vulkanState.setPolygonOffset(factor, units);
    }
    
    /**
     * Set line width.
     */
    public void lineWidth(float width) {
        assertRenderThread();
        
        if (settings.cacheBindings && stateCache.lineWidth == width) {
            return;
        }
        
        stateCache.lineWidth = width;
        stateCache.markDirty(StateCache.DIRTY_LINE_WIDTH);
        
        VulkanCallMapper.lineWidth(width);
        vulkanState.setLineWidth(width);
    }
    
    /**
     * Set stencil function.
     */
    public void stencilFunc(int func, int ref, int mask) {
        assertRenderThread();
        
        stateCache.stencilCompareMaskFront = mask;
        stateCache.stencilCompareMaskBack = mask;
        stateCache.stencilReferenceFront = ref;
        stateCache.stencilReferenceBack = ref;
        stateCache.markDirty(StateCache.DIRTY_STENCIL_COMPARE | StateCache.DIRTY_STENCIL_REF);
        
        VulkanCallMapper.stencilFunc(func, ref, mask);
        vulkanState.setStencilFunc(func, ref, mask);
    }
    
    /**
     * Set stencil operation.
     */
    public void stencilOp(int sfail, int dpfail, int dppass) {
        assertRenderThread();
        VulkanCallMapper.stencilOp(sfail, dpfail, dppass);
        vulkanState.setStencilOp(sfail, dpfail, dppass);
    }
    
    /**
     * Set stencil mask.
     */
    public void stencilMask(int mask) {
        assertRenderThread();
        
        stateCache.stencilWriteMaskFront = mask;
        stateCache.stencilWriteMaskBack = mask;
        stateCache.markDirty(StateCache.DIRTY_STENCIL_WRITE);
        
        VulkanCallMapper.stencilMask(mask);
        vulkanState.setStencilMask(mask);
    }
    
    /**
     * Set color mask.
     */
    public void colorMask(boolean r, boolean g, boolean b, boolean a) {
        assertRenderThread();
        VulkanCallMapper.colorMask(r, g, b, a);
        vulkanState.setColorMask(r, g, b, a);
    }
    
    /**
     * Clear buffers.
     */
    public void clear(int mask) {
        assertRenderThread();
        VulkanCallMapper.clear(mask);
        vulkanState.markClearRequested(mask);
    }
    
    /**
     * Set clear color.
     */
    public void clearColor(float r, float g, float b, float a) {
        assertRenderThread();
        VulkanCallMapper.clearColor(r, g, b, a);
        vulkanState.setClearColor(r, g, b, a);
    }
    
    /**
     * Set clear depth.
     */
    public void clearDepth(double depth) {
        assertRenderThread();
        VulkanCallMapper.clearDepth(depth);
        vulkanState.setClearDepth((float) depth);
    }
    
    /**
     * Set clear stencil.
     */
    public void clearStencil(int s) {
        assertRenderThread();
        VulkanCallMapper.clearStencil(s);
        vulkanState.setClearStencil(s);
    }
    
    /**
     * Flush dirty dynamic state before draw.
     */
    private void flushDirtyState() {
        if (!stateCache.commandBufferRecording) return;
        
        int dirty = stateCache.dirtyBits;
        if (dirty == 0) return;
        
        // Flush viewport if dirty
        if ((dirty & StateCache.DIRTY_VIEWPORT) != 0) {
            VulkanCallMapper.setDynamicViewport(
                stateCache.viewportX, stateCache.viewportY,
                stateCache.viewportWidth, stateCache.viewportHeight,
                stateCache.viewportMinDepth, stateCache.viewportMaxDepth);
        }
        
        // Flush scissor if dirty
        if ((dirty & StateCache.DIRTY_SCISSOR) != 0) {
            VulkanCallMapper.setDynamicScissor(
                stateCache.scissorX, stateCache.scissorY,
                stateCache.scissorWidth, stateCache.scissorHeight);
        }
        
        // Flush depth bias if dirty
        if ((dirty & StateCache.DIRTY_DEPTH_BIAS) != 0) {
            VulkanCallMapper.setDynamicDepthBias(
                stateCache.depthBiasConstant,
                stateCache.depthBiasClamp,
                stateCache.depthBiasSlope);
        }
        
        // Flush blend constants if dirty
        if ((dirty & StateCache.DIRTY_BLEND_CONSTANTS) != 0) {
            VulkanCallMapper.setDynamicBlendConstants(stateCache.blendConstants);
        }
        
        // Flush line width if dirty
        if ((dirty & StateCache.DIRTY_LINE_WIDTH) != 0) {
            VulkanCallMapper.setDynamicLineWidth(stateCache.lineWidth);
        }
        
        // VK 1.3+ extended dynamic state
        if (effectiveVulkanVersion >= VULKAN_1_3) {
            if ((dirty & StateCache.DIRTY_CULL_MODE) != 0) {
                VulkanCallMapper.setDynamicCullMode(stateCache.cullMode);
            }
            if ((dirty & StateCache.DIRTY_FRONT_FACE) != 0) {
                VulkanCallMapper.setDynamicFrontFace(stateCache.frontFace);
            }
            if ((dirty & StateCache.DIRTY_DEPTH_TEST) != 0) {
                VulkanCallMapper.setDynamicDepthTestEnable(
                    stateCache.hasCapability(StateCache.CAP_DEPTH_TEST));
            }
            if ((dirty & StateCache.DIRTY_DEPTH_WRITE) != 0) {
                VulkanCallMapper.setDynamicDepthWriteEnable(
                    stateCache.hasCapability(StateCache.CAP_DEPTH_WRITE));
            }
            if ((dirty & StateCache.DIRTY_DEPTH_COMPARE) != 0) {
                VulkanCallMapper.setDynamicDepthCompareOp(stateCache.depthCompareOp);
            }
        }
        
        // Clear dirty bits
        stateCache.dirtyBits = 0;
    }
    
    /**
     * Translate GL depth func to VK compare op.
     */
    private static int translateDepthFunc(int glFunc) {
        switch (glFunc) {
            case 0x0200: return VK_COMPARE_OP_NEVER;
            case 0x0201: return VK_COMPARE_OP_LESS;
            case 0x0202: return VK_COMPARE_OP_EQUAL;
            case 0x0203: return VK_COMPARE_OP_LESS_OR_EQUAL;
            case 0x0204: return VK_COMPARE_OP_GREATER;
            case 0x0205: return VK_COMPARE_OP_NOT_EQUAL;
            case 0x0206: return VK_COMPARE_OP_GREATER_OR_EQUAL;
            case 0x0207: return VK_COMPARE_OP_ALWAYS;
            default: return VK_COMPARE_OP_LESS;
        }
    }
    
    /**
     * Translate GL cull mode to VK cull mode.
     */
    private static int translateCullMode(int glMode) {
        switch (glMode) {
            case 0x0404: return VK_CULL_MODE_FRONT_BIT;
            case 0x0405: return VK_CULL_MODE_BACK_BIT;
            case 0x0408: return VK_CULL_MODE_FRONT_AND_BACK;
            default: return VK_CULL_MODE_BACK_BIT;
        }
    }
    
    /**
     * Translate GL front face to VK front face.
     */
    private static int translateFrontFace(int glMode) {
        switch (glMode) {
            case 0x0900: return VK_FRONT_FACE_CLOCKWISE;
            case 0x0901: return VK_FRONT_FACE_COUNTER_CLOCKWISE;
            default: return VK_FRONT_FACE_COUNTER_CLOCKWISE;
        }
    }

    // =========================================================================
    // GPU-DRIVEN RENDERING API
    // =========================================================================
    
    /**
     * GPU-driven draw batch builder.
     * <p>Efficiently builds indirect draw commands for GPU-driven rendering.</p>
     */
    public final class DrawBatch {
        private final int frameIndex;
        private int drawCount;
        private int instanceCount;
        private final ByteBuffer indirectBuffer;
        private final ByteBuffer instanceBuffer;
        
        private DrawBatch(int frameIndex) {
            this.frameIndex = frameIndex;
            this.drawCount = 0;
            this.instanceCount = 0;
            this.indirectBuffer = gpuDrivenState.indirectBufferMapped[frameIndex];
            this.instanceBuffer = gpuDrivenState.instanceBufferMapped[frameIndex];
            
            // Reset buffers
            indirectBuffer.clear();
            instanceBuffer.clear();
            
            // Reset count
            gpuDrivenState.countBufferMapped[frameIndex].putInt(0, 0);
        }
        
        /**
         * Add an indexed draw command.
         * 
         * @param indexCount number of indices
         * @param firstIndex first index in buffer
         * @param vertexOffset offset added to vertex indices
         * @return draw index for instancing
         */
        public int addDraw(int indexCount, int firstIndex, int vertexOffset) {
            if (drawCount >= gpuDrivenState.indirectBufferSize / 20) {
                throw new RuntimeException("Draw batch full");
            }
            
            int pos = drawCount * 20;
            indirectBuffer.putInt(pos, indexCount);      // indexCount
            indirectBuffer.putInt(pos + 4, 0);           // instanceCount (filled by GPU or later)
            indirectBuffer.putInt(pos + 8, firstIndex);  // firstIndex
            indirectBuffer.putInt(pos + 12, vertexOffset); // vertexOffset
            indirectBuffer.putInt(pos + 16, 0);          // firstInstance (filled later)
            
            return drawCount++;
        }
        
        /**
         * Add instance data.
         * 
         * @param transform 4x4 transform matrix (16 floats)
         * @param customData custom data (up to 4 floats)
         * @return instance index
         */
        public int addInstance(float[] transform, float[] customData) {
            if (instanceCount >= gpuDrivenState.maxInstances) {
                throw new RuntimeException("Instance buffer full");
            }
            
            int pos = instanceCount * 80;
            
            // Write transform (64 bytes)
            for (int i = 0; i < 16; i++) {
                instanceBuffer.putFloat(pos + i * 4, transform[i]);
            }
            
            // Write custom data (16 bytes)
            for (int i = 0; i < Math.min(4, customData.length); i++) {
                instanceBuffer.putFloat(pos + 64 + i * 4, customData[i]);
            }
            
            return instanceCount++;
        }
        
        /**
         * Set instance count for a draw.
         */
        public void setDrawInstanceCount(int drawIndex, int instanceCount, int firstInstance) {
            int pos = drawIndex * 20;
            indirectBuffer.putInt(pos + 4, instanceCount);
            indirectBuffer.putInt(pos + 16, firstInstance);
        }
        
        /**
         * Get current draw count.
         */
        public int getDrawCount() {
            return drawCount;
        }
        
        /**
         * Get current instance count.
         */
        public int getInstanceCount() {
            return instanceCount;
        }
        
        /**
         * Finalize batch and update count buffer.
         */
        public void finalize() {
            gpuDrivenState.countBufferMapped[frameIndex].putInt(0, drawCount);
            
            if (debug) {
                gpuDrivenState.totalDrawCalls.addAndGet(drawCount);
                gpuDrivenState.visibleInstances.addAndGet(instanceCount);
            }
        }
    }
    
    /**
     * Begin building a GPU-driven draw batch.
     * 
     * @return draw batch builder
     */
    public DrawBatch beginDrawBatch() {
        assertRenderThread();
        
        if (!gpuDrivenState.initialized) {
            throw new IllegalStateException("GPU-driven rendering not initialized");
        }
        
        return new DrawBatch(currentFrame.get());
    }
    
    /**
     * Submit a completed draw batch.
     * 
     * @param batch completed batch
     * @param mode primitive mode
     * @param indexType index type
     */
    public void submitDrawBatch(DrawBatch batch, int mode, int indexType) {
        assertRenderThread();
        ensureRecording();
        flushDirtyState();
        
        batch.finalize();
        
        int frameIndex = batch.frameIndex;
        long indirectBuffer = gpuDrivenState.indirectBuffers[frameIndex];
        long countBuffer = gpuDrivenState.countBuffers[frameIndex];
        
        // Bind instance buffer as vertex buffer (for instancing data)
        long instanceBuffer = gpuDrivenState.instanceBuffers[frameIndex];
        VulkanCallMapper.bindVertexBufferSlot(1, instanceBuffer, 0);
        
        if (hasIndirectCount) {
            // Use indirect count for fully GPU-driven
            multiDrawElementsIndirectCount(mode, indexType, indirectBuffer, 0,
                countBuffer, 0, batch.getDrawCount(), 20);
        } else {
            // Fallback to regular MDI
            multiDrawElementsIndirect(mode, indexType, indirectBuffer, 0, 
                batch.getDrawCount(), 20);
        }
    }
    
    /**
     * Execute GPU culling compute pass.
     * <p>Culls instances using GPU compute shader and updates draw counts.</p>
     * 
     * @param frustumPlanes 6 frustum planes (24 floats)
     * @param instanceCount number of instances to cull
     */
    public void executeGPUCulling(float[] frustumPlanes, int instanceCount) {
        assertRenderThread();
        ensureRecording();
        
        if (!settings.enableGPUCulling || gpuDrivenState.cullingPipeline == VK_NULL_HANDLE) {
            return;
        }
        
        // Upload frustum data
        // Bind compute pipeline
        // Dispatch culling compute shader
        // Insert memory barrier
        
        int workGroupSize = 256;
        int workGroupCount = (instanceCount + workGroupSize - 1) / workGroupSize;
        
        // TODO: Implement actual culling dispatch
        // computeDispatch.dispatch(workGroupCount, 1, 1);
        
        if (debug) {
            gpuDrivenState.culledInstances.addAndGet(0); // Update with actual culled count
        }
    }

    // =========================================================================
    // DIAGNOSTICS AND STATUS
    // =========================================================================
    
    /**
     * Get Vulkan version info.
     */
    public int getDetectedVulkanVersion() { return detectedVulkanVersion; }
    public int getEffectiveVulkanVersion() { return effectiveVulkanVersion; }
    public int getBufferOpsVersion() { return bufferOpsVersion; }
    public String getVersionString() { return rawVersionString; }
    
    /**
     * Feature availability queries.
     */
    public boolean hasTimelineSemaphores() { return hasTimelineSemaphores; }
    public boolean hasDynamicRendering() { return hasDynamicRendering; }
    public boolean hasSynchronization2() { return hasSynchronization2; }
    public boolean hasBufferDeviceAddress() { return hasBufferDeviceAddress; }
    public boolean hasDescriptorIndexing() { return hasDescriptorIndexing; }
    public boolean hasMeshShaders() { return hasMeshShaders; }
    public boolean hasMultiDrawIndirect() { return hasMultiDrawIndirect; }
    public boolean hasIndirectCount() { return hasIndirectCount; }
    
    /**
     * Settings queries.
     */
    public boolean isCacheBindingsEnabled() { return settings.cacheBindings; }
    public boolean isStrictNoEmulation() { return settings.strictNoEmulation; }
    public boolean isDebugEnabled() { return debug; }
    public boolean isGPUDrivenRenderingEnabled() { 
        return settings.enableGPUDrivenRendering && gpuDrivenState != null && gpuDrivenState.initialized; 
    }
    
    /**
     * Get configuration summary.
     */
    public String getConfigSummary() {
        return String.format(
            "VulkanManager[VK=%s, detected=%s, effective=%s, ops=%d, " +
            "timeline=%b, dynRender=%b, sync2=%b, BDA=%b, descIdx=%b, mesh=%b, MDI=%b, indCount=%b, " +
            "gpuDriven=%b, frames=%d, batch=%d]",
            rawVersionString,
            formatVulkanVersion(detectedVulkanVersion),
            formatVulkanVersion(effectiveVulkanVersion),
            bufferOpsVersion,
            hasTimelineSemaphores, hasDynamicRendering, hasSynchronization2,
            hasBufferDeviceAddress, hasDescriptorIndexing, hasMeshShaders,
            hasMultiDrawIndirect, hasIndirectCount,
            isGPUDrivenRenderingEnabled(),
            maxFramesInFlight, commandBatchSize
        );
    }
    
    /**
     * Get detailed status report.
     */
    public String getStatusReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== VulkanManager Status Report ===\n\n");
        
        // Version info
        sb.append("VERSION INFO:\n");
        sb.append("  Detected Vulkan: ").append(formatVulkanVersion(detectedVulkanVersion)).append("\n");
        sb.append("  Effective Vulkan: ").append(formatVulkanVersion(effectiveVulkanVersion)).append("\n");
        sb.append("  Buffer Ops Version: ").append(bufferOpsVersion).append("\n");
        sb.append("  Raw Version String: ").append(rawVersionString).append("\n\n");
        
        // Features
        sb.append("FEATURES:\n");
        sb.append("  Timeline Semaphores: ").append(hasTimelineSemaphores).append("\n");
        sb.append("  Dynamic Rendering: ").append(hasDynamicRendering).append("\n");
        sb.append("  Synchronization2: ").append(hasSynchronization2).append("\n");
        sb.append("  Buffer Device Address: ").append(hasBufferDeviceAddress).append("\n");
        sb.append("  Descriptor Indexing: ").append(hasDescriptorIndexing).append("\n");
        sb.append("  Mesh Shaders: ").append(hasMeshShaders).append("\n");
        sb.append("  Multi-Draw Indirect: ").append(hasMultiDrawIndirect).append("\n");
        sb.append("  Indirect Count: ").append(hasIndirectCount).append("\n\n");
        
        // Frame info
        sb.append("FRAME INFO:\n");
        sb.append("  Max Frames in Flight: ").append(maxFramesInFlight).append("\n");
        sb.append("  Current Frame Index: ").append(currentFrame.get()).append("\n");
        sb.append("  Total Frames: ").append(frameCounter.get()).append("\n");
        sb.append("  Command Recording: ").append(stateCache.commandBufferRecording).append("\n\n");
        
        // GPU-driven info
        if (gpuDrivenState != null && gpuDrivenState.initialized) {
            sb.append("GPU-DRIVEN RENDERING:\n");
            sb.append("  Total Draw Calls: ").append(gpuDrivenState.totalDrawCalls.get()).append("\n");
            sb.append("  Visible Instances: ").append(gpuDrivenState.visibleInstances.get()).append("\n");
            sb.append("  Culled Instances: ").append(gpuDrivenState.culledInstances.get()).append("\n");
            sb.append("  Max Instances/Frame: ").append(gpuDrivenState.maxInstances).append("\n\n");
        }
        
        // Frame statistics
        sb.append("FRAME STATS:\n");
        sb.append("  Draw Calls: ").append(frameStats.drawCalls).append("\n");
        sb.append("  Triangles: ").append(frameStats.triangles).append("\n");
        sb.append("  Vertices: ").append(frameStats.vertices).append("\n");
        sb.append("  Instances: ").append(frameStats.instances).append("\n");
        sb.append("  Pipeline Binds: ").append(frameStats.pipelineBinds).append("\n");
        sb.append("  Buffer Binds: ").append(frameStats.bufferBinds).append("\n");
        sb.append("  Barriers: ").append(frameStats.barriers).append("\n\n");
        
        // State cache
        sb.append("STATE CACHE:\n");
        sb.append("  Buffer Binding Mask: 0x").append(Integer.toHexString(stateCache.bufferBindingMask)).append("\n");
        sb.append("  Texture Binding Mask: 0x").append(Integer.toHexString(stateCache.textureBindingMask)).append("\n");
        sb.append("  Capability Bits: 0x").append(Integer.toHexString(stateCache.capabilityBits)).append("\n");
        sb.append("  Dirty Bits: 0x").append(Integer.toHexString(stateCache.dirtyBits)).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Log status to console.
     */
    public void logStatus() {
        Astralis.LOGGER.info(getStatusReport());
    }

    // =========================================================================
    // SHUTDOWN
    // =========================================================================
    
    /**
     * Shutdown the manager and release all resources.
     */
    public void shutdown() {
        assertRenderThread();
        
        Astralis.LOGGER.info("[VulkanManager] Shutting down...");
        
        // Wait for GPU to finish
        try {
            vkDeviceWaitIdle(context.device);
        } catch (Exception e) {
            Astralis.LOGGER.warn("[VulkanManager] Error waiting for device idle: {}", e.getMessage());
        }
        
        // Shutdown GPU-driven systems
        if (gpuDrivenState != null && gpuDrivenState.initialized) {
            shutdownGPUDrivenSystems();
        }
        
        // Destroy timeline semaphore
        if (timelineSemaphore != 0) {
            syncDispatch.destroySemaphore(timelineSemaphore);
        }
        
        // Shutdown buffer dispatch
        try {
            bufferDispatch.shutdown();
        } catch (Exception e) {
            Astralis.LOGGER.warn("[VulkanManager] Error shutting down buffer dispatch: {}", e.getMessage());
        }
        
        // Shutdown call mapper
        try {
            VulkanCallMapper.shutdown();
        } catch (Exception e) {
            Astralis.LOGGER.warn("[VulkanManager] Error shutting down call mapper: {}", e.getMessage());
        }
        
        // Clear state
        stateCache.invalidateAll();
        
        // Clear singleton
        synchronized (VulkanManager.class) {
            PUBLISHED = null;
            FAST = null;
        }
        
        Astralis.LOGGER.info("[VulkanManager] Shutdown complete");
    }
    
    /**
     * Shutdown GPU-driven rendering systems.
     */
    private void shutdownGPUDrivenSystems() {
        // Destroy indirect buffers
        for (int i = 0; i < maxFramesInFlight; i++) {
            if (gpuDrivenState.indirectBufferMapped[i] != null) {
                vkUnmapMemory(context.device, gpuDrivenState.indirectBufferMemory[i]);
            }
            if (gpuDrivenState.indirectBuffers[i] != 0) {
                vkDestroyBuffer(context.device, gpuDrivenState.indirectBuffers[i], null);
                vkFreeMemory(context.device, gpuDrivenState.indirectBufferMemory[i], null);
            }
            
            if (gpuDrivenState.countBufferMapped[i] != null) {
                vkUnmapMemory(context.device, gpuDrivenState.countBufferMemory[i]);
            }
            if (gpuDrivenState.countBuffers[i] != 0) {
                vkDestroyBuffer(context.device, gpuDrivenState.countBuffers[i], null);
                vkFreeMemory(context.device, gpuDrivenState.countBufferMemory[i], null);
            }
            
            if (gpuDrivenState.instanceBufferMapped[i] != null) {
                vkUnmapMemory(context.device, gpuDrivenState.instanceBufferMemory[i]);
            }
            if (gpuDrivenState.instanceBuffers[i] != 0) {
                vkDestroyBuffer(context.device, gpuDrivenState.instanceBuffers[i], null);
                vkFreeMemory(context.device, gpuDrivenState.instanceBufferMemory[i], null);
            }
        }
        
        // Destroy culling resources
        if (gpuDrivenState.cullingPipeline != VK_NULL_HANDLE) {
            vkDestroyPipeline(context.device, gpuDrivenState.cullingPipeline, null);
        }
        if (gpuDrivenState.cullingPipelineLayout != 0) {
            vkDestroyPipelineLayout(context.device, gpuDrivenState.cullingPipelineLayout, null);
        }
        if (gpuDrivenState.cullingDescriptorSetLayout != 0) {
            vkDestroyDescriptorSetLayout(context.device, gpuDrivenState.cullingDescriptorSetLayout, null);
        }
        
        // Destroy meshlet resources
        if (gpuDrivenState.meshletBuffer != 0) {
            vkDestroyBuffer(context.device, gpuDrivenState.meshletBuffer, null);
            vkFreeMemory(context.device, gpuDrivenState.meshletBufferMemory, null);
        }
        if (gpuDrivenState.meshletDataBuffer != 0) {
            vkDestroyBuffer(context.device, gpuDrivenState.meshletDataBuffer, null);
            vkFreeMemory(context.device, gpuDrivenState.meshletDataBufferMemory, null);
        }
        
        gpuDrivenState.initialized = false;
    }
}
