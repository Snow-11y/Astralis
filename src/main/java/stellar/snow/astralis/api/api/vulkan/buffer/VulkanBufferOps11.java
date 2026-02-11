package stellar.snow.astralis.api.vulkan.buffer;

import stellar.snow.astralis.api.opengl.buffer.BufferOps;
import stellar.snow.astralis.api.vulkan.mapping.VulkanCallMapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * VulkanBufferOps11 - Vulkan 1.1 Multi-GPU & Subgroup Pipeline
 * 
 * ╔════════════════════════════════════════════════════════════════════════════════════╗
 * ║       ★★★★★★★★★ VULKAN 1.1 - MULTI-GPU & SUBGROUP REVOLUTION ★★★★★★★★★          ║
 * ║                                                                                    ║
 * ║  Vulkan 1.1 (March 2018) - THE PARALLEL COMPUTING EVOLUTION:                       ║
 * ║                                                                                    ║
 * ║  ┌────────────────────────────────────────────────────────────────────────────┐    ║
 * ║  │                    EVERYTHING FROM 1.0 PLUS:                               │    ║
 * ║  │                                                                            │    ║
 * ║  │  ★ SUBGROUP OPERATIONS                                                     │    ║
 * ║  │    • Wave-level parallel operations (shuffle, ballot, arithmetic)          │    ║
 * ║  │    • 32-64 threads execute in lockstep (GPU-dependent)                     │    ║
 * ║  │    • Massive speedup for parallel reductions and scans                     │    ║
 * ║  │                                                                            │    ║
 * ║  │  ★ DEVICE GROUPS (Multi-GPU)                                               │    ║
 * ║  │    • Explicit control over multiple GPUs                                   │    ║
 * ║  │    • AFR (Alternate Frame Rendering)                                       │    ║
 * ║  │    • SFR (Split Frame Rendering)                                           │    ║
 * ║  │    • Memory sharing between devices                                        │    ║
 * ║  │                                                                            │    ║
 * ║  │  ★ DESCRIPTOR UPDATE TEMPLATES                                             │    ║
 * ║  │    • Pre-compiled update patterns (5-10x faster binding updates)           │    ║
 * ║  │    • Reduce CPU overhead for resource binding                              │    ║
 * ║  │    • Batch multiple descriptor updates into one call                       │    ║
 * ║  │                                                                            │    ║
 * ║  │  ★ 16-BIT STORAGE                                                          │    ║
 * ║  │    • Half-precision floats in storage buffers                              │    ║
 * ║  │    • 50% memory reduction for compatible data                              │    ║
 * ║  │    • Faster memory bandwidth (2x throughput potential)                     │    ║
 * ║  │                                                                            │    ║
 * ║  │  ★ EXTERNAL MEMORY/SEMAPHORE/FENCE                                         │    ║
 * ║  │    • Share resources between Vulkan instances                              │    ║
 * ║  │    • Interop with OpenGL, CUDA, DirectX                                    │    ║
 * ║  │    • Cross-process resource sharing                                        │    ║
 * ║  │                                                                            │    ║
 * ║  │  ★ PROTECTED CONTENT                                                       │    ║
 * ║  │    • DRM-protected memory for premium content                              │    ║
 * ║  │    • Secure path from decode to display                                    │    ║
 * ║  │                                                                            │    ║
 * ║  │  ★ MULTIVIEW                                                               │    ║
 * ║  │    • Single-pass stereo rendering (VR optimization)                        │    ║
 * ║  │    • Multiple views from single draw call                                  │    ║
 * ║  │    • 40-50% VR performance improvement                                     │    ║
 * ║  └────────────────────────────────────────────────────────────────────────────┘    ║
 * ║                                                                                    ║
 * ║  ┌────────────────────────────────────────────────────────────────────────────┐    ║
 * ║  │                    SUBGROUP OPERATION PIPELINE:                            │    ║
 * ║  │                                                                            │    ║
 * ║  │   ┌─────────────────────────────────────────────────────────────────┐      │    ║
 * ║  │   │  SUBGROUP (Wave/Wavefront) - 32-64 threads                      │      │    ║
 * ║  │   │  ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐              │      │    ║
 * ║  │   │  │ T0  │ T1  │ T2  │ T3  │ T4  │ T5  │ T6  │ T7  │ ... T31     │      │    ║
 * ║  │   │  └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┘              │      │    ║
 * ║  │   │     │     │     │     │     │     │     │                       │      │    ║
 * ║  │   │     └─────┴─────┴─────┴─────┴─────┴─────┘                       │      │    ║
 * ║  │   │                    │                                            │      │    ║
 * ║  │   │                    ▼                                            │      │    ║
 * ║  │   │     ┌──────────────────────────────────┐                        │      │    ║
 * ║  │   │     │ subgroupAdd, subgroupBallot,     │                        │      │    ║
 * ║  │   │     │ subgroupShuffle, subgroupBroadcast│                       │      │    ║
 * ║  │   │     └──────────────────────────────────┘                        │      │    ║
 * ║  │   │                    │                                            │      │    ║
 * ║  │   │                    ▼                                            │      │    ║
 * ║  │   │              SINGLE RESULT                                      │      │    ║
 * ║  │   │         (All 32 threads → 1 value)                              │      │    ║
 * ║  │   └─────────────────────────────────────────────────────────────────┘      │    ║
 * ║  └────────────────────────────────────────────────────────────────────────────┘    ║
 * ║                                                                                    ║
 * ║  ┌────────────────────────────────────────────────────────────────────────────┐    ║
 * ║  │                    DEVICE GROUP ARCHITECTURE:                              │    ║
 * ║  │                                                                            │    ║
 * ║  │    ┌─────────────────────────────────────────────────────────────────┐     │    ║
 * ║  │    │                    DEVICE GROUP                                 │     │    ║
 * ║  │    │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │     │    ║
 * ║  │    │  │   GPU 0     │  │   GPU 1     │  │   GPU 2     │              │     │    ║
 * ║  │    │  │  (Primary)  │  │ (Secondary) │  │ (Secondary) │              │     │    ║
 * ║  │    │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │     │    ║
 * ║  │    │         │                │                │                     │     │    ║
 * ║  │    │         └────────────────┼────────────────┘                     │     │    ║
 * ║  │    │                          ▼                                      │     │    ║
 * ║  │    │              ┌─────────────────────┐                            │     │    ║
 * ║  │    │              │  PEER MEMORY ACCESS │                            │     │    ║
 * ║  │    │              │  (NVLink/Infinity   │                            │     │    ║
 * ║  │    │              │   Fabric/PCIe)      │                            │     │    ║
 * ║  │    │              └─────────────────────┘                            │     │    ║
 * ║  │    └─────────────────────────────────────────────────────────────────┘     │    ║
 * ║  └────────────────────────────────────────────────────────────────────────────┘    ║
 * ║                                                                                    ║
 * ║  THIS PIPELINE INCLUDES:                                                           ║
 * ║  ✓ Full Vulkan 1.0 features (staging, fences, command pools)                       ║
 * ║  ✓ Subgroup query and capability detection                                         ║
 * ║  ✓ Device group enumeration and management                                         ║
 * ║  ✓ Descriptor update template system                                               ║
 * ║  ✓ 16-bit storage buffer support                                                   ║
 * ║  ✓ External memory handle management                                               ║
 * ║  ✓ Multiview buffer allocation                                                     ║
 * ║  ✓ Protected memory path                                                           ║
 * ║  ✓ Peer-to-peer memory transfers                                                   ║
 * ║  ✓ Advanced statistics tracking                                                    ║
 * ║                                                                                    ║
 * ║  PERFORMANCE VS VULKAN 1.0:                                                        ║
 * ║  • 5-15% shader performance (subgroup operations)                                  ║
 * ║  • Near-linear multi-GPU scaling (device groups)                                   ║
 * ║  • 5-10x faster descriptor updates (templates)                                     ║
 * ║  • 50% memory reduction for fp16 data (16-bit storage)                             ║
 * ║  • 40-50% VR improvement (multiview)                                               ║
 * ╚════════════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌───────────────────────────────────────────────────────────────┐
 * │ Snowium Render: Vulkan 1.1 ★ MULTI-GPU & SUBGROUPS ★          │
 * │ Color: #FF5722 (Deep Orange - Evolution)                      │
 * └───────────────────────────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public class VulkanBufferOps11 extends VulkanBufferOps10 {

    // ═══════════════════════════════════════════════════════════════════════════════
    // VERSION CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VERSION_MAJOR_11 = 1;
    public static final int VERSION_MINOR_11 = 1;
    public static final int VERSION_PATCH_11 = 0;
    public static final int VERSION_CODE_11 = VK_MAKE_API_VERSION(0, 1, 1, 0);
    public static final int DISPLAY_COLOR_11 = 0xFF5722; // Deep Orange
    public static final String VERSION_NAME_11 = "Vulkan 1.1";
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VULKAN 1.1 STRUCTURE TYPES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SUBGROUP_PROPERTIES = 1000094000;
    public static final int VK_STRUCTURE_TYPE_BIND_BUFFER_MEMORY_DEVICE_GROUP_INFO = 1000060013;
    public static final int VK_STRUCTURE_TYPE_BIND_IMAGE_MEMORY_DEVICE_GROUP_INFO = 1000060014;
    public static final int VK_STRUCTURE_TYPE_DEVICE_GROUP_RENDER_PASS_BEGIN_INFO = 1000060003;
    public static final int VK_STRUCTURE_TYPE_DEVICE_GROUP_COMMAND_BUFFER_BEGIN_INFO = 1000060004;
    public static final int VK_STRUCTURE_TYPE_DEVICE_GROUP_SUBMIT_INFO = 1000060005;
    public static final int VK_STRUCTURE_TYPE_DEVICE_GROUP_BIND_SPARSE_INFO = 1000060006;
    public static final int VK_STRUCTURE_TYPE_DEVICE_GROUP_DEVICE_CREATE_INFO = 1000070001;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_GROUP_PROPERTIES = 1000070000;
    public static final int VK_STRUCTURE_TYPE_DESCRIPTOR_UPDATE_TEMPLATE_CREATE_INFO = 1000085000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_16BIT_STORAGE_FEATURES = 1000083000;
    public static final int VK_STRUCTURE_TYPE_MEMORY_DEDICATED_REQUIREMENTS = 1000127000;
    public static final int VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO = 1000127001;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTIVIEW_FEATURES = 1000053000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTIVIEW_PROPERTIES = 1000053002;
    public static final int VK_STRUCTURE_TYPE_PROTECTED_SUBMIT_INFO = 1000145000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROTECTED_MEMORY_FEATURES = 1000145001;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROTECTED_MEMORY_PROPERTIES = 1000145002;
    public static final int VK_STRUCTURE_TYPE_DEVICE_QUEUE_INFO_2 = 1000145003;
    public static final int VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_CREATE_INFO = 1000156000;
    public static final int VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_INFO = 1000156001;
    public static final int VK_STRUCTURE_TYPE_BIND_BUFFER_MEMORY_INFO = 1000157000;
    public static final int VK_STRUCTURE_TYPE_BIND_IMAGE_MEMORY_INFO = 1000157001;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTERNAL_BUFFER_INFO = 1000071002;
    public static final int VK_STRUCTURE_TYPE_EXTERNAL_BUFFER_PROPERTIES = 1000071003;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ID_PROPERTIES = 1000071004;
    public static final int VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_BUFFER_CREATE_INFO = 1000072000;
    public static final int VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO = 1000072001;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTERNAL_SEMAPHORE_INFO = 1000076000;
    public static final int VK_STRUCTURE_TYPE_EXTERNAL_SEMAPHORE_PROPERTIES = 1000076001;
    public static final int VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO = 1000077000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTERNAL_FENCE_INFO = 1000112000;
    public static final int VK_STRUCTURE_TYPE_EXTERNAL_FENCE_PROPERTIES = 1000112001;
    public static final int VK_STRUCTURE_TYPE_EXPORT_FENCE_CREATE_INFO = 1000113000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VARIABLE_POINTERS_FEATURES = 1000120000;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SUBGROUP FEATURE FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_SUBGROUP_FEATURE_BASIC_BIT = 0x00000001;
    public static final int VK_SUBGROUP_FEATURE_VOTE_BIT = 0x00000002;
    public static final int VK_SUBGROUP_FEATURE_ARITHMETIC_BIT = 0x00000004;
    public static final int VK_SUBGROUP_FEATURE_BALLOT_BIT = 0x00000008;
    public static final int VK_SUBGROUP_FEATURE_SHUFFLE_BIT = 0x00000010;
    public static final int VK_SUBGROUP_FEATURE_SHUFFLE_RELATIVE_BIT = 0x00000020;
    public static final int VK_SUBGROUP_FEATURE_CLUSTERED_BIT = 0x00000040;
    public static final int VK_SUBGROUP_FEATURE_QUAD_BIT = 0x00000080;
    
    // All subgroup features combined
    public static final int VK_SUBGROUP_FEATURE_ALL_BITS = 
        VK_SUBGROUP_FEATURE_BASIC_BIT | VK_SUBGROUP_FEATURE_VOTE_BIT |
        VK_SUBGROUP_FEATURE_ARITHMETIC_BIT | VK_SUBGROUP_FEATURE_BALLOT_BIT |
        VK_SUBGROUP_FEATURE_SHUFFLE_BIT | VK_SUBGROUP_FEATURE_SHUFFLE_RELATIVE_BIT |
        VK_SUBGROUP_FEATURE_CLUSTERED_BIT | VK_SUBGROUP_FEATURE_QUAD_BIT;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SUBGROUP SHADER STAGES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_SHADER_STAGE_VERTEX_BIT = 0x00000001;
    public static final int VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT = 0x00000002;
    public static final int VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT = 0x00000004;
    public static final int VK_SHADER_STAGE_GEOMETRY_BIT = 0x00000008;
    public static final int VK_SHADER_STAGE_FRAGMENT_BIT = 0x00000010;
    public static final int VK_SHADER_STAGE_COMPUTE_BIT = 0x00000020;
    public static final int VK_SHADER_STAGE_ALL_GRAPHICS = 0x0000001F;
    public static final int VK_SHADER_STAGE_ALL = 0x7FFFFFFF;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DEVICE GROUP FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_MEMORY_ALLOCATE_DEVICE_MASK_BIT = 0x00000001;
    public static final int VK_PEER_MEMORY_FEATURE_COPY_SRC_BIT = 0x00000001;
    public static final int VK_PEER_MEMORY_FEATURE_COPY_DST_BIT = 0x00000002;
    public static final int VK_PEER_MEMORY_FEATURE_GENERIC_SRC_BIT = 0x00000004;
    public static final int VK_PEER_MEMORY_FEATURE_GENERIC_DST_BIT = 0x00000008;
    
    public static final int VK_DEVICE_GROUP_PRESENT_MODE_LOCAL_BIT = 0x00000001;
    public static final int VK_DEVICE_GROUP_PRESENT_MODE_REMOTE_BIT = 0x00000002;
    public static final int VK_DEVICE_GROUP_PRESENT_MODE_SUM_BIT = 0x00000004;
    public static final int VK_DEVICE_GROUP_PRESENT_MODE_LOCAL_MULTI_DEVICE_BIT = 0x00000008;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DESCRIPTOR UPDATE TEMPLATE TYPES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_DESCRIPTOR_UPDATE_TEMPLATE_TYPE_DESCRIPTOR_SET = 0;
    public static final int VK_DESCRIPTOR_UPDATE_TEMPLATE_TYPE_PUSH_DESCRIPTORS = 1;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EXTERNAL MEMORY HANDLE TYPES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT = 0x00000001;
    public static final int VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT = 0x00000002;
    public static final int VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_KMT_BIT = 0x00000004;
    public static final int VK_EXTERNAL_MEMORY_HANDLE_TYPE_D3D11_TEXTURE_BIT = 0x00000008;
    public static final int VK_EXTERNAL_MEMORY_HANDLE_TYPE_D3D11_TEXTURE_KMT_BIT = 0x00000010;
    public static final int VK_EXTERNAL_MEMORY_HANDLE_TYPE_D3D12_HEAP_BIT = 0x00000020;
    public static final int VK_EXTERNAL_MEMORY_HANDLE_TYPE_D3D12_RESOURCE_BIT = 0x00000040;
    public static final int VK_EXTERNAL_MEMORY_HANDLE_TYPE_DMA_BUF_BIT_EXT = 0x00000200;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EXTERNAL MEMORY FEATURE FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_EXTERNAL_MEMORY_FEATURE_DEDICATED_ONLY_BIT = 0x00000001;
    public static final int VK_EXTERNAL_MEMORY_FEATURE_EXPORTABLE_BIT = 0x00000002;
    public static final int VK_EXTERNAL_MEMORY_FEATURE_IMPORTABLE_BIT = 0x00000004;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EXTERNAL SEMAPHORE HANDLE TYPES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_FD_BIT = 0x00000001;
    public static final int VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT = 0x00000002;
    public static final int VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_KMT_BIT = 0x00000004;
    public static final int VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_D3D12_FENCE_BIT = 0x00000008;
    public static final int VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_SYNC_FD_BIT = 0x00000010;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EXTERNAL FENCE HANDLE TYPES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_EXTERNAL_FENCE_HANDLE_TYPE_OPAQUE_FD_BIT = 0x00000001;
    public static final int VK_EXTERNAL_FENCE_HANDLE_TYPE_OPAQUE_WIN32_BIT = 0x00000002;
    public static final int VK_EXTERNAL_FENCE_HANDLE_TYPE_OPAQUE_WIN32_KMT_BIT = 0x00000004;
    public static final int VK_EXTERNAL_FENCE_HANDLE_TYPE_SYNC_FD_BIT = 0x00000008;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PROTECTED MEMORY FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_QUEUE_PROTECTED_BIT = 0x00000010;
    public static final int VK_DEVICE_QUEUE_CREATE_PROTECTED_BIT = 0x00000001;
    public static final int VK_MEMORY_PROPERTY_PROTECTED_BIT = 0x00000020;
    public static final int VK_BUFFER_CREATE_PROTECTED_BIT = 0x00000008;
    public static final int VK_COMMAND_POOL_CREATE_PROTECTED_BIT = 0x00000004;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DESCRIPTOR TYPES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_DESCRIPTOR_TYPE_SAMPLER = 0;
    public static final int VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER = 1;
    public static final int VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE = 2;
    public static final int VK_DESCRIPTOR_TYPE_STORAGE_IMAGE = 3;
    public static final int VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER = 4;
    public static final int VK_DESCRIPTOR_TYPE_STORAGE_TEXEL_BUFFER = 5;
    public static final int VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER = 6;
    public static final int VK_DESCRIPTOR_TYPE_STORAGE_BUFFER = 7;
    public static final int VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC = 8;
    public static final int VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC = 9;
    public static final int VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT = 10;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // POINT CLIPPING BEHAVIOR
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_POINT_CLIPPING_BEHAVIOR_ALL_CLIP_PLANES = 0;
    public static final int VK_POINT_CLIPPING_BEHAVIOR_USER_CLIP_PLANES_ONLY = 1;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONFIGURATION CONSTANTS (Vulkan 1.1 Specific)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    protected static final int MAX_DEVICE_GROUP_SIZE = 8;
    protected static final int MAX_DESCRIPTOR_UPDATE_TEMPLATES = 64;
    protected static final int DESCRIPTOR_TEMPLATE_POOL_SIZE = 16;
    protected static final int EXTERNAL_MEMORY_HANDLE_CACHE_SIZE = 32;
    protected static final int MULTIVIEW_MAX_VIEWS = 6;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SUBGROUP PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Subgroup properties cached from physical device query.
     */
    public static final class SubgroupProperties {
        /** Number of invocations in a subgroup (typically 32 NVIDIA, 64 AMD) */
        public int subgroupSize = 0;
        
        /** Supported subgroup operations (VK_SUBGROUP_FEATURE_* bits) */
        public int supportedOperations = 0;
        
        /** Shader stages supporting subgroup operations */
        public int supportedStages = 0;
        
        /** Whether subgroup operations are quad-aligned */
        public boolean quadOperationsInAllStages = false;
        
        // Convenience methods
        public boolean supportsBasic() { return (supportedOperations & VK_SUBGROUP_FEATURE_BASIC_BIT) != 0; }
        public boolean supportsVote() { return (supportedOperations & VK_SUBGROUP_FEATURE_VOTE_BIT) != 0; }
        public boolean supportsArithmetic() { return (supportedOperations & VK_SUBGROUP_FEATURE_ARITHMETIC_BIT) != 0; }
        public boolean supportsBallot() { return (supportedOperations & VK_SUBGROUP_FEATURE_BALLOT_BIT) != 0; }
        public boolean supportsShuffle() { return (supportedOperations & VK_SUBGROUP_FEATURE_SHUFFLE_BIT) != 0; }
        public boolean supportsShuffleRelative() { return (supportedOperations & VK_SUBGROUP_FEATURE_SHUFFLE_RELATIVE_BIT) != 0; }
        public boolean supportsClustered() { return (supportedOperations & VK_SUBGROUP_FEATURE_CLUSTERED_BIT) != 0; }
        public boolean supportsQuad() { return (supportedOperations & VK_SUBGROUP_FEATURE_QUAD_BIT) != 0; }
        
        public boolean supportsInVertex() { return (supportedStages & VK_SHADER_STAGE_VERTEX_BIT) != 0; }
        public boolean supportsInFragment() { return (supportedStages & VK_SHADER_STAGE_FRAGMENT_BIT) != 0; }
        public boolean supportsInCompute() { return (supportedStages & VK_SHADER_STAGE_COMPUTE_BIT) != 0; }
        
        public String getSupportedOperationsString() {
            StringBuilder sb = new StringBuilder();
            if (supportsBasic()) sb.append("BASIC ");
            if (supportsVote()) sb.append("VOTE ");
            if (supportsArithmetic()) sb.append("ARITHMETIC ");
            if (supportsBallot()) sb.append("BALLOT ");
            if (supportsShuffle()) sb.append("SHUFFLE ");
            if (supportsShuffleRelative()) sb.append("SHUFFLE_RELATIVE ");
            if (supportsClustered()) sb.append("CLUSTERED ");
            if (supportsQuad()) sb.append("QUAD ");
            return sb.toString().trim();
        }
        
        @Override
        public String toString() {
            return String.format("Subgroup[size=%d, ops=%s, stages=0x%X, quad=%b]",
                subgroupSize, getSupportedOperationsString(), supportedStages, quadOperationsInAllStages);
        }
    }
    
    protected final SubgroupProperties subgroupProperties = new SubgroupProperties();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DEVICE GROUP SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Represents a physical device group for multi-GPU rendering.
     */
    public static final class DeviceGroup {
        /** Physical devices in the group */
        public final long[] physicalDevices;
        
        /** Number of devices in the group */
        public final int deviceCount;
        
        /** Whether the group can do subset allocations */
        public final boolean subsetAllocation;
        
        /** Device masks for each device */
        public final int[] deviceMasks;
        
        /** Peer memory features between device pairs [src][dst] */
        public final int[][] peerMemoryFeatures;
        
        /** Device properties for each device */
        public final DeviceProperties[] deviceProperties;
        
        /** Currently active device index for rendering */
        private volatile int activeDeviceIndex = 0;
        
        public DeviceGroup(long[] devices, boolean subsetAlloc) {
            this.physicalDevices = devices.clone();
            this.deviceCount = devices.length;
            this.subsetAllocation = subsetAlloc;
            this.deviceMasks = new int[deviceCount];
            this.peerMemoryFeatures = new int[deviceCount][deviceCount];
            this.deviceProperties = new DeviceProperties[deviceCount];
            
            for (int i = 0; i < deviceCount; i++) {
                deviceMasks[i] = 1 << i;
            }
        }
        
        public long getPrimaryDevice() { return physicalDevices[0]; }
        public long getDevice(int index) { return physicalDevices[index]; }
        public int getActiveDeviceIndex() { return activeDeviceIndex; }
        public void setActiveDeviceIndex(int index) { activeDeviceIndex = index; }
        public int getAllDevicesMask() { return (1 << deviceCount) - 1; }
        
        /** Check if device pair supports direct peer memory access */
        public boolean supportsPeerMemory(int srcDevice, int dstDevice) {
            return peerMemoryFeatures[srcDevice][dstDevice] != 0;
        }
        
        /** Check if peer copy is supported */
        public boolean supportsPeerCopy(int srcDevice, int dstDevice) {
            int features = peerMemoryFeatures[srcDevice][dstDevice];
            return (features & VK_PEER_MEMORY_FEATURE_COPY_SRC_BIT) != 0 &&
                   (features & VK_PEER_MEMORY_FEATURE_COPY_DST_BIT) != 0;
        }
        
        /** Get optimal device for workload */
        public int selectOptimalDevice(WorkloadType type) {
            if (deviceCount == 1) return 0;
            
            // Simple round-robin for now, could be enhanced with load balancing
            switch (type) {
                case RENDER:
                    return activeDeviceIndex;
                case COMPUTE:
                    // Prefer secondary device for compute
                    return deviceCount > 1 ? 1 : 0;
                case TRANSFER:
                    // Prefer device with best transfer capability
                    return 0;
                default:
                    return 0;
            }
        }
    }
    
    public enum WorkloadType {
        RENDER,
        COMPUTE,
        TRANSFER
    }
    
    /**
     * Basic device properties.
     */
    public static final class DeviceProperties {
        public int vendorID;
        public int deviceID;
        public int deviceType;
        public String deviceName;
        public long localMemorySize;
        public int maxComputeWorkGroupSize;
        public int maxComputeSharedMemorySize;
    }
    
    protected DeviceGroup deviceGroup;
    protected boolean multiGpuEnabled = false;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DESCRIPTOR UPDATE TEMPLATE SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Descriptor update template entry.
     */
    public static final class DescriptorTemplateEntry {
        public int dstBinding;
        public int dstArrayElement;
        public int descriptorCount;
        public int descriptorType;
        public long offset;
        public long stride;
        
        public DescriptorTemplateEntry(int binding, int type, int count, long offset, long stride) {
            this.dstBinding = binding;
            this.dstArrayElement = 0;
            this.descriptorCount = count;
            this.descriptorType = type;
            this.offset = offset;
            this.stride = stride;
        }
    }
    
    /**
     * Cached descriptor update template.
     */
    public static final class DescriptorUpdateTemplate {
        public final long handle;
        public final long descriptorSetLayout;
        public final long pipelineLayout;
        public final int templateType;
        public final int pipelineBindPoint;
        public final int set;
        public final DescriptorTemplateEntry[] entries;
        public final int totalDescriptorCount;
        public final long dataSize;
        
        // Pre-allocated update data buffer
        private final ByteBuffer updateBuffer;
        private volatile boolean dirty = false;
        
        public DescriptorUpdateTemplate(long handle, long dsLayout, long pipeLayout, 
                                        int type, int bindPoint, int set,
                                        DescriptorTemplateEntry[] entries) {
            this.handle = handle;
            this.descriptorSetLayout = dsLayout;
            this.pipelineLayout = pipeLayout;
            this.templateType = type;
            this.pipelineBindPoint = bindPoint;
            this.set = set;
            this.entries = entries.clone();
            
            int count = 0;
            long size = 0;
            for (DescriptorTemplateEntry entry : entries) {
                count += entry.descriptorCount;
                size = Math.max(size, entry.offset + entry.stride * entry.descriptorCount);
            }
            this.totalDescriptorCount = count;
            this.dataSize = size;
            this.updateBuffer = ByteBuffer.allocateDirect((int) size).order(ByteOrder.nativeOrder());
        }
        
        public ByteBuffer getUpdateBuffer() {
            updateBuffer.clear();
            return updateBuffer;
        }
        
        public void markDirty() { dirty = true; }
        public boolean isDirty() { return dirty; }
        public void clearDirty() { dirty = false; }
    }
    
    protected final ConcurrentHashMap<Long, DescriptorUpdateTemplate> descriptorTemplates = 
            new ConcurrentHashMap<>();
    protected final ConcurrentLinkedQueue<Long> templatePool = new ConcurrentLinkedQueue<>();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // 16-BIT STORAGE FEATURES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * 16-bit storage capabilities.
     */
    public static final class Storage16BitFeatures {
        /** 16-bit storage in storage buffers */
        public boolean storageBuffer16BitAccess = false;
        
        /** 16-bit storage in uniform buffers */
        public boolean uniformAndStorageBuffer16BitAccess = false;
        
        /** 16-bit storage in push constants */
        public boolean storagePushConstant16 = false;
        
        /** 16-bit storage for input/output variables */
        public boolean storageInputOutput16 = false;
        
        public boolean supportsAny() {
            return storageBuffer16BitAccess || uniformAndStorageBuffer16BitAccess ||
                   storagePushConstant16 || storageInputOutput16;
        }
        
        @Override
        public String toString() {
            return String.format("16BitStorage[SSBO=%b, UBO=%b, Push=%b, IO=%b]",
                storageBuffer16BitAccess, uniformAndStorageBuffer16BitAccess,
                storagePushConstant16, storageInputOutput16);
        }
    }
    
    protected final Storage16BitFeatures storage16BitFeatures = new Storage16BitFeatures();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EXTERNAL MEMORY SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * External memory handle for cross-API/process sharing.
     */
    public static final class ExternalMemoryHandle {
        public final long vkBuffer;
        public final long vkMemory;
        public final long size;
        public final int handleType;
        public final long externalHandle; // OS-specific handle (fd on Linux, HANDLE on Windows)
        public final boolean dedicated;
        
        public ExternalMemoryHandle(long buffer, long memory, long size, int handleType, 
                                    long externalHandle, boolean dedicated) {
            this.vkBuffer = buffer;
            this.vkMemory = memory;
            this.size = size;
            this.handleType = handleType;
            this.externalHandle = externalHandle;
            this.dedicated = dedicated;
        }
        
        public boolean isExportable() {
            return externalHandle != 0 && externalHandle != -1;
        }
    }
    
    protected final ConcurrentHashMap<Long, ExternalMemoryHandle> externalHandles = 
            new ConcurrentHashMap<>();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // MULTIVIEW SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Multiview capabilities and configuration.
     */
    public static final class MultiviewCapabilities {
        /** Maximum number of views for multiview rendering */
        public int maxMultiviewViewCount = 0;
        
        /** Maximum number of multiview instances */
        public int maxMultiviewInstanceIndex = 0;
        
        /** Whether multiview is supported */
        public boolean multiview = false;
        
        /** Whether geometry and tessellation shaders can use multiview */
        public boolean multiviewGeometryShader = false;
        public boolean multiviewTessellationShader = false;
        
        public boolean supportsVR() {
            return multiview && maxMultiviewViewCount >= 2;
        }
        
        @Override
        public String toString() {
            return String.format("Multiview[enabled=%b, maxViews=%d, geom=%b, tess=%b]",
                multiview, maxMultiviewViewCount, multiviewGeometryShader, multiviewTessellationShader);
        }
    }
    
    protected final MultiviewCapabilities multiviewCapabilities = new MultiviewCapabilities();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PROTECTED MEMORY SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Protected memory capabilities.
     */
    public static final class ProtectedMemoryCapabilities {
        /** Whether protected memory is supported */
        public boolean protectedMemory = false;
        
        /** Whether protected no-fault is supported */
        public boolean protectedNoFault = false;
        
        /** Protected queue index (if available) */
        public int protectedQueueIndex = -1;
        
        public boolean isAvailable() { return protectedMemory; }
    }
    
    protected final ProtectedMemoryCapabilities protectedMemoryCapabilities = 
            new ProtectedMemoryCapabilities();
    protected long protectedCommandPool = VK_NULL_HANDLE;
    protected long protectedQueue = VK_NULL_HANDLE;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DEVICE GROUP BUFFER (Multi-GPU aware)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Buffer that can be accessed across multiple GPUs in a device group.
     */
    public static class DeviceGroupBuffer extends VulkanBuffer {
        /** Device mask indicating which devices have allocations */
        public final int deviceMask;
        
        /** Per-device memory allocations */
        public final long[] deviceMemory;
        
        /** Per-device mapped regions (if host-visible) */
        public final ByteBuffer[] deviceMappings;
        
        public DeviceGroupBuffer(long handle, long memory, long size, int usage, int memProps,
                                 int deviceMask, long[] deviceMemory) {
            super(handle, memory, size, usage, memProps);
            this.deviceMask = deviceMask;
            this.deviceMemory = deviceMemory.clone();
            this.deviceMappings = new ByteBuffer[deviceMemory.length];
        }
        
        public boolean isOnDevice(int deviceIndex) {
            return (deviceMask & (1 << deviceIndex)) != 0;
        }
        
        public long getMemoryForDevice(int deviceIndex) {
            return deviceMemory[deviceIndex];
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // HALF-PRECISION BUFFER (16-bit storage)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Buffer optimized for half-precision (16-bit) storage.
     */
    public static class HalfPrecisionBuffer extends VulkanBuffer {
        /** Whether this buffer uses 16-bit storage */
        public final boolean uses16BitStorage;
        
        /** Number of half-precision elements */
        public final long elementCount;
        
        public HalfPrecisionBuffer(long handle, long memory, long size, int usage, int memProps,
                                   long elementCount) {
            super(handle, memory, size, usage, memProps);
            this.uses16BitStorage = true;
            this.elementCount = elementCount;
        }
        
        /** Write float array as half-precision */
        public void writeHalfPrecision(float[] data, long offset) {
            if (persistentMap == null) {
                throw new IllegalStateException("Buffer not persistently mapped");
            }
            
            ShortBuffer shorts = persistentMap.asShortBuffer();
            shorts.position((int) (offset / 2));
            
            for (float f : data) {
                shorts.put(floatToHalf(f));
            }
        }
        
        /** Read half-precision as float array */
        public void readHalfPrecision(float[] data, long offset) {
            if (persistentMap == null) {
                throw new IllegalStateException("Buffer not persistently mapped");
            }
            
            ShortBuffer shorts = persistentMap.asShortBuffer();
            shorts.position((int) (offset / 2));
            
            for (int i = 0; i < data.length; i++) {
                data[i] = halfToFloat(shorts.get());
            }
        }
        
        // IEEE 754 half-precision conversion
        private static short floatToHalf(float f) {
            int bits = Float.floatToIntBits(f);
            int sign = (bits >> 16) & 0x8000;
            int exp = ((bits >> 23) & 0xFF) - 127 + 15;
            int mantissa = bits & 0x007FFFFF;
            
            if (exp <= 0) {
                if (exp < -10) return (short) sign;
                mantissa = (mantissa | 0x00800000) >> (1 - exp);
                return (short) (sign | (mantissa >> 13));
            } else if (exp >= 31) {
                return (short) (sign | 0x7C00 | ((mantissa != 0) ? (mantissa >> 13) : 0));
            }
            
            return (short) (sign | (exp << 10) | (mantissa >> 13));
        }
        
        private static float halfToFloat(short h) {
            int sign = (h & 0x8000) << 16;
            int exp = (h >> 10) & 0x1F;
            int mantissa = h & 0x03FF;
            
            if (exp == 0) {
                if (mantissa == 0) return Float.intBitsToFloat(sign);
                while ((mantissa & 0x0400) == 0) {
                    mantissa <<= 1;
                    exp--;
                }
                exp++;
                mantissa &= ~0x0400;
            } else if (exp == 31) {
                return Float.intBitsToFloat(sign | 0x7F800000 | (mantissa << 13));
            }
            
            exp = exp + 127 - 15;
            mantissa = mantissa << 13;
            
            return Float.intBitsToFloat(sign | (exp << 23) | mantissa);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATISTICS (Vulkan 1.1 Specific)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    protected static final AtomicLong STAT_TEMPLATE_UPDATES = new AtomicLong();
    protected static final AtomicLong STAT_TEMPLATE_CREATIONS = new AtomicLong();
    protected static final AtomicLong STAT_MULTI_GPU_TRANSFERS = new AtomicLong();
    protected static final AtomicLong STAT_EXTERNAL_EXPORTS = new AtomicLong();
    protected static final AtomicLong STAT_EXTERNAL_IMPORTS = new AtomicLong();
    protected static final AtomicLong STAT_16BIT_BUFFERS = new AtomicLong();
    protected static final AtomicLong STAT_MULTIVIEW_ALLOCATIONS = new AtomicLong();
    protected static final AtomicLong STAT_PROTECTED_ALLOCATIONS = new AtomicLong();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public VulkanBufferOps11() {
        super();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION (Override to add 1.1 features)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Override
    public boolean initialize(boolean enableValidation) {
        // First, initialize Vulkan 1.0 base
        if (!super.initialize(enableValidation)) {
            return false;
        }
        
        try {
            // Query Vulkan 1.1 features
            querySubgroupProperties();
            query16BitStorageFeatures();
            queryMultiviewCapabilities();
            queryProtectedMemoryCapabilities();
            
            // Enumerate device groups for multi-GPU
            enumerateDeviceGroups();
            
            // Initialize protected queue if available
            if (protectedMemoryCapabilities.isAvailable()) {
                initializeProtectedQueue();
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to initialize Vulkan 1.1 features: " + e.getMessage());
            // Continue anyway - 1.0 features are still available
            return true;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SUBGROUP OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Query subgroup properties from the physical device.
     */
    protected void querySubgroupProperties() {
        /*
         * VkPhysicalDeviceSubgroupProperties subgroupProps = VkPhysicalDeviceSubgroupProperties.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SUBGROUP_PROPERTIES);
         * 
         * VkPhysicalDeviceProperties2 props2 = VkPhysicalDeviceProperties2.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2)
         *     .pNext(subgroupProps);
         * 
         * vkGetPhysicalDeviceProperties2(vkPhysicalDevice, props2);
         * 
         * subgroupProperties.subgroupSize = subgroupProps.subgroupSize();
         * subgroupProperties.supportedOperations = subgroupProps.supportedOperations();
         * subgroupProperties.supportedStages = subgroupProps.supportedStages();
         * subgroupProperties.quadOperationsInAllStages = subgroupProps.quadOperationsInAllStages();
         */
        
        int[] props = VulkanCallMapper.vkGetSubgroupProperties(vkPhysicalDevice);
        if (props != null && props.length >= 4) {
            subgroupProperties.subgroupSize = props[0];
            subgroupProperties.supportedOperations = props[1];
            subgroupProperties.supportedStages = props[2];
            subgroupProperties.quadOperationsInAllStages = props[3] != 0;
        }
        
        System.out.println("[Vulkan 1.1] " + subgroupProperties);
    }
    
    /**
     * Get the subgroup size for optimal compute dispatch.
     */
    public int getSubgroupSize() {
        return subgroupProperties.subgroupSize;
    }
    
    /**
     * Calculate optimal workgroup size based on subgroup size.
     */
    public int calculateOptimalWorkgroupSize(int minSize) {
        int subgroupSize = subgroupProperties.subgroupSize;
        if (subgroupSize == 0) return minSize;
        
        // Round up to next multiple of subgroup size
        return ((minSize + subgroupSize - 1) / subgroupSize) * subgroupSize;
    }
    
    /**
     * Check if a specific subgroup feature is supported.
     */
    public boolean supportsSubgroupFeature(int feature) {
        return (subgroupProperties.supportedOperations & feature) != 0;
    }
    
    /**
     * Get subgroup properties for shader compilation.
     */
    public SubgroupProperties getSubgroupProperties() {
        return subgroupProperties;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DEVICE GROUPS (Multi-GPU)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Enumerate and initialize device groups for multi-GPU rendering.
     */
    protected void enumerateDeviceGroups() {
        /*
         * IntBuffer groupCount = stack.ints(0);
         * vkEnumeratePhysicalDeviceGroups(vkInstance, groupCount, null);
         * 
         * if (groupCount.get(0) == 0) return;
         * 
         * VkPhysicalDeviceGroupProperties.Buffer groups = 
         *     VkPhysicalDeviceGroupProperties.calloc(groupCount.get(0));
         * 
         * for (int i = 0; i < groupCount.get(0); i++) {
         *     groups.get(i).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_GROUP_PROPERTIES);
         * }
         * 
         * vkEnumeratePhysicalDeviceGroups(vkInstance, groupCount, groups);
         * 
         * // Find best group (one with our selected physical device)
         * for (int i = 0; i < groupCount.get(0); i++) {
         *     VkPhysicalDeviceGroupProperties group = groups.get(i);
         *     for (int j = 0; j < group.physicalDeviceCount(); j++) {
         *         if (group.physicalDevices(j) == vkPhysicalDevice) {
         *             // Found our device group
         *             long[] devices = new long[group.physicalDeviceCount()];
         *             for (int k = 0; k < group.physicalDeviceCount(); k++) {
         *                 devices[k] = group.physicalDevices(k);
         *             }
         *             deviceGroup = new DeviceGroup(devices, group.subsetAllocation());
         *             multiGpuEnabled = deviceGroup.deviceCount > 1;
         *             break;
         *         }
         *     }
         * }
         */
        
        long[][] groups = VulkanCallMapper.vkEnumeratePhysicalDeviceGroups(vkInstance);
        if (groups == null || groups.length == 0) {
            // Single device, create simple group
            deviceGroup = new DeviceGroup(new long[] { vkPhysicalDevice }, false);
            return;
        }
        
        // Find group containing our device
        for (long[] group : groups) {
            for (long device : group) {
                if (device == vkPhysicalDevice) {
                    deviceGroup = new DeviceGroup(group, group.length > 1);
                    multiGpuEnabled = group.length > 1;
                    
                    // Query peer memory features
                    queryPeerMemoryFeatures();
                    
                    // Query device properties for each device
                    queryDeviceGroupProperties();
                    
                    System.out.println("[Vulkan 1.1] Device group: " + deviceGroup.deviceCount + " GPUs, multi-GPU=" + multiGpuEnabled);
                    return;
                }
            }
        }
        
        // Fallback
        deviceGroup = new DeviceGroup(new long[] { vkPhysicalDevice }, false);
    }
    
    /**
     * Query peer memory features between devices in the group.
     */
    protected void queryPeerMemoryFeatures() {
        if (deviceGroup == null || deviceGroup.deviceCount < 2) return;
        
        /*
         * for (int src = 0; src < deviceGroup.deviceCount; src++) {
         *     for (int dst = 0; dst < deviceGroup.deviceCount; dst++) {
         *         if (src == dst) continue;
         *         
         *         IntBuffer features = stack.ints(0);
         *         vkGetDeviceGroupPeerMemoryFeatures(vkDevice, 0, src, dst, features);
         *         deviceGroup.peerMemoryFeatures[src][dst] = features.get(0);
         *     }
         * }
         */
        
        for (int src = 0; src < deviceGroup.deviceCount; src++) {
            for (int dst = 0; dst < deviceGroup.deviceCount; dst++) {
                if (src == dst) {
                    deviceGroup.peerMemoryFeatures[src][dst] = 
                        VK_PEER_MEMORY_FEATURE_COPY_SRC_BIT | 
                        VK_PEER_MEMORY_FEATURE_COPY_DST_BIT |
                        VK_PEER_MEMORY_FEATURE_GENERIC_SRC_BIT |
                        VK_PEER_MEMORY_FEATURE_GENERIC_DST_BIT;
                } else {
                    deviceGroup.peerMemoryFeatures[src][dst] = 
                        VulkanCallMapper.vkGetDeviceGroupPeerMemoryFeatures(vkDevice, 0, src, dst);
                }
            }
        }
    }
    
    /**
     * Query properties for each device in the group.
     */
    protected void queryDeviceGroupProperties() {
        if (deviceGroup == null) return;
        
        for (int i = 0; i < deviceGroup.deviceCount; i++) {
            DeviceProperties props = new DeviceProperties();
            VulkanCallMapper.vkGetPhysicalDeviceProperties(deviceGroup.physicalDevices[i], props);
            deviceGroup.deviceProperties[i] = props;
        }
    }
    
    /**
     * Create a buffer accessible across all devices in the group.
     */
    public int createDeviceGroupBuffer(long size, int usage, int memoryProperties) {
        if (!multiGpuEnabled || deviceGroup == null) {
            // Fall back to single-device buffer
            return createBuffer(size, usage, memoryProperties);
        }
        
        /*
         * VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
         *     .size(size)
         *     .usage(usage)
         *     .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
         * 
         * LongBuffer pBuffer = stack.mallocLong(1);
         * vkCreateBuffer(vkDevice, bufferInfo, null, pBuffer);
         * long bufferHandle = pBuffer.get(0);
         * 
         * VkMemoryRequirements memReqs = VkMemoryRequirements.malloc();
         * vkGetBufferMemoryRequirements(vkDevice, bufferHandle, memReqs);
         * 
         * int memTypeIndex = findMemoryType(memoryProperties);
         * int allDevicesMask = deviceGroup.getAllDevicesMask();
         * 
         * VkMemoryAllocateFlagsInfo flagsInfo = VkMemoryAllocateFlagsInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_FLAGS_INFO)
         *     .flags(VK_MEMORY_ALLOCATE_DEVICE_MASK_BIT)
         *     .deviceMask(allDevicesMask);
         * 
         * VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
         *     .pNext(flagsInfo)
         *     .allocationSize(memReqs.size())
         *     .memoryTypeIndex(memTypeIndex);
         * 
         * LongBuffer pMemory = stack.mallocLong(1);
         * vkAllocateMemory(vkDevice, allocInfo, null, pMemory);
         * long memoryHandle = pMemory.get(0);
         * 
         * // Bind with device group info
         * VkBindBufferMemoryDeviceGroupInfo deviceGroupBindInfo = 
         *     VkBindBufferMemoryDeviceGroupInfo.calloc()
         *         .sType(VK_STRUCTURE_TYPE_BIND_BUFFER_MEMORY_DEVICE_GROUP_INFO)
         *         .deviceIndexCount(deviceGroup.deviceCount)
         *         .pDeviceIndices(deviceIndices);
         * 
         * VkBindBufferMemoryInfo bindInfo = VkBindBufferMemoryInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_BIND_BUFFER_MEMORY_INFO)
         *     .pNext(deviceGroupBindInfo)
         *     .buffer(bufferHandle)
         *     .memory(memoryHandle)
         *     .memoryOffset(0);
         * 
         * vkBindBufferMemory2(vkDevice, bindInfo);
         */
        
        long bufferHandle = VulkanCallMapper.vkCreateBuffer(vkDevice, size, usage);
        if (bufferHandle == VK_NULL_HANDLE) return 0;
        
        long memReqSize = VulkanCallMapper.vkGetBufferMemoryRequirements(vkDevice, bufferHandle);
        int memTypeIndex = findMemoryType(memoryProperties);
        int allDevicesMask = deviceGroup.getAllDevicesMask();
        
        // Allocate with device mask
        long memoryHandle = VulkanCallMapper.vkAllocateMemoryWithDeviceMask(
            vkDevice, memReqSize, memTypeIndex, allDevicesMask);
        
        if (memoryHandle == VK_NULL_HANDLE) {
            VulkanCallMapper.vkDestroyBuffer(vkDevice, bufferHandle);
            return 0;
        }
        
        // Bind memory with device group info
        VulkanCallMapper.vkBindBufferMemory2WithDeviceGroup(
            vkDevice, bufferHandle, memoryHandle, 0, deviceGroup.deviceCount);
        
        // Create per-device memory array
        long[] deviceMemory = new long[deviceGroup.deviceCount];
        Arrays.fill(deviceMemory, memoryHandle);
        
        DeviceGroupBuffer buffer = new DeviceGroupBuffer(
            bufferHandle, memoryHandle, size, usage, memoryProperties,
            allDevicesMask, deviceMemory);
        
        int id = bufferIdGenerator.getAndIncrement();
        managedBuffers.put((long) id, buffer);
        STAT_BUFFERS_CREATED.incrementAndGet();
        STAT_BYTES_ALLOCATED.addAndGet(size * deviceGroup.deviceCount);
        STAT_MULTI_GPU_TRANSFERS.incrementAndGet();
        
        return id;
    }
    
    /**
     * Transfer data between devices in a device group.
     */
    public void transferBetweenDevices(int bufferId, int srcDevice, int dstDevice) {
        if (!multiGpuEnabled) return;
        
        VulkanBuffer buffer = managedBuffers.get((long) bufferId);
        if (buffer == null) return;
        
        if (!(buffer instanceof DeviceGroupBuffer)) {
            throw new IllegalArgumentException("Buffer is not a device group buffer");
        }
        
        DeviceGroupBuffer dgBuffer = (DeviceGroupBuffer) buffer;
        
        if (!deviceGroup.supportsPeerCopy(srcDevice, dstDevice)) {
            throw new UnsupportedOperationException(
                "Peer copy not supported between device " + srcDevice + " and " + dstDevice);
        }
        
        /*
         * VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
         * 
         * VkDeviceGroupCommandBufferBeginInfo deviceGroupBeginInfo = 
         *     VkDeviceGroupCommandBufferBeginInfo.calloc()
         *         .sType(VK_STRUCTURE_TYPE_DEVICE_GROUP_COMMAND_BUFFER_BEGIN_INFO)
         *         .deviceMask((1 << srcDevice) | (1 << dstDevice));
         * 
         * beginInfo.pNext(deviceGroupBeginInfo);
         * 
         * long cmdBuffer = acquireTransferCommandBuffer();
         * vkBeginCommandBuffer(cmdBuffer, beginInfo);
         * 
         * // Copy with device group barrier
         * VkBufferCopy copyRegion = VkBufferCopy.calloc()
         *     .srcOffset(0)
         *     .dstOffset(0)
         *     .size(buffer.size);
         * 
         * vkCmdCopyBuffer(cmdBuffer, dgBuffer.handle, dgBuffer.handle, copyRegion);
         * vkEndCommandBuffer(cmdBuffer);
         * 
         * // Submit with device group info
         * VkDeviceGroupSubmitInfo submitDeviceGroupInfo = VkDeviceGroupSubmitInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_DEVICE_GROUP_SUBMIT_INFO)
         *     .commandBufferCount(1)
         *     .pCommandBufferDeviceMasks(stack.ints((1 << srcDevice) | (1 << dstDevice)));
         * 
         * VkSubmitInfo submitInfo = VkSubmitInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
         *     .pNext(submitDeviceGroupInfo)
         *     .commandBufferCount(1)
         *     .pCommandBuffers(cmdBuffer);
         * 
         * long fence = acquireFence();
         * vkQueueSubmit(vkTransferQueue, submitInfo, fence);
         * waitForFence(fence, DEFAULT_FENCE_TIMEOUT);
         * releaseFence(fence);
         */
        
        long cmdBuffer = acquireTransferCommandBuffer();
        int deviceMask = (1 << srcDevice) | (1 << dstDevice);
        
        VulkanCallMapper.vkBeginCommandBufferWithDeviceGroup(cmdBuffer, deviceMask);
        VulkanCallMapper.vkCmdCopyBuffer(cmdBuffer, dgBuffer.handle, dgBuffer.handle, 0, 0, dgBuffer.size);
        VulkanCallMapper.vkEndCommandBuffer(cmdBuffer);
        
        long fence = acquireFence();
        VulkanCallMapper.vkQueueSubmitWithDeviceGroup(vkTransferQueue, cmdBuffer, fence, deviceMask);
        waitForFence(fence, DEFAULT_FENCE_TIMEOUT);
        releaseFence(fence);
        
        STAT_MULTI_GPU_TRANSFERS.incrementAndGet();
    }
    
    /**
     * Get device group information.
     */
    public DeviceGroup getDeviceGroup() {
        return deviceGroup;
    }
    
    /**
     * Check if multi-GPU is enabled.
     */
    public boolean isMultiGpuEnabled() {
        return multiGpuEnabled;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DESCRIPTOR UPDATE TEMPLATES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a descriptor update template for fast binding updates.
     * Templates are 5-10x faster than regular descriptor updates.
     */
    public long createDescriptorUpdateTemplate(long descriptorSetLayout, 
                                                DescriptorTemplateEntry[] entries) {
        /*
         * VkDescriptorUpdateTemplateEntry.Buffer templateEntries = 
         *     VkDescriptorUpdateTemplateEntry.calloc(entries.length);
         * 
         * for (int i = 0; i < entries.length; i++) {
         *     DescriptorTemplateEntry entry = entries[i];
         *     templateEntries.get(i)
         *         .dstBinding(entry.dstBinding)
         *         .dstArrayElement(entry.dstArrayElement)
         *         .descriptorCount(entry.descriptorCount)
         *         .descriptorType(entry.descriptorType)
         *         .offset(entry.offset)
         *         .stride(entry.stride);
         * }
         * 
         * VkDescriptorUpdateTemplateCreateInfo createInfo = 
         *     VkDescriptorUpdateTemplateCreateInfo.calloc()
         *         .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_UPDATE_TEMPLATE_CREATE_INFO)
         *         .descriptorUpdateEntryCount(entries.length)
         *         .pDescriptorUpdateEntries(templateEntries)
         *         .templateType(VK_DESCRIPTOR_UPDATE_TEMPLATE_TYPE_DESCRIPTOR_SET)
         *         .descriptorSetLayout(descriptorSetLayout);
         * 
         * LongBuffer pTemplate = stack.mallocLong(1);
         * vkCreateDescriptorUpdateTemplate(vkDevice, createInfo, null, pTemplate);
         * return pTemplate.get(0);
         */
        
        long templateHandle = VulkanCallMapper.vkCreateDescriptorUpdateTemplate(
            vkDevice, descriptorSetLayout, entries);
        
        if (templateHandle != VK_NULL_HANDLE) {
            DescriptorUpdateTemplate template = new DescriptorUpdateTemplate(
                templateHandle, descriptorSetLayout, VK_NULL_HANDLE,
                VK_DESCRIPTOR_UPDATE_TEMPLATE_TYPE_DESCRIPTOR_SET, 0, 0, entries);
            
            descriptorTemplates.put(templateHandle, template);
            STAT_TEMPLATE_CREATIONS.incrementAndGet();
        }
        
        return templateHandle;
    }
    
    /**
     * Create a push descriptor update template for even faster updates.
     */
    public long createPushDescriptorUpdateTemplate(long pipelineLayout, int set,
                                                    DescriptorTemplateEntry[] entries) {
        /*
         * VkDescriptorUpdateTemplateCreateInfo createInfo = 
         *     VkDescriptorUpdateTemplateCreateInfo.calloc()
         *         .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_UPDATE_TEMPLATE_CREATE_INFO)
         *         .descriptorUpdateEntryCount(entries.length)
         *         .pDescriptorUpdateEntries(templateEntries)
         *         .templateType(VK_DESCRIPTOR_UPDATE_TEMPLATE_TYPE_PUSH_DESCRIPTORS)
         *         .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
         *         .pipelineLayout(pipelineLayout)
         *         .set(set);
         */
        
        long templateHandle = VulkanCallMapper.vkCreatePushDescriptorUpdateTemplate(
            vkDevice, pipelineLayout, set, entries);
        
        if (templateHandle != VK_NULL_HANDLE) {
            DescriptorUpdateTemplate template = new DescriptorUpdateTemplate(
                templateHandle, VK_NULL_HANDLE, pipelineLayout,
                VK_DESCRIPTOR_UPDATE_TEMPLATE_TYPE_PUSH_DESCRIPTORS, 0, set, entries);
            
            descriptorTemplates.put(templateHandle, template);
            STAT_TEMPLATE_CREATIONS.incrementAndGet();
        }
        
        return templateHandle;
    }
    
    /**
     * Update descriptor set using a template.
     * Much faster than individual descriptor writes.
     */
    public void updateDescriptorSetWithTemplate(long descriptorSet, long templateHandle, 
                                                 ByteBuffer data) {
        DescriptorUpdateTemplate template = descriptorTemplates.get(templateHandle);
        if (template == null) {
            throw new IllegalArgumentException("Invalid template handle");
        }
        
        /*
         * vkUpdateDescriptorSetWithTemplate(vkDevice, descriptorSet, templateHandle, data);
         */
        
        VulkanCallMapper.vkUpdateDescriptorSetWithTemplate(
            vkDevice, descriptorSet, templateHandle, data);
        
        STAT_TEMPLATE_UPDATES.incrementAndGet();
    }
    
    /**
     * Push descriptors using a template.
     * Fastest binding update path in Vulkan.
     */
    public void cmdPushDescriptorSetWithTemplate(long commandBuffer, long templateHandle,
                                                  long layout, int set, ByteBuffer data) {
        /*
         * vkCmdPushDescriptorSetWithTemplateKHR(commandBuffer, templateHandle, layout, set, data);
         */
        
        VulkanCallMapper.vkCmdPushDescriptorSetWithTemplate(
            commandBuffer, templateHandle, layout, set, data);
        
        STAT_TEMPLATE_UPDATES.incrementAndGet();
    }
    
    /**
     * Destroy a descriptor update template.
     */
    public void destroyDescriptorUpdateTemplate(long templateHandle) {
        descriptorTemplates.remove(templateHandle);
        VulkanCallMapper.vkDestroyDescriptorUpdateTemplate(vkDevice, templateHandle);
    }
    
    /**
     * Get or create a standard template for buffer binding.
     */
    public long getBufferBindingTemplate(long descriptorSetLayout, int binding, int descriptorType) {
        // Check pool first
        Long pooled = templatePool.poll();
        if (pooled != null) {
            return pooled;
        }
        
        // Create new template
        DescriptorTemplateEntry[] entries = new DescriptorTemplateEntry[] {
            new DescriptorTemplateEntry(binding, descriptorType, 1, 0, 24) // VkDescriptorBufferInfo size
        };
        
        return createDescriptorUpdateTemplate(descriptorSetLayout, entries);
    }
    
    /**
     * Return a template to the pool for reuse.
     */
    public void releaseBufferBindingTemplate(long templateHandle) {
        if (templatePool.size() < DESCRIPTOR_TEMPLATE_POOL_SIZE) {
            templatePool.offer(templateHandle);
        } else {
            destroyDescriptorUpdateTemplate(templateHandle);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // 16-BIT STORAGE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Query 16-bit storage feature support.
     */
    protected void query16BitStorageFeatures() {
        /*
         * VkPhysicalDevice16BitStorageFeatures features16Bit = 
         *     VkPhysicalDevice16BitStorageFeatures.calloc()
         *         .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_16BIT_STORAGE_FEATURES);
         * 
         * VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
         *     .pNext(features16Bit);
         * 
         * vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, features2);
         * 
         * storage16BitFeatures.storageBuffer16BitAccess = features16Bit.storageBuffer16BitAccess();
         * storage16BitFeatures.uniformAndStorageBuffer16BitAccess = 
         *     features16Bit.uniformAndStorageBuffer16BitAccess();
         * storage16BitFeatures.storagePushConstant16 = features16Bit.storagePushConstant16();
         * storage16BitFeatures.storageInputOutput16 = features16Bit.storageInputOutput16();
         */
        
        boolean[] features = VulkanCallMapper.vkGet16BitStorageFeatures(vkPhysicalDevice);
        if (features != null && features.length >= 4) {
            storage16BitFeatures.storageBuffer16BitAccess = features[0];
            storage16BitFeatures.uniformAndStorageBuffer16BitAccess = features[1];
            storage16BitFeatures.storagePushConstant16 = features[2];
            storage16BitFeatures.storageInputOutput16 = features[3];
        }
        
        System.out.println("[Vulkan 1.1] " + storage16BitFeatures);
    }
    
    /**
     * Check if 16-bit storage is supported.
     */
    public boolean supports16BitStorage() {
        return storage16BitFeatures.supportsAny();
    }
    
    /**
     * Get 16-bit storage capabilities.
     */
    public Storage16BitFeatures get16BitStorageFeatures() {
        return storage16BitFeatures;
    }
    
    /**
     * Create a buffer optimized for 16-bit (half-precision) data.
     * Uses 50% less memory than 32-bit buffers.
     */
    public int createHalfPrecisionBuffer(long elementCount, int usage) {
        if (!storage16BitFeatures.storageBuffer16BitAccess) {
            throw new UnsupportedOperationException("16-bit storage not supported");
        }
        
        long size = elementCount * 2; // 2 bytes per half-precision element
        
        // Align to minimum buffer alignment
        size = alignTo(size, ALIGNMENT_STORAGE_BUFFER);
        
        VulkanBuffer baseBuffer = createBufferInternal(size, usage, MEMORY_HOST_VISIBLE, true);
        if (baseBuffer == null) return 0;
        
        HalfPrecisionBuffer buffer = new HalfPrecisionBuffer(
            baseBuffer.handle, baseBuffer.memory, size, usage, 
            baseBuffer.memoryProperties, elementCount);
        
        // Copy persistent mapping
        buffer.persistentMap = baseBuffer.persistentMap;
        
        int id = bufferIdGenerator.getAndIncrement();
        managedBuffers.put((long) id, buffer);
        STAT_BUFFERS_CREATED.incrementAndGet();
        STAT_BYTES_ALLOCATED.addAndGet(size);
        STAT_16BIT_BUFFERS.incrementAndGet();
        
        return id;
    }
    
    /**
     * Upload float data as half-precision to save bandwidth.
     */
    public void uploadHalfPrecision(int bufferId, long elementOffset, float[] data) {
        VulkanBuffer buffer = managedBuffers.get((long) bufferId);
        if (buffer == null) throw new IllegalArgumentException("Invalid buffer ID");
        
        if (!(buffer instanceof HalfPrecisionBuffer)) {
            throw new IllegalArgumentException("Buffer is not a half-precision buffer");
        }
        
        HalfPrecisionBuffer hpBuffer = (HalfPrecisionBuffer) buffer;
        hpBuffer.writeHalfPrecision(data, elementOffset * 2);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EXTERNAL MEMORY
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a buffer with external memory handle for cross-API sharing.
     */
    public ExternalMemoryHandle createExternalBuffer(long size, int usage, int handleType) {
        /*
         * // Check external memory support
         * VkPhysicalDeviceExternalBufferInfo externalInfo = 
         *     VkPhysicalDeviceExternalBufferInfo.calloc()
         *         .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTERNAL_BUFFER_INFO)
         *         .usage(usage)
         *         .handleType(handleType);
         * 
         * VkExternalBufferProperties externalProps = VkExternalBufferProperties.calloc()
         *     .sType(VK_STRUCTURE_TYPE_EXTERNAL_BUFFER_PROPERTIES);
         * 
         * vkGetPhysicalDeviceExternalBufferProperties(vkPhysicalDevice, externalInfo, externalProps);
         * 
         * boolean exportable = (externalProps.externalMemoryProperties().externalMemoryFeatures() &
         *     VK_EXTERNAL_MEMORY_FEATURE_EXPORTABLE_BIT) != 0;
         * 
         * if (!exportable) {
         *     throw new UnsupportedOperationException("External memory export not supported");
         * }
         * 
         * // Create buffer with external memory
         * VkExternalMemoryBufferCreateInfo externalBufferInfo = 
         *     VkExternalMemoryBufferCreateInfo.calloc()
         *         .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_BUFFER_CREATE_INFO)
         *         .handleTypes(handleType);
         * 
         * VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
         *     .pNext(externalBufferInfo)
         *     .size(size)
         *     .usage(usage);
         * 
         * long bufferHandle = vkCreateBuffer(vkDevice, bufferInfo);
         * 
         * // Allocate with export capability
         * VkExportMemoryAllocateInfo exportInfo = VkExportMemoryAllocateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO)
         *     .handleTypes(handleType);
         * 
         * VkMemoryDedicatedAllocateInfo dedicatedInfo = VkMemoryDedicatedAllocateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO)
         *     .pNext(exportInfo)
         *     .buffer(bufferHandle);
         * 
         * VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
         *     .pNext(dedicatedInfo);
         * 
         * long memoryHandle = vkAllocateMemory(vkDevice, allocInfo);
         * vkBindBufferMemory(vkDevice, bufferHandle, memoryHandle, 0);
         * 
         * // Get external handle
         * long externalHandle = 0;
         * if (handleType == VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT) {
         *     VkMemoryGetFdInfoKHR getFdInfo = VkMemoryGetFdInfoKHR.calloc()
         *         .sType(VK_STRUCTURE_TYPE_MEMORY_GET_FD_INFO_KHR)
         *         .memory(memoryHandle)
         *         .handleType(handleType);
         *     IntBuffer pFd = stack.ints(0);
         *     vkGetMemoryFdKHR(vkDevice, getFdInfo, pFd);
         *     externalHandle = pFd.get(0);
         * } else if (handleType == VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT) {
         *     VkMemoryGetWin32HandleInfoKHR getHandleInfo = ...;
         *     ...
         * }
         */
        
        // Verify external memory support
        int features = VulkanCallMapper.vkGetExternalBufferFeatures(vkPhysicalDevice, usage, handleType);
        if ((features & VK_EXTERNAL_MEMORY_FEATURE_EXPORTABLE_BIT) == 0) {
            throw new UnsupportedOperationException("External memory export not supported for handle type: " + handleType);
        }
        
        boolean dedicatedRequired = (features & VK_EXTERNAL_MEMORY_FEATURE_DEDICATED_ONLY_BIT) != 0;
        
        // Create buffer with external memory capability
        long bufferHandle = VulkanCallMapper.vkCreateExternalBuffer(vkDevice, size, usage, handleType);
        if (bufferHandle == VK_NULL_HANDLE) {
            throw new RuntimeException("Failed to create external buffer");
        }
        
        long memReqSize = VulkanCallMapper.vkGetBufferMemoryRequirements(vkDevice, bufferHandle);
        int memTypeIndex = findMemoryType(MEMORY_DEVICE_LOCAL);
        
        // Allocate with export and dedicated allocation
        long memoryHandle = VulkanCallMapper.vkAllocateExternalMemory(
            vkDevice, memReqSize, memTypeIndex, handleType, bufferHandle, dedicatedRequired);
        
        if (memoryHandle == VK_NULL_HANDLE) {
            VulkanCallMapper.vkDestroyBuffer(vkDevice, bufferHandle);
            throw new RuntimeException("Failed to allocate external memory");
        }
        
        VulkanCallMapper.vkBindBufferMemory(vkDevice, bufferHandle, memoryHandle, 0);
        
        // Get the external handle
        long externalHandle = VulkanCallMapper.vkGetMemoryExternalHandle(vkDevice, memoryHandle, handleType);
        
        ExternalMemoryHandle handle = new ExternalMemoryHandle(
            bufferHandle, memoryHandle, size, handleType, externalHandle, dedicatedRequired);
        
        externalHandles.put(bufferHandle, handle);
        STAT_EXTERNAL_EXPORTS.incrementAndGet();
        
        return handle;
    }
    
    /**
     * Import external memory from another API or process.
     */
    public int importExternalBuffer(long size, int usage, int handleType, long externalHandle) {
        /*
         * VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
         *     .size(size)
         *     .usage(usage);
         * 
         * long bufferHandle = vkCreateBuffer(vkDevice, bufferInfo);
         * 
         * VkImportMemoryFdInfoKHR importFdInfo = VkImportMemoryFdInfoKHR.calloc()
         *     .sType(VK_STRUCTURE_TYPE_IMPORT_MEMORY_FD_INFO_KHR)
         *     .handleType(handleType)
         *     .fd((int) externalHandle);
         * 
         * VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
         *     .pNext(importFdInfo)
         *     .allocationSize(size)
         *     .memoryTypeIndex(memTypeIndex);
         * 
         * long memoryHandle = vkAllocateMemory(vkDevice, allocInfo);
         * vkBindBufferMemory(vkDevice, bufferHandle, memoryHandle, 0);
         */
        
        long bufferHandle = VulkanCallMapper.vkCreateBuffer(vkDevice, size, usage);
        if (bufferHandle == VK_NULL_HANDLE) return 0;
        
        int memTypeIndex = findMemoryType(MEMORY_DEVICE_LOCAL);
        
        long memoryHandle = VulkanCallMapper.vkImportExternalMemory(
            vkDevice, size, memTypeIndex, handleType, externalHandle);
        
        if (memoryHandle == VK_NULL_HANDLE) {
            VulkanCallMapper.vkDestroyBuffer(vkDevice, bufferHandle);
            return 0;
        }
        
        VulkanCallMapper.vkBindBufferMemory(vkDevice, bufferHandle, memoryHandle, 0);
        
        VulkanBuffer buffer = new VulkanBuffer(bufferHandle, memoryHandle, size, usage, MEMORY_DEVICE_LOCAL);
        
        int id = bufferIdGenerator.getAndIncrement();
        managedBuffers.put((long) id, buffer);
        STAT_BUFFERS_CREATED.incrementAndGet();
        STAT_EXTERNAL_IMPORTS.incrementAndGet();
        
        return id;
    }
    
    /**
     * Get platform-specific external handle for sharing.
     */
    public long getExternalHandle(int bufferId) {
        VulkanBuffer buffer = managedBuffers.get((long) bufferId);
        if (buffer == null) return -1;
        
        ExternalMemoryHandle handle = externalHandles.get(buffer.handle);
        return handle != null ? handle.externalHandle : -1;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // MULTIVIEW
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Query multiview capabilities.
     */
    protected void queryMultiviewCapabilities() {
        /*
         * VkPhysicalDeviceMultiviewFeatures multiviewFeatures = 
         *     VkPhysicalDeviceMultiviewFeatures.calloc()
         *         .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTIVIEW_FEATURES);
         * 
         * VkPhysicalDeviceMultiviewProperties multiviewProps = 
         *     VkPhysicalDeviceMultiviewProperties.calloc()
         *         .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTIVIEW_PROPERTIES);
         * 
         * VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
         *     .pNext(multiviewFeatures);
         * 
         * VkPhysicalDeviceProperties2 props2 = VkPhysicalDeviceProperties2.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2)
         *     .pNext(multiviewProps);
         * 
         * vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, features2);
         * vkGetPhysicalDeviceProperties2(vkPhysicalDevice, props2);
         */
        
        int[] caps = VulkanCallMapper.vkGetMultiviewCapabilities(vkPhysicalDevice);
        if (caps != null && caps.length >= 5) {
            multiviewCapabilities.multiview = caps[0] != 0;
            multiviewCapabilities.multiviewGeometryShader = caps[1] != 0;
            multiviewCapabilities.multiviewTessellationShader = caps[2] != 0;
            multiviewCapabilities.maxMultiviewViewCount = caps[3];
            multiviewCapabilities.maxMultiviewInstanceIndex = caps[4];
        }
        
        System.out.println("[Vulkan 1.1] " + multiviewCapabilities);
    }
    
    /**
     * Check if multiview (stereo VR) is supported.
     */
    public boolean supportsMultiview() {
        return multiviewCapabilities.multiview;
    }
    
    /**
     * Get multiview capabilities.
     */
    public MultiviewCapabilities getMultiviewCapabilities() {
        return multiviewCapabilities;
    }
    
    /**
     * Create a buffer sized for multiview rendering.
     * Automatically allocates space for multiple views.
     */
    public int createMultiviewBuffer(long perViewSize, int viewCount, int usage) {
        if (!multiviewCapabilities.multiview) {
            throw new UnsupportedOperationException("Multiview not supported");
        }
        
        if (viewCount > multiviewCapabilities.maxMultiviewViewCount) {
            throw new IllegalArgumentException("View count exceeds maximum: " + 
                multiviewCapabilities.maxMultiviewViewCount);
        }
        
        long totalSize = perViewSize * viewCount;
        int bufferId = createBuffer(totalSize, usage, MEMORY_DEVICE_LOCAL);
        
        STAT_MULTIVIEW_ALLOCATIONS.incrementAndGet();
        return bufferId;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PROTECTED MEMORY
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Query protected memory capabilities.
     */
    protected void queryProtectedMemoryCapabilities() {
        /*
         * VkPhysicalDeviceProtectedMemoryFeatures protectedFeatures = 
         *     VkPhysicalDeviceProtectedMemoryFeatures.calloc()
         *         .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROTECTED_MEMORY_FEATURES);
         * 
         * VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
         *     .pNext(protectedFeatures);
         * 
         * vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, features2);
         */
        
        boolean[] features = VulkanCallMapper.vkGetProtectedMemoryFeatures(vkPhysicalDevice);
        if (features != null && features.length >= 2) {
            protectedMemoryCapabilities.protectedMemory = features[0];
            protectedMemoryCapabilities.protectedNoFault = features[1];
        }
        
        // Find protected queue
        if (protectedMemoryCapabilities.protectedMemory) {
            protectedMemoryCapabilities.protectedQueueIndex = 
                VulkanCallMapper.vkFindProtectedQueueFamily(vkPhysicalDevice);
        }
        
        if (protectedMemoryCapabilities.isAvailable()) {
            System.out.println("[Vulkan 1.1] Protected memory available");
        }
    }
    
    /**
     * Initialize protected queue for DRM content.
     */
    protected void initializeProtectedQueue() {
        if (!protectedMemoryCapabilities.isAvailable()) return;
        if (protectedMemoryCapabilities.protectedQueueIndex < 0) return;
        
        /*
         * VkDeviceQueueInfo2 queueInfo = VkDeviceQueueInfo2.calloc()
         *     .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_INFO_2)
         *     .flags(VK_DEVICE_QUEUE_CREATE_PROTECTED_BIT)
         *     .queueFamilyIndex(protectedMemoryCapabilities.protectedQueueIndex)
         *     .queueIndex(0);
         * 
         * PointerBuffer pQueue = stack.mallocPointer(1);
         * vkGetDeviceQueue2(vkDevice, queueInfo, pQueue);
         * protectedQueue = pQueue.get(0);
         * 
         * // Create protected command pool
         * VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
         *     .flags(VK_COMMAND_POOL_CREATE_PROTECTED_BIT | 
         *            VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
         *     .queueFamilyIndex(protectedMemoryCapabilities.protectedQueueIndex);
         * 
         * LongBuffer pPool = stack.mallocLong(1);
         * vkCreateCommandPool(vkDevice, poolInfo, null, pPool);
         * protectedCommandPool = pPool.get(0);
         */
        
        protectedQueue = VulkanCallMapper.vkGetProtectedQueue(
            vkDevice, protectedMemoryCapabilities.protectedQueueIndex);
        
        if (protectedQueue != VK_NULL_HANDLE) {
            protectedCommandPool = VulkanCallMapper.vkCreateProtectedCommandPool(
                vkDevice,                 protectedMemoryCapabilities.protectedQueueIndex);
        }
        
        System.out.println("[Vulkan 1.1] Protected queue initialized: " + 
            (protectedQueue != VK_NULL_HANDLE));
    }
    
    /**
     * Check if protected memory is available.
     */
    public boolean supportsProtectedMemory() {
        return protectedMemoryCapabilities.isAvailable();
    }
    
    /**
     * Get protected memory capabilities.
     */
    public ProtectedMemoryCapabilities getProtectedMemoryCapabilities() {
        return protectedMemoryCapabilities;
    }
    
    /**
     * Create a protected buffer for DRM content.
     * Protected buffers cannot be read back by the CPU.
     */
    public int createProtectedBuffer(long size, int usage) {
        if (!protectedMemoryCapabilities.isAvailable()) {
            throw new UnsupportedOperationException("Protected memory not supported");
        }
        
        /*
         * VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
         *     .flags(VK_BUFFER_CREATE_PROTECTED_BIT)
         *     .size(size)
         *     .usage(usage)
         *     .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
         * 
         * LongBuffer pBuffer = stack.mallocLong(1);
         * vkCreateBuffer(vkDevice, bufferInfo, null, pBuffer);
         * long bufferHandle = pBuffer.get(0);
         * 
         * VkMemoryRequirements memReqs = VkMemoryRequirements.malloc();
         * vkGetBufferMemoryRequirements(vkDevice, bufferHandle, memReqs);
         * 
         * // Find protected memory type
         * int memTypeIndex = findProtectedMemoryType(memReqs.memoryTypeBits());
         * 
         * VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
         *     .allocationSize(memReqs.size())
         *     .memoryTypeIndex(memTypeIndex);
         * 
         * LongBuffer pMemory = stack.mallocLong(1);
         * vkAllocateMemory(vkDevice, allocInfo, null, pMemory);
         * long memoryHandle = pMemory.get(0);
         * 
         * vkBindBufferMemory(vkDevice, bufferHandle, memoryHandle, 0);
         */
        
        long bufferHandle = VulkanCallMapper.vkCreateProtectedBuffer(vkDevice, size, usage);
        if (bufferHandle == VK_NULL_HANDLE) {
            throw new RuntimeException("Failed to create protected buffer");
        }
        
        long memReqSize = VulkanCallMapper.vkGetBufferMemoryRequirements(vkDevice, bufferHandle);
        int memTypeIndex = findProtectedMemoryType();
        
        long memoryHandle = VulkanCallMapper.vkAllocateProtectedMemory(vkDevice, memReqSize, memTypeIndex);
        if (memoryHandle == VK_NULL_HANDLE) {
            VulkanCallMapper.vkDestroyBuffer(vkDevice, bufferHandle);
            throw new RuntimeException("Failed to allocate protected memory");
        }
        
        VulkanCallMapper.vkBindBufferMemory(vkDevice, bufferHandle, memoryHandle, 0);
        
        // Protected buffers use device-local memory with protected bit
        int memProps = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | VK_MEMORY_PROPERTY_PROTECTED_BIT;
        VulkanBuffer buffer = new VulkanBuffer(bufferHandle, memoryHandle, size, usage, memProps);
        
        int id = bufferIdGenerator.getAndIncrement();
        managedBuffers.put((long) id, buffer);
        STAT_BUFFERS_CREATED.incrementAndGet();
        STAT_BYTES_ALLOCATED.addAndGet(size);
        STAT_PROTECTED_ALLOCATIONS.incrementAndGet();
        
        return id;
    }
    
    /**
     * Find memory type with protected bit.
     */
    protected int findProtectedMemoryType() {
        for (int i = 0; i < memoryTypeCache.memoryTypeCount; i++) {
            int flags = memoryTypeCache.memoryTypeFlags[i];
            if ((flags & VK_MEMORY_PROPERTY_PROTECTED_BIT) != 0 &&
                (flags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0) {
                return i;
            }
        }
        return memoryTypeCache.deviceLocalIndex; // Fallback
    }
    
    /**
     * Submit protected content command buffer.
     */
    public void submitProtectedCommandBuffer(long commandBuffer, long fence) {
        if (protectedQueue == VK_NULL_HANDLE) {
            throw new IllegalStateException("Protected queue not initialized");
        }
        
        /*
         * VkProtectedSubmitInfo protectedSubmitInfo = VkProtectedSubmitInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PROTECTED_SUBMIT_INFO)
         *     .protectedSubmit(true);
         * 
         * VkSubmitInfo submitInfo = VkSubmitInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
         *     .pNext(protectedSubmitInfo)
         *     .commandBufferCount(1)
         *     .pCommandBuffers(commandBuffer);
         * 
         * vkQueueSubmit(protectedQueue, submitInfo, fence);
         */
        
        VulkanCallMapper.vkQueueSubmitProtected(protectedQueue, commandBuffer, fence);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // YCBCR SAMPLER CONVERSION (for video)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * YCbCr conversion configuration for video buffers.
     */
    public static final class YCbCrConversionConfig {
        public int format;
        public int ycbcrModel;
        public int ycbcrRange;
        public int xChromaOffset;
        public int yChromaOffset;
        public int chromaFilter;
        public boolean forceExplicitReconstruction;
        
        public YCbCrConversionConfig() {
            // Default to BT.709 (HD video)
            this.ycbcrModel = 2; // VK_SAMPLER_YCBCR_MODEL_CONVERSION_YCBCR_709
            this.ycbcrRange = 0; // VK_SAMPLER_YCBCR_RANGE_ITU_FULL
            this.xChromaOffset = 1; // VK_CHROMA_LOCATION_MIDPOINT
            this.yChromaOffset = 1;
            this.chromaFilter = 1; // VK_FILTER_LINEAR
            this.forceExplicitReconstruction = false;
        }
    }
    
    protected final ConcurrentHashMap<Long, Long> ycbcrConversions = new ConcurrentHashMap<>();
    
    /**
     * Create YCbCr sampler conversion for video processing.
     */
    public long createYCbCrConversion(YCbCrConversionConfig config) {
        /*
         * VkSamplerYcbcrConversionCreateInfo conversionInfo = 
         *     VkSamplerYcbcrConversionCreateInfo.calloc()
         *         .sType(VK_STRUCTURE_TYPE_SAMPLER_YCBCR_CONVERSION_CREATE_INFO)
         *         .format(config.format)
         *         .ycbcrModel(config.ycbcrModel)
         *         .ycbcrRange(config.ycbcrRange)
         *         .components(VkComponentMapping.calloc()
         *             .r(VK_COMPONENT_SWIZZLE_IDENTITY)
         *             .g(VK_COMPONENT_SWIZZLE_IDENTITY)
         *             .b(VK_COMPONENT_SWIZZLE_IDENTITY)
         *             .a(VK_COMPONENT_SWIZZLE_IDENTITY))
         *         .xChromaOffset(config.xChromaOffset)
         *         .yChromaOffset(config.yChromaOffset)
         *         .chromaFilter(config.chromaFilter)
         *         .forceExplicitReconstruction(config.forceExplicitReconstruction);
         * 
         * LongBuffer pConversion = stack.mallocLong(1);
         * vkCreateSamplerYcbcrConversion(vkDevice, conversionInfo, null, pConversion);
         * return pConversion.get(0);
         */
        
        return VulkanCallMapper.vkCreateSamplerYcbcrConversion(vkDevice, config);
    }
    
    /**
     * Destroy YCbCr conversion.
     */
    public void destroyYCbCrConversion(long conversion) {
        VulkanCallMapper.vkDestroySamplerYcbcrConversion(vkDevice, conversion);
        ycbcrConversions.remove(conversion);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BIND BUFFER MEMORY 2 (Enhanced binding)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Bind multiple buffers to memory in a single call.
     * More efficient than individual binds.
     */
    public void bindBufferMemory2(long[] buffers, long[] memories, long[] offsets) {
        if (buffers.length != memories.length || buffers.length != offsets.length) {
            throw new IllegalArgumentException("Array lengths must match");
        }
        
        /*
         * VkBindBufferMemoryInfo.Buffer bindInfos = VkBindBufferMemoryInfo.calloc(buffers.length);
         * 
         * for (int i = 0; i < buffers.length; i++) {
         *     bindInfos.get(i)
         *         .sType(VK_STRUCTURE_TYPE_BIND_BUFFER_MEMORY_INFO)
         *         .buffer(buffers[i])
         *         .memory(memories[i])
         *         .memoryOffset(offsets[i]);
         * }
         * 
         * vkBindBufferMemory2(vkDevice, bindInfos);
         */
        
        VulkanCallMapper.vkBindBufferMemory2(vkDevice, buffers, memories, offsets);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // GET BUFFER MEMORY REQUIREMENTS 2 (Enhanced query)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Extended memory requirements including dedicated allocation requirements.
     */
    public static final class MemoryRequirements2 {
        public long size;
        public long alignment;
        public int memoryTypeBits;
        public boolean prefersDedicatedAllocation;
        public boolean requiresDedicatedAllocation;
    }
    
    /**
     * Get extended memory requirements for a buffer.
     */
    public MemoryRequirements2 getBufferMemoryRequirements2(long buffer) {
        /*
         * VkBufferMemoryRequirementsInfo2 info = VkBufferMemoryRequirementsInfo2.calloc()
         *     .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_REQUIREMENTS_INFO_2)
         *     .buffer(buffer);
         * 
         * VkMemoryDedicatedRequirements dedicatedReqs = VkMemoryDedicatedRequirements.calloc()
         *     .sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_REQUIREMENTS);
         * 
         * VkMemoryRequirements2 memReqs2 = VkMemoryRequirements2.calloc()
         *     .sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2)
         *     .pNext(dedicatedReqs);
         * 
         * vkGetBufferMemoryRequirements2(vkDevice, info, memReqs2);
         * 
         * MemoryRequirements2 result = new MemoryRequirements2();
         * result.size = memReqs2.memoryRequirements().size();
         * result.alignment = memReqs2.memoryRequirements().alignment();
         * result.memoryTypeBits = memReqs2.memoryRequirements().memoryTypeBits();
         * result.prefersDedicatedAllocation = dedicatedReqs.prefersDedicatedAllocation();
         * result.requiresDedicatedAllocation = dedicatedReqs.requiresDedicatedAllocation();
         */
        
        return VulkanCallMapper.vkGetBufferMemoryRequirements2(vkDevice, buffer);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DEVICE UUID AND DRIVER INFO
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Device identification for external memory sharing.
     */
    public static final class DeviceIdProperties {
        public byte[] deviceUUID = new byte[16];
        public byte[] driverUUID = new byte[16];
        public byte[] deviceLUID = new byte[8];
        public int deviceNodeMask;
        public boolean deviceLUIDValid;
        
        public String getDeviceUUIDString() {
            return bytesToUUID(deviceUUID);
        }
        
        public String getDriverUUIDString() {
            return bytesToUUID(driverUUID);
        }
        
        private static String bytesToUUID(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                if (i == 4 || i == 6 || i == 8 || i == 10) sb.append('-');
                sb.append(String.format("%02x", bytes[i] & 0xFF));
            }
            return sb.toString();
        }
    }
    
    protected DeviceIdProperties deviceIdProperties;
    
    /**
     * Get device UUID for external memory compatibility checking.
     */
    public DeviceIdProperties getDeviceIdProperties() {
        if (deviceIdProperties == null) {
            deviceIdProperties = new DeviceIdProperties();
            VulkanCallMapper.vkGetPhysicalDeviceIdProperties(vkPhysicalDevice, deviceIdProperties);
        }
        return deviceIdProperties;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VARIABLE POINTERS FEATURE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    protected boolean variablePointersEnabled = false;
    protected boolean variablePointersStorageBufferEnabled = false;
    
    /**
     * Check if variable pointers are supported (for complex shader buffer access).
     */
    public boolean supportsVariablePointers() {
        return variablePointersEnabled;
    }
    
    /**
     * Check if variable pointers in storage buffers are supported.
     */
    public boolean supportsVariablePointersStorageBuffer() {
        return variablePointersStorageBufferEnabled;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // OPTIMAL BUFFER CREATION HELPERS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Create buffer with optimal memory allocation strategy.
     * Uses dedicated allocation when beneficial.
     */
    public int createOptimalBuffer(long size, int usage, int memoryProperties) {
        long bufferHandle = VulkanCallMapper.vkCreateBuffer(vkDevice, size, usage);
        if (bufferHandle == VK_NULL_HANDLE) return 0;
        
        // Get extended memory requirements
        MemoryRequirements2 memReqs = getBufferMemoryRequirements2(bufferHandle);
        int memTypeIndex = findMemoryType(memoryProperties);
        
        long memoryHandle;
        
        if (memReqs.requiresDedicatedAllocation || 
            (memReqs.prefersDedicatedAllocation && size >= 1024 * 1024)) {
            // Use dedicated allocation for large buffers or when required
            memoryHandle = VulkanCallMapper.vkAllocateDedicatedMemory(
                vkDevice, memReqs.size, memTypeIndex, bufferHandle, VK_NULL_HANDLE);
        } else {
            // Standard allocation
            memoryHandle = VulkanCallMapper.vkAllocateMemory(vkDevice, memReqs.size, memTypeIndex);
        }
        
        if (memoryHandle == VK_NULL_HANDLE) {
            VulkanCallMapper.vkDestroyBuffer(vkDevice, bufferHandle);
            return 0;
        }
        
        VulkanCallMapper.vkBindBufferMemory(vkDevice, bufferHandle, memoryHandle, 0);
        
        VulkanBuffer buffer = new VulkanBuffer(bufferHandle, memoryHandle, size, usage, memoryProperties);
        
        // Persistent mapping for host-visible
        if ((memoryProperties & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) {
            buffer.persistentMap = mapMemory(memoryHandle, 0, size);
        }
        
        int id = bufferIdGenerator.getAndIncrement();
        managedBuffers.put((long) id, buffer);
        STAT_BUFFERS_CREATED.incrementAndGet();
        STAT_BYTES_ALLOCATED.addAndGet(size);
        
        return id;
    }
    
    /**
     * Create vertex buffer with subgroup-optimized size alignment.
     */
    public int createSubgroupOptimizedVertexBuffer(long vertexCount, int vertexSize) {
        // Align vertex count to subgroup size for optimal parallel access
        int subgroupSize = subgroupProperties.subgroupSize;
        if (subgroupSize > 0) {
            vertexCount = ((vertexCount + subgroupSize - 1) / subgroupSize) * subgroupSize;
        }
        
        long size = vertexCount * vertexSize;
        return createBuffer(size, USAGE_STATIC_VERTEX, MEMORY_DEVICE_LOCAL);
    }
    
    /**
     * Create compute storage buffer with optimal subgroup alignment.
     */
    public int createSubgroupOptimizedStorageBuffer(long elementCount, int elementSize) {
        int subgroupSize = subgroupProperties.subgroupSize;
        if (subgroupSize > 0) {
            elementCount = ((elementCount + subgroupSize - 1) / subgroupSize) * subgroupSize;
        }
        
        long size = alignTo(elementCount * elementSize, ALIGNMENT_STORAGE_BUFFER);
        return createBuffer(size, USAGE_STORAGE, MEMORY_DEVICE_LOCAL);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EXTERNAL SEMAPHORE SUPPORT
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Create semaphore with external handle for cross-API sync.
     */
    public long createExternalSemaphore(int handleType) {
        /*
         * VkExportSemaphoreCreateInfo exportInfo = VkExportSemaphoreCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO)
         *     .handleTypes(handleType);
         * 
         * VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
         *     .pNext(exportInfo);
         * 
         * LongBuffer pSemaphore = stack.mallocLong(1);
         * vkCreateSemaphore(vkDevice, semaphoreInfo, null, pSemaphore);
         * return pSemaphore.get(0);
         */
        
        return VulkanCallMapper.vkCreateExternalSemaphore(vkDevice, handleType);
    }
    
    /**
     * Get external handle from semaphore.
     */
    public long getSemaphoreExternalHandle(long semaphore, int handleType) {
        return VulkanCallMapper.vkGetSemaphoreExternalHandle(vkDevice, semaphore, handleType);
    }
    
    /**
     * Import semaphore from external handle.
     */
    public long importExternalSemaphore(int handleType, long externalHandle) {
        return VulkanCallMapper.vkImportExternalSemaphore(vkDevice, handleType, externalHandle);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EXTERNAL FENCE SUPPORT
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Create fence with external handle for cross-API sync.
     */
    public long createExternalFence(int handleType, boolean signaled) {
        /*
         * VkExportFenceCreateInfo exportInfo = VkExportFenceCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_EXPORT_FENCE_CREATE_INFO)
         *     .handleTypes(handleType);
         * 
         * VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
         *     .pNext(exportInfo)
         *     .flags(signaled ? VK_FENCE_CREATE_SIGNALED_BIT : 0);
         * 
         * LongBuffer pFence = stack.mallocLong(1);
         * vkCreateFence(vkDevice, fenceInfo, null, pFence);
         * return pFence.get(0);
         */
        
        return VulkanCallMapper.vkCreateExternalFence(vkDevice, handleType, signaled);
    }
    
    /**
     * Get external handle from fence.
     */
    public long getFenceExternalHandle(long fence, int handleType) {
        return VulkanCallMapper.vkGetFenceExternalHandle(vkDevice, fence, handleType);
    }
    
    /**
     * Import fence from external handle.
     */
    public long importExternalFence(int handleType, long externalHandle) {
        return.VulkanCallMapper.vkImportExternalFence(vkDevice, handleType, externalHandle);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TRIM COMMAND POOL (Memory optimization)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Trim command pool to release unused memory.
     * Call periodically during low-activity periods.
     */
    public void trimCommandPool() {
        if (vkCommandPool != VK_NULL_HANDLE) {
            VulkanCallMapper.vkTrimCommandPool(vkDevice, vkCommandPool, 0);
        }
        if (vkTransferCommandPool != VK_NULL_HANDLE) {
            VulkanCallMapper.vkTrimCommandPool(vkDevice, vkTransferCommandPool, 0);
        }
        if (protectedCommandPool != VK_NULL_HANDLE) {
            VulkanCallMapper.vkTrimCommandPool(vkDevice, protectedCommandPool, 0);
        }
        
        // Trim thread-local pools
        for (ThreadCommandPool pool : threadCommandPools.values()) {
            if (pool.commandPool != VK_NULL_HANDLE) {
                VulkanCallMapper.vkTrimCommandPool(vkDevice, pool.commandPool, 0);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BATCH OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Batch buffer creation for reduced overhead.
     */
    public int[] createBuffersBatch(long[] sizes, int[] usages, int[] memoryProperties) {
        if (sizes.length != usages.length || sizes.length != memoryProperties.length) {
            throw new IllegalArgumentException("Array lengths must match");
        }
        
        int[] bufferIds = new int[sizes.length];
        
        // Create all buffers
        long[] bufferHandles = new long[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            bufferHandles[i] = VulkanCallMapper.vkCreateBuffer(vkDevice, sizes[i], usages[i]);
            if (bufferHandles[i] == VK_NULL_HANDLE) {
                // Cleanup already created
                for (int j = 0; j < i; j++) {
                    VulkanCallMapper.vkDestroyBuffer(vkDevice, bufferHandles[j]);
                }
                throw new RuntimeException("Failed to create buffer at index " + i);
            }
        }
        
        // Get memory requirements for all
        long[] memSizes = new long[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            memSizes[i] = VulkanCallMapper.vkGetBufferMemoryRequirements(vkDevice, bufferHandles[i]);
        }
        
        // Allocate memory for all
        long[] memoryHandles = new long[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            int memTypeIndex = findMemoryType(memoryProperties[i]);
            memoryHandles[i] = VulkanCallMapper.vkAllocateMemory(vkDevice, memSizes[i], memTypeIndex);
        }
        
        // Bind all using vkBindBufferMemory2
        long[] offsets = new long[sizes.length];
        Arrays.fill(offsets, 0);
        VulkanCallMapper.vkBindBufferMemory2(vkDevice, bufferHandles, memoryHandles, offsets);
        
        // Register all buffers
        for (int i = 0; i < sizes.length; i++) {
            VulkanBuffer buffer = new VulkanBuffer(
                bufferHandles[i], memoryHandles[i], sizes[i], usages[i], memoryProperties[i]);
            
            if ((memoryProperties[i] & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) {
                buffer.persistentMap = mapMemory(memoryHandles[i], 0, sizes[i]);
            }
            
            int id = bufferIdGenerator.getAndIncrement();
            managedBuffers.put((long) id, buffer);
            bufferIds[i] = id;
            
            STAT_BUFFERS_CREATED.incrementAndGet();
            STAT_BYTES_ALLOCATED.addAndGet(sizes[i]);
        }
        
        return bufferIds;
    }
    
    /**
     * Batch buffer deletion.
     */
    public void deleteBuffersBatch(int[] bufferIds) {
        // Collect all handles
        List<VulkanBuffer> toDelete = new ArrayList<>();
        
        for (int id : bufferIds) {
            VulkanBuffer buffer = managedBuffers.remove((long) id);
            if (buffer != null) {
                toDelete.add(buffer);
            }
        }
        
        // Wait for GPU to finish using these buffers
        VulkanCallMapper.vkDeviceWaitIdle(vkDevice);
        
        // Destroy all
        for (VulkanBuffer buffer : toDelete) {
            if (buffer.persistentMap != null) {
                unmapMemory(buffer.memory);
            }
            VulkanCallMapper.vkDestroyBuffer(vkDevice, buffer.handle);
            VulkanCallMapper.vkFreeMemory(vkDevice, buffer.memory);
            
            STAT_BUFFERS_DESTROYED.incrementAndGet();
            STAT_BYTES_FREED.addAndGet(buffer.size);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // QUEUE FAMILY OWNERSHIP TRANSFER
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Transfer buffer ownership between queue families.
     * Required when using dedicated transfer queue.
     */
    public void transferQueueOwnership(int bufferId, int srcQueueFamily, int dstQueueFamily) {
        VulkanBuffer buffer = managedBuffers.get((long) bufferId);
        if (buffer == null) return;
        
        if (srcQueueFamily == dstQueueFamily) return;
        
        // Release barrier on source queue
        long srcCmdBuffer = allocateCommandBuffer(vkCommandPool, VK_COMMAND_BUFFER_LEVEL_PRIMARY);
        beginCommandBuffer(srcCmdBuffer, VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        
        VulkanCallMapper.vkCmdPipelineBarrierQueueTransfer(
            srcCmdBuffer,
            VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
            VK_ACCESS_MEMORY_WRITE_BIT, 0,
            buffer.handle, 0, buffer.size,
            srcQueueFamily, dstQueueFamily);
        
        endCommandBuffer(srcCmdBuffer);
        
        long fence = acquireFence();
        long srcQueue = srcQueueFamily == graphicsQueueFamily ? vkGraphicsQueue : vkTransferQueue;
        submitCommandBuffer(srcQueue, srcCmdBuffer, fence);
        waitForFence(fence, DEFAULT_FENCE_TIMEOUT);
        releaseFence(fence);
        
        // Acquire barrier on destination queue
        long dstCmdBuffer = allocateCommandBuffer(vkTransferCommandPool, VK_COMMAND_BUFFER_LEVEL_PRIMARY);
        beginCommandBuffer(dstCmdBuffer, VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        
        VulkanCallMapper.vkCmdPipelineBarrierQueueTransfer(
            dstCmdBuffer,
            VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
            0, VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT,
            buffer.handle, 0, buffer.size,
            srcQueueFamily, dstQueueFamily);
        
        endCommandBuffer(dstCmdBuffer);
        
        fence = acquireFence();
        long dstQueue = dstQueueFamily == graphicsQueueFamily ? vkGraphicsQueue : vkTransferQueue;
        submitCommandBuffer(dstQueue, dstCmdBuffer, fence);
        waitForFence(fence, DEFAULT_FENCE_TIMEOUT);
        releaseFence(fence);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATISTICS (Vulkan 1.1 Specific)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static long getTemplateUpdates() { return STAT_TEMPLATE_UPDATES.get(); }
    public static long getTemplateCreations() { return STAT_TEMPLATE_CREATIONS.get(); }
    public static long getMultiGpuTransfers() { return STAT_MULTI_GPU_TRANSFERS.get(); }
    public static long getExternalExports() { return STAT_EXTERNAL_EXPORTS.get(); }
    public static long getExternalImports() { return STAT_EXTERNAL_IMPORTS.get(); }
    public static long get16BitBuffers() { return STAT_16BIT_BUFFERS.get(); }
    public static long getMultiviewAllocations() { return STAT_MULTIVIEW_ALLOCATIONS.get(); }
    public static long getProtectedAllocations() { return STAT_PROTECTED_ALLOCATIONS.get(); }
    
    public static void resetStats11() {
        resetStats(); // Reset 1.0 stats
        STAT_TEMPLATE_UPDATES.set(0);
        STAT_TEMPLATE_CREATIONS.set(0);
        STAT_MULTI_GPU_TRANSFERS.set(0);
        STAT_EXTERNAL_EXPORTS.set(0);
        STAT_EXTERNAL_IMPORTS.set(0);
        STAT_16BIT_BUFFERS.set(0);
        STAT_MULTIVIEW_ALLOCATIONS.set(0);
        STAT_PROTECTED_ALLOCATIONS.set(0);
    }
    
    /**
     * Get comprehensive statistics report.
     */
    public String getStatisticsReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Vulkan 1.1 Statistics ===\n");
        sb.append(String.format("Buffers: created=%d, destroyed=%d, active=%d\n",
            getBuffersCreated(), getBuffersDestroyed(), getBuffersActive()));
        sb.append(String.format("Memory: allocated=%d MB, freed=%d MB, in use=%d MB\n",
            getBytesAllocated() / (1024*1024), getBytesFreed() / (1024*1024), 
            getBytesInUse() / (1024*1024)));
        sb.append(String.format("Transfers: staged=%d, direct=%d, bytes=%d MB\n",
            getStagingUploads(), getDirectWrites(), getBytesTransferred() / (1024*1024)));
        sb.append(String.format("Commands: recorded=%d, fences created=%d, fences waited=%d\n",
            getCommandBuffersRecorded(), getFencesCreated(), getFencesWaited()));
        sb.append(String.format("Templates: created=%d, updates=%d\n",
            getTemplateCreations(), getTemplateUpdates()));
        sb.append(String.format("Multi-GPU: enabled=%b, transfers=%d\n",
            isMultiGpuEnabled(), getMultiGpuTransfers()));
        sb.append(String.format("External: exports=%d, imports=%d\n",
            getExternalExports(), getExternalImports()));
        sb.append(String.format("16-bit buffers: %d\n", get16BitBuffers()));
        sb.append(String.format("Multiview allocations: %d\n", getMultiviewAllocations()));
        sb.append(String.format("Protected allocations: %d\n", getProtectedAllocations()));
        return sb.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VERSION INFO OVERRIDES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Override 
    public int getVersionCode() { return 11; }
    
    @Override 
    public int getDisplayColor() { return DISPLAY_COLOR_11; }
    
    @Override 
    public String getVersionName() { return VERSION_NAME_11; }
    
    // Additional capability queries
    public boolean supportsSubgroups() { 
        return subgroupProperties.subgroupSize > 0; 
    }
    
    public boolean supportsDeviceGroups() { 
        return multiGpuEnabled; 
    }
    
    public boolean supportsDescriptorUpdateTemplates() { 
        return true; // Core in 1.1
    }
    
    public boolean supportsExternalMemory() { 
        return true; // Core in 1.1
    }
    
    public boolean supportsExternalSemaphore() { 
        return true; // Core in 1.1
    }
    
    public boolean supportsExternalFence() { 
        return true; // Core in 1.1
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CLEANUP OVERRIDE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Override
    protected void cleanup() {
        // Cleanup Vulkan 1.1 specific resources
        
        // Destroy descriptor update templates
        for (Long handle : descriptorTemplates.keySet()) {
            VulkanCallMapper.vkDestroyDescriptorUpdateTemplate(vkDevice, handle);
        }
        descriptorTemplates.clear();
        
        while (!templatePool.isEmpty()) {
            Long handle = templatePool.poll();
            if (handle != null) {
                VulkanCallMapper.vkDestroyDescriptorUpdateTemplate(vkDevice, handle);
            }
        }
        
        // Destroy external memory handles
        for (ExternalMemoryHandle handle : externalHandles.values()) {
            // Note: Don't close the external handles - they may be in use by other APIs
            VulkanCallMapper.vkDestroyBuffer(vkDevice, handle.vkBuffer);
            VulkanCallMapper.vkFreeMemory(vkDevice, handle.vkMemory);
        }
        externalHandles.clear();
        
        // Destroy YCbCr conversions
        for (Long conversion : ycbcrConversions.keySet()) {
            VulkanCallMapper.vkDestroySamplerYcbcrConversion(vkDevice, conversion);
        }
        ycbcrConversions.clear();
        
        // Destroy protected command pool
        if (protectedCommandPool != VK_NULL_HANDLE) {
            VulkanCallMapper.vkDestroyCommandPool(vkDevice, protectedCommandPool);
            protectedCommandPool = VK_NULL_HANDLE;
        }
        
        // Reset device group
        deviceGroup = null;
        multiGpuEnabled = false;
        
        // Call parent cleanup
        super.cleanup();
    }
    
    @Override
    public void shutdown() {
        cleanup();
        resetStats11();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DEBUG UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Print detailed capability report.
     */
    public void printCapabilityReport() {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║              VULKAN 1.1 CAPABILITY REPORT                      ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║ SUBGROUPS:                                                     ║");
        System.out.printf("║   Size: %-3d  Operations: %-40s║%n", 
            subgroupProperties.subgroupSize, subgroupProperties.getSupportedOperationsString());
        System.out.println("║                                                                ║");
        System.out.println("║ DEVICE GROUPS:                                                 ║");
        System.out.printf("║   Multi-GPU: %-5b  Device Count: %-3d                          ║%n", 
            multiGpuEnabled, deviceGroup != null ? deviceGroup.deviceCount : 1);
        System.out.println("║                                                                ║");
        System.out.println("║ 16-BIT STORAGE:                                                ║");
        System.out.printf("║   SSBO: %-5b  UBO: %-5b  Push: %-5b  IO: %-5b               ║%n",
            storage16BitFeatures.storageBuffer16BitAccess,
            storage16BitFeatures.uniformAndStorageBuffer16BitAccess,
            storage16BitFeatures.storagePushConstant16,
            storage16BitFeatures.storageInputOutput16);
        System.out.println("║                                                                ║");
        System.out.println("║ MULTIVIEW:                                                     ║");
        System.out.printf("║   Supported: %-5b  Max Views: %-3d  Geometry: %-5b            ║%n",
            multiviewCapabilities.multiview,
            multiviewCapabilities.maxMultiviewViewCount,
            multiviewCapabilities.multiviewGeometryShader);
        System.out.println("║                                                                ║");
        System.out.println("║ PROTECTED MEMORY:                                              ║");
        System.out.printf("║   Available: %-5b  No-Fault: %-5b                             ║%n",
            protectedMemoryCapabilities.protectedMemory,
            protectedMemoryCapabilities.protectedNoFault);
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
    }
}
