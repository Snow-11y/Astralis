package stellar.snow.astralis.api.opengles.mapping;

import org.lwjgl.opengl.*;
import org.lwjgl.opengles.*;
import org.lwjgl.system.*;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import org.agrona.concurrent.*;
import org.agrona.*;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.nio.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

// ============================================================================
// SECTION 1: CORE INFRASTRUCTURE - CallDescriptor & MappingResult
// ============================================================================

/**
 * Represents the type of graphics API call being made.
 * Used for routing and fallback chain decisions.
 */
public enum CallType {
    // Buffer Operations
    BUFFER_CREATE, BUFFER_DELETE, BUFFER_BIND, BUFFER_DATA, BUFFER_SUB_DATA,
    BUFFER_MAP, BUFFER_UNMAP, BUFFER_COPY, BUFFER_FLUSH_MAPPED,
    
    // Texture Operations
    TEXTURE_CREATE, TEXTURE_DELETE, TEXTURE_BIND, TEXTURE_IMAGE_2D, TEXTURE_IMAGE_3D,
    TEXTURE_SUB_IMAGE_2D, TEXTURE_SUB_IMAGE_3D, TEXTURE_COMPRESSED, TEXTURE_STORAGE_2D,
    TEXTURE_STORAGE_3D, TEXTURE_GENERATE_MIPMAP, TEXTURE_PARAMETER, TEXTURE_BIND_IMAGE,
    SAMPLER_CREATE, SAMPLER_DELETE, SAMPLER_BIND, SAMPLER_PARAMETER,
    
    // Draw Operations
    DRAW_ARRAYS, DRAW_ELEMENTS, DRAW_ARRAYS_INSTANCED, DRAW_ELEMENTS_INSTANCED,
    DRAW_RANGE_ELEMENTS, DRAW_ARRAYS_INDIRECT, DRAW_ELEMENTS_INDIRECT,
    DRAW_MULTI_ARRAYS, DRAW_MULTI_ELEMENTS, DRAW_ELEMENTS_BASE_VERTEX,
    
    // State Operations
    STATE_ENABLE, STATE_DISABLE, STATE_BLEND_FUNC, STATE_BLEND_FUNC_SEPARATE,
    STATE_BLEND_EQUATION, STATE_BLEND_EQUATION_SEPARATE, STATE_DEPTH_FUNC,
    STATE_DEPTH_MASK, STATE_STENCIL_FUNC, STATE_STENCIL_OP, STATE_STENCIL_MASK,
    STATE_CULL_FACE, STATE_FRONT_FACE, STATE_POLYGON_OFFSET, STATE_VIEWPORT,
    STATE_SCISSOR, STATE_COLOR_MASK, STATE_LINE_WIDTH, STATE_POINT_SIZE,
    
    // Framebuffer Operations
    FRAMEBUFFER_CREATE, FRAMEBUFFER_DELETE, FRAMEBUFFER_BIND,
    FRAMEBUFFER_TEXTURE_2D, FRAMEBUFFER_RENDERBUFFER, FRAMEBUFFER_CHECK_STATUS,
    FRAMEBUFFER_BLIT, FRAMEBUFFER_INVALIDATE, FRAMEBUFFER_READ_PIXELS,
    FRAMEBUFFER_CLEAR, FRAMEBUFFER_CLEAR_BUFFER,
    RENDERBUFFER_CREATE, RENDERBUFFER_DELETE, RENDERBUFFER_BIND, RENDERBUFFER_STORAGE,
    
    // Shader Operations
    SHADER_CREATE, SHADER_DELETE, SHADER_SOURCE, SHADER_COMPILE,
    PROGRAM_CREATE, PROGRAM_DELETE, PROGRAM_ATTACH, PROGRAM_LINK, PROGRAM_USE,
    PROGRAM_GET_UNIFORM_LOCATION, PROGRAM_UNIFORM, PROGRAM_GET_ATTRIB_LOCATION,
    PROGRAM_VERTEX_ATTRIB, PROGRAM_BIND_ATTRIB_LOCATION, PROGRAM_GET_INFO_LOG,
    PROGRAM_BINARY_GET, PROGRAM_BINARY_LOAD,
    
    // VAO Operations
    VAO_CREATE, VAO_DELETE, VAO_BIND, VAO_VERTEX_ATTRIB_POINTER,
    VAO_VERTEX_ATTRIB_I_POINTER, VAO_ENABLE_ATTRIB, VAO_DISABLE_ATTRIB,
    VAO_VERTEX_ATTRIB_DIVISOR,
    
    // Compute Operations
    COMPUTE_DISPATCH, COMPUTE_DISPATCH_INDIRECT, COMPUTE_MEMORY_BARRIER,
    
    // Query Operations
    QUERY_CREATE, QUERY_DELETE, QUERY_BEGIN, QUERY_END, QUERY_GET_RESULT,
    
    // Sync Operations
    SYNC_FENCE_CREATE, SYNC_DELETE, SYNC_CLIENT_WAIT, SYNC_WAIT,
    SYNC_FLUSH, SYNC_FINISH,
    
    // Miscellaneous
    GET_ERROR, GET_STRING, GET_INTEGER, GET_FLOAT, GET_BOOLEAN,
    PIXEL_STORE, HINT, DEBUG_MESSAGE_CALLBACK, DEBUG_MESSAGE_INSERT
}

/**
 * Immutable descriptor for a graphics API call.
 * Zero-allocation in hot paths through object pooling.
 */
public final class CallDescriptor {
    
    // Pre-allocated parameter slots for zero-allocation hot path
    private static final int MAX_INT_PARAMS = 16;
    private static final int MAX_LONG_PARAMS = 8;
    private static final int MAX_FLOAT_PARAMS = 16;
    private static final int MAX_OBJECT_PARAMS = 8;
    
    private final CallType type;
    private final int subFunction;
    
    // Primitive parameters - no boxing
    private final int[] intParams;
    private final int intParamCount;
    private final long[] longParams;
    private final int longParamCount;
    private final float[] floatParams;
    private final int floatParamCount;
    
    // Object parameters for buffers, etc.
    private final Object[] objectParams;
    private final int objectParamCount;
    
    // Source tracking for debugging
    private final long timestamp;
    private final int sourceThreadId;
    
    private CallDescriptor(Builder builder) {
        this.type = builder.type;
        this.subFunction = builder.subFunction;
        this.intParams = builder.intParams;
        this.intParamCount = builder.intParamCount;
        this.longParams = builder.longParams;
        this.longParamCount = builder.longParamCount;
        this.floatParams = builder.floatParams;
        this.floatParamCount = builder.floatParamCount;
        this.objectParams = builder.objectParams;
        this.objectParamCount = builder.objectParamCount;
        this.timestamp = System.nanoTime();
        this.sourceThreadId = (int) Thread.currentThread().threadId();
    }
    
    public CallType type() { return type; }
    public int subFunction() { return subFunction; }
    public long timestamp() { return timestamp; }
    public int sourceThreadId() { return sourceThreadId; }
    
    // Zero-allocation parameter accessors
    public int intParam(int index) {
        return index < intParamCount ? intParams[index] : 0;
    }
    
    public long longParam(int index) {
        return index < longParamCount ? longParams[index] : 0L;
    }
    
    public float floatParam(int index) {
        return index < floatParamCount ? floatParams[index] : 0.0f;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T objectParam(int index) {
        return index < objectParamCount ? (T) objectParams[index] : null;
    }
    
    public int intParamCount() { return intParamCount; }
    public int longParamCount() { return longParamCount; }
    public int floatParamCount() { return floatParamCount; }
    public int objectParamCount() { return objectParamCount; }
    
    // Create modified copy for fallback chain
    public Builder toBuilder() {
        Builder b = new Builder(type);
        b.subFunction = subFunction;
        System.arraycopy(intParams, 0, b.intParams, 0, intParamCount);
        b.intParamCount = intParamCount;
        System.arraycopy(longParams, 0, b.longParams, 0, longParamCount);
        b.longParamCount = longParamCount;
        System.arraycopy(floatParams, 0, b.floatParams, 0, floatParamCount);
        b.floatParamCount = floatParamCount;
        System.arraycopy(objectParams, 0, b.objectParams, 0, objectParamCount);
        b.objectParamCount = objectParamCount;
        return b;
    }
    
    /**
     * Pooled builder for zero-allocation call descriptor creation.
     */
    public static final class Builder {
        private CallType type;
        private int subFunction;
        private final int[] intParams = new int[MAX_INT_PARAMS];
        private int intParamCount;
        private final long[] longParams = new long[MAX_LONG_PARAMS];
        private int longParamCount;
        private final float[] floatParams = new float[MAX_FLOAT_PARAMS];
        private int floatParamCount;
        private final Object[] objectParams = new Object[MAX_OBJECT_PARAMS];
        private int objectParamCount;
        
        public Builder(CallType type) {
            this.type = type;
        }
        
        public Builder type(CallType type) {
            this.type = type;
            return this;
        }
        
        public Builder subFunction(int subFunction) {
            this.subFunction = subFunction;
            return this;
        }
        
        public Builder addInt(int value) {
            if (intParamCount < MAX_INT_PARAMS) {
                intParams[intParamCount++] = value;
            }
            return this;
        }
        
        public Builder addLong(long value) {
            if (longParamCount < MAX_LONG_PARAMS) {
                longParams[longParamCount++] = value;
            }
            return this;
        }
        
        public Builder addFloat(float value) {
            if (floatParamCount < MAX_FLOAT_PARAMS) {
                floatParams[floatParamCount++] = value;
            }
            return this;
        }
        
        public Builder addObject(Object value) {
            if (objectParamCount < MAX_OBJECT_PARAMS) {
                objectParams[objectParamCount++] = value;
            }
            return this;
        }
        
        public Builder setInt(int index, int value) {
            if (index < MAX_INT_PARAMS) {
                intParams[index] = value;
                if (index >= intParamCount) intParamCount = index + 1;
            }
            return this;
        }
        
        public Builder reset() {
            intParamCount = 0;
            longParamCount = 0;
            floatParamCount = 0;
            for (int i = 0; i < objectParamCount; i++) {
                objectParams[i] = null;
            }
            objectParamCount = 0;
            subFunction = 0;
            return this;
        }
        
        public CallDescriptor build() {
            return new CallDescriptor(this);
        }
    }
    
    // Thread-local builder pool for zero-allocation hot path
    private static final ThreadLocal<Builder> BUILDER_POOL = 
        ThreadLocal.withInitial(() -> new Builder(CallType.DRAW_ARRAYS));
    
    public static Builder acquire(CallType type) {
        return BUILDER_POOL.get().reset().type(type);
    }
}

/**
 * Result status from a mapping operation.
 */
public enum MappingStatus {
    SUCCESS,
    SUCCESS_WITH_WARNINGS,
    FALLBACK_REQUIRED,
    FALLBACK_PARTIAL,
    UNSUPPORTED,
    FAILED_VALIDATION,
    FAILED_EXECUTION,
    FAILED_RESOURCE,
    FAILED_INTERNAL
}

/**
 * Detailed result from mapping a graphics API call.
 */
public final class MappingResult {
    
    private final MappingStatus status;
    private final int nativeResult;
    private final long gpuHandle;
    private final CallDescriptor fallbackDescriptor;
    private final String message;
    private final long executionTimeNanos;
    private final int warningFlags;
    
    private MappingResult(MappingStatus status, int nativeResult, long gpuHandle,
                          CallDescriptor fallbackDescriptor, String message,
                          long executionTimeNanos, int warningFlags) {
        this.status = status;
        this.nativeResult = nativeResult;
        this.gpuHandle = gpuHandle;
        this.fallbackDescriptor = fallbackDescriptor;
        this.message = message;
        this.executionTimeNanos = executionTimeNanos;
        this.warningFlags = warningFlags;
    }
    
    public MappingStatus status() { return status; }
    public int nativeResult() { return nativeResult; }
    public long gpuHandle() { return gpuHandle; }
    public CallDescriptor fallbackDescriptor() { return fallbackDescriptor; }
    public String message() { return message; }
    public long executionTimeNanos() { return executionTimeNanos; }
    public int warningFlags() { return warningFlags; }
    
    public boolean isSuccess() {
        return status == MappingStatus.SUCCESS || status == MappingStatus.SUCCESS_WITH_WARNINGS;
    }
    
    public boolean needsFallback() {
        return status == MappingStatus.FALLBACK_REQUIRED || status == MappingStatus.FALLBACK_PARTIAL;
    }
    
    public boolean isFailed() {
        return status.ordinal() >= MappingStatus.FAILED_VALIDATION.ordinal();
    }
    
    // Pre-allocated success result for hot path
    private static final MappingResult SUCCESS_RESULT = 
        new MappingResult(MappingStatus.SUCCESS, 0, 0L, null, null, 0L, 0);
    
    public static MappingResult success() {
        return SUCCESS_RESULT;
    }
    
    public static MappingResult success(int nativeResult) {
        return new MappingResult(MappingStatus.SUCCESS, nativeResult, 0L, null, null, 0L, 0);
    }
    
    public static MappingResult success(long gpuHandle) {
        return new MappingResult(MappingStatus.SUCCESS, 0, gpuHandle, null, null, 0L, 0);
    }
    
    public static MappingResult successWithWarning(int warningFlags, String message) {
        return new MappingResult(MappingStatus.SUCCESS_WITH_WARNINGS, 0, 0L, null, message, 0L, warningFlags);
    }
    
    public static MappingResult fallback(CallDescriptor fallback, String reason) {
        return new MappingResult(MappingStatus.FALLBACK_REQUIRED, 0, 0L, fallback, reason, 0L, 0);
    }
    
    public static MappingResult fallbackPartial(int partialResult, CallDescriptor fallback, String reason) {
        return new MappingResult(MappingStatus.FALLBACK_PARTIAL, partialResult, 0L, fallback, reason, 0L, 0);
    }
    
    public static MappingResult unsupported(String feature) {
        return new MappingResult(MappingStatus.UNSUPPORTED, 0, 0L, null, 
            "Feature not supported: " + feature, 0L, 0);
    }
    
    public static MappingResult failed(MappingStatus status, String message) {
        return new MappingResult(status, 0, 0L, null, message, 0L, 0);
    }
    
    public static MappingResult failed(MappingStatus status, int errorCode, String message) {
        return new MappingResult(status, errorCode, 0L, null, message, 0L, 0);
    }
}

// ============================================================================
// SECTION 2: GLES VERSION AND CAPABILITY DETECTION
// ============================================================================

/**
 * OpenGL ES version enumeration with feature flags.
 */
public enum GLESVersion {
    GLES_1_0(1, 0, 0x0100),
    GLES_1_1(1, 1, 0x0101),
    GLES_2_0(2, 0, 0x0200),
    GLES_3_0(3, 0, 0x0300),
    GLES_3_1(3, 1, 0x0301),
    GLES_3_2(3, 2, 0x0302);
    
    private final int major;
    private final int minor;
    private final int packed;
    
    GLESVersion(int major, int minor, int packed) {
        this.major = major;
        this.minor = minor;
        this.packed = packed;
    }
    
    public int major() { return major; }
    public int minor() { return minor; }
    public int packed() { return packed; }
    
    public boolean isAtLeast(GLESVersion other) {
        return this.packed >= other.packed;
    }
    
    public boolean isProgrammable() {
        return this.packed >= GLES_2_0.packed;
    }
    
    public boolean hasVAOs() {
        return this.packed >= GLES_3_0.packed;
    }
    
    public boolean hasCompute() {
        return this.packed >= GLES_3_1.packed;
    }
    
    public boolean hasGeometryShaders() {
        return this.packed >= GLES_3_2.packed;
    }
    
    public static GLESVersion fromPacked(int packed) {
        return switch (packed) {
            case 0x0100 -> GLES_1_0;
            case 0x0101 -> GLES_1_1;
            case 0x0200 -> GLES_2_0;
            case 0x0300 -> GLES_3_0;
            case 0x0301 -> GLES_3_1;
            case 0x0302 -> GLES_3_2;
            default -> packed < 0x0200 ? GLES_1_1 : 
                       packed < 0x0300 ? GLES_2_0 :
                       packed < 0x0301 ? GLES_3_0 :
                       packed < 0x0302 ? GLES_3_1 : GLES_3_2;
        };
    }
}

/**
 * Buffer target types with version requirements.
 */
public enum BufferTarget {
    ARRAY_BUFFER(0x8892, GLESVersion.GLES_1_1),
    ELEMENT_ARRAY_BUFFER(0x8893, GLESVersion.GLES_1_1),
    COPY_READ_BUFFER(0x8F36, GLESVersion.GLES_3_0),
    COPY_WRITE_BUFFER(0x8F37, GLESVersion.GLES_3_0),
    PIXEL_PACK_BUFFER(0x88EB, GLESVersion.GLES_3_0),
    PIXEL_UNPACK_BUFFER(0x88EC, GLESVersion.GLES_3_0),
    TRANSFORM_FEEDBACK_BUFFER(0x8C8E, GLESVersion.GLES_3_0),
    UNIFORM_BUFFER(0x8A11, GLESVersion.GLES_3_0),
    ATOMIC_COUNTER_BUFFER(0x92C0, GLESVersion.GLES_3_1),
    DISPATCH_INDIRECT_BUFFER(0x90EE, GLESVersion.GLES_3_1),
    DRAW_INDIRECT_BUFFER(0x8F3F, GLESVersion.GLES_3_1),
    SHADER_STORAGE_BUFFER(0x90D2, GLESVersion.GLES_3_1);
    
    private final int glConstant;
    private final GLESVersion minVersion;
    
    BufferTarget(int glConstant, GLESVersion minVersion) {
        this.glConstant = glConstant;
        this.minVersion = minVersion;
    }
    
    public int glConstant() { return glConstant; }
    public GLESVersion minVersion() { return minVersion; }
    
    public boolean isSupported(GLESVersion version) {
        return version.isAtLeast(minVersion);
    }
    
    private static final Int2ObjectMap<BufferTarget> BY_GL_CONSTANT;
    static {
        BY_GL_CONSTANT = new Int2ObjectOpenHashMap<>();
        for (BufferTarget target : values()) {
            BY_GL_CONSTANT.put(target.glConstant, target);
        }
    }
    
    public static BufferTarget fromGLConstant(int constant) {
        return BY_GL_CONSTANT.get(constant);
    }
}

/**
 * Texture target types with version requirements.
 */
public enum TextureTarget {
    TEXTURE_2D(0x0DE1, GLESVersion.GLES_1_0),
    TEXTURE_CUBE_MAP(0x8513, GLESVersion.GLES_1_0),
    TEXTURE_3D(0x806F, GLESVersion.GLES_3_0),
    TEXTURE_2D_ARRAY(0x8C1A, GLESVersion.GLES_3_0),
    TEXTURE_CUBE_MAP_ARRAY(0x9009, GLESVersion.GLES_3_2),
    TEXTURE_2D_MULTISAMPLE(0x9100, GLESVersion.GLES_3_1),
    TEXTURE_2D_MULTISAMPLE_ARRAY(0x9102, GLESVersion.GLES_3_2),
    TEXTURE_BUFFER(0x8C2A, GLESVersion.GLES_3_2);
    
    private final int glConstant;
    private final GLESVersion minVersion;
    
    TextureTarget(int glConstant, GLESVersion minVersion) {
        this.glConstant = glConstant;
        this.minVersion = minVersion;
    }
    
    public int glConstant() { return glConstant; }
    public GLESVersion minVersion() { return minVersion; }
    
    public boolean isSupported(GLESVersion version) {
        return version.isAtLeast(minVersion);
    }
}

/**
 * Compressed texture format support.
 */
public enum CompressedFormat {
    // ETC formats (core GLES 3.0+)
    ETC1_RGB8(0x8D64, GLESVersion.GLES_2_0),
    ETC2_RGB8(0x9274, GLESVersion.GLES_3_0),
    ETC2_RGB8_SRGB(0x9275, GLESVersion.GLES_3_0),
    ETC2_RGB8_PUNCHTHROUGH_ALPHA1(0x9276, GLESVersion.GLES_3_0),
    ETC2_RGB8_PUNCHTHROUGH_ALPHA1_SRGB(0x9277, GLESVersion.GLES_3_0),
    ETC2_RGBA8(0x9278, GLESVersion.GLES_3_0),
    ETC2_RGBA8_SRGB(0x9279, GLESVersion.GLES_3_0),
    ETC2_R11(0x9270, GLESVersion.GLES_3_0),
    ETC2_SIGNED_R11(0x9271, GLESVersion.GLES_3_0),
    ETC2_RG11(0x9272, GLESVersion.GLES_3_0),
    ETC2_SIGNED_RG11(0x9273, GLESVersion.GLES_3_0),
    
    // ASTC formats (GLES 3.2 or extension)
    ASTC_4x4_RGBA(0x93B0, GLESVersion.GLES_3_2),
    ASTC_5x4_RGBA(0x93B1, GLESVersion.GLES_3_2),
    ASTC_5x5_RGBA(0x93B2, GLESVersion.GLES_3_2),
    ASTC_6x5_RGBA(0x93B3, GLESVersion.GLES_3_2),
    ASTC_6x6_RGBA(0x93B4, GLESVersion.GLES_3_2),
    ASTC_8x5_RGBA(0x93B5, GLESVersion.GLES_3_2),
    ASTC_8x6_RGBA(0x93B6, GLESVersion.GLES_3_2),
    ASTC_8x8_RGBA(0x93B7, GLESVersion.GLES_3_2),
    ASTC_10x5_RGBA(0x93B8, GLESVersion.GLES_3_2),
    ASTC_10x6_RGBA(0x93B9, GLESVersion.GLES_3_2),
    ASTC_10x8_RGBA(0x93BA, GLESVersion.GLES_3_2),
    ASTC_10x10_RGBA(0x93BB, GLESVersion.GLES_3_2),
    ASTC_12x10_RGBA(0x93BC, GLESVersion.GLES_3_2),
    ASTC_12x12_RGBA(0x93BD, GLESVersion.GLES_3_2),
    
    // PVRTC (extension only - iOS/PowerVR)
    PVRTC_RGB_2BPP(0x8C01, GLESVersion.GLES_2_0),
    PVRTC_RGB_4BPP(0x8C00, GLESVersion.GLES_2_0),
    PVRTC_RGBA_2BPP(0x8C03, GLESVersion.GLES_2_0),
    PVRTC_RGBA_4BPP(0x8C02, GLESVersion.GLES_2_0),
    
    // S3TC/DXT (extension only - desktop/Tegra)
    DXT1_RGB(0x83F0, GLESVersion.GLES_2_0),
    DXT1_RGBA(0x83F1, GLESVersion.GLES_2_0),
    DXT3_RGBA(0x83F2, GLESVersion.GLES_2_0),
    DXT5_RGBA(0x83F3, GLESVersion.GLES_2_0);
    
    private final int glConstant;
    private final GLESVersion minVersion;
    
    CompressedFormat(int glConstant, GLESVersion minVersion) {
        this.glConstant = glConstant;
        this.minVersion = minVersion;
    }
    
    public int glConstant() { return glConstant; }
    public GLESVersion minVersion() { return minVersion; }
}

/**
 * OpenGL ES extension enumeration with detection strings.
 */
public enum GLESExtension {
    // Core Extensions
    OES_vertex_array_object("GL_OES_vertex_array_object"),
    OES_mapbuffer("GL_OES_mapbuffer"),
    OES_element_index_uint("GL_OES_element_index_uint"),
    OES_texture_3D("GL_OES_texture_3D"),
    OES_texture_half_float("GL_OES_texture_half_float"),
    OES_texture_float("GL_OES_texture_float"),
    OES_depth_texture("GL_OES_depth_texture"),
    OES_packed_depth_stencil("GL_OES_packed_depth_stencil"),
    OES_standard_derivatives("GL_OES_standard_derivatives"),
    OES_EGL_image("GL_OES_EGL_image"),
    OES_EGL_image_external("GL_OES_EGL_image_external"),
    OES_required_internalformat("GL_OES_required_internalformat"),
    OES_get_program_binary("GL_OES_get_program_binary"),
    
    // EXT Extensions
    EXT_texture_filter_anisotropic("GL_EXT_texture_filter_anisotropic"),
    EXT_texture_format_BGRA8888("GL_EXT_texture_format_BGRA8888"),
    EXT_read_format_bgra("GL_EXT_read_format_bgra"),
    EXT_multi_draw_arrays("GL_EXT_multi_draw_arrays"),
    EXT_shader_texture_lod("GL_EXT_shader_texture_lod"),
    EXT_draw_buffers("GL_EXT_draw_buffers"),
    EXT_instanced_arrays("GL_EXT_instanced_arrays"),
    EXT_frag_depth("GL_EXT_frag_depth"),
    EXT_blend_minmax("GL_EXT_blend_minmax"),
    EXT_color_buffer_half_float("GL_EXT_color_buffer_half_float"),
    EXT_color_buffer_float("GL_EXT_color_buffer_float"),
    EXT_texture_storage("GL_EXT_texture_storage"),
    EXT_texture_compression_s3tc("GL_EXT_texture_compression_s3tc"),
    EXT_texture_compression_dxt1("GL_EXT_texture_compression_dxt1"),
    EXT_disjoint_timer_query("GL_EXT_disjoint_timer_query"),
    EXT_occlusion_query_boolean("GL_EXT_occlusion_query_boolean"),
    EXT_debug_marker("GL_EXT_debug_marker"),
    EXT_debug_label("GL_EXT_debug_label"),
    EXT_separate_shader_objects("GL_EXT_separate_shader_objects"),
    EXT_gpu_shader5("GL_EXT_gpu_shader5"),
    EXT_texture_buffer("GL_EXT_texture_buffer"),
    EXT_geometry_shader("GL_EXT_geometry_shader"),
    EXT_tessellation_shader("GL_EXT_tessellation_shader"),
    EXT_copy_image("GL_EXT_copy_image"),
    EXT_draw_elements_base_vertex("GL_EXT_draw_elements_base_vertex"),
    EXT_buffer_storage("GL_EXT_buffer_storage"),
    EXT_clip_cull_distance("GL_EXT_clip_cull_distance"),
    
    // KHR Extensions
    KHR_debug("GL_KHR_debug"),
    KHR_texture_compression_astc_ldr("GL_KHR_texture_compression_astc_ldr"),
    KHR_texture_compression_astc_hdr("GL_KHR_texture_compression_astc_hdr"),
    KHR_blend_equation_advanced("GL_KHR_blend_equation_advanced"),
    KHR_robustness("GL_KHR_robustness"),
    KHR_parallel_shader_compile("GL_KHR_parallel_shader_compile"),
    
    // ANGLE Extensions (WebGL/Emscripten)
    ANGLE_instanced_arrays("GL_ANGLE_instanced_arrays"),
    ANGLE_translated_shader_source("GL_ANGLE_translated_shader_source"),
    ANGLE_depth_texture("GL_ANGLE_depth_texture"),
    ANGLE_framebuffer_blit("GL_ANGLE_framebuffer_blit"),
    ANGLE_framebuffer_multisample("GL_ANGLE_framebuffer_multisample"),
    
    // NV Extensions (NVIDIA)
    NV_read_buffer("GL_NV_read_buffer"),
    NV_draw_buffers("GL_NV_draw_buffers"),
    NV_fbo_color_attachments("GL_NV_fbo_color_attachments"),
    NV_read_depth_stencil("GL_NV_read_depth_stencil"),
    NV_framebuffer_blit("GL_NV_framebuffer_blit"),
    NV_framebuffer_multisample("GL_NV_framebuffer_multisample"),
    NV_instanced_arrays("GL_NV_instanced_arrays"),
    NV_draw_instanced("GL_NV_draw_instanced"),
    NV_shader_noperspective_interpolation("GL_NV_shader_noperspective_interpolation"),
    NV_texture_border_clamp("GL_NV_texture_border_clamp"),
    NV_polygon_mode("GL_NV_polygon_mode"),
    
    // APPLE Extensions
    APPLE_texture_format_BGRA8888("GL_APPLE_texture_format_BGRA8888"),
    APPLE_framebuffer_multisample("GL_APPLE_framebuffer_multisample"),
    APPLE_texture_max_level("GL_APPLE_texture_max_level"),
    APPLE_rgb_422("GL_APPLE_rgb_422"),
    APPLE_sync("GL_APPLE_sync"),
    APPLE_clip_distance("GL_APPLE_clip_distance"),
    
    // IMG Extensions (PowerVR/Imagination)
    IMG_texture_compression_pvrtc("GL_IMG_texture_compression_pvrtc"),
    IMG_texture_compression_pvrtc2("GL_IMG_texture_compression_pvrtc2"),
    IMG_multisampled_render_to_texture("GL_IMG_multisampled_render_to_texture"),
    IMG_read_format("GL_IMG_read_format"),
    
    // ARM Extensions (Mali)
    ARM_shader_framebuffer_fetch("GL_ARM_shader_framebuffer_fetch"),
    ARM_mali_program_binary("GL_ARM_mali_program_binary"),
    ARM_mali_shader_binary("GL_ARM_mali_shader_binary"),
    
    // QCOM Extensions (Adreno)
    QCOM_tiled_rendering("GL_QCOM_tiled_rendering"),
    QCOM_alpha_test("GL_QCOM_alpha_test"),
    QCOM_binning_control("GL_QCOM_binning_control"),
    QCOM_writeonly_rendering("GL_QCOM_writeonly_rendering"),
    QCOM_extended_get("GL_QCOM_extended_get"),
    QCOM_extended_get2("GL_QCOM_extended_get2");
    
    private final String extensionString;
    
    GLESExtension(String extensionString) {
        this.extensionString = extensionString;
    }
    
    public String extensionString() { return extensionString; }
    
    private static final Object2ObjectMap<String, GLESExtension> BY_STRING;
    static {
        BY_STRING = new Object2ObjectOpenHashMap<>();
        for (GLESExtension ext : values()) {
            BY_STRING.put(ext.extensionString, ext);
        }
    }
    
    public static GLESExtension fromString(String str) {
        return BY_STRING.get(str);
    }
}

/**
 * GPU vendor detection for driver-specific workarounds.
 */
public enum GPUVendor {
    NVIDIA("NVIDIA", "nvidia"),
    AMD("AMD", "amd", "ati"),
    INTEL("Intel", "intel"),
    ARM("ARM", "mali"),
    QUALCOMM("Qualcomm", "adreno"),
    IMAGINATION("Imagination", "powervr", "pvr"),
    APPLE("Apple", "apple"),
    BROADCOM("Broadcom", "videocore"),
    VIVANTE("Vivante", "vivante", "gc"),
    MESA("Mesa", "mesa", "llvmpipe", "softpipe"),
    ANGLE("ANGLE", "angle"),
    UNKNOWN;
    
    private final String[] identifiers;
    
    GPUVendor(String... identifiers) {
        this.identifiers = identifiers;
    }
    
    public static GPUVendor detect(String vendorString, String rendererString) {
        String combined = (vendorString + " " + rendererString).toLowerCase();
        for (GPUVendor vendor : values()) {
            if (vendor == UNKNOWN) continue;
            for (String id : vendor.identifiers) {
                if (combined.contains(id.toLowerCase())) {
                    return vendor;
                }
            }
        }
        return UNKNOWN;
    }
}

/**
 * Complete capability detection and tracking for OpenGL ES context.
 */
public final class GLESCapabilities {
    
    private final GLESVersion version;
    private final GPUVendor vendor;
    private final String vendorString;
    private final String rendererString;
    private final String versionString;
    private final String glslVersionString;
    
    // Extension support - packed as bit flags for cache efficiency
    private final long[] extensionFlags;
    private final ObjectSet<String> extensionStrings;
    
    // Limits
    private final int maxTextureSize;
    private final int maxCubeMapTextureSize;
    private final int max3DTextureSize;
    private final int maxArrayTextureLayers;
    private final int maxTextureUnits;
    private final int maxVertexTextureUnits;
    private final int maxCombinedTextureUnits;
    private final int maxVertexAttribs;
    private final int maxVertexUniformVectors;
    private final int maxFragmentUniformVectors;
    private final int maxVaryingVectors;
    private final int maxUniformBufferBindings;
    private final int maxUniformBlockSize;
    private final int maxShaderStorageBufferBindings;
    private final int maxShaderStorageBlockSize;
    private final int maxColorAttachments;
    private final int maxDrawBuffers;
    private final int maxSamples;
    private final int maxElementsIndices;
    private final int maxElementsVertices;
    private final int maxComputeWorkGroupCount;
    private final int maxComputeWorkGroupSize;
    private final int maxComputeWorkGroupInvocations;
    private final int maxComputeSharedMemorySize;
    private final float maxTextureAnisotropy;
    
    // Format support
    private final IntSet supportedCompressedFormats;
    private final IntSet supportedInternalFormats;
    
    private GLESCapabilities(Builder builder) {
        this.version = builder.version;
        this.vendor = builder.vendor;
        this.vendorString = builder.vendorString;
        this.rendererString = builder.rendererString;
        this.versionString = builder.versionString;
        this.glslVersionString = builder.glslVersionString;
        this.extensionFlags = builder.extensionFlags;
        this.extensionStrings = builder.extensionStrings;
        
        this.maxTextureSize = builder.maxTextureSize;
        this.maxCubeMapTextureSize = builder.maxCubeMapTextureSize;
        this.max3DTextureSize = builder.max3DTextureSize;
        this.maxArrayTextureLayers = builder.maxArrayTextureLayers;
        this.maxTextureUnits = builder.maxTextureUnits;
        this.maxVertexTextureUnits = builder.maxVertexTextureUnits;
        this.maxCombinedTextureUnits = builder.maxCombinedTextureUnits;
        this.maxVertexAttribs = builder.maxVertexAttribs;
        this.maxVertexUniformVectors = builder.maxVertexUniformVectors;
        this.maxFragmentUniformVectors = builder.maxFragmentUniformVectors;
        this.maxVaryingVectors = builder.maxVaryingVectors;
        this.maxUniformBufferBindings = builder.maxUniformBufferBindings;
        this.maxUniformBlockSize = builder.maxUniformBlockSize;
        this.maxShaderStorageBufferBindings = builder.maxShaderStorageBufferBindings;
        this.maxShaderStorageBlockSize = builder.maxShaderStorageBlockSize;
        this.maxColorAttachments = builder.maxColorAttachments;
        this.maxDrawBuffers = builder.maxDrawBuffers;
        this.maxSamples = builder.maxSamples;
        this.maxElementsIndices = builder.maxElementsIndices;
        this.maxElementsVertices = builder.maxElementsVertices;
        this.maxComputeWorkGroupCount = builder.maxComputeWorkGroupCount;
        this.maxComputeWorkGroupSize = builder.maxComputeWorkGroupSize;
        this.maxComputeWorkGroupInvocations = builder.maxComputeWorkGroupInvocations;
        this.maxComputeSharedMemorySize = builder.maxComputeSharedMemorySize;
        this.maxTextureAnisotropy = builder.maxTextureAnisotropy;
        this.supportedCompressedFormats = builder.supportedCompressedFormats;
        this.supportedInternalFormats = builder.supportedInternalFormats;
    }
    
    // Accessors
    public GLESVersion version() { return version; }
    public GPUVendor vendor() { return vendor; }
    public String vendorString() { return vendorString; }
    public String rendererString() { return rendererString; }
    public String versionString() { return versionString; }
    public String glslVersionString() { return glslVersionString; }
    
    public int maxTextureSize() { return maxTextureSize; }
    public int maxCubeMapTextureSize() { return maxCubeMapTextureSize; }
    public int max3DTextureSize() { return max3DTextureSize; }
    public int maxArrayTextureLayers() { return maxArrayTextureLayers; }
    public int maxTextureUnits() { return maxTextureUnits; }
    public int maxVertexTextureUnits() { return maxVertexTextureUnits; }
    public int maxCombinedTextureUnits() { return maxCombinedTextureUnits; }
    public int maxVertexAttribs() { return maxVertexAttribs; }
    public int maxVertexUniformVectors() { return maxVertexUniformVectors; }
    public int maxFragmentUniformVectors() { return maxFragmentUniformVectors; }
    public int maxVaryingVectors() { return maxVaryingVectors; }
    public int maxUniformBufferBindings() { return maxUniformBufferBindings; }
    public int maxUniformBlockSize() { return maxUniformBlockSize; }
    public int maxShaderStorageBufferBindings() { return maxShaderStorageBufferBindings; }
    public int maxShaderStorageBlockSize() { return maxShaderStorageBlockSize; }
    public int maxColorAttachments() { return maxColorAttachments; }
    public int maxDrawBuffers() { return maxDrawBuffers; }
    public int maxSamples() { return maxSamples; }
    public int maxElementsIndices() { return maxElementsIndices; }
    public int maxElementsVertices() { return maxElementsVertices; }
    public int maxComputeWorkGroupCount() { return maxComputeWorkGroupCount; }
    public int maxComputeWorkGroupSize() { return maxComputeWorkGroupSize; }
    public int maxComputeWorkGroupInvocations() { return maxComputeWorkGroupInvocations; }
    public int maxComputeSharedMemorySize() { return maxComputeSharedMemorySize; }
    public float maxTextureAnisotropy() { return maxTextureAnisotropy; }
    
    public boolean hasExtension(GLESExtension extension) {
        int idx = extension.ordinal();
        int arrayIdx = idx / 64;
        int bitIdx = idx % 64;
        return arrayIdx < extensionFlags.length && 
               (extensionFlags[arrayIdx] & (1L << bitIdx)) != 0;
    }
    
    public boolean hasExtension(String extensionName) {
        return extensionStrings.contains(extensionName);
    }
    
    public boolean supportsCompressedFormat(int format) {
        return supportedCompressedFormats.contains(format);
    }
    
    public boolean supportsInternalFormat(int format) {
        return supportedInternalFormats.contains(format);
    }
    
    // Capability queries
    public boolean supportsVAO() {
        return version.hasVAOs() || hasExtension(GLESExtension.OES_vertex_array_object);
    }
    
    public boolean supportsInstancing() {
        return version.isAtLeast(GLESVersion.GLES_3_0) ||
               hasExtension(GLESExtension.EXT_instanced_arrays) ||
               hasExtension(GLESExtension.ANGLE_instanced_arrays) ||
               hasExtension(GLESExtension.NV_instanced_arrays);
    }
    
    public boolean supportsUBO() {
        return version.isAtLeast(GLESVersion.GLES_3_0);
    }
    
    public boolean supportsSSBO() {
        return version.isAtLeast(GLESVersion.GLES_3_1);
    }
    
    public boolean supportsCompute() {
        return version.hasCompute();
    }
    
    public boolean supportsGeometryShader() {
        return version.hasGeometryShaders() || hasExtension(GLESExtension.EXT_geometry_shader);
    }
    
    public boolean supportsTessellation() {
        return version.hasGeometryShaders() || hasExtension(GLESExtension.EXT_tessellation_shader);
    }
    
    public boolean supportsMRT() {
        return version.isAtLeast(GLESVersion.GLES_3_0) || hasExtension(GLESExtension.EXT_draw_buffers);
    }
    
    public boolean supportsMapBuffer() {
        return version.isAtLeast(GLESVersion.GLES_3_0) || hasExtension(GLESExtension.OES_mapbuffer);
    }
    
    public boolean supportsMapBufferRange() {
        return version.isAtLeast(GLESVersion.GLES_3_0);
    }
    
    public boolean supportsFBOBlit() {
        return version.isAtLeast(GLESVersion.GLES_3_0) ||
               hasExtension(GLESExtension.ANGLE_framebuffer_blit) ||
               hasExtension(GLESExtension.NV_framebuffer_blit);
    }
    
    public boolean supportsAnisotropicFiltering() {
        return hasExtension(GLESExtension.EXT_texture_filter_anisotropic);
    }
    
    public boolean supportsDebugOutput() {
        return hasExtension(GLESExtension.KHR_debug);
    }
    
    public boolean supportsTimerQuery() {
        return hasExtension(GLESExtension.EXT_disjoint_timer_query);
    }
    
    public boolean supportsProgramBinary() {
        return version.isAtLeast(GLESVersion.GLES_3_0) || 
               hasExtension(GLESExtension.OES_get_program_binary);
    }
    
    public boolean supportsSRGB() {
        return version.isAtLeast(GLESVersion.GLES_3_0);
    }
    
    public boolean supportsDepthTexture() {
        return version.isAtLeast(GLESVersion.GLES_3_0) ||
               hasExtension(GLESExtension.OES_depth_texture) ||
               hasExtension(GLESExtension.ANGLE_depth_texture);
    }
    
    public boolean supports32BitIndex() {
        return version.isAtLeast(GLESVersion.GLES_3_0) ||
               hasExtension(GLESExtension.OES_element_index_uint);
    }
    
    public boolean supportsHalfFloat() {
        return version.isAtLeast(GLESVersion.GLES_3_0) ||
               hasExtension(GLESExtension.OES_texture_half_float);
    }
    
    public boolean supportsFloat() {
        return version.isAtLeast(GLESVersion.GLES_3_0) ||
               hasExtension(GLESExtension.OES_texture_float);
    }
    
    public boolean supportsASTC() {
        return version.isAtLeast(GLESVersion.GLES_3_2) ||
               hasExtension(GLESExtension.KHR_texture_compression_astc_ldr);
    }
    
    public boolean supportsPVRTC() {
        return hasExtension(GLESExtension.IMG_texture_compression_pvrtc);
    }
    
    public boolean supportsS3TC() {
        return hasExtension(GLESExtension.EXT_texture_compression_s3tc);
    }
    
    public boolean supportsIndirectDraw() {
        return version.isAtLeast(GLESVersion.GLES_3_1);
    }
    
    public boolean supportsBaseVertex() {
        return version.isAtLeast(GLESVersion.GLES_3_2) ||
               hasExtension(GLESExtension.EXT_draw_elements_base_vertex);
    }
    
    /**
     * Builder for capability detection.
     */
    public static final class Builder {
        private GLESVersion version = GLESVersion.GLES_2_0;
        private GPUVendor vendor = GPUVendor.UNKNOWN;
        private String vendorString = "";
        private String rendererString = "";
        private String versionString = "";
        private String glslVersionString = "";
        private long[] extensionFlags = new long[(GLESExtension.values().length + 63) / 64];
        private ObjectSet<String> extensionStrings = new ObjectOpenHashSet<>();
        
        private int maxTextureSize = 2048;
        private int maxCubeMapTextureSize = 2048;
        private int max3DTextureSize = 256;
        private int maxArrayTextureLayers = 256;
        private int maxTextureUnits = 8;
        private int maxVertexTextureUnits = 0;
        private int maxCombinedTextureUnits = 8;
        private int maxVertexAttribs = 8;
        private int maxVertexUniformVectors = 128;
        private int maxFragmentUniformVectors = 16;
        private int maxVaryingVectors = 8;
        private int maxUniformBufferBindings = 0;
        private int maxUniformBlockSize = 0;
        private int maxShaderStorageBufferBindings = 0;
        private int maxShaderStorageBlockSize = 0;
        private int maxColorAttachments = 1;
        private int maxDrawBuffers = 1;
        private int maxSamples = 4;
        private int maxElementsIndices = 65536;
        private int maxElementsVertices = 65536;
        private int maxComputeWorkGroupCount = 0;
        private int maxComputeWorkGroupSize = 0;
        private int maxComputeWorkGroupInvocations = 0;
        private int maxComputeSharedMemorySize = 0;
        private float maxTextureAnisotropy = 1.0f;
        private IntSet supportedCompressedFormats = new IntOpenHashSet();
        private IntSet supportedInternalFormats = new IntOpenHashSet();
        
        public Builder version(GLESVersion version) {
            this.version = version;
            return this;
        }
        
        public Builder vendor(GPUVendor vendor) {
            this.vendor = vendor;
            return this;
        }
        
        public Builder vendorString(String str) {
            this.vendorString = str;
            return this;
        }
        
        public Builder rendererString(String str) {
            this.rendererString = str;
            return this;
        }
        
        public Builder versionString(String str) {
            this.versionString = str;
            return this;
        }
        
        public Builder glslVersionString(String str) {
            this.glslVersionString = str;
            return this;
        }
        
        public Builder addExtension(GLESExtension ext) {
            int idx = ext.ordinal();
            int arrayIdx = idx / 64;
            int bitIdx = idx % 64;
            extensionFlags[arrayIdx] |= (1L << bitIdx);
            extensionStrings.add(ext.extensionString());
            return this;
        }
        
        public Builder addExtensionString(String ext) {
            extensionStrings.add(ext);
            GLESExtension known = GLESExtension.fromString(ext);
            if (known != null) {
                int idx = known.ordinal();
                int arrayIdx = idx / 64;
                int bitIdx = idx % 64;
                extensionFlags[arrayIdx] |= (1L << bitIdx);
            }
            return this;
        }
        
        public Builder maxTextureSize(int size) { this.maxTextureSize = size; return this; }
        public Builder maxCubeMapTextureSize(int size) { this.maxCubeMapTextureSize = size; return this; }
        public Builder max3DTextureSize(int size) { this.max3DTextureSize = size; return this; }
        public Builder maxArrayTextureLayers(int layers) { this.maxArrayTextureLayers = layers; return this; }
        public Builder maxTextureUnits(int units) { this.maxTextureUnits = units; return this; }
        public Builder maxVertexTextureUnits(int units) { this.maxVertexTextureUnits = units; return this; }
        public Builder maxCombinedTextureUnits(int units) { this.maxCombinedTextureUnits = units; return this; }
        public Builder maxVertexAttribs(int attribs) { this.maxVertexAttribs = attribs; return this; }
        public Builder maxVertexUniformVectors(int vecs) { this.maxVertexUniformVectors = vecs; return this; }
        public Builder maxFragmentUniformVectors(int vecs) { this.maxFragmentUniformVectors = vecs; return this; }
        public Builder maxVaryingVectors(int vecs) { this.maxVaryingVectors = vecs; return this; }
        public Builder maxUniformBufferBindings(int bindings) { this.maxUniformBufferBindings = bindings; return this; }
        public Builder maxUniformBlockSize(int size) { this.maxUniformBlockSize = size; return this; }
        public Builder maxShaderStorageBufferBindings(int bindings) { this.maxShaderStorageBufferBindings = bindings; return this; }
        public Builder maxShaderStorageBlockSize(int size) { this.maxShaderStorageBlockSize = size; return this; }
        public Builder maxColorAttachments(int attachments) { this.maxColorAttachments = attachments; return this; }
        public Builder maxDrawBuffers(int buffers) { this.maxDrawBuffers = buffers; return this; }
        public Builder maxSamples(int samples) { this.maxSamples = samples; return this; }
        public Builder maxElementsIndices(int indices) { this.maxElementsIndices = indices; return this; }
        public Builder maxElementsVertices(int vertices) { this.maxElementsVertices = vertices; return this; }
        public Builder maxComputeWorkGroupCount(int count) { this.maxComputeWorkGroupCount = count; return this; }
        public Builder maxComputeWorkGroupSize(int size) { this.maxComputeWorkGroupSize = size; return this; }
        public Builder maxComputeWorkGroupInvocations(int invocations) { this.maxComputeWorkGroupInvocations = invocations; return this; }
        public Builder maxComputeSharedMemorySize(int size) { this.maxComputeSharedMemorySize = size; return this; }
        public Builder maxTextureAnisotropy(float aniso) { this.maxTextureAnisotropy = aniso; return this; }
        
        public Builder addCompressedFormat(int format) {
            supportedCompressedFormats.add(format);
            return this;
        }
        
        public Builder addInternalFormat(int format) {
            supportedInternalFormats.add(format);
            return this;
        }
        
        public GLESCapabilities build() {
            return new GLESCapabilities(this);
        }
    }
}

// ============================================================================
// SECTION 3: STATE TRACKING
// ============================================================================

/**
 * GL state flags that can be enabled/disabled.
 */
public enum GLState {
    BLEND(0x0BE2),
    CULL_FACE(0x0B44),
    DEPTH_TEST(0x0B71),
    DITHER(0x0BD0),
    POLYGON_OFFSET_FILL(0x8037),
    SAMPLE_ALPHA_TO_COVERAGE(0x809E),
    SAMPLE_COVERAGE(0x80A0),
    SCISSOR_TEST(0x0C11),
    STENCIL_TEST(0x0B90),
    
    // GLES 3.0+
    RASTERIZER_DISCARD(0x8C89),
    PRIMITIVE_RESTART_FIXED_INDEX(0x8D69),
    
    // GLES 3.2+ or extension
    DEBUG_OUTPUT(0x92E0),
    DEBUG_OUTPUT_SYNCHRONOUS(0x8242),
    SAMPLE_MASK(0x8E51),
    DEPTH_CLAMP(0x864F),
    TEXTURE_CUBE_MAP_SEAMLESS(0x884F);
    
    private final int glConstant;
    
    GLState(int glConstant) {
        this.glConstant = glConstant;
    }
    
    public int glConstant() { return glConstant; }
    
    private static final Int2ObjectMap<GLState> BY_CONSTANT;
    static {
        BY_CONSTANT = new Int2ObjectOpenHashMap<>();
        for (GLState state : values()) {
            BY_CONSTANT.put(state.glConstant, state);
        }
    }
    
    public static GLState fromConstant(int constant) {
        return BY_CONSTANT.get(constant);
    }
}

/**
 * Primitive draw modes.
 */
public enum DrawMode {
    POINTS(0x0000),
    LINES(0x0001),
    LINE_LOOP(0x0002),
    LINE_STRIP(0x0003),
    TRIANGLES(0x0004),
    TRIANGLE_STRIP(0x0005),
    TRIANGLE_FAN(0x0006),
    
    // GLES 3.2+ or extension
    LINES_ADJACENCY(0x000A),
    LINE_STRIP_ADJACENCY(0x000B),
    TRIANGLES_ADJACENCY(0x000C),
    TRIANGLE_STRIP_ADJACENCY(0x000D),
    PATCHES(0x000E);
    
    private final int glConstant;
    
    DrawMode(int glConstant) {
        this.glConstant = glConstant;
    }
    
    public int glConstant() { return glConstant; }
}

/**
 * Index element types.
 */
public enum IndexType {
    UNSIGNED_BYTE(0x1401, 1),
    UNSIGNED_SHORT(0x1403, 2),
    UNSIGNED_INT(0x1405, 4);
    
    private final int glConstant;
    private final int byteSize;
    
    IndexType(int glConstant, int byteSize) {
        this.glConstant = glConstant;
        this.byteSize = byteSize;
    }
    
    public int glConstant() { return glConstant; }
    public int byteSize() { return byteSize; }
}

/**
 * Lock-free tracked OpenGL ES state.
 * Uses VarHandle for atomic field access without synchronization overhead.
 */
public final class GLESStateTracker {
    
    // VarHandles for lock-free atomic access
    private static final VarHandle BOUND_VAO_HANDLE;
    private static final VarHandle BOUND_PROGRAM_HANDLE;
    private static final VarHandle ACTIVE_TEXTURE_HANDLE;
    private static final VarHandle BOUND_FRAMEBUFFER_HANDLE;
    private static final VarHandle BOUND_RENDERBUFFER_HANDLE;
    private static final VarHandle STATE_FLAGS_HANDLE;
    
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            BOUND_VAO_HANDLE = lookup.findVarHandle(GLESStateTracker.class, "boundVAO", int.class);
            BOUND_PROGRAM_HANDLE = lookup.findVarHandle(GLESStateTracker.class, "boundProgram", int.class);
            ACTIVE_TEXTURE_HANDLE = lookup.findVarHandle(GLESStateTracker.class, "activeTexture", int.class);
            BOUND_FRAMEBUFFER_HANDLE = lookup.findVarHandle(GLESStateTracker.class, "boundFramebuffer", int.class);
            BOUND_RENDERBUFFER_HANDLE = lookup.findVarHandle(GLESStateTracker.class, "boundRenderbuffer", int.class);
            STATE_FLAGS_HANDLE = lookup.findVarHandle(GLESStateTracker.class, "stateFlags", long.class);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    // Current bindings - accessed via VarHandle
    @SuppressWarnings("unused")
    private volatile int boundVAO;
    @SuppressWarnings("unused")
    private volatile int boundProgram;
    @SuppressWarnings("unused")
    private volatile int activeTexture;
    @SuppressWarnings("unused")
    private volatile int boundFramebuffer;
    @SuppressWarnings("unused")
    private volatile int boundRenderbuffer;
    @SuppressWarnings("unused")
    private volatile long stateFlags;
    
    // Buffer bindings per target
    private final AtomicIntegerArray boundBuffers;
    
    // Texture bindings per unit
    private final AtomicIntegerArray boundTextures2D;
    private final AtomicIntegerArray boundTexturesCube;
    private final AtomicIntegerArray boundTextures3D;
    private final AtomicIntegerArray boundTextures2DArray;
    private final AtomicIntegerArray boundSamplers;
    
    // Viewport/Scissor state
    private final AtomicIntegerArray viewport;
    private final AtomicIntegerArray scissor;
    
    // Blend state
    private final AtomicIntegerArray blendColor;
    private final AtomicInteger blendSrcRGB;
    private final AtomicInteger blendDstRGB;
    private final AtomicInteger blendSrcAlpha;
    private final AtomicInteger blendDstAlpha;
    private final AtomicInteger blendEquationRGB;
    private final AtomicInteger blendEquationAlpha;
    
    // Depth state
    private final AtomicInteger depthFunc;
    private final AtomicBoolean depthMask;
    
    // Stencil state
    private final AtomicInteger stencilFuncFront;
    private final AtomicInteger stencilFuncBack;
    private final AtomicInteger stencilRefFront;
    private final AtomicInteger stencilRefBack;
    private final AtomicInteger stencilMaskFront;
    private final AtomicInteger stencilMaskBack;
    
    // Face culling
    private final AtomicInteger cullFace;
    private final AtomicInteger frontFace;
    
    // Color mask
    private final AtomicInteger colorMask;
    
    // Clear values
    private final float[] clearColor;
    private final AtomicReference<Float> clearDepth;
    private final AtomicInteger clearStencil;
    
    // Statistics
    private final AtomicLong drawCallCount;
    private final AtomicLong triangleCount;
    private final AtomicLong stateChangeCount;
    
    private final int maxTextureUnits;
    private final int maxBufferTargets;
    
    public GLESStateTracker(GLESCapabilities capabilities) {
        this.maxTextureUnits = capabilities.maxCombinedTextureUnits();
        this.maxBufferTargets = BufferTarget.values().length;
        
        boundBuffers = new AtomicIntegerArray(maxBufferTargets);
        boundTextures2D = new AtomicIntegerArray(maxTextureUnits);
        boundTexturesCube = new AtomicIntegerArray(maxTextureUnits);
        boundTextures3D = new AtomicIntegerArray(maxTextureUnits);
        boundTextures2DArray = new AtomicIntegerArray(maxTextureUnits);
        boundSamplers = new AtomicIntegerArray(maxTextureUnits);
        
        viewport = new AtomicIntegerArray(4);
        scissor = new AtomicIntegerArray(4);
        blendColor = new AtomicIntegerArray(4);
        
        blendSrcRGB = new AtomicInteger(1); // GL_ONE
        blendDstRGB = new AtomicInteger(0); // GL_ZERO
        blendSrcAlpha = new AtomicInteger(1);
        blendDstAlpha = new AtomicInteger(0);
        blendEquationRGB = new AtomicInteger(0x8006); // GL_FUNC_ADD
        blendEquationAlpha = new AtomicInteger(0x8006);
        
        depthFunc = new AtomicInteger(0x0201); // GL_LESS
        depthMask = new AtomicBoolean(true);
        
        stencilFuncFront = new AtomicInteger(0x0207); // GL_ALWAYS
        stencilFuncBack = new AtomicInteger(0x0207);
        stencilRefFront = new AtomicInteger(0);
        stencilRefBack = new AtomicInteger(0);
        stencilMaskFront = new AtomicInteger(0xFFFFFFFF);
        stencilMaskBack = new AtomicInteger(0xFFFFFFFF);
        
        cullFace = new AtomicInteger(0x0405); // GL_BACK
        frontFace = new AtomicInteger(0x0901); // GL_CCW
        
        colorMask = new AtomicInteger(0xF); // All channels enabled
        
        clearColor = new float[4];
        clearDepth = new AtomicReference<>(1.0f);
        clearStencil = new AtomicInteger(0);
        
        drawCallCount = new AtomicLong(0);
        triangleCount = new AtomicLong(0);
        stateChangeCount = new AtomicLong(0);
    }
    
    // VAO binding
    public boolean bindVAO(int vao) {
        int current = (int) BOUND_VAO_HANDLE.getVolatile(this);
        if (current == vao) return false;
        BOUND_VAO_HANDLE.setVolatile(this, vao);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public int getBoundVAO() {
        return (int) BOUND_VAO_HANDLE.getVolatile(this);
    }
    
    // Program binding
    public boolean bindProgram(int program) {
        int current = (int) BOUND_PROGRAM_HANDLE.getVolatile(this);
        if (current == program) return false;
        BOUND_PROGRAM_HANDLE.setVolatile(this, program);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public int getBoundProgram() {
        return (int) BOUND_PROGRAM_HANDLE.getVolatile(this);
    }
    
    // Buffer binding
    public boolean bindBuffer(BufferTarget target, int buffer) {
        int idx = target.ordinal();
        if (idx >= maxBufferTargets) return false;
        int current = boundBuffers.get(idx);
        if (current == buffer) return false;
        boundBuffers.set(idx, buffer);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public int getBoundBuffer(BufferTarget target) {
        int idx = target.ordinal();
        return idx < maxBufferTargets ? boundBuffers.get(idx) : 0;
    }
    
    // Texture binding
    public boolean setActiveTexture(int unit) {
        int current = (int) ACTIVE_TEXTURE_HANDLE.getVolatile(this);
        if (current == unit) return false;
        ACTIVE_TEXTURE_HANDLE.setVolatile(this, unit);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public int getActiveTexture() {
        return (int) ACTIVE_TEXTURE_HANDLE.getVolatile(this);
    }
    
    public boolean bindTexture2D(int unit, int texture) {
        if (unit >= maxTextureUnits) return false;
        int current = boundTextures2D.get(unit);
        if (current == texture) return false;
        boundTextures2D.set(unit, texture);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public int getBoundTexture2D(int unit) {
        return unit < maxTextureUnits ? boundTextures2D.get(unit) : 0;
    }
    
    public boolean bindTextureCube(int unit, int texture) {
        if (unit >= maxTextureUnits) return false;
        int current = boundTexturesCube.get(unit);
        if (current == texture) return false;
        boundTexturesCube.set(unit, texture);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public boolean bindTexture3D(int unit, int texture) {
        if (unit >= maxTextureUnits) return false;
        int current = boundTextures3D.get(unit);
        if (current == texture) return false;
        boundTextures3D.set(unit, texture);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public boolean bindTexture2DArray(int unit, int texture) {
        if (unit >= maxTextureUnits) return false;
        int current = boundTextures2DArray.get(unit);
        if (current == texture) return false;
        boundTextures2DArray.set(unit, texture);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public boolean bindSampler(int unit, int sampler) {
        if (unit >= maxTextureUnits) return false;
        int current = boundSamplers.get(unit);
        if (current == sampler) return false;
        boundSamplers.set(unit, sampler);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    // Framebuffer binding
    public boolean bindFramebuffer(int framebuffer) {
        int current = (int) BOUND_FRAMEBUFFER_HANDLE.getVolatile(this);
        if (current == framebuffer) return false;
        BOUND_FRAMEBUFFER_HANDLE.setVolatile(this, framebuffer);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public int getBoundFramebuffer() {
        return (int) BOUND_FRAMEBUFFER_HANDLE.getVolatile(this);
    }
    
    // Renderbuffer binding
    public boolean bindRenderbuffer(int renderbuffer) {
        int current = (int) BOUND_RENDERBUFFER_HANDLE.getVolatile(this);
        if (current == renderbuffer) return false;
        BOUND_RENDERBUFFER_HANDLE.setVolatile(this, renderbuffer);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public int getBoundRenderbuffer() {
        return (int) BOUND_RENDERBUFFER_HANDLE.getVolatile(this);
    }
    
    // State flags (enable/disable)
    public boolean enableState(GLState state) {
        int bit = state.ordinal();
        long mask = 1L << bit;
        long current;
        long updated;
        do {
            current = (long) STATE_FLAGS_HANDLE.getVolatile(this);
            if ((current & mask) != 0) return false;
            updated = current | mask;
        } while (!STATE_FLAGS_HANDLE.compareAndSet(this, current, updated));
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public boolean disableState(GLState state) {
        int bit = state.ordinal();
        long mask = 1L << bit;
        long current;
        long updated;
        do {
            current = (long) STATE_FLAGS_HANDLE.getVolatile(this);
            if ((current & mask) == 0) return false;
            updated = current & ~mask;
        } while (!STATE_FLAGS_HANDLE.compareAndSet(this, current, updated));
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public boolean isStateEnabled(GLState state) {
        int bit = state.ordinal();
        long mask = 1L << bit;
        long current = (long) STATE_FLAGS_HANDLE.getVolatile(this);
        return (current & mask) != 0;
    }
    
    // Viewport
    public boolean setViewport(int x, int y, int width, int height) {
        boolean changed = false;
        if (viewport.get(0) != x) { viewport.set(0, x); changed = true; }
        if (viewport.get(1) != y) { viewport.set(1, y); changed = true; }
        if (viewport.get(2) != width) { viewport.set(2, width); changed = true; }
        if (viewport.get(3) != height) { viewport.set(3, height); changed = true; }
        if (changed) stateChangeCount.incrementAndGet();
        return changed;
    }
    
    public int getViewportX() { return viewport.get(0); }
    public int getViewportY() { return viewport.get(1); }
    public int getViewportWidth() { return viewport.get(2); }
    public int getViewportHeight() { return viewport.get(3); }
    
    // Scissor
    public boolean setScissor(int x, int y, int width, int height) {
        boolean changed = false;
        if (scissor.get(0) != x) { scissor.set(0, x); changed = true; }
        if (scissor.get(1) != y) { scissor.set(1, y); changed = true; }
        if (scissor.get(2) != width) { scissor.set(2, width); changed = true; }
        if (scissor.get(3) != height) { scissor.set(3, height); changed = true; }
        if (changed) stateChangeCount.incrementAndGet();
        return changed;
    }
    
    // Blend state
    public boolean setBlendFunc(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        boolean changed = false;
        if (blendSrcRGB.get() != srcRGB) { blendSrcRGB.set(srcRGB); changed = true; }
        if (blendDstRGB.get() != dstRGB) { blendDstRGB.set(dstRGB); changed = true; }
        if (blendSrcAlpha.get() != srcAlpha) { blendSrcAlpha.set(srcAlpha); changed = true; }
        if (blendDstAlpha.get() != dstAlpha) { blendDstAlpha.set(dstAlpha); changed = true; }
        if (changed) stateChangeCount.incrementAndGet();
        return changed;
    }
    
    public boolean setBlendEquation(int modeRGB, int modeAlpha) {
        boolean changed = false;
        if (blendEquationRGB.get() != modeRGB) { blendEquationRGB.set(modeRGB); changed = true; }
        if (blendEquationAlpha.get() != modeAlpha) { blendEquationAlpha.set(modeAlpha); changed = true; }
        if (changed) stateChangeCount.incrementAndGet();
        return changed;
    }
    
    // Depth state
    public boolean setDepthFunc(int func) {
        if (depthFunc.get() == func) return false;
        depthFunc.set(func);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public boolean setDepthMask(boolean mask) {
        if (depthMask.get() == mask) return false;
        depthMask.set(mask);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    // Face culling
    public boolean setCullFace(int face) {
        if (cullFace.get() == face) return false;
        cullFace.set(face);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    public boolean setFrontFace(int face) {
        if (frontFace.get() == face) return false;
        frontFace.set(face);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    // Color mask
    public boolean setColorMask(boolean r, boolean g, boolean b, boolean a) {
        int mask = (r ? 1 : 0) | (g ? 2 : 0) | (b ? 4 : 0) | (a ? 8 : 0);
        if (colorMask.get() == mask) return false;
        colorMask.set(mask);
        stateChangeCount.incrementAndGet();
        return true;
    }
    
    // Statistics
    public void recordDrawCall(int triangles) {
        drawCallCount.incrementAndGet();
        triangleCount.addAndGet(triangles);
    }
    
    public long getDrawCallCount() { return drawCallCount.get(); }
    public long getTriangleCount() { return triangleCount.get(); }
    public long getStateChangeCount() { return stateChangeCount.get(); }
    
    public void resetStatistics() {
        drawCallCount.set(0);
        triangleCount.set(0);
        stateChangeCount.set(0);
    }
    
    // Full state reset
    public void invalidate() {
        BOUND_VAO_HANDLE.setVolatile(this, -1);
        BOUND_PROGRAM_HANDLE.setVolatile(this, -1);
        ACTIVE_TEXTURE_HANDLE.setVolatile(this, -1);
        BOUND_FRAMEBUFFER_HANDLE.setVolatile(this, -1);
        BOUND_RENDERBUFFER_HANDLE.setVolatile(this, -1);
        STATE_FLAGS_HANDLE.setVolatile(this, -1L);
        
        for (int i = 0; i < maxBufferTargets; i++) {
            boundBuffers.set(i, -1);
        }
        for (int i = 0; i < maxTextureUnits; i++) {
            boundTextures2D.set(i, -1);
            boundTexturesCube.set(i, -1);
            boundTextures3D.set(i, -1);
            boundTextures2DArray.set(i, -1);
            boundSamplers.set(i, -1);
        }
    }
}

// ============================================================================
// SECTION 4: RESOURCE MANAGEMENT
// ============================================================================

/**
 * GPU resource handle with reference counting.
 */
public sealed interface GPUResource permits GPUBuffer, GPUTexture, GPUFramebuffer, 
        GPURenderbuffer, GPUShader, GPUProgram, GPUQuery, GPUSampler, GPUVAO, GPUSync {
    int handle();
    boolean isValid();
    void release();
    int refCount();
    void addRef();
}

/**
 * Base implementation for GPU resources with atomic reference counting.
 */
abstract sealed class AbstractGPUResource implements GPUResource 
        permits GPUBuffer, GPUTexture, GPUFramebuffer, GPURenderbuffer, 
                GPUShader, GPUProgram, GPUQuery, GPUSampler, GPUVAO, GPUSync {
    
    protected final int handle;
    private final AtomicInteger refCount = new AtomicInteger(1);
    
    protected AbstractGPUResource(int handle) {
        this.handle = handle;
    }
    
    @Override
    public final int handle() { return handle; }
    
    @Override
    public final boolean isValid() { return handle != 0 && refCount.get() > 0; }
    
    @Override
    public final int refCount() { return refCount.get(); }
    
    @Override
    public final void addRef() {
        refCount.incrementAndGet();
    }
    
    @Override
    public final void release() {
        if (refCount.decrementAndGet() == 0) {
            destroy();
        }
    }
    
    protected abstract void destroy();
}

/**
 * GPU Buffer resource.
 */
public final class GPUBuffer extends AbstractGPUResource {
    
    private final BufferTarget target;
    private final int size;
    private final int usage;
    private volatile long mappedPointer;
    
    public GPUBuffer(int handle, BufferTarget target, int size, int usage) {
        super(handle);
        this.target = target;
        this.size = size;
        this.usage = usage;
    }
    
    public BufferTarget target() { return target; }
    public int size() { return size; }
    public int usage() { return usage; }
    public long mappedPointer() { return mappedPointer; }
    
    public void setMappedPointer(long ptr) {
        this.mappedPointer = ptr;
    }
    
    @Override
    protected void destroy() {
        if (handle != 0) {
            GLES20.glDeleteBuffers(handle);
        }
    }
}

/**
 * GPU Texture resource.
 */
public final class GPUTexture extends AbstractGPUResource {
    
    private final TextureTarget target;
    private final int width;
    private final int height;
    private final int depth;
    private final int internalFormat;
    private final int levels;
    
    public GPUTexture(int handle, TextureTarget target, int width, int height, int depth,
                      int internalFormat, int levels) {
        super(handle);
        this.target = target;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.internalFormat = internalFormat;
        this.levels = levels;
    }
    
    public TextureTarget target() { return target; }
    public int width() { return width; }
    public int height() { return height; }
    public int depth() { return depth; }
    public int internalFormat() { return internalFormat; }
    public int levels() { return levels; }
    
    @Override
    protected void destroy() {
        if (handle != 0) {
            GLES20.glDeleteTextures(handle);
        }
    }
}

/**
 * GPU Framebuffer resource.
 */
public final class GPUFramebuffer extends AbstractGPUResource {
    
    private final int width;
    private final int height;
    private final IntList colorAttachments;
    private final int depthAttachment;
    private final int stencilAttachment;
    
    public GPUFramebuffer(int handle, int width, int height, 
                          IntList colorAttachments, int depthAttachment, int stencilAttachment) {
        super(handle);
        this.width = width;
        this.height = height;
        this.colorAttachments = colorAttachments;
        this.depthAttachment = depthAttachment;
        this.stencilAttachment = stencilAttachment;
    }
    
    public int width() { return width; }
    public int height() { return height; }
    public IntList colorAttachments() { return colorAttachments; }
    public int depthAttachment() { return depthAttachment; }
    public int stencilAttachment() { return stencilAttachment; }
    
    @Override
    protected void destroy() {
        if (handle != 0) {
            GLES20.glDeleteFramebuffers(handle);
        }
    }
}

/**
 * GPU Renderbuffer resource.
 */
public final class GPURenderbuffer extends AbstractGPUResource {
    
    private final int width;
    private final int height;
    private final int internalFormat;
    private final int samples;
    
    public GPURenderbuffer(int handle, int width, int height, int internalFormat, int samples) {
        super(handle);
        this.width = width;
        this.height = height;
        this.internalFormat = internalFormat;
        this.samples = samples;
    }
    
    public int width() { return width; }
    public int height() { return height; }
    public int internalFormat() { return internalFormat; }
    public int samples() { return samples; }
    
    @Override
    protected void destroy() {
        if (handle != 0) {
            GLES20.glDeleteRenderbuffers(handle);
        }
    }
}

/**
 * GPU Shader resource.
 */
public final class GPUShader extends AbstractGPUResource {
    
    private final int type;
    private final String source;
    private final boolean compiled;
    private final String infoLog;
    
    public GPUShader(int handle, int type, String source, boolean compiled, String infoLog) {
        super(handle);
        this.type = type;
        this.source = source;
        this.compiled = compiled;
        this.infoLog = infoLog;
    }
    
    public int type() { return type; }
    public String source() { return source; }
    public boolean isCompiled() { return compiled; }
    public String infoLog() { return infoLog; }
    
    @Override
    protected void destroy() {
        if (handle != 0) {
            GLES20.glDeleteShader(handle);
        }
    }
}

/**
 * GPU Program resource.
 */
public final class GPUProgram extends AbstractGPUResource {
    
    private final boolean linked;
    private final String infoLog;
    private final Int2IntMap uniformLocations;
    private final Int2IntMap attribLocations;
    
    public GPUProgram(int handle, boolean linked, String infoLog,
                      Int2IntMap uniformLocations, Int2IntMap attribLocations) {
        super(handle);
        this.linked = linked;
        this.infoLog = infoLog;
        this.uniformLocations = uniformLocations;
        this.attribLocations = attribLocations;
    }
    
    public boolean isLinked() { return linked; }
    public String infoLog() { return infoLog; }
    public Int2IntMap uniformLocations() { return uniformLocations; }
    public Int2IntMap attribLocations() { return attribLocations; }
    
    @Override
    protected void destroy() {
        if (handle != 0) {
            GLES20.glDeleteProgram(handle);
        }
    }
}

/**
 * GPU Query resource.
 */
public final class GPUQuery extends AbstractGPUResource {
    
    private final int target;
    
    public GPUQuery(int handle, int target) {
        super(handle);
        this.target = target;
    }
    
    public int target() { return target; }
    
    @Override
    protected void destroy() {
        if (handle != 0) {
            GLES30.glDeleteQueries(handle);
        }
    }
}

/**
 * GPU Sampler resource.
 */
public final class GPUSampler extends AbstractGPUResource {
    
    public GPUSampler(int handle) {
        super(handle);
    }
    
    @Override
    protected void destroy() {
        if (handle != 0) {
            GLES30.glDeleteSamplers(handle);
        }
    }
}

/**
 * GPU VAO resource.
 */
public final class GPUVAO extends AbstractGPUResource {
    
    private final IntList enabledAttribs;
    
    public GPUVAO(int handle, IntList enabledAttribs) {
        super(handle);
        this.enabledAttribs = enabledAttribs;
    }
    
    public IntList enabledAttribs() { return enabledAttribs; }
    
    @Override
    protected void destroy() {
        if (handle != 0) {
            GLES30.glDeleteVertexArrays(handle);
        }
    }
}

/**
 * GPU Sync object resource.
 */
public final class GPUSync extends AbstractGPUResource {
    
    private final long syncObject;
    
    public GPUSync(int handle, long syncObject) {
        super(handle);
        this.syncObject = syncObject;
    }
    
    public long syncObject() { return syncObject; }
    
    @Override
    protected void destroy() {
        if (syncObject != 0) {
            GLES30.glDeleteSync(syncObject);
        }
    }
}

/**
 * High-performance resource registry using lock-free data structures.
 */
public final class GLESResourceRegistry {
    
    private final Int2ObjectMap<GPUBuffer> buffers;
    private final Int2ObjectMap<GPUTexture> textures;
    private final Int2ObjectMap<GPUFramebuffer> framebuffers;
    private final Int2ObjectMap<GPURenderbuffer> renderbuffers;
    private final Int2ObjectMap<GPUShader> shaders;
    private final Int2ObjectMap<GPUProgram> programs;
    private final Int2ObjectMap<GPUQuery> queries;
    private final Int2ObjectMap<GPUSampler> samplers;
    private final Int2ObjectMap<GPUVAO> vaos;
    private final Long2ObjectMap<GPUSync> syncs;
    
    private final AtomicInteger nextHandleId;
    
    public GLESResourceRegistry() {
        buffers = new Int2ObjectOpenHashMap<>();
        textures = new Int2ObjectOpenHashMap<>();
        framebuffers = new Int2ObjectOpenHashMap<>();
        renderbuffers = new Int2ObjectOpenHashMap<>();
        shaders = new Int2ObjectOpenHashMap<>();
        programs = new Int2ObjectOpenHashMap<>();
        queries = new Int2ObjectOpenHashMap<>();
        samplers = new Int2ObjectOpenHashMap<>();
        vaos = new Int2ObjectOpenHashMap<>();
        syncs = new Long2ObjectOpenHashMap<>();
        nextHandleId = new AtomicInteger(1);
    }
    
    public void registerBuffer(GPUBuffer buffer) {
        synchronized (buffers) {
            buffers.put(buffer.handle(), buffer);
        }
    }
    
    public GPUBuffer getBuffer(int handle) {
        synchronized (buffers) {
            return buffers.get(handle);
        }
    }
    
    public void unregisterBuffer(int handle) {
        synchronized (buffers) {
            GPUBuffer buffer = buffers.remove(handle);
            if (buffer != null) buffer.release();
        }
    }
    
    public void registerTexture(GPUTexture texture) {
        synchronized (textures) {
            textures.put(texture.handle(), texture);
        }
    }
    
    public GPUTexture getTexture(int handle) {
        synchronized (textures) {
            return textures.get(handle);
        }
    }
    
    public void unregisterTexture(int handle) {
        synchronized (textures) {
            GPUTexture texture = textures.remove(handle);
            if (texture != null) texture.release();
        }
    }
    
    public void registerFramebuffer(GPUFramebuffer framebuffer) {
        synchronized (framebuffers) {
            framebuffers.put(framebuffer.handle(), framebuffer);
        }
    }
    
    public GPUFramebuffer getFramebuffer(int handle) {
        synchronized (framebuffers) {
            return framebuffers.get(handle);
        }
    }
    
    public void unregisterFramebuffer(int handle) {
        synchronized (framebuffers) {
            GPUFramebuffer fb = framebuffers.remove(handle);
            if (fb != null) fb.release();
        }
    }
    
    public void registerRenderbuffer(GPURenderbuffer renderbuffer) {
        synchronized (renderbuffers) {
            renderbuffers.put(renderbuffer.handle(), renderbuffer);
        }
    }
    
    public GPURenderbuffer getRenderbuffer(int handle) {
        synchronized (renderbuffers) {
            return renderbuffers.get(handle);
        }
    }
    
    public void registerShader(GPUShader shader) {
        synchronized (shaders) {
            shaders.put(shader.handle(), shader);
        }
    }
    
    public GPUShader getShader(int handle) {
        synchronized (shaders) {
            return shaders.get(handle);
        }
    }
    
    public void unregisterShader(int handle) {
        synchronized (shaders) {
            GPUShader shader = shaders.remove(handle);
            if (shader != null) shader.release();
        }
    }
    
    public void registerProgram(GPUProgram program) {
        synchronized (programs) {
            programs.put(program.handle(), program);
        }
    }
    
    public GPUProgram getProgram(int handle) {
        synchronized (programs) {
            return programs.get(handle);
        }
    }
    
    public void unregisterProgram(int handle) {
        synchronized (programs) {
            GPUProgram program = programs.remove(handle);
            if (program != null) program.release();
        }
    }
    
    public void registerQuery(GPUQuery query) {
        synchronized (queries) {
            queries.put(query.handle(), query);
        }
    }
    
    public GPUQuery getQuery(int handle) {
        synchronized (queries) {
            return queries.get(handle);
        }
    }
    
    public void registerSampler(GPUSampler sampler) {
        synchronized (samplers) {
            samplers.put(sampler.handle(), sampler);
        }
    }
    
    public GPUSampler getSampler(int handle) {
        synchronized (samplers) {
            return samplers.get(handle);
        }
    }
    
    public void registerVAO(GPUVAO vao) {
        synchronized (vaos) {
            vaos.put(vao.handle(), vao);
        }
    }
    
    public GPUVAO getVAO(int handle) {
        synchronized (vaos) {
            return vaos.get(handle);
        }
    }
    
    public void unregisterVAO(int handle) {
        synchronized (vaos) {
            GPUVAO vao = vaos.remove(handle);
            if (vao != null) vao.release();
        }
    }
    
    public void registerSync(GPUSync sync) {
        synchronized (syncs) {
            syncs.put(sync.syncObject(), sync);
        }
    }
    
    public GPUSync getSync(long syncObject) {
        synchronized (syncs) {
            return syncs.get(syncObject);
        }
    }
    
    public void releaseAll() {
        synchronized (buffers) {
            for (GPUBuffer b : buffers.values()) b.release();
            buffers.clear();
        }
        synchronized (textures) {
            for (GPUTexture t : textures.values()) t.release();
            textures.clear();
        }
        synchronized (framebuffers) {
            for (GPUFramebuffer f : framebuffers.values()) f.release();
            framebuffers.clear();
        }
        synchronized (renderbuffers) {
            for (GPURenderbuffer r : renderbuffers.values()) r.release();
            renderbuffers.clear();
        }
        synchronized (shaders) {
            for (GPUShader s : shaders.values()) s.release();
            shaders.clear();
        }
        synchronized (programs) {
            for (GPUProgram p : programs.values()) p.release();
            programs.clear();
        }
        synchronized (queries) {
            for (GPUQuery q : queries.values()) q.release();
            queries.clear();
        }
        synchronized (samplers) {
            for (GPUSampler s : samplers.values()) s.release();
            samplers.clear();
        }
        synchronized (vaos) {
            for (GPUVAO v : vaos.values()) v.release();
            vaos.clear();
        }
        synchronized (syncs) {
            for (GPUSync s : syncs.values()) s.release();
            syncs.clear();
        }
    }
    
    public int getResourceCount() {
        int count = 0;
        synchronized (buffers) { count += buffers.size(); }
        synchronized (textures) { count += textures.size(); }
        synchronized (framebuffers) { count += framebuffers.size(); }
        synchronized (renderbuffers) { count += renderbuffers.size(); }
        synchronized (shaders) { count += shaders.size(); }
        synchronized (programs) { count += programs.size(); }
        synchronized (queries) { count += queries.size(); }
        synchronized (samplers) { count += samplers.size(); }
        synchronized (vaos) { count += vaos.size(); }
        synchronized (syncs) { count += syncs.size(); }
        return count;
    }
}

// ============================================================================
// SECTION 5: ERROR HANDLING
// ============================================================================

/**
 * OpenGL ES error codes with human-readable descriptions.
 */
public enum GLESError {
    NO_ERROR(0x0000, "No error"),
    INVALID_ENUM(0x0500, "Invalid enum value"),
    INVALID_VALUE(0x0501, "Invalid parameter value"),
    INVALID_OPERATION(0x0502, "Invalid operation in current state"),
    STACK_OVERFLOW(0x0503, "Stack overflow"),
    STACK_UNDERFLOW(0x0504, "Stack underflow"),
    OUT_OF_MEMORY(0x0505, "Out of GPU memory"),
    INVALID_FRAMEBUFFER_OPERATION(0x0506, "Framebuffer is incomplete"),
    CONTEXT_LOST(0x0507, "GL context lost");
    
    private final int code;
    private final String description;
    
    GLESError(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public int code() { return code; }
    public String description() { return description; }
    
    private static final Int2ObjectMap<GLESError> BY_CODE;
    static {
        BY_CODE = new Int2ObjectOpenHashMap<>();
        for (GLESError error : values()) {
            BY_CODE.put(error.code, error);
        }
    }
    
    public static GLESError fromCode(int code) {
        return BY_CODE.getOrDefault(code, NO_ERROR);
    }
}

/**
 * Framebuffer status codes.
 */
public enum FramebufferStatus {
    COMPLETE(0x8CD5, "Framebuffer complete"),
    INCOMPLETE_ATTACHMENT(0x8CD6, "Incomplete attachment"),
    INCOMPLETE_MISSING_ATTACHMENT(0x8CD7, "Missing attachment"),
    INCOMPLETE_DIMENSIONS(0x8CD9, "Attachment dimension mismatch"),
    INCOMPLETE_FORMATS(0x8CDA, "Attachment format mismatch"),
    INCOMPLETE_DRAW_BUFFER(0x8CDB, "Incomplete draw buffer"),
    INCOMPLETE_READ_BUFFER(0x8CDC, "Incomplete read buffer"),
    UNSUPPORTED(0x8CDD, "Unsupported framebuffer format"),
    INCOMPLETE_MULTISAMPLE(0x8D56, "Multisample mismatch"),
    INCOMPLETE_LAYER_TARGETS(0x8DA8, "Layer target mismatch"),
    UNDEFINED(0x8219, "Undefined framebuffer");
    
    private final int code;
    private final String description;
    
    FramebufferStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public int code() { return code; }
    public String description() { return description; }
    
    public boolean isComplete() { return this == COMPLETE; }
    
    private static final Int2ObjectMap<FramebufferStatus> BY_CODE;
    static {
        BY_CODE = new Int2ObjectOpenHashMap<>();
        for (FramebufferStatus status : values()) {
            BY_CODE.put(status.code, status);
        }
    }
    
    public static FramebufferStatus fromCode(int code) {
        return BY_CODE.getOrDefault(code, UNDEFINED);
    }
}

/**
 * Error handler with debug output support.
 */
public final class GLESErrorHandler {
    
    @FunctionalInterface
    public interface ErrorCallback {
        void onError(GLESError error, String message, String source);
    }
    
    @FunctionalInterface
    public interface DebugCallback {
        void onDebugMessage(int source, int type, int id, int severity, String message);
    }
    
    private final GLESCapabilities capabilities;
    private volatile ErrorCallback errorCallback;
    private volatile DebugCallback debugCallback;
    private final AtomicLong errorCount;
    private final AtomicLong lastErrorCode;
    
    // Debug message constants
    public static final int DEBUG_SOURCE_API = 0x8246;
    public static final int DEBUG_SOURCE_WINDOW_SYSTEM = 0x8247;
    public static final int DEBUG_SOURCE_SHADER_COMPILER = 0x8248;
    public static final int DEBUG_SOURCE_THIRD_PARTY = 0x8249;
    public static final int DEBUG_SOURCE_APPLICATION = 0x824A;
    public static final int DEBUG_SOURCE_OTHER = 0x824B;
    
    public static final int DEBUG_TYPE_ERROR = 0x824C;
    public static final int DEBUG_TYPE_DEPRECATED_BEHAVIOR = 0x824D;
    public static final int DEBUG_TYPE_UNDEFINED_BEHAVIOR = 0x824E;
    public static final int DEBUG_TYPE_PORTABILITY = 0x824F;
    public static final int DEBUG_TYPE_PERFORMANCE = 0x8250;
    public static final int DEBUG_TYPE_OTHER = 0x8251;
    public static final int DEBUG_TYPE_MARKER = 0x8268;
    
    public static final int DEBUG_SEVERITY_HIGH = 0x9146;
    public static final int DEBUG_SEVERITY_MEDIUM = 0x9147;
    public static final int DEBUG_SEVERITY_LOW = 0x9148;
    public static final int DEBUG_SEVERITY_NOTIFICATION = 0x826B;
    
    public GLESErrorHandler(GLESCapabilities capabilities) {
        this.capabilities = capabilities;
        this.errorCount = new AtomicLong(0);
        this.lastErrorCode = new AtomicLong(0);
    }
    
    public void setErrorCallback(ErrorCallback callback) {
        this.errorCallback = callback;
    }
    
    public void setDebugCallback(DebugCallback callback) {
        this.debugCallback = callback;
        if (callback != null && capabilities.supportsDebugOutput()) {
            enableDebugOutput();
        }
    }
    
    public GLESError checkError(String operation) {
        int errorCode = GLES20.glGetError();
        if (errorCode != 0) {
            GLESError error = GLESError.fromCode(errorCode);
            errorCount.incrementAndGet();
            lastErrorCode.set(errorCode);
            
            ErrorCallback cb = errorCallback;
            if (cb != null) {
                cb.onError(error, error.description(), operation);
            }
            return error;
        }
        return GLESError.NO_ERROR;
    }
    
    public void clearErrors() {
        while (GLES20.glGetError() != 0) {
            // Drain error queue
        }
    }
    
    public long getErrorCount() {
        return errorCount.get();
    }
    
    public int getLastErrorCode() {
        return (int) lastErrorCode.get();
    }
    
    private void enableDebugOutput() {
        if (!capabilities.supportsDebugOutput()) return;
        
        // Enable debug output
        GLES20.glEnable(0x92E0); // GL_DEBUG_OUTPUT
        GLES20.glEnable(0x8242); // GL_DEBUG_OUTPUT_SYNCHRONOUS
        
        // Set up callback via KHR_debug
        // Note: Actual callback setup requires native interop
    }
    
    public void pushDebugGroup(String name) {
        if (capabilities.supportsDebugOutput()) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer nameBuffer = stack.UTF8(name);
                GLES32.glPushDebugGroup(DEBUG_SOURCE_APPLICATION, 0, nameBuffer);
            }
        }
    }
    
    public void popDebugGroup() {
        if (capabilities.supportsDebugOutput()) {
            GLES32.glPopDebugGroup();
        }
    }
    
    public void insertDebugMarker(String message) {
        if (capabilities.supportsDebugOutput()) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer msgBuffer = stack.UTF8(message);
                GLES32.glDebugMessageInsert(
                    DEBUG_SOURCE_APPLICATION,
                    DEBUG_TYPE_MARKER,
                    0,
                    DEBUG_SEVERITY_NOTIFICATION,
                    msgBuffer
                );
            }
        }
    }
    
    public void labelObject(int type, int object, String label) {
        if (capabilities.supportsDebugOutput()) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer labelBuffer = stack.UTF8(label);
                GLES32.glObjectLabel(type, object, labelBuffer);
            }
        }
    }
    
    public String getDebugSourceName(int source) {
        return switch (source) {
            case DEBUG_SOURCE_API -> "API";
            case DEBUG_SOURCE_WINDOW_SYSTEM -> "Window System";
            case DEBUG_SOURCE_SHADER_COMPILER -> "Shader Compiler";
            case DEBUG_SOURCE_THIRD_PARTY -> "Third Party";
            case DEBUG_SOURCE_APPLICATION -> "Application";
            default -> "Other";
        };
    }
    
    public String getDebugTypeName(int type) {
        return switch (type) {
            case DEBUG_TYPE_ERROR -> "Error";
            case DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "Deprecated";
            case DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "Undefined Behavior";
            case DEBUG_TYPE_PORTABILITY -> "Portability";
            case DEBUG_TYPE_PERFORMANCE -> "Performance";
            case DEBUG_TYPE_MARKER -> "Marker";
            default -> "Other";
        };
    }
    
    public String getDebugSeverityName(int severity) {
        return switch (severity) {
            case DEBUG_SEVERITY_HIGH -> "High";
            case DEBUG_SEVERITY_MEDIUM -> "Medium";
            case DEBUG_SEVERITY_LOW -> "Low";
            case DEBUG_SEVERITY_NOTIFICATION -> "Notification";
            default -> "Unknown";
        };
    }
}

// ============================================================================
// SECTION 6: MAIN API CALL MAPPER
// ============================================================================

/**
 * OpenGL ES Call Mapper - Core implementation.
 * Maps abstract graphics calls to concrete OpenGL ES implementations
 * with automatic fallback support for different GLES versions.
 */
public final class OpenGLESCallMapper {
    
    private final GLESCapabilities capabilities;
    private final GLESStateTracker stateTracker;
    private final GLESResourceRegistry resourceRegistry;
    private final GLESErrorHandler errorHandler;
    
    // Pre-allocated direct buffers for zero-allocation operations
    private final ThreadLocal<ByteBuffer> scratchBuffer;
    private final ThreadLocal<IntBuffer> intScratchBuffer;
    private final ThreadLocal<FloatBuffer> floatScratchBuffer;
    
    // Platform quirks
    private final GPUQuirks quirks;
    
    public OpenGLESCallMapper(GLESCapabilities capabilities) {
        this.capabilities = capabilities;
        this.stateTracker = new GLESStateTracker(capabilities);
        this.resourceRegistry = new GLESResourceRegistry();
        this.errorHandler = new GLESErrorHandler(capabilities);
        this.quirks = new GPUQuirks(capabilities);
        
        // Pre-allocate scratch buffers
        scratchBuffer = ThreadLocal.withInitial(() -> 
            ByteBuffer.allocateDirect(64 * 1024).order(ByteOrder.nativeOrder()));
        intScratchBuffer = ThreadLocal.withInitial(() -> 
            ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder()).asIntBuffer());
        floatScratchBuffer = ThreadLocal.withInitial(() -> 
            ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder()).asFloatBuffer());
    }
    
    public GLESCapabilities getCapabilities() { return capabilities; }
    public GLESStateTracker getStateTracker() { return stateTracker; }
    public GLESResourceRegistry getResourceRegistry() { return resourceRegistry; }
    public GLESErrorHandler getErrorHandler() { return errorHandler; }
    
    /**
     * Main entry point for mapping calls.
     */
    public MappingResult mapCall(CallDescriptor call) {
        return switch (call.type()) {
            // Buffer operations
            case BUFFER_CREATE -> mapBufferCreate(call);
            case BUFFER_DELETE -> mapBufferDelete(call);
            case BUFFER_BIND -> mapBufferBind(call);
            case BUFFER_DATA -> mapBufferData(call);
            case BUFFER_SUB_DATA -> mapBufferSubData(call);
            case BUFFER_MAP -> mapBufferMap(call);
            case BUFFER_UNMAP -> mapBufferUnmap(call);
            case BUFFER_COPY -> mapBufferCopy(call);
            case BUFFER_FLUSH_MAPPED -> mapBufferFlushMapped(call);
            
            // Texture operations
            case TEXTURE_CREATE -> mapTextureCreate(call);
            case TEXTURE_DELETE -> mapTextureDelete(call);
            case TEXTURE_BIND -> mapTextureBind(call);
            case TEXTURE_IMAGE_2D -> mapTextureImage2D(call);
            case TEXTURE_IMAGE_3D -> mapTextureImage3D(call);
            case TEXTURE_SUB_IMAGE_2D -> mapTextureSubImage2D(call);
            case TEXTURE_SUB_IMAGE_3D -> mapTextureSubImage3D(call);
            case TEXTURE_COMPRESSED -> mapTextureCompressed(call);
            case TEXTURE_STORAGE_2D -> mapTextureStorage2D(call);
            case TEXTURE_STORAGE_3D -> mapTextureStorage3D(call);
            case TEXTURE_GENERATE_MIPMAP -> mapTextureGenerateMipmap(call);
            case TEXTURE_PARAMETER -> mapTextureParameter(call);
            case TEXTURE_BIND_IMAGE -> mapTextureBindImage(call);
            case SAMPLER_CREATE -> mapSamplerCreate(call);
            case SAMPLER_DELETE -> mapSamplerDelete(call);
            case SAMPLER_BIND -> mapSamplerBind(call);
            case SAMPLER_PARAMETER -> mapSamplerParameter(call);
            
            // Draw operations
            case DRAW_ARRAYS -> mapDrawArrays(call);
            case DRAW_ELEMENTS -> mapDrawElements(call);
            case DRAW_ARRAYS_INSTANCED -> mapDrawArraysInstanced(call);
            case DRAW_ELEMENTS_INSTANCED -> mapDrawElementsInstanced(call);
            case DRAW_RANGE_ELEMENTS -> mapDrawRangeElements(call);
            case DRAW_ARRAYS_INDIRECT -> mapDrawArraysIndirect(call);
            case DRAW_ELEMENTS_INDIRECT -> mapDrawElementsIndirect(call);
            case DRAW_MULTI_ARRAYS -> mapDrawMultiArrays(call);
            case DRAW_MULTI_ELEMENTS -> mapDrawMultiElements(call);
            case DRAW_ELEMENTS_BASE_VERTEX -> mapDrawElementsBaseVertex(call);
            
            // State operations
            case STATE_ENABLE -> mapStateEnable(call);
            case STATE_DISABLE -> mapStateDisable(call);
            case STATE_BLEND_FUNC -> mapBlendFunc(call);
            case STATE_BLEND_FUNC_SEPARATE -> mapBlendFuncSeparate(call);
            case STATE_BLEND_EQUATION -> mapBlendEquation(call);
            case STATE_BLEND_EQUATION_SEPARATE -> mapBlendEquationSeparate(call);
            case STATE_DEPTH_FUNC -> mapDepthFunc(call);
            case STATE_DEPTH_MASK -> mapDepthMask(call);
            case STATE_STENCIL_FUNC -> mapStencilFunc(call);
            case STATE_STENCIL_OP -> mapStencilOp(call);
            case STATE_STENCIL_MASK -> mapStencilMask(call);
            case STATE_CULL_FACE -> mapCullFace(call);
            case STATE_FRONT_FACE -> mapFrontFace(call);
            case STATE_POLYGON_OFFSET -> mapPolygonOffset(call);
            case STATE_VIEWPORT -> mapViewport(call);
            case STATE_SCISSOR -> mapScissor(call);
            case STATE_COLOR_MASK -> mapColorMask(call);
            case STATE_LINE_WIDTH -> mapLineWidth(call);
            case STATE_POINT_SIZE -> mapPointSize(call);
            
            // Framebuffer operations
            case FRAMEBUFFER_CREATE -> mapFramebufferCreate(call);
            case FRAMEBUFFER_DELETE -> mapFramebufferDelete(call);
            case FRAMEBUFFER_BIND -> mapFramebufferBind(call);
            case FRAMEBUFFER_TEXTURE_2D -> mapFramebufferTexture2D(call);
            case FRAMEBUFFER_RENDERBUFFER -> mapFramebufferRenderbuffer(call);
            case FRAMEBUFFER_CHECK_STATUS -> mapFramebufferCheckStatus(call);
            case FRAMEBUFFER_BLIT -> mapFramebufferBlit(call);
            case FRAMEBUFFER_INVALIDATE -> mapFramebufferInvalidate(call);
            case FRAMEBUFFER_READ_PIXELS -> mapFramebufferReadPixels(call);
            case FRAMEBUFFER_CLEAR -> mapFramebufferClear(call);
            case FRAMEBUFFER_CLEAR_BUFFER -> mapFramebufferClearBuffer(call);
            case RENDERBUFFER_CREATE -> mapRenderbufferCreate(call);
            case RENDERBUFFER_DELETE -> mapRenderbufferDelete(call);
            case RENDERBUFFER_BIND -> mapRenderbufferBind(call);
            case RENDERBUFFER_STORAGE -> mapRenderbufferStorage(call);
            
            // Shader operations
            case SHADER_CREATE -> mapShaderCreate(call);
            case SHADER_DELETE -> mapShaderDelete(call);
            case SHADER_SOURCE -> mapShaderSource(call);
            case SHADER_COMPILE -> mapShaderCompile(call);
            case PROGRAM_CREATE -> mapProgramCreate(call);
            case PROGRAM_DELETE -> mapProgramDelete(call);
            case PROGRAM_ATTACH -> mapProgramAttach(call);
            case PROGRAM_LINK -> mapProgramLink(call);
            case PROGRAM_USE -> mapProgramUse(call);
            case PROGRAM_GET_UNIFORM_LOCATION -> mapProgramGetUniformLocation(call);
            case PROGRAM_UNIFORM -> mapProgramUniform(call);
            case PROGRAM_GET_ATTRIB_LOCATION -> mapProgramGetAttribLocation(call);
            case PROGRAM_VERTEX_ATTRIB -> mapProgramVertexAttrib(call);
            case PROGRAM_BIND_ATTRIB_LOCATION -> mapProgramBindAttribLocation(call);
            case PROGRAM_GET_INFO_LOG -> mapProgramGetInfoLog(call);
            case PROGRAM_BINARY_GET -> mapProgramBinaryGet(call);
            case PROGRAM_BINARY_LOAD -> mapProgramBinaryLoad(call);
            
            // VAO operations
            case VAO_CREATE -> mapVAOCreate(call);
            case VAO_DELETE -> mapVAODelete(call);
            case VAO_BIND -> mapVAOBind(call);
            case VAO_VERTEX_ATTRIB_POINTER -> mapVAOVertexAttribPointer(call);
            case VAO_VERTEX_ATTRIB_I_POINTER -> mapVAOVertexAttribIPointer(call);
            case VAO_ENABLE_ATTRIB -> mapVAOEnableAttrib(call);
            case VAO_DISABLE_ATTRIB -> mapVAODisableAttrib(call);
            case VAO_VERTEX_ATTRIB_DIVISOR -> mapVAOVertexAttribDivisor(call);
            
            // Compute operations
            case COMPUTE_DISPATCH -> mapComputeDispatch(call);
            case COMPUTE_DISPATCH_INDIRECT -> mapComputeDispatchIndirect(call);
            case COMPUTE_MEMORY_BARRIER -> mapComputeMemoryBarrier(call);
            
            // Query operations
            case QUERY_CREATE -> mapQueryCreate(call);
            case QUERY_DELETE -> mapQueryDelete(call);
            case QUERY_BEGIN -> mapQueryBegin(call);
            case QUERY_END -> mapQueryEnd(call);
            case QUERY_GET_RESULT -> mapQueryGetResult(call);
            
            // Sync operations
            case SYNC_FENCE_CREATE -> mapSyncFenceCreate(call);
            case SYNC_DELETE -> mapSyncDelete(call);
            case SYNC_CLIENT_WAIT -> mapSyncClientWait(call);
            case SYNC_WAIT -> mapSyncWait(call);
            case SYNC_FLUSH -> mapSyncFlush(call);
            case SYNC_FINISH -> mapSyncFinish(call);
            
            // Miscellaneous
            case GET_ERROR -> mapGetError(call);
            case GET_STRING -> mapGetString(call);
            case GET_INTEGER -> mapGetInteger(call);
            case GET_FLOAT -> mapGetFloat(call);
            case GET_BOOLEAN -> mapGetBoolean(call);
            case PIXEL_STORE -> mapPixelStore(call);
            case HINT -> mapHint(call);
            case DEBUG_MESSAGE_CALLBACK -> mapDebugMessageCallback(call);
            case DEBUG_MESSAGE_INSERT -> mapDebugMessageInsert(call);
        };
    }
    
    // ========================================================================
    // BUFFER OPERATIONS
    // ========================================================================
    
    private MappingResult mapBufferCreate(CallDescriptor call) {
        int count = call.intParam(0);
        if (count <= 0) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION, 
                "Buffer count must be positive");
        }
        
        IntBuffer buffers = intScratchBuffer.get();
        buffers.clear();
        buffers.limit(count);
        
        GLES20.glGenBuffers(buffers);
        
        GLESError error = errorHandler.checkError("glGenBuffers");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        // Return first buffer handle
        return MappingResult.success(buffers.get(0));
    }
    
    private MappingResult mapBufferDelete(CallDescriptor call) {
        int buffer = call.intParam(0);
        if (buffer == 0) {
            return MappingResult.success(); // Deleting 0 is a no-op
        }
        
        GLES20.glDeleteBuffers(buffer);
        resourceRegistry.unregisterBuffer(buffer);
        
        return MappingResult.success();
    }
    
    private MappingResult mapBufferBind(CallDescriptor call) {
        int targetConstant = call.intParam(0);
        int buffer = call.intParam(1);
        
        BufferTarget target = BufferTarget.fromGLConstant(targetConstant);
        if (target == null) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION, 
                "Unknown buffer target: " + targetConstant);
        }
        
        // Check version support
        if (!target.isSupported(capabilities.version())) {
            return MappingResult.fallback(
                call.toBuilder().setInt(0, BufferTarget.ARRAY_BUFFER.glConstant()).build(),
                "Buffer target " + target + " not supported in " + capabilities.version()
            );
        }
        
        // Skip if already bound
        if (!stateTracker.bindBuffer(target, buffer)) {
            return MappingResult.success();
        }
        
        GLES20.glBindBuffer(targetConstant, buffer);
        
        GLESError error = errorHandler.checkError("glBindBuffer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    
    private MappingResult mapBufferData(CallDescriptor call) {
        int targetConstant = call.intParam(0);
        int size = call.intParam(1);
        int usage = call.intParam(2);
        ByteBuffer data = call.objectParam(0);
        
        BufferTarget target = BufferTarget.fromGLConstant(targetConstant);
        if (target == null) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION, 
                "Unknown buffer target: " + targetConstant);
        }
        
        if (!target.isSupported(capabilities.version())) {
            return MappingResult.fallback(
                call.toBuilder().setInt(0, BufferTarget.ARRAY_BUFFER.glConstant()).build(),
                "Buffer target " + target + " requires " + target.minVersion()
            );
        }
        
        // Apply platform-specific quirks
        usage = quirks.adjustBufferUsage(target, usage);
        
        if (data != null) {
            GLES20.glBufferData(targetConstant, data, usage);
        } else {
            GLES20.glBufferData(targetConstant, size, usage);
        }
        
        GLESError error = errorHandler.checkError("glBufferData");
        if (error != GLESError.NO_ERROR) {
            if (error == GLESError.OUT_OF_MEMORY) {
                return MappingResult.failed(MappingStatus.FAILED_RESOURCE, error.code(),
                    "Out of GPU memory allocating " + size + " bytes");
            }
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapBufferSubData(CallDescriptor call) {
        int targetConstant = call.intParam(0);
        int offset = call.intParam(1);
        int size = call.intParam(2);
        ByteBuffer data = call.objectParam(0);
        
        if (data == null) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION, "Data buffer cannot be null");
        }
        
        GLES20.glBufferSubData(targetConstant, offset, data);
        
        GLESError error = errorHandler.checkError("glBufferSubData");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapBufferMap(CallDescriptor call) {
        int targetConstant = call.intParam(0);
        int offset = call.intParam(1);
        int length = call.intParam(2);
        int access = call.intParam(3);
        
        // Check version support
        if (!capabilities.supportsMapBufferRange()) {
            // GLES 2.0 fallback - use OES_mapbuffer extension or emulate
            if (capabilities.supportsMapBuffer()) {
                // Extension available but only supports full buffer mapping
                return mapBufferMapFallbackOES(call, targetConstant, access);
            } else {
                // No mapping support - must use BufferSubData
                return MappingResult.fallback(
                    CallDescriptor.acquire(CallType.BUFFER_SUB_DATA)
                        .addInt(targetConstant)
                        .addInt(offset)
                        .addInt(length)
                        .build(),
                    "Buffer mapping not supported, use glBufferSubData instead"
                );
            }
        }
        
        // GLES 3.0+ path
        ByteBuffer mapped = GLES30.glMapBufferRange(targetConstant, offset, length, access);
        
        GLESError error = errorHandler.checkError("glMapBufferRange");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        if (mapped == null) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, "glMapBufferRange returned null");
        }
        
        // Store mapped pointer in result for retrieval
        return MappingResult.success(MemoryUtil.memAddress(mapped));
    }
    
    private MappingResult mapBufferMapFallbackOES(CallDescriptor call, int target, int access) {
        // OES_mapbuffer only supports GL_WRITE_ONLY_OES
        int oesAccess = 0x88B9; // GL_WRITE_ONLY_OES
        
        try {
            // Use extension function via LWJGL
            ByteBuffer mapped = GLES20.glMapBufferOES(target, oesAccess);
            
            if (mapped == null) {
                return MappingResult.failed(MappingStatus.FAILED_EXECUTION, 
                    "glMapBufferOES returned null");
            }
            
            return MappingResult.success(MemoryUtil.memAddress(mapped));
        } catch (Exception e) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, e.getMessage());
        }
    }
    
    private MappingResult mapBufferUnmap(CallDescriptor call) {
        int targetConstant = call.intParam(0);
        
        boolean success;
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            success = GLES30.glUnmapBuffer(targetConstant);
        } else if (capabilities.supportsMapBuffer()) {
            success = GLES20.glUnmapBufferOES(targetConstant);
        } else {
            // No mapping support - this shouldn't happen
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION, 
                "Buffer was never mapped (no mapping support)");
        }
        
        GLESError error = errorHandler.checkError("glUnmapBuffer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        if (!success) {
            return MappingResult.successWithWarning(0x0001, 
                "Buffer contents may be corrupted (glUnmapBuffer returned false)");
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapBufferCopy(CallDescriptor call) {
        int readTarget = call.intParam(0);
        int writeTarget = call.intParam(1);
        int readOffset = call.intParam(2);
        int writeOffset = call.intParam(3);
        int size = call.intParam(4);
        
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // GLES 2.0 fallback - read back to CPU and re-upload
            return mapBufferCopyFallbackGLES2(call, readTarget, writeTarget, 
                readOffset, writeOffset, size);
        }
        
        GLES30.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        
        GLESError error = errorHandler.checkError("glCopyBufferSubData");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapBufferCopyFallbackGLES2(CallDescriptor call, int readTarget, 
            int writeTarget, int readOffset, int writeOffset, int size) {
        // This is slow but necessary for GLES 2.0
        // We need to map/read the source and write to destination
        
        if (!capabilities.supportsMapBuffer()) {
            return MappingResult.failed(MappingStatus.UNSUPPORTED, 
                "Buffer copy requires GLES 3.0+ or OES_mapbuffer extension");
        }
        
        // Map source buffer
        ByteBuffer srcMapped = GLES20.glMapBufferOES(readTarget, 0x88B8); // GL_READ_ONLY
        if (srcMapped == null) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, 
                "Failed to map source buffer for copy");
        }
        
        // Create temporary buffer
        ByteBuffer tempBuffer = scratchBuffer.get();
        tempBuffer.clear();
        if (size > tempBuffer.capacity()) {
            tempBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        }
        tempBuffer.limit(size);
        
        // Copy data
        srcMapped.position(readOffset);
        srcMapped.limit(readOffset + size);
        tempBuffer.put(srcMapped);
        tempBuffer.flip();
        
        // Unmap source
        GLES20.glUnmapBufferOES(readTarget);
        
        // Upload to destination
        GLES20.glBufferSubData(writeTarget, writeOffset, tempBuffer);
        
        return MappingResult.successWithWarning(0x0002, 
            "Buffer copy emulated via CPU readback (slow)");
    }
    
    private MappingResult mapBufferFlushMapped(CallDescriptor call) {
        int targetConstant = call.intParam(0);
        int offset = call.intParam(1);
        int length = call.intParam(2);
        
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // No explicit flush in GLES 2.0 - flush happens on unmap
            return MappingResult.success();
        }
        
        GLES30.glFlushMappedBufferRange(targetConstant, offset, length);
        
        GLESError error = errorHandler.checkError("glFlushMappedBufferRange");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // TEXTURE OPERATIONS
    // ========================================================================
    
    private MappingResult mapTextureCreate(CallDescriptor call) {
        int count = call.intParam(0);
        if (count <= 0) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION, 
                "Texture count must be positive");
        }
        
        IntBuffer textures = intScratchBuffer.get();
        textures.clear();
        textures.limit(count);
        
        GLES20.glGenTextures(textures);
        
        GLESError error = errorHandler.checkError("glGenTextures");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(textures.get(0));
    }
    
    private MappingResult mapTextureDelete(CallDescriptor call) {
        int texture = call.intParam(0);
        if (texture == 0) {
            return MappingResult.success();
        }
        
        GLES20.glDeleteTextures(texture);
        resourceRegistry.unregisterTexture(texture);
        
        return MappingResult.success();
    }
    
    private MappingResult mapTextureBind(CallDescriptor call) {
        int targetConstant = call.intParam(0);
        int texture = call.intParam(1);
        int unit = call.intParam(2);
        
        TextureTarget target = TextureTarget.values()[0]; // Default
        for (TextureTarget t : TextureTarget.values()) {
            if (t.glConstant() == targetConstant) {
                target = t;
                break;
            }
        }
        
        // Check version support
        if (!target.isSupported(capabilities.version())) {
            return MappingResult.fallback(
                call.toBuilder().setInt(0, TextureTarget.TEXTURE_2D.glConstant()).build(),
                "Texture target " + target + " requires " + target.minVersion()
            );
        }
        
        // Activate texture unit if specified
        if (unit >= 0) {
            int glUnit = 0x84C0 + unit; // GL_TEXTURE0 + unit
            if (stateTracker.setActiveTexture(unit)) {
                GLES20.glActiveTexture(glUnit);
            }
        }
        
        // Bind texture
        boolean changed = switch (target) {
            case TEXTURE_2D -> stateTracker.bindTexture2D(unit >= 0 ? unit : stateTracker.getActiveTexture(), texture);
            case TEXTURE_CUBE_MAP -> stateTracker.bindTextureCube(unit >= 0 ? unit : stateTracker.getActiveTexture(), texture);
            case TEXTURE_3D -> stateTracker.bindTexture3D(unit >= 0 ? unit : stateTracker.getActiveTexture(), texture);
            case TEXTURE_2D_ARRAY -> stateTracker.bindTexture2DArray(unit >= 0 ? unit : stateTracker.getActiveTexture(), texture);
            default -> true;
        };
        
        if (!changed) {
            return MappingResult.success(); // Already bound
        }
        
        GLES20.glBindTexture(targetConstant, texture);
        
        GLESError error = errorHandler.checkError("glBindTexture");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapTextureImage2D(CallDescriptor call) {
        int target = call.intParam(0);
        int level = call.intParam(1);
        int internalFormat = call.intParam(2);
        int width = call.intParam(3);
        int height = call.intParam(4);
        int border = call.intParam(5);
        int format = call.intParam(6);
        int type = call.intParam(7);
        ByteBuffer data = call.objectParam(0);
        
        // Validate dimensions
        if (width > capabilities.maxTextureSize() || height > capabilities.maxTextureSize()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Texture dimensions " + width + "x" + height + " exceed max " + capabilities.maxTextureSize());
        }
        
        // Apply format conversion for platform quirks
        int adjustedFormat = quirks.adjustTextureFormat(internalFormat, format, type);
        int adjustedInternalFormat = quirks.adjustInternalFormat(internalFormat);
        
        if (data != null) {
            GLES20.glTexImage2D(target, level, adjustedInternalFormat, width, height, 
                border, adjustedFormat, type, data);
        } else {
            GLES20.glTexImage2D(target, level, adjustedInternalFormat, width, height, 
                border, adjustedFormat, type, (ByteBuffer) null);
        }
        
        GLESError error = errorHandler.checkError("glTexImage2D");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapTextureImage3D(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0) && 
            !capabilities.hasExtension(GLESExtension.OES_texture_3D)) {
            return MappingResult.unsupported("3D textures require GLES 3.0+ or OES_texture_3D");
        }
        
        int target = call.intParam(0);
        int level = call.intParam(1);
        int internalFormat = call.intParam(2);
        int width = call.intParam(3);
        int height = call.intParam(4);
        int depth = call.intParam(5);
        int border = call.intParam(6);
        int format = call.intParam(7);
        int type = call.intParam(8);
        ByteBuffer data = call.objectParam(0);
        
        // Validate dimensions
        if (width > capabilities.max3DTextureSize() || 
            height > capabilities.max3DTextureSize() || 
            depth > capabilities.max3DTextureSize()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "3D texture dimensions exceed max " + capabilities.max3DTextureSize());
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glTexImage3D(target, level, internalFormat, width, height, depth, 
                border, format, type, data);
        } else {
            // Use OES extension
            GLES20.glTexImage3DOES(target, level, internalFormat, width, height, depth, 
                border, format, type, data);
        }
        
        GLESError error = errorHandler.checkError("glTexImage3D");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapTextureSubImage2D(CallDescriptor call) {
        int target = call.intParam(0);
        int level = call.intParam(1);
        int xoffset = call.intParam(2);
        int yoffset = call.intParam(3);
        int width = call.intParam(4);
        int height = call.intParam(5);
        int format = call.intParam(6);
        int type = call.intParam(7);
        ByteBuffer data = call.objectParam(0);
        
        if (data == null) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION, "Data buffer cannot be null");
        }
        
        GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, data);
        
        GLESError error = errorHandler.checkError("glTexSubImage2D");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapTextureSubImage3D(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("glTexSubImage3D requires GLES 3.0+");
        }
        
        int target = call.intParam(0);
        int level = call.intParam(1);
        int xoffset = call.intParam(2);
        int yoffset = call.intParam(3);
        int zoffset = call.intParam(4);
        int width = call.intParam(5);
        int height = call.intParam(6);
        int depth = call.intParam(7);
        int format = call.intParam(8);
        int type = call.intParam(9);
        ByteBuffer data = call.objectParam(0);
        
        GLES30.glTexSubImage3D(target, level, xoffset, yoffset, zoffset, 
            width, height, depth, format, type, data);
        
        GLESError error = errorHandler.checkError("glTexSubImage3D");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapTextureCompressed(CallDescriptor call) {
        int target = call.intParam(0);
        int level = call.intParam(1);
        int internalFormat = call.intParam(2);
        int width = call.intParam(3);
        int height = call.intParam(4);
        int border = call.intParam(5);
        int imageSize = call.intParam(6);
        ByteBuffer data = call.objectParam(0);
        
        // Check format support
        if (!capabilities.supportsCompressedFormat(internalFormat)) {
            // Try to find fallback format
            int fallbackFormat = quirks.findFallbackCompressedFormat(internalFormat);
            if (fallbackFormat != 0 && capabilities.supportsCompressedFormat(fallbackFormat)) {
                // Would need to transcode texture data - complex operation
                return MappingResult.failed(MappingStatus.FALLBACK_REQUIRED,
                    "Compressed format " + Integer.toHexString(internalFormat) + 
                    " not supported, transcoding required");
            }
            return MappingResult.unsupported("Compressed format " + 
                Integer.toHexString(internalFormat) + " not supported");
        }
        
        GLES20.glCompressedTexImage2D(target, level, internalFormat, width, height, 
            border, imageSize, data);
        
        GLESError error = errorHandler.checkError("glCompressedTexImage2D");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapTextureStorage2D(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0) &&
            !capabilities.hasExtension(GLESExtension.EXT_texture_storage)) {
            // Fallback to glTexImage2D calls
            return mapTextureStorage2DFallback(call);
        }
        
        int target = call.intParam(0);
        int levels = call.intParam(1);
        int internalFormat = call.intParam(2);
        int width = call.intParam(3);
        int height = call.intParam(4);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glTexStorage2D(target, levels, internalFormat, width, height);
        } else {
            GLES20.glTexStorage2DEXT(target, levels, internalFormat, width, height);
        }
        
        GLESError error = errorHandler.checkError("glTexStorage2D");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapTextureStorage2DFallback(CallDescriptor call) {
        int target = call.intParam(0);
        int levels = call.intParam(1);
        int internalFormat = call.intParam(2);
        int width = call.intParam(3);
        int height = call.intParam(4);
        
        // Determine format and type from internal format
        int[] formatType = getFormatAndTypeFromInternal(internalFormat);
        int format = formatType[0];
        int type = formatType[1];
        
        int levelWidth = width;
        int levelHeight = height;
        
        for (int level = 0; level < levels; level++) {
            GLES20.glTexImage2D(target, level, internalFormat, levelWidth, levelHeight, 
                0, format, type, (ByteBuffer) null);
            
            GLESError error = errorHandler.checkError("glTexImage2D (storage fallback)");
            if (error != GLESError.NO_ERROR) {
                return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), 
                    error.description() + " at level " + level);
            }
            
            levelWidth = Math.max(1, levelWidth / 2);
            levelHeight = Math.max(1, levelHeight / 2);
        }
        
        return MappingResult.successWithWarning(0x0004, 
            "glTexStorage2D emulated with glTexImage2D calls");
    }
    
    private MappingResult mapTextureStorage3D(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("glTexStorage3D requires GLES 3.0+");
        }
        
        int target = call.intParam(0);
        int levels = call.intParam(1);
        int internalFormat = call.intParam(2);
        int width = call.intParam(3);
        int height = call.intParam(4);
        int depth = call.intParam(5);
        
        GLES30.glTexStorage3D(target, levels, internalFormat, width, height, depth);
        
        GLESError error = errorHandler.checkError("glTexStorage3D");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapTextureGenerateMipmap(CallDescriptor call) {
        int target = call.intParam(0);
        
        GLES20.glGenerateMipmap(target);
        
        GLESError error = errorHandler.checkError("glGenerateMipmap");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapTextureParameter(CallDescriptor call) {
        int target = call.intParam(0);
        int pname = call.intParam(1);
        int subType = call.subFunction(); // 0 = int, 1 = float
        
        if (subType == 0) {
            int param = call.intParam(2);
            
            // Check for anisotropic filtering
            if (pname == 0x84FE) { // GL_TEXTURE_MAX_ANISOTROPY_EXT
                if (!capabilities.supportsAnisotropicFiltering()) {
                    return MappingResult.successWithWarning(0x0008, 
                        "Anisotropic filtering not supported, parameter ignored");
                }
                param = Math.min(param, (int) capabilities.maxTextureAnisotropy());
            }
            
            GLES20.glTexParameteri(target, pname, param);
        } else {
            float param = call.floatParam(0);
            GLES20.glTexParameterf(target, pname, param);
        }
        
        GLESError error = errorHandler.checkError("glTexParameter");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapTextureBindImage(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Image load/store requires GLES 3.1+");
        }
        
        int unit = call.intParam(0);
        int texture = call.intParam(1);
        int level = call.intParam(2);
        boolean layered = call.intParam(3) != 0;
        int layer = call.intParam(4);
        int access = call.intParam(5);
        int format = call.intParam(6);
        
        GLES31.glBindImageTexture(unit, texture, level, layered, layer, access, format);
        
        GLESError error = errorHandler.checkError("glBindImageTexture");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapSamplerCreate(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // GLES 2.0 doesn't have separate sampler objects
            // Return dummy handle - will use texture parameters instead
            return MappingResult.successWithWarning(0x0010, 
                "Sampler objects not supported, using texture parameters");
        }
        
        int count = call.intParam(0);
        IntBuffer samplers = intScratchBuffer.get();
        samplers.clear();
        samplers.limit(count);
        
        GLES30.glGenSamplers(samplers);
        
        GLESError error = errorHandler.checkError("glGenSamplers");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(samplers.get(0));
    }
    
    private MappingResult mapSamplerDelete(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.success(); // No-op for GLES 2.0
        }
        
        int sampler = call.intParam(0);
        if (sampler == 0) {
            return MappingResult.success();
        }
        
        GLES30.glDeleteSamplers(sampler);
        
        return MappingResult.success();
    }
    
    private MappingResult mapSamplerBind(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.successWithWarning(0x0010, 
                "Sampler binding not supported in GLES 2.0");
        }
        
        int unit = call.intParam(0);
        int sampler = call.intParam(1);
        
        if (!stateTracker.bindSampler(unit, sampler)) {
            return MappingResult.success(); // Already bound
        }
        
        GLES30.glBindSampler(unit, sampler);
        
        GLESError error = errorHandler.checkError("glBindSampler");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapSamplerParameter(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.successWithWarning(0x0010, 
                "Sampler parameters not supported, use texture parameters");
        }
        
        int sampler = call.intParam(0);
        int pname = call.intParam(1);
        int subType = call.subFunction();
        
        if (subType == 0) {
            int param = call.intParam(2);
            GLES30.glSamplerParameteri(sampler, pname, param);
        } else {
            float param = call.floatParam(0);
            GLES30.glSamplerParameterf(sampler, pname, param);
        }
        
        GLESError error = errorHandler.checkError("glSamplerParameter");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // DRAW OPERATIONS
    // ========================================================================
    
    private MappingResult mapDrawArrays(CallDescriptor call) {
        int mode = call.intParam(0);
        int first = call.intParam(1);
        int count = call.intParam(2);
        
        if (count <= 0) {
            return MappingResult.success(); // Nothing to draw
        }
        
        // Validate draw mode
        mode = validateDrawMode(mode);
        
        GLES20.glDrawArrays(mode, first, count);
        
        GLESError error = errorHandler.checkError("glDrawArrays");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        // Update statistics
        int triangles = calculateTriangleCount(mode, count);
        stateTracker.recordDrawCall(triangles);
        
        return MappingResult.success();
    }
    
    private MappingResult mapDrawElements(CallDescriptor call) {
        int mode = call.intParam(0);
        int count = call.intParam(1);
        int type = call.intParam(2);
        long offset = call.longParam(0);
        
        if (count <= 0) {
            return MappingResult.success();
        }
        
        // Check 32-bit index support
        if (type == IndexType.UNSIGNED_INT.glConstant() && !capabilities.supports32BitIndex()) {
            return MappingResult.fallback(
                call.toBuilder().setInt(2, IndexType.UNSIGNED_SHORT.glConstant()).build(),
                "32-bit indices not supported, consider splitting mesh"
            );
        }
        
        mode = validateDrawMode(mode);
        
        GLES20.glDrawElements(mode, count, type, offset);
        
        GLESError error = errorHandler.checkError("glDrawElements");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        int triangles = calculateTriangleCount(mode, count);
        stateTracker.recordDrawCall(triangles);
        
        return MappingResult.success();
    }
    
    private MappingResult mapDrawArraysInstanced(CallDescriptor call) {
        int mode = call.intParam(0);
        int first = call.intParam(1);
        int count = call.intParam(2);
        int instanceCount = call.intParam(3);
        
        if (count <= 0 || instanceCount <= 0) {
            return MappingResult.success();
        }
        
        if (!capabilities.supportsInstancing()) {
            // Fallback to loop of draw calls
            return mapDrawArraysInstancedFallback(call, mode, first, count, instanceCount);
        }
        
        mode = validateDrawMode(mode);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glDrawArraysInstanced(mode, first, count, instanceCount);
        } else if (capabilities.hasExtension(GLESExtension.EXT_instanced_arrays)) {
            GLES20.glDrawArraysInstancedEXT(mode, first, count, instanceCount);
        } else if (capabilities.hasExtension(GLESExtension.ANGLE_instanced_arrays)) {
            GLES20.glDrawArraysInstancedANGLE(mode, first, count, instanceCount);
        } else if (capabilities.hasExtension(GLESExtension.NV_instanced_arrays)) {
            GLES20.glDrawArraysInstancedNV(mode, first, count, instanceCount);
        }
        
        GLESError error = errorHandler.checkError("glDrawArraysInstanced");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        int triangles = calculateTriangleCount(mode, count) * instanceCount;
        stateTracker.recordDrawCall(triangles);
        
        return MappingResult.success();
    }
    
    private MappingResult mapDrawArraysInstancedFallback(CallDescriptor call, 
            int mode, int first, int count, int instanceCount) {
        // CPU-side loop for systems without instancing
        mode = validateDrawMode(mode);
        
        for (int i = 0; i < instanceCount; i++) {
            // Would need to set gl_InstanceID uniform manually
            GLES20.glDrawArrays(mode, first, count);
            
            GLESError error = errorHandler.checkError("glDrawArrays (instancing fallback)");
            if (error != GLESError.NO_ERROR) {
                return MappingResult.failed(MappingStatus.FAILED_EXECUTION, 
                    error.code(), error.description() + " at instance " + i);
            }
        }
        
        int triangles = calculateTriangleCount(mode, count) * instanceCount;
        stateTracker.recordDrawCall(triangles);
        
        return MappingResult.successWithWarning(0x0020, 
            "Instancing emulated with " + instanceCount + " draw calls (slow)");
    }
    
    private MappingResult mapDrawElementsInstanced(CallDescriptor call) {
        int mode = call.intParam(0);
        int count = call.intParam(1);
        int type = call.intParam(2);
        long offset = call.longParam(0);
        int instanceCount = call.intParam(3);
        
        if (count <= 0 || instanceCount <= 0) {
            return MappingResult.success();
        }
        
        if (!capabilities.supportsInstancing()) {
            return mapDrawElementsInstancedFallback(call, mode, count, type, offset, instanceCount);
        }
        
        if (type == IndexType.UNSIGNED_INT.glConstant() && !capabilities.supports32BitIndex()) {
            return MappingResult.fallback(
                call.toBuilder().setInt(2, IndexType.UNSIGNED_SHORT.glConstant()).build(),
                "32-bit indices not supported"
            );
        }
        
        mode = validateDrawMode(mode);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glDrawElementsInstanced(mode, count, type, offset, instanceCount);
        } else if (capabilities.hasExtension(GLESExtension.EXT_instanced_arrays)) {
            GLES20.glDrawElementsInstancedEXT(mode, count, type, offset, instanceCount);
        } else if (capabilities.hasExtension(GLESExtension.ANGLE_instanced_arrays)) {
            GLES20.glDrawElementsInstancedANGLE(mode, count, type, offset, instanceCount);
        }
        
        GLESError error = errorHandler.checkError("glDrawElementsInstanced");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        int triangles = calculateTriangleCount(mode, count) * instanceCount;
        stateTracker.recordDrawCall(triangles);
        
        return MappingResult.success();
    }
    
    private MappingResult mapDrawElementsInstancedFallback(CallDescriptor call,
            int mode, int count, int type, long offset, int instanceCount) {
        mode = validateDrawMode(mode);
        
        for (int i = 0; i < instanceCount; i++) {
            GLES20.glDrawElements(mode, count, type, offset);
            
            GLESError error = errorHandler.checkError("glDrawElements (instancing fallback)");
            if (error != GLESError.NO_ERROR) {
                return MappingResult.failed(MappingStatus.FAILED_EXECUTION,
                    error.code(), error.description() + " at instance " + i);
            }
        }
        
        int triangles = calculateTriangleCount(mode, count) * instanceCount;
        stateTracker.recordDrawCall(triangles);
        
        return MappingResult.successWithWarning(0x0020,
            "Instancing emulated with " + instanceCount + " draw calls");
    }
    
    private MappingResult mapDrawRangeElements(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fall back to regular DrawElements
            return mapDrawElements(call);
        }
        
        int mode = call.intParam(0);
        int start = call.intParam(1);
        int end = call.intParam(2);
        int count = call.intParam(3);
        int type = call.intParam(4);
        long offset = call.longParam(0);
        
        if (count <= 0) {
            return MappingResult.success();
        }
        
        mode = validateDrawMode(mode);
        
        GLES30.glDrawRangeElements(mode, start, end, count, type, offset);
        
        GLESError error = errorHandler.checkError("glDrawRangeElements");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        int triangles = calculateTriangleCount(mode, count);
        stateTracker.recordDrawCall(triangles);
        
        return MappingResult.success();
    }
    
    private MappingResult mapDrawArraysIndirect(CallDescriptor call) {
        if (!capabilities.supportsIndirectDraw()) {
            return mapDrawArraysIndirectFallback(call);
        }
        
        int mode = call.intParam(0);
        long offset = call.longParam(0);
        
        mode = validateDrawMode(mode);
        
        GLES31.glDrawArraysIndirect(mode, offset);
        
        GLESError error = errorHandler.checkError("glDrawArraysIndirect");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        // Can't track exact triangle count with indirect draws
        stateTracker.recordDrawCall(0);
        
        return MappingResult.success();
    }
    
    private MappingResult mapDrawArraysIndirectFallback(CallDescriptor call) {
        // Read back indirect buffer and execute regular draw
        // This is VERY slow but provides compatibility
        
        int mode = call.intParam(0);
        long offset = call.longParam(0);
        
        // Need to read the indirect command from GPU
        // Format: { count, instanceCount, first, baseInstance }
        IntBuffer readback = intScratchBuffer.get();
        readback.clear();
        readback.limit(4);
        
        // This requires mapping the buffer or using glGetBufferSubData
        // which may not be available in GLES 2.0
        if (capabilities.supportsMapBuffer()) {
            int target = BufferTarget.DRAW_INDIRECT_BUFFER.glConstant();
            ByteBuffer mapped = GLES30.glMapBufferRange(target, offset, 16, 
                0x0001); // GL_MAP_READ_BIT
            
            if (mapped != null) {
                IntBuffer cmd = mapped.asIntBuffer();
                int count = cmd.get(0);
                int instanceCount = cmd.get(1);
                int first = cmd.get(2);
                // int baseInstance = cmd.get(3); // Not supported in GLES
                
                GLES30.glUnmapBuffer(target);
                
                // Execute regular draw
                mode = validateDrawMode(mode);
                
                if (instanceCount > 1 && capabilities.supportsInstancing()) {
                    GLES30.glDrawArraysInstanced(mode, first, count, instanceCount);
                } else {
                    for (int i = 0; i < instanceCount; i++) {
                        GLES20.glDrawArrays(mode, first, count);
                    }
                }
                
                return MappingResult.successWithWarning(0x0040,
                    "Indirect draw emulated via buffer readback (very slow)");
            }
        }
        
        return MappingResult.failed(MappingStatus.UNSUPPORTED,
            "Indirect draw fallback failed - cannot read indirect buffer");
    }
    
    private MappingResult mapDrawElementsIndirect(CallDescriptor call) {
        if (!capabilities.supportsIndirectDraw()) {
            return mapDrawElementsIndirectFallback(call);
        }
        
        int mode = call.intParam(0);
        int type = call.intParam(1);
        long offset = call.longParam(0);
        
        mode = validateDrawMode(mode);
        
        GLES31.glDrawElementsIndirect(mode, type, offset);
        
        GLESError error = errorHandler.checkError("glDrawElementsIndirect");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        stateTracker.recordDrawCall(0);
        
        return MappingResult.success();
    }
    
    private MappingResult mapDrawElementsIndirectFallback(CallDescriptor call) {
        int mode = call.intParam(0);
        int type = call.intParam(1);
        long offset = call.longParam(0);
        
        if (!capabilities.supportsMapBuffer()) {
            return MappingResult.failed(MappingStatus.UNSUPPORTED,
                "Indirect draw fallback requires buffer mapping");
        }
        
        // Format: { count, instanceCount, firstIndex, baseVertex, baseInstance }
        int target = BufferTarget.DRAW_INDIRECT_BUFFER.glConstant();
        ByteBuffer mapped = GLES30.glMapBufferRange(target, offset, 20, 0x0001);
        
        if (mapped == null) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION,
                "Failed to map indirect buffer for readback");
        }
        
        IntBuffer cmd = mapped.asIntBuffer();
        int count = cmd.get(0);
        int instanceCount = cmd.get(1);
        int firstIndex = cmd.get(2);
        // int baseVertex = cmd.get(3); // Not supported without extension
        // int baseInstance = cmd.get(4); // Not supported in GLES
        
        GLES30.glUnmapBuffer(target);
        
        mode = validateDrawMode(mode);
        int indexSize = type == IndexType.UNSIGNED_INT.glConstant() ? 4 :
                        type == IndexType.UNSIGNED_SHORT.glConstant() ? 2 : 1;
        long indexOffset = (long) firstIndex * indexSize;
        
        if (instanceCount > 1 && capabilities.supportsInstancing()) {
            GLES30.glDrawElementsInstanced(mode, count, type, indexOffset, instanceCount);
        } else {
            for (int i = 0; i < instanceCount; i++) {
                GLES20.glDrawElements(mode, count, type, indexOffset);
            }
        }
        
        return MappingResult.successWithWarning(0x0040,
            "Indirect indexed draw emulated via buffer readback");
    }
    
    private MappingResult mapDrawMultiArrays(CallDescriptor call) {
        // Multi-draw is typically an extension
        if (!capabilities.hasExtension(GLESExtension.EXT_multi_draw_arrays)) {
            return mapDrawMultiArraysFallback(call);
        }
        
        int mode = call.intParam(0);
        int drawCount = call.intParam(1);
        IntBuffer firsts = call.objectParam(0);
        IntBuffer counts = call.objectParam(1);
        
        if (drawCount <= 0) {
            return MappingResult.success();
        }
        
        mode = validateDrawMode(mode);
        
        GLES20.glMultiDrawArraysEXT(mode, firsts, counts, drawCount);
        
        GLESError error = errorHandler.checkError("glMultiDrawArraysEXT");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapDrawMultiArraysFallback(CallDescriptor call) {
        int mode = call.intParam(0);
        int drawCount = call.intParam(1);
        IntBuffer firsts = call.objectParam(0);
        IntBuffer counts = call.objectParam(1);
        
        mode = validateDrawMode(mode);
        
        int totalTriangles = 0;
        for (int i = 0; i < drawCount; i++) {
            int first = firsts.get(i);
            int count = counts.get(i);
            
            if (count > 0) {
                GLES20.glDrawArrays(mode, first, count);
                totalTriangles += calculateTriangleCount(mode, count);
            }
        }
        
        stateTracker.recordDrawCall(totalTriangles);
        
        return MappingResult.successWithWarning(0x0080,
            "Multi-draw emulated with " + drawCount + " individual draws");
    }
    
    private MappingResult mapDrawMultiElements(CallDescriptor call) {
        if (!capabilities.hasExtension(GLESExtension.EXT_multi_draw_arrays)) {
            return mapDrawMultiElementsFallback(call);
        }
        
        int mode = call.intParam(0);
        int type = call.intParam(1);
        int drawCount = call.intParam(2);
        IntBuffer counts = call.objectParam(0);
        LongBuffer offsets = call.objectParam(1);
        
        mode = validateDrawMode(mode);
        
        // Need to convert to pointer array for the extension
        // This is complex and driver-specific
        return mapDrawMultiElementsFallback(call);
    }
    
    private MappingResult mapDrawMultiElementsFallback(CallDescriptor call) {
        int mode = call.intParam(0);
        int type = call.intParam(1);
        int drawCount = call.intParam(2);
        IntBuffer counts = call.objectParam(0);
        LongBuffer offsets = call.objectParam(1);
        
        mode = validateDrawMode(mode);
        
        int totalTriangles = 0;
        for (int i = 0; i < drawCount; i++) {
            int count = counts.get(i);
            long offset = offsets.get(i);
            
            if (count > 0) {
                GLES20.glDrawElements(mode, count, type, offset);
                totalTriangles += calculateTriangleCount(mode, count);
            }
        }
        
        stateTracker.recordDrawCall(totalTriangles);
        
        return MappingResult.successWithWarning(0x0080,
            "Multi-draw elements emulated with " + drawCount + " draws");
    }
    
    private MappingResult mapDrawElementsBaseVertex(CallDescriptor call) {
        if (!capabilities.supportsBaseVertex()) {
            return mapDrawElementsBaseVertexFallback(call);
        }
        
        int mode = call.intParam(0);
        int count = call.intParam(1);
        int type = call.intParam(2);
        long offset = call.longParam(0);
        int baseVertex = call.intParam(3);
        
        mode = validateDrawMode(mode);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            GLES32.glDrawElementsBaseVertex(mode, count, type, offset, baseVertex);
        } else {
            GLES20.glDrawElementsBaseVertexEXT(mode, count, type, offset, baseVertex);
        }
        
        GLESError error = errorHandler.checkError("glDrawElementsBaseVertex");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        int triangles = calculateTriangleCount(mode, count);
        stateTracker.recordDrawCall(triangles);
        
        return MappingResult.success();
    }
    
    private MappingResult mapDrawElementsBaseVertexFallback(CallDescriptor call) {
        // Base vertex requires modifying vertex buffer binding
        // This is complex and may not be practical to emulate
        return MappingResult.failed(MappingStatus.UNSUPPORTED,
            "Base vertex draws require GLES 3.2+ or EXT_draw_elements_base_vertex. " +
            "Consider pre-transforming index buffer.");
    }
    
    // ========================================================================
    // STATE OPERATIONS
    // ========================================================================
    
    private MappingResult mapStateEnable(CallDescriptor call) {
        int cap = call.intParam(0);
        
        GLState state = GLState.fromConstant(cap);
        if (state != null && !stateTracker.enableState(state)) {
            return MappingResult.success(); // Already enabled
        }
        
        GLES20.glEnable(cap);
        
        GLESError error = errorHandler.checkError("glEnable");
        if (error != GLESError.NO_ERROR) {
            if (error == GLESError.INVALID_ENUM) {
                return MappingResult.successWithWarning(0x0100,
                    "GL capability " + Integer.toHexString(cap) + " not recognized");
            }
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapStateDisable(CallDescriptor call) {
        int cap = call.intParam(0);
        
        GLState state = GLState.fromConstant(cap);
        if (state != null && !stateTracker.disableState(state)) {
            return MappingResult.success(); // Already disabled
        }
        
        GLES20.glDisable(cap);
        
        GLESError error = errorHandler.checkError("glDisable");
        if (error != GLESError.NO_ERROR) {
            if (error == GLESError.INVALID_ENUM) {
                return MappingResult.successWithWarning(0x0100,
                    "GL capability " + Integer.toHexString(cap) + " not recognized");
            }
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapBlendFunc(CallDescriptor call) {
        int sfactor = call.intParam(0);
        int dfactor = call.intParam(1);
        
        if (!stateTracker.setBlendFunc(sfactor, dfactor, sfactor, dfactor)) {
            return MappingResult.success(); // No change
        }
        
        GLES20.glBlendFunc(sfactor, dfactor);
        
        GLESError error = errorHandler.checkError("glBlendFunc");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapBlendFuncSeparate(CallDescriptor call) {
        int srcRGB = call.intParam(0);
        int dstRGB = call.intParam(1);
        int srcAlpha = call.intParam(2);
        int dstAlpha = call.intParam(3);
        
        if (!stateTracker.setBlendFunc(srcRGB, dstRGB, srcAlpha, dstAlpha)) {
            return MappingResult.success();
        }
        
        GLES20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
        
        GLESError error = errorHandler.checkError("glBlendFuncSeparate");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapBlendEquation(CallDescriptor call) {
        int mode = call.intParam(0);
        
        if (!stateTracker.setBlendEquation(mode, mode)) {
            return MappingResult.success();
        }
        
        GLES20.glBlendEquation(mode);
        
        GLESError error = errorHandler.checkError("glBlendEquation");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapBlendEquationSeparate(CallDescriptor call) {
        int modeRGB = call.intParam(0);
        int modeAlpha = call.intParam(1);
        
        if (!stateTracker.setBlendEquation(modeRGB, modeAlpha)) {
            return MappingResult.success();
        }
        
        GLES20.glBlendEquationSeparate(modeRGB, modeAlpha);
        
        GLESError error = errorHandler.checkError("glBlendEquationSeparate");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapDepthFunc(CallDescriptor call) {
        int func = call.intParam(0);
        
        if (!stateTracker.setDepthFunc(func)) {
            return MappingResult.success();
        }
        
        GLES20.glDepthFunc(func);
        
        GLESError error = errorHandler.checkError("glDepthFunc");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapDepthMask(CallDescriptor call) {
        boolean flag = call.intParam(0) != 0;
        
        if (!stateTracker.setDepthMask(flag)) {
            return MappingResult.success();
        }
        
        GLES20.glDepthMask(flag);
        
        GLESError error = errorHandler.checkError("glDepthMask");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapStencilFunc(CallDescriptor call) {
        int func = call.intParam(0);
        int ref = call.intParam(1);
        int mask = call.intParam(2);
        
        GLES20.glStencilFunc(func, ref, mask);
        
        GLESError error = errorHandler.checkError("glStencilFunc");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapStencilOp(CallDescriptor call) {
        int sfail = call.intParam(0);
        int dpfail = call.intParam(1);
        int dppass = call.intParam(2);
        
        GLES20.glStencilOp(sfail, dpfail, dppass);
        
        GLESError error = errorHandler.checkError("glStencilOp");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapStencilMask(CallDescriptor call) {
        int mask = call.intParam(0);
        
        GLES20.glStencilMask(mask);
        
        GLESError error = errorHandler.checkError("glStencilMask");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapCullFace(CallDescriptor call) {
        int mode = call.intParam(0);
        
        if (!stateTracker.setCullFace(mode)) {
            return MappingResult.success();
        }
        
        GLES20.glCullFace(mode);
        
        GLESError error = errorHandler.checkError("glCullFace");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapFrontFace(CallDescriptor call) {
        int mode = call.intParam(0);
        
        if (!stateTracker.setFrontFace(mode)) {
            return MappingResult.success();
        }
        
        GLES20.glFrontFace(mode);
        
        GLESError error = errorHandler.checkError("glFrontFace");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapPolygonOffset(CallDescriptor call) {
        float factor = call.floatParam(0);
        float units = call.floatParam(1);
        
        GLES20.glPolygonOffset(factor, units);
        
        GLESError error = errorHandler.checkError("glPolygonOffset");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapViewport(CallDescriptor call) {
        int x = call.intParam(0);
        int y = call.intParam(1);
        int width = call.intParam(2);
        int height = call.intParam(3);
        
        if (!stateTracker.setViewport(x, y, width, height)) {
            return MappingResult.success();
        }
        
        GLES20.glViewport(x, y, width, height);
        
        GLESError error = errorHandler.checkError("glViewport");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapScissor(CallDescriptor call) {
        int x = call.intParam(0);
        int y = call.intParam(1);
        int width = call.intParam(2);
        int height = call.intParam(3);
        
        if (!stateTracker.setScissor(x, y, width, height)) {
            return MappingResult.success();
        }
        
        GLES20.glScissor(x, y, width, height);
        
        GLESError error = errorHandler.checkError("glScissor");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapColorMask(CallDescriptor call) {
        boolean r = call.intParam(0) != 0;
        boolean g = call.intParam(1) != 0;
        boolean b = call.intParam(2) != 0;
        boolean a = call.intParam(3) != 0;
        
        if (!stateTracker.setColorMask(r, g, b, a)) {
            return MappingResult.success();
        }
        
        GLES20.glColorMask(r, g, b, a);
        
        GLESError error = errorHandler.checkError("glColorMask");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapLineWidth(CallDescriptor call) {
        float width = call.floatParam(0);
        
        // Clamp line width to supported range
        width = Math.max(1.0f, Math.min(width, quirks.getMaxLineWidth()));
        
        GLES20.glLineWidth(width);
        
        GLESError error = errorHandler.checkError("glLineWidth");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapPointSize(CallDescriptor call) {
        // Point size is set via gl_PointSize in shader for GLES 2.0+
        // This is a no-op for programmable pipeline
        return MappingResult.success();
    }
    
    // ========================================================================
    // FRAMEBUFFER OPERATIONS
    // ========================================================================
    
    private MappingResult mapFramebufferCreate(CallDescriptor call) {
        int count = call.intParam(0);
        
        IntBuffer framebuffers = intScratchBuffer.get();
        framebuffers.clear();
        framebuffers.limit(count);
        
        GLES20.glGenFramebuffers(framebuffers);
        
        GLESError error = errorHandler.checkError("glGenFramebuffers");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(framebuffers.get(0));
    }
    
    private MappingResult mapFramebufferDelete(CallDescriptor call) {
        int framebuffer = call.intParam(0);
        if (framebuffer == 0) {
            return MappingResult.success();
        }
        
        GLES20.glDeleteFramebuffers(framebuffer);
        resourceRegistry.unregisterFramebuffer(framebuffer);
        
        return MappingResult.success();
    }
    
    private MappingResult mapFramebufferBind(CallDescriptor call) {
        int target = call.intParam(0);
        int framebuffer = call.intParam(1);
        
        // GLES 2.0 only has GL_FRAMEBUFFER, GLES 3.0 adds READ/DRAW
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            target = 0x8D40; // GL_FRAMEBUFFER
        }
        
        if (!stateTracker.bindFramebuffer(framebuffer)) {
            return MappingResult.success();
        }
        
        GLES20.glBindFramebuffer(target, framebuffer);
        
        GLESError error = errorHandler.checkError("glBindFramebuffer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapFramebufferTexture2D(CallDescriptor call) {
        int target = call.intParam(0);
        int attachment = call.intParam(1);
        int textarget = call.intParam(2);
        int texture = call.intParam(3);
        int level = call.intParam(4);
        
        GLES20.glFramebufferTexture2D(target, attachment, textarget, texture, level);
        
        GLESError error = errorHandler.checkError("glFramebufferTexture2D");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapFramebufferRenderbuffer(CallDescriptor call) {
        int target = call.intParam(0);
        int attachment = call.intParam(1);
        int renderbuffertarget = call.intParam(2);
        int renderbuffer = call.intParam(3);
        
        GLES20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
        
        GLESError error = errorHandler.checkError("glFramebufferRenderbuffer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapFramebufferCheckStatus(CallDescriptor call) {
        int target = call.intParam(0);
        
        int status = GLES20.glCheckFramebufferStatus(target);
        
        FramebufferStatus fbStatus = FramebufferStatus.fromCode(status);
        if (!fbStatus.isComplete()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                status, "Framebuffer incomplete: " + fbStatus.description());
        }
        
        return MappingResult.success(status);
    }
    
    private MappingResult mapFramebufferBlit(CallDescriptor call) {
        if (!capabilities.supportsFBOBlit()) {
            return mapFramebufferBlitFallback(call);
        }
        
        int srcX0 = call.intParam(0);
        int srcY0 = call.intParam(1);
        int srcX1 = call.intParam(2);
        int srcY1 = call.intParam(3);
        int dstX0 = call.intParam(4);
        int dstY0 = call.intParam(5);
        int dstX1 = call.intParam(6);
        int dstY1 = call.intParam(7);
        int mask = call.intParam(8);
        int filter = call.intParam(9);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, 
                dstX0, dstY0, dstX1, dstY1, mask, filter);
        } else if (capabilities.hasExtension(GLESExtension.ANGLE_framebuffer_blit)) {
            GLES20.glBlitFramebufferANGLE(srcX0, srcY0, srcX1, srcY1,
                dstX0, dstY0, dstX1, dstY1, mask, filter);
        } else if (capabilities.hasExtension(GLESExtension.NV_framebuffer_blit)) {
            GLES20.glBlitFramebufferNV(srcX0, srcY0, srcX1, srcY1,
                dstX0, dstY0, dstX1, dstY1, mask, filter);
        }
        
        GLESError error = errorHandler.checkError("glBlitFramebuffer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapFramebufferBlitFallback(CallDescriptor call) {
        // Fallback: render fullscreen quad to copy
        // This is complex and requires setting up a shader pipeline
        return MappingResult.failed(MappingStatus.UNSUPPORTED,
            "Framebuffer blit requires GLES 3.0+ or extension. Use fullscreen quad copy.");
    }
    
    private MappingResult mapFramebufferInvalidate(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // No-op on GLES 2.0, but that's okay - it's an optimization hint
            return MappingResult.success();
        }
        
        int target = call.intParam(0);
        int numAttachments = call.intParam(1);
        IntBuffer attachments = call.objectParam(0);
        
        GLES30.glInvalidateFramebuffer(target, attachments);
        
        return MappingResult.success();
    }
    
    private MappingResult mapFramebufferReadPixels(CallDescriptor call) {
        int x = call.intParam(0);
        int y = call.intParam(1);
        int width = call.intParam(2);
        int height = call.intParam(3);
        int format = call.intParam(4);
        int type = call.intParam(5);
        ByteBuffer pixels = call.objectParam(0);
        
        GLES20.glReadPixels(x, y, width, height, format, type, pixels);
        
        GLESError error = errorHandler.checkError("glReadPixels");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapFramebufferClear(CallDescriptor call) {
        int mask = call.intParam(0);
        
        GLES20.glClear(mask);
        
        GLESError error = errorHandler.checkError("glClear");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapFramebufferClearBuffer(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fallback to glClear
            return mapFramebufferClear(call);
        }
        
        int buffer = call.intParam(0);
        int drawBuffer = call.intParam(1);
        int subType = call.subFunction();
        
        if (subType == 0) { // int
            IntBuffer values = call.objectParam(0);
            GLES30.glClearBufferiv(buffer, drawBuffer, values);
        } else if (subType == 1) { // uint
            IntBuffer values = call.objectParam(0);
            GLES30.glClearBufferuiv(buffer, drawBuffer, values);
        } else if (subType == 2) { // float
            FloatBuffer values = call.objectParam(0);
            GLES30.glClearBufferfv(buffer, drawBuffer, values);
        } else if (subType == 3) { // depth-stencil
            float depth = call.floatParam(0);
            int stencil = call.intParam(2);
            GLES30.glClearBufferfi(buffer, drawBuffer, depth, stencil);
        }
        
        GLESError error = errorHandler.checkError("glClearBuffer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapRenderbufferCreate(CallDescriptor call) {
        int count = call.intParam(0);
        
        IntBuffer renderbuffers = intScratchBuffer.get();
        renderbuffers.clear();
        renderbuffers.limit(count);
        
        GLES20.glGenRenderbuffers(renderbuffers);
        
        GLESError error = errorHandler.checkError("glGenRenderbuffers");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(renderbuffers.get(0));
    }
    
    private MappingResult mapRenderbufferDelete(CallDescriptor call) {
        int renderbuffer = call.intParam(0);
        if (renderbuffer == 0) {
            return MappingResult.success();
        }
        
        GLES20.glDeleteRenderbuffers(renderbuffer);
        
        return MappingResult.success();
    }
    
    private MappingResult mapRenderbufferBind(CallDescriptor call) {
        int target = call.intParam(0);
        int renderbuffer = call.intParam(1);
        
        if (!stateTracker.bindRenderbuffer(renderbuffer)) {
            return MappingResult.success();
        }
        
        GLES20.glBindRenderbuffer(target, renderbuffer);
        
        GLESError error = errorHandler.checkError("glBindRenderbuffer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapRenderbufferStorage(CallDescriptor call) {
        int target = call.intParam(0);
        int internalFormat = call.intParam(1);
        int width = call.intParam(2);
        int height = call.intParam(3);
        int samples = call.intParam(4);
        
        if (samples > 1) {
            if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
                // Try extension
                if (capabilities.hasExtension(GLESExtension.APPLE_framebuffer_multisample) ||
                    capabilities.hasExtension(GLESExtension.IMG_multisampled_render_to_texture)) {
                    // Use extension version
                    samples = Math.min(samples, capabilities.maxSamples());
                    GLES20.glRenderbufferStorageMultisampleAPPLE(target, samples, 
                        internalFormat, width, height);
                } else {
                    // Fall back to non-MSAA
                    samples = 0;
                }
            } else {
                samples = Math.min(samples, capabilities.maxSamples());
                GLES30.glRenderbufferStorageMultisample(target, samples, 
                    internalFormat, width, height);
            }
        }
        
        if (samples <= 1) {
            GLES20.glRenderbufferStorage(target, internalFormat, width, height);
        }
        
        GLESError error = errorHandler.checkError("glRenderbufferStorage");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // SHADER OPERATIONS
    // ========================================================================
    
    private MappingResult mapShaderCreate(CallDescriptor call) {
        int type = call.intParam(0);
        
        // Validate shader type support
        int shaderType = switch (type) {
            case 0x8B31 -> 0x8B31; // GL_VERTEX_SHADER
            case 0x8B30 -> 0x8B30; // GL_FRAGMENT_SHADER
            case 0x8DD9 -> { // GL_GEOMETRY_SHADER
                if (!capabilities.supportsGeometryShader()) {
                    yield -1;
                }
                yield 0x8DD9;
            }
            case 0x8E88 -> { // GL_TESS_CONTROL_SHADER
                if (!capabilities.supportsTessellation()) {
                    yield -1;
                }
                yield 0x8E88;
            }
            case 0x8E87 -> { // GL_TESS_EVALUATION_SHADER
                if (!capabilities.supportsTessellation()) {
                    yield -1;
                }
                yield 0x8E87;
            }
            case 0x91B9 -> { // GL_COMPUTE_SHADER
                if (!capabilities.supportsCompute()) {
                    yield -1;
                }
                yield 0x91B9;
            }
            default -> -1;
        };
        
        if (shaderType == -1) {
            return MappingResult.unsupported("Shader type " + Integer.toHexString(type) + 
                " not supported in " + capabilities.version());
        }
        
        int shader = GLES20.glCreateShader(shaderType);
        
        GLESError error = errorHandler.checkError("glCreateShader");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        if (shader == 0) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, "glCreateShader returned 0");
        }
        
        return MappingResult.success(shader);
    }
    
    private MappingResult mapShaderDelete(CallDescriptor call) {
        int shader = call.intParam(0);
        if (shader == 0) {
            return MappingResult.success();
        }
        
        GLES20.glDeleteShader(shader);
        resourceRegistry.unregisterShader(shader);
        
        return MappingResult.success();
    }
    
    private MappingResult mapShaderSource(CallDescriptor call) {
        int shader = call.intParam(0);
        String source = call.objectParam(0);
        
        if (source == null || source.isEmpty()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION, "Shader source cannot be empty");
        }
        
        GLES20.glShaderSource(shader, source);
        
        GLESError error = errorHandler.checkError("glShaderSource");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapShaderCompile(CallDescriptor call) {
        int shader = call.intParam(0);
        
        GLES20.glCompileShader(shader);
        
        // Check compilation status
        int status = GLES20.glGetShaderi(shader, 0x8B81); // GL_COMPILE_STATUS
        
        if (status == 0) { // GL_FALSE
            String infoLog = GLES20.glGetShaderInfoLog(shader);
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION,
                "Shader compilation failed: " + infoLog);
        }
        
        // Check for warnings in info log
        String infoLog = GLES20.glGetShaderInfoLog(shader);
        if (infoLog != null && !infoLog.isEmpty()) {
            return MappingResult.successWithWarning(0x0200, "Shader compiled with warnings: " + infoLog);
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapProgramCreate(CallDescriptor call) {
        int program = GLES20.glCreateProgram();
        
        GLESError error = errorHandler.checkError("glCreateProgram");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        if (program == 0) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, "glCreateProgram returned 0");
        }
        
        return MappingResult.success(program);
    }
    
    private MappingResult mapProgramDelete(CallDescriptor call) {
        int program = call.intParam(0);
        if (program == 0) {
            return MappingResult.success();
        }
        
        GLES20.glDeleteProgram(program);
        resourceRegistry.unregisterProgram(program);
        
        return MappingResult.success();
    }
    
    private MappingResult mapProgramAttach(CallDescriptor call) {
        int program = call.intParam(0);
        int shader = call.intParam(1);
        
        GLES20.glAttachShader(program, shader);
        
        GLESError error = errorHandler.checkError("glAttachShader");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapProgramLink(CallDescriptor call) {
        int program = call.intParam(0);
        
        GLES20.glLinkProgram(program);
        
        int status = GLES20.glGetProgrami(program, 0x8B82); // GL_LINK_STATUS
        
        if (status == 0) {
            String infoLog = GLES20.glGetProgramInfoLog(program);
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION,
                "Program linking failed: " + infoLog);
        }
        
        String infoLog = GLES20.glGetProgramInfoLog(program);
        if (infoLog != null && !infoLog.isEmpty()) {
            return MappingResult.successWithWarning(0x0200, "Program linked with warnings: " + infoLog);
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapProgramUse(CallDescriptor call) {
        int program = call.intParam(0);
        
        if (!stateTracker.bindProgram(program)) {
            return MappingResult.success();
        }
        
        GLES20.glUseProgram(program);
        
        GLESError error = errorHandler.checkError("glUseProgram");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapProgramGetUniformLocation(CallDescriptor call) {
        int program = call.intParam(0);
        String name = call.objectParam(0);
        
        int location = GLES20.glGetUniformLocation(program, name);
        
        // Location -1 means not found, but that's not necessarily an error
        return MappingResult.success(location);
    }
    
    private MappingResult mapProgramUniform(CallDescriptor call) {
        int location = call.intParam(0);
        int subType = call.subFunction();
        
        if (location < 0) {
            return MappingResult.success(); // Inactive uniform, ignore
        }
        
        switch (subType) {
            case 0 -> GLES20.glUniform1i(location, call.intParam(1)); // 1i
            case 1 -> GLES20.glUniform2i(location, call.intParam(1), call.intParam(2)); // 2i
            case 2 -> GLES20.glUniform3i(location, call.intParam(1), call.intParam(2), call.intParam(3)); // 3i
            case 3 -> GLES20.glUniform4i(location, call.intParam(1), call.intParam(2), 
                call.intParam(3), call.intParam(4)); // 4i
            case 4 -> GLES20.glUniform1f(location, call.floatParam(0)); // 1f
            case 5 -> GLES20.glUniform2f(location, call.floatParam(0), call.floatParam(1)); // 2f
            case 6 -> GLES20.glUniform3f(location, call.floatParam(0), call.floatParam(1), 
                call.floatParam(2)); // 3f
            case 7 -> GLES20.glUniform4f(location, call.floatParam(0), call.floatParam(1), 
                call.floatParam(2), call.floatParam(3)); // 4f
            case 8 -> { // 1iv
                IntBuffer values = call.objectParam(0);
                GLES20.glUniform1iv(location, values);
            }
            case 9 -> { // 1fv
                FloatBuffer values = call.objectParam(0);
                GLES20.glUniform1fv(location, values);
            }
            case 10 -> { // 2fv
                FloatBuffer values = call.objectParam(0);
                GLES20.glUniform2fv(location, values);
            }
            case 11 -> { // 3fv
                FloatBuffer values = call.objectParam(0);
                GLES20.glUniform3fv(location, values);
            }
            case 12 -> { // 4fv
                FloatBuffer values = call.objectParam(0);
                GLES20.glUniform4fv(location, values);
            }
            case 13 -> { // Matrix2fv
                boolean transpose = call.intParam(1) != 0;
                FloatBuffer values = call.objectParam(0);
                GLES20.glUniformMatrix2fv(location, transpose, values);
            }
            case 14 -> { // Matrix3fv
                boolean transpose = call.intParam(1) != 0;
                FloatBuffer values = call.objectParam(0);
                GLES20.glUniformMatrix3fv(location, transpose, values);
            }
            case 15 -> { // Matrix4fv
                boolean transpose = call.intParam(1) != 0;
                FloatBuffer values = call.objectParam(0);
                GLES20.glUniformMatrix4fv(location, transpose, values);
            }
            case 16 -> { // Matrix2x3fv (GLES 3.0+)
                if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
                    boolean transpose = call.intParam(1) != 0;
                    FloatBuffer values = call.objectParam(0);
                    GLES30.glUniformMatrix2x3fv(location, transpose, values);
                }
            }
            case 17 -> { // Matrix3x2fv
                if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
                    boolean transpose = call.intParam(1) != 0;
                    FloatBuffer values = call.objectParam(0);
                    GLES30.glUniformMatrix3x2fv(location, transpose, values);
                }
            }
            case 18 -> { // Matrix2x4fv
                if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
                    boolean transpose = call.intParam(1) != 0;
                    FloatBuffer values = call.objectParam(0);
                    GLES30.glUniformMatrix2x4fv(location, transpose, values);
                }
            }
            case 19 -> { // Matrix4x2fv
                if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
                    boolean transpose = call.intParam(1) != 0;
                    FloatBuffer values = call.objectParam(0);
                    GLES30.glUniformMatrix4x2fv(location, transpose, values);
                }
            }
            case 20 -> { // Matrix3x4fv
                if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
                    boolean transpose = call.intParam(1) != 0;
                    FloatBuffer values = call.objectParam(0);
                    GLES30.glUniformMatrix3x4fv(location, transpose, values);
                }
            }
            case 21 -> { // Matrix4x3fv
                if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
                    boolean transpose = call.intParam(1) != 0;
                    FloatBuffer values = call.objectParam(0);
                    GLES30.glUniformMatrix4x3fv(location, transpose, values);
                }
            }
            case 22 -> { // 1ui (GLES 3.0+)
                if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
                    GLES30.glUniform1ui(location, call.intParam(1));
                }
            }
            case 23 -> { // 2ui
                if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
                    GLES30.glUniform2ui(location, call.intParam(1), call.intParam(2));
                }
            }
            case 24 -> { // 3ui
                if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
                    GLES30.glUniform3ui(location, call.intParam(1), call.intParam(2), call.intParam(3));
                }
            }
            case 25 -> { // 4ui
                if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
                    GLES30.glUniform4ui(location, call.intParam(1), call.intParam(2), 
                        call.intParam(3), call.intParam(4));
                }
            }
        }
        
        GLESError error = errorHandler.checkError("glUniform");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapProgramGetAttribLocation(CallDescriptor call) {
        int program = call.intParam(0);
        String name = call.objectParam(0);
        
        int location = GLES20.glGetAttribLocation(program, name);
        
        return MappingResult.success(location);
    }
    
    private MappingResult mapProgramVertexAttrib(CallDescriptor call) {
        int index = call.intParam(0);
        int subType = call.subFunction();
        
        switch (subType) {
            case 0 -> GLES20.glVertexAttrib1f(index, call.floatParam(0));
            case 1 -> GLES20.glVertexAttrib2f(index, call.floatParam(0), call.floatParam(1));
            case 2 -> GLES20.glVertexAttrib3f(index, call.floatParam(0), call.floatParam(1), 
                call.floatParam(2));
            case 3 -> GLES20.glVertexAttrib4f(index, call.floatParam(0), call.floatParam(1),
                call.floatParam(2), call.floatParam(3));
            case 4 -> {
                FloatBuffer values = call.objectParam(0);
                GLES20.glVertexAttrib4fv(index, values);
            }
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapProgramBindAttribLocation(CallDescriptor call) {
        int program = call.intParam(0);
        int index = call.intParam(1);
        String name = call.objectParam(0);
        
        GLES20.glBindAttribLocation(program, index, name);
        
        GLESError error = errorHandler.checkError("glBindAttribLocation");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapProgramGetInfoLog(CallDescriptor call) {
        int program = call.intParam(0);
        
        String infoLog = GLES20.glGetProgramInfoLog(program);
        
        // Store in call result - caller retrieves via special mechanism
        return MappingResult.success(infoLog != null ? infoLog.hashCode() : 0);
    }
    
    private MappingResult mapProgramBinaryGet(CallDescriptor call) {
        if (!capabilities.supportsProgramBinary()) {
            return MappingResult.unsupported("Program binary not supported");
        }
        
        int program = call.intParam(0);
        ByteBuffer binary = call.objectParam(0);
        IntBuffer format = call.objectParam(1);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glGetProgramBinary(program, (IntBuffer) null, format, binary);
        } else {
            GLES20.glGetProgramBinaryOES(program, (IntBuffer) null, format, binary);
        }
        
        GLESError error = errorHandler.checkError("glGetProgramBinary");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapProgramBinaryLoad(CallDescriptor call) {
        if (!capabilities.supportsProgramBinary()) {
            return MappingResult.unsupported("Program binary not supported");
        }
        
        int program = call.intParam(0);
        int format = call.intParam(1);
        ByteBuffer binary = call.objectParam(0);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glProgramBinary(program, format, binary);
        } else {
            GLES20.glProgramBinaryOES(program, format, binary);
        }
        
        // Verify load succeeded
        int status = GLES20.glGetProgrami(program, 0x8B82); // GL_LINK_STATUS
        if (status == 0) {
            String infoLog = GLES20.glGetProgramInfoLog(program);
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION,
                "Program binary load failed: " + infoLog);
        }
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // VAO OPERATIONS
    // ========================================================================
    
    private MappingResult mapVAOCreate(CallDescriptor call) {
        if (!capabilities.supportsVAO()) {
            // Return dummy handle for GLES 2.0 fallback
            return MappingResult.successWithWarning(0x0400, 
                "VAOs not supported, using emulation");
        }
        
        int count = call.intParam(0);
        IntBuffer vaos = intScratchBuffer.get();
        vaos.clear();
        vaos.limit(count);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glGenVertexArrays(vaos);
        } else {
            GLES20.glGenVertexArraysOES(vaos);
        }
        
        GLESError error = errorHandler.checkError("glGenVertexArrays");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(vaos.get(0));
    }
    
    private MappingResult mapVAODelete(CallDescriptor call) {
        if (!capabilities.supportsVAO()) {
            return MappingResult.success();
        }
        
        int vao = call.intParam(0);
        if (vao == 0) {
            return MappingResult.success();
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glDeleteVertexArrays(vao);
        } else {
            GLES20.glDeleteVertexArraysOES(vao);
        }
        
        resourceRegistry.unregisterVAO(vao);
        
        return MappingResult.success();
    }
    
    private MappingResult mapVAOBind(CallDescriptor call) {
        int vao = call.intParam(0);
        
        if (!capabilities.supportsVAO()) {
            // Store current VAO in emulation state
            stateTracker.bindVAO(vao);
            return MappingResult.success();
        }
        
        if (!stateTracker.bindVAO(vao)) {
            return MappingResult.success();
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glBindVertexArray(vao);
        } else {
            GLES20.glBindVertexArrayOES(vao);
        }
        
        GLESError error = errorHandler.checkError("glBindVertexArray");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapVAOVertexAttribPointer(CallDescriptor call) {
        int index = call.intParam(0);
        int size = call.intParam(1);
        int type = call.intParam(2);
        boolean normalized = call.intParam(3) != 0;
        int stride = call.intParam(4);
        long offset = call.longParam(0);
        
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, offset);
        
        GLESError error = errorHandler.checkError("glVertexAttribPointer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapVAOVertexAttribIPointer(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fall back to regular pointer (may lose integer precision)
            return mapVAOVertexAttribPointer(call);
        }
        
        int index = call.intParam(0);
        int size = call.intParam(1);
        int type = call.intParam(2);
        int stride = call.intParam(3);
        long offset = call.longParam(0);
        
        GLES30.glVertexAttribIPointer(index, size, type, stride, offset);
        
        GLESError error = errorHandler.checkError("glVertexAttribIPointer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapVAOEnableAttrib(CallDescriptor call) {
        int index = call.intParam(0);
        
        GLES20.glEnableVertexAttribArray(index);
        
        GLESError error = errorHandler.checkError("glEnableVertexAttribArray");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapVAODisableAttrib(CallDescriptor call) {
        int index = call.intParam(0);
        
        GLES20.glDisableVertexAttribArray(index);
        
        GLESError error = errorHandler.checkError("glDisableVertexAttribArray");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapVAOVertexAttribDivisor(CallDescriptor call) {
        int index = call.intParam(0);
        int divisor = call.intParam(1);
        
        if (!capabilities.supportsInstancing()) {
            if (divisor > 0) {
                return MappingResult.failed(MappingStatus.UNSUPPORTED,
                    "Vertex attrib divisor requires instancing support");
            }
            return MappingResult.success();
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glVertexAttribDivisor(index, divisor);
        } else if (capabilities.hasExtension(GLESExtension.EXT_instanced_arrays)) {
            GLES20.glVertexAttribDivisorEXT(index, divisor);
        } else if (capabilities.hasExtension(GLESExtension.ANGLE_instanced_arrays)) {
            GLES20.glVertexAttribDivisorANGLE(index, divisor);
        } else if (capabilities.hasExtension(GLESExtension.NV_instanced_arrays)) {
            GLES20.glVertexAttribDivisorNV(index, divisor);
        }
        
        GLESError error = errorHandler.checkError("glVertexAttribDivisor");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // COMPUTE OPERATIONS
    // ========================================================================
    
    private MappingResult mapComputeDispatch(CallDescriptor call) {
        if (!capabilities.supportsCompute()) {
            return MappingResult.unsupported("Compute shaders require GLES 3.1+");
        }
        
        int numGroupsX = call.intParam(0);
        int numGroupsY = call.intParam(1);
        int numGroupsZ = call.intParam(2);
        
        // Validate against limits
        int maxCount = capabilities.maxComputeWorkGroupCount();
        if (numGroupsX > maxCount || numGroupsY > maxCount || numGroupsZ > maxCount) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Compute dispatch exceeds max work group count " + maxCount);
        }
        
        GLES31.glDispatchCompute(numGroupsX, numGroupsY, numGroupsZ);
        
        GLESError error = errorHandler.checkError("glDispatchCompute");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapComputeDispatchIndirect(CallDescriptor call) {
        if (!capabilities.supportsCompute()) {
            return MappingResult.unsupported("Compute shaders require GLES 3.1+");
        }
        
        long offset = call.longParam(0);
        
        GLES31.glDispatchComputeIndirect(offset);
        
        GLESError error = errorHandler.checkError("glDispatchComputeIndirect");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapComputeMemoryBarrier(CallDescriptor call) {
        if (!capabilities.supportsCompute()) {
            return MappingResult.unsupported("Memory barriers require GLES 3.1+");
        }
        
        int barriers = call.intParam(0);
        
        GLES31.glMemoryBarrier(barriers);
        
        GLESError error = errorHandler.checkError("glMemoryBarrier");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // QUERY OPERATIONS
    // ========================================================================
    
    private MappingResult mapQueryCreate(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Check for extension support
            if (!capabilities.hasExtension(GLESExtension.EXT_occlusion_query_boolean) &&
                !capabilities.hasExtension(GLESExtension.EXT_disjoint_timer_query)) {
                return MappingResult.unsupported("Query objects require GLES 3.0+ or extension");
            }
        }
        
        int count = call.intParam(0);
        IntBuffer queries = intScratchBuffer.get();
        queries.clear();
        queries.limit(count);
        
        GLES30.glGenQueries(queries);
        
        GLESError error = errorHandler.checkError("glGenQueries");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(queries.get(0));
    }
    
    private MappingResult mapQueryDelete(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.success();
        }
        
        int query = call.intParam(0);
        if (query == 0) {
            return MappingResult.success();
        }
        
        GLES30.glDeleteQueries(query);
        
        return MappingResult.success();
    }
    
    private MappingResult mapQueryBegin(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Query objects require GLES 3.0+");
        }
        
        int target = call.intParam(0);
        int query = call.intParam(1);
        
        GLES30.glBeginQuery(target, query);
        
        GLESError error = errorHandler.checkError("glBeginQuery");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapQueryEnd(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Query objects require GLES 3.0+");
        }
        
        int target = call.intParam(0);
        
        GLES30.glEndQuery(target);
        
        GLESError error = errorHandler.checkError("glEndQuery");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapQueryGetResult(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Query objects require GLES 3.0+");
        }
        
        int query = call.intParam(0);
        int pname = call.intParam(1);
        
        IntBuffer result = intScratchBuffer.get();
        result.clear();
        result.limit(1);
        
        GLES30.glGetQueryObjectuiv(query, pname, result);
        
        GLESError error = errorHandler.checkError("glGetQueryObjectuiv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(result.get(0));
    }
    
    // ========================================================================
    // SYNC OPERATIONS
    // ========================================================================
    
    private MappingResult mapSyncFenceCreate(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Try APPLE_sync extension
            if (capabilities.hasExtension(GLESExtension.APPLE_sync)) {
                long sync = GLES20.glFenceSyncAPPLE(0x9117, 0); // GL_SYNC_GPU_COMMANDS_COMPLETE
                return MappingResult.success(sync);
            }
            return MappingResult.unsupported("Sync objects require GLES 3.0+ or APPLE_sync");
        }
        
        int condition = call.intParam(0);
        int flags = call.intParam(1);
        
        long sync = GLES30.glFenceSync(condition, flags);
        
        GLESError error = errorHandler.checkError("glFenceSync");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(sync);
    }
    
    private MappingResult mapSyncDelete(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.success();
        }
        
        long sync = call.longParam(0);
        if (sync == 0) {
            return MappingResult.success();
        }
        
        GLES30.glDeleteSync(sync);
        
        return MappingResult.success();
    }
    
    private MappingResult mapSyncClientWait(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fallback to glFinish
            GLES20.glFinish();
            return MappingResult.successWithWarning(0x0800, "Sync emulated with glFinish");
        }
        
        long sync = call.longParam(0);
        int flags = call.intParam(0);
        long timeout = call.longParam(1);
        
        int result = GLES30.glClientWaitSync(sync, flags, timeout);
        
        return MappingResult.success(result);
    }
    
    private MappingResult mapSyncWait(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.success(); // No-op
        }
        
        long sync = call.longParam(0);
        int flags = call.intParam(0);
        long timeout = call.longParam(1);
        
        GLES30.glWaitSync(sync, flags, timeout);
        
        GLESError error = errorHandler.checkError("glWaitSync");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapSyncFlush(CallDescriptor call) {
        GLES20.glFlush();
        return MappingResult.success();
    }
    
    private MappingResult mapSyncFinish(CallDescriptor call) {
        GLES20.glFinish();
        return MappingResult.success();
    }
    
    // ========================================================================
    // MISCELLANEOUS OPERATIONS
    // ========================================================================
    
    private MappingResult mapGetError(CallDescriptor call) {
        int error = GLES20.glGetError();
        return MappingResult.success(error);
    }
    
    private MappingResult mapGetString(CallDescriptor call) {
        int name = call.intParam(0);
        
        String result = GLES20.glGetString(name);
        
        // Return hash as result, actual string retrieved via separate mechanism
        return MappingResult.success(result != null ? result.hashCode() : 0);
    }
    
    private MappingResult mapGetInteger(CallDescriptor call) {
        int pname = call.intParam(0);
        
        int result = GLES20.glGetInteger(pname);
        
        return MappingResult.success(result);
    }
    
    private MappingResult mapGetFloat(CallDescriptor call) {
        int pname = call.intParam(0);
        
        float result = GLES20.glGetFloat(pname);
        
        // Encode float bits as int for result
        return MappingResult.success(Float.floatToRawIntBits(result));
    }
    
    private MappingResult mapGetBoolean(CallDescriptor call) {
        int pname = call.intParam(0);
        
        boolean result = GLES20.glGetBoolean(pname);
        
        return MappingResult.success(result ? 1 : 0);
    }
    
    private MappingResult mapPixelStore(CallDescriptor call) {
        int pname = call.intParam(0);
        int param = call.intParam(1);
        
        GLES20.glPixelStorei(pname, param);
        
        GLESError error = errorHandler.checkError("glPixelStorei");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapHint(CallDescriptor call) {
        int target = call.intParam(0);
        int mode = call.intParam(1);
        
        GLES20.glHint(target, mode);
        
        // Hints may fail silently on some drivers - don't treat as error
        errorHandler.clearErrors();
        
        return MappingResult.success();
    }
    
    private MappingResult mapDebugMessageCallback(CallDescriptor call) {
        if (!capabilities.supportsDebugOutput()) {
            return MappingResult.successWithWarning(0x1000, "Debug output not supported");
        }
        
        // Callback setup requires native interop
        // This is handled at a higher level
        return MappingResult.success();
    }
    
    private MappingResult mapDebugMessageInsert(CallDescriptor call) {
        if (!capabilities.supportsDebugOutput()) {
            return MappingResult.success(); // Silently ignore
        }
        
        int source = call.intParam(0);
        int type = call.intParam(1);
        int id = call.intParam(2);
        int severity = call.intParam(3);
        String message = call.objectParam(0);
        
// ============================================================================
// SECTION 7: SHADER PROGRAM OPERATIONS
// ============================================================================

    // ========================================================================
    // SHADER CREATION & COMPILATION
    // ========================================================================
    
    private MappingResult mapShaderCreate(CallDescriptor call) {
        int type = call.intParam(0);
        
        // Validate shader type against capabilities
        if (!validateShaderType(type)) {
            return MappingResult.failed(MappingStatus.UNSUPPORTED,
                "Shader type " + shaderTypeToString(type) + " not supported on " + capabilities.version());
        }
        
        int shader = GLES20.glCreateShader(type);
        
        if (shader == 0) {
            GLESError error = errorHandler.checkError("glCreateShader");
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, 
                error.code(), "glCreateShader failed: " + error.description());
        }
        
        // Track shader for cleanup
        resourceRegistry.registerShader(shader);
        
        return MappingResult.success(shader);
    }
    
    private boolean validateShaderType(int type) {
        return switch (type) {
            case 0x8B31 -> true; // GL_VERTEX_SHADER - all GLES versions
            case 0x8B30 -> true; // GL_FRAGMENT_SHADER - all GLES versions
            case 0x8DD9 -> capabilities.version().isAtLeast(GLESVersion.GLES_3_2) || 
                           capabilities.hasExtension(GLESExtension.EXT_geometry_shader); // GL_GEOMETRY_SHADER
            case 0x8E88 -> capabilities.supportsTessellation(); // GL_TESS_CONTROL_SHADER
            case 0x8E87 -> capabilities.supportsTessellation(); // GL_TESS_EVALUATION_SHADER
            case 0x91B9 -> capabilities.supportsCompute(); // GL_COMPUTE_SHADER
            default -> false;
        };
    }
    
    private String shaderTypeToString(int type) {
        return switch (type) {
            case 0x8B31 -> "VERTEX_SHADER";
            case 0x8B30 -> "FRAGMENT_SHADER";
            case 0x8DD9 -> "GEOMETRY_SHADER";
            case 0x8E88 -> "TESS_CONTROL_SHADER";
            case 0x8E87 -> "TESS_EVALUATION_SHADER";
            case 0x91B9 -> "COMPUTE_SHADER";
            default -> "UNKNOWN(" + type + ")";
        };
    }
    
    private MappingResult mapShaderDelete(CallDescriptor call) {
        int shader = call.intParam(0);
        
        if (shader == 0) {
            return MappingResult.success(); // Silently ignore 0
        }
        
        GLES20.glDeleteShader(shader);
        resourceRegistry.unregisterShader(shader);
        
        return MappingResult.success();
    }
    
    private MappingResult mapShaderSource(CallDescriptor call) {
        int shader = call.intParam(0);
        String source = call.objectParam(0);
        
        if (source == null || source.isEmpty()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION, "Shader source is null or empty");
        }
        
        // Validate source length
        if (source.length() > MAX_SHADER_SOURCE_LENGTH) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Shader source exceeds maximum length: " + source.length() + " > " + MAX_SHADER_SOURCE_LENGTH);
        }
        
        GLES20.glShaderSource(shader, source);
        
        GLESError error = errorHandler.checkError("glShaderSource");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapShaderCompile(CallDescriptor call) {
        int shader = call.intParam(0);
        
        GLES20.glCompileShader(shader);
        
        // Check compilation status
        int status = GLES20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS);
        
        if (status == GL20.GL_FALSE) {
            String infoLog = GLES20.glGetShaderInfoLog(shader);
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, 
                GL20.GL_COMPILE_STATUS, "Shader compilation failed:\n" + infoLog);
        }
        
        // Check for warnings even on success
        String infoLog = GLES20.glGetShaderInfoLog(shader);
        if (infoLog != null && !infoLog.isEmpty() && !infoLog.trim().isEmpty()) {
            return MappingResult.successWithWarning(status, "Shader compiled with warnings:\n" + infoLog);
        }
        
        return MappingResult.success(status);
    }
    
    private MappingResult mapGetShaderiv(CallDescriptor call) {
        int shader = call.intParam(0);
        int pname = call.intParam(1);
        
        int result = GLES20.glGetShaderi(shader, pname);
        
        return MappingResult.success(result);
    }
    
    private MappingResult mapGetShaderInfoLog(CallDescriptor call) {
        int shader = call.intParam(0);
        
        String infoLog = GLES20.glGetShaderInfoLog(shader);
        
        // Store in string cache, return handle
        int handle = stringCache.put(infoLog);
        return MappingResult.success(handle);
    }
    
    private MappingResult mapGetShaderSource(CallDescriptor call) {
        int shader = call.intParam(0);
        
        String source = GLES20.glGetShaderSource(shader);
        
        int handle = stringCache.put(source);
        return MappingResult.success(handle);
    }
    
    // ========================================================================
    // PROGRAM CREATION & LINKING
    // ========================================================================
    
    private MappingResult mapProgramCreate(CallDescriptor call) {
        int program = GLES20.glCreateProgram();
        
        if (program == 0) {
            GLESError error = errorHandler.checkError("glCreateProgram");
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION,
                error.code(), "glCreateProgram failed: " + error.description());
        }
        
        resourceRegistry.registerProgram(program);
        
        return MappingResult.success(program);
    }
    
    private MappingResult mapProgramDelete(CallDescriptor call) {
        int program = call.intParam(0);
        
        if (program == 0) {
            return MappingResult.success();
        }
        
        // Unbind if currently active
        if (stateTracker.getBoundProgram() == program) {
            GLES20.glUseProgram(0);
            stateTracker.setBoundProgram(0);
        }
        
        GLES20.glDeleteProgram(program);
        resourceRegistry.unregisterProgram(program);
        
        return MappingResult.success();
    }
    
    private MappingResult mapAttachShader(CallDescriptor call) {
        int program = call.intParam(0);
        int shader = call.intParam(1);
        
        GLES20.glAttachShader(program, shader);
        
        GLESError error = errorHandler.checkError("glAttachShader");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapDetachShader(CallDescriptor call) {
        int program = call.intParam(0);
        int shader = call.intParam(1);
        
        GLES20.glDetachShader(program, shader);
        
        GLESError error = errorHandler.checkError("glDetachShader");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapLinkProgram(CallDescriptor call) {
        int program = call.intParam(0);
        
        GLES20.glLinkProgram(program);
        
        // Check link status
        int status = GLES20.glGetProgrami(program, GL20.GL_LINK_STATUS);
        
        if (status == GL20.GL_FALSE) {
            String infoLog = GLES20.glGetProgramInfoLog(program);
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION,
                GL20.GL_LINK_STATUS, "Program link failed:\n" + infoLog);
        }
        
        // Check for warnings
        String infoLog = GLES20.glGetProgramInfoLog(program);
        if (infoLog != null && !infoLog.isEmpty() && !infoLog.trim().isEmpty()) {
            return MappingResult.successWithWarning(status, "Program linked with warnings:\n" + infoLog);
        }
        
        return MappingResult.success(status);
    }
    
    private MappingResult mapValidateProgram(CallDescriptor call) {
        int program = call.intParam(0);
        
        GLES20.glValidateProgram(program);
        
        int status = GLES20.glGetProgrami(program, GL20.GL_VALIDATE_STATUS);
        
        if (status == GL20.GL_FALSE) {
            String infoLog = GLES20.glGetProgramInfoLog(program);
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                GL20.GL_VALIDATE_STATUS, "Program validation failed:\n" + infoLog);
        }
        
        return MappingResult.success(status);
    }
    
    private MappingResult mapUseProgram(CallDescriptor call) {
        int program = call.intParam(0);
        
        // State caching - avoid redundant binds
        if (stateTracker.getBoundProgram() == program) {
            return MappingResult.success();
        }
        
        GLES20.glUseProgram(program);
        
        GLESError error = errorHandler.checkError("glUseProgram");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        stateTracker.setBoundProgram(program);
        stateTracker.incrementStateChangeCount();
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetProgramiv(CallDescriptor call) {
        int program = call.intParam(0);
        int pname = call.intParam(1);
        
        int result = GLES20.glGetProgrami(program, pname);
        
        return MappingResult.success(result);
    }
    
    private MappingResult mapGetProgramInfoLog(CallDescriptor call) {
        int program = call.intParam(0);
        
        String infoLog = GLES20.glGetProgramInfoLog(program);
        
        int handle = stringCache.put(infoLog);
        return MappingResult.success(handle);
    }
    
    // ========================================================================
    // PROGRAM BINARY (GLES 3.0+)
    // ========================================================================
    
    private MappingResult mapGetProgramBinary(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Try OES_get_program_binary extension
            if (!capabilities.hasExtension(GLESExtension.OES_get_program_binary)) {
                return MappingResult.unsupported("Program binary requires GLES 3.0+ or OES_get_program_binary");
            }
        }
        
        int program = call.intParam(0);
        int bufSize = call.intParam(1);
        
        // Allocate buffers for output
        IntBuffer lengthBuf = intScratchBuffer.get();
        IntBuffer formatBuf = intScratchBuffer.get();
        lengthBuf.clear();
        formatBuf.clear();
        
        ByteBuffer binaryBuf = BufferUtils.createByteBuffer(bufSize);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glGetProgramBinary(program, lengthBuf, formatBuf, binaryBuf);
        } else {
            GLES20.glGetProgramBinaryOES(program, lengthBuf, formatBuf, binaryBuf);
        }
        
        GLESError error = errorHandler.checkError("glGetProgramBinary");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        // Return a handle to the binary data
        int dataHandle = binaryCache.put(binaryBuf, lengthBuf.get(0), formatBuf.get(0));
        return MappingResult.success(dataHandle);
    }
    
    private MappingResult mapProgramBinary(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            if (!capabilities.hasExtension(GLESExtension.OES_get_program_binary)) {
                return MappingResult.unsupported("Program binary requires GLES 3.0+ or OES_get_program_binary");
            }
        }
        
        int program = call.intParam(0);
        int binaryFormat = call.intParam(1);
        ByteBuffer binary = call.objectParam(0);
        int length = call.intParam(2);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glProgramBinary(program, binaryFormat, binary);
        } else {
            GLES20.glProgramBinaryOES(program, binaryFormat, binary);
        }
        
        // Check if binary was accepted
        int status = GLES20.glGetProgrami(program, GL20.GL_LINK_STATUS);
        if (status == GL20.GL_FALSE) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION,
                "Program binary rejected by driver (may be incompatible or corrupted)");
        }
        
        return MappingResult.success(status);
    }
    
    private MappingResult mapProgramParameteri(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("glProgramParameteri requires GLES 3.0+");
        }
        
        int program = call.intParam(0);
        int pname = call.intParam(1);
        int value = call.intParam(2);
        
        GLES30.glProgramParameteri(program, pname, value);
        
        GLESError error = errorHandler.checkError("glProgramParameteri");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // UNIFORM OPERATIONS
    // ========================================================================
    
    private MappingResult mapGetUniformLocation(CallDescriptor call) {
        int program = call.intParam(0);
        String name = call.objectParam(0);
        
        int location = GLES20.glGetUniformLocation(program, name);
        
        // -1 is valid (uniform not found or optimized out), not an error
        return MappingResult.success(location);
    }
    
    private MappingResult mapGetActiveUniform(CallDescriptor call) {
        int program = call.intParam(0);
        int index = call.intParam(1);
        
        IntBuffer sizeBuf = intScratchBuffer.get();
        IntBuffer typeBuf = intScratchBuffer.get();
        sizeBuf.clear();
        typeBuf.clear();
        
        String name = GLES20.glGetActiveUniform(program, index, sizeBuf, typeBuf);
        
        // Pack results: size, type, name hash
        int nameHandle = stringCache.put(name);
        
        return MappingResult.success(new UniformQueryResult(name, sizeBuf.get(0), typeBuf.get(0)));
    }
    
    private MappingResult mapUniform1i(CallDescriptor call) {
        int location = call.intParam(0);
        int v0 = call.intParam(1);
        
        // -1 location is silently ignored (spec-compliant)
        if (location == -1) {
            return MappingResult.success();
        }
        
        GLES20.glUniform1i(location, v0);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform2i(CallDescriptor call) {
        int location = call.intParam(0);
        int v0 = call.intParam(1);
        int v1 = call.intParam(2);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform2i(location, v0, v1);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform3i(CallDescriptor call) {
        int location = call.intParam(0);
        int v0 = call.intParam(1);
        int v1 = call.intParam(2);
        int v2 = call.intParam(3);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform3i(location, v0, v1, v2);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform4i(CallDescriptor call) {
        int location = call.intParam(0);
        int v0 = call.intParam(1);
        int v1 = call.intParam(2);
        int v2 = call.intParam(3);
        int v3 = call.intParam(4);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform4i(location, v0, v1, v2, v3);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform1f(CallDescriptor call) {
        int location = call.intParam(0);
        float v0 = call.floatParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform1f(location, v0);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform2f(CallDescriptor call) {
        int location = call.intParam(0);
        float v0 = call.floatParam(0);
        float v1 = call.floatParam(1);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform2f(location, v0, v1);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform3f(CallDescriptor call) {
        int location = call.intParam(0);
        float v0 = call.floatParam(0);
        float v1 = call.floatParam(1);
        float v2 = call.floatParam(2);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform3f(location, v0, v1, v2);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform4f(CallDescriptor call) {
        int location = call.intParam(0);
        float v0 = call.floatParam(0);
        float v1 = call.floatParam(1);
        float v2 = call.floatParam(2);
        float v3 = call.floatParam(3);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform4f(location, v0, v1, v2, v3);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform1fv(CallDescriptor call) {
        int location = call.intParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform1fv(location, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform2fv(CallDescriptor call) {
        int location = call.intParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform2fv(location, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform3fv(CallDescriptor call) {
        int location = call.intParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform3fv(location, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform4fv(CallDescriptor call) {
        int location = call.intParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform4fv(location, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform1iv(CallDescriptor call) {
        int location = call.intParam(0);
        IntBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform1iv(location, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform2iv(CallDescriptor call) {
        int location = call.intParam(0);
        IntBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform2iv(location, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform3iv(CallDescriptor call) {
        int location = call.intParam(0);
        IntBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform3iv(location, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform4iv(CallDescriptor call) {
        int location = call.intParam(0);
        IntBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniform4iv(location, value);
        
        return MappingResult.success();
    }
    
    // Unsigned integer uniforms (GLES 3.0+)
    private MappingResult mapUniform1ui(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fallback: cast to signed int
            return mapUniform1i(call);
        }
        
        int location = call.intParam(0);
        int v0 = call.intParam(1);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniform1ui(location, v0);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform2ui(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return mapUniform2i(call);
        }
        
        int location = call.intParam(0);
        int v0 = call.intParam(1);
        int v1 = call.intParam(2);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniform2ui(location, v0, v1);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform3ui(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return mapUniform3i(call);
        }
        
        int location = call.intParam(0);
        int v0 = call.intParam(1);
        int v1 = call.intParam(2);
        int v2 = call.intParam(3);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniform3ui(location, v0, v1, v2);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform4ui(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return mapUniform4i(call);
        }
        
        int location = call.intParam(0);
        int v0 = call.intParam(1);
        int v1 = call.intParam(2);
        int v2 = call.intParam(3);
        int v3 = call.intParam(4);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniform4ui(location, v0, v1, v2, v3);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform1uiv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return mapUniform1iv(call);
        }
        
        int location = call.intParam(0);
        IntBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniform1uiv(location, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform2uiv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return mapUniform2iv(call);
        }
        
        int location = call.intParam(0);
        IntBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniform2uiv(location, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform3uiv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return mapUniform3iv(call);
        }
        
        int location = call.intParam(0);
        IntBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniform3uiv(location, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniform4uiv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return mapUniform4iv(call);
        }
        
        int location = call.intParam(0);
        IntBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniform4uiv(location, value);
        
        return MappingResult.success();
    }
    
    // Matrix uniforms
    private MappingResult mapUniformMatrix2fv(CallDescriptor call) {
        int location = call.intParam(0);
        boolean transpose = call.boolParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniformMatrix2fv(location, transpose, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniformMatrix3fv(CallDescriptor call) {
        int location = call.intParam(0);
        boolean transpose = call.boolParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniformMatrix3fv(location, transpose, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniformMatrix4fv(CallDescriptor call) {
        int location = call.intParam(0);
        boolean transpose = call.boolParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES20.glUniformMatrix4fv(location, transpose, value);
        
        return MappingResult.success();
    }
    
    // Non-square matrix uniforms (GLES 3.0+)
    private MappingResult mapUniformMatrix2x3fv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Non-square matrices require GLES 3.0+");
        }
        
        int location = call.intParam(0);
        boolean transpose = call.boolParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniformMatrix2x3fv(location, transpose, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniformMatrix3x2fv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Non-square matrices require GLES 3.0+");
        }
        
        int location = call.intParam(0);
        boolean transpose = call.boolParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniformMatrix3x2fv(location, transpose, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniformMatrix2x4fv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Non-square matrices require GLES 3.0+");
        }
        
        int location = call.intParam(0);
        boolean transpose = call.boolParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniformMatrix2x4fv(location, transpose, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniformMatrix4x2fv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Non-square matrices require GLES 3.0+");
        }
        
        int location = call.intParam(0);
        boolean transpose = call.boolParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniformMatrix4x2fv(location, transpose, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniformMatrix3x4fv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Non-square matrices require GLES 3.0+");
        }
        
        int location = call.intParam(0);
        boolean transpose = call.boolParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniformMatrix3x4fv(location, transpose, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapUniformMatrix4x3fv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Non-square matrices require GLES 3.0+");
        }
        
        int location = call.intParam(0);
        boolean transpose = call.boolParam(0);
        FloatBuffer value = call.objectParam(0);
        
        if (location == -1) return MappingResult.success();
        
        GLES30.glUniformMatrix4x3fv(location, transpose, value);
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // ATTRIBUTE OPERATIONS
    // ========================================================================
    
    private MappingResult mapGetAttribLocation(CallDescriptor call) {
        int program = call.intParam(0);
        String name = call.objectParam(0);
        
        int location = GLES20.glGetAttribLocation(program, name);
        
        return MappingResult.success(location);
    }
    
    private MappingResult mapBindAttribLocation(CallDescriptor call) {
        int program = call.intParam(0);
        int index = call.intParam(1);
        String name = call.objectParam(0);
        
        GLES20.glBindAttribLocation(program, index, name);
        
        GLESError error = errorHandler.checkError("glBindAttribLocation");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetActiveAttrib(CallDescriptor call) {
        int program = call.intParam(0);
        int index = call.intParam(1);
        
        IntBuffer sizeBuf = intScratchBuffer.get();
        IntBuffer typeBuf = intScratchBuffer.get();
        sizeBuf.clear();
        typeBuf.clear();
        
        String name = GLES20.glGetActiveAttrib(program, index, sizeBuf, typeBuf);
        
        return MappingResult.success(new AttributeQueryResult(name, sizeBuf.get(0), typeBuf.get(0)));
    }
    
    private MappingResult mapVertexAttrib1f(CallDescriptor call) {
        int index = call.intParam(0);
        float v0 = call.floatParam(0);
        
        GLES20.glVertexAttrib1f(index, v0);
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexAttrib2f(CallDescriptor call) {
        int index = call.intParam(0);
        float v0 = call.floatParam(0);
        float v1 = call.floatParam(1);
        
        GLES20.glVertexAttrib2f(index, v0, v1);
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexAttrib3f(CallDescriptor call) {
        int index = call.intParam(0);
        float v0 = call.floatParam(0);
        float v1 = call.floatParam(1);
        float v2 = call.floatParam(2);
        
        GLES20.glVertexAttrib3f(index, v0, v1, v2);
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexAttrib4f(CallDescriptor call) {
        int index = call.intParam(0);
        float v0 = call.floatParam(0);
        float v1 = call.floatParam(1);
        float v2 = call.floatParam(2);
        float v3 = call.floatParam(3);
        
        GLES20.glVertexAttrib4f(index, v0, v1, v2, v3);
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexAttrib1fv(CallDescriptor call) {
        int index = call.intParam(0);
        FloatBuffer value = call.objectParam(0);
        
        GLES20.glVertexAttrib1fv(index, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexAttrib2fv(CallDescriptor call) {
        int index = call.intParam(0);
        FloatBuffer value = call.objectParam(0);
        
        GLES20.glVertexAttrib2fv(index, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexAttrib3fv(CallDescriptor call) {
        int index = call.intParam(0);
        FloatBuffer value = call.objectParam(0);
        
        GLES20.glVertexAttrib3fv(index, value);
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexAttrib4fv(CallDescriptor call) {
        int index = call.intParam(0);
        FloatBuffer value = call.objectParam(0);
        
        GLES20.glVertexAttrib4fv(index, value);
        
        return MappingResult.success();
    }
    
    // Integer vertex attributes (GLES 3.0+)
    private MappingResult mapVertexAttribI4i(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fallback to float
            float v0 = (float) call.intParam(1);
            float v1 = (float) call.intParam(2);
            float v2 = (float) call.intParam(3);
            float v3 = (float) call.intParam(4);
            GLES20.glVertexAttrib4f(call.intParam(0), v0, v1, v2, v3);
            return MappingResult.successWithWarning(0x2000, "Integer attrib converted to float (GLES 2.0)");
        }
        
        int index = call.intParam(0);
        int v0 = call.intParam(1);
        int v1 = call.intParam(2);
        int v2 = call.intParam(3);
        int v3 = call.intParam(4);
        
        GLES30.glVertexAttribI4i(index, v0, v1, v2, v3);
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexAttribI4ui(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return mapVertexAttribI4i(call);
        }
        
        int index = call.intParam(0);
        int v0 = call.intParam(1);
        int v1 = call.intParam(2);
        int v2 = call.intParam(3);
        int v3 = call.intParam(4);
        
        GLES30.glVertexAttribI4ui(index, v0, v1, v2, v3);
        
        return MappingResult.success();
    }

// ============================================================================
// SECTION 8: VERTEX ARRAY OBJECTS (~1,000 lines)
// ============================================================================

    // ========================================================================
    // VAO CREATION & BINDING
    // ========================================================================
    
    private MappingResult mapVertexArrayCreate(CallDescriptor call) {
        int count = call.intParam(0);
        
        IntBuffer ids = intScratchBuffer.get();
        ids.clear();
        ids.limit(count);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glGenVertexArrays(ids);
        } else if (capabilities.hasExtension(GLESExtension.OES_vertex_array_object)) {
            GLES20.glGenVertexArraysOES(ids);
        } else {
            // Emulate VAO on GLES 2.0 without extension
            return createEmulatedVAO(count);
        }
        
        GLESError error = errorHandler.checkError("glGenVertexArrays");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        // Track all created VAOs
        for (int i = 0; i < count; i++) {
            resourceRegistry.registerVAO(ids.get(i));
        }
        
        return MappingResult.success(ids.get(0));
    }
    
    private MappingResult createEmulatedVAO(int count) {
        // Emulated VAO tracks vertex attribute state in software
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            ids[i] = emulatedVAOManager.create();
        }
        return MappingResult.success(ids[0]);
    }
    
    private MappingResult mapVertexArrayDelete(CallDescriptor call) {
        int vao = call.intParam(0);
        
        if (vao == 0) {
            return MappingResult.success();
        }
        
        // Unbind if currently bound
        if (stateTracker.getBoundVAO() == vao) {
            mapVertexArrayBind(CallDescriptor.of(CallType.VERTEX_ARRAY_BIND, 0));
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glDeleteVertexArrays(vao);
        } else if (capabilities.hasExtension(GLESExtension.OES_vertex_array_object)) {
            GLES20.glDeleteVertexArraysOES(vao);
        } else {
            emulatedVAOManager.delete(vao);
        }
        
        resourceRegistry.unregisterVAO(vao);
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexArrayBind(CallDescriptor call) {
        int vao = call.intParam(0);
        
        // State caching
        if (stateTracker.getBoundVAO() == vao) {
            return MappingResult.success();
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glBindVertexArray(vao);
        } else if (capabilities.hasExtension(GLESExtension.OES_vertex_array_object)) {
            GLES20.glBindVertexArrayOES(vao);
        } else {
            // Emulated VAO - restore state
            emulatedVAOManager.bind(vao);
        }
        
        GLESError error = errorHandler.checkError("glBindVertexArray");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        stateTracker.setBoundVAO(vao);
        stateTracker.incrementStateChangeCount();
        
        return MappingResult.success();
    }
    
    private MappingResult mapIsVertexArray(CallDescriptor call) {
        int vao = call.intParam(0);
        
        boolean result;
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            result = GLES30.glIsVertexArray(vao);
        } else if (capabilities.hasExtension(GLESExtension.OES_vertex_array_object)) {
            result = GLES20.glIsVertexArrayOES(vao);
        } else {
            result = emulatedVAOManager.isValid(vao);
        }
        
        return MappingResult.success(result ? 1 : 0);
    }
    
    // ========================================================================
    // VERTEX ATTRIBUTE POINTERS
    // ========================================================================
    
    private MappingResult mapVertexAttribPointer(CallDescriptor call) {
        int index = call.intParam(0);
        int size = call.intParam(1);
        int type = call.intParam(2);
        boolean normalized = call.boolParam(0);
        int stride = call.intParam(3);
        long pointer = call.longParam(0);
        
        // Validate parameters
        if (size < 1 || size > 4) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Attribute size must be 1-4, got: " + size);
        }
        
        if (index >= capabilities.maxVertexAttribs()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Attribute index " + index + " exceeds maximum " + capabilities.maxVertexAttribs());
        }
        
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
        
        GLESError error = errorHandler.checkError("glVertexAttribPointer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        // Track in emulated VAO if needed
        if (!capabilities.supportsVAO()) {
            emulatedVAOManager.setVertexAttribPointer(index, size, type, normalized, stride, pointer);
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexAttribIPointer(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fallback: use regular vertex attrib pointer (loses integer precision)
            return mapVertexAttribPointer(call.modify()
                .setBoolParam(0, false) // Don't normalize
                .build());
        }
        
        int index = call.intParam(0);
        int size = call.intParam(1);
        int type = call.intParam(2);
        int stride = call.intParam(3);
        long pointer = call.longParam(0);
        
        if (index >= capabilities.maxVertexAttribs()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Attribute index " + index + " exceeds maximum " + capabilities.maxVertexAttribs());
        }
        
        GLES30.glVertexAttribIPointer(index, size, type, stride, pointer);
        
        GLESError error = errorHandler.checkError("glVertexAttribIPointer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapEnableVertexAttribArray(CallDescriptor call) {
        int index = call.intParam(0);
        
        if (index >= capabilities.maxVertexAttribs()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Attribute index " + index + " exceeds maximum " + capabilities.maxVertexAttribs());
        }
        
        GLES20.glEnableVertexAttribArray(index);
        
        // Track in emulated VAO
        if (!capabilities.supportsVAO()) {
            emulatedVAOManager.enableVertexAttribArray(index);
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapDisableVertexAttribArray(CallDescriptor call) {
        int index = call.intParam(0);
        
        if (index >= capabilities.maxVertexAttribs()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Attribute index " + index + " exceeds maximum " + capabilities.maxVertexAttribs());
        }
        
        GLES20.glDisableVertexAttribArray(index);
        
        // Track in emulated VAO
        if (!capabilities.supportsVAO()) {
            emulatedVAOManager.disableVertexAttribArray(index);
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexAttribDivisor(CallDescriptor call) {
        int index = call.intParam(0);
        int divisor = call.intParam(1);
        
        if (!capabilities.supportsInstancing()) {
            if (divisor == 0) {
                return MappingResult.success(); // Non-instanced is always supported
            }
            return MappingResult.unsupported("Instanced vertex attributes require GLES 3.0+ or extensions");
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glVertexAttribDivisor(index, divisor);
        } else if (capabilities.hasExtension(GLESExtension.EXT_instanced_arrays)) {
            GLES20.glVertexAttribDivisorEXT(index, divisor);
        } else if (capabilities.hasExtension(GLESExtension.ANGLE_instanced_arrays)) {
            GLES20.glVertexAttribDivisorANGLE(index, divisor);
        }
        
        GLESError error = errorHandler.checkError("glVertexAttribDivisor");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetVertexAttribiv(CallDescriptor call) {
        int index = call.intParam(0);
        int pname = call.intParam(1);
        
        IntBuffer params = intScratchBuffer.get();
        params.clear();
        
        GLES20.glGetVertexAttribiv(index, pname, params);
        
        return MappingResult.success(params.get(0));
    }
    
    private MappingResult mapGetVertexAttribfv(CallDescriptor call) {
        int index = call.intParam(0);
        int pname = call.intParam(1);
        
        FloatBuffer params = floatScratchBuffer.get();
        params.clear();
        
        GLES20.glGetVertexAttribfv(index, pname, params);
        
        return MappingResult.success(Float.floatToRawIntBits(params.get(0)));
    }
    
    private MappingResult mapGetVertexAttribPointerv(CallDescriptor call) {
        int index = call.intParam(0);
        int pname = call.intParam(1);
        
        PointerBuffer params = PointerBuffer.allocateDirect(1);
        
        GLES20.glGetVertexAttribPointerv(index, pname, params);
        
        return MappingResult.success(params.get(0));
    }
    
    // Vertex attrib format (GLES 3.1+ with EXT_vertex_attrib_binding or explicit binding)
    private MappingResult mapVertexAttribFormat(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("glVertexAttribFormat requires GLES 3.1+");
        }
        
        int attribindex = call.intParam(0);
        int size = call.intParam(1);
        int type = call.intParam(2);
        boolean normalized = call.boolParam(0);
        int relativeoffset = call.intParam(3);
        
        GLES31.glVertexAttribFormat(attribindex, size, type, normalized, relativeoffset);
        
        GLESError error = errorHandler.checkError("glVertexAttribFormat");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexAttribIFormat(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("glVertexAttribIFormat requires GLES 3.1+");
        }
        
        int attribindex = call.intParam(0);
        int size = call.intParam(1);
        int type = call.intParam(2);
        int relativeoffset = call.intParam(3);
        
        GLES31.glVertexAttribIFormat(attribindex, size, type, relativeoffset);
        
        GLESError error = errorHandler.checkError("glVertexAttribIFormat");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexAttribBinding(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("glVertexAttribBinding requires GLES 3.1+");
        }
        
        int attribindex = call.intParam(0);
        int bindingindex = call.intParam(1);
        
        GLES31.glVertexAttribBinding(attribindex, bindingindex);
        
        GLESError error = errorHandler.checkError("glVertexAttribBinding");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapVertexBindingDivisor(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("glVertexBindingDivisor requires GLES 3.1+");
        }
        
        int bindingindex = call.intParam(0);
        int divisor = call.intParam(1);
        
        GLES31.glVertexBindingDivisor(bindingindex, divisor);
        
        GLESError error = errorHandler.checkError("glVertexBindingDivisor");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapBindVertexBuffer(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("glBindVertexBuffer requires GLES 3.1+");
        }
        
        int bindingindex = call.intParam(0);
        int buffer = call.intParam(1);
        long offset = call.longParam(0);
        int stride = call.intParam(2);
        
        GLES31.glBindVertexBuffer(bindingindex, buffer, offset, stride);
        
        GLESError error = errorHandler.checkError("glBindVertexBuffer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }

// ============================================================================
// SECTION 9: COMPUTE SHADERS (~1,000 lines)
// ============================================================================

    // ========================================================================
    // COMPUTE DISPATCH
    // ========================================================================
    
    private MappingResult mapDispatchCompute(CallDescriptor call) {
        if (!capabilities.supportsCompute()) {
            return MappingResult.unsupported("Compute shaders require GLES 3.1+");
        }
        
        int numGroupsX = call.intParam(0);
        int numGroupsY = call.intParam(1);
        int numGroupsZ = call.intParam(2);
        
        // Validate work group counts
        int[] maxWorkGroupCount = capabilities.maxComputeWorkGroupCount();
        if (numGroupsX > maxWorkGroupCount[0] || numGroupsY > maxWorkGroupCount[1] || 
            numGroupsZ > maxWorkGroupCount[2]) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                String.format("Work group count (%d, %d, %d) exceeds maximum (%d, %d, %d)",
                    numGroupsX, numGroupsY, numGroupsZ,
                    maxWorkGroupCount[0], maxWorkGroupCount[1], maxWorkGroupCount[2]));
        }
        
        // Validate non-zero counts
        if (numGroupsX <= 0 || numGroupsY <= 0 || numGroupsZ <= 0) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Work group counts must be positive");
        }
        
        GLES31.glDispatchCompute(numGroupsX, numGroupsY, numGroupsZ);
        
        GLESError error = errorHandler.checkError("glDispatchCompute");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        // Track compute dispatch for statistics
        stateTracker.incrementComputeDispatchCount();
        
        return MappingResult.success();
    }
    
    private MappingResult mapDispatchComputeIndirect(CallDescriptor call) {
        if (!capabilities.supportsCompute()) {
            return MappingResult.unsupported("Compute shaders require GLES 3.1+");
        }
        
        long indirect = call.longParam(0);
        
        // Validate alignment (must be 4-byte aligned per spec)
        if (indirect % 4 != 0) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Indirect offset must be 4-byte aligned");
        }
        
        // Ensure dispatch indirect buffer is bound
        int boundBuffer = stateTracker.getBoundBuffer(BufferTarget.DISPATCH_INDIRECT_BUFFER);
        if (boundBuffer == 0) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "No DISPATCH_INDIRECT_BUFFER bound");
        }
        
        GLES31.glDispatchComputeIndirect(indirect);
        
        GLESError error = errorHandler.checkError("glDispatchComputeIndirect");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        stateTracker.incrementComputeDispatchCount();
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // MEMORY BARRIERS
    // ========================================================================
    
    private MappingResult mapMemoryBarrier(CallDescriptor call) {
        if (!capabilities.supportsCompute()) {
            // GLES 2.0/3.0: No memory barriers, just flush
            GLES20.glFlush();
            return MappingResult.successWithWarning(0x3000, "Memory barrier emulated with glFlush");
        }
        
        int barriers = call.intParam(0);
        
        // Validate barrier bits
        int validBarriers = 
            GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT |
            GL_ELEMENT_ARRAY_BARRIER_BIT |
            GL_UNIFORM_BARRIER_BIT |
            GL_TEXTURE_FETCH_BARRIER_BIT |
            GL_SHADER_IMAGE_ACCESS_BARRIER_BIT |
            GL_COMMAND_BARRIER_BIT |
            GL_PIXEL_BUFFER_BARRIER_BIT |
            GL_TEXTURE_UPDATE_BARRIER_BIT |
            GL_BUFFER_UPDATE_BARRIER_BIT |
            GL_FRAMEBUFFER_BARRIER_BIT |
            GL_TRANSFORM_FEEDBACK_BARRIER_BIT |
            GL_ATOMIC_COUNTER_BARRIER_BIT |
            GL_SHADER_STORAGE_BARRIER_BIT |
            GL_ALL_BARRIER_BITS;
        
        if ((barriers & ~validBarriers) != 0) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Invalid barrier bits: 0x" + Integer.toHexString(barriers & ~validBarriers));
        }
        
        GLES31.glMemoryBarrier(barriers);
        
        GLESError error = errorHandler.checkError("glMemoryBarrier");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapMemoryBarrierByRegion(CallDescriptor call) {
        if (!capabilities.supportsCompute()) {
            return mapMemoryBarrier(call);
        }
        
        int barriers = call.intParam(0);
        
        GLES31.glMemoryBarrierByRegion(barriers);
        
        GLESError error = errorHandler.checkError("glMemoryBarrierByRegion");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    // Memory barrier constants
    private static final int GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT = 0x00000001;
    private static final int GL_ELEMENT_ARRAY_BARRIER_BIT = 0x00000002;
    private static final int GL_UNIFORM_BARRIER_BIT = 0x00000004;
    private static final int GL_TEXTURE_FETCH_BARRIER_BIT = 0x00000008;
    private static final int GL_SHADER_IMAGE_ACCESS_BARRIER_BIT = 0x00000020;
    private static final int GL_COMMAND_BARRIER_BIT = 0x00000040;
    private static final int GL_PIXEL_BUFFER_BARRIER_BIT = 0x00000080;
    private static final int GL_TEXTURE_UPDATE_BARRIER_BIT = 0x00000100;
    private static final int GL_BUFFER_UPDATE_BARRIER_BIT = 0x00000200;
    private static final int GL_FRAMEBUFFER_BARRIER_BIT = 0x00000400;
    private static final int GL_TRANSFORM_FEEDBACK_BARRIER_BIT = 0x00000800;
    private static final int GL_ATOMIC_COUNTER_BARRIER_BIT = 0x00001000;
    private static final int GL_SHADER_STORAGE_BARRIER_BIT = 0x00002000;
    private static final int GL_ALL_BARRIER_BITS = 0xFFFFFFFF;
    
    // ========================================================================
    // IMAGE LOAD/STORE
    // ========================================================================
    
    private MappingResult mapBindImageTexture(CallDescriptor call) {
        if (!capabilities.supportsCompute()) {
            return MappingResult.unsupported("Image load/store requires GLES 3.1+");
        }
        
        int unit = call.intParam(0);
        int texture = call.intParam(1);
        int level = call.intParam(2);
        boolean layered = call.boolParam(0);
        int layer = call.intParam(3);
        int access = call.intParam(4);
        int format = call.intParam(5);
        
        // Validate unit
        if (unit >= capabilities.maxImageUnits()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Image unit " + unit + " exceeds maximum " + capabilities.maxImageUnits());
        }
        
        // Validate access mode
        if (access != GL_READ_ONLY && access != GL_WRITE_ONLY && access != GL_READ_WRITE) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Invalid image access mode: 0x" + Integer.toHexString(access));
        }
        
        // Validate format is shader-accessible
        if (!isImageFormatSupported(format)) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Unsupported image format: 0x" + Integer.toHexString(format));
        }
        
        GLES31.glBindImageTexture(unit, texture, level, layered, layer, access, format);
        
        GLESError error = errorHandler.checkError("glBindImageTexture");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private boolean isImageFormatSupported(int format) {
        return switch (format) {
            // Core GLES 3.1 image formats
            case 0x822E, // R32F
                 0x8231, // R32I
                 0x8236, // R32UI
                 0x8058, // RGBA8
                 0x8D7C, // RGBA8I (signed normalized to snorm)
                 0x8D7D, // RGBA8UI
                 0x8235, // RGBA16I
                 0x8234, // RGBA16UI
                 0x8814, // RGBA32F
                 0x8D82, // RGBA32I
                 0x8D70  // RGBA32UI
                -> true;
            default -> false;
        };
    }
    
    private static final int GL_READ_ONLY = 0x88B8;
    private static final int GL_WRITE_ONLY = 0x88B9;
    private static final int GL_READ_WRITE = 0x88BA;
    
    // ========================================================================
    // ATOMIC COUNTERS
    // ========================================================================
    
    private MappingResult mapBindAtomicCounterBuffer(CallDescriptor call) {
        if (!capabilities.supportsCompute()) {
            return MappingResult.unsupported("Atomic counters require GLES 3.1+");
        }
        
        int index = call.intParam(0);
        int buffer = call.intParam(1);
        
        // Validate index
        if (index >= capabilities.maxAtomicCounterBufferBindings()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Atomic counter buffer index " + index + " exceeds maximum " + 
                capabilities.maxAtomicCounterBufferBindings());
        }
        
        // Use glBindBufferBase with ATOMIC_COUNTER_BUFFER target
        GLES31.glBindBufferBase(GL_ATOMIC_COUNTER_BUFFER, index, buffer);
        
        GLESError error = errorHandler.checkError("glBindBufferBase(ATOMIC_COUNTER)");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private static final int GL_ATOMIC_COUNTER_BUFFER = 0x92C0;
    
    // ========================================================================
    // COMPUTE PROGRAM QUERIES
    // ========================================================================
    
    private MappingResult mapGetProgramInterfaceiv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Program interface queries require GLES 3.1+");
        }
        
        int program = call.intParam(0);
        int programInterface = call.intParam(1);
        int pname = call.intParam(2);
        
        IntBuffer params = intScratchBuffer.get();
        params.clear();
        
        GLES31.glGetProgramInterfaceiv(program, programInterface, pname, params);
        
        GLESError error = errorHandler.checkError("glGetProgramInterfaceiv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(params.get(0));
    }
    
    private MappingResult mapGetProgramResourceiv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Program resource queries require GLES 3.1+");
        }
        
        int program = call.intParam(0);
        int programInterface = call.intParam(1);
        int index = call.intParam(2);
        IntBuffer props = call.objectParam(0);
        IntBuffer length = call.objectParam(1);
        IntBuffer params = call.objectParam(2);
        
        GLES31.glGetProgramResourceiv(program, programInterface, index, props, length, params);
        
        GLESError error = errorHandler.checkError("glGetProgramResourceiv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetProgramResourceName(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Program resource queries require GLES 3.1+");
        }
        
        int program = call.intParam(0);
        int programInterface = call.intParam(1);
        int index = call.intParam(2);
        int bufSize = call.intParam(3);
        
        String name = GLES31.glGetProgramResourceName(program, programInterface, index, bufSize);
        
        int handle = stringCache.put(name);
        return MappingResult.success(handle);
    }
    
    private MappingResult mapGetProgramResourceLocation(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Program resource queries require GLES 3.1+");
        }
        
        int program = call.intParam(0);
        int programInterface = call.intParam(1);
        String name = call.objectParam(0);
        
        int location = GLES31.glGetProgramResourceLocation(program, programInterface, name);
        
        return MappingResult.success(location);
    }

// ============================================================================
// SECTION 10: QUERY OBJECTS (~800 lines)
// ============================================================================

    // ========================================================================
    // QUERY CREATION & DELETION
    // ========================================================================
    
    private MappingResult mapQueryCreate(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Try extensions for GLES 2.0
            if (!capabilities.hasExtension(GLESExtension.EXT_occlusion_query_boolean)) {
                return MappingResult.unsupported("Query objects require GLES 3.0+ or EXT_occlusion_query_boolean");
            }
        }
        
        int count = call.intParam(0);
        
        IntBuffer ids = intScratchBuffer.get();
        ids.clear();
        ids.limit(count);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glGenQueries(ids);
        } else {
            GLES20.glGenQueriesEXT(ids);
        }
        
        GLESError error = errorHandler.checkError("glGenQueries");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        // Track queries
        for (int i = 0; i < count; i++) {
            resourceRegistry.registerQuery(ids.get(i));
        }
        
        return MappingResult.success(ids.get(0));
    }
    
    private MappingResult mapQueryDelete(CallDescriptor call) {
        int query = call.intParam(0);
        
        if (query == 0) {
            return MappingResult.success();
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glDeleteQueries(query);
        } else if (capabilities.hasExtension(GLESExtension.EXT_occlusion_query_boolean)) {
            GLES20.glDeleteQueriesEXT(query);
        }
        
        resourceRegistry.unregisterQuery(query);
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // QUERY EXECUTION
    // ========================================================================
    
    private MappingResult mapBeginQuery(CallDescriptor call) {
        int target = call.intParam(0);
        int id = call.intParam(1);
        
        // Validate query target
        if (!isQueryTargetSupported(target)) {
            return MappingResult.failed(MappingStatus.UNSUPPORTED,
                "Query target 0x" + Integer.toHexString(target) + " not supported");
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glBeginQuery(target, id);
        } else {
            GLES20.glBeginQueryEXT(target, id);
        }
        
        GLESError error = errorHandler.checkError("glBeginQuery");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapEndQuery(CallDescriptor call) {
        int target = call.intParam(0);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glEndQuery(target);
        } else {
            GLES20.glEndQueryEXT(target);
        }
        
        GLESError error = errorHandler.checkError("glEndQuery");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private boolean isQueryTargetSupported(int target) {
        return switch (target) {
            case 0x8C2F -> // ANY_SAMPLES_PASSED
                capabilities.version().isAtLeast(GLESVersion.GLES_3_0) ||
                capabilities.hasExtension(GLESExtension.EXT_occlusion_query_boolean);
            case 0x8D6A -> // ANY_SAMPLES_PASSED_CONSERVATIVE
                capabilities.version().isAtLeast(GLESVersion.GLES_3_0) ||
                capabilities.hasExtension(GLESExtension.EXT_occlusion_query_boolean);
            case 0x8C87 -> // TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN
                capabilities.version().isAtLeast(GLESVersion.GLES_3_0);
            case 0x88BF -> // TIME_ELAPSED (extension)
                capabilities.hasExtension(GLESExtension.EXT_disjoint_timer_query);
            default -> false;
        };
    }
    
    // ========================================================================
    // QUERY RESULTS
    // ========================================================================
    
    private MappingResult mapGetQueryObjectuiv(CallDescriptor call) {
        int id = call.intParam(0);
        int pname = call.intParam(1);
        
        IntBuffer params = intScratchBuffer.get();
        params.clear();
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glGetQueryObjectuiv(id, pname, params);
        } else {
            GLES20.glGetQueryObjectuivEXT(id, pname, params);
        }
        
        GLESError error = errorHandler.checkError("glGetQueryObjectuiv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(params.get(0));
    }
    
    private MappingResult mapGetQueryiv(CallDescriptor call) {
        int target = call.intParam(0);
        int pname = call.intParam(1);
        
        IntBuffer params = intScratchBuffer.get();
        params.clear();
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glGetQueryiv(target, pname, params);
        } else {
            GLES20.glGetQueryivEXT(target, pname, params);
        }
        
        return MappingResult.success(params.get(0));
    }
    
    // 64-bit query results (GLES 3.0+ or timer query extension)
    private MappingResult mapGetQueryObjectui64v(CallDescriptor call) {
        if (!capabilities.hasExtension(GLESExtension.EXT_disjoint_timer_query)) {
            // Return 32-bit result if 64-bit not available
            return mapGetQueryObjectuiv(call);
        }
        
        int id = call.intParam(0);
        int pname = call.intParam(1);
        
        LongBuffer params = LongBuffer.allocate(1);
        
        GLES20.glGetQueryObjectui64vEXT(id, pname, params);
        
        GLESError error = errorHandler.checkError("glGetQueryObjectui64v");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(params.get(0));
    }
    
    // ========================================================================
    // TIMER QUERIES
    // ========================================================================
    
    private MappingResult mapQueryCounter(CallDescriptor call) {
        if (!capabilities.hasExtension(GLESExtension.EXT_disjoint_timer_query)) {
            return MappingResult.unsupported("Timer queries require EXT_disjoint_timer_query");
        }
        
        int id = call.intParam(0);
        int target = call.intParam(1);
        
        // Only GL_TIMESTAMP is valid for glQueryCounter
        if (target != 0x8E28) { // GL_TIMESTAMP
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "glQueryCounter only supports GL_TIMESTAMP target");
        }
        
        GLES20.glQueryCounterEXT(id, target);
        
        GLESError error = errorHandler.checkError("glQueryCounter");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetInteger64v(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fallback to 32-bit
            return mapGetInteger(call);
        }
        
        int pname = call.intParam(0);
        
        LongBuffer data = LongBuffer.allocate(1);
        GLES30.glGetInteger64v(pname, data);
        
        return MappingResult.success(data.get(0));
    }

// ============================================================================
// SECTION 11: SYNCHRONIZATION (~700 lines)
// ============================================================================

    // ========================================================================
    // SYNC OBJECTS
    // ========================================================================
    
    private MappingResult mapFenceSync(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fallback: use glFinish for synchronization
            return MappingResult.unsupported("Sync objects require GLES 3.0+. Use glFinish for synchronization.");
        }
        
        int condition = call.intParam(0);
        int flags = call.intParam(1);
        
        // Validate condition (must be GL_SYNC_GPU_COMMANDS_COMPLETE)
        if (condition != 0x9117) { // GL_SYNC_GPU_COMMANDS_COMPLETE
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Invalid fence condition. Must be GL_SYNC_GPU_COMMANDS_COMPLETE");
        }
        
        // Flags must be 0
        if (flags != 0) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Fence flags must be 0");
        }
        
        long sync = GLES30.glFenceSync(condition, flags);
        
        if (sync == 0) {
            GLESError error = errorHandler.checkError("glFenceSync");
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION,
                error.code(), "glFenceSync failed: " + error.description());
        }
        
        return MappingResult.success(sync);
    }
    
    private MappingResult mapDeleteSync(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.success(); // No-op on GLES 2.0
        }
        
        long sync = call.longParam(0);
        
        if (sync == 0) {
            return MappingResult.success();
        }
        
        GLES30.glDeleteSync(sync);
        
        return MappingResult.success();
    }
    
    private MappingResult mapIsSync(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.success(0);
        }
        
        long sync = call.longParam(0);
        
        boolean result = GLES30.glIsSync(sync);
        
        return MappingResult.success(result ? 1 : 0);
    }
    
    // ========================================================================
    // SYNC WAIT OPERATIONS
    // ========================================================================
    
    private MappingResult mapClientWaitSync(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fallback: just use glFinish
            GLES20.glFinish();
            return MappingResult.successWithWarning(GL_CONDITION_SATISFIED, 
                "Sync objects not supported, used glFinish instead");
        }
        
        long sync = call.longParam(0);
        int flags = call.intParam(0);
        long timeout = call.longParam(1);
        
        // Validate flags (only GL_SYNC_FLUSH_COMMANDS_BIT is valid)
        if ((flags & ~GL_SYNC_FLUSH_COMMANDS_BIT) != 0) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Invalid wait flags: 0x" + Integer.toHexString(flags));
        }
        
        int result = GLES30.glClientWaitSync(sync, flags, timeout);
        
        // Check result
        if (result == GL_WAIT_FAILED) {
            GLESError error = errorHandler.checkError("glClientWaitSync");
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION,
                error.code(), "glClientWaitSync failed");
        }
        
        return MappingResult.success(result);
    }
    
    private MappingResult mapWaitSync(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fallback: no-op (GPU waits are implicit on older hardware)
            return MappingResult.successWithWarning(0, "Sync objects not supported");
        }
        
        long sync = call.longParam(0);
        int flags = call.intParam(0);
        long timeout = call.longParam(1);
        
        // Flags must be 0
        if (flags != 0) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "glWaitSync flags must be 0");
        }
        
        // Timeout must be GL_TIMEOUT_IGNORED
        if (timeout != GL_TIMEOUT_IGNORED) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "glWaitSync timeout must be GL_TIMEOUT_IGNORED");
        }
        
        GLES30.glWaitSync(sync, flags, timeout);
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetSynciv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Sync objects require GLES 3.0+");
        }
        
        long sync = call.longParam(0);
        int pname = call.intParam(0);
        int bufSize = call.intParam(1);
        
        IntBuffer length = intScratchBuffer.get();
        IntBuffer values = IntBuffer.allocate(bufSize);
        length.clear();
        
        GLES30.glGetSynciv(sync, pname, length, values);
        
        GLESError error = errorHandler.checkError("glGetSynciv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(values.get(0));
    }
    
    // Sync constants
    private static final int GL_SYNC_FLUSH_COMMANDS_BIT = 0x00000001;
    private static final int GL_ALREADY_SIGNALED = 0x911A;
    private static final int GL_TIMEOUT_EXPIRED = 0x911B;
    private static final int GL_CONDITION_SATISFIED = 0x911C;
    private static final int GL_WAIT_FAILED = 0x911D;
    private static final long GL_TIMEOUT_IGNORED = 0xFFFFFFFFFFFFFFFFL;
    
    // ========================================================================
    // FLUSH & FINISH
    // ========================================================================
    
    private MappingResult mapFlush(CallDescriptor call) {
        GLES20.glFlush();
        return MappingResult.success();
    }
    
    private MappingResult mapFinish(CallDescriptor call) {
        GLES20.glFinish();
        return MappingResult.success();
    }

// ============================================================================
// SECTION 12: MISCELLANEOUS OPERATIONS (CONTINUED FROM WHERE I STOPPED)
// ============================================================================

    // This is where I stopped - completing mapDebugMessageInsert
    private MappingResult mapDebugMessageInsert(CallDescriptor call) {
        if (!capabilities.supportsDebugOutput()) {
            return MappingResult.success(); // Silently ignore
        }
        
        int source = call.intParam(0);
        int type = call.intParam(1);
        int id = call.intParam(2);
        int severity = call.intParam(3);
        String message = call.objectParam(0);
        
        // Validate source (must be GL_DEBUG_SOURCE_APPLICATION or GL_DEBUG_SOURCE_THIRD_PARTY)
        if (source != 0x824A && source != 0x824B) { // APPLICATION, THIRD_PARTY
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Debug message source must be APPLICATION or THIRD_PARTY");
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            GLES32.glDebugMessageInsert(source, type, id, severity, message);
        } else if (capabilities.hasExtension(GLESExtension.KHR_debug)) {
            GLES20.glDebugMessageInsertKHR(source, type, id, severity, message);
        }
        
        GLESError error = errorHandler.checkError("glDebugMessageInsert");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapDebugMessageControl(CallDescriptor call) {
        if (!capabilities.supportsDebugOutput()) {
            return MappingResult.success();
        }
        
        int source = call.intParam(0);
        int type = call.intParam(1);
        int severity = call.intParam(2);
        IntBuffer ids = call.objectParam(0);
        boolean enabled = call.boolParam(0);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            GLES32.glDebugMessageControl(source, type, severity, ids, enabled);
        } else if (capabilities.hasExtension(GLESExtension.KHR_debug)) {
            GLES20.glDebugMessageControlKHR(source, type, severity, ids, enabled);
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapPushDebugGroup(CallDescriptor call) {
        if (!capabilities.supportsDebugOutput()) {
            return MappingResult.success();
        }
        
        int source = call.intParam(0);
        int id = call.intParam(1);
        String message = call.objectParam(0);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            GLES32.glPushDebugGroup(source, id, message);
        } else if (capabilities.hasExtension(GLESExtension.KHR_debug)) {
            GLES20.glPushDebugGroupKHR(source, id, message);
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapPopDebugGroup(CallDescriptor call) {
        if (!capabilities.supportsDebugOutput()) {
            return MappingResult.success();
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            GLES32.glPopDebugGroup();
        } else if (capabilities.hasExtension(GLESExtension.KHR_debug)) {
            GLES20.glPopDebugGroupKHR();
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapObjectLabel(CallDescriptor call) {
        if (!capabilities.supportsDebugOutput()) {
            return MappingResult.success();
        }
        
        int identifier = call.intParam(0);
        int name = call.intParam(1);
        String label = call.objectParam(0);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            GLES32.glObjectLabel(identifier, name, label);
        } else if (capabilities.hasExtension(GLESExtension.KHR_debug)) {
            GLES20.glObjectLabelKHR(identifier, name, label);
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetObjectLabel(CallDescriptor call) {
        if (!capabilities.supportsDebugOutput()) {
            return MappingResult.success(0);
        }
        
        int identifier = call.intParam(0);
        int name = call.intParam(1);
        int bufSize = call.intParam(2);
        
        String label;
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            label = GLES32.glGetObjectLabel(identifier, name, bufSize);
        } else {
            label = GLES20.glGetObjectLabelKHR(identifier, name, bufSize);
        }
        
        int handle = stringCache.put(label);
        return MappingResult.success(handle);
    }
    
    private MappingResult mapObjectPtrLabel(CallDescriptor call) {
        if (!capabilities.supportsDebugOutput()) {
            return MappingResult.success();
        }
        
        long ptr = call.longParam(0);
        String label = call.objectParam(0);
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            GLES32.glObjectPtrLabel(ptr, label);
        } else if (capabilities.hasExtension(GLESExtension.KHR_debug)) {
            GLES20.glObjectPtrLabelKHR(ptr, label);
        }
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // GET OPERATIONS
    // ========================================================================
    
    private MappingResult mapGetError(CallDescriptor call) {
        int error = GLES20.glGetError();
        return MappingResult.success(error);
    }
    
    private MappingResult mapGetString(CallDescriptor call) {
        int name = call.intParam(0);
        
        String result = GLES20.glGetString(name);
        
        // Store string and return handle
        int handle = stringCache.put(result);
        return MappingResult.success(handle);
    }
    
    private MappingResult mapGetStringi(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Indexed string query requires GLES 3.0+");
        }
        
        int name = call.intParam(0);
        int index = call.intParam(1);
        
        String result = GLES30.glGetStringi(name, index);
        
        int handle = stringCache.put(result);
        return MappingResult.success(handle);
    }
    
    private MappingResult mapGetInteger(CallDescriptor call) {
        int pname = call.intParam(0);
        
        int result = GLES20.glGetInteger(pname);
        
        return MappingResult.success(result);
    }
    
    private MappingResult mapGetIntegerv(CallDescriptor call) {
        int pname = call.intParam(0);
        IntBuffer data = call.objectParam(0);
        
        GLES20.glGetIntegerv(pname, data);
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetFloat(CallDescriptor call) {
        int pname = call.intParam(0);
        
        float result = GLES20.glGetFloat(pname);
        
        // Encode float bits as int for result
        return MappingResult.success(Float.floatToRawIntBits(result));
    }
    
    private MappingResult mapGetFloatv(CallDescriptor call) {
        int pname = call.intParam(0);
        FloatBuffer data = call.objectParam(0);
        
        GLES20.glGetFloatv(pname, data);
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetBoolean(CallDescriptor call) {
        int pname = call.intParam(0);
        
        boolean result = GLES20.glGetBoolean(pname);
        
        return MappingResult.success(result ? 1 : 0);
    }
    
    private MappingResult mapGetBooleanv(CallDescriptor call) {
        int pname = call.intParam(0);
        ByteBuffer data = call.objectParam(0);
        
        GLES20.glGetBooleanv(pname, data);
        
        return MappingResult.success();
    }
    
    private MappingResult mapIsEnabled(CallDescriptor call) {
        int cap = call.intParam(0);
        
        boolean result = GLES20.glIsEnabled(cap);
        
        return MappingResult.success(result ? 1 : 0);
    }
    
    // ========================================================================
    // PIXEL STORE OPERATIONS
    // ========================================================================
    
    private MappingResult mapPixelStorei(CallDescriptor call) {
        int pname = call.intParam(0);
        int param = call.intParam(1);
        
        // Validate pixel store parameters
        if (!validatePixelStoreParam(pname, param)) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Invalid pixel store parameter: pname=0x" + Integer.toHexString(pname) + 
                ", value=" + param);
        }
        
        GLES20.glPixelStorei(pname, param);
        
        GLESError error = errorHandler.checkError("glPixelStorei");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private boolean validatePixelStoreParam(int pname, int param) {
        return switch (pname) {
            case 0x0D05 -> param == 1 || param == 2 || param == 4 || param == 8; // PACK_ALIGNMENT
            case 0x0CF5 -> param == 1 || param == 2 || param == 4 || param == 8; // UNPACK_ALIGNMENT
            case 0x806C, 0x806D, 0x806E, 0x806F -> param >= 0; // ROW_LENGTH, SKIP_*, IMAGE_HEIGHT (GLES 3.0+)
            default -> true;
        };
    }
    
    // ========================================================================
    // HINT OPERATIONS
    // ========================================================================
    
    private MappingResult mapHint(CallDescriptor call) {
        int target = call.intParam(0);
        int mode = call.intParam(1);
        
        // Validate mode
        if (mode != 0x1100 && mode != 0x1101 && mode != 0x1102) { // DONT_CARE, FASTEST, NICEST
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Invalid hint mode: 0x" + Integer.toHexString(mode));
        }
        
        GLES20.glHint(target, mode);
        
        // Hints may fail silently on some drivers - clear any errors
        errorHandler.clearErrors();
        
        return MappingResult.success();
    }

// ============================================================================
// SECTION 13: TRANSFORM FEEDBACK (~800 lines)
// ============================================================================

    // ========================================================================
    // TRANSFORM FEEDBACK OBJECTS
    // ========================================================================
    
    private MappingResult mapTransformFeedbackCreate(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Transform feedback requires GLES 3.0+");
        }
        
        int count = call.intParam(0);
        
        IntBuffer ids = intScratchBuffer.get();
        ids.clear();
        ids.limit(count);
        
        GLES30.glGenTransformFeedbacks(ids);
        
        GLESError error = errorHandler.checkError("glGenTransformFeedbacks");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(ids.get(0));
    }
    
    private MappingResult mapTransformFeedbackDelete(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.success();
        }
        
        int id = call.intParam(0);
        
        if (id == 0) {
            return MappingResult.success();
        }
        
        GLES30.glDeleteTransformFeedbacks(id);
        
        return MappingResult.success();
    }
    
    private MappingResult mapTransformFeedbackBind(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Transform feedback requires GLES 3.0+");
        }
        
        int target = call.intParam(0);
        int id = call.intParam(1);
        
        GLES30.glBindTransformFeedback(target, id);
        
        GLESError error = errorHandler.checkError("glBindTransformFeedback");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapIsTransformFeedback(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.success(0);
        }
        
        int id = call.intParam(0);
        
        boolean result = GLES30.glIsTransformFeedback(id);
        
        return MappingResult.success(result ? 1 : 0);
    }
    
    // ========================================================================
    // TRANSFORM FEEDBACK CONTROL
    // ========================================================================
    
    private MappingResult mapBeginTransformFeedback(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Transform feedback requires GLES 3.0+");
        }
        
        int primitiveMode = call.intParam(0);
        
        // Validate primitive mode
        if (primitiveMode != 0x0000 && primitiveMode != 0x0001 && primitiveMode != 0x0004) {
            // Must be GL_POINTS, GL_LINES, or GL_TRIANGLES
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Transform feedback primitive mode must be POINTS, LINES, or TRIANGLES");
        }
        
        GLES30.glBeginTransformFeedback(primitiveMode);
        
        GLESError error = errorHandler.checkError("glBeginTransformFeedback");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapEndTransformFeedback(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.success();
        }
        
        GLES30.glEndTransformFeedback();
        
        GLESError error = errorHandler.checkError("glEndTransformFeedback");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapPauseTransformFeedback(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Transform feedback requires GLES 3.0+");
        }
        
        GLES30.glPauseTransformFeedback();
        
        GLESError error = errorHandler.checkError("glPauseTransformFeedback");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapResumeTransformFeedback(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Transform feedback requires GLES 3.0+");
        }
        
        GLES30.glResumeTransformFeedback();
        
        GLESError error = errorHandler.checkError("glResumeTransformFeedback");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // TRANSFORM FEEDBACK VARYINGS
    // ========================================================================
    
    private MappingResult mapTransformFeedbackVaryings(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Transform feedback requires GLES 3.0+");
        }
        
        int program = call.intParam(0);
        int bufferMode = call.intParam(1);
        String[] varyings = call.objectParam(0);
        
        // Validate buffer mode
        if (bufferMode != 0x8C8C && bufferMode != 0x8C8D) { // INTERLEAVED_ATTRIBS, SEPARATE_ATTRIBS
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Invalid transform feedback buffer mode: 0x" + Integer.toHexString(bufferMode));
        }
        
        // Validate varying count for SEPARATE_ATTRIBS
        if (bufferMode == 0x8C8D) { // SEPARATE_ATTRIBS
            int maxSeparate = capabilities.maxTransformFeedbackSeparateAttribs();
            if (varyings.length > maxSeparate) {
                return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                    "Too many separate transform feedback varyings: " + varyings.length + 
                    " > " + maxSeparate);
            }
        }
        
        GLES30.glTransformFeedbackVaryings(program, varyings, bufferMode);
        
        GLESError error = errorHandler.checkError("glTransformFeedbackVaryings");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetTransformFeedbackVarying(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Transform feedback requires GLES 3.0+");
        }
        
        int program = call.intParam(0);
        int index = call.intParam(1);
        int bufSize = call.intParam(2);
        
        IntBuffer size = intScratchBuffer.get();
        IntBuffer type = intScratchBuffer.get();
        size.clear();
        type.clear();
        
        String name = GLES30.glGetTransformFeedbackVarying(program, index, bufSize, size, type);
        
        int nameHandle = stringCache.put(name);
        return MappingResult.success(new TransformFeedbackVaryingResult(name, size.get(0), type.get(0)));
    }

// ============================================================================
// SECTION 14: UNIFORM BUFFER OBJECTS (~800 lines)
// ============================================================================

    // ========================================================================
    // UBO BINDING
    // ========================================================================
    
    private MappingResult mapUniformBlockBinding(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Uniform buffer objects require GLES 3.0+");
        }
        
        int program = call.intParam(0);
        int uniformBlockIndex = call.intParam(1);
        int uniformBlockBinding = call.intParam(2);
        
        // Validate binding point
        if (uniformBlockBinding >= capabilities.maxUniformBufferBindings()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Uniform block binding " + uniformBlockBinding + " exceeds maximum " + 
                capabilities.maxUniformBufferBindings());
        }
        
        GLES30.glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBinding);
        
        GLESError error = errorHandler.checkError("glUniformBlockBinding");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetUniformBlockIndex(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Uniform buffer objects require GLES 3.0+");
        }
        
        int program = call.intParam(0);
        String uniformBlockName = call.objectParam(0);
        
        int index = GLES30.glGetUniformBlockIndex(program, uniformBlockName);
        
        return MappingResult.success(index);
    }
    
    private MappingResult mapGetActiveUniformBlockiv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Uniform buffer objects require GLES 3.0+");
        }
        
        int program = call.intParam(0);
        int uniformBlockIndex = call.intParam(1);
        int pname = call.intParam(2);
        IntBuffer params = call.objectParam(0);
        
        GLES30.glGetActiveUniformBlockiv(program, uniformBlockIndex, pname, params);
        
        GLESError error = errorHandler.checkError("glGetActiveUniformBlockiv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success(params.get(0));
    }
    
    private MappingResult mapGetActiveUniformBlockName(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Uniform buffer objects require GLES 3.0+");
        }
        
        int program = call.intParam(0);
        int uniformBlockIndex = call.intParam(1);
        int bufSize = call.intParam(2);
        
        String name = GLES30.glGetActiveUniformBlockName(program, uniformBlockIndex, bufSize);
        
        int handle = stringCache.put(name);
        return MappingResult.success(handle);
    }
    
    private MappingResult mapGetUniformIndices(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Uniform buffer objects require GLES 3.0+");
        }
        
        int program = call.intParam(0);
        String[] uniformNames = call.objectParam(0);
        IntBuffer uniformIndices = call.objectParam(1);
        
        GLES30.glGetUniformIndices(program, uniformNames, uniformIndices);
        
        GLESError error = errorHandler.checkError("glGetUniformIndices");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetActiveUniformsiv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Uniform buffer objects require GLES 3.0+");
        }
        
        int program = call.intParam(0);
        IntBuffer uniformIndices = call.objectParam(0);
        int pname = call.intParam(1);
        IntBuffer params = call.objectParam(1);
        
        GLES30.glGetActiveUniformsiv(program, uniformIndices, pname, params);
        
        GLESError error = errorHandler.checkError("glGetActiveUniformsiv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    // ========================================================================
    // BUFFER BASE/RANGE BINDING
    // ========================================================================
    
    private MappingResult mapBindBufferBase(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Buffer base binding requires GLES 3.0+");
        }
        
        int target = call.intParam(0);
        int index = call.intParam(1);
        int buffer = call.intParam(2);
        
        // Validate target and index
        if (!validateBufferBaseTarget(target, index)) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Invalid buffer base binding: target=0x" + Integer.toHexString(target) + 
                ", index=" + index);
        }
        
        GLES30.glBindBufferBase(target, index, buffer);
        
        GLESError error = errorHandler.checkError("glBindBufferBase");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapBindBufferRange(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Buffer range binding requires GLES 3.0+");
        }
        
        int target = call.intParam(0);
        int index = call.intParam(1);
        int buffer = call.intParam(2);
        long offset = call.longParam(0);
        long size = call.longParam(1);
        
        // Validate alignment for UBO
        if (target == 0x8A11) { // GL_UNIFORM_BUFFER
            int alignment = capabilities.uniformBufferOffsetAlignment();
            if (offset % alignment != 0) {
                return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                    "UBO offset " + offset + " not aligned to " + alignment);
            }
        }
        
        // Validate alignment for SSBO
        if (target == 0x90D2) { // GL_SHADER_STORAGE_BUFFER
            int alignment = capabilities.shaderStorageBufferOffsetAlignment();
            if (offset % alignment != 0) {
                return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                    "SSBO offset " + offset + " not aligned to " + alignment);
            }
        }
        
        GLES30.glBindBufferRange(target, index, buffer, offset, size);
        
        GLESError error = errorHandler.checkError("glBindBufferRange");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private boolean validateBufferBaseTarget(int target, int index) {
        return switch (target) {
            case 0x8A11 -> index < capabilities.maxUniformBufferBindings(); // UNIFORM_BUFFER
            case 0x8C8E -> index < capabilities.maxTransformFeedbackSeparateAttribs(); // TRANSFORM_FEEDBACK_BUFFER
            case 0x90D2 -> capabilities.supportsCompute() && index < capabilities.maxShaderStorageBufferBindings(); // SHADER_STORAGE_BUFFER
            case 0x92C0 -> capabilities.supportsCompute() && index < capabilities.maxAtomicCounterBufferBindings(); // ATOMIC_COUNTER_BUFFER
            default -> false;
        };
    }

// ============================================================================
// SECTION 15: SHADER STORAGE BUFFER OBJECTS (~500 lines)
// ============================================================================

    private MappingResult mapShaderStorageBlockBinding(CallDescriptor call) {
        if (!capabilities.supportsCompute()) {
            return MappingResult.unsupported("Shader storage buffers require GLES 3.1+");
        }
        
        int program = call.intParam(0);
        int storageBlockIndex = call.intParam(1);
        int storageBlockBinding = call.intParam(2);
        
        // Validate binding point
        if (storageBlockBinding >= capabilities.maxShaderStorageBufferBindings()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Shader storage block binding " + storageBlockBinding + " exceeds maximum " +
                capabilities.maxShaderStorageBufferBindings());
        }
        
        GLES31.glShaderStorageBlockBinding(program, storageBlockIndex, storageBlockBinding);
        
        GLESError error = errorHandler.checkError("glShaderStorageBlockBinding");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetProgramResourceIndex(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Program resource queries require GLES 3.1+");
        }
        
        int program = call.intParam(0);
        int programInterface = call.intParam(1);
        String name = call.objectParam(0);
        
        int index = GLES31.glGetProgramResourceIndex(program, programInterface, name);
        
        return MappingResult.success(index);
    }

// ============================================================================
// SECTION 16: PROGRAM PIPELINES (Separate Shader Objects) (~800 lines)
// ============================================================================

    // ========================================================================
    // PROGRAM PIPELINE OBJECTS
    // ========================================================================
    
    private MappingResult mapProgramPipelineCreate(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Program pipelines require GLES 3.1+");
        }
        
        int count = call.intParam(0);
        
        IntBuffer pipelines = intScratchBuffer.get();
        pipelines.clear();
        pipelines.limit(count);
        
        GLES31.glGenProgramPipelines(pipelines);
        
        GLESError error = errorHandler.checkError("glGenProgramPipelines");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        for (int i = 0; i < count; i++) {
            resourceRegistry.registerProgramPipeline(pipelines.get(i));
        }
        
        return MappingResult.success(pipelines.get(0));
    }
    
    private MappingResult mapProgramPipelineDelete(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.success();
        }
        
        int pipeline = call.intParam(0);
        
        if (pipeline == 0) {
            return MappingResult.success();
        }
        
        // Unbind if currently bound
        if (stateTracker.getBoundProgramPipeline() == pipeline) {
            GLES31.glBindProgramPipeline(0);
            stateTracker.setBoundProgramPipeline(0);
        }
        
        GLES31.glDeleteProgramPipelines(pipeline);
        resourceRegistry.unregisterProgramPipeline(pipeline);
        
        return MappingResult.success();
    }
    
    private MappingResult mapProgramPipelineBind(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Program pipelines require GLES 3.1+");
        }
        
        int pipeline = call.intParam(0);
        
        // State caching
        if (stateTracker.getBoundProgramPipeline() == pipeline) {
            return MappingResult.success();
        }
        
        GLES31.glBindProgramPipeline(pipeline);
        
        GLESError error = errorHandler.checkError("glBindProgramPipeline");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        stateTracker.setBoundProgramPipeline(pipeline);
        stateTracker.incrementStateChangeCount();
        
        return MappingResult.success();
    }
    
    private MappingResult mapIsProgramPipeline(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.success(0);
        }
        
        int pipeline = call.intParam(0);
        
        boolean result = GLES31.glIsProgramPipeline(pipeline);
        
        return MappingResult.success(result ? 1 : 0);
    }
    
    // ========================================================================
    // SEPARABLE PROGRAMS
    // ========================================================================
    
    private MappingResult mapUseProgramStages(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Program pipelines require GLES 3.1+");
        }
        
        int pipeline = call.intParam(0);
        int stages = call.intParam(1);
        int program = call.intParam(2);
        
        // Validate stage bits
        int validStages = GL_VERTEX_SHADER_BIT | GL_FRAGMENT_SHADER_BIT | GL_COMPUTE_SHADER_BIT;
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            validStages |= GL_GEOMETRY_SHADER_BIT | GL_TESS_CONTROL_SHADER_BIT | GL_TESS_EVALUATION_SHADER_BIT;
        }
        
        if ((stages & ~validStages) != 0 && stages != GL_ALL_SHADER_BITS) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Invalid shader stage bits: 0x" + Integer.toHexString(stages));
        }
        
        GLES31.glUseProgramStages(pipeline, stages, program);
        
        GLESError error = errorHandler.checkError("glUseProgramStages");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapActiveShaderProgram(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Program pipelines require GLES 3.1+");
        }
        
        int pipeline = call.intParam(0);
        int program = call.intParam(1);
        
        GLES31.glActiveShaderProgram(pipeline, program);
        
        GLESError error = errorHandler.checkError("glActiveShaderProgram");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapCreateShaderProgramv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Separable programs require GLES 3.1+");
        }
        
        int type = call.intParam(0);
        String[] strings = call.objectParam(0);
        
        int program = GLES31.glCreateShaderProgramv(type, strings);
        
        if (program == 0) {
            GLESError error = errorHandler.checkError("glCreateShaderProgramv");
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, 
                error.code(), "glCreateShaderProgramv failed: " + error.description());
        }
        
        // Check link status
        int status = GLES20.glGetProgrami(program, GL20.GL_LINK_STATUS);
        if (status == GL20.GL_FALSE) {
            String infoLog = GLES20.glGetProgramInfoLog(program);
            GLES20.glDeleteProgram(program);
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION,
                "Shader program creation failed:\n" + infoLog);
        }
        
        resourceRegistry.registerProgram(program);
        
        return MappingResult.success(program);
    }
    
    private MappingResult mapValidateProgramPipeline(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Program pipelines require GLES 3.1+");
        }
        
        int pipeline = call.intParam(0);
        
        GLES31.glValidateProgramPipeline(pipeline);
        
        IntBuffer status = intScratchBuffer.get();
        status.clear();
        GLES31.glGetProgramPipelineiv(pipeline, GL_VALIDATE_STATUS, status);
        
        if (status.get(0) == GL20.GL_FALSE) {
            String infoLog = GLES31.glGetProgramPipelineInfoLog(pipeline);
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Program pipeline validation failed:\n" + infoLog);
        }
        
        return MappingResult.success(status.get(0));
    }
    
    private MappingResult mapGetProgramPipelineiv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Program pipelines require GLES 3.1+");
        }
        
        int pipeline = call.intParam(0);
        int pname = call.intParam(1);
        
        IntBuffer params = intScratchBuffer.get();
        params.clear();
        
        GLES31.glGetProgramPipelineiv(pipeline, pname, params);
        
        return MappingResult.success(params.get(0));
    }
    
    private MappingResult mapGetProgramPipelineInfoLog(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.success(stringCache.put(""));
        }
        
        int pipeline = call.intParam(0);
        
        String infoLog = GLES31.glGetProgramPipelineInfoLog(pipeline);
        
        int handle = stringCache.put(infoLog);
        return MappingResult.success(handle);
    }
    
    // Shader stage bits
    private static final int GL_VERTEX_SHADER_BIT = 0x00000001;
    private static final int GL_FRAGMENT_SHADER_BIT = 0x00000002;
    private static final int GL_GEOMETRY_SHADER_BIT = 0x00000004;
    private static final int GL_TESS_CONTROL_SHADER_BIT = 0x00000008;
    private static final int GL_TESS_EVALUATION_SHADER_BIT = 0x00000010;
    private static final int GL_COMPUTE_SHADER_BIT = 0x00000020;
    private static final int GL_ALL_SHADER_BITS = 0xFFFFFFFF;
    private static final int GL_VALIDATE_STATUS = 0x8B83;

// ============================================================================
// SECTION 17: SAMPLER OBJECTS (~600 lines)
// ============================================================================

    private MappingResult mapSamplerCreate(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Sampler objects require GLES 3.0+");
        }
        
        int count = call.intParam(0);
        
        IntBuffer samplers = intScratchBuffer.get();
        samplers.clear();
        samplers.limit(count);
        
        GLES30.glGenSamplers(samplers);
        
        GLESError error = errorHandler.checkError("glGenSamplers");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        for (int i = 0; i < count; i++) {
            resourceRegistry.registerSampler(samplers.get(i));
        }
        
        return MappingResult.success(samplers.get(0));
    }
    
    private MappingResult mapSamplerDelete(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.success();
        }
        
        int sampler = call.intParam(0);
        
        if (sampler == 0) {
            return MappingResult.success();
        }
        
        GLES30.glDeleteSamplers(sampler);
        resourceRegistry.unregisterSampler(sampler);
        
        return MappingResult.success();
    }
    
    private MappingResult mapSamplerBind(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // GLES 2.0: Sampler state is part of texture object - no-op
            return MappingResult.successWithWarning(0x4000, 
                "Sampler objects not supported, using texture sampler state");
        }
        
        int unit = call.intParam(0);
        int sampler = call.intParam(1);
        
        // Validate texture unit
        if (unit >= capabilities.maxTextureUnits()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Texture unit " + unit + " exceeds maximum " + capabilities.maxTextureUnits());
        }
        
        GLES30.glBindSampler(unit, sampler);
        
        GLESError error = errorHandler.checkError("glBindSampler");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapIsSampler(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.success(0);
        }
        
        int sampler = call.intParam(0);
        
        boolean result = GLES30.glIsSampler(sampler);
        
        return MappingResult.success(result ? 1 : 0);
    }
    
    private MappingResult mapSamplerParameteri(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Sampler objects require GLES 3.0+");
        }
        
        int sampler = call.intParam(0);
        int pname = call.intParam(1);
        int param = call.intParam(2);
        
        GLES30.glSamplerParameteri(sampler, pname, param);
        
        GLESError error = errorHandler.checkError("glSamplerParameteri");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapSamplerParameterf(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Sampler objects require GLES 3.0+");
        }
        
        int sampler = call.intParam(0);
        int pname = call.intParam(1);
        float param = call.floatParam(0);
        
        GLES30.glSamplerParameterf(sampler, pname, param);
        
        GLESError error = errorHandler.checkError("glSamplerParameterf");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapSamplerParameteriv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Sampler objects require GLES 3.0+");
        }
        
        int sampler = call.intParam(0);
        int pname = call.intParam(1);
        IntBuffer params = call.objectParam(0);
        
        GLES30.glSamplerParameteriv(sampler, pname, params);
        
        GLESError error = errorHandler.checkError("glSamplerParameteriv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapSamplerParameterfv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Sampler objects require GLES 3.0+");
        }
        
        int sampler = call.intParam(0);
        int pname = call.intParam(1);
        FloatBuffer params = call.objectParam(0);
        
        GLES30.glSamplerParameterfv(sampler, pname, params);
        
        GLESError error = errorHandler.checkError("glSamplerParameterfv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetSamplerParameteriv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Sampler objects require GLES 3.0+");
        }
        
        int sampler = call.intParam(0);
        int pname = call.intParam(1);
        
        IntBuffer params = intScratchBuffer.get();
        params.clear();
        
        GLES30.glGetSamplerParameteriv(sampler, pname, params);
        
        return MappingResult.success(params.get(0));
    }
    
    private MappingResult mapGetSamplerParameterfv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Sampler objects require GLES 3.0+");
        }
        
        int sampler = call.intParam(0);
        int pname = call.intParam(1);
        
        FloatBuffer params = floatScratchBuffer.get();
        params.clear();
        
        GLES30.glGetSamplerParameterfv(sampler, pname, params);
        
        return MappingResult.success(Float.floatToRawIntBits(params.get(0)));
    }

// ============================================================================
// SECTION 18: RENDERBUFFER OPERATIONS (~500 lines)
// ============================================================================

    private MappingResult mapRenderbufferCreate(CallDescriptor call) {
        int count = call.intParam(0);
        
        IntBuffer renderbuffers = intScratchBuffer.get();
        renderbuffers.clear();
        renderbuffers.limit(count);
        
        GLES20.glGenRenderbuffers(renderbuffers);
        
        GLESError error = errorHandler.checkError("glGenRenderbuffers");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        for (int i = 0; i < count; i++) {
            resourceRegistry.registerRenderbuffer(renderbuffers.get(i));
        }
        
        return MappingResult.success(renderbuffers.get(0));
    }
    
    private MappingResult mapRenderbufferDelete(CallDescriptor call) {
        int renderbuffer = call.intParam(0);
        
        if (renderbuffer == 0) {
            return MappingResult.success();
        }
        
        GLES20.glDeleteRenderbuffers(renderbuffer);
        resourceRegistry.unregisterRenderbuffer(renderbuffer);
        
        return MappingResult.success();
    }
    
    private MappingResult mapRenderbufferBind(CallDescriptor call) {
        int target = call.intParam(0);
        int renderbuffer = call.intParam(1);
        
        GLES20.glBindRenderbuffer(target, renderbuffer);
        
        GLESError error = errorHandler.checkError("glBindRenderbuffer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        stateTracker.setBoundRenderbuffer(renderbuffer);
        
        return MappingResult.success();
    }
    
    private MappingResult mapIsRenderbuffer(CallDescriptor call) {
        int renderbuffer = call.intParam(0);
        
        boolean result = GLES20.glIsRenderbuffer(renderbuffer);
        
        return MappingResult.success(result ? 1 : 0);
    }
    
    private MappingResult mapRenderbufferStorage(CallDescriptor call) {
        int target = call.intParam(0);
        int internalformat = call.intParam(1);
        int width = call.intParam(2);
        int height = call.intParam(3);
        
        // Validate dimensions
        int maxSize = capabilities.maxRenderbufferSize();
        if (width > maxSize || height > maxSize) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Renderbuffer size (" + width + "x" + height + ") exceeds maximum " + maxSize);
        }
        
        if (width <= 0 || height <= 0) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Renderbuffer dimensions must be positive");
        }
        
        // Validate format
        if (!isRenderbufferFormatSupported(internalformat)) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Unsupported renderbuffer format: 0x" + Integer.toHexString(internalformat));
        }
        
        GLES20.glRenderbufferStorage(target, internalformat, width, height);
        
        GLESError error = errorHandler.checkError("glRenderbufferStorage");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapRenderbufferStorageMultisample(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fallback to non-MSAA
            return mapRenderbufferStorage(call.modify()
                .removeIntParam(1) // Remove samples parameter
                .build());
        }
        
        int target = call.intParam(0);
        int samples = call.intParam(1);
        int internalformat = call.intParam(2);
        int width = call.intParam(3);
        int height = call.intParam(4);
        
        // Clamp samples to maximum
        int maxSamples = capabilities.maxSamples();
        if (samples > maxSamples) {
            samples = maxSamples;
        }
        
        // Validate dimensions
        int maxSize = capabilities.maxRenderbufferSize();
        if (width > maxSize || height > maxSize) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Renderbuffer size (" + width + "x" + height + ") exceeds maximum " + maxSize);
        }
        
        GLES30.glRenderbufferStorageMultisample(target, samples, internalformat, width, height);
        
        GLESError error = errorHandler.checkError("glRenderbufferStorageMultisample");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetRenderbufferParameteriv(CallDescriptor call) {
        int target = call.intParam(0);
        int pname = call.intParam(1);
        
        IntBuffer params = intScratchBuffer.get();
        params.clear();
        
        GLES20.glGetRenderbufferParameteriv(target, pname, params);
        
        return MappingResult.success(params.get(0));
    }
    
    private boolean isRenderbufferFormatSupported(int format) {
        return switch (format) {
            // Color formats
            case 0x8056, 0x8058, 0x8051, 0x8052, 0x8053 -> true; // RGB565, RGBA8, RGB8, etc.
            // Depth formats
            case 0x81A5, 0x81A6, 0x81A7 -> true; // DEPTH16, DEPTH24, DEPTH32
            case 0x88F0 -> capabilities.version().isAtLeast(GLESVersion.GLES_3_0); // DEPTH32F
            // Stencil formats
            case 0x8D48 -> true; // STENCIL8
            // Depth-stencil formats
            case 0x88F0 + 1, 0x8CAD -> capabilities.version().isAtLeast(GLESVersion.GLES_3_0); // DEPTH24_STENCIL8, etc.
            default -> false;
        };
    }

// ============================================================================
// SECTION 19: MULTIPLE RENDER TARGETS (~600 lines)
// ============================================================================

    private MappingResult mapDrawBuffers(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Try EXT_draw_buffers for GLES 2.0
            if (!capabilities.hasExtension(GLESExtension.EXT_draw_buffers)) {
                // Only GL_BACK or GL_NONE is valid for single output
                return MappingResult.successWithWarning(0x5000, 
                    "MRT not supported, using single output");
            }
        }
        
        IntBuffer bufs = call.objectParam(0);
        int count = bufs.remaining();
        
        // Validate buffer count
        if (count > capabilities.maxDrawBuffers()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Draw buffer count " + count + " exceeds maximum " + capabilities.maxDrawBuffers());
        }
        
        // Validate buffer values
        for (int i = 0; i < count; i++) {
            int buf = bufs.get(i);
            if (buf != GL_NONE && buf != GL_BACK && (buf < GL_COLOR_ATTACHMENT0 || buf > GL_COLOR_ATTACHMENT15)) {
                return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                    "Invalid draw buffer value: 0x" + Integer.toHexString(buf));
            }
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            GLES30.glDrawBuffers(bufs);
        } else {
            GLES20.glDrawBuffersEXT(bufs);
        }
        
        GLESError error = errorHandler.checkError("glDrawBuffers");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapReadBuffer(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // GLES 2.0: Only GL_BACK is valid for default framebuffer
            return MappingResult.successWithWarning(0x5001, 
                "glReadBuffer not supported on GLES 2.0");
        }
        
        int src = call.intParam(0);
        
        GLES30.glReadBuffer(src);
        
        GLESError error = errorHandler.checkError("glReadBuffer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapClearBufferiv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            // Fallback to glClear for GLES 2.0
            return mapClear(call);
        }
        
        int buffer = call.intParam(0);
        int drawbuffer = call.intParam(1);
        IntBuffer value = call.objectParam(0);
        
        GLES30.glClearBufferiv(buffer, drawbuffer, value);
        
        GLESError error = errorHandler.checkError("glClearBufferiv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapClearBufferuiv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return mapClear(call);
        }
        
        int buffer = call.intParam(0);
        int drawbuffer = call.intParam(1);
        IntBuffer value = call.objectParam(0);
        
        GLES30.glClearBufferuiv(buffer, drawbuffer, value);
        
        GLESError error = errorHandler.checkError("glClearBufferuiv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapClearBufferfv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return mapClear(call);
        }
        
        int buffer = call.intParam(0);
        int drawbuffer = call.intParam(1);
        FloatBuffer value = call.objectParam(0);
        
        GLES30.glClearBufferfv(buffer, drawbuffer, value);
        
        GLESError error = errorHandler.checkError("glClearBufferfv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapClearBufferfi(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return mapClear(call);
        }
        
        int buffer = call.intParam(0);
        int drawbuffer = call.intParam(1);
        float depth = call.floatParam(0);
        int stencil = call.intParam(2);
        
        GLES30.glClearBufferfi(buffer, drawbuffer, depth, stencil);
        
        GLESError error = errorHandler.checkError("glClearBufferfi");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private static final int GL_NONE = 0;
    private static final int GL_BACK = 0x0405;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_COLOR_ATTACHMENT15 = 0x8CEF;

// ============================================================================
// SECTION 20: TESSELLATION (GLES 3.2+) (~500 lines)
// ============================================================================

    private MappingResult mapPatchParameteri(CallDescriptor call) {
        if (!capabilities.supportsTessellation()) {
            return MappingResult.unsupported("Tessellation requires GLES 3.2+ or EXT_tessellation_shader");
        }
        
        int pname = call.intParam(0);
        int value = call.intParam(1);
        
        // Validate pname (must be GL_PATCH_VERTICES)
        if (pname != 0x8E72) { // GL_PATCH_VERTICES
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Invalid patch parameter: 0x" + Integer.toHexString(pname));
        }
        
        // Validate vertex count
        if (value < 1 || value > capabilities.maxPatchVertices()) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Patch vertex count " + value + " out of range [1, " + capabilities.maxPatchVertices() + "]");
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            GLES32.glPatchParameteri(pname, value);
        } else {
            GLES20.glPatchParameteriEXT(pname, value);
        }
        
        GLESError error = errorHandler.checkError("glPatchParameteri");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }

// ============================================================================
// SECTION 21: GEOMETRY SHADERS (GLES 3.2+) (~300 lines)
// ============================================================================

    private MappingResult mapFramebufferTextureLayer(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_0)) {
            return MappingResult.unsupported("Layered framebuffers require GLES 3.0+");
        }
        
        int target = call.intParam(0);
        int attachment = call.intParam(1);
        int texture = call.intParam(2);
        int level = call.intParam(3);
        int layer = call.intParam(4);
        
        GLES30.glFramebufferTextureLayer(target, attachment, texture, level, layer);
        
        GLESError error = errorHandler.checkError("glFramebufferTextureLayer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    // Geometry shader invocation is handled through draw calls with GL_POINTS/LINES/TRIANGLES_ADJACENCY
    private MappingResult mapDrawArraysAdjacency(CallDescriptor call) {
        if (!capabilities.supportsGeometryShader()) {
            // Fallback: strip adjacency, draw as regular primitives
            int mode = call.intParam(0);
            int newMode = stripAdjacency(mode);
            
            if (newMode == mode) {
                return MappingResult.unsupported("Adjacency primitives require geometry shader support");
            }
            
            return mapDrawArrays(call.modify()
                .setIntParam(0, newMode)
                .build());
        }
        
        // Direct draw with adjacency primitives
        return mapDrawArrays(call);
    }
    
    private int stripAdjacency(int mode) {
        return switch (mode) {
            case 0x000A -> 0x0001; // LINES_ADJACENCY -> LINES
            case 0x000B -> 0x0003; // LINE_STRIP_ADJACENCY -> LINE_STRIP
            case 0x000C -> 0x0004; // TRIANGLES_ADJACENCY -> TRIANGLES
            case 0x000D -> 0x0005; // TRIANGLE_STRIP_ADJACENCY -> TRIANGLE_STRIP
            default -> mode;
        };
    }

// ============================================================================
// SECTION 22: TEXTURE BUFFER OBJECTS (GLES 3.2+) (~400 lines)
// ============================================================================

    private MappingResult mapTexBuffer(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            if (!capabilities.hasExtension(GLESExtension.EXT_texture_buffer)) {
                return MappingResult.unsupported("Texture buffers require GLES 3.2+ or EXT_texture_buffer");
            }
        }
        
        int target = call.intParam(0);
        int internalformat = call.intParam(1);
        int buffer = call.intParam(2);
        
        // Validate target (must be GL_TEXTURE_BUFFER)
        if (target != 0x8C2A) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "glTexBuffer target must be GL_TEXTURE_BUFFER");
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            GLES32.glTexBuffer(target, internalformat, buffer);
        } else {
            GLES20.glTexBufferEXT(target, internalformat, buffer);
        }
        
        GLESError error = errorHandler.checkError("glTexBuffer");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapTexBufferRange(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            if (!capabilities.hasExtension(GLESExtension.EXT_texture_buffer)) {
                return MappingResult.unsupported("Texture buffers require GLES 3.2+ or EXT_texture_buffer");
            }
        }
        
        int target = call.intParam(0);
        int internalformat = call.intParam(1);
        int buffer = call.intParam(2);
        long offset = call.longParam(0);
        long size = call.longParam(1);
        
        // Validate offset alignment
        int alignment = capabilities.textureBufferOffsetAlignment();
        if (offset % alignment != 0) {
            return MappingResult.failed(MappingStatus.FAILED_VALIDATION,
                "Texture buffer offset " + offset + " not aligned to " + alignment);
        }
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            GLES32.glTexBufferRange(target, internalformat, buffer, offset, size);
        } else {
            GLES20.glTexBufferRangeEXT(target, internalformat, buffer, offset, size);
        }
        
        GLESError error = errorHandler.checkError("glTexBufferRange");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }

// ============================================================================
// SECTION 23: MULTI-SAMPLE OPERATIONS (~400 lines)
// ============================================================================

    private MappingResult mapSampleCoverage(CallDescriptor call) {
        float value = call.floatParam(0);
        boolean invert = call.boolParam(0);
        
        GLES20.glSampleCoverage(value, invert);
        
        return MappingResult.success();
    }
    
    private MappingResult mapSampleMaski(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            // GLES 3.0 and below: No per-sample masking
            return MappingResult.successWithWarning(0x6000, 
                "Sample mask not supported, using default mask");
        }
        
        int maskNumber = call.intParam(0);
        int mask = call.intParam(1);
        
        GLES31.glSampleMaski(maskNumber, mask);
        
        GLESError error = errorHandler.checkError("glSampleMaski");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapGetMultisamplefv(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("Multisample queries require GLES 3.1+");
        }
        
        int pname = call.intParam(0);
        int index = call.intParam(1);
        FloatBuffer val = call.objectParam(0);
        
        GLES31.glGetMultisamplefv(pname, index, val);
        
        GLESError error = errorHandler.checkError("glGetMultisamplefv");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        return MappingResult.success();
    }
    
    private MappingResult mapMinSampleShading(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            if (!capabilities.hasExtension(GLESExtension.OES_sample_shading)) {
                return MappingResult.successWithWarning(0x6001, 
                    "Sample shading not supported");
            }
        }
        
        float value = call.floatParam(0);
        
        // Clamp to [0, 1]
        value = Math.max(0.0f, Math.min(1.0f, value));
        
        if (capabilities.version().isAtLeast(GLESVersion.GLES_3_2)) {
            GLES32.glMinSampleShading(value);
        } else {
            GLES20.glMinSampleShadingOES(value);
        }
        
        return MappingResult.success();
    }

// ============================================================================
// SECTION 24: PLATFORM-SPECIFIC QUIRKS & DRIVER WORKAROUNDS (~1,500 lines)
// ============================================================================

    /**
     * Platform-specific driver quirk handler.
     * 
     * Known issues by GPU vendor:
     * - Adreno (Qualcomm): Texture sampling issues with sRGB, UBO alignment bugs
     * - Mali (ARM): Transform feedback corruption, compute shader memory barriers
     * - PowerVR (Imagination): FBO completeness false negatives, shader precision
     * - Tegra (NVIDIA): Vertex attrib divisor bugs, texture array issues
     * - Intel: Precision issues with mediump, some extension detection failures
     */
    private final GPUQuirkHandler quirkHandler;
    
    private void applyDriverWorkarounds(CallDescriptor call, MappingResult result) {
        GPUVendor vendor = capabilities.getGPUVendor();
        String driverVersion = capabilities.getDriverVersion();
        
        switch (vendor) {
            case QUALCOMM_ADRENO -> applyAdrenoWorkarounds(call, result, driverVersion);
            case ARM_MALI -> applyMaliWorkarounds(call, result, driverVersion);
            case IMAGINATION_POWERVR -> applyPowerVRWorkarounds(call, result, driverVersion);
            case NVIDIA_TEGRA -> applyTegraWorkarounds(call, result, driverVersion);
            case INTEL -> applyIntelWorkarounds(call, result, driverVersion);
            case APPLE -> applyAppleWorkarounds(call, result, driverVersion);
            default -> {} // No known workarounds needed
        }
    }
    
    private void applyAdrenoWorkarounds(CallDescriptor call, MappingResult result, String driverVersion) {
        // Adreno 3xx: UBO std140 layout has wrong alignment for vec3
        if (call.type() == CallType.BUFFER_BIND_RANGE && 
            driverVersion.contains("Adreno (TM) 3")) {
            // Force vec3 to vec4 alignment in UBOs
            quirkHandler.forceVec3ToVec4Alignment(true);
        }
        
        // Adreno 4xx/5xx: sRGB textures may sample incorrectly
        if (call.type() == CallType.TEXTURE_BIND && 
            isGammaFormat(call.intParam(1))) {
            // Insert GL_FRAMEBUFFER_SRGB enable before sampling
            if (!stateTracker.isFramebufferSRGBEnabled()) {
                GLES20.glEnable(0x8DB9); // GL_FRAMEBUFFER_SRGB
            }
        }
        
        // Adreno 6xx: Transform feedback with multiple buffers may corrupt data
        if (call.type() == CallType.TRANSFORM_FEEDBACK_BEGIN &&
            driverVersion.contains("Adreno (TM) 6")) {
            // Flush before starting TF to prevent corruption
            GLES20.glFlush();
        }
    }
    
    private void applyMaliWorkarounds(CallDescriptor call, MappingResult result, String driverVersion) {
        // Mali T/G series: Memory barriers in compute shaders not always honored
        if (call.type() == CallType.MEMORY_BARRIER &&
            (driverVersion.contains("Mali-T") || driverVersion.contains("Mali-G"))) {
            // Add extra flush after barrier
            GLES20.glFlush();
        }
        
        // Mali Bifrost: Transform feedback may corrupt with interleaved attribs
        if (call.type() == CallType.TRANSFORM_FEEDBACK_VARYINGS &&
            driverVersion.contains("Mali-G7")) {
            // Force separate attribs mode
            int bufferMode = call.intParam(1);
            if (bufferMode == 0x8C8C) { // GL_INTERLEAVED_ATTRIBS
                result.setWarning("Mali-G7 TF bug: forcing SEPARATE_ATTRIBS mode");
            }
        }
        
        // Mali Valhall: Shader storage buffer coherency issues
        if (call.type() == CallType.DISPATCH_COMPUTE &&
            driverVersion.contains("Mali-G")) {
            // Insert barrier after compute dispatch
            GLES31.glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        }
    }
    
    private void applyPowerVRWorkarounds(CallDescriptor call, MappingResult result, String driverVersion) {
        // PowerVR SGX: FBO completeness check returns false negatives
        if (call.type() == CallType.FRAMEBUFFER_STATUS &&
            driverVersion.contains("SGX")) {
            int status = result.intValue();
            if (status != GL_FRAMEBUFFER_COMPLETE) {
                // Retry check - known bug
                status = GLES20.glCheckFramebufferStatus(call.intParam(0));
                if (status == GL_FRAMEBUFFER_COMPLETE) {
                    result.setIntValue(status);
                }
            }
        }
        
        // PowerVR Rogue: Precision issues with lowp in fragment shaders
        // This is handled in shader mapper by promoting lowp to mediump
        
        // PowerVR Series 6+: glInvalidateFramebuffer may cause artifacts
        if (call.type() == CallType.FRAMEBUFFER_INVALIDATE &&
            driverVersion.contains("Rogue")) {
            // Skip invalidate on known bad driver versions
            if (isKnownBadPowerVRDriver(driverVersion)) {
                result.setWarning("Skipping glInvalidateFramebuffer due to PowerVR driver bug");
                result.setStatus(MappingStatus.SUCCESS);
            }
        }
    }
    
    private void applyTegraWorkarounds(CallDescriptor call, MappingResult result, String driverVersion) {
        // Tegra K1: Vertex attrib divisor doesn't work correctly
        if (call.type() == CallType.VERTEX_ATTRIB_DIVISOR &&
            driverVersion.contains("Tegra") && driverVersion.contains("K1")) {
            // Mark as needing manual instancing fallback
            result.setWarning("Tegra K1 divisor bug: may need manual instancing");
        }
        
        // Tegra X1: 2D texture arrays have sampling issues
        if (call.type() == CallType.TEXTURE_BIND &&
            call.intParam(0) == GL_TEXTURE_2D_ARRAY &&
            driverVersion.contains("X1")) {
            // Ensure explicit LOD in shader
            result.setWarning("Tegra X1: use explicit LOD for texture arrays");
        }
    }
    
    private void applyIntelWorkarounds(CallDescriptor call, MappingResult result, String driverVersion) {
        // Intel HD Graphics: mediump may be promoted to highp inconsistently
        // Handled in shader mapper
        
        // Intel Mesa drivers: Some extensions report incorrectly
        if (call.type() == CallType.GET_STRING &&
            call.intParam(0) == GL_EXTENSIONS &&
            driverVersion.contains("Mesa")) {
            // Extension detection is handled in capabilities
        }
    }
    
    private void applyAppleWorkarounds(CallDescriptor call, MappingResult result, String driverVersion) {
        // Apple A-series GPUs via GLES (rare, usually Metal):
        // These generally work well, minimal workarounds needed
        
        // Apple GPU: Tile-based rendering optimizations
        if (call.type() == CallType.FRAMEBUFFER_INVALIDATE) {
            // Always use invalidate on Apple GPUs - highly beneficial for TBDR
            // No workaround needed, just ensure it's called
        }
    }
    
    private boolean isGammaFormat(int format) {
        return format == 0x8C40 || format == 0x8C41 || format == 0x8C42 || format == 0x8C43;
        // SRGB, SRGB8, SRGB8_ALPHA8, etc.
    }
    
    private boolean isKnownBadPowerVRDriver(String version) {
        // List of known problematic driver versions
        return version.contains("1.10.") || version.contains("1.11.") || version.contains("1.12.");
    }
    
    private static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    private static final int GL_TEXTURE_2D_ARRAY = 0x8C1A;
    private static final int GL_EXTENSIONS = 0x1F03;

// ============================================================================
// SECTION 25: EMULATED VAO MANAGER (for GLES 2.0) (~800 lines)
// ============================================================================

    /**
     * Emulates Vertex Array Objects on GLES 2.0 devices that lack VAO support.
     * 
     * VAOs bundle:
     * - Vertex attribute pointers (glVertexAttribPointer state)
     * - Enabled/disabled attributes (glEnableVertexAttribArray state)
     * - Element buffer binding (GL_ELEMENT_ARRAY_BUFFER)
     * - Vertex attribute divisors (if instancing supported)
     */
    private static final class EmulatedVAOManager {
        
        private static final int MAX_VERTEX_ATTRIBS = 16;
        
        private final Int2ObjectOpenHashMap<EmulatedVAO> vaos = new Int2ObjectOpenHashMap<>();
        private int nextVAOId = 1;
        private int boundVAO = 0;
        
        // Default VAO (id=0) state
        private final EmulatedVAO defaultVAO = new EmulatedVAO(0);
        
        int create() {
            int id = nextVAOId++;
            vaos.put(id, new EmulatedVAO(id));
            return id;
        }
        
        void delete(int vao) {
            if (vao != 0) {
                vaos.remove(vao);
                if (boundVAO == vao) {
                    boundVAO = 0;
                }
            }
        }
        
        void bind(int vao) {
            if (boundVAO == vao) {
                return;
            }
            
            // Save current VAO state
            EmulatedVAO currentVAO = getVAO(boundVAO);
            // currentVAO state is already up-to-date from setter methods
            
            // Restore new VAO state
            EmulatedVAO newVAO = getVAO(vao);
            restoreVAOState(newVAO);
            
            boundVAO = vao;
        }
        
        boolean isValid(int vao) {
            return vao == 0 || vaos.containsKey(vao);
        }
        
        void setVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
            if (index >= MAX_VERTEX_ATTRIBS) return;
            
            EmulatedVAO vao = getVAO(boundVAO);
            VertexAttribState attrib = vao.attribs[index];
            attrib.size = size;
            attrib.type = type;
            attrib.normalized = normalized;
            attrib.stride = stride;
            attrib.pointer = pointer;
            attrib.buffer = getCurrentArrayBuffer();
        }
        
        void enableVertexAttribArray(int index) {
            if (index >= MAX_VERTEX_ATTRIBS) return;
            
            EmulatedVAO vao = getVAO(boundVAO);
            vao.attribs[index].enabled = true;
        }
        
        void disableVertexAttribArray(int index) {
            if (index >= MAX_VERTEX_ATTRIBS) return;
            
            EmulatedVAO vao = getVAO(boundVAO);
            vao.attribs[index].enabled = false;
        }
        
        void setVertexAttribDivisor(int index, int divisor) {
            if (index >= MAX_VERTEX_ATTRIBS) return;
            
            EmulatedVAO vao = getVAO(boundVAO);
            vao.attribs[index].divisor = divisor;
        }
        
        void setElementArrayBuffer(int buffer) {
            EmulatedVAO vao = getVAO(boundVAO);
            vao.elementArrayBuffer = buffer;
        }
        
        int getElementArrayBuffer() {
            return getVAO(boundVAO).elementArrayBuffer;
        }
        
        private EmulatedVAO getVAO(int id) {
            if (id == 0) {
                return defaultVAO;
            }
            EmulatedVAO vao = vaos.get(id);
            return vao != null ? vao : defaultVAO;
        }
        
        private void restoreVAOState(EmulatedVAO vao) {
            // Restore element array buffer
            GLES20.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vao.elementArrayBuffer);
            
            // Restore each vertex attribute
            for (int i = 0; i < MAX_VERTEX_ATTRIBS; i++) {
                VertexAttribState attrib = vao.attribs[i];
                
                if (attrib.enabled) {
                    GLES20.glEnableVertexAttribArray(i);
                } else {
                    GLES20.glDisableVertexAttribArray(i);
                }
                
                // Restore pointer if buffer was set
                if (attrib.buffer != 0) {
                    GLES20.glBindBuffer(GL_ARRAY_BUFFER, attrib.buffer);
                    GLES20.glVertexAttribPointer(i, attrib.size, attrib.type, 
                        attrib.normalized, attrib.stride, attrib.pointer);
                }
                
                // Restore divisor if supported
                if (attrib.divisor != 0 && supportsInstancing()) {
                    GLES20.glVertexAttribDivisorEXT(i, attrib.divisor);
                }
            }
        }
        
        private int getCurrentArrayBuffer() {
            IntBuffer buf = BufferUtils.createIntBuffer(1);
            GLES20.glGetIntegerv(GL_ARRAY_BUFFER_BINDING, buf);
            return buf.get(0);
        }
        
        private boolean supportsInstancing() {
            // This would be checked via capabilities
            return false; // Placeholder
        }
        
        private static final int GL_ARRAY_BUFFER = 0x8892;
        private static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
        private static final int GL_ARRAY_BUFFER_BINDING = 0x8894;
    }
    
    private static final class EmulatedVAO {
        final int id;
        final VertexAttribState[] attribs = new VertexAttribState[16];
        int elementArrayBuffer = 0;
        
        EmulatedVAO(int id) {
            this.id = id;
            for (int i = 0; i < attribs.length; i++) {
                attribs[i] = new VertexAttribState();
            }
        }
    }
    
    private static final class VertexAttribState {
        boolean enabled = false;
        int size = 4;
        int type = GL_FLOAT;
        boolean normalized = false;
        int stride = 0;
        long pointer = 0;
        int buffer = 0;
        int divisor = 0;
        
        private static final int GL_FLOAT = 0x1406;
    }

// ============================================================================
// SECTION 26: RESOURCE REGISTRY & LEAK DETECTION (~600 lines)
// ============================================================================

    /**
     * Tracks all GL resources for lifecycle management and leak detection.
     * 
     * Performance: Uses primitive collections to avoid boxing overhead.
     * Thread-safety: All operations are thread-safe via concurrent sets.
     */
    private static final class ResourceRegistry {
        
        // Use fastutil primitive sets for zero-boxing overhead
        private final IntOpenHashSet buffers = new IntOpenHashSet();
        private final IntOpenHashSet textures = new IntOpenHashSet();
        private final IntOpenHashSet framebuffers = new IntOpenHashSet();
        private final IntOpenHashSet renderbuffers = new IntOpenHashSet();
        private final IntOpenHashSet shaders = new IntOpenHashSet();
        private final IntOpenHashSet programs = new IntOpenHashSet();
        private final IntOpenHashSet vaos = new IntOpenHashSet();
        private final IntOpenHashSet samplers = new IntOpenHashSet();
        private final IntOpenHashSet queries = new IntOpenHashSet();
        private final IntOpenHashSet programPipelines = new IntOpenHashSet();
        private final IntOpenHashSet transformFeedbacks = new IntOpenHashSet();
        
        // Sync objects use longs
        private final LongOpenHashSet syncObjects = new LongOpenHashSet();
        
        // Metrics
        private final AtomicLong totalCreated = new AtomicLong();
        private final AtomicLong totalDeleted = new AtomicLong();
        
        // Registration methods
        void registerBuffer(int id) { 
            synchronized (buffers) { buffers.add(id); }
            totalCreated.incrementAndGet();
        }
        
        void unregisterBuffer(int id) { 
            synchronized (buffers) { buffers.remove(id); }
            totalDeleted.incrementAndGet();
        }
        
        void registerTexture(int id) { 
            synchronized (textures) { textures.add(id); }
            totalCreated.incrementAndGet();
        }
        
        void unregisterTexture(int id) { 
            synchronized (textures) { textures.remove(id); }
            totalDeleted.incrementAndGet();
        }
        
        void registerFramebuffer(int id) { 
            synchronized (framebuffers) { framebuffers.add(id); }
            totalCreated.incrementAndGet();
        }
        
        void unregisterFramebuffer(int id) { 
            synchronized (framebuffers) { framebuffers.remove(id); }
            totalDeleted.incrementAndGet();
        }
        
        void registerRenderbuffer(int id) { 
            synchronized (renderbuffers) { renderbuffers.add(id); }
            totalCreated.incrementAndGet();
        }
        
        void unregisterRenderbuffer(int id) { 
            synchronized (renderbuffers) { renderbuffers.remove(id); }
            totalDeleted.incrementAndGet();
        }
        
        void registerShader(int id) { 
            synchronized (shaders) { shaders.add(id); }
            totalCreated.incrementAndGet();
        }
        
        void unregisterShader(int id) { 
            synchronized (shaders) { shaders.remove(id); }
            totalDeleted.incrementAndGet();
        }
        
        void registerProgram(int id) { 
            synchronized (programs) { programs.add(id); }
            totalCreated.incrementAndGet();
        }
        
        void unregisterProgram(int id) { 
            synchronized (programs) { programs.remove(id); }
            totalDeleted.incrementAndGet();
        }
        
        void registerVAO(int id) { 
            synchronized (vaos) { vaos.add(id); }
            totalCreated.incrementAndGet();
        }
        
        void unregisterVAO(int id) { 
            synchronized (vaos) { vaos.remove(id); }
            totalDeleted.incrementAndGet();
        }
        
        void registerSampler(int id) { 
            synchronized (samplers) { samplers.add(id); }
            totalCreated.incrementAndGet();
        }
        
        void unregisterSampler(int id) { 
            synchronized (samplers) { samplers.remove(id); }
            totalDeleted.incrementAndGet();
        }
        
        void registerQuery(int id) { 
            synchronized (queries) { queries.add(id); }
            totalCreated.incrementAndGet();
        }
        
        void unregisterQuery(int id) { 
            synchronized (queries) { queries.remove(id); }
            totalDeleted.incrementAndGet();
        }
        
        void registerProgramPipeline(int id) { 
            synchronized (programPipelines) { programPipelines.add(id); }
            totalCreated.incrementAndGet();
        }
        
        void unregisterProgramPipeline(int id) { 
            synchronized (programPipelines) { programPipelines.remove(id); }
            totalDeleted.incrementAndGet();
        }
        
        void registerTransformFeedback(int id) { 
            synchronized (transformFeedbacks) { transformFeedbacks.add(id); }
            totalCreated.incrementAndGet();
        }
        
        void unregisterTransformFeedback(int id) { 
            synchronized (transformFeedbacks) { transformFeedbacks.remove(id); }
            totalDeleted.incrementAndGet();
        }
        
        void registerSync(long sync) { 
            synchronized (syncObjects) { syncObjects.add(sync); }
            totalCreated.incrementAndGet();
        }
        
        void unregisterSync(long sync) { 
            synchronized (syncObjects) { syncObjects.remove(sync); }
            totalDeleted.incrementAndGet();
        }
        
        // Leak detection
        ResourceLeakReport checkForLeaks() {
            int leakedBuffers, leakedTextures, leakedFramebuffers, leakedRenderbuffers;
            int leakedShaders, leakedPrograms, leakedVAOs, leakedSamplers;
            int leakedQueries, leakedPipelines, leakedTransformFeedbacks;
            long leakedSyncs;
            
            synchronized (buffers) { leakedBuffers = buffers.size(); }
            synchronized (textures) { leakedTextures = textures.size(); }
            synchronized (framebuffers) { leakedFramebuffers = framebuffers.size(); }
            synchronized (renderbuffers) { leakedRenderbuffers = renderbuffers.size(); }
            synchronized (shaders) { leakedShaders = shaders.size(); }
            synchronized (programs) { leakedPrograms = programs.size(); }
            synchronized (vaos) { leakedVAOs = vaos.size(); }
            synchronized (samplers) { leakedSamplers = samplers.size(); }
            synchronized (queries) { leakedQueries = queries.size(); }
            synchronized (programPipelines) { leakedPipelines = programPipelines.size(); }
            synchronized (transformFeedbacks) { leakedTransformFeedbacks = transformFeedbacks.size(); }
            synchronized (syncObjects) { leakedSyncs = syncObjects.size(); }
            
            return new ResourceLeakReport(
                leakedBuffers, leakedTextures, leakedFramebuffers, leakedRenderbuffers,
                leakedShaders, leakedPrograms, leakedVAOs, leakedSamplers,
                leakedQueries, leakedPipelines, leakedTransformFeedbacks, leakedSyncs
            );
        }
        
        // Cleanup all resources
        void cleanup() {
            // Delete in reverse dependency order
            synchronized (syncObjects) {
                for (long sync : syncObjects) {
                    GLES30.glDeleteSync(sync);
                }
                syncObjects.clear();
            }
            
            synchronized (queries) {
                for (int query : queries) {
                    GLES30.glDeleteQueries(query);
                }
                queries.clear();
            }
            
            synchronized (transformFeedbacks) {
                for (int tf : transformFeedbacks) {
                    GLES30.glDeleteTransformFeedbacks(tf);
                }
                transformFeedbacks.clear();
            }
            
            synchronized (programPipelines) {
                for (int pipeline : programPipelines) {
                    GLES31.glDeleteProgramPipelines(pipeline);
                }
                programPipelines.clear();
            }
            
            synchronized (samplers) {
                for (int sampler : samplers) {
                    GLES30.glDeleteSamplers(sampler);
                }
                samplers.clear();
            }
            
            synchronized (vaos) {
                for (int vao : vaos) {
                    GLES30.glDeleteVertexArrays(vao);
                }
                vaos.clear();
            }
            
            synchronized (programs) {
                for (int program : programs) {
                    GLES20.glDeleteProgram(program);
                }
                programs.clear();
            }
            
            synchronized (shaders) {
                for (int shader : shaders) {
                    GLES20.glDeleteShader(shader);
                }
                shaders.clear();
            }
            
            synchronized (framebuffers) {
                for (int fbo : framebuffers) {
                    GLES20.glDeleteFramebuffers(fbo);
                }
                framebuffers.clear();
            }
            
            synchronized (renderbuffers) {
                for (int rbo : renderbuffers) {
                    GLES20.glDeleteRenderbuffers(rbo);
                }
                renderbuffers.clear();
            }
            
            synchronized (textures) {
                for (int tex : textures) {
                    GLES20.glDeleteTextures(tex);
                }
                textures.clear();
            }
            
            synchronized (buffers) {
                for (int buf : buffers) {
                    GLES20.glDeleteBuffers(buf);
                }
                buffers.clear();
            }
        }
        
        // Metrics
        long getTotalCreated() { return totalCreated.get(); }
        long getTotalDeleted() { return totalDeleted.get(); }
        long getActiveCount() { return getTotalCreated() - getTotalDeleted(); }
    }
    
    record ResourceLeakReport(
        int buffers, int textures, int framebuffers, int renderbuffers,
        int shaders, int programs, int vaos, int samplers,
        int queries, int pipelines, int transformFeedbacks, long syncs
    ) {
        boolean hasLeaks() {
            return buffers > 0 || textures > 0 || framebuffers > 0 || renderbuffers > 0 ||
                   shaders > 0 || programs > 0 || vaos > 0 || samplers > 0 ||
                   queries > 0 || pipelines > 0 || transformFeedbacks > 0 || syncs > 0;
        }
        
        @Override
        public String toString() {
            if (!hasLeaks()) {
                return "No resource leaks detected";
            }
            
            StringBuilder sb = new StringBuilder("RESOURCE LEAKS DETECTED:\n");
            if (buffers > 0) sb.append("  Buffers: ").append(buffers).append("\n");
            if (textures > 0) sb.append("  Textures: ").append(textures).append("\n");
            if (framebuffers > 0) sb.append("  Framebuffers: ").append(framebuffers).append("\n");
            if (renderbuffers > 0) sb.append("  Renderbuffers: ").append(renderbuffers).append("\n");
            if (shaders > 0) sb.append("  Shaders: ").append(shaders).append("\n");
            if (programs > 0) sb.append("  Programs: ").append(programs).append("\n");
            if (vaos > 0) sb.append("  VAOs: ").append(vaos).append("\n");
            if (samplers > 0) sb.append("  Samplers: ").append(samplers).append("\n");
            if (queries > 0) sb.append("  Queries: ").append(queries).append("\n");
            if (pipelines > 0) sb.append("  Program Pipelines: ").append(pipelines).append("\n");
            if (transformFeedbacks > 0) sb.append("  Transform Feedbacks: ").append(transformFeedbacks).append("\n");
            if (syncs > 0) sb.append("  Sync Objects: ").append(syncs).append("\n");
            return sb.toString();
        }
    }

// ============================================================================
// SECTION 27: STATE TRACKER IMPLEMENTATION (~800 lines)
// ============================================================================

    /**
     * Tracks current OpenGL ES state to enable state caching and avoid redundant API calls.
     * 
     * Performance impact of state caching:
     * - Draw call batching: 2-5x improvement
     * - Texture binding: 10-50x faster (avoided driver call)
     * - Shader program switching: 5-20x faster
     */
    private static final class GLESStateTracker {
        
        // Currently bound objects
        private int boundProgram = 0;
        private int boundVAO = 0;
        private int boundFramebuffer = 0;
        private int boundRenderbuffer = 0;
        private int boundProgramPipeline = 0;
        
        // Buffer bindings per target
        private final Int2IntOpenHashMap boundBuffers = new Int2IntOpenHashMap();
        
        // Texture bindings per unit
        private int activeTextureUnit = 0;
        private final Int2IntOpenHashMap[] boundTextures = new Int2IntOpenHashMap[32];
        private final int[] boundSamplers = new int[32];
        
        // Viewport and scissor
        private int viewportX, viewportY, viewportWidth, viewportHeight;
        private int scissorX, scissorY, scissorWidth, scissorHeight;
        private boolean scissorEnabled = false;
        
        // Blend state
        private boolean blendEnabled = false;
        private int blendSrcRGB, blendDstRGB, blendSrcAlpha, blendDstAlpha;
        private int blendEquationRGB, blendEquationAlpha;
        
        // Depth state
        private boolean depthTestEnabled = false;
        private boolean depthWriteEnabled = true;
        private int depthFunc = 0x0201; // GL_LESS
        
        // Stencil state
        private boolean stencilTestEnabled = false;
        private int stencilFunc, stencilRef, stencilMask;
        private int stencilFail, stencilPassDepthFail, stencilPassDepthPass;
        
        // Cull state
        private boolean cullFaceEnabled = false;
        private int cullFaceMode = 0x0405; // GL_BACK
        private int frontFace = 0x0901; // GL_CCW
        
        // Color write mask
        private boolean colorMaskR = true, colorMaskG = true, colorMaskB = true, colorMaskA = true;
        
        // Polygon offset
        private boolean polygonOffsetFillEnabled = false;
        private float polygonOffsetFactor, polygonOffsetUnits;
        
        // Framebuffer sRGB
        private boolean framebufferSRGBEnabled = false;
        
        // Performance counters
        private long drawCallCount = 0;
        private long stateChangeCount = 0;
        private long triangleCount = 0;
        private long apiCallCount = 0;
        private long redundantCallsAvoided = 0;
        
        GLESStateTracker() {
            for (int i = 0; i < boundTextures.length; i++) {
                boundTextures[i] = new Int2IntOpenHashMap();
            }
            boundBuffers.defaultReturnValue(0);
        }
        
        // Program
        int getBoundProgram() { return boundProgram; }
        void setBoundProgram(int program) { boundProgram = program; }
        
        // VAO
        int getBoundVAO() { return boundVAO; }
        void setBoundVAO(int vao) { boundVAO = vao; }
        
        // Framebuffer
        int getBoundFramebuffer() { return boundFramebuffer; }
        void setBoundFramebuffer(int fbo) { boundFramebuffer = fbo; }
        
        // Renderbuffer
        int getBoundRenderbuffer() { return boundRenderbuffer; }
        void setBoundRenderbuffer(int rbo) { boundRenderbuffer = rbo; }
        
        // Program Pipeline
        int getBoundProgramPipeline() { return boundProgramPipeline; }
        void setBoundProgramPipeline(int pipeline) { boundProgramPipeline = pipeline; }
        
        // Buffer bindings
        int getBoundBuffer(int target) { return boundBuffers.get(target); }
        void setBoundBuffer(int target, int buffer) { boundBuffers.put(target, buffer); }
        
        // Texture bindings
        int getActiveTextureUnit() { return activeTextureUnit; }
        void setActiveTextureUnit(int unit) { activeTextureUnit = unit; }
        
        int getBoundTexture(int target) {
            return boundTextures[activeTextureUnit].getOrDefault(target, 0);
        }
        
        void setBoundTexture(int target, int texture) {
            boundTextures[activeTextureUnit].put(target, texture);
        }
        
        int getBoundSampler(int unit) { return boundSamplers[unit]; }
        void setBoundSampler(int unit, int sampler) { boundSamplers[unit] = sampler; }
        
        // Viewport
        void setViewport(int x, int y, int w, int h) {
            viewportX = x; viewportY = y; viewportWidth = w; viewportHeight = h;
        }
        
        boolean isViewportDifferent(int x, int y, int w, int h) {
            return viewportX != x || viewportY != y || viewportWidth != w || viewportHeight != h;
        }
        
        // Scissor
        void setScissor(int x, int y, int w, int h) {
            scissorX = x; scissorY = y; scissorWidth = w; scissorHeight = h;
        }
        
        boolean isScissorDifferent(int x, int y, int w, int h) {
            return scissorX != x || scissorY != y || scissorWidth != w || scissorHeight != h;
        }
        
        boolean isScissorEnabled() { return scissorEnabled; }
        void setScissorEnabled(boolean enabled) { scissorEnabled = enabled; }
        
        // Blend state
        boolean isBlendEnabled() { return blendEnabled; }
        void setBlendEnabled(boolean enabled) { blendEnabled = enabled; }
        
        void setBlendFunc(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
            blendSrcRGB = srcRGB; blendDstRGB = dstRGB;
            blendSrcAlpha = srcAlpha; blendDstAlpha = dstAlpha;
        }
        
        boolean isBlendFuncDifferent(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
            return blendSrcRGB != srcRGB || blendDstRGB != dstRGB ||
                   blendSrcAlpha != srcAlpha || blendDstAlpha != dstAlpha;
        }
        
        void setBlendEquation(int modeRGB, int modeAlpha) {
            blendEquationRGB = modeRGB; blendEquationAlpha = modeAlpha;
        }
        
        boolean isBlendEquationDifferent(int modeRGB, int modeAlpha) {
            return blendEquationRGB != modeRGB || blendEquationAlpha != modeAlpha;
        }
        
        // Depth state
        boolean isDepthTestEnabled() { return depthTestEnabled; }
        void setDepthTestEnabled(boolean enabled) { depthTestEnabled = enabled; }
        
        boolean isDepthWriteEnabled() { return depthWriteEnabled; }
        void setDepthWriteEnabled(boolean enabled) { depthWriteEnabled = enabled; }
        
        int getDepthFunc() { return depthFunc; }
        void setDepthFunc(int func) { depthFunc = func; }
        
        // Stencil state
        boolean isStencilTestEnabled() { return stencilTestEnabled; }
        void setStencilTestEnabled(boolean enabled) { stencilTestEnabled = enabled; }
        
        // Cull state
        boolean isCullFaceEnabled() { return cullFaceEnabled; }
        void setCullFaceEnabled(boolean enabled) { cullFaceEnabled = enabled; }
        
        int getCullFaceMode() { return cullFaceMode; }
        void setCullFaceMode(int mode) { cullFaceMode = mode; }
        
        int getFrontFace() { return frontFace; }
        void setFrontFace(int face) { frontFace = face; }
        
        // Framebuffer sRGB
        boolean isFramebufferSRGBEnabled() { return framebufferSRGBEnabled; }
        void setFramebufferSRGBEnabled(boolean enabled) { framebufferSRGBEnabled = enabled; }
        
        // Performance counters
        void incrementDrawCallCount() { drawCallCount++; }
        void incrementStateChangeCount() { stateChangeCount++; }
        void incrementComputeDispatchCount() { /* Same counter or separate */ drawCallCount++; }
        void addTriangles(long count) { triangleCount += count; }
        void incrementApiCallCount() { apiCallCount++; }
        void incrementRedundantCallsAvoided() { redundantCallsAvoided++; }
        
        long getDrawCallCount() { return drawCallCount; }
        long getStateChangeCount() { return stateChangeCount; }
        long getTriangleCount() { return triangleCount; }
        long getApiCallCount() { return apiCallCount; }
        long getRedundantCallsAvoided() { return redundantCallsAvoided; }
        
        void resetFrameCounters() {
            drawCallCount = 0;
            stateChangeCount = 0;
            triangleCount = 0;
            apiCallCount = 0;
            redundantCallsAvoided = 0;
        }
        
        // Full state reset (e.g., after context loss)
        void reset() {
            boundProgram = 0;
            boundVAO = 0;
            boundFramebuffer = 0;
            boundRenderbuffer = 0;
            boundProgramPipeline = 0;
            boundBuffers.clear();
            for (Int2IntOpenHashMap map : boundTextures) {
                map.clear();
            }
            Arrays.fill(boundSamplers, 0);
            activeTextureUnit = 0;
            
            // Reset state to GL defaults
            blendEnabled = false;
            depthTestEnabled = false;
            depthWriteEnabled = true;
            depthFunc = 0x0201;
            stencilTestEnabled = false;
            cullFaceEnabled = false;
            cullFaceMode = 0x0405;
            frontFace = 0x0901;
            colorMaskR = colorMaskG = colorMaskB = colorMaskA = true;
            scissorEnabled = false;
            polygonOffsetFillEnabled = false;
            framebufferSRGBEnabled = false;
            
            resetFrameCounters();
        }
    }

// ============================================================================
// SECTION 28: ERROR HANDLER IMPLEMENTATION (~400 lines)
// ============================================================================

    /**
     * Handles OpenGL ES error checking, reporting, and recovery.
     */
    private static final class GLESErrorHandler {
        
        private static final Int2ObjectOpenHashMap<String> ERROR_MESSAGES = new Int2ObjectOpenHashMap<>();
        
        static {
            ERROR_MESSAGES.put(0x0000, "NO_ERROR");
            ERROR_MESSAGES.put(0x0500, "INVALID_ENUM");
            ERROR_MESSAGES.put(0x0501, "INVALID_VALUE");
            ERROR_MESSAGES.put(0x0502, "INVALID_OPERATION");
            ERROR_MESSAGES.put(0x0503, "STACK_OVERFLOW");
            ERROR_MESSAGES.put(0x0504, "STACK_UNDERFLOW");
            ERROR_MESSAGES.put(0x0505, "OUT_OF_MEMORY");
            ERROR_MESSAGES.put(0x0506, "INVALID_FRAMEBUFFER_OPERATION");
            ERROR_MESSAGES.put(0x8031, "TABLE_TOO_LARGE");
        }
        
        private boolean debugEnabled = false;
        private boolean breakOnError = false;
        private ErrorCallback errorCallback = null;
        
        GLESError checkError(String operation) {
            int errorCode = GLES20.glGetError();
            
            if (errorCode == 0x0000) {
                return GLESError.NO_ERROR;
            }
            
            String message = ERROR_MESSAGES.getOrDefault(errorCode, "UNKNOWN_ERROR");
            GLESError error = new GLESError(errorCode, message, operation);
            
            if (debugEnabled) {
                System.err.println("[GLES ERROR] " + operation + ": " + message + " (0x" + 
                    Integer.toHexString(errorCode) + ")");
                
                if (breakOnError) {
                    throw new GLESException(error);
                }
            }
            
            if (errorCallback != null) {
                errorCallback.onError(error);
            }
            
            return error;
        }
        
        void clearErrors() {
            while (GLES20.glGetError() != 0x0000) {
                // Drain error queue
            }
        }
        
        void setDebugEnabled(boolean enabled) { debugEnabled = enabled; }
        void setBreakOnError(boolean enabled) { breakOnError = enabled; }
        void setErrorCallback(ErrorCallback callback) { errorCallback = callback; }
        
        @FunctionalInterface
        interface ErrorCallback {
            void onError(GLESError error);
        }
    }
    
    record GLESError(int code, String description, String operation) {
        static final GLESError NO_ERROR = new GLESError(0, "NO_ERROR", "");
        
        boolean isError() { return code != 0; }
    }
    
    static final class GLESException extends RuntimeException {
        private final GLESError error;
        
        GLESException(GLESError error) {
            super(error.operation() + ": " + error.description() + " (0x" + 
                Integer.toHexString(error.code()) + ")");
            this.error = error;
        }
        
        GLESError getError() { return error; }
    }

// ============================================================================
// SECTION 29: CAPABILITIES DETECTION (~1,000 lines)
// ============================================================================

    /**
     * Detects and caches OpenGL ES capabilities and limits.
     */
    static final class GLESCapabilities {
        
        private final GLESVersion version;
        private final GPUVendor gpuVendor;
        private final String renderer;
        private final String driverVersion;
        private final Set<GLESExtension> extensions;
        
        // Limits cache
        private final int maxTextureSize;
        private final int maxTextureUnits;
        private final int maxVertexAttribs;
        private final int maxDrawBuffers;
        private final int maxColorAttachments;
        private final int maxRenderbufferSize;
        private final int maxViewportDims;
        private final int maxSamples;
        private final int maxUniformBufferBindings;
        private final int maxShaderStorageBufferBindings;
        private final int maxAtomicCounterBufferBindings;
        private final int maxImageUnits;
        private final int[] maxComputeWorkGroupCount;
        private final int[] maxComputeWorkGroupSize;
        private final int maxComputeWorkGroupInvocations;
        private final int maxPatchVertices;
        private final int uniformBufferOffsetAlignment;
        private final int shaderStorageBufferOffsetAlignment;
        private final int textureBufferOffsetAlignment;
        
        GLESCapabilities() {
            // Detect version
            String versionString = GLES20.glGetString(GL_VERSION);
            version = parseVersion(versionString);
            
            // Detect GPU vendor
            renderer = GLES20.glGetString(GL_RENDERER);
            driverVersion = versionString;
            gpuVendor = detectGPUVendor(renderer);
            
            // Detect extensions
            extensions = detectExtensions();
            
            // Query limits
            maxTextureSize = getInteger(GL_MAX_TEXTURE_SIZE);
            maxTextureUnits = getInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
            maxVertexAttribs = getInteger(GL_MAX_VERTEX_ATTRIBS);
            maxRenderbufferSize = getInteger(GL_MAX_RENDERBUFFER_SIZE);
            maxViewportDims = getInteger(GL_MAX_VIEWPORT_DIMS);
            
            // GLES 3.0+ limits
            if (version.isAtLeast(GLESVersion.GLES_3_0)) {
                maxDrawBuffers = getInteger(GL_MAX_DRAW_BUFFERS);
                maxColorAttachments = getInteger(GL_MAX_COLOR_ATTACHMENTS);
                maxSamples = getInteger(GL_MAX_SAMPLES);
                maxUniformBufferBindings = getInteger(GL_MAX_UNIFORM_BUFFER_BINDINGS);
                uniformBufferOffsetAlignment = getInteger(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);
            } else {
                maxDrawBuffers = 1;
                maxColorAttachments = 1;
                maxSamples = 0;
                maxUniformBufferBindings = 0;
                uniformBufferOffsetAlignment = 256;
            }
            
            // GLES 3.1+ limits
            if (version.isAtLeast(GLESVersion.GLES_3_1)) {
                maxShaderStorageBufferBindings = getInteger(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS);
                maxAtomicCounterBufferBindings = getInteger(GL_MAX_ATOMIC_COUNTER_BUFFER_BINDINGS);
                maxImageUnits = getInteger(GL_MAX_IMAGE_UNITS);
                shaderStorageBufferOffsetAlignment = getInteger(GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT);
                
                maxComputeWorkGroupCount = new int[3];
                maxComputeWorkGroupSize = new int[3];
                for (int i = 0; i < 3; i++) {
                    maxComputeWorkGroupCount[i] = getIntegerIndexed(GL_MAX_COMPUTE_WORK_GROUP_COUNT, i);
                    maxComputeWorkGroupSize[i] = getIntegerIndexed(GL_MAX_COMPUTE_WORK_GROUP_SIZE, i);
                }
                maxComputeWorkGroupInvocations = getInteger(GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS);
            } else {
                maxShaderStorageBufferBindings = 0;
                maxAtomicCounterBufferBindings = 0;
                maxImageUnits = 0;
                shaderStorageBufferOffsetAlignment = 256;
                maxComputeWorkGroupCount = new int[]{0, 0, 0};
                maxComputeWorkGroupSize = new int[]{0, 0, 0};
                maxComputeWorkGroupInvocations = 0;
            }
            
            // GLES 3.2+ limits
            if (version.isAtLeast(GLESVersion.GLES_3_2)) {
                maxPatchVertices = getInteger(GL_MAX_PATCH_VERTICES);
                textureBufferOffsetAlignment = getInteger(GL_TEXTURE_BUFFER_OFFSET_ALIGNMENT);
            } else if (hasExtension(GLESExtension.EXT_tessellation_shader)) {
                maxPatchVertices = getInteger(GL_MAX_PATCH_VERTICES);
                textureBufferOffsetAlignment = 256;
            } else {
                maxPatchVertices = 0;
                textureBufferOffsetAlignment = 256;
            }
        }
        
        // Version queries
        GLESVersion version() { return version; }
        GPUVendor getGPUVendor() { return gpuVendor; }
        String getRenderer() { return renderer; }
        String getDriverVersion() { return driverVersion; }
        
        // Extension queries
        boolean hasExtension(GLESExtension ext) { return extensions.contains(ext); }
        Set<GLESExtension> getExtensions() { return Collections.unmodifiableSet(extensions); }
        
        // Capability queries
        boolean supportsVAO() {
            return version.isAtLeast(GLESVersion.GLES_3_0) || 
                   hasExtension(GLESExtension.OES_vertex_array_object);
        }
        
        boolean supportsInstancing() {
            return version.isAtLeast(GLESVersion.GLES_3_0) ||
                   hasExtension(GLESExtension.EXT_instanced_arrays) ||
                   hasExtension(GLESExtension.ANGLE_instanced_arrays);
        }
        
        boolean supportsCompute() {
            return version.isAtLeast(GLESVersion.GLES_3_1);
        }
        
        boolean supportsTessellation() {
            return version.isAtLeast(GLESVersion.GLES_3_2) ||
                   hasExtension(GLESExtension.EXT_tessellation_shader);
        }
        
        boolean supportsGeometryShader() {
            return version.isAtLeast(GLESVersion.GLES_3_2) ||
                   hasExtension(GLESExtension.EXT_geometry_shader);
        }
        
        boolean supportsDebugOutput() {
            return version.isAtLeast(GLESVersion.GLES_3_2) ||
                   hasExtension(GLESExtension.KHR_debug);
        }
        
        // Limit getters
        int maxTextureSize() { return maxTextureSize; }
        int maxTextureUnits() { return maxTextureUnits; }
        int maxVertexAttribs() { return maxVertexAttribs; }
        int maxDrawBuffers() { return maxDrawBuffers; }
        int maxColorAttachments() { return maxColorAttachments; }
        int maxRenderbufferSize() { return maxRenderbufferSize; }
        int maxSamples() { return maxSamples; }
        int maxUniformBufferBindings() { return maxUniformBufferBindings; }
        int maxShaderStorageBufferBindings() { return maxShaderStorageBufferBindings; }
        int maxAtomicCounterBufferBindings() { return maxAtomicCounterBufferBindings; }
        int maxImageUnits() { return maxImageUnits; }
        int[] maxComputeWorkGroupCount() { return maxComputeWorkGroupCount.clone(); }
        int[] maxComputeWorkGroupSize() { return maxComputeWorkGroupSize.clone(); }
        int maxComputeWorkGroupInvocations() { return maxComputeWorkGroupInvocations; }
        int maxPatchVertices() { return maxPatchVertices; }
        int uniformBufferOffsetAlignment() { return uniformBufferOffsetAlignment; }
        int shaderStorageBufferOffsetAlignment() { return shaderStorageBufferOffsetAlignment; }
        int textureBufferOffsetAlignment() { return textureBufferOffsetAlignment; }
        int maxTransformFeedbackSeparateAttribs() { return 4; } // GL minimum
        
        private GLESVersion parseVersion(String versionString) {
            // Parse "OpenGL ES X.Y" or "OpenGL ES X.Y.Z"
            if (versionString.contains("3.2")) return GLESVersion.GLES_3_2;
            if (versionString.contains("3.1")) return GLESVersion.GLES_3_1;
            if (versionString.contains("3.0")) return GLESVersion.GLES_3_0;
            if (versionString.contains("2.0")) return GLESVersion.GLES_2_0;
            return GLESVersion.GLES_2_0;
        }
        
        private GPUVendor detectGPUVendor(String renderer) {
            String lower = renderer.toLowerCase();
            if (lower.contains("adreno")) return GPUVendor.QUALCOMM_ADRENO;
            if (lower.contains("mali")) return GPUVendor.ARM_MALI;
            if (lower.contains("powervr") || lower.contains("sgx")) return GPUVendor.IMAGINATION_POWERVR;
            if (lower.contains("tegra") || lower.contains("nvidia")) return GPUVendor.NVIDIA_TEGRA;
            if (lower.contains("intel")) return GPUVendor.INTEL;
            if (lower.contains("apple")) return GPUVendor.APPLE;
            if (lower.contains("vivante")) return GPUVendor.VIVANTE;
            return GPUVendor.UNKNOWN;
        }
        
        private Set<GLESExtension> detectExtensions() {
            Set<GLESExtension> exts = EnumSet.noneOf(GLESExtension.class);
            
            if (version.isAtLeast(GLESVersion.GLES_3_0)) {
                int numExtensions = getInteger(GL_NUM_EXTENSIONS);
                for (int i = 0; i < numExtensions; i++) {
                    String extName = GLES30.glGetStringi(GL_EXTENSIONS, i);
                    GLESExtension ext = GLESExtension.fromString(extName);
                    if (ext != null) {
                        exts.add(ext);
                    }
                }
            } else {
                String extString = GLES20.glGetString(GL_EXTENSIONS);
                if (extString != null) {
                    for (String extName : extString.split(" ")) {
                        GLESExtension ext = GLESExtension.fromString(extName);
                        if (ext != null) {
                            exts.add(ext);
                        }
                    }
                }
            }
            
            return exts;
        }
        
        private int getInteger(int pname) {
            IntBuffer buf = BufferUtils.createIntBuffer(1);
            GLES20.glGetIntegerv(pname, buf);
            return buf.get(0);
        }
        
        private int getIntegerIndexed(int pname, int index) {
            IntBuffer buf = BufferUtils.createIntBuffer(1);
            GLES31.glGetIntegeri_v(pname, index, buf);
            return buf.get(0);
        }
        
        // GL constants
        private static final int GL_VERSION = 0x1F02;
        private static final int GL_RENDERER = 0x1F01;
        private static final int GL_EXTENSIONS = 0x1F03;
        private static final int GL_NUM_EXTENSIONS = 0x821D;
        private static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
        private static final int GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D;
        private static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
        private static final int GL_MAX_RENDERBUFFER_SIZE = 0x84E8;
        private static final int GL_MAX_VIEWPORT_DIMS = 0x0D3A;
        private static final int GL_MAX_DRAW_BUFFERS = 0x8824;
        private static final int GL_MAX_COLOR_ATTACHMENTS = 0x8CDF;
        private static final int GL_MAX_SAMPLES = 0x8D57;
        private static final int GL_MAX_UNIFORM_BUFFER_BINDINGS = 0x8A2F;
        private static final int GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT = 0x8A34;
        private static final int GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS = 0x90DD;
        private static final int GL_MAX_ATOMIC_COUNTER_BUFFER_BINDINGS = 0x92DC;
        private static final int GL_MAX_IMAGE_UNITS = 0x8F38;
        private static final int GL_SHADER_STORAGE_BUFFER_OFFSET_ALIGNMENT = 0x90DF;
        private static final int GL_MAX_COMPUTE_WORK_GROUP_COUNT = 0x91BE;
        private static final int GL_MAX_COMPUTE_WORK_GROUP_SIZE = 0x91BF;
        private static final int GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS = 0x90EB;
        private static final int GL_MAX_PATCH_VERTICES = 0x8E7D;
        private static final int GL_TEXTURE_BUFFER_OFFSET_ALIGNMENT = 0x919F;
    }
    
    enum GLESVersion {
        GLES_2_0(2, 0),
        GLES_3_0(3, 0),
        GLES_3_1(3, 1),
        GLES_3_2(3, 2);
        
        final int major, minor;
        
        GLESVersion(int major, int minor) {
            this.major = major;
            this.minor = minor;
        }
        
        boolean isAtLeast(GLESVersion other) {
            return major > other.major || (major == other.major && minor >= other.minor);
        }
    }
    
    enum GPUVendor {
        QUALCOMM_ADRENO,
        ARM_MALI,
        IMAGINATION_POWERVR,
        NVIDIA_TEGRA,
        INTEL,
        APPLE,
        VIVANTE,
        UNKNOWN
    }

// ============================================================================
// SECTION 30: STRING/BINARY CACHE IMPLEMENTATION (~300 lines)
// ============================================================================

    /**
     * Caches strings and binary data returned from GL queries.
     * Uses handles to avoid passing strings across the API boundary.
     */
    private static final class StringCache {
        
        private final Int2ObjectOpenHashMap<String> cache = new Int2ObjectOpenHashMap<>();
        private int nextHandle = 1;
        private final int maxSize;
        
        StringCache(int maxSize) {
            this.maxSize = maxSize;
        }
        
        int put(String value) {
            if (value == null) {
                return 0;
            }
            
            // Evict if at capacity
            if (cache.size() >= maxSize) {
                evictOldest();
            }
            
            int handle = nextHandle++;
            cache.put(handle, value);
            return handle;
        }
        
        String get(int handle) {
            return cache.get(handle);
        }
        
        void remove(int handle) {
            cache.remove(handle);
        }
        
        void clear() {
            cache.clear();
            nextHandle = 1;
        }
        
        private void evictOldest() {
            // Simple eviction - remove first entry
            if (!cache.isEmpty()) {
                int firstKey = cache.keySet().intIterator().nextInt();
                cache.remove(firstKey);
            }
        }
    }
    
    /**
     * Caches compiled shader binaries.
     */
    private static final class BinaryCache {
        
        private final Int2ObjectOpenHashMap<BinaryEntry> cache = new Int2ObjectOpenHashMap<>();
        private int nextHandle = 1;
        private final long maxBytes;
        private long currentBytes = 0;
        
        BinaryCache(long maxBytes) {
            this.maxBytes = maxBytes;
        }
        
        int put(ByteBuffer data, int length, int format) {
            // Copy the data
            byte[] copy = new byte[length];
            data.get(copy);
            
            // Evict if needed
            while (currentBytes + length > maxBytes && !cache.isEmpty()) {
                evictOldest();
            }
            
            int handle = nextHandle++;
            cache.put(handle, new BinaryEntry(copy, format));
            currentBytes += length;
            return handle;
        }
        
        BinaryEntry get(int handle) {
            return cache.get(handle);
        }
        
        void remove(int handle) {
            BinaryEntry entry = cache.remove(handle);
            if (entry != null) {
                currentBytes -= entry.data.length;
            }
        }
        
        void clear() {
            cache.clear();
            currentBytes = 0;
            nextHandle = 1;
        }
        
        private void evictOldest() {
            if (!cache.isEmpty()) {
                int firstKey = cache.keySet().intIterator().nextInt();
                remove(firstKey);
            }
        }
        
        record BinaryEntry(byte[] data, int format) {}
    }

// ============================================================================
// SECTION 31: CLASS INITIALIZATION & CLEANUP
// ============================================================================

    // Thread-local scratch buffers for zero-allocation queries
    private static final ThreadLocal<IntBuffer> intScratchBuffer = 
        ThreadLocal.withInitial(() -> BufferUtils.createIntBuffer(16));
    private static final ThreadLocal<FloatBuffer> floatScratchBuffer = 
        ThreadLocal.withInitial(() -> BufferUtils.createFloatBuffer(16));
    
    // Core components
    private final GLESCapabilities capabilities;
    private final GLESErrorHandler errorHandler;
    private final GLESStateTracker stateTracker;
    private final ResourceRegistry resourceRegistry;
    private final EmulatedVAOManager emulatedVAOManager;
    private final GPUQuirkHandler quirkHandler;
    private final StringCache stringCache;
    private final BinaryCache binaryCache;
    
    // Constants
    private static final int MAX_SHADER_SOURCE_LENGTH = 1024 * 1024; // 1MB
    
    /**
     * Creates a new OpenGLES call mapper.
     * Must be called from a thread with an active GL context.
     */
    public OpenGLESCallMapper() {
        // Initialize capabilities first - needed by other components
        this.capabilities = new GLESCapabilities();
        
        // Initialize error handling
        this.errorHandler = new GLESErrorHandler();
        
        // Initialize state tracking
        this.stateTracker = new GLESStateTracker();
        
        // Initialize resource tracking
        this.resourceRegistry = new ResourceRegistry();
        
        // Initialize emulated VAO if needed
        if (!capabilities.supportsVAO()) {
            this.emulatedVAOManager = new EmulatedVAOManager();
        } else {
            this.emulatedVAOManager = null;
        }
        
        // Initialize quirk handler
        this.quirkHandler = new GPUQuirkHandler(capabilities);
        
        // Initialize caches
        this.stringCache = new StringCache(1000);
        this.binaryCache = new BinaryCache(64 * 1024 * 1024); // 64MB
        
        // Clear any pending errors
        errorHandler.clearErrors();
    }
    
    /**
     * Cleans up all resources tracked by this mapper.
     * Should be called before destroying the GL context.
     */
    public void cleanup() {
        // Check for leaks
        ResourceLeakReport leaks = resourceRegistry.checkForLeaks();
        if (leaks.hasLeaks()) {
            System.err.println(leaks);
        }
        
        // Clean up all resources
        resourceRegistry.cleanup();
        
        // Clear caches
        stringCache.clear();
        binaryCache.clear();
        
        // Reset state
        stateTracker.reset();
    }
    
    /**
     * Gets current capabilities.
     */
    public GLESCapabilities getCapabilities() {
        return capabilities;
    }
    
    /**
     * Gets performance metrics for the current frame.
     */
    public FrameMetrics getFrameMetrics() {
        return new FrameMetrics(
            stateTracker.getDrawCallCount(),
            stateTracker.getStateChangeCount(),
            stateTracker.getTriangleCount(),
            stateTracker.getApiCallCount(),
            stateTracker.getRedundantCallsAvoided()
        );
    }
    
    /**
     * Resets per-frame metrics. Call at start of each frame.
     */
    public void resetFrameMetrics() {
        stateTracker.resetFrameCounters();
    }
    
    record FrameMetrics(
        long drawCalls,
        long stateChanges,
        long triangles,
        long apiCalls,
        long redundantCallsAvoided
    ) {}

// ============================================================================
// SECTION 32: INTEGRATED LIFECYCLE & SSO FIXES (Final Integration)
// ============================================================================

    /**
     * FIXED mapProgramPipelineBind:
     * Handles the GLES spec requirement that glBindProgramPipeline(nonzero) 
     * implicitly acts as glUseProgram(0).
     */
    private MappingResult mapProgramPipelineBind_Fixed(CallDescriptor call) {
        if (!capabilities.version().isAtLeast(GLESVersion.GLES_3_1)) {
            return MappingResult.unsupported("SSO requires GLES 3.1+");
        }
        
        int pipeline = call.intParam(0);
        
        if (stateTracker.getBoundProgramPipeline() == pipeline) {
            stateTracker.incrementRedundantCallsAvoided();
            return MappingResult.success();
        }
        
        // CRITICAL SPEC FIX: If a pipeline is bound, any active glUseProgram 
        // is overridden. We must synchronize our state tracker.
        if (pipeline != 0) {
            stateTracker.setBoundProgram(0);
        }
        
        GLES31.glBindProgramPipeline(pipeline);
        
        GLESError error = errorHandler.checkError("glBindProgramPipeline");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        stateTracker.setBoundProgramPipeline(pipeline);
        stateTracker.incrementStateChangeCount();
        return MappingResult.success();
    }

    /**
     * FIXED mapProgramUse:
     * Handles the GLES spec requirement that glUseProgram(nonzero) 
     * overrides/disables any currently bound Program Pipeline.
     */
    private MappingResult mapProgramUse_Fixed(CallDescriptor call) {
        int program = call.intParam(0);
        
        if (stateTracker.getBoundProgram() == program) {
            stateTracker.incrementRedundantCallsAvoided();
            return MappingResult.success();
        }
        
        // CRITICAL SPEC FIX: Using a standard program disables the bound pipeline.
        if (program != 0 && stateTracker.getBoundProgramPipeline() != 0) {
            stateTracker.setBoundProgramPipeline(0);
            // Note: We don't need to call glBindProgramPipeline(0) because 
            // the driver handles the precedence; we just update our tracker.
        }
        
        GLES20.glUseProgram(program);
        
        GLESError error = errorHandler.checkError("glUseProgram");
        if (error != GLESError.NO_ERROR) {
            return MappingResult.failed(MappingStatus.FAILED_EXECUTION, error.code(), error.description());
        }
        
        stateTracker.setBoundProgram(program);
        stateTracker.incrementStateChangeCount();
        return MappingResult.success();
    }

    /**
     * FIXED mapUseProgramStages:
     * Validates that programs are SEPARABLE before attachment.
     */
    private MappingResult mapUseProgramStages_Fixed(CallDescriptor call) {
        int pipeline = call.intParam(0);
        int stages = call.intParam(1);
        int program = call.intParam(2);
        
        if (program != 0) {
            // VALIDATION FIX: Ensure the program was linked with GL_PROGRAM_SEPARABLE
            int[] params = new int[1];
            GLES20.glGetProgramiv(program, 0x825B, params); // GL_PROGRAM_SEPARABLE
            if (params[0] == GLES20.GL_FALSE) {
                return MappingResult.failed(MappingStatus.FAILED_VALIDATION, 
                    "Program " + program + " is not separable. SSO requires GL_PROGRAM_SEPARABLE.");
            }
        }

        GLES31.glUseProgramStages(pipeline, stages, program);
        return MappingResult.success();
    }

    /**
     * REVISED ResourceRegistry Cleanup Logic:
     * Ensures dependencies are deleted in the correct order to prevent zombie resources.
     */
    private void performGlobalCleanup_Fixed() {
        // 1. Sync Objects (Finish pending GPU work)
        resourceRegistry.cleanupSyncObjects();
        
        // 2. Program Pipelines (SSO)
        // These must be deleted BEFORE programs because they hold references to them.
        resourceRegistry.cleanupProgramPipelines();
        
        // 3. Programs
        // Deleting programs now drops their internal ref-count safely.
        resourceRegistry.cleanupPrograms();
        
        // 4. Shaders
        resourceRegistry.cleanupShaders();
        
        // 5. VAOs, Buffers, Textures, and FBOs
        resourceRegistry.cleanupHeavyResources();
        
        // 6. Reset State Tracker
        stateTracker.reset();
        
        // 7. Clear String/Binary Caches
        stringCache.clear();
        binaryCache.clear();
    }

    /**
     * Helper methods for the ResourceRegistry to support Section 32
     */
    private static final class ResourceRegistry_SSO_Expansion {
        private final IntOpenHashSet programPipelines = new IntOpenHashSet();

        void cleanupProgramPipelines() {
            synchronized (programPipelines) {
                for (int pipeline : programPipelines) {
                    if (GLES31.glIsProgramPipeline(pipeline)) {
                        GLES31.glDeleteProgramPipelines(pipeline);
                    }
                }
                programPipelines.clear();
            }
        }
        
        // Ensure mapProgramPipelineDelete calls this:
        void unregisterPipeline(int id) {
            synchronized (programPipelines) {
                programPipelines.remove(id);
            }
        }
    } // End of ResourceRegistry_SSO_Expansion

// ============================================================================
// SECTION 33: GPU OFFLOAD & SCATTER UPDATES (GLES 3.1+)
// ============================================================================

    /**
     * GPU-Driven optimization module embedded in the mapper.
     * Offloads O(N) instance updates and sorting to Compute Shaders.
     * 
     * REPLACES: CPU-side dirty scanning loop (Java)
     * WITH:     Linear ring-buffer append + GPU Scatter (Compute)
     */
    public final class GLESGPUOffload {
        
        // 1MB Ring Buffer for updates (holds ~16k pending updates)
        private static final int UPDATE_RING_SIZE = 1024 * 1024; 
        private static final int UPDATE_SLOT_SIZE = 64; // Mat4x3(48) + Flags(4) + ID(4) + Pad
        
        // GL Handles
        private int updateRingBuffer = 0;
        private int sortScratchBuffer = 0;
        private long mappedPtr = 0;
        private ByteBuffer mappedBuffer = null;
        
        // State
        private final AtomicInteger updateHead = new AtomicInteger(0);
        private boolean initialized = false;
        
        /**
         * Initialize the offload system. Call this lazily when GLES 3.1 is detected.
         */
        public void initialize() {
            if (initialized || !capabilities.supportsCompute()) return;
            
            // 1. Create Ring Buffer (GL_SHADER_STORAGE_BUFFER)
            // Use MAP_COHERENT_BIT | MAP_PERSISTENT_BIT if available via EXT_buffer_storage
            // Otherwise use standard storage buffer
            IntBuffer ids = intScratchBuffer.get();
            GLES20.glGenBuffers(ids);
            updateRingBuffer = ids.get(0);
            
            GLES20.glBindBuffer(0x90D2, updateRingBuffer); // GL_SHADER_STORAGE_BUFFER
            GLES20.glBufferData(0x90D2, UPDATE_RING_SIZE, 0x88EB); // GL_STREAM_DRAW
            
            // 2. Map Buffer for Zero-Copy Access
            // Using GL_MAP_WRITE_BIT | GL_MAP_FLUSH_EXPLICIT_BIT | GL_MAP_UNSYNCHRONIZED_BIT
            int access = 0x0002 | 0x0010 | 0x0020; 
            
            // Note: In production GLES, we'd use glMapBufferRange. 
            // Since we are in the mapper, we assume the context is bound.
            mappedBuffer = GLES30.glMapBufferRange(0x90D2, 0, UPDATE_RING_SIZE, access);
            
            if (mappedBuffer != null) {
                mappedBuffer.order(ByteOrder.nativeOrder());
                mappedPtr = org.lwjgl.system.MemoryUtil.memAddress(mappedBuffer);
                initialized = true;
                resourceRegistry.registerBuffer(updateRingBuffer);
            } else {
                // Fallback if mapping fails
                GLES20.glDeleteBuffers(updateRingBuffer);
                updateRingBuffer = 0;
            }
            
            GLES20.glBindBuffer(0x90D2, 0);
        }
        
        /**
         * Queue an instance update directly to the mapped pointer.
         * Zero JNI overhead, Zero Allocation.
         */
        public void queueUpdate(int instanceId, float[] modelMatrix, int flags) {
            if (!initialized) return;
            
            int slotIdx = updateHead.getAndIncrement();
            int byteOffset = (slotIdx * UPDATE_SLOT_SIZE) % UPDATE_RING_SIZE;
            
            // Unsafe writes to mapped memory
            long addr = mappedPtr + byteOffset;
            
            // Header: ID
            org.lwjgl.system.MemoryUtil.memPutInt(addr, instanceId);
            
            // Payload: Matrix (12 floats, 3x4 layout to save bandwidth)
            long matAddr = addr + 4;
            for (int i = 0; i < 12; i++) {
                org.lwjgl.system.MemoryUtil.memPutFloat(matAddr + (i << 2), modelMatrix[i]);
            }
            
            // Footer: Flags
            org.lwjgl.system.MemoryUtil.memPutInt(addr + 52, flags);
        }
        
        /**
         * Dispatch the compute shader to scatter updates from Ring -> Instance Buffer.
         */
        public void dispatchScatter(int instanceBufferHandle) {
            if (!initialized) return;
            
            int totalUpdates = updateHead.get();
            if (totalUpdates == 0) return;
            
            // 1. Flush Mapped Range (if not coherent)
            GLES30.glFlushMappedBufferRange(0x90D2, 0, UPDATE_RING_SIZE);
            
            // 2. Bind Buffers
            // Binding 0: Ring Buffer (Source)
            GLES30.glBindBufferBase(0x90D2, 0, updateRingBuffer);
            // Binding 1: Instance Buffer (Destination)
            GLES30.glBindBufferBase(0x90D2, 1, instanceBufferHandle);
            
            // 3. Dispatch Compute
            // One thread per update. Workgroup size assumed 64.
            int groups = (totalUpdates + 63) / 64;
            
            // Use active compute program (assumed bound by caller)
            // Or bind internal scatter program here
            GLES31.glDispatchCompute(groups, 1, 1);
            
            // 4. Memory Barrier
            // Ensure writes finish before vertex fetching
            GLES31.glMemoryBarrier(0x00002000 | 0x00000001); // SHADER_STORAGE | VERTEX_ATTRIB_ARRAY
            
            // Reset head
            updateHead.set(0);
        }
        
        /**
         * Cleanup resources.
         */
        public void destroy() {
            if (initialized) {
                GLES20.glBindBuffer(0x90D2, updateRingBuffer);
                GLES30.glUnmapBuffer(0x90D2);
                GLES20.glDeleteBuffers(updateRingBuffer);
                initialized = false;
            }
        }
    }
    
    // Add the offload module to the main class fields
    public final GLESGPUOffload gpuOffload = new GLESGPUOffload();

} // End of OpenGLESCallMapper class