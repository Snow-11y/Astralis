package stellar.snow.astralis.api.vulkan.buffer;

import stellar.snow.astralis.api.opengl.buffer.BufferOps;
import stellar.snow.astralis.api.vulkan.mapping.VulkanCallMapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * VulkanBufferOps12 - Vulkan 1.2 Timeline Semaphores & Bindless Pipeline
 * 
 * ╔════════════════════════════════════════════════════════════════════════════════════════╗
 * ║     ★★★★★★★★★ VULKAN 1.2 - TIMELINE SEMAPHORES & BINDLESS REVOLUTION ★★★★★★★★★       ║
 * ║                                                                                        ║
 * ║  Vulkan 1.2 (January 2020) - THE ASYNC & BINDLESS ERA:                                 ║
 * ║                                                                                        ║
 * ║  ┌──────────────────────────────────────────────────────────────────────────────┐      ║
 * ║  │                      EVERYTHING FROM 1.1 PLUS:                               │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ TIMELINE SEMAPHORES                                                       │      ║
 * ║  │    • 64-bit monotonic counter semaphores                                     │      ║
 * ║  │    • Wait/signal specific values (not just binary)                           │      ║
 * ║  │    • CPU can wait on GPU progress without fences                             │      ║
 * ║  │    • GPU can wait on CPU signals                                             │      ║
 * ║  │    • Enables complex dependency graphs                                       │      ║
 * ║  │    • No more fence pools needed!                                             │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ BUFFER DEVICE ADDRESS (Bindless Buffers)                                  │      ║
 * ║  │    • Get GPU virtual address for any buffer                                  │      ║
 * ║  │    • Pass addresses to shaders via push constants                            │      ║
 * ║  │    • No descriptor binding needed for buffer access                          │      ║
 * ║  │    • Enables GPU-driven rendering                                            │      ║
 * ║  │    • Massive draw call reduction (1000s → 1)                                 │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ DESCRIPTOR INDEXING                                                       │      ║
 * ║  │    • Arrays of descriptors (textures, buffers)                               │      ║
 * ║  │    • Non-uniform indexing in shaders                                         │      ║
 * ║  │    • Update after bind (modify while in use)                                 │      ║
 * ║  │    • Partially bound descriptors                                             │      ║
 * ║  │    • Runtime-sized descriptor arrays                                         │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ SHADER FLOAT16/INT8                                                       │      ║
 * ║  │    • 8-bit integer shader operations                                         │      ║
 * ║  │    • 16-bit float shader operations                                          │      ║
 * ║  │    • 2-4x compute throughput for ML workloads                                │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ SCALAR BLOCK LAYOUT                                                       │      ║
 * ║  │    • C-style struct packing in shaders                                       │      ║
 * ║  │    • No more std140/std430 padding                                           │      ║
 * ║  │    • 20-40% memory savings for complex structs                               │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ SEPARATE DEPTH/STENCIL LAYOUTS                                            │      ║
 * ║  │    • Independent depth and stencil image layouts                             │      ║
 * ║  │    • Read depth while writing stencil (or vice versa)                        │      ║
 * ║  │    • Better depth buffer reuse                                               │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ HOST QUERY RESET                                                          │      ║
 * ║  │    • Reset query pools from CPU (no command buffer needed)                   │      ║
 * ║  │    • Simplifies occlusion/timestamp query usage                              │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ DRAW INDIRECT COUNT                                                       │      ║
 * ║  │    • GPU-specified draw count                                                │      ║
 * ║  │    • Perfect for GPU culling                                                 │      ║
 * ║  │    • Zero CPU involvement in draw submission                                 │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ IMAGELESS FRAMEBUFFERS                                                    │      ║
 * ║  │    • Framebuffers without image attachments                                  │      ║
 * ║  │    • Specify images at render pass begin                                     │      ║
 * ║  │    • Reduces framebuffer object count                                        │      ║
 * ║  └──────────────────────────────────────────────────────────────────────────────┘      ║
 * ║                                                                                        ║
 * ║  ┌──────────────────────────────────────────────────────────────────────────────┐      ║
 * ║  │                   TIMELINE SEMAPHORE ARCHITECTURE:                           │      ║
 * ║  │                                                                              │      ║
 * ║  │   ┌─────────────────────────────────────────────────────────────────────┐    │      ║
 * ║  │   │              TIMELINE SEMAPHORE (Value: 0 → ∞)                      │    │      ║
 * ║  │   │                                                                     │    │      ║
 * ║  │   │   Value:  0    1    2    3    4    5    6    7    8    9   ...      │    │      ║
 * ║  │   │           │    │    │    │    │    │    │    │    │    │            │    │      ║
 * ║  │   │           ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼    ▼            │    │      ║
 * ║  │   │          ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐ ┌──┐          │    │      ║
 * ║  │   │   GPU    │F0│→│F1│→│F2│→│F3│→│F4│→│F5│→│F6│→│F7│→│F8│→│F9│→ ...    │    │      ║
 * ║  │   │          └──┘ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘ └──┘          │    │      ║
 * ║  │   │                  ▲              ▲                   ▲               │    │      ║
 * ║  │   │                  │              │                   │               │    │      ║
 * ║  │   │   CPU      Wait(1)         Wait(4)             Wait(8)              │    │      ║
 * ║  │   │            (done!)         (done!)              (wait)              │    │      ║
 * ║  │   │                                                                     │    │      ║
 * ║  │   │   Current GPU Value: 6                                              │    │      ║
 * ║  │   │   • Wait(≤6) = immediate return                                     │    │      ║
 * ║  │   │   • Wait(>6) = block until GPU signals                              │    │      ║
 * ║  │   └─────────────────────────────────────────────────────────────────────┘    │      ║
 * ║  │                                                                              │      ║
 * ║  │   BENEFITS OVER BINARY SEMAPHORES + FENCES:                                  │      ║
 * ║  │   ✓ Single semaphore for entire frame sequence                               │      ║
 * ║  │   ✓ No fence pool management                                                 │      ║
 * ║  │   ✓ GPU-GPU dependencies without CPU involvement                             │      ║
 * ║  │   ✓ Fine-grained synchronization (per-operation, not per-frame)              │      ║
 * ║  │   ✓ CPU can query current value without blocking                             │      ║
 * ║  └──────────────────────────────────────────────────────────────────────────────┘      ║
 * ║                                                                                        ║
 * ║  ┌──────────────────────────────────────────────────────────────────────────────┐      ║
 * ║  │                   BUFFER DEVICE ADDRESS (BINDLESS):                          │      ║
 * ║  │                                                                              │      ║
 * ║  │   ┌─────────────────────────────────────────────────────────────────────┐    │      ║
 * ║  │   │                     GPU VIRTUAL ADDRESS SPACE                       │    │      ║
 * ║  │   │                                                                     │    │      ║
 * ║  │   │   0x0000_0000_0000_0000 ┌─────────────────────────────────────┐     │    │      ║
 * ║  │   │                        │         Buffer A                     │     │    │      ║
 * ║  │   │   0x0000_0000_0001_0000 │  Address: 0x0000_0000_0001_0000     │     │    │      ║
 * ║  │   │                        └─────────────────────────────────────┘     │    │      ║
 * ║  │   │                                                                     │    │      ║
 * ║  │   │   0x0000_0000_0002_0000 ┌─────────────────────────────────────┐     │    │      ║
 * ║  │   │                        │         Buffer B                     │     │    │      ║
 * ║  │   │                        │  Address: 0x0000_0000_0002_0000     │     │    │      ║
 * ║  │   │   0x0000_0000_0003_0000 └─────────────────────────────────────┘     │    │      ║
 * ║  │   │                                                                     │    │      ║
 * ║  │   │                              ...                                    │    │      ║
 * ║  │   │                                                                     │    │      ║
 * ║  │   │   SHADER ACCESS (no descriptors needed!):                           │    │      ║
 * ║  │   │   ─────────────────────────────────────                             │    │      ║
 * ║  │   │   layout(push_constant) uniform PushConstants {                     │    │      ║
 * ║  │   │       uint64_t vertexBufferAddress;                                 │    │      ║
 * ║  │   │       uint64_t indexBufferAddress;                                  │    │      ║
 * ║  │   │       uint64_t materialBufferAddress;                               │    │      ║
 * ║  │   │   };                                                                │    │      ║
 * ║  │   │                                                                     │    │      ║
 * ║  │   │   Vertex v = Vertex(vertexBufferAddress)[gl_VertexIndex];           │    │      ║
 * ║  │   │   Material m = Material(materialBufferAddress)[materialId];         │    │      ║
 * ║  │   └─────────────────────────────────────────────────────────────────────┘    │      ║
 * ║  └──────────────────────────────────────────────────────────────────────────────┘      ║
 * ║                                                                                        ║
 * ║  ┌──────────────────────────────────────────────────────────────────────────────┐      ║
 * ║  │                      DESCRIPTOR INDEXING:                                    │      ║
 * ║  │                                                                              │      ║
 * ║  │   BEFORE (Without Descriptor Indexing):                                      │      ║
 * ║  │   ┌─────────────────────────────────────────────────────────────────────┐    │      ║
 * ║  │   │  For each material:                                                 │    │      ║
 * ║  │   │    1. Bind descriptor set with that material's textures             │    │      ║
 * ║  │   │    2. Draw objects with that material                               │    │      ║
 * ║  │   │    3. Repeat... (thousands of bind calls!)                          │    │      ║
 * ║  │   └─────────────────────────────────────────────────────────────────────┘    │      ║
 * ║  │                                                                              │      ║
 * ║  │   AFTER (With Descriptor Indexing):                                          │      ║
 * ║  │   ┌─────────────────────────────────────────────────────────────────────┐    │      ║
 * ║  │   │  layout(set=0, binding=0) uniform sampler2D textures[];             │    │      ║
 * ║  │   │                                                                     │    │      ║
 * ║  │   │  1. Bind ONE descriptor set with ALL textures                       │    │      ║
 * ║  │   │  2. Shader: color = texture(textures[materialIndex], uv);           │    │      ║
 * ║  │   │  3. Draw EVERYTHING in one call!                                    │    │      ║
 * ║  │   └─────────────────────────────────────────────────────────────────────┘    │      ║
 * ║  └──────────────────────────────────────────────────────────────────────────────┘      ║
 * ║                                                                                        ║
 * ║  THIS PIPELINE INCLUDES:                                                               ║
 * ║  ✓ Full Vulkan 1.0/1.1 features                                                        ║
 * ║  ✓ Timeline semaphore management system                                                ║
 * ║  ✓ Buffer device address tracking                                                      ║
 * ║  ✓ Descriptor indexing support                                                         ║
 * ║  ✓ Scalar block layout buffers                                                         ║
 * ║  ✓ 8-bit/16-bit shader storage                                                         ║
 * ║  ✓ Host query reset                                                                    ║
 * ║  ✓ Draw indirect count buffers                                                         ║
 * ║  ✓ Async transfer with timeline sync                                                   ║
 * ║  ✓ GPU-driven rendering support                                                        ║
 * ║                                                                                        ║
 * ║  PERFORMANCE VS VULKAN 1.1:                                                            ║
 * ║  • 10-15% from better synchronization (timeline semaphores)                            ║
 * ║  • 50-90% draw call reduction (bindless + descriptor indexing)                         ║
 * ║  • 20-40% memory savings (scalar layout)                                               ║
 * ║  • 2-4x compute throughput for ML (fp16/int8)                                          ║
 * ╚════════════════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ Snowium Render: Vulkan 1.2 ★ TIMELINE SEMAPHORES & BINDLESS ★       │
 * │ Color: #4CAF50 (Green - The Future)                                 │
 * └─────────────────────────────────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public class VulkanBufferOps12 extends VulkanBufferOps11 {

    // ═══════════════════════════════════════════════════════════════════════════════
    // VERSION CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VERSION_MAJOR_12 = 1;
    public static final int VERSION_MINOR_12 = 2;
    public static final int VERSION_PATCH_12 = 0;
    public static final int VERSION_CODE_12 = VK_MAKE_API_VERSION(0, 1, 2, 0);
    public static final int DISPLAY_COLOR_12 = 0x4CAF50; // Material Green
    public static final String VERSION_NAME_12 = "Vulkan 1.2";
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VULKAN 1.2 STRUCTURE TYPES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES = 51;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_PROPERTIES = 52;
    public static final int VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO = 1000207002;
    public static final int VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO = 1000207003;
    public static final int VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO = 1000207004;
    public static final int VK_STRUCTURE_TYPE_SEMAPHORE_SIGNAL_INFO = 1000207005;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TIMELINE_SEMAPHORE_FEATURES = 1000207000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TIMELINE_SEMAPHORE_PROPERTIES = 1000207001;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_BUFFER_DEVICE_ADDRESS_FEATURES = 1000257000;
    public static final int VK_STRUCTURE_TYPE_BUFFER_DEVICE_ADDRESS_INFO = 1000244001;
    public static final int VK_STRUCTURE_TYPE_BUFFER_OPAQUE_CAPTURE_ADDRESS_CREATE_INFO = 1000257002;
    public static final int VK_STRUCTURE_TYPE_MEMORY_OPAQUE_CAPTURE_ADDRESS_ALLOCATE_INFO = 1000257003;
    public static final int VK_STRUCTURE_TYPE_DEVICE_MEMORY_OPAQUE_CAPTURE_ADDRESS_INFO = 1000257004;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES = 1000161001;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_PROPERTIES = 1000161002;
    public static final int VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO = 1000161000;
    public static final int VK_STRUCTURE_TYPE_DESCRIPTOR_SET_VARIABLE_DESCRIPTOR_COUNT_ALLOCATE_INFO = 1000161003;
    public static final int VK_STRUCTURE_TYPE_DESCRIPTOR_SET_VARIABLE_DESCRIPTOR_COUNT_LAYOUT_SUPPORT = 1000161004;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SCALAR_BLOCK_LAYOUT_FEATURES = 1000221000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_8BIT_STORAGE_FEATURES = 1000177000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_FLOAT16_INT8_FEATURES = 1000082000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_HOST_QUERY_RESET_FEATURES = 1000261000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_IMAGELESS_FRAMEBUFFER_FEATURES = 1000108000;
    public static final int VK_STRUCTURE_TYPE_FRAMEBUFFER_ATTACHMENTS_CREATE_INFO = 1000108001;
    public static final int VK_STRUCTURE_TYPE_FRAMEBUFFER_ATTACHMENT_IMAGE_INFO = 1000108002;
    public static final int VK_STRUCTURE_TYPE_RENDER_PASS_ATTACHMENT_BEGIN_INFO = 1000108003;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SEPARATE_DEPTH_STENCIL_LAYOUTS_FEATURES = 1000241000;
    public static final int VK_STRUCTURE_TYPE_ATTACHMENT_REFERENCE_STENCIL_LAYOUT = 1000241001;
    public static final int VK_STRUCTURE_TYPE_ATTACHMENT_DESCRIPTION_STENCIL_LAYOUT = 1000241002;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_SUBGROUP_EXTENDED_TYPES_FEATURES = 1000175000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_UNIFORM_BUFFER_STANDARD_LAYOUT_FEATURES = 1000253000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_MEMORY_MODEL_FEATURES = 1000211000;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SEMAPHORE TYPES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_SEMAPHORE_TYPE_BINARY = 0;
    public static final int VK_SEMAPHORE_TYPE_TIMELINE = 1;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SEMAPHORE WAIT FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_SEMAPHORE_WAIT_ANY_BIT = 0x00000001;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BUFFER USAGE FLAGS (1.2 additions)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT = 0x00020000;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BUFFER CREATE FLAGS (1.2 additions)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_BUFFER_CREATE_DEVICE_ADDRESS_CAPTURE_REPLAY_BIT = 0x00000010;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // MEMORY ALLOCATE FLAGS (1.2 additions)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT = 0x00000002;
    public static final int VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_CAPTURE_REPLAY_BIT = 0x00000004;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DESCRIPTOR BINDING FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT = 0x00000001;
    public static final int VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT = 0x00000002;
    public static final int VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT = 0x00000004;
    public static final int VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT = 0x00000008;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DESCRIPTOR SET LAYOUT CREATE FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT = 0x00000002;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DESCRIPTOR POOL CREATE FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT = 0x00000002;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // FRAMEBUFFER CREATE FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_FRAMEBUFFER_CREATE_IMAGELESS_BIT = 0x00000001;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DRIVER IDS (for identifying GPU vendor)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_DRIVER_ID_AMD_PROPRIETARY = 1;
    public static final int VK_DRIVER_ID_AMD_OPEN_SOURCE = 2;
    public static final int VK_DRIVER_ID_MESA_RADV = 3;
    public static final int VK_DRIVER_ID_NVIDIA_PROPRIETARY = 4;
    public static final int VK_DRIVER_ID_INTEL_PROPRIETARY_WINDOWS = 5;
    public static final int VK_DRIVER_ID_INTEL_OPEN_SOURCE_MESA = 6;
    public static final int VK_DRIVER_ID_IMAGINATION_PROPRIETARY = 7;
    public static final int VK_DRIVER_ID_QUALCOMM_PROPRIETARY = 8;
    public static final int VK_DRIVER_ID_ARM_PROPRIETARY = 9;
    public static final int VK_DRIVER_ID_GOOGLE_SWIFTSHADER = 10;
    public static final int VK_DRIVER_ID_GGP_PROPRIETARY = 11;
    public static final int VK_DRIVER_ID_BROADCOM_PROPRIETARY = 12;
    public static final int VK_DRIVER_ID_MESA_LLVMPIPE = 13;
    public static final int VK_DRIVER_ID_MOLTENVK = 14;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // RESOLVE MODE FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_RESOLVE_MODE_NONE = 0;
    public static final int VK_RESOLVE_MODE_SAMPLE_ZERO_BIT = 0x00000001;
    public static final int VK_RESOLVE_MODE_AVERAGE_BIT = 0x00000002;
    public static final int VK_RESOLVE_MODE_MIN_BIT = 0x00000004;
    public static final int VK_RESOLVE_MODE_MAX_BIT = 0x00000008;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SAMPLER REDUCTION MODE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_SAMPLER_REDUCTION_MODE_WEIGHTED_AVERAGE = 0;
    public static final int VK_SAMPLER_REDUCTION_MODE_MIN = 1;
    public static final int VK_SAMPLER_REDUCTION_MODE_MAX = 2;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SHADER FLOAT CONTROLS INDEPENDENCE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_SHADER_FLOAT_CONTROLS_INDEPENDENCE_32_BIT_ONLY = 0;
    public static final int VK_SHADER_FLOAT_CONTROLS_INDEPENDENCE_ALL = 1;
    public static final int VK_SHADER_FLOAT_CONTROLS_INDEPENDENCE_NONE = 2;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONFIGURATION CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    protected static final int MAX_TIMELINE_SEMAPHORES = 16;
    protected static final int BUFFER_ADDRESS_CACHE_SIZE = 1024;
    protected static final int MAX_DESCRIPTOR_INDEXING_SAMPLERS = 500000;
    protected static final int MAX_DESCRIPTOR_INDEXING_UNIFORM_BUFFERS = 500000;
    protected static final int MAX_DESCRIPTOR_INDEXING_STORAGE_BUFFERS = 500000;
    protected static final long TIMELINE_SEMAPHORE_MAX_DIFFERENCE = 0x7FFFFFFFL;
    
    // Pre-composed usage patterns with device address
    public static final int USAGE_BINDLESS_VERTEX = 
        VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | 
        VK_BUFFER_USAGE_TRANSFER_DST_BIT | 
        VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;
    
    public static final int USAGE_BINDLESS_INDEX = 
        VK_BUFFER_USAGE_INDEX_BUFFER_BIT | 
        VK_BUFFER_USAGE_TRANSFER_DST_BIT | 
        VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;
    
    public static final int USAGE_BINDLESS_STORAGE = 
        VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | 
        VK_BUFFER_USAGE_TRANSFER_DST_BIT | 
        VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;
    
    public static final int USAGE_INDIRECT_COUNT = 
        VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT | 
        VK_BUFFER_USAGE_TRANSFER_DST_BIT | 
        VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TIMELINE SEMAPHORE SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Timeline semaphore wrapper with tracking.
     */
    public static final class TimelineSemaphore {
        public final long handle;
        public final String name;
        private final AtomicLong currentValue;
        private final AtomicLong pendingValue;
        
        // Statistics
        private final AtomicLong signalCount = new AtomicLong();
        private final AtomicLong waitCount = new AtomicLong();
        private final AtomicLong cpuWaitTimeNs = new AtomicLong();
        
        public TimelineSemaphore(long handle, String name, long initialValue) {
            this.handle = handle;
            this.name = name;
            this.currentValue = new AtomicLong(initialValue);
            this.pendingValue = new AtomicLong(initialValue);
        }
        
        /**
         * Get the last known signaled value.
         */
        public long getCurrentValue() {
            return currentValue.get();
        }
        
        /**
         * Get the highest pending signal value.
         */
        public long getPendingValue() {
            return pendingValue.get();
        }
        
        /**
         * Reserve the next value for signaling.
         */
        public long reserveNextValue() {
            return pendingValue.incrementAndGet();
        }
        
        /**
         * Reserve multiple values.
         */
        public long reserveValues(int count) {
            return pendingValue.addAndGet(count);
        }
        
        /**
         * Update current value (after query or wait).
         */
        public void updateCurrentValue(long value) {
            long current;
            do {
                current = currentValue.get();
                if (value <= current) return;
            } while (!currentValue.compareAndSet(current, value));
        }
        
        public void recordSignal() { signalCount.incrementAndGet(); }
        public void recordWait(long waitTimeNs) { 
            waitCount.incrementAndGet(); 
            cpuWaitTimeNs.addAndGet(waitTimeNs);
        }
        
        public long getSignalCount() { return signalCount.get(); }
        public long getWaitCount() { return waitCount.get(); }
        public long getTotalCpuWaitTimeNs() { return cpuWaitTimeNs.get(); }
        
        @Override
        public String toString() {
            return String.format("TimelineSemaphore[%s, current=%d, pending=%d]",
                name, currentValue.get(), pendingValue.get());
        }
    }
    
    protected final ConcurrentHashMap<Long, TimelineSemaphore> timelineSemaphores = 
            new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, TimelineSemaphore> namedSemaphores = 
            new ConcurrentHashMap<>();
    
    // Default timeline semaphores for common operations
    protected TimelineSemaphore transferSemaphore;
    protected TimelineSemaphore graphicsSemaphore;
    protected TimelineSemaphore computeSemaphore;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BUFFER DEVICE ADDRESS SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Buffer with device address for bindless access.
     */
    public static class BindlessBuffer extends VulkanBuffer {
        /** GPU virtual address */
        public final long deviceAddress;
        
        /** Capture address for replay (if enabled) */
        public final long captureAddress;
        
        public BindlessBuffer(long handle, long memory, long size, int usage, int memProps,
                              long deviceAddress, long captureAddress) {
            super(handle, memory, size, usage, memProps);
            this.deviceAddress = deviceAddress;
            this.captureAddress = captureAddress;
        }
        
        public boolean hasDeviceAddress() {
            return deviceAddress != 0;
        }
    }
    
    /** Cache of buffer addresses for fast lookup */
    protected final ConcurrentHashMap<Long, Long> bufferAddressCache = new ConcurrentHashMap<>();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DESCRIPTOR INDEXING SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Descriptor indexing features and limits.
     */
    public static final class DescriptorIndexingFeatures {
        // Per-stage limits
        public int maxPerStageDescriptorUpdateAfterBindSamplers;
        public int maxPerStageDescriptorUpdateAfterBindUniformBuffers;
        public int maxPerStageDescriptorUpdateAfterBindStorageBuffers;
        public int maxPerStageDescriptorUpdateAfterBindSampledImages;
        public int maxPerStageDescriptorUpdateAfterBindStorageImages;
        public int maxPerStageDescriptorUpdateAfterBindInputAttachments;
        public int maxPerStageUpdateAfterBindResources;
        
        // Total limits
        public int maxDescriptorSetUpdateAfterBindSamplers;
        public int maxDescriptorSetUpdateAfterBindUniformBuffers;
        public int maxDescriptorSetUpdateAfterBindUniformBuffersDynamic;
        public int maxDescriptorSetUpdateAfterBindStorageBuffers;
        public int maxDescriptorSetUpdateAfterBindStorageBuffersDynamic;
        public int maxDescriptorSetUpdateAfterBindSampledImages;
        public int maxDescriptorSetUpdateAfterBindStorageImages;
        public int maxDescriptorSetUpdateAfterBindInputAttachments;
        
        // Feature flags
        public boolean shaderInputAttachmentArrayDynamicIndexing;
        public boolean shaderUniformTexelBufferArrayDynamicIndexing;
        public boolean shaderStorageTexelBufferArrayDynamicIndexing;
        public boolean shaderUniformBufferArrayNonUniformIndexing;
        public boolean shaderSampledImageArrayNonUniformIndexing;
        public boolean shaderStorageBufferArrayNonUniformIndexing;
        public boolean shaderStorageImageArrayNonUniformIndexing;
        public boolean shaderInputAttachmentArrayNonUniformIndexing;
        public boolean shaderUniformTexelBufferArrayNonUniformIndexing;
        public boolean shaderStorageTexelBufferArrayNonUniformIndexing;
        public boolean descriptorBindingUniformBufferUpdateAfterBind;
        public boolean descriptorBindingSampledImageUpdateAfterBind;
        public boolean descriptorBindingStorageImageUpdateAfterBind;
        public boolean descriptorBindingStorageBufferUpdateAfterBind;
        public boolean descriptorBindingUniformTexelBufferUpdateAfterBind;
        public boolean descriptorBindingStorageTexelBufferUpdateAfterBind;
        public boolean descriptorBindingUpdateUnusedWhilePending;
        public boolean descriptorBindingPartiallyBound;
        public boolean descriptorBindingVariableDescriptorCount;
        public boolean runtimeDescriptorArray;
        
        public boolean supportsBindlessTextures() {
            return shaderSampledImageArrayNonUniformIndexing && 
                   descriptorBindingSampledImageUpdateAfterBind &&
                   descriptorBindingPartiallyBound;
        }
        
        public boolean supportsBindlessBuffers() {
            return shaderStorageBufferArrayNonUniformIndexing &&
                   descriptorBindingStorageBufferUpdateAfterBind &&
                   descriptorBindingPartiallyBound;
        }
        
        @Override
        public String toString() {
            return String.format("DescriptorIndexing[bindlessTextures=%b, bindlessBuffers=%b, " +
                "maxSamplers=%d, maxStorageBuffers=%d]",
                supportsBindlessTextures(), supportsBindlessBuffers(),
                maxDescriptorSetUpdateAfterBindSamplers,
                maxDescriptorSetUpdateAfterBindStorageBuffers);
        }
    }
    
    protected final DescriptorIndexingFeatures descriptorIndexingFeatures = 
            new DescriptorIndexingFeatures();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VULKAN 1.2 FEATURES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Vulkan 1.2 feature set.
     */
    public static final class Vulkan12Features {
        // Timeline Semaphores
        public boolean timelineSemaphore;
        public long maxTimelineSemaphoreValueDifference;
        
        // Buffer Device Address
        public boolean bufferDeviceAddress;
        public boolean bufferDeviceAddressCaptureReplay;
        public boolean bufferDeviceAddressMultiDevice;
        
        // Scalar Block Layout
        public boolean scalarBlockLayout;
        
        // 8-bit Storage
        public boolean storageBuffer8BitAccess;
        public boolean uniformAndStorageBuffer8BitAccess;
        public boolean storagePushConstant8;
        
        // Float16/Int8
        public boolean shaderFloat16;
        public boolean shaderInt8;
        
        // Host Query Reset
        public boolean hostQueryReset;
        
        // Draw Indirect Count
        public boolean drawIndirectCount;
        
        // Imageless Framebuffer
        public boolean imagelessFramebuffer;
        
        // Separate Depth/Stencil
        public boolean separateDepthStencilLayouts;
        
        // Uniform Buffer Standard Layout
        public boolean uniformBufferStandardLayout;
        
        // Subgroup Extended Types
        public boolean shaderSubgroupExtendedTypes;
        
        // Vulkan Memory Model
        public boolean vulkanMemoryModel;
        public boolean vulkanMemoryModelDeviceScope;
        public boolean vulkanMemoryModelAvailabilityVisibilityChains;
        
        // Sampler Filter Minmax
        public boolean samplerFilterMinmax;
        public boolean filterMinmaxSingleComponentFormats;
        public boolean filterMinmaxImageComponentMapping;
        
        // Sampler Mirror Clamp To Edge
        public boolean samplerMirrorClampToEdge;
        
        // Shader Output
        public boolean shaderOutputViewportIndex;
        public boolean shaderOutputLayer;
        
        // Depth Clip
        public boolean shaderDemoteToHelperInvocation;
        
        // Driver properties
        public int driverID;
        public String driverName;
        public String driverInfo;
        public int conformanceVersion;
        
        @Override
        public String toString() {
            return String.format("Vulkan12Features[timeline=%b, BDA=%b, scalar=%b, " +
                "fp16=%b, int8=%b, drawIndirectCount=%b]",
                timelineSemaphore, bufferDeviceAddress, scalarBlockLayout,
                shaderFloat16, shaderInt8, drawIndirectCount);
        }
    }
    
    protected final Vulkan12Features vulkan12Features = new Vulkan12Features();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // 8-BIT STORAGE BUFFER
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Buffer optimized for 8-bit (byte) storage.
     */
    public static class Int8Buffer extends VulkanBuffer {
        public final long elementCount;
        
        public Int8Buffer(long handle, long memory, long size, int usage, int memProps,
                          long elementCount) {
            super(handle, memory, size, usage, memProps);
            this.elementCount = elementCount;
        }
        
        /** Write byte array to buffer */
        public void writeBytes(byte[] data, long offset) {
            if (persistentMap == null) {
                throw new IllegalStateException("Buffer not persistently mapped");
            }
            persistentMap.position((int) offset);
            persistentMap.put(data);
            persistentMap.clear();
        }
        
        /** Read bytes from buffer */
        public void readBytes(byte[] data, long offset) {
            if (persistentMap == null) {
                throw new IllegalStateException("Buffer not persistently mapped");
            }
            persistentMap.position((int) offset);
            persistentMap.get(data);
            persistentMap.clear();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DRAW INDIRECT COUNT BUFFER
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Buffer for GPU-driven draw indirect with count.
     */
    public static class DrawIndirectCountBuffer extends VulkanBuffer {
        /** Draw command buffer */
        public final long drawCommandBuffer;
        
        /** Count buffer (single uint32) */
        public final long countBuffer;
        
        /** Maximum draw commands */
        public final int maxDrawCount;
        
        /** Stride between draw commands */
        public final int stride;
        
        /** Whether this is indexed draw */
        public final boolean indexed;
        
        public DrawIndirectCountBuffer(long handle, long memory, long size, int usage, int memProps,
                                       long drawCmdBuffer, long countBuffer, int maxDrawCount, 
                                       int stride, boolean indexed) {
            super(handle, memory, size, usage, memProps);
            this.drawCommandBuffer = drawCmdBuffer;
            this.countBuffer = countBuffer;
            this.maxDrawCount = maxDrawCount;
            this.stride = stride;
            this.indexed = indexed;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // QUERY POOL FOR HOST RESET
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Query pool with host reset support.
     */
    public static final class HostResetQueryPool {
        public final long handle;
        public final int queryType;
        public final int queryCount;
        public final int pipelineStatistics; // For pipeline statistics queries
        
        // Query state tracking
        private final boolean[] queryActive;
        private final long[] queryResults;
        
        public HostResetQueryPool(long handle, int type, int count, int stats) {
            this.handle = handle;
            this.queryType = type;
            this.queryCount = count;
            this.pipelineStatistics = stats;
            this.queryActive = new boolean[count];
            this.queryResults = new long[count];
        }
        
        public void markActive(int queryIndex) {
            queryActive[queryIndex] = true;
        }
        
        public void markReset(int firstQuery, int queryCount) {
            for (int i = firstQuery; i < firstQuery + queryCount && i < this.queryCount; i++) {
                queryActive[i] = false;
                queryResults[i] = 0;
            }
        }
        
        public boolean isActive(int queryIndex) {
            return queryActive[queryIndex];
        }
        
        public void setResult(int queryIndex, long result) {
            queryResults[queryIndex] = result;
        }
        
        public long getResult(int queryIndex) {
            return queryResults[queryIndex];
        }
    }
    
    protected final ConcurrentHashMap<Long, HostResetQueryPool> queryPools = new ConcurrentHashMap<>();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATISTICS (Vulkan 1.2 Specific)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    protected static final AtomicLong STAT_TIMELINE_SIGNALS = new AtomicLong();
    protected static final AtomicLong STAT_TIMELINE_WAITS = new AtomicLong();
    protected static final AtomicLong STAT_TIMELINE_CPU_WAIT_NS = new AtomicLong();
    protected static final AtomicLong STAT_BINDLESS_BUFFERS = new AtomicLong();
    protected static final AtomicLong STAT_DEVICE_ADDRESS_QUERIES = new AtomicLong();
    protected static final AtomicLong STAT_DRAW_INDIRECT_COUNT_CALLS = new AtomicLong();
    protected static final AtomicLong STAT_HOST_QUERY_RESETS = new AtomicLong();
    protected static final AtomicLong STAT_8BIT_BUFFERS = new AtomicLong();
    protected static final AtomicLong STAT_SCALAR_LAYOUT_BUFFERS = new AtomicLong();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public VulkanBufferOps12() {
        super();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Override
    public boolean initialize(boolean enableValidation) {
        // Initialize Vulkan 1.0/1.1 base
        if (!super.initialize(enableValidation)) {
            return false;
        }
        
        try {
            // Query Vulkan 1.2 features
            queryVulkan12Features();
            queryDescriptorIndexingFeatures();
            
            // Initialize timeline semaphores if supported
            if (vulkan12Features.timelineSemaphore) {
                initializeTimelineSemaphores();
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to initialize Vulkan 1.2 features: " + e.getMessage());
            return true; // 1.1 features still available
        }
    }
    
    /**
     * Query all Vulkan 1.2 features and properties.
     */
    protected void queryVulkan12Features() {
        /*
         * VkPhysicalDeviceVulkan12Features features12 = VkPhysicalDeviceVulkan12Features.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);
         * 
         * VkPhysicalDeviceVulkan12Properties props12 = VkPhysicalDeviceVulkan12Properties.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_PROPERTIES);
         * 
         * VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
         *     .pNext(features12);
         * 
         * VkPhysicalDeviceProperties2 props2 = VkPhysicalDeviceProperties2.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2)
         *     .pNext(props12);
         * 
         * vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, features2);
         * vkGetPhysicalDeviceProperties2(vkPhysicalDevice, props2);
         */
        
        VulkanCallMapper.vkGetVulkan12Features(vkPhysicalDevice, vulkan12Features);
        
        System.out.println("[Vulkan 1.2] " + vulkan12Features);
        
        if (vulkan12Features.driverName != null) {
            System.out.println("[Vulkan 1.2] Driver: " + vulkan12Features.driverName + 
                " (" + vulkan12Features.driverInfo + ")");
        }
    }
    
    /**
     * Query descriptor indexing features and limits.
     */
    protected void queryDescriptorIndexingFeatures() {
        /*
         * VkPhysicalDeviceDescriptorIndexingFeatures indexingFeatures = 
         *     VkPhysicalDeviceDescriptorIndexingFeatures.calloc()
         *         .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES);
         * 
         * VkPhysicalDeviceDescriptorIndexingProperties indexingProps = 
         *     VkPhysicalDeviceDescriptorIndexingProperties.calloc()
         *         .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_PROPERTIES);
         */
        
        VulkanCallMapper.vkGetDescriptorIndexingFeatures(vkPhysicalDevice, descriptorIndexingFeatures);
        
        System.out.println("[Vulkan 1.2] " + descriptorIndexingFeatures);
    }
    
    /**
     * Initialize default timeline semaphores.
     */
    protected void initializeTimelineSemaphores() {
        transferSemaphore = createTimelineSemaphore("transfer", 0);
        graphicsSemaphore = createTimelineSemaphore("graphics", 0);
        computeSemaphore = createTimelineSemaphore("compute", 0);
        
        System.out.println("[Vulkan 1.2] Timeline semaphores initialized");
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TIMELINE SEMAPHORE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a timeline semaphore with initial value.
     */
    public TimelineSemaphore createTimelineSemaphore(String name, long initialValue) {
        if (!vulkan12Features.timelineSemaphore) {
            throw new UnsupportedOperationException("Timeline semaphores not supported");
        }
        
        /*
         * VkSemaphoreTypeCreateInfo typeInfo = VkSemaphoreTypeCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO)
         *     .semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE)
         *     .initialValue(initialValue);
         * 
         * VkSemaphoreCreateInfo createInfo = VkSemaphoreCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
         *     .pNext(typeInfo);
         * 
         * LongBuffer pSemaphore = stack.mallocLong(1);
         * vkCreateSemaphore(vkDevice, createInfo, null, pSemaphore);
         * return pSemaphore.get(0);
         */
        
        long handle = VulkanCallMapper.vkCreateTimelineSemaphore(vkDevice, initialValue);
        if (handle == VK_NULL_HANDLE) {
            throw new RuntimeException("Failed to create timeline semaphore");
        }
        
        TimelineSemaphore semaphore = new TimelineSemaphore(handle, name, initialValue);
        timelineSemaphores.put(handle, semaphore);
        namedSemaphores.put(name, semaphore);
        
        return semaphore;
    }
    
    /**
     * Get current value of a timeline semaphore (non-blocking).
     */
    public long getTimelineSemaphoreValue(TimelineSemaphore semaphore) {
        /*
         * LongBuffer pValue = stack.mallocLong(1);
         * vkGetSemaphoreCounterValue(vkDevice, semaphore.handle, pValue);
         * return pValue.get(0);
         */
        
        long value = VulkanCallMapper.vkGetSemaphoreCounterValue(vkDevice, semaphore.handle);
        semaphore.updateCurrentValue(value);
        return value;
    }
    
    /**
     * CPU wait for timeline semaphore to reach a value.
     */
    public boolean waitTimelineSemaphore(TimelineSemaphore semaphore, long value, long timeoutNs) {
        if (semaphore.getCurrentValue() >= value) {
            return true; // Already reached
        }
        
        /*
         * VkSemaphoreWaitInfo waitInfo = VkSemaphoreWaitInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
         *     .semaphoreCount(1)
         *     .pSemaphores(stack.longs(semaphore.handle))
         *     .pValues(stack.longs(value));
         * 
         * int result = vkWaitSemaphores(vkDevice, waitInfo, timeoutNs);
         * return result == VK_SUCCESS;
         */
        
        long startNs = System.nanoTime();
        int result = VulkanCallMapper.vkWaitSemaphores(vkDevice, semaphore.handle, value, timeoutNs);
        long waitTimeNs = System.nanoTime() - startNs;
        
        semaphore.recordWait(waitTimeNs);
        STAT_TIMELINE_WAITS.incrementAndGet();
        STAT_TIMELINE_CPU_WAIT_NS.addAndGet(waitTimeNs);
        
        if (result == VK_SUCCESS) {
            semaphore.updateCurrentValue(value);
            return true;
        }
        
        return false;
    }
    
    /**
     * Wait for multiple timeline semaphores.
     */
    public boolean waitTimelineSemaphores(TimelineSemaphore[] semaphores, long[] values, 
                                          boolean waitAll, long timeoutNs) {
        /*
         * LongBuffer semaphoreHandles = stack.mallocLong(semaphores.length);
         * LongBuffer semaphoreValues = stack.mallocLong(values.length);
         * 
         * for (int i = 0; i < semaphores.length; i++) {
         *     semaphoreHandles.put(i, semaphores[i].handle);
         *     semaphoreValues.put(i, values[i]);
         * }
         * 
         * VkSemaphoreWaitInfo waitInfo = VkSemaphoreWaitInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
         *     .flags(waitAll ? 0 : VK_SEMAPHORE_WAIT_ANY_BIT)
         *     .semaphoreCount(semaphores.length)
         *     .pSemaphores(semaphoreHandles)
         *     .pValues(semaphoreValues);
         * 
         * return vkWaitSemaphores(vkDevice, waitInfo, timeoutNs) == VK_SUCCESS;
         */
        
        long[] handles = new long[semaphores.length];
        for (int i = 0; i < semaphores.length; i++) {
            handles[i] = semaphores[i].handle;
        }
        
        long startNs = System.nanoTime();
        int result = VulkanCallMapper.vkWaitSemaphoresMultiple(
            vkDevice, handles, values, waitAll, timeoutNs);
        long waitTimeNs = System.nanoTime() - startNs;
        
        for (TimelineSemaphore sem : semaphores) {
            sem.recordWait(waitTimeNs / semaphores.length);
        }
        STAT_TIMELINE_WAITS.addAndGet(semaphores.length);
        STAT_TIMELINE_CPU_WAIT_NS.addAndGet(waitTimeNs);
        
        return result == VK_SUCCESS;
    }
    
    /**
     * CPU signal a timeline semaphore to a value.
     */
    public void signalTimelineSemaphore(TimelineSemaphore semaphore, long value) {
        /*
         * VkSemaphoreSignalInfo signalInfo = VkSemaphoreSignalInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SIGNAL_INFO)
         *     .semaphore(semaphore.handle)
         *     .value(value);
         * 
         * vkSignalSemaphore(vkDevice, signalInfo);
         */
        
        VulkanCallMapper.vkSignalSemaphore(vkDevice, semaphore.handle, value);
        semaphore.updateCurrentValue(value);
        semaphore.recordSignal();
        STAT_TIMELINE_SIGNALS.incrementAndGet();
    }
    
    /**
     * Submit command buffer with timeline semaphore wait/signal.
     */
    public void submitWithTimeline(long queue, long commandBuffer,
                                   TimelineSemaphore waitSemaphore, long waitValue,
                                   TimelineSemaphore signalSemaphore, long signalValue) {
        /*
         * VkTimelineSemaphoreSubmitInfo timelineInfo = VkTimelineSemaphoreSubmitInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO)
         *     .waitSemaphoreValueCount(waitSemaphore != null ? 1 : 0)
         *     .pWaitSemaphoreValues(waitSemaphore != null ? stack.longs(waitValue) : null)
         *     .signalSemaphoreValueCount(signalSemaphore != null ? 1 : 0)
         *     .pSignalSemaphoreValues(signalSemaphore != null ? stack.longs(signalValue) : null);
         * 
         * VkSubmitInfo submitInfo = VkSubmitInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
         *     .pNext(timelineInfo)
         *     .waitSemaphoreCount(waitSemaphore != null ? 1 : 0)
         *     .pWaitSemaphores(waitSemaphore != null ? stack.longs(waitSemaphore.handle) : null)
         *     .pWaitDstStageMask(waitSemaphore != null ? stack.ints(VK_PIPELINE_STAGE_ALL_COMMANDS_BIT) : null)
         *     .commandBufferCount(1)
         *     .pCommandBuffers(stack.pointers(commandBuffer))
         *     .signalSemaphoreCount(signalSemaphore != null ? 1 : 0)
         *     .pSignalSemaphores(signalSemaphore != null ? stack.longs(signalSemaphore.handle) : null);
         * 
         * vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE);
         */
        
        VulkanCallMapper.vkQueueSubmitWithTimeline(
            queue, commandBuffer,
            waitSemaphore != null ? waitSemaphore.handle : VK_NULL_HANDLE, waitValue,
            signalSemaphore != null ? signalSemaphore.handle : VK_NULL_HANDLE, signalValue);
        
        if (signalSemaphore != null) {
            signalSemaphore.recordSignal();
            STAT_TIMELINE_SIGNALS.incrementAndGet();
        }
    }
    
    /**
     * Destroy a timeline semaphore.
     */
    public void destroyTimelineSemaphore(TimelineSemaphore semaphore) {
        timelineSemaphores.remove(semaphore.handle);
        namedSemaphores.remove(semaphore.name);
        VulkanCallMapper.vkDestroySemaphore(vkDevice, semaphore.handle);
    }
    
    /**
     * Get named timeline semaphore.
     */
    public TimelineSemaphore getTimelineSemaphore(String name) {
        return namedSemaphores.get(name);
    }
    
    /**
     * Get transfer timeline semaphore for staging operations.
     */
    public TimelineSemaphore getTransferSemaphore() {
        return transferSemaphore;
    }
    
    /**
     * Get graphics timeline semaphore.
     */
    public TimelineSemaphore getGraphicsSemaphore() {
        return graphicsSemaphore;
    }
    
    /**
     * Get compute timeline semaphore.
     */
    public TimelineSemaphore getComputeSemaphore() {
        return computeSemaphore;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BUFFER DEVICE ADDRESS (BINDLESS)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Create a buffer with device address for bindless access.
     */
    public int createBindlessBuffer(long size, int usage) {
        if (!vulkan12Features.bufferDeviceAddress) {
            throw new UnsupportedOperationException("Buffer device address not supported");
        }
        
        // Add device address usage flag
        usage |= VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;
        
        /*
         * VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
         *     .size(size)
         *     .usage(usage)
         *     .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
         * 
         * long bufferHandle = vkCreateBuffer(vkDevice, bufferInfo);
         * 
         * VkMemoryRequirements memReqs = VkMemoryRequirements.malloc();
         * vkGetBufferMemoryRequirements(vkDevice, bufferHandle, memReqs);
         * 
         * // Allocate with device address flag
         * VkMemoryAllocateFlagsInfo flagsInfo = VkMemoryAllocateFlagsInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_FLAGS_INFO)
         *     .flags(VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT);
         * 
         * VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
         *     .pNext(flagsInfo)
         *     .allocationSize(memReqs.size())
         *     .memoryTypeIndex(memTypeIndex);
         * 
         * long memoryHandle = vkAllocateMemory(vkDevice, allocInfo);
         * vkBindBufferMemory(vkDevice, bufferHandle, memoryHandle, 0);
         * 
         * // Get device address
         * VkBufferDeviceAddressInfo addressInfo = VkBufferDeviceAddressInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_BUFFER_DEVICE_ADDRESS_INFO)
         *     .buffer(bufferHandle);
         * 
         * long deviceAddress = vkGetBufferDeviceAddress(vkDevice, addressInfo);
         */
        
        long bufferHandle = VulkanCallMapper.vkCreateBuffer(vkDevice, size, usage);
        if (bufferHandle == VK_NULL_HANDLE) return 0;
        
        long memReqSize = VulkanCallMapper.vkGetBufferMemoryRequirements(vkDevice, bufferHandle);
        int memTypeIndex = findMemoryType(MEMORY_DEVICE_LOCAL);
        
        // Allocate with device address flag
        long memoryHandle = VulkanCallMapper.vkAllocateMemoryWithDeviceAddress(
            vkDevice, memReqSize, memTypeIndex);
        
        if (memoryHandle == VK_NULL_HANDLE) {
            VulkanCallMapper.vkDestroyBuffer(vkDevice, bufferHandle);
            return 0;
        }
        
        VulkanCallMapper.vkBindBufferMemory(vkDevice, bufferHandle, memoryHandle, 0);
        
        // Get device address
        long deviceAddress = VulkanCallMapper.vkGetBufferDeviceAddress(vkDevice, bufferHandle);
        
        BindlessBuffer buffer = new BindlessBuffer(
            bufferHandle, memoryHandle, size, usage, MEMORY_DEVICE_LOCAL,
            deviceAddress, 0);
        
        // Cache the address
        bufferAddressCache.put(bufferHandle, deviceAddress);
        
        int id = bufferIdGenerator.getAndIncrement();
        managedBuffers.put((long) id, buffer);
        STAT_BUFFERS_CREATED.incrementAndGet();
        STAT_BYTES_ALLOCATED.addAndGet(size);
        STAT_BINDLESS_BUFFERS.incrementAndGet();
        
        return id;
    }
    
    /**
     * Create a bindless vertex buffer.
     */
    public int createBindlessVertexBuffer(long size) {
        return createBindlessBuffer(size, USAGE_BINDLESS_VERTEX);
    }
    
    /**
     * Create a bindless index buffer.
     */
    public int createBindlessIndexBuffer(long size) {
        return createBindlessBuffer(size, USAGE_BINDLESS_INDEX);
    }
    
    /**
     * Create a bindless storage buffer.
     */
    public int createBindlessStorageBuffer(long size) {
        return createBindlessBuffer(size, USAGE_BINDLESS_STORAGE);
    }
    
    /**
     * Get device address for a buffer.
     */
    public long getBufferDeviceAddress(int bufferId) {
        VulkanBuffer buffer = managedBuffers.get((long) bufferId);
        if (buffer == null) return 0;
        
        // Check cache first
        Long cached = bufferAddressCache.get(buffer.handle);
        if (cached != null) {
            return cached;
        }
        
        // Query device address
        long address = VulkanCallMapper.vkGetBufferDeviceAddress(vkDevice, buffer.handle);
        bufferAddressCache.put(buffer.handle, address);
        STAT_DEVICE_ADDRESS_QUERIES.incrementAndGet();
        
        return address;
    }
    
    /**
     * Get device address from buffer handle.
     */
    public long getBufferDeviceAddressFromHandle(long bufferHandle) {
        Long cached = bufferAddressCache.get(bufferHandle);
        if (cached != null) {
            return cached;
        }
        
        long address = VulkanCallMapper.vkGetBufferDeviceAddress(vkDevice, bufferHandle);
        bufferAddressCache.put(bufferHandle, address);
        STAT_DEVICE_ADDRESS_QUERIES.incrementAndGet();
        
        return address;
    }
    
    /**
     * Create buffer address table for shader access.
     * Returns a storage buffer containing device addresses.
     */
    public int createBufferAddressTable(int[] bufferIds) {
        // Calculate table size (8 bytes per address)
        long tableSize = bufferIds.length * 8L;
        tableSize = alignTo(tableSize, ALIGNMENT_STORAGE_BUFFER);
        
        // Create storage buffer for the table
        int tableBufferId = createBindlessBuffer(tableSize, USAGE_BINDLESS_STORAGE);
        VulkanBuffer tableBuffer = managedBuffers.get((long) tableBufferId);
        
        // Upload addresses
        ByteBuffer addressData = ByteBuffer.allocateDirect((int) tableSize).order(ByteOrder.nativeOrder());
        LongBuffer addresses = addressData.asLongBuffer();
        
        for (int bufferId : bufferIds) {
            long address = getBufferDeviceAddress(bufferId);
            addresses.put(address);
        }
        
        addressData.clear();
        uploadData(tableBufferId, 0, addressData);
        
        return tableBufferId;
    }
    
    /**
     * Check if buffer device address is supported.
     */
    public boolean supportsBufferDeviceAddress() {
        return vulkan12Features.bufferDeviceAddress;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DESCRIPTOR INDEXING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get descriptor indexing features.
     */
    public DescriptorIndexingFeatures getDescriptorIndexingFeatures() {
        return descriptorIndexingFeatures;
    }
    
    /**
     * Check if bindless textures are supported.
     */
    public boolean supportsBindlessTextures() {
        return descriptorIndexingFeatures.supportsBindlessTextures();
    }
    
    /**
     * Check if bindless buffers are supported.
     */
    public boolean supportsBindlessBuffers() {
        return descriptorIndexingFeatures.supportsBindlessBuffers();
    }
    
    /**
     * Create descriptor set layout with indexing flags.
     */
    public long createBindlessDescriptorSetLayout(int descriptorType, int maxDescriptors) {
        /*
         * VkDescriptorSetLayoutBinding binding = VkDescriptorSetLayoutBinding.calloc()
         *     .binding(0)
         *     .descriptorType(descriptorType)
         *     .descriptorCount(maxDescriptors)
         *     .stageFlags(VK_SHADER_STAGE_ALL);
         * 
         * VkDescriptorBindingFlags bindingFlags = 
         *     VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT |
         *     VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT |
         *     VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT;
         * 
         * VkDescriptorSetLayoutBindingFlagsCreateInfo flagsInfo = 
         *     VkDescriptorSetLayoutBindingFlagsCreateInfo.calloc()
         *         .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO)
         *         .bindingCount(1)
         *         .pBindingFlags(stack.ints(bindingFlags));
         * 
         * VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
         *     .pNext(flagsInfo)
         *     .flags(VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT)
         *     .bindingCount(1)
         *     .pBindings(binding);
         * 
         * LongBuffer pLayout = stack.mallocLong(1);
         * vkCreateDescriptorSetLayout(vkDevice, layoutInfo, null, pLayout);
         * return pLayout.get(0);
         */
        
        int bindingFlags = VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT |
                          VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT |
                          VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT;
        
        return VulkanCallMapper.vkCreateBindlessDescriptorSetLayout(
            vkDevice, descriptorType, maxDescriptors, bindingFlags);
    }
    
    /**
     * Create descriptor pool for bindless descriptors.
     */
    public long createBindlessDescriptorPool(int descriptorType, int maxDescriptors, int maxSets) {
        /*
         * VkDescriptorPoolSize poolSize = VkDescriptorPoolSize.calloc()
         *     .type(descriptorType)
         *     .descriptorCount(maxDescriptors);
         * 
         * VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
         *     .flags(VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT)
         *     .maxSets(maxSets)
         *     .poolSizeCount(1)
         *     .pPoolSizes(poolSize);
         * 
         * LongBuffer pPool = stack.mallocLong(1);
         * vkCreateDescriptorPool(vkDevice, poolInfo, null, pPool);
         * return pPool.get(0);
         */
        
        return VulkanCallMapper.vkCreateBindlessDescriptorPool(
            vkDevice, descriptorType, maxDescriptors, maxSets);
    }
    
    /**
     * Allocate descriptor set with variable count.
     */
    public long allocateBindlessDescriptorSet(long pool, long layout, int actualDescriptorCount) {
        /*
         * VkDescriptorSetVariableDescriptorCountAllocateInfo variableInfo = 
         *     VkDescriptorSetVariableDescriptorCountAllocateInfo.calloc()
         *         .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_VARIABLE_DESCRIPTOR_COUNT_ALLOCATE_INFO)
         *         .descriptorSetCount(1)
         *         .pDescriptorCounts(stack.ints(actualDescriptorCount));
         * 
         * VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
         *     .pNext(variableInfo)
         *     .descriptorPool(pool)
         *     .descriptorSetCount(1)
         *     .pSetLayouts(stack.longs(layout));
         * 
         * LongBuffer pSet = stack.mallocLong(1);
         * vkAllocateDescriptorSets(vkDevice, allocInfo, pSet);
         * return pSet.get(0);
         */
        
        return VulkanCallMapper.vkAllocateBindlessDescriptorSet(
            vkDevice, pool, layout, actualDescriptorCount);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SCALAR BLOCK LAYOUT
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Create buffer with scalar block layout (C-style packing).
     * No std140/std430 padding - uses actual C struct layout.
     */
    public int createScalarLayoutBuffer(long size, int usage) {
        if (!vulkan12Features.scalarBlockLayout) {
            throw new UnsupportedOperationException("Scalar block layout not supported");
        }
        
        int bufferId = createBuffer(size, usage, MEMORY_DEVICE_LOCAL);
        STAT_SCALAR_LAYOUT_BUFFERS.incrementAndGet();
        
        return bufferId;
    }
    
    /**
     * Create uniform buffer with scalar layout.
     */
    public int createScalarUniformBuffer(long size) {
        return createScalarLayoutBuffer(
            alignTo(size, 16), // Still need 16-byte alignment for UBOs
            VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT);
    }
    
    /**
     * Check if scalar block layout is supported.
     */
    public boolean supportsScalarBlockLayout() {
        return vulkan12Features.scalarBlockLayout;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // 8-BIT STORAGE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if 8-bit storage is supported.
     */
    public boolean supports8BitStorage() {
        return vulkan12Features.storageBuffer8BitAccess;
    }
    
    /**
     * Create buffer for 8-bit (byte) storage.
     */
    public int create8BitStorageBuffer(long elementCount) {
        if (!vulkan12Features.storageBuffer8BitAccess) {
            throw new UnsupportedOperationException("8-bit storage not supported");
        }
        
        long size = alignTo(elementCount, ALIGNMENT_STORAGE_BUFFER);
        
        VulkanBuffer baseBuffer = createBufferInternal(size, USAGE_STORAGE, MEMORY_HOST_VISIBLE, true);
        if (baseBuffer == null) return 0;
        
        Int8Buffer buffer = new Int8Buffer(
            baseBuffer.handle, baseBuffer.memory, size, USAGE_STORAGE,
            baseBuffer.memoryProperties, elementCount);
        
        buffer.persistentMap = baseBuffer.persistentMap;
        
        int id = bufferIdGenerator.getAndIncrement();
        managedBuffers.put((long) id, buffer);
        STAT_BUFFERS_CREATED.incrementAndGet();
        STAT_BYTES_ALLOCATED.addAndGet(size);
        STAT_8BIT_BUFFERS.incrementAndGet();
        
        return id;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SHADER FLOAT16/INT8
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if shader float16 is supported.
     */
    public boolean supportsShaderFloat16() {
        return vulkan12Features.shaderFloat16;
    }
    
    /**
     * Check if shader int8 is supported.
     */
    public boolean supportsShaderInt8() {
        return vulkan12Features.shaderInt8;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // HOST QUERY RESET
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if host query reset is supported.
     */
    public boolean supportsHostQueryReset() {
        return vulkan12Features.hostQueryReset;
    }
    
    /**
     * Create query pool with host reset support.
     */
    public HostResetQueryPool createQueryPool(int queryType, int queryCount, int pipelineStats) {
        /*
         * VkQueryPoolCreateInfo createInfo = VkQueryPoolCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_QUERY_POOL_CREATE_INFO)
         *     .queryType(queryType)
         *     .queryCount(queryCount)
         *     .pipelineStatistics(pipelineStats);
         * 
         * LongBuffer pPool = stack.mallocLong(1);
         * vkCreateQueryPool(vkDevice, createInfo, null, pPool);
         * return pPool.get(0);
         */
        
        long handle = VulkanCallMapper.vkCreateQueryPool(vkDevice, queryType, queryCount, pipelineStats);
        if (handle == VK_NULL_HANDLE) return null;
        
        HostResetQueryPool pool = new HostResetQueryPool(handle, queryType, queryCount, pipelineStats);
        queryPools.put(handle, pool);
        
        return pool;
    }
    
    /**
     * Reset queries from host (CPU).
     * No command buffer needed!
     */
    public void resetQueryPoolHost(HostResetQueryPool pool, int firstQuery, int queryCount) {
        if (!vulkan12Features.hostQueryReset) {
            throw new UnsupportedOperationException("Host query reset not supported");
        }
        
        /*
         * vkResetQueryPool(vkDevice, pool.handle, firstQuery, queryCount);
         */
        
        VulkanCallMapper.vkResetQueryPool(vkDevice, pool.handle, firstQuery, queryCount);
        pool.markReset(firstQuery, queryCount);
        STAT_HOST_QUERY_RESETS.incrementAndGet();
    }
    
    /**
     * Destroy query pool.
     */
    public void destroyQueryPool(HostResetQueryPool pool) {
        queryPools.remove(pool.handle);
        VulkanCallMapper.vkDestroyQueryPool(vkDevice, pool.handle);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DRAW INDIRECT COUNT
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if draw indirect count is supported.
     */
    public boolean supportsDrawIndirectCount() {
        return vulkan12Features.drawIndirectCount;
    }
    
    /**
     * Create buffer for draw indirect count.
     * Contains draw commands + separate count buffer.
     */
    public int createDrawIndirectCountBuffer(int maxDrawCount, boolean indexed) {
        if (!vulkan12Features.drawIndirectCount) {
            throw new UnsupportedOperationException("Draw indirect count not supported");
        }
        
        // Size per draw command
        int stride = indexed ? 20 : 16; // VkDrawIndexedIndirectCommand or VkDrawIndirectCommand
        long drawCmdSize = (long) maxDrawCount * stride;
        long countSize = 4; // uint32_t
        long totalSize = alignTo(drawCmdSize + countSize, ALIGNMENT_STORAGE_BUFFER);
        
        // Create combined buffer
        int usage = USAGE_INDIRECT_COUNT | VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;
        
        VulkanBuffer baseBuffer = createBufferInternal(totalSize, usage, MEMORY_HOST_VISIBLE, true);
        if (baseBuffer == null) return 0;
        
        // Calculate offsets
        long countOffset = drawCmdSize;
        
        DrawIndirectCountBuffer buffer = new DrawIndirectCountBuffer(
            baseBuffer.handle, baseBuffer.memory, totalSize, usage, baseBuffer.memoryProperties,
            baseBuffer.handle, // Draw commands start at offset 0
            baseBuffer.handle, // Count is in same buffer
            maxDrawCount, stride, indexed);
        
        buffer.persistentMap = baseBuffer.persistentMap;
        
        int id = bufferIdGenerator.getAndIncrement();
        managedBuffers.put((long) id, buffer);
        STAT_BUFFERS_CREATED.incrementAndGet();
        STAT_BYTES_ALLOCATED.addAndGet(totalSize);
        
        return id;
    }
    
    /**
     * Record draw indexed indirect count command.
     */
    public void cmdDrawIndexedIndirectCount(long commandBuffer, 
                                            long drawBuffer, long drawOffset,
                                            long countBuffer, long countOffset,
                                            int maxDrawCount, int stride) {
        /*
         * vkCmdDrawIndexedIndirectCount(
         *     commandBuffer, 
         *     drawBuffer, drawOffset,
         *     countBuffer, countOffset,
         *     maxDrawCount, stride);
         */
        
        VulkanCallMapper.vkCmdDrawIndexedIndirectCount(
            commandBuffer, drawBuffer, drawOffset, countBuffer, countOffset, maxDrawCount, stride);
        STAT_DRAW_INDIRECT_COUNT_CALLS.incrementAndGet();
    }
    
    /**
     * Record draw indirect count command.
     */
    public void cmdDrawIndirectCount(long commandBuffer,
                                     long drawBuffer, long drawOffset,
                                     long countBuffer, long countOffset,
                                     int maxDrawCount, int stride) {
        /*
         * vkCmdDrawIndirectCount(
         *     commandBuffer,
         *     drawBuffer, drawOffset,
         *     countBuffer, countOffset,
         *     maxDrawCount, stride);
         */
        
        VulkanCallMapper.vkCmdDrawIndirectCount(
            commandBuffer, drawBuffer, drawOffset, countBuffer, countOffset, maxDrawCount, stride);
        STAT_DRAW_INDIRECT_COUNT_CALLS.incrementAndGet();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // ASYNC TRANSFER WITH TIMELINE SEMAPHORES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Upload data asynchronously using timeline semaphores.
     * Returns the timeline value to wait for completion.
     */
    public long uploadDataAsync(int bufferId, long offset, ByteBuffer data) {
        if (transferSemaphore == null) {
            // Fall back to synchronous upload
            uploadData(bufferId, offset, data);
            return 0;
        }
        
        VulkanBuffer buffer = managedBuffers.get((long) bufferId);
        if (buffer == null) throw new IllegalArgumentException("Invalid buffer ID");
        
        int dataSize = data.remaining();
        
        // Get staging buffer
        VulkanBuffer staging = stagingRing.buffers[stagingRing.currentIndex];
        
        // Wait for previous use of this staging buffer
        long waitValue = transferSemaphore.getCurrentValue();
        if (stagingRing.fences[stagingRing.currentIndex] != VK_NULL_HANDLE) {
            // Old fence-based wait - will be replaced by timeline
            waitForFence(stagingRing.fences[stagingRing.currentIndex], DEFAULT_FENCE_TIMEOUT);
        }
        
        // Copy to staging
        ByteBuffer stagingMapped = staging.persistentMap;
        stagingMapped.position((int) stagingRing.currentOffset);
        int pos = data.position();
        stagingMapped.put(data);
        data.position(pos);
        stagingMapped.clear();
        
        long stagingOffset = stagingRing.currentOffset;
        stagingRing.currentOffset += dataSize;
        
        // Record copy command
        long cmdBuffer = acquireTransferCommandBuffer();
        beginCommandBuffer(cmdBuffer, VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        recordBufferCopy(cmdBuffer, staging.handle, buffer.handle, stagingOffset, offset, dataSize);
        recordBufferBarrier(cmdBuffer,
            VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
            VK_ACCESS_TRANSFER_WRITE_BIT, VK_ACCESS_MEMORY_READ_BIT,
            buffer.handle, offset, dataSize);
        endCommandBuffer(cmdBuffer);
        
        // Reserve next timeline value
        long signalValue = transferSemaphore.reserveNextValue();
        
        // Submit with timeline semaphore
        submitWithTimeline(vkTransferQueue, cmdBuffer, null, 0, transferSemaphore, signalValue);
        
        STAT_BYTES_TRANSFERRED.addAndGet(dataSize);
        
        return signalValue;
    }
    
    /**
     * Wait for async upload to complete.
     */
    public void waitForUpload(long timelineValue) {
        if (transferSemaphore != null && timelineValue > 0) {
            waitTimelineSemaphore(transferSemaphore, timelineValue, UINT64_MAX);
        }
    }
    
    /**
     * Check if async upload is complete (non-blocking).
     */
    public boolean isUploadComplete(long timelineValue) {
        if (transferSemaphore == null || timelineValue <= 0) return true;
        return getTimelineSemaphoreValue(transferSemaphore) >= timelineValue;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // FRAME MANAGEMENT WITH TIMELINE SEMAPHORES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Begin frame using timeline semaphores instead of fences.
     */
    @Override
    public void beginFrame() {
        if (graphicsSemaphore != null) {
            // Wait for frame N-2 to complete (double buffering)
            int frameIndex = currentFrame.get() % MAX_FRAMES_IN_FLIGHT;
            long waitValue = Math.max(0, graphicsSemaphore.getCurrentValue() - MAX_FRAMES_IN_FLIGHT + 1);
            
            if (waitValue > 0) {
                waitTimelineSemaphore(graphicsSemaphore, waitValue, UINT64_MAX);
            }
        } else {
            // Fall back to fence-based
            super.beginFrame();
        }
        
        processPendingDeletions();
    }
    
    /**
     * End frame and signal timeline semaphore.
     */
    @Override
    public void endFrame() {
        if (graphicsSemaphore != null) {
            // Signal frame completion
            long signalValue = graphicsSemaphore.reserveNextValue();
            signalTimelineSemaphore(graphicsSemaphore, signalValue);
        }
        
        currentFrame.incrementAndGet();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SAMPLER FILTER MINMAX
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if sampler filter minmax is supported.
     */
    public boolean supportsSamplerFilterMinmax() {
        return vulkan12Features.samplerFilterMinmax;
    }
    
    /**
     * Create sampler with min reduction mode.
     */
    public long createMinReductionSampler() {
        if (!vulkan12Features.samplerFilterMinmax) {
            throw new UnsupportedOperationException("Sampler filter minmax not supported");
        }
        
        return VulkanCallMapper.vkCreateReductionSampler(vkDevice, VK_SAMPLER_REDUCTION_MODE_MIN);
    }
    
    /**
     * Create sampler with max reduction mode.
     */
    public long createMaxReductionSampler() {
        if (!vulkan12Features.samplerFilterMinmax) {
            throw new UnsupportedOperationException("Sampler filter minmax not supported");
        }
        
        return VulkanCallMapper.vkCreateReductionSampler(vkDevice, VK_SAMPLER_REDUCTION_MODE_MAX);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DRIVER PROPERTIES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get driver ID.
     */
    public int getDriverID() {
        return vulkan12Features.driverID;
    }
    
    /**
     * Get driver name.
     */
    public String getDriverName() {
        return vulkan12Features.driverName;
    }
    
    /**
     * Get driver info.
     */
    public String getDriverInfo() {
        return vulkan12Features.driverInfo;
    }
    
    /**
     * Check if using NVIDIA driver.
     */
    public boolean isNvidiaDriver() {
        return vulkan12Features.driverID == VK_DRIVER_ID_NVIDIA_PROPRIETARY;
    }
    
    /**
     * Check if using AMD driver.
     */
    public boolean isAmdDriver() {
        return vulkan12Features.driverID == VK_DRIVER_ID_AMD_PROPRIETARY ||
               vulkan12Features.driverID == VK_DRIVER_ID_AMD_OPEN_SOURCE ||
               vulkan12Features.driverID == VK_DRIVER_ID_MESA_RADV;
    }
    
    /**
     * Check if using Intel driver.
     */
    public boolean isIntelDriver() {
        return vulkan12Features.driverID == VK_DRIVER_ID_INTEL_PROPRIETARY_WINDOWS ||
               vulkan12Features.driverID == VK_DRIVER_ID_INTEL_OPEN_SOURCE_MESA;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // GPU-DRIVEN RENDERING HELPERS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * GPU-driven scene buffer structure.
     * Contains all data needed for GPU-driven rendering.
     */
    public static final class GpuDrivenScene {
        public final int vertexBuffer;
        public final int indexBuffer;
        public final int instanceBuffer;
        public final int indirectBuffer;
        public final int materialBuffer;
        
        public final long vertexBufferAddress;
        public final long indexBufferAddress;
        public final long instanceBufferAddress;
        public final long indirectBufferAddress;
        public final long materialBufferAddress;
        
        public final int maxInstances;
        public final int maxDrawCalls;
        
        public GpuDrivenScene(int vertexBuf, int indexBuf, int instanceBuf, int indirectBuf, int materialBuf,
                              long vertexAddr, long indexAddr, long instanceAddr, long indirectAddr, long materialAddr,
                              int maxInstances, int maxDrawCalls) {
            this.vertexBuffer = vertexBuf;
            this.indexBuffer = indexBuf;
            this.instanceBuffer = instanceBuf;
            this.indirectBuffer = indirectBuf;
            this.materialBuffer = materialBuf;
            this.vertexBufferAddress = vertexAddr;
            this.indexBufferAddress = indexAddr;
            this.instanceBufferAddress = instanceAddr;
            this.indirectBufferAddress = indirectAddr;
            this.materialBufferAddress = materialAddr;
            this.maxInstances = maxInstances;
            this.maxDrawCalls = maxDrawCalls;
        }
    }
    
    /**
     * Create a complete GPU-driven scene setup.
     */
    public GpuDrivenScene createGpuDrivenScene(long vertexSize, long indexSize, 
                                                int maxInstances, int maxDrawCalls,
                                                long materialSize) {
        // Create all buffers with device addresses
        int vertexBuf = createBindlessVertexBuffer(vertexSize);
        int indexBuf = createBindlessIndexBuffer(indexSize);
        int instanceBuf = createBindlessStorageBuffer(maxInstances * 64L); // 64 bytes per instance
        int indirectBuf = createDrawIndirectCountBuffer(maxDrawCalls, true);
        int materialBuf = createBindlessStorageBuffer(materialSize);

            // Get device addresses
        long vertexAddr = getBufferDeviceAddress(vertexBuf);
        long indexAddr = getBufferDeviceAddress(indexBuf);
        long instanceAddr = getBufferDeviceAddress(instanceBuf);
        long indirectAddr = getBufferDeviceAddress(indirectBuf);
        long materialAddr = getBufferDeviceAddress(materialBuf);
        
        return new GpuDrivenScene(
            vertexBuf, indexBuf, instanceBuf, indirectBuf, materialBuf,
            vertexAddr, indexAddr, instanceAddr, indirectAddr, materialAddr,
            maxInstances, maxDrawCalls);
    }
    
    /**
     * Create push constant data for GPU-driven rendering.
     * Contains all buffer addresses for shader access.
     */
    public ByteBuffer createGpuDrivenPushConstants(GpuDrivenScene scene) {
        // 5 addresses × 8 bytes = 40 bytes
        ByteBuffer pushConstants = ByteBuffer.allocateDirect(40).order(ByteOrder.nativeOrder());
        
        pushConstants.putLong(scene.vertexBufferAddress);
        pushConstants.putLong(scene.indexBufferAddress);
        pushConstants.putLong(scene.instanceBufferAddress);
        pushConstants.putLong(scene.indirectBufferAddress);
        pushConstants.putLong(scene.materialBufferAddress);
        
        pushConstants.flip();
        return pushConstants;
    }
    
    /**
     * Destroy GPU-driven scene and all its buffers.
     */
    public void destroyGpuDrivenScene(GpuDrivenScene scene) {
        deleteBuffer(scene.vertexBuffer);
        deleteBuffer(scene.indexBuffer);
        deleteBuffer(scene.instanceBuffer);
        deleteBuffer(scene.indirectBuffer);
        deleteBuffer(scene.materialBuffer);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // IMAGELESS FRAMEBUFFER SUPPORT
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if imageless framebuffers are supported.
     */
    public boolean supportsImagelessFramebuffer() {
        return vulkan12Features.imagelessFramebuffer;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SEPARATE DEPTH/STENCIL LAYOUTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if separate depth/stencil layouts are supported.
     */
    public boolean supportsSeparateDepthStencilLayouts() {
        return vulkan12Features.separateDepthStencilLayouts;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VULKAN MEMORY MODEL
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if Vulkan memory model is supported.
     */
    public boolean supportsVulkanMemoryModel() {
        return vulkan12Features.vulkanMemoryModel;
    }
    
    /**
     * Check if device scope memory model is supported.
     */
    public boolean supportsVulkanMemoryModelDeviceScope() {
        return vulkan12Features.vulkanMemoryModelDeviceScope;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // UNIFORM BUFFER STANDARD LAYOUT
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if uniform buffer standard layout is supported.
     */
    public boolean supportsUniformBufferStandardLayout() {
        return vulkan12Features.uniformBufferStandardLayout;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SUBGROUP EXTENDED TYPES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if shader subgroup extended types are supported.
     * Allows 8/16/64-bit types in subgroup operations.
     */
    public boolean supportsShaderSubgroupExtendedTypes() {
        return vulkan12Features.shaderSubgroupExtendedTypes;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DEPTH RESOLVE MODES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Depth/stencil resolve configuration.
     */
    public static final class DepthStencilResolve {
        public final int supportedDepthResolveModes;
        public final int supportedStencilResolveModes;
        public final boolean independentResolveNone;
        public final boolean independentResolve;
        
        public DepthStencilResolve(int depthModes, int stencilModes, 
                                    boolean indNone, boolean ind) {
            this.supportedDepthResolveModes = depthModes;
            this.supportedStencilResolveModes = stencilModes;
            this.independentResolveNone = indNone;
            this.independentResolve = ind;
        }
        
        public boolean supportsDepthSampleZero() {
            return (supportedDepthResolveModes & VK_RESOLVE_MODE_SAMPLE_ZERO_BIT) != 0;
        }
        
        public boolean supportsDepthAverage() {
            return (supportedDepthResolveModes & VK_RESOLVE_MODE_AVERAGE_BIT) != 0;
        }
        
        public boolean supportsDepthMin() {
            return (supportedDepthResolveModes & VK_RESOLVE_MODE_MIN_BIT) != 0;
        }
        
        public boolean supportsDepthMax() {
            return (supportedDepthResolveModes & VK_RESOLVE_MODE_MAX_BIT) != 0;
        }
    }
    
    protected DepthStencilResolve depthStencilResolve;
    
    /**
     * Get depth/stencil resolve capabilities.
     */
    public DepthStencilResolve getDepthStencilResolve() {
        if (depthStencilResolve == null) {
            int[] modes = VulkanCallMapper.vkGetDepthStencilResolveProperties(vkPhysicalDevice);
            if (modes != null && modes.length >= 4) {
                depthStencilResolve = new DepthStencilResolve(
                    modes[0], modes[1], modes[2] != 0, modes[3] != 0);
            }
        }
        return depthStencilResolve;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BATCH TIMELINE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Submit multiple command buffers with complex timeline dependencies.
     */
    public void submitBatchWithTimeline(long queue, 
                                        long[] commandBuffers,
                                        TimelineSemaphore[] waitSemaphores,
                                        long[] waitValues,
                                        int[] waitStages,
                                        TimelineSemaphore[] signalSemaphores,
                                        long[] signalValues) {
        /*
         * VkTimelineSemaphoreSubmitInfo timelineInfo = VkTimelineSemaphoreSubmitInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO)
         *     .waitSemaphoreValueCount(waitValues.length)
         *     .pWaitSemaphoreValues(waitValuesBuffer)
         *     .signalSemaphoreValueCount(signalValues.length)
         *     .pSignalSemaphoreValues(signalValuesBuffer);
         * 
         * LongBuffer waitSemaphoreHandles = ...;
         * LongBuffer signalSemaphoreHandles = ...;
         * 
         * VkSubmitInfo submitInfo = VkSubmitInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
         *     .pNext(timelineInfo)
         *     .waitSemaphoreCount(waitSemaphores.length)
         *     .pWaitSemaphores(waitSemaphoreHandles)
         *     .pWaitDstStageMask(waitStagesBuffer)
         *     .commandBufferCount(commandBuffers.length)
         *     .pCommandBuffers(commandBuffersBuffer)
         *     .signalSemaphoreCount(signalSemaphores.length)
         *     .pSignalSemaphores(signalSemaphoreHandles);
         * 
         * vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE);
         */
        
        long[] waitHandles = new long[waitSemaphores.length];
        for (int i = 0; i < waitSemaphores.length; i++) {
            waitHandles[i] = waitSemaphores[i].handle;
        }
        
        long[] signalHandles = new long[signalSemaphores.length];
        for (int i = 0; i < signalSemaphores.length; i++) {
            signalHandles[i] = signalSemaphores[i].handle;
        }
        
        VulkanCallMapper.vkQueueSubmitBatchWithTimeline(
            queue, commandBuffers,
            waitHandles, waitValues, waitStages,
            signalHandles, signalValues);
        
        for (TimelineSemaphore sem : signalSemaphores) {
            sem.recordSignal();
        }
        STAT_TIMELINE_SIGNALS.addAndGet(signalSemaphores.length);
    }
    
    /**
     * Create dependency chain between semaphores.
     * Useful for complex multi-queue synchronization.
     */
    public static final class DependencyChain {
        private final List<TimelineSemaphore> semaphores = new ArrayList<>();
        private final List<Long> values = new ArrayList<>();
        
        public DependencyChain wait(TimelineSemaphore semaphore, long value) {
            semaphores.add(semaphore);
            values.add(value);
            return this;
        }
        
        public TimelineSemaphore[] getSemaphores() {
            return semaphores.toArray(new TimelineSemaphore[0]);
        }
        
        public long[] getValues() {
            long[] result = new long[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i);
            }
            return result;
        }
        
        public void clear() {
            semaphores.clear();
            values.clear();
        }
    }
    
    /**
     * Create a new dependency chain builder.
     */
    public DependencyChain createDependencyChain() {
        return new DependencyChain();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BUFFER CAPTURE/REPLAY
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Create buffer with capture replay for deterministic addresses.
     * Useful for GPU debugging and replay.
     */
    public int createCaptureReplayBuffer(long size, int usage, long captureAddress) {
        if (!vulkan12Features.bufferDeviceAddressCaptureReplay) {
            throw new UnsupportedOperationException("Buffer device address capture replay not supported");
        }
        
        usage |= VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;
        
        /*
         * VkBufferOpaqueCaptureAddressCreateInfo captureInfo = 
         *     VkBufferOpaqueCaptureAddressCreateInfo.calloc()
         *         .sType(VK_STRUCTURE_TYPE_BUFFER_OPAQUE_CAPTURE_ADDRESS_CREATE_INFO)
         *         .opaqueCaptureAddress(captureAddress);
         * 
         * VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
         *     .pNext(captureInfo)
         *     .flags(VK_BUFFER_CREATE_DEVICE_ADDRESS_CAPTURE_REPLAY_BIT)
         *     .size(size)
         *     .usage(usage);
         */
        
        long bufferHandle = VulkanCallMapper.vkCreateCaptureReplayBuffer(
            vkDevice, size, usage, captureAddress);
        
        if (bufferHandle == VK_NULL_HANDLE) return 0;
        
        long memReqSize = VulkanCallMapper.vkGetBufferMemoryRequirements(vkDevice, bufferHandle);
        int memTypeIndex = findMemoryType(MEMORY_DEVICE_LOCAL);
        
        long memoryHandle = VulkanCallMapper.vkAllocateMemoryWithCaptureReplay(
            vkDevice, memReqSize, memTypeIndex, captureAddress);
        
        if (memoryHandle == VK_NULL_HANDLE) {
            VulkanCallMapper.vkDestroyBuffer(vkDevice, bufferHandle);
            return 0;
        }
        
        VulkanCallMapper.vkBindBufferMemory(vkDevice, bufferHandle, memoryHandle, 0);
        
        long deviceAddress = VulkanCallMapper.vkGetBufferDeviceAddress(vkDevice, bufferHandle);
        
        BindlessBuffer buffer = new BindlessBuffer(
            bufferHandle, memoryHandle, size, usage, MEMORY_DEVICE_LOCAL,
            deviceAddress, captureAddress);
        
        bufferAddressCache.put(bufferHandle, deviceAddress);
        
        int id = bufferIdGenerator.getAndIncrement();
        managedBuffers.put((long) id, buffer);
        STAT_BUFFERS_CREATED.incrementAndGet();
        STAT_BYTES_ALLOCATED.addAndGet(size);
        STAT_BINDLESS_BUFFERS.incrementAndGet();
        
        return id;
    }
    
    /**
     * Get opaque capture address for a buffer.
     */
    public long getBufferOpaqueCaptureAddress(int bufferId) {
        VulkanBuffer buffer = managedBuffers.get((long) bufferId);
        if (buffer == null) return 0;
        
        if (buffer instanceof BindlessBuffer) {
            return ((BindlessBuffer) buffer).captureAddress;
        }
        
        return VulkanCallMapper.vkGetBufferOpaqueCaptureAddress(vkDevice, buffer.handle);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // MULTI-QUEUE COORDINATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Coordinate work between graphics and compute queues using timeline semaphores.
     */
    public static final class MultiQueueCoordinator {
        private final VulkanBufferOps12 ops;
        private final TimelineSemaphore graphicsToCompute;
        private final TimelineSemaphore computeToGraphics;
        
        public MultiQueueCoordinator(VulkanBufferOps12 ops) {
            this.ops = ops;
            this.graphicsToCompute = ops.createTimelineSemaphore("g2c", 0);
            this.computeToGraphics = ops.createTimelineSemaphore("c2g", 0);
        }
        
        /**
         * Submit graphics work that compute will wait on.
         */
        public long submitGraphicsForCompute(long commandBuffer) {
            long signalValue = graphicsToCompute.reserveNextValue();
            ops.submitWithTimeline(ops.vkGraphicsQueue, commandBuffer, 
                null, 0, graphicsToCompute, signalValue);
            return signalValue;
        }
        
        /**
         * Submit compute work that waits on graphics and signals for graphics.
         */
        public long submitComputeWithSync(long commandBuffer, long waitGraphicsValue) {
            long signalValue = computeToGraphics.reserveNextValue();
            ops.submitWithTimeline(ops.vkComputeQueue, commandBuffer,
                graphicsToCompute, waitGraphicsValue, computeToGraphics, signalValue);
            return signalValue;
        }
        
        /**
         * Submit graphics work that waits on compute.
         */
        public void submitGraphicsWaitCompute(long commandBuffer, long waitComputeValue, 
                                               TimelineSemaphore signalSem, long signalValue) {
            ops.submitWithTimeline(ops.vkGraphicsQueue, commandBuffer,
                computeToGraphics, waitComputeValue, signalSem, signalValue);
        }
        
        /**
         * Destroy coordinator semaphores.
         */
        public void destroy() {
            ops.destroyTimelineSemaphore(graphicsToCompute);
            ops.destroyTimelineSemaphore(computeToGraphics);
        }
    }
    
    /**
     * Create a multi-queue coordinator for graphics/compute sync.
     */
    public MultiQueueCoordinator createMultiQueueCoordinator() {
        return new MultiQueueCoordinator(this);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VULKAN 1.2 FEATURES QUERY
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get all Vulkan 1.2 features.
     */
    public Vulkan12Features getVulkan12Features() {
        return vulkan12Features;
    }
    
    /**
     * Check if timeline semaphores are supported.
     */
    public boolean supportsTimelineSemaphores() {
        return vulkan12Features.timelineSemaphore;
    }
    
    /**
     * Get maximum timeline semaphore value difference.
     */
    public long getMaxTimelineSemaphoreValueDifference() {
        return vulkan12Features.maxTimelineSemaphoreValueDifference;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATISTICS (Vulkan 1.2 Specific)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static long getTimelineSignals() { return STAT_TIMELINE_SIGNALS.get(); }
    public static long getTimelineWaits() { return STAT_TIMELINE_WAITS.get(); }
    public static long getTimelineCpuWaitNs() { return STAT_TIMELINE_CPU_WAIT_NS.get(); }
    public static long getBindlessBuffers() { return STAT_BINDLESS_BUFFERS.get(); }
    public static long getDeviceAddressQueries() { return STAT_DEVICE_ADDRESS_QUERIES.get(); }
    public static long getDrawIndirectCountCalls() { return STAT_DRAW_INDIRECT_COUNT_CALLS.get(); }
    public static long getHostQueryResets() { return STAT_HOST_QUERY_RESETS.get(); }
    public static long get8BitBuffers() { return STAT_8BIT_BUFFERS.get(); }
    public static long getScalarLayoutBuffers() { return STAT_SCALAR_LAYOUT_BUFFERS.get(); }
    
    public static void resetStats12() {
        resetStats11(); // Reset 1.0/1.1 stats
        STAT_TIMELINE_SIGNALS.set(0);
        STAT_TIMELINE_WAITS.set(0);
        STAT_TIMELINE_CPU_WAIT_NS.set(0);
        STAT_BINDLESS_BUFFERS.set(0);
        STAT_DEVICE_ADDRESS_QUERIES.set(0);
        STAT_DRAW_INDIRECT_COUNT_CALLS.set(0);
        STAT_HOST_QUERY_RESETS.set(0);
        STAT_8BIT_BUFFERS.set(0);
        STAT_SCALAR_LAYOUT_BUFFERS.set(0);
    }
    
    /**
     * Get comprehensive statistics report for Vulkan 1.2.
     */
    @Override
    public String getStatisticsReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Vulkan 1.2 Statistics ===\n");
        sb.append(super.getStatisticsReport());
        sb.append("\n--- Vulkan 1.2 Specific ---\n");
        sb.append(String.format("Timeline Semaphores: signals=%d, waits=%d, cpuWaitMs=%d\n",
            getTimelineSignals(), getTimelineWaits(), getTimelineCpuWaitNs() / 1_000_000));
        sb.append(String.format("Bindless: buffers=%d, addressQueries=%d\n",
            getBindlessBuffers(), getDeviceAddressQueries()));
        sb.append(String.format("Draw Indirect Count: calls=%d\n", getDrawIndirectCountCalls()));
        sb.append(String.format("Host Query Resets: %d\n", getHostQueryResets()));
        sb.append(String.format("8-bit Buffers: %d, Scalar Layout Buffers: %d\n",
            get8BitBuffers(), getScalarLayoutBuffers()));
        
        // Timeline semaphore details
        if (!timelineSemaphores.isEmpty()) {
            sb.append("\n--- Timeline Semaphore Details ---\n");
            for (TimelineSemaphore sem : timelineSemaphores.values()) {
                sb.append(String.format("  %s: current=%d, pending=%d, signals=%d, waits=%d\n",
                    sem.name, sem.getCurrentValue(), sem.getPendingValue(),
                    sem.getSignalCount(), sem.getWaitCount()));
            }
        }
        
        return sb.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VERSION INFO OVERRIDES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Override
    public int getVersionCode() { return 12; }
    
    @Override
    public int getDisplayColor() { return DISPLAY_COLOR_12; }
    
    @Override
    public String getVersionName() { return VERSION_NAME_12; }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CLEANUP OVERRIDE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Override
    protected void cleanup() {
        // Wait for all timeline semaphores to complete
        if (graphicsSemaphore != null) {
            long currentValue = getTimelineSemaphoreValue(graphicsSemaphore);
            waitTimelineSemaphore(graphicsSemaphore, currentValue, UINT64_MAX);
        }
        
        if (transferSemaphore != null) {
            long currentValue = getTimelineSemaphoreValue(transferSemaphore);
            waitTimelineSemaphore(transferSemaphore, currentValue, UINT64_MAX);
        }
        
        if (computeSemaphore != null) {
            long currentValue = getTimelineSemaphoreValue(computeSemaphore);
            waitTimelineSemaphore(computeSemaphore, currentValue, UINT64_MAX);
        }
        
        // Destroy timeline semaphores
        for (TimelineSemaphore sem : timelineSemaphores.values()) {
            VulkanCallMapper.vkDestroySemaphore(vkDevice, sem.handle);
        }
        timelineSemaphores.clear();
        namedSemaphores.clear();
        transferSemaphore = null;
        graphicsSemaphore = null;
        computeSemaphore = null;
        
        // Destroy query pools
        for (HostResetQueryPool pool : queryPools.values()) {
            VulkanCallMapper.vkDestroyQueryPool(vkDevice, pool.handle);
        }
        queryPools.clear();
        
        // Clear address cache
        bufferAddressCache.clear();
        
        // Call parent cleanup
        super.cleanup();
    }
    
    @Override
    public void shutdown() {
        cleanup();
        resetStats12();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DEBUG UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Print detailed Vulkan 1.2 capability report.
     */
    @Override
    public void printCapabilityReport() {
        super.printCapabilityReport();
        
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║               VULKAN 1.2 CAPABILITY REPORT                         ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ TIMELINE SEMAPHORES:                                               ║");
        System.out.printf("║   Supported: %-5b  Max Difference: %-20d         ║%n",
            vulkan12Features.timelineSemaphore, 
            vulkan12Features.maxTimelineSemaphoreValueDifference);
        System.out.println("║                                                                    ║");
        System.out.println("║ BUFFER DEVICE ADDRESS (Bindless):                                  ║");
        System.out.printf("║   Supported: %-5b  Capture/Replay: %-5b  Multi-Device: %-5b    ║%n",
            vulkan12Features.bufferDeviceAddress,
            vulkan12Features.bufferDeviceAddressCaptureReplay,
            vulkan12Features.bufferDeviceAddressMultiDevice);
        System.out.println("║                                                                    ║");
        System.out.println("║ DESCRIPTOR INDEXING:                                               ║");
        System.out.printf("║   Bindless Textures: %-5b  Bindless Buffers: %-5b              ║%n",
            descriptorIndexingFeatures.supportsBindlessTextures(),
            descriptorIndexingFeatures.supportsBindlessBuffers());
        System.out.printf("║   Max Samplers: %-10d  Max Storage Buffers: %-10d      ║%n",
            descriptorIndexingFeatures.maxDescriptorSetUpdateAfterBindSamplers,
            descriptorIndexingFeatures.maxDescriptorSetUpdateAfterBindStorageBuffers);
        System.out.println("║                                                                    ║");
        System.out.println("║ SCALAR BLOCK LAYOUT:                                               ║");
        System.out.printf("║   Supported: %-5b (C-style struct packing)                        ║%n",
            vulkan12Features.scalarBlockLayout);
        System.out.println("║                                                                    ║");
        System.out.println("║ SHADER CAPABILITIES:                                               ║");
        System.out.printf("║   Float16: %-5b  Int8: %-5b  8-bit Storage: %-5b               ║%n",
            vulkan12Features.shaderFloat16, 
            vulkan12Features.shaderInt8,
            vulkan12Features.storageBuffer8BitAccess);
        System.out.println("║                                                                    ║");
        System.out.println("║ ADDITIONAL FEATURES:                                               ║");
        System.out.printf("║   Host Query Reset: %-5b  Draw Indirect Count: %-5b             ║%n",
            vulkan12Features.hostQueryReset, 
            vulkan12Features.drawIndirectCount);
        System.out.printf("║   Imageless Framebuffer: %-5b  Separate D/S: %-5b               ║%n",
            vulkan12Features.imagelessFramebuffer,
            vulkan12Features.separateDepthStencilLayouts);
        System.out.printf("║   Sampler Filter Minmax: %-5b  Memory Model: %-5b               ║%n",
            vulkan12Features.samplerFilterMinmax,
            vulkan12Features.vulkanMemoryModel);
        System.out.println("║                                                                    ║");
        System.out.println("║ DRIVER INFO:                                                       ║");
        System.out.printf("║   Name: %-50s       ║%n", 
            vulkan12Features.driverName != null ? vulkan12Features.driverName : "Unknown");
        System.out.printf("║   Info: %-50s       ║%n",
            vulkan12Features.driverInfo != null ? vulkan12Features.driverInfo : "Unknown");
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Validate that all required 1.2 features are available.
     */
    public boolean validateRequiredFeatures(boolean requireTimeline, boolean requireBindless,
                                            boolean requireDescriptorIndexing) {
        if (requireTimeline && !vulkan12Features.timelineSemaphore) {
            System.err.println("[Vulkan 1.2] Timeline semaphores required but not available");
            return false;
        }
        
        if (requireBindless && !vulkan12Features.bufferDeviceAddress) {
            System.err.println("[Vulkan 1.2] Buffer device address required but not available");
            return false;
        }
        
        if (requireDescriptorIndexing && !descriptorIndexingFeatures.supportsBindlessBuffers()) {
            System.err.println("[Vulkan 1.2] Descriptor indexing required but not available");
            return false;
        }
        
        return true;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONVENIENCE METHODS FOR COMMON PATTERNS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Upload data and immediately get a timeline value to wait on.
     * Simplified async upload pattern.
     */
    public long uploadAndGetFuture(int bufferId, ByteBuffer data) {
        return uploadDataAsync(bufferId, 0, data);
    }
    
    /**
     * Create bindless buffer and upload initial data.
     */
    public int createBindlessBufferWithData(ByteBuffer data, int usage) {
        int bufferId = createBindlessBuffer(data.remaining(), usage);
        long future = uploadDataAsync(bufferId, 0, data);
        waitForUpload(future); // Sync for simplicity
        return bufferId;
    }
    
    /**
     * Create complete bindless vertex + index buffer pair.
     */
    public int[] createBindlessGeometry(ByteBuffer vertices, ByteBuffer indices) {
        int vertexBuf = createBindlessVertexBuffer(vertices.remaining());
        int indexBuf = createBindlessIndexBuffer(indices.remaining());
        
        long vFuture = uploadDataAsync(vertexBuf, 0, vertices);
        long iFuture = uploadDataAsync(indexBuf, 0, indices);
        
        // Wait for both
        waitForUpload(Math.max(vFuture, iFuture));
        
        return new int[] { vertexBuf, indexBuf };
    }
    
    /**
     * Get buffer addresses for geometry pair.
     */
    public long[] getGeometryAddresses(int[] geometryBuffers) {
        return new long[] {
            getBufferDeviceAddress(geometryBuffers[0]),
            getBufferDeviceAddress(geometryBuffers[1])
        };
    }
}
