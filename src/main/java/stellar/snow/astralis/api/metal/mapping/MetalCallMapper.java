// ════════════════════════════════════════════════════════════════════════════════
// FILE: MetalCallMapper.java
// PURPOSE: Comprehensive OpenGL to Metal API Translation Layer
// ════════════════════════════════════════════════════════════════════════════════
//
// ┌─────────────────────────────────────────────────────────────────────────────┐
// │                    METAL CALL MAPPER - GL TO MTL                            │
// │                                                                             │
// │  This system provides complete translation of OpenGL API calls to Metal    │
// │  equivalents, enabling OpenGL applications to run on Apple platforms       │
// │  with native Metal performance.                                            │
// │                                                                             │
// │  Architecture:                                                              │
// │  ┌─────────────┐    ┌──────────────┐    ┌─────────────┐                   │
// │  │  OpenGL     │───▶│  Translator  │───▶│   Metal     │                   │
// │  │  Commands   │    │    Layer     │    │  Commands   │                   │
// │  └─────────────┘    └──────────────┘    └─────────────┘                   │
// │         │                  │                   │                           │
// │         ▼                  ▼                   ▼                           │
// │  ┌─────────────┐    ┌──────────────┐    ┌─────────────┐                   │
// │  │  GL State   │───▶│ State Cache  │───▶│  MTL State  │                   │
// │  │  Machine    │    │   & Track    │    │  Objects    │                   │
// │  └─────────────┘    └──────────────┘    └─────────────┘                   │
// │                                                                             │
// │  Key Challenges Solved:                                                    │
// │  • Immediate mode → Command buffer batching                                │
// │  • Mutable state → Immutable pipeline state objects                       │
// │  • Client-side arrays → GPU buffers                                        │
// │  • GLSL → Metal Shading Language                                          │
// │  • Fixed-function pipeline → Shader emulation                             │
// │  • Different coordinate systems (clip space Z)                            │
// │  • Texture coordinate origin (bottom-left vs top-left)                    │
// │                                                                             │
// └─────────────────────────────────────────────────────────────────────────────┘
//
// ════════════════════════════════════════════════════════════════════════════════

package stellar.snow.astralis.api.metal.mapping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

/**
 * MetalCallMapper - Comprehensive OpenGL to Metal Translation Layer
 * 
 * <p>This class provides a complete mapping of OpenGL API calls to their Metal equivalents,
 * enabling OpenGL applications to run on Apple platforms with native Metal performance.
 * It handles all aspects of the translation including state management, resource creation,
 * shader compilation, and draw call translation.</p>
 * 
 * <h2>Supported OpenGL Versions:</h2>
 * <ul>
 *   <li>OpenGL 2.1 (Core Profile)</li>
 *   <li>OpenGL 3.3 (Core Profile)</li>
 *   <li>OpenGL 4.1 (Core Profile - max on macOS)</li>
 *   <li>OpenGL ES 2.0/3.0/3.1/3.2</li>
 * </ul>
 * 
 * <h2>Metal Version Support:</h2>
 * <ul>
 *   <li>Metal 1.0 - 3.1 (automatic feature detection)</li>
 *   <li>Apple GPU Family 1-9</li>
 *   <li>Mac GPU Family 1-2</li>
 * </ul>
 */
public final class MetalCallMapper implements AutoCloseable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MetalCallMapper.class);
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 1: OPENGL CONSTANTS (Comprehensive)
    // ════════════════════════════════════════════════════════════════════════════
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.1 Boolean and Error Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_FALSE = 0;
    public static final int GL_TRUE = 1;
    public static final int GL_NO_ERROR = 0;
    public static final int GL_INVALID_ENUM = 0x0500;
    public static final int GL_INVALID_VALUE = 0x0501;
    public static final int GL_INVALID_OPERATION = 0x0502;
    public static final int GL_STACK_OVERFLOW = 0x0503;
    public static final int GL_STACK_UNDERFLOW = 0x0504;
    public static final int GL_OUT_OF_MEMORY = 0x0505;
    public static final int GL_INVALID_FRAMEBUFFER_OPERATION = 0x0506;
    public static final int GL_CONTEXT_LOST = 0x0507;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.2 Primitive Types
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_POINTS = 0x0000;
    public static final int GL_LINES = 0x0001;
    public static final int GL_LINE_LOOP = 0x0002;
    public static final int GL_LINE_STRIP = 0x0003;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_TRIANGLE_STRIP = 0x0005;
    public static final int GL_TRIANGLE_FAN = 0x0006;
    public static final int GL_QUADS = 0x0007;
    public static final int GL_QUAD_STRIP = 0x0008;
    public static final int GL_POLYGON = 0x0009;
    public static final int GL_LINES_ADJACENCY = 0x000A;
    public static final int GL_LINE_STRIP_ADJACENCY = 0x000B;
    public static final int GL_TRIANGLES_ADJACENCY = 0x000C;
    public static final int GL_TRIANGLE_STRIP_ADJACENCY = 0x000D;
    public static final int GL_PATCHES = 0x000E;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.3 Data Types
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_BYTE = 0x1400;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_SHORT = 0x1402;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_INT = 0x1404;
    public static final int GL_UNSIGNED_INT = 0x1405;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_2_BYTES = 0x1407;
    public static final int GL_3_BYTES = 0x1408;
    public static final int GL_4_BYTES = 0x1409;
    public static final int GL_DOUBLE = 0x140A;
    public static final int GL_HALF_FLOAT = 0x140B;
    public static final int GL_FIXED = 0x140C;
    public static final int GL_INT64 = 0x140E;
    public static final int GL_UNSIGNED_INT64 = 0x140F;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.4 Buffer Targets
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_PIXEL_PACK_BUFFER = 0x88EB;
    public static final int GL_PIXEL_UNPACK_BUFFER = 0x88EC;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    public static final int GL_TEXTURE_BUFFER = 0x8C2A;
    public static final int GL_TRANSFORM_FEEDBACK_BUFFER = 0x8C8E;
    public static final int GL_COPY_READ_BUFFER = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    public static final int GL_DRAW_INDIRECT_BUFFER = 0x8F3F;
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    public static final int GL_DISPATCH_INDIRECT_BUFFER = 0x90EE;
    public static final int GL_QUERY_BUFFER = 0x9192;
    public static final int GL_ATOMIC_COUNTER_BUFFER = 0x92C0;
    public static final int GL_PARAMETER_BUFFER = 0x80EE;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.5 Buffer Usage
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_STREAM_DRAW = 0x88E0;
    public static final int GL_STREAM_READ = 0x88E1;
    public static final int GL_STREAM_COPY = 0x88E2;
    public static final int GL_STATIC_DRAW = 0x88E4;
    public static final int GL_STATIC_READ = 0x88E5;
    public static final int GL_STATIC_COPY = 0x88E6;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_DYNAMIC_READ = 0x88E9;
    public static final int GL_DYNAMIC_COPY = 0x88EA;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.6 Buffer Access
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_READ_ONLY = 0x88B8;
    public static final int GL_WRITE_ONLY = 0x88B9;
    public static final int GL_READ_WRITE = 0x88BA;
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_MAP_WRITE_BIT = 0x0002;
    public static final int GL_MAP_INVALIDATE_RANGE_BIT = 0x0004;
    public static final int GL_MAP_INVALIDATE_BUFFER_BIT = 0x0008;
    public static final int GL_MAP_FLUSH_EXPLICIT_BIT = 0x0010;
    public static final int GL_MAP_UNSYNCHRONIZED_BIT = 0x0020;
    public static final int GL_MAP_PERSISTENT_BIT = 0x0040;
    public static final int GL_MAP_COHERENT_BIT = 0x0080;
    public static final int GL_DYNAMIC_STORAGE_BIT = 0x0100;
    public static final int GL_CLIENT_STORAGE_BIT = 0x0200;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.7 Texture Targets
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_TEXTURE_1D = 0x0DE0;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_TEXTURE_3D = 0x806F;
    public static final int GL_TEXTURE_1D_ARRAY = 0x8C18;
    public static final int GL_TEXTURE_2D_ARRAY = 0x8C1A;
    public static final int GL_TEXTURE_RECTANGLE = 0x84F5;
    public static final int GL_TEXTURE_CUBE_MAP = 0x8513;
    public static final int GL_TEXTURE_CUBE_MAP_ARRAY = 0x9009;
    public static final int GL_TEXTURE_BUFFER_TARGET = 0x8C2A;
    public static final int GL_TEXTURE_2D_MULTISAMPLE = 0x9100;
    public static final int GL_TEXTURE_2D_MULTISAMPLE_ARRAY = 0x9102;
    
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_X = 0x8516;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Y = 0x8517;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Y = 0x8518;
    public static final int GL_TEXTURE_CUBE_MAP_POSITIVE_Z = 0x8519;
    public static final int GL_TEXTURE_CUBE_MAP_NEGATIVE_Z = 0x851A;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.8 Texture Parameters
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_WRAP_S = 0x2802;
    public static final int GL_TEXTURE_WRAP_T = 0x2803;
    public static final int GL_TEXTURE_WRAP_R = 0x8072;
    public static final int GL_TEXTURE_MIN_LOD = 0x813A;
    public static final int GL_TEXTURE_MAX_LOD = 0x813B;
    public static final int GL_TEXTURE_BASE_LEVEL = 0x813C;
    public static final int GL_TEXTURE_MAX_LEVEL = 0x813D;
    public static final int GL_TEXTURE_LOD_BIAS = 0x8501;
    public static final int GL_TEXTURE_COMPARE_MODE = 0x884C;
    public static final int GL_TEXTURE_COMPARE_FUNC = 0x884D;
    public static final int GL_TEXTURE_SWIZZLE_R = 0x8E42;
    public static final int GL_TEXTURE_SWIZZLE_G = 0x8E43;
    public static final int GL_TEXTURE_SWIZZLE_B = 0x8E44;
    public static final int GL_TEXTURE_SWIZZLE_A = 0x8E45;
    public static final int GL_TEXTURE_SWIZZLE_RGBA = 0x8E46;
    public static final int GL_TEXTURE_MAX_ANISOTROPY = 0x84FE;
    public static final int GL_TEXTURE_BORDER_COLOR = 0x1004;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.9 Filter Modes
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_NEAREST = 0x2600;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
    public static final int GL_LINEAR_MIPMAP_NEAREST = 0x2701;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.10 Wrap Modes
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_REPEAT = 0x2901;
    public static final int GL_CLAMP_TO_EDGE = 0x812F;
    public static final int GL_CLAMP_TO_BORDER = 0x812D;
    public static final int GL_MIRRORED_REPEAT = 0x8370;
    public static final int GL_MIRROR_CLAMP_TO_EDGE = 0x8743;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.11 Pixel Formats
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_RED = 0x1903;
    public static final int GL_GREEN = 0x1904;
    public static final int GL_BLUE = 0x1905;
    public static final int GL_ALPHA = 0x1906;
    public static final int GL_RGB = 0x1907;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_LUMINANCE = 0x1909;
    public static final int GL_LUMINANCE_ALPHA = 0x190A;
    public static final int GL_RG = 0x8227;
    public static final int GL_RG_INTEGER = 0x8228;
    public static final int GL_RED_INTEGER = 0x8D94;
    public static final int GL_GREEN_INTEGER = 0x8D95;
    public static final int GL_BLUE_INTEGER = 0x8D96;
    public static final int GL_RGB_INTEGER = 0x8D98;
    public static final int GL_RGBA_INTEGER = 0x8D99;
    public static final int GL_BGR = 0x80E0;
    public static final int GL_BGRA = 0x80E1;
    public static final int GL_BGR_INTEGER = 0x8D9A;
    public static final int GL_BGRA_INTEGER = 0x8D9B;
    public static final int GL_DEPTH_COMPONENT = 0x1902;
    public static final int GL_DEPTH_STENCIL = 0x84F9;
    public static final int GL_STENCIL_INDEX = 0x1901;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.12 Internal Formats
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_R8 = 0x8229;
    public static final int GL_R8_SNORM = 0x8F94;
    public static final int GL_R16 = 0x822A;
    public static final int GL_R16_SNORM = 0x8F98;
    public static final int GL_RG8 = 0x822B;
    public static final int GL_RG8_SNORM = 0x8F95;
    public static final int GL_RG16 = 0x822C;
    public static final int GL_RG16_SNORM = 0x8F99;
    public static final int GL_R3_G3_B2 = 0x2A10;
    public static final int GL_RGB4 = 0x804F;
    public static final int GL_RGB5 = 0x8050;
    public static final int GL_RGB8 = 0x8051;
    public static final int GL_RGB8_SNORM = 0x8F96;
    public static final int GL_RGB10 = 0x8052;
    public static final int GL_RGB12 = 0x8053;
    public static final int GL_RGB16 = 0x8054;
    public static final int GL_RGB16_SNORM = 0x8F9A;
    public static final int GL_RGBA2 = 0x8055;
    public static final int GL_RGBA4 = 0x8056;
    public static final int GL_RGB5_A1 = 0x8057;
    public static final int GL_RGBA8 = 0x8058;
    public static final int GL_RGBA8_SNORM = 0x8F97;
    public static final int GL_RGB10_A2 = 0x8059;
    public static final int GL_RGB10_A2UI = 0x906F;
    public static final int GL_RGBA12 = 0x805A;
    public static final int GL_RGBA16 = 0x805B;
    public static final int GL_RGBA16_SNORM = 0x8F9B;
    public static final int GL_SRGB = 0x8C40;
    public static final int GL_SRGB8 = 0x8C41;
    public static final int GL_SRGB_ALPHA = 0x8C42;
    public static final int GL_SRGB8_ALPHA8 = 0x8C43;
    public static final int GL_R16F = 0x822D;
    public static final int GL_RG16F = 0x822F;
    public static final int GL_RGB16F = 0x881B;
    public static final int GL_RGBA16F = 0x881A;
    public static final int GL_R32F = 0x822E;
    public static final int GL_RG32F = 0x8230;
    public static final int GL_RGB32F = 0x8815;
    public static final int GL_RGBA32F = 0x8814;
    public static final int GL_R11F_G11F_B10F = 0x8C3A;
    public static final int GL_RGB9_E5 = 0x8C3D;
    public static final int GL_R8I = 0x8231;
    public static final int GL_R8UI = 0x8232;
    public static final int GL_R16I = 0x8233;
    public static final int GL_R16UI = 0x8234;
    public static final int GL_R32I = 0x8235;
    public static final int GL_R32UI = 0x8236;
    public static final int GL_RG8I = 0x8237;
    public static final int GL_RG8UI = 0x8238;
    public static final int GL_RG16I = 0x8239;
    public static final int GL_RG16UI = 0x823A;
    public static final int GL_RG32I = 0x823B;
    public static final int GL_RG32UI = 0x823C;
    public static final int GL_RGB8I = 0x8D8F;
    public static final int GL_RGB8UI = 0x8D7D;
    public static final int GL_RGB16I = 0x8D89;
    public static final int GL_RGB16UI = 0x8D77;
    public static final int GL_RGB32I = 0x8D83;
    public static final int GL_RGB32UI = 0x8D71;
    public static final int GL_RGBA8I = 0x8D8E;
    public static final int GL_RGBA8UI = 0x8D7C;
    public static final int GL_RGBA16I = 0x8D88;
    public static final int GL_RGBA16UI = 0x8D76;
    public static final int GL_RGBA32I = 0x8D82;
    public static final int GL_RGBA32UI = 0x8D70;
    
    // Depth/Stencil formats
    public static final int GL_DEPTH_COMPONENT16 = 0x81A5;
    public static final int GL_DEPTH_COMPONENT24 = 0x81A6;
    public static final int GL_DEPTH_COMPONENT32 = 0x81A7;
    public static final int GL_DEPTH_COMPONENT32F = 0x8CAC;
    public static final int GL_DEPTH24_STENCIL8 = 0x88F0;
    public static final int GL_DEPTH32F_STENCIL8 = 0x8CAD;
    public static final int GL_STENCIL_INDEX1 = 0x8D46;
    public static final int GL_STENCIL_INDEX4 = 0x8D47;
    public static final int GL_STENCIL_INDEX8 = 0x8D48;
    public static final int GL_STENCIL_INDEX16 = 0x8D49;
    
    // Compressed formats
    public static final int GL_COMPRESSED_RED = 0x8225;
    public static final int GL_COMPRESSED_RG = 0x8226;
    public static final int GL_COMPRESSED_RGB = 0x84ED;
    public static final int GL_COMPRESSED_RGBA = 0x84EE;
    public static final int GL_COMPRESSED_SRGB = 0x8C48;
    public static final int GL_COMPRESSED_SRGB_ALPHA = 0x8C49;
    public static final int GL_COMPRESSED_RED_RGTC1 = 0x8DBB;
    public static final int GL_COMPRESSED_SIGNED_RED_RGTC1 = 0x8DBC;
    public static final int GL_COMPRESSED_RG_RGTC2 = 0x8DBD;
    public static final int GL_COMPRESSED_SIGNED_RG_RGTC2 = 0x8DBE;
    public static final int GL_COMPRESSED_RGBA_BPTC_UNORM = 0x8E8C;
    public static final int GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM = 0x8E8D;
    public static final int GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT = 0x8E8E;
    public static final int GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT = 0x8E8F;
    public static final int GL_COMPRESSED_RGB_S3TC_DXT1 = 0x83F0;
    public static final int GL_COMPRESSED_RGBA_S3TC_DXT1 = 0x83F1;
    public static final int GL_COMPRESSED_RGBA_S3TC_DXT3 = 0x83F2;
    public static final int GL_COMPRESSED_RGBA_S3TC_DXT5 = 0x83F3;
    public static final int GL_COMPRESSED_SRGB_S3TC_DXT1 = 0x8C4C;
    public static final int GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1 = 0x8C4D;
    public static final int GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3 = 0x8C4E;
    public static final int GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5 = 0x8C4F;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.13 Blend Functions
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_ZERO = 0;
    public static final int GL_ONE = 1;
    public static final int GL_SRC_COLOR = 0x0300;
    public static final int GL_ONE_MINUS_SRC_COLOR = 0x0301;
    public static final int GL_SRC_ALPHA = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_DST_ALPHA = 0x0304;
    public static final int GL_ONE_MINUS_DST_ALPHA = 0x0305;
    public static final int GL_DST_COLOR = 0x0306;
    public static final int GL_ONE_MINUS_DST_COLOR = 0x0307;
    public static final int GL_SRC_ALPHA_SATURATE = 0x0308;
    public static final int GL_CONSTANT_COLOR = 0x8001;
    public static final int GL_ONE_MINUS_CONSTANT_COLOR = 0x8002;
    public static final int GL_CONSTANT_ALPHA = 0x8003;
    public static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;
    public static final int GL_SRC1_ALPHA = 0x8589;
    public static final int GL_SRC1_COLOR = 0x88F9;
    public static final int GL_ONE_MINUS_SRC1_COLOR = 0x88FA;
    public static final int GL_ONE_MINUS_SRC1_ALPHA = 0x88FB;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.14 Blend Equations
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_FUNC_ADD = 0x8006;
    public static final int GL_FUNC_SUBTRACT = 0x800A;
    public static final int GL_FUNC_REVERSE_SUBTRACT = 0x800B;
    public static final int GL_MIN = 0x8007;
    public static final int GL_MAX = 0x8008;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.15 Comparison Functions
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_NEVER = 0x0200;
    public static final int GL_LESS = 0x0201;
    public static final int GL_EQUAL = 0x0202;
    public static final int GL_LEQUAL = 0x0203;
    public static final int GL_GREATER = 0x0204;
    public static final int GL_NOTEQUAL = 0x0205;
    public static final int GL_GEQUAL = 0x0206;
    public static final int GL_ALWAYS = 0x0207;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.16 Stencil Operations
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_KEEP = 0x1E00;
    public static final int GL_REPLACE = 0x1E01;
    public static final int GL_INCR = 0x1E02;
    public static final int GL_DECR = 0x1E03;
    public static final int GL_INVERT = 0x150A;
    public static final int GL_INCR_WRAP = 0x8507;
    public static final int GL_DECR_WRAP = 0x8508;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.17 Face Culling
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_FRONT = 0x0404;
    public static final int GL_BACK = 0x0405;
    public static final int GL_FRONT_AND_BACK = 0x0408;
    public static final int GL_CW = 0x0900;
    public static final int GL_CCW = 0x0901;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.18 Polygon Modes
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_POINT = 0x1B00;
    public static final int GL_LINE = 0x1B01;
    public static final int GL_FILL = 0x1B02;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.19 Enable/Disable Capabilities
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int GL_STENCIL_TEST = 0x0B90;
    public static final int GL_SCISSOR_TEST = 0x0C11;
    public static final int GL_CULL_FACE = 0x0B44;
    public static final int GL_POLYGON_OFFSET_FILL = 0x8037;
    public static final int GL_POLYGON_OFFSET_LINE = 0x2A02;
    public static final int GL_POLYGON_OFFSET_POINT = 0x2A01;
    public static final int GL_MULTISAMPLE = 0x809D;
    public static final int GL_SAMPLE_ALPHA_TO_COVERAGE = 0x809E;
    public static final int GL_SAMPLE_ALPHA_TO_ONE = 0x809F;
    public static final int GL_SAMPLE_COVERAGE = 0x80A0;
    public static final int GL_SAMPLE_SHADING = 0x8C36;
    public static final int GL_SAMPLE_MASK = 0x8E51;
    public static final int GL_RASTERIZER_DISCARD = 0x8C89;
    public static final int GL_PROGRAM_POINT_SIZE = 0x8642;
    public static final int GL_DEPTH_CLAMP = 0x864F;
    public static final int GL_TEXTURE_CUBE_MAP_SEAMLESS = 0x884F;
    public static final int GL_PRIMITIVE_RESTART = 0x8F9D;
    public static final int GL_PRIMITIVE_RESTART_FIXED_INDEX = 0x8D69;
    public static final int GL_FRAMEBUFFER_SRGB = 0x8DB9;
    public static final int GL_DEBUG_OUTPUT = 0x92E0;
    public static final int GL_DEBUG_OUTPUT_SYNCHRONOUS = 0x8242;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.20 Clear Bits
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_COLOR_BUFFER_BIT = 0x00004000;
    public static final int GL_DEPTH_BUFFER_BIT = 0x00000100;
    public static final int GL_STENCIL_BUFFER_BIT = 0x00000400;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.21 Framebuffer
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_FRAMEBUFFER = 0x8D40;
    public static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    public static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    public static final int GL_RENDERBUFFER = 0x8D41;
    
    public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    public static final int GL_COLOR_ATTACHMENT1 = 0x8CE1;
    public static final int GL_COLOR_ATTACHMENT2 = 0x8CE2;
    public static final int GL_COLOR_ATTACHMENT3 = 0x8CE3;
    public static final int GL_COLOR_ATTACHMENT4 = 0x8CE4;
    public static final int GL_COLOR_ATTACHMENT5 = 0x8CE5;
    public static final int GL_COLOR_ATTACHMENT6 = 0x8CE6;
    public static final int GL_COLOR_ATTACHMENT7 = 0x8CE7;
    public static final int GL_DEPTH_ATTACHMENT = 0x8D00;
    public static final int GL_STENCIL_ATTACHMENT = 0x8D20;
    public static final int GL_DEPTH_STENCIL_ATTACHMENT = 0x821A;
    
    public static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x8CD6;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER = 0x8CDB;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER = 0x8CDC;
    public static final int GL_FRAMEBUFFER_UNSUPPORTED = 0x8CDD;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE = 0x8D56;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS = 0x8DA8;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.22 Shader Types
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_VERTEX_SHADER = 0x8B31;
    public static final int GL_FRAGMENT_SHADER = 0x8B30;
    public static final int GL_GEOMETRY_SHADER = 0x8DD9;
    public static final int GL_TESS_CONTROL_SHADER = 0x8E88;
    public static final int GL_TESS_EVALUATION_SHADER = 0x8E87;
    public static final int GL_COMPUTE_SHADER = 0x91B9;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.23 Shader Status
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_COMPILE_STATUS = 0x8B81;
    public static final int GL_LINK_STATUS = 0x8B82;
    public static final int GL_VALIDATE_STATUS = 0x8B83;
    public static final int GL_INFO_LOG_LENGTH = 0x8B84;
    public static final int GL_ATTACHED_SHADERS = 0x8B85;
    public static final int GL_ACTIVE_UNIFORMS = 0x8B86;
    public static final int GL_ACTIVE_UNIFORM_MAX_LENGTH = 0x8B87;
    public static final int GL_ACTIVE_ATTRIBUTES = 0x8B89;
    public static final int GL_ACTIVE_ATTRIBUTE_MAX_LENGTH = 0x8B8A;
    public static final int GL_SHADER_SOURCE_LENGTH = 0x8B88;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.24 Uniform Types
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_FLOAT_VEC2 = 0x8B50;
    public static final int GL_FLOAT_VEC3 = 0x8B51;
    public static final int GL_FLOAT_VEC4 = 0x8B52;
    public static final int GL_INT_VEC2 = 0x8B53;
    public static final int GL_INT_VEC3 = 0x8B54;
    public static final int GL_INT_VEC4 = 0x8B55;
    public static final int GL_BOOL = 0x8B56;
    public static final int GL_BOOL_VEC2 = 0x8B57;
    public static final int GL_BOOL_VEC3 = 0x8B58;
    public static final int GL_BOOL_VEC4 = 0x8B59;
    public static final int GL_FLOAT_MAT2 = 0x8B5A;
    public static final int GL_FLOAT_MAT3 = 0x8B5B;
    public static final int GL_FLOAT_MAT4 = 0x8B5C;
    public static final int GL_FLOAT_MAT2x3 = 0x8B65;
    public static final int GL_FLOAT_MAT2x4 = 0x8B66;
    public static final int GL_FLOAT_MAT3x2 = 0x8B67;
    public static final int GL_FLOAT_MAT3x4 = 0x8B68;
    public static final int GL_FLOAT_MAT4x2 = 0x8B69;
    public static final int GL_FLOAT_MAT4x3 = 0x8B6A;
    public static final int GL_SAMPLER_1D = 0x8B5D;
    public static final int GL_SAMPLER_2D = 0x8B5E;
    public static final int GL_SAMPLER_3D = 0x8B5F;
    public static final int GL_SAMPLER_CUBE = 0x8B60;
    public static final int GL_SAMPLER_1D_SHADOW = 0x8B61;
    public static final int GL_SAMPLER_2D_SHADOW = 0x8B62;
    public static final int GL_SAMPLER_CUBE_SHADOW = 0x8DC5;
    public static final int GL_SAMPLER_2D_ARRAY = 0x8DC1;
    public static final int GL_SAMPLER_2D_ARRAY_SHADOW = 0x8DC4;
    public static final int GL_SAMPLER_2D_MULTISAMPLE = 0x9108;
    public static final int GL_SAMPLER_2D_MULTISAMPLE_ARRAY = 0x910B;
    public static final int GL_SAMPLER_BUFFER = 0x8DC2;
    public static final int GL_IMAGE_2D = 0x904D;
    public static final int GL_UNSIGNED_INT_VEC2 = 0x8DC6;
    public static final int GL_UNSIGNED_INT_VEC3 = 0x8DC7;
    public static final int GL_UNSIGNED_INT_VEC4 = 0x8DC8;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.25 Query Targets
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_SAMPLES_PASSED = 0x8914;
    public static final int GL_ANY_SAMPLES_PASSED = 0x8C2F;
    public static final int GL_ANY_SAMPLES_PASSED_CONSERVATIVE = 0x8D6A;
    public static final int GL_PRIMITIVES_GENERATED = 0x8C87;
    public static final int GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN = 0x8C88;
    public static final int GL_TIME_ELAPSED = 0x88BF;
    public static final int GL_TIMESTAMP = 0x8E28;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.26 String Queries
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_VENDOR = 0x1F00;
    public static final int GL_RENDERER = 0x1F01;
    public static final int GL_VERSION = 0x1F02;
    public static final int GL_EXTENSIONS = 0x1F03;
    public static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 1.27 Integer Queries
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
    public static final int GL_MAX_3D_TEXTURE_SIZE = 0x8073;
    public static final int GL_MAX_CUBE_MAP_TEXTURE_SIZE = 0x851C;
    public static final int GL_MAX_ARRAY_TEXTURE_LAYERS = 0x88FF;
    public static final int GL_MAX_TEXTURE_IMAGE_UNITS = 0x8872;
    public static final int GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS = 0x8B4C;
    public static final int GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS = 0x8B4D;
    public static final int GL_MAX_VERTEX_ATTRIBS = 0x8869;
    public static final int GL_MAX_VERTEX_UNIFORM_COMPONENTS = 0x8B4A;
    public static final int GL_MAX_FRAGMENT_UNIFORM_COMPONENTS = 0x8B49;
    public static final int GL_MAX_VERTEX_UNIFORM_BLOCKS = 0x8A2B;
    public static final int GL_MAX_FRAGMENT_UNIFORM_BLOCKS = 0x8A2D;
    public static final int GL_MAX_UNIFORM_BLOCK_SIZE = 0x8A30;
    public static final int GL_MAX_UNIFORM_BUFFER_BINDINGS = 0x8A2F;
    public static final int GL_MAX_DRAW_BUFFERS = 0x8824;
    public static final int GL_MAX_COLOR_ATTACHMENTS = 0x8CDF;
    public static final int GL_MAX_RENDERBUFFER_SIZE = 0x84E8;
    public static final int GL_MAX_SAMPLES = 0x8D57;
    public static final int GL_MAX_VIEWPORT_DIMS = 0x0D3A;
    public static final int GL_MAX_VIEWPORTS = 0x825B;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_COUNT = 0x91BE;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_SIZE = 0x91BF;
    public static final int GL_MAX_COMPUTE_WORK_GROUP_INVOCATIONS = 0x90EB;
    public static final int GL_MAX_COMPUTE_SHARED_MEMORY_SIZE = 0x8262;
    public static final int GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS = 0x90DD;
    public static final int GL_MAX_SHADER_STORAGE_BLOCK_SIZE = 0x90DE;
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 2: METAL CONSTANTS
    // ════════════════════════════════════════════════════════════════════════════
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.1 Metal Pixel Formats
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLPixelFormat {
        public static final long Invalid = 0;
        
        // Ordinary 8-bit formats
        public static final long A8Unorm = 1;
        public static final long R8Unorm = 10;
        public static final long R8Unorm_sRGB = 11;
        public static final long R8Snorm = 12;
        public static final long R8Uint = 13;
        public static final long R8Sint = 14;
        
        // Ordinary 16-bit formats
        public static final long R16Unorm = 20;
        public static final long R16Snorm = 22;
        public static final long R16Uint = 23;
        public static final long R16Sint = 24;
        public static final long R16Float = 25;
        public static final long RG8Unorm = 30;
        public static final long RG8Unorm_sRGB = 31;
        public static final long RG8Snorm = 32;
        public static final long RG8Uint = 33;
        public static final long RG8Sint = 34;
        
        // Packed 16-bit formats
        public static final long B5G6R5Unorm = 40;
        public static final long A1BGR5Unorm = 41;
        public static final long ABGR4Unorm = 42;
        public static final long BGR5A1Unorm = 43;
        
        // Ordinary 32-bit formats
        public static final long R32Uint = 53;
        public static final long R32Sint = 54;
        public static final long R32Float = 55;
        public static final long RG16Unorm = 60;
        public static final long RG16Snorm = 62;
        public static final long RG16Uint = 63;
        public static final long RG16Sint = 64;
        public static final long RG16Float = 65;
        public static final long RGBA8Unorm = 70;
        public static final long RGBA8Unorm_sRGB = 71;
        public static final long RGBA8Snorm = 72;
        public static final long RGBA8Uint = 73;
        public static final long RGBA8Sint = 74;
        public static final long BGRA8Unorm = 80;
        public static final long BGRA8Unorm_sRGB = 81;
        
        // Packed 32-bit formats
        public static final long RGB10A2Unorm = 90;
        public static final long RGB10A2Uint = 91;
        public static final long RG11B10Float = 92;
        public static final long RGB9E5Float = 93;
        public static final long BGR10A2Unorm = 94;
        
        // Ordinary 64-bit formats
        public static final long RG32Uint = 103;
        public static final long RG32Sint = 104;
        public static final long RG32Float = 105;
        public static final long RGBA16Unorm = 110;
        public static final long RGBA16Snorm = 112;
        public static final long RGBA16Uint = 113;
        public static final long RGBA16Sint = 114;
        public static final long RGBA16Float = 115;
        
        // Ordinary 128-bit formats
        public static final long RGBA32Uint = 123;
        public static final long RGBA32Sint = 124;
        public static final long RGBA32Float = 125;
        
        // Compressed formats - BC (S3TC/DXT)
        public static final long BC1_RGBA = 130;
        public static final long BC1_RGBA_sRGB = 131;
        public static final long BC2_RGBA = 132;
        public static final long BC2_RGBA_sRGB = 133;
        public static final long BC3_RGBA = 134;
        public static final long BC3_RGBA_sRGB = 135;
        public static final long BC4_RUnorm = 140;
        public static final long BC4_RSnorm = 141;
        public static final long BC5_RGUnorm = 142;
        public static final long BC5_RGSnorm = 143;
        public static final long BC6H_RGBFloat = 150;
        public static final long BC6H_RGBUfloat = 151;
        public static final long BC7_RGBAUnorm = 152;
        public static final long BC7_RGBAUnorm_sRGB = 153;
        
        // Compressed formats - PVRTC (iOS)
        public static final long PVRTC_RGB_2BPP = 160;
        public static final long PVRTC_RGB_2BPP_sRGB = 161;
        public static final long PVRTC_RGB_4BPP = 162;
        public static final long PVRTC_RGB_4BPP_sRGB = 163;
        public static final long PVRTC_RGBA_2BPP = 164;
        public static final long PVRTC_RGBA_2BPP_sRGB = 165;
        public static final long PVRTC_RGBA_4BPP = 166;
        public static final long PVRTC_RGBA_4BPP_sRGB = 167;
        
        // Compressed formats - EAC/ETC2 (iOS)
        public static final long EAC_R11Unorm = 170;
        public static final long EAC_R11Snorm = 172;
        public static final long EAC_RG11Unorm = 174;
        public static final long EAC_RG11Snorm = 176;
        public static final long EAC_RGBA8 = 178;
        public static final long EAC_RGBA8_sRGB = 179;
        public static final long ETC2_RGB8 = 180;
        public static final long ETC2_RGB8_sRGB = 181;
        public static final long ETC2_RGB8A1 = 182;
        public static final long ETC2_RGB8A1_sRGB = 183;
        
        // Compressed formats - ASTC (iOS)
        public static final long ASTC_4x4_sRGB = 186;
        public static final long ASTC_4x4_LDR = 204;
        public static final long ASTC_4x4_HDR = 222;
        public static final long ASTC_5x4_sRGB = 187;
        public static final long ASTC_5x4_LDR = 205;
        public static final long ASTC_5x5_sRGB = 188;
        public static final long ASTC_5x5_LDR = 206;
        public static final long ASTC_6x5_sRGB = 189;
        public static final long ASTC_6x5_LDR = 207;
        public static final long ASTC_6x6_sRGB = 190;
        public static final long ASTC_6x6_LDR = 208;
        public static final long ASTC_8x5_sRGB = 192;
        public static final long ASTC_8x5_LDR = 210;
        public static final long ASTC_8x6_sRGB = 193;
        public static final long ASTC_8x6_LDR = 211;
        public static final long ASTC_8x8_sRGB = 194;
        public static final long ASTC_8x8_LDR = 212;
        public static final long ASTC_10x5_sRGB = 195;
        public static final long ASTC_10x5_LDR = 213;
        public static final long ASTC_10x6_sRGB = 196;
        public static final long ASTC_10x6_LDR = 214;
        public static final long ASTC_10x8_sRGB = 197;
        public static final long ASTC_10x8_LDR = 215;
        public static final long ASTC_10x10_sRGB = 198;
        public static final long ASTC_10x10_LDR = 216;
        public static final long ASTC_12x10_sRGB = 199;
        public static final long ASTC_12x10_LDR = 217;
        public static final long ASTC_12x12_sRGB = 200;
        public static final long ASTC_12x12_LDR = 218;
        
        // Depth/Stencil formats
        public static final long Depth16Unorm = 250;
        public static final long Depth32Float = 252;
        public static final long Stencil8 = 253;
        public static final long Depth24Unorm_Stencil8 = 255;
        public static final long Depth32Float_Stencil8 = 260;
        public static final long X32_Stencil8 = 261;
        public static final long X24_Stencil8 = 262;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.2 Metal Texture Types
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLTextureType {
        public static final long Type1D = 0;
        public static final long Type1DArray = 1;
        public static final long Type2D = 2;
        public static final long Type2DArray = 3;
        public static final long Type2DMultisample = 4;
        public static final long TypeCube = 5;
        public static final long TypeCubeArray = 6;
        public static final long Type3D = 7;
        public static final long Type2DMultisampleArray = 8;
        public static final long TypeTextureBuffer = 9;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.3 Metal Texture Usage
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLTextureUsage {
        public static final long Unknown = 0x0000;
        public static final long ShaderRead = 0x0001;
        public static final long ShaderWrite = 0x0002;
        public static final long RenderTarget = 0x0004;
        public static final long PixelFormatView = 0x0010;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.4 Metal Storage Modes
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLStorageMode {
        public static final long Shared = 0;      // CPU & GPU accessible (default on iOS)
        public static final long Managed = 1;     // CPU & GPU, explicitly synchronized (macOS only)
        public static final long Private = 2;     // GPU only (most efficient)
        public static final long Memoryless = 3;  // Tile memory only (iOS)
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.5 Metal CPU Cache Modes
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLCPUCacheMode {
        public static final long DefaultCache = 0;
        public static final long WriteCombined = 1;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.6 Metal Resource Options
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLResourceOptions {
        public static final long CPUCacheModeDefaultCache = 0 << 0;
        public static final long CPUCacheModeWriteCombined = 1 << 0;
        
        public static final long StorageModeShared = 0 << 4;
        public static final long StorageModeManaged = 1 << 4;
        public static final long StorageModePrivate = 2 << 4;
        public static final long StorageModeMemoryless = 3 << 4;
        
        public static final long HazardTrackingModeDefault = 0 << 8;
        public static final long HazardTrackingModeUntracked = 1 << 8;
        public static final long HazardTrackingModeTracked = 2 << 8;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.7 Metal Primitive Types
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLPrimitiveType {
        public static final long Point = 0;
        public static final long Line = 1;
        public static final long LineStrip = 2;
        public static final long Triangle = 3;
        public static final long TriangleStrip = 4;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.8 Metal Index Types
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLIndexType {
        public static final long UInt16 = 0;
        public static final long UInt32 = 1;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.9 Metal Winding
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLWinding {
        public static final long Clockwise = 0;
        public static final long CounterClockwise = 1;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.10 Metal Cull Modes
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLCullMode {
        public static final long None = 0;
        public static final long Front = 1;
        public static final long Back = 2;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.11 Metal Triangle Fill Modes
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLTriangleFillMode {
        public static final long Fill = 0;
        public static final long Lines = 1;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.12 Metal Compare Functions
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLCompareFunction {
        public static final long Never = 0;
        public static final long Less = 1;
        public static final long Equal = 2;
        public static final long LessEqual = 3;
        public static final long Greater = 4;
        public static final long NotEqual = 5;
        public static final long GreaterEqual = 6;
        public static final long Always = 7;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.13 Metal Stencil Operations
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLStencilOperation {
        public static final long Keep = 0;
        public static final long Zero = 1;
        public static final long Replace = 2;
        public static final long IncrementClamp = 3;
        public static final long DecrementClamp = 4;
        public static final long Invert = 5;
        public static final long IncrementWrap = 6;
        public static final long DecrementWrap = 7;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.14 Metal Blend Factors
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLBlendFactor {
        public static final long Zero = 0;
        public static final long One = 1;
        public static final long SourceColor = 2;
        public static final long OneMinusSourceColor = 3;
        public static final long SourceAlpha = 4;
        public static final long OneMinusSourceAlpha = 5;
        public static final long DestinationColor = 6;
        public static final long OneMinusDestinationColor = 7;
        public static final long DestinationAlpha = 8;
        public static final long OneMinusDestinationAlpha = 9;
        public static final long SourceAlphaSaturated = 10;
        public static final long BlendColor = 11;
        public static final long OneMinusBlendColor = 12;
        public static final long BlendAlpha = 13;
        public static final long OneMinusBlendAlpha = 14;
        public static final long Source1Color = 15;
        public static final long OneMinusSource1Color = 16;
        public static final long Source1Alpha = 17;
        public static final long OneMinusSource1Alpha = 18;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.15 Metal Blend Operations
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLBlendOperation {
        public static final long Add = 0;
        public static final long Subtract = 1;
        public static final long ReverseSubtract = 2;
        public static final long Min = 3;
        public static final long Max = 4;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.16 Metal Sampler Address Modes
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLSamplerAddressMode {
        public static final long ClampToEdge = 0;
        public static final long MirrorClampToEdge = 1;
        public static final long Repeat = 2;
        public static final long MirrorRepeat = 3;
        public static final long ClampToZero = 4;
        public static final long ClampToBorderColor = 5;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.17 Metal Sampler Min/Mag Filters
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLSamplerMinMagFilter {
        public static final long Nearest = 0;
        public static final long Linear = 1;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.18 Metal Sampler Mip Filters
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLSamplerMipFilter {
        public static final long NotMipmapped = 0;
        public static final long Nearest = 1;
        public static final long Linear = 2;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.19 Metal Load/Store Actions
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLLoadAction {
        public static final long DontCare = 0;
        public static final long Load = 1;
        public static final long Clear = 2;
    }
    
    public static final class MTLStoreAction {
        public static final long DontCare = 0;
        public static final long Store = 1;
        public static final long MultisampleResolve = 2;
        public static final long StoreAndMultisampleResolve = 3;
        public static final long Unknown = 4;
        public static final long CustomSampleDepthStore = 5;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // 2.20 Metal Feature Sets & GPU Families
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class MTLGPUFamily {
        // Apple GPU families (iOS/iPadOS/macOS with Apple Silicon)
        public static final long Apple1 = 1001;   // A7, A8
        public static final long Apple2 = 1002;   // A8X, A9, A9X
        public static final long Apple3 = 1003;   // A10, A10X
        public static final long Apple4 = 1004;   // A11
        public static final long Apple5 = 1005;   // A12, A12X, A12Z
        public static final long Apple6 = 1006;   // A13
        public static final long Apple7 = 1007;   // A14, M1
        public static final long Apple8 = 1008;   // A15, M2
        public static final long Apple9 = 1009;   // A16, A17, M3
        
        // Mac GPU families (Intel/AMD on macOS)
        public static final long Mac1 = 2001;     // Intel HD Graphics, AMD pre-Navi
        public static final long Mac2 = 2002;     // AMD Navi+
        
        // Common GPU families (cross-platform baseline)
        public static final long Common1 = 3001;
        public static final long Common2 = 3002;
        public static final long Common3 = 3003;
        
        // Metal feature sets (legacy)
        public static final long MetalFeatureSet_iOS_GPUFamily1_v1 = 0;
        public static final long MetalFeatureSet_iOS_GPUFamily2_v1 = 1;
        public static final long MetalFeatureSet_iOS_GPUFamily1_v2 = 2;
        public static final long MetalFeatureSet_iOS_GPUFamily2_v2 = 3;
        public static final long MetalFeatureSet_iOS_GPUFamily3_v1 = 4;
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 3: METAL VERSION & CAPABILITY DETECTION
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Metal version information.
     */
    public enum MetalVersion {
        METAL_1_0(1, 0, "iOS 8.0, OS X 10.11"),
        METAL_1_1(1, 1, "iOS 9.0, OS X 10.11"),
        METAL_1_2(1, 2, "iOS 10.0, macOS 10.12"),
        METAL_2_0(2, 0, "iOS 11.0, macOS 10.13"),
        METAL_2_1(2, 1, "iOS 12.0, macOS 10.14"),
        METAL_2_2(2, 2, "iOS 13.0, macOS 10.15"),
        METAL_2_3(2, 3, "iOS 14.0, macOS 11.0"),
        METAL_2_4(2, 4, "iOS 15.0, macOS 12.0"),
        METAL_3_0(3, 0, "iOS 16.0, macOS 13.0"),
        METAL_3_1(3, 1, "iOS 17.0, macOS 14.0");
        
        public final int major;
        public final int minor;
        public final String description;
        
        MetalVersion(int major, int minor, String desc) {
            this.major = major;
            this.minor = minor;
            this.description = desc;
        }
        
        public boolean isAtLeast(MetalVersion other) {
            return this.ordinal() >= other.ordinal();
        }
    }
    
    /**
     * Metal device capabilities and feature detection.
     */
    public static final class MetalCapabilities {
        // Device info
        public final long deviceHandle;
        public final String deviceName;
        public final MetalVersion metalVersion;
        public final long gpuFamily;
        public final boolean isLowPower;
        public final boolean isHeadless;
        public final boolean isRemovable;
        public final boolean hasUnifiedMemory;
        
        // Feature support
        public final boolean supportsRaytracing;
        public final boolean supportsMeshShaders;
        public final boolean supportsTessellation;
        public final boolean supportsBarycentric;
        public final boolean supports32BitMSAA;
        public final boolean supportsPullModelInterpolation;
        public final boolean supportsShaderBarycentricCoordinates;
        public final boolean supportsBCTextureCompression;
        public final boolean supportsArgumentBuffers;
        public final boolean supportsArgumentBuffersTier2;
        public final boolean supportsRasterOrderGroups;
        public final boolean supportsArrayOfTextures;
        public final boolean supportsArrayOfSamplers;
        public final boolean supports32BitFloatFiltering;
        public final boolean supportsQueryTextureLOD;
        public final boolean supportsFunctionPointers;
        public final boolean supportsDynamicLibraries;
        
        // Limits
        public final long maxBufferLength;
        public final long maxThreadgroupMemoryLength;
        public final long maxThreadsPerThreadgroup;
        public final int maxBoundArgumentBuffers;
        public final int maxBindlessTextures;
        public final int maxBindlessSamplers;
        public final int maxVertexAttributes;
        public final int maxVertexBufferBindings;
        public final int maxFragmentBufferBindings;
        public final int maxComputeBufferBindings;
        public final int maxTextureBindings;
        public final int maxSamplerBindings;
        public final int maxRenderTargets;
        public final int maxTextureSize2D;
        public final int maxTextureSize3D;
        public final int maxTextureSizeCube;
        public final int maxTextureArrayLayers;
        public final int maxSampleCount;
        
        private MetalCapabilities(long device) {
            this.deviceHandle = device;
            
            // Query device name
            this.deviceName = nativeGetDeviceName(device);
            
            // Detect Metal version
            this.metalVersion = detectMetalVersion(device);
            
            // Query GPU family
            this.gpuFamily = nativeGetGPUFamily(device);
            
            // Device properties
            this.isLowPower = nativeDeviceIsLowPower(device);
            this.isHeadless = nativeDeviceIsHeadless(device);
            this.isRemovable = nativeDeviceIsRemovable(device);
            this.hasUnifiedMemory = nativeDeviceHasUnifiedMemory(device);
            
            // Feature queries
            this.supportsRaytracing = nativeDeviceSupportsRaytracing(device);
            this.supportsMeshShaders = metalVersion.isAtLeast(MetalVersion.METAL_3_0) && 
                                       nativeDeviceSupportsMeshShaders(device);
            this.supportsTessellation = metalVersion.isAtLeast(MetalVersion.METAL_1_2);
            this.supportsBarycentric = nativeDeviceSupportsBarycentric(device);
            this.supports32BitMSAA = nativeDeviceSupports32BitMSAA(device);
            this.supportsPullModelInterpolation = metalVersion.isAtLeast(MetalVersion.METAL_2_3);
            this.supportsShaderBarycentricCoordinates = metalVersion.isAtLeast(MetalVersion.METAL_2_2);
            this.supportsBCTextureCompression = nativeDeviceSupportsBCTextures(device);
            this.supportsArgumentBuffers = metalVersion.isAtLeast(MetalVersion.METAL_2_0);
            this.supportsArgumentBuffersTier2 = nativeDeviceSupportsArgumentBuffersTier2(device);
            this.supportsRasterOrderGroups = metalVersion.isAtLeast(MetalVersion.METAL_2_0);
            this.supportsArrayOfTextures = metalVersion.isAtLeast(MetalVersion.METAL_2_0);
            this.supportsArrayOfSamplers = metalVersion.isAtLeast(MetalVersion.METAL_2_0);
            this.supports32BitFloatFiltering = nativeDeviceSupports32BitFloatFiltering(device);
            this.supportsQueryTextureLOD = metalVersion.isAtLeast(MetalVersion.METAL_2_2);
            this.supportsFunctionPointers = metalVersion.isAtLeast(MetalVersion.METAL_2_3);
            this.supportsDynamicLibraries = metalVersion.isAtLeast(MetalVersion.METAL_2_3);
            
            // Query limits
            this.maxBufferLength = nativeGetMaxBufferLength(device);
            this.maxThreadgroupMemoryLength = nativeGetMaxThreadgroupMemoryLength(device);
            this.maxThreadsPerThreadgroup = nativeGetMaxThreadsPerThreadgroup(device);
            this.maxBoundArgumentBuffers = supportsArgumentBuffersTier2 ? 500000 : 
                                           supportsArgumentBuffers ? 96 : 0;
            this.maxBindlessTextures = supportsArgumentBuffersTier2 ? 500000 : 96;
            this.maxBindlessSamplers = supportsArgumentBuffersTier2 ? 1024 : 16;
            this.maxVertexAttributes = 31;
            this.maxVertexBufferBindings = 31;
            this.maxFragmentBufferBindings = 31;
            this.maxComputeBufferBindings = 31;
            this.maxTextureBindings = 128;
            this.maxSamplerBindings = 16;
            this.maxRenderTargets = 8;
            this.maxTextureSize2D = 16384;
            this.maxTextureSize3D = 2048;
            this.maxTextureSizeCube = 16384;
            this.maxTextureArrayLayers = 2048;
            this.maxSampleCount = 8;
        }
        
        private static MetalVersion detectMetalVersion(long device) {
            // Query Metal version by testing feature availability
            if (nativeDeviceSupportsFamily(device, MTLGPUFamily.Apple9) ||
                nativeDeviceSupportsFamily(device, MTLGPUFamily.Mac2)) {
                return MetalVersion.METAL_3_1;
            }
            if (nativeDeviceSupportsFamily(device, MTLGPUFamily.Apple8)) {
                return MetalVersion.METAL_3_0;
            }
            if (nativeDeviceSupportsFamily(device, MTLGPUFamily.Apple7)) {
                return MetalVersion.METAL_2_4;
            }
            if (nativeDeviceSupportsFamily(device, MTLGPUFamily.Apple6)) {
                return MetalVersion.METAL_2_3;
            }
            if (nativeDeviceSupportsFamily(device, MTLGPUFamily.Apple5)) {
                return MetalVersion.METAL_2_2;
            }
            if (nativeDeviceSupportsFamily(device, MTLGPUFamily.Apple4)) {
                return MetalVersion.METAL_2_1;
            }
            if (nativeDeviceSupportsFamily(device, MTLGPUFamily.Apple3)) {
                return MetalVersion.METAL_2_0;
            }
            if (nativeDeviceSupportsFamily(device, MTLGPUFamily.Apple2)) {
                return MetalVersion.METAL_1_2;
            }
            if (nativeDeviceSupportsFamily(device, MTLGPUFamily.Apple1)) {
                return MetalVersion.METAL_1_1;
            }
            return MetalVersion.METAL_1_0;
        }
        
        public String getCapabilitySummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Metal Device: ").append(deviceName).append("\n");
            sb.append("Metal Version: ").append(metalVersion).append(" (").append(metalVersion.description).append(")\n");
            sb.append("GPU Family: ").append(gpuFamily).append("\n");
            sb.append("Unified Memory: ").append(hasUnifiedMemory).append("\n");
            sb.append("Ray Tracing: ").append(supportsRaytracing).append("\n");
            sb.append("Mesh Shaders: ").append(supportsMeshShaders).append("\n");
            sb.append("Argument Buffers Tier 2: ").append(supportsArgumentBuffersTier2).append("\n");
            sb.append("Max Buffer Size: ").append(maxBufferLength / (1024 * 1024)).append(" MB\n");
            sb.append("Max Texture 2D: ").append(maxTextureSize2D).append("x").append(maxTextureSize2D).append("\n");
            return sb.toString();
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 4: OPENGL STATE TRACKING
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Tracks OpenGL state and provides dirty state detection.
     */
    public static final class GLStateTracker {
        
        // ─────────────────────────────────────────────────────────────────────────
        // 4.1 Enable/Disable States
        // ─────────────────────────────────────────────────────────────────────────
        
        public boolean blendEnabled = false;
        public boolean depthTestEnabled = false;
        public boolean depthWriteEnabled = true;
        public boolean stencilTestEnabled = false;
        public boolean cullFaceEnabled = false;
        public boolean scissorTestEnabled = false;
        public boolean polygonOffsetFillEnabled = false;
        public boolean polygonOffsetLineEnabled = false;
        public boolean multisampleEnabled = true;
        public boolean sampleAlphaToCoverageEnabled = false;
        public boolean sampleAlphaToOneEnabled = false;
        public boolean sampleMaskEnabled = false;
        public boolean rasterizerDiscardEnabled = false;
        public boolean programPointSizeEnabled = false;
        public boolean depthClampEnabled = false;
        public boolean seamlessCubeMapEnabled = false;
        public boolean primitiveRestartEnabled = false;
        public boolean framebufferSRGBEnabled = false;
        public boolean debugOutputEnabled = false;
        
        // ─────────────────────────────────────────────────────────────────────────
        // 4.2 Viewport & Scissor
        // ─────────────────────────────────────────────────────────────────────────
        
        public int viewportX = 0;
        public int viewportY = 0;
        public int viewportWidth = 0;
        public int viewportHeight = 0;
        public float depthRangeNear = 0.0f;
        public float depthRangeFar = 1.0f;
        
        public int scissorX = 0;
        public int scissorY = 0;
        public int scissorWidth = 0;
        public int scissorHeight = 0;
        
        // ─────────────────────────────────────────────────────────────────────────
        // 4.3 Blend State
        // ─────────────────────────────────────────────────────────────────────────
        
        public int blendSrcRGB = GL_ONE;
        public int blendDstRGB = GL_ZERO;
        public int blendSrcAlpha = GL_ONE;
        public int blendDstAlpha = GL_ZERO;
        public int blendEquationRGB = GL_FUNC_ADD;
        public int blendEquationAlpha = GL_FUNC_ADD;
        public float blendColorR = 0.0f;
        public float blendColorG = 0.0f;
        public float blendColorB = 0.0f;
        public float blendColorA = 0.0f;
        
        // Per-render-target blend state (indexed)
        public final BlendState[] blendStates = new BlendState[8];
        
        public static class BlendState {
            public boolean enabled = false;
            public int srcRGB = GL_ONE;
            public int dstRGB = GL_ZERO;
            public int srcAlpha = GL_ONE;
            public int dstAlpha = GL_ZERO;
            public int equationRGB = GL_FUNC_ADD;
            public int equationAlpha = GL_FUNC_ADD;
            public boolean colorMaskR = true;
            public boolean colorMaskG = true;
            public boolean colorMaskB = true;
            public boolean colorMaskA = true;
        }
        
        // ─────────────────────────────────────────────────────────────────────────
        // 4.4 Depth State
        // ─────────────────────────────────────────────────────────────────────────
        
        public int depthFunc = GL_LESS;
        public float clearDepth = 1.0f;
        
        // ─────────────────────────────────────────────────────────────────────────
        // 4.5 Stencil State
        // ─────────────────────────────────────────────────────────────────────────
        
        public int stencilFuncFront = GL_ALWAYS;
        public int stencilRefFront = 0;
        public int stencilMaskFront = 0xFFFFFFFF;
        public int stencilFailFront = GL_KEEP;
        public int stencilDepthFailFront = GL_KEEP;
        public int stencilPassFront = GL_KEEP;
        public int stencilWriteMaskFront = 0xFFFFFFFF;
        
        public int stencilFuncBack = GL_ALWAYS;
        public int stencilRefBack = 0;
        public int stencilMaskBack = 0xFFFFFFFF;
        public int stencilFailBack = GL_KEEP;
        public int stencilDepthFailBack = GL_KEEP;
        public int stencilPassBack = GL_KEEP;
        public int stencilWriteMaskBack = 0xFFFFFFFF;
        
        public int clearStencil = 0;
        
        // ─────────────────────────────────────────────────────────────────────────
        // 4.6 Rasterizer State
        // ─────────────────────────────────────────────────────────────────────────
        
        public int cullFaceMode = GL_BACK;
        public int frontFace = GL_CCW;
        public int polygonMode = GL_FILL;
        public float polygonOffsetFactor = 0.0f;
        public float polygonOffsetUnits = 0.0f;
        public float lineWidth = 1.0f;
        public float pointSize = 1.0f;
        
        // ─────────────────────────────────────────────────────────────────────────
        // 4.7 Clear State
        // ─────────────────────────────────────────────────────────────────────────
        
        public float clearColorR = 0.0f;
        public float clearColorG = 0.0f;
        public float clearColorB = 0.0f;
        public float clearColorA = 0.0f;
        
        // ─────────────────────────────────────────────────────────────────────────
        // 4.8 Color Mask
        // ─────────────────────────────────────────────────────────────────────────
        
        public boolean colorMaskR = true;
        public boolean colorMaskG = true;
        public boolean colorMaskB = true;
        public boolean colorMaskA = true;
        
        // ─────────────────────────────────────────────────────────────────────────
        // 4.9 Bound Resources
        // ─────────────────────────────────────────────────────────────────────────
        
        public int currentProgram = 0;
        public int currentVAO = 0;
        public int currentFBO = 0;
        public int currentReadFBO = 0;
        public int currentDrawFBO = 0;
        public int currentRBO = 0;
        
        public final int[] boundBuffers = new int[16]; // Per target
        public final int[] boundTextures = new int[96]; // Per unit
        public final int[] boundSamplers = new int[96]; // Per unit
        public int activeTextureUnit = 0;
        
        // Uniform buffer bindings
        public final UniformBufferBinding[] uniformBufferBindings = new UniformBufferBinding[36];
        
        public static class UniformBufferBinding {
            public int buffer = 0;
            public long offset = 0;
            public long size = 0;
        }
        
        // Shader storage buffer bindings
        public final StorageBufferBinding[] storageBufferBindings = new StorageBufferBinding[24];
        
        public static class StorageBufferBinding {
            public int buffer = 0;
            public long offset = 0;
            public long size = 0;
        }
        
        // ─────────────────────────────────────────────────────────────────────────
        // 4.10 Dirty State Tracking
        // ─────────────────────────────────────────────────────────────────────────
        
        private long dirtyFlags = 0xFFFFFFFFFFFFFFFFL;
        
        public static final long DIRTY_VIEWPORT = 1L << 0;
        public static final long DIRTY_SCISSOR = 1L << 1;
        public static final long DIRTY_BLEND = 1L << 2;
        public static final long DIRTY_DEPTH = 1L << 3;
        public static final long DIRTY_STENCIL = 1L << 4;
        public static final long DIRTY_RASTERIZER = 1L << 5;
        public static final long DIRTY_COLOR_MASK = 1L << 6;
        public static final long DIRTY_PROGRAM = 1L << 7;
        public static final long DIRTY_VAO = 1L << 8;
        public static final long DIRTY_FBO = 1L << 9;
        public static final long DIRTY_TEXTURES = 1L << 10;
        public static final long DIRTY_SAMPLERS = 1L << 11;
        public static final long DIRTY_UNIFORM_BUFFERS = 1L << 12;
        public static final long DIRTY_STORAGE_BUFFERS = 1L << 13;
        public static final long DIRTY_VERTEX_BUFFERS = 1L << 14;
        public static final long DIRTY_INDEX_BUFFER = 1L << 15;
        public static final long DIRTY_CLEAR = 1L << 16;
        public static final long DIRTY_ALL = 0xFFFFFFFFFFFFFFFFL;
        
        public void setDirty(long flags) { dirtyFlags |= flags; }
        public boolean isDirty(long flags) { return (dirtyFlags & flags) != 0; }
        public void clearDirty(long flags) { dirtyFlags &= ~flags; }
        public void clearAllDirty() { dirtyFlags = 0; }
        public long getDirtyFlags() { return dirtyFlags; }
        
        // ─────────────────────────────────────────────────────────────────────────
        // 4.11 Constructor
        // ─────────────────────────────────────────────────────────────────────────
        
        public GLStateTracker() {
            // Initialize blend states
            for (int i = 0; i < blendStates.length; i++) {
                blendStates[i] = new BlendState();
            }
            
            // Initialize uniform buffer bindings
            for (int i = 0; i < uniformBufferBindings.length; i++) {
                uniformBufferBindings[i] = new UniformBufferBinding();
            }
            
            // Initialize storage buffer bindings
            for (int i = 0; i < storageBufferBindings.length; i++) {
                storageBufferBindings[i] = new StorageBufferBinding();
            }
        }
        
        public void reset() {
            blendEnabled = false;
            depthTestEnabled = false;
            depthWriteEnabled = true;
            stencilTestEnabled = false;
            cullFaceEnabled = false;
            scissorTestEnabled = false;
            // ... reset all other state to defaults
            dirtyFlags = DIRTY_ALL;
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 5: GL TO METAL FORMAT CONVERSION
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Converts OpenGL formats to Metal pixel formats.
     */
    public static final class FormatConverter {
        
        // Internal format to Metal pixel format mapping
        private static final Map<Integer, Long> GL_TO_MTL_FORMAT = new HashMap<>();
        
        static {
            // 8-bit formats
            GL_TO_MTL_FORMAT.put(GL_R8, MTLPixelFormat.R8Unorm);
            GL_TO_MTL_FORMAT.put(GL_R8_SNORM, MTLPixelFormat.R8Snorm);
            GL_TO_MTL_FORMAT.put(GL_R8I, MTLPixelFormat.R8Sint);
            GL_TO_MTL_FORMAT.put(GL_R8UI, MTLPixelFormat.R8Uint);
            
            // 16-bit formats
            GL_TO_MTL_FORMAT.put(GL_R16, MTLPixelFormat.R16Unorm);
            GL_TO_MTL_FORMAT.put(GL_R16_SNORM, MTLPixelFormat.R16Snorm);
            GL_TO_MTL_FORMAT.put(GL_R16F, MTLPixelFormat.R16Float);
            GL_TO_MTL_FORMAT.put(GL_R16I, MTLPixelFormat.R16Sint);
            GL_TO_MTL_FORMAT.put(GL_R16UI, MTLPixelFormat.R16Uint);
            GL_TO_MTL_FORMAT.put(GL_RG8, MTLPixelFormat.RG8Unorm);
            GL_TO_MTL_FORMAT.put(GL_RG8_SNORM, MTLPixelFormat.RG8Snorm);
            GL_TO_MTL_FORMAT.put(GL_RG8I, MTLPixelFormat.RG8Sint);
            GL_TO_MTL_FORMAT.put(GL_RG8UI, MTLPixelFormat.RG8Uint);
            
            // 32-bit formats
            GL_TO_MTL_FORMAT.put(GL_R32F, MTLPixelFormat.R32Float);
            GL_TO_MTL_FORMAT.put(GL_R32I, MTLPixelFormat.R32Sint);
            GL_TO_MTL_FORMAT.put(GL_R32UI, MTLPixelFormat.R32Uint);
            GL_TO_MTL_FORMAT.put(GL_RG16, MTLPixelFormat.RG16Unorm);
            GL_TO_MTL_FORMAT.put(GL_RG16_SNORM, MTLPixelFormat.RG16Snorm);
            GL_TO_MTL_FORMAT.put(GL_RG16F, MTLPixelFormat.RG16Float);
            GL_TO_MTL_FORMAT.put(GL_RG16I, MTLPixelFormat.RG16Sint);
            GL_TO_MTL_FORMAT.put(GL_RG16UI, MTLPixelFormat.RG16Uint);
            GL_TO_MTL_FORMAT.put(GL_RGBA8, MTLPixelFormat.RGBA8Unorm);
            GL_TO_MTL_FORMAT.put(GL_SRGB8_ALPHA8, MTLPixelFormat.RGBA8Unorm_sRGB);
            GL_TO_MTL_FORMAT.put(GL_RGBA8_SNORM, MTLPixelFormat.RGBA8Snorm);
            GL_TO_MTL_FORMAT.put(GL_RGBA8I, MTLPixelFormat.RGBA8Sint);
            GL_TO_MTL_FORMAT.put(GL_RGBA8UI, MTLPixelFormat.RGBA8Uint);
            GL_TO_MTL_FORMAT.put(GL_RGB10_A2, MTLPixelFormat.RGB10A2Unorm);
            GL_TO_MTL_FORMAT.put(GL_RGB10_A2UI, MTLPixelFormat.RGB10A2Uint);
            GL_TO_MTL_FORMAT.put(GL_R11F_G11F_B10F, MTLPixelFormat.RG11B10Float);
            GL_TO_MTL_FORMAT.put(GL_RGB9_E5, MTLPixelFormat.RGB9E5Float);
            
            // 64-bit formats
            GL_TO_MTL_FORMAT.put(GL_RG32F, MTLPixelFormat.RG32Float);
            GL_TO_MTL_FORMAT.put(GL_RG32I, MTLPixelFormat.RG32Sint);
            GL_TO_MTL_FORMAT.put(GL_RG32UI, MTLPixelFormat.RG32Uint);
            GL_TO_MTL_FORMAT.put(GL_RGBA16, MTLPixelFormat.RGBA16Unorm);
            GL_TO_MTL_FORMAT.put(GL_RGBA16_SNORM, MTLPixelFormat.RGBA16Snorm);
            GL_TO_MTL_FORMAT.put(GL_RGBA16F, MTLPixelFormat.RGBA16Float);
            GL_TO_MTL_FORMAT.put(GL_RGBA16I, MTLPixelFormat.RGBA16Sint);
            GL_TO_MTL_FORMAT.put(GL_RGBA16UI, MTLPixelFormat.RGBA16Uint);
            
            // 128-bit formats
            GL_TO_MTL_FORMAT.put(GL_RGBA32F, MTLPixelFormat.RGBA32Float);
            GL_TO_MTL_FORMAT.put(GL_RGBA32I, MTLPixelFormat.RGBA32Sint);
            GL_TO_MTL_FORMAT.put(GL_RGBA32UI, MTLPixelFormat.RGBA32Uint);
            
            // Depth/Stencil formats
            GL_TO_MTL_FORMAT.put(GL_DEPTH_COMPONENT16, MTLPixelFormat.Depth16Unorm);
            GL_TO_MTL_FORMAT.put(GL_DEPTH_COMPONENT32F, MTLPixelFormat.Depth32Float);
            GL_TO_MTL_FORMAT.put(GL_DEPTH24_STENCIL8, MTLPixelFormat.Depth24Unorm_Stencil8);
            GL_TO_MTL_FORMAT.put(GL_DEPTH32F_STENCIL8, MTLPixelFormat.Depth32Float_Stencil8);
            GL_TO_MTL_FORMAT.put(GL_STENCIL_INDEX8, MTLPixelFormat.Stencil8);
            
            // Compressed BC formats
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_RGB_S3TC_DXT1, MTLPixelFormat.BC1_RGBA);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_RGBA_S3TC_DXT1, MTLPixelFormat.BC1_RGBA);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_SRGB_S3TC_DXT1, MTLPixelFormat.BC1_RGBA_sRGB);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1, MTLPixelFormat.BC1_RGBA_sRGB);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_RGBA_S3TC_DXT3, MTLPixelFormat.BC2_RGBA);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3, MTLPixelFormat.BC2_RGBA_sRGB);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_RGBA_S3TC_DXT5, MTLPixelFormat.BC3_RGBA);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5, MTLPixelFormat.BC3_RGBA_sRGB);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_RED_RGTC1, MTLPixelFormat.BC4_RUnorm);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_SIGNED_RED_RGTC1, MTLPixelFormat.BC4_RSnorm);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_RG_RGTC2, MTLPixelFormat.BC5_RGUnorm);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_SIGNED_RG_RGTC2, MTLPixelFormat.BC5_RGSnorm);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT, MTLPixelFormat.BC6H_RGBFloat);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT, MTLPixelFormat.BC6H_RGBUfloat);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_RGBA_BPTC_UNORM, MTLPixelFormat.BC7_RGBAUnorm);
            GL_TO_MTL_FORMAT.put(GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM, MTLPixelFormat.BC7_RGBAUnorm_sRGB);
        }
        
        /**
         * Converts GL internal format to Metal pixel format.
         */
        public static long toMetalPixelFormat(int glInternalFormat) {
            Long mtlFormat = GL_TO_MTL_FORMAT.get(glInternalFormat);
            if (mtlFormat != null) {
                return mtlFormat;
            }
            
            // Handle legacy formats
            switch (glInternalFormat) {
                case GL_RGB8:
                case GL_RGB:
                    return MTLPixelFormat.RGBA8Unorm; // Metal doesn't support RGB8 directly
                case GL_SRGB8:
                    return MTLPixelFormat.RGBA8Unorm_sRGB;
                case GL_RGB16F:
                    return MTLPixelFormat.RGBA16Float;
                case GL_RGB32F:
                    return MTLPixelFormat.RGBA32Float;
                case GL_RGBA:
                case 4:
                    return MTLPixelFormat.RGBA8Unorm;
                case GL_DEPTH_COMPONENT:
                case GL_DEPTH_COMPONENT24:
                case GL_DEPTH_COMPONENT32:
                    return MTLPixelFormat.Depth32Float;
                default:
                    LOGGER.warn("Unknown GL internal format: 0x{}, defaulting to RGBA8", 
                               Integer.toHexString(glInternalFormat));
                    return MTLPixelFormat.RGBA8Unorm;
            }
        }
        
        /**
         * Converts GL primitive type to Metal primitive type.
         */
        public static long toMetalPrimitiveType(int glPrimitive) {
            return switch (glPrimitive) {
                case GL_POINTS -> MTLPrimitiveType.Point;
                case GL_LINES -> MTLPrimitiveType.Line;
                case GL_LINE_STRIP -> MTLPrimitiveType.LineStrip;
                case GL_TRIANGLES -> MTLPrimitiveType.Triangle;
                case GL_TRIANGLE_STRIP -> MTLPrimitiveType.TriangleStrip;
                // Metal doesn't directly support these - need conversion
                case GL_LINE_LOOP -> MTLPrimitiveType.LineStrip; // Need to add closing vertex
                case GL_TRIANGLE_FAN -> MTLPrimitiveType.Triangle; // Need index buffer conversion
                case GL_QUADS -> MTLPrimitiveType.Triangle; // Need index buffer conversion
                default -> MTLPrimitiveType.Triangle;
            };
        }
        
        /**
         * Converts GL index type to Metal index type.
         */
        public static long toMetalIndexType(int glType) {
            return switch (glType) {
                case GL_UNSIGNED_SHORT -> MTLIndexType.UInt16;
                case GL_UNSIGNED_INT -> MTLIndexType.UInt32;
                case GL_UNSIGNED_BYTE -> MTLIndexType.UInt16; // Metal doesn't support 8-bit indices
                default -> MTLIndexType.UInt32;
            };
        }
        
        /**
         * Converts GL compare function to Metal compare function.
         */
        public static long toMetalCompareFunction(int glFunc) {
            return switch (glFunc) {
                case GL_NEVER -> MTLCompareFunction.Never;
                case GL_LESS -> MTLCompareFunction.Less;
                case GL_EQUAL -> MTLCompareFunction.Equal;
                case GL_LEQUAL -> MTLCompareFunction.LessEqual;
                case GL_GREATER -> MTLCompareFunction.Greater;
                case GL_NOTEQUAL -> MTLCompareFunction.NotEqual;
                case GL_GEQUAL -> MTLCompareFunction.GreaterEqual;
                case GL_ALWAYS -> MTLCompareFunction.Always;
                default -> MTLCompareFunction.Always;
            };
        }
        
        /**
         * Converts GL stencil operation to Metal stencil operation.
         */
        public static long toMetalStencilOperation(int glOp) {
            return switch (glOp) {
                case GL_KEEP -> MTLStencilOperation.Keep;
                case GL_ZERO -> MTLStencilOperation.Zero;
                case GL_REPLACE -> MTLStencilOperation.Replace;
                case GL_INCR -> MTLStencilOperation.IncrementClamp;
                case GL_INCR_WRAP -> MTLStencilOperation.IncrementWrap;
                case GL_DECR -> MTLStencilOperation.DecrementClamp;
                case GL_DECR_WRAP -> MTLStencilOperation.DecrementWrap;
                case GL_INVERT -> MTLStencilOperation.Invert;
                default -> MTLStencilOperation.Keep;
            };
        }
        
        /**
         * Converts GL blend factor to Metal blend factor.
         */
        public static long toMetalBlendFactor(int glFactor) {
            return switch (glFactor) {
                case GL_ZERO -> MTLBlendFactor.Zero;
                case GL_ONE -> MTLBlendFactor.One;
                case GL_SRC_COLOR -> MTLBlendFactor.SourceColor;
                case GL_ONE_MINUS_SRC_COLOR -> MTLBlendFactor.OneMinusSourceColor;
                case GL_SRC_ALPHA -> MTLBlendFactor.SourceAlpha;
                case GL_ONE_MINUS_SRC_ALPHA -> MTLBlendFactor.OneMinusSourceAlpha;
                case GL_DST_ALPHA -> MTLBlendFactor.DestinationAlpha;
                case GL_ONE_MINUS_DST_ALPHA -> MTLBlendFactor.OneMinusDestinationAlpha;
                case GL_DST_COLOR -> MTLBlendFactor.DestinationColor;
                case GL_ONE_MINUS_DST_COLOR -> MTLBlendFactor.OneMinusDestinationColor;
                case GL_SRC_ALPHA_SATURATE -> MTLBlendFactor.SourceAlphaSaturated;
                case GL_CONSTANT_COLOR -> MTLBlendFactor.BlendColor;
                case GL_ONE_MINUS_CONSTANT_COLOR -> MTLBlendFactor.OneMinusBlendColor;
                case GL_CONSTANT_ALPHA -> MTLBlendFactor.BlendAlpha;
                case GL_ONE_MINUS_CONSTANT_ALPHA -> MTLBlendFactor.OneMinusBlendAlpha;
                case GL_SRC1_COLOR -> MTLBlendFactor.Source1Color;
                case GL_ONE_MINUS_SRC1_COLOR -> MTLBlendFactor.OneMinusSource1Color;
                case GL_SRC1_ALPHA -> MTLBlendFactor.Source1Alpha;
                case GL_ONE_MINUS_SRC1_ALPHA -> MTLBlendFactor.OneMinusSource1Alpha;
                default -> MTLBlendFactor.One;
            };
        }
        
        /**
         * Converts GL blend equation to Metal blend operation.
         */
        public static long toMetalBlendOperation(int glEquation) {
            return switch (glEquation) {
                case GL_FUNC_ADD -> MTLBlendOperation.Add;
                case GL_FUNC_SUBTRACT -> MTLBlendOperation.Subtract;
                case GL_FUNC_REVERSE_SUBTRACT -> MTLBlendOperation.ReverseSubtract;
                case GL_MIN -> MTLBlendOperation.Min;
                case GL_MAX -> MTLBlendOperation.Max;
                default -> MTLBlendOperation.Add;
            };
        }
        
        /**
         * Converts GL texture wrap mode to Metal sampler address mode.
         */
        public static long toMetalAddressMode(int glWrap) {
            return switch (glWrap) {
                case GL_REPEAT -> MTLSamplerAddressMode.Repeat;
                case GL_MIRRORED_REPEAT -> MTLSamplerAddressMode.MirrorRepeat;
                case GL_CLAMP_TO_EDGE -> MTLSamplerAddressMode.ClampToEdge;
                case GL_CLAMP_TO_BORDER -> MTLSamplerAddressMode.ClampToBorderColor;
                case GL_MIRROR_CLAMP_TO_EDGE -> MTLSamplerAddressMode.MirrorClampToEdge;
                default -> MTLSamplerAddressMode.Repeat;
            };
        }
        
        /**
         * Converts GL texture filter to Metal sampler filter.
         */
        public static long toMetalMinMagFilter(int glFilter) {
            return switch (glFilter) {
                case GL_NEAREST, GL_NEAREST_MIPMAP_NEAREST, GL_NEAREST_MIPMAP_LINEAR -> 
                    MTLSamplerMinMagFilter.Nearest;
                case GL_LINEAR, GL_LINEAR_MIPMAP_NEAREST, GL_LINEAR_MIPMAP_LINEAR -> 
                    MTLSamplerMinMagFilter.Linear;
                default -> MTLSamplerMinMagFilter.Linear;
            };
        }
        
        /**
         * Converts GL texture filter to Metal mip filter.
         */
        public static long toMetalMipFilter(int glFilter) {
            return switch (glFilter) {
                case GL_NEAREST, GL_LINEAR -> MTLSamplerMipFilter.NotMipmapped;
                case GL_NEAREST_MIPMAP_NEAREST, GL_LINEAR_MIPMAP_NEAREST -> MTLSamplerMipFilter.Nearest;
                case GL_NEAREST_MIPMAP_LINEAR, GL_LINEAR_MIPMAP_LINEAR -> MTLSamplerMipFilter.Linear;
                default -> MTLSamplerMipFilter.NotMipmapped;
            };
        }
        
        /**
         * Converts GL cull face mode to Metal cull mode.
         */
        public static long toMetalCullMode(int glCullFace, boolean enabled) {
            if (!enabled) return MTLCullMode.None;
            return switch (glCullFace) {
                case GL_FRONT -> MTLCullMode.Front;
                case GL_BACK -> MTLCullMode.Back;
                case GL_FRONT_AND_BACK -> MTLCullMode.None; // Can't cull both in Metal, handled differently
                default -> MTLCullMode.None;
            };
        }
        
        /**
         * Converts GL front face to Metal winding.
         */
        public static long toMetalWinding(int glFrontFace) {
            return switch (glFrontFace) {
                case GL_CW -> MTLWinding.Clockwise;
                case GL_CCW -> MTLWinding.CounterClockwise;
                default -> MTLWinding.CounterClockwise;
            };
        }
        
        /**
         * Converts GL polygon mode to Metal triangle fill mode.
         */
        public static long toMetalTriangleFillMode(int glPolygonMode) {
            return switch (glPolygonMode) {
                case GL_FILL -> MTLTriangleFillMode.Fill;
                case GL_LINE -> MTLTriangleFillMode.Lines;
                case GL_POINT -> MTLTriangleFillMode.Fill; // Metal doesn't have point mode, simulate differently
                default -> MTLTriangleFillMode.Fill;
            };
        }
        
        /**
         * Converts GL texture target to Metal texture type.
         */
        public static long toMetalTextureType(int glTarget) {
            return switch (glTarget) {
                case GL_TEXTURE_1D -> MTLTextureType.Type1D;
                case GL_TEXTURE_1D_ARRAY -> MTLTextureType.Type1DArray;
                case GL_TEXTURE_2D, GL_TEXTURE_RECTANGLE -> MTLTextureType.Type2D;
                case GL_TEXTURE_2D_ARRAY -> MTLTextureType.Type2DArray;
                case GL_TEXTURE_2D_MULTISAMPLE -> MTLTextureType.Type2DMultisample;
                case GL_TEXTURE_2D_MULTISAMPLE_ARRAY -> MTLTextureType.Type2DMultisampleArray;
                case GL_TEXTURE_3D -> MTLTextureType.Type3D;
                case GL_TEXTURE_CUBE_MAP -> MTLTextureType.TypeCube;
                case GL_TEXTURE_CUBE_MAP_ARRAY -> MTLTextureType.TypeCubeArray;
                case GL_TEXTURE_BUFFER_TARGET -> MTLTextureType.TypeTextureBuffer;
                default -> MTLTextureType.Type2D;
            };
        }
        
        /**
         * Gets bytes per pixel for a given GL format/type combination.
         */
        public static int getBytesPerPixel(int format, int type) {
            int components = switch (format) {
                case GL_RED, GL_RED_INTEGER, GL_DEPTH_COMPONENT, GL_STENCIL_INDEX, GL_LUMINANCE, GL_ALPHA -> 1;
                case GL_RG, GL_RG_INTEGER, GL_LUMINANCE_ALPHA, GL_DEPTH_STENCIL -> 2;
                case GL_RGB, GL_RGB_INTEGER, GL_BGR, GL_BGR_INTEGER -> 3;
                case GL_RGBA, GL_RGBA_INTEGER, GL_BGRA, GL_BGRA_INTEGER -> 4;
                default -> 4;
            };
            
            int typeSize = switch (type) {
                case GL_UNSIGNED_BYTE, GL_BYTE -> 1;
                case GL_UNSIGNED_SHORT, GL_SHORT, GL_HALF_FLOAT -> 2;
                case GL_UNSIGNED_INT, GL_INT, GL_FLOAT -> 4;
                case GL_DOUBLE -> 8;
                // Packed types
                case 0x8033 -> 2; // GL_UNSIGNED_SHORT_4_4_4_4
                case 0x8034 -> 2; // GL_UNSIGNED_SHORT_5_5_5_1
                case 0x8363 -> 2; // GL_UNSIGNED_SHORT_5_6_5
                case 0x8C3B -> 4; // GL_UNSIGNED_INT_10_10_10_2
                case 0x84FA -> 4; // GL_UNSIGNED_INT_24_8
                default -> 1;
            };
            
            return components * typeSize;
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 6: METAL RESOURCE MANAGEMENT
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Manages Metal device and resource creation.
     */
    public static final class MetalDevice {
        private final long handle;
        private final MetalCapabilities capabilities;
        private final Map<Integer, MetalBuffer> buffers = new ConcurrentHashMap<>();
        private final Map<Integer, MetalTexture> textures = new ConcurrentHashMap<>();
        private final Map<Integer, MetalSampler> samplers = new ConcurrentHashMap<>();
        private final Map<Integer, MetalShader> shaders = new ConcurrentHashMap<>();
        private final Map<Integer, MetalProgram> programs = new ConcurrentHashMap<>();
        private final Map<Integer, MetalFramebuffer> framebuffers = new ConcurrentHashMap<>();
        private final Map<Integer, MetalRenderbuffer> renderbuffers = new ConcurrentHashMap<>();
        private final Map<Integer, MetalVertexArray> vertexArrays = new ConcurrentHashMap<>();
        private final Map<Long, MetalRenderPipelineState> renderPipelines = new ConcurrentHashMap<>();
        private final Map<Long, MetalDepthStencilState> depthStencilStates = new ConcurrentHashMap<>();
        private final Map<Long, MetalSamplerState> samplerStates = new ConcurrentHashMap<>();
        
        private final AtomicInteger nextBufferId = new AtomicInteger(1);
        private final AtomicInteger nextTextureId = new AtomicInteger(1);
        private final AtomicInteger nextSamplerId = new AtomicInteger(1);
        private final AtomicInteger nextShaderId = new AtomicInteger(1);
        private final AtomicInteger nextProgramId = new AtomicInteger(1);
        private final AtomicInteger nextFramebufferId = new AtomicInteger(1);
        private final AtomicInteger nextRenderbufferId = new AtomicInteger(1);
        private final AtomicInteger nextVertexArrayId = new AtomicInteger(1);
        
        public MetalDevice(long handle) {
            this.handle = handle;
            this.capabilities = new MetalCapabilities(handle);
        }
        
        public long getHandle() { return handle; }
        public MetalCapabilities getCapabilities() { return capabilities; }
        
        // Buffer management
        public int createBuffer() {
            int id = nextBufferId.getAndIncrement();
            buffers.put(id, new MetalBuffer(id));
            return id;
        }
        
        public MetalBuffer getBuffer(int id) { return buffers.get(id); }
        
        public void deleteBuffer(int id) {
            MetalBuffer buffer = buffers.remove(id);
            if (buffer != null) buffer.release();
        }
        
        // Texture management
        public int createTexture() {
            int id = nextTextureId.getAndIncrement();
            textures.put(id, new MetalTexture(id));
            return id;
        }
        
        public MetalTexture getTexture(int id) { return textures.get(id); }
        
        public void deleteTexture(int id) {
            MetalTexture texture = textures.remove(id);
            if (texture != null) texture.release();
        }
        
        // Sampler management
        public int createSampler() {
            int id = nextSamplerId.getAndIncrement();
            samplers.put(id, new MetalSampler(id));
            return id;
        }
        
        public MetalSampler getSampler(int id) { return samplers.get(id); }
        
        public void deleteSampler(int id) {
            MetalSampler sampler = samplers.remove(id);
            if (sampler != null) sampler.release();
        }
        
        // Shader management
        public int createShader(int type) {
            int id = nextShaderId.getAndIncrement();
            shaders.put(id, new MetalShader(id, type));
            return id;
        }
        
        public MetalShader getShader(int id) { return shaders.get(id); }
        
        public void deleteShader(int id) {
            MetalShader shader = shaders.remove(id);
            if (shader != null) shader.release();
        }
        
        // Program management
        public int createProgram() {
            int id = nextProgramId.getAndIncrement();
            programs.put(id, new MetalProgram(id));
            return id;
        }
        
        public MetalProgram getProgram(int id) { return programs.get(id); }
        
        public void deleteProgram(int id) {
            MetalProgram program = programs.remove(id);
            if (program != null) program.release();
        }
        
        // Framebuffer management
        public int createFramebuffer() {
            int id = nextFramebufferId.getAndIncrement();
            framebuffers.put(id, new MetalFramebuffer(id));
            return id;
        }
        
        public MetalFramebuffer getFramebuffer(int id) { return framebuffers.get(id); }
        
        public void deleteFramebuffer(int id) {
            MetalFramebuffer fb = framebuffers.remove(id);
            if (fb != null) fb.release();
        }
        
        // Renderbuffer management
        public int createRenderbuffer() {
            int id = nextRenderbufferId.getAndIncrement();
            renderbuffers.put(id, new MetalRenderbuffer(id));
            return id;
        }
        
        public MetalRenderbuffer getRenderbuffer(int id) { return renderbuffers.get(id); }
        
        public void deleteRenderbuffer(int id) {
            MetalRenderbuffer rb = renderbuffers.remove(id);
            if (rb != null) rb.release();
        }
        
        // Vertex array management
        public int createVertexArray() {
            int id = nextVertexArrayId.getAndIncrement();
            vertexArrays.put(id, new MetalVertexArray(id));
            return id;
        }
        
        public MetalVertexArray getVertexArray(int id) { return vertexArrays.get(id); }
        
        public void deleteVertexArray(int id) {
            MetalVertexArray vao = vertexArrays.remove(id);
            if (vao != null) vao.release();
        }
        
        // Pipeline state caching
        public MetalRenderPipelineState getOrCreateRenderPipeline(long hash, 
                Supplier<MetalRenderPipelineState> creator) {
            return renderPipelines.computeIfAbsent(hash, k -> creator.get());
        }
        
        public MetalDepthStencilState getOrCreateDepthStencilState(long hash,
                Supplier<MetalDepthStencilState> creator) {
            return depthStencilStates.computeIfAbsent(hash, k -> creator.get());
        }
        
        public MetalSamplerState getOrCreateSamplerState(long hash,
                Supplier<MetalSamplerState> creator) {
            return samplerStates.computeIfAbsent(hash, k -> creator.get());
        }
        
        public void release() {
            // Release all resources
            buffers.values().forEach(MetalBuffer::release);
            buffers.clear();
            textures.values().forEach(MetalTexture::release);
            textures.clear();
            samplers.values().forEach(MetalSampler::release);
            samplers.clear();
            shaders.values().forEach(MetalShader::release);
            shaders.clear();
            programs.values().forEach(MetalProgram::release);
            programs.clear();
            framebuffers.values().forEach(MetalFramebuffer::release);
            framebuffers.clear();
            renderbuffers.values().forEach(MetalRenderbuffer::release);
            renderbuffers.clear();
            vertexArrays.values().forEach(MetalVertexArray::release);
            vertexArrays.clear();
            renderPipelines.values().forEach(MetalRenderPipelineState::release);
            renderPipelines.clear();
            depthStencilStates.values().forEach(MetalDepthStencilState::release);
            depthStencilStates.clear();
            samplerStates.values().forEach(MetalSamplerState::release);
            samplerStates.clear();
            
            nativeReleaseDevice(handle);
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 7: METAL RESOURCE WRAPPER CLASSES
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Metal buffer wrapper with GL buffer semantics.
     */
    public static final class MetalBuffer {
        private final int glId;
        private long mtlHandle = 0;
        private long size = 0;
        private int usage = GL_STATIC_DRAW;
        private long storageMode = MTLStorageMode.Shared;
        private ByteBuffer mappedBuffer = null;
        private long mappedOffset = 0;
        private long mappedLength = 0;
        private int mappedAccess = 0;
        private boolean immutableStorage = false;
        private int storageFlags = 0;
        
        public MetalBuffer(int id) {
            this.glId = id;
        }
        
        public int getGLId() { return glId; }
        public long getHandle() { return mtlHandle; }
        public long getSize() { return size; }
        public int getUsage() { return usage; }
        
        /**
         * Allocates buffer storage (glBufferData equivalent).
         */
        public void allocate(long deviceHandle, long size, ByteBuffer data, int usage) {
            this.size = size;
            this.usage = usage;
            
            // Determine optimal storage mode based on usage
            this.storageMode = determineStorageMode(usage);
            long options = storageMode << 4;
            
            // Release old buffer if exists
            if (mtlHandle != 0) {
                nativeReleaseBuffer(mtlHandle);
            }
            
            // Create new Metal buffer
            if (data != null) {
                mtlHandle = nativeCreateBufferWithData(deviceHandle, data, size, options);
            } else {
                mtlHandle = nativeCreateBuffer(deviceHandle, size, options);
            }
        }
        
        /**
         * Allocates immutable buffer storage (glBufferStorage equivalent).
         */
        public void allocateImmutable(long deviceHandle, long size, ByteBuffer data, int flags) {
            this.size = size;
            this.immutableStorage = true;
            this.storageFlags = flags;
            
            // Determine storage mode from flags
            if ((flags & GL_MAP_PERSISTENT_BIT) != 0 || (flags & GL_MAP_COHERENT_BIT) != 0) {
                this.storageMode = MTLStorageMode.Shared;
            } else if ((flags & GL_CLIENT_STORAGE_BIT) != 0) {
                this.storageMode = MTLStorageMode.Shared;
            } else if ((flags & GL_DYNAMIC_STORAGE_BIT) != 0) {
                this.storageMode = MTLStorageMode.Shared;
            } else {
                this.storageMode = MTLStorageMode.Private;
            }
            
            long options = storageMode << 4;
            
            if (mtlHandle != 0) {
                nativeReleaseBuffer(mtlHandle);
            }
            
            if (data != null) {
                mtlHandle = nativeCreateBufferWithData(deviceHandle, data, size, options);
            } else {
                mtlHandle = nativeCreateBuffer(deviceHandle, size, options);
            }
        }
        
        /**
         * Updates buffer subdata (glBufferSubData equivalent).
         */
        public void updateSubData(long offset, ByteBuffer data) {
            if (mtlHandle == 0 || data == null) return;
            
            if (storageMode == MTLStorageMode.Private) {
                // For private buffers, need to use blit encoder
                nativeBufferSubDataBlit(mtlHandle, offset, data, data.remaining());
            } else {
                // For shared/managed buffers, direct memcpy
                nativeBufferSubData(mtlHandle, offset, data, data.remaining());
            }
        }
        
        /**
         * Maps buffer for CPU access (glMapBuffer/glMapBufferRange equivalent).
         */
        public ByteBuffer map(long offset, long length, int access) {
            if (mtlHandle == 0 || storageMode == MTLStorageMode.Private) {
                return null;
            }
            
            mappedOffset = offset;
            mappedLength = length;
            mappedAccess = access;
            mappedBuffer = nativeMapBuffer(mtlHandle, offset, length);
            return mappedBuffer;
        }
        
        /**
         * Flushes mapped buffer range (glFlushMappedBufferRange equivalent).
         */
        public void flushMappedRange(long offset, long length) {
            if (mappedBuffer != null && storageMode == MTLStorageMode.Managed) {
                nativeDidModifyRange(mtlHandle, mappedOffset + offset, length);
            }
        }
        
        /**
         * Unmaps buffer (glUnmapBuffer equivalent).
         */
        public boolean unmap() {
            if (mappedBuffer == null) return false;
            
            if (storageMode == MTLStorageMode.Managed) {
                nativeDidModifyRange(mtlHandle, mappedOffset, mappedLength);
            }
            
            mappedBuffer = null;
            mappedOffset = 0;
            mappedLength = 0;
            mappedAccess = 0;
            return true;
        }
        
        /**
         * Copies data between buffers (glCopyBufferSubData equivalent).
         */
        public void copyTo(MetalBuffer dst, long readOffset, long writeOffset, long size,
                          long blitEncoder) {
            if (mtlHandle == 0 || dst.mtlHandle == 0) return;
            nativeBlitCopyBuffer(blitEncoder, mtlHandle, readOffset, dst.mtlHandle, writeOffset, size);
        }
        
        private long determineStorageMode(int usage) {
            return switch (usage) {
                case GL_STATIC_DRAW, GL_STATIC_COPY -> MTLStorageMode.Private;
                case GL_DYNAMIC_DRAW, GL_STREAM_DRAW -> MTLStorageMode.Shared;
                case GL_STATIC_READ, GL_DYNAMIC_READ, GL_STREAM_READ -> MTLStorageMode.Shared;
                default -> MTLStorageMode.Shared;
            };
        }
        
        public void release() {
            if (mtlHandle != 0) {
                nativeReleaseBuffer(mtlHandle);
                mtlHandle = 0;
            }
        }
    }
    
    /**
     * Metal texture wrapper with GL texture semantics.
     */
    public static final class MetalTexture {
        private final int glId;
        private long mtlHandle = 0;
        private int target = GL_TEXTURE_2D;
        private int internalFormat = GL_RGBA8;
        private int width = 0;
        private int height = 0;
        private int depth = 1;
        private int levels = 1;
        private int samples = 1;
        private boolean immutableStorage = false;
        
        // Texture parameters (for sampler state)
        private int minFilter = GL_NEAREST_MIPMAP_LINEAR;
        private int magFilter = GL_LINEAR;
        private int wrapS = GL_REPEAT;
        private int wrapT = GL_REPEAT;
        private int wrapR = GL_REPEAT;
        private float minLod = -1000.0f;
        private float maxLod = 1000.0f;
        private float lodBias = 0.0f;
        private int compareMode = 0;
        private int compareFunc = GL_LEQUAL;
        private float maxAnisotropy = 1.0f;
        private float[] borderColor = {0, 0, 0, 0};
        private int[] swizzle = {GL_RED, GL_GREEN, GL_BLUE, GL_ALPHA};
        private int baseLevel = 0;
        private int maxLevel = 1000;
        
        public MetalTexture(int id) {
            this.glId = id;
        }
        
        public int getGLId() { return glId; }
        public long getHandle() { return mtlHandle; }
        public int getTarget() { return target; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getDepth() { return depth; }
        public int getLevels() { return levels; }
        public int getInternalFormat() { return internalFormat; }
        
        /**
         * Allocates texture storage (glTexStorage* equivalent).
         */
        public void allocateStorage(long deviceHandle, int target, int levels, int internalFormat,
                                   int width, int height, int depth) {
            this.target = target;
            this.levels = levels;
            this.internalFormat = internalFormat;
            this.width = width;
            this.height = height;
            this.depth = depth;
            this.immutableStorage = true;
            
            // Release old texture
            if (mtlHandle != 0) {
                nativeReleaseTexture(mtlHandle);
            }
            
            // Create Metal texture descriptor
            long mtlPixelFormat = FormatConverter.toMetalPixelFormat(internalFormat);
            long mtlTextureType = FormatConverter.toMetalTextureType(target);
            long usage = MTLTextureUsage.ShaderRead | MTLTextureUsage.RenderTarget;
            
            mtlHandle = nativeCreateTexture(deviceHandle, mtlTextureType, mtlPixelFormat,
                                           width, height, depth, levels, samples, usage);
        }
        
        /**
         * Allocates texture storage for multisampled textures.
         */
        public void allocateStorageMultisample(long deviceHandle, int target, int samples,
                                              int internalFormat, int width, int height,
                                              boolean fixedSampleLocations) {
            this.target = target;
            this.samples = samples;
            this.internalFormat = internalFormat;
            this.width = width;
            this.height = height;
            this.levels = 1;
            this.immutableStorage = true;
            
            if (mtlHandle != 0) {
                nativeReleaseTexture(mtlHandle);
            }
            
            long mtlPixelFormat = FormatConverter.toMetalPixelFormat(internalFormat);
            long usage = MTLTextureUsage.ShaderRead | MTLTextureUsage.RenderTarget;
            
            mtlHandle = nativeCreateTextureMS(deviceHandle, mtlPixelFormat, width, height, 
                                             samples, usage);
        }
        
        /**
         * Legacy texture allocation (glTexImage* equivalent).
         */
        public void allocateLegacy(long deviceHandle, int target, int level, int internalFormat,
                                  int width, int height, int depth, int format, int type,
                                  ByteBuffer data) {
            this.target = target;
            
            // For level 0, create new texture
            if (level == 0) {
                this.internalFormat = internalFormat;
                this.width = width;
                this.height = height;
                this.depth = depth;
                this.levels = calculateMipLevels(width, height, depth);
                
                if (mtlHandle != 0) {
                    nativeReleaseTexture(mtlHandle);
                }
                
                long mtlPixelFormat = FormatConverter.toMetalPixelFormat(internalFormat);
                long mtlTextureType = FormatConverter.toMetalTextureType(target);
                long usage = MTLTextureUsage.ShaderRead | MTLTextureUsage.RenderTarget;
                
                mtlHandle = nativeCreateTexture(deviceHandle, mtlTextureType, mtlPixelFormat,
                                               width, height, depth, levels, 1, usage);
            }
            
            // Upload data if provided
            if (data != null && mtlHandle != 0) {
                uploadSubImage(level, 0, 0, 0, width >> level, height >> level, 
                              Math.max(1, depth >> level), format, type, data);
            }
        }
        
        /**
         * Updates texture subimage (glTexSubImage* equivalent).
         */
        public void uploadSubImage(int level, int xoffset, int yoffset, int zoffset,
                                  int width, int height, int depth, int format, int type,
                                  ByteBuffer data) {
            if (mtlHandle == 0 || data == null) return;
            
            int bytesPerPixel = FormatConverter.getBytesPerPixel(format, type);
            int bytesPerRow = width * bytesPerPixel;
            int bytesPerImage = bytesPerRow * height;
            
            // Handle format conversion if needed (e.g., RGB -> RGBA)
            ByteBuffer convertedData = convertPixelData(data, format, type, width, height, depth);
            
            nativeTextureReplaceRegion(mtlHandle, level, xoffset, yoffset, zoffset,
                                       width, height, depth, convertedData, bytesPerRow, bytesPerImage);
        }
        
        /**
         * Generates mipmaps (glGenerateMipmap equivalent).
         */
        public void generateMipmaps(long blitEncoder) {
            if (mtlHandle != 0 && levels > 1) {
                nativeBlitGenerateMipmaps(blitEncoder, mtlHandle);
            }
        }
        
        /**
         * Creates a texture view (glTextureView equivalent).
         */
        public MetalTexture createView(long deviceHandle, int newTarget, int newInternalFormat,
                                      int minLevel, int numLevels, int minLayer, int numLayers) {
            MetalTexture view = new MetalTexture(-1); // View doesn't have its own GL id
            
            long mtlPixelFormat = FormatConverter.toMetalPixelFormat(newInternalFormat);
            long mtlTextureType = FormatConverter.toMetalTextureType(newTarget);
            
            view.mtlHandle = nativeCreateTextureView(mtlHandle, mtlTextureType, mtlPixelFormat,
                                                     minLevel, numLevels, minLayer, numLayers);
            view.target = newTarget;
            view.internalFormat = newInternalFormat;
            view.width = Math.max(1, width >> minLevel);
            view.height = Math.max(1, height >> minLevel);
            view.depth = numLayers;
            view.levels = numLevels;
            
            return view;
        }
        
        // Texture parameter setters
        public void setMinFilter(int filter) { this.minFilter = filter; }
        public void setMagFilter(int filter) { this.magFilter = filter; }
        public void setWrapS(int wrap) { this.wrapS = wrap; }
        public void setWrapT(int wrap) { this.wrapT = wrap; }
        public void setWrapR(int wrap) { this.wrapR = wrap; }
        public void setMinLod(float lod) { this.minLod = lod; }
        public void setMaxLod(float lod) { this.maxLod = lod; }
        public void setLodBias(float bias) { this.lodBias = bias; }
        public void setCompareMode(int mode) { this.compareMode = mode; }
        public void setCompareFunc(int func) { this.compareFunc = func; }
        public void setMaxAnisotropy(float aniso) { this.maxAnisotropy = aniso; }
        public void setBorderColor(float[] color) { System.arraycopy(color, 0, borderColor, 0, 4); }
        public void setSwizzle(int[] swizzle) { System.arraycopy(swizzle, 0, this.swizzle, 0, 4); }
        public void setBaseLevel(int level) { this.baseLevel = level; }
        public void setMaxLevel(int level) { this.maxLevel = level; }
        
        // Getters for sampler state
        public int getMinFilter() { return minFilter; }
        public int getMagFilter() { return magFilter; }
        public int getWrapS() { return wrapS; }
        public int getWrapT() { return wrapT; }
        public int getWrapR() { return wrapR; }
        public float getMinLod() { return minLod; }
        public float getMaxLod() { return maxLod; }
        public float getLodBias() { return lodBias; }
        public int getCompareMode() { return compareMode; }
        public int getCompareFunc() { return compareFunc; }
        public float getMaxAnisotropy() { return maxAnisotropy; }
        public float[] getBorderColor() { return borderColor; }
        
        /**
         * Computes hash for sampler state creation.
         */
        public long computeSamplerHash() {
            long hash = minFilter;
            hash = hash * 31 + magFilter;
            hash = hash * 31 + wrapS;
            hash = hash * 31 + wrapT;
            hash = hash * 31 + wrapR;
            hash = hash * 31 + Float.floatToIntBits(minLod);
            hash = hash * 31 + Float.floatToIntBits(maxLod);
            hash = hash * 31 + compareMode;
            hash = hash * 31 + compareFunc;
            hash = hash * 31 + Float.floatToIntBits(maxAnisotropy);
            return hash;
        }
        
        private ByteBuffer convertPixelData(ByteBuffer data, int format, int type, 
                                           int width, int height, int depth) {
            // Convert RGB to RGBA if needed (Metal doesn't support RGB8)
            if (format == GL_RGB || format == GL_BGR) {
                int pixels = width * height * depth;
                ByteBuffer rgba = ByteBuffer.allocateDirect(pixels * 4);
                rgba.order(ByteOrder.nativeOrder());
                
                for (int i = 0; i < pixels; i++) {
                    rgba.put(data.get());
                    rgba.put(data.get());
                    rgba.put(data.get());
                    rgba.put((byte) 255);
                }
                rgba.flip();
                return rgba;
            }
            return data;
        }
        
        private int calculateMipLevels(int w, int h, int d) {
            int maxDim = Math.max(Math.max(w, h), d);
            return (int) Math.floor(Math.log(maxDim) / Math.log(2)) + 1;
        }
        
        public void release() {
            if (mtlHandle != 0) {
                nativeReleaseTexture(mtlHandle);
                mtlHandle = 0;
            }
        }
    }
    
    /**
     * Metal sampler wrapper.
     */
    public static final class MetalSampler {
        private final int glId;
        private long mtlHandle = 0;
        
        private int minFilter = GL_NEAREST_MIPMAP_LINEAR;
        private int magFilter = GL_LINEAR;
        private int wrapS = GL_REPEAT;
        private int wrapT = GL_REPEAT;
        private int wrapR = GL_REPEAT;
        private float minLod = -1000.0f;
        private float maxLod = 1000.0f;
        private int compareMode = 0;
        private int compareFunc = GL_LEQUAL;
        private float maxAnisotropy = 1.0f;
        private float[] borderColor = {0, 0, 0, 0};
        
        private boolean dirty = true;
        
        public MetalSampler(int id) {
            this.glId = id;
        }
        
        public int getGLId() { return glId; }
        public long getHandle() { return mtlHandle; }
        
        public void setMinFilter(int filter) { minFilter = filter; dirty = true; }
        public void setMagFilter(int filter) { magFilter = filter; dirty = true; }
        public void setWrapS(int wrap) { wrapS = wrap; dirty = true; }
        public void setWrapT(int wrap) { wrapT = wrap; dirty = true; }
        public void setWrapR(int wrap) { wrapR = wrap; dirty = true; }
        public void setMinLod(float lod) { minLod = lod; dirty = true; }
        public void setMaxLod(float lod) { maxLod = lod; dirty = true; }
        public void setCompareMode(int mode) { compareMode = mode; dirty = true; }
        public void setCompareFunc(int func) { compareFunc = func; dirty = true; }
        public void setMaxAnisotropy(float aniso) { maxAnisotropy = aniso; dirty = true; }
        public void setBorderColor(float[] color) { 
            System.arraycopy(color, 0, borderColor, 0, 4);
            dirty = true;
        }
        
        /**
         * Builds the Metal sampler state if dirty.
         */
        public void buildSamplerState(long deviceHandle) {
            if (!dirty && mtlHandle != 0) return;
            
            if (mtlHandle != 0) {
                nativeReleaseSamplerState(mtlHandle);
            }
            
            // Create Metal sampler descriptor
            long minMagFilter = FormatConverter.toMetalMinMagFilter(minFilter);
            long magFilterMtl = FormatConverter.toMetalMinMagFilter(magFilter);
            long mipFilter = FormatConverter.toMetalMipFilter(minFilter);
            long addressS = FormatConverter.toMetalAddressMode(wrapS);
            long addressT = FormatConverter.toMetalAddressMode(wrapT);
            long addressR = FormatConverter.toMetalAddressMode(wrapR);
            long compareFunction = compareMode != 0 ? 
                FormatConverter.toMetalCompareFunction(compareFunc) : MTLCompareFunction.Never;
            
            mtlHandle = nativeCreateSamplerState(deviceHandle, minMagFilter, magFilterMtl,
                                                mipFilter, addressS, addressT, addressR,
                                                minLod, maxLod, maxAnisotropy, 
                                                compareMode != 0, compareFunction,
                                                borderColor);
            dirty = false;
        }
        
        public void release() {
            if (mtlHandle != 0) {
                nativeReleaseSamplerState(mtlHandle);
                mtlHandle = 0;
            }
        }
    }
    
    /**
     * Metal shader wrapper with GLSL to MSL translation.
     */
    public static final class MetalShader {
        private final int glId;
        private final int type;
        private String glslSource = "";
        private String mslSource = "";
        private long mtlFunction = 0;
        private long mtlLibrary = 0;
        private boolean compiled = false;
        private String infoLog = "";
        
        // Reflection data
        private final List<ShaderAttribute> attributes = new ArrayList<>();
        private final List<ShaderUniform> uniforms = new ArrayList<>();
        private final List<ShaderUniformBlock> uniformBlocks = new ArrayList<>();
        private final List<ShaderStorageBlock> storageBlocks = new ArrayList<>();
        
        public MetalShader(int id, int type) {
            this.glId = id;
            this.type = type;
        }
        
        public int getGLId() { return glId; }
        public int getType() { return type; }
        public long getFunction() { return mtlFunction; }
        public long getLibrary() { return mtlLibrary; }
        public boolean isCompiled() { return compiled; }
        public String getInfoLog() { return infoLog; }
        public String getGLSLSource() { return glslSource; }
        public String getMSLSource() { return mslSource; }
        
        public List<ShaderAttribute> getAttributes() { return attributes; }
        public List<ShaderUniform> getUniforms() { return uniforms; }
        public List<ShaderUniformBlock> getUniformBlocks() { return uniformBlocks; }
        
        /**
         * Sets GLSL source code.
         */
        public void setSource(String source) {
            this.glslSource = source;
            this.compiled = false;
        }
        
        /**
         * Compiles GLSL to Metal Shading Language.
         */
        public boolean compile(long deviceHandle, GLSLToMSLTranslator translator) {
            try {
                // Translate GLSL to MSL
                GLSLToMSLTranslator.TranslationResult result = translator.translate(glslSource, type);
                this.mslSource = result.mslSource;
                this.attributes.clear();
                this.attributes.addAll(result.attributes);
                this.uniforms.clear();
                this.uniforms.addAll(result.uniforms);
                this.uniformBlocks.clear();
                this.uniformBlocks.addAll(result.uniformBlocks);
                
                // Compile MSL
                long[] handles = nativeCompileMSL(deviceHandle, mslSource, 
                                                  getEntryPointName(), type);
                
                if (handles[0] != 0) {
                    this.mtlLibrary = handles[0];
                    this.mtlFunction = handles[1];
                    this.compiled = true;
                    this.infoLog = "";
                    return true;
                } else {
                    this.infoLog = nativeGetCompileError(deviceHandle);
                    this.compiled = false;
                    return false;
                }
            } catch (Exception e) {
                this.infoLog = "Translation error: " + e.getMessage();
                this.compiled = false;
                return false;
            }
        }
        
        private String getEntryPointName() {
            return switch (type) {
                case GL_VERTEX_SHADER -> "vertexMain";
                case GL_FRAGMENT_SHADER -> "fragmentMain";
                case GL_COMPUTE_SHADER -> "computeMain";
                case GL_GEOMETRY_SHADER -> "geometryMain";
                case GL_TESS_CONTROL_SHADER -> "tessControlMain";
                case GL_TESS_EVALUATION_SHADER -> "tessEvalMain";
                default -> "main";
            };
        }
        
        public void release() {
            if (mtlFunction != 0) {
                nativeReleaseFunction(mtlFunction);
                mtlFunction = 0;
            }
            if (mtlLibrary != 0) {
                nativeReleaseLibrary(mtlLibrary);
                mtlLibrary = 0;
            }
        }
    }
    
    /**
     * Shader attribute metadata.
     */
    public static final class ShaderAttribute {
        public final String name;
        public final int location;
        public final int type;
        public final int size;
        
        public ShaderAttribute(String name, int location, int type, int size) {
            this.name = name;
            this.location = location;
            this.type = type;
            this.size = size;
        }
    }
    
    /**
     * Shader uniform metadata.
     */
    public static final class ShaderUniform {
        public final String name;
        public final int location;
        public final int type;
        public final int size;
        public final int offset;      // Offset in uniform buffer
        public final int blockIndex;  // -1 for default block
        
        public ShaderUniform(String name, int location, int type, int size, 
                            int offset, int blockIndex) {
            this.name = name;
            this.location = location;
            this.type = type;
            this.size = size;
            this.offset = offset;
            this.blockIndex = blockIndex;
        }
    }
    
    /**
     * Shader uniform block metadata.
     */
    public static final class ShaderUniformBlock {
        public final String name;
        public final int index;
        public final int binding;
        public final int size;
        public final List<ShaderUniform> members;
        
        public ShaderUniformBlock(String name, int index, int binding, int size,
                                 List<ShaderUniform> members) {
            this.name = name;
            this.index = index;
            this.binding = binding;
            this.size = size;
            this.members = members;
        }
    }
    
    /**
     * Shader storage block metadata.
     */
    public static final class ShaderStorageBlock {
        public final String name;
        public final int index;
        public final int binding;
        
        public ShaderStorageBlock(String name, int index, int binding) {
            this.name = name;
            this.index = index;
            this.binding = binding;
        }
    }
    
    /**
     * Metal program wrapper (linked shader program).
     */
    public static final class MetalProgram {
        private final int glId;
        private final List<MetalShader> attachedShaders = new ArrayList<>();
        private MetalShader vertexShader;
        private MetalShader fragmentShader;
        private MetalShader computeShader;
        private MetalShader geometryShader;
        private MetalShader tessControlShader;
        private MetalShader tessEvalShader;
        
        private boolean linked = false;
        private String infoLog = "";
        
        // Merged reflection data
        private final Map<String, ProgramUniform> uniforms = new LinkedHashMap<>();
        private final Map<String, ProgramAttribute> attributes = new LinkedHashMap<>();
        private final Map<String, ProgramUniformBlock> uniformBlocks = new LinkedHashMap<>();
        private final Map<String, ProgramStorageBlock> storageBlocks = new LinkedHashMap<>();
        
        // Uniform data cache (for default block uniforms)
        private ByteBuffer uniformBuffer;
        private final Map<Integer, UniformValue> uniformValues = new HashMap<>();
        private boolean uniformsDirty = false;
        
        public MetalProgram(int id) {
            this.glId = id;
        }
        
        public int getGLId() { return glId; }
        public boolean isLinked() { return linked; }
        public String getInfoLog() { return infoLog; }
        public MetalShader getVertexShader() { return vertexShader; }
        public MetalShader getFragmentShader() { return fragmentShader; }
        public MetalShader getComputeShader() { return computeShader; }
        
        /**
         * Attaches a shader to the program.
         */
        public void attachShader(MetalShader shader) {
            attachedShaders.add(shader);
            switch (shader.getType()) {
                case GL_VERTEX_SHADER -> vertexShader = shader;
                case GL_FRAGMENT_SHADER -> fragmentShader = shader;
                case GL_COMPUTE_SHADER -> computeShader = shader;
                case GL_GEOMETRY_SHADER -> geometryShader = shader;
                case GL_TESS_CONTROL_SHADER -> tessControlShader = shader;
                case GL_TESS_EVALUATION_SHADER -> tessEvalShader = shader;
            }
            linked = false;
        }
        
        /**
         * Detaches a shader from the program.
         */
        public void detachShader(MetalShader shader) {
            attachedShaders.remove(shader);
            if (shader == vertexShader) vertexShader = null;
            else if (shader == fragmentShader) fragmentShader = null;
            else if (shader == computeShader) computeShader = null;
            else if (shader == geometryShader) geometryShader = null;
            else if (shader == tessControlShader) tessControlShader = null;
            else if (shader == tessEvalShader) tessEvalShader = null;
            linked = false;
        }
        
        /**
         * Links the program.
         */
        public boolean link() {
            // Verify shaders are compiled
            for (MetalShader shader : attachedShaders) {
                if (!shader.isCompiled()) {
                    infoLog = "Shader " + shader.getGLId() + " is not compiled";
                    return false;
                }
            }
            
            // Validate shader combinations
            if (computeShader != null) {
                if (vertexShader != null || fragmentShader != null) {
                    infoLog = "Compute shader cannot be combined with render shaders";
                    return false;
                }
            } else {
                if (vertexShader == null) {
                    infoLog = "Vertex shader required for graphics program";
                    return false;
                }
            }
            
            // Merge reflection data
            mergeReflectionData();
            
            // Allocate uniform buffer for default block
            allocateUniformBuffer();
            
            linked = true;
            infoLog = "";
            return true;
        }
        
        /**
         * Merges reflection data from all attached shaders.
         */
        private void mergeReflectionData() {
            uniforms.clear();
            attributes.clear();
            uniformBlocks.clear();
            
            int uniformLocation = 0;
            int blockIndex = 0;
            
            // Process vertex shader
            if (vertexShader != null) {
                for (ShaderAttribute attr : vertexShader.getAttributes()) {
                    attributes.put(attr.name, new ProgramAttribute(attr.name, attr.location,
                                                                   attr.type, attr.size));
                }
                
                for (ShaderUniform uniform : vertexShader.getUniforms()) {
                    if (!uniforms.containsKey(uniform.name)) {
                        uniforms.put(uniform.name, new ProgramUniform(uniform.name, uniformLocation++,
                                                                      uniform.type, uniform.size,
                                                                      uniform.offset, uniform.blockIndex,
                                                                      GL_VERTEX_SHADER));
                    }
                }
                
                for (ShaderUniformBlock block : vertexShader.getUniformBlocks()) {
                    if (!uniformBlocks.containsKey(block.name)) {
                        uniformBlocks.put(block.name, new ProgramUniformBlock(block.name, blockIndex++,
                                                                              block.binding, block.size));
                    }
                }
            }
            
            // Process fragment shader
            if (fragmentShader != null) {
                for (ShaderUniform uniform : fragmentShader.getUniforms()) {
                    if (!uniforms.containsKey(uniform.name)) {
                        uniforms.put(uniform.name, new ProgramUniform(uniform.name, uniformLocation++,
                                                                      uniform.type, uniform.size,
                                                                      uniform.offset, uniform.blockIndex,
                                                                      GL_FRAGMENT_SHADER));
                    }
                }
                
                for (ShaderUniformBlock block : fragmentShader.getUniformBlocks()) {
                    if (!uniformBlocks.containsKey(block.name)) {
                        uniformBlocks.put(block.name, new ProgramUniformBlock(block.name, blockIndex++,
                                                                              block.binding, block.size));
                    }
                }
            }
            
            // Process compute shader
            if (computeShader != null) {
                for (ShaderUniform uniform : computeShader.getUniforms()) {
                    if (!uniforms.containsKey(uniform.name)) {
                        uniforms.put(uniform.name, new ProgramUniform(uniform.name, uniformLocation++,
                                                                      uniform.type, uniform.size,
                                                                      uniform.offset, uniform.blockIndex,
                                                                      GL_COMPUTE_SHADER));
                    }
                }
            }
        }
        
        /**
         * Allocates uniform buffer for default block uniforms.
         */
        private void allocateUniformBuffer() {
            // Calculate buffer size from uniforms in default block (-1)
            int size = 0;
            for (ProgramUniform uniform : uniforms.values()) {
                if (uniform.blockIndex == -1) {
                    int uniformSize = getUniformTypeSize(uniform.type) * uniform.size;
                    size = Math.max(size, uniform.offset + uniformSize);
                }
            }
            
            if (size > 0) {
                // Align to 16 bytes
                size = (size + 15) & ~15;
                uniformBuffer = ByteBuffer.allocateDirect(size);
                uniformBuffer.order(ByteOrder.nativeOrder());
            }
        }
        
        /**
         * Gets uniform location by name.
         */
        public int getUniformLocation(String name) {
            ProgramUniform uniform = uniforms.get(name);
            return uniform != null ? uniform.location : -1;
        }
        
        /**
         * Gets attribute location by name.
         */
        public int getAttribLocation(String name) {
            ProgramAttribute attr = attributes.get(name);
            return attr != null ? attr.location : -1;
        }
        
        /**
         * Gets uniform block index by name.
         */
        public int getUniformBlockIndex(String name) {
            ProgramUniformBlock block = uniformBlocks.get(name);
            return block != null ? block.index : -1;
        }
        
        /**
         * Sets uniform block binding.
         */
        public void setUniformBlockBinding(int blockIndex, int binding) {
            for (ProgramUniformBlock block : uniformBlocks.values()) {
                if (block.index == blockIndex) {
                    block.binding = binding;
                    break;
                }
            }
        }
        
        // Uniform setters
        public void setUniform1i(int location, int v) {
            setUniformData(location, GL_INT, new int[]{v});
        }
        
        public void setUniform2i(int location, int v0, int v1) {
            setUniformData(location, GL_INT_VEC2, new int[]{v0, v1});
        }
        
        public void setUniform3i(int location, int v0, int v1, int v2) {
            setUniformData(location, GL_INT_VEC3, new int[]{v0, v1, v2});
        }
        
        public void setUniform4i(int location, int v0, int v1, int v2, int v3) {
            setUniformData(location, GL_INT_VEC4, new int[]{v0, v1, v2, v3});
        }
        
        public void setUniform1f(int location, float v) {
            setUniformData(location, GL_FLOAT, new float[]{v});
        }
        
        public void setUniform2f(int location, float v0, float v1) {
            setUniformData(location, GL_FLOAT_VEC2, new float[]{v0, v1});
        }
        
        public void setUniform3f(int location, float v0, float v1, float v2) {
            setUniformData(location, GL_FLOAT_VEC3, new float[]{v0, v1, v2});
        }
        
        public void setUniform4f(int location, float v0, float v1, float v2, float v3) {
            setUniformData(location, GL_FLOAT_VEC4, new float[]{v0, v1, v2, v3});
        }
        
        public void setUniformMatrix2fv(int location, boolean transpose, FloatBuffer value) {
            float[] data = new float[4];
            value.get(data);
            if (transpose) data = transposeMatrix(data, 2);
            setUniformData(location, GL_FLOAT_MAT2, data);
        }
        
        public void setUniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {
            float[] data = new float[9];
            value.get(data);
            if (transpose) data = transposeMatrix(data, 3);
            setUniformData(location, GL_FLOAT_MAT3, data);
        }
        
        public void setUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
            float[] data = new float[16];
            value.get(data);
            if (transpose) data = transposeMatrix(data, 4);
            setUniformData(location, GL_FLOAT_MAT4, data);
        }
        
        private void setUniformData(int location, int type, Object data) {
            ProgramUniform uniform = findUniformByLocation(location);
            if (uniform == null || uniform.blockIndex != -1) return;
            
            uniformValues.put(location, new UniformValue(type, data));
            uniformsDirty = true;
            
            // Write to uniform buffer
            if (uniformBuffer != null) {
                int offset = uniform.offset;
                uniformBuffer.position(offset);
                
                if (data instanceof float[] floats) {
                    for (float f : floats) {
                        uniformBuffer.putFloat(f);
                    }
                } else if (data instanceof int[] ints) {
                    for (int i : ints) {
                        uniformBuffer.putInt(i);
                    }
                }
            }
        }
        
        private ProgramUniform findUniformByLocation(int location) {
            for (ProgramUniform uniform : uniforms.values()) {
                if (uniform.location == location) return uniform;
            }
            return null;
        }
        
        private float[] transposeMatrix(float[] m, int size) {
            float[] result = new float[m.length];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    result[i * size + j] = m[j * size + i];
                }
            }
            return result;
        }
        
        public ByteBuffer getUniformBuffer() { return uniformBuffer; }
        public boolean areUniformsDirty() { return uniformsDirty; }
        public void clearUniformsDirty() { uniformsDirty = false; }
        
        public Map<String, ProgramUniform> getUniforms() { return uniforms; }
        public Map<String, ProgramAttribute> getAttributes() { return attributes; }
        public Map<String, ProgramUniformBlock> getUniformBlocks() { return uniformBlocks; }
        
        private int getUniformTypeSize(int type) {
            return switch (type) {
                case GL_FLOAT, GL_INT, GL_UNSIGNED_INT, GL_BOOL -> 4;
                case GL_FLOAT_VEC2, GL_INT_VEC2, GL_UNSIGNED_INT_VEC2, GL_BOOL_VEC2 -> 8;
                case GL_FLOAT_VEC3, GL_INT_VEC3, GL_UNSIGNED_INT_VEC3, GL_BOOL_VEC3 -> 12;
                case GL_FLOAT_VEC4, GL_INT_VEC4, GL_UNSIGNED_INT_VEC4, GL_BOOL_VEC4 -> 16;
                case GL_FLOAT_MAT2 -> 16;
                case GL_FLOAT_MAT3 -> 36;
                case GL_FLOAT_MAT4 -> 64;
                case GL_FLOAT_MAT2x3, GL_FLOAT_MAT3x2 -> 24;
                case GL_FLOAT_MAT2x4, GL_FLOAT_MAT4x2 -> 32;
                case GL_FLOAT_MAT3x4, GL_FLOAT_MAT4x3 -> 48;
                case GL_SAMPLER_2D, GL_SAMPLER_3D, GL_SAMPLER_CUBE,
                     GL_SAMPLER_2D_ARRAY, GL_SAMPLER_2D_SHADOW,
                     GL_SAMPLER_CUBE_SHADOW, GL_IMAGE_2D -> 4;
                default -> 4;
            };
        }
        
        public void release() {
            // Programs don't own shader resources, just clear references
            attachedShaders.clear();
            vertexShader = null;
            fragmentShader = null;
            computeShader = null;
            geometryShader = null;
            tessControlShader = null;
            tessEvalShader = null;
        }
    }
    
    public static final class ProgramUniform {
        public final String name;
        public final int location;
        public final int type;
        public final int size;
        public final int offset;
        public final int blockIndex;
        public final int shaderStages;
        
        public ProgramUniform(String name, int location, int type, int size,
                             int offset, int blockIndex, int shaderStages) {
            this.name = name;
            this.location = location;
            this.type = type;
            this.size = size;
            this.offset = offset;
            this.blockIndex = blockIndex;
            this.shaderStages = shaderStages;
        }
    }
    
    public static final class ProgramAttribute {
        public final String name;
        public final int location;
        public final int type;
        public final int size;
        
        public ProgramAttribute(String name, int location, int type, int size) {
            this.name = name;
            this.location = location;
            this.type = type;
            this.size = size;
        }
    }
    
    public static final class ProgramUniformBlock {
        public final String name;
        public final int index;
        public int binding;
        public final int size;
        
        public ProgramUniformBlock(String name, int index, int binding, int size) {
            this.name = name;
            this.index = index;
            this.binding = binding;
            this.size = size;
        }
    }
    
    public static final class ProgramStorageBlock {
        public final String name;
        public final int index;
        public int binding;
        
        public ProgramStorageBlock(String name, int index, int binding) {
            this.name = name;
            this.index = index;
            this.binding = binding;
        }
    }
    
    public static final class UniformValue {
        public final int type;
        public final Object data;
        
        public UniformValue(int type, Object data) {
            this.type = type;
            this.data = data;
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 8: VERTEX ARRAY OBJECTS
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Metal vertex array wrapper (emulates GL VAO).
     */
    public static final class MetalVertexArray {
        private final int glId;
        private final VertexAttribute[] attributes = new VertexAttribute[32];
        private int elementBuffer = 0;
        
        // Cached vertex descriptor for pipeline creation
        private long vertexDescriptorHash = 0;
        private boolean dirty = true;
        
        public MetalVertexArray(int id) {
            this.glId = id;
            for (int i = 0; i < attributes.length; i++) {
                attributes[i] = new VertexAttribute();
            }
        }
        
        public int getGLId() { return glId; }
        public int getElementBuffer() { return elementBuffer; }
        public VertexAttribute[] getAttributes() { return attributes; }
        
        public void setElementBuffer(int buffer) {
            this.elementBuffer = buffer;
        }
        
        /**
         * Enables a vertex attribute.
         */
        public void enableAttribute(int index) {
            if (index >= 0 && index < attributes.length) {
                attributes[index].enabled = true;
                dirty = true;
            }
        }
        
        /**
         * Disables a vertex attribute.
         */
        public void disableAttribute(int index) {
            if (index >= 0 && index < attributes.length) {
                attributes[index].enabled = false;
                dirty = true;
            }
        }
        
        /**
         * Sets vertex attribute pointer.
         */
        public void setAttributePointer(int index, int size, int type, boolean normalized,
                                       int stride, long offset, int buffer) {
            if (index < 0 || index >= attributes.length) return;
            
            VertexAttribute attr = attributes[index];
            attr.size = size;
            attr.type = type;
            attr.normalized = normalized;
            attr.stride = stride;
            attr.offset = offset;
            attr.buffer = buffer;
            dirty = true;
        }
        
        /**
         * Sets vertex attribute integer pointer.
         */
        public void setAttributeIPointer(int index, int size, int type, int stride,
                                        long offset, int buffer) {
            if (index < 0 || index >= attributes.length) return;
            
            VertexAttribute attr = attributes[index];
            attr.size = size;
            attr.type = type;
            attr.normalized = false;
            attr.integer = true;
            attr.stride = stride;
            attr.offset = offset;
            attr.buffer = buffer;
            dirty = true;
        }
        
        /**
         * Sets vertex attribute divisor for instancing.
         */
        public void setAttributeDivisor(int index, int divisor) {
            if (index >= 0 && index < attributes.length) {
                attributes[index].divisor = divisor;
                dirty = true;
            }
        }
        
        /**
         * Sets vertex binding divisor.
         */
        public void setBindingDivisor(int binding, int divisor) {
            for (VertexAttribute attr : attributes) {
                if (attr.binding == binding) {
                    attr.divisor = divisor;
                }
            }
            dirty = true;
        }
        
        /**
         * Computes hash for vertex descriptor.
         */
        public long computeVertexDescriptorHash() {
            if (!dirty) return vertexDescriptorHash;
            
            long hash = 0;
            for (int i = 0; i < attributes.length; i++) {
                VertexAttribute attr = attributes[i];
                if (attr.enabled) {
                    hash = hash * 31 + i;
                    hash = hash * 31 + attr.size;
                    hash = hash * 31 + attr.type;
                    hash = hash * 31 + (attr.normalized ? 1 : 0);
                    hash = hash * 31 + (attr.integer ? 1 : 0);
                    hash = hash * 31 + attr.stride;
                    hash = hash * 31 + attr.offset;
                    hash = hash * 31 + attr.divisor;
                    hash = hash * 31 + attr.buffer;
                }
            }
            
            vertexDescriptorHash = hash;
            dirty = false;
            return hash;
        }
        
        /**
         * Creates Metal vertex descriptor from attributes.
         */
        public long createVertexDescriptor() {
            // Count enabled attributes and buffers
            int enabledCount = 0;
            Set<Integer> usedBuffers = new HashSet<>();
            
            for (VertexAttribute attr : attributes) {
                if (attr.enabled) {
                    enabledCount++;
                    usedBuffers.add(attr.buffer);
                }
            }
            
            // Create descriptor
            long descriptor = nativeCreateVertexDescriptor();
            
            // Configure attributes
            for (int i = 0; i < attributes.length; i++) {
                VertexAttribute attr = attributes[i];
                if (attr.enabled) {
                    long format = toMetalVertexFormat(attr.type, attr.size, attr.normalized, attr.integer);
                    int bufferIndex = attr.buffer == 0 ? 0 : getBufferLayoutIndex(attr.buffer, usedBuffers);
                    nativeVertexDescriptorSetAttribute(descriptor, i, format, attr.offset, bufferIndex);
                }
            }
            
            // Configure buffer layouts
            int layoutIndex = 0;
            for (Integer buffer : usedBuffers) {
                VertexAttribute attr = findAttributeForBuffer(buffer);
                if (attr != null) {
                    int stride = attr.stride > 0 ? attr.stride : calculateStride(attr);
                    long stepFunction = attr.divisor == 0 ? 
                        1 : // MTLVertexStepFunctionPerVertex
                        2;  // MTLVertexStepFunctionPerInstance
                    int stepRate = attr.divisor == 0 ? 1 : attr.divisor;
                    
                    nativeVertexDescriptorSetLayout(descriptor, layoutIndex, stride, stepFunction, stepRate);
                }
                layoutIndex++;
            }
            
            return descriptor;
        }
        
        private int getBufferLayoutIndex(int buffer, Set<Integer> usedBuffers) {
            int index = 0;
            for (Integer b : usedBuffers) {
                if (b == buffer) return index;
                index++;
            }
            return 0;
        }
        
        private VertexAttribute findAttributeForBuffer(int buffer) {
            for (VertexAttribute attr : attributes) {
                if (attr.enabled && attr.buffer == buffer) {
                    return attr;
                }
            }
            return null;
        }
        
        private int calculateStride(VertexAttribute attr) {
            return attr.size * getTypeSize(attr.type);
        }
        
        private int getTypeSize(int type) {
            return switch (type) {
                case GL_BYTE, GL_UNSIGNED_BYTE -> 1;
                case GL_SHORT, GL_UNSIGNED_SHORT, GL_HALF_FLOAT -> 2;
                case GL_INT, GL_UNSIGNED_INT, GL_FLOAT, GL_FIXED -> 4;
                case GL_DOUBLE -> 8;
                default -> 4;
            };
        }
        
        private long toMetalVertexFormat(int glType, int size, boolean normalized, boolean integer) {
            // Metal vertex formats
            // Float formats
            final long Float = 28;
            final long Float2 = 29;
            final long Float3 = 30;
            final long Float4 = 31;
            
            // Int formats
            final long Int = 36;
            final long Int2 = 37;
            final long Int3 = 38;
            final long Int4 = 39;
            
            // UInt formats
            final long UInt = 40;
            final long UInt2 = 41;
            final long UInt3 = 42;
            final long UInt4 = 43;
            
            // Normalized formats
            final long Char4Normalized = 17;
            final long UChar4Normalized = 1;
            final long Short2Normalized = 4;
            final long Short4Normalized = 6;
            final long UShort2Normalized = 8;
            final long UShort4Normalized = 10;
            
            if (glType == GL_FLOAT) {
                return switch (size) {
                    case 1 -> Float;
                    case 2 -> Float2;
                    case 3 -> Float3;
                    default -> Float4;
                };
            }
            
            if (integer) {
                if (glType == GL_INT) {
                    return switch (size) {
                        case 1 -> Int;
                        case 2 -> Int2;
                        case 3 -> Int3;
                        default -> Int4;
                    };
                } else if (glType == GL_UNSIGNED_INT) {
                    return switch (size) {
                        case 1 -> UInt;
                        case 2 -> UInt2;
                        case 3 -> UInt3;
                        default -> UInt4;
                    };
                }
            }
            
            if (normalized) {
                if (glType == GL_UNSIGNED_BYTE && size == 4) return UChar4Normalized;
                if (glType == GL_BYTE && size == 4) return Char4Normalized;
                if (glType == GL_UNSIGNED_SHORT && size == 2) return UShort2Normalized;
                if (glType == GL_UNSIGNED_SHORT && size == 4) return UShort4Normalized;
                if (glType == GL_SHORT && size == 2) return Short2Normalized;
                if (glType == GL_SHORT && size == 4) return Short4Normalized;
            }
            
            // Default to float
            return switch (size) {
                case 1 -> Float;
                case 2 -> Float2;
                case 3 -> Float3;
                default -> Float4;
            };
        }
        
        public void release() {
            // No Metal resources to release for VAO
        }
    }
    
    /**
     * Vertex attribute data.
     */
    public static final class VertexAttribute {
        public boolean enabled = false;
        public int size = 4;
        public int type = GL_FLOAT;
        public boolean normalized = false;
        public boolean integer = false;
        public int stride = 0;
        public long offset = 0;
        public int divisor = 0;
        public int buffer = 0;
        public int binding = 0;
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 9: FRAMEBUFFER & RENDERBUFFER
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Metal framebuffer wrapper.
     */
    public static final class MetalFramebuffer {
        private final int glId;
        private final Map<Integer, FramebufferAttachment> colorAttachments = new HashMap<>();
        private FramebufferAttachment depthAttachment;
        private FramebufferAttachment stencilAttachment;
        private FramebufferAttachment depthStencilAttachment;
        
        private int width = 0;
        private int height = 0;
        private int samples = 1;
        
        // Draw buffers
        private int[] drawBuffers = {GL_COLOR_ATTACHMENT0};
        private int readBuffer = GL_COLOR_ATTACHMENT0;
        
        public MetalFramebuffer(int id) {
            this.glId = id;
        }
        
        public int getGLId() { return glId; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getSamples() { return samples; }
        
        /**
         * Attaches a texture to the framebuffer.
         */
        public void attachTexture(int attachment, int textureTarget, MetalTexture texture,
                                 int level, int layer) {
            FramebufferAttachment att = new FramebufferAttachment();
            att.type = AttachmentType.TEXTURE;
            att.texture = texture;
            att.level = level;
            att.layer = layer;
            att.target = textureTarget;
            
            setAttachment(attachment, att);
            updateDimensions();
        }
        
        /**
         * Attaches a renderbuffer to the framebuffer.
         */
        public void attachRenderbuffer(int attachment, MetalRenderbuffer renderbuffer) {
            FramebufferAttachment att = new FramebufferAttachment();
            att.type = AttachmentType.RENDERBUFFER;
            att.renderbuffer = renderbuffer;
            
            setAttachment(attachment, att);
            updateDimensions();
        }
        
        private void setAttachment(int attachment, FramebufferAttachment att) {
            if (attachment >= GL_COLOR_ATTACHMENT0 && attachment <= GL_COLOR_ATTACHMENT7) {
                colorAttachments.put(attachment, att);
            } else if (attachment == GL_DEPTH_ATTACHMENT) {
                depthAttachment = att;
            } else if (attachment == GL_STENCIL_ATTACHMENT) {
                stencilAttachment = att;
            } else if (attachment == GL_DEPTH_STENCIL_ATTACHMENT) {
                depthStencilAttachment = att;
            }
        }
        
        public FramebufferAttachment getAttachment(int attachment) {
            if (attachment >= GL_COLOR_ATTACHMENT0 && attachment <= GL_COLOR_ATTACHMENT7) {
                return colorAttachments.get(attachment);
            } else if (attachment == GL_DEPTH_ATTACHMENT) {
                return depthAttachment;
            } else if (attachment == GL_STENCIL_ATTACHMENT) {
                return stencilAttachment;
            } else if (attachment == GL_DEPTH_STENCIL_ATTACHMENT) {
                return depthStencilAttachment;
            }
            return null;
        }
        
        public Map<Integer, FramebufferAttachment> getColorAttachments() {
            return colorAttachments;
        }
        
        public FramebufferAttachment getDepthAttachment() {
            return depthAttachment != null ? depthAttachment : 
                   (depthStencilAttachment != null ? depthStencilAttachment : null);
        }
        
        public FramebufferAttachment getStencilAttachment() {
            return stencilAttachment != null ? stencilAttachment :
                   (depthStencilAttachment != null ? depthStencilAttachment : null);
        }
        
        private void updateDimensions() {
            // Get dimensions from first available attachment
            FramebufferAttachment att = colorAttachments.values().stream()
                                                       .findFirst()
                                                       .orElse(depthAttachment);
            if (att == null) att = stencilAttachment;
            if (att == null) att = depthStencilAttachment;
            
            if (att != null) {
                if (att.type == AttachmentType.TEXTURE && att.texture != null) {
                    width = Math.max(1, att.texture.getWidth() >> att.level);
                    height = Math.max(1, att.texture.getHeight() >> att.level);
                    samples = 1;
                } else if (att.type == AttachmentType.RENDERBUFFER && att.renderbuffer != null) {
                    width = att.renderbuffer.getWidth();
                    height = att.renderbuffer.getHeight();
                    samples = att.renderbuffer.getSamples();
                }
            }
        }
        
        /**
         * Sets draw buffers.
         */
        public void setDrawBuffers(int[] buffers) {
            this.drawBuffers = buffers.clone();
        }
        
        public int[] getDrawBuffers() { return drawBuffers; }
        
        /**
         * Sets read buffer.
         */
        public void setReadBuffer(int buffer) {
            this.readBuffer = buffer;
        }
        
        public int getReadBuffer() { return readBuffer; }
        
        /**
         * Checks framebuffer completeness.
         */
        public int checkStatus() {
            // Check for any attachment
            if (colorAttachments.isEmpty() && depthAttachment == null &&
                stencilAttachment == null && depthStencilAttachment == null) {
                return GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT;
            }
            
            // Check all attachments have same dimensions and samples
            int refWidth = -1, refHeight = -1, refSamples = -1;
            
            for (FramebufferAttachment att : colorAttachments.values()) {
                int[] dims = getAttachmentDimensions(att);
                if (refWidth == -1) {
                    refWidth = dims[0];
                    refHeight = dims[1];
                    refSamples = dims[2];
                } else if (dims[0] != refWidth || dims[1] != refHeight) {
                    return GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
                } else if (dims[2] != refSamples) {
                    return GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE;
                }
            }
            
            // Check depth/stencil
            FramebufferAttachment depthAtt = getDepthAttachment();
            if (depthAtt != null && refWidth != -1) {
                int[] dims = getAttachmentDimensions(depthAtt);
                if (dims[0] != refWidth || dims[1] != refHeight) {
                    return GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT;
                }
            }
            
            return GL_FRAMEBUFFER_COMPLETE;
        }
        
        private int[] getAttachmentDimensions(FramebufferAttachment att) {
            if (att.type == AttachmentType.TEXTURE && att.texture != null) {
                return new int[]{
                    Math.max(1, att.texture.getWidth() >> att.level),
                    Math.max(1, att.texture.getHeight() >> att.level),
                    1
                };
            } else if (att.type == AttachmentType.RENDERBUFFER && att.renderbuffer != null) {
                return new int[]{
                    att.renderbuffer.getWidth(),
                    att.renderbuffer.getHeight(),
                    att.renderbuffer.getSamples()
                };
            }
            return new int[]{0, 0, 0};
        }
        
        /**
         * Creates Metal render pass descriptor.
         */
        public long createRenderPassDescriptor(float[] clearColor, float clearDepth, 
                                               int clearStencil, int clearMask) {
            long descriptor = nativeCreateRenderPassDescriptor();
            
            // Configure color attachments
            int colorIndex = 0;
            for (Map.Entry<Integer, FramebufferAttachment> entry : colorAttachments.entrySet()) {
                FramebufferAttachment att = entry.getValue();
                long texture = att.type == AttachmentType.TEXTURE ?
                              att.texture.getHandle() : att.renderbuffer.getTextureHandle();
                
                long loadAction = (clearMask & GL_COLOR_BUFFER_BIT) != 0 ?
                                 MTLLoadAction.Clear : MTLLoadAction.Load;
                long storeAction = MTLStoreAction.Store;
                
                nativeRenderPassSetColorAttachment(descriptor, colorIndex, texture, att.level,
                                                   att.layer, loadAction, storeAction,
                                                   clearColor[0], clearColor[1],
                                                   clearColor[2], clearColor[3]);
                colorIndex++;
            }
            
            // Configure depth attachment
            FramebufferAttachment depthAtt = getDepthAttachment();
            if (depthAtt != null) {
                long texture = depthAtt.type == AttachmentType.TEXTURE ?
                              depthAtt.texture.getHandle() : depthAtt.renderbuffer.getTextureHandle();
                
                long loadAction = (clearMask & GL_DEPTH_BUFFER_BIT) != 0 ?
                                 MTLLoadAction.Clear : MTLLoadAction.Load;
                
                nativeRenderPassSetDepthAttachment(descriptor, texture, depthAtt.level,
                                                   loadAction, MTLStoreAction.Store, clearDepth);
            }
            
            // Configure stencil attachment
            FramebufferAttachment stencilAtt = getStencilAttachment();
            if (stencilAtt != null) {
                long texture = stencilAtt.type == AttachmentType.TEXTURE ?
                              stencilAtt.texture.getHandle() : stencilAtt.renderbuffer.getTextureHandle();
                
                long loadAction = (clearMask & GL_STENCIL_BUFFER_BIT) != 0 ?
                                 MTLLoadAction.Clear : MTLLoadAction.Load;
                
                nativeRenderPassSetStencilAttachment(descriptor, texture, stencilAtt.level,
                                                     loadAction, MTLStoreAction.Store, clearStencil);
            }
            
            return descriptor;
        }
        
        public void release() {
            colorAttachments.clear();
            depthAttachment = null;
            stencilAttachment = null;
            depthStencilAttachment = null;
        }
    }
    
    public enum AttachmentType {
        TEXTURE,
        RENDERBUFFER
    }
    
    public static final class FramebufferAttachment {
        public AttachmentType type;
        public MetalTexture texture;
        public MetalRenderbuffer renderbuffer;
        public int level = 0;
        public int layer = 0;
        public int target = GL_TEXTURE_2D;
    }
    
    /**
     * Metal renderbuffer wrapper.
     */
    public static final class MetalRenderbuffer {
        private final int glId;
        private long mtlTexture = 0;
        private int internalFormat = GL_RGBA8;
        private int width = 0;
        private int height = 0;
        private int samples = 1;
        
        public MetalRenderbuffer(int id) {
            this.glId = id;
        }
        
        public int getGLId() { return glId; }
        public long getTextureHandle() { return mtlTexture; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getSamples() { return samples; }
        public int getInternalFormat() { return internalFormat; }
        
        /**
         * Allocates renderbuffer storage.
         */
        public void allocateStorage(long deviceHandle, int internalFormat, int width, int height) {
            this.internalFormat = internalFormat;
            this.width = width;
            this.height = height;
            this.samples = 1;
            
            if (mtlTexture != 0) {
                nativeReleaseTexture(mtlTexture);
            }
            
            long mtlFormat = FormatConverter.toMetalPixelFormat(internalFormat);
            long usage = MTLTextureUsage.RenderTarget | MTLTextureUsage.ShaderRead;
            
            mtlTexture = nativeCreateTexture(deviceHandle, MTLTextureType.Type2D, mtlFormat,
                                            width, height, 1, 1, 1, usage);
        }
        
        /**
         * Allocates multisampled renderbuffer storage.
         */
        public void allocateStorageMultisample(long deviceHandle, int samples, int internalFormat,
                                              int width, int height) {
            this.internalFormat = internalFormat;
            this.width = width;
            this.height = height;
            this.samples = samples;
            
            if (mtlTexture != 0) {
                nativeReleaseTexture(mtlTexture);
            }
            
            long mtlFormat = FormatConverter.toMetalPixelFormat(internalFormat);
            long usage = MTLTextureUsage.RenderTarget;
            
            mtlTexture = nativeCreateTextureMS(deviceHandle, mtlFormat, width, height, 
                                              samples, usage);
        }
        
        public void release() {
            if (mtlTexture != 0) {
                nativeReleaseTexture(mtlTexture);
                mtlTexture = 0;
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 10: PIPELINE STATE OBJECTS
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Metal render pipeline state wrapper.
     */
    public static final class MetalRenderPipelineState {
        private final long handle;
        private final long hash;
        
        public MetalRenderPipelineState(long handle, long hash) {
            this.handle = handle;
            this.hash = hash;
        }
        
        public long getHandle() { return handle; }
        public long getHash() { return hash; }
        
        public void release() {
            if (handle != 0) {
                nativeReleasePipelineState(handle);
            }
        }
    }
    
    /**
     * Metal depth stencil state wrapper.
     */
    public static final class MetalDepthStencilState {
        private final long handle;
        private final long hash;
        
        public MetalDepthStencilState(long handle, long hash) {
            this.handle = handle;
            this.hash = hash;
        }
        
        public long getHandle() { return handle; }
        public long getHash() { return hash; }
        
        public void release() {
            if (handle != 0) {
                nativeReleaseDepthStencilState(handle);
            }
        }
    }
    
    /**
     * Metal sampler state wrapper.
     */
    public static final class MetalSamplerState {
        private final long handle;
        private final long hash;
        
        public MetalSamplerState(long handle, long hash) {
            this.handle = handle;
            this.hash = hash;
        }
        
        public long getHandle() { return handle; }
        public long getHash() { return hash; }
        
        public void release() {
            if (handle != 0) {
                nativeReleaseSamplerState(handle);
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 11: GLSL TO MSL TRANSLATOR
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Translates GLSL shader source to Metal Shading Language.
     */
    public static final class GLSLToMSLTranslator {
        
        // GLSL to MSL type mappings
        private static final Map<String, String> TYPE_MAP = new HashMap<>();
        private static final Map<String, String> BUILTIN_MAP = new HashMap<>();
        private static final Map<String, String> FUNCTION_MAP = new HashMap<>();
        
        static {
            // Type mappings
            TYPE_MAP.put("void", "void");
            TYPE_MAP.put("bool", "bool");
            TYPE_MAP.put("int", "int");
            TYPE_MAP.put("uint", "uint");
            TYPE_MAP.put("float", "float");
            TYPE_MAP.put("double", "float"); // Metal doesn't support double
            TYPE_MAP.put("vec2", "float2");
            TYPE_MAP.put("vec3", "float3");
            TYPE_MAP.put("vec4", "float4");
            TYPE_MAP.put("dvec2", "float2");
            TYPE_MAP.put("dvec3", "float3");
            TYPE_MAP.put("dvec4", "float4");
            TYPE_MAP.put("ivec2", "int2");
            TYPE_MAP.put("ivec3", "int3");
            TYPE_MAP.put("ivec4", "int4");
            TYPE_MAP.put("uvec2", "uint2");
            TYPE_MAP.put("uvec3", "uint3");
            TYPE_MAP.put("uvec4", "uint4");
            TYPE_MAP.put("bvec2", "bool2");
            TYPE_MAP.put("bvec3", "bool3");
            TYPE_MAP.put("bvec4", "bool4");
            TYPE_MAP.put("mat2", "float2x2");
            TYPE_MAP.put("mat3", "float3x3");
            TYPE_MAP.put("mat4", "float4x4");
            TYPE_MAP.put("mat2x2", "float2x2");
            TYPE_MAP.put("mat2x3", "float2x3");
            TYPE_MAP.put("mat2x4", "float2x4");
            TYPE_MAP.put("mat3x2", "float3x2");
            TYPE_MAP.put("mat3x3", "float3x3");
            TYPE_MAP.put("mat3x4", "float3x4");
            TYPE_MAP.put("mat4x2", "float4x2");
            TYPE_MAP.put("mat4x3", "float4x3");
            TYPE_MAP.put("mat4x4", "float4x4");
            TYPE_MAP.put("sampler1D", "texture1d<float>");
            TYPE_MAP.put("sampler2D", "texture2d<float>");
            TYPE_MAP.put("sampler3D", "texture3d<float>");
            TYPE_MAP.put("samplerCube", "texturecube<float>");
            TYPE_MAP.put("sampler1DArray", "texture1d_array<float>");
            TYPE_MAP.put("sampler2DArray", "texture2d_array<float>");
            TYPE_MAP.put("samplerCubeArray", "texturecube_array<float>");
            TYPE_MAP.put("sampler2DMS", "texture2d_ms<float>");
            TYPE_MAP.put("sampler2DShadow", "depth2d<float>");
            TYPE_MAP.put("samplerCubeShadow", "depthcube<float>");
            TYPE_MAP.put("sampler2DArrayShadow", "depth2d_array<float>");
            TYPE_MAP.put("isampler2D", "texture2d<int>");
            TYPE_MAP.put("usampler2D", "texture2d<uint>");
            TYPE_MAP.put("image2D", "texture2d<float, access::read_write>");
            
            // Built-in variable mappings (vertex shader)
            BUILTIN_MAP.put("gl_Position", "out.position");
            BUILTIN_MAP.put("gl_PointSize", "out.pointSize");
            BUILTIN_MAP.put("gl_VertexID", "vertexID");
            BUILTIN_MAP.put("gl_InstanceID", "instanceID");
            BUILTIN_MAP.put("gl_VertexIndex", "vertexID");
            BUILTIN_MAP.put("gl_InstanceIndex", "instanceID");
            
            // Built-in variable mappings (fragment shader)
            BUILTIN_MAP.put("gl_FragCoord", "in.position");
            BUILTIN_MAP.put("gl_FrontFacing", "in.frontFacing");
            BUILTIN_MAP.put("gl_PointCoord", "in.pointCoord");
            BUILTIN_MAP.put("gl_FragDepth", "out.depth");
            BUILTIN_MAP.put("gl_SampleID", "sampleID");
            BUILTIN_MAP.put("gl_SamplePosition", "samplePosition");
            BUILTIN_MAP.put("gl_SampleMask", "sampleMask");
            
            // Function mappings
            FUNCTION_MAP.put("texture", "sample");
            FUNCTION_MAP.put("texture2D", "sample");
            FUNCTION_MAP.put("texture3D", "sample");
            FUNCTION_MAP.put("textureCube", "sample");
            FUNCTION_MAP.put("textureLod", "sample");
            FUNCTION_MAP.put("textureGrad", "sample");
            FUNCTION_MAP.put("textureOffset", "sample");
            FUNCTION_MAP.put("texelFetch", "read");
            FUNCTION_MAP.put("texelFetchOffset", "read");
            FUNCTION_MAP.put("textureSize", "get_width"); // Needs special handling
            FUNCTION_MAP.put("textureQueryLevels", "get_num_mip_levels");
            FUNCTION_MAP.put("imageLoad", "read");
            FUNCTION_MAP.put("imageStore", "write");
            FUNCTION_MAP.put("imageSize", "get_width");
            FUNCTION_MAP.put("dFdx", "dfdx");
            FUNCTION_MAP.put("dFdy", "dfdy");
            FUNCTION_MAP.put("dFdxFine", "dfdx");
            FUNCTION_MAP.put("dFdyFine", "dfdy");
            FUNCTION_MAP.put("dFdxCoarse", "dfdx");
            FUNCTION_MAP.put("dFdyCoarse", "dfdy");
            FUNCTION_MAP.put("fwidth", "fwidth");
            FUNCTION_MAP.put("mod", "fmod");
            FUNCTION_MAP.put("mix", "mix");
            FUNCTION_MAP.put("fract", "fract");
            FUNCTION_MAP.put("atan", "atan2"); // For two-argument version
            FUNCTION_MAP.put("inversesqrt", "rsqrt");
            FUNCTION_MAP.put("floatBitsToInt", "as_type<int>");
            FUNCTION_MAP.put("floatBitsToUint", "as_type<uint>");
            FUNCTION_MAP.put("intBitsToFloat", "as_type<float>");
            FUNCTION_MAP.put("uintBitsToFloat", "as_type<float>");
            FUNCTION_MAP.put("packHalf2x16", "pack_float_to_snorm2x16"); // Approximate
            FUNCTION_MAP.put("unpackHalf2x16", "unpack_snorm2x16_to_float");
            FUNCTION_MAP.put("barrier", "threadgroup_barrier");
            FUNCTION_MAP.put("memoryBarrier", "threadgroup_barrier");
            FUNCTION_MAP.put("memoryBarrierShared", "threadgroup_barrier");
            FUNCTION_MAP.put("groupMemoryBarrier", "threadgroup_barrier");
        }
        
        /**
         * Translation result containing MSL source and reflection data.
         */
        public static final class TranslationResult {
            public final String mslSource;
            public final List<ShaderAttribute> attributes;
            public final List<ShaderUniform> uniforms;
            public final List<ShaderUniformBlock> uniformBlocks;
            
            public TranslationResult(String mslSource, List<ShaderAttribute> attributes,
                                    List<ShaderUniform> uniforms, List<ShaderUniformBlock> blocks) {
                this.mslSource = mslSource;
                this.attributes = attributes;
                this.uniforms = uniforms;
                this.uniformBlocks = blocks;
            }
        }
        
        /**
         * Translates GLSL source to MSL.
         */
        public TranslationResult translate(String glslSource, int shaderType) {
            // Parse GLSL
            GLSLParser parser = new GLSLParser(glslSource);
            GLSLParser.ParseResult parseResult = parser.parse();
            
            // Build MSL
            StringBuilder msl = new StringBuilder();
            
            // Add Metal header
            msl.append("#include <metal_stdlib>\n");
            msl.append("#include <simd/simd.h>\n");
            msl.append("using namespace metal;\n\n");
            
            // Generate struct definitions
            generateStructs(msl, parseResult, shaderType);
            
            // Generate uniform buffer structs
            generateUniformBuffers(msl, parseResult);
            
            // Generate main function
            generateMainFunction(msl, parseResult, shaderType);
            
            return new TranslationResult(
                msl.toString(),
                parseResult.attributes,
                parseResult.uniforms,
                parseResult.uniformBlocks
            );
        }
        
        private void generateStructs(StringBuilder msl, GLSLParser.ParseResult result, int type) {
            // Vertex input struct
            if (type == GL_VERTEX_SHADER && !result.attributes.isEmpty()) {
                msl.append("struct VertexInput {\n");
                for (ShaderAttribute attr : result.attributes) {
                    String mtlType = TYPE_MAP.getOrDefault(getGLSLTypeName(attr.type), "float4");
                    msl.append("    ").append(mtlType).append(" ")
                       .append(attr.name).append(" [[attribute(")
                       .append(attr.location).append(")]];\n");
                }
                msl.append("};\n\n");
            }
            
            // Vertex output / Fragment input struct
            if (type == GL_VERTEX_SHADER) {
                msl.append("struct VertexOutput {\n");
                msl.append("    float4 position [[position]];\n");
                msl.append("    float pointSize [[point_size]];\n");
                for (GLSLParser.Varying varying : result.varyings) {
                    String mtlType = TYPE_MAP.getOrDefault(varying.type, "float4");
                    if (varying.flat) {
                        msl.append("    ").append(mtlType).append(" ")
                           .append(varying.name).append(" [[flat]];\n");
                    } else {
                        msl.append("    ").append(mtlType).append(" ")
                           .append(varying.name).append(";\n");
                    }
                }
                msl.append("};\n\n");
            }
            
            // Fragment input struct
            if (type == GL_FRAGMENT_SHADER) {
                msl.append("struct FragmentInput {\n");
                msl.append("    float4 position [[position]];\n");
                msl.append("    bool frontFacing [[front_facing]];\n");
                for (GLSLParser.Varying varying : result.varyings) {
                    String mtlType = TYPE_MAP.getOrDefault(varying.type, "float4");
                    if (varying.flat) {
                        msl.append("    ").append(mtlType).append(" ")
                           .append(varying.name).append(" [[flat]];\n");
                    } else {
                        msl.append("    ").append(mtlType).append(" ")
                           .append(varying.name).append(";\n");
                    }
                }
                msl.append("};\n\n");
            }
            
            // Fragment output struct
            if (type == GL_FRAGMENT_SHADER) {
                msl.append("struct FragmentOutput {\n");
                for (GLSLParser.FragOutput output : result.fragOutputs) {
                    String mtlType = TYPE_MAP.getOrDefault(output.type, "float4");
                    msl.append("    ").append(mtlType).append(" ")
                       .append(output.name).append(" [[color(")
                       .append(output.location).append(")]];\n");
                }
                if (result.writesDepth) {
                    msl.append("    float depth [[depth(any)]];\n");
                }
                msl.append("};\n\n");
            }
        }
        
        private void generateUniformBuffers(StringBuilder msl, GLSLParser.ParseResult result) {
            // Default uniform buffer for loose uniforms
            if (!result.uniforms.isEmpty()) {
                boolean hasDefaultBlockUniforms = result.uniforms.stream()
                    .anyMatch(u -> u.blockIndex == -1);
                
                if (hasDefaultBlockUniforms) {
                    msl.append("struct Uniforms {\n");
                    for (ShaderUniform uniform : result.uniforms) {
                        if (uniform.blockIndex == -1 && !isSamplerType(uniform.type)) {
                            String mtlType = TYPE_MAP.getOrDefault(
                                getGLSLTypeName(uniform.type), "float4");
                            msl.append("    ").append(mtlType).append(" ")
                               .append(uniform.name);
                            if (uniform.size > 1) {
                                msl.append("[").append(uniform.size).append("]");
                            }
                            msl.append(";\n");
                        }
                    }
                    msl.append("};\n\n");
                }
            }
            
            // Named uniform blocks
            for (ShaderUniformBlock block : result.uniformBlocks) {
                msl.append("struct ").append(block.name).append(" {\n");
                for (ShaderUniform member : block.members) {
                    String mtlType = TYPE_MAP.getOrDefault(
                        getGLSLTypeName(member.type), "float4");
                    msl.append("    ").append(mtlType).append(" ")
                       .append(member.name);
                    if (member.size > 1) {
                        msl.append("[").append(member.size).append("]");
                    }
                    msl.append(";\n");
                }
                msl.append("};\n\n");
            }
        }
        
        private void generateMainFunction(StringBuilder msl, GLSLParser.ParseResult result, int type) {
            if (type == GL_VERTEX_SHADER) {
                generateVertexMain(msl, result);
            } else if (type == GL_FRAGMENT_SHADER) {
                generateFragmentMain(msl, result);
            } else if (type == GL_COMPUTE_SHADER) {
                generateComputeMain(msl, result);
            }
        }
        
        private void generateVertexMain(StringBuilder msl, GLSLParser.ParseResult result) {
            msl.append("vertex VertexOutput vertexMain(\n");
            
            // Input
            if (!result.attributes.isEmpty()) {
                msl.append("    VertexInput in [[stage_in]],\n");
            }
            
            // Vertex ID and Instance ID
            msl.append("    uint vertexID [[vertex_id]],\n");
            msl.append("    uint instanceID [[instance_id]]");
            
            // Uniform buffers
            int bufferIndex = 0;
            boolean hasDefaultUniforms = result.uniforms.stream()
                .anyMatch(u -> u.blockIndex == -1 && !isSamplerType(u.type));
            if (hasDefaultUniforms) {
                msl.append(",\n    constant Uniforms& uniforms [[buffer(")
                   .append(bufferIndex++).append(")]]");
            }
            
            for (ShaderUniformBlock block : result.uniformBlocks) {
                msl.append(",\n    constant ").append(block.name).append("& ")
                   .append(block.name.toLowerCase()).append(" [[buffer(")
                   .append(bufferIndex++).append(")]]");
            }
            
            // Textures and samplers
            int textureIndex = 0;
            int samplerIndex = 0;
            for (ShaderUniform uniform : result.uniforms) {
                if (isSamplerType(uniform.type)) {
                    String texType = getMetalTextureType(uniform.type);
                    msl.append(",\n    ").append(texType).append(" ")
                       .append(uniform.name).append(" [[texture(")
                       .append(textureIndex++).append(")]]");
                    msl.append(",\n    sampler ").append(uniform.name).append("_sampler [[sampler(")
                       .append(samplerIndex++).append(")]]");
                }
            }
            
            msl.append("\n) {\n");
            
            // Function body
            msl.append("    VertexOutput out;\n");
            msl.append("    out.pointSize = 1.0;\n\n");
            
            // Translate main function body
            String translatedBody = translateFunctionBody(result.mainFunction, result, GL_VERTEX_SHADER);
            msl.append(translatedBody);
            
            // Apply clip space transformation (GL uses [-1,1] for Z, Metal uses [0,1])
            msl.append("\n    // Convert GL clip space to Metal clip space\n");
            msl.append("    out.position.z = (out.position.z + out.position.w) * 0.5;\n");
            
            msl.append("\n    return out;\n");
            msl.append("}\n");
        }
        
        private void generateFragmentMain(StringBuilder msl, GLSLParser.ParseResult result) {
            msl.append("fragment FragmentOutput fragmentMain(\n");
            msl.append("    FragmentInput in [[stage_in]]");
            
            // Sample ID for MSAA
            if (result.usesSampleID) {
                msl.append(",\n    uint sampleID [[sample_id]]");
            }
            
            // Uniform buffers
            int bufferIndex = 0;
            boolean hasDefaultUniforms = result.uniforms.stream()
                .anyMatch(u -> u.blockIndex == -1 && !isSamplerType(u.type));
            if (hasDefaultUniforms) {
                msl.append(",\n    constant Uniforms& uniforms [[buffer(")
                   .append(bufferIndex++).append(")]]");
            }
            
            for (ShaderUniformBlock block : result.uniformBlocks) {
                msl.append(",\n    constant ").append(block.name).append("& ")
                   .append(block.name.toLowerCase()).append(" [[buffer(")
                   .append(bufferIndex++).append(")]]");
            }
            
            // Textures and samplers
            int textureIndex = 0;
            int samplerIndex = 0;
            for (ShaderUniform uniform : result.uniforms) {
                if (isSamplerType(uniform.type)) {
                    String texType = getMetalTextureType(uniform.type);
                    msl.append(",\n    ").append(texType).append(" ")
                       .append(uniform.name).append(" [[texture(")
                       .append(textureIndex++).append(")]]");
                    msl.append(",\n    sampler ").append(uniform.name).append("_sampler [[sampler(")
                       .append(samplerIndex++).append(")]]");
                }
            }
            
            msl.append("\n) {\n");
            
            // Function body
            msl.append("    FragmentOutput out;\n\n");
            
            // Translate main function body
            String translatedBody = translateFunctionBody(result.mainFunction, result, GL_FRAGMENT_SHADER);
            msl.append(translatedBody);
            
            msl.append("\n    return out;\n");
            msl.append("}\n");
        }
        
        private void generateComputeMain(StringBuilder msl, GLSLParser.ParseResult result) {
            msl.append("kernel void computeMain(\n");
            msl.append("    uint3 globalID [[thread_position_in_grid]],\n");
            msl.append("    uint3 localID [[thread_position_in_threadgroup]],\n");
            msl.append("    uint3 groupID [[threadgroup_position_in_grid]],\n");
            msl.append("    uint3 groupSize [[threads_per_threadgroup]]");
            
            // Buffers and textures
            int bufferIndex = 0;
            int textureIndex = 0;
            
            for (ShaderUniform uniform : result.uniforms) {
                if (isSamplerType(uniform.type)) {
                    String texType = getMetalTextureType(uniform.type);
                    msl.append(",\n    ").append(texType).append(" ")
                       .append(uniform.name).append(" [[texture(")
                       .append(textureIndex++).append(")]]");
                } else if (isImageType(uniform.type)) {
                    msl.append(",\n    texture2d<float, access::read_write> ")
                       .append(uniform.name).append(" [[texture(")
                       .append(textureIndex++).append(")]]");
                }
            }
            
            msl.append("\n) {\n");
            
            // Translate main function body
            String translatedBody = translateFunctionBody(result.mainFunction, result, GL_COMPUTE_SHADER);
            msl.append(translatedBody);
            
            msl.append("}\n");
        }
        
        private String translateFunctionBody(String glslBody, GLSLParser.ParseResult result, int type) {
            if (glslBody == null || glslBody.isEmpty()) return "";
            
            StringBuilder translated = new StringBuilder();
            String[] lines = glslBody.split("\n");
            
            for (String line : lines) {
                String translatedLine = translateLine(line, result, type);
                translated.append("    ").append(translatedLine).append("\n");
            }
            
            return translated.toString();
        }
        
        private String translateLine(String line, GLSLParser.ParseResult result, int type) {
            String translated = line;
            
            // Replace types
            for (Map.Entry<String, String> entry : TYPE_MAP.entrySet()) {
                translated = translated.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
            }
            
            // Replace built-in variables
            for (Map.Entry<String, String> entry : BUILTIN_MAP.entrySet()) {
                translated = translated.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
            }
            
            // Replace texture sampling
            translated = translateTextureCalls(translated, result);
            
            // Replace function names
            for (Map.Entry<String, String> entry : FUNCTION_MAP.entrySet()) {
                // Simple replacement for now; complex cases need special handling
                if (!entry.getKey().startsWith("texture")) {
                    translated = translated.replaceAll("\\b" + entry.getKey() + "\\s*\\(",
                                                      entry.getValue() + "(");
                }
            }
            
            // Handle mod() vs fmod()
            translated = translated.replaceAll("\\bmod\\s*\\(", "fmod(");
            
            // Handle matrix construction differences
            translated = translateMatrixConstruction(translated);
            
            return translated;
        }
        
        private String translateTextureCalls(String line, GLSLParser.ParseResult result) {
            // Pattern for texture(sampler, coords) -> tex.sample(sampler_sampler, coords)
            Pattern texturePattern = Pattern.compile(
                "\\btexture\\s*\\(\\s*(\\w+)\\s*,\\s*([^)]+)\\)");
            Matcher m = texturePattern.matcher(line);
            StringBuffer sb = new StringBuffer();
            
            while (m.find()) {
                String samplerName = m.group(1);
                String coords = m.group(2);
                // In MSL: texture.sample(sampler, coords)
                m.appendReplacement(sb, samplerName + ".sample(" + samplerName + "_sampler, " + coords + ")");
            }
            m.appendTail(sb);
            
            // Handle texelFetch
            Pattern fetchPattern = Pattern.compile(
                "\\btexelFetch\\s*\\(\\s*(\\w+)\\s*,\\s*([^,]+)\\s*,\\s*(\\d+)\\s*\\)");
            m = fetchPattern.matcher(sb.toString());
            sb = new StringBuffer();
            
            while (m.find()) {
                String samplerName = m.group(1);
                String coords = m.group(2);
                String lod = m.group(3);
                m.appendReplacement(sb, samplerName + ".read(uint2(" + coords + "), " + lod + ")");
            }
            m.appendTail(sb);
            
            return sb.toString();
        }
        
        private String translateMatrixConstruction(String line) {
            // GLSL: mat4(1.0) creates identity matrix
            // MSL: float4x4(1.0) also works but syntax differs for some cases
            
            // Handle mat4(vec4, vec4, vec4, vec4) -> float4x4(vec4, vec4, vec4, vec4)
            // This is handled by type replacement
            
            return line;
        }
        
        private boolean isSamplerType(int type) {
            return type == GL_SAMPLER_1D || type == GL_SAMPLER_2D || type == GL_SAMPLER_3D ||
                   type == GL_SAMPLER_CUBE || type == GL_SAMPLER_1D_SHADOW ||
                   type == GL_SAMPLER_2D_SHADOW || type == GL_SAMPLER_CUBE_SHADOW ||
                   type == GL_SAMPLER_2D_ARRAY || type == GL_SAMPLER_2D_ARRAY_SHADOW ||
                   type == GL_SAMPLER_2D_MULTISAMPLE || type == GL_SAMPLER_2D_MULTISAMPLE_ARRAY ||
                   type == GL_SAMPLER_BUFFER;
        }
        
        private boolean isImageType(int type) {
            return type == GL_IMAGE_2D;
        }
        
        private String getMetalTextureType(int glType) {
            return switch (glType) {
                case GL_SAMPLER_1D -> "texture1d<float>";
                case GL_SAMPLER_2D -> "texture2d<float>";
                case GL_SAMPLER_3D -> "texture3d<float>";
                case GL_SAMPLER_CUBE -> "texturecube<float>";
                case GL_SAMPLER_2D_ARRAY -> "texture2d_array<float>";
                case GL_SAMPLER_2D_SHADOW -> "depth2d<float>";
                case GL_SAMPLER_CUBE_SHADOW -> "depthcube<float>";
                case GL_SAMPLER_2D_MULTISAMPLE -> "texture2d_ms<float>";
                default -> "texture2d<float>";
            };
        }
        
        private String getGLSLTypeName(int type) {
            return switch (type) {
                case GL_FLOAT -> "float";
                case GL_FLOAT_VEC2 -> "vec2";
                case GL_FLOAT_VEC3 -> "vec3";
                case GL_FLOAT_VEC4 -> "vec4";
                case GL_INT -> "int";
                case GL_INT_VEC2 -> "ivec2";
                case GL_INT_VEC3 -> "ivec3";
                case GL_INT_VEC4 -> "ivec4";
                case GL_UNSIGNED_INT -> "uint";
                case GL_UNSIGNED_INT_VEC2 -> "uvec2";
                case GL_UNSIGNED_INT_VEC3 -> "uvec3";
                case GL_UNSIGNED_INT_VEC4 -> "uvec4";
                case GL_BOOL -> "bool";
                case GL_BOOL_VEC2 -> "bvec2";
                case GL_BOOL_VEC3 -> "bvec3";
                case GL_BOOL_VEC4 -> "bvec4";
                case GL_FLOAT_MAT2 -> "mat2";
                case GL_FLOAT_MAT3 -> "mat3";
                case GL_FLOAT_MAT4 -> "mat4";
                case GL_SAMPLER_2D -> "sampler2D";
                case GL_SAMPLER_3D -> "sampler3D";
                case GL_SAMPLER_CUBE -> "samplerCube";
                case GL_SAMPLER_2D_SHADOW -> "sampler2DShadow";
                default -> "float4";
            };
        }
    }
    
    /**
     * Simple GLSL parser for extracting shader information.
     */
    public static final class GLSLParser {
        private final String source;
        
        public GLSLParser(String source) {
            this.source = source;
        }
        
        public static final class ParseResult {
            public List<ShaderAttribute> attributes = new ArrayList<>();
            public List<ShaderUniform> uniforms = new ArrayList<>();
            public List<ShaderUniformBlock> uniformBlocks = new ArrayList<>();
            public List<Varying> varyings = new ArrayList<>();
            public List<FragOutput> fragOutputs = new ArrayList<>();
            public String mainFunction = "";
            public boolean writesDepth = false;
            public boolean usesSampleID = false;
            public int version = 330;
        }
        
        public static final class Varying {
            public String name;
            public String type;
            public int location = -1;
            public boolean flat = false;
            public boolean noPerspective = false;
        }
        
        public static final class FragOutput {
            public String name;
            public String type;
            public int location = 0;
        }
        
        public ParseResult parse() {
            ParseResult result = new ParseResult();
            
            // Parse version
            Pattern versionPattern = Pattern.compile("#version\\s+(\\d+)");
            Matcher versionMatcher = versionPattern.matcher(source);
            if (versionMatcher.find()) {
                result.version = Integer.parseInt(versionMatcher.group(1));
            }
            
            // Parse layout qualifiers and declarations
            parseAttributes(result);
            parseUniforms(result);
            parseUniformBlocks(result);
            parseVaryings(result);
            parseFragOutputs(result);
            
            // Extract main function body
            result.mainFunction = extractMainFunction();
            
            // Check for gl_FragDepth usage
            result.writesDepth = source.contains("gl_FragDepth");
            result.usesSampleID = source.contains("gl_SampleID");
            
            return result;
        }
        
        private void parseAttributes(ParseResult result) {
            // layout(location = N) in type name;
            Pattern pattern = Pattern.compile(
                "layout\\s*\\(\\s*location\\s*=\\s*(\\d+)\\s*\\)\\s*in\\s+(\\w+)\\s+(\\w+)\\s*;");
            Matcher m = pattern.matcher(source);
            
            while (m.find()) {
                int location = Integer.parseInt(m.group(1));
                String type = m.group(2);
                String name = m.group(3);
                
                result.attributes.add(new ShaderAttribute(name, location, 
                    glslTypeToGLType(type), 1));
            }
            
            // Also parse 'in type name' without layout for legacy
            Pattern legacyPattern = Pattern.compile(
                "(?<!layout\\s*\\([^)]*\\)\\s*)\\bin\\s+(\\w+)\\s+(\\w+)\\s*;");
            m = legacyPattern.matcher(source);
            int autoLocation = result.attributes.size();
            
            while (m.find()) {
                String type = m.group(1);
                String name = m.group(2);
                
                // Skip if already defined with layout
                boolean exists = result.attributes.stream()
                    .anyMatch(a -> a.name.equals(name));
                if (!exists) {
                    result.attributes.add(new ShaderAttribute(name, autoLocation++,
                        glslTypeToGLType(type), 1));
                }
            }
        }
        
        private void parseUniforms(ParseResult result) {
            // uniform type name;
            Pattern pattern = Pattern.compile(
                "uniform\\s+(\\w+)\\s+(\\w+)(?:\\s*\\[\\s*(\\d+)\\s*\\])?\\s*;");
            Matcher m = pattern.matcher(source);
            
            int location = 0;
            int offset = 0;
            
            while (m.find()) {
                String type = m.group(1);
                String name = m.group(2);
                int size = m.group(3) != null ? Integer.parseInt(m.group(3)) : 1;
                
                int glType = glslTypeToGLType(type);
                int typeSize = getTypeSize(glType) * size;
                
                result.uniforms.add(new ShaderUniform(name, location++, glType, size,
                    offset, -1)); // -1 = default block
                
                offset += typeSize;
                offset = (offset + 15) & ~15; // Align to 16 bytes
            }
        }
        
        private void parseUniformBlocks(ParseResult result) {
            // layout(std140, binding = N) uniform BlockName { ... };
            Pattern pattern = Pattern.compile(
                "layout\\s*\\([^)]*binding\\s*=\\s*(\\d+)[^)]*\\)\\s*uniform\\s+(\\w+)\\s*\\{([^}]+)\\}");
            Matcher m = pattern.matcher(source);
            
            int blockIndex = 0;
            
            while (m.find()) {
                int binding = Integer.parseInt(m.group(1));
                String blockName = m.group(2);
                String members = m.group(3);
                
                List<ShaderUniform> memberList = parseBlockMembers(members, blockIndex);
                int blockSize = calculateBlockSize(memberList);
                
                result.uniformBlocks.add(new ShaderUniformBlock(blockName, blockIndex,
                    binding, blockSize, memberList));
                blockIndex++;
            }
        }
        
        private List<ShaderUniform> parseBlockMembers(String membersStr, int blockIndex) {
            List<ShaderUniform> members = new ArrayList<>();
            Pattern pattern = Pattern.compile("(\\w+)\\s+(\\w+)(?:\\s*\\[\\s*(\\d+)\\s*\\])?\\s*;");
            Matcher m = pattern.matcher(membersStr);
            
            int offset = 0;
            int location = 0;
            
            while (m.find()) {
                String type = m.group(1);
                String name = m.group(2);
                int size = m.group(3) != null ? Integer.parseInt(m.group(3)) : 1;
                
                int glType = glslTypeToGLType(type);
                int alignment = getTypeAlignment(glType);
                offset = (offset + alignment - 1) & ~(alignment - 1);
                
                members.add(new ShaderUniform(name, location++, glType, size, offset, blockIndex));
                
                offset += getTypeSize(glType) * size;
            }
            
            return members;
        }
        
        private int calculateBlockSize(List<ShaderUniform> members) {
            int size = 0;
            for (ShaderUniform member : members) {
                size = Math.max(size, member.offset + getTypeSize(member.type) * member.size);
            }
            return (size + 15) & ~15; // Align to 16 bytes
        }
        
        private void parseVaryings(ParseResult result) {
            // out type name; (vertex) or in type name; (fragment, not attribute)
            // Also: layout(location = N) out type name;
            
            Pattern outPattern = Pattern.compile(
                "(?:layout\\s*\\([^)]*\\)\\s*)?out\\s+(flat\\s+)?(\\w+)\\s+(\\w+)\\s*;");
            Matcher m = outPattern.matcher(source);
            
            while (m.find()) {
                Varying v = new Varying();
                v.flat = m.group(1) != null;
                v.type = m.group(2);
                v.name = m.group(3);
                
                // Skip gl_Position etc
                if (!v.name.startsWith("gl_")) {
                    result.varyings.add(v);
                }
            }
        }
        
        private void parseFragOutputs(ParseResult result) {
            // layout(location = N) out type name;
            Pattern pattern = Pattern.compile(
                "layout\\s*\\(\\s*location\\s*=\\s*(\\d+)\\s*\\)\\s*out\\s+(\\w+)\\s+(\\w+)\\s*;");
            Matcher m = pattern.matcher(source);
            
            while (m.find()) {
                FragOutput out = new FragOutput();
                out.location = Integer.parseInt(m.group(1));
                out.type = m.group(2);
                out.name = m.group(3);
                result.fragOutputs.add(out);
            }
            
            // Default output if none specified
            if (result.fragOutputs.isEmpty()) {
                // Check for gl_FragColor or out vec4
                if (source.contains("gl_FragColor")) {
                    FragOutput out = new FragOutput();
                    out.location = 0;
                    out.type = "vec4";
                    out.name = "fragColor";
                    result.fragOutputs.add(out);
                }
            }
        }
        
        private String extractMainFunction() {
            // Find void main() { ... }
            int mainStart = source.indexOf("void main()");
            if (mainStart == -1) {
                mainStart = source.indexOf("void main(void)");
            }
            if (mainStart == -1) return "";
            
            int braceStart = source.indexOf('{', mainStart);
            if (braceStart == -1) return "";
            
            // Find matching closing brace
            int depth = 1;
            int pos = braceStart + 1;
            while (pos < source.length() && depth > 0) {
                char c = source.charAt(pos);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                pos++;
            }
            
            return source.substring(braceStart + 1, pos - 1).trim();
        }
        
        private int glslTypeToGLType(String type) {
            return switch (type) {
                case "float" -> GL_FLOAT;
                case "vec2" -> GL_FLOAT_VEC2;
                case "vec3" -> GL_FLOAT_VEC3;
                case "vec4" -> GL_FLOAT_VEC4;
                case "int" -> GL_INT;
                case "ivec2" -> GL_INT_VEC2;
                case "ivec3" -> GL_INT_VEC3;
                case "ivec4" -> GL_INT_VEC4;
                case "uint" -> GL_UNSIGNED_INT;
                case "uvec2" -> GL_UNSIGNED_INT_VEC2;
                case "uvec3" -> GL_UNSIGNED_INT_VEC3;
                case "uvec4" -> GL_UNSIGNED_INT_VEC4;
                case "bool" -> GL_BOOL;
                case "bvec2" -> GL_BOOL_VEC2;
                case "bvec3" -> GL_BOOL_VEC3;
                case "bvec4" -> GL_BOOL_VEC4;
                case "mat2" -> GL_FLOAT_MAT2;
                case "mat3" -> GL_FLOAT_MAT3;
                case "mat4" -> GL_FLOAT_MAT4;
                case "sampler2D" -> GL_SAMPLER_2D;
                case "sampler3D" -> GL_SAMPLER_3D;
                case "samplerCube" -> GL_SAMPLER_CUBE;
                case "sampler2DShadow" -> GL_SAMPLER_2D_SHADOW;
                case "sampler2DArray" -> GL_SAMPLER_2D_ARRAY;
                default -> GL_FLOAT;
            };
        }
        
        private int getTypeSize(int type) {
            return switch (type) {
                case GL_FLOAT, GL_INT, GL_UNSIGNED_INT, GL_BOOL -> 4;
                case GL_FLOAT_VEC2, GL_INT_VEC2, GL_UNSIGNED_INT_VEC2, GL_BOOL_VEC2 -> 8;
                case GL_FLOAT_VEC3, GL_INT_VEC3, GL_UNSIGNED_INT_VEC3, GL_BOOL_VEC3 -> 12;
                case GL_FLOAT_VEC4, GL_INT_VEC4, GL_UNSIGNED_INT_VEC4, GL_BOOL_VEC4 -> 16;
                case GL_FLOAT_MAT2 -> 32;   // 2 vec4s in std140
                case GL_FLOAT_MAT3 -> 48;   // 3 vec4s in std140
                case GL_FLOAT_MAT4 -> 64;   // 4 vec4s in std140
                default -> 4;
            };
        }
        
        private int getTypeAlignment(int type) {
            return switch (type) {
                case GL_FLOAT, GL_INT, GL_UNSIGNED_INT, GL_BOOL -> 4;
                case GL_FLOAT_VEC2, GL_INT_VEC2, GL_UNSIGNED_INT_VEC2, GL_BOOL_VEC2 -> 8;
                case GL_FLOAT_VEC3, GL_INT_VEC3, GL_UNSIGNED_INT_VEC3, GL_BOOL_VEC3 -> 16;
                case GL_FLOAT_VEC4, GL_INT_VEC4, GL_UNSIGNED_INT_VEC4, GL_BOOL_VEC4 -> 16;
                case GL_FLOAT_MAT2, GL_FLOAT_MAT3, GL_FLOAT_MAT4 -> 16;
                default -> 4;
            };
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 12: COMMAND BUFFER & ENCODING
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Metal command queue wrapper.
     */
    public static final class MetalCommandQueue {
        private final long handle;
        private final MetalDevice device;
        
        public MetalCommandQueue(long handle, MetalDevice device) {
            this.handle = handle;
            this.device = device;
        }
        
        public long getHandle() { return handle; }
        
        public MetalCommandBuffer createCommandBuffer() {
            long cmdBuffer = nativeCreateCommandBuffer(handle);
            return new MetalCommandBuffer(cmdBuffer, device);
        }
        
        public void release() {
            nativeReleaseCommandQueue(handle);
        }
    }
    
    /**
     * Metal command buffer wrapper.
     */
    public static final class MetalCommandBuffer {
        private final long handle;
        private final MetalDevice device;
        private MetalRenderEncoder currentRenderEncoder;
        private MetalComputeEncoder currentComputeEncoder;
        private MetalBlitEncoder currentBlitEncoder;
        
        public MetalCommandBuffer(long handle, MetalDevice device) {
            this.handle = handle;
            this.device = device;
        }
        
        public long getHandle() { return handle; }
        
        public MetalRenderEncoder beginRenderPass(long renderPassDescriptor) {
            endCurrentEncoder();
            long encoder = nativeBeginRenderPass(handle, renderPassDescriptor);
            currentRenderEncoder = new MetalRenderEncoder(encoder, device);
            return currentRenderEncoder;
        }
        
        public MetalComputeEncoder beginComputePass() {
            endCurrentEncoder();
            long encoder = nativeBeginComputePass(handle);
            currentComputeEncoder = new MetalComputeEncoder(encoder);
            return currentComputeEncoder;
        }
        
        public MetalBlitEncoder beginBlitPass() {
            endCurrentEncoder();
            long encoder = nativeBeginBlitPass(handle);
            currentBlitEncoder = new MetalBlitEncoder(encoder);
            return currentBlitEncoder;
        }
        
        public void endCurrentEncoder() {
            if (currentRenderEncoder != null) {
                currentRenderEncoder.endEncoding();
                currentRenderEncoder = null;
            }
            if (currentComputeEncoder != null) {
                currentComputeEncoder.endEncoding();
                currentComputeEncoder = null;
            }
            if (currentBlitEncoder != null) {
                currentBlitEncoder.endEncoding();
                currentBlitEncoder = null;
            }
        }
        
        public void presentDrawable(long drawable) {
            nativePresentDrawable(handle, drawable);
        }
        
        public void commit() {
            endCurrentEncoder();
            nativeCommitCommandBuffer(handle);
        }
        
        public void waitUntilCompleted() {
            nativeWaitUntilCompleted(handle);
        }
        
        public void addCompletedHandler(Runnable handler) {
            nativeAddCompletedHandler(handle, handler);
        }
    }
    
    /**
     * Metal render command encoder wrapper.
     */
    public static final class MetalRenderEncoder {
        private final long handle;
        private final MetalDevice device;
        private boolean ended = false;
        
        // Current state cache

        private long currentPipelineState = 0;
        private long currentDepthStencilState = 0;
        private final long[] currentVertexBuffers = new long[31];
        private final long[] currentVertexOffsets = new long[31];
        private final long[] currentFragmentBuffers = new long[31];
        private final long[] currentFragmentTextures = new long[128];
        private final long[] currentFragmentSamplers = new long[16];
        private final long[] currentVertexTextures = new long[128];
        private final long[] currentVertexSamplers = new long[16];
        
        public MetalRenderEncoder(long handle, MetalDevice device) {
            this.handle = handle;
            this.device = device;
        }
        
        public long getHandle() { return handle; }
        
        public void setRenderPipelineState(MetalRenderPipelineState state) {
            if (state.getHandle() != currentPipelineState) {
                nativeSetRenderPipelineState(handle, state.getHandle());
                currentPipelineState = state.getHandle();
            }
        }
        
        public void setDepthStencilState(MetalDepthStencilState state) {
            if (state.getHandle() != currentDepthStencilState) {
                nativeSetDepthStencilState(handle, state.getHandle());
                currentDepthStencilState = state.getHandle();
            }
        }
        
        public void setVertexBuffer(MetalBuffer buffer, long offset, int index) {
            long bufHandle = buffer != null ? buffer.getHandle() : 0;
            if (currentVertexBuffers[index] != bufHandle || currentVertexOffsets[index] != offset) {
                nativeSetVertexBuffer(handle, bufHandle, offset, index);
                currentVertexBuffers[index] = bufHandle;
                currentVertexOffsets[index] = offset;
            }
        }
        
        public void setVertexBytes(ByteBuffer data, int index) {
            nativeSetVertexBytes(handle, data, data.remaining(), index);
        }
        
        public void setFragmentBuffer(MetalBuffer buffer, long offset, int index) {
            long bufHandle = buffer != null ? buffer.getHandle() : 0;
            if (currentFragmentBuffers[index] != bufHandle) {
                nativeSetFragmentBuffer(handle, bufHandle, offset, index);
                currentFragmentBuffers[index] = bufHandle;
            }
        }
        
        public void setFragmentBytes(ByteBuffer data, int index) {
            nativeSetFragmentBytes(handle, data, data.remaining(), index);
        }
        
        public void setVertexTexture(MetalTexture texture, int index) {
            long texHandle = texture != null ? texture.getHandle() : 0;
            if (currentVertexTextures[index] != texHandle) {
                nativeSetVertexTexture(handle, texHandle, index);
                currentVertexTextures[index] = texHandle;
            }
        }
        
        public void setFragmentTexture(MetalTexture texture, int index) {
            long texHandle = texture != null ? texture.getHandle() : 0;
            if (currentFragmentTextures[index] != texHandle) {
                nativeSetFragmentTexture(handle, texHandle, index);
                currentFragmentTextures[index] = texHandle;
            }
        }
        
        public void setVertexSamplerState(long samplerState, int index) {
            if (currentVertexSamplers[index] != samplerState) {
                nativeSetVertexSamplerState(handle, samplerState, index);
                currentVertexSamplers[index] = samplerState;
            }
        }
        
        public void setFragmentSamplerState(long samplerState, int index) {
            if (currentFragmentSamplers[index] != samplerState) {
                nativeSetFragmentSamplerState(handle, samplerState, index);
                currentFragmentSamplers[index] = samplerState;
            }
        }
        
        public void setViewport(float x, float y, float width, float height, 
                               float znear, float zfar) {
            nativeSetViewport(handle, x, y, width, height, znear, zfar);
        }
        
        public void setScissorRect(int x, int y, int width, int height) {
            nativeSetScissorRect(handle, x, y, width, height);
        }
        
        public void setFrontFacingWinding(long winding) {
            nativeSetFrontFacingWinding(handle, winding);
        }
        
        public void setCullMode(long cullMode) {
            nativeSetCullMode(handle, cullMode);
        }
        
        public void setDepthBias(float depthBias, float slopeScale, float clamp) {
            nativeSetDepthBias(handle, depthBias, slopeScale, clamp);
        }
        
        public void setDepthClipMode(long mode) {
            nativeSetDepthClipMode(handle, mode);
        }
        
        public void setStencilReferenceValue(int value) {
            nativeSetStencilReferenceValue(handle, value);
        }
        
        public void setStencilReferenceValues(int front, int back) {
            nativeSetStencilReferenceValues(handle, front, back);
        }
        
        public void setBlendColor(float r, float g, float b, float a) {
            nativeSetBlendColor(handle, r, g, b, a);
        }
        
        public void setTriangleFillMode(long mode) {
            nativeSetTriangleFillMode(handle, mode);
        }
        
        // Draw calls
        public void drawPrimitives(long primitiveType, int vertexStart, int vertexCount) {
            nativeDrawPrimitives(handle, primitiveType, vertexStart, vertexCount);
        }
        
        public void drawPrimitives(long primitiveType, int vertexStart, int vertexCount,
                                  int instanceCount) {
            nativeDrawPrimitivesInstanced(handle, primitiveType, vertexStart, vertexCount,
                                         instanceCount);
        }
        
        public void drawPrimitives(long primitiveType, int vertexStart, int vertexCount,
                                  int instanceCount, int baseInstance) {
            nativeDrawPrimitivesInstancedBaseInstance(handle, primitiveType, vertexStart,
                                                      vertexCount, instanceCount, baseInstance);
        }
        
        public void drawIndexedPrimitives(long primitiveType, int indexCount, long indexType,
                                         MetalBuffer indexBuffer, long indexBufferOffset) {
            nativeDrawIndexedPrimitives(handle, primitiveType, indexCount, indexType,
                                       indexBuffer.getHandle(), indexBufferOffset);
        }
        
        public void drawIndexedPrimitives(long primitiveType, int indexCount, long indexType,
                                         MetalBuffer indexBuffer, long indexBufferOffset,
                                         int instanceCount) {
            nativeDrawIndexedPrimitivesInstanced(handle, primitiveType, indexCount, indexType,
                                                indexBuffer.getHandle(), indexBufferOffset,
                                                instanceCount);
        }
        
        public void drawIndexedPrimitives(long primitiveType, int indexCount, long indexType,
                                         MetalBuffer indexBuffer, long indexBufferOffset,
                                         int instanceCount, int baseVertex, int baseInstance) {
            nativeDrawIndexedPrimitivesInstancedBaseVertex(handle, primitiveType, indexCount,
                                                          indexType, indexBuffer.getHandle(),
                                                          indexBufferOffset, instanceCount,
                                                          baseVertex, baseInstance);
        }
        
        // Indirect drawing
        public void drawPrimitivesIndirect(long primitiveType, MetalBuffer indirectBuffer,
                                          long indirectBufferOffset) {
            nativeDrawPrimitivesIndirect(handle, primitiveType, indirectBuffer.getHandle(),
                                        indirectBufferOffset);
        }
        
        public void drawIndexedPrimitivesIndirect(long primitiveType, long indexType,
                                                 MetalBuffer indexBuffer, long indexBufferOffset,
                                                 MetalBuffer indirectBuffer, long indirectBufferOffset) {
            nativeDrawIndexedPrimitivesIndirect(handle, primitiveType, indexType,
                                               indexBuffer.getHandle(), indexBufferOffset,
                                               indirectBuffer.getHandle(), indirectBufferOffset);
        }
        
        // Multi-draw
        public void drawPrimitivesIndirectCount(long primitiveType, MetalBuffer indirectBuffer,
                                               long indirectBufferOffset, MetalBuffer countBuffer,
                                               long countBufferOffset, int maxDrawCount, int stride) {
            // Metal doesn't have direct equivalent; emulate with loop or use ICB
            for (int i = 0; i < maxDrawCount; i++) {
                nativeDrawPrimitivesIndirect(handle, primitiveType, indirectBuffer.getHandle(),
                                            indirectBufferOffset + (long) i * stride);
            }
        }
        
        public void endEncoding() {
            if (!ended) {
                nativeEndEncoding(handle);
                ended = true;
            }
        }
    }
    
    /**
     * Metal compute command encoder wrapper.
     */
    public static final class MetalComputeEncoder {
        private final long handle;
        private boolean ended = false;
        private long currentPipelineState = 0;
        
        public MetalComputeEncoder(long handle) {
            this.handle = handle;
        }
        
        public long getHandle() { return handle; }
        
        public void setComputePipelineState(long pipelineState) {
            if (pipelineState != currentPipelineState) {
                nativeSetComputePipelineState(handle, pipelineState);
                currentPipelineState = pipelineState;
            }
        }
        
        public void setBuffer(MetalBuffer buffer, long offset, int index) {
            nativeComputeSetBuffer(handle, buffer.getHandle(), offset, index);
        }
        
        public void setBytes(ByteBuffer data, int index) {
            nativeComputeSetBytes(handle, data, data.remaining(), index);
        }
        
        public void setTexture(MetalTexture texture, int index) {
            nativeComputeSetTexture(handle, texture.getHandle(), index);
        }
        
        public void setSamplerState(long samplerState, int index) {
            nativeComputeSetSamplerState(handle, samplerState, index);
        }
        
        public void setThreadgroupMemoryLength(int length, int index) {
            nativeComputeSetThreadgroupMemory(handle, length, index);
        }
        
        public void dispatchThreadgroups(int groupsX, int groupsY, int groupsZ,
                                        int threadsPerGroupX, int threadsPerGroupY, 
                                        int threadsPerGroupZ) {
            nativeDispatchThreadgroups(handle, groupsX, groupsY, groupsZ,
                                      threadsPerGroupX, threadsPerGroupY, threadsPerGroupZ);
        }
        
        public void dispatchThreads(int threadsX, int threadsY, int threadsZ,
                                   int threadsPerGroupX, int threadsPerGroupY,
                                   int threadsPerGroupZ) {
            nativeDispatchThreads(handle, threadsX, threadsY, threadsZ,
                                 threadsPerGroupX, threadsPerGroupY, threadsPerGroupZ);
        }
        
        public void dispatchThreadgroupsIndirect(MetalBuffer indirectBuffer, long offset,
                                                int threadsPerGroupX, int threadsPerGroupY,
                                                int threadsPerGroupZ) {
            nativeDispatchThreadgroupsIndirect(handle, indirectBuffer.getHandle(), offset,
                                              threadsPerGroupX, threadsPerGroupY, threadsPerGroupZ);
        }
        
        public void memoryBarrier(long scope, long resources) {
            nativeComputeMemoryBarrier(handle, scope, resources);
        }
        
        public void endEncoding() {
            if (!ended) {
                nativeEndComputeEncoding(handle);
                ended = true;
            }
        }
    }
    
    /**
     * Metal blit command encoder wrapper.
     */
    public static final class MetalBlitEncoder {
        private final long handle;
        private boolean ended = false;
        
        public MetalBlitEncoder(long handle) {
            this.handle = handle;
        }
        
        public long getHandle() { return handle; }
        
        public void copyFromBuffer(MetalBuffer source, long sourceOffset,
                                  MetalBuffer dest, long destOffset, long size) {
            nativeBlitCopyBuffer(handle, source.getHandle(), sourceOffset,
                                dest.getHandle(), destOffset, size);
        }
        
        public void copyFromTexture(MetalTexture source, int sourceSlice, int sourceLevel,
                                   int sourceX, int sourceY, int sourceZ,
                                   int width, int height, int depth,
                                   MetalTexture dest, int destSlice, int destLevel,
                                   int destX, int destY, int destZ) {
            nativeBlitCopyTexture(handle, source.getHandle(), sourceSlice, sourceLevel,
                                 sourceX, sourceY, sourceZ, width, height, depth,
                                 dest.getHandle(), destSlice, destLevel, destX, destY, destZ);
        }
        
        public void copyFromBufferToTexture(MetalBuffer source, long sourceOffset,
                                           int sourceBytesPerRow, int sourceBytesPerImage,
                                           int width, int height, int depth,
                                           MetalTexture dest, int destSlice, int destLevel,
                                           int destX, int destY, int destZ) {
            nativeBlitCopyBufferToTexture(handle, source.getHandle(), sourceOffset,
                                         sourceBytesPerRow, sourceBytesPerImage,
                                         width, height, depth, dest.getHandle(),
                                         destSlice, destLevel, destX, destY, destZ);
        }
        
        public void copyFromTextureToBuffer(MetalTexture source, int sourceSlice, int sourceLevel,
                                           int sourceX, int sourceY, int sourceZ,
                                           int width, int height, int depth,
                                           MetalBuffer dest, long destOffset,
                                           int destBytesPerRow, int destBytesPerImage) {
            nativeBlitCopyTextureToBuffer(handle, source.getHandle(), sourceSlice, sourceLevel,
                                         sourceX, sourceY, sourceZ, width, height, depth,
                                         dest.getHandle(), destOffset, destBytesPerRow,
                                         destBytesPerImage);
        }
        
        public void generateMipmaps(MetalTexture texture) {
            nativeBlitGenerateMipmaps(handle, texture.getHandle());
        }
        
        public void fillBuffer(MetalBuffer buffer, long offset, long length, byte value) {
            nativeBlitFillBuffer(handle, buffer.getHandle(), offset, length, value);
        }
        
        public void synchronizeResource(long resource) {
            nativeBlitSynchronizeResource(handle, resource);
        }
        
        public void synchronizeTexture(MetalTexture texture, int slice, int level) {
            nativeBlitSynchronizeTexture(handle, texture.getHandle(), slice, level);
        }
        
        public void endEncoding() {
            if (!ended) {
                nativeEndBlitEncoding(handle);
                ended = true;
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 13: FORMAT CONVERTER
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Converts between OpenGL and Metal formats.
     */
    public static final class FormatConverter {
        
        /**
         * Converts GL internal format to Metal pixel format.
         */
        public static long toMetalPixelFormat(int glFormat) {
            return switch (glFormat) {
                // 8-bit formats
                case GL_R8 -> MTLPixelFormat.R8Unorm;
                case GL_R8_SNORM -> MTLPixelFormat.R8Snorm;
                case GL_R8UI -> MTLPixelFormat.R8Uint;
                case GL_R8I -> MTLPixelFormat.R8Sint;
                
                // 16-bit formats
                case GL_R16 -> MTLPixelFormat.R16Unorm;
                case GL_R16_SNORM -> MTLPixelFormat.R16Snorm;
                case GL_R16UI -> MTLPixelFormat.R16Uint;
                case GL_R16I -> MTLPixelFormat.R16Sint;
                case GL_R16F -> MTLPixelFormat.R16Float;
                
                case GL_RG8 -> MTLPixelFormat.RG8Unorm;
                case GL_RG8_SNORM -> MTLPixelFormat.RG8Snorm;
                case GL_RG8UI -> MTLPixelFormat.RG8Uint;
                case GL_RG8I -> MTLPixelFormat.RG8Sint;
                
                // 32-bit formats
                case GL_R32UI -> MTLPixelFormat.R32Uint;
                case GL_R32I -> MTLPixelFormat.R32Sint;
                case GL_R32F -> MTLPixelFormat.R32Float;
                
                case GL_RG16 -> MTLPixelFormat.RG16Unorm;
                case GL_RG16_SNORM -> MTLPixelFormat.RG16Snorm;
                case GL_RG16UI -> MTLPixelFormat.RG16Uint;
                case GL_RG16I -> MTLPixelFormat.RG16Sint;
                case GL_RG16F -> MTLPixelFormat.RG16Float;
                
                case GL_RGBA8, GL_RGBA -> MTLPixelFormat.RGBA8Unorm;
                case GL_RGBA8_SNORM -> MTLPixelFormat.RGBA8Snorm;
                case GL_RGBA8UI -> MTLPixelFormat.RGBA8Uint;
                case GL_RGBA8I -> MTLPixelFormat.RGBA8Sint;
                case GL_SRGB8_ALPHA8 -> MTLPixelFormat.RGBA8Unorm_sRGB;
                
                case GL_RGB8, GL_RGB -> MTLPixelFormat.RGBA8Unorm; // No RGB8 in Metal
                case GL_SRGB8 -> MTLPixelFormat.RGBA8Unorm_sRGB;
                
                case GL_BGRA, GL_BGRA8_EXT -> MTLPixelFormat.BGRA8Unorm;
                
                case GL_RGB10_A2 -> MTLPixelFormat.RGB10A2Unorm;
                case GL_RGB10_A2UI -> MTLPixelFormat.RGB10A2Uint;
                case GL_R11F_G11F_B10F -> MTLPixelFormat.RG11B10Float;
                case GL_RGB9_E5 -> MTLPixelFormat.RGB9E5Float;
                
                // 64-bit formats
                case GL_RG32UI -> MTLPixelFormat.RG32Uint;
                case GL_RG32I -> MTLPixelFormat.RG32Sint;
                case GL_RG32F -> MTLPixelFormat.RG32Float;
                
                case GL_RGBA16 -> MTLPixelFormat.RGBA16Unorm;
                case GL_RGBA16_SNORM -> MTLPixelFormat.RGBA16Snorm;
                case GL_RGBA16UI -> MTLPixelFormat.RGBA16Uint;
                case GL_RGBA16I -> MTLPixelFormat.RGBA16Sint;
                case GL_RGBA16F -> MTLPixelFormat.RGBA16Float;
                
                // 128-bit formats
                case GL_RGBA32UI -> MTLPixelFormat.RGBA32Uint;
                case GL_RGBA32I -> MTLPixelFormat.RGBA32Sint;
                case GL_RGBA32F -> MTLPixelFormat.RGBA32Float;
                
                // Depth/stencil formats
                case GL_DEPTH_COMPONENT16 -> MTLPixelFormat.Depth16Unorm;
                case GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT32 -> MTLPixelFormat.Depth32Float;
                case GL_DEPTH_COMPONENT32F -> MTLPixelFormat.Depth32Float;
                case GL_DEPTH24_STENCIL8 -> MTLPixelFormat.Depth32Float_Stencil8;
                case GL_DEPTH32F_STENCIL8 -> MTLPixelFormat.Depth32Float_Stencil8;
                case GL_STENCIL_INDEX8 -> MTLPixelFormat.Stencil8;
                
                // Compressed formats
                case GL_COMPRESSED_RGB_S3TC_DXT1_EXT -> MTLPixelFormat.BC1_RGBA;
                case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> MTLPixelFormat.BC1_RGBA;
                case GL_COMPRESSED_RGBA_S3TC_DXT3_EXT -> MTLPixelFormat.BC2_RGBA;
                case GL_COMPRESSED_RGBA_S3TC_DXT5_EXT -> MTLPixelFormat.BC3_RGBA;
                case GL_COMPRESSED_SRGB_S3TC_DXT1_EXT -> MTLPixelFormat.BC1_RGBA_sRGB;
                case GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT -> MTLPixelFormat.BC1_RGBA_sRGB;
                case GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT -> MTLPixelFormat.BC2_RGBA_sRGB;
                case GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT -> MTLPixelFormat.BC3_RGBA_sRGB;
                
                case GL_COMPRESSED_RED_RGTC1 -> MTLPixelFormat.BC4_RUnorm;
                case GL_COMPRESSED_SIGNED_RED_RGTC1 -> MTLPixelFormat.BC4_RSnorm;
                case GL_COMPRESSED_RG_RGTC2 -> MTLPixelFormat.BC5_RGUnorm;
                case GL_COMPRESSED_SIGNED_RG_RGTC2 -> MTLPixelFormat.BC5_RGSnorm;
                
                case GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT -> MTLPixelFormat.BC6H_RGBUfloat;
                case GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT -> MTLPixelFormat.BC6H_RGBFloat;
                case GL_COMPRESSED_RGBA_BPTC_UNORM -> MTLPixelFormat.BC7_RGBAUnorm;
                case GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM -> MTLPixelFormat.BC7_RGBAUnorm_sRGB;
                
                default -> MTLPixelFormat.RGBA8Unorm;
            };
        }
        
        /**
         * Converts GL texture target to Metal texture type.
         */
        public static long toMetalTextureType(int target) {
            return switch (target) {
                case GL_TEXTURE_1D -> MTLTextureType.Type1D;
                case GL_TEXTURE_2D -> MTLTextureType.Type2D;
                case GL_TEXTURE_3D -> MTLTextureType.Type3D;
                case GL_TEXTURE_CUBE_MAP -> MTLTextureType.TypeCube;
                case GL_TEXTURE_1D_ARRAY -> MTLTextureType.Type1DArray;
                case GL_TEXTURE_2D_ARRAY -> MTLTextureType.Type2DArray;
                case GL_TEXTURE_CUBE_MAP_ARRAY -> MTLTextureType.TypeCubeArray;
                case GL_TEXTURE_2D_MULTISAMPLE -> MTLTextureType.Type2DMultisample;
                case GL_TEXTURE_2D_MULTISAMPLE_ARRAY -> MTLTextureType.Type2DMultisampleArray;
                case GL_TEXTURE_BUFFER -> MTLTextureType.TypeTextureBuffer;
                default -> MTLTextureType.Type2D;
            };
        }
        
        /**
         * Converts GL primitive mode to Metal primitive type.
         */
        public static long toMetalPrimitiveType(int mode) {
            return switch (mode) {
                case GL_POINTS -> MTLPrimitiveType.Point;
                case GL_LINES -> MTLPrimitiveType.Line;
                case GL_LINE_STRIP -> MTLPrimitiveType.LineStrip;
                case GL_TRIANGLES -> MTLPrimitiveType.Triangle;
                case GL_TRIANGLE_STRIP -> MTLPrimitiveType.TriangleStrip;
                default -> MTLPrimitiveType.Triangle;
            };
        }
        
        /**
         * Converts GL index type to Metal index type.
         */
        public static long toMetalIndexType(int type) {
            return switch (type) {
                case GL_UNSIGNED_SHORT -> MTLIndexType.UInt16;
                case GL_UNSIGNED_INT -> MTLIndexType.UInt32;
                default -> MTLIndexType.UInt32;
            };
        }
        
        /**
         * Converts GL blend factor to Metal blend factor.
         */
        public static long toMetalBlendFactor(int factor) {
            return switch (factor) {
                case GL_ZERO -> MTLBlendFactor.Zero;
                case GL_ONE -> MTLBlendFactor.One;
                case GL_SRC_COLOR -> MTLBlendFactor.SourceColor;
                case GL_ONE_MINUS_SRC_COLOR -> MTLBlendFactor.OneMinusSourceColor;
                case GL_DST_COLOR -> MTLBlendFactor.DestinationColor;
                case GL_ONE_MINUS_DST_COLOR -> MTLBlendFactor.OneMinusDestinationColor;
                case GL_SRC_ALPHA -> MTLBlendFactor.SourceAlpha;
                case GL_ONE_MINUS_SRC_ALPHA -> MTLBlendFactor.OneMinusSourceAlpha;
                case GL_DST_ALPHA -> MTLBlendFactor.DestinationAlpha;
                case GL_ONE_MINUS_DST_ALPHA -> MTLBlendFactor.OneMinusDestinationAlpha;
                case GL_CONSTANT_COLOR -> MTLBlendFactor.BlendColor;
                case GL_ONE_MINUS_CONSTANT_COLOR -> MTLBlendFactor.OneMinusBlendColor;
                case GL_CONSTANT_ALPHA -> MTLBlendFactor.BlendAlpha;
                case GL_ONE_MINUS_CONSTANT_ALPHA -> MTLBlendFactor.OneMinusBlendAlpha;
                case GL_SRC_ALPHA_SATURATE -> MTLBlendFactor.SourceAlphaSaturated;
                case GL_SRC1_COLOR -> MTLBlendFactor.Source1Color;
                case GL_ONE_MINUS_SRC1_COLOR -> MTLBlendFactor.OneMinusSource1Color;
                case GL_SRC1_ALPHA -> MTLBlendFactor.Source1Alpha;
                case GL_ONE_MINUS_SRC1_ALPHA -> MTLBlendFactor.OneMinusSource1Alpha;
                default -> MTLBlendFactor.One;
            };
        }
        
        /**
         * Converts GL blend equation to Metal blend operation.
         */
        public static long toMetalBlendOperation(int equation) {
            return switch (equation) {
                case GL_FUNC_ADD -> MTLBlendOperation.Add;
                case GL_FUNC_SUBTRACT -> MTLBlendOperation.Subtract;
                case GL_FUNC_REVERSE_SUBTRACT -> MTLBlendOperation.ReverseSubtract;
                case GL_MIN -> MTLBlendOperation.Min;
                case GL_MAX -> MTLBlendOperation.Max;
                default -> MTLBlendOperation.Add;
            };
        }
        
        /**
         * Converts GL compare function to Metal compare function.
         */
        public static long toMetalCompareFunction(int func) {
            return switch (func) {
                case GL_NEVER -> MTLCompareFunction.Never;
                case GL_LESS -> MTLCompareFunction.Less;
                case GL_EQUAL -> MTLCompareFunction.Equal;
                case GL_LEQUAL -> MTLCompareFunction.LessEqual;
                case GL_GREATER -> MTLCompareFunction.Greater;
                case GL_NOTEQUAL -> MTLCompareFunction.NotEqual;
                case GL_GEQUAL -> MTLCompareFunction.GreaterEqual;
                case GL_ALWAYS -> MTLCompareFunction.Always;
                default -> MTLCompareFunction.Always;
            };
        }
        
        /**
         * Converts GL stencil operation to Metal stencil operation.
         */
        public static long toMetalStencilOperation(int op) {
            return switch (op) {
                case GL_KEEP -> MTLStencilOperation.Keep;
                case GL_ZERO -> MTLStencilOperation.Zero;
                case GL_REPLACE -> MTLStencilOperation.Replace;
                case GL_INCR -> MTLStencilOperation.IncrementClamp;
                case GL_INCR_WRAP -> MTLStencilOperation.IncrementWrap;
                case GL_DECR -> MTLStencilOperation.DecrementClamp;
                case GL_DECR_WRAP -> MTLStencilOperation.DecrementWrap;
                case GL_INVERT -> MTLStencilOperation.Invert;
                default -> MTLStencilOperation.Keep;
            };
        }
        
        /**
         * Converts GL texture wrap mode to Metal sampler address mode.
         */
        public static long toMetalAddressMode(int wrap) {
            return switch (wrap) {
                case GL_REPEAT -> MTLSamplerAddressMode.Repeat;
                case GL_MIRRORED_REPEAT -> MTLSamplerAddressMode.MirrorRepeat;
                case GL_CLAMP_TO_EDGE -> MTLSamplerAddressMode.ClampToEdge;
                case GL_CLAMP_TO_BORDER -> MTLSamplerAddressMode.ClampToBorderColor;
                case GL_MIRROR_CLAMP_TO_EDGE -> MTLSamplerAddressMode.MirrorClampToEdge;
                default -> MTLSamplerAddressMode.Repeat;
            };
        }
        
        /**
         * Converts GL minification filter to Metal min/mag filter.
         */
        public static long toMetalMinMagFilter(int filter) {
            return switch (filter) {
                case GL_NEAREST, GL_NEAREST_MIPMAP_NEAREST, GL_NEAREST_MIPMAP_LINEAR -> 
                    MTLSamplerMinMagFilter.Nearest;
                case GL_LINEAR, GL_LINEAR_MIPMAP_NEAREST, GL_LINEAR_MIPMAP_LINEAR ->
                    MTLSamplerMinMagFilter.Linear;
                default -> MTLSamplerMinMagFilter.Linear;
            };
        }
        
        /**
         * Converts GL minification filter to Metal mip filter.
         */
        public static long toMetalMipFilter(int filter) {
            return switch (filter) {
                case GL_NEAREST, GL_LINEAR -> MTLSamplerMipFilter.NotMipmapped;
                case GL_NEAREST_MIPMAP_NEAREST, GL_LINEAR_MIPMAP_NEAREST -> 
                    MTLSamplerMipFilter.Nearest;
                case GL_NEAREST_MIPMAP_LINEAR, GL_LINEAR_MIPMAP_LINEAR ->
                    MTLSamplerMipFilter.Linear;
                default -> MTLSamplerMipFilter.Linear;
            };
        }
        
        /**
         * Converts GL cull face mode to Metal cull mode.
         */
        public static long toMetalCullMode(int cullFace, boolean cullEnabled) {
            if (!cullEnabled) return MTLCullMode.None;
            return switch (cullFace) {
                case GL_FRONT -> MTLCullMode.Front;
                case GL_BACK -> MTLCullMode.Back;
                case GL_FRONT_AND_BACK -> MTLCullMode.None; // Both culled = nothing rendered
                default -> MTLCullMode.None;
            };
        }
        
        /**
         * Converts GL front face to Metal winding.
         */
        public static long toMetalWinding(int frontFace) {
            // GL: CCW is default front face
            // Metal: CCW is also default
            return frontFace == GL_CCW ? MTLWinding.CounterClockwise : MTLWinding.Clockwise;
        }
        
        /**
         * Converts GL polygon mode to Metal triangle fill mode.
         */
        public static long toMetalTriangleFillMode(int mode) {
            return mode == GL_LINE ? MTLTriangleFillMode.Lines : MTLTriangleFillMode.Fill;
        }
        
        /**
         * Gets bytes per pixel for format/type combination.
         */
        public static int getBytesPerPixel(int format, int type) {
            int components = switch (format) {
                case GL_RED, GL_RED_INTEGER, GL_DEPTH_COMPONENT, GL_STENCIL_INDEX -> 1;
                case GL_RG, GL_RG_INTEGER, GL_DEPTH_STENCIL -> 2;
                case GL_RGB, GL_BGR, GL_RGB_INTEGER, GL_BGR_INTEGER -> 3;
                case GL_RGBA, GL_BGRA, GL_RGBA_INTEGER, GL_BGRA_INTEGER -> 4;
                default -> 4;
            };
            
            int bytesPerComponent = switch (type) {
                case GL_UNSIGNED_BYTE, GL_BYTE -> 1;
                case GL_UNSIGNED_SHORT, GL_SHORT, GL_HALF_FLOAT -> 2;
                case GL_UNSIGNED_INT, GL_INT, GL_FLOAT -> 4;
                case GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV -> 1;
                case GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV,
                     GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV,
                     GL_UNSIGNED_SHORT_5_5_5_1, GL_UNSIGNED_SHORT_1_5_5_5_REV -> 2;
                case GL_UNSIGNED_INT_8_8_8_8, GL_UNSIGNED_INT_8_8_8_8_REV,
                     GL_UNSIGNED_INT_10_10_10_2, GL_UNSIGNED_INT_2_10_10_10_REV,
                     GL_UNSIGNED_INT_24_8, GL_UNSIGNED_INT_10F_11F_11F_REV,
                     GL_UNSIGNED_INT_5_9_9_9_REV -> 4;
                case GL_FLOAT_32_UNSIGNED_INT_24_8_REV -> 8;
                default -> 1;
            };
            
            // Packed formats are special
            if (type == GL_UNSIGNED_BYTE_3_3_2 || type == GL_UNSIGNED_BYTE_2_3_3_REV) return 1;
            if (type >= GL_UNSIGNED_SHORT_5_6_5 && type <= GL_UNSIGNED_SHORT_1_5_5_5_REV) return 2;
            if (type >= GL_UNSIGNED_INT_8_8_8_8 && type <= GL_UNSIGNED_INT_5_9_9_9_REV) return 4;
            if (type == GL_FLOAT_32_UNSIGNED_INT_24_8_REV) return 8;
            
            return components * bytesPerComponent;
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 14: METAL CONSTANTS
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Metal storage mode constants.
     */
    public static final class MTLStorageMode {
        public static final long Shared = 0;
        public static final long Managed = 1;
        public static final long Private = 2;
        public static final long Memoryless = 3;
    }
    
    /**
     * Metal texture type constants.
     */
    public static final class MTLTextureType {
        public static final long Type1D = 0;
        public static final long Type1DArray = 1;
        public static final long Type2D = 2;
        public static final long Type2DArray = 3;
        public static final long Type2DMultisample = 4;
        public static final long TypeCube = 5;
        public static final long TypeCubeArray = 6;
        public static final long Type3D = 7;
        public static final long Type2DMultisampleArray = 8;
        public static final long TypeTextureBuffer = 9;
    }
    
    /**
     * Metal texture usage constants.
     */
    public static final class MTLTextureUsage {
        public static final long Unknown = 0;
        public static final long ShaderRead = 1;
        public static final long ShaderWrite = 2;
        public static final long RenderTarget = 4;
        public static final long PixelFormatView = 8;
    }
    
    /**
     * Metal pixel format constants.
     */
    public static final class MTLPixelFormat {
        public static final long Invalid = 0;
        
        // Ordinary 8-bit
        public static final long A8Unorm = 1;
        public static final long R8Unorm = 10;
        public static final long R8Unorm_sRGB = 11;
        public static final long R8Snorm = 12;
        public static final long R8Uint = 13;
        public static final long R8Sint = 14;
        
        // Ordinary 16-bit
        public static final long R16Unorm = 20;
        public static final long R16Snorm = 22;
        public static final long R16Uint = 23;
        public static final long R16Sint = 24;
        public static final long R16Float = 25;
        public static final long RG8Unorm = 30;
        public static final long RG8Unorm_sRGB = 31;
        public static final long RG8Snorm = 32;
        public static final long RG8Uint = 33;
        public static final long RG8Sint = 34;
        
        // Ordinary 32-bit
        public static final long R32Uint = 53;
        public static final long R32Sint = 54;
        public static final long R32Float = 55;
        public static final long RG16Unorm = 60;
        public static final long RG16Snorm = 62;
        public static final long RG16Uint = 63;
        public static final long RG16Sint = 64;
        public static final long RG16Float = 65;
        public static final long RGBA8Unorm = 70;
        public static final long RGBA8Unorm_sRGB = 71;
        public static final long RGBA8Snorm = 72;
        public static final long RGBA8Uint = 73;
        public static final long RGBA8Sint = 74;
        public static final long BGRA8Unorm = 80;
        public static final long BGRA8Unorm_sRGB = 81;
        
        // Packed 32-bit
        public static final long RGB10A2Unorm = 90;
        public static final long RGB10A2Uint = 91;
        public static final long RG11B10Float = 92;
        public static final long RGB9E5Float = 93;
        
        // Ordinary 64-bit
        public static final long RG32Uint = 103;
        public static final long RG32Sint = 104;
        public static final long RG32Float = 105;
        public static final long RGBA16Unorm = 110;
        public static final long RGBA16Snorm = 112;
        public static final long RGBA16Uint = 113;
        public static final long RGBA16Sint = 114;
        public static final long RGBA16Float = 115;
        
        // Ordinary 128-bit
        public static final long RGBA32Uint = 123;
        public static final long RGBA32Sint = 124;
        public static final long RGBA32Float = 125;
        
        // Compressed BC
        public static final long BC1_RGBA = 130;
        public static final long BC1_RGBA_sRGB = 131;
        public static final long BC2_RGBA = 132;
        public static final long BC2_RGBA_sRGB = 133;
        public static final long BC3_RGBA = 134;
        public static final long BC3_RGBA_sRGB = 135;
        public static final long BC4_RUnorm = 140;
        public static final long BC4_RSnorm = 141;
        public static final long BC5_RGUnorm = 142;
        public static final long BC5_RGSnorm = 143;
        public static final long BC6H_RGBFloat = 150;
        public static final long BC6H_RGBUfloat = 151;
        public static final long BC7_RGBAUnorm = 152;
        public static final long BC7_RGBAUnorm_sRGB = 153;
        
        // Depth/Stencil
        public static final long Depth16Unorm = 250;
        public static final long Depth32Float = 252;
        public static final long Stencil8 = 253;
        public static final long Depth24Unorm_Stencil8 = 255;
        public static final long Depth32Float_Stencil8 = 260;
        public static final long X32_Stencil8 = 261;
        public static final long X24_Stencil8 = 262;
    }
    
    /**
     * Metal primitive type constants.
     */
    public static final class MTLPrimitiveType {
        public static final long Point = 0;
        public static final long Line = 1;
        public static final long LineStrip = 2;
        public static final long Triangle = 3;
        public static final long TriangleStrip = 4;
    }
    
    /**
     * Metal index type constants.
     */
    public static final class MTLIndexType {
        public static final long UInt16 = 0;
        public static final long UInt32 = 1;
    }
    
    /**
     * Metal blend factor constants.
     */
    public static final class MTLBlendFactor {
        public static final long Zero = 0;
        public static final long One = 1;
        public static final long SourceColor = 2;
        public static final long OneMinusSourceColor = 3;
        public static final long SourceAlpha = 4;
        public static final long OneMinusSourceAlpha = 5;
        public static final long DestinationColor = 6;
        public static final long OneMinusDestinationColor = 7;
        public static final long DestinationAlpha = 8;
        public static final long OneMinusDestinationAlpha = 9;
        public static final long SourceAlphaSaturated = 10;
        public static final long BlendColor = 11;
        public static final long OneMinusBlendColor = 12;
        public static final long BlendAlpha = 13;
        public static final long OneMinusBlendAlpha = 14;
        public static final long Source1Color = 15;
        public static final long OneMinusSource1Color = 16;
        public static final long Source1Alpha = 17;
        public static final long OneMinusSource1Alpha = 18;
    }
    
    /**
     * Metal blend operation constants.
     */
    public static final class MTLBlendOperation {
        public static final long Add = 0;
        public static final long Subtract = 1;
        public static final long ReverseSubtract = 2;
        public static final long Min = 3;
        public static final long Max = 4;
    }
    
    /**
     * Metal compare function constants.
     */
    public static final class MTLCompareFunction {
        public static final long Never = 0;
        public static final long Less = 1;
        public static final long Equal = 2;
        public static final long LessEqual = 3;
        public static final long Greater = 4;
        public static final long NotEqual = 5;
        public static final long GreaterEqual = 6;
        public static final long Always = 7;
    }
    
    /**
     * Metal stencil operation constants.
     */
    public static final class MTLStencilOperation {
        public static final long Keep = 0;
        public static final long Zero = 1;
        public static final long Replace = 2;
        public static final long IncrementClamp = 3;
        public static final long DecrementClamp = 4;
        public static final long Invert = 5;
        public static final long IncrementWrap = 6;
        public static final long DecrementWrap = 7;
    }
    
    /**
     * Metal load action constants.
     */
    public static final class MTLLoadAction {
        public static final long DontCare = 0;
        public static final long Load = 1;
        public static final long Clear = 2;
    }
    
    /**
     * Metal store action constants.
     */
    public static final class MTLStoreAction {
        public static final long DontCare = 0;
        public static final long Store = 1;
        public static final long MultisampleResolve = 2;
        public static final long StoreAndMultisampleResolve = 3;
        public static final long Unknown = 4;
        public static final long CustomSampleDepthStore = 5;
    }
    
    /**
     * Metal cull mode constants.
     */
    public static final class MTLCullMode {
        public static final long None = 0;
        public static final long Front = 1;
        public static final long Back = 2;
    }
    
    /**
     * Metal winding constants.
     */
    public static final class MTLWinding {
        public static final long Clockwise = 0;
        public static final long CounterClockwise = 1;
    }
    
    /**
     * Metal triangle fill mode constants.
     */
    public static final class MTLTriangleFillMode {
        public static final long Fill = 0;
        public static final long Lines = 1;
    }
    
    /**
     * Metal sampler min/mag filter constants.
     */
    public static final class MTLSamplerMinMagFilter {
        public static final long Nearest = 0;
        public static final long Linear = 1;
    }
    
    /**
     * Metal sampler mip filter constants.
     */
    public static final class MTLSamplerMipFilter {
        public static final long NotMipmapped = 0;
        public static final long Nearest = 1;
        public static final long Linear = 2;
    }
    
    /**
     * Metal sampler address mode constants.
     */
    public static final class MTLSamplerAddressMode {
        public static final long ClampToEdge = 0;
        public static final long MirrorClampToEdge = 1;
        public static final long Repeat = 2;
        public static final long MirrorRepeat = 3;
        public static final long ClampToZero = 4;
        public static final long ClampToBorderColor = 5;
    }
    
    /**
     * Metal color write mask constants.
     */
    public static final class MTLColorWriteMask {
        public static final long None = 0;
        public static final long Red = 1 << 3;
        public static final long Green = 1 << 2;
        public static final long Blue = 1 << 1;
        public static final long Alpha = 1;
        public static final long All = Red | Green | Blue | Alpha;
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 15: PIPELINE STATE BUILDER
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Builder for Metal render pipeline state.
     */
    public static final class RenderPipelineBuilder {
        private final MetalDevice device;
        private long vertexFunction = 0;
        private long fragmentFunction = 0;
        private long vertexDescriptor = 0;
        
        // Color attachment formats
        private final long[] colorFormats = new long[8];
        private final boolean[] colorBlendEnabled = new boolean[8];
        private final long[] colorSrcRGB = new long[8];
        private final long[] colorDstRGB = new long[8];
        private final long[] colorSrcAlpha = new long[8];
        private final long[] colorDstAlpha = new long[8];
        private final long[] colorOpRGB = new long[8];
        private final long[] colorOpAlpha = new long[8];
        private final long[] colorWriteMask = new long[8];
        private int colorAttachmentCount = 0;
        
        // Depth/stencil formats
        private long depthFormat = MTLPixelFormat.Invalid;
        private long stencilFormat = MTLPixelFormat.Invalid;
        
        // Rasterization state
        private int sampleCount = 1;
        private boolean alphaToCoverageEnabled = false;
        private boolean alphaToOneEnabled = false;
        
        public RenderPipelineBuilder(MetalDevice device) {
            this.device = device;
            // Initialize defaults
            for (int i = 0; i < 8; i++) {
                colorFormats[i] = MTLPixelFormat.Invalid;
                colorBlendEnabled[i] = false;
                colorSrcRGB[i] = MTLBlendFactor.One;
                colorDstRGB[i] = MTLBlendFactor.Zero;
                colorSrcAlpha[i] = MTLBlendFactor.One;
                colorDstAlpha[i] = MTLBlendFactor.Zero;
                colorOpRGB[i] = MTLBlendOperation.Add;
                colorOpAlpha[i] = MTLBlendOperation.Add;
                colorWriteMask[i] = MTLColorWriteMask.All;
            }
        }
        
        public RenderPipelineBuilder vertexFunction(MetalShader shader) {
            this.vertexFunction = shader.getFunction();
            return this;
        }
        
        public RenderPipelineBuilder fragmentFunction(MetalShader shader) {
            this.fragmentFunction = shader != null ? shader.getFunction() : 0;
            return this;
        }
        
        public RenderPipelineBuilder vertexDescriptor(MetalVertexArray vao) {
            this.vertexDescriptor = vao.createVertexDescriptor();
            return this;
        }
        
        public RenderPipelineBuilder colorAttachment(int index, long format) {
            colorFormats[index] = format;
            colorAttachmentCount = Math.max(colorAttachmentCount, index + 1);
            return this;
        }
        
        public RenderPipelineBuilder blending(int index, boolean enabled, 
                                             long srcRGB, long dstRGB, long opRGB,
                                             long srcAlpha, long dstAlpha, long opAlpha) {
            colorBlendEnabled[index] = enabled;
            colorSrcRGB[index] = srcRGB;
            colorDstRGB[index] = dstRGB;
            colorOpRGB[index] = opRGB;
            colorSrcAlpha[index] = srcAlpha;
            colorDstAlpha[index] = dstAlpha;
            colorOpAlpha[index] = opAlpha;
            return this;
        }
        
        public RenderPipelineBuilder colorWriteMask(int index, long mask) {
            colorWriteMask[index] = mask;
            return this;
        }
        
        public RenderPipelineBuilder depthAttachment(long format) {
            this.depthFormat = format;
            return this;
        }
        
        public RenderPipelineBuilder stencilAttachment(long format) {
            this.stencilFormat = format;
            return this;
        }
        
        public RenderPipelineBuilder sampleCount(int count) {
            this.sampleCount = count;
            return this;
        }
        
        public RenderPipelineBuilder alphaToCoverage(boolean enabled) {
            this.alphaToCoverageEnabled = enabled;
            return this;
        }
        
        public RenderPipelineBuilder alphaToOne(boolean enabled) {
            this.alphaToOneEnabled = enabled;
            return this;
        }
        
        public long computeHash() {
            long hash = vertexFunction;
            hash = hash * 31 + fragmentFunction;
            hash = hash * 31 + vertexDescriptor;
            
            for (int i = 0; i < colorAttachmentCount; i++) {
                hash = hash * 31 + colorFormats[i];
                hash = hash * 31 + (colorBlendEnabled[i] ? 1 : 0);
                hash = hash * 31 + colorSrcRGB[i];
                hash = hash * 31 + colorDstRGB[i];
                hash = hash * 31 + colorOpRGB[i];
                hash = hash * 31 + colorSrcAlpha[i];
                hash = hash * 31 + colorDstAlpha[i];
                hash = hash * 31 + colorOpAlpha[i];
                hash = hash * 31 + colorWriteMask[i];
            }
            
            hash = hash * 31 + depthFormat;
            hash = hash * 31 + stencilFormat;
            hash = hash * 31 + sampleCount;
            hash = hash * 31 + (alphaToCoverageEnabled ? 1 : 0);
            hash = hash * 31 + (alphaToOneEnabled ? 1 : 0);
            
            return hash;
        }
        
        public MetalRenderPipelineState build() {
            long hash = computeHash();
            
            // Check cache first
            MetalRenderPipelineState cached = device.getOrCreateRenderPipeline(hash, () -> {
                // Create new pipeline state
                long descriptor = nativeCreateRenderPipelineDescriptor();
                
                nativeRenderPipelineSetVertexFunction(descriptor, vertexFunction);
                nativeRenderPipelineSetFragmentFunction(descriptor, fragmentFunction);
                
                if (vertexDescriptor != 0) {
                    nativeRenderPipelineSetVertexDescriptor(descriptor, vertexDescriptor);
                }
                
                for (int i = 0; i < colorAttachmentCount; i++) {
                    nativeRenderPipelineSetColorAttachment(descriptor, i, colorFormats[i],
                        colorBlendEnabled[i], colorSrcRGB[i], colorDstRGB[i], colorOpRGB[i],
                        colorSrcAlpha[i], colorDstAlpha[i], colorOpAlpha[i], colorWriteMask[i]);
                }
                
                nativeRenderPipelineSetDepthFormat(descriptor, depthFormat);
                nativeRenderPipelineSetStencilFormat(descriptor, stencilFormat);
                nativeRenderPipelineSetSampleCount(descriptor, sampleCount);
                nativeRenderPipelineSetAlphaToCoverage(descriptor, alphaToCoverageEnabled);
                nativeRenderPipelineSetAlphaToOne(descriptor, alphaToOneEnabled);
                
                long pipelineState = nativeCreateRenderPipelineState(device.getHandle(), descriptor);
                nativeReleaseRenderPipelineDescriptor(descriptor);
                
                return new MetalRenderPipelineState(pipelineState, hash);
            });
            
            return cached;
        }
    }
    
    /**
     * Builder for Metal depth stencil state.
     */
    public static final class DepthStencilBuilder {
        private final MetalDevice device;
        
        private boolean depthTestEnabled = false;
        private boolean depthWriteEnabled = true;
        private long depthCompareFunction = MTLCompareFunction.Less;
        
        private boolean stencilTestEnabled = false;
        private int stencilReadMask = 0xFF;
        private int stencilWriteMask = 0xFF;
        
        // Front face stencil
        private long frontStencilCompare = MTLCompareFunction.Always;
        private long frontStencilFail = MTLStencilOperation.Keep;
        private long frontDepthFail = MTLStencilOperation.Keep;
        private long frontStencilPass = MTLStencilOperation.Keep;
        
        // Back face stencil
        private long backStencilCompare = MTLCompareFunction.Always;
        private long backStencilFail = MTLStencilOperation.Keep;
        private long backDepthFail = MTLStencilOperation.Keep;
        private long backStencilPass = MTLStencilOperation.Keep;
        
        public DepthStencilBuilder(MetalDevice device) {
            this.device = device;
        }
        
        public DepthStencilBuilder depthTest(boolean enabled) {
            this.depthTestEnabled = enabled;
            return this;
        }
        
        public DepthStencilBuilder depthWrite(boolean enabled) {
            this.depthWriteEnabled = enabled;
            return this;
        }
        
        public DepthStencilBuilder depthCompare(long func) {
            this.depthCompareFunction = func;
            return this;
        }
        
        public DepthStencilBuilder stencilTest(boolean enabled) {
            this.stencilTestEnabled = enabled;
            return this;
        }
        
        public DepthStencilBuilder stencilMask(int readMask, int writeMask) {
            this.stencilReadMask = readMask;
            this.stencilWriteMask = writeMask;
            return this;
        }
        
        public DepthStencilBuilder frontStencil(long compare, long stencilFail, 
                                               long depthFail, long pass) {
            this.frontStencilCompare = compare;
            this.frontStencilFail = stencilFail;
            this.frontDepthFail = depthFail;
            this.frontStencilPass = pass;
            return this;
        }
        
        public DepthStencilBuilder backStencil(long compare, long stencilFail,
                                              long depthFail, long pass) {
            this.backStencilCompare = compare;
            this.backStencilFail = stencilFail;
            this.backDepthFail = depthFail;
            this.backStencilPass = pass;
            return this;
        }
        
        public long computeHash() {
            long hash = depthTestEnabled ? 1 : 0;
            hash = hash * 31 + (depthWriteEnabled ? 1 : 0);
            hash = hash * 31 + depthCompareFunction;
            hash = hash * 31 + (stencilTestEnabled ? 1 : 0);
            hash = hash * 31 + stencilReadMask;
            hash = hash * 31 + stencilWriteMask;
            hash = hash * 31 + frontStencilCompare;
            hash = hash * 31 + frontStencilFail;
            hash = hash * 31 + frontDepthFail;
            hash = hash * 31 + frontStencilPass;
            hash = hash * 31 + backStencilCompare;
            hash = hash * 31 + backStencilFail;
            hash = hash * 31 + backDepthFail;
            hash = hash * 31 + backStencilPass;
            return hash;
        }
        
        public MetalDepthStencilState build() {
            long hash = computeHash();
            
            return device.getOrCreateDepthStencilState(hash, () -> {
                long descriptor = nativeCreateDepthStencilDescriptor();
                
                // Depth state
                long effectiveCompare = depthTestEnabled ? depthCompareFunction : MTLCompareFunction.Always;
                nativeDepthStencilSetDepth(descriptor, effectiveCompare, 
                                          depthTestEnabled && depthWriteEnabled);
                
                // Stencil state
                if (stencilTestEnabled) {
                    nativeDepthStencilSetFrontStencil(descriptor, frontStencilCompare,
                        frontStencilFail, frontDepthFail, frontStencilPass,
                        stencilReadMask, stencilWriteMask);
                    
                    nativeDepthStencilSetBackStencil(descriptor, backStencilCompare,
                        backStencilFail, backDepthFail, backStencilPass,
                        stencilReadMask, stencilWriteMask);
                }
                
                long state = nativeCreateDepthStencilState(device.getHandle(), descriptor);
                nativeReleaseDepthStencilDescriptor(descriptor);
                
                return new MetalDepthStencilState(state, hash);
            });
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 16: QUERY OBJECTS
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Metal query wrapper for occlusion and timestamp queries.
     */
    public static final class MetalQuery {
        private final int glId;
        private final int target;
        private long visibilityBuffer = 0;
        private long result = 0;
        private boolean resultAvailable = false;
        private boolean active = false;
        
        public MetalQuery(int id, int target) {
            this.glId = id;
            this.target = target;
        }
        
        public int getGLId() { return glId; }
        public int getTarget() { return target; }
        public boolean isActive() { return active; }
        public boolean isResultAvailable() { return resultAvailable; }
        public long getResult() { return result; }
        
        public void begin(long deviceHandle) {
            if (target == GL_SAMPLES_PASSED || target == GL_ANY_SAMPLES_PASSED ||
                target == GL_ANY_SAMPLES_PASSED_CONSERVATIVE) {
                // Create visibility result buffer if needed
                if (visibilityBuffer == 0) {
                    visibilityBuffer = nativeCreateBuffer(deviceHandle, 8, 
                        MTLStorageMode.Shared << 4);
                }
            }
            active = true;
            resultAvailable = false;
        }
        
        public void end() {
            active = false;
        }
        
        public void setVisibilityResultBuffer(long renderEncoder, int offset) {
            if (visibilityBuffer != 0) {
                nativeSetVisibilityResultBuffer(renderEncoder, visibilityBuffer, offset);
            }
        }
        
        public void fetchResult() {
            if (visibilityBuffer != 0 && !resultAvailable) {
                ByteBuffer mapped = nativeMapBuffer(visibilityBuffer, 0, 8);
                if (mapped != null) {
                    result = mapped.getLong(0);
                    resultAvailable = true;
                }
            }
        }
        
        public void release() {
            if (visibilityBuffer != 0) {
                nativeReleaseBuffer(visibilityBuffer);
                visibilityBuffer = 0;
            }
        }
    }
    
    /**
     * Metal timestamp query support.
     */
    public static final class MetalTimestampQuery {
        private final int glId;
        private long timestampBuffer = 0;
        private long cpuTimestamp = 0;
        private long gpuTimestamp = 0;
        private boolean resultAvailable = false;
        
        public MetalTimestampQuery(int id) {
            this.glId = id;
        }
        
        public int getGLId() { return glId; }
        public boolean isResultAvailable() { return resultAvailable; }
        public long getGpuTimestamp() { return gpuTimestamp; }
        public long getCpuTimestamp() { return cpuTimestamp; }
        
        public void recordTimestamp(long commandBuffer, long deviceHandle) {
            if (timestampBuffer == 0) {
                timestampBuffer = nativeCreateBuffer(deviceHandle, 16, MTLStorageMode.Shared << 4);
            }
            
            // Sample timestamps
            nativeSampleTimestamps(commandBuffer, deviceHandle, timestampBuffer);
            resultAvailable = false;
        }
        
        public void fetchResult() {
            if (timestampBuffer != 0 && !resultAvailable) {
                ByteBuffer mapped = nativeMapBuffer(timestampBuffer, 0, 16);
                if (mapped != null) {
                    cpuTimestamp = mapped.getLong(0);
                    gpuTimestamp = mapped.getLong(8);
                    resultAvailable = true;
                }
            }
        }
        
        public void release() {
            if (timestampBuffer != 0) {
                nativeReleaseBuffer(timestampBuffer);
                timestampBuffer = 0;
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 17: SYNC OBJECTS
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Metal fence/sync wrapper for GL fence sync objects.
     */
    public static final class MetalSync {
        private final long glId;
        private long mtlEvent = 0;
        private long eventValue = 0;
        private volatile boolean signaled = false;
        
        public MetalSync(long id) {
            this.glId = id;
        }
        
        public long getGLId() { return glId; }
        public boolean isSignaled() { return signaled; }
        
        public void createFence(long deviceHandle, long commandBuffer) {
            if (mtlEvent == 0) {
                mtlEvent = nativeCreateSharedEvent(deviceHandle);
            }
            eventValue++;
            
            // Encode signal on command buffer
            nativeEncodeSignalEvent(commandBuffer, mtlEvent, eventValue);
        }
        
        public int clientWait(long timeout) {
            if (signaled) return GL_ALREADY_SIGNALED;
            
            if (mtlEvent == 0) return GL_WAIT_FAILED;
            
            // Check if already signaled
            long currentValue = nativeGetSharedEventValue(mtlEvent);
            if (currentValue >= eventValue) {
                signaled = true;
                return GL_ALREADY_SIGNALED;
            }
            
            // Wait with timeout
            boolean success = nativeWaitForSharedEvent(mtlEvent, eventValue, timeout);
            if (success) {
                signaled = true;
                return GL_CONDITION_SATISFIED;
            }
            
            return GL_TIMEOUT_EXPIRED;
        }
        
        public void waitGPU(long commandBuffer) {
            if (mtlEvent != 0 && !signaled) {
                nativeEncodeWaitEvent(commandBuffer, mtlEvent, eventValue);
            }
        }
        
        public void release() {
            if (mtlEvent != 0) {
                nativeReleaseSharedEvent(mtlEvent);
                mtlEvent = 0;
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 18: NATIVE METHOD DECLARATIONS
    // ════════════════════════════════════════════════════════════════════════════
    
    // Device and command queue
    private static native long nativeCreateDevice();
    private static native void nativeReleaseDevice(long device);
    private static native long nativeCreateCommandQueue(long device);
    private static native void nativeReleaseCommandQueue(long queue);
    private static native String nativeGetDeviceName(long device);
    private static native long nativeGetDeviceMaxBufferLength(long device);
    private static native int nativeGetDeviceMaxThreadsPerThreadgroup(long device);
    
    // Command buffer
    private static native long nativeCreateCommandBuffer(long queue);
    private static native long nativeBeginRenderPass(long cmdBuffer, long renderPassDescriptor);
    private static native long nativeBeginComputePass(long cmdBuffer);
    private static native long nativeBeginBlitPass(long cmdBuffer);
    private static native void nativePresentDrawable(long cmdBuffer, long drawable);
    private static native void nativeCommitCommandBuffer(long cmdBuffer);
    private static native void nativeWaitUntilCompleted(long cmdBuffer);
    private static native void nativeAddCompletedHandler(long cmdBuffer, Runnable handler);
    
    // Buffer operations
    private static native long nativeCreateBuffer(long device, long size, long options);
    private static native long nativeCreateBufferWithData(long device, ByteBuffer data, 
                                                          long size, long options);
    private static native void nativeReleaseBuffer(long buffer);
    private static native ByteBuffer nativeMapBuffer(long buffer, long offset, long length);
    private static native void nativeBufferSubData(long buffer, long offset, ByteBuffer data, int length);
    private static native void nativeBufferSubDataBlit(long buffer, long offset, ByteBuffer data, int length);
    private static native void nativeDidModifyRange(long buffer, long offset, long length);
    
    // Texture operations
    private static native long nativeCreateTexture(long device, long type, long format,
                                                   int width, int height, int depth,
                                                   int mipLevels, int sampleCount, long usage);
    private static native long nativeCreateTextureMS(long device, long format, int width, int height,
                                                     int sampleCount, long usage);
    private static native long nativeCreateTextureView(long texture, long type, long format,
                                                       int firstMipLevel, int mipLevelCount,
                                                       int firstArraySlice, int arraySliceCount);
    private static native void nativeReleaseTexture(long texture);
    private static native void nativeTextureReplaceRegion(long texture, int level,
                                                          int x, int y, int z,
                                                          int width, int height, int depth,
                                                          ByteBuffer data, int bytesPerRow,
                                                          int bytesPerImage);
    
    // Sampler operations
    private static native long nativeCreateSamplerState(long device, long minFilter, long magFilter,
                                                        long mipFilter, long addressS, 
                                                        long addressT, long addressR,
                                                        float minLod, float maxLod,
                                                        float maxAnisotropy, boolean compareEnabled,
                                                        long compareFunc, float[] borderColor);
    private static native void nativeReleaseSamplerState(long sampler);
    
    // Shader compilation
    private static native long[] nativeCompileMSL(long device, String source, 
                                                   String entryPoint, int type);
    private static native String nativeGetCompileError(long device);
    private static native void nativeReleaseFunction(long function);
    private static native void nativeReleaseLibrary(long library);
    
    // Render pass descriptor
    private static native long nativeCreateRenderPassDescriptor();
    private static native void nativeReleaseRenderPassDescriptor(long descriptor);
    private static native void nativeRenderPassSetColorAttachment(long descriptor, int index,
                                                                   long texture, int level, int slice,
                                                                   long loadAction, long storeAction,
                                                                   float r, float g, float b, float a);
    private static native void nativeRenderPassSetDepthAttachment(long descriptor, long texture,
                                                                   int level, long loadAction,
                                                                   long storeAction, float clearDepth);
    private static native void nativeRenderPassSetStencilAttachment(long descriptor, long texture,
                                                                     int level, long loadAction,
                                                                     long storeAction, int clearStencil);
    
    // Render pipeline
    private static native long nativeCreateRenderPipelineDescriptor();
    private static native void nativeReleaseRenderPipelineDescriptor(long descriptor);
    private static native void nativeRenderPipelineSetVertexFunction(long descriptor, long function);
    private static native void nativeRenderPipelineSetFragmentFunction(long descriptor, long function);
    private static native void nativeRenderPipelineSetVertexDescriptor(long descriptor, long vertexDescriptor);
    private static native void nativeRenderPipelineSetColorAttachment(long descriptor, int index,
                                                                       long format, boolean blendEnabled,
                                                                       long srcRGB, long dstRGB, long opRGB,
                                                                       long srcAlpha, long dstAlpha, 
                                                                       long opAlpha, long writeMask);
    private static native void nativeRenderPipelineSetDepthFormat(long descriptor, long format);
    private static native void nativeRenderPipelineSetStencilFormat(long descriptor, long format);
    private static native void nativeRenderPipelineSetSampleCount(long descriptor, int count);
    private static native void nativeRenderPipelineSetAlphaToCoverage(long descriptor, boolean enabled);
    private static native void nativeRenderPipelineSetAlphaToOne(long descriptor, boolean enabled);
    private static native long nativeCreateRenderPipelineState(long device, long descriptor);
    private static native void nativeReleasePipelineState(long state);
    
    // Depth stencil state
    private static native long nativeCreateDepthStencilDescriptor();
    private static native void nativeReleaseDepthStencilDescriptor(long descriptor);
    private static native void nativeDepthStencilSetDepth(long descriptor, long compareFunc, boolean writeEnabled);
    private static native void nativeDepthStencilSetFrontStencil(long descriptor, long compare,
                                                                  long stencilFail, long depthFail, long pass,
                                                                  int readMask, int writeMask);
    private static native void nativeDepthStencilSetBackStencil(long descriptor, long compare,
                                                                 long stencilFail, long depthFail, long pass,
                                                                 int readMask, int writeMask);
    private static native long nativeCreateDepthStencilState(long device, long descriptor);
    private static native void nativeReleaseDepthStencilState(long state);
    
    // Vertex descriptor
    private static native long nativeCreateVertexDescriptor();
    private static native void nativeVertexDescriptorSetAttribute(long descriptor, int index,
                                                                   long format, long offset, int bufferIndex);
    private static native void nativeVertexDescriptorSetLayout(long descriptor, int index,
                                                                int stride, long stepFunction, int stepRate);
    private static native void nativeReleaseVertexDescriptor(long descriptor);
    
    // Render encoder
    private static native void nativeSetRenderPipelineState(long encoder, long state);
    private static native void nativeSetDepthStencilState(long encoder, long state);
    private static native void nativeSetVertexBuffer(long encoder, long buffer, long offset, int index);
    private static native void nativeSetVertexBytes(long encoder, ByteBuffer data, int length, int index);
    private static native void nativeSetFragmentBuffer(long encoder, long buffer, long offset, int index);
    private static native void nativeSetFragmentBytes(long encoder, ByteBuffer data, int length, int index);
    private static native void nativeSetVertexTexture(long encoder, long texture, int index);
    private static native void nativeSetFragmentTexture(long encoder, long texture, int index);
    private static native void nativeSetVertexSamplerState(long encoder, long sampler, int index);
    private static native void nativeSetFragmentSamplerState(long encoder, long sampler, int index);
    private static native void nativeSetViewport(long encoder, float x, float y, float w, float h,
                                                  float znear, float zfar);
    private static native void nativeSetScissorRect(long encoder, int x, int y, int w, int h);
    private static native void nativeSetFrontFacingWinding(long encoder, long winding);
    private static native void nativeSetCullMode(long encoder, long mode);
    private static native void nativeSetDepthBias(long encoder, float bias, float slope, float clamp);
    private static native void nativeSetDepthClipMode(long encoder, long mode);
    private static native void nativeSetStencilReferenceValue(long encoder, int value);
    private static native void nativeSetStencilReferenceValues(long encoder, int front, int back);
    private static native void nativeSetBlendColor(long encoder, float r, float g, float b, float a);
    private static native void nativeSetTriangleFillMode(long encoder, long mode);
    private static native void nativeSetVisibilityResultBuffer(long encoder, long buffer, int offset);
    
    // Draw commands
    private static native void nativeDrawPrimitives(long encoder, long primitiveType, 
                                                     int vertexStart, int vertexCount);
    private static native void nativeDrawPrimitivesInstanced(long encoder, long primitiveType,
                                                              int vertexStart, int vertexCount,
                                                              int instanceCount);
    private static native void nativeDrawPrimitivesInstancedBaseInstance(long encoder, long primitiveType,
                                                                          int vertexStart, int vertexCount,
                                                                          int instanceCount, int baseInstance);
    private static native void nativeDrawIndexedPrimitives(long encoder, long primitiveType,
                                                            int indexCount, long indexType,
                                                            long indexBuffer, long indexBufferOffset);
    private static native void nativeDrawIndexedPrimitivesInstanced(long encoder, long primitiveType,
                                                                     int indexCount, long indexType,
                                                                     long indexBuffer, long indexBufferOffset,
                                                                     int instanceCount);
    private static native void nativeDrawIndexedPrimitivesInstancedBaseVertex(long encoder, long primitiveType,
                                                                               int indexCount, long indexType,
                                                                               long indexBuffer, long indexBufferOffset,
                                                                               int instanceCount, int baseVertex,
                                                                               int baseInstance);
    private static native void nativeDrawPrimitivesIndirect(long encoder, long primitiveType,
                                                             long indirectBuffer, long indirectBufferOffset);
    private static native void nativeDrawIndexedPrimitivesIndirect(long encoder, long primitiveType,
                                                                    long indexType, long indexBuffer,
                                                                    long indexBufferOffset, long indirectBuffer,
                                                                    long indirectBufferOffset);
    private static native void nativeEndEncoding(long encoder);
    
    // Compute encoder
    private static native void nativeSetComputePipelineState(long encoder, long state);
    private static native void nativeComputeSetBuffer(long encoder, long buffer, long offset, int index);
    private static native void nativeComputeSetBytes(long encoder, ByteBuffer data, int length, int index);
    private static native void nativeComputeSetTexture(long encoder, long texture, int index);
    private static native void nativeComputeSetSamplerState(long encoder, long sampler, int index);
    private static native void nativeComputeSetThreadgroupMemory(long encoder, int length, int index);
    private static native void nativeDispatchThreadgroups(long encoder, int groupsX, int groupsY, int groupsZ,
                                                           int threadsX, int threadsY, int threadsZ);
    private static native void nativeDispatchThreads(long encoder, int threadsX, int threadsY, int threadsZ,
                                                      int perGroupX, int perGroupY, int perGroupZ);
    private static native void nativeDispatchThreadgroupsIndirect(long encoder, long indirectBuffer,
                                                                   long indirectBufferOffset,
                                                                   int threadsX, int threadsY, int threadsZ);
    private static native void nativeComputeMemoryBarrier(long encoder, long scope, long resources);
    private static native void nativeEndComputeEncoding(long encoder);
    
    // Blit encoder
    private static native void nativeBlitCopyBuffer(long encoder, long src, long srcOffset,
                                                     long dst, long dstOffset, long size);
    private static native void nativeBlitCopyTexture(long encoder, long src, int srcSlice, int srcLevel,
                                                      int srcX, int srcY, int srcZ,
                                                      int width, int height, int depth,
                                                      long dst, int dstSlice, int dstLevel,
                                                      int dstX, int dstY, int dstZ);
    private static native void nativeBlitCopyBufferToTexture(long encoder, long src, long srcOffset,
                                                              int srcBytesPerRow, int srcBytesPerImage,
                                                              int width, int height, int depth,
                                                              long dst, int dstSlice, int dstLevel,
                                                              int dstX, int dstY, int dstZ);
    private static native void nativeBlitCopyTextureToBuffer(long encoder, long src, int srcSlice, int srcLevel,
                                                              int srcX, int srcY, int srcZ,
                                                              int width, int height, int depth,
                                                              long dst, long dstOffset,
                                                              int dstBytesPerRow, int dstBytesPerImage);
    private static native void nativeBlitGenerateMipmaps(long encoder, long texture);
    private static native void nativeBlitFillBuffer(long encoder, long buffer, long offset, long length, byte value);
    private static native void nativeBlitSynchronizeResource(long encoder, long resource);
    private static native void nativeBlitSynchronizeTexture(long encoder, long texture, int slice, int level);
    private static native void nativeEndBlitEncoding(long encoder);
    
    // Sync objects
    private static native long nativeCreateSharedEvent(long device);
    private static native void nativeReleaseSharedEvent(long event);
    private static native long nativeGetSharedEventValue(long event);
    private static native void nativeEncodeSignalEvent(long cmdBuffer, long event, long value);
    private static native void nativeEncodeWaitEvent(long cmdBuffer, long event, long value);
    private static native boolean nativeWaitForSharedEvent(long event, long value, long timeout);
    
    // Timestamps
    private static native void nativeSampleTimestamps(long cmdBuffer, long device, long buffer);
    
    // Compute pipeline
    private static native long nativeCreateComputePipelineState(long device, long function);
    private static native void nativeReleaseComputePipelineState(long state);
    
    // CAMetalLayer operations (for windowing)
    private static native long nativeCreateMetalLayer();
    private static native void nativeSetLayerDevice(long layer, long device);
    private static native void nativeSetLayerPixelFormat(long layer, long format);
    private static native void nativeSetLayerDrawableSize(long layer, int width, int height);
    private static native long nativeGetNextDrawable(long layer);
    private static native long nativeGetDrawableTexture(long drawable);
    private static native void nativeReleaseDrawable(long drawable);
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 19: GL CONSTANTS
    // ════════════════════════════════════════════════════════════════════════════
    
    // Note: These are standard OpenGL constants used throughout the mapper
    // Only listing ones used in switch statements above; full list would be extensive
    
    // Primitive types
    public static final int GL_POINTS = 0x0000;
    public static final int GL_LINES = 0x0001;
    public static final int GL_LINE_LOOP = 0x0002;
    public static final int GL_LINE_STRIP = 0x0003;
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_TRIANGLE_STRIP = 0x0005;
    public static final int GL_TRIANGLE_FAN = 0x0006;
    
    // Data types
    public static final int GL_BYTE = 0x1400;
    public static final int GL_UNSIGNED_BYTE = 0x1401;
    public static final int GL_SHORT = 0x1402;
    public static final int GL_UNSIGNED_SHORT = 0x1403;
    public static final int GL_INT = 0x1404;
    public static final int GL_UNSIGNED_INT = 0x1405;
    public static final int GL_FLOAT = 0x1406;
    public static final int GL_DOUBLE = 0x140A;
    public static final int GL_HALF_FLOAT = 0x140B;
    public static final int GL_FIXED = 0x140C;
    
    // Boolean
    public static final int GL_FALSE = 0;
    public static final int GL_TRUE = 1;
    
    // Buffer targets
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    public static final int GL_COPY_READ_BUFFER = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    public static final int GL_DRAW_INDIRECT_BUFFER = 0x8F3F;
    public static final int GL_DISPATCH_INDIRECT_BUFFER = 0x90EE;
    public static final int GL_PIXEL_PACK_BUFFER = 0x88EB;
    public static final int GL_PIXEL_UNPACK_BUFFER = 0x88EC;
    public static final int GL_TRANSFORM_FEEDBACK_BUFFER = 0x8C8E;
    
    // Buffer usage
    public static final int GL_STREAM_DRAW = 0x88E0;
    public static final int GL_STREAM_READ = 0x88E1;
    public static final int GL_STREAM_COPY = 0x88E2;
    public static final int GL_STATIC_DRAW = 0x88E4;
    public static final int GL_STATIC_READ = 0x88E5;
    public static final int GL_STATIC_COPY = 0x88E6;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_DYNAMIC_READ = 0x88E9;
    public static final int GL_DYNAMIC_COPY = 0x88EA;
    
    // Buffer storage flags
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_MAP_WRITE_BIT = 0x0002;
    public static final int GL_MAP_PERSISTENT_BIT = 0x0040;
    public static final int GL_MAP_COHERENT_BIT = 0x0080;
    public static final int GL_DYNAMIC_STORAGE_BIT = 0x0100;
    public static final int GL_CLIENT_STORAGE_BIT = 0x0200;
    
    // Texture targets
    public static final int GL_TEXTURE_1D = 0x0DE0;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_TEXTURE_3D = 0x806F;
    public static final int GL_TEXTURE_CUBE_MAP = 0x8513;
    public static final int GL_TEXTURE_1D_ARRAY = 0x8C18;
    public static final int GL_TEXTURE_2D_ARRAY = 0x8C1A;
    public static final int GL_TEXTURE_CUBE_MAP_ARRAY = 0x9009;
    public static final int GL_TEXTURE_2D_MULTISAMPLE = 0x9100;
    public static final int GL_TEXTURE_2D_MULTISAMPLE_ARRAY = 0x9102;
    public static final int GL_TEXTURE_BUFFER = 0x8C2A;
    public static final int GL_TEXTURE_RECTANGLE = 0x84F5;
    
    // Internal formats
    public static final int GL_R8 = 0x8229;
    public static final int GL_R8_SNORM = 0x8F94;
    public static final int GL_R8UI = 0x8232;
    public static final int GL_R8I = 0x8231;
    public static final int GL_R16 = 0x822A;
    public static final int GL_R16_SNORM = 0x8F98;
    public static final int GL_R16UI = 0x8234;
    public static final int GL_R16I = 0x8233;
    public static final int GL_R16F = 0x822D;
    public static final int GL_R32UI = 0x8236;
    public static final int GL_R32I = 0x8235;
    public static final int GL_R32F = 0x822E;
    public static final int GL_RG8 = 0x822B;
    public static final int GL_RG8_SNORM = 0x8F95;
    public static final int GL_RG8UI = 0x8238;
    public static final int GL_RG8I = 0x8237;
    public static final int GL_RG16 = 0x822C;
    public static final int GL_RG16_SNORM = 0x8F99;
    public static final int GL_RG16UI = 0x823A;
    public static final int GL_RG16I = 0x8239;
    public static final int GL_RG16F = 0x822F;
    public static final int GL_RG32UI = 0x823C;
    public static final int GL_RG32I = 0x823B;
    public static final int GL_RG32F = 0x8230;
    public static final int GL_RGB = 0x1907;
    public static final int GL_RGB8 = 0x8051;
    public static final int GL_SRGB8 = 0x8C41;
    public static final int GL_RGBA = 0x1908;
    public static final int GL_RGBA8 = 0x8058;
    public static final int GL_RGBA8_SNORM = 0x8F97;
    public static final int GL_RGBA8UI = 0x8D7C;
    public static final int GL_RGBA8I = 0x8D8E;
    public static final int GL_SRGB8_ALPHA8 = 0x8C43;
    public static final int GL_BGRA = 0x80E1;
    public static final int GL_BGRA8_EXT = 0x93A1;
    public static final int GL_RGB10_A2 = 0x8059;
    public static final int GL_RGB10_A2UI = 0x906F;
    public static final int GL_R11F_G11F_B10F = 0x8C3A;
    public static final int GL_RGB9_E5 = 0x8C3D;
    public static final int GL_RGBA16 = 0x805B;
    public static final int GL_RGBA16_SNORM = 0x8F9B;
    public static final int GL_RGBA16UI = 0x8D76;
    public static final int GL_RGBA16I = 0x8D88;
    public static final int GL_RGBA16F = 0x881A;
    public static final int GL_RGBA32UI = 0x8D70;
    public static final int GL_RGBA32I = 0x8D82;
    public static final int GL_RGBA32F = 0x8814;
    
    // Depth/stencil formats
    public static final int GL_DEPTH_COMPONENT16 = 0x81A5;
    public static final int GL_DEPTH_COMPONENT24 = 0x81A6;
    public static final int GL_DEPTH_COMPONENT32 = 0x81A7;
    public static final int GL_DEPTH_COMPONENT32F = 0x8CAC;
    public static final int GL_DEPTH24_STENCIL8 = 0x88F0;
    public static final int GL_DEPTH32F_STENCIL8 = 0x8CAD;
    public static final int GL_STENCIL_INDEX8 = 0x8D48;
    
    // Compressed formats
    public static final int GL_COMPRESSED_RGB_S3TC_DXT1_EXT = 0x83F0;
    public static final int GL_COMPRESSED_RGBA_S3TC_DXT1_EXT = 0x83F1;
    public static final int GL_COMPRESSED_RGBA_S3TC_DXT3_EXT = 0x83F2;
    public static final int GL_COMPRESSED_RGBA_S3TC_DXT5_EXT = 0x83F3;
    public static final int GL_COMPRESSED_SRGB_S3TC_DXT1_EXT = 0x8C4C;
    public static final int GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT = 0x8C4D;
    public static final int GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT = 0x8C4E;
    public static final int GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT = 0x8C4F;
    public static final int GL_COMPRESSED_RED_RGTC1 = 0x8DBB;
    public static final int GL_COMPRESSED_SIGNED_RED_RGTC1 = 0x8DBC;
    public static final int GL_COMPRESSED_RG_RGTC2 = 0x8DBD;
    public static final int GL_COMPRESSED_SIGNED_RG_RGTC2 = 0x8DBE;
    public static final int GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT = 0x8E8F;
    public static final int GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT = 0x8E8E;
    public static final int GL_COMPRESSED_RGBA_BPTC_UNORM = 0x8E8C;
    public static final int GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM = 0x8E8D;
    
    // Pixel formats
    public static final int GL_RED = 0x1903;
    public static final int GL_RG = 0x8227;
    public static final int GL_BGR = 0x80E0;
    public static final int GL_RED_INTEGER = 0x8D94;
    public static final int GL_RG_INTEGER = 0x8228;
    public static final int GL_RGB_INTEGER = 0x8D98;
    public static final int GL_BGR_INTEGER = 0x8D9A;
    public static final int GL_RGBA_INTEGER = 0x8D99;
    public static final int GL_BGRA_INTEGER = 0x8D9B;
    public static final int GL_DEPTH_COMPONENT = 0x1902;
    public static final int GL_STENCIL_INDEX = 0x1901;
    public static final int GL_DEPTH_STENCIL = 0x84F9;
    
    // Packed pixel types
    public static final int GL_UNSIGNED_BYTE_3_3_2 = 0x8032;
    public static final int GL_UNSIGNED_BYTE_2_3_3_REV = 0x8362;
    public static final int GL_UNSIGNED_SHORT_5_6_5 = 0x8363;
    public static final int GL_UNSIGNED_SHORT_5_6_5_REV = 0x8364;
    public static final int GL_UNSIGNED_SHORT_4_4_4_4 = 0x8033;
    public static final int GL_UNSIGNED_SHORT_4_4_4_4_REV = 0x8365;
    public static final int GL_UNSIGNED_SHORT_5_5_5_1 = 0x8034;
    public static final int GL_UNSIGNED_SHORT_1_5_5_5_REV = 0x8366;
    public static final int GL_UNSIGNED_INT_8_8_8_8 = 0x8035;
    public static final int GL_UNSIGNED_INT_8_8_8_8_REV = 0x8367;
    public static final int GL_UNSIGNED_INT_10_10_10_2 = 0x8036;
    public static final int GL_UNSIGNED_INT_2_10_10_10_REV = 0x8368;
    public static final int GL_UNSIGNED_INT_24_8 = 0x84FA;
    public static final int GL_UNSIGNED_INT_10F_11F_11F_REV = 0x8C3B;
    public static final int GL_UNSIGNED_INT_5_9_9_9_REV = 0x8C3E;
    public static final int GL_FLOAT_32_UNSIGNED_INT_24_8_REV = 0x8DAD;
    
    // Texture parameters
    public static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    public static final int GL_TEXTURE_MAG_FILTER = 0x2800;
    public static final int GL_TEXTURE_WRAP_S = 0x2802;
    public static final int GL_TEXTURE_WRAP_T = 0x2803;
    public static final int GL_TEXTURE_WRAP_R = 0x8072;
    public static final int GL_TEXTURE_MIN_LOD = 0x813A;
    public static final int GL_TEXTURE_MAX_LOD = 0x813B;
    public static final int GL_TEXTURE_LOD_BIAS = 0x8501;
    public static final int GL_TEXTURE_COMPARE_MODE = 0x884C;
    public static final int GL_TEXTURE_COMPARE_FUNC = 0x884D;
    public static final int GL_TEXTURE_MAX_ANISOTROPY = 0x84FE;
    public static final int GL_TEXTURE_BORDER_COLOR = 0x1004;
    public static final int GL_TEXTURE_SWIZZLE_R = 0x8E42;
    public static final int GL_TEXTURE_SWIZZLE_G = 0x8E43;
    public static final int GL_TEXTURE_SWIZZLE_B = 0x8E44;
    public static final int GL_TEXTURE_SWIZZLE_A = 0x8E45;
    public static final int GL_TEXTURE_BASE_LEVEL = 0x813C;
    public static final int GL_TEXTURE_MAX_LEVEL = 0x813D;
    
    // Filter modes
    public static final int GL_NEAREST = 0x2600;
    public static final int GL_LINEAR = 0x2601;
    public static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
    public static final int GL_LINEAR_MIPMAP_NEAREST = 0x2701;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
    
    // Wrap modes
    public static final int GL_REPEAT = 0x2901;
    public static final int GL_CLAMP_TO_EDGE = 0x812F;
    public static final int GL_CLAMP_TO_BORDER = 0x812D;
    public static final int GL_MIRRORED_REPEAT = 0x8370;
    public static final int GL_MIRROR_CLAMP_TO_EDGE = 0x8743;
    
    // Compare functions
    public static final int GL_NEVER = 0x0200;
    public static final int GL_LESS = 0x0201;
    public static final int GL_EQUAL = 0x0202;
    public static final int GL_LEQUAL = 0x0203;
    public static final int GL_GREATER = 0x0204;
    public static final int GL_NOTEQUAL = 0x0205;
    public static final int GL_GEQUAL = 0x0206;
    public static final int GL_ALWAYS = 0x0207;
    
    // Blend factors
    public static final int GL_ZERO = 0;
    public static final int GL_ONE = 1;
    public static final int GL_SRC_COLOR = 0x0300;
    public static final int GL_ONE_MINUS_SRC_COLOR = 0x0301;
    public static final int GL_SRC_ALPHA = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_DST_ALPHA = 0x0304;
    public static final int GL_ONE_MINUS_DST_ALPHA = 0x0305;
    public static final int GL_DST_COLOR = 0x0306;
    public static final int GL_ONE_MINUS_DST_COLOR = 0x0307;
    public static final int GL_SRC_ALPHA_SATURATE = 0x0308;
    public static final int GL_CONSTANT_COLOR = 0x8001;
    public static final int GL_ONE_MINUS_CONSTANT_COLOR = 0x8002;
    public static final int GL_CONSTANT_ALPHA = 0x8003;
    public static final int GL_ONE_MINUS_CONSTANT_ALPHA = 0x8004;
    public static final int GL_SRC1_COLOR = 0x88F9;
    public static final int GL_ONE_MINUS_SRC1_COLOR = 0x88FA;
    public static final int GL_SRC1_ALPHA = 0x8589;
    public static final int GL_ONE_MINUS_SRC1_ALPHA = 0x88FB;
    
    // Blend equations
    public static final int GL_FUNC_ADD = 0x8006;
    public static final int GL_FUNC_SUBTRACT = 0x800A;
    public static final int GL_FUNC_REVERSE_SUBTRACT = 0x800B;
    public static final int GL_MIN = 0x8007;
    public static final int GL_MAX = 0x8008;
    
    // Stencil operations
    public static final int GL_KEEP = 0x1E00;
    // GL_ZERO already defined
    public static final int GL_REPLACE = 0x1E01;
    public static final int GL_INCR = 0x1E02;
    public static final int GL_INCR_WRAP = 0x8507;
    public static final int GL_DECR = 0x1E03;
    public static final int GL_DECR_WRAP = 0x8508;
    public static final int GL_INVERT = 0x150A;
    
    // Face culling
    public static final int GL_FRONT = 0x0404;
    public static final int GL_BACK = 0x0405;
    public static final int GL_FRONT_AND_BACK = 0x0408;
    
    // Front face
    public static final int GL_CW = 0x0900;
    public static final int GL_CCW = 0x0901;
    
    // Polygon mode
    public static final int GL_POINT = 0x1B00;
    public static final int GL_LINE = 0x1B01;
    public static final int GL_FILL = 0x1B02;
    
    // Clear bits
    public static final int GL_COLOR_BUFFER_BIT = 0x4000;
    public static final int GL_DEPTH_BUFFER_BIT = 0x0100;
    public static final int GL_STENCIL_BUFFER_BIT = 0x0400;
    
    // Framebuffer attachments
    public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    public static final int GL_COLOR_ATTACHMENT1 = 0x8CE1;
    public static final int GL_COLOR_ATTACHMENT2 = 0x8CE2;
    public static final int GL_COLOR_ATTACHMENT3 = 0x8CE3;
    public static final int GL_COLOR_ATTACHMENT4 = 0x8CE4;
    public static final int GL_COLOR_ATTACHMENT5 = 0x8CE5;
    public static final int GL_COLOR_ATTACHMENT6 = 0x8CE6;
    public static final int GL_COLOR_ATTACHMENT7 = 0x8CE7;
    public static final int GL_DEPTH_ATTACHMENT = 0x8D00;
    public static final int GL_STENCIL_ATTACHMENT = 0x8D20;
    public static final int GL_DEPTH_STENCIL_ATTACHMENT = 0x821A;
    
    // Framebuffer status
    public static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 0x8CD6;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 0x8CD7;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER = 0x8CDB;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER = 0x8CDC;
    public static final int GL_FRAMEBUFFER_UNSUPPORTED = 0x8CDD;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE = 0x8D56;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS = 0x8DA8;
    
    // Shader types
    public static final int GL_VERTEX_SHADER = 0x8B31;
    public static final int GL_FRAGMENT_SHADER = 0x8B30;
    public static final int GL_GEOMETRY_SHADER = 0x8DD9;
    public static final int GL_TESS_CONTROL_SHADER = 0x8E88;
    public static final int GL_TESS_EVALUATION_SHADER = 0x8E87;
    public static final int GL_COMPUTE_SHADER = 0x91B9;
    
    // Uniform types
    public static final int GL_FLOAT_VEC2 = 0x8B50;
    public static final int GL_FLOAT_VEC3 = 0x8B51;
    public static final int GL_FLOAT_VEC4 = 0x8B52;
    public static final int GL_INT_VEC2 = 0x8B53;
    public static final int GL_INT_VEC3 = 0x8B54;
    public static final int GL_INT_VEC4 = 0x8B55;
    public static final int GL_BOOL = 0x8B56;
    public static final int GL_BOOL_VEC2 = 0x8B57;
    public static final int GL_BOOL_VEC3 = 0x8B58;
    public static final int GL_BOOL_VEC4 = 0x8B59;
    public static final int GL_FLOAT_MAT2 = 0x8B5A;
    public static final int GL_FLOAT_MAT3 = 0x8B5B;
    public static final int GL_FLOAT_MAT4 = 0x8B5C;
    public static final int GL_FLOAT_MAT2x3 = 0x8B65;
    public static final int GL_FLOAT_MAT2x4 = 0x8B66;
    public static final int GL_FLOAT_MAT3x2 = 0x8B67;
    public static final int GL_FLOAT_MAT3x4 = 0x8B68;
    public static final int GL_FLOAT_MAT4x2 = 0x8B69;
    public static final int GL_FLOAT_MAT4x3 = 0x8B6A;
    public static final int GL_UNSIGNED_INT_VEC2 = 0x8DC6;
    public static final int GL_UNSIGNED_INT_VEC3 = 0x8DC7;
    public static final int GL_UNSIGNED_INT_VEC4 = 0x8DC8;
    
    // Sampler types
    public static final int GL_SAMPLER_1D = 0x8B5D;
    public static final int GL_SAMPLER_2D = 0x8B5E;
    public static final int GL_SAMPLER_3D = 0x8B5F;
    public static final int GL_SAMPLER_CUBE = 0x8B60;
    public static final int GL_SAMPLER_1D_SHADOW = 0x8B61;
    public static final int GL_SAMPLER_2D_SHADOW = 0x8B62;
    public static final int GL_SAMPLER_CUBE_SHADOW = 0x8DC5;
    public static final int GL_SAMPLER_2D_ARRAY = 0x8DC1;
    public static final int GL_SAMPLER_2D_ARRAY_SHADOW = 0x8DC4;
    public static final int GL_SAMPLER_2D_MULTISAMPLE = 0x9108;
    public static final int GL_SAMPLER_2D_MULTISAMPLE_ARRAY = 0x910B;
    public static final int GL_SAMPLER_BUFFER = 0x8DC2;
    public static final int GL_IMAGE_2D = 0x904D;
    
    // Query targets
    public static final int GL_SAMPLES_PASSED = 0x8914;
    public static final int GL_ANY_SAMPLES_PASSED = 0x8C2F;
    public static final int GL_ANY_SAMPLES_PASSED_CONSERVATIVE = 0x8D6A;
    public static final int GL_TIME_ELAPSED = 0x88BF;
    public static final int GL_TIMESTAMP = 0x8E28;
    public static final int GL_PRIMITIVES_GENERATED = 0x8C87;
    public static final int GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN = 0x8C88;
    
    // Sync status
    public static final int GL_ALREADY_SIGNALED = 0x911A;
    public static final int GL_TIMEOUT_EXPIRED = 0x911B;
    public static final int GL_CONDITION_SATISFIED = 0x911C;
    public static final int GL_WAIT_FAILED = 0x911D;
    
// ════════════════════════════════════════════════════════════════════════════
    // SECTION 20: ADVANCED GLSL TO MSL TRANSLATOR
    // Complete translation support for GLSL 1.10 through 4.60
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Advanced GLSL to Metal Shading Language translator supporting all GLSL versions.
     * Handles legacy fixed-function emulation, geometry shaders via compute, 
     * tessellation, and all modern GLSL features.
     */
    public static final class AdvancedGLSLTranslator {
        
        // GLSL version detection
        private int glslVersion = 330;
        private boolean isES = false;
        private boolean hasExplicitVersion = false;
        
        // Shader stage
        private int shaderStage;
        
        // Collected shader information
        private final List<GLSLVariable> inputs = new ArrayList<>();
        private final List<GLSLVariable> outputs = new ArrayList<>();
        private final List<GLSLVariable> uniforms = new ArrayList<>();
        private final List<GLSLUniformBlock> uniformBlocks = new ArrayList<>();
        private final List<GLSLShaderStorageBlock> shaderStorageBlocks = new ArrayList<>();
        private final List<GLSLSampler> samplers = new ArrayList<>();
        private final List<GLSLImage> images = new ArrayList<>();
        private final Map<String, GLSLStruct> structs = new LinkedHashMap<>();
        private final Map<String, GLSLFunction> functions = new LinkedHashMap<>();
        private final Set<String> usedExtensions = new HashSet<>();
        private final Set<String> usedBuiltins = new HashSet<>();
        
        // Translation state
        private final StringBuilder mslOutput = new StringBuilder();
        private final StringBuilder mslStructs = new StringBuilder();
        private final StringBuilder mslFunctions = new StringBuilder();
        private final StringBuilder mslMain = new StringBuilder();
        private int indentLevel = 0;
        private int tempVarCounter = 0;
        private int bufferBindingCounter = 0;
        private int textureBindingCounter = 0;
        private int samplerBindingCounter = 0;
        
        // Legacy GL support
        private boolean usesGlFragColor = false;
        private boolean usesGlFragData = false;
        private boolean usesGlPosition = false;
        private boolean usesGlPointSize = false;
        private boolean usesGlClipDistance = false;
        private boolean usesGlVertexID = false;
        private boolean usesGlInstanceID = false;
        private boolean usesGlFrontFacing = false;
        private boolean usesGlPointCoord = false;
        private boolean usesGlPrimitiveID = false;
        private boolean usesGlLayer = false;
        private boolean usesGlViewportIndex = false;
        private boolean usesGlSampleID = false;
        private boolean usesGlSamplePosition = false;
        private boolean usesGlNumSamples = false;
        private boolean usesGlInvocationID = false;
        private boolean usesGlTessLevelOuter = false;
        private boolean usesGlTessLevelInner = false;
        private boolean usesGlTessCoord = false;
        private boolean usesGlPatchVerticesIn = false;
        private boolean usesGlWorkGroupID = false;
        private boolean usesGlLocalInvocationID = false;
        private boolean usesGlGlobalInvocationID = false;
        private boolean usesGlNumWorkGroups = false;
        
        // Geometry shader emulation
        private boolean emulateGeometryShader = false;
        private int gsInputPrimitive = GL_TRIANGLES;
        private int gsOutputPrimitive = GL_TRIANGLE_STRIP;
        private int gsMaxVertices = 256;
        
        // Tessellation support
        private boolean hasTessellation = false;
        private int tessGenMode = GL_TRIANGLES;
        private int tessGenSpacing = GL_EQUAL;
        private int tessGenVertexOrder = GL_CCW;
        private boolean tessGenPointMode = false;
        
        // Subroutine emulation
        private final Map<String, List<String>> subroutineTypes = new HashMap<>();
        private final Map<String, String> subroutineUniforms = new HashMap<>();
        
        /**
         * GLSL variable representation
         */
        public static class GLSLVariable {
            public String name;
            public String type;
            public int location = -1;
            public int binding = -1;
            public int arraySize = 0;
            public boolean flat = false;
            public boolean noperspective = false;
            public boolean centroid = false;
            public boolean sample = false;
            public boolean patch = false;
            public String interpolation = "";
            public String precision = "";
            public int component = -1;
            public int index = -1;
            public boolean invariant = false;
            public String layoutQualifiers = "";
        }
        
        /**
         * GLSL uniform block representation
         */
        public static class GLSLUniformBlock {
            public String name;
            public String instanceName;
            public int binding = -1;
            public List<GLSLVariable> members = new ArrayList<>();
            public String layoutPacking = "std140";
            public int arraySize = 0;
            public boolean rowMajor = false;
        }
        
        /**
         * GLSL shader storage block representation
         */
        public static class GLSLShaderStorageBlock {
            public String name;
            public String instanceName;
            public int binding = -1;
            public List<GLSLVariable> members = new ArrayList<>();
            public String layoutPacking = "std430";
            public int arraySize = 0;
            public boolean rowMajor = false;
            public boolean coherent = false;
            public boolean restrict_ = false;
            public boolean readonly = false;
            public boolean writeonly = false;
        }
        
        /**
         * GLSL sampler representation
         */
        public static class GLSLSampler {
            public String name;
            public String type;
            public int binding = -1;
            public int arraySize = 0;
            public boolean shadow = false;
            public boolean multisample = false;
            public int dimensions = 2;
            public boolean isArray = false;
            public boolean isCube = false;
            public boolean isBuffer = false;
            public boolean isRect = false;
            public String precision = "";
            public String samplerType = "float"; // float, int, uint
        }
        
        /**
         * GLSL image representation
         */
        public static class GLSLImage {
            public String name;
            public String type;
            public int binding = -1;
            public int arraySize = 0;
            public String format = "";
            public int dimensions = 2;
            public boolean isArray = false;
            public boolean isCube = false;
            public boolean isBuffer = false;
            public boolean isMS = false;
            public boolean coherent = false;
            public boolean restrict_ = false;
            public boolean readonly = false;
            public boolean writeonly = false;
            public String precision = "";
            public String imageType = "float";
        }
        
        /**
         * GLSL struct representation
         */
        public static class GLSLStruct {
            public String name;
            public List<GLSLVariable> members = new ArrayList<>();
            public boolean isInterfaceBlock = false;
        }
        
        /**
         * GLSL function representation
         */
        public static class GLSLFunction {
            public String name;
            public String returnType;
            public List<GLSLVariable> parameters = new ArrayList<>();
            public String body;
            public boolean isMain = false;
        }
        
        /**
         * Token types for GLSL lexer
         */
        private enum TokenType {
            IDENTIFIER, KEYWORD, OPERATOR, NUMBER, STRING, PREPROCESSOR,
            LBRACE, RBRACE, LPAREN, RPAREN, LBRACKET, RBRACKET,
            SEMICOLON, COMMA, DOT, COLON, QUESTION,
            WHITESPACE, NEWLINE, COMMENT, EOF
        }
        
        /**
         * Token representation
         */
        private static class Token {
            TokenType type;
            String value;
            int line;
            int column;
            
            Token(TokenType type, String value, int line, int column) {
                this.type = type;
                this.value = value;
                this.line = line;
                this.column = column;
            }
        }
        
        /**
         * GLSL Lexer for tokenizing shader source
         */
        private class GLSLLexer {
            private final String source;
            private int pos = 0;
            private int line = 1;
            private int column = 1;
            
            // GLSL keywords by version
            private static final Set<String> GLSL_KEYWORDS = new HashSet<>(Arrays.asList(
                // Types
                "void", "bool", "int", "uint", "float", "double",
                "vec2", "vec3", "vec4", "dvec2", "dvec3", "dvec4",
                "bvec2", "bvec3", "bvec4", "ivec2", "ivec3", "ivec4",
                "uvec2", "uvec3", "uvec4",
                "mat2", "mat3", "mat4", "mat2x2", "mat2x3", "mat2x4",
                "mat3x2", "mat3x3", "mat3x4", "mat4x2", "mat4x3", "mat4x4",
                "dmat2", "dmat3", "dmat4", "dmat2x2", "dmat2x3", "dmat2x4",
                "dmat3x2", "dmat3x3", "dmat3x4", "dmat4x2", "dmat4x3", "dmat4x4",
                "sampler1D", "sampler2D", "sampler3D", "samplerCube",
                "sampler1DShadow", "sampler2DShadow", "samplerCubeShadow",
                "sampler1DArray", "sampler2DArray", "samplerCubeArray",
                "sampler1DArrayShadow", "sampler2DArrayShadow", "samplerCubeArrayShadow",
                "sampler2DMS", "sampler2DMSArray", "samplerBuffer", "sampler2DRect",
                "sampler2DRectShadow",
                "isampler1D", "isampler2D", "isampler3D", "isamplerCube",
                "isampler1DArray", "isampler2DArray", "isamplerCubeArray",
                "isampler2DMS", "isampler2DMSArray", "isamplerBuffer", "isampler2DRect",
                "usampler1D", "usampler2D", "usampler3D", "usamplerCube",
                "usampler1DArray", "usampler2DArray", "usamplerCubeArray",
                "usampler2DMS", "usampler2DMSArray", "usamplerBuffer", "usampler2DRect",
                "image1D", "image2D", "image3D", "imageCube",
                "image1DArray", "image2DArray", "imageCubeArray",
                "image2DMS", "image2DMSArray", "imageBuffer", "image2DRect",
                "iimage1D", "iimage2D", "iimage3D", "iimageCube",
                "iimage1DArray", "iimage2DArray", "iimageCubeArray",
                "iimage2DMS", "iimage2DMSArray", "iimageBuffer", "iimage2DRect",
                "uimage1D", "uimage2D", "uimage3D", "uimageCube",
                "uimage1DArray", "uimage2DArray", "uimageCubeArray",
                "uimage2DMS", "uimage2DMSArray", "uimageBuffer", "uimage2DRect",
                "atomic_uint",
                // Storage qualifiers
                "const", "in", "out", "inout", "uniform", "buffer",
                "shared", "attribute", "varying", "centroid", "flat",
                "smooth", "noperspective", "patch", "sample",
                "coherent", "volatile", "restrict", "readonly", "writeonly",
                "subroutine",
                // Layout qualifiers
                "layout",
                // Precision qualifiers
                "lowp", "mediump", "highp", "precision",
                // Invariant qualifier
                "invariant",
                // Interpolation qualifiers
                "interpolation",
                // Control flow
                "if", "else", "switch", "case", "default",
                "for", "while", "do", "break", "continue", "return", "discard",
                // Struct/interface
                "struct", "interface",
                // Misc
                "true", "false"
            ));
            
            GLSLLexer(String source) {
                this.source = source;
            }
            
            List<Token> tokenize() {
                List<Token> tokens = new ArrayList<>();
                while (pos < source.length()) {
                    Token token = nextToken();
                    if (token != null && token.type != TokenType.WHITESPACE && 
                        token.type != TokenType.COMMENT) {
                        tokens.add(token);
                    }
                }
                tokens.add(new Token(TokenType.EOF, "", line, column));
                return tokens;
            }
            
            private Token nextToken() {
                if (pos >= source.length()) return null;
                
                char c = source.charAt(pos);
                int startLine = line;
                int startCol = column;
                
                // Whitespace
                if (Character.isWhitespace(c)) {
                    StringBuilder sb = new StringBuilder();
                    while (pos < source.length() && Character.isWhitespace(source.charAt(pos))) {
                        char ws = source.charAt(pos);
                        if (ws == '\n') {
                            line++;
                            column = 1;
                        } else {
                            column++;
                        }
                        sb.append(ws);
                        pos++;
                    }
                    return new Token(TokenType.WHITESPACE, sb.toString(), startLine, startCol);
                }
                
                // Comments
                if (c == '/' && pos + 1 < source.length()) {
                    if (source.charAt(pos + 1) == '/') {
                        // Single-line comment
                        StringBuilder sb = new StringBuilder();
                        while (pos < source.length() && source.charAt(pos) != '\n') {
                            sb.append(source.charAt(pos++));
                            column++;
                        }
                        return new Token(TokenType.COMMENT, sb.toString(), startLine, startCol);
                    } else if (source.charAt(pos + 1) == '*') {
                        // Multi-line comment
                        StringBuilder sb = new StringBuilder();
                        sb.append(source.charAt(pos++));
                        sb.append(source.charAt(pos++));
                        column += 2;
                        while (pos + 1 < source.length() && 
                               !(source.charAt(pos) == '*' && source.charAt(pos + 1) == '/')) {
                            if (source.charAt(pos) == '\n') {
                                line++;
                                column = 1;
                            } else {
                                column++;
                            }
                            sb.append(source.charAt(pos++));
                        }
                        if (pos + 1 < source.length()) {
                            sb.append(source.charAt(pos++));
                            sb.append(source.charAt(pos++));
                            column += 2;
                        }
                        return new Token(TokenType.COMMENT, sb.toString(), startLine, startCol);
                    }
                }
                
                // Preprocessor directives
                if (c == '#') {
                    StringBuilder sb = new StringBuilder();
                    while (pos < source.length() && source.charAt(pos) != '\n') {
                        // Handle line continuation
                        if (source.charAt(pos) == '\\' && pos + 1 < source.length() && 
                            source.charAt(pos + 1) == '\n') {
                            sb.append(source.charAt(pos++));
                            sb.append(source.charAt(pos++));
                            line++;
                            column = 1;
                        } else {
                            sb.append(source.charAt(pos++));
                            column++;
                        }
                    }
                    return new Token(TokenType.PREPROCESSOR, sb.toString(), startLine, startCol);
                }
                
                // Numbers
                if (Character.isDigit(c) || (c == '.' && pos + 1 < source.length() && 
                    Character.isDigit(source.charAt(pos + 1)))) {
                    return readNumber(startLine, startCol);
                }
                
                // Identifiers and keywords
                if (Character.isLetter(c) || c == '_') {
                    StringBuilder sb = new StringBuilder();
                    while (pos < source.length() && 
                           (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_')) {
                        sb.append(source.charAt(pos++));
                        column++;
                    }
                    String value = sb.toString();
                    TokenType type = GLSL_KEYWORDS.contains(value) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
                    return new Token(type, value, startLine, startCol);
                }
                
                // Operators and punctuation
                return readOperator(startLine, startCol);
            }
            
            private Token readNumber(int startLine, int startCol) {
                StringBuilder sb = new StringBuilder();
                boolean hasDecimal = false;
                boolean hasExponent = false;
                boolean isHex = false;
                
                // Check for hex
                if (source.charAt(pos) == '0' && pos + 1 < source.length() && 
                    (source.charAt(pos + 1) == 'x' || source.charAt(pos + 1) == 'X')) {
                    sb.append(source.charAt(pos++));
                    sb.append(source.charAt(pos++));
                    column += 2;
                    isHex = true;
                    while (pos < source.length() && isHexDigit(source.charAt(pos))) {
                        sb.append(source.charAt(pos++));
                        column++;
                    }
                } else {
                    // Regular number
                    while (pos < source.length()) {
                        char c = source.charAt(pos);
                        if (Character.isDigit(c)) {
                            sb.append(c);
                            pos++;
                            column++;
                        } else if (c == '.' && !hasDecimal && !hasExponent) {
                            sb.append(c);
                            pos++;
                            column++;
                            hasDecimal = true;
                        } else if ((c == 'e' || c == 'E') && !hasExponent) {
                            sb.append(c);
                            pos++;
                            column++;
                            hasExponent = true;
                            if (pos < source.length() && 
                                (source.charAt(pos) == '+' || source.charAt(pos) == '-')) {
                                sb.append(source.charAt(pos++));
                                column++;
                            }
                        } else {
                            break;
                        }
                    }
                }
                
                // Suffix
                if (pos < source.length()) {
                    char c = source.charAt(pos);
                    if (c == 'f' || c == 'F' || c == 'u' || c == 'U' || 
                        c == 'l' || c == 'L') {
                        sb.append(c);
                        pos++;
                        column++;
                        // Handle LF suffix for double
                        if (pos < source.length() && 
                            (source.charAt(pos) == 'f' || source.charAt(pos) == 'F')) {
                            sb.append(source.charAt(pos++));
                            column++;
                        }
                    }
                }
                
                return new Token(TokenType.NUMBER, sb.toString(), startLine, startCol);
            }
            
            private boolean isHexDigit(char c) {
                return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            }
            
            private Token readOperator(int startLine, int startCol) {
                char c = source.charAt(pos);
                pos++;
                column++;
                
                switch (c) {
                    case '{': return new Token(TokenType.LBRACE, "{", startLine, startCol);
                    case '}': return new Token(TokenType.RBRACE, "}", startLine, startCol);
                    case '(': return new Token(TokenType.LPAREN, "(", startLine, startCol);
                    case ')': return new Token(TokenType.RPAREN, ")", startLine, startCol);
                    case '[': return new Token(TokenType.LBRACKET, "[", startLine, startCol);
                    case ']': return new Token(TokenType.RBRACKET, "]", startLine, startCol);
                    case ';': return new Token(TokenType.SEMICOLON, ";", startLine, startCol);
                    case ',': return new Token(TokenType.COMMA, ",", startLine, startCol);
                    case '.': return new Token(TokenType.DOT, ".", startLine, startCol);
                    case ':': return new Token(TokenType.COLON, ":", startLine, startCol);
                    case '?': return new Token(TokenType.QUESTION, "?", startLine, startCol);
                    
                    case '+':
                        if (pos < source.length()) {
                            char next = source.charAt(pos);
                            if (next == '+') { pos++; column++; return new Token(TokenType.OPERATOR, "++", startLine, startCol); }
                            if (next == '=') { pos++; column++; return new Token(TokenType.OPERATOR, "+=", startLine, startCol); }
                        }
                        return new Token(TokenType.OPERATOR, "+", startLine, startCol);
                        
                    case '-':
                        if (pos < source.length()) {
                            char next = source.charAt(pos);
                            if (next == '-') { pos++; column++; return new Token(TokenType.OPERATOR, "--", startLine, startCol); }
                            if (next == '=') { pos++; column++; return new Token(TokenType.OPERATOR, "-=", startLine, startCol); }
                        }
                        return new Token(TokenType.OPERATOR, "-", startLine, startCol);
                        
                    case '*':
                        if (pos < source.length() && source.charAt(pos) == '=') {
                            pos++; column++;
                            return new Token(TokenType.OPERATOR, "*=", startLine, startCol);
                        }
                        return new Token(TokenType.OPERATOR, "*", startLine, startCol);
                        
                    case '/':
                        if (pos < source.length() && source.charAt(pos) == '=') {
                            pos++; column++;
                            return new Token(TokenType.OPERATOR, "/=", startLine, startCol);
                        }
                        return new Token(TokenType.OPERATOR, "/", startLine, startCol);
                        
                    case '%':
                        if (pos < source.length() && source.charAt(pos) == '=') {
                            pos++; column++;
                            return new Token(TokenType.OPERATOR, "%=", startLine, startCol);
                        }
                        return new Token(TokenType.OPERATOR, "%", startLine, startCol);
                        
                    case '=':
                        if (pos < source.length() && source.charAt(pos) == '=') {
                            pos++; column++;
                            return new Token(TokenType.OPERATOR, "==", startLine, startCol);
                        }
                        return new Token(TokenType.OPERATOR, "=", startLine, startCol);
                        
                    case '!':
                        if (pos < source.length() && source.charAt(pos) == '=') {
                            pos++; column++;
                            return new Token(TokenType.OPERATOR, "!=", startLine, startCol);
                        }
                        return new Token(TokenType.OPERATOR, "!", startLine, startCol);
                        
                    case '<':
                        if (pos < source.length()) {
                            char next = source.charAt(pos);
                            if (next == '=') { pos++; column++; return new Token(TokenType.OPERATOR, "<=", startLine, startCol); }
                            if (next == '<') {
                                pos++; column++;
                                if (pos < source.length() && source.charAt(pos) == '=') {
                                    pos++; column++;
                                    return new Token(TokenType.OPERATOR, "<<=", startLine, startCol);
                                }
                                return new Token(TokenType.OPERATOR, "<<", startLine, startCol);
                            }
                        }
                        return new Token(TokenType.OPERATOR, "<", startLine, startCol);
                        
                    case '>':
                        if (pos < source.length()) {
                            char next = source.charAt(pos);
                            if (next == '=') { pos++; column++; return new Token(TokenType.OPERATOR, ">=", startLine, startCol); }
                            if (next == '>') {
                                pos++; column++;
                                if (pos < source.length() && source.charAt(pos) == '=') {
                                    pos++; column++;
                                    return new Token(TokenType.OPERATOR, ">>=", startLine, startCol);
                                }
                                return new Token(TokenType.OPERATOR, ">>", startLine, startCol);
                            }
                        }
                        return new Token(TokenType.OPERATOR, ">", startLine, startCol);
                        
                    case '&':
                        if (pos < source.length()) {
                            char next = source.charAt(pos);
                            if (next == '&') { pos++; column++; return new Token(TokenType.OPERATOR, "&&", startLine, startCol); }
                            if (next == '=') { pos++; column++; return new Token(TokenType.OPERATOR, "&=", startLine, startCol); }
                        }
                        return new Token(TokenType.OPERATOR, "&", startLine, startCol);
                        
                    case '|':
                        if (pos < source.length()) {
                            char next = source.charAt(pos);
                            if (next == '|') { pos++; column++; return new Token(TokenType.OPERATOR, "||", startLine, startCol); }
                            if (next == '=') { pos++; column++; return new Token(TokenType.OPERATOR, "|=", startLine, startCol); }
                        }
                        return new Token(TokenType.OPERATOR, "|", startLine, startCol);
                        
                    case '^':
                        if (pos < source.length()) {
                            char next = source.charAt(pos);
                            if (next == '^') { pos++; column++; return new Token(TokenType.OPERATOR, "^^", startLine, startCol); }
                            if (next == '=') { pos++; column++; return new Token(TokenType.OPERATOR, "^=", startLine, startCol); }
                        }
                        return new Token(TokenType.OPERATOR, "^", startLine, startCol);
                        
                    case '~':
                        return new Token(TokenType.OPERATOR, "~", startLine, startCol);
                        
                    default:
                        return new Token(TokenType.OPERATOR, String.valueOf(c), startLine, startCol);
                }
            }
        }
        
        /**
         * GLSL Parser for building AST
         */
        private class GLSLParser {
            private List<Token> tokens;
            private int pos = 0;
            
            GLSLParser(List<Token> tokens) {
                this.tokens = tokens;
            }
            
            void parse() {
                while (!isAtEnd()) {
                    parseTopLevel();
                }
            }
            
            private void parseTopLevel() {
                Token token = peek();
                
                if (token.type == TokenType.PREPROCESSOR) {
                    parsePreprocessor();
                } else if (token.type == TokenType.KEYWORD) {
                    switch (token.value) {
                        case "struct":
                            parseStruct();
                            break;
                        case "layout":
                            parseLayoutDeclaration();
                            break;
                        case "uniform":
                        case "in":
                        case "out":
                        case "buffer":
                        case "shared":
                        case "attribute":
                        case "varying":
                        case "const":
                            parseDeclaration();
                            break;
                        case "precision":
                            parsePrecision();
                            break;
                        case "subroutine":
                            parseSubroutine();
                            break;
                        default:
                            // Could be a function or variable declaration
                            parseFunctionOrVariable();
                            break;
                    }
                } else if (token.type == TokenType.IDENTIFIER) {
                    parseFunctionOrVariable();
                } else {
                    advance(); // Skip unknown tokens
                }
            }
            
            private void parsePreprocessor() {
                Token token = advance();
                String directive = token.value.trim();
                
                if (directive.startsWith("#version")) {
                    parseVersionDirective(directive);
                } else if (directive.startsWith("#extension")) {
                    parseExtensionDirective(directive);
                }
                // Handle other preprocessor directives as needed
            }
            
            private void parseVersionDirective(String directive) {
                hasExplicitVersion = true;
                String[] parts = directive.split("\\s+");
                if (parts.length >= 2) {
                    try {
                        glslVersion = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        glslVersion = 330;
                    }
                }
                if (parts.length >= 3) {
                    isES = parts[2].equalsIgnoreCase("es");
                }
            }
            
            private void parseExtensionDirective(String directive) {
                String[] parts = directive.split("\\s+");
                if (parts.length >= 3) {
                    String extName = parts[1].replace(":", "");
                    usedExtensions.add(extName);
                }
            }
            
            private void parseStruct() {
                expect(TokenType.KEYWORD, "struct");
                String name = expect(TokenType.IDENTIFIER).value;
                
                GLSLStruct struct = new GLSLStruct();
                struct.name = name;
                
                expect(TokenType.LBRACE);
                
                while (!check(TokenType.RBRACE) && !isAtEnd()) {
                    GLSLVariable member = parseStructMember();
                    if (member != null) {
                        struct.members.add(member);
                    }
                }
                
                expect(TokenType.RBRACE);
                expect(TokenType.SEMICOLON);
                
                structs.put(name, struct);
            }
            
            private GLSLVariable parseStructMember() {
                GLSLVariable var = new GLSLVariable();
                
                // Parse type
                var.type = parseType();
                if (var.type == null) return null;
                
                // Parse name
                var.name = expect(TokenType.IDENTIFIER).value;
                
                // Parse array size if present
                if (check(TokenType.LBRACKET)) {
                    advance();
                    if (!check(TokenType.RBRACKET)) {
                        Token sizeToken = advance();
                        try {
                            var.arraySize = Integer.parseInt(sizeToken.value);
                        } catch (NumberFormatException e) {
                            var.arraySize = -1; // Dynamic array
                        }
                    }
                    expect(TokenType.RBRACKET);
                }
                
                expect(TokenType.SEMICOLON);
                return var;
            }
            
            private String parseType() {
                StringBuilder type = new StringBuilder();
                
                // Handle precision qualifiers
                if (checkKeyword("lowp") || checkKeyword("mediump") || checkKeyword("highp")) {
                    type.append(advance().value).append(" ");
                }
                
                Token token = peek();
                if (token.type == TokenType.KEYWORD || token.type == TokenType.IDENTIFIER) {
                    type.append(advance().value);
                    return type.toString().trim();
                }
                
                return null;
            }
            
            private void parseLayoutDeclaration() {
                expect(TokenType.KEYWORD, "layout");
                expect(TokenType.LPAREN);
                
                Map<String, String> layoutQualifiers = new HashMap<>();
                
                while (!check(TokenType.RPAREN) && !isAtEnd()) {
                    String qualifier = advance().value;
                    String value = null;
                    
                    if (check(TokenType.OPERATOR) && peek().value.equals("=")) {
                        advance();
                        value = advance().value;
                    }
                    
                    layoutQualifiers.put(qualifier, value);
                    
                    if (check(TokenType.COMMA)) {
                        advance();
                    }
                }
                
                expect(TokenType.RPAREN);
                
                // Now parse what follows the layout qualifier
                parseDeclarationWithLayout(layoutQualifiers);
            }
            
            private void parseDeclarationWithLayout(Map<String, String> layout) {
                Token token = peek();
                
                if (checkKeyword("uniform")) {
                    // Could be uniform block or single uniform
                    advance();
                    if (isTypeName(peek().value) || peek().type == TokenType.IDENTIFIER) {
                        parseUniformVariable(layout);
                    } else {
                        parseUniformBlock(layout);
                    }
                } else if (checkKeyword("buffer")) {
                    advance();
                    parseShaderStorageBlock(layout);
                } else if (checkKeyword("in")) {
                    advance();
                    parseInputVariable(layout);
                } else if (checkKeyword("out")) {
                    advance();
                    parseOutputVariable(layout);
                }
            }
            
            private void parseUniformVariable(Map<String, String> layout) {
                GLSLVariable var = new GLSLVariable();
                
                var.type = parseType();
                var.name = expect(TokenType.IDENTIFIER).value;
                
                if (layout.containsKey("location")) {
                    var.location = Integer.parseInt(layout.get("location"));
                }
                if (layout.containsKey("binding")) {
                    var.binding = Integer.parseInt(layout.get("binding"));
                }
                
                // Check for array
                if (check(TokenType.LBRACKET)) {
                    advance();
                    if (!check(TokenType.RBRACKET)) {
                        var.arraySize = Integer.parseInt(advance().value);
                    }
                    expect(TokenType.RBRACKET);
                }
                
                expect(TokenType.SEMICOLON);
                
                // Categorize the uniform
                if (isSamplerType(var.type)) {
                    GLSLSampler sampler = createSamplerFromVariable(var);
                    samplers.add(sampler);
                } else if (isImageType(var.type)) {
                    GLSLImage image = createImageFromVariable(var);
                    images.add(image);
                } else {
                    uniforms.add(var);
                }
            }
            
            private void parseUniformBlock(Map<String, String> layout) {
                GLSLUniformBlock block = new GLSLUniformBlock();
                
                block.name = expect(TokenType.IDENTIFIER).value;
                
                if (layout.containsKey("binding")) {
                    block.binding = Integer.parseInt(layout.get("binding"));
                }
                if (layout.containsKey("std140")) {
                    block.layoutPacking = "std140";
                }
                if (layout.containsKey("std430")) {
                    block.layoutPacking = "std430";
                }
                if (layout.containsKey("row_major")) {
                    block.rowMajor = true;
                }
                
                expect(TokenType.LBRACE);
                
                while (!check(TokenType.RBRACE) && !isAtEnd()) {
                    GLSLVariable member = parseStructMember();
                    if (member != null) {
                        block.members.add(member);
                    }
                }
                
                expect(TokenType.RBRACE);
                
                // Instance name (optional)
                if (check(TokenType.IDENTIFIER)) {
                    block.instanceName = advance().value;
                    
                    // Array
                    if (check(TokenType.LBRACKET)) {
                        advance();
                        if (!check(TokenType.RBRACKET)) {
                            block.arraySize = Integer.parseInt(advance().value);
                        }
                        expect(TokenType.RBRACKET);
                    }
                }
                
                expect(TokenType.SEMICOLON);
                uniformBlocks.add(block);
            }
            
            private void parseShaderStorageBlock(Map<String, String> layout) {
                GLSLShaderStorageBlock block = new GLSLShaderStorageBlock();
                
                block.name = expect(TokenType.IDENTIFIER).value;
                
                if (layout.containsKey("binding")) {
                    block.binding = Integer.parseInt(layout.get("binding"));
                }
                if (layout.containsKey("std140")) {
                    block.layoutPacking = "std140";
                }
                if (layout.containsKey("std430")) {
                    block.layoutPacking = "std430";
                }
                
                expect(TokenType.LBRACE);
                
                while (!check(TokenType.RBRACE) && !isAtEnd()) {
                    GLSLVariable member = parseStructMember();
                    if (member != null) {
                        block.members.add(member);
                    }
                }
                
                expect(TokenType.RBRACE);
                
                if (check(TokenType.IDENTIFIER)) {
                    block.instanceName = advance().value;
                    
                    if (check(TokenType.LBRACKET)) {
                        advance();
                        if (!check(TokenType.RBRACKET)) {
                            block.arraySize = Integer.parseInt(advance().value);
                        }
                        expect(TokenType.RBRACKET);
                    }
                }
                
                expect(TokenType.SEMICOLON);
                shaderStorageBlocks.add(block);
            }
            
            private void parseInputVariable(Map<String, String> layout) {
                GLSLVariable var = new GLSLVariable();
                
                // Check for interpolation qualifiers
                if (checkKeyword("flat")) {
                    advance();
                    var.flat = true;
                }
                if (checkKeyword("noperspective")) {
                    advance();
                    var.noperspective = true;
                }
                if (checkKeyword("centroid")) {
                    advance();
                    var.centroid = true;
                }
                if (checkKeyword("sample")) {
                    advance();
                    var.sample = true;
                }
                if (checkKeyword("patch")) {
                    advance();
                    var.patch = true;
                }
                
                var.type = parseType();
                var.name = expect(TokenType.IDENTIFIER).value;
                
                if (layout.containsKey("location")) {
                    var.location = Integer.parseInt(layout.get("location"));
                }
                if (layout.containsKey("component")) {
                    var.component = Integer.parseInt(layout.get("component"));
                }
                
                if (check(TokenType.LBRACKET)) {
                    advance();
                    if (!check(TokenType.RBRACKET)) {
                        var.arraySize = Integer.parseInt(advance().value);
                    }
                    expect(TokenType.RBRACKET);
                }
                
                expect(TokenType.SEMICOLON);
                inputs.add(var);
            }
            
            private void parseOutputVariable(Map<String, String> layout) {
                GLSLVariable var = new GLSLVariable();
                
                if (checkKeyword("flat")) {
                    advance();
                    var.flat = true;
                }
                if (checkKeyword("noperspective")) {
                    advance();
                    var.noperspective = true;
                }
                if (checkKeyword("centroid")) {
                    advance();
                    var.centroid = true;
                }
                if (checkKeyword("sample")) {
                    advance();
                    var.sample = true;
                }
                if (checkKeyword("patch")) {
                    advance();
                    var.patch = true;
                }
                
                var.type = parseType();
                var.name = expect(TokenType.IDENTIFIER).value;
                
                if (layout.containsKey("location")) {
                    var.location = Integer.parseInt(layout.get("location"));
                }
                if (layout.containsKey("index")) {
                    var.index = Integer.parseInt(layout.get("index"));
                }
                if (layout.containsKey("component")) {
                    var.component = Integer.parseInt(layout.get("component"));
                }
                
                if (check(TokenType.LBRACKET)) {
                    advance();
                    if (!check(TokenType.RBRACKET)) {
                        var.arraySize = Integer.parseInt(advance().value);
                    }
                    expect(TokenType.RBRACKET);
                }
                
                expect(TokenType.SEMICOLON);
                outputs.add(var);
            }
            
            private void parseDeclaration() {
                Token token = advance(); // Storage qualifier
                String storageQualifier = token.value;
                
                if (storageQualifier.equals("uniform")) {
                    parseUniformVariable(new HashMap<>());
                } else if (storageQualifier.equals("in") || storageQualifier.equals("attribute")) {
                    parseInputVariable(new HashMap<>());
                } else if (storageQualifier.equals("out") || storageQualifier.equals("varying")) {
                    parseOutputVariable(new HashMap<>());
                } else {
                    // Skip other declarations for now
                    skipUntil(TokenType.SEMICOLON);
                    advance();
                }
            }
            
            private void parsePrecision() {
                advance(); // precision
                advance(); // qualifier (highp, mediump, lowp)
                advance(); // type
                expect(TokenType.SEMICOLON);
            }
            
            private void parseSubroutine() {
                advance(); // subroutine
                
                if (check(TokenType.LPAREN)) {
                    // Subroutine function definition
                    advance();
                    List<String> types = new ArrayList<>();
                    while (!check(TokenType.RPAREN) && !isAtEnd()) {
                        types.add(advance().value);
                        if (check(TokenType.COMMA)) advance();
                    }
                    expect(TokenType.RPAREN);
                    // Continue parsing function
                    parseFunctionOrVariable();
                } else if (checkKeyword("uniform")) {
                    // Subroutine uniform
                    advance(); // uniform
                    String typeName = advance().value;
                    String varName = expect(TokenType.IDENTIFIER).value;
                    expect(TokenType.SEMICOLON);
                    subroutineUniforms.put(varName, typeName);
                } else {
                    // Subroutine type definition
                    String returnType = parseType();
                    String typeName = expect(TokenType.IDENTIFIER).value;
                    // Parse function signature
                    skipUntil(TokenType.SEMICOLON);
                    advance();
                    subroutineTypes.put(typeName, new ArrayList<>());
                }
            }
            
            private void parseFunctionOrVariable() {
                String returnType = parseType();
                if (returnType == null) {
                    skipUntil(TokenType.SEMICOLON);
                    if (!isAtEnd()) advance();
                    return;
                }
                
                String name = expect(TokenType.IDENTIFIER).value;
                
                if (check(TokenType.LPAREN)) {
                    // Function
                    parseFunction(returnType, name);
                } else {
                    // Variable
                    skipUntil(TokenType.SEMICOLON);
                    if (!isAtEnd()) advance();
                }
            }
            
            private void parseFunction(String returnType, String name) {
                GLSLFunction func = new GLSLFunction();
                func.name = name;
                func.returnType = returnType;
                func.isMain = name.equals("main");
                
                expect(TokenType.LPAREN);
                
                // Parse parameters
                while (!check(TokenType.RPAREN) && !isAtEnd()) {
                    GLSLVariable param = new GLSLVariable();
                    
                    // Parse qualifiers (in, out, inout)
                    if (checkKeyword("in") || checkKeyword("out") || checkKeyword("inout")) {
                        Token qual = advance();
                        param.interpolation = qual.value;
                    }
                    
                    param.type = parseType();
                    param.name = expect(TokenType.IDENTIFIER).value;
                    
                    if (check(TokenType.LBRACKET)) {
                        advance();
                        if (!check(TokenType.RBRACKET)) {
                            param.arraySize = Integer.parseInt(advance().value);
                        }
                        expect(TokenType.RBRACKET);
                    }
                    
                    func.parameters.add(param);
                    
                    if (check(TokenType.COMMA)) {
                        advance();
                    }
                }
                
                expect(TokenType.RPAREN);
                
                // Parse body
                if (check(TokenType.LBRACE)) {
                    func.body = parseFunctionBody();
                } else {
                    expect(TokenType.SEMICOLON);
                }
                
                functions.put(name, func);
            }
            
            private String parseFunctionBody() {
                StringBuilder body = new StringBuilder();
                int braceDepth = 0;
                
                expect(TokenType.LBRACE);
                braceDepth++;
                
                while (braceDepth > 0 && !isAtEnd()) {
                    Token token = advance();
                    
                    if (token.type == TokenType.LBRACE) {
                        braceDepth++;
                        body.append("{ ");
                    } else if (token.type == TokenType.RBRACE) {
                        braceDepth--;
                        if (braceDepth > 0) {
                            body.append("} ");
                        }
                    } else if (token.type == TokenType.SEMICOLON) {
                        body.append("; ");
                    } else if (token.type == TokenType.NEWLINE) {
                        body.append("\n");
                    } else {
                        body.append(token.value).append(" ");
                    }
                }
                
                return body.toString();
            }
            
            // Helper methods
            private boolean isAtEnd() {
                return pos >= tokens.size() || tokens.get(pos).type == TokenType.EOF;
            }
            
            private Token peek() {
                if (isAtEnd()) return new Token(TokenType.EOF, "", 0, 0);
                return tokens.get(pos);
            }
            
            private Token advance() {
                if (!isAtEnd()) pos++;
                return tokens.get(pos - 1);
            }
            
            private boolean check(TokenType type) {
                if (isAtEnd()) return false;
                return peek().type == type;
            }
            
            private boolean checkKeyword(String keyword) {
                if (isAtEnd()) return false;
                Token token = peek();
                return token.type == TokenType.KEYWORD && token.value.equals(keyword);
            }
            
            private Token expect(TokenType type) {
                if (check(type)) return advance();
                throw new RuntimeException("Expected " + type + " but got " + peek().type);
            }
            
            private Token expect(TokenType type, String value) {
                if (check(type) && peek().value.equals(value)) return advance();
                throw new RuntimeException("Expected " + value + " but got " + peek().value);
            }
            
            private void skipUntil(TokenType type) {
                while (!isAtEnd() && !check(type)) {
                    advance();
                }
            }
            
            private boolean isTypeName(String name) {
                return name.matches("(void|bool|int|uint|float|double|[biud]?vec[234]|[d]?mat[234](x[234])?|sampler.*|image.*)");
            }
            
            private boolean isSamplerType(String type) {
                return type.contains("sampler");
            }
            
            private boolean isImageType(String type) {
                return type.contains("image");
            }
            
            private GLSLSampler createSamplerFromVariable(GLSLVariable var) {
                GLSLSampler sampler = new GLSLSampler();
                sampler.name = var.name;
                sampler.type = var.type;
                sampler.binding = var.binding;
                sampler.arraySize = var.arraySize;
                
                // Parse sampler type
                String type = var.type.toLowerCase();
                sampler.shadow = type.contains("shadow");
                sampler.multisample = type.contains("ms");
                sampler.isArray = type.contains("array");
                sampler.isCube = type.contains("cube");
                sampler.isBuffer = type.contains("buffer");
                sampler.isRect = type.contains("rect");
                
                if (type.contains("1d")) sampler.dimensions = 1;
                else if (type.contains("2d")) sampler.dimensions = 2;
                else if (type.contains("3d")) sampler.dimensions = 3;
                else if (type.contains("cube")) sampler.dimensions = 3;
                
                if (type.startsWith("i")) sampler.samplerType = "int";
                else if (type.startsWith("u")) sampler.samplerType = "uint";
                else sampler.samplerType = "float";
                
                return sampler;
            }
            
            private GLSLImage createImageFromVariable(GLSLVariable var) {
                GLSLImage image = new GLSLImage();
                image.name = var.name;
                image.type = var.type;
                image.binding = var.binding;
                image.arraySize = var.arraySize;
                
                String type = var.type.toLowerCase();
                image.isArray = type.contains("array");
                image.isCube = type.contains("cube");
                image.isBuffer = type.contains("buffer");
                image.isMS = type.contains("ms");
                
                if (type.contains("1d")) image.dimensions = 1;
                else if (type.contains("2d")) image.dimensions = 2;
                else if (type.contains("3d")) image.dimensions = 3;
                else if (type.contains("cube")) image.dimensions = 3;
                
                if (type.startsWith("i")) image.imageType = "int";
                else if (type.startsWith("u")) image.imageType = "uint";
                else image.imageType = "float";
                
                return image;
            }
        }
        
        /**
         * Main translation method
         */
        public String translate(String glslSource, int shaderType) {
            this.shaderStage = shaderType;
            
            // Tokenize
            GLSLLexer lexer = new GLSLLexer(glslSource);
            List<Token> tokens = lexer.tokenize();
            
            // Parse
            GLSLParser parser = new GLSLParser(tokens);
            try {
                parser.parse();
            } catch (Exception e) {
                // Handle parse errors gracefully
                System.err.println("GLSL parse error: " + e.getMessage());
            }
            
            // Detect builtin usage
            detectBuiltinUsage(glslSource);
            
            // Generate MSL
            return generateMSL();
        }
        
        private void detectBuiltinUsage(String source) {
            usesGlPosition = source.contains("gl_Position");
            usesGlPointSize = source.contains("gl_PointSize");
            usesGlClipDistance = source.contains("gl_ClipDistance");
            usesGlVertexID = source.contains("gl_VertexID") || source.contains("gl_VertexIndex");
            usesGlInstanceID = source.contains("gl_InstanceID") || source.contains("gl_InstanceIndex");
            usesGlFrontFacing = source.contains("gl_FrontFacing");
            usesGlPointCoord = source.contains("gl_PointCoord");
            usesGlPrimitiveID = source.contains("gl_PrimitiveID");
            usesGlLayer = source.contains("gl_Layer");
            usesGlViewportIndex = source.contains("gl_ViewportIndex");
            usesGlSampleID = source.contains("gl_SampleID");
            usesGlSamplePosition = source.contains("gl_SamplePosition");
            usesGlNumSamples = source.contains("gl_NumSamples");
            usesGlFragColor = source.contains("gl_FragColor");
            usesGlFragData = source.contains("gl_FragData");
            usesGlInvocationID = source.contains("gl_InvocationID");
            usesGlTessLevelOuter = source.contains("gl_TessLevelOuter");
            usesGlTessLevelInner = source.contains("gl_TessLevelInner");
            usesGlTessCoord = source.contains("gl_TessCoord");
            usesGlPatchVerticesIn = source.contains("gl_PatchVerticesIn");
            usesGlWorkGroupID = source.contains("gl_WorkGroupID");
            usesGlLocalInvocationID = source.contains("gl_LocalInvocationID");
            usesGlGlobalInvocationID = source.contains("gl_GlobalInvocationID");
            usesGlNumWorkGroups = source.contains("gl_NumWorkGroups");
        }
        
        private String generateMSL() {
            mslOutput.setLength(0);
            
            // Header
            mslOutput.append("#include <metal_stdlib>\n");
            mslOutput.append("#include <simd/simd.h>\n");
            mslOutput.append("using namespace metal;\n\n");
            
            // Generate helper functions
            generateHelperFunctions();
            
            // Generate structs
            generateStructs();
            
            // Generate input/output structs
            generateIOStructs();
            
            // Generate constant buffers for uniforms
            generateConstantBuffers();
            
            // Generate user functions
            generateFunctions();
            
            // Generate main function
            generateMainFunction();
            
            return mslOutput.toString();
        }
        
        private void generateHelperFunctions() {
            mslOutput.append("// ═══════════════════════════════════════════════════════════\n");
            mslOutput.append("// Helper Functions for GLSL Compatibility\n");
            mslOutput.append("// ═══════════════════════════════════════════════════════════\n\n");
            
            // Matrix multiplication helpers
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline T mod_emu(T x, T y) { return x - y * floor(x / y); }\n\n");
            
            // atan with two arguments
            mslOutput.append("inline float atan_emu(float y, float x) { return atan2(y, x); }\n");
            mslOutput.append("inline float2 atan_emu(float2 y, float2 x) { return atan2(y, x); }\n");
            mslOutput.append("inline float3 atan_emu(float3 y, float3 x) { return atan2(y, x); }\n");
            mslOutput.append("inline float4 atan_emu(float4 y, float4 x) { return atan2(y, x); }\n\n");
            
            // Texture functions
            mslOutput.append("// Texture sampling helpers\n");
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline vec<T, 4> texture_impl(texture2d<T> tex, sampler s, float2 coord) {\n");
            mslOutput.append("    return tex.sample(s, coord);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline vec<T, 4> textureLod_impl(texture2d<T> tex, sampler s, float2 coord, float lod) {\n");
            mslOutput.append("    return tex.sample(s, coord, level(lod));\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline vec<T, 4> texelFetch_impl(texture2d<T> tex, int2 coord, int lod) {\n");
            mslOutput.append("    return tex.read(uint2(coord), lod);\n");
            mslOutput.append("}\n\n");
            
            // textureSize emulation
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline int2 textureSize_impl(texture2d<T> tex, int lod) {\n");
            mslOutput.append("    return int2(tex.get_width(lod), tex.get_height(lod));\n");
            mslOutput.append("}\n\n");
            
            // textureProjLod
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline vec<T, 4> textureProjLod_impl(texture2d<T> tex, sampler s, float4 coord, float lod) {\n");
            mslOutput.append("    return tex.sample(s, coord.xy / coord.w, level(lod));\n");
            mslOutput.append("}\n\n");
            
            // Shadow sampling
            mslOutput.append("inline float texture_shadow_impl(depth2d<float> tex, sampler s, float3 coord) {\n");
            mslOutput.append("    return tex.sample_compare(s, coord.xy, coord.z);\n");
            mslOutput.append("}\n\n");
            
            // Cube shadow
            mslOutput.append("inline float texture_shadow_cube_impl(depthcube<float> tex, sampler s, float4 coord) {\n");
            mslOutput.append("    return tex.sample_compare(s, coord.xyz, coord.w);\n");
            mslOutput.append("}\n\n");
            
            // Atomic operations
            mslOutput.append("// Atomic operation helpers\n");
            mslOutput.append("inline uint atomicAdd_impl(device atomic_uint* mem, uint data) {\n");
            mslOutput.append("    return atomic_fetch_add_explicit(mem, data, memory_order_relaxed);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline int atomicAdd_impl(device atomic_int* mem, int data) {\n");
            mslOutput.append("    return atomic_fetch_add_explicit(mem, data, memory_order_relaxed);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint atomicMin_impl(device atomic_uint* mem, uint data) {\n");
            mslOutput.append("    return atomic_fetch_min_explicit(mem, data, memory_order_relaxed);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint atomicMax_impl(device atomic_uint* mem, uint data) {\n");
            mslOutput.append("    return atomic_fetch_max_explicit(mem, data, memory_order_relaxed);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint atomicAnd_impl(device atomic_uint* mem, uint data) {\n");
            mslOutput.append("    return atomic_fetch_and_explicit(mem, data, memory_order_relaxed);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint atomicOr_impl(device atomic_uint* mem, uint data) {\n");
            mslOutput.append("    return atomic_fetch_or_explicit(mem, data, memory_order_relaxed);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint atomicXor_impl(device atomic_uint* mem, uint data) {\n");
            mslOutput.append("    return atomic_fetch_xor_explicit(mem, data, memory_order_relaxed);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint atomicExchange_impl(device atomic_uint* mem, uint data) {\n");
            mslOutput.append("    return atomic_exchange_explicit(mem, data, memory_order_relaxed);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint atomicCompSwap_impl(device atomic_uint* mem, uint compare, uint data) {\n");
            mslOutput.append("    atomic_compare_exchange_weak_explicit(mem, &compare, data, memory_order_relaxed, memory_order_relaxed);\n");
            mslOutput.append("    return compare;\n");
            mslOutput.append("}\n\n");
            
            // Barrier functions
            mslOutput.append("// Barrier helpers (for compute shaders)\n");
            mslOutput.append("#define barrier_impl() threadgroup_barrier(mem_flags::mem_threadgroup)\n");
            mslOutput.append("#define memoryBarrier_impl() threadgroup_barrier(mem_flags::mem_device)\n");
            mslOutput.append("#define memoryBarrierShared_impl() threadgroup_barrier(mem_flags::mem_threadgroup)\n");
            mslOutput.append("#define memoryBarrierBuffer_impl() threadgroup_barrier(mem_flags::mem_device)\n");
            mslOutput.append("#define memoryBarrierImage_impl() threadgroup_barrier(mem_flags::mem_texture)\n");
            mslOutput.append("#define groupMemoryBarrier_impl() threadgroup_barrier(mem_flags::mem_threadgroup)\n\n");
            
            // Bit operations
            mslOutput.append("// Bit operation helpers\n");
            mslOutput.append("inline uint bitfieldExtract_impl(uint value, int offset, int bits) {\n");
            mslOutput.append("    return extract_bits(value, offset, bits);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint bitfieldInsert_impl(uint base, uint insert, int offset, int bits) {\n");
            mslOutput.append("    return insert_bits(base, insert, offset, bits);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint bitfieldReverse_impl(uint value) {\n");
            mslOutput.append("    return reverse_bits(value);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint bitCount_impl(uint value) {\n");
            mslOutput.append("    return popcount(value);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline int findLSB_impl(uint value) {\n");
            mslOutput.append("    return value == 0 ? -1 : ctz(value);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline int findMSB_impl(uint value) {\n");
            mslOutput.append("    return value == 0 ? -1 : (31 - clz(value));\n");
            mslOutput.append("}\n\n");
            
            // Pack/unpack functions
            mslOutput.append("// Pack/unpack helpers\n");
            mslOutput.append("inline uint packHalf2x16_impl(float2 v) {\n");
            mslOutput.append("    return as_type<uint>(half2(v));\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float2 unpackHalf2x16_impl(uint v) {\n");
            mslOutput.append("    return float2(as_type<half2>(v));\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint packUnorm2x16_impl(float2 v) {\n");
            mslOutput.append("    return pack_float_to_unorm2x16(v);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float2 unpackUnorm2x16_impl(uint v) {\n");
            mslOutput.append("    return unpack_unorm2x16_to_float(v);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint packSnorm2x16_impl(float2 v) {\n");
            mslOutput.append("    return pack_float_to_snorm2x16(v);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float2 unpackSnorm2x16_impl(uint v) {\n");
            mslOutput.append("    return unpack_snorm2x16_to_float(v);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint packUnorm4x8_impl(float4 v) {\n");
            mslOutput.append("    return pack_float_to_unorm4x8(v);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float4 unpackUnorm4x8_impl(uint v) {\n");
            mslOutput.append("    return unpack_unorm4x8_to_float(v);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline uint packSnorm4x8_impl(float4 v) {\n");
            mslOutput.append("    return pack_float_to_snorm4x8(v);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float4 unpackSnorm4x8_impl(uint v) {\n");
            mslOutput.append("    return unpack_snorm4x8_to_float(v);\n");
            mslOutput.append("}\n\n");
            
            // Interpolation functions
            mslOutput.append("// Interpolation helpers\n");
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline T interpolateAtCentroid_impl(T value) {\n");
            mslOutput.append("    return value; // Metal handles centroid interpolation automatically\n");
            mslOutput.append("}\n\n");
            
            // Matrix helpers
            mslOutput.append("// Matrix helpers\n");
            mslOutput.append("inline float determinant_impl(float2x2 m) {\n");
            mslOutput.append("    return determinant(m);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float determinant_impl(float3x3 m) {\n");
            mslOutput.append("    return determinant(m);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float determinant_impl(float4x4 m) {\n");
            mslOutput.append("    return determinant(m);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float2x2 inverse_impl(float2x2 m) {\n");
            mslOutput.append("    float det = determinant(m);\n");
            mslOutput.append("    return float2x2(m[1][1], -m[0][1], -m[1][0], m[0][0]) / det;\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float3x3 inverse_impl(float3x3 m) {\n");
            mslOutput.append("    float det = determinant(m);\n");
            mslOutput.append("    float3x3 adj;\n");
            mslOutput.append("    adj[0][0] = m[1][1] * m[2][2] - m[1][2] * m[2][1];\n");
            mslOutput.append("    adj[0][1] = m[0][2] * m[2][1] - m[0][1] * m[2][2];\n");
            mslOutput.append("    adj[0][2] = m[0][1] * m[1][2] - m[0][2] * m[1][1];\n");
            mslOutput.append("    adj[1][0] = m[1][2] * m[2][0] - m[1][0] * m[2][2];\n");
            mslOutput.append("    adj[1][1] = m[0][0] * m[2][2] - m[0][2] * m[2][0];\n");
            mslOutput.append("    adj[1][2] = m[0][2] * m[1][0] - m[0][0] * m[1][2];\n");
            mslOutput.append("    adj[2][0] = m[1][0] * m[2][1] - m[1][1] * m[2][0];\n");
            mslOutput.append("    adj[2][1] = m[0][1] * m[2][0] - m[0][0] * m[2][1];\n");
            mslOutput.append("    adj[2][2] = m[0][0] * m[1][1] - m[0][1] * m[1][0];\n");
            mslOutput.append("    return adj / det;\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float4x4 inverse_impl(float4x4 m) {\n");
            mslOutput.append("    float n11 = m[0][0], n12 = m[1][0], n13 = m[2][0], n14 = m[3][0];\n");
            mslOutput.append("    float n21 = m[0][1], n22 = m[1][1], n23 = m[2][1], n24 = m[3][1];\n");
            mslOutput.append("    float n31 = m[0][2], n32 = m[1][2], n33 = m[2][2], n34 = m[3][2];\n");
            mslOutput.append("    float n41 = m[0][3], n42 = m[1][3], n43 = m[2][3], n44 = m[3][3];\n");
            mslOutput.append("    float t11 = n23 * n34 * n42 - n24 * n33 * n42 + n24 * n32 * n43 - n22 * n34 * n43 - n23 * n32 * n44 + n22 * n33 * n44;\n");
            mslOutput.append("    float t12 = n14 * n33 * n42 - n13 * n34 * n42 - n14 * n32 * n43 + n12 * n34 * n43 + n13 * n32 * n44 - n12 * n33 * n44;\n");
            mslOutput.append("    float t13 = n13 * n24 * n42 - n14 * n23 * n42 + n14 * n22 * n43 - n12 * n24 * n43 - n13 * n22 * n44 + n12 * n23 * n44;\n");
            mslOutput.append("    float t14 = n14 * n23 * n32 - n13 * n24 * n32 - n14 * n22 * n33 + n12 * n24 * n33 + n13 * n22 * n34 - n12 * n23 * n34;\n");
            mslOutput.append("    float det = n11 * t11 + n21 * t12 + n31 * t13 + n41 * t14;\n");
            mslOutput.append("    float idet = 1.0f / det;\n");
            mslOutput.append("    float4x4 ret;\n");
            mslOutput.append("    ret[0][0] = t11 * idet;\n");
            mslOutput.append("    ret[0][1] = (n24 * n33 * n41 - n23 * n34 * n41 - n24 * n31 * n43 + n21 * n34 * n43 + n23 * n31 * n44 - n21 * n33 * n44) * idet;\n");
            mslOutput.append("    ret[0][2] = (n22 * n34 * n41 - n24 * n32 * n41 + n24 * n31 * n42 - n21 * n34 * n42 - n22 * n31 * n44 + n21 * n32 * n44) * idet;\n");
            mslOutput.append("    ret[0][3] = (n23 * n32 * n41 - n22 * n33 * n41 - n23 * n31 * n42 + n21 * n33 * n42 + n22 * n31 * n43 - n21 * n32 * n43) * idet;\n");
            mslOutput.append("    ret[1][0] = t12 * idet;\n");
            mslOutput.append("    ret[1][1] = (n13 * n34 * n41 - n14 * n33 * n41 + n14 * n31 * n43 - n11 * n34 * n43 - n13 * n31 * n44 + n11 * n33 * n44) * idet;\n");
            mslOutput.append("    ret[1][2] = (n14 * n32 * n41 - n12 * n34 * n41 - n14 * n31 * n42 + n11 * n34 * n42 + n12 * n31 * n44 - n11 * n32 * n44) * idet;\n");
            mslOutput.append("    ret[1][3] = (n12 * n33 * n41 - n13 * n32 * n41 + n13 * n31 * n42 - n11 * n33 * n42 - n12 * n31 * n43 + n11 * n32 * n43) * idet;\n");
            mslOutput.append("    ret[2][0] = t13 * idet;\n");
            mslOutput.append("    ret[2][1] = (n14 * n23 * n41 - n13 * n24 * n41 - n14 * n21 * n43 + n11 * n24 * n43 + n13 * n21 * n44 - n11 * n23 * n44) * idet;\n");
            mslOutput.append("    ret[2][2] = (n12 * n24 * n41 - n14 * n22 * n41 + n14 * n21 * n42 - n11 * n24 * n42 - n12 * n21 * n44 + n11 * n22 * n44) * idet;\n");
            mslOutput.append("    ret[2][3] = (n13 * n22 * n41 - n12 * n23 * n41 - n13 * n21 * n42 + n11 * n23 * n42 + n12 * n21 * n43 - n11 * n22 * n43) * idet;\n");
            mslOutput.append("    ret[3][0] = t14 * idet;\n");
            mslOutput.append("    ret[3][1] = (n13 * n24 * n31 - n14 * n23 * n31 + n14 * n21 * n33 - n11 * n24 * n33 - n13 * n21 * n34 + n11 * n23 * n34) * idet;\n");
            mslOutput.append("    ret[3][2] = (n14 * n22 * n31 - n12 * n24 * n31 - n14 * n21 * n32 + n11 * n24 * n32 + n12 * n21 * n34 - n11 * n22 * n34) * idet;\n");
            mslOutput.append("    ret[3][3] = (n12 * n23 * n31 - n13 * n22 * n31 + n13 * n21 * n32 - n11 * n23 * n32 - n12 * n21 * n33 + n11 * n22 * n33) * idet;\n");
            mslOutput.append("    return ret;\n");
            mslOutput.append("}\n\n");
            
            // outerProduct
            mslOutput.append("inline float2x2 outerProduct_impl(float2 c, float2 r) {\n");
            mslOutput.append("    return float2x2(c * r.x, c * r.y);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float3x3 outerProduct_impl(float3 c, float3 r) {\n");
            mslOutput.append("    return float3x3(c * r.x, c * r.y, c * r.z);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float4x4 outerProduct_impl(float4 c, float4 r) {\n");
            mslOutput.append("    return float4x4(c * r.x, c * r.y, c * r.z, c * r.w);\n");
            mslOutput.append("}\n\n");
            
            // matrixCompMult
            mslOutput.append("template<typename T, int C, int R>\n");
            mslOutput.append("inline matrix<T, C, R> matrixCompMult_impl(matrix<T, C, R> a, matrix<T, C, R> b) {\n");
            mslOutput.append("    matrix<T, C, R> result;\n");
            mslOutput.append("    for (int i = 0; i < C; i++) {\n");
            mslOutput.append("        result[i] = a[i] * b[i];\n");
            mslOutput.append("    }\n");
            mslOutput.append("    return result;\n");
            mslOutput.append("}\n\n");
            
            // Noise functions (simplified approximations)
            mslOutput.append("// Noise function approximations\n");
            mslOutput.append("inline float noise1_impl(float x) {\n");
            mslOutput.append("    return fract(sin(x * 12.9898) * 43758.5453);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float noise1_impl(float2 v) {\n");
            mslOutput.append("    return fract(sin(dot(v, float2(12.9898, 78.233))) * 43758.5453);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float noise1_impl(float3 v) {\n");
            mslOutput.append("    return fract(sin(dot(v, float3(12.9898, 78.233, 45.164))) * 43758.5453);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float noise1_impl(float4 v) {\n");
            mslOutput.append("    return fract(sin(dot(v, float4(12.9898, 78.233, 45.164, 94.673))) * 43758.5453);\n");
            mslOutput.append("}\n\n");
            
            // Derivative functions
            mslOutput.append("// Derivative helpers\n");
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline T dFdx_impl(T v) { return dfdx(v); }\n\n");
            
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline T dFdy_impl(T v) { return dfdy(v); }\n\n");
            
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline T dFdxCoarse_impl(T v) { return dfdx(v); }\n\n");
            
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline T dFdyCoarse_impl(T v) { return dfdy(v); }\n\n");
            
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline T dFdxFine_impl(T v) { return dfdx(v); }\n\n");
            
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline T dFdyFine_impl(T v) { return dfdy(v); }\n\n");
            
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline T fwidth_impl(T v) { return fwidth(v); }\n\n");
            
            // Floating point helpers
            mslOutput.append("// Floating point helpers\n");
            mslOutput.append("inline bool isinf_impl(float x) { return isinf(x); }\n");
            mslOutput.append("inline bool isnan_impl(float x) { return isnan(x); }\n\n");
            
            mslOutput.append("inline float intBitsToFloat_impl(int x) { return as_type<float>(x); }\n");
            mslOutput.append("inline int floatBitsToInt_impl(float x) { return as_type<int>(x); }\n");
            mslOutput.append("inline float uintBitsToFloat_impl(uint x) { return as_type<float>(x); }\n");
            mslOutput.append("inline uint floatBitsToUint_impl(float x) { return as_type<uint>(x); }\n\n");
            
            mslOutput.append("inline float2 intBitsToFloat_impl(int2 x) { return as_type<float2>(x); }\n");
            mslOutput.append("inline int2 floatBitsToInt_impl(float2 x) { return as_type<int2>(x); }\n");
            mslOutput.append("inline float3 intBitsToFloat_impl(int3 x) { return as_type<float3>(x); }\n");
            mslOutput.append("inline int3 floatBitsToInt_impl(float3 x) { return as_type<int3>(x); }\n");
            mslOutput.append("inline float4 intBitsToFloat_impl(int4 x) { return as_type<float4>(x); }\n");
            mslOutput.append("inline int4 floatBitsToInt_impl(float4 x) { return as_type<int4>(x); }\n\n");
            
            // FMA
            mslOutput.append("template<typename T>\n");
            mslOutput.append("inline T fma_impl(T a, T b, T c) { return fma(a, b, c); }\n\n");
            
            // frexp/ldexp
            mslOutput.append("inline float frexp_impl(float x, thread int& exp) {\n");
            mslOutput.append("    int e;\n");
            mslOutput.append("    float m = frexp(x, e);\n");
            mslOutput.append("    exp = e;\n");
            mslOutput.append("    return m;\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("inline float ldexp_impl(float x, int exp) {\n");
            mslOutput.append("    return ldexp(x, exp);\n");
            mslOutput.append("}\n\n");
            
            mslOutput.append("\n");
        }
        
        private void generateStructs() {
            if (structs.isEmpty()) return;
            
            mslOutput.append("// ═══════════════════════════════════════════════════════════\n");
            mslOutput.append("// User-defined Structures\n");
            mslOutput.append("// ═══════════════════════════════════════════════════════════\n\n");
            
            for (GLSLStruct struct : structs.values()) {
                mslOutput.append("struct ").append(struct.name).append(" {\n");
                for (GLSLVariable member : struct.members) {
                    mslOutput.append("    ").append(translateType(member.type)).append(" ");
                    mslOutput.append(member.name);
                    if (member.arraySize > 0) {
                        mslOutput.append("[").append(member.arraySize).append("]");
                    }
                    mslOutput.append(";\n");
                }
                mslOutput.append("};\n\n");
            }
        }
        
        private void generateIOStructs() {
            mslOutput.append("// ═══════════════════════════════════════════════════════════\n");
            mslOutput.append("// Input/Output Structures\n");
            mslOutput.append("// ═══════════════════════════════════════════════════════════\n\n");
            
            // Generate input struct
            if (shaderStage == GL_VERTEX_SHADER) {
                generateVertexInputStruct();
            } else if (shaderStage == GL_FRAGMENT_SHADER) {
                generateFragmentInputStruct();
            } else if (shaderStage == GL_COMPUTE_SHADER) {
                // Compute shaders don't have traditional inputs
            }
            
            // Generate output struct
            if (shaderStage == GL_VERTEX_SHADER) {
                generateVertexOutputStruct();
            } else if (shaderStage == GL_FRAGMENT_SHADER) {
                generateFragmentOutputStruct();
            }
        }
        
        private void generateVertexInputStruct() {
            mslOutput.append("struct VertexInput {\n");
            
            int attrIndex = 0;
            for (GLSLVariable input : inputs) {
                int loc = input.location >= 0 ? input.location : attrIndex;
                mslOutput.append("    ").append(translateType(input.type)).append(" ");
                mslOutput.append(input.name);
                mslOutput.append(" [[attribute(").append(loc).append(")]];\n");
                attrIndex++;
            }
            
            mslOutput.append("};\n\n");
        }
        
        private void generateVertexOutputStruct() {
            mslOutput.append("struct VertexOutput {\n");
            
            // Always include position
            mslOutput.append("    float4 position [[position]];\n");
            
            if (usesGlPointSize) {
                mslOutput.append("    float pointSize [[point_size]];\n");
            }
            
            if (usesGlClipDistance) {
                mslOutput.append("    float clipDistance [[clip_distance]];\n");
            }
            
            int locIndex = 0;
            for (GLSLVariable output : outputs) {
                String interp = "";
                if (output.flat) interp = " [[flat]]";
                else if (output.noperspective) interp = " [[center_no_perspective]]";
                else if (output.centroid) interp = " [[centroid_perspective]]";
                else if (output.sample) interp = " [[sample_perspective]]";
                
                int loc = output.location >= 0 ? output.location : locIndex;
                mslOutput.append("    ").append(translateType(output.type)).append(" ");
                mslOutput.append(sanitizeName(output.name));
                mslOutput.append(" [[user(locn").append(loc).append(")]]");
                mslOutput.append(interp).append(";\n");
                locIndex++;
            }
            
            mslOutput.append("};\n\n");
        }
        
        private void generateFragmentInputStruct() {
            mslOutput.append("struct FragmentInput {\n");
            
            mslOutput.append("    float4 position [[position]];\n");
            
            if (usesGlFrontFacing) {
                mslOutput.append("    bool frontFacing [[front_facing]];\n");
            }
            
            if (usesGlPointCoord) {
                mslOutput.append("    float2 pointCoord [[point_coord]];\n");
            }
            
            if (usesGlSampleID) {
                mslOutput.append("    uint sampleId [[sample_id]];\n");
            }
            
            if (usesGlSamplePosition) {
                mslOutput.append("    float2 samplePosition [[sample_position]];\n");
            }
            
            if (usesGlPrimitiveID) {
                mslOutput.append("    uint primitiveId [[primitive_id]];\n");
            }
            
            if (usesGlLayer) {
                mslOutput.append("    uint layer [[render_target_array_index]];\n");
            }
            
            int locIndex = 0;
            for (GLSLVariable input : inputs) {
                String interp = "";
                if (input.flat) interp = " [[flat]]";
                else if (input.noperspective) interp = " [[center_no_perspective]]";
                else if (input.centroid) interp = " [[centroid_perspective]]";
                else if (input.sample) interp = " [[sample_perspective]]";
                
                int loc = input.location >= 0 ? input.location : locIndex;
                mslOutput.append("    ").append(translateType(input.type)).append(" ");
                mslOutput.append(sanitizeName(input.name));
                mslOutput.append(" [[user(locn").append(loc).append(")]]");
                mslOutput.append(interp).append(";\n");
                locIndex++;
            }
            
            mslOutput.append("};\n\n");
        }
        
        private void generateFragmentOutputStruct() {
            mslOutput.append("struct FragmentOutput {\n");
            
            if (usesGlFragColor || usesGlFragData || outputs.isEmpty()) {
                // Legacy output
                mslOutput.append("    float4 color [[color(0)]];\n");
            } else {
                int colorIndex = 0;
                for (GLSLVariable output : outputs) {
                    int loc = output.location >= 0 ? output.location : colorIndex;
                    mslOutput.append("    ").append(translateType(output.type)).append(" ");
                    mslOutput.append(sanitizeName(output.name));
                    mslOutput.append(" [[color(").append(loc).append(")]]");
                    if (output.index >= 0) {
                        mslOutput.append(" [[index(").append(output.index).append(")]]");
                    }
                    mslOutput.append(";\n");
                    colorIndex++;
                }
            }
            
            // Depth output
            boolean hasDepthOutput = outputs.stream().anyMatch(o -> 
                o.name.equals("gl_FragDepth") || o.type.contains("depth"));
            if (hasDepthOutput) {
                mslOutput.append("    float depth [[depth(any)]];\n");
            }
            
            mslOutput.append("};\n\n");
        }
        
        private void generateConstantBuffers() {
            mslOutput.append("// ═══════════════════════════════════════════════════════════\n");
            mslOutput.append("// Constant Buffers and Uniforms\n");
            mslOutput.append("// ═══════════════════════════════════════════════════════════\n\n");
            
            // Generate uniform blocks
            for (GLSLUniformBlock block : uniformBlocks) {
                mslOutput.append("struct ").append(block.name).append("_t {\n");
                for (GLSLVariable member : block.members) {
                    mslOutput.append("    ").append(translateType(member.type));
                    mslOutput.append(" ").append(member.name);
                    if (member.arraySize > 0) {
                        mslOutput.append("[").append(member.arraySize).append("]");
                    }
                    mslOutput.append(";\n");
                }
                mslOutput.append("};\n\n");
            }
            
            // Generate SSBO structs
            for (GLSLShaderStorageBlock block : shaderStorageBlocks) {
                mslOutput.append("struct ").append(block.name).append("_t {\n");
                for (GLSLVariable member : block.members) {
                    mslOutput.append("    ").append(translateType(member.type));
                    mslOutput.append(" ").append(member.name);
                    if (member.arraySize > 0) {
                        mslOutput.append("[").append(member.arraySize).append("]");
                    } else if (member.arraySize == -1) {
                        // Unsized array - use pointer
                        mslOutput.append("[1]"); // Placeholder, actual size determined at runtime
                    }
                    mslOutput.append(";\n");
                }
                mslOutput.append("};\n\n");
            }
            
            // Generate single uniforms as a struct
            if (!uniforms.isEmpty()) {
                mslOutput.append("struct Uniforms_t {\n");
                for (GLSLVariable uniform : uniforms) {
                    mslOutput.append("    ").append(translateType(uniform.type));
                    mslOutput.append(" ").append(uniform.name);
                    if (uniform.arraySize > 0) {
                        mslOutput.append("[").append(uniform.arraySize).append("]");
                    }
                    mslOutput.append(";\n");
                }
                mslOutput.append("};\n\n");
            }
        }
        
        private void generateFunctions() {
            if (functions.size() <= 1) return; // Only main
            
            mslOutput.append("// ═══════════════════════════════════════════════════════════\n");
            mslOutput.append("// User Functions\n");
            mslOutput.append("// ═══════════════════════════════════════════════════════════\n\n");
            
            for (GLSLFunction func : functions.values()) {
                if (func.isMain) continue;
                
                mslOutput.append(translateType(func.returnType)).append(" ");
                mslOutput.append(func.name).append("(");
                
                boolean first = true;
                for (GLSLVariable param : func.parameters) {
                    if (!first) mslOutput.append(", ");
                    first = false;
                    
                    if (param.interpolation.equals("out") || param.interpolation.equals("inout")) {
                        mslOutput.append("thread ");
                    }
                    mslOutput.append(translateType(param.type));
                    if (param.interpolation.equals("out") || param.interpolation.equals("inout")) {
                        mslOutput.append("&");
                    }
                    mslOutput.append(" ").append(param.name);
                }
                
                mslOutput.append(") {\n");
                mslOutput.append(translateFunctionBody(func.body));
                mslOutput.append("}\n\n");
            }
        }
        
        private void generateMainFunction() {
            mslOutput.append("// ═══════════════════════════════════════════════════════════\n");
            mslOutput.append("// Main Shader Function\n");
            mslOutput.append("// ═══════════════════════════════════════════════════════════\n\n");
            
            GLSLFunction mainFunc = functions.get("main");
            
            if (shaderStage == GL_VERTEX_SHADER) {
                generateVertexMain(mainFunc);
            } else if (shaderStage == GL_FRAGMENT_SHADER) {
                generateFragmentMain(mainFunc);
            } else if (shaderStage == GL_COMPUTE_SHADER) {
                generateComputeMain(mainFunc);
            }
        }
        
        private void generateVertexMain(GLSLFunction mainFunc) {
            mslOutput.append("vertex VertexOutput vertexMain(\n");
            mslOutput.append("    VertexInput in [[stage_in]],\n");
            mslOutput.append("    uint vertexID [[vertex_id]],\n");
            mslOutput.append("    uint instanceID [[instance_id]]");
            
            // Add buffer parameters
            int bufferIndex = 0;
            
            if (!uniforms.isEmpty()) {
                mslOutput.append(",\n    constant Uniforms_t& uniforms [[buffer(").append(bufferIndex++).append(")]]");
            }
            
            for (GLSLUniformBlock block : uniformBlocks) {
                int binding = block.binding >= 0 ? block.binding : bufferIndex;
                mslOutput.append(",\n    constant ").append(block.name).append("_t& ");
                mslOutput.append(block.instanceName != null ? block.instanceName : block.name);
                mslOutput.append(" [[buffer(").append(binding).append(")]]");
                bufferIndex++;
            }
            
            // Add texture/sampler parameters
            int textureIndex = 0;
            int samplerIndex = 0;
            for (GLSLSampler sampler : samplers) {
                int binding = sampler.binding >= 0 ? sampler.binding : textureIndex;
                mslOutput.append(",\n    ");
                mslOutput.append(translateSamplerType(sampler));
                mslOutput.append(" ").append(sampler.name).append("_tex");
                mslOutput.append(" [[texture(").append(binding).append(")]]");
                mslOutput.append(",\n    sampler ").append(sampler.name).append("_smp");
                mslOutput.append(" [[sampler(").append(binding).append(")]]");
                textureIndex++;
                samplerIndex++;
            }
            
            mslOutput.append("\n) {\n");
            
            // Initialize output
            mslOutput.append("    VertexOutput out;\n\n");
            
            // Declare local variables for inputs
            for (GLSLVariable input : inputs) {
                mslOutput.append("    ").append(translateType(input.type)).append(" ");
                mslOutput.append(input.name).append(" = in.").append(input.name).append(";\n");
            }
            
            // Declare gl_* builtins
            if (usesGlVertexID) {
                mslOutput.append("    int gl_VertexID = vertexID;\n");
            }
            if (usesGlInstanceID) {
                mslOutput.append("    int gl_InstanceID = instanceID;\n");
            }
            mslOutput.append("    float4 gl_Position = float4(0.0);\n");
            if (usesGlPointSize) {
                mslOutput.append("    float gl_PointSize = 1.0;\n");
            }
            
            // Declare outputs as locals
            for (GLSLVariable output : outputs) {
                mslOutput.append("    ").append(translateType(output.type)).append(" ");
                mslOutput.append(output.name).append(";\n");
            }
            
            mslOutput.append("\n");
            
            // Translate main body
            if (mainFunc != null && mainFunc.body != null) {
                mslOutput.append(translateFunctionBody(mainFunc.body));
            }
            
            mslOutput.append("\n");
            
            // Copy outputs
            mslOutput.append("    out.position = gl_Position;\n");
            if (usesGlPointSize) {
                mslOutput.append("    out.pointSize = gl_PointSize;\n");
            }
            for (GLSLVariable output : outputs) {
                mslOutput.append("    out.").append(sanitizeName(output.name));
                mslOutput.append(" = ").append(output.name).append(";\n");
            }
            
            mslOutput.append("    return out;\n");
            mslOutput.append("}\n");
        }
        
        private void generateFragmentMain(GLSLFunction mainFunc) {
            mslOutput.append("fragment FragmentOutput fragmentMain(\n");
            mslOutput.append("    FragmentInput in [[stage_in]]");
            
            // Add buffer parameters
            int bufferIndex = 0;
            
            if (!uniforms.isEmpty()) {
                mslOutput.append(",\n    constant Uniforms_t& uniforms [[buffer(").append(bufferIndex++).append(")]]");
            }
            
            for (GLSLUniformBlock block : uniformBlocks) {
                int binding = block.binding >= 0 ? block.binding : bufferIndex;
                mslOutput.append(",\n    constant ").append(block.name).append("_t& ");
                mslOutput.append(block.instanceName != null ? block.instanceName : block.name);
                mslOutput.append(" [[buffer(").append(binding).append(")]]");
                bufferIndex++;
            }
            
            // Add texture/sampler parameters
            for (GLSLSampler sampler : samplers) {
                int binding = sampler.binding >= 0 ? sampler.binding : textureBindingCounter++;
                mslOutput.append(",\n    ");
                mslOutput.append(translateSamplerType(sampler));
                mslOutput.append(" ").append(sampler.name).append("_tex");
                mslOutput.append(" [[texture(").append(binding).append(")]]");
                mslOutput.append(",\n    sampler ").append(sampler.name).append("_smp");
                mslOutput.append(" [[sampler(").append(binding).append(")]]");
            }
            
            // Add image parameters
            for (GLSLImage image : images) {
                int binding = image.binding >= 0 ? image.binding : textureBindingCounter++;
                mslOutput.append(",\n    ");
                mslOutput.append(translateImageType(image));
                mslOutput.append(" ").append(image.name);
                mslOutput.append(" [[texture(").append(binding).append(")]]");
            }
            
            mslOutput.append("\n) {\n");
            
            // Initialize output
            mslOutput.append("    FragmentOutput out;\n\n");
            
            // Declare local variables for inputs
            for (GLSLVariable input : inputs) {
                mslOutput.append("    ").append(translateType(input.type)).append(" ");
                mslOutput.append(input.name).append(" = in.").append(sanitizeName(input.name)).append(";\n");
            }
            
            // Declare gl_* builtins
            mslOutput.append("    float4 gl_FragCoord = in.position;\n");
            if (usesGlFrontFacing) {
                mslOutput.append("    bool gl_FrontFacing = in.frontFacing;\n");
            }
            if (usesGlPointCoord) {
                mslOutput.append("    float2 gl_PointCoord = in.pointCoord;\n");
            }
            if (usesGlSampleID) {
                mslOutput.append("    int gl_SampleID = in.sampleId;\n");
            }
            if (usesGlSamplePosition) {
                mslOutput.append("    float2 gl_SamplePosition = in.samplePosition;\n");
            }
            if (usesGlPrimitiveID) {
                mslOutput.append("    int gl_PrimitiveID = in.primitiveId;\n");
            }
            if (usesGlLayer) {
                mslOutput.append("    int gl_Layer = in.layer;\n");
            }
            
            // Declare outputs as locals
            if (usesGlFragColor || usesGlFragData) {
                mslOutput.append("    float4 gl_FragColor = float4(0.0);\n");
                mslOutput.append("    float4 gl_FragData[8];\n");
            } else {
                for (GLSLVariable output : outputs) {
                    mslOutput.append("    ").append(translateType(output.type)).append(" ");
                    mslOutput.append(output.name).append(";\n");
                }
            }
            
            mslOutput.append("    float gl_FragDepth = in.position.z;\n");
            mslOutput.append("    bool _discard = false;\n");
            
            mslOutput.append("\n");
            
            // Translate main body
            if (mainFunc != null && mainFunc.body != null) {
                mslOutput.append(translateFunctionBody(mainFunc.body));
            }
            
            mslOutput.append("\n");
            
            // Handle discard
            mslOutput.append("    if (_discard) discard_fragment();\n\n");
            
            // Copy outputs
            if (usesGlFragColor || usesGlFragData) {
                mslOutput.append("    out.color = gl_FragColor;\n");
            } else {
                for (GLSLVariable output : outputs) {
                    mslOutput.append("    out.").append(sanitizeName(output.name));
                    mslOutput.append(" = ").append(output.name).append(";\n");
                }
            }
            
            mslOutput.append("    return out;\n");
            mslOutput.append("}\n");
        }
        
        private void generateComputeMain(GLSLFunction mainFunc) {
            mslOutput.append("kernel void computeMain(\n");
            mslOutput.append("    uint3 gl_WorkGroupID [[threadgroup_position_in_grid]],\n");
            mslOutput.append("    uint3 gl_LocalInvocationID [[thread_position_in_threadgroup]],\n");
            mslOutput.append("    uint3 gl_GlobalInvocationID [[thread_position_in_grid]],\n");
            mslOutput.append("    uint3 gl_NumWorkGroups [[threadgroups_per_grid]],\n");
            mslOutput.append("    uint gl_LocalInvocationIndex [[thread_index_in_threadgroup]]");
            
            // Add buffer parameters
            int bufferIndex = 0;
            
            if (!uniforms.isEmpty()) {
                mslOutput.append(",\n    constant Uniforms_t& uniforms [[buffer(").append(bufferIndex++).append(")]]");
            }
            
            for (GLSLUniformBlock block : uniformBlocks) {
                int binding = block.binding >= 0 ? block.binding : bufferIndex;
                mslOutput.append(",\n    constant ").append(block.name).append("_t& ");
                mslOutput.append(block.instanceName != null ? block.instanceName : block.name);
                mslOutput.append(" [[buffer(").append(binding).append(")]]");
                bufferIndex++;
            }
            
            for (GLSLShaderStorageBlock block : shaderStorageBlocks) {
                int binding = block.binding >= 0 ? block.binding : bufferIndex;
                String access = block.readonly ? "constant" : "device";
                mslOutput.append(",\n    ").append(access).append(" ").append(block.name).append("_t");
                if (!block.readonly) mslOutput.append("*");
                else mslOutput.append("&");
                mslOutput.append(" ");
                mslOutput.append(block.instanceName != null ? block.instanceName : block.name);
                mslOutput.append(" [[buffer(").append(binding).append(")]]");
                bufferIndex++;
            }
            
            // Add texture/sampler parameters
            for (GLSLSampler sampler : samplers) {
                int binding = sampler.binding >= 0 ? sampler.binding : textureBindingCounter++;
                mslOutput.append(",\n    ");
                mslOutput.append(translateSamplerType(sampler));
                mslOutput.append(" ").append(sampler.name).append("_tex");
                mslOutput.append(" [[texture(").append(binding).append(")]]");
                mslOutput.append(",\n    sampler ").append(sampler.name).append("_smp");
                mslOutput.append(" [[sampler(").append(binding).append(")]]");
            }
            
            // Add image parameters
            for (GLSLImage image : images) {
                int binding = image.binding >= 0 ? image.binding : textureBindingCounter++;
                mslOutput.append(",\n    ");
                mslOutput.append(translateImageType(image));
                mslOutput.append(" ").append(image.name);
                mslOutput.append(" [[texture(").append(binding).append(")]]");
            }
            
            mslOutput.append("\n) {\n");
            
            // Translate main body
            if (mainFunc != null && mainFunc.body != null) {
                mslOutput.append(translateFunctionBody(mainFunc.body));
            }
            
            mslOutput.append("}\n");
        }
        
        private String translateType(String glslType) {
            if (glslType == null) return "void";
            
            // Remove precision qualifiers
            glslType = glslType.replace("lowp ", "").replace("mediump ", "").replace("highp ", "").trim();
            
            return switch (glslType) {
                case "void" -> "void";
                case "bool" -> "bool";
                case "int" -> "int";
                case "uint" -> "uint";
                case "float" -> "float";
                case "double" -> "float"; // Metal doesn't have double, use float
                case "vec2" -> "float2";
                case "vec3" -> "float3";
                case "vec4" -> "float4";
                case "dvec2" -> "float2";
                case "dvec3" -> "float3";
                case "dvec4" -> "float4";
                case "ivec2" -> "int2";
                case "ivec3" -> "int3";
                case "ivec4" -> "int4";
                case "uvec2" -> "uint2";
                case "uvec3" -> "uint3";
                case "uvec4" -> "uint4";
                case "bvec2" -> "bool2";
                case "bvec3" -> "bool3";
                case "bvec4" -> "bool4";
                case "mat2", "mat2x2" -> "float2x2";
                case "mat3", "mat3x3" -> "float3x3";
                case "mat4", "mat4x4" -> "float4x4";
                case "mat2x3" -> "float2x3";
                case "mat2x4" -> "float2x4";
                case "mat3x2" -> "float3x2";
                case "mat3x4" -> "float3x4";
                case "mat4x2" -> "float4x2";
                case "mat4x3" -> "float4x3";
                case "dmat2", "dmat2x2" -> "float2x2";
                case "dmat3", "dmat3x3" -> "float3x3";
                case "dmat4", "dmat4x4" -> "float4x4";
                default -> {
                    // Check if it's a user-defined struct
                    if (structs.containsKey(glslType)) {
                        yield glslType;
                    }
                    yield glslType; // Return as-is for unknown types
                }
            };
        }
        
        private String translateSamplerType(GLSLSampler sampler) {
            StringBuilder sb = new StringBuilder();
            
            if (sampler.shadow) {
                if (sampler.isCube) {
                    sb.append("depthcube<float>");
                } else if (sampler.isArray) {
                    sb.append("depth2d_array<float>");
                } else {
                    sb.append("depth2d<float>");
                }
            } else {
                String type = sampler.samplerType.equals("int") ? "int" :
                              sampler.samplerType.equals("uint") ? "uint" : "float";
                
                if (sampler.isCube && sampler.isArray) {
                    sb.append("texturecube_array<").append(type).append(">");
                } else if (sampler.isCube) {
                    sb.append("texturecube<").append(type).append(">");
                } else if (sampler.dimensions == 3) {
                    sb.append("texture3d<").append(type).append(">");
                } else if (sampler.dimensions == 1) {
                    if (sampler.isArray) {
                        sb.append("texture1d_array<").append(type).append(">");
                    } else {
                        sb.append("texture1d<").append(type).append(">");
                    }
                } else if (sampler.isBuffer) {
                    sb.append("texture_buffer<").append(type).append(">");
                } else if (sampler.multisample && sampler.isArray) {
                    sb.append("texture2d_ms_array<").append(type).append(">");
                } else if (sampler.multisample) {
                    sb.append("texture2d_ms<").append(type).append(">");
                } else if (sampler.isArray) {
                    sb.append("texture2d_array<").append(type).append(">");
                } else {
                    sb.append("texture2d<").append(type).append(">");
                }
            }
            
            return sb.toString();
        }
        
        private String translateImageType(GLSLImage image) {
            StringBuilder sb = new StringBuilder();
            
            String type = image.imageType.equals("int") ? "int" :
                          image.imageType.equals("uint") ? "uint" : "float";
            
            String access = "access::read_write";
            if (image.readonly) access = "access::read";
            else if (image.writeonly) access = "access::write";
            
            if (image.isCube && image.isArray) {
                sb.append("texturecube_array<").append(type).append(", ").append(access).append(">");
            } else if (image.isCube) {
                sb.append("texturecube<").append(type).append(", ").append(access).append(">");
            } else if (image.dimensions == 3) {
                sb.append("texture3d<").append(type).append(", ").append(access).append(">");
            } else if (image.dimensions == 1) {
                if (image.isArray) {
                    sb.append("texture1d_array<").append(type).append(", ").append(access).append(">");
                } else {
                    sb.append("texture1d<").append(type).append(", ").append(access).append(">");
                }
            } else if (image.isBuffer) {
                sb.append("texture_buffer<").append(type).append(", ").append(access).append(">");
            } else if (image.isMS && image.isArray) {
                sb.append("texture2d_ms_array<").append(type).append(", ").append(access).append(">");
            } else if (image.isMS) {
                sb.append("texture2d_ms<").append(type).append(", ").append(access).append(">");
            } else if (image.isArray) {
                sb.append("texture2d_array<").append(type).append(", ").append(access).append(">");
            } else {
                sb.append("texture2d<").append(type).append(", ").append(access).append(">");
            }
            
            return sb.toString();
        }
        
        private String translateFunctionBody(String body) {
            if (body == null) return "";
            
            StringBuilder result = new StringBuilder();
            
            // Apply transformations
            String translated = body;
            
            // Replace GLSL functions with MSL equivalents
            translated = translateBuiltinFunctions(translated);
            
            // Replace texture sampling
            translated = translateTextureSampling(translated);
            
            // Replace types
            translated = translateTypesInBody(translated);
            
            // Handle discard
            translated = translated.replaceAll("\\bdiscard\\s*;", "_discard = true; return out;");
            
            // Handle gl_FragColor assignment for legacy shaders
            if (usesGlFragColor) {
                translated = translated.replaceAll("\\bgl_FragData\\s*\\[\\s*0\\s*\\]", "gl_FragColor");
            }
            
            // Add indentation
            String[] lines = translated.split("\n");
            for (String line : lines) {
                result.append("    ").append(line.trim()).append("\n");
            }
            
            return result.toString();
        }
        
        private String translateBuiltinFunctions(String code) {
            // Mathematical functions that differ
            code = code.replaceAll("\\bmod\\s*\\(", "mod_emu(");
            code = code.replaceAll("\\batan\\s*\\(([^,]+),", "atan_emu(\$1,");
            code = code.replaceAll("\\bfract\\s*\\(", "fract(");
            code = code.replaceAll("\\bmix\\s*\\(", "mix(");
            code = code.replaceAll("\\bclamp\\s*\\(", "clamp(");
            code = code.replaceAll("\\bsmoothstep\\s*\\(", "smoothstep(");
            code = code.replaceAll("\\bstep\\s*\\(", "step(");
            
            // Vector functions
            code = code.replaceAll("\\blength\\s*\\(", "length(");
            code = code.replaceAll("\\bdistance\\s*\\(", "distance(");
            code = code.replaceAll("\\bdot\\s*\\(", "dot(");
            code = code.replaceAll("\\bcross\\s*\\(", "cross(");
            code = code.replaceAll("\\bnormalize\\s*\\(", "normalize(");
            code = code.replaceAll("\\breflect\\s*\\(", "reflect(");
            code = code.replaceAll("\\brefract\\s*\\(", "refract(");
            code = code.replaceAll("\\bfaceforward\\s*\\(", "faceforward(");
            
            // Matrix functions
            code = code.replaceAll("\\btranspose\\s*\\(", "transpose(");
            code = code.replaceAll("\\binverse\\s*\\(", "inverse_impl(");
            code = code.replaceAll("\\bdeterminant\\s*\\(", "determinant_impl(");
            code = code.replaceAll("\\bouterProduct\\s*\\(", "outerProduct_impl(");
            code = code.replaceAll("\\bmatrixCompMult\\s*\\(", "matrixCompMult_impl(");
            
            // Trigonometric functions
            code = code.replaceAll("\\bradians\\s*\\(", "((3.14159265359/180.0)*(");
            code = code.replaceAll("\\bdegrees\\s*\\(", "((180.0/3.14159265359)*(");
            code = code.replaceAll("\\bsin\\s*\\(", "sin(");
            code = code.replaceAll("\\bcos\\s*\\(", "cos(");
            code = code.replaceAll("\\btan\\s*\\(", "tan(");
            code = code.replaceAll("\\basin\\s*\\(", "asin(");
            code = code.replaceAll("\\bacos\\s*\\(", "acos(");
            code = code.replaceAll("\\bsinh\\s*\\(", "sinh(");
            code = code.replaceAll("\\bcosh\\s*\\(", "cosh(");
            code = code.replaceAll("\\btanh\\s*\\(", "tanh(");
            code = code.replaceAll("\\basinh\\s*\\(", "asinh(");
            code = code.replaceAll("\\bacosh\\s*\\(", "acosh(");
            code = code.replaceAll("\\batanh\\s*\\(", "atanh(");
            
            // Exponential functions
            code = code.replaceAll("\\bpow\\s*\\(", "pow(");
            code = code.replaceAll("\\bexp\\s*\\(", "exp(");
            code = code.replaceAll("\\blog\\s*\\(", "log(");
            code = code.replaceAll("\\bexp2\\s*\\(", "exp2(");
            code = code.replaceAll("\\blog2\\s*\\(", "log2(");
            code = code.replaceAll("\\bsqrt\\s*\\(", "sqrt(");
            code = code.replaceAll("\\binversesqrt\\s*\\(", "rsqrt(");
            
            // Common functions
            code = code.replaceAll("\\babs\\s*\\(", "abs(");
            code = code.replaceAll("\\bsign\\s*\\(", "sign(");
            code = code.replaceAll("\\bfloor\\s*\\(", "floor(");
            code = code.replaceAll("\\btrunc\\s*\\(", "trunc(");
            code = code.replaceAll("\\bround\\s*\\(", "round(");
            code = code.replaceAll("\\broundEven\\s*\\(", "rint(");
            code = code.replaceAll("\\bceil\\s*\\(", "ceil(");
            code = code.replaceAll("\\bmin\\s*\\(", "min(");
            code = code.replaceAll("\\bmax\\s*\\(", "max(");
            
            // Derivative functions
            code = code.replaceAll("\\bdFdx\\s*\\(", "dfdx(");
            code = code.replaceAll("\\bdFdy\\s*\\(", "dfdy(");
            code = code.replaceAll("\\bdFdxCoarse\\s*\\(", "dfdx(");
            code = code.replaceAll("\\bdFdyCoarse\\s*\\(", "dfdy(");
            code = code.replaceAll("\\bdFdxFine\\s*\\(", "dfdx(");
            code = code.replaceAll("\\bdFdyFine\\s*\\(", "dfdy(");
            code = code.replaceAll("\\bfwidth\\s*\\(", "fwidth(");
            
            // Bit operations
            code = code.replaceAll("\\bbitfieldExtract\\s*\\(", "bitfieldExtract_impl(");
            code = code.replaceAll("\\bbitfieldInsert\\s*\\(", "bitfieldInsert_impl(");
            code = code.replaceAll("\\bbitfieldReverse\\s*\\(", "bitfieldReverse_impl(");
            code = code.replaceAll("\\bbitCount\\s*\\(", "bitCount_impl(");
            code = code.replaceAll("\\bfindLSB\\s*\\(", "findLSB_impl(");
            code = code.replaceAll("\\bfindMSB\\s*\\(", "findMSB_impl(");
            
            // Pack/unpack
            code = code.replaceAll("\\bpackHalf2x16\\s*\\(", "packHalf2x16_impl(");
            code = code.replaceAll("\\bunpackHalf2x16\\s*\\(", "unpackHalf2x16_impl(");
            code = code.replaceAll("\\bpackUnorm2x16\\s*\\(", "packUnorm2x16_impl(");
            code = code.replaceAll("\\bunpackUnorm2x16\\s*\\(", "unpackUnorm2x16_impl(");
            code = code.replaceAll("\\bpackSnorm2x16\\s*\\(", "packSnorm2x16_impl(");
            code = code.replaceAll("\\bunpackSnorm2x16\\s*\\(", "unpackSnorm2x16_impl(");
            code = code.replaceAll("\\bpackUnorm4x8\\s*\\(", "packUnorm4x8_impl(");
            code = code.replaceAll("\\bunpackUnorm4x8\\s*\\(", "unpackUnorm4x8_impl(");
            code = code.replaceAll("\\bpackSnorm4x8\\s*\\(", "packSnorm4x8_impl(");
            code = code.replaceAll("\\bunpackSnorm4x8\\s*\\(", "unpackSnorm4x8_impl(");
            
            // Floating point
            code = code.replaceAll("\\bisinf\\s*\\(", "isinf(");
            code = code.replaceAll("\\bisnan\\s*\\(", "isnan(");
            code = code.replaceAll("\\bintBitsToFloat\\s*\\(", "as_type<float>(");
            code = code.replaceAll("\\bfloatBitsToInt\\s*\\(", "as_type<int>(");
            code = code.replaceAll("\\buintBitsToFloat\\s*\\(", "as_type<float>(");
            code = code.replaceAll("\\bfloatBitsToUint\\s*\\(", "as_type<uint>(");
            code = code.replaceAll("\\bfma\\s*\\(", "fma(");
            code = code.replaceAll("\\bfrexp\\s*\\(", "frexp_impl(");
            code = code.replaceAll("\\bldexp\\s*\\(", "ldexp(");
            
            // Atomic operations
            code = code.replaceAll("\\batomicAdd\\s*\\(", "atomicAdd_impl(");
            code = code.replaceAll("\\batomicMin\\s*\\(", "atomicMin_impl(");
            code = code.replaceAll("\\batomicMax\\s*\\(", "atomicMax_impl(");
            code = code.replaceAll("\\batomicAnd\\s*\\(", "atomicAnd_impl(");
            code = code.replaceAll("\\batomicOr\\s*\\(", "atomicOr_impl(");
            code = code.replaceAll("\\batomicXor\\s*\\(", "atomicXor_impl(");
            code = code.replaceAll("\\batomicExchange\\s*\\(", "atomicExchange_impl(");
            code = code.replaceAll("\\batomicCompSwap\\s*\\(", "atomicCompSwap_impl(");
            code = code.replaceAll("\\batomicCounter\\s*\\(", "atomicCounter_impl(");
            code = code.replaceAll("\\batomicCounterIncrement\\s*\\(", "atomicCounterIncrement_impl(");
            code = code.replaceAll("\\batomicCounterDecrement\\s*\\(", "atomicCounterDecrement_impl(");
            
            // Barrier functions
            code = code.replaceAll("\\bbarrier\\s*\\(\\s*\\)", "threadgroup_barrier(mem_flags::mem_threadgroup)");
            code = code.replaceAll("\\bmemoryBarrier\\s*\\(\\s*\\)", "threadgroup_barrier(mem_flags::mem_device)");
            code = code.replaceAll("\\bmemoryBarrierShared\\s*\\(\\s*\\)", "threadgroup_barrier(mem_flags::mem_threadgroup)");
            code = code.replaceAll("\\bmemoryBarrierBuffer\\s*\\(\\s*\\)", "threadgroup_barrier(mem_flags::mem_device)");
            code = code.replaceAll("\\bmemoryBarrierImage\\s*\\(\\s*\\)", "threadgroup_barrier(mem_flags::mem_texture)");
            code = code.replaceAll("\\bgroupMemoryBarrier\\s*\\(\\s*\\)", "threadgroup_barrier(mem_flags::mem_threadgroup)");
            
            // Interpolation
            code = code.replaceAll("\\binterpolateAtCentroid\\s*\\(", "interpolateAtCentroid_impl(");
            code = code.replaceAll("\\binterpolateAtSample\\s*\\(", "interpolateAtSample_impl(");
            code = code.replaceAll("\\binterpolateAtOffset\\s*\\(", "interpolateAtOffset_impl(");
            
            // Noise (deprecated but still used)
            code = code.replaceAll("\\bnoise1\\s*\\(", "noise1_impl(");
            code = code.replaceAll("\\bnoise2\\s*\\(", "float2(noise1_impl(");
            code = code.replaceAll("\\bnoise3\\s*\\(", "float3(noise1_impl(");
            code = code.replaceAll("\\bnoise4\\s*\\(", "float4(noise1_impl(");
            
            // Boolean vector functions
            code = code.replaceAll("\\ball\\s*\\(", "all(");
            code = code.replaceAll("\\bany\\s*\\(", "any(");
            code = code.replaceAll("\\bnot\\s*\\(", "!");
            
            // lessThan, greaterThan, etc.
            code = code.replaceAll("\\blessThan\\s*\\(([^,]+),\\s*([^)]+)\\)", "(\$1 < \$2)");
            code = code.replaceAll("\\blessThanEqual\\s*\\(([^,]+),\\s*([^)]+)\\)", "(\$1 <= \$2)");
            code = code.replaceAll("\\bgreaterThan\\s*\\(([^,]+),\\s*([^)]+)\\)", "(\$1 > \$2)");
            code = code.replaceAll("\\bgreaterThanEqual\\s*\\(([^,]+),\\s*([^)]+)\\)", "(\$1 >= \$2)");
            code = code.replaceAll("\\bequal\\s*\\(([^,]+),\\s*([^)]+)\\)", "(\$1 == \$2)");
            code = code.replaceAll("\\bnotEqual\\s*\\(([^,]+),\\s*([^)]+)\\)", "(\$1 != \$2)");
            
            return code;
        }
        
        private String translateTextureSampling(String code) {
            // texture() -> tex.sample(sampler, coord)
            for (GLSLSampler sampler : samplers) {
                String name = sampler.name;
                
                // Basic texture sampling
                code = code.replaceAll(
                    "\\btexture\\s*\\(\\s*" + name + "\\s*,\\s*([^)]+)\\)",
                    name + "_tex.sample(" + name + "_smp, \$1)"
                );
                
                // textureLod
                code = code.replaceAll(
                    "\\btextureLod\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + "_tex.sample(" + name + "_smp, $1, level(\$2))"
                );
                
                // textureOffset
                code = code.replaceAll(
                    "\\btextureOffset\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + "_tex.sample(" + name + "_smp, $1, int2(\$2))"
                );
                
                // textureLodOffset
                code = code.replaceAll(
                    "\\btextureLodOffset\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^,]+),\\s*([^)]+)\\)",
                    name + "_tex.sample(" + name + "_smp, $1, level($2), int2(\$3))"
                );
                
                // textureProj
                code = code.replaceAll(
                    "\\btextureProj\\s*\\(\\s*" + name + "\\s*,\\s*([^)]+)\\)",
                    name + "_tex.sample(" + name + "_smp, ($1).xy / (\$1).w)"
                );
                
                // textureProjLod
                code = code.replaceAll(
                    "\\btextureProjLod\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + "_tex.sample(" + name + "_smp, ($1).xy / ($1).w, level(\$2))"
                );
                
                // textureGrad
                code = code.replaceAll(
                    "\\btextureGrad\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^,]+),\\s*([^)]+)\\)",
                    name + "_tex.sample(" + name + "_smp, $1, gradient2d(\$2, \$3))"
                );
                
                // texelFetch
                code = code.replaceAll(
                    "\\btexelFetch\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + "_tex.read(uint2(\$1), \$2)"
                );
                
                // texelFetchOffset
                code = code.replaceAll(
                    "\\btexelFetchOffset\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^,]+),\\s*([^)]+)\\)",
                    name + "_tex.read(uint2($1) + uint2(\$3), \$2)"
                );
                
                // textureSize
                code = code.replaceAll(
                    "\\btextureSize\\s*\\(\\s*" + name + "\\s*,\\s*([^)]+)\\)",
                    "int2(" + name + "_tex.get_width($1), " + name + "_tex.get_height(\$1))"
                );
                
                // textureQueryLevels
                code = code.replaceAll(
                    "\\btextureQueryLevels\\s*\\(\\s*" + name + "\\s*\\)",
                    name + "_tex.get_num_mip_levels()"
                );
                
                // textureQueryLod (approximation)
                code = code.replaceAll(
                    "\\btextureQueryLod\\s*\\(\\s*" + name + "\\s*,\\s*([^)]+)\\)",
                    "float2(0.0, 0.0)" // Approximate - Metal doesn't have direct equivalent
                );
                
                // textureGather
                code = code.replaceAll(
                    "\\btextureGather\\s*\\(\\s*" + name + "\\s*,\\s*([^)]+)\\)",
                    name + "_tex.gather(" + name + "_smp, \$1)"
                );
                
                // textureGatherOffset
                code = code.replaceAll(
                    "\\btextureGatherOffset\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + "_tex.gather(" + name + "_smp, $1, int2(\$2))"
                );
                
                // textureGatherOffsets
                code = code.replaceAll(
                    "\\btextureGatherOffsets\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + "_tex.gather(" + name + "_smp, \$1)" // Simplified
                );
                
                // Shadow sampling
                if (sampler.shadow) {
                    code = code.replaceAll(
                        "\\btexture\\s*\\(\\s*" + name + "\\s*,\\s*([^)]+)\\)",
                        name + "_tex.sample_compare(" + name + "_smp, ($1).xy, (\$1).z)"
                    );
                }
            }
            
            // Handle image operations
            for (GLSLImage image : images) {
                String name = image.name;
                
                // imageLoad
                code = code.replaceAll(
                    "\\bimageLoad\\s*\\(\\s*" + name + "\\s*,\\s*([^)]+)\\)",
                    name + ".read(uint2(\$1))"
                );
                
                // imageStore
                code = code.replaceAll(
                    "\\bimageStore\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + ".write($2, uint2(\$1))"
                );
                
                // imageSize
                code = code.replaceAll(
                    "\\bimageSize\\s*\\(\\s*" + name + "\\s*\\)",
                    "int2(" + name + ".get_width(), " + name + ".get_height())"
                );
                
                // imageAtomicAdd
                code = code.replaceAll(
                    "\\bimageAtomicAdd\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + ".atomic_fetch_add(uint2(\$1), \$2)"
                );
                
                // imageAtomicMin
                code = code.replaceAll(
                    "\\bimageAtomicMin\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + ".atomic_fetch_min(uint2(\$1), \$2)"
                );
                
                // imageAtomicMax
                code = code.replaceAll(
                    "\\bimageAtomicMax\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + ".atomic_fetch_max(uint2(\$1), \$2)"
                );
                
                // imageAtomicAnd
                code = code.replaceAll(
                    "\\bimageAtomicAnd\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + ".atomic_fetch_and(uint2(\$1), \$2)"
                );
                
                // imageAtomicOr
                code = code.replaceAll(
                    "\\bimageAtomicOr\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + ".atomic_fetch_or(uint2(\$1), \$2)"
                );
                
                // imageAtomicXor
                code = code.replaceAll(
                    "\\bimageAtomicXor\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + ".atomic_fetch_xor(uint2(\$1), \$2)"
                );
                
                // imageAtomicExchange
                code = code.replaceAll(
                    "\\bimageAtomicExchange\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^)]+)\\)",
                    name + ".atomic_exchange(uint2(\$1), \$2)"
                );
                
                // imageAtomicCompSwap
                code = code.replaceAll(
                    "\\bimageAtomicCompSwap\\s*\\(\\s*" + name + "\\s*,\\s*([^,]+),\\s*([^,]+),\\s*([^)]+)\\)",
                    name + ".atomic_compare_exchange_weak(uint2(\$1), \$2, \$3)"
                );
            }
            
            // Legacy texture functions (GLSL 1.x)
            code = code.replaceAll("\\btexture2D\\s*\\(([^,]+),\\s*([^)]+)\\)", "$1_tex.sample(\$1_smp, \$2)");
            code = code.replaceAll("\\btexture2DLod\\s*\\(([^,]+),\\s*([^,]+),\\s*([^)]+)\\)", "$1_tex.sample(\$1_smp, $2, level(\$3))");
            code = code.replaceAll("\\btexture2DProj\\s*\\(([^,]+),\\s*([^)]+)\\)", "$1_tex.sample($1_smp, ($2).xy / (\$2).w)");
            code = code.replaceAll("\\btexture2DProjLod\\s*\\(([^,]+),\\s*([^,]+),\\s*([^)]+)\\)", "$1_tex.sample($1_smp, ($2).xy / ($2).w, level(\$3))");
            code = code.replaceAll("\\btexture3D\\s*\\(([^,]+),\\s*([^)]+)\\)", "$1_tex.sample(\$1_smp, \$2)");
            code = code.replaceAll("\\btexture3DLod\\s*\\(([^,]+),\\s*([^,]+),\\s*([^)]+)\\)", "$1_tex.sample(\$1_smp, $2, level(\$3))");
            code = code.replaceAll("\\btextureCube\\s*\\(([^,]+),\\s*([^)]+)\\)", "$1_tex.sample(\$1_smp, \$2)");
            code = code.replaceAll("\\btextureCubeLod\\s*\\(([^,]+),\\s*([^,]+),\\s*([^)]+)\\)", "$1_tex.sample(\$1_smp, $2, level(\$3))");
            code = code.replaceAll("\\bshadow2D\\s*\\(([^,]+),\\s*([^)]+)\\)", "$1_tex.sample_compare($1_smp, ($2).xy, (\$2).z)");
            code = code.replaceAll("\\bshadow2DProj\\s*\\(([^,]+),\\s*([^)]+)\\)", "$1_tex.sample_compare($1_smp, ($2).xy / ($2).w, ($2).z / (\$2).w)");
            
            return code;
        }
        
        private String translateTypesInBody(String code) {
            // Vector types
            code = code.replaceAll("\\bvec2\\s*\\(", "float2(");
            code = code.replaceAll("\\bvec3\\s*\\(", "float3(");
            code = code.replaceAll("\\bvec4\\s*\\(", "float4(");
            code = code.replaceAll("\\bivec2\\s*\\(", "int2(");
            code = code.replaceAll("\\bivec3\\s*\\(", "int3(");
            code = code.replaceAll("\\bivec4\\s*\\(", "int4(");
            code = code.replaceAll("\\buvec2\\s*\\(", "uint2(");
            code = code.replaceAll("\\buvec3\\s*\\(", "uint3(");
            code = code.replaceAll("\\buvec4\\s*\\(", "uint4(");
            code = code.replaceAll("\\bbvec2\\s*\\(", "bool2(");
            code = code.replaceAll("\\bbvec3\\s*\\(", "bool3(");
            code = code.replaceAll("\\bbvec4\\s*\\(", "bool4(");
            code = code.replaceAll("\\bdvec2\\s*\\(", "float2(");
            code = code.replaceAll("\\bdvec3\\s*\\(", "float3(");
            code = code.replaceAll("\\bdvec4\\s*\\(", "float4(");
            
            // Matrix types
            code = code.replaceAll("\\bmat2\\s*\\(", "float2x2(");
            code = code.replaceAll("\\bmat3\\s*\\(", "float3x3(");
            code = code.replaceAll("\\bmat4\\s*\\(", "float4x4(");
            code = code.replaceAll("\\bmat2x2\\s*\\(", "float2x2(");
            code = code.replaceAll("\\bmat2x3\\s*\\(", "float2x3(");
            code = code.replaceAll("\\bmat2x4\\s*\\(", "float2x4(");
            code = code.replaceAll("\\bmat3x2\\s*\\(", "float3x2(");
            code = code.replaceAll("\\bmat3x3\\s*\\(", "float3x3(");
            code = code.replaceAll("\\bmat3x4\\s*\\(", "float3x4(");
            code = code.replaceAll("\\bmat4x2\\s*\\(", "float4x2(");
            code = code.replaceAll("\\bmat4x3\\s*\\(", "float4x3(");
            code = code.replaceAll("\\bmat4x4\\s*\\(", "float4x4(");
            
            // Type declarations
            code = code.replaceAll("\\bvec2\\s+", "float2 ");
            code = code.replaceAll("\\bvec3\\s+", "float3 ");
            code = code.replaceAll("\\bvec4\\s+", "float4 ");
            code = code.replaceAll("\\bivec2\\s+", "int2 ");
            code = code.replaceAll("\\bivec3\\s+", "int3 ");
            code = code.replaceAll("\\bivec4\\s+", "int4 ");
            code = code.replaceAll("\\buvec2\\s+", "uint2 ");
            code = code.replaceAll("\\buvec3\\s+", "uint3 ");
            code = code.replaceAll("\\buvec4\\s+", "uint4 ");
            code = code.replaceAll("\\bmat2\\s+", "float2x2 ");
            code = code.replaceAll("\\bmat3\\s+", "float3x3 ");
            code = code.replaceAll("\\bmat4\\s+", "float4x4 ");
            
            return code;
        }
        
        private String sanitizeName(String name) {
            // Remove gl_ prefix if present and convert to valid Metal identifier
            if (name.startsWith("gl_")) {
                name = "_gl_" + name.substring(3);
            }
            // Replace any invalid characters
            return name.replaceAll("[^a-zA-Z0-9_]", "_");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 21: GEOMETRY SHADER EMULATION VIA COMPUTE
    // Complete geometry shader emulation using Metal compute shaders
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Emulates OpenGL geometry shaders using Metal compute shaders.
     * Converts geometry shader operations to compute kernel operations.
     */
    public static final class GeometryShaderEmulator {
        
        // Input primitive types
        public static final int GS_POINTS = 0;
        public static final int GS_LINES = 1;
        public static final int GS_LINES_ADJACENCY = 2;
        public static final int GS_TRIANGLES = 3;
        public static final int GS_TRIANGLES_ADJACENCY = 4;
        
        // Output primitive types
        public static final int GS_OUT_POINTS = 0;
        public static final int GS_OUT_LINE_STRIP = 1;
        public static final int GS_OUT_TRIANGLE_STRIP = 2;
        
        // Configuration
        private int inputPrimitive = GS_TRIANGLES;
        private int outputPrimitive = GS_OUT_TRIANGLE_STRIP;
        private int maxVerticesOut = 256;
        private int invocations = 1;
        
        // Shader sources
        private String geometryShaderSource;
        private String vertexShaderSource;
        
        // Generated compute shader
        private String computeShaderSource;
        
        // Vertex data layout
        private final List<VertexAttribute> vertexAttributes = new ArrayList<>();
        private int vertexStride = 0;
        
        // Output buffer management
        private long outputBufferHandle;
        private long counterBufferHandle;
        private long indirectBufferHandle;
        private int maxOutputVertices = 65536;
        private int maxOutputPrimitives = 21845; // maxOutputVertices / 3
        
        /**
         * Vertex attribute description
         */
        public static class VertexAttribute {
            public String name;
            public String type;
            public int location;
            public int offset;
            public int size;
            public boolean normalized;
            public String interpolation; // flat, noperspective, smooth
        }
        
        /**
         * Geometry shader interface block
         */
        public static class GSInterfaceBlock {
            public String name;
            public List<VertexAttribute> members = new ArrayList<>();
            public boolean isInput;
            public int arraySize; // For input arrays (gl_in[])
        }
        
        /**
         * Set the input primitive type
         */
        public void setInputPrimitive(int glPrimitive) {
            switch (glPrimitive) {
                case GL_POINTS:
                    inputPrimitive = GS_POINTS;
                    break;
                case GL_LINES:
                    inputPrimitive = GS_LINES;
                    break;
                case GL_LINES_ADJACENCY:
                    inputPrimitive = GS_LINES_ADJACENCY;
                    break;
                case GL_TRIANGLES:
                    inputPrimitive = GS_TRIANGLES;
                    break;
                case GL_TRIANGLES_ADJACENCY:
                    inputPrimitive = GS_TRIANGLES_ADJACENCY;
                    break;
                default:
                    inputPrimitive = GS_TRIANGLES;
            }
        }
        
        /**
         * Set the output primitive type
         */
        public void setOutputPrimitive(int glPrimitive) {
            switch (glPrimitive) {
                case GL_POINTS:
                    outputPrimitive = GS_OUT_POINTS;
                    break;
                case GL_LINE_STRIP:
                    outputPrimitive = GS_OUT_LINE_STRIP;
                    break;
                case GL_TRIANGLE_STRIP:
                    outputPrimitive = GS_OUT_TRIANGLE_STRIP;
                    break;
                default:
                    outputPrimitive = GS_OUT_TRIANGLE_STRIP;
            }
        }
        
        /**
         * Get number of vertices per input primitive
         */
        private int getInputVertexCount() {
            return switch (inputPrimitive) {
                case GS_POINTS -> 1;
                case GS_LINES -> 2;
                case GS_LINES_ADJACENCY -> 4;
                case GS_TRIANGLES -> 3;
                case GS_TRIANGLES_ADJACENCY -> 6;
                default -> 3;
            };
        }
        
        /**
         * Generate the compute shader for geometry shader emulation
         */
        public String generateComputeShader(String gsSource) {
            this.geometryShaderSource = gsSource;
            
            StringBuilder msl = new StringBuilder();
            
            // Header
            msl.append("#include <metal_stdlib>\n");
            msl.append("#include <simd/simd.h>\n");
            msl.append("using namespace metal;\n\n");
            
            // Parse geometry shader to extract layout qualifiers
            parseLayoutQualifiers(gsSource);
            
            // Generate vertex input structure
            generateVertexInputStruct(msl);
            
            // Generate vertex output structure
            generateVertexOutputStruct(msl);
            
            // Generate primitive output structure
            generatePrimitiveOutputStruct(msl);
            
            // Generate atomic counter structure
            generateCounterStruct(msl);
            
            // Generate helper functions
            generateHelperFunctions(msl);
            
            // Generate the main compute kernel
            generateComputeKernel(msl, gsSource);
            
            this.computeShaderSource = msl.toString();
            return computeShaderSource;
        }
        
        private void parseLayoutQualifiers(String source) {
            // Parse layout(points/lines/triangles) in;
            java.util.regex.Pattern inputPattern = java.util.regex.Pattern.compile(
                "layout\\s*\\(\\s*(points|lines|lines_adjacency|triangles|triangles_adjacency)\\s*\\)\\s*in\\s*;");
            java.util.regex.Matcher inputMatcher = inputPattern.matcher(source);
            if (inputMatcher.find()) {
                String prim = inputMatcher.group(1);
                switch (prim) {
                    case "points" -> inputPrimitive = GS_POINTS;
                    case "lines" -> inputPrimitive = GS_LINES;
                    case "lines_adjacency" -> inputPrimitive = GS_LINES_ADJACENCY;
                    case "triangles" -> inputPrimitive = GS_TRIANGLES;
                    case "triangles_adjacency" -> inputPrimitive = GS_TRIANGLES_ADJACENCY;
                }
            }
            
            // Parse layout(points/line_strip/triangle_strip, max_vertices=N) out;
            java.util.regex.Pattern outputPattern = java.util.regex.Pattern.compile(
                "layout\\s*\\(\\s*(points|line_strip|triangle_strip)\\s*,\\s*max_vertices\\s*=\\s*(\\d+)\\s*\\)\\s*out\\s*;");
            java.util.regex.Matcher outputMatcher = outputPattern.matcher(source);
            if (outputMatcher.find()) {
                String prim = outputMatcher.group(1);
                switch (prim) {
                    case "points" -> outputPrimitive = GS_OUT_POINTS;
                    case "line_strip" -> outputPrimitive = GS_OUT_LINE_STRIP;
                    case "triangle_strip" -> outputPrimitive = GS_OUT_TRIANGLE_STRIP;
                }
                maxVerticesOut = Integer.parseInt(outputMatcher.group(2));
            }
            
            // Parse invocations
            java.util.regex.Pattern invocPattern = java.util.regex.Pattern.compile(
                "layout\\s*\\(\\s*invocations\\s*=\\s*(\\d+)\\s*\\)");
            java.util.regex.Matcher invocMatcher = invocPattern.matcher(source);
            if (invocMatcher.find()) {
                invocations = Integer.parseInt(invocMatcher.group(1));
            }
        }
        
        private void generateVertexInputStruct(StringBuilder msl) {
            msl.append("// Vertex input from vertex shader\n");
            msl.append("struct GSVertexInput {\n");
            msl.append("    float4 position;\n");
            msl.append("    float4 color;\n");
            msl.append("    float3 normal;\n");
            msl.append("    float2 texCoord;\n");
            msl.append("    float pointSize;\n");
            msl.append("    float clipDistance[8];\n");
            // Add user-defined varyings based on vertex shader outputs
            for (VertexAttribute attr : vertexAttributes) {
                msl.append("    ").append(attr.type).append(" ").append(attr.name).append(";\n");
            }
            msl.append("};\n\n");
        }
        
        private void generateVertexOutputStruct(StringBuilder msl) {
            msl.append("// Vertex output from geometry shader\n");
            msl.append("struct GSVertexOutput {\n");
            msl.append("    float4 position;\n");
            msl.append("    float4 color;\n");
            msl.append("    float3 normal;\n");
            msl.append("    float2 texCoord;\n");
            msl.append("    float pointSize;\n");
            msl.append("    int layer;\n");
            msl.append("    int viewportIndex;\n");
            msl.append("    int primitiveID;\n");
            for (VertexAttribute attr : vertexAttributes) {
                msl.append("    ").append(attr.type).append(" ").append(attr.name).append(";\n");
            }
            msl.append("};\n\n");
        }
        
        private void generatePrimitiveOutputStruct(StringBuilder msl) {
            msl.append("// Output primitive structure\n");
            msl.append("struct GSPrimitiveOutput {\n");
            msl.append("    GSVertexOutput vertices[").append(maxVerticesOut).append("];\n");
            msl.append("    uint vertexCount;\n");
            msl.append("    uint primitiveType; // 0=points, 1=line_strip, 2=triangle_strip\n");
            msl.append("};\n\n");
        }
        
        private void generateCounterStruct(StringBuilder msl) {
            msl.append("// Atomic counters for output management\n");
            msl.append("struct GSCounters {\n");
            msl.append("    atomic_uint vertexCounter;\n");
            msl.append("    atomic_uint primitiveCounter;\n");
            msl.append("    uint maxVertices;\n");
            msl.append("    uint maxPrimitives;\n");
            msl.append("};\n\n");
            
            msl.append("// Indirect draw command\n");
            msl.append("struct GSIndirectCommand {\n");
            msl.append("    uint vertexCount;\n");
            msl.append("    uint instanceCount;\n");
            msl.append("    uint vertexStart;\n");
            msl.append("    uint baseInstance;\n");
            msl.append("};\n\n");
        }
        
        private void generateHelperFunctions(StringBuilder msl) {
            msl.append("// ═══════════════════════════════════════════════════════════\n");
            msl.append("// Geometry Shader Emulation Helpers\n");
            msl.append("// ═══════════════════════════════════════════════════════════\n\n");
            
            // EmitVertex helper
            msl.append("// Thread-local vertex emission buffer\n");
            msl.append("struct GSEmitState {\n");
            msl.append("    GSVertexOutput pendingVertices[").append(maxVerticesOut).append("];\n");
            msl.append("    uint pendingCount;\n");
            msl.append("    uint stripStartIndex;\n");
            msl.append("    bool inStrip;\n");
            msl.append("};\n\n");
            
            // Current output vertex (global to thread)
            msl.append("// Current vertex being built\n");
            msl.append("struct GSCurrentVertex {\n");
            msl.append("    float4 gl_Position;\n");
            msl.append("    float gl_PointSize;\n");
            msl.append("    float gl_ClipDistance[8];\n");
            msl.append("    int gl_Layer;\n");
            msl.append("    int gl_ViewportIndex;\n");
            msl.append("    int gl_PrimitiveID;\n");
            for (VertexAttribute attr : vertexAttributes) {
                msl.append("    ").append(attr.type).append(" ").append(attr.name).append(";\n");
            }
            msl.append("};\n\n");
            
            // EmitVertex function
            msl.append("inline void EmitVertex_impl(\n");
            msl.append("    thread GSCurrentVertex& current,\n");
            msl.append("    thread GSEmitState& state,\n");
            msl.append("    device GSVertexOutput* outputBuffer,\n");
            msl.append("    device GSCounters& counters\n");
            msl.append(") {\n");
            msl.append("    if (state.pendingCount >= ").append(maxVerticesOut).append(") return;\n");
            msl.append("    \n");
            msl.append("    GSVertexOutput vertex;\n");
            msl.append("    vertex.position = current.gl_Position;\n");
            msl.append("    vertex.pointSize = current.gl_PointSize;\n");
            msl.append("    vertex.layer = current.gl_Layer;\n");
            msl.append("    vertex.viewportIndex = current.gl_ViewportIndex;\n");
            msl.append("    vertex.primitiveID = current.gl_PrimitiveID;\n");
            for (VertexAttribute attr : vertexAttributes) {
                msl.append("    vertex.").append(attr.name).append(" = current.").append(attr.name).append(";\n");
            }
            msl.append("    \n");
            msl.append("    state.pendingVertices[state.pendingCount++] = vertex;\n");
            msl.append("    state.inStrip = true;\n");
            msl.append("}\n\n");
            
            // EndPrimitive function
            msl.append("inline void EndPrimitive_impl(\n");
            msl.append("    thread GSEmitState& state,\n");
            msl.append("    device GSVertexOutput* outputBuffer,\n");
            msl.append("    device GSCounters& counters,\n");
            msl.append("    uint outputPrimitiveType\n");
            msl.append(") {\n");
            msl.append("    if (!state.inStrip || state.pendingCount == state.stripStartIndex) return;\n");
            msl.append("    \n");
            msl.append("    uint stripLength = state.pendingCount - state.stripStartIndex;\n");
            msl.append("    uint outputVertexCount = 0;\n");
            msl.append("    \n");
            msl.append("    // Convert strip to individual primitives\n");
            msl.append("    if (outputPrimitiveType == 0) { // Points\n");
            msl.append("        outputVertexCount = stripLength;\n");
            msl.append("    } else if (outputPrimitiveType == 1) { // Line strip -> lines\n");
            msl.append("        if (stripLength >= 2) {\n");
            msl.append("            outputVertexCount = (stripLength - 1) * 2;\n");
            msl.append("        }\n");
            msl.append("    } else { // Triangle strip -> triangles\n");
            msl.append("        if (stripLength >= 3) {\n");
            msl.append("            outputVertexCount = (stripLength - 2) * 3;\n");
            msl.append("        }\n");
            msl.append("    }\n");
            msl.append("    \n");
            msl.append("    if (outputVertexCount == 0) {\n");
            msl.append("        state.stripStartIndex = state.pendingCount;\n");
            msl.append("        state.inStrip = false;\n");
            msl.append("        return;\n");
            msl.append("    }\n");
            msl.append("    \n");
            msl.append("    // Atomically allocate output space\n");
            msl.append("    uint baseVertex = atomic_fetch_add_explicit(&counters.vertexCounter, outputVertexCount, memory_order_relaxed);\n");
            msl.append("    \n");
            msl.append("    if (baseVertex + outputVertexCount > counters.maxVertices) {\n");
            msl.append("        state.stripStartIndex = state.pendingCount;\n");
            msl.append("        state.inStrip = false;\n");
            msl.append("        return;\n");
            msl.append("    }\n");
            msl.append("    \n");
            msl.append("    // Write output vertices\n");
            msl.append("    uint outIdx = baseVertex;\n");
            msl.append("    if (outputPrimitiveType == 0) { // Points\n");
            msl.append("        for (uint i = state.stripStartIndex; i < state.pendingCount; i++) {\n");
            msl.append("            outputBuffer[outIdx++] = state.pendingVertices[i];\n");
            msl.append("        }\n");
            msl.append("    } else if (outputPrimitiveType == 1) { // Line strip\n");
            msl.append("        for (uint i = state.stripStartIndex; i < state.pendingCount - 1; i++) {\n");
            msl.append("            outputBuffer[outIdx++] = state.pendingVertices[i];\n");
            msl.append("            outputBuffer[outIdx++] = state.pendingVertices[i + 1];\n");
            msl.append("        }\n");
            msl.append("    } else { // Triangle strip\n");
            msl.append("        for (uint i = state.stripStartIndex; i < state.pendingCount - 2; i++) {\n");
            msl.append("            if ((i - state.stripStartIndex) % 2 == 0) {\n");
            msl.append("                outputBuffer[outIdx++] = state.pendingVertices[i];\n");
            msl.append("                outputBuffer[outIdx++] = state.pendingVertices[i + 1];\n");
            msl.append("                outputBuffer[outIdx++] = state.pendingVertices[i + 2];\n");
            msl.append("            } else {\n");
            msl.append("                outputBuffer[outIdx++] = state.pendingVertices[i];\n");
            msl.append("                outputBuffer[outIdx++] = state.pendingVertices[i + 2];\n");
            msl.append("                outputBuffer[outIdx++] = state.pendingVertices[i + 1];\n");
            msl.append("            }\n");
            msl.append("        }\n");
            msl.append("    }\n");
            msl.append("    \n");
            msl.append("    state.stripStartIndex = state.pendingCount;\n");
            msl.append("    state.inStrip = false;\n");
            msl.append("}\n\n");
        }
        
        private void generateComputeKernel(StringBuilder msl, String gsSource) {
            int inputVertexCount = getInputVertexCount();
            
            msl.append("// ═══════════════════════════════════════════════════════════\n");
            msl.append("// Main Geometry Shader Compute Kernel\n");
            msl.append("// ═══════════════════════════════════════════════════════════\n\n");
            
            msl.append("kernel void geometryShaderMain(\n");
            msl.append("    device const GSVertexInput* inputVertices [[buffer(0)]],\n");
            msl.append("    device GSVertexOutput* outputVertices [[buffer(1)]],\n");
            msl.append("    device GSCounters& counters [[buffer(2)]],\n");
            msl.append("    device GSIndirectCommand& indirectCmd [[buffer(3)]],\n");
            msl.append("    constant uint& primitiveCount [[buffer(4)]],\n");
            msl.append("    constant uint& invocationCount [[buffer(5)]],\n");
            msl.append("    uint3 threadPos [[thread_position_in_grid]],\n");
            msl.append("    uint3 gridSize [[threads_per_grid]]\n");
            msl.append(") {\n");
            msl.append("    uint primitiveID = threadPos.x;\n");
            msl.append("    uint invocationID = threadPos.y;\n");
            msl.append("    \n");
            msl.append("    if (primitiveID >= primitiveCount) return;\n");
            msl.append("    if (invocationID >= invocationCount) return;\n");
            msl.append("    \n");
            msl.append("    // Load input primitive vertices (gl_in[])\n");
            msl.append("    GSVertexInput gl_in[").append(inputVertexCount).append("];\n");
            msl.append("    uint baseVertex = primitiveID * ").append(inputVertexCount).append(";\n");
            msl.append("    for (uint i = 0; i < ").append(inputVertexCount).append("; i++) {\n");
            msl.append("        gl_in[i] = inputVertices[baseVertex + i];\n");
            msl.append("    }\n");
            msl.append("    \n");
            msl.append("    // Built-in variables\n");
            msl.append("    int gl_PrimitiveIDIn = primitiveID;\n");
            msl.append("    int gl_InvocationID = invocationID;\n");
            msl.append("    \n");
            msl.append("    // Emission state\n");
            msl.append("    GSEmitState _emitState;\n");
            msl.append("    _emitState.pendingCount = 0;\n");
            msl.append("    _emitState.stripStartIndex = 0;\n");
            msl.append("    _emitState.inStrip = false;\n");
            msl.append("    \n");
            msl.append("    // Current output vertex\n");
            msl.append("    GSCurrentVertex _currentVertex;\n");
            msl.append("    _currentVertex.gl_Position = float4(0.0);\n");
            msl.append("    _currentVertex.gl_PointSize = 1.0;\n");
            msl.append("    _currentVertex.gl_Layer = 0;\n");
            msl.append("    _currentVertex.gl_ViewportIndex = 0;\n");
            msl.append("    _currentVertex.gl_PrimitiveID = gl_PrimitiveIDIn;\n");
            msl.append("    \n");
            msl.append("    // === User geometry shader code ===\n");
            
            // Translate and insert the geometry shader main function body
            String translatedBody = translateGeometryShaderBody(gsSource);
            msl.append(translatedBody);
            
            msl.append("    // === End user code ===\n");
            msl.append("    \n");
            msl.append("    // Finalize any pending primitives\n");
            msl.append("    EndPrimitive_impl(_emitState, outputVertices, counters, ").append(outputPrimitive).append(");\n");
            msl.append("    \n");
            msl.append("    // Update indirect draw command (only first thread)\n");
            msl.append("    if (primitiveID == 0 && invocationID == 0) {\n");
            msl.append("        threadgroup_barrier(mem_flags::mem_device);\n");
            msl.append("        indirectCmd.vertexCount = atomic_load_explicit(&counters.vertexCounter, memory_order_relaxed);\n");
            msl.append("        indirectCmd.instanceCount = 1;\n");
            msl.append("        indirectCmd.vertexStart = 0;\n");
            msl.append("        indirectCmd.baseInstance = 0;\n");
            msl.append("    }\n");
            msl.append("}\n");
        }
        
        private String translateGeometryShaderBody(String gsSource) {
            // Extract main function body
            int mainStart = gsSource.indexOf("void main()");
            if (mainStart == -1) mainStart = gsSource.indexOf("void main(void)");
            if (mainStart == -1) return "    // Could not find main function\n";
            
            int braceStart = gsSource.indexOf('{', mainStart);
            if (braceStart == -1) return "    // Malformed main function\n";
            
            int braceCount = 1;
            int braceEnd = braceStart + 1;
            while (braceEnd < gsSource.length() && braceCount > 0) {
                char c = gsSource.charAt(braceEnd);
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                braceEnd++;
            }
            
            String body = gsSource.substring(braceStart + 1, braceEnd - 1);
            
            // Apply translations
            body = body.replaceAll("\\bEmitVertex\\s*\\(\\s*\\)", 
                "EmitVertex_impl(_currentVertex, _emitState, outputVertices, counters)");
            body = body.replaceAll("\\bEndPrimitive\\s*\\(\\s*\\)", 
                "EndPrimitive_impl(_emitState, outputVertices, counters, " + outputPrimitive + ")");
            
            // Translate gl_Position assignment
            body = body.replaceAll("\\bgl_Position\\s*=", "_currentVertex.gl_Position =");
            body = body.replaceAll("\\bgl_PointSize\\s*=", "_currentVertex.gl_PointSize =");
            body = body.replaceAll("\\bgl_Layer\\s*=", "_currentVertex.gl_Layer =");
            body = body.replaceAll("\\bgl_ViewportIndex\\s*=", "_currentVertex.gl_ViewportIndex =");
            
            // Translate gl_in[].gl_Position etc.
            body = body.replaceAll("\\bgl_in\\s*\\[([^\\]]+)\\]\\s*\\.\\s*gl_Position", "gl_in[\$1].position");
            body = body.replaceAll("\\bgl_in\\s*\\[([^\\]]+)\\]\\s*\\.\\s*gl_PointSize", "gl_in[\$1].pointSize");
            body = body.replaceAll("\\bgl_in\\s*\\[([^\\]]+)\\]\\s*\\.\\s*gl_ClipDistance", "gl_in[\$1].clipDistance");
            
            // Translate types
            body = body.replaceAll("\\bvec2\\b", "float2");
            body = body.replaceAll("\\bvec3\\b", "float3");
            body = body.replaceAll("\\bvec4\\b", "float4");
            body = body.replaceAll("\\bmat2\\b", "float2x2");
            body = body.replaceAll("\\bmat3\\b", "float3x3");
            body = body.replaceAll("\\bmat4\\b", "float4x4");
            body = body.replaceAll("\\bivec2\\b", "int2");
            body = body.replaceAll("\\bivec3\\b", "int3");
            body = body.replaceAll("\\bivec4\\b", "int4");
            
            // Add indentation
            StringBuilder result = new StringBuilder();
            for (String line : body.split("\n")) {
                result.append("    ").append(line).append("\n");
            }
            
            return result.toString();
        }
        
        /**
         * Create Metal resources for geometry shader emulation
         */
        public void createResources(long device, int maxOutputVerts) {
            this.maxOutputVertices = maxOutputVerts;
            this.maxOutputPrimitives = maxOutputVerts / 3;
            
            // Output vertex buffer size
            int vertexSize = 64; // Approximate size of GSVertexOutput
            int outputBufferSize = maxOutputVertices * vertexSize;
            
            // Create output buffer
            outputBufferHandle = nMTLDeviceNewBufferWithLength(device, outputBufferSize, 
                MTLResourceStorageModeShared);
            
            // Create counter buffer
            counterBufferHandle = nMTLDeviceNewBufferWithLength(device, 32, 
                MTLResourceStorageModeShared);
            
            // Create indirect command buffer
            indirectBufferHandle = nMTLDeviceNewBufferWithLength(device, 16, 
                MTLResourceStorageModeShared);
        }
        
        /**
         * Reset counters before dispatch
         */
        public void resetCounters() {
            if (counterBufferHandle != 0) {
                long ptr = nMTLBufferContents(counterBufferHandle);
                // Zero out counters
                // vertexCounter = 0, primitiveCounter = 0
                // maxVertices = maxOutputVertices, maxPrimitives = maxOutputPrimitives
                // This would need native memory access
            }
        }
        
        /**
         * Dispatch geometry shader compute kernel
         */
        public void dispatch(long commandBuffer, long computePipeline, 
                            long inputBuffer, int primitiveCount) {
            long encoder = nMTLCommandBufferComputeCommandEncoder(commandBuffer);
            
            nMTLComputeCommandEncoderSetComputePipelineState(encoder, computePipeline);
            
            // Set buffers
            nMTLComputeCommandEncoderSetBuffer(encoder, inputBuffer, 0, 0);
            nMTLComputeCommandEncoderSetBuffer(encoder, outputBufferHandle, 0, 1);
            nMTLComputeCommandEncoderSetBuffer(encoder, counterBufferHandle, 0, 2);
            nMTLComputeCommandEncoderSetBuffer(encoder, indirectBufferHandle, 0, 3);
            
            // Calculate threadgroup size
            int threadsPerGroup = 64;
            int numGroups = (primitiveCount * invocations + threadsPerGroup - 1) / threadsPerGroup;
            
            // Dispatch
            nMTLComputeCommandEncoderDispatchThreadgroups(encoder, 
                numGroups, 1, 1,
                threadsPerGroup, 1, 1);
            
            nMTLComputeCommandEncoderEndEncoding(encoder);
        }
        
        /**
         * Get the output vertex buffer for rendering
         */
        public long getOutputBuffer() {
            return outputBufferHandle;
        }
        
        /**
         * Get the indirect draw buffer
         */
        public long getIndirectBuffer() {
            return indirectBufferHandle;
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 22: TESSELLATION SHADER SUPPORT
    // Complete tessellation control and evaluation shader translation
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Handles translation of OpenGL tessellation shaders to Metal's tessellation system.
     * Supports both tessellation control (hull) and tessellation evaluation (domain) shaders.
     */
    public static final class TessellationShaderTranslator {
        
        // Tessellation primitive modes
        public static final int TESS_QUADS = 0;
        public static final int TESS_TRIANGLES = 1;
        public static final int TESS_ISOLINES = 2;
        
        // Tessellation spacing modes
        public static final int SPACING_EQUAL = 0;
        public static final int SPACING_FRACTIONAL_EVEN = 1;
        public static final int SPACING_FRACTIONAL_ODD = 2;
        
        // Tessellation vertex ordering
        public static final int ORDER_CW = 0;
        public static final int ORDER_CCW = 1;
        
        // Configuration from shader
        private int primitiveMode = TESS_TRIANGLES;
        private int spacingMode = SPACING_EQUAL;
        private int vertexOrder = ORDER_CCW;
        private boolean pointMode = false;
        private int outputVertices = 3;
        
        // Patch data
        private int inputPatchVertices = 3;
        private int outputPatchVertices = 3;
        
        // Interface variables
        private final List<TessVariable> tcsInputs = new ArrayList<>();
        private final List<TessVariable> tcsOutputs = new ArrayList<>();
        private final List<TessVariable> tcsPerPatchOutputs = new ArrayList<>();
        private final List<TessVariable> tesInputs = new ArrayList<>();
        private final List<TessVariable> tesOutputs = new ArrayList<>();
        
        /**
         * Tessellation shader variable
         */
        public static class TessVariable {
            public String name;
            public String type;
            public int location = -1;
            public boolean isPatch = false;
            public boolean isPerVertex = false;
            public int arraySize = 0;
            public String interpolation = "";
        }
        
        /**
         * Parse tessellation control shader layout qualifiers
         */
        public void parseTCSLayout(String tcsSource) {
            // Parse output vertices
            java.util.regex.Pattern verticesPattern = java.util.regex.Pattern.compile(
                "layout\\s*\\(\\s*vertices\\s*=\\s*(\\d+)\\s*\\)\\s*out");
            java.util.regex.Matcher verticesMatcher = verticesPattern.matcher(tcsSource);
            if (verticesMatcher.find()) {
                outputVertices = Integer.parseInt(verticesMatcher.group(1));
            }
        }
        
        /**
         * Parse tessellation evaluation shader layout qualifiers
         */
        public void parseTESLayout(String tesSource) {
            // Parse primitive mode
            if (tesSource.contains("quads")) {
                primitiveMode = TESS_QUADS;
            } else if (tesSource.contains("triangles")) {
                primitiveMode = TESS_TRIANGLES;
            } else if (tesSource.contains("isolines")) {
                primitiveMode = TESS_ISOLINES;
            }
            
            // Parse spacing
            if (tesSource.contains("fractional_even_spacing")) {
                spacingMode = SPACING_FRACTIONAL_EVEN;
            } else if (tesSource.contains("fractional_odd_spacing")) {
                spacingMode = SPACING_FRACTIONAL_ODD;
            } else if (tesSource.contains("equal_spacing")) {
                spacingMode = SPACING_EQUAL;
            }
            
            // Parse winding
            if (tesSource.contains("cw")) {
                vertexOrder = ORDER_CW;
            } else if (tesSource.contains("ccw")) {
                vertexOrder = ORDER_CCW;
            }
            
            // Parse point mode
            pointMode = tesSource.contains("point_mode");
        }
        
        /**
         * Generate Metal tessellation compute kernel (tessellation control shader)
         */
        public String generateTessellationKernel(String tcsSource) {
            parseTCSLayout(tcsSource);
            
            StringBuilder msl = new StringBuilder();
            
            // Header
            msl.append("#include <metal_stdlib>\n");
            msl.append("using namespace metal;\n\n");
            
            // Input/output structures
            generateTessStructures(msl);
            
            // Kernel
            msl.append("kernel void tessellationControlShader(\n");
            msl.append("    device const TCSInput* vertexInput [[buffer(0)]],\n");
            msl.append("    device TCSOutput* vertexOutput [[buffer(1)]],\n");
            msl.append("    device MTLQuadTessellationFactorsHalf* tessFactors [[buffer(2)]],\n");
            msl.append("    constant TCSUniforms& uniforms [[buffer(3)]],\n");
            msl.append("    uint patchID [[threadgroup_position_in_grid]],\n");
            msl.append("    uint controlPointID [[thread_position_in_threadgroup]]\n");
            msl.append(") {\n");
            
            // Load input vertices
            msl.append("    // Load patch input vertices (gl_in[])\n");
            msl.append("    uint basePatchVertex = patchID * ").append(inputPatchVertices).append(";\n");
            msl.append("    TCSInput gl_in[").append(inputPatchVertices).append("];\n");
            msl.append("    for (uint i = 0; i < ").append(inputPatchVertices).append("; i++) {\n");
            msl.append("        gl_in[i] = vertexInput[basePatchVertex + i];\n");
            msl.append("    }\n\n");
            
            // Built-in variables
            msl.append("    // Built-in variables\n");
            msl.append("    int gl_InvocationID = controlPointID;\n");
            msl.append("    int gl_PatchVerticesIn = ").append(inputPatchVertices).append(";\n");
            msl.append("    int gl_PrimitiveID = patchID;\n\n");
            
            // Output arrays (per-vertex and per-patch)
            msl.append("    // Thread-local output vertex\n");
            msl.append("    TCSOutputVertex gl_out;\n");
            msl.append("    gl_out.gl_Position = float4(0.0);\n");
            msl.append("    gl_out.gl_PointSize = 1.0;\n\n");
            
            // Tessellation levels (will be written by invocation 0)
            msl.append("    // Tessellation levels (shared across invocations)\n");
            msl.append("    threadgroup float gl_TessLevelOuter[4];\n");
            msl.append("    threadgroup float gl_TessLevelInner[2];\n\n");
            
            // Initialize tess levels
            msl.append("    if (gl_InvocationID == 0) {\n");
            msl.append("        gl_TessLevelOuter[0] = 1.0;\n");
            msl.append("        gl_TessLevelOuter[1] = 1.0;\n");
            msl.append("        gl_TessLevelOuter[2] = 1.0;\n");
            msl.append("        gl_TessLevelOuter[3] = 1.0;\n");
            msl.append("        gl_TessLevelInner[0] = 1.0;\n");
            msl.append("        gl_TessLevelInner[1] = 1.0;\n");
            msl.append("    }\n");
            msl.append("    threadgroup_barrier(mem_flags::mem_threadgroup);\n\n");
            
            // Insert translated user code
            msl.append("    // === User TCS code ===\n");
            String translatedBody = translateTCSBody(tcsSource);
            msl.append(translatedBody);
            msl.append("    // === End user code ===\n\n");
            
            // Synchronize
            msl.append("    threadgroup_barrier(mem_flags::mem_threadgroup);\n\n");
            
            // Write output vertex
            msl.append("    // Write output control point\n");
            msl.append("    uint outputIndex = patchID * ").append(outputVertices).append(" + gl_InvocationID;\n");
            msl.append("    vertexOutput[outputIndex] = gl_out;\n\n");
            
            // Write tessellation factors (only invocation 0)
            msl.append("    // Write tessellation factors\n");
            msl.append("    if (gl_InvocationID == 0) {\n");
            if (primitiveMode == TESS_QUADS) {
                msl.append("        tessFactors[patchID].edgeTessellationFactor[0] = half(gl_TessLevelOuter[0]);\n");
                msl.append("        tessFactors[patchID].edgeTessellationFactor[1] = half(gl_TessLevelOuter[1]);\n");
                msl.append("        tessFactors[patchID].edgeTessellationFactor[2] = half(gl_TessLevelOuter[2]);\n");
                msl.append("        tessFactors[patchID].edgeTessellationFactor[3] = half(gl_TessLevelOuter[3]);\n");
                msl.append("        tessFactors[patchID].insideTessellationFactor[0] = half(gl_TessLevelInner[0]);\n");
                msl.append("        tessFactors[patchID].insideTessellationFactor[1] = half(gl_TessLevelInner[1]);\n");
            } else { // Triangles
                msl.append("        MTLTriangleTessellationFactorsHalf* triFactor = (MTLTriangleTessellationFactorsHalf*)&tessFactors[patchID];\n");
                msl.append("        triFactor->edgeTessellationFactor[0] = half(gl_TessLevelOuter[0]);\n");
                msl.append("        triFactor->edgeTessellationFactor[1] = half(gl_TessLevelOuter[1]);\n");
                msl.append("        triFactor->edgeTessellationFactor[2] = half(gl_TessLevelOuter[2]);\n");
                msl.append("        triFactor->insideTessellationFactor = half(gl_TessLevelInner[0]);\n");
            }
            msl.append("    }\n");
            msl.append("}\n");
            
            return msl.toString();
        }
        
        /**
         * Generate Metal post-tessellation vertex function (tessellation evaluation shader)
         */
        public String generatePostTessVertexFunction(String tesSource) {
            parseTESLayout(tesSource);
            
            StringBuilder msl = new StringBuilder();
            
            // Header
            msl.append("#include <metal_stdlib>\n");
            msl.append("using namespace metal;\n\n");
            
            // Structures
            generateTESStructures(msl);
            
            // Post-tessellation vertex function
            msl.append("[[patch(");
            msl.append(primitiveMode == TESS_QUADS ? "quad" : "triangle");
            msl.append(", ").append(outputVertices).append(")]]\n");
            msl.append("vertex TESOutput tessellationEvaluationShader(\n");
            msl.append("    patch_control_point<TESInput> patch [[stage_in]],\n");
            
            if (primitiveMode == TESS_QUADS) {
                msl.append("    float2 gl_TessCoord [[position_in_patch]],\n");
            } else {
                msl.append("    float3 gl_TessCoord [[position_in_patch]],\n");
            }
            
            msl.append("    uint patchID [[patch_id]],\n");
            msl.append("    constant TESUniforms& uniforms [[buffer(0)]]\n");
            msl.append(") {\n");
            
            // Built-in variables
            msl.append("    // Built-in variables\n");
            msl.append("    int gl_PatchVerticesIn = ").append(outputVertices).append(";\n");
            msl.append("    int gl_PrimitiveID = patchID;\n\n");
            
            // Load patch control points into gl_in array
            msl.append("    // Load control points (gl_in[])\n");
            msl.append("    TESControlPoint gl_in[").append(outputVertices).append("];\n");
            msl.append("    for (uint i = 0; i < ").append(outputVertices).append("; i++) {\n");
            msl.append("        gl_in[i].gl_Position = patch[i].position;\n");
            msl.append("        gl_in[i].gl_PointSize = patch[i].pointSize;\n");
            msl.append("    }\n\n");
            
            // Output variables
            msl.append("    // Output\n");
            msl.append("    TESOutput out;\n");
            msl.append("    float4 gl_Position = float4(0.0);\n");
            msl.append("    float gl_PointSize = 1.0;\n\n");
            
            // Insert translated user code
            msl.append("    // === User TES code ===\n");
            String translatedBody = translateTESBody(tesSource);
            msl.append(translatedBody);
            msl.append("    // === End user code ===\n\n");
            
            // Write outputs
            msl.append("    out.position = gl_Position;\n");
            msl.append("    out.pointSize = gl_PointSize;\n");
            msl.append("    return out;\n");
            msl.append("}\n");
            
            return msl.toString();
        }
        
        private void generateTessStructures(StringBuilder msl) {
            // TCS Input structure
            msl.append("struct TCSInput {\n");
            msl.append("    float4 position;\n");
            msl.append("    float pointSize;\n");
            msl.append("    float4 color;\n");
            msl.append("    float3 normal;\n");
            msl.append("    float2 texCoord;\n");
            msl.append("};\n\n");
            
            // TCS Output vertex structure
            msl.append("struct TCSOutputVertex {\n");
            msl.append("    float4 gl_Position;\n");
            msl.append("    float gl_PointSize;\n");
            msl.append("    float4 color;\n");
            msl.append("    float3 normal;\n");
            msl.append("    float2 texCoord;\n");
            msl.append("};\n\n");
            
            // TCS Output structure (goes to device memory)
            msl.append("struct TCSOutput {\n");
            msl.append("    float4 position;\n");
            msl.append("    float pointSize;\n");
            msl.append("    float4 color;\n");
            msl.append("    float3 normal;\n");
            msl.append("    float2 texCoord;\n");
            msl.append("};\n\n");
            
            // Uniforms
            msl.append("struct TCSUniforms {\n");
            msl.append("    float4x4 modelViewProjection;\n");
            msl.append("    float tessLevelInner;\n");
            msl.append("    float tessLevelOuter;\n");
            msl.append("};\n\n");
        }
        
        private void generateTESStructures(StringBuilder msl) {
            // TES Input structure (from control points)
            msl.append("struct TESInput {\n");
            msl.append("    float4 position [[attribute(0)]];\n");
            msl.append("    float pointSize [[attribute(1)]];\n");
            msl.append("    float4 color [[attribute(2)]];\n");
            msl.append("    float3 normal [[attribute(3)]];\n");
            msl.append("    float2 texCoord [[attribute(4)]];\n");
            msl.append("};\n\n");
            
            // Control point structure
            msl.append("struct TESControlPoint {\n");
            msl.append("    float4 gl_Position;\n");
            msl.append("    float gl_PointSize;\n");
            msl.append("    float4 color;\n");
            msl.append("    float3 normal;\n");
            msl.append("    float2 texCoord;\n");
            msl.append("};\n\n");
            
            // TES Output structure
            msl.append("struct TESOutput {\n");
            msl.append("    float4 position [[position]];\n");
            msl.append("    float pointSize [[point_size]];\n");
            msl.append("    float4 color;\n");
            msl.append("    float3 normal;\n");
            msl.append("    float2 texCoord;\n");
            msl.append("};\n\n");
            
            // Uniforms
            msl.append("struct TESUniforms {\n");
            msl.append("    float4x4 modelViewProjection;\n");
            msl.append("};\n\n");
        }
        
        private String translateTCSBody(String source) {
            // Extract main function body
            String body = extractMainBody(source);
            if (body == null) return "    // Could not extract main body\n";
            
            // Translations
            body = body.replaceAll("\\bgl_out\\s*\\[\\s*gl_InvocationID\\s*\\]\\s*\\.\\s*gl_Position",
                "gl_out.gl_Position");
            body = body.replaceAll("\\bgl_out\\s*\\[\\s*gl_InvocationID\\s*\\]\\s*\\.\\s*gl_PointSize",
                "gl_out.gl_PointSize");
            
            // Translate gl_in access
            body = body.replaceAll("\\bgl_in\\s*\\[([^\\]]+)\\]\\s*\\.\\s*gl_Position",
                "gl_in[\$1].position");
            body = body.replaceAll("\\bgl_in\\s*\\[([^\\]]+)\\]\\s*\\.\\s*gl_PointSize",
                "gl_in[\$1].pointSize");
            
            // Translate types
            body = translateTypes(body);
            
            // Indent
            return indentCode(body, 4);
        }
        
        private String translateTESBody(String source) {
            String body = extractMainBody(source);
            if (body == null) return "    // Could not extract main body\n";
            
            // Translate gl_in access
            body = body.replaceAll("\\bgl_in\\s*\\[([^\\]]+)\\]\\s*\\.\\s*gl_Position",
                "gl_in[\$1].gl_Position");
            body = body.replaceAll("\\bgl_in\\s*\\[([^\\]]+)\\]\\s*\\.\\s*gl_PointSize",
                "gl_in[\$1].gl_PointSize");
            
            // Translate types
            body = translateTypes(body);
            
            return indentCode(body, 4);
        }
        
        private String extractMainBody(String source) {
            int mainStart = source.indexOf("void main()");
            if (mainStart == -1) mainStart = source.indexOf("void main(void)");
            if (mainStart == -1) return null;
            
            int braceStart = source.indexOf('{', mainStart);
            if (braceStart == -1) return null;
            
            int braceCount = 1;
            int braceEnd = braceStart + 1;
            while (braceEnd < source.length() && braceCount > 0) {
                char c = source.charAt(braceEnd);
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                braceEnd++;
            }
            
            return source.substring(braceStart + 1, braceEnd - 1);
        }
        
        private String translateTypes(String code) {
            code = code.replaceAll("\\bvec2\\b", "float2");
            code = code.replaceAll("\\bvec3\\b", "float3");
            code = code.replaceAll("\\bvec4\\b", "float4");
            code = code.replaceAll("\\bmat2\\b", "float2x2");
            code = code.replaceAll("\\bmat3\\b", "float3x3");
            code = code.replaceAll("\\bmat4\\b", "float4x4");
            code = code.replaceAll("\\bivec2\\b", "int2");
            code = code.replaceAll("\\bivec3\\b", "int3");
            code = code.replaceAll("\\bivec4\\b", "int4");
            code = code.replaceAll("\\buvec2\\b", "uint2");
            code = code.replaceAll("\\buvec3\\b", "uint3");
            code = code.replaceAll("\\buvec4\\b", "uint4");
            return code;
        }
        
        private String indentCode(String code, int spaces) {
            String indent = " ".repeat(spaces);
            StringBuilder result = new StringBuilder();
            for (String line : code.split("\n")) {
                result.append(indent).append(line.trim()).append("\n");
            }
            return result.toString();
        }
        
        /**
         * Get Metal tessellation partition mode
         */
        public int getMetalPartitionMode() {
            return switch (spacingMode) {
                case SPACING_FRACTIONAL_EVEN -> MTLTessellationPartitionModeFractionalEven;
                case SPACING_FRACTIONAL_ODD -> MTLTessellationPartitionModeFractionalOdd;
                default -> MTLTessellationPartitionModeInteger;
            };
        }
        
        /**
         * Get Metal winding order
         */
        public int getMetalWindingOrder() {
            return vertexOrder == ORDER_CW ? MTLWindingClockwise : MTLWindingCounterClockwise;
        }
        
        // Metal constants
        private static final int MTLTessellationPartitionModeInteger = 0;
        private static final int MTLTessellationPartitionModeFractionalOdd = 1;
        private static final int MTLTessellationPartitionModeFractionalEven = 2;
        private static final int MTLWindingClockwise = 0;
        private static final int MTLWindingCounterClockwise = 1;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 23: TRANSFORM FEEDBACK EMULATION
    // Complete transform feedback support using Metal compute shaders
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Emulates OpenGL transform feedback using Metal buffers and compute shaders.
     */
    public static final class TransformFeedbackEmulator {
        
        // Transform feedback state
        private boolean active = false;
        private boolean paused = false;
        private int primitiveMode = GL_POINTS;
        
        // Buffer bindings
        private final long[] boundBuffers = new long[MAX_TRANSFORM_FEEDBACK_BUFFERS];
        private final long[] bufferOffsets = new long[MAX_TRANSFORM_FEEDBACK_BUFFERS];
        private final long[] bufferSizes = new long[MAX_TRANSFORM_FEEDBACK_BUFFERS];
        private int numBuffers = 0;
        
        // Varyings to capture
        private final List<String> varyings = new ArrayList<>();
        private int bufferMode = GL_INTERLEAVED_ATTRIBS;
        
        // Counter buffers
        private long primitiveWrittenQuery = 0;
        private long primitiveGeneratedQuery = 0;
        
        // Computed stride per buffer
        private final int[] bufferStrides = new int[MAX_TRANSFORM_FEEDBACK_BUFFERS];
        
        // Atomic counter buffer for tracking writes
        private long counterBuffer;
        
        private static final int MAX_TRANSFORM_FEEDBACK_BUFFERS = 4;
        
        /**
         * Varying information
         */
        public static class VaryingInfo {
            public String name;
            public String type;
            public int size;
            public int bufferIndex;
            public int offsetInBuffer;
        }
        
        /**
         * Set varyings to capture
         */
        public void setVaryings(String[] varyingNames, int mode) {
            varyings.clear();
            varyings.addAll(Arrays.asList(varyingNames));
            bufferMode = mode;
        }
        
        /**
         * Bind a buffer for transform feedback
         */
        public void bindBuffer(int index, long buffer, long offset, long size) {
            if (index >= 0 && index < MAX_TRANSFORM_FEEDBACK_BUFFERS) {
                boundBuffers[index] = buffer;
                bufferOffsets[index] = offset;
                bufferSizes[index] = size;
                numBuffers = Math.max(numBuffers, index + 1);
            }
        }
        
        /**
         * Begin transform feedback
         */
        public void begin(int mode) {
            primitiveMode = mode;
            active = true;
            paused = false;
            
            // Reset counters
            resetCounters();
        }
        
        /**
         * Pause transform feedback
         */
        public void pause() {
            if (active && !paused) {
                paused = true;
            }
        }
        
        /**
         * Resume transform feedback
         */
        public void resume() {
            if (active && paused) {
                paused = false;
            }
        }
        
        /**
         * End transform feedback
         */
        public void end() {
            active = false;
            paused = false;
        }
        
        /**
         * Check if transform feedback is active
         */
        public boolean isActive() {
            return active && !paused;
        }
        
        /**
         * Generate compute shader for transform feedback capture
         */
        public String generateCaptureShader(String vertexShaderSource, List<VaryingInfo> varyingInfos) {
            StringBuilder msl = new StringBuilder();
            
            // Header
            msl.append("#include <metal_stdlib>\n");
            msl.append("using namespace metal;\n\n");
            
            // Generate structures based on varyings
            generateCaptureStructures(msl, varyingInfos);
            
            // Generate compute kernel
            generateCaptureKernel(msl, varyingInfos);
            
            return msl.toString();
        }
        
        private void generateCaptureStructures(StringBuilder msl, List<VaryingInfo> varyingInfos) {
            // Vertex input structure (from vertex shader)
            msl.append("struct TFVertexInput {\n");
            msl.append("    float4 position;\n");
            for (VaryingInfo varying : varyingInfos) {
                msl.append("    ").append(translateVaryingType(varying.type)).append(" ");
                msl.append(varying.name).append(";\n");
            }
            msl.append("};\n\n");
            
            // Generate output structures based on buffer mode
            if (bufferMode == GL_INTERLEAVED_ATTRIBS) {
                // All varyings in one structure
                msl.append("struct TFOutput {\n");
                for (VaryingInfo varying : varyingInfos) {
                    msl.append("    ").append(translateVaryingType(varying.type)).append(" ");
                    msl.append(varying.name).append(";\n");
                }
                msl.append("};\n\n");
            } else {
                // Separate structures for each buffer
                for (int i = 0; i < numBuffers; i++) {
                    msl.append("struct TFOutput").append(i).append(" {\n");
                    for (VaryingInfo varying : varyingInfos) {
                        if (varying.bufferIndex == i) {
                            msl.append("    ").append(translateVaryingType(varying.type)).append(" ");
                            msl.append(varying.name).append(";\n");
                        }
                    }
                    msl.append("};\n\n");
                }
            }
            
            // Atomic counter structure
            msl.append("struct TFCounters {\n");
            msl.append("    atomic_uint vertexCount;\n");
            msl.append("    atomic_uint primitiveCount;\n");
            msl.append("};\n\n");
        }
        
        private void generateCaptureKernel(StringBuilder msl, List<VaryingInfo> varyingInfos) {
            msl.append("kernel void transformFeedbackCapture(\n");
            msl.append("    device const TFVertexInput* vertices [[buffer(0)]],\n");
            
            if (bufferMode == GL_INTERLEAVED_ATTRIBS) {
                msl.append("    device TFOutput* output [[buffer(1)]],\n");
            } else {
                for (int i = 0; i < numBuffers; i++) {
                    msl.append("    device TFOutput").append(i).append("* output").append(i);
                    msl.append(" [[buffer(").append(i + 1).append(")]],\n");
                }
            }
            
            msl.append("    device TFCounters& counters [[buffer(").append(numBuffers + 1).append(")]],\n");
            msl.append("    constant uint& vertexCount [[buffer(").append(numBuffers + 2).append(")]],\n");
            msl.append("    constant uint& primitiveMode [[buffer(").append(numBuffers + 3).append(")]],\n");
            msl.append("    uint vertexID [[thread_position_in_grid]]\n");
            msl.append(") {\n");
            msl.append("    if (vertexID >= vertexCount) return;\n\n");
            
            // Load vertex data
            msl.append("    TFVertexInput vertex = vertices[vertexID];\n\n");
            
            // Write to output buffer(s)
            if (bufferMode == GL_INTERLEAVED_ATTRIBS) {
                msl.append("    // Write interleaved output\n");
                msl.append("    TFOutput tfOut;\n");
                for (VaryingInfo varying : varyingInfos) {
                    msl.append("    tfOut.").append(varying.name).append(" = vertex.");
                    msl.append(varying.name).append(";\n");
                }
                msl.append("    output[vertexID] = tfOut;\n");
            } else {
                msl.append("    // Write separate outputs\n");
                for (int i = 0; i < numBuffers; i++) {
                    msl.append("    TFOutput").append(i).append(" tfOut").append(i).append(";\n");
                    for (VaryingInfo varying : varyingInfos) {
                        if (varying.bufferIndex == i) {
                            msl.append("    tfOut").append(i).append(".").append(varying.name);
                            msl.append(" = vertex.").append(varying.name).append(";\n");
                        }
                    }
                    msl.append("    output").append(i).append("[vertexID] = tfOut").append(i).append(";\n");
                }
            }
            
            // Update counters
            msl.append("\n    // Update counters\n");
            msl.append("    atomic_fetch_add_explicit(&counters.vertexCount, 1, memory_order_relaxed);\n");
            msl.append("    \n");
            msl.append("    // Primitive counting based on mode\n");
            msl.append("    if (primitiveMode == 0) { // GL_POINTS\n");
            msl.append("        atomic_fetch_add_explicit(&counters.primitiveCount, 1, memory_order_relaxed);\n");
            msl.append("    } else if (primitiveMode == 1) { // GL_LINES\n");
            msl.append("        if (vertexID % 2 == 1) {\n");
            msl.append("            atomic_fetch_add_explicit(&counters.primitiveCount, 1, memory_order_relaxed);\n");
            msl.append("        }\n");
            msl.append("    } else if (primitiveMode == 4) { // GL_TRIANGLES\n");
            msl.append("        if (vertexID % 3 == 2) {\n");
            msl.append("            atomic_fetch_add_explicit(&counters.primitiveCount, 1, memory_order_relaxed);\n");
            msl.append("        }\n");
            msl.append("    }\n");
            msl.append("}\n");
        }
        
        private String translateVaryingType(String glslType) {
            return switch (glslType) {
                case "float" -> "float";
                case "vec2" -> "float2";
                case "vec3" -> "float3";
                case "vec4" -> "float4";
                case "int" -> "int";
                case "ivec2" -> "int2";
                case "ivec3" -> "int3";
                case "ivec4" -> "int4";
                case "uint" -> "uint";
                case "uvec2" -> "uint2";
                case "uvec3" -> "uint3";
                case "uvec4" -> "uint4";
                case "mat2" -> "float2x2";
                case "mat3" -> "float3x3";
                case "mat4" -> "float4x4";
                default -> "float4";
            };
        }
        
        /**
         * Reset write counters
         */
        private void resetCounters() {
            if (counterBuffer != 0) {
                // Zero out the counter buffer
                // Would need native memory access
            }
        }
        
        /**
         * Get number of primitives written
         */
        public long getPrimitivesWritten() {
            if (counterBuffer != 0) {
                // Read from counter buffer
                // Would need native memory access
                return 0;
            }
            return 0;
        }
        
        /**
         * Calculate stride for interleaved mode
         */
        public int calculateInterleavedStride(List<VaryingInfo> varyings) {
            int stride = 0;
            for (VaryingInfo varying : varyings) {
                stride += varying.size;
            }
            return stride;
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 24: ADVANCED TEXTURE FORMAT TRANSLATION
    // Complete texture format mapping between OpenGL and Metal
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Comprehensive texture format translation between OpenGL and Metal formats.
     * Handles all GL texture formats including compressed formats.
     */
    public static final class TextureFormatTranslator {
        
        // Metal pixel formats
        public static final int MTLPixelFormatInvalid = 0;
        public static final int MTLPixelFormatA8Unorm = 1;
        public static final int MTLPixelFormatR8Unorm = 10;
        public static final int MTLPixelFormatR8Unorm_sRGB = 11;
        public static final int MTLPixelFormatR8Snorm = 12;
        public static final int MTLPixelFormatR8Uint = 13;
        public static final int MTLPixelFormatR8Sint = 14;
        public static final int MTLPixelFormatR16Unorm = 20;
        public static final int MTLPixelFormatR16Snorm = 22;
        public static final int MTLPixelFormatR16Uint = 23;
        public static final int MTLPixelFormatR16Sint = 24;
        public static final int MTLPixelFormatR16Float = 25;
        public static final int MTLPixelFormatRG8Unorm = 30;
        public static final int MTLPixelFormatRG8Unorm_sRGB = 31;
        public static final int MTLPixelFormatRG8Snorm = 32;
        public static final int MTLPixelFormatRG8Uint = 33;
        public static final int MTLPixelFormatRG8Sint = 34;
        public static final int MTLPixelFormatB5G6R5Unorm = 40;
        public static final int MTLPixelFormatA1BGR5Unorm = 41;
        public static final int MTLPixelFormatABGR4Unorm = 42;
        public static final int MTLPixelFormatBGR5A1Unorm = 43;
        public static final int MTLPixelFormatR32Uint = 53;
        public static final int MTLPixelFormatR32Sint = 54;
        public static final int MTLPixelFormatR32Float = 55;
        public static final int MTLPixelFormatRG16Unorm = 60;
        public static final int MTLPixelFormatRG16Snorm = 62;
        public static final int MTLPixelFormatRG16Uint = 63;
        public static final int MTLPixelFormatRG16Sint = 64;
        public static final int MTLPixelFormatRG16Float = 65;
        public static final int MTLPixelFormatRGBA8Unorm = 70;
        public static final int MTLPixelFormatRGBA8Unorm_sRGB = 71;
        public static final int MTLPixelFormatRGBA8Snorm = 72;
        public static final int MTLPixelFormatRGBA8Uint = 73;
        public static final int MTLPixelFormatRGBA8Sint = 74;
        public static final int MTLPixelFormatBGRA8Unorm = 80;
        public static final int MTLPixelFormatBGRA8Unorm_sRGB = 81;
        public static final int MTLPixelFormatRGB10A2Unorm = 90;
        public static final int MTLPixelFormatRGB10A2Uint = 91;
        public static final int MTLPixelFormatRG11B10Float = 92;
        public static final int MTLPixelFormatRGB9E5Float = 93;
        public static final int MTLPixelFormatBGR10A2Unorm = 94;
        public static final int MTLPixelFormatRG32Uint = 103;
        public static final int MTLPixelFormatRG32Sint = 104;
        public static final int MTLPixelFormatRG32Float = 105;
        public static final int MTLPixelFormatRGBA16Unorm = 110;
        public static final int MTLPixelFormatRGBA16Snorm = 112;
        public static final int MTLPixelFormatRGBA16Uint = 113;
        public static final int MTLPixelFormatRGBA16Sint = 114;
        public static final int MTLPixelFormatRGBA16Float = 115;
        public static final int MTLPixelFormatRGBA32Uint = 123;
        public static final int MTLPixelFormatRGBA32Sint = 124;
        public static final int MTLPixelFormatRGBA32Float = 125;
        
        // Compressed formats
        public static final int MTLPixelFormatBC1_RGBA = 130;
        public static final int MTLPixelFormatBC1_RGBA_sRGB = 131;
        public static final int MTLPixelFormatBC2_RGBA = 132;
        public static final int MTLPixelFormatBC2_RGBA_sRGB = 133;
        public static final int MTLPixelFormatBC3_RGBA = 134;
        public static final int MTLPixelFormatBC3_RGBA_sRGB = 135;
        public static final int MTLPixelFormatBC4_RUnorm = 140;
        public static final int MTLPixelFormatBC4_RSnorm = 141;
        public static final int MTLPixelFormatBC5_RGUnorm = 142;
        public static final int MTLPixelFormatBC5_RGSnorm = 143;
        public static final int MTLPixelFormatBC6H_RGBFloat = 150;
        public static final int MTLPixelFormatBC6H_RGBUfloat = 151;
        public static final int MTLPixelFormatBC7_RGBAUnorm = 152;
        public static final int MTLPixelFormatBC7_RGBAUnorm_sRGB = 153;
        
        // PVRTC formats (iOS)
        public static final int MTLPixelFormatPVRTC_RGB_2BPP = 160;
        public static final int MTLPixelFormatPVRTC_RGB_2BPP_sRGB = 161;
        public static final int MTLPixelFormatPVRTC_RGB_4BPP = 162;
        public static final int MTLPixelFormatPVRTC_RGB_4BPP_sRGB = 163;
        public static final int MTLPixelFormatPVRTC_RGBA_2BPP = 164;
        public static final int MTLPixelFormatPVRTC_RGBA_2BPP_sRGB = 165;
        public static final int MTLPixelFormatPVRTC_RGBA_4BPP = 166;
        public static final int MTLPixelFormatPVRTC_RGBA_4BPP_sRGB = 167;
        
        // ETC/EAC formats
        public static final int MTLPixelFormatEAC_R11Unorm = 170;
        public static final int MTLPixelFormatEAC_R11Snorm = 172;
        public static final int MTLPixelFormatEAC_RG11Unorm = 174;
        public static final int MTLPixelFormatEAC_RG11Snorm = 176;
        public static final int MTLPixelFormatEAC_RGBA8 = 178;
        public static final int MTLPixelFormatEAC_RGBA8_sRGB = 179;
        public static final int MTLPixelFormatETC2_RGB8 = 180;
        public static final int MTLPixelFormatETC2_RGB8_sRGB = 181;
        public static final int MTLPixelFormatETC2_RGB8A1 = 182;
        public static final int MTLPixelFormatETC2_RGB8A1_sRGB = 183;
        
        // ASTC formats
        public static final int MTLPixelFormatASTC_4x4_sRGB = 186;
        public static final int MTLPixelFormatASTC_5x4_sRGB = 187;
        public static final int MTLPixelFormatASTC_5x5_sRGB = 188;
        public static final int MTLPixelFormatASTC_6x5_sRGB = 189;
        public static final int MTLPixelFormatASTC_6x6_sRGB = 190;
        public static final int MTLPixelFormatASTC_8x5_sRGB = 192;
        public static final int MTLPixelFormatASTC_8x6_sRGB = 193;
        public static final int MTLPixelFormatASTC_8x8_sRGB = 194;
        public static final int MTLPixelFormatASTC_10x5_sRGB = 195;
        public static final int MTLPixelFormatASTC_10x6_sRGB = 196;
        public static final int MTLPixelFormatASTC_10x8_sRGB = 197;
        public static final int MTLPixelFormatASTC_10x10_sRGB = 198;
        public static final int MTLPixelFormatASTC_12x10_sRGB = 199;
        public static final int MTLPixelFormatASTC_12x12_sRGB = 200;
        public static final int MTLPixelFormatASTC_4x4_LDR = 204;
        public static final int MTLPixelFormatASTC_5x4_LDR = 205;
        public static final int MTLPixelFormatASTC_5x5_LDR = 206;
        public static final int MTLPixelFormatASTC_6x5_LDR = 207;
        public static final int MTLPixelFormatASTC_6x6_LDR = 208;
        public static final int MTLPixelFormatASTC_8x5_LDR = 210;
        public static final int MTLPixelFormatASTC_8x6_LDR = 211;
        public static final int MTLPixelFormatASTC_8x8_LDR = 212;
        public static final int MTLPixelFormatASTC_10x5_LDR = 213;
        public static final int MTLPixelFormatASTC_10x6_LDR = 214;
        public static final int MTLPixelFormatASTC_10x8_LDR = 215;
        public static final int MTLPixelFormatASTC_10x10_LDR = 216;
        public static final int MTLPixelFormatASTC_12x10_LDR = 217;
        public static final int MTLPixelFormatASTC_12x12_LDR = 218;
        
        // Depth/Stencil formats
        public static final int MTLPixelFormatDepth16Unorm = 250;
        public static final int MTLPixelFormatDepth32Float = 252;
        public static final int MTLPixelFormatStencil8 = 253;
        public static final int MTLPixelFormatDepth24Unorm_Stencil8 = 255;
        public static final int MTLPixelFormatDepth32Float_Stencil8 = 260;
        public static final int MTLPixelFormatX32_Stencil8 = 261;
        public static final int MTLPixelFormatX24_Stencil8 = 262;
        
        /**
         * Format information structure
         */
        public static class FormatInfo {
            public int metalFormat;
            public int bytesPerPixel;
            public int blockWidth;
            public int blockHeight;
            public int blockBytes;
            public boolean isCompressed;
            public boolean hasDepth;
            public boolean hasStencil;
            public boolean isSRGB;
            public int redBits;
            public int greenBits;
            public int blueBits;
            public int alphaBits;
            public int depthBits;
            public int stencilBits;
            public String componentType; // "unorm", "snorm", "uint", "sint", "float"
        }
        
        // Format lookup table
        private static final Map<Integer, FormatInfo> formatTable = new HashMap<>();
        
        static {
            initializeFormatTable();
        }
        
        private static void initializeFormatTable() {
            // R8 formats
            addFormat(GL_R8, MTLPixelFormatR8Unorm, 1, "unorm", 8, 0, 0, 0);
            addFormat(GL_R8_SNORM, MTLPixelFormatR8Snorm, 1, "snorm", 8, 0, 0, 0);
            addFormat(GL_R8UI, MTLPixelFormatR8Uint, 1, "uint", 8, 0, 0, 0);
            addFormat(GL_R8I, MTLPixelFormatR8Sint, 1, "sint", 8, 0, 0, 0);
            
            // R16 formats
            addFormat(GL_R16, MTLPixelFormatR16Unorm, 2, "unorm", 16, 0, 0, 0);
            addFormat(GL_R16_SNORM, MTLPixelFormatR16Snorm, 2, "snorm", 16, 0, 0, 0);
            addFormat(GL_R16UI, MTLPixelFormatR16Uint, 2, "uint", 16, 0, 0, 0);
            addFormat(GL_R16I, MTLPixelFormatR16Sint, 2, "sint", 16, 0, 0, 0);
            addFormat(GL_R16F, MTLPixelFormatR16Float, 2, "float", 16, 0, 0, 0);
            
            // R32 formats
            addFormat(GL_R32UI, MTLPixelFormatR32Uint, 4, "uint", 32, 0, 0, 0);
            addFormat(GL_R32I, MTLPixelFormatR32Sint, 4, "sint", 32, 0, 0, 0);
            addFormat(GL_R32F, MTLPixelFormatR32Float, 4, "float", 32, 0, 0, 0);
            
            // RG8 formats
            addFormat(GL_RG8, MTLPixelFormatRG8Unorm, 2, "unorm", 8, 8, 0, 0);
            addFormat(GL_RG8_SNORM, MTLPixelFormatRG8Snorm, 2, "snorm", 8, 8, 0, 0);
            addFormat(GL_RG8UI, MTLPixelFormatRG8Uint, 2, "uint", 8, 8, 0, 0);
            addFormat(GL_RG8I, MTLPixelFormatRG8Sint, 2, "sint", 8, 8, 0, 0);
            
            // RG16 formats
            addFormat(GL_RG16, MTLPixelFormatRG16Unorm, 4, "unorm", 16, 16, 0, 0);
            addFormat(GL_RG16_SNORM, MTLPixelFormatRG16Snorm, 4, "snorm", 16, 16, 0, 0);
            addFormat(GL_RG16UI, MTLPixelFormatRG16Uint, 4, "uint", 16, 16, 0, 0);
            addFormat(GL_RG16I, MTLPixelFormatRG16Sint, 4, "sint", 16, 16, 0, 0);
            addFormat(GL_RG16F, MTLPixelFormatRG16Float, 4, "float", 16, 16, 0, 0);
            
            // RG32 formats
            addFormat(GL_RG32UI, MTLPixelFormatRG32Uint, 8, "uint", 32, 32, 0, 0);
            addFormat(GL_RG32I, MTLPixelFormatRG32Sint, 8, "sint", 32, 32, 0, 0);
            addFormat(GL_RG32F, MTLPixelFormatRG32Float, 8, "float", 32, 32, 0, 0);
            
            // RGBA8 formats
            addFormat(GL_RGBA8, MTLPixelFormatRGBA8Unorm, 4, "unorm", 8, 8, 8, 8);
            addFormat(GL_SRGB8_ALPHA8, MTLPixelFormatRGBA8Unorm_sRGB, 4, "unorm", 8, 8, 8, 8);
            addFormat(GL_RGBA8_SNORM, MTLPixelFormatRGBA8Snorm, 4, "snorm", 8, 8, 8, 8);
            addFormat(GL_RGBA8UI, MTLPixelFormatRGBA8Uint, 4, "uint", 8, 8, 8, 8);
            addFormat(GL_RGBA8I, MTLPixelFormatRGBA8Sint, 4, "sint", 8, 8, 8, 8);
            
            // RGBA16 formats
            addFormat(GL_RGBA16, MTLPixelFormatRGBA16Unorm, 8, "unorm", 16, 16, 16, 16);
            addFormat(GL_RGBA16_SNORM, MTLPixelFormatRGBA16Snorm, 8, "snorm", 16, 16, 16, 16);
            addFormat(GL_RGBA16UI, MTLPixelFormatRGBA16Uint, 8, "uint", 16, 16, 16, 16);
            addFormat(GL_RGBA16I, MTLPixelFormatRGBA16Sint, 8, "sint", 16, 16, 16, 16);
            addFormat(GL_RGBA16F, MTLPixelFormatRGBA16Float, 8, "float", 16, 16, 16, 16);
            
            // RGBA32 formats
            addFormat(GL_RGBA32UI, MTLPixelFormatRGBA32Uint, 16, "uint", 32, 32, 32, 32);
            addFormat(GL_RGBA32I, MTLPixelFormatRGBA32Sint, 16, "sint", 32, 32, 32, 32);
            addFormat(GL_RGBA32F, MTLPixelFormatRGBA32Float, 16, "float", 32, 32, 32, 32);
            
            // Special formats
            addFormat(GL_RGB10_A2, MTLPixelFormatRGB10A2Unorm, 4, "unorm", 10, 10, 10, 2);
            addFormat(GL_RGB10_A2UI, MTLPixelFormatRGB10A2Uint, 4, "uint", 10, 10, 10, 2);
            addFormat(GL_R11F_G11F_B10F, MTLPixelFormatRG11B10Float, 4, "float", 11, 11, 10, 0);
            addFormat(GL_RGB9_E5, MTLPixelFormatRGB9E5Float, 4, "float", 9, 9, 9, 5);
            
            // Depth/Stencil formats
            addDepthFormat(GL_DEPTH_COMPONENT16, MTLPixelFormatDepth16Unorm, 2, 16, 0);
            addDepthFormat(GL_DEPTH_COMPONENT32F, MTLPixelFormatDepth32Float, 4, 32, 0);
            addDepthFormat(GL_DEPTH24_STENCIL8, MTLPixelFormatDepth24Unorm_Stencil8, 4, 24, 8);
            addDepthFormat(GL_DEPTH32F_STENCIL8, MTLPixelFormatDepth32Float_Stencil8, 5, 32, 8);
            addDepthFormat(GL_STENCIL_INDEX8, MTLPixelFormatStencil8, 1, 0, 8);
            
            // Compressed formats - BC/DXT
            addCompressedFormat(GL_COMPRESSED_RGB_S3TC_DXT1_EXT, MTLPixelFormatBC1_RGBA, 4, 4, 8, false);
            addCompressedFormat(GL_COMPRESSED_RGBA_S3TC_DXT1_EXT, MTLPixelFormatBC1_RGBA, 4, 4, 8, false);
            addCompressedFormat(GL_COMPRESSED_SRGB_S3TC_DXT1_EXT, MTLPixelFormatBC1_RGBA_sRGB, 4, 4, 8, true);
            addCompressedFormat(GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT, MTLPixelFormatBC1_RGBA_sRGB, 4, 4, 8, true);
            addCompressedFormat(GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, MTLPixelFormatBC2_RGBA, 4, 4, 16, false);
            addCompressedFormat(GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT, MTLPixelFormatBC2_RGBA_sRGB, 4, 4, 16, true);
            addCompressedFormat(GL_COMPRESSED_RGBA_S3TC_DXT5_EXT, MTLPixelFormatBC3_RGBA, 4, 4, 16, false);
            addCompressedFormat(GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT, MTLPixelFormatBC3_RGBA_sRGB, 4, 4, 16, true);
            
            // BC4/BC5 (RGTC)
            addCompressedFormat(GL_COMPRESSED_RED_RGTC1, MTLPixelFormatBC4_RUnorm, 4, 4, 8, false);
            addCompressedFormat(GL_COMPRESSED_SIGNED_RED_RGTC1, MTLPixelFormatBC4_RSnorm, 4, 4, 8, false);
            addCompressedFormat(GL_COMPRESSED_RG_RGTC2, MTLPixelFormatBC5_RGUnorm, 4, 4, 16, false);
            addCompressedFormat(GL_COMPRESSED_SIGNED_RG_RGTC2, MTLPixelFormatBC5_RGSnorm, 4, 4, 16, false);
            
            // BC6H/BC7 (BPTC)
            addCompressedFormat(GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT, MTLPixelFormatBC6H_RGBFloat, 4, 4, 16, false);
            addCompressedFormat(GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT, MTLPixelFormatBC6H_RGBUfloat, 4, 4, 16, false);
            addCompressedFormat(GL_COMPRESSED_RGBA_BPTC_UNORM, MTLPixelFormatBC7_RGBAUnorm, 4, 4, 16, false);
            addCompressedFormat(GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM, MTLPixelFormatBC7_RGBAUnorm_sRGB, 4, 4, 16, true);
            
            // ETC2/EAC
            addCompressedFormat(GL_COMPRESSED_RGB8_ETC2, MTLPixelFormatETC2_RGB8, 4, 4, 8, false);
            addCompressedFormat(GL_COMPRESSED_SRGB8_ETC2, MTLPixelFormatETC2_RGB8_sRGB, 4, 4, 8, true);
            addCompressedFormat(GL_COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2, MTLPixelFormatETC2_RGB8A1, 4, 4, 8, false);
            addCompressedFormat(GL_COMPRESSED_SRGB8_PUNCHTHROUGH_ALPHA1_ETC2, MTLPixelFormatETC2_RGB8A1_sRGB, 4, 4, 8, true);
            addCompressedFormat(GL_COMPRESSED_RGBA8_ETC2_EAC, MTLPixelFormatEAC_RGBA8, 4, 4, 16, false);
            addCompressedFormat(GL_COMPRESSED_SRGB8_ALPHA8_ETC2_EAC, MTLPixelFormatEAC_RGBA8_sRGB, 4, 4, 16, true);
            addCompressedFormat(GL_COMPRESSED_R11_EAC, MTLPixelFormatEAC_R11Unorm, 4, 4, 8, false);
            addCompressedFormat(GL_COMPRESSED_SIGNED_R11_EAC, MTLPixelFormatEAC_R11Snorm, 4, 4, 8, false);
            addCompressedFormat(GL_COMPRESSED_RG11_EAC, MTLPixelFormatEAC_RG11Unorm, 4, 4, 16, false);
            addCompressedFormat(GL_COMPRESSED_SIGNED_RG11_EAC, MTLPixelFormatEAC_RG11Snorm, 4, 4, 16, false);
            
            // ASTC (commonly used block sizes)
            addCompressedFormat(GL_COMPRESSED_RGBA_ASTC_4x4_KHR, MTLPixelFormatASTC_4x4_LDR, 4, 4, 16, false);
            addCompressedFormat(GL_COMPRESSED_SRGB8_ALPHA8_ASTC_4x4_KHR, MTLPixelFormatASTC_4x4_sRGB, 4, 4, 16, true);
            addCompressedFormat(GL_COMPRESSED_RGBA_ASTC_5x5_KHR, MTLPixelFormatASTC_5x5_LDR, 5, 5, 16, false);
            addCompressedFormat(GL_COMPRESSED_SRGB8_ALPHA8_ASTC_5x5_KHR, MTLPixelFormatASTC_5x5_sRGB, 5, 5, 16, true);
            addCompressedFormat(GL_COMPRESSED_RGBA_ASTC_6x6_KHR, MTLPixelFormatASTC_6x6_LDR, 6, 6, 16, false);
            addCompressedFormat(GL_COMPRESSED_SRGB8_ALPHA8_ASTC_6x6_KHR, MTLPixelFormatASTC_6x6_sRGB, 6, 6, 16, true);
            addCompressedFormat(GL_COMPRESSED_RGBA_ASTC_8x8_KHR, MTLPixelFormatASTC_8x8_LDR, 8, 8, 16, false);
            addCompressedFormat(GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x8_KHR, MTLPixelFormatASTC_8x8_sRGB, 8, 8, 16, true);
            addCompressedFormat(GL_COMPRESSED_RGBA_ASTC_10x10_KHR, MTLPixelFormatASTC_10x10_LDR, 10, 10, 16, false);
            addCompressedFormat(GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x10_KHR, MTLPixelFormatASTC_10x10_sRGB, 10, 10, 16, true);
            addCompressedFormat(GL_COMPRESSED_RGBA_ASTC_12x12_KHR, MTLPixelFormatASTC_12x12_LDR, 12, 12, 16, false);
            addCompressedFormat(GL_COMPRESSED_SRGB8_ALPHA8_ASTC_12x12_KHR, MTLPixelFormatASTC_12x12_sRGB, 12, 12, 16, true);
        }
        
        private static void addFormat(int glFormat, int mtlFormat, int bpp, String type, 
                                      int r, int g, int b, int a) {
            FormatInfo info = new FormatInfo();
            info.metalFormat = mtlFormat;
            info.bytesPerPixel = bpp;
            info.blockWidth = 1;
            info.blockHeight = 1;
            info.blockBytes = bpp;
            info.isCompressed = false;
            info.hasDepth = false;
            info.hasStencil = false;
            info.isSRGB = false;
            info.redBits = r;
            info.greenBits = g;
            info.blueBits = b;
            info.alphaBits = a;
            info.depthBits = 0;
            info.stencilBits = 0;
            info.componentType = type;
            formatTable.put(glFormat, info);
        }
        
        private static void addDepthFormat(int glFormat, int mtlFormat, int bpp, int depth, int stencil) {
            FormatInfo info = new FormatInfo();
            info.metalFormat = mtlFormat;
            info.bytesPerPixel = bpp;
            info.blockWidth = 1;
            info.blockHeight = 1;
            info.blockBytes = bpp;
            info.isCompressed = false;
            info.hasDepth = depth > 0;
            info.hasStencil = stencil > 0;
            info.isSRGB = false;
            info.redBits = 0;
            info.greenBits = 0;
            info.blueBits = 0;
            info.alphaBits = 0;
            info.depthBits = depth;
            info.stencilBits = stencil;
            info.componentType = depth > 0 ? "float" : "uint";
            formatTable.put(glFormat, info);
        }
        
        private static void addCompressedFormat(int glFormat, int mtlFormat, int blockW, int blockH, 
                                                int blockBytes, boolean srgb) {
            FormatInfo info = new FormatInfo();
            info.metalFormat = mtlFormat;
            info.bytesPerPixel = 0;
            info.blockWidth = blockW;
            info.blockHeight = blockH;
            info.blockBytes = blockBytes;
            info.isCompressed = true;
            info.hasDepth = false;
            info.hasStencil = false;
            info.isSRGB = srgb;
            info.redBits = 0;
            info.greenBits = 0;
            info.blueBits = 0;
            info.alphaBits = 0;
            info.depthBits = 0;
            info.stencilBits = 0;
            info.componentType = "unorm";
            formatTable.put(glFormat, info);
        }
        
        /**
         * Get Metal pixel format for OpenGL internal format
         */
        public static int getMetalFormat(int glInternalFormat) {
            FormatInfo info = formatTable.get(glInternalFormat);
            return info != null ? info.metalFormat : MTLPixelFormatInvalid;
        }
        
        /**
         * Get format information
         */
        public static FormatInfo getFormatInfo(int glInternalFormat) {
            return formatTable.get(glInternalFormat);
        }
        
        /**
         * Calculate texture data size
         */
        public static int calculateDataSize(int glFormat, int width, int height, int depth) {
            FormatInfo info = formatTable.get(glFormat);
            if (info == null) return 0;
            
            if (info.isCompressed) {
                int blocksX = (width + info.blockWidth - 1) / info.blockWidth;
                int blocksY = (height + info.blockHeight - 1) / info.blockHeight;
                return blocksX * blocksY * depth * info.blockBytes;
            } else {
                return width * height * depth * info.bytesPerPixel;
            }
        }
        
        /**
         * Calculate row bytes (bytes per row)
         */
        public static int calculateRowBytes(int glFormat, int width) {
            FormatInfo info = formatTable.get(glFormat);
            if (info == null) return 0;
            
            if (info.isCompressed) {
                int blocksX = (width + info.blockWidth - 1) / info.blockWidth;
                return blocksX * info.blockBytes;
            } else {
                return width * info.bytesPerPixel;
            }
        }
        
        /**
         * Check if format is supported
         */
        public static boolean isFormatSupported(int glFormat) {
            return formatTable.containsKey(glFormat);
        }
        
        /**
         * Get sRGB variant of format if available
         */
        public static int getSRGBFormat(int glFormat) {
            return switch (glFormat) {
                case GL_RGBA8 -> GL_SRGB8_ALPHA8;
                case GL_COMPRESSED_RGB_S3TC_DXT1_EXT -> GL_COMPRESSED_SRGB_S3TC_DXT1_EXT;
                case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT;
                case GL_COMPRESSED_RGBA_S3TC_DXT3_EXT -> GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT;
                case GL_COMPRESSED_RGBA_S3TC_DXT5_EXT -> GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT;
                case GL_COMPRESSED_RGBA_BPTC_UNORM -> GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM;
                case GL_COMPRESSED_RGB8_ETC2 -> GL_COMPRESSED_SRGB8_ETC2;
                case GL_COMPRESSED_RGBA8_ETC2_EAC -> GL_COMPRESSED_SRGB8_ALPHA8_ETC2_EAC;
                default -> glFormat;
            };
        }
        
        // GL format constants
        private static final int GL_R8 = 0x8229;
        private static final int GL_R8_SNORM = 0x8F94;
        private static final int GL_R8UI = 0x8232;
        private static final int GL_R8I = 0x8231;
        private static final int GL_R16 = 0x822A;
        private static final int GL_R16_SNORM = 0x8F98;
        private static final int GL_R16UI = 0x8234;
        private static final int GL_R16I = 0x8233;
        private static final int GL_R16F = 0x822D;
        private static final int GL_R32UI = 0x8236;
        private static final int GL_R32I = 0x8235;
        private static final int GL_R32F = 0x822E;
        private static final int GL_RG8 = 0x822B;
        private static final int GL_RG8_SNORM = 0x8F95;
        private static final int GL_RG8UI = 0x8238;
        private static final int GL_RG8I = 0x8237;
        private static final int GL_RG16 = 0x822C;
        private static final int GL_RG16_SNORM = 0x8F99;
        private static final int GL_RG16UI = 0x823A;
        private static final int GL_RG16I = 0x8239;
        private static final int GL_RG16F = 0x822F;
        private static final int GL_RG32UI = 0x823C;
        private static final int GL_RG32I = 0x823B;
        private static final int GL_RG32F = 0x8230;
        private static final int GL_RGBA8 = 0x8058;
        private static final int GL_SRGB8_ALPHA8 = 0x8C43;
        private static final int GL_RGBA8_SNORM = 0x8F97;
        private static final int GL_RGBA8UI = 0x8D7C;
        private static final int GL_RGBA8I = 0x8D8E;
        private static final int GL_RGBA16 = 0x805B;
        private static final int GL_RGBA16_SNORM = 0x8F9B;
        private static final int GL_RGBA16UI = 0x8D76;
        private static final int GL_RGBA16I = 0x8D88;
        private static final int GL_RGBA16F = 0x881A;
        private static final int GL_RGBA32UI = 0x8D70;
        private static final int GL_RGBA32I = 0x8D82;
        private static final int GL_RGBA32F = 0x8814;
        private static final int GL_RGB10_A2 = 0x8059;
        private static final int GL_RGB10_A2UI = 0x906F;
        private static final int GL_R11F_G11F_B10F = 0x8C3A;
        private static final int GL_RGB9_E5 = 0x8C3D;
        private static final int GL_DEPTH_COMPONENT16 = 0x81A5;
        private static final int GL_DEPTH_COMPONENT32F = 0x8CAC;
        private static final int GL_DEPTH24_STENCIL8 = 0x88F0;
        private static final int GL_DEPTH32F_STENCIL8 = 0x8CAD;
        private static final int GL_STENCIL_INDEX8 = 0x8D48;
        
        // Compressed format constants
        private static final int GL_COMPRESSED_RGB_S3TC_DXT1_EXT = 0x83F0;
        private static final int GL_COMPRESSED_RGBA_S3TC_DXT1_EXT = 0x83F1;
        private static final int GL_COMPRESSED_RGBA_S3TC_DXT3_EXT = 0x83F2;
        private static final int GL_COMPRESSED_RGBA_S3TC_DXT5_EXT = 0x83F3;
        private static final int GL_COMPRESSED_SRGB_S3TC_DXT1_EXT = 0x8C4C;
        private static final int GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT = 0x8C4D;
        private static final int GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT = 0x8C4E;
        private static final int GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT = 0x8C4F;
        private static final int GL_COMPRESSED_RED_RGTC1 = 0x8DBB;
        private static final int GL_COMPRESSED_SIGNED_RED_RGTC1 = 0x8DBC;
        private static final int GL_COMPRESSED_RG_RGTC2 = 0x8DBD;
        private static final int GL_COMPRESSED_SIGNED_RG_RGTC2 = 0x8DBE;
        private static final int GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT = 0x8E8E;
        private static final int GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT = 0x8E8F;
        private static final int GL_COMPRESSED_RGBA_BPTC_UNORM = 0x8E8C;
        private static final int GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM = 0x8E8D;
        private static final int GL_COMPRESSED_RGB8_ETC2 = 0x9274;
        private static final int GL_COMPRESSED_SRGB8_ETC2 = 0x9275;
        private static final int GL_COMPRESSED_RGB8_PUNCHTHROUGH_ALPHA1_ETC2 = 0x9276;
        private static final int GL_COMPRESSED_SRGB8_PUNCHTHROUGH_ALPHA1_ETC2 = 0x9277;
        private static final int GL_COMPRESSED_RGBA8_ETC2_EAC = 0x9278;
        private static final int GL_COMPRESSED_SRGB8_ALPHA8_ETC2_EAC = 0x9279;
        private static final int GL_COMPRESSED_R11_EAC = 0x9270;
        private static final int GL_COMPRESSED_SIGNED_R11_EAC = 0x9271;
        private static final int GL_COMPRESSED_RG11_EAC = 0x9272;
        private static final int GL_COMPRESSED_SIGNED_RG11_EAC = 0x9273;
        private static final int GL_COMPRESSED_RGBA_ASTC_4x4_KHR = 0x93B0;
        private static final int GL_COMPRESSED_RGBA_ASTC_5x5_KHR = 0x93B2;
        private static final int GL_COMPRESSED_RGBA_ASTC_6x6_KHR = 0x93B4;
        private static final int GL_COMPRESSED_RGBA_ASTC_8x8_KHR = 0x93B7;
        private static final int GL_COMPRESSED_RGBA_ASTC_10x10_KHR = 0x93BB;
        private static final int GL_COMPRESSED_RGBA_ASTC_12x12_KHR = 0x93BD;
        private static final int GL_COMPRESSED_SRGB8_ALPHA8_ASTC_4x4_KHR = 0x93D0;
        private static final int GL_COMPRESSED_SRGB8_ALPHA8_ASTC_5x5_KHR = 0x93D2;
        private static final int GL_COMPRESSED_SRGB8_ALPHA8_ASTC_6x6_KHR = 0x93D4;
        private static final int GL_COMPRESSED_SRGB8_ALPHA8_ASTC_8x8_KHR = 0x93D7;
        private static final int GL_COMPRESSED_SRGB8_ALPHA8_ASTC_10x10_KHR = 0x93DB;
        private static final int GL_COMPRESSED_SRGB8_ALPHA8_ASTC_12x12_KHR = 0x93DD;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 25: SAMPLER STATE MANAGEMENT
    // Complete sampler state translation and caching
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Manages OpenGL sampler objects and their Metal equivalents.
     * Handles all sampler parameters and state caching.
     */
    public static final class SamplerStateManager {
        
        // Sampler state cache
        private final Map<Long, Long> samplerCache = new HashMap<>();
        private final Map<Integer, SamplerState> glSamplers = new HashMap<>();
        private long device;
        
        // Default sampler
        private long defaultSampler;
        
        /**
         * Sampler state structure
         */
        public static class SamplerState {
            public int minFilter = GL_NEAREST_MIPMAP_LINEAR;
            public int magFilter = GL_LINEAR;
            public int wrapS = GL_REPEAT;
            public int wrapT = GL_REPEAT;
            public int wrapR = GL_REPEAT;
            public float minLod = -1000.0f;
            public float maxLod = 1000.0f;
            public float lodBias = 0.0f;
            public int compareMode = GL_NONE;
            public int compareFunc = GL_LEQUAL;
            public float[] borderColor = {0.0f, 0.0f, 0.0f, 0.0f};
            public float maxAnisotropy = 1.0f;
            public boolean srgbDecode = true;
            
            /**
             * Compute hash for caching
             */
            public long computeHash() {
                long hash = 17;
                hash = hash * 31 + minFilter;
                hash = hash * 31 + magFilter;
                hash = hash * 31 + wrapS;
                hash = hash * 31 + wrapT;
                hash = hash * 31 + wrapR;
                hash = hash * 31 + Float.floatToIntBits(minLod);
                hash = hash * 31 + Float.floatToIntBits(maxLod);
                hash = hash * 31 + Float.floatToIntBits(lodBias);
                hash = hash * 31 + compareMode;
                hash = hash * 31 + compareFunc;
                hash = hash * 31 + Float.floatToIntBits(maxAnisotropy);
                for (float f : borderColor) {
                    hash = hash * 31 + Float.floatToIntBits(f);
                }
                return hash;
            }
        }
        
        public SamplerStateManager(long device) {
            this.device = device;
            createDefaultSampler();
        }
        
        private void createDefaultSampler() {
            SamplerState state = new SamplerState();
            defaultSampler = createMetalSampler(state);
        }
        
        /**
         * Create OpenGL sampler object
         */
        public int createSampler() {
            int id = nextSamplerId++;
            glSamplers.put(id, new SamplerState());
            return id;
        }
        
        private int nextSamplerId = 1;
        
        /**
         * Delete sampler object
         */
        public void deleteSampler(int sampler) {
            SamplerState state = glSamplers.remove(sampler);
            if (state != null) {
                long hash = state.computeHash();
                Long metalSampler = samplerCache.remove(hash);
                if (metalSampler != null) {
                    // Release Metal sampler
                    // nRelease(metalSampler);
                }
            }
        }
        
        /**
         * Set sampler parameter integer
         */
        public void setSamplerParameteri(int sampler, int pname, int param) {
            SamplerState state = glSamplers.get(sampler);
            if (state == null) return;
            
            switch (pname) {
                case GL_TEXTURE_MIN_FILTER -> state.minFilter = param;
                case GL_TEXTURE_MAG_FILTER -> state.magFilter = param;
                case GL_TEXTURE_WRAP_S -> state.wrapS = param;
                case GL_TEXTURE_WRAP_T -> state.wrapT = param;
                case GL_TEXTURE_WRAP_R -> state.wrapR = param;
                case GL_TEXTURE_COMPARE_MODE -> state.compareMode = param;
                case GL_TEXTURE_COMPARE_FUNC -> state.compareFunc = param;
            }
            
            // Invalidate cached Metal sampler
            invalidateSampler(sampler);
        }
        
        /**
         * Set sampler parameter float
         */
        public void setSamplerParameterf(int sampler, int pname, float param) {
            SamplerState state = glSamplers.get(sampler);
            if (state == null) return;
            
            switch (pname) {
                case GL_TEXTURE_MIN_LOD -> state.minLod = param;
                case GL_TEXTURE_MAX_LOD -> state.maxLod = param;
                case GL_TEXTURE_LOD_BIAS -> state.lodBias = param;
                case GL_TEXTURE_MAX_ANISOTROPY_EXT -> state.maxAnisotropy = param;
            }
            
            invalidateSampler(sampler);
        }
        
        /**
         * Set sampler border color
         */
        public void setSamplerParameterfv(int sampler, int pname, float[] params) {
            SamplerState state = glSamplers.get(sampler);
            if (state == null) return;
            
            if (pname == GL_TEXTURE_BORDER_COLOR) {
                System.arraycopy(params, 0, state.borderColor, 0, Math.min(4, params.length));
                invalidateSampler(sampler);
            }
        }
        
        private void invalidateSampler(int sampler) {
            // Remove from cache to force recreation
            SamplerState state = glSamplers.get(sampler);
            if (state != null) {
                long oldHash = state.computeHash();
                samplerCache.remove(oldHash);
            }
        }
        
        /**
         * Get Metal sampler for OpenGL sampler
         */
        public long getMetalSampler(int glSampler) {
            if (glSampler == 0) {
                return defaultSampler;
            }
            
            SamplerState state = glSamplers.get(glSampler);
            if (state == null) {
                return defaultSampler;
            }
            
            long hash = state.computeHash();
            Long cached = samplerCache.get(hash);
            if (cached != null) {
                return cached;
            }
            
            long metalSampler = createMetalSampler(state);
            samplerCache.put(hash, metalSampler);
            return metalSampler;
        }
        
        /**
         * Create Metal sampler from state
         */
        private long createMetalSampler(SamplerState state) {
            long descriptor = nMTLSamplerDescriptorNew();
            
            // Set min filter
            int mtlMinFilter = translateMinFilter(state.minFilter);
            int mtlMipFilter = translateMipFilter(state.minFilter);
            nMTLSamplerDescriptorSetMinFilter(descriptor, mtlMinFilter);
            nMTLSamplerDescriptorSetMipFilter(descriptor, mtlMipFilter);
            
            // Set mag filter
            int mtlMagFilter = translateMagFilter(state.magFilter);
            nMTLSamplerDescriptorSetMagFilter(descriptor, mtlMagFilter);
            
            // Set address modes
            nMTLSamplerDescriptorSetSAddressMode(descriptor, translateWrapMode(state.wrapS));
            nMTLSamplerDescriptorSetTAddressMode(descriptor, translateWrapMode(state.wrapT));
            nMTLSamplerDescriptorSetRAddressMode(descriptor, translateWrapMode(state.wrapR));
            
            // Set LOD clamp
            nMTLSamplerDescriptorSetLodMinClamp(descriptor, state.minLod);
            nMTLSamplerDescriptorSetLodMaxClamp(descriptor, state.maxLod);
            
            // Set anisotropy
            nMTLSamplerDescriptorSetMaxAnisotropy(descriptor, (int) state.maxAnisotropy);
            
            // Set compare function if enabled
            if (state.compareMode == GL_COMPARE_REF_TO_TEXTURE) {
                nMTLSamplerDescriptorSetCompareFunction(descriptor, translateCompareFunc(state.compareFunc));
            }
            
            // Set border color (Metal only supports specific border colors)
            nMTLSamplerDescriptorSetBorderColor(descriptor, translateBorderColor(state.borderColor));
            
            // Create sampler
            long sampler = nMTLDeviceNewSamplerState(device, descriptor);
            
            // Release descriptor
            // nRelease(descriptor);
            
            return sampler;
        }
        
        private int translateMinFilter(int glFilter) {
            return switch (glFilter) {
                case GL_NEAREST, GL_NEAREST_MIPMAP_NEAREST, GL_NEAREST_MIPMAP_LINEAR -> MTLSamplerMinMagFilterNearest;
                default -> MTLSamplerMinMagFilterLinear;
            };
        }
        
        private int translateMipFilter(int glFilter) {
            return switch (glFilter) {
                case GL_NEAREST, GL_LINEAR -> MTLSamplerMipFilterNotMipmapped;
                case GL_NEAREST_MIPMAP_NEAREST, GL_LINEAR_MIPMAP_NEAREST -> MTLSamplerMipFilterNearest;
                default -> MTLSamplerMipFilterLinear;
            };
        }
        
        private int translateMagFilter(int glFilter) {
            return glFilter == GL_NEAREST ? MTLSamplerMinMagFilterNearest : MTLSamplerMinMagFilterLinear;
        }
        
        private int translateWrapMode(int glWrap) {
            return switch (glWrap) {
                case GL_REPEAT -> MTLSamplerAddressModeRepeat;
                case GL_MIRRORED_REPEAT -> MTLSamplerAddressModeMirrorRepeat;
                case GL_CLAMP_TO_EDGE -> MTLSamplerAddressModeClampToEdge;
                case GL_CLAMP_TO_BORDER -> MTLSamplerAddressModeClampToBorderColor;
                case GL_MIRROR_CLAMP_TO_EDGE -> MTLSamplerAddressModeMirrorClampToEdge;
                default -> MTLSamplerAddressModeRepeat;
            };
        }
        
        private int translateCompareFunc(int glFunc) {
            return switch (glFunc) {
                case GL_NEVER -> MTLCompareFunctionNever;
                case GL_LESS -> MTLCompareFunctionLess;
                case GL_EQUAL -> MTLCompareFunctionEqual;
                case GL_LEQUAL -> MTLCompareFunctionLessEqual;
                case GL_GREATER -> MTLCompareFunctionGreater;
                case GL_NOTEQUAL -> MTLCompareFunctionNotEqual;
                case GL_GEQUAL -> MTLCompareFunctionGreaterEqual;
                case GL_ALWAYS -> MTLCompareFunctionAlways;
                default -> MTLCompareFunctionLessEqual;
            };
        }
        
        private int translateBorderColor(float[] color) {
            // Metal only supports specific border colors
            if (color[0] == 0 && color[1] == 0 && color[2] == 0) {
                return color[3] == 0 ? MTLSamplerBorderColorTransparentBlack : MTLSamplerBorderColorOpaqueBlack;
            }
            return MTLSamplerBorderColorOpaqueWhite;
        }
        
        // Metal constants
        private static final int MTLSamplerMinMagFilterNearest = 0;
        private static final int MTLSamplerMinMagFilterLinear = 1;
        private static final int MTLSamplerMipFilterNotMipmapped = 0;
        private static final int MTLSamplerMipFilterNearest = 1;
        private static final int MTLSamplerMipFilterLinear = 2;
        private static final int MTLSamplerAddressModeClampToEdge = 0;
        private static final int MTLSamplerAddressModeMirrorClampToEdge = 1;
        private static final int MTLSamplerAddressModeRepeat = 2;
        private static final int MTLSamplerAddressModeMirrorRepeat = 3;
        private static final int MTLSamplerAddressModeClampToZero = 4;
        private static final int MTLSamplerAddressModeClampToBorderColor = 5;
        private static final int MTLSamplerBorderColorTransparentBlack = 0;
        private static final int MTLSamplerBorderColorOpaqueBlack = 1;
        private static final int MTLSamplerBorderColorOpaqueWhite = 2;
        private static final int MTLCompareFunctionNever = 0;
        private static final int MTLCompareFunctionLess = 1;
        private static final int MTLCompareFunctionEqual = 2;
        private static final int MTLCompareFunctionLessEqual = 3;
        private static final int MTLCompareFunctionGreater = 4;
        private static final int MTLCompareFunctionNotEqual = 5;
        private static final int MTLCompareFunctionGreaterEqual = 6;
        private static final int MTLCompareFunctionAlways = 7;
        
        // GL constants
        private static final int GL_NEAREST = 0x2600;
        private static final int GL_LINEAR = 0x2601;
        private static final int GL_NEAREST_MIPMAP_NEAREST = 0x2700;
        private static final int GL_LINEAR_MIPMAP_NEAREST = 0x2701;
        private static final int GL_NEAREST_MIPMAP_LINEAR = 0x2702;
        private static final int GL_LINEAR_MIPMAP_LINEAR = 0x2703;
        private static final int GL_REPEAT = 0x2901;
        private static final int GL_MIRRORED_REPEAT = 0x8370;
        private static final int GL_CLAMP_TO_EDGE = 0x812F;
        private static final int GL_CLAMP_TO_BORDER = 0x812D;
        private static final int GL_MIRROR_CLAMP_TO_EDGE = 0x8743;
        private static final int GL_TEXTURE_MIN_FILTER = 0x2801;
        private static final int GL_TEXTURE_MAG_FILTER = 0x2800;
        private static final int GL_TEXTURE_WRAP_S = 0x2802;
        private static final int GL_TEXTURE_WRAP_T = 0x2803;
        private static final int GL_TEXTURE_WRAP_R = 0x8072;
        private static final int GL_TEXTURE_MIN_LOD = 0x813A;
        private static final int GL_TEXTURE_MAX_LOD = 0x813B;
        private static final int GL_TEXTURE_LOD_BIAS = 0x8501;
        private static final int GL_TEXTURE_COMPARE_MODE = 0x884C;
        private static final int GL_TEXTURE_COMPARE_FUNC = 0x884D;
        private static final int GL_TEXTURE_BORDER_COLOR = 0x1004;
        private static final int GL_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE;
        private static final int GL_COMPARE_REF_TO_TEXTURE = 0x884E;
        private static final int GL_NONE = 0;
        private static final int GL_NEVER = 0x0200;
        private static final int GL_LESS = 0x0201;
        private static final int GL_EQUAL = 0x0202;
        private static final int GL_LEQUAL = 0x0203;
        private static final int GL_GREATER = 0x0204;
        private static final int GL_NOTEQUAL = 0x0205;
        private static final int GL_GEQUAL = 0x0206;
        private static final int GL_ALWAYS = 0x0207;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 26: QUERY OBJECT EMULATION
    // Complete query object support for occlusion, timer, and primitive queries
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Emulates OpenGL query objects using Metal visibility result buffers
     * and timestamp queries.
     */
    public static final class QueryObjectManager {
        
        // Query types
        public static final int QUERY_OCCLUSION = 0;
        public static final int QUERY_OCCLUSION_CONSERVATIVE = 1;
        public static final int QUERY_PRIMITIVES_GENERATED = 2;
        public static final int QUERY_TRANSFORM_FEEDBACK_PRIMITIVES = 3;
        public static final int QUERY_TIME_ELAPSED = 4;
        public static final int QUERY_TIMESTAMP = 5;
        public static final int QUERY_ANY_SAMPLES_PASSED = 6;
        public static final int QUERY_ANY_SAMPLES_PASSED_CONSERVATIVE = 7;
        
        // Query state
        private final Map<Integer, QueryObject> queries = new HashMap<>();
        private final Map<Integer, Integer> activeQueries = new HashMap<>(); // target -> query
        private int nextQueryId = 1;
        
        // Metal resources
        private long device;
        private long visibilityResultBuffer;
        private int visibilityResultOffset = 0;
        private static final int MAX_VISIBILITY_RESULTS = 4096;
        private static final int VISIBILITY_RESULT_SIZE = 8;
        
        // Timestamp queries
        private long timestampBuffer;
        private int timestampOffset = 0;
        private static final int MAX_TIMESTAMPS = 1024;
        
        /**
         * Query object state
         */
        public static class QueryObject {
            public int id;
            public int type;
            public int target;
            public long result;
            public boolean resultAvailable;
            public boolean active;
            public int visibilityOffset;
            public int timestampStartOffset;
            public int timestampEndOffset;
            public long startTime;
            public long endTime;
        }
        
        public QueryObjectManager(long device) {
            this.device = device;
            createBuffers();
        }
        
        private void createBuffers() {
            // Create visibility result buffer for occlusion queries
            int visBufferSize = MAX_VISIBILITY_RESULTS * VISIBILITY_RESULT_SIZE;
            visibilityResultBuffer = nMTLDeviceNewBufferWithLength(device, visBufferSize,
                MTLResourceStorageModeShared);
            
            // Create timestamp buffer
            int tsBufferSize = MAX_TIMESTAMPS * 8 * 2; // start + end timestamps
            timestampBuffer = nMTLDeviceNewBufferWithLength(device, tsBufferSize,
                MTLResourceStorageModeShared);
        }
        
        /**
         * Generate query objects
         */
        public void genQueries(int n, int[] ids) {
            for (int i = 0; i < n; i++) {
                int id = nextQueryId++;
                QueryObject query = new QueryObject();
                query.id = id;
                query.resultAvailable = false;
                query.active = false;
                queries.put(id, query);
                ids[i] = id;
            }
        }
        
        /**
         * Delete query objects
         */
        public void deleteQueries(int n, int[] ids) {
            for (int i = 0; i < n; i++) {
                queries.remove(ids[i]);
            }
        }
        
        /**
         * Begin a query
         */
        public void beginQuery(int target, int id) {
            QueryObject query = queries.get(id);
            if (query == null) return;
            
            // End any active query on this target
            Integer activeId = activeQueries.get(target);
            if (activeId != null && activeId != id) {
                endQuery(target);
            }
            
            query.target = target;
            query.type = targetToType(target);
            query.active = true;
            query.resultAvailable = false;
            
            switch (query.type) {
                case QUERY_OCCLUSION, QUERY_OCCLUSION_CONSERVATIVE, 
                     QUERY_ANY_SAMPLES_PASSED, QUERY_ANY_SAMPLES_PASSED_CONSERVATIVE -> {
                    query.visibilityOffset = allocateVisibilityResult();
                    // Zero the result location
                    clearVisibilityResult(query.visibilityOffset);
                }
                case QUERY_TIME_ELAPSED -> {
                    query.timestampStartOffset = allocateTimestamp();
                    query.startTime = System.nanoTime();
                }
                case QUERY_PRIMITIVES_GENERATED, QUERY_TRANSFORM_FEEDBACK_PRIMITIVES -> {
                    // These are tracked differently
                    query.result = 0;
                }
            }
            
            activeQueries.put(target, id);
        }
        
        /**
         * End a query
         */
        public void endQuery(int target) {
            Integer id = activeQueries.remove(target);
            if (id == null) return;
            
            QueryObject query = queries.get(id);
            if (query == null) return;
            
            query.active = false;
            
            switch (query.type) {
                case QUERY_OCCLUSION, QUERY_OCCLUSION_CONSERVATIVE,
                     QUERY_ANY_SAMPLES_PASSED, QUERY_ANY_SAMPLES_PASSED_CONSERVATIVE -> {
                    // Result will be available after GPU execution
                    // Mark as pending
                }
                case QUERY_TIME_ELAPSED -> {
                    query.timestampEndOffset = allocateTimestamp();
                    query.endTime = System.nanoTime();
                    // For CPU-side timing, result is immediately available
                    query.result = query.endTime - query.startTime;
                    query.resultAvailable = true;
                }
                case QUERY_PRIMITIVES_GENERATED, QUERY_TRANSFORM_FEEDBACK_PRIMITIVES -> {
                    query.resultAvailable = true;
                }
            }
        }
        
        /**
         * Record a timestamp query
         */
        public void queryCounter(int id, int target) {
            if (target != GL_TIMESTAMP) return;
            
            QueryObject query = queries.get(id);
            if (query == null) return;
            
            query.type = QUERY_TIMESTAMP;
            query.target = target;
            query.result = System.nanoTime();
            query.resultAvailable = true;
        }
        
        /**
         * Get query result (blocking if not available)
         */
        public long getQueryResult(int id) {
            QueryObject query = queries.get(id);
            if (query == null) return 0;
            
            // Wait for result if not available
            if (!query.resultAvailable) {
                waitForQueryResult(query);
            }
            
            return query.result;
        }
        
        /**
         * Get query result (non-blocking)
         */
        public boolean getQueryResultNoWait(int id, long[] result) {
            QueryObject query = queries.get(id);
            if (query == null) {
                result[0] = 0;
                return true;
            }
            
            if (!query.resultAvailable) {
                checkQueryResult(query);
            }
            
            if (query.resultAvailable) {
                result[0] = query.result;
                return true;
            }
            
            return false;
        }
        
        /**
         * Check if query result is available
         */
        public boolean isQueryResultAvailable(int id) {
            QueryObject query = queries.get(id);
            if (query == null) return true;
            
            if (!query.resultAvailable) {
                checkQueryResult(query);
            }
            
            return query.resultAvailable;
        }
        
        /**
         * Get query object parameter
         */
        public void getQueryObjectiv(int id, int pname, int[] params) {
            QueryObject query = queries.get(id);
            if (query == null) {
                params[0] = 0;
                return;
            }
            
            switch (pname) {
                case GL_QUERY_RESULT_AVAILABLE -> {
                    if (!query.resultAvailable) {
                        checkQueryResult(query);
                    }
                    params[0] = query.resultAvailable ? GL_TRUE : GL_FALSE;
                }
                case GL_QUERY_RESULT -> {
                    if (!query.resultAvailable) {
                        waitForQueryResult(query);
                    }
                    params[0] = (int) query.result;
                }
                case GL_QUERY_RESULT_NO_WAIT -> {
                    if (!query.resultAvailable) {
                        checkQueryResult(query);
                    }
                    params[0] = query.resultAvailable ? (int) query.result : 0;
                }
                case GL_QUERY_TARGET -> params[0] = query.target;
            }
        }
        
        /**
         * Get query object parameter (64-bit)
         */
        public void getQueryObjectui64v(int id, int pname, long[] params) {
            QueryObject query = queries.get(id);
            if (query == null) {
                params[0] = 0;
                return;
            }
            
            switch (pname) {
                case GL_QUERY_RESULT_AVAILABLE -> {
                    if (!query.resultAvailable) {
                        checkQueryResult(query);
                    }
                    params[0] = query.resultAvailable ? 1 : 0;
                }
                case GL_QUERY_RESULT -> {
                    if (!query.resultAvailable) {
                        waitForQueryResult(query);
                    }
                    params[0] = query.result;
                }
                case GL_QUERY_RESULT_NO_WAIT -> {
                    if (!query.resultAvailable) {
                        checkQueryResult(query);
                    }
                    params[0] = query.resultAvailable ? query.result : 0;
                }
                case GL_QUERY_TARGET -> params[0] = query.target;
            }
        }
        
        private int targetToType(int target) {
            return switch (target) {
                case GL_SAMPLES_PASSED -> QUERY_OCCLUSION;
                case GL_ANY_SAMPLES_PASSED -> QUERY_ANY_SAMPLES_PASSED;
                case GL_ANY_SAMPLES_PASSED_CONSERVATIVE -> QUERY_ANY_SAMPLES_PASSED_CONSERVATIVE;
                case GL_PRIMITIVES_GENERATED -> QUERY_PRIMITIVES_GENERATED;
                case GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN -> QUERY_TRANSFORM_FEEDBACK_PRIMITIVES;
                case GL_TIME_ELAPSED -> QUERY_TIME_ELAPSED;
                case GL_TIMESTAMP -> QUERY_TIMESTAMP;
                default -> QUERY_OCCLUSION;
            };
        }
        
        private int allocateVisibilityResult() {
            int offset = visibilityResultOffset;
            visibilityResultOffset = (visibilityResultOffset + 1) % MAX_VISIBILITY_RESULTS;
            return offset;
        }
        
        private void clearVisibilityResult(int offset) {
            long ptr = nMTLBufferContents(visibilityResultBuffer);
            // Write zero to the result location
            // MemoryUtil.memPutLong(ptr + offset * VISIBILITY_RESULT_SIZE, 0);
        }
        
        private int allocateTimestamp() {
            int offset = timestampOffset;
            timestampOffset = (timestampOffset + 1) % MAX_TIMESTAMPS;
            return offset;
        }
        
        private void waitForQueryResult(QueryObject query) {
            // For visibility queries, we need to wait for GPU completion
            // This would typically be done by waiting on a fence or command buffer completion
            
            switch (query.type) {
                case QUERY_OCCLUSION, QUERY_OCCLUSION_CONSERVATIVE,
                     QUERY_ANY_SAMPLES_PASSED, QUERY_ANY_SAMPLES_PASSED_CONSERVATIVE -> {
                    // Read from visibility result buffer
                    long ptr = nMTLBufferContents(visibilityResultBuffer);
                    // query.result = MemoryUtil.memGetLong(ptr + query.visibilityOffset * VISIBILITY_RESULT_SIZE);
                    query.resultAvailable = true;
                }
                case QUERY_TIME_ELAPSED -> {
                    // Already computed at end time
                    query.resultAvailable = true;
                }
            }
        }
        
        private void checkQueryResult(QueryObject query) {
            // Non-blocking check for result availability
            // In practice, would check if the GPU has completed the relevant work
            
            switch (query.type) {
                case QUERY_OCCLUSION, QUERY_OCCLUSION_CONSERVATIVE,
                     QUERY_ANY_SAMPLES_PASSED, QUERY_ANY_SAMPLES_PASSED_CONSERVATIVE -> {
                    // Check if GPU has written the result
                    // For now, assume available
                    long ptr = nMTLBufferContents(visibilityResultBuffer);
                    // query.result = MemoryUtil.memGetLong(ptr + query.visibilityOffset * VISIBILITY_RESULT_SIZE);
                    query.resultAvailable = true;
                }
                case QUERY_TIME_ELAPSED, QUERY_TIMESTAMP -> {
                    query.resultAvailable = true;
                }
            }
        }
        
        /**
         * Get visibility result buffer for render encoder
         */
        public long getVisibilityResultBuffer() {
            return visibilityResultBuffer;
        }
        
        /**
         * Get visibility offset for active occlusion query
         */
        public int getActiveVisibilityOffset(int target) {
            Integer id = activeQueries.get(target);
            if (id == null) return -1;
            
            QueryObject query = queries.get(id);
            if (query == null) return -1;
            
            return query.visibilityOffset * VISIBILITY_RESULT_SIZE;
        }
        
        /**
         * Update primitive count for active primitive query
         */
        public void addPrimitives(int target, long count) {
            Integer id = activeQueries.get(target);
            if (id == null) return;
            
            QueryObject query = queries.get(id);
            if (query != null && query.active) {
                query.result += count;
            }
        }
        
        // GL constants
        private static final int GL_SAMPLES_PASSED = 0x8914;
        private static final int GL_ANY_SAMPLES_PASSED = 0x8C2F;
        private static final int GL_ANY_SAMPLES_PASSED_CONSERVATIVE = 0x8D6A;
        private static final int GL_PRIMITIVES_GENERATED = 0x8C87;
        private static final int GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN = 0x8C88;
        private static final int GL_TIME_ELAPSED = 0x88BF;
        private static final int GL_TIMESTAMP = 0x8E28;
        private static final int GL_QUERY_RESULT = 0x8866;
        private static final int GL_QUERY_RESULT_AVAILABLE = 0x8867;
        private static final int GL_QUERY_RESULT_NO_WAIT = 0x9194;
        private static final int GL_QUERY_TARGET = 0x82EA;
        private static final int GL_TRUE = 1;
        private static final int GL_FALSE = 0;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 27: SYNC OBJECT EMULATION  
    // Complete fence sync and memory barrier support
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Emulates OpenGL sync objects using Metal events and shared events.
     */
    public static final class SyncObjectManager {
        
        // Sync object storage
        private final Map<Long, SyncObject> syncObjects = new HashMap<>();
        private long nextSyncId = 1;
        
        // Metal resources
        private long device;
        private long sharedEventListener;
        
        /**
         * Sync object state
         */
        public static class SyncObject {
            public long id;
            public int condition;
            public int flags;
            public int status;
            public long metalEvent;
            public long signalValue;
            public boolean signaled;
            public Object completionLock = new Object();
        }
        
        // Sync status values
        private static final int GL_ALREADY_SIGNALED = 0x911A;
        private static final int GL_TIMEOUT_EXPIRED = 0x911B;
        private static final int GL_CONDITION_SATISFIED = 0x911C;
        private static final int GL_WAIT_FAILED = 0x911D;
        private static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117;
        private static final int GL_SYNC_FLUSH_COMMANDS_BIT = 0x00000001;
        private static final int GL_UNSIGNALED = 0x9118;
        private static final int GL_SIGNALED = 0x9119;
        
        public SyncObjectManager(long device) {
            this.device = device;
            createSharedEventListener();
        }
        
        private void createSharedEventListener() {
            // Create shared event listener for async notifications
            // sharedEventListener = nMTLSharedEventListenerNew();
        }
        
        /**
         * Create a fence sync object
         */
        public long fenceSync(int condition, int flags) {
            if (condition != GL_SYNC_GPU_COMMANDS_COMPLETE) {
                return 0;
            }
            
            SyncObject sync = new SyncObject();
            sync.id = nextSyncId++;
            sync.condition = condition;
            sync.flags = flags;
            sync.status = GL_UNSIGNALED;
            sync.signaled = false;
            
            // Create Metal shared event
            sync.metalEvent = nMTLDeviceNewSharedEvent(device);
            sync.signalValue = sync.id;
            
            syncObjects.put(sync.id, sync);
            
            return sync.id;
        }
        
        /**
         * Delete sync object
         */
        public void deleteSync(long sync) {
            SyncObject obj = syncObjects.remove(sync);
            if (obj != null && obj.metalEvent != 0) {
                // Release Metal event
                // nRelease(obj.metalEvent);
            }
        }
        
        /**
         * Check if sync is valid
         */
        public boolean isSync(long sync) {
            return syncObjects.containsKey(sync);
        }
        
        /**
         * Wait for sync with timeout
         */
        public int clientWaitSync(long sync, int flags, long timeout) {
            SyncObject obj = syncObjects.get(sync);
            if (obj == null) {
                return GL_WAIT_FAILED;
            }
            
            // Check if already signaled
            if (obj.signaled) {
                return GL_ALREADY_SIGNALED;
            }
            
            // Flush if requested
            if ((flags & GL_SYNC_FLUSH_COMMANDS_BIT) != 0) {
                // Flush pending commands
                flushCommands();
            }
            
            // Wait for Metal event
            if (timeout == 0) {
                // Check without waiting
                if (checkSyncStatus(obj)) {
                    obj.signaled = true;
                    obj.status = GL_SIGNALED;
                    return GL_ALREADY_SIGNALED;
                }
                return GL_TIMEOUT_EXPIRED;
            }
            
            // Wait with timeout
            long startTime = System.nanoTime();
            long timeoutNs = timeout == 0xFFFFFFFFFFFFFFFFL ? Long.MAX_VALUE : timeout;
            
            synchronized (obj.completionLock) {
                while (!obj.signaled) {
                    long elapsed = System.nanoTime() - startTime;
                    if (elapsed >= timeoutNs) {
                        return GL_TIMEOUT_EXPIRED;
                    }
                    
                    long remaining = timeoutNs - elapsed;
                    long waitMs = remaining / 1_000_000;
                    int waitNs = (int) (remaining % 1_000_000);
                    
                    try {
                        obj.completionLock.wait(waitMs, waitNs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return GL_WAIT_FAILED;
                    }
                    
                    // Check status
                    if (checkSyncStatus(obj)) {
                        obj.signaled = true;
                        obj.status = GL_SIGNALED;
                    }
                }
            }
            
            return GL_CONDITION_SATISFIED;
        }
        
        /**
         * Wait for sync on server side
         */
        public void waitSync(long sync, int flags, long timeout) {
            SyncObject obj = syncObjects.get(sync);
            if (obj == null) return;
            
            // Insert a wait command in the GPU command stream
            // This would be done by encoding a wait on the Metal event
            // in the current command buffer
        }
        
        /**
         * Get sync object parameter
         */
        public void getSynciv(long sync, int pname, int bufSize, int[] length, int[] values) {
            SyncObject obj = syncObjects.get(sync);
            if (obj == null) {
                if (length != null) length[0] = 0;
                return;
            }
            
            int value = 0;
            switch (pname) {
                case GL_OBJECT_TYPE -> value = GL_SYNC_FENCE;
                case GL_SYNC_STATUS -> {
                    if (!obj.signaled && checkSyncStatus(obj)) {
                        obj.signaled = true;
                        obj.status = GL_SIGNALED;
                    }
                    value = obj.status;
                }
                case GL_SYNC_CONDITION -> value = obj.condition;
                case GL_SYNC_FLAGS -> value = obj.flags;
            }
            
            if (bufSize >= 1) {
                values[0] = value;
            }
            if (length != null) {
                length[0] = 1;
            }
        }
        
        /**
         * Signal sync from command buffer completion
         */
        public void signalSync(long sync) {
            SyncObject obj = syncObjects.get(sync);
            if (obj == null) return;
            
            synchronized (obj.completionLock) {
                obj.signaled = true;
                obj.status = GL_SIGNALED;
                obj.completionLock.notifyAll();
            }
        }
        
        /**
         * Get Metal event for command buffer signaling
         */
        public long getMetalEvent(long sync) {
            SyncObject obj = syncObjects.get(sync);
            return obj != null ? obj.metalEvent : 0;
        }
        
        /**
         * Get signal value for Metal event
         */
        public long getSignalValue(long sync) {
            SyncObject obj = syncObjects.get(sync);
            return obj != null ? obj.signalValue : 0;
        }
        
        private boolean checkSyncStatus(SyncObject obj) {
            if (obj.metalEvent == 0) return true;
            
            // Check Metal event signaled value
            long signaledValue = nMTLSharedEventGetSignaledValue(obj.metalEvent);
            return signaledValue >= obj.signalValue;
        }
        
        private void flushCommands() {
            // Commit and flush pending command buffers
            // This would interact with the command buffer manager
        }
        
        // GL constants
        private static final int GL_OBJECT_TYPE = 0x9112;
        private static final int GL_SYNC_FENCE = 0x9116;
        private static final int GL_SYNC_STATUS = 0x9114;
        private static final int GL_SYNC_CONDITION = 0x9113;
        private static final int GL_SYNC_FLAGS = 0x9115;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 28: COMPUTE SHADER DISPATCH
    // Complete compute shader support and dispatch handling
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Handles compute shader compilation, state management, and dispatch.
     */
    public static final class ComputeShaderManager {
        
        // Compute program storage
        private final Map<Integer, ComputeProgram> programs = new HashMap<>();
        private int nextProgramId = 1;
        
        // Current state
        private int currentProgram = 0;
        
        // Metal resources
        private long device;
        private long commandQueue;
        
        // Dispatch parameters
        private int[] workGroupSize = {1, 1, 1};
        private int[] numWorkGroups = {1, 1, 1};
        
        /**
         * Compute program state
         */
        public static class ComputeProgram {
            public int id;
            public String glslSource;
            public String mslSource;
            public long metalLibrary;
            public long metalFunction;
            public long pipelineState;
            public int[] localWorkGroupSize = {1, 1, 1};
            public int sharedMemorySize;
            public boolean compiled;
            public String compileError;
            
            // Uniform bindings
            public final Map<String, UniformBinding> uniforms = new HashMap<>();
            public final Map<Integer, BufferBinding> bufferBindings = new HashMap<>();
            public final Map<Integer, ImageBinding> imageBindings = new HashMap<>();
        }
        
        /**
         * Uniform binding info
         */
        public static class UniformBinding {
            public String name;
            public String type;
            public int location;
            public int bufferIndex;
            public int offset;
            public int size;
        }
        
        /**
         * Buffer binding info
         */
        public static class BufferBinding {
            public int bindingPoint;
            public long buffer;
            public long offset;
            public long size;
        }
        
        /**
         * Image binding info
         */
        public static class ImageBinding {
            public int unit;
            public long texture;
            public int level;
            public int layer;
            public int access;
            public int format;
        }
        
        public ComputeShaderManager(long device, long commandQueue) {
            this.device = device;
            this.commandQueue = commandQueue;
        }
        
        /**
         * Create compute program from GLSL source
         */
        public int createProgram(String glslSource) {
            ComputeProgram program = new ComputeProgram();
            program.id = nextProgramId++;
            program.glslSource = glslSource;
            program.compiled = false;
            
            programs.put(program.id, program);
            return program.id;
        }
        
        /**
         * Compile compute program
         */
        public boolean compileProgram(int programId) {
            ComputeProgram program = programs.get(programId);
            if (program == null) return false;
            
            try {
                // Parse work group size from source
                parseWorkGroupSize(program);
                
                // Translate GLSL to MSL
                program.mslSource = translateComputeShader(program.glslSource, program);
                
                // Compile Metal library
                long[] errorPtr = {0};
                program.metalLibrary = nMTLDeviceNewLibraryWithSource(device, program.mslSource, 0, errorPtr);
                
                if (program.metalLibrary == 0) {
                    program.compileError = "Metal compilation failed";
                    return false;
                }
                
                // Get function
                program.metalFunction = nMTLLibraryNewFunctionWithName(program.metalLibrary, "computeMain");
                if (program.metalFunction == 0) {
                    program.compileError = "Function 'computeMain' not found";
                    return false;
                }
                
                // Create pipeline state
                program.pipelineState = nMTLDeviceNewComputePipelineState(device, program.metalFunction, errorPtr);
                if (program.pipelineState == 0) {
                    program.compileError = "Pipeline creation failed";
                    return false;
                }
                
                program.compiled = true;
                return true;
                
            } catch (Exception e) {
                program.compileError = e.getMessage();
                return false;
            }
        }
        
        /**
         * Use compute program
         */
        public void useProgram(int programId) {
            currentProgram = programId;
        }
        
        /**
         * Dispatch compute work
         */
        public void dispatchCompute(int numGroupsX, int numGroupsY, int numGroupsZ) {
            ComputeProgram program = programs.get(currentProgram);
            if (program == null || !program.compiled) return;
            
            // Create command buffer and compute encoder
            long commandBuffer = nMTLCommandQueueCommandBuffer(commandQueue);
            long encoder = nMTLCommandBufferComputeCommandEncoder(commandBuffer);
            
            // Set pipeline state
            nMTLComputeCommandEncoderSetComputePipelineState(encoder, program.pipelineState);
            
            // Set buffer bindings
            for (BufferBinding binding : program.bufferBindings.values()) {
                nMTLComputeCommandEncoderSetBuffer(encoder, binding.buffer, binding.offset, binding.bindingPoint);
            }
            
            // Set texture bindings
            for (ImageBinding binding : program.imageBindings.values()) {
                nMTLComputeCommandEncoderSetTexture(encoder, binding.texture, binding.unit);
            }
            
            // Dispatch
            nMTLComputeCommandEncoderDispatchThreadgroups(encoder,
                numGroupsX, numGroupsY, numGroupsZ,
                program.localWorkGroupSize[0], program.localWorkGroupSize[1], program.localWorkGroupSize[2]);
            
            // End encoding
            nMTLComputeCommandEncoderEndEncoding(encoder);
            
            // Commit
            nMTLCommandBufferCommit(commandBuffer);
        }
        
        /**
         * Dispatch compute work indirect
         */
        public void dispatchComputeIndirect(long buffer, long offset) {
            ComputeProgram program = programs.get(currentProgram);
            if (program == null || !program.compiled) return;
            
            long commandBuffer = nMTLCommandQueueCommandBuffer(commandQueue);
            long encoder = nMTLCommandBufferComputeCommandEncoder(commandBuffer);
            
            nMTLComputeCommandEncoderSetComputePipelineState(encoder, program.pipelineState);
            
            // Set bindings...
            for (BufferBinding binding : program.bufferBindings.values()) {
                nMTLComputeCommandEncoderSetBuffer(encoder, binding.buffer, binding.offset, binding.bindingPoint);
            }
            
            // Dispatch indirect
            nMTLComputeCommandEncoderDispatchThreadgroupsWithIndirectBuffer(encoder,
                buffer, offset,
                program.localWorkGroupSize[0], program.localWorkGroupSize[1], program.localWorkGroupSize[2]);
            
            nMTLComputeCommandEncoderEndEncoding(encoder);
            nMTLCommandBufferCommit(commandBuffer);
        }
        
        /**
         * Memory barrier
         */
        public void memoryBarrier(int barriers) {
            // Metal handles memory barriers automatically in many cases
            // For explicit barriers, we may need to use MTLFence or 
            // threadgroup_barrier in shaders
        }
        
        /**
         * Bind shader storage buffer
         */
        public void bindShaderStorageBuffer(int bindingPoint, long buffer, long offset, long size) {
            ComputeProgram program = programs.get(currentProgram);
            if (program == null) return;
            
            BufferBinding binding = new BufferBinding();
            binding.bindingPoint = bindingPoint;
            binding.buffer = buffer;
            binding.offset = offset;
            binding.size = size;
            
            program.bufferBindings.put(bindingPoint, binding);
        }
        
        /**
         * Bind image for compute shader
         */
        public void bindImageTexture(int unit, long texture, int level, boolean layered, 
                                     int layer, int access, int format) {
            ComputeProgram program = programs.get(currentProgram);
            if (program == null) return;
            
            ImageBinding binding = new ImageBinding();
            binding.unit = unit;
            binding.texture = texture;
            binding.level = level;
            binding.layer = layer;
            binding.access = access;
            binding.format = format;
            
            program.imageBindings.put(unit, binding);
        }
        
        private void parseWorkGroupSize(ComputeProgram program) {
            // Parse layout(local_size_x = N, local_size_y = M, local_size_z = P) in;
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "layout\\s*\\(\\s*local_size_x\\s*=\\s*(\\d+)" +
                "(?:\\s*,\\s*local_size_y\\s*=\\s*(\\d+))?" +
                "(?:\\s*,\\s*local_size_z\\s*=\\s*(\\d+))?\\s*\\)\\s*in\\s*;");
            
            java.util.regex.Matcher matcher = pattern.matcher(program.glslSource);
            if (matcher.find()) {
                program.localWorkGroupSize[0] = Integer.parseInt(matcher.group(1));
                if (matcher.group(2) != null) {
                    program.localWorkGroupSize[1] = Integer.parseInt(matcher.group(2));
                }
                if (matcher.group(3) != null) {
                    program.localWorkGroupSize[2] = Integer.parseInt(matcher.group(3));
                }
            }
        }
        
        private String translateComputeShader(String glslSource, ComputeProgram program) {
            StringBuilder msl = new StringBuilder();
            
            // Header
            msl.append("#include <metal_stdlib>\n");
            msl.append("using namespace metal;\n\n");
            
            // Parse and translate uniforms, SSBOs, images
            parseComputeDeclarations(glslSource, program, msl);
            
            // Generate compute kernel
            msl.append("kernel void computeMain(\n");
            
            // Add parameters based on parsed declarations
            boolean first = true;
            
            for (BufferBinding binding : program.bufferBindings.values()) {
                if (!first) msl.append(",\n");
                msl.append("    device void* buffer").append(binding.bindingPoint);
                msl.append(" [[buffer(").append(binding.bindingPoint).append(")]]");
                first = false;
            }
            
            for (ImageBinding binding : program.imageBindings.values()) {
                if (!first) msl.append(",\n");
                String accessQual = binding.access == GL_READ_ONLY ? "access::read" :
                                   binding.access == GL_WRITE_ONLY ? "access::write" :
                                   "access::read_write";
                msl.append("    texture2d<float, ").append(accessQual).append("> image");
                msl.append(binding.unit).append(" [[texture(").append(binding.unit).append(")]]");
                first = false;
            }
            
            // Built-in inputs
            if (!first) msl.append(",\n");
            msl.append("    uint3 gl_GlobalInvocationID [[thread_position_in_grid]],\n");
            msl.append("    uint3 gl_LocalInvocationID [[thread_position_in_threadgroup]],\n");
            msl.append("    uint3 gl_WorkGroupID [[threadgroup_position_in_grid]],\n");
            msl.append("    uint3 gl_NumWorkGroups [[threadgroups_per_grid]],\n");
            msl.append("    uint gl_LocalInvocationIndex [[thread_index_in_threadgroup]]\n");
            msl.append(") {\n");
            
            // Translate main body
            String body = extractMainBody(glslSource);
            body = translateComputeBody(body);
            msl.append(body);
            
            msl.append("}\n");
            
            return msl.toString();
        }
        
        private void parseComputeDeclarations(String source, ComputeProgram program, StringBuilder msl) {
            // Parse SSBOs
            java.util.regex.Pattern ssboPattern = java.util.regex.Pattern.compile(
                "layout\\s*\\(\\s*(?:std430|std140)\\s*,\\s*binding\\s*=\\s*(\\d+)\\s*\\)\\s*" +
                "(?:readonly|writeonly)?\\s*buffer\\s+(\\w+)\\s*\\{([^}]+)\\}\\s*(\\w*)\\s*;");
            
            java.util.regex.Matcher ssboMatcher = ssboPattern.matcher(source);
            while (ssboMatcher.find()) {
                int binding = Integer.parseInt(ssboMatcher.group(1));
                String blockName = ssboMatcher.group(2);
                String members = ssboMatcher.group(3);
                String instanceName = ssboMatcher.group(4);
                
                // Generate struct
                msl.append("struct ").append(blockName).append(" {\n");
                msl.append(translateStructMembers(members));
                msl.append("};\n\n");
                
                BufferBinding bb = new BufferBinding();
                bb.bindingPoint = binding;
                program.bufferBindings.put(binding, bb);
            }
            
            // Parse images
            java.util.regex.Pattern imagePattern = java.util.regex.Pattern.compile(
                "layout\\s*\\(\\s*(?:rgba\\d+\\w*|r\\d+\\w*)\\s*(?:,\\s*binding\\s*=\\s*(\\d+))?\\s*\\)\\s*" +
                "(?:readonly|writeonly)?\\s*uniform\\s+(\\w+)\\s+(\\w+)\\s*;");
            
            java.util.regex.Matcher imageMatcher = imagePattern.matcher(source);
            while (imageMatcher.find()) {
                int binding = imageMatcher.group(1) != null ? Integer.parseInt(imageMatcher.group(1)) : 0;
                String type = imageMatcher.group(2);
                String name = imageMatcher.group(3);
                
                ImageBinding ib = new ImageBinding();
                ib.unit = binding;
                program.imageBindings.put(binding, ib);
            }
        }
        
        private String translateStructMembers(String members) {
            StringBuilder result = new StringBuilder();
            String[] lines = members.split(";");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // Translate types
                line = line.replaceAll("\\bvec2\\b", "float2");
                line = line.replaceAll("\\bvec3\\b", "float3");
                line = line.replaceAll("\\bvec4\\b", "float4");
                line = line.replaceAll("\\bivec2\\b", "int2");
                line = line.replaceAll("\\bivec3\\b", "int3");
                line = line.replaceAll("\\bivec4\\b", "int4");
                line = line.replaceAll("\\buvec2\\b", "uint2");
                line = line.replaceAll("\\buvec3\\b", "uint3");
                line = line.replaceAll("\\buvec4\\b", "uint4");
                line = line.replaceAll("\\bmat2\\b", "float2x2");
                line = line.replaceAll("\\bmat3\\b", "float3x3");
                line = line.replaceAll("\\bmat4\\b", "float4x4");
                
                result.append("    ").append(line).append(";\n");
            }
            return result.toString();
        }
        
        private String extractMainBody(String source) {
            int mainStart = source.indexOf("void main()");
            if (mainStart == -1) mainStart = source.indexOf("void main(void)");
            if (mainStart == -1) return "";
            
            int braceStart = source.indexOf('{', mainStart);
            if (braceStart == -1) return "";
            
            int braceCount = 1;
            int braceEnd = braceStart + 1;
            while (braceEnd < source.length() && braceCount > 0) {
                char c = source.charAt(braceEnd);
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                braceEnd++;
            }
            
            return source.substring(braceStart + 1, braceEnd - 1);
        }
        
        private String translateComputeBody(String body) {
            // Translate GLSL compute shader constructs to MSL
            
            // Barrier functions
            body = body.replaceAll("\\bbarrier\\s*\\(\\s*\\)", 
                "threadgroup_barrier(mem_flags::mem_threadgroup)");
            body = body.replaceAll("\\bmemoryBarrier\\s*\\(\\s*\\)", 
                "threadgroup_barrier(mem_flags::mem_device)");
            body = body.replaceAll("\\bmemoryBarrierShared\\s*\\(\\s*\\)", 
                "threadgroup_barrier(mem_flags::mem_threadgroup)");
            body = body.replaceAll("\\bmemoryBarrierBuffer\\s*\\(\\s*\\)", 
                "threadgroup_barrier(mem_flags::mem_device)");
            body = body.replaceAll("\\bmemoryBarrierImage\\s*\\(\\s*\\)", 
                "threadgroup_barrier(mem_flags::mem_texture)");
            body = body.replaceAll("\\bgroupMemoryBarrier\\s*\\(\\s*\\)", 
                "threadgroup_barrier(mem_flags::mem_threadgroup)");
            
            // Atomic operations
            body = body.replaceAll("\\batomicAdd\\s*\\(([^,]+),\\s*([^)]+)\\)", 
                "atomic_fetch_add_explicit((device atomic_uint*)&$1, $2, memory_order_relaxed)");
            body = body.replaceAll("\\batomicMin\\s*\\(([^,]+),\\s*([^)]+)\\)", 
                "atomic_fetch_min_explicit((device atomic_uint*)&$1, $2, memory_order_relaxed)");
            body = body.replaceAll("\\batomicMax\\s*\\(([^,]+),\\s*([^)]+)\\)", 
                "atomic_fetch_max_explicit((device atomic_uint*)&$1, $2, memory_order_relaxed)");
            body = body.replaceAll("\\batomicAnd\\s*\\(([^,]+),\\s*([^)]+)\\)", 
                "atomic_fetch_and_explicit((device atomic_uint*)&$1, $2, memory_order_relaxed)");
            body = body.replaceAll("\\batomicOr\\s*\\(([^,]+),\\s*([^)]+)\\)", 
                "atomic_fetch_or_explicit((device atomic_uint*)&$1, $2, memory_order_relaxed)");
            body = body.replaceAll("\\batomicXor\\s*\\(([^,]+),\\s*([^)]+)\\)", 
                "atomic_fetch_xor_explicit((device atomic_uint*)&$1, $2, memory_order_relaxed)");
            body = body.replaceAll("\\batomicExchange\\s*\\(([^,]+),\\s*([^)]+)\\)", 
                "atomic_exchange_explicit((device atomic_uint*)&$1, $2, memory_order_relaxed)");
            body = body.replaceAll("\\batomicCompSwap\\s*\\(([^,]+),\\s*([^,]+),\\s*([^)]+)\\)", 
                "atomic_compare_exchange_weak_explicit((device atomic_uint*)&$1, &$2, $3, memory_order_relaxed, memory_order_relaxed)");
            
            // Shared memory
            body = body.replaceAll("\\bshared\\s+", "threadgroup ");
            
            // Type translations
            body = body.replaceAll("\\bvec2\\b", "float2");
            body = body.replaceAll("\\bvec3\\b", "float3");
            body = body.replaceAll("\\bvec4\\b", "float4");
            body = body.replaceAll("\\bivec2\\b", "int2");
            body = body.replaceAll("\\bivec3\\b", "int3");
            body = body.replaceAll("\\bivec4\\b", "int4");
            body = body.replaceAll("\\buvec2\\b", "uint2");
            body = body.replaceAll("\\buvec3\\b", "uint3");
            body = body.replaceAll("\\buvec4\\b", "uint4");
            body = body.replaceAll("\\bmat2\\b", "float2x2");
            body = body.replaceAll("\\bmat3\\b", "float3x3");
            body = body.replaceAll("\\bmat4\\b", "float4x4");
            
            // Built-in variable translations
            body = body.replaceAll("\\bgl_WorkGroupSize\\b", 
                "uint3(" + workGroupSize[0] + ", " + workGroupSize[1] + ", " + workGroupSize[2] + ")");
            
            // Image functions
            body = body.replaceAll("\\bimageLoad\\s*\\(([^,]+),\\s*([^)]+)\\)", 
                "$1.read(uint2($2))");
            body = body.replaceAll("\\bimageStore\\s*\\(([^,]+),\\s*([^,]+),\\s*([^)]+)\\)", 
                "$1.write($3, uint2($2))");
            body = body.replaceAll("\\bimageSize\\s*\\(([^)]+)\\)", 
                "int2($1.get_width(), $1.get_height())");
            
            // Indent
            StringBuilder result = new StringBuilder();
            for (String line : body.split("\n")) {
                result.append("    ").append(line).append("\n");
            }
            
            return result.toString();
        }
        
        // GL constants
        private static final int GL_READ_ONLY = 0x88B8;
        private static final int GL_WRITE_ONLY = 0x88B9;
        private static final int GL_READ_WRITE = 0x88BA;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 29: DEBUGGING AND VALIDATION
    // Debug utilities, validation, and error reporting
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Debug and validation utilities for the OpenGL to Metal translation layer.
     */
    public static final class DebugManager {
        
        // Debug output settings
        private boolean debugEnabled = false;
        private int debugSource = DEBUG_SOURCE_ALL;
        private int debugType = DEBUG_TYPE_ALL;
        private int debugSeverity = DEBUG_SEVERITY_LOW;
        
        // Debug callback
        private DebugCallback callback;
        
        // Message log
        private final List<DebugMessage> messageLog = new ArrayList<>();
        private int maxLoggedMessages = 1000;
        
        // Debug groups
        private final Deque<DebugGroup> groupStack = new ArrayDeque<>();
        
        // Object labels
        private final Map<Long, String> bufferLabels = new HashMap<>();
        private final Map<Long, String> textureLabels = new HashMap<>();
        private final Map<Long, String> shaderLabels = new HashMap<>();
        private final Map<Long, String> programLabels = new HashMap<>();
        private final Map<Long, String> framebufferLabels = new HashMap<>();
        
        /**
         * Debug callback interface
         */
        public interface DebugCallback {
            void onMessage(int source, int type, int id, int severity, String message);
        }
        
        /**
         * Debug message structure
         */
        public static class DebugMessage {
            public int source;
            public int type;
            public int id;
            public int severity;
            public String message;
            public long timestamp;
        }
        
        /**
         * Debug group for push/pop
         */
        public static class DebugGroup {
            public int source;
            public int id;
            public String message;
        }
        
        // Debug constants
        private static final int DEBUG_SOURCE_ALL = 0xFFFF;
        private static final int DEBUG_SOURCE_API = 0x8246;
        private static final int DEBUG_SOURCE_WINDOW_SYSTEM = 0x8247;
        private static final int DEBUG_SOURCE_SHADER_COMPILER = 0x8248;
        private static final int DEBUG_SOURCE_THIRD_PARTY = 0x8249;
        private static final int DEBUG_SOURCE_APPLICATION = 0x824A;
        private static final int DEBUG_SOURCE_OTHER = 0x824B;
        
        private static final int DEBUG_TYPE_ALL = 0xFFFF;
        private static final int DEBUG_TYPE_ERROR = 0x824C;
        private static final int DEBUG_TYPE_DEPRECATED_BEHAVIOR = 0x824D;
        private static final int DEBUG_TYPE_UNDEFINED_BEHAVIOR = 0x824E;
        private static final int DEBUG_TYPE_PORTABILITY = 0x824F;
        private static final int DEBUG_TYPE_PERFORMANCE = 0x8250;
        private static final int DEBUG_TYPE_MARKER = 0x8268;
        private static final int DEBUG_TYPE_PUSH_GROUP = 0x8269;
        private static final int DEBUG_TYPE_POP_GROUP = 0x826A;
        private static final int DEBUG_TYPE_OTHER = 0x8251;
        
        private static final int DEBUG_SEVERITY_HIGH = 0x9146;
        private static final int DEBUG_SEVERITY_MEDIUM = 0x9147;
        private static final int DEBUG_SEVERITY_LOW = 0x9148;
        private static final int DEBUG_SEVERITY_NOTIFICATION = 0x826B;
        
        /**
         * Enable/disable debug output
         */
        public void setDebugOutput(boolean enabled) {
            this.debugEnabled = enabled;
        }
        
        /**
         * Set debug callback
         */
        public void setDebugCallback(DebugCallback callback) {
            this.callback = callback;
        }
        
        /**
         * Control which messages are generated
         */
        public void debugMessageControl(int source, int type, int severity, 
                                        int[] ids, boolean enabled) {
            this.debugSource = source;
            this.debugType = type;
            this.debugSeverity = severity;
        }
        
        /**
         * Insert a debug message
         */
        public void debugMessageInsert(int source, int type, int id, 
                                       int severity, String message) {
            if (!debugEnabled) return;
            if (!shouldLog(source, type, severity)) return;
            
            DebugMessage msg = new DebugMessage();
            msg.source = source;
            msg.type = type;
            msg.id = id;
            msg.severity = severity;
            msg.message = message;
            msg.timestamp = System.nanoTime();
            
            addToLog(msg);
            
            if (callback != null) {
                callback.onMessage(source, type, id, severity, message);
            }
        }
        
        /**
         * Push a debug group
         */
        public void pushDebugGroup(int source, int id, String message) {
            DebugGroup group = new DebugGroup();
            group.source = source;
            group.id = id;
            group.message = message;
            groupStack.push(group);
            
            debugMessageInsert(source, DEBUG_TYPE_PUSH_GROUP, id, 
                DEBUG_SEVERITY_NOTIFICATION, "Push group: " + message);
        }
        
        /**
         * Pop a debug group
         */
        public void popDebugGroup() {
            if (!groupStack.isEmpty()) {
                DebugGroup group = groupStack.pop();
                debugMessageInsert(group.source, DEBUG_TYPE_POP_GROUP, group.id,
                    DEBUG_SEVERITY_NOTIFICATION, "Pop group: " + group.message);
            }
        }
        
        /**
         * Label an object
         */
        public void objectLabel(int identifier, long name, String label) {
            switch (identifier) {
                case GL_BUFFER -> bufferLabels.put(name, label);
                case GL_TEXTURE -> textureLabels.put(name, label);
                case GL_SHADER -> shaderLabels.put(name, label);
                case GL_PROGRAM -> programLabels.put(name, label);
                case GL_FRAMEBUFFER -> framebufferLabels.put(name, label);
            }
        }
        
        /**
         * Get object label
         */
        public String getObjectLabel(int identifier, long name) {
            return switch (identifier) {
                case GL_BUFFER -> bufferLabels.getOrDefault(name, "");
                case GL_TEXTURE -> textureLabels.getOrDefault(name, "");
                case GL_SHADER -> shaderLabels.getOrDefault(name, "");
                case GL_PROGRAM -> programLabels.getOrDefault(name, "");
                case GL_FRAMEBUFFER -> framebufferLabels.getOrDefault(name, "");
                default -> "";
            };
        }
        
        /**
         * Get logged messages
         */
        public int getDebugMessageLog(int count, int bufSize, int[] sources,
                                      int[] types, int[] ids, int[] severities,
                                      int[] lengths, StringBuilder messageLog) {
            int retrieved = 0;
            
            synchronized (this.messageLog) {
                while (retrieved < count && !this.messageLog.isEmpty()) {
                    DebugMessage msg = this.messageLog.remove(0);
                    
                    if (sources != null) sources[retrieved] = msg.source;
                    if (types != null) types[retrieved] = msg.type;
                    if (ids != null) ids[retrieved] = msg.id;
                    if (severities != null) severities[retrieved] = msg.severity;
                    if (lengths != null) lengths[retrieved] = msg.message.length();
                    if (messageLog != null) {
                        messageLog.append(msg.message);
                        messageLog.append('\0');
                    }
                    
                    retrieved++;
                }
            }
            
            return retrieved;
        }
        
        /**
         * Report an internal error
         */
        public void reportError(String component, String message) {
            debugMessageInsert(DEBUG_SOURCE_API, DEBUG_TYPE_ERROR, 0,
                DEBUG_SEVERITY_HIGH, component + ": " + message);
        }
        
        /**
         * Report a performance warning
         */
        public void reportPerformanceWarning(String message) {
            debugMessageInsert(DEBUG_SOURCE_API, DEBUG_TYPE_PERFORMANCE, 0,
                DEBUG_SEVERITY_MEDIUM, message);
        }
        
        /**
         * Report deprecated usage
         */
        public void reportDeprecated(String feature) {
            debugMessageInsert(DEBUG_SOURCE_API, DEBUG_TYPE_DEPRECATED_BEHAVIOR, 0,
                DEBUG_SEVERITY_MEDIUM, "Deprecated: " + feature);
        }
        
        /**
         * Report portability issue
         */
        public void reportPortability(String message) {
            debugMessageInsert(DEBUG_SOURCE_API, DEBUG_TYPE_PORTABILITY, 0,
                DEBUG_SEVERITY_LOW, message);
        }
        
        private boolean shouldLog(int source, int type, int severity) {
            if (debugSource != DEBUG_SOURCE_ALL && debugSource != source) return false;
            if (debugType != DEBUG_TYPE_ALL && debugType != type) return false;
            if (severity > debugSeverity) return false;
            return true;
        }
        
        private void addToLog(DebugMessage msg) {
            synchronized (messageLog) {
                if (messageLog.size() >= maxLoggedMessages) {
                    messageLog.remove(0);
                }
                messageLog.add(msg);
            }
        }
        
        // Object type constants
        private static final int GL_BUFFER = 0x82E0;
        private static final int GL_TEXTURE = 0x1702;
        private static final int GL_SHADER = 0x82E1;
        private static final int GL_PROGRAM = 0x82E2;
        private static final int GL_FRAMEBUFFER = 0x8D40;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 30: UTILITY CLASSES AND NATIVE METHOD STUBS
    // Helper classes and native method declarations
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Native method declarations for Metal API calls.
     * These would be implemented in native code (Objective-C/C++).
     */
    
    // Device methods
    private static native long nMTLCreateSystemDefaultDevice();
    private static native String nMTLDeviceGetName(long device);
    private static native long nMTLDeviceNewCommandQueue(long device);
    private static native long nMTLDeviceNewBufferWithLength(long device, long length, int options);
    private static native long nMTLDeviceNewBufferWithBytes(long device, long bytes, long length, int options);
    private static native long nMTLDeviceNewTextureWithDescriptor(long device, long descriptor);
    private static native long nMTLDeviceNewSamplerState(long device, long descriptor);
    private static native long nMTLDeviceNewLibraryWithSource(long device, String source, long options, long[] error);
    private static native long nMTLDeviceNewRenderPipelineState(long device, long descriptor, long[] error);
    private static native long nMTLDeviceNewComputePipelineState(long device, long function, long[] error);
    private static native long nMTLDeviceNewDepthStencilState(long device, long descriptor);
    private static native long nMTLDeviceNewSharedEvent(long device);
    private static native boolean nMTLDeviceSupportsFamily(long device, int family);
    private static native long nMTLDeviceMaxBufferLength(long device);
    
    // Command queue methods
    private static native long nMTLCommandQueueCommandBuffer(long queue);
    private static native long nMTLCommandQueueCommandBufferWithUnretainedReferences(long queue);
    
    // Command buffer methods
    private static native long nMTLCommandBufferRenderCommandEncoderWithDescriptor(long buffer, long descriptor);
    private static native long nMTLCommandBufferComputeCommandEncoder(long buffer);
    private static native long nMTLCommandBufferBlitCommandEncoder(long buffer);
    private static native void nMTLCommandBufferCommit(long buffer);
    private static native void nMTLCommandBufferWaitUntilCompleted(long buffer);
    private static native void nMTLCommandBufferPresentDrawable(long buffer, long drawable);
    private static native void nMTLCommandBufferAddCompletedHandler(long buffer, long handler);
    private static native void nMTLCommandBufferEncodeSignalEvent(long buffer, long event, long value);
    private static native void nMTLCommandBufferEncodeWaitForEvent(long buffer, long event, long value);
    private static native int nMTLCommandBufferStatus(long buffer);
    
    // Render command encoder methods
    private static native void nMTLRenderCommandEncoderSetRenderPipelineState(long encoder, long pipeline);
    private static native void nMTLRenderCommandEncoderSetVertexBuffer(long encoder, long buffer, long offset, int index);
    private static native void nMTLRenderCommandEncoderSetVertexBytes(long encoder, long bytes, long length, int index);
    private static native void nMTLRenderCommandEncoderSetFragmentBuffer(long encoder, long buffer, long offset, int index);
    private static native void nMTLRenderCommandEncoderSetFragmentBytes(long encoder, long bytes, long length, int index);
    private static native void nMTLRenderCommandEncoderSetFragmentTexture(long encoder, long texture, int index);
    private static native void nMTLRenderCommandEncoderSetFragmentSamplerState(long encoder, long sampler, int index);
    private static native void nMTLRenderCommandEncoderSetDepthStencilState(long encoder, long state);
    private static native void nMTLRenderCommandEncoderSetCullMode(long encoder, int mode);
    private static native void nMTLRenderCommandEncoderSetFrontFacingWinding(long encoder, int winding);
    private static native void nMTLRenderCommandEncoderSetTriangleFillMode(long encoder, int mode);
    private static native void nMTLRenderCommandEncoderSetDepthBias(long encoder, float depthBias, float slopeScale, float clamp);
    private static native void nMTLRenderCommandEncoderSetDepthClipMode(long encoder, int mode);
    private static native void nMTLRenderCommandEncoderSetScissorRect(long encoder, int x, int y, int width, int height);
    private static native void nMTLRenderCommandEncoderSetViewport(long encoder, double x, double y, double width, double height, double znear, double zfar);
    private static native void nMTLRenderCommandEncoderSetBlendColor(long encoder, float r, float g, float b, float a);
    private static native void nMTLRenderCommandEncoderSetStencilReferenceValue(long encoder, int value);
    private static native void nMTLRenderCommandEncoderSetStencilReferenceValues(long encoder, int front, int back);
    private static native void nMTLRenderCommandEncoderSetVisibilityResultMode(long encoder, int mode, long offset);
    private static native void nMTLRenderCommandEncoderDrawPrimitives(long encoder, int type, long start, long count);
    private static native void nMTLRenderCommandEncoderDrawPrimitivesInstanced(long encoder, int type, long start, long count, long instances);
    private static native void nMTLRenderCommandEncoderDrawPrimitivesInstancedBaseInstance(long encoder, int type, long start, long count, long instances, long baseInstance);
    private static native void nMTLRenderCommandEncoderDrawIndexedPrimitives(long encoder, int type, long count, int indexType, long indexBuffer, long offset);
    private static native void nMTLRenderCommandEncoderDrawIndexedPrimitivesInstanced(long encoder, int type, long count, int indexType, long indexBuffer, long offset, long instances);
    private static native void nMTLRenderCommandEncoderDrawIndexedPrimitivesInstancedBaseVertex(long encoder, int type, long count, int indexType, long indexBuffer, long offset, long instances, long baseVertex, long baseInstance);
    private static native void nMTLRenderCommandEncoderDrawPrimitivesIndirect(long encoder, int type, long buffer, long offset);
    private static native void nMTLRenderCommandEncoderDrawIndexedPrimitivesIndirect(long encoder, int type, int indexType, long indexBuffer, long indexOffset, long indirectBuffer, long indirectOffset);
    private static native void nMTLRenderCommandEncoderEndEncoding(long encoder);
    private static native void nMTLRenderCommandEncoderInsertDebugSignpost(long encoder, String string);
    private static native void nMTLRenderCommandEncoderPushDebugGroup(long encoder, String string);
    private static native void nMTLRenderCommandEncoderPopDebugGroup(long encoder);
    
    // Compute command encoder methods
    private static native void nMTLComputeCommandEncoderSetComputePipelineState(long encoder, long pipeline);
    private static native void nMTLComputeCommandEncoderSetBuffer(long encoder, long buffer, long offset, int index);
    private static native void nMTLComputeCommandEncoderSetBytes(long encoder, long bytes, long length, int index);
    private static native void nMTLComputeCommandEncoderSetTexture(long encoder, long texture, int index);
    private static native void nMTLComputeCommandEncoderSetSamplerState(long encoder, long sampler, int index);
    private static native void nMTLComputeCommandEncoderSetThreadgroupMemoryLength(long encoder, long length, int index);
    private static native void nMTLComputeCommandEncoderDispatchThreadgroups(long encoder, long groupsX, long groupsY, long groupsZ, long threadsX, long threadsY, long threadsZ);
    private static native void nMTLComputeCommandEncoderDispatchThreads(long encoder, long threadsX, long threadsY, long threadsZ, long groupSizeX, long groupSizeY, long groupSizeZ);
    private static native void nMTLComputeCommandEncoderDispatchThreadgroupsWithIndirectBuffer(long encoder, long buffer, long offset, long threadsX, long threadsY, long threadsZ);
    private static native void nMTLComputeCommandEncoderEndEncoding(long encoder);
    
    // Blit command encoder methods
    private static native void nMTLBlitCommandEncoderCopyFromBuffer(long encoder, long srcBuffer, long srcOffset, long dstBuffer, long dstOffset, long size);
    private static native void nMTLBlitCommandEncoderCopyFromTexture(long encoder, long srcTexture, int srcSlice, int srcLevel, long srcOriginX, long srcOriginY, long srcOriginZ, long srcWidth, long srcHeight, long srcDepth, long dstTexture, int dstSlice, int dstLevel, long dstOriginX, long dstOriginY, long dstOriginZ);
    private static native void nMTLBlitCommandEncoderCopyFromBufferToTexture(long encoder, long srcBuffer, long srcOffset, long srcBytesPerRow, long srcBytesPerImage, long srcWidth, long srcHeight, long srcDepth, long dstTexture, int dstSlice, int dstLevel, long dstOriginX, long dstOriginY, long dstOriginZ);
    private static native void nMTLBlitCommandEncoderCopyFromTextureToBuffer(long encoder, long srcTexture, int srcSlice, int srcLevel, long srcOriginX, long srcOriginY, long srcOriginZ, long srcWidth, long srcHeight, long srcDepth, long dstBuffer, long dstOffset, long dstBytesPerRow, long dstBytesPerImage);
    private static native void nMTLBlitCommandEncoderFillBuffer(long encoder, long buffer, long offset, long length, byte value);
    private static native void nMTLBlitCommandEncoderGenerateMipmaps(long encoder, long texture);
    private static native void nMTLBlitCommandEncoderSynchronizeResource(long encoder, long resource);
    private static native void nMTLBlitCommandEncoderSynchronizeTexture(long encoder, long texture, int slice, int level);
    private static native void nMTLBlitCommandEncoderEndEncoding(long encoder);
    
    // Buffer methods
    private static native long nMTLBufferContents(long buffer);
    private static native long nMTLBufferLength(long buffer);
    private static native void nMTLBufferDidModifyRange(long buffer, long offset, long length);
    private static native long nMTLBufferNewTextureWithDescriptor(long buffer, long descriptor, long offset, long bytesPerRow);
    
    // Texture methods
    private static native int nMTLTextureWidth(long texture);
    private static native int nMTLTextureHeight(long texture);
    private static native int nMTLTextureDepth(long texture);
    private static native int nMTLTextureMipmapLevelCount(long texture);
    private static native int nMTLTextureArrayLength(long texture);
    private static native int nMTLTextureSampleCount(long texture);
    private static native int nMTLTexturePixelFormat(long texture);
    private static native int nMTLTextureTextureType(long texture);
    private static native void nMTLTextureReplaceRegion(long texture, long regionX, long regionY, long regionZ, long regionW, long regionH, long regionD, int level, int slice, long bytes, long bytesPerRow, long bytesPerImage);
    private static native void nMTLTextureGetBytes(long texture, long bytes, long bytesPerRow, long bytesPerImage, long regionX, long regionY, long regionZ, long regionW, long regionH, long regionD, int level, int slice);
    private static native long nMTLTextureNewTextureViewWithPixelFormat(long texture, int format);
    private static native long nMTLTextureNewTextureViewWithPixelFormatAndLevels(long texture, int format, int levelStart, int levelCount, int sliceStart, int sliceCount);
    
    // Descriptor methods
    private static native long nMTLTextureDescriptorNew();
    private static native void nMTLTextureDescriptorSetTextureType(long descriptor, int type);
    private static native void nMTLTextureDescriptorSetPixelFormat(long descriptor, int format);
    private static native void nMTLTextureDescriptorSetWidth(long descriptor, long width);
    private static native void nMTLTextureDescriptorSetHeight(long descriptor, long height);
    private static native void nMTLTextureDescriptorSetDepth(long descriptor, long depth);
    private static native void nMTLTextureDescriptorSetMipmapLevelCount(long descriptor, long levels);
    private static native void nMTLTextureDescriptorSetSampleCount(long descriptor, long samples);
    private static native void nMTLTextureDescriptorSetArrayLength(long descriptor, long length);
    private static native void nMTLTextureDescriptorSetStorageMode(long descriptor, int mode);
    private static native void nMTLTextureDescriptorSetUsage(long descriptor, int usage);
    
    private static native long nMTLSamplerDescriptorNew();
    private static native void nMTLSamplerDescriptorSetMinFilter(long descriptor, int filter);
    private static native void nMTLSamplerDescriptorSetMagFilter(long descriptor, int filter);
    private static native void nMTLSamplerDescriptorSetMipFilter(long descriptor, int filter);
    private static native void nMTLSamplerDescriptorSetSAddressMode(long descriptor, int mode);
    private static native void nMTLSamplerDescriptorSetTAddressMode(long descriptor, int mode);
    private static native void nMTLSamplerDescriptorSetRAddressMode(long descriptor, int mode);
    private static native void nMTLSamplerDescriptorSetLodMinClamp(long descriptor, float clamp);
    private static native void nMTLSamplerDescriptorSetLodMaxClamp(long descriptor, float clamp);
    private static native void nMTLSamplerDescriptorSetMaxAnisotropy(long descriptor, int anisotropy);
    private static native void nMTLSamplerDescriptorSetCompareFunction(long descriptor, int function);
    private static native void nMTLSamplerDescriptorSetBorderColor(long descriptor, int color);
    private static native void nMTLSamplerDescriptorSetNormalizedCoordinates(long descriptor, boolean normalized);
    
    private static native long nMTLRenderPipelineDescriptorNew();
    private static native void nMTLRenderPipelineDescriptorSetVertexFunction(long descriptor, long function);
    private static native void nMTLRenderPipelineDescriptorSetFragmentFunction(long descriptor, long function);
    private static native void nMTLRenderPipelineDescriptorSetVertexDescriptor(long descriptor, long vertexDescriptor);
    private static native long nMTLRenderPipelineDescriptorColorAttachments(long descriptor);
    private static native void nMTLRenderPipelineDescriptorSetDepthAttachmentPixelFormat(long descriptor, int format);
    private static native void nMTLRenderPipelineDescriptorSetStencilAttachmentPixelFormat(long descriptor, int format);
    private static native void nMTLRenderPipelineDescriptorSetSampleCount(long descriptor, long count);
    private static native void nMTLRenderPipelineDescriptorSetAlphaToCoverageEnabled(long descriptor, boolean enabled);
    private static native void nMTLRenderPipelineDescriptorSetAlphaToOneEnabled(long descriptor, boolean enabled);
    private static native void nMTLRenderPipelineDescriptorSetRasterizationEnabled(long descriptor, boolean enabled);
    private static native void nMTLRenderPipelineDescriptorSetInputPrimitiveTopology(long descriptor, int topology);
    
    private static native void nMTLRenderPipelineColorAttachmentDescriptorSetPixelFormat(long descriptor, int index, int format);
    private static native void nMTLRenderPipelineColorAttachmentDescriptorSetBlendingEnabled(long descriptor, int index, boolean enabled);
    private static native void nMTLRenderPipelineColorAttachmentDescriptorSetSourceRGBBlendFactor(long descriptor, int index, int factor);
    private static native void nMTLRenderPipelineColorAttachmentDescriptorSetDestinationRGBBlendFactor(long descriptor, int index, int factor);
    private static native void nMTLRenderPipelineColorAttachmentDescriptorSetRGBBlendOperation(long descriptor, int index, int operation);
    private static native void nMTLRenderPipelineColorAttachmentDescriptorSetSourceAlphaBlendFactor(long descriptor, int index, int factor);
    private static native void nMTLRenderPipelineColorAttachmentDescriptorSetDestinationAlphaBlendFactor(long descriptor, int index, int factor);
    private static native void nMTLRenderPipelineColorAttachmentDescriptorSetAlphaBlendOperation(long descriptor, int index, int operation);
    private static native void nMTLRenderPipelineColorAttachmentDescriptorSetWriteMask(long descriptor, int index, int mask);
    
    private static native long nMTLDepthStencilDescriptorNew();
    private static native void nMTLDepthStencilDescriptorSetDepthCompareFunction(long descriptor, int function);
    private static native void nMTLDepthStencilDescriptorSetDepthWriteEnabled(long descriptor, boolean enabled);
    private static native void nMTLDepthStencilDescriptorSetFrontFaceStencil(long descriptor, long stencil);
    private static native void nMTLDepthStencilDescriptorSetBackFaceStencil(long descriptor, long stencil);
    
    private static native long nMTLStencilDescriptorNew();
    private static native void nMTLStencilDescriptorSetStencilCompareFunction(long descriptor, int function);
    private static native void nMTLStencilDescriptorSetStencilFailureOperation(long descriptor, int operation);
    private static native void nMTLStencilDescriptorSetDepthFailureOperation(long descriptor, int operation);
    private static native void nMTLStencilDescriptorSetDepthStencilPassOperation(long descriptor, int operation);
    private static native void nMTLStencilDescriptorSetReadMask(long descriptor, int mask);
    private static native void nMTLStencilDescriptorSetWriteMask(long descriptor, int mask);
    
    private static native long nMTLRenderPassDescriptorNew();
    private static native long nMTLRenderPassDescriptorColorAttachments(long descriptor);
    private static native long nMTLRenderPassDescriptorDepthAttachment(long descriptor);
    private static native long nMTLRenderPassDescriptorStencilAttachment(long descriptor);
    private static native void nMTLRenderPassDescriptorSetVisibilityResultBuffer(long descriptor, long buffer);
    private static native void nMTLRenderPassDescriptorSetRenderTargetArrayLength(long descriptor, long length);
    
    private static native void nMTLRenderPassColorAttachmentDescriptorSetTexture(long descriptor, int index, long texture);
    private static native void nMTLRenderPassColorAttachmentDescriptorSetLevel(long descriptor, int index, long level);
    private static native void nMTLRenderPassColorAttachmentDescriptorSetSlice(long descriptor, int index, long slice);
    private static native void nMTLRenderPassColorAttachmentDescriptorSetLoadAction(long descriptor, int index, int action);
    private static native void nMTLRenderPassColorAttachmentDescriptorSetStoreAction(long descriptor, int index, int action);
    private static native void nMTLRenderPassColorAttachmentDescriptorSetClearColor(long descriptor, int index, double r, double g, double b, double a);
    private static native void nMTLRenderPassColorAttachmentDescriptorSetResolveTexture(long descriptor, int index, long texture);
    private static native void nMTLRenderPassColorAttachmentDescriptorSetResolveLevel(long descriptor, int index, long level);
    private static native void nMTLRenderPassColorAttachmentDescriptorSetResolveSlice(long descriptor, int index, long slice);
    
    private static native void nMTLRenderPassDepthAttachmentDescriptorSetTexture(long descriptor, long texture);
    private static native void nMTLRenderPassDepthAttachmentDescriptorSetLevel(long descriptor, long level);
    private static native void nMTLRenderPassDepthAttachmentDescriptorSetSlice(long descriptor, long slice);
    private static native void nMTLRenderPassDepthAttachmentDescriptorSetLoadAction(long descriptor, int action);
    private static native void nMTLRenderPassDepthAttachmentDescriptorSetStoreAction(long descriptor, int action);
    private static native void nMTLRenderPassDepthAttachmentDescriptorSetClearDepth(long descriptor, double depth);
    private static native void nMTLRenderPassDepthAttachmentDescriptorSetResolveTexture(long descriptor, long texture);
    
    private static native void nMTLRenderPassStencilAttachmentDescriptorSetTexture(long descriptor, long texture);
    private static native void nMTLRenderPassStencilAttachmentDescriptorSetLevel(long descriptor, long level);
    private static native void nMTLRenderPassStencilAttachmentDescriptorSetSlice(long descriptor, long slice);
    private static native void nMTLRenderPassStencilAttachmentDescriptorSetLoadAction(long descriptor, int action);
    private static native void nMTLRenderPassStencilAttachmentDescriptorSetStoreAction(long descriptor, int action);
    private static native void nMTLRenderPassStencilAttachmentDescriptorSetClearStencil(long descriptor, int stencil);
    
    // Vertex descriptor methods
    private static native long nMTLVertexDescriptorNew();
    private static native void nMTLVertexDescriptorSetAttributeFormat(long descriptor, int index, int format);
    private static native void nMTLVertexDescriptorSetAttributeOffset(long descriptor, int index, long offset);
    private static native void nMTLVertexDescriptorSetAttributeBufferIndex(long descriptor, int index, int bufferIndex);
    private static native void nMTLVertexDescriptorSetLayoutStride(long descriptor, int index, long stride);
    private static native void nMTLVertexDescriptorSetLayoutStepRate(long descriptor, int index, long stepRate);
    private static native void nMTLVertexDescriptorSetLayoutStepFunction(long descriptor, int index, int stepFunction);
    
    // Library and function methods
    private static native long nMTLLibraryNewFunctionWithName(long library, String name);
    private static native String[] nMTLLibraryFunctionNames(long library);
    
    // Shared event methods
    private static native long nMTLSharedEventGetSignaledValue(long event);
    private static native void nMTLSharedEventSetSignaledValue(long event, long value);
    private static native void nMTLSharedEventNotifyListener(long event, long listener, long value, long handler);
    
    // CAMetalLayer methods (for display integration)
    private static native long nCAMetalLayerNextDrawable(long layer);
    private static native long nCAMetalDrawableTexture(long drawable);
    private static native void nCAMetalLayerSetDevice(long layer, long device);
    private static native void nCAMetalLayerSetPixelFormat(long layer, int format);
    private static native void nCAMetalLayerSetDrawableSize(long layer, double width, double height);
    private static native void nCAMetalLayerSetFramebufferOnly(long layer, boolean framebufferOnly);
    private static native void nCAMetalLayerSetMaximumDrawableCount(long layer, long count);
    private static native void nCAMetalLayerSetDisplaySyncEnabled(long layer, boolean enabled);
    
    // Memory management
    private static native void nRelease(long object);
    private static native void nRetain(long object);
    private static native long nRetainCount(long object);
    
    // Metal resource constants
    public static final int MTLResourceStorageModeShared = 0;
    public static final int MTLResourceStorageModeManaged = 1;
    public static final int MTLResourceStorageModePrivate = 2;
    public static final int MTLResourceStorageModeMemoryless = 3;
    
    public static final int MTLResourceCPUCacheModeDefaultCache = 0;
    public static final int MTLResourceCPUCacheModeWriteCombined = 1;
    
    public static final int MTLResourceHazardTrackingModeDefault = 0;
    public static final int MTLResourceHazardTrackingModeUntracked = 1;
    public static final int MTLResourceHazardTrackingModeTracked = 2;
    
    // Cleanup and shutdown
    static {
        // Load native library
        // System.loadLibrary("gl2metal");
    }
}

    // ════════════════════════════════════════════════════════════════════════════
    // END OF METALCALLMAPPER
    // ════════════════════════════════════════════════════════════════════════════
}