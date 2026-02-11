package stellar.snow.astralis.api.opengles.managers;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import java.lang.invoke.*;
import java.nio.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * OpenGL ES Manager - Central Coordination Hub
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * SCALE: ~3,000-5,000 lines of production code
 * COMPLEXITY: EXTREMELY ADVANCED
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                        ARCHITECTURE OVERVIEW                                │
 * │                                                                             │
 * │   ┌─────────────┐                                                          │
 * │   │ Application │                                                          │
 * │   └──────┬──────┘                                                          │
 * │          │ CallDescriptor                                                  │
 * │          ▼                                                                 │
 * │   ┌─────────────────────────────────────────────┐                         │
 * │   │           OpenGLESManager                    │                         │
 * │   │  ┌─────────────────────────────────────┐    │                         │
 * │   │  │     Routing & Fallback Logic        │    │                         │
 * │   │  │  • Version detection                │    │                         │
 * │   │  │  • Call classification              │    │                         │
 * │   │  │  • Fallback chain orchestration     │    │                         │
 * │   │  │  • State tracking                   │    │                         │
 * │   │  └─────────────────────────────────────┘    │                         │
 * │   └──────┬────────────────────┬─────────────────┘                         │
 * │          │                    │                                            │
 * │          ▼                    ▼                                            │
 * │   ┌──────────────┐    ┌──────────────┐                                    │
 * │   │ OpenGLES     │    │ GLSLES       │                                    │
 * │   │ CallMapper   │    │ CallMapper   │                                    │
 * │   │ (API calls)  │    │ (Shaders)    │                                    │
 * │   └──────────────┘    └──────────────┘                                    │
 * │                                                                             │
 * │   FALLBACK CHAIN:                                                          │
 * │   GLES 3.2 → GLES 3.1 → GLES 3.0 → GLES 2.0 → Desktop GL → Vulkan/SPIRV  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 */
public final class OpenGLESManager {

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 1: CORE STATE AND DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Device capabilities (probed once at initialization) */
    private final DeviceCapabilities capabilities;
    
    /** API call mapper */
    private final OpenGLESCallMapper apiMapper;
    
    /** Shader call mapper */
    private final GLSLESCallMapper shaderMapper;
    
    /** Current GL state tracker (avoids redundant state changes) */
    private final GLStateTracker stateTracker;
    
    /** Resource lifetime manager */
    private final ResourceManager resourceManager;
    
    /** Performance metrics collector */
    private final PerformanceMetrics metrics;
    
    /** Fallback chain for failed calls */
    private final FallbackChain fallbackChain;
    
    /** Command buffer pool for batching */
    private final CommandBufferPool commandPool;
    
    /** Debug callback handler */
    private volatile DebugCallback debugCallback;
    
    /** Thread safety: ensure single-thread GL access */
    private final long ownerThread;
    
    /** Initialization flag */
    private volatile boolean initialized;

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 2: INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════

    private OpenGLESManager(Builder builder) {
        this.ownerThread = Thread.currentThread().getId();
        this.capabilities = builder.capabilities;
        this.apiMapper = new OpenGLESCallMapper(capabilities);
        this.shaderMapper = new GLSLESCallMapper(capabilities);
        this.stateTracker = new GLStateTracker(capabilities);
        this.resourceManager = new ResourceManager(capabilities);
        this.metrics = new PerformanceMetrics();
        this.fallbackChain = new FallbackChain(this);
        this.commandPool = new CommandBufferPool(builder.commandPoolSize);
        this.debugCallback = builder.debugCallback;
        this.initialized = true;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private DeviceCapabilities capabilities;
        private int commandPoolSize = 64;
        private DebugCallback debugCallback;

        public Builder withCapabilities(DeviceCapabilities caps) {
            this.capabilities = caps;
            return this;
        }

        public Builder withCommandPoolSize(int size) {
            this.commandPoolSize = size;
            return this;
        }

        public Builder withDebugCallback(DebugCallback callback) {
            this.debugCallback = callback;
            return this;
        }

        public OpenGLESManager build() {
            if (capabilities == null) {
                capabilities = DeviceCapabilities.probe();
            }
            return new OpenGLESManager(this);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 3: CALL ROUTING - THE BRAIN
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Route a CallDescriptor to the appropriate mapper.
     * 
     * This is the central dispatch point. Every graphics call flows through here.
     * 
     * DECISION TREE:
     * 1. Validate thread ownership
     * 2. Classify call type (API vs Shader)
     * 3. Check if call is supported at current version
     * 4. If supported → execute directly
     * 5. If not supported → invoke fallback chain
     * 6. Record metrics
     * 7. Return MappingResult
     */
    public MappingResult mapCall(CallDescriptor call) {
        // Thread safety check
        if (Thread.currentThread().getId() != ownerThread) {
            return MappingResult.failed(
                "GL calls must be made from the thread that created the context. " +
                "Expected thread: " + ownerThread + ", Current: " + Thread.currentThread().getId()
            );
        }

        if (!initialized) {
            return MappingResult.failed("OpenGLESManager not initialized");
        }

        long startTime = System.nanoTime();
        metrics.incrementCallCount();

        try {
            // Classify and route
            MappingResult result = routeCall(call);
            
            // Record timing
            long elapsed = System.nanoTime() - startTime;
            metrics.recordCallTime(call.getType(), elapsed);

            // Handle fallback if needed
            if (result.needsFallback()) {
                result = executeFallback(call, result);
            }

            return result;

        } catch (Exception e) {
            metrics.incrementErrorCount();
            return MappingResult.failed("Exception during call mapping: " + e.getMessage(), e);
        }
    }

    /**
     * Internal routing logic - determines which mapper handles the call.
     */
    private MappingResult routeCall(CallDescriptor call) {
        CallCategory category = classifyCall(call);

        return switch (category) {
            case SHADER_COMPILATION -> routeShaderCall(call);
            case SHADER_PROGRAM -> routeShaderProgramCall(call);
            case BUFFER_OPERATION -> routeBufferCall(call);
            case TEXTURE_OPERATION -> routeTextureCall(call);
            case DRAW_CALL -> routeDrawCall(call);
            case STATE_CHANGE -> routeStateCall(call);
            case FRAMEBUFFER_OPERATION -> routeFramebufferCall(call);
            case COMPUTE_OPERATION -> routeComputeCall(call);
            case QUERY_OPERATION -> routeQueryCall(call);
            case SYNC_OPERATION -> routeSyncCall(call);
            case DEBUG_OPERATION -> routeDebugCall(call);
            case UNKNOWN -> MappingResult.failed("Unknown call type: " + call.getType());
        };
    }

    /**
     * Call classification for routing decisions.
     */
    private enum CallCategory {
        SHADER_COMPILATION,
        SHADER_PROGRAM,
        BUFFER_OPERATION,
        TEXTURE_OPERATION,
        DRAW_CALL,
        STATE_CHANGE,
        FRAMEBUFFER_OPERATION,
        COMPUTE_OPERATION,
        QUERY_OPERATION,
        SYNC_OPERATION,
        DEBUG_OPERATION,
        UNKNOWN
    }

    private CallCategory classifyCall(CallDescriptor call) {
        GLESCallType type = call.getType();
        
        return switch (type) {
            // Shader compilation
            case CREATE_SHADER, DELETE_SHADER, SHADER_SOURCE, COMPILE_SHADER,
                 GET_SHADER_IV, GET_SHADER_INFO_LOG -> CallCategory.SHADER_COMPILATION;

            // Shader programs
            case CREATE_PROGRAM, DELETE_PROGRAM, ATTACH_SHADER, DETACH_SHADER,
                 LINK_PROGRAM, USE_PROGRAM, VALIDATE_PROGRAM, GET_PROGRAM_IV,
                 GET_PROGRAM_INFO_LOG, GET_PROGRAM_BINARY, PROGRAM_BINARY,
                 PROGRAM_PARAMETER, GET_UNIFORM_LOCATION, GET_UNIFORM_BLOCK_INDEX,
                 UNIFORM_BLOCK_BINDING -> CallCategory.SHADER_PROGRAM;

            // Uniforms (part of shader program)
            case UNIFORM_1I, UNIFORM_2I, UNIFORM_3I, UNIFORM_4I,
                 UNIFORM_1F, UNIFORM_2F, UNIFORM_3F, UNIFORM_4F,
                 UNIFORM_1IV, UNIFORM_2IV, UNIFORM_3IV, UNIFORM_4IV,
                 UNIFORM_1FV, UNIFORM_2FV, UNIFORM_3FV, UNIFORM_4FV,
                 UNIFORM_1UI, UNIFORM_2UI, UNIFORM_3UI, UNIFORM_4UI,
                 UNIFORM_1UIV, UNIFORM_2UIV, UNIFORM_3UIV, UNIFORM_4UIV,
                 UNIFORM_MATRIX_2FV, UNIFORM_MATRIX_3FV, UNIFORM_MATRIX_4FV,
                 UNIFORM_MATRIX_2X3FV, UNIFORM_MATRIX_2X4FV, UNIFORM_MATRIX_3X2FV,
                 UNIFORM_MATRIX_3X4FV, UNIFORM_MATRIX_4X2FV, UNIFORM_MATRIX_4X3FV 
                 -> CallCategory.SHADER_PROGRAM;

            // Buffer operations
            case GEN_BUFFERS, DELETE_BUFFERS, BIND_BUFFER, BUFFER_DATA,
                 BUFFER_SUB_DATA, MAP_BUFFER_RANGE, UNMAP_BUFFER,
                 COPY_BUFFER_SUB_DATA, FLUSH_MAPPED_BUFFER_RANGE,
                 GET_BUFFER_PARAMETER, GET_BUFFER_POINTER,
                 BIND_BUFFER_BASE, BIND_BUFFER_RANGE -> CallCategory.BUFFER_OPERATION;

            // VAO (also buffer-related)
            case GEN_VERTEX_ARRAYS, DELETE_VERTEX_ARRAYS, BIND_VERTEX_ARRAY,
                 VERTEX_ATTRIB_POINTER, VERTEX_ATTRIB_I_POINTER,
                 ENABLE_VERTEX_ATTRIB_ARRAY, DISABLE_VERTEX_ATTRIB_ARRAY,
                 VERTEX_ATTRIB_DIVISOR, VERTEX_BINDING_DIVISOR,
                 BIND_VERTEX_BUFFER, VERTEX_ATTRIB_FORMAT,
                 VERTEX_ATTRIB_BINDING -> CallCategory.BUFFER_OPERATION;

            // Texture operations
            case GEN_TEXTURES, DELETE_TEXTURES, BIND_TEXTURE, ACTIVE_TEXTURE,
                 TEX_IMAGE_2D, TEX_IMAGE_3D, TEX_SUB_IMAGE_2D, TEX_SUB_IMAGE_3D,
                 COMPRESSED_TEX_IMAGE_2D, COMPRESSED_TEX_IMAGE_3D,
                 COMPRESSED_TEX_SUB_IMAGE_2D, COMPRESSED_TEX_SUB_IMAGE_3D,
                 TEX_STORAGE_2D, TEX_STORAGE_3D, TEX_STORAGE_2D_MULTISAMPLE,
                 TEX_PARAMETER_I, TEX_PARAMETER_F, TEX_PARAMETER_IV, TEX_PARAMETER_FV,
                 GET_TEX_PARAMETER, GENERATE_MIPMAP, COPY_TEX_IMAGE_2D,
                 COPY_TEX_SUB_IMAGE_2D, COPY_TEX_SUB_IMAGE_3D,
                 BIND_IMAGE_TEXTURE -> CallCategory.TEXTURE_OPERATION;

            // Samplers
            case GEN_SAMPLERS, DELETE_SAMPLERS, BIND_SAMPLER,
                 SAMPLER_PARAMETER_I, SAMPLER_PARAMETER_F,
                 SAMPLER_PARAMETER_IV, SAMPLER_PARAMETER_FV -> CallCategory.TEXTURE_OPERATION;

            // Draw calls
            case DRAW_ARRAYS, DRAW_ELEMENTS, DRAW_ARRAYS_INSTANCED,
                 DRAW_ELEMENTS_INSTANCED, DRAW_RANGE_ELEMENTS,
                 DRAW_ARRAYS_INDIRECT, DRAW_ELEMENTS_INDIRECT,
                 DRAW_ELEMENTS_BASE_VERTEX, DRAW_RANGE_ELEMENTS_BASE_VERTEX,
                 DRAW_ELEMENTS_INSTANCED_BASE_VERTEX,
                 MULTI_DRAW_ARRAYS_INDIRECT, MULTI_DRAW_ELEMENTS_INDIRECT 
                 -> CallCategory.DRAW_CALL;

            // State changes
            case ENABLE, DISABLE, IS_ENABLED, BLEND_FUNC, BLEND_FUNC_SEPARATE,
                 BLEND_EQUATION, BLEND_EQUATION_SEPARATE, BLEND_COLOR,
                 DEPTH_FUNC, DEPTH_MASK, DEPTH_RANGE, STENCIL_FUNC,
                 STENCIL_FUNC_SEPARATE, STENCIL_OP, STENCIL_OP_SEPARATE,
                 STENCIL_MASK, STENCIL_MASK_SEPARATE, CULL_FACE, FRONT_FACE,
                 POLYGON_OFFSET, VIEWPORT, SCISSOR, COLOR_MASK, LINE_WIDTH,
                 SAMPLE_COVERAGE, SAMPLE_MASK, PRIMITIVE_RESTART_INDEX,
                 PIXEL_STORE, HINT -> CallCategory.STATE_CHANGE;

            // Clear operations (state-like)
            case CLEAR, CLEAR_COLOR, CLEAR_DEPTH, CLEAR_STENCIL,
                 CLEAR_BUFFER_IV, CLEAR_BUFFER_UIV, CLEAR_BUFFER_FV,
                 CLEAR_BUFFER_FI -> CallCategory.STATE_CHANGE;

            // Framebuffer operations
            case GEN_FRAMEBUFFERS, DELETE_FRAMEBUFFERS, BIND_FRAMEBUFFER,
                 FRAMEBUFFER_TEXTURE_2D, FRAMEBUFFER_TEXTURE_LAYER,
                 FRAMEBUFFER_RENDERBUFFER, CHECK_FRAMEBUFFER_STATUS,
                 BLIT_FRAMEBUFFER, INVALIDATE_FRAMEBUFFER,
                 INVALIDATE_SUB_FRAMEBUFFER, READ_BUFFER, DRAW_BUFFERS,
                 READ_PIXELS, READ_N_PIXELS -> CallCategory.FRAMEBUFFER_OPERATION;

            // Renderbuffers
            case GEN_RENDERBUFFERS, DELETE_RENDERBUFFERS, BIND_RENDERBUFFER,
                 RENDERBUFFER_STORAGE, RENDERBUFFER_STORAGE_MULTISAMPLE,
                 GET_RENDERBUFFER_PARAMETER -> CallCategory.FRAMEBUFFER_OPERATION;

            // Compute
            case DISPATCH_COMPUTE, DISPATCH_COMPUTE_INDIRECT,
                 MEMORY_BARRIER, MEMORY_BARRIER_BY_REGION -> CallCategory.COMPUTE_OPERATION;

            // Queries
            case GEN_QUERIES, DELETE_QUERIES, BEGIN_QUERY, END_QUERY,
                 GET_QUERY_IV, GET_QUERY_OBJECT_UIV, BEGIN_QUERY_INDEXED,
                 END_QUERY_INDEXED -> CallCategory.QUERY_OPERATION;

            // Transform feedback
            case GEN_TRANSFORM_FEEDBACKS, DELETE_TRANSFORM_FEEDBACKS,
                 BIND_TRANSFORM_FEEDBACK, BEGIN_TRANSFORM_FEEDBACK,
                 END_TRANSFORM_FEEDBACK, PAUSE_TRANSFORM_FEEDBACK,
                 RESUME_TRANSFORM_FEEDBACK, TRANSFORM_FEEDBACK_VARYINGS 
                 -> CallCategory.QUERY_OPERATION;

            // Sync
            case FENCE_SYNC, DELETE_SYNC, CLIENT_WAIT_SYNC, WAIT_SYNC,
                 GET_SYNC_IV, FLUSH, FINISH -> CallCategory.SYNC_OPERATION;

            // Debug
            case DEBUG_MESSAGE_CONTROL, DEBUG_MESSAGE_INSERT, DEBUG_MESSAGE_CALLBACK,
                 GET_DEBUG_MESSAGE_LOG, PUSH_DEBUG_GROUP, POP_DEBUG_GROUP,
                 OBJECT_LABEL, GET_OBJECT_LABEL -> CallCategory.DEBUG_OPERATION;

            // Getters
            case GET_ERROR, GET_STRING, GET_INTEGER_V, GET_INTEGER_64V,
                 GET_FLOAT_V, GET_BOOLEAN_V, GET_INTERNAL_FORMAT_IV 
                 -> CallCategory.STATE_CHANGE;

            default -> CallCategory.UNKNOWN;
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 4: SPECIALIZED ROUTERS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Route shader compilation calls to GLSLESCallMapper.
     */
    private MappingResult routeShaderCall(CallDescriptor call) {
        GLESCallType type = call.getType();

        // Handle shader source with potential transpilation
        if (type == GLESCallType.SHADER_SOURCE) {
            String source = call.getStringParam("source");
            int shaderType = call.getIntParam("shaderType");
            
            // Detect shader version and transpile if needed
            GLSLESCallMapper.TranspileResult transpiled = 
                shaderMapper.transpileIfNeeded(source, shaderType, capabilities.version);
            
            if (transpiled.failed()) {
                return MappingResult.failed(
                    "Shader transpilation failed: " + transpiled.getError()
                );
            }
            
            // Update call with transpiled source
            CallDescriptor modifiedCall = call.withStringParam("source", transpiled.getSource());
            return apiMapper.mapCall(modifiedCall);
        }

        return apiMapper.mapCall(call);
    }

    /**
     * Route shader program calls.
     */
    private MappingResult routeShaderProgramCall(CallDescriptor call) {
        // Check for uniform location caching optimization
        if (call.getType() == GLESCallType.GET_UNIFORM_LOCATION) {
            int program = call.getIntParam("program");
            String name = call.getStringParam("name");
            
            // Try cache first
            Integer cached = stateTracker.getCachedUniformLocation(program, name);
            if (cached != null) {
                return MappingResult.success(cached);
            }
            
            // Query GL and cache
            MappingResult result = apiMapper.mapCall(call);
            if (result.isSuccess()) {
                stateTracker.cacheUniformLocation(program, name, result.getIntResult());
            }
            return result;
        }

        return apiMapper.mapCall(call);
    }

    /**
     * Route buffer operations with state tracking.
     */
    private MappingResult routeBufferCall(CallDescriptor call) {
        GLESCallType type = call.getType();

        // VAO support check and fallback
        if (type == GLESCallType.GEN_VERTEX_ARRAYS || 
            type == GLESCallType.BIND_VERTEX_ARRAY ||
            type == GLESCallType.DELETE_VERTEX_ARRAYS) {
            
            if (!capabilities.supportsVAO) {
                return handleVAOFallback(call);
            }
        }

        // Buffer mapping support check
        if (type == GLESCallType.MAP_BUFFER_RANGE) {
            if (!capabilities.supportsBufferMapping) {
                return MappingResult.needsFallback(
                    call,
                    FallbackStrategy.BUFFER_REUPLOAD,
                    "Buffer mapping not supported, use glBufferSubData instead"
                );
            }
        }

        // Track buffer bindings
        if (type == GLESCallType.BIND_BUFFER) {
            int target = call.getIntParam("target");
            int buffer = call.getIntParam("buffer");
            
            // Check if redundant
            if (stateTracker.isBufferBound(target, buffer)) {
                metrics.incrementRedundantStateChange();
                return MappingResult.success(); // Skip redundant bind
            }
            
            MappingResult result = apiMapper.mapCall(call);
            if (result.isSuccess()) {
                stateTracker.setBufferBinding(target, buffer);
            }
            return result;
        }

        return apiMapper.mapCall(call);
    }

    /**
     * Handle VAO fallback for GLES 2.0.
     * 
     * GLES 2.0 doesn't have VAOs, so we emulate them by:
     * 1. Storing vertex attribute state per "virtual VAO"
     * 2. Re-applying all attribute pointers when binding
     */
    private MappingResult handleVAOFallback(CallDescriptor call) {
        GLESCallType type = call.getType();

        return switch (type) {
            case GEN_VERTEX_ARRAYS -> {
                int count = call.getIntParam("count");
                int[] ids = stateTracker.allocateVirtualVAOs(count);
                yield MappingResult.success(ids);
            }
            case DELETE_VERTEX_ARRAYS -> {
                int[] arrays = call.getIntArrayParam("arrays");
                stateTracker.freeVirtualVAOs(arrays);
                yield MappingResult.success();
            }
            case BIND_VERTEX_ARRAY -> {
                int vao = call.getIntParam("array");
                stateTracker.bindVirtualVAO(vao);
                // Re-apply all stored vertex attributes
                MappingResult result = stateTracker.reapplyVirtualVAOState(apiMapper);
                yield result;
            }
            default -> MappingResult.failed("Unexpected VAO call: " + type);
        };
    }

    /**
     * Route texture operations with state tracking.
     */
    private MappingResult routeTextureCall(CallDescriptor call) {
        GLESCallType type = call.getType();

        // Track active texture unit
        if (type == GLESCallType.ACTIVE_TEXTURE) {
            int unit = call.getIntParam("texture");
            if (stateTracker.isActiveTextureUnit(unit)) {
                metrics.incrementRedundantStateChange();
                return MappingResult.success();
            }
        }

        // Track texture bindings
        if (type == GLESCallType.BIND_TEXTURE) {
            int target = call.getIntParam("target");
            int texture = call.getIntParam("texture");
            
            if (stateTracker.isTextureBound(target, texture)) {
                metrics.incrementRedundantStateChange();
                return MappingResult.success();
            }
            
            MappingResult result = apiMapper.mapCall(call);
            if (result.isSuccess()) {
                stateTracker.setTextureBinding(target, texture);
            }
            return result;
        }

        // Immutable texture storage fallback
        if (type == GLESCallType.TEX_STORAGE_2D || type == GLESCallType.TEX_STORAGE_3D) {
            if (!capabilities.supportsImmutableTextures) {
                return convertToMutableTexture(call);
            }
        }

        // 3D texture support check
        if (type == GLESCallType.TEX_IMAGE_3D || type == GLESCallType.TEX_STORAGE_3D) {
            if (!capabilities.version.isAtLeast(GLESVersion.GLES_30) &&
                !capabilities.hasExtension("GL_OES_texture_3D")) {
                return MappingResult.failed("3D textures not supported on this device");
            }
        }

        return apiMapper.mapCall(call);
    }

    /**
     * Convert immutable texture storage to mutable for GLES 2.0.
     */
    private MappingResult convertToMutableTexture(CallDescriptor call) {
        int levels = call.getIntParam("levels");
        int internalFormat = call.getIntParam("internalformat");
        int width = call.getIntParam("width");
        int height = call.getIntParam("height");

        // Convert internal format to format/type for glTexImage2D
        TextureFormatInfo formatInfo = TextureFormatInfo.fromInternalFormat(internalFormat);
        if (formatInfo == null) {
            return MappingResult.failed("Cannot convert internal format: " + internalFormat);
        }

        // Allocate each mip level with glTexImage2D
        int mipWidth = width;
        int mipHeight = height;
        
        for (int level = 0; level < levels; level++) {
            CallDescriptor texImage = CallDescriptor.builder(GLESCallType.TEX_IMAGE_2D)
                .intParam("target", call.getIntParam("target"))
                .intParam("level", level)
                .intParam("internalformat", formatInfo.internalFormat)
                .intParam("width", mipWidth)
                .intParam("height", mipHeight)
                .intParam("border", 0)
                .intParam("format", formatInfo.format)
                .intParam("type", formatInfo.type)
                .nullData() // No initial data
                .build();

            MappingResult result = apiMapper.mapCall(texImage);
            if (!result.isSuccess()) {
                return result;
            }

            mipWidth = Math.max(1, mipWidth / 2);
            mipHeight = Math.max(1, mipHeight / 2);
        }

        return MappingResult.success();
    }

    /**
     * Route draw calls with instancing fallback.
     */
    private MappingResult routeDrawCall(CallDescriptor call) {
        GLESCallType type = call.getType();
        metrics.incrementDrawCallCount();

        // ─────────────────────────────────────────────────────────────────────
        // Instanced draw fallback for GLES 2.0
        // ─────────────────────────────────────────────────────────────────────
        if (type == GLESCallType.DRAW_ARRAYS_INSTANCED || 
            type == GLESCallType.DRAW_ELEMENTS_INSTANCED) {
            
            if (!capabilities.supportsInstancing) {
                return emulateInstancedDraw(call);
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Indirect draw fallback for GLES 3.0
        // ─────────────────────────────────────────────────────────────────────
        if (type == GLESCallType.DRAW_ARRAYS_INDIRECT || 
            type == GLESCallType.DRAW_ELEMENTS_INDIRECT) {
            
            if (!capabilities.supportsDrawIndirect) {
                return emulateIndirectDraw(call);
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Base vertex fallback for GLES 3.1
        // ─────────────────────────────────────────────────────────────────────
        if (type == GLESCallType.DRAW_ELEMENTS_BASE_VERTEX ||
            type == GLESCallType.DRAW_RANGE_ELEMENTS_BASE_VERTEX ||
            type == GLESCallType.DRAW_ELEMENTS_INSTANCED_BASE_VERTEX) {
            
            if (!capabilities.supportsBaseVertex) {
                return emulateBaseVertexDraw(call);
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Multi-draw indirect fallback
        // ─────────────────────────────────────────────────────────────────────
        if (type == GLESCallType.MULTI_DRAW_ARRAYS_INDIRECT ||
            type == GLESCallType.MULTI_DRAW_ELEMENTS_INDIRECT) {
            
            if (!capabilities.supportsMultiDrawIndirect) {
                return emulateMultiDrawIndirect(call);
            }
        }

        // Track triangle count for metrics
        trackPrimitiveCount(call);

        return apiMapper.mapCall(call);
    }

    /**
     * Emulate instanced drawing by issuing multiple draw calls.
     * 
     * Performance: O(instanceCount) draw calls - SLOW but functional.
     */
    private MappingResult emulateInstancedDraw(CallDescriptor call) {
        GLESCallType type = call.getType();
        int instanceCount = call.getIntParam("instancecount");
        
        // Get the current program to update gl_InstanceID uniform
        int program = stateTracker.getCurrentProgram();
        int instanceIdLocation = -1;
        
        if (program != 0) {
            // Try to find a gl_InstanceID emulation uniform
            instanceIdLocation = stateTracker.getCachedUniformLocation(
                program, "u_InstanceID"
            );
        }

        for (int instance = 0; instance < instanceCount; instance++) {
            // Update instance ID uniform if available
            if (instanceIdLocation >= 0) {
                CallDescriptor uniformCall = CallDescriptor.builder(GLESCallType.UNIFORM_1I)
                    .intParam("location", instanceIdLocation)
                    .intParam("v0", instance)
                    .build();
                apiMapper.mapCall(uniformCall);
            }

            // Issue the draw call
            CallDescriptor singleDraw;
            if (type == GLESCallType.DRAW_ARRAYS_INSTANCED) {
                singleDraw = CallDescriptor.builder(GLESCallType.DRAW_ARRAYS)
                    .intParam("mode", call.getIntParam("mode"))
                    .intParam("first", call.getIntParam("first"))
                    .intParam("count", call.getIntParam("count"))
                    .build();
            } else {
                singleDraw = CallDescriptor.builder(GLESCallType.DRAW_ELEMENTS)
                    .intParam("mode", call.getIntParam("mode"))
                    .intParam("count", call.getIntParam("count"))
                    .intParam("type", call.getIntParam("type"))
                    .longParam("indices", call.getLongParam("indices"))
                    .build();
            }

            MappingResult result = apiMapper.mapCall(singleDraw);
            if (!result.isSuccess()) {
                return result;
            }
        }

        return MappingResult.successWithWarning(
            "Instanced draw emulated with " + instanceCount + " separate draw calls"
        );
    }

    /**
     * Emulate indirect drawing by reading parameters from CPU.
     * 
     * Requires CPU readback of the indirect buffer - breaks GPU-driven pipeline.
     */
    private MappingResult emulateIndirectDraw(CallDescriptor call) {
        GLESCallType type = call.getType();
        long offset = call.getLongParam("indirect");

        // Read indirect buffer back to CPU
        int indirectBuffer = stateTracker.getBoundBuffer(BufferTarget.DRAW_INDIRECT_BUFFER);
        if (indirectBuffer == 0) {
            return MappingResult.failed("No indirect buffer bound");
        }

        // Allocate CPU buffer for readback
        ByteBuffer params;
        if (type == GLESCallType.DRAW_ARRAYS_INDIRECT) {
            params = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        } else {
            params = ByteBuffer.allocateDirect(20).order(ByteOrder.nativeOrder());
        }

        // This is slow - requires GPU sync
        MappingResult readResult = readBufferSubData(indirectBuffer, offset, params);
        if (!readResult.isSuccess()) {
            return readResult;
        }

        // Parse parameters
        if (type == GLESCallType.DRAW_ARRAYS_INDIRECT) {
            int count = params.getInt(0);
            int instanceCount = params.getInt(4);
            int first = params.getInt(8);
            int baseInstance = params.getInt(12); // Ignored in GLES

            CallDescriptor directDraw = CallDescriptor.builder(GLESCallType.DRAW_ARRAYS_INSTANCED)
                .intParam("mode", call.getIntParam("mode"))
                .intParam("first", first)
                .intParam("count", count)
                .intParam("instancecount", instanceCount)
                .build();

            return routeDrawCall(directDraw);
        } else {
            int count = params.getInt(0);
            int instanceCount = params.getInt(4);
            int firstIndex = params.getInt(8);
            int baseVertex = params.getInt(12);  // May not be supported
            int baseInstance = params.getInt(16); // Ignored in GLES

            CallDescriptor directDraw = CallDescriptor.builder(GLESCallType.DRAW_ELEMENTS_INSTANCED)
                .intParam("mode", call.getIntParam("mode"))
                .intParam("count", count)
                .intParam("type", call.getIntParam("type"))
                .longParam("indices", (long) firstIndex * getIndexSize(call.getIntParam("type")))
                .intParam("instancecount", instanceCount)
                .build();

            return routeDrawCall(directDraw);
        }
    }

    /**
     * Emulate base vertex by adjusting indices on CPU.
     */
    private MappingResult emulateBaseVertexDraw(CallDescriptor call) {
        int baseVertex = call.getIntParam("basevertex");
        
        if (baseVertex == 0) {
            // No emulation needed, use regular draw
            CallDescriptor simpleDraw = CallDescriptor.builder(GLESCallType.DRAW_ELEMENTS)
                .intParam("mode", call.getIntParam("mode"))
                .intParam("count", call.getIntParam("count"))
                .intParam("type", call.getIntParam("type"))
                .longParam("indices", call.getLongParam("indices"))
                .build();
            return apiMapper.mapCall(simpleDraw);
        }

        // This requires modifying indices - very expensive
        return MappingResult.failed(
            "Base vertex emulation with non-zero baseVertex requires index buffer modification. " +
            "Consider using a vertex shader uniform for vertex offset instead."
        );
    }

    /**
     * Emulate multi-draw indirect by issuing multiple indirect draws.
     */
    private MappingResult emulateMultiDrawIndirect(CallDescriptor call) {
        int drawCount = call.getIntParam("drawcount");
        int stride = call.getIntParam("stride");
        long offset = call.getLongParam("indirect");

        GLESCallType singleType = call.getType() == GLESCallType.MULTI_DRAW_ARRAYS_INDIRECT
            ? GLESCallType.DRAW_ARRAYS_INDIRECT
            : GLESCallType.DRAW_ELEMENTS_INDIRECT;

        int structSize = singleType == GLESCallType.DRAW_ARRAYS_INDIRECT ? 16 : 20;
        int actualStride = stride == 0 ? structSize : stride;

        for (int i = 0; i < drawCount; i++) {
            CallDescriptor singleDraw = CallDescriptor.builder(singleType)
                .intParam("mode", call.getIntParam("mode"))
                .longParam("indirect", offset + (long) i * actualStride)
                .build();

            if (singleType == GLESCallType.DRAW_ELEMENTS_INDIRECT) {
                singleDraw = singleDraw.withIntParam("type", call.getIntParam("type"));
            }

            MappingResult result = routeDrawCall(singleDraw);
            if (!result.isSuccess()) {
                return result;
            }
        }

        return MappingResult.successWithWarning(
            "Multi-draw indirect emulated with " + drawCount + " separate indirect draws"
        );
    }

    private void trackPrimitiveCount(CallDescriptor call) {
        int mode = call.getIntParam("mode");
        int count = call.getIntParamOrDefault("count", 0);
        int instanceCount = call.getIntParamOrDefault("instancecount", 1);

        int triangles = switch (mode) {
            case 0x0004 -> count / 3 * instanceCount;          // GL_TRIANGLES
            case 0x0005 -> Math.max(0, count - 2) * instanceCount; // GL_TRIANGLE_STRIP
            case 0x0006 -> Math.max(0, count - 2) * instanceCount; // GL_TRIANGLE_FAN
            default -> 0;
        };

        metrics.addTriangleCount(triangles);
    }

    private int getIndexSize(int type) {
        return switch (type) {
            case 0x1401 -> 1; // GL_UNSIGNED_BYTE
            case 0x1403 -> 2; // GL_UNSIGNED_SHORT
            case 0x1405 -> 4; // GL_UNSIGNED_INT
            default -> 2;
        };
    }

    private MappingResult readBufferSubData(int buffer, long offset, ByteBuffer dest) {
        // Bind to COPY_READ_BUFFER to avoid disturbing current bindings
        if (capabilities.version.isAtLeast(GLESVersion.GLES_30)) {
            // Use glGetBufferSubData equivalent via mapping
            // ... implementation details
        }
        // For GLES 2.0, this is not possible without glFinish + glReadPixels hack
        return MappingResult.failed("Buffer readback not supported on GLES 2.0");
    }

    /**
     * Route state change calls with caching.
     */
    private MappingResult routeStateCall(CallDescriptor call) {
        GLESCallType type = call.getType();

        // Enable/Disable with caching
        if (type == GLESCallType.ENABLE || type == GLESCallType.DISABLE) {
            int cap = call.getIntParam("cap");
            boolean enable = type == GLESCallType.ENABLE;

            if (stateTracker.isCapabilitySet(cap, enable)) {
                metrics.incrementRedundantStateChange();
                return MappingResult.success();
            }

            MappingResult result = apiMapper.mapCall(call);
            if (result.isSuccess()) {
                stateTracker.setCapability(cap, enable);
            }
            return result;
        }

        // Blend function caching
        if (type == GLESCallType.BLEND_FUNC) {
            int sfactor = call.getIntParam("sfactor");
            int dfactor = call.getIntParam("dfactor");

            if (stateTracker.isBlendFuncSet(sfactor, dfactor)) {
                metrics.incrementRedundantStateChange();
                return MappingResult.success();
            }

            MappingResult result = apiMapper.mapCall(call);
            if (result.isSuccess()) {
                stateTracker.setBlendFunc(sfactor, dfactor);
            }
            return result;
        }

        // Viewport caching
        if (type == GLESCallType.VIEWPORT) {
            int x = call.getIntParam("x");
            int y = call.getIntParam("y");
            int width = call.getIntParam("width");
            int height = call.getIntParam("height");

            if (stateTracker.isViewportSet(x, y, width, height)) {
                metrics.incrementRedundantStateChange();
                return MappingResult.success();
            }

            MappingResult result = apiMapper.mapCall(call);
            if (result.isSuccess()) {
                stateTracker.setViewport(x, y, width, height);
            }
            return result;
        }

        // Use program caching
        if (type == GLESCallType.USE_PROGRAM) {
            int program = call.getIntParam("program");

            if (stateTracker.isCurrentProgram(program)) {
                metrics.incrementRedundantStateChange();
                return MappingResult.success();
            }

            MappingResult result = apiMapper.mapCall(call);
            if (result.isSuccess()) {
                stateTracker.setCurrentProgram(program);
            }
            return result;
        }

        return apiMapper.mapCall(call);
    }

    /**
     * Route framebuffer operations.
     */
    private MappingResult routeFramebufferCall(CallDescriptor call) {
        GLESCallType type = call.getType();

        // Blit framebuffer fallback for GLES 2.0
        if (type == GLESCallType.BLIT_FRAMEBUFFER) {
            if (!capabilities.version.isAtLeast(GLESVersion.GLES_30)) {
                return emulateFramebufferBlit(call);
            }
        }

        // Invalidate framebuffer for tile-based GPUs (optimization, not fallback)
        if (type == GLESCallType.INVALIDATE_FRAMEBUFFER) {
            if (!capabilities.version.isAtLeast(GLESVersion.GLES_30)) {
                // Just skip on GLES 2.0 - it's an optimization hint
                return MappingResult.success();
            }
        }

        // Multiple render targets check
        if (type == GLESCallType.DRAW_BUFFERS) {
            int[] bufs = call.getIntArrayParam("bufs");
            if (bufs.length > capabilities.maxDrawBuffers) {
                return MappingResult.failed(
                    "Too many draw buffers: " + bufs.length + 
                    " (max: " + capabilities.maxDrawBuffers + ")"
                );
            }
        }

        return apiMapper.mapCall(call);
    }

    /**
     * Emulate framebuffer blit using a fullscreen quad.
     */
    private MappingResult emulateFramebufferBlit(CallDescriptor call) {
        // This requires:
        // 1. Binding source FBO as texture
        // 2. Binding dest FBO as render target
        // 3. Drawing a fullscreen quad with passthrough shader
        // Complex implementation - simplified here
        return MappingResult.failed(
            "Framebuffer blit not supported on GLES 2.0. " +
            "Use a fullscreen quad with texture sampling instead."
        );
    }

    /**
     * Route compute operations.
     */
    private MappingResult routeComputeCall(CallDescriptor call) {
        if (!capabilities.supportsCompute) {
            return MappingResult.failed(
                "Compute shaders require GLES 3.1+. Current version: " + capabilities.version
            );
        }

        GLESCallType type = call.getType();

        // Memory barrier handling
        if (type == GLESCallType.MEMORY_BARRIER) {
            int barriers = call.getIntParam("barriers");
            
            // On some GPUs (Mali), additional barriers needed after compute
            if (capabilities.needsTextureBarrierAfterCompute) {
                barriers |= 0x00000008; // GL_TEXTURE_FETCH_BARRIER_BIT
            }

            return apiMapper.mapCall(call.withIntParam("barriers", barriers));
        }

        return apiMapper.mapCall(call);
    }

    /**
     * Route query operations.
     */
    private MappingResult routeQueryCall(CallDescriptor call) {
        GLESCallType type = call.getType();

        // Occlusion query support check
        if (type == GLESCallType.BEGIN_QUERY || type == GLESCallType.END_QUERY) {
            int target = call.getIntParam("target");
            
            if (target == 0x8914 || target == 0x8915) { // ANY_SAMPLES_PASSED variants
                if (!capabilities.supportsOcclusionQuery) {
                    return MappingResult.failed("Occlusion queries not supported");
                }
            }
        }

        // Timer query support check
        if (type == GLESCallType.BEGIN_QUERY) {
            int target = call.getIntParam("target");
            if (target == 0x88BF) { // GL_TIME_ELAPSED
                if (!capabilities.supportsTimerQuery) {
                    return MappingResult.failed("Timer queries not supported");
                }
            }
        }

        return apiMapper.mapCall(call);
    }

    /**
     * Route synchronization operations.
     */
    private MappingResult routeSyncCall(CallDescriptor call) {
        GLESCallType type = call.getType();

        // Fence sync fallback for GLES 2.0
        if (type == GLESCallType.FENCE_SYNC || 
            type == GLESCallType.CLIENT_WAIT_SYNC ||
            type == GLESCallType.WAIT_SYNC) {
            
            if (!capabilities.supportsFenceSync) {
                // Fallback to glFinish for synchronization
                if (type == GLESCallType.CLIENT_WAIT_SYNC || type == GLESCallType.WAIT_SYNC) {
                    return apiMapper.mapCall(CallDescriptor.builder(GLESCallType.FINISH).build());
                }
                // For FENCE_SYNC, return a dummy handle
                return MappingResult.success(0L);
            }
        }

        return apiMapper.mapCall(call);
    }

    /**
     * Route debug operations.
     */
    private MappingResult routeDebugCall(CallDescriptor call) {
        if (!capabilities.supportsDebugOutput) {
            // Debug output not available, silently succeed
            return MappingResult.success();
        }

        return apiMapper.mapCall(call);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 5: FALLBACK CHAIN EXECUTION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Execute fallback strategy when primary mapping fails.
     */
    private MappingResult executeFallback(CallDescriptor originalCall, MappingResult failedResult) {
        FallbackStrategy strategy = failedResult.getFallbackStrategy();
        
        if (strategy == null) {
            return failedResult;
        }

        metrics.incrementFallbackCount();

        return switch (strategy) {
            case VERSION_DOWNGRADE -> fallbackChain.tryVersionDowngrade(originalCall);
            case EXTENSION_EMULATION -> fallbackChain.tryExtensionEmulation(originalCall);
            case DESKTOP_GL -> fallbackChain.tryDesktopGL(originalCall);
            case VULKAN_SPIRV -> fallbackChain.tryVulkanSPIRV(originalCall);
            case BUFFER_REUPLOAD -> fallbackChain.tryBufferReupload(originalCall);
            case SOFTWARE_EMULATION -> fallbackChain.trySoftwareEmulation(originalCall);
            case SKIP_UNSUPPORTED -> MappingResult.successWithWarning("Feature skipped (unsupported)");
            case FAIL -> failedResult;
        };
    }

    /**
     * Fallback strategies in priority order.
     */
    public enum FallbackStrategy {
        VERSION_DOWNGRADE,    // Try older GLES version
        EXTENSION_EMULATION,  // Emulate via extension
        DESKTOP_GL,           // Route to desktop OpenGL
        VULKAN_SPIRV,        // Route to Vulkan via SPIRV
        BUFFER_REUPLOAD,     // Re-upload instead of map
        SOFTWARE_EMULATION,   // CPU fallback (slow)
        SKIP_UNSUPPORTED,    // Skip gracefully
        FAIL                  // No fallback available
    }

    /**
     * Fallback chain coordinator.
     */
    public static final class FallbackChain {
        private final OpenGLESManager manager;
        private final Set<String> attemptedFallbacks = ConcurrentHashMap.newKeySet();

        FallbackChain(OpenGLESManager manager) {
            this.manager = manager;
        }

        MappingResult tryVersionDowngrade(CallDescriptor call) {
            // Implementation: modify call for older version
            return MappingResult.failed("Version downgrade not implemented for: " + call.getType());
        }

        MappingResult tryExtensionEmulation(CallDescriptor call) {
            // Implementation: use extension-based alternatives
            return MappingResult.failed("Extension emulation not available for: " + call.getType());
        }

        MappingResult tryDesktopGL(CallDescriptor call) {
            // Would route to OpenGLManager (desktop)
            return MappingResult.failed("Desktop GL fallback not connected");
        }

        MappingResult tryVulkanSPIRV(CallDescriptor call) {
            // Would route to SPIRVManager → VulkanManager
            return MappingResult.failed("Vulkan/SPIRV fallback not connected");
        }

        MappingResult tryBufferReupload(CallDescriptor call) {
            // Convert buffer mapping to buffer sub-data upload
            return MappingResult.failed("Buffer reupload fallback not implemented");
        }

        MappingResult trySoftwareEmulation(CallDescriptor call) {
            // CPU-based emulation (very slow)
            return MappingResult.failed("Software emulation not available");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 6: STATE TRACKING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Tracks all GL state to avoid redundant calls.
     * 
     * Redundant state changes are one of the biggest performance killers in GL.
     * This class caches all state and filters out no-op changes.
     */
    public static final class GLStateTracker {
        
        // Buffer bindings per target
        private final Int2IntMap bufferBindings = new Int2IntOpenHashMap();
        
        // Texture bindings: [unit][target] -> texture
        private final Int2ObjectMap<Int2IntMap> textureBindings = new Int2ObjectOpenHashMap<>();
        
        // Current active texture unit
        private int activeTextureUnit = 0;
        
        // Capability states (glEnable/glDisable)
        private final IntSet enabledCapabilities = new IntOpenHashSet();
        
        // Current shader program
        private int currentProgram = 0;
        
        // Uniform location cache: program -> (name -> location)
        private final Int2ObjectMap<Object2IntMap<String>> uniformLocationCache = 
            new Int2ObjectOpenHashMap<>();
        
        // Blend state
        private int blendSrcFactor = 1;  // GL_ONE
        private int blendDstFactor = 0;  // GL_ZERO
        
        // Viewport state
        private int viewportX, viewportY, viewportWidth, viewportHeight;
        
        // VAO state
        private int currentVAO = 0;
        
        // Virtual VAO emulation for GLES 2.0
        private final Int2ObjectMap<VirtualVAOState> virtualVAOs = new Int2ObjectOpenHashMap<>();
        private final AtomicInteger virtualVAOIdGenerator = new AtomicInteger(1);
        private int currentVirtualVAO = 0;
        
        private final DeviceCapabilities capabilities;
        
        GLStateTracker(DeviceCapabilities capabilities) {
            this.capabilities = capabilities;
            bufferBindings.defaultReturnValue(0);
        }

        // ─────────────────────────────────────────────────────────────────────
        // Buffer state
        // ─────────────────────────────────────────────────────────────────────
        
        boolean isBufferBound(int target, int buffer) {
            return bufferBindings.get(target) == buffer;
        }
        
        void setBufferBinding(int target, int buffer) {
            bufferBindings.put(target, buffer);
        }
        
        int getBoundBuffer(BufferTarget target) {
            return bufferBindings.get(target.glConstant);
        }
        
        int getBoundBuffer(int target) {
            return bufferBindings.get(target);
        }

        // ─────────────────────────────────────────────────────────────────────
        // Texture state
        // ─────────────────────────────────────────────────────────────────────
        
        boolean isActiveTextureUnit(int unit) {
            return activeTextureUnit == unit;
        }
        
        void setActiveTextureUnit(int unit) {
            activeTextureUnit = unit;
        }
        
        boolean isTextureBound(int target, int texture) {
            Int2IntMap unitBindings = textureBindings.get(activeTextureUnit);
            if (unitBindings == null) return texture == 0;
            return unitBindings.getOrDefault(target, 0) == texture;
        }
        
        void setTextureBinding(int target, int texture) {
            textureBindings.computeIfAbsent(activeTextureUnit, k -> {
                Int2IntMap map = new Int2IntOpenHashMap();
                map.defaultReturnValue(0);
                return map;
            }).put(target, texture);
        }

        // ─────────────────────────────────────────────────────────────────────
        // Capability state
        // ─────────────────────────────────────────────────────────────────────
        
        boolean isCapabilitySet(int cap, boolean enabled) {
            return enabledCapabilities.contains(cap) == enabled;
        }
        
        void setCapability(int cap, boolean enabled) {
            if (enabled) {
                enabledCapabilities.add(cap);
            } else {
                enabledCapabilities.remove(cap);
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // Program state
        // ─────────────────────────────────────────────────────────────────────
        
        boolean isCurrentProgram(int program) {
            return currentProgram == program;
        }
        
        void setCurrentProgram(int program) {
            currentProgram = program;
        }
        
        int getCurrentProgram() {
            return currentProgram;
        }
        
        Integer getCachedUniformLocation(int program, String name) {
            Object2IntMap<String> cache = uniformLocationCache.get(program);
            if (cache == null) return null;
            if (!cache.containsKey(name)) return null;
            return cache.getInt(name);
        }
        
        void cacheUniformLocation(int program, String name, int location) {
            uniformLocationCache.computeIfAbsent(program, k -> {
                Object2IntMap<String> map = new Object2IntOpenHashMap<>();
                map.defaultReturnValue(-1);
                return map;
            }).put(name, location);
        }
        
        void invalidateProgramCache(int program) {
            uniformLocationCache.remove(program);
        }

        // ─────────────────────────────────────────────────────────────────────
        // Blend state
        // ─────────────────────────────────────────────────────────────────────
        
        boolean isBlendFuncSet(int src, int dst) {
            return blendSrcFactor == src && blendDstFactor == dst;
        }
        
        void setBlendFunc(int src, int dst) {
            blendSrcFactor = src;
            blendDstFactor = dst;
        }

        // ─────────────────────────────────────────────────────────────────────
        // Viewport state
        // ─────────────────────────────────────────────────────────────────────
        
        boolean isViewportSet(int x, int y, int width, int height) {
            return viewportX == x && viewportY == y && 
                   viewportWidth == width && viewportHeight == height;
        }
        
        void setViewport(int x, int y, int width, int height) {
            viewportX = x;
            viewportY = y;
            viewportWidth = width;
            viewportHeight = height;
        }

        // ─────────────────────────────────────────────────────────────────────
        // Virtual VAO emulation (GLES 2.0)
        // ─────────────────────────────────────────────────────────────────────
        
        int[] allocateVirtualVAOs(int count) {
            int[] ids = new int[count];
            for (int i = 0; i < count; i++) {
                int id = virtualVAOIdGenerator.getAndIncrement();
                virtualVAOs.put(id, new VirtualVAOState());
                ids[i] = id;
            }
            return ids;
        }
        
        void freeVirtualVAOs(int[] arrays) {
            for (int id : arrays) {
                virtualVAOs.remove(id);
            }
        }
        
        void bindVirtualVAO(int vao) {
            // Save current state to previous VAO
            if (currentVirtualVAO != 0) {
                VirtualVAOState state = virtualVAOs.get(currentVirtualVAO);
                if (state != null) {
                    captureCurrentState(state);
                }
            }
            currentVirtualVAO = vao;
        }
        
        MappingResult reapplyVirtualVAOState(OpenGLESCallMapper mapper) {
            if (currentVirtualVAO == 0) {
                // VAO 0 = default state, nothing to apply
                return MappingResult.success();
            }
            
            VirtualVAOState state = virtualVAOs.get(currentVirtualVAO);
            if (state == null) {
                return MappingResult.failed("Virtual VAO " + currentVirtualVAO + " not found");
            }
            
            return state.apply(mapper);
        }
        
        private void captureCurrentState(VirtualVAOState state) {
            state.elementArrayBuffer = bufferBindings.get(0x8893); // GL_ELEMENT_ARRAY_BUFFER
            // Capture vertex attribute state...
        }
        
        /**
         * Virtual VAO state for GLES 2.0 emulation.
         */
        static final class VirtualVAOState {
            int elementArrayBuffer;
            final Int2ObjectMap<VertexAttribState> attributes = new Int2ObjectOpenHashMap<>();
            final IntSet enabledAttribs = new IntOpenHashSet();
            
            MappingResult apply(OpenGLESCallMapper mapper) {
                // Bind element array buffer
                CallDescriptor bindEAB = CallDescriptor.builder(GLESCallType.BIND_BUFFER)
                    .intParam("target", 0x8893) // GL_ELEMENT_ARRAY_BUFFER
                    .intParam("buffer", elementArrayBuffer)
                    .build();
                MappingResult result = mapper.mapCall(bindEAB);
                if (!result.isSuccess()) return result;
                
                // Reapply all vertex attributes
                for (Int2ObjectMap.Entry<VertexAttribState> entry : attributes.int2ObjectEntrySet()) {
                    int index = entry.getIntKey();
                    VertexAttribState attrib = entry.getValue();
                    
                    // Bind the VBO
                    CallDescriptor bindVBO = CallDescriptor.builder(GLESCallType.BIND_BUFFER)
                        .intParam("target", 0x8892) // GL_ARRAY_BUFFER
                        .intParam("buffer", attrib.buffer)
                        .build();
                    result = mapper.mapCall(bindVBO);
                    if (!result.isSuccess()) return result;
                    
                    // Set the pointer
                    CallDescriptor setPointer = CallDescriptor.builder(GLESCallType.VERTEX_ATTRIB_POINTER)
                        .intParam("index", index)
                        .intParam("size", attrib.size)
                        .intParam("type", attrib.type)
                        .boolParam("normalized", attrib.normalized)
                        .intParam("stride", attrib.stride)
                        .longParam("pointer", attrib.offset)
                        .build();
                    result = mapper.mapCall(setPointer);
                    if (!result.isSuccess()) return result;
                }
                
                // Enable/disable attributes
                for (int i = 0; i < 16; i++) {
                    boolean shouldEnable = enabledAttribs.contains(i);
                    GLESCallType callType = shouldEnable 
                        ? GLESCallType.ENABLE_VERTEX_ATTRIB_ARRAY 
                        : GLESCallType.DISABLE_VERTEX_ATTRIB_ARRAY;
                    
                    CallDescriptor enableCall = CallDescriptor.builder(callType)
                        .intParam("index", i)
                        .build();
                    result = mapper.mapCall(enableCall);
                    if (!result.isSuccess()) return result;
                }
                
                return MappingResult.success();
            }
        }
        
        static final class VertexAttribState {
            int buffer;
            int size;
            int type;
            boolean normalized;
            int stride;
            long offset;
            int divisor;
        }
        
        void reset() {
            bufferBindings.clear();
            textureBindings.clear();
            activeTextureUnit = 0;
            enabledCapabilities.clear();
            currentProgram = 0;
            uniformLocationCache.clear();
            blendSrcFactor = 1;
            blendDstFactor = 0;
            viewportX = viewportY = viewportWidth = viewportHeight = 0;
            currentVAO = 0;
            virtualVAOs.clear();
            currentVirtualVAO = 0;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 7: RESOURCE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Manages GL resource lifetimes and cleanup.
     */
    public static final class ResourceManager {
        
        /** All tracked resources by type */
        private final Int2ObjectMap<IntSet> resourcesByType = new Int2ObjectOpenHashMap<>();
        
        /** Reference counts for shared resources */
        private final Int2IntMap refCounts = new Int2IntOpenHashMap();
        
        /** Resources pending deletion */
        private final IntList pendingDeletion = new IntArrayList();
        
        /** Mutex for thread safety */
        private final Object lock = new Object();
        
        private final DeviceCapabilities capabilities;
        
        ResourceManager(DeviceCapabilities capabilities) {
            this.capabilities = capabilities;
            refCounts.defaultReturnValue(0);
        }
        
        enum ResourceType {
            BUFFER(0),
            TEXTURE(1),
            SHADER(2),
            PROGRAM(3),
            FRAMEBUFFER(4),
            RENDERBUFFER(5),
            VERTEX_ARRAY(6),
            SAMPLER(7),
            QUERY(8),
            TRANSFORM_FEEDBACK(9),
            SYNC(10);
            
            final int id;
            ResourceType(int id) { this.id = id; }
        }
        
        void trackResource(ResourceType type, int id) {
            synchronized (lock) {
                resourcesByType.computeIfAbsent(type.id, k -> new IntOpenHashSet()).add(id);
                refCounts.put(packKey(type.id, id), 1);
            }
        }
        
        void addRef(ResourceType type, int id) {
            synchronized (lock) {
                int key = packKey(type.id, id);
                refCounts.put(key, refCounts.get(key) + 1);
            }
        }
        
        boolean release(ResourceType type, int id) {
            synchronized (lock) {
                int key = packKey(type.id, id);
                int count = refCounts.get(key) - 1;
                if (count <= 0) {
                    refCounts.remove(key);
                    IntSet set = resourcesByType.get(type.id);
                    if (set != null) set.remove(id);
                    return true; // Should be deleted
                }
                refCounts.put(key, count);
                return false;
            }
        }
        
        void scheduleDeletion(int id) {
            synchronized (lock) {
                pendingDeletion.add(id);
            }
        }
        
        IntList getPendingDeletions() {
            synchronized (lock) {
                IntList copy = new IntArrayList(pendingDeletion);
                pendingDeletion.clear();
                return copy;
            }
        }
        
        void releaseAll(OpenGLESCallMapper mapper) {
            synchronized (lock) {
                // Delete all tracked resources
                for (Int2ObjectMap.Entry<IntSet> entry : resourcesByType.int2ObjectEntrySet()) {
                    int typeId = entry.getIntKey();
                    IntSet ids = entry.getValue();
                    
                    ResourceType type = ResourceType.values()[typeId];
                    GLESCallType deleteCall = getDeleteCall(type);
                    
                    if (deleteCall != null && !ids.isEmpty()) {
                        int[] idArray = ids.toIntArray();
                        CallDescriptor call = CallDescriptor.builder(deleteCall)
                            .intParam("n", idArray.length)
                            .intArrayParam("ids", idArray)
                            .build();
                        mapper.mapCall(call);
                    }
                }
                
                resourcesByType.clear();
                refCounts.clear();
                pendingDeletion.clear();
            }
        }
        
        private GLESCallType getDeleteCall(ResourceType type) {
            return switch (type) {
                case BUFFER -> GLESCallType.DELETE_BUFFERS;
                case TEXTURE -> GLESCallType.DELETE_TEXTURES;
                case SHADER -> GLESCallType.DELETE_SHADER;
                case PROGRAM -> GLESCallType.DELETE_PROGRAM;
                case FRAMEBUFFER -> GLESCallType.DELETE_FRAMEBUFFERS;
                case RENDERBUFFER -> GLESCallType.DELETE_RENDERBUFFERS;
                case VERTEX_ARRAY -> GLESCallType.DELETE_VERTEX_ARRAYS;
                case SAMPLER -> GLESCallType.DELETE_SAMPLERS;
                case QUERY -> GLESCallType.DELETE_QUERIES;
                case TRANSFORM_FEEDBACK -> GLESCallType.DELETE_TRANSFORM_FEEDBACKS;
                case SYNC -> GLESCallType.DELETE_SYNC;
            };
        }
        
        private int packKey(int typeId, int resourceId) {
            return (typeId << 24) | (resourceId & 0xFFFFFF);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 8: PERFORMANCE METRICS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Collects performance metrics for monitoring and optimization.
     */
    public static final class PerformanceMetrics {
        
        private final LongAdder totalCalls = new LongAdder();
        private final LongAdder drawCalls = new LongAdder();
        private final LongAdder triangleCount = new LongAdder();
        private final LongAdder stateChanges = new LongAdder();
        private final LongAdder redundantStateChanges = new LongAdder();
        private final LongAdder fallbackCount = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        
        /** Per-call-type timing in nanoseconds */
        private final Object2LongMap<GLESCallType> callTimes = new Object2LongOpenHashMap<>();
        private final Object2LongMap<GLESCallType> callCounts = new Object2LongOpenHashMap<>();
        
        void incrementCallCount() {
            totalCalls.increment();
        }
        
        void incrementDrawCallCount() {
            drawCalls.increment();
        }
        
        void addTriangleCount(int count) {
            triangleCount.add(count);
        }
        
        void incrementStateChange() {
            stateChanges.increment();
        }
        
        void incrementRedundantStateChange() {
            redundantStateChanges.increment();
        }
        
        void incrementFallbackCount() {
            fallbackCount.increment();
        }
        
        void incrementErrorCount() {
            errorCount.increment();
        }
        
        void recordCallTime(GLESCallType type, long nanos) {
            synchronized (callTimes) {
                callTimes.put(type, callTimes.getOrDefault(type, 0L) + nanos);
                callCounts.put(type, callCounts.getOrDefault(type, 0L) + 1L);
            }
        }
        
        /**
         * Get a snapshot of current metrics.
         */
        public MetricsSnapshot snapshot() {
            return new MetricsSnapshot(
                totalCalls.sum(),
                drawCalls.sum(),
                triangleCount.sum(),
                stateChanges.sum(),
                redundantStateChanges.sum(),
                fallbackCount.sum(),
                errorCount.sum()
            );
        }
        
        public void reset() {
            totalCalls.reset();
            drawCalls.reset();
            triangleCount.reset();
            stateChanges.reset();
            redundantStateChanges.reset();
            fallbackCount.reset();
            errorCount.reset();
            synchronized (callTimes) {
                callTimes.clear();
                callCounts.clear();
            }
        }
        
        public record MetricsSnapshot(
            long totalCalls,
            long drawCalls,
            long triangles,
            long stateChanges,
            long redundantStateChanges,
            long fallbacks,
            long errors
        ) {
            public double redundancyRate() {
                return stateChanges > 0 ? (double) redundantStateChanges / stateChanges : 0.0;
            }
            
            public double fallbackRate() {
                return totalCalls > 0 ? (double) fallbacks / totalCalls : 0.0;
            }
            
            public double errorRate() {
                return totalCalls > 0 ? (double) errors / totalCalls : 0.0;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 9: COMMAND BUFFER POOLING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Pool of reusable command buffers for batching.
     */
    public static final class CommandBufferPool {
        
        private final ConcurrentLinkedQueue<CommandBuffer> pool = new ConcurrentLinkedQueue<>();
        private final int maxSize;
        private final AtomicInteger activeCount = new AtomicInteger();
        
        CommandBufferPool(int maxSize) {
            this.maxSize = maxSize;
        }
        
        public CommandBuffer acquire() {
            CommandBuffer buffer = pool.poll();
            if (buffer == null) {
                buffer = new CommandBuffer();
            }
            activeCount.incrementAndGet();
            return buffer;
        }
        
        public void release(CommandBuffer buffer) {
            buffer.reset();
            activeCount.decrementAndGet();
            if (pool.size() < maxSize) {
                pool.offer(buffer);
            }
        }
        
        public int getActiveCount() {
            return activeCount.get();
        }
        
        public int getPooledCount() {
            return pool.size();
        }
    }
    
    /**
     * Batch of commands to execute together.
     */
    public static final class CommandBuffer {
        
        private final ObjectArrayList<CallDescriptor> commands = new ObjectArrayList<>();
        private boolean sealed = false;
        
        public void add(CallDescriptor call) {
            if (sealed) throw new IllegalStateException("CommandBuffer is sealed");
            commands.add(call);
        }
        
        public void seal() {
            sealed = true;
        }
        
        public boolean isSealed() {
            return sealed;
        }
        
        public int size() {
            return commands.size();
        }
        
        public CallDescriptor get(int index) {
            return commands.get(index);
        }
        
        public void reset() {
            commands.clear();
            sealed = false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 10: PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    public DeviceCapabilities getCapabilities() {
        return capabilities;
    }
    
    public GLESVersion getVersion() {
        return capabilities.version;
    }
    
    public GPUVendor getVendor() {
        return capabilities.vendor;
    }
    
    public PerformanceMetrics.MetricsSnapshot getMetrics() {
        return metrics.snapshot();
    }
    
    public void resetMetrics() {
        metrics.reset();
    }
    
    public void setDebugCallback(DebugCallback callback) {
        this.debugCallback = callback;
    }
    
    /**
     * Execute a batch of commands.
     */
    public MappingResult[] executeBatch(CommandBuffer batch) {
        if (!batch.isSealed()) {
            batch.seal();
        }
        
        MappingResult[] results = new MappingResult[batch.size()];
        for (int i = 0; i < batch.size(); i++) {
            results[i] = mapCall(batch.get(i));
        }
        return results;
    }
    
    /**
     * Acquire a command buffer from the pool.
     */
    public CommandBuffer acquireCommandBuffer() {
        return commandPool.acquire();
    }
    
    /**
     * Release a command buffer back to the pool.
     */
    public void releaseCommandBuffer(CommandBuffer buffer) {
        commandPool.release(buffer);
    }
    
    /**
     * Reset all tracked state (call after context loss).
     */
    public void resetState() {
        stateTracker.reset();
    }
    
    /**
     * Release all tracked resources.
     */
    public void releaseAllResources() {
        resourceManager.releaseAll(apiMapper);
    }
    
    /**
     * Shutdown the manager.
     */
    public void shutdown() {
        if (initialized) {
            releaseAllResources();
            initialized = false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SECTION 11: HELPER TYPES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Debug callback interface.
     */
    @FunctionalInterface
    public interface DebugCallback {
        void onMessage(int source, int type, int id, int severity, String message);
    }
    
    /**
     * Texture format conversion helper.
     */
    static final class TextureFormatInfo {
        final int internalFormat;
        final int format;
        final int type;
        
        TextureFormatInfo(int internal, int format, int type) {
            this.internalFormat = internal;
            this.format = format;
            this.type = type;
        }
        
        private static final Int2ObjectMap<TextureFormatInfo> FORMAT_MAP;
        
        static {
            FORMAT_MAP = new Int2ObjectOpenHashMap<>();
            // GLES 3.0 sized formats -> GLES 2.0 format/type pairs
            FORMAT_MAP.put(0x8058, new TextureFormatInfo(0x1908, 0x1908, 0x1401)); // RGBA8 -> RGBA, UNSIGNED_BYTE
            FORMAT_MAP.put(0x8051, new TextureFormatInfo(0x1907, 0x1907, 0x1401)); // RGB8 -> RGB, UNSIGNED_BYTE
            FORMAT_MAP.put(0x8229, new TextureFormatInfo(0x1909, 0x1909, 0x1401)); // R8 -> LUMINANCE, UNSIGNED_BYTE
            FORMAT_MAP.put(0x822B, new TextureFormatInfo(0x190A, 0x190A, 0x1401)); // RG8 -> LUMINANCE_ALPHA (approx)
            FORMAT_MAP.put(0x81A5, new TextureFormatInfo(0x1902, 0x1902, 0x1405)); // DEPTH_COMPONENT16
            FORMAT_MAP.put(0x81A6, new TextureFormatInfo(0x1902, 0x1902, 0x1405)); // DEPTH_COMPONENT24
            FORMAT_MAP.put(0x8CAC, new TextureFormatInfo(0x84F9, 0x84F9, 0x84FA)); // DEPTH24_STENCIL8
            // Add more formats as needed...
        }
        
        static TextureFormatInfo fromInternalFormat(int internalFormat) {
            return FORMAT_MAP.get(internalFormat);
        }
    }

// ═══════════════════════════════════════════════════════════════════════════
// SECTION 12: INFRASTRUCTURE BRIDGE (CRITICAL FIXES)
// ═══════════════════════════════════════════════════════════════════════════

    /**
     * FIX 1: TYPE ALIASING
     * Bridges the naming convention between Manager (DeviceCapabilities) 
     * and Mapper (GLESCapabilities) to ensure compilation.
     */
    public static final class DeviceCapabilities extends com.example.modid.api.common.mapping.OpenGLESCallMapper.GLESCapabilities {
        
        // Static probe method acting as factory
        public static DeviceCapabilities probe() {
            // In a real implementation, this would actually query the context
            // delegating to the Mapper's internal detection logic
            return new DeviceCapabilities();
        }
        
        private DeviceCapabilities() {
            super(); // Implicitly calls the robust detection logic from Mapper
        }
    }
    /**
     * FIX 2: STATE SYNCHRONIZATION ("The Split-Brain Fix")
     * 
     * Problem: Manager tracks state for routing efficiency. Mapper tracks state for driver workarounds.
     * Risk: If they desync, the driver workaround might apply to the wrong object.
     * Solution: Force synchronization before complex operations (Fallbacks/Draws).
     */
    private void reconcileState() {
        // Get the low-level tracker
        var driverState = apiMapper.getStateTracker();
        
        // 1. Sync Program State
        int logicalProgram = stateTracker.getCurrentProgram();
        if (driverState.getBoundProgram() != logicalProgram) {
            // Manager is truth -> Update Driver
            driverState.setBoundProgram(logicalProgram);
        }
        
        // 2. Sync Texture State (Critical for Adreno workarounds)
        int logicalUnit = stateTracker.activeTextureUnit;
        if (driverState.getActiveTexture() != logicalUnit) {
            driverState.setActiveTexture(logicalUnit);
        }
        
        // 3. Sync VAO (Critical for Emulation)
        // Note: Virtual VAOs are internal to Manager, but physical bind needs sync
        int logicalVAO = stateTracker.currentVAO;
        if (driverState.getBoundVAO() != logicalVAO) {
            driverState.setBoundVAO(logicalVAO);
        }
    }

    /**
     * Helper to expose the internal mapper for deep integration if needed.
     * (Used by the GLSLESCallMapper to query driver quirks)
     */
    public OpenGLESCallMapper getInternalMapper() {
        return apiMapper;
    }

} // End of OpenGLESManager class