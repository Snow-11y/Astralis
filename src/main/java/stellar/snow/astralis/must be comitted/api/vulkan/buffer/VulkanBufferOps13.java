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
 * VulkanBufferOps13 - Vulkan 1.3 Dynamic Rendering & Synchronization2 Pipeline
 * 
 * ╔════════════════════════════════════════════════════════════════════════════════════════╗
 * ║      ★★★★★★★★★ VULKAN 1.3 - DYNAMIC RENDERING & SYNC2 REVOLUTION ★★★★★★★★★          ║
 * ║                                                                                        ║
 * ║  Vulkan 1.3 (January 2022) - THE MODERN SIMPLIFICATION:                                ║
 * ║                                                                                        ║
 * ║  ┌──────────────────────────────────────────────────────────────────────────────┐      ║
 * ║  │                      EVERYTHING FROM 1.2 PLUS:                               │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ DYNAMIC RENDERING                                                         │      ║
 * ║  │    • NO MORE RENDER PASSES! Render directly to images                        │      ║
 * ║  │    • No more framebuffer objects                                             │      ║
 * ║  │    • Specify attachments at render time, not creation time                   │      ║
 * ║  │    • Massive reduction in object count & state tracking                      │      ║
 * ║  │    • 5-10% faster rendering (less validation, fewer objects)                 │      ║
 * ║  │    • Simpler API - just begin/end rendering                                  │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ SYNCHRONIZATION2 (VK_KHR_synchronization2)                                │      ║
 * ║  │    • Complete sync redesign - clearer, more powerful                         │      ║
 * ║  │    • 64-bit pipeline stage flags (vs old 32-bit)                             │      ║
 * ║  │    • Fine-grained access masks                                               │      ║
 * ║  │    • Dependency info structure (replaces 5 different barrier types)          │      ║
 * ║  │    • Queue submission with full dependency graph                             │      ║
 * ║  │    • Event-based synchronization improvements                                │      ║
 * ║  │    • Easier to use, harder to get wrong                                      │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ EXTENDED DYNAMIC STATE                                                    │      ║
 * ║  │    • Change MORE pipeline state without rebinding                            │      ║
 * ║  │    • Dynamic: vertex input, primitive topology, logic op, etc.               │      ║
 * ║  │    • Reduces pipeline object count by 10-100x                                │      ║
 * ║  │    • Single pipeline for many configurations                                 │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ PIPELINE CREATION CACHE CONTROL                                           │      ║
 * ║  │    • Deterministic pipeline cache behavior                                   │      ║
 * ║  │    • Better shader compilation control                                       │      ║
 * ║  │    • Faster startup times                                                    │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ FORMAT FEATURE FLAGS 2                                                    │      ║
 * ║  │    • 64-bit format feature flags (vs old 32-bit)                             │      ║
 * ║  │    • More precise format capability queries                                  │      ║
 * ║  │    • Better validation                                                       │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ MAINTENANCE4                                                              │      ║
 * ║  │    • Various quality-of-life improvements                                    │      ║
 * ║  │    • Better limit guarantees                                                 │      ║
 * ║  │    • LocalSizeId for compute shaders                                         │      ║
 * ║  │    • Simpler device memory query                                             │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ ZERO INITIALIZATION                                                       │      ║
 * ║  │    • Automatic zero-init for local variables                                 │      ║
 * ║  │    • Security & determinism                                                  │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ INLINE UNIFORM BLOCKS                                                     │      ║
 * ║  │    • Small uniform data in descriptor sets (no buffer needed)                │      ║
 * ║  │    • Lower memory usage for tiny uniforms                                    │      ║
 * ║  │    • Faster binding updates                                                  │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ TEXTURE COMPRESSION (ETC2/EAC/ASTC mandatory)                             │      ║
 * ║  │    • All devices MUST support these formats                                  │      ║
 * ║  │    • Better mobile texture compatibility                                     │      ║
 * ║  │                                                                              │      ║
 * ║  │  ★ SUBGROUP SIZE CONTROL                                                     │      ║
 * ║  │    • Require specific subgroup sizes                                         │      ║
 * ║  │    • Better performance tuning                                               │      ║
 * ║  └──────────────────────────────────────────────────────────────────────────────┘      ║
 * ║                                                                                        ║
 * ║  ┌──────────────────────────────────────────────────────────────────────────────┐      ║
 * ║  │                   DYNAMIC RENDERING ARCHITECTURE:                            │      ║
 * ║  │                                                                              │      ║
 * ║  │   OLD WAY (Vulkan 1.0-1.2):                                                  │      ║
 * ║  │   ┌─────────────────────────────────────────────────────────────────────┐    │      ║
 * ║  │   │  1. Create RenderPass (complex compatibility rules)                 │    │      ║
 * ║  │   │  2. Create Framebuffer (binds images to render pass)                │    │      ║
 * ║  │   │  3. vkCmdBeginRenderPass(cmdBuf, renderPass, framebuffer)           │    │      ║
 * ║  │   │  4. Draw commands...                                                │    │      ║
 * ║  │   │  5. vkCmdEndRenderPass(cmdBuf)                                      │    │      ║
 * ║  │   │                                                                     │    │      ║
 * ║  │   │  Objects: RenderPass + Framebuffer + ImageViews                     │    │      ║
 * ║  │   │  Complexity: High (compatibility rules, subpass dependencies)       │    │      ║
 * ║  │   └─────────────────────────────────────────────────────────────────────┘    │      ║
 * ║  │                                                                              │      ║
 * ║  │   NEW WAY (Vulkan 1.3):                                                      │      ║
 * ║  │   ┌─────────────────────────────────────────────────────────────────────┐    │      ║
 * ║  │   │  1. vkCmdBeginRendering(cmdBuf, {color, depth, stencil images})     │    │      ║
 * ║  │   │  2. Draw commands...                                                │    │      ║
 * ║  │   │  3. vkCmdEndRendering(cmdBuf)                                       │    │      ║
 * ║  │   │                                                                     │    │      ║
 * ║  │   │  Objects: Just ImageViews                                           │    │      ║
 * ║  │   │  Complexity: Low (specify what you need, when you need it)          │    │      ║
 * ║  │   └─────────────────────────────────────────────────────────────────────┘    │      ║
 * ║  └──────────────────────────────────────────────────────────────────────────────┘      ║
 * ║                                                                                        ║
 * ║  ┌──────────────────────────────────────────────────────────────────────────────┐      ║
 * ║  │                   SYNCHRONIZATION2 IMPROVEMENTS:                             │      ║
 * ║  │                                                                              │      ║
 * ║  │   OLD BARRIER (Vulkan 1.0-1.2):                                              │      ║
 * ║  │   ┌─────────────────────────────────────────────────────────────────────┐    │      ║
 * ║  │   │  vkCmdPipelineBarrier(                                              │    │      ║
 * ║  │   │      cmdBuf,                                                        │    │      ║
 * ║  │   │      srcStageMask,    // 32-bit, coarse stages                     │    │      ║
 * ║  │   │      dstStageMask,    // 32-bit, coarse stages                     │    │      ║
 * ║  │   │      dependencyFlags,                                               │    │      ║
 * ║  │   │      memoryBarriers,  // 3 separate arrays                         │    │      ║
 * ║  │   │      bufferBarriers,                                                │    │      ║
 * ║  │   │      imageBarriers                                                  │    │      ║
 * ║  │   │  );                                                                 │    │      ║
 * ║  │   └─────────────────────────────────────────────────────────────────────┘    │      ║
 * ║  │                                                                              │      ║
 * ║  │   NEW BARRIER (Vulkan 1.3):                                                  │      ║
 * ║  │   ┌─────────────────────────────────────────────────────────────────────┐    │      ║
 * ║  │   │  VkDependencyInfo depInfo = {                                       │    │      ║
 * ║  │   │      .memoryBarriers = {                                            │    │      ║
 * ║  │   │          srcStageMask: VK_PIPELINE_STAGE_2_COPY_BIT,               │    │      ║
 * ║  │   │          srcAccessMask: VK_ACCESS_2_TRANSFER_WRITE_BIT,            │    │      ║
 * ║  │   │          dstStageMask: VK_PIPELINE_STAGE_2_VERTEX_ATTRIBUTE_INPUT, │    │      ║
 * ║  │   │          dstAccessMask: VK_ACCESS_2_VERTEX_ATTRIBUTE_READ_BIT      │    │      ║
 * ║  │   │      },                                                             │    │      ║
 * ║  │   │      .bufferBarriers = {...},                                      │    │      ║
 * ║  │   │      .imageBarriers = {...}                                        │    │      ║
 * ║  │   │  };                                                                 │    │      ║
 * ║  │   │  vkCmdPipelineBarrier2(cmdBuf, &depInfo);                          │    │      ║
 * ║  │   └─────────────────────────────────────────────────────────────────────┘    │      ║
 * ║  │                                                                              │      ║
 * ║  │   BENEFITS:                                                                  │      ║
 * ║  │   • 64-bit stage flags → 40+ fine-grained stages (vs 16 coarse)              │      ║
 * ║  │   • Per-barrier stage/access (not global)                                    │      ║
 * ║  │   • Self-documenting (clear what waits on what)                              │      ║
 * ║  │   • Easier to validate/debug                                                 │      ║
 * ║  └──────────────────────────────────────────────────────────────────────────────┘      ║
 * ║                                                                                        ║
 * ║  ┌──────────────────────────────────────────────────────────────────────────────┐      ║
 * ║  │                   EXTENDED DYNAMIC STATE:                                    │      ║
 * ║  │                                                                              │      ║
 * ║  │   BEFORE:                                                                    │      ║
 * ║  │   • 1000 meshes × 3 topologies × 2 cull modes = 6000 pipelines!              │      ║
 * ║  │   • Huge memory usage                                                        │      ║
 * ║  │   • Long startup time                                                        │      ║
 * ║  │                                                                              │      ║
 * ║  │   AFTER:                                                                     │      ║
 * ║  │   • 1 pipeline with dynamic state                                            │      ║
 * ║  │   • vkCmdSetPrimitiveTopology(TRIANGLE_LIST)                                 │      ║
 * ║  │   • vkCmdSetCullMode(CULL_BACK)                                              │      ║
 * ║  │   • 99% reduction in pipeline objects!                                       │      ║
 * ║  └──────────────────────────────────────────────────────────────────────────────┘      ║
 * ║                                                                                        ║
 * ║  THIS PIPELINE INCLUDES:                                                               ║
 * ║  ✓ Full Vulkan 1.0/1.1/1.2 features                                                    ║
 * ║  ✓ Dynamic rendering support                                                           ║
 * ║  ✓ Synchronization2 barrier system                                                     ║
 * ║  ✓ Extended dynamic state commands                                                     ║
 * ║  ✓ Pipeline cache control                                                              ║
 * ║  ✓ Format feature flags 2                                                              ║
 * ║  ✓ Maintenance4 improvements                                                           ║
 * ║  ✓ Inline uniform blocks                                                               ║
 * ║  ✓ Subgroup size control                                                               ║
 * ║  ✓ Private data slots                                                                  ║
 * ║  ✓ Zero initialization                                                                 ║
 * ║                                                                                        ║
 * ║  PERFORMANCE VS VULKAN 1.2:                                                            ║
 * ║  • 5-10% faster rendering (dynamic rendering, less validation)                         ║
 * ║  • 90-99% fewer pipeline objects (extended dynamic state)                              ║
 * ║  • Better CPU cache utilization (fewer objects)                                        ║
 * ║  • Clearer sync = fewer bugs = better real-world performance                           ║
 * ╚════════════════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ Snowium Render: Vulkan 1.3 ★ DYNAMIC RENDERING & SYNC2 ★                │
 * │ Color: #2196F3 (Blue - The Modern Era)                                  │
 * └─────────────────────────────────────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public class VulkanBufferOps13 extends VulkanBufferOps12 {

    // ═══════════════════════════════════════════════════════════════════════════════
    // VERSION CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VERSION_MAJOR_13 = 1;
    public static final int VERSION_MINOR_13 = 3;
    public static final int VERSION_PATCH_13 = 0;
    public static final int VERSION_CODE_13 = VK_MAKE_API_VERSION(0, 1, 3, 0);
    public static final int DISPLAY_COLOR_13 = 0x2196F3; // Material Blue
    public static final String VERSION_NAME_13 = "Vulkan 1.3";
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VULKAN 1.3 STRUCTURE TYPES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES = 53;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_PROPERTIES = 54;
    public static final int VK_STRUCTURE_TYPE_RENDERING_INFO = 1000044000;
    public static final int VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO = 1000044001;
    public static final int VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO = 1000044002;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES = 1000044003;
    public static final int VK_STRUCTURE_TYPE_COMMAND_BUFFER_INHERITANCE_RENDERING_INFO = 1000044004;
    public static final int VK_STRUCTURE_TYPE_MEMORY_BARRIER_2 = 1000314000;
    public static final int VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2 = 1000314001;
    public static final int VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2 = 1000314002;
    public static final int VK_STRUCTURE_TYPE_DEPENDENCY_INFO = 1000314003;
    public static final int VK_STRUCTURE_TYPE_SUBMIT_INFO_2 = 1000314004;
    public static final int VK_STRUCTURE_TYPE_SEMAPHORE_SUBMIT_INFO = 1000314005;
    public static final int VK_STRUCTURE_TYPE_COMMAND_BUFFER_SUBMIT_INFO = 1000314006;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SYNCHRONIZATION_2_FEATURES = 1000314007;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTENDED_DYNAMIC_STATE_FEATURES = 1000267000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTENDED_DYNAMIC_STATE_2_FEATURES = 1000377000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTENDED_DYNAMIC_STATE_3_FEATURES = 1000455000;
    public static final int VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO = 17;
    public static final int VK_STRUCTURE_TYPE_PIPELINE_CREATION_FEEDBACK_CREATE_INFO = 1000192000;
    public static final int VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_3 = 1000360000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_4_FEATURES = 1000413000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_4_PROPERTIES = 1000413001;
    public static final int VK_STRUCTURE_TYPE_DEVICE_BUFFER_MEMORY_REQUIREMENTS = 1000413002;
    public static final int VK_STRUCTURE_TYPE_DEVICE_IMAGE_MEMORY_REQUIREMENTS = 1000413003;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_INLINE_UNIFORM_BLOCK_FEATURES = 1000138000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_INLINE_UNIFORM_BLOCK_PROPERTIES = 1000138001;
    public static final int VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET_INLINE_UNIFORM_BLOCK = 1000138002;
    public static final int VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_INLINE_UNIFORM_BLOCK_CREATE_INFO = 1000138003;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SUBGROUP_SIZE_CONTROL_FEATURES = 1000225000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SUBGROUP_SIZE_CONTROL_PROPERTIES = 1000225001;
    public static final int VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_REQUIRED_SUBGROUP_SIZE_CREATE_INFO = 1000225002;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ZERO_INITIALIZE_WORKGROUP_MEMORY_FEATURES = 1000325000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PRIVATE_DATA_FEATURES = 1000295000;
    public static final int VK_STRUCTURE_TYPE_DEVICE_PRIVATE_DATA_CREATE_INFO = 1000295001;
    public static final int VK_STRUCTURE_TYPE_PRIVATE_DATA_SLOT_CREATE_INFO = 1000295002;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TEXTURE_COMPRESSION_ASTC_HDR_FEATURES = 1000066000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_DEMOTE_TO_HELPER_INVOCATION_FEATURES = 1000276000;
    public static final int VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_TERMINATE_INVOCATION_FEATURES = 1000215000;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PIPELINE STAGE FLAGS 2 (64-bit)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final long VK_PIPELINE_STAGE_2_NONE = 0L;
    public static final long VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT = 0x00000001L;
    public static final long VK_PIPELINE_STAGE_2_DRAW_INDIRECT_BIT = 0x00000002L;
    public static final long VK_PIPELINE_STAGE_2_VERTEX_INPUT_BIT = 0x00000004L;
    public static final long VK_PIPELINE_STAGE_2_VERTEX_SHADER_BIT = 0x00000008L;
    public static final long VK_PIPELINE_STAGE_2_TESSELLATION_CONTROL_SHADER_BIT = 0x00000010L;
    public static final long VK_PIPELINE_STAGE_2_TESSELLATION_EVALUATION_SHADER_BIT = 0x00000020L;
    public static final long VK_PIPELINE_STAGE_2_GEOMETRY_SHADER_BIT = 0x00000040L;
    public static final long VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT = 0x00000080L;
    public static final long VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT = 0x00000100L;
    public static final long VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT = 0x00000200L;
    public static final long VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT = 0x00000400L;
    public static final long VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT = 0x00000800L;
    public static final long VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT = 0x00001000L;
    public static final long VK_PIPELINE_STAGE_2_TRANSFER_BIT = 0x00001000L;
    public static final long VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT = 0x00002000L;
    public static final long VK_PIPELINE_STAGE_2_HOST_BIT = 0x00004000L;
    public static final long VK_PIPELINE_STAGE_2_ALL_GRAPHICS_BIT = 0x00008000L;
    public static final long VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT = 0x00010000L;
    public static final long VK_PIPELINE_STAGE_2_COPY_BIT = 0x100000000L;
    public static final long VK_PIPELINE_STAGE_2_RESOLVE_BIT = 0x200000000L;
    public static final long VK_PIPELINE_STAGE_2_BLIT_BIT = 0x400000000L;
    public static final long VK_PIPELINE_STAGE_2_CLEAR_BIT = 0x800000000L;
    public static final long VK_PIPELINE_STAGE_2_INDEX_INPUT_BIT = 0x1000000000L;
    public static final long VK_PIPELINE_STAGE_2_VERTEX_ATTRIBUTE_INPUT_BIT = 0x2000000000L;
    public static final long VK_PIPELINE_STAGE_2_PRE_RASTERIZATION_SHADERS_BIT = 0x4000000000L;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // ACCESS FLAGS 2 (64-bit)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final long VK_ACCESS_2_NONE = 0L;
    public static final long VK_ACCESS_2_INDIRECT_COMMAND_READ_BIT = 0x00000001L;
    public static final long VK_ACCESS_2_INDEX_READ_BIT = 0x00000002L;
    public static final long VK_ACCESS_2_VERTEX_ATTRIBUTE_READ_BIT = 0x00000004L;
    public static final long VK_ACCESS_2_UNIFORM_READ_BIT = 0x00000008L;
    public static final long VK_ACCESS_2_INPUT_ATTACHMENT_READ_BIT = 0x00000010L;
    public static final long VK_ACCESS_2_SHADER_READ_BIT = 0x00000020L;
    public static final long VK_ACCESS_2_SHADER_WRITE_BIT = 0x00000040L;
    public static final long VK_ACCESS_2_COLOR_ATTACHMENT_READ_BIT = 0x00000080L;
    public static final long VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT = 0x00000100L;
    public static final long VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_READ_BIT = 0x00000200L;
    public static final long VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT = 0x00000400L;
    public static final long VK_ACCESS_2_TRANSFER_READ_BIT = 0x00000800L;
    public static final long VK_ACCESS_2_TRANSFER_WRITE_BIT = 0x00001000L;
    public static final long VK_ACCESS_2_HOST_READ_BIT = 0x00002000L;
    public static final long VK_ACCESS_2_HOST_WRITE_BIT = 0x00004000L;
    public static final long VK_ACCESS_2_MEMORY_READ_BIT = 0x00008000L;
    public static final long VK_ACCESS_2_MEMORY_WRITE_BIT = 0x00010000L;
    public static final long VK_ACCESS_2_SHADER_SAMPLED_READ_BIT = 0x100000000L;
    public static final long VK_ACCESS_2_SHADER_STORAGE_READ_BIT = 0x200000000L;
    public static final long VK_ACCESS_2_SHADER_STORAGE_WRITE_BIT = 0x400000000L;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SUBMIT FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_SUBMIT_PROTECTED_BIT = 0x00000001;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // RENDERING FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_RENDERING_CONTENTS_SECONDARY_COMMAND_BUFFERS_BIT = 0x00000001;
    public static final int VK_RENDERING_SUSPENDING_BIT = 0x00000002;
    public static final int VK_RENDERING_RESUMING_BIT = 0x00000004;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // ATTACHMENT LOAD/STORE OPS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_ATTACHMENT_LOAD_OP_LOAD = 0;
    public static final int VK_ATTACHMENT_LOAD_OP_CLEAR = 1;
    public static final int VK_ATTACHMENT_LOAD_OP_DONT_CARE = 2;
    public static final int VK_ATTACHMENT_LOAD_OP_NONE = 1000400000;
    
    public static final int VK_ATTACHMENT_STORE_OP_STORE = 0;
    public static final int VK_ATTACHMENT_STORE_OP_DONT_CARE = 1;
    public static final int VK_ATTACHMENT_STORE_OP_NONE = 1000301000;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DYNAMIC STATE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_DYNAMIC_STATE_VIEWPORT = 0;
    public static final int VK_DYNAMIC_STATE_SCISSOR = 1;
    public static final int VK_DYNAMIC_STATE_LINE_WIDTH = 2;
    public static final int VK_DYNAMIC_STATE_DEPTH_BIAS = 3;
    public static final int VK_DYNAMIC_STATE_BLEND_CONSTANTS = 4;
    public static final int VK_DYNAMIC_STATE_DEPTH_BOUNDS = 5;
    public static final int VK_DYNAMIC_STATE_STENCIL_COMPARE_MASK = 6;
    public static final int VK_DYNAMIC_STATE_STENCIL_WRITE_MASK = 7;
    public static final int VK_DYNAMIC_STATE_STENCIL_REFERENCE = 8;
    
    // Extended dynamic state
    public static final int VK_DYNAMIC_STATE_CULL_MODE = 1000267000;
    public static final int VK_DYNAMIC_STATE_FRONT_FACE = 1000267001;
    public static final int VK_DYNAMIC_STATE_PRIMITIVE_TOPOLOGY = 1000267002;
    public static final int VK_DYNAMIC_STATE_VIEWPORT_WITH_COUNT = 1000267003;
    public static final int VK_DYNAMIC_STATE_SCISSOR_WITH_COUNT = 1000267004;
    public static final int VK_DYNAMIC_STATE_VERTEX_INPUT_BINDING_STRIDE = 1000267005;
    public static final int VK_DYNAMIC_STATE_DEPTH_TEST_ENABLE = 1000267006;
    public static final int VK_DYNAMIC_STATE_DEPTH_WRITE_ENABLE = 1000267007;
    public static final int VK_DYNAMIC_STATE_DEPTH_COMPARE_OP = 1000267008;
    public static final int VK_DYNAMIC_STATE_DEPTH_BOUNDS_TEST_ENABLE = 1000267009;
    public static final int VK_DYNAMIC_STATE_STENCIL_TEST_ENABLE = 1000267010;
    public static final int VK_DYNAMIC_STATE_STENCIL_OP = 1000267011;
    
    // Extended dynamic state 2
    public static final int VK_DYNAMIC_STATE_RASTERIZER_DISCARD_ENABLE = 1000377001;
    public static final int VK_DYNAMIC_STATE_DEPTH_BIAS_ENABLE = 1000377002;
    public static final int VK_DYNAMIC_STATE_PRIMITIVE_RESTART_ENABLE = 1000377004;
    
    // Extended dynamic state 3
    public static final int VK_DYNAMIC_STATE_DEPTH_CLAMP_ENABLE = 1000455003;
    public static final int VK_DYNAMIC_STATE_POLYGON_MODE = 1000455004;
    public static final int VK_DYNAMIC_STATE_RASTERIZATION_SAMPLES = 1000455005;
    public static final int VK_DYNAMIC_STATE_SAMPLE_MASK = 1000455006;
    public static final int VK_DYNAMIC_STATE_ALPHA_TO_COVERAGE_ENABLE = 1000455007;
    public static final int VK_DYNAMIC_STATE_ALPHA_TO_ONE_ENABLE = 1000455008;
    public static final int VK_DYNAMIC_STATE_LOGIC_OP_ENABLE = 1000455009;
    public static final int VK_DYNAMIC_STATE_COLOR_BLEND_ENABLE = 1000455010;
    public static final int VK_DYNAMIC_STATE_COLOR_BLEND_EQUATION = 1000455011;
    public static final int VK_DYNAMIC_STATE_COLOR_WRITE_MASK = 1000455012;
    public static final int VK_DYNAMIC_STATE_LOGIC_OP = 1000455014;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIMITIVE TOPOLOGY
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_PRIMITIVE_TOPOLOGY_POINT_LIST = 0;
    public static final int VK_PRIMITIVE_TOPOLOGY_LINE_LIST = 1;
    public static final int VK_PRIMITIVE_TOPOLOGY_LINE_STRIP = 2;
    public static final int VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST = 3;
    public static final int VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP = 4;
    public static final int VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN = 5;
    public static final int VK_PRIMITIVE_TOPOLOGY_LINE_LIST_WITH_ADJACENCY = 6;
    public static final int VK_PRIMITIVE_TOPOLOGY_LINE_STRIP_WITH_ADJACENCY = 7;
    public static final int VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST_WITH_ADJACENCY = 8;
    public static final int VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP_WITH_ADJACENCY = 9;
    public static final int VK_PRIMITIVE_TOPOLOGY_PATCH_LIST = 10;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // FRONT FACE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_FRONT_FACE_COUNTER_CLOCKWISE = 0;
    public static final int VK_FRONT_FACE_CLOCKWISE = 1;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CULL MODE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_CULL_MODE_NONE = 0;
    public static final int VK_CULL_MODE_FRONT_BIT = 0x00000001;
    public static final int VK_CULL_MODE_BACK_BIT = 0x00000002;
    public static final int VK_CULL_MODE_FRONT_AND_BACK = 0x00000003;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // COMPARE OP
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_COMPARE_OP_NEVER = 0;
    public static final int VK_COMPARE_OP_LESS = 1;
    public static final int VK_COMPARE_OP_EQUAL = 2;
    public static final int VK_COMPARE_OP_LESS_OR_EQUAL = 3;
    public static final int VK_COMPARE_OP_GREATER = 4;
    public static final int VK_COMPARE_OP_NOT_EQUAL = 5;
    public static final int VK_COMPARE_OP_GREATER_OR_EQUAL = 6;
    public static final int VK_COMPARE_OP_ALWAYS = 7;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STENCIL OP
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_STENCIL_OP_KEEP = 0;
    public static final int VK_STENCIL_OP_ZERO = 1;
    public static final int VK_STENCIL_OP_REPLACE = 2;
    public static final int VK_STENCIL_OP_INCREMENT_AND_CLAMP = 3;
    public static final int VK_STENCIL_OP_DECREMENT_AND_CLAMP = 4;
    public static final int VK_STENCIL_OP_INVERT = 5;
    public static final int VK_STENCIL_OP_INCREMENT_AND_WRAP = 6;
    public static final int VK_STENCIL_OP_DECREMENT_AND_WRAP = 7;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STENCIL FACE FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_STENCIL_FACE_FRONT_BIT = 0x00000001;
    public static final int VK_STENCIL_FACE_BACK_BIT = 0x00000002;
    public static final int VK_STENCIL_FACE_FRONT_AND_BACK = 0x00000003;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // POLYGON MODE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_POLYGON_MODE_FILL = 0;
    public static final int VK_POLYGON_MODE_LINE = 1;
    public static final int VK_POLYGON_MODE_POINT = 2;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // LOGIC OP
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_LOGIC_OP_CLEAR = 0;
    public static final int VK_LOGIC_OP_AND = 1;
    public static final int VK_LOGIC_OP_AND_REVERSE = 2;
    public static final int VK_LOGIC_OP_COPY = 3;
    public static final int VK_LOGIC_OP_AND_INVERTED = 4;
    public static final int VK_LOGIC_OP_NO_OP = 5;
    public static final int VK_LOGIC_OP_XOR = 6;
    public static final int VK_LOGIC_OP_OR = 7;
    public static final int VK_LOGIC_OP_NOR = 8;
    public static final int VK_LOGIC_OP_EQUIVALENT = 9;
    public static final int VK_LOGIC_OP_INVERT = 10;
    public static final int VK_LOGIC_OP_OR_REVERSE = 11;
    public static final int VK_LOGIC_OP_COPY_INVERTED = 12;
    public static final int VK_LOGIC_OP_OR_INVERTED = 13;
    public static final int VK_LOGIC_OP_NAND = 14;
    public static final int VK_LOGIC_OP_SET = 15;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PIPELINE CACHE CREATE FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_PIPELINE_CACHE_CREATE_EXTERNALLY_SYNCHRONIZED_BIT = 0x00000001;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PIPELINE CREATE FLAGS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static final int VK_PIPELINE_CREATE_DISABLE_OPTIMIZATION_BIT = 0x00000001;
    public static final int VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT = 0x00000002;
    public static final int VK_PIPELINE_CREATE_DERIVATIVE_BIT = 0x00000004;
    public static final int VK_PIPELINE_CREATE_EARLY_RETURN_ON_FAILURE_BIT = 0x00000200;
    public static final int VK_PIPELINE_CREATE_FAIL_ON_PIPELINE_COMPILE_REQUIRED_BIT = 0x00000100;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONFIGURATION CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    protected static final int MAX_INLINE_UNIFORM_BLOCK_SIZE = 256;
    protected static final int MAX_PRIVATE_DATA_SLOTS = 16;
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VULKAN 1.3 FEATURES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Vulkan 1.3 feature set.
     */
    public static final class Vulkan13Features {
        // Dynamic Rendering
        public boolean dynamicRendering;
        
        // Synchronization2
        public boolean synchronization2;
        
        // Extended Dynamic State
        public boolean extendedDynamicState;
        public boolean extendedDynamicState2;
        public boolean extendedDynamicState2LogicOp;
        public boolean extendedDynamicState2PatchControlPoints;
        public boolean extendedDynamicState3TessellationDomainOrigin;
        public boolean extendedDynamicState3DepthClampEnable;
        public boolean extendedDynamicState3PolygonMode;
        public boolean extendedDynamicState3RasterizationSamples;
        public boolean extendedDynamicState3SampleMask;
        public boolean extendedDynamicState3AlphaToCoverageEnable;
        public boolean extendedDynamicState3AlphaToOneEnable;
        public boolean extendedDynamicState3LogicOpEnable;
        public boolean extendedDynamicState3ColorBlendEnable;
        public boolean extendedDynamicState3ColorBlendEquation;
        public boolean extendedDynamicState3ColorWriteMask;
        public boolean extendedDynamicState3RasterizationStream;
        public boolean extendedDynamicState3ConservativeRasterizationMode;
        public boolean extendedDynamicState3ExtraPrimitiveOverestimationSize;
        public boolean extendedDynamicState3DepthClipEnable;
        public boolean extendedDynamicState3SampleLocationsEnable;
        public boolean extendedDynamicState3ColorBlendAdvanced;
        public boolean extendedDynamicState3ProvokingVertexMode;
        public boolean extendedDynamicState3LineRasterizationMode;
        public boolean extendedDynamicState3LineStippleEnable;
        public boolean extendedDynamicState3DepthClipNegativeOneToOne;
        public boolean extendedDynamicState3ViewportWScalingEnable;
        public boolean extendedDynamicState3ViewportSwizzle;
        public boolean extendedDynamicState3CoverageToColorEnable;
        public boolean extendedDynamicState3CoverageToColorLocation;
        public boolean extendedDynamicState3CoverageModulationMode;
        public boolean extendedDynamicState3CoverageModulationTableEnable;
        public boolean extendedDynamicState3CoverageModulationTable;
        public boolean extendedDynamicState3CoverageReductionMode;
        public boolean extendedDynamicState3RepresentativeFragmentTestEnable;
        public boolean extendedDynamicState3ShadingRateImageEnable;
        
        // Maintenance4
        public boolean maintenance4;
        public long maxBufferSize;
        
        // Inline Uniform Block
        public boolean inlineUniformBlock;
        public boolean descriptorBindingInlineUniformBlockUpdateAfterBind;
        public int maxInlineUniformBlockSize;
        public int maxPerStageDescriptorInlineUniformBlocks;
        public int maxPerStageDescriptorUpdateAfterBindInlineUniformBlocks;
        public int maxDescriptorSetInlineUniformBlocks;
        public int maxDescriptorSetUpdateAfterBindInlineUniformBlocks;
        
        // Subgroup Size Control
        public boolean subgroupSizeControl;
        public boolean computeFullSubgroups;
        public int minSubgroupSize;
        public int maxSubgroupSize;
        public int maxComputeWorkgroupSubgroups;
        public int requiredSubgroupSizeStages;
        
        // Private Data
        public boolean privateData;
        
        // Pipeline Creation Cache Control
        public boolean pipelineCreationCacheControl;
        
        // Zero Initialize Workgroup Memory
        public boolean shaderZeroInitializeWorkgroupMemory;
        
        // Shader Demote to Helper Invocation
        public boolean shaderDemoteToHelperInvocation;
        
        // Shader Terminate Invocation
        public boolean shaderTerminateInvocation;
        
        // Texture Compression ASTC HDR
        public boolean textureCompressionASTC_HDR;
        
        // Image Robustness
        public boolean imageRobustness;
        
        @Override
        public String toString() {
            return String.format("Vulkan13Features[dynamicRendering=%b, sync2=%b, " +
                "extDynState=%b, maintenance4=%b, inlineUB=%b]",
                dynamicRendering, synchronization2, extendedDynamicState, 
                maintenance4, inlineUniformBlock);
        }
    }
    
    protected final Vulkan13Features vulkan13Features = new Vulkan13Features();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SYNCHRONIZATION2 SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Memory barrier 2 (Synchronization2).
     */
    public static final class MemoryBarrier2 {
        public long srcStageMask;
        public long srcAccessMask;
        public long dstStageMask;
        public long dstAccessMask;
        
        public MemoryBarrier2(long srcStage, long srcAccess, long dstStage, long dstAccess) {
            this.srcStageMask = srcStage;
            this.srcAccessMask = srcAccess;
            this.dstStageMask = dstStage;
            this.dstAccessMask = dstAccess;
        }
    }
    
    /**
     * Buffer memory barrier 2 (Synchronization2).
     */
    public static final class BufferMemoryBarrier2 {
        public long srcStageMask;
        public long srcAccessMask;
        public long dstStageMask;
        public long dstAccessMask;
        public int srcQueueFamilyIndex;
        public int dstQueueFamilyIndex;
        public long buffer;
        public long offset;
        public long size;
        
        public BufferMemoryBarrier2(long srcStage, long srcAccess, long dstStage, long dstAccess,
                                    long buffer, long offset, long size) {
            this.srcStageMask = srcStage;
            this.srcAccessMask = srcAccess;
            this.dstStageMask = dstStage;
            this.dstAccessMask = dstAccess;
            this.srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
            this.dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED;
            this.buffer = buffer;
            this.offset = offset;
            this.size = size;
        }
    }
    
    /**
     * Dependency info (Synchronization2).
     */
    public static final class DependencyInfo {
        public int dependencyFlags;
        public MemoryBarrier2[] memoryBarriers;
        public BufferMemoryBarrier2[] bufferMemoryBarriers;
        // ImageMemoryBarrier2[] imageMemoryBarriers; // For completeness
        
        public DependencyInfo() {
            this.dependencyFlags = 0;
            this.memoryBarriers = new MemoryBarrier2[0];
            this.bufferMemoryBarriers = new BufferMemoryBarrier2[0];
        }
        
        public DependencyInfo(MemoryBarrier2... memBarriers) {
            this.dependencyFlags = 0;
            this.memoryBarriers = memBarriers;
            this.bufferMemoryBarriers = new BufferMemoryBarrier2[0];
        }
        
        public DependencyInfo(BufferMemoryBarrier2... bufBarriers) {
            this.dependencyFlags = 0;
            this.memoryBarriers = new MemoryBarrier2[0];
            this.bufferMemoryBarriers = bufBarriers;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DYNAMIC RENDERING SYSTEM
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Rendering attachment info.
     */
    public static final class RenderingAttachmentInfo {
        public long imageView;
        public int imageLayout;
        public int resolveMode;
        public long resolveImageView;
        public int resolveImageLayout;
        public int loadOp;
        public int storeOp;
        public float[] clearColor; // RGBA
        
        public RenderingAttachmentInfo(long imageView, int loadOp, int storeOp) {
            this.imageView = imageView;
            this.imageLayout = 2; // VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
            this.resolveMode = VK_RESOLVE_MODE_NONE;
            this.resolveImageView = VK_NULL_HANDLE;
            this.resolveImageLayout = 0;
            this.loadOp = loadOp;
            this.storeOp = storeOp;
            this.clearColor = new float[]{0, 0, 0, 0};
        }
        
        public RenderingAttachmentInfo setClearColor(float r, float g, float b, float a) {
            this.clearColor[0] = r;
            this.clearColor[1] = g;
            this.clearColor[2] = b;
            this.clearColor[3] = a;
            return this;
        }
    }
    
    /**
     * Depth/stencil attachment info.
     */
    public static final class DepthStencilAttachmentInfo {
        public long imageView;
        public int imageLayout;
        public int depthLoadOp;
        public int depthStoreOp;
        public float clearDepth;
        public int stencilLoadOp;
        public int stencilStoreOp;
        public int clearStencil;
        
        public DepthStencilAttachmentInfo(long imageView, int depthLoadOp, int depthStoreOp,
                                           int stencilLoadOp, int stencilStoreOp) {
            this.imageView = imageView;
            this.imageLayout = 1; // VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL
            this.depthLoadOp = depthLoadOp;
            this.depthStoreOp = depthStoreOp;
            this.clearDepth = 1.0f;
            this.stencilLoadOp = stencilLoadOp;
            this.stencilStoreOp = stencilStoreOp;
            this.clearStencil = 0;
        }
        
        public DepthStencilAttachmentInfo setClearDepth(float depth) {
            this.clearDepth = depth;
            return this;
        }
        
        public DepthStencilAttachmentInfo setClearStencil(int stencil) {
            this.clearStencil = stencil;
            return this;
        }
    }
    
    /**
     * Rendering info for dynamic rendering.
     */
    public static final class RenderingInfo {
        public int flags;
        public int renderAreaX;
        public int renderAreaY;
        public int renderAreaWidth;
        public int renderAreaHeight;
        public int layerCount;
        public int viewMask;
        public RenderingAttachmentInfo[] colorAttachments;
        public DepthStencilAttachmentInfo depthAttachment;
        public DepthStencilAttachmentInfo stencilAttachment;
        
        public RenderingInfo(int x, int y, int width, int height) {
            this.flags = 0;
            this.renderAreaX = x;
            this.renderAreaY = y;
            this.renderAreaWidth = width;
            this.renderAreaHeight = height;
            this.layerCount = 1;
            this.viewMask = 0;
            this.colorAttachments = new RenderingAttachmentInfo[0];
            this.depthAttachment = null;
            this.stencilAttachment = null;
        }
        
        public RenderingInfo setColorAttachments(RenderingAttachmentInfo... attachments) {
            this.colorAttachments = attachments;
            return this;
        }
        
        public RenderingInfo setDepthAttachment(DepthStencilAttachmentInfo attachment) {
            this.depthAttachment = attachment;
            return this;
        }
        
        public RenderingInfo setStencilAttachment(DepthStencilAttachmentInfo attachment) {
            this.stencilAttachment = attachment;
            return this;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EXTENDED DYNAMIC STATE TRACKING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Dynamic state tracker for current command buffer state.
     */
    public static final class DynamicStateTracker {
        public int cullMode = VK_CULL_MODE_BACK_BIT;
        public int frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;
        public int primitiveTopology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
        public boolean depthTestEnable = true;
        public boolean depthWriteEnable = true;
        public int depthCompareOp = VK_COMPARE_OP_LESS;
        public boolean depthBoundsTestEnable = false;
        public boolean stencilTestEnable = false;
        public boolean rasterizerDiscardEnable = false;
        public boolean depthBiasEnable = false;
        public boolean primitiveRestartEnable = false;
        public boolean depthClampEnable = false;
        public int polygonMode = VK_POLYGON_MODE_FILL;
        public boolean logicOpEnable = false;
        public int logicOp = VK_LOGIC_OP_COPY;
        
        public void reset() {
            cullMode = VK_CULL_MODE_BACK_BIT;
            frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;
            primitiveTopology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            depthTestEnable = true;
            depthWriteEnable = true;
            depthCompareOp = VK_COMPARE_OP_LESS;
            depthBoundsTestEnable = false;
            stencilTestEnable = false;
            rasterizerDiscardEnable = false;
            depthBiasEnable = false;
            primitiveRestartEnable = false;
            depthClampEnable = false;
            polygonMode = VK_POLYGON_MODE_FILL;
            logicOpEnable = false;
            logicOp = VK_LOGIC_OP_COPY;
        }
    }
    
    protected final ThreadLocal<DynamicStateTracker> dynamicStateTracker = 
            ThreadLocal.withInitial(DynamicStateTracker::new);
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE DATA SLOTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    protected final ConcurrentHashMap<Long, Long> privateDataSlots = new ConcurrentHashMap<>();
    protected final AtomicInteger privateDataSlotCounter = new AtomicInteger(0);
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INLINE UNIFORM BLOCKS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Inline uniform block wrapper.
     */
    public static final class InlineUniformBlock {
        public final int binding;
        public final ByteBuffer data;
        
        public InlineUniformBlock(int binding, int size) {
            this.binding = binding;
            this.data = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        }
        
        public InlineUniformBlock putFloat(int offset, float value) {
            data.putFloat(offset, value);
            return this;
        }
        
        public InlineUniformBlock putInt(int offset, int value) {
            data.putInt(offset, value);
            return this;
        }
        
        public InlineUniformBlock putLong(int offset, long value) {
            data.putLong(offset, value);
            return this;
        }
        
        public InlineUniformBlock putFloats(int offset, float... values) {
            for (int i = 0; i < values.length; i++) {
                data.putFloat(offset + i * 4, values[i]);
            }
            return this;
        }
        
        public InlineUniformBlock putInts(int offset, int... values) {
            for (int i = 0; i < values.length; i++) {
                data.putInt(offset + i * 4, values[i]);
            }
            return this;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATISTICS (Vulkan 1.3 Specific)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    protected static final AtomicLong STAT_DYNAMIC_RENDERING_BEGINS = new AtomicLong();
    protected static final AtomicLong STAT_SYNC2_BARRIERS = new AtomicLong();
    protected static final AtomicLong STAT_DYNAMIC_STATE_CHANGES = new AtomicLong();
    protected static final AtomicLong STAT_INLINE_UNIFORM_BLOCKS = new AtomicLong();
    protected static final AtomicLong STAT_PIPELINE_CACHE_HITS = new AtomicLong();
    protected static final AtomicLong STAT_PIPELINE_CACHE_MISSES = new AtomicLong();
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public VulkanBufferOps13() {
        super();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Override
    public boolean initialize(boolean enableValidation) {
        // Initialize Vulkan 1.0/1.1/1.2 base
        if (!super.initialize(enableValidation)) {
            return false;
        }
        
        try {
            // Query Vulkan 1.3 features
            queryVulkan13Features();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to initialize Vulkan 1.3 features: " + e.getMessage());
            return true; // 1.2 features still available
        }
    }
    
    /**
     * Query all Vulkan 1.3 features and properties.
     */
    protected void queryVulkan13Features() {
        /*
         * VkPhysicalDeviceVulkan13Features features13 = VkPhysicalDeviceVulkan13Features.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES);
         * 
         * VkPhysicalDeviceVulkan13Properties props13 = VkPhysicalDeviceVulkan13Properties.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_PROPERTIES);
         * 
         * VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
         *     .pNext(features13);
         * 
         * VkPhysicalDeviceProperties2 props2 = VkPhysicalDeviceProperties2.calloc()
         *     .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2)
         *     .pNext(props13);
         * 
         * vkGetPhysicalDeviceFeatures2(vkPhysicalDevice, features2);
         * vkGetPhysicalDeviceProperties2(vkPhysicalDevice, props2);
         */
        
        VulkanCallMapper.vkGetVulkan13Features(vkPhysicalDevice, vulkan13Features);
        
        System.out.println("[Vulkan 1.3] " + vulkan13Features);
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DYNAMIC RENDERING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Begin dynamic rendering (no render pass needed!).
     */
    public void cmdBeginRendering(long commandBuffer, RenderingInfo renderingInfo) {
        if (!vulkan13Features.dynamicRendering) {
            throw new UnsupportedOperationException("Dynamic rendering not supported");
        }
        
        /*
         * VkRenderingAttachmentInfo.Buffer colorAttachments = null;
         * if (renderingInfo.colorAttachments.length > 0) {
         *     colorAttachments = VkRenderingAttachmentInfo.calloc(renderingInfo.colorAttachments.length);
         *     for (int i = 0; i < renderingInfo.colorAttachments.length; i++) {
         *         RenderingAttachmentInfo att = renderingInfo.colorAttachments[i];
         *         colorAttachments.get(i)
         *             .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
         *             .imageView(att.imageView)
         *             .imageLayout(att.imageLayout)
         *             .resolveMode(att.resolveMode)
         *             .resolveImageView(att.resolveImageView)
         *             .resolveImageLayout(att.resolveImageLayout)
         *             .loadOp(att.loadOp)
         *             .storeOp(att.storeOp)
         *             .clearValue(VkClearValue.calloc()
         *                 .color(VkClearColorValue.calloc()
         *                     .float32(stack.floats(att.clearColor))));
         *     }
         * }
         * 
         * VkRenderingAttachmentInfo depthAttachment = null;
         * if (renderingInfo.depthAttachment != null) {
         *     depthAttachment = VkRenderingAttachmentInfo.calloc()
         *         .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
         *         .imageView(renderingInfo.depthAttachment.imageView)
         *         .imageLayout(renderingInfo.depthAttachment.imageLayout)
         *         .loadOp(renderingInfo.depthAttachment.depthLoadOp)
         *         .storeOp(renderingInfo.depthAttachment.depthStoreOp)
         *         .clearValue(VkClearValue.calloc()
         *             .depthStencil(VkClearDepthStencilValue.calloc()
         *                 .depth(renderingInfo.depthAttachment.clearDepth)
         *                 .stencil(renderingInfo.depthAttachment.clearStencil)));
         * }
         * 
         * VkRenderingInfo vkRenderingInfo = VkRenderingInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_RENDERING_INFO)
         *     .flags(renderingInfo.flags)
         *     .renderArea(VkRect2D.calloc()
         *         .offset(VkOffset2D.calloc()
         *             .x(renderingInfo.renderAreaX)
         *             .y(renderingInfo.renderAreaY))
         *         .extent(VkExtent2D.calloc()
         *             .width(renderingInfo.renderAreaWidth)
         *             .height(renderingInfo.renderAreaHeight)))
         *     .layerCount(renderingInfo.layerCount)
         *     .viewMask(renderingInfo.viewMask)
         *     .colorAttachmentCount(renderingInfo.colorAttachments.length)
         *     .pColorAttachments(colorAttachments)
         *     .pDepthAttachment(depthAttachment)
         *     .pStencilAttachment(renderingInfo.stencilAttachment != null ? depthAttachment : null);
         * 
         * vkCmdBeginRendering(commandBuffer, vkRenderingInfo);
         */
        
        VulkanCallMapper.vkCmdBeginRendering(commandBuffer, renderingInfo);
        STAT_DYNAMIC_RENDERING_BEGINS.incrementAndGet();
    }
    
    /**
     * End dynamic rendering.
     */
    public void cmdEndRendering(long commandBuffer) {
        VulkanCallMapper.vkCmdEndRendering(commandBuffer);
    }
    
    /**
     * Check if dynamic rendering is supported.
     */
    public boolean supportsDynamicRendering() {
        return vulkan13Features.dynamicRendering;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SYNCHRONIZATION2
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Pipeline barrier 2 (Synchronization2).
     * Much clearer and more powerful than the old barrier system!
     */
    public void cmdPipelineBarrier2(long commandBuffer, DependencyInfo dependencyInfo) {
        if (!vulkan13Features.synchronization2) {
            throw new UnsupportedOperationException("Synchronization2 not supported");
        }
        
        /*
         * VkMemoryBarrier2.Buffer memoryBarriers = null;
         * if (dependencyInfo.memoryBarriers.length > 0) {
         *     memoryBarriers = VkMemoryBarrier2.calloc(dependencyInfo.memoryBarriers.length);
         *     for (int i = 0; i < dependencyInfo.memoryBarriers.length; i++) {
         *         MemoryBarrier2 mb = dependencyInfo.memoryBarriers[i];
         *         memoryBarriers.get(i)
         *             .sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER_2)
         *             .srcStageMask(mb.srcStageMask)
         *             .srcAccessMask(mb.srcAccessMask)
         *             .dstStageMask(mb.dstStageMask)
         *             .dstAccessMask(mb.dstAccessMask);
         *     }
         * }
         * 
         * VkBufferMemoryBarrier2.Buffer bufferBarriers = null;
         * if (dependencyInfo.bufferMemoryBarriers.length > 0) {
         *     bufferBarriers = VkBufferMemoryBarrier2.calloc(dependencyInfo.bufferMemoryBarriers.length);
         *     for (int i = 0; i < dependencyInfo.bufferMemoryBarriers.length; i++) {
         *         BufferMemoryBarrier2 bmb = dependencyInfo.bufferMemoryBarriers[i];
         *         bufferBarriers.get(i)
         *             .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
         *             .srcStageMask(bmb.srcStageMask)
         *             .srcAccessMask(bmb.srcAccessMask)
         *             .dstStageMask(bmb.dstStageMask)
         *             .dstAccessMask(bmb.dstAccessMask)
         *             .srcQueueFamilyIndex(bmb.srcQueueFamilyIndex)
         *             .dstQueueFamilyIndex(bmb.dstQueueFamilyIndex)
         *             .buffer(bmb.buffer)
         *             .offset(bmb.offset)
         *             .size(bmb.size);
         *     }
         * }
         * 
         * VkDependencyInfo vkDepInfo = VkDependencyInfo.calloc()
         *     .sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO)
         *     .dependencyFlags(dependencyInfo.dependencyFlags)
         *     .memoryBarrierCount(dependencyInfo.memoryBarriers.length)
         *     .pMemoryBarriers(memoryBarriers)
         *     .bufferMemoryBarrierCount(dependencyInfo.bufferMemoryBarriers.length)
         *     .pBufferMemoryBarriers(bufferBarriers);
         * 
         * vkCmdPipelineBarrier2(commandBuffer, vkDepInfo);
         */
        
        VulkanCallMapper.vkCmdPipelineBarrier2(commandBuffer, dependencyInfo);
        STAT_SYNC2_BARRIERS.incrementAndGet();
    }
    
    /**
     * Create a transfer → vertex read barrier (common pattern).
     */
    public DependencyInfo createTransferToVertexBarrier(long buffer, long offset, long size) {
        return new DependencyInfo(
            new BufferMemoryBarrier2(
                VK_PIPELINE_STAGE_2_COPY_BIT,
                VK_ACCESS_2_TRANSFER_WRITE_BIT,
                VK_PIPELINE_STAGE_2_VERTEX_ATTRIBUTE_INPUT_BIT,
                VK_ACCESS_2_VERTEX_ATTRIBUTE_READ_BIT,
                buffer, offset, size
            )
        );
    }
    
    /**
     * Create a transfer → index read barrier.
     */
    public DependencyInfo createTransferToIndexBarrier(long buffer, long offset, long size) {
        return new DependencyInfo(
            new BufferMemoryBarrier2(
                VK_PIPELINE_STAGE_2_COPY_BIT,
                VK_ACCESS_2_TRANSFER_WRITE_BIT,
                VK_PIPELINE_STAGE_2_INDEX_INPUT_BIT,
                VK_ACCESS_2_INDEX_READ_BIT,
                buffer, offset, size
            )
        );
    }
    
    /**
     * Create a compute write → shader read barrier.
     */
    public DependencyInfo createComputeToShaderBarrier(long buffer, long offset, long size) {
        return new DependencyInfo(
            new BufferMemoryBarrier2(
                VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT,
                VK_ACCESS_2_SHADER_STORAGE_WRITE_BIT,
                VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT | VK_PIPELINE_STAGE_2_VERTEX_SHADER_BIT,
                VK_ACCESS_2_SHADER_STORAGE_READ_BIT,
                buffer, offset, size
            )
        );
    }
    
    /**
     * Check if Synchronization2 is supported.
     */
    public boolean supportsSynchronization2() {
        return vulkan13Features.synchronization2;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EXTENDED DYNAMIC STATE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Set cull mode dynamically.
     */
    public void cmdSetCullMode(long commandBuffer, int cullMode) {
        if (!vulkan13Features.extendedDynamicState) {
            throw new UnsupportedOperationException("Extended dynamic state not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.cullMode != cullMode) {
            VulkanCallMapper.vkCmdSetCullMode(commandBuffer, cullMode);
            tracker.cullMode = cullMode;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set front face dynamically.
     */
    public void cmdSetFrontFace(long commandBuffer, int frontFace) {
        if (!vulkan13Features.extendedDynamicState) {
            throw new UnsupportedOperationException("Extended dynamic state not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.frontFace != frontFace) {
            VulkanCallMapper.vkCmdSetFrontFace(commandBuffer, frontFace);
            tracker.frontFace = frontFace;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set primitive topology dynamically.
     */
    public void cmdSetPrimitiveTopology(long commandBuffer, int primitiveTopology) {
        if (!vulkan13Features.extendedDynamicState) {
            throw new UnsupportedOperationException("Extended dynamic state not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.primitiveTopology != primitiveTopology) {
            VulkanCallMapper.vkCmdSetPrimitiveTopology(commandBuffer, primitiveTopology);
            tracker.primitiveTopology = primitiveTopology;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set depth test enable dynamically.
     */
    public void cmdSetDepthTestEnable(long commandBuffer, boolean depthTestEnable) {
        if (!vulkan13Features.extendedDynamicState) {
            throw new UnsupportedOperationException("Extended dynamic state not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.depthTestEnable != depthTestEnable) {
            VulkanCallMapper.vkCmdSetDepthTestEnable(commandBuffer, depthTestEnable);
            tracker.depthTestEnable = depthTestEnable;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set depth write enable dynamically.
     */
    public void cmdSetDepthWriteEnable(long commandBuffer, boolean depthWriteEnable) {
        if (!vulkan13Features.extendedDynamicState) {
            throw new UnsupportedOperationException("Extended dynamic state not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.depthWriteEnable != depthWriteEnable) {
            VulkanCallMapper.vkCmdSetDepthWriteEnable(commandBuffer, depthWriteEnable);
            tracker.depthWriteEnable = depthWriteEnable;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set depth compare op dynamically.
     */
    public void cmdSetDepthCompareOp(long commandBuffer, int depthCompareOp) {
        if (!vulkan13Features.extendedDynamicState) {
            throw new UnsupportedOperationException("Extended dynamic state not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.depthCompareOp != depthCompareOp) {
            VulkanCallMapper.vkCmdSetDepthCompareOp(commandBuffer, depthCompareOp);
            tracker.depthCompareOp = depthCompareOp;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set depth bounds test enable dynamically.
     */
    public void cmdSetDepthBoundsTestEnable(long commandBuffer, boolean enable) {
        if (!vulkan13Features.extendedDynamicState) {
            throw new UnsupportedOperationException("Extended dynamic state not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.depthBoundsTestEnable != enable) {
            VulkanCallMapper.vkCmdSetDepthBoundsTestEnable(commandBuffer, enable);
            tracker.depthBoundsTestEnable = enable;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set stencil test enable dynamically.
     */
    public void cmdSetStencilTestEnable(long commandBuffer, boolean enable) {
        if (!vulkan13Features.extendedDynamicState) {
            throw new UnsupportedOperationException("Extended dynamic state not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.stencilTestEnable != enable) {
            VulkanCallMapper.vkCmdSetStencilTestEnable(commandBuffer, enable);
            tracker.stencilTestEnable = enable;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set stencil op dynamically.
     */
    public void cmdSetStencilOp(long commandBuffer, int faceMask, 
                                 int failOp, int passOp, int depthFailOp, int compareOp) {
        if (!vulkan13Features.extendedDynamicState) {
            throw new UnsupportedOperationException("Extended dynamic state not supported");
        }
        
        VulkanCallMapper.vkCmdSetStencilOp(commandBuffer, faceMask, failOp, passOp, depthFailOp, compareOp);
        STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
    }
    
    /**
     * Set rasterizer discard enable dynamically.
     */
    public void cmdSetRasterizerDiscardEnable(long commandBuffer, boolean enable) {
        if (!vulkan13Features.extendedDynamicState2) {
            throw new UnsupportedOperationException("Extended dynamic state 2 not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.rasterizerDiscardEnable != enable) {
            VulkanCallMapper.vkCmdSetRasterizerDiscardEnable(commandBuffer, enable);
            tracker.rasterizerDiscardEnable = enable;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set depth bias enable dynamically.
     */
    public void cmdSetDepthBiasEnable(long commandBuffer, boolean enable) {
        if (!vulkan13Features.extendedDynamicState2) {
            throw new UnsupportedOperationException("Extended dynamic state 2 not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.depthBiasEnable != enable) {
            VulkanCallMapper.vkCmdSetDepthBiasEnable(commandBuffer, enable);
            tracker.depthBiasEnable = enable;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set primitive restart enable dynamically.
     */
    public void cmdSetPrimitiveRestartEnable(long commandBuffer, boolean enable) {
        if (!vulkan13Features.extendedDynamicState2) {
            throw new UnsupportedOperationException("Extended dynamic state 2 not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.primitiveRestartEnable != enable) {
            VulkanCallMapper.vkCmdSetPrimitiveRestartEnable(commandBuffer, enable);
            tracker.primitiveRestartEnable = enable;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set depth clamp enable dynamically.
     */
    public void cmdSetDepthClampEnable(long commandBuffer, boolean enable) {
        if (!vulkan13Features.extendedDynamicState3DepthClampEnable) {
            throw new UnsupportedOperationException("Extended dynamic state 3 depth clamp not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.depthClampEnable != enable) {
            VulkanCallMapper.vkCmdSetDepthClampEnable(commandBuffer, enable);
            tracker.depthClampEnable = enable;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set polygon mode dynamically.
     */
    public void cmdSetPolygonMode(long commandBuffer, int polygonMode) {
        if (!vulkan13Features.extendedDynamicState3PolygonMode) {
            throw new UnsupportedOperationException("Extended dynamic state 3 polygon mode not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.polygonMode != polygonMode) {
            VulkanCallMapper.vkCmdSetPolygonMode(commandBuffer, polygonMode);
            tracker.polygonMode = polygonMode;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set logic op enable dynamically.
     */
    public void cmdSetLogicOpEnable(long commandBuffer, boolean enable) {
        if (!vulkan13Features.extendedDynamicState3LogicOpEnable) {
            throw new UnsupportedOperationException("Extended dynamic state 3 logic op enable not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.logicOpEnable != enable) {
            VulkanCallMapper.vkCmdSetLogicOpEnable(commandBuffer, enable);
            tracker.logicOpEnable = enable;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Set logic op dynamically.
     */
    public void cmdSetLogicOp(long commandBuffer, int logicOp) {
        if (!vulkan13Features.extendedDynamicState2LogicOp) {
            throw new UnsupportedOperationException("Extended dynamic state 2 logic op not supported");
        }
        
        DynamicStateTracker tracker = dynamicStateTracker.get();
        if (tracker.logicOp != logicOp) {
            VulkanCallMapper.vkCmdSetLogicOp(commandBuffer, logicOp);
            tracker.logicOp = logicOp;
            STAT_DYNAMIC_STATE_CHANGES.incrementAndGet();
        }
    }
    
    /**
     * Reset dynamic state tracker (call at command buffer begin).
     */
    public void resetDynamicStateTracker() {
        dynamicStateTracker.get().reset();
    }
    
    /**
     * Check if extended dynamic state is supported.
     */
    public boolean supportsExtendedDynamicState() {
        return vulkan13Features.extendedDynamicState;
    }
    
    /**
     * Check if extended dynamic state 2 is supported.
     */
    public boolean supportsExtendedDynamicState2() {
        return vulkan13Features.extendedDynamicState2;
    }
    
    /**
     * Check if extended dynamic state 3 is supported.
     */
    public boolean supportsExtendedDynamicState3() {
        return vulkan13Features.extendedDynamicState3PolygonMode; // Any EDS3 feature
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // MAINTENANCE4
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if Maintenance4 is supported.
     */
    public boolean supportsMaintenance4() {
        return vulkan13Features.maintenance4;
    }
    
    /**
     * Get maximum buffer size.
     */
    public long getMaxBufferSize() {
        return vulkan13Features.maxBufferSize;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INLINE UNIFORM BLOCKS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if inline uniform blocks are supported.
     */
    public boolean supportsInlineUniformBlock() {
        return vulkan13Features.inlineUniformBlock;
    }
    
    /**
     * Create an inline uniform block.
     */
    public InlineUniformBlock createInlineUniformBlock(int binding, int size) {
        if (!vulkan13Features.inlineUniformBlock) {
            throw new UnsupportedOperationException("Inline uniform blocks not supported");
        }
        
        if (size > vulkan13Features.maxInlineUniformBlockSize) {
            throw new IllegalArgumentException("Inline uniform block size exceeds maximum: " + 
                vulkan13Features.maxInlineUniformBlockSize);
        }
        
        InlineUniformBlock block = new InlineUniformBlock(binding, size);
        STAT_INLINE_UNIFORM_BLOCKS.incrementAndGet();
        return block;
    }
    
    /**
     * Get maximum inline uniform block size.
     */
    public int getMaxInlineUniformBlockSize() {
        return vulkan13Features.maxInlineUniformBlockSize;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SUBGROUP SIZE CONTROL
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if subgroup size control is supported.
     */
    public boolean supportsSubgroupSizeControl() {
        return vulkan13Features.subgroupSizeControl;
    }
    
    /**
     * Get minimum subgroup size.
     */
    public int getMinSubgroupSize() {
        return vulkan13Features.minSubgroupSize;
    }
    
    /**
     * Get maximum subgroup size.
     */
    public int getMaxSubgroupSize() {
        return vulkan13Features.maxSubgroupSize;
    }
    
    /**
     * Get maximum compute workgroup subgroups.
     */
    public int getMaxComputeWorkgroupSubgroups() {
        return vulkan13Features.maxComputeWorkgroupSubgroups;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE DATA SLOTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if private data is supported.
     */
    public boolean supportsPrivateData() {
        return vulkan13Features.privateData;
    }
    
    /**
     * Create a private data slot.
     */
    public long createPrivateDataSlot() {
        if (!vulkan13Features.privateData) {
            throw new UnsupportedOperationException("Private data not supported");
        }
        
        if (privateDataSlotCounter.get() >= MAX_PRIVATE_DATA_SLOTS) {
            throw new IllegalStateException("Maximum private data slots reached");
        }
        
        long slot = VulkanCallMapper.vkCreatePrivateDataSlot(vkDevice);
        if (slot != VK_NULL_HANDLE) {
            privateDataSlots.put(slot, 0L);
            privateDataSlotCounter.incrementAndGet();
        }
        
        return slot;
    }
    
    /**
     * Set private data.
     */
    public void setPrivateData(long objectHandle, long slot, long data) {
        VulkanCallMapper.vkSetPrivateData(vkDevice, objectHandle, slot, data);
        privateDataSlots.put(slot, data);
    }
    
    /**
     * Get private data.
     */
    public long getPrivateData(long objectHandle, long slot) {
        return VulkanCallMapper.vkGetPrivateData(vkDevice, objectHandle, slot);
    }
    
    /**
     * Destroy private data slot.
     */
    public void destroyPrivateDataSlot(long slot) {
        privateDataSlots.remove(slot);
        VulkanCallMapper.vkDestroyPrivateDataSlot(vkDevice, slot);
        privateDataSlotCounter.decrementAndGet();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PIPELINE CACHE CONTROL
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if pipeline creation cache control is supported.
     */
    public boolean supportsPipelineCreationCacheControl() {
        return vulkan13Features.pipelineCreationCacheControl;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // ZERO INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if shader zero initialize workgroup memory is supported.
     */
    public boolean supportsShaderZeroInitializeWorkgroupMemory() {
        return vulkan13Features.shaderZeroInitializeWorkgroupMemory;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SHADER DEMOTE/TERMINATE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if shader demote to helper invocation is supported.
     */
    public boolean supportsShaderDemoteToHelperInvocation() {
        return vulkan13Features.shaderDemoteToHelperInvocation;
    }
    
    /**
     * Check if shader terminate invocation is supported.
     */
    public boolean supportsShaderTerminateInvocation() {
        return vulkan13Features.shaderTerminateInvocation;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TEXTURE COMPRESSION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if texture compression ASTC HDR is supported.
     */
    public boolean supportsTextureCompressionASTC_HDR() {
        return vulkan13Features.textureCompressionASTC_HDR;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // IMAGE ROBUSTNESS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if image robustness is supported.
     */
    public boolean supportsImageRobustness() {
        return vulkan13Features.imageRobustness;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VULKAN 1.3 FEATURES QUERY
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get all Vulkan 1.3 features.
     */
    public Vulkan13Features getVulkan13Features() {
        return vulkan13Features;
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATISTICS (Vulkan 1.3 Specific)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    public static long getDynamicRenderingBegins() { return STAT_DYNAMIC_RENDERING_BEGINS.get(); }
    public static long getSync2Barriers() { return STAT_SYNC2_BARRIERS.get(); }
    public static long getDynamicStateChanges() { return STAT_DYNAMIC_STATE_CHANGES.get(); }
    public static long getInlineUniformBlocks() { return STAT_INLINE_UNIFORM_BLOCKS.get(); }
    public static long getPipelineCacheHits() { return STAT_PIPELINE_CACHE_HITS.get(); }
    public static long getPipelineCacheMisses() { return STAT_PIPELINE_CACHE_MISSES.get(); }
    
    public static void resetStats13() {
        resetStats12(); // Reset 1.0/1.1/1.2 stats
        STAT_DYNAMIC_RENDERING_BEGINS.set(0);
        STAT_SYNC2_BARRIERS.set(0);
        STAT_DYNAMIC_STATE_CHANGES.set(0);
        STAT_INLINE_UNIFORM_BLOCKS.set(0);
        STAT_PIPELINE_CACHE_HITS.set(0);
        STAT_PIPELINE_CACHE_MISSES.set(0);
    }
    
    /**
     * Get comprehensive statistics report for Vulkan 1.3.
     */
    @Override
    public String getStatisticsReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Vulkan 1.3 Statistics ===\n");
        sb.append(super.getStatisticsReport());
        sb.append("\n--- Vulkan 1.3 Specific ---\n");
        sb.append(String.format("Dynamic Rendering: begins=%d\n", getDynamicRenderingBegins()));
        sb.append(String.format("Synchronization2: barriers=%d\n", getSync2Barriers()));
        sb.append(String.format("Dynamic State: changes=%d\n", getDynamicStateChanges()));
        sb.append(String.format("Inline Uniform Blocks: %d\n", getInlineUniformBlocks()));
        sb.append(String.format("Pipeline Cache: hits=%d, misses=%d\n",
            getPipelineCacheHits(), getPipelineCacheMisses()));
        
        return sb.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VERSION INFO OVERRIDES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Override
    public int getVersionCode() { return 13; }
    
    @Override
    public int getDisplayColor() { return DISPLAY_COLOR_13; }
    
    @Override
    public String getVersionName() { return VERSION_NAME_13; }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CLEANUP OVERRIDE
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Override
    protected void cleanup() {
        // Destroy private data slots
        for (Long slot : privateDataSlots.keySet()) {
            VulkanCallMapper.vkDestroyPrivateDataSlot(vkDevice, slot);
        }
        privateDataSlots.clear();
        privateDataSlotCounter.set(0);
        
        // Call parent cleanup
        super.cleanup();
    }
    
    @Override
    public void shutdown() {
        cleanup();
        resetStats13();
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DEBUG UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Print detailed Vulkan 1.3 capability report.
     */
    @Override
    public void printCapabilityReport() {
        super.printCapabilityReport();
        
        System.out.println("╔════════════════════════════════════════════════════════════════════╗");
        System.out.println("║               VULKAN 1.3 CAPABILITY REPORT                         ║");
        System.out.println("╠════════════════════════════════════════════════════════════════════╣");
        System.out.println("║ DYNAMIC RENDERING:                                                 ║");
        System.out.printf("║   Supported: %-5b (No render passes!)                             ║%n",
            vulkan13Features.dynamicRendering);
        System.out.println("║                                                                    ║");
        System.out.println("║ SYNCHRONIZATION2:                                                  ║");
        System.out.printf("║   Supported: %-5b (64-bit stage/access flags)                     ║%n",
            vulkan13Features.synchronization2);
        System.out.println("║                                                                    ║");
        System.out.println("║ EXTENDED DYNAMIC STATE:                                            ║");
        System.out.printf("║   EDS1: %-5b  EDS2: %-5b  EDS3: %-5b                             ║%n",
            vulkan13Features.extendedDynamicState,
            vulkan13Features.extendedDynamicState2,
            vulkan13Features.extendedDynamicState3PolygonMode);
        System.out.println("║                                                                    ║");
        System.out.println("║ MAINTENANCE4:                                                      ║");
        System.out.printf("║   Supported: %-5b  Max Buffer Size: %d GB                  ║%n",
            vulkan13Features.maintenance4,
            vulkan13Features.maxBufferSize / (1024*1024*1024));
        System.out.println("║                                                                    ║");
        System.out.println("║ INLINE UNIFORM BLOCKS:                                             ║");
        System.out.printf("║   Supported: %-5b  Max Size: %d bytes                            ║%n",
            vulkan13Features.inlineUniformBlock,
            vulkan13Features.maxInlineUniformBlockSize);
        System.out.println("║                                                                    ║");
        System.out.println("║ SUBGROUP SIZE CONTROL:                                             ║");
        System.out.printf("║   Supported: %-5b  Range: %d-%d                                   ║%n",
            vulkan13Features.subgroupSizeControl,
            vulkan13Features.minSubgroupSize,
            vulkan13Features.maxSubgroupSize);
        System.out.println("║                                                                    ║");
        System.out.println("║ ADDITIONAL FEATURES:                                               ║");
        System.out.printf("║   Private Data: %-5b  Pipeline Cache Control: %-5b              ║%n",
            vulkan13Features.privateData,
            vulkan13Features.pipelineCreationCacheControl);
        System.out.printf("║   Zero Init Workgroup: %-5b  Image Robustness: %-5b             ║%n",
            vulkan13Features.shaderZeroInitializeWorkgroupMemory,
            vulkan13Features.imageRobustness);
        System.out.printf("║   Shader Demote: %-5b  Shader Terminate: %-5b                    ║%n",
            vulkan13Features.shaderDemoteToHelperInvocation,
            vulkan13Features.shaderTerminateInvocation);
        System.out.printf("║   ASTC HDR: %-5b                                                   ║%n",
            vulkan13Features.textureCompressionASTC_HDR);
        System.out.println("╚════════════════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Validate that all required 1.3 features are available.
     */
    public boolean validateRequiredFeatures(boolean requireDynamicRendering, 
                                            boolean requireSync2,
                                            boolean requireExtendedDynamicState) {
        if (requireDynamicRendering && !vulkan13Features.dynamicRendering) {
            System.err.println("[Vulkan 1.3] Dynamic rendering required but not available");
            return false;
        }
        
        if (requireSync2 && !vulkan13Features.synchronization2) {
            System.err.println("[Vulkan 1.3] Synchronization2 required but not available");
            return false;
        }
        
        if (requireExtendedDynamicState && !vulkan13Features.extendedDynamicState) {
            System.err.println("[Vulkan 1.3] Extended dynamic state required but not available");
            return false;
        }
        
        return true;
    }
}
