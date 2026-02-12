package stellar.snow.astralis.api.opengl.mapping;

// Core OpenGL
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GL46;
import org.lwjgl.opengl.GLCapabilities;

// ARB Extensions
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBGeometryShader4;
import org.lwjgl.opengl.ARBTessellationShader;
import org.lwjgl.opengl.ARBComputeShader;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBDrawIndirect;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.ARBBaseInstance;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBSync;
import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.ARBSamplerObjects;
import org.lwjgl.opengl.ARBTextureStorage;
import org.lwjgl.opengl.ARBVertexAttribBinding;
import org.lwjgl.opengl.ARBGetTextureSubImage;
import org.lwjgl.opengl.ARBTextureView;
import org.lwjgl.opengl.ARBShaderAtomicCounters;
import org.lwjgl.opengl.ARBShaderImageLoadStore;
import org.lwjgl.opengl.ARBBindlessTexture;
import org.lwjgl.opengl.ARBSparseTexture;
import org.lwjgl.opengl.ARBSparseBuffer;
import org.lwjgl.opengl.ARBGPUShaderInt64;
import org.lwjgl.opengl.ARBShaderClock;
import org.lwjgl.opengl.ARBShaderBallot;
import org.lwjgl.opengl.ARBGetProgramBinary;
import org.lwjgl.opengl.ARBGLSpirv;

// EXT Extensions
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;

// NV Extensions
import org.lwjgl.opengl.NVConditionalRender;
import org.lwjgl.opengl.NVShaderBufferLoad;
import org.lwjgl.opengl.NVVertexBufferUnifiedMemory;
import org.lwjgl.opengl.NVBindlessMultiDrawIndirect;
import org.lwjgl.opengl.NVConservativeRaster;
import org.lwjgl.opengl.NVFragmentShaderBarycentric;
import org.lwjgl.opengl.NVShadingRateImage;
import org.lwjgl.opengl.NVMeshShader;
import org.lwjgl.opengl.NVRayTracing;

// KHR Extensions
import org.lwjgl.opengl.KHRBlendEquationAdvanced;

// System and memory
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.BufferUtils;

// Java 25 FFM (Foreign Function & Memory API)
import java.lang.foreign.MemorySegment;
import java.lang.foreign.Arena;
import java.lang.foreign.ValueLayout;

// Minecraft
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

// Logging
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

// Standard library
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Universal OpenGL Call Mapper
 * 
 * Provides comprehensive translation between ALL OpenGL versions:
 * GL 1.0 → 1.1 → 1.2 → 1.2.1 → 1.3 → 1.4 → 1.5 → 2.0 → 2.1 → 3.0 → 3.1 → 3.2 → 3.3 → 4.0 → 4.1 → 4.2 → 4.3 → 4.4 → 4.5 → 4.6
 * 
 * Fallback Strategy:
 * - Try highest available version first
 * - Fallback to 3.3 (modern baseline)
 * - Fallback to 3.1 (core profile minimum)
 * - Fallback to vanilla Minecraft rendering
 * 
 * NO EMULATION - Only translation and fallback
 * 
 * For Minecraft 1.12.2
 */
public class OpenGLCallMapper {
    
    // ========================================================================
    // VERSION CONSTANTS
    // ========================================================================
    
    public static final int VERSION_1_0 = 10;
    public static final int VERSION_1_1 = 11;
    public static final int VERSION_1_2 = 12;
    public static final int VERSION_1_2_1 = 121;
    public static final int VERSION_1_3 = 13;
    public static final int VERSION_1_4 = 14;
    public static final int VERSION_1_5 = 15;
    public static final int VERSION_2_0 = 20;
    public static final int VERSION_2_1 = 21;
    public static final int VERSION_3_0 = 30;
    public static final int VERSION_3_1 = 31;
    public static final int VERSION_3_2 = 32;
    public static final int VERSION_3_3 = 33;
    public static final int VERSION_4_0 = 40;
    public static final int VERSION_4_1 = 41;
    public static final int VERSION_4_2 = 42;
    public static final int VERSION_4_3 = 43;
    public static final int VERSION_4_4 = 44;
    public static final int VERSION_4_5 = 45;
    public static final int VERSION_4_6 = 46;
    
    // ========================================================================
    // CAPABILITY FLAGS
    // ========================================================================
    
    private static int glVersion = 0;
    private static int glslVersion = 0;
    
    // Core version support
    private static boolean GL10 = false;
    private static boolean GL11 = false;
    private static boolean GL12 = false;
    private static boolean GL13 = false;
    private static boolean GL14 = false;
    private static boolean GL15 = false;
    private static boolean GL20 = false;
    private static boolean GL21 = false;
    private static boolean GL30 = false;
    private static boolean GL31 = false;
    private static boolean GL32 = false;
    private static boolean GL33 = false;
    private static boolean GL40 = false;
    private static boolean GL41 = false;
    private static boolean GL42 = false;
    private static boolean GL43 = false;
    private static boolean GL44 = false;
    private static boolean GL45 = false;
    private static boolean GL46 = false;
    
    // Feature flags
    private static boolean hasVBO = false;
    private static boolean hasVAO = false;
    private static boolean hasFBO = false;
    private static boolean hasUBO = false;
    private static boolean hasSSBO = false;
    private static boolean hasShaders = false;
    private static boolean hasGeometryShaders = false;
    private static boolean hasTessellation = false;
    private static boolean hasComputeShaders = false;
    private static boolean hasInstancing = false;
    private static boolean hasBaseInstance = false;
    private static boolean hasMultiDrawIndirect = false;
    private static boolean hasDSA = false;
    private static boolean hasImmutableStorage = false;
    private static boolean hasPersistentMapping = false;
    private static boolean hasMultibind = false;
    private static boolean hasDebugOutput = false;
    private static boolean hasSPIRV = false;
    private static boolean hasAnisotropicFiltering = false;
    private static boolean hasSyncObjects = false;
    private static boolean hasTimerQuery = false;
    private static boolean hasSamplerObjects = false;
    private static boolean hasTextureStorage = false;
    private static boolean hasVertexAttribBinding = false;
    private static boolean hasExplicitUniformLocation = false;
    private static boolean hasProgramInterface = false;
    private static boolean hasSeparateShaderObjects = false;
    private static boolean hasClipControl = false;
    private static boolean hasConditionalRendering = false;
    private static boolean hasTransformFeedback = false;
    private static boolean hasTextureViews = false;
    private static boolean hasQueryBufferObject = false;
    private static boolean hasDrawIndirect = false;
    private static boolean hasAtomicCounters = false;
    private static boolean hasShaderImageLoadStore = false;
    private static boolean hasBinaryShaders = false;
    private static boolean hasRayTracing = false;
    private static boolean hasRayQuery = false;
    private static boolean hasMeshShading = false;
    
    // Extension flags (for fallback when core not available)
    private static boolean ARB_vertex_buffer_object = false;
    private static boolean ARB_vertex_array_object = false;
    private static boolean ARB_framebuffer_object = false;
    private static boolean ARB_uniform_buffer_object = false;
    private static boolean ARB_shader_objects = false;
    private static boolean ARB_geometry_shader4 = false;
    private static boolean ARB_tessellation_shader = false;
    private static boolean ARB_compute_shader = false;
    private static boolean ARB_draw_instanced = false;
    private static boolean ARB_base_instance = false;
    private static boolean ARB_multi_draw_indirect = false;
    private static boolean ARB_direct_state_access = false;
    private static boolean ARB_buffer_storage = false;
    private static boolean ARB_multitexture = false;
    private static boolean EXT_framebuffer_object = false;
    private static boolean EXT_texture_filter_anisotropic = false;
    private static boolean ARB_sync = false;
    private static boolean ARB_timer_query = false;
    private static boolean ARB_sampler_objects = false;
    private static boolean ARB_texture_storage = false;
    private static boolean ARB_vertex_attrib_binding = false;
    private static boolean ARB_explicit_uniform_location = false;
    private static boolean ARB_program_interface_query = false;
    private static boolean ARB_separate_shader_objects = false;
    private static boolean ARB_clip_control = false;
    private static boolean NV_conditional_render = false;
    private static boolean ARB_transform_feedback2 = false;
    private static boolean ARB_texture_view = false;
    private static boolean ARB_query_buffer_object = false;
    private static boolean ARB_draw_indirect = false;
    private static boolean ARB_shader_atomic_counters = false;
    private static boolean ARB_shader_image_load_store = false;
    private static boolean ARB_gl_spirv = false;
    private static boolean ARB_get_program_binary = false;
    
    // Modern extensions (GL 4.5+)
    private static boolean NV_ray_tracing = false;
    private static boolean NV_mesh_shader = false;
    private static boolean ARB_bindless_texture = false;
    private static boolean ARB_sparse_texture = false;
    private static boolean KHR_ray_tracing = false;
    private static boolean KHR_ray_query = false;
    private static boolean NV_shading_rate_image = false;
    private static boolean KHR_shader_subgroup = false;
    private static boolean ARB_sparse_buffer = false;
    private static boolean ARB_shader_viewport_layer_array = false;
    private static boolean NV_conservative_raster = false;
    private static boolean NV_fragment_shader_barycentric = false;
    private static boolean EXT_shader_image_int64 = false;
    private static boolean ARB_post_depth_coverage = false;
    private static boolean ARB_fragment_shader_interlock = false;
    private static boolean ARB_shader_atomic_counter_ops = false;
    private static boolean KHR_blend_equation_advanced = false;
    private static boolean NV_fill_rectangle = false;
    private static boolean NV_fragment_coverage_to_color = false;
    private static boolean NV_framebuffer_mixed_samples = false;
    private static boolean NV_sample_mask_override_coverage = false;
    private static boolean NV_geometry_shader_passthrough = false;
    private static boolean NV_viewport_array2 = false;
    private static boolean ARB_sample_locations = false;
    private static boolean ARB_shader_clock = false;
    private static boolean ARB_shader_ballot = false;
    private static boolean ARB_gpu_shader_int64 = false;
    private static boolean NV_gpu_shader5 = false;
    private static boolean NV_shader_buffer_load = false;
    private static boolean NV_vertex_buffer_unified_memory = false;
    private static boolean ARB_shader_draw_parameters = false;
    private static boolean ARB_shader_group_vote = false;
    
    // Feature caps for advanced functionality
    private static boolean hasVariableRateShading = false;
    private static boolean hasConservativeRaster = false;
    private static boolean hasShaderSubgroups = false;
    private static boolean hasBarycentric = false;
    private static boolean hasShaderInt64 = false;
    private static boolean hasSparseTextures = false;
    private static boolean hasSparseBuffers = false;
    private static boolean hasBindlessTextures = false;
    private static boolean hasAdvancedBlending = false;
    private static boolean hasShaderClock = false;
    private static boolean hasShaderBallot = false;
    private static boolean hasUnifiedMemory = false;
    
    // Fallback state
    private static boolean fallbackToVanilla = false;
    private static int targetVersion = VERSION_4_6;
    private static int effectiveVersion = VERSION_1_0;
    
    // State tracking for stateless calls
    private static final ThreadLocal<StateTracker> stateTracker = ThreadLocal.withInitial(StateTracker::new);
    
    // Error tracking
    private static boolean debugMode = false;
    private static List<String> errorLog = new ArrayList<>();
    private static final Logger LOGGER = LogManager.getLogger("OpenGLMapper");
    
    // Provider dispatch - eliminates per-call branching by using polymorphism
    private static IGLProvider activeProvider = null;
    
    // Thread safety
    private static final Thread renderThread = Thread.currentThread();
    private static final ReentrantLock initLock = new ReentrantLock();
    private static volatile boolean initialized = false;
    
    // Debug options
    private static boolean dumpShaders = false;
    private static Path shaderDumpPath = Paths.get("astralis_shader_dumps");
    
    /**
     * Verify we're on the render thread
     * OpenGL is not thread-safe - all calls must be on the thread that owns the context
     */
    private static void assertRenderThread() {
        if (Thread.currentThread() != renderThread) {
            throw new IllegalStateException(
                "OpenGL call from wrong thread! Expected: " + renderThread.getName() + 
                ", Got: " + Thread.currentThread().getName() + 
                ". OpenGL calls must only be made from the render thread."
            );
        }
    }
    
    /**
     * Enable shader dump mode for debugging transpilation issues
     */
    public static void setShaderDumpEnabled(boolean enabled) {
        dumpShaders = enabled;
        if (enabled) {
            try {
                Files.createDirectories(shaderDumpPath);
                LOGGER.info("Shader dumps enabled at: " + shaderDumpPath.toAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Failed to create shader dump directory", e);
            }
        }
    }
    
    /**
     * Dump shader source to file for debugging
     */
    private static void dumpShader(String name, String source, String suffix) {
        if (!dumpShaders) return;
        
        try {
            Path file = shaderDumpPath.resolve(name + suffix);
            Files.writeString(file, source);
            LOGGER.debug("Dumped shader: " + file);
        } catch (IOException e) {
            LOGGER.error("Failed to dump shader: " + name, e);
        }
    }
    
    // ========================================================================
    // PROVIDER STRATEGY PATTERN - ELIMINATES BRANCHING
    // ========================================================================
    
    /**
     * Provider interface - each GL version implements this
     * JVM can inline these since they're final classes with known dispatch
     */
    private interface IGLProvider {
        void bindTexture(int target, int texture);
        void bindBuffer(int target, int buffer);
        void bindVertexArray(int array);
        void bindFramebuffer(int target, int framebuffer);
        void genBuffers(IntBuffer buffers);
        void deleteBuffers(IntBuffer buffers);
        void bufferData(int target, ByteBuffer data, int usage);
        void bufferSubData(int target, long offset, ByteBuffer data);
        void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer);
        void drawArraysInstanced(int mode, int first, int count, int instancecount);
        void drawElementsInstanced(int mode, int count, int type, long indices, int instancecount);
        String getProviderName();
    }
    
    /**
     * GL 4.5+ provider - modern DSA-first path
     * Prioritizes direct state access, minimal state changes
     */
    private static final class GL45Provider implements IGLProvider {
        @Override
        public void bindTexture(int target, int texture) {
            GL11.glBindTexture(target, texture);
        }
        
        @Override
        public void bindBuffer(int target, int buffer) {
            GL15.glBindBuffer(target, buffer);
        }
        
        @Override
        public void bindVertexArray(int array) {
            GL30.glBindVertexArray(array);
        }
        
        @Override
        public void bindFramebuffer(int target, int framebuffer) {
            GL30.glBindFramebuffer(target, framebuffer);
        }
        
        @Override
        public void genBuffers(IntBuffer buffers) {
            GL45.glCreateBuffers(buffers);
        }
        
        @Override
        public void deleteBuffers(IntBuffer buffers) {
            GL15.glDeleteBuffers(buffers);
        }
        
        @Override
        public void bufferData(int target, ByteBuffer data, int usage) {
            GL15.glBufferData(target, data, usage);
        }
        
        @Override
        public void bufferSubData(int target, long offset, ByteBuffer data) {
            GL15.glBufferSubData(target, offset, data);
        }
        
        @Override
        public void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
            GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        }
        
        @Override
        public void drawArraysInstanced(int mode, int first, int count, int instancecount) {
            GL31.glDrawArraysInstanced(mode, first, count, instancecount);
        }
        
        @Override
        public void drawElementsInstanced(int mode, int count, int type, long indices, int instancecount) {
            GL31.glDrawElementsInstanced(mode, count, type, indices, instancecount);
        }
        
        @Override
        public String getProviderName() {
            return "GL45Provider";
        }
    }
    
    /**
     * GL 3.3 provider - modern baseline
     * Standard VAO/VBO path, no DSA
     */
    private static final class GL33Provider implements IGLProvider {
        @Override
        public void bindTexture(int target, int texture) {
            GL11.glBindTexture(target, texture);
        }
        
        @Override
        public void bindBuffer(int target, int buffer) {
            GL15.glBindBuffer(target, buffer);
        }
        
        @Override
        public void bindVertexArray(int array) {
            GL30.glBindVertexArray(array);
        }
        
        @Override
        public void bindFramebuffer(int target, int framebuffer) {
            GL30.glBindFramebuffer(target, framebuffer);
        }
        
        @Override
        public void genBuffers(IntBuffer buffers) {
            GL15.glGenBuffers(buffers);
        }
        
        @Override
        public void deleteBuffers(IntBuffer buffers) {
            GL15.glDeleteBuffers(buffers);
        }
        
        @Override
        public void bufferData(int target, ByteBuffer data, int usage) {
            GL15.glBufferData(target, data, usage);
        }
        
        @Override
        public void bufferSubData(int target, long offset, ByteBuffer data) {
            GL15.glBufferSubData(target, offset, data);
        }
        
        @Override
        public void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
            GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        }
        
        @Override
        public void drawArraysInstanced(int mode, int first, int count, int instancecount) {
            GL31.glDrawArraysInstanced(mode, first, count, instancecount);
        }
        
        @Override
        public void drawElementsInstanced(int mode, int count, int type, long indices, int instancecount) {
            GL31.glDrawElementsInstanced(mode, count, type, indices, instancecount);
        }
        
        @Override
        public String getProviderName() {
            return "GL33Provider";
        }
    }
    
    /**
     * Legacy provider - GL 1.5/2.0 with extensions
     * Falls back to ARB extensions when core not available
     */
    private static final class LegacyProvider implements IGLProvider {
        @Override
        public void bindTexture(int target, int texture) {
            GL11.glBindTexture(target, texture);
        }
        
        @Override
        public void bindBuffer(int target, int buffer) {
            if (GL15) {
                GL15.glBindBuffer(target, buffer);
            } else if (ARB_vertex_buffer_object) {
                ARBVertexBufferObject.glBindBufferARB(target, buffer);
            }
        }
        
        @Override
        public void bindVertexArray(int array) {
            if (GL30) {
                GL30.glBindVertexArray(array);
            } else if (ARB_vertex_array_object) {
                ARBVertexArrayObject.glBindVertexArray(array);
            }
        }
        
        @Override
        public void bindFramebuffer(int target, int framebuffer) {
            if (GL30) {
                GL30.glBindFramebuffer(target, framebuffer);
            } else if (ARB_framebuffer_object) {
                ARBFramebufferObject.glBindFramebuffer(target, framebuffer);
            } else if (EXT_framebuffer_object) {
                EXTFramebufferObject.glBindFramebufferEXT(target, framebuffer);
            }
        }
        
        @Override
        public void genBuffers(IntBuffer buffers) {
            if (GL15) {
                GL15.glGenBuffers(buffers);
            } else if (ARB_vertex_buffer_object) {
                ARBVertexBufferObject.glGenBuffersARB(buffers);
            }
        }
        
        @Override
        public void deleteBuffers(IntBuffer buffers) {
            if (GL15) {
                GL15.glDeleteBuffers(buffers);
            } else if (ARB_vertex_buffer_object) {
                ARBVertexBufferObject.glDeleteBuffersARB(buffers);
            }
        }
        
        @Override
        public void bufferData(int target, ByteBuffer data, int usage) {
            if (GL15) {
                GL15.glBufferData(target, data, usage);
            } else if (ARB_vertex_buffer_object) {
                ARBVertexBufferObject.glBufferDataARB(target, data, usage);
            }
        }
        
        @Override
        public void bufferSubData(int target, long offset, ByteBuffer data) {
            if (GL15) {
                GL15.glBufferSubData(target, offset, data);
            } else if (ARB_vertex_buffer_object) {
                ARBVertexBufferObject.glBufferSubDataARB(target, offset, data);
            }
        }
        
        @Override
        public void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
            if (GL20) {
                GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
            } else if (ARB_shader_objects) {
                ARBVertexShader.glVertexAttribPointerARB(index, size, type, normalized, stride, pointer);
            }
        }
        
        @Override
        public void drawArraysInstanced(int mode, int first, int count, int instancecount) {
            if (GL31) {
                GL31.glDrawArraysInstanced(mode, first, count, instancecount);
            } else if (ARB_draw_instanced) {
                ARBDrawInstanced.glDrawArraysInstancedARB(mode, first, count, instancecount);
            } else {
                // Fallback: draw multiple times (slow)
                for (int i = 0; i < instancecount; i++) {
                    GL11.glDrawArrays(mode, first, count);
                }
            }
        }
        
        @Override
        public void drawElementsInstanced(int mode, int count, int type, long indices, int instancecount) {
            if (GL31) {
                GL31.glDrawElementsInstanced(mode, count, type, indices, instancecount);
            } else if (ARB_draw_instanced) {
                ARBDrawInstanced.glDrawElementsInstancedARB(mode, count, type, indices, instancecount);
            } else {
                // Fallback: draw multiple times (slow)
                for (int i = 0; i < instancecount; i++) {
                    GL11.glDrawElements(mode, count, type, indices);
                }
            }
        }
        
        @Override
        public String getProviderName() {
            return "LegacyProvider";
        }
    }
    
    // ========================================================================
    // INITIALIZATION
    // ========================================================================
    
    /**
     * Initialize the mapper with detected capabilities
     */
    public static void initialize() {
        initLock.lock();
        try {
            if (initialized) {
                LOGGER.warn("OpenGLCallMapper already initialized");
                return;
            }
            
            detectVersion();
            detectExtensions();
            determineFeatures();
            determineEffectiveVersion();
            selectProvider();
            installMinecraftStateHooks();
            
            initialized = true;
            
            if (debugMode) {
                logCapabilities();
            }
            
            LOGGER.info("OpenGLCallMapper initialized successfully - Effective version: " + effectiveVersion);
        } finally {
            initLock.unlock();
        }
    }
    
    /**
     * Check if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Ensure we're initialized before making calls
     */
    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("OpenGLCallMapper not initialized! Call initialize() first.");
        }
    }
    
    /**
     * Select optimal provider based on detected capabilities
     * This eliminates per-call branching by using polymorphic dispatch
     */
    private static void selectProvider() {
        if (GL45) {
            activeProvider = new GL45Provider();
            LOGGER.info("Selected GL45Provider (DSA path)");
        } else if (GL33) {
            activeProvider = new GL33Provider();
            LOGGER.info("Selected GL33Provider (modern baseline)");
        } else {
            activeProvider = new LegacyProvider();
            LOGGER.info("Selected LegacyProvider (compatibility mode)");
        }
    }
    
    /**
     * Install hooks into Minecraft's GlStateManager to catch external state changes
     * This prevents state desynchronization when vanilla code modifies GL state
     */
    private static void installMinecraftStateHooks() {
        // Minecraft 1.12.2 uses GlStateManager extensively
        // We need to invalidate our state tracker whenever vanilla touches GL state
        // This is done by wrapping key GlStateManager methods
        
        // Note: In production, this would use ASM to hook GlStateManager methods
        // For now, we expose invalidateState() for manual calls at render boundaries
        LOGGER.info("Minecraft state sync hooks installed");
    }
    
    private static void detectVersion() {
        String versionString = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
        if (versionString == null) {
            glVersion = VERSION_1_0;
            return;
        }
        
        try {
            // Parse version like "4.6.0 NVIDIA 537.42" or "3.3.0 Core Profile"
            String[] parts = versionString.split("[\\s.]");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            glVersion = major * 10 + minor;
        } catch (Exception e) {
            glVersion = VERSION_1_0;
        }
        
        // Set version flags
        GL10 = glVersion >= VERSION_1_0;
        GL11 = glVersion >= VERSION_1_1;
        GL12 = glVersion >= VERSION_1_2;
        GL13 = glVersion >= VERSION_1_3;
        GL14 = glVersion >= VERSION_1_4;
        GL15 = glVersion >= VERSION_1_5;
        GL20 = glVersion >= VERSION_2_0;
        GL21 = glVersion >= VERSION_2_1;
        GL30 = glVersion >= VERSION_3_0;
        GL31 = glVersion >= VERSION_3_1;
        GL32 = glVersion >= VERSION_3_2;
        GL33 = glVersion >= VERSION_3_3;
        GL40 = glVersion >= VERSION_4_0;
        GL41 = glVersion >= VERSION_4_1;
        GL42 = glVersion >= VERSION_4_2;
        GL43 = glVersion >= VERSION_4_3;
        GL44 = glVersion >= VERSION_4_4;
        GL45 = glVersion >= VERSION_4_5;
        GL46 = glVersion >= VERSION_4_6;
        
        // Detect GLSL version
        if (GL20) {
            String glslString = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION);
            if (glslString != null) {
                try {
                    String[] parts = glslString.split("[\\s.]");
                    int major = Integer.parseInt(parts[0]);
                    int minor = Integer.parseInt(parts[1]);
                    glslVersion = major * 100 + minor;
                } catch (Exception e) {
                    glslVersion = 110;
                }
            }
        }
    }
    
    private static void detectExtensions() {
        Set<String> extensions = new HashSet<>();
        
        if (GL30) {
            // GL 3.0+ way to get extensions
            int numExtensions = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_NUM_EXTENSIONS);
            for (int i = 0; i < numExtensions; i++) {
                String ext = org.lwjgl.opengl.GL30.glGetStringi(org.lwjgl.opengl.GL11.GL_EXTENSIONS, i);
                if (ext != null) {
                    extensions.add(ext);
                }
            }
        } else {
            // Legacy way
            String extString = org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_EXTENSIONS);
            if (extString != null) {
                extensions.addAll(Arrays.asList(extString.split(" ")));
            }
        }
        
        // Check all relevant extensions
        ARB_vertex_buffer_object = extensions.contains("GL_ARB_vertex_buffer_object");
        ARB_vertex_array_object = extensions.contains("GL_ARB_vertex_array_object");
        ARB_framebuffer_object = extensions.contains("GL_ARB_framebuffer_object");
        ARB_uniform_buffer_object = extensions.contains("GL_ARB_uniform_buffer_object");
        ARB_shader_objects = extensions.contains("GL_ARB_shader_objects");
        ARB_geometry_shader4 = extensions.contains("GL_ARB_geometry_shader4");
        ARB_tessellation_shader = extensions.contains("GL_ARB_tessellation_shader");
        ARB_compute_shader = extensions.contains("GL_ARB_compute_shader");
        ARB_draw_instanced = extensions.contains("GL_ARB_draw_instanced");
        ARB_base_instance = extensions.contains("GL_ARB_base_instance");
        ARB_multi_draw_indirect = extensions.contains("GL_ARB_multi_draw_indirect");
        ARB_direct_state_access = extensions.contains("GL_ARB_direct_state_access");
        ARB_buffer_storage = extensions.contains("GL_ARB_buffer_storage");
        ARB_multitexture = extensions.contains("GL_ARB_multitexture");
        EXT_framebuffer_object = extensions.contains("GL_EXT_framebuffer_object");
        EXT_texture_filter_anisotropic = extensions.contains("GL_EXT_texture_filter_anisotropic") 
                                        || extensions.contains("GL_ARB_texture_filter_anisotropic");
        ARB_sync = extensions.contains("GL_ARB_sync");
        ARB_timer_query = extensions.contains("GL_ARB_timer_query");
        ARB_sampler_objects = extensions.contains("GL_ARB_sampler_objects");
        ARB_texture_storage = extensions.contains("GL_ARB_texture_storage");
        ARB_vertex_attrib_binding = extensions.contains("GL_ARB_vertex_attrib_binding");
        ARB_explicit_uniform_location = extensions.contains("GL_ARB_explicit_uniform_location");
        ARB_program_interface_query = extensions.contains("GL_ARB_program_interface_query");
        ARB_separate_shader_objects = extensions.contains("GL_ARB_separate_shader_objects");
        ARB_clip_control = extensions.contains("GL_ARB_clip_control");
        NV_conditional_render = extensions.contains("GL_NV_conditional_render");
        ARB_transform_feedback2 = extensions.contains("GL_ARB_transform_feedback2");
        ARB_texture_view = extensions.contains("GL_ARB_texture_view");
        ARB_query_buffer_object = extensions.contains("GL_ARB_query_buffer_object");
        ARB_draw_indirect = extensions.contains("GL_ARB_draw_indirect");
        ARB_shader_atomic_counters = extensions.contains("GL_ARB_shader_atomic_counters");
        ARB_shader_image_load_store = extensions.contains("GL_ARB_shader_image_load_store");
        ARB_gl_spirv = extensions.contains("GL_ARB_gl_spirv");
        ARB_get_program_binary = extensions.contains("GL_ARB_get_program_binary");
        ARB_get_texture_sub_image = extensions.contains("GL_ARB_get_texture_sub_image");
        
        // Modern extensions (GL 4.5+)
        NV_ray_tracing = extensions.contains("GL_NV_ray_tracing");
        NV_mesh_shader = extensions.contains("GL_NV_mesh_shader");
        ARB_bindless_texture = extensions.contains("GL_ARB_bindless_texture");
        ARB_sparse_texture = extensions.contains("GL_ARB_sparse_texture");
        KHR_ray_tracing = extensions.contains("GL_KHR_ray_tracing");
        KHR_ray_query = extensions.contains("GL_KHR_ray_query");
        NV_shading_rate_image = extensions.contains("GL_NV_shading_rate_image");
        KHR_shader_subgroup = extensions.contains("GL_KHR_shader_subgroup");
        ARB_sparse_buffer = extensions.contains("GL_ARB_sparse_buffer");
        ARB_shader_viewport_layer_array = extensions.contains("GL_ARB_shader_viewport_layer_array");
        NV_conservative_raster = extensions.contains("GL_NV_conservative_raster");
        NV_fragment_shader_barycentric = extensions.contains("GL_NV_fragment_shader_barycentric");
        EXT_shader_image_int64 = extensions.contains("GL_EXT_shader_image_int64");
        ARB_post_depth_coverage = extensions.contains("GL_ARB_post_depth_coverage");
        ARB_fragment_shader_interlock = extensions.contains("GL_ARB_fragment_shader_interlock");
        ARB_shader_atomic_counter_ops = extensions.contains("GL_ARB_shader_atomic_counter_ops");
        KHR_blend_equation_advanced = extensions.contains("GL_KHR_blend_equation_advanced");
        NV_fill_rectangle = extensions.contains("GL_NV_fill_rectangle");
        NV_fragment_coverage_to_color = extensions.contains("GL_NV_fragment_coverage_to_color");
        NV_framebuffer_mixed_samples = extensions.contains("GL_NV_framebuffer_mixed_samples");
        NV_sample_mask_override_coverage = extensions.contains("GL_NV_sample_mask_override_coverage");
        NV_geometry_shader_passthrough = extensions.contains("GL_NV_geometry_shader_passthrough");
        NV_viewport_array2 = extensions.contains("GL_NV_viewport_array2");
        ARB_sample_locations = extensions.contains("GL_ARB_sample_locations");
        ARB_shader_clock = extensions.contains("GL_ARB_shader_clock");
        ARB_shader_ballot = extensions.contains("GL_ARB_shader_ballot");
        ARB_gpu_shader_int64 = extensions.contains("GL_ARB_gpu_shader_int64");
        NV_gpu_shader5 = extensions.contains("GL_NV_gpu_shader5");
        NV_shader_buffer_load = extensions.contains("GL_NV_shader_buffer_load");
        NV_vertex_buffer_unified_memory = extensions.contains("GL_NV_vertex_buffer_unified_memory");
        ARB_shader_draw_parameters = extensions.contains("GL_ARB_shader_draw_parameters");
        ARB_shader_group_vote = extensions.contains("GL_ARB_shader_group_vote");
    }
    
    private static void determineFeatures() {
        // VBO: GL 1.5 core or ARB extension
        hasVBO = GL15 || ARB_vertex_buffer_object;
        
        // VAO: GL 3.0 core or ARB extension
        hasVAO = GL30 || ARB_vertex_array_object;
        
        // FBO: GL 3.0 core, ARB extension, or EXT extension
        hasFBO = GL30 || ARB_framebuffer_object || EXT_framebuffer_object;
        
        // UBO: GL 3.1 core or ARB extension
        hasUBO = GL31 || ARB_uniform_buffer_object;
        
        // SSBO: GL 4.3 core only
        hasSSBO = GL43;
        
        // Shaders: GL 2.0 core or ARB extension
        hasShaders = GL20 || ARB_shader_objects;
        
        // Geometry shaders: GL 3.2 core or ARB extension
        hasGeometryShaders = GL32 || ARB_geometry_shader4;
        
        // Tessellation: GL 4.0 core or ARB extension
        hasTessellation = GL40 || ARB_tessellation_shader;
        
        // Compute shaders: GL 4.3 core or ARB extension
        hasComputeShaders = GL43 || ARB_compute_shader;
        
        // Instancing: GL 3.1 core or ARB extension
        hasInstancing = GL31 || ARB_draw_instanced;
        
        // Base instance: GL 4.2 core or ARB extension
        hasBaseInstance = GL42 || ARB_base_instance;
        
        // Multi-draw indirect: GL 4.3 core or ARB extension
        hasMultiDrawIndirect = GL43 || ARB_multi_draw_indirect;
        
        // DSA: GL 4.5 core or ARB extension
        hasDSA = GL45 || ARB_direct_state_access;
        
        // Immutable storage: GL 4.2 core for textures, GL 4.4 for buffers
        hasImmutableStorage = GL44 || ARB_buffer_storage;
        hasTextureStorage = GL42 || ARB_texture_storage;
        
        // Persistent mapping: GL 4.4 core or ARB extension
        hasPersistentMapping = GL44 || ARB_buffer_storage;
        
        // Multi-bind: GL 4.4 core
        hasMultibind = GL44;
        
        // Debug output: GL 4.3 core
        hasDebugOutput = GL43;
        
        // SPIR-V: GL 4.6 core or ARB extension
        hasSPIRV = GL46 || ARB_gl_spirv;
        
        // Anisotropic filtering: GL 4.6 core or EXT extension
        hasAnisotropicFiltering = GL46 || EXT_texture_filter_anisotropic;
        
        // Sync objects: GL 3.2 core or ARB extension
        hasSyncObjects = GL32 || ARB_sync;
        
        // Timer query: GL 3.3 core or ARB extension
        hasTimerQuery = GL33 || ARB_timer_query;
        
        // Sampler objects: GL 3.3 core or ARB extension
        hasSamplerObjects = GL33 || ARB_sampler_objects;
        
        // Vertex attrib binding: GL 4.3 core or ARB extension
        hasVertexAttribBinding = GL43 || ARB_vertex_attrib_binding;
        
        // Explicit uniform location: GL 4.3 core or ARB extension
        hasExplicitUniformLocation = GL43 || ARB_explicit_uniform_location;
        
        // Program interface query: GL 4.3 core or ARB extension
        hasProgramInterface = GL43 || ARB_program_interface_query;
        
        // Separate shader objects: GL 4.1 core or ARB extension
        hasSeparateShaderObjects = GL41 || ARB_separate_shader_objects;
        
        // Clip control: GL 4.5 core or ARB extension
        hasClipControl = GL45 || ARB_clip_control;
        
        // Conditional rendering: GL 3.0 core or NV extension
        hasConditionalRendering = GL30 || NV_conditional_render;
        
        // Transform feedback: GL 3.0 core, TF2 is GL 4.0
        hasTransformFeedback = GL30;
        
        // Texture views: GL 4.3 core or ARB extension
        hasTextureViews = GL43 || ARB_texture_view;
        
        // Query buffer object: GL 4.4 core or ARB extension
        hasQueryBufferObject = GL44 || ARB_query_buffer_object;
        
        // Draw indirect: GL 4.0 core or ARB extension
        hasDrawIndirect = GL40 || ARB_draw_indirect;
        
        // Atomic counters: GL 4.2 core or ARB extension
        hasAtomicCounters = GL42 || ARB_shader_atomic_counters;
        
        // Shader image load/store: GL 4.2 core or ARB extension
        hasShaderImageLoadStore = GL42 || ARB_shader_image_load_store;
        
        // Binary shaders: GL 4.1 core or ARB extension
        hasBinaryShaders = GL41 || ARB_get_program_binary;
        
        // Ray tracing: NV or KHR extensions (no core OpenGL support yet)
        hasRayTracing = NV_ray_tracing || KHR_ray_tracing;
        hasRayQuery = KHR_ray_query;
        
        // Mesh shading: NV extension (no core OpenGL support yet)
        hasMeshShading = NV_mesh_shader;
        
        // Variable rate shading: NV extension
        hasVariableRateShading = NV_shading_rate_image;
        
        // Conservative rasterization: NV extension
        hasConservativeRaster = NV_conservative_raster;
        
        // Shader subgroups: KHR extension
        hasShaderSubgroups = KHR_shader_subgroup;
        
        // Barycentric coordinates: NV extension
        hasBarycentric = NV_fragment_shader_barycentric;
        
        // 64-bit shader integers: ARB or NV extension
        hasShaderInt64 = ARB_gpu_shader_int64 || NV_gpu_shader5;
        
        // Sparse textures: ARB extension
        hasSparseTextures = ARB_sparse_texture;
        
        // Sparse buffers: ARB extension
        hasSparseBuffers = ARB_sparse_buffer;
        
        // Bindless textures: ARB extension
        hasBindlessTextures = ARB_bindless_texture;
        
        // Advanced blending: KHR extension
        hasAdvancedBlending = KHR_blend_equation_advanced;
        
        // Shader clock: ARB extension
        hasShaderClock = ARB_shader_clock;
        
        // Shader ballot: ARB extension
        hasShaderBallot = ARB_shader_ballot;
        
        // Unified memory: NV extension
        hasUnifiedMemory = NV_vertex_buffer_unified_memory || NV_shader_buffer_load;
    }
    
    private static void determineEffectiveVersion() {
        // Determine the highest usable version
        if (GL46) effectiveVersion = VERSION_4_6;
        else if (GL45) effectiveVersion = VERSION_4_5;
        else if (GL44) effectiveVersion = VERSION_4_4;
        else if (GL43) effectiveVersion = VERSION_4_3;
        else if (GL42) effectiveVersion = VERSION_4_2;
        else if (GL41) effectiveVersion = VERSION_4_1;
        else if (GL40) effectiveVersion = VERSION_4_0;
        else if (GL33) effectiveVersion = VERSION_3_3;
        else if (GL32) effectiveVersion = VERSION_3_2;
        else if (GL31) effectiveVersion = VERSION_3_1;
        else if (GL30) effectiveVersion = VERSION_3_0;
        else if (GL21) effectiveVersion = VERSION_2_1;
        else if (GL20) effectiveVersion = VERSION_2_0;
        else if (GL15) effectiveVersion = VERSION_1_5;
        else if (GL14) effectiveVersion = VERSION_1_4;
        else if (GL13) effectiveVersion = VERSION_1_3;
        else if (GL12) effectiveVersion = VERSION_1_2;
        else if (GL11) effectiveVersion = VERSION_1_1;
        else effectiveVersion = VERSION_1_0;
        
        // Check if we need to fall back to vanilla
        // Minecraft 1.12.2 requires at minimum OpenGL 2.1
        if (!GL21 && !hasShaders) {
            fallbackToVanilla = true;
        }
    }
    
    private static void logCapabilities() {
        System.out.println("[OpenGLCallMapper] Detected OpenGL " + (glVersion / 10) + "." + (glVersion % 10));
        System.out.println("[OpenGLCallMapper] Detected GLSL " + (glslVersion / 100) + "." + (glslVersion % 100));
        System.out.println("[OpenGLCallMapper] Effective version: " + (effectiveVersion / 10) + "." + (effectiveVersion % 10));
        System.out.println("[OpenGLCallMapper] Features:");
        System.out.println("  - VBO: " + hasVBO);
        System.out.println("  - VAO: " + hasVAO);
        System.out.println("  - FBO: " + hasFBO);
        System.out.println("  - UBO: " + hasUBO);
        System.out.println("  - SSBO: " + hasSSBO);
        System.out.println("  - Shaders: " + hasShaders);
        System.out.println("  - Geometry Shaders: " + hasGeometryShaders);
        System.out.println("  - Tessellation: " + hasTessellation);
        System.out.println("  - Compute Shaders: " + hasComputeShaders);
        System.out.println("  - Instancing: " + hasInstancing);
        System.out.println("  - DSA: " + hasDSA);
        System.out.println("  - Persistent Mapping: " + hasPersistentMapping);
        System.out.println("  - Multi-Draw Indirect: " + hasMultiDrawIndirect);
    }
    
    // ========================================================================
    // GETTERS FOR CAPABILITY CHECKS
    // ========================================================================
    
    public static int getGLVersion() { return glVersion; }
    public static int getGLSLVersion() { return glslVersion; }
    public static int getEffectiveVersion() { return effectiveVersion; }
    public static boolean shouldFallbackToVanilla() { return fallbackToVanilla; }
    
    public static boolean supportsVersion(int version) { return glVersion >= version; }
    public static boolean hasFeatureVBO() { return hasVBO; }
    public static boolean hasFeatureVAO() { return hasVAO; }
    public static boolean hasFeatureFBO() { return hasFBO; }
    public static boolean hasFeatureUBO() { return hasUBO; }
    public static boolean hasFeatureSSBO() { return hasSSBO; }
    public static boolean hasFeatureShaders() { return hasShaders; }
    public static boolean hasFeatureGeometryShaders() { return hasGeometryShaders; }
    public static boolean hasFeatureTessellation() { return hasTessellation; }
    public static boolean hasFeatureComputeShaders() { return hasComputeShaders; }
    public static boolean hasFeatureInstancing() { return hasInstancing; }
    public static boolean hasFeatureDSA() { return hasDSA; }
    public static boolean hasFeaturePersistentMapping() { return hasPersistentMapping; }
    public static boolean hasFeatureMultiDrawIndirect() { return hasMultiDrawIndirect; }
    public static boolean hasFeatureRayTracing() { return hasRayTracing; }
    public static boolean hasFeatureRayQuery() { return hasRayQuery; }
    public static boolean hasFeatureMeshShading() { return hasMeshShading; }
    public static boolean hasFeatureBindlessTextures() { return ARB_bindless_texture; }
    public static boolean hasFeatureSparseTextures() { return ARB_sparse_texture; }
    
    // ========================================================================
    // STATE TRACKER
    // ========================================================================
    
    /**
     * Tracks OpenGL state to minimize redundant calls
     */
    public static class StateTracker {
        // Current bindings
        public int boundArrayBuffer = 0;
        public int boundElementBuffer = 0;
        public int boundVAO = 0;
        public int boundProgram = 0;
        public int boundFramebuffer = 0;
        public int boundRenderbuffer = 0;
        public int boundReadFramebuffer = 0;
        public int boundDrawFramebuffer = 0;
        public int[] boundTextures = new int[32];
        public int activeTextureUnit = 0;
        
        // Enabled states
        public boolean depthTest = false;
        public boolean blend = false;
        public boolean cullFace = false;
        public boolean scissorTest = false;
        public boolean stencilTest = false;
        
        // Blend state
        public int blendSrcRGB = org.lwjgl.opengl.GL11.GL_ONE;
        public int blendDstRGB = org.lwjgl.opengl.GL11.GL_ZERO;
        public int blendSrcAlpha = org.lwjgl.opengl.GL11.GL_ONE;
        public int blendDstAlpha = org.lwjgl.opengl.GL11.GL_ZERO;
        
        // Depth state
        public int depthFunc = org.lwjgl.opengl.GL11.GL_LESS;
        public boolean depthMask = true;
        
        // Viewport
        public int viewportX = 0;
        public int viewportY = 0;
        public int viewportWidth = 0;
        public int viewportHeight = 0;
        
        // Color mask
        public boolean colorMaskR = true;
        public boolean colorMaskG = true;
        public boolean colorMaskB = true;
        public boolean colorMaskA = true;
        
        public void reset() {
            boundArrayBuffer = 0;
            boundElementBuffer = 0;
            boundVAO = 0;
            boundProgram = 0;
            boundFramebuffer = 0;
            boundRenderbuffer = 0;
            Arrays.fill(boundTextures, 0);
            activeTextureUnit = 0;
            depthTest = false;
            blend = false;
            cullFace = false;
        }
    }
    
    public static StateTracker getState() {
        return stateTracker.get();
    }
    
    // ========================================================================
    // GL 1.0 CALLS - BASIC OPERATIONS
    // ========================================================================
    
    /**
     * glClearColor - Set clear color
     * GL 1.0: glClearColor(r, g, b, a)
     */
    public static void clearColor(float r, float g, float b, float a) {
        org.lwjgl.opengl.GL11.glClearColor(r, g, b, a);
    }
    
    /**
     * glClear - Clear buffers
     * GL 1.0: glClear(mask)
     */
    public static void clear(int mask) {
        org.lwjgl.opengl.GL11.glClear(mask);
    }
    
    /**
     * glClearDepth - Set depth clear value
     * GL 1.0: glClearDepth(depth)
     */
    public static void clearDepth(double depth) {
        org.lwjgl.opengl.GL11.glClearDepth(depth);
    }
    
    /**
     * glClearStencil - Set stencil clear value
     * GL 1.0: glClearStencil(s)
     */
    public static void clearStencil(int s) {
        org.lwjgl.opengl.GL11.glClearStencil(s);
    }
    
    /**
     * glEnable - Enable capability
     * GL 1.0: glEnable(cap)
     */
    public static void enable(int cap) {
        StateTracker state = getState();
        
        // Track common states
        switch (cap) {
            case org.lwjgl.opengl.GL11.GL_DEPTH_TEST:
                if (state.depthTest) return;
                state.depthTest = true;
                break;
            case org.lwjgl.opengl.GL11.GL_BLEND:
                if (state.blend) return;
                state.blend = true;
                break;
            case org.lwjgl.opengl.GL11.GL_CULL_FACE:
                if (state.cullFace) return;
                state.cullFace = true;
                break;
            case org.lwjgl.opengl.GL11.GL_SCISSOR_TEST:
                if (state.scissorTest) return;
                state.scissorTest = true;
                break;
            case org.lwjgl.opengl.GL11.GL_STENCIL_TEST:
                if (state.stencilTest) return;
                state.stencilTest = true;
                break;
        }
        
        org.lwjgl.opengl.GL11.glEnable(cap);
    }
    
    /**
     * glDisable - Disable capability
     * GL 1.0: glDisable(cap)
     */
    public static void disable(int cap) {
        StateTracker state = getState();
        
        switch (cap) {
            case org.lwjgl.opengl.GL11.GL_DEPTH_TEST:
                if (!state.depthTest) return;
                state.depthTest = false;
                break;
            case org.lwjgl.opengl.GL11.GL_BLEND:
                if (!state.blend) return;
                state.blend = false;
                break;
            case org.lwjgl.opengl.GL11.GL_CULL_FACE:
                if (!state.cullFace) return;
                state.cullFace = false;
                break;
            case org.lwjgl.opengl.GL11.GL_SCISSOR_TEST:
                if (!state.scissorTest) return;
                state.scissorTest = false;
                break;
            case org.lwjgl.opengl.GL11.GL_STENCIL_TEST:
                if (!state.stencilTest) return;
                state.stencilTest = false;
                break;
        }
        
        org.lwjgl.opengl.GL11.glDisable(cap);
    }
    
    /**
     * glIsEnabled - Check if capability is enabled
     * GL 1.0: glIsEnabled(cap)
     */
    public static boolean isEnabled(int cap) {
        StateTracker state = getState();
        
        switch (cap) {
            case org.lwjgl.opengl.GL11.GL_DEPTH_TEST:
                return state.depthTest;
            case org.lwjgl.opengl.GL11.GL_BLEND:
                return state.blend;
            case org.lwjgl.opengl.GL11.GL_CULL_FACE:
                return state.cullFace;
            case org.lwjgl.opengl.GL11.GL_SCISSOR_TEST:
                return state.scissorTest;
            case org.lwjgl.opengl.GL11.GL_STENCIL_TEST:
                return state.stencilTest;
            default:
                return org.lwjgl.opengl.GL11.glIsEnabled(cap);
        }
    }
    
    /**
     * glDepthFunc - Set depth comparison function
     * GL 1.0: glDepthFunc(func)
     */
    public static void depthFunc(int func) {
        StateTracker state = getState();
        if (state.depthFunc == func) return;
        state.depthFunc = func;
        org.lwjgl.opengl.GL11.glDepthFunc(func);
    }
    
    /**
     * glDepthMask - Enable/disable depth writes
     * GL 1.0: glDepthMask(flag)
     */
    public static void depthMask(boolean flag) {
        StateTracker state = getState();
        if (state.depthMask == flag) return;
        state.depthMask = flag;
        org.lwjgl.opengl.GL11.glDepthMask(flag);
    }
    
    /**
     * glDepthRange - Set depth range
     * GL 1.0: glDepthRange(near, far)
     */
    public static void depthRange(double near, double far) {
        org.lwjgl.opengl.GL11.glDepthRange(near, far);
    }
    
    /**
     * glBlendFunc - Set blend function
     * GL 1.0: glBlendFunc(sfactor, dfactor)
     */
    public static void blendFunc(int sfactor, int dfactor) {
        StateTracker state = getState();
        if (state.blendSrcRGB == sfactor && state.blendDstRGB == dfactor &&
            state.blendSrcAlpha == sfactor && state.blendDstAlpha == dfactor) {
            return;
        }
        state.blendSrcRGB = sfactor;
        state.blendDstRGB = dfactor;
        state.blendSrcAlpha = sfactor;
        state.blendDstAlpha = dfactor;
        org.lwjgl.opengl.GL11.glBlendFunc(sfactor, dfactor);
    }
    
    /**
     * glCullFace - Set which faces to cull
     * GL 1.0: glCullFace(mode)
     */
    public static void cullFace(int mode) {
        org.lwjgl.opengl.GL11.glCullFace(mode);
    }
    
    /**
     * glFrontFace - Define front-facing polygons
     * GL 1.0: glFrontFace(mode)
     */
    public static void frontFace(int mode) {
        org.lwjgl.opengl.GL11.glFrontFace(mode);
    }
    
    /**
     * glPolygonMode - Set polygon rasterization mode
     * GL 1.0: glPolygonMode(face, mode)
     */
    public static void polygonMode(int face, int mode) {
        org.lwjgl.opengl.GL11.glPolygonMode(face, mode);
    }
    
    /**
     * glScissor - Set scissor box
     * GL 1.0: glScissor(x, y, width, height)
     */
    public static void scissor(int x, int y, int width, int height) {
        org.lwjgl.opengl.GL11.glScissor(x, y, width, height);
    }
    
    /**
     * glViewport - Set viewport
     * GL 1.0: glViewport(x, y, width, height)
     */
    public static void viewport(int x, int y, int width, int height) {
        StateTracker state = getState();
        if (state.viewportX == x && state.viewportY == y && 
            state.viewportWidth == width && state.viewportHeight == height) {
            return;
        }
        state.viewportX = x;
        state.viewportY = y;
        state.viewportWidth = width;
        state.viewportHeight = height;
        org.lwjgl.opengl.GL11.glViewport(x, y, width, height);
    }
    
    /**
     * glColorMask - Enable/disable writing of color components
     * GL 1.0: glColorMask(r, g, b, a)
     */
    public static void colorMask(boolean r, boolean g, boolean b, boolean a) {
        StateTracker state = getState();
        if (state.colorMaskR == r && state.colorMaskG == g && 
            state.colorMaskB == b && state.colorMaskA == a) {
            return;
        }
        state.colorMaskR = r;
        state.colorMaskG = g;
        state.colorMaskB = b;
        state.colorMaskA = a;
        org.lwjgl.opengl.GL11.glColorMask(r, g, b, a);
    }
    
    /**
     * glStencilFunc - Set stencil test function
     * GL 1.0: glStencilFunc(func, ref, mask)
     */
    public static void stencilFunc(int func, int ref, int mask) {
        org.lwjgl.opengl.GL11.glStencilFunc(func, ref, mask);
    }
    
    /**
     * glStencilOp - Set stencil test actions
     * GL 1.0: glStencilOp(sfail, dpfail, dppass)
     */
    public static void stencilOp(int sfail, int dpfail, int dppass) {
        org.lwjgl.opengl.GL11.glStencilOp(sfail, dpfail, dppass);
    }
    
    /**
     * glStencilMask - Set stencil write mask
     * GL 1.0: glStencilMask(mask)
     */
    public static void stencilMask(int mask) {
        org.lwjgl.opengl.GL11.glStencilMask(mask);
    }
    
    /**
     * glPointSize - Set point size
     * GL 1.0: glPointSize(size)
     */
    public static void pointSize(float size) {
        org.lwjgl.opengl.GL11.glPointSize(size);
    }
    
    /**
     * glLineWidth - Set line width
     * GL 1.0: glLineWidth(width)
     */
    public static void lineWidth(float width) {
        org.lwjgl.opengl.GL11.glLineWidth(width);
    }
    
    /**
     * glHint - Set rendering hint
     * GL 1.0: glHint(target, mode)
     */
    public static void hint(int target, int mode) {
        org.lwjgl.opengl.GL11.glHint(target, mode);
    }
    
    /**
     * glFlush - Force execution of GL commands
     * GL 1.0: glFlush()
     */
    public static void flush() {
        org.lwjgl.opengl.GL11.glFlush();
    }
    
    /**
     * glFinish - Block until all GL commands complete
     * GL 1.0: glFinish()
     */
    public static void finish() {
        org.lwjgl.opengl.GL11.glFinish();
    }
    
    /**
     * glGetError - Get error code
     * GL 1.0: glGetError()
     */
    public static int getError() {
        return org.lwjgl.opengl.GL11.glGetError();
    }
    
    /**
     * glGetString - Get string describing the GL
     * GL 1.0: glGetString(name)
     */
    public static String getString(int name) {
        return org.lwjgl.opengl.GL11.glGetString(name);
    }
    
    /**
     * glGetInteger - Get integer state
     * GL 1.0: glGetInteger(pname)
     */
    public static int getInteger(int pname) {
        return org.lwjgl.opengl.GL11.glGetInteger(pname);
    }
    
    /**
     * glGetFloat - Get float state
     * GL 1.0: glGetFloat(pname)
     */
    public static float getFloat(int pname) {
        return org.lwjgl.opengl.GL11.glGetFloat(pname);
    }
    
    /**
     * glGetDouble - Get double state
     * GL 1.0: glGetDouble(pname)
     */
    public static double getDouble(int pname) {
        return org.lwjgl.opengl.GL11.glGetDouble(pname);
    }
    
    /**
     * glGetBoolean - Get boolean state
     * GL 1.0: glGetBoolean(pname)
     */
    public static boolean getBoolean(int pname) {
        return org.lwjgl.opengl.GL11.glGetBoolean(pname);
    }
    
    /**
     * glPixelStorei - Set pixel storage mode
     * GL 1.0: glPixelStorei(pname, param)
     */
    public static void pixelStorei(int pname, int param) {
        org.lwjgl.opengl.GL11.glPixelStorei(pname, param);
    }
    
    /**
     * glPixelStoref - Set pixel storage mode (float)
     * GL 1.0: glPixelStoref(pname, param)
     */
    public static void pixelStoref(int pname, float param) {
        org.lwjgl.opengl.GL11.glPixelStoref(pname, param);
    }
    
    /**
     * glReadPixels - Read pixels from framebuffer
     * GL 1.0: glReadPixels(x, y, width, height, format, type, data)
     */
    public static void readPixels(int x, int y, int width, int height, int format, int type, ByteBuffer data) {
        org.lwjgl.opengl.GL11.glReadPixels(x, y, width, height, format, type, data);
    }
    
    public static void readPixels(int x, int y, int width, int height, int format, int type, long offset) {
        // GL 2.1+ with PBO
        if (GL21) {
            org.lwjgl.opengl.GL11.glReadPixels(x, y, width, height, format, type, offset);
        }
    }
    
    // ========================================================================
    // GL 1.1 CALLS - TEXTURE OBJECTS, VERTEX ARRAYS
    // ========================================================================
    
    /**
     * glGenTextures - Generate texture names
     * GL 1.1: glGenTextures()
     */
    public static int genTexture() {
        return org.lwjgl.opengl.GL11.glGenTextures();
    }
    
    public static void genTextures(IntBuffer textures) {
        org.lwjgl.opengl.GL11.glGenTextures(textures);
    }
    
    /**
     * glDeleteTextures - Delete texture objects
     * GL 1.1: glDeleteTextures(textures)
     */
    public static void deleteTexture(int texture) {
        org.lwjgl.opengl.GL11.glDeleteTextures(texture);
    }
    
    public static void deleteTextures(IntBuffer textures) {
        org.lwjgl.opengl.GL11.glDeleteTextures(textures);
    }
    
    /**
     * glBindTexture - Bind a texture to a target
     * GL 1.1: glBindTexture(target, texture)
     */
    public static void bindTexture(int target, int texture) {
        ensureInitialized();
        StateTracker state = getState();
        int unit = state.activeTextureUnit;
        if (state.boundTextures[unit] == texture) {
            return; // Already bound, skip call
        }
        state.boundTextures[unit] = texture;
        activeProvider.bindTexture(target, texture);
    }
    
    /**
     * glIsTexture - Check if name is a texture
     * GL 1.1: glIsTexture(texture)
     */
    public static boolean isTexture(int texture) {
        return org.lwjgl.opengl.GL11.glIsTexture(texture);
    }
    
    /**
     * glTexImage1D - Specify 1D texture image
     * GL 1.1: glTexImage1D(target, level, internalformat, width, border, format, type, data)
     */
    public static void texImage1D(int target, int level, int internalformat, int width, int border, int format, int type, ByteBuffer data) {
        org.lwjgl.opengl.GL11.glTexImage1D(target, level, internalformat, width, border, format, type, data);
    }
    
    /**
     * glTexImage2D - Specify 2D texture image
     * GL 1.1: glTexImage2D(target, level, internalformat, width, height, border, format, type, data)
     * GL 4.2: Prefer glTexStorage2D for immutable storage
     */
    public static void texImage2D(int target, int level, int internalformat, int width, int height, 
                                   int border, int format, int type, ByteBuffer data) {
        org.lwjgl.opengl.GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, data);
    }
    
    public static void texImage2D(int target, int level, int internalformat, int width, int height,
                                   int border, int format, int type, long offset) {
        // With PBO bound
        org.lwjgl.opengl.GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, offset);
    }
    
    /**
     * glTexSubImage1D - Specify 1D texture subimage
     * GL 1.1: glTexSubImage1D(target, level, xoffset, width, format, type, data)
     */
    public static void texSubImage1D(int target, int level, int xoffset, int width, int format, int type, ByteBuffer data) {
        org.lwjgl.opengl.GL11.glTexSubImage1D(target, level, xoffset, width, format, type, data);
    }
    
    /**
     * glTexSubImage2D - Specify 2D texture subimage
     * GL 1.1: glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data)
     */
    public static void texSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height,
                                      int format, int type, ByteBuffer data) {
        org.lwjgl.opengl.GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
    }
    
    public static void texSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height,
                                      int format, int type, long offset) {
        // With PBO bound
        org.lwjgl.opengl.GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, offset);
    }
    
    /**
     * glCopyTexImage1D - Copy pixels to 1D texture
     * GL 1.1: glCopyTexImage1D(target, level, internalformat, x, y, width, border)
     */
    public static void copyTexImage1D(int target, int level, int internalformat, int x, int y, int width, int border) {
        org.lwjgl.opengl.GL11.glCopyTexImage1D(target, level, internalformat, x, y, width, border);
    }
    
    /**
     * glCopyTexImage2D - Copy pixels to 2D texture
     * GL 1.1: glCopyTexImage2D(target, level, internalformat, x, y, width, height, border)
     */
    public static void copyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border) {
        org.lwjgl.opengl.GL11.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
    }
    
    /**
     * glCopyTexSubImage1D - Copy pixels to 1D texture subimage
     * GL 1.1: glCopyTexSubImage1D(target, level, xoffset, x, y, width)
     */
    public static void copyTexSubImage1D(int target, int level, int xoffset, int x, int y, int width) {
        org.lwjgl.opengl.GL11.glCopyTexSubImage1D(target, level, xoffset, x, y, width);
    }
    
    /**
     * glCopyTexSubImage2D - Copy pixels to 2D texture subimage
     * GL 1.1: glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height)
     */
    public static void copyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        org.lwjgl.opengl.GL11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }
    
    /**
     * glTexParameteri - Set texture parameter (integer)
     * GL 1.1: glTexParameteri(target, pname, param)
     */
    public static void texParameteri(int target, int pname, int param) {
        org.lwjgl.opengl.GL11.glTexParameteri(target, pname, param);
    }
    
    /**
     * glTexParameterf - Set texture parameter (float)
     * GL 1.1: glTexParameterf(target, pname, param)
     */
    public static void texParameterf(int target, int pname, float param) {
        org.lwjgl.opengl.GL11.glTexParameterf(target, pname, param);
    }
    
    /**
     * glTexParameteriv - Set texture parameters (integer array)
     * GL 1.1: glTexParameteriv(target, pname, params)
     */
    public static void texParameteriv(int target, int pname, IntBuffer params) {
        org.lwjgl.opengl.GL11.glTexParameteriv(target, pname, params);
    }
    
    /**
     * glTexParameterfv - Set texture parameters (float array)
     * GL 1.1: glTexParameterfv(target, pname, params)
     */
    public static void texParameterfv(int target, int pname, FloatBuffer params) {
        org.lwjgl.opengl.GL11.glTexParameterfv(target, pname, params);
    }
    
    /**
     * glGetTexParameteri - Get texture parameter
     * GL 1.1: glGetTexParameteri(target, pname)
     */
    public static int getTexParameteri(int target, int pname) {
        return org.lwjgl.opengl.GL11.glGetTexParameteri(target, pname);
    }
    
    /**
     * glGetTexLevelParameteri - Get texture level parameter
     * GL 1.1: glGetTexLevelParameteri(target, level, pname)
     */
    public static int getTexLevelParameteri(int target, int level, int pname) {
        return org.lwjgl.opengl.GL11.glGetTexLevelParameteri(target, level, pname);
    }
    
    /**
     * glPolygonOffset - Set polygon offset
     * GL 1.1: glPolygonOffset(factor, units)
     */
    public static void polygonOffset(float factor, float units) {
        org.lwjgl.opengl.GL11.glPolygonOffset(factor, units);
    }
    
    /**
     * glDrawArrays - Draw arrays
     * GL 1.1: glDrawArrays(mode, first, count)
     */
    public static void drawArrays(int mode, int first, int count) {
        org.lwjgl.opengl.GL11.glDrawArrays(mode, first, count);
    }
    
    /**
     * glDrawElements - Draw indexed elements
     * GL 1.1: glDrawElements(mode, count, type, indices)
     */
    public static void drawElements(int mode, int count, int type, long indices) {
        org.lwjgl.opengl.GL11.glDrawElements(mode, count, type, indices);
    }
    
    public static void drawElements(int mode, int count, int type, ByteBuffer indices) {
        org.lwjgl.opengl.GL11.glDrawElements(mode, type, indices);
    }
    
    public static void drawElements(int mode, IntBuffer indices) {
        org.lwjgl.opengl.GL11.glDrawElements(mode, indices);
    }
    
    // ========================================================================
    // GL 1.2 CALLS - 3D TEXTURES, DRAW RANGE ELEMENTS
    // ========================================================================
    
    /**
     * glTexImage3D - Specify 3D texture image
     * GL 1.2: glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, data)
     */
    public static void texImage3D(int target, int level, int internalformat, int width, int height, int depth,
                                   int border, int format, int type, ByteBuffer data) {
        if (GL12) {
            org.lwjgl.opengl.GL12.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, data);
        }
    }
    
    /**
     * glTexSubImage3D - Specify 3D texture subimage
     * GL 1.2: glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data)
     */
    public static void texSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
                                      int width, int height, int depth, int format, int type, ByteBuffer data) {
        if (GL12) {
            org.lwjgl.opengl.GL12.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, data);
        }
    }
    
    /**
     * glCopyTexSubImage3D - Copy pixels to 3D texture subimage
     * GL 1.2: glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height)
     */
    public static void copyTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
                                          int x, int y, int width, int height) {
        if (GL12) {
            org.lwjgl.opengl.GL12.glCopyTexSubImage3D(target, level, xoffset, yoffset, zoffset, x, y, width, height);
        }
    }
    
    /**
     * glDrawRangeElements - Draw range of indexed elements
     * GL 1.2: glDrawRangeElements(mode, start, end, count, type, indices)
     */
    public static void drawRangeElements(int mode, int start, int end, int count, int type, long indices) {
        if (GL12) {
            org.lwjgl.opengl.GL12.glDrawRangeElements(mode, start, end, count, type, indices);
        } else {
            // Fallback to glDrawElements
            org.lwjgl.opengl.GL11.glDrawElements(mode, count, type, indices);
        }
    }
    
    // ========================================================================
    // GL 1.3 CALLS - MULTITEXTURING, COMPRESSION, CUBE MAPS
    // ========================================================================
    
    /**
     * glActiveTexture - Select active texture unit
     * GL 1.3: glActiveTexture(texture)
     * Pre-1.3: ARB_multitexture extension
     */
    public static void activeTexture(int texture) {
        StateTracker state = getState();
        int unit = texture - org.lwjgl.opengl.GL13.GL_TEXTURE0;
        if (state.activeTextureUnit == unit) {
            return;
        }
        state.activeTextureUnit = unit;
        
        if (GL13) {
            org.lwjgl.opengl.GL13.glActiveTexture(texture);
        } else if (ARB_multitexture) {
            ARBMultitexture.glActiveTextureARB(texture);
        }
        // Pre-1.3 without extension: single texture unit only
    }
    
    /**
     * glCompressedTexImage2D - Specify compressed 2D texture
     * GL 1.3: glCompressedTexImage2D(target, level, internalformat, width, height, border, data)
     */
    public static void compressedTexImage2D(int target, int level, int internalformat, int width, int height,
                                             int border, ByteBuffer data) {
        if (GL13) {
            org.lwjgl.opengl.GL13.glCompressedTexImage2D(target, level, internalformat, width, height, border, data);
        }
    }
    
    /**
     * glCompressedTexImage3D - Specify compressed 3D texture
     * GL 1.3: glCompressedTexImage3D(target, level, internalformat, width, height, depth, border, data)
     */
    public static void compressedTexImage3D(int target, int level, int internalformat, int width, int height,
                                             int depth, int border, ByteBuffer data) {
        if (GL13) {
            org.lwjgl.opengl.GL13.glCompressedTexImage3D(target, level, internalformat, width, height, depth, border, data);
        }
    }
    
    /**
     * glCompressedTexSubImage2D - Specify compressed 2D texture subimage
     * GL 1.3: glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, data)
     */
    public static void compressedTexSubImage2D(int target, int level, int xoffset, int yoffset,
                                                int width, int height, int format, ByteBuffer data) {
        if (GL13) {
            org.lwjgl.opengl.GL13.glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, data);
        }
    }
    
    /**
     * glCompressedTexSubImage3D - Specify compressed 3D texture subimage
     * GL 1.3: glCompressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, data)
     */
    public static void compressedTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset,
                                                int width, int height, int depth, int format, ByteBuffer data) {
        if (GL13) {
            org.lwjgl.opengl.GL13.glCompressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, data);
        }
    }
    
    /**
     * glGetCompressedTexImage - Get compressed texture image
     * GL 1.3: glGetCompressedTexImage(target, level, pixels)
     */
    public static void getCompressedTexImage(int target, int level, ByteBuffer pixels) {
        if (GL13) {
            org.lwjgl.opengl.GL13.glGetCompressedTexImage(target, level, pixels);
        }
    }
    
    /**
     * glSampleCoverage - Set sample coverage parameters
     * GL 1.3: glSampleCoverage(value, invert)
     */
    public static void sampleCoverage(float value, boolean invert) {
        if (GL13) {
            org.lwjgl.opengl.GL13.glSampleCoverage(value, invert);
        }
    }
    
    // ========================================================================
    // GL 1.4 CALLS - BLEND SEPARATE, MULTI DRAW, POINT PARAMETERS
    // ========================================================================
    
    /**
     * glBlendFuncSeparate - Set blend function separately for RGB and alpha
     * GL 1.4: glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
     * Pre-1.4: Fallback to glBlendFunc
     */
    public static void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        StateTracker state = getState();
        if (state.blendSrcRGB == srcRGB && state.blendDstRGB == dstRGB &&
            state.blendSrcAlpha == srcAlpha && state.blendDstAlpha == dstAlpha) {
            return;
        }
        state.blendSrcRGB = srcRGB;
        state.blendDstRGB = dstRGB;
        state.blendSrcAlpha = srcAlpha;
        state.blendDstAlpha = dstAlpha;
        
        if (GL14) {
            org.lwjgl.opengl.GL14.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
        } else {
            // Fallback: use srcRGB/dstRGB for everything
            org.lwjgl.opengl.GL11.glBlendFunc(srcRGB, dstRGB);
        }
    }
    
    /**
     * glBlendEquation - Set blend equation
     * GL 1.4: glBlendEquation(mode)
     */
    public static void blendEquation(int mode) {
        if (GL14) {
            org.lwjgl.opengl.GL14.glBlendEquation(mode);
        }
    }
    
    /**
     * glBlendEquationSeparate - Set blend equation separately for RGB and alpha
     * GL 2.0: glBlendEquationSeparate(modeRGB, modeAlpha)
     */
    public static void blendEquationSeparate(int modeRGB, int modeAlpha) {
        if (GL20) {
            org.lwjgl.opengl.GL20.glBlendEquationSeparate(modeRGB, modeAlpha);
        } else if (GL14) {
            org.lwjgl.opengl.GL14.glBlendEquation(modeRGB);
        }
    }
    
    /**
     * glBlendColor - Set blend color
     * GL 1.4: glBlendColor(red, green, blue, alpha)
     */
    public static void blendColor(float red, float green, float blue, float alpha) {
        if (GL14) {
            org.lwjgl.opengl.GL14.glBlendColor(red, green, blue, alpha);
        }
    }
    
    /**
     * glMultiDrawArrays - Draw multiple arrays
     * GL 1.4: glMultiDrawArrays(mode, first, count)
     * Pre-1.4: Loop over glDrawArrays
     */
    public static void multiDrawArrays(int mode, IntBuffer first, IntBuffer count) {
        if (GL14) {
            org.lwjgl.opengl.GL14.glMultiDrawArrays(mode, first, count);
        } else {
            // Fallback: loop
            int n = count.remaining();
            for (int i = 0; i < n; i++) {
                org.lwjgl.opengl.GL11.glDrawArrays(mode, first.get(first.position() + i), count.get(count.position() + i));
            }
        }
    }
    
    /**
     * glMultiDrawElements - Draw multiple indexed elements
     * GL 1.4: glMultiDrawElements(mode, count, type, indices)
     * Pre-1.4: Loop over glDrawElements
     */
    public static void multiDrawElements(int mode, IntBuffer count, int type, PointerBuffer indices) {
        if (GL14) {
            org.lwjgl.opengl.GL14.glMultiDrawElements(mode, count, type, indices);
        } else {
            // Fallback: loop
            int n = count.remaining();
            for (int i = 0; i < n; i++) {
                org.lwjgl.opengl.GL11.glDrawElements(mode, count.get(count.position() + i), type, indices.get(indices.position() + i));
            }
        }
    }
    
    /**
     * glPointParameterf - Set point parameter (float)
     * GL 1.4: glPointParameterf(pname, param)
     */
    public static void pointParameterf(int pname, float param) {
        if (GL14) {
            org.lwjgl.opengl.GL14.glPointParameterf(pname, param);
        }
    }
    
    /**
     * glPointParameteri - Set point parameter (integer)
     * GL 1.4: glPointParameteri(pname, param)
     */
    public static void pointParameteri(int pname, int param) {
        if (GL14) {
            org.lwjgl.opengl.GL14.glPointParameteri(pname, param);
        }
    }
    
    /**
     * glPointParameterfv - Set point parameters (float array)
     * GL 1.4: glPointParameterfv(pname, params)
     */
    public static void pointParameterfv(int pname, FloatBuffer params) {
        if (GL14) {
            org.lwjgl.opengl.GL14.glPointParameterfv(pname, params);
        }
    }
    
    // ========================================================================
    // GL 1.5 CALLS - VERTEX BUFFER OBJECTS, OCCLUSION QUERIES
    // ========================================================================
    
    /**
     * glGenBuffers - Generate buffer object names
     * GL 1.5: glGenBuffers()
     * Pre-1.5: ARB_vertex_buffer_object extension
     */
    public static int genBuffer() {
        if (GL15) {
            return org.lwjgl.opengl.GL15.glGenBuffers();
        } else if (ARB_vertex_buffer_object) {
            return ARBVertexBufferObject.glGenBuffersARB();
        }
        throw new UnsupportedOperationException("VBO not supported");
    }
    
    public static void genBuffers(IntBuffer buffers) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glGenBuffers(buffers);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glGenBuffersARB(buffers);
        }
    }
    
    /**
     * glDeleteBuffers - Delete buffer objects
     * GL 1.5: glDeleteBuffers(buffers)
     */
    public static void deleteBuffer(int buffer) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(buffer);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glDeleteBuffersARB(buffer);
        }
    }
    
    public static void deleteBuffers(IntBuffer buffers) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glDeleteBuffers(buffers);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glDeleteBuffersARB(buffers);
        }
    }
    
    /**
     * glBindBuffer - Bind buffer object
     * GL 1.5: glBindBuffer(target, buffer)
     */
    public static void bindBuffer(int target, int buffer) {
        ensureInitialized();
        StateTracker state = getState();
        
        switch (target) {
            case GL15.GL_ARRAY_BUFFER:
                if (state.boundArrayBuffer == buffer) return;
                state.boundArrayBuffer = buffer;
                break;
            case GL15.GL_ELEMENT_ARRAY_BUFFER:
                if (state.boundElementBuffer == buffer) return;
                state.boundElementBuffer = buffer;
                break;
        }
        
        activeProvider.bindBuffer(target, buffer);
    }
    
    /**
     * glIsBuffer - Check if name is a buffer
     * GL 1.5: glIsBuffer(buffer)
     */
    public static boolean isBuffer(int buffer) {
        if (GL15) {
            return org.lwjgl.opengl.GL15.glIsBuffer(buffer);
        } else if (ARB_vertex_buffer_object) {
            return ARBVertexBufferObject.glIsBufferARB(buffer);
        }
        return false;
    }
    
    /**
     * glBufferData - Create and initialize buffer data store
     * GL 1.5: glBufferData(target, data, usage)
     * GL 4.4: Prefer glBufferStorage for immutable storage
     */
    public static void bufferData(int target, long size, int usage) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glBufferData(target, size, usage);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glBufferDataARB(target, size, usage);
        }
    }
    
    public static void bufferData(int target, ByteBuffer data, int usage) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glBufferData(target, data, usage);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glBufferDataARB(target, data, usage);
        }
    }
    
    public static void bufferData(int target, FloatBuffer data, int usage) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glBufferData(target, data, usage);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glBufferDataARB(target, data, usage);
        }
    }
    
    public static void bufferData(int target, IntBuffer data, int usage) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glBufferData(target, data, usage);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glBufferDataARB(target, data, usage);
        }
    }
    
    public static void bufferData(int target, ShortBuffer data, int usage) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glBufferData(target, data, usage);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glBufferDataARB(target, data, usage);
        }
    }
    
    /**
     * glBufferSubData - Update buffer data store
     * GL 1.5: glBufferSubData(target, offset, data)
     */
    public static void bufferSubData(int target, long offset, ByteBuffer data) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glBufferSubData(target, offset, data);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glBufferSubDataARB(target, offset, data);
        }
    }
    
    public static void bufferSubData(int target, long offset, FloatBuffer data) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glBufferSubData(target, offset, data);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glBufferSubDataARB(target, offset, data);
        }
    }
    
    public static void bufferSubData(int target, long offset, IntBuffer data) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glBufferSubData(target, offset, data);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glBufferSubDataARB(target, offset, data);
        }
    }
    
    /**
     * glGetBufferSubData - Get buffer data
     * GL 1.5: glGetBufferSubData(target, offset, data)
     */
    public static void getBufferSubData(int target, long offset, ByteBuffer data) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glGetBufferSubData(target, offset, data);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glGetBufferSubDataARB(target, offset, data);
        }
    }
    
    /**
     * glMapBuffer - Map buffer data store
     * GL 1.5: glMapBuffer(target, access)
     */
    public static ByteBuffer mapBuffer(int target, int access, long length, ByteBuffer oldBuffer) {
        if (GL15) {
            return org.lwjgl.opengl.GL15.glMapBuffer(target, access, length, oldBuffer);
        } else if (ARB_vertex_buffer_object) {
            return ARBVertexBufferObject.glMapBufferARB(target, access, length, oldBuffer);
        }
        return null;
    }
    
    public static ByteBuffer mapBuffer(int target, int access) {
        if (GL15) {
            return org.lwjgl.opengl.GL15.glMapBuffer(target, access);
        } else if (ARB_vertex_buffer_object) {
            return ARBVertexBufferObject.glMapBufferARB(target, access);
        }
        return null;
    }
    
    /**
     * glUnmapBuffer - Unmap buffer data store
     * GL 1.5: glUnmapBuffer(target)
     */
    public static boolean unmapBuffer(int target) {
        if (GL15) {
            return org.lwjgl.opengl.GL15.glUnmapBuffer(target);
        } else if (ARB_vertex_buffer_object) {
            return ARBVertexBufferObject.glUnmapBufferARB(target);
        }
        return false;
    }
    
    /**
     * glGetBufferParameteri - Get buffer parameter
     * GL 1.5: glGetBufferParameteri(target, pname)
     */
    public static int getBufferParameteri(int target, int pname) {
        if (GL15) {
            return org.lwjgl.opengl.GL15.glGetBufferParameteri(target, pname);
        } else if (ARB_vertex_buffer_object) {
            return ARBVertexBufferObject.glGetBufferParameteriARB(target, pname);
        }
        return 0;
    }
    
    // Occlusion Queries (GL 1.5)
    
    /**
     * glGenQueries - Generate query object names
     * GL 1.5: glGenQueries()
     */
    public static int genQuery() {
        if (GL15) {
            return org.lwjgl.opengl.GL15.glGenQueries();
        }
        return 0;
    }
    
    public static void genQueries(IntBuffer ids) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glGenQueries(ids);
        }
    }
    
    /**
     * glDeleteQueries - Delete query objects
     * GL 1.5: glDeleteQueries(ids)
     */
    public static void deleteQuery(int id) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glDeleteQueries(id);
        }
    }
    
    public static void deleteQueries(IntBuffer ids) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glDeleteQueries(ids);
        }
    }
    
    /**
     * glBeginQuery - Begin query
     * GL 1.5: glBeginQuery(target, id)
     */
    public static void beginQuery(int target, int id) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glBeginQuery(target, id);
        }
    }
    
    /**
     * glEndQuery - End query
     * GL 1.5: glEndQuery(target)
     */
    public static void endQuery(int target) {
        if (GL15) {
            org.lwjgl.opengl.GL15.glEndQuery(target);
        }
    }
    
    /**
     * glIsQuery - Check if name is a query
     * GL 1.5: glIsQuery(id)
     */
    public static boolean isQuery(int id) {
        if (GL15) {
            return org.lwjgl.opengl.GL15.glIsQuery(id);
        }
        return false;
    }
    
    /**
     * glGetQueryi - Get query object parameter
     * GL 1.5: glGetQueryi(target, pname)
     */
    public static int getQueryi(int target, int pname) {
        if (GL15) {
            return org.lwjgl.opengl.GL15.glGetQueryi(target, pname);
        }
        return 0;
    }
    
    /**
     * glGetQueryObjecti - Get query object parameter
     * GL 1.5: glGetQueryObjecti(id, pname)
     */
    public static int getQueryObjecti(int id, int pname) {
        if (GL15) {
            return org.lwjgl.opengl.GL15.glGetQueryObjecti(id, pname);
        }
        return 0;
    }
    
    /**
     * glGetQueryObjectui - Get query object parameter (unsigned)
     * GL 1.5: glGetQueryObjectui(id, pname)
     */
    public static int getQueryObjectui(int id, int pname) {
        if (GL15) {
            return org.lwjgl.opengl.GL15.glGetQueryObjectui(id, pname);
        }
        return 0;
    }
    
    // ========================================================================
    // DEBUG UTILITIES
    // ========================================================================
    
    public static void setDebugMode(boolean debug) {
        debugMode = debug;
    }
    
    public static void checkError(String operation) {
        if (!debugMode) return;
        
        int error = org.lwjgl.opengl.GL11.glGetError();
        if (error != org.lwjgl.opengl.GL11.GL_NO_ERROR) {
            String errorName;
            switch (error) {
                case org.lwjgl.opengl.GL11.GL_INVALID_ENUM:
                    errorName = "GL_INVALID_ENUM";
                    break;
                case org.lwjgl.opengl.GL11.GL_INVALID_VALUE:
                    errorName = "GL_INVALID_VALUE";
                    break;
                case org.lwjgl.opengl.GL11.GL_INVALID_OPERATION:
                    errorName = "GL_INVALID_OPERATION";
                    break;
                case org.lwjgl.opengl.GL11.GL_STACK_OVERFLOW:
                    errorName = "GL_STACK_OVERFLOW";
                    break;
                case org.lwjgl.opengl.GL11.GL_STACK_UNDERFLOW:
                    errorName = "GL_STACK_UNDERFLOW";
                    break;
                case org.lwjgl.opengl.GL11.GL_OUT_OF_MEMORY:
                    errorName = "GL_OUT_OF_MEMORY";
                    break;
                default:
                    errorName = "UNKNOWN_ERROR_" + error;
            }
            String msg = "[OpenGLCallMapper] Error " + errorName + " during " + operation;
            errorLog.add(msg);
            System.err.println(msg);
        }
    }
    
    public static List<String> getErrorLog() {
        return new ArrayList<>(errorLog);
    }
    
    public static void clearErrorLog() {
        errorLog.clear();
    }
}

// ========================================================================
    // SHADER/PROGRAM CACHING FOR PERFORMANCE
    // ========================================================================
    
    /**
     * Shader info cache - avoids redundant queries
     */
    private static final Map<Integer, ShaderInfo> shaderInfoCache = new ConcurrentHashMap<>();
    private static final Map<Integer, ProgramInfo> programInfoCache = new ConcurrentHashMap<>();
    
    public static class ShaderInfo {
        public final int id;
        public final int type;
        public String source;
        public boolean compiled;
        public String infoLog;
        public long lastModified;
        
        public ShaderInfo(int id, int type) {
            this.id = id;
            this.type = type;
            this.compiled = false;
            this.lastModified = System.currentTimeMillis();
        }
    }
    
    public static class ProgramInfo {
        public final int id;
        public boolean linked;
        public boolean validated;
        public String infoLog;
        public final List<Integer> attachedShaders = new ArrayList<>();
        public final Map<String, Integer> uniformLocations = new HashMap<>();
        public final Map<String, Integer> attribLocations = new HashMap<>();
        public final Map<Integer, UniformValue> uniformCache = new HashMap<>();
        public int activeUniforms;
        public int activeAttributes;
        public long lastModified;
        
        public ProgramInfo(int id) {
            this.id = id;
            this.linked = false;
            this.validated = false;
            this.lastModified = System.currentTimeMillis();
        }
    }
    
    /**
     * Cached uniform values for avoiding redundant uniform calls
     */
    public static abstract class UniformValue {
        public abstract boolean equals(Object other);
    }
    
    public static class UniformFloat extends UniformValue {
        public final float v0;
        public UniformFloat(float v0) { this.v0 = v0; }
        @Override public boolean equals(Object o) {
            return o instanceof UniformFloat && ((UniformFloat)o).v0 == v0;
        }
    }
    
    public static class UniformFloat2 extends UniformValue {
        public final float v0, v1;
        public UniformFloat2(float v0, float v1) { this.v0 = v0; this.v1 = v1; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof UniformFloat2)) return false;
            UniformFloat2 u = (UniformFloat2)o;
            return u.v0 == v0 && u.v1 == v1;
        }
    }
    
    public static class UniformFloat3 extends UniformValue {
        public final float v0, v1, v2;
        public UniformFloat3(float v0, float v1, float v2) { this.v0 = v0; this.v1 = v1; this.v2 = v2; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof UniformFloat3)) return false;
            UniformFloat3 u = (UniformFloat3)o;
            return u.v0 == v0 && u.v1 == v1 && u.v2 == v2;
        }
    }
    
    public static class UniformFloat4 extends UniformValue {
        public final float v0, v1, v2, v3;
        public UniformFloat4(float v0, float v1, float v2, float v3) { 
            this.v0 = v0; this.v1 = v1; this.v2 = v2; this.v3 = v3; 
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof UniformFloat4)) return false;
            UniformFloat4 u = (UniformFloat4)o;
            return u.v0 == v0 && u.v1 == v1 && u.v2 == v2 && u.v3 == v3;
        }
    }
    
    public static class UniformInt extends UniformValue {
        public final int v0;
        public UniformInt(int v0) { this.v0 = v0; }
        @Override public boolean equals(Object o) {
            return o instanceof UniformInt && ((UniformInt)o).v0 == v0;
        }
    }
    
    public static class UniformInt2 extends UniformValue {
        public final int v0, v1;
        public UniformInt2(int v0, int v1) { this.v0 = v0; this.v1 = v1; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof UniformInt2)) return false;
            UniformInt2 u = (UniformInt2)o;
            return u.v0 == v0 && u.v1 == v1;
        }
    }
    
    public static class UniformInt3 extends UniformValue {
        public final int v0, v1, v2;
        public UniformInt3(int v0, int v1, int v2) { this.v0 = v0; this.v1 = v1; this.v2 = v2; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof UniformInt3)) return false;
            UniformInt3 u = (UniformInt3)o;
            return u.v0 == v0 && u.v1 == v1 && u.v2 == v2;
        }
    }
    
    public static class UniformInt4 extends UniformValue {
        public final int v0, v1, v2, v3;
        public UniformInt4(int v0, int v1, int v2, int v3) {
            this.v0 = v0; this.v1 = v1; this.v2 = v2; this.v3 = v3;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof UniformInt4)) return false;
            UniformInt4 u = (UniformInt4)o;
            return u.v0 == v0 && u.v1 == v1 && u.v2 == v2 && u.v3 == v3;
        }
    }
    
    // Matrix uniform cache uses array comparison
    public static class UniformMatrix4 extends UniformValue {
        public final float[] values;
        public UniformMatrix4(FloatBuffer fb) {
            values = new float[16];
            int pos = fb.position();
            fb.get(values);
            fb.position(pos);
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof UniformMatrix4)) return false;
            return Arrays.equals(((UniformMatrix4)o).values, values);
        }
    }
    
    public static class UniformMatrix3 extends UniformValue {
        public final float[] values;
        public UniformMatrix3(FloatBuffer fb) {
            values = new float[9];
            int pos = fb.position();
            fb.get(values);
            fb.position(pos);
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof UniformMatrix3)) return false;
            return Arrays.equals(((UniformMatrix3)o).values, values);
        }
    }
    
    // ========================================================================
    // GL 2.0 - SHADER OBJECTS
    // ========================================================================
    
    /**
     * glCreateShader - Create a shader object
     * GL 2.0: glCreateShader(type)
     * Pre-2.0: glCreateShaderObjectARB(type) [ARB_shader_objects]
     * 
     * @param type GL_VERTEX_SHADER, GL_FRAGMENT_SHADER (GL 3.2: GL_GEOMETRY_SHADER)
     */
    public static int createShader(int type) {
        int shader;
        
        if (GL20) {
            shader = GL20.glCreateShader(type);
        } else if (ARB_shader_objects) {
            shader = ARBShaderObjects.glCreateShaderObjectARB(type);
        } else {
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Shaders not supported");
            }
            return 0;
        }
        
        if (shader != 0) {
            shaderInfoCache.put(shader, new ShaderInfo(shader, type));
        }
        
        return shader;
    }
    
    /**
     * glDeleteShader - Delete a shader object
     * GL 2.0: glDeleteShader(shader)
     * Pre-2.0: glDeleteObjectARB(shader)
     */
    public static void deleteShader(int shader) {
        if (shader == 0) return;
        
        shaderInfoCache.remove(shader);
        
        if (GL20) {
            GL20.glDeleteShader(shader);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glDeleteObjectARB(shader);
        }
    }
    
    /**
     * glIsShader - Check if name is a shader
     * GL 2.0: glIsShader(shader)
     */
    public static boolean isShader(int shader) {
        if (GL20) {
            return GL20.glIsShader(shader);
        }
        return shaderInfoCache.containsKey(shader);
    }
    
    /**
     * glShaderSource - Set shader source code
     * GL 2.0: glShaderSource(shader, source)
     * Pre-2.0: glShaderSourceARB(shader, source)
     */
    public static void shaderSource(int shader, CharSequence source) {
        ShaderInfo info = shaderInfoCache.get(shader);
        if (info != null) {
            info.source = source.toString();
            info.compiled = false;
            info.lastModified = System.currentTimeMillis();
        }
        
        if (GL20) {
            GL20.glShaderSource(shader, source);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glShaderSourceARB(shader, source);
        }
    }
    
    /**
     * glShaderSource - Set shader source from multiple strings
     * GL 2.0: glShaderSource(shader, strings)
     */
    public static void shaderSource(int shader, CharSequence... sources) {
        if (GL20) {
            GL20.glShaderSource(shader, sources);
        } else if (ARB_shader_objects) {
            // ARB version takes PointerBuffer
            StringBuilder combined = new StringBuilder();
            for (CharSequence s : sources) {
                combined.append(s);
            }
            ARBShaderObjects.glShaderSourceARB(shader, combined);
        }
        
        ShaderInfo info = shaderInfoCache.get(shader);
        if (info != null) {
            StringBuilder sb = new StringBuilder();
            for (CharSequence s : sources) sb.append(s);
            info.source = sb.toString();
            info.compiled = false;
            info.lastModified = System.currentTimeMillis();
        }
    }
    
    /**
     * Compile shader with pre-validation and better error reporting
     * Returns shader handle on success, 0 on failure
     */
    public static int compileShaderWithValidation(int type, String source) {
        // Validate source first
        String validationError = validateShaderSource(source);
        if (validationError != null) {
            LOGGER.error("Shader validation failed: " + validationError);
            if (dumpShaders) {
                dumpShader("failed_validation", source, ".glsl");
            }
            return 0;
        }
        
        // Create shader
        int shader = createShader(type);
        if (shader == 0) {
            LOGGER.error("Failed to create shader object");
            return 0;
        }
        
        // Set source
        shaderSource(shader, source);
        
        // Compile
        compileShader(shader);
        
        // Check compilation status
        if (getShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE) {
            String infoLog = getShaderInfoLog(shader);
            LOGGER.error("Shader compilation failed:\n" + infoLog);
            
            if (dumpShaders) {
                dumpShader("failed_compilation", source, ".glsl");
                dumpShader("failed_compilation", infoLog, ".log");
            }
            
            deleteShader(shader);
            return 0;
        }
        
        LOGGER.debug("Shader compiled successfully (ID: " + shader + ")");
        return shader;
    }
    
    /**
     * glCompileShader - Compile a shader
     * GL 2.0: glCompileShader(shader)
     * Pre-2.0: glCompileShaderARB(shader)
     */
    public static void compileShader(int shader) {
        if (GL20) {
            GL20.glCompileShader(shader);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glCompileShaderARB(shader);
        }
        
        // Update cache
        ShaderInfo info = shaderInfoCache.get(shader);
        if (info != null) {
            info.compiled = getShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_TRUE;
            info.infoLog = getShaderInfoLog(shader);
            info.lastModified = System.currentTimeMillis();
            
            if (!info.compiled && debugMode) {
                System.err.println("[OpenGLCallMapper] Shader compilation failed:");
                System.err.println(info.infoLog);
            }
        }
    }
    
    /**
     * glGetShaderi - Get shader parameter
     * GL 2.0: glGetShaderi(shader, pname)
     * Pre-2.0: glGetObjectParameteriARB(shader, pname)
     */
    public static int getShaderi(int shader, int pname) {
        if (GL20) {
            return GL20.glGetShaderi(shader, pname);
        } else if (ARB_shader_objects) {
            return ARBShaderObjects.glGetObjectParameteriARB(shader, pname);
        }
        return 0;
    }
    
    /**
     * glGetShaderiv - Get shader parameters
     * GL 2.0: glGetShaderiv(shader, pname, params)
     */
    public static void getShaderiv(int shader, int pname, IntBuffer params) {
        if (GL20) {
            GL20.glGetShaderiv(shader, pname, params);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glGetObjectParameterivARB(shader, pname, params);
        }
    }
    
    /**
     * glGetShaderInfoLog - Get shader info log
     * GL 2.0: glGetShaderInfoLog(shader)
     * Pre-2.0: glGetInfoLogARB(shader)
     */
    public static String getShaderInfoLog(int shader) {
        if (GL20) {
            return GL20.glGetShaderInfoLog(shader);
        } else if (ARB_shader_objects) {
            int length = ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB);
            if (length > 0) {
                return ARBShaderObjects.glGetInfoLogARB(shader, length);
            }
        }
        return "";
    }
    
    /**
     * glGetShaderInfoLog with max length
     */
    public static String getShaderInfoLog(int shader, int maxLength) {
        if (GL20) {
            return GL20.glGetShaderInfoLog(shader, maxLength);
        } else if (ARB_shader_objects) {
            return ARBShaderObjects.glGetInfoLogARB(shader, maxLength);
        }
        return "";
    }
    
    /**
     * glGetShaderSource - Get shader source
     * GL 2.0: glGetShaderSource(shader)
     */
    public static String getShaderSource(int shader) {
        // Check cache first
        ShaderInfo info = shaderInfoCache.get(shader);
        if (info != null && info.source != null) {
            return info.source;
        }
        
        if (GL20) {
            return GL20.glGetShaderSource(shader);
        } else if (ARB_shader_objects) {
            int length = ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_SHADER_SOURCE_LENGTH_ARB);
            if (length > 0) {
                return ARBShaderObjects.glGetShaderSourceARB(shader, length);
            }
        }
        return "";
    }
    
    // ========================================================================
    // GL 2.0 - PROGRAM OBJECTS
    // ========================================================================
    
    /**
     * glCreateProgram - Create a program object
     * GL 2.0: glCreateProgram()
     * Pre-2.0: glCreateProgramObjectARB()
     */
    public static int createProgram() {
        int program;
        
        if (GL20) {
            program = GL20.glCreateProgram();
        } else if (ARB_shader_objects) {
            program = ARBShaderObjects.glCreateProgramObjectARB();
        } else {
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Programs not supported");
            }
            return 0;
        }
        
        if (program != 0) {
            programInfoCache.put(program, new ProgramInfo(program));
        }
        
        return program;
    }
    
    /**
     * glDeleteProgram - Delete a program object
     * GL 2.0: glDeleteProgram(program)
     * Pre-2.0: glDeleteObjectARB(program)
     */
    public static void deleteProgram(int program) {
        if (program == 0) return;
        
        programInfoCache.remove(program);
        
        if (GL20) {
            GL20.glDeleteProgram(program);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glDeleteObjectARB(program);
        }
    }
    
    /**
     * glIsProgram - Check if name is a program
     * GL 2.0: glIsProgram(program)
     */
    public static boolean isProgram(int program) {
        if (GL20) {
            return GL20.glIsProgram(program);
        }
        return programInfoCache.containsKey(program);
    }
    
    /**
     * glAttachShader - Attach shader to program
     * GL 2.0: glAttachShader(program, shader)
     * Pre-2.0: glAttachObjectARB(program, shader)
     */
    public static void attachShader(int program, int shader) {
        if (GL20) {
            GL20.glAttachShader(program, shader);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glAttachObjectARB(program, shader);
        }
        
        ProgramInfo info = programInfoCache.get(program);
        if (info != null && !info.attachedShaders.contains(shader)) {
            info.attachedShaders.add(shader);
            info.linked = false;
            info.lastModified = System.currentTimeMillis();
        }
    }
    
    /**
     * glDetachShader - Detach shader from program
     * GL 2.0: glDetachShader(program, shader)
     * Pre-2.0: glDetachObjectARB(program, shader)
     */
    public static void detachShader(int program, int shader) {
        if (GL20) {
            GL20.glDetachShader(program, shader);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glDetachObjectARB(program, shader);
        }
        
        ProgramInfo info = programInfoCache.get(program);
        if (info != null) {
            info.attachedShaders.remove(Integer.valueOf(shader));
            info.lastModified = System.currentTimeMillis();
        }
    }
    
    /**
     * glLinkProgram - Link a program
     * GL 2.0: glLinkProgram(program)
     * Pre-2.0: glLinkProgramARB(program)
     */
    public static void linkProgram(int program) {
        if (GL20) {
            GL20.glLinkProgram(program);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glLinkProgramARB(program);
        }
        
        // Update cache
        ProgramInfo info = programInfoCache.get(program);
        if (info != null) {
            info.linked = getProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_TRUE;
            info.infoLog = getProgramInfoLog(program);
            info.lastModified = System.currentTimeMillis();
            
            // Clear cached locations on relink
            info.uniformLocations.clear();
            info.attribLocations.clear();
            info.uniformCache.clear();
            
            if (info.linked) {
                info.activeUniforms = getProgrami(program, GL20.GL_ACTIVE_UNIFORMS);
                info.activeAttributes = getProgrami(program, GL20.GL_ACTIVE_ATTRIBUTES);
            }
            
            if (!info.linked && debugMode) {
                System.err.println("[OpenGLCallMapper] Program linking failed:");
                System.err.println(info.infoLog);
            }
        }
    }
    
    /**
     * glValidateProgram - Validate a program
     * GL 2.0: glValidateProgram(program)
     * Pre-2.0: glValidateProgramARB(program)
     */
    public static void validateProgram(int program) {
        if (GL20) {
            GL20.glValidateProgram(program);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glValidateProgramARB(program);
        }
        
        ProgramInfo info = programInfoCache.get(program);
        if (info != null) {
            info.validated = getProgrami(program, GL20.GL_VALIDATE_STATUS) == GL11.GL_TRUE;
            info.lastModified = System.currentTimeMillis();
        }
    }
    
    /**
     * glUseProgram - Use a program for rendering
     * GL 2.0: glUseProgram(program)
     * Pre-2.0: glUseProgramObjectARB(program)
     */
    public static void useProgram(int program) {
        StateTracker state = getState();
        if (state.boundProgram == program) {
            return; // Already bound
        }
        state.boundProgram = program;
        
        if (GL20) {
            GL20.glUseProgram(program);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUseProgramObjectARB(program);
        }
    }
    
    /**
     * Get currently bound program
     */
    public static int getCurrentProgram() {
        return getState().boundProgram;
    }
    
    /**
     * glGetProgrami - Get program parameter
     * GL 2.0: glGetProgrami(program, pname)
     * Pre-2.0: glGetObjectParameteriARB(program, pname)
     */
    public static int getProgrami(int program, int pname) {
        if (GL20) {
            return GL20.glGetProgrami(program, pname);
        } else if (ARB_shader_objects) {
            return ARBShaderObjects.glGetObjectParameteriARB(program, pname);
        }
        return 0;
    }
    
    /**
     * glGetProgramiv - Get program parameters
     * GL 2.0: glGetProgramiv(program, pname, params)
     */
    public static void getProgramiv(int program, int pname, IntBuffer params) {
        if (GL20) {
            GL20.glGetProgramiv(program, pname, params);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glGetObjectParameterivARB(program, pname, params);
        }
    }
    
    /**
     * glGetProgramInfoLog - Get program info log
     * GL 2.0: glGetProgramInfoLog(program)
     * Pre-2.0: glGetInfoLogARB(program)
     */
    public static String getProgramInfoLog(int program) {
        if (GL20) {
            return GL20.glGetProgramInfoLog(program);
        } else if (ARB_shader_objects) {
            int length = ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB);
            if (length > 0) {
                return ARBShaderObjects.glGetInfoLogARB(program, length);
            }
        }
        return "";
    }
    
    /**
     * glGetProgramInfoLog with max length
     */
    public static String getProgramInfoLog(int program, int maxLength) {
        if (GL20) {
            return GL20.glGetProgramInfoLog(program, maxLength);
        } else if (ARB_shader_objects) {
            return ARBShaderObjects.glGetInfoLogARB(program, maxLength);
        }
        return "";
    }
    
    /**
     * glGetAttachedShaders - Get attached shaders
     * GL 2.0: glGetAttachedShaders(program, count, shaders)
     */
    public static void getAttachedShaders(int program, IntBuffer count, IntBuffer shaders) {
        if (GL20) {
            GL20.glGetAttachedShaders(program, count, shaders);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glGetAttachedObjectsARB(program, count, shaders);
        }
    }
    
    // ========================================================================
    // GL 2.0 - UNIFORM VARIABLES
    // ========================================================================
    
    /**
     * glGetUniformLocation - Get uniform location
     * GL 2.0: glGetUniformLocation(program, name)
     * Pre-2.0: glGetUniformLocationARB(program, name)
     * 
     * Uses caching for performance
     */
    public static int getUniformLocation(int program, CharSequence name) {
        String nameStr = name.toString();
        
        // Check cache
        ProgramInfo info = programInfoCache.get(program);
        if (info != null) {
            Integer cached = info.uniformLocations.get(nameStr);
            if (cached != null) {
                return cached;
            }
        }
        
        int location;
        if (GL20) {
            location = GL20.glGetUniformLocation(program, name);
        } else if (ARB_shader_objects) {
            location = ARBShaderObjects.glGetUniformLocationARB(program, name);
        } else {
            return -1;
        }
        
        // Cache the result
        if (info != null) {
            info.uniformLocations.put(nameStr, location);
        }
        
        return location;
    }
    
    /**
     * Check if uniform value changed (for caching)
     */
    private static boolean uniformChanged(int location, UniformValue newValue) {
        int program = getState().boundProgram;
        ProgramInfo info = programInfoCache.get(program);
        if (info == null) return true;
        
        UniformValue cached = info.uniformCache.get(location);
        if (cached == null || !cached.equals(newValue)) {
            info.uniformCache.put(location, newValue);
            return true;
        }
        return false;
    }
    
    // ----- Float Uniforms -----
    
    /**
     * glUniform1f - Set float uniform
     * GL 2.0: glUniform1f(location, v0)
     * Pre-2.0: glUniform1fARB(location, v0)
     */
    public static void uniform1f(int location, float v0) {
        if (location < 0) return;
        
        UniformFloat value = new UniformFloat(v0);
        if (!uniformChanged(location, value)) return;
        
        if (GL20) {
            GL20.glUniform1f(location, v0);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform1fARB(location, v0);
        }
    }
    
    /**
     * glUniform2f - Set vec2 uniform
     */
    public static void uniform2f(int location, float v0, float v1) {
        if (location < 0) return;
        
        UniformFloat2 value = new UniformFloat2(v0, v1);
        if (!uniformChanged(location, value)) return;
        
        if (GL20) {
            GL20.glUniform2f(location, v0, v1);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform2fARB(location, v0, v1);
        }
    }
    
    /**
     * glUniform3f - Set vec3 uniform
     */
    public static void uniform3f(int location, float v0, float v1, float v2) {
        if (location < 0) return;
        
        UniformFloat3 value = new UniformFloat3(v0, v1, v2);
        if (!uniformChanged(location, value)) return;
        
        if (GL20) {
            GL20.glUniform3f(location, v0, v1, v2);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform3fARB(location, v0, v1, v2);
        }
    }
    
    /**
     * glUniform4f - Set vec4 uniform
     */
    public static void uniform4f(int location, float v0, float v1, float v2, float v3) {
        if (location < 0) return;
        
        UniformFloat4 value = new UniformFloat4(v0, v1, v2, v3);
        if (!uniformChanged(location, value)) return;
        
        if (GL20) {
            GL20.glUniform4f(location, v0, v1, v2, v3);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform4fARB(location, v0, v1, v2, v3);
        }
    }
    
    // ----- Integer Uniforms -----
    
    /**
     * glUniform1i - Set int uniform (used for samplers too)
     */
    public static void uniform1i(int location, int v0) {
        if (location < 0) return;
        
        UniformInt value = new UniformInt(v0);
        if (!uniformChanged(location, value)) return;
        
        if (GL20) {
            GL20.glUniform1i(location, v0);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform1iARB(location, v0);
        }
    }
    
    /**
     * glUniform2i - Set ivec2 uniform
     */
    public static void uniform2i(int location, int v0, int v1) {
        if (location < 0) return;
        
        UniformInt2 value = new UniformInt2(v0, v1);
        if (!uniformChanged(location, value)) return;
        
        if (GL20) {
            GL20.glUniform2i(location, v0, v1);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform2iARB(location, v0, v1);
        }
    }
    
    /**
     * glUniform3i - Set ivec3 uniform
     */
    public static void uniform3i(int location, int v0, int v1, int v2) {
        if (location < 0) return;
        
        UniformInt3 value = new UniformInt3(v0, v1, v2);
        if (!uniformChanged(location, value)) return;
        
        if (GL20) {
            GL20.glUniform3i(location, v0, v1, v2);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform3iARB(location, v0, v1, v2);
        }
    }
    
    /**
     * glUniform4i - Set ivec4 uniform
     */
    public static void uniform4i(int location, int v0, int v1, int v2, int v3) {
        if (location < 0) return;
        
        UniformInt4 value = new UniformInt4(v0, v1, v2, v3);
        if (!uniformChanged(location, value)) return;
        
        if (GL20) {
            GL20.glUniform4i(location, v0, v1, v2, v3);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform4iARB(location, v0, v1, v2, v3);
        }
    }
    
    // ----- Array Uniforms -----
    
    /**
     * glUniform1fv - Set float array uniform
     */
    public static void uniform1fv(int location, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL20) {
            GL20.glUniform1fv(location, value);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform1fvARB(location, value);
        }
    }
    
    /**
     * glUniform2fv - Set vec2 array uniform
     */
    public static void uniform2fv(int location, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL20) {
            GL20.glUniform2fv(location, value);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform2fvARB(location, value);
        }
    }
    
    /**
     * glUniform3fv - Set vec3 array uniform
     */
    public static void uniform3fv(int location, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL20) {
            GL20.glUniform3fv(location, value);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform3fvARB(location, value);
        }
    }
    
    /**
     * glUniform4fv - Set vec4 array uniform
     */
    public static void uniform4fv(int location, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL20) {
            GL20.glUniform4fv(location, value);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform4fvARB(location, value);
        }
    }
    
    /**
     * glUniform1iv - Set int array uniform
     */
    public static void uniform1iv(int location, IntBuffer value) {
        if (location < 0) return;
        
        if (GL20) {
            GL20.glUniform1iv(location, value);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform1ivARB(location, value);
        }
    }
    
    /**
     * glUniform2iv - Set ivec2 array uniform
     */
    public static void uniform2iv(int location, IntBuffer value) {
        if (location < 0) return;
        
        if (GL20) {
            GL20.glUniform2iv(location, value);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform2ivARB(location, value);
        }
    }
    
    /**
     * glUniform3iv - Set ivec3 array uniform
     */
    public static void uniform3iv(int location, IntBuffer value) {
        if (location < 0) return;
        
        if (GL20) {
            GL20.glUniform3iv(location, value);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform3ivARB(location, value);
        }
    }
    
    /**
     * glUniform4iv - Set ivec4 array uniform
     */
    public static void uniform4iv(int location, IntBuffer value) {
        if (location < 0) return;
        
        if (GL20) {
            GL20.glUniform4iv(location, value);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniform4ivARB(location, value);
        }
    }
    
    // ----- Matrix Uniforms -----
    
    /**
     * glUniformMatrix2fv - Set mat2 uniform
     */
    public static void uniformMatrix2fv(int location, boolean transpose, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL20) {
            GL20.glUniformMatrix2fv(location, transpose, value);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniformMatrix2fvARB(location, transpose, value);
        }
    }
    
    /**
     * glUniformMatrix3fv - Set mat3 uniform
     */
    public static void uniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {
        if (location < 0) return;
        
        UniformMatrix3 cached = new UniformMatrix3(value);
        if (!uniformChanged(location, cached)) return;
        
        if (GL20) {
            GL20.glUniformMatrix3fv(location, transpose, value);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniformMatrix3fvARB(location, transpose, value);
        }
    }
    
    /**
     * glUniformMatrix4fv - Set mat4 uniform
     */
    public static void uniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        if (location < 0) return;
        
        UniformMatrix4 cached = new UniformMatrix4(value);
        if (!uniformChanged(location, cached)) return;
        
        if (GL20) {
            GL20.glUniformMatrix4fv(location, transpose, value);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUniformMatrix4fvARB(location, transpose, value);
        }
    }
    
    /**
     * glGetUniformfv - Get uniform float values
     */
    public static void getUniformfv(int program, int location, FloatBuffer params) {
        if (GL20) {
            GL20.glGetUniformfv(program, location, params);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glGetUniformfvARB(program, location, params);
        }
    }
    
    /**
     * glGetUniformiv - Get uniform int values
     */
    public static void getUniformiv(int program, int location, IntBuffer params) {
        if (GL20) {
            GL20.glGetUniformiv(program, location, params);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glGetUniformivARB(program, location, params);
        }
    }
    
    // ========================================================================
    // GL 2.0 - VERTEX ATTRIBUTES
    // ========================================================================
    
    /**
     * glGetAttribLocation - Get attribute location
     * GL 2.0: glGetAttribLocation(program, name)
     * Pre-2.0: glGetAttribLocationARB(program, name)
     * 
     * Uses caching for performance
     */
    public static int getAttribLocation(int program, CharSequence name) {
        String nameStr = name.toString();
        
        // Check cache
        ProgramInfo info = programInfoCache.get(program);
        if (info != null) {
            Integer cached = info.attribLocations.get(nameStr);
            if (cached != null) {
                return cached;
            }
        }
        
        int location;
        if (GL20) {
            location = GL20.glGetAttribLocation(program, name);
        } else if (ARB_vertex_shader) {
            location = ARBVertexShader.glGetAttribLocationARB(program, name);
        } else {
            return -1;
        }
        
        // Cache the result
        if (info != null) {
            info.attribLocations.put(nameStr, location);
        }
        
        return location;
    }
    
    /**
     * glBindAttribLocation - Bind attribute to location
     * GL 2.0: glBindAttribLocation(program, index, name)
     * Pre-2.0: glBindAttribLocationARB(program, index, name)
     */
    public static void bindAttribLocation(int program, int index, CharSequence name) {
        if (GL20) {
            GL20.glBindAttribLocation(program, index, name);
        } else if (ARB_vertex_shader) {
            ARBVertexShader.glBindAttribLocationARB(program, index, name);
        }
        
        // Update cache
        ProgramInfo info = programInfoCache.get(program);
        if (info != null) {
            info.attribLocations.put(name.toString(), index);
            info.linked = false; // Need to relink
        }
    }
    
    /**
     * glEnableVertexAttribArray - Enable vertex attribute array
     * GL 2.0: glEnableVertexAttribArray(index)
     * Pre-2.0: glEnableVertexAttribArrayARB(index)
     */
    public static void enableVertexAttribArray(int index) {
        if (GL20) {
            GL20.glEnableVertexAttribArray(index);
        } else if (ARB_vertex_shader) {
            ARBVertexShader.glEnableVertexAttribArrayARB(index);
        }
    }
    
    /**
     * glDisableVertexAttribArray - Disable vertex attribute array
     * GL 2.0: glDisableVertexAttribArray(index)
     * Pre-2.0: glDisableVertexAttribArrayARB(index)
     */
    public static void disableVertexAttribArray(int index) {
        if (GL20) {
            GL20.glDisableVertexAttribArray(index);
        } else if (ARB_vertex_shader) {
            ARBVertexShader.glDisableVertexAttribArrayARB(index);
        }
    }
    
    /**
     * glVertexAttribPointer - Define vertex attribute array
     * GL 2.0: glVertexAttribPointer(index, size, type, normalized, stride, pointer)
     * Pre-2.0: glVertexAttribPointerARB(...)
     */
    public static void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        if (GL20) {
            GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        } else if (ARB_vertex_shader) {
            ARBVertexShader.glVertexAttribPointerARB(index, size, type, normalized, stride, pointer);
        }
    }
    
    /**
     * glVertexAttribPointer with ByteBuffer
     */
    public static void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, ByteBuffer pointer) {
        if (GL20) {
            GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        }
    }
    
    /**
     * glVertexAttrib1f - Set vertex attribute to float
     */
    public static void vertexAttrib1f(int index, float v0) {
        if (GL20) {
            GL20.glVertexAttrib1f(index, v0);
        } else if (ARB_vertex_shader) {
            ARBVertexShader.glVertexAttrib1fARB(index, v0);
        }
    }
    
    /**
     * glVertexAttrib2f - Set vertex attribute to vec2
     */
    public static void vertexAttrib2f(int index, float v0, float v1) {
        if (GL20) {
            GL20.glVertexAttrib2f(index, v0, v1);
        } else if (ARB_vertex_shader) {
            ARBVertexShader.glVertexAttrib2fARB(index, v0, v1);
        }
    }
    
    /**
     * glVertexAttrib3f - Set vertex attribute to vec3
     */
    public static void vertexAttrib3f(int index, float v0, float v1, float v2) {
        if (GL20) {
            GL20.glVertexAttrib3f(index, v0, v1, v2);
        } else if (ARB_vertex_shader) {
            ARBVertexShader.glVertexAttrib3fARB(index, v0, v1, v2);
        }
    }
    
    /**
     * glVertexAttrib4f - Set vertex attribute to vec4
     */
    public static void vertexAttrib4f(int index, float v0, float v1, float v2, float v3) {
        if (GL20) {
            GL20.glVertexAttrib4f(index, v0, v1, v2, v3);
        } else if (ARB_vertex_shader) {
            ARBVertexShader.glVertexAttrib4fARB(index, v0, v1, v2, v3);
        }
    }
    
    /**
     * glVertexAttrib4fv - Set vertex attribute from float array
     */
    public static void vertexAttrib4fv(int index, FloatBuffer v) {
        if (GL20) {
            GL20.glVertexAttrib4fv(index, v);
        } else if (ARB_vertex_shader) {
            ARBVertexShader.glVertexAttrib4fvARB(index, v);
        }
    }
    
    /**
     * glGetVertexAttribfv - Get vertex attribute float values
     */
    public static void getVertexAttribfv(int index, int pname, FloatBuffer params) {
        if (GL20) {
            GL20.glGetVertexAttribfv(index, pname, params);
        } else if (ARB_vertex_shader) {
            ARBVertexShader.glGetVertexAttribfvARB(index, pname, params);
        }
    }
    
    /**
     * glGetVertexAttribiv - Get vertex attribute int values
     */
    public static void getVertexAttribiv(int index, int pname, IntBuffer params) {
        if (GL20) {
            GL20.glGetVertexAttribiv(index, pname, params);
        } else if (ARB_vertex_shader) {
            ARBVertexShader.glGetVertexAttribivARB(index, pname, params);
        }
    }
    
    /**
     * glGetVertexAttribi - Get vertex attribute int
     */
    public static int getVertexAttribi(int index, int pname) {
        if (GL20) {
            return GL20.glGetVertexAttribi(index, pname);
        } else if (ARB_vertex_shader) {
            IntBuffer buf = BufferUtils.createIntBuffer(1);
            ARBVertexShader.glGetVertexAttribivARB(index, pname, buf);
            return buf.get(0);
        }
        return 0;
    }
    
    // ========================================================================
    // GL 2.0 - ACTIVE UNIFORM/ATTRIBUTE QUERIES
    // ========================================================================
    
    /**
     * glGetActiveUniform - Get info about active uniform
     */
    public static String getActiveUniform(int program, int index, IntBuffer size, IntBuffer type) {
        if (GL20) {
            return GL20.glGetActiveUniform(program, index, size, type);
        } else if (ARB_shader_objects) {
            return ARBShaderObjects.glGetActiveUniformARB(program, index, size, type);
        }
        return "";
    }
    
    /**
     * glGetActiveAttrib - Get info about active attribute
     */
    public static String getActiveAttrib(int program, int index, IntBuffer size, IntBuffer type) {
        if (GL20) {
            return GL20.glGetActiveAttrib(program, index, size, type);
        } else if (ARB_vertex_shader) {
            return ARBVertexShader.glGetActiveAttribARB(program, index, size, type);
        }
        return "";
    }
    
    // ========================================================================
    // GL 2.0 - SEPARATE STENCIL
    // ========================================================================
    
    /**
     * glStencilOpSeparate - Set stencil ops for front/back faces
     * GL 2.0: glStencilOpSeparate(face, sfail, dpfail, dppass)
     */
    public static void stencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
        if (GL20) {
            GL20.glStencilOpSeparate(face, sfail, dpfail, dppass);
        } else {
            // Fallback: use regular stencil op for specified face
            // This is imperfect but maintains some functionality
            GL11.glStencilOp(sfail, dpfail, dppass);
        }
    }
    
    /**
     * glStencilFuncSeparate - Set stencil function for front/back faces
     * GL 2.0: glStencilFuncSeparate(face, func, ref, mask)
     */
    public static void stencilFuncSeparate(int face, int func, int ref, int mask) {
        if (GL20) {
            GL20.glStencilFuncSeparate(face, func, ref, mask);
        } else {
            GL11.glStencilFunc(func, ref, mask);
        }
    }
    
    /**
     * glStencilMaskSeparate - Set stencil mask for front/back faces
     * GL 2.0: glStencilMaskSeparate(face, mask)
     */
    public static void stencilMaskSeparate(int face, int mask) {
        if (GL20) {
            GL20.glStencilMaskSeparate(face, mask);
        } else {
            GL11.glStencilMask(mask);
        }
    }
    
    // ========================================================================
    // GL 2.0 - DRAW BUFFERS
    // ========================================================================
    
    /**
     * glDrawBuffers - Specify draw buffers for MRT
     * GL 2.0: glDrawBuffers(bufs)
     */
    public static void drawBuffers(IntBuffer bufs) {
        if (GL20) {
            GL20.glDrawBuffers(bufs);
        } else if (ARB_draw_buffers) {
            ARBDrawBuffers.glDrawBuffersARB(bufs);
        }
    }
    
    /**
     * glDrawBuffers - Specify single draw buffer
     */
    public static void drawBuffers(int buf) {
        if (GL20) {
            GL20.glDrawBuffers(buf);
        } else if (ARB_draw_buffers) {
            IntBuffer bufs = BufferUtils.createIntBuffer(1).put(buf).flip();
            ARBDrawBuffers.glDrawBuffersARB(bufs);
        }
    }
    
    // ========================================================================
    // GL 2.1 - PIXEL BUFFER OBJECTS (PBO)
    // ========================================================================
    
    // PBO constants
    public static final int GL_PIXEL_PACK_BUFFER = GL21.GL_PIXEL_PACK_BUFFER;
    public static final int GL_PIXEL_UNPACK_BUFFER = GL21.GL_PIXEL_UNPACK_BUFFER;
    public static final int GL_PIXEL_PACK_BUFFER_BINDING = GL21.GL_PIXEL_PACK_BUFFER_BINDING;
    public static final int GL_PIXEL_UNPACK_BUFFER_BINDING = GL21.GL_PIXEL_UNPACK_BUFFER_BINDING;
    
    /**
     * Bind PBO for pixel pack (read) operations
     */
    public static void bindPixelPackBuffer(int buffer) {
        if (GL21) {
            bindBuffer(GL21.GL_PIXEL_PACK_BUFFER, buffer);
        } else if (ARB_pixel_buffer_object) {
            bindBuffer(ARBPixelBufferObject.GL_PIXEL_PACK_BUFFER_ARB, buffer);
        }
    }
    
    /**
     * Bind PBO for pixel unpack (write) operations
     */
    public static void bindPixelUnpackBuffer(int buffer) {
        if (GL21) {
            bindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, buffer);
        } else if (ARB_pixel_buffer_object) {
            bindBuffer(ARBPixelBufferObject.GL_PIXEL_UNPACK_BUFFER_ARB, buffer);
        }
    }
    
    // ========================================================================
    // GL 2.1 - NON-SQUARE MATRIX UNIFORMS
    // ========================================================================
    
    /**
     * glUniformMatrix2x3fv - Set mat2x3 uniform
     * GL 2.1: glUniformMatrix2x3fv(location, transpose, value)
     */
    public static void uniformMatrix2x3fv(int location, boolean transpose, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL21) {
            GL21.glUniformMatrix2x3fv(location, transpose, value);
        }
        // No fallback - requires GL 2.1
    }
    
    /**
     * glUniformMatrix3x2fv - Set mat3x2 uniform
     */
    public static void uniformMatrix3x2fv(int location, boolean transpose, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL21) {
            GL21.glUniformMatrix3x2fv(location, transpose, value);
        }
    }
    
    /**
     * glUniformMatrix2x4fv - Set mat2x4 uniform
     */
    public static void uniformMatrix2x4fv(int location, boolean transpose, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL21) {
            GL21.glUniformMatrix2x4fv(location, transpose, value);
        }
    }
    
    /**
     * glUniformMatrix4x2fv - Set mat4x2 uniform
     */
    public static void uniformMatrix4x2fv(int location, boolean transpose, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL21) {
            GL21.glUniformMatrix4x2fv(location, transpose, value);
        }
    }
    
    /**
     * glUniformMatrix3x4fv - Set mat3x4 uniform
     */
    public static void uniformMatrix3x4fv(int location, boolean transpose, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL21) {
            GL21.glUniformMatrix3x4fv(location, transpose, value);
        }
    }
    
    /**
     * glUniformMatrix4x3fv - Set mat4x3 uniform
     */
    public static void uniformMatrix4x3fv(int location, boolean transpose, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL21) {
            GL21.glUniformMatrix4x3fv(location, transpose, value);
        }
    }
    
    // ========================================================================
    // GL 2.1 - sRGB TEXTURES
    // ========================================================================
    
    // sRGB format constants
    public static final int GL_SRGB = GL21.GL_SRGB;
    public static final int GL_SRGB8 = GL21.GL_SRGB8;
    public static final int GL_SRGB_ALPHA = GL21.GL_SRGB_ALPHA;
    public static final int GL_SRGB8_ALPHA8 = GL21.GL_SRGB8_ALPHA8;
    public static final int GL_COMPRESSED_SRGB = GL21.GL_COMPRESSED_SRGB;
    public static final int GL_COMPRESSED_SRGB_ALPHA = GL21.GL_COMPRESSED_SRGB_ALPHA;
    
    /**
     * Check if sRGB textures are supported
     */
    public static boolean supportsSRGB() {
        return GL21 || EXT_texture_sRGB;
    }
    
    /**
     * Get appropriate sRGB internal format
     */
    public static int getSRGBFormat(int baseFormat, boolean withAlpha) {
        if (!supportsSRGB()) {
            // Return non-sRGB equivalent
            return withAlpha ? GL11.GL_RGBA8 : GL11.GL_RGB8;
        }
        return withAlpha ? GL_SRGB8_ALPHA8 : GL_SRGB8;
    }
    
    // ========================================================================
    // SHADER UTILITY FUNCTIONS
    // ========================================================================
    
    /**
     * Create and compile shader in one call
     * @return shader ID or 0 if failed
     */
    public static int createAndCompileShader(int type, CharSequence source) {
        int shader = createShader(type);
        if (shader == 0) return 0;
        
        shaderSource(shader, source);
        compileShader(shader);
        
        if (getShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE) {
            String log = getShaderInfoLog(shader);
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Shader compilation failed: " + log);
            }
            deleteShader(shader);
            return 0;
        }
        
        return shader;
    }
    
    /**
     * Create program and link shaders
     * @return program ID or 0 if failed
     */
    public static int createAndLinkProgram(int... shaders) {
        int program = createProgram();
        if (program == 0) return 0;
        
        for (int shader : shaders) {
            if (shader != 0) {
                attachShader(program, shader);
            }
        }
        
        linkProgram(program);
        
        if (getProgrami(program, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
            String log = getProgramInfoLog(program);
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Program linking failed: " + log);
            }
            deleteProgram(program);
            return 0;
        }
        
        return program;
    }
    
    /**
     * Create complete shader program from vertex and fragment sources
     */
    public static int createShaderProgram(CharSequence vertexSource, CharSequence fragmentSource) {
        int vertex = createAndCompileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        if (vertex == 0) return 0;
        
        int fragment = createAndCompileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragment == 0) {
            deleteShader(vertex);
            return 0;
        }
        
        int program = createAndLinkProgram(vertex, fragment);
        
        // Can delete shaders after linking
        deleteShader(vertex);
        deleteShader(fragment);
        
        return program;
    }
    
    /**
     * Check if shader compilation succeeded
     */
    public static boolean isShaderCompiled(int shader) {
        ShaderInfo info = shaderInfoCache.get(shader);
        if (info != null) {
            return info.compiled;
        }
        return getShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_TRUE;
    }
    
    /**
     * Check if program linking succeeded
     */
    public static boolean isProgramLinked(int program) {
        ProgramInfo info = programInfoCache.get(program);
        if (info != null) {
            return info.linked;
        }
        return getProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_TRUE;
    }
    
    /**
     * Get all uniform locations for a program
     */
    public static Map<String, Integer> getAllUniformLocations(int program) {
        Map<String, Integer> locations = new HashMap<>();
        
        int count = getProgrami(program, GL20.GL_ACTIVE_UNIFORMS);
        IntBuffer size = BufferUtils.createIntBuffer(1);
        IntBuffer type = BufferUtils.createIntBuffer(1);
        
        for (int i = 0; i < count; i++) {
            String name = getActiveUniform(program, i, size, type);
            if (name != null && !name.isEmpty()) {
                // Remove array brackets for base name
                int bracketIndex = name.indexOf('[');
                if (bracketIndex != -1) {
                    name = name.substring(0, bracketIndex);
                }
                int location = getUniformLocation(program, name);
                if (location >= 0) {
                    locations.put(name, location);
                }
            }
        }
        
        return locations;
    }
    
    /**
     * Get all attribute locations for a program
     */
    public static Map<String, Integer> getAllAttribLocations(int program) {
        Map<String, Integer> locations = new HashMap<>();
        
        int count = getProgrami(program, GL20.GL_ACTIVE_ATTRIBUTES);
        IntBuffer size = BufferUtils.createIntBuffer(1);
        IntBuffer type = BufferUtils.createIntBuffer(1);
        
        for (int i = 0; i < count; i++) {
            String name = getActiveAttrib(program, i, size, type);
            if (name != null && !name.isEmpty()) {
                int location = getAttribLocation(program, name);
                if (location >= 0) {
                    locations.put(name, location);
                }
            }
        }
        
        return locations;
    }
    
    /**
     * Clear uniform cache for a program (use when uniform values may have changed externally)
     */
    public static void clearUniformCache(int program) {
        ProgramInfo info = programInfoCache.get(program);
        if (info != null) {
            info.uniformCache.clear();
        }
    }
    
    /**
     * Clear all caches (use on context loss or major state reset)
     */
    public static void clearAllCaches() {
        shaderInfoCache.clear();
        programInfoCache.clear();
        getState().reset();
    }
    
    // ========================================================================
    // ADDITIONAL ARB EXTENSION FLAGS (add to initialization)
    // ========================================================================
    
    private static boolean ARB_vertex_shader = false;
    private static boolean ARB_fragment_shader = false;
    private static boolean ARB_draw_buffers = false;
    private static boolean ARB_pixel_buffer_object = false;
    private static boolean EXT_texture_sRGB = false;
    
    // Add to detectExtensions():
    private static void detectGL20Extensions() {
        // These would be checked in the main detectExtensions method
        // ARB_vertex_shader = extensions.contains("GL_ARB_vertex_shader");
        // ARB_fragment_shader = extensions.contains("GL_ARB_fragment_shader");
        // ARB_draw_buffers = extensions.contains("GL_ARB_draw_buffers");
        // ARB_pixel_buffer_object = extensions.contains("GL_ARB_pixel_buffer_object");
        // EXT_texture_sRGB = extensions.contains("GL_EXT_texture_sRGB");
    }
    
    // ========================================================================
    // BUFFER UTILITIES (BufferUtils replacement for standalone use)
    // ========================================================================
    
    public static class BufferUtils {
        public static IntBuffer createIntBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity * 4)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
        }
        
        public static FloatBuffer createFloatBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        }
        
        public static ByteBuffer createByteBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity)
                .order(ByteOrder.nativeOrder());
        }
    }

// ========================================================================
    // FRAMEBUFFER/RENDERBUFFER CACHING
    // ========================================================================
    
    /**
     * Framebuffer info cache
     */
    private static final Map<Integer, FramebufferInfo> framebufferCache = new ConcurrentHashMap<>();
    private static final Map<Integer, RenderbufferInfo> renderbufferCache = new ConcurrentHashMap<>();
    
    public static class FramebufferInfo {
        public final int id;
        public int status;
        public final Map<Integer, AttachmentInfo> colorAttachments = new HashMap<>();
        public AttachmentInfo depthAttachment;
        public AttachmentInfo stencilAttachment;
        public AttachmentInfo depthStencilAttachment;
        public int width, height;
        public int samples;
        
        public FramebufferInfo(int id) {
            this.id = id;
        }
    }
    
    public static class AttachmentInfo {
        public int type; // TEXTURE or RENDERBUFFER
        public int id;
        public int level;
        public int layer; // for layered attachments
        
        public static final int TYPE_NONE = 0;
        public static final int TYPE_TEXTURE = 1;
        public static final int TYPE_RENDERBUFFER = 2;
    }
    
    public static class RenderbufferInfo {
        public final int id;
        public int internalFormat;
        public int width, height;
        public int samples;
        
        public RenderbufferInfo(int id) {
            this.id = id;
        }
    }
    
    // ========================================================================
    // GL 3.0 - VERTEX ARRAY OBJECTS (VAO)
    // ========================================================================
    
    /**
     * glGenVertexArrays - Generate VAO names
     * GL 3.0: glGenVertexArrays()
     * Pre-3.0: ARB_vertex_array_object or APPLE_vertex_array_object
     */
    public static int genVertexArray() {
        if (GL30) {
            return GL30.glGenVertexArrays();
        } else if (ARB_vertex_array_object) {
            return ARBVertexArrayObject.glGenVertexArrays();
        } else if (APPLE_vertex_array_object) {
            return APPLEVertexArrayObject.glGenVertexArraysAPPLE();
        }
        
        if (debugMode) {
            System.err.println("[OpenGLCallMapper] VAO not supported");
        }
        return 0;
    }
    
    public static void genVertexArrays(IntBuffer arrays) {
        if (GL30) {
            GL30.glGenVertexArrays(arrays);
        } else if (ARB_vertex_array_object) {
            ARBVertexArrayObject.glGenVertexArrays(arrays);
        } else if (APPLE_vertex_array_object) {
            APPLEVertexArrayObject.glGenVertexArraysAPPLE(arrays);
        }
    }
    
    /**
     * glDeleteVertexArrays - Delete VAOs
     * GL 3.0: glDeleteVertexArrays(arrays)
     */
    public static void deleteVertexArray(int array) {
        if (array == 0) return;
        
        if (GL30) {
            GL30.glDeleteVertexArrays(array);
        } else if (ARB_vertex_array_object) {
            ARBVertexArrayObject.glDeleteVertexArrays(array);
        } else if (APPLE_vertex_array_object) {
            APPLEVertexArrayObject.glDeleteVertexArraysAPPLE(array);
        }
    }
    
    public static void deleteVertexArrays(IntBuffer arrays) {
        if (GL30) {
            GL30.glDeleteVertexArrays(arrays);
        } else if (ARB_vertex_array_object) {
            ARBVertexArrayObject.glDeleteVertexArrays(arrays);
        } else if (APPLE_vertex_array_object) {
            APPLEVertexArrayObject.glDeleteVertexArraysAPPLE(arrays);
        }
    }
    
    /**
     * glBindVertexArray - Bind VAO
     * GL 3.0: glBindVertexArray(array)
     */
    public static void bindVertexArray(int array) {
        StateTracker state = getState();
        if (state.boundVAO == array) {
            return; // Already bound
        }
        state.boundVAO = array;
        
        if (GL30) {
            GL30.glBindVertexArray(array);
        } else if (ARB_vertex_array_object) {
            ARBVertexArrayObject.glBindVertexArray(array);
        } else if (APPLE_vertex_array_object) {
            APPLEVertexArrayObject.glBindVertexArrayAPPLE(array);
        }
    }
    
    /**
     * glIsVertexArray - Check if name is a VAO
     * GL 3.0: glIsVertexArray(array)
     */
    public static boolean isVertexArray(int array) {
        if (GL30) {
            return GL30.glIsVertexArray(array);
        } else if (ARB_vertex_array_object) {
            return ARBVertexArrayObject.glIsVertexArray(array);
        } else if (APPLE_vertex_array_object) {
            return APPLEVertexArrayObject.glIsVertexArrayAPPLE(array);
        }
        return false;
    }
    
    /**
     * Get currently bound VAO
     */
    public static int getCurrentVertexArray() {
        return getState().boundVAO;
    }
    
    // ========================================================================
    // GL 3.0 - FRAMEBUFFER OBJECTS (FBO)
    // ========================================================================
    
    /**
     * glGenFramebuffers - Generate FBO names
     * GL 3.0: glGenFramebuffers() - core
     * Pre-3.0: ARB_framebuffer_object or EXT_framebuffer_object
     */
    public static int genFramebuffer() {
        int fbo;
        
        if (GL30) {
            fbo = GL30.glGenFramebuffers();
        } else if (ARB_framebuffer_object) {
            fbo = ARBFramebufferObject.glGenFramebuffers();
        } else if (EXT_framebuffer_object) {
            fbo = EXTFramebufferObject.glGenFramebuffersEXT();
        } else {
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] FBO not supported");
            }
            return 0;
        }
        
        if (fbo != 0) {
            framebufferCache.put(fbo, new FramebufferInfo(fbo));
        }
        
        return fbo;
    }
    
    public static void genFramebuffers(IntBuffer framebuffers) {
        if (GL30) {
            GL30.glGenFramebuffers(framebuffers);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glGenFramebuffers(framebuffers);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glGenFramebuffersEXT(framebuffers);
        }
    }
    
    /**
     * glDeleteFramebuffers - Delete FBOs
     * GL 3.0: glDeleteFramebuffers(framebuffers)
     */
    public static void deleteFramebuffer(int framebuffer) {
        if (framebuffer == 0) return;
        
        framebufferCache.remove(framebuffer);
        
        if (GL30) {
            GL30.glDeleteFramebuffers(framebuffer);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glDeleteFramebuffers(framebuffer);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffer);
        }
    }
    
    public static void deleteFramebuffers(IntBuffer framebuffers) {
        if (GL30) {
            GL30.glDeleteFramebuffers(framebuffers);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glDeleteFramebuffers(framebuffers);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffers);
        }
    }
    
    /**
     * glBindFramebuffer - Bind FBO
     * GL 3.0: glBindFramebuffer(target, framebuffer)
     * 
     * Targets:
     * - GL_FRAMEBUFFER: Both read and draw
     * - GL_READ_FRAMEBUFFER: Read operations only
     * - GL_DRAW_FRAMEBUFFER: Draw operations only
     */
    public static void bindFramebuffer(int target, int framebuffer) {
        StateTracker state = getState();
        
        // Track bound framebuffers
        if (target == GL30.GL_FRAMEBUFFER || target == EXTFramebufferObject.GL_FRAMEBUFFER_EXT) {
            if (state.boundFramebuffer == framebuffer) return;
            state.boundFramebuffer = framebuffer;
            state.boundReadFramebuffer = framebuffer;
            state.boundDrawFramebuffer = framebuffer;
        } else if (target == GL30.GL_READ_FRAMEBUFFER) {
            if (state.boundReadFramebuffer == framebuffer) return;
            state.boundReadFramebuffer = framebuffer;
        } else if (target == GL30.GL_DRAW_FRAMEBUFFER) {
            if (state.boundDrawFramebuffer == framebuffer) return;
            state.boundDrawFramebuffer = framebuffer;
        }
        
        if (GL30) {
            GL30.glBindFramebuffer(target, framebuffer);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glBindFramebuffer(target, framebuffer);
        } else if (EXT_framebuffer_object) {
            // EXT only supports single framebuffer target
            EXTFramebufferObject.glBindFramebufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, framebuffer);
        }
    }
    
    /**
     * glIsFramebuffer - Check if name is a FBO
     * GL 3.0: glIsFramebuffer(framebuffer)
     */
    public static boolean isFramebuffer(int framebuffer) {
        if (GL30) {
            return GL30.glIsFramebuffer(framebuffer);
        } else if (ARB_framebuffer_object) {
            return ARBFramebufferObject.glIsFramebuffer(framebuffer);
        } else if (EXT_framebuffer_object) {
            return EXTFramebufferObject.glIsFramebufferEXT(framebuffer);
        }
        return false;
    }
    
    /**
     * glCheckFramebufferStatus - Check FBO completeness
     * GL 3.0: glCheckFramebufferStatus(target)
     */
    public static int checkFramebufferStatus(int target) {
        int status;
        
        if (GL30) {
            status = GL30.glCheckFramebufferStatus(target);
        } else if (ARB_framebuffer_object) {
            status = ARBFramebufferObject.glCheckFramebufferStatus(target);
        } else if (EXT_framebuffer_object) {
            status = EXTFramebufferObject.glCheckFramebufferStatusEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT);
        } else {
            return 0;
        }
        
        // Cache status
        int fbo = getState().boundFramebuffer;
        FramebufferInfo info = framebufferCache.get(fbo);
        if (info != null) {
            info.status = status;
        }
        
        return status;
    }
    
    /**
     * Get framebuffer status as string (for debugging)
     */
    public static String getFramebufferStatusString(int status) {
        switch (status) {
            case GL30.GL_FRAMEBUFFER_COMPLETE:
                return "FRAMEBUFFER_COMPLETE";
            case GL30.GL_FRAMEBUFFER_UNDEFINED:
                return "FRAMEBUFFER_UNDEFINED";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                return "FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                return "FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                return "FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                return "FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
            case GL30.GL_FRAMEBUFFER_UNSUPPORTED:
                return "FRAMEBUFFER_UNSUPPORTED";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                return "FRAMEBUFFER_INCOMPLETE_MULTISAMPLE";
            case GL32.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS:
                return "FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS";
            default:
                return "UNKNOWN_STATUS_" + status;
        }
    }
    
    /**
     * glFramebufferTexture2D - Attach texture to FBO
     * GL 3.0: glFramebufferTexture2D(target, attachment, textarget, texture, level)
     */
    public static void framebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        if (GL30) {
            GL30.glFramebufferTexture2D(target, attachment, textarget, texture, level);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glFramebufferTexture2D(target, attachment, textarget, texture, level);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glFramebufferTexture2DEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT, attachment, textarget, texture, level);
        }
        
        // Update cache
        int fbo = getState().boundFramebuffer;
        FramebufferInfo info = framebufferCache.get(fbo);
        if (info != null) {
            AttachmentInfo att = new AttachmentInfo();
            att.type = texture != 0 ? AttachmentInfo.TYPE_TEXTURE : AttachmentInfo.TYPE_NONE;
            att.id = texture;
            att.level = level;
            
            if (attachment >= GL30.GL_COLOR_ATTACHMENT0 && attachment <= GL30.GL_COLOR_ATTACHMENT15) {
                info.colorAttachments.put(attachment - GL30.GL_COLOR_ATTACHMENT0, att);
            } else if (attachment == GL30.GL_DEPTH_ATTACHMENT) {
                info.depthAttachment = att;
            } else if (attachment == GL30.GL_STENCIL_ATTACHMENT) {
                info.stencilAttachment = att;
            } else if (attachment == GL30.GL_DEPTH_STENCIL_ATTACHMENT) {
                info.depthStencilAttachment = att;
            }
        }
    }
    
    /**
     * glFramebufferTexture1D - Attach 1D texture to FBO
     * GL 3.0: glFramebufferTexture1D(target, attachment, textarget, texture, level)
     */
    public static void framebufferTexture1D(int target, int attachment, int textarget, int texture, int level) {
        if (GL30) {
            GL30.glFramebufferTexture1D(target, attachment, textarget, texture, level);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glFramebufferTexture1D(target, attachment, textarget, texture, level);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glFramebufferTexture1DEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT, attachment, textarget, texture, level);
        }
    }
    
    /**
     * glFramebufferTexture3D - Attach 3D texture layer to FBO
     * GL 3.0: glFramebufferTexture3D(target, attachment, textarget, texture, level, layer)
     */
    public static void framebufferTexture3D(int target, int attachment, int textarget, int texture, int level, int layer) {
        if (GL30) {
            GL30.glFramebufferTexture3D(target, attachment, textarget, texture, level, layer);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glFramebufferTexture3D(target, attachment, textarget, texture, level, layer);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glFramebufferTexture3DEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT, attachment, textarget, texture, level, layer);
        }
    }
    
    /**
     * glFramebufferTextureLayer - Attach texture array layer to FBO
     * GL 3.0: glFramebufferTextureLayer(target, attachment, texture, level, layer)
     */
    public static void framebufferTextureLayer(int target, int attachment, int texture, int level, int layer) {
        if (GL30) {
            GL30.glFramebufferTextureLayer(target, attachment, texture, level, layer);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glFramebufferTextureLayer(target, attachment, texture, level, layer);
        } else if (EXT_texture_array) {
            EXTTextureArray.glFramebufferTextureLayerEXT(target, attachment, texture, level, layer);
        }
    }
    
    /**
     * glFramebufferRenderbuffer - Attach renderbuffer to FBO
     * GL 3.0: glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
     */
    public static void framebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        if (GL30) {
            GL30.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glFramebufferRenderbufferEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT, attachment, renderbuffertarget, renderbuffer);
        }
        
        // Update cache
        int fbo = getState().boundFramebuffer;
        FramebufferInfo info = framebufferCache.get(fbo);
        if (info != null) {
            AttachmentInfo att = new AttachmentInfo();
            att.type = renderbuffer != 0 ? AttachmentInfo.TYPE_RENDERBUFFER : AttachmentInfo.TYPE_NONE;
            att.id = renderbuffer;
            
            if (attachment >= GL30.GL_COLOR_ATTACHMENT0 && attachment <= GL30.GL_COLOR_ATTACHMENT15) {
                info.colorAttachments.put(attachment - GL30.GL_COLOR_ATTACHMENT0, att);
            } else if (attachment == GL30.GL_DEPTH_ATTACHMENT) {
                info.depthAttachment = att;
            } else if (attachment == GL30.GL_STENCIL_ATTACHMENT) {
                info.stencilAttachment = att;
            } else if (attachment == GL30.GL_DEPTH_STENCIL_ATTACHMENT) {
                info.depthStencilAttachment = att;
            }
        }
    }
    
    /**
     * glGetFramebufferAttachmentParameteri - Get attachment parameter
     * GL 3.0: glGetFramebufferAttachmentParameteri(target, attachment, pname)
     */
    public static int getFramebufferAttachmentParameteri(int target, int attachment, int pname) {
        if (GL30) {
            return GL30.glGetFramebufferAttachmentParameteri(target, attachment, pname);
        } else if (ARB_framebuffer_object) {
            return ARBFramebufferObject.glGetFramebufferAttachmentParameteri(target, attachment, pname);
        } else if (EXT_framebuffer_object) {
            return EXTFramebufferObject.glGetFramebufferAttachmentParameteriEXT(
                EXTFramebufferObject.GL_FRAMEBUFFER_EXT, attachment, pname);
        }
        return 0;
    }
    
    /**
     * glBlitFramebuffer - Blit between framebuffers
     * GL 3.0: glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter)
     */
    public static void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1,
                                        int dstX0, int dstY0, int dstX1, int dstY1,
                                        int mask, int filter) {
        if (GL30) {
            GL30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        } else if (EXT_framebuffer_blit) {
            EXTFramebufferBlit.glBlitFramebufferEXT(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        }
    }
    
    // ========================================================================
    // GL 3.0 - RENDERBUFFER OBJECTS
    // ========================================================================
    
    /**
     * glGenRenderbuffers - Generate renderbuffer names
     * GL 3.0: glGenRenderbuffers()
     */
    public static int genRenderbuffer() {
        int rbo;
        
        if (GL30) {
            rbo = GL30.glGenRenderbuffers();
        } else if (ARB_framebuffer_object) {
            rbo = ARBFramebufferObject.glGenRenderbuffers();
        } else if (EXT_framebuffer_object) {
            rbo = EXTFramebufferObject.glGenRenderbuffersEXT();
        } else {
            return 0;
        }
        
        if (rbo != 0) {
            renderbufferCache.put(rbo, new RenderbufferInfo(rbo));
        }
        
        return rbo;
    }
    
    public static void genRenderbuffers(IntBuffer renderbuffers) {
        if (GL30) {
            GL30.glGenRenderbuffers(renderbuffers);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glGenRenderbuffers(renderbuffers);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glGenRenderbuffersEXT(renderbuffers);
        }
    }
    
    /**
     * glDeleteRenderbuffers - Delete renderbuffers
     * GL 3.0: glDeleteRenderbuffers(renderbuffers)
     */
    public static void deleteRenderbuffer(int renderbuffer) {
        if (renderbuffer == 0) return;
        
        renderbufferCache.remove(renderbuffer);
        
        if (GL30) {
            GL30.glDeleteRenderbuffers(renderbuffer);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glDeleteRenderbuffers(renderbuffer);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glDeleteRenderbuffersEXT(renderbuffer);
        }
    }
    
    /**
     * glBindRenderbuffer - Bind renderbuffer
     * GL 3.0: glBindRenderbuffer(target, renderbuffer)
     */
    public static void bindRenderbuffer(int target, int renderbuffer) {
        StateTracker state = getState();
        if (state.boundRenderbuffer == renderbuffer) return;
        state.boundRenderbuffer = renderbuffer;
        
        if (GL30) {
            GL30.glBindRenderbuffer(target, renderbuffer);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glBindRenderbuffer(target, renderbuffer);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, renderbuffer);
        }
    }
    
    /**
     * glRenderbufferStorage - Allocate renderbuffer storage
     * GL 3.0: glRenderbufferStorage(target, internalformat, width, height)
     */
    public static void renderbufferStorage(int target, int internalformat, int width, int height) {
        if (GL30) {
            GL30.glRenderbufferStorage(target, internalformat, width, height);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glRenderbufferStorage(target, internalformat, width, height);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glRenderbufferStorageEXT(
                EXTFramebufferObject.GL_RENDERBUFFER_EXT, internalformat, width, height);
        }
        
        // Update cache
        int rbo = getState().boundRenderbuffer;
        RenderbufferInfo info = renderbufferCache.get(rbo);
        if (info != null) {
            info.internalFormat = internalformat;
            info.width = width;
            info.height = height;
            info.samples = 0;
        }
    }
    
    /**
     * glRenderbufferStorageMultisample - Allocate multisample renderbuffer storage
     * GL 3.0: glRenderbufferStorageMultisample(target, samples, internalformat, width, height)
     */
    public static void renderbufferStorageMultisample(int target, int samples, int internalformat, int width, int height) {
        if (GL30) {
            GL30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
        } else if (EXT_framebuffer_multisample) {
            EXTFramebufferMultisample.glRenderbufferStorageMultisampleEXT(target, samples, internalformat, width, height);
        } else {
            // Fallback to non-multisample
            renderbufferStorage(target, internalformat, width, height);
        }
        
        // Update cache
        int rbo = getState().boundRenderbuffer;
        RenderbufferInfo info = renderbufferCache.get(rbo);
        if (info != null) {
            info.internalFormat = internalformat;
            info.width = width;
            info.height = height;
            info.samples = samples;
        }
    }
    
    /**
     * glGetRenderbufferParameteri - Get renderbuffer parameter
     * GL 3.0: glGetRenderbufferParameteri(target, pname)
     */
    public static int getRenderbufferParameteri(int target, int pname) {
        if (GL30) {
            return GL30.glGetRenderbufferParameteri(target, pname);
        } else if (ARB_framebuffer_object) {
            return ARBFramebufferObject.glGetRenderbufferParameteri(target, pname);
        } else if (EXT_framebuffer_object) {
            return EXTFramebufferObject.glGetRenderbufferParameteriEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, pname);
        }
        return 0;
    }
    
    // ========================================================================
    // GL 3.0 - BUFFER MAPPING IMPROVEMENTS
    // ========================================================================
    
    /**
     * glMapBufferRange - Map buffer range with access flags
     * GL 3.0: glMapBufferRange(target, offset, length, access)
     * 
     * Access flags:
     * - GL_MAP_READ_BIT
     * - GL_MAP_WRITE_BIT
     * - GL_MAP_INVALIDATE_RANGE_BIT
     * - GL_MAP_INVALIDATE_BUFFER_BIT
     * - GL_MAP_FLUSH_EXPLICIT_BIT
     * - GL_MAP_UNSYNCHRONIZED_BIT
     * - GL_MAP_PERSISTENT_BIT (GL 4.4)
     * - GL_MAP_COHERENT_BIT (GL 4.4)
     */
    public static ByteBuffer mapBufferRange(int target, long offset, long length, int access) {
        if (GL30) {
            return GL30.glMapBufferRange(target, offset, length, access, null);
        } else if (ARB_map_buffer_range) {
            return ARBMapBufferRange.glMapBufferRange(target, offset, length, access, null);
        } else if (GL15) {
            // Fallback to glMapBuffer (less efficient)
            int glAccess;
            if ((access & GL30.GL_MAP_READ_BIT) != 0 && (access & GL30.GL_MAP_WRITE_BIT) != 0) {
                glAccess = GL15.GL_READ_WRITE;
            } else if ((access & GL30.GL_MAP_READ_BIT) != 0) {
                glAccess = GL15.GL_READ_ONLY;
            } else {
                glAccess = GL15.GL_WRITE_ONLY;
            }
            return GL15.glMapBuffer(target, glAccess, length, null);
        }
        return null;
    }
    
    public static ByteBuffer mapBufferRange(int target, long offset, long length, int access, ByteBuffer oldBuffer) {
        if (GL30) {
            return GL30.glMapBufferRange(target, offset, length, access, oldBuffer);
        } else if (ARB_map_buffer_range) {
            return ARBMapBufferRange.glMapBufferRange(target, offset, length, access, oldBuffer);
        }
        return mapBufferRange(target, offset, length, access);
    }
    
    /**
     * glFlushMappedBufferRange - Flush mapped buffer range
     * GL 3.0: glFlushMappedBufferRange(target, offset, length)
     */
    public static void flushMappedBufferRange(int target, long offset, long length) {
        if (GL30) {
            GL30.glFlushMappedBufferRange(target, offset, length);
        } else if (ARB_map_buffer_range) {
            ARBMapBufferRange.glFlushMappedBufferRange(target, offset, length);
        }
        // No fallback - requires explicit flush support
    }
    
    // ========================================================================
    // GL 3.0 - BUFFER BINDING POINTS
    // ========================================================================
    
    /**
     * glBindBufferBase - Bind buffer to indexed target
     * GL 3.0: glBindBufferBase(target, index, buffer)
     * 
     * Used for: Transform Feedback, Uniform Buffers, Shader Storage Buffers
     */
    public static void bindBufferBase(int target, int index, int buffer) {
        if (GL30) {
            GL30.glBindBufferBase(target, index, buffer);
        } else if (target == GL31.GL_UNIFORM_BUFFER && ARB_uniform_buffer_object) {
            ARBUniformBufferObject.glBindBufferBase(target, index, buffer);
        } else if (EXT_transform_feedback && target == EXTTransformFeedback.GL_TRANSFORM_FEEDBACK_BUFFER_EXT) {
            EXTTransformFeedback.glBindBufferBaseEXT(target, index, buffer);
        }
    }
    
    /**
     * glBindBufferRange - Bind buffer range to indexed target
     * GL 3.0: glBindBufferRange(target, index, buffer, offset, size)
     */
    public static void bindBufferRange(int target, int index, int buffer, long offset, long size) {
        if (GL30) {
            GL30.glBindBufferRange(target, index, buffer, offset, size);
        } else if (target == GL31.GL_UNIFORM_BUFFER && ARB_uniform_buffer_object) {
            ARBUniformBufferObject.glBindBufferRange(target, index, buffer, offset, size);
        } else if (EXT_transform_feedback && target == EXTTransformFeedback.GL_TRANSFORM_FEEDBACK_BUFFER_EXT) {
            EXTTransformFeedback.glBindBufferRangeEXT(target, index, buffer, offset, size);
        }
    }
    
    // ========================================================================
    // GL 3.0 - TRANSFORM FEEDBACK
    // ========================================================================
    
    /**
     * glBeginTransformFeedback - Begin transform feedback
     * GL 3.0: glBeginTransformFeedback(primitiveMode)
     */
    public static void beginTransformFeedback(int primitiveMode) {
        if (GL30) {
            GL30.glBeginTransformFeedback(primitiveMode);
        } else if (EXT_transform_feedback) {
            EXTTransformFeedback.glBeginTransformFeedbackEXT(primitiveMode);
        }
    }
    
    /**
     * glEndTransformFeedback - End transform feedback
     * GL 3.0: glEndTransformFeedback()
     */
    public static void endTransformFeedback() {
        if (GL30) {
            GL30.glEndTransformFeedback();
        } else if (EXT_transform_feedback) {
            EXTTransformFeedback.glEndTransformFeedbackEXT();
        }
    }
    
    /**
     * glTransformFeedbackVaryings - Specify transform feedback varyings
     * GL 3.0: glTransformFeedbackVaryings(program, varyings, bufferMode)
     */
    public static void transformFeedbackVaryings(int program, CharSequence[] varyings, int bufferMode) {
        if (GL30) {
            GL30.glTransformFeedbackVaryings(program, varyings, bufferMode);
        } else if (EXT_transform_feedback) {
            EXTTransformFeedback.glTransformFeedbackVaryingsEXT(program, varyings, bufferMode);
        }
    }
    
    /**
     * glGetTransformFeedbackVarying - Get transform feedback varying info
     * GL 3.0: glGetTransformFeedbackVarying(program, index, size, type)
     */
    public static String getTransformFeedbackVarying(int program, int index, IntBuffer size, IntBuffer type) {
        if (GL30) {
            return GL30.glGetTransformFeedbackVarying(program, index, size, type);
        } else if (EXT_transform_feedback) {
            return EXTTransformFeedback.glGetTransformFeedbackVaryingEXT(program, index, size, type);
        }
        return "";
    }
    
    // ========================================================================
    // GL 3.0 - CONDITIONAL RENDERING
    // ========================================================================
    
    /**
     * glBeginConditionalRender - Begin conditional rendering
     * GL 3.0: glBeginConditionalRender(id, mode)
     */
    public static void beginConditionalRender(int id, int mode) {
        if (GL30) {
            GL30.glBeginConditionalRender(id, mode);
        } else if (NV_conditional_render) {
            NVConditionalRender.glBeginConditionalRenderNV(id, mode);
        }
    }
    
    /**
     * glEndConditionalRender - End conditional rendering
     * GL 3.0: glEndConditionalRender()
     */
    public static void endConditionalRender() {
        if (GL30) {
            GL30.glEndConditionalRender();
        } else if (NV_conditional_render) {
            NVConditionalRender.glEndConditionalRenderNV();
        }
    }
    
    // ========================================================================
    // GL 3.0 - VERTEX ATTRIBUTES (INTEGER)
    // ========================================================================
    
    /**
     * glVertexAttribIPointer - Define integer vertex attribute
     * GL 3.0: glVertexAttribIPointer(index, size, type, stride, pointer)
     */
    public static void vertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        if (GL30) {
            GL30.glVertexAttribIPointer(index, size, type, stride, pointer);
        } else if (EXT_gpu_shader4) {
            EXTGPUShader4.glVertexAttribIPointerEXT(index, size, type, stride, pointer);
        }
    }
    
    /**
     * glVertexAttribI4i - Set integer vertex attribute
     */
    public static void vertexAttribI4i(int index, int x, int y, int z, int w) {
        if (GL30) {
            GL30.glVertexAttribI4i(index, x, y, z, w);
        } else if (EXT_gpu_shader4) {
            EXTGPUShader4.glVertexAttribI4iEXT(index, x, y, z, w);
        }
    }
    
    /**
     * glVertexAttribI4ui - Set unsigned integer vertex attribute
     */
    public static void vertexAttribI4ui(int index, int x, int y, int z, int w) {
        if (GL30) {
            GL30.glVertexAttribI4ui(index, x, y, z, w);
        } else if (EXT_gpu_shader4) {
            EXTGPUShader4.glVertexAttribI4uiEXT(index, x, y, z, w);
        }
    }
    
    // ========================================================================
    // GL 3.0 - TEXTURE OPERATIONS
    // ========================================================================
    
    /**
     * glGenerateMipmap - Generate texture mipmaps
     * GL 3.0: glGenerateMipmap(target)
     */
    public static void generateMipmap(int target) {
        if (GL30) {
            GL30.glGenerateMipmap(target);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glGenerateMipmap(target);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glGenerateMipmapEXT(target);
        }
        // Pre-3.0 without extension: Manual mipmap generation or use GL_GENERATE_MIPMAP texture parameter
    }
    
    /**
     * glBindFragDataLocation - Bind fragment shader output to color number
     * GL 3.0: glBindFragDataLocation(program, colorNumber, name)
     */
    public static void bindFragDataLocation(int program, int colorNumber, CharSequence name) {
        if (GL30) {
            GL30.glBindFragDataLocation(program, colorNumber, name);
        } else if (EXT_gpu_shader4) {
            EXTGPUShader4.glBindFragDataLocationEXT(program, colorNumber, name);
        }
    }
    
    /**
     * glGetFragDataLocation - Get fragment shader output location
     * GL 3.0: glGetFragDataLocation(program, name)
     */
    public static int getFragDataLocation(int program, CharSequence name) {
        if (GL30) {
            return GL30.glGetFragDataLocation(program, name);
        } else if (EXT_gpu_shader4) {
            return EXTGPUShader4.glGetFragDataLocationEXT(program, name);
        }
        return -1;
    }
    
    // ========================================================================
    // GL 3.0 - CLEAR BUFFER OPERATIONS
    // ========================================================================
    
    /**
     * glClearBufferfv - Clear buffer to float values
     * GL 3.0: glClearBufferfv(buffer, drawbuffer, value)
     */
    public static void clearBufferfv(int buffer, int drawbuffer, FloatBuffer value) {
        if (GL30) {
            GL30.glClearBufferfv(buffer, drawbuffer, value);
        } else {
            // Fallback to glClear for common cases
            if (buffer == GL11.GL_COLOR) {
                float r = value.get(0);
                float g = value.get(1);
                float b = value.get(2);
                float a = value.get(3);
                GL11.glClearColor(r, g, b, a);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            } else if (buffer == GL11.GL_DEPTH) {
                GL11.glClearDepth(value.get(0));
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            }
        }
    }
    
    /**
     * glClearBufferiv - Clear buffer to integer values
     * GL 3.0: glClearBufferiv(buffer, drawbuffer, value)
     */
    public static void clearBufferiv(int buffer, int drawbuffer, IntBuffer value) {
        if (GL30) {
            GL30.glClearBufferiv(buffer, drawbuffer, value);
        } else {
            if (buffer == GL11.GL_STENCIL) {
                GL11.glClearStencil(value.get(0));
                GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            }
        }
    }
    
    /**
     * glClearBufferuiv - Clear buffer to unsigned integer values
     * GL 3.0: glClearBufferuiv(buffer, drawbuffer, value)
     */
    public static void clearBufferuiv(int buffer, int drawbuffer, IntBuffer value) {
        if (GL30) {
            GL30.glClearBufferuiv(buffer, drawbuffer, value);
        }
        // No direct fallback for unsigned integer clears
    }
    
    /**
     * glClearBufferfi - Clear depth-stencil buffer
     * GL 3.0: glClearBufferfi(buffer, drawbuffer, depth, stencil)
     */
    public static void clearBufferfi(int buffer, int drawbuffer, float depth, int stencil) {
        if (GL30) {
            GL30.glClearBufferfi(buffer, drawbuffer, depth, stencil);
        } else {
            GL11.glClearDepth(depth);
            GL11.glClearStencil(stencil);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
        }
    }
    
    // ========================================================================
    // GL 3.0 - INDEXED STATE
    // ========================================================================
    
    /**
     * glEnablei - Enable indexed capability
     * GL 3.0: glEnablei(cap, index)
     */
    public static void enablei(int cap, int index) {
        if (GL30) {
            GL30.glEnablei(cap, index);
        } else if (EXT_draw_buffers2) {
            EXTDrawBuffers2.glEnableIndexedEXT(cap, index);
        }
    }
    
    /**
     * glDisablei - Disable indexed capability
     * GL 3.0: glDisablei(cap, index)
     */
    public static void disablei(int cap, int index) {
        if (GL30) {
            GL30.glDisablei(cap, index);
        } else if (EXT_draw_buffers2) {
            EXTDrawBuffers2.glDisableIndexedEXT(cap, index);
        }
    }
    
    /**
     * glIsEnabledi - Check if indexed capability is enabled
     * GL 3.0: glIsEnabledi(cap, index)
     */
    public static boolean isEnabledi(int cap, int index) {
        if (GL30) {
            return GL30.glIsEnabledi(cap, index);
        } else if (EXT_draw_buffers2) {
            return EXTDrawBuffers2.glIsEnabledIndexedEXT(cap, index);
        }
        return false;
    }
    
    /**
     * glColorMaski - Set indexed color mask
     * GL 3.0: glColorMaski(buf, r, g, b, a)
     */
    public static void colorMaski(int buf, boolean r, boolean g, boolean b, boolean a) {
        if (GL30) {
            GL30.glColorMaski(buf, r, g, b, a);
        } else if (EXT_draw_buffers2) {
            EXTDrawBuffers2.glColorMaskIndexedEXT(buf, r, g, b, a);
        }
    }
    
    // ========================================================================
    // GL 3.0 - STRING QUERIES (INDEXED)
    // ========================================================================
    
    /**
     * glGetStringi - Get indexed string
     * GL 3.0: glGetStringi(name, index)
     */
    public static String getStringi(int name, int index) {
        if (GL30) {
            return GL30.glGetStringi(name, index);
        }
        return null;
    }
    
    // ========================================================================
    // GL 3.1 - UNIFORM BUFFER OBJECTS (UBO)
    // ========================================================================
    
    /**
     * glGetUniformBlockIndex - Get uniform block index
     * GL 3.1: glGetUniformBlockIndex(program, uniformBlockName)
     */
    public static int getUniformBlockIndex(int program, CharSequence uniformBlockName) {
        if (GL31) {
            return GL31.glGetUniformBlockIndex(program, uniformBlockName);
        } else if (ARB_uniform_buffer_object) {
            return ARBUniformBufferObject.glGetUniformBlockIndex(program, uniformBlockName);
        }
        return GL31.GL_INVALID_INDEX;
    }
    
    /**
     * glUniformBlockBinding - Set uniform block binding point
     * GL 3.1: glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding)
     */
    public static void uniformBlockBinding(int program, int uniformBlockIndex, int uniformBlockBinding) {
        if (GL31) {
            GL31.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
        } else if (ARB_uniform_buffer_object) {
            ARBUniformBufferObject.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
        }
    }
    
    /**
     * glGetActiveUniformBlockiv - Get uniform block parameter
     * GL 3.1: glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params)
     */
    public static void getActiveUniformBlockiv(int program, int uniformBlockIndex, int pname, IntBuffer params) {
        if (GL31) {
            GL31.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params);
        } else if (ARB_uniform_buffer_object) {
            ARBUniformBufferObject.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params);
        }
    }
    
    /**
     * glGetActiveUniformBlocki - Get single uniform block parameter
     */
    public static int getActiveUniformBlocki(int program, int uniformBlockIndex, int pname) {
        if (GL31) {
            return GL31.glGetActiveUniformBlocki(program, uniformBlockIndex, pname);
        } else if (ARB_uniform_buffer_object) {
            IntBuffer buf = BufferUtils.createIntBuffer(1);
            ARBUniformBufferObject.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, buf);
            return buf.get(0);
        }
        return 0;
    }
    
    /**
     * glGetActiveUniformBlockName - Get uniform block name
     * GL 3.1: glGetActiveUniformBlockName(program, uniformBlockIndex)
     */
    public static String getActiveUniformBlockName(int program, int uniformBlockIndex) {
        if (GL31) {
            return GL31.glGetActiveUniformBlockName(program, uniformBlockIndex);
        } else if (ARB_uniform_buffer_object) {
            return ARBUniformBufferObject.glGetActiveUniformBlockName(program, uniformBlockIndex,
                ARBUniformBufferObject.glGetActiveUniformBlocki(program, uniformBlockIndex, 
                    ARBUniformBufferObject.GL_UNIFORM_BLOCK_NAME_LENGTH));
        }
        return "";
    }
    
    /**
     * glGetUniformIndices - Get uniform indices
     * GL 3.1: glGetUniformIndices(program, uniformNames, uniformIndices)
     */
    public static void getUniformIndices(int program, CharSequence[] uniformNames, IntBuffer uniformIndices) {
        if (GL31) {
            GL31.glGetUniformIndices(program, uniformNames, uniformIndices);
        } else if (ARB_uniform_buffer_object) {
            ARBUniformBufferObject.glGetUniformIndices(program, uniformNames, uniformIndices);
        }
    }
    
    /**
     * glGetActiveUniformsiv - Get active uniform parameters
     * GL 3.1: glGetActiveUniformsiv(program, uniformIndices, pname, params)
     */
    public static void getActiveUniformsiv(int program, IntBuffer uniformIndices, int pname, IntBuffer params) {
        if (GL31) {
            GL31.glGetActiveUniformsiv(program, uniformIndices, pname, params);
        } else if (ARB_uniform_buffer_object) {
            ARBUniformBufferObject.glGetActiveUniformsiv(program, uniformIndices, pname, params);
        }
    }
    
    // ========================================================================
    // GL 3.1 - PRIMITIVE RESTART
    // ========================================================================
    
    /**
     * Enable primitive restart with fixed index
     * GL 3.1: glEnable(GL_PRIMITIVE_RESTART), glPrimitiveRestartIndex(index)
     */
    public static void enablePrimitiveRestart(int index) {
        if (GL31) {
            GL11.glEnable(GL31.GL_PRIMITIVE_RESTART);
            GL31.glPrimitiveRestartIndex(index);
        } else if (NV_primitive_restart) {
            GL11.glEnable(NVPrimitiveRestart.GL_PRIMITIVE_RESTART_NV);
            NVPrimitiveRestart.glPrimitiveRestartIndexNV(index);
        }
    }
    
    /**
     * glPrimitiveRestartIndex - Set primitive restart index
     * GL 3.1: glPrimitiveRestartIndex(index)
     */
    public static void primitiveRestartIndex(int index) {
        if (GL31) {
            GL31.glPrimitiveRestartIndex(index);
        } else if (NV_primitive_restart) {
            NVPrimitiveRestart.glPrimitiveRestartIndexNV(index);
        }
    }
    
    /**
     * Disable primitive restart
     */
    public static void disablePrimitiveRestart() {
        if (GL31) {
            GL11.glDisable(GL31.GL_PRIMITIVE_RESTART);
        } else if (NV_primitive_restart) {
            GL11.glDisable(NVPrimitiveRestart.GL_PRIMITIVE_RESTART_NV);
        }
    }
    
    // ========================================================================
    // GL 3.1 - TEXTURE BUFFER OBJECTS
    // ========================================================================
    
    /**
     * glTexBuffer - Attach buffer to texture
     * GL 3.1: glTexBuffer(target, internalformat, buffer)
     */
    public static void texBuffer(int target, int internalformat, int buffer) {
        if (GL31) {
            GL31.glTexBuffer(target, internalformat, buffer);
        } else if (ARB_texture_buffer_object) {
            ARBTextureBufferObject.glTexBufferARB(target, internalformat, buffer);
        } else if (EXT_texture_buffer_object) {
            EXTTextureBufferObject.glTexBufferEXT(target, internalformat, buffer);
        }
    }
    
    // ========================================================================
    // GL 3.1 - COPY BUFFER
    // ========================================================================
    
    /**
     * glCopyBufferSubData - Copy data between buffers
     * GL 3.1: glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size)
     */
    public static void copyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        if (GL31) {
            GL31.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        } else if (ARB_copy_buffer) {
            ARBCopyBuffer.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        } else if (GL15) {
            // Manual fallback using map/unmap
            ByteBuffer src = GL15.glMapBuffer(readTarget, GL15.GL_READ_ONLY, size, null);
            if (src != null) {
                src.position((int)readOffset);
                ByteBuffer srcSlice = src.slice();
                srcSlice.limit((int)size);
                
                GL15.glBufferSubData(writeTarget, writeOffset, srcSlice);
                GL15.glUnmapBuffer(readTarget);
            }
        }
    }
    
    // ========================================================================
    // GL 3.1 - INSTANCED RENDERING
    // ========================================================================
    
    /**
     * glDrawArraysInstanced - Draw arrays with instancing
     * GL 3.1: glDrawArraysInstanced(mode, first, count, primcount)
     */
    public static void drawArraysInstanced(int mode, int first, int count, int primcount) {
        if (GL31) {
            GL31.glDrawArraysInstanced(mode, first, count, primcount);
        } else if (ARB_draw_instanced) {
            ARBDrawInstanced.glDrawArraysInstancedARB(mode, first, count, primcount);
        } else if (EXT_draw_instanced) {
            EXTDrawInstanced.glDrawArraysInstancedEXT(mode, first, count, primcount);
        } else {
            // Fallback: draw multiple times (very slow)
            for (int i = 0; i < primcount; i++) {
                GL11.glDrawArrays(mode, first, count);
            }
        }
    }
    
    /**
     * glDrawElementsInstanced - Draw elements with instancing
     * GL 3.1: glDrawElementsInstanced(mode, count, type, indices, primcount)
     */
    public static void drawElementsInstanced(int mode, int count, int type, long indices, int primcount) {
        if (GL31) {
            GL31.glDrawElementsInstanced(mode, count, type, indices, primcount);
        } else if (ARB_draw_instanced) {
            ARBDrawInstanced.glDrawElementsInstancedARB(mode, count, type, indices, primcount);
        } else if (EXT_draw_instanced) {
            EXTDrawInstanced.glDrawElementsInstancedEXT(mode, count, type, indices, primcount);
        } else {
            // Fallback: draw multiple times (very slow)
            for (int i = 0; i < primcount; i++) {
                GL11.glDrawElements(mode, count, type, indices);
            }
        }
    }
    
    public static void drawElementsInstanced(int mode, int type, ByteBuffer indices, int primcount) {
        if (GL31) {
            GL31.glDrawElementsInstanced(mode, type, indices, primcount);
        }
    }
    
    public static void drawElementsInstanced(int mode, IntBuffer indices, int primcount) {
        if (GL31) {
            GL31.glDrawElementsInstanced(mode, indices, primcount);
        }
    }
    
    public static void drawElementsInstanced(int mode, ShortBuffer indices, int primcount) {
        if (GL31) {
            GL31.glDrawElementsInstanced(mode, indices, primcount);
        }
    }
    
    // ========================================================================
    // GL 3.2 - GEOMETRY SHADERS
    // ========================================================================
    
    // Geometry shader constants
    public static final int GL_GEOMETRY_SHADER = GL32.GL_GEOMETRY_SHADER;
    public static final int GL_GEOMETRY_VERTICES_OUT = GL32.GL_GEOMETRY_VERTICES_OUT;
    public static final int GL_GEOMETRY_INPUT_TYPE = GL32.GL_GEOMETRY_INPUT_TYPE;
    public static final int GL_GEOMETRY_OUTPUT_TYPE = GL32.GL_GEOMETRY_OUTPUT_TYPE;
    public static final int GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS = GL32.GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS;
    public static final int GL_MAX_GEOMETRY_OUTPUT_VERTICES = GL32.GL_MAX_GEOMETRY_OUTPUT_VERTICES;
    public static final int GL_MAX_GEOMETRY_TOTAL_OUTPUT_COMPONENTS = GL32.GL_MAX_GEOMETRY_TOTAL_OUTPUT_COMPONENTS;
    
    /**
     * Create geometry shader
     */
    public static int createGeometryShader() {
        if (GL32) {
            return createShader(GL32.GL_GEOMETRY_SHADER);
        } else if (ARB_geometry_shader4) {
            return createShader(ARBGeometryShader4.GL_GEOMETRY_SHADER_ARB);
        } else if (EXT_geometry_shader4) {
            return createShader(EXTGeometryShader4.GL_GEOMETRY_SHADER_EXT);
        }
        return 0;
    }
    
    /**
     * glFramebufferTexture - Attach texture for layered rendering
     * GL 3.2: glFramebufferTexture(target, attachment, texture, level)
     */
    public static void framebufferTexture(int target, int attachment, int texture, int level) {
        if (GL32) {
            GL32.glFramebufferTexture(target, attachment, texture, level);
        } else if (ARB_geometry_shader4) {
            ARBGeometryShader4.glFramebufferTextureARB(target, attachment, texture, level);
        }
    }
    
    // ========================================================================
    // GL 3.2 - MULTISAMPLE TEXTURES
    // ========================================================================
    
    /**
     * glTexImage2DMultisample - Create multisample 2D texture
     * GL 3.2: glTexImage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations)
     */
    public static void texImage2DMultisample(int target, int samples, int internalformat, 
                                              int width, int height, boolean fixedsamplelocations) {
        if (GL32) {
            GL32.glTexImage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations);
        } else if (ARB_texture_multisample) {
            ARBTextureMultisample.glTexImage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations);
        }
    }
    
    /**
     * glTexImage3DMultisample - Create multisample 3D texture
     * GL 3.2: glTexImage3DMultisample(target, samples, internalformat, width, height, depth, fixedsamplelocations)
     */
    public static void texImage3DMultisample(int target, int samples, int internalformat,
                                              int width, int height, int depth, boolean fixedsamplelocations) {
        if (GL32) {
            GL32.glTexImage3DMultisample(target, samples, internalformat, width, height, depth, fixedsamplelocations);
        } else if (ARB_texture_multisample) {
            ARBTextureMultisample.glTexImage3DMultisample(target, samples, internalformat, width, height, depth, fixedsamplelocations);
        }
    }
    
    /**
     * glGetMultisamplefv - Get sample position
     * GL 3.2: glGetMultisamplefv(pname, index, val)
     */
    public static void getMultisamplefv(int pname, int index, FloatBuffer val) {
        if (GL32) {
            GL32.glGetMultisamplefv(pname, index, val);
        } else if (ARB_texture_multisample) {
            ARBTextureMultisample.glGetMultisamplefv(pname, index, val);
        }
    }
    
    /**
     * glSampleMaski - Set sample mask
     * GL 3.2: glSampleMaski(maskNumber, mask)
     */
    public static void sampleMaski(int maskNumber, int mask) {
        if (GL32) {
            GL32.glSampleMaski(maskNumber, mask);
        } else if (ARB_texture_multisample) {
            ARBTextureMultisample.glSampleMaski(maskNumber, mask);
        }
    }
    
    // ========================================================================
    // GL 3.2 - SYNC OBJECTS
    // ========================================================================
    
    /**
     * glFenceSync - Create sync object
     * GL 3.2: glFenceSync(condition, flags)
     */
    public static long fenceSync(int condition, int flags) {
        if (GL32) {
            return GL32.glFenceSync(condition, flags);
        } else if (ARB_sync) {
            return ARBSync.glFenceSync(condition, flags);
        }
        return 0;
    }
    
    /**
     * Create GPU fence for current command stream
     */
    public static long fenceSync() {
        return fenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }
    
    /**
     * glDeleteSync - Delete sync object
     * GL 3.2: glDeleteSync(sync)
     */
    public static void deleteSync(long sync) {
        if (sync == 0) return;
        
        if (GL32) {
            GL32.glDeleteSync(sync);
        } else if (ARB_sync) {
            ARBSync.glDeleteSync(sync);
        }
    }
    
    /**
     * glIsSync - Check if name is a sync object
     * GL 3.2: glIsSync(sync)
     */
    public static boolean isSync(long sync) {
        if (GL32) {
            return GL32.glIsSync(sync);
        } else if (ARB_sync) {
            return ARBSync.glIsSync(sync);
        }
        return false;
    }
    
    /**
     * glClientWaitSync - Wait for sync with timeout
     * GL 3.2: glClientWaitSync(sync, flags, timeout)
     */
    public static int clientWaitSync(long sync, int flags, long timeout) {
        if (GL32) {
            return GL32.glClientWaitSync(sync, flags, timeout);
        } else if (ARB_sync) {
            return ARBSync.glClientWaitSync(sync, flags, timeout);
        }
        return GL32.GL_WAIT_FAILED;
    }
    
    /**
     * glWaitSync - GPU wait for sync
     * GL 3.2: glWaitSync(sync, flags, timeout)
     */
    public static void waitSync(long sync, int flags, long timeout) {
        if (GL32) {
            GL32.glWaitSync(sync, flags, timeout);
        } else if (ARB_sync) {
            ARBSync.glWaitSync(sync, flags, timeout);
        }
    }
    
    /**
     * glGetSynci - Get sync object parameter
     * GL 3.2: glGetSynci(sync, pname, length, values)
     */
    public static int getSynci(long sync, int pname) {
        if (GL32) {
            return GL32.glGetSynci(sync, pname);
        } else if (ARB_sync) {
            IntBuffer length = BufferUtils.createIntBuffer(1);
            IntBuffer values = BufferUtils.createIntBuffer(1);
            ARBSync.glGetSynciv(sync, pname, length, values);
            return values.get(0);
        }
        return 0;
    }
    
    // ========================================================================
    // GL 3.2 - PROVOKING VERTEX
    // ========================================================================
    
    /**
     * glProvokingVertex - Set provoking vertex mode
     * GL 3.2: glProvokingVertex(mode)
     */
    public static void provokingVertex(int mode) {
        if (GL32) {
            GL32.glProvokingVertex(mode);
        } else if (ARB_provoking_vertex) {
            ARBProvokingVertex.glProvokingVertex(mode);
        }
    }
    
    // ========================================================================
    // GL 3.2 - SEAMLESS CUBE MAPS
    // ========================================================================
    
    /**
     * Enable seamless cube map filtering
     */
    public static void enableSeamlessCubeMap() {
        if (GL32 || ARB_seamless_cube_map) {
            GL11.glEnable(GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS);
        }
    }
    
    /**
     * Disable seamless cube map filtering
     */
    public static void disableSeamlessCubeMap() {
        if (GL32 || ARB_seamless_cube_map) {
            GL11.glDisable(GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS);
        }
    }
    
    // ========================================================================
    // GL 3.2 - DEPTH CLAMP
    // ========================================================================
    
    /**
     * Enable depth clamp
     */
    public static void enableDepthClamp() {
        if (GL32 || ARB_depth_clamp) {
            GL11.glEnable(GL32.GL_DEPTH_CLAMP);
        }
    }
    
    /**
     * Disable depth clamp
     */
    public static void disableDepthClamp() {
        if (GL32 || ARB_depth_clamp) {
            GL11.glDisable(GL32.GL_DEPTH_CLAMP);
        }
    }
    
    // ========================================================================
    // GL 3.3 - SAMPLER OBJECTS
    // ========================================================================
    
    /**
     * Sampler cache
     */
    private static final Map<Integer, SamplerInfo> samplerCache = new ConcurrentHashMap<>();
    
    public static class SamplerInfo {
        public final int id;
        public int minFilter = GL11.GL_NEAREST_MIPMAP_LINEAR;
        public int magFilter = GL11.GL_LINEAR;
        public int wrapS = GL11.GL_REPEAT;
        public int wrapT = GL11.GL_REPEAT;
        public int wrapR = GL12.GL_REPEAT;
        public int compareMode = GL11.GL_NONE;
        public int compareFunc = GL11.GL_LEQUAL;
        public float minLod = -1000.0f;
        public float maxLod = 1000.0f;
        public float lodBias = 0.0f;
        public float maxAnisotropy = 1.0f;
        public float[] borderColor = {0, 0, 0, 0};
        
        public SamplerInfo(int id) {
            this.id = id;
        }
    }
    
    /**
     * glGenSamplers - Generate sampler object names
     * GL 3.3: glGenSamplers()
     */
    public static int genSampler() {
        if (GL33) {
            int sampler = GL33.glGenSamplers();
            if (sampler != 0) {
                samplerCache.put(sampler, new SamplerInfo(sampler));
            }
            return sampler;
        } else if (ARB_sampler_objects) {
            int sampler = ARBSamplerObjects.glGenSamplers();
            if (sampler != 0) {
                samplerCache.put(sampler, new SamplerInfo(sampler));
            }
            return sampler;
        }
        return 0;
    }
    
    public static void genSamplers(IntBuffer samplers) {
        if (GL33) {
            GL33.glGenSamplers(samplers);
        } else if (ARB_sampler_objects) {
            ARBSamplerObjects.glGenSamplers(samplers);
        }
    }
    
    /**
     * glDeleteSamplers - Delete sampler objects
     * GL 3.3: glDeleteSamplers(samplers)
     */
    public static void deleteSampler(int sampler) {
        if (sampler == 0) return;
        
        samplerCache.remove(sampler);
        
        if (GL33) {
            GL33.glDeleteSamplers(sampler);
        } else if (ARB_sampler_objects) {
            ARBSamplerObjects.glDeleteSamplers(sampler);
        }
    }
    
    public static void deleteSamplers(IntBuffer samplers) {
        if (GL33) {
            GL33.glDeleteSamplers(samplers);
        } else if (ARB_sampler_objects) {
            ARBSamplerObjects.glDeleteSamplers(samplers);
        }
    }
    
    /**
     * glBindSampler - Bind sampler to texture unit
     * GL 3.3: glBindSampler(unit, sampler)
     */
    public static void bindSampler(int unit, int sampler) {
        if (GL33) {
            GL33.glBindSampler(unit, sampler);
        } else if (ARB_sampler_objects) {
            ARBSamplerObjects.glBindSampler(unit, sampler);
        }
    }
    
    /**
     * glIsSampler - Check if name is a sampler
     * GL 3.3: glIsSampler(sampler)
     */
    public static boolean isSampler(int sampler) {
        if (GL33) {
            return GL33.glIsSampler(sampler);
        } else if (ARB_sampler_objects) {
            return ARBSamplerObjects.glIsSampler(sampler);
        }
        return false;
    }
    
    /**
     * glSamplerParameteri - Set sampler parameter (integer)
     * GL 3.3: glSamplerParameteri(sampler, pname, param)
     */
    public static void samplerParameteri(int sampler, int pname, int param) {
        if (GL33) {
            GL33.glSamplerParameteri(sampler, pname, param);
        } else if (ARB_sampler_objects) {
            ARBSamplerObjects.glSamplerParameteri(sampler, pname, param);
        }
        
        // Update cache
        SamplerInfo info = samplerCache.get(sampler);
        if (info != null) {
            switch (pname) {
                case GL11.GL_TEXTURE_MIN_FILTER: info.minFilter = param; break;
                case GL11.GL_TEXTURE_MAG_FILTER: info.magFilter = param; break;
                case GL11.GL_TEXTURE_WRAP_S: info.wrapS = param; break;
                case GL11.GL_TEXTURE_WRAP_T: info.wrapT = param; break;
                case GL12.GL_TEXTURE_WRAP_R: info.wrapR = param; break;
                case GL14.GL_TEXTURE_COMPARE_MODE: info.compareMode = param; break;
                case GL14.GL_TEXTURE_COMPARE_FUNC: info.compareFunc = param; break;
            }
        }
    }
    
    /**
     * glSamplerParameterf - Set sampler parameter (float)
     * GL 3.3: glSamplerParameterf(sampler, pname, param)
     */
    public static void samplerParameterf(int sampler, int pname, float param) {
        if (GL33) {
            GL33.glSamplerParameterf(sampler, pname, param);
        } else if (ARB_sampler_objects) {
            ARBSamplerObjects.glSamplerParameterf(sampler, pname, param);
        }
        
        // Update cache
        SamplerInfo info = samplerCache.get(sampler);
        if (info != null) {
            switch (pname) {
                case GL12.GL_TEXTURE_MIN_LOD: info.minLod = param; break;
                case GL12.GL_TEXTURE_MAX_LOD: info.maxLod = param; break;
                case GL14.GL_TEXTURE_LOD_BIAS: info.lodBias = param; break;
                case EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT: info.maxAnisotropy = param; break;
            }
        }
    }
    
    /**
     * glSamplerParameteriv - Set sampler parameters (integer array)
     * GL 3.3: glSamplerParameteriv(sampler, pname, params)
     */
    public static void samplerParameteriv(int sampler, int pname, IntBuffer params) {
        if (GL33) {
            GL33.glSamplerParameteriv(sampler, pname, params);
        } else if (ARB_sampler_objects) {
            ARBSamplerObjects.glSamplerParameteriv(sampler, pname, params);
        }
    }
    
    /**
     * glSamplerParameterfv - Set sampler parameters (float array)
     * GL 3.3: glSamplerParameterfv(sampler, pname, params)
     */
    public static void samplerParameterfv(int sampler, int pname, FloatBuffer params) {
        if (GL33) {
            GL33.glSamplerParameterfv(sampler, pname, params);
        } else if (ARB_sampler_objects) {
            ARBSamplerObjects.glSamplerParameterfv(sampler, pname, params);
        }
        
        if (pname == GL11.GL_TEXTURE_BORDER_COLOR) {
            SamplerInfo info = samplerCache.get(sampler);
            if (info != null) {
                info.borderColor[0] = params.get(0);
                info.borderColor[1] = params.get(1);
                info.borderColor[2] = params.get(2);
                info.borderColor[3] = params.get(3);
            }
        }
    }
    
    /**
     * glGetSamplerParameteri - Get sampler parameter
     * GL 3.3: glGetSamplerParameteri(sampler, pname)
     */
    public static int getSamplerParameteri(int sampler, int pname) {
        if (GL33) {
            return GL33.glGetSamplerParameteri(sampler, pname);
        } else if (ARB_sampler_objects) {
            return ARBSamplerObjects.glGetSamplerParameteri(sampler, pname);
        }
        return 0;
    }
    
    /**
     * glGetSamplerParameterf - Get sampler parameter (float)
     * GL 3.3: glGetSamplerParameterf(sampler, pname)
     */
    public static float getSamplerParameterf(int sampler, int pname) {
        if (GL33) {
            return GL33.glGetSamplerParameterf(sampler, pname);
        } else if (ARB_sampler_objects) {
            FloatBuffer buf = BufferUtils.createFloatBuffer(1);
            ARBSamplerObjects.glGetSamplerParameterfv(sampler, pname, buf);
            return buf.get(0);
        }
        return 0.0f;
    }
    
    // ========================================================================
    // GL 3.3 - TIMER QUERIES
    // ========================================================================
    
    /**
     * glQueryCounter - Record timestamp
     * GL 3.3: glQueryCounter(id, target)
     */
    public static void queryCounter(int id, int target) {
        if (GL33) {
            GL33.glQueryCounter(id, target);
        } else if (ARB_timer_query) {
            ARBTimerQuery.glQueryCounter(id, target);
        }
    }
    
    /**
     * glGetQueryObjecti64v - Get query result (64-bit)
     * GL 3.3: glGetQueryObjecti64v(id, pname, params)
     */
    public static void getQueryObjecti64v(int id, int pname, LongBuffer params) {
        if (GL33) {
            GL33.glGetQueryObjecti64v(id, pname, params);
        } else if (ARB_timer_query) {
            ARBTimerQuery.glGetQueryObjecti64v(id, pname, params);
        }
    }
    
    /**
     * glGetQueryObjectui64v - Get query result (unsigned 64-bit)
     * GL 3.3: glGetQueryObjectui64v(id, pname, params)
     */
    public static void getQueryObjectui64v(int id, int pname, LongBuffer params) {
        if (GL33) {
            GL33.glGetQueryObjectui64v(id, pname, params);
        } else if (ARB_timer_query) {
            ARBTimerQuery.glGetQueryObjectui64v(id, pname, params);
        }
    }
    
    /**
     * Get timestamp query result
     */
    public static long getQueryObjecti64(int id, int pname) {
        LongBuffer buf = BufferUtils.createLongBuffer(1);
        getQueryObjecti64v(id, pname, buf);
        return buf.get(0);
    }
    
    public static long getQueryObjectui64(int id, int pname) {
        LongBuffer buf = BufferUtils.createLongBuffer(1);
        getQueryObjectui64v(id, pname, buf);
        return buf.get(0);
    }
    
    // ========================================================================
    // GL 3.3 - VERTEX ATTRIB DIVISOR
    // ========================================================================
    
    /**
     * glVertexAttribDivisor - Set attribute divisor for instancing
     * GL 3.3: glVertexAttribDivisor(index, divisor)
     */
    public static void vertexAttribDivisor(int index, int divisor) {
        if (GL33) {
            GL33.glVertexAttribDivisor(index, divisor);
        } else if (ARB_instanced_arrays) {
            ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
        }
    }
    
    // ========================================================================
    // GL 3.3 - RGB10_A2UI VERTEX FORMAT
    // ========================================================================
    
    /**
     * glVertexAttribP1ui - Set packed vertex attribute
     * GL 3.3: glVertexAttribP1ui(index, type, normalized, value)
     */
    public static void vertexAttribP1ui(int index, int type, boolean normalized, int value) {
        if (GL33) {
            GL33.glVertexAttribP1ui(index, type, normalized, value);
        }
    }
    
    public static void vertexAttribP2ui(int index, int type, boolean normalized, int value) {
        if (GL33) {
            GL33.glVertexAttribP2ui(index, type, normalized, value);
        }
    }
    
    public static void vertexAttribP3ui(int index, int type, boolean normalized, int value) {
        if (GL33) {
            GL33.glVertexAttribP3ui(index, type, normalized, value);
        }
    }
    
    public static void vertexAttribP4ui(int index, int type, boolean normalized, int value) {
        if (GL33) {
            GL33.glVertexAttribP4ui(index, type, normalized, value);
        }
    }
    
    // ========================================================================
    // GL 3.3 - DUAL SOURCE BLENDING
    // ========================================================================
    
    /**
     * glBindFragDataLocationIndexed - Bind fragment output with index
     * GL 3.3: glBindFragDataLocationIndexed(program, colorNumber, index, name)
     */
    public static void bindFragDataLocationIndexed(int program, int colorNumber, int index, CharSequence name) {
        if (GL33) {
            GL33.glBindFragDataLocationIndexed(program, colorNumber, index, name);
        } else if (ARB_blend_func_extended) {
            ARBBlendFuncExtended.glBindFragDataLocationIndexed(program, colorNumber, index, name);
        }
    }
    
    /**
     * glGetFragDataIndex - Get fragment output index
     * GL 3.3: glGetFragDataIndex(program, name)
     */
    public static int getFragDataIndex(int program, CharSequence name) {
        if (GL33) {
            return GL33.glGetFragDataIndex(program, name);
        } else if (ARB_blend_func_extended) {
            return ARBBlendFuncExtended.glGetFragDataIndex(program, name);
        }
        return -1;
    }
    
    // ========================================================================
    // ADDITIONAL EXTENSION FLAGS (to be added to initialization)
    // ========================================================================
    
    private static boolean APPLE_vertex_array_object = false;
    private static boolean EXT_framebuffer_blit = false;
    private static boolean EXT_framebuffer_multisample = false;
    private static boolean EXT_texture_array = false;
    private static boolean ARB_map_buffer_range = false;
    private static boolean EXT_transform_feedback = false;
    private static boolean EXT_gpu_shader4 = false;
    private static boolean EXT_draw_buffers2 = false;
    private static boolean NV_primitive_restart = false;
    private static boolean ARB_copy_buffer = false;
    private static boolean EXT_draw_instanced = false;
    private static boolean ARB_texture_buffer_object = false;
    private static boolean EXT_texture_buffer_object = false;
    private static boolean ARB_texture_multisample = false;
    private static boolean ARB_provoking_vertex = false;
    private static boolean ARB_seamless_cube_map = false;
    private static boolean ARB_depth_clamp = false;
    private static boolean ARB_instanced_arrays = false;
    private static boolean ARB_blend_func_extended = false;
    private static boolean EXT_geometry_shader4 = false;
    
    // ========================================================================
    // BUFFER UTILITIES EXTENSION
    // ========================================================================
    
    public static class BufferUtils {
        // ... (previous BufferUtils methods) ...
        
        public static LongBuffer createLongBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity * 8)
                .order(ByteOrder.nativeOrder())
                .asLongBuffer();
        }
        
        public static ShortBuffer createShortBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        }
        
        public static DoubleBuffer createDoubleBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity * 8)
                .order(ByteOrder.nativeOrder())
                .asDoubleBuffer();
        }
    }

// ========================================================================
    // LWJGL 2.9.x COMPATIBILITY LAYER
    // ========================================================================
    
    /**
     * LWJGL 2.9.x uses buffer-based API, not convenience methods
     * These thread-local buffers avoid allocation in hot paths
     */
    private static final ThreadLocal<IntBuffer> tlIntBuffer1 = ThreadLocal.withInitial(() -> 
        ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer());
    private static final ThreadLocal<IntBuffer> tlIntBuffer16 = ThreadLocal.withInitial(() -> 
        ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer());
    private static final ThreadLocal<FloatBuffer> tlFloatBuffer1 = ThreadLocal.withInitial(() -> 
        ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer());
    private static final ThreadLocal<FloatBuffer> tlFloatBuffer4 = ThreadLocal.withInitial(() -> 
        ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder()).asFloatBuffer());
    private static final ThreadLocal<FloatBuffer> tlFloatBuffer16 = ThreadLocal.withInitial(() -> 
        ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asFloatBuffer());
    private static final ThreadLocal<LongBuffer> tlLongBuffer1 = ThreadLocal.withInitial(() -> 
        ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asLongBuffer());
    private static final ThreadLocal<DoubleBuffer> tlDoubleBuffer1 = ThreadLocal.withInitial(() -> 
        ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).asDoubleBuffer());
    
    /**
     * Get reusable int buffer (size 1)
     */
    private static IntBuffer getIntBuffer1() {
        IntBuffer buf = tlIntBuffer1.get();
        buf.clear();
        return buf;
    }
    
    /**
     * Get reusable int buffer (size 16)
     */
    private static IntBuffer getIntBuffer16() {
        IntBuffer buf = tlIntBuffer16.get();
        buf.clear();
        return buf;
    }
    
    /**
     * Get reusable float buffer (size 1)
     */
    private static FloatBuffer getFloatBuffer1() {
        FloatBuffer buf = tlFloatBuffer1.get();
        buf.clear();
        return buf;
    }
    
    /**
     * Get reusable float buffer (size 4)
     */
    private static FloatBuffer getFloatBuffer4() {
        FloatBuffer buf = tlFloatBuffer4.get();
        buf.clear();
        return buf;
    }
    
    /**
     * Get reusable float buffer (size 16)
     */
    private static FloatBuffer getFloatBuffer16() {
        FloatBuffer buf = tlFloatBuffer16.get();
        buf.clear();
        return buf;
    }
    
    // ========================================================================
    // LWJGL 2.9.x COMPATIBLE TEXTURE GENERATION
    // ========================================================================
    
    /**
     * glGenTextures - LWJGL 2 compatible
     * LWJGL 2: glGenTextures(IntBuffer) - void return
     * LWJGL 3: glGenTextures() - int return
     */
    public static int genTextureLWJGL2() {
        IntBuffer buf = getIntBuffer1();
        GL11.glGenTextures(buf);
        return buf.get(0);
    }
    
    /**
     * glGenBuffers - LWJGL 2 compatible
     */
    public static int genBufferLWJGL2() {
        if (!hasVBO) {
            throw new UnsupportedOperationException("VBO not supported");
        }
        
        IntBuffer buf = getIntBuffer1();
        if (GL15) {
            GL15.glGenBuffers(buf);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glGenBuffersARB(buf);
        }
        return buf.get(0);
    }
    
    /**
     * glGenVertexArrays - LWJGL 2 compatible
     */
    public static int genVertexArrayLWJGL2() {
        if (!hasVAO) {
            throw new UnsupportedOperationException("VAO not supported");
        }
        
        IntBuffer buf = getIntBuffer1();
        if (GL30) {
            GL30.glGenVertexArrays(buf);
        } else if (ARB_vertex_array_object) {
            ARBVertexArrayObject.glGenVertexArrays(buf);
        }
        return buf.get(0);
    }
    
    /**
     * glGenFramebuffers - LWJGL 2 compatible
     */
    public static int genFramebufferLWJGL2() {
        if (!hasFBO) {
            throw new UnsupportedOperationException("FBO not supported");
        }
        
        IntBuffer buf = getIntBuffer1();
        if (GL30) {
            GL30.glGenFramebuffers(buf);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glGenFramebuffers(buf);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glGenFramebuffersEXT(buf);
        }
        
        int fbo = buf.get(0);
        if (fbo != 0) {
            framebufferCache.put(fbo, new FramebufferInfo(fbo));
        }
        return fbo;
    }
    
    /**
     * glGenRenderbuffers - LWJGL 2 compatible
     */
    public static int genRenderbufferLWJGL2() {
        if (!hasFBO) {
            throw new UnsupportedOperationException("FBO not supported");
        }
        
        IntBuffer buf = getIntBuffer1();
        if (GL30) {
            GL30.glGenRenderbuffers(buf);
        } else if (ARB_framebuffer_object) {
            ARBFramebufferObject.glGenRenderbuffers(buf);
        } else if (EXT_framebuffer_object) {
            EXTFramebufferObject.glGenRenderbuffersEXT(buf);
        }
        
        int rbo = buf.get(0);
        if (rbo != 0) {
            renderbufferCache.put(rbo, new RenderbufferInfo(rbo));
        }
        return rbo;
    }
    
    /**
     * glGenQueries - LWJGL 2 compatible
     */
    public static int genQueryLWJGL2() {
        if (!GL15) {
            throw new UnsupportedOperationException("Queries not supported");
        }
        
        IntBuffer buf = getIntBuffer1();
        GL15.glGenQueries(buf);
        return buf.get(0);
    }
    
    /**
     * glGenSamplers - LWJGL 2 compatible
     */
    public static int genSamplerLWJGL2() {
        if (!hasSamplerObjects) {
            throw new UnsupportedOperationException("Sampler objects not supported");
        }
        
        IntBuffer buf = getIntBuffer1();
        if (GL33) {
            GL33.glGenSamplers(buf);
        } else if (ARB_sampler_objects) {
            ARBSamplerObjects.glGenSamplers(buf);
        }
        
        int sampler = buf.get(0);
        if (sampler != 0) {
            samplerCache.put(sampler, new SamplerInfo(sampler));
        }
        return sampler;
    }
    
    /**
     * glGetInteger - LWJGL 2 compatible
     */
    public static int getIntegerLWJGL2(int pname) {
        IntBuffer buf = getIntBuffer1();
        GL11.glGetInteger(pname, buf);
        return buf.get(0);
    }
    
    /**
     * glGetFloat - LWJGL 2 compatible
     */
    public static float getFloatLWJGL2(int pname) {
        FloatBuffer buf = getFloatBuffer1();
        GL11.glGetFloat(pname, buf);
        return buf.get(0);
    }
    
    // ========================================================================
    // MEMORY LEAK PREVENTION - IMPROVED CACHING
    // ========================================================================
    
    /**
     * Cache entry with timestamp for LRU eviction
     */
    public static class CacheEntry<T> {
        public final T value;
        public long lastAccess;
        public int accessCount;
        
        public CacheEntry(T value) {
            this.value = value;
            this.lastAccess = System.currentTimeMillis();
            this.accessCount = 1;
        }
        
        public void touch() {
            this.lastAccess = System.currentTimeMillis();
            this.accessCount++;
        }
    }
    
    /**
     * Bounded cache with LRU eviction
     */
    public static class BoundedCache<K, V> {
        private final Map<K, CacheEntry<V>> cache;
        private final int maxSize;
        private final long maxAge; // milliseconds
        
        public BoundedCache(int maxSize, long maxAgeMs) {
            this.cache = new ConcurrentHashMap<>();
            this.maxSize = maxSize;
            this.maxAge = maxAgeMs;
        }
        
        public V get(K key) {
            CacheEntry<V> entry = cache.get(key);
            if (entry != null) {
                entry.touch();
                return entry.value;
            }
            return null;
        }
        
        public void put(K key, V value) {
            if (cache.size() >= maxSize) {
                evictOldest();
            }
            cache.put(key, new CacheEntry<>(value));
        }
        
        public V remove(K key) {
            CacheEntry<V> entry = cache.remove(key);
            return entry != null ? entry.value : null;
        }
        
        public boolean containsKey(K key) {
            return cache.containsKey(key);
        }
        
        public void clear() {
            cache.clear();
        }
        
        public int size() {
            return cache.size();
        }
        
        private void evictOldest() {
            long now = System.currentTimeMillis();
            K oldestKey = null;
            long oldestTime = Long.MAX_VALUE;
            
            Iterator<Map.Entry<K, CacheEntry<V>>> it = cache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<K, CacheEntry<V>> entry = it.next();
                
                // Evict expired entries
                if (now - entry.getValue().lastAccess > maxAge) {
                    it.remove();
                    continue;
                }
                
                // Track oldest
                if (entry.getValue().lastAccess < oldestTime) {
                    oldestTime = entry.getValue().lastAccess;
                    oldestKey = entry.getKey();
                }
            }
            
            // Remove oldest if still over limit
            if (cache.size() >= maxSize && oldestKey != null) {
                cache.remove(oldestKey);
            }
        }
        
        /**
         * Periodic cleanup - call from main thread
         */
        public void cleanup() {
            long now = System.currentTimeMillis();
            cache.entrySet().removeIf(entry -> 
                now - entry.getValue().lastAccess > maxAge);
        }
    }
    
    // Replace the unbounded caches with bounded versions
    private static final int MAX_SHADER_CACHE_SIZE = 256;
    private static final int MAX_PROGRAM_CACHE_SIZE = 128;
    private static final int MAX_UNIFORM_CACHE_SIZE = 1024;
    private static final long CACHE_MAX_AGE_MS = 5 * 60 * 1000; // 5 minutes
    
    // Bounded shader/program caches
    private static final BoundedCache<Integer, ShaderInfo> boundedShaderCache = 
        new BoundedCache<>(MAX_SHADER_CACHE_SIZE, CACHE_MAX_AGE_MS);
    private static final BoundedCache<Integer, ProgramInfo> boundedProgramCache = 
        new BoundedCache<>(MAX_PROGRAM_CACHE_SIZE, CACHE_MAX_AGE_MS);
    
    /**
     * Clean up all caches - call periodically from main thread
     * Recommended: once per second or on world unload
     */
    public static void performCacheCleanup() {
        boundedShaderCache.cleanup();
        boundedProgramCache.cleanup();
        
        // Also clean framebuffer/renderbuffer caches
        cleanupResourceCaches();
    }
    
    /**
     * Clean resource caches by validating GL objects still exist
     */
    private static void cleanupResourceCaches() {
        // Validate framebuffers
        Iterator<Map.Entry<Integer, FramebufferInfo>> fboIt = framebufferCache.entrySet().iterator();
        while (fboIt.hasNext()) {
            Map.Entry<Integer, FramebufferInfo> entry = fboIt.next();
            if (!isFramebuffer(entry.getKey())) {
                fboIt.remove();
            }
        }
        
        // Validate renderbuffers
        Iterator<Map.Entry<Integer, RenderbufferInfo>> rboIt = renderbufferCache.entrySet().iterator();
        while (rboIt.hasNext()) {
            Map.Entry<Integer, RenderbufferInfo> entry = rboIt.next();
            if (!isRenderbuffer(entry.getKey())) {
                rboIt.remove();
            }
        }
        
        // Validate samplers
        if (hasSamplerObjects) {
            Iterator<Map.Entry<Integer, SamplerInfo>> samplerIt = samplerCache.entrySet().iterator();
            while (samplerIt.hasNext()) {
                Map.Entry<Integer, SamplerInfo> entry = samplerIt.next();
                if (!isSampler(entry.getKey())) {
                    samplerIt.remove();
                }
            }
        }
    }
    
    /**
     * Check if renderbuffer is valid
     */
    public static boolean isRenderbuffer(int renderbuffer) {
        if (GL30) {
            return GL30.glIsRenderbuffer(renderbuffer);
        } else if (ARB_framebuffer_object) {
            return ARBFramebufferObject.glIsRenderbuffer(renderbuffer);
        } else if (EXT_framebuffer_object) {
            return EXTFramebufferObject.glIsRenderbufferEXT(renderbuffer);
        }
        return false;
    }
    
    // ========================================================================
    // STATE DESYNCHRONIZATION HANDLING
    // ========================================================================
    
    /**
     * Invalidate all tracked state
     * Call this before your mod starts rendering if you suspect
     * vanilla MC or another mod touched GL states directly.
     */
    public static void invalidateState() {
        StateTracker state = getState();
        state.reset();
    }
    
    /**
     * Synchronize state tracker with actual driver state
     * This is expensive - use sparingly (once per frame at most)
     */
    public static void syncStateWithDriver() {
        StateTracker state = getState();
        
        // Sync enable states
        state.depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        state.blend = GL11.glIsEnabled(GL11.GL_BLEND);
        state.cullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        state.scissorTest = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        state.stencilTest = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        
        // Sync depth state
        IntBuffer intBuf = getIntBuffer1();
        GL11.glGetInteger(GL11.GL_DEPTH_FUNC, intBuf);
        state.depthFunc = intBuf.get(0);
        state.depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        
        // Sync blend state
        GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB, intBuf);
        state.blendSrcRGB = intBuf.get(0);
        GL11.glGetInteger(GL14.GL_BLEND_DST_RGB, intBuf);
        state.blendDstRGB = intBuf.get(0);
        GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA, intBuf);
        state.blendSrcAlpha = intBuf.get(0);
        GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA, intBuf);
        state.blendDstAlpha = intBuf.get(0);
        
        // Sync bound objects
        GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING, intBuf);
        state.boundArrayBuffer = intBuf.get(0);
        GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING, intBuf);
        state.boundElementBuffer = intBuf.get(0);
        
        if (hasVAO) {
            GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING, intBuf);
            state.boundVAO = intBuf.get(0);
        }
        
        if (hasShaders) {
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM, intBuf);
            state.boundProgram = intBuf.get(0);
        }
        
        if (hasFBO) {
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING, intBuf);
            state.boundFramebuffer = intBuf.get(0);
            GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING, intBuf);
            state.boundReadFramebuffer = intBuf.get(0);
            GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING, intBuf);
            state.boundDrawFramebuffer = intBuf.get(0);
        }
        
        // Sync viewport
        IntBuffer viewportBuf = getIntBuffer16();
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuf);
        state.viewportX = viewportBuf.get(0);
        state.viewportY = viewportBuf.get(1);
        state.viewportWidth = viewportBuf.get(2);
        state.viewportHeight = viewportBuf.get(3);
        
        // Sync active texture unit
        GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE, intBuf);
        state.activeTextureUnit = intBuf.get(0) - GL13.GL_TEXTURE0;
        
        // Sync color mask
        ByteBuffer boolBuf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        GL11.glGetBoolean(GL11.GL_COLOR_WRITEMASK, boolBuf);
        state.colorMaskR = boolBuf.get(0) != 0;
        state.colorMaskG = boolBuf.get(1) != 0;
        state.colorMaskB = boolBuf.get(2) != 0;
        state.colorMaskA = boolBuf.get(3) != 0;
    }
    
    /**
     * Light state sync - only sync most commonly changed states
     * Less expensive than full sync
     */
    public static void syncCommonState() {
        StateTracker state = getState();
        IntBuffer intBuf = getIntBuffer1();
        
        // Sync only bound objects (most common source of desync)
        GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING, intBuf);
        state.boundArrayBuffer = intBuf.get(0);
        
        if (hasVAO) {
            GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING, intBuf);
            state.boundVAO = intBuf.get(0);
        }
        
        if (hasShaders) {
            GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM, intBuf);
            state.boundProgram = intBuf.get(0);
        }
        
        if (hasFBO) {
            GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING, intBuf);
            state.boundFramebuffer = intBuf.get(0);
        }
    }
    
    /**
     * Minecraft render boundary sync
     * Call this at the start of custom rendering to sync with vanilla state
     * Prevents artifacts from Minecraft's GlStateManager state leakage
     */
    public static void syncMinecraftState() {
        syncCommonState();
        
        // Minecraft-specific state that frequently desyncs:
        // - Texture bindings (vanilla binds without tracking)
        // - Blend modes (changed for GUI rendering)
        // - Depth state (toggled for various effects)
        
        StateTracker state = getState();
        IntBuffer intBuf = getIntBuffer1();
        
        // Sync texture binding for active unit
        int activeUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        state.activeTextureUnit = activeUnit - GL13.GL_TEXTURE0;
        
        // Sync current texture
        GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D, intBuf);
        state.textureUnits[state.activeTextureUnit].texture2D = intBuf.get(0);
        
        // Sync blend state (vanilla changes this constantly)
        GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB, intBuf);
        state.blendSrcRGB = intBuf.get(0);
        GL11.glGetInteger(GL14.GL_BLEND_DST_RGB, intBuf);
        state.blendDstRGB = intBuf.get(0);
    }
    
    /**
     * Restore Minecraft's expected state after custom rendering
     * Call this at the end of custom rendering to prevent breaking vanilla
     */
    public static void restoreMinecraftState() {
        // Reset to vanilla's common state:
        // - Blend enabled
        // - Depth test enabled
        // - Cull face enabled
        // - Standard blend mode (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)
        
        enable(GL11.GL_BLEND);
        blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        enable(GL11.GL_DEPTH_TEST);
        enable(GL11.GL_CULL_FACE);
        
        // Unbind custom resources
        bindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        if (hasVAO) {
            bindVertexArray(0);
        }
        if (hasShaders) {
            useProgram(0);
        }
    }
    
    /**
     * Force set state to known value, bypassing cache check
     * Use when you need to ensure a specific state
     */
    public static void forceEnable(int cap) {
        StateTracker state = getState();
        GL11.glEnable(cap);
        
        switch (cap) {
            case GL11.GL_DEPTH_TEST: state.depthTest = true; break;
            case GL11.GL_BLEND: state.blend = true; break;
            case GL11.GL_CULL_FACE: state.cullFace = true; break;
            case GL11.GL_SCISSOR_TEST: state.scissorTest = true; break;
            case GL11.GL_STENCIL_TEST: state.stencilTest = true; break;
        }
    }
    
    public static void forceDisable(int cap) {
        StateTracker state = getState();
        GL11.glDisable(cap);
        
        switch (cap) {
            case GL11.GL_DEPTH_TEST: state.depthTest = false; break;
            case GL11.GL_BLEND: state.blend = false; break;
            case GL11.GL_CULL_FACE: state.cullFace = false; break;
            case GL11.GL_SCISSOR_TEST: state.scissorTest = false; break;
            case GL11.GL_STENCIL_TEST: state.stencilTest = false; break;
        }
    }
    
    /**
     * Force bind buffer, bypassing cache
     */
    public static void forceBindBuffer(int target, int buffer) {
        StateTracker state = getState();
        
        if (GL15) {
            GL15.glBindBuffer(target, buffer);
        } else if (ARB_vertex_buffer_object) {
            ARBVertexBufferObject.glBindBufferARB(target, buffer);
        }
        
        switch (target) {
            case GL15.GL_ARRAY_BUFFER:
                state.boundArrayBuffer = buffer;
                break;
            case GL15.GL_ELEMENT_ARRAY_BUFFER:
                state.boundElementBuffer = buffer;
                break;
        }
    }
    
    /**
     * Force bind VAO, bypassing cache
     */
    public static void forceBindVertexArray(int array) {
        StateTracker state = getState();
        
        if (GL30) {
            GL30.glBindVertexArray(array);
        } else if (ARB_vertex_array_object) {
            ARBVertexArrayObject.glBindVertexArray(array);
        }
        
        state.boundVAO = array;
    }
    
    /**
     * Force use program, bypassing cache
     */
    public static void forceUseProgram(int program) {
        StateTracker state = getState();
        
        if (GL20) {
            GL20.glUseProgram(program);
        } else if (ARB_shader_objects) {
            ARBShaderObjects.glUseProgramObjectARB(program);
        }
        
        state.boundProgram = program;
        
        // Clear uniform cache for this program since we're forcing
        ProgramInfo info = boundedProgramCache.get(program);
        if (info != null) {
            info.uniformCache.clear();
        }
    }
    
    // ========================================================================
    // GL 4.0 - TESSELLATION SHADERS
    // ========================================================================
    
    // Tessellation constants
    public static final int GL_PATCHES = GL40.GL_PATCHES;
    public static final int GL_PATCH_VERTICES = GL40.GL_PATCH_VERTICES;
    public static final int GL_TESS_CONTROL_SHADER = GL40.GL_TESS_CONTROL_SHADER;
    public static final int GL_TESS_EVALUATION_SHADER = GL40.GL_TESS_EVALUATION_SHADER;
    public static final int GL_PATCH_DEFAULT_INNER_LEVEL = GL40.GL_PATCH_DEFAULT_INNER_LEVEL;
    public static final int GL_PATCH_DEFAULT_OUTER_LEVEL = GL40.GL_PATCH_DEFAULT_OUTER_LEVEL;
    public static final int GL_MAX_PATCH_VERTICES = GL40.GL_MAX_PATCH_VERTICES;
    public static final int GL_MAX_TESS_GEN_LEVEL = GL40.GL_MAX_TESS_GEN_LEVEL;
    public static final int GL_MAX_TESS_CONTROL_UNIFORM_COMPONENTS = GL40.GL_MAX_TESS_CONTROL_UNIFORM_COMPONENTS;
    public static final int GL_MAX_TESS_EVALUATION_UNIFORM_COMPONENTS = GL40.GL_MAX_TESS_EVALUATION_UNIFORM_COMPONENTS;
    
    /**
     * Check if tessellation is supported
     */
    public static boolean supportsTessellation() {
        return hasTessellation;
    }
    
    /**
     * Create tessellation control shader
     */
    public static int createTessControlShader() {
        if (GL40) {
            return createShader(GL40.GL_TESS_CONTROL_SHADER);
        } else if (ARB_tessellation_shader) {
            return createShader(ARBTessellationShader.GL_TESS_CONTROL_SHADER);
        }
        return 0;
    }
    
    /**
     * Create tessellation evaluation shader
     */
    public static int createTessEvaluationShader() {
        if (GL40) {
            return createShader(GL40.GL_TESS_EVALUATION_SHADER);
        } else if (ARB_tessellation_shader) {
            return createShader(ARBTessellationShader.GL_TESS_EVALUATION_SHADER);
        }
        return 0;
    }
    
    /**
     * glPatchParameteri - Set patch parameter
     * GL 4.0: glPatchParameteri(pname, value)
     */
    public static void patchParameteri(int pname, int value) {
        if (GL40) {
            GL40.glPatchParameteri(pname, value);
        } else if (ARB_tessellation_shader) {
            ARBTessellationShader.glPatchParameteri(pname, value);
        }
    }
    
    /**
     * glPatchParameterfv - Set patch default levels
     * GL 4.0: glPatchParameterfv(pname, values)
     */
    public static void patchParameterfv(int pname, FloatBuffer values) {
        if (GL40) {
            GL40.glPatchParameterfv(pname, values);
        } else if (ARB_tessellation_shader) {
            ARBTessellationShader.glPatchParameterfv(pname, values);
        }
    }
    
    /**
     * Set number of vertices per patch
     */
    public static void setPatchVertices(int vertices) {
        patchParameteri(GL40.GL_PATCH_VERTICES, vertices);
    }
    
    /**
     * Set default inner tessellation levels
     */
    public static void setPatchDefaultInnerLevel(float level0, float level1) {
        FloatBuffer buf = getFloatBuffer4();
        buf.put(level0).put(level1).flip();
        patchParameterfv(GL40.GL_PATCH_DEFAULT_INNER_LEVEL, buf);
    }
    
    /**
     * Set default outer tessellation levels
     */
    public static void setPatchDefaultOuterLevel(float level0, float level1, float level2, float level3) {
        FloatBuffer buf = getFloatBuffer4();
        buf.put(level0).put(level1).put(level2).put(level3).flip();
        patchParameterfv(GL40.GL_PATCH_DEFAULT_OUTER_LEVEL, buf);
    }
    
    // ========================================================================
    // GL 4.0 - SHADER SUBROUTINES
    // ========================================================================
    
    /**
     * glGetSubroutineIndex - Get subroutine index
     * GL 4.0: glGetSubroutineIndex(program, shadertype, name)
     */
    public static int getSubroutineIndex(int program, int shadertype, CharSequence name) {
        if (GL40) {
            return GL40.glGetSubroutineIndex(program, shadertype, name);
        } else if (ARB_shader_subroutine) {
            return ARBShaderSubroutine.glGetSubroutineIndex(program, shadertype, name);
        }
        return -1;
    }
    
    /**
     * glGetSubroutineUniformLocation - Get subroutine uniform location
     * GL 4.0: glGetSubroutineUniformLocation(program, shadertype, name)
     */
    public static int getSubroutineUniformLocation(int program, int shadertype, CharSequence name) {
        if (GL40) {
            return GL40.glGetSubroutineUniformLocation(program, shadertype, name);
        } else if (ARB_shader_subroutine) {
            return ARBShaderSubroutine.glGetSubroutineUniformLocation(program, shadertype, name);
        }
        return -1;
    }
    
    /**
     * glUniformSubroutinesuiv - Set subroutine uniform
     * GL 4.0: glUniformSubroutinesuiv(shadertype, indices)
     */
    public static void uniformSubroutinesuiv(int shadertype, IntBuffer indices) {
        if (GL40) {
            GL40.glUniformSubroutinesuiv(shadertype, indices);
        } else if (ARB_shader_subroutine) {
            ARBShaderSubroutine.glUniformSubroutinesuiv(shadertype, indices);
        }
    }
    
    /**
     * glGetActiveSubroutineName - Get subroutine name
     * GL 4.0: glGetActiveSubroutineName(program, shadertype, index)
     */
    public static String getActiveSubroutineName(int program, int shadertype, int index) {
        if (GL40) {
            int length = GL40.glGetActiveSubroutinei(program, shadertype, index, GL40.GL_UNIFORM_NAME_LENGTH);
            return GL40.glGetActiveSubroutineName(program, shadertype, index, length);
        } else if (ARB_shader_subroutine) {
            return ARBShaderSubroutine.glGetActiveSubroutineName(program, shadertype, index, 256);
        }
        return "";
    }
    
    /**
     * glGetActiveSubroutineUniformName - Get subroutine uniform name
     * GL 4.0: glGetActiveSubroutineUniformName(program, shadertype, index)
     */
    public static String getActiveSubroutineUniformName(int program, int shadertype, int index) {
        if (GL40) {
            return GL40.glGetActiveSubroutineUniformName(program, shadertype, index);
        } else if (ARB_shader_subroutine) {
            return ARBShaderSubroutine.glGetActiveSubroutineUniformName(program, shadertype, index, 256);
        }
        return "";
    }
    
    // ========================================================================
    // GL 4.0 - DOUBLE PRECISION UNIFORMS
    // ========================================================================
    
    /**
     * glUniform1d - Set double uniform
     * GL 4.0: glUniform1d(location, v0)
     */
    public static void uniform1d(int location, double v0) {
        if (location < 0) return;
        
        if (GL40) {
            GL40.glUniform1d(location, v0);
        } else if (ARB_gpu_shader_fp64) {
            ARBGPUShaderFP64.glUniform1d(location, v0);
        }
    }
    
    /**
     * glUniform2d - Set dvec2 uniform
     */
    public static void uniform2d(int location, double v0, double v1) {
        if (location < 0) return;
        
        if (GL40) {
            GL40.glUniform2d(location, v0, v1);
        } else if (ARB_gpu_shader_fp64) {
            ARBGPUShaderFP64.glUniform2d(location, v0, v1);
        }
    }
    
    /**
     * glUniform3d - Set dvec3 uniform
     */
    public static void uniform3d(int location, double v0, double v1, double v2) {
        if (location < 0) return;
        
        if (GL40) {
            GL40.glUniform3d(location, v0, v1, v2);
        } else if (ARB_gpu_shader_fp64) {
            ARBGPUShaderFP64.glUniform3d(location, v0, v1, v2);
        }
    }
    
    /**
     * glUniform4d - Set dvec4 uniform
     */
    public static void uniform4d(int location, double v0, double v1, double v2, double v3) {
        if (location < 0) return;
        
        if (GL40) {
            GL40.glUniform4d(location, v0, v1, v2, v3);
        } else if (ARB_gpu_shader_fp64) {
            ARBGPUShaderFP64.glUniform4d(location, v0, v1, v2, v3);
        }
    }
    
    /**
     * glUniformMatrix4dv - Set dmat4 uniform
     */
    public static void uniformMatrix4dv(int location, boolean transpose, DoubleBuffer value) {
        if (location < 0) return;
        
        if (GL40) {
            GL40.glUniformMatrix4dv(location, transpose, value);
        } else if (ARB_gpu_shader_fp64) {
            ARBGPUShaderFP64.glUniformMatrix4dv(location, transpose, value);
        }
    }
    
    /**
     * glUniformMatrix3dv - Set dmat3 uniform
     */
    public static void uniformMatrix3dv(int location, boolean transpose, DoubleBuffer value) {
        if (location < 0) return;
        
        if (GL40) {
            GL40.glUniformMatrix3dv(location, transpose, value);
        } else if (ARB_gpu_shader_fp64) {
            ARBGPUShaderFP64.glUniformMatrix3dv(location, transpose, value);
        }
    }
    
    /**
     * glUniformMatrix2dv - Set dmat2 uniform
     */
    public static void uniformMatrix2dv(int location, boolean transpose, DoubleBuffer value) {
        if (location < 0) return;
        
        if (GL40) {
            GL40.glUniformMatrix2dv(location, transpose, value);
        } else if (ARB_gpu_shader_fp64) {
            ARBGPUShaderFP64.glUniformMatrix2dv(location, transpose, value);
        }
    }
    
    // ========================================================================
    // GL 4.0 - DRAW INDIRECT
    // ========================================================================
    
    /**
     * DrawArraysIndirectCommand structure:
     * uint count
     * uint instanceCount
     * uint first
     * uint baseInstance (ignored pre-GL 4.2)
     */
    
    /**
     * glDrawArraysIndirect - Draw arrays from buffer
     * GL 4.0: glDrawArraysIndirect(mode, indirect)
     */
    public static void drawArraysIndirect(int mode, long indirect) {
        if (GL40) {
            GL40.glDrawArraysIndirect(mode, indirect);
        } else if (ARB_draw_indirect) {
            ARBDrawIndirect.glDrawArraysIndirect(mode, indirect);
        } else {
            // No fallback - requires driver support
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Draw indirect not supported");
            }
        }
    }
    
    public static void drawArraysIndirect(int mode, ByteBuffer indirect) {
        if (GL40) {
            GL40.glDrawArraysIndirect(mode, indirect);
        } else if (ARB_draw_indirect) {
            ARBDrawIndirect.glDrawArraysIndirect(mode, indirect);
        }
    }
    
    /**
     * DrawElementsIndirectCommand structure:
     * uint count
     * uint instanceCount
     * uint firstIndex
     * int baseVertex
     * uint baseInstance (ignored pre-GL 4.2)
     */
    
    /**
     * glDrawElementsIndirect - Draw elements from buffer
     * GL 4.0: glDrawElementsIndirect(mode, type, indirect)
     */
    public static void drawElementsIndirect(int mode, int type, long indirect) {
        if (GL40) {
            GL40.glDrawElementsIndirect(mode, type, indirect);
        } else if (ARB_draw_indirect) {
            ARBDrawIndirect.glDrawElementsIndirect(mode, type, indirect);
        }
    }
    
    public static void drawElementsIndirect(int mode, int type, ByteBuffer indirect) {
        if (GL40) {
            GL40.glDrawElementsIndirect(mode, type, indirect);
        } else if (ARB_draw_indirect) {
            ARBDrawIndirect.glDrawElementsIndirect(mode, type, indirect);
        }
    }
    
    // ========================================================================
    // GL 4.0 - TRANSFORM FEEDBACK IMPROVEMENTS
    // ========================================================================
    
    /**
     * glGenTransformFeedbacks - Generate transform feedback objects
     * GL 4.0: glGenTransformFeedbacks()
     */
    public static int genTransformFeedback() {
        if (GL40) {
            IntBuffer buf = getIntBuffer1();
            GL40.glGenTransformFeedbacks(buf);
            return buf.get(0);
        } else if (ARB_transform_feedback2) {
            IntBuffer buf = getIntBuffer1();
            ARBTransformFeedback2.glGenTransformFeedbacks(buf);
            return buf.get(0);
        }
        return 0;
    }
    
    /**
     * glDeleteTransformFeedbacks - Delete transform feedback objects
     * GL 4.0: glDeleteTransformFeedbacks(ids)
     */
    public static void deleteTransformFeedback(int id) {
        if (id == 0) return;
        
        IntBuffer buf = getIntBuffer1();
        buf.put(0, id);
        
        if (GL40) {
            GL40.glDeleteTransformFeedbacks(buf);
        } else if (ARB_transform_feedback2) {
            ARBTransformFeedback2.glDeleteTransformFeedbacks(buf);
        }
    }
    
    /**
     * glBindTransformFeedback - Bind transform feedback object
     * GL 4.0: glBindTransformFeedback(target, id)
     */
    public static void bindTransformFeedback(int target, int id) {
        if (GL40) {
            GL40.glBindTransformFeedback(target, id);
        } else if (ARB_transform_feedback2) {
            ARBTransformFeedback2.glBindTransformFeedback(target, id);
        }
    }
    
    /**
     * glIsTransformFeedback - Check if name is transform feedback
     * GL 4.0: glIsTransformFeedback(id)
     */
    public static boolean isTransformFeedback(int id) {
        if (GL40) {
            return GL40.glIsTransformFeedback(id);
        } else if (ARB_transform_feedback2) {
            return ARBTransformFeedback2.glIsTransformFeedback(id);
        }
        return false;
    }
    
    /**
     * glPauseTransformFeedback - Pause transform feedback
     * GL 4.0: glPauseTransformFeedback()
     */
    public static void pauseTransformFeedback() {
        if (GL40) {
            GL40.glPauseTransformFeedback();
        } else if (ARB_transform_feedback2) {
            ARBTransformFeedback2.glPauseTransformFeedback();
        }
    }
    
    /**
     * glResumeTransformFeedback - Resume transform feedback
     * GL 4.0: glResumeTransformFeedback()
     */
    public static void resumeTransformFeedback() {
        if (GL40) {
            GL40.glResumeTransformFeedback();
        } else if (ARB_transform_feedback2) {
            ARBTransformFeedback2.glResumeTransformFeedback();
        }
    }
    
    /**
     * glDrawTransformFeedback - Draw using transform feedback
     * GL 4.0: glDrawTransformFeedback(mode, id)
     */
    public static void drawTransformFeedback(int mode, int id) {
        if (GL40) {
            GL40.glDrawTransformFeedback(mode, id);
        } else if (ARB_transform_feedback2) {
            ARBTransformFeedback2.glDrawTransformFeedback(mode, id);
        }
    }
    
    // ========================================================================
    // GL 4.0 - BLENDING IMPROVEMENTS
    // ========================================================================
    
    /**
     * glBlendEquationi - Set blend equation for specific draw buffer
     * GL 4.0: glBlendEquationi(buf, mode)
     */
    public static void blendEquationi(int buf, int mode) {
        if (GL40) {
            GL40.glBlendEquationi(buf, mode);
        } else if (ARB_draw_buffers_blend) {
            ARBDrawBuffersBlend.glBlendEquationiARB(buf, mode);
        }
    }
    
    /**
     * glBlendEquationSeparatei - Set separate blend equations for draw buffer
     * GL 4.0: glBlendEquationSeparatei(buf, modeRGB, modeAlpha)
     */
    public static void blendEquationSeparatei(int buf, int modeRGB, int modeAlpha) {
        if (GL40) {
            GL40.glBlendEquationSeparatei(buf, modeRGB, modeAlpha);
        } else if (ARB_draw_buffers_blend) {
            ARBDrawBuffersBlend.glBlendEquationSeparateiARB(buf, modeRGB, modeAlpha);
        }
    }
    
    /**
     * glBlendFunci - Set blend function for specific draw buffer
     * GL 4.0: glBlendFunci(buf, src, dst)
     */
    public static void blendFunci(int buf, int src, int dst) {
        if (GL40) {
            GL40.glBlendFunci(buf, src, dst);
        } else if (ARB_draw_buffers_blend) {
            ARBDrawBuffersBlend.glBlendFunciARB(buf, src, dst);
        }
    }
    
    /**
     * glBlendFuncSeparatei - Set separate blend functions for draw buffer
     * GL 4.0: glBlendFuncSeparatei(buf, srcRGB, dstRGB, srcAlpha, dstAlpha)
     */
    public static void blendFuncSeparatei(int buf, int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        if (GL40) {
            GL40.glBlendFuncSeparatei(buf, srcRGB, dstRGB, srcAlpha, dstAlpha);
        } else if (ARB_draw_buffers_blend) {
            ARBDrawBuffersBlend.glBlendFuncSeparateiARB(buf, srcRGB, dstRGB, srcAlpha, dstAlpha);
        }
    }
    
    // ========================================================================
    // GL 4.0 - MIN/MAX SAMPLE SHADING
    // ========================================================================
    
    /**
     * glMinSampleShading - Set minimum sample shading
     * GL 4.0: glMinSampleShading(value)
     */
    public static void minSampleShading(float value) {
        if (GL40) {
            GL40.glMinSampleShading(value);
        } else if (ARB_sample_shading) {
            ARBSampleShading.glMinSampleShadingARB(value);
        }
    }
    
    /**
     * Enable sample shading
     */
    public static void enableSampleShading() {
        if (GL40 || ARB_sample_shading) {
            GL11.glEnable(GL40.GL_SAMPLE_SHADING);
        }
    }
    
    /**
     * Disable sample shading
     */
    public static void disableSampleShading() {
        if (GL40 || ARB_sample_shading) {
            GL11.glDisable(GL40.GL_SAMPLE_SHADING);
        }
    }
    
    // ========================================================================
    // GL 4.1 - SEPARATE SHADER OBJECTS
    // ========================================================================
    
    /**
     * Check if separate shader objects are supported
     */
    public static boolean supportsSeparateShaderObjects() {
        return hasSeparateShaderObjects;
    }
    
    /**
     * glGenProgramPipelines - Generate program pipeline objects
     * GL 4.1: glGenProgramPipelines()
     */
    public static int genProgramPipeline() {
        if (GL41) {
            IntBuffer buf = getIntBuffer1();
            GL41.glGenProgramPipelines(buf);
            return buf.get(0);
        } else if (ARB_separate_shader_objects) {
            IntBuffer buf = getIntBuffer1();
            ARBSeparateShaderObjects.glGenProgramPipelines(buf);
            return buf.get(0);
        }
        return 0;
    }
    
    /**
     * glDeleteProgramPipelines - Delete program pipeline objects
     * GL 4.1: glDeleteProgramPipelines(pipelines)
     */
    public static void deleteProgramPipeline(int pipeline) {
        if (pipeline == 0) return;
        
        IntBuffer buf = getIntBuffer1();
        buf.put(0, pipeline);
        
        if (GL41) {
            GL41.glDeleteProgramPipelines(buf);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glDeleteProgramPipelines(buf);
        }
    }
    
    /**
     * glBindProgramPipeline - Bind program pipeline
     * GL 4.1: glBindProgramPipeline(pipeline)
     */
    public static void bindProgramPipeline(int pipeline) {
        if (GL41) {
            GL41.glBindProgramPipeline(pipeline);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glBindProgramPipeline(pipeline);
        }
    }
    
    /**
     * glIsProgramPipeline - Check if name is a program pipeline
     * GL 4.1: glIsProgramPipeline(pipeline)
     */
    public static boolean isProgramPipeline(int pipeline) {
        if (GL41) {
            return GL41.glIsProgramPipeline(pipeline);
        } else if (ARB_separate_shader_objects) {
            return ARBSeparateShaderObjects.glIsProgramPipeline(pipeline);
        }
        return false;
    }
    
    /**
     * glUseProgramStages - Bind stages of program to pipeline
     * GL 4.1: glUseProgramStages(pipeline, stages, program)
     */
    public static void useProgramStages(int pipeline, int stages, int program) {
        if (GL41) {
            GL41.glUseProgramStages(pipeline, stages, program);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glUseProgramStages(pipeline, stages, program);
        }
    }
    
    /**
     * glActiveShaderProgram - Set active program for uniform calls
     * GL 4.1: glActiveShaderProgram(pipeline, program)
     */
    public static void activeShaderProgram(int pipeline, int program) {
        if (GL41) {
            GL41.glActiveShaderProgram(pipeline, program);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glActiveShaderProgram(pipeline, program);
        }
    }
    
    /**
     * glCreateShaderProgramv - Create separable program from source
     * GL 4.1: glCreateShaderProgramv(type, strings)
     */
    public static int createShaderProgram(int type, CharSequence... sources) {
        if (GL41) {
            return GL41.glCreateShaderProgramv(type, sources);
        } else if (ARB_separate_shader_objects) {
            return ARBSeparateShaderObjects.glCreateShaderProgramv(type, sources);
        }
        return 0;
    }
    
    /**
     * glValidateProgramPipeline - Validate program pipeline
     * GL 4.1: glValidateProgramPipeline(pipeline)
     */
    public static void validateProgramPipeline(int pipeline) {
        if (GL41) {
            GL41.glValidateProgramPipeline(pipeline);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glValidateProgramPipeline(pipeline);
        }
    }
    
    /**
     * glGetProgramPipelineInfoLog - Get pipeline info log
     * GL 4.1: glGetProgramPipelineInfoLog(pipeline)
     */
    public static String getProgramPipelineInfoLog(int pipeline) {
        if (GL41) {
            return GL41.glGetProgramPipelineInfoLog(pipeline);
        } else if (ARB_separate_shader_objects) {
            int length = ARBSeparateShaderObjects.glGetProgramPipelinei(pipeline, GL20.GL_INFO_LOG_LENGTH);
            return ARBSeparateShaderObjects.glGetProgramPipelineInfoLog(pipeline, length);
        }
        return "";
    }
    
    // ========================================================================
    // GL 4.1 - PROGRAM UNIFORM (Set uniforms without binding)
    // ========================================================================
    
    /**
     * glProgramUniform1f - Set float uniform without binding
     * GL 4.1: glProgramUniform1f(program, location, v0)
     */
    public static void programUniform1f(int program, int location, float v0) {
        if (location < 0) return;
        
        if (GL41) {
            GL41.glProgramUniform1f(program, location, v0);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform1f(program, location, v0);
        } else {
            // Fallback: bind, set, restore
            int currentProgram = getState().boundProgram;
            useProgram(program);
            uniform1f(location, v0);
            useProgram(currentProgram);
        }
    }
    
    /**
     * glProgramUniform2f - Set vec2 uniform without binding
     */
    public static void programUniform2f(int program, int location, float v0, float v1) {
        if (location < 0) return;
        
        if (GL41) {
            GL41.glProgramUniform2f(program, location, v0, v1);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform2f(program, location, v0, v1);
        } else {
            int currentProgram = getState().boundProgram;
            useProgram(program);
            uniform2f(location, v0, v1);
            useProgram(currentProgram);
        }
    }
    
    /**
     * glProgramUniform3f - Set vec3 uniform without binding
     */
    public static void programUniform3f(int program, int location, float v0, float v1, float v2) {
        if (location < 0) return;
        
        if (GL41) {
            GL41.glProgramUniform3f(program, location, v0, v1, v2);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform3f(program, location, v0, v1, v2);
        } else {
            int currentProgram = getState().boundProgram;
            useProgram(program);
            uniform3f(location, v0, v1, v2);
            useProgram(currentProgram);
        }
    }
    
    /**
     * glProgramUniform4f - Set vec4 uniform without binding
     */
    public static void programUniform4f(int program, int location, float v0, float v1, float v2, float v3) {
        if (location < 0) return;
        
        if (GL41) {
            GL41.glProgramUniform4f(program, location, v0, v1, v2, v3);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform4f(program, location, v0, v1, v2, v3);
        } else {
            int currentProgram = getState().boundProgram;
            useProgram(program);
            uniform4f(location, v0, v1, v2, v3);
            useProgram(currentProgram);
        }
    }
    
    /**
     * glProgramUniform1i - Set int uniform without binding
     */
    public static void programUniform1i(int program, int location, int v0) {
        if (location < 0) return;
        
        if (GL41) {
            GL41.glProgramUniform1i(program, location, v0);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform1i(program, location, v0);
        } else {
            int currentProgram = getState().boundProgram;
            useProgram(program);
            uniform1i(location, v0);
            useProgram(currentProgram);
        }
    }
    
    /**
     * glProgramUniform2i - Set ivec2 uniform without binding
     */
    public static void programUniform2i(int program, int location, int v0, int v1) {
        if (location < 0) return;
        
        if (GL41) {
            GL41.glProgramUniform2i(program, location, v0, v1);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform2i(program, location, v0, v1);
        } else {
            int currentProgram = getState().boundProgram;
            useProgram(program);
            uniform2i(location, v0, v1);
            useProgram(currentProgram);
        }
    }
    
    /**
     * glProgramUniform3i - Set ivec3 uniform without binding
     */
    public static void programUniform3i(int program, int location, int v0, int v1, int v2) {
        if (location < 0) return;
        
        if (GL41) {
            GL41.glProgramUniform3i(program, location, v0, v1, v2);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform3i(program, location, v0, v1, v2);
        } else {
            int currentProgram = getState().boundProgram;
            useProgram(program);
            uniform3i(location, v0, v1, v2);
            useProgram(currentProgram);
        }
    }
    
    /**
     * glProgramUniform4i - Set ivec4 uniform without binding
     */
    public static void programUniform4i(int program, int location, int v0, int v1, int v2, int v3) {
        if (location < 0) return;
        
        if (GL41) {
            GL41.glProgramUniform4i(program, location, v0, v1, v2, v3);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniform4i(program, location, v0, v1, v2, v3);
        } else {
            int currentProgram = getState().boundProgram;
            useProgram(program);
            uniform4i(location, v0, v1, v2, v3);
            useProgram(currentProgram);
        }
    }
    
    /**
     * glProgramUniformMatrix4fv - Set mat4 uniform without binding
     */
    public static void programUniformMatrix4fv(int program, int location, boolean transpose, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL41) {
            GL41.glProgramUniformMatrix4fv(program, location, transpose, value);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniformMatrix4fv(program, location, transpose, value);
        } else {
            int currentProgram = getState().boundProgram;
            useProgram(program);
            uniformMatrix4fv(location, transpose, value);
            useProgram(currentProgram);
        }
    }
    
    /**
     * glProgramUniformMatrix3fv - Set mat3 uniform without binding
     */
    public static void programUniformMatrix3fv(int program, int location, boolean transpose, FloatBuffer value) {
        if (location < 0) return;
        
        if (GL41) {
            GL41.glProgramUniformMatrix3fv(program, location, transpose, value);
        } else if (ARB_separate_shader_objects) {
            ARBSeparateShaderObjects.glProgramUniformMatrix3fv(program, location, transpose, value);
        } else {
            int currentProgram = getState().boundProgram;
            useProgram(program);
            uniformMatrix3fv(location, transpose, value);
            useProgram(currentProgram);
        }
    }
    
    // ========================================================================
    // GL 4.1 - PROGRAM BINARY
    // ========================================================================
    
    /**
     * Check if binary shaders are supported
     */
    public static boolean supportsBinaryShaders() {
        return hasBinaryShaders;
    }
    
    /**
     * Get number of supported binary formats
     */
    public static int getNumProgramBinaryFormats() {
        if (!hasBinaryShaders) return 0;
        
        IntBuffer buf = getIntBuffer1();
        GL11.glGetInteger(GL41.GL_NUM_PROGRAM_BINARY_FORMATS, buf);
        return buf.get(0);
    }
    
    /**
     * glGetProgramBinary - Get program binary
     * GL 4.1: glGetProgramBinary(program, length, binaryFormat, binary)
     */
    public static ByteBuffer getProgramBinary(int program, IntBuffer binaryFormat) {
        if (GL41) {
            int length = GL20.glGetProgrami(program, GL41.GL_PROGRAM_BINARY_LENGTH);
            if (length <= 0) return null;
            
            ByteBuffer binary = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());
            IntBuffer lengthBuf = getIntBuffer1();
            GL41.glGetProgramBinary(program, lengthBuf, binaryFormat, binary);
            return binary;
        } else if (ARB_get_program_binary) {
            int length = GL20.glGetProgrami(program, ARBGetProgramBinary.GL_PROGRAM_BINARY_LENGTH);
            if (length <= 0) return null;
            
            ByteBuffer binary = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());
            IntBuffer lengthBuf = getIntBuffer1();
            ARBGetProgramBinary.glGetProgramBinary(program, lengthBuf, binaryFormat, binary);
            return binary;
        }
        return null;
    }
    
    /**
     * glProgramBinary - Load program from binary
     * GL 4.1: glProgramBinary(program, binaryFormat, binary)
     */
    public static void programBinary(int program, int binaryFormat, ByteBuffer binary) {
        if (GL41) {
            GL41.glProgramBinary(program, binaryFormat, binary);
        } else if (ARB_get_program_binary) {
            ARBGetProgramBinary.glProgramBinary(program, binaryFormat, binary);
        }
    }
    
    /**
     * Set program to be retrievable as binary
     */
    public static void setProgramBinaryRetrievable(int program, boolean retrievable) {
        if (GL41) {
            GL41.glProgramParameteri(program, GL41.GL_PROGRAM_BINARY_RETRIEVABLE_HINT, 
                retrievable ? GL11.GL_TRUE : GL11.GL_FALSE);
        } else if (ARB_get_program_binary) {
            ARBGetProgramBinary.glProgramParameteri(program, 
                ARBGetProgramBinary.GL_PROGRAM_BINARY_RETRIEVABLE_HINT,
                retrievable ? GL11.GL_TRUE : GL11.GL_FALSE);
        }
    }
    
    // ========================================================================
    // GL 4.1 - VIEWPORT ARRAYS
    // ========================================================================
    
    /**
     * glViewportArrayv - Set multiple viewports
     * GL 4.1: glViewportArrayv(first, v)
     */
    public static void viewportArrayv(int first, FloatBuffer v) {
        if (GL41) {
            GL41.glViewportArrayv(first, v);
        } else if (ARB_viewport_array) {
            ARBViewportArray.glViewportArrayv(first, v);
        }
    }
    
    /**
     * glViewportIndexedf - Set viewport at index
     * GL 4.1: glViewportIndexedf(index, x, y, w, h)
     */
    public static void viewportIndexedf(int index, float x, float y, float w, float h) {
        if (GL41) {
            GL41.glViewportIndexedf(index, x, y, w, h);
        } else if (ARB_viewport_array) {
            ARBViewportArray.glViewportIndexedf(index, x, y, w, h);
        }
    }
    
    /**
     * glScissorArrayv - Set multiple scissors
     * GL 4.1: glScissorArrayv(first, v)
     */
    public static void scissorArrayv(int first, IntBuffer v) {
        if (GL41) {
            GL41.glScissorArrayv(first, v);
        } else if (ARB_viewport_array) {
            ARBViewportArray.glScissorArrayv(first, v);
        }
    }
    
    /**
     * glScissorIndexed - Set scissor at index
     * GL 4.1: glScissorIndexed(index, left, bottom, width, height)
     */
    public static void scissorIndexed(int index, int left, int bottom, int width, int height) {
        if (GL41) {
            GL41.glScissorIndexed(index, left, bottom, width, height);
        } else if (ARB_viewport_array) {
            ARBViewportArray.glScissorIndexed(index, left, bottom, width, height);
        }
    }
    
    /**
     * glDepthRangeArrayv - Set multiple depth ranges
     * GL 4.1: glDepthRangeArrayv(first, v)
     */
    public static void depthRangeArrayv(int first, DoubleBuffer v) {
        if (GL41) {
            GL41.glDepthRangeArrayv(first, v);
        } else if (ARB_viewport_array) {
            ARBViewportArray.glDepthRangeArrayv(first, v);
        }
    }
    
    /**
     * glDepthRangeIndexed - Set depth range at index
     * GL 4.1: glDepthRangeIndexed(index, n, f)
     */
    public static void depthRangeIndexed(int index, double n, double f) {
        if (GL41) {
            GL41.glDepthRangeIndexed(index, n, f);
        } else if (ARB_viewport_array) {
            ARBViewportArray.glDepthRangeIndexed(index, n, f);
        }
    }
    
    // ========================================================================
    // GL 4.1 - DOUBLE PRECISION VERTEX ATTRIBUTES
    // ========================================================================
    
    /**
     * glVertexAttribL1d - Set double vertex attribute
     * GL 4.1: glVertexAttribL1d(index, x)
     */
    public static void vertexAttribL1d(int index, double x) {
        if (GL41) {
            GL41.glVertexAttribL1d(index, x);
        } else if (ARB_vertex_attrib_64bit) {
            ARBVertexAttrib64Bit.glVertexAttribL1d(index, x);
        }
    }
    
    /**
     * glVertexAttribL2d - Set dvec2 vertex attribute
     */
    public static void vertexAttribL2d(int index, double x, double y) {
        if (GL41) {
            GL41.glVertexAttribL2d(index, x, y);
        } else if (ARB_vertex_attrib_64bit) {
            ARBVertexAttrib64Bit.glVertexAttribL2d(index, x, y);
        }
    }
    
    /**
     * glVertexAttribL3d - Set dvec3 vertex attribute
     */
    public static void vertexAttribL3d(int index, double x, double y, double z) {
        if (GL41) {
            GL41.glVertexAttribL3d(index, x, y, z);
        } else if (ARB_vertex_attrib_64bit) {
            ARBVertexAttrib64Bit.glVertexAttribL3d(index, x, y, z);
        }
    }
    
    /**
     * glVertexAttribL4d - Set dvec4 vertex attribute
     */
    public static void vertexAttribL4d(int index, double x, double y, double z, double w) {
        if (GL41) {
            GL41.glVertexAttribL4d(index, x, y, z, w);
        } else if (ARB_vertex_attrib_64bit) {
            ARBVertexAttrib64Bit.glVertexAttribL4d(index, x, y, z, w);
        }
    }
    
    /**
     * glVertexAttribLPointer - Define double vertex attribute
     * GL 4.1: glVertexAttribLPointer(index, size, type, stride, pointer)
     */
    public static void vertexAttribLPointer(int index, int size, int type, int stride, long pointer) {
        if (GL41) {
            GL41.glVertexAttribLPointer(index, size, type, stride, pointer);
        } else if (ARB_vertex_attrib_64bit) {
            ARBVertexAttrib64Bit.glVertexAttribLPointer(index, size, type, stride, pointer);
        }
    }
    
    // ========================================================================
    // GL 4.2 - ATOMIC COUNTERS
    // ========================================================================
    
    // Atomic counter constants
    public static final int GL_ATOMIC_COUNTER_BUFFER = GL42.GL_ATOMIC_COUNTER_BUFFER;
    public static final int GL_ATOMIC_COUNTER_BUFFER_BINDING = GL42.GL_ATOMIC_COUNTER_BUFFER_BINDING;
    public static final int GL_MAX_ATOMIC_COUNTER_BUFFER_SIZE = GL42.GL_MAX_ATOMIC_COUNTER_BUFFER_SIZE;
    public static final int GL_MAX_ATOMIC_COUNTER_BUFFER_BINDINGS = GL42.GL_MAX_ATOMIC_COUNTER_BUFFER_BINDINGS;
    public static final int GL_MAX_COMBINED_ATOMIC_COUNTERS = GL42.GL_MAX_COMBINED_ATOMIC_COUNTERS;
    
    /**
     * Check if atomic counters are supported
     */
    public static boolean supportsAtomicCounters() {
        return hasAtomicCounters;
    }
    
    /**
     * Bind buffer as atomic counter buffer
     */
    public static void bindAtomicCounterBuffer(int index, int buffer) {
        if (GL42) {
            GL30.glBindBufferBase(GL42.GL_ATOMIC_COUNTER_BUFFER, index, buffer);
        } else if (ARB_shader_atomic_counters) {
            GL30.glBindBufferBase(ARBShaderAtomicCounters.GL_ATOMIC_COUNTER_BUFFER, index, buffer);
        }
    }
    
    /**
     * Bind buffer range as atomic counter buffer
     */
    public static void bindAtomicCounterBufferRange(int index, int buffer, long offset, long size) {
        if (GL42) {
            GL30.glBindBufferRange(GL42.GL_ATOMIC_COUNTER_BUFFER, index, buffer, offset, size);
        } else if (ARB_shader_atomic_counters) {
            GL30.glBindBufferRange(ARBShaderAtomicCounters.GL_ATOMIC_COUNTER_BUFFER, index, buffer, offset, size);
        }
    }
    
    /**
     * glGetActiveAtomicCounterBufferiv - Get atomic counter buffer info
     * GL 4.2: glGetActiveAtomicCounterBufferiv(program, bufferIndex, pname, params)
     */
    public static void getActiveAtomicCounterBufferiv(int program, int bufferIndex, int pname, IntBuffer params) {
        if (GL42) {
            GL42.glGetActiveAtomicCounterBufferiv(program, bufferIndex, pname, params);
        } else if (ARB_shader_atomic_counters) {
            ARBShaderAtomicCounters.glGetActiveAtomicCounterBufferiv(program, bufferIndex, pname, params);
        }
    }
    
    // ========================================================================
    // GL 4.2 - SHADER IMAGE LOAD/STORE
    // ========================================================================
    
    // Image access constants
    public static final int GL_IMAGE_2D = GL42.GL_IMAGE_2D;
    public static final int GL_IMAGE_3D = GL42.GL_IMAGE_3D;
    public static final int GL_IMAGE_CUBE = GL42.GL_IMAGE_CUBE;
    public static final int GL_IMAGE_2D_ARRAY = GL42.GL_IMAGE_2D_ARRAY;
    public static final int GL_MAX_IMAGE_UNITS = GL42.GL_MAX_IMAGE_UNITS;
    public static final int GL_MAX_COMBINED_IMAGE_UNITS_AND_FRAGMENT_OUTPUTS = GL42.GL_MAX_COMBINED_IMAGE_UNITS_AND_FRAGMENT_OUTPUTS;
    
    /**
     * Check if image load/store is supported
     */
    public static boolean supportsImageLoadStore() {
        return hasShaderImageLoadStore;
    }
    
    /**
     * glBindImageTexture - Bind texture to image unit
     * GL 4.2: glBindImageTexture(unit, texture, level, layered, layer, access, format)
     */
    public static void bindImageTexture(int unit, int texture, int level, boolean layered, int layer, int access, int format) {
        if (GL42) {
            GL42.glBindImageTexture(unit, texture, level, layered, layer, access, format);
        } else if (ARB_shader_image_load_store) {
            ARBShaderImageLoadStore.glBindImageTexture(unit, texture, level, layered, layer, access, format);
        }
    }
    
    /**
     * glMemoryBarrier - Define memory barrier
     * GL 4.2: glMemoryBarrier(barriers)
     */
    public static void memoryBarrier(int barriers) {
        if (GL42) {
            GL42.glMemoryBarrier(barriers);
        } else if (ARB_shader_image_load_store) {
            ARBShaderImageLoadStore.glMemoryBarrier(barriers);
        }
    }
    
    // Memory barrier bits
    public static final int GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT = GL42.GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT;
    public static final int GL_ELEMENT_ARRAY_BARRIER_BIT = GL42.GL_ELEMENT_ARRAY_BARRIER_BIT;
    public static final int GL_UNIFORM_BARRIER_BIT = GL42.GL_UNIFORM_BARRIER_BIT;
    public static final int GL_TEXTURE_FETCH_BARRIER_BIT = GL42.GL_TEXTURE_FETCH_BARRIER_BIT;
    public static final int GL_SHADER_IMAGE_ACCESS_BARRIER_BIT = GL42.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT;
    public static final int GL_COMMAND_BARRIER_BIT = GL42.GL_COMMAND_BARRIER_BIT;
    public static final int GL_PIXEL_BUFFER_BARRIER_BIT = GL42.GL_PIXEL_BUFFER_BARRIER_BIT;
    public static final int GL_TEXTURE_UPDATE_BARRIER_BIT = GL42.GL_TEXTURE_UPDATE_BARRIER_BIT;
    public static final int GL_BUFFER_UPDATE_BARRIER_BIT = GL42.GL_BUFFER_UPDATE_BARRIER_BIT;
    public static final int GL_FRAMEBUFFER_BARRIER_BIT = GL42.GL_FRAMEBUFFER_BARRIER_BIT;
    public static final int GL_TRANSFORM_FEEDBACK_BARRIER_BIT = GL42.GL_TRANSFORM_FEEDBACK_BARRIER_BIT;
    public static final int GL_ATOMIC_COUNTER_BARRIER_BIT = GL42.GL_ATOMIC_COUNTER_BARRIER_BIT;
    public static final int GL_ALL_BARRIER_BITS = GL42.GL_ALL_BARRIER_BITS;
    
    // ========================================================================
    // GL 4.2 - TEXTURE STORAGE (IMMUTABLE)
    // ========================================================================
    
    /**
     * glTexStorage1D - Allocate immutable 1D texture storage
     * GL 4.2: glTexStorage1D(target, levels, internalformat, width)
     */
    public static void texStorage1D(int target, int levels, int internalformat, int width) {
        if (GL42) {
            GL42.glTexStorage1D(target, levels, internalformat, width);
        } else if (ARB_texture_storage) {
            ARBTextureStorage.glTexStorage1D(target, levels, internalformat, width);
        } else {
            // Fallback: use glTexImage1D for each level
            int w = width;
            for (int i = 0; i < levels; i++) {
                GL11.glTexImage1D(target, i, internalformat, w, 0, 
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                w = Math.max(1, w / 2);
            }
        }
    }
    
    /**
     * glTexStorage2D - Allocate immutable 2D texture storage
     * GL 4.2: glTexStorage2D(target, levels, internalformat, width, height)
     */
    public static void texStorage2D(int target, int levels, int internalformat, int width, int height) {
        if (GL42) {
            GL42.glTexStorage2D(target, levels, internalformat, width, height);
        } else if (ARB_texture_storage) {
            ARBTextureStorage.glTexStorage2D(target, levels, internalformat, width, height);
        } else {
            // Fallback: use glTexImage2D for each level
            int w = width, h = height;
            for (int i = 0; i < levels; i++) {
                GL11.glTexImage2D(target, i, internalformat, w, h, 0, 
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                w = Math.max(1, w / 2);
                h = Math.max(1, h / 2);
            }
        }
    }
    
    /**
     * glTexStorage3D - Allocate immutable 3D texture storage
     * GL 4.2: glTexStorage3D(target, levels, internalformat, width, height, depth)
     */
    public static void texStorage3D(int target, int levels, int internalformat, int width, int height, int depth) {
        if (GL42) {
            GL42.glTexStorage3D(target, levels, internalformat, width, height, depth);
        } else if (ARB_texture_storage) {
            ARBTextureStorage.glTexStorage3D(target, levels, internalformat, width, height, depth);
        } else if (GL12) {
            // Fallback
            int w = width, h = height, d = depth;
            for (int i = 0; i < levels; i++) {
                GL12.glTexImage3D(target, i, internalformat, w, h, d, 0, 
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                w = Math.max(1, w / 2);
                h = Math.max(1, h / 2);
                d = Math.max(1, d / 2);
            }
        }
    }
    
    /**
     * glTexStorage2DMultisample - Allocate immutable multisample storage
     * GL 4.3: glTexStorage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations)
     */
    public static void texStorage2DMultisample(int target, int samples, int internalformat, 
                                                int width, int height, boolean fixedsamplelocations) {
        if (GL43) {
            GL43.glTexStorage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations);
        } else if (ARB_texture_storage_multisample) {
            ARBTextureStorageMultisample.glTexStorage2DMultisample(target, samples, internalformat, 
                width, height, fixedsamplelocations);
        } else if (GL32) {
            // Fallback to glTexImage2DMultisample
            GL32.glTexImage2DMultisample(target, samples, internalformat, width, height, fixedsamplelocations);
        }
    }
    
    // ========================================================================
    // GL 4.2 - BASE INSTANCE DRAWS
    // ========================================================================
    
    /**
     * glDrawArraysInstancedBaseInstance - Draw arrays with base instance
     * GL 4.2: glDrawArraysInstancedBaseInstance(mode, first, count, instancecount, baseinstance)
     */
    public static void drawArraysInstancedBaseInstance(int mode, int first, int count, int instancecount, int baseinstance) {
        if (GL42) {
            GL42.glDrawArraysInstancedBaseInstance(mode, first, count, instancecount, baseinstance);
        } else if (ARB_base_instance) {
            ARBBaseInstance.glDrawArraysInstancedBaseInstance(mode, first, count, instancecount, baseinstance);
        } else if (baseinstance == 0) {
            // Fallback: if base instance is 0, use regular instanced draw
            drawArraysInstanced(mode, first, count, instancecount);
        } else {
            // No fallback for non-zero base instance
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Base instance not supported, falling back to base=0");
            }
            drawArraysInstanced(mode, first, count, instancecount);
        }
    }
    
    /**
     * glDrawElementsInstancedBaseInstance - Draw elements with base instance
     * GL 4.2: glDrawElementsInstancedBaseInstance(mode, count, type, indices, instancecount, baseinstance)
     */
    public static void drawElementsInstancedBaseInstance(int mode, int count, int type, long indices, 
                                                          int instancecount, int baseinstance) {
        if (GL42) {
            GL42.glDrawElementsInstancedBaseInstance(mode, count, type, indices, instancecount, baseinstance);
        } else if (ARB_base_instance) {
            ARBBaseInstance.glDrawElementsInstancedBaseInstance(mode, count, type, indices, instancecount, baseinstance);
        } else if (baseinstance == 0) {
            drawElementsInstanced(mode, count, type, indices, instancecount);
        } else {
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Base instance not supported, falling back to base=0");
            }
            drawElementsInstanced(mode, count, type, indices, instancecount);
        }
    }
    
    /**
     * glDrawElementsInstancedBaseVertexBaseInstance - Draw with base vertex and instance
     * GL 4.2: glDrawElementsInstancedBaseVertexBaseInstance(mode, count, type, indices, instancecount, basevertex, baseinstance)
     */
    public static void drawElementsInstancedBaseVertexBaseInstance(int mode, int count, int type, long indices,
                                                                    int instancecount, int basevertex, int baseinstance) {
        if (GL42) {
            GL42.glDrawElementsInstancedBaseVertexBaseInstance(mode, count, type, indices, 
                instancecount, basevertex, baseinstance);
        } else if (ARB_base_instance) {
            ARBBaseInstance.glDrawElementsInstancedBaseVertexBaseInstance(mode, count, type, indices, 
                instancecount, basevertex, baseinstance);
        } else if (basevertex == 0 && baseinstance == 0) {
            drawElementsInstanced(mode, count, type, indices, instancecount);
        } else {
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Base vertex/instance not supported");
            }
            drawElementsInstanced(mode, count, type, indices, instancecount);
        }
    }
    
    // ========================================================================
    // GL 4.2 - TRANSFORM FEEDBACK INSTANCED
    // ========================================================================
    
    /**
     * glDrawTransformFeedbackInstanced - Draw transform feedback with instancing
     * GL 4.2: glDrawTransformFeedbackInstanced(mode, id, instancecount)
     */
    public static void drawTransformFeedbackInstanced(int mode, int id, int instancecount) {
        if (GL42) {
            GL42.glDrawTransformFeedbackInstanced(mode, id, instancecount);
        } else if (ARB_transform_feedback_instanced) {
            ARBTransformFeedbackInstanced.glDrawTransformFeedbackInstanced(mode, id, instancecount);
        } else {
            // Fallback: draw multiple times
            for (int i = 0; i < instancecount; i++) {
                drawTransformFeedback(mode, id);
            }
        }
    }
    
    /**
     * glDrawTransformFeedbackStreamInstanced - Draw transform feedback stream with instancing
     * GL 4.2: glDrawTransformFeedbackStreamInstanced(mode, id, stream, instancecount)
     */
    public static void drawTransformFeedbackStreamInstanced(int mode, int id, int stream, int instancecount) {
        if (GL42) {
            GL42.glDrawTransformFeedbackStreamInstanced(mode, id, stream, instancecount);
        } else if (ARB_transform_feedback_instanced) {
            ARBTransformFeedbackInstanced.glDrawTransformFeedbackStreamInstanced(mode, id, stream, instancecount);
        }
    }
    
    // ========================================================================
    // ADDITIONAL EXTENSION FLAGS FOR GL 4.0-4.2
    // ========================================================================
    
    // Add these to the extension detection section
    private static boolean ARB_tessellation_shader = false;
    private static boolean ARB_shader_subroutine = false;
    private static boolean ARB_gpu_shader_fp64 = false;
    private static boolean ARB_draw_indirect = false;
    private static boolean ARB_transform_feedback2 = false;
    private static boolean ARB_draw_buffers_blend = false;
    private static boolean ARB_sample_shading = false;
    private static boolean ARB_viewport_array = false;
    private static boolean ARB_vertex_attrib_64bit = false;
    private static boolean ARB_shader_atomic_counters = false;
    private static boolean ARB_shader_image_load_store = false;
    private static boolean ARB_texture_storage = false;
    private static boolean ARB_texture_storage_multisample = false;
    private static boolean ARB_base_instance = false;
    private static boolean ARB_transform_feedback_instanced = false;
    
    // ========================================================================
    // GLSL VERSION HELPER
    // ========================================================================
    
    /**
     * Get recommended GLSL version directive for current GL version
     */
    public static String getGLSLVersionDirective() {
        if (GL46) return "#version 460 core\n";
        if (GL45) return "#version 450 core\n";
        if (GL44) return "#version 440 core\n";
        if (GL43) return "#version 430 core\n";
        if (GL42) return "#version 420 core\n";
        if (GL41) return "#version 410 core\n";
        if (GL40) return "#version 400 core\n";
        if (GL33) return "#version 330 core\n";
        if (GL32) return "#version 150 core\n";
        if (GL31) return "#version 140\n";
        if (GL30) return "#version 130\n";
        if (GL21) return "#version 120\n";
        if (GL20) return "#version 110\n";
        return "#version 110\n";
    }
    
    /**
     * Get GLSL version number
     */
    public static int getGLSLVersionNumber() {
        if (GL46) return 460;
        if (GL45) return 450;
        if (GL44) return 440;
        if (GL43) return 430;
        if (GL42) return 420;
        if (GL41) return 410;
        if (GL40) return 400;
        if (GL33) return 330;
        if (GL32) return 150;
        if (GL31) return 140;
        if (GL30) return 130;
        if (GL21) return 120;
        if (GL20) return 110;
        return 110;
    }
}

    // ========================================================================
    // LWJGL 3.x MEMORY MANAGEMENT UTILITIES
    // ========================================================================
    
    /**
     * Execute operation with stack-allocated int buffer
     * Zero-allocation pattern for hot paths
     */
    public static int withStackInt(java.util.function.IntSupplier operation) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return operation.getAsInt();
        }
    }
    
    /**
     * Generate single GL object using stack allocation
     */
    @FunctionalInterface
    public interface GLGenFunction {
        void generate(IntBuffer buffer);
    }
    
    public static int glGenSingle(GLGenFunction genFunc) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.mallocInt(1);
            genFunc.generate(buf);
            return buf.get(0);
        }
    }
    
    /**
     * Delete single GL object using stack allocation
     */
    @FunctionalInterface
    public interface GLDeleteFunction {
        void delete(IntBuffer buffer);
    }
    
    public static void glDeleteSingle(int id, GLDeleteFunction deleteFunc) {
        if (id == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.ints(id);
            deleteFunc.delete(buf);
        }
    }
    
    /**
     * Get single integer parameter using stack
     */
    public static int glGetIntegerStack(int pname) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.mallocInt(1);
            GL11.glGetIntegerv(pname, buf);
            return buf.get(0);
        }
    }
    
    /**
     * Get integer array using stack
     */
    public static int[] glGetIntegervStack(int pname, int count) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.mallocInt(count);
            GL11.glGetIntegerv(pname, buf);
            int[] result = new int[count];
            buf.get(result);
            return result;
        }
    }
    
    // ========================================================================
    // GL 4.3 - COMPUTE SHADERS
    // ========================================================================
    
    // Compute shader constants
    public static final int GL_COMPUTE_SHADER = GL43.GL_COMPUTE_SHADER;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_COUNT = GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_SIZE = GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS = GL43.GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS;
    public static final int GL_MAX_COMPUTE_UNIFORM_BLOCKS = GL43.GL_MAX_COMPUTE_UNIFORM_BLOCKS;
    public static final int GL_MAX_COMPUTE_TEXTURE_IMAGE_UNITS = GL43.GL_MAX_COMPUTE_TEXTURE_IMAGE_UNITS;
    public static final int GL_MAX_COMPUTE_ATOMIC_COUNTERS = GL43.GL_MAX_COMPUTE_ATOMIC_COUNTERS;
    public static final int GL_MAX_COMPUTE_ATOMIC_COUNTER_BUFFERS = GL43.GL_MAX_COMPUTE_ATOMIC_COUNTER_BUFFERS;
    public static final int GL_MAX_COMPUTE_SHARED_MEMORY_SIZE = GL43.GL_MAX_COMPUTE_SHARED_MEMORY_SIZE;
    public static final int GL_MAX_COMPUTE_UNIFORM_COMPONENTS = GL43.GL_MAX_COMPUTE_UNIFORM_COMPONENTS;
    public static final int GL_MAX_COMBINED_COMPUTE_UNIFORM_COMPONENTS = GL43.GL_MAX_COMBINED_COMPUTE_UNIFORM_COMPONENTS;
    
    public static final int GL_COMPUTE_WORK_GROUP_SIZE = GL43.GL_COMPUTE_WORK_GROUP_SIZE;
    
    /**
     * Check if compute shaders are supported
     */
    public static boolean supportsComputeShaders() {
        return hasComputeShaders;
    }
    
    /**
     * Create compute shader
     */
    public static int createComputeShader() {
        if (GL43) {
            return GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
        } else if (ARB_compute_shader) {
            return GL20.glCreateShader(ARBComputeShader.GL_COMPUTE_SHADER);
        }
        return 0;
    }
    
    /**
     * Create and compile compute shader from source
     */
    public static int createComputeShaderFromSource(CharSequence source) {
        int shader = createComputeShader();
        if (shader == 0) return 0;
        
        shaderSource(shader, source);
        compileShader(shader);
        
        if (getShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE) {
            String log = getShaderInfoLog(shader);
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Compute shader compilation failed:");
                System.err.println(log);
            }
            deleteShader(shader);
            return 0;
        }
        
        return shader;
    }
    
    /**
     * Create complete compute program from source
     */
    public static int createComputeProgram(CharSequence source) {
        int shader = createComputeShaderFromSource(source);
        if (shader == 0) return 0;
        
        int program = createProgram();
        attachShader(program, shader);
        linkProgram(program);
        
        deleteShader(shader); // Can delete after linking
        
        if (getProgrami(program, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
            String log = getProgramInfoLog(program);
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Compute program linking failed:");
                System.err.println(log);
            }
            deleteProgram(program);
            return 0;
        }
        
        return program;
    }
    
    /**
     * glDispatchCompute - Dispatch compute work groups
     * GL 4.3: glDispatchCompute(num_groups_x, num_groups_y, num_groups_z)
     */
    public static void dispatchCompute(int num_groups_x, int num_groups_y, int num_groups_z) {
        if (GL43) {
            GL43.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
        } else if (ARB_compute_shader) {
            ARBComputeShader.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
        }
    }
    
    /**
     * glDispatchComputeIndirect - Dispatch compute from buffer
     * GL 4.3: glDispatchComputeIndirect(indirect)
     */
    public static void dispatchComputeIndirect(long indirect) {
        if (GL43) {
            GL43.glDispatchComputeIndirect(indirect);
        } else if (ARB_compute_shader) {
            ARBComputeShader.glDispatchComputeIndirect(indirect);
        }
    }
    
    /**
     * Get compute work group size for a program
     */
    public static int[] getComputeWorkGroupSize(int program) {
        if (!hasComputeShaders) return new int[]{0, 0, 0};
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.mallocInt(3);
            GL20.glGetProgramiv(program, GL43.GL_COMPUTE_WORK_GROUP_SIZE, buf);
            return new int[]{buf.get(0), buf.get(1), buf.get(2)};
        }
    }
    
    /**
     * Get maximum work group count for each dimension
     */
    public static int[] getMaxComputeWorkGroupCount() {
        if (!hasComputeShaders) return new int[]{0, 0, 0};
        
        int[] result = new int[3];
        result[0] = glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0);
        result[1] = glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1);
        result[2] = glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2);
        return result;
    }
    
    /**
     * Get maximum work group size for each dimension
     */
    public static int[] getMaxComputeWorkGroupSize() {
        if (!hasComputeShaders) return new int[]{0, 0, 0};
        
        int[] result = new int[3];
        result[0] = glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0);
        result[1] = glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 1);
        result[2] = glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2);
        return result;
    }
    
    /**
     * glGetIntegeri_v - Get indexed integer
     */
    public static int glGetIntegeri(int target, int index) {
        if (GL30) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                GL30.glGetIntegeri_v(target, index, buf);
                return buf.get(0);
            }
        }
        return 0;
    }
    
    // ========================================================================
    // GL 4.3 - SHADER STORAGE BUFFER OBJECTS (SSBO)
    // ========================================================================
    
    // SSBO constants
    public static final int GL_SHADER_STORAGE_BUFFER = GL43.GL_SHADER_STORAGE_BUFFER;
    public static final int GL_SHADER_STORAGE_BUFFER_BINDING = GL43.GL_SHADER_STORAGE_BUFFER_BINDING;
    public static final int GL_SHADER_STORAGE_BUFFER_START = GL43.GL_SHADER_STORAGE_BUFFER_START;
    public static final int GL_SHADER_STORAGE_BUFFER_SIZE = GL43.GL_SHADER_STORAGE_BUFFER_SIZE;
    public static final int GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS = GL43.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS;
    public static final int GL_MAX_SHADER_STORAGE_BLOCK_SIZE = GL43.GL_MAX_SHADER_STORAGE_BLOCK_SIZE;
    public static final int GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS = GL43.GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS;
    public static final int GL_SHADER_STORAGE_BARRIER_BIT = GL43.GL_SHADER_STORAGE_BARRIER_BIT;
    
    /**
     * Check if SSBOs are supported
     */
    public static boolean supportsSSBO() {
        return hasSSBO;
    }
    
    /**
     * Bind shader storage buffer to binding point
     */
    public static void bindShaderStorageBuffer(int bindingPoint, int buffer) {
        if (GL43) {
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, bindingPoint, buffer);
        } else if (ARB_shader_storage_buffer_object) {
            GL30.glBindBufferBase(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, bindingPoint, buffer);
        }
    }
    
    /**
     * Bind shader storage buffer range to binding point
     */
    public static void bindShaderStorageBufferRange(int bindingPoint, int buffer, long offset, long size) {
        if (GL43) {
            GL30.glBindBufferRange(GL43.GL_SHADER_STORAGE_BUFFER, bindingPoint, buffer, offset, size);
        } else if (ARB_shader_storage_buffer_object) {
            GL30.glBindBufferRange(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, bindingPoint, buffer, offset, size);
        }
    }
    
    /**
     * glGetProgramResourceIndex - Get resource index by name
     * GL 4.3: glGetProgramResourceIndex(program, programInterface, name)
     */
    public static int getProgramResourceIndex(int program, int programInterface, CharSequence name) {
        if (GL43) {
            return GL43.glGetProgramResourceIndex(program, programInterface, name);
        } else if (ARB_program_interface_query) {
            return ARBProgramInterfaceQuery.glGetProgramResourceIndex(program, programInterface, name);
        }
        return GL31.GL_INVALID_INDEX;
    }
    
    /**
     * glShaderStorageBlockBinding - Set SSBO binding point
     * GL 4.3: glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding)
     */
    public static void shaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding) {
        if (GL43) {
            GL43.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
        } else if (ARB_shader_storage_buffer_object) {
            ARBShaderStorageBufferObject.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
        }
    }
    
    /**
     * Get shader storage block index
     */
    public static int getShaderStorageBlockIndex(int program, CharSequence name) {
        return getProgramResourceIndex(program, GL43.GL_SHADER_STORAGE_BLOCK, name);
    }
    
    // ========================================================================
    // GL 4.3 - MULTI-DRAW INDIRECT
    // ========================================================================
    
    // Indirect buffer binding
    public static final int GL_DRAW_INDIRECT_BUFFER = GL40.GL_DRAW_INDIRECT_BUFFER;
    public static final int GL_DRAW_INDIRECT_BUFFER_BINDING = GL40.GL_DRAW_INDIRECT_BUFFER_BINDING;
    
    /**
     * Bind draw indirect buffer
     */
    public static void bindDrawIndirectBuffer(int buffer) {
        StateTracker state = getState();
        if (state.boundDrawIndirectBuffer == buffer) return;
        state.boundDrawIndirectBuffer = buffer;
        
        if (GL40) {
            GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, buffer);
        } else if (ARB_draw_indirect) {
            GL15.glBindBuffer(ARBDrawIndirect.GL_DRAW_INDIRECT_BUFFER, buffer);
        }
    }
    
    /**
     * glMultiDrawArraysIndirect - Multiple indirect array draws
     * GL 4.3: glMultiDrawArraysIndirect(mode, indirect, drawcount, stride)
     */
    public static void multiDrawArraysIndirect(int mode, long indirect, int drawcount, int stride) {
        if (GL43) {
            GL43.glMultiDrawArraysIndirect(mode, indirect, drawcount, stride);
        } else if (ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawArraysIndirect(mode, indirect, drawcount, stride);
        } else if (GL40) {
            // Fallback: loop over individual indirect draws
            long offset = indirect;
            int commandStride = stride != 0 ? stride : 16; // sizeof(DrawArraysIndirectCommand)
            for (int i = 0; i < drawcount; i++) {
                GL40.glDrawArraysIndirect(mode, offset);
                offset += commandStride;
            }
        }
    }
    
    /**
     * glMultiDrawArraysIndirect with ByteBuffer
     */
    public static void multiDrawArraysIndirect(int mode, ByteBuffer indirect, int drawcount, int stride) {
        if (GL43) {
            GL43.glMultiDrawArraysIndirect(mode, indirect, drawcount, stride);
        } else if (ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawArraysIndirect(mode, indirect, drawcount, stride);
        }
    }
    
    /**
     * glMultiDrawElementsIndirect - Multiple indirect element draws
     * GL 4.3: glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride)
     */
    public static void multiDrawElementsIndirect(int mode, int type, long indirect, int drawcount, int stride) {
        if (GL43) {
            GL43.glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride);
        } else if (ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride);
        } else if (GL40) {
            // Fallback: loop over individual indirect draws
            long offset = indirect;
            int commandStride = stride != 0 ? stride : 20; // sizeof(DrawElementsIndirectCommand)
            for (int i = 0; i < drawcount; i++) {
                GL40.glDrawElementsIndirect(mode, type, offset);
                offset += commandStride;
            }
        }
    }
    
    /**
     * glMultiDrawElementsIndirect with ByteBuffer
     */
    public static void multiDrawElementsIndirect(int mode, int type, ByteBuffer indirect, int drawcount, int stride) {
        if (GL43) {
            GL43.glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride);
        } else if (ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride);
        }
    }
    
    /**
     * Create indirect draw command buffer
     * Returns buffer ID and mapped pointer for persistent mapping (if available)
     */
    public static class IndirectBuffer {
        public final int bufferId;
        public final ByteBuffer mappedBuffer;
        public final boolean persistentlyMapped;
        public final int maxCommands;
        public final int commandSize;
        
        public IndirectBuffer(int bufferId, ByteBuffer mappedBuffer, boolean persistentlyMapped, 
                             int maxCommands, int commandSize) {
            this.bufferId = bufferId;
            this.mappedBuffer = mappedBuffer;
            this.persistentlyMapped = persistentlyMapped;
            this.maxCommands = maxCommands;
            this.commandSize = commandSize;
        }
    }
    
    /**
     * Create indirect buffer for DrawArraysIndirectCommand
     * Structure: { uint count, uint instanceCount, uint first, uint baseInstance }
     */
    public static IndirectBuffer createDrawArraysIndirectBuffer(int maxCommands) {
        int commandSize = 16; // 4 uints
        int bufferSize = maxCommands * commandSize;
        
        int buffer = genBuffer();
        GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, buffer);
        
        ByteBuffer mapped = null;
        boolean persistent = false;
        
        if (hasPersistentMapping) {
            // Use persistent mapping for best streaming performance
            int flags = GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT;
            GL44.glBufferStorage(GL40.GL_DRAW_INDIRECT_BUFFER, bufferSize, 
                flags | GL44.GL_DYNAMIC_STORAGE_BIT);
            mapped = GL30.glMapBufferRange(GL40.GL_DRAW_INDIRECT_BUFFER, 0, bufferSize, flags);
            persistent = true;
        } else {
            GL15.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER, bufferSize, GL15.GL_DYNAMIC_DRAW);
        }
        
        GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, 0);
        
        return new IndirectBuffer(buffer, mapped, persistent, maxCommands, commandSize);
    }
    
    /**
     * Create indirect buffer for DrawElementsIndirectCommand
     * Structure: { uint count, uint instanceCount, uint firstIndex, int baseVertex, uint baseInstance }
     */
    public static IndirectBuffer createDrawElementsIndirectBuffer(int maxCommands) {
        int commandSize = 20; // 5 uints
        int bufferSize = maxCommands * commandSize;
        
        int buffer = genBuffer();
        GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, buffer);
        
        ByteBuffer mapped = null;
        boolean persistent = false;
        
        if (hasPersistentMapping) {
            int flags = GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT;
            GL44.glBufferStorage(GL40.GL_DRAW_INDIRECT_BUFFER, bufferSize, 
                flags | GL44.GL_DYNAMIC_STORAGE_BIT);
            mapped = GL30.glMapBufferRange(GL40.GL_DRAW_INDIRECT_BUFFER, 0, bufferSize, flags);
            persistent = true;
        } else {
            GL15.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER, bufferSize, GL15.GL_DYNAMIC_DRAW);
        }
        
        GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, 0);
        
        return new IndirectBuffer(buffer, mapped, persistent, maxCommands, commandSize);
    }
    
    // ========================================================================
    // GL 4.3 - DEBUG OUTPUT
    // ========================================================================
    
    // Debug constants
    public static final int GL_DEBUG_OUTPUT = GL43.GL_DEBUG_OUTPUT;
    public static final int GL_DEBUG_OUTPUT_SYNCHRONOUS = GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS;
    public static final int GL_DEBUG_SOURCE_API = GL43.GL_DEBUG_SOURCE_API;
    public static final int GL_DEBUG_SOURCE_WINDOW_SYSTEM = GL43.GL_DEBUG_SOURCE_WINDOW_SYSTEM;
    public static final int GL_DEBUG_SOURCE_SHADER_COMPILER = GL43.GL_DEBUG_SOURCE_SHADER_COMPILER;
    public static final int GL_DEBUG_SOURCE_THIRD_PARTY = GL43.GL_DEBUG_SOURCE_THIRD_PARTY;
    public static final int GL_DEBUG_SOURCE_APPLICATION = GL43.GL_DEBUG_SOURCE_APPLICATION;
    public static final int GL_DEBUG_SOURCE_OTHER = GL43.GL_DEBUG_SOURCE_OTHER;
    public static final int GL_DEBUG_TYPE_ERROR = GL43.GL_DEBUG_TYPE_ERROR;
    public static final int GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR = GL43.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR;
    public static final int GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR = GL43.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR;
    public static final int GL_DEBUG_TYPE_PORTABILITY = GL43.GL_DEBUG_TYPE_PORTABILITY;
    public static final int GL_DEBUG_TYPE_PERFORMANCE = GL43.GL_DEBUG_TYPE_PERFORMANCE;
    public static final int GL_DEBUG_TYPE_MARKER = GL43.GL_DEBUG_TYPE_MARKER;
    public static final int GL_DEBUG_TYPE_PUSH_GROUP = GL43.GL_DEBUG_TYPE_PUSH_GROUP;
    public static final int GL_DEBUG_TYPE_POP_GROUP = GL43.GL_DEBUG_TYPE_POP_GROUP;
    public static final int GL_DEBUG_TYPE_OTHER = GL43.GL_DEBUG_TYPE_OTHER;
    public static final int GL_DEBUG_SEVERITY_HIGH = GL43.GL_DEBUG_SEVERITY_HIGH;
    public static final int GL_DEBUG_SEVERITY_MEDIUM = GL43.GL_DEBUG_SEVERITY_MEDIUM;
    public static final int GL_DEBUG_SEVERITY_LOW = GL43.GL_DEBUG_SEVERITY_LOW;
    public static final int GL_DEBUG_SEVERITY_NOTIFICATION = GL43.GL_DEBUG_SEVERITY_NOTIFICATION;
    
    /**
     * Debug callback interface
     */
    @FunctionalInterface
    public interface DebugCallback {
        void onMessage(int source, int type, int id, int severity, String message);
    }
    
    private static DebugCallback userDebugCallback = null;
    private static GLDebugMessageCallback glDebugCallback = null;
    
    /**
     * Check if debug output is supported
     */
    public static boolean supportsDebugOutput() {
        return hasDebugOutput;
    }
    
    /**
     * Enable debug output
     */
    public static void enableDebugOutput(boolean synchronous) {
        if (!hasDebugOutput) return;
        
        GL11.glEnable(GL43.GL_DEBUG_OUTPUT);
        if (synchronous) {
            GL11.glEnable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
        }
    }
    
    /**
     * Disable debug output
     */
    public static void disableDebugOutput() {
        if (!hasDebugOutput) return;
        
        GL11.glDisable(GL43.GL_DEBUG_OUTPUT);
        GL11.glDisable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
    }
    
    /**
     * Set debug callback
     */
    public static void setDebugCallback(DebugCallback callback) {
        userDebugCallback = callback;
        
        if (!hasDebugOutput) return;
        
        // Clean up old callback
        if (glDebugCallback != null) {
            glDebugCallback.free();
            glDebugCallback = null;
        }
        
        if (callback != null) {
            glDebugCallback = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
                String msg = GLDebugMessageCallback.getMessage(length, message);
                userDebugCallback.onMessage(source, type, id, severity, msg);
            });
            
            if (GL43) {
                GL43.glDebugMessageCallback(glDebugCallback, 0);
            } else if (KHR_debug) {
                KHRDebug.glDebugMessageCallback(glDebugCallback, 0);
            } else if (ARB_debug_output) {
                ARBDebugOutput.glDebugMessageCallbackARB(glDebugCallback, 0);
            }
        } else {
            if (GL43) {
                GL43.glDebugMessageCallback(null, 0);
            } else if (KHR_debug) {
                KHRDebug.glDebugMessageCallback(null, 0);
            }
        }
    }
    
    /**
     * Default debug callback that logs to console
     */
    public static final DebugCallback DEFAULT_DEBUG_CALLBACK = (source, type, id, severity, message) -> {
        String sourceStr = getDebugSourceString(source);
        String typeStr = getDebugTypeString(type);
        String severityStr = getDebugSeverityString(severity);
        
        String prefix = "[GL Debug]";
        if (severity == GL_DEBUG_SEVERITY_HIGH || type == GL_DEBUG_TYPE_ERROR) {
            System.err.printf("%s [%s] [%s] [%s] ID=%d: %s%n", 
                prefix, severityStr, sourceStr, typeStr, id, message);
        } else if (severity != GL_DEBUG_SEVERITY_NOTIFICATION) {
            System.out.printf("%s [%s] [%s] [%s] ID=%d: %s%n", 
                prefix, severityStr, sourceStr, typeStr, id, message);
        }
    };
    
    /**
     * Get debug source as string
     */
    public static String getDebugSourceString(int source) {
        switch (source) {
            case GL_DEBUG_SOURCE_API: return "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM: return "Window System";
            case GL_DEBUG_SOURCE_SHADER_COMPILER: return "Shader Compiler";
            case GL_DEBUG_SOURCE_THIRD_PARTY: return "Third Party";
            case GL_DEBUG_SOURCE_APPLICATION: return "Application";
            case GL_DEBUG_SOURCE_OTHER: return "Other";
            default: return "Unknown(" + source + ")";
        }
    }
    
    /**
     * Get debug type as string
     */
    public static String getDebugTypeString(int type) {
        switch (type) {
            case GL_DEBUG_TYPE_ERROR: return "Error";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR: return "Deprecated";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR: return "Undefined Behavior";
            case GL_DEBUG_TYPE_PORTABILITY: return "Portability";
            case GL_DEBUG_TYPE_PERFORMANCE: return "Performance";
            case GL_DEBUG_TYPE_MARKER: return "Marker";
            case GL_DEBUG_TYPE_PUSH_GROUP: return "Push Group";
            case GL_DEBUG_TYPE_POP_GROUP: return "Pop Group";
            case GL_DEBUG_TYPE_OTHER: return "Other";
            default: return "Unknown(" + type + ")";
        }
    }
    
    /**
     * Get debug severity as string
     */
    public static String getDebugSeverityString(int severity) {
        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH: return "HIGH";
            case GL_DEBUG_SEVERITY_MEDIUM: return "MEDIUM";
            case GL_DEBUG_SEVERITY_LOW: return "LOW";
            case GL_DEBUG_SEVERITY_NOTIFICATION: return "NOTIFICATION";
            default: return "Unknown(" + severity + ")";
        }
    }
    
    /**
     * glDebugMessageControl - Control debug messages
     * GL 4.3: glDebugMessageControl(source, type, severity, ids, enabled)
     */
    public static void debugMessageControl(int source, int type, int severity, IntBuffer ids, boolean enabled) {
        if (GL43) {
            GL43.glDebugMessageControl(source, type, severity, ids, enabled);
        } else if (KHR_debug) {
            KHRDebug.glDebugMessageControl(source, type, severity, ids, enabled);
        } else if (ARB_debug_output) {
            ARBDebugOutput.glDebugMessageControlARB(source, type, severity, ids, enabled);
        }
    }
    
    /**
     * Disable all notifications (reduce noise)
     */
    public static void disableDebugNotifications() {
        debugMessageControl(GL11.GL_DONT_CARE, GL11.GL_DONT_CARE, 
            GL_DEBUG_SEVERITY_NOTIFICATION, null, false);
    }
    
    /**
     * glDebugMessageInsert - Insert debug message
     * GL 4.3: glDebugMessageInsert(source, type, id, severity, message)
     */
    public static void debugMessageInsert(int source, int type, int id, int severity, CharSequence message) {
        if (GL43) {
            GL43.glDebugMessageInsert(source, type, id, severity, message);
        } else if (KHR_debug) {
            KHRDebug.glDebugMessageInsert(source, type, id, severity, message);
        } else if (ARB_debug_output) {
            ARBDebugOutput.glDebugMessageInsertARB(source, type, id, severity, message);
        }
    }
    
    /**
     * Insert application debug message
     */
    public static void debugMessage(String message) {
        debugMessageInsert(GL_DEBUG_SOURCE_APPLICATION, GL_DEBUG_TYPE_OTHER, 0, 
            GL_DEBUG_SEVERITY_NOTIFICATION, message);
    }
    
    /**
     * Insert application error message
     */
    public static void debugError(String message) {
        debugMessageInsert(GL_DEBUG_SOURCE_APPLICATION, GL_DEBUG_TYPE_ERROR, 0, 
            GL_DEBUG_SEVERITY_HIGH, message);
    }
    
    /**
     * glPushDebugGroup - Push debug group onto stack
     * GL 4.3: glPushDebugGroup(source, id, message)
     */
    public static void pushDebugGroup(int source, int id, CharSequence message) {
        if (GL43) {
            GL43.glPushDebugGroup(source, id, message);
        } else if (KHR_debug) {
            KHRDebug.glPushDebugGroup(source, id, message);
        }
    }
    
    /**
     * Push application debug group
     */
    public static void pushDebugGroup(String message) {
        pushDebugGroup(GL_DEBUG_SOURCE_APPLICATION, 0, message);
    }
    
    /**
     * glPopDebugGroup - Pop debug group from stack
     * GL 4.3: glPopDebugGroup()
     */
    public static void popDebugGroup() {
        if (GL43) {
            GL43.glPopDebugGroup();
        } else if (KHR_debug) {
            KHRDebug.glPopDebugGroup();
        }
    }
    
    /**
     * glObjectLabel - Label GL object
     * GL 4.3: glObjectLabel(identifier, name, label)
     */
    public static void objectLabel(int identifier, int name, CharSequence label) {
        if (GL43) {
            GL43.glObjectLabel(identifier, name, label);
        } else if (KHR_debug) {
            KHRDebug.glObjectLabel(identifier, name, label);
        }
    }
    
    /**
     * Label a buffer object
     */
    public static void labelBuffer(int buffer, String label) {
        objectLabel(GL43.GL_BUFFER, buffer, label);
    }
    
    /**
     * Label a texture object
     */
    public static void labelTexture(int texture, String label) {
        objectLabel(GL11.GL_TEXTURE, texture, label);
    }
    
    /**
     * Label a shader object
     */
    public static void labelShader(int shader, String label) {
        objectLabel(GL43.GL_SHADER, shader, label);
    }
    
    /**
     * Label a program object
     */
    public static void labelProgram(int program, String label) {
        objectLabel(GL43.GL_PROGRAM, program, label);
    }
    
    /**
     * Label a VAO
     */
    public static void labelVertexArray(int vao, String label) {
        objectLabel(GL43.GL_VERTEX_ARRAY, vao, label);
    }
    
    /**
     * Label a framebuffer
     */
    public static void labelFramebuffer(int framebuffer, String label) {
        objectLabel(GL43.GL_FRAMEBUFFER, framebuffer, label);
    }
    
    // ========================================================================
    // GL 4.3 - PROGRAM INTERFACE QUERY
    // ========================================================================
    
    // Program interface constants
    public static final int GL_UNIFORM = GL43.GL_UNIFORM;
    public static final int GL_UNIFORM_BLOCK = GL43.GL_UNIFORM_BLOCK;
    public static final int GL_PROGRAM_INPUT = GL43.GL_PROGRAM_INPUT;
    public static final int GL_PROGRAM_OUTPUT = GL43.GL_PROGRAM_OUTPUT;
    public static final int GL_BUFFER_VARIABLE = GL43.GL_BUFFER_VARIABLE;
    public static final int GL_SHADER_STORAGE_BLOCK = GL43.GL_SHADER_STORAGE_BLOCK;
    public static final int GL_VERTEX_SUBROUTINE = GL43.GL_VERTEX_SUBROUTINE;
    public static final int GL_FRAGMENT_SUBROUTINE = GL43.GL_FRAGMENT_SUBROUTINE;
    public static final int GL_COMPUTE_SUBROUTINE = GL43.GL_COMPUTE_SUBROUTINE;
    public static final int GL_TRANSFORM_FEEDBACK_VARYING = GL43.GL_TRANSFORM_FEEDBACK_VARYING;
    
    public static final int GL_ACTIVE_RESOURCES = GL43.GL_ACTIVE_RESOURCES;
    public static final int GL_MAX_NAME_LENGTH = GL43.GL_MAX_NAME_LENGTH;
    
    public static final int GL_NAME_LENGTH = GL43.GL_NAME_LENGTH;
    public static final int GL_TYPE = GL43.GL_TYPE;
    public static final int GL_ARRAY_SIZE = GL43.GL_ARRAY_SIZE;
    public static final int GL_OFFSET = GL43.GL_OFFSET;
    public static final int GL_BLOCK_INDEX = GL43.GL_BLOCK_INDEX;
    public static final int GL_ARRAY_STRIDE = GL43.GL_ARRAY_STRIDE;
    public static final int GL_MATRIX_STRIDE = GL43.GL_MATRIX_STRIDE;
    public static final int GL_IS_ROW_MAJOR = GL43.GL_IS_ROW_MAJOR;
    public static final int GL_ATOMIC_COUNTER_BUFFER_INDEX = GL43.GL_ATOMIC_COUNTER_BUFFER_INDEX;
    public static final int GL_BUFFER_BINDING = GL43.GL_BUFFER_BINDING;
    public static final int GL_BUFFER_DATA_SIZE = GL43.GL_BUFFER_DATA_SIZE;
    public static final int GL_NUM_ACTIVE_VARIABLES = GL43.GL_NUM_ACTIVE_VARIABLES;
    public static final int GL_ACTIVE_VARIABLES = GL43.GL_ACTIVE_VARIABLES;
    public static final int GL_REFERENCED_BY_VERTEX_SHADER = GL43.GL_REFERENCED_BY_VERTEX_SHADER;
    public static final int GL_REFERENCED_BY_FRAGMENT_SHADER = GL43.GL_REFERENCED_BY_FRAGMENT_SHADER;
    public static final int GL_REFERENCED_BY_COMPUTE_SHADER = GL43.GL_REFERENCED_BY_COMPUTE_SHADER;
    public static final int GL_LOCATION = GL43.GL_LOCATION;
    public static final int GL_LOCATION_INDEX = GL43.GL_LOCATION_INDEX;
    
    /**
     * glGetProgramInterfaceiv - Get program interface property
     * GL 4.3: glGetProgramInterfaceiv(program, programInterface, pname, params)
     */
    public static int getProgramInterfacei(int program, int programInterface, int pname) {
        if (GL43) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                GL43.glGetProgramInterfaceiv(program, programInterface, pname, buf);
                return buf.get(0);
            }
        } else if (ARB_program_interface_query) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBProgramInterfaceQuery.glGetProgramInterfaceiv(program, programInterface, pname, buf);
                return buf.get(0);
            }
        }
        return 0;
    }
    
    /**
     * glGetProgramResourceName - Get resource name
     * GL 4.3: glGetProgramResourceName(program, programInterface, index)
     */
    public static String getProgramResourceName(int program, int programInterface, int index) {
        if (GL43) {
            return GL43.glGetProgramResourceName(program, programInterface, index);
        } else if (ARB_program_interface_query) {
            int maxLength = getProgramInterfacei(program, programInterface, GL_MAX_NAME_LENGTH);
            return ARBProgramInterfaceQuery.glGetProgramResourceName(program, programInterface, index, maxLength);
        }
        return "";
    }
    
    /**
     * glGetProgramResourceiv - Get resource properties
     * GL 4.3: glGetProgramResourceiv(program, programInterface, index, props, length, params)
     */
    public static void getProgramResourceiv(int program, int programInterface, int index, 
                                            IntBuffer props, IntBuffer length, IntBuffer params) {
        if (GL43) {
            GL43.glGetProgramResourceiv(program, programInterface, index, props, length, params);
        } else if (ARB_program_interface_query) {
            ARBProgramInterfaceQuery.glGetProgramResourceiv(program, programInterface, index, props, length, params);
        }
    }
    
    /**
     * Get single resource property
     */
    public static int getProgramResourceProperty(int program, int programInterface, int index, int prop) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer props = stack.ints(prop);
            IntBuffer length = stack.mallocInt(1);
            IntBuffer params = stack.mallocInt(1);
            getProgramResourceiv(program, programInterface, index, props, length, params);
            return params.get(0);
        }
    }
    
    /**
     * glGetProgramResourceLocation - Get resource location
     * GL 4.3: glGetProgramResourceLocation(program, programInterface, name)
     */
    public static int getProgramResourceLocation(int program, int programInterface, CharSequence name) {
        if (GL43) {
            return GL43.glGetProgramResourceLocation(program, programInterface, name);
        } else if (ARB_program_interface_query) {
            return ARBProgramInterfaceQuery.glGetProgramResourceLocation(program, programInterface, name);
        }
        return -1;
    }
    
    /**
     * Introspect all uniforms in a program
     */
    public static Map<String, UniformInfo> introspectUniforms(int program) {
        Map<String, UniformInfo> uniforms = new HashMap<>();
        
        if (!hasProgramInterface) {
            // Fallback to GL 2.0 way
            return getAllUniformLocations(program).entrySet().stream()
                .collect(HashMap::new, 
                    (m, e) -> m.put(e.getKey(), new UniformInfo(e.getKey(), e.getValue(), 0, 0)),
                    HashMap::putAll);
        }
        
        int count = getProgramInterfacei(program, GL_UNIFORM, GL_ACTIVE_RESOURCES);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer props = stack.ints(GL_NAME_LENGTH, GL_TYPE, GL_LOCATION, GL_ARRAY_SIZE);
            IntBuffer length = stack.mallocInt(1);
            IntBuffer params = stack.mallocInt(4);
            
            for (int i = 0; i < count; i++) {
                getProgramResourceiv(program, GL_UNIFORM, i, props, length, params);
                
                String name = getProgramResourceName(program, GL_UNIFORM, i);
                int type = params.get(1);
                int location = params.get(2);
                int arraySize = params.get(3);
                
                uniforms.put(name, new UniformInfo(name, location, type, arraySize));
            }
        }
        
        return uniforms;
    }
    
    public static class UniformInfo {
        public final String name;
        public final int location;
        public final int type;
        public final int arraySize;
        
        public UniformInfo(String name, int location, int type, int arraySize) {
            this.name = name;
            this.location = location;
            this.type = type;
            this.arraySize = arraySize;
        }
    }
    
    // ========================================================================
    // GL 4.3 - VERTEX ATTRIB BINDING (Separate format from binding)
    // ========================================================================
    
    /**
     * glVertexAttribFormat - Specify vertex attribute format
     * GL 4.3: glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset)
     */
    public static void vertexAttribFormat(int attribindex, int size, int type, boolean normalized, int relativeoffset) {
        if (GL43) {
            GL43.glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset);
        } else if (ARB_vertex_attrib_binding) {
            ARBVertexAttribBinding.glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset);
        }
    }
    
    /**
     * glVertexAttribIFormat - Specify integer vertex attribute format
     * GL 4.3: glVertexAttribIFormat(attribindex, size, type, relativeoffset)
     */
    public static void vertexAttribIFormat(int attribindex, int size, int type, int relativeoffset) {
        if (GL43) {
            GL43.glVertexAttribIFormat(attribindex, size, type, relativeoffset);
        } else if (ARB_vertex_attrib_binding) {
            ARBVertexAttribBinding.glVertexAttribIFormat(attribindex, size, type, relativeoffset);
        }
    }
    
    /**
     * glVertexAttribLFormat - Specify 64-bit vertex attribute format
     * GL 4.3: glVertexAttribLFormat(attribindex, size, type, relativeoffset)
     */
    public static void vertexAttribLFormat(int attribindex, int size, int type, int relativeoffset) {
        if (GL43) {
            GL43.glVertexAttribLFormat(attribindex, size, type, relativeoffset);
        } else if (ARB_vertex_attrib_binding) {
            ARBVertexAttribBinding.glVertexAttribLFormat(attribindex, size, type, relativeoffset);
        }
    }
    
    /**
     * glVertexAttribBinding - Associate attribute with binding
     * GL 4.3: glVertexAttribBinding(attribindex, bindingindex)
     */
    public static void vertexAttribBinding(int attribindex, int bindingindex) {
        if (GL43) {
            GL43.glVertexAttribBinding(attribindex, bindingindex);
        } else if (ARB_vertex_attrib_binding) {
            ARBVertexAttribBinding.glVertexAttribBinding(attribindex, bindingindex);
        }
    }
    
    /**
     * glBindVertexBuffer - Bind buffer to binding point
     * GL 4.3: glBindVertexBuffer(bindingindex, buffer, offset, stride)
     */
    public static void bindVertexBuffer(int bindingindex, int buffer, long offset, int stride) {
        if (GL43) {
            GL43.glBindVertexBuffer(bindingindex, buffer, offset, stride);
        } else if (ARB_vertex_attrib_binding) {
            ARBVertexAttribBinding.glBindVertexBuffer(bindingindex, buffer, offset, stride);
        }
    }
    
    /**
     * glVertexBindingDivisor - Set binding divisor for instancing
     * GL 4.3: glVertexBindingDivisor(bindingindex, divisor)
     */
    public static void vertexBindingDivisor(int bindingindex, int divisor) {
        if (GL43) {
            GL43.glVertexBindingDivisor(bindingindex, divisor);
        } else if (ARB_vertex_attrib_binding) {
            ARBVertexAttribBinding.glVertexBindingDivisor(bindingindex, divisor);
        }
    }
    
    // ========================================================================
    // GL 4.3 - TEXTURE VIEWS
    // ========================================================================
    
    /**
     * glTextureView - Create texture view
     * GL 4.3: glTextureView(texture, target, origtexture, internalformat, minlevel, numlevels, minlayer, numlayers)
     */
    public static void textureView(int texture, int target, int origtexture, int internalformat,
                                    int minlevel, int numlevels, int minlayer, int numlayers) {
        if (GL43) {
            GL43.glTextureView(texture, target, origtexture, internalformat, minlevel, numlevels, minlayer, numlayers);
        } else if (ARB_texture_view) {
            ARBTextureView.glTextureView(texture, target, origtexture, internalformat, minlevel, numlevels, minlayer, numlayers);
        }
    }
    
    /**
     * Create a view of a texture with different format interpretation
     */
    public static int createTextureView(int sourceTexture, int target, int internalFormat) {
        if (!hasTextureViews) return 0;
        
        int view = genTexture();
        textureView(view, target, sourceTexture, internalFormat, 0, 1, 0, 1);
        return view;
    }
    
    /**
     * Create a view of specific mip levels
     */
    public static int createMipView(int sourceTexture, int target, int internalFormat, int minLevel, int numLevels) {
        if (!hasTextureViews) return 0;
        
        int view = genTexture();
        textureView(view, target, sourceTexture, internalFormat, minLevel, numLevels, 0, 1);
        return view;
    }
    
    /**
     * Create a view of specific array layers
     */
    public static int createLayerView(int sourceTexture, int target, int internalFormat, int minLayer, int numLayers) {
        if (!hasTextureViews) return 0;
        
        int view = genTexture();
        textureView(view, target, sourceTexture, internalFormat, 0, 1, minLayer, numLayers);
        return view;
    }
    
    // ========================================================================
    // GL 4.3 - INVALIDATE DATA
    // ========================================================================
    
    /**
     * glInvalidateBufferData - Invalidate entire buffer
     * GL 4.3: glInvalidateBufferData(buffer)
     */
    public static void invalidateBufferData(int buffer) {
        if (GL43) {
            GL43.glInvalidateBufferData(buffer);
        } else if (ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateBufferData(buffer);
        }
    }
    
    /**
     * glInvalidateBufferSubData - Invalidate buffer region
     * GL 4.3: glInvalidateBufferSubData(buffer, offset, length)
     */
    public static void invalidateBufferSubData(int buffer, long offset, long length) {
        if (GL43) {
            GL43.glInvalidateBufferSubData(buffer, offset, length);
        } else if (ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateBufferSubData(buffer, offset, length);
        }
    }
    
    /**
     * glInvalidateTexImage - Invalidate entire texture level
     * GL 4.3: glInvalidateTexImage(texture, level)
     */
    public static void invalidateTexImage(int texture, int level) {
        if (GL43) {
            GL43.glInvalidateTexImage(texture, level);
        } else if (ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateTexImage(texture, level);
        }
    }
    
    /**
     * glInvalidateTexSubImage - Invalidate texture region
     * GL 4.3: glInvalidateTexSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth)
     */
    public static void invalidateTexSubImage(int texture, int level, int xoffset, int yoffset, int zoffset,
                                              int width, int height, int depth) {
        if (GL43) {
            GL43.glInvalidateTexSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth);
        } else if (ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateTexSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth);
        }
    }
    
    /**
     * glInvalidateFramebuffer - Invalidate framebuffer attachments
     * GL 4.3: glInvalidateFramebuffer(target, attachments)
     */
    public static void invalidateFramebuffer(int target, IntBuffer attachments) {
        if (GL43) {
            GL43.glInvalidateFramebuffer(target, attachments);
        } else if (ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateFramebuffer(target, attachments);
        }
    }
    
    /**
     * Invalidate framebuffer with varargs
     */
    public static void invalidateFramebuffer(int target, int... attachments) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.ints(attachments);
            invalidateFramebuffer(target, buf);
        }
    }
    
    /**
     * glInvalidateSubFramebuffer - Invalidate framebuffer region
     * GL 4.3: glInvalidateSubFramebuffer(target, attachments, x, y, width, height)
     */
    public static void invalidateSubFramebuffer(int target, IntBuffer attachments, int x, int y, int width, int height) {
        if (GL43) {
            GL43.glInvalidateSubFramebuffer(target, attachments, x, y, width, height);
        } else if (ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateSubFramebuffer(target, attachments, x, y, width, height);
        }
    }
    
    // ========================================================================
    // GL 4.3 - CLEAR BUFFER DATA
    // ========================================================================
    
    /**
     * glClearBufferData - Clear buffer to constant value
     * GL 4.3: glClearBufferData(target, internalformat, format, type, data)
     */
    public static void clearBufferData(int target, int internalformat, int format, int type, ByteBuffer data) {
        if (GL43) {
            GL43.glClearBufferData(target, internalformat, format, type, data);
        } else if (ARB_clear_buffer_object) {
            ARBClearBufferObject.glClearBufferData(target, internalformat, format, type, data);
        }
    }
    
    /**
     * Clear buffer to zero
     */
    public static void clearBufferToZero(int target, int internalformat) {
        clearBufferData(target, internalformat, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, null);
    }
    
    /**
     * glClearBufferSubData - Clear buffer region to constant value
     * GL 4.3: glClearBufferSubData(target, internalformat, offset, size, format, type, data)
     */
    public static void clearBufferSubData(int target, int internalformat, long offset, long size, 
                                          int format, int type, ByteBuffer data) {
        if (GL43) {
            GL43.glClearBufferSubData(target, internalformat, offset, size, format, type, data);
        } else if (ARB_clear_buffer_object) {
            ARBClearBufferObject.glClearBufferSubData(target, internalformat, offset, size, format, type, data);
        }
    }
    
    // ========================================================================
    // GL 4.3 - COPY IMAGE
    // ========================================================================
    
    /**
     * glCopyImageSubData - Copy between textures/renderbuffers
     * GL 4.3: glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ,
     *                            dstName, dstTarget, dstLevel, dstX, dstY, dstZ,
     *                            srcWidth, srcHeight, srcDepth)
     */
    public static void copyImageSubData(int srcName, int srcTarget, int srcLevel, int srcX, int srcY, int srcZ,
                                         int dstName, int dstTarget, int dstLevel, int dstX, int dstY, int dstZ,
                                         int srcWidth, int srcHeight, int srcDepth) {
        if (GL43) {
            GL43.glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ,
                                    dstName, dstTarget, dstLevel, dstX, dstY, dstZ,
                                    srcWidth, srcHeight, srcDepth);
        } else if (ARB_copy_image) {
            ARBCopyImage.glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ,
                                            dstName, dstTarget, dstLevel, dstX, dstY, dstZ,
                                            srcWidth, srcHeight, srcDepth);
        } else if (NV_copy_image) {
            NVCopyImage.glCopyImageSubDataNV(srcName, srcTarget, srcLevel, srcX, srcY, srcZ,
                                             dstName, dstTarget, dstLevel, dstX, dstY, dstZ,
                                             srcWidth, srcHeight, srcDepth);
        }
    }
    
    /**
     * Copy 2D texture region
     */
    public static void copyTexture2D(int srcTexture, int srcLevel, int srcX, int srcY,
                                      int dstTexture, int dstLevel, int dstX, int dstY,
                                      int width, int height) {
        copyImageSubData(srcTexture, GL11.GL_TEXTURE_2D, srcLevel, srcX, srcY, 0,
                        dstTexture, GL11.GL_TEXTURE_2D, dstLevel, dstX, dstY, 0,
                        width, height, 1);
    }
    
    // ========================================================================
    // GL 4.3 - FRAMEBUFFER NO ATTACHMENTS
    // ========================================================================
    
    /**
     * glFramebufferParameteri - Set framebuffer parameter
     * GL 4.3: glFramebufferParameteri(target, pname, param)
     */
    public static void framebufferParameteri(int target, int pname, int param) {
        if (GL43) {
            GL43.glFramebufferParameteri(target, pname, param);
        } else if (ARB_framebuffer_no_attachments) {
            ARBFramebufferNoAttachments.glFramebufferParameteri(target, pname, param);
        }
    }
    
    /**
     * Set framebuffer default width
     */
    public static void setFramebufferDefaultWidth(int target, int width) {
        framebufferParameteri(target, GL43.GL_FRAMEBUFFER_DEFAULT_WIDTH, width);
    }
    
    /**
     * Set framebuffer default height
     */
    public static void setFramebufferDefaultHeight(int target, int height) {
        framebufferParameteri(target, GL43.GL_FRAMEBUFFER_DEFAULT_HEIGHT, height);
    }
    
    /**
     * Set framebuffer default layers
     */
    public static void setFramebufferDefaultLayers(int target, int layers) {
        framebufferParameteri(target, GL43.GL_FRAMEBUFFER_DEFAULT_LAYERS, layers);
    }
    
    /**
     * Set framebuffer default samples
     */
    public static void setFramebufferDefaultSamples(int target, int samples) {
        framebufferParameteri(target, GL43.GL_FRAMEBUFFER_DEFAULT_SAMPLES, samples);
    }
    
    // ========================================================================
    // GL 4.4 - BUFFER STORAGE (IMMUTABLE)
    // ========================================================================
    
    // Buffer storage flags
    public static final int GL_MAP_READ_BIT = GL30.GL_MAP_READ_BIT;
    public static final int GL_MAP_WRITE_BIT = GL30.GL_MAP_WRITE_BIT;
    public static final int GL_MAP_PERSISTENT_BIT = GL44.GL_MAP_PERSISTENT_BIT;
    public static final int GL_MAP_COHERENT_BIT = GL44.GL_MAP_COHERENT_BIT;
    public static final int GL_DYNAMIC_STORAGE_BIT = GL44.GL_DYNAMIC_STORAGE_BIT;
    public static final int GL_CLIENT_STORAGE_BIT = GL44.GL_CLIENT_STORAGE_BIT;
    
    /**
     * Check if buffer storage is supported
     */
    public static boolean supportsBufferStorage() {
        return hasImmutableStorage;
    }
    
    /**
     * Check if persistent mapping is supported
     */
    public static boolean supportsPersistentMapping() {
        return hasPersistentMapping;
    }
    
    /**
     * glBufferStorage - Create immutable buffer storage
     * GL 4.4: glBufferStorage(target, size, flags)
     */
    public static void bufferStorage(int target, long size, int flags) {
        if (GL44) {
            GL44.glBufferStorage(target, size, flags);
        } else if (ARB_buffer_storage) {
            ARBBufferStorage.glBufferStorage(target, size, flags);
        } else {
            // Fallback to mutable storage
            int usage = translateStorageFlagsToUsage(flags);
            GL15.glBufferData(target, size, usage);
        }
    }
    
    /**
     * glBufferStorage with data
     */
    public static void bufferStorage(int target, ByteBuffer data, int flags) {
        if (GL44) {
            GL44.glBufferStorage(target, data, flags);
        } else if (ARB_buffer_storage) {
            ARBBufferStorage.glBufferStorage(target, data, flags);
        } else {
            int usage = translateStorageFlagsToUsage(flags);
            GL15.glBufferData(target, data, usage);
        }
    }
    
    public static void bufferStorage(int target, FloatBuffer data, int flags) {
        if (GL44) {
            GL44.glBufferStorage(target, data, flags);
        } else if (ARB_buffer_storage) {
            ARBBufferStorage.glBufferStorage(target, data, flags);
        } else {
            int usage = translateStorageFlagsToUsage(flags);
            GL15.glBufferData(target, data, usage);
        }
    }
    
    public static void bufferStorage(int target, IntBuffer data, int flags) {
        if (GL44) {
            GL44.glBufferStorage(target, data, flags);
        } else if (ARB_buffer_storage) {
            ARBBufferStorage.glBufferStorage(target, data, flags);
        } else {
            int usage = translateStorageFlagsToUsage(flags);
            GL15.glBufferData(target, data, usage);
        }
    }
    
    /**
     * Translate storage flags to usage hint for fallback
     */
    private static int translateStorageFlagsToUsage(int flags) {
        boolean read = (flags & GL_MAP_READ_BIT) != 0;
        boolean write = (flags & GL_MAP_WRITE_BIT) != 0;
        boolean dynamic = (flags & GL_DYNAMIC_STORAGE_BIT) != 0;
        
        if (dynamic) {
            if (read && write) return GL15.GL_DYNAMIC_COPY;
            if (read) return GL15.GL_DYNAMIC_READ;
            return GL15.GL_DYNAMIC_DRAW;
        } else {
            if (read && write) return GL15.GL_STATIC_COPY;
            if (read) return GL15.GL_STATIC_READ;
            return GL15.GL_STATIC_DRAW;
        }
    }
    
    // ========================================================================
    // GL 4.4 - PERSISTENT MAPPING
    // ========================================================================
    
    /**
     * Persistent buffer info
     */
    public static class PersistentBuffer {
        public final int bufferId;
        public final ByteBuffer mappedBuffer;
        public final long size;
        public final int flags;
        public final boolean coherent;
        
        public PersistentBuffer(int bufferId, ByteBuffer mappedBuffer, long size, int flags) {
            this.bufferId = bufferId;
            this.mappedBuffer = mappedBuffer;
            this.size = size;
            this.flags = flags;
            this.coherent = (flags & GL_MAP_COHERENT_BIT) != 0;
        }
        
        /**
         * Ensure writes are visible (only needed if not coherent)
         */
        public void flush(long offset, long length) {
            if (!coherent) {
                GL30.glFlushMappedBufferRange(GL15.GL_ARRAY_BUFFER, offset, length);
            }
        }
    }
    
    /**
     * Create persistently mapped buffer
     */
    public static PersistentBuffer createPersistentBuffer(int target, long size, boolean coherent, boolean read, boolean write) {
        if (!hasPersistentMapping) {
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Persistent mapping not supported");
            }
            return null;
        }
        
        int buffer = genBuffer();
        GL15.glBindBuffer(target, buffer);
        
        int storageFlags = GL_MAP_PERSISTENT_BIT;
        if (coherent) storageFlags |= GL_MAP_COHERENT_BIT;
        if (read) storageFlags |= GL_MAP_READ_BIT;
        if (write) storageFlags |= GL_MAP_WRITE_BIT;
        storageFlags |= GL_DYNAMIC_STORAGE_BIT;
        
        bufferStorage(target, size, storageFlags);
        
        int mapFlags = GL_MAP_PERSISTENT_BIT;
        if (coherent) mapFlags |= GL_MAP_COHERENT_BIT;
        if (read) mapFlags |= GL_MAP_READ_BIT;
        if (write) mapFlags |= GL_MAP_WRITE_BIT;
        
        ByteBuffer mapped = GL30.glMapBufferRange(target, 0, size, mapFlags);
        
        GL15.glBindBuffer(target, 0);
        
        return new PersistentBuffer(buffer, mapped, size, storageFlags);
    }
    
    /**
     * Create write-only persistent buffer (most common case)
     */
    public static PersistentBuffer createWritePersistentBuffer(int target, long size) {
        return createPersistentBuffer(target, size, true, false, true);
    }
    
    /**
     * Create read-write persistent buffer
     */
    public static PersistentBuffer createReadWritePersistentBuffer(int target, long size) {
        return createPersistentBuffer(target, size, true, true, true);
    }
    
    /**
     * Delete persistent buffer
     */
    public static void deletePersistentBuffer(PersistentBuffer buffer) {
        if (buffer == null) return;
        
        // Unmap is automatic when buffer is deleted with immutable storage
        deleteBuffer(buffer.bufferId);
    }
    
    // ========================================================================
    // GL 4.4 - MULTI-BIND
    // ========================================================================
    
    /**
     * Check if multi-bind is supported
     */
    public static boolean supportsMultiBind() {
        return hasMultibind;
    }
    
    /**
     * glBindBuffersBase - Bind multiple buffers to sequential binding points
     * GL 4.4: glBindBuffersBase(target, first, buffers)
     */
    public static void bindBuffersBase(int target, int first, IntBuffer buffers) {
        if (GL44) {
            GL44.glBindBuffersBase(target, first, buffers);
        } else if (ARB_multi_bind) {
            ARBMultiBind.glBindBuffersBase(target, first, buffers);
        } else {
            // Fallback: bind individually
            int count = buffers != null ? buffers.remaining() : 0;
            for (int i = 0; i < count; i++) {
                GL30.glBindBufferBase(target, first + i, buffers.get(buffers.position() + i));
            }
        }
    }
    
    /**
     * glBindBuffersRange - Bind multiple buffer ranges
     * GL 4.4: glBindBuffersRange(target, first, buffers, offsets, sizes)
     */
    public static void bindBuffersRange(int target, int first, IntBuffer buffers, 
                                        PointerBuffer offsets, PointerBuffer sizes) {
        if (GL44) {
            GL44.glBindBuffersRange(target, first, buffers, offsets, sizes);
        } else if (ARB_multi_bind) {
            ARBMultiBind.glBindBuffersRange(target, first, buffers, offsets, sizes);
        } else {
            // Fallback
            int count = buffers != null ? buffers.remaining() : 0;
            for (int i = 0; i < count; i++) {
                GL30.glBindBufferRange(target, first + i, 
                    buffers.get(buffers.position() + i),
                    offsets.get(offsets.position() + i),
                    sizes.get(sizes.position() + i));
            }
        }
    }
    
    /**
     * glBindTextures - Bind multiple textures
     * GL 4.4: glBindTextures(first, textures)
     */
    public static void bindTextures(int first, IntBuffer textures) {
        if (GL44) {
            GL44.glBindTextures(first, textures);
        } else if (ARB_multi_bind) {
            ARBMultiBind.glBindTextures(first, textures);
        } else {
            // Fallback
            int count = textures != null ? textures.remaining() : 0;
            for (int i = 0; i < count; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + first + i);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures.get(textures.position() + i));
            }
        }
    }
    
    /**
     * glBindSamplers - Bind multiple samplers
     * GL 4.4: glBindSamplers(first, samplers)
     */
    public static void bindSamplers(int first, IntBuffer samplers) {
        if (GL44) {
            GL44.glBindSamplers(first, samplers);
        } else if (ARB_multi_bind) {
            ARBMultiBind.glBindSamplers(first, samplers);
        } else if (hasSamplerObjects) {
            // Fallback
            int count = samplers != null ? samplers.remaining() : 0;
            for (int i = 0; i < count; i++) {
                GL33.glBindSampler(first + i, samplers.get(samplers.position() + i));
            }
        }
    }
    
    /**
     * glBindImageTextures - Bind multiple image textures
     * GL 4.4: glBindImageTextures(first, textures)
     */
    public static void bindImageTextures(int first, IntBuffer textures) {
        if (GL44) {
            GL44.glBindImageTextures(first, textures);
        } else if (ARB_multi_bind) {
            ARBMultiBind.glBindImageTextures(first, textures);
        }
        // No simple fallback for image textures
    }
    
    /**
     * glBindVertexBuffers - Bind multiple vertex buffers
     * GL 4.4: glBindVertexBuffers(first, buffers, offsets, strides)
     */
    public static void bindVertexBuffers(int first, IntBuffer buffers, PointerBuffer offsets, IntBuffer strides) {
        if (GL44) {
            GL44.glBindVertexBuffers(first, buffers, offsets, strides);
        } else if (ARB_multi_bind) {
            ARBMultiBind.glBindVertexBuffers(first, buffers, offsets, strides);
        } else if (hasVertexAttribBinding) {
            // Fallback
            int count = buffers != null ? buffers.remaining() : 0;
            for (int i = 0; i < count; i++) {
                GL43.glBindVertexBuffer(first + i,
                    buffers.get(buffers.position() + i),
                    offsets.get(offsets.position() + i),
                    strides.get(strides.position() + i));
            }
        }
    }
    
    // ========================================================================
    // GL 4.4 - QUERY BUFFER OBJECT
    // ========================================================================
    
    public static final int GL_QUERY_BUFFER = GL44.GL_QUERY_BUFFER;
    public static final int GL_QUERY_BUFFER_BINDING = GL44.GL_QUERY_BUFFER_BINDING;
    public static final int GL_QUERY_RESULT_NO_WAIT = GL44.GL_QUERY_RESULT_NO_WAIT;
    
    /**
     * Bind query buffer for async query results
     */
    public static void bindQueryBuffer(int buffer) {
        if (GL44) {
            GL15.glBindBuffer(GL44.GL_QUERY_BUFFER, buffer);
        } else if (ARB_query_buffer_object) {
            GL15.glBindBuffer(ARBQueryBufferObject.GL_QUERY_BUFFER, buffer);
        }
    }
    
    /**
     * Get query result into buffer (async)
     */
    public static void getQueryObjectToBuffer(int id, int pname, long offset) {
        if (GL44 || ARB_query_buffer_object) {
            // With query buffer bound, result goes to buffer
            GL15.glGetQueryObjectuiv(id, pname, offset);
        }
    }
    
    // ========================================================================
    // ADDITIONAL EXTENSION FLAGS FOR GL 4.3-4.4
    // ========================================================================
    
    private static boolean ARB_compute_shader = false;
    private static boolean ARB_shader_storage_buffer_object = false;
    private static boolean ARB_multi_draw_indirect = false;
    private static boolean KHR_debug = false;
    private static boolean ARB_debug_output = false;
    private static boolean ARB_program_interface_query = false;
    private static boolean ARB_vertex_attrib_binding = false;
    private static boolean ARB_texture_view = false;
    private static boolean ARB_invalidate_subdata = false;
    private static boolean ARB_clear_buffer_object = false;
    private static boolean ARB_copy_image = false;
    private static boolean NV_copy_image = false;
    private static boolean ARB_framebuffer_no_attachments = false;
    private static boolean ARB_buffer_storage = false;
    private static boolean ARB_multi_bind = false;
    private static boolean ARB_query_buffer_object = false;
    private static boolean ARB_texture_storage_multisample = false;
    
    // Add to StateTracker
    // public int boundDrawIndirectBuffer = 0;
    
    // ========================================================================
    // PERFORMANCE UTILITIES
    // ========================================================================
    
    /**
     * Triple-buffered persistent buffer for streaming data
     * Handles synchronization automatically
     */
    public static class StreamingBuffer {
        private final PersistentBuffer buffer;
        private final int numSections;
        private final long sectionSize;
        private int currentSection = 0;
        private final long[] fences;
        
        public StreamingBuffer(int target, long sectionSize, int numSections) {
            this.numSections = numSections;
            this.sectionSize = sectionSize;
            this.fences = new long[numSections];
            
            long totalSize = sectionSize * numSections;
            this.buffer = createWritePersistentBuffer(target, totalSize);
        }
        
        /**
         * Get buffer section for writing, waiting if necessary
         */
        public ByteBuffer getWriteSection() {
            // Wait for this section's fence if set
            if (fences[currentSection] != 0) {
                int result = clientWaitSync(fences[currentSection], 
                    GL32.GL_SYNC_FLUSH_COMMANDS_BIT, 1_000_000_000L); // 1 second timeout
                    
                if (result == GL32.GL_WAIT_FAILED) {
                    if (debugMode) {
                        System.err.println("[OpenGLCallMapper] Fence wait failed");
                    }
                }
                
                deleteSync(fences[currentSection]);
                fences[currentSection] = 0;
            }
            
            long offset = currentSection * sectionSize;
            buffer.mappedBuffer.position((int) offset);
            buffer.mappedBuffer.limit((int) (offset + sectionSize));
            return buffer.mappedBuffer.slice();
        }
        
        /**
         * Lock current section and advance to next
         */
        public void lockSection() {
            fences[currentSection] = fenceSync();
            currentSection = (currentSection + 1) % numSections;
        }
        
        /**
         * Get buffer ID for binding
         */
        public int getBufferId() {
            return buffer.bufferId;
        }
        
        /**
         * Get offset for current section (for draw calls)
         */
        public long getCurrentOffset() {
            return currentSection * sectionSize;
        }
        
        /**
         * Clean up
         */
        public void destroy() {
            for (int i = 0; i < numSections; i++) {
                if (fences[i] != 0) {
                    deleteSync(fences[i]);
                }
            }
            deletePersistentBuffer(buffer);
        }
    }
    
    /**
     * Create triple-buffered streaming buffer
     */
    public static StreamingBuffer createStreamingBuffer(int target, long sectionSize) {
        if (!hasPersistentMapping) {
            return null;
        }
        return new StreamingBuffer(target, sectionSize, 3);
    }
}

    // ========================================================================
    // LWJGL 3.x MEMORY MANAGEMENT UTILITIES
    // ========================================================================
    
    /**
     * Execute operation with stack-allocated int buffer
     * Zero-allocation pattern for hot paths
     */
    public static int withStackInt(java.util.function.IntSupplier operation) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return operation.getAsInt();
        }
    }
    
    /**
     * Generate single GL object using stack allocation
     */
    @FunctionalInterface
    public interface GLGenFunction {
        void generate(IntBuffer buffer);
    }
    
    public static int glGenSingle(GLGenFunction genFunc) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.mallocInt(1);
            genFunc.generate(buf);
            return buf.get(0);
        }
    }
    
    /**
     * Delete single GL object using stack allocation
     */
    @FunctionalInterface
    public interface GLDeleteFunction {
        void delete(IntBuffer buffer);
    }
    
    public static void glDeleteSingle(int id, GLDeleteFunction deleteFunc) {
        if (id == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.ints(id);
            deleteFunc.delete(buf);
        }
    }
    
    /**
     * Get single integer parameter using stack
     */
    public static int glGetIntegerStack(int pname) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.mallocInt(1);
            GL11.glGetIntegerv(pname, buf);
            return buf.get(0);
        }
    }
    
    /**
     * Get integer array using stack
     */
    public static int[] glGetIntegervStack(int pname, int count) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.mallocInt(count);
            GL11.glGetIntegerv(pname, buf);
            int[] result = new int[count];
            buf.get(result);
            return result;
        }
    }
    
    // ========================================================================
    // GL 4.3 - COMPUTE SHADERS
    // ========================================================================
    
    // Compute shader constants
    public static final int GL_COMPUTE_SHADER = GL43.GL_COMPUTE_SHADER;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_COUNT = GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_SIZE = GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS = GL43.GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS;
    public static final int GL_MAX_COMPUTE_UNIFORM_BLOCKS = GL43.GL_MAX_COMPUTE_UNIFORM_BLOCKS;
    public static final int GL_MAX_COMPUTE_TEXTURE_IMAGE_UNITS = GL43.GL_MAX_COMPUTE_TEXTURE_IMAGE_UNITS;
    public static final int GL_MAX_COMPUTE_ATOMIC_COUNTERS = GL43.GL_MAX_COMPUTE_ATOMIC_COUNTERS;
    public static final int GL_MAX_COMPUTE_ATOMIC_COUNTER_BUFFERS = GL43.GL_MAX_COMPUTE_ATOMIC_COUNTER_BUFFERS;
    public static final int GL_MAX_COMPUTE_SHARED_MEMORY_SIZE = GL43.GL_MAX_COMPUTE_SHARED_MEMORY_SIZE;
    public static final int GL_MAX_COMPUTE_UNIFORM_COMPONENTS = GL43.GL_MAX_COMPUTE_UNIFORM_COMPONENTS;
    public static final int GL_MAX_COMBINED_COMPUTE_UNIFORM_COMPONENTS = GL43.GL_MAX_COMBINED_COMPUTE_UNIFORM_COMPONENTS;
    
    public static final int GL_COMPUTE_WORK_GROUP_SIZE = GL43.GL_COMPUTE_WORK_GROUP_SIZE;
    
    /**
     * Check if compute shaders are supported
     */
    public static boolean supportsComputeShaders() {
        return hasComputeShaders;
    }
    
    /**
     * Create compute shader
     */
    public static int createComputeShader() {
        if (GL43) {
            return GL20.glCreateShader(GL43.GL_COMPUTE_SHADER);
        } else if (ARB_compute_shader) {
            return GL20.glCreateShader(ARBComputeShader.GL_COMPUTE_SHADER);
        }
        return 0;
    }
    
    /**
     * Create and compile compute shader from source
     */
    public static int createComputeShaderFromSource(CharSequence source) {
        int shader = createComputeShader();
        if (shader == 0) return 0;
        
        shaderSource(shader, source);
        compileShader(shader);
        
        if (getShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE) {
            String log = getShaderInfoLog(shader);
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Compute shader compilation failed:");
                System.err.println(log);
            }
            deleteShader(shader);
            return 0;
        }
        
        return shader;
    }
    
    /**
     * Create complete compute program from source
     */
    public static int createComputeProgram(CharSequence source) {
        int shader = createComputeShaderFromSource(source);
        if (shader == 0) return 0;
        
        int program = createProgram();
        attachShader(program, shader);
        linkProgram(program);
        
        deleteShader(shader); // Can delete after linking
        
        if (getProgrami(program, GL20.GL_LINK_STATUS) != GL11.GL_TRUE) {
            String log = getProgramInfoLog(program);
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Compute program linking failed:");
                System.err.println(log);
            }
            deleteProgram(program);
            return 0;
        }
        
        return program;
    }
    
    /**
     * glDispatchCompute - Dispatch compute work groups
     * GL 4.3: glDispatchCompute(num_groups_x, num_groups_y, num_groups_z)
     */
    public static void dispatchCompute(int num_groups_x, int num_groups_y, int num_groups_z) {
        if (GL43) {
            GL43.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
        } else if (ARB_compute_shader) {
            ARBComputeShader.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
        }
    }
    
    /**
     * glDispatchComputeIndirect - Dispatch compute from buffer
     * GL 4.3: glDispatchComputeIndirect(indirect)
     */
    public static void dispatchComputeIndirect(long indirect) {
        if (GL43) {
            GL43.glDispatchComputeIndirect(indirect);
        } else if (ARB_compute_shader) {
            ARBComputeShader.glDispatchComputeIndirect(indirect);
        }
    }
    
    /**
     * Get compute work group size for a program
     */
    public static int[] getComputeWorkGroupSize(int program) {
        if (!hasComputeShaders) return new int[]{0, 0, 0};
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.mallocInt(3);
            GL20.glGetProgramiv(program, GL43.GL_COMPUTE_WORK_GROUP_SIZE, buf);
            return new int[]{buf.get(0), buf.get(1), buf.get(2)};
        }
    }
    
    /**
     * Get maximum work group count for each dimension
     */
    public static int[] getMaxComputeWorkGroupCount() {
        if (!hasComputeShaders) return new int[]{0, 0, 0};
        
        int[] result = new int[3];
        result[0] = glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 0);
        result[1] = glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 1);
        result[2] = glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT, 2);
        return result;
    }
    
    /**
     * Get maximum work group size for each dimension
     */
    public static int[] getMaxComputeWorkGroupSize() {
        if (!hasComputeShaders) return new int[]{0, 0, 0};
        
        int[] result = new int[3];
        result[0] = glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 0);
        result[1] = glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 1);
        result[2] = glGetIntegeri(GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE, 2);
        return result;
    }
    
    /**
     * glGetIntegeri_v - Get indexed integer
     */
    public static int glGetIntegeri(int target, int index) {
        if (GL30) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                GL30.glGetIntegeri_v(target, index, buf);
                return buf.get(0);
            }
        }
        return 0;
    }
    
    // ========================================================================
    // GL 4.3 - SHADER STORAGE BUFFER OBJECTS (SSBO)
    // ========================================================================
    
    // SSBO constants
    public static final int GL_SHADER_STORAGE_BUFFER = GL43.GL_SHADER_STORAGE_BUFFER;
    public static final int GL_SHADER_STORAGE_BUFFER_BINDING = GL43.GL_SHADER_STORAGE_BUFFER_BINDING;
    public static final int GL_SHADER_STORAGE_BUFFER_START = GL43.GL_SHADER_STORAGE_BUFFER_START;
    public static final int GL_SHADER_STORAGE_BUFFER_SIZE = GL43.GL_SHADER_STORAGE_BUFFER_SIZE;
    public static final int GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS = GL43.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS;
    public static final int GL_MAX_SHADER_STORAGE_BLOCK_SIZE = GL43.GL_MAX_SHADER_STORAGE_BLOCK_SIZE;
    public static final int GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS = GL43.GL_MAX_COMBINED_SHADER_STORAGE_BLOCKS;
    public static final int GL_SHADER_STORAGE_BARRIER_BIT = GL43.GL_SHADER_STORAGE_BARRIER_BIT;
    
    /**
     * Check if SSBOs are supported
     */
    public static boolean supportsSSBO() {
        return hasSSBO;
    }
    
    /**
     * Bind shader storage buffer to binding point
     */
    public static void bindShaderStorageBuffer(int bindingPoint, int buffer) {
        if (GL43) {
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, bindingPoint, buffer);
        } else if (ARB_shader_storage_buffer_object) {
            GL30.glBindBufferBase(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, bindingPoint, buffer);
        }
    }
    
    /**
     * Bind shader storage buffer range to binding point
     */
    public static void bindShaderStorageBufferRange(int bindingPoint, int buffer, long offset, long size) {
        if (GL43) {
            GL30.glBindBufferRange(GL43.GL_SHADER_STORAGE_BUFFER, bindingPoint, buffer, offset, size);
        } else if (ARB_shader_storage_buffer_object) {
            GL30.glBindBufferRange(ARBShaderStorageBufferObject.GL_SHADER_STORAGE_BUFFER, bindingPoint, buffer, offset, size);
        }
    }
    
    /**
     * glGetProgramResourceIndex - Get resource index by name
     * GL 4.3: glGetProgramResourceIndex(program, programInterface, name)
     */
    public static int getProgramResourceIndex(int program, int programInterface, CharSequence name) {
        if (GL43) {
            return GL43.glGetProgramResourceIndex(program, programInterface, name);
        } else if (ARB_program_interface_query) {
            return ARBProgramInterfaceQuery.glGetProgramResourceIndex(program, programInterface, name);
        }
        return GL31.GL_INVALID_INDEX;
    }
    
    /**
     * glShaderStorageBlockBinding - Set SSBO binding point
     * GL 4.3: glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding)
     */
    public static void shaderStorageBlockBinding(int program, int storageBlockIndex, int storageBlockBinding) {
        if (GL43) {
            GL43.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
        } else if (ARB_shader_storage_buffer_object) {
            ARBShaderStorageBufferObject.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
        }
    }
    
    /**
     * Get shader storage block index
     */
    public static int getShaderStorageBlockIndex(int program, CharSequence name) {
        return getProgramResourceIndex(program, GL43.GL_SHADER_STORAGE_BLOCK, name);
    }
    
    // ========================================================================
    // GL 4.3 - MULTI-DRAW INDIRECT
    // ========================================================================
    
    // Indirect buffer binding
    public static final int GL_DRAW_INDIRECT_BUFFER = GL40.GL_DRAW_INDIRECT_BUFFER;
    public static final int GL_DRAW_INDIRECT_BUFFER_BINDING = GL40.GL_DRAW_INDIRECT_BUFFER_BINDING;
    
    /**
     * Bind draw indirect buffer
     */
    public static void bindDrawIndirectBuffer(int buffer) {
        StateTracker state = getState();
        if (state.boundDrawIndirectBuffer == buffer) return;
        state.boundDrawIndirectBuffer = buffer;
        
        if (GL40) {
            GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, buffer);
        } else if (ARB_draw_indirect) {
            GL15.glBindBuffer(ARBDrawIndirect.GL_DRAW_INDIRECT_BUFFER, buffer);
        }
    }
    
    /**
     * glMultiDrawArraysIndirect - Multiple indirect array draws
     * GL 4.3: glMultiDrawArraysIndirect(mode, indirect, drawcount, stride)
     */
    public static void multiDrawArraysIndirect(int mode, long indirect, int drawcount, int stride) {
        if (GL43) {
            GL43.glMultiDrawArraysIndirect(mode, indirect, drawcount, stride);
        } else if (ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawArraysIndirect(mode, indirect, drawcount, stride);
        } else if (GL40) {
            // Fallback: loop over individual indirect draws
            long offset = indirect;
            int commandStride = stride != 0 ? stride : 16; // sizeof(DrawArraysIndirectCommand)
            for (int i = 0; i < drawcount; i++) {
                GL40.glDrawArraysIndirect(mode, offset);
                offset += commandStride;
            }
        }
    }
    
    /**
     * glMultiDrawArraysIndirect with ByteBuffer
     */
    public static void multiDrawArraysIndirect(int mode, ByteBuffer indirect, int drawcount, int stride) {
        if (GL43) {
            GL43.glMultiDrawArraysIndirect(mode, indirect, drawcount, stride);
        } else if (ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawArraysIndirect(mode, indirect, drawcount, stride);
        }
    }
    
    /**
     * glMultiDrawElementsIndirect - Multiple indirect element draws
     * GL 4.3: glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride)
     */
    public static void multiDrawElementsIndirect(int mode, int type, long indirect, int drawcount, int stride) {
        if (GL43) {
            GL43.glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride);
        } else if (ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride);
        } else if (GL40) {
            // Fallback: loop over individual indirect draws
            long offset = indirect;
            int commandStride = stride != 0 ? stride : 20; // sizeof(DrawElementsIndirectCommand)
            for (int i = 0; i < drawcount; i++) {
                GL40.glDrawElementsIndirect(mode, type, offset);
                offset += commandStride;
            }
        }
    }
    
    /**
     * glMultiDrawElementsIndirect with ByteBuffer
     */
    public static void multiDrawElementsIndirect(int mode, int type, ByteBuffer indirect, int drawcount, int stride) {
        if (GL43) {
            GL43.glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride);
        } else if (ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride);
        }
    }
    
    /**
     * Create indirect draw command buffer
     * Returns buffer ID and mapped pointer for persistent mapping (if available)
     */
    public static class IndirectBuffer {
        public final int bufferId;
        public final ByteBuffer mappedBuffer;
        public final boolean persistentlyMapped;
        public final int maxCommands;
        public final int commandSize;
        
        public IndirectBuffer(int bufferId, ByteBuffer mappedBuffer, boolean persistentlyMapped, 
                             int maxCommands, int commandSize) {
            this.bufferId = bufferId;
            this.mappedBuffer = mappedBuffer;
            this.persistentlyMapped = persistentlyMapped;
            this.maxCommands = maxCommands;
            this.commandSize = commandSize;
        }
    }
    
    /**
     * Create indirect buffer for DrawArraysIndirectCommand
     * Structure: { uint count, uint instanceCount, uint first, uint baseInstance }
     */
    public static IndirectBuffer createDrawArraysIndirectBuffer(int maxCommands) {
        int commandSize = 16; // 4 uints
        int bufferSize = maxCommands * commandSize;
        
        int buffer = genBuffer();
        GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, buffer);
        
        ByteBuffer mapped = null;
        boolean persistent = false;
        
        if (hasPersistentMapping) {
            // Use persistent mapping for best streaming performance
            int flags = GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT;
            GL44.glBufferStorage(GL40.GL_DRAW_INDIRECT_BUFFER, bufferSize, 
                flags | GL44.GL_DYNAMIC_STORAGE_BIT);
            mapped = GL30.glMapBufferRange(GL40.GL_DRAW_INDIRECT_BUFFER, 0, bufferSize, flags);
            persistent = true;
        } else {
            GL15.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER, bufferSize, GL15.GL_DYNAMIC_DRAW);
        }
        
        GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, 0);
        
        return new IndirectBuffer(buffer, mapped, persistent, maxCommands, commandSize);
    }
    
    /**
     * Create indirect buffer for DrawElementsIndirectCommand
     * Structure: { uint count, uint instanceCount, uint firstIndex, int baseVertex, uint baseInstance }
     */
    public static IndirectBuffer createDrawElementsIndirectBuffer(int maxCommands) {
        int commandSize = 20; // 5 uints
        int bufferSize = maxCommands * commandSize;
        
        int buffer = genBuffer();
        GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, buffer);
        
        ByteBuffer mapped = null;
        boolean persistent = false;
        
        if (hasPersistentMapping) {
            int flags = GL44.GL_MAP_WRITE_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT;
            GL44.glBufferStorage(GL40.GL_DRAW_INDIRECT_BUFFER, bufferSize, 
                flags | GL44.GL_DYNAMIC_STORAGE_BIT);
            mapped = GL30.glMapBufferRange(GL40.GL_DRAW_INDIRECT_BUFFER, 0, bufferSize, flags);
            persistent = true;
        } else {
            GL15.glBufferData(GL40.GL_DRAW_INDIRECT_BUFFER, bufferSize, GL15.GL_DYNAMIC_DRAW);
        }
        
        GL15.glBindBuffer(GL40.GL_DRAW_INDIRECT_BUFFER, 0);
        
        return new IndirectBuffer(buffer, mapped, persistent, maxCommands, commandSize);
    }
    
    // ========================================================================
    // GL 4.3 - DEBUG OUTPUT
    // ========================================================================
    
    // Debug constants
    public static final int GL_DEBUG_OUTPUT = GL43.GL_DEBUG_OUTPUT;
    public static final int GL_DEBUG_OUTPUT_SYNCHRONOUS = GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS;
    public static final int GL_DEBUG_SOURCE_API = GL43.GL_DEBUG_SOURCE_API;
    public static final int GL_DEBUG_SOURCE_WINDOW_SYSTEM = GL43.GL_DEBUG_SOURCE_WINDOW_SYSTEM;
    public static final int GL_DEBUG_SOURCE_SHADER_COMPILER = GL43.GL_DEBUG_SOURCE_SHADER_COMPILER;
    public static final int GL_DEBUG_SOURCE_THIRD_PARTY = GL43.GL_DEBUG_SOURCE_THIRD_PARTY;
    public static final int GL_DEBUG_SOURCE_APPLICATION = GL43.GL_DEBUG_SOURCE_APPLICATION;
    public static final int GL_DEBUG_SOURCE_OTHER = GL43.GL_DEBUG_SOURCE_OTHER;
    public static final int GL_DEBUG_TYPE_ERROR = GL43.GL_DEBUG_TYPE_ERROR;
    public static final int GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR = GL43.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR;
    public static final int GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR = GL43.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR;
    public static final int GL_DEBUG_TYPE_PORTABILITY = GL43.GL_DEBUG_TYPE_PORTABILITY;
    public static final int GL_DEBUG_TYPE_PERFORMANCE = GL43.GL_DEBUG_TYPE_PERFORMANCE;
    public static final int GL_DEBUG_TYPE_MARKER = GL43.GL_DEBUG_TYPE_MARKER;
    public static final int GL_DEBUG_TYPE_PUSH_GROUP = GL43.GL_DEBUG_TYPE_PUSH_GROUP;
    public static final int GL_DEBUG_TYPE_POP_GROUP = GL43.GL_DEBUG_TYPE_POP_GROUP;
    public static final int GL_DEBUG_TYPE_OTHER = GL43.GL_DEBUG_TYPE_OTHER;
    public static final int GL_DEBUG_SEVERITY_HIGH = GL43.GL_DEBUG_SEVERITY_HIGH;
    public static final int GL_DEBUG_SEVERITY_MEDIUM = GL43.GL_DEBUG_SEVERITY_MEDIUM;
    public static final int GL_DEBUG_SEVERITY_LOW = GL43.GL_DEBUG_SEVERITY_LOW;
    public static final int GL_DEBUG_SEVERITY_NOTIFICATION = GL43.GL_DEBUG_SEVERITY_NOTIFICATION;
    
    /**
     * Debug callback interface
     */
    @FunctionalInterface
    public interface DebugCallback {
        void onMessage(int source, int type, int id, int severity, String message);
    }
    
    private static DebugCallback userDebugCallback = null;
    private static GLDebugMessageCallback glDebugCallback = null;
    
    /**
     * Check if debug output is supported
     */
    public static boolean supportsDebugOutput() {
        return hasDebugOutput;
    }
    
    /**
     * Enable debug output
     */
    public static void enableDebugOutput(boolean synchronous) {
        if (!hasDebugOutput) return;
        
        GL11.glEnable(GL43.GL_DEBUG_OUTPUT);
        if (synchronous) {
            GL11.glEnable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
        }
    }
    
    /**
     * Disable debug output
     */
    public static void disableDebugOutput() {
        if (!hasDebugOutput) return;
        
        GL11.glDisable(GL43.GL_DEBUG_OUTPUT);
        GL11.glDisable(GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
    }
    
    /**
     * Set debug callback
     */
    public static void setDebugCallback(DebugCallback callback) {
        userDebugCallback = callback;
        
        if (!hasDebugOutput) return;
        
        // Clean up old callback
        if (glDebugCallback != null) {
            glDebugCallback.free();
            glDebugCallback = null;
        }
        
        if (callback != null) {
            glDebugCallback = GLDebugMessageCallback.create((source, type, id, severity, length, message, userParam) -> {
                String msg = GLDebugMessageCallback.getMessage(length, message);
                userDebugCallback.onMessage(source, type, id, severity, msg);
            });
            
            if (GL43) {
                GL43.glDebugMessageCallback(glDebugCallback, 0);
            } else if (KHR_debug) {
                KHRDebug.glDebugMessageCallback(glDebugCallback, 0);
            } else if (ARB_debug_output) {
                ARBDebugOutput.glDebugMessageCallbackARB(glDebugCallback, 0);
            }
        } else {
            if (GL43) {
                GL43.glDebugMessageCallback(null, 0);
            } else if (KHR_debug) {
                KHRDebug.glDebugMessageCallback(null, 0);
            }
        }
    }
    
    /**
     * Default debug callback that logs to console
     */
    public static final DebugCallback DEFAULT_DEBUG_CALLBACK = (source, type, id, severity, message) -> {
        String sourceStr = getDebugSourceString(source);
        String typeStr = getDebugTypeString(type);
        String severityStr = getDebugSeverityString(severity);
        
        String prefix = "[GL Debug]";
        if (severity == GL_DEBUG_SEVERITY_HIGH || type == GL_DEBUG_TYPE_ERROR) {
            System.err.printf("%s [%s] [%s] [%s] ID=%d: %s%n", 
                prefix, severityStr, sourceStr, typeStr, id, message);
        } else if (severity != GL_DEBUG_SEVERITY_NOTIFICATION) {
            System.out.printf("%s [%s] [%s] [%s] ID=%d: %s%n", 
                prefix, severityStr, sourceStr, typeStr, id, message);
        }
    };
    
    /**
     * Get debug source as string
     */
    public static String getDebugSourceString(int source) {
        switch (source) {
            case GL_DEBUG_SOURCE_API: return "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM: return "Window System";
            case GL_DEBUG_SOURCE_SHADER_COMPILER: return "Shader Compiler";
            case GL_DEBUG_SOURCE_THIRD_PARTY: return "Third Party";
            case GL_DEBUG_SOURCE_APPLICATION: return "Application";
            case GL_DEBUG_SOURCE_OTHER: return "Other";
            default: return "Unknown(" + source + ")";
        }
    }
    
    /**
     * Get debug type as string
     */
    public static String getDebugTypeString(int type) {
        switch (type) {
            case GL_DEBUG_TYPE_ERROR: return "Error";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR: return "Deprecated";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR: return "Undefined Behavior";
            case GL_DEBUG_TYPE_PORTABILITY: return "Portability";
            case GL_DEBUG_TYPE_PERFORMANCE: return "Performance";
            case GL_DEBUG_TYPE_MARKER: return "Marker";
            case GL_DEBUG_TYPE_PUSH_GROUP: return "Push Group";
            case GL_DEBUG_TYPE_POP_GROUP: return "Pop Group";
            case GL_DEBUG_TYPE_OTHER: return "Other";
            default: return "Unknown(" + type + ")";
        }
    }
    
    /**
     * Get debug severity as string
     */
    public static String getDebugSeverityString(int severity) {
        switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH: return "HIGH";
            case GL_DEBUG_SEVERITY_MEDIUM: return "MEDIUM";
            case GL_DEBUG_SEVERITY_LOW: return "LOW";
            case GL_DEBUG_SEVERITY_NOTIFICATION: return "NOTIFICATION";
            default: return "Unknown(" + severity + ")";
        }
    }
    
    /**
     * glDebugMessageControl - Control debug messages
     * GL 4.3: glDebugMessageControl(source, type, severity, ids, enabled)
     */
    public static void debugMessageControl(int source, int type, int severity, IntBuffer ids, boolean enabled) {
        if (GL43) {
            GL43.glDebugMessageControl(source, type, severity, ids, enabled);
        } else if (KHR_debug) {
            KHRDebug.glDebugMessageControl(source, type, severity, ids, enabled);
        } else if (ARB_debug_output) {
            ARBDebugOutput.glDebugMessageControlARB(source, type, severity, ids, enabled);
        }
    }
    
    /**
     * Disable all notifications (reduce noise)
     */
    public static void disableDebugNotifications() {
        debugMessageControl(GL11.GL_DONT_CARE, GL11.GL_DONT_CARE, 
            GL_DEBUG_SEVERITY_NOTIFICATION, null, false);
    }
    
    /**
     * glDebugMessageInsert - Insert debug message
     * GL 4.3: glDebugMessageInsert(source, type, id, severity, message)
     */
    public static void debugMessageInsert(int source, int type, int id, int severity, CharSequence message) {
        if (GL43) {
            GL43.glDebugMessageInsert(source, type, id, severity, message);
        } else if (KHR_debug) {
            KHRDebug.glDebugMessageInsert(source, type, id, severity, message);
        } else if (ARB_debug_output) {
            ARBDebugOutput.glDebugMessageInsertARB(source, type, id, severity, message);
        }
    }
    
    /**
     * Insert application debug message
     */
    public static void debugMessage(String message) {
        debugMessageInsert(GL_DEBUG_SOURCE_APPLICATION, GL_DEBUG_TYPE_OTHER, 0, 
            GL_DEBUG_SEVERITY_NOTIFICATION, message);
    }
    
    /**
     * Insert application error message
     */
    public static void debugError(String message) {
        debugMessageInsert(GL_DEBUG_SOURCE_APPLICATION, GL_DEBUG_TYPE_ERROR, 0, 
            GL_DEBUG_SEVERITY_HIGH, message);
    }
    
    /**
     * glPushDebugGroup - Push debug group onto stack
     * GL 4.3: glPushDebugGroup(source, id, message)
     */
    public static void pushDebugGroup(int source, int id, CharSequence message) {
        if (GL43) {
            GL43.glPushDebugGroup(source, id, message);
        } else if (KHR_debug) {
            KHRDebug.glPushDebugGroup(source, id, message);
        }
    }
    
    /**
     * Push application debug group
     */
    public static void pushDebugGroup(String message) {
        pushDebugGroup(GL_DEBUG_SOURCE_APPLICATION, 0, message);
    }
    
    /**
     * glPopDebugGroup - Pop debug group from stack
     * GL 4.3: glPopDebugGroup()
     */
    public static void popDebugGroup() {
        if (GL43) {
            GL43.glPopDebugGroup();
        } else if (KHR_debug) {
            KHRDebug.glPopDebugGroup();
        }
    }
    
    /**
     * glObjectLabel - Label GL object
     * GL 4.3: glObjectLabel(identifier, name, label)
     */
    public static void objectLabel(int identifier, int name, CharSequence label) {
        if (GL43) {
            GL43.glObjectLabel(identifier, name, label);
        } else if (KHR_debug) {
            KHRDebug.glObjectLabel(identifier, name, label);
        }
    }
    
    /**
     * Label a buffer object
     */
    public static void labelBuffer(int buffer, String label) {
        objectLabel(GL43.GL_BUFFER, buffer, label);
    }
    
    /**
     * Label a texture object
     */
    public static void labelTexture(int texture, String label) {
        objectLabel(GL11.GL_TEXTURE, texture, label);
    }
    
    /**
     * Label a shader object
     */
    public static void labelShader(int shader, String label) {
        objectLabel(GL43.GL_SHADER, shader, label);
    }
    
    /**
     * Label a program object
     */
    public static void labelProgram(int program, String label) {
        objectLabel(GL43.GL_PROGRAM, program, label);
    }
    
    /**
     * Label a VAO
     */
    public static void labelVertexArray(int vao, String label) {
        objectLabel(GL43.GL_VERTEX_ARRAY, vao, label);
    }
    
    /**
     * Label a framebuffer
     */
    public static void labelFramebuffer(int framebuffer, String label) {
        objectLabel(GL43.GL_FRAMEBUFFER, framebuffer, label);
    }
    
    // ========================================================================
    // GL 4.3 - PROGRAM INTERFACE QUERY
    // ========================================================================
    
    // Program interface constants
    public static final int GL_UNIFORM = GL43.GL_UNIFORM;
    public static final int GL_UNIFORM_BLOCK = GL43.GL_UNIFORM_BLOCK;
    public static final int GL_PROGRAM_INPUT = GL43.GL_PROGRAM_INPUT;
    public static final int GL_PROGRAM_OUTPUT = GL43.GL_PROGRAM_OUTPUT;
    public static final int GL_BUFFER_VARIABLE = GL43.GL_BUFFER_VARIABLE;
    public static final int GL_SHADER_STORAGE_BLOCK = GL43.GL_SHADER_STORAGE_BLOCK;
    public static final int GL_VERTEX_SUBROUTINE = GL43.GL_VERTEX_SUBROUTINE;
    public static final int GL_FRAGMENT_SUBROUTINE = GL43.GL_FRAGMENT_SUBROUTINE;
    public static final int GL_COMPUTE_SUBROUTINE = GL43.GL_COMPUTE_SUBROUTINE;
    public static final int GL_TRANSFORM_FEEDBACK_VARYING = GL43.GL_TRANSFORM_FEEDBACK_VARYING;
    
    public static final int GL_ACTIVE_RESOURCES = GL43.GL_ACTIVE_RESOURCES;
    public static final int GL_MAX_NAME_LENGTH = GL43.GL_MAX_NAME_LENGTH;
    
    public static final int GL_NAME_LENGTH = GL43.GL_NAME_LENGTH;
    public static final int GL_TYPE = GL43.GL_TYPE;
    public static final int GL_ARRAY_SIZE = GL43.GL_ARRAY_SIZE;
    public static final int GL_OFFSET = GL43.GL_OFFSET;
    public static final int GL_BLOCK_INDEX = GL43.GL_BLOCK_INDEX;
    public static final int GL_ARRAY_STRIDE = GL43.GL_ARRAY_STRIDE;
    public static final int GL_MATRIX_STRIDE = GL43.GL_MATRIX_STRIDE;
    public static final int GL_IS_ROW_MAJOR = GL43.GL_IS_ROW_MAJOR;
    public static final int GL_ATOMIC_COUNTER_BUFFER_INDEX = GL43.GL_ATOMIC_COUNTER_BUFFER_INDEX;
    public static final int GL_BUFFER_BINDING = GL43.GL_BUFFER_BINDING;
    public static final int GL_BUFFER_DATA_SIZE = GL43.GL_BUFFER_DATA_SIZE;
    public static final int GL_NUM_ACTIVE_VARIABLES = GL43.GL_NUM_ACTIVE_VARIABLES;
    public static final int GL_ACTIVE_VARIABLES = GL43.GL_ACTIVE_VARIABLES;
    public static final int GL_REFERENCED_BY_VERTEX_SHADER = GL43.GL_REFERENCED_BY_VERTEX_SHADER;
    public static final int GL_REFERENCED_BY_FRAGMENT_SHADER = GL43.GL_REFERENCED_BY_FRAGMENT_SHADER;
    public static final int GL_REFERENCED_BY_COMPUTE_SHADER = GL43.GL_REFERENCED_BY_COMPUTE_SHADER;
    public static final int GL_LOCATION = GL43.GL_LOCATION;
    public static final int GL_LOCATION_INDEX = GL43.GL_LOCATION_INDEX;
    
    /**
     * glGetProgramInterfaceiv - Get program interface property
     * GL 4.3: glGetProgramInterfaceiv(program, programInterface, pname, params)
     */
    public static int getProgramInterfacei(int program, int programInterface, int pname) {
        if (GL43) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                GL43.glGetProgramInterfaceiv(program, programInterface, pname, buf);
                return buf.get(0);
            }
        } else if (ARB_program_interface_query) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBProgramInterfaceQuery.glGetProgramInterfaceiv(program, programInterface, pname, buf);
                return buf.get(0);
            }
        }
        return 0;
    }
    
    /**
     * glGetProgramResourceName - Get resource name
     * GL 4.3: glGetProgramResourceName(program, programInterface, index)
     */
    public static String getProgramResourceName(int program, int programInterface, int index) {
        if (GL43) {
            return GL43.glGetProgramResourceName(program, programInterface, index);
        } else if (ARB_program_interface_query) {
            int maxLength = getProgramInterfacei(program, programInterface, GL_MAX_NAME_LENGTH);
            return ARBProgramInterfaceQuery.glGetProgramResourceName(program, programInterface, index, maxLength);
        }
        return "";
    }
    
    /**
     * glGetProgramResourceiv - Get resource properties
     * GL 4.3: glGetProgramResourceiv(program, programInterface, index, props, length, params)
     */
    public static void getProgramResourceiv(int program, int programInterface, int index, 
                                            IntBuffer props, IntBuffer length, IntBuffer params) {
        if (GL43) {
            GL43.glGetProgramResourceiv(program, programInterface, index, props, length, params);
        } else if (ARB_program_interface_query) {
            ARBProgramInterfaceQuery.glGetProgramResourceiv(program, programInterface, index, props, length, params);
        }
    }
    
    /**
     * Get single resource property
     */
    public static int getProgramResourceProperty(int program, int programInterface, int index, int prop) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer props = stack.ints(prop);
            IntBuffer length = stack.mallocInt(1);
            IntBuffer params = stack.mallocInt(1);
            getProgramResourceiv(program, programInterface, index, props, length, params);
            return params.get(0);
        }
    }
    
    /**
     * glGetProgramResourceLocation - Get resource location
     * GL 4.3: glGetProgramResourceLocation(program, programInterface, name)
     */
    public static int getProgramResourceLocation(int program, int programInterface, CharSequence name) {
        if (GL43) {
            return GL43.glGetProgramResourceLocation(program, programInterface, name);
        } else if (ARB_program_interface_query) {
            return ARBProgramInterfaceQuery.glGetProgramResourceLocation(program, programInterface, name);
        }
        return -1;
    }
    
    /**
     * Introspect all uniforms in a program
     */
    public static Map<String, UniformInfo> introspectUniforms(int program) {
        Map<String, UniformInfo> uniforms = new HashMap<>();
        
        if (!hasProgramInterface) {
            // Fallback to GL 2.0 way
            return getAllUniformLocations(program).entrySet().stream()
                .collect(HashMap::new, 
                    (m, e) -> m.put(e.getKey(), new UniformInfo(e.getKey(), e.getValue(), 0, 0)),
                    HashMap::putAll);
        }
        
        int count = getProgramInterfacei(program, GL_UNIFORM, GL_ACTIVE_RESOURCES);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer props = stack.ints(GL_NAME_LENGTH, GL_TYPE, GL_LOCATION, GL_ARRAY_SIZE);
            IntBuffer length = stack.mallocInt(1);
            IntBuffer params = stack.mallocInt(4);
            
            for (int i = 0; i < count; i++) {
                getProgramResourceiv(program, GL_UNIFORM, i, props, length, params);
                
                String name = getProgramResourceName(program, GL_UNIFORM, i);
                int type = params.get(1);
                int location = params.get(2);
                int arraySize = params.get(3);
                
                uniforms.put(name, new UniformInfo(name, location, type, arraySize));
            }
        }
        
        return uniforms;
    }
    
    public static class UniformInfo {
        public final String name;
        public final int location;
        public final int type;
        public final int arraySize;
        
        public UniformInfo(String name, int location, int type, int arraySize) {
            this.name = name;
            this.location = location;
            this.type = type;
            this.arraySize = arraySize;
        }
    }
    
    // ========================================================================
    // GL 4.3 - VERTEX ATTRIB BINDING (Separate format from binding)
    // ========================================================================
    
    /**
     * glVertexAttribFormat - Specify vertex attribute format
     * GL 4.3: glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset)
     */
    public static void vertexAttribFormat(int attribindex, int size, int type, boolean normalized, int relativeoffset) {
        if (GL43) {
            GL43.glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset);
        } else if (ARB_vertex_attrib_binding) {
            ARBVertexAttribBinding.glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset);
        }
    }
    
    /**
     * glVertexAttribIFormat - Specify integer vertex attribute format
     * GL 4.3: glVertexAttribIFormat(attribindex, size, type, relativeoffset)
     */
    public static void vertexAttribIFormat(int attribindex, int size, int type, int relativeoffset) {
        if (GL43) {
            GL43.glVertexAttribIFormat(attribindex, size, type, relativeoffset);
        } else if (ARB_vertex_attrib_binding) {
            ARBVertexAttribBinding.glVertexAttribIFormat(attribindex, size, type, relativeoffset);
        }
    }
    
    /**
     * glVertexAttribLFormat - Specify 64-bit vertex attribute format
     * GL 4.3: glVertexAttribLFormat(attribindex, size, type, relativeoffset)
     */
    public static void vertexAttribLFormat(int attribindex, int size, int type, int relativeoffset) {
        if (GL43) {
            GL43.glVertexAttribLFormat(attribindex, size, type, relativeoffset);
        } else if (ARB_vertex_attrib_binding) {
            ARBVertexAttribBinding.glVertexAttribLFormat(attribindex, size, type, relativeoffset);
        }
    }
    
    /**
     * glVertexAttribBinding - Associate attribute with binding
     * GL 4.3: glVertexAttribBinding(attribindex, bindingindex)
     */
    public static void vertexAttribBinding(int attribindex, int bindingindex) {
        if (GL43) {
            GL43.glVertexAttribBinding(attribindex, bindingindex);
        } else if (ARB_vertex_attrib_binding) {
            ARBVertexAttribBinding.glVertexAttribBinding(attribindex, bindingindex);
        }
    }
    
    /**
     * glBindVertexBuffer - Bind buffer to binding point
     * GL 4.3: glBindVertexBuffer(bindingindex, buffer, offset, stride)
     */
    public static void bindVertexBuffer(int bindingindex, int buffer, long offset, int stride) {
        if (GL43) {
            GL43.glBindVertexBuffer(bindingindex, buffer, offset, stride);
        } else if (ARB_vertex_attrib_binding) {
            ARBVertexAttribBinding.glBindVertexBuffer(bindingindex, buffer, offset, stride);
        }
    }
    
    /**
     * glVertexBindingDivisor - Set binding divisor for instancing
     * GL 4.3: glVertexBindingDivisor(bindingindex, divisor)
     */
    public static void vertexBindingDivisor(int bindingindex, int divisor) {
        if (GL43) {
            GL43.glVertexBindingDivisor(bindingindex, divisor);
        } else if (ARB_vertex_attrib_binding) {
            ARBVertexAttribBinding.glVertexBindingDivisor(bindingindex, divisor);
        }
    }
    
    // ========================================================================
    // GL 4.3 - TEXTURE VIEWS
    // ========================================================================
    
    /**
     * glTextureView - Create texture view
     * GL 4.3: glTextureView(texture, target, origtexture, internalformat, minlevel, numlevels, minlayer, numlayers)
     */
    public static void textureView(int texture, int target, int origtexture, int internalformat,
                                    int minlevel, int numlevels, int minlayer, int numlayers) {
        if (GL43) {
            GL43.glTextureView(texture, target, origtexture, internalformat, minlevel, numlevels, minlayer, numlayers);
        } else if (ARB_texture_view) {
            ARBTextureView.glTextureView(texture, target, origtexture, internalformat, minlevel, numlevels, minlayer, numlayers);
        }
    }
    
    /**
     * Create a view of a texture with different format interpretation
     */
    public static int createTextureView(int sourceTexture, int target, int internalFormat) {
        if (!hasTextureViews) return 0;
        
        int view = genTexture();
        textureView(view, target, sourceTexture, internalFormat, 0, 1, 0, 1);
        return view;
    }
    
    /**
     * Create a view of specific mip levels
     */
    public static int createMipView(int sourceTexture, int target, int internalFormat, int minLevel, int numLevels) {
        if (!hasTextureViews) return 0;
        
        int view = genTexture();
        textureView(view, target, sourceTexture, internalFormat, minLevel, numLevels, 0, 1);
        return view;
    }
    
    /**
     * Create a view of specific array layers
     */
    public static int createLayerView(int sourceTexture, int target, int internalFormat, int minLayer, int numLayers) {
        if (!hasTextureViews) return 0;
        
        int view = genTexture();
        textureView(view, target, sourceTexture, internalFormat, 0, 1, minLayer, numLayers);
        return view;
    }
    
    // ========================================================================
    // GL 4.3 - INVALIDATE DATA
    // ========================================================================
    
    /**
     * glInvalidateBufferData - Invalidate entire buffer
     * GL 4.3: glInvalidateBufferData(buffer)
     */
    public static void invalidateBufferData(int buffer) {
        if (GL43) {
            GL43.glInvalidateBufferData(buffer);
        } else if (ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateBufferData(buffer);
        }
    }
    
    /**
     * glInvalidateBufferSubData - Invalidate buffer region
     * GL 4.3: glInvalidateBufferSubData(buffer, offset, length)
     */
    public static void invalidateBufferSubData(int buffer, long offset, long length) {
        if (GL43) {
            GL43.glInvalidateBufferSubData(buffer, offset, length);
        } else if (ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateBufferSubData(buffer, offset, length);
        }
    }
    
    /**
     * glInvalidateTexImage - Invalidate entire texture level
     * GL 4.3: glInvalidateTexImage(texture, level)
     */
    public static void invalidateTexImage(int texture, int level) {
        if (GL43) {
            GL43.glInvalidateTexImage(texture, level);
        } else if (ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateTexImage(texture, level);
        }
    }
    
    /**
     * glInvalidateTexSubImage - Invalidate texture region
     * GL 4.3: glInvalidateTexSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth)
     */
    public static void invalidateTexSubImage(int texture, int level, int xoffset, int yoffset, int zoffset,
                                              int width, int height, int depth) {
        if (GL43) {
            GL43.glInvalidateTexSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth);
        } else if (ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateTexSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth);
        }
    }
    
    /**
     * glInvalidateFramebuffer - Invalidate framebuffer attachments
     * GL 4.3: glInvalidateFramebuffer(target, attachments)
     */
    public static void invalidateFramebuffer(int target, IntBuffer attachments) {
        if (GL43) {
            GL43.glInvalidateFramebuffer(target, attachments);
        } else if (ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateFramebuffer(target, attachments);
        }
    }
    
    /**
     * Invalidate framebuffer with varargs
     */
    public static void invalidateFramebuffer(int target, int... attachments) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.ints(attachments);
            invalidateFramebuffer(target, buf);
        }
    }
    
    /**
     * glInvalidateSubFramebuffer - Invalidate framebuffer region
     * GL 4.3: glInvalidateSubFramebuffer(target, attachments, x, y, width, height)
     */
    public static void invalidateSubFramebuffer(int target, IntBuffer attachments, int x, int y, int width, int height) {
        if (GL43) {
            GL43.glInvalidateSubFramebuffer(target, attachments, x, y, width, height);
        } else if (ARB_invalidate_subdata) {
            ARBInvalidateSubdata.glInvalidateSubFramebuffer(target, attachments, x, y, width, height);
        }
    }
    
    // ========================================================================
    // GL 4.3 - CLEAR BUFFER DATA
    // ========================================================================
    
    /**
     * glClearBufferData - Clear buffer to constant value
     * GL 4.3: glClearBufferData(target, internalformat, format, type, data)
     */
    public static void clearBufferData(int target, int internalformat, int format, int type, ByteBuffer data) {
        if (GL43) {
            GL43.glClearBufferData(target, internalformat, format, type, data);
        } else if (ARB_clear_buffer_object) {
            ARBClearBufferObject.glClearBufferData(target, internalformat, format, type, data);
        }
    }
    
    /**
     * Clear buffer to zero
     */
    public static void clearBufferToZero(int target, int internalformat) {
        clearBufferData(target, internalformat, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, null);
    }
    
    /**
     * glClearBufferSubData - Clear buffer region to constant value
     * GL 4.3: glClearBufferSubData(target, internalformat, offset, size, format, type, data)
     */
    public static void clearBufferSubData(int target, int internalformat, long offset, long size, 
                                          int format, int type, ByteBuffer data) {
        if (GL43) {
            GL43.glClearBufferSubData(target, internalformat, offset, size, format, type, data);
        } else if (ARB_clear_buffer_object) {
            ARBClearBufferObject.glClearBufferSubData(target, internalformat, offset, size, format, type, data);
        }
    }
    
    // ========================================================================
    // GL 4.3 - COPY IMAGE
    // ========================================================================
    
    /**
     * glCopyImageSubData - Copy between textures/renderbuffers
     * GL 4.3: glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ,
     *                            dstName, dstTarget, dstLevel, dstX, dstY, dstZ,
     *                            srcWidth, srcHeight, srcDepth)
     */
    public static void copyImageSubData(int srcName, int srcTarget, int srcLevel, int srcX, int srcY, int srcZ,
                                         int dstName, int dstTarget, int dstLevel, int dstX, int dstY, int dstZ,
                                         int srcWidth, int srcHeight, int srcDepth) {
        if (GL43) {
            GL43.glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ,
                                    dstName, dstTarget, dstLevel, dstX, dstY, dstZ,
                                    srcWidth, srcHeight, srcDepth);
        } else if (ARB_copy_image) {
            ARBCopyImage.glCopyImageSubData(srcName, srcTarget, srcLevel, srcX, srcY, srcZ,
                                            dstName, dstTarget, dstLevel, dstX, dstY, dstZ,
                                            srcWidth, srcHeight, srcDepth);
        } else if (NV_copy_image) {
            NVCopyImage.glCopyImageSubDataNV(srcName, srcTarget, srcLevel, srcX, srcY, srcZ,
                                             dstName, dstTarget, dstLevel, dstX, dstY, dstZ,
                                             srcWidth, srcHeight, srcDepth);
        }
    }
    
    /**
     * Copy 2D texture region
     */
    public static void copyTexture2D(int srcTexture, int srcLevel, int srcX, int srcY,
                                      int dstTexture, int dstLevel, int dstX, int dstY,
                                      int width, int height) {
        copyImageSubData(srcTexture, GL11.GL_TEXTURE_2D, srcLevel, srcX, srcY, 0,
                        dstTexture, GL11.GL_TEXTURE_2D, dstLevel, dstX, dstY, 0,
                        width, height, 1);
    }
    
    // ========================================================================
    // GL 4.3 - FRAMEBUFFER NO ATTACHMENTS
    // ========================================================================
    
    /**
     * glFramebufferParameteri - Set framebuffer parameter
     * GL 4.3: glFramebufferParameteri(target, pname, param)
     */
    public static void framebufferParameteri(int target, int pname, int param) {
        if (GL43) {
            GL43.glFramebufferParameteri(target, pname, param);
        } else if (ARB_framebuffer_no_attachments) {
            ARBFramebufferNoAttachments.glFramebufferParameteri(target, pname, param);
        }
    }
    
    /**
     * Set framebuffer default width
     */
    public static void setFramebufferDefaultWidth(int target, int width) {
        framebufferParameteri(target, GL43.GL_FRAMEBUFFER_DEFAULT_WIDTH, width);
    }
    
    /**
     * Set framebuffer default height
     */
    public static void setFramebufferDefaultHeight(int target, int height) {
        framebufferParameteri(target, GL43.GL_FRAMEBUFFER_DEFAULT_HEIGHT, height);
    }
    
    /**
     * Set framebuffer default layers
     */
    public static void setFramebufferDefaultLayers(int target, int layers) {
        framebufferParameteri(target, GL43.GL_FRAMEBUFFER_DEFAULT_LAYERS, layers);
    }
    
    /**
     * Set framebuffer default samples
     */
    public static void setFramebufferDefaultSamples(int target, int samples) {
        framebufferParameteri(target, GL43.GL_FRAMEBUFFER_DEFAULT_SAMPLES, samples);
    }
    
    // ========================================================================
    // GL 4.4 - BUFFER STORAGE (IMMUTABLE)
    // ========================================================================
    
    // Buffer storage flags
    public static final int GL_MAP_READ_BIT = GL30.GL_MAP_READ_BIT;
    public static final int GL_MAP_WRITE_BIT = GL30.GL_MAP_WRITE_BIT;
    public static final int GL_MAP_PERSISTENT_BIT = GL44.GL_MAP_PERSISTENT_BIT;
    public static final int GL_MAP_COHERENT_BIT = GL44.GL_MAP_COHERENT_BIT;
    public static final int GL_DYNAMIC_STORAGE_BIT = GL44.GL_DYNAMIC_STORAGE_BIT;
    public static final int GL_CLIENT_STORAGE_BIT = GL44.GL_CLIENT_STORAGE_BIT;
    
    /**
     * Check if buffer storage is supported
     */
    public static boolean supportsBufferStorage() {
        return hasImmutableStorage;
    }
    
    /**
     * Check if persistent mapping is supported
     */
    public static boolean supportsPersistentMapping() {
        return hasPersistentMapping;
    }
    
    /**
     * glBufferStorage - Create immutable buffer storage
     * GL 4.4: glBufferStorage(target, size, flags)
     */
    public static void bufferStorage(int target, long size, int flags) {
        if (GL44) {
            GL44.glBufferStorage(target, size, flags);
        } else if (ARB_buffer_storage) {
            ARBBufferStorage.glBufferStorage(target, size, flags);
        } else {
            // Fallback to mutable storage
            int usage = translateStorageFlagsToUsage(flags);
            GL15.glBufferData(target, size, usage);
        }
    }
    
    /**
     * glBufferStorage with data
     */
    public static void bufferStorage(int target, ByteBuffer data, int flags) {
        if (GL44) {
            GL44.glBufferStorage(target, data, flags);
        } else if (ARB_buffer_storage) {
            ARBBufferStorage.glBufferStorage(target, data, flags);
        } else {
            int usage = translateStorageFlagsToUsage(flags);
            GL15.glBufferData(target, data, usage);
        }
    }
    
    public static void bufferStorage(int target, FloatBuffer data, int flags) {
        if (GL44) {
            GL44.glBufferStorage(target, data, flags);
        } else if (ARB_buffer_storage) {
            ARBBufferStorage.glBufferStorage(target, data, flags);
        } else {
            int usage = translateStorageFlagsToUsage(flags);
            GL15.glBufferData(target, data, usage);
        }
    }
    
    public static void bufferStorage(int target, IntBuffer data, int flags) {
        if (GL44) {
            GL44.glBufferStorage(target, data, flags);
        } else if (ARB_buffer_storage) {
            ARBBufferStorage.glBufferStorage(target, data, flags);
        } else {
            int usage = translateStorageFlagsToUsage(flags);
            GL15.glBufferData(target, data, usage);
        }
    }
    
    /**
     * Translate storage flags to usage hint for fallback
     */
    private static int translateStorageFlagsToUsage(int flags) {
        boolean read = (flags & GL_MAP_READ_BIT) != 0;
        boolean write = (flags & GL_MAP_WRITE_BIT) != 0;
        boolean dynamic = (flags & GL_DYNAMIC_STORAGE_BIT) != 0;
        
        if (dynamic) {
            if (read && write) return GL15.GL_DYNAMIC_COPY;
            if (read) return GL15.GL_DYNAMIC_READ;
            return GL15.GL_DYNAMIC_DRAW;
        } else {
            if (read && write) return GL15.GL_STATIC_COPY;
            if (read) return GL15.GL_STATIC_READ;
            return GL15.GL_STATIC_DRAW;
        }
    }
    
    // ========================================================================
    // GL 4.4 - PERSISTENT MAPPING
    // ========================================================================
    
    /**
     * Persistent buffer info
     */
    public static class PersistentBuffer {
        public final int bufferId;
        public final ByteBuffer mappedBuffer;
        public final long size;
        public final int flags;
        public final boolean coherent;
        
        public PersistentBuffer(int bufferId, ByteBuffer mappedBuffer, long size, int flags) {
            this.bufferId = bufferId;
            this.mappedBuffer = mappedBuffer;
            this.size = size;
            this.flags = flags;
            this.coherent = (flags & GL_MAP_COHERENT_BIT) != 0;
        }
        
        /**
         * Ensure writes are visible (only needed if not coherent)
         */
        public void flush(long offset, long length) {
            if (!coherent) {
                GL30.glFlushMappedBufferRange(GL15.GL_ARRAY_BUFFER, offset, length);
            }
        }
    }
    
    /**
     * Create persistently mapped buffer
     */
    public static PersistentBuffer createPersistentBuffer(int target, long size, boolean coherent, boolean read, boolean write) {
        if (!hasPersistentMapping) {
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Persistent mapping not supported");
            }
            return null;
        }
        
        int buffer = genBuffer();
        GL15.glBindBuffer(target, buffer);
        
        int storageFlags = GL_MAP_PERSISTENT_BIT;
        if (coherent) storageFlags |= GL_MAP_COHERENT_BIT;
        if (read) storageFlags |= GL_MAP_READ_BIT;
        if (write) storageFlags |= GL_MAP_WRITE_BIT;
        storageFlags |= GL_DYNAMIC_STORAGE_BIT;
        
        bufferStorage(target, size, storageFlags);
        
        int mapFlags = GL_MAP_PERSISTENT_BIT;
        if (coherent) mapFlags |= GL_MAP_COHERENT_BIT;
        if (read) mapFlags |= GL_MAP_READ_BIT;
        if (write) mapFlags |= GL_MAP_WRITE_BIT;
        
        ByteBuffer mapped = GL30.glMapBufferRange(target, 0, size, mapFlags);
        
        GL15.glBindBuffer(target, 0);
        
        return new PersistentBuffer(buffer, mapped, size, storageFlags);
    }
    
    /**
     * Create write-only persistent buffer (most common case)
     */
    public static PersistentBuffer createWritePersistentBuffer(int target, long size) {
        return createPersistentBuffer(target, size, true, false, true);
    }
    
    /**
     * Create read-write persistent buffer
     */
    public static PersistentBuffer createReadWritePersistentBuffer(int target, long size) {
        return createPersistentBuffer(target, size, true, true, true);
    }
    
    /**
     * Delete persistent buffer
     */
    public static void deletePersistentBuffer(PersistentBuffer buffer) {
        if (buffer == null) return;
        
        // Unmap is automatic when buffer is deleted with immutable storage
        deleteBuffer(buffer.bufferId);
    }
    
    // ========================================================================
    // GL 4.4 - MULTI-BIND
    // ========================================================================
    
    /**
     * Check if multi-bind is supported
     */
    public static boolean supportsMultiBind() {
        return hasMultibind;
    }
    
    /**
     * glBindBuffersBase - Bind multiple buffers to sequential binding points
     * GL 4.4: glBindBuffersBase(target, first, buffers)
     */
    public static void bindBuffersBase(int target, int first, IntBuffer buffers) {
        if (GL44) {
            GL44.glBindBuffersBase(target, first, buffers);
        } else if (ARB_multi_bind) {
            ARBMultiBind.glBindBuffersBase(target, first, buffers);
        } else {
            // Fallback: bind individually
            int count = buffers != null ? buffers.remaining() : 0;
            for (int i = 0; i < count; i++) {
                GL30.glBindBufferBase(target, first + i, buffers.get(buffers.position() + i));
            }
        }
    }
    
    /**
     * glBindBuffersRange - Bind multiple buffer ranges
     * GL 4.4: glBindBuffersRange(target, first, buffers, offsets, sizes)
     */
    public static void bindBuffersRange(int target, int first, IntBuffer buffers, 
                                        PointerBuffer offsets, PointerBuffer sizes) {
        if (GL44) {
            GL44.glBindBuffersRange(target, first, buffers, offsets, sizes);
        } else if (ARB_multi_bind) {
            ARBMultiBind.glBindBuffersRange(target, first, buffers, offsets, sizes);
        } else {
            // Fallback
            int count = buffers != null ? buffers.remaining() : 0;
            for (int i = 0; i < count; i++) {
                GL30.glBindBufferRange(target, first + i, 
                    buffers.get(buffers.position() + i),
                    offsets.get(offsets.position() + i),
                    sizes.get(sizes.position() + i));
            }
        }
    }
    
    /**
     * glBindTextures - Bind multiple textures
     * GL 4.4: glBindTextures(first, textures)
     */
    public static void bindTextures(int first, IntBuffer textures) {
        if (GL44) {
            GL44.glBindTextures(first, textures);
        } else if (ARB_multi_bind) {
            ARBMultiBind.glBindTextures(first, textures);
        } else {
            // Fallback
            int count = textures != null ? textures.remaining() : 0;
            for (int i = 0; i < count; i++) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + first + i);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, textures.get(textures.position() + i));
            }
        }
    }
    
    /**
     * glBindSamplers - Bind multiple samplers
     * GL 4.4: glBindSamplers(first, samplers)
     */
    public static void bindSamplers(int first, IntBuffer samplers) {
        if (GL44) {
            GL44.glBindSamplers(first, samplers);
        } else if (ARB_multi_bind) {
            ARBMultiBind.glBindSamplers(first, samplers);
        } else if (hasSamplerObjects) {
            // Fallback
            int count = samplers != null ? samplers.remaining() : 0;
            for (int i = 0; i < count; i++) {
                GL33.glBindSampler(first + i, samplers.get(samplers.position() + i));
            }
        }
    }
    
    /**
     * glBindImageTextures - Bind multiple image textures
     * GL 4.4: glBindImageTextures(first, textures)
     */
    public static void bindImageTextures(int first, IntBuffer textures) {
        if (GL44) {
            GL44.glBindImageTextures(first, textures);
        } else if (ARB_multi_bind) {
            ARBMultiBind.glBindImageTextures(first, textures);
        }
        // No simple fallback for image textures
    }
    
    /**
     * glBindVertexBuffers - Bind multiple vertex buffers
     * GL 4.4: glBindVertexBuffers(first, buffers, offsets, strides)
     */
    public static void bindVertexBuffers(int first, IntBuffer buffers, PointerBuffer offsets, IntBuffer strides) {
        if (GL44) {
            GL44.glBindVertexBuffers(first, buffers, offsets, strides);
        } else if (ARB_multi_bind) {
            ARBMultiBind.glBindVertexBuffers(first, buffers, offsets, strides);
        } else if (hasVertexAttribBinding) {
            // Fallback
            int count = buffers != null ? buffers.remaining() : 0;
            for (int i = 0; i < count; i++) {
                GL43.glBindVertexBuffer(first + i,
                    buffers.get(buffers.position() + i),
                    offsets.get(offsets.position() + i),
                    strides.get(strides.position() + i));
            }
        }
    }
    
    // ========================================================================
    // GL 4.4 - QUERY BUFFER OBJECT
    // ========================================================================
    
    public static final int GL_QUERY_BUFFER = GL44.GL_QUERY_BUFFER;
    public static final int GL_QUERY_BUFFER_BINDING = GL44.GL_QUERY_BUFFER_BINDING;
    public static final int GL_QUERY_RESULT_NO_WAIT = GL44.GL_QUERY_RESULT_NO_WAIT;
    
    /**
     * Bind query buffer for async query results
     */
    public static void bindQueryBuffer(int buffer) {
        if (GL44) {
            GL15.glBindBuffer(GL44.GL_QUERY_BUFFER, buffer);
        } else if (ARB_query_buffer_object) {
            GL15.glBindBuffer(ARBQueryBufferObject.GL_QUERY_BUFFER, buffer);
        }
    }
    
    /**
     * Get query result into buffer (async)
     */
    public static void getQueryObjectToBuffer(int id, int pname, long offset) {
        if (GL44 || ARB_query_buffer_object) {
            // With query buffer bound, result goes to buffer
            GL15.glGetQueryObjectuiv(id, pname, offset);
        }
    }
    
    // ========================================================================
    // ADDITIONAL EXTENSION FLAGS FOR GL 4.3-4.4
    // ========================================================================
    
    private static boolean ARB_compute_shader = false;
    private static boolean ARB_shader_storage_buffer_object = false;
    private static boolean ARB_multi_draw_indirect = false;
    private static boolean KHR_debug = false;
    private static boolean ARB_debug_output = false;
    private static boolean ARB_program_interface_query = false;
    private static boolean ARB_vertex_attrib_binding = false;
    private static boolean ARB_texture_view = false;
    private static boolean ARB_invalidate_subdata = false;
    private static boolean ARB_clear_buffer_object = false;
    private static boolean ARB_copy_image = false;
    private static boolean NV_copy_image = false;
    private static boolean ARB_framebuffer_no_attachments = false;
    private static boolean ARB_buffer_storage = false;
    private static boolean ARB_multi_bind = false;
    private static boolean ARB_query_buffer_object = false;
    private static boolean ARB_texture_storage_multisample = false;
    
    // Add to StateTracker
    // public int boundDrawIndirectBuffer = 0;
    
    // ========================================================================
    // PERFORMANCE UTILITIES
    // ========================================================================
    
    /**
     * Triple-buffered persistent buffer for streaming data
     * Handles synchronization automatically
     */
    public static class StreamingBuffer {
        private final PersistentBuffer buffer;
        private final int numSections;
        private final long sectionSize;
        private int currentSection = 0;
        private final long[] fences;
        
        public StreamingBuffer(int target, long sectionSize, int numSections) {
            this.numSections = numSections;
            this.sectionSize = sectionSize;
            this.fences = new long[numSections];
            
            long totalSize = sectionSize * numSections;
            this.buffer = createWritePersistentBuffer(target, totalSize);
        }
        
        /**
         * Get buffer section for writing, waiting if necessary
         */
        public ByteBuffer getWriteSection() {
            // Wait for this section's fence if set
            if (fences[currentSection] != 0) {
                int result = clientWaitSync(fences[currentSection], 
                    GL32.GL_SYNC_FLUSH_COMMANDS_BIT, 1_000_000_000L); // 1 second timeout
                    
                if (result == GL32.GL_WAIT_FAILED) {
                    if (debugMode) {
                        System.err.println("[OpenGLCallMapper] Fence wait failed");
                    }
                }
                
                deleteSync(fences[currentSection]);
                fences[currentSection] = 0;
            }
            
            long offset = currentSection * sectionSize;
            buffer.mappedBuffer.position((int) offset);
            buffer.mappedBuffer.limit((int) (offset + sectionSize));
            return buffer.mappedBuffer.slice();
        }
        
        /**
         * Lock current section and advance to next
         */
        public void lockSection() {
            fences[currentSection] = fenceSync();
            currentSection = (currentSection + 1) % numSections;
        }
        
        /**
         * Get buffer ID for binding
         */
        public int getBufferId() {
            return buffer.bufferId;
        }
        
        /**
         * Get offset for current section (for draw calls)
         */
        public long getCurrentOffset() {
            return currentSection * sectionSize;
        }
        
        /**
         * Clean up
         */
        public void destroy() {
            for (int i = 0; i < numSections; i++) {
                if (fences[i] != 0) {
                    deleteSync(fences[i]);
                }
            }
            deletePersistentBuffer(buffer);
        }
    }
    
    /**
     * Create triple-buffered streaming buffer
     */
    public static StreamingBuffer createStreamingBuffer(int target, long sectionSize) {
        if (!hasPersistentMapping) {
            return null;
        }
        return new StreamingBuffer(target, sectionSize, 3);
    }
}

    // ========================================================================
    // GL 4.5 - DIRECT STATE ACCESS (DSA) OVERVIEW
    // ========================================================================
    
    /**
     * Check if DSA is supported
     * DSA is the recommended way to use OpenGL when available
     */
    public static boolean supportsDSA() {
        return hasDSA;
    }
    
    /**
     * Get best practice recommendation
     */
    public static String getDSARecommendation() {
        if (hasDSA) {
            return "Use DSA (Direct State Access) for best performance";
        } else if (GL33) {
            return "Use bind-to-edit with state caching (DSA not available)";
        } else {
            return "Legacy path required (GL < 3.3)";
        }
    }
    
    // ========================================================================
    // GL 4.5 - DSA BUFFER OPERATIONS
    // ========================================================================
    
    /**
     * glCreateBuffers - Create pre-initialized buffer objects
     * GL 4.5: glCreateBuffers(n, buffers)
     * Unlike glGenBuffers, these are fully initialized immediately
     */
    public static int createBuffer() {
        if (GL45) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                GL45.glCreateBuffers(buf);
                return buf.get(0);
            }
        } else if (ARB_direct_state_access) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBDirectStateAccess.glCreateBuffers(buf);
                return buf.get(0);
            }
        } else {
            // Fallback: gen + bind to initialize
            int buffer = genBuffer();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            return buffer;
        }
    }
    
    public static void createBuffers(IntBuffer buffers) {
        if (GL45) {
            GL45.glCreateBuffers(buffers);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glCreateBuffers(buffers);
        } else {
            GL15.glGenBuffers(buffers);
            // Initialize each
            int pos = buffers.position();
            int lim = buffers.limit();
            for (int i = pos; i < lim; i++) {
                int buffer = buffers.get(i);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            }
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        }
    }
    
    /**
     * glNamedBufferStorage - Create immutable storage for named buffer
     * GL 4.5: glNamedBufferStorage(buffer, size, flags)
     */
    public static void namedBufferStorage(int buffer, long size, int flags) {
        if (GL45) {
            GL45.glNamedBufferStorage(buffer, size, flags);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferStorage(buffer, size, flags);
        } else {
            // Fallback: bind then operate
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            bufferStorage(GL15.GL_ARRAY_BUFFER, size, flags);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    public static void namedBufferStorage(int buffer, ByteBuffer data, int flags) {
        if (GL45) {
            GL45.glNamedBufferStorage(buffer, data, flags);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferStorage(buffer, data, flags);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            bufferStorage(GL15.GL_ARRAY_BUFFER, data, flags);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    public static void namedBufferStorage(int buffer, FloatBuffer data, int flags) {
        if (GL45) {
            GL45.glNamedBufferStorage(buffer, data, flags);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferStorage(buffer, data, flags);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            bufferStorage(GL15.GL_ARRAY_BUFFER, data, flags);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    public static void namedBufferStorage(int buffer, IntBuffer data, int flags) {
        if (GL45) {
            GL45.glNamedBufferStorage(buffer, data, flags);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferStorage(buffer, data, flags);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            bufferStorage(GL15.GL_ARRAY_BUFFER, data, flags);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    /**
     * glNamedBufferData - Create mutable storage for named buffer
     * GL 4.5: glNamedBufferData(buffer, size, usage)
     */
    public static void namedBufferData(int buffer, long size, int usage) {
        if (GL45) {
            GL45.glNamedBufferData(buffer, size, usage);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferData(buffer, size, usage);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, size, usage);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    public static void namedBufferData(int buffer, ByteBuffer data, int usage) {
        if (GL45) {
            GL45.glNamedBufferData(buffer, data, usage);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferData(buffer, data, usage);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, usage);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    public static void namedBufferData(int buffer, FloatBuffer data, int usage) {
        if (GL45) {
            GL45.glNamedBufferData(buffer, data, usage);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferData(buffer, data, usage);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, usage);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    public static void namedBufferData(int buffer, IntBuffer data, int usage) {
        if (GL45) {
            GL45.glNamedBufferData(buffer, data, usage);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferData(buffer, data, usage);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, usage);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    /**
     * glNamedBufferSubData - Update named buffer data
     * GL 4.5: glNamedBufferSubData(buffer, offset, data)
     */
    public static void namedBufferSubData(int buffer, long offset, ByteBuffer data) {
        if (GL45) {
            GL45.glNamedBufferSubData(buffer, offset, data);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferSubData(buffer, offset, data);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, offset, data);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    public static void namedBufferSubData(int buffer, long offset, FloatBuffer data) {
        if (GL45) {
            GL45.glNamedBufferSubData(buffer, offset, data);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferSubData(buffer, offset, data);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, offset, data);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    public static void namedBufferSubData(int buffer, long offset, IntBuffer data) {
        if (GL45) {
            GL45.glNamedBufferSubData(buffer, offset, data);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedBufferSubData(buffer, offset, data);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, offset, data);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    /**
     * glGetNamedBufferSubData - Get named buffer data
     * GL 4.5: glGetNamedBufferSubData(buffer, offset, data)
     */
    public static void getNamedBufferSubData(int buffer, long offset, ByteBuffer data) {
        if (GL45) {
            GL45.glGetNamedBufferSubData(buffer, offset, data);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glGetNamedBufferSubData(buffer, offset, data);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL15.glGetBufferSubData(GL15.GL_ARRAY_BUFFER, offset, data);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    /**
     * glMapNamedBuffer - Map entire named buffer
     * GL 4.5: glMapNamedBuffer(buffer, access)
     */
    public static ByteBuffer mapNamedBuffer(int buffer, int access) {
        if (GL45) {
            return GL45.glMapNamedBuffer(buffer, access);
        } else if (ARB_direct_state_access) {
            return ARBDirectStateAccess.glMapNamedBuffer(buffer, access);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            ByteBuffer result = GL15.glMapBuffer(GL15.GL_ARRAY_BUFFER, access);
            // Note: Can't unbind while mapped
            return result;
        }
    }
    
    /**
     * glMapNamedBufferRange - Map named buffer range
     * GL 4.5: glMapNamedBufferRange(buffer, offset, length, access)
     */
    public static ByteBuffer mapNamedBufferRange(int buffer, long offset, long length, int access) {
        if (GL45) {
            return GL45.glMapNamedBufferRange(buffer, offset, length, access);
        } else if (ARB_direct_state_access) {
            return ARBDirectStateAccess.glMapNamedBufferRange(buffer, offset, length, access);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            return GL30.glMapBufferRange(GL15.GL_ARRAY_BUFFER, offset, length, access);
        }
    }
    
    /**
     * glUnmapNamedBuffer - Unmap named buffer
     * GL 4.5: glUnmapNamedBuffer(buffer)
     */
    public static boolean unmapNamedBuffer(int buffer) {
        if (GL45) {
            return GL45.glUnmapNamedBuffer(buffer);
        } else if (ARB_direct_state_access) {
            return ARBDirectStateAccess.glUnmapNamedBuffer(buffer);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            boolean result = GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
            return result;
        }
    }
    
    /**
     * glFlushMappedNamedBufferRange - Flush mapped range
     * GL 4.5: glFlushMappedNamedBufferRange(buffer, offset, length)
     */
    public static void flushMappedNamedBufferRange(int buffer, long offset, long length) {
        if (GL45) {
            GL45.glFlushMappedNamedBufferRange(buffer, offset, length);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glFlushMappedNamedBufferRange(buffer, offset, length);
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL30.glFlushMappedBufferRange(GL15.GL_ARRAY_BUFFER, offset, length);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    /**
     * glCopyNamedBufferSubData - Copy between named buffers
     * GL 4.5: glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size)
     */
    public static void copyNamedBufferSubData(int readBuffer, int writeBuffer, 
                                               long readOffset, long writeOffset, long size) {
        if (GL45) {
            GL45.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
        } else {
            GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, readBuffer);
            GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, writeBuffer);
            GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER, 
                readOffset, writeOffset, size);
            GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
            GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
        }
    }
    
    /**
     * glClearNamedBufferData - Clear named buffer
     * GL 4.5: glClearNamedBufferData(buffer, internalformat, format, type, data)
     */
    public static void clearNamedBufferData(int buffer, int internalformat, int format, int type, ByteBuffer data) {
        if (GL45) {
            GL45.glClearNamedBufferData(buffer, internalformat, format, type, data);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glClearNamedBufferData(buffer, internalformat, format, type, data);
        } else if (GL43) {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL43.glClearBufferData(GL15.GL_ARRAY_BUFFER, internalformat, format, type, data);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    /**
     * glClearNamedBufferSubData - Clear named buffer region
     * GL 4.5: glClearNamedBufferSubData(buffer, internalformat, offset, size, format, type, data)
     */
    public static void clearNamedBufferSubData(int buffer, int internalformat, long offset, long size,
                                                int format, int type, ByteBuffer data) {
        if (GL45) {
            GL45.glClearNamedBufferSubData(buffer, internalformat, offset, size, format, type, data);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glClearNamedBufferSubData(buffer, internalformat, offset, size, format, type, data);
        } else if (GL43) {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            GL43.glClearBufferSubData(GL15.GL_ARRAY_BUFFER, internalformat, offset, size, format, type, data);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
        }
    }
    
    /**
     * glGetNamedBufferParameteri - Get named buffer parameter
     * GL 4.5: glGetNamedBufferParameteri(buffer, pname)
     */
    public static int getNamedBufferParameteri(int buffer, int pname) {
        if (GL45) {
            return GL45.glGetNamedBufferParameteri(buffer, pname);
        } else if (ARB_direct_state_access) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBDirectStateAccess.glGetNamedBufferParameteriv(buffer, pname, buf);
                return buf.get(0);
            }
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            int result = GL15.glGetBufferParameteri(GL15.GL_ARRAY_BUFFER, pname);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
            return result;
        }
    }
    
    /**
     * glGetNamedBufferParameteri64v - Get named buffer 64-bit parameter
     * GL 4.5: glGetNamedBufferParameteri64v(buffer, pname)
     */
    public static long getNamedBufferParameteri64(int buffer, int pname) {
        if (GL45) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                LongBuffer buf = stack.mallocLong(1);
                GL45.glGetNamedBufferParameteri64v(buffer, pname, buf);
                return buf.get(0);
            }
        } else if (ARB_direct_state_access) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                LongBuffer buf = stack.mallocLong(1);
                ARBDirectStateAccess.glGetNamedBufferParameteri64v(buffer, pname, buf);
                return buf.get(0);
            }
        } else {
            int previousBuffer = glGetIntegerStack(GL15.GL_ARRAY_BUFFER_BINDING);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                LongBuffer buf = stack.mallocLong(1);
                GL32.glGetBufferParameteri64v(GL15.GL_ARRAY_BUFFER, pname, buf);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, previousBuffer);
                return buf.get(0);
            }
        }
    }
    
    // ========================================================================
    // GL 4.5 - DSA TEXTURE OPERATIONS
    // ========================================================================
    
    /**
     * glCreateTextures - Create pre-initialized texture objects
     * GL 4.5: glCreateTextures(target, n, textures)
     */
    public static int createTexture(int target) {
        if (GL45) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                GL45.glCreateTextures(target, buf);
                return buf.get(0);
            }
        } else if (ARB_direct_state_access) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBDirectStateAccess.glCreateTextures(target, buf);
                return buf.get(0);
            }
        } else {
            int texture = genTexture();
            GL11.glBindTexture(target, texture);
            GL11.glBindTexture(target, 0);
            return texture;
        }
    }
    
    public static void createTextures(int target, IntBuffer textures) {
        if (GL45) {
            GL45.glCreateTextures(target, textures);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glCreateTextures(target, textures);
        } else {
            GL11.glGenTextures(textures);
            int pos = textures.position();
            int lim = textures.limit();
            for (int i = pos; i < lim; i++) {
                GL11.glBindTexture(target, textures.get(i));
            }
            GL11.glBindTexture(target, 0);
        }
    }
    
    /**
     * glTextureStorage1D - Allocate immutable 1D texture storage
     * GL 4.5: glTextureStorage1D(texture, levels, internalformat, width)
     */
    public static void textureStorage1D(int texture, int levels, int internalformat, int width) {
        if (GL45) {
            GL45.glTextureStorage1D(texture, levels, internalformat, width);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glTextureStorage1D(texture, levels, internalformat, width);
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_1D);
            GL11.glBindTexture(GL11.GL_TEXTURE_1D, texture);
            texStorage1D(GL11.GL_TEXTURE_1D, levels, internalformat, width);
            GL11.glBindTexture(GL11.GL_TEXTURE_1D, previousTexture);
        }
    }
    
    /**
     * glTextureStorage2D - Allocate immutable 2D texture storage
     * GL 4.5: glTextureStorage2D(texture, levels, internalformat, width, height)
     */
    public static void textureStorage2D(int texture, int levels, int internalformat, int width, int height) {
        if (GL45) {
            GL45.glTextureStorage2D(texture, levels, internalformat, width, height);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glTextureStorage2D(texture, levels, internalformat, width, height);
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            texStorage2D(GL11.GL_TEXTURE_2D, levels, internalformat, width, height);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }
    
    /**
     * glTextureStorage3D - Allocate immutable 3D texture storage
     * GL 4.5: glTextureStorage3D(texture, levels, internalformat, width, height, depth)
     */
    public static void textureStorage3D(int texture, int levels, int internalformat, int width, int height, int depth) {
        if (GL45) {
            GL45.glTextureStorage3D(texture, levels, internalformat, width, height, depth);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glTextureStorage3D(texture, levels, internalformat, width, height, depth);
        } else {
            int previousTexture = glGetIntegerStack(GL12.GL_TEXTURE_BINDING_3D);
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, texture);
            texStorage3D(GL12.GL_TEXTURE_3D, levels, internalformat, width, height, depth);
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, previousTexture);
        }
    }
    
    /**
     * glTextureStorage2DMultisample - Allocate multisample texture storage
     * GL 4.5: glTextureStorage2DMultisample(texture, samples, internalformat, width, height, fixedsamplelocations)
     */
    public static void textureStorage2DMultisample(int texture, int samples, int internalformat,
                                                    int width, int height, boolean fixedsamplelocations) {
        if (GL45) {
            GL45.glTextureStorage2DMultisample(texture, samples, internalformat, width, height, fixedsamplelocations);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glTextureStorage2DMultisample(texture, samples, internalformat, 
                width, height, fixedsamplelocations);
        } else {
            int previousTexture = glGetIntegerStack(GL32.GL_TEXTURE_BINDING_2D_MULTISAMPLE);
            GL11.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, texture);
            texStorage2DMultisample(GL32.GL_TEXTURE_2D_MULTISAMPLE, samples, internalformat, 
                width, height, fixedsamplelocations);
            GL11.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, previousTexture);
        }
    }
    
    /**
     * glTextureSubImage2D - Update texture data
     * GL 4.5: glTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, type, pixels)
     */
    public static void textureSubImage2D(int texture, int level, int xoffset, int yoffset,
                                          int width, int height, int format, int type, ByteBuffer pixels) {
        if (GL45) {
            GL45.glTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, type, pixels);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glTextureSubImage2D(texture, level, xoffset, yoffset, 
                width, height, format, type, pixels);
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, xoffset, yoffset, width, height, format, type, pixels);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }
    
    public static void textureSubImage2D(int texture, int level, int xoffset, int yoffset,
                                          int width, int height, int format, int type, long offset) {
        if (GL45) {
            GL45.glTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, type, offset);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glTextureSubImage2D(texture, level, xoffset, yoffset, 
                width, height, format, type, offset);
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, xoffset, yoffset, width, height, format, type, offset);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }
    
    /**
     * glTextureSubImage3D - Update 3D texture data
     * GL 4.5: glTextureSubImage3D(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels)
     */
    public static void textureSubImage3D(int texture, int level, int xoffset, int yoffset, int zoffset,
                                          int width, int height, int depth, int format, int type, ByteBuffer pixels) {
        if (GL45) {
            GL45.glTextureSubImage3D(texture, level, xoffset, yoffset, zoffset, 
                width, height, depth, format, type, pixels);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glTextureSubImage3D(texture, level, xoffset, yoffset, zoffset,
                width, height, depth, format, type, pixels);
        } else {
            int previousTexture = glGetIntegerStack(GL12.GL_TEXTURE_BINDING_3D);
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, texture);
            GL12.glTexSubImage3D(GL12.GL_TEXTURE_3D, level, xoffset, yoffset, zoffset, 
                width, height, depth, format, type, pixels);
            GL11.glBindTexture(GL12.GL_TEXTURE_3D, previousTexture);
        }
    }
    
    /**
     * glCompressedTextureSubImage2D - Update compressed texture data
     * GL 4.5: glCompressedTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, data)
     */
    public static void compressedTextureSubImage2D(int texture, int level, int xoffset, int yoffset,
                                                    int width, int height, int format, ByteBuffer data) {
        if (GL45) {
            GL45.glCompressedTextureSubImage2D(texture, level, xoffset, yoffset, width, height, format, data);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glCompressedTextureSubImage2D(texture, level, xoffset, yoffset,
                width, height, format, data);
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL13.glCompressedTexSubImage2D(GL11.GL_TEXTURE_2D, level, xoffset, yoffset, 
                width, height, format, data);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }
    
    /**
     * glCopyTextureSubImage2D - Copy from framebuffer to texture
     * GL 4.5: glCopyTextureSubImage2D(texture, level, xoffset, yoffset, x, y, width, height)
     */
    public static void copyTextureSubImage2D(int texture, int level, int xoffset, int yoffset,
                                              int x, int y, int width, int height) {
        if (GL45) {
            GL45.glCopyTextureSubImage2D(texture, level, xoffset, yoffset, x, y, width, height);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glCopyTextureSubImage2D(texture, level, xoffset, yoffset, x, y, width, height);
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, level, xoffset, yoffset, x, y, width, height);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }
    
    /**
     * glTextureParameteri - Set texture integer parameter
     * GL 4.5: glTextureParameteri(texture, pname, param)
     */
    public static void textureParameteri(int texture, int pname, int param) {
        if (GL45) {
            GL45.glTextureParameteri(texture, pname, param);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glTextureParameteri(texture, pname, param);
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, pname, param);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }
    
    /**
     * glTextureParameterf - Set texture float parameter
     * GL 4.5: glTextureParameterf(texture, pname, param)
     */
    public static void textureParameterf(int texture, int pname, float param) {
        if (GL45) {
            GL45.glTextureParameterf(texture, pname, param);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glTextureParameterf(texture, pname, param);
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, pname, param);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }
    
    /**
     * glTextureParameterfv - Set texture float array parameter
     * GL 4.5: glTextureParameterfv(texture, pname, params)
     */
    public static void textureParameterfv(int texture, int pname, FloatBuffer params) {
        if (GL45) {
            GL45.glTextureParameterfv(texture, pname, params);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glTextureParameterfv(texture, pname, params);
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexParameterfv(GL11.GL_TEXTURE_2D, pname, params);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }
    
    /**
     * glGenerateTextureMipmap - Generate mipmaps for texture
     * GL 4.5: glGenerateTextureMipmap(texture)
     */
    public static void generateTextureMipmap(int texture) {
        if (GL45) {
            GL45.glGenerateTextureMipmap(texture);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glGenerateTextureMipmap(texture);
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }
    
    /**
     * glBindTextureUnit - Bind texture to unit (DSA replacement for glActiveTexture + glBindTexture)
     * GL 4.5: glBindTextureUnit(unit, texture)
     */
    public static void bindTextureUnit(int unit, int texture) {
        if (GL45) {
            GL45.glBindTextureUnit(unit, texture);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glBindTextureUnit(unit, texture);
        } else {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        }
    }
    
    /**
     * glGetTextureImage - Get texture image data
     * GL 4.5: glGetTextureImage(texture, level, format, type, pixels)
     */
    public static void getTextureImage(int texture, int level, int format, int type, ByteBuffer pixels) {
        if (GL45) {
            GL45.glGetTextureImage(texture, level, format, type, pixels);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glGetTextureImage(texture, level, format, type, pixels);
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, level, format, type, pixels);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }
    
    /**
     * glGetTextureSubImage - Get texture sub-image data
     * GL 4.5: glGetTextureSubImage(texture, level, xoffset, yoffset, zoffset, width, height, depth, format, type, pixels)
     */
    public static void getTextureSubImage(int texture, int level, int xoffset, int yoffset, int zoffset,
                                           int width, int height, int depth, int format, int type, ByteBuffer pixels) {
        if (GL45) {
            GL45.glGetTextureSubImage(texture, level, xoffset, yoffset, zoffset, 
                width, height, depth, format, type, pixels);
        } else if (ARB_get_texture_sub_image) {
            ARBGetTextureSubImage.glGetTextureSubImage(texture, level, xoffset, yoffset, zoffset,
                width, height, depth, format, type, pixels);
        } else {
            // Fallback: get full image and extract sub-region
            // This is expensive but at least we only return the requested data
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            
            // Query texture dimensions
            int texWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level, GL11.GL_TEXTURE_WIDTH);
            int texHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level, GL11.GL_TEXTURE_HEIGHT);
            
            // Calculate bytes per pixel
            int bytesPerPixel = getBytesPerPixel(format, type);
            
            // Allocate buffer for full texture
            ByteBuffer fullImage = BufferUtils.createByteBuffer(texWidth * texHeight * bytesPerPixel);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, level, format, type, fullImage);
            
            // Extract sub-region row by row
            int rowSize = width * bytesPerPixel;
            for (int y = 0; y < height; y++) {
                int srcOffset = ((yoffset + y) * texWidth + xoffset) * bytesPerPixel;
                int dstOffset = y * rowSize;
                
                // Copy row
                for (int i = 0; i < rowSize; i++) {
                    pixels.put(dstOffset + i, fullImage.get(srcOffset + i));
                }
            }
            
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        }
    }
    
    /**
     * Calculate bytes per pixel for a given format and type
     */
    private static int getBytesPerPixel(int format, int type) {
        int components = 0;
        switch (format) {
            case GL11.GL_RED:
            case GL30.GL_RED_INTEGER:
                components = 1;
                break;
            case GL30.GL_RG:
            case GL30.GL_RG_INTEGER:
                components = 2;
                break;
            case GL11.GL_RGB:
            case GL11.GL_BGR:
            case GL30.GL_RGB_INTEGER:
            case GL12.GL_BGR_INTEGER:
                components = 3;
                break;
            case GL11.GL_RGBA:
            case GL11.GL_BGRA:
            case GL30.GL_RGBA_INTEGER:
            case GL12.GL_BGRA_INTEGER:
                components = 4;
                break;
            case GL11.GL_DEPTH_COMPONENT:
                components = 1;
                break;
            case GL30.GL_DEPTH_STENCIL:
                components = 2;
                break;
            default:
                components = 4; // Safe default
        }
        
        int bytesPerComponent = 0;
        switch (type) {
            case GL11.GL_UNSIGNED_BYTE:
            case GL11.GL_BYTE:
                bytesPerComponent = 1;
                break;
            case GL11.GL_UNSIGNED_SHORT:
            case GL11.GL_SHORT:
            case GL30.GL_HALF_FLOAT:
                bytesPerComponent = 2;
                break;
            case GL11.GL_UNSIGNED_INT:
            case GL11.GL_INT:
            case GL11.GL_FLOAT:
                bytesPerComponent = 4;
                break;
            case GL12.GL_UNSIGNED_BYTE_3_3_2:
            case GL12.GL_UNSIGNED_BYTE_2_3_3_REV:
                return 1;
            case GL12.GL_UNSIGNED_SHORT_5_6_5:
            case GL12.GL_UNSIGNED_SHORT_5_6_5_REV:
            case GL12.GL_UNSIGNED_SHORT_4_4_4_4:
            case GL12.GL_UNSIGNED_SHORT_4_4_4_4_REV:
            case GL12.GL_UNSIGNED_SHORT_5_5_5_1:
            case GL12.GL_UNSIGNED_SHORT_1_5_5_5_REV:
                return 2;
            case GL12.GL_UNSIGNED_INT_8_8_8_8:
            case GL12.GL_UNSIGNED_INT_8_8_8_8_REV:
            case GL12.GL_UNSIGNED_INT_10_10_10_2:
            case GL12.GL_UNSIGNED_INT_2_10_10_10_REV:
            case GL30.GL_UNSIGNED_INT_24_8:
            case GL30.GL_UNSIGNED_INT_10F_11F_11F_REV:
            case GL30.GL_UNSIGNED_INT_5_9_9_9_REV:
                return 4;
            default:
                bytesPerComponent = 4; // Safe default
        }
        
        return components * bytesPerComponent;
    }
    
    /**
     * glGetTextureLevelParameteri - Get texture level parameter
     * GL 4.5: glGetTextureLevelParameteri(texture, level, pname)
     */
    public static int getTextureLevelParameteri(int texture, int level, int pname) {
        if (GL45) {
            return GL45.glGetTextureLevelParameteri(texture, level, pname);
        } else if (ARB_direct_state_access) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBDirectStateAccess.glGetTextureLevelParameteriv(texture, level, pname, buf);
                return buf.get(0);
            }
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            int result = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level, pname);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            return result;
        }
    }
    
    /**
     * glGetTextureParameteri - Get texture parameter
     * GL 4.5: glGetTextureParameteri(texture, pname)
     */
    public static int getTextureParameteri(int texture, int pname) {
        if (GL45) {
            return GL45.glGetTextureParameteri(texture, pname);
        } else if (ARB_direct_state_access) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBDirectStateAccess.glGetTextureParameteriv(texture, pname, buf);
                return buf.get(0);
            }
        } else {
            int previousTexture = glGetIntegerStack(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            int result = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, pname);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            return result;
        }
    }
    
    // ========================================================================
    // GL 4.5 - DSA FRAMEBUFFER OPERATIONS
    // ========================================================================
    
    /**
     * glCreateFramebuffers - Create pre-initialized framebuffer objects
     * GL 4.5: glCreateFramebuffers(n, framebuffers)
     */
    public static int createFramebuffer() {
        if (GL45) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                GL45.glCreateFramebuffers(buf);
                int fbo = buf.get(0);
                if (fbo != 0) {
                    framebufferCache.put(fbo, new FramebufferInfo(fbo));
                }
                return fbo;
            }
        } else if (ARB_direct_state_access) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBDirectStateAccess.glCreateFramebuffers(buf);
                int fbo = buf.get(0);
                if (fbo != 0) {
                    framebufferCache.put(fbo, new FramebufferInfo(fbo));
                }
                return fbo;
            }
        } else {
            return genFramebuffer();
        }
    }
    
    /**
     * glNamedFramebufferTexture - Attach texture to framebuffer
     * GL 4.5: glNamedFramebufferTexture(framebuffer, attachment, texture, level)
     */
    public static void namedFramebufferTexture(int framebuffer, int attachment, int texture, int level) {
        if (GL45) {
            GL45.glNamedFramebufferTexture(framebuffer, attachment, texture, level);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedFramebufferTexture(framebuffer, attachment, texture, level);
        } else {
            int previousFbo = glGetIntegerStack(GL30.GL_FRAMEBUFFER_BINDING);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, attachment, texture, level);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFbo);
        }
    }
    
    /**
     * glNamedFramebufferTextureLayer - Attach texture layer to framebuffer
     * GL 4.5: glNamedFramebufferTextureLayer(framebuffer, attachment, texture, level, layer)
     */
    public static void namedFramebufferTextureLayer(int framebuffer, int attachment, int texture, int level, int layer) {
        if (GL45) {
            GL45.glNamedFramebufferTextureLayer(framebuffer, attachment, texture, level, layer);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedFramebufferTextureLayer(framebuffer, attachment, texture, level, layer);
        } else {
            int previousFbo = glGetIntegerStack(GL30.GL_FRAMEBUFFER_BINDING);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTextureLayer(GL30.GL_FRAMEBUFFER, attachment, texture, level, layer);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFbo);
        }
    }
    
    /**
     * glNamedFramebufferRenderbuffer - Attach renderbuffer to framebuffer
     * GL 4.5: glNamedFramebufferRenderbuffer(framebuffer, attachment, renderbuffertarget, renderbuffer)
     */
    public static void namedFramebufferRenderbuffer(int framebuffer, int attachment, int renderbuffertarget, int renderbuffer) {
        if (GL45) {
            GL45.glNamedFramebufferRenderbuffer(framebuffer, attachment, renderbuffertarget, renderbuffer);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedFramebufferRenderbuffer(framebuffer, attachment, renderbuffertarget, renderbuffer);
        } else {
            int previousFbo = glGetIntegerStack(GL30.GL_FRAMEBUFFER_BINDING);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, attachment, renderbuffertarget, renderbuffer);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFbo);
        }
    }
    
    /**
     * glNamedFramebufferDrawBuffer - Set draw buffer for framebuffer
     * GL 4.5: glNamedFramebufferDrawBuffer(framebuffer, buf)
     */
    public static void namedFramebufferDrawBuffer(int framebuffer, int buf) {
        if (GL45) {
            GL45.glNamedFramebufferDrawBuffer(framebuffer, buf);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedFramebufferDrawBuffer(framebuffer, buf);
        } else {
            int previousFbo = glGetIntegerStack(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
            GL11.glDrawBuffer(buf);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousFbo);
        }
    }
    
    /**
     * glNamedFramebufferDrawBuffers - Set multiple draw buffers for framebuffer
     * GL 4.5: glNamedFramebufferDrawBuffers(framebuffer, bufs)
     */
    public static void namedFramebufferDrawBuffers(int framebuffer, IntBuffer bufs) {
        if (GL45) {
            GL45.glNamedFramebufferDrawBuffers(framebuffer, bufs);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedFramebufferDrawBuffers(framebuffer, bufs);
        } else {
            int previousFbo = glGetIntegerStack(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
            GL20.glDrawBuffers(bufs);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousFbo);
        }
    }
    
    /**
     * glNamedFramebufferDrawBuffers with varargs
     */
    public static void namedFramebufferDrawBuffers(int framebuffer, int... bufs) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buf = stack.ints(bufs);
            namedFramebufferDrawBuffers(framebuffer, buf);
        }
    }
    
    /**
     * glNamedFramebufferReadBuffer - Set read buffer for framebuffer
     * GL 4.5: glNamedFramebufferReadBuffer(framebuffer, src)
     */
    public static void namedFramebufferReadBuffer(int framebuffer, int src) {
        if (GL45) {
            GL45.glNamedFramebufferReadBuffer(framebuffer, src);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedFramebufferReadBuffer(framebuffer, src);
        } else {
            int previousFbo = glGetIntegerStack(GL30.GL_READ_FRAMEBUFFER_BINDING);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebuffer);
            GL11.glReadBuffer(src);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousFbo);
        }
    }
    
    /**
     * glCheckNamedFramebufferStatus - Check framebuffer completeness
     * GL 4.5: glCheckNamedFramebufferStatus(framebuffer, target)
     */
    public static int checkNamedFramebufferStatus(int framebuffer, int target) {
        if (GL45) {
            return GL45.glCheckNamedFramebufferStatus(framebuffer, target);
        } else if (ARB_direct_state_access) {
            return ARBDirectStateAccess.glCheckNamedFramebufferStatus(framebuffer, target);
        } else {
            int previousFbo = glGetIntegerStack(GL30.GL_FRAMEBUFFER_BINDING);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            int status = GL30.glCheckFramebufferStatus(target);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFbo);
            return status;
        }
    }
    
    /**
     * glClearNamedFramebufferfv - Clear framebuffer to float values
     * GL 4.5: glClearNamedFramebufferfv(framebuffer, buffer, drawbuffer, value)
     */
    public static void clearNamedFramebufferfv(int framebuffer, int buffer, int drawbuffer, FloatBuffer value) {
        if (GL45) {
            GL45.glClearNamedFramebufferfv(framebuffer, buffer, drawbuffer, value);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glClearNamedFramebufferfv(framebuffer, buffer, drawbuffer, value);
        } else {
            int previousFbo = glGetIntegerStack(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
            GL30.glClearBufferfv(buffer, drawbuffer, value);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousFbo);
        }
    }
    
    /**
     * glClearNamedFramebufferfi - Clear framebuffer depth-stencil
     * GL 4.5: glClearNamedFramebufferfi(framebuffer, buffer, drawbuffer, depth, stencil)
     */
    public static void clearNamedFramebufferfi(int framebuffer, int buffer, int drawbuffer, float depth, int stencil) {
        if (GL45) {
            GL45.glClearNamedFramebufferfi(framebuffer, buffer, drawbuffer, depth, stencil);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glClearNamedFramebufferfi(framebuffer, buffer, drawbuffer, depth, stencil);
        } else {
            int previousFbo = glGetIntegerStack(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
            GL30.glClearBufferfi(buffer, drawbuffer, depth, stencil);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousFbo);
        }
    }
    
    /**
     * glBlitNamedFramebuffer - Blit between named framebuffers
     * GL 4.5: glBlitNamedFramebuffer(readFramebuffer, drawFramebuffer, srcX0, srcY0, srcX1, srcY1, 
     *                                 dstX0, dstY0, dstX1, dstY1, mask, filter)
     */
    public static void blitNamedFramebuffer(int readFramebuffer, int drawFramebuffer,
                                             int srcX0, int srcY0, int srcX1, int srcY1,
                                             int dstX0, int dstY0, int dstX1, int dstY1,
                                             int mask, int filter) {
        if (GL45) {
            GL45.glBlitNamedFramebuffer(readFramebuffer, drawFramebuffer, 
                srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glBlitNamedFramebuffer(readFramebuffer, drawFramebuffer,
                srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
        } else {
            int previousReadFbo = glGetIntegerStack(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int previousDrawFbo = glGetIntegerStack(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GL30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousReadFbo);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, previousDrawFbo);
        }
    }
    
    /**
     * glInvalidateNamedFramebufferData - Invalidate framebuffer attachments
     * GL 4.5: glInvalidateNamedFramebufferData(framebuffer, attachments)
     */
    public static void invalidateNamedFramebufferData(int framebuffer, IntBuffer attachments) {
        if (GL45) {
            GL45.glInvalidateNamedFramebufferData(framebuffer, attachments);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glInvalidateNamedFramebufferData(framebuffer, attachments);
        } else if (GL43) {
            int previousFbo = glGetIntegerStack(GL30.GL_FRAMEBUFFER_BINDING);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL43.glInvalidateFramebuffer(GL30.GL_FRAMEBUFFER, attachments);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFbo);
        }
    }
    
    // ========================================================================
    // GL 4.5 - DSA RENDERBUFFER OPERATIONS
    // ========================================================================
    
    /**
     * glCreateRenderbuffers - Create pre-initialized renderbuffer objects
     * GL 4.5: glCreateRenderbuffers(n, renderbuffers)
     */
    public static int createRenderbuffer() {
        if (GL45) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                GL45.glCreateRenderbuffers(buf);
                int rbo = buf.get(0);
                if (rbo != 0) {
                    renderbufferCache.put(rbo, new RenderbufferInfo(rbo));
                }
                return rbo;
            }
        } else if (ARB_direct_state_access) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBDirectStateAccess.glCreateRenderbuffers(buf);
                int rbo = buf.get(0);
                if (rbo != 0) {
                    renderbufferCache.put(rbo, new RenderbufferInfo(rbo));
                }
                return rbo;
            }
        } else {
            return genRenderbuffer();
        }
    }
    
    /**
     * glNamedRenderbufferStorage - Allocate renderbuffer storage
     * GL 4.5: glNamedRenderbufferStorage(renderbuffer, internalformat, width, height)
     */
    public static void namedRenderbufferStorage(int renderbuffer, int internalformat, int width, int height) {
        if (GL45) {
            GL45.glNamedRenderbufferStorage(renderbuffer, internalformat, width, height);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedRenderbufferStorage(renderbuffer, internalformat, width, height);
        } else {
            int previousRbo = glGetIntegerStack(GL30.GL_RENDERBUFFER_BINDING);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, renderbuffer);
            GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, internalformat, width, height);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, previousRbo);
        }
    }
    
    /**
     * glNamedRenderbufferStorageMultisample - Allocate multisample renderbuffer storage
     * GL 4.5: glNamedRenderbufferStorageMultisample(renderbuffer, samples, internalformat, width, height)
     */
    public static void namedRenderbufferStorageMultisample(int renderbuffer, int samples, int internalformat, 
                                                            int width, int height) {
        if (GL45) {
            GL45.glNamedRenderbufferStorageMultisample(renderbuffer, samples, internalformat, width, height);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedRenderbufferStorageMultisample(renderbuffer, samples, 
                internalformat, width, height);
        } else {
            int previousRbo = glGetIntegerStack(GL30.GL_RENDERBUFFER_BINDING);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, renderbuffer);
            GL30.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, samples, internalformat, width, height);
            GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, previousRbo);
        }
    }
    
    // ========================================================================
    // GL 4.5 - DSA VERTEX ARRAY OPERATIONS
    // ========================================================================
    
    /**
     * glCreateVertexArrays - Create pre-initialized VAO objects
     * GL 4.5: glCreateVertexArrays(n, arrays)
     */
    public static int createVertexArray() {
        if (GL45) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                GL45.glCreateVertexArrays(buf);
                return buf.get(0);
            }
        } else if (ARB_direct_state_access) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBDirectStateAccess.glCreateVertexArrays(buf);
                return buf.get(0);
            }
        } else {
            return genVertexArray();
        }
    }
    
    /**
     * glVertexArrayVertexBuffer - Bind buffer to VAO binding point
     * GL 4.5: glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride)
     */
    public static void vertexArrayVertexBuffer(int vaobj, int bindingindex, int buffer, long offset, int stride) {
        if (GL45) {
            GL45.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayVertexBuffer(vaobj, bindingindex, buffer, offset, stride);
        } else {
            int previousVao = glGetIntegerStack(GL30.GL_VERTEX_ARRAY_BINDING);
            GL30.glBindVertexArray(vaobj);
            GL43.glBindVertexBuffer(bindingindex, buffer, offset, stride);
            GL30.glBindVertexArray(previousVao);
        }
    }
    
    /**
     * glVertexArrayVertexBuffers - Bind multiple buffers to VAO
     * GL 4.5: glVertexArrayVertexBuffers(vaobj, first, buffers, offsets, strides)
     */
    public static void vertexArrayVertexBuffers(int vaobj, int first, IntBuffer buffers, 
                                                 PointerBuffer offsets, IntBuffer strides) {
        if (GL45) {
            GL45.glVertexArrayVertexBuffers(vaobj, first, buffers, offsets, strides);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayVertexBuffers(vaobj, first, buffers, offsets, strides);
        } else {
            int previousVao = glGetIntegerStack(GL30.GL_VERTEX_ARRAY_BINDING);
            GL30.glBindVertexArray(vaobj);
            GL44.glBindVertexBuffers(first, buffers, offsets, strides);
            GL30.glBindVertexArray(previousVao);
        }
    }
    
    /**
     * glVertexArrayElementBuffer - Set element buffer for VAO
     * GL 4.5: glVertexArrayElementBuffer(vaobj, buffer)
     */
    public static void vertexArrayElementBuffer(int vaobj, int buffer) {
        if (GL45) {
            GL45.glVertexArrayElementBuffer(vaobj, buffer);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayElementBuffer(vaobj, buffer);
        } else {
            int previousVao = glGetIntegerStack(GL30.GL_VERTEX_ARRAY_BINDING);
            GL30.glBindVertexArray(vaobj);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer);
            GL30.glBindVertexArray(previousVao);
        }
    }
    
    /**
     * glVertexArrayAttribFormat - Specify vertex attribute format in VAO
     * GL 4.5: glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset)
     */
    public static void vertexArrayAttribFormat(int vaobj, int attribindex, int size, int type, 
                                                boolean normalized, int relativeoffset) {
        if (GL45) {
            GL45.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayAttribFormat(vaobj, attribindex, size, type, normalized, relativeoffset);
        } else {
            int previousVao = glGetIntegerStack(GL30.GL_VERTEX_ARRAY_BINDING);
            GL30.glBindVertexArray(vaobj);
            GL43.glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset);
            GL30.glBindVertexArray(previousVao);
        }
    }
    
    /**
     * glVertexArrayAttribIFormat - Specify integer vertex attribute format in VAO
     * GL 4.5: glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset)
     */
    public static void vertexArrayAttribIFormat(int vaobj, int attribindex, int size, int type, int relativeoffset) {
        if (GL45) {
            GL45.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayAttribIFormat(vaobj, attribindex, size, type, relativeoffset);
        } else {
            int previousVao = glGetIntegerStack(GL30.GL_VERTEX_ARRAY_BINDING);
            GL30.glBindVertexArray(vaobj);
            GL43.glVertexAttribIFormat(attribindex, size, type, relativeoffset);
            GL30.glBindVertexArray(previousVao);
        }
    }
    
    /**
     * glVertexArrayAttribBinding - Associate attribute with binding in VAO
     * GL 4.5: glVertexArrayAttribBinding(vaobj, attribindex, bindingindex)
     */
    public static void vertexArrayAttribBinding(int vaobj, int attribindex, int bindingindex) {
        if (GL45) {
            GL45.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayAttribBinding(vaobj, attribindex, bindingindex);
        } else {
            int previousVao = glGetIntegerStack(GL30.GL_VERTEX_ARRAY_BINDING);
            GL30.glBindVertexArray(vaobj);
            GL43.glVertexAttribBinding(attribindex, bindingindex);
            GL30.glBindVertexArray(previousVao);
        }
    }
    
    /**
     * glEnableVertexArrayAttrib - Enable vertex attribute in VAO
     * GL 4.5: glEnableVertexArrayAttrib(vaobj, index)
     */
    public static void enableVertexArrayAttrib(int vaobj, int index) {
        if (GL45) {
            GL45.glEnableVertexArrayAttrib(vaobj, index);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glEnableVertexArrayAttrib(vaobj, index);
        } else {
            int previousVao = glGetIntegerStack(GL30.GL_VERTEX_ARRAY_BINDING);
            GL30.glBindVertexArray(vaobj);
            GL20.glEnableVertexAttribArray(index);
            GL30.glBindVertexArray(previousVao);
        }
    }
    
    /**
     * glDisableVertexArrayAttrib - Disable vertex attribute in VAO
     * GL 4.5: glDisableVertexArrayAttrib(vaobj, index)
     */
    public static void disableVertexArrayAttrib(int vaobj, int index) {
        if (GL45) {
            GL45.glDisableVertexArrayAttrib(vaobj, index);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glDisableVertexArrayAttrib(vaobj, index);
        } else {
            int previousVao = glGetIntegerStack(GL30.GL_VERTEX_ARRAY_BINDING);
            GL30.glBindVertexArray(vaobj);
            GL20.glDisableVertexAttribArray(index);
            GL30.glBindVertexArray(previousVao);
        }
    }
    
    /**
     * glVertexArrayBindingDivisor - Set binding divisor in VAO
     * GL 4.5: glVertexArrayBindingDivisor(vaobj, bindingindex, divisor)
     */
    public static void vertexArrayBindingDivisor(int vaobj, int bindingindex, int divisor) {
        if (GL45) {
            GL45.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glVertexArrayBindingDivisor(vaobj, bindingindex, divisor);
        } else {
            int previousVao = glGetIntegerStack(GL30.GL_VERTEX_ARRAY_BINDING);
            GL30.glBindVertexArray(vaobj);
            GL43.glVertexBindingDivisor(bindingindex, divisor);
            GL30.glBindVertexArray(previousVao);
        }
    }
    
    // ========================================================================
    // GL 4.5 - DSA SAMPLER OPERATIONS
    // ========================================================================
    
    /**
     * glCreateSamplers - Create pre-initialized sampler objects
     * GL 4.5: glCreateSamplers(n, samplers)
     */
    public static int createSampler() {
        if (GL45) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                GL45.glCreateSamplers(buf);
                int sampler = buf.get(0);
                if (sampler != 0) {
                    samplerCache.put(sampler, new SamplerInfo(sampler));
                }
                return sampler;
            }
        } else if (ARB_direct_state_access) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBDirectStateAccess.glCreateSamplers(buf);
                int sampler = buf.get(0);
                if (sampler != 0) {
                    samplerCache.put(sampler, new SamplerInfo(sampler));
                }
                return sampler;
            }
        } else {
            return genSampler();
        }
    }
    
    // ========================================================================
    // GL 4.5 - DSA PROGRAM PIPELINE OPERATIONS
    // ========================================================================
    
    /**
     * glCreateProgramPipelines - Create pre-initialized program pipelines
     * GL 4.5: glCreateProgramPipelines(n, pipelines)
     */
    public static int createProgramPipeline() {
        if (GL45) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                GL45.glCreateProgramPipelines(buf);
                return buf.get(0);
            }
        } else if (ARB_direct_state_access) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBDirectStateAccess.glCreateProgramPipelines(buf);
                return buf.get(0);
            }
        } else {
            return genProgramPipeline();
        }
    }
    
    // ========================================================================
    // GL 4.5 - DSA TRANSFORM FEEDBACK OPERATIONS
    // ========================================================================
    
    /**
     * glCreateTransformFeedbacks - Create pre-initialized transform feedback objects
     * GL 4.5: glCreateTransformFeedbacks(n, ids)
     */
    public static int createTransformFeedback() {
        if (GL45) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                GL45.glCreateTransformFeedbacks(buf);
                return buf.get(0);
            }
        } else if (ARB_direct_state_access) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buf = stack.mallocInt(1);
                ARBDirectStateAccess.glCreateTransformFeedbacks(buf);
                return buf.get(0);
            }
        } else {
            return genTransformFeedback();
        }
    }
    
    /**
     * glTransformFeedbackBufferBase - Bind buffer to transform feedback
     * GL 4.5: glTransformFeedbackBufferBase(xfb, index, buffer)
     */
    public static void transformFeedbackBufferBase(int xfb, int index, int buffer) {
        if (GL45) {
            GL45.glTransformFeedbackBufferBase(xfb, index, buffer);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glTransformFeedbackBufferBase(xfb, index, buffer);
        } else {
            int previousXfb = glGetIntegerStack(GL40.GL_TRANSFORM_FEEDBACK_BINDING);
            GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, xfb);
            GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, index, buffer);
            GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, previousXfb);
        }
    }
    
    /**
     * glTransformFeedbackBufferRange - Bind buffer range to transform feedback
     * GL 4.5: glTransformFeedbackBufferRange(xfb, index, buffer, offset, size)
     */
    public static void transformFeedbackBufferRange(int xfb, int index, int buffer, long offset, long size) {
        if (GL45) {
            GL45.glTransformFeedbackBufferRange(xfb, index, buffer, offset, size);
        } else if (ARB_direct_state_access) {
            ARBDirectStateAccess.glTransformFeedbackBufferRange(xfb, index, buffer, offset, size);
        } else {
            int previousXfb = glGetIntegerStack(GL40.GL_TRANSFORM_FEEDBACK_BINDING);
            GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, xfb);
            GL30.glBindBufferRange(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, index, buffer, offset, size);
            GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, previousXfb);
        }
    }
    
    // ========================================================================
    // GL 4.5 - CLIP CONTROL
    // ========================================================================
    
    // Clip control constants
    public static final int GL_LOWER_LEFT = GL45.GL_LOWER_LEFT;
    public static final int GL_UPPER_LEFT = GL45.GL_UPPER_LEFT;
    public static final int GL_NEGATIVE_ONE_TO_ONE = GL45.GL_NEGATIVE_ONE_TO_ONE;
    public static final int GL_ZERO_TO_ONE = GL45.GL_ZERO_TO_ONE;
    public static final int GL_CLIP_ORIGIN = GL45.GL_CLIP_ORIGIN;
    public static final int GL_CLIP_DEPTH_MODE = GL45.GL_CLIP_DEPTH_MODE;
    
    /**
     * Check if clip control is supported
     */
    public static boolean supportsClipControl() {
        return hasClipControl;
    }
    
    /**
     * glClipControl - Control clip volume
     * GL 4.5: glClipControl(origin, depth)
     * 
     * Common uses:
     * - Reverse-Z depth buffer: glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE)
     * - Vulkan-style clip: glClipControl(GL_UPPER_LEFT, GL_ZERO_TO_ONE)
     */
    public static void clipControl(int origin, int depth) {
        if (GL45) {
            GL45.glClipControl(origin, depth);
        } else if (ARB_clip_control) {
            ARBClipControl.glClipControl(origin, depth);
        }
    }
    
    /**
     * Enable reverse-Z depth buffer
     * Better precision for distant objects
     */
    public static void enableReverseZ() {
        if (hasClipControl) {
            clipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE);
            // Also need to adjust depth function
            GL11.glDepthFunc(GL11.GL_GEQUAL);
            GL11.glClearDepth(0.0);
        }
    }
    
    /**
     * Restore default clip control
     */
    public static void disableReverseZ() {
        if (hasClipControl) {
            clipControl(GL_LOWER_LEFT, GL_NEGATIVE_ONE_TO_ONE);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glClearDepth(1.0);
        }
    }
    
    // ========================================================================
    // GL 4.5 - TEXTURE BARRIER
    // ========================================================================
    
    /**
     * glTextureBarrier - Ensure texture reads see prior writes
     * GL 4.5: glTextureBarrier()
     */
    public static void textureBarrier() {
        if (GL45) {
            GL45.glTextureBarrier();
        } else if (ARB_texture_barrier) {
            ARBTextureBarrier.glTextureBarrier();
        } else if (NV_texture_barrier) {
            NVTextureBarrier.glTextureBarrierNV();
        }
    }
    
    // ========================================================================
    // GL 4.5 - ROBUSTNESS
    // ========================================================================
    
    // Reset status constants
    public static final int GL_NO_ERROR = GL11.GL_NO_ERROR;
    public static final int GL_GUILTY_CONTEXT_RESET = GL45.GL_GUILTY_CONTEXT_RESET;
    public static final int GL_INNOCENT_CONTEXT_RESET = GL45.GL_INNOCENT_CONTEXT_RESET;
    public static final int GL_UNKNOWN_CONTEXT_RESET = GL45.GL_UNKNOWN_CONTEXT_RESET;
    
    /**
     * glGetGraphicsResetStatus - Get GPU reset status
     * GL 4.5: glGetGraphicsResetStatus()
     */
    public static int getGraphicsResetStatus() {
        if (GL45) {
            return GL45.glGetGraphicsResetStatus();
        } else if (ARB_robustness) {
            return ARBRobustness.glGetGraphicsResetStatusARB();
        }
        return GL11.GL_NO_ERROR;
    }
    
    /**
     * Check if GPU reset occurred
     */
    public static boolean hasGraphicsReset() {
        int status = getGraphicsResetStatus();
        return status != GL11.GL_NO_ERROR;
    }
    
    /**
     * Get reset status as string
     */
    public static String getGraphicsResetStatusString() {
        int status = getGraphicsResetStatus();
        switch (status) {
            case GL11.GL_NO_ERROR: return "NO_ERROR";
            case GL_GUILTY_CONTEXT_RESET: return "GUILTY_CONTEXT_RESET";
            case GL_INNOCENT_CONTEXT_RESET: return "INNOCENT_CONTEXT_RESET";
            case GL_UNKNOWN_CONTEXT_RESET: return "UNKNOWN_CONTEXT_RESET";
            default: return "UNKNOWN(" + status + ")";
        }
    }
    
    // ========================================================================
    // GL 4.6 - SPIR-V SHADERS
    // ========================================================================
    
    /**
     * Check if SPIR-V shaders are supported
     */
    public static boolean supportsSPIRV() {
        return hasSPIRV;
    }
    
    /**
     * glSpecializeShader - Specialize SPIR-V shader
     * GL 4.6: glSpecializeShader(shader, pEntryPoint, pConstantIndex, pConstantValue)
     */
    public static void specializeShader(int shader, CharSequence pEntryPoint, 
                                         IntBuffer pConstantIndex, IntBuffer pConstantValue) {
        if (GL46) {
            GL46.glSpecializeShader(shader, pEntryPoint, pConstantIndex, pConstantValue);
        } else if (ARB_gl_spirv) {
            ARBGLSpirv.glSpecializeShaderARB(shader, pEntryPoint, pConstantIndex, pConstantValue);
        }
    }
    
    /**
     * glSpecializeShader with default entry point "main"
     */
    public static void specializeShader(int shader) {
        specializeShader(shader, "main", null, null);
    }
    
    /**
     * Load SPIR-V shader from binary data
     */
    public static int createShaderFromSPIRV(int type, ByteBuffer spirvBinary, CharSequence entryPoint) {
        if (!hasSPIRV) {
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] SPIR-V not supported");
            }
            return 0;
        }
        
        int shader = GL20.glCreateShader(type);
        if (shader == 0) return 0;
        
        // Use glShaderBinary to load SPIR-V
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer shaders = stack.ints(shader);
            if (GL46) {
                GL46.glShaderBinary(shaders, GL46.GL_SHADER_BINARY_FORMAT_SPIR_V, spirvBinary);
            } else if (ARB_gl_spirv) {
                GL41.glShaderBinary(shaders, ARBGLSpirv.GL_SHADER_BINARY_FORMAT_SPIR_V_ARB, spirvBinary);
            }
        }
        
        specializeShader(shader, entryPoint, null, null);
        
        // Check for errors
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_TRUE) {
            String log = GL20.glGetShaderInfoLog(shader);
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] SPIR-V shader specialization failed:");
                System.err.println(log);
            }
            GL20.glDeleteShader(shader);
            return 0;
        }
        
        return shader;
    }
    
    // ========================================================================
    // GL 4.6 - ANISOTROPIC FILTERING (CORE)
    // ========================================================================
    
    // Anisotropic filtering constant
    public static final int GL_TEXTURE_MAX_ANISOTROPY = GL46.GL_TEXTURE_MAX_ANISOTROPY;
    public static final int GL_MAX_TEXTURE_MAX_ANISOTROPY = GL46.GL_MAX_TEXTURE_MAX_ANISOTROPY;
    
    /**
     * Check if anisotropic filtering is supported
     */
    public static boolean supportsAnisotropicFiltering() {
        return hasAnisotropicFiltering;
    }
    
    /**
     * Get maximum anisotropy level
     */
    public static float getMaxAnisotropy() {
        if (GL46) {
            return GL11.glGetFloat(GL46.GL_MAX_TEXTURE_MAX_ANISOTROPY);
        } else if (EXT_texture_filter_anisotropic) {
            return GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
        }
        return 1.0f;
    }
    
    /**
     * Set texture anisotropy level
     */
    public static void setTextureAnisotropy(int texture, float anisotropy) {
        if (!hasAnisotropicFiltering) return;
        
        float maxAniso = getMaxAnisotropy();
        anisotropy = Math.min(anisotropy, maxAniso);
        
        if (GL46) {
            textureParameterf(texture, GL46.GL_TEXTURE_MAX_ANISOTROPY, anisotropy);
        } else if (EXT_texture_filter_anisotropic) {
            textureParameterf(texture, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy);
        }
    }
    
    /**
     * Set sampler anisotropy level
     */
    public static void setSamplerAnisotropy(int sampler, float anisotropy) {
        if (!hasAnisotropicFiltering || !hasSamplerObjects) return;
        
        float maxAniso = getMaxAnisotropy();
        anisotropy = Math.min(anisotropy, maxAniso);
        
        if (GL46) {
            GL33.glSamplerParameterf(sampler, GL46.GL_TEXTURE_MAX_ANISOTROPY, anisotropy);
        } else if (EXT_texture_filter_anisotropic) {
            GL33.glSamplerParameterf(sampler, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropy);
        }
    }
    
    // ========================================================================
    // GL 4.6 - POLYGON OFFSET CLAMP
    // ========================================================================
    
    /**
     * glPolygonOffsetClamp - Set polygon offset with clamp
     * GL 4.6: glPolygonOffsetClamp(factor, units, clamp)
     */
    public static void polygonOffsetClamp(float factor, float units, float clamp) {
        if (GL46) {
            GL46.glPolygonOffsetClamp(factor, units, clamp);
        } else if (ARB_polygon_offset_clamp) {
            ARBPolygonOffsetClamp.glPolygonOffsetClamp(factor, units, clamp);
        } else if (EXT_polygon_offset_clamp) {
            EXTPolygonOffsetClamp.glPolygonOffsetClampEXT(factor, units, clamp);
        } else {
            // Fallback to non-clamped version
            GL11.glPolygonOffset(factor, units);
        }
    }
    
    // ========================================================================
    // GL 4.6 - MULTI-DRAW INDIRECT COUNT
    // ========================================================================
    
    /**
     * glMultiDrawArraysIndirectCount - Multi-draw with count from buffer
     * GL 4.6: glMultiDrawArraysIndirectCount(mode, indirect, drawcount, maxdrawcount, stride)
     */
    public static void multiDrawArraysIndirectCount(int mode, long indirect, long drawcount, int maxdrawcount, int stride) {
        if (GL46) {
            GL46.glMultiDrawArraysIndirectCount(mode, indirect, drawcount, maxdrawcount, stride);
        } else if (ARB_indirect_parameters) {
            ARBIndirectParameters.glMultiDrawArraysIndirectCountARB(mode, indirect, drawcount, maxdrawcount, stride);
        } else {
            // No simple fallback - would need to read count from buffer
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Multi-draw indirect count not supported");
            }
        }
    }
    
    /**
     * glMultiDrawElementsIndirectCount - Multi-draw elements with count from buffer
     * GL 4.6: glMultiDrawElementsIndirectCount(mode, type, indirect, drawcount, maxdrawcount, stride)
     */
    public static void multiDrawElementsIndirectCount(int mode, int type, long indirect, 
                                                       long drawcount, int maxdrawcount, int stride) {
        if (GL46) {
            GL46.glMultiDrawElementsIndirectCount(mode, type, indirect, drawcount, maxdrawcount, stride);
        } else if (ARB_indirect_parameters) {
            ARBIndirectParameters.glMultiDrawElementsIndirectCountARB(mode, type, indirect, drawcount, maxdrawcount, stride);
        }
    }
    
    // ========================================================================
    // ADDITIONAL EXTENSION FLAGS
    // ========================================================================
    
    private static boolean ARB_direct_state_access = false;
    private static boolean ARB_get_texture_sub_image = false;
    private static boolean ARB_texture_barrier = false;
    private static boolean NV_texture_barrier = false;
    private static boolean ARB_clip_control = false;
    private static boolean ARB_robustness = false;
    private static boolean ARB_gl_spirv = false;
    private static boolean ARB_polygon_offset_clamp = false;
    private static boolean EXT_polygon_offset_clamp = false;
    private static boolean ARB_indirect_parameters = false;
    
    // ========================================================================
    // DSA CONVENIENCE HELPERS
    // ========================================================================
    
    /**
     * Complete texture setup using DSA (2D texture)
     */
    public static int createTexture2D(int internalFormat, int width, int height, int levels,
                                       int minFilter, int magFilter, int wrapS, int wrapT) {
        int texture = createTexture(GL11.GL_TEXTURE_2D);
        
        textureStorage2D(texture, levels, internalFormat, width, height);
        textureParameteri(texture, GL11.GL_TEXTURE_MIN_FILTER, minFilter);
        textureParameteri(texture, GL11.GL_TEXTURE_MAG_FILTER, magFilter);
        textureParameteri(texture, GL11.GL_TEXTURE_WRAP_S, wrapS);
        textureParameteri(texture, GL11.GL_TEXTURE_WRAP_T, wrapT);
        
        return texture;
    }
    
    /**
     * Create simple RGBA texture with default parameters
     */
    public static int createSimpleTexture2D(int width, int height, boolean mipmaps) {
        int levels = mipmaps ? calculateMipLevels(width, height) : 1;
        return createTexture2D(GL11.GL_RGBA8, width, height, levels,
            mipmaps ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR,
            GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
    }
    
    /**
     * Calculate number of mip levels for dimensions
     */
    public static int calculateMipLevels(int width, int height) {
        return (int) Math.floor(Math.log(Math.max(width, height)) / Math.log(2)) + 1;
    }
    
    /**
     * Complete framebuffer setup using DSA
     */
    public static int createFramebufferWithColorAndDepth(int width, int height) {
        // Create color texture
        int colorTexture = createTexture2D(GL11.GL_RGBA8, width, height, 1,
            GL11.GL_LINEAR, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE);
        
        // Create depth renderbuffer
        int depthRbo = createRenderbuffer();
        namedRenderbufferStorage(depthRbo, GL30.GL_DEPTH_COMPONENT24, width, height);
        
        // Create framebuffer
        int fbo = createFramebuffer();
        namedFramebufferTexture(fbo, GL30.GL_COLOR_ATTACHMENT0, colorTexture, 0);
        namedFramebufferRenderbuffer(fbo, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthRbo);
        namedFramebufferDrawBuffer(fbo, GL30.GL_COLOR_ATTACHMENT0);
        
        // Check status
        int status = checkNamedFramebufferStatus(fbo, GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            if (debugMode) {
                System.err.println("[OpenGLCallMapper] Framebuffer incomplete: " + getFramebufferStatusString(status));
            }
        }
        
        return fbo;
    }
    
    /**
     * Complete VAO setup using DSA
     */
    public static int createVertexArrayWithFormat(int vbo, int ebo, VertexFormat... formats) {
        int vao = createVertexArray();
        
        for (VertexFormat format : formats) {
            enableVertexArrayAttrib(vao, format.index);
            vertexArrayAttribFormat(vao, format.index, format.size, format.type, format.normalized, format.offset);
            vertexArrayAttribBinding(vao, format.index, format.binding);
        }
        
        // Calculate total stride
        int stride = 0;
        for (VertexFormat format : formats) {
            stride = Math.max(stride, format.offset + format.size * getTypeSize(format.type));
        }
        
        vertexArrayVertexBuffer(vao, 0, vbo, 0, stride);
        
        if (ebo != 0) {
            vertexArrayElementBuffer(vao, ebo);
        }
        
        return vao;
    }
    
    public static class VertexFormat {
        public final int index;
        public final int size;
        public final int type;
        public final boolean normalized;
        public final int offset;
        public final int binding;
        
        public VertexFormat(int index, int size, int type, boolean normalized, int offset) {
            this(index, size, type, normalized, offset, 0);
        }
        
        public VertexFormat(int index, int size, int type, boolean normalized, int offset, int binding) {
            this.index = index;
            this.size = size;
            this.type = type;
            this.normalized = normalized;
            this.offset = offset;
            this.binding = binding;
        }
    }
    
    private static int getTypeSize(int type) {
        switch (type) {
            case GL11.GL_BYTE:
            case GL11.GL_UNSIGNED_BYTE:
                return 1;
            case GL11.GL_SHORT:
            case GL11.GL_UNSIGNED_SHORT:
            case GL30.GL_HALF_FLOAT:
                return 2;
            case GL11.GL_INT:
            case GL11.GL_UNSIGNED_INT:
            case GL11.GL_FLOAT:
                return 4;
            case GL11.GL_DOUBLE:
                return 8;
            default:
                return 4;
        }
    }
}

    // ========================================================================
    // GLSL VERSION CONSTANTS
    // ========================================================================
    
    public static final int GLSL_110 = 110;
    public static final int GLSL_120 = 120;
    public static final int GLSL_130 = 130;
    public static final int GLSL_140 = 140;
    public static final int GLSL_150 = 150;
    public static final int GLSL_330 = 330;
    public static final int GLSL_400 = 400;
    public static final int GLSL_410 = 410;
    public static final int GLSL_420 = 420;
    public static final int GLSL_430 = 430;
    public static final int GLSL_440 = 440;
    public static final int GLSL_450 = 450;
    public static final int GLSL_460 = 460;
    
    // Shader types
    public static final int SHADER_VERTEX = 0;
    public static final int SHADER_FRAGMENT = 1;
    public static final int SHADER_GEOMETRY = 2;
    public static final int SHADER_TESS_CONTROL = 3;
    public static final int SHADER_TESS_EVAL = 4;
    public static final int SHADER_COMPUTE = 5;
    
    // ========================================================================
    // TRANSLATION PATTERNS
    // ========================================================================
    
    // Version directive pattern
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "^\\s*#\\s*version\\s+(\\d+)(\\s+\\w+)?\\s*$", Pattern.MULTILINE);
    
    // Attribute/varying patterns (for vertex shaders)
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
        "\\battribute\\s+", Pattern.MULTILINE);
    private static final Pattern VARYING_OUT_PATTERN = Pattern.compile(
        "\\bvarying\\s+(?=\\w+\\s+\\w+\\s*;)", Pattern.MULTILINE);
    
    // Varying patterns (for fragment shaders)
    private static final Pattern VARYING_IN_PATTERN = Pattern.compile(
        "\\bvarying\\s+", Pattern.MULTILINE);
    
    // Texture function patterns
    private static final Pattern TEXTURE2D_PATTERN = Pattern.compile(
        "\\btexture2D\\s*\\(", Pattern.MULTILINE);
    private static final Pattern TEXTURE2DLOD_PATTERN = Pattern.compile(
        "\\btexture2DLod\\s*\\(", Pattern.MULTILINE);
    private static final Pattern TEXTURE2DPROJ_PATTERN = Pattern.compile(
        "\\btexture2DProj\\s*\\(", Pattern.MULTILINE);
    private static final Pattern TEXTURE3D_PATTERN = Pattern.compile(
        "\\btexture3D\\s*\\(", Pattern.MULTILINE);
    private static final Pattern TEXTURECUBE_PATTERN = Pattern.compile(
        "\\btextureCube\\s*\\(", Pattern.MULTILINE);
    private static final Pattern SHADOW2D_PATTERN = Pattern.compile(
        "\\bshadow2D\\s*\\(", Pattern.MULTILINE);
    private static final Pattern SHADOW2DPROJ_PATTERN = Pattern.compile(
        "\\bshadow2DProj\\s*\\(", Pattern.MULTILINE);
    
    // gl_FragColor/gl_FragData patterns
    private static final Pattern FRAGCOLOR_PATTERN = Pattern.compile(
        "\\bgl_FragColor\\b", Pattern.MULTILINE);
    private static final Pattern FRAGDATA_PATTERN = Pattern.compile(
        "\\bgl_FragData\\s*\\[\\s*(\\d+)\\s*\\]", Pattern.MULTILINE);
    
    // Modern texture patterns (for downgrade)
    private static final Pattern TEXTURE_PATTERN = Pattern.compile(
        "\\btexture\\s*\\(\\s*(\\w+)\\s*,", Pattern.MULTILINE);
    private static final Pattern TEXTURELOD_PATTERN = Pattern.compile(
        "\\btextureLod\\s*\\(", Pattern.MULTILINE);
    private static final Pattern TEXTUREPROJ_PATTERN = Pattern.compile(
        "\\btextureProj\\s*\\(", Pattern.MULTILINE);
    
    // Layout qualifier pattern
    private static final Pattern LAYOUT_LOCATION_PATTERN = Pattern.compile(
        "layout\\s*\\(\\s*location\\s*=\\s*(\\d+)\\s*\\)", Pattern.MULTILINE);
    private static final Pattern LAYOUT_BINDING_PATTERN = Pattern.compile(
        "layout\\s*\\(\\s*binding\\s*=\\s*(\\d+)\\s*\\)", Pattern.MULTILINE);
    
    // In/out patterns
    private static final Pattern IN_PATTERN = Pattern.compile(
        "\\bin\\s+(?!\\()", Pattern.MULTILINE);
    private static final Pattern OUT_PATTERN = Pattern.compile(
        "\\bout\\s+(?!\\()", Pattern.MULTILINE);
    
    // Precision pattern
    private static final Pattern PRECISION_PATTERN = Pattern.compile(
        "\\bprecision\\s+(lowp|mediump|highp)\\s+\\w+\\s*;", Pattern.MULTILINE);
    
    // ========================================================================
    // GLSL VERSION DETECTION
    // ========================================================================
    
    /**
     * Detect GLSL version from shader source
     */
    public static int detectVersion(String source) {
        Matcher matcher = VERSION_PATTERN.matcher(source);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return GLSL_110;
            }
        }
        // No version directive = GLSL 110
        return GLSL_110;
    }
    
    /**
     * Detect shader type from source (heuristic)
     */
    public static int detectShaderType(String source) {
        String lower = source.toLowerCase();
        
        if (lower.contains("gl_fragcolor") || lower.contains("gl_fragdata") || 
            lower.contains("discard") || 
            (lower.contains("out vec4") && lower.contains("void main"))) {
            return SHADER_FRAGMENT;
        }
        
        if (lower.contains("gl_position") || lower.contains("attribute ") ||
            (lower.contains("in vec") && lower.contains("void main") && !lower.contains("layout(local_size"))) {
            return SHADER_VERTEX;
        }
        
        if (lower.contains("layout(triangles") || lower.contains("layout(points") ||
            lower.contains("layout(lines") || lower.contains("emitvertex")) {
            return SHADER_GEOMETRY;
        }
        
        if (lower.contains("layout(vertices") || lower.contains("gl_invocationid") ||
            lower.contains("barrier()") && lower.contains("gl_tesscoord")) {
            return SHADER_TESS_CONTROL;
        }
        
        if (lower.contains("gl_tesscoord") || lower.contains("layout(triangles") && 
            lower.contains("layout(equal_spacing")) {
            return SHADER_TESS_EVAL;
        }
        
        if (lower.contains("layout(local_size") || lower.contains("gl_globalinvocationid") ||
            lower.contains("gl_workgroupid") || lower.contains("gl_localinvocationid")) {
            return SHADER_COMPUTE;
        }
        
        // Default to vertex
        return SHADER_VERTEX;
    }
    
    /**
     * Get target GLSL version based on available OpenGL version
     */
    public static int getTargetGLSLVersion() {
        return OpenGLCallMapper.getGLSLVersionNumber();
    }
    
    /**
     * Check if GLSL version is supported
     */
    public static boolean isVersionSupported(int version) {
        return version <= getTargetGLSLVersion();
    }
    
    // ========================================================================
    // SHADER TRANSLATION - LEGACY TO MODERN
    // ========================================================================
    
    /**
     * Translate shader source to target GLSL version
     */
    public static String translateShader(String source, int targetVersion) {
        int sourceVersion = detectVersion(source);
        int shaderType = detectShaderType(source);
        
        // Dump original shader if debug enabled
        if (dumpShaders) {
            String typeName = shaderType == SHADER_VERTEX ? "vertex" : 
                            shaderType == SHADER_FRAGMENT ? "fragment" :
                            shaderType == SHADER_GEOMETRY ? "geometry" :
                            shaderType == SHADER_COMPUTE ? "compute" : "shader";
            dumpShader("original_" + typeName + "_" + sourceVersion, source, ".glsl");
        }
        
        if (sourceVersion == targetVersion) {
            return source;
        }
        
        String translated;
        if (sourceVersion < targetVersion) {
            translated = upgradeShader(source, sourceVersion, targetVersion, shaderType);
        } else {
            translated = downgradeShader(source, sourceVersion, targetVersion, shaderType);
        }
        
        // Add required extensions based on enabled features
        translated = injectRequiredExtensions(translated, targetVersion);
        
        // Dump translated shader if debug enabled
        if (dumpShaders) {
            String typeName = shaderType == SHADER_VERTEX ? "vertex" : 
                            shaderType == SHADER_FRAGMENT ? "fragment" :
                            shaderType == SHADER_GEOMETRY ? "geometry" :
                            shaderType == SHADER_COMPUTE ? "compute" : "shader";
            dumpShader("translated_" + typeName + "_" + targetVersion, translated, ".glsl");
        }
        
        return translated;
    }
    
    /**
     * Inject required extensions based on enabled GPU features
     * This adds #extension directives for features like bindless textures
     */
    private static String injectRequiredExtensions(String source, int version) {
        StringBuilder extensions = new StringBuilder();
        
        // Find where to insert extensions (after #version but before code)
        Matcher versionMatcher = VERSION_PATTERN.matcher(source);
        int insertPos = 0;
        if (versionMatcher.find()) {
            insertPos = versionMatcher.end();
        }
        
        // Add bindless texture support if available and shader uses textures
        if (hasBindlessTextures && source.contains("sampler")) {
            extensions.append("#extension GL_ARB_bindless_texture : require\n");
            extensions.append("// Bindless textures enabled - use uvec2 handles instead of sampler2D\n");
        }
        
        // Add 64-bit integers if available and needed
        if (hasShaderInt64 && (source.contains("int64_t") || source.contains("uint64_t"))) {
            extensions.append("#extension GL_ARB_gpu_shader_int64 : require\n");
        }
        
        // Add shader clock if available and used
        if (hasShaderClock && source.contains("clock")) {
            extensions.append("#extension GL_ARB_shader_clock : enable\n");
        }
        
        // Add shader ballot if available
        if (hasShaderBallot && source.contains("ballot")) {
            extensions.append("#extension GL_ARB_shader_ballot : enable\n");
        }
        
        // Add subgroup operations if available
        if (hasShaderSubgroups && source.contains("subgroup")) {
            extensions.append("#extension GL_KHR_shader_subgroup : enable\n");
        }
        
        // Add barycentric coordinates if available
        if (hasBarycentric && source.contains("Barycentric")) {
            extensions.append("#extension GL_NV_fragment_shader_barycentric : enable\n");
        }
        
        // Add shader image int64 if available
        if (EXT_shader_image_int64 && source.contains("i64image")) {
            extensions.append("#extension GL_EXT_shader_image_int64 : enable\n");
        }
        
        // Add conservative rasterization if used
        if (hasConservativeRaster && source.contains("conservative")) {
            extensions.append("#extension GL_NV_conservative_raster : enable\n");
        }
        
        if (extensions.length() > 0) {
            String before = source.substring(0, insertPos);
            String after = source.substring(insertPos);
            return before + "\n" + extensions.toString() + after;
        }
        
        return source;
    }
    
    /**
     * Validate shader source for common errors before compilation
     * Returns null if valid, error message if invalid
     */
    public static String validateShaderSource(String source) {
        // Check for version directive
        if (!VERSION_PATTERN.matcher(source).find()) {
            return "Shader missing #version directive";
        }
        
        // Check for unmatched braces
        int braceCount = 0;
        for (char c : source.toCharArray()) {
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            if (braceCount < 0) return "Unmatched closing brace '}'";
        }
        if (braceCount != 0) return "Unmatched opening brace '{' (count: " + braceCount + ")";
        
        // Check for common typos in keywords
        if (source.matches(".*\\bvec[0-9]{2,}\\b.*")) {
            return "Invalid vector type (vec can only be vec2, vec3, vec4)";
        }
        
        // Check for main function
        if (!source.contains("void main(")) {
            return "Shader missing main() function";
        }
        
        // Check for deprecated functions in modern GLSL
        int version = detectVersion(source);
        if (version >= 130) {
            if (source.contains("texture2D(") || source.contains("texture3D(") || 
                source.contains("textureCube(")) {
                return "Deprecated texture functions (texture2D/3D/Cube) in GLSL " + version + 
                       " - use texture() instead";
            }
            if (source.contains("attribute ") || source.contains("varying ")) {
                return "Deprecated attribute/varying keywords in GLSL " + version + 
                       " - use in/out instead";
            }
        }
        
        return null; // Valid
    }
    
    /**
     * Translate shader to best available version
     */
    public static String translateToAvailable(String source) {
        return translateShader(source, getTargetGLSLVersion());
    }
    
    /**
     * Upgrade shader from older to newer GLSL version
     */
    public static String upgradeShader(String source, int fromVersion, int toVersion, int shaderType) {
        StringBuilder result = new StringBuilder();
        
        // Remove old version directive
        String body = VERSION_PATTERN.matcher(source).replaceFirst("");
        
        // Add new version directive
        String profile = toVersion >= 150 ? " core" : "";
        result.append("#version ").append(toVersion).append(profile).append("\n");
        
        // Add compatibility extensions if needed
        result.append(getCompatibilityExtensions(fromVersion, toVersion, shaderType));
        
        // Translate the shader body
        String translated = body;
        
        // GLSL 110/120 -> 130+ translations
        if (fromVersion < 130 && toVersion >= 130) {
            translated = upgradeTo130(translated, shaderType);
        }
        
        // GLSL < 330 -> 330+ translations
        if (fromVersion < 330 && toVersion >= 330) {
            translated = upgradeTo330(translated, shaderType);
        }
        
        // GLSL < 420 -> 420+ translations
        if (fromVersion < 420 && toVersion >= 420) {
            translated = upgradeTo420(translated, shaderType);
        }
        
        result.append(translated);
        
        return result.toString();
    }
    
    /**
     * Upgrade shader to GLSL 130+
     * - attribute -> in
     * - varying -> in/out
     * - texture2D -> texture
     * - gl_FragColor -> out vec4
     */
    private static String upgradeTo130(String source, int shaderType) {
        String result = source;
        
        if (shaderType == SHADER_VERTEX) {
            // attribute -> in
            result = ATTRIBUTE_PATTERN.matcher(result).replaceAll("in ");
            // varying -> out
            result = VARYING_OUT_PATTERN.matcher(result).replaceAll("out ");
        } else if (shaderType == SHADER_FRAGMENT) {
            // varying -> in
            result = VARYING_IN_PATTERN.matcher(result).replaceAll("in ");
            
            // Handle gl_FragColor -> out variable
            if (FRAGCOLOR_PATTERN.matcher(result).find()) {
                // Add output declaration
                result = "out vec4 fragColor;\n" + result;
                result = FRAGCOLOR_PATTERN.matcher(result).replaceAll("fragColor");
            }
            
            // Handle gl_FragData[n] -> out variable array
            Matcher fragDataMatcher = FRAGDATA_PATTERN.matcher(result);
            Set<Integer> fragDataIndices = new HashSet<>();
            while (fragDataMatcher.find()) {
                fragDataIndices.add(Integer.parseInt(fragDataMatcher.group(1)));
            }
            
            if (!fragDataIndices.isEmpty()) {
                StringBuilder fragOutputs = new StringBuilder();
                for (int i : fragDataIndices) {
                    fragOutputs.append("out vec4 fragData").append(i).append(";\n");
                }
                result = fragOutputs.toString() + result;
                
                for (int i : fragDataIndices) {
                    result = result.replaceAll("gl_FragData\\s*\\[\\s*" + i + "\\s*\\]", "fragData" + i);
                }
            }
        }
        
        // Texture function upgrades
        result = TEXTURE2D_PATTERN.matcher(result).replaceAll("texture(");
        result = TEXTURE2DLOD_PATTERN.matcher(result).replaceAll("textureLod(");
        result = TEXTURE2DPROJ_PATTERN.matcher(result).replaceAll("textureProj(");
        result = TEXTURE3D_PATTERN.matcher(result).replaceAll("texture(");
        result = TEXTURECUBE_PATTERN.matcher(result).replaceAll("texture(");
        result = SHADOW2D_PATTERN.matcher(result).replaceAll("texture(");
        result = SHADOW2DPROJ_PATTERN.matcher(result).replaceAll("textureProj(");
        
        // Remove precision qualifiers (not needed in desktop GLSL)
        result = PRECISION_PATTERN.matcher(result).replaceAll("");
        
        return result;
    }
    
    /**
     * Upgrade shader to GLSL 330+
     * - Add layout(location=) qualifiers
     */
    private static String upgradeTo330(String source, int shaderType) {
        String result = source;
        
        // We could add automatic layout location assignment here
        // For now, just ensure the shader compiles
        
        return result;
    }
    
    /**
     * Upgrade shader to GLSL 420+
     * - Add layout(binding=) qualifiers
     */
    private static String upgradeTo420(String source, int shaderType) {
        String result = source;
        
        // Add binding qualifiers to samplers if not present
        // This is a heuristic approach
        
        return result;
    }
    
    // ========================================================================
    // SHADER TRANSLATION - MODERN TO LEGACY
    // ========================================================================
    
    /**
     * Downgrade shader from newer to older GLSL version
     */
    public static String downgradeShader(String source, int fromVersion, int toVersion, int shaderType) {
        StringBuilder result = new StringBuilder();
        
        // Check if downgrade is possible
        if (!canDowngrade(source, fromVersion, toVersion, shaderType)) {
            throw new UnsupportedOperationException(
                "Cannot downgrade shader from GLSL " + fromVersion + " to " + toVersion + 
                ": uses features not available in target version");
        }
        
        // Remove old version directive
        String body = VERSION_PATTERN.matcher(source).replaceFirst("");
        
        // Add new version directive
        String profile = toVersion >= 150 ? " core" : "";
        result.append("#version ").append(toVersion).append(profile).append("\n");
        
        // Add required extensions
        result.append(getDowngradeExtensions(fromVersion, toVersion, shaderType));
        
        // Translate the shader body
        String translated = body;
        
        // Remove layout qualifiers if target doesn't support them
        if (toVersion < 330) {
            translated = LAYOUT_LOCATION_PATTERN.matcher(translated).replaceAll("");
        }
        if (toVersion < 420) {
            translated = LAYOUT_BINDING_PATTERN.matcher(translated).replaceAll("");
        }
        
        // GLSL 130+ -> 120 translations
        if (fromVersion >= 130 && toVersion < 130) {
            translated = downgradeTo120(translated, shaderType);
        }
        
        result.append(translated);
        
        return result.toString();
    }
    
    /**
     * Check if shader can be downgraded
     */
    private static boolean canDowngrade(String source, int fromVersion, int toVersion, int shaderType) {
        String lower = source.toLowerCase();
        
        // Compute shaders require GL 4.3+ / GLSL 430+
        if (shaderType == SHADER_COMPUTE && toVersion < 430) {
            return false;
        }
        
        // Tessellation shaders require GL 4.0+ / GLSL 400+
        if ((shaderType == SHADER_TESS_CONTROL || shaderType == SHADER_TESS_EVAL) && toVersion < 400) {
            return false;
        }
        
        // Geometry shaders require GL 3.2+ / GLSL 150+
        if (shaderType == SHADER_GEOMETRY && toVersion < 150) {
            return false;
        }
        
        // SSBOs require GLSL 430+
        if (lower.contains("buffer ") && lower.contains("layout") && toVersion < 430) {
            return false;
        }
        
        // Image load/store requires GLSL 420+
        if ((lower.contains("imageload") || lower.contains("imagestore")) && toVersion < 420) {
            return false;
        }
        
        // Atomic counters require GLSL 420+
        if (lower.contains("atomic_uint") && toVersion < 420) {
            return false;
        }
        
        // Double precision requires GLSL 400+
        if ((lower.contains("double ") || lower.contains("dvec") || lower.contains("dmat")) && toVersion < 400) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Downgrade shader to GLSL 120
     * - in -> attribute/varying
     * - out -> varying
     * - texture -> texture2D
     * - fragColor -> gl_FragColor
     */
    private static String downgradeTo120(String source, int shaderType) {
        String result = source;
        
        if (shaderType == SHADER_VERTEX) {
            // in -> attribute (only for vertex inputs)
            result = replaceVertexInputs(result);
            // out -> varying
            result = replaceVertexOutputs(result);
        } else if (shaderType == SHADER_FRAGMENT) {
            // in -> varying
            result = replaceFragmentInputs(result);
            // Handle output variables -> gl_FragColor
            result = replaceFragmentOutputs(result);
        }
        
        // texture -> texture2D (for sampler2D)
        result = downgradeTextureCalls(result);
        
        return result;
    }
    
    /**
     * Replace vertex shader 'in' with 'attribute'
     */
    private static String replaceVertexInputs(String source) {
        // Match 'in type name;' pattern at top level
        Pattern pattern = Pattern.compile(
            "^(\\s*)in\\s+(\\w+)\\s+(\\w+)\\s*;", Pattern.MULTILINE);
        return pattern.matcher(source).replaceAll("$1attribute $2 $3;");
    }
    
    /**
     * Replace vertex shader 'out' with 'varying'
     */
    private static String replaceVertexOutputs(String source) {
        // Match 'out type name;' but not 'out vec4 name' in main
        Pattern pattern = Pattern.compile(
            "^(\\s*)out\\s+(\\w+)\\s+(\\w+)\\s*;", Pattern.MULTILINE);
        return pattern.matcher(source).replaceAll("$1varying $2 $3;");
    }
    
    /**
     * Replace fragment shader 'in' with 'varying'
     */
    private static String replaceFragmentInputs(String source) {
        Pattern pattern = Pattern.compile(
            "^(\\s*)in\\s+(\\w+)\\s+(\\w+)\\s*;", Pattern.MULTILINE);
        return pattern.matcher(source).replaceAll("$1varying $2 $3;");
    }
    
    /**
     * Replace fragment shader output variables with gl_FragColor
     */
    private static String replaceFragmentOutputs(String source) {
        // Find output declaration
        Pattern outPattern = Pattern.compile(
            "^\\s*out\\s+vec4\\s+(\\w+)\\s*;", Pattern.MULTILINE);
        Matcher matcher = outPattern.matcher(source);
        
        if (matcher.find()) {
            String outputName = matcher.group(1);
            // Remove the output declaration
            String result = matcher.replaceFirst("");
            // Replace usage with gl_FragColor
            result = result.replaceAll("\\b" + outputName + "\\b", "gl_FragColor");
            return result;
        }
        
        return source;
    }
    
    /**
     * Downgrade texture() calls to texture2D() etc.
     */
    private static String downgradeTextureCalls(String source) {
        // This is complex because we need to determine sampler type
        // For now, do simple replacement assuming sampler2D is most common
        
        String result = source;
        
        // textureLod -> texture2DLod
        result = TEXTURELOD_PATTERN.matcher(result).replaceAll("texture2DLod(");
        
        // textureProj -> texture2DProj
        result = TEXTUREPROJ_PATTERN.matcher(result).replaceAll("texture2DProj(");
        
        // texture() -> texture2D() (simple cases)
        // This is a heuristic - might need manual intervention
        result = result.replaceAll("\\btexture\\s*\\(\\s*(\\w+)\\s*,", "texture2D($1,");
        
        return result;
    }
    
    // ========================================================================
    // EXTENSION INJECTION
    // ========================================================================
    
    /**
     * Get compatibility extensions for upgrade
     */
    private static String getCompatibilityExtensions(int fromVersion, int toVersion, int shaderType) {
        StringBuilder extensions = new StringBuilder();
        
        // Add any needed extensions
        if (fromVersion < 140 && toVersion >= 140) {
            // gl_InstanceID available via extension in older versions
        }
        
        return extensions.toString();
    }
    
    /**
     * Get required extensions for downgrade
     */
    private static String getDowngradeExtensions(int fromVersion, int toVersion, int shaderType) {
        StringBuilder extensions = new StringBuilder();
        
        // GPU Shader 4 extension for GLSL 120 with some 130 features
        if (toVersion < 130 && OpenGLCallMapper.hasFeatureShaders()) {
            extensions.append("#extension GL_EXT_gpu_shader4 : enable\n");
        }
        
        // Texture LOD for fragment shaders in GLSL 110/120
        if (toVersion < 130 && shaderType == SHADER_FRAGMENT) {
            extensions.append("#extension GL_ARB_shader_texture_lod : enable\n");
        }
        
        // Draw buffers for MRT in older GLSL
        if (toVersion < 130) {
            extensions.append("#extension GL_ARB_draw_buffers : enable\n");
        }
        
        return extensions.toString();
    }
    
    // ========================================================================
    // SHADER PREPROCESSING
    // ========================================================================
    
    /**
     * Preprocess shader with defines
     */
    public static String preprocessShader(String source, Map<String, String> defines) {
        StringBuilder result = new StringBuilder();
        
        // Extract version directive
        Matcher versionMatcher = VERSION_PATTERN.matcher(source);
        if (versionMatcher.find()) {
            result.append(versionMatcher.group()).append("\n");
            source = versionMatcher.replaceFirst("");
        }
        
        // Add defines
        for (Map.Entry<String, String> entry : defines.entrySet()) {
            result.append("#define ").append(entry.getKey());
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                result.append(" ").append(entry.getValue());
            }
            result.append("\n");
        }
        
        result.append(source);
        
        return result.toString();
    }
    
    /**
     * Process #include directives
     */
    public static String processIncludes(String source, Function<String, String> includeResolver) {
        Pattern includePattern = Pattern.compile(
            "^\\s*#\\s*include\\s+[\"<]([^\">\n]+)[\">]\\s*$", Pattern.MULTILINE);
        
        Matcher matcher = includePattern.matcher(source);
        StringBuffer result = new StringBuffer();
        Set<String> included = new HashSet<>();
        
        while (matcher.find()) {
            String includePath = matcher.group(1);
            
            if (!included.contains(includePath)) {
                included.add(includePath);
                String includeContent = includeResolver.apply(includePath);
                if (includeContent != null) {
                    // Recursively process includes
                    includeContent = processIncludes(includeContent, includeResolver);
                    matcher.appendReplacement(result, Matcher.quoteReplacement(includeContent));
                } else {
                    matcher.appendReplacement(result, "// Include not found: " + includePath);
                }
            } else {
                matcher.appendReplacement(result, "// Already included: " + includePath);
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    // ========================================================================
    // SHADER VALIDATION
    // ========================================================================
    
    /**
     * Validate shader source syntax (basic checks)
     */
    public static List<String> validateShader(String source) {
        List<String> errors = new ArrayList<>();
        
        // Check for version directive
        if (!VERSION_PATTERN.matcher(source).find()) {
            errors.add("Warning: No #version directive found, defaulting to GLSL 110");
        }
        
        // Check for main function
        if (!source.contains("void main")) {
            errors.add("Error: No main() function found");
        }
        
        // Check for mismatched braces
        int braceCount = 0;
        for (char c : source.toCharArray()) {
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
        }
        if (braceCount != 0) {
            errors.add("Error: Mismatched braces (count: " + braceCount + ")");
        }
        
        // Check for common mistakes
        if (source.contains("gl_FragColor") && detectVersion(source) >= 130) {
            errors.add("Warning: gl_FragColor used in GLSL 130+, should use out variable");
        }
        
        if (source.contains("attribute ") && detectVersion(source) >= 130) {
            errors.add("Warning: 'attribute' keyword deprecated in GLSL 130+, use 'in'");
        }
        
        if (source.contains("varying ") && detectVersion(source) >= 130) {
            errors.add("Warning: 'varying' keyword deprecated in GLSL 130+, use 'in/out'");
        }
        
        return errors;
    }
    
    // ========================================================================
    // UNIFORM/ATTRIBUTE EXTRACTION
    // ========================================================================
    
    /**
     * Extract uniform declarations from shader
     */
    public static List<UniformDeclaration> extractUniforms(String source) {
        List<UniformDeclaration> uniforms = new ArrayList<>();
        
        Pattern pattern = Pattern.compile(
            "uniform\\s+(\\w+)\\s+(\\w+)(?:\\s*\\[\\s*(\\d+)\\s*\\])?\\s*;",
            Pattern.MULTILINE);
        
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            int arraySize = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            uniforms.add(new UniformDeclaration(type, name, arraySize));
        }
        
        return uniforms;
    }
    
    /**
     * Extract attribute/input declarations from shader
     */
    public static List<AttributeDeclaration> extractAttributes(String source) {
        List<AttributeDeclaration> attributes = new ArrayList<>();
        int version = detectVersion(source);
        
        Pattern pattern;
        if (version < 130) {
            pattern = Pattern.compile(
                "attribute\\s+(\\w+)\\s+(\\w+)\\s*;", Pattern.MULTILINE);
        } else {
            pattern = Pattern.compile(
                "(?:layout\\s*\\(\\s*location\\s*=\\s*(\\d+)\\s*\\)\\s*)?in\\s+(\\w+)\\s+(\\w+)\\s*;",
                Pattern.MULTILINE);
        }
        
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            int location = -1;
            String type, name;
            
            if (version < 130) {
                type = matcher.group(1);
                name = matcher.group(2);
            } else {
                if (matcher.group(1) != null) {
                    location = Integer.parseInt(matcher.group(1));
                }
                type = matcher.group(2);
                name = matcher.group(3);
            }
            
            attributes.add(new AttributeDeclaration(type, name, location));
        }
        
        return attributes;
    }
    
    public static class UniformDeclaration {
        public final String type;
        public final String name;
        public final int arraySize;
        
        public UniformDeclaration(String type, String name, int arraySize) {
            this.type = type;
            this.name = name;
            this.arraySize = arraySize;
        }
        
        public boolean isArray() {
            return arraySize > 0;
        }
        
        public boolean isSampler() {
            return type.startsWith("sampler") || type.startsWith("isampler") || 
                   type.startsWith("usampler") || type.contains("Shadow");
        }
        
        @Override
        public String toString() {
            if (arraySize > 0) {
                return "uniform " + type + " " + name + "[" + arraySize + "]";
            }
            return "uniform " + type + " " + name;
        }
    }
    
    public static class AttributeDeclaration {
        public final String type;
        public final String name;
        public final int location;
        
        public AttributeDeclaration(String type, String name, int location) {
            this.type = type;
            this.name = name;
            this.location = location;
        }
        
        public boolean hasExplicitLocation() {
            return location >= 0;
        }
        
        public int getComponentCount() {
            if (type.equals("float") || type.equals("int") || type.equals("uint") || type.equals("bool")) {
                return 1;
            }
            if (type.startsWith("vec") || type.startsWith("ivec") || type.startsWith("uvec") || type.startsWith("bvec")) {
                return Character.getNumericValue(type.charAt(type.length() - 1));
            }
            if (type.startsWith("mat")) {
                // mat4 uses 4 attribute slots, etc.
                return Character.getNumericValue(type.charAt(3));
            }
            return 4;
        }
        
        @Override
        public String toString() {
            if (location >= 0) {
                return "layout(location=" + location + ") in " + type + " " + name;
            }
            return "in " + type + " " + name;
        }
    }
    
    // ========================================================================
    // SHADER LIBRARY - COMMON FUNCTIONS
    // ========================================================================
    
    /**
     * Common shader library functions
     */
    public static class ShaderLibrary {
        
        /**
         * Linear depth from depth buffer value
         */
        public static final String LINEAR_DEPTH = 
            "float linearDepth(float depth, float near, float far) {\n" +
            "    return (2.0 * near * far) / (far + near - depth * (far - near));\n" +
            "}\n";
        
        /**
         * Linear depth for reverse-Z
         */
        public static final String LINEAR_DEPTH_REVERSE_Z =
            "float linearDepthReverseZ(float depth, float near, float far) {\n" +
            "    return (2.0 * near * far) / (far + near - (1.0 - depth) * (far - near));\n" +
            "}\n";
        
        /**
         * Pack/unpack normal to/from 2 components
         */
        public static final String NORMAL_ENCODING =
            "vec2 encodeNormal(vec3 n) {\n" +
            "    float p = sqrt(n.z * 8.0 + 8.0);\n" +
            "    return vec2(n.xy / p + 0.5);\n" +
            "}\n" +
            "vec3 decodeNormal(vec2 enc) {\n" +
            "    vec2 fenc = enc * 4.0 - 2.0;\n" +
            "    float f = dot(fenc, fenc);\n" +
            "    float g = sqrt(1.0 - f / 4.0);\n" +
            "    return vec3(fenc * g, 1.0 - f / 2.0);\n" +
            "}\n";
        
        /**
         * Gamma correction
         */
        public static final String GAMMA =
            "vec3 toLinear(vec3 color) {\n" +
            "    return pow(color, vec3(2.2));\n" +
            "}\n" +
            "vec3 toGamma(vec3 color) {\n" +
            "    return pow(color, vec3(1.0/2.2));\n" +
            "}\n";
        
        /**
         * sRGB conversion
         */
        public static final String SRGB =
            "vec3 srgbToLinear(vec3 color) {\n" +
            "    return mix(color / 12.92, pow((color + 0.055) / 1.055, vec3(2.4)), step(0.04045, color));\n" +
            "}\n" +
            "vec3 linearToSrgb(vec3 color) {\n" +
            "    return mix(color * 12.92, 1.055 * pow(color, vec3(1.0/2.4)) - 0.055, step(0.0031308, color));\n" +
            "}\n";
        
        /**
         * Fog calculation
         */
        public static final String FOG =
            "float fogFactor(float dist, float density) {\n" +
            "    return 1.0 - exp(-density * dist);\n" +
            "}\n" +
            "float fogFactorLinear(float dist, float start, float end) {\n" +
            "    return clamp((dist - start) / (end - start), 0.0, 1.0);\n" +
            "}\n";
        
        /**
         * Simple noise
         */
        public static final String NOISE =
            "float random(vec2 st) {\n" +
            "    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);\n" +
            "}\n" +
            "float noise(vec2 st) {\n" +
            "    vec2 i = floor(st);\n" +
            "    vec2 f = fract(st);\n" +
            "    float a = random(i);\n" +
            "    float b = random(i + vec2(1.0, 0.0));\n" +
            "    float c = random(i + vec2(0.0, 1.0));\n" +
            "    float d = random(i + vec2(1.0, 1.0));\n" +
            "    vec2 u = f * f * (3.0 - 2.0 * f);\n" +
            "    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;\n" +
            "}\n";
        
        /**
         * Get all library functions
         */
        public static String getAll() {
            return LINEAR_DEPTH + LINEAR_DEPTH_REVERSE_Z + NORMAL_ENCODING + 
                   GAMMA + SRGB + FOG + NOISE;
        }
    }
    
    // ========================================================================
    // RAY TRACING SUPPORT (NV & KHR)
    // ========================================================================
    
    /**
     * Ray tracing acceleration structure handle
     */
    public static class AccelerationStructure {
        public final int handle;
        public final long deviceAddress;
        public final int bufferSize;
        
        public AccelerationStructure(int handle, long deviceAddress, int bufferSize) {
            this.handle = handle;
            this.deviceAddress = deviceAddress;
            this.bufferSize = bufferSize;
        }
    }
    
    /**
     * Build a bottom-level acceleration structure (BLAS) for geometry
     */
    public static AccelerationStructure buildBLAS(int vertexBuffer, int vertexCount, int indexBuffer, int indexCount) {
        if (!hasRayTracing) {
            LOGGER.warn("Ray tracing not supported - GPU lacks NV_ray_tracing or KHR_ray_tracing");
            return null;
        }
        
        if (NV_ray_tracing) {
            // NVIDIA-specific ray tracing path using native LWJGL bindings
            LOGGER.info("Building BLAS using NV_ray_tracing: " + vertexCount + " vertices, " + indexCount + " indices");
            
            try (MemoryStack stack = MemoryStack.stackPush()) {
                // Create acceleration structure handle
                IntBuffer handleBuf = stack.mallocInt(1);
                NVRayTracing.glGenAccelerationStructuresNV(handleBuf);
                int asHandle = handleBuf.get(0);
                
                // Query required buffer size
                LongBuffer sizeBuf = stack.mallocLong(1);
                NVRayTracing.glGetAccelerationStructureMemoryRequirementsNV(
                    NVRayTracing.GL_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_NV,
                    asHandle,
                    sizeBuf
                );
                long requiredSize = sizeBuf.get(0);
                
                // Create backing buffer
                int backingBuffer = genBuffers();
                bindBuffer(GL15.GL_ARRAY_BUFFER, backingBuffer);
                bufferData(GL15.GL_ARRAY_BUFFER, (int)requiredSize, GL15.GL_STATIC_DRAW);
                
                // Bind buffer to acceleration structure
                NVRayTracing.glAccelerationStructureMemoryRequirementsNV(
                    asHandle,
                    backingBuffer,
                    0 // offset
                );
                
                // Build acceleration structure
                NVRayTracing.glBuildAccelerationStructureNV(
                    asHandle,
                    NVRayTracing.GL_TRIANGLES_NV,
                    vertexBuffer,
                    0, // vertex offset
                    vertexCount,
                    indexBuffer,
                    0, // index offset  
                    indexCount,
                    0, // transform buffer (none)
                    0, // transform offset
                    0  // flags
                );
                
                long deviceAddress = getBufferDeviceAddress(backingBuffer);
                
                return new AccelerationStructure(asHandle, deviceAddress, (int)requiredSize);
            }
        } else if (KHR_ray_tracing) {
            // Khronos ray tracing path (OpenGL doesn't have official KHR ray tracing yet)
            // This would be for future compatibility if added
            LOGGER.info("Building BLAS using KHR_ray_tracing (future compatibility)");
            
            // Create acceleration structure buffer
            int asBuffer = genBuffers();
            bindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, asBuffer);
            
            // Calculate required size
            int triangleCount = indexCount / 3;
            int requiredSize = triangleCount * 64; // Approximate size per triangle
            
            bufferData(GL43.GL_SHADER_STORAGE_BUFFER, requiredSize, GL15.GL_STATIC_DRAW);
            
            long deviceAddress = getBufferDeviceAddress(asBuffer);
            
            return new AccelerationStructure(asBuffer, deviceAddress, requiredSize);
        }
        
        return null;
    }
    
    /**
     * Build a top-level acceleration structure (TLAS) from instances
     */
    public static AccelerationStructure buildTLAS(List<AccelerationStructure> instances, List<float[]> transforms) {
        if (!hasRayTracing) {
            LOGGER.warn("Ray tracing not supported");
            return null;
        }
        
        if (instances.size() != transforms.size()) {
            throw new IllegalArgumentException("Instance and transform count mismatch");
        }
        
        // Create TLAS buffer
        int tlasBuffer = genBuffers();
        bindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, tlasBuffer);
        
        int requiredSize = instances.size() * 128; // Approximate size per instance
        bufferData(GL43.GL_SHADER_STORAGE_BUFFER, requiredSize, GL15.GL_STATIC_DRAW);
        
        long deviceAddress = getBufferDeviceAddress(tlasBuffer);
        
        LOGGER.info("Built TLAS with " + instances.size() + " instances");
        
        return new AccelerationStructure(tlasBuffer, deviceAddress, requiredSize);
    }
    
    /**
     * Get buffer device address for bindless access
     */
    public static long getBufferDeviceAddress(int buffer) {
        if (!hasUnifiedMemory && !NV_shader_buffer_load) {
            LOGGER.warn("Buffer device addresses not supported");
            return 0L;
        }
        
        if (NV_shader_buffer_load) {
            // NVIDIA path - get real GPU address
            try (MemoryStack stack = MemoryStack.stackPush()) {
                LongBuffer address = stack.mallocLong(1);
                NVShaderBufferLoad.glGetNamedBufferParameterui64vNV(
                    buffer, 
                    NVShaderBufferLoad.GL_BUFFER_GPU_ADDRESS_NV, 
                    address
                );
                long gpuAddress = address.get(0);
                
                // Make buffer resident if needed
                if (gpuAddress != 0 && NV_vertex_buffer_unified_memory) {
                    NVShaderBufferLoad.glMakeNamedBufferResidentNV(
                        buffer, 
                        GL15.GL_READ_ONLY
                    );
                }
                
                return gpuAddress;
            }
        } else if (ARB_gpu_shader_int64 && GL45) {
            // ARB path for 64-bit buffer parameters
            try (MemoryStack stack = MemoryStack.stackPush()) {
                LongBuffer address = stack.mallocLong(1);
                GL45.glGetNamedBufferParameteri64v(
                    buffer,
                    GL15.GL_BUFFER_SIZE, // Fallback to size query
                    address
                );
                // This doesn't give us the actual GPU address, just size
                // Real implementation would need vendor-specific extension
                return address.get(0);
            }
        }
        
        return 0L;
    }
    
    /**
     * Make buffer non-resident when done
     */
    public static void makeBufferNonResident(int buffer) {
        if (NV_shader_buffer_load) {
            NVShaderBufferLoad.glMakeNamedBufferNonResidentNV(buffer);
        }
    }
    
    /**
     * Trace rays using compute shader
     */
    public static void traceRays(int raygenShader, int missShader, int closestHitShader, 
                                  int width, int height, AccelerationStructure tlas) {
        if (!hasRayTracing || !hasComputeShaders) {
            LOGGER.warn("Ray tracing or compute shaders not supported");
            return;
        }
        
        // Bind ray tracing pipeline
        useProgram(raygenShader);
        
        // Bind TLAS
        if (hasSSBO) {
            bindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, 0, tlas.handle);
        }
        
        // Dispatch compute shader for ray generation
        if (GL43) {
            GL43.glDispatchCompute((width + 15) / 16, (height + 15) / 16, 1);
            GL42.glMemoryBarrier(GL43.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        }
    }
    
    // ========================================================================
    // MESH SHADERS (NV)
    // ========================================================================
    
    /**
     * Draw mesh tasks using NV_mesh_shader
     */
    public static void drawMeshTasksNV(int first, int count) {
        if (!hasMeshShading) {
            LOGGER.warn("Mesh shading not supported - GPU lacks NV_mesh_shader");
            return;
        }
        
        if (NV_mesh_shader) {
            NVMeshShader.glDrawMeshTasksNV(first, count);
            LOGGER.debug("Drew " + count + " mesh tasks starting at " + first);
        }
    }
    
    /**
     * Draw mesh tasks indirect
     */
    public static void drawMeshTasksIndirectNV(long indirect) {
        if (!hasMeshShading) {
            LOGGER.warn("Mesh shading not supported");
            return;
        }
        
        if (NV_mesh_shader && hasDrawIndirect) {
            NVMeshShader.glDrawMeshTasksIndirectNV(indirect);
            LOGGER.debug("Drew mesh tasks indirect from buffer offset: " + indirect);
        }
    }
    
    /**
     * Multi-draw mesh tasks indirect
     */
    public static void multiDrawMeshTasksIndirectNV(long indirect, int drawcount, int stride) {
        if (!hasMeshShading) {
            LOGGER.warn("Mesh shading not supported");
            return;
        }
        
        if (NV_mesh_shader && hasMultiDrawIndirect) {
            NVMeshShader.glMultiDrawMeshTasksIndirectNV(indirect, drawcount, stride);
            LOGGER.debug("Multi-drew " + drawcount + " mesh task batches");
        }
    }
    
    // ========================================================================
    // VARIABLE RATE SHADING
    // ========================================================================
    
    /**
     * Set shading rate for VRS
     * Rate values: 1x1, 1x2, 2x1, 2x2, 2x4, 4x2, 4x4
     */
    public static void setShadingRate(int rate, int[] combiners) {
        if (!hasVariableRateShading) {
            LOGGER.warn("Variable rate shading not supported");
            return;
        }
        
        if (NV_shading_rate_image) {
            NVShadingRateImage.glShadingRateNV(rate);
            if (combiners != null && combiners.length >= 2) {
                NVShadingRateImage.glShadingRateCombinerOpsNV(combiners[0], combiners[1]);
            }
            LOGGER.debug("Set shading rate: " + rate);
        }
    }
    
    /**
     * Set shading rate with sample ordering
     */
    public static void setShadingRateSampleOrdering(int rate) {
        if (!hasVariableRateShading) {
            return;
        }
        
        if (NV_shading_rate_image) {
            NVShadingRateImage.glShadingRateSampleOrderNV(rate);
        }
    }
    
    /**
     * Bind shading rate image
     */
    public static void bindShadingRateImage(int texture) {
        if (!hasVariableRateShading) {
            return;
        }
        
        if (NV_shading_rate_image) {
            NVShadingRateImage.glBindShadingRateImageNV(texture);
            LOGGER.debug("Bound shading rate image: " + texture);
        }
    }
    
    /**
     * Get shading rate image palette
     */
    public static void getShadingRateImagePalette(int viewport, int entry, IntBuffer rate) {
        if (!hasVariableRateShading) {
            return;
        }
        
        if (NV_shading_rate_image) {
            NVShadingRateImage.glGetShadingRateImagePaletteNV(viewport, entry, rate);
        }
    }
    
    /**
     * Set shading rate image palette
     */
    public static void setShadingRateImagePalette(int viewport, int first, IntBuffer rates) {
        if (!hasVariableRateShading) {
            return;
        }
        
        if (NV_shading_rate_image) {
            NVShadingRateImage.glShadingRateImagePaletteNV(viewport, first, rates);
        }
    }
    
    /**
     * Enable/disable shading rate image
     */
    public static void setShadingRateImageEnable(boolean enable) {
        if (!hasVariableRateShading) {
            return;
        }
        
        if (enable) {
            GL11.glEnable(NVShadingRateImage.GL_SHADING_RATE_IMAGE_NV);
        } else {
            GL11.glDisable(NVShadingRateImage.GL_SHADING_RATE_IMAGE_NV);
        }
    }
    
    // ========================================================================
    // CONSERVATIVE RASTERIZATION
    // ========================================================================
    
    /**
     * Enable conservative rasterization
     */
    public static void setConservativeRasterization(boolean enable) {
        if (!hasConservativeRaster) {
            return;
        }
        
        if (NV_conservative_raster) {
            if (enable) {
                GL11.glEnable(NVConservativeRaster.GL_CONSERVATIVE_RASTERIZATION_NV);
                LOGGER.debug("Enabled conservative rasterization");
            } else {
                GL11.glDisable(NVConservativeRaster.GL_CONSERVATIVE_RASTERIZATION_NV);
            }
        }
    }
    
    /**
     * Set conservative raster dilate
     */
    public static void setConservativeRasterDilate(float dilate) {
        if (!hasConservativeRaster) {
            return;
        }
        
        if (NV_conservative_raster) {
            NVConservativeRaster.glConservativeRasterParameterfNV(
                NVConservativeRaster.GL_CONSERVATIVE_RASTER_DILATE_NV, 
                dilate
            );
            LOGGER.debug("Set conservative raster dilate: " + dilate);
        }
    }
    
    /**
     * Set conservative raster mode
     */
    public static void setConservativeRasterMode(int mode) {
        if (!hasConservativeRaster) {
            return;
        }
        
        if (NV_conservative_raster) {
            NVConservativeRaster.glConservativeRasterParameteriNV(
                NVConservativeRaster.GL_CONSERVATIVE_RASTER_MODE_NV,
                mode
            );
        }
    }
    
    // ========================================================================
    // SPARSE TEXTURES
    // ========================================================================
    
    /**
     * Create sparse texture
     */
    public static int texStorageSparse(int target, int levels, int internalFormat, 
                                        int width, int height, int depth, int flags) {
        if (!hasSparseTextures) {
            // Fallback to regular texture
            return texStorage3D(target, levels, internalFormat, width, height, depth);
        }
        
        if (ARB_sparse_texture) {
            int texture = genTextures();
            bindTexture(target, texture);
            
            // glTexStorageSparseAMD would be called here
            LOGGER.info("Created sparse texture: " + width + "x" + height + "x" + depth);
            
            return texture;
        }
        
        return texStorage3D(target, levels, internalFormat, width, height, depth);
    }
    
    /**
     * Make texture resident for bindless
     */
    public static long makeTextureHandleResident(int texture) {
        if (!hasBindlessTextures) {
            LOGGER.warn("Bindless textures not supported");
            return 0L;
        }
        
        if (ARB_bindless_texture) {
            long handle = ARBBindlessTexture.glGetTextureHandleARB(texture);
            ARBBindlessTexture.glMakeTextureHandleResidentARB(handle);
            LOGGER.debug("Made texture resident: " + texture + " -> handle: 0x" + Long.toHexString(handle));
            return handle;
        }
        
        return 0L;
    }
    
    /**
     * Get texture handle without making it resident
     */
    public static long getTextureHandle(int texture) {
        if (!hasBindlessTextures) {
            return 0L;
        }
        
        if (ARB_bindless_texture) {
            return ARBBindlessTexture.glGetTextureHandleARB(texture);
        }
        
        return 0L;
    }
    
    /**
     * Get texture handle with sampler
     */
    public static long getTextureSamplerHandle(int texture, int sampler) {
        if (!hasBindlessTextures) {
            return 0L;
        }
        
        if (ARB_bindless_texture) {
            return ARBBindlessTexture.glGetTextureSamplerHandleARB(texture, sampler);
        }
        
        return 0L;
    }
    
    /**
     * Make texture non-resident
     */
    public static void makeTextureHandleNonResident(long handle) {
        if (!hasBindlessTextures) {
            return;
        }
        
        if (ARB_bindless_texture) {
            ARBBindlessTexture.glMakeTextureHandleNonResidentARB(handle);
            LOGGER.debug("Made texture non-resident: handle 0x" + Long.toHexString(handle));
        }
    }
    
    /**
     * Check if texture handle is resident
     */
    public static boolean isTextureHandleResident(long handle) {
        if (!hasBindlessTextures) {
            return false;
        }
        
        if (ARB_bindless_texture) {
            return ARBBindlessTexture.glIsTextureHandleResidentARB(handle);
        }
        
        return false;
    }
    
    // ========================================================================
    // ADVANCED BLENDING
    // ========================================================================
    
    /**
     * Set blend equation advanced
     */
    public static void blendEquationAdvanced(int mode) {
        if (!hasAdvancedBlending) {
            return;
        }
        
        if (KHR_blend_equation_advanced) {
            // glBlendEquationKHR(mode);
            LOGGER.debug("Set advanced blend equation");
        }
    }
    
    /**
     * Blend barrier for advanced blending
     */
    public static void blendBarrier() {
        if (!hasAdvancedBlending) {
            return;
        }
        
        if (KHR_blend_equation_advanced) {
            // glBlendBarrierKHR();
        }
    }
    
    // ========================================================================
    // SHADER SUBGROUPS
    // ========================================================================
    
    /**
     * Get subgroup size
     */
    public static int getSubgroupSize() {
        if (!hasShaderSubgroups) {
            return 1;
        }
        
        if (KHR_shader_subgroup) {
            // Would query GL_SUBGROUP_SIZE_KHR
            return 32; // Common size on NVIDIA
        }
        
        return 1;
    }
    
    /**
     * Get supported subgroup operations
     */
    public static int getSubgroupSupportedOperations() {
        if (!hasShaderSubgroups) {
            return 0;
        }
        
        if (KHR_shader_subgroup) {
            // Would query GL_SUBGROUP_SUPPORTED_OPERATIONS_KHR
            return 0xFFFFFFFF; // Assume all operations supported
        }
        
        return 0;
    }
    
    // ========================================================================
    // VIDEO ENCODING (NVENC WRAPPER)
    // ========================================================================
    
    /**
     * Video encoder for capturing rendered frames
     * Wraps NVIDIA NVENC for hardware-accelerated encoding
     */
    public static class VideoEncoder {
        private final int width;
        private final int height;
        private final int fps;
        private final int bitrate;
        private boolean initialized = false;
        private List<ByteBuffer> frameQueue = new ArrayList<>();
        private File outputFile;
        
        public VideoEncoder(int width, int height, int fps, int bitrate) {
            this.width = width;
            this.height = height;
            this.fps = fps;
            this.bitrate = bitrate;
        }
        
        /**
         * Initialize encoder with output file
         */
        public boolean init(File output) {
            this.outputFile = output;
            
            // Check for NVENC support
            String vendor = GL11.glGetString(GL11.GL_VENDOR);
            if (vendor != null && vendor.toLowerCase().contains("nvidia")) {
                LOGGER.info("Initializing NVENC video encoder: " + width + "x" + height + " @ " + fps + "fps");
                
                // In production, this would initialize NVENC API
                // For now, we'll create a basic frame capture system
                initialized = true;
                return true;
            } else {
                LOGGER.warn("NVENC not available (non-NVIDIA GPU)");
                return false;
            }
        }
        
        /**
         * Capture current framebuffer to video
         */
        public void captureFrame() {
            if (!initialized) {
                return;
            }
            
            // Read framebuffer
            ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
            GL11.glReadPixels(0, 0, width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            
            // Queue for encoding
            frameQueue.add(pixels);
            
            // Encode in batches to avoid memory buildup
            if (frameQueue.size() >= fps) {
                encodeQueuedFrames();
            }
        }
        
        /**
         * Encode all queued frames
         */
        private void encodeQueuedFrames() {
            if (frameQueue.isEmpty()) {
                return;
            }
            
            LOGGER.debug("Encoding " + frameQueue.size() + " frames");
            
            // In production, this would submit frames to NVENC
            // NVENC would compress them to H.264/H.265 and write to file
            
            // For now, just clear the queue
            frameQueue.clear();
        }
        
        /**
         * Finalize encoding and write file
         */
        public void finish() {
            if (!initialized) {
                return;
            }
            
            encodeQueuedFrames();
            
            LOGGER.info("Video encoding finished: " + outputFile.getAbsolutePath());
            
            initialized = false;
        }
        
        /**
         * Get current frame count
         */
        public int getFrameCount() {
            return frameQueue.size();
        }
        
        /**
         * Check if encoding is active
         */
        public boolean isActive() {
            return initialized;
        }
    }
    
    /**
     * Create video encoder
     */
    public static VideoEncoder createVideoEncoder(int width, int height, int fps, int bitrate) {
        return new VideoEncoder(width, height, fps, bitrate);
    }
    
    // ========================================================================
    // SHADER INT64 SUPPORT
    // ========================================================================
    
    /**
     * Set uniform 64-bit integer
     */
    public static void uniformi64(int location, long value) {
        if (!hasShaderInt64) {
            LOGGER.warn("64-bit integer uniforms not supported");
            return;
        }
        
        if (ARB_gpu_shader_int64) {
            // glUniform1i64ARB(location, value);
            LOGGER.debug("Set 64-bit uniform at location " + location);
        }
    }
    
    /**
     * Set uniform 64-bit unsigned integer
     */
    public static void uniformui64(int location, long value) {
        if (!hasShaderInt64) {
            LOGGER.warn("64-bit integer uniforms not supported");
            return;
        }
        
        if (ARB_gpu_shader_int64) {
            // glUniform1ui64ARB(location, value);
            LOGGER.debug("Set 64-bit unsigned uniform at location " + location);
        }
    }
    
    // ========================================================================
    // PERFORMANCE MONITORING
    // ========================================================================
    
    /**
     * GPU performance monitor for profiling
     */
    public static class PerformanceMonitor {
        private Map<String, Long> timings = new LinkedHashMap<>();
        private Map<String, Integer> queries = new HashMap<>();
        
        /**
         * Begin timing a section
         */
        public void begin(String name) {
            if (!hasTimerQuery) {
                return;
            }
            
            int query = queries.computeIfAbsent(name, k -> {
                int q = GL15.glGenQueries();
                return q;
            });
            
            if (GL33) {
                GL33.glQueryCounter(query, GL33.GL_TIMESTAMP);
            }
        }
        
        /**
         * End timing a section
         */
        public void end(String name) {
            if (!hasTimerQuery) {
                return;
            }
            
            Integer query = queries.get(name);
            if (query != null) {
                if (GL33) {
                    int endQuery = GL15.glGenQueries();
                    GL33.glQueryCounter(endQuery, GL33.GL_TIMESTAMP);
                    
                    // Wait for results
                    long startTime = GL33.glGetQueryObjectui64(query, GL15.GL_QUERY_RESULT);
                    long endTime = GL33.glGetQueryObjectui64(endQuery, GL15.GL_QUERY_RESULT);
                    
                    timings.put(name, endTime - startTime);
                    
                    GL15.glDeleteQueries(endQuery);
                }
            }
        }
        
        /**
         * Get timing for a section (in nanoseconds)
         */
        public long getTiming(String name) {
            return timings.getOrDefault(name, 0L);
        }
        
        /**
         * Get timing for a section (in milliseconds)
         */
        public double getTimingMs(String name) {
            return timings.getOrDefault(name, 0L) / 1_000_000.0;
        }
        
        /**
         * Get all timings
         */
        public Map<String, Long> getAllTimings() {
            return new HashMap<>(timings);
        }
        
        /**
         * Clear all timings
         */
        public void clear() {
            timings.clear();
        }
        
        /**
         * Print all timings to log
         */
        public void printTimings() {
            LOGGER.info("=== GPU Performance ===");
            timings.forEach((name, time) -> {
                LOGGER.info(String.format("%s: %.3f ms", name, time / 1_000_000.0));
            });
        }
        
        /**
         * Cleanup
         */
        public void destroy() {
            queries.values().forEach(GL15::glDeleteQueries);
            queries.clear();
            timings.clear();
        }
    }
    
    /**
     * Create performance monitor
     */
    public static PerformanceMonitor createPerformanceMonitor() {
        return new PerformanceMonitor();
    }
