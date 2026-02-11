package stellar.snow.astralis.engine.render.pipeline;

// ============================================================================
// COMPLETE OPENGL IMPORTS (1.1 - 4.6)
// ============================================================================
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

// ARB Extensions
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.ARBBindlessTexture;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBComputeShader;
import org.lwjgl.opengl.ARBShaderStorageBufferObject;
import org.lwjgl.opengl.ARBSparseTexture;
import org.lwjgl.opengl.ARBTextureFilterAnisotropic;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.ARBShaderImageLoadStore;
import org.lwjgl.opengl.ARBGPUShaderInt64;
import org.lwjgl.opengl.ARBShadingLanguageInclude;
import org.lwjgl.opengl.ARBSPIRVExtensions;
import org.lwjgl.opengl.ARBGLSpirv;

// EXT Extensions
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;

// NV Extensions
import org.lwjgl.opengl.NVMeshShader;
import org.lwjgl.opengl.NVRayTracing;
import org.lwjgl.opengl.NVCommandList;
import org.lwjgl.opengl.NVBindlessTexture;
import org.lwjgl.opengl.NVShaderBufferLoad;

// ============================================================================
// COMPLETE VULKAN IMPORTS (1.0 - 1.3+)
// ============================================================================
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VK13;

// Vulkan Extensions
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRDynamicRendering;
import org.lwjgl.vulkan.KHRSynchronization2;
import org.lwjgl.vulkan.KHRBufferDeviceAddress;
import org.lwjgl.vulkan.KHRRayTracingPipeline;
import org.lwjgl.vulkan.KHRAccelerationStructure;
import org.lwjgl.vulkan.KHRDeferredHostOperations;
import org.lwjgl.vulkan.KHRSpirv14;
import org.lwjgl.vulkan.KHRShaderFloatControls;
import org.lwjgl.vulkan.KHRMaintenance4;
import org.lwjgl.vulkan.KHRMaintenance5;
import org.lwjgl.vulkan.KHRMaintenance6;
import org.lwjgl.vulkan.EXTDescriptorIndexing;
import org.lwjgl.vulkan.EXTMeshShader;
import org.lwjgl.vulkan.EXTShaderAtomicFloat;
import org.lwjgl.vulkan.EXTMemoryBudget;
import org.lwjgl.vulkan.EXTConditionalRendering;
import org.lwjgl.vulkan.EXTExtendedDynamicState;
import org.lwjgl.vulkan.EXTExtendedDynamicState2;
import org.lwjgl.vulkan.EXTExtendedDynamicState3;
import org.lwjgl.vulkan.NVRayTracing;
import org.lwjgl.vulkan.NVMeshShader;

import java.lang.foreign.MemorySegment;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RenderConstants - Comprehensive bidirectional translation layer between
 * OpenGL (1.0-4.6), Vulkan (1.0-1.4), SPIR-V (1.0-1.6), and GLSL (110-460).
 *
 * <h2>Supported API Versions:</h2>
 * <ul>
 *   <li><b>OpenGL:</b> 1.0, 1.1, 1.2, 1.2.1, 1.5, 2.0, 2.1, 3.0, 3.1, 3.2, 3.3, 4.0-4.6</li>
 *   <li><b>Vulkan:</b> 1.0, 1.1, 1.2, 1.3, 1.4 (via extensions)</li>
 *   <li><b>SPIR-V:</b> 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6</li>
 *   <li><b>GLSL:</b> 110, 120, 130, 140, 150, 330, 400, 410, 420, 430, 440, 450, 460</li>
 * </ul>
 *
 * <h2>Design Principles:</h2>
 * <ul>
 *   <li>Zero-allocation lookups via precomputed arrays</li>
 *   <li>Complete feature parity tracking between APIs</li>
 *   <li>Version-aware translation with fallbacks</li>
 *   <li>Extension detection and capability querying</li>
 * </ul>
 */
public final class RenderConstants {

    private RenderConstants() {}

    // ========================================================================
    // VERSION ENUMERATIONS
    // ========================================================================

    /**
     * OpenGL version enumeration with feature level tracking.
     */
    public enum GLVersion {
        GL_1_0(1, 0, 100),
        GL_1_1(1, 1, 110),
        GL_1_2(1, 2, 120),
        GL_1_2_1(1, 2, 121),
        GL_1_3(1, 3, 130),
        GL_1_4(1, 4, 140),
        GL_1_5(1, 5, 150),
        GL_2_0(2, 0, 200),
        GL_2_1(2, 1, 210),
        GL_3_0(3, 0, 300),
        GL_3_1(3, 1, 310),
        GL_3_2(3, 2, 320),
        GL_3_3(3, 3, 330),
        GL_4_0(4, 0, 400),
        GL_4_1(4, 1, 410),
        GL_4_2(4, 2, 420),
        GL_4_3(4, 3, 430),
        GL_4_4(4, 4, 440),
        GL_4_5(4, 5, 450),
        GL_4_6(4, 6, 460);

        public final int major;
        public final int minor;
        public final int glslVersion;
        public final int numericValue;

        GLVersion(int major, int minor, int glslVersion) {
            this.major = major;
            this.minor = minor;
            this.glslVersion = glslVersion;
            this.numericValue = major * 100 + minor * 10;
        }

        public boolean isAtLeast(GLVersion other) {
            return this.numericValue >= other.numericValue;
        }

        public static GLVersion fromNumbers(int major, int minor) {
            for (GLVersion v : values()) {
                if (v.major == major && v.minor == minor) return v;
            }
            return GL_1_0;
        }
    }

    /**
     * Vulkan version enumeration.
     */
    public enum VKVersion {
        VK_1_0(1, 0, VK10.VK_API_VERSION_1_0),
        VK_1_1(1, 1, VK11.VK_API_VERSION_1_1),
        VK_1_2(1, 2, VK12.VK_API_VERSION_1_2),
        VK_1_3(1, 3, VK13.VK_API_VERSION_1_3),
        VK_1_4(1, 4, makeVersion(1, 4, 0)); // Manual for 1.4

        public final int major;
        public final int minor;
        public final int apiVersion;

        VKVersion(int major, int minor, int apiVersion) {
            this.major = major;
            this.minor = minor;
            this.apiVersion = apiVersion;
        }

        public boolean isAtLeast(VKVersion other) {
            return this.ordinal() >= other.ordinal();
        }

        private static int makeVersion(int major, int minor, int patch) {
            return (major << 22) | (minor << 12) | patch;
        }
    }

    /**
     * SPIR-V version enumeration.
     */
    public enum SPIRVVersion {
        SPIRV_1_0(0x00010000, VKVersion.VK_1_0),
        SPIRV_1_1(0x00010100, VKVersion.VK_1_0),
        SPIRV_1_2(0x00010200, VKVersion.VK_1_0),
        SPIRV_1_3(0x00010300, VKVersion.VK_1_1),
        SPIRV_1_4(0x00010400, VKVersion.VK_1_2),
        SPIRV_1_5(0x00010500, VKVersion.VK_1_2),
        SPIRV_1_6(0x00010600, VKVersion.VK_1_3);

        public final int version;
        public final VKVersion minVulkan;

        SPIRVVersion(int version, VKVersion minVulkan) {
            this.version = version;
            this.minVulkan = minVulkan;
        }

        public int getMajor() {
            return (version >> 16) & 0xFF;
        }

        public int getMinor() {
            return (version >> 8) & 0xFF;
        }

        public boolean isAtLeast(SPIRVVersion other) {
            return this.ordinal() >= other.ordinal();
        }

        public static SPIRVVersion fromVulkan(VKVersion vkVersion) {
            return switch (vkVersion) {
                case VK_1_0 -> SPIRV_1_0;
                case VK_1_1 -> SPIRV_1_3;
                case VK_1_2 -> SPIRV_1_5;
                case VK_1_3, VK_1_4 -> SPIRV_1_6;
            };
        }
    }

    /**
     * GLSL version enumeration with feature tracking.
     */
    public enum GLSLVersion {
        GLSL_110(110, GLVersion.GL_2_0, false, false),
        GLSL_120(120, GLVersion.GL_2_1, false, false),
        GLSL_130(130, GLVersion.GL_3_0, true, false),
        GLSL_140(140, GLVersion.GL_3_1, true, false),
        GLSL_150(150, GLVersion.GL_3_2, true, true),
        GLSL_330(330, GLVersion.GL_3_3, true, true),
        GLSL_400(400, GLVersion.GL_4_0, true, true),
        GLSL_410(410, GLVersion.GL_4_1, true, true),
        GLSL_420(420, GLVersion.GL_4_2, true, true),
        GLSL_430(430, GLVersion.GL_4_3, true, true),
        GLSL_440(440, GLVersion.GL_4_4, true, true),
        GLSL_450(450, GLVersion.GL_4_5, true, true),
        GLSL_460(460, GLVersion.GL_4_6, true, true);

        public final int version;
        public final GLVersion minGL;
        public final boolean hasInOut;           // in/out keywords vs attribute/varying
        public final boolean hasExplicitLayouts; // layout(location = N)

        GLSLVersion(int version, GLVersion minGL, boolean hasInOut, boolean hasExplicitLayouts) {
            this.version = version;
            this.minGL = minGL;
            this.hasInOut = hasInOut;
            this.hasExplicitLayouts = hasExplicitLayouts;
        }

        public boolean isAtLeast(GLSLVersion other) {
            return this.version >= other.version;
        }

        public static GLSLVersion fromGL(GLVersion glVersion) {
            for (int i = values().length - 1; i >= 0; i--) {
                if (values()[i].minGL.numericValue <= glVersion.numericValue) {
                    return values()[i];
                }
            }
            return GLSL_110;
        }

        public String getVersionDirective() {
            return "#version " + version + (version >= 150 ? " core" : "");
        }

        public String getVersionDirectiveVulkan() {
            return "#version " + version + " core\n#extension GL_GOOGLE_include_directive : require";
        }
    }

    // ========================================================================
    // CAPABILITY FLAGS (Unified across all APIs)
    // ========================================================================

    /**
     * GPU capabilities that can be queried across API versions.
     */
    public enum Capability {
        // Core Features
        COMPUTE_SHADERS,
        GEOMETRY_SHADERS,
        TESSELLATION_SHADERS,
        MESH_SHADERS,
        TASK_SHADERS,
        RAY_TRACING,
        RAY_QUERY,
        
        // Memory Features
        BUFFER_DEVICE_ADDRESS,
        BINDLESS_TEXTURES,
        SPARSE_TEXTURES,
        SPARSE_BUFFERS,
        MEMORY_BUDGET,
        
        // Rendering Features
        MULTI_DRAW_INDIRECT,
        INDIRECT_COUNT,
        DYNAMIC_RENDERING,
        CONDITIONAL_RENDERING,
        TRANSFORM_FEEDBACK,
        
        // Shader Features
        SHADER_INT64,
        SHADER_FLOAT64,
        SHADER_INT16,
        SHADER_FLOAT16,
        SHADER_ATOMIC_FLOAT,
        SHADER_SUBGROUP_OPERATIONS,
        
        // Synchronization
        TIMELINE_SEMAPHORES,
        SYNCHRONIZATION2,
        
        // Texture Features
        ANISOTROPIC_FILTERING,
        TEXTURE_COMPRESSION_BC,
        TEXTURE_COMPRESSION_ETC2,
        TEXTURE_COMPRESSION_ASTC,
        
        // Buffer Features
        BUFFER_STORAGE,
        PERSISTENT_MAPPING,
        COHERENT_MAPPING,
        
        // Extension Features
        DESCRIPTOR_INDEXING,
        SCALAR_BLOCK_LAYOUT,
        EXTENDED_DYNAMIC_STATE,
        EXTENDED_DYNAMIC_STATE_2,
        EXTENDED_DYNAMIC_STATE_3,
        
        // SPIR-V Features
        SPIRV_1_4,
        SPIRV_1_5,
        SPIRV_1_6,
        GL_SPIRV
    }

    // ========================================================================
    // GL CONSTANTS BY VERSION
    // ========================================================================

    // -------------------- GL 1.1 Core --------------------
    public static final int GL11_POINTS = GL11.GL_POINTS;
    public static final int GL11_LINES = GL11.GL_LINES;
    public static final int GL11_LINE_LOOP = GL11.GL_LINE_LOOP;
    public static final int GL11_LINE_STRIP = GL11.GL_LINE_STRIP;
    public static final int GL11_TRIANGLES = GL11.GL_TRIANGLES;
    public static final int GL11_TRIANGLE_STRIP = GL11.GL_TRIANGLE_STRIP;
    public static final int GL11_TRIANGLE_FAN = GL11.GL_TRIANGLE_FAN;
    public static final int GL11_QUADS = GL11.GL_QUADS;
    public static final int GL11_QUAD_STRIP = GL11.GL_QUAD_STRIP;
    public static final int GL11_POLYGON = GL11.GL_POLYGON;

    // Primitive Types
    public static final int GL11_BYTE = GL11.GL_BYTE;
    public static final int GL11_UNSIGNED_BYTE = GL11.GL_UNSIGNED_BYTE;
    public static final int GL11_SHORT = GL11.GL_SHORT;
    public static final int GL11_UNSIGNED_SHORT = GL11.GL_UNSIGNED_SHORT;
    public static final int GL11_INT = GL11.GL_INT;
    public static final int GL11_UNSIGNED_INT = GL11.GL_UNSIGNED_INT;
    public static final int GL11_FLOAT = GL11.GL_FLOAT;
    public static final int GL11_DOUBLE = GL11.GL_DOUBLE;

    // Compare Functions
    public static final int GL11_NEVER = GL11.GL_NEVER;
    public static final int GL11_LESS = GL11.GL_LESS;
    public static final int GL11_EQUAL = GL11.GL_EQUAL;
    public static final int GL11_LEQUAL = GL11.GL_LEQUAL;
    public static final int GL11_GREATER = GL11.GL_GREATER;
    public static final int GL11_NOTEQUAL = GL11.GL_NOTEQUAL;
    public static final int GL11_GEQUAL = GL11.GL_GEQUAL;
    public static final int GL11_ALWAYS = GL11.GL_ALWAYS;

    // Blend Factors
    public static final int GL11_ZERO = GL11.GL_ZERO;
    public static final int GL11_ONE = GL11.GL_ONE;
    public static final int GL11_SRC_COLOR = GL11.GL_SRC_COLOR;
    public static final int GL11_ONE_MINUS_SRC_COLOR = GL11.GL_ONE_MINUS_SRC_COLOR;
    public static final int GL11_SRC_ALPHA = GL11.GL_SRC_ALPHA;
    public static final int GL11_ONE_MINUS_SRC_ALPHA = GL11.GL_ONE_MINUS_SRC_ALPHA;
    public static final int GL11_DST_ALPHA = GL11.GL_DST_ALPHA;
    public static final int GL11_ONE_MINUS_DST_ALPHA = GL11.GL_ONE_MINUS_DST_ALPHA;
    public static final int GL11_DST_COLOR = GL11.GL_DST_COLOR;
    public static final int GL11_ONE_MINUS_DST_COLOR = GL11.GL_ONE_MINUS_DST_COLOR;
    public static final int GL11_SRC_ALPHA_SATURATE = GL11.GL_SRC_ALPHA_SATURATE;

    // Capabilities (GL 1.1)
    public static final int GL11_DEPTH_TEST = GL11.GL_DEPTH_TEST;
    public static final int GL11_BLEND = GL11.GL_BLEND;
    public static final int GL11_CULL_FACE = GL11.GL_CULL_FACE;
    public static final int GL11_ALPHA_TEST = GL11.GL_ALPHA_TEST;
    public static final int GL11_SCISSOR_TEST = GL11.GL_SCISSOR_TEST;
    public static final int GL11_STENCIL_TEST = GL11.GL_STENCIL_TEST;
    public static final int GL11_FOG = GL11.GL_FOG;
    public static final int GL11_LIGHTING = GL11.GL_LIGHTING;
    public static final int GL11_TEXTURE_2D = GL11.GL_TEXTURE_2D;
    public static final int GL11_POLYGON_OFFSET_FILL = GL11.GL_POLYGON_OFFSET_FILL;
    public static final int GL11_COLOR_LOGIC_OP = GL11.GL_COLOR_LOGIC_OP;

    // Cull Modes
    public static final int GL11_FRONT = GL11.GL_FRONT;
    public static final int GL11_BACK = GL11.GL_BACK;
    public static final int GL11_FRONT_AND_BACK = GL11.GL_FRONT_AND_BACK;

    // Front Face
    public static final int GL11_CW = GL11.GL_CW;
    public static final int GL11_CCW = GL11.GL_CCW;

    // Polygon Mode
    public static final int GL11_POINT = GL11.GL_POINT;
    public static final int GL11_LINE = GL11.GL_LINE;
    public static final int GL11_FILL = GL11.GL_FILL;

    // Texture Parameters
    public static final int GL11_TEXTURE_MAG_FILTER = GL11.GL_TEXTURE_MAG_FILTER;
    public static final int GL11_TEXTURE_MIN_FILTER = GL11.GL_TEXTURE_MIN_FILTER;
    public static final int GL11_TEXTURE_WRAP_S = GL11.GL_TEXTURE_WRAP_S;
    public static final int GL11_TEXTURE_WRAP_T = GL11.GL_TEXTURE_WRAP_T;
    public static final int GL11_NEAREST = GL11.GL_NEAREST;
    public static final int GL11_LINEAR = GL11.GL_LINEAR;
    public static final int GL11_NEAREST_MIPMAP_NEAREST = GL11.GL_NEAREST_MIPMAP_NEAREST;
    public static final int GL11_LINEAR_MIPMAP_NEAREST = GL11.GL_LINEAR_MIPMAP_NEAREST;
    public static final int GL11_NEAREST_MIPMAP_LINEAR = GL11.GL_NEAREST_MIPMAP_LINEAR;
    public static final int GL11_LINEAR_MIPMAP_LINEAR = GL11.GL_LINEAR_MIPMAP_LINEAR;
    public static final int GL11_REPEAT = GL11.GL_REPEAT;

    // Formats (GL 1.1)
    public static final int GL11_RGBA = GL11.GL_RGBA;
    public static final int GL11_RGB = GL11.GL_RGB;
    public static final int GL11_DEPTH_COMPONENT = GL11.GL_DEPTH_COMPONENT;

    // -------------------- GL 1.2 --------------------
    public static final int GL12_TEXTURE_WRAP_R = GL12.GL_TEXTURE_WRAP_R;
    public static final int GL12_CLAMP_TO_EDGE = GL12.GL_CLAMP_TO_EDGE;
    public static final int GL12_TEXTURE_3D = GL12.GL_TEXTURE_3D;
    public static final int GL12_UNSIGNED_INT_8_8_8_8 = GL12.GL_UNSIGNED_INT_8_8_8_8;
    public static final int GL12_UNSIGNED_INT_8_8_8_8_REV = GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
    public static final int GL12_BGR = GL12.GL_BGR;
    public static final int GL12_BGRA = GL12.GL_BGRA;

    // -------------------- GL 1.3 --------------------
    public static final int GL13_TEXTURE0 = GL13.GL_TEXTURE0;
    public static final int GL13_TEXTURE_CUBE_MAP = GL13.GL_TEXTURE_CUBE_MAP;
    public static final int GL13_COMPRESSED_RGB = GL13.GL_COMPRESSED_RGB;
    public static final int GL13_COMPRESSED_RGBA = GL13.GL_COMPRESSED_RGBA;
    public static final int GL13_MULTISAMPLE = GL13.GL_MULTISAMPLE;
    public static final int GL13_CLAMP_TO_BORDER = GL13.GL_CLAMP_TO_BORDER;

    // -------------------- GL 1.4 --------------------
    public static final int GL14_MIRRORED_REPEAT = GL14.GL_MIRRORED_REPEAT;
    public static final int GL14_DEPTH_COMPONENT16 = GL14.GL_DEPTH_COMPONENT16;
    public static final int GL14_DEPTH_COMPONENT24 = GL14.GL_DEPTH_COMPONENT24;
    public static final int GL14_DEPTH_COMPONENT32 = GL14.GL_DEPTH_COMPONENT32;
    public static final int GL14_CONSTANT_COLOR = GL14.GL_CONSTANT_COLOR;
    public static final int GL14_ONE_MINUS_CONSTANT_COLOR = GL14.GL_ONE_MINUS_CONSTANT_COLOR;
    public static final int GL14_CONSTANT_ALPHA = GL14.GL_CONSTANT_ALPHA;
    public static final int GL14_ONE_MINUS_CONSTANT_ALPHA = GL14.GL_ONE_MINUS_CONSTANT_ALPHA;
    public static final int GL14_FUNC_ADD = GL14.GL_FUNC_ADD;
    public static final int GL14_FUNC_SUBTRACT = GL14.GL_FUNC_SUBTRACT;
    public static final int GL14_FUNC_REVERSE_SUBTRACT = GL14.GL_FUNC_REVERSE_SUBTRACT;
    public static final int GL14_MIN = GL14.GL_MIN;
    public static final int GL14_MAX = GL14.GL_MAX;
    public static final int GL14_INCR_WRAP = GL14.GL_INCR_WRAP;
    public static final int GL14_DECR_WRAP = GL14.GL_DECR_WRAP;

    // -------------------- GL 1.5 --------------------
    public static final int GL15_ARRAY_BUFFER = GL15.GL_ARRAY_BUFFER;
    public static final int GL15_ELEMENT_ARRAY_BUFFER = GL15.GL_ELEMENT_ARRAY_BUFFER;
    public static final int GL15_STREAM_DRAW = GL15.GL_STREAM_DRAW;
    public static final int GL15_STREAM_READ = GL15.GL_STREAM_READ;
    public static final int GL15_STREAM_COPY = GL15.GL_STREAM_COPY;
    public static final int GL15_STATIC_DRAW = GL15.GL_STATIC_DRAW;
    public static final int GL15_STATIC_READ = GL15.GL_STATIC_READ;
    public static final int GL15_STATIC_COPY = GL15.GL_STATIC_COPY;
    public static final int GL15_DYNAMIC_DRAW = GL15.GL_DYNAMIC_DRAW;
    public static final int GL15_DYNAMIC_READ = GL15.GL_DYNAMIC_READ;
    public static final int GL15_DYNAMIC_COPY = GL15.GL_DYNAMIC_COPY;
    public static final int GL15_READ_ONLY = GL15.GL_READ_ONLY;
    public static final int GL15_WRITE_ONLY = GL15.GL_WRITE_ONLY;
    public static final int GL15_READ_WRITE = GL15.GL_READ_WRITE;
    public static final int GL15_QUERY_RESULT = GL15.GL_QUERY_RESULT;
    public static final int GL15_QUERY_RESULT_AVAILABLE = GL15.GL_QUERY_RESULT_AVAILABLE;

    // -------------------- GL 2.0 --------------------
    public static final int GL20_VERTEX_SHADER = GL20.GL_VERTEX_SHADER;
    public static final int GL20_FRAGMENT_SHADER = GL20.GL_FRAGMENT_SHADER;
    public static final int GL20_COMPILE_STATUS = GL20.GL_COMPILE_STATUS;
    public static final int GL20_LINK_STATUS = GL20.GL_LINK_STATUS;
    public static final int GL20_VALIDATE_STATUS = GL20.GL_VALIDATE_STATUS;
    public static final int GL20_INFO_LOG_LENGTH = GL20.GL_INFO_LOG_LENGTH;
    public static final int GL20_ACTIVE_UNIFORMS = GL20.GL_ACTIVE_UNIFORMS;
    public static final int GL20_ACTIVE_ATTRIBUTES = GL20.GL_ACTIVE_ATTRIBUTES;
    public static final int GL20_MAX_VERTEX_ATTRIBS = GL20.GL_MAX_VERTEX_ATTRIBS;
    public static final int GL20_MAX_TEXTURE_IMAGE_UNITS = GL20.GL_MAX_TEXTURE_IMAGE_UNITS;
    public static final int GL20_SHADING_LANGUAGE_VERSION = GL20.GL_SHADING_LANGUAGE_VERSION;

    // -------------------- GL 2.1 --------------------
    public static final int GL21_SRGB = GL21.GL_SRGB;
    public static final int GL21_SRGB8 = GL21.GL_SRGB8;
    public static final int GL21_SRGB_ALPHA = GL21.GL_SRGB_ALPHA;
    public static final int GL21_SRGB8_ALPHA8 = GL21.GL_SRGB8_ALPHA8;
    public static final int GL21_PIXEL_PACK_BUFFER = GL21.GL_PIXEL_PACK_BUFFER;
    public static final int GL21_PIXEL_UNPACK_BUFFER = GL21.GL_PIXEL_UNPACK_BUFFER;

    // -------------------- GL 3.0 --------------------
    public static final int GL30_RGBA16F = GL30.GL_RGBA16F;
    public static final int GL30_RGBA32F = GL30.GL_RGBA32F;
    public static final int GL30_RGB16F = GL30.GL_RGB16F;
    public static final int GL30_RGB32F = GL30.GL_RGB32F;
    public static final int GL30_R8 = GL30.GL_R8;
    public static final int GL30_R16 = GL30.GL_R16;
    public static final int GL30_R16F = GL30.GL_R16F;
    public static final int GL30_R32F = GL30.GL_R32F;
    public static final int GL30_RG8 = GL30.GL_RG8;
    public static final int GL30_RG16 = GL30.GL_RG16;
    public static final int GL30_RG16F = GL30.GL_RG16F;
    public static final int GL30_RG32F = GL30.GL_RG32F;
    public static final int GL30_DEPTH_COMPONENT32F = GL30.GL_DEPTH_COMPONENT32F;
    public static final int GL30_DEPTH24_STENCIL8 = GL30.GL_DEPTH24_STENCIL8;
    public static final int GL30_DEPTH32F_STENCIL8 = GL30.GL_DEPTH32F_STENCIL8;
    public static final int GL30_FRAMEBUFFER = GL30.GL_FRAMEBUFFER;
    public static final int GL30_RENDERBUFFER = GL30.GL_RENDERBUFFER;
    public static final int GL30_COLOR_ATTACHMENT0 = GL30.GL_COLOR_ATTACHMENT0;
    public static final int GL30_DEPTH_ATTACHMENT = GL30.GL_DEPTH_ATTACHMENT;
    public static final int GL30_STENCIL_ATTACHMENT = GL30.GL_STENCIL_ATTACHMENT;
    public static final int GL30_DEPTH_STENCIL_ATTACHMENT = GL30.GL_DEPTH_STENCIL_ATTACHMENT;
    public static final int GL30_FRAMEBUFFER_COMPLETE = GL30.GL_FRAMEBUFFER_COMPLETE;
    public static final int GL30_TEXTURE_2D_ARRAY = GL30.GL_TEXTURE_2D_ARRAY;
    public static final int GL30_VERTEX_ARRAY_BINDING = GL30.GL_VERTEX_ARRAY_BINDING;
    public static final int GL30_TRANSFORM_FEEDBACK_BUFFER = GL30.GL_TRANSFORM_FEEDBACK_BUFFER;
    public static final int GL30_MAP_READ_BIT = GL30.GL_MAP_READ_BIT;
    public static final int GL30_MAP_WRITE_BIT = GL30.GL_MAP_WRITE_BIT;
    public static final int GL30_MAP_INVALIDATE_RANGE_BIT = GL30.GL_MAP_INVALIDATE_RANGE_BIT;
    public static final int GL30_MAP_INVALIDATE_BUFFER_BIT = GL30.GL_MAP_INVALIDATE_BUFFER_BIT;
    public static final int GL30_MAP_FLUSH_EXPLICIT_BIT = GL30.GL_MAP_FLUSH_EXPLICIT_BIT;
    public static final int GL30_MAP_UNSYNCHRONIZED_BIT = GL30.GL_MAP_UNSYNCHRONIZED_BIT;

    // -------------------- GL 3.1 --------------------
    public static final int GL31_UNIFORM_BUFFER = GL31.GL_UNIFORM_BUFFER;
    public static final int GL31_UNIFORM_BUFFER_BINDING = GL31.GL_UNIFORM_BUFFER_BINDING;
    public static final int GL31_MAX_UNIFORM_BUFFER_BINDINGS = GL31.GL_MAX_UNIFORM_BUFFER_BINDINGS;
    public static final int GL31_TEXTURE_RECTANGLE = GL31.GL_TEXTURE_RECTANGLE;
    public static final int GL31_TEXTURE_BUFFER = GL31.GL_TEXTURE_BUFFER;
    public static final int GL31_COPY_READ_BUFFER = GL31.GL_COPY_READ_BUFFER;
    public static final int GL31_COPY_WRITE_BUFFER = GL31.GL_COPY_WRITE_BUFFER;
    public static final int GL31_PRIMITIVE_RESTART = GL31.GL_PRIMITIVE_RESTART;

    // -------------------- GL 3.2 --------------------
    public static final int GL32_GEOMETRY_SHADER = GL32.GL_GEOMETRY_SHADER;
    public static final int GL32_DEPTH_CLAMP = GL32.GL_DEPTH_CLAMP;
    public static final int GL32_TEXTURE_CUBE_MAP_SEAMLESS = GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS;
    public static final int GL32_FIRST_VERTEX_CONVENTION = GL32.GL_FIRST_VERTEX_CONVENTION;
    public static final int GL32_LAST_VERTEX_CONVENTION = GL32.GL_LAST_VERTEX_CONVENTION;
    public static final int GL32_PROGRAM_POINT_SIZE = GL32.GL_PROGRAM_POINT_SIZE;
    public static final int GL32_TEXTURE_2D_MULTISAMPLE = GL32.GL_TEXTURE_2D_MULTISAMPLE;
    public static final int GL32_TEXTURE_2D_MULTISAMPLE_ARRAY = GL32.GL_TEXTURE_2D_MULTISAMPLE_ARRAY;
    public static final int GL32_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS = GL32.GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS;
    public static final int GL32_MAX_GEOMETRY_OUTPUT_VERTICES = GL32.GL_MAX_GEOMETRY_OUTPUT_VERTICES;
    public static final int GL32_SYNC_GPU_COMMANDS_COMPLETE = GL32.GL_SYNC_GPU_COMMANDS_COMPLETE;
    public static final int GL32_TIMEOUT_EXPIRED = GL32.GL_TIMEOUT_EXPIRED;
    public static final int GL32_CONDITION_SATISFIED = GL32.GL_CONDITION_SATISFIED;
    public static final int GL32_WAIT_FAILED = GL32.GL_WAIT_FAILED;

    // -------------------- GL 3.3 --------------------
    public static final int GL33_SAMPLER_BINDING = GL33.GL_SAMPLER_BINDING;
    public static final int GL33_ANY_SAMPLES_PASSED = GL33.GL_ANY_SAMPLES_PASSED;
    public static final int GL33_TIME_ELAPSED = GL33.GL_TIME_ELAPSED;
    public static final int GL33_TIMESTAMP = GL33.GL_TIMESTAMP;
    public static final int GL33_VERTEX_ATTRIB_ARRAY_DIVISOR = GL33.GL_VERTEX_ATTRIB_ARRAY_DIVISOR;

    // -------------------- GL 4.0 --------------------
    public static final int GL40_TESS_CONTROL_SHADER = GL40.GL_TESS_CONTROL_SHADER;
    public static final int GL40_TESS_EVALUATION_SHADER = GL40.GL_TESS_EVALUATION_SHADER;
    public static final int GL40_PATCHES = GL40.GL_PATCHES;
    public static final int GL40_PATCH_VERTICES = GL40.GL_PATCH_VERTICES;
    public static final int GL40_MAX_PATCH_VERTICES = GL40.GL_MAX_PATCH_VERTICES;
    public static final int GL40_SAMPLE_SHADING = GL40.GL_SAMPLE_SHADING;
    public static final int GL40_MIN_SAMPLE_SHADING_VALUE = GL40.GL_MIN_SAMPLE_SHADING_VALUE;
    public static final int GL40_DRAW_INDIRECT_BUFFER = GL40.GL_DRAW_INDIRECT_BUFFER;
    public static final int GL40_GEOMETRY_SHADER_INVOCATIONS = GL40.GL_GEOMETRY_SHADER_INVOCATIONS;
    public static final int GL40_TRANSFORM_FEEDBACK = GL40.GL_TRANSFORM_FEEDBACK;
    public static final int GL40_TRANSFORM_FEEDBACK_BUFFER_PAUSED = GL40.GL_TRANSFORM_FEEDBACK_BUFFER_PAUSED;
    public static final int GL40_TRANSFORM_FEEDBACK_BUFFER_ACTIVE = GL40.GL_TRANSFORM_FEEDBACK_BUFFER_ACTIVE;

    // -------------------- GL 4.1 --------------------
    public static final int GL41_PROGRAM_BINARY_RETRIEVABLE_HINT = GL41.GL_PROGRAM_BINARY_RETRIEVABLE_HINT;
    public static final int GL41_PROGRAM_BINARY_LENGTH = GL41.GL_PROGRAM_BINARY_LENGTH;
    public static final int GL41_NUM_PROGRAM_BINARY_FORMATS = GL41.GL_NUM_PROGRAM_BINARY_FORMATS;
    public static final int GL41_VIEWPORT_BOUNDS_RANGE = GL41.GL_VIEWPORT_BOUNDS_RANGE;
    public static final int GL41_MAX_VIEWPORTS = GL41.GL_MAX_VIEWPORTS;

    // -------------------- GL 4.2 --------------------
    public static final int GL42_ATOMIC_COUNTER_BUFFER = GL42.GL_ATOMIC_COUNTER_BUFFER;
    public static final int GL42_MAX_ATOMIC_COUNTER_BUFFER_SIZE = GL42.GL_MAX_ATOMIC_COUNTER_BUFFER_SIZE;
    public static final int GL42_IMAGE_BINDING_NAME = GL42.GL_IMAGE_BINDING_NAME;
    public static final int GL42_IMAGE_BINDING_LEVEL = GL42.GL_IMAGE_BINDING_LEVEL;
    public static final int GL42_IMAGE_BINDING_LAYERED = GL42.GL_IMAGE_BINDING_LAYERED;
    public static final int GL42_IMAGE_BINDING_LAYER = GL42.GL_IMAGE_BINDING_LAYER;
    public static final int GL42_IMAGE_BINDING_ACCESS = GL42.GL_IMAGE_BINDING_ACCESS;
    public static final int GL42_IMAGE_BINDING_FORMAT = GL42.GL_IMAGE_BINDING_FORMAT;
    public static final int GL42_COMPRESSED_RGBA_BPTC_UNORM = GL42.GL_COMPRESSED_RGBA_BPTC_UNORM;
    public static final int GL42_COMPRESSED_SRGB_ALPHA_BPTC_UNORM = GL42.GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM;
    public static final int GL42_COMPRESSED_RGB_BPTC_SIGNED_FLOAT = GL42.GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT;
    public static final int GL42_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT = GL42.GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT;

    // -------------------- GL 4.3 --------------------
    public static final int GL43_COMPUTE_SHADER = GL43.GL_COMPUTE_SHADER;
    public static final int GL43_SHADER_STORAGE_BUFFER = GL43.GL_SHADER_STORAGE_BUFFER;
    public static final int GL43_MAX_COMPUTE_WORK_GROUP_COUNT = GL43.GL_MAX_COMPUTE_WORK_GROUP_COUNT;
    public static final int GL43_MAX_COMPUTE_WORK_GROUP_SIZE = GL43.GL_MAX_COMPUTE_WORK_GROUP_SIZE;
    public static final int GL43_MAX_COMPUTE_WORK_GROUP_INVOCATIONS = GL43.GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS;
    public static final int GL43_DISPATCH_INDIRECT_BUFFER = GL43.GL_DISPATCH_INDIRECT_BUFFER;
    public static final int GL43_DEBUG_OUTPUT = GL43.GL_DEBUG_OUTPUT;
    public static final int GL43_DEBUG_OUTPUT_SYNCHRONOUS = GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS;
    public static final int GL43_DEBUG_SEVERITY_HIGH = GL43.GL_DEBUG_SEVERITY_HIGH;
    public static final int GL43_DEBUG_SEVERITY_MEDIUM = GL43.GL_DEBUG_SEVERITY_MEDIUM;
    public static final int GL43_DEBUG_SEVERITY_LOW = GL43.GL_DEBUG_SEVERITY_LOW;
    public static final int GL43_DEBUG_SEVERITY_NOTIFICATION = GL43.GL_DEBUG_SEVERITY_NOTIFICATION;
    public static final int GL43_TEXTURE_BUFFER_OFFSET = GL43.GL_TEXTURE_BUFFER_OFFSET;
    public static final int GL43_TEXTURE_BUFFER_SIZE = GL43.GL_TEXTURE_BUFFER_SIZE;
    public static final int GL43_TEXTURE_VIEW = GL43.GL_TEXTURE_VIEW;
    public static final int GL43_INTERNALFORMAT_SUPPORTED = GL43.GL_INTERNALFORMAT_SUPPORTED;
    public static final int GL43_VERTEX_ATTRIB_BINDING = GL43.GL_VERTEX_ATTRIB_BINDING;
    public static final int GL43_VERTEX_ATTRIB_RELATIVE_OFFSET = GL43.GL_VERTEX_ATTRIB_RELATIVE_OFFSET;
    public static final int GL43_VERTEX_BINDING_DIVISOR = GL43.GL_VERTEX_BINDING_DIVISOR;
    public static final int GL43_VERTEX_BINDING_OFFSET = GL43.GL_VERTEX_BINDING_OFFSET;
    public static final int GL43_VERTEX_BINDING_STRIDE = GL43.GL_VERTEX_BINDING_STRIDE;

    // -------------------- GL 4.4 --------------------
    public static final int GL44_BUFFER_STORAGE_FLAGS = GL44.GL_BUFFER_STORAGE_FLAGS;
    public static final int GL44_MAP_PERSISTENT_BIT = GL44.GL_MAP_PERSISTENT_BIT;
    public static final int GL44_MAP_COHERENT_BIT = GL44.GL_MAP_COHERENT_BIT;
    public static final int GL44_DYNAMIC_STORAGE_BIT = GL44.GL_DYNAMIC_STORAGE_BIT;
    public static final int GL44_CLIENT_STORAGE_BIT = GL44.GL_CLIENT_STORAGE_BIT;
    public static final int GL44_CLEAR_TEXTURE = GL44.GL_CLEAR_TEXTURE;
    public static final int GL44_QUERY_BUFFER = GL44.GL_QUERY_BUFFER;
    public static final int GL44_QUERY_BUFFER_BINDING = GL44.GL_QUERY_BUFFER_BINDING;
    public static final int GL44_QUERY_RESULT_NO_WAIT = GL44.GL_QUERY_RESULT_NO_WAIT;
    public static final int GL44_MIRROR_CLAMP_TO_EDGE = GL44.GL_MIRROR_CLAMP_TO_EDGE;

    // -------------------- GL 4.5 --------------------
    public static final int GL45_CONTEXT_LOST = GL45.GL_CONTEXT_LOST;
    public static final int GL45_TEXTURE_TARGET = GL45.GL_TEXTURE_TARGET;
    public static final int GL45_QUERY_TARGET = GL45.GL_QUERY_TARGET;
    public static final int GL45_CLIP_ORIGIN = GL45.GL_CLIP_ORIGIN;
    public static final int GL45_CLIP_DEPTH_MODE = GL45.GL_CLIP_DEPTH_MODE;
    public static final int GL45_NEGATIVE_ONE_TO_ONE = GL45.GL_NEGATIVE_ONE_TO_ONE;
    public static final int GL45_ZERO_TO_ONE = GL45.GL_ZERO_TO_ONE;
    public static final int GL45_LOWER_LEFT = GL45.GL_LOWER_LEFT;
    public static final int GL45_UPPER_LEFT = GL45.GL_UPPER_LEFT;

    // -------------------- GL 4.6 --------------------
    public static final int GL46_PARAMETER_BUFFER = GL46.GL_PARAMETER_BUFFER;
    public static final int GL46_POLYGON_OFFSET_CLAMP = GL46.GL_POLYGON_OFFSET_CLAMP;
    public static final int GL46_SHADER_BINARY_FORMAT_SPIR_V = GL46.GL_SHADER_BINARY_FORMAT_SPIR_V;
    public static final int GL46_SPIR_V_BINARY = GL46.GL_SPIR_V_BINARY;
    public static final int GL46_SPIR_V_EXTENSIONS = GL46.GL_SPIR_V_EXTENSIONS;
    public static final int GL46_NUM_SPIR_V_EXTENSIONS = GL46.GL_NUM_SPIR_V_EXTENSIONS;
    public static final int GL46_TEXTURE_MAX_ANISOTROPY = GL46.GL_TEXTURE_MAX_ANISOTROPY;
    public static final int GL46_MAX_TEXTURE_MAX_ANISOTROPY = GL46.GL_MAX_TEXTURE_MAX_ANISOTROPY;

    // ========================================================================
    // VULKAN CONSTANTS BY VERSION
    // ========================================================================

    // -------------------- VK 1.0 Core --------------------
    public static final int VK10_PRIMITIVE_TOPOLOGY_POINT_LIST = VK10.VK_PRIMITIVE_TOPOLOGY_POINT_LIST;
    public static final int VK10_PRIMITIVE_TOPOLOGY_LINE_LIST = VK10.VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
    public static final int VK10_PRIMITIVE_TOPOLOGY_LINE_STRIP = VK10.VK_PRIMITIVE_TOPOLOGY_LINE_STRIP;
    public static final int VK10_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST = VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    public static final int VK10_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP = VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
    public static final int VK10_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN = VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN;
    public static final int VK10_PRIMITIVE_TOPOLOGY_LINE_LIST_WITH_ADJACENCY = VK10.VK_PRIMITIVE_TOPOLOGY_LINE_LIST_WITH_ADJACENCY;
    public static final int VK10_PRIMITIVE_TOPOLOGY_LINE_STRIP_WITH_ADJACENCY = VK10.VK_PRIMITIVE_TOPOLOGY_LINE_STRIP_WITH_ADJACENCY;
    public static final int VK10_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST_WITH_ADJACENCY = VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST_WITH_ADJACENCY;
    public static final int VK10_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP_WITH_ADJACENCY = VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP_WITH_ADJACENCY;
    public static final int VK10_PRIMITIVE_TOPOLOGY_PATCH_LIST = VK10.VK_PRIMITIVE_TOPOLOGY_PATCH_LIST;

    // Compare Ops
    public static final int VK10_COMPARE_OP_NEVER = VK10.VK_COMPARE_OP_NEVER;
    public static final int VK10_COMPARE_OP_LESS = VK10.VK_COMPARE_OP_LESS;
    public static final int VK10_COMPARE_OP_EQUAL = VK10.VK_COMPARE_OP_EQUAL;
    public static final int VK10_COMPARE_OP_LESS_OR_EQUAL = VK10.VK_COMPARE_OP_LESS_OR_EQUAL;
    public static final int VK10_COMPARE_OP_GREATER = VK10.VK_COMPARE_OP_GREATER;
    public static final int VK10_COMPARE_OP_NOT_EQUAL = VK10.VK_COMPARE_OP_NOT_EQUAL;
    public static final int VK10_COMPARE_OP_GREATER_OR_EQUAL = VK10.VK_COMPARE_OP_GREATER_OR_EQUAL;
    public static final int VK10_COMPARE_OP_ALWAYS = VK10.VK_COMPARE_OP_ALWAYS;

    // Blend Factors
    public static final int VK10_BLEND_FACTOR_ZERO = VK10.VK_BLEND_FACTOR_ZERO;
    public static final int VK10_BLEND_FACTOR_ONE = VK10.VK_BLEND_FACTOR_ONE;
    public static final int VK10_BLEND_FACTOR_SRC_COLOR = VK10.VK_BLEND_FACTOR_SRC_COLOR;
    public static final int VK10_BLEND_FACTOR_ONE_MINUS_SRC_COLOR = VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
    public static final int VK10_BLEND_FACTOR_DST_COLOR = VK10.VK_BLEND_FACTOR_DST_COLOR;
    public static final int VK10_BLEND_FACTOR_ONE_MINUS_DST_COLOR = VK10.VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
    public static final int VK10_BLEND_FACTOR_SRC_ALPHA = VK10.VK_BLEND_FACTOR_SRC_ALPHA;
    public static final int VK10_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA = VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
    public static final int VK10_BLEND_FACTOR_DST_ALPHA = VK10.VK_BLEND_FACTOR_DST_ALPHA;
    public static final int VK10_BLEND_FACTOR_ONE_MINUS_DST_ALPHA = VK10.VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
    public static final int VK10_BLEND_FACTOR_CONSTANT_COLOR = VK10.VK_BLEND_FACTOR_CONSTANT_COLOR;
    public static final int VK10_BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR = VK10.VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR;
    public static final int VK10_BLEND_FACTOR_CONSTANT_ALPHA = VK10.VK_BLEND_FACTOR_CONSTANT_ALPHA;
    public static final int VK10_BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA = VK10.VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA;
    public static final int VK10_BLEND_FACTOR_SRC_ALPHA_SATURATE = VK10.VK_BLEND_FACTOR_SRC_ALPHA_SATURATE;

    // Blend Ops
    public static final int VK10_BLEND_OP_ADD = VK10.VK_BLEND_OP_ADD;
    public static final int VK10_BLEND_OP_SUBTRACT = VK10.VK_BLEND_OP_SUBTRACT;
    public static final int VK10_BLEND_OP_REVERSE_SUBTRACT = VK10.VK_BLEND_OP_REVERSE_SUBTRACT;
    public static final int VK10_BLEND_OP_MIN = VK10.VK_BLEND_OP_MIN;
    public static final int VK10_BLEND_OP_MAX = VK10.VK_BLEND_OP_MAX;

    // Cull Modes
    public static final int VK10_CULL_MODE_NONE = VK10.VK_CULL_MODE_NONE;
    public static final int VK10_CULL_MODE_FRONT_BIT = VK10.VK_CULL_MODE_FRONT_BIT;
    public static final int VK10_CULL_MODE_BACK_BIT = VK10.VK_CULL_MODE_BACK_BIT;
    public static final int VK10_CULL_MODE_FRONT_AND_BACK = VK10.VK_CULL_MODE_FRONT_AND_BACK;

    // Front Face
    public static final int VK10_FRONT_FACE_COUNTER_CLOCKWISE = VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE;
    public static final int VK10_FRONT_FACE_CLOCKWISE = VK10.VK_FRONT_FACE_CLOCKWISE;

    // Polygon Mode
    public static final int VK10_POLYGON_MODE_FILL = VK10.VK_POLYGON_MODE_FILL;
    public static final int VK10_POLYGON_MODE_LINE = VK10.VK_POLYGON_MODE_LINE;
    public static final int VK10_POLYGON_MODE_POINT = VK10.VK_POLYGON_MODE_POINT;

    // Shader Stages
    public static final int VK10_SHADER_STAGE_VERTEX_BIT = VK10.VK_SHADER_STAGE_VERTEX_BIT;
    public static final int VK10_SHADER_STAGE_TESSELLATION_CONTROL_BIT = VK10.VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT;
    public static final int VK10_SHADER_STAGE_TESSELLATION_EVALUATION_BIT = VK10.VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT;
    public static final int VK10_SHADER_STAGE_GEOMETRY_BIT = VK10.VK_SHADER_STAGE_GEOMETRY_BIT;
    public static final int VK10_SHADER_STAGE_FRAGMENT_BIT = VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
    public static final int VK10_SHADER_STAGE_COMPUTE_BIT = VK10.VK_SHADER_STAGE_COMPUTE_BIT;
    public static final int VK10_SHADER_STAGE_ALL_GRAPHICS = VK10.VK_SHADER_STAGE_ALL_GRAPHICS;
    public static final int VK10_SHADER_STAGE_ALL = VK10.VK_SHADER_STAGE_ALL;

    // Buffer Usage
    public static final int VK10_BUFFER_USAGE_TRANSFER_SRC_BIT = VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
    public static final int VK10_BUFFER_USAGE_TRANSFER_DST_BIT = VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
    public static final int VK10_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT = VK10.VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT;
    public static final int VK10_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT = VK10.VK_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT;
    public static final int VK10_BUFFER_USAGE_UNIFORM_BUFFER_BIT = VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
    public static final int VK10_BUFFER_USAGE_STORAGE_BUFFER_BIT = VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
    public static final int VK10_BUFFER_USAGE_INDEX_BUFFER_BIT = VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
    public static final int VK10_BUFFER_USAGE_VERTEX_BUFFER_BIT = VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
    public static final int VK10_BUFFER_USAGE_INDIRECT_BUFFER_BIT = VK10.VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT;

    // Formats
    public static final int VK10_FORMAT_UNDEFINED = VK10.VK_FORMAT_UNDEFINED;
    public static final int VK10_FORMAT_R8_UNORM = VK10.VK_FORMAT_R8_UNORM;
    public static final int VK10_FORMAT_R8_SNORM = VK10.VK_FORMAT_R8_SNORM;
    public static final int VK10_FORMAT_R8_UINT = VK10.VK_FORMAT_R8_UINT;
    public static final int VK10_FORMAT_R8_SINT = VK10.VK_FORMAT_R8_SINT;
    public static final int VK10_FORMAT_R8_SRGB = VK10.VK_FORMAT_R8_SRGB;
    public static final int VK10_FORMAT_R8G8_UNORM = VK10.VK_FORMAT_R8G8_UNORM;
    public static final int VK10_FORMAT_R8G8B8_UNORM = VK10.VK_FORMAT_R8G8B8_UNORM;
    public static final int VK10_FORMAT_R8G8B8_SRGB = VK10.VK_FORMAT_R8G8B8_SRGB;
    public static final int VK10_FORMAT_R8G8B8A8_UNORM = VK10.VK_FORMAT_R8G8B8A8_UNORM;
    public static final int VK10_FORMAT_R8G8B8A8_SNORM = VK10.VK_FORMAT_R8G8B8A8_SNORM;
    public static final int VK10_FORMAT_R8G8B8A8_UINT = VK10.VK_FORMAT_R8G8B8A8_UINT;
    public static final int VK10_FORMAT_R8G8B8A8_SINT = VK10.VK_FORMAT_R8G8B8A8_SINT;
    public static final int VK10_FORMAT_R8G8B8A8_SRGB = VK10.VK_FORMAT_R8G8B8A8_SRGB;
    public static final int VK10_FORMAT_B8G8R8A8_UNORM = VK10.VK_FORMAT_B8G8R8A8_UNORM;
    public static final int VK10_FORMAT_B8G8R8A8_SRGB = VK10.VK_FORMAT_B8G8R8A8_SRGB;
    public static final int VK10_FORMAT_R16_UNORM = VK10.VK_FORMAT_R16_UNORM;
    public static final int VK10_FORMAT_R16_SFLOAT = VK10.VK_FORMAT_R16_SFLOAT;
    public static final int VK10_FORMAT_R16G16_SFLOAT = VK10.VK_FORMAT_R16G16_SFLOAT;
    public static final int VK10_FORMAT_R16G16B16A16_UNORM = VK10.VK_FORMAT_R16G16B16A16_UNORM;
    public static final int VK10_FORMAT_R16G16B16A16_SFLOAT = VK10.VK_FORMAT_R16G16B16A16_SFLOAT;
    public static final int VK10_FORMAT_R32_UINT = VK10.VK_FORMAT_R32_UINT;
    public static final int VK10_FORMAT_R32_SINT = VK10.VK_FORMAT_R32_SINT;
    public static final int VK10_FORMAT_R32_SFLOAT = VK10.VK_FORMAT_R32_SFLOAT;
    public static final int VK10_FORMAT_R32G32_SFLOAT = VK10.VK_FORMAT_R32G32_SFLOAT;
    public static final int VK10_FORMAT_R32G32B32_SFLOAT = VK10.VK_FORMAT_R32G32B32_SFLOAT;
    public static final int VK10_FORMAT_R32G32B32A32_SFLOAT = VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
    public static final int VK10_FORMAT_R64_SFLOAT = VK10.VK_FORMAT_R64_SFLOAT;
    public static final int VK10_FORMAT_D16_UNORM = VK10.VK_FORMAT_D16_UNORM;
    public static final int VK10_FORMAT_D32_SFLOAT = VK10.VK_FORMAT_D32_SFLOAT;
    public static final int VK10_FORMAT_D24_UNORM_S8_UINT = VK10.VK_FORMAT_D24_UNORM_S8_UINT;
    public static final int VK10_FORMAT_D32_SFLOAT_S8_UINT = VK10.VK_FORMAT_D32_SFLOAT_S8_UINT;

    // Sampler Address Mode
    public static final int VK10_SAMPLER_ADDRESS_MODE_REPEAT = VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT;
    public static final int VK10_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT = VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT;
    public static final int VK10_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE = VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    public static final int VK10_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER = VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
    public static final int VK10_SAMPLER_ADDRESS_MODE_MIRROR_CLAMP_TO_EDGE = VK10.VK_SAMPLER_ADDRESS_MODE_MIRROR_CLAMP_TO_EDGE;

    // Filter
    public static final int VK10_FILTER_NEAREST = VK10.VK_FILTER_NEAREST;
    public static final int VK10_FILTER_LINEAR = VK10.VK_FILTER_LINEAR;

    // Mipmap Mode
    public static final int VK10_SAMPLER_MIPMAP_MODE_NEAREST = VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST;
    public static final int VK10_SAMPLER_MIPMAP_MODE_LINEAR = VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR;

    // -------------------- VK 1.1 --------------------
    public static final int VK11_STRUCTURE_TYPE_PHYSICAL_DEVICE_SUBGROUP_PROPERTIES = VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SUBGROUP_PROPERTIES;
    public static final int VK11_SUBGROUP_FEATURE_BASIC_BIT = VK11.VK_SUBGROUP_FEATURE_BASIC_BIT;
    public static final int VK11_SUBGROUP_FEATURE_VOTE_BIT = VK11.VK_SUBGROUP_FEATURE_VOTE_BIT;
    public static final int VK11_SUBGROUP_FEATURE_ARITHMETIC_BIT = VK11.VK_SUBGROUP_FEATURE_ARITHMETIC_BIT;
    public static final int VK11_SUBGROUP_FEATURE_BALLOT_BIT = VK11.VK_SUBGROUP_FEATURE_BALLOT_BIT;
    public static final int VK11_SUBGROUP_FEATURE_SHUFFLE_BIT = VK11.VK_SUBGROUP_FEATURE_SHUFFLE_BIT;
    public static final int VK11_SUBGROUP_FEATURE_SHUFFLE_RELATIVE_BIT = VK11.VK_SUBGROUP_FEATURE_SHUFFLE_RELATIVE_BIT;
    public static final int VK11_SUBGROUP_FEATURE_CLUSTERED_BIT = VK11.VK_SUBGROUP_FEATURE_CLUSTERED_BIT;
    public static final int VK11_SUBGROUP_FEATURE_QUAD_BIT = VK11.VK_SUBGROUP_FEATURE_QUAD_BIT;

    // -------------------- VK 1.2 --------------------
    public static final int VK12_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT = VK12.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;
    public static final int VK12_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT = VK12.VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT;
    public static final int VK12_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT = VK12.VK_DESCRIPTOR_BINDING_UPDATE_AFTER_BIND_BIT;
    public static final int VK12_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT = VK12.VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT;
    public static final int VK12_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT = VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT;
    public static final int VK12_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT = VK12.VK_DESCRIPTOR_BINDING_VARIABLE_DESCRIPTOR_COUNT_BIT;

    // -------------------- VK 1.3 --------------------
    public static final int VK13_PIPELINE_STAGE_2_NONE = VK13.VK_PIPELINE_STAGE_2_NONE;
    public static final long VK13_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT = VK13.VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT;
    public static final long VK13_PIPELINE_STAGE_2_DRAW_INDIRECT_BIT = VK13.VK_PIPELINE_STAGE_2_DRAW_INDIRECT_BIT;
    public static final long VK13_PIPELINE_STAGE_2_VERTEX_INPUT_BIT = VK13.VK_PIPELINE_STAGE_2_VERTEX_INPUT_BIT;
    public static final long VK13_PIPELINE_STAGE_2_VERTEX_SHADER_BIT = VK13.VK_PIPELINE_STAGE_2_VERTEX_SHADER_BIT;
    public static final long VK13_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT = VK13.VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT;
    public static final long VK13_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT = VK13.VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT;
    public static final long VK13_PIPELINE_STAGE_2_ALL_GRAPHICS_BIT = VK13.VK_PIPELINE_STAGE_2_ALL_GRAPHICS_BIT;
    public static final long VK13_PIPELINE_STAGE_2_ALL_COMMANDS_BIT = VK13.VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT;
    public static final long VK13_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT = VK13.VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT;
    public static final long VK13_PIPELINE_STAGE_2_TRANSFER_BIT = VK13.VK_PIPELINE_STAGE_2_TRANSFER_BIT;

    // ========================================================================
    // SPIR-V CONSTANTS
    // ========================================================================

    public static final int SPIRV_MAGIC_NUMBER = 0x07230203;
    public static final int SPIRV_OP_CAPABILITY = 17;
    public static final int SPIRV_OP_EXTENSION = 10;
    public static final int SPIRV_OP_EXT_INST_IMPORT = 11;
    public static final int SPIRV_OP_MEMORY_MODEL = 14;
    public static final int SPIRV_OP_ENTRY_POINT = 15;
    public static final int SPIRV_OP_EXECUTION_MODE = 16;
    public static final int SPIRV_OP_DECORATE = 71;
    public static final int SPIRV_OP_MEMBER_DECORATE = 72;

    // Capabilities
    public static final int SPIRV_CAP_MATRIX = 0;
    public static final int SPIRV_CAP_SHADER = 1;
    public static final int SPIRV_CAP_GEOMETRY = 2;
    public static final int SPIRV_CAP_TESSELLATION = 3;
    public static final int SPIRV_CAP_ADDRESSES = 4;
    public static final int SPIRV_CAP_LINKAGE = 5;
    public static final int SPIRV_CAP_KERNEL = 6;
    public static final int SPIRV_CAP_FLOAT64 = 10;
    public static final int SPIRV_CAP_INT64 = 11;
    public static final int SPIRV_CAP_INT16 = 22;
    public static final int SPIRV_CAP_TESSELLATION_POINT_SIZE = 23;
    public static final int SPIRV_CAP_GEOMETRY_POINT_SIZE = 24;
    public static final int SPIRV_CAP_IMAGE_GATHER_EXTENDED = 25;
    public static final int SPIRV_CAP_STORAGE_IMAGE_MULTISAMPLE = 27;
    public static final int SPIRV_CAP_CLIP_DISTANCE = 32;
    public static final int SPIRV_CAP_CULL_DISTANCE = 33;
    public static final int SPIRV_CAP_IMAGE_CUBE_ARRAY = 34;
    public static final int SPIRV_CAP_SAMPLE_RATE_SHADING = 35;
    public static final int SPIRV_CAP_SAMPLED_RECT = 37;
    public static final int SPIRV_CAP_INPUT_ATTACHMENT = 40;
    public static final int SPIRV_CAP_SPARSE_RESIDENCY = 41;
    public static final int SPIRV_CAP_MIN_LOD = 42;
    public static final int SPIRV_CAP_SAMPLED_1D = 43;
    public static final int SPIRV_CAP_IMAGE_1D = 44;
    public static final int SPIRV_CAP_SAMPLED_CUBE_ARRAY = 45;
    public static final int SPIRV_CAP_SAMPLED_BUFFER = 46;
    public static final int SPIRV_CAP_IMAGE_MS_ARRAY = 48;
    public static final int SPIRV_CAP_STORAGE_IMAGE_EXTENDED_FORMATS = 49;
    public static final int SPIRV_CAP_IMAGE_QUERY = 50;
    public static final int SPIRV_CAP_DERIVATIVE_CONTROL = 51;
    public static final int SPIRV_CAP_INTERPOLATION_FUNCTION = 52;
    public static final int SPIRV_CAP_TRANSFORM_FEEDBACK = 53;
    public static final int SPIRV_CAP_GEOMETRY_STREAMS = 54;
    public static final int SPIRV_CAP_STORAGE_IMAGE_READ_WITHOUT_FORMAT = 55;
    public static final int SPIRV_CAP_STORAGE_IMAGE_WRITE_WITHOUT_FORMAT = 56;
    public static final int SPIRV_CAP_MULTI_VIEWPORT = 57;
    public static final int SPIRV_CAP_SUBGROUP_DISPATCH = 58;
    public static final int SPIRV_CAP_NAMED_BARRIER = 59;
    public static final int SPIRV_CAP_PIPE_STORAGE = 60;
    public static final int SPIRV_CAP_GROUP_NON_UNIFORM = 61;
    public static final int SPIRV_CAP_GROUP_NON_UNIFORM_VOTE = 62;
    public static final int SPIRV_CAP_GROUP_NON_UNIFORM_ARITHMETIC = 63;
    public static final int SPIRV_CAP_GROUP_NON_UNIFORM_BALLOT = 64;
    public static final int SPIRV_CAP_GROUP_NON_UNIFORM_SHUFFLE = 65;
    public static final int SPIRV_CAP_GROUP_NON_UNIFORM_SHUFFLE_RELATIVE = 66;
    public static final int SPIRV_CAP_GROUP_NON_UNIFORM_CLUSTERED = 67;
    public static final int SPIRV_CAP_GROUP_NON_UNIFORM_QUAD = 68;
    public static final int SPIRV_CAP_SHADER_LAYER = 69;
    public static final int SPIRV_CAP_SHADER_VIEWPORT_INDEX = 70;
    public static final int SPIRV_CAP_RAY_TRACING_KHR = 4479;
    public static final int SPIRV_CAP_RAY_QUERY_KHR = 4472;
    public static final int SPIRV_CAP_MESH_SHADING_NV = 5266;
    public static final int SPIRV_CAP_MESH_SHADING_EXT = 5283;

    // Execution Models
    public static final int SPIRV_EXEC_MODEL_VERTEX = 0;
    public static final int SPIRV_EXEC_MODEL_TESSELLATION_CONTROL = 1;
    public static final int SPIRV_EXEC_MODEL_TESSELLATION_EVALUATION = 2;
    public static final int SPIRV_EXEC_MODEL_GEOMETRY = 3;
    public static final int SPIRV_EXEC_MODEL_FRAGMENT = 4;
    public static final int SPIRV_EXEC_MODEL_GL_COMPUTE = 5;
    public static final int SPIRV_EXEC_MODEL_KERNEL = 6;
    public static final int SPIRV_EXEC_MODEL_TASK_NV = 5267;
    public static final int SPIRV_EXEC_MODEL_MESH_NV = 5268;
    public static final int SPIRV_EXEC_MODEL_RAY_GENERATION_KHR = 5313;
    public static final int SPIRV_EXEC_MODEL_INTERSECTION_KHR = 5314;
    public static final int SPIRV_EXEC_MODEL_ANY_HIT_KHR = 5315;
    public static final int SPIRV_EXEC_MODEL_CLOSEST_HIT_KHR = 5316;
    public static final int SPIRV_EXEC_MODEL_MISS_KHR = 5317;
    public static final int SPIRV_EXEC_MODEL_CALLABLE_KHR = 5318;
    public static final int SPIRV_EXEC_MODEL_TASK_EXT = 5364;
    public static final int SPIRV_EXEC_MODEL_MESH_EXT = 5365;

    // ========================================================================
    // TRANSLATION TABLES (Precomputed for O(1) lookup)
    // ========================================================================

    private static final int[] GL_TO_VK_COMPARE_OP = new int[0x0210];
    private static final int[] GL_TO_VK_BLEND_FACTOR = new int[0x0320];
    private static final int[] GL_TO_VK_BLEND_OP = new int[0x8010];
    private static final int[] GL_TO_VK_TOPOLOGY = new int[0x000A];
    private static final int[] GL_TO_VK_FILTER = new int[0x2800];
    private static final int[] GL_TO_VK_MIPMAP_MODE = new int[0x2800];
    private static final int[] GL_TO_VK_ADDRESS_MODE = new int[0x8500];
    private static final int[] GL_TO_VK_FORMAT = new int[0x9000];
    private static final int[] GL_TO_VK_CULL_MODE = new int[0x0410];
    private static final int[] GL_TO_VK_FRONT_FACE = new int[0x0910];
    private static final int[] GL_TO_VK_POLYGON_MODE = new int[0x1B10];
    private static final int[] GL_TO_VK_SHADER_STAGE = new int[0x9200];
    private static final int[] GL_TYPE_SIZES = new int[0x1500];

    private static final int[] VK_TO_GL_FORMAT = new int[200];
    private static final int[] VK_TO_GL_COMPARE_OP = new int[10];

    static {
        initCompareOps();
        initBlendFactors();
        initBlendOps();
        initTopologies();
        initFilters();
        initAddressModes();
        initFormats();
        initCullModes();
        initFrontFace();
        initPolygonModes();
        initShaderStages();
        initTypeSizes();
        initReverseFormats();
    }

    private static void initCompareOps() {
        GL_TO_VK_COMPARE_OP[GL11_NEVER] = VK10_COMPARE_OP_NEVER;
        GL_TO_VK_COMPARE_OP[GL11_LESS] = VK10_COMPARE_OP_LESS;
        GL_TO_VK_COMPARE_OP[GL11_EQUAL] = VK10_COMPARE_OP_EQUAL;
        GL_TO_VK_COMPARE_OP[GL11_LEQUAL] = VK10_COMPARE_OP_LESS_OR_EQUAL;
        GL_TO_VK_COMPARE_OP[GL11_GREATER] = VK10_COMPARE_OP_GREATER;
        GL_TO_VK_COMPARE_OP[GL11_NOTEQUAL] = VK10_COMPARE_OP_NOT_EQUAL;
        GL_TO_VK_COMPARE_OP[GL11_GEQUAL] = VK10_COMPARE_OP_GREATER_OR_EQUAL;
        GL_TO_VK_COMPARE_OP[GL11_ALWAYS] = VK10_COMPARE_OP_ALWAYS;

        VK_TO_GL_COMPARE_OP[VK10_COMPARE_OP_NEVER] = GL11_NEVER;
        VK_TO_GL_COMPARE_OP[VK10_COMPARE_OP_LESS] = GL11_LESS;
        VK_TO_GL_COMPARE_OP[VK10_COMPARE_OP_EQUAL] = GL11_EQUAL;
        VK_TO_GL_COMPARE_OP[VK10_COMPARE_OP_LESS_OR_EQUAL] = GL11_LEQUAL;
        VK_TO_GL_COMPARE_OP[VK10_COMPARE_OP_GREATER] = GL11_GREATER;
        VK_TO_GL_COMPARE_OP[VK10_COMPARE_OP_NOT_EQUAL] = GL11_NOTEQUAL;
        VK_TO_GL_COMPARE_OP[VK10_COMPARE_OP_GREATER_OR_EQUAL] = GL11_GEQUAL;
        VK_TO_GL_COMPARE_OP[VK10_COMPARE_OP_ALWAYS] = GL11_ALWAYS;
    }

    private static void initBlendFactors() {
        GL_TO_VK_BLEND_FACTOR[GL11_ZERO] = VK10_BLEND_FACTOR_ZERO;
        GL_TO_VK_BLEND_FACTOR[GL11_ONE] = VK10_BLEND_FACTOR_ONE;
        GL_TO_VK_BLEND_FACTOR[GL11_SRC_COLOR] = VK10_BLEND_FACTOR_SRC_COLOR;
        GL_TO_VK_BLEND_FACTOR[GL11_ONE_MINUS_SRC_COLOR] = VK10_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
        GL_TO_VK_BLEND_FACTOR[GL11_SRC_ALPHA] = VK10_BLEND_FACTOR_SRC_ALPHA;
        GL_TO_VK_BLEND_FACTOR[GL11_ONE_MINUS_SRC_ALPHA] = VK10_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
        GL_TO_VK_BLEND_FACTOR[GL11_DST_ALPHA] = VK10_BLEND_FACTOR_DST_ALPHA;
        GL_TO_VK_BLEND_FACTOR[GL11_ONE_MINUS_DST_ALPHA] = VK10_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
        GL_TO_VK_BLEND_FACTOR[GL11_DST_COLOR] = VK10_BLEND_FACTOR_DST_COLOR;
        GL_TO_VK_BLEND_FACTOR[GL11_ONE_MINUS_DST_COLOR] = VK10_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
        GL_TO_VK_BLEND_FACTOR[GL11_SRC_ALPHA_SATURATE] = VK10_BLEND_FACTOR_SRC_ALPHA_SATURATE;
        GL_TO_VK_BLEND_FACTOR[GL14_CONSTANT_COLOR] = VK10_BLEND_FACTOR_CONSTANT_COLOR;
        GL_TO_VK_BLEND_FACTOR[GL14_ONE_MINUS_CONSTANT_COLOR] = VK10_BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR;
        GL_TO_VK_BLEND_FACTOR[GL14_CONSTANT_ALPHA] = VK10_BLEND_FACTOR_CONSTANT_ALPHA;
        GL_TO_VK_BLEND_FACTOR[GL14_ONE_MINUS_CONSTANT_ALPHA] = VK10_BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA;
    }

    private static void initBlendOps() {
        GL_TO_VK_BLEND_OP[GL14_FUNC_ADD] = VK10_BLEND_OP_ADD;
        GL_TO_VK_BLEND_OP[GL14_FUNC_SUBTRACT] = VK10_BLEND_OP_SUBTRACT;
        GL_TO_VK_BLEND_OP[GL14_FUNC_REVERSE_SUBTRACT] = VK10_BLEND_OP_REVERSE_SUBTRACT;
        GL_TO_VK_BLEND_OP[GL14_MIN] = VK10_BLEND_OP_MIN;
        GL_TO_VK_BLEND_OP[GL14_MAX] = VK10_BLEND_OP_MAX;
    }

    private static void initTopologies() {
        GL_TO_VK_TOPOLOGY[GL11_POINTS] = VK10_PRIMITIVE_TOPOLOGY_POINT_LIST;
        GL_TO_VK_TOPOLOGY[GL11_LINES] = VK10_PRIMITIVE_TOPOLOGY_LINE_LIST;
        GL_TO_VK_TOPOLOGY[GL11_LINE_LOOP] = VK10_PRIMITIVE_TOPOLOGY_LINE_STRIP; // Emulated
        GL_TO_VK_TOPOLOGY[GL11_LINE_STRIP] = VK10_PRIMITIVE_TOPOLOGY_LINE_STRIP;
        GL_TO_VK_TOPOLOGY[GL11_TRIANGLES] = VK10_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
        GL_TO_VK_TOPOLOGY[GL11_TRIANGLE_STRIP] = VK10_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
        GL_TO_VK_TOPOLOGY[GL11_TRIANGLE_FAN] = VK10_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN;
        GL_TO_VK_TOPOLOGY[GL11_QUADS] = VK10_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST; // Converted
        GL_TO_VK_TOPOLOGY[GL11_QUAD_STRIP] = VK10_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
        GL_TO_VK_TOPOLOGY[GL11_POLYGON] = VK10_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN;
    }

    private static void initFilters() {
        GL_TO_VK_FILTER[GL11_NEAREST] = VK10_FILTER_NEAREST;
        GL_TO_VK_FILTER[GL11_LINEAR] = VK10_FILTER_LINEAR;
        GL_TO_VK_FILTER[GL11_NEAREST_MIPMAP_NEAREST] = VK10_FILTER_NEAREST;
        GL_TO_VK_FILTER[GL11_LINEAR_MIPMAP_NEAREST] = VK10_FILTER_LINEAR;
        GL_TO_VK_FILTER[GL11_NEAREST_MIPMAP_LINEAR] = VK10_FILTER_NEAREST;
        GL_TO_VK_FILTER[GL11_LINEAR_MIPMAP_LINEAR] = VK10_FILTER_LINEAR;

        GL_TO_VK_MIPMAP_MODE[GL11_NEAREST] = VK10_SAMPLER_MIPMAP_MODE_NEAREST;
        GL_TO_VK_MIPMAP_MODE[GL11_LINEAR] = VK10_SAMPLER_MIPMAP_MODE_LINEAR;
        GL_TO_VK_MIPMAP_MODE[GL11_NEAREST_MIPMAP_NEAREST] = VK10_SAMPLER_MIPMAP_MODE_NEAREST;
        GL_TO_VK_MIPMAP_MODE[GL11_LINEAR_MIPMAP_NEAREST] = VK10_SAMPLER_MIPMAP_MODE_NEAREST;
        GL_TO_VK_MIPMAP_MODE[GL11_NEAREST_MIPMAP_LINEAR] = VK10_SAMPLER_MIPMAP_MODE_LINEAR;
        GL_TO_VK_MIPMAP_MODE[GL11_LINEAR_MIPMAP_LINEAR] = VK10_SAMPLER_MIPMAP_MODE_LINEAR;
    }

    private static void initAddressModes() {
        GL_TO_VK_ADDRESS_MODE[GL11_REPEAT & 0xFF] = VK10_SAMPLER_ADDRESS_MODE_REPEAT;
        GL_TO_VK_ADDRESS_MODE[GL12_CLAMP_TO_EDGE & 0xFF] = VK10_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
        GL_TO_VK_ADDRESS_MODE[GL13_CLAMP_TO_BORDER & 0xFF] = VK10_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
        GL_TO_VK_ADDRESS_MODE[GL14_MIRRORED_REPEAT & 0xFF] = VK10_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT;
        GL_TO_VK_ADDRESS_MODE[GL44_MIRROR_CLAMP_TO_EDGE & 0xFF] = VK10_SAMPLER_ADDRESS_MODE_MIRROR_CLAMP_TO_EDGE;
    }

    private static void initFormats() {
        // Basic formats
        GL_TO_VK_FORMAT[GL11_RGBA] = VK10_FORMAT_R8G8B8A8_UNORM;
        GL_TO_VK_FORMAT[GL11_RGB] = VK10_FORMAT_R8G8B8_UNORM;
        GL_TO_VK_FORMAT[GL11_DEPTH_COMPONENT] = VK10_FORMAT_D32_SFLOAT;

        // Sized formats (using hash function for lookup)
        // These are set directly in the method since they're sparse
    }

    private static void initCullModes() {
        GL_TO_VK_CULL_MODE[GL11_FRONT] = VK10_CULL_MODE_FRONT_BIT;
        GL_TO_VK_CULL_MODE[GL11_BACK] = VK10_CULL_MODE_BACK_BIT;
        GL_TO_VK_CULL_MODE[GL11_FRONT_AND_BACK] = VK10_CULL_MODE_FRONT_AND_BACK;
    }

    private static void initFrontFace() {
        GL_TO_VK_FRONT_FACE[GL11_CW] = VK10_FRONT_FACE_CLOCKWISE;
        GL_TO_VK_FRONT_FACE[GL11_CCW] = VK10_FRONT_FACE_COUNTER_CLOCKWISE;
    }

    private static void initPolygonModes() {
        GL_TO_VK_POLYGON_MODE[GL11_POINT] = VK10_POLYGON_MODE_POINT;
        GL_TO_VK_POLYGON_MODE[GL11_LINE] = VK10_POLYGON_MODE_LINE;
        GL_TO_VK_POLYGON_MODE[GL11_FILL] = VK10_POLYGON_MODE_FILL;
    }

    private static void initShaderStages() {
        GL_TO_VK_SHADER_STAGE[GL20_VERTEX_SHADER & 0x1FF] = VK10_SHADER_STAGE_VERTEX_BIT;
        GL_TO_VK_SHADER_STAGE[GL20_FRAGMENT_SHADER & 0x1FF] = VK10_SHADER_STAGE_FRAGMENT_BIT;
        GL_TO_VK_SHADER_STAGE[GL32_GEOMETRY_SHADER & 0x1FF] = VK10_SHADER_STAGE_GEOMETRY_BIT;
        GL_TO_VK_SHADER_STAGE[GL40_TESS_CONTROL_SHADER & 0x1FF] = VK10_SHADER_STAGE_TESSELLATION_CONTROL_BIT;
        GL_TO_VK_SHADER_STAGE[GL40_TESS_EVALUATION_SHADER & 0x1FF] = VK10_SHADER_STAGE_TESSELLATION_EVALUATION_BIT;
        GL_TO_VK_SHADER_STAGE[GL43_COMPUTE_SHADER & 0x1FF] = VK10_SHADER_STAGE_COMPUTE_BIT;
    }

    private static void initTypeSizes() {
        GL_TYPE_SIZES[GL11_BYTE] = 1;
        GL_TYPE_SIZES[GL11_UNSIGNED_BYTE] = 1;
        GL_TYPE_SIZES[GL11_SHORT] = 2;
        GL_TYPE_SIZES[GL11_UNSIGNED_SHORT] = 2;
        GL_TYPE_SIZES[GL11_INT] = 4;
        GL_TYPE_SIZES[GL11_UNSIGNED_INT] = 4;
        GL_TYPE_SIZES[GL11_FLOAT] = 4;
        GL_TYPE_SIZES[GL11_DOUBLE] = 8;
    }

    private static void initReverseFormats() {
        VK_TO_GL_FORMAT[VK10_FORMAT_R8G8B8A8_UNORM] = GL11_RGBA;
        VK_TO_GL_FORMAT[VK10_FORMAT_R8G8B8_UNORM] = GL11_RGB;
        VK_TO_GL_FORMAT[VK10_FORMAT_D32_SFLOAT] = GL30_DEPTH_COMPONENT32F;
        VK_TO_GL_FORMAT[VK10_FORMAT_D24_UNORM_S8_UINT] = GL30_DEPTH24_STENCIL8;
        VK_TO_GL_FORMAT[VK10_FORMAT_D32_SFLOAT_S8_UINT] = GL30_DEPTH32F_STENCIL8;
        VK_TO_GL_FORMAT[VK10_FORMAT_R16G16B16A16_SFLOAT] = GL30_RGBA16F;
        VK_TO_GL_FORMAT[VK10_FORMAT_R32G32B32A32_SFLOAT] = GL30_RGBA32F;
    }

    // ========================================================================
    // TRANSLATION METHODS
    // ========================================================================

    public static int toVkCompareOp(int glFunc) {
        if (glFunc >= GL11_NEVER && glFunc <= GL11_ALWAYS) {
            return GL_TO_VK_COMPARE_OP[glFunc];
        }
        return VK10_COMPARE_OP_ALWAYS;
    }

    public static int toGlCompareFunc(int vkOp) {
        if (vkOp >= 0 && vkOp < VK_TO_GL_COMPARE_OP.length) {
            return VK_TO_GL_COMPARE_OP[vkOp];
        }
        return GL11_ALWAYS;
    }

    public static int toVkBlendFactor(int glFactor) {
        if (glFactor < GL_TO_VK_BLEND_FACTOR.length) {
            return GL_TO_VK_BLEND_FACTOR[glFactor];
        }
        return VK10_BLEND_FACTOR_ONE;
    }

    public static int toVkBlendOp(int glOp) {
        if (glOp < GL_TO_VK_BLEND_OP.length) {
            return GL_TO_VK_BLEND_OP[glOp];
        }
        return VK10_BLEND_OP_ADD;
    }

    public static int toVkTopology(int glMode) {
        if (glMode < GL_TO_VK_TOPOLOGY.length) {
            return GL_TO_VK_TOPOLOGY[glMode];
        }
        return VK10_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    }

    public static int toVkFilter(int glFilter) {
        if (glFilter < GL_TO_VK_FILTER.length) {
            return GL_TO_VK_FILTER[glFilter];
        }
        return VK10_FILTER_LINEAR;
    }

    public static int toVkMipmapMode(int glFilter) {
        if (glFilter < GL_TO_VK_MIPMAP_MODE.length) {
            return GL_TO_VK_MIPMAP_MODE[glFilter];
        }
        return VK10_SAMPLER_MIPMAP_MODE_LINEAR;
    }

    public static int toVkAddressMode(int glWrap) {
        return switch (glWrap) {
            case GL11_REPEAT -> VK10_SAMPLER_ADDRESS_MODE_REPEAT;
            case GL12_CLAMP_TO_EDGE -> VK10_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
            case GL13_CLAMP_TO_BORDER -> VK10_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
            case GL14_MIRRORED_REPEAT -> VK10_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT;
            case GL44_MIRROR_CLAMP_TO_EDGE -> VK10_SAMPLER_ADDRESS_MODE_MIRROR_CLAMP_TO_EDGE;
            default -> VK10_SAMPLER_ADDRESS_MODE_REPEAT;
        };
    }

    public static int toVkFormat(int glInternalFormat) {
        return switch (glInternalFormat) {
            // Basic formats
            case GL11_RGBA -> VK10_FORMAT_R8G8B8A8_UNORM;
            case GL11_RGB -> VK10_FORMAT_R8G8B8_UNORM;
            case GL12_BGR -> VK10_FORMAT_B8G8R8_UNORM;
            case GL12_BGRA -> VK10_FORMAT_B8G8R8A8_UNORM;
            
            // Sized RGBA
            case 0x8058 /* GL_RGBA8 */ -> VK10_FORMAT_R8G8B8A8_UNORM;
            case GL30_RGBA16F -> VK10_FORMAT_R16G16B16A16_SFLOAT;
            case GL30_RGBA32F -> VK10_FORMAT_R32G32B32A32_SFLOAT;
            
            // Sized RGB
            case 0x8051 /* GL_RGB8 */ -> VK10_FORMAT_R8G8B8_UNORM;
            case GL30_RGB16F -> VK10_FORMAT_R16G16B16_SFLOAT;
            case GL30_RGB32F -> VK10_FORMAT_R32G32B32_SFLOAT;
            
            // Single channel
            case GL30_R8 -> VK10_FORMAT_R8_UNORM;
            case GL30_R16 -> VK10_FORMAT_R16_UNORM;
            case GL30_R16F -> VK10_FORMAT_R16_SFLOAT;
            case GL30_R32F -> VK10_FORMAT_R32_SFLOAT;
            
            // Two channel
            case GL30_RG8 -> VK10_FORMAT_R8G8_UNORM;
            case GL30_RG16 -> VK10_FORMAT_R16G16_UNORM;
            case GL30_RG16F -> VK10_FORMAT_R16G16_SFLOAT;
            case GL30_RG32F -> VK10_FORMAT_R32G32_SFLOAT;
            
            // Depth
            case GL11_DEPTH_COMPONENT -> VK10_FORMAT_D32_SFLOAT;
            case GL14_DEPTH_COMPONENT16 -> VK10_FORMAT_D16_UNORM;
            case GL14_DEPTH_COMPONENT24 -> VK10_FORMAT_D24_UNORM_S8_UINT;
            case GL14_DEPTH_COMPONENT32 -> VK10_FORMAT_D32_SFLOAT;
            case GL30_DEPTH_COMPONENT32F -> VK10_FORMAT_D32_SFLOAT;
            case GL30_DEPTH24_STENCIL8 -> VK10_FORMAT_D24_UNORM_S8_UINT;
            case GL30_DEPTH32F_STENCIL8 -> VK10_FORMAT_D32_SFLOAT_S8_UINT;
            
            // SRGB
            case GL21_SRGB8 -> VK10_FORMAT_R8G8B8_SRGB;
            case GL21_SRGB8_ALPHA8 -> VK10_FORMAT_R8G8B8A8_SRGB;
            
            // Compressed BC
            case GL42_COMPRESSED_RGBA_BPTC_UNORM -> VK10.VK_FORMAT_BC7_UNORM_BLOCK;
            case GL42_COMPRESSED_SRGB_ALPHA_BPTC_UNORM -> VK10.VK_FORMAT_BC7_SRGB_BLOCK;
            case GL42_COMPRESSED_RGB_BPTC_SIGNED_FLOAT -> VK10.VK_FORMAT_BC6H_SFLOAT_BLOCK;
            case GL42_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT -> VK10.VK_FORMAT_BC6H_UFLOAT_BLOCK;
            
            default -> VK10_FORMAT_R8G8B8A8_UNORM;
        };
    }

    public static int toGlFormat(int vkFormat) {
        if (vkFormat >= 0 && vkFormat < VK_TO_GL_FORMAT.length) {
            int result = VK_TO_GL_FORMAT[vkFormat];
            return result != 0 ? result : GL11_RGBA;
        }
        return GL11_RGBA;
    }

    public static int toVkCullMode(int glFace) {
        return switch (glFace) {
            case GL11_FRONT -> VK10_CULL_MODE_FRONT_BIT;
            case GL11_BACK -> VK10_CULL_MODE_BACK_BIT;
            case GL11_FRONT_AND_BACK -> VK10_CULL_MODE_FRONT_AND_BACK;
            default -> VK10_CULL_MODE_NONE;
        };
    }

    public static int toVkFrontFace(int glFrontFace) {
        return switch (glFrontFace) {
            case GL11_CW -> VK10_FRONT_FACE_CLOCKWISE;
            case GL11_CCW -> VK10_FRONT_FACE_COUNTER_CLOCKWISE;
            default -> VK10_FRONT_FACE_COUNTER_CLOCKWISE;
        };
    }

    public static int toVkPolygonMode(int glMode) {
        return switch (glMode) {
            case GL11_POINT -> VK10_POLYGON_MODE_POINT;
            case GL11_LINE -> VK10_POLYGON_MODE_LINE;
            case GL11_FILL -> VK10_POLYGON_MODE_FILL;
            default -> VK10_POLYGON_MODE_FILL;
        };
    }

    public static int toVkShaderStage(int glShaderType) {
        return switch (glShaderType) {
            case GL20_VERTEX_SHADER -> VK10_SHADER_STAGE_VERTEX_BIT;
            case GL20_FRAGMENT_SHADER -> VK10_SHADER_STAGE_FRAGMENT_BIT;
            case GL32_GEOMETRY_SHADER -> VK10_SHADER_STAGE_GEOMETRY_BIT;
            case GL40_TESS_CONTROL_SHADER -> VK10_SHADER_STAGE_TESSELLATION_CONTROL_BIT;
            case GL40_TESS_EVALUATION_SHADER -> VK10_SHADER_STAGE_TESSELLATION_EVALUATION_BIT;
            case GL43_COMPUTE_SHADER -> VK10_SHADER_STAGE_COMPUTE_BIT;
            default -> VK10_SHADER_STAGE_VERTEX_BIT;
        };
    }

    public static int toSpirvExecutionModel(int glShaderType) {
        return switch (glShaderType) {
            case GL20_VERTEX_SHADER -> SPIRV_EXEC_MODEL_VERTEX;
            case GL20_FRAGMENT_SHADER -> SPIRV_EXEC_MODEL_FRAGMENT;
            case GL32_GEOMETRY_SHADER -> SPIRV_EXEC_MODEL_GEOMETRY;
            case GL40_TESS_CONTROL_SHADER -> SPIRV_EXEC_MODEL_TESSELLATION_CONTROL;
            case GL40_TESS_EVALUATION_SHADER -> SPIRV_EXEC_MODEL_TESSELLATION_EVALUATION;
            case GL43_COMPUTE_SHADER -> SPIRV_EXEC_MODEL_GL_COMPUTE;
            default -> SPIRV_EXEC_MODEL_VERTEX;
        };
    }

    public static int getGlTypeSize(int glType) {
        if (glType < GL_TYPE_SIZES.length) {
            return GL_TYPE_SIZES[glType];
        }
        return 4;
    }

    public static int toVkBufferUsage(int glTarget, int glUsage) {
        int usage = VK10_BUFFER_USAGE_TRANSFER_DST_BIT;

        // Target-based usage
        usage |= switch (glTarget) {
            case GL15_ARRAY_BUFFER -> VK10_BUFFER_USAGE_VERTEX_BUFFER_BIT;
            case GL15_ELEMENT_ARRAY_BUFFER -> VK10_BUFFER_USAGE_INDEX_BUFFER_BIT;
            case GL31_UNIFORM_BUFFER -> VK10_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
            case GL43_SHADER_STORAGE_BUFFER -> VK10_BUFFER_USAGE_STORAGE_BUFFER_BIT;
            case GL40_DRAW_INDIRECT_BUFFER -> VK10_BUFFER_USAGE_INDIRECT_BUFFER_BIT;
            case GL30_TRANSFORM_FEEDBACK_BUFFER -> VK10_BUFFER_USAGE_TRANSFORM_FEEDBACK_BUFFER_BIT_EXT;
            default -> VK10_BUFFER_USAGE_VERTEX_BUFFER_BIT;
        };

        return usage;
    }

    // VK 1.2 extension constant
    private static final int VK10_BUFFER_USAGE_TRANSFORM_FEEDBACK_BUFFER_BIT_EXT = 0x00000800;

    // ========================================================================
    // TOPOLOGY CONVERSION UTILITIES
    // ========================================================================

    public static boolean requiresQuadConversion(int glMode) {
        return glMode == GL11_QUADS || glMode == GL11_QUAD_STRIP || glMode == GL11_POLYGON;
    }

    public static boolean requiresLineLoopConversion(int glMode) {
        return glMode == GL11_LINE_LOOP;
    }

    public static int calculateQuadIndexCount(int vertexCount) {
        // 4 vertices per quad -> 6 indices (2 triangles)
        return (vertexCount / 4) * 6;
    }

    public static int calculateLineLoopIndexCount(int vertexCount) {
        // N vertices -> N lines (including closing line)
        return vertexCount * 2;
    }

    // ========================================================================
    // CAPABILITY QUERIES
    // ========================================================================

    private static final Map<Capability, GLVersion> GL_CAPABILITY_MIN_VERSION = new HashMap<>();
    private static final Map<Capability, VKVersion> VK_CAPABILITY_MIN_VERSION = new HashMap<>();
    private static final Map<Capability, SPIRVVersion> SPIRV_CAPABILITY_MIN_VERSION = new HashMap<>();

    static {
        // GL capability minimums
        GL_CAPABILITY_MIN_VERSION.put(Capability.COMPUTE_SHADERS, GLVersion.GL_4_3);
        GL_CAPABILITY_MIN_VERSION.put(Capability.GEOMETRY_SHADERS, GLVersion.GL_3_2);
        GL_CAPABILITY_MIN_VERSION.put(Capability.TESSELLATION_SHADERS, GLVersion.GL_4_0);
        GL_CAPABILITY_MIN_VERSION.put(Capability.MULTI_DRAW_INDIRECT, GLVersion.GL_4_3);
        GL_CAPABILITY_MIN_VERSION.put(Capability.BUFFER_STORAGE, GLVersion.GL_4_4);
        GL_CAPABILITY_MIN_VERSION.put(Capability.PERSISTENT_MAPPING, GLVersion.GL_4_4);
        GL_CAPABILITY_MIN_VERSION.put(Capability.BINDLESS_TEXTURES, GLVersion.GL_4_4); // Via extension
        GL_CAPABILITY_MIN_VERSION.put(Capability.GL_SPIRV, GLVersion.GL_4_6);
        GL_CAPABILITY_MIN_VERSION.put(Capability.ANISOTROPIC_FILTERING, GLVersion.GL_4_6);
        GL_CAPABILITY_MIN_VERSION.put(Capability.SHADER_INT64, GLVersion.GL_4_0); // Via extension
        GL_CAPABILITY_MIN_VERSION.put(Capability.SHADER_FLOAT64, GLVersion.GL_4_0);
        GL_CAPABILITY_MIN_VERSION.put(Capability.TRANSFORM_FEEDBACK, GLVersion.GL_3_0);

        // VK capability minimums
        VK_CAPABILITY_MIN_VERSION.put(Capability.COMPUTE_SHADERS, VKVersion.VK_1_0);
        VK_CAPABILITY_MIN_VERSION.put(Capability.GEOMETRY_SHADERS, VKVersion.VK_1_0);
        VK_CAPABILITY_MIN_VERSION.put(Capability.TESSELLATION_SHADERS, VKVersion.VK_1_0);
        VK_CAPABILITY_MIN_VERSION.put(Capability.MULTI_DRAW_INDIRECT, VKVersion.VK_1_0);
        VK_CAPABILITY_MIN_VERSION.put(Capability.BUFFER_DEVICE_ADDRESS, VKVersion.VK_1_2);
        VK_CAPABILITY_MIN_VERSION.put(Capability.DESCRIPTOR_INDEXING, VKVersion.VK_1_2);
        VK_CAPABILITY_MIN_VERSION.put(Capability.TIMELINE_SEMAPHORES, VKVersion.VK_1_2);
        VK_CAPABILITY_MIN_VERSION.put(Capability.DYNAMIC_RENDERING, VKVersion.VK_1_3);
        VK_CAPABILITY_MIN_VERSION.put(Capability.SYNCHRONIZATION2, VKVersion.VK_1_3);
        VK_CAPABILITY_MIN_VERSION.put(Capability.SHADER_SUBGROUP_OPERATIONS, VKVersion.VK_1_1);

        // SPIR-V capability minimums
        SPIRV_CAPABILITY_MIN_VERSION.put(Capability.SPIRV_1_4, SPIRVVersion.SPIRV_1_4);
        SPIRV_CAPABILITY_MIN_VERSION.put(Capability.SPIRV_1_5, SPIRVVersion.SPIRV_1_5);
        SPIRV_CAPABILITY_MIN_VERSION.put(Capability.SPIRV_1_6, SPIRVVersion.SPIRV_1_6);
        SPIRV_CAPABILITY_MIN_VERSION.put(Capability.RAY_TRACING, SPIRVVersion.SPIRV_1_4);
        SPIRV_CAPABILITY_MIN_VERSION.put(Capability.RAY_QUERY, SPIRVVersion.SPIRV_1_4);
        SPIRV_CAPABILITY_MIN_VERSION.put(Capability.MESH_SHADERS, SPIRVVersion.SPIRV_1_4);
        SPIRV_CAPABILITY_MIN_VERSION.put(Capability.TASK_SHADERS, SPIRVVersion.SPIRV_1_4);
        SPIRV_CAPABILITY_MIN_VERSION.put(Capability.SHADER_INT64, SPIRVVersion.SPIRV_1_0);
        SPIRV_CAPABILITY_MIN_VERSION.put(Capability.SHADER_FLOAT64, SPIRVVersion.SPIRV_1_0);
        SPIRV_CAPABILITY_MIN_VERSION.put(Capability.SHADER_INT16, SPIRVVersion.SPIRV_1_0);
        SPIRV_CAPABILITY_MIN_VERSION.put(Capability.SHADER_SUBGROUP_OPERATIONS, SPIRVVersion.SPIRV_1_3);
    }

    /**
     * Checks if a capability is available for a given GL version.
     */
    public static boolean isCapabilityAvailable(Capability cap, GLVersion version) {
        GLVersion minVersion = GL_CAPABILITY_MIN_VERSION.get(cap);
        return minVersion != null && version.isAtLeast(minVersion);
    }

    /**
     * Checks if a capability is available for a given Vulkan version.
     */
    public static boolean isCapabilityAvailable(Capability cap, VKVersion version) {
        VKVersion minVersion = VK_CAPABILITY_MIN_VERSION.get(cap);
        return minVersion != null && version.isAtLeast(minVersion);
    }

    /**
     * Checks if a capability is available for a given SPIR-V version.
     */
    public static boolean isCapabilityAvailable(Capability cap, SPIRVVersion version) {
        SPIRVVersion minVersion = SPIRV_CAPABILITY_MIN_VERSION.get(cap);
        return minVersion != null && version.isAtLeast(minVersion);
    }

    /**
     * Gets all capabilities available for a GL version.
     */
    public static EnumSet<Capability> getAvailableCapabilities(GLVersion version) {
        EnumSet<Capability> result = EnumSet.noneOf(Capability.class);
        for (Map.Entry<Capability, GLVersion> entry : GL_CAPABILITY_MIN_VERSION.entrySet()) {
            if (version.isAtLeast(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Gets all capabilities available for a Vulkan version.
     */
    public static EnumSet<Capability> getAvailableCapabilities(VKVersion version) {
        EnumSet<Capability> result = EnumSet.noneOf(Capability.class);
        for (Map.Entry<Capability, VKVersion> entry : VK_CAPABILITY_MIN_VERSION.entrySet()) {
            if (version.isAtLeast(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    // ========================================================================
    // GLSL VERSION TRANSLATION
    // ========================================================================

    /**
     * Translates legacy GLSL keywords to modern equivalents.
     */
    public static String translateGlslKeyword(String keyword, GLSLVersion from, GLSLVersion to) {
        if (from.hasInOut == to.hasInOut) {
            return keyword;
        }

        // Legacy to modern
        if (!from.hasInOut && to.hasInOut) {
            return switch (keyword) {
                case "attribute" -> "in";
                case "varying" -> "out"; // In vertex shader
                case "gl_FragColor" -> "fragColor"; // Requires output declaration
                case "gl_FragData" -> "fragData";
                case "texture2D" -> "texture";
                case "texture3D" -> "texture";
                case "textureCube" -> "texture";
                case "shadow2D" -> "texture";
                case "shadow2DProj" -> "textureProj";
                default -> keyword;
            };
        }

        // Modern to legacy (less common but supported)
        if (from.hasInOut && !to.hasInOut) {
            return switch (keyword) {
                case "in" -> "attribute"; // Context dependent
                case "out" -> "varying";
                case "texture" -> "texture2D"; // Context dependent
                default -> keyword;
            };
        }

        return keyword;
    }

    /**
     * Gets the SPIR-V capability required for a GLSL feature.
     */
    public static int getSpirvCapabilityForGlslFeature(String feature) {
        return switch (feature.toLowerCase()) {
            case "double", "dvec2", "dvec3", "dvec4", "dmat2", "dmat3", "dmat4" -> SPIRV_CAP_FLOAT64;
            case "int64_t", "uint64_t", "i64vec2", "u64vec2" -> SPIRV_CAP_INT64;
            case "int16_t", "uint16_t", "i16vec2", "u16vec2" -> SPIRV_CAP_INT16;
            case "gl_clipdistance" -> SPIRV_CAP_CLIP_DISTANCE;
            case "gl_culldistance" -> SPIRV_CAP_CULL_DISTANCE;
            case "gl_layer" -> SPIRV_CAP_SHADER_LAYER;
            case "gl_viewportindex" -> SPIRV_CAP_SHADER_VIEWPORT_INDEX;
            case "subgroupsize", "gl_subgroupsize" -> SPIRV_CAP_GROUP_NON_UNIFORM;
            case "subgroupinvocationid" -> SPIRV_CAP_GROUP_NON_UNIFORM;
            case "subgroupballot" -> SPIRV_CAP_GROUP_NON_UNIFORM_BALLOT;
            case "subgroupshuffle" -> SPIRV_CAP_GROUP_NON_UNIFORM_SHUFFLE;
            case "sparsetexture" -> SPIRV_CAP_SPARSE_RESIDENCY;
            case "imagecubearray" -> SPIRV_CAP_IMAGE_CUBE_ARRAY;
            case "imagemsarray" -> SPIRV_CAP_IMAGE_MS_ARRAY;
            case "derivative" -> SPIRV_CAP_DERIVATIVE_CONTROL;
            default -> SPIRV_CAP_SHADER;
        };
    }

    // ========================================================================
    // FORMAT COMPATIBILITY TABLES
    // ========================================================================

    /**
     * Vertex attribute format descriptor.
     */
    public record VertexFormat(
        int glType,
        int componentCount,
        boolean normalized,
        int vkFormat,
        int byteSize
    ) {
        public static VertexFormat of(int glType, int count, boolean normalized) {
            int vkFormat = getVkVertexFormat(glType, count, normalized);
            int size = getGlTypeSize(glType) * count;
            return new VertexFormat(glType, count, normalized, vkFormat, size);
        }
    }

    /**
     * Gets the Vulkan format for a vertex attribute.
     */
    public static int getVkVertexFormat(int glType, int componentCount, boolean normalized) {
        return switch (glType) {
            case GL11_FLOAT -> switch (componentCount) {
                case 1 -> VK10_FORMAT_R32_SFLOAT;
                case 2 -> VK10_FORMAT_R32G32_SFLOAT;
                case 3 -> VK10_FORMAT_R32G32B32_SFLOAT;
                case 4 -> VK10_FORMAT_R32G32B32A32_SFLOAT;
                default -> VK10_FORMAT_R32G32B32A32_SFLOAT;
            };
            case GL11_DOUBLE -> switch (componentCount) {
                case 1 -> VK10_FORMAT_R64_SFLOAT;
                case 2 -> VK10.VK_FORMAT_R64G64_SFLOAT;
                case 3 -> VK10.VK_FORMAT_R64G64B64_SFLOAT;
                case 4 -> VK10.VK_FORMAT_R64G64B64A64_SFLOAT;
                default -> VK10.VK_FORMAT_R64G64B64A64_SFLOAT;
            };
            case GL11_INT -> switch (componentCount) {
                case 1 -> VK10_FORMAT_R32_SINT;
                case 2 -> VK10.VK_FORMAT_R32G32_SINT;
                case 3 -> VK10.VK_FORMAT_R32G32B32_SINT;
                case 4 -> VK10.VK_FORMAT_R32G32B32A32_SINT;
                default -> VK10.VK_FORMAT_R32G32B32A32_SINT;
            };
            case GL11_UNSIGNED_INT -> switch (componentCount) {
                case 1 -> VK10_FORMAT_R32_UINT;
                case 2 -> VK10.VK_FORMAT_R32G32_UINT;
                case 3 -> VK10.VK_FORMAT_R32G32B32_UINT;
                case 4 -> VK10.VK_FORMAT_R32G32B32A32_UINT;
                default -> VK10.VK_FORMAT_R32G32B32A32_UINT;
            };
            case GL11_SHORT -> normalized ? switch (componentCount) {
                case 1 -> VK10.VK_FORMAT_R16_SNORM;
                case 2 -> VK10.VK_FORMAT_R16G16_SNORM;
                case 3 -> VK10.VK_FORMAT_R16G16B16_SNORM;
                case 4 -> VK10.VK_FORMAT_R16G16B16A16_SNORM;
                default -> VK10.VK_FORMAT_R16G16B16A16_SNORM;
            } : switch (componentCount) {
                case 1 -> VK10.VK_FORMAT_R16_SINT;
                case 2 -> VK10.VK_FORMAT_R16G16_SINT;
                case 3 -> VK10.VK_FORMAT_R16G16B16_SINT;
                case 4 -> VK10.VK_FORMAT_R16G16B16A16_SINT;
                default -> VK10.VK_FORMAT_R16G16B16A16_SINT;
            };
            case GL11_UNSIGNED_SHORT -> normalized ? switch (componentCount) {
                case 1 -> VK10.VK_FORMAT_R16_UNORM;
                case 2 -> VK10.VK_FORMAT_R16G16_UNORM;
                case 3 -> VK10.VK_FORMAT_R16G16B16_UNORM;
                case 4 -> VK10.VK_FORMAT_R16G16B16A16_UNORM;
                default -> VK10.VK_FORMAT_R16G16B16A16_UNORM;
            } : switch (componentCount) {
                case 1 -> VK10.VK_FORMAT_R16_UINT;
                case 2 -> VK10.VK_FORMAT_R16G16_UINT;
                case 3 -> VK10.VK_FORMAT_R16G16B16_UINT;
                case 4 -> VK10.VK_FORMAT_R16G16B16A16_UINT;
                default -> VK10.VK_FORMAT_R16G16B16A16_UINT;
            };
            case GL11_BYTE -> normalized ? switch (componentCount) {
                case 1 -> VK10.VK_FORMAT_R8_SNORM;
                case 2 -> VK10.VK_FORMAT_R8G8_SNORM;
                case 3 -> VK10.VK_FORMAT_R8G8B8_SNORM;
                case 4 -> VK10.VK_FORMAT_R8G8B8A8_SNORM;
                default -> VK10.VK_FORMAT_R8G8B8A8_SNORM;
            } : switch (componentCount) {
                case 1 -> VK10.VK_FORMAT_R8_SINT;
                case 2 -> VK10.VK_FORMAT_R8G8_SINT;
                case 3 -> VK10.VK_FORMAT_R8G8B8_SINT;
                case 4 -> VK10.VK_FORMAT_R8G8B8A8_SINT;
                default -> VK10.VK_FORMAT_R8G8B8A8_SINT;
            };
            case GL11_UNSIGNED_BYTE -> normalized ? switch (componentCount) {
                case 1 -> VK10_FORMAT_R8_UNORM;
                case 2 -> VK10_FORMAT_R8G8_UNORM;
                case 3 -> VK10_FORMAT_R8G8B8_UNORM;
                case 4 -> VK10_FORMAT_R8G8B8A8_UNORM;
                default -> VK10_FORMAT_R8G8B8A8_UNORM;
            } : switch (componentCount) {
                case 1 -> VK10_FORMAT_R8_UINT;
                case 2 -> VK10.VK_FORMAT_R8G8_UINT;
                case 3 -> VK10.VK_FORMAT_R8G8B8_UINT;
                case 4 -> VK10_FORMAT_R8G8B8A8_UINT;
                default -> VK10_FORMAT_R8G8B8A8_UINT;
            };
            default -> VK10_FORMAT_R32G32B32A32_SFLOAT;
        };
    }

    // ========================================================================
    // EXTENSION TRACKING
    // ========================================================================

    /**
     * OpenGL extension identifiers.
     */
    public enum GLExtension {
        ARB_DIRECT_STATE_ACCESS("GL_ARB_direct_state_access", GLVersion.GL_4_5),
        ARB_BINDLESS_TEXTURE("GL_ARB_bindless_texture", GLVersion.GL_4_4),
        ARB_BUFFER_STORAGE("GL_ARB_buffer_storage", GLVersion.GL_4_4),
        ARB_COMPUTE_SHADER("GL_ARB_compute_shader", GLVersion.GL_4_3),
        ARB_SHADER_STORAGE_BUFFER_OBJECT("GL_ARB_shader_storage_buffer_object", GLVersion.GL_4_3),
        ARB_SPARSE_TEXTURE("GL_ARB_sparse_texture", GLVersion.GL_4_4),
        ARB_TEXTURE_FILTER_ANISOTROPIC("GL_ARB_texture_filter_anisotropic", GLVersion.GL_4_6),
        ARB_MULTI_DRAW_INDIRECT("GL_ARB_multi_draw_indirect", GLVersion.GL_4_3),
        ARB_SHADER_IMAGE_LOAD_STORE("GL_ARB_shader_image_load_store", GLVersion.GL_4_2),
        ARB_GPU_SHADER_INT64("GL_ARB_gpu_shader_int64", GLVersion.GL_4_0),
        ARB_SHADING_LANGUAGE_INCLUDE("GL_ARB_shading_language_include", GLVersion.GL_3_2),
        ARB_GL_SPIRV("GL_ARB_gl_spirv", GLVersion.GL_4_6),
        ARB_SPIRV_EXTENSIONS("GL_ARB_spirv_extensions", GLVersion.GL_4_6),
        EXT_TEXTURE_FILTER_ANISOTROPIC("GL_EXT_texture_filter_anisotropic", GLVersion.GL_1_2),
        NV_MESH_SHADER("GL_NV_mesh_shader", GLVersion.GL_4_5),
        NV_RAY_TRACING("GL_NV_ray_tracing", GLVersion.GL_4_5),
        NV_COMMAND_LIST("GL_NV_command_list", GLVersion.GL_4_5),
        NV_BINDLESS_TEXTURE("GL_NV_bindless_texture", GLVersion.GL_4_0),
        NV_SHADER_BUFFER_LOAD("GL_NV_shader_buffer_load", GLVersion.GL_4_0);

        public final String name;
        public final GLVersion coreVersion;

        GLExtension(String name, GLVersion coreVersion) {
            this.name = name;
            this.coreVersion = coreVersion;
        }
    }

    /**
     * Vulkan extension identifiers.
     */
    public enum VKExtension {
        KHR_SWAPCHAIN("VK_KHR_swapchain", VKVersion.VK_1_0),
        KHR_SURFACE("VK_KHR_surface", VKVersion.VK_1_0),
        KHR_DYNAMIC_RENDERING("VK_KHR_dynamic_rendering", VKVersion.VK_1_3),
        KHR_SYNCHRONIZATION2("VK_KHR_synchronization2", VKVersion.VK_1_3),
        KHR_BUFFER_DEVICE_ADDRESS("VK_KHR_buffer_device_address", VKVersion.VK_1_2),
        KHR_RAY_TRACING_PIPELINE("VK_KHR_ray_tracing_pipeline", VKVersion.VK_1_2),
        KHR_ACCELERATION_STRUCTURE("VK_KHR_acceleration_structure", VKVersion.VK_1_2),
        KHR_DEFERRED_HOST_OPERATIONS("VK_KHR_deferred_host_operations", VKVersion.VK_1_2),
        KHR_SPIRV_1_4("VK_KHR_spirv_1_4", VKVersion.VK_1_2),
        KHR_SHADER_FLOAT_CONTROLS("VK_KHR_shader_float_controls", VKVersion.VK_1_2),
        KHR_MAINTENANCE4("VK_KHR_maintenance4", VKVersion.VK_1_3),
        KHR_MAINTENANCE5("VK_KHR_maintenance5", VKVersion.VK_1_3),
        KHR_MAINTENANCE6("VK_KHR_maintenance6", VKVersion.VK_1_3),
        EXT_DESCRIPTOR_INDEXING("VK_EXT_descriptor_indexing", VKVersion.VK_1_2),
        EXT_MESH_SHADER("VK_EXT_mesh_shader", VKVersion.VK_1_2),
        EXT_SHADER_ATOMIC_FLOAT("VK_EXT_shader_atomic_float", VKVersion.VK_1_2),
        EXT_MEMORY_BUDGET("VK_EXT_memory_budget", VKVersion.VK_1_1),
        EXT_CONDITIONAL_RENDERING("VK_EXT_conditional_rendering", VKVersion.VK_1_0),
        EXT_EXTENDED_DYNAMIC_STATE("VK_EXT_extended_dynamic_state", VKVersion.VK_1_3),
        EXT_EXTENDED_DYNAMIC_STATE_2("VK_EXT_extended_dynamic_state2", VKVersion.VK_1_3),
        EXT_EXTENDED_DYNAMIC_STATE_3("VK_EXT_extended_dynamic_state3", VKVersion.VK_1_3),
        NV_RAY_TRACING("VK_NV_ray_tracing", VKVersion.VK_1_1),
        NV_MESH_SHADER("VK_NV_mesh_shader", VKVersion.VK_1_1);

        public final String name;
        public final VKVersion coreVersion;

        VKExtension(String name, VKVersion coreVersion) {
            this.name = name;
            this.coreVersion = coreVersion;
        }
    }

    // ========================================================================
    // DYNAMIC STATE MAPPING (VK 1.3 Extended Dynamic State)
    // ========================================================================

    /**
     * Dynamic state capability enumeration.
     */
    public enum DynamicState {
        VIEWPORT(VK10.VK_DYNAMIC_STATE_VIEWPORT),
        SCISSOR(VK10.VK_DYNAMIC_STATE_SCISSOR),
        LINE_WIDTH(VK10.VK_DYNAMIC_STATE_LINE_WIDTH),
        DEPTH_BIAS(VK10.VK_DYNAMIC_STATE_DEPTH_BIAS),
        BLEND_CONSTANTS(VK10.VK_DYNAMIC_STATE_BLEND_CONSTANTS),
        DEPTH_BOUNDS(VK10.VK_DYNAMIC_STATE_DEPTH_BOUNDS),
        STENCIL_COMPARE_MASK(VK10.VK_DYNAMIC_STATE_STENCIL_COMPARE_MASK),
        STENCIL_WRITE_MASK(VK10.VK_DYNAMIC_STATE_STENCIL_WRITE_MASK),
        STENCIL_REFERENCE(VK10.VK_DYNAMIC_STATE_STENCIL_REFERENCE),
        // VK 1.3 / EXT_extended_dynamic_state
        CULL_MODE(VK13.VK_DYNAMIC_STATE_CULL_MODE),
        FRONT_FACE(VK13.VK_DYNAMIC_STATE_FRONT_FACE),
        PRIMITIVE_TOPOLOGY(VK13.VK_DYNAMIC_STATE_PRIMITIVE_TOPOLOGY),
        VIEWPORT_WITH_COUNT(VK13.VK_DYNAMIC_STATE_VIEWPORT_WITH_COUNT),
        SCISSOR_WITH_COUNT(VK13.VK_DYNAMIC_STATE_SCISSOR_WITH_COUNT),
        VERTEX_INPUT_BINDING_STRIDE(VK13.VK_DYNAMIC_STATE_VERTEX_INPUT_BINDING_STRIDE),
        DEPTH_TEST_ENABLE(VK13.VK_DYNAMIC_STATE_DEPTH_TEST_ENABLE),
        DEPTH_WRITE_ENABLE(VK13.VK_DYNAMIC_STATE_DEPTH_WRITE_ENABLE),
        DEPTH_COMPARE_OP(VK13.VK_DYNAMIC_STATE_DEPTH_COMPARE_OP),
        DEPTH_BOUNDS_TEST_ENABLE(VK13.VK_DYNAMIC_STATE_DEPTH_BOUNDS_TEST_ENABLE),
        STENCIL_TEST_ENABLE(VK13.VK_DYNAMIC_STATE_STENCIL_TEST_ENABLE),
        STENCIL_OP(VK13.VK_DYNAMIC_STATE_STENCIL_OP),
        RASTERIZER_DISCARD_ENABLE(VK13.VK_DYNAMIC_STATE_RASTERIZER_DISCARD_ENABLE),
        DEPTH_BIAS_ENABLE(VK13.VK_DYNAMIC_STATE_DEPTH_BIAS_ENABLE),
        PRIMITIVE_RESTART_ENABLE(VK13.VK_DYNAMIC_STATE_PRIMITIVE_RESTART_ENABLE);

        public final int vkValue;

        DynamicState(int vkValue) {
            this.vkValue = vkValue;
        }

        public static EnumSet<DynamicState> getVk10States() {
            return EnumSet.of(VIEWPORT, SCISSOR, LINE_WIDTH, DEPTH_BIAS, BLEND_CONSTANTS,
                    DEPTH_BOUNDS, STENCIL_COMPARE_MASK, STENCIL_WRITE_MASK, STENCIL_REFERENCE);
        }

        public static EnumSet<DynamicState> getVk13States() {
            return EnumSet.allOf(DynamicState.class);
        }
    }

    // ========================================================================
    // MEMORY TYPE FLAGS
    // ========================================================================

    public static final int VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT = VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
    public static final int VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT = VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
    public static final int VK_MEMORY_PROPERTY_HOST_COHERENT_BIT = VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
    public static final int VK_MEMORY_PROPERTY_HOST_CACHED_BIT = VK10.VK_MEMORY_PROPERTY_HOST_CACHED_BIT;

    /**
     * Gets optimal memory properties for a buffer usage pattern.
     */
    public static int getOptimalMemoryProperties(int glUsage, boolean requiresMapping) {
        return switch (glUsage) {
            case GL15_STATIC_DRAW, GL15_STATIC_READ, GL15_STATIC_COPY ->
                    requiresMapping ? VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
                            : VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
            case GL15_DYNAMIC_DRAW, GL15_DYNAMIC_READ, GL15_DYNAMIC_COPY ->
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
            case GL15_STREAM_DRAW, GL15_STREAM_READ, GL15_STREAM_COPY ->
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
            default -> VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
        };
    }

    // ========================================================================
    // INDEX TYPE TRANSLATION
    // ========================================================================

    public static int toVkIndexType(int glType) {
        return switch (glType) {
            case GL11_UNSIGNED_BYTE -> VK10.VK_INDEX_TYPE_UINT8_EXT;
            case GL11_UNSIGNED_SHORT -> VK10.VK_INDEX_TYPE_UINT16;
            case GL11_UNSIGNED_INT -> VK10.VK_INDEX_TYPE_UINT32;
            default -> VK10.VK_INDEX_TYPE_UINT32;
        };
    }

    public static int getIndexTypeSize(int vkIndexType) {
        return switch (vkIndexType) {
            case VK10.VK_INDEX_TYPE_UINT16 -> 2;
            case VK10.VK_INDEX_TYPE_UINT32 -> 4;
            default -> 1; // UINT8
        };
    }
}
      
