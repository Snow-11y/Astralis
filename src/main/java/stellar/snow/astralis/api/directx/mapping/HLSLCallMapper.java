// ════════════════════════════════════════════════════════════════════════════════
// FILE: HLSLCallMapper.java
// COMPREHENSIVE GLSL → HLSL TRANSLATOR
// PART 1 OF 3: CORE INFRASTRUCTURE, LEXER, TOKEN HANDLING
// ════════════════════════════════════════════════════════════════════════════════

package stellar.snow.astralis.api.directx.mapping;

import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

/**
 * Production-grade GLSL to HLSL/DirectX shader translator.
 * 
 * Features:
 * - Proper tokenizer (not regex) that handles comments, preprocessor, multiline
 * - AST-based parsing for accurate transformation
 * - Matrix column-major → row-major handling
 * - Separated texture/sampler object management
 * - Complete function call remapping
 * - Built-in variable semantic mapping
 * - Resource binding tracking for DirectX
 * 
 * @author PixelVerse Engine
 * @version 2.0
 */
public final class HLSLCallMapper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HLSLCallMapper.class);
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 1: CONFIGURATION & ENUMS
    // ════════════════════════════════════════════════════════════════════════════
    
    /** Target shader model for HLSL output */
    public enum ShaderModel {
        SM_4_0("4_0", 40),   // DX10
        SM_4_1("4_1", 41),
        SM_5_0("5_0", 50),   // DX11
        SM_5_1("5_1", 51),   // DX11.3 / DX12
        SM_6_0("6_0", 60),   // DX12 Wave ops
        SM_6_5("6_5", 65),   // Raytracing, mesh shaders
        SM_6_6("6_6", 66);   // Latest
        
        public final String profile;
        public final int numericVersion;
        
        ShaderModel(String profile, int version) {
            this.profile = profile;
            this.numericVersion = version;
        }
    }
    
    /** Shader stage type */
    public enum ShaderStage {
        VERTEX("vs", "VSMain"),
        FRAGMENT("ps", "PSMain"),
        GEOMETRY("gs", "GSMain"),
        HULL("hs", "HSMain"),           // Tessellation control
        DOMAIN("ds", "DSMain"),          // Tessellation evaluation
        COMPUTE("cs", "CSMain");
        
        public final String profilePrefix;
        public final String entryPoint;
        
        ShaderStage(String prefix, String entry) {
            this.profilePrefix = prefix;
            this.entryPoint = entry;
        }
        
        public String getProfile(ShaderModel sm) {
            return profilePrefix + "_" + sm.profile;
        }
    }
    
    /** How to handle GLSL column-major matrices in HLSL row-major world */
    public enum MatrixConvention {
        /** Use #pragma pack_matrix(column_major) - simplest, keeps GLSL math correct */
        PRAGMA_COLUMN_MAJOR,
        /** Transpose matrices on CPU before upload - requires ConstantBufferManager cooperation */
        CPU_TRANSPOSE,
        /** Generate transpose() calls when loading matrix uniforms in shader */
        SHADER_TRANSPOSE,
        /** Swap multiplication order (M*v becomes v*M) - risky, can break custom math */
        SWAP_MULTIPLY_ORDER
    }
    
    /** Sampler filtering mode for auto-generated sampler states */
    public enum DefaultSamplerMode {
        LINEAR_WRAP,
        LINEAR_CLAMP,
        POINT_WRAP,
        POINT_CLAMP,
        ANISOTROPIC
    }
    
    /** Configuration for the translator */
    public static final class Config {
        public final ShaderStage stage;
        public final ShaderModel shaderModel;
        public final MatrixConvention matrixConvention;
        public final DefaultSamplerMode defaultSampler;
        public final boolean generateDebugComments;
        public final boolean preserveLineNumbers;
        public final boolean enableStrictMode;          // Fail on unknown constructs
        public final int maxTextureSlots;               // DX9=16, DX11=128
        public final int maxSamplerSlots;
        public final int maxCBufferSlots;
        public final int maxUAVSlots;
        
        private Config(Builder b) {
            this.stage = b.stage;
            this.shaderModel = b.shaderModel;
            this.matrixConvention = b.matrixConvention;
            this.defaultSampler = b.defaultSampler;
            this.generateDebugComments = b.generateDebugComments;
            this.preserveLineNumbers = b.preserveLineNumbers;
            this.enableStrictMode = b.enableStrictMode;
            this.maxTextureSlots = b.maxTextureSlots;
            this.maxSamplerSlots = b.maxSamplerSlots;
            this.maxCBufferSlots = b.maxCBufferSlots;
            this.maxUAVSlots = b.maxUAVSlots;
        }
        
        public static Builder builder(ShaderStage stage) { return new Builder(stage); }
        
        public static Config vertex() { return builder(ShaderStage.VERTEX).build(); }
        public static Config fragment() { return builder(ShaderStage.FRAGMENT).build(); }
        public static Config compute() { return builder(ShaderStage.COMPUTE).build(); }
        
        public static final class Builder {
            private final ShaderStage stage;
            private ShaderModel shaderModel = ShaderModel.SM_5_0;
            private MatrixConvention matrixConvention = MatrixConvention.PRAGMA_COLUMN_MAJOR;
            private DefaultSamplerMode defaultSampler = DefaultSamplerMode.LINEAR_WRAP;
            private boolean generateDebugComments = false;
            private boolean preserveLineNumbers = false;
            private boolean enableStrictMode = false;
            private int maxTextureSlots = 128;
            private int maxSamplerSlots = 16;
            private int maxCBufferSlots = 14;
            private int maxUAVSlots = 64;
            
            private Builder(ShaderStage stage) { this.stage = stage; }
            
            public Builder shaderModel(ShaderModel sm) { this.shaderModel = sm; return this; }
            public Builder matrixConvention(MatrixConvention mc) { this.matrixConvention = mc; return this; }
            public Builder defaultSampler(DefaultSamplerMode mode) { this.defaultSampler = mode; return this; }
            public Builder debugComments(boolean b) { this.generateDebugComments = b; return this; }
            public Builder preserveLines(boolean b) { this.preserveLineNumbers = b; return this; }
            public Builder strictMode(boolean b) { this.enableStrictMode = b; return this; }
            public Builder maxTextures(int n) { this.maxTextureSlots = n; return this; }
            public Builder maxSamplers(int n) { this.maxSamplerSlots = n; return this; }
            public Builder maxCBuffers(int n) { this.maxCBufferSlots = n; return this; }
            public Builder maxUAVs(int n) { this.maxUAVSlots = n; return this; }
            
            public Config build() { return new Config(this); }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 2: TOKEN TYPES
    // ════════════════════════════════════════════════════════════════════════════
    
    /** All token types recognized by the lexer */
    public enum TokenType {
        // ─── Literals ───
        IDENTIFIER,
        INT_LITERAL,
        UINT_LITERAL,
        FLOAT_LITERAL,
        DOUBLE_LITERAL,
        BOOL_LITERAL,
        
        // ─── Type Keywords ───
        KW_VOID, KW_BOOL, KW_INT, KW_UINT, KW_FLOAT, KW_DOUBLE,
        KW_VEC2, KW_VEC3, KW_VEC4,
        KW_DVEC2, KW_DVEC3, KW_DVEC4,
        KW_IVEC2, KW_IVEC3, KW_IVEC4,
        KW_UVEC2, KW_UVEC3, KW_UVEC4,
        KW_BVEC2, KW_BVEC3, KW_BVEC4,
        KW_MAT2, KW_MAT3, KW_MAT4,
        KW_MAT2X2, KW_MAT2X3, KW_MAT2X4,
        KW_MAT3X2, KW_MAT3X3, KW_MAT3X4,
        KW_MAT4X2, KW_MAT4X3, KW_MAT4X4,
        KW_DMAT2, KW_DMAT3, KW_DMAT4,
        // Samplers
        KW_SAMPLER1D, KW_SAMPLER2D, KW_SAMPLER3D, KW_SAMPLERCUBE,
        KW_SAMPLER2DRECT, KW_SAMPLER1DARRAY, KW_SAMPLER2DARRAY, KW_SAMPLERCUBEARRAY,
        KW_SAMPLERBUFFER, KW_SAMPLER2DMS, KW_SAMPLER2DMSARRAY,
        KW_SAMPLER1DSHADOW, KW_SAMPLER2DSHADOW, KW_SAMPLERCUBESHADOW,
        KW_SAMPLER2DRECTSHADOW, KW_SAMPLER1DARRAYSHADOW, KW_SAMPLER2DARRAYSHADOW,
        KW_SAMPLERCUBEARRAYSHADOW,
        KW_ISAMPLER1D, KW_ISAMPLER2D, KW_ISAMPLER3D, KW_ISAMPLERCUBE,
        KW_ISAMPLER1DARRAY, KW_ISAMPLER2DARRAY, KW_ISAMPLERCUBEARRAY,
        KW_ISAMPLERBUFFER, KW_ISAMPLER2DMS, KW_ISAMPLER2DMSARRAY, KW_ISAMPLER2DRECT,
        KW_USAMPLER1D, KW_USAMPLER2D, KW_USAMPLER3D, KW_USAMPLERCUBE,
        KW_USAMPLER1DARRAY, KW_USAMPLER2DARRAY, KW_USAMPLERCUBEARRAY,
        KW_USAMPLERBUFFER, KW_USAMPLER2DMS, KW_USAMPLER2DMSARRAY, KW_USAMPLER2DRECT,
        // Images
        KW_IMAGE1D, KW_IMAGE2D, KW_IMAGE3D, KW_IMAGECUBE,
        KW_IMAGE1DARRAY, KW_IMAGE2DARRAY, KW_IMAGECUBEARRAY,
        KW_IMAGEBUFFER, KW_IMAGE2DMS, KW_IMAGE2DMSARRAY, KW_IMAGE2DRECT,
        KW_IIMAGE1D, KW_IIMAGE2D, KW_IIMAGE3D, KW_IIMAGECUBE,
        KW_UIMAGE1D, KW_UIMAGE2D, KW_UIMAGE3D, KW_UIMAGECUBE,
        KW_ATOMIC_UINT,
        
        // ─── Qualifier Keywords ───
        KW_CONST, KW_IN, KW_OUT, KW_INOUT,
        KW_UNIFORM, KW_BUFFER, KW_SHARED,
        KW_ATTRIBUTE, KW_VARYING,
        KW_CENTROID, KW_FLAT, KW_SMOOTH, KW_NOPERSPECTIVE,
        KW_PATCH, KW_SAMPLE,
        KW_HIGHP, KW_MEDIUMP, KW_LOWP, KW_PRECISION,
        KW_INVARIANT, KW_PRECISE,
        KW_COHERENT, KW_VOLATILE, KW_RESTRICT, KW_READONLY, KW_WRITEONLY,
        KW_LAYOUT,
        
        // ─── Control Flow Keywords ───
        KW_IF, KW_ELSE, KW_SWITCH, KW_CASE, KW_DEFAULT,
        KW_FOR, KW_WHILE, KW_DO,
        KW_BREAK, KW_CONTINUE, KW_RETURN, KW_DISCARD,
        
        // ─── Other Keywords ───
        KW_STRUCT, KW_SUBROUTINE, KW_TRUE, KW_FALSE,
        
        // ─── Operators ───
        OP_PLUS, OP_MINUS, OP_STAR, OP_SLASH, OP_PERCENT,
        OP_PLUS_PLUS, OP_MINUS_MINUS,
        OP_EQ, OP_NE, OP_LT, OP_GT, OP_LE, OP_GE,
        OP_AND, OP_OR, OP_XOR, OP_NOT,
        OP_BIT_AND, OP_BIT_OR, OP_BIT_XOR, OP_BIT_NOT,
        OP_LSHIFT, OP_RSHIFT,
        OP_ASSIGN,
        OP_ADD_ASSIGN, OP_SUB_ASSIGN, OP_MUL_ASSIGN, OP_DIV_ASSIGN, OP_MOD_ASSIGN,
        OP_AND_ASSIGN, OP_OR_ASSIGN, OP_XOR_ASSIGN, OP_LSHIFT_ASSIGN, OP_RSHIFT_ASSIGN,
        OP_QUESTION, OP_COLON, OP_DOT, OP_COMMA, OP_SEMICOLON,
        
        // ─── Brackets ───
        LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE,
        
        // ─── Preprocessor ───
        PP_VERSION, PP_EXTENSION, PP_DEFINE, PP_UNDEF,
        PP_IF, PP_IFDEF, PP_IFNDEF, PP_ELSE, PP_ELIF, PP_ENDIF,
        PP_LINE, PP_PRAGMA, PP_ERROR, PP_INCLUDE, PP_UNKNOWN,
        
        // ─── Special ───
        COMMENT_LINE,
        COMMENT_BLOCK,
        WHITESPACE,
        NEWLINE,
        EOF,
        ERROR;
        
        public boolean isType() {
            return ordinal() >= KW_VOID.ordinal() && ordinal() <= KW_ATOMIC_UINT.ordinal();
        }
        
        public boolean isQualifier() {
            return ordinal() >= KW_CONST.ordinal() && ordinal() <= KW_LAYOUT.ordinal();
        }
        
        public boolean isKeyword() {
            return name().startsWith("KW_");
        }
        
        public boolean isPreprocessor() {
            return name().startsWith("PP_");
        }
        
        public boolean isOperator() {
            return name().startsWith("OP_");
        }
        
        public boolean isLiteral() {
            return this == INT_LITERAL || this == UINT_LITERAL || 
                   this == FLOAT_LITERAL || this == DOUBLE_LITERAL || this == BOOL_LITERAL;
        }
        
        public boolean isSampler() {
            return name().contains("SAMPLER");
        }
        
        public boolean isImage() {
            return name().contains("IMAGE");
        }
        
        public boolean isMatrix() {
            return name().contains("MAT");
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 3: TOKEN CLASS
    // ════════════════════════════════════════════════════════════════════════════
    
    /** Immutable token with source location tracking */
    public static final class Token {
        public final TokenType type;
        public final String text;
        public final int line;
        public final int column;
        public final int startOffset;
        public final int endOffset;
        
        public Token(TokenType type, String text, int line, int column, int start, int end) {
            this.type = type;
            this.text = text;
            this.line = line;
            this.column = column;
            this.startOffset = start;
            this.endOffset = end;
        }
        
        public boolean is(TokenType t) { return type == t; }
        public boolean isNot(TokenType t) { return type != t; }
        public boolean isOneOf(TokenType... types) {
            for (TokenType t : types) if (type == t) return true;
            return false;
        }
        
        @Override
        public String toString() {
            return String.format("Token[%s '%s' @%d:%d]", type, 
                text.length() > 20 ? text.substring(0, 17) + "..." : text, line, column);
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 4: KEYWORD MAP
    // ════════════════════════════════════════════════════════════════════════════
    
    private static final Object2ObjectOpenHashMap<String, TokenType> KEYWORDS = new Object2ObjectOpenHashMap<>();
    
    static {
        // Types - Scalars
        KEYWORDS.put("void", TokenType.KW_VOID);
        KEYWORDS.put("bool", TokenType.KW_BOOL);
        KEYWORDS.put("int", TokenType.KW_INT);
        KEYWORDS.put("uint", TokenType.KW_UINT);
        KEYWORDS.put("float", TokenType.KW_FLOAT);
        KEYWORDS.put("double", TokenType.KW_DOUBLE);
        
        // Types - Float vectors
        KEYWORDS.put("vec2", TokenType.KW_VEC2);
        KEYWORDS.put("vec3", TokenType.KW_VEC3);
        KEYWORDS.put("vec4", TokenType.KW_VEC4);
        
        // Types - Double vectors
        KEYWORDS.put("dvec2", TokenType.KW_DVEC2);
        KEYWORDS.put("dvec3", TokenType.KW_DVEC3);
        KEYWORDS.put("dvec4", TokenType.KW_DVEC4);
        
        // Types - Int vectors
        KEYWORDS.put("ivec2", TokenType.KW_IVEC2);
        KEYWORDS.put("ivec3", TokenType.KW_IVEC3);
        KEYWORDS.put("ivec4", TokenType.KW_IVEC4);
        
        // Types - Uint vectors
        KEYWORDS.put("uvec2", TokenType.KW_UVEC2);
        KEYWORDS.put("uvec3", TokenType.KW_UVEC3);
        KEYWORDS.put("uvec4", TokenType.KW_UVEC4);
        
        // Types - Bool vectors
        KEYWORDS.put("bvec2", TokenType.KW_BVEC2);
        KEYWORDS.put("bvec3", TokenType.KW_BVEC3);
        KEYWORDS.put("bvec4", TokenType.KW_BVEC4);
        
        // Types - Float matrices
        KEYWORDS.put("mat2", TokenType.KW_MAT2);
        KEYWORDS.put("mat3", TokenType.KW_MAT3);
        KEYWORDS.put("mat4", TokenType.KW_MAT4);
        KEYWORDS.put("mat2x2", TokenType.KW_MAT2X2);
        KEYWORDS.put("mat2x3", TokenType.KW_MAT2X3);
        KEYWORDS.put("mat2x4", TokenType.KW_MAT2X4);
        KEYWORDS.put("mat3x2", TokenType.KW_MAT3X2);
        KEYWORDS.put("mat3x3", TokenType.KW_MAT3X3);
        KEYWORDS.put("mat3x4", TokenType.KW_MAT3X4);
        KEYWORDS.put("mat4x2", TokenType.KW_MAT4X2);
        KEYWORDS.put("mat4x3", TokenType.KW_MAT4X3);
        KEYWORDS.put("mat4x4", TokenType.KW_MAT4X4);
        
        // Types - Double matrices
        KEYWORDS.put("dmat2", TokenType.KW_DMAT2);
        KEYWORDS.put("dmat3", TokenType.KW_DMAT3);
        KEYWORDS.put("dmat4", TokenType.KW_DMAT4);
        
        // Types - Float samplers
        KEYWORDS.put("sampler1D", TokenType.KW_SAMPLER1D);
        KEYWORDS.put("sampler2D", TokenType.KW_SAMPLER2D);
        KEYWORDS.put("sampler3D", TokenType.KW_SAMPLER3D);
        KEYWORDS.put("samplerCube", TokenType.KW_SAMPLERCUBE);
        KEYWORDS.put("sampler2DRect", TokenType.KW_SAMPLER2DRECT);
        KEYWORDS.put("sampler1DArray", TokenType.KW_SAMPLER1DARRAY);
        KEYWORDS.put("sampler2DArray", TokenType.KW_SAMPLER2DARRAY);
        KEYWORDS.put("samplerCubeArray", TokenType.KW_SAMPLERCUBEARRAY);
        KEYWORDS.put("samplerBuffer", TokenType.KW_SAMPLERBUFFER);
        KEYWORDS.put("sampler2DMS", TokenType.KW_SAMPLER2DMS);
        KEYWORDS.put("sampler2DMSArray", TokenType.KW_SAMPLER2DMSARRAY);
        
        // Types - Shadow samplers
        KEYWORDS.put("sampler1DShadow", TokenType.KW_SAMPLER1DSHADOW);
        KEYWORDS.put("sampler2DShadow", TokenType.KW_SAMPLER2DSHADOW);
        KEYWORDS.put("samplerCubeShadow", TokenType.KW_SAMPLERCUBESHADOW);
        KEYWORDS.put("sampler2DRectShadow", TokenType.KW_SAMPLER2DRECTSHADOW);
        KEYWORDS.put("sampler1DArrayShadow", TokenType.KW_SAMPLER1DARRAYSHADOW);
        KEYWORDS.put("sampler2DArrayShadow", TokenType.KW_SAMPLER2DARRAYSHADOW);
        KEYWORDS.put("samplerCubeArrayShadow", TokenType.KW_SAMPLERCUBEARRAYSHADOW);
        
        // Types - Integer samplers
        KEYWORDS.put("isampler1D", TokenType.KW_ISAMPLER1D);
        KEYWORDS.put("isampler2D", TokenType.KW_ISAMPLER2D);
        KEYWORDS.put("isampler3D", TokenType.KW_ISAMPLER3D);
        KEYWORDS.put("isamplerCube", TokenType.KW_ISAMPLERCUBE);
        KEYWORDS.put("isampler1DArray", TokenType.KW_ISAMPLER1DARRAY);
        KEYWORDS.put("isampler2DArray", TokenType.KW_ISAMPLER2DARRAY);
        KEYWORDS.put("isamplerCubeArray", TokenType.KW_ISAMPLERCUBEARRAY);
        KEYWORDS.put("isamplerBuffer", TokenType.KW_ISAMPLERBUFFER);
        KEYWORDS.put("isampler2DMS", TokenType.KW_ISAMPLER2DMS);
        KEYWORDS.put("isampler2DMSArray", TokenType.KW_ISAMPLER2DMSARRAY);
        KEYWORDS.put("isampler2DRect", TokenType.KW_ISAMPLER2DRECT);
        
        // Types - Unsigned integer samplers
        KEYWORDS.put("usampler1D", TokenType.KW_USAMPLER1D);
        KEYWORDS.put("usampler2D", TokenType.KW_USAMPLER2D);
        KEYWORDS.put("usampler3D", TokenType.KW_USAMPLER3D);
        KEYWORDS.put("usamplerCube", TokenType.KW_USAMPLERCUBE);
        KEYWORDS.put("usampler1DArray", TokenType.KW_USAMPLER1DARRAY);
        KEYWORDS.put("usampler2DArray", TokenType.KW_USAMPLER2DARRAY);
        KEYWORDS.put("usamplerCubeArray", TokenType.KW_USAMPLERCUBEARRAY);
        KEYWORDS.put("usamplerBuffer", TokenType.KW_USAMPLERBUFFER);
        KEYWORDS.put("usampler2DMS", TokenType.KW_USAMPLER2DMS);
        KEYWORDS.put("usampler2DMSArray", TokenType.KW_USAMPLER2DMSARRAY);
        KEYWORDS.put("usampler2DRect", TokenType.KW_USAMPLER2DRECT);
        
        // Types - Images
        KEYWORDS.put("image1D", TokenType.KW_IMAGE1D);
        KEYWORDS.put("image2D", TokenType.KW_IMAGE2D);
        KEYWORDS.put("image3D", TokenType.KW_IMAGE3D);
        KEYWORDS.put("imageCube", TokenType.KW_IMAGECUBE);
        KEYWORDS.put("image1DArray", TokenType.KW_IMAGE1DARRAY);
        KEYWORDS.put("image2DArray", TokenType.KW_IMAGE2DARRAY);
        KEYWORDS.put("imageCubeArray", TokenType.KW_IMAGECUBEARRAY);
        KEYWORDS.put("imageBuffer", TokenType.KW_IMAGEBUFFER);
        KEYWORDS.put("image2DMS", TokenType.KW_IMAGE2DMS);
        KEYWORDS.put("image2DMSArray", TokenType.KW_IMAGE2DMSARRAY);
        KEYWORDS.put("image2DRect", TokenType.KW_IMAGE2DRECT);
        KEYWORDS.put("iimage1D", TokenType.KW_IIMAGE1D);
        KEYWORDS.put("iimage2D", TokenType.KW_IIMAGE2D);
        KEYWORDS.put("iimage3D", TokenType.KW_IIMAGE3D);
        KEYWORDS.put("iimageCube", TokenType.KW_IIMAGECUBE);
        KEYWORDS.put("uimage1D", TokenType.KW_UIMAGE1D);
        KEYWORDS.put("uimage2D", TokenType.KW_UIMAGE2D);
        KEYWORDS.put("uimage3D", TokenType.KW_UIMAGE3D);
        KEYWORDS.put("uimageCube", TokenType.KW_UIMAGECUBE);
        KEYWORDS.put("atomic_uint", TokenType.KW_ATOMIC_UINT);
        
        // Qualifiers
        KEYWORDS.put("const", TokenType.KW_CONST);
        KEYWORDS.put("in", TokenType.KW_IN);
        KEYWORDS.put("out", TokenType.KW_OUT);
        KEYWORDS.put("inout", TokenType.KW_INOUT);
        KEYWORDS.put("uniform", TokenType.KW_UNIFORM);
        KEYWORDS.put("buffer", TokenType.KW_BUFFER);
        KEYWORDS.put("shared", TokenType.KW_SHARED);
        KEYWORDS.put("attribute", TokenType.KW_ATTRIBUTE);
        KEYWORDS.put("varying", TokenType.KW_VARYING);
        KEYWORDS.put("centroid", TokenType.KW_CENTROID);
        KEYWORDS.put("flat", TokenType.KW_FLAT);
        KEYWORDS.put("smooth", TokenType.KW_SMOOTH);
        KEYWORDS.put("noperspective", TokenType.KW_NOPERSPECTIVE);
        KEYWORDS.put("patch", TokenType.KW_PATCH);
        KEYWORDS.put("sample", TokenType.KW_SAMPLE);
        KEYWORDS.put("highp", TokenType.KW_HIGHP);
        KEYWORDS.put("mediump", TokenType.KW_MEDIUMP);
        KEYWORDS.put("lowp", TokenType.KW_LOWP);
        KEYWORDS.put("precision", TokenType.KW_PRECISION);
        KEYWORDS.put("invariant", TokenType.KW_INVARIANT);
        KEYWORDS.put("precise", TokenType.KW_PRECISE);
        KEYWORDS.put("coherent", TokenType.KW_COHERENT);
        KEYWORDS.put("volatile", TokenType.KW_VOLATILE);
        KEYWORDS.put("restrict", TokenType.KW_RESTRICT);
        KEYWORDS.put("readonly", TokenType.KW_READONLY);
        KEYWORDS.put("writeonly", TokenType.KW_WRITEONLY);
        KEYWORDS.put("layout", TokenType.KW_LAYOUT);
        
        // Control flow
        KEYWORDS.put("if", TokenType.KW_IF);
        KEYWORDS.put("else", TokenType.KW_ELSE);
        KEYWORDS.put("switch", TokenType.KW_SWITCH);
        KEYWORDS.put("case", TokenType.KW_CASE);
        KEYWORDS.put("default", TokenType.KW_DEFAULT);
        KEYWORDS.put("for", TokenType.KW_FOR);
        KEYWORDS.put("while", TokenType.KW_WHILE);
        KEYWORDS.put("do", TokenType.KW_DO);
        KEYWORDS.put("break", TokenType.KW_BREAK);
        KEYWORDS.put("continue", TokenType.KW_CONTINUE);
        KEYWORDS.put("return", TokenType.KW_RETURN);
        KEYWORDS.put("discard", TokenType.KW_DISCARD);
        
        // Other
        KEYWORDS.put("struct", TokenType.KW_STRUCT);
        KEYWORDS.put("subroutine", TokenType.KW_SUBROUTINE);
        KEYWORDS.put("true", TokenType.KW_TRUE);
        KEYWORDS.put("false", TokenType.KW_FALSE);
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 5: LEXER
    // ════════════════════════════════════════════════════════════════════════════
    
    /** 
     * Proper lexer that handles:
     * - Single-line and multi-line comments (won't match code inside comments)
     * - Preprocessor directives with line continuations
     * - All numeric literals (hex, octal, float with exponent, suffixes)
     * - Multi-character operators
     * - Proper line/column tracking
     */
    private static final class Lexer {
        private final String source;
        private final int length;
        private int pos = 0;
        private int line = 1;
        private int column = 1;
        private int tokenStart;
        private int tokenLine;
        private int tokenColumn;
        
        private final ObjectArrayList<Token> tokens = new ObjectArrayList<>();
        private final ObjectArrayList<String> errors = new ObjectArrayList<>();
        
        Lexer(String source) {
            this.source = source;
            this.length = source.length();
        }
        
        ObjectArrayList<Token> tokenize(boolean includeTrivia) {
            while (!isAtEnd()) {
                tokenStart = pos;
                tokenLine = line;
                tokenColumn = column;
                scanToken(includeTrivia);
            }
            
            tokens.add(new Token(TokenType.EOF, "", line, column, pos, pos));
            return tokens;
        }
        
        ObjectArrayList<String> getErrors() { return errors; }
        
        private void scanToken(boolean includeTrivia) {
            char c = advance();
            
            switch (c) {
                // Whitespace
                case ' ', '\t', '\r' -> {
                    while (!isAtEnd() && isWhitespace(peek())) advance();
                    if (includeTrivia) emit(TokenType.WHITESPACE);
                }
                
                // Newline
                case '\n' -> {
                    if (includeTrivia) emit(TokenType.NEWLINE);
                }
                
                // Preprocessor
                case '#' -> scanPreprocessor();
                
                // Potential comment or division
                case '/' -> {
                    if (match('/')) {
                        scanLineComment(includeTrivia);
                    } else if (match('*')) {
                        scanBlockComment(includeTrivia);
                    } else if (match('=')) {
                        emit(TokenType.OP_DIV_ASSIGN);
                    } else {
                        emit(TokenType.OP_SLASH);
                    }
                }
                
                // Brackets
                case '(' -> emit(TokenType.LPAREN);
                case ')' -> emit(TokenType.RPAREN);
                case '[' -> emit(TokenType.LBRACKET);
                case ']' -> emit(TokenType.RBRACKET);
                case '{' -> emit(TokenType.LBRACE);
                case '}' -> emit(TokenType.RBRACE);
                
                // Single-char operators
                case ';' -> emit(TokenType.OP_SEMICOLON);
                case ',' -> emit(TokenType.OP_COMMA);
                case '?' -> emit(TokenType.OP_QUESTION);
                case ':' -> emit(TokenType.OP_COLON);
                case '~' -> emit(TokenType.OP_BIT_NOT);
                
                // Multi-char operators
                case '.' -> {
                    if (isDigit(peek())) {
                        scanNumber();
                    } else {
                        emit(TokenType.OP_DOT);
                    }
                }
                
                case '+' -> {
                    if (match('+')) emit(TokenType.OP_PLUS_PLUS);
                    else if (match('=')) emit(TokenType.OP_ADD_ASSIGN);
                    else emit(TokenType.OP_PLUS);
                }
                
                case '-' -> {
                    if (match('-')) emit(TokenType.OP_MINUS_MINUS);
                    else if (match('=')) emit(TokenType.OP_SUB_ASSIGN);
                    else emit(TokenType.OP_MINUS);
                }
                
                case '*' -> {
                    if (match('=')) emit(TokenType.OP_MUL_ASSIGN);
                    else emit(TokenType.OP_STAR);
                }
                
                case '%' -> {
                    if (match('=')) emit(TokenType.OP_MOD_ASSIGN);
                    else emit(TokenType.OP_PERCENT);
                }
                
                case '=' -> {
                    if (match('=')) emit(TokenType.OP_EQ);
                    else emit(TokenType.OP_ASSIGN);
                }
                
                case '!' -> {
                    if (match('=')) emit(TokenType.OP_NE);
                    else emit(TokenType.OP_NOT);
                }
                
                case '<' -> {
                    if (match('<')) {
                        if (match('=')) emit(TokenType.OP_LSHIFT_ASSIGN);
                        else emit(TokenType.OP_LSHIFT);
                    } else if (match('=')) {
                        emit(TokenType.OP_LE);
                    } else {
                        emit(TokenType.OP_LT);
                    }
                }
                
                case '>' -> {
                    if (match('>')) {
                        if (match('=')) emit(TokenType.OP_RSHIFT_ASSIGN);
                        else emit(TokenType.OP_RSHIFT);
                    } else if (match('=')) {
                        emit(TokenType.OP_GE);
                    } else {
                        emit(TokenType.OP_GT);
                    }
                }
                
                case '&' -> {
                    if (match('&')) emit(TokenType.OP_AND);
                    else if (match('=')) emit(TokenType.OP_AND_ASSIGN);
                    else emit(TokenType.OP_BIT_AND);
                }
                
                case '|' -> {
                    if (match('|')) emit(TokenType.OP_OR);
                    else if (match('=')) emit(TokenType.OP_OR_ASSIGN);
                    else emit(TokenType.OP_BIT_OR);
                }
                
                case '^' -> {
                    if (match('^')) emit(TokenType.OP_XOR);
                    else if (match('=')) emit(TokenType.OP_XOR_ASSIGN);
                    else emit(TokenType.OP_BIT_XOR);
                }
                
                default -> {
                    if (isDigit(c)) {
                        scanNumber();
                    } else if (isAlpha(c) || c == '_') {
                        scanIdentifier();
                    } else {
                        error("Unexpected character: '" + c + "' (0x" + Integer.toHexString(c) + ")");
                        emit(TokenType.ERROR);
                    }
                }
            }
        }
        
        private void scanPreprocessor() {
            // Skip whitespace after #
            while (!isAtEnd() && (peek() == ' ' || peek() == '\t')) advance();
            
            // Read directive name
            int nameStart = pos;
            while (!isAtEnd() && isAlpha(peek())) advance();
            String directive = source.substring(nameStart, pos);
            
            // Read rest of line, handling line continuations
            StringBuilder content = new StringBuilder();
            content.append("#").append(directive);
            
            while (!isAtEnd()) {
                char c = peek();
                if (c == '\n') {
                    // Check for line continuation (backslash before newline)
                    if (pos > 0 && source.charAt(pos - 1) == '\\') {
                        content.setLength(content.length() - 1); // Remove backslash
                        advance(); // Skip newline
                        continue;
                    }
                    break;
                }
                content.append(advance());
            }
            
            TokenType type = switch (directive) {
                case "version" -> TokenType.PP_VERSION;
                case "extension" -> TokenType.PP_EXTENSION;
                case "define" -> TokenType.PP_DEFINE;
                case "undef" -> TokenType.PP_UNDEF;
                case "if" -> TokenType.PP_IF;
                case "ifdef" -> TokenType.PP_IFDEF;
                case "ifndef" -> TokenType.PP_IFNDEF;
                case "else" -> TokenType.PP_ELSE;
                case "elif" -> TokenType.PP_ELIF;
                case "endif" -> TokenType.PP_ENDIF;
                case "line" -> TokenType.PP_LINE;
                case "pragma" -> TokenType.PP_PRAGMA;
                case "error" -> TokenType.PP_ERROR;
                case "include" -> TokenType.PP_INCLUDE;
                default -> TokenType.PP_UNKNOWN;
            };
            
            tokens.add(new Token(type, content.toString(), tokenLine, tokenColumn, tokenStart, pos));
        }
        
        private void scanLineComment(boolean includeTrivia) {
            while (!isAtEnd() && peek() != '\n') advance();
            if (includeTrivia) emit(TokenType.COMMENT_LINE);
        }
        
        private void scanBlockComment(boolean includeTrivia) {
            int depth = 1;
            while (!isAtEnd() && depth > 0) {
                char c = advance();
                if (c == '/' && peek() == '*') {
                    advance();
                    depth++;
                } else if (c == '*' && peek() == '/') {
                    advance();
                    depth--;
                }
            }
            if (depth > 0) {
                error("Unterminated block comment");
            }
            if (includeTrivia) emit(TokenType.COMMENT_BLOCK);
        }
        
        private void scanNumber() {
            boolean isFloat = source.charAt(tokenStart) == '.';
            boolean isHex = false;
            boolean isOctal = false;
            boolean hasExponent = false;
            
            // Check for hex/octal prefix
            if (!isFloat && source.charAt(tokenStart) == '0' && !isAtEnd()) {
                char next = Character.toLowerCase(peek());
                if (next == 'x') {
                    isHex = true;
                    advance();
                } else if (isDigit(next)) {
                    isOctal = true;
                }
            }
            
            // Consume main digits
            if (isHex) {
                while (!isAtEnd() && isHexDigit(peek())) advance();
            } else {
                while (!isAtEnd() && isDigit(peek())) advance();
            }
            
            // Decimal point (if not already a float and not hex)
            if (!isHex && !isFloat && peek() == '.' && isDigit(peekNext())) {
                isFloat = true;
                advance(); // consume '.'
                while (!isAtEnd() && isDigit(peek())) advance();
            }
            
            // Exponent
            if (!isHex) {
                char e = peek();
                if (e == 'e' || e == 'E') {
                    hasExponent = true;
                    isFloat = true;
                    advance();
                    if (peek() == '+' || peek() == '-') advance();
                    if (!isDigit(peek())) {
                        error("Invalid exponent in number literal");
                    }
                    while (!isAtEnd() && isDigit(peek())) advance();
                }
            }
            
            // Suffix
            char suffix = Character.toLowerCase(peek());
            boolean isUnsigned = false;
            boolean isDouble = false;
            boolean isLong = false;
            
            if (suffix == 'u') {
                isUnsigned = true;
                advance();
            } else if (suffix == 'f') {
                isFloat = true;
                advance();
            } else if (suffix == 'l') {
                if (isFloat) {
                    isDouble = true;
                } else {
                    isLong = true;
                }
                advance();
                if (Character.toLowerCase(peek()) == 'f') advance(); // lf suffix
            }
            
            TokenType type;
            if (isDouble) {
                type = TokenType.DOUBLE_LITERAL;
            } else if (isFloat) {
                type = TokenType.FLOAT_LITERAL;
            } else if (isUnsigned) {
                type = TokenType.UINT_LITERAL;
            } else {
                type = TokenType.INT_LITERAL;
            }
            
            emit(type);
        }
        
        private void scanIdentifier() {
            while (!isAtEnd() && (isAlphaNumeric(peek()) || peek() == '_')) {
                advance();
            }
            
            String text = source.substring(tokenStart, pos);
            TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENTIFIER);
            
            // true/false are bool literals
            if (type == TokenType.KW_TRUE || type == TokenType.KW_FALSE) {
                type = TokenType.BOOL_LITERAL;
            }
            
            emit(type);
        }
        
        // ─── Helpers ───
        
        private boolean isAtEnd() { return pos >= length; }
        private char peek() { return isAtEnd() ? '\0' : source.charAt(pos); }
        private char peekNext() { return pos + 1 >= length ? '\0' : source.charAt(pos + 1); }
        
        private char advance() {
            char c = source.charAt(pos++);
            if (c == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
            return c;
        }
        
        private boolean match(char expected) {
            if (isAtEnd() || source.charAt(pos) != expected) return false;
            advance();
            return true;
        }
        
        private void emit(TokenType type) {
            String text = source.substring(tokenStart, pos);
            tokens.add(new Token(type, text, tokenLine, tokenColumn, tokenStart, pos));
        }
        
        private void error(String message) {
            errors.add(String.format("[%d:%d] Lexer error: %s", line, column, message));
        }
        
        private static boolean isWhitespace(char c) { return c == ' ' || c == '\t' || c == '\r'; }
        private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
        private static boolean isHexDigit(char c) { return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'); }
        private static boolean isAlpha(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }
        private static boolean isAlphaNumeric(char c) { return isAlpha(c) || isDigit(c); }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 6: TYPE SYSTEM
    // ════════════════════════════════════════════════════════════════════════════
    
    /** Complete type information for translation */
    public static final class TypeInfo {
        public final String glslName;
        public final String hlslName;
        public final Category category;
        public final String elementType;  // For samplers: the texel type
        public final int rows;
        public final int cols;
        public final int components;      // For vectors
        
        public enum Category {
            SCALAR, VECTOR, MATRIX, SAMPLER, IMAGE, STRUCT, VOID
        }
        
        private TypeInfo(String glsl, String hlsl, Category cat, String elem, int rows, int cols, int comp) {
            this.glslName = glsl;
            this.hlslName = hlsl;
            this.category = cat;
            this.elementType = elem;
            this.rows = rows;
            this.cols = cols;
            this.components = comp;
        }
        
        public boolean isMatrix() { return category == Category.MATRIX; }
        public boolean isSampler() { return category == Category.SAMPLER; }
        public boolean isImage() { return category == Category.IMAGE; }
        public boolean isVector() { return category == Category.VECTOR; }
        public boolean isScalar() { return category == Category.SCALAR; }
        public boolean needsSeparatedSampler() { return isSampler() || isImage(); }
        
        // Factory methods
        public static TypeInfo scalar(String glsl, String hlsl) {
            return new TypeInfo(glsl, hlsl, Category.SCALAR, null, 1, 1, 1);
        }
        public static TypeInfo vector(String glsl, String hlsl, int comp) {
            return new TypeInfo(glsl, hlsl, Category.VECTOR, null, 1, comp, comp);
        }
        public static TypeInfo matrix(String glsl, String hlsl, int rows, int cols) {
            return new TypeInfo(glsl, hlsl, Category.MATRIX, null, rows, cols, rows * cols);
        }
        public static TypeInfo sampler(String glsl, String hlsl, String texelType) {
            return new TypeInfo(glsl, hlsl, Category.SAMPLER, texelType, 0, 0, 0);
        }
        public static TypeInfo image(String glsl, String hlsl, String texelType) {
            return new TypeInfo(glsl, hlsl, Category.IMAGE, texelType, 0, 0, 0);
        }
        public static TypeInfo struct(String name) {
            return new TypeInfo(name, name, Category.STRUCT, null, 0, 0, 0);
        }
        public static TypeInfo voidType() {
            return new TypeInfo("void", "void", Category.VOID, null, 0, 0, 0);
        }
    }
    
    /** Complete type mapping table */
    private static final Object2ObjectOpenHashMap<String, TypeInfo> TYPE_MAP = new Object2ObjectOpenHashMap<>();
    
    static {
        // Scalars
        TYPE_MAP.put("void", TypeInfo.voidType());
        TYPE_MAP.put("bool", TypeInfo.scalar("bool", "bool"));
        TYPE_MAP.put("int", TypeInfo.scalar("int", "int"));
        TYPE_MAP.put("uint", TypeInfo.scalar("uint", "uint"));
        TYPE_MAP.put("float", TypeInfo.scalar("float", "float"));
        TYPE_MAP.put("double", TypeInfo.scalar("double", "double"));
        
        // Float vectors
        TYPE_MAP.put("vec2", TypeInfo.vector("vec2", "float2", 2));
        TYPE_MAP.put("vec3", TypeInfo.vector("vec3", "float3", 3));
        TYPE_MAP.put("vec4", TypeInfo.vector("vec4", "float4", 4));
        
        // Double vectors
        TYPE_MAP.put("dvec2", TypeInfo.vector("dvec2", "double2", 2));
        TYPE_MAP.put("dvec3", TypeInfo.vector("dvec3", "double3", 3));
        TYPE_MAP.put("dvec4", TypeInfo.vector("dvec4", "double4", 4));
        
        // Int vectors
        TYPE_MAP.put("ivec2", TypeInfo.vector("ivec2", "int2", 2));
        TYPE_MAP.put("ivec3", TypeInfo.vector("ivec3", "int3", 3));
        TYPE_MAP.put("ivec4", TypeInfo.vector("ivec4", "int4", 4));
        
        // Uint vectors
        TYPE_MAP.put("uvec2", TypeInfo.vector("uvec2", "uint2", 2));
        TYPE_MAP.put("uvec3", TypeInfo.vector("uvec3", "uint3", 3));
        TYPE_MAP.put("uvec4", TypeInfo.vector("uvec4", "uint4", 4));
        
        // Bool vectors
        TYPE_MAP.put("bvec2", TypeInfo.vector("bvec2", "bool2", 2));
        TYPE_MAP.put("bvec3", TypeInfo.vector("bvec3", "bool3", 3));
        TYPE_MAP.put("bvec4", TypeInfo.vector("bvec4", "bool4", 4));
        
        // Float matrices - Note GLSL matCxR = C columns, R rows
        // HLSL floatRxC = R rows, C columns - so mat3x4 (3 cols, 4 rows) → float4x3
        TYPE_MAP.put("mat2", TypeInfo.matrix("mat2", "float2x2", 2, 2));
        TYPE_MAP.put("mat3", TypeInfo.matrix("mat3", "float3x3", 3, 3));
        TYPE_MAP.put("mat4", TypeInfo.matrix("mat4", "float4x4", 4, 4));
        TYPE_MAP.put("mat2x2", TypeInfo.matrix("mat2x2", "float2x2", 2, 2));
        TYPE_MAP.put("mat2x3", TypeInfo.matrix("mat2x3", "float3x2", 2, 3)); // 2 cols, 3 rows → 3 rows, 2 cols
        TYPE_MAP.put("mat2x4", TypeInfo.matrix("mat2x4", "float4x2", 2, 4));
        TYPE_MAP.put("mat3x2", TypeInfo.matrix("mat3x2", "float2x3", 3, 2));
        TYPE_MAP.put("mat3x3", TypeInfo.matrix("mat3x3", "float3x3", 3, 3));
        TYPE_MAP.put("mat3x4", TypeInfo.matrix("mat3x4", "float4x3", 3, 4));
        TYPE_MAP.put("mat4x2", TypeInfo.matrix("mat4x2", "float2x4", 4, 2));
        TYPE_MAP.put("mat4x3", TypeInfo.matrix("mat4x3", "float3x4", 4, 3));
        TYPE_MAP.put("mat4x4", TypeInfo.matrix("mat4x4", "float4x4", 4, 4));
        
        // Double matrices
        TYPE_MAP.put("dmat2", TypeInfo.matrix("dmat2", "double2x2", 2, 2));
        TYPE_MAP.put("dmat3", TypeInfo.matrix("dmat3", "double3x3", 3, 3));
        TYPE_MAP.put("dmat4", TypeInfo.matrix("dmat4", "double4x4", 4, 4));
        
        // Float samplers → Texture + Sampler pair
        TYPE_MAP.put("sampler1D", TypeInfo.sampler("sampler1D", "Texture1D<float4>", "float4"));
        TYPE_MAP.put("sampler2D", TypeInfo.sampler("sampler2D", "Texture2D<float4>", "float4"));
        TYPE_MAP.put("sampler3D", TypeInfo.sampler("sampler3D", "Texture3D<float4>", "float4"));
        TYPE_MAP.put("samplerCube", TypeInfo.sampler("samplerCube", "TextureCube<float4>", "float4"));
        TYPE_MAP.put("sampler2DRect", TypeInfo.sampler("sampler2DRect", "Texture2D<float4>", "float4"));
        TYPE_MAP.put("sampler1DArray", TypeInfo.sampler("sampler1DArray", "Texture1DArray<float4>", "float4"));
        TYPE_MAP.put("sampler2DArray", TypeInfo.sampler("sampler2DArray", "Texture2DArray<float4>", "float4"));
        TYPE_MAP.put("samplerCubeArray", TypeInfo.sampler("samplerCubeArray", "TextureCubeArray<float4>", "float4"));
        TYPE_MAP.put("samplerBuffer", TypeInfo.sampler("samplerBuffer", "Buffer<float4>", "float4"));
        TYPE_MAP.put("sampler2DMS", TypeInfo.sampler("sampler2DMS", "Texture2DMS<float4>", "float4"));
        TYPE_MAP.put("sampler2DMSArray", TypeInfo.sampler("sampler2DMSArray", "Texture2DMSArray<float4>", "float4"));
        
        // Shadow samplers (comparison samplers)
        TYPE_MAP.put("sampler1DShadow", TypeInfo.sampler("sampler1DShadow", "Texture1D<float>", "float"));
        TYPE_MAP.put("sampler2DShadow", TypeInfo.sampler("sampler2DShadow", "Texture2D<float>", "float"));
        TYPE_MAP.put("samplerCubeShadow", TypeInfo.sampler("samplerCubeShadow", "TextureCube<float>", "float"));
        TYPE_MAP.put("sampler2DRectShadow", TypeInfo.sampler("sampler2DRectShadow", "Texture2D<float>", "float"));
        TYPE_MAP.put("sampler1DArrayShadow", TypeInfo.sampler("sampler1DArrayShadow", "Texture1DArray<float>", "float"));
        TYPE_MAP.put("sampler2DArrayShadow", TypeInfo.sampler("sampler2DArrayShadow", "Texture2DArray<float>", "float"));
        TYPE_MAP.put("samplerCubeArrayShadow", TypeInfo.sampler("samplerCubeArrayShadow", "TextureCubeArray<float>", "float"));
        
        // Integer samplers
        TYPE_MAP.put("isampler1D", TypeInfo.sampler("isampler1D", "Texture1D<int4>", "int4"));
        TYPE_MAP.put("isampler2D", TypeInfo.sampler("isampler2D", "Texture2D<int4>", "int4"));
        TYPE_MAP.put("isampler3D", TypeInfo.sampler("isampler3D", "Texture3D<int4>", "int4"));
        TYPE_MAP.put("isamplerCube", TypeInfo.sampler("isamplerCube", "TextureCube<int4>", "int4"));
        TYPE_MAP.put("isampler1DArray", TypeInfo.sampler("isampler1DArray", "Texture1DArray<int4>", "int4"));
        TYPE_MAP.put("isampler2DArray", TypeInfo.sampler("isampler2DArray", "Texture2DArray<int4>", "int4"));
        TYPE_MAP.put("isamplerCubeArray", TypeInfo.sampler("isamplerCubeArray", "TextureCubeArray<int4>", "int4"));
        TYPE_MAP.put("isamplerBuffer", TypeInfo.sampler("isamplerBuffer", "Buffer<int4>", "int4"));
        TYPE_MAP.put("isampler2DMS", TypeInfo.sampler("isampler2DMS", "Texture2DMS<int4>", "int4"));
        TYPE_MAP.put("isampler2DMSArray", TypeInfo.sampler("isampler2DMSArray", "Texture2DMSArray<int4>", "int4"));
        TYPE_MAP.put("isampler2DRect", TypeInfo.sampler("isampler2DRect", "Texture2D<int4>", "int4"));
        
        // Unsigned integer samplers
        TYPE_MAP.put("usampler1D", TypeInfo.sampler("usampler1D", "Texture1D<uint4>", "uint4"));
        TYPE_MAP.put("usampler2D", TypeInfo.sampler("usampler2D", "Texture2D<uint4>", "uint4"));
        TYPE_MAP.put("usampler3D", TypeInfo.sampler("usampler3D", "Texture3D<uint4>", "uint4"));
        TYPE_MAP.put("usamplerCube", TypeInfo.sampler("usamplerCube", "TextureCube<uint4>", "uint4"));
        TYPE_MAP.put("usampler1DArray", TypeInfo.sampler("usampler1DArray", "Texture1DArray<uint4>", "uint4"));
        TYPE_MAP.put("usampler2DArray", TypeInfo.sampler("usampler2DArray", "Texture2DArray<uint4>", "uint4"));
        TYPE_MAP.put("usamplerCubeArray", TypeInfo.sampler("usamplerCubeArray", "TextureCubeArray<uint4>", "uint4"));
        TYPE_MAP.put("usamplerBuffer", TypeInfo.sampler("usamplerBuffer", "Buffer<uint4>", "uint4"));
        TYPE_MAP.put("usampler2DMS", TypeInfo.sampler("usampler2DMS", "Texture2DMS<uint4>", "uint4"));
        TYPE_MAP.put("usampler2DMSArray", TypeInfo.sampler("usampler2DMSArray", "Texture2DMSArray<uint4>", "uint4"));
        TYPE_MAP.put("usampler2DRect", TypeInfo.sampler("usampler2DRect", "Texture2D<uint4>", "uint4"));
        
        // Images → RWTexture (UAV)
        TYPE_MAP.put("image1D", TypeInfo.image("image1D", "RWTexture1D<float4>", "float4"));
        TYPE_MAP.put("image2D", TypeInfo.image("image2D", "RWTexture2D<float4>", "float4"));
        TYPE_MAP.put("image3D", TypeInfo.image("image3D", "RWTexture3D<float4>", "float4"));
        TYPE_MAP.put("imageCube", TypeInfo.image("imageCube", "RWTextureCube<float4>", "float4"));
        TYPE_MAP.put("image1DArray", TypeInfo.image("image1DArray", "RWTexture1DArray<float4>", "float4"));
        TYPE_MAP.put("image2DArray", TypeInfo.image("image2DArray", "RWTexture2DArray<float4>", "float4"));
        TYPE_MAP.put("imageCubeArray", TypeInfo.image("imageCubeArray", "RWTextureCubeArray<float4>", "float4"));
        TYPE_MAP.put("imageBuffer", TypeInfo.image("imageBuffer", "RWBuffer<float4>", "float4"));
        TYPE_MAP.put("image2DMS", TypeInfo.image("image2DMS", "RWTexture2DMS<float4>", "float4"));
        TYPE_MAP.put("image2DMSArray", TypeInfo.image("image2DMSArray", "RWTexture2DMSArray<float4>", "float4"));
        TYPE_MAP.put("image2DRect", TypeInfo.image("image2DRect", "RWTexture2D<float4>", "float4"));
        
        // Integer images
        TYPE_MAP.put("iimage1D", TypeInfo.image("iimage1D", "RWTexture1D<int4>", "int4"));
        TYPE_MAP.put("iimage2D", TypeInfo.image("iimage2D", "RWTexture2D<int4>", "int4"));
        TYPE_MAP.put("iimage3D", TypeInfo.image("iimage3D", "RWTexture3D<int4>", "int4"));
        TYPE_MAP.put("iimageCube", TypeInfo.image("iimageCube", "RWTextureCube<int4>", "int4"));
        
        // Unsigned integer images
        TYPE_MAP.put("uimage1D", TypeInfo.image("uimage1D", "RWTexture1D<uint4>", "uint4"));
        TYPE_MAP.put("uimage2D", TypeInfo.image("uimage2D", "RWTexture2D<uint4>", "uint4"));
        TYPE_MAP.put("uimage3D", TypeInfo.image("uimage3D", "RWTexture3D<uint4>", "uint4"));
        TYPE_MAP.put("uimageCube", TypeInfo.image("uimageCube", "RWTextureCube<uint4>", "uint4"));
        
        // Atomic counter
        TYPE_MAP.put("atomic_uint", TypeInfo.scalar("atomic_uint", "uint")); // Handled specially
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // END OF PART 1
    // CONTINUED IN PART 2: AST NODES, PARSER, FUNCTION MAPPINGS
    // ════════════════════════════════════════════════════════════════════════════

    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 9 CONTINUED: COMPLEX FUNCTION MAPPINGS
    // ════════════════════════════════════════════════════════════════════════════
        
        // Image load/store functions
        COMPLEX_FUNC_MAP.put("imageLoad", HLSLCallMapper::transformImageLoad);
        COMPLEX_FUNC_MAP.put("imageStore", HLSLCallMapper::transformImageStore);
        COMPLEX_FUNC_MAP.put("imageSize", HLSLCallMapper::transformImageSize);
        COMPLEX_FUNC_MAP.put("imageSamples", HLSLCallMapper::transformImageSamples);
        COMPLEX_FUNC_MAP.put("imageAtomicAdd", HLSLCallMapper::transformImageAtomic);
        COMPLEX_FUNC_MAP.put("imageAtomicMin", HLSLCallMapper::transformImageAtomic);
        COMPLEX_FUNC_MAP.put("imageAtomicMax", HLSLCallMapper::transformImageAtomic);
        COMPLEX_FUNC_MAP.put("imageAtomicAnd", HLSLCallMapper::transformImageAtomic);
        COMPLEX_FUNC_MAP.put("imageAtomicOr", HLSLCallMapper::transformImageAtomic);
        COMPLEX_FUNC_MAP.put("imageAtomicXor", HLSLCallMapper::transformImageAtomic);
        COMPLEX_FUNC_MAP.put("imageAtomicExchange", HLSLCallMapper::transformImageAtomic);
        COMPLEX_FUNC_MAP.put("imageAtomicCompSwap", HLSLCallMapper::transformImageAtomicCompSwap);
        
        // Matrix functions
        COMPLEX_FUNC_MAP.put("matrixCompMult", (name, args, ctx) -> {
            // Component-wise multiply - HLSL has no direct equivalent
            return "(" + args.get(0) + " * " + args.get(1) + ")";
        });
        
        COMPLEX_FUNC_MAP.put("outerProduct", (name, args, ctx) -> {
            // outerProduct(c, r) = column * row^T
            return "_glsl_outerProduct(" + args.get(0) + ", " + args.get(1) + ")";
        });
        
        COMPLEX_FUNC_MAP.put("transpose", (name, args, ctx) -> "transpose(" + args.get(0) + ")");
        COMPLEX_FUNC_MAP.put("determinant", (name, args, ctx) -> "determinant(" + args.get(0) + ")");
        COMPLEX_FUNC_MAP.put("inverse", (name, args, ctx) -> "_glsl_inverse(" + args.get(0) + ")");
        
        // Vector relational (component-wise comparison returning bool vector)
        COMPLEX_FUNC_MAP.put("lessThan", (name, args, ctx) -> 
            "(" + args.get(0) + " < " + args.get(1) + ")");
        COMPLEX_FUNC_MAP.put("lessThanEqual", (name, args, ctx) -> 
            "(" + args.get(0) + " <= " + args.get(1) + ")");
        COMPLEX_FUNC_MAP.put("greaterThan", (name, args, ctx) -> 
            "(" + args.get(0) + " > " + args.get(1) + ")");
        COMPLEX_FUNC_MAP.put("greaterThanEqual", (name, args, ctx) -> 
            "(" + args.get(0) + " >= " + args.get(1) + ")");
        COMPLEX_FUNC_MAP.put("equal", (name, args, ctx) -> 
            "(" + args.get(0) + " == " + args.get(1) + ")");
        COMPLEX_FUNC_MAP.put("notEqual", (name, args, ctx) -> 
            "(" + args.get(0) + " != " + args.get(1) + ")");
        
        COMPLEX_FUNC_MAP.put("any", (name, args, ctx) -> "any(" + args.get(0) + ")");
        COMPLEX_FUNC_MAP.put("all", (name, args, ctx) -> "all(" + args.get(0) + ")");
        COMPLEX_FUNC_MAP.put("not", (name, args, ctx) -> "(!" + args.get(0) + ")");
        
        // Noise functions (deprecated in GLSL 4.4+, need emulation)
        COMPLEX_FUNC_MAP.put("noise1", (name, args, ctx) -> "_glsl_noise1(" + args.get(0) + ")");
        COMPLEX_FUNC_MAP.put("noise2", (name, args, ctx) -> "_glsl_noise2(" + args.get(0) + ")");
        COMPLEX_FUNC_MAP.put("noise3", (name, args, ctx) -> "_glsl_noise3(" + args.get(0) + ")");
        COMPLEX_FUNC_MAP.put("noise4", (name, args, ctx) -> "_glsl_noise4(" + args.get(0) + ")");
        
        // Geometry shader functions
        COMPLEX_FUNC_MAP.put("EmitVertex", (name, args, ctx) -> {
            ctx.requiresStreamOutput = true;
            return "outputStream.Append(output)";
        });
        COMPLEX_FUNC_MAP.put("EndPrimitive", (name, args, ctx) -> {
            ctx.requiresStreamOutput = true;
            return "outputStream.RestartStrip()";
        });
        
        // Tessellation functions
        COMPLEX_FUNC_MAP.put("EmitStreamVertex", (name, args, ctx) -> 
            "stream" + args.get(0) + ".Append(output)");
        COMPLEX_FUNC_MAP.put("EndStreamPrimitive", (name, args, ctx) -> 
            "stream" + args.get(0) + ".RestartStrip()");
        
        // interpolateAt* functions
        COMPLEX_FUNC_MAP.put("interpolateAtCentroid", (name, args, ctx) -> 
            "EvaluateAttributeAtCentroid(" + args.get(0) + ")");
        COMPLEX_FUNC_MAP.put("interpolateAtSample", (name, args, ctx) -> 
            "EvaluateAttributeAtSample(" + args.get(0) + ", " + args.get(1) + ")");
        COMPLEX_FUNC_MAP.put("interpolateAtOffset", (name, args, ctx) -> 
            "EvaluateAttributeSnapped(" + args.get(0) + ", int2(" + args.get(1) + " * 16.0))");
        
        // Floating point functions
        COMPLEX_FUNC_MAP.put("frexp", (name, args, ctx) -> {
            // GLSL: frexp(x, out exp) returns mantissa
            // HLSL: frexp(x, mantissa, exponent) is different
            return "_glsl_frexp(" + args.get(0) + ", " + args.get(1) + ")";
        });
        
        COMPLEX_FUNC_MAP.put("ldexp", (name, args, ctx) -> 
            "ldexp(" + args.get(0) + ", " + args.get(1) + ")");
        
        COMPLEX_FUNC_MAP.put("uintBitsToFloat", (name, args, ctx) -> 
            "asfloat(" + args.get(0) + ")");
        COMPLEX_FUNC_MAP.put("intBitsToFloat", (name, args, ctx) -> 
            "asfloat(" + args.get(0) + ")");
        COMPLEX_FUNC_MAP.put("floatBitsToUint", (name, args, ctx) -> 
            "asuint(" + args.get(0) + ")");
        COMPLEX_FUNC_MAP.put("floatBitsToInt", (name, args, ctx) -> 
            "asint(" + args.get(0) + ")");
        
        // Double precision
        COMPLEX_FUNC_MAP.put("packDouble2x32", (name, args, ctx) -> 
            "asdouble(" + args.get(0) + ".x, " + args.get(0) + ".y)");
        COMPLEX_FUNC_MAP.put("unpackDouble2x32", (name, args, ctx) -> {
            ctx.needsUnpackDouble = true;
            return "_glsl_unpackDouble2x32(" + args.get(0) + ")";
        });
        
        // fma - fused multiply-add
        COMPLEX_FUNC_MAP.put("fma", (name, args, ctx) -> 
            "mad(" + args.get(0) + ", " + args.get(1) + ", " + args.get(2) + ")");
    }
    
    // ─── Texture Transformation Helpers ───
    
    private static String transformTexture(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.isEmpty()) return name + "()";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getSamplerStateForSampler(sampler);
        
        if (args.size() == 2) {
            // texture(sampler, coord)
            return texObj + ".Sample(" + samplerObj + ", " + args.get(1) + ")";
        } else if (args.size() == 3) {
            // texture(sampler, coord, bias)
            return texObj + ".SampleBias(" + samplerObj + ", " + args.get(1) + ", " + args.get(2) + ")";
        }
        return texObj + ".Sample(" + samplerObj + ", " + String.join(", ", args.subList(1, args.size())) + ")";
    }
    
    private static String transformTextureLod(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 3) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getSamplerStateForSampler(sampler);
        return texObj + ".SampleLevel(" + samplerObj + ", " + args.get(1) + ", " + args.get(2) + ")";
    }
    
    private static String transformTextureLodOffset(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 4) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getSamplerStateForSampler(sampler);
        return texObj + ".SampleLevel(" + samplerObj + ", " + args.get(1) + ", " + args.get(2) + ", " + args.get(3) + ")";
    }
    
    private static String transformTextureGrad(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 4) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getSamplerStateForSampler(sampler);
        return texObj + ".SampleGrad(" + samplerObj + ", " + args.get(1) + ", " + args.get(2) + ", " + args.get(3) + ")";
    }
    
    private static String transformTextureGradOffset(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 5) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getSamplerStateForSampler(sampler);
        return texObj + ".SampleGrad(" + samplerObj + ", " + args.get(1) + ", " + args.get(2) + ", " + args.get(3) + ", " + args.get(4) + ")";
    }
    
    private static String transformTextureProj(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 2) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getSamplerStateForSampler(sampler);
        String coord = args.get(1);
        // textureProj divides by last component
        return texObj + ".Sample(" + samplerObj + ", " + coord + ".xy / " + coord + ".w)";
    }
    
    private static String transformTextureProjLod(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 3) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getSamplerStateForSampler(sampler);
        String coord = args.get(1);
        return texObj + ".SampleLevel(" + samplerObj + ", " + coord + ".xy / " + coord + ".w, " + args.get(2) + ")";
    }
    
    private static String transformTextureOffset(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 3) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getSamplerStateForSampler(sampler);
        if (args.size() == 3) {
            return texObj + ".Sample(" + samplerObj + ", " + args.get(1) + ", " + args.get(2) + ")";
        } else {
            return texObj + ".SampleBias(" + samplerObj + ", " + args.get(1) + ", " + args.get(3) + ", " + args.get(2) + ")";
        }
    }
    
    private static String transformTexelFetch(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 2) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String coord = args.get(1);
        
        if (args.size() >= 3) {
            // texelFetch(sampler, coord, lod)
            return texObj + ".Load(int3(" + coord + ", " + args.get(2) + "))";
        }
        return texObj + ".Load(int3(" + coord + ", 0))";
    }
    
    private static String transformTexelFetchOffset(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 4) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        return texObj + ".Load(int3(" + args.get(1) + ", " + args.get(2) + "), " + args.get(3) + ")";
    }
    
    private static String transformTextureSize(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.isEmpty()) return "int2(0, 0)";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String lod = args.size() > 1 ? args.get(1) : "0";
        
        // GetDimensions has out parameters - need helper
        ctx.needsTextureSizeHelper = true;
        TypeInfo samplerType = ctx.getSamplerType(sampler);
        
        // Determine dimension count
        String dims = samplerType != null ? getDimensionality(samplerType.glslName) : "2D";
        return "_glsl_textureSize_" + dims + "(" + texObj + ", " + lod + ")";
    }
    
    private static String transformTextureQueryLod(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 2) return "float2(0.0, 0.0)";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getSamplerStateForSampler(sampler);
        return texObj + ".CalculateLevelOfDetail(" + samplerObj + ", " + args.get(1) + ")";
    }
    
    private static String transformTextureQueryLevels(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.isEmpty()) return "0";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        ctx.needsQueryLevelsHelper = true;
        return "_glsl_textureQueryLevels(" + texObj + ")";
    }
    
    private static String transformTextureSamples(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.isEmpty()) return "0";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        ctx.needsSamplesHelper = true;
        return "_glsl_textureSamples(" + texObj + ")";
    }
    
    private static String transformTextureGather(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 2) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getSamplerStateForSampler(sampler);
        
        if (args.size() == 2) {
            // textureGather(sampler, coord) - gathers red component
            return texObj + ".GatherRed(" + samplerObj + ", " + args.get(1) + ")";
        } else {
            // textureGather(sampler, coord, comp)
            String comp = args.get(2);
            return switch (comp) {
                case "0" -> texObj + ".GatherRed(" + samplerObj + ", " + args.get(1) + ")";
                case "1" -> texObj + ".GatherGreen(" + samplerObj + ", " + args.get(1) + ")";
                case "2" -> texObj + ".GatherBlue(" + samplerObj + ", " + args.get(1) + ")";
                case "3" -> texObj + ".GatherAlpha(" + samplerObj + ", " + args.get(1) + ")";
                default -> texObj + ".GatherRed(" + samplerObj + ", " + args.get(1) + ")";
            };
        }
    }
    
    private static String transformTextureGatherOffset(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 3) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getSamplerStateForSampler(sampler);
        
        String offset = args.get(2);
        String comp = args.size() > 3 ? args.get(3) : "0";
        
        return switch (comp) {
            case "0" -> texObj + ".GatherRed(" + samplerObj + ", " + args.get(1) + ", " + offset + ")";
            case "1" -> texObj + ".GatherGreen(" + samplerObj + ", " + args.get(1) + ", " + offset + ")";
            case "2" -> texObj + ".GatherBlue(" + samplerObj + ", " + args.get(1) + ", " + offset + ")";
            case "3" -> texObj + ".GatherAlpha(" + samplerObj + ", " + args.get(1) + ", " + offset + ")";
            default -> texObj + ".GatherRed(" + samplerObj + ", " + args.get(1) + ", " + offset + ")";
        };
    }
    
    private static String transformTextureGatherOffsets(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        // textureGatherOffsets(sampler, coord, offsets[4], comp)
        // HLSL doesn't have direct equivalent - need 4 separate gathers
        ctx.needsGatherOffsetsHelper = true;
        return "_glsl_textureGatherOffsets(" + String.join(", ", args) + ")";
    }
    
    private static String transformShadow(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 2) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getComparisonSamplerForSampler(sampler);
        // shadow2D coord.z is the compare value
        String coord = args.get(1);
        return texObj + ".SampleCmpLevelZero(" + samplerObj + ", " + coord + ".xy, " + coord + ".z)";
    }
    
    private static String transformShadowProj(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 2) return name + "(" + String.join(", ", args) + ")";
        String sampler = args.get(0);
        String texObj = ctx.getTextureForSampler(sampler);
        String samplerObj = ctx.getComparisonSamplerForSampler(sampler);
        String coord = args.get(1);
        return texObj + ".SampleCmpLevelZero(" + samplerObj + ", " + coord + ".xy / " + coord + ".w, " + coord + ".z / " + coord + ".w)";
    }
    
    // ─── Image Function Transformers ───
    
    private static String transformImageLoad(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 2) return name + "(" + String.join(", ", args) + ")";
        String image = args.get(0);
        String imageObj = ctx.getTextureForSampler(image); // Images use same tracking
        return imageObj + "[" + args.get(1) + "]";
    }
    
    private static String transformImageStore(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 3) return name + "(" + String.join(", ", args) + ")";
        String image = args.get(0);
        String imageObj = ctx.getTextureForSampler(image);
        return imageObj + "[" + args.get(1) + "] = " + args.get(2);
    }
    
    private static String transformImageSize(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.isEmpty()) return "int2(0, 0)";
        String image = args.get(0);
        String imageObj = ctx.getTextureForSampler(image);
        ctx.needsImageSizeHelper = true;
        return "_glsl_imageSize(" + imageObj + ")";
    }
    
    private static String transformImageSamples(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.isEmpty()) return "0";
        ctx.needsImageSamplesHelper = true;
        return "_glsl_imageSamples(" + args.get(0) + ")";
    }
    
    private static String transformImageAtomic(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 3) return name + "(" + String.join(", ", args) + ")";
        String image = args.get(0);
        String imageObj = ctx.getTextureForSampler(image);
        String coord = args.get(1);
        String data = args.get(2);
        
        // imageAtomicAdd -> InterlockedAdd on RWTexture
        String hlslOp = switch (name) {
            case "imageAtomicAdd" -> "InterlockedAdd";
            case "imageAtomicMin" -> "InterlockedMin";
            case "imageAtomicMax" -> "InterlockedMax";
            case "imageAtomicAnd" -> "InterlockedAnd";
            case "imageAtomicOr" -> "InterlockedOr";
            case "imageAtomicXor" -> "InterlockedXor";
            case "imageAtomicExchange" -> "InterlockedExchange";
            default -> "InterlockedAdd";
        };
        
        // HLSL atomics on images need original value out param
        ctx.needsImageAtomicHelper = true;
        return "_glsl_" + name + "(" + imageObj + ", " + coord + ", " + data + ")";
    }
    
    private static String transformImageAtomicCompSwap(String name, ObjectArrayList<String> args, TranslationContext ctx) {
        if (args.size() < 4) return name + "(" + String.join(", ", args) + ")";
        ctx.needsImageAtomicHelper = true;
        return "_glsl_imageAtomicCompSwap(" + String.join(", ", args) + ")";
    }
    
    private static String getDimensionality(String samplerType) {
        if (samplerType.contains("1D")) return "1D";
        if (samplerType.contains("3D")) return "3D";
        if (samplerType.contains("Cube")) return "Cube";
        if (samplerType.contains("Buffer")) return "Buffer";
        return "2D";
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 10: BUILT-IN VARIABLE MAPPINGS
    // ════════════════════════════════════════════════════════════════════════════
    
    /** Maps GLSL built-in variables to HLSL semantics */
    public static final class BuiltInVariable {
        public final String glslName;
        public final String hlslName;
        public final String hlslSemantic;
        public final String hlslType;
        public final boolean isInput;
        public final boolean isOutput;
        public final EnumSet<ShaderStage> validStages;
        
        private BuiltInVariable(String glsl, String hlsl, String semantic, String type,
                               boolean in, boolean out, ShaderStage... stages) {
            this.glslName = glsl;
            this.hlslName = hlsl;
            this.hlslSemantic = semantic;
            this.hlslType = type;
            this.isInput = in;
            this.isOutput = out;
            this.validStages = stages.length > 0 ? 
                EnumSet.of(stages[0], stages) : EnumSet.allOf(ShaderStage.class);
        }
        
        public static BuiltInVariable input(String glsl, String hlsl, String semantic, String type, ShaderStage... stages) {
            return new BuiltInVariable(glsl, hlsl, semantic, type, true, false, stages);
        }
        
        public static BuiltInVariable output(String glsl, String hlsl, String semantic, String type, ShaderStage... stages) {
            return new BuiltInVariable(glsl, hlsl, semantic, type, false, true, stages);
        }
        
        public static BuiltInVariable inout(String glsl, String hlsl, String semantic, String type, ShaderStage... stages) {
            return new BuiltInVariable(glsl, hlsl, semantic, type, true, true, stages);
        }
    }
    
    private static final Object2ObjectOpenHashMap<String, BuiltInVariable> BUILTIN_VARS = new Object2ObjectOpenHashMap<>();
    
    static {
        // ─── Vertex Shader ───
        BUILTIN_VARS.put("gl_VertexID", BuiltInVariable.input(
            "gl_VertexID", "vertexID", "SV_VertexID", "uint", ShaderStage.VERTEX));
        BUILTIN_VARS.put("gl_InstanceID", BuiltInVariable.input(
            "gl_InstanceID", "instanceID", "SV_InstanceID", "uint", ShaderStage.VERTEX));
        BUILTIN_VARS.put("gl_BaseVertex", BuiltInVariable.input(
            "gl_BaseVertex", "baseVertex", "SV_StartVertexLocation", "uint", ShaderStage.VERTEX));
        BUILTIN_VARS.put("gl_BaseInstance", BuiltInVariable.input(
            "gl_BaseInstance", "baseInstance", "SV_StartInstanceLocation", "uint", ShaderStage.VERTEX));
        BUILTIN_VARS.put("gl_DrawID", BuiltInVariable.input(
            "gl_DrawID", "drawID", "SV_DrawID", "uint", ShaderStage.VERTEX));
        
        BUILTIN_VARS.put("gl_Position", BuiltInVariable.output(
            "gl_Position", "position", "SV_Position", "float4", 
            ShaderStage.VERTEX, ShaderStage.GEOMETRY, ShaderStage.DOMAIN));
        BUILTIN_VARS.put("gl_PointSize", BuiltInVariable.output(
            "gl_PointSize", "pointSize", "PSIZE", "float", ShaderStage.VERTEX));
        BUILTIN_VARS.put("gl_ClipDistance", BuiltInVariable.output(
            "gl_ClipDistance", "clipDistance", "SV_ClipDistance", "float", 
            ShaderStage.VERTEX, ShaderStage.GEOMETRY));
        BUILTIN_VARS.put("gl_CullDistance", BuiltInVariable.output(
            "gl_CullDistance", "cullDistance", "SV_CullDistance", "float",
            ShaderStage.VERTEX, ShaderStage.GEOMETRY));
        
        // ─── Fragment Shader ───
        BUILTIN_VARS.put("gl_FragCoord", BuiltInVariable.input(
            "gl_FragCoord", "fragCoord", "SV_Position", "float4", ShaderStage.FRAGMENT));
        BUILTIN_VARS.put("gl_FrontFacing", BuiltInVariable.input(
            "gl_FrontFacing", "frontFacing", "SV_IsFrontFace", "bool", ShaderStage.FRAGMENT));
        BUILTIN_VARS.put("gl_PointCoord", BuiltInVariable.input(
            "gl_PointCoord", "pointCoord", "SV_Position", "float2", ShaderStage.FRAGMENT)); // Approximation
        BUILTIN_VARS.put("gl_SampleID", BuiltInVariable.input(
            "gl_SampleID", "sampleID", "SV_SampleIndex", "uint", ShaderStage.FRAGMENT));
        BUILTIN_VARS.put("gl_SamplePosition", BuiltInVariable.input(
            "gl_SamplePosition", "samplePosition", "SV_Position", "float2", ShaderStage.FRAGMENT));
        BUILTIN_VARS.put("gl_SampleMaskIn", BuiltInVariable.input(
            "gl_SampleMaskIn", "sampleMaskIn", "SV_Coverage", "uint", ShaderStage.FRAGMENT));
        BUILTIN_VARS.put("gl_Layer", BuiltInVariable.input(
            "gl_Layer", "layer", "SV_RenderTargetArrayIndex", "uint", ShaderStage.FRAGMENT));
        BUILTIN_VARS.put("gl_ViewportIndex", BuiltInVariable.input(
            "gl_ViewportIndex", "viewportIndex", "SV_ViewportArrayIndex", "uint", ShaderStage.FRAGMENT));
        BUILTIN_VARS.put("gl_PrimitiveID", BuiltInVariable.input(
            "gl_PrimitiveID", "primitiveID", "SV_PrimitiveID", "uint", 
            ShaderStage.FRAGMENT, ShaderStage.GEOMETRY, ShaderStage.HULL, ShaderStage.DOMAIN));
        
        BUILTIN_VARS.put("gl_FragColor", BuiltInVariable.output(
            "gl_FragColor", "fragColor", "SV_Target0", "float4", ShaderStage.FRAGMENT));
        BUILTIN_VARS.put("gl_FragData", BuiltInVariable.output(
            "gl_FragData", "fragData", "SV_Target", "float4", ShaderStage.FRAGMENT));
        BUILTIN_VARS.put("gl_FragDepth", BuiltInVariable.output(
            "gl_FragDepth", "fragDepth", "SV_Depth", "float", ShaderStage.FRAGMENT));
        BUILTIN_VARS.put("gl_SampleMask", BuiltInVariable.output(
            "gl_SampleMask", "sampleMask", "SV_Coverage", "uint", ShaderStage.FRAGMENT));
        
        // ─── Geometry Shader ───
        BUILTIN_VARS.put("gl_InvocationID", BuiltInVariable.input(
            "gl_InvocationID", "invocationID", "SV_GSInstanceID", "uint", ShaderStage.GEOMETRY));
        BUILTIN_VARS.put("gl_PrimitiveIDIn", BuiltInVariable.input(
            "gl_PrimitiveIDIn", "primitiveIDIn", "SV_PrimitiveID", "uint", ShaderStage.GEOMETRY));
        
        // ─── Tessellation Control Shader (Hull Shader) ───
        BUILTIN_VARS.put("gl_TessLevelOuter", BuiltInVariable.output(
            "gl_TessLevelOuter", "tessLevelOuter", "SV_TessFactor", "float", ShaderStage.HULL));
        BUILTIN_VARS.put("gl_TessLevelInner", BuiltInVariable.output(
            "gl_TessLevelInner", "tessLevelInner", "SV_InsideTessFactor", "float", ShaderStage.HULL));
        BUILTIN_VARS.put("gl_PatchVerticesIn", BuiltInVariable.input(
            "gl_PatchVerticesIn", "patchVerticesIn", "", "uint", ShaderStage.HULL, ShaderStage.DOMAIN));
        
        // ─── Tessellation Evaluation Shader (Domain Shader) ───
        BUILTIN_VARS.put("gl_TessCoord", BuiltInVariable.input(
            "gl_TessCoord", "tessCoord", "SV_DomainLocation", "float3", ShaderStage.DOMAIN));
        
        // ─── Compute Shader ───
        BUILTIN_VARS.put("gl_NumWorkGroups", BuiltInVariable.input(
            "gl_NumWorkGroups", "numWorkGroups", "", "uint3", ShaderStage.COMPUTE));
        BUILTIN_VARS.put("gl_WorkGroupSize", BuiltInVariable.input(
            "gl_WorkGroupSize", "workGroupSize", "", "uint3", ShaderStage.COMPUTE));
        BUILTIN_VARS.put("gl_WorkGroupID", BuiltInVariable.input(
            "gl_WorkGroupID", "workGroupID", "SV_GroupID", "uint3", ShaderStage.COMPUTE));
        BUILTIN_VARS.put("gl_LocalInvocationID", BuiltInVariable.input(
            "gl_LocalInvocationID", "localInvocationID", "SV_GroupThreadID", "uint3", ShaderStage.COMPUTE));
        BUILTIN_VARS.put("gl_GlobalInvocationID", BuiltInVariable.input(
            "gl_GlobalInvocationID", "globalInvocationID", "SV_DispatchThreadID", "uint3", ShaderStage.COMPUTE));
        BUILTIN_VARS.put("gl_LocalInvocationIndex", BuiltInVariable.input(
            "gl_LocalInvocationIndex", "localInvocationIndex", "SV_GroupIndex", "uint", ShaderStage.COMPUTE));
        
        // ─── Subgroup / Wave Operations (GLSL 4.6 / SM 6.0) ───
        BUILTIN_VARS.put("gl_SubgroupSize", BuiltInVariable.input(
            "gl_SubgroupSize", "WaveGetLaneCount()", "", "uint", ShaderStage.COMPUTE, ShaderStage.FRAGMENT));
        BUILTIN_VARS.put("gl_SubgroupInvocationID", BuiltInVariable.input(
            "gl_SubgroupInvocationID", "WaveGetLaneIndex()", "", "uint", ShaderStage.COMPUTE, ShaderStage.FRAGMENT));
        BUILTIN_VARS.put("gl_SubgroupEqMask", BuiltInVariable.input(
            "gl_SubgroupEqMask", "_glsl_SubgroupEqMask()", "", "uint4", ShaderStage.COMPUTE));
        BUILTIN_VARS.put("gl_SubgroupGeMask", BuiltInVariable.input(
            "gl_SubgroupGeMask", "_glsl_SubgroupGeMask()", "", "uint4", ShaderStage.COMPUTE));
        BUILTIN_VARS.put("gl_SubgroupGtMask", BuiltInVariable.input(
            "gl_SubgroupGtMask", "_glsl_SubgroupGtMask()", "", "uint4", ShaderStage.COMPUTE));
        BUILTIN_VARS.put("gl_SubgroupLeMask", BuiltInVariable.input(
            "gl_SubgroupLeMask", "_glsl_SubgroupLeMask()", "", "uint4", ShaderStage.COMPUTE));
        BUILTIN_VARS.put("gl_SubgroupLtMask", BuiltInVariable.input(
            "gl_SubgroupLtMask", "_glsl_SubgroupLtMask()", "", "uint4", ShaderStage.COMPUTE));
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // END OF PART 2
    // CONTINUED IN PART 3: CODE GENERATOR, TRANSLATION CONTEXT, PUBLIC API
    // ════════════════════════════════════════════════════════════════════════════


    // ════════════════════════════════════════════════════════════════════════════
    // PART 3: CODE GENERATOR, TRANSLATION CONTEXT, PUBLIC API
    // ════════════════════════════════════════════════════════════════════════════
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 11: TRANSLATION CONTEXT
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Maintains state during translation including:
     * - Sampler/texture bindings
     * - Variable tracking
     * - Required helper functions
     * - Constant buffer organization
     */
    public static final class TranslationContext {
        public final Config config;
        
        // Resource binding tracking
        private final Object2ObjectOpenHashMap<String, SamplerBinding> samplerBindings = new Object2ObjectOpenHashMap<>();
        private final Object2ObjectOpenHashMap<String, UniformInfo> uniformInfos = new Object2ObjectOpenHashMap<>();
        private final ObjectArrayList<ConstantBufferInfo> constantBuffers = new ObjectArrayList<>();
        private final Object2ObjectOpenHashMap<String, TypeInfo> variableTypes = new Object2ObjectOpenHashMap<>();
        
        // Binding slot counters
        private int nextTextureSlot = 0;
        private int nextSamplerSlot = 0;
        private int nextCBufferSlot = 0;
        private int nextUAVSlot = 0;
        
        // Input/output tracking for struct generation
        private final ObjectArrayList<IOVariable> vertexInputs = new ObjectArrayList<>();
        private final ObjectArrayList<IOVariable> stageInputs = new ObjectArrayList<>();
        private final ObjectArrayList<IOVariable> stageOutputs = new ObjectArrayList<>();
        private final ObjectOpenHashSet<String> usedBuiltins = new ObjectOpenHashSet<>();
        
        // Required helper function flags
        boolean needsModHelper = false;
        boolean needsInverseHelper = false;
        boolean needsOuterProductHelper = false;
        boolean needsTextureSizeHelper = false;
        boolean needsQueryLevelsHelper = false;
        boolean needsSamplesHelper = false;
        boolean needsGatherOffsetsHelper = false;
        boolean needsImageSizeHelper = false;
        boolean needsImageSamplesHelper = false;
        boolean needsImageAtomicHelper = false;
        boolean needsUnpackDouble = false;
        boolean needsBitfieldHelpers = false;
        boolean needsPackingHelpers = false;
        boolean needsNoiseHelpers = false;
        boolean needsFrexpHelper = false;
        boolean needsSubgroupMaskHelpers = false;
        boolean requiresStreamOutput = false;
        
        // Collected errors and warnings
        private final ObjectArrayList<String> errors = new ObjectArrayList<>();
        private final ObjectArrayList<String> warnings = new ObjectArrayList<>();
        
        public TranslationContext(Config config) {
            this.config = config;
        }
        
        // ─── Sampler/Texture Management ───
        
        public void registerSampler(String glslName, String glslType, @Nullable LayoutQualifier layout) {
            TypeInfo typeInfo = TYPE_MAP.get(glslType);
            if (typeInfo == null) {
                warnings.add("Unknown sampler type: " + glslType);
                typeInfo = TYPE_MAP.get("sampler2D");
            }
            
            int binding = -1;
            if (layout != null && layout.qualifiers().containsKey("binding")) {
                Expression bindExpr = layout.qualifiers().get("binding");
                if (bindExpr instanceof LiteralExpr lit) {
                    binding = ((Number) lit.value()).intValue();
                }
            }
            
            if (binding < 0) {
                binding = nextTextureSlot;
            }
            
            SamplerBinding sb = new SamplerBinding(
                glslName,
                glslName + "_Texture",
                glslName + "_Sampler",
                typeInfo.hlslName,
                binding,
                nextSamplerSlot++,
                typeInfo.glslName.contains("Shadow")
            );
            
            samplerBindings.put(glslName, sb);
            variableTypes.put(glslName, typeInfo);
            nextTextureSlot = Math.max(nextTextureSlot, binding + 1);
        }
        
        public void registerImage(String glslName, String glslType, @Nullable LayoutQualifier layout) {
            TypeInfo typeInfo = TYPE_MAP.get(glslType);
            if (typeInfo == null) {
                warnings.add("Unknown image type: " + glslType);
                typeInfo = TYPE_MAP.get("image2D");
            }
            
            int binding = nextUAVSlot;
            if (layout != null && layout.qualifiers().containsKey("binding")) {
                Expression bindExpr = layout.qualifiers().get("binding");
                if (bindExpr instanceof LiteralExpr lit) {
                    binding = ((Number) lit.value()).intValue();
                }
            }
            
            // Images don't need samplers, just UAV binding
            SamplerBinding sb = new SamplerBinding(
                glslName,
                glslName, // No separate texture
                null,     // No sampler
                typeInfo.hlslName,
                binding,
                -1,
                false
            );
            
            samplerBindings.put(glslName, sb);
            variableTypes.put(glslName, typeInfo);
            nextUAVSlot = Math.max(nextUAVSlot, binding + 1);
        }
        
        public String getTextureForSampler(String glslSampler) {
            SamplerBinding sb = samplerBindings.get(glslSampler);
            return sb != null ? sb.textureName : glslSampler + "_Texture";
        }
        
        public String getSamplerStateForSampler(String glslSampler) {
            SamplerBinding sb = samplerBindings.get(glslSampler);
            return sb != null && sb.samplerName != null ? sb.samplerName : glslSampler + "_Sampler";
        }
        
        public String getComparisonSamplerForSampler(String glslSampler) {
            return getSamplerStateForSampler(glslSampler) + "_Cmp";
        }
        
        public TypeInfo getSamplerType(String glslSampler) {
            return variableTypes.get(glslSampler);
        }
        
        // ─── Uniform/Constant Buffer Management ───
        
        public void registerUniform(String name, String glslType, @Nullable LayoutQualifier layout) {
            TypeInfo typeInfo = TYPE_MAP.getOrDefault(glslType, TypeInfo.struct(glslType));
            
            int location = -1;
            String bufferName = "Globals";
            
            if (layout != null) {
                if (layout.qualifiers().containsKey("location")) {
                    Expression locExpr = layout.qualifiers().get("location");
                    if (locExpr instanceof LiteralExpr lit) {
                        location = ((Number) lit.value()).intValue();
                    }
                }
                if (layout.qualifiers().containsKey("binding")) {
                    Expression bindExpr = layout.qualifiers().get("binding");
                    if (bindExpr instanceof LiteralExpr lit) {
                        bufferName = "CBuffer" + ((Number) lit.value()).intValue();
                    }
                }
            }
            
            UniformInfo info = new UniformInfo(name, typeInfo, location, bufferName);
            uniformInfos.put(name, info);
            variableTypes.put(name, typeInfo);
        }
        
        public void registerVariable(String name, TypeInfo type) {
            variableTypes.put(name, type);
        }
        
        // ─── Input/Output Management ───
        
        public void registerInput(String name, String glslType, @Nullable LayoutQualifier layout,
                                  EnumSet<Qualifier> qualifiers) {
            int location = extractLocation(layout);
            String semantic = inferSemantic(name, location, true);
            String interpolation = extractInterpolation(qualifiers);
            
            IOVariable io = new IOVariable(name, glslType, semantic, location, interpolation, true);
            
            if (config.stage == ShaderStage.VERTEX) {
                vertexInputs.add(io);
            } else {
                stageInputs.add(io);
            }
            
            TypeInfo typeInfo = TYPE_MAP.getOrDefault(glslType, TypeInfo.struct(glslType));
            variableTypes.put(name, typeInfo);
        }
        
        public void registerOutput(String name, String glslType, @Nullable LayoutQualifier layout,
                                   EnumSet<Qualifier> qualifiers) {
            int location = extractLocation(layout);
            String semantic = inferSemantic(name, location, false);
            String interpolation = extractInterpolation(qualifiers);
            
            IOVariable io = new IOVariable(name, glslType, semantic, location, interpolation, false);
            stageOutputs.add(io);
            
            TypeInfo typeInfo = TYPE_MAP.getOrDefault(glslType, TypeInfo.struct(glslType));
            variableTypes.put(name, typeInfo);
        }
        
        public void markBuiltinUsed(String name) {
            usedBuiltins.add(name);
        }
        
        private int extractLocation(@Nullable LayoutQualifier layout) {
            if (layout == null) return -1;
            Expression locExpr = layout.qualifiers().get("location");
            if (locExpr instanceof LiteralExpr lit) {
                return ((Number) lit.value()).intValue();
            }
            return -1;
        }
        
        private String inferSemantic(String name, int location, boolean isInput) {
            // Check if it's a built-in
            BuiltInVariable builtin = BUILTIN_VARS.get(name);
            if (builtin != null) {
                return builtin.hlslSemantic;
            }
            
            // Standard naming conventions
            String upperName = name.toUpperCase();
            if (upperName.contains("POSITION") || upperName.contains("POS")) {
                return isInput && config.stage == ShaderStage.VERTEX ? "POSITION" : "SV_Position";
            }
            if (upperName.contains("NORMAL") || upperName.contains("NORM")) return "NORMAL";
            if (upperName.contains("TANGENT")) return "TANGENT";
            if (upperName.contains("BINORMAL") || upperName.contains("BITANGENT")) return "BINORMAL";
            if (upperName.contains("COLOR") || upperName.contains("COL")) return "COLOR";
            if (upperName.contains("TEXCOORD") || upperName.contains("UV")) {
                return "TEXCOORD" + Math.max(0, location);
            }
            
            // Default: use TEXCOORD with location
            if (location >= 0) return "TEXCOORD" + location;
            return "TEXCOORD" + (isInput ? stageInputs.size() : stageOutputs.size());
        }
        
        private String extractInterpolation(EnumSet<Qualifier> qualifiers) {
            if (qualifiers.contains(Qualifier.FLAT)) return "nointerpolation";
            if (qualifiers.contains(Qualifier.NOPERSPECTIVE)) return "noperspective";
            if (qualifiers.contains(Qualifier.CENTROID)) return "centroid";
            if (qualifiers.contains(Qualifier.SAMPLE)) return "sample";
            return "";
        }
        
        // ─── Error Reporting ───
        
        public void error(String msg) { errors.add(msg); }
        public void warning(String msg) { warnings.add(msg); }
        public ObjectArrayList<String> getErrors() { return errors; }
        public ObjectArrayList<String> getWarnings() { return warnings; }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }
    
    // ─── Supporting Records ───
    
    public record SamplerBinding(
        String glslName,
        String textureName,
        @Nullable String samplerName,
        String hlslType,
        int textureSlot,
        int samplerSlot,
        boolean isShadow
    ) {}
    
    public record UniformInfo(
        String name,
        TypeInfo type,
        int location,
        String bufferName
    ) {}
    
    public record ConstantBufferInfo(
        String name,
        int slot,
        ObjectArrayList<UniformInfo> members,
        int sizeBytes
    ) {}
    
    public record IOVariable(
        String name,
        String glslType,
        String semantic,
        int location,
        String interpolation,
        boolean isInput
    ) {}
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 12: CODE GENERATOR
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Generates HLSL code from parsed AST with proper formatting.
     */
    private static final class CodeGenerator {
        private final TranslationContext ctx;
        private final StringBuilder output = new StringBuilder(8192);
        private int indentLevel = 0;
        private static final String INDENT = "    ";
        
        CodeGenerator(TranslationContext ctx) {
            this.ctx = ctx;
        }
        
        String generate(TranslationUnit unit) {
            // Header
            emitHeader();
            
            // First pass: collect declarations
            collectDeclarations(unit);
            
            // Emit resource declarations
            emitResourceDeclarations();
            
            // Emit input/output structs
            emitIOStructs();
            
            // Emit helper functions
            emitHelperFunctions();
            
            // Emit user structs
            for (ASTNode node : unit.declarations()) {
                if (node instanceof StructDecl sd) {
                    emitStruct(sd);
                }
            }
            
            // Emit global constants
            emitGlobalConstants(unit);
            
            // Emit functions
            for (ASTNode node : unit.declarations()) {
                if (node instanceof FunctionDecl fd) {
                    emitFunction(fd);
                }
            }
            
            return output.toString();
        }
        
        // ─── Header Generation ───
        
        private void emitHeader() {
            line("// ═══════════════════════════════════════════════════════════════");
            line("// Auto-generated HLSL - Translated from GLSL");
            line("// Target: " + ctx.config.shaderModel.profile);
            line("// Stage: " + ctx.config.stage.name());
            line("// ═══════════════════════════════════════════════════════════════");
            line("");
            
            // Matrix convention pragma
            if (ctx.config.matrixConvention == MatrixConvention.PRAGMA_COLUMN_MAJOR) {
                line("#pragma pack_matrix(column_major)");
                line("");
            }
        }
        
        // ─── Declaration Collection ───
        
        private void collectDeclarations(TranslationUnit unit) {
            for (ASTNode node : unit.declarations()) {
                if (node instanceof VariableDecl vd) {
                    collectVariableDecl(vd);
                } else if (node instanceof InterfaceBlockDecl ibd) {
                    collectInterfaceBlock(ibd);
                }
            }
        }
        
        private void collectVariableDecl(VariableDecl vd) {
            String typeName = vd.type().name();
            TypeInfo typeInfo = TYPE_MAP.get(typeName);
            
            if (vd.qualifiers().contains(Qualifier.UNIFORM)) {
                if (typeInfo != null && typeInfo.isSampler()) {
                    ctx.registerSampler(vd.name(), typeName, vd.layout());
                } else if (typeInfo != null && typeInfo.isImage()) {
                    ctx.registerImage(vd.name(), typeName, vd.layout());
                } else {
                    ctx.registerUniform(vd.name(), typeName, vd.layout());
                }
            } else if (vd.qualifiers().contains(Qualifier.IN) || vd.qualifiers().contains(Qualifier.ATTRIBUTE)) {
                ctx.registerInput(vd.name(), typeName, vd.layout(), vd.qualifiers());
            } else if (vd.qualifiers().contains(Qualifier.OUT) || vd.qualifiers().contains(Qualifier.VARYING)) {
                ctx.registerOutput(vd.name(), typeName, vd.layout(), vd.qualifiers());
            }
        }
        
        private void collectInterfaceBlock(InterfaceBlockDecl ibd) {
            // Handle uniform blocks as constant buffers
            if (ibd.qualifiers().contains(Qualifier.UNIFORM)) {
                // Will be emitted as cbuffer
                for (VariableDecl member : ibd.members()) {
                    ctx.registerUniform(
                        ibd.instanceName() != null ? ibd.instanceName() + "." + member.name() : member.name(),
                        member.type().name(),
                        member.layout()
                    );
                }
            }
        }
        
        // ─── Resource Declaration Emission ───
        
        private void emitResourceDeclarations() {
            // Textures
            boolean hasTextures = false;
            for (SamplerBinding sb : ctx.samplerBindings.values()) {
                if (sb.samplerName() != null) { // Regular texture, not image
                    if (!hasTextures) {
                        line("// ═══ Textures ═══");
                        hasTextures = true;
                    }
                    line(sb.hlslType() + " " + sb.textureName() + " : register(t" + sb.textureSlot() + ");");
                }
            }
            if (hasTextures) line("");
            
            // Samplers
            boolean hasSamplers = false;
            for (SamplerBinding sb : ctx.samplerBindings.values()) {
                if (sb.samplerName() != null) {
                    if (!hasSamplers) {
                        line("// ═══ Samplers ═══");
                        hasSamplers = true;
                    }
                    line("SamplerState " + sb.samplerName() + " : register(s" + sb.samplerSlot() + ");");
                    if (sb.isShadow()) {
                        line("SamplerComparisonState " + sb.samplerName() + "_Cmp : register(s" + (sb.samplerSlot() + 8) + ");");
                    }
                }
            }
            if (hasSamplers) line("");
            
            // UAVs (Images)
            boolean hasUAVs = false;
            for (SamplerBinding sb : ctx.samplerBindings.values()) {
                if (sb.samplerName() == null) { // Image/UAV
                    if (!hasUAVs) {
                        line("// ═══ UAVs (Images) ═══");
                        hasUAVs = true;
                    }
                    line(sb.hlslType() + " " + sb.textureName() + " : register(u" + sb.textureSlot() + ");");
                }
            }
            if (hasUAVs) line("");
            
            // Constant Buffers
            emitConstantBuffers();
        }
        
        private void emitConstantBuffers() {
            // Group uniforms by buffer
            Object2ObjectOpenHashMap<String, ObjectArrayList<UniformInfo>> bufferGroups = new Object2ObjectOpenHashMap<>();
            for (UniformInfo ui : ctx.uniformInfos.values()) {
                bufferGroups.computeIfAbsent(ui.bufferName(), k -> new ObjectArrayList<>()).add(ui);
            }
            
            if (bufferGroups.isEmpty()) return;
            
            line("// ═══ Constant Buffers ═══");
            int slot = 0;
            for (var entry : bufferGroups.object2ObjectEntrySet()) {
                line("cbuffer " + entry.getKey() + " : register(b" + slot + ")");
                line("{");
                indentLevel++;
                for (UniformInfo ui : entry.getValue()) {
                    String hlslType = ui.type().hlslName;
                    line(hlslType + " " + ui.name() + ";");
                }
                indentLevel--;
                line("};");
                line("");
                slot++;
            }
        }
        
        // ─── IO Struct Emission ───
        
        private void emitIOStructs() {
            // Vertex Input struct
            if (ctx.config.stage == ShaderStage.VERTEX && !ctx.vertexInputs.isEmpty()) {
                line("// ═══ Vertex Input ═══");
                line("struct VS_INPUT");
                line("{");
                indentLevel++;
                for (IOVariable io : ctx.vertexInputs) {
                    String hlslType = translateType(io.glslType());
                    line(hlslType + " " + io.name() + " : " + io.semantic() + ";");
                }
                indentLevel--;
                line("};");
                line("");
            }
            
            // Stage output struct
            if (!ctx.stageOutputs.isEmpty()) {
                String structName = switch (ctx.config.stage) {
                    case VERTEX -> "VS_OUTPUT";
                    case FRAGMENT -> "PS_OUTPUT";
                    case GEOMETRY -> "GS_OUTPUT";
                    case HULL -> "HS_OUTPUT";
                    case DOMAIN -> "DS_OUTPUT";
                    case COMPUTE -> "CS_OUTPUT";
                };
                
                line("// ═══ " + ctx.config.stage.name() + " Output ═══");
                line("struct " + structName);
                line("{");
                indentLevel++;
                
                // Add position for vertex shader
                if (ctx.config.stage == ShaderStage.VERTEX && ctx.usedBuiltins.contains("gl_Position")) {
                    line("float4 position : SV_Position;");
                }
                
                for (IOVariable io : ctx.stageOutputs) {
                    String hlslType = translateType(io.glslType());
                    String interp = io.interpolation().isEmpty() ? "" : io.interpolation() + " ";
                    line(interp + hlslType + " " + io.name() + " : " + io.semantic() + ";");
                }
                
                // Fragment shader built-ins
                if (ctx.config.stage == ShaderStage.FRAGMENT) {
                    if (ctx.usedBuiltins.contains("gl_FragDepth")) {
                        line("float fragDepth : SV_Depth;");
                    }
                }
                
                indentLevel--;
                line("};");
                line("");
            }
            
            // Stage input struct (for non-vertex shaders)
            if (ctx.config.stage != ShaderStage.VERTEX && !ctx.stageInputs.isEmpty()) {
                String structName = switch (ctx.config.stage) {
                    case FRAGMENT -> "PS_INPUT";
                    case GEOMETRY -> "GS_INPUT";
                    case HULL -> "HS_INPUT";
                    case DOMAIN -> "DS_INPUT";
                    default -> "STAGE_INPUT";
                };
                
                line("// ═══ " + ctx.config.stage.name() + " Input ═══");
                line("struct " + structName);
                line("{");
                indentLevel++;
                
                if (ctx.config.stage == ShaderStage.FRAGMENT) {
                    line("float4 position : SV_Position;");
                }
                
                for (IOVariable io : ctx.stageInputs) {
                    String hlslType = translateType(io.glslType());
                    String interp = io.interpolation().isEmpty() ? "" : io.interpolation() + " ";
                    line(interp + hlslType + " " + io.name() + " : " + io.semantic() + ";");
                }
                
                indentLevel--;
                line("};");
                line("");
            }
        }
        
        // ─── Helper Function Emission ───
        
        private void emitHelperFunctions() {
            boolean needsSection = ctx.needsModHelper || ctx.needsInverseHelper || 
                                   ctx.needsTextureSizeHelper || ctx.needsPackingHelpers;
            
            if (!needsSection) return;
            
            line("// ═══ GLSL Compatibility Helpers ═══");
            line("");
            
            if (ctx.needsModHelper) {
                line("// GLSL mod() has different semantics than HLSL fmod()");
                line("float _glsl_mod(float x, float y) { return x - y * floor(x / y); }");
                line("float2 _glsl_mod(float2 x, float2 y) { return x - y * floor(x / y); }");
                line("float3 _glsl_mod(float3 x, float3 y) { return x - y * floor(x / y); }");
                line("float4 _glsl_mod(float4 x, float4 y) { return x - y * floor(x / y); }");
                line("float2 _glsl_mod(float2 x, float y) { return x - y * floor(x / y); }");
                line("float3 _glsl_mod(float3 x, float y) { return x - y * floor(x / y); }");
                line("float4 _glsl_mod(float4 x, float y) { return x - y * floor(x / y); }");
                line("");
            }
            
            if (ctx.needsInverseHelper) {
                line("// Matrix inverse (2x2)");
                line("float2x2 _glsl_inverse(float2x2 m) {");
                line("    float det = m[0][0] * m[1][1] - m[0][1] * m[1][0];");
                line("    return float2x2(m[1][1], -m[0][1], -m[1][0], m[0][0]) / det;");
                line("}");
                line("");
                line("// Matrix inverse (3x3)");
                line("float3x3 _glsl_inverse(float3x3 m) {");
                line("    float3 c0 = m[0], c1 = m[1], c2 = m[2];");
                line("    float3 t0 = float3(c1.y*c2.z - c1.z*c2.y, c1.z*c2.x - c1.x*c2.z, c1.x*c2.y - c1.y*c2.x);");
                line("    float3 t1 = float3(c0.z*c2.y - c0.y*c2.z, c0.x*c2.z - c0.z*c2.x, c0.y*c2.x - c0.x*c2.y);");
                line("    float3 t2 = float3(c0.y*c1.z - c0.z*c1.y, c0.z*c1.x - c0.x*c1.z, c0.x*c1.y - c0.y*c1.x);");
                line("    float det = dot(c0, t0);");
                line("    return float3x3(t0/det, t1/det, t2/det);");
                line("}");
                line("");
                line("// Matrix inverse (4x4)");
                line("float4x4 _glsl_inverse(float4x4 m) {");
                line("    float n11=m[0][0],n12=m[1][0],n13=m[2][0],n14=m[3][0];");
                line("    float n21=m[0][1],n22=m[1][1],n23=m[2][1],n24=m[3][1];");
                line("    float n31=m[0][2],n32=m[1][2],n33=m[2][2],n34=m[3][2];");
                line("    float n41=m[0][3],n42=m[1][3],n43=m[2][3],n44=m[3][3];");
                line("    float t11=n23*n34*n42-n24*n33*n42+n24*n32*n43-n22*n34*n43-n23*n32*n44+n22*n33*n44;");
                line("    float t12=n14*n33*n42-n13*n34*n42-n14*n32*n43+n12*n34*n43+n13*n32*n44-n12*n33*n44;");
                line("    float t13=n13*n24*n42-n14*n23*n42+n14*n22*n43-n12*n24*n43-n13*n22*n44+n12*n23*n44;");
                line("    float t14=n14*n23*n32-n13*n24*n32-n14*n22*n33+n12*n24*n33+n13*n22*n34-n12*n23*n34;");
                line("    float det=n11*t11+n21*t12+n31*t13+n41*t14;");
                line("    float idet=1.0/det;");
                line("    float4x4 ret;");
                line("    ret[0][0]=t11*idet;");
                line("    ret[0][1]=(n24*n33*n41-n23*n34*n41-n24*n31*n43+n21*n34*n43+n23*n31*n44-n21*n33*n44)*idet;");
                line("    ret[0][2]=(n22*n34*n41-n24*n32*n41+n24*n31*n42-n21*n34*n42-n22*n31*n44+n21*n32*n44)*idet;");
                line("    ret[0][3]=(n23*n32*n41-n22*n33*n41-n23*n31*n42+n21*n33*n42+n22*n31*n43-n21*n32*n43)*idet;");
                line("    ret[1][0]=t12*idet;");
                line("    ret[1][1]=(n13*n34*n41-n14*n33*n41+n14*n31*n43-n11*n34*n43-n13*n31*n44+n11*n33*n44)*idet;");
                line("    ret[1][2]=(n14*n32*n41-n12*n34*n41-n14*n31*n42+n11*n34*n42+n12*n31*n44-n11*n32*n44)*idet;");
                line("    ret[1][3]=(n12*n33*n41-n13*n32*n41+n13*n31*n42-n11*n33*n42-n12*n31*n43+n11*n32*n43)*idet;");
                line("    ret[2][0]=t13*idet;");
                line("    ret[2][1]=(n14*n23*n41-n13*n24*n41-n14*n21*n43+n11*n24*n43+n13*n21*n44-n11*n23*n44)*idet;");
                line("    ret[2][2]=(n12*n24*n41-n14*n22*n41+n14*n21*n42-n11*n24*n42-n12*n21*n44+n11*n22*n44)*idet;");
                line("    ret[2][3]=(n13*n22*n41-n12*n23*n41-n13*n21*n42+n11*n23*n42+n12*n21*n43-n11*n22*n43)*idet;");
                line("    ret[3][0]=t14*idet;");
                line("    ret[3][1]=(n13*n24*n31-n14*n23*n31+n14*n21*n33-n11*n24*n33-n13*n21*n34+n11*n23*n34)*idet;");
                line("    ret[3][2]=(n14*n22*n31-n12*n24*n31-n14*n21*n32+n11*n24*n32+n12*n21*n34-n11*n22*n34)*idet;");
                line("    ret[3][3]=(n12*n23*n31-n13*n22*n31+n13*n21*n32-n11*n23*n32-n12*n21*n33+n11*n22*n33)*idet;");
                line("    return ret;");
                line("}");
                line("");
            }
            
            if (ctx.needsTextureSizeHelper) {
                line("// textureSize helpers");
                line("int2 _glsl_textureSize_2D(Texture2D<float4> tex, int lod) {");
                line("    uint w, h, levels;");
                line("    tex.GetDimensions(lod, w, h, levels);");
                line("    return int2(w, h);");
                line("}");
                line("int3 _glsl_textureSize_3D(Texture3D<float4> tex, int lod) {");
                line("    uint w, h, d, levels;");
                line("    tex.GetDimensions(lod, w, h, d, levels);");
                line("    return int3(w, h, d);");
                line("}");
                line("");
            }
            
            if (ctx.needsQueryLevelsHelper) {
                line("// textureQueryLevels helper");
                line("uint _glsl_textureQueryLevels(Texture2D<float4> tex) {");
                line("    uint w, h, levels;");
                line("    tex.GetDimensions(0, w, h, levels);");
                line("    return levels;");
                line("}");
                line("");
            }
            
            if (ctx.needsBitfieldHelpers) {
                line("// Bitfield helpers");
                line("uint _glsl_bitfieldExtract(uint value, int offset, int bits) {");
                line("    return (value >> offset) & ((1u << bits) - 1u);");
                line("}");
                line("int _glsl_bitfieldExtract(int value, int offset, int bits) {");
                line("    uint mask = (1u << bits) - 1u;");
                line("    uint shifted = uint(value) >> offset;");
                line("    uint extracted = shifted & mask;");
                line("    // Sign extend");
                line("    if ((extracted & (1u << (bits-1))) != 0) extracted |= ~mask;");
                line("    return int(extracted);");
                line("}");
                line("uint _glsl_bitfieldInsert(uint base, uint insert, int offset, int bits) {");
                line("    uint mask = ((1u << bits) - 1u) << offset;");
                line("    return (base & ~mask) | ((insert << offset) & mask);");
                line("}");
                line("");
            }
            
            if (ctx.needsPackingHelpers) {
                line("// Packing helpers");
                line("uint _glsl_packHalf2x16(float2 v) {");
                line("    uint x = f32tof16(v.x);");
                line("    uint y = f32tof16(v.y);");
                line("    return x | (y << 16);");
                line("}");
                line("float2 _glsl_unpackHalf2x16(uint p) {");
                line("    return float2(f16tof32(p & 0xFFFF), f16tof32(p >> 16));");
                line("}");
                line("");
            }
            
            if (ctx.needsOuterProductHelper) {
                line("// outerProduct");
                line("float2x2 _glsl_outerProduct(float2 c, float2 r) {");
                line("    return float2x2(c * r.x, c * r.y);");
                line("}");
                line("float3x3 _glsl_outerProduct(float3 c, float3 r) {");
                line("    return float3x3(c * r.x, c * r.y, c * r.z);");
                line("}");
                line("float4x4 _glsl_outerProduct(float4 c, float4 r) {");
                line("    return float4x4(c * r.x, c * r.y, c * r.z, c * r.w);");
                line("}");
                line("");
            }
        }
        
        // ─── Struct Emission ───
        
        private void emitStruct(StructDecl sd) {
            line("struct " + sd.name());
            line("{");
            indentLevel++;
            for (VariableDecl member : sd.members()) {
                String hlslType = translateType(member.type().name());
                String arrayPart = "";
                if (member.arraySize() != null) {
                    arrayPart = "[" + emitExpr(member.arraySize()) + "]";
                } else if (member.type().isArray()) {
                    arrayPart = member.type().arraySize() != null ? 
                        "[" + emitExpr(member.type().arraySize()) + "]" : "[]";
                }
                line(hlslType + " " + member.name() + arrayPart + ";");
            }
            indentLevel--;
            line("};");
            line("");
        }
        
        // ─── Global Constants ───
        
        private void emitGlobalConstants(TranslationUnit unit) {
            boolean hasConstants = false;
            for (ASTNode node : unit.declarations()) {
                if (node instanceof VariableDecl vd && vd.qualifiers().contains(Qualifier.CONST)) {
                    if (!hasConstants) {
                        line("// ═══ Constants ═══");
                        hasConstants = true;
                    }
                    String hlslType = translateType(vd.type().name());
                    String init = vd.initializer() != null ? " = " + emitExpr(vd.initializer()) : "";
                    line("static const " + hlslType + " " + vd.name() + init + ";");
                }
            }
            if (hasConstants) line("");
        }
        
        // ─── Function Emission ───
        
        private void emitFunction(FunctionDecl fd) {
            // Handle main() specially - transform to entry point
            if (fd.name().equals("main")) {
                emitEntryPoint(fd);
                return;
            }
            
            String retType = translateType(fd.returnType().name());
            StringBuilder sig = new StringBuilder();
            sig.append(retType).append(" ").append(fd.name()).append("(");
            
            for (int i = 0; i < fd.parameters().size(); i++) {
                if (i > 0) sig.append(", ");
                ParameterDecl param = fd.parameters().get(i);
                
                if (param.qualifiers().contains(Qualifier.OUT)) sig.append("out ");
                else if (param.qualifiers().contains(Qualifier.INOUT)) sig.append("inout ");
                
                sig.append(translateType(param.type().name()));
                sig.append(" ").append(param.name());
            }
            sig.append(")");
            
            if (fd.body() == null) {
                line(sig + ";");
            } else {
                line(sig.toString());
                emitBlock(fd.body());
            }
            line("");
        }
        
        private void emitEntryPoint(FunctionDecl fd) {
            String outputType = switch (ctx.config.stage) {
                case VERTEX -> "VS_OUTPUT";
                case FRAGMENT -> "PS_OUTPUT";
                case GEOMETRY -> "GS_OUTPUT";
                case HULL -> "HS_OUTPUT";
                case DOMAIN -> "DS_OUTPUT";
                case COMPUTE -> "void";
            };
            
            String inputType = switch (ctx.config.stage) {
                case VERTEX -> "VS_INPUT";
                case FRAGMENT -> "PS_INPUT";
                case GEOMETRY -> "GS_INPUT";
                case HULL -> "HS_INPUT";
                case DOMAIN -> "DS_INPUT";
                case COMPUTE -> "";
            };
            
            // Function signature
            StringBuilder sig = new StringBuilder();
            sig.append(outputType).append(" ").append(ctx.config.stage.entryPoint).append("(");
            
            if (!inputType.isEmpty()) {
                sig.append(inputType).append(" input");
            }
            
            // Add system value inputs
            if (ctx.config.stage == ShaderStage.FRAGMENT) {
                if (ctx.usedBuiltins.contains("gl_FrontFacing")) {
                    if (!inputType.isEmpty()) sig.append(", ");
                    sig.append("bool frontFacing : SV_IsFrontFace");
                }
                if (ctx.usedBuiltins.contains("gl_SampleID")) {
                    sig.append(", uint sampleID : SV_SampleIndex");
                }
            } else if (ctx.config.stage == ShaderStage.COMPUTE) {
                sig.append("uint3 globalInvocationID : SV_DispatchThreadID");
                sig.append(", uint3 localInvocationID : SV_GroupThreadID");
                sig.append(", uint3 workGroupID : SV_GroupID");
                sig.append(", uint localInvocationIndex : SV_GroupIndex");
            }
            
            sig.append(")");
            line(sig.toString());
            line("{");
            indentLevel++;
            
            // Declare output
            if (!outputType.equals("void")) {
                line(outputType + " output = (" + outputType + ")0;");
                line("");
            }
            
            // Map input variables to local names
            if (ctx.config.stage == ShaderStage.VERTEX) {
                for (IOVariable io : ctx.vertexInputs) {
                    String hlslType = translateType(io.glslType());
                    line(hlslType + " " + io.name() + " = input." + io.name() + ";");
                }
            } else if (!ctx.stageInputs.isEmpty()) {
                for (IOVariable io : ctx.stageInputs) {
                    String hlslType = translateType(io.glslType());
                    line(hlslType + " " + io.name() + " = input." + io.name() + ";");
                }
            }
            
            // Declare output variables
            for (IOVariable io : ctx.stageOutputs) {
                String hlslType = translateType(io.glslType());
                line(hlslType + " " + io.name() + " = (" + hlslType + ")0;");
            }
            
            // Map built-in inputs
            emitBuiltinInputMappings();
            
            if (fd.body() != null) {
                line("");
                line("// ─── Original main() body ───");
                // Emit body statements directly (not the block braces)
                for (Statement stmt : fd.body().statements()) {
                    emitStatement(stmt);
                }
            }
            
            line("");
            // Map outputs
            emitBuiltinOutputMappings();
            for (IOVariable io : ctx.stageOutputs) {
                line("output." + io.name() + " = " + io.name() + ";");
            }
            
            if (!outputType.equals("void")) {
                line("return output;");
            }
            
            indentLevel--;
            line("}");
        }
        
        private void emitBuiltinInputMappings() {
            if (ctx.config.stage == ShaderStage.FRAGMENT) {
                if (ctx.usedBuiltins.contains("gl_FragCoord")) {
                    line("float4 gl_FragCoord = input.position;");
                }
                if (ctx.usedBuiltins.contains("gl_FrontFacing")) {
                    line("bool gl_FrontFacing = frontFacing;");
                }
            } else if (ctx.config.stage == ShaderStage.COMPUTE) {
                if (ctx.usedBuiltins.contains("gl_GlobalInvocationID")) {
                    line("uint3 gl_GlobalInvocationID = globalInvocationID;");
                }
                if (ctx.usedBuiltins.contains("gl_LocalInvocationID")) {
                    line("uint3 gl_LocalInvocationID = localInvocationID;");
                }
                if (ctx.usedBuiltins.contains("gl_WorkGroupID")) {
                    line("uint3 gl_WorkGroupID = workGroupID;");
                }
                if (ctx.usedBuiltins.contains("gl_LocalInvocationIndex")) {
                    line("uint gl_LocalInvocationIndex = localInvocationIndex;");
                }
            }
        }
        
        private void emitBuiltinOutputMappings() {
            if (ctx.config.stage == ShaderStage.VERTEX) {
                if (ctx.usedBuiltins.contains("gl_Position")) {
                    line("output.position = gl_Position;");
                }
            } else if (ctx.config.stage == ShaderStage.FRAGMENT) {
                if (ctx.usedBuiltins.contains("gl_FragDepth")) {
                    line("output.fragDepth = gl_FragDepth;");
                }
            }
        }
        
        // ─── Statement Emission ───
        
        private void emitStatement(Statement stmt) {
            switch (stmt) {
                case BlockStmt bs -> emitBlock(bs);
                case ExprStmt es -> line(emitExpr(es.expr()) + ";");
                case DeclStmt ds -> emitLocalDecl(ds.decl());
                case IfStmt is -> emitIf(is);
                case SwitchStmt ss -> emitSwitch(ss);
                case ForStmt fs -> emitFor(fs);
                case WhileStmt ws -> emitWhile(ws);
                case DoWhileStmt dws -> emitDoWhile(dws);
                case ReturnStmt rs -> {
                    if (rs.value() != null) {
                        line("return " + emitExpr(rs.value()) + ";");
                    } else {
                        line("return;");
                    }
                }
                case BreakStmt ignored -> line("break;");
                case ContinueStmt ignored -> line("continue;");
                case DiscardStmt ignored -> line("discard;");
                case EmptyStmt ignored -> line(";");
            }
        }
        
        private void emitBlock(BlockStmt bs) {
            line("{");
            indentLevel++;
            for (Statement s : bs.statements()) {
                emitStatement(s);
            }
            indentLevel--;
            line("}");
        }
        
        private void emitLocalDecl(VariableDecl vd) {
            String hlslType = translateType(vd.type().name());
            String init = vd.initializer() != null ? " = " + emitExpr(vd.initializer()) : "";
            String arrayPart = "";
            if (vd.arraySize() != null) {
                arrayPart = "[" + emitExpr(vd.arraySize()) + "]";
            }
            line(hlslType + " " + vd.name() + arrayPart + init + ";");
        }
        
        private void emitIf(IfStmt is) {
            line("if (" + emitExpr(is.cond()) + ")");
            if (is.thenBranch() instanceof BlockStmt bs) {
                emitBlock(bs);
            } else {
                indentLevel++;
                emitStatement(is.thenBranch());
                indentLevel--;
            }
            
            if (is.elseBranch() != null) {
                if (is.elseBranch() instanceof IfStmt elseIf) {
                    append("else ");
                    emitIf(elseIf);
                } else if (is.elseBranch() instanceof BlockStmt bs) {
                    line("else");
                    emitBlock(bs);
                } else {
                    line("else");
                    indentLevel++;
                    emitStatement(is.elseBranch());
                    indentLevel--;
                }
            }
        }
        
        private void emitSwitch(SwitchStmt ss) {
            line("switch (" + emitExpr(ss.selector()) + ")");
            line("{");
            for (CaseClause cc : ss.cases()) {
                if (cc.value() != null) {
                    line("case " + emitExpr(cc.value()) + ":");
                } else {
                    line("default:");
                }
                indentLevel++;
                for (Statement s : cc.stmts()) {
                    emitStatement(s);
                }
                indentLevel--;
            }
            line("}");
        }
        
        private void emitFor(ForStmt fs) {
            StringBuilder forLine = new StringBuilder("for (");
            
            if (fs.init() != null) {
                if (fs.init() instanceof DeclStmt ds) {
                    String hlslType = translateType(ds.decl().type().name());
                    String init = ds.decl().initializer() != null ? 
                        " = " + emitExpr(ds.decl().initializer()) : "";
                    forLine.append(hlslType).append(" ").append(ds.decl().name()).append(init);
                } else if (fs.init() instanceof ExprStmt es) {
                    forLine.append(emitExpr(es.expr()));
                }
            }
            forLine.append("; ");
            
            if (fs.cond() != null) {
                forLine.append(emitExpr(fs.cond()));
            }
            forLine.append("; ");
            
            if (fs.incr() != null) {
                forLine.append(emitExpr(fs.incr()));
            }
            forLine.append(")");
            
            line(forLine.toString());
            if (fs.body() instanceof BlockStmt bs) {
                emitBlock(bs);
            } else {
                indentLevel++;
                emitStatement(fs.body());
                indentLevel--;
            }
        }
        
        private void emitWhile(WhileStmt ws) {
            line("while (" + emitExpr(ws.cond()) + ")");
            if (ws.body() instanceof BlockStmt bs) {
                emitBlock(bs);
            } else {
                indentLevel++;
                emitStatement(ws.body());
                indentLevel--;
            }
        }
        
        private void emitDoWhile(DoWhileStmt dws) {
            line("do");
            if (dws.body() instanceof BlockStmt bs) {
                emitBlock(bs);
            } else {
                indentLevel++;
                emitStatement(dws.body());
                indentLevel--;
            }
            line("while (" + emitExpr(dws.cond()) + ");");
        }
        
        // ─── Expression Emission ───
        
        private String emitExpr(Expression expr) {
            return switch (expr) {
                case LiteralExpr le -> emitLiteral(le);
                case IdentExpr ie -> emitIdent(ie);
                case BinaryExpr be -> emitBinary(be);
                case UnaryExpr ue -> emitUnary(ue);
                case TernaryExpr te -> "(" + emitExpr(te.cond()) + " ? " + 
                                       emitExpr(te.thenExpr()) + " : " + emitExpr(te.elseExpr()) + ")";
                case CallExpr ce -> emitCall(ce);
                case ConstructExpr ce -> emitConstruct(ce);
                case MemberExpr me -> emitMember(me);
                case IndexExpr ie -> emitExpr(ie.array()) + "[" + emitExpr(ie.index()) + "]";
                case AssignExpr ae -> emitAssign(ae);
                case SequenceExpr se -> {
                    StringBuilder sb = new StringBuilder("(");
                    for (int i = 0; i < se.exprs().size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(emitExpr(se.exprs().get(i)));
                    }
                    sb.append(")");
                    yield sb.toString();
                }
            };
        }
        
        private String emitLiteral(LiteralExpr le) {
            return switch (le.kind()) {
                case INT -> le.value().toString();
                case UINT -> le.value().toString() + "u";
                case FLOAT -> {
                    String s = le.value().toString();
                    if (!s.contains(".") && !s.contains("e") && !s.contains("E")) {
                        s += ".0";
                    }
                    yield s + "f";
                }
                case DOUBLE -> le.value().toString();
                case BOOL -> le.value().toString();
            };
        }
        
        private String emitIdent(IdentExpr ie) {
            String name = ie.name();
            
            // Check for built-in variable
            BuiltInVariable builtin = BUILTIN_VARS.get(name);
            if (builtin != null) {
                ctx.markBuiltinUsed(name);
                // Some built-ins map to function calls
                if (builtin.hlslName.contains("(")) {
                    return builtin.hlslName;
                }
                return name; // Keep same name, will be mapped in entry point
            }
            
            return name;
        }
        
        private String emitBinary(BinaryExpr be) {
            String left = emitExpr(be.left());
            String right = emitExpr(be.right());
            
            String op = switch (be.op()) {
                case ADD -> "+";
                case SUB -> "-";
                case MUL -> "*";
                case DIV -> "/";
                case MOD -> "%";
                case EQ -> "==";
                case NE -> "!=";
                case LT -> "<";
                case GT -> ">";
                case LE -> "<=";
                case GE -> ">=";
                case AND -> "&&";
                case OR -> "||";
                case XOR -> "^^";
                case BIT_AND -> "&";
                case BIT_OR -> "|";
                case BIT_XOR -> "^";
                case LSHIFT -> "<<";
                case RSHIFT -> ">>";
            };
            
            // Handle matrix * vector multiplication order if needed
            if (be.op() == BinOp.MUL && ctx.config.matrixConvention == MatrixConvention.SWAP_MULTIPLY_ORDER) {
                TypeInfo leftType = ctx.variableTypes.get(extractBaseName(be.left()));
                TypeInfo rightType = ctx.variableTypes.get(extractBaseName(be.right()));
                
                if (leftType != null && leftType.isMatrix() && rightType != null && rightType.isVector()) {
                    // Swap order: M*v becomes v*M in HLSL row-major
                    return "mul(" + right + ", " + left + ")";
                }
                if (leftType != null && leftType.isVector() && rightType != null && rightType.isMatrix()) {
                    return "mul(" + left + ", " + right + ")";
                }
                if ((leftType != null && leftType.isMatrix()) || (rightType != null && rightType.isMatrix())) {
                    return "mul(" + left + ", " + right + ")";
                }
            }
            
            return "(" + left + " " + op + " " + right + ")";
        }
        
        private String extractBaseName(Expression expr) {
            if (expr instanceof IdentExpr ie) return ie.name();
            if (expr instanceof MemberExpr me) return extractBaseName(me.object());
            if (expr instanceof IndexExpr ie) return extractBaseName(ie.array());
            return "";
        }
        
        private String emitUnary(UnaryExpr ue) {
            String operand = emitExpr(ue.operand());
            return switch (ue.op()) {
                case NEG -> "(-" + operand + ")";
                case NOT -> "(!" + operand + ")";
                case BIT_NOT -> "(~" + operand + ")";
                case PRE_INC -> "(++" + operand + ")";
                case PRE_DEC -> "(--" + operand + ")";
                case POST_INC -> "(" + operand + "++)";
                case POST_DEC -> "(" + operand + "--)";
            };
        }
        
        private String emitCall(CallExpr ce) {
            String funcName = ce.name();
            ObjectArrayList<String> args = new ObjectArrayList<>();
            for (Expression arg : ce.args()) {
                args.add(emitExpr(arg));
            }
            
            // Check for complex transformer
            FunctionTransformer transformer = COMPLEX_FUNC_MAP.get(funcName);
            if (transformer != null) {
                return transformer.transform(funcName, args, ctx);
            }
            
            // Check for simple mapping
            String hlslName = SIMPLE_FUNC_MAP.getOrDefault(funcName, funcName);
            
            // Special handling for mod
            if (funcName.equals("mod")) {
                ctx.needsModHelper = true;
            }
            
            return hlslName + "(" + String.join(", ", args) + ")";
        }
        
        private String emitConstruct(ConstructExpr ce) {
            String hlslType = translateType(ce.type());
            ObjectArrayList<String> args = new ObjectArrayList<>();
            for (Expression arg : ce.args()) {
                args.add(emitExpr(arg));
            }
            return hlslType + "(" + String.join(", ", args) + ")";
        }
        
        private String emitMember(MemberExpr me) {
            String obj = emitExpr(me.object());
            String member = me.member();
            
            // Handle swizzle - HLSL uses same syntax
            // But GLSL allows rgba and stpq, HLSL only xyzw and rgba
            if (member.length() <= 4 && member.matches("[stpq]+")) {
                // Convert stpq to xyzw
                member = member.replace('s', 'x').replace('t', 'y')
                               .replace('p', 'z').replace('q', 'w');
            }
            
            return obj + "." + member;
        }
        
        private String emitAssign(AssignExpr ae) {
            String target = emitExpr(ae.target());
            String value = emitExpr(ae.value());
            
            String op = switch (ae.op()) {
                case ASSIGN -> "=";
                case ADD_ASSIGN -> "+=";
                case SUB_ASSIGN -> "-=";
                case MUL_ASSIGN -> "*=";
                case DIV_ASSIGN -> "/=";
                case MOD_ASSIGN -> "%=";
                case AND_ASSIGN -> "&=";
                case OR_ASSIGN -> "|=";
                case XOR_ASSIGN -> "^=";
                case LSHIFT_ASSIGN -> "<<=";
                case RSHIFT_ASSIGN -> ">>=";
            };
            
            return "(" + target + " " + op + " " + value + ")";
        }
        
        // ─── Type Translation ───
        
        private String translateType(String glslType) {
            TypeInfo info = TYPE_MAP.get(glslType);
            if (info != null) {
                return info.hlslName;
            }
            // Assume it's a user-defined struct
            return glslType;
        }
        
        // ─── Output Helpers ───
        
        private void line(String s) {
            for (int i = 0; i < indentLevel; i++) output.append(INDENT);
            output.append(s).append("\n");
        }
        
        private void append(String s) {
            for (int i = 0; i < indentLevel; i++) output.append(INDENT);
            output.append(s);
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 13: PUBLIC API
    // ════════════════════════════════════════════════════════════════════════════
    
    /** Translation result containing HLSL code and metadata */
    public record TranslationResult(
        String hlslCode,
        boolean success,
        ObjectArrayList<String> errors,
        ObjectArrayList<String> warnings,
        TranslationContext context
    ) {
        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
    }
    
    // Private constructor - use static methods
    private HLSLCallMapper() {}
    
    /**
     * Translate GLSL source code to HLSL.
     * 
     * @param glslSource The GLSL source code
     * @param config Translation configuration
     * @return Translation result with HLSL code or errors
     */
    public static TranslationResult translate(String glslSource, Config config) {
        TranslationContext ctx = new TranslationContext(config);
        
        try {
            // Tokenize
            Lexer lexer = new Lexer(glslSource);
            ObjectArrayList<Token> tokens = lexer.tokenize(false); // Exclude trivia
            
            for (String err : lexer.getErrors()) {
                ctx.error(err);
            }
            
            if (ctx.hasErrors() && config.enableStrictMode) {
                return new TranslationResult("", false, ctx.getErrors(), ctx.getWarnings(), ctx);
            }
            
            // Parse
            Parser parser = new Parser(tokens);
            TranslationUnit ast = parser.parse();
            
            for (String err : parser.getErrors()) {
                ctx.error(err);
            }
            
            if (ctx.hasErrors() && config.enableStrictMode) {
                return new TranslationResult("", false, ctx.getErrors(), ctx.getWarnings(), ctx);
            }
            
            // Generate
            CodeGenerator generator = new CodeGenerator(ctx);
            String hlsl = generator.generate(ast);
            
            return new TranslationResult(hlsl, !ctx.hasErrors(), ctx.getErrors(), ctx.getWarnings(), ctx);
            
        } catch (Exception e) {
            ctx.error("Translation failed: " + e.getMessage());
            LOGGER.error("GLSL→HLSL translation failed", e);
            return new TranslationResult("", false, ctx.getErrors(), ctx.getWarnings(), ctx);
        }
    }
    
    /**
     * Translate a vertex shader with default configuration.
     */
    public static TranslationResult translateVertex(String glslSource) {
        return translate(glslSource, Config.vertex());
    }
    
    /**
     * Translate a fragment/pixel shader with default configuration.
     */
    public static TranslationResult translateFragment(String glslSource) {
        return translate(glslSource, Config.fragment());
    }
    
    /**
     * Translate a compute shader with default configuration.
     */
    public static TranslationResult translateCompute(String glslSource) {
        return translate(glslSource, Config.compute());
    }
    
    /**
     * Quick translation for simple shaders - returns HLSL or throws on error.
     */
    public static String translateOrThrow(String glslSource, Config config) {
        TranslationResult result = translate(glslSource, config);
        if (!result.success()) {
            throw new RuntimeException("GLSL translation failed:\n" + String.join("\n", result.errors()));
        }
        return result.hlslCode();
    }
    
    /**
     * Get resource binding information from a translation result.
     * Useful for setting up DirectX resource binding.
     */
    public static ObjectArrayList<ResourceBinding> getResourceBindings(TranslationContext ctx) {
        ObjectArrayList<ResourceBinding> bindings = new ObjectArrayList<>();
        
        for (SamplerBinding sb : ctx.samplerBindings.values()) {
            bindings.add(new ResourceBinding(
                sb.glslName(),
                sb.samplerName() != null ? ResourceBinding.Type.TEXTURE : ResourceBinding.Type.UAV,
                sb.textureSlot(),
                sb.samplerSlot()
            ));
        }
        
        return bindings;
    }
    
    public record ResourceBinding(
        String name,
        Type type,
        int slot,
        int samplerSlot
    ) {
        public enum Type { TEXTURE, SAMPLER, CBUFFER, UAV }
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// PART 4: ADVANCED FEATURES
// Preprocessor, Validation, Optimization Passes, Geometry/Tessellation Support
// ════════════════════════════════════════════════════════════════════════════════

package net.opengl.glsl;

import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.fastutil.ints.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.*;
import java.util.function.*;

/**
 * Extended GLSL processing utilities including preprocessor, validation, and optimization.
 * Companion class to HLSLCallMapper for complete shader translation pipeline.
 */
public final class GLSLProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GLSLProcessor.class);
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 1: GLSL PREPROCESSOR
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Full GLSL preprocessor supporting:
     * - #define / #undef (with function-like macros)
     * - #if / #ifdef / #ifndef / #elif / #else / #endif
     * - #include (with custom resolver)
     * - #pragma
     * - #extension
     * - #version
     * - #line
     * - #error / #warning
     * - __LINE__, __FILE__, __VERSION__, GL_* predefined macros
     */
    public static final class Preprocessor {
        
        /** Callback for resolving #include directives */
        @FunctionalInterface
        public interface IncludeResolver {
            @Nullable String resolve(String path, String currentFile);
        }
        
        /** Preprocessor configuration */
        public record Config(
            int glslVersion,
            boolean isES,
            Object2ObjectMap<String, String> predefinedMacros,
            ObjectSet<String> enabledExtensions,
            IncludeResolver includeResolver,
            int maxIncludeDepth,
            boolean keepLineDirectives,
            boolean stripComments
        ) {
            public static Builder builder() { return new Builder(); }
            
            public static final class Builder {
                private int glslVersion = 450;
                private boolean isES = false;
                private Object2ObjectOpenHashMap<String, String> predefinedMacros = new Object2ObjectOpenHashMap<>();
                private ObjectOpenHashSet<String> enabledExtensions = new ObjectOpenHashSet<>();
                private IncludeResolver includeResolver = (path, current) -> null;
                private int maxIncludeDepth = 32;
                private boolean keepLineDirectives = true;
                private boolean stripComments = true;
                
                public Builder glslVersion(int v) { this.glslVersion = v; return this; }
                public Builder esProfile(boolean es) { this.isES = es; return this; }
                public Builder define(String name, String value) { predefinedMacros.put(name, value); return this; }
                public Builder define(String name) { return define(name, "1"); }
                public Builder extension(String ext) { enabledExtensions.add(ext); return this; }
                public Builder includeResolver(IncludeResolver r) { this.includeResolver = r; return this; }
                public Builder maxIncludeDepth(int d) { this.maxIncludeDepth = d; return this; }
                public Builder keepLineDirectives(boolean k) { this.keepLineDirectives = k; return this; }
                public Builder stripComments(boolean s) { this.stripComments = s; return this; }
                
                public Config build() {
                    return new Config(glslVersion, isES, predefinedMacros, enabledExtensions,
                                     includeResolver, maxIncludeDepth, keepLineDirectives, stripComments);
                }
            }
        }
        
        /** Result of preprocessing */
        public record Result(
            String processedSource,
            int detectedVersion,
            boolean isES,
            ObjectArrayList<String> errors,
            ObjectArrayList<String> warnings,
            ObjectArrayList<String> enabledExtensions,
            Object2ObjectMap<String, MacroDef> finalMacros
        ) {
            public boolean hasErrors() { return !errors.isEmpty(); }
        }
        
        /** Macro definition */
        public record MacroDef(
            String name,
            @Nullable ObjectArrayList<String> parameters, // null = object-like macro
            String body,
            String sourceFile,
            int sourceLine
        ) {
            public boolean isFunctionLike() { return parameters != null; }
        }
        
        // Internal state
        private final Config config;
        private final Object2ObjectOpenHashMap<String, MacroDef> macros = new Object2ObjectOpenHashMap<>();
        private final ObjectArrayList<String> errors = new ObjectArrayList<>();
        private final ObjectArrayList<String> warnings = new ObjectArrayList<>();
        private final ObjectArrayList<String> extensions = new ObjectArrayList<>();
        private final IntArrayList conditionalStack = new IntArrayList(); // 0=false, 1=true, 2=else-seen
        private int detectedVersion = 110;
        private boolean detectedES = false;
        private int currentLine = 1;
        private String currentFile = "<main>";
        private int includeDepth = 0;
        
        // Conditional state constants
        private static final int COND_FALSE = 0;
        private static final int COND_TRUE = 1;
        private static final int COND_ELSE_FALSE = 2;
        private static final int COND_ELSE_TRUE = 3;
        
        public Preprocessor(Config config) {
            this.config = config;
            initPredefinedMacros();
        }
        
        private void initPredefinedMacros() {
            // Standard predefined macros
            macros.put("__VERSION__", new MacroDef("__VERSION__", null, 
                String.valueOf(config.glslVersion), "<builtin>", 0));
            macros.put("GL_core_profile", new MacroDef("GL_core_profile", null, "1", "<builtin>", 0));
            
            if (config.isES) {
                macros.put("GL_ES", new MacroDef("GL_ES", null, "1", "<builtin>", 0));
                macros.put("GL_FRAGMENT_PRECISION_HIGH", new MacroDef(
                    "GL_FRAGMENT_PRECISION_HIGH", null, "1", "<builtin>", 0));
            }
            
            // User-defined macros
            for (var entry : config.predefinedMacros.object2ObjectEntrySet()) {
                macros.put(entry.getKey(), new MacroDef(entry.getKey(), null, 
                    entry.getValue(), "<predefined>", 0));
            }
            
            // Extension macros
            for (String ext : config.enabledExtensions) {
                macros.put(ext, new MacroDef(ext, null, "1", "<extension>", 0));
            }
        }
        
        /**
         * Preprocess GLSL source code.
         */
        public Result process(String source) {
            StringBuilder output = new StringBuilder(source.length());
            String[] lines = source.split("\n", -1);
            
            for (int i = 0; i < lines.length; i++) {
                currentLine = i + 1;
                String line = lines[i];
                String trimmed = line.trim();
                
                // Handle preprocessor directives
                if (trimmed.startsWith("#")) {
                    processDirective(trimmed, output);
                } else if (isOutputEnabled()) {
                    // Process macro expansions in regular code
                    String processed = expandMacros(line);
                    output.append(processed).append("\n");
                } else {
                    // Keep blank line for line number tracking
                    output.append("\n");
                }
            }
            
            // Check for unclosed conditionals
            if (!conditionalStack.isEmpty()) {
                error("Unterminated #if/#ifdef/#ifndef");
            }
            
            return new Result(
                output.toString(),
                detectedVersion,
                detectedES,
                errors,
                warnings,
                extensions,
                new Object2ObjectOpenHashMap<>(macros)
            );
        }
        
        private boolean isOutputEnabled() {
            if (conditionalStack.isEmpty()) return true;
            int top = conditionalStack.topInt();
            return top == COND_TRUE || top == COND_ELSE_TRUE;
        }
        
        private void processDirective(String line, StringBuilder output) {
            // Remove leading # and whitespace
            String directive = line.substring(1).trim();
            
            // Handle line continuation
            while (directive.endsWith("\\")) {
                // Would need to read next line - simplified here
                directive = directive.substring(0, directive.length() - 1);
            }
            
            // Parse directive name
            int spaceIdx = directive.indexOf(' ');
            String name = spaceIdx > 0 ? directive.substring(0, spaceIdx) : directive;
            String args = spaceIdx > 0 ? directive.substring(spaceIdx + 1).trim() : "";
            
            // Process based on directive type
            switch (name) {
                case "version" -> processVersion(args, output);
                case "extension" -> processExtension(args, output);
                case "define" -> processDefine(args);
                case "undef" -> processUndef(args);
                case "if" -> processIf(args);
                case "ifdef" -> processIfdef(args, false);
                case "ifndef" -> processIfdef(args, true);
                case "elif" -> processElif(args);
                case "else" -> processElse();
                case "endif" -> processEndif();
                case "include" -> processInclude(args, output);
                case "pragma" -> processPragma(args, output);
                case "line" -> processLine(args, output);
                case "error" -> {
                    if (isOutputEnabled()) error("#error: " + args);
                }
                case "warning" -> {
                    if (isOutputEnabled()) warning("#warning: " + args);
                }
                default -> {
                    if (isOutputEnabled()) {
                        warning("Unknown preprocessor directive: #" + name);
                    }
                }
            }
        }
        
        // ─── Directive Processors ───
        
        private void processVersion(String args, StringBuilder output) {
            // #version 450 core
            // #version 300 es
            Pattern p = Pattern.compile("(\\d+)\\s*(core|compatibility|es)?");
            Matcher m = p.matcher(args);
            if (m.matches()) {
                detectedVersion = Integer.parseInt(m.group(1));
                String profile = m.group(2);
                detectedES = "es".equalsIgnoreCase(profile);
                
                // Update __VERSION__ macro
                macros.put("__VERSION__", new MacroDef("__VERSION__", null, 
                    String.valueOf(detectedVersion), currentFile, currentLine));
                
                if (detectedES) {
                    macros.put("GL_ES", new MacroDef("GL_ES", null, "1", currentFile, currentLine));
                }
            } else {
                error("Invalid #version directive: " + args);
            }
            
            // Keep version directive in output
            output.append("#version ").append(args).append("\n");
        }
        
        private void processExtension(String args, StringBuilder output) {
            // #extension GL_ARB_something : enable
            Pattern p = Pattern.compile("(\\w+)\\s*:\\s*(require|enable|warn|disable)");
            Matcher m = p.matcher(args);
            if (m.matches()) {
                String extName = m.group(1);
                String behavior = m.group(2);
                
                if ("enable".equals(behavior) || "require".equals(behavior)) {
                    extensions.add(extName);
                    macros.put(extName, new MacroDef(extName, null, "1", currentFile, currentLine));
                } else if ("disable".equals(behavior)) {
                    macros.remove(extName);
                }
            } else if ("all".equals(args.split(":")[0].trim())) {
                // #extension all : warn/disable
            } else {
                error("Invalid #extension directive: " + args);
            }
            
            // Keep extension directive (might be needed by subsequent stages)
            output.append("#extension ").append(args).append("\n");
        }
        
        private void processDefine(String args) {
            if (!isOutputEnabled()) return;
            
            // Parse macro name and optional parameters
            // #define NAME value
            // #define NAME(a, b) expression
            
            Pattern funcMacro = Pattern.compile("(\\w+)\\s*\\(([^)]*)\\)\\s*(.*)");
            Pattern objMacro = Pattern.compile("(\\w+)\\s*(.*)");
            
            Matcher fm = funcMacro.matcher(args);
            if (fm.matches()) {
                String name = fm.group(1);
                String params = fm.group(2);
                String body = fm.group(3);
                
                ObjectArrayList<String> paramList = new ObjectArrayList<>();
                if (!params.isBlank()) {
                    for (String p : params.split(",")) {
                        paramList.add(p.trim());
                    }
                }
                
                macros.put(name, new MacroDef(name, paramList, body, currentFile, currentLine));
                return;
            }
            
            Matcher om = objMacro.matcher(args);
            if (om.matches()) {
                String name = om.group(1);
                String body = om.group(2);
                macros.put(name, new MacroDef(name, null, body, currentFile, currentLine));
            } else {
                error("Invalid #define directive: " + args);
            }
        }
        
        private void processUndef(String args) {
            if (!isOutputEnabled()) return;
            String name = args.trim();
            macros.remove(name);
        }
        
        private void processIf(String args) {
            if (!isOutputEnabled()) {
                // Parent conditional is false, push false
                conditionalStack.push(COND_FALSE);
                return;
            }
            
            boolean result = evaluateExpression(args);
            conditionalStack.push(result ? COND_TRUE : COND_FALSE);
        }
        
        private void processIfdef(String args, boolean negate) {
            if (!isOutputEnabled()) {
                conditionalStack.push(COND_FALSE);
                return;
            }
            
            String name = args.trim();
            boolean defined = macros.containsKey(name);
            boolean result = negate ? !defined : defined;
            conditionalStack.push(result ? COND_TRUE : COND_FALSE);
        }
        
        private void processElif(String args) {
            if (conditionalStack.isEmpty()) {
                error("#elif without matching #if");
                return;
            }
            
            int current = conditionalStack.popInt();
            
            if (current == COND_ELSE_FALSE || current == COND_ELSE_TRUE) {
                error("#elif after #else");
                conditionalStack.push(current);
                return;
            }
            
            // If we already found a true branch, this and all subsequent are false
            if (current == COND_TRUE) {
                conditionalStack.push(COND_ELSE_FALSE); // Mark that we've had a true
                return;
            }
            
            // Previous was false, evaluate this one
            boolean result = evaluateExpression(args);
            conditionalStack.push(result ? COND_TRUE : COND_FALSE);
        }
        
        private void processElse() {
            if (conditionalStack.isEmpty()) {
                error("#else without matching #if");
                return;
            }
            
            int current = conditionalStack.popInt();
            
            if (current == COND_ELSE_FALSE || current == COND_ELSE_TRUE) {
                error("Multiple #else clauses");
                conditionalStack.push(current);
                return;
            }
            
            // Flip the condition
            if (current == COND_TRUE) {
                conditionalStack.push(COND_ELSE_FALSE);
            } else {
                conditionalStack.push(COND_ELSE_TRUE);
            }
        }
        
        private void processEndif() {
            if (conditionalStack.isEmpty()) {
                error("#endif without matching #if");
                return;
            }
            conditionalStack.popInt();
        }
        
        private void processInclude(String args, StringBuilder output) {
            if (!isOutputEnabled()) return;
            
            // #include "path" or #include <path>
            Pattern p = Pattern.compile("[\"<]([^\"'>]+)[\">]");
            Matcher m = p.matcher(args);
            
            if (!m.find()) {
                error("Invalid #include directive: " + args);
                return;
            }
            
            String path = m.group(1);
            
            if (includeDepth >= config.maxIncludeDepth) {
                error("Maximum include depth exceeded: " + path);
                return;
            }
            
            String content = config.includeResolver.resolve(path, currentFile);
            if (content == null) {
                error("Cannot resolve include: " + path);
                return;
            }
            
            // Process included content
            String savedFile = currentFile;
            int savedLine = currentLine;
            currentFile = path;
            includeDepth++;
            
            if (config.keepLineDirectives) {
                output.append("#line 1 \"").append(path).append("\"\n");
            }
            
            String[] lines = content.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                currentLine = i + 1;
                String line = lines[i];
                String trimmed = line.trim();
                
                if (trimmed.startsWith("#")) {
                    processDirective(trimmed, output);
                } else if (isOutputEnabled()) {
                    output.append(expandMacros(line)).append("\n");
                } else {
                    output.append("\n");
                }
            }
            
            includeDepth--;
            currentFile = savedFile;
            currentLine = savedLine;
            
            if (config.keepLineDirectives) {
                output.append("#line ").append(currentLine + 1).append(" \"").append(savedFile).append("\"\n");
            }
        }
        
        private void processPragma(String args, StringBuilder output) {
            if (!isOutputEnabled()) return;
            
            // Handle specific pragmas
            if (args.startsWith("optimize")) {
                // #pragma optimize(on/off)
            } else if (args.startsWith("debug")) {
                // #pragma debug(on/off)
            } else if (args.startsWith("STDGL")) {
                // Standard pragmas
            }
            
            // Keep pragma in output
            output.append("#pragma ").append(args).append("\n");
        }
        
        private void processLine(String args, StringBuilder output) {
            // #line linenum "filename"
            Pattern p = Pattern.compile("(\\d+)(?:\\s+\"([^\"]+)\")?");
            Matcher m = p.matcher(args);
            
            if (m.matches()) {
                currentLine = Integer.parseInt(m.group(1)) - 1;
                if (m.group(2) != null) {
                    currentFile = m.group(2);
                }
            }
            
            if (config.keepLineDirectives) {
                output.append("#line ").append(args).append("\n");
            }
        }
        
        // ─── Expression Evaluation ───
        
        private boolean evaluateExpression(String expr) {
            // Expand macros first
            String expanded = expandMacros(expr);
            
            // Handle defined() operator
            expanded = expandDefined(expanded);
            
            // Replace remaining identifiers with 0
            expanded = expanded.replaceAll("\\b[a-zA-Z_]\\w*\\b", "0");
            
            // Simple expression evaluator
            try {
                return evaluateConstExpr(expanded) != 0;
            } catch (Exception e) {
                error("Cannot evaluate expression: " + expr);
                return false;
            }
        }
        
        private String expandDefined(String expr) {
            // defined(X) or defined X
            Pattern p1 = Pattern.compile("defined\\s*\\(\\s*(\\w+)\\s*\\)");
            Pattern p2 = Pattern.compile("defined\\s+(\\w+)");
            
            Matcher m1 = p1.matcher(expr);
            StringBuffer sb = new StringBuffer();
            while (m1.find()) {
                String name = m1.group(1);
                m1.appendReplacement(sb, macros.containsKey(name) ? "1" : "0");
            }
            m1.appendTail(sb);
            
            String result = sb.toString();
            
            Matcher m2 = p2.matcher(result);
            sb = new StringBuffer();
            while (m2.find()) {
                String name = m2.group(1);
                m2.appendReplacement(sb, macros.containsKey(name) ? "1" : "0");
            }
            m2.appendTail(sb);
            
            return sb.toString();
        }
        
        private long evaluateConstExpr(String expr) {
            // Simple recursive descent for preprocessor expressions
            // Supports: + - * / % << >> < > <= >= == != & ^ | && || ! ~ ( )
            return new ConstExprEvaluator(expr).parse();
        }
        
        // ─── Macro Expansion ───
        
        private String expandMacros(String text) {
            return expandMacros(text, new ObjectOpenHashSet<>(), 0);
        }
        
        private String expandMacros(String text, ObjectOpenHashSet<String> expanding, int depth) {
            if (depth > 256) {
                error("Macro expansion depth limit exceeded");
                return text;
            }
            
            StringBuilder result = new StringBuilder();
            int i = 0;
            
            while (i < text.length()) {
                // Skip string literals
                if (text.charAt(i) == '"') {
                    int end = text.indexOf('"', i + 1);
                    if (end < 0) end = text.length();
                    result.append(text, i, end + 1);
                    i = end + 1;
                    continue;
                }
                
                // Check for identifier
                if (Character.isJavaIdentifierStart(text.charAt(i))) {
                    int start = i;
                    while (i < text.length() && Character.isJavaIdentifierPart(text.charAt(i))) {
                        i++;
                    }
                    String ident = text.substring(start, i);
                    
                    // Special macros
                    if (ident.equals("__LINE__")) {
                        result.append(currentLine);
                        continue;
                    }
                    if (ident.equals("__FILE__")) {
                        result.append('"').append(currentFile).append('"');
                        continue;
                    }
                    
                    MacroDef macro = macros.get(ident);
                    if (macro != null && !expanding.contains(ident)) {
                        String expanded;
                        if (macro.isFunctionLike()) {
                            // Parse arguments
                            if (i < text.length() && text.charAt(i) == '(') {
                                int argStart = i + 1;
                                int parenDepth = 1;
                                i++;
                                while (i < text.length() && parenDepth > 0) {
                                    if (text.charAt(i) == '(') parenDepth++;
                                    else if (text.charAt(i) == ')') parenDepth--;
                                    i++;
                                }
                                String argString = text.substring(argStart, i - 1);
                                ObjectArrayList<String> args = parseArguments(argString);
                                expanded = expandFunctionMacro(macro, args);
                            } else {
                                // Function-like macro without args - not expanded
                                result.append(ident);
                                continue;
                            }
                        } else {
                            expanded = macro.body();
                        }
                        
                        // Recursive expansion
                        expanding.add(ident);
                        expanded = expandMacros(expanded, expanding, depth + 1);
                        expanding.remove(ident);
                        
                        result.append(expanded);
                    } else {
                        result.append(ident);
                    }
                } else {
                    result.append(text.charAt(i));
                    i++;
                }
            }
            
            return result.toString();
        }
        
        private ObjectArrayList<String> parseArguments(String argString) {
            ObjectArrayList<String> args = new ObjectArrayList<>();
            int depth = 0;
            int start = 0;
            
            for (int i = 0; i <= argString.length(); i++) {
                char c = i < argString.length() ? argString.charAt(i) : ',';
                
                if (c == '(' || c == '[' || c == '{') depth++;
                else if (c == ')' || c == ']' || c == '}') depth--;
                else if (c == ',' && depth == 0) {
                    args.add(argString.substring(start, i).trim());
                    start = i + 1;
                }
            }
            
            return args;
        }
        
        private String expandFunctionMacro(MacroDef macro, ObjectArrayList<String> args) {
            if (macro.parameters().size() != args.size()) {
                error("Macro " + macro.name() + " expects " + macro.parameters().size() + 
                      " arguments, got " + args.size());
                return "";
            }
            
            String body = macro.body();
            
            // Handle # (stringification) and ## (concatenation)
            for (int i = 0; i < macro.parameters().size(); i++) {
                String param = macro.parameters().get(i);
                String arg = args.get(i);
                
                // Stringification: #param -> "arg"
                body = body.replaceAll("#\\s*" + param + "\\b", "\"" + arg + "\"");
                
                // Token pasting: handled after substitution
            }
            
            // Parameter substitution
            for (int i = 0; i < macro.parameters().size(); i++) {
                String param = macro.parameters().get(i);
                String arg = args.get(i);
                body = body.replaceAll("\\b" + param + "\\b", arg);
            }
            
            // Token pasting: ## removes surrounding whitespace and concatenates
            body = body.replaceAll("\\s*##\\s*", "");
            
            return body;
        }
        
        private void error(String msg) {
            errors.add(currentFile + ":" + currentLine + ": " + msg);
        }
        
        private void warning(String msg) {
            warnings.add(currentFile + ":" + currentLine + ": " + msg);
        }
    }
    
    // ─── Constant Expression Evaluator ───
    
    private static final class ConstExprEvaluator {
        private final String expr;
        private int pos = 0;
        
        ConstExprEvaluator(String expr) {
            this.expr = expr.trim();
        }
        
        long parse() {
            long result = parseOr();
            skipWhitespace();
            return result;
        }
        
        private long parseOr() {
            long left = parseAnd();
            while (match("||")) {
                long right = parseAnd();
                left = (left != 0 || right != 0) ? 1 : 0;
            }
            return left;
        }
        
        private long parseAnd() {
            long left = parseBitOr();
            while (match("&&")) {
                long right = parseBitOr();
                left = (left != 0 && right != 0) ? 1 : 0;
            }
            return left;
        }
        
        private long parseBitOr() {
            long left = parseBitXor();
            while (match("|") && !check("|")) {
                left |= parseBitXor();
            }
            return left;
        }
        
        private long parseBitXor() {
            long left = parseBitAnd();
            while (match("^")) {
                left ^= parseBitAnd();
            }
            return left;
        }
        
        private long parseBitAnd() {
            long left = parseEquality();
            while (match("&") && !check("&")) {
                left &= parseEquality();
            }
            return left;
        }
        
        private long parseEquality() {
            long left = parseRelational();
            while (true) {
                if (match("==")) left = left == parseRelational() ? 1 : 0;
                else if (match("!=")) left = left != parseRelational() ? 1 : 0;
                else break;
            }
            return left;
        }
        
        private long parseRelational() {
            long left = parseShift();
            while (true) {
                if (match("<=")) left = left <= parseShift() ? 1 : 0;
                else if (match(">=")) left = left >= parseShift() ? 1 : 0;
                else if (match("<")) left = left < parseShift() ? 1 : 0;
                else if (match(">")) left = left > parseShift() ? 1 : 0;
                else break;
            }
            return left;
        }
        
        private long parseShift() {
            long left = parseAdditive();
            while (true) {
                if (match("<<")) left <<= parseAdditive();
                else if (match(">>")) left >>= parseAdditive();
                else break;
            }
            return left;
        }
        
        private long parseAdditive() {
            long left = parseMultiplicative();
            while (true) {
                if (match("+")) left += parseMultiplicative();
                else if (match("-")) left -= parseMultiplicative();
                else break;
            }
            return left;
        }
        
        private long parseMultiplicative() {
            long left = parseUnary();
            while (true) {
                if (match("*")) left *= parseUnary();
                else if (match("/")) {
                    long right = parseUnary();
                    left = right != 0 ? left / right : 0;
                }
                else if (match("%")) {
                    long right = parseUnary();
                    left = right != 0 ? left % right : 0;
                }
                else break;
            }
            return left;
        }
        
        private long parseUnary() {
            if (match("!")) return parseUnary() == 0 ? 1 : 0;
            if (match("~")) return ~parseUnary();
            if (match("-")) return -parseUnary();
            if (match("+")) return parseUnary();
            return parsePrimary();
        }
        
        private long parsePrimary() {
            skipWhitespace();
            
            if (match("(")) {
                long result = parseOr();
                match(")");
                return result;
            }
            
            // Parse number
            int start = pos;
            boolean hex = false;
            
            if (pos + 1 < expr.length() && expr.charAt(pos) == '0' && 
                (expr.charAt(pos + 1) == 'x' || expr.charAt(pos + 1) == 'X')) {
                hex = true;
                pos += 2;
            }
            
            while (pos < expr.length()) {
                char c = expr.charAt(pos);
                if (hex) {
                    if (!Character.isDigit(c) && !(c >= 'a' && c <= 'f') && !(c >= 'A' && c <= 'F')) break;
                } else {
                    if (!Character.isDigit(c)) break;
                }
                pos++;
            }
            
            // Skip suffix (u, U, l, L)
            while (pos < expr.length() && "uUlL".indexOf(expr.charAt(pos)) >= 0) pos++;
            
            String numStr = expr.substring(start, pos);
            if (numStr.isEmpty()) return 0;
            
            try {
                if (hex) {
                    return Long.parseLong(numStr.substring(2).replaceAll("[uUlL]", ""), 16);
                }
                return Long.parseLong(numStr.replaceAll("[uUlL]", ""));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        
        private void skipWhitespace() {
            while (pos < expr.length() && Character.isWhitespace(expr.charAt(pos))) pos++;
        }
        
        private boolean check(String s) {
            skipWhitespace();
            return expr.startsWith(s, pos);
        }
        
        private boolean match(String s) {
            skipWhitespace();
            if (expr.startsWith(s, pos)) {
                pos += s.length();
                return true;
            }
            return false;
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 2: SHADER VALIDATOR
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Validates GLSL AST for semantic correctness before translation.
     */
    public static final class Validator {
        
        public record ValidationResult(
            boolean valid,
            ObjectArrayList<ValidationError> errors,
            ObjectArrayList<ValidationWarning> warnings
        ) {
            public boolean hasErrors() { return !errors.isEmpty(); }
        }
        
        public record ValidationError(
            String message,
            @Nullable String location,
            ErrorKind kind
        ) {}
        
        public record ValidationWarning(
            String message,
            @Nullable String location,
            WarningKind kind
        ) {}
        
        public enum ErrorKind {
            TYPE_MISMATCH,
            UNDECLARED_IDENTIFIER,
            REDEFINITION,
            INVALID_OPERATION,
            UNSUPPORTED_FEATURE,
            SEMANTIC_ERROR
        }
        
        public enum WarningKind {
            UNUSED_VARIABLE,
            IMPLICIT_CONVERSION,
            DEPRECATED_FEATURE,
            PRECISION_LOSS,
            PERFORMANCE
        }
        
        private final ObjectArrayList<ValidationError> errors = new ObjectArrayList<>();
        private final ObjectArrayList<ValidationWarning> warnings = new ObjectArrayList<>();
        private final Object2ObjectOpenHashMap<String, TypeInfo> symbolTable = new Object2ObjectOpenHashMap<>();
        private final ObjectArrayList<Object2ObjectOpenHashMap<String, TypeInfo>> scopeStack = new ObjectArrayList<>();
        private final HLSLCallMapper.ShaderStage stage;
        
        public Validator(HLSLCallMapper.ShaderStage stage) {
            this.stage = stage;
            initBuiltins();
        }
        
        private void initBuiltins() {
            // Add built-in functions
            for (String func : HLSLCallMapper.SIMPLE_FUNC_MAP.keySet()) {
                symbolTable.put(func, HLSLCallMapper.TypeInfo.function(func));
            }
            for (String func : HLSLCallMapper.COMPLEX_FUNC_MAP.keySet()) {
                symbolTable.put(func, HLSLCallMapper.TypeInfo.function(func));
            }
            
            // Add built-in variables for this stage
            for (var entry : HLSLCallMapper.BUILTIN_VARS.entrySet()) {
                HLSLCallMapper.BuiltInVariable builtin = entry.getValue();
                if (builtin.validStages.contains(stage)) {
                    HLSLCallMapper.TypeInfo type = HLSLCallMapper.TYPE_MAP.get(builtin.hlslType);
                    if (type != null) {
                        symbolTable.put(builtin.glslName, type);
                    }
                }
            }
        }
        
        public ValidationResult validate(HLSLCallMapper.TranslationUnit unit) {
            pushScope();
            
            for (HLSLCallMapper.ASTNode node : unit.declarations()) {
                validateDeclaration(node);
            }
            
            popScope();
            
            return new ValidationResult(errors.isEmpty(), errors, warnings);
        }
        
        private void validateDeclaration(HLSLCallMapper.ASTNode node) {
            switch (node) {
                case HLSLCallMapper.VariableDecl vd -> validateVariableDecl(vd);
                case HLSLCallMapper.FunctionDecl fd -> validateFunctionDecl(fd);
                case HLSLCallMapper.StructDecl sd -> validateStructDecl(sd);
                case HLSLCallMapper.InterfaceBlockDecl ibd -> validateInterfaceBlock(ibd);
                default -> {}
            }
        }
        
        private void validateVariableDecl(HLSLCallMapper.VariableDecl vd) {
            // Check type exists
            HLSLCallMapper.TypeInfo type = HLSLCallMapper.TYPE_MAP.get(vd.type().name());
            if (type == null && !symbolTable.containsKey(vd.type().name())) {
                error("Unknown type: " + vd.type().name(), vd.name(), ErrorKind.UNDECLARED_IDENTIFIER);
            }
            
            // Check for redefinition in current scope
            if (currentScope().containsKey(vd.name())) {
                error("Redefinition of '" + vd.name() + "'", vd.name(), ErrorKind.REDEFINITION);
            }
            
            // Add to symbol table
            if (type != null) {
                currentScope().put(vd.name(), type);
            }
            
            // Validate initializer
            if (vd.initializer() != null) {
                HLSLCallMapper.TypeInfo initType = inferType(vd.initializer());
                if (type != null && initType != null && !isAssignable(type, initType)) {
                    error("Cannot initialize '" + vd.name() + "' of type " + type.glslName + 
                          " with expression of type " + initType.glslName, vd.name(), ErrorKind.TYPE_MISMATCH);
                }
            }
            
            // Check qualifiers
            validateQualifiers(vd);
        }
        
        private void validateFunctionDecl(HLSLCallMapper.FunctionDecl fd) {
            pushScope();
            
            // Add parameters to scope
            for (HLSLCallMapper.ParameterDecl param : fd.parameters()) {
                HLSLCallMapper.TypeInfo type = HLSLCallMapper.TYPE_MAP.get(param.type().name());
                if (type != null) {
                    currentScope().put(param.name(), type);
                }
            }
            
            // Validate body
            if (fd.body() != null) {
                validateStatement(fd.body());
            }
            
            // Special validation for main()
            if (fd.name().equals("main")) {
                validateMainFunction(fd);
            }
            
            popScope();
        }
        
        private void validateMainFunction(HLSLCallMapper.FunctionDecl fd) {
            // main() should return void
            if (!fd.returnType().name().equals("void")) {
                warning("main() should return void", "main", WarningKind.DEPRECATED_FEATURE);
            }
            
            // main() should have no parameters
            if (!fd.parameters().isEmpty()) {
                error("main() cannot have parameters", "main", ErrorKind.SEMANTIC_ERROR);
            }
        }
        
        private void validateStructDecl(HLSLCallMapper.StructDecl sd) {
            // Add struct to symbol table
            symbolTable.put(sd.name(), HLSLCallMapper.TypeInfo.struct(sd.name()));
            
            // Validate members
            ObjectOpenHashSet<String> memberNames = new ObjectOpenHashSet<>();
            for (HLSLCallMapper.VariableDecl member : sd.members()) {
                if (memberNames.contains(member.name())) {
                    error("Duplicate member '" + member.name() + "' in struct " + sd.name(),
                          member.name(), ErrorKind.REDEFINITION);
                }
                memberNames.add(member.name());
            }
        }
        
        private void validateInterfaceBlock(HLSLCallMapper.InterfaceBlockDecl ibd) {
            // Check for uniform blocks without binding
            if (ibd.qualifiers().contains(HLSLCallMapper.Qualifier.UNIFORM)) {
                if (ibd.layout() == null || !ibd.layout().qualifiers().containsKey("binding")) {
                    warning("Uniform block '" + ibd.name() + "' has no binding qualifier",
                            ibd.name(), WarningKind.PERFORMANCE);
                }
            }
        }
        
        private void validateQualifiers(HLSLCallMapper.VariableDecl vd) {
            EnumSet<HLSLCallMapper.Qualifier> quals = vd.qualifiers();
            
            // in and out are mutually exclusive (except inout for parameters)
            if (quals.contains(HLSLCallMapper.Qualifier.IN) && quals.contains(HLSLCallMapper.Qualifier.OUT)) {
                error("Variable cannot be both 'in' and 'out'", vd.name(), ErrorKind.SEMANTIC_ERROR);
            }
            
            // Check stage-specific qualifiers
            if (quals.contains(HLSLCallMapper.Qualifier.CENTROID) && stage == HLSLCallMapper.ShaderStage.VERTEX) {
                warning("'centroid' qualifier has no effect in vertex shader", 
                        vd.name(), WarningKind.DEPRECATED_FEATURE);
            }
        }
        
        private void validateStatement(HLSLCallMapper.Statement stmt) {
            switch (stmt) {
                case HLSLCallMapper.BlockStmt bs -> {
                    pushScope();
                    for (HLSLCallMapper.Statement s : bs.statements()) {
                        validateStatement(s);
                    }
                    popScope();
                }
                case HLSLCallMapper.DeclStmt ds -> validateVariableDecl(ds.decl());
                case HLSLCallMapper.ExprStmt es -> validateExpression(es.expr());
                case HLSLCallMapper.IfStmt is -> {
                    validateExpression(is.cond());
                    validateStatement(is.thenBranch());
                    if (is.elseBranch() != null) validateStatement(is.elseBranch());
                }
                case HLSLCallMapper.ForStmt fs -> {
                    pushScope();
                    if (fs.init() != null) validateStatement(fs.init());
                    if (fs.cond() != null) validateExpression(fs.cond());
                    if (fs.incr() != null) validateExpression(fs.incr());
                    validateStatement(fs.body());
                    popScope();
                }
                case HLSLCallMapper.WhileStmt ws -> {
                    validateExpression(ws.cond());
                    validateStatement(ws.body());
                }
                case HLSLCallMapper.DoWhileStmt dws -> {
                    validateStatement(dws.body());
                    validateExpression(dws.cond());
                }
                case HLSLCallMapper.ReturnStmt rs -> {
                    if (rs.value() != null) validateExpression(rs.value());
                }
                case HLSLCallMapper.SwitchStmt ss -> {
                    validateExpression(ss.selector());
                    for (HLSLCallMapper.CaseClause cc : ss.cases()) {
                        if (cc.value() != null) validateExpression(cc.value());
                        for (HLSLCallMapper.Statement s : cc.stmts()) {
                            validateStatement(s);
                        }
                    }
                }
                default -> {}
            }
        }
        
        private void validateExpression(HLSLCallMapper.Expression expr) {
            switch (expr) {
                case HLSLCallMapper.IdentExpr ie -> {
                    if (!lookupSymbol(ie.name())) {
                        error("Undeclared identifier: " + ie.name(), ie.name(), ErrorKind.UNDECLARED_IDENTIFIER);
                    }
                }
                case HLSLCallMapper.CallExpr ce -> {
                    // Check function exists
                    if (!lookupSymbol(ce.name()) && 
                        !HLSLCallMapper.TYPE_MAP.containsKey(ce.name())) { // Constructor
                        error("Unknown function: " + ce.name(), ce.name(), ErrorKind.UNDECLARED_IDENTIFIER);
                    }
                    for (HLSLCallMapper.Expression arg : ce.args()) {
                        validateExpression(arg);
                    }
                }
                case HLSLCallMapper.BinaryExpr be -> {
                    validateExpression(be.left());
                    validateExpression(be.right());
                    validateBinaryOp(be);
                }
                case HLSLCallMapper.UnaryExpr ue -> validateExpression(ue.operand());
                case HLSLCallMapper.TernaryExpr te -> {
                    validateExpression(te.cond());
                    validateExpression(te.thenExpr());
                    validateExpression(te.elseExpr());
                }
                case HLSLCallMapper.MemberExpr me -> validateExpression(me.object());
                case HLSLCallMapper.IndexExpr ie -> {
                    validateExpression(ie.array());
                    validateExpression(ie.index());
                }
                case HLSLCallMapper.AssignExpr ae -> {
                    validateExpression(ae.target());
                    validateExpression(ae.value());
                }
                case HLSLCallMapper.ConstructExpr ce -> {
                    for (HLSLCallMapper.Expression arg : ce.args()) {
                        validateExpression(arg);
                    }
                }
                case HLSLCallMapper.SequenceExpr se -> {
                    for (HLSLCallMapper.Expression e : se.exprs()) {
                        validateExpression(e);
                    }
                }
                default -> {}
            }
        }
        
        private void validateBinaryOp(HLSLCallMapper.BinaryExpr be) {
            HLSLCallMapper.TypeInfo leftType = inferType(be.left());
            HLSLCallMapper.TypeInfo rightType = inferType(be.right());
            
            if (leftType == null || rightType == null) return;
            
            // Matrix-specific operations
            if (be.op() == HLSLCallMapper.BinOp.MUL) {
                if (leftType.isMatrix() || rightType.isMatrix()) {
                    // Matrix multiplication is valid
                    return;
                }
            }
            
            // Vector operations require matching sizes
            if (leftType.isVector() && rightType.isVector()) {
                if (!leftType.glslName.equals(rightType.glslName)) {
                    // Allow operations between same-size vectors of compatible types
                    // e.g., vec3 + ivec3 with implicit conversion
                }
            }
        }
        
        private HLSLCallMapper.TypeInfo inferType(HLSLCallMapper.Expression expr) {
            return switch (expr) {
                case HLSLCallMapper.LiteralExpr le -> switch (le.kind()) {
                    case INT -> HLSLCallMapper.TYPE_MAP.get("int");
                    case UINT -> HLSLCallMapper.TYPE_MAP.get("uint");
                    case FLOAT -> HLSLCallMapper.TYPE_MAP.get("float");
                    case DOUBLE -> HLSLCallMapper.TYPE_MAP.get("double");
                    case BOOL -> HLSLCallMapper.TYPE_MAP.get("bool");
                };
                case HLSLCallMapper.IdentExpr ie -> getSymbolType(ie.name());
                case HLSLCallMapper.ConstructExpr ce -> HLSLCallMapper.TYPE_MAP.get(ce.type());
                case HLSLCallMapper.CallExpr ce -> inferCallType(ce);
                case HLSLCallMapper.BinaryExpr be -> inferBinaryType(be);
                case HLSLCallMapper.UnaryExpr ue -> inferType(ue.operand());
                case HLSLCallMapper.TernaryExpr te -> inferType(te.thenExpr());
                case HLSLCallMapper.MemberExpr me -> inferMemberType(me);
                case HLSLCallMapper.IndexExpr ie -> inferIndexType(ie);
                case HLSLCallMapper.AssignExpr ae -> inferType(ae.target());
                case HLSLCallMapper.SequenceExpr se -> 
                    se.exprs().isEmpty() ? null : inferType(se.exprs().get(se.exprs().size() - 1));
                default -> null;
            };
        }
        
        private HLSLCallMapper.TypeInfo inferCallType(HLSLCallMapper.CallExpr ce) {
            // Constructor returns the constructed type
            HLSLCallMapper.TypeInfo constructedType = HLSLCallMapper.TYPE_MAP.get(ce.name());
            if (constructedType != null) return constructedType;
            
            // Built-in functions - infer from arguments
            // This is simplified; full implementation would need function signatures
            return null;
        }
        
        private HLSLCallMapper.TypeInfo inferBinaryType(HLSLCallMapper.BinaryExpr be) {
            HLSLCallMapper.TypeInfo left = inferType(be.left());
            HLSLCallMapper.TypeInfo right = inferType(be.right());
            
            // Comparison operators return bool
            if (be.op().isComparison()) {
                return HLSLCallMapper.TYPE_MAP.get("bool");
            }
            
            // Logical operators return bool
            if (be.op().isLogical()) {
                return HLSLCallMapper.TYPE_MAP.get("bool");
            }
            
            // Arithmetic: wider type wins
            if (left != null && right != null) {
                if (left.isMatrix() || right.isMatrix()) {
                    return left.isMatrix() ? left : right;
                }
                // Simplified type promotion
                return left;
            }
            
            return left;
        }
        
        private HLSLCallMapper.TypeInfo inferMemberType(HLSLCallMapper.MemberExpr me) {
            HLSLCallMapper.TypeInfo objType = inferType(me.object());
            if (objType == null) return null;
            
            String member = me.member();
            
            // Swizzle
            if (objType.isVector() && member.length() <= 4 && 
                member.matches("[xyzwrgbastpq]+")) {
                if (member.length() == 1) {
                    return HLSLCallMapper.TYPE_MAP.get(objType.componentType());
                }
                return HLSLCallMapper.TYPE_MAP.get(objType.componentType().charAt(0) + "vec" + member.length());
            }
            
            // Struct member - would need struct definition
            return null;
        }
        
        private HLSLCallMapper.TypeInfo inferIndexType(HLSLCallMapper.IndexExpr ie) {
            HLSLCallMapper.TypeInfo arrayType = inferType(ie.array());
            if (arrayType == null) return null;
            
            // Vector indexing returns scalar
            if (arrayType.isVector()) {
                return HLSLCallMapper.TYPE_MAP.get(arrayType.componentType());
            }
            
            // Matrix indexing returns column vector
            if (arrayType.isMatrix()) {
                // mat4[i] returns vec4
                int cols = arrayType.cols();
                return HLSLCallMapper.TYPE_MAP.get("vec" + cols);
            }
            
            // Array indexing returns element type
            return arrayType;
        }
        
        private boolean isAssignable(HLSLCallMapper.TypeInfo target, HLSLCallMapper.TypeInfo source) {
            if (target.glslName.equals(source.glslName)) return true;
            
            // Implicit conversions
            if (target.glslName.equals("float") && source.glslName.equals("int")) return true;
            if (target.glslName.equals("double") && 
                (source.glslName.equals("float") || source.glslName.equals("int"))) return true;
            
            // Vector implicit conversions
            if (target.isVector() && source.isVector()) {
                if (target.vectorSize() == source.vectorSize()) {
                    // Same size, check component type compatibility
                    return true; // Simplified
                }
            }
            
            return false;
        }
        
        private boolean lookupSymbol(String name) {
            for (int i = scopeStack.size() - 1; i >= 0; i--) {
                if (scopeStack.get(i).containsKey(name)) return true;
            }
            return symbolTable.containsKey(name);
        }
        
        private HLSLCallMapper.TypeInfo getSymbolType(String name) {
            for (int i = scopeStack.size() - 1; i >= 0; i--) {
                HLSLCallMapper.TypeInfo type = scopeStack.get(i).get(name);
                if (type != null) return type;
            }
            return symbolTable.get(name);
        }
        
        private void pushScope() {
            scopeStack.add(new Object2ObjectOpenHashMap<>());
        }
        
        private void popScope() {
            if (!scopeStack.isEmpty()) scopeStack.remove(scopeStack.size() - 1);
        }
        
        private Object2ObjectOpenHashMap<String, HLSLCallMapper.TypeInfo> currentScope() {
            return scopeStack.isEmpty() ? symbolTable : scopeStack.get(scopeStack.size() - 1);
        }
        
        private void error(String msg, String location, ErrorKind kind) {
            errors.add(new ValidationError(msg, location, kind));
        }
        
        private void warning(String msg, String location, WarningKind kind) {
            warnings.add(new ValidationWarning(msg, location, kind));
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 3: AST OPTIMIZER
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Performs optimization passes on the GLSL AST before translation.
     */
    public static final class Optimizer {
        
        public enum Pass {
            CONSTANT_FOLDING,      // Evaluate constant expressions
            DEAD_CODE_ELIMINATION, // Remove unreachable code
            COMMON_SUBEXPRESSION,  // Eliminate duplicate computations
            FUNCTION_INLINING,     // Inline small functions
            LOOP_UNROLLING,        // Unroll small constant loops
            STRENGTH_REDUCTION,    // Replace expensive ops with cheaper ones
            ALGEBRAIC_SIMPLIFY     // Simplify algebraic expressions
        }
        
        private final EnumSet<Pass> enabledPasses;
        
        public Optimizer(EnumSet<Pass> passes) {
            this.enabledPasses = passes;
        }
        
        public Optimizer() {
            this(EnumSet.of(Pass.CONSTANT_FOLDING, Pass.ALGEBRAIC_SIMPLIFY));
        }
        
        public HLSLCallMapper.TranslationUnit optimize(HLSLCallMapper.TranslationUnit unit) {
            ObjectArrayList<HLSLCallMapper.ASTNode> optimized = new ObjectArrayList<>();
            
            for (HLSLCallMapper.ASTNode node : unit.declarations()) {
                optimized.add(optimizeNode(node));
            }
            
            return new HLSLCallMapper.TranslationUnit(optimized);
        }
        
        private HLSLCallMapper.ASTNode optimizeNode(HLSLCallMapper.ASTNode node) {
            return switch (node) {
                case HLSLCallMapper.FunctionDecl fd -> optimizeFunction(fd);
                case HLSLCallMapper.VariableDecl vd -> optimizeVariable(vd);
                default -> node;
            };
        }
        
        private HLSLCallMapper.FunctionDecl optimizeFunction(HLSLCallMapper.FunctionDecl fd) {
            if (fd.body() == null) return fd;
            
            HLSLCallMapper.BlockStmt optimizedBody = optimizeBlock(fd.body());
            
            return new HLSLCallMapper.FunctionDecl(
                fd.returnType(), fd.name(), fd.parameters(), optimizedBody
            );
        }
        
        private HLSLCallMapper.BlockStmt optimizeBlock(HLSLCallMapper.BlockStmt block) {
            ObjectArrayList<HLSLCallMapper.Statement> optimized = new ObjectArrayList<>();
            
            for (HLSLCallMapper.Statement stmt : block.statements()) {
                HLSLCallMapper.Statement optStmt = optimizeStatement(stmt);
                
                // Dead code elimination: skip statements after return
                if (enabledPasses.contains(Pass.DEAD_CODE_ELIMINATION)) {
                    if (optStmt instanceof HLSLCallMapper.ReturnStmt) {
                        optimized.add(optStmt);
                        break; // Everything after is dead
                    }
                }
                
                optimized.add(optStmt);
            }
            
            return new HLSLCallMapper.BlockStmt(optimized);
        }
        
        private HLSLCallMapper.Statement optimizeStatement(HLSLCallMapper.Statement stmt) {
            return switch (stmt) {
                case HLSLCallMapper.BlockStmt bs -> optimizeBlock(bs);
                case HLSLCallMapper.ExprStmt es -> 
                    new HLSLCallMapper.ExprStmt(optimizeExpression(es.expr()));
                case HLSLCallMapper.DeclStmt ds -> optimizeDeclStatement(ds);
                case HLSLCallMapper.IfStmt is -> optimizeIf(is);
                case HLSLCallMapper.ForStmt fs -> optimizeFor(fs);
                case HLSLCallMapper.WhileStmt ws -> optimizeWhile(ws);
                case HLSLCallMapper.ReturnStmt rs -> rs.value() != null ?
                    new HLSLCallMapper.ReturnStmt(optimizeExpression(rs.value())) : rs;
                default -> stmt;
            };
        }
        
        private HLSLCallMapper.DeclStmt optimizeDeclStatement(HLSLCallMapper.DeclStmt ds) {
            HLSLCallMapper.VariableDecl vd = ds.decl();
            if (vd.initializer() == null) return ds;
            
            return new HLSLCallMapper.DeclStmt(new HLSLCallMapper.VariableDecl(
                vd.type(), vd.name(), optimizeExpression(vd.initializer()),
                vd.arraySize(), vd.qualifiers(), vd.layout()
            ));
        }
        
        private HLSLCallMapper.VariableDecl optimizeVariable(HLSLCallMapper.VariableDecl vd) {
            if (vd.initializer() == null) return vd;
            
            return new HLSLCallMapper.VariableDecl(
                vd.type(), vd.name(), optimizeExpression(vd.initializer()),
                vd.arraySize(), vd.qualifiers(), vd.layout()
            );
        }
        
        private HLSLCallMapper.IfStmt optimizeIf(HLSLCallMapper.IfStmt is) {
            HLSLCallMapper.Expression cond = optimizeExpression(is.cond());
            
            // Constant condition elimination
            if (enabledPasses.contains(Pass.DEAD_CODE_ELIMINATION)) {
                if (cond instanceof HLSLCallMapper.LiteralExpr lit && 
                    lit.kind() == HLSLCallMapper.LiteralKind.BOOL) {
                    // if (true) -> just then branch
                    // if (false) -> just else branch or nothing
                    // Return wrapped in block if needed
                }
            }
            
            return new HLSLCallMapper.IfStmt(
                cond,
                optimizeStatement(is.thenBranch()),
                is.elseBranch() != null ? optimizeStatement(is.elseBranch()) : null
            );
        }
        
        private HLSLCallMapper.ForStmt optimizeFor(HLSLCallMapper.ForStmt fs) {
            // Could unroll constant-count loops
            return new HLSLCallMapper.ForStmt(
                fs.init() != null ? optimizeStatement(fs.init()) : null,
                fs.cond() != null ? optimizeExpression(fs.cond()) : null,
                fs.incr() != null ? optimizeExpression(fs.incr()) : null,
                optimizeStatement(fs.body())
            );
        }
        
        private HLSLCallMapper.WhileStmt optimizeWhile(HLSLCallMapper.WhileStmt ws) {
            return new HLSLCallMapper.WhileStmt(
                optimizeExpression(ws.cond()),
                optimizeStatement(ws.body())
            );
        }
        
        private HLSLCallMapper.Expression optimizeExpression(HLSLCallMapper.Expression expr) {
            // First, recursively optimize children
            HLSLCallMapper.Expression optimized = switch (expr) {
                case HLSLCallMapper.BinaryExpr be -> new HLSLCallMapper.BinaryExpr(
                    optimizeExpression(be.left()), be.op(), optimizeExpression(be.right())
                );
                case HLSLCallMapper.UnaryExpr ue -> new HLSLCallMapper.UnaryExpr(
                    ue.op(), optimizeExpression(ue.operand())
                );
                case HLSLCallMapper.TernaryExpr te -> new HLSLCallMapper.TernaryExpr(
                    optimizeExpression(te.cond()),
                    optimizeExpression(te.thenExpr()),
                    optimizeExpression(te.elseExpr())
                );
                case HLSLCallMapper.CallExpr ce -> {
                    ObjectArrayList<HLSLCallMapper.Expression> args = new ObjectArrayList<>();
                    for (HLSLCallMapper.Expression arg : ce.args()) {
                        args.add(optimizeExpression(arg));
                    }
                    yield new HLSLCallMapper.CallExpr(ce.name(), args);
                }
                case HLSLCallMapper.ConstructExpr ce -> {
                    ObjectArrayList<HLSLCallMapper.Expression> args = new ObjectArrayList<>();
                    for (HLSLCallMapper.Expression arg : ce.args()) {
                        args.add(optimizeExpression(arg));
                    }
                    yield new HLSLCallMapper.ConstructExpr(ce.type(), args);
                }
                case HLSLCallMapper.IndexExpr ie -> new HLSLCallMapper.IndexExpr(
                    optimizeExpression(ie.array()), optimizeExpression(ie.index())
                );
                case HLSLCallMapper.MemberExpr me -> new HLSLCallMapper.MemberExpr(
                    optimizeExpression(me.object()), me.member()
                );
                case HLSLCallMapper.AssignExpr ae -> new HLSLCallMapper.AssignExpr(
                    optimizeExpression(ae.target()), ae.op(), optimizeExpression(ae.value())
                );
                default -> expr;
            };
            
            // Apply optimization passes
            if (enabledPasses.contains(Pass.CONSTANT_FOLDING)) {
                optimized = foldConstants(optimized);
            }
            if (enabledPasses.contains(Pass.ALGEBRAIC_SIMPLIFY)) {
                optimized = simplifyAlgebraic(optimized);
            }
            
            return optimized;
        }
        
        // ─── Constant Folding ───
        
        private HLSLCallMapper.Expression foldConstants(HLSLCallMapper.Expression expr) {
            if (!(expr instanceof HLSLCallMapper.BinaryExpr be)) return expr;
            
            if (be.left() instanceof HLSLCallMapper.LiteralExpr left &&
                be.right() instanceof HLSLCallMapper.LiteralExpr right) {
                
                // Both operands are literals - can fold
                Object result = foldBinaryOp(left, be.op(), right);
                if (result != null) {
                    return literalFromValue(result);
                }
            }
            
            return expr;
        }
        
        private Object foldBinaryOp(HLSLCallMapper.LiteralExpr left, 
                                    HLSLCallMapper.BinOp op, 
                                    HLSLCallMapper.LiteralExpr right) {
            // Integer operations
            if (left.kind() == HLSLCallMapper.LiteralKind.INT && 
                right.kind() == HLSLCallMapper.LiteralKind.INT) {
                int l = ((Number) left.value()).intValue();
                int r = ((Number) right.value()).intValue();
                
                return switch (op) {
                    case ADD -> l + r;
                    case SUB -> l - r;
                    case MUL -> l * r;
                    case DIV -> r != 0 ? l / r : null;
                    case MOD -> r != 0 ? l % r : null;
                    case BIT_AND -> l & r;
                    case BIT_OR -> l | r;
                    case BIT_XOR -> l ^ r;
                    case LSHIFT -> l << r;
                    case RSHIFT -> l >> r;
                    case EQ -> l == r;
                    case NE -> l != r;
                    case LT -> l < r;
                    case GT -> l > r;
                    case LE -> l <= r;
                    case GE -> l >= r;
                    default -> null;
                };
            }
            
            // Float operations
            if ((left.kind() == HLSLCallMapper.LiteralKind.FLOAT || 
                 left.kind() == HLSLCallMapper.LiteralKind.INT) &&
                (right.kind() == HLSLCallMapper.LiteralKind.FLOAT || 
                 right.kind() == HLSLCallMapper.LiteralKind.INT)) {
                
                double l = ((Number) left.value()).doubleValue();
                double r = ((Number) right.value()).doubleValue();
                
                return switch (op) {
                    case ADD -> (float)(l + r);
                    case SUB -> (float)(l - r);
                    case MUL -> (float)(l * r);
                    case DIV -> r != 0 ? (float)(l / r) : null;
                    case EQ -> l == r;
                    case NE -> l != r;
                    case LT -> l < r;
                    case GT -> l > r;
                    case LE -> l <= r;
                    case GE -> l >= r;
                    default -> null;
                };
            }
            
            // Boolean operations
            if (left.kind() == HLSLCallMapper.LiteralKind.BOOL && 
                right.kind() == HLSLCallMapper.LiteralKind.BOOL) {
                boolean l = (Boolean) left.value();
                boolean r = (Boolean) right.value();
                
                return switch (op) {
                    case AND -> l && r;
                    case OR -> l || r;
                    case XOR -> l ^ r;
                    case EQ -> l == r;
                    case NE -> l != r;
                    default -> null;
                };
            }
            
            return null;
        }
        
        private HLSLCallMapper.LiteralExpr literalFromValue(Object value) {
            if (value instanceof Integer i) {
                return new HLSLCallMapper.LiteralExpr(HLSLCallMapper.LiteralKind.INT, i);
            }
            if (value instanceof Float f) {
                return new HLSLCallMapper.LiteralExpr(HLSLCallMapper.LiteralKind.FLOAT, f);
            }
            if (value instanceof Double d) {
                return new HLSLCallMapper.LiteralExpr(HLSLCallMapper.LiteralKind.FLOAT, d.floatValue());
            }
            if (value instanceof Boolean b) {
                return new HLSLCallMapper.LiteralExpr(HLSLCallMapper.LiteralKind.BOOL, b);
            }
            throw new IllegalArgumentException("Unknown literal type: " + value.getClass());
        }
        
        // ─── Algebraic Simplification ───
        
        private HLSLCallMapper.Expression simplifyAlgebraic(HLSLCallMapper.Expression expr) {
            if (!(expr instanceof HLSLCallMapper.BinaryExpr be)) return expr;
            
            HLSLCallMapper.Expression left = be.left();
            HLSLCallMapper.Expression right = be.right();
            
            // x + 0 = x, 0 + x = x
            if (be.op() == HLSLCallMapper.BinOp.ADD) {
                if (isZero(right)) return left;
                if (isZero(left)) return right;
            }
            
            // x - 0 = x
            if (be.op() == HLSLCallMapper.BinOp.SUB) {
                if (isZero(right)) return left;
            }
            
            // x * 1 = x, 1 * x = x
            if (be.op() == HLSLCallMapper.BinOp.MUL) {
                if (isOne(right)) return left;
                if (isOne(left)) return right;
                // x * 0 = 0, 0 * x = 0
                if (isZero(right)) return right;
                if (isZero(left)) return left;
                // x * 2 = x + x (might be faster)
                // -x * -y = x * y
            }
            
            // x / 1 = x
            if (be.op() == HLSLCallMapper.BinOp.DIV) {
                if (isOne(right)) return left;
            }
            
            // x && true = x, true && x = x
            if (be.op() == HLSLCallMapper.BinOp.AND) {
                if (isTrue(right)) return left;
                if (isTrue(left)) return right;
                if (isFalse(right) || isFalse(left)) {
                    return new HLSLCallMapper.LiteralExpr(HLSLCallMapper.LiteralKind.BOOL, false);
                }
            }
            
            // x || false = x, false || x = x
            if (be.op() == HLSLCallMapper.BinOp.OR) {
                if (isFalse(right)) return left;
                if (isFalse(left)) return right;
                if (isTrue(right) || isTrue(left)) {
                    return new HLSLCallMapper.LiteralExpr(HLSLCallMapper.LiteralKind.BOOL, true);
                }
            }
            
            return expr;
        }
        
        private boolean isZero(HLSLCallMapper.Expression expr) {
            if (!(expr instanceof HLSLCallMapper.LiteralExpr lit)) return false;
            return switch (lit.kind()) {
                case INT, UINT -> ((Number) lit.value()).intValue() == 0;
                case FLOAT, DOUBLE -> ((Number) lit.value()).doubleValue() == 0.0;
                default -> false;
            };
        }
        
        private boolean isOne(HLSLCallMapper.Expression expr) {
            if (!(expr instanceof HLSLCallMapper.LiteralExpr lit)) return false;
            return switch (lit.kind()) {
                case INT, UINT -> ((Number) lit.value()).intValue() == 1;
                case FLOAT, DOUBLE -> ((Number) lit.value()).doubleValue() == 1.0;
                default -> false;
            };
        }
        
        private boolean isTrue(HLSLCallMapper.Expression expr) {
            if (!(expr instanceof HLSLCallMapper.LiteralExpr lit)) return false;
            return lit.kind() == HLSLCallMapper.LiteralKind.BOOL && (Boolean) lit.value();
        }
        
        private boolean isFalse(HLSLCallMapper.Expression expr) {
            if (!(expr instanceof HLSLCallMapper.LiteralExpr lit)) return false;
            return lit.kind() == HLSLCallMapper.LiteralKind.BOOL && !(Boolean) lit.value();
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 4: SHADER REFLECTION
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Extracts reflection data from parsed GLSL for runtime binding setup.
     */
    public static final class Reflector {
        
        public record ReflectionData(
            ObjectArrayList<UniformBlock> uniformBlocks,
            ObjectArrayList<UniformVariable> uniforms,
            ObjectArrayList<TextureBinding> textures,
            ObjectArrayList<ImageBinding> images,
            ObjectArrayList<InputAttribute> inputs,
            ObjectArrayList<OutputAttribute> outputs,
            ObjectArrayList<BufferBinding> buffers
        ) {}
        
        public record UniformBlock(
            String name,
            int binding,
            int size,
            ObjectArrayList<UniformVariable> members
        ) {}
        
        public record UniformVariable(
            String name,
            String type,
            int location,
            int arraySize,
            int offset,
            int size
        ) {}
        
        public record TextureBinding(
            String name,
            String samplerType,
            int binding,
            int set
        ) {}
        
        public record ImageBinding(
            String name,
            String imageType,
            int binding,
            int set,
            String format
        ) {}
        
        public record InputAttribute(
            String name,
            String type,
            int location,
            String semantic
        ) {}
        
        public record OutputAttribute(
            String name,
            String type,
            int location,
            String semantic
        ) {}
        
        public record BufferBinding(
            String name,
            int binding,
            int set,
            BufferType type
        ) {
            public enum BufferType { SSBO, UBO, ATOMIC }
        }
        
        public static ReflectionData reflect(HLSLCallMapper.TranslationUnit unit) {
            ObjectArrayList<UniformBlock> blocks = new ObjectArrayList<>();
            ObjectArrayList<UniformVariable> uniforms = new ObjectArrayList<>();
            ObjectArrayList<TextureBinding> textures = new ObjectArrayList<>();
            ObjectArrayList<ImageBinding> images = new ObjectArrayList<>();
            ObjectArrayList<InputAttribute> inputs = new ObjectArrayList<>();
            ObjectArrayList<OutputAttribute> outputs = new ObjectArrayList<>();
            ObjectArrayList<BufferBinding> buffers = new ObjectArrayList<>();
            
            for (HLSLCallMapper.ASTNode node : unit.declarations()) {
                switch (node) {
                    case HLSLCallMapper.VariableDecl vd -> {
                        processVariableReflection(vd, uniforms, textures, images, inputs, outputs);
                    }
                    case HLSLCallMapper.InterfaceBlockDecl ibd -> {
                        processBlockReflection(ibd, blocks, buffers);
                    }
                    default -> {}
                }
            }
            
            return new ReflectionData(blocks, uniforms, textures, images, inputs, outputs, buffers);
        }
        
        private static void processVariableReflection(
            HLSLCallMapper.VariableDecl vd,
            ObjectArrayList<UniformVariable> uniforms,
            ObjectArrayList<TextureBinding> textures,
            ObjectArrayList<ImageBinding> images,
            ObjectArrayList<InputAttribute> inputs,
            ObjectArrayList<OutputAttribute> outputs
        ) {
            String typeName = vd.type().name();
            HLSLCallMapper.TypeInfo type = HLSLCallMapper.TYPE_MAP.get(typeName);
            
            int location = -1;
            int binding = -1;
            int set = 0;
            
            if (vd.layout() != null) {
                var quals = vd.layout().qualifiers();
                if (quals.containsKey("location")) {
                    location = extractInt(quals.get("location"));
                }
                if (quals.containsKey("binding")) {
                    binding = extractInt(quals.get("binding"));
                }
                if (quals.containsKey("set")) {
                    set = extractInt(quals.get("set"));
                }
            }
            
            if (vd.qualifiers().contains(HLSLCallMapper.Qualifier.UNIFORM)) {
                if (type != null && type.isSampler()) {
                    textures.add(new TextureBinding(vd.name(), typeName, binding, set));
                } else if (type != null && type.isImage()) {
                    String format = "";
                    if (vd.layout() != null) {
                        for (var entry : vd.layout().qualifiers().entrySet()) {
                            String key = entry.getKey();
                            if (key.startsWith("r") || key.startsWith("rgba") || key.equals("r32f")) {
                                format = key;
                                break;
                            }
                        }
                    }
                    images.add(new ImageBinding(vd.name(), typeName, binding, set, format));
                } else {
                    int size = type != null ? type.sizeBytes() : 4;
                    uniforms.add(new UniformVariable(vd.name(), typeName, location, 
                        vd.type().isArray() ? extractArraySize(vd) : 0, -1, size));
                }
            } else if (vd.qualifiers().contains(HLSLCallMapper.Qualifier.IN) ||
                       vd.qualifiers().contains(HLSLCallMapper.Qualifier.ATTRIBUTE)) {
                inputs.add(new InputAttribute(vd.name(), typeName, location, ""));
            } else if (vd.qualifiers().contains(HLSLCallMapper.Qualifier.OUT) ||
                       vd.qualifiers().contains(HLSLCallMapper.Qualifier.VARYING)) {
                outputs.add(new OutputAttribute(vd.name(), typeName, location, ""));
            }
        }
        
        private static void processBlockReflection(
            HLSLCallMapper.InterfaceBlockDecl ibd,
            ObjectArrayList<UniformBlock> blocks,
            ObjectArrayList<BufferBinding> buffers
        ) {
            int binding = -1;
            int set = 0;
            
            if (ibd.layout() != null) {
                var quals = ibd.layout().qualifiers();
                if (quals.containsKey("binding")) {
                    binding = extractInt(quals.get("binding"));
                }
                if (quals.containsKey("set")) {
                    set = extractInt(quals.get("set"));
                }
            }
            
            if (ibd.qualifiers().contains(HLSLCallMapper.Qualifier.UNIFORM)) {
                // Uniform block
                ObjectArrayList<UniformVariable> members = new ObjectArrayList<>();
                int offset = 0;
                
                for (HLSLCallMapper.VariableDecl member : ibd.members()) {
                    HLSLCallMapper.TypeInfo type = HLSLCallMapper.TYPE_MAP.get(member.type().name());
                    int size = type != null ? type.sizeBytes() : 4;
                    
                    // std140 alignment rules
                    int alignment = type != null ? type.alignment() : 4;
                    offset = (offset + alignment - 1) & ~(alignment - 1);
                    
                    members.add(new UniformVariable(
                        member.name(), member.type().name(), -1,
                        member.type().isArray() ? extractArraySize(member) : 0,
                        offset, size
                    ));
                    
                    offset += size;
                }
                
                blocks.add(new UniformBlock(ibd.name(), binding, offset, members));
            } else if (ibd.qualifiers().contains(HLSLCallMapper.Qualifier.BUFFER)) {
                // Shader storage buffer
                buffers.add(new BufferBinding(ibd.name(), binding, set, BufferBinding.BufferType.SSBO));
            }
        }
        
        private static int extractInt(HLSLCallMapper.Expression expr) {
            if (expr instanceof HLSLCallMapper.LiteralExpr lit) {
                return ((Number) lit.value()).intValue();
            }
            return -1;
        }
        
        private static int extractArraySize(HLSLCallMapper.VariableDecl vd) {
            if (vd.arraySize() != null) {
                return extractInt(vd.arraySize());
            }
            if (vd.type().arraySize() != null) {
                return extractInt(vd.type().arraySize());
            }
            return 0;
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 5: COMPLETE PIPELINE
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Complete GLSL→HLSL translation pipeline with all features.
     */
    public record Pipeline(
        Preprocessor.Config preprocessorConfig,
        HLSLCallMapper.Config translationConfig,
        EnumSet<Optimizer.Pass> optimizerPasses,
        boolean validate,
        boolean optimize
    ) {
        public static Builder builder(HLSLCallMapper.ShaderStage stage) {
            return new Builder(stage);
        }
        
        public static final class Builder {
            private final HLSLCallMapper.ShaderStage stage;
            private Preprocessor.Config ppConfig;
            private HLSLCallMapper.Config.Builder transConfigBuilder;
            private EnumSet<Optimizer.Pass> optimizerPasses = EnumSet.of(
                Optimizer.Pass.CONSTANT_FOLDING,
                Optimizer.Pass.ALGEBRAIC_SIMPLIFY
            );
            private boolean validate = true;
            private boolean optimize = true;
            
            Builder(HLSLCallMapper.ShaderStage stage) {
                this.stage = stage;
                this.ppConfig = Preprocessor.Config.builder().build();
                this.transConfigBuilder = HLSLCallMapper.Config.builder(stage);
            }
            
            public Builder preprocessor(Preprocessor.Config config) {
                this.ppConfig = config;
                return this;
            }
            
            public Builder translation(Consumer<HLSLCallMapper.Config.Builder> configurer) {
                configurer.accept(transConfigBuilder);
                return this;
            }
            
            public Builder optimizerPasses(EnumSet<Optimizer.Pass> passes) {
                this.optimizerPasses = passes;
                return this;
            }
            
            public Builder validate(boolean v) {
                this.validate = v;
                return this;
            }
            
            public Builder optimize(boolean o) {
                this.optimize = o;
                return this;
            }
            
            public Pipeline build() {
                return new Pipeline(
                    ppConfig,
                    transConfigBuilder.build(),
                    optimizerPasses,
                    validate,
                    optimize
                );
            }
        }
    }
    
    public record PipelineResult(
        String hlslCode,
        boolean success,
        ObjectArrayList<String> errors,
        ObjectArrayList<String> warnings,
        Reflector.ReflectionData reflection,
        Preprocessor.Result preprocessorResult,
        Validator.ValidationResult validationResult,
        HLSLCallMapper.TranslationResult translationResult
    ) {
        public boolean hasErrors() { return !errors.isEmpty(); }
    }
    
    /**
     * Execute the complete translation pipeline.
     */
    public static PipelineResult execute(String glslSource, Pipeline pipeline) {
        ObjectArrayList<String> allErrors = new ObjectArrayList<>();
        ObjectArrayList<String> allWarnings = new ObjectArrayList<>();
        
        // 1. Preprocess
        Preprocessor pp = new Preprocessor(pipeline.preprocessorConfig());
        Preprocessor.Result ppResult = pp.process(glslSource);
        allErrors.addAll(ppResult.errors());
        allWarnings.addAll(ppResult.warnings());
        
        if (ppResult.hasErrors()) {
            return new PipelineResult("", false, allErrors, allWarnings, null, ppResult, null, null);
        }
        
        // 2. Tokenize and Parse
        HLSLCallMapper.Lexer lexer = new HLSLCallMapper.Lexer(ppResult.processedSource());
        var tokens = lexer.tokenize(false);
        allErrors.addAll(lexer.getErrors());
        
        HLSLCallMapper.Parser parser = new HLSLCallMapper.Parser(tokens);
        HLSLCallMapper.TranslationUnit ast = parser.parse();
        allErrors.addAll(parser.getErrors());
        
        if (!allErrors.isEmpty() && pipeline.translationConfig().enableStrictMode) {
            return new PipelineResult("", false, allErrors, allWarnings, null, ppResult, null, null);
        }
        
        // 3. Validate (optional)
        Validator.ValidationResult validationResult = null;
        if (pipeline.validate()) {
            Validator validator = new Validator(pipeline.translationConfig().stage);
            validationResult = validator.validate(ast);
            
            for (var err : validationResult.errors()) {
                allErrors.add(err.message());
            }
            for (var warn : validationResult.warnings()) {
                allWarnings.add(warn.message());
            }
            
            if (validationResult.hasErrors() && pipeline.translationConfig().enableStrictMode) {
                return new PipelineResult("", false, allErrors, allWarnings, null, ppResult, validationResult, null);
            }
        }
        
        // 4. Optimize (optional)
        if (pipeline.optimize()) {
            Optimizer optimizer = new Optimizer(pipeline.optimizerPasses());
            ast = optimizer.optimize(ast);
        }
        
        // 5. Extract reflection
        Reflector.ReflectionData reflection = Reflector.reflect(ast);
        
        // 6. Translate to HLSL
        // Note: We need to re-translate since we potentially modified the AST
        HLSLCallMapper.TranslationResult transResult = HLSLCallMapper.translate(
            ppResult.processedSource(), 
            pipeline.translationConfig()
        );
        
        allErrors.addAll(transResult.errors());
        allWarnings.addAll(transResult.warnings());
        
        return new PipelineResult(
            transResult.hlslCode(),
            transResult.success(),
            allErrors,
            allWarnings,
            reflection,
            ppResult,
            validationResult,
            transResult
        );
    }
    
    /**
     * Simple translation with default pipeline.
     */
    public static PipelineResult translate(String glslSource, HLSLCallMapper.ShaderStage stage) {
        Pipeline pipeline = Pipeline.builder(stage).build();
        return execute(glslSource, pipeline);
    }
    
    // Private constructor
    private GLSLProcessor() {}
}

// ════════════════════════════════════════════════════════════════════════════════
// END OF HLSLCallMapper.java
// ════════════════════════════════════════════════════════════════════════════════