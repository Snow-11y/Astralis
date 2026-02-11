package stellar.snow.astralis.api.vulkan.managers;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/**
 * VulkanState - Ultra-high-performance OpenGL state tracking for Vulkan translation
 * 
 * <h2>Architecture Overview</h2>
 * This class provides complete OpenGL state machine emulation optimized for Vulkan backend translation.
 * It employs multiple advanced techniques for maximum performance:
 * 
 * <h3>Performance Optimizations</h3>
 * <ul>
 *   <li><b>Zero-allocation hot paths:</b> All frequently called methods avoid heap allocation</li>
 *   <li><b>Primitive arrays over collections:</b> Fixed-size arrays replace HashMap for bounded state</li>
 *   <li><b>Dirty tracking:</b> Granular dirty bits minimize redundant Vulkan state updates</li>
 *   <li><b>State hashing:</b> Incremental hash computation for pipeline cache lookups</li>
 *   <li><b>Object pooling:</b> Reusable objects for temporary state snapshots</li>
 *   <li><b>Cache-friendly layout:</b> Related state grouped for CPU cache efficiency</li>
 *   <li><b>VarHandle access:</b> Volatile/atomic access without boxing overhead</li>
 *   <li><b>SIMD-ready:</b> State arrays aligned for potential Vector API acceleration</li>
 * </ul>
 * 
 * <h3>Java 25 Features Utilized</h3>
 * <ul>
 *   <li>Records for immutable state snapshots</li>
 *   <li>Sealed interfaces for state type hierarchies</li>
 *   <li>Pattern matching in switch expressions</li>
 *   <li>Foreign Function & Memory API for native interop</li>
 *   <li>Virtual thread compatibility</li>
 *   <li>Value-based class optimizations</li>
 * </ul>
 * 
 * <h3>LWJGL 3.3.6 Integration</h3>
 * <ul>
 *   <li>Direct VkPipeline*CreateInfo population</li>
 *   <li>MemoryStack-aware state export</li>
 *   <li>Native handle management</li>
 * </ul>
 * 
 * Snowy Enhanced for production use
 * @version 2.0.0
 * @since Java 25, LWJGL 3.3.6
 */
@SuppressWarnings("preview")
public final class VulkanState {
    
    // ========================================================================
    // CONFIGURATION CONSTANTS
    // ========================================================================
    
    /** Maximum number of texture units supported */
    public static final int MAX_TEXTURE_UNITS = 32;
    
    /** Maximum number of vertex attributes supported */
    public static final int MAX_VERTEX_ATTRIBS = 32;
    
    /** Maximum number of vertex buffer bindings */
    public static final int MAX_VERTEX_BINDINGS = 32;
    
    /** Maximum number of color attachments for MRT */
    public static final int MAX_COLOR_ATTACHMENTS = 8;
    
    /** Maximum number of viewports (for multi-viewport rendering) */
    public static final int MAX_VIEWPORTS = 16;
    
    /** Maximum number of UBO binding points */
    public static final int MAX_UNIFORM_BUFFER_BINDINGS = 36;
    
    /** Maximum number of SSBO binding points */
    public static final int MAX_SHADER_STORAGE_BINDINGS = 16;
    
    /** Maximum number of transform feedback buffer bindings */
    public static final int MAX_TRANSFORM_FEEDBACK_BINDINGS = 4;
    
    /** Maximum uniform locations tracked per program */
    public static final int MAX_UNIFORM_LOCATIONS = 1024;
    
    /** Maximum number of clip distances */
    public static final int MAX_CLIP_DISTANCES = 8;
    
    /** Maximum number of cull distances */
    public static final int MAX_CULL_DISTANCES = 8;
    
    /** Object pool size for temporary allocations */
    private static final int OBJECT_POOL_SIZE = 64;
    
    /** Initial capacity for sparse object maps */
    private static final int INITIAL_OBJECT_MAP_CAPACITY = 256;
    
    // ========================================================================
    // GL CONSTANTS - Complete enumeration for translation
    // ========================================================================
    
    /**
     * Comprehensive GL constants organized by category.
     * Using nested classes for namespace organization and compile-time constant folding.
     */
    public static final class GL {
        private GL() {}
        
        // ---- Buffer Targets ----
        public static final class Buffer {
            public static final int ARRAY_BUFFER = 0x8892;
            public static final int ELEMENT_ARRAY_BUFFER = 0x8893;
            public static final int UNIFORM_BUFFER = 0x8A11;
            public static final int SHADER_STORAGE_BUFFER = 0x90D2;
            public static final int DRAW_INDIRECT_BUFFER = 0x8F3F;
            public static final int DISPATCH_INDIRECT_BUFFER = 0x90EE;
            public static final int COPY_READ_BUFFER = 0x8F36;
            public static final int COPY_WRITE_BUFFER = 0x8F37;
            public static final int TRANSFORM_FEEDBACK_BUFFER = 0x8C8E;
            public static final int TEXTURE_BUFFER = 0x8C2A;
            public static final int PIXEL_PACK_BUFFER = 0x88EB;
            public static final int PIXEL_UNPACK_BUFFER = 0x88EC;
            public static final int ATOMIC_COUNTER_BUFFER = 0x92C0;
            public static final int QUERY_BUFFER = 0x9192;
            public static final int PARAMETER_BUFFER = 0x80EE;
            
            // Buffer usage hints
            public static final int STREAM_DRAW = 0x88E0;
            public static final int STREAM_READ = 0x88E1;
            public static final int STREAM_COPY = 0x88E2;
            public static final int STATIC_DRAW = 0x88E4;
            public static final int STATIC_READ = 0x88E5;
            public static final int STATIC_COPY = 0x88E6;
            public static final int DYNAMIC_DRAW = 0x88E8;
            public static final int DYNAMIC_READ = 0x88E9;
            public static final int DYNAMIC_COPY = 0x88EA;
            
            // Buffer storage flags
            public static final int MAP_READ_BIT = 0x0001;
            public static final int MAP_WRITE_BIT = 0x0002;
            public static final int MAP_PERSISTENT_BIT = 0x0040;
            public static final int MAP_COHERENT_BIT = 0x0080;
            public static final int DYNAMIC_STORAGE_BIT = 0x0100;
            public static final int CLIENT_STORAGE_BIT = 0x0200;
            
            // Buffer access flags
            public static final int MAP_INVALIDATE_RANGE_BIT = 0x0004;
            public static final int MAP_INVALIDATE_BUFFER_BIT = 0x0008;
            public static final int MAP_FLUSH_EXPLICIT_BIT = 0x0010;
            public static final int MAP_UNSYNCHRONIZED_BIT = 0x0020;
            
            private Buffer() {}
        }
        
        // ---- Shader Types ----
        public static final class Shader {
            public static final int VERTEX_SHADER = 0x8B31;
            public static final int FRAGMENT_SHADER = 0x8B30;
            public static final int GEOMETRY_SHADER = 0x8DD9;
            public static final int TESS_CONTROL_SHADER = 0x8E88;
            public static final int TESS_EVALUATION_SHADER = 0x8E87;
            public static final int COMPUTE_SHADER = 0x91B9;
            public static final int MESH_SHADER_NV = 0x9559;
            public static final int TASK_SHADER_NV = 0x955A;
            
            // Shader parameters
            public static final int SHADER_TYPE = 0x8B4F;
            public static final int DELETE_STATUS = 0x8B80;
            public static final int COMPILE_STATUS = 0x8B81;
            public static final int INFO_LOG_LENGTH = 0x8B84;
            public static final int SHADER_SOURCE_LENGTH = 0x8B88;
            
            private Shader() {}
        }
        
        // ---- Texture Targets ----
        public static final class Texture {
            public static final int TEXTURE_1D = 0x0DE0;
            public static final int TEXTURE_2D = 0x0DE1;
            public static final int TEXTURE_3D = 0x806F;
            public static final int TEXTURE_1D_ARRAY = 0x8C18;
            public static final int TEXTURE_2D_ARRAY = 0x8C1A;
            public static final int TEXTURE_RECTANGLE = 0x84F5;
            public static final int TEXTURE_CUBE_MAP = 0x8513;
            public static final int TEXTURE_CUBE_MAP_ARRAY = 0x9009;
            public static final int TEXTURE_BUFFER = 0x8C2A;
            public static final int TEXTURE_2D_MULTISAMPLE = 0x9100;
            public static final int TEXTURE_2D_MULTISAMPLE_ARRAY = 0x9102;
            
            // Texture parameters
            public static final int TEXTURE_MAG_FILTER = 0x2800;
            public static final int TEXTURE_MIN_FILTER = 0x2801;
            public static final int TEXTURE_WRAP_S = 0x2802;
            public static final int TEXTURE_WRAP_T = 0x2803;
            public static final int TEXTURE_WRAP_R = 0x8072;
            public static final int TEXTURE_MIN_LOD = 0x813A;
            public static final int TEXTURE_MAX_LOD = 0x813B;
            public static final int TEXTURE_LOD_BIAS = 0x8501;
            public static final int TEXTURE_COMPARE_MODE = 0x884C;
            public static final int TEXTURE_COMPARE_FUNC = 0x884D;
            public static final int TEXTURE_MAX_ANISOTROPY = 0x84FE;
            public static final int TEXTURE_BORDER_COLOR = 0x1004;
            public static final int TEXTURE_SWIZZLE_R = 0x8E42;
            public static final int TEXTURE_SWIZZLE_G = 0x8E43;
            public static final int TEXTURE_SWIZZLE_B = 0x8E44;
            public static final int TEXTURE_SWIZZLE_A = 0x8E45;
            public static final int TEXTURE_SWIZZLE_RGBA = 0x8E46;
            public static final int TEXTURE_BASE_LEVEL = 0x813C;
            public static final int TEXTURE_MAX_LEVEL = 0x813D;
            
            // Filter modes
            public static final int NEAREST = 0x2600;
            public static final int LINEAR = 0x2601;
            public static final int NEAREST_MIPMAP_NEAREST = 0x2700;
            public static final int LINEAR_MIPMAP_NEAREST = 0x2701;
            public static final int NEAREST_MIPMAP_LINEAR = 0x2702;
            public static final int LINEAR_MIPMAP_LINEAR = 0x2703;
            
            // Wrap modes
            public static final int REPEAT = 0x2901;
            public static final int CLAMP_TO_EDGE = 0x812F;
            public static final int CLAMP_TO_BORDER = 0x812D;
            public static final int MIRRORED_REPEAT = 0x8370;
            public static final int MIRROR_CLAMP_TO_EDGE = 0x8743;
            
            // Compare modes
            public static final int COMPARE_REF_TO_TEXTURE = 0x884E;
            public static final int NONE = 0;
            
            // Texture unit base
            public static final int TEXTURE0 = 0x84C0;
            
            private Texture() {}
        }
        
        // ---- Capabilities ----
        public static final class Capability {
            public static final int BLEND = 0x0BE2;
            public static final int DEPTH_TEST = 0x0B71;
            public static final int STENCIL_TEST = 0x0B90;
            public static final int CULL_FACE = 0x0B44;
            public static final int SCISSOR_TEST = 0x0C11;
            public static final int POLYGON_OFFSET_FILL = 0x8037;
            public static final int POLYGON_OFFSET_LINE = 0x2A02;
            public static final int POLYGON_OFFSET_POINT = 0x2A01;
            public static final int MULTISAMPLE = 0x809D;
            public static final int SAMPLE_ALPHA_TO_COVERAGE = 0x809E;
            public static final int SAMPLE_ALPHA_TO_ONE = 0x809F;
            public static final int SAMPLE_COVERAGE = 0x80A0;
            public static final int SAMPLE_SHADING = 0x8C36;
            public static final int SAMPLE_MASK = 0x8E51;
            public static final int PROGRAM_POINT_SIZE = 0x8642;
            public static final int DEPTH_CLAMP = 0x864F;
            public static final int PRIMITIVE_RESTART = 0x8F9D;
            public static final int PRIMITIVE_RESTART_FIXED_INDEX = 0x8D69;
            public static final int RASTERIZER_DISCARD = 0x8C89;
            public static final int COLOR_LOGIC_OP = 0x0BF2;
            public static final int LINE_SMOOTH = 0x0B20;
            public static final int POLYGON_SMOOTH = 0x0B41;
            public static final int FRAMEBUFFER_SRGB = 0x8DB9;
            public static final int DITHER = 0x0BD0;
            public static final int CLIP_DISTANCE0 = 0x3000;
            public static final int DEBUG_OUTPUT = 0x92E0;
            public static final int DEBUG_OUTPUT_SYNCHRONOUS = 0x8242;
            public static final int TEXTURE_CUBE_MAP_SEAMLESS = 0x884F;
            
            private Capability() {}
        }
        
        // ---- Blend Functions ----
        public static final class Blend {
            public static final int ZERO = 0;
            public static final int ONE = 1;
            public static final int SRC_COLOR = 0x0300;
            public static final int ONE_MINUS_SRC_COLOR = 0x0301;
            public static final int DST_COLOR = 0x0306;
            public static final int ONE_MINUS_DST_COLOR = 0x0307;
            public static final int SRC_ALPHA = 0x0302;
            public static final int ONE_MINUS_SRC_ALPHA = 0x0303;
            public static final int DST_ALPHA = 0x0304;
            public static final int ONE_MINUS_DST_ALPHA = 0x0305;
            public static final int CONSTANT_COLOR = 0x8001;
            public static final int ONE_MINUS_CONSTANT_COLOR = 0x8002;
            public static final int CONSTANT_ALPHA = 0x8003;
            public static final int ONE_MINUS_CONSTANT_ALPHA = 0x8004;
            public static final int SRC_ALPHA_SATURATE = 0x0308;
            public static final int SRC1_COLOR = 0x88F9;
            public static final int ONE_MINUS_SRC1_COLOR = 0x88FA;
            public static final int SRC1_ALPHA = 0x8589;
            public static final int ONE_MINUS_SRC1_ALPHA = 0x88FB;
            
            // Blend equations
            public static final int FUNC_ADD = 0x8006;
            public static final int FUNC_SUBTRACT = 0x800A;
            public static final int FUNC_REVERSE_SUBTRACT = 0x800B;
            public static final int MIN = 0x8007;
            public static final int MAX = 0x8008;
            
            private Blend() {}
        }
        
        // ---- Comparison Functions ----
        public static final class Compare {
            public static final int NEVER = 0x0200;
            public static final int LESS = 0x0201;
            public static final int EQUAL = 0x0202;
            public static final int LEQUAL = 0x0203;
            public static final int GREATER = 0x0204;
            public static final int NOTEQUAL = 0x0205;
            public static final int GEQUAL = 0x0206;
            public static final int ALWAYS = 0x0207;
            
            private Compare() {}
        }
        
        // ---- Stencil Operations ----
        public static final class Stencil {
            public static final int KEEP = 0x1E00;
            public static final int ZERO = 0;
            public static final int REPLACE = 0x1E01;
            public static final int INCR = 0x1E02;
            public static final int INCR_WRAP = 0x8507;
            public static final int DECR = 0x1E03;
            public static final int DECR_WRAP = 0x8508;
            public static final int INVERT = 0x150A;
            
            private Stencil() {}
        }
        
        // ---- Face Culling ----
        public static final class Face {
            public static final int FRONT = 0x0404;
            public static final int BACK = 0x0405;
            public static final int FRONT_AND_BACK = 0x0408;
            public static final int CW = 0x0900;
            public static final int CCW = 0x0901;
            
            private Face() {}
        }
        
        // ---- Polygon Modes ----
        public static final class Polygon {
            public static final int POINT = 0x1B00;
            public static final int LINE = 0x1B01;
            public static final int FILL = 0x1B02;
            
            private Polygon() {}
        }
        
        // ---- Logic Operations ----
        public static final class Logic {
            public static final int CLEAR = 0x1500;
            public static final int AND = 0x1501;
            public static final int AND_REVERSE = 0x1502;
            public static final int COPY = 0x1503;
            public static final int AND_INVERTED = 0x1504;
            public static final int NOOP = 0x1505;
            public static final int XOR = 0x1506;
            public static final int OR = 0x1507;
            public static final int NOR = 0x1508;
            public static final int EQUIV = 0x1509;
            public static final int INVERT = 0x150A;
            public static final int OR_REVERSE = 0x150B;
            public static final int COPY_INVERTED = 0x150C;
            public static final int OR_INVERTED = 0x150D;
            public static final int NAND = 0x150E;
            public static final int SET = 0x150F;
            
            private Logic() {}
        }
        
        // ---- Data Types ----
        public static final class Type {
            public static final int BYTE = 0x1400;
            public static final int UNSIGNED_BYTE = 0x1401;
            public static final int SHORT = 0x1402;
            public static final int UNSIGNED_SHORT = 0x1403;
            public static final int INT = 0x1404;
            public static final int UNSIGNED_INT = 0x1405;
            public static final int FLOAT = 0x1406;
            public static final int DOUBLE = 0x140A;
            public static final int HALF_FLOAT = 0x140B;
            public static final int FIXED = 0x140C;
            public static final int INT_2_10_10_10_REV = 0x8D9F;
            public static final int UNSIGNED_INT_2_10_10_10_REV = 0x8368;
            public static final int UNSIGNED_INT_10F_11F_11F_REV = 0x8C3B;
            
            private Type() {}
        }
        
        // ---- Primitive Types ----
        public static final class Primitive {
            public static final int POINTS = 0x0000;
            public static final int LINES = 0x0001;
            public static final int LINE_LOOP = 0x0002;
            public static final int LINE_STRIP = 0x0003;
            public static final int TRIANGLES = 0x0004;
            public static final int TRIANGLE_STRIP = 0x0005;
            public static final int TRIANGLE_FAN = 0x0006;
            public static final int LINES_ADJACENCY = 0x000A;
            public static final int LINE_STRIP_ADJACENCY = 0x000B;
            public static final int TRIANGLES_ADJACENCY = 0x000C;
            public static final int TRIANGLE_STRIP_ADJACENCY = 0x000D;
            public static final int PATCHES = 0x000E;
            
            private Primitive() {}
        }
        
        // ---- Clear Bits ----
        public static final class Clear {
            public static final int COLOR_BUFFER_BIT = 0x00004000;
            public static final int DEPTH_BUFFER_BIT = 0x00000100;
            public static final int STENCIL_BUFFER_BIT = 0x00000400;
            
            private Clear() {}
        }
        
        // ---- Internal Formats ----
        public static final class Format {
            // Sized internal formats
            public static final int R8 = 0x8229;
            public static final int R16 = 0x822A;
            public static final int R16F = 0x822D;
            public static final int R32F = 0x822E;
            public static final int R8I = 0x8231;
            public static final int R16I = 0x8233;
            public static final int R32I = 0x8235;
            public static final int R8UI = 0x8232;
            public static final int R16UI = 0x8234;
            public static final int R32UI = 0x8236;
            public static final int RG8 = 0x822B;
            public static final int RG16 = 0x822C;
            public static final int RG16F = 0x822F;
            public static final int RG32F = 0x8230;
            public static final int RG8I = 0x8237;
            public static final int RG16I = 0x8239;
            public static final int RG32I = 0x823B;
            public static final int RG8UI = 0x8238;
            public static final int RG16UI = 0x823A;
            public static final int RG32UI = 0x823C;
            public static final int RGB8 = 0x8051;
            public static final int RGB16F = 0x881B;
            public static final int RGB32F = 0x8815;
            public static final int RGBA8 = 0x8058;
            public static final int RGBA16 = 0x805B;
            public static final int RGBA16F = 0x881A;
            public static final int RGBA32F = 0x8814;
            public static final int RGBA8I = 0x8D8E;
            public static final int RGBA16I = 0x8D88;
            public static final int RGBA32I = 0x8D82;
            public static final int RGBA8UI = 0x8D7C;
            public static final int RGBA16UI = 0x8D76;
            public static final int RGBA32UI = 0x8D70;
            public static final int SRGB8 = 0x8C41;
            public static final int SRGB8_ALPHA8 = 0x8C43;
            public static final int DEPTH_COMPONENT16 = 0x81A5;
            public static final int DEPTH_COMPONENT24 = 0x81A6;
            public static final int DEPTH_COMPONENT32 = 0x81A7;
            public static final int DEPTH_COMPONENT32F = 0x8CAC;
            public static final int DEPTH24_STENCIL8 = 0x88F0;
            public static final int DEPTH32F_STENCIL8 = 0x8CAD;
            public static final int STENCIL_INDEX8 = 0x8D48;
            
            // Compressed formats
            public static final int COMPRESSED_RGB_S3TC_DXT1 = 0x83F0;
            public static final int COMPRESSED_RGBA_S3TC_DXT1 = 0x83F1;
            public static final int COMPRESSED_RGBA_S3TC_DXT3 = 0x83F2;
            public static final int COMPRESSED_RGBA_S3TC_DXT5 = 0x83F3;
            public static final int COMPRESSED_SRGB_S3TC_DXT1 = 0x8C4C;
            public static final int COMPRESSED_SRGB_ALPHA_S3TC_DXT1 = 0x8C4D;
            public static final int COMPRESSED_SRGB_ALPHA_S3TC_DXT3 = 0x8C4E;
            public static final int COMPRESSED_SRGB_ALPHA_S3TC_DXT5 = 0x8C4F;
            public static final int COMPRESSED_RED_RGTC1 = 0x8DBB;
            public static final int COMPRESSED_SIGNED_RED_RGTC1 = 0x8DBC;
            public static final int COMPRESSED_RG_RGTC2 = 0x8DBD;
            public static final int COMPRESSED_SIGNED_RG_RGTC2 = 0x8DBE;
            public static final int COMPRESSED_RGBA_BPTC_UNORM = 0x8E8C;
            public static final int COMPRESSED_SRGB_ALPHA_BPTC_UNORM = 0x8E8D;
            public static final int COMPRESSED_RGB_BPTC_SIGNED_FLOAT = 0x8E8E;
            public static final int COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT = 0x8E8F;
            
            private Format() {}
        }
    }
    
    // ========================================================================
    // DIRTY FLAGS - Bit-packed for efficient checking
    // ========================================================================
    
    /**
     * Dirty flag bits for tracking state changes.
     * Using long for up to 64 distinct dirty categories.
     */
    public static final class DirtyFlags {
        public static final long NONE = 0L;
        public static final long VIEWPORT = 1L;
        public static final long SCISSOR = 1L << 1;
        public static final long BLEND_STATE = 1L << 2;
        public static final long BLEND_CONSTANTS = 1L << 3;
        public static final long DEPTH_STATE = 1L << 4;
        public static final long DEPTH_BOUNDS = 1L << 5;
        public static final long DEPTH_BIAS = 1L << 6;
        public static final long STENCIL_STATE = 1L << 7;
        public static final long STENCIL_REFERENCE = 1L << 8;
        public static final long CULL_STATE = 1L << 9;
        public static final long LINE_WIDTH = 1L << 10;
        public static final long VERTEX_INPUT = 1L << 11;
        public static final long VERTEX_BINDINGS = 1L << 12;
        public static final long TEXTURE_BINDINGS = 1L << 13;
        public static final long UNIFORM_BUFFER_BINDINGS = 1L << 14;
        public static final long SHADER_STORAGE_BINDINGS = 1L << 15;
        public static final long PROGRAM = 1L << 16;
        public static final long DESCRIPTORS = 1L << 17;
        public static final long PUSH_CONSTANTS = 1L << 18;
        public static final long COLOR_MASK = 1L << 19;
        public static final long POLYGON_MODE = 1L << 20;
        public static final long SAMPLE_STATE = 1L << 21;
        public static final long LOGIC_OP = 1L << 22;
        public static final long PRIMITIVE_TOPOLOGY = 1L << 23;
        public static final long PRIMITIVE_RESTART = 1L << 24;
        public static final long RASTERIZER_DISCARD = 1L << 25;
        public static final long DEPTH_CLAMP = 1L << 26;
        public static final long FRONT_FACE = 1L << 27;
        public static final long PATCH_CONTROL_POINTS = 1L << 28;
        public static final long SAMPLE_MASK = 1L << 29;
        public static final long CLEAR_VALUES = 1L << 30;
        public static final long VAO = 1L << 31;
        public static final long ELEMENT_BUFFER = 1L << 32;
        
        // Composite flags for common update patterns
        public static final long ALL_DYNAMIC_STATE = 
            VIEWPORT | SCISSOR | BLEND_CONSTANTS | DEPTH_BOUNDS | DEPTH_BIAS |
            STENCIL_REFERENCE | LINE_WIDTH | VERTEX_BINDINGS;
        
        public static final long ALL_PIPELINE_STATE = 
            BLEND_STATE | DEPTH_STATE | STENCIL_STATE | CULL_STATE |
            VERTEX_INPUT | COLOR_MASK | POLYGON_MODE | SAMPLE_STATE |
            LOGIC_OP | PRIMITIVE_TOPOLOGY | PRIMITIVE_RESTART |
            RASTERIZER_DISCARD | DEPTH_CLAMP | FRONT_FACE | PATCH_CONTROL_POINTS;
        
        public static final long ALL = -1L;
        
        private DirtyFlags() {}
    }
    
    // ========================================================================
    // VARHANDLE FOR ATOMIC DIRTY FLAG ACCESS
    // ========================================================================
    
    private static final VarHandle DIRTY_FLAGS_HANDLE;
    private static final VarHandle STATE_VERSION_HANDLE;
    
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            DIRTY_FLAGS_HANDLE = lookup.findVarHandle(VulkanState.class, "dirtyFlags", long.class);
            STATE_VERSION_HANDLE = lookup.findVarHandle(VulkanState.class, "stateVersion", long.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    // ========================================================================
    // CORE STATE FIELDS
    // ========================================================================
    
    /** Dirty flags indicating which state needs to be synchronized */
    @SuppressWarnings("unused") // Accessed via VarHandle
    private volatile long dirtyFlags = DirtyFlags.ALL;
    
    /** Monotonically increasing version for cache invalidation */
    @SuppressWarnings("unused") // Accessed via VarHandle
    private volatile long stateVersion = 0;
    
    /** Lock for thread-safe state access (optional - enable via constructor) */
    private final StampedLock stateLock;
    private final boolean threadSafe;
    
    /** Pipeline state hash cache - invalidated on relevant state changes */
    private long cachedPipelineHash = 0;
    private boolean pipelineHashValid = false;
    
    /** Vertex input state hash cache */
    private int cachedVertexInputHash = 0;
    private boolean vertexInputHashValid = false;
    
    // ========================================================================
    // OBJECT ID GENERATION
    // ========================================================================
    
    private final AtomicLong nextTextureId = new AtomicLong(1);
    private final AtomicLong nextBufferId = new AtomicLong(1);
    private final AtomicLong nextShaderId = new AtomicLong(1);
    private final AtomicLong nextProgramId = new AtomicLong(1);
    private final AtomicLong nextVaoId = new AtomicLong(1);
    private final AtomicLong nextQueryId = new AtomicLong(1);
    private final AtomicLong nextSamplerId = new AtomicLong(1);
    private final AtomicLong nextFramebufferId = new AtomicLong(1);
    private final AtomicLong nextRenderbufferId = new AtomicLong(1);
    
    // ========================================================================
    // TEXTURE STATE
    // ========================================================================
    
    /** Currently active texture unit (0-based index) */
    private int activeTextureUnit = 0;
    
    /** Bound textures per unit - primitive array for cache efficiency */
    private final long[] boundTextures = new long[MAX_TEXTURE_UNITS];
    
    /** Bound samplers per unit (separate sampler objects) */
    private final long[] boundSamplers = new long[MAX_TEXTURE_UNITS];
    
    /** Texture object storage - using custom lightweight map */
    private final LongObjectMap<TextureObject> textures;
    
    /** Sampler object storage */
    private final LongObjectMap<SamplerObject> samplers;
    
    /**
     * Texture object representing a GL texture.
     * Designed for minimal memory footprint and cache-friendly access.
     */
    public static final class TextureObject {
        // Vulkan handles
        public long vkImage;
        public long vkMemory;
        public long vkImageView;
        public long vkSampler; // Embedded sampler (if not using separate sampler objects)
        
        // Dimensions and format
        public int width;
        public int height;
        public int depth = 1;
        public int mipLevels = 1;
        public int arrayLayers = 1;
        public int internalFormat;
        public int target; // GL_TEXTURE_2D, etc.
        
        // Sampler state (packed for efficiency)
        private int magFilter = GL.Texture.LINEAR;
        private int minFilter = GL.Texture.LINEAR_MIPMAP_LINEAR;
        private int wrapS = GL.Texture.REPEAT;
        private int wrapT = GL.Texture.REPEAT;
        private int wrapR = GL.Texture.REPEAT;
        private int compareMode = GL.Texture.NONE;
        private int compareFunc = GL.Compare.LEQUAL;
        private int swizzleR = 0x1903; // GL_RED
        private int swizzleG = 0x1904; // GL_GREEN
        private int swizzleB = 0x1905; // GL_BLUE
        private int swizzleA = 0x1906; // GL_ALPHA
        private int baseLevel = 0;
        private int maxLevel = 1000;
        private float minLod = -1000.0f;
        private float maxLod = 1000.0f;
        private float lodBias = 0.0f;
        private float maxAnisotropy = 1.0f;
        private final float[] borderColor = {0.0f, 0.0f, 0.0f, 0.0f};
        
        // State tracking
        private boolean samplerDirty = true;
        private long samplerStateHash = 0;
        
        // Immutability flag (for immutable storage textures)
        public boolean immutable = false;
        public int immutableLevels = 0;
        
        // Parameter access with bounds checking
        public void setParameteri(int pname, int value) {
            switch (pname) {
                case GL.Texture.TEXTURE_MAG_FILTER -> magFilter = value;
                case GL.Texture.TEXTURE_MIN_FILTER -> minFilter = value;
                case GL.Texture.TEXTURE_WRAP_S -> wrapS = value;
                case GL.Texture.TEXTURE_WRAP_T -> wrapT = value;
                case GL.Texture.TEXTURE_WRAP_R -> wrapR = value;
                case GL.Texture.TEXTURE_COMPARE_MODE -> compareMode = value;
                case GL.Texture.TEXTURE_COMPARE_FUNC -> compareFunc = value;
                case GL.Texture.TEXTURE_SWIZZLE_R -> swizzleR = value;
                case GL.Texture.TEXTURE_SWIZZLE_G -> swizzleG = value;
                case GL.Texture.TEXTURE_SWIZZLE_B -> swizzleB = value;
                case GL.Texture.TEXTURE_SWIZZLE_A -> swizzleA = value;
                case GL.Texture.TEXTURE_BASE_LEVEL -> baseLevel = value;
                case GL.Texture.TEXTURE_MAX_LEVEL -> maxLevel = value;
                default -> { return; } // Unknown parameter, don't invalidate
            }
            samplerDirty = true;
        }
        
        public int getParameteri(int pname) {
            return switch (pname) {
                case GL.Texture.TEXTURE_MAG_FILTER -> magFilter;
                case GL.Texture.TEXTURE_MIN_FILTER -> minFilter;
                case GL.Texture.TEXTURE_WRAP_S -> wrapS;
                case GL.Texture.TEXTURE_WRAP_T -> wrapT;
                case GL.Texture.TEXTURE_WRAP_R -> wrapR;
                case GL.Texture.TEXTURE_COMPARE_MODE -> compareMode;
                case GL.Texture.TEXTURE_COMPARE_FUNC -> compareFunc;
                case GL.Texture.TEXTURE_SWIZZLE_R -> swizzleR;
                case GL.Texture.TEXTURE_SWIZZLE_G -> swizzleG;
                case GL.Texture.TEXTURE_SWIZZLE_B -> swizzleB;
                case GL.Texture.TEXTURE_SWIZZLE_A -> swizzleA;
                case GL.Texture.TEXTURE_BASE_LEVEL -> baseLevel;
                case GL.Texture.TEXTURE_MAX_LEVEL -> maxLevel;
                default -> 0;
            };
        }
        
        public void setParameterf(int pname, float value) {
            switch (pname) {
                case GL.Texture.TEXTURE_MIN_LOD -> minLod = value;
                case GL.Texture.TEXTURE_MAX_LOD -> maxLod = value;
                case GL.Texture.TEXTURE_LOD_BIAS -> lodBias = value;
                case GL.Texture.TEXTURE_MAX_ANISOTROPY -> maxAnisotropy = Math.max(1.0f, value);
                default -> { return; }
            }
            samplerDirty = true;
        }
        
        public float getParameterf(int pname) {
            return switch (pname) {
                case GL.Texture.TEXTURE_MIN_LOD -> minLod;
                case GL.Texture.TEXTURE_MAX_LOD -> maxLod;
                case GL.Texture.TEXTURE_LOD_BIAS -> lodBias;
                case GL.Texture.TEXTURE_MAX_ANISOTROPY -> maxAnisotropy;
                default -> 0.0f;
            };
        }
        
        public void setBorderColor(float r, float g, float b, float a) {
            borderColor[0] = r;
            borderColor[1] = g;
            borderColor[2] = b;
            borderColor[3] = a;
            samplerDirty = true;
        }
        
        public float[] getBorderColor() {
            return borderColor.clone();
        }
        
        public void getBorderColor(float[] dest) {
            System.arraycopy(borderColor, 0, dest, 0, 4);
        }
        
        public boolean isSamplerDirty() {
            return samplerDirty;
        }
        
        public void clearSamplerDirty() {
            samplerDirty = false;
        }
        
        /**
         * Compute sampler state hash for Vulkan sampler caching.
         */
        public long computeSamplerHash() {
            if (!samplerDirty && samplerStateHash != 0) {
                return samplerStateHash;
            }
            
            long hash = 17;
            hash = 31 * hash + magFilter;
            hash = 31 * hash + minFilter;
            hash = 31 * hash + wrapS;
            hash = 31 * hash + wrapT;
            hash = 31 * hash + wrapR;
            hash = 31 * hash + compareMode;
            hash = 31 * hash + compareFunc;
            hash = 31 * hash + Float.floatToRawIntBits(minLod);
            hash = 31 * hash + Float.floatToRawIntBits(maxLod);
            hash = 31 * hash + Float.floatToRawIntBits(lodBias);
            hash = 31 * hash + Float.floatToRawIntBits(maxAnisotropy);
            hash = 31 * hash + Float.floatToRawIntBits(borderColor[0]);
            hash = 31 * hash + Float.floatToRawIntBits(borderColor[1]);
            hash = 31 * hash + Float.floatToRawIntBits(borderColor[2]);
            hash = 31 * hash + Float.floatToRawIntBits(borderColor[3]);
            
            samplerStateHash = hash;
            return hash;
        }
        
        /**
         * Populate Vulkan sampler create info from texture state.
         */
        public void populateSamplerCreateInfo(VkSamplerCreateInfo info) {
            info.magFilter(glFilterToVk(magFilter, false));
            info.minFilter(glFilterToVk(minFilter, true));
            info.mipmapMode(glMipmapModeToVk(minFilter));
            info.addressModeU(glWrapToVk(wrapS));
            info.addressModeV(glWrapToVk(wrapT));
            info.addressModeW(glWrapToVk(wrapR));
            info.mipLodBias(lodBias);
            info.anisotropyEnable(maxAnisotropy > 1.0f);
            info.maxAnisotropy(maxAnisotropy);
            info.compareEnable(compareMode == GL.Texture.COMPARE_REF_TO_TEXTURE);
            info.compareOp(glCompareToVk(compareFunc));
            info.minLod(minLod);
            info.maxLod(maxLod);
            info.borderColor(determineBorderColor());
            info.unnormalizedCoordinates(false);
        }
        
        private static int glFilterToVk(int glFilter, boolean forMin) {
            return switch (glFilter) {
                case GL.Texture.NEAREST, GL.Texture.NEAREST_MIPMAP_NEAREST, 
                     GL.Texture.NEAREST_MIPMAP_LINEAR -> VK10.VK_FILTER_NEAREST;
                default -> VK10.VK_FILTER_LINEAR;
            };
        }
        
        private static int glMipmapModeToVk(int glFilter) {
            return switch (glFilter) {
                case GL.Texture.NEAREST_MIPMAP_NEAREST, GL.Texture.LINEAR_MIPMAP_NEAREST 
                    -> VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST;
                default -> VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR;
            };
        }
        
        private static int glWrapToVk(int glWrap) {
            return switch (glWrap) {
                case GL.Texture.CLAMP_TO_EDGE -> VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
                case GL.Texture.CLAMP_TO_BORDER -> VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
                case GL.Texture.MIRRORED_REPEAT -> VK10.VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT;
                case GL.Texture.MIRROR_CLAMP_TO_EDGE -> VK12.VK_SAMPLER_ADDRESS_MODE_MIRROR_CLAMP_TO_EDGE;
                default -> VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT;
            };
        }
        
        private static int glCompareToVk(int glCompare) {
            return switch (glCompare) {
                case GL.Compare.NEVER -> VK10.VK_COMPARE_OP_NEVER;
                case GL.Compare.LESS -> VK10.VK_COMPARE_OP_LESS;
                case GL.Compare.EQUAL -> VK10.VK_COMPARE_OP_EQUAL;
                case GL.Compare.LEQUAL -> VK10.VK_COMPARE_OP_LESS_OR_EQUAL;
                case GL.Compare.GREATER -> VK10.VK_COMPARE_OP_GREATER;
                case GL.Compare.NOTEQUAL -> VK10.VK_COMPARE_OP_NOT_EQUAL;
                case GL.Compare.GEQUAL -> VK10.VK_COMPARE_OP_GREATER_OR_EQUAL;
                case GL.Compare.ALWAYS -> VK10.VK_COMPARE_OP_ALWAYS;
                default -> VK10.VK_COMPARE_OP_ALWAYS;
            };
        }
        
        private int determineBorderColor() {
            // Determine border color enum based on actual color values
            boolean isInt = false; // Would need format info to determine
            boolean isTransparent = borderColor[3] == 0.0f;
            boolean isBlack = borderColor[0] == 0.0f && borderColor[1] == 0.0f && borderColor[2] == 0.0f;
            boolean isWhite = borderColor[0] == 1.0f && borderColor[1] == 1.0f && borderColor[2] == 1.0f;
            
            if (isTransparent && isBlack) {
                return isInt ? VK10.VK_BORDER_COLOR_INT_TRANSPARENT_BLACK 
                             : VK10.VK_BORDER_COLOR_FLOAT_TRANSPARENT_BLACK;
            } else if (!isTransparent && isBlack) {
                return isInt ? VK10.VK_BORDER_COLOR_INT_OPAQUE_BLACK 
                             : VK10.VK_BORDER_COLOR_FLOAT_OPAQUE_BLACK;
            } else if (!isTransparent && isWhite) {
                return isInt ? VK10.VK_BORDER_COLOR_INT_OPAQUE_WHITE 
                             : VK10.VK_BORDER_COLOR_FLOAT_OPAQUE_WHITE;
            }
            // Custom border color requires VK_EXT_custom_border_color
            return VK10.VK_BORDER_COLOR_FLOAT_OPAQUE_BLACK;
        }
        
        /** Reset to default state */
        public void reset() {
            vkImage = 0;
            vkMemory = 0;
            vkImageView = 0;
            vkSampler = 0;
            width = 0;
            height = 0;
            depth = 1;
            mipLevels = 1;
            arrayLayers = 1;
            internalFormat = 0;
            target = 0;
            magFilter = GL.Texture.LINEAR;
            minFilter = GL.Texture.LINEAR_MIPMAP_LINEAR;
            wrapS = GL.Texture.REPEAT;
            wrapT = GL.Texture.REPEAT;
            wrapR = GL.Texture.REPEAT;
            compareMode = GL.Texture.NONE;
            compareFunc = GL.Compare.LEQUAL;
            swizzleR = 0x1903;
            swizzleG = 0x1904;
            swizzleB = 0x1905;
            swizzleA = 0x1906;
            baseLevel = 0;
            maxLevel = 1000;
            minLod = -1000.0f;
            maxLod = 1000.0f;
            lodBias = 0.0f;
            maxAnisotropy = 1.0f;
            borderColor[0] = 0.0f;
            borderColor[1] = 0.0f;
            borderColor[2] = 0.0f;
            borderColor[3] = 0.0f;
            samplerDirty = true;
            samplerStateHash = 0;
            immutable = false;
            immutableLevels = 0;
        }
    }
    
    /**
     * Separate sampler object (for bindless/separate sampler usage).
     */
    public static final class SamplerObject {
        public long vkSampler;
        
        private int magFilter = GL.Texture.LINEAR;
        private int minFilter = GL.Texture.LINEAR_MIPMAP_LINEAR;
        private int wrapS = GL.Texture.REPEAT;
        private int wrapT = GL.Texture.REPEAT;
        private int wrapR = GL.Texture.REPEAT;
        private int compareMode = GL.Texture.NONE;
        private int compareFunc = GL.Compare.LEQUAL;
        private float minLod = -1000.0f;
        private float maxLod = 1000.0f;
        private float lodBias = 0.0f;
        private float maxAnisotropy = 1.0f;
        private final float[] borderColor = {0.0f, 0.0f, 0.0f, 0.0f};
        
        private boolean dirty = true;
        private long stateHash = 0;
        
        public void setParameteri(int pname, int value) {
            switch (pname) {
                case GL.Texture.TEXTURE_MAG_FILTER -> magFilter = value;
                case GL.Texture.TEXTURE_MIN_FILTER -> minFilter = value;
                case GL.Texture.TEXTURE_WRAP_S -> wrapS = value;
                case GL.Texture.TEXTURE_WRAP_T -> wrapT = value;
                case GL.Texture.TEXTURE_WRAP_R -> wrapR = value;
                case GL.Texture.TEXTURE_COMPARE_MODE -> compareMode = value;
                case GL.Texture.TEXTURE_COMPARE_FUNC -> compareFunc = value;
                default -> { return; }
            }
            dirty = true;
        }
        
        public void setParameterf(int pname, float value) {
            switch (pname) {
                case GL.Texture.TEXTURE_MIN_LOD -> minLod = value;
                case GL.Texture.TEXTURE_MAX_LOD -> maxLod = value;
                case GL.Texture.TEXTURE_LOD_BIAS -> lodBias = value;
                case GL.Texture.TEXTURE_MAX_ANISOTROPY -> maxAnisotropy = Math.max(1.0f, value);
                default -> { return; }
            }
            dirty = true;
        }
        
        public boolean isDirty() { return dirty; }
        public void clearDirty() { dirty = false; }
    }
    
    // ========================================================================
    // BUFFER STATE
    // ========================================================================
    
    /** Buffer target binding slots */
    private static final int BUFFER_TARGET_COUNT = 16;
    private final long[] boundBuffersByTarget = new long[BUFFER_TARGET_COUNT];
    
    /** Buffer target to index mapping (computed at static init for O(1) lookup) */
    private static final int[] BUFFER_TARGET_INDEX = new int[0x10000];
    
    static {
        Arrays.fill(BUFFER_TARGET_INDEX, -1);
        BUFFER_TARGET_INDEX[GL.Buffer.ARRAY_BUFFER & 0xFFFF] = 0;
        BUFFER_TARGET_INDEX[GL.Buffer.ELEMENT_ARRAY_BUFFER & 0xFFFF] = 1;
        BUFFER_TARGET_INDEX[GL.Buffer.UNIFORM_BUFFER & 0xFFFF] = 2;
        BUFFER_TARGET_INDEX[GL.Buffer.SHADER_STORAGE_BUFFER & 0xFFFF] = 3;
        BUFFER_TARGET_INDEX[GL.Buffer.DRAW_INDIRECT_BUFFER & 0xFFFF] = 4;
        BUFFER_TARGET_INDEX[GL.Buffer.DISPATCH_INDIRECT_BUFFER & 0xFFFF] = 5;
        BUFFER_TARGET_INDEX[GL.Buffer.COPY_READ_BUFFER & 0xFFFF] = 6;
        BUFFER_TARGET_INDEX[GL.Buffer.COPY_WRITE_BUFFER & 0xFFFF] = 7;
        BUFFER_TARGET_INDEX[GL.Buffer.TRANSFORM_FEEDBACK_BUFFER & 0xFFFF] = 8;
        BUFFER_TARGET_INDEX[GL.Buffer.TEXTURE_BUFFER & 0xFFFF] = 9;
        BUFFER_TARGET_INDEX[GL.Buffer.PIXEL_PACK_BUFFER & 0xFFFF] = 10;
        BUFFER_TARGET_INDEX[GL.Buffer.PIXEL_UNPACK_BUFFER & 0xFFFF] = 11;
        BUFFER_TARGET_INDEX[GL.Buffer.ATOMIC_COUNTER_BUFFER & 0xFFFF] = 12;
        BUFFER_TARGET_INDEX[GL.Buffer.QUERY_BUFFER & 0xFFFF] = 13;
        BUFFER_TARGET_INDEX[GL.Buffer.PARAMETER_BUFFER & 0xFFFF] = 14;
    }
    
    /** Buffer object storage */
    private final LongObjectMap<BufferObject> buffers;
    
    /** Indexed buffer bindings (UBO, SSBO, etc.) */
    private final IndexedBufferBinding[] uniformBufferBindings = new IndexedBufferBinding[MAX_UNIFORM_BUFFER_BINDINGS];
    private final IndexedBufferBinding[] shaderStorageBindings = new IndexedBufferBinding[MAX_SHADER_STORAGE_BINDINGS];
    private final IndexedBufferBinding[] transformFeedbackBindings = new IndexedBufferBinding[MAX_TRANSFORM_FEEDBACK_BINDINGS];
    private final IndexedBufferBinding[] atomicCounterBindings = new IndexedBufferBinding[8];
    
    /** Vertex buffer bindings (separate from target bindings) */
    private final VertexBufferBinding[] vertexBufferBindings = new VertexBufferBinding[MAX_VERTEX_BINDINGS];
    
    /**
     * Buffer object representing a GL buffer.
     */
    public static final class BufferObject {
        public long vkBuffer;
        public long vkMemory;
        public long size;
        public int usage;
        public int storageFlags;
        
        // Mapping state
        public boolean mapped = false;
        public long mapOffset;
        public long mapLength;
        public int mapAccess;
        public ByteBuffer mappedBuffer;
        public MemorySegment mappedSegment;
        
        // For buffer views (texture buffers)
        public long vkBufferView;
        public int bufferViewFormat;
        
        // Sparse buffer support
        public boolean sparse = false;
        
        // Binding tracking for validation
        public int lastBoundTarget;
        
        public void reset() {
            vkBuffer = 0;
            vkMemory = 0;
            size = 0;
            usage = 0;
            storageFlags = 0;
            mapped = false;
            mapOffset = 0;
            mapLength = 0;
            mapAccess = 0;
            mappedBuffer = null;
            mappedSegment = null;
            vkBufferView = 0;
            bufferViewFormat = 0;
            sparse = false;
            lastBoundTarget = 0;
        }
    }
    
    /**
     * Indexed buffer binding for UBO/SSBO/etc.
     */
    public static final class IndexedBufferBinding {
        public long buffer;
        public long offset;
        public long size;
        
        public void set(long buffer, long offset, long size) {
            this.buffer = buffer;
            this.offset = offset;
            this.size = size;
        }
        
        public void clear() {
            buffer = 0;
            offset = 0;
            size = 0;
        }
        
        public boolean isBound() {
            return buffer != 0;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IndexedBufferBinding that)) return false;
            return buffer == that.buffer && offset == that.offset && size == that.size;
        }
        
        @Override
        public int hashCode() {
            return Long.hashCode(buffer) * 31 * 31 + Long.hashCode(offset) * 31 + Long.hashCode(size);
        }
    }
    
    /**
     * Vertex buffer binding point state.
     */
    public static final class VertexBufferBinding {
        public long buffer;
        public long offset;
        public int stride;
        public int divisor;
        
        public void set(long buffer, long offset, int stride) {
            this.buffer = buffer;
            this.offset = offset;
            this.stride = stride;
        }
        
        public void clear() {
            buffer = 0;
            offset = 0;
            stride = 0;
            divisor = 0;
        }
        
        public boolean isBound() {
            return buffer != 0;
        }
    }
    
    // ========================================================================
    // SHADER STATE
    // ========================================================================
    
    /** Shader object storage */
    private final LongObjectMap<ShaderObject> shaders;
    
    /**
     * Shader object representing a GL shader.
     */
    public static final class ShaderObject {
        public final int type;
        public String source;
        public ByteBuffer spirvCode;
        public long vkShaderModule;
        
        public boolean compiled = false;
        public boolean compileStatus = false;
        public String infoLog = "";
        
        // Reference counting for proper deletion
        public int refCount = 0;
        public boolean deleteRequested = false;
        
        // Shader reflection data (populated after compilation)
        public ShaderReflection reflection;
        
        public ShaderObject(int type) {
            this.type = type;
        }
        
        public void incrementRef() {
            refCount++;
        }
        
        public void decrementRef() {
            refCount--;
        }
        
        public boolean canDelete() {
            return deleteRequested && refCount <= 0;
        }
    }
    
    /**
     * Shader reflection data extracted from SPIR-V.
     */
    public static final class ShaderReflection {
        public final List<UniformInfo> uniforms = new ArrayList<>();
        public final List<UniformBlockInfo> uniformBlocks = new ArrayList<>();
        public final List<ShaderStorageBlockInfo> storageBlocks = new ArrayList<>();
        public final List<AttributeInfo> attributes = new ArrayList<>();
        public final List<OutputInfo> outputs = new ArrayList<>();
        public final List<SamplerInfo> samplers = new ArrayList<>();
        public final List<ImageInfo> images = new ArrayList<>();
        
        public int[] localSize = {1, 1, 1}; // For compute shaders
    }
    
    /** Uniform variable info */
    public record UniformInfo(String name, int type, int location, int arraySize, int offset, int blockIndex) {}
    
    /** Uniform block info */
    public record UniformBlockInfo(String name, int binding, int size, List<UniformInfo> members) {}
    
    /** Shader storage block info */
    public record ShaderStorageBlockInfo(String name, int binding, int size, boolean readonly) {}
    
    /** Vertex attribute info */
    public record AttributeInfo(String name, int type, int location, int components) {}
    
    /** Fragment output info */
    public record OutputInfo(String name, int type, int location, int components) {}
    
    /** Sampler info */
    public record SamplerInfo(String name, int type, int binding, int set) {}
    
    /** Image info */
    public record ImageInfo(String name, int type, int binding, int set, int format) {}
    
    // ========================================================================
    // PROGRAM STATE
    // ========================================================================
    
    /** Program object storage */
    private final LongObjectMap<ProgramObject> programs;
    
    /** Currently bound program */
    private long currentProgram = 0;
    
    /**
     * Program object representing a linked GL program.
     */
    public static final class ProgramObject {
        public final List<Long> attachedShaders = new ArrayList<>(6);
        
        // Vulkan pipeline objects
        public long vkPipelineLayout;
        public long vkGraphicsPipeline;
        public long vkComputePipeline;
        
        // Descriptor set layouts
        public long[] vkDescriptorSetLayouts;
        public int descriptorSetCount;
        
        // Push constant range
        public int pushConstantOffset;
        public int pushConstantSize;
        public int pushConstantStages;
        
        // Link status
        public boolean linked = false;
        public boolean linkStatus = false;
        public boolean validateStatus = false;
        public String infoLog = "";
        
        // Program binary
        public ByteBuffer programBinary;
        public int binaryFormat;
        
        // Active resources
        public int activeUniforms;
        public int activeUniformMaxLength;
        public int activeAttributes;
        public int activeAttributeMaxLength;
        public int activeUniformBlocks;
        public int activeUniformBlockMaxLength;
        
        // Uniform location mapping (name -> location)
        public final Map<String, Integer> uniformLocations = new HashMap<>();
        private int nextUniformLocation = 0;
        
        // Uniform block binding mapping
        public final Map<String, Integer> uniformBlockBindings = new HashMap<>();
        
        // Shader storage block binding mapping
        public final Map<String, Integer> storageBlockBindings = new HashMap<>();
        
        // Deletion state
        public boolean deleteRequested = false;
        
        // Transform feedback state
        public String[] transformFeedbackVaryings;
        public int transformFeedbackBufferMode;
        
        // Separable program support
        public boolean separable = false;
        
        public int getUniformLocation(String name) {
            return uniformLocations.computeIfAbsent(name, k -> nextUniformLocation++);
        }
        
        public void reset() {
            attachedShaders.clear();
            vkPipelineLayout = 0;
            vkGraphicsPipeline = 0;
            vkComputePipeline = 0;
            vkDescriptorSetLayouts = null;
            descriptorSetCount = 0;
            pushConstantOffset = 0;
            pushConstantSize = 0;
            pushConstantStages = 0;
            linked = false;
            linkStatus = false;
            validateStatus = false;
            infoLog = "";
            programBinary = null;
            binaryFormat = 0;
            activeUniforms = 0;
            activeUniformMaxLength = 0;
            activeAttributes = 0;
            activeAttributeMaxLength = 0;
            activeUniformBlocks = 0;
            activeUniformBlockMaxLength = 0;
            uniformLocations.clear();
            nextUniformLocation = 0;
            uniformBlockBindings.clear();
            storageBlockBindings.clear();
            deleteRequested = false;
            transformFeedbackVaryings = null;
            transformFeedbackBufferMode = 0;
            separable = false;
        }
    }
    
    // ========================================================================
    // VERTEX ATTRIBUTE STATE
    // ========================================================================
    
    /** Vertex attribute configurations */
    private final VertexAttrib[] vertexAttribs = new VertexAttrib[MAX_VERTEX_ATTRIBS];
    
    /** Bitmask of enabled vertex attributes */
    private int enabledAttribMask = 0;
    
    /**
     * Vertex attribute configuration.
     * Designed for efficient Vulkan translation.
     */
    public static final class VertexAttrib {
        // Attribute format
        public int size = 4;
        public int type = GL.Type.FLOAT;
        public boolean normalized = false;
        public boolean integer = false;
        public boolean isLong = false;
        
        // Binding and offset
        public int binding = 0;
        public int relativeOffset = 0;
        
        // Legacy pointer (absolute offset when using bound VBO)
        public long legacyPointer = 0;
        public int legacyStride = 0;
        
        // Generic vertex attribute value (used when no buffer bound)
        public final float[] genericValue = {0.0f, 0.0f, 0.0f, 1.0f};
        public final int[] genericValueI = {0, 0, 0, 1};
        public final double[] genericValueD = {0.0, 0.0, 0.0, 1.0};
        
        // State hash for caching
        private int stateHash = 0;
        private boolean hashDirty = true;
        
        public void setPointer(int size, int type, boolean normalized, int stride, long pointer, boolean integer) {
            this.size = size;
            this.type = type;
            this.normalized = normalized;
            this.integer = integer;
            this.legacyStride = stride;
            this.legacyPointer = pointer;
            this.relativeOffset = (int) pointer; // For non-DSA path
            hashDirty = true;
        }
        
        public void setFormat(int size, int type, boolean normalized, int relativeOffset) {
            this.size = size;
            this.type = type;
            this.normalized = normalized;
            this.relativeOffset = relativeOffset;
            hashDirty = true;
        }
        
        public void setBinding(int binding) {
            this.binding = binding;
            hashDirty = true;
        }
        
        public void setGenericValue(float x, float y, float z, float w) {
            genericValue[0] = x;
            genericValue[1] = y;
            genericValue[2] = z;
            genericValue[3] = w;
        }
        
        public void setGenericValueI(int x, int y, int z, int w) {
            genericValueI[0] = x;
            genericValueI[1] = y;
            genericValueI[2] = z;
            genericValueI[3] = w;
        }
        
        public int computeHash() {
            if (!hashDirty) return stateHash;
            
            int hash = size;
            hash = 31 * hash + type;
            hash = 31 * hash + (normalized ? 1 : 0);
            hash = 31 * hash + (integer ? 2 : 0);
            hash = 31 * hash + binding;
            hash = 31 * hash + relativeOffset;
            hash = 31 * hash + legacyStride;
            
            stateHash = hash;
            hashDirty = false;
            return hash;
        }
        
        /**
         * Convert to Vulkan format.
         */
        public int toVkFormat() {
            return switch (type) {
                case GL.Type.BYTE -> switch (size) {
                    case 1 -> normalized ? VK10.VK_FORMAT_R8_SNORM : (integer ? VK10.VK_FORMAT_R8_SINT : VK10.VK_FORMAT_R8_SSCALED);
                    case 2 -> normalized ? VK10.VK_FORMAT_R8G8_SNORM : (integer ? VK10.VK_FORMAT_R8G8_SINT : VK10.VK_FORMAT_R8G8_SSCALED);
                    case 3 -> normalized ? VK10.VK_FORMAT_R8G8B8_SNORM : (integer ? VK10.VK_FORMAT_R8G8B8_SINT : VK10.VK_FORMAT_R8G8B8_SSCALED);
                    default -> normalized ? VK10.VK_FORMAT_R8G8B8A8_SNORM : (integer ? VK10.VK_FORMAT_R8G8B8A8_SINT : VK10.VK_FORMAT_R8G8B8A8_SSCALED);
                };
                case GL.Type.UNSIGNED_BYTE -> switch (size) {
                    case 1 -> normalized ? VK10.VK_FORMAT_R8_UNORM : (integer ? VK10.VK_FORMAT_R8_UINT : VK10.VK_FORMAT_R8_USCALED);
                    case 2 -> normalized ? VK10.VK_FORMAT_R8G8_UNORM : (integer ? VK10.VK_FORMAT_R8G8_UINT : VK10.VK_FORMAT_R8G8_USCALED);
                    case 3 -> normalized ? VK10.VK_FORMAT_R8G8B8_UNORM : (integer ? VK10.VK_FORMAT_R8G8B8_UINT : VK10.VK_FORMAT_R8G8B8_USCALED);
                    default -> normalized ? VK10.VK_FORMAT_R8G8B8A8_UNORM : (integer ? VK10.VK_FORMAT_R8G8B8A8_UINT : VK10.VK_FORMAT_R8G8B8A8_USCALED);
                };
                case GL.Type.SHORT -> switch (size) {
                    case 1 -> normalized ? VK10.VK_FORMAT_R16_SNORM : (integer ? VK10.VK_FORMAT_R16_SINT : VK10.VK_FORMAT_R16_SSCALED);
                    case 2 -> normalized ? VK10.VK_FORMAT_R16G16_SNORM : (integer ? VK10.VK_FORMAT_R16G16_SINT : VK10.VK_FORMAT_R16G16_SSCALED);
                    case 3 -> normalized ? VK10.VK_FORMAT_R16G16B16_SNORM : (integer ? VK10.VK_FORMAT_R16G16B16_SINT : VK10.VK_FORMAT_R16G16B16_SSCALED);
                    default -> normalized ? VK10.VK_FORMAT_R16G16B16A16_SNORM : (integer ? VK10.VK_FORMAT_R16G16B16A16_SINT : VK10.VK_FORMAT_R16G16B16A16_SSCALED);
                };
                case GL.Type.UNSIGNED_SHORT -> switch (size) {
                    case 1 -> normalized ? VK10.VK_FORMAT_R16_UNORM : (integer ? VK10.VK_FORMAT_R16_UINT : VK10.VK_FORMAT_R16_USCALED);
                    case 2 -> normalized ? VK10.VK_FORMAT_R16G16_UNORM : (integer ? VK10.VK_FORMAT_R16G16_UINT : VK10.VK_FORMAT_R16G16_USCALED);
                    case 3 -> normalized ? VK10.VK_FORMAT_R16G16B16_UNORM : (integer ? VK10.VK_FORMAT_R16G16B16_UINT : VK10.VK_FORMAT_R16G16B16_USCALED);
                    default -> normalized ? VK10.VK_FORMAT_R16G16B16A16_UNORM : (integer ? VK10.VK_FORMAT_R16G16B16A16_UINT : VK10.VK_FORMAT_R16G16B16A16_USCALED);
                };
                case GL.Type.INT -> switch (size) {
                    case 1 -> VK10.VK_FORMAT_R32_SINT;
                    case 2 -> VK10.VK_FORMAT_R32G32_SINT;
                    case 3 -> VK10.VK_FORMAT_R32G32B32_SINT;
                    default -> VK10.VK_FORMAT_R32G32B32A32_SINT;
                };
                case GL.Type.UNSIGNED_INT -> switch (size) {
                    case 1 -> VK10.VK_FORMAT_R32_UINT;
                    case 2 -> VK10.VK_FORMAT_R32G32_UINT;
                    case 3 -> VK10.VK_FORMAT_R32G32B32_UINT;
                    default -> VK10.VK_FORMAT_R32G32B32A32_UINT;
                };
                case GL.Type.HALF_FLOAT -> switch (size) {
                    case 1 -> VK10.VK_FORMAT_R16_SFLOAT;
                    case 2 -> VK10.VK_FORMAT_R16G16_SFLOAT;
                    case 3 -> VK10.VK_FORMAT_R16G16B16_SFLOAT;
                    default -> VK10.VK_FORMAT_R16G16B16A16_SFLOAT;
                };
                case GL.Type.FLOAT -> switch (size) {
                    case 1 -> VK10.VK_FORMAT_R32_SFLOAT;
                    case 2 -> VK10.VK_FORMAT_R32G32_SFLOAT;
                    case 3 -> VK10.VK_FORMAT_R32G32B32_SFLOAT;
                    default -> VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
                };
                case GL.Type.DOUBLE -> switch (size) {
                    case 1 -> VK10.VK_FORMAT_R64_SFLOAT;
                    case 2 -> VK10.VK_FORMAT_R64G64_SFLOAT;
                    case 3 -> VK10.VK_FORMAT_R64G64B64_SFLOAT;
                    default -> VK10.VK_FORMAT_R64G64B64A64_SFLOAT;
                };
                case GL.Type.INT_2_10_10_10_REV -> normalized ? VK10.VK_FORMAT_A2B10G10R10_SNORM_PACK32 : VK10.VK_FORMAT_A2B10G10R10_SINT_PACK32;
                case GL.Type.UNSIGNED_INT_2_10_10_10_REV -> normalized ? VK10.VK_FORMAT_A2B10G10R10_UNORM_PACK32 : VK10.VK_FORMAT_A2B10G10R10_UINT_PACK32;
                case GL.Type.UNSIGNED_INT_10F_11F_11F_REV -> VK10.VK_FORMAT_B10G11R11_UFLOAT_PACK32;
                default -> VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
            };
        }
        
        /**
         * Get size in bytes for this attribute.
         */
        public int getByteSize() {
            int componentSize = switch (type) {
                case GL.Type.BYTE, GL.Type.UNSIGNED_BYTE -> 1;
                case GL.Type.SHORT, GL.Type.UNSIGNED_SHORT, GL.Type.HALF_FLOAT -> 2;
                case GL.Type.INT, GL.Type.UNSIGNED_INT, GL.Type.FLOAT, GL.Type.FIXED,
                     GL.Type.INT_2_10_10_10_REV, GL.Type.UNSIGNED_INT_2_10_10_10_REV,
                     GL.Type.UNSIGNED_INT_10F_11F_11F_REV -> 4;
                case GL.Type.DOUBLE -> 8;
                default -> 4;
            };
            
            // Packed formats are always 4 bytes total
            if (type == GL.Type.INT_2_10_10_10_REV || 
                type == GL.Type.UNSIGNED_INT_2_10_10_10_REV ||
                type == GL.Type.UNSIGNED_INT_10F_11F_11F_REV) {
                return 4;
            }
            
            return componentSize * size;
        }
        
        public void reset() {
            size = 4;
            type = GL.Type.FLOAT;
            normalized = false;
            integer = false;
            isLong = false;
            binding = 0;
            relativeOffset = 0;
            legacyPointer = 0;
            legacyStride = 0;
            genericValue[0] = 0.0f;
            genericValue[1] = 0.0f;
            genericValue[2] = 0.0f;
            genericValue[3] = 1.0f;
            genericValueI[0] = 0;
            genericValueI[1] = 0;
            genericValueI[2] = 0;
            genericValueI[3] = 1;
            hashDirty = true;
        }
    }
    
    // ========================================================================
    // LIGHTWEIGHT LONG-TO-OBJECT MAP
    // ========================================================================
    
    /**
     * High-performance primitive long key to object value map.
     * Optimized for the access patterns in state tracking.
     */
    public static final class LongObjectMap<V> {
        private static final int DEFAULT_CAPACITY = 256;
        private static final float LOAD_FACTOR = 0.75f;
        private static final long NULL_KEY = 0L;
        
        private long[] keys;
        private Object[] values;
        private int size;
        private int threshold;
        private int mask;
        
        public LongObjectMap() {
            this(DEFAULT_CAPACITY);
        }
        
        public LongObjectMap(int initialCapacity) {
            int capacity = nextPowerOfTwo(initialCapacity);
            keys = new long[capacity];
            values = new Object[capacity];
            mask = capacity - 1;
            threshold = (int) (capacity * LOAD_FACTOR);
        }
        
        @SuppressWarnings("unchecked")
        public V get(long key) {
            if (key == NULL_KEY) return null;
            
            int index = hash(key) & mask;
            while (keys[index] != NULL_KEY) {
                if (keys[index] == key) {
                    return (V) values[index];
                }
                index = (index + 1) & mask;
            }
            return null;
        }
        
        public V put(long key, V value) {
            if (key == NULL_KEY) {
                throw new IllegalArgumentException("Key cannot be 0");
            }
            
            if (size >= threshold) {
                resize();
            }
            
            return putInternal(key, value);
        }
        
        @SuppressWarnings("unchecked")
        private V putInternal(long key, V value) {
            int index = hash(key) & mask;
            while (keys[index] != NULL_KEY) {
                if (keys[index] == key) {
                    V old = (V) values[index];
                    values[index] = value;
                    return old;
                }
                index = (index + 1) & mask;
            }
            
            keys[index] = key;
            values[index] = value;
            size++;
            return null;
        }
        
        @SuppressWarnings("unchecked")
        public V remove(long key) {
            if (key == NULL_KEY) return null;
            
            int index = hash(key) & mask;
            while (keys[index] != NULL_KEY) {
                if (keys[index] == key) {
                    V old = (V) values[index];
                    keys[index] = NULL_KEY;
                    values[index] = null;
                    size--;
                    rehashFrom(index);
                    return old;
                }
                index = (index + 1) & mask;
            }
            return null;
        }
        
        private void rehashFrom(int start) {
            int index = (start + 1) & mask;
            while (keys[index] != NULL_KEY) {
                long key = keys[index];
                Object value = values[index];
                keys[index] = NULL_KEY;
                values[index] = null;
                size--;
                putInternal(key, (V) value);
                index = (index + 1) & mask;
            }
        }
        
        public boolean containsKey(long key) {
            return get(key) != null;
        }
        
        public int size() {
            return size;
        }
        
        public boolean isEmpty() {
            return size == 0;
        }
        
        public void clear() {
            Arrays.fill(keys, NULL_KEY);
            Arrays.fill(values, null);
            size = 0;
        }
        
        public void forEach(LongObjectConsumer<V> consumer) {
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] != NULL_KEY) {
                    consumer.accept(keys[i], (V) values[i]);
                }
            }
        }
        
        public void forEachValue(Consumer<V> consumer) {
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] != NULL_KEY) {
                    consumer.accept((V) values[i]);
                }
            }
        }
        
        public void forEachKey(LongConsumer consumer) {
            for (int i = 0; i < keys.length; i++) {
                if (keys[i] != NULL_KEY) {
                    consumer.accept(keys[i]);
                }
            }
        }
        
        private void resize() {
            int newCapacity = keys.length * 2;
            long[] oldKeys = keys;
            Object[] oldValues = values;
            
            keys = new long[newCapacity];
            values = new Object[newCapacity];
            mask = newCapacity - 1;
            threshold = (int) (newCapacity * LOAD_FACTOR);
            size = 0;
            
            for (int i = 0; i < oldKeys.length; i++) {
                if (oldKeys[i] != NULL_KEY) {
                    putInternal(oldKeys[i], (V) oldValues[i]);
                }
            }
        }
        
        private static int hash(long key) {
            key = key ^ (key >>> 33);
            key = key * 0xff51afd7ed558ccdL;
            key = key ^ (key >>> 33);
            key = key * 0xc4ceb9fe1a85ec53L;
            key = key ^ (key >>> 33);
            return (int) key;
        }
        
        private static int nextPowerOfTwo(int value) {
            int highestOneBit = Integer.highestOneBit(value);
            if (value == highestOneBit) return value;
            return highestOneBit << 1;
        }
        
        @FunctionalInterface
        public interface LongObjectConsumer<V> {
            void accept(long key, V value);
        }
    }
    
    // ========================================================================
    // CONSTRUCTOR AND INITIALIZATION
    // ========================================================================
    
    /**
     * Create a new VulkanState with default configuration.
     * Thread safety is disabled for maximum performance.
     */
    public VulkanState() {
        this(false);
    }
    
    /**
     * Create a new VulkanState with optional thread safety.
     *
     * @param threadSafe If true, enables thread-safe state access using StampedLock
     */
    public VulkanState(boolean threadSafe) {
        this.threadSafe = threadSafe;
        this.stateLock = threadSafe ? new StampedLock() : null;
        
        // Initialize object maps
        this.textures = new LongObjectMap<>(INITIAL_OBJECT_MAP_CAPACITY);
        this.samplers = new LongObjectMap<>(64);
        this.buffers = new LongObjectMap<>(INITIAL_OBJECT_MAP_CAPACITY);
        this.shaders = new LongObjectMap<>(64);
        this.programs = new LongObjectMap<>(32);
        
        // Initialize indexed buffer bindings
        for (int i = 0; i < uniformBufferBindings.length; i++) {
            uniformBufferBindings[i] = new IndexedBufferBinding();
        }
        for (int i = 0; i < shaderStorageBindings.length; i++) {
            shaderStorageBindings[i] = new IndexedBufferBinding();
        }
        for (int i = 0; i < transformFeedbackBindings.length; i++) {
            transformFeedbackBindings[i] = new IndexedBufferBinding();
        }
        for (int i = 0; i < atomicCounterBindings.length; i++) {
            atomicCounterBindings[i] = new IndexedBufferBinding();
        }
        
        // Initialize vertex buffer bindings
        for (int i = 0; i < vertexBufferBindings.length; i++) {
            vertexBufferBindings[i] = new VertexBufferBinding();
        }
        
        // Initialize vertex attributes
        for (int i = 0; i < vertexAttribs.length; i++) {
            vertexAttribs[i] = new VertexAttrib();
        }
        
        // Set initial state
        reset();
    }
    
    // ========================================================================
    // DIRTY FLAG MANAGEMENT
    // ========================================================================
    
    /**
     * Mark specific state as dirty.
     * Uses atomic OR for thread safety when enabled.
     */
    public void markDirty(long flags) {
        long current;
        long updated;
        do {
            current = (long) DIRTY_FLAGS_HANDLE.getVolatile(this);
            updated = current | flags;
        } while (!DIRTY_FLAGS_HANDLE.compareAndSet(this, current, updated));
        
        // Invalidate cached hashes if relevant state changed
        if ((flags & DirtyFlags.ALL_PIPELINE_STATE) != 0) {
            pipelineHashValid = false;
        }
        if ((flags & DirtyFlags.VERTEX_INPUT) != 0) {
            vertexInputHashValid = false;
        }
    }
    
    /**
     * Clear specific dirty flags.
     * Returns the flags that were set before clearing.
     */
    public long clearDirty(long flags) {
        long current;
        long updated;
        do {
            current = (long) DIRTY_FLAGS_HANDLE.getVolatile(this);
            updated = current & ~flags;
        } while (!DIRTY_FLAGS_HANDLE.compareAndSet(this, current, updated));
        return current & flags;
    }
    
    /**
     * Check if any of the specified flags are dirty.
     */
    public boolean isDirty(long flags) {
        return ((long) DIRTY_FLAGS_HANDLE.getVolatile(this) & flags) != 0;
    }
    
    /**
     * Get all dirty flags.
     */
    public long getDirtyFlags() {
        return (long) DIRTY_FLAGS_HANDLE.getVolatile(this);
    }
    
    /**
     * Increment state version for cache invalidation.
     */
    private void incrementVersion() {
        STATE_VERSION_HANDLE.getAndAdd(this, 1L);
    }
    
    /**
     * Get current state version.
     */
    public long getStateVersion() {
        return (long) STATE_VERSION_HANDLE.getVolatile(this);
    }
    
    // ========================================================================
    // TEXTURE STATE METHODS
    // ========================================================================
    
    /**
     * Generate a new texture ID.
     */
    public long genTexture() {
        return nextTextureId.getAndIncrement();
    }
    
    /**
     * Generate multiple texture IDs.
     */
    public void genTextures(int count, long[] ids) {
        for (int i = 0; i < count; i++) {
            ids[i] = nextTextureId.getAndIncrement();
        }
    }
    
    /**
     * Register a new texture object with Vulkan handles.
     */
    public long createTexture(long vkImage, long vkMemory, long vkImageView, long vkSampler) {
        TextureObject tex = new TextureObject();
        tex.vkImage = vkImage;
        tex.vkMemory = vkMemory;
        tex.vkImageView = vkImageView;
        tex.vkSampler = vkSampler;
        
        long id = nextTextureId.getAndIncrement();
        textures.put(id, tex);
        return id;
    }
    
    /**
     * Get texture object by ID.
     */
    public TextureObject getTexture(long id) {
        return textures.get(id);
    }
    
    /**
     * Delete a texture.
     */
    public TextureObject deleteTexture(long id) {
        // Unbind from all units
        for (int i = 0; i < MAX_TEXTURE_UNITS; i++) {
            if (boundTextures[i] == id) {
                boundTextures[i] = 0;
            }
        }
        markDirty(DirtyFlags.TEXTURE_BINDINGS);
        return textures.remove(id);
    }
    
    /**
     * Set active texture unit.
     * @param unit GL texture unit (GL_TEXTURE0 + n)
     */
    public void activeTexture(int unit) {
        activeTextureUnit = unit - GL.Texture.TEXTURE0;
    }
    
    /**
     * Get active texture unit index (0-based).
     */
    public int getActiveTextureUnit() {
        return activeTextureUnit;
    }
    
    /**
     * Bind texture to current active unit.
     */
    public void bindTexture(int target, long texture) {
        if (boundTextures[activeTextureUnit] != texture) {
            boundTextures[activeTextureUnit] = texture;
            
            TextureObject tex = textures.get(texture);
            if (tex != null && tex.target == 0) {
                tex.target = target;
            }
            
            markDirty(DirtyFlags.TEXTURE_BINDINGS);
        }
    }
    
    /**
     * Bind texture to specific unit (DSA style).
     */
    public void bindTextureUnit(int unit, long texture) {
        if (unit >= 0 && unit < MAX_TEXTURE_UNITS && boundTextures[unit] != texture) {
            boundTextures[unit] = texture;
            markDirty(DirtyFlags.TEXTURE_BINDINGS);
        }
    }
    
    /**
     * Get bound texture at unit.
     */
    public long getBoundTexture(int unit) {
        return (unit >= 0 && unit < MAX_TEXTURE_UNITS) ? boundTextures[unit] : 0;
    }
    
    /**
     * Get bound texture at current active unit.
     */
    public long getBoundTexture() {
        return boundTextures[activeTextureUnit];
    }
    
    /**
     * Get all bound textures.
     * @param dest Destination array (must be at least MAX_TEXTURE_UNITS)
     */
    public void getBoundTextures(long[] dest) {
        System.arraycopy(boundTextures, 0, dest, 0, MAX_TEXTURE_UNITS);
    }
    
    /**
     * Bind separate sampler to unit.
     */
    public void bindSampler(int unit, long sampler) {
        if (unit >= 0 && unit < MAX_TEXTURE_UNITS && boundSamplers[unit] != sampler) {
            boundSamplers[unit] = sampler;
            markDirty(DirtyFlags.TEXTURE_BINDINGS);
        }
    }
    
    /**
     * Get bound sampler at unit.
     */
    public long getBoundSampler(int unit) {
        return (unit >= 0 && unit < MAX_TEXTURE_UNITS) ? boundSamplers[unit] : 0;
    }
    
    /**
     * Create a separate sampler object.
     */
    public long createSampler() {
        SamplerObject sampler = new SamplerObject();
        long id = nextSamplerId.getAndIncrement();
        samplers.put(id, sampler);
        return id;
    }
    
    /**
     * Get sampler object.
     */
    public SamplerObject getSampler(long id) {
        return samplers.get(id);
    }
    
    /**
     * Delete sampler.
     */
    public SamplerObject deleteSampler(long id) {
        for (int i = 0; i < MAX_TEXTURE_UNITS; i++) {
            if (boundSamplers[i] == id) {
                boundSamplers[i] = 0;
            }
        }
        return samplers.remove(id);
    }
    
    // ========================================================================
    // BUFFER STATE METHODS  
    // ========================================================================
    
    /**
     * Generate a new buffer ID.
     */
    public long genBuffer() {
        return nextBufferId.getAndIncrement();
    }
    
    /**
     * Generate multiple buffer IDs.
     */
    public void genBuffers(int count, long[] ids) {
        for (int i = 0; i < count; i++) {
            ids[i] = nextBufferId.getAndIncrement();
        }
    }
    
    /**
     * Create and register a buffer with Vulkan handles.
     */
    public long createBuffer(long vkBuffer, long vkMemory, long size) {
        BufferObject buf = new BufferObject();
        buf.vkBuffer = vkBuffer;
        buf.vkMemory = vkMemory;
        buf.size = size;
        
        long id = nextBufferId.getAndIncrement();
        buffers.put(id, buf);
        return id;
    }
    
    /**
     * Get buffer object.
     */
    public BufferObject getBuffer(long id) {
        return buffers.get(id);
    }
    
    /**
     * Delete buffer.
     */
    public BufferObject deleteBuffer(long id) {
        // Unbind from all targets
        for (int i = 0; i < boundBuffersByTarget.length; i++) {
            if (boundBuffersByTarget[i] == id) {
                boundBuffersByTarget[i] = 0;
            }
        }
        
        // Unbind from indexed bindings
        for (IndexedBufferBinding binding : uniformBufferBindings) {
            if (binding.buffer == id) binding.clear();
        }
        for (IndexedBufferBinding binding : shaderStorageBindings) {
            if (binding.buffer == id) binding.clear();
        }
        for (IndexedBufferBinding binding : transformFeedbackBindings) {
            if (binding.buffer == id) binding.clear();
        }
        for (IndexedBufferBinding binding : atomicCounterBindings) {
            if (binding.buffer == id) binding.clear();
        }
        
        // Unbind from vertex buffer bindings
        for (VertexBufferBinding binding : vertexBufferBindings) {
            if (binding.buffer == id) binding.clear();
        }
        
        markDirty(DirtyFlags.VERTEX_BINDINGS | DirtyFlags.UNIFORM_BUFFER_BINDINGS | 
                  DirtyFlags.SHADER_STORAGE_BINDINGS);
        
        return buffers.remove(id);
    }
    
    /**
     * Bind buffer to target.
     */
    public void bindBuffer(int target, long buffer) {
        int index = getBufferTargetIndex(target);
        if (index >= 0 && boundBuffersByTarget[index] != buffer) {
            boundBuffersByTarget[index] = buffer;
            
            if (buffer != 0) {
                BufferObject buf = buffers.get(buffer);
                if (buf != null) {
                    buf.lastBoundTarget = target;
                }
            }
            
            // Mark appropriate dirty flag
            if (target == GL.Buffer.ELEMENT_ARRAY_BUFFER) {
                markDirty(DirtyFlags.ELEMENT_BUFFER);
            } else if (target == GL.Buffer.ARRAY_BUFFER) {
                markDirty(DirtyFlags.VERTEX_BINDINGS);
            }
        }
    }
    
    /**
     * Get bound buffer for target.
     */
    public long getBoundBuffer(int target) {
        int index = getBufferTargetIndex(target);
        return (index >= 0) ? boundBuffersByTarget[index] : 0;
    }
    
    /**
     * Convert GL buffer target to internal index.
     */
    private static int getBufferTargetIndex(int target) {
        int masked = target & 0xFFFF;
        return (masked < BUFFER_TARGET_INDEX.length) ? BUFFER_TARGET_INDEX[masked] : -1;
    }
    
    /**
     * Bind buffer to indexed binding point (UBO, SSBO, etc.).
     */
    public void bindBufferBase(int target, int index, long buffer) {
        IndexedBufferBinding[] bindings = getIndexedBindingsArray(target);
        if (bindings != null && index >= 0 && index < bindings.length) {
            BufferObject buf = buffers.get(buffer);
            long size = (buf != null) ? buf.size : 0;
            
            IndexedBufferBinding binding = bindings[index];
            if (binding.buffer != buffer || binding.offset != 0 || binding.size != size) {
                binding.set(buffer, 0, size);
                markDirtyForIndexedTarget(target);
            }
        }
        
        // Also bind to the target binding point
        bindBuffer(target, buffer);
    }
    
    /**
     * Bind buffer range to indexed binding point.
     */
    public void bindBufferRange(int target, int index, long buffer, long offset, long size) {
        IndexedBufferBinding[] bindings = getIndexedBindingsArray(target);
        if (bindings != null && index >= 0 && index < bindings.length) {
            IndexedBufferBinding binding = bindings[index];
            if (binding.buffer != buffer || binding.offset != offset || binding.size != size) {
                binding.set(buffer, offset, size);
                markDirtyForIndexedTarget(target);
            }
        }
        
        bindBuffer(target, buffer);
    }
    
    /**
     * Bind multiple buffers to consecutive indexed binding points.
     */
    public void bindBuffersBase(int target, int first, int count, long[] bufferIds) {
        IndexedBufferBinding[] bindings = getIndexedBindingsArray(target);
        if (bindings == null) return;
        
        boolean changed = false;
        for (int i = 0; i < count && (first + i) < bindings.length; i++) {
            long buffer = (bufferIds != null && i < bufferIds.length) ? bufferIds[i] : 0;
            BufferObject buf = buffers.get(buffer);
            long size = (buf != null) ? buf.size : 0;
            
            IndexedBufferBinding binding = bindings[first + i];
            if (binding.buffer != buffer || binding.offset != 0 || binding.size != size) {
                binding.set(buffer, 0, size);
                changed = true;
            }
        }
        
        if (changed) {
            markDirtyForIndexedTarget(target);
        }
    }
    
    /**
     * Bind multiple buffer ranges to consecutive indexed binding points.
     */
    public void bindBuffersRange(int target, int first, int count, 
                                  long[] bufferIds, long[] offsets, long[] sizes) {
        IndexedBufferBinding[] bindings = getIndexedBindingsArray(target);
        if (bindings == null) return;
        
        boolean changed = false;
        for (int i = 0; i < count && (first + i) < bindings.length; i++) {
            long buffer = (bufferIds != null && i < bufferIds.length) ? bufferIds[i] : 0;
            long offset = (offsets != null && i < offsets.length) ? offsets[i] : 0;
            long size = (sizes != null && i < sizes.length) ? sizes[i] : 0;
            
            IndexedBufferBinding binding = bindings[first + i];
            if (binding.buffer != buffer || binding.offset != offset || binding.size != size) {
                binding.set(buffer, offset, size);
                changed = true;
            }
        }
        
        if (changed) {
            markDirtyForIndexedTarget(target);
        }
    }
    
    /**
     * Get indexed buffer binding.
     */
    public IndexedBufferBinding getIndexedBufferBinding(int target, int index) {
        IndexedBufferBinding[] bindings = getIndexedBindingsArray(target);
        if (bindings != null && index >= 0 && index < bindings.length) {
            return bindings[index];
        }
        return null;
    }
    
    /**
     * Get the indexed bindings array for a target.
     */
    private IndexedBufferBinding[] getIndexedBindingsArray(int target) {
        return switch (target) {
            case GL.Buffer.UNIFORM_BUFFER -> uniformBufferBindings;
            case GL.Buffer.SHADER_STORAGE_BUFFER -> shaderStorageBindings;
            case GL.Buffer.TRANSFORM_FEEDBACK_BUFFER -> transformFeedbackBindings;
            case GL.Buffer.ATOMIC_COUNTER_BUFFER -> atomicCounterBindings;
            default -> null;
        };
    }
    
    /**
     * Mark dirty for indexed target.
     */
    private void markDirtyForIndexedTarget(int target) {
        switch (target) {
            case GL.Buffer.UNIFORM_BUFFER -> markDirty(DirtyFlags.UNIFORM_BUFFER_BINDINGS);
            case GL.Buffer.SHADER_STORAGE_BUFFER -> markDirty(DirtyFlags.SHADER_STORAGE_BINDINGS);
            default -> markDirty(DirtyFlags.DESCRIPTORS);
        }
    }
    
    // ---- Vertex Buffer Binding Methods ----
    
    /**
     * Bind vertex buffer to binding point (DSA style).
     */
    public void bindVertexBuffer(int binding, long buffer, long offset, int stride) {
        if (binding >= 0 && binding < MAX_VERTEX_BINDINGS) {
            VertexBufferBinding vbb = vertexBufferBindings[binding];
            if (vbb.buffer != buffer || vbb.offset != offset || vbb.stride != stride) {
                vbb.set(buffer, offset, stride);
                markDirty(DirtyFlags.VERTEX_BINDINGS);
            }
        }
    }
    
    /**
     * Bind multiple vertex buffers.
     */
    public void bindVertexBuffers(int first, int count, long[] bufferIds, long[] offsets, int[] strides) {
        boolean changed = false;
        for (int i = 0; i < count && (first + i) < MAX_VERTEX_BINDINGS; i++) {
            long buffer = (bufferIds != null && i < bufferIds.length) ? bufferIds[i] : 0;
            long offset = (offsets != null && i < offsets.length) ? offsets[i] : 0;
            int stride = (strides != null && i < strides.length) ? strides[i] : 0;
            
            VertexBufferBinding vbb = vertexBufferBindings[first + i];
            if (vbb.buffer != buffer || vbb.offset != offset || vbb.stride != stride) {
                vbb.set(buffer, offset, stride);
                changed = true;
            }
        }
        
        if (changed) {
            markDirty(DirtyFlags.VERTEX_BINDINGS);
        }
    }
    
    /**
     * Set vertex binding divisor for instancing.
     */
    public void vertexBindingDivisor(int binding, int divisor) {
        if (binding >= 0 && binding < MAX_VERTEX_BINDINGS) {
            if (vertexBufferBindings[binding].divisor != divisor) {
                vertexBufferBindings[binding].divisor = divisor;
                markDirty(DirtyFlags.VERTEX_INPUT);
            }
        }
    }
    
    /**
     * Get vertex buffer binding.
     */
    public VertexBufferBinding getVertexBufferBinding(int binding) {
        return (binding >= 0 && binding < MAX_VERTEX_BINDINGS) ? vertexBufferBindings[binding] : null;
    }
    
    /**
     * Get count of active vertex buffer bindings.
     */
    public int getActiveVertexBindingCount() {
        int max = 0;
        for (int i = 0; i < MAX_VERTEX_BINDINGS; i++) {
            if (vertexBufferBindings[i].isBound()) {
                max = i + 1;
            }
        }
        // Also check vertex attribs for their bindings
        for (int i = 0; i < MAX_VERTEX_ATTRIBS; i++) {
            if ((enabledAttribMask & (1 << i)) != 0) {
                max = Math.max(max, vertexAttribs[i].binding + 1);
            }
        }
        return max;
    }
    
    // ========================================================================
    // SHADER STATE METHODS
    // ========================================================================
    
    /**
     * Create a new shader object.
     */
    public long createShader(int type) {
        ShaderObject shader = new ShaderObject(type);
        long id = nextShaderId.getAndIncrement();
        shaders.put(id, shader);
        return id;
    }
    
    /**
     * Get shader object.
     */
    public ShaderObject getShader(long id) {
        return shaders.get(id);
    }
    
    /**
     * Set shader source.
     */
    public void shaderSource(long shader, String source) {
        ShaderObject s = shaders.get(shader);
        if (s != null) {
            s.source = source;
            s.compiled = false;
            s.spirvCode = null;
        }
    }
    
    /**
     * Set shader source from multiple strings.
     */
    public void shaderSource(long shader, String[] sources) {
        ShaderObject s = shaders.get(shader);
        if (s != null) {
            StringBuilder combined = new StringBuilder();
            for (String src : sources) {
                if (src != null) combined.append(src);
            }
            s.source = combined.toString();
            s.compiled = false;
            s.spirvCode = null;
        }
    }
    
    /**
     * Set pre-compiled SPIR-V binary for shader.
     */
    public void shaderBinary(long shader, ByteBuffer spirv) {
        ShaderObject s = shaders.get(shader);
        if (s != null) {
            s.spirvCode = spirv;
            s.compiled = true;
            s.compileStatus = true;
        }
    }
    
    /**
     * Mark shader as compiled with status.
     */
    public void setShaderCompileStatus(long shader, boolean success, String infoLog) {
        ShaderObject s = shaders.get(shader);
        if (s != null) {
            s.compiled = true;
            s.compileStatus = success;
            s.infoLog = infoLog != null ? infoLog : "";
        }
    }
    
    /**
     * Delete shader (or mark for deletion if attached).
     */
    public ShaderObject deleteShader(long id) {
        ShaderObject shader = shaders.get(id);
        if (shader != null) {
            shader.deleteRequested = true;
            if (shader.canDelete()) {
                return shaders.remove(id);
            }
        }
        return null;
    }
    
    /**
     * Get shader type as Vulkan stage flag.
     */
    public static int shaderTypeToVkStage(int glType) {
        return switch (glType) {
            case GL.Shader.VERTEX_SHADER -> VK10.VK_SHADER_STAGE_VERTEX_BIT;
            case GL.Shader.FRAGMENT_SHADER -> VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
            case GL.Shader.GEOMETRY_SHADER -> VK10.VK_SHADER_STAGE_GEOMETRY_BIT;
            case GL.Shader.TESS_CONTROL_SHADER -> VK10.VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT;
            case GL.Shader.TESS_EVALUATION_SHADER -> VK10.VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT;
            case GL.Shader.COMPUTE_SHADER -> VK10.VK_SHADER_STAGE_COMPUTE_BIT;
            default -> 0;
        };
    }
    
    // ========================================================================
    // PROGRAM STATE METHODS
    // ========================================================================
    
    /**
     * Create a new program object.
     */
    public long createProgram() {
        ProgramObject program = new ProgramObject();
        long id = nextProgramId.getAndIncrement();
        programs.put(id, program);
        return id;
    }
    
    /**
     * Get program object.
     */
    public ProgramObject getProgram(long id) {
        return programs.get(id);
    }
    
    /**
     * Attach shader to program.
     */
    public void attachShader(long program, long shader) {
        ProgramObject prog = programs.get(program);
        ShaderObject shad = shaders.get(shader);
        
        if (prog != null && shad != null && !prog.attachedShaders.contains(shader)) {
            prog.attachedShaders.add(shader);
            shad.incrementRef();
        }
    }
    
    /**
     * Detach shader from program.
     */
    public void detachShader(long program, long shader) {
        ProgramObject prog = programs.get(program);
        ShaderObject shad = shaders.get(shader);
        
        if (prog != null && prog.attachedShaders.remove(shader)) {
            if (shad != null) {
                shad.decrementRef();
                if (shad.canDelete()) {
                    shaders.remove(shader);
                }
            }
        }
    }
    
    /**
     * Set program link status.
     */
    public void setProgramLinkStatus(long program, boolean success, String infoLog) {
        ProgramObject prog = programs.get(program);
        if (prog != null) {
            prog.linked = true;
            prog.linkStatus = success;
            prog.infoLog = infoLog != null ? infoLog : "";
        }
    }
    
    /**
     * Use program (bind for rendering).
     */
    public void useProgram(long program) {
        if (currentProgram != program) {
            currentProgram = program;
            markDirty(DirtyFlags.PROGRAM | DirtyFlags.DESCRIPTORS | DirtyFlags.PUSH_CONSTANTS);
        }
    }
    
    /**
     * Get current program.
     */
    public long getCurrentProgram() {
        return currentProgram;
    }
    
    /**
     * Get current program object.
     */
    public ProgramObject getCurrentProgramObject() {
        return programs.get(currentProgram);
    }
    
    /**
     * Delete program.
     */
    public ProgramObject deleteProgram(long id) {
        ProgramObject prog = programs.get(id);
        if (prog != null) {
            prog.deleteRequested = true;
            
            // Detach all shaders
            for (long shaderId : prog.attachedShaders) {
                ShaderObject shader = shaders.get(shaderId);
                if (shader != null) {
                    shader.decrementRef();
                    if (shader.canDelete()) {
                        shaders.remove(shaderId);
                    }
                }
            }
            prog.attachedShaders.clear();
            
            if (currentProgram == id) {
                currentProgram = 0;
                markDirty(DirtyFlags.PROGRAM);
            }
            
            return programs.remove(id);
        }
        return null;
    }
    
    /**
     * Get uniform location for current program.
     */
    public int getUniformLocation(String name) {
        ProgramObject prog = programs.get(currentProgram);
        return (prog != null) ? prog.getUniformLocation(name) : -1;
    }
    
    /**
     * Get uniform location for specified program.
     */
    public int getUniformLocation(long program, String name) {
        ProgramObject prog = programs.get(program);
        return (prog != null) ? prog.getUniformLocation(name) : -1;
    }
    
    /**
     * Get uniform block index.
     */
    public int getUniformBlockIndex(long program, String name) {
        ProgramObject prog = programs.get(program);
        if (prog != null) {
            Integer binding = prog.uniformBlockBindings.get(name);
            return (binding != null) ? binding : -1;
        }
        return -1;
    }
    
    /**
     * Set uniform block binding.
     */
    public void uniformBlockBinding(long program, int uniformBlockIndex, int uniformBlockBinding) {
        ProgramObject prog = programs.get(program);
        if (prog != null) {
            // Find block name by index and update binding
            for (Map.Entry<String, Integer> entry : prog.uniformBlockBindings.entrySet()) {
                if (entry.getValue() == uniformBlockIndex) {
                    entry.setValue(uniformBlockBinding);
                    break;
                }
            }
            markDirty(DirtyFlags.UNIFORM_BUFFER_BINDINGS);
        }
    }
    
    /**
     * Get graphics pipeline for current state.
     */
    public long getGraphicsPipeline() {
        ProgramObject prog = programs.get(currentProgram);
        return (prog != null && prog.linkStatus) ? prog.vkGraphicsPipeline : 0;
    }
    
    /**
     * Get compute pipeline for current state.
     */
    public long getComputePipeline() {
        ProgramObject prog = programs.get(currentProgram);
        return (prog != null && prog.linkStatus) ? prog.vkComputePipeline : 0;
    }
    
    /**
     * Get pipeline layout for current program.
     */
    public long getPipelineLayout() {
        ProgramObject prog = programs.get(currentProgram);
        return (prog != null) ? prog.vkPipelineLayout : 0;
    }
    
    // ========================================================================
    // VERTEX ATTRIBUTE STATE METHODS
    // ========================================================================
    
    /**
     * Set vertex attribute pointer (legacy style).
     */
    public void vertexAttribPointer(int index, int size, int type, boolean normalized, 
                                     int stride, long pointer) {
        if (index >= 0 && index < MAX_VERTEX_ATTRIBS) {
            VertexAttrib attrib = vertexAttribs[index];
            attrib.setPointer(size, type, normalized, stride, pointer, false);
            
            // Associate with current VBO
            long vbo = getBoundBuffer(GL.Buffer.ARRAY_BUFFER);
            if (vbo != 0) {
                // In legacy mode, binding index equals attribute index
                attrib.binding = index;
                vertexBufferBindings[index].set(vbo, 0, stride);
            }
            
            markDirty(DirtyFlags.VERTEX_INPUT | DirtyFlags.VERTEX_BINDINGS);
        }
    }
    
    /**
     * Set vertex attribute pointer for integer data.
     */
    public void vertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        if (index >= 0 && index < MAX_VERTEX_ATTRIBS) {
            VertexAttrib attrib = vertexAttribs[index];
            attrib.setPointer(size, type, false, stride, pointer, true);
            
            long vbo = getBoundBuffer(GL.Buffer.ARRAY_BUFFER);
            if (vbo != 0) {
                attrib.binding = index;
                vertexBufferBindings[index].set(vbo, 0, stride);
            }
            
            markDirty(DirtyFlags.VERTEX_INPUT | DirtyFlags.VERTEX_BINDINGS);
        }
    }
    
    /**
     * Set vertex attribute pointer for double data.
     */
    public void vertexAttribLPointer(int index, int size, int type, int stride, long pointer) {
        if (index >= 0 && index < MAX_VERTEX_ATTRIBS) {
            VertexAttrib attrib = vertexAttribs[index];
            attrib.setPointer(size, type, false, stride, pointer, false);
            attrib.isLong = true;
            
            long vbo = getBoundBuffer(GL.Buffer.ARRAY_BUFFER);
            if (vbo != 0) {
                attrib.binding = index;
                vertexBufferBindings[index].set(vbo, 0, stride);
            }
            
            markDirty(DirtyFlags.VERTEX_INPUT | DirtyFlags.VERTEX_BINDINGS);
        }
    }
    
    /**
     * Set vertex attribute format (DSA style).
     */
    public void vertexAttribFormat(int attribIndex, int size, int type, 
                                    boolean normalized, int relativeOffset) {
        if (attribIndex >= 0 && attribIndex < MAX_VERTEX_ATTRIBS) {
            vertexAttribs[attribIndex].setFormat(size, type, normalized, relativeOffset);
            vertexAttribs[attribIndex].integer = false;
            markDirty(DirtyFlags.VERTEX_INPUT);
        }
    }
    
    /**
     * Set vertex attribute format for integer data (DSA style).
     */
    public void vertexAttribIFormat(int attribIndex, int size, int type, int relativeOffset) {
        if (attribIndex >= 0 && attribIndex < MAX_VERTEX_ATTRIBS) {
            vertexAttribs[attribIndex].setFormat(size, type, false, relativeOffset);
            vertexAttribs[attribIndex].integer = true;
            markDirty(DirtyFlags.VERTEX_INPUT);
        }
    }
    
    /**
     * Set vertex attribute binding (DSA style).
     */
    public void vertexAttribBinding(int attribIndex, int bindingIndex) {
        if (attribIndex >= 0 && attribIndex < MAX_VERTEX_ATTRIBS) {
            vertexAttribs[attribIndex].setBinding(bindingIndex);
            markDirty(DirtyFlags.VERTEX_INPUT);
        }
    }
    
    /**
     * Enable vertex attribute array.
     */
    public void enableVertexAttribArray(int index) {
        if (index >= 0 && index < MAX_VERTEX_ATTRIBS) {
            int mask = 1 << index;
            if ((enabledAttribMask & mask) == 0) {
                enabledAttribMask |= mask;
                markDirty(DirtyFlags.VERTEX_INPUT);
            }
        }
    }
    
    /**
     * Disable vertex attribute array.
     */
    public void disableVertexAttribArray(int index) {
        if (index >= 0 && index < MAX_VERTEX_ATTRIBS) {
            int mask = 1 << index;
            if ((enabledAttribMask & mask) != 0) {
                enabledAttribMask &= ~mask;
                markDirty(DirtyFlags.VERTEX_INPUT);
            }
        }
    }
    
    /**
     * Check if vertex attribute is enabled.
     */
    public boolean isVertexAttribEnabled(int index) {
        return (index >= 0 && index < MAX_VERTEX_ATTRIBS) && ((enabledAttribMask & (1 << index)) != 0);
    }
    
    /**
     * Get enabled attribute mask.
     */
    public int getEnabledAttribMask() {
        return enabledAttribMask;
    }
    
    /**
     * Get vertex attribute.
     */
    public VertexAttrib getVertexAttrib(int index) {
        return (index >= 0 && index < MAX_VERTEX_ATTRIBS) ? vertexAttribs[index] : null;
    }
    
    /**
     * Set vertex attribute divisor.
     */
    public void vertexAttribDivisor(int index, int divisor) {
        if (index >= 0 && index < MAX_VERTEX_ATTRIBS) {
            int binding = vertexAttribs[index].binding;
            if (binding >= 0 && binding < MAX_VERTEX_BINDINGS) {
                vertexBufferBindings[binding].divisor = divisor;
                markDirty(DirtyFlags.VERTEX_INPUT);
            }
        }
    }
    
    /**
     * Set generic vertex attribute value.
     */
    public void vertexAttrib4f(int index, float x, float y, float z, float w) {
        if (index >= 0 && index < MAX_VERTEX_ATTRIBS) {
            vertexAttribs[index].setGenericValue(x, y, z, w);
        }
    }
    
    /**
     * Set generic vertex attribute value (integer).
     */
    public void vertexAttribI4i(int index, int x, int y, int z, int w) {
        if (index >= 0 && index < MAX_VERTEX_ATTRIBS) {
            vertexAttribs[index].setGenericValueI(x, y, z, w);
        }
    }
    
    /**
     * Get count of enabled vertex attributes.
     */
    public int getEnabledAttribCount() {
        return Integer.bitCount(enabledAttribMask);
    }
    
    /**
     * Compute vertex input state hash for pipeline caching.
     */
    public int computeVertexInputHash() {
        if (vertexInputHashValid) {
            return cachedVertexInputHash;
        }
        
        int hash = 17;
        int mask = enabledAttribMask;
        
        while (mask != 0) {
            int index = Integer.numberOfTrailingZeros(mask);
            mask &= (mask - 1);
            
            VertexAttrib attrib = vertexAttribs[index];
            hash = 31 * hash + index;
            hash = 31 * hash + attrib.computeHash();
        }
        
        // Include binding state
        for (int i = 0; i < MAX_VERTEX_BINDINGS; i++) {
            if (vertexBufferBindings[i].divisor != 0) {
                hash = 31 * hash + i;
                hash = 31 * hash + vertexBufferBindings[i].divisor;
            }
        }
        
        cachedVertexInputHash = hash;
        vertexInputHashValid = true;
        return hash;
    }
    
    // ========================================================================
    // VAO STATE
    // ========================================================================
    
    /** VAO storage */
    private final LongObjectMap<VAOState> vertexArrays = new LongObjectMap<>(64);
    
    /** Current VAO */
    private long currentVAO = 0;
    
    /**
     * VAO state snapshot.
     */
    public static final class VAOState {
        // Attribute state
        public final int[] attribSizes = new int[MAX_VERTEX_ATTRIBS];
        public final int[] attribTypes = new int[MAX_VERTEX_ATTRIBS];
        public final boolean[] attribNormalized = new boolean[MAX_VERTEX_ATTRIBS];
        public final boolean[] attribInteger = new boolean[MAX_VERTEX_ATTRIBS];
        public final int[] attribBindings = new int[MAX_VERTEX_ATTRIBS];
        public final int[] attribRelativeOffsets = new int[MAX_VERTEX_ATTRIBS];
        public final long[] attribLegacyPointers = new long[MAX_VERTEX_ATTRIBS];
        public final int[] attribLegacyStrides = new int[MAX_VERTEX_ATTRIBS];
        public int enabledMask = 0;
        
        // Binding state
        public final long[] bindingBuffers = new long[MAX_VERTEX_BINDINGS];
        public final long[] bindingOffsets = new long[MAX_VERTEX_BINDINGS];
        public final int[] bindingStrides = new int[MAX_VERTEX_BINDINGS];
        public final int[] bindingDivisors = new int[MAX_VERTEX_BINDINGS];
        
        // Element buffer
        public long elementArrayBuffer = 0;
        
        public void reset() {
            Arrays.fill(attribSizes, 4);
            Arrays.fill(attribTypes, GL.Type.FLOAT);
            Arrays.fill(attribNormalized, false);
            Arrays.fill(attribInteger, false);
            Arrays.fill(attribBindings, 0);
            Arrays.fill(attribRelativeOffsets, 0);
            Arrays.fill(attribLegacyPointers, 0);
            Arrays.fill(attribLegacyStrides, 0);
            enabledMask = 0;
            Arrays.fill(bindingBuffers, 0);
            Arrays.fill(bindingOffsets, 0);
            Arrays.fill(bindingStrides, 0);
            Arrays.fill(bindingDivisors, 0);
            elementArrayBuffer = 0;
        }
    }
    
    /**
     * Generate a VAO.
     */
    public long genVertexArray() {
        long id = nextVaoId.getAndIncrement();
        VAOState state = new VAOState();
        state.reset();
        vertexArrays.put(id, state);
        return id;
    }
    
    /**
     * Generate multiple VAOs.
     */
    public void genVertexArrays(int count, long[] ids) {
        for (int i = 0; i < count; i++) {
            ids[i] = genVertexArray();
        }
    }
    
    /**
     * Bind VAO.
     */
    public void bindVertexArray(long vao) {
        if (currentVAO != vao) {
            // Save current state to current VAO
            if (currentVAO != 0) {
                saveVAOState(currentVAO);
            }
            
            currentVAO = vao;
            
            // Restore state from new VAO
            if (vao != 0) {
                restoreVAOState(vao);
            } else {
                // Default VAO - reset to defaults
                resetVertexInputState();
            }
            
            markDirty(DirtyFlags.VAO | DirtyFlags.VERTEX_INPUT | DirtyFlags.VERTEX_BINDINGS | DirtyFlags.ELEMENT_BUFFER);
        }
    }
    
    /**
     * Save current vertex input state to VAO.
     */
    private void saveVAOState(long vao) {
        VAOState state = vertexArrays.get(vao);
        if (state == null) return;
        
        state.enabledMask = enabledAttribMask;
        
        for (int i = 0; i < MAX_VERTEX_ATTRIBS; i++) {
            VertexAttrib src = vertexAttribs[i];
            state.attribSizes[i] = src.size;
            state.attribTypes[i] = src.type;
            state.attribNormalized[i] = src.normalized;
            state.attribInteger[i] = src.integer;
            state.attribBindings[i] = src.binding;
            state.attribRelativeOffsets[i] = src.relativeOffset;
            state.attribLegacyPointers[i] = src.legacyPointer;
            state.attribLegacyStrides[i] = src.legacyStride;
        }
        
        for (int i = 0; i < MAX_VERTEX_BINDINGS; i++) {
            VertexBufferBinding src = vertexBufferBindings[i];
            state.bindingBuffers[i] = src.buffer;
            state.bindingOffsets[i] = src.offset;
            state.bindingStrides[i] = src.stride;
            state.bindingDivisors[i] = src.divisor;
        }
        
        state.elementArrayBuffer = getBoundBuffer(GL.Buffer.ELEMENT_ARRAY_BUFFER);
    }
    
    /**
     * Restore vertex input state from VAO.
     */
    private void restoreVAOState(long vao) {
        VAOState state = vertexArrays.get(vao);
        if (state == null) return;
        
        enabledAttribMask = state.enabledMask;
        
        for (int i = 0; i < MAX_VERTEX_ATTRIBS; i++) {
            VertexAttrib dst = vertexAttribs[i];
            dst.size = state.attribSizes[i];
            dst.type = state.attribTypes[i];
            dst.normalized = state.attribNormalized[i];
            dst.integer = state.attribInteger[i];
            dst.binding = state.attribBindings[i];
            dst.relativeOffset = state.attribRelativeOffsets[i];
            dst.legacyPointer = state.attribLegacyPointers[i];
            dst.legacyStride = state.attribLegacyStrides[i];
        }
        
        for (int i = 0; i < MAX_VERTEX_BINDINGS; i++) {
            VertexBufferBinding dst = vertexBufferBindings[i];
            dst.buffer = state.bindingBuffers[i];
            dst.offset = state.bindingOffsets[i];
            dst.stride = state.bindingStrides[i];
            dst.divisor = state.bindingDivisors[i];
        }
        
        bindBuffer(GL.Buffer.ELEMENT_ARRAY_BUFFER, state.elementArrayBuffer);
        
        vertexInputHashValid = false;
    }
    
    /**
     * Reset vertex input state to defaults.
     */
    private void resetVertexInputState() {
        enabledAttribMask = 0;
        for (VertexAttrib attrib : vertexAttribs) {
            attrib.reset();
        }
        for (VertexBufferBinding binding : vertexBufferBindings) {
            binding.clear();
        }
        bindBuffer(GL.Buffer.ELEMENT_ARRAY_BUFFER, 0);
        vertexInputHashValid = false;
    }
    
    /**
     * Delete VAO.
     */
    public VAOState deleteVertexArray(long vao) {
        if (currentVAO == vao) {
            bindVertexArray(0);
        }
        return vertexArrays.remove(vao);
    }
    
    /**
     * Get current VAO.
     */
    public long getCurrentVAO() {
        return currentVAO;
    }
    
    // ========================================================================
    // BLEND STATE
    // ========================================================================
    
    /**
     * Per-attachment blend state for MRT support.
     */
    public static final class AttachmentBlendState {
        public boolean enabled = false;
        public int srcRGB = GL.Blend.ONE;
        public int dstRGB = GL.Blend.ZERO;
        public int srcAlpha = GL.Blend.ONE;
        public int dstAlpha = GL.Blend.ZERO;
        public int opRGB = GL.Blend.FUNC_ADD;
        public int opAlpha = GL.Blend.FUNC_ADD;
        public int colorMask = 0xF; // RGBA all enabled
        
        public void reset() {
            enabled = false;
            srcRGB = GL.Blend.ONE;
            dstRGB = GL.Blend.ZERO;
            srcAlpha = GL.Blend.ONE;
            dstAlpha = GL.Blend.ZERO;
            opRGB = GL.Blend.FUNC_ADD;
            opAlpha = GL.Blend.FUNC_ADD;
            colorMask = 0xF;
        }
        
        public long computeHash() {
            if (!enabled) return 0;
            long hash = srcRGB;
            hash = hash * 31 + dstRGB;
            hash = hash * 31 + srcAlpha;
            hash = hash * 31 + dstAlpha;
            hash = hash * 31 + opRGB;
            hash = hash * 31 + opAlpha;
            hash = hash * 31 + colorMask;
            return hash;
        }
        
        public int toVkSrcFactor(int glFactor) {
            return switch (glFactor) {
                case GL.Blend.ZERO -> VK10.VK_BLEND_FACTOR_ZERO;
                case GL.Blend.ONE -> VK10.VK_BLEND_FACTOR_ONE;
                case GL.Blend.SRC_COLOR -> VK10.VK_BLEND_FACTOR_SRC_COLOR;
                case GL.Blend.ONE_MINUS_SRC_COLOR -> VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
                case GL.Blend.DST_COLOR -> VK10.VK_BLEND_FACTOR_DST_COLOR;
                case GL.Blend.ONE_MINUS_DST_COLOR -> VK10.VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
                case GL.Blend.SRC_ALPHA -> VK10.VK_BLEND_FACTOR_SRC_ALPHA;
                case GL.Blend.ONE_MINUS_SRC_ALPHA -> VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
                case GL.Blend.DST_ALPHA -> VK10.VK_BLEND_FACTOR_DST_ALPHA;
                case GL.Blend.ONE_MINUS_DST_ALPHA -> VK10.VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
                case GL.Blend.CONSTANT_COLOR -> VK10.VK_BLEND_FACTOR_CONSTANT_COLOR;
                case GL.Blend.ONE_MINUS_CONSTANT_COLOR -> VK10.VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR;
                case GL.Blend.CONSTANT_ALPHA -> VK10.VK_BLEND_FACTOR_CONSTANT_ALPHA;
                case GL.Blend.ONE_MINUS_CONSTANT_ALPHA -> VK10.VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA;
                case GL.Blend.SRC_ALPHA_SATURATE -> VK10.VK_BLEND_FACTOR_SRC_ALPHA_SATURATE;
                case GL.Blend.SRC1_COLOR -> VK10.VK_BLEND_FACTOR_SRC1_COLOR;
                case GL.Blend.ONE_MINUS_SRC1_COLOR -> VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC1_COLOR;
                case GL.Blend.SRC1_ALPHA -> VK10.VK_BLEND_FACTOR_SRC1_ALPHA;
                case GL.Blend.ONE_MINUS_SRC1_ALPHA -> VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC1_ALPHA;
                default -> VK10.VK_BLEND_FACTOR_ONE;
            };
        }
        
        public int toVkBlendOp(int glOp) {
            return switch (glOp) {
                case GL.Blend.FUNC_ADD -> VK10.VK_BLEND_OP_ADD;
                case GL.Blend.FUNC_SUBTRACT -> VK10.VK_BLEND_OP_SUBTRACT;
                case GL.Blend.FUNC_REVERSE_SUBTRACT -> VK10.VK_BLEND_OP_REVERSE_SUBTRACT;
                case GL.Blend.MIN -> VK10.VK_BLEND_OP_MIN;
                case GL.Blend.MAX -> VK10.VK_BLEND_OP_MAX;
                default -> VK10.VK_BLEND_OP_ADD;
            };
        }
        
        public void populate(VkPipelineColorBlendAttachmentState state) {
            state.blendEnable(enabled);
            state.srcColorBlendFactor(toVkSrcFactor(srcRGB));
            state.dstColorBlendFactor(toVkSrcFactor(dstRGB));
            state.colorBlendOp(toVkBlendOp(opRGB));
            state.srcAlphaBlendFactor(toVkSrcFactor(srcAlpha));
            state.dstAlphaBlendFactor(toVkSrcFactor(dstAlpha));
            state.alphaBlendOp(toVkBlendOp(opAlpha));
            state.colorWriteMask(colorMask);
        }
    }
    
    /** Per-attachment blend state */
    private final AttachmentBlendState[] attachmentBlendStates = new AttachmentBlendState[MAX_COLOR_ATTACHMENTS];
    
    /** Global blend constants */
    private final float[] blendConstants = {0.0f, 0.0f, 0.0f, 0.0f};
    
    {
        for (int i = 0; i < MAX_COLOR_ATTACHMENTS; i++) {
            attachmentBlendStates[i] = new AttachmentBlendState();
        }
    }
    
    /**
     * Enable/disable blending.
     */
    public void setBlendEnabled(boolean enabled) {
        setBlendEnabledi(0, enabled);
    }
    
    /**
     * Enable/disable blending for specific draw buffer.
     */
    public void setBlendEnabledi(int buf, boolean enabled) {
        if (buf >= 0 && buf < MAX_COLOR_ATTACHMENTS) {
            if (attachmentBlendStates[buf].enabled != enabled) {
                attachmentBlendStates[buf].enabled = enabled;
                markDirty(DirtyFlags.BLEND_STATE);
            }
        }
    }
    
    /**
     * Set blend function for all attachments.
     */
    public void blendFunc(int sfactor, int dfactor) {
        blendFuncSeparate(sfactor, dfactor, sfactor, dfactor);
    }
    
    /**
     * Set separate RGB/Alpha blend functions for all attachments.
     */
    public void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        boolean changed = false;
        for (AttachmentBlendState state : attachmentBlendStates) {
            if (state.srcRGB != srcRGB || state.dstRGB != dstRGB ||
                state.srcAlpha != srcAlpha || state.dstAlpha != dstAlpha) {
                state.srcRGB = srcRGB;
                state.dstRGB = dstRGB;
                state.srcAlpha = srcAlpha;
                state.dstAlpha = dstAlpha;
                changed = true;
            }
        }
        if (changed) markDirty(DirtyFlags.BLEND_STATE);
    }
    
    /**
     * Set blend function for specific draw buffer.
     */
    public void blendFunci(int buf, int sfactor, int dfactor) {
        blendFuncSeparatei(buf, sfactor, dfactor, sfactor, dfactor);
    }
    
    /**
     * Set separate RGB/Alpha blend functions for specific draw buffer.
     */
    public void blendFuncSeparatei(int buf, int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        if (buf >= 0 && buf < MAX_COLOR_ATTACHMENTS) {
            AttachmentBlendState state = attachmentBlendStates[buf];
            if (state.srcRGB != srcRGB || state.dstRGB != dstRGB ||
                state.srcAlpha != srcAlpha || state.dstAlpha != dstAlpha) {
                state.srcRGB = srcRGB;
                state.dstRGB = dstRGB;
                state.srcAlpha = srcAlpha;
                state.dstAlpha = dstAlpha;
                markDirty(DirtyFlags.BLEND_STATE);
            }
        }
    }
    
    /**
     * Set blend equation for all attachments.
     */
    public void blendEquation(int mode) {
        blendEquationSeparate(mode, mode);
    }
    
    /**
     * Set separate RGB/Alpha blend equations for all attachments.
     */
    public void blendEquationSeparate(int modeRGB, int modeAlpha) {
        boolean changed = false;
        for (AttachmentBlendState state : attachmentBlendStates) {
            if (state.opRGB != modeRGB || state.opAlpha != modeAlpha) {
                state.opRGB = modeRGB;
                state.opAlpha = modeAlpha;
                changed = true;
            }
        }
        if (changed) markDirty(DirtyFlags.BLEND_STATE);
    }
    
    /**
     * Set blend equation for specific draw buffer.
     */
    public void blendEquationi(int buf, int mode) {
        blendEquationSeparatei(buf, mode, mode);
    }
    
    /**
     * Set separate RGB/Alpha blend equations for specific draw buffer.
     */
    public void blendEquationSeparatei(int buf, int modeRGB, int modeAlpha) {
        if (buf >= 0 && buf < MAX_COLOR_ATTACHMENTS) {
            AttachmentBlendState state = attachmentBlendStates[buf];
            if (state.opRGB != modeRGB || state.opAlpha != modeAlpha) {
                state.opRGB = modeRGB;
                state.opAlpha = modeAlpha;
                markDirty(DirtyFlags.BLEND_STATE);
            }
        }
    }
    
    /**
     * Set blend constants.
     */
    public void blendColor(float r, float g, float b, float a) {
        if (blendConstants[0] != r || blendConstants[1] != g ||
            blendConstants[2] != b || blendConstants[3] != a) {
            blendConstants[0] = r;
            blendConstants[1] = g;
            blendConstants[2] = b;
            blendConstants[3] = a;
            markDirty(DirtyFlags.BLEND_CONSTANTS);
        }
    }
    
    /**
     * Get blend constants.
     */
    public void getBlendColor(float[] dest) {
        System.arraycopy(blendConstants, 0, dest, 0, 4);
    }
    
    /**
     * Get blend state for attachment.
     */
    public AttachmentBlendState getAttachmentBlendState(int index) {
        return (index >= 0 && index < MAX_COLOR_ATTACHMENTS) ? attachmentBlendStates[index] : null;
    }
    
    /**
     * Check if blend is enabled on any attachment.
     */
    public boolean isBlendEnabled() {
        for (AttachmentBlendState state : attachmentBlendStates) {
            if (state.enabled) return true;
        }
        return false;
    }
    
    // ========================================================================
    // DEPTH STATE
    // ========================================================================
    
    private boolean depthTestEnabled = false;
    private boolean depthWriteEnabled = true;
    private int depthFunc = GL.Compare.LESS;
    private float depthRangeNear = 0.0f;
    private float depthRangeFar = 1.0f;
    private boolean depthClampEnabled = false;
    
    // Depth bias (polygon offset)
    private boolean depthBiasEnabled = false;
    private float depthBiasConstant = 0.0f;
    private float depthBiasSlope = 0.0f;
    private float depthBiasClamp = 0.0f;
    
    // Depth bounds test (requires extension)
    private boolean depthBoundsTestEnabled = false;
    private float depthBoundsMin = 0.0f;
    private float depthBoundsMax = 1.0f;
    
    /**
     * Enable/disable depth testing.
     */
    public void setDepthTestEnabled(boolean enabled) {
        if (depthTestEnabled != enabled) {
            depthTestEnabled = enabled;
            markDirty(DirtyFlags.DEPTH_STATE);
        }
    }
    
    /**
     * Set depth write mask.
     */
    public void depthMask(boolean flag) {
        if (depthWriteEnabled != flag) {
            depthWriteEnabled = flag;
            markDirty(DirtyFlags.DEPTH_STATE);
        }
    }
    
    /**
     * Set depth comparison function.
     */
    public void depthFunc(int func) {
        if (depthFunc != func) {
            depthFunc = func;
            markDirty(DirtyFlags.DEPTH_STATE);
        }
    }
    
    /**
     * Set depth range.
     */
    public void depthRange(float near, float far) {
        if (depthRangeNear != near || depthRangeFar != far) {
            depthRangeNear = near;
            depthRangeFar = far;
            markDirty(DirtyFlags.VIEWPORT); // Depth range is part of viewport in Vulkan
        }
    }
    
    /**
     * Set polygon offset (depth bias).
     */
    public void polygonOffset(float factor, float units) {
        if (depthBiasSlope != factor || depthBiasConstant != units) {
            depthBiasSlope = factor;
            depthBiasConstant = units;
            markDirty(DirtyFlags.DEPTH_BIAS);
        }
    }
    
    /**
     * Set polygon offset with clamp.
     */
    public void polygonOffsetClamp(float factor, float units, float clamp) {
        if (depthBiasSlope != factor || depthBiasConstant != units || depthBiasClamp != clamp) {
            depthBiasSlope = factor;
            depthBiasConstant = units;
            depthBiasClamp = clamp;
            markDirty(DirtyFlags.DEPTH_BIAS);
        }
    }
    
    /**
     * Enable/disable depth bias.
     */
    public void setDepthBiasEnabled(boolean enabled) {
        if (depthBiasEnabled != enabled) {
            depthBiasEnabled = enabled;
            markDirty(DirtyFlags.DEPTH_STATE);
        }
    }
    
    /**
     * Enable/disable depth clamp.
     */
    public void setDepthClampEnabled(boolean enabled) {
        if (depthClampEnabled != enabled) {
            depthClampEnabled = enabled;
            markDirty(DirtyFlags.DEPTH_CLAMP);
        }
    }
    
    /**
     * Set depth bounds.
     */
    public void depthBounds(float min, float max) {
        if (depthBoundsMin != min || depthBoundsMax != max) {
            depthBoundsMin = min;
            depthBoundsMax = max;
            markDirty(DirtyFlags.DEPTH_BOUNDS);
        }
    }
    
    /**
     * Enable/disable depth bounds test.
     */
    public void setDepthBoundsTestEnabled(boolean enabled) {
        if (depthBoundsTestEnabled != enabled) {
            depthBoundsTestEnabled = enabled;
            markDirty(DirtyFlags.DEPTH_STATE);
        }
    }
    
    // Getters
    public boolean isDepthTestEnabled() { return depthTestEnabled; }
    public boolean isDepthWriteEnabled() { return depthWriteEnabled; }
    public int getDepthFunc() { return depthFunc; }
    public float getDepthRangeNear() { return depthRangeNear; }
    public float getDepthRangeFar() { return depthRangeFar; }
    public boolean isDepthClampEnabled() { return depthClampEnabled; }
    public boolean isDepthBiasEnabled() { return depthBiasEnabled; }
    public float getDepthBiasConstant() { return depthBiasConstant; }
    public float getDepthBiasSlope() { return depthBiasSlope; }
    public float getDepthBiasClamp() { return depthBiasClamp; }
    public boolean isDepthBoundsTestEnabled() { return depthBoundsTestEnabled; }
    public float getDepthBoundsMin() { return depthBoundsMin; }
    public float getDepthBoundsMax() { return depthBoundsMax; }
    
    /**
     * Convert GL compare func to Vulkan.
     */
    public static int glCompareToVk(int glFunc) {
        return switch (glFunc) {
            case GL.Compare.NEVER -> VK10.VK_COMPARE_OP_NEVER;
            case GL.Compare.LESS -> VK10.VK_COMPARE_OP_LESS;
            case GL.Compare.EQUAL -> VK10.VK_COMPARE_OP_EQUAL;
            case GL.Compare.LEQUAL -> VK10.VK_COMPARE_OP_LESS_OR_EQUAL;
            case GL.Compare.GREATER -> VK10.VK_COMPARE_OP_GREATER;
            case GL.Compare.NOTEQUAL -> VK10.VK_COMPARE_OP_NOT_EQUAL;
            case GL.Compare.GEQUAL -> VK10.VK_COMPARE_OP_GREATER_OR_EQUAL;
            case GL.Compare.ALWAYS -> VK10.VK_COMPARE_OP_ALWAYS;
            default -> VK10.VK_COMPARE_OP_LESS;
        };
    }
    
    // ========================================================================
    // STENCIL STATE
    // ========================================================================
    
    /**
     * Stencil state for one face.
     */
    public static final class StencilFaceState {
        public int failOp = GL.Stencil.KEEP;
        public int depthFailOp = GL.Stencil.KEEP;
        public int passOp = GL.Stencil.KEEP;
        public int compareOp = GL.Compare.ALWAYS;
        public int compareMask = 0xFF;
        public int writeMask = 0xFF;
        public int reference = 0;
        
        public void reset() {
            failOp = GL.Stencil.KEEP;
            depthFailOp = GL.Stencil.KEEP;
            passOp = GL.Stencil.KEEP;
            compareOp = GL.Compare.ALWAYS;
            compareMask = 0xFF;
            writeMask = 0xFF;
            reference = 0;
        }
        
        public long computeHash() {
            long hash = failOp;
            hash = hash * 31 + depthFailOp;
            hash = hash * 31 + passOp;
            hash = hash * 31 + compareOp;
            hash = hash * 31 + compareMask;
            hash = hash * 31 + writeMask;
            return hash;
        }
        
        public static int glStencilOpToVk(int glOp) {
            return switch (glOp) {
                case GL.Stencil.KEEP -> VK10.VK_STENCIL_OP_KEEP;
                case GL.Stencil.ZERO -> VK10.VK_STENCIL_OP_ZERO;
                case GL.Stencil.REPLACE -> VK10.VK_STENCIL_OP_REPLACE;
                case GL.Stencil.INCR -> VK10.VK_STENCIL_OP_INCREMENT_AND_CLAMP;
                case GL.Stencil.INCR_WRAP -> VK10.VK_STENCIL_OP_INCREMENT_AND_WRAP;
                case GL.Stencil.DECR -> VK10.VK_STENCIL_OP_DECREMENT_AND_CLAMP;
                case GL.Stencil.DECR_WRAP -> VK10.VK_STENCIL_OP_DECREMENT_AND_WRAP;
                case GL.Stencil.INVERT -> VK10.VK_STENCIL_OP_INVERT;
                default -> VK10.VK_STENCIL_OP_KEEP;
            };
        }
        
        public void populate(VkStencilOpState state) {
            state.failOp(glStencilOpToVk(failOp));
            state.passOp(glStencilOpToVk(passOp));
            state.depthFailOp(glStencilOpToVk(depthFailOp));
            state.compareOp(glCompareToVk(compareOp));
            state.compareMask(compareMask);
            state.writeMask(writeMask);
            state.reference(reference);
        }
    }
    
    private boolean stencilTestEnabled = false;
    private final StencilFaceState stencilFront = new StencilFaceState();
    private final StencilFaceState stencilBack = new StencilFaceState();
    
    /**
     * Enable/disable stencil testing.
     */
    public void setStencilTestEnabled(boolean enabled) {
        if (stencilTestEnabled != enabled) {
            stencilTestEnabled = enabled;
            markDirty(DirtyFlags.STENCIL_STATE);
        }
    }
    
    /**
     * Set stencil function for both faces.
     */
    public void stencilFunc(int func, int ref, int mask) {
        stencilFuncSeparate(GL.Face.FRONT_AND_BACK, func, ref, mask);
    }
    
    /**
     * Set stencil function for specific face(s).
     */
    public void stencilFuncSeparate(int face, int func, int ref, int mask) {
        boolean changed = false;
        
        if (face == GL.Face.FRONT || face == GL.Face.FRONT_AND_BACK) {
            if (stencilFront.compareOp != func || stencilFront.reference != ref || 
                stencilFront.compareMask != mask) {
                stencilFront.compareOp = func;
                stencilFront.reference = ref;
                stencilFront.compareMask = mask;
                changed = true;
            }
        }
        
        if (face == GL.Face.BACK || face == GL.Face.FRONT_AND_BACK) {
            if (stencilBack.compareOp != func || stencilBack.reference != ref || 
                stencilBack.compareMask != mask) {
                stencilBack.compareOp = func;
                stencilBack.reference = ref;
                stencilBack.compareMask = mask;
                changed = true;
            }
        }
        
        if (changed) {
            markDirty(DirtyFlags.STENCIL_STATE | DirtyFlags.STENCIL_REFERENCE);
        }
    }
    
    /**
     * Set stencil operations for both faces.
     */
    public void stencilOp(int sfail, int dpfail, int dppass) {
        stencilOpSeparate(GL.Face.FRONT_AND_BACK, sfail, dpfail, dppass);
    }
    
    /**
     * Set stencil operations for specific face(s).
     */
    public void stencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
        boolean changed = false;
        
        if (face == GL.Face.FRONT || face == GL.Face.FRONT_AND_BACK) {
            if (stencilFront.failOp != sfail || stencilFront.depthFailOp != dpfail || 
                stencilFront.passOp != dppass) {
                stencilFront.failOp = sfail;
                stencilFront.depthFailOp = dpfail;
                stencilFront.passOp = dppass;
                changed = true;
            }
        }
        
        if (face == GL.Face.BACK || face == GL.Face.FRONT_AND_BACK) {
            if (stencilBack.failOp != sfail || stencilBack.depthFailOp != dpfail || 
                stencilBack.passOp != dppass) {
                stencilBack.failOp = sfail;
                stencilBack.depthFailOp = dpfail;
                stencilBack.passOp = dppass;
                changed = true;
            }
        }
        
        if (changed) markDirty(DirtyFlags.STENCIL_STATE);
    }
    
    /**
     * Set stencil write mask for both faces.
     */
    public void stencilMask(int mask) {
        stencilMaskSeparate(GL.Face.FRONT_AND_BACK, mask);
    }
    
    /**
     * Set stencil write mask for specific face(s).
     */
    public void stencilMaskSeparate(int face, int mask) {
        boolean changed = false;
        
        if (face == GL.Face.FRONT || face == GL.Face.FRONT_AND_BACK) {
            if (stencilFront.writeMask != mask) {
                stencilFront.writeMask = mask;
                changed = true;
            }
        }
        
        if (face == GL.Face.BACK || face == GL.Face.FRONT_AND_BACK) {
            if (stencilBack.writeMask != mask) {
                stencilBack.writeMask = mask;
                changed = true;
            }
        }
        
        if (changed) markDirty(DirtyFlags.STENCIL_STATE);
    }
    
    // Getters
    public boolean isStencilTestEnabled() { return stencilTestEnabled; }
    public StencilFaceState getStencilFront() { return stencilFront; }
    public StencilFaceState getStencilBack() { return stencilBack; }
    
    // ========================================================================
    // CULL AND POLYGON STATE
    // ========================================================================
    
    private boolean cullFaceEnabled = false;
    private int cullFaceMode = GL.Face.BACK;
    private int frontFace = GL.Face.CCW;
    private int polygonModeFront = GL.Polygon.FILL;
    private int polygonModeBack = GL.Polygon.FILL;
    private float lineWidth = 1.0f;
    private float pointSize = 1.0f;
    private boolean rasterizerDiscardEnabled = false;
    
    /**
     * Enable/disable face culling.
     */
    public void setCullFaceEnabled(boolean enabled) {
        if (cullFaceEnabled != enabled) {
            cullFaceEnabled = enabled;
            markDirty(DirtyFlags.CULL_STATE);
        }
    }
    
    /**
     * Set which face to cull.
     */
    public void cullFace(int mode) {
        if (cullFaceMode != mode) {
            cullFaceMode = mode;
            markDirty(DirtyFlags.CULL_STATE);
        }
    }
    
    /**
     * Set front face winding.
     */
    public void frontFace(int mode) {
        if (frontFace != mode) {
            frontFace = mode;
            markDirty(DirtyFlags.FRONT_FACE);
        }
    }
    
    /**
     * Set polygon mode.
     */
    public void polygonMode(int face, int mode) {
        boolean changed = false;
        if (face == GL.Face.FRONT || face == GL.Face.FRONT_AND_BACK) {
            if (polygonModeFront != mode) {
                polygonModeFront = mode;
                changed = true;
            }
        }
        if (face == GL.Face.BACK || face == GL.Face.FRONT_AND_BACK) {
            if (polygonModeBack != mode) {
                polygonModeBack = mode;
                changed = true;
            }
        }
        if (changed) markDirty(DirtyFlags.POLYGON_MODE);
    }
    
    /**
     * Set line width.
     */
    public void lineWidth(float width) {
        if (lineWidth != width) {
            lineWidth = width;
            markDirty(DirtyFlags.LINE_WIDTH);
        }
    }
    
    /**
     * Set point size.
     */
    public void pointSize(float size) {
        pointSize = size;
    }
    
    /**
     * Enable/disable rasterizer discard.
     */
    public void setRasterizerDiscardEnabled(boolean enabled) {
        if (rasterizerDiscardEnabled != enabled) {
            rasterizerDiscardEnabled = enabled;
            markDirty(DirtyFlags.RASTERIZER_DISCARD);
        }
    }
    
    // Getters
    public boolean isCullFaceEnabled() { return cullFaceEnabled; }
    public int getCullFaceMode() { return cullFaceMode; }
    public int getFrontFace() { return frontFace; }
    public int getPolygonMode() { return polygonModeFront; } // Vulkan only supports one mode
    public float getLineWidth() { return lineWidth; }
    public float getPointSize() { return pointSize; }
    public boolean isRasterizerDiscardEnabled() { return rasterizerDiscardEnabled; }
    
    /**
     * Convert GL cull mode to Vulkan.
     */
    public int getVkCullMode() {
        if (!cullFaceEnabled) return VK10.VK_CULL_MODE_NONE;
        return switch (cullFaceMode) {
            case GL.Face.FRONT -> VK10.VK_CULL_MODE_FRONT_BIT;
            case GL.Face.BACK -> VK10.VK_CULL_MODE_BACK_BIT;
            case GL.Face.FRONT_AND_BACK -> VK10.VK_CULL_MODE_FRONT_AND_BACK;
            default -> VK10.VK_CULL_MODE_NONE;
        };
    }
    
    /**
     * Convert GL front face to Vulkan.
     */
    public int getVkFrontFace() {
        // Note: OpenGL and Vulkan have opposite Y directions, so we flip
        return (frontFace == GL.Face.CCW) ? VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE 
                                          : VK10.VK_FRONT_FACE_CLOCKWISE;
    }
    
    /**
     * Convert GL polygon mode to Vulkan.
     */
    public int getVkPolygonMode() {
        return switch (polygonModeFront) {
            case GL.Polygon.POINT -> VK10.VK_POLYGON_MODE_POINT;
            case GL.Polygon.LINE -> VK10.VK_POLYGON_MODE_LINE;
            default -> VK10.VK_POLYGON_MODE_FILL;
        };
    }
    
    // ========================================================================
    // COLOR MASK AND LOGIC OP
    // ========================================================================
    
    private boolean logicOpEnabled = false;
    private int logicOp = GL.Logic.COPY;
    
    /**
     * Set color write mask for all attachments.
     */
    public void colorMask(boolean r, boolean g, boolean b, boolean a) {
        int mask = (r ? 0x1 : 0) | (g ? 0x2 : 0) | (b ? 0x4 : 0) | (a ? 0x8 : 0);
        boolean changed = false;
        for (AttachmentBlendState state : attachmentBlendStates) {
            if (state.colorMask != mask) {
                state.colorMask = mask;
                changed = true;
            }
        }
        if (changed) markDirty(DirtyFlags.COLOR_MASK);
    }
    
    /**
     * Set color write mask for specific draw buffer.
     */
    public void colorMaski(int buf, boolean r, boolean g, boolean b, boolean a) {
        if (buf >= 0 && buf < MAX_COLOR_ATTACHMENTS) {
            int mask = (r ? 0x1 : 0) | (g ? 0x2 : 0) | (b ? 0x4 : 0) | (a ? 0x8 : 0);
            if (attachmentBlendStates[buf].colorMask != mask) {
                attachmentBlendStates[buf].colorMask = mask;
                markDirty(DirtyFlags.COLOR_MASK);
            }
        }
    }
    
    /**
     * Enable/disable logic operations.
     */
    public void setLogicOpEnabled(boolean enabled) {
        if (logicOpEnabled != enabled) {
            logicOpEnabled = enabled;
            markDirty(DirtyFlags.LOGIC_OP);
        }
    }
    
    /**
     * Set logic operation.
     */
    public void logicOp(int op) {
        if (logicOp != op) {
            logicOp = op;
            markDirty(DirtyFlags.LOGIC_OP);
        }
    }
    
    public boolean isLogicOpEnabled() { return logicOpEnabled; }
    public int getLogicOp() { return logicOp; }
    
    /**
     * Convert GL logic op to Vulkan.
     */
    public int getVkLogicOp() {
        return switch (logicOp) {
            case GL.Logic.CLEAR -> VK10.VK_LOGIC_OP_CLEAR;
            case GL.Logic.AND -> VK10.VK_LOGIC_OP_AND;
            case GL.Logic.AND_REVERSE -> VK10.VK_LOGIC_OP_AND_REVERSE;
            case GL.Logic.COPY -> VK10.VK_LOGIC_OP_COPY;
            case GL.Logic.AND_INVERTED -> VK10.VK_LOGIC_OP_AND_INVERTED;
            case GL.Logic.NOOP -> VK10.VK_LOGIC_OP_NO_OP;
            case GL.Logic.XOR -> VK10.VK_LOGIC_OP_XOR;
            case GL.Logic.OR -> VK10.VK_LOGIC_OP_OR;
            case GL.Logic.NOR -> VK10.VK_LOGIC_OP_NOR;
            case GL.Logic.EQUIV -> VK10.VK_LOGIC_OP_EQUIVALENT;
            case GL.Logic.INVERT -> VK10.VK_LOGIC_OP_INVERT;
            case GL.Logic.OR_REVERSE -> VK10.VK_LOGIC_OP_OR_REVERSE;
            case GL.Logic.COPY_INVERTED -> VK10.VK_LOGIC_OP_COPY_INVERTED;
            case GL.Logic.OR_INVERTED -> VK10.VK_LOGIC_OP_OR_INVERTED;
            case GL.Logic.NAND -> VK10.VK_LOGIC_OP_NAND;
            case GL.Logic.SET -> VK10.VK_LOGIC_OP_SET;
            default -> VK10.VK_LOGIC_OP_COPY;
        };
    }
    
    // ========================================================================
    // MULTISAMPLING STATE
    // ========================================================================
    
    private int sampleCount = 1;
    private boolean sampleShadingEnabled = false;
    private float minSampleShading = 1.0f;
    private boolean alphaToCoverageEnabled = false;
    private boolean alphaToOneEnabled = false;
    private boolean sampleMaskEnabled = false;
    private final int[] sampleMask = new int[4]; // Up to 128 samples
    private float sampleCoverageValue = 1.0f;
    private boolean sampleCoverageInvert = false;
    
    public void setSampleCount(int count) {
        if (sampleCount != count) {
            sampleCount = count;
            markDirty(DirtyFlags.SAMPLE_STATE);
        }
    }
    
    public void setSampleShadingEnabled(boolean enabled) {
        if (sampleShadingEnabled != enabled) {
            sampleShadingEnabled = enabled;
            markDirty(DirtyFlags.SAMPLE_STATE);
        }
    }
    
    public void minSampleShading(float value) {
        if (minSampleShading != value) {
            minSampleShading = value;
            markDirty(DirtyFlags.SAMPLE_STATE);
        }
    }
    
    public void setAlphaToCoverageEnabled(boolean enabled) {
        if (alphaToCoverageEnabled != enabled) {
            alphaToCoverageEnabled = enabled;
            markDirty(DirtyFlags.SAMPLE_STATE);
        }
    }
    
    public void setAlphaToOneEnabled(boolean enabled) {
        if (alphaToOneEnabled != enabled) {
            alphaToOneEnabled = enabled;
            markDirty(DirtyFlags.SAMPLE_STATE);
        }
    }
    
    public void setSampleMaskEnabled(boolean enabled) {
        if (sampleMaskEnabled != enabled) {
            sampleMaskEnabled = enabled;
            markDirty(DirtyFlags.SAMPLE_MASK);
        }
    }
    
    public void sampleMask(int index, int mask) {
        if (index >= 0 && index < sampleMask.length && sampleMask[index] != mask) {
            sampleMask[index] = mask;
            markDirty(DirtyFlags.SAMPLE_MASK);
        }
    }
    
    public void sampleCoverage(float value, boolean invert) {
        sampleCoverageValue = value;
        sampleCoverageInvert = invert;
    }
    
    // Getters
    public int getSampleCount() { return sampleCount; }
    public boolean isSampleShadingEnabled() { return sampleShadingEnabled; }
    public float getMinSampleShading() { return minSampleShading; }
    public boolean isAlphaToCoverageEnabled() { return alphaToCoverageEnabled; }
    public boolean isAlphaToOneEnabled() { return alphaToOneEnabled; }
    public boolean isSampleMaskEnabled() { return sampleMaskEnabled; }
    public int getSampleMask(int index) { return (index >= 0 && index < sampleMask.length) ? sampleMask[index] : 0xFFFFFFFF; }
    
    // ========================================================================
    // PRIMITIVE STATE
    // ========================================================================
    
    private int primitiveTopology = GL.Primitive.TRIANGLES;
    private boolean primitiveRestartEnabled = false;
    private int patchControlPoints = 3;
    
    public void setPrimitiveTopology(int topology) {
        if (primitiveTopology != topology) {
            primitiveTopology = topology;
            markDirty(DirtyFlags.PRIMITIVE_TOPOLOGY);
        }
    }
    
    public void setPrimitiveRestartEnabled(boolean enabled) {
        if (primitiveRestartEnabled != enabled) {
            primitiveRestartEnabled = enabled;
            markDirty(DirtyFlags.PRIMITIVE_RESTART);
        }
    }
    
    public void patchParameteri(int pname, int value) {
        if (pname == 0x8E72) { // GL_PATCH_VERTICES
            if (patchControlPoints != value) {
                patchControlPoints = value;
                markDirty(DirtyFlags.PATCH_CONTROL_POINTS);
            }
        }
    }
    
    public int getPrimitiveTopology() { return primitiveTopology; }
    public boolean isPrimitiveRestartEnabled() { return primitiveRestartEnabled; }
    public int getPatchControlPoints() { return patchControlPoints; }
    
    /**
     * Convert GL primitive mode to Vulkan topology.
     */
    public int getVkPrimitiveTopology() {
        return switch (primitiveTopology) {
            case GL.Primitive.POINTS -> VK10.VK_PRIMITIVE_TOPOLOGY_POINT_LIST;
            case GL.Primitive.LINES -> VK10.VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            case GL.Primitive.LINE_LOOP -> VK10.VK_PRIMITIVE_TOPOLOGY_LINE_STRIP; // Approximate
            case GL.Primitive.LINE_STRIP -> VK10.VK_PRIMITIVE_TOPOLOGY_LINE_STRIP;
            case GL.Primitive.TRIANGLES -> VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            case GL.Primitive.TRIANGLE_STRIP -> VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
            case GL.Primitive.TRIANGLE_FAN -> VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN;
            case GL.Primitive.LINES_ADJACENCY -> VK10.VK_PRIMITIVE_TOPOLOGY_LINE_LIST_WITH_ADJACENCY;
            case GL.Primitive.LINE_STRIP_ADJACENCY -> VK10.VK_PRIMITIVE_TOPOLOGY_LINE_STRIP_WITH_ADJACENCY;
            case GL.Primitive.TRIANGLES_ADJACENCY -> VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST_WITH_ADJACENCY;
            case GL.Primitive.TRIANGLE_STRIP_ADJACENCY -> VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP_WITH_ADJACENCY;
            case GL.Primitive.PATCHES -> VK10.VK_PRIMITIVE_TOPOLOGY_PATCH_LIST;
            default -> VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
        };
    }
    
    // ========================================================================
    // VIEWPORT AND SCISSOR STATE
    // ========================================================================
    
    /**
     * Viewport state.
     */
    public static final class ViewportState {
        public float x, y, width, height;
        public float minDepth = 0.0f, maxDepth = 1.0f;
        
        public void set(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        public void setDepthRange(float min, float max) {
            this.minDepth = min;
            this.maxDepth = max;
        }
    }
    
    /**
     * Scissor state.
     */
    public static final class ScissorState {
        public int x, y, width, height;
        
        public void set(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
    
    private final ViewportState[] viewports = new ViewportState[MAX_VIEWPORTS];
    private final ScissorState[] scissors = new ScissorState[MAX_VIEWPORTS];
    private boolean scissorTestEnabled = false;
    private int activeViewportCount = 1;
    
    {
        for (int i = 0; i < MAX_VIEWPORTS; i++) {
            viewports[i] = new ViewportState();
            scissors[i] = new ScissorState();
        }
    }
    
    /**
     * Set viewport (single viewport).
     */
    public void viewport(int x, int y, int width, int height) {
        viewportIndexed(0, x, y, width, height);
    }
    
    /**
     * Set viewport at index.
     */
    public void viewportIndexed(int index, float x, float y, float width, float height) {
        if (index >= 0 && index < MAX_VIEWPORTS) {
            ViewportState vp = viewports[index];
            if (vp.x != x || vp.y != y || vp.width != width || vp.height != height) {
                vp.set(x, y, width, height);
                markDirty(DirtyFlags.VIEWPORT);
            }
        }
    }
    
    /**
     * Set multiple viewports.
     */
    public void viewportArray(int first, int count, float[] v) {
        boolean changed = false;
        for (int i = 0; i < count && (first + i) < MAX_VIEWPORTS; i++) {
            int offset = i * 4;
            ViewportState vp = viewports[first + i];
            if (vp.x != v[offset] || vp.y != v[offset + 1] ||
                vp.width != v[offset + 2] || vp.height != v[offset + 3]) {
                vp.set(v[offset], v[offset + 1], v[offset + 2], v[offset + 3]);
                changed = true;
            }
        }
        if (changed) markDirty(DirtyFlags.VIEWPORT);
    }
    
    /**
     * Set depth range at index.
     */
    public void depthRangeIndexed(int index, float near, float far) {
        if (index >= 0 && index < MAX_VIEWPORTS) {
            ViewportState vp = viewports[index];
            if (vp.minDepth != near || vp.maxDepth != far) {
                vp.setDepthRange(near, far);
                markDirty(DirtyFlags.VIEWPORT);
            }
        }
    }
    
    /**
     * Set scissor (single scissor).
     */
    public void scissor(int x, int y, int width, int height) {
        scissorIndexed(0, x, y, width, height);
    }
    
    /**
     * Set scissor at index.
     */
    public void scissorIndexed(int index, int x, int y, int width, int height) {
        if (index >= 0 && index < MAX_VIEWPORTS) {
            ScissorState sc = scissors[index];
            if (sc.x != x || sc.y != y || sc.width != width || sc.height != height) {
                sc.set(x, y, width, height);
                markDirty(DirtyFlags.SCISSOR);
            }
        }
    }
    
    /**
     * Set multiple scissors.
     */
    public void scissorArray(int first, int count, int[] v) {
        boolean changed = false;
        for (int i = 0; i < count && (first + i) < MAX_VIEWPORTS; i++) {
            int offset = i * 4;
            ScissorState sc = scissors[first + i];
            if (sc.x != v[offset] || sc.y != v[offset + 1] ||
                sc.width != v[offset + 2] || sc.height != v[offset + 3]) {
                sc.set(v[offset], v[offset + 1], v[offset + 2], v[offset + 3]);
                changed = true;
            }
        }
        if (changed) markDirty(DirtyFlags.SCISSOR);
    }
    
    /**
     * Enable/disable scissor test.
     */
    public void setScissorTestEnabled(boolean enabled) {
        if (scissorTestEnabled != enabled) {
            scissorTestEnabled = enabled;
            markDirty(DirtyFlags.SCISSOR);
        }
    }
    
    // Getters
    public ViewportState getViewport(int index) {
        return (index >= 0 && index < MAX_VIEWPORTS) ? viewports[index] : null;
    }
    
    public ScissorState getScissor(int index) {
        return (index >= 0 && index < MAX_VIEWPORTS) ? scissors[index] : null;
    }
    
    public boolean isScissorTestEnabled() { return scissorTestEnabled; }
    public int getActiveViewportCount() { return activeViewportCount; }
    
    /**
     * Get primary viewport dimensions.
     */
    public int getViewportWidth() { return (int) viewports[0].width; }
    public int getViewportHeight() { return (int) viewports[0].height; }
    
    // ========================================================================
    // CLEAR STATE
    // ========================================================================
    
    private final float[] clearColor = {0.0f, 0.0f, 0.0f, 1.0f};
    private float clearDepth = 1.0f;
    private int clearStencil = 0;
    
    public void clearColor(float r, float g, float b, float a) {
        clearColor[0] = r;
        clearColor[1] = g;
        clearColor[2] = b;
        clearColor[3] = a;
        markDirty(DirtyFlags.CLEAR_VALUES);
    }
    
    public void clearDepthf(float depth) {
        clearDepth = depth;
        markDirty(DirtyFlags.CLEAR_VALUES);
    }
    
    public void clearStencil(int stencil) {
        clearStencil = stencil;
        markDirty(DirtyFlags.CLEAR_VALUES);
    }
    
    public void getClearColor(float[] dest) {
        System.arraycopy(clearColor, 0, dest, 0, 4);
    }
    
    public float getClearDepth() { return clearDepth; }
    public int getClearStencil() { return clearStencil; }
    
    // ========================================================================
    // UNIFORM STATE (Push Constants)
    // ========================================================================
    
    /** Push constant data - expandable buffer */
    private ByteBuffer pushConstantBuffer;
    private int pushConstantSize = 0;
    private static final int INITIAL_PUSH_CONSTANT_SIZE = 256;
    private static final int MAX_PUSH_CONSTANT_SIZE = 256; // Vulkan minimum guarantee
    
    {
        pushConstantBuffer = MemoryUtil.memAlloc(INITIAL_PUSH_CONSTANT_SIZE);
    }
    
    /**
     * Set uniform value (maps to push constants).
     */
    public void uniform1f(int location, float v0) {
        ensurePushConstantCapacity(location + 4);
        pushConstantBuffer.putFloat(location, v0);
        updatePushConstantSize(location + 4);
        markDirty(DirtyFlags.PUSH_CONSTANTS);
    }
    
    public void uniform2f(int location, float v0, float v1) {
        ensurePushConstantCapacity(location + 8);
        pushConstantBuffer.putFloat(location, v0);
        pushConstantBuffer.putFloat(location + 4, v1);
        updatePushConstantSize(location + 8);
        markDirty(DirtyFlags.PUSH_CONSTANTS);
    }
    
    public void uniform3f(int location, float v0, float v1, float v2) {
        ensurePushConstantCapacity(location + 12);
        pushConstantBuffer.putFloat(location, v0);
        pushConstantBuffer.putFloat(location + 4, v1);
        pushConstantBuffer.putFloat(location + 8, v2);
        updatePushConstantSize(location + 12);
        markDirty(DirtyFlags.PUSH_CONSTANTS);
    }
    
    public void uniform4f(int location, float v0, float v1, float v2, float v3) {
        ensurePushConstantCapacity(location + 16);
        pushConstantBuffer.putFloat(location, v0);
        pushConstantBuffer.putFloat(location + 4, v1);
        pushConstantBuffer.putFloat(location + 8, v2);
        pushConstantBuffer.putFloat(location + 12, v3);
        updatePushConstantSize(location + 16);
        markDirty(DirtyFlags.PUSH_CONSTANTS);
    }
    
    public void uniform1i(int location, int v0) {
        ensurePushConstantCapacity(location + 4);
        pushConstantBuffer.putInt(location, v0);
        updatePushConstantSize(location + 4);
        markDirty(DirtyFlags.PUSH_CONSTANTS);
    }
    
    public void uniform2i(int location, int v0, int v1) {
        ensurePushConstantCapacity(location + 8);
        pushConstantBuffer.putInt(location, v0);
        pushConstantBuffer.putInt(location + 4, v1);
        updatePushConstantSize(location + 8);
        markDirty(DirtyFlags.PUSH_CONSTANTS);
    }
    
    public void uniform3i(int location, int v0, int v1, int v2) {
        ensurePushConstantCapacity(location + 12);
        pushConstantBuffer.putInt(location, v0);
        pushConstantBuffer.putInt(location + 4, v1);
        pushConstantBuffer.putInt(location + 8, v2);
        updatePushConstantSize(location + 12);
        markDirty(DirtyFlags.PUSH_CONSTANTS);
    }
    
    public void uniform4i(int location, int v0, int v1, int v2, int v3) {
        ensurePushConstantCapacity(location + 16);
        pushConstantBuffer.putInt(location, v0);
        pushConstantBuffer.putInt(location + 4, v1);
        pushConstantBuffer.putInt(location + 8, v2);
        pushConstantBuffer.putInt(location + 12, v3);
        updatePushConstantSize(location + 16);
        markDirty(DirtyFlags.PUSH_CONSTANTS);
    }
    
    public void uniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        ensurePushConstantCapacity(location + 64);
        
        if (transpose) {
            // Transpose while copying
            for (int col = 0; col < 4; col++) {
                for (int row = 0; row < 4; row++) {
                    pushConstantBuffer.putFloat(location + (row * 4 + col) * 4, value.get(col * 4 + row));
                }
            }
        } else {
            for (int i = 0; i < 16; i++) {
                pushConstantBuffer.putFloat(location + i * 4, value.get(i));
            }
        }
        
        updatePushConstantSize(location + 64);
        markDirty(DirtyFlags.PUSH_CONSTANTS);
    }
    
    public void uniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {
        // Pad to std140 layout (vec4 per row)
        ensurePushConstantCapacity(location + 48);
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int srcIndex = transpose ? (col * 3 + row) : (row * 3 + col);
                pushConstantBuffer.putFloat(location + (row * 4 + col) * 4, value.get(srcIndex));
            }
            // Padding
            pushConstantBuffer.putFloat(location + (row * 4 + 3) * 4, 0.0f);
        }
        
        updatePushConstantSize(location + 48);
        markDirty(DirtyFlags.PUSH_CONSTANTS);
    }
    
    private void ensurePushConstantCapacity(int required) {
        if (required > pushConstantBuffer.capacity()) {
            int newSize = Math.min(MAX_PUSH_CONSTANT_SIZE, 
                                   Integer.highestOneBit(required - 1) << 1);
            ByteBuffer newBuffer = MemoryUtil.memAlloc(newSize);
            pushConstantBuffer.rewind();
            newBuffer.put(pushConstantBuffer);
            MemoryUtil.memFree(pushConstantBuffer);
            pushConstantBuffer = newBuffer;
        }
    }
    
    private void updatePushConstantSize(int offset) {
        pushConstantSize = Math.max(pushConstantSize, offset);
    }
    
    /**
     * Get push constant buffer for Vulkan commands.
     */
    public ByteBuffer getPushConstantBuffer() {
        pushConstantBuffer.limit(pushConstantSize);
        pushConstantBuffer.rewind();
        return pushConstantBuffer;
    }
    
    public int getPushConstantSize() {
        return pushConstantSize;
    }
    
    public void clearPushConstants() {
        pushConstantSize = 0;
        MemoryUtil.memSet(pushConstantBuffer, 0);
    }
    
    // ========================================================================
    // PIPELINE STATE HASH COMPUTATION
    // ========================================================================
    
    /**
     * Compute a hash of all pipeline-relevant state for caching.
     * This hash can be used to look up pre-created VkPipeline objects.
     */
    public long computePipelineStateHash() {
        if (pipelineHashValid) {
            return cachedPipelineHash;
        }
        
        long hash = 17;
        
        // Program
        hash = hash * 31 + currentProgram;
        
        // Vertex input
        hash = hash * 31 + computeVertexInputHash();
        
        // Input assembly
        hash = hash * 31 + getVkPrimitiveTopology();
        hash = hash * 31 + (primitiveRestartEnabled ? 1 : 0);
        
        // Rasterization
        hash = hash * 31 + (rasterizerDiscardEnabled ? 1 : 0);
        hash = hash * 31 + getVkPolygonMode();
        hash = hash * 31 + getVkCullMode();
        hash = hash * 31 + getVkFrontFace();
        hash = hash * 31 + (depthClampEnabled ? 1 : 0);
        hash = hash * 31 + (depthBiasEnabled ? 1 : 0);
        
        // Depth stencil
        hash = hash * 31 + (depthTestEnabled ? 1 : 0);
        hash = hash * 31 + (depthWriteEnabled ? 1 : 0);
        hash = hash * 31 + glCompareToVk(depthFunc);
        hash = hash * 31 + (depthBoundsTestEnabled ? 1 : 0);
        hash = hash * 31 + (stencilTestEnabled ? 1 : 0);
        if (stencilTestEnabled) {
            hash = hash * 31 + stencilFront.computeHash();
            hash = hash * 31 + stencilBack.computeHash();
        }
        
        // Color blend (per attachment)
        for (int i = 0; i < MAX_COLOR_ATTACHMENTS; i++) {
            hash = hash * 31 + attachmentBlendStates[i].computeHash();
        }
        hash = hash * 31 + (logicOpEnabled ? getVkLogicOp() : 0);
        
        // Multisampling
        hash = hash * 31 + sampleCount;
        hash = hash * 31 + (sampleShadingEnabled ? Float.floatToIntBits(minSampleShading) : 0);
        hash = hash * 31 + (alphaToCoverageEnabled ? 1 : 0);
        hash = hash * 31 + (alphaToOneEnabled ? 1 : 0);
        
        // Tessellation
        if (primitiveTopology == GL.Primitive.PATCHES) {
            hash = hash * 31 + patchControlPoints;
        }
        
        cachedPipelineHash = hash;
        pipelineHashValid = true;
        return hash;
    }
    
    // ========================================================================
    // STATE SNAPSHOT AND RESTORE
    // ========================================================================
    
    /**
     * Immutable snapshot of complete state for save/restore operations.
     */
    public record StateSnapshot(
        // Program
        long program,
        
        // Blend
        boolean[] blendEnabled,
        int[] blendSrcRGB, int[] blendDstRGB,
        int[] blendSrcAlpha, int[] blendDstAlpha,
        int[] blendOpRGB, int[] blendOpAlpha,
        int[] colorMasks,
        float[] blendConstants,
        
        // Depth
        boolean depthTest, boolean depthWrite, int depthFunc,
        float depthNear, float depthFar,
        boolean depthClamp, boolean depthBias,
        float biasConstant, float biasSlope, float biasClamp,
        
        // Stencil
        boolean stencilTest,
        int frontFailOp, int frontPassOp, int frontDepthFailOp, int frontCompareOp,
        int frontCompareMask, int frontWriteMask, int frontRef,
        int backFailOp, int backPassOp, int backDepthFailOp, int backCompareOp,
        int backCompareMask, int backWriteMask, int backRef,
        
        // Rasterization
        boolean cullFace, int cullMode, int frontFaceMode,
        int polygonMode, float lineWidth,
        boolean rasterizerDiscard,
        
        // Viewport/Scissor
        float[] viewportData, int[] scissorData,
        boolean scissorTest,
        
        // Primitive
        int topology, boolean primitiveRestart, int patchVertices,
        
        // Multisampling
        int samples, boolean sampleShading, float minSampleShading,
        boolean alphaToCoverage, boolean alphaToOne
    ) {
        public static StateSnapshot capture(VulkanState state) {
            // Capture blend state
            boolean[] blendEnabled = new boolean[MAX_COLOR_ATTACHMENTS];
            int[] srcRGB = new int[MAX_COLOR_ATTACHMENTS];
            int[] dstRGB = new int[MAX_COLOR_ATTACHMENTS];
            int[] srcAlpha = new int[MAX_COLOR_ATTACHMENTS];
            int[] dstAlpha = new int[MAX_COLOR_ATTACHMENTS];
            int[] opRGB = new int[MAX_COLOR_ATTACHMENTS];
            int[] opAlpha = new int[MAX_COLOR_ATTACHMENTS];
            int[] colorMasks = new int[MAX_COLOR_ATTACHMENTS];
            
            for (int i = 0; i < MAX_COLOR_ATTACHMENTS; i++) {
                AttachmentBlendState abs = state.attachmentBlendStates[i];
                blendEnabled[i] = abs.enabled;
                srcRGB[i] = abs.srcRGB;
                dstRGB[i] = abs.dstRGB;
                srcAlpha[i] = abs.srcAlpha;
                dstAlpha[i] = abs.dstAlpha;
                opRGB[i] = abs.opRGB;
                opAlpha[i] = abs.opAlpha;
                colorMasks[i] = abs.colorMask;
            }
            
            // Capture viewport/scissor
            float[] viewportData = new float[MAX_VIEWPORTS * 6];
            int[] scissorData = new int[MAX_VIEWPORTS * 4];
            for (int i = 0; i < MAX_VIEWPORTS; i++) {
                ViewportState vp = state.viewports[i];
                viewportData[i * 6] = vp.x;
                viewportData[i * 6 + 1] = vp.y;
                viewportData[i * 6 + 2] = vp.width;
                viewportData[i * 6 + 3] = vp.height;
                viewportData[i * 6 + 4] = vp.minDepth;
                viewportData[i * 6 + 5] = vp.maxDepth;
                
                ScissorState sc = state.scissors[i];
                scissorData[i * 4] = sc.x;
                scissorData[i * 4 + 1] = sc.y;
                scissorData[i * 4 + 2] = sc.width;
                scissorData[i * 4 + 3] = sc.height;
            }
            
            return new StateSnapshot(
                state.currentProgram,
                blendEnabled, srcRGB, dstRGB, srcAlpha, dstAlpha, opRGB, opAlpha, colorMasks,
                state.blendConstants.clone(),
                state.depthTestEnabled, state.depthWriteEnabled, state.depthFunc,
                state.depthRangeNear, state.depthRangeFar,
                state.depthClampEnabled, state.depthBiasEnabled,
                state.depthBiasConstant, state.depthBiasSlope, state.depthBiasClamp,
                state.stencilTestEnabled,
                state.stencilFront.failOp, state.stencilFront.passOp, 
                state.stencilFront.depthFailOp, state.stencilFront.compareOp,
                state.stencilFront.compareMask, state.stencilFront.writeMask, state.stencilFront.reference,
                state.stencilBack.failOp, state.stencilBack.passOp,
                state.stencilBack.depthFailOp, state.stencilBack.compareOp,
                state.stencilBack.compareMask, state.stencilBack.writeMask, state.stencilBack.reference,
                state.cullFaceEnabled, state.cullFaceMode, state.frontFace,
                state.polygonModeFront, state.lineWidth,
                state.rasterizerDiscardEnabled,
                viewportData, scissorData, state.scissorTestEnabled,
                state.primitiveTopology, state.primitiveRestartEnabled, state.patchControlPoints,
                state.sampleCount, state.sampleShadingEnabled, state.minSampleShading,
                state.alphaToCoverageEnabled, state.alphaToOneEnabled
            );
        }
    }
    
    /**
     * Capture current state as snapshot.
     */
    public StateSnapshot captureState() {
        return StateSnapshot.capture(this);
    }
    
    /**
     * Restore state from snapshot.
     */
    public void restoreState(StateSnapshot snapshot) {
        currentProgram = snapshot.program();
        
        // Blend
        for (int i = 0; i < MAX_COLOR_ATTACHMENTS; i++) {
            AttachmentBlendState abs = attachmentBlendStates[i];
            abs.enabled = snapshot.blendEnabled()[i];
            abs.srcRGB = snapshot.blendSrcRGB()[i];
            abs.dstRGB = snapshot.blendDstRGB()[i];
            abs.srcAlpha = snapshot.blendSrcAlpha()[i];
            abs.dstAlpha = snapshot.blendDstAlpha()[i];
            abs.opRGB = snapshot.blendOpRGB()[i];
            abs.opAlpha = snapshot.blendOpAlpha()[i];
            abs.colorMask = snapshot.colorMasks()[i];
        }
        System.arraycopy(snapshot.blendConstants(), 0, blendConstants, 0, 4);
        
        // Depth
        depthTestEnabled = snapshot.depthTest();
        depthWriteEnabled = snapshot.depthWrite();
        depthFunc = snapshot.depthFunc();
        depthRangeNear = snapshot.depthNear();
        depthRangeFar = snapshot.depthFar();
        depthClampEnabled = snapshot.depthClamp();
        depthBiasEnabled = snapshot.depthBias();
        depthBiasConstant = snapshot.biasConstant();
        depthBiasSlope = snapshot.biasSlope();
        depthBiasClamp = snapshot.biasClamp();
        
        // Stencil
        stencilTestEnabled = snapshot.stencilTest();
        stencilFront.failOp = snapshot.frontFailOp();
        stencilFront.passOp = snapshot.frontPassOp();
        stencilFront.depthFailOp = snapshot.frontDepthFailOp();
        stencilFront.compareOp = snapshot.frontCompareOp();
        stencilFront.compareMask = snapshot.frontCompareMask();
        stencilFront.writeMask = snapshot.frontWriteMask();
        stencilFront.reference = snapshot.frontRef();
        stencilBack.failOp = snapshot.backFailOp();
        stencilBack.passOp = snapshot.backPassOp();
        stencilBack.depthFailOp = snapshot.backDepthFailOp();
        stencilBack.compareOp = snapshot.backCompareOp();
        stencilBack.compareMask = snapshot.backCompareMask();
        stencilBack.writeMask = snapshot.backWriteMask();
        stencilBack.reference = snapshot.backRef();
        
        // Rasterization
        cullFaceEnabled = snapshot.cullFace();
        cullFaceMode = snapshot.cullMode();
        frontFace = snapshot.frontFaceMode();
        polygonModeFront = snapshot.polygonMode();
        polygonModeBack = snapshot.polygonMode();
        lineWidth = snapshot.lineWidth();
        rasterizerDiscardEnabled = snapshot.rasterizerDiscard();
        
        // Viewport/Scissor
        float[] vpData = snapshot.viewportData();
        int[] scData = snapshot.scissorData();
        for (int i = 0; i < MAX_VIEWPORTS; i++) {
            viewports[i].x = vpData[i * 6];
            viewports[i].y = vpData[i * 6 + 1];
            viewports[i].width = vpData[i * 6 + 2];
            viewports[i].height = vpData[i * 6 + 3];
            viewports[i].minDepth = vpData[i * 6 + 4];
            viewports[i].maxDepth = vpData[i * 6 + 5];
            
            scissors[i].x = scData[i * 4];
            scissors[i].y = scData[i * 4 + 1];
            scissors[i].width = scData[i * 4 + 2];
            scissors[i].height = scData[i * 4 + 3];
        }
        scissorTestEnabled = snapshot.scissorTest();
        
        // Primitive
        primitiveTopology = snapshot.topology();
        primitiveRestartEnabled = snapshot.primitiveRestart();
        patchControlPoints = snapshot.patchVertices();
        
        // Multisampling
        sampleCount = snapshot.samples();
        sampleShadingEnabled = snapshot.sampleShading();
        minSampleShading = snapshot.minSampleShading();
        alphaToCoverageEnabled = snapshot.alphaToCoverage();
        alphaToOneEnabled = snapshot.alphaToOne();
        
        // Mark all as dirty
        markDirty(DirtyFlags.ALL);
    }
    
    // ========================================================================
    // RESET TO DEFAULT STATE
    // ========================================================================
    
    /**
     * Reset all state to OpenGL defaults.
     */
    public void reset() {
        // Texture bindings
        activeTextureUnit = 0;
        Arrays.fill(boundTextures, 0);
        Arrays.fill(boundSamplers, 0);
        
        // Buffer bindings
        Arrays.fill(boundBuffersByTarget, 0);
        for (IndexedBufferBinding b : uniformBufferBindings) b.clear();
        for (IndexedBufferBinding b : shaderStorageBindings) b.clear();
        for (IndexedBufferBinding b : transformFeedbackBindings) b.clear();
        for (IndexedBufferBinding b : atomicCounterBindings) b.clear();
        for (VertexBufferBinding b : vertexBufferBindings) b.clear();
        
        // Vertex input
        enabledAttribMask = 0;
        for (VertexAttrib a : vertexAttribs) a.reset();
        
        // VAO
        currentVAO = 0;
        
        // Program
        currentProgram = 0;
        
        // Blend
        for (AttachmentBlendState abs : attachmentBlendStates) abs.reset();
        Arrays.fill(blendConstants, 0.0f);
        
        // Depth
        depthTestEnabled = false;
        depthWriteEnabled = true;
        depthFunc = GL.Compare.LESS;
        depthRangeNear = 0.0f;
        depthRangeFar = 1.0f;
        depthClampEnabled = false;
        depthBiasEnabled = false;
        depthBiasConstant = 0.0f;
        depthBiasSlope = 0.0f;
        depthBiasClamp = 0.0f;
        depthBoundsTestEnabled = false;
        depthBoundsMin = 0.0f;
        depthBoundsMax = 1.0f;
        
        // Stencil
        stencilTestEnabled = false;
        stencilFront.reset();
        stencilBack.reset();
        
        // Cull/Polygon
        cullFaceEnabled = false;
        cullFaceMode = GL.Face.BACK;
        frontFace = GL.Face.CCW;
        polygonModeFront = GL.Polygon.FILL;
        polygonModeBack = GL.Polygon.FILL;
        lineWidth = 1.0f;
        pointSize = 1.0f;
        rasterizerDiscardEnabled = false;
        
        // Logic op
        logicOpEnabled = false;
        logicOp = GL.Logic.COPY;
        
        // Multisampling
        sampleCount = 1;
        sampleShadingEnabled = false;
        minSampleShading = 1.0f;
        alphaToCoverageEnabled = false;
        alphaToOneEnabled = false;
        sampleMaskEnabled = false;
        Arrays.fill(sampleMask, 0xFFFFFFFF);
        
        // Primitive (continuing reset())
        primitiveTopology = GL.Primitive.TRIANGLES;
        primitiveRestartEnabled = false;
        patchControlPoints = 3;
        
        // Viewport/Scissor
        for (ViewportState vp : viewports) {
            vp.x = 0;
            vp.y = 0;
            vp.width = 1;
            vp.height = 1;
            vp.minDepth = 0.0f;
            vp.maxDepth = 1.0f;
        }
        for (ScissorState sc : scissors) {
            sc.x = 0;
            sc.y = 0;
            sc.width = 1;
            sc.height = 1;
        }
        scissorTestEnabled = false;
        activeViewportCount = 1;
        
        // Clear values
        clearColor[0] = 0.0f;
        clearColor[1] = 0.0f;
        clearColor[2] = 0.0f;
        clearColor[3] = 0.0f;
        clearDepth = 1.0f;
        clearStencil = 0;
        
        // Push constants
        clearPushConstants();
        
        // Capabilities
        enabledCapabilities = 0L;
        
        // Descriptor sets
        Arrays.fill(boundDescriptorSets, 0);
        descriptorSetCount = 0;
        
        // Framebuffer
        currentDrawFramebuffer = 0;
        currentReadFramebuffer = 0;
        
        // Transform feedback
        transformFeedbackActive = false;
        transformFeedbackPaused = false;
        
        // Queries
        Arrays.fill(activeQueries, 0);
        
        // Invalidate caches
        pipelineHashValid = false;
        vertexInputHashValid = false;
        cachedPipelineHash = 0;
        cachedVertexInputHash = 0;
        
        // Mark everything dirty
        markDirty(DirtyFlags.ALL);
        incrementVersion();
    }
    
    // ========================================================================
    // CAPABILITY STATE (GL Enable/Disable)
    // ========================================================================
    
    /**
     * Bit-packed capability flags for O(1) enable/disable checks.
     * Maps GL capability enums to bit positions for fast lookup.
     */
    private long enabledCapabilities = 0L;
    
    /**
     * Capability bit positions.
     */
    private static final class CapabilityBits {
        static final int BLEND = 0;
        static final int DEPTH_TEST = 1;
        static final int STENCIL_TEST = 2;
        static final int CULL_FACE = 3;
        static final int SCISSOR_TEST = 4;
        static final int POLYGON_OFFSET_FILL = 5;
        static final int POLYGON_OFFSET_LINE = 6;
        static final int POLYGON_OFFSET_POINT = 7;
        static final int MULTISAMPLE = 8;
        static final int SAMPLE_ALPHA_TO_COVERAGE = 9;
        static final int SAMPLE_ALPHA_TO_ONE = 10;
        static final int SAMPLE_COVERAGE = 11;
        static final int SAMPLE_SHADING = 12;
        static final int SAMPLE_MASK = 13;
        static final int PROGRAM_POINT_SIZE = 14;
        static final int DEPTH_CLAMP = 15;
        static final int PRIMITIVE_RESTART = 16;
        static final int PRIMITIVE_RESTART_FIXED_INDEX = 17;
        static final int RASTERIZER_DISCARD = 18;
        static final int COLOR_LOGIC_OP = 19;
        static final int LINE_SMOOTH = 20;
        static final int POLYGON_SMOOTH = 21;
        static final int FRAMEBUFFER_SRGB = 22;
        static final int DITHER = 23;
        static final int DEBUG_OUTPUT = 24;
        static final int DEBUG_OUTPUT_SYNCHRONOUS = 25;
        static final int TEXTURE_CUBE_MAP_SEAMLESS = 26;
        static final int CLIP_DISTANCE_BASE = 27; // Uses 27-34 for clip distances 0-7
        static final int UNKNOWN = -1;
        
        private CapabilityBits() {}
    }
    
    /**
     * Map GL capability enum to bit position.
     */
    private static int capabilityToBit(int cap) {
        return switch (cap) {
            case GL.Capability.BLEND -> CapabilityBits.BLEND;
            case GL.Capability.DEPTH_TEST -> CapabilityBits.DEPTH_TEST;
            case GL.Capability.STENCIL_TEST -> CapabilityBits.STENCIL_TEST;
            case GL.Capability.CULL_FACE -> CapabilityBits.CULL_FACE;
            case GL.Capability.SCISSOR_TEST -> CapabilityBits.SCISSOR_TEST;
            case GL.Capability.POLYGON_OFFSET_FILL -> CapabilityBits.POLYGON_OFFSET_FILL;
            case GL.Capability.POLYGON_OFFSET_LINE -> CapabilityBits.POLYGON_OFFSET_LINE;
            case GL.Capability.POLYGON_OFFSET_POINT -> CapabilityBits.POLYGON_OFFSET_POINT;
            case GL.Capability.MULTISAMPLE -> CapabilityBits.MULTISAMPLE;
            case GL.Capability.SAMPLE_ALPHA_TO_COVERAGE -> CapabilityBits.SAMPLE_ALPHA_TO_COVERAGE;
            case GL.Capability.SAMPLE_ALPHA_TO_ONE -> CapabilityBits.SAMPLE_ALPHA_TO_ONE;
            case GL.Capability.SAMPLE_COVERAGE -> CapabilityBits.SAMPLE_COVERAGE;
            case GL.Capability.SAMPLE_SHADING -> CapabilityBits.SAMPLE_SHADING;
            case GL.Capability.SAMPLE_MASK -> CapabilityBits.SAMPLE_MASK;
            case GL.Capability.PROGRAM_POINT_SIZE -> CapabilityBits.PROGRAM_POINT_SIZE;
            case GL.Capability.DEPTH_CLAMP -> CapabilityBits.DEPTH_CLAMP;
            case GL.Capability.PRIMITIVE_RESTART -> CapabilityBits.PRIMITIVE_RESTART;
            case GL.Capability.PRIMITIVE_RESTART_FIXED_INDEX -> CapabilityBits.PRIMITIVE_RESTART_FIXED_INDEX;
            case GL.Capability.RASTERIZER_DISCARD -> CapabilityBits.RASTERIZER_DISCARD;
            case GL.Capability.COLOR_LOGIC_OP -> CapabilityBits.COLOR_LOGIC_OP;
            case GL.Capability.LINE_SMOOTH -> CapabilityBits.LINE_SMOOTH;
            case GL.Capability.POLYGON_SMOOTH -> CapabilityBits.POLYGON_SMOOTH;
            case GL.Capability.FRAMEBUFFER_SRGB -> CapabilityBits.FRAMEBUFFER_SRGB;
            case GL.Capability.DITHER -> CapabilityBits.DITHER;
            case GL.Capability.DEBUG_OUTPUT -> CapabilityBits.DEBUG_OUTPUT;
            case GL.Capability.DEBUG_OUTPUT_SYNCHRONOUS -> CapabilityBits.DEBUG_OUTPUT_SYNCHRONOUS;
            case GL.Capability.TEXTURE_CUBE_MAP_SEAMLESS -> CapabilityBits.TEXTURE_CUBE_MAP_SEAMLESS;
            default -> {
                // Check for clip distances (GL_CLIP_DISTANCE0 + i)
                if (cap >= GL.Capability.CLIP_DISTANCE0 && cap < GL.Capability.CLIP_DISTANCE0 + MAX_CLIP_DISTANCES) {
                    yield CapabilityBits.CLIP_DISTANCE_BASE + (cap - GL.Capability.CLIP_DISTANCE0);
                }
                yield CapabilityBits.UNKNOWN;
            }
        };
    }
    
    /**
     * Enable a GL capability.
     */
    public void enable(int cap) {
        int bit = capabilityToBit(cap);
        if (bit == CapabilityBits.UNKNOWN) return;
        
        long mask = 1L << bit;
        if ((enabledCapabilities & mask) == 0) {
            enabledCapabilities |= mask;
            
            // Update derived state
            updateCapabilityDerivedState(cap, true);
        }
    }
    
    /**
     * Disable a GL capability.
     */
    public void disable(int cap) {
        int bit = capabilityToBit(cap);
        if (bit == CapabilityBits.UNKNOWN) return;
        
        long mask = 1L << bit;
        if ((enabledCapabilities & mask) != 0) {
            enabledCapabilities &= ~mask;
            
            // Update derived state
            updateCapabilityDerivedState(cap, false);
        }
    }
    
    /**
     * Check if a GL capability is enabled.
     */
    public boolean isEnabled(int cap) {
        int bit = capabilityToBit(cap);
        if (bit == CapabilityBits.UNKNOWN) return false;
        return (enabledCapabilities & (1L << bit)) != 0;
    }
    
    /**
     * Enable capability for specific index (e.g., blend for draw buffer i).
     */
    public void enablei(int cap, int index) {
        if (cap == GL.Capability.BLEND && index >= 0 && index < MAX_COLOR_ATTACHMENTS) {
            setBlendEnabledi(index, true);
        } else if (cap == GL.Capability.SCISSOR_TEST && index >= 0 && index < MAX_VIEWPORTS) {
            // Per-viewport scissor (if supported)
            enable(cap);
        }
    }
    
    /**
     * Disable capability for specific index.
     */
    public void disablei(int cap, int index) {
        if (cap == GL.Capability.BLEND && index >= 0 && index < MAX_COLOR_ATTACHMENTS) {
            setBlendEnabledi(index, false);
        }
    }
    
    /**
     * Check if capability is enabled for specific index.
     */
    public boolean isEnabledi(int cap, int index) {
        if (cap == GL.Capability.BLEND && index >= 0 && index < MAX_COLOR_ATTACHMENTS) {
            return attachmentBlendStates[index].enabled;
        }
        return isEnabled(cap);
    }
    
    /**
     * Update derived state when capability changes.
     */
    private void updateCapabilityDerivedState(int cap, boolean enabled) {
        switch (cap) {
            case GL.Capability.BLEND -> {
                setBlendEnabled(enabled);
                markDirty(DirtyFlags.BLEND_STATE);
            }
            case GL.Capability.DEPTH_TEST -> {
                setDepthTestEnabled(enabled);
            }
            case GL.Capability.STENCIL_TEST -> {
                setStencilTestEnabled(enabled);
            }
            case GL.Capability.CULL_FACE -> {
                setCullFaceEnabled(enabled);
            }
            case GL.Capability.SCISSOR_TEST -> {
                setScissorTestEnabled(enabled);
            }
            case GL.Capability.POLYGON_OFFSET_FILL, GL.Capability.POLYGON_OFFSET_LINE, 
                 GL.Capability.POLYGON_OFFSET_POINT -> {
                boolean anyOffset = isEnabled(GL.Capability.POLYGON_OFFSET_FILL) ||
                                   isEnabled(GL.Capability.POLYGON_OFFSET_LINE) ||
                                   isEnabled(GL.Capability.POLYGON_OFFSET_POINT);
                setDepthBiasEnabled(anyOffset);
            }
            case GL.Capability.SAMPLE_SHADING -> {
                setSampleShadingEnabled(enabled);
            }
            case GL.Capability.SAMPLE_ALPHA_TO_COVERAGE -> {
                setAlphaToCoverageEnabled(enabled);
            }
            case GL.Capability.SAMPLE_ALPHA_TO_ONE -> {
                setAlphaToOneEnabled(enabled);
            }
            case GL.Capability.SAMPLE_MASK -> {
                setSampleMaskEnabled(enabled);
            }
            case GL.Capability.DEPTH_CLAMP -> {
                setDepthClampEnabled(enabled);
            }
            case GL.Capability.PRIMITIVE_RESTART, GL.Capability.PRIMITIVE_RESTART_FIXED_INDEX -> {
                setPrimitiveRestartEnabled(enabled);
            }
            case GL.Capability.RASTERIZER_DISCARD -> {
                setRasterizerDiscardEnabled(enabled);
            }
            case GL.Capability.COLOR_LOGIC_OP -> {
                setLogicOpEnabled(enabled);
            }
        }
    }
    
    /**
     * Get enabled capabilities as bitmask.
     */
    public long getEnabledCapabilitiesMask() {
        return enabledCapabilities;
    }
    
    // ========================================================================
    // DESCRIPTOR SET STATE
    // ========================================================================
    
    /** Bound descriptor sets per set index */
    private final long[] boundDescriptorSets = new long[8];
    
    /** Dynamic offsets for descriptor sets */
    private final int[][] descriptorSetDynamicOffsets = new int[8][16];
    private final int[] dynamicOffsetCounts = new int[8];
    
    /** Number of bound descriptor sets */
    private int descriptorSetCount = 0;
    
    /**
     * Bind descriptor set.
     */
    public void bindDescriptorSet(int set, long descriptorSet) {
        if (set >= 0 && set < boundDescriptorSets.length) {
            if (boundDescriptorSets[set] != descriptorSet) {
                boundDescriptorSets[set] = descriptorSet;
                descriptorSetCount = Math.max(descriptorSetCount, set + 1);
                markDirty(DirtyFlags.DESCRIPTORS);
            }
        }
    }
    
    /**
     * Bind descriptor set with dynamic offsets.
     */
    public void bindDescriptorSet(int set, long descriptorSet, int[] dynamicOffsets) {
        if (set >= 0 && set < boundDescriptorSets.length) {
            boundDescriptorSets[set] = descriptorSet;
            if (dynamicOffsets != null && dynamicOffsets.length > 0) {
                int count = Math.min(dynamicOffsets.length, descriptorSetDynamicOffsets[set].length);
                System.arraycopy(dynamicOffsets, 0, descriptorSetDynamicOffsets[set], 0, count);
                dynamicOffsetCounts[set] = count;
            } else {
                dynamicOffsetCounts[set] = 0;
            }
            descriptorSetCount = Math.max(descriptorSetCount, set + 1);
            markDirty(DirtyFlags.DESCRIPTORS);
        }
    }
    
    /**
     * Get bound descriptor set.
     */
    public long getBoundDescriptorSet(int set) {
        return (set >= 0 && set < boundDescriptorSets.length) ? boundDescriptorSets[set] : 0;
    }
    
    /**
     * Get all bound descriptor sets.
     */
    public void getBoundDescriptorSets(long[] dest, int maxSets) {
        int count = Math.min(maxSets, descriptorSetCount);
        System.arraycopy(boundDescriptorSets, 0, dest, 0, count);
    }
    
    /**
     * Get dynamic offsets for a set.
     */
    public int[] getDynamicOffsets(int set) {
        if (set >= 0 && set < dynamicOffsetCounts.length && dynamicOffsetCounts[set] > 0) {
            int[] result = new int[dynamicOffsetCounts[set]];
            System.arraycopy(descriptorSetDynamicOffsets[set], 0, result, 0, dynamicOffsetCounts[set]);
            return result;
        }
        return null;
    }
    
    /**
     * Get descriptor set count.
     */
    public int getDescriptorSetCount() {
        return descriptorSetCount;
    }
    
    /**
     * Unbind all descriptor sets.
     */
    public void unbindAllDescriptorSets() {
        Arrays.fill(boundDescriptorSets, 0);
        Arrays.fill(dynamicOffsetCounts, 0);
        descriptorSetCount = 0;
        markDirty(DirtyFlags.DESCRIPTORS);
    }
    
    // ========================================================================
    // FRAMEBUFFER STATE
    // ========================================================================
    
    /** Framebuffer object storage */
    private final LongObjectMap<FramebufferObject> framebuffers = new LongObjectMap<>(32);
    
    /** Renderbuffer object storage */
    private final LongObjectMap<RenderbufferObject> renderbuffers = new LongObjectMap<>(32);
    
    /** Currently bound framebuffers */
    private long currentDrawFramebuffer = 0;
    private long currentReadFramebuffer = 0;
    
    /** Default framebuffer dimensions */
    private int defaultFramebufferWidth = 1;
    private int defaultFramebufferHeight = 1;
    
    /**
     * Framebuffer object.
     */
    public static final class FramebufferObject {
        // Vulkan handles
        public long vkFramebuffer;
        public long vkRenderPass;
        
        // Attachments
        public final FramebufferAttachment[] colorAttachments = new FramebufferAttachment[MAX_COLOR_ATTACHMENTS];
        public FramebufferAttachment depthAttachment;
        public FramebufferAttachment stencilAttachment;
        public FramebufferAttachment depthStencilAttachment;
        
        // Framebuffer properties
        public int width;
        public int height;
        public int layers = 1;
        public int samples = 1;
        
        // Draw buffers
        public final int[] drawBuffers = new int[MAX_COLOR_ATTACHMENTS];
        public int drawBufferCount = 1;
        public int readBuffer = 0x8CE0; // GL_COLOR_ATTACHMENT0
        
        // Status
        public int status = 0; // 0 = not checked
        
        public FramebufferObject() {
            drawBuffers[0] = 0x8CE0; // GL_COLOR_ATTACHMENT0
            for (int i = 1; i < MAX_COLOR_ATTACHMENTS; i++) {
                drawBuffers[i] = 0; // GL_NONE
            }
        }
        
        /**
         * Check completeness and return status.
         */
        public int checkStatus() {
            // Basic completeness checks
            boolean hasAttachment = false;
            int attachmentWidth = 0;
            int attachmentHeight = 0;
            
            for (FramebufferAttachment ca : colorAttachments) {
                if (ca != null && ca.isValid()) {
                    hasAttachment = true;
                    if (attachmentWidth == 0) {
                        attachmentWidth = ca.width;
                        attachmentHeight = ca.height;
                    }
                }
            }
            
            if (depthStencilAttachment != null && depthStencilAttachment.isValid()) {
                hasAttachment = true;
                if (attachmentWidth == 0) {
                    attachmentWidth = depthStencilAttachment.width;
                    attachmentHeight = depthStencilAttachment.height;
                }
            }
            
            if (!hasAttachment) {
                return 0x8CDB; // GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT
            }
            
            width = attachmentWidth;
            height = attachmentHeight;
            
            status = 0x8CD5; // GL_FRAMEBUFFER_COMPLETE
            return status;
        }
        
        public void reset() {
            vkFramebuffer = 0;
            vkRenderPass = 0;
            Arrays.fill(colorAttachments, null);
            depthAttachment = null;
            stencilAttachment = null;
            depthStencilAttachment = null;
            width = 0;
            height = 0;
            layers = 1;
            samples = 1;
            drawBuffers[0] = 0x8CE0;
            Arrays.fill(drawBuffers, 1, MAX_COLOR_ATTACHMENTS, 0);
            drawBufferCount = 1;
            readBuffer = 0x8CE0;
            status = 0;
        }
    }
    
    /**
     * Framebuffer attachment.
     */
    public static final class FramebufferAttachment {
        public static final int TYPE_NONE = 0;
        public static final int TYPE_TEXTURE = 1;
        public static final int TYPE_RENDERBUFFER = 2;
        
        public int type = TYPE_NONE;
        public long object; // Texture or renderbuffer ID
        public int level; // Mip level for textures
        public int layer; // Layer for array/3D textures
        public int face; // Cube face
        
        // Cached properties
        public int width;
        public int height;
        public int format;
        public int samples = 1;
        
        public boolean isValid() {
            return type != TYPE_NONE && object != 0;
        }
        
        public void attachTexture(long texture, int level, int layer) {
            this.type = TYPE_TEXTURE;
            this.object = texture;
            this.level = level;
            this.layer = layer;
        }
        
        public void attachRenderbuffer(long renderbuffer) {
            this.type = TYPE_RENDERBUFFER;
            this.object = renderbuffer;
            this.level = 0;
            this.layer = 0;
        }
        
        public void detach() {
            type = TYPE_NONE;
            object = 0;
            level = 0;
            layer = 0;
        }
    }
    
    /**
     * Renderbuffer object.
     */
    public static final class RenderbufferObject {
        public long vkImage;
        public long vkMemory;
        public long vkImageView;
        
        public int width;
        public int height;
        public int internalFormat;
        public int samples = 1;
    }
    
    /**
     * Generate framebuffer.
     */
    public long genFramebuffer() {
        long id = nextFramebufferId.getAndIncrement();
        framebuffers.put(id, new FramebufferObject());
        return id;
    }
    
    /**
     * Generate multiple framebuffers.
     */
    public void genFramebuffers(int count, long[] ids) {
        for (int i = 0; i < count; i++) {
            ids[i] = genFramebuffer();
        }
    }
    
    /**
     * Bind framebuffer.
     */
    public void bindFramebuffer(int target, long framebuffer) {
        // target: GL_FRAMEBUFFER (0x8D40), GL_DRAW_FRAMEBUFFER (0x8CA9), GL_READ_FRAMEBUFFER (0x8CA8)
        if (target == 0x8D40) { // GL_FRAMEBUFFER
            if (currentDrawFramebuffer != framebuffer || currentReadFramebuffer != framebuffer) {
                currentDrawFramebuffer = framebuffer;
                currentReadFramebuffer = framebuffer;
                markDirty(DirtyFlags.VIEWPORT); // Framebuffer change may affect viewport
            }
        } else if (target == 0x8CA9) { // GL_DRAW_FRAMEBUFFER
            if (currentDrawFramebuffer != framebuffer) {
                currentDrawFramebuffer = framebuffer;
                markDirty(DirtyFlags.VIEWPORT);
            }
        } else if (target == 0x8CA8) { // GL_READ_FRAMEBUFFER
            currentReadFramebuffer = framebuffer;
        }
    }
    
    /**
     * Get framebuffer object.
     */
    public FramebufferObject getFramebuffer(long id) {
        return framebuffers.get(id);
    }
    
    /**
     * Get current draw framebuffer.
     */
    public long getCurrentDrawFramebuffer() {
        return currentDrawFramebuffer;
    }
    
    /**
     * Get current read framebuffer.
     */
    public long getCurrentReadFramebuffer() {
        return currentReadFramebuffer;
    }
    
    /**
     * Get current draw framebuffer object.
     */
    public FramebufferObject getCurrentDrawFramebufferObject() {
        return currentDrawFramebuffer != 0 ? framebuffers.get(currentDrawFramebuffer) : null;
    }
    
    /**
     * Attach texture to framebuffer.
     */
    public void framebufferTexture2D(int target, int attachment, int textarget, long texture, int level) {
        FramebufferObject fbo = getCurrentDrawFramebufferObject();
        if (fbo == null) return;
        
        int attachmentIndex = attachmentToIndex(attachment);
        if (attachmentIndex >= 0 && attachmentIndex < MAX_COLOR_ATTACHMENTS) {
            if (fbo.colorAttachments[attachmentIndex] == null) {
                fbo.colorAttachments[attachmentIndex] = new FramebufferAttachment();
            }
            if (texture != 0) {
                fbo.colorAttachments[attachmentIndex].attachTexture(texture, level, 0);
                TextureObject tex = textures.get(texture);
                if (tex != null) {
                    fbo.colorAttachments[attachmentIndex].width = Math.max(1, tex.width >> level);
                    fbo.colorAttachments[attachmentIndex].height = Math.max(1, tex.height >> level);
                    fbo.colorAttachments[attachmentIndex].format = tex.internalFormat;
                }
            } else {
                fbo.colorAttachments[attachmentIndex].detach();
            }
        } else if (attachment == 0x8D00) { // GL_DEPTH_ATTACHMENT
            if (fbo.depthAttachment == null) fbo.depthAttachment = new FramebufferAttachment();
            if (texture != 0) {
                fbo.depthAttachment.attachTexture(texture, level, 0);
            } else {
                fbo.depthAttachment.detach();
            }
        } else if (attachment == 0x8D20) { // GL_STENCIL_ATTACHMENT
            if (fbo.stencilAttachment == null) fbo.stencilAttachment = new FramebufferAttachment();
            if (texture != 0) {
                fbo.stencilAttachment.attachTexture(texture, level, 0);
            } else {
                fbo.stencilAttachment.detach();
            }
        } else if (attachment == 0x821A) { // GL_DEPTH_STENCIL_ATTACHMENT
            if (fbo.depthStencilAttachment == null) fbo.depthStencilAttachment = new FramebufferAttachment();
            if (texture != 0) {
                fbo.depthStencilAttachment.attachTexture(texture, level, 0);
            } else {
                fbo.depthStencilAttachment.detach();
            }
        }
        
        fbo.status = 0; // Mark for recheck
    }
    
    /**
     * Convert attachment enum to index.
     */
    private static int attachmentToIndex(int attachment) {
        // GL_COLOR_ATTACHMENT0 = 0x8CE0
        if (attachment >= 0x8CE0 && attachment < 0x8CE0 + MAX_COLOR_ATTACHMENTS) {
            return attachment - 0x8CE0;
        }
        return -1;
    }
    
    /**
     * Generate renderbuffer.
     */
    public long genRenderbuffer() {
        long id = nextRenderbufferId.getAndIncrement();
        renderbuffers.put(id, new RenderbufferObject());
        return id;
    }
    
    /**
     * Delete framebuffer.
     */
    public FramebufferObject deleteFramebuffer(long id) {
        if (currentDrawFramebuffer == id) currentDrawFramebuffer = 0;
        if (currentReadFramebuffer == id) currentReadFramebuffer = 0;
        return framebuffers.remove(id);
    }
    
    /**
     * Delete renderbuffer.
     */
    public RenderbufferObject deleteRenderbuffer(long id) {
        return renderbuffers.remove(id);
    }
    
    /**
     * Set draw buffers.
     */
    public void drawBuffers(int[] bufs) {
        FramebufferObject fbo = getCurrentDrawFramebufferObject();
        if (fbo != null) {
            int count = Math.min(bufs.length, MAX_COLOR_ATTACHMENTS);
            System.arraycopy(bufs, 0, fbo.drawBuffers, 0, count);
            fbo.drawBufferCount = count;
        }
    }
    
    /**
     * Set read buffer.
     */
    public void readBuffer(int mode) {
        FramebufferObject fbo = framebuffers.get(currentReadFramebuffer);
        if (fbo != null) {
            fbo.readBuffer = mode;
        }
    }
    
    /**
     * Set default framebuffer dimensions.
     */
    public void setDefaultFramebufferSize(int width, int height) {
        defaultFramebufferWidth = width;
        defaultFramebufferHeight = height;
    }
    
    /**
     * Get current framebuffer dimensions.
     */
    public int getFramebufferWidth() {
        if (currentDrawFramebuffer == 0) return defaultFramebufferWidth;
        FramebufferObject fbo = framebuffers.get(currentDrawFramebuffer);
        return (fbo != null && fbo.width > 0) ? fbo.width : defaultFramebufferWidth;
    }
    
    public int getFramebufferHeight() {
        if (currentDrawFramebuffer == 0) return defaultFramebufferHeight;
        FramebufferObject fbo = framebuffers.get(currentDrawFramebuffer);
        return (fbo != null && fbo.height > 0) ? fbo.height : defaultFramebufferHeight;
    }
    
    // ========================================================================
    // TRANSFORM FEEDBACK STATE
    // ========================================================================
    
    private final LongObjectMap<TransformFeedbackObject> transformFeedbackObjects = new LongObjectMap<>(8);
    private long currentTransformFeedback = 0;
    private boolean transformFeedbackActive = false;
    private boolean transformFeedbackPaused = false;
    private int transformFeedbackPrimitiveMode = GL.Primitive.TRIANGLES;
    
    /**
     * Transform feedback object.
     */
    public static final class TransformFeedbackObject {
        public final IndexedBufferBinding[] bindings = new IndexedBufferBinding[MAX_TRANSFORM_FEEDBACK_BINDINGS];
        public int primitiveMode;
        public boolean active = false;
        public boolean paused = false;
        
        public TransformFeedbackObject() {
            for (int i = 0; i < bindings.length; i++) {
                bindings[i] = new IndexedBufferBinding();
            }
        }
    }
    
    /**
     * Generate transform feedback object.
     */
    public long genTransformFeedback() {
        long id = nextQueryId.getAndIncrement(); // Reuse query counter
        transformFeedbackObjects.put(id, new TransformFeedbackObject());
        return id;
    }
    
    /**
     * Bind transform feedback.
     */
    public void bindTransformFeedback(int target, long id) {
        if (target == 0x8E22) { // GL_TRANSFORM_FEEDBACK
            currentTransformFeedback = id;
        }
    }
    
    /**
     * Begin transform feedback.
     */
    public void beginTransformFeedback(int primitiveMode) {
        transformFeedbackActive = true;
        transformFeedbackPaused = false;
        transformFeedbackPrimitiveMode = primitiveMode;
        
        if (currentTransformFeedback != 0) {
            TransformFeedbackObject tfo = transformFeedbackObjects.get(currentTransformFeedback);
            if (tfo != null) {
                tfo.active = true;
                tfo.paused = false;
                tfo.primitiveMode = primitiveMode;
            }
        }
    }
    
    /**
     * End transform feedback.
     */
    public void endTransformFeedback() {
        transformFeedbackActive = false;
        transformFeedbackPaused = false;
        
        if (currentTransformFeedback != 0) {
            TransformFeedbackObject tfo = transformFeedbackObjects.get(currentTransformFeedback);
            if (tfo != null) {
                tfo.active = false;
                tfo.paused = false;
            }
        }
    }
    
    /**
     * Pause transform feedback.
     */
    public void pauseTransformFeedback() {
        transformFeedbackPaused = true;
        if (currentTransformFeedback != 0) {
            TransformFeedbackObject tfo = transformFeedbackObjects.get(currentTransformFeedback);
            if (tfo != null) tfo.paused = true;
        }
    }
    
    /**
     * Resume transform feedback.
     */
    public void resumeTransformFeedback() {
        transformFeedbackPaused = false;
        if (currentTransformFeedback != 0) {
            TransformFeedbackObject tfo = transformFeedbackObjects.get(currentTransformFeedback);
            if (tfo != null) tfo.paused = false;
        }
    }
    
    public boolean isTransformFeedbackActive() { return transformFeedbackActive; }
    public boolean isTransformFeedbackPaused() { return transformFeedbackPaused; }
    public int getTransformFeedbackPrimitiveMode() { return transformFeedbackPrimitiveMode; }
    
    // ========================================================================
    // QUERY STATE
    // ========================================================================
    
    /** Query object storage */
    private final LongObjectMap<QueryObject> queries = new LongObjectMap<>(32);
    
    /** Active queries per target */
    private final long[] activeQueries = new long[8];
    
    // Query target indices
    private static final int QUERY_SAMPLES_PASSED = 0;
    private static final int QUERY_ANY_SAMPLES_PASSED = 1;
    private static final int QUERY_ANY_SAMPLES_PASSED_CONSERVATIVE = 2;
    private static final int QUERY_PRIMITIVES_GENERATED = 3;
    private static final int QUERY_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN = 4;
    private static final int QUERY_TIME_ELAPSED = 5;
    private static final int QUERY_TIMESTAMP = 6;
    
    /**
     * Query object.
     */
    public static final class QueryObject {
        public int target;
        public long vkQueryPool;
        public int queryIndex;
        public boolean active = false;
        public boolean resultAvailable = false;
        public long result;
    }
    
    /**
     * Generate query.
     */
    public long genQuery() {
        long id = nextQueryId.getAndIncrement();
        queries.put(id, new QueryObject());
        return id;
    }
    
    /**
     * Begin query.
     */
    public void beginQuery(int target, long id) {
        QueryObject query = queries.get(id);
        if (query != null) {
            query.target = target;
            query.active = true;
            
            int index = queryTargetToIndex(target);
            if (index >= 0) {
                activeQueries[index] = id;
            }
        }
    }
    
    /**
     * End query.
     */
    public void endQuery(int target) {
        int index = queryTargetToIndex(target);
        if (index >= 0 && activeQueries[index] != 0) {
            QueryObject query = queries.get(activeQueries[index]);
            if (query != null) {
                query.active = false;
            }
            activeQueries[index] = 0;
        }
    }
    
    /**
     * Get active query for target.
     */
    public long getActiveQuery(int target) {
        int index = queryTargetToIndex(target);
        return (index >= 0) ? activeQueries[index] : 0;
    }
    
    /**
     * Delete query.
     */
    public QueryObject deleteQuery(long id) {
        return queries.remove(id);
    }
    
    private static int queryTargetToIndex(int target) {
        return switch (target) {
            case 0x8914 -> QUERY_SAMPLES_PASSED; // GL_SAMPLES_PASSED
            case 0x8C2F -> QUERY_ANY_SAMPLES_PASSED; // GL_ANY_SAMPLES_PASSED
            case 0x8D6A -> QUERY_ANY_SAMPLES_PASSED_CONSERVATIVE; // GL_ANY_SAMPLES_PASSED_CONSERVATIVE
            case 0x8C87 -> QUERY_PRIMITIVES_GENERATED; // GL_PRIMITIVES_GENERATED
            case 0x8C88 -> QUERY_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN;
            case 0x88BF -> QUERY_TIME_ELAPSED; // GL_TIME_ELAPSED
            case 0x8E28 -> QUERY_TIMESTAMP; // GL_TIMESTAMP
            default -> -1;
        };
    }
    
    // ========================================================================
    // VULKAN PIPELINE CREATE INFO HELPERS
    // ========================================================================
    
    /**
     * Populate VkPipelineVertexInputStateCreateInfo from current state.
     */
    public void populateVertexInputState(
            VkPipelineVertexInputStateCreateInfo vertexInputInfo,
            VkVertexInputBindingDescription.Buffer bindingDescs,
            VkVertexInputAttributeDescription.Buffer attributeDescs) {
        
        int bindingCount = 0;
        int attributeCount = 0;
        
        // Collect unique bindings
        boolean[] usedBindings = new boolean[MAX_VERTEX_BINDINGS];
        int mask = enabledAttribMask;
        
        while (mask != 0) {
            int attribIndex = Integer.numberOfTrailingZeros(mask);
            mask &= (mask - 1);
            
            VertexAttrib attrib = vertexAttribs[attribIndex];
            int binding = attrib.binding;
            
            // Add binding description if not already added
            if (!usedBindings[binding]) {
                usedBindings[binding] = true;
                
                VertexBufferBinding vbb = vertexBufferBindings[binding];
                VkVertexInputBindingDescription bd = bindingDescs.get(bindingCount);
                bd.binding(binding);
                bd.stride(vbb.stride > 0 ? vbb.stride : attrib.legacyStride);
                bd.inputRate(vbb.divisor > 0 ? VK10.VK_VERTEX_INPUT_RATE_INSTANCE 
                                              : VK10.VK_VERTEX_INPUT_RATE_VERTEX);
                bindingCount++;
            }
            
            // Add attribute description
            VkVertexInputAttributeDescription ad = attributeDescs.get(attributeCount);
            ad.location(attribIndex);
            ad.binding(binding);
            ad.format(attrib.toVkFormat());
            ad.offset(attrib.relativeOffset);
            attributeCount++;
        }
        
        bindingDescs.limit(bindingCount);
        attributeDescs.limit(attributeCount);
        
        vertexInputInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
        vertexInputInfo.pVertexBindingDescriptions(bindingDescs);
        vertexInputInfo.pVertexAttributeDescriptions(attributeDescs);
    }
    
    /**
     * Populate VkPipelineInputAssemblyStateCreateInfo.
     */
    public void populateInputAssemblyState(VkPipelineInputAssemblyStateCreateInfo inputAssembly) {
        inputAssembly.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
        inputAssembly.topology(getVkPrimitiveTopology());
        inputAssembly.primitiveRestartEnable(primitiveRestartEnabled);
    }
    
    /**
     * Populate VkPipelineTessellationStateCreateInfo.
     */
    public void populateTessellationState(VkPipelineTessellationStateCreateInfo tessellation) {
        tessellation.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_TESSELLATION_STATE_CREATE_INFO);
        tessellation.patchControlPoints(patchControlPoints);
    }
    
    /**
     * Populate VkPipelineViewportStateCreateInfo.
     */
    public void populateViewportState(
            VkPipelineViewportStateCreateInfo viewportState,
            VkViewport.Buffer viewportBuffer,
            VkRect2D.Buffer scissorBuffer) {
        
        for (int i = 0; i < activeViewportCount; i++) {
            ViewportState vp = viewports[i];
            VkViewport vkVp = viewportBuffer.get(i);
            vkVp.x(vp.x);
            vkVp.y(vp.y);
            vkVp.width(vp.width);
            vkVp.height(vp.height);
            vkVp.minDepth(vp.minDepth);
            vkVp.maxDepth(vp.maxDepth);
            
            ScissorState sc = scissors[i];
            VkRect2D vkSc = scissorBuffer.get(i);
            vkSc.offset().set(sc.x, sc.y);
            vkSc.extent().set(sc.width, sc.height);
        }
        
        viewportBuffer.limit(activeViewportCount);
        scissorBuffer.limit(activeViewportCount);
        
        viewportState.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
        viewportState.pViewports(viewportBuffer);
        viewportState.pScissors(scissorBuffer);
    }
    
    /**
     * Populate VkPipelineRasterizationStateCreateInfo.
     */
    public void populateRasterizationState(VkPipelineRasterizationStateCreateInfo rasterization) {
        rasterization.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
        rasterization.depthClampEnable(depthClampEnabled);
        rasterization.rasterizerDiscardEnable(rasterizerDiscardEnabled);
        rasterization.polygonMode(getVkPolygonMode());
        rasterization.cullMode(getVkCullMode());
        rasterization.frontFace(getVkFrontFace());
        rasterization.depthBiasEnable(depthBiasEnabled);
        rasterization.depthBiasConstantFactor(depthBiasConstant);
        rasterization.depthBiasClamp(depthBiasClamp);
        rasterization.depthBiasSlopeFactor(depthBiasSlope);
        rasterization.lineWidth(lineWidth);
    }
    
    /**
     * Populate VkPipelineMultisampleStateCreateInfo.
     */
    public void populateMultisampleState(VkPipelineMultisampleStateCreateInfo multisample) {
        multisample.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
        multisample.rasterizationSamples(sampleCountToVk(sampleCount));
        multisample.sampleShadingEnable(sampleShadingEnabled);
        multisample.minSampleShading(minSampleShading);
        // multisample.pSampleMask() - set separately if needed
        multisample.alphaToCoverageEnable(alphaToCoverageEnabled);
        multisample.alphaToOneEnable(alphaToOneEnabled);
    }
    
    /**
     * Convert sample count to Vulkan sample count flag.
     */
    private static int sampleCountToVk(int samples) {
        return switch (samples) {
            case 1 -> VK10.VK_SAMPLE_COUNT_1_BIT;
            case 2 -> VK10.VK_SAMPLE_COUNT_2_BIT;
            case 4 -> VK10.VK_SAMPLE_COUNT_4_BIT;
            case 8 -> VK10.VK_SAMPLE_COUNT_8_BIT;
            case 16 -> VK10.VK_SAMPLE_COUNT_16_BIT;
            case 32 -> VK10.VK_SAMPLE_COUNT_32_BIT;
            case 64 -> VK10.VK_SAMPLE_COUNT_64_BIT;
            default -> VK10.VK_SAMPLE_COUNT_1_BIT;
        };
    }
    
    /**
     * Populate VkPipelineDepthStencilStateCreateInfo.
     */
    public void populateDepthStencilState(VkPipelineDepthStencilStateCreateInfo depthStencil) {
        depthStencil.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
        depthStencil.depthTestEnable(depthTestEnabled);
        depthStencil.depthWriteEnable(depthWriteEnabled);
        depthStencil.depthCompareOp(glCompareToVk(depthFunc));
        depthStencil.depthBoundsTestEnable(depthBoundsTestEnabled);
        depthStencil.stencilTestEnable(stencilTestEnabled);
        
        stencilFront.populate(depthStencil.front());
        stencilBack.populate(depthStencil.back());
        
        depthStencil.minDepthBounds(depthBoundsMin);
        depthStencil.maxDepthBounds(depthBoundsMax);
    }
    
    /**
     * Populate VkPipelineColorBlendStateCreateInfo.
     */
    public void populateColorBlendState(
            VkPipelineColorBlendStateCreateInfo colorBlend,
            VkPipelineColorBlendAttachmentState.Buffer attachments,
            int attachmentCount) {
        
        for (int i = 0; i < attachmentCount; i++) {
            attachmentBlendStates[i].populate(attachments.get(i));
        }
        attachments.limit(attachmentCount);
        
        colorBlend.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
        colorBlend.logicOpEnable(logicOpEnabled);
        colorBlend.logicOp(getVkLogicOp());
        colorBlend.pAttachments(attachments);
        colorBlend.blendConstants(0, blendConstants[0]);
        colorBlend.blendConstants(1, blendConstants[1]);
        colorBlend.blendConstants(2, blendConstants[2]);
        colorBlend.blendConstants(3, blendConstants[3]);
    }
    
    /**
     * Populate VkPipelineDynamicStateCreateInfo for commonly dynamic state.
     */
    public void populateDynamicState(
            VkPipelineDynamicStateCreateInfo dynamicState,
            IntBuffer dynamicStates) {
        
        // Standard dynamic states for flexibility
        dynamicStates.put(VK10.VK_DYNAMIC_STATE_VIEWPORT);
        dynamicStates.put(VK10.VK_DYNAMIC_STATE_SCISSOR);
        dynamicStates.put(VK10.VK_DYNAMIC_STATE_LINE_WIDTH);
        dynamicStates.put(VK10.VK_DYNAMIC_STATE_DEPTH_BIAS);
        dynamicStates.put(VK10.VK_DYNAMIC_STATE_BLEND_CONSTANTS);
        dynamicStates.put(VK10.VK_DYNAMIC_STATE_DEPTH_BOUNDS);
        dynamicStates.put(VK10.VK_DYNAMIC_STATE_STENCIL_COMPARE_MASK);
        dynamicStates.put(VK10.VK_DYNAMIC_STATE_STENCIL_WRITE_MASK);
        dynamicStates.put(VK10.VK_DYNAMIC_STATE_STENCIL_REFERENCE);
        dynamicStates.flip();
        
        dynamicState.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
        dynamicState.pDynamicStates(dynamicStates);
    }
    
    // ========================================================================
    // DYNAMIC STATE COMMAND HELPERS
    // ========================================================================
    
    /**
     * Record dynamic state commands to command buffer.
     * Call this before each draw to ensure Vulkan state matches GL state.
     */
    public void recordDynamicState(VkCommandBuffer commandBuffer) {
        long dirty = clearDirty(DirtyFlags.ALL_DYNAMIC_STATE);
        
        if ((dirty & DirtyFlags.VIEWPORT) != 0) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkViewport.Buffer vp = VkViewport.calloc(activeViewportCount, stack);
                for (int i = 0; i < activeViewportCount; i++) {
                    ViewportState vs = viewports[i];
                    vp.get(i).set(vs.x, vs.y, vs.width, vs.height, vs.minDepth, vs.maxDepth);
                }
                VK10.vkCmdSetViewport(commandBuffer, 0, vp);
            }
        }
        
        if ((dirty & DirtyFlags.SCISSOR) != 0) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkRect2D.Buffer sc = VkRect2D.calloc(activeViewportCount, stack);
                for (int i = 0; i < activeViewportCount; i++) {
                    ScissorState ss = scissors[i];
                    if (scissorTestEnabled) {
                        sc.get(i).offset().set(ss.x, ss.y);
                        sc.get(i).extent().set(ss.width, ss.height);
                    } else {
                        // Full viewport if scissor disabled
                        sc.get(i).offset().set(0, 0);
                        sc.get(i).extent().set(getFramebufferWidth(), getFramebufferHeight());
                    }
                }
                VK10.vkCmdSetScissor(commandBuffer, 0, sc);
            }
        }
        
        if ((dirty & DirtyFlags.LINE_WIDTH) != 0) {
            VK10.vkCmdSetLineWidth(commandBuffer, lineWidth);
        }
        
        if ((dirty & DirtyFlags.DEPTH_BIAS) != 0) {
            VK10.vkCmdSetDepthBias(commandBuffer, depthBiasConstant, depthBiasClamp, depthBiasSlope);
        }
        
        if ((dirty & DirtyFlags.BLEND_CONSTANTS) != 0) {
            VK10.vkCmdSetBlendConstants(commandBuffer, blendConstants);
        }
        
        if ((dirty & DirtyFlags.DEPTH_BOUNDS) != 0 && depthBoundsTestEnabled) {
            VK10.vkCmdSetDepthBounds(commandBuffer, depthBoundsMin, depthBoundsMax);
        }
        
        if ((dirty & DirtyFlags.STENCIL_REFERENCE) != 0 && stencilTestEnabled) {
            VK10.vkCmdSetStencilCompareMask(commandBuffer, VK10.VK_STENCIL_FACE_FRONT_BIT, stencilFront.compareMask);
            VK10.vkCmdSetStencilCompareMask(commandBuffer, VK10.VK_STENCIL_FACE_BACK_BIT, stencilBack.compareMask);
            VK10.vkCmdSetStencilWriteMask(commandBuffer, VK10.VK_STENCIL_FACE_FRONT_BIT, stencilFront.writeMask);
            VK10.vkCmdSetStencilWriteMask(commandBuffer, VK10.VK_STENCIL_FACE_BACK_BIT, stencilBack.writeMask);
            VK10.vkCmdSetStencilReference(commandBuffer, VK10.VK_STENCIL_FACE_FRONT_BIT, stencilFront.reference);
            VK10.vkCmdSetStencilReference(commandBuffer, VK10.VK_STENCIL_FACE_BACK_BIT, stencilBack.reference);
        }
    }
    
    /**
     * Bind vertex buffers to command buffer based on current state.
     */
    public void bindVertexBuffers(VkCommandBuffer commandBuffer) {
        if (!isDirty(DirtyFlags.VERTEX_BINDINGS)) return;
        clearDirty(DirtyFlags.VERTEX_BINDINGS);
        
        int bindingCount = getActiveVertexBindingCount();
        if (bindingCount == 0) return;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer bufferHandles = stack.mallocLong(bindingCount);
            LongBuffer offsets = stack.mallocLong(bindingCount);
            
            for (int i = 0; i < bindingCount; i++) {
                VertexBufferBinding vbb = vertexBufferBindings[i];
                if (vbb.buffer != 0) {
                    BufferObject buf = buffers.get(vbb.buffer);
                    bufferHandles.put(i, buf != null ? buf.vkBuffer : 0);
                    offsets.put(i, vbb.offset);
                } else {
                    bufferHandles.put(i, 0);
                    offsets.put(i, 0);
                }
            }
            
            VK10.vkCmdBindVertexBuffers(commandBuffer, 0, bufferHandles, offsets);
        }
    }
    
    /**
     * Bind index buffer to command buffer.
     */
    public void bindIndexBuffer(VkCommandBuffer commandBuffer) {
        long ebo = getBoundBuffer(GL.Buffer.ELEMENT_ARRAY_BUFFER);
        if (ebo != 0) {
            BufferObject buf = buffers.get(ebo);
            if (buf != null && buf.vkBuffer != 0) {
                // Determine index type based on usage or default to UINT16
                int indexType = VK10.VK_INDEX_TYPE_UINT16; // Could be determined from draw call
                VK10.vkCmdBindIndexBuffer(commandBuffer, buf.vkBuffer, 0, indexType);
            }
        }
    }
    
    /**
     * Bind descriptor sets to command buffer.
     */
    public void bindDescriptorSets(VkCommandBuffer commandBuffer, int pipelineBindPoint) {
        if (!isDirty(DirtyFlags.DESCRIPTORS) || descriptorSetCount == 0) return;
        clearDirty(DirtyFlags.DESCRIPTORS);
        
        long pipelineLayout = getPipelineLayout();
        if (pipelineLayout == 0) return;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer sets = stack.mallocLong(descriptorSetCount);
            for (int i = 0; i < descriptorSetCount; i++) {
                sets.put(i, boundDescriptorSets[i]);
            }
            
            // Gather all dynamic offsets
            int totalDynamicOffsets = 0;
            for (int i = 0; i < descriptorSetCount; i++) {
                totalDynamicOffsets += dynamicOffsetCounts[i];
            }
            
            IntBuffer dynamicOffsets = null;
            if (totalDynamicOffsets > 0) {
                dynamicOffsets = stack.mallocInt(totalDynamicOffsets);
                for (int i = 0; i < descriptorSetCount; i++) {
                    for (int j = 0; j < dynamicOffsetCounts[i]; j++) {
                        dynamicOffsets.put(descriptorSetDynamicOffsets[i][j]);
                    }
                }
                dynamicOffsets.flip();
            }
            
            VK10.vkCmdBindDescriptorSets(commandBuffer, pipelineBindPoint, pipelineLayout,
                                          0, sets, dynamicOffsets);
        }
    }
    
    /**
     * Push constants to command buffer.
     */
    public void pushConstants(VkCommandBuffer commandBuffer, int stageFlags) {
        if (!isDirty(DirtyFlags.PUSH_CONSTANTS) || pushConstantSize == 0) return;
        clearDirty(DirtyFlags.PUSH_CONSTANTS);
        
        long pipelineLayout = getPipelineLayout();
        if (pipelineLayout == 0) return;
        
        ByteBuffer data = getPushConstantBuffer();
        VK10.vkCmdPushConstants(commandBuffer, pipelineLayout, stageFlags, 0, data);
    }
    
    // ========================================================================
    // RESOURCE CLEANUP
    // ========================================================================
    
    /**
     * Resource pending deletion.
     */
    private record PendingDeletion(int type, long handle, long frame) {}
    
    private static final int DELETE_TYPE_BUFFER = 0;
    private static final int DELETE_TYPE_IMAGE = 1;
    private static final int DELETE_TYPE_IMAGE_VIEW = 2;
    private static final int DELETE_TYPE_SAMPLER = 3;
    private static final int DELETE_TYPE_SHADER_MODULE = 4;
    private static final int DELETE_TYPE_PIPELINE = 5;
    private static final int DELETE_TYPE_PIPELINE_LAYOUT = 6;
    private static final int DELETE_TYPE_DESCRIPTOR_SET_LAYOUT = 7;
    private static final int DELETE_TYPE_FRAMEBUFFER = 8;
    private static final int DELETE_TYPE_RENDER_PASS = 9;
    private static final int DELETE_TYPE_MEMORY = 10;
    
    private final List<PendingDeletion> pendingDeletions = new ArrayList<>();
    private long currentFrame = 0;
    private static final int DELETION_DELAY_FRAMES = 3;
    
    /**
     * Queue a Vulkan resource for delayed deletion.
     */
    public void queueDeletion(int type, long handle) {
        if (handle != 0) {
            pendingDeletions.add(new PendingDeletion(type, handle, currentFrame));
        }
    }
    
    /**
     * Process pending deletions that are safe to execute.
     */
    public void processDeletions(VkDevice device) {
        currentFrame++;
        
        Iterator<PendingDeletion> it = pendingDeletions.iterator();
        while (it.hasNext()) {
            PendingDeletion pd = it.next();
            if (currentFrame - pd.frame() >= DELETION_DELAY_FRAMES) {
                deleteVulkanResource(device, pd.type(), pd.handle());
                it.remove();
            }
        }
    }
    
    /**
     * Delete Vulkan resource.
     */
    private void deleteVulkanResource(VkDevice device, int type, long handle) {
        switch (type) {
            case DELETE_TYPE_BUFFER -> VK10.vkDestroyBuffer(device, handle, null);
            case DELETE_TYPE_IMAGE -> VK10.vkDestroyImage(device, handle, null);
            case DELETE_TYPE_IMAGE_VIEW -> VK10.vkDestroyImageView(device, handle, null);
            case DELETE_TYPE_SAMPLER -> VK10.vkDestroySampler(device, handle, null);
            case DELETE_TYPE_SHADER_MODULE -> VK10.vkDestroyShaderModule(device, handle, null);
            case DELETE_TYPE_PIPELINE -> VK10.vkDestroyPipeline(device, handle, null);
            case DELETE_TYPE_PIPELINE_LAYOUT -> VK10.vkDestroyPipelineLayout(device, handle, null);
            case DELETE_TYPE_DESCRIPTOR_SET_LAYOUT -> VK10.vkDestroyDescriptorSetLayout(device, handle, null);
            case DELETE_TYPE_FRAMEBUFFER -> VK10.vkDestroyFramebuffer(device, handle, null);
            case DELETE_TYPE_RENDER_PASS -> VK10.vkDestroyRenderPass(device, handle, null);
            case DELETE_TYPE_MEMORY -> VK10.vkFreeMemory(device, handle, null);
        }
    }
    
    /**
     * Force deletion of all pending resources.
     */
    public void flushDeletions(VkDevice device) {
        for (PendingDeletion pd : pendingDeletions) {
            deleteVulkanResource(device, pd.type(), pd.handle());
        }
        pendingDeletions.clear();
    }
    
    /**
     * Dispose all resources and cleanup.
     */
    public void dispose(VkDevice device) {
        // Free push constant buffer
        if (pushConstantBuffer != null) {
            MemoryUtil.memFree(pushConstantBuffer);
            pushConstantBuffer = null;
        }
        
        // Queue all Vulkan resources for deletion
        textures.forEachValue(tex -> {
            queueDeletion(DELETE_TYPE_IMAGE_VIEW, tex.vkImageView);
            queueDeletion(DELETE_TYPE_IMAGE, tex.vkImage);
            queueDeletion(DELETE_TYPE_MEMORY, tex.vkMemory);
            queueDeletion(DELETE_TYPE_SAMPLER, tex.vkSampler);
        });
        
        samplers.forEachValue(sampler -> {
            queueDeletion(DELETE_TYPE_SAMPLER, sampler.vkSampler);
        });
        
        buffers.forEachValue(buf -> {
            queueDeletion(DELETE_TYPE_BUFFER, buf.vkBuffer);
            queueDeletion(DELETE_TYPE_MEMORY, buf.vkMemory);
        });
        
        shaders.forEachValue(shader -> {
            queueDeletion(DELETE_TYPE_SHADER_MODULE, shader.vkShaderModule);
        });
        
        programs.forEachValue(prog -> {
            queueDeletion(DELETE_TYPE_PIPELINE, prog.vkGraphicsPipeline);
            queueDeletion(DELETE_TYPE_PIPELINE, prog.vkComputePipeline);
            queueDeletion(DELETE_TYPE_PIPELINE_LAYOUT, prog.vkPipelineLayout);
            if (prog.vkDescriptorSetLayouts != null) {
                for (long layout : prog.vkDescriptorSetLayouts) {
                    queueDeletion(DELETE_TYPE_DESCRIPTOR_SET_LAYOUT, layout);
                }
            }
        });
        
        framebuffers.forEachValue(fbo -> {
            queueDeletion(DELETE_TYPE_FRAMEBUFFER, fbo.vkFramebuffer);
            queueDeletion(DELETE_TYPE_RENDER_PASS, fbo.vkRenderPass);
        });
        
        renderbuffers.forEachValue(rbo -> {
            queueDeletion(DELETE_TYPE_IMAGE_VIEW, rbo.vkImageView);
            queueDeletion(DELETE_TYPE_IMAGE, rbo.vkImage);
            queueDeletion(DELETE_TYPE_MEMORY, rbo.vkMemory);
        });
        
        // Flush all deletions immediately
        flushDeletions(device);
        
        // Clear all maps
        textures.clear();
        samplers.clear();
        buffers.clear();
        shaders.clear();
        programs.clear();
        framebuffers.clear();
        renderbuffers.clear();
        vertexArrays.clear();
        transformFeedbackObjects.clear();
        queries.clear();
    }
    
    // ========================================================================
    // DEBUG AND DIAGNOSTICS
    // ========================================================================
    
    /**
     * Validation error levels.
     */
    public enum ValidationLevel {
        NONE,
        ERRORS_ONLY,
        WARNINGS,
        ALL
    }
    
    private ValidationLevel validationLevel = ValidationLevel.NONE;
    private Consumer<String> validationCallback = System.err::println;
    
    /**
     * Set validation level.
     */
    public void setValidationLevel(ValidationLevel level) {
        this.validationLevel = level;
    }
    
    /**
     * Set validation message callback.
     */
    public void setValidationCallback(Consumer<String> callback) {
        this.validationCallback = callback;
    }
    
    /**
     * Validate current state for draw call.
     */
    public boolean validateForDraw() {
        if (validationLevel == ValidationLevel.NONE) return true;
        
        boolean valid = true;
        
        // Check program
        if (currentProgram == 0) {
            reportValidation("ERROR", "No program bound for draw");
            valid = false;
        } else {
            ProgramObject prog = programs.get(currentProgram);
            if (prog == null || !prog.linkStatus) {
                reportValidation("ERROR", "Bound program is not linked");
                valid = false;
            }
        }
        
        // Check VAO (if core profile)
        if (currentVAO == 0 && getEnabledAttribCount() == 0) {
            if (validationLevel.ordinal() >= ValidationLevel.WARNINGS.ordinal()) {
                reportValidation("WARN", "No VAO bound and no vertex attributes enabled");
            }
        }
        
        // Check vertex buffers for enabled attributes
        int mask = enabledAttribMask;
        while (mask != 0) {
            int index = Integer.numberOfTrailingZeros(mask);
            mask &= (mask - 1);
            
            VertexAttrib attrib = vertexAttribs[index];
            VertexBufferBinding vbb = vertexBufferBindings[attrib.binding];
            
            if (vbb.buffer == 0) {
                reportValidation("ERROR", "Vertex attribute " + index + 
                                " enabled but no buffer bound to binding " + attrib.binding);
                valid = false;
            }
        }
        
        // Check framebuffer
        if (currentDrawFramebuffer != 0) {
            FramebufferObject fbo = framebuffers.get(currentDrawFramebuffer);
            if (fbo == null) {
                reportValidation("ERROR", "Invalid framebuffer bound");
                valid = false;
            } else if (fbo.status != 0x8CD5) { // GL_FRAMEBUFFER_COMPLETE
                fbo.checkStatus();
                if (fbo.status != 0x8CD5) {
                    reportValidation("ERROR", "Framebuffer is not complete: 0x" + 
                                    Integer.toHexString(fbo.status));
                    valid = false;
                }
            }
        }
        
        return valid;
    }
    
    /**
     * Validate for indexed draw.
     */
    public boolean validateForIndexedDraw() {
        if (!validateForDraw()) return false;
        
        if (validationLevel == ValidationLevel.NONE) return true;
        
        long ebo = getBoundBuffer(GL.Buffer.ELEMENT_ARRAY_BUFFER);
        if (ebo == 0) {
            reportValidation("ERROR", "No element buffer bound for indexed draw");
            return false;
        }
        
        BufferObject buf = buffers.get(ebo);
        if (buf == null) {
            reportValidation("ERROR", "Invalid element buffer bound");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate for compute dispatch.
     */
    public boolean validateForCompute() {
        if (validationLevel == ValidationLevel.NONE) return true;
        
        if (currentProgram == 0) {
            reportValidation("ERROR", "No program bound for compute dispatch");
            return false;
        }
        
        ProgramObject prog = programs.get(currentProgram);
        if (prog == null || !prog.linkStatus) {
            reportValidation("ERROR", "Bound program is not linked");
            return false;
        }
        
        if (prog.vkComputePipeline == 0) {
            reportValidation("ERROR", "Program does not have compute pipeline");
            return false;
        }
        
        return true;
    }
    
    private void reportValidation(String level, String message) {
        if (validationCallback != null) {
            validationCallback.accept("[VulkanState " + level + "] " + message);
        }
    }
    
    /**
     * Generate comprehensive state report.
     */
    public String generateStateReport() {
        StringBuilder sb = new StringBuilder(8192);
        
        sb.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                           VULKAN STATE REPORT                                 ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        
        // Version and dirty flags
        sb.append("║ State Version: ").append(getStateVersion()).append("\n");
        sb.append("║ Dirty Flags: 0x").append(Long.toHexString(getDirtyFlags())).append("\n");
        sb.append("║ Pipeline Hash Valid: ").append(pipelineHashValid).append("\n");
        if (pipelineHashValid) {
            sb.append("║ Pipeline Hash: 0x").append(Long.toHexString(cachedPipelineHash)).append("\n");
        }
        
        // Program state
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ PROGRAM STATE\n");
        sb.append("║   Current Program: ").append(currentProgram).append("\n");
        if (currentProgram != 0) {
            ProgramObject prog = programs.get(currentProgram);
            if (prog != null) {
                sb.append("║   Link Status: ").append(prog.linkStatus).append("\n");
                sb.append("║   Attached Shaders: ").append(prog.attachedShaders.size()).append("\n");
                sb.append("║   Active Uniforms: ").append(prog.activeUniforms).append("\n");
                sb.append("║   Active Attributes: ").append(prog.activeAttributes).append("\n");
            }
        }
        
        // Vertex input state
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ VERTEX INPUT STATE\n");
        sb.append("║   Current VAO: ").append(currentVAO).append("\n");
        sb.append("║   Enabled Attribs Mask: 0x").append(Integer.toHexString(enabledAttribMask)).append("\n");
        sb.append("║   Enabled Attrib Count: ").append(getEnabledAttribCount()).append("\n");
        
        int mask = enabledAttribMask;
        while (mask != 0) {
            int idx = Integer.numberOfTrailingZeros(mask);
            mask &= (mask - 1);
            VertexAttrib attr = vertexAttribs[idx];
            sb.append(String.format("║     [%2d] size=%d type=0x%04X binding=%d offset=%d%n",
                idx, attr.size, attr.type, attr.binding, attr.relativeOffset));
        }
        
        // Buffer bindings
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ BUFFER BINDINGS\n");
        sb.append("║   Array Buffer: ").append(getBoundBuffer(GL.Buffer.ARRAY_BUFFER)).append("\n");
        sb.append("║   Element Buffer: ").append(getBoundBuffer(GL.Buffer.ELEMENT_ARRAY_BUFFER)).append("\n");
        sb.append("║   Uniform Buffers Bound: ");
        int uboCount = 0;
        for (IndexedBufferBinding b : uniformBufferBindings) if (b.isBound()) uboCount++;
        sb.append(uboCount).append("\n");
        sb.append("║   SSBOs Bound: ");
        int ssboCount = 0;
        for (IndexedBufferBinding b : shaderStorageBindings) if (b.isBound()) ssboCount++;
        sb.append(ssboCount).append("\n");
        
        // Texture bindings
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ TEXTURE BINDINGS\n");
        sb.append("║   Active Unit: ").append(activeTextureUnit).append("\n");
        for (int i = 0; i < MAX_TEXTURE_UNITS; i++) {
            if (boundTextures[i] != 0) {
                sb.append(String.format("║     Unit %2d: texture=%d sampler=%d%n",
                    i, boundTextures[i], boundSamplers[i]));
            }
        }
        
        // Blend state
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ BLEND STATE\n");
        sb.append("║   Enabled: ").append(isBlendEnabled()).append("\n");
        if (isBlendEnabled()) {
            AttachmentBlendState abs = attachmentBlendStates[0];
            sb.append(String.format("║   SrcRGB=0x%04X DstRGB=0x%04X OpRGB=0x%04X%n",
                abs.srcRGB, abs.dstRGB, abs.opRGB));
            sb.append(String.format("║   SrcA=0x%04X   DstA=0x%04X   OpA=0x%04X%n",
                abs.srcAlpha, abs.dstAlpha, abs.opAlpha));
        }
        sb.append("║   Blend Constants: [").append(blendConstants[0]).append(", ")
          .append(blendConstants[1]).append(", ").append(blendConstants[2]).append(", ")
          .append(blendConstants[3]).append("]\n");
        
        // Depth state
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ DEPTH STATE\n");
        sb.append("║   Test Enabled: ").append(depthTestEnabled).append("\n");
        sb.append("║   Write Enabled: ").append(depthWriteEnabled).append("\n");
        sb.append(String.format("║   Func: 0x%04X (%s)%n", depthFunc, 
            switch(depthFunc) {
                case GL.Compare.NEVER -> "NEVER";
                case GL.Compare.LESS -> "LESS";
                case GL.Compare.EQUAL -> "EQUAL";
                case GL.Compare.LEQUAL -> "LEQUAL";
                case GL.Compare.GREATER -> "GREATER";
                case GL.Compare.NOTEQUAL -> "NOTEQUAL";
                case GL.Compare.GEQUAL -> "GEQUAL";
                case GL.Compare.ALWAYS -> "ALWAYS";
                default -> "?";
            }));
        sb.append("║   Range: [").append(depthRangeNear).append(", ").append(depthRangeFar).append("]\n");
        sb.append("║   Clamp: ").append(depthClampEnabled).append("\n");
        sb.append("║   Bias: ").append(depthBiasEnabled);
        if (depthBiasEnabled) {
            sb.append(" (const=").append(depthBiasConstant)
              .append(" slope=").append(depthBiasSlope)
              .append(" clamp=").append(depthBiasClamp).append(")");
        }
        sb.append("\n");
        
        // Stencil state
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ STENCIL STATE\n");
        sb.append("║   Test Enabled: ").append(stencilTestEnabled).append("\n");
        if (stencilTestEnabled) {
            sb.append(String.format("║   Front: fail=0x%04X dpfail=0x%04X pass=0x%04X cmp=0x%04X ref=%d mask=0x%02X%n",
                stencilFront.failOp, stencilFront.depthFailOp, stencilFront.passOp,
                stencilFront.compareOp, stencilFront.reference, stencilFront.compareMask));
            sb.append(String.format("║   Back:  fail=0x%04X dpfail=0x%04X pass=0x%04X cmp=0x%04X ref=%d mask=0x%02X%n",
                stencilBack.failOp, stencilBack.depthFailOp, stencilBack.passOp,
                stencilBack.compareOp, stencilBack.reference, stencilBack.compareMask));
        }
        
        // Rasterization state
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ RASTERIZATION STATE\n");
        sb.append("║   Cull Face: ").append(cullFaceEnabled);
        if (cullFaceEnabled) {
            sb.append(" (mode=").append(cullFaceMode == GL.Face.BACK ? "BACK" : 
                                        cullFaceMode == GL.Face.FRONT ? "FRONT" : "BOTH").append(")");
        }
        sb.append("\n");
        sb.append("║   Front Face: ").append(frontFace == GL.Face.CCW ? "CCW" : "CW").append("\n");
        sb.append("║   Polygon Mode: ").append(
            polygonModeFront == GL.Polygon.FILL ? "FILL" :
            polygonModeFront == GL.Polygon.LINE ? "LINE" : "POINT").append("\n");
        sb.append("║   Line Width: ").append(lineWidth).append("\n");
        sb.append("║   Rasterizer Discard: ").append(rasterizerDiscardEnabled).append("\n");
        
        // Multisample state
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ MULTISAMPLE STATE\n");
        sb.append("║   Sample Count: ").append(sampleCount).append("\n");
        sb.append("║   Sample Shading: ").append(sampleShadingEnabled);
        if (sampleShadingEnabled) sb.append(" (min=").append(minSampleShading).append(")");
        sb.append("\n");
        sb.append("║   Alpha To Coverage: ").append(alphaToCoverageEnabled).append("\n");
        sb.append("║   Alpha To One: ").append(alphaToOneEnabled).append("\n");
        
        // Viewport/Scissor
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ VIEWPORT/SCISSOR STATE\n");
        ViewportState vp = viewports[0];
        sb.append(String.format("║   Viewport[0]: x=%.1f y=%.1f w=%.1f h=%.1f depth=[%.2f,%.2f]%n",
            vp.x, vp.y, vp.width, vp.height, vp.minDepth, vp.maxDepth));
        ScissorState sc = scissors[0];
        sb.append(String.format("║   Scissor[0]: x=%d y=%d w=%d h=%d (enabled=%b)%n",
            sc.x, sc.y, sc.width, sc.height, scissorTestEnabled));
        
        // Framebuffer
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ FRAMEBUFFER STATE\n");
        sb.append("║   Draw FBO: ").append(currentDrawFramebuffer).append("\n");
        sb.append("║   Read FBO: ").append(currentReadFramebuffer).append("\n");
        sb.append("║   Current Size: ").append(getFramebufferWidth()).append("x")
          .append(getFramebufferHeight()).append("\n");
        
        // Clear values
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ CLEAR VALUES\n");
        sb.append("║   Color: [").append(clearColor[0]).append(", ").append(clearColor[1])
          .append(", ").append(clearColor[2]).append(", ").append(clearColor[3]).append("]\n");
        sb.append("║   Depth: ").append(clearDepth).append("\n");
        sb.append("║   Stencil: ").append(clearStencil).append("\n");
        
        // Object counts
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ OBJECT COUNTS\n");
        sb.append("║   Textures: ").append(textures.size()).append("\n");
        sb.append("║   Samplers: ").append(samplers.size()).append("\n");
        sb.append("║   Buffers: ").append(buffers.size()).append("\n");
        sb.append("║   Shaders: ").append(shaders.size()).append("\n");
        sb.append("║   Programs: ").append(programs.size()).append("\n");
        sb.append("║   VAOs: ").append(vertexArrays.size()).append("\n");
        sb.append("║   Framebuffers: ").append(framebuffers.size()).append("\n");
        sb.append("║   Renderbuffers: ").append(renderbuffers.size()).append("\n");
        sb.append("║   Queries: ").append(queries.size()).append("\n");
        sb.append("║   Pending Deletions: ").append(pendingDeletions.size()).append("\n");
        
        // Enabled capabilities
        sb.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║ ENABLED CAPABILITIES: 0x").append(Long.toHexString(enabledCapabilities)).append("\n");
        
        sb.append("╚══════════════════════════════════════════════════════════════════════════════╝\n");
        
        return sb.toString();
    }
    
    /**
     * Get memory usage statistics.
     */
    public MemoryStats getMemoryStats() {
        long textureMemory = 0;
        long bufferMemory = 0;
        int textureCount = 0;
        int bufferCount = 0;
        
        // This is approximate - actual Vulkan memory tracked separately
        textures.forEachValue(tex -> {});
        buffers.forEachValue(buf -> {});
        
        return new MemoryStats(
            textures.size(),
            buffers.size(),
            programs.size(),
            shaders.size(),
            vertexArrays.size(),
            framebuffers.size(),
            pushConstantSize,
            pendingDeletions.size()
        );
    }
    
    /**
     * Memory statistics record.
     */
    public record MemoryStats(
        int textureCount,
        int bufferCount,
        int programCount,
        int shaderCount,
        int vaoCount,
        int fboCount,
        int pushConstantBytes,
        int pendingDeletions
    ) {}
    
    // ========================================================================
    // THREAD SAFETY HELPERS
    // ========================================================================
    
    /**
     * Execute action with read lock (if thread safety enabled).
     */
    public <T> T withReadLock(java.util.function.Supplier<T> action) {
        if (!threadSafe || stateLock == null) {
            return action.get();
        }
        
        long stamp = stateLock.readLock();
        try {
            return action.get();
        } finally {
            stateLock.unlockRead(stamp);
        }
    }
    
    /**
     * Execute action with write lock (if thread safety enabled).
     */
    public void withWriteLock(Runnable action) {
        if (!threadSafe || stateLock == null) {
            action.run();
            return;
        }
        
        long stamp = stateLock.writeLock();
        try {
            action.run();
        } finally {
            stateLock.unlockWrite(stamp);
        }
    }
    
    /**
     * Try to execute with optimistic read, falling back to read lock.
     */
    public <T> T withOptimisticRead(java.util.function.Supplier<T> action) {
        if (!threadSafe || stateLock == null) {
            return action.get();
        }
        
        long stamp = stateLock.tryOptimisticRead();
        T result = action.get();
        
        if (!stateLock.validate(stamp)) {
            stamp = stateLock.readLock();
            try {
                result = action.get();
            } finally {
                stateLock.unlockRead(stamp);
            }
        }
        
        return result;
    }
}
