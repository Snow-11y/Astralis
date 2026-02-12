package stellar.snow.astralis.api.opengl.mapping;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Java 25 FFM (Foreign Function & Memory API)
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * GLSLCallMapper - Universal GLSL Translation Layer
 * 
 * Translates GLSL shader code between any supported versions (1.10 - 4.60)
 * without emulation - only uses features truly supported by hardware.
 * 
 * Performance-first design with zero-allocation hot paths, object pooling,
 * and aggressive caching.
 * 
 * snowy Advanced GL Translation System
 * @version 1.0.0
 */
public final class GLSLCallMapper {
    
    // ==================== SINGLETON ====================
    
    private static volatile GLSLCallMapper INSTANCE;
    private static final Object LOCK = new Object();
    
    public static GLSLCallMapper getInstance() {
        GLSLCallMapper local = INSTANCE;
        if (local == null) {
            synchronized (LOCK) {
                local = INSTANCE;
                if (local == null) {
                    INSTANCE = local = new GLSLCallMapper();
                }
            }
        }
        return local;
    }
    
    // ==================== CORE COMPONENTS ====================
    
    private final GLSLVersionManager versionManager;
    private final GLSLCapabilityRegistry capabilityRegistry;
    private final GLSLTranslationCache translationCache;
    private final GLSLMemoryPool memoryPool;
    private final GLSLTokenizerPool tokenizerPool;
    private final GLSLParserPool parserPool;
    private final GLSLOptimizer optimizer;
    private final GLSLCodeGenerator codeGenerator;
    private final GLSLTranslationEngine translationEngine;
    private final GLSLMetrics metrics;
    
    private volatile GLSLVersion targetVersion;
    private volatile GLSLVersion hardwareMaxVersion;
    private volatile boolean strictMode;
    private volatile int optimizationLevel;
    
    private GLSLCallMapper() {
        this.memoryPool = new GLSLMemoryPool();
        this.versionManager = new GLSLVersionManager();
        this.capabilityRegistry = new GLSLCapabilityRegistry();
        this.translationCache = new GLSLTranslationCache(memoryPool);
        this.tokenizerPool = new GLSLTokenizerPool(memoryPool);
        this.parserPool = new GLSLParserPool(memoryPool);
        this.optimizer = new GLSLOptimizer(memoryPool);
        this.codeGenerator = new GLSLCodeGenerator(memoryPool);
        this.translationEngine = new GLSLTranslationEngine(this);
        this.metrics = new GLSLMetrics();
        
        this.targetVersion = GLSLVersion.GLSL_330;
        this.hardwareMaxVersion = GLSLVersion.GLSL_460;
        this.strictMode = false;
        this.optimizationLevel = 2;
        
        detectHardwareCapabilities();
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Translates GLSL shader source from source version to target version.
     * Uses cached results when available.
     * 
     * @param source The shader source code
     * @param shaderType The type of shader (vertex, fragment, etc.)
     * @return Translated and optimized shader source
     */
    public GLSLTranslationResult translate(String source, GLSLShaderType shaderType) {
        return translate(source, shaderType, null, targetVersion);
    }
    
    public GLSLTranslationResult translate(String source, GLSLShaderType shaderType, 
                                           GLSLVersion sourceVersion, GLSLVersion targetVersion) {
        long startTime = System.nanoTime();
        
        // Check cache first
        long cacheKey = computeCacheKey(source, shaderType, targetVersion);
        GLSLTranslationResult cached = translationCache.get(cacheKey);
        if (cached != null) {
            metrics.recordCacheHit();
            return cached;
        }
        
        metrics.recordCacheMiss();
        
        // Detect source version if not provided
        if (sourceVersion == null) {
            sourceVersion = versionManager.detectVersion(source);
        }
        
        // Get pooled resources
        GLSLTokenizer tokenizer = tokenizerPool.acquire();
        GLSLParser parser = parserPool.acquire();
        
        try {
            // Tokenize
            GLSLTokenStream tokens = tokenizer.tokenize(source);
            
            // Parse to AST
            GLSLShaderAST ast = parser.parse(tokens, shaderType);
            
            // Translate AST
            GLSLShaderAST translatedAST = translationEngine.translate(
                ast, sourceVersion, targetVersion, shaderType);
            
            // Optimize if enabled
            if (optimizationLevel > 0) {
                translatedAST = optimizer.optimize(translatedAST, optimizationLevel);
            }
            
            // Generate code
            String translatedSource = codeGenerator.generate(translatedAST, targetVersion);
            
            // Build result
            GLSLTranslationResult result = new GLSLTranslationResult(
                translatedSource,
                sourceVersion,
                targetVersion,
                shaderType,
                translatedAST.getRequiredExtensions(),
                translatedAST.getWarnings(),
                System.nanoTime() - startTime
            );
            
            // Cache result
            translationCache.put(cacheKey, result);
            
            return result;
            
        } finally {
            // Return pooled resources
            tokenizerPool.release(tokenizer);
            parserPool.release(parser);
        }
    }
    
    /**
     * Batch translate multiple shaders efficiently.
     * Shares parsing context and optimizes memory usage.
     */
    public GLSLTranslationResult[] translateBatch(GLSLShaderSource[] sources) {
        GLSLTranslationResult[] results = new GLSLTranslationResult[sources.length];
        
        // Sort by version to minimize context switches
        Integer[] indices = new Integer[sources.length];
        for (int i = 0; i < sources.length; i++) indices[i] = i;
        
        Arrays.sort(indices, (a, b) -> {
            GLSLVersion va = sources[a].getSourceVersion();
            GLSLVersion vb = sources[b].getSourceVersion();
            if (va == null) va = GLSLVersion.GLSL_110;
            if (vb == null) vb = GLSLVersion.GLSL_110;
            return va.compareTo(vb);
        });
        
        for (int idx : indices) {
            GLSLShaderSource src = sources[idx];
            results[idx] = translate(src.getSource(), src.getType(), 
                                     src.getSourceVersion(), 
                                     src.getTargetVersion() != null ? 
                                         src.getTargetVersion() : targetVersion);
        }
        
        return results;
    }
    
    /**
     * Checks if a feature is supported at the given version.
     */
    public boolean isFeatureSupported(GLSLFeature feature, GLSLVersion version) {
        return capabilityRegistry.isSupported(feature, version);
    }
    
    /**
     * Gets the minimum version that supports all required features.
     */
    public GLSLVersion getMinimumRequiredVersion(Set<GLSLFeature> features) {
        return capabilityRegistry.getMinimumVersion(features);
    }
    
    /**
     * Validates shader source for the given version.
     */
    public GLSLValidationResult validate(String source, GLSLVersion version, 
                                         GLSLShaderType shaderType) {
        GLSLTokenizer tokenizer = tokenizerPool.acquire();
        GLSLParser parser = parserPool.acquire();
        
        try {
            GLSLTokenStream tokens = tokenizer.tokenize(source);
            GLSLShaderAST ast = parser.parse(tokens, shaderType);
            return translationEngine.validate(ast, version, shaderType);
        } catch (GLSLParseException e) {
            return new GLSLValidationResult(false, e.getErrors(), Collections.emptyList());
        } finally {
            tokenizerPool.release(tokenizer);
            parserPool.release(parser);
        }
    }
    
    // ==================== CONFIGURATION ====================
    
    public GLSLCallMapper setTargetVersion(GLSLVersion version) {
        if (version.ordinal() > hardwareMaxVersion.ordinal()) {
            throw new GLSLVersionException(
                "Target version " + version + " exceeds hardware max " + hardwareMaxVersion);
        }
        this.targetVersion = version;
        return this;
    }
    
    public GLSLCallMapper setOptimizationLevel(int level) {
        this.optimizationLevel = Math.max(0, Math.min(3, level));
        return this;
    }
    
    public GLSLCallMapper setStrictMode(boolean strict) {
        this.strictMode = strict;
        return this;
    }
    
    public GLSLVersion getTargetVersion() { return targetVersion; }
    public GLSLVersion getHardwareMaxVersion() { return hardwareMaxVersion; }
    public int getOptimizationLevel() { return optimizationLevel; }
    public boolean isStrictMode() { return strictMode; }
    
    // ==================== INTERNAL ====================
    
    private void detectHardwareCapabilities() {
        // Query OpenGL for max supported GLSL version
        String versionStr = OpenGLCallMapper.getInstance().glGetString(
            OpenGLCallMapper.GL_SHADING_LANGUAGE_VERSION);
        
        if (versionStr != null) {
            hardwareMaxVersion = versionManager.parseVersionString(versionStr);
        }
        
        // Query extensions
        capabilityRegistry.detectCapabilities(OpenGLCallMapper.getInstance());
    }
    
    private long computeCacheKey(String source, GLSLShaderType type, GLSLVersion version) {
        long hash = type.ordinal();
        hash = hash * 31 + version.ordinal();
        hash = hash * 31 + source.hashCode();
        hash = hash * 31 + optimizationLevel;
        return hash;
    }
    
    // ==================== ACCESSORS FOR COMPONENTS ====================
    
    GLSLVersionManager getVersionManager() { return versionManager; }
    GLSLCapabilityRegistry getCapabilityRegistry() { return capabilityRegistry; }
    GLSLMemoryPool getMemoryPool() { return memoryPool; }
    GLSLMetrics getMetrics() { return metrics; }
    
    /**
     * Releases all cached resources and resets pools.
     * Call during cleanup or when memory pressure is high.
     */
    public void releaseResources() {
        translationCache.clear();
        memoryPool.releaseAll();
        tokenizerPool.releaseAll();
        parserPool.releaseAll();
        metrics.reset();
    }
    
    /**
     * Trims caches and pools to reduce memory usage.
     * Keeps frequently used entries.
     */
    public void trimMemory() {
        translationCache.trim();
        memoryPool.trim();
        tokenizerPool.trim();
        parserPool.trim();
    }
    
    /**
     * Shutdown the GLSL mapper and free all resources
     * 
     * CRITICAL: Must be called on game shutdown to prevent memory-mapped file locks
     * and to properly free all off-heap FFM Arena memory.
     * 
     * This method:
     * 1. Shuts down the memory pool (closes FFM Arena)
     * 2. Clears all caches
     * 3. Releases pooled objects
     * 4. Resets the singleton
     */
    public static void shutdown() {
        synchronized (LOCK) {
            if (INSTANCE != null) {
                GLSLCallMapper instance = INSTANCE;
                
                // Clear all caches first
                instance.releaseResources();
                
                // Shutdown the memory pool - this closes the FFM Arena
                instance.memoryPool.shutdown();
                
                // Clear singleton reference
                INSTANCE = null;
                
                LogManager.getLogger("GLSLCallMapper").info("GLSLCallMapper shutdown complete");
            }
        }
    }
    
    /**
     * Reinitialize after OpenGL context loss
     * 
     * Context Loss occurs when:
     * - User changes display scale
     * - User toggles fullscreen
     * - User switches between monitors
     * - Graphics driver crashes and recovers
     * - System suspends and resumes
     * 
     * When this happens, all our GLSL shader compilations become invalid.
     * This method ensures:
     * 1. All caches are cleared
     * 2. Singleton instance is recreated
     * 3. Memory pool is reset
     * 4. Hardware capabilities are re-detected
     */
    public static void reinitializeAfterContextLoss() {
        synchronized (LOCK) {
            LogManager.getLogger("GLSLCallMapper").warn("Context loss detected - reinitializing GLSL mapper");
            
            if (INSTANCE != null) {
                // Don't shutdown completely - just clear caches
                INSTANCE.releaseResources();
            }
            
            // Force recreation of singleton on next getInstance()
            INSTANCE = null;
            
            // Next call to getInstance() will create a new instance with fresh detection
            LogManager.getLogger("GLSLCallMapper").info("GLSL mapper ready for re-initialization");
        }
    }
}

// ============================================================================
// GLSL VERSION ENUM
// ============================================================================

/**
 * All supported GLSL versions with their corresponding OpenGL versions
 * and core capabilities.
 */
enum GLSLVersion {
    GLSL_110(110, 200, "1.10", "2.0"),
    GLSL_120(120, 210, "1.20", "2.1"),
    GLSL_130(130, 300, "1.30", "3.0"),
    GLSL_140(140, 310, "1.40", "3.1"),
    GLSL_150(150, 320, "1.50", "3.2"),
    GLSL_330(330, 330, "3.30", "3.3"),
    GLSL_400(400, 400, "4.00", "4.0"),
    GLSL_410(410, 410, "4.10", "4.1"),
    GLSL_420(420, 420, "4.20", "4.2"),
    GLSL_430(430, 430, "4.30", "4.3"),
    GLSL_440(440, 440, "4.40", "4.4"),
    GLSL_450(450, 450, "4.50", "4.5"),
    GLSL_460(460, 460, "4.60", "4.6");
    
    public final int versionNumber;
    public final int glVersion;
    public final String versionString;
    public final String glVersionString;
    
    // Cached arrays for fast lookup
    private static final GLSLVersion[] BY_NUMBER = new GLSLVersion[500];
    private static final Map<String, GLSLVersion> BY_STRING = new HashMap<>();
    
    static {
        for (GLSLVersion v : values()) {
            BY_NUMBER[v.versionNumber] = v;
            BY_STRING.put(v.versionString, v);
            BY_STRING.put(v.versionString.replace(".", ""), v);
        }
    }
    
    GLSLVersion(int versionNumber, int glVersion, String versionString, String glVersionString) {
        this.versionNumber = versionNumber;
        this.glVersion = glVersion;
        this.versionString = versionString;
        this.glVersionString = glVersionString;
    }
    
    public static GLSLVersion fromNumber(int number) {
        if (number >= 0 && number < BY_NUMBER.length && BY_NUMBER[number] != null) {
            return BY_NUMBER[number];
        }
        // Find closest lower version
        GLSLVersion closest = GLSL_110;
        for (GLSLVersion v : values()) {
            if (v.versionNumber <= number && v.versionNumber > closest.versionNumber) {
                closest = v;
            }
        }
        return closest;
    }
    
    public static GLSLVersion fromString(String version) {
        return BY_STRING.getOrDefault(version.trim(), GLSL_110);
    }
    
    public boolean supports(GLSLVersion other) {
        return this.versionNumber >= other.versionNumber;
    }
    
    public String getVersionDirective() {
        return "#version " + versionNumber;
    }
    
    public String getVersionDirectiveCore() {
        return "#version " + versionNumber + " core";
    }
    
    public String getVersionDirectiveCompatibility() {
        return "#version " + versionNumber + " compatibility";
    }
    
    public boolean isLegacy() {
        return versionNumber < 140;
    }
    
    public boolean hasGeometryShaders() {
        return versionNumber >= 150;
    }
    
    public boolean hasTessellationShaders() {
        return versionNumber >= 400;
    }
    
    public boolean hasComputeShaders() {
        return versionNumber >= 430;
    }
    
    public boolean hasExplicitLocations() {
        return versionNumber >= 330;
    }
}

// ============================================================================
// SHADER TYPE ENUM
// ============================================================================

enum GLSLShaderType {
    VERTEX(0x8B31, "vert", "vs"),
    FRAGMENT(0x8B30, "frag", "fs"),
    GEOMETRY(0x8DD9, "geom", "gs"),
    TESS_CONTROL(0x8E88, "tesc", "tcs"),
    TESS_EVALUATION(0x8E87, "tese", "tes"),
    COMPUTE(0x91B9, "comp", "cs");
    
    public final int glType;
    public final String extension;
    public final String shortName;
    
    private static final GLSLShaderType[] BY_GL_TYPE;
    private static final Map<String, GLSLShaderType> BY_EXTENSION = new HashMap<>();
    
    static {
        BY_GL_TYPE = new GLSLShaderType[0x91BA];
        for (GLSLShaderType t : values()) {
            BY_GL_TYPE[t.glType] = t;
            BY_EXTENSION.put(t.extension, t);
            BY_EXTENSION.put(t.shortName, t);
        }
    }
    
    GLSLShaderType(int glType, String extension, String shortName) {
        this.glType = glType;
        this.extension = extension;
        this.shortName = shortName;
    }
    
    public static GLSLShaderType fromGLType(int type) {
        return type < BY_GL_TYPE.length ? BY_GL_TYPE[type] : null;
    }
    
    public static GLSLShaderType fromExtension(String ext) {
        return BY_EXTENSION.get(ext.toLowerCase());
    }
    
    public GLSLVersion getMinimumVersion() {
        switch (this) {
            case GEOMETRY: return GLSLVersion.GLSL_150;
            case TESS_CONTROL:
            case TESS_EVALUATION: return GLSLVersion.GLSL_400;
            case COMPUTE: return GLSLVersion.GLSL_430;
            default: return GLSLVersion.GLSL_110;
        }
    }
}

// ============================================================================
// GLSL FEATURE FLAGS
// ============================================================================

enum GLSLFeature {
    // Basic features
    ATTRIBUTE_KEYWORD(GLSLVersion.GLSL_110, GLSLVersion.GLSL_130),
    VARYING_KEYWORD(GLSLVersion.GLSL_110, GLSLVersion.GLSL_130),
    IN_OUT_KEYWORDS(GLSLVersion.GLSL_130, null),
    
    // Texture functions
    TEXTURE2D_FUNCTION(GLSLVersion.GLSL_110, GLSLVersion.GLSL_130),
    TEXTURE3D_FUNCTION(GLSLVersion.GLSL_110, GLSLVersion.GLSL_130),
    TEXTURE_CUBE_FUNCTION(GLSLVersion.GLSL_110, GLSLVersion.GLSL_130),
    TEXTURE_GENERIC_FUNCTION(GLSLVersion.GLSL_130, null),
    TEXTURE_GATHER(GLSLVersion.GLSL_400, null),
    TEXTURE_QUERY_LOD(GLSLVersion.GLSL_400, null),
    TEXTURE_QUERY_LEVELS(GLSLVersion.GLSL_430, null),
    
    // Built-in variables
    GL_FRAGCOLOR(GLSLVersion.GLSL_110, GLSLVersion.GLSL_130),
    GL_FRAGDATA(GLSLVersion.GLSL_110, GLSLVersion.GLSL_130),
    USER_DEFINED_OUTPUT(GLSLVersion.GLSL_130, null),
    GL_VERTEX_ID(GLSLVersion.GLSL_130, null),
    GL_INSTANCE_ID(GLSLVersion.GLSL_140, null),
    
    // Matrix types
    NON_SQUARE_MATRICES(GLSLVersion.GLSL_120, null),
    DOUBLE_PRECISION(GLSLVersion.GLSL_400, null),
    
    // Interface blocks
    INTERFACE_BLOCKS(GLSLVersion.GLSL_150, null),
    UNIFORM_BLOCKS(GLSLVersion.GLSL_140, null),
    SHADER_STORAGE_BLOCKS(GLSLVersion.GLSL_430, null),
    
    // Layout qualifiers
    LAYOUT_LOCATION_INPUT(GLSLVersion.GLSL_330, null),
    LAYOUT_LOCATION_OUTPUT(GLSLVersion.GLSL_330, null),
    LAYOUT_BINDING(GLSLVersion.GLSL_420, null),
    LAYOUT_COMPONENT(GLSLVersion.GLSL_440, null),
    
    // Shader types
    GEOMETRY_SHADER(GLSLVersion.GLSL_150, null),
    TESSELLATION_SHADER(GLSLVersion.GLSL_400, null),
    COMPUTE_SHADER(GLSLVersion.GLSL_430, null),
    
    // Advanced features
    SUBROUTINES(GLSLVersion.GLSL_400, null),
    ATOMIC_COUNTERS(GLSLVersion.GLSL_420, null),
    IMAGE_LOAD_STORE(GLSLVersion.GLSL_420, null),
    SHARED_MEMORY(GLSLVersion.GLSL_430, null),
    
    // Interpolation
    FLAT_INTERPOLATION(GLSLVersion.GLSL_130, null),
    NOPERSPECTIVE_INTERPOLATION(GLSLVersion.GLSL_130, null),
    CENTROID_INTERPOLATION(GLSLVersion.GLSL_120, null),
    SAMPLE_INTERPOLATION(GLSLVersion.GLSL_400, null),
    
    // Precision
    PRECISION_QUALIFIERS(GLSLVersion.GLSL_130, null),
    
    // Functions
    INVERSE_FUNCTION(GLSLVersion.GLSL_140, null),
    DETERMINANT_FUNCTION(GLSLVersion.GLSL_150, null),
    BITWISE_OPERATIONS(GLSLVersion.GLSL_130, null),
    FMA_FUNCTION(GLSLVersion.GLSL_400, null),
    FREXP_LDEXP(GLSLVersion.GLSL_400, null),
    PACK_UNPACK(GLSLVersion.GLSL_330, null),
    
    // Derivative
    DFDX_FINE(GLSLVersion.GLSL_450, null),
    DFDY_FINE(GLSLVersion.GLSL_450, null),
    FWIDTH_FINE(GLSLVersion.GLSL_450, null),
    
    // Misc
    SWITCH_STATEMENT(GLSLVersion.GLSL_130, null),
    EXPLICIT_UNIFORM_LOCATION(GLSLVersion.GLSL_430, null),
    SHADER_IMAGE_SIZE(GLSLVersion.GLSL_430, null),
    CULL_DISTANCE(GLSLVersion.GLSL_450, null);
    
    public final GLSLVersion introducedIn;
    public final GLSLVersion deprecatedIn;
    
    GLSLFeature(GLSLVersion introduced, GLSLVersion deprecated) {
        this.introducedIn = introduced;
        this.deprecatedIn = deprecated;
    }
    
    public boolean isAvailableIn(GLSLVersion version) {
        return version.versionNumber >= introducedIn.versionNumber;
    }
    
    public boolean isDeprecatedIn(GLSLVersion version) {
        return deprecatedIn != null && version.versionNumber >= deprecatedIn.versionNumber;
    }
    
    public boolean isLegacyOnly() {
        return deprecatedIn != null;
    }
}

// ============================================================================
// VERSION MANAGER
// ============================================================================

final class GLSLVersionManager {
    
    // Pre-compiled patterns for version detection
    private static final int VERSION_DIRECTIVE_HASH = "#version".hashCode();
    
    GLSLVersion detectVersion(String source) {
        // Fast path: look for #version directive
        int idx = 0;
        int len = source.length();
        
        // Skip leading whitespace and comments
        while (idx < len) {
            char c = source.charAt(idx);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                idx++;
            } else if (c == '/' && idx + 1 < len) {
                char next = source.charAt(idx + 1);
                if (next == '/') {
                    // Line comment
                    idx += 2;
                    while (idx < len && source.charAt(idx) != '\n') idx++;
                    idx++;
                } else if (next == '*') {
                    // Block comment
                    idx += 2;
                    while (idx + 1 < len && !(source.charAt(idx) == '*' && 
                           source.charAt(idx + 1) == '/')) idx++;
                    idx += 2;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        
        // Check for #version
        if (idx + 8 < len && source.charAt(idx) == '#') {
            if (source.regionMatches(idx, "#version", 0, 8)) {
                idx += 8;
                
                // Skip whitespace
                while (idx < len && (source.charAt(idx) == ' ' || 
                       source.charAt(idx) == '\t')) idx++;
                
                // Parse version number
                int versionNum = 0;
                while (idx < len) {
                    char c = source.charAt(idx);
                    if (c >= '0' && c <= '9') {
                        versionNum = versionNum * 10 + (c - '0');
                        idx++;
                    } else {
                        break;
                    }
                }
                
                if (versionNum > 0) {
                    return GLSLVersion.fromNumber(versionNum);
                }
            }
        }
        
        // No version directive found - analyze content
        return inferVersionFromContent(source);
    }
    
    private GLSLVersion inferVersionFromContent(String source) {
        // Check for modern keywords
        if (containsKeyword(source, "layout")) return GLSLVersion.GLSL_330;
        if (containsKeyword(source, "in") || containsKeyword(source, "out")) {
            if (containsKeyword(source, "flat") || containsKeyword(source, "noperspective")) {
                return GLSLVersion.GLSL_130;
            }
        }
        if (containsKeyword(source, "attribute") || containsKeyword(source, "varying")) {
            return GLSLVersion.GLSL_110;
        }
        if (source.contains("texture(")) return GLSLVersion.GLSL_130;
        if (source.contains("texture2D(") || source.contains("texture3D(")) {
            return GLSLVersion.GLSL_110;
        }
        
        return GLSLVersion.GLSL_110;
    }
    
    private boolean containsKeyword(String source, String keyword) {
        int idx = source.indexOf(keyword);
        if (idx < 0) return false;
        
        // Check it's actually a keyword, not part of identifier
        if (idx > 0) {
            char before = source.charAt(idx - 1);
            if (Character.isLetterOrDigit(before) || before == '_') return false;
        }
        int end = idx + keyword.length();
        if (end < source.length()) {
            char after = source.charAt(end);
            if (Character.isLetterOrDigit(after) || after == '_') return false;
        }
        return true;
    }
    
    GLSLVersion parseVersionString(String glslVersion) {
        // Parse strings like "4.60 NVIDIA" or "3.30"
        if (glslVersion == null || glslVersion.isEmpty()) {
            return GLSLVersion.GLSL_110;
        }
        
        int major = 0;
        int minor = 0;
        int idx = 0;
        int len = glslVersion.length();
        
        // Skip leading whitespace
        while (idx < len && !Character.isDigit(glslVersion.charAt(idx))) idx++;
        
        // Parse major
        while (idx < len && Character.isDigit(glslVersion.charAt(idx))) {
            major = major * 10 + (glslVersion.charAt(idx) - '0');
            idx++;
        }
        
        // Skip dot
        if (idx < len && glslVersion.charAt(idx) == '.') idx++;
        
        // Parse minor
        while (idx < len && Character.isDigit(glslVersion.charAt(idx))) {
            minor = minor * 10 + (glslVersion.charAt(idx) - '0');
            idx++;
        }
        
        // Convert to version number
        int versionNum = major * 100 + minor;
        return GLSLVersion.fromNumber(versionNum);
    }
}

// ============================================================================
// CAPABILITY REGISTRY
// ============================================================================

final class GLSLCapabilityRegistry {
    
    private final EnumSet<GLSLFeature>[] featuresByVersion;
    private final Map<String, GLSLVersion> extensionVersionMap;
    private EnumSet<GLSLExtension> availableExtensions;
    
    @SuppressWarnings("unchecked")
    GLSLCapabilityRegistry() {
        featuresByVersion = new EnumSet[GLSLVersion.values().length];
        extensionVersionMap = new HashMap<>();
        availableExtensions = EnumSet.noneOf(GLSLExtension.class);
        
        initializeFeatures();
        initializeExtensionMap();
    }
    
    private void initializeFeatures() {
        for (int i = 0; i < featuresByVersion.length; i++) {
            featuresByVersion[i] = EnumSet.noneOf(GLSLFeature.class);
            GLSLVersion version = GLSLVersion.values()[i];
            
            for (GLSLFeature feature : GLSLFeature.values()) {
                if (feature.isAvailableIn(version)) {
                    featuresByVersion[i].add(feature);
                }
            }
        }
    }
    
    private void initializeExtensionMap() {
        // Extensions that provide features before they're core
        extensionVersionMap.put("GL_ARB_explicit_attrib_location", GLSLVersion.GLSL_120);
        extensionVersionMap.put("GL_ARB_explicit_uniform_location", GLSLVersion.GLSL_330);
        extensionVersionMap.put("GL_ARB_gpu_shader5", GLSLVersion.GLSL_330);
        extensionVersionMap.put("GL_ARB_shading_language_420pack", GLSLVersion.GLSL_330);
        extensionVersionMap.put("GL_ARB_shader_image_load_store", GLSLVersion.GLSL_330);
        extensionVersionMap.put("GL_ARB_compute_shader", GLSLVersion.GLSL_330);
        extensionVersionMap.put("GL_ARB_tessellation_shader", GLSLVersion.GLSL_330);
        extensionVersionMap.put("GL_ARB_geometry_shader4", GLSLVersion.GLSL_110);
        extensionVersionMap.put("GL_EXT_geometry_shader4", GLSLVersion.GLSL_110);
    }
    
    void detectCapabilities(OpenGLCallMapper gl) {
        // Query available extensions
        int numExtensions = gl.glGetInteger(OpenGLCallMapper.GL_NUM_EXTENSIONS);
        
        for (int i = 0; i < numExtensions; i++) {
            String ext = gl.glGetStringi(OpenGLCallMapper.GL_EXTENSIONS, i);
            if (ext != null) {
                GLSLExtension parsed = GLSLExtension.fromString(ext);
                if (parsed != null) {
                    availableExtensions.add(parsed);
                }
            }
        }
    }
    
    boolean isSupported(GLSLFeature feature, GLSLVersion version) {
        int ordinal = version.ordinal();
        return ordinal < featuresByVersion.length && featuresByVersion[ordinal].contains(feature);
    }
    
    boolean isExtensionAvailable(GLSLExtension extension) {
        return availableExtensions.contains(extension);
    }
    
    GLSLVersion getMinimumVersion(Set<GLSLFeature> features) {
        GLSLVersion min = GLSLVersion.GLSL_110;
        for (GLSLFeature feature : features) {
            if (feature.introducedIn.ordinal() > min.ordinal()) {
                min = feature.introducedIn;
            }
        }
        return min;
    }
    
    Set<GLSLFeature> getFeaturesFor(GLSLVersion version) {
        return Collections.unmodifiableSet(featuresByVersion[version.ordinal()]);
    }
}

// ============================================================================
// EXTENSION ENUM
// ============================================================================

enum GLSLExtension {
    ARB_EXPLICIT_ATTRIB_LOCATION("GL_ARB_explicit_attrib_location"),
    ARB_EXPLICIT_UNIFORM_LOCATION("GL_ARB_explicit_uniform_location"),
    ARB_GPU_SHADER5("GL_ARB_gpu_shader5"),
    ARB_SHADING_LANGUAGE_420PACK("GL_ARB_shading_language_420pack"),
    ARB_SHADER_IMAGE_LOAD_STORE("GL_ARB_shader_image_load_store"),
    ARB_COMPUTE_SHADER("GL_ARB_compute_shader"),
    ARB_TESSELLATION_SHADER("GL_ARB_tessellation_shader"),
    ARB_GEOMETRY_SHADER4("GL_ARB_geometry_shader4"),
    ARB_SHADER_STORAGE_BUFFER_OBJECT("GL_ARB_shader_storage_buffer_object"),
    ARB_UNIFORM_BUFFER_OBJECT("GL_ARB_uniform_buffer_object"),
    ARB_TEXTURE_GATHER("GL_ARB_texture_gather"),
    ARB_DERIVATIVE_CONTROL("GL_ARB_derivative_control"),
    ARB_CULL_DISTANCE("GL_ARB_cull_distance"),
    EXT_GEOMETRY_SHADER4("GL_EXT_geometry_shader4"),
    EXT_GPU_SHADER4("GL_EXT_gpu_shader4");
    
    public final String name;
    private static final Map<String, GLSLExtension> BY_NAME = new HashMap<>();
    
    static {
        for (GLSLExtension ext : values()) {
            BY_NAME.put(ext.name, ext);
        }
    }
    
    GLSLExtension(String name) {
        this.name = name;
    }
    
    public static GLSLExtension fromString(String name) {
        return BY_NAME.get(name);
    }
    
    public String getEnableDirective() {
        return "#extension " + name + " : enable";
    }
    
    public String getRequireDirective() {
        return "#extension " + name + " : require";
    }
}

// ============================================================================
// MEMORY POOL - ZERO ALLOCATION HOT PATHS
// ============================================================================

final class GLSLMemoryPool {
    
    // FFM Arena for off-heap memory management
    // Uses Arena.ofShared() for thread-safe access across the pool
    private Arena arena;
    private volatile boolean closed = false;
    
    // StringBuilder pool
    private final ArrayDeque<StringBuilder> stringBuilderPool = new ArrayDeque<>(32);
    private static final int SB_INITIAL_CAPACITY = 4096;
    private static final int SB_MAX_CAPACITY = 65536;
    
    // Char array pool
    private final ArrayDeque<char[]> charArrayPool = new ArrayDeque<>(16);
    private static final int CHAR_ARRAY_SIZE = 8192;
    
    // Token list pool
    private final ArrayDeque<ArrayList<GLSLToken>> tokenListPool = new ArrayDeque<>(16);
    
    // AST node pools
    private final Map<Class<?>, ArrayDeque<Object>> nodePoolMap = new ConcurrentHashMap<>();
    
    // Int array pools by size
    private final int[][] intArrayPools = new int[8][];
    private final int[] intArraySizes = {16, 32, 64, 128, 256, 512, 1024, 4096};
    
    // Stats
    private final AtomicLong allocations = new AtomicLong();
    private final AtomicLong poolHits = new AtomicLong();
    
    private static final Logger LOGGER = LogManager.getLogger("GLSLMemoryPool");
    
    GLSLMemoryPool() {
        // Create shared arena for thread-safe off-heap memory
        this.arena = Arena.ofShared();
        
        // Pre-allocate some objects
        for (int i = 0; i < 8; i++) {
            stringBuilderPool.offer(new StringBuilder(SB_INITIAL_CAPACITY));
        }
        for (int i = 0; i < 4; i++) {
            charArrayPool.offer(new char[CHAR_ARRAY_SIZE]);
        }
        for (int i = 0; i < 8; i++) {
            tokenListPool.offer(new ArrayList<>(256));
        }
    }
    
    StringBuilder acquireStringBuilder() {
        StringBuilder sb;
        synchronized (stringBuilderPool) {
            sb = stringBuilderPool.poll();
        }
        if (sb == null) {
            allocations.incrementAndGet();
            return new StringBuilder(SB_INITIAL_CAPACITY);
        }
        poolHits.incrementAndGet();
        sb.setLength(0);
        return sb;
    }
    
    void releaseStringBuilder(StringBuilder sb) {
        if (sb == null) return;
        if (sb.capacity() > SB_MAX_CAPACITY) {
            // Too large, let GC handle it
            return;
        }
        sb.setLength(0);
        synchronized (stringBuilderPool) {
            if (stringBuilderPool.size() < 32) {
                stringBuilderPool.offer(sb);
            }
        }
    }
    
    char[] acquireCharArray() {
        char[] arr;
        synchronized (charArrayPool) {
            arr = charArrayPool.poll();
        }
        if (arr == null) {
            allocations.incrementAndGet();
            return new char[CHAR_ARRAY_SIZE];
        }
        poolHits.incrementAndGet();
        return arr;
    }
    
    void releaseCharArray(char[] arr) {
        if (arr == null || arr.length != CHAR_ARRAY_SIZE) return;
        synchronized (charArrayPool) {
            if (charArrayPool.size() < 16) {
                charArrayPool.offer(arr);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    ArrayList<GLSLToken> acquireTokenList() {
        ArrayList<GLSLToken> list;
        synchronized (tokenListPool) {
            list = tokenListPool.poll();
        }
        if (list == null) {
            allocations.incrementAndGet();
            return new ArrayList<>(256);
        }
        poolHits.incrementAndGet();
        list.clear();
        return list;
    }
    
    void releaseTokenList(ArrayList<GLSLToken> list) {
        if (list == null) return;
        list.clear();
        synchronized (tokenListPool) {
            if (tokenListPool.size() < 16) {
                tokenListPool.offer(list);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    <T> T acquireNode(Class<T> type, Supplier<T> factory) {
        ArrayDeque<Object> pool = nodePoolMap.get(type);
        if (pool != null) {
            synchronized (pool) {
                Object node = pool.poll();
                if (node != null) {
                    poolHits.incrementAndGet();
                    return (T) node;
                }
            }
        }
        allocations.incrementAndGet();
        return factory.get();
    }
    
    <T> void releaseNode(Class<T> type, T node) {
        if (node == null) return;
        ArrayDeque<Object> pool = nodePoolMap.computeIfAbsent(type, 
            k -> new ArrayDeque<>(64));
        synchronized (pool) {
            if (pool.size() < 64) {
                pool.offer(node);
            }
        }
    }
    
    void releaseAll() {
        synchronized (stringBuilderPool) { stringBuilderPool.clear(); }
        synchronized (charArrayPool) { charArrayPool.clear(); }
        synchronized (tokenListPool) { tokenListPool.clear(); }
        nodePoolMap.clear();
    }
    
    /**
     * Get the FFM Arena for off-heap memory allocations
     * This is used for zero-copy buffer operations
     */
    Arena getArena() {
        if (closed) {
            throw new IllegalStateException("GLSLMemoryPool has been closed");
        }
        return arena;
    }
    
    /**
     * Shutdown the memory pool and close the FFM Arena
     * 
     * CRITICAL: Must be called on game shutdown to prevent memory-mapped file locks
     * and to properly free off-heap memory.
     * 
     * After calling this method, the pool cannot be used anymore.
     */
    void shutdown() {
        if (closed) {
            return;
        }
        
        synchronized (this) {
            if (closed) {
                return;
            }
            
            LOGGER.info("Shutting down GLSLMemoryPool");
            
            // Clear all pools first
            releaseAll();
            
            // Close the FFM Arena - this frees all off-heap memory
            // and releases any memory-mapped file locks
            try {
                if (arena != null) {
                    arena.close();
                    LOGGER.info("FFM Arena closed successfully");
                }
            } catch (Exception e) {
                LOGGER.error("Error closing FFM Arena", e);
            } finally {
                arena = null;
                closed = true;
            }
            
            long totalOps = allocations.get() + poolHits.get();
            if (totalOps > 0) {
                LOGGER.info(String.format("GLSLMemoryPool stats - Allocations: %d, Pool hits: %d, Hit rate: %.2f%%",
                    allocations.get(), poolHits.get(), getHitRate() * 100));
            }
        }
    }
    
    /**
     * Check if the pool is closed
     */
    boolean isClosed() {
        return closed;
    }
    
    void trim() {
        synchronized (stringBuilderPool) {
            while (stringBuilderPool.size() > 4) stringBuilderPool.poll();
        }
        synchronized (charArrayPool) {
            while (charArrayPool.size() > 2) charArrayPool.poll();
        }
        synchronized (tokenListPool) {
            while (tokenListPool.size() > 4) tokenListPool.poll();
        }
        for (ArrayDeque<Object> pool : nodePoolMap.values()) {
            synchronized (pool) {
                while (pool.size() > 16) pool.poll();
            }
        }
    }
    
    long getAllocations() { return allocations.get(); }
    long getPoolHits() { return poolHits.get(); }
    double getHitRate() {
        long total = allocations.get() + poolHits.get();
        return total > 0 ? (double) poolHits.get() / total : 0;
    }
}

// ============================================================================
// TRANSLATION CACHE
// ============================================================================

final class GLSLTranslationCache {
    
    private static final int MAX_ENTRIES = 256;
    private static final int INITIAL_CAPACITY = 64;
    
    private final GLSLMemoryPool memoryPool;
    private final ConcurrentHashMap<Long, CacheEntry> cache;
    private final AtomicInteger size = new AtomicInteger(0);
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    
    GLSLTranslationCache(GLSLMemoryPool pool) {
        this.memoryPool = pool;
        this.cache = new ConcurrentHashMap<>(INITIAL_CAPACITY);
    }
    
    GLSLTranslationResult get(long key) {
        CacheEntry entry = cache.get(key);
        if (entry != null) {
            entry.accessCount++;
            entry.lastAccess = System.nanoTime();
            hits.incrementAndGet();
            return entry.result;
        }
        misses.incrementAndGet();
        return null;
    }
    
    void put(long key, GLSLTranslationResult result) {
        if (size.get() >= MAX_ENTRIES) {
            evict();
        }
        
        CacheEntry entry = new CacheEntry(result);
        if (cache.putIfAbsent(key, entry) == null) {
            size.incrementAndGet();
        }
    }
    
    private void evict() {
        // LFU with aging - evict 25% of entries
        int toEvict = MAX_ENTRIES / 4;
        long now = System.nanoTime();
        
        // Find entries with lowest score
        long[] scores = new long[toEvict];
        Long[] keys = new Long[toEvict];
        Arrays.fill(scores, Long.MAX_VALUE);
        
        for (Map.Entry<Long, CacheEntry> e : cache.entrySet()) {
            CacheEntry entry = e.getValue();
            // Score: access count with time decay
            long age = (now - entry.lastAccess) / 1_000_000_000L; // seconds
            long score = entry.accessCount - age;
            
            // Insert into sorted array if lower
            for (int i = 0; i < toEvict; i++) {
                if (score < scores[i]) {
                    // Shift down
                    System.arraycopy(scores, i, scores, i + 1, toEvict - i - 1);
                    System.arraycopy(keys, i, keys, i + 1, toEvict - i - 1);
                    scores[i] = score;
                    keys[i] = e.getKey();
                    break;
                }
            }
        }
        
        // Remove lowest scoring entries
        for (Long key : keys) {
            if (key != null && cache.remove(key) != null) {
                size.decrementAndGet();
            }
        }
    }
    
    void clear() {
        cache.clear();
        size.set(0);
    }
    
    void trim() {
        if (size.get() > INITIAL_CAPACITY) {
            evict();
        }
    }
    
    double getHitRate() {
        long total = hits.get() + misses.get();
        return total > 0 ? (double) hits.get() / total : 0;
    }
    
    private static class CacheEntry {
        final GLSLTranslationResult result;
        volatile int accessCount = 1;
        volatile long lastAccess;
        
        CacheEntry(GLSLTranslationResult result) {
            this.result = result;
            this.lastAccess = System.nanoTime();
        }
    }
}

// ============================================================================
// METRICS
// ============================================================================

final class GLSLMetrics {
    
    private final AtomicLong translationCount = new AtomicLong();
    private final AtomicLong totalTranslationTimeNanos = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();
    private final AtomicLong optimizationTimeNanos = new AtomicLong();
    private final AtomicLong parseTimeNanos = new AtomicLong();
    private final AtomicLong codegenTimeNanos = new AtomicLong();
    
    void recordTranslation(long nanos) {
        translationCount.incrementAndGet();
        totalTranslationTimeNanos.addAndGet(nanos);
    }
    
    void recordCacheHit() { cacheHits.incrementAndGet(); }
    void recordCacheMiss() { cacheMisses.incrementAndGet(); }
    void recordOptimizationTime(long nanos) { optimizationTimeNanos.addAndGet(nanos); }
    void recordParseTime(long nanos) { parseTimeNanos.addAndGet(nanos); }
    void recordCodegenTime(long nanos) { codegenTimeNanos.addAndGet(nanos); }
    
    long getTranslationCount() { return translationCount.get(); }
    double getAverageTranslationTimeMs() {
        long count = translationCount.get();
        return count > 0 ? totalTranslationTimeNanos.get() / count / 1_000_000.0 : 0;
    }
    double getCacheHitRate() {
        long total = cacheHits.get() + cacheMisses.get();
        return total > 0 ? (double) cacheHits.get() / total : 0;
    }
    
    void reset() {
        translationCount.set(0);
        totalTranslationTimeNanos.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        optimizationTimeNanos.set(0);
        parseTimeNanos.set(0);
        codegenTimeNanos.set(0);
    }
    
    @Override
    public String toString() {
        return String.format(
            "GLSLMetrics{translations=%d, avgTime=%.2fms, cacheHit=%.1f%%}",
            translationCount.get(),
            getAverageTranslationTimeMs(),
            getCacheHitRate() * 100
        );
    }
}

// ============================================================================
// RESULT CLASSES
// ============================================================================

final class GLSLTranslationResult {
    
    private final String translatedSource;
    private final GLSLVersion sourceVersion;
    private final GLSLVersion targetVersion;
    private final GLSLShaderType shaderType;
    private final Set<GLSLExtension> requiredExtensions;
    private final List<String> warnings;
    private final long translationTimeNanos;
    
    GLSLTranslationResult(String translatedSource, GLSLVersion sourceVersion,
                          GLSLVersion targetVersion, GLSLShaderType shaderType,
                          Set<GLSLExtension> requiredExtensions, List<String> warnings,
                          long translationTimeNanos) {
        this.translatedSource = translatedSource;
        this.sourceVersion = sourceVersion;
        this.targetVersion = targetVersion;
        this.shaderType = shaderType;
        this.requiredExtensions = requiredExtensions != null ? 
            Collections.unmodifiableSet(requiredExtensions) : Collections.emptySet();
        this.warnings = warnings != null ? 
            Collections.unmodifiableList(warnings) : Collections.emptyList();
        this.translationTimeNanos = translationTimeNanos;
    }
    
    public String getSource() { return translatedSource; }
    public GLSLVersion getSourceVersion() { return sourceVersion; }
    public GLSLVersion getTargetVersion() { return targetVersion; }
    public GLSLShaderType getShaderType() { return shaderType; }
    public Set<GLSLExtension> getRequiredExtensions() { return requiredExtensions; }
    public List<String> getWarnings() { return warnings; }
    public double getTranslationTimeMs() { return translationTimeNanos / 1_000_000.0; }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
}

final class GLSLValidationResult {
    
    private final boolean valid;
    private final List<GLSLError> errors;
    private final List<String> warnings;
    
    GLSLValidationResult(boolean valid, List<GLSLError> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = errors != null ? Collections.unmodifiableList(errors) : Collections.emptyList();
        this.warnings = warnings != null ? Collections.unmodifiableList(warnings) : Collections.emptyList();
    }
    
    public boolean isValid() { return valid; }
    public List<GLSLError> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
}

final class GLSLError {
    public final int line;
    public final int column;
    public final String message;
    public final GLSLErrorType type;
    
    GLSLError(int line, int column, String message, GLSLErrorType type) {
        this.line = line;
        this.column = column;
        this.message = message;
        this.type = type;
    }
    
    @Override
    public String toString() {
        return String.format("%s at %d:%d: %s", type, line, column, message);
    }
}

enum GLSLErrorType {
    SYNTAX_ERROR,
    UNSUPPORTED_FEATURE,
    TYPE_ERROR,
    UNDEFINED_SYMBOL,
    REDEFINITION,
    VERSION_MISMATCH
}

final class GLSLShaderSource {
    private final String source;
    private final GLSLShaderType type;
    private final GLSLVersion sourceVersion;
    private final GLSLVersion targetVersion;
    
    public GLSLShaderSource(String source, GLSLShaderType type) {
        this(source, type, null, null);
    }
    
    public GLSLShaderSource(String source, GLSLShaderType type, 
                            GLSLVersion sourceVersion, GLSLVersion targetVersion) {
        this.source = source;
        this.type = type;
        this.sourceVersion = sourceVersion;
        this.targetVersion = targetVersion;
    }
    
    public String getSource() { return source; }
    public GLSLShaderType getType() { return type; }
    public GLSLVersion getSourceVersion() { return sourceVersion; }
    public GLSLVersion getTargetVersion() { return targetVersion; }
}

// ============================================================================
// EXCEPTIONS
// ============================================================================

class GLSLException extends RuntimeException {
    public GLSLException(String message) { super(message); }
    public GLSLException(String message, Throwable cause) { super(message, cause); }
}

class GLSLVersionException extends GLSLException {
    public GLSLVersionException(String message) { super(message); }
}

class GLSLParseException extends GLSLException {
    private final List<GLSLError> errors;
    
    public GLSLParseException(String message, List<GLSLError> errors) {
        super(message);
        this.errors = errors;
    }
    
    public List<GLSLError> getErrors() { return errors; }
}

class GLSLTranslationException extends GLSLException {
    public GLSLTranslationException(String message) { super(message); }
}

// ============================================================================
// PLACEHOLDER CLASSES (To be implemented in later parts)
// ============================================================================

// Forward declarations - will be fully implemented in Parts 2-5

class GLSLTokenizer {
    private final GLSLMemoryPool pool;
    GLSLTokenizer(GLSLMemoryPool pool) { this.pool = pool; }
    GLSLTokenStream tokenize(String source) { throw new UnsupportedOperationException(); }
    void reset() {}
}

class GLSLParser {
    private final GLSLMemoryPool pool;
    GLSLParser(GLSLMemoryPool pool) { this.pool = pool; }
    GLSLShaderAST parse(GLSLTokenStream tokens, GLSLShaderType type) { 
        throw new UnsupportedOperationException(); 
    }
    void reset() {}
}

class GLSLTokenStream {
    // Implemented in Part 2
}

class GLSLToken {
    // Implemented in Part 2
}

class GLSLShaderAST {
    Set<GLSLExtension> getRequiredExtensions() { return Collections.emptySet(); }
    List<String> getWarnings() { return Collections.emptyList(); }
}

class GLSLOptimizer {
    private final GLSLMemoryPool pool;
    GLSLOptimizer(GLSLMemoryPool pool) { this.pool = pool; }
    GLSLShaderAST optimize(GLSLShaderAST ast, int level) { return ast; }
}

class GLSLCodeGenerator {
    private final GLSLMemoryPool pool;
    GLSLCodeGenerator(GLSLMemoryPool pool) { this.pool = pool; }
    String generate(GLSLShaderAST ast, GLSLVersion version) { return ""; }
}

class GLSLTranslationEngine {
    private final GLSLCallMapper mapper;
    GLSLTranslationEngine(GLSLCallMapper mapper) { this.mapper = mapper; }
    GLSLShaderAST translate(GLSLShaderAST ast, GLSLVersion from, GLSLVersion to, 
                            GLSLShaderType type) { return ast; }
    GLSLValidationResult validate(GLSLShaderAST ast, GLSLVersion version, 
                                  GLSLShaderType type) { 
        return new GLSLValidationResult(true, null, null); 
    }
}

// ============================================================================
// OBJECT POOLS FOR COMPONENTS
// ============================================================================

final class GLSLTokenizerPool {
    private final ArrayDeque<GLSLTokenizer> pool = new ArrayDeque<>(8);
    private final GLSLMemoryPool memoryPool;
    
    GLSLTokenizerPool(GLSLMemoryPool memoryPool) {
        this.memoryPool = memoryPool;
        // Pre-allocate
        for (int i = 0; i < 4; i++) {
            pool.offer(new GLSLTokenizer(memoryPool));
        }
    }
    
    GLSLTokenizer acquire() {
        GLSLTokenizer tokenizer;
        synchronized (pool) {
            tokenizer = pool.poll();
        }
        if (tokenizer == null) {
            tokenizer = new GLSLTokenizer(memoryPool);
        }
        return tokenizer;
    }
    
    void release(GLSLTokenizer tokenizer) {
        if (tokenizer == null) return;
        tokenizer.reset();
        synchronized (pool) {
            if (pool.size() < 8) {
                pool.offer(tokenizer);
            }
        }
    }
    
    void releaseAll() {
        synchronized (pool) {
            pool.clear();
        }
    }
    
    void trim() {
        synchronized (pool) {
            while (pool.size() > 2) pool.poll();
        }
    }
}

final class GLSLParserPool {
    private final ArrayDeque<GLSLParser> pool = new ArrayDeque<>(8);
    private final GLSLMemoryPool memoryPool;
    
    GLSLParserPool(GLSLMemoryPool memoryPool) {
        this.memoryPool = memoryPool;
        for (int i = 0; i < 4; i++) {
            pool.offer(new GLSLParser(memoryPool));
        }
    }
    
    GLSLParser acquire() {
        GLSLParser parser;
        synchronized (pool) {
            parser = pool.poll();
        }
        if (parser == null) {
            parser = new GLSLParser(memoryPool);
        }
        return parser;
    }
    
    void release(GLSLParser parser) {
        if (parser == null) return;
        parser.reset();
        synchronized (pool) {
            if (pool.size() < 8) {
                pool.offer(parser);
            }
        }
    }
    
    void releaseAll() {
        synchronized (pool) {
            pool.clear();
        }
    }
    
    void trim() {
        synchronized (pool) {
            while (pool.size() > 2) pool.poll();
        }
    }
}

// ============================================================================
// TOKEN TYPES - Complete GLSL Token Classification
// ============================================================================

/**
 * Complete enumeration of all GLSL token types.
 * Organized by category for fast lookup and classification.
 */
enum GLSLTokenType {
    // ==================== LITERALS ====================
    INT_LITERAL(Category.LITERAL),
    UINT_LITERAL(Category.LITERAL),
    FLOAT_LITERAL(Category.LITERAL),
    DOUBLE_LITERAL(Category.LITERAL),
    BOOL_LITERAL(Category.LITERAL),
    
    // ==================== IDENTIFIERS ====================
    IDENTIFIER(Category.IDENTIFIER),
    
    // ==================== KEYWORDS - Types ====================
    VOID(Category.TYPE_KEYWORD),
    BOOL(Category.TYPE_KEYWORD),
    INT(Category.TYPE_KEYWORD),
    UINT(Category.TYPE_KEYWORD),
    FLOAT(Category.TYPE_KEYWORD),
    DOUBLE(Category.TYPE_KEYWORD),
    
    // Vector types
    VEC2(Category.TYPE_KEYWORD), VEC3(Category.TYPE_KEYWORD), VEC4(Category.TYPE_KEYWORD),
    DVEC2(Category.TYPE_KEYWORD), DVEC3(Category.TYPE_KEYWORD), DVEC4(Category.TYPE_KEYWORD),
    BVEC2(Category.TYPE_KEYWORD), BVEC3(Category.TYPE_KEYWORD), BVEC4(Category.TYPE_KEYWORD),
    IVEC2(Category.TYPE_KEYWORD), IVEC3(Category.TYPE_KEYWORD), IVEC4(Category.TYPE_KEYWORD),
    UVEC2(Category.TYPE_KEYWORD), UVEC3(Category.TYPE_KEYWORD), UVEC4(Category.TYPE_KEYWORD),
    
    // Matrix types
    MAT2(Category.TYPE_KEYWORD), MAT3(Category.TYPE_KEYWORD), MAT4(Category.TYPE_KEYWORD),
    MAT2X2(Category.TYPE_KEYWORD), MAT2X3(Category.TYPE_KEYWORD), MAT2X4(Category.TYPE_KEYWORD),
    MAT3X2(Category.TYPE_KEYWORD), MAT3X3(Category.TYPE_KEYWORD), MAT3X4(Category.TYPE_KEYWORD),
    MAT4X2(Category.TYPE_KEYWORD), MAT4X3(Category.TYPE_KEYWORD), MAT4X4(Category.TYPE_KEYWORD),
    DMAT2(Category.TYPE_KEYWORD), DMAT3(Category.TYPE_KEYWORD), DMAT4(Category.TYPE_KEYWORD),
    DMAT2X2(Category.TYPE_KEYWORD), DMAT2X3(Category.TYPE_KEYWORD), DMAT2X4(Category.TYPE_KEYWORD),
    DMAT3X2(Category.TYPE_KEYWORD), DMAT3X3(Category.TYPE_KEYWORD), DMAT3X4(Category.TYPE_KEYWORD),
    DMAT4X2(Category.TYPE_KEYWORD), DMAT4X3(Category.TYPE_KEYWORD), DMAT4X4(Category.TYPE_KEYWORD),
    
    // Sampler types
    SAMPLER1D(Category.TYPE_KEYWORD), SAMPLER2D(Category.TYPE_KEYWORD), SAMPLER3D(Category.TYPE_KEYWORD),
    SAMPLERCUBE(Category.TYPE_KEYWORD), SAMPLER1DSHADOW(Category.TYPE_KEYWORD),
    SAMPLER2DSHADOW(Category.TYPE_KEYWORD), SAMPLERCUBESHADOW(Category.TYPE_KEYWORD),
    SAMPLER1DARRAY(Category.TYPE_KEYWORD), SAMPLER2DARRAY(Category.TYPE_KEYWORD),
    SAMPLER1DARRAYSHADOW(Category.TYPE_KEYWORD), SAMPLER2DARRAYSHADOW(Category.TYPE_KEYWORD),
    SAMPLERCUBEARRAY(Category.TYPE_KEYWORD), SAMPLERCUBEARRAYSHADOW(Category.TYPE_KEYWORD),
    ISAMPLER1D(Category.TYPE_KEYWORD), ISAMPLER2D(Category.TYPE_KEYWORD), ISAMPLER3D(Category.TYPE_KEYWORD),
    ISAMPLERCUBE(Category.TYPE_KEYWORD), ISAMPLER1DARRAY(Category.TYPE_KEYWORD),
    ISAMPLER2DARRAY(Category.TYPE_KEYWORD), ISAMPLERCUBEARRAY(Category.TYPE_KEYWORD),
    USAMPLER1D(Category.TYPE_KEYWORD), USAMPLER2D(Category.TYPE_KEYWORD), USAMPLER3D(Category.TYPE_KEYWORD),
    USAMPLERCUBE(Category.TYPE_KEYWORD), USAMPLER1DARRAY(Category.TYPE_KEYWORD),
    USAMPLER2DARRAY(Category.TYPE_KEYWORD), USAMPLERCUBEARRAY(Category.TYPE_KEYWORD),
    SAMPLER2DRECT(Category.TYPE_KEYWORD), SAMPLER2DRECTSHADOW(Category.TYPE_KEYWORD),
    ISAMPLER2DRECT(Category.TYPE_KEYWORD), USAMPLER2DRECT(Category.TYPE_KEYWORD),
    SAMPLERBUFFER(Category.TYPE_KEYWORD), ISAMPLERBUFFER(Category.TYPE_KEYWORD),
    USAMPLERBUFFER(Category.TYPE_KEYWORD), SAMPLER2DMS(Category.TYPE_KEYWORD),
    ISAMPLER2DMS(Category.TYPE_KEYWORD), USAMPLER2DMS(Category.TYPE_KEYWORD),
    SAMPLER2DMSARRAY(Category.TYPE_KEYWORD), ISAMPLER2DMSARRAY(Category.TYPE_KEYWORD),
    USAMPLER2DMSARRAY(Category.TYPE_KEYWORD),
    
    // Image types
    IMAGE1D(Category.TYPE_KEYWORD), IMAGE2D(Category.TYPE_KEYWORD), IMAGE3D(Category.TYPE_KEYWORD),
    IMAGE2DRECT(Category.TYPE_KEYWORD), IMAGECUBE(Category.TYPE_KEYWORD),
    IMAGEBUFFER(Category.TYPE_KEYWORD), IMAGE1DARRAY(Category.TYPE_KEYWORD),
    IMAGE2DARRAY(Category.TYPE_KEYWORD), IMAGECUBEARRAY(Category.TYPE_KEYWORD),
    IMAGE2DMS(Category.TYPE_KEYWORD), IMAGE2DMSARRAY(Category.TYPE_KEYWORD),
    IIMAGE1D(Category.TYPE_KEYWORD), IIMAGE2D(Category.TYPE_KEYWORD), IIMAGE3D(Category.TYPE_KEYWORD),
    IIMAGE2DRECT(Category.TYPE_KEYWORD), IIMAGECUBE(Category.TYPE_KEYWORD),
    IIMAGEBUFFER(Category.TYPE_KEYWORD), IIMAGE1DARRAY(Category.TYPE_KEYWORD),
    IIMAGE2DARRAY(Category.TYPE_KEYWORD), IIMAGECUBEARRAY(Category.TYPE_KEYWORD),
    IIMAGE2DMS(Category.TYPE_KEYWORD), IIMAGE2DMSARRAY(Category.TYPE_KEYWORD),
    UIMAGE1D(Category.TYPE_KEYWORD), UIMAGE2D(Category.TYPE_KEYWORD), UIMAGE3D(Category.TYPE_KEYWORD),
    UIMAGE2DRECT(Category.TYPE_KEYWORD), UIMAGECUBE(Category.TYPE_KEYWORD),
    UIMAGEBUFFER(Category.TYPE_KEYWORD), UIMAGE1DARRAY(Category.TYPE_KEYWORD),
    UIMAGE2DARRAY(Category.TYPE_KEYWORD), UIMAGECUBEARRAY(Category.TYPE_KEYWORD),
    UIMAGE2DMS(Category.TYPE_KEYWORD), UIMAGE2DMSARRAY(Category.TYPE_KEYWORD),
    
    // Atomic counter
    ATOMIC_UINT(Category.TYPE_KEYWORD),
    
    // ==================== KEYWORDS - Storage Qualifiers ====================
    CONST(Category.QUALIFIER_KEYWORD),
    IN(Category.QUALIFIER_KEYWORD),
    OUT(Category.QUALIFIER_KEYWORD),
    INOUT(Category.QUALIFIER_KEYWORD),
    UNIFORM(Category.QUALIFIER_KEYWORD),
    BUFFER(Category.QUALIFIER_KEYWORD),
    SHARED(Category.QUALIFIER_KEYWORD),
    ATTRIBUTE(Category.QUALIFIER_KEYWORD),  // Legacy
    VARYING(Category.QUALIFIER_KEYWORD),    // Legacy
    CENTROID(Category.QUALIFIER_KEYWORD),
    SAMPLE(Category.QUALIFIER_KEYWORD),
    PATCH(Category.QUALIFIER_KEYWORD),
    
    // ==================== KEYWORDS - Layout Qualifiers ====================
    LAYOUT(Category.QUALIFIER_KEYWORD),
    
    // ==================== KEYWORDS - Interpolation Qualifiers ====================
    FLAT(Category.QUALIFIER_KEYWORD),
    SMOOTH(Category.QUALIFIER_KEYWORD),
    NOPERSPECTIVE(Category.QUALIFIER_KEYWORD),
    
    // ==================== KEYWORDS - Precision Qualifiers ====================
    HIGHP(Category.QUALIFIER_KEYWORD),
    MEDIUMP(Category.QUALIFIER_KEYWORD),
    LOWP(Category.QUALIFIER_KEYWORD),
    PRECISION(Category.QUALIFIER_KEYWORD),
    
    // ==================== KEYWORDS - Invariant ====================
    INVARIANT(Category.QUALIFIER_KEYWORD),
    PRECISE(Category.QUALIFIER_KEYWORD),
    
    // ==================== KEYWORDS - Memory Qualifiers ====================
    COHERENT(Category.QUALIFIER_KEYWORD),
    VOLATILE(Category.QUALIFIER_KEYWORD),
    RESTRICT(Category.QUALIFIER_KEYWORD),
    READONLY(Category.QUALIFIER_KEYWORD),
    WRITEONLY(Category.QUALIFIER_KEYWORD),
    
    // ==================== KEYWORDS - Control Flow ====================
    IF(Category.CONTROL_KEYWORD),
    ELSE(Category.CONTROL_KEYWORD),
    SWITCH(Category.CONTROL_KEYWORD),
    CASE(Category.CONTROL_KEYWORD),
    DEFAULT(Category.CONTROL_KEYWORD),
    FOR(Category.CONTROL_KEYWORD),
    WHILE(Category.CONTROL_KEYWORD),
    DO(Category.CONTROL_KEYWORD),
    BREAK(Category.CONTROL_KEYWORD),
    CONTINUE(Category.CONTROL_KEYWORD),
    RETURN(Category.CONTROL_KEYWORD),
    DISCARD(Category.CONTROL_KEYWORD),
    
    // ==================== KEYWORDS - Struct/Interface ====================
    STRUCT(Category.STRUCT_KEYWORD),
    SUBROUTINE(Category.STRUCT_KEYWORD),
    
    // ==================== OPERATORS - Arithmetic ====================
    PLUS(Category.OPERATOR, "+"),
    MINUS(Category.OPERATOR, "-"),
    STAR(Category.OPERATOR, "*"),
    SLASH(Category.OPERATOR, "/"),
    PERCENT(Category.OPERATOR, "%"),
    
    // ==================== OPERATORS - Increment/Decrement ====================
    INCREMENT(Category.OPERATOR, "++"),
    DECREMENT(Category.OPERATOR, "--"),
    
    // ==================== OPERATORS - Comparison ====================
    EQ(Category.OPERATOR, "=="),
    NE(Category.OPERATOR, "!="),
    LT(Category.OPERATOR, "<"),
    GT(Category.OPERATOR, ">"),
    LE(Category.OPERATOR, "<="),
    GE(Category.OPERATOR, ">="),
    
    // ==================== OPERATORS - Logical ====================
    AND(Category.OPERATOR, "&&"),
    OR(Category.OPERATOR, "||"),
    XOR(Category.OPERATOR, "^^"),
    NOT(Category.OPERATOR, "!"),
    
    // ==================== OPERATORS - Bitwise ====================
    AMPERSAND(Category.OPERATOR, "&"),
    PIPE(Category.OPERATOR, "|"),
    CARET(Category.OPERATOR, "^"),
    TILDE(Category.OPERATOR, "~"),
    LEFT_SHIFT(Category.OPERATOR, "<<"),
    RIGHT_SHIFT(Category.OPERATOR, ">>"),
    
    // ==================== OPERATORS - Assignment ====================
    ASSIGN(Category.OPERATOR, "="),
    ADD_ASSIGN(Category.OPERATOR, "+="),
    SUB_ASSIGN(Category.OPERATOR, "-="),
    MUL_ASSIGN(Category.OPERATOR, "*="),
    DIV_ASSIGN(Category.OPERATOR, "/="),
    MOD_ASSIGN(Category.OPERATOR, "%="),
    LEFT_SHIFT_ASSIGN(Category.OPERATOR, "<<="),
    RIGHT_SHIFT_ASSIGN(Category.OPERATOR, ">>="),
    AND_ASSIGN(Category.OPERATOR, "&="),
    XOR_ASSIGN(Category.OPERATOR, "^="),
    OR_ASSIGN(Category.OPERATOR, "|="),
    
    // ==================== OPERATORS - Ternary ====================
    QUESTION(Category.OPERATOR, "?"),
    COLON(Category.OPERATOR, ":"),
    
    // ==================== PUNCTUATION ====================
    SEMICOLON(Category.PUNCTUATION, ";"),
    COMMA(Category.PUNCTUATION, ","),
    DOT(Category.PUNCTUATION, "."),
    LEFT_PAREN(Category.PUNCTUATION, "("),
    RIGHT_PAREN(Category.PUNCTUATION, ")"),
    LEFT_BRACKET(Category.PUNCTUATION, "["),
    RIGHT_BRACKET(Category.PUNCTUATION, "]"),
    LEFT_BRACE(Category.PUNCTUATION, "{"),
    RIGHT_BRACE(Category.PUNCTUATION, "}"),
    
    // ==================== PREPROCESSOR ====================
    HASH(Category.PREPROCESSOR, "#"),
    PP_VERSION(Category.PREPROCESSOR),
    PP_EXTENSION(Category.PREPROCESSOR),
    PP_LINE(Category.PREPROCESSOR),
    PP_DEFINE(Category.PREPROCESSOR),
    PP_UNDEF(Category.PREPROCESSOR),
    PP_IF(Category.PREPROCESSOR),
    PP_IFDEF(Category.PREPROCESSOR),
    PP_IFNDEF(Category.PREPROCESSOR),
    PP_ELSE(Category.PREPROCESSOR),
    PP_ELIF(Category.PREPROCESSOR),
    PP_ENDIF(Category.PREPROCESSOR),
    PP_ERROR(Category.PREPROCESSOR),
    PP_PRAGMA(Category.PREPROCESSOR),
    
    // ==================== SPECIAL ====================
    EOF(Category.SPECIAL),
    ERROR(Category.SPECIAL),
    NEWLINE(Category.SPECIAL),
    WHITESPACE(Category.SPECIAL),
    COMMENT(Category.SPECIAL),
    
    // ==================== BUILT-IN VARIABLES ====================
    GL_POSITION(Category.BUILTIN),
    GL_POINTSIZE(Category.BUILTIN),
    GL_CLIPDISTANCE(Category.BUILTIN),
    GL_CULLDISTANCE(Category.BUILTIN),
    GL_VERTEXID(Category.BUILTIN),
    GL_INSTANCEID(Category.BUILTIN),
    GL_PRIMITIVEID(Category.BUILTIN),
    GL_INVOCATIONID(Category.BUILTIN),
    GL_LAYER(Category.BUILTIN),
    GL_VIEWPORTINDEX(Category.BUILTIN),
    GL_TESSCOORD(Category.BUILTIN),
    GL_TESSLEVELOUTER(Category.BUILTIN),
    GL_TESSLEVELINNER(Category.BUILTIN),
    GL_PATCHVERTICESIN(Category.BUILTIN),
    GL_FRAGCOORD(Category.BUILTIN),
    GL_FRONTFACING(Category.BUILTIN),
    GL_FRAGDEPTH(Category.BUILTIN),
    GL_SAMPLEID(Category.BUILTIN),
    GL_SAMPLEPOSITION(Category.BUILTIN),
    GL_SAMPLEMASK(Category.BUILTIN),
    GL_NUMWORKGROUPS(Category.BUILTIN),
    GL_WORKGROUPSIZE(Category.BUILTIN),
    GL_WORKGROUPID(Category.BUILTIN),
    GL_LOCALINVOCATIONID(Category.BUILTIN),
    GL_GLOBALINVOCATIONID(Category.BUILTIN),
    GL_LOCALINVOCATIONINDEX(Category.BUILTIN),
    
    // Legacy built-ins
    GL_FRAGCOLOR(Category.BUILTIN),
    GL_FRAGDATA(Category.BUILTIN),
    GL_VERTEX(Category.BUILTIN),
    GL_NORMAL(Category.BUILTIN),
    GL_COLOR(Category.BUILTIN),
    GL_TEXCOORD(Category.BUILTIN),
    GL_MODELVIEWMATRIX(Category.BUILTIN),
    GL_PROJECTIONMATRIX(Category.BUILTIN),
    GL_MODELVIEWPROJECTIONMATRIX(Category.BUILTIN);
    
    public final Category category;
    public final String symbol;
    
    GLSLTokenType(Category category) {
        this(category, null);
    }
    
    GLSLTokenType(Category category, String symbol) {
        this.category = category;
        this.symbol = symbol;
    }
    
    public boolean isTypeKeyword() { return category == Category.TYPE_KEYWORD; }
    public boolean isQualifier() { return category == Category.QUALIFIER_KEYWORD; }
    public boolean isOperator() { return category == Category.OPERATOR; }
    public boolean isPunctuation() { return category == Category.PUNCTUATION; }
    public boolean isLiteral() { return category == Category.LITERAL; }
    public boolean isPreprocessor() { return category == Category.PREPROCESSOR; }
    public boolean isBuiltin() { return category == Category.BUILTIN; }
    public boolean isControlFlow() { return category == Category.CONTROL_KEYWORD; }
    
    enum Category {
        LITERAL,
        IDENTIFIER,
        TYPE_KEYWORD,
        QUALIFIER_KEYWORD,
        CONTROL_KEYWORD,
        STRUCT_KEYWORD,
        OPERATOR,
        PUNCTUATION,
        PREPROCESSOR,
        SPECIAL,
        BUILTIN
    }
}

// ============================================================================
// TOKEN CLASS - Flyweight Pattern with Pooling
// ============================================================================

/**
 * Immutable token representation with source location tracking.
 * Uses flyweight pattern for common tokens.
 */
final class GLSLToken {
    
    // Flyweight cache for common tokens
    private static final GLSLToken[] OPERATOR_TOKENS;
    private static final GLSLToken[] PUNCTUATION_TOKENS;
    private static final GLSLToken EOF_TOKEN;
    
    static {
        GLSLTokenType[] types = GLSLTokenType.values();
        OPERATOR_TOKENS = new GLSLToken[64];
        PUNCTUATION_TOKENS = new GLSLToken[16];
        
        int opIdx = 0, punctIdx = 0;
        for (GLSLTokenType type : types) {
            if (type.category == GLSLTokenType.Category.OPERATOR && opIdx < OPERATOR_TOKENS.length) {
                OPERATOR_TOKENS[opIdx++] = new GLSLToken(type, type.symbol, -1, -1);
            } else if (type.category == GLSLTokenType.Category.PUNCTUATION && punctIdx < PUNCTUATION_TOKENS.length) {
                PUNCTUATION_TOKENS[punctIdx++] = new GLSLToken(type, type.symbol, -1, -1);
            }
        }
        EOF_TOKEN = new GLSLToken(GLSLTokenType.EOF, "", -1, -1);
    }
    
    public final GLSLTokenType type;
    public final String value;
    public final int line;
    public final int column;
    
    // For numeric literals - avoid re-parsing
    private long intValue;
    private double floatValue;
    private byte flags;
    
    private static final byte FLAG_INT_PARSED = 1;
    private static final byte FLAG_FLOAT_PARSED = 2;
    
    GLSLToken(GLSLTokenType type, String value, int line, int column) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
    }
    
    // Factory methods for flyweight tokens
    static GLSLToken ofOperator(GLSLTokenType type, int line, int column) {
        for (GLSLToken t : OPERATOR_TOKENS) {
            if (t != null && t.type == type) {
                return line < 0 ? t : new GLSLToken(type, type.symbol, line, column);
            }
        }
        return new GLSLToken(type, type.symbol, line, column);
    }
    
    static GLSLToken ofPunctuation(GLSLTokenType type, int line, int column) {
        for (GLSLToken t : PUNCTUATION_TOKENS) {
            if (t != null && t.type == type) {
                return line < 0 ? t : new GLSLToken(type, type.symbol, line, column);
            }
        }
        return new GLSLToken(type, type.symbol, line, column);
    }
    
    static GLSLToken eof() {
        return EOF_TOKEN;
    }
    
    static GLSLToken eof(int line, int column) {
        return new GLSLToken(GLSLTokenType.EOF, "", line, column);
    }
    
    // Lazy parsing for numeric values
    public long asInt() {
        if ((flags & FLAG_INT_PARSED) == 0) {
            intValue = parseIntValue(value);
            flags |= FLAG_INT_PARSED;
        }
        return intValue;
    }
    
    public double asFloat() {
        if ((flags & FLAG_FLOAT_PARSED) == 0) {
            floatValue = parseFloatValue(value);
            flags |= FLAG_FLOAT_PARSED;
        }
        return floatValue;
    }
    
    public boolean asBool() {
        return "true".equals(value);
    }
    
    private static long parseIntValue(String s) {
        if (s == null || s.isEmpty()) return 0;
        
        int len = s.length();
        boolean unsigned = s.charAt(len - 1) == 'u' || s.charAt(len - 1) == 'U';
        if (unsigned) len--;
        
        // Check for hex/octal
        if (len > 2 && s.charAt(0) == '0') {
            char c1 = s.charAt(1);
            if (c1 == 'x' || c1 == 'X') {
                return Long.parseUnsignedLong(s.substring(2, len), 16);
            } else if (Character.isDigit(c1)) {
                return Long.parseUnsignedLong(s.substring(1, len), 8);
            }
        }
        
        return Long.parseLong(s.substring(0, len));
    }
    
    private static double parseFloatValue(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        
        int len = s.length();
        char last = s.charAt(len - 1);
        if (last == 'f' || last == 'F' || last == 'l' || last == 'L') {
            s = s.substring(0, len - 1);
        } else if ((last == 'f' || last == 'F') && len > 2) {
            // Check for "lf" or "LF" suffix
            char prev = s.charAt(len - 2);
            if (prev == 'l' || prev == 'L') {
                s = s.substring(0, len - 2);
            }
        }
        
        return Double.parseDouble(s);
    }
    
    public boolean isType() {
        return type.isTypeKeyword();
    }
    
    public boolean isQualifier() {
        return type.isQualifier();
    }
    
    public boolean isLiteral() {
        return type.isLiteral();
    }
    
    @Override
    public String toString() {
        return String.format("Token(%s, '%s', %d:%d)", type, value, line, column);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GLSLToken)) return false;
        GLSLToken other = (GLSLToken) o;
        return type == other.type && Objects.equals(value, other.value);
    }
    
    @Override
    public int hashCode() {
        return type.hashCode() * 31 + (value != null ? value.hashCode() : 0);
    }
}

// ============================================================================
// TOKEN STREAM - Efficient Streaming Interface
// ============================================================================

/**
 * Efficient token stream with lookahead and backtracking support.
 * Backed by a resizable array for O(1) random access.
 */
final class GLSLTokenStream {
    
    private GLSLToken[] tokens;
    private int size;
    private int position;
    private int mark;
    
    // Source info for error messages
    private final String source;
    private final int[] lineOffsets;
    
    GLSLTokenStream(String source) {
        this.source = source;
        this.tokens = new GLSLToken[256];
        this.size = 0;
        this.position = 0;
        this.mark = 0;
        this.lineOffsets = computeLineOffsets(source);
    }
    
    private int[] computeLineOffsets(String src) {
        int[] offsets = new int[64];
        int count = 0;
        offsets[count++] = 0;
        
        for (int i = 0; i < src.length(); i++) {
            if (src.charAt(i) == '\n') {
                if (count >= offsets.length) {
                    offsets = Arrays.copyOf(offsets, offsets.length * 2);
                }
                offsets[count++] = i + 1;
            }
        }
        
        return Arrays.copyOf(offsets, count);
    }
    
    void add(GLSLToken token) {
        if (size >= tokens.length) {
            tokens = Arrays.copyOf(tokens, tokens.length * 2);
        }
        tokens[size++] = token;
    }
    
    void addAll(ArrayList<GLSLToken> list) {
        int newSize = size + list.size();
        if (newSize > tokens.length) {
            tokens = Arrays.copyOf(tokens, Math.max(tokens.length * 2, newSize));
        }
        for (int i = 0; i < list.size(); i++) {
            tokens[size++] = list.get(i);
        }
    }
    
    // ==================== Navigation ====================
    
    GLSLToken current() {
        return position < size ? tokens[position] : GLSLToken.eof();
    }
    
    GLSLToken peek() {
        return current();
    }
    
    GLSLToken peek(int offset) {
        int idx = position + offset;
        return idx >= 0 && idx < size ? tokens[idx] : GLSLToken.eof();
    }
    
    GLSLToken advance() {
        GLSLToken token = current();
        if (position < size) position++;
        return token;
    }
    
    GLSLToken consume() {
        return advance();
    }
    
    GLSLToken consume(GLSLTokenType expected) {
        GLSLToken token = current();
        if (token.type != expected) {
            throw new GLSLParseException(
                String.format("Expected %s but got %s at %d:%d", 
                    expected, token.type, token.line, token.column),
                Collections.singletonList(new GLSLError(
                    token.line, token.column, 
                    "Expected " + expected, GLSLErrorType.SYNTAX_ERROR))
            );
        }
        position++;
        return token;
    }
    
    boolean check(GLSLTokenType type) {
        return current().type == type;
    }
    
    boolean check(GLSLTokenType... types) {
        GLSLTokenType current = current().type;
        for (GLSLTokenType type : types) {
            if (current == type) return true;
        }
        return false;
    }
    
    boolean match(GLSLTokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }
    
    boolean match(GLSLTokenType... types) {
        for (GLSLTokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    
    boolean isAtEnd() {
        return position >= size || current().type == GLSLTokenType.EOF;
    }
    
    // ==================== Backtracking ====================
    
    void mark() {
        this.mark = position;
    }
    
    void reset() {
        this.position = mark;
    }
    
    void rewind(int count) {
        position = Math.max(0, position - count);
    }
    
    int getPosition() {
        return position;
    }
    
    void setPosition(int pos) {
        this.position = Math.max(0, Math.min(pos, size));
    }
    
    // ==================== Info ====================
    
    int size() {
        return size;
    }
    
    String getSource() {
        return source;
    }
    
    String getSourceLine(int lineNumber) {
        if (lineNumber < 1 || lineNumber > lineOffsets.length) return "";
        
        int start = lineOffsets[lineNumber - 1];
        int end = lineNumber < lineOffsets.length ? lineOffsets[lineNumber] - 1 : source.length();
        
        return source.substring(start, Math.min(end, source.length()));
    }
    
    GLSLToken[] toArray() {
        return Arrays.copyOf(tokens, size);
    }
    
    // ==================== Memory Management ====================
    
    void clear() {
        Arrays.fill(tokens, 0, size, null);
        size = 0;
        position = 0;
        mark = 0;
    }
    
    void compact() {
        if (tokens.length > size * 2 && tokens.length > 256) {
            tokens = Arrays.copyOf(tokens, Math.max(256, size));
        }
    }
}

// ============================================================================
// HIGH-PERFORMANCE TOKENIZER
// ============================================================================

/**
 * High-performance GLSL tokenizer with zero-allocation hot paths.
 * Uses pre-computed lookup tables and direct character processing.
 */
final class GLSLTokenizer {
    
    private final GLSLMemoryPool pool;
    
    // Input state
    private String source;
    private int pos;
    private int length;
    private int line;
    private int column;
    private int lineStart;
    
    // Keyword lookup - perfect hash table
    private static final Map<String, GLSLTokenType> KEYWORDS;
    private static final Map<String, GLSLTokenType> PREPROCESSOR_KEYWORDS;
    private static final Map<String, GLSLTokenType> BUILTINS;
    
    // Character classification tables
    private static final boolean[] IS_IDENT_START = new boolean[128];
    private static final boolean[] IS_IDENT_PART = new boolean[128];
    private static final boolean[] IS_DIGIT = new boolean[128];
    private static final boolean[] IS_HEX_DIGIT = new boolean[128];
    private static final boolean[] IS_WHITESPACE = new boolean[128];
    private static final boolean[] IS_OPERATOR_START = new boolean[128];
    
    static {
        // Initialize character tables
        for (int i = 0; i < 128; i++) {
            char c = (char) i;
            IS_IDENT_START[i] = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
            IS_IDENT_PART[i] = IS_IDENT_START[i] || (c >= '0' && c <= '9');
            IS_DIGIT[i] = c >= '0' && c <= '9';
            IS_HEX_DIGIT[i] = IS_DIGIT[i] || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            IS_WHITESPACE[i] = c == ' ' || c == '\t' || c == '\r' || c == '\n';
            IS_OPERATOR_START[i] = "+-*/%=<>!&|^~?:".indexOf(c) >= 0;
        }
        
        // Initialize keyword map
        KEYWORDS = new HashMap<>(256);
        
        // Type keywords
        KEYWORDS.put("void", GLSLTokenType.VOID);
        KEYWORDS.put("bool", GLSLTokenType.BOOL);
        KEYWORDS.put("int", GLSLTokenType.INT);
        KEYWORDS.put("uint", GLSLTokenType.UINT);
        KEYWORDS.put("float", GLSLTokenType.FLOAT);
        KEYWORDS.put("double", GLSLTokenType.DOUBLE);
        
        // Vector types
        KEYWORDS.put("vec2", GLSLTokenType.VEC2);
        KEYWORDS.put("vec3", GLSLTokenType.VEC3);
        KEYWORDS.put("vec4", GLSLTokenType.VEC4);
        KEYWORDS.put("dvec2", GLSLTokenType.DVEC2);
        KEYWORDS.put("dvec3", GLSLTokenType.DVEC3);
        KEYWORDS.put("dvec4", GLSLTokenType.DVEC4);
        KEYWORDS.put("bvec2", GLSLTokenType.BVEC2);
        KEYWORDS.put("bvec3", GLSLTokenType.BVEC3);
        KEYWORDS.put("bvec4", GLSLTokenType.BVEC4);
        KEYWORDS.put("ivec2", GLSLTokenType.IVEC2);
        KEYWORDS.put("ivec3", GLSLTokenType.IVEC3);
        KEYWORDS.put("ivec4", GLSLTokenType.IVEC4);
        KEYWORDS.put("uvec2", GLSLTokenType.UVEC2);
        KEYWORDS.put("uvec3", GLSLTokenType.UVEC3);
        KEYWORDS.put("uvec4", GLSLTokenType.UVEC4);
        
        // Matrix types
        KEYWORDS.put("mat2", GLSLTokenType.MAT2);
        KEYWORDS.put("mat3", GLSLTokenType.MAT3);
        KEYWORDS.put("mat4", GLSLTokenType.MAT4);
        KEYWORDS.put("mat2x2", GLSLTokenType.MAT2X2);
        KEYWORDS.put("mat2x3", GLSLTokenType.MAT2X3);
        KEYWORDS.put("mat2x4", GLSLTokenType.MAT2X4);
        KEYWORDS.put("mat3x2", GLSLTokenType.MAT3X2);
        KEYWORDS.put("mat3x3", GLSLTokenType.MAT3X3);
        KEYWORDS.put("mat3x4", GLSLTokenType.MAT3X4);
        KEYWORDS.put("mat4x2", GLSLTokenType.MAT4X2);
        KEYWORDS.put("mat4x3", GLSLTokenType.MAT4X3);
        KEYWORDS.put("mat4x4", GLSLTokenType.MAT4X4);
        KEYWORDS.put("dmat2", GLSLTokenType.DMAT2);
        KEYWORDS.put("dmat3", GLSLTokenType.DMAT3);
        KEYWORDS.put("dmat4", GLSLTokenType.DMAT4);
        KEYWORDS.put("dmat2x2", GLSLTokenType.DMAT2X2);
        KEYWORDS.put("dmat2x3", GLSLTokenType.DMAT2X3);
        KEYWORDS.put("dmat2x4", GLSLTokenType.DMAT2X4);
        KEYWORDS.put("dmat3x2", GLSLTokenType.DMAT3X2);
        KEYWORDS.put("dmat3x3", GLSLTokenType.DMAT3X3);
        KEYWORDS.put("dmat3x4", GLSLTokenType.DMAT3X4);
        KEYWORDS.put("dmat4x2", GLSLTokenType.DMAT4X2);
        KEYWORDS.put("dmat4x3", GLSLTokenType.DMAT4X3);
        KEYWORDS.put("dmat4x4", GLSLTokenType.DMAT4X4);
        
        // Sampler types
        KEYWORDS.put("sampler1D", GLSLTokenType.SAMPLER1D);
        KEYWORDS.put("sampler2D", GLSLTokenType.SAMPLER2D);
        KEYWORDS.put("sampler3D", GLSLTokenType.SAMPLER3D);
        KEYWORDS.put("samplerCube", GLSLTokenType.SAMPLERCUBE);
        KEYWORDS.put("sampler1DShadow", GLSLTokenType.SAMPLER1DSHADOW);
        KEYWORDS.put("sampler2DShadow", GLSLTokenType.SAMPLER2DSHADOW);
        KEYWORDS.put("samplerCubeShadow", GLSLTokenType.SAMPLERCUBESHADOW);
        KEYWORDS.put("sampler1DArray", GLSLTokenType.SAMPLER1DARRAY);
        KEYWORDS.put("sampler2DArray", GLSLTokenType.SAMPLER2DARRAY);
        KEYWORDS.put("sampler1DArrayShadow", GLSLTokenType.SAMPLER1DARRAYSHADOW);
        KEYWORDS.put("sampler2DArrayShadow", GLSLTokenType.SAMPLER2DARRAYSHADOW);
        KEYWORDS.put("samplerCubeArray", GLSLTokenType.SAMPLERCUBEARRAY);
        KEYWORDS.put("samplerCubeArrayShadow", GLSLTokenType.SAMPLERCUBEARRAYSHADOW);
        KEYWORDS.put("isampler1D", GLSLTokenType.ISAMPLER1D);
        KEYWORDS.put("isampler2D", GLSLTokenType.ISAMPLER2D);
        KEYWORDS.put("isampler3D", GLSLTokenType.ISAMPLER3D);
        KEYWORDS.put("isamplerCube", GLSLTokenType.ISAMPLERCUBE);
        KEYWORDS.put("isampler1DArray", GLSLTokenType.ISAMPLER1DARRAY);
        KEYWORDS.put("isampler2DArray", GLSLTokenType.ISAMPLER2DARRAY);
        KEYWORDS.put("isamplerCubeArray", GLSLTokenType.ISAMPLERCUBEARRAY);
        KEYWORDS.put("usampler1D", GLSLTokenType.USAMPLER1D);
        KEYWORDS.put("usampler2D", GLSLTokenType.USAMPLER2D);
        KEYWORDS.put("usampler3D", GLSLTokenType.USAMPLER3D);
        KEYWORDS.put("usamplerCube", GLSLTokenType.USAMPLERCUBE);
        KEYWORDS.put("usampler1DArray", GLSLTokenType.USAMPLER1DARRAY);
        KEYWORDS.put("usampler2DArray", GLSLTokenType.USAMPLER2DARRAY);
        KEYWORDS.put("usamplerCubeArray", GLSLTokenType.USAMPLERCUBEARRAY);
        KEYWORDS.put("sampler2DRect", GLSLTokenType.SAMPLER2DRECT);
        KEYWORDS.put("sampler2DRectShadow", GLSLTokenType.SAMPLER2DRECTSHADOW);
        KEYWORDS.put("isampler2DRect", GLSLTokenType.ISAMPLER2DRECT);
        KEYWORDS.put("usampler2DRect", GLSLTokenType.USAMPLER2DRECT);
        KEYWORDS.put("samplerBuffer", GLSLTokenType.SAMPLERBUFFER);
        KEYWORDS.put("isamplerBuffer", GLSLTokenType.ISAMPLERBUFFER);
        KEYWORDS.put("usamplerBuffer", GLSLTokenType.USAMPLERBUFFER);
        KEYWORDS.put("sampler2DMS", GLSLTokenType.SAMPLER2DMS);
        KEYWORDS.put("isampler2DMS", GLSLTokenType.ISAMPLER2DMS);
        KEYWORDS.put("usampler2DMS", GLSLTokenType.USAMPLER2DMS);
        KEYWORDS.put("sampler2DMSArray", GLSLTokenType.SAMPLER2DMSARRAY);
        KEYWORDS.put("isampler2DMSArray", GLSLTokenType.ISAMPLER2DMSARRAY);
        KEYWORDS.put("usampler2DMSArray", GLSLTokenType.USAMPLER2DMSARRAY);
        
        // Image types
        KEYWORDS.put("image1D", GLSLTokenType.IMAGE1D);
        KEYWORDS.put("image2D", GLSLTokenType.IMAGE2D);
        KEYWORDS.put("image3D", GLSLTokenType.IMAGE3D);
        KEYWORDS.put("image2DRect", GLSLTokenType.IMAGE2DRECT);
        KEYWORDS.put("imageCube", GLSLTokenType.IMAGECUBE);
        KEYWORDS.put("imageBuffer", GLSLTokenType.IMAGEBUFFER);
        KEYWORDS.put("image1DArray", GLSLTokenType.IMAGE1DARRAY);
        KEYWORDS.put("image2DArray", GLSLTokenType.IMAGE2DARRAY);
        KEYWORDS.put("imageCubeArray", GLSLTokenType.IMAGECUBEARRAY);
        KEYWORDS.put("image2DMS", GLSLTokenType.IMAGE2DMS);
        KEYWORDS.put("image2DMSArray", GLSLTokenType.IMAGE2DMSARRAY);
        KEYWORDS.put("iimage1D", GLSLTokenType.IIMAGE1D);
        KEYWORDS.put("iimage2D", GLSLTokenType.IIMAGE2D);
        KEYWORDS.put("iimage3D", GLSLTokenType.IIMAGE3D);
        KEYWORDS.put("iimage2DRect", GLSLTokenType.IIMAGE2DRECT);
        KEYWORDS.put("iimageCube", GLSLTokenType.IIMAGECUBE);
        KEYWORDS.put("iimageBuffer", GLSLTokenType.IIMAGEBUFFER);
        KEYWORDS.put("iimage1DArray", GLSLTokenType.IIMAGE1DARRAY);
        KEYWORDS.put("iimage2DArray", GLSLTokenType.IIMAGE2DARRAY);
        KEYWORDS.put("iimageCubeArray", GLSLTokenType.IIMAGECUBEARRAY);
        KEYWORDS.put("iimage2DMS", GLSLTokenType.IIMAGE2DMS);
        KEYWORDS.put("iimage2DMSArray", GLSLTokenType.IIMAGE2DMSARRAY);
        KEYWORDS.put("uimage1D", GLSLTokenType.UIMAGE1D);
        KEYWORDS.put("uimage2D", GLSLTokenType.UIMAGE2D);
        KEYWORDS.put("uimage3D", GLSLTokenType.UIMAGE3D);
        KEYWORDS.put("uimage2DRect", GLSLTokenType.UIMAGE2DRECT);
        KEYWORDS.put("uimageCube", GLSLTokenType.UIMAGECUBE);
        KEYWORDS.put("uimageBuffer", GLSLTokenType.UIMAGEBUFFER);
        KEYWORDS.put("uimage1DArray", GLSLTokenType.UIMAGE1DARRAY);
        KEYWORDS.put("uimage2DArray", GLSLTokenType.UIMAGE2DARRAY);
        KEYWORDS.put("uimageCubeArray", GLSLTokenType.UIMAGECUBEARRAY);
        KEYWORDS.put("uimage2DMS", GLSLTokenType.UIMAGE2DMS);
        KEYWORDS.put("uimage2DMSArray", GLSLTokenType.UIMAGE2DMSARRAY);
        
        // Atomic counter
        KEYWORDS.put("atomic_uint", GLSLTokenType.ATOMIC_UINT);
        
        // Qualifiers
        KEYWORDS.put("const", GLSLTokenType.CONST);
        KEYWORDS.put("in", GLSLTokenType.IN);
        KEYWORDS.put("out", GLSLTokenType.OUT);
        KEYWORDS.put("inout", GLSLTokenType.INOUT);
        KEYWORDS.put("uniform", GLSLTokenType.UNIFORM);
        KEYWORDS.put("buffer", GLSLTokenType.BUFFER);
        KEYWORDS.put("shared", GLSLTokenType.SHARED);
        KEYWORDS.put("attribute", GLSLTokenType.ATTRIBUTE);
        KEYWORDS.put("varying", GLSLTokenType.VARYING);
        KEYWORDS.put("centroid", GLSLTokenType.CENTROID);
        KEYWORDS.put("sample", GLSLTokenType.SAMPLE);
        KEYWORDS.put("patch", GLSLTokenType.PATCH);
        KEYWORDS.put("layout", GLSLTokenType.LAYOUT);
        KEYWORDS.put("flat", GLSLTokenType.FLAT);
        KEYWORDS.put("smooth", GLSLTokenType.SMOOTH);
        KEYWORDS.put("noperspective", GLSLTokenType.NOPERSPECTIVE);
        KEYWORDS.put("highp", GLSLTokenType.HIGHP);
        KEYWORDS.put("mediump", GLSLTokenType.MEDIUMP);
        KEYWORDS.put("lowp", GLSLTokenType.LOWP);
        KEYWORDS.put("precision", GLSLTokenType.PRECISION);
        KEYWORDS.put("invariant", GLSLTokenType.INVARIANT);
        KEYWORDS.put("precise", GLSLTokenType.PRECISE);
        KEYWORDS.put("coherent", GLSLTokenType.COHERENT);
        KEYWORDS.put("volatile", GLSLTokenType.VOLATILE);
        KEYWORDS.put("restrict", GLSLTokenType.RESTRICT);
        KEYWORDS.put("readonly", GLSLTokenType.READONLY);
        KEYWORDS.put("writeonly", GLSLTokenType.WRITEONLY);
        
        // Control flow
        KEYWORDS.put("if", GLSLTokenType.IF);
        KEYWORDS.put("else", GLSLTokenType.ELSE);
        KEYWORDS.put("switch", GLSLTokenType.SWITCH);
        KEYWORDS.put("case", GLSLTokenType.CASE);
        KEYWORDS.put("default", GLSLTokenType.DEFAULT);
        KEYWORDS.put("for", GLSLTokenType.FOR);
        KEYWORDS.put("while", GLSLTokenType.WHILE);
        KEYWORDS.put("do", GLSLTokenType.DO);
        KEYWORDS.put("break", GLSLTokenType.BREAK);
        KEYWORDS.put("continue", GLSLTokenType.CONTINUE);
        KEYWORDS.put("return", GLSLTokenType.RETURN);
        KEYWORDS.put("discard", GLSLTokenType.DISCARD);
        
        // Struct keywords
        KEYWORDS.put("struct", GLSLTokenType.STRUCT);
        KEYWORDS.put("subroutine", GLSLTokenType.SUBROUTINE);
        
        // Boolean literals
        KEYWORDS.put("true", GLSLTokenType.BOOL_LITERAL);
        KEYWORDS.put("false", GLSLTokenType.BOOL_LITERAL);
        
        // Preprocessor keywords
        PREPROCESSOR_KEYWORDS = new HashMap<>(16);
        PREPROCESSOR_KEYWORDS.put("version", GLSLTokenType.PP_VERSION);
        PREPROCESSOR_KEYWORDS.put("extension", GLSLTokenType.PP_EXTENSION);
        PREPROCESSOR_KEYWORDS.put("line", GLSLTokenType.PP_LINE);
        PREPROCESSOR_KEYWORDS.put("define", GLSLTokenType.PP_DEFINE);
        PREPROCESSOR_KEYWORDS.put("undef", GLSLTokenType.PP_UNDEF);
        PREPROCESSOR_KEYWORDS.put("if", GLSLTokenType.PP_IF);
        PREPROCESSOR_KEYWORDS.put("ifdef", GLSLTokenType.PP_IFDEF);
        PREPROCESSOR_KEYWORDS.put("ifndef", GLSLTokenType.PP_IFNDEF);
        PREPROCESSOR_KEYWORDS.put("else", GLSLTokenType.PP_ELSE);
        PREPROCESSOR_KEYWORDS.put("elif", GLSLTokenType.PP_ELIF);
        PREPROCESSOR_KEYWORDS.put("endif", GLSLTokenType.PP_ENDIF);
        PREPROCESSOR_KEYWORDS.put("error", GLSLTokenType.PP_ERROR);
        PREPROCESSOR_KEYWORDS.put("pragma", GLSLTokenType.PP_PRAGMA);
        
        // Built-in variables
        BUILTINS = new HashMap<>(64);
        BUILTINS.put("gl_Position", GLSLTokenType.GL_POSITION);
        BUILTINS.put("gl_PointSize", GLSLTokenType.GL_POINTSIZE);
        BUILTINS.put("gl_ClipDistance", GLSLTokenType.GL_CLIPDISTANCE);
        BUILTINS.put("gl_CullDistance", GLSLTokenType.GL_CULLDISTANCE);
        BUILTINS.put("gl_VertexID", GLSLTokenType.GL_VERTEXID);
        BUILTINS.put("gl_InstanceID", GLSLTokenType.GL_INSTANCEID);
        BUILTINS.put("gl_PrimitiveID", GLSLTokenType.GL_PRIMITIVEID);
        BUILTINS.put("gl_InvocationID", GLSLTokenType.GL_INVOCATIONID);
        BUILTINS.put("gl_Layer", GLSLTokenType.GL_LAYER);
        BUILTINS.put("gl_ViewportIndex", GLSLTokenType.GL_VIEWPORTINDEX);
        BUILTINS.put("gl_TessCoord", GLSLTokenType.GL_TESSCOORD);
        BUILTINS.put("gl_TessLevelOuter", GLSLTokenType.GL_TESSLEVELOUTER);
        BUILTINS.put("gl_TessLevelInner", GLSLTokenType.GL_TESSLEVELINNER);
        BUILTINS.put("gl_PatchVerticesIn", GLSLTokenType.GL_PATCHVERTICESIN);
        BUILTINS.put("gl_FragCoord", GLSLTokenType.GL_FRAGCOORD);
        BUILTINS.put("gl_FrontFacing", GLSLTokenType.GL_FRONTFACING);
        BUILTINS.put("gl_FragDepth", GLSLTokenType.GL_FRAGDEPTH);
        BUILTINS.put("gl_SampleID", GLSLTokenType.GL_SAMPLEID);
        BUILTINS.put("gl_SamplePosition", GLSLTokenType.GL_SAMPLEPOSITION);
        BUILTINS.put("gl_SampleMask", GLSLTokenType.GL_SAMPLEMASK);
        BUILTINS.put("gl_NumWorkGroups", GLSLTokenType.GL_NUMWORKGROUPS);
        BUILTINS.put("gl_WorkGroupSize", GLSLTokenType.GL_WORKGROUPSIZE);
        BUILTINS.put("gl_WorkGroupID", GLSLTokenType.GL_WORKGROUPID);
        BUILTINS.put("gl_LocalInvocationID", GLSLTokenType.GL_LOCALINVOCATIONID);
        BUILTINS.put("gl_GlobalInvocationID", GLSLTokenType.GL_GLOBALINVOCATIONID);
        BUILTINS.put("gl_LocalInvocationIndex", GLSLTokenType.GL_LOCALINVOCATIONINDEX);
        
        // Legacy built-ins
        BUILTINS.put("gl_FragColor", GLSLTokenType.GL_FRAGCOLOR);
        BUILTINS.put("gl_FragData", GLSLTokenType.GL_FRAGDATA);
        BUILTINS.put("gl_Vertex", GLSLTokenType.GL_VERTEX);
        BUILTINS.put("gl_Normal", GLSLTokenType.GL_NORMAL);
        BUILTINS.put("gl_Color", GLSLTokenType.GL_COLOR);
        BUILTINS.put("gl_TexCoord", GLSLTokenType.GL_TEXCOORD);
        BUILTINS.put("gl_ModelViewMatrix", GLSLTokenType.GL_MODELVIEWMATRIX);
        BUILTINS.put("gl_ProjectionMatrix", GLSLTokenType.GL_PROJECTIONMATRIX);
        BUILTINS.put("gl_ModelViewProjectionMatrix", GLSLTokenType.GL_MODELVIEWPROJECTIONMATRIX);
    }
    
    GLSLTokenizer(GLSLMemoryPool pool) {
        this.pool = pool;
    }
    
    GLSLTokenStream tokenize(String source) {
        this.source = source;
        this.pos = 0;
        this.length = source.length();
        this.line = 1;
        this.column = 1;
        this.lineStart = 0;
        
        GLSLTokenStream stream = new GLSLTokenStream(source);
        ArrayList<GLSLToken> tokens = pool.acquireTokenList();
        
        try {
            while (pos < length) {
                GLSLToken token = nextToken();
                if (token != null && token.type != GLSLTokenType.WHITESPACE 
                    && token.type != GLSLTokenType.COMMENT) {
                    tokens.add(token);
                }
            }
            
            tokens.add(GLSLToken.eof(line, column));
            stream.addAll(tokens);
            
        } finally {
            pool.releaseTokenList(tokens);
        }
        
        return stream;
    }
    
    private GLSLToken nextToken() {
        // Skip whitespace
        while (pos < length && isWhitespace(peek())) {
            if (peek() == '\n') {
                line++;
                pos++;
                lineStart = pos;
                column = 1;
            } else {
                pos++;
                column++;
            }
        }
        
        if (pos >= length) {
            return null;
        }
        
        int startLine = line;
        int startColumn = column;
        char c = peek();
        
        // Comments
        if (c == '/') {
            if (pos + 1 < length) {
                char next = source.charAt(pos + 1);
                if (next == '/') {
                    return scanLineComment(startLine, startColumn);
                } else if (next == '*') {
                    return scanBlockComment(startLine, startColumn);
                }
            }
        }
        
        // Preprocessor
        if (c == '#') {
            return scanPreprocessor(startLine, startColumn);
        }
        
        // Identifier or keyword
        if (isIdentStart(c)) {
            return scanIdentifierOrKeyword(startLine, startColumn);
        }
        
        // Number
        if (isDigit(c) || (c == '.' && pos + 1 < length && isDigit(source.charAt(pos + 1)))) {
            return scanNumber(startLine, startColumn);
        }
        
        // Operators and punctuation
        return scanOperatorOrPunctuation(startLine, startColumn);
    }
    
    private GLSLToken scanLineComment(int startLine, int startColumn) {
        int start = pos;
        pos += 2;
        column += 2;
        
        while (pos < length && peek() != '\n') {
            pos++;
            column++;
        }
        
        return new GLSLToken(GLSLTokenType.COMMENT, 
            source.substring(start, pos), startLine, startColumn);
    }
    
    private GLSLToken scanBlockComment(int startLine, int startColumn) {
        int start = pos;
        pos += 2;
        column += 2;
        
        while (pos + 1 < length) {
            if (peek() == '*' && source.charAt(pos + 1) == '/') {
                pos += 2;
                column += 2;
                break;
            }
            if (peek() == '\n') {
                line++;
                lineStart = pos + 1;
                column = 0;
            }
            pos++;
            column++;
        }
        
        return new GLSLToken(GLSLTokenType.COMMENT, 
            source.substring(start, pos), startLine, startColumn);
    }
    
    private GLSLToken scanPreprocessor(int startLine, int startColumn) {
        pos++; // skip #
        column++;
        
        // Skip whitespace
        while (pos < length && (peek() == ' ' || peek() == '\t')) {
            pos++;
            column++;
        }
        
        // Get directive name
        int nameStart = pos;
        while (pos < length && isIdentPart(peek())) {
            pos++;
            column++;
        }
        
        String directive = source.substring(nameStart, pos);
        GLSLTokenType type = PREPROCESSOR_KEYWORDS.getOrDefault(directive, GLSLTokenType.HASH);
        
        // For version/extension, include the rest of the line
        if (type == GLSLTokenType.PP_VERSION || type == GLSLTokenType.PP_EXTENSION 
            || type == GLSLTokenType.PP_DEFINE || type == GLSLTokenType.PP_ERROR) {
            
            // Skip whitespace
            while (pos < length && (peek() == ' ' || peek() == '\t')) {
                pos++;
                column++;
            }
            
            int valueStart = pos;
            while (pos < length && peek() != '\n') {
                // Handle line continuation
                if (peek() == '\\' && pos + 1 < length && source.charAt(pos + 1) == '\n') {
                    pos += 2;
                    line++;
                    lineStart = pos;
                    column = 1;
                    continue;
                }
                pos++;
                column++;
            }
            
            String fullDirective = "#" + directive + " " + source.substring(valueStart, pos).trim();
            return new GLSLToken(type, fullDirective, startLine, startColumn);
        }
        
        return new GLSLToken(type, "#" + directive, startLine, startColumn);
    }
    
    private GLSLToken scanIdentifierOrKeyword(int startLine, int startColumn) {
        int start = pos;
        
        while (pos < length && isIdentPart(peek())) {
            pos++;
            column++;
        }
        
        String text = source.substring(start, pos);
        
        // Check if it's a keyword
        GLSLTokenType kwType = KEYWORDS.get(text);
        if (kwType != null) {
            return new GLSLToken(kwType, text, startLine, startColumn);
        }
        
        // Check if it's a built-in
        GLSLTokenType builtinType = BUILTINS.get(text);
        if (builtinType != null) {
            return new GLSLToken(builtinType, text, startLine, startColumn);
        }
        
        return new GLSLToken(GLSLTokenType.IDENTIFIER, text, startLine, startColumn);
    }
    
    private GLSLToken scanNumber(int startLine, int startColumn) {
        int start = pos;
        GLSLTokenType type = GLSLTokenType.INT_LITERAL;
        
        // Check for hex/octal
        if (peek() == '0' && pos + 1 < length) {
            char next = source.charAt(pos + 1);
            if (next == 'x' || next == 'X') {
                pos += 2;
                column += 2;
                while (pos < length && isHexDigit(peek())) {
                    pos++;
                    column++;
                }
                // Check for unsigned suffix
                if (pos < length && (peek() == 'u' || peek() == 'U')) {
                    pos++;
                    column++;
                    type = GLSLTokenType.UINT_LITERAL;
                }
                return new GLSLToken(type, source.substring(start, pos), startLine, startColumn);
            }
        }
        
        // Integer part
        while (pos < length && isDigit(peek())) {
            pos++;
            column++;
        }
        
        // Check for float
        boolean isFloat = false;
        
        // Decimal point
        if (pos < length && peek() == '.') {
            if (pos + 1 < length && isDigit(source.charAt(pos + 1))) {
                isFloat = true;
                pos++;
                column++;
                while (pos < length && isDigit(peek())) {
                    pos++;
                    column++;
                }
            } else if (pos == start) {
                // Just a dot
                isFloat = true;
                pos++;
                column++;
                while (pos < length && isDigit(peek())) {
                    pos++;
                    column++;
                }
            }
        }
        
        // Exponent
        if (pos < length && (peek() == 'e' || peek() == 'E')) {
            isFloat = true;
            pos++;
            column++;
            if (pos < length && (peek() == '+' || peek() == '-')) {
                pos++;
                column++;
            }
            while (pos < length && isDigit(peek())) {
                pos++;
                column++;
            }
        }
        
        // Suffix
        if (pos < length) {
            char suffix = peek();
            if (suffix == 'f' || suffix == 'F') {
                isFloat = true;
                pos++;
                column++;
                type = GLSLTokenType.FLOAT_LITERAL;
            } else if (suffix == 'l' || suffix == 'L') {
                pos++;
                column++;
                if (pos < length && (peek() == 'f' || peek() == 'F')) {
                    pos++;
                    column++;
                }
                type = GLSLTokenType.DOUBLE_LITERAL;
            } else if (suffix == 'u' || suffix == 'U') {
                pos++;
                column++;
                type = GLSLTokenType.UINT_LITERAL;
            }
        }
        
        if (isFloat && type == GLSLTokenType.INT_LITERAL) {
            type = GLSLTokenType.FLOAT_LITERAL;
        }
        
        return new GLSLToken(type, source.substring(start, pos), startLine, startColumn);
    }
    
    private GLSLToken scanOperatorOrPunctuation(int startLine, int startColumn) {
        char c = peek();
        pos++;
        column++;
        
        switch (c) {
            // Single-character punctuation
            case '(': return GLSLToken.ofPunctuation(GLSLTokenType.LEFT_PAREN, startLine, startColumn);
            case ')': return GLSLToken.ofPunctuation(GLSLTokenType.RIGHT_PAREN, startLine, startColumn);
            case '[': return GLSLToken.ofPunctuation(GLSLTokenType.LEFT_BRACKET, startLine, startColumn);
            case ']': return GLSLToken.ofPunctuation(GLSLTokenType.RIGHT_BRACKET, startLine, startColumn);
            case '{': return GLSLToken.ofPunctuation(GLSLTokenType.LEFT_BRACE, startLine, startColumn);
            case '}': return GLSLToken.ofPunctuation(GLSLTokenType.RIGHT_BRACE, startLine, startColumn);
            case ';': return GLSLToken.ofPunctuation(GLSLTokenType.SEMICOLON, startLine, startColumn);
            case ',': return GLSLToken.ofPunctuation(GLSLTokenType.COMMA, startLine, startColumn);
            case '.': return GLSLToken.ofPunctuation(GLSLTokenType.DOT, startLine, startColumn);
            case '~': return GLSLToken.ofOperator(GLSLTokenType.TILDE, startLine, startColumn);
            case '?': return GLSLToken.ofOperator(GLSLTokenType.QUESTION, startLine, startColumn);
            case ':': return GLSLToken.ofOperator(GLSLTokenType.COLON, startLine, startColumn);
            
            // Multi-character operators
            case '+':
                if (pos < length) {
                    if (peek() == '+') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.INCREMENT, startLine, startColumn); }
                    if (peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.ADD_ASSIGN, startLine, startColumn); }
                }
                return GLSLToken.ofOperator(GLSLTokenType.PLUS, startLine, startColumn);
                
            case '-':
                if (pos < length) {
                    if (peek() == '-') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.DECREMENT, startLine, startColumn); }
                    if (peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.SUB_ASSIGN, startLine, startColumn); }
                }
                return GLSLToken.ofOperator(GLSLTokenType.MINUS, startLine, startColumn);
                
            case '*':
                if (pos < length && peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.MUL_ASSIGN, startLine, startColumn); }
                return GLSLToken.ofOperator(GLSLTokenType.STAR, startLine, startColumn);
                
            case '/':
                if (pos < length && peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.DIV_ASSIGN, startLine, startColumn); }
                return GLSLToken.ofOperator(GLSLTokenType.SLASH, startLine, startColumn);
                
            case '%':
                if (pos < length && peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.MOD_ASSIGN, startLine, startColumn); }
                return GLSLToken.ofOperator(GLSLTokenType.PERCENT, startLine, startColumn);
                
            case '=':
                if (pos < length && peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.EQ, startLine, startColumn); }
                return GLSLToken.ofOperator(GLSLTokenType.ASSIGN, startLine, startColumn);
                
            case '!':
                if (pos < length && peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.NE, startLine, startColumn); }
                return GLSLToken.ofOperator(GLSLTokenType.NOT, startLine, startColumn);
                
            case '<':
                if (pos < length) {
                    if (peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.LE, startLine, startColumn); }
                    if (peek() == '<') {
                        pos++; column++;
                        if (pos < length && peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.LEFT_SHIFT_ASSIGN, startLine, startColumn); }
                        return GLSLToken.ofOperator(GLSLTokenType.LEFT_SHIFT, startLine, startColumn);
                    }
                }
                return GLSLToken.ofOperator(GLSLTokenType.LT, startLine, startColumn);
                
            case '>':
                if (pos < length) {
                    if (peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.GE, startLine, startColumn); }
                    if (peek() == '>') {
                        pos++; column++;
                        if (pos < length && peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.RIGHT_SHIFT_ASSIGN, startLine, startColumn); }
                        return GLSLToken.ofOperator(GLSLTokenType.RIGHT_SHIFT, startLine, startColumn);
                    }
                }
                return GLSLToken.ofOperator(GLSLTokenType.GT, startLine, startColumn);
                
            case '&':
                if (pos < length) {
                    if (peek() == '&') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.AND, startLine, startColumn); }
                    if (peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.AND_ASSIGN, startLine, startColumn); }
                }
                return GLSLToken.ofOperator(GLSLTokenType.AMPERSAND, startLine, startColumn);
                
            case '|':
                if (pos < length) {
                    if (peek() == '|') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.OR, startLine, startColumn); }
                    if (peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.OR_ASSIGN, startLine, startColumn); }
                }
                return GLSLToken.ofOperator(GLSLTokenType.PIPE, startLine, startColumn);
                
            case '^':
                if (pos < length) {
                    if (peek() == '^') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.XOR, startLine, startColumn); }
                    if (peek() == '=') { pos++; column++; return GLSLToken.ofOperator(GLSLTokenType.XOR_ASSIGN, startLine, startColumn); }
                }
                return GLSLToken.ofOperator(GLSLTokenType.CARET, startLine, startColumn);
                
            default:
                return new GLSLToken(GLSLTokenType.ERROR, String.valueOf(c), startLine, startColumn);
        }
    }
    
    // ==================== Helper Methods ====================
    
    private char peek() {
        return source.charAt(pos);
    }
    
    private static boolean isWhitespace(char c) {
        return c < 128 && IS_WHITESPACE[c];
    }
    
    private static boolean isIdentStart(char c) {
        return c < 128 ? IS_IDENT_START[c] : Character.isLetter(c);
    }
    
    private static boolean isIdentPart(char c) {
        return c < 128 ? IS_IDENT_PART[c] : Character.isLetterOrDigit(c);
    }
    
    private static boolean isDigit(char c) {
        return c < 128 && IS_DIGIT[c];
    }
    
    private static boolean isHexDigit(char c) {
        return c < 128 && IS_HEX_DIGIT[c];
    }
    
    void reset() {
        this.source = null;
        this.pos = 0;
        this.length = 0;
        this.line = 1;
        this.column = 1;
        this.lineStart = 0;
    }
}

// ============================================================================
// AST NODE BASE CLASSES
// ============================================================================

/**
 * Base class for all AST nodes.
 * Uses flyweight pattern and object pooling for memory efficiency.
 */
abstract class GLSLASTNode {
    
    // Source location
    int line;
    int column;
    int endLine;
    int endColumn;
    
    // Parent reference for tree traversal
    GLSLASTNode parent;
    
    // Node flags for optimization passes
    int flags;
    
    static final int FLAG_CONSTANT = 1;
    static final int FLAG_PURE = 2;
    static final int FLAG_SIDE_EFFECT = 4;
    static final int FLAG_DEAD = 8;
    static final int FLAG_VISITED = 16;
    static final int FLAG_MODIFIED = 32;
    
    GLSLASTNode() {}
    
    GLSLASTNode(int line, int column) {
        this.line = line;
        this.column = column;
    }
    
    abstract void accept(GLSLASTVisitor visitor);
    abstract <T> T accept(GLSLASTVisitorWithResult<T> visitor);
    abstract GLSLASTNode copy(GLSLMemoryPool pool);
    
    void setLocation(int line, int column) {
        this.line = line;
        this.column = column;
    }
    
    void setEndLocation(int line, int column) {
        this.endLine = line;
        this.endColumn = column;
    }
    
    boolean hasFlag(int flag) { return (flags & flag) != 0; }
    void setFlag(int flag) { flags |= flag; }
    void clearFlag(int flag) { flags &= ~flag; }
    void clearAllFlags() { flags = 0; }
    
    // Reset for pooling
    void reset() {
        line = 0;
        column = 0;
        endLine = 0;
        endColumn = 0;
        parent = null;
        flags = 0;
    }
    
    // Child iteration
    abstract void forEachChild(Consumer<GLSLASTNode> consumer);
    abstract int getChildCount();
    abstract GLSLASTNode getChild(int index);
    abstract void setChild(int index, GLSLASTNode child);
}

// ============================================================================
// SHADER AST - ROOT NODE
// ============================================================================

/**
 * Root node representing a complete shader.
 */
final class GLSLShaderAST extends GLSLASTNode {
    
    GLSLVersion version;
    GLSLShaderType shaderType;
    String profile; // "core", "compatibility", or null
    
    // Top-level declarations
    final ArrayList<GLSLASTNode> declarations = new ArrayList<>();
    
    // Collected info
    final ArrayList<GLSLExtensionDecl> extensions = new ArrayList<>();
    final ArrayList<GLSLPrecisionDecl> precisionDecls = new ArrayList<>();
    final ArrayList<GLSLFunctionDecl> functions = new ArrayList<>();
    final ArrayList<GLSLVariableDecl> globalVariables = new ArrayList<>();
    final ArrayList<GLSLStructDecl> structs = new ArrayList<>();
    final ArrayList<GLSLInterfaceBlock> interfaceBlocks = new ArrayList<>();
    
    // Warnings and required extensions for output
    final ArrayList<String> warnings = new ArrayList<>();
    final EnumSet<GLSLExtension> requiredExtensions = EnumSet.noneOf(GLSLExtension.class);
    
    // Symbol table
    GLSLSymbolTable symbolTable;
    
    GLSLShaderAST() {}
    
    GLSLShaderAST(GLSLVersion version, GLSLShaderType shaderType) {
        this.version = version;
        this.shaderType = shaderType;
    }
    
    void addDeclaration(GLSLASTNode decl) {
        declarations.add(decl);
        decl.parent = this;
        
        // Categorize
        if (decl instanceof GLSLExtensionDecl) {
            extensions.add((GLSLExtensionDecl) decl);
        } else if (decl instanceof GLSLPrecisionDecl) {
            precisionDecls.add((GLSLPrecisionDecl) decl);
        } else if (decl instanceof GLSLFunctionDecl) {
            functions.add((GLSLFunctionDecl) decl);
        } else if (decl instanceof GLSLVariableDecl) {
            globalVariables.add((GLSLVariableDecl) decl);
        } else if (decl instanceof GLSLStructDecl) {
            structs.add((GLSLStructDecl) decl);
        } else if (decl instanceof GLSLInterfaceBlock) {
            interfaceBlocks.add((GLSLInterfaceBlock) decl);
        }
    }
    
    GLSLFunctionDecl findFunction(String name) {
        for (GLSLFunctionDecl func : functions) {
            if (name.equals(func.name)) {
                return func;
            }
        }
        return null;
    }
    
    GLSLFunctionDecl getMainFunction() {
        return findFunction("main");
    }
    
    Set<GLSLExtension> getRequiredExtensions() {
        return requiredExtensions;
    }
    
    List<String> getWarnings() {
        return warnings;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) {
        visitor.visitShader(this);
    }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) {
        return visitor.visitShader(this);
    }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLShaderAST copy = new GLSLShaderAST(version, shaderType);
        copy.profile = profile;
        for (GLSLASTNode decl : declarations) {
            copy.addDeclaration(decl.copy(pool));
        }
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        declarations.forEach(consumer);
    }
    
    @Override
    int getChildCount() { return declarations.size(); }
    
    @Override
    GLSLASTNode getChild(int index) { return declarations.get(index); }
    
    @Override
    void setChild(int index, GLSLASTNode child) {
        declarations.set(index, child);
        child.parent = this;
    }
    
    @Override
    void reset() {
        super.reset();
        version = null;
        shaderType = null;
        profile = null;
        declarations.clear();
        extensions.clear();
        precisionDecls.clear();
        functions.clear();
        globalVariables.clear();
        structs.clear();
        interfaceBlocks.clear();
        warnings.clear();
        requiredExtensions.clear();
        symbolTable = null;
    }
}

// ============================================================================
// TYPE SYSTEM
// ============================================================================

/**
 * Represents GLSL types with full support for arrays, structs, and qualifiers.
 */
final class GLSLType {
    
    // Base types
    enum BaseType {
        VOID, BOOL, INT, UINT, FLOAT, DOUBLE,
        VEC2, VEC3, VEC4,
        DVEC2, DVEC3, DVEC4,
        BVEC2, BVEC3, BVEC4,
        IVEC2, IVEC3, IVEC4,
        UVEC2, UVEC3, UVEC4,
        MAT2, MAT3, MAT4,
        MAT2X2, MAT2X3, MAT2X4,
        MAT3X2, MAT3X3, MAT3X4,
        MAT4X2, MAT4X3, MAT4X4,
        DMAT2, DMAT3, DMAT4,
        DMAT2X2, DMAT2X3, DMAT2X4,
        DMAT3X2, DMAT3X3, DMAT3X4,
        DMAT4X2, DMAT4X3, DMAT4X4,
        SAMPLER_1D, SAMPLER_2D, SAMPLER_3D, SAMPLER_CUBE,
        SAMPLER_1D_SHADOW, SAMPLER_2D_SHADOW, SAMPLER_CUBE_SHADOW,
        SAMPLER_1D_ARRAY, SAMPLER_2D_ARRAY,
        SAMPLER_1D_ARRAY_SHADOW, SAMPLER_2D_ARRAY_SHADOW,
        SAMPLER_CUBE_ARRAY, SAMPLER_CUBE_ARRAY_SHADOW,
        SAMPLER_2D_RECT, SAMPLER_2D_RECT_SHADOW,
        SAMPLER_BUFFER, SAMPLER_2D_MS, SAMPLER_2D_MS_ARRAY,
        ISAMPLER_1D, ISAMPLER_2D, ISAMPLER_3D, ISAMPLER_CUBE,
        ISAMPLER_1D_ARRAY, ISAMPLER_2D_ARRAY, ISAMPLER_CUBE_ARRAY,
        ISAMPLER_2D_RECT, ISAMPLER_BUFFER, ISAMPLER_2D_MS, ISAMPLER_2D_MS_ARRAY,
        USAMPLER_1D, USAMPLER_2D, USAMPLER_3D, USAMPLER_CUBE,
        USAMPLER_1D_ARRAY, USAMPLER_2D_ARRAY, USAMPLER_CUBE_ARRAY,
        USAMPLER_2D_RECT, USAMPLER_BUFFER, USAMPLER_2D_MS, USAMPLER_2D_MS_ARRAY,
        IMAGE_1D, IMAGE_2D, IMAGE_3D, IMAGE_CUBE,
        IMAGE_1D_ARRAY, IMAGE_2D_ARRAY, IMAGE_CUBE_ARRAY,
        IMAGE_2D_RECT, IMAGE_BUFFER, IMAGE_2D_MS, IMAGE_2D_MS_ARRAY,
        IIMAGE_1D, IIMAGE_2D, IIMAGE_3D, IIMAGE_CUBE,
        IIMAGE_1D_ARRAY, IIMAGE_2D_ARRAY, IIMAGE_CUBE_ARRAY,
        IIMAGE_2D_RECT, IIMAGE_BUFFER, IIMAGE_2D_MS, IIMAGE_2D_MS_ARRAY,
        UIMAGE_1D, UIMAGE_2D, UIMAGE_3D, UIMAGE_CUBE,
        UIMAGE_1D_ARRAY, UIMAGE_2D_ARRAY, UIMAGE_CUBE_ARRAY,
        UIMAGE_2D_RECT, UIMAGE_BUFFER, UIMAGE_2D_MS, UIMAGE_2D_MS_ARRAY,
        ATOMIC_UINT,
        STRUCT,
        BLOCK,
        ERROR
    }
    
    final BaseType baseType;
    final String structName; // For struct types
    final int[] arrayDimensions; // null for non-arrays
    final GLSLTypeQualifiers qualifiers;
    
    // Cached common types
    private static final Map<BaseType, GLSLType> SIMPLE_TYPES = new EnumMap<>(BaseType.class);
    
    static {
        for (BaseType bt : BaseType.values()) {
            if (bt != BaseType.STRUCT && bt != BaseType.BLOCK && bt != BaseType.ERROR) {
                SIMPLE_TYPES.put(bt, new GLSLType(bt));
            }
        }
    }
    
    private GLSLType(BaseType baseType) {
        this.baseType = baseType;
        this.structName = null;
        this.arrayDimensions = null;
        this.qualifiers = null;
    }
    
    GLSLType(BaseType baseType, String structName, int[] arrayDimensions, GLSLTypeQualifiers qualifiers) {
        this.baseType = baseType;
        this.structName = structName;
        this.arrayDimensions = arrayDimensions;
        this.qualifiers = qualifiers;
    }
    
    static GLSLType simple(BaseType baseType) {
        GLSLType cached = SIMPLE_TYPES.get(baseType);
        return cached != null ? cached : new GLSLType(baseType);
    }
    
    static GLSLType struct(String name) {
        return new GLSLType(BaseType.STRUCT, name, null, null);
    }
    
    static GLSLType array(GLSLType elementType, int size) {
        int[] dims = elementType.arrayDimensions;
        int[] newDims;
        if (dims == null) {
            newDims = new int[] { size };
        } else {
            newDims = new int[dims.length + 1];
            newDims[0] = size;
            System.arraycopy(dims, 0, newDims, 1, dims.length);
        }
        return new GLSLType(elementType.baseType, elementType.structName, newDims, elementType.qualifiers);
    }
    
    GLSLType withQualifiers(GLSLTypeQualifiers quals) {
        return new GLSLType(baseType, structName, arrayDimensions, quals);
    }
    
    boolean isScalar() {
        return baseType == BaseType.BOOL || baseType == BaseType.INT || 
               baseType == BaseType.UINT || baseType == BaseType.FLOAT || 
               baseType == BaseType.DOUBLE;
    }
    
    boolean isVector() {
        return baseType.name().contains("VEC");
    }
    
    boolean isMatrix() {
        return baseType.name().contains("MAT");
    }
    
    boolean isSampler() {
        return baseType.name().contains("SAMPLER");
    }
    
    boolean isImage() {
        return baseType.name().contains("IMAGE");
    }
    
    boolean isArray() {
        return arrayDimensions != null && arrayDimensions.length > 0;
    }
    
    boolean isStruct() {
        return baseType == BaseType.STRUCT;
    }
    
    boolean isOpaque() {
        return isSampler() || isImage() || baseType == BaseType.ATOMIC_UINT;
    }
    
    int getVectorSize() {
        String name = baseType.name();
        if (name.endsWith("2")) return 2;
        if (name.endsWith("3")) return 3;
        if (name.endsWith("4")) return 4;
        if (isScalar()) return 1;
        return 0;
    }
    
    int[] getMatrixDimensions() {
        String name = baseType.name();
        if (name.startsWith("MAT") || name.startsWith("DMAT")) {
            if (name.contains("X")) {
                // Non-square matrix
                int xIdx = name.indexOf('X');
                int rows = name.charAt(xIdx - 1) - '0';
                int cols = name.charAt(xIdx + 1) - '0';
                return new int[] { rows, cols };
            } else {
                // Square matrix
                int size = name.charAt(name.length() - 1) - '0';
                return new int[] { size, size };
            }
        }
        return null;
    }
    
    BaseType getScalarType() {
        String name = baseType.name();
        if (name.startsWith("D") && (name.contains("VEC") || name.contains("MAT"))) {
            return BaseType.DOUBLE;
        }
        if (name.startsWith("B")) return BaseType.BOOL;
        if (name.startsWith("I")) return BaseType.INT;
        if (name.startsWith("U")) return BaseType.UINT;
        if (isScalar()) return baseType;
        return BaseType.FLOAT;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GLSLType)) return false;
        GLSLType other = (GLSLType) o;
        return baseType == other.baseType && 
               Objects.equals(structName, other.structName) &&
               Arrays.equals(arrayDimensions, other.arrayDimensions);
    }
    
    @Override
    public int hashCode() {
        int result = baseType.hashCode();
        result = 31 * result + (structName != null ? structName.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(arrayDimensions);
        return result;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (qualifiers != null) {
            sb.append(qualifiers).append(" ");
        }
        
        if (baseType == BaseType.STRUCT) {
            sb.append(structName != null ? structName : "struct");
        } else {
            sb.append(baseTypeToString(baseType));
        }
        
        if (arrayDimensions != null) {
            for (int dim : arrayDimensions) {
                sb.append("[");
                if (dim > 0) sb.append(dim);
                sb.append("]");
            }
        }
        
        return sb.toString();
    }
    
    private static String baseTypeToString(BaseType bt) {
        String name = bt.name().toLowerCase();
        // Convert underscores for samplers/images
        name = name.replace("_", "");
        // Handle special cases
        switch (bt) {
            case SAMPLER_1D: return "sampler1D";
            case SAMPLER_2D: return "sampler2D";
            case SAMPLER_3D: return "sampler3D";
            case SAMPLER_CUBE: return "samplerCube";
            // ... add more as needed
            default: return name;
        }
    }
    
    // Convert token type to base type
    static BaseType fromTokenType(GLSLTokenType tokenType) {
        switch (tokenType) {
            case VOID: return BaseType.VOID;
            case BOOL: return BaseType.BOOL;
            case INT: return BaseType.INT;
            case UINT: return BaseType.UINT;
            case FLOAT: return BaseType.FLOAT;
            case DOUBLE: return BaseType.DOUBLE;
            case VEC2: return BaseType.VEC2;
            case VEC3: return BaseType.VEC3;
            case VEC4: return BaseType.VEC4;
            case DVEC2: return BaseType.DVEC2;
            case DVEC3: return BaseType.DVEC3;
            case DVEC4: return BaseType.DVEC4;
            case BVEC2: return BaseType.BVEC2;
            case BVEC3: return BaseType.BVEC3;
            case BVEC4: return BaseType.BVEC4;
            case IVEC2: return BaseType.IVEC2;
            case IVEC3: return BaseType.IVEC3;
            case IVEC4: return BaseType.IVEC4;
            case UVEC2: return BaseType.UVEC2;
            case UVEC3: return BaseType.UVEC3;
            case UVEC4: return BaseType.UVEC4;
            case MAT2: return BaseType.MAT2;
            case MAT3: return BaseType.MAT3;
            case MAT4: return BaseType.MAT4;
            case MAT2X2: return BaseType.MAT2X2;
            case MAT2X3: return BaseType.MAT2X3;
            case MAT2X4: return BaseType.MAT2X4;
            case MAT3X2: return BaseType.MAT3X2;
            case MAT3X3: return BaseType.MAT3X3;
            case MAT3X4: return BaseType.MAT3X4;
            case MAT4X2: return BaseType.MAT4X2;
            case MAT4X3: return BaseType.MAT4X3;
            case MAT4X4: return BaseType.MAT4X4;
            case DMAT2: return BaseType.DMAT2;
            case DMAT3: return BaseType.DMAT3;
            case DMAT4: return BaseType.DMAT4;
            case SAMPLER1D: return BaseType.SAMPLER_1D;
            case SAMPLER2D: return BaseType.SAMPLER_2D;
            case SAMPLER3D: return BaseType.SAMPLER_3D;
            case SAMPLERCUBE: return BaseType.SAMPLER_CUBE;
            case SAMPLER2DSHADOW: return BaseType.SAMPLER_2D_SHADOW;
            case ATOMIC_UINT: return BaseType.ATOMIC_UINT;
            default: return BaseType.ERROR;
        }
    }
}

/**
 * Type qualifiers (storage, interpolation, layout, etc.)
 */
final class GLSLTypeQualifiers {
    
    // Storage qualifiers
    enum Storage {
        NONE, CONST, IN, OUT, INOUT, UNIFORM, BUFFER, SHARED, ATTRIBUTE, VARYING
    }
    
    // Interpolation qualifiers  
    enum Interpolation {
        NONE, FLAT, SMOOTH, NOPERSPECTIVE
    }
    
    // Precision qualifiers
    enum Precision {
        NONE, HIGHP, MEDIUMP, LOWP
    }
    
    Storage storage = Storage.NONE;
    Interpolation interpolation = Interpolation.NONE;
    Precision precision = Precision.NONE;
    boolean centroid;
    boolean sample;
    boolean patch;
    boolean invariant;
    boolean precise;
    
    // Memory qualifiers
    boolean coherent;
    boolean volatileQ;
    boolean restrict;
    boolean readonly;
    boolean writeonly;
    
    // Layout qualifiers
    GLSLLayoutQualifier layout;
    
    GLSLTypeQualifiers() {}
    
    GLSLTypeQualifiers copy() {
        GLSLTypeQualifiers copy = new GLSLTypeQualifiers();
        copy.storage = storage;
        copy.interpolation = interpolation;
        copy.precision = precision;
        copy.centroid = centroid;
        copy.sample = sample;
        copy.patch = patch;
        copy.invariant = invariant;
        copy.precise = precise;
        copy.coherent = coherent;
        copy.volatileQ = volatileQ;
        copy.restrict = restrict;
        copy.readonly = readonly;
        copy.writeonly = writeonly;
        copy.layout = layout != null ? layout.copy() : null;
        return copy;
    }
    
    boolean hasAnyQualifier() {
        return storage != Storage.NONE || interpolation != Interpolation.NONE ||
               precision != Precision.NONE || centroid || sample || patch ||
               invariant || precise || coherent || volatileQ || restrict ||
               readonly || writeonly || layout != null;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (invariant) sb.append("invariant ");
        if (precise) sb.append("precise ");
        if (layout != null) sb.append(layout).append(" ");
        if (centroid) sb.append("centroid ");
        if (sample) sb.append("sample ");
        if (patch) sb.append("patch ");
        if (interpolation != Interpolation.NONE) sb.append(interpolation.name().toLowerCase()).append(" ");
        if (storage != Storage.NONE) sb.append(storage.name().toLowerCase()).append(" ");
        if (precision != Precision.NONE) sb.append(precision.name().toLowerCase()).append(" ");
        if (coherent) sb.append("coherent ");
        if (volatileQ) sb.append("volatile ");
        if (restrict) sb.append("restrict ");
        if (readonly) sb.append("readonly ");
        if (writeonly) sb.append("writeonly ");
        return sb.toString().trim();
    }
}

/**
 * Layout qualifier with all possible parameters.
 */
final class GLSLLayoutQualifier {
    
    // Common layout qualifiers
    int location = -1;
    int binding = -1;
    int offset = -1;
    int component = -1;
    int index = -1;
    int set = -1;
    
    // UBO/SSBO layout
    enum PackingMode { NONE, SHARED, PACKED, STD140, STD430 }
    PackingMode packing = PackingMode.NONE;
    
    enum MatrixLayout { NONE, ROW_MAJOR, COLUMN_MAJOR }
    MatrixLayout matrixLayout = MatrixLayout.NONE;
    
    // Compute shader
    int localSizeX = -1;
    int localSizeY = -1;
    int localSizeZ = -1;
    
    // Geometry/Tessellation
    String primitiveType; // points, lines, triangles, etc.
    int maxVertices = -1;
    int vertices = -1;
    int invocations = -1;
    
    // Fragment shader
    boolean originUpperLeft;
    boolean pixelCenterInteger;
    boolean earlyFragmentTests;
    boolean depthAny;
    boolean depthGreater;
    boolean depthLess;
    boolean depthUnchanged;
    
    // Image format
    String imageFormat;
    
    // General parameters map for extension qualifiers
    Map<String, String> otherParams;
    
    GLSLLayoutQualifier copy() {
        GLSLLayoutQualifier copy = new GLSLLayoutQualifier();
        copy.location = location;
        copy.binding = binding;
        copy.offset = offset;
        copy.component = component;
        copy.index = index;
        copy.set = set;
        copy.packing = packing;
        copy.matrixLayout = matrixLayout;
        copy.localSizeX = localSizeX;
        copy.localSizeY = localSizeY;
        copy.localSizeZ = localSizeZ;
        copy.primitiveType = primitiveType;
        copy.maxVertices = maxVertices;
        copy.vertices = vertices;
        copy.invocations = invocations;
        copy.originUpperLeft = originUpperLeft;
        copy.pixelCenterInteger = pixelCenterInteger;
        copy.earlyFragmentTests = earlyFragmentTests;
        copy.depthAny = depthAny;
        copy.depthGreater = depthGreater;
        copy.depthLess = depthLess;
        copy.depthUnchanged = depthUnchanged;
        copy.imageFormat = imageFormat;
        if (otherParams != null) {
            copy.otherParams = new HashMap<>(otherParams);
        }
        return copy;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("layout(");
        boolean first = true;
        
        if (location >= 0) { sb.append("location = ").append(location); first = false; }
        if (binding >= 0) { if (!first) sb.append(", "); sb.append("binding = ").append(binding); first = false; }
        if (offset >= 0) { if (!first) sb.append(", "); sb.append("offset = ").append(offset); first = false; }
        if (component >= 0) { if (!first) sb.append(", "); sb.append("component = ").append(component); first = false; }
        if (set >= 0) { if (!first) sb.append(", "); sb.append("set = ").append(set); first = false; }
        if (packing != PackingMode.NONE) { if (!first) sb.append(", "); sb.append(packing.name().toLowerCase()); first = false; }
        if (matrixLayout != MatrixLayout.NONE) { if (!first) sb.append(", "); sb.append(matrixLayout.name().toLowerCase().replace("_", " ")); first = false; }
        if (localSizeX >= 0) { if (!first) sb.append(", "); sb.append("local_size_x = ").append(localSizeX); first = false; }
        if (localSizeY >= 0) { if (!first) sb.append(", "); sb.append("local_size_y = ").append(localSizeY); first = false; }
        if (localSizeZ >= 0) { if (!first) sb.append(", "); sb.append("local_size_z = ").append(localSizeZ); first = false; }
        if (primitiveType != null) { if (!first) sb.append(", "); sb.append(primitiveType); first = false; }
        if (maxVertices >= 0) { if (!first) sb.append(", "); sb.append("max_vertices = ").append(maxVertices); first = false; }
        if (imageFormat != null) { if (!first) sb.append(", "); sb.append(imageFormat); first = false; }
        
        sb.append(")");
        return sb.toString();
    }
}

// ============================================================================
// DECLARATION NODES
// ============================================================================

/**
 * Extension declaration: #extension name : behavior
 */
final class GLSLExtensionDecl extends GLSLASTNode {
    
    String extensionName;
    String behavior; // enable, require, warn, disable
    
    GLSLExtensionDecl() {}
    
    GLSLExtensionDecl(String name, String behavior) {
        this.extensionName = name;
        this.behavior = behavior;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitExtensionDecl(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitExtensionDecl(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLExtensionDecl copy = new GLSLExtensionDecl(extensionName, behavior);
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override void forEachChild(Consumer<GLSLASTNode> consumer) {}
    @Override int getChildCount() { return 0; }
    @Override GLSLASTNode getChild(int index) { return null; }
    @Override void setChild(int index, GLSLASTNode child) {}
}

/**
 * Precision declaration: precision highp float;
 */
final class GLSLPrecisionDecl extends GLSLASTNode {
    
    GLSLTypeQualifiers.Precision precision;
    GLSLType type;
    
    GLSLPrecisionDecl() {}
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitPrecisionDecl(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitPrecisionDecl(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLPrecisionDecl copy = new GLSLPrecisionDecl();
        copy.precision = precision;
        copy.type = type;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override void forEachChild(Consumer<GLSLASTNode> consumer) {}
    @Override int getChildCount() { return 0; }
    @Override GLSLASTNode getChild(int index) { return null; }
    @Override void setChild(int index, GLSLASTNode child) {}
}

/**
 * Variable declaration with optional initializer.
 */
final class GLSLVariableDecl extends GLSLASTNode {
    
    GLSLType type;
    String name;
    GLSLExpression initializer; // may be null
    int[] arrayDimensions; // additional array dimensions on variable
    
    // Semantic info
    GLSLSymbol symbol;
    
    GLSLVariableDecl() {}
    
    GLSLVariableDecl(GLSLType type, String name) {
        this.type = type;
        this.name = name;
    }
    
    GLSLVariableDecl(GLSLType type, String name, GLSLExpression initializer) {
        this.type = type;
        this.name = name;
        this.initializer = initializer;
        if (initializer != null) {
            initializer.parent = this;
        }
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitVariableDecl(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitVariableDecl(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLVariableDecl copy = new GLSLVariableDecl();
        copy.type = type;
        copy.name = name;
        if (initializer != null) {
            copy.initializer = (GLSLExpression) initializer.copy(pool);
            copy.initializer.parent = copy;
        }
        if (arrayDimensions != null) {
            copy.arrayDimensions = arrayDimensions.clone();
        }
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        if (initializer != null) consumer.accept(initializer);
    }
    
    @Override int getChildCount() { return initializer != null ? 1 : 0; }
    @Override GLSLASTNode getChild(int index) { return index == 0 ? initializer : null; }
    @Override void setChild(int index, GLSLASTNode child) {
        if (index == 0) { initializer = (GLSLExpression) child; child.parent = this; }
    }
}

/**
 * Struct declaration.
 */
final class GLSLStructDecl extends GLSLASTNode {
    
    String name;
    final ArrayList<GLSLVariableDecl> members = new ArrayList<>();
    
    // Semantic info
    GLSLSymbol symbol;
    
    GLSLStructDecl() {}
    
    GLSLStructDecl(String name) {
        this.name = name;
    }
    
    void addMember(GLSLVariableDecl member) {
        members.add(member);
        member.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitStructDecl(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitStructDecl(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLStructDecl copy = new GLSLStructDecl(name);
        for (GLSLVariableDecl member : members) {
            copy.addMember((GLSLVariableDecl) member.copy(pool));
        }
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) { members.forEach(consumer); }
    
    @Override int getChildCount() { return members.size(); }
    @Override GLSLASTNode getChild(int index) { return members.get(index); }
    @Override void setChild(int index, GLSLASTNode child) {
        members.set(index, (GLSLVariableDecl) child);
        child.parent = this;
    }
}

/**
 * Interface block (uniform block, buffer block, in/out block).
 */
final class GLSLInterfaceBlock extends GLSLASTNode {
    
    GLSLTypeQualifiers qualifiers;
    String blockName;
    String instanceName; // may be null
    int[] arrayDimensions; // may be null
    final ArrayList<GLSLVariableDecl> members = new ArrayList<>();
    
    GLSLInterfaceBlock() {}
    
    void addMember(GLSLVariableDecl member) {
        members.add(member);
        member.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitInterfaceBlock(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitInterfaceBlock(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLInterfaceBlock copy = new GLSLInterfaceBlock();
        copy.qualifiers = qualifiers != null ? qualifiers.copy() : null;
        copy.blockName = blockName;
        copy.instanceName = instanceName;
        if (arrayDimensions != null) {
            copy.arrayDimensions = arrayDimensions.clone();
        }
        for (GLSLVariableDecl member : members) {
            copy.addMember((GLSLVariableDecl) member.copy(pool));
        }
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) { members.forEach(consumer); }
    
    @Override int getChildCount() { return members.size(); }
    @Override GLSLASTNode getChild(int index) { return members.get(index); }
    @Override void setChild(int index, GLSLASTNode child) {
        members.set(index, (GLSLVariableDecl) child);
        child.parent = this;
    }
}

/**
 * Function declaration with optional body.
 */
final class GLSLFunctionDecl extends GLSLASTNode {
    
    GLSLType returnType;
    String name;
    final ArrayList<GLSLParameterDecl> parameters = new ArrayList<>();
    GLSLBlockStmt body; // null for prototypes
    
    // Semantic info
    GLSLSymbol symbol;
    boolean isPrototype;
    boolean isBuiltin;
    
    GLSLFunctionDecl() {}
    
    GLSLFunctionDecl(GLSLType returnType, String name) {
        this.returnType = returnType;
        this.name = name;
    }
    
    void addParameter(GLSLParameterDecl param) {
        parameters.add(param);
        param.parent = this;
    }
    
    void setBody(GLSLBlockStmt body) {
        this.body = body;
        if (body != null) {
            body.parent = this;
        }
    }
    
    boolean isMain() {
        return "main".equals(name);
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitFunctionDecl(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitFunctionDecl(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLFunctionDecl copy = new GLSLFunctionDecl(returnType, name);
        for (GLSLParameterDecl param : parameters) {
            copy.addParameter((GLSLParameterDecl) param.copy(pool));
        }
        if (body != null) {
            copy.setBody((GLSLBlockStmt) body.copy(pool));
        }
        copy.isPrototype = isPrototype;
        copy.isBuiltin = isBuiltin;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        parameters.forEach(consumer);
        if (body != null) consumer.accept(body);
    }
    
    @Override int getChildCount() { return parameters.size() + (body != null ? 1 : 0); }
    @Override GLSLASTNode getChild(int index) {
        if (index < parameters.size()) return parameters.get(index);
        if (index == parameters.size() && body != null) return body;
        return null;
    }
    @Override void setChild(int index, GLSLASTNode child) {
        if (index < parameters.size()) {
            parameters.set(index, (GLSLParameterDecl) child);
            child.parent = this;
        } else if (index == parameters.size()) {
            body = (GLSLBlockStmt) child;
            child.parent = this;
        }
    }
}

/**
 * Function parameter declaration.
 */
final class GLSLParameterDecl extends GLSLASTNode {
    
    GLSLType type;
    String name; // may be null for unnamed parameters
    GLSLTypeQualifiers.Storage qualifier; // in, out, inout
    
    GLSLParameterDecl() {}
    
    GLSLParameterDecl(GLSLType type, String name) {
        this.type = type;
        this.name = name;
        this.qualifier = GLSLTypeQualifiers.Storage.IN;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitParameterDecl(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitParameterDecl(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLParameterDecl copy = new GLSLParameterDecl(type, name);
        copy.qualifier = qualifier;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override void forEachChild(Consumer<GLSLASTNode> consumer) {}
    @Override int getChildCount() { return 0; }
    @Override GLSLASTNode getChild(int index) { return null; }
    @Override void setChild(int index, GLSLASTNode child) {}
}

// ============================================================================
// STATEMENT NODES
// ============================================================================

/**
 * Base class for all statements.
 */
abstract class GLSLStatement extends GLSLASTNode {
    GLSLStatement() {}
}

/**
 * Block statement (compound statement).
 */
final class GLSLBlockStmt extends GLSLStatement {
    
    final ArrayList<GLSLStatement> statements = new ArrayList<>();
    GLSLSymbolTable localSymbols; // Scope for this block
    
    GLSLBlockStmt() {}
    
    void addStatement(GLSLStatement stmt) {
        statements.add(stmt);
        stmt.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitBlockStmt(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitBlockStmt(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLBlockStmt copy = new GLSLBlockStmt();
        for (GLSLStatement stmt : statements) {
            copy.addStatement((GLSLStatement) stmt.copy(pool));
        }
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) { statements.forEach(consumer); }
    
    @Override int getChildCount() { return statements.size(); }
    @Override GLSLASTNode getChild(int index) { return statements.get(index); }
    @Override void setChild(int index, GLSLASTNode child) {
        statements.set(index, (GLSLStatement) child);
        child.parent = this;
    }
    
    @Override
    void reset() {
        super.reset();
        statements.clear();
        localSymbols = null;
    }
}

/**
 * Expression statement.
 */
final class GLSLExpressionStmt extends GLSLStatement {
    
    GLSLExpression expression; // may be null for empty statement
    
    GLSLExpressionStmt() {}
    
    GLSLExpressionStmt(GLSLExpression expr) {
        this.expression = expr;
        if (expr != null) expr.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitExpressionStmt(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitExpressionStmt(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLExpressionStmt copy = new GLSLExpressionStmt();
        if (expression != null) {
            copy.expression = (GLSLExpression) expression.copy(pool);
            copy.expression.parent = copy;
        }
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        if (expression != null) consumer.accept(expression);
    }
    
    @Override int getChildCount() { return expression != null ? 1 : 0; }
    @Override GLSLASTNode getChild(int index) { return index == 0 ? expression : null; }
    @Override void setChild(int index, GLSLASTNode child) {
        if (index == 0) { expression = (GLSLExpression) child; child.parent = this; }
    }
}

/**
 * Declaration statement.
 */
final class GLSLDeclarationStmt extends GLSLStatement {
    
    final ArrayList<GLSLVariableDecl> declarations = new ArrayList<>();
    
    GLSLDeclarationStmt() {}
    
    void addDeclaration(GLSLVariableDecl decl) {
        declarations.add(decl);
        decl.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitDeclarationStmt(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitDeclarationStmt(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLDeclarationStmt copy = new GLSLDeclarationStmt();
        for (GLSLVariableDecl decl : declarations) {
            copy.addDeclaration((GLSLVariableDecl) decl.copy(pool));
        }
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) { declarations.forEach(consumer); }
    
    @Override int getChildCount() { return declarations.size(); }
    @Override GLSLASTNode getChild(int index) { return declarations.get(index); }
    @Override void setChild(int index, GLSLASTNode child) {
        declarations.set(index, (GLSLVariableDecl) child);
        child.parent = this;
    }
}

/**
 * If statement.
 */
final class GLSLIfStmt extends GLSLStatement {
    
    GLSLExpression condition;
    GLSLStatement thenBranch;
    GLSLStatement elseBranch; // may be null
    
    GLSLIfStmt() {}
    
    GLSLIfStmt(GLSLExpression condition, GLSLStatement thenBranch, GLSLStatement elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
        condition.parent = this;
        thenBranch.parent = this;
        if (elseBranch != null) elseBranch.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitIfStmt(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitIfStmt(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLIfStmt copy = new GLSLIfStmt();
        copy.condition = (GLSLExpression) condition.copy(pool);
        copy.condition.parent = copy;
        copy.thenBranch = (GLSLStatement) thenBranch.copy(pool);
        copy.thenBranch.parent = copy;
        if (elseBranch != null) {
            copy.elseBranch = (GLSLStatement) elseBranch.copy(pool);
            copy.elseBranch.parent = copy;
        }
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        consumer.accept(condition);
        consumer.accept(thenBranch);
        if (elseBranch != null) consumer.accept(elseBranch);
    }
    
    @Override int getChildCount() { return elseBranch != null ? 3 : 2; }
    @Override GLSLASTNode getChild(int index) {
        switch (index) {
            case 0: return condition;
            case 1: return thenBranch;
            case 2: return elseBranch;
            default: return null;
        }
    }
    @Override void setChild(int index, GLSLASTNode child) {
        switch (index) {
            case 0: condition = (GLSLExpression) child; break;
            case 1: thenBranch = (GLSLStatement) child; break;
            case 2: elseBranch = (GLSLStatement) child; break;
        }
        if (child != null) child.parent = this;
    }
}

/**
 * For loop statement.
 */
final class GLSLForStmt extends GLSLStatement {
    
    GLSLStatement init; // may be declaration or expression
    GLSLExpression condition; // may be null
    GLSLExpression update; // may be null
    GLSLStatement body;
    
    GLSLForStmt() {}
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitForStmt(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitForStmt(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLForStmt copy = new GLSLForStmt();
        if (init != null) { copy.init = (GLSLStatement) init.copy(pool); copy.init.parent = copy; }
        if (condition != null) { copy.condition = (GLSLExpression) condition.copy(pool); copy.condition.parent = copy; }
        if (update != null) { copy.update = (GLSLExpression) update.copy(pool); copy.update.parent = copy; }
        copy.body = (GLSLStatement) body.copy(pool);
        copy.body.parent = copy;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        if (init != null) consumer.accept(init);
        if (condition != null) consumer.accept(condition);
        if (update != null) consumer.accept(update);
        consumer.accept(body);
    }
    
    @Override int getChildCount() {
        int count = 1; // body
        if (init != null) count++;
        if (condition != null) count++;
        if (update != null) count++;
        return count;
    }
    @Override GLSLASTNode getChild(int index) {
        if (index == 0 && init != null) return init;
        if (init != null) index--;
        if (index == 0 && condition != null) return condition;
        if (condition != null) index--;
        if (index == 0 && update != null) return update;
        if (update != null) index--;
        if (index == 0) return body;
        return null;
    }
    @Override void setChild(int index, GLSLASTNode child) {
        // Complex indexing - simplified
        child.parent = this;
    }
}

/**
 * While loop statement.
 */
final class GLSLWhileStmt extends GLSLStatement {
    
    GLSLExpression condition;
    GLSLStatement body;
    
    GLSLWhileStmt() {}
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitWhileStmt(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitWhileStmt(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLWhileStmt copy = new GLSLWhileStmt();
        copy.condition = (GLSLExpression) condition.copy(pool);
        copy.condition.parent = copy;
        copy.body = (GLSLStatement) body.copy(pool);
        copy.body.parent = copy;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        consumer.accept(condition);
        consumer.accept(body);
    }
    
    @Override int getChildCount() { return 2; }
    @Override GLSLASTNode getChild(int index) {
        return index == 0 ? condition : (index == 1 ? body : null);
    }
    @Override void setChild(int index, GLSLASTNode child) {
        if (index == 0) condition = (GLSLExpression) child;
        else if (index == 1) body = (GLSLStatement) child;
        child.parent = this;
    }
}

/**
 * Do-while loop statement.
 */
final class GLSLDoWhileStmt extends GLSLStatement {
    
    GLSLStatement body;
    GLSLExpression condition;
    
    GLSLDoWhileStmt() {}
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitDoWhileStmt(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitDoWhileStmt(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLDoWhileStmt copy = new GLSLDoWhileStmt();
        copy.body = (GLSLStatement) body.copy(pool);
        copy.body.parent = copy;
        copy.condition = (GLSLExpression) condition.copy(pool);
        copy.condition.parent = copy;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        consumer.accept(body);
        consumer.accept(condition);
    }
    
    @Override int getChildCount() { return 2; }
    @Override GLSLASTNode getChild(int index) {
        return index == 0 ? body : (index == 1 ? condition : null);
    }
    @Override void setChild(int index, GLSLASTNode child) {
        if (index == 0) body = (GLSLStatement) child;
        else if (index == 1) condition = (GLSLExpression) child;
        child.parent = this;
    }
}

/**
 * Switch statement.
 */
final class GLSLSwitchStmt extends GLSLStatement {
    
    GLSLExpression expression;
    final ArrayList<GLSLCaseLabel> cases = new ArrayList<>();
    final ArrayList<GLSLStatement> statements = new ArrayList<>();
    
    GLSLSwitchStmt() {}
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitSwitchStmt(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitSwitchStmt(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLSwitchStmt copy = new GLSLSwitchStmt();
        copy.expression = (GLSLExpression) expression.copy(pool);
        copy.expression.parent = copy;
        for (GLSLCaseLabel c : cases) {
            GLSLCaseLabel cc = (GLSLCaseLabel) c.copy(pool);
            cc.parent = copy;
            copy.cases.add(cc);
        }
        for (GLSLStatement s : statements) {
            GLSLStatement sc = (GLSLStatement) s.copy(pool);
            sc.parent = copy;
            copy.statements.add(sc);
        }
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        consumer.accept(expression);
        cases.forEach(consumer);
        statements.forEach(consumer);
    }
    
    @Override int getChildCount() { return 1 + cases.size() + statements.size(); }
    @Override GLSLASTNode getChild(int index) {
        if (index == 0) return expression;
        index--;
        if (index < cases.size()) return cases.get(index);
        index -= cases.size();
        if (index < statements.size()) return statements.get(index);
        return null;
    }
    @Override void setChild(int index, GLSLASTNode child) { child.parent = this; }
}

/**
 * Case label in switch statement.
 */
final class GLSLCaseLabel extends GLSLStatement {
    
    GLSLExpression value; // null for default
    boolean isDefault;
    
    GLSLCaseLabel() {}
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitCaseLabel(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitCaseLabel(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLCaseLabel copy = new GLSLCaseLabel();
        copy.isDefault = isDefault;
        if (value != null) {
            copy.value = (GLSLExpression) value.copy(pool);
            copy.value.parent = copy;
        }
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        if (value != null) consumer.accept(value);
    }
    
    @Override int getChildCount() { return value != null ? 1 : 0; }
    @Override GLSLASTNode getChild(int index) { return index == 0 ? value : null; }
    @Override void setChild(int index, GLSLASTNode child) {
        if (index == 0) { value = (GLSLExpression) child; child.parent = this; }
    }
}

/**
 * Return statement.
 */
final class GLSLReturnStmt extends GLSLStatement {
    
    GLSLExpression value; // may be null
    
    GLSLReturnStmt() {}
    
    GLSLReturnStmt(GLSLExpression value) {
        this.value = value;
        if (value != null) value.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitReturnStmt(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitReturnStmt(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLReturnStmt copy = new GLSLReturnStmt();
        if (value != null) {
            copy.value = (GLSLExpression) value.copy(pool);
            copy.value.parent = copy;
        }
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        if (value != null) consumer.accept(value);
    }
    
    @Override int getChildCount() { return value != null ? 1 : 0; }
    @Override GLSLASTNode getChild(int index) { return index == 0 ? value : null; }
    @Override void setChild(int index, GLSLASTNode child) {
        if (index == 0) { value = (GLSLExpression) child; child.parent = this; }
    }
}

/**
 * Break statement.
 */
final class GLSLBreakStmt extends GLSLStatement {
    GLSLBreakStmt() {}
    
    @Override void accept(GLSLASTVisitor visitor) { visitor.visitBreakStmt(this); }
    @Override <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitBreakStmt(this); }
    @Override GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLBreakStmt copy = new GLSLBreakStmt();
        copy.setLocation(line, column);
        return copy;
    }
    @Override void forEachChild(Consumer<GLSLASTNode> consumer) {}
    @Override int getChildCount() { return 0; }
    @Override GLSLASTNode getChild(int index) { return null; }
    @Override void setChild(int index, GLSLASTNode child) {}
}

/**
 * Continue statement.
 */
final class GLSLContinueStmt extends GLSLStatement {
    GLSLContinueStmt() {}
    
    @Override void accept(GLSLASTVisitor visitor) { visitor.visitContinueStmt(this); }
    @Override <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitContinueStmt(this); }
    @Override GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLContinueStmt copy = new GLSLContinueStmt();
        copy.setLocation(line, column);
        return copy;
    }
    @Override void forEachChild(Consumer<GLSLASTNode> consumer) {}
    @Override int getChildCount() { return 0; }
    @Override GLSLASTNode getChild(int index) { return null; }
    @Override void setChild(int index, GLSLASTNode child) {}
}

/**
 * Discard statement (fragment shader).
 */
final class GLSLDiscardStmt extends GLSLStatement {
    GLSLDiscardStmt() {}
    
    @Override void accept(GLSLASTVisitor visitor) { visitor.visitDiscardStmt(this); }
    @Override <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitDiscardStmt(this); }
    @Override GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLDiscardStmt copy = new GLSLDiscardStmt();
        copy.setLocation(line, column);
        return copy;
    }
    @Override void forEachChild(Consumer<GLSLASTNode> consumer) {}
    @Override int getChildCount() { return 0; }
    @Override GLSLASTNode getChild(int index) { return null; }
    @Override void setChild(int index, GLSLASTNode child) {}
}

// ============================================================================
// EXPRESSION NODES
// ============================================================================

/**
 * Base class for all expressions.
 */
abstract class GLSLExpression extends GLSLASTNode {
    
    GLSLType resolvedType; // Set during semantic analysis
    boolean isLValue;
    boolean isConstant;
    
    GLSLExpression() {}
}

/**
 * Literal expression (int, float, bool).
 */
final class GLSLLiteralExpr extends GLSLExpression {
    
    enum LiteralType { INT, UINT, FLOAT, DOUBLE, BOOL }
    
    LiteralType literalType;
    String rawValue;
    
    // Cached parsed values
    long intValue;
    double floatValue;
    boolean boolValue;
    
    GLSLLiteralExpr() {}
    
    GLSLLiteralExpr(long value) {
        this.literalType = LiteralType.INT;
        this.intValue = value;
        this.rawValue = String.valueOf(value);
        this.isConstant = true;
    }
    
    GLSLLiteralExpr(double value) {
        this.literalType = LiteralType.FLOAT;
        this.floatValue = value;
        this.rawValue = String.valueOf(value);
        this.isConstant = true;
    }
    
    GLSLLiteralExpr(boolean value) {
        this.literalType = LiteralType.BOOL;
        this.boolValue = value;
        this.rawValue = String.valueOf(value);
        this.isConstant = true;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitLiteralExpr(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitLiteralExpr(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLLiteralExpr copy = new GLSLLiteralExpr();
        copy.literalType = literalType;
        copy.rawValue = rawValue;
        copy.intValue = intValue;
        copy.floatValue = floatValue;
        copy.boolValue = boolValue;
        copy.resolvedType = resolvedType;
        copy.isConstant = true;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override void forEachChild(Consumer<GLSLASTNode> consumer) {}
    @Override int getChildCount() { return 0; }
    @Override GLSLASTNode getChild(int index) { return null; }
    @Override void setChild(int index, GLSLASTNode child) {}
}

/**
 * Identifier expression.
 */
final class GLSLIdentifierExpr extends GLSLExpression {
    
    String name;
    GLSLSymbol resolvedSymbol;
    
    GLSLIdentifierExpr() {}
    
    GLSLIdentifierExpr(String name) {
        this.name = name;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitIdentifierExpr(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitIdentifierExpr(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLIdentifierExpr copy = new GLSLIdentifierExpr(name);
        copy.resolvedType = resolvedType;
        copy.resolvedSymbol = resolvedSymbol;
        copy.isLValue = isLValue;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override void forEachChild(Consumer<GLSLASTNode> consumer) {}
    @Override int getChildCount() { return 0; }
    @Override GLSLASTNode getChild(int index) { return null; }
    @Override void setChild(int index, GLSLASTNode child) {}
}

/**
 * Binary expression.
 */
final class GLSLBinaryExpr extends GLSLExpression {
    
    enum Operator {
        ADD("+"), SUB("-"), MUL("*"), DIV("/"), MOD("%"),
        EQ("=="), NE("!="), LT("<"), GT(">"), LE("<="), GE(">="),
        AND("&&"), OR("||"), XOR("^^"),
        BIT_AND("&"), BIT_OR("|"), BIT_XOR("^"),
        LEFT_SHIFT("<<"), RIGHT_SHIFT(">>"),
        ASSIGN("="), ADD_ASSIGN("+="), SUB_ASSIGN("-="),
        MUL_ASSIGN("*="), DIV_ASSIGN("/="), MOD_ASSIGN("%="),
        LEFT_SHIFT_ASSIGN("<<="), RIGHT_SHIFT_ASSIGN(">>="),
        AND_ASSIGN("&="), OR_ASSIGN("|="), XOR_ASSIGN("^="),
        COMMA(",");
        
        public final String symbol;
        Operator(String symbol) { this.symbol = symbol; }
        
        public boolean isAssignment() {
            return this == ASSIGN || this.name().endsWith("_ASSIGN");
        }
        
        public boolean isComparison() {
            return this == EQ || this == NE || this == LT || 
                   this == GT || this == LE || this == GE;
        }
        
        public boolean isLogical() {
            return this == AND || this == OR || this == XOR;
        }
    }
    
    Operator operator;
    GLSLExpression left;
    GLSLExpression right;
    
    GLSLBinaryExpr() {}
    
    GLSLBinaryExpr(Operator op, GLSLExpression left, GLSLExpression right) {
        this.operator = op;
        this.left = left;
        this.right = right;
        left.parent = this;
        right.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitBinaryExpr(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitBinaryExpr(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLBinaryExpr copy = new GLSLBinaryExpr();
        copy.operator = operator;
        copy.left = (GLSLExpression) left.copy(pool);
        copy.left.parent = copy;
        copy.right = (GLSLExpression) right.copy(pool);
        copy.right.parent = copy;
        copy.resolvedType = resolvedType;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        consumer.accept(left);
        consumer.accept(right);
    }
    
    @Override int getChildCount() { return 2; }
    @Override GLSLASTNode getChild(int index) {
        return index == 0 ? left : (index == 1 ? right : null);
    }
    @Override void setChild(int index, GLSLASTNode child) {
        if (index == 0) left = (GLSLExpression) child;
        else if (index == 1) right = (GLSLExpression) child;
        child.parent = this;
    }
}

/**
 * Unary expression.
 */
final class GLSLUnaryExpr extends GLSLExpression {
    
    enum Operator {
        PLUS("+"), MINUS("-"), NOT("!"), BIT_NOT("~"),
        PRE_INCREMENT("++"), PRE_DECREMENT("--"),
        POST_INCREMENT("++"), POST_DECREMENT("--");
        
        public final String symbol;
        Operator(String symbol) { this.symbol = symbol; }
        
        public boolean isPrefix() {
            return this != POST_INCREMENT && this != POST_DECREMENT;
        }
    }
    
    Operator operator;
    GLSLExpression operand;
    
    GLSLUnaryExpr() {}
    
    GLSLUnaryExpr(Operator op, GLSLExpression operand) {
        this.operator = op;
        this.operand = operand;
        operand.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitUnaryExpr(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitUnaryExpr(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLUnaryExpr copy = new GLSLUnaryExpr();
        copy.operator = operator;
        copy.operand = (GLSLExpression) operand.copy(pool);
        copy.operand.parent = copy;
        copy.resolvedType = resolvedType;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) { consumer.accept(operand); }
    @Override int getChildCount() { return 1; }
    @Override GLSLASTNode getChild(int index) { return index == 0 ? operand : null; }
    @Override void setChild(int index, GLSLASTNode child) {
        if (index == 0) { operand = (GLSLExpression) child; child.parent = this; }
    }
}

/**
 * Ternary conditional expression.
 */
final class GLSLTernaryExpr extends GLSLExpression {
    
    GLSLExpression condition;
    GLSLExpression thenExpr;
    GLSLExpression elseExpr;
    
    GLSLTernaryExpr() {}
    
    GLSLTernaryExpr(GLSLExpression condition, GLSLExpression thenExpr, GLSLExpression elseExpr) {
        this.condition = condition;
        this.thenExpr = thenExpr;
        this.elseExpr = elseExpr;
        condition.parent = this;
        thenExpr.parent = this;
        elseExpr.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitTernaryExpr(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitTernaryExpr(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLTernaryExpr copy = new GLSLTernaryExpr();
        copy.condition = (GLSLExpression) condition.copy(pool);
        copy.condition.parent = copy;
        copy.thenExpr = (GLSLExpression) thenExpr.copy(pool);
        copy.thenExpr.parent = copy;
        copy.elseExpr = (GLSLExpression) elseExpr.copy(pool);
        copy.elseExpr.parent = copy;
        copy.resolvedType = resolvedType;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        consumer.accept(condition);
        consumer.accept(thenExpr);
        consumer.accept(elseExpr);
    }
    
    @Override int getChildCount() { return 3; }
    @Override GLSLASTNode getChild(int index) {
        switch (index) {
            case 0: return condition;
            case 1: return thenExpr;
            case 2: return elseExpr;
            default: return null;
        }
    }
    @Override void setChild(int index, GLSLASTNode child) {
        switch (index) {
            case 0: condition = (GLSLExpression) child; break;
            case 1: thenExpr = (GLSLExpression) child; break;
            case 2: elseExpr = (GLSLExpression) child; break;
        }
        child.parent = this;
    }
}

/**
 * Function call expression.
 */
final class GLSLCallExpr extends GLSLExpression {
    
    String functionName;
    final ArrayList<GLSLExpression> arguments = new ArrayList<>();
    
    // Semantic info
    GLSLFunctionDecl resolvedFunction;
    boolean isConstructor;
    boolean isBuiltinFunction;
    
    GLSLCallExpr() {}
    
    GLSLCallExpr(String name) {
        this.functionName = name;
    }
    
    void addArgument(GLSLExpression arg) {
        arguments.add(arg);
        arg.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitCallExpr(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitCallExpr(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLCallExpr copy = new GLSLCallExpr(functionName);
        for (GLSLExpression arg : arguments) {
            copy.addArgument((GLSLExpression) arg.copy(pool));
        }
        copy.resolvedType = resolvedType;
        copy.isConstructor = isConstructor;
        copy.isBuiltinFunction = isBuiltinFunction;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) { arguments.forEach(consumer); }
    @Override int getChildCount() { return arguments.size(); }
    @Override GLSLASTNode getChild(int index) { return arguments.get(index); }
    @Override void setChild(int index, GLSLASTNode child) {
        arguments.set(index, (GLSLExpression) child);
        child.parent = this;
    }
}

/**
 * Member access expression (dot operator).
 */
final class GLSLMemberExpr extends GLSLExpression {
    
    GLSLExpression object;
    String member;
    boolean isSwizzle;
    
    GLSLMemberExpr() {}
    
    GLSLMemberExpr(GLSLExpression object, String member) {
        this.object = object;
        this.member = member;
        object.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitMemberExpr(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitMemberExpr(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLMemberExpr copy = new GLSLMemberExpr();
        copy.object = (GLSLExpression) object.copy(pool);
        copy.object.parent = copy;
        copy.member = member;
        copy.isSwizzle = isSwizzle;
        copy.resolvedType = resolvedType;
        copy.isLValue = isLValue;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) { consumer.accept(object); }
    @Override int getChildCount() { return 1; }
    @Override GLSLASTNode getChild(int index) { return index == 0 ? object : null; }
    @Override void setChild(int index, GLSLASTNode child) {
        if (index == 0) { object = (GLSLExpression) child; child.parent = this; }
    }
}

/**
 * Array subscript expression.
 */
final class GLSLSubscriptExpr extends GLSLExpression {
    
    GLSLExpression array;
    GLSLExpression index;
    
    GLSLSubscriptExpr() {}
    
    GLSLSubscriptExpr(GLSLExpression array, GLSLExpression index) {
        this.array = array;
        this.index = index;
        array.parent = this;
        index.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitSubscriptExpr(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitSubscriptExpr(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLSubscriptExpr copy = new GLSLSubscriptExpr();
        copy.array = (GLSLExpression) array.copy(pool);
        copy.array.parent = copy;
        copy.index = (GLSLExpression) index.copy(pool);
        copy.index.parent = copy;
        copy.resolvedType = resolvedType;
        copy.isLValue = isLValue;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) {
        consumer.accept(array);
        consumer.accept(index);
    }
    @Override int getChildCount() { return 2; }
    @Override GLSLASTNode getChild(int index) {
        return index == 0 ? array : (index == 1 ? this.index : null);
    }
    @Override void setChild(int i, GLSLASTNode child) {
        if (i == 0) array = (GLSLExpression) child;
        else if (i == 1) index = (GLSLExpression) child;
        child.parent = this;
    }
}

/**
 * Initializer list expression (for arrays and structs).
 */
final class GLSLInitializerListExpr extends GLSLExpression {
    
    final ArrayList<GLSLExpression> elements = new ArrayList<>();
    
    GLSLInitializerListExpr() {}
    
    void addElement(GLSLExpression elem) {
        elements.add(elem);
        elem.parent = this;
    }
    
    @Override
    void accept(GLSLASTVisitor visitor) { visitor.visitInitializerListExpr(this); }
    
    @Override
    <T> T accept(GLSLASTVisitorWithResult<T> visitor) { return visitor.visitInitializerListExpr(this); }
    
    @Override
    GLSLASTNode copy(GLSLMemoryPool pool) {
        GLSLInitializerListExpr copy = new GLSLInitializerListExpr();
        for (GLSLExpression elem : elements) {
            copy.addElement((GLSLExpression) elem.copy(pool));
        }
        copy.resolvedType = resolvedType;
        copy.setLocation(line, column);
        return copy;
    }
    
    @Override
    void forEachChild(Consumer<GLSLASTNode> consumer) { elements.forEach(consumer); }
    @Override int getChildCount() { return elements.size(); }
    @Override GLSLASTNode getChild(int index) { return elements.get(index); }
    @Override void setChild(int index, GLSLASTNode child) {
        elements.set(index, (GLSLExpression) child);
        child.parent = this;
    }
}

// ============================================================================
// SYMBOL TABLE
// ============================================================================

/**
 * Symbol in the symbol table.
 */
final class GLSLSymbol {
    
    enum Kind {
        VARIABLE, FUNCTION, STRUCT, INTERFACE_BLOCK, PARAMETER
    }
    
    final String name;
    final Kind kind;
    final GLSLType type;
    final GLSLASTNode declaration;
    final int scopeLevel;
    
    // Usage tracking for dead code elimination
    int useCount;
    boolean isWritten;
    boolean isRead;
    
    GLSLSymbol(String name, Kind kind, GLSLType type, GLSLASTNode declaration, int scopeLevel) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.declaration = declaration;
        this.scopeLevel = scopeLevel;
    }
}

/**
 * Hierarchical symbol table with scope support.
 */
final class GLSLSymbolTable {
    
    private final GLSLSymbolTable parent;
    private final Map<String, GLSLSymbol> symbols = new HashMap<>();
    private final int scopeLevel;
    
    GLSLSymbolTable() {
        this.parent = null;
        this.scopeLevel = 0;
    }
    
    GLSLSymbolTable(GLSLSymbolTable parent) {
        this.parent = parent;
        this.scopeLevel = parent != null ? parent.scopeLevel + 1 : 0;
    }
    
    void define(GLSLSymbol symbol) {
        symbols.put(symbol.name, symbol);
    }
    
    GLSLSymbol lookup(String name) {
        GLSLSymbol symbol = symbols.get(name);
        if (symbol != null) return symbol;
        if (parent != null) return parent.lookup(name);
        return null;
    }
    
    GLSLSymbol lookupLocal(String name) {
        return symbols.get(name);
    }
    
    boolean isDefined(String name) {
        return lookup(name) != null;
    }
    
    boolean isDefinedLocally(String name) {
        return symbols.containsKey(name);
    }
    
    GLSLSymbolTable getParent() {
        return parent;
    }
    
    int getScopeLevel() {
        return scopeLevel;
    }
    
    Collection<GLSLSymbol> getSymbols() {
        return symbols.values();
    }
    
    void clear() {
        symbols.clear();
    }
}

// ============================================================================
// AST VISITOR INTERFACES
// ============================================================================

/**
 * Visitor pattern for AST traversal without return value.
 */
interface GLSLASTVisitor {
    
    // Declarations
    void visitShader(GLSLShaderAST node);
    void visitExtensionDecl(GLSLExtensionDecl node);
    void visitPrecisionDecl(GLSLPrecisionDecl node);
    void visitVariableDecl(GLSLVariableDecl node);
    void visitStructDecl(GLSLStructDecl node);
    void visitInterfaceBlock(GLSLInterfaceBlock node);
    void visitFunctionDecl(GLSLFunctionDecl node);
    void visitParameterDecl(GLSLParameterDecl node);
    
    // Statements
    void visitBlockStmt(GLSLBlockStmt node);
    void visitExpressionStmt(GLSLExpressionStmt node);
    void visitDeclarationStmt(GLSLDeclarationStmt node);
    void visitIfStmt(GLSLIfStmt node);
    void visitForStmt(GLSLForStmt node);
    void visitWhileStmt(GLSLWhileStmt node);
    void visitDoWhileStmt(GLSLDoWhileStmt node);
    void visitSwitchStmt(GLSLSwitchStmt node);
    void visitCaseLabel(GLSLCaseLabel node);
    void visitReturnStmt(GLSLReturnStmt node);
    void visitBreakStmt(GLSLBreakStmt node);
    void visitContinueStmt(GLSLContinueStmt node);
    void visitDiscardStmt(GLSLDiscardStmt node);
    
    // Expressions
    void visitLiteralExpr(GLSLLiteralExpr node);
    void visitIdentifierExpr(GLSLIdentifierExpr node);
    void visitBinaryExpr(GLSLBinaryExpr node);
    void visitUnaryExpr(GLSLUnaryExpr node);
    void visitTernaryExpr(GLSLTernaryExpr node);
    void visitCallExpr(GLSLCallExpr node);
    void visitMemberExpr(GLSLMemberExpr node);
    void visitSubscriptExpr(GLSLSubscriptExpr node);
    void visitInitializerListExpr(GLSLInitializerListExpr node);
}

/**
 * Visitor pattern for AST traversal with return value.
 */
interface GLSLASTVisitorWithResult<T> {
    
    T visitShader(GLSLShaderAST node);
    T visitExtensionDecl(GLSLExtensionDecl node);
    T visitPrecisionDecl(GLSLPrecisionDecl node);
    T visitVariableDecl(GLSLVariableDecl node);
    T visitStructDecl(GLSLStructDecl node);
    T visitInterfaceBlock(GLSLInterfaceBlock node);
    T visitFunctionDecl(GLSLFunctionDecl node);
    T visitParameterDecl(GLSLParameterDecl node);
    
    T visitBlockStmt(GLSLBlockStmt node);
    T visitExpressionStmt(GLSLExpressionStmt node);
    T visitDeclarationStmt(GLSLDeclarationStmt node);
    T visitIfStmt(GLSLIfStmt node);
    T visitForStmt(GLSLForStmt node);
    T visitWhileStmt(GLSLWhileStmt node);
    T visitDoWhileStmt(GLSLDoWhileStmt node);
    T visitSwitchStmt(GLSLSwitchStmt node);
    T visitCaseLabel(GLSLCaseLabel node);
    T visitReturnStmt(GLSLReturnStmt node);
    T visitBreakStmt(GLSLBreakStmt node);
    T visitContinueStmt(GLSLContinueStmt node);
    T visitDiscardStmt(GLSLDiscardStmt node);
    
    T visitLiteralExpr(GLSLLiteralExpr node);
    T visitIdentifierExpr(GLSLIdentifierExpr node);
    T visitBinaryExpr(GLSLBinaryExpr node);
    T visitUnaryExpr(GLSLUnaryExpr node);
    T visitTernaryExpr(GLSLTernaryExpr node);
    T visitCallExpr(GLSLCallExpr node);
    T visitMemberExpr(GLSLMemberExpr node);
    T visitSubscriptExpr(GLSLSubscriptExpr node);
    T visitInitializerListExpr(GLSLInitializerListExpr node);
}

/**
 * Base visitor with default traversal behavior.
 */
abstract class GLSLASTBaseVisitor implements GLSLASTVisitor {
    
    protected void visitChildren(GLSLASTNode node) {
        node.forEachChild(child -> child.accept(this));
    }
    
    @Override public void visitShader(GLSLShaderAST node) { visitChildren(node); }
    @Override public void visitExtensionDecl(GLSLExtensionDecl node) {}
    @Override public void visitPrecisionDecl(GLSLPrecisionDecl node) {}
    @Override public void visitVariableDecl(GLSLVariableDecl node) { visitChildren(node); }
    @Override public void visitStructDecl(GLSLStructDecl node) { visitChildren(node); }
    @Override public void visitInterfaceBlock(GLSLInterfaceBlock node) { visitChildren(node); }
    @Override public void visitFunctionDecl(GLSLFunctionDecl node) { visitChildren(node); }
    @Override public void visitParameterDecl(GLSLParameterDecl node) {}
    @Override public void visitBlockStmt(GLSLBlockStmt node) { visitChildren(node); }
    @Override public void visitExpressionStmt(GLSLExpressionStmt node) { visitChildren(node); }
    @Override public void visitDeclarationStmt(GLSLDeclarationStmt node) { visitChildren(node); }
    @Override public void visitIfStmt(GLSLIfStmt node) { visitChildren(node); }
    @Override public void visitForStmt(GLSLForStmt node) { visitChildren(node); }
    @Override public void visitWhileStmt(GLSLWhileStmt node) { visitChildren(node); }
    @Override public void visitDoWhileStmt(GLSLDoWhileStmt node) { visitChildren(node); }
    @Override public void visitSwitchStmt(GLSLSwitchStmt node) { visitChildren(node); }
    @Override public void visitCaseLabel(GLSLCaseLabel node) { visitChildren(node); }
    @Override public void visitReturnStmt(GLSLReturnStmt node) { visitChildren(node); }
    @Override public void visitBreakStmt(GLSLBreakStmt node) {}
    @Override public void visitContinueStmt(GLSLContinueStmt node) {}
    @Override public void visitDiscardStmt(GLSLDiscardStmt node) {}
    @Override public void visitLiteralExpr(GLSLLiteralExpr node) {}
    @Override public void visitIdentifierExpr(GLSLIdentifierExpr node) {}
    @Override public void visitBinaryExpr(GLSLBinaryExpr node) { visitChildren(node); }
    @Override public void visitUnaryExpr(GLSLUnaryExpr node) { visitChildren(node); }
    @Override public void visitTernaryExpr(GLSLTernaryExpr node) { visitChildren(node); }
    @Override public void visitCallExpr(GLSLCallExpr node) { visitChildren(node); }
    @Override public void visitMemberExpr(GLSLMemberExpr node) { visitChildren(node); }
    @Override public void visitSubscriptExpr(GLSLSubscriptExpr node) { visitChildren(node); }
    @Override public void visitInitializerListExpr(GLSLInitializerListExpr node) { visitChildren(node); }
}

/**
 * Transforming visitor that can modify nodes.
 */
abstract class GLSLASTTransformer implements GLSLASTVisitorWithResult<GLSLASTNode> {
    
    protected final GLSLMemoryPool pool;
    
    GLSLASTTransformer(GLSLMemoryPool pool) {
        this.pool = pool;
    }
    
    protected GLSLASTNode transformChildren(GLSLASTNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            GLSLASTNode child = node.getChild(i);
            if (child != null) {
                GLSLASTNode transformed = child.accept(this);
                if (transformed != child) {
                    node.setChild(i, transformed);
                }
            }
        }
        return node;
    }
    
    @Override public GLSLASTNode visitShader(GLSLShaderAST node) { return transformChildren(node); }
    @Override public GLSLASTNode visitExtensionDecl(GLSLExtensionDecl node) { return node; }
    @Override public GLSLASTNode visitPrecisionDecl(GLSLPrecisionDecl node) { return node; }
    @Override public GLSLASTNode visitVariableDecl(GLSLVariableDecl node) { return transformChildren(node); }
    @Override public GLSLASTNode visitStructDecl(GLSLStructDecl node) { return transformChildren(node); }
    @Override public GLSLASTNode visitInterfaceBlock(GLSLInterfaceBlock node) { return transformChildren(node); }
    @Override public GLSLASTNode visitFunctionDecl(GLSLFunctionDecl node) { return transformChildren(node); }
    @Override public GLSLASTNode visitParameterDecl(GLSLParameterDecl node) { return node; }
    @Override public GLSLASTNode visitBlockStmt(GLSLBlockStmt node) { return transformChildren(node); }
    @Override public GLSLASTNode visitExpressionStmt(GLSLExpressionStmt node) { return transformChildren(node); }
    @Override public GLSLASTNode visitDeclarationStmt(GLSLDeclarationStmt node) { return transformChildren(node); }
    @Override public GLSLASTNode visitIfStmt(GLSLIfStmt node) { return transformChildren(node); }
    @Override public GLSLASTNode visitForStmt(GLSLForStmt node) { return transformChildren(node); }
    @Override public GLSLASTNode visitWhileStmt(GLSLWhileStmt node) { return transformChildren(node); }
    @Override public GLSLASTNode visitDoWhileStmt(GLSLDoWhileStmt node) { return transformChildren(node); }
    @Override public GLSLASTNode visitSwitchStmt(GLSLSwitchStmt node) { return transformChildren(node); }
    @Override public GLSLASTNode visitCaseLabel(GLSLCaseLabel node) { return transformChildren(node); }
    @Override public GLSLASTNode visitReturnStmt(GLSLReturnStmt node) { return transformChildren(node); }
    @Override public GLSLASTNode visitBreakStmt(GLSLBreakStmt node) { return node; }
    @Override public GLSLASTNode visitContinueStmt(GLSLContinueStmt node) { return node; }
    @Override public GLSLASTNode visitDiscardStmt(GLSLDiscardStmt node) { return node; }
    @Override public GLSLASTNode visitLiteralExpr(GLSLLiteralExpr node) { return node; }
    @Override public GLSLASTNode visitIdentifierExpr(GLSLIdentifierExpr node) { return node; }
    @Override public GLSLASTNode visitBinaryExpr(GLSLBinaryExpr node) { return transformChildren(node); }
    @Override public GLSLASTNode visitUnaryExpr(GLSLUnaryExpr node) { return transformChildren(node); }
    @Override public GLSLASTNode visitTernaryExpr(GLSLTernaryExpr node) { return transformChildren(node); }
    @Override public GLSLASTNode visitCallExpr(GLSLCallExpr node) { return transformChildren(node); }
    @Override public GLSLASTNode visitMemberExpr(GLSLMemberExpr node) { return transformChildren(node); }
    @Override public GLSLASTNode visitSubscriptExpr(GLSLSubscriptExpr node) { return transformChildren(node); }
    @Override public GLSLASTNode visitInitializerListExpr(GLSLInitializerListExpr node) { return transformChildren(node); }
}

// ============================================================================
// GLSL PARSER - High-Performance Recursive Descent with Precedence Climbing
// ============================================================================

/**
 * High-performance GLSL parser using recursive descent with Pratt-style
 * precedence climbing for expressions.
 * 
 * Supports all GLSL versions from 1.10 to 4.60.
 * Features:
 * - Zero-allocation hot paths via object pooling
 * - LL(2) lookahead for disambiguation
 * - Comprehensive error recovery
 * - Full source location tracking
 */
final class GLSLParser {
    
    private final GLSLMemoryPool pool;
    
    // Parser state
    private GLSLTokenStream tokens;
    private GLSLShaderType shaderType;
    private GLSLVersion detectedVersion;
    private GLSLSymbolTable symbolTable;
    private GLSLSymbolTable currentScope;
    
    // Error collection
    private final ArrayList<GLSLError> errors = new ArrayList<>();
    private final ArrayList<String> warnings = new ArrayList<>();
    
    // Known type names (for distinguishing type constructors from function calls)
    private final Set<String> knownTypes = new HashSet<>();
    private final Set<String> knownStructs = new HashSet<>();
    
    // Parsing context flags
    private boolean inLoop;
    private boolean inSwitch;
    private boolean inFunction;
    private int parenDepth;
    private int braceDepth;
    
    // Operator precedence table (Pratt parser style)
    private static final int[] PRECEDENCE = new int[GLSLTokenType.values().length];
    private static final boolean[] RIGHT_ASSOC = new boolean[GLSLTokenType.values().length];
    
    static {
        // Initialize precedence levels (higher = binds tighter)
        // Assignment operators (right associative)
        setPrecedence(GLSLTokenType.ASSIGN, 1, true);
        setPrecedence(GLSLTokenType.ADD_ASSIGN, 1, true);
        setPrecedence(GLSLTokenType.SUB_ASSIGN, 1, true);
        setPrecedence(GLSLTokenType.MUL_ASSIGN, 1, true);
        setPrecedence(GLSLTokenType.DIV_ASSIGN, 1, true);
        setPrecedence(GLSLTokenType.MOD_ASSIGN, 1, true);
        setPrecedence(GLSLTokenType.LEFT_SHIFT_ASSIGN, 1, true);
        setPrecedence(GLSLTokenType.RIGHT_SHIFT_ASSIGN, 1, true);
        setPrecedence(GLSLTokenType.AND_ASSIGN, 1, true);
        setPrecedence(GLSLTokenType.XOR_ASSIGN, 1, true);
        setPrecedence(GLSLTokenType.OR_ASSIGN, 1, true);
        
        // Ternary (right associative)
        setPrecedence(GLSLTokenType.QUESTION, 2, true);
        
        // Logical
        setPrecedence(GLSLTokenType.OR, 3, false);
        setPrecedence(GLSLTokenType.XOR, 4, false);
        setPrecedence(GLSLTokenType.AND, 5, false);
        
        // Bitwise
        setPrecedence(GLSLTokenType.PIPE, 6, false);
        setPrecedence(GLSLTokenType.CARET, 7, false);
        setPrecedence(GLSLTokenType.AMPERSAND, 8, false);
        
        // Equality
        setPrecedence(GLSLTokenType.EQ, 9, false);
        setPrecedence(GLSLTokenType.NE, 9, false);
        
        // Relational
        setPrecedence(GLSLTokenType.LT, 10, false);
        setPrecedence(GLSLTokenType.GT, 10, false);
        setPrecedence(GLSLTokenType.LE, 10, false);
        setPrecedence(GLSLTokenType.GE, 10, false);
        
        // Shift
        setPrecedence(GLSLTokenType.LEFT_SHIFT, 11, false);
        setPrecedence(GLSLTokenType.RIGHT_SHIFT, 11, false);
        
        // Additive
        setPrecedence(GLSLTokenType.PLUS, 12, false);
        setPrecedence(GLSLTokenType.MINUS, 12, false);
        
        // Multiplicative
        setPrecedence(GLSLTokenType.STAR, 13, false);
        setPrecedence(GLSLTokenType.SLASH, 13, false);
        setPrecedence(GLSLTokenType.PERCENT, 13, false);
    }
    
    private static void setPrecedence(GLSLTokenType type, int prec, boolean rightAssoc) {
        PRECEDENCE[type.ordinal()] = prec;
        RIGHT_ASSOC[type.ordinal()] = rightAssoc;
    }
    
    private static int getPrecedence(GLSLTokenType type) {
        return PRECEDENCE[type.ordinal()];
    }
    
    private static boolean isRightAssociative(GLSLTokenType type) {
        return RIGHT_ASSOC[type.ordinal()];
    }
    
    // ========================================================================
    // CONSTRUCTOR & INITIALIZATION
    // ========================================================================
    
    GLSLParser(GLSLMemoryPool pool) {
        this.pool = pool;
        initializeBuiltinTypes();
    }
    
    private void initializeBuiltinTypes() {
        // Scalar types
        knownTypes.add("void");
        knownTypes.add("bool");
        knownTypes.add("int");
        knownTypes.add("uint");
        knownTypes.add("float");
        knownTypes.add("double");
        
        // Vector types
        for (String prefix : new String[]{"", "b", "i", "u", "d"}) {
            for (int i = 2; i <= 4; i++) {
                knownTypes.add(prefix + "vec" + i);
            }
        }
        
        // Matrix types
        for (String prefix : new String[]{"", "d"}) {
            for (int c = 2; c <= 4; c++) {
                knownTypes.add(prefix + "mat" + c);
                for (int r = 2; r <= 4; r++) {
                    knownTypes.add(prefix + "mat" + c + "x" + r);
                }
            }
        }
        
        // Sampler types
        String[] samplerPrefixes = {"", "i", "u"};
        String[] samplerTypes = {
            "sampler1D", "sampler2D", "sampler3D", "samplerCube",
            "sampler1DArray", "sampler2DArray", "samplerCubeArray",
            "sampler2DRect", "samplerBuffer", "sampler2DMS", "sampler2DMSArray"
        };
        for (String prefix : samplerPrefixes) {
            for (String type : samplerTypes) {
                knownTypes.add(prefix + type);
            }
        }
        
        // Shadow samplers
        knownTypes.add("sampler1DShadow");
        knownTypes.add("sampler2DShadow");
        knownTypes.add("samplerCubeShadow");
        knownTypes.add("sampler1DArrayShadow");
        knownTypes.add("sampler2DArrayShadow");
        knownTypes.add("samplerCubeArrayShadow");
        knownTypes.add("sampler2DRectShadow");
        
        // Image types
        String[] imagePrefixes = {"", "i", "u"};
        String[] imageTypes = {
            "image1D", "image2D", "image3D", "imageCube",
            "image1DArray", "image2DArray", "imageCubeArray",
            "image2DRect", "imageBuffer", "image2DMS", "image2DMSArray"
        };
        for (String prefix : imagePrefixes) {
            for (String type : imageTypes) {
                knownTypes.add(prefix + type);
            }
        }
        
        // Atomic counter
        knownTypes.add("atomic_uint");
    }
    
    // ========================================================================
    // MAIN PARSE ENTRY POINT
    // ========================================================================
    
    GLSLShaderAST parse(GLSLTokenStream tokens, GLSLShaderType shaderType) {
        this.tokens = tokens;
        this.shaderType = shaderType;
        this.errors.clear();
        this.warnings.clear();
        this.knownStructs.clear();
        this.inLoop = false;
        this.inSwitch = false;
        this.inFunction = false;
        this.parenDepth = 0;
        this.braceDepth = 0;
        
        // Create global symbol table
        this.symbolTable = new GLSLSymbolTable();
        this.currentScope = symbolTable;
        
        GLSLShaderAST shader = new GLSLShaderAST();
        shader.shaderType = shaderType;
        shader.symbolTable = symbolTable;
        
        try {
            // Parse version directive (must be first non-comment)
            parseVersionDirective(shader);
            
            // Parse extension directives
            while (check(GLSLTokenType.PP_EXTENSION)) {
                GLSLExtensionDecl ext = parseExtensionDirective();
                if (ext != null) {
                    shader.addDeclaration(ext);
                }
            }
            
            // Parse top-level declarations
            while (!isAtEnd()) {
                GLSLASTNode decl = parseTopLevelDeclaration();
                if (decl != null) {
                    shader.addDeclaration(decl);
                }
            }
            
        } catch (GLSLParseException e) {
            // Collect remaining errors
            errors.addAll(e.getErrors());
        }
        
        // Copy warnings to shader
        shader.warnings.addAll(warnings);
        
        if (!errors.isEmpty()) {
            throw new GLSLParseException("Parse failed with " + errors.size() + " errors", errors);
        }
        
        return shader;
    }
    
    // ========================================================================
    // VERSION & EXTENSION DIRECTIVES
    // ========================================================================
    
    private void parseVersionDirective(GLSLShaderAST shader) {
        if (check(GLSLTokenType.PP_VERSION)) {
            GLSLToken token = advance();
            String directive = token.value;
            
            // Parse: #version 330 core
            // directive format: "#version NNN [profile]"
            String[] parts = directive.split("\\s+");
            if (parts.length >= 2) {
                try {
                    int versionNum = Integer.parseInt(parts[1]);
                    shader.version = GLSLVersion.fromNumber(versionNum);
                    detectedVersion = shader.version;
                    
                    if (parts.length >= 3) {
                        shader.profile = parts[2];
                    }
                } catch (NumberFormatException e) {
                    error(token, "Invalid version number: " + parts[1]);
                    shader.version = GLSLVersion.GLSL_110;
                }
            } else {
                shader.version = GLSLVersion.GLSL_110;
            }
        } else {
            // No version directive - default to 1.10
            shader.version = GLSLVersion.GLSL_110;
            detectedVersion = shader.version;
        }
    }
    
    private GLSLExtensionDecl parseExtensionDirective() {
        GLSLToken token = advance(); // consume PP_EXTENSION
        String directive = token.value;
        
        // Parse: #extension name : behavior
        // directive format: "#extension GL_ARB_xxx : enable"
        int colonIdx = directive.indexOf(':');
        if (colonIdx < 0) {
            error(token, "Invalid extension directive");
            return null;
        }
        
        String namePart = directive.substring(11, colonIdx).trim(); // skip "#extension "
        String behavior = directive.substring(colonIdx + 1).trim();
        
        GLSLExtensionDecl ext = new GLSLExtensionDecl(namePart, behavior);
        ext.setLocation(token.line, token.column);
        return ext;
    }
    
    // ========================================================================
    // TOP-LEVEL DECLARATIONS
    // ========================================================================
    
    private GLSLASTNode parseTopLevelDeclaration() {
        // Skip stray semicolons
        while (match(GLSLTokenType.SEMICOLON)) {}
        
        if (isAtEnd()) return null;
        
        GLSLToken start = peek();
        
        // Check for precision declaration
        if (check(GLSLTokenType.PRECISION)) {
            return parsePrecisionDeclaration();
        }
        
        // Parse qualifiers and type
        GLSLTypeQualifiers qualifiers = parseTypeQualifiers();
        
        // Check for interface block
        if (qualifiers != null && qualifiers.storage != GLSLTypeQualifiers.Storage.NONE) {
            if (check(GLSLTokenType.IDENTIFIER) && peek(1).type == GLSLTokenType.LEFT_BRACE) {
                return parseInterfaceBlock(qualifiers);
            }
        }
        
        // Check for struct
        if (check(GLSLTokenType.STRUCT)) {
            return parseStructDeclaration(qualifiers);
        }
        
        // Must be variable or function declaration
        GLSLType type = parseTypeSpecifier(qualifiers);
        if (type == null) {
            error(peek(), "Expected type specifier");
            synchronize();
            return null;
        }
        
        // Get name
        if (!check(GLSLTokenType.IDENTIFIER)) {
            error(peek(), "Expected identifier");
            synchronize();
            return null;
        }
        
        String name = advance().value;
        
        // Function or variable?
        if (check(GLSLTokenType.LEFT_PAREN)) {
            return parseFunctionDeclaration(type, name, start);
        } else {
            return parseVariableDeclaration(type, name, start);
        }
    }
    
    // ========================================================================
    // TYPE PARSING
    // ========================================================================
    
    private GLSLTypeQualifiers parseTypeQualifiers() {
        GLSLTypeQualifiers quals = null;
        
        while (true) {
            GLSLToken token = peek();
            
            switch (token.type) {
                // Layout qualifier
                case LAYOUT:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.layout = parseLayoutQualifier();
                    break;
                
                // Storage qualifiers
                case CONST:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.storage = GLSLTypeQualifiers.Storage.CONST;
                    advance();
                    break;
                case IN:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.storage = GLSLTypeQualifiers.Storage.IN;
                    advance();
                    break;
                case OUT:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.storage = GLSLTypeQualifiers.Storage.OUT;
                    advance();
                    break;
                case INOUT:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.storage = GLSLTypeQualifiers.Storage.INOUT;
                    advance();
                    break;
                case UNIFORM:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.storage = GLSLTypeQualifiers.Storage.UNIFORM;
                    advance();
                    break;
                case BUFFER:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.storage = GLSLTypeQualifiers.Storage.BUFFER;
                    advance();
                    break;
                case SHARED:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.storage = GLSLTypeQualifiers.Storage.SHARED;
                    advance();
                    break;
                case ATTRIBUTE:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.storage = GLSLTypeQualifiers.Storage.ATTRIBUTE;
                    advance();
                    break;
                case VARYING:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.storage = GLSLTypeQualifiers.Storage.VARYING;
                    advance();
                    break;
                
                // Interpolation qualifiers
                case FLAT:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.interpolation = GLSLTypeQualifiers.Interpolation.FLAT;
                    advance();
                    break;
                case SMOOTH:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.interpolation = GLSLTypeQualifiers.Interpolation.SMOOTH;
                    advance();
                    break;
                case NOPERSPECTIVE:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.interpolation = GLSLTypeQualifiers.Interpolation.NOPERSPECTIVE;
                    advance();
                    break;
                
                // Precision qualifiers
                case HIGHP:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.precision = GLSLTypeQualifiers.Precision.HIGHP;
                    advance();
                    break;
                case MEDIUMP:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.precision = GLSLTypeQualifiers.Precision.MEDIUMP;
                    advance();
                    break;
                case LOWP:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.precision = GLSLTypeQualifiers.Precision.LOWP;
                    advance();
                    break;
                
                // Auxiliary qualifiers
                case CENTROID:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.centroid = true;
                    advance();
                    break;
                case SAMPLE:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.sample = true;
                    advance();
                    break;
                case PATCH:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.patch = true;
                    advance();
                    break;
                case INVARIANT:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.invariant = true;
                    advance();
                    break;
                case PRECISE:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.precise = true;
                    advance();
                    break;
                
                // Memory qualifiers
                case COHERENT:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.coherent = true;
                    advance();
                    break;
                case VOLATILE:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.volatileQ = true;
                    advance();
                    break;
                case RESTRICT:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.restrict = true;
                    advance();
                    break;
                case READONLY:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.readonly = true;
                    advance();
                    break;
                case WRITEONLY:
                    if (quals == null) quals = new GLSLTypeQualifiers();
                    quals.writeonly = true;
                    advance();
                    break;
                
                default:
                    return quals;
            }
        }
    }
    
    private GLSLLayoutQualifier parseLayoutQualifier() {
        consume(GLSLTokenType.LAYOUT);
        consume(GLSLTokenType.LEFT_PAREN);
        
        GLSLLayoutQualifier layout = new GLSLLayoutQualifier();
        
        do {
            if (check(GLSLTokenType.IDENTIFIER)) {
                String id = advance().value;
                
                if (match(GLSLTokenType.ASSIGN)) {
                    // id = value
                    GLSLExpression valueExpr = parseConditionalExpression();
                    int value = evaluateConstantInt(valueExpr);
                    
                    switch (id) {
                        case "location": layout.location = value; break;
                        case "binding": layout.binding = value; break;
                        case "offset": layout.offset = value; break;
                        case "component": layout.component = value; break;
                        case "index": layout.index = value; break;
                        case "set": layout.set = value; break;
                        case "local_size_x": layout.localSizeX = value; break;
                        case "local_size_y": layout.localSizeY = value; break;
                        case "local_size_z": layout.localSizeZ = value; break;
                        case "max_vertices": layout.maxVertices = value; break;
                        case "vertices": layout.vertices = value; break;
                        case "invocations": layout.invocations = value; break;
                        default:
                            if (layout.otherParams == null) {
                                layout.otherParams = new HashMap<>();
                            }
                            layout.otherParams.put(id, String.valueOf(value));
                    }
                } else {
                    // Just an identifier
                    switch (id) {
                        case "std140": layout.packing = GLSLLayoutQualifier.PackingMode.STD140; break;
                        case "std430": layout.packing = GLSLLayoutQualifier.PackingMode.STD430; break;
                        case "shared": layout.packing = GLSLLayoutQualifier.PackingMode.SHARED; break;
                        case "packed": layout.packing = GLSLLayoutQualifier.PackingMode.PACKED; break;
                        case "row_major": layout.matrixLayout = GLSLLayoutQualifier.MatrixLayout.ROW_MAJOR; break;
                        case "column_major": layout.matrixLayout = GLSLLayoutQualifier.MatrixLayout.COLUMN_MAJOR; break;
                        case "origin_upper_left": layout.originUpperLeft = true; break;
                        case "pixel_center_integer": layout.pixelCenterInteger = true; break;
                        case "early_fragment_tests": layout.earlyFragmentTests = true; break;
                        case "depth_any": layout.depthAny = true; break;
                        case "depth_greater": layout.depthGreater = true; break;
                        case "depth_less": layout.depthLess = true; break;
                        case "depth_unchanged": layout.depthUnchanged = true; break;
                        
                        // Primitive types
                        case "points":
                        case "lines":
                        case "lines_adjacency":
                        case "triangles":
                        case "triangles_adjacency":
                        case "line_strip":
                        case "triangle_strip":
                        case "quads":
                        case "isolines":
                        case "equal_spacing":
                        case "fractional_even_spacing":
                        case "fractional_odd_spacing":
                        case "cw":
                        case "ccw":
                        case "point_mode":
                            layout.primitiveType = id;
                            break;
                        
                        // Image formats
                        case "rgba32f": case "rgba16f": case "rg32f": case "rg16f":
                        case "r11f_g11f_b10f": case "r32f": case "r16f":
                        case "rgba16": case "rgb10_a2": case "rgba8": case "rg16":
                        case "rg8": case "r16": case "r8": case "rgba16_snorm":
                        case "rgba8_snorm": case "rg16_snorm": case "rg8_snorm":
                        case "r16_snorm": case "r8_snorm":
                        case "rgba32i": case "rgba16i": case "rgba8i": case "rg32i":
                        case "rg16i": case "rg8i": case "r32i": case "r16i": case "r8i":
                        case "rgba32ui": case "rgba16ui": case "rgb10_a2ui":
                        case "rgba8ui": case "rg32ui": case "rg16ui": case "rg8ui":
                        case "r32ui": case "r16ui": case "r8ui":
                            layout.imageFormat = id;
                            break;
                        
                        default:
                            if (layout.otherParams == null) {
                                layout.otherParams = new HashMap<>();
                            }
                            layout.otherParams.put(id, "");
                    }
                }
            } else {
                error(peek(), "Expected layout qualifier identifier");
                break;
            }
        } while (match(GLSLTokenType.COMMA));
        
        consume(GLSLTokenType.RIGHT_PAREN);
        return layout;
    }
    
    private GLSLType parseTypeSpecifier(GLSLTypeQualifiers qualifiers) {
        GLSLToken token = peek();
        GLSLType.BaseType baseType = null;
        String structName = null;
        
        // Check for struct type reference
        if (check(GLSLTokenType.IDENTIFIER)) {
            String name = token.value;
            if (knownStructs.contains(name)) {
                advance();
                return new GLSLType(GLSLType.BaseType.STRUCT, name, null, qualifiers);
            }
        }
        
        // Check for built-in type
        if (token.type.isTypeKeyword()) {
            advance();
            baseType = GLSLType.fromTokenType(token.type);
        } else {
            return null;
        }
        
        GLSLType type = new GLSLType(baseType, structName, null, qualifiers);
        
        // Check for array specifier on type
        if (check(GLSLTokenType.LEFT_BRACKET)) {
            type = parseArraySpecifier(type);
        }
        
        return type;
    }
    
    private GLSLType parseArraySpecifier(GLSLType elementType) {
        ArrayList<Integer> dimensions = new ArrayList<>();
        
        while (match(GLSLTokenType.LEFT_BRACKET)) {
            if (check(GLSLTokenType.RIGHT_BRACKET)) {
                // Unsized array
                dimensions.add(-1);
            } else {
                GLSLExpression sizeExpr = parseConditionalExpression();
                int size = evaluateConstantInt(sizeExpr);
                dimensions.add(size);
            }
            consume(GLSLTokenType.RIGHT_BRACKET);
        }
        
        if (dimensions.isEmpty()) {
            return elementType;
        }
        
        int[] dims = new int[dimensions.size()];
        for (int i = 0; i < dims.length; i++) {
            dims[i] = dimensions.get(i);
        }
        
        return new GLSLType(elementType.baseType, elementType.structName, dims, elementType.qualifiers);
    }
    
    // ========================================================================
    // DECLARATION PARSING
    // ========================================================================
    
    private GLSLPrecisionDecl parsePrecisionDeclaration() {
        GLSLToken start = advance(); // consume 'precision'
        
        GLSLPrecisionDecl decl = new GLSLPrecisionDecl();
        decl.setLocation(start.line, start.column);
        
        // Parse precision qualifier
        if (check(GLSLTokenType.HIGHP)) {
            decl.precision = GLSLTypeQualifiers.Precision.HIGHP;
            advance();
        } else if (check(GLSLTokenType.MEDIUMP)) {
            decl.precision = GLSLTypeQualifiers.Precision.MEDIUMP;
            advance();
        } else if (check(GLSLTokenType.LOWP)) {
            decl.precision = GLSLTypeQualifiers.Precision.LOWP;
            advance();
        } else {
            error(peek(), "Expected precision qualifier");
        }
        
        // Parse type
        decl.type = parseTypeSpecifier(null);
        
        consume(GLSLTokenType.SEMICOLON);
        return decl;
    }
    
    private GLSLStructDecl parseStructDeclaration(GLSLTypeQualifiers qualifiers) {
        GLSLToken start = advance(); // consume 'struct'
        
        // Optional struct name
        String name = null;
        if (check(GLSLTokenType.IDENTIFIER)) {
            name = advance().value;
            knownStructs.add(name);
        }
        
        GLSLStructDecl decl = new GLSLStructDecl(name);
        decl.setLocation(start.line, start.column);
        
        consume(GLSLTokenType.LEFT_BRACE);
        
        // Parse members
        while (!check(GLSLTokenType.RIGHT_BRACE) && !isAtEnd()) {
            GLSLTypeQualifiers memberQuals = parseTypeQualifiers();
            GLSLType memberType = parseTypeSpecifier(memberQuals);
            
            if (memberType == null) {
                error(peek(), "Expected type in struct member");
                synchronize();
                continue;
            }
            
            // Parse member declarators
            do {
                if (!check(GLSLTokenType.IDENTIFIER)) {
                    error(peek(), "Expected member name");
                    break;
                }
                
                String memberName = advance().value;
                GLSLVariableDecl member = new GLSLVariableDecl(memberType, memberName);
                member.setLocation(peek().line, peek().column);
                
                // Check for array specifier on member
                if (check(GLSLTokenType.LEFT_BRACKET)) {
                    member.type = parseArraySpecifier(memberType);
                }
                
                decl.addMember(member);
                
            } while (match(GLSLTokenType.COMMA));
            
            consume(GLSLTokenType.SEMICOLON);
        }
        
        consume(GLSLTokenType.RIGHT_BRACE);
        
        // Check for instance declaration: struct S { ... } s;
        if (check(GLSLTokenType.IDENTIFIER)) {
            // This creates both a struct type and a variable
            // Return struct decl, variable is handled separately
            // For now, we consume the instance and ignore (simplified)
            advance(); // instance name
            if (check(GLSLTokenType.LEFT_BRACKET)) {
                parseArraySpecifier(null); // array
            }
        }
        
        consume(GLSLTokenType.SEMICOLON);
        
        // Register in symbol table
        if (name != null) {
            GLSLSymbol symbol = new GLSLSymbol(name, GLSLSymbol.Kind.STRUCT, 
                GLSLType.struct(name), decl, currentScope.getScopeLevel());
            currentScope.define(symbol);
            decl.symbol = symbol;
        }
        
        return decl;
    }
    
    private GLSLInterfaceBlock parseInterfaceBlock(GLSLTypeQualifiers qualifiers) {
        GLSLToken start = peek();
        String blockName = advance().value; // block type name
        
        GLSLInterfaceBlock block = new GLSLInterfaceBlock();
        block.qualifiers = qualifiers;
        block.blockName = blockName;
        block.setLocation(start.line, start.column);
        
        consume(GLSLTokenType.LEFT_BRACE);
        
        // Parse members
        while (!check(GLSLTokenType.RIGHT_BRACE) && !isAtEnd()) {
            GLSLTypeQualifiers memberQuals = parseTypeQualifiers();
            GLSLType memberType = parseTypeSpecifier(memberQuals);
            
            if (memberType == null) {
                error(peek(), "Expected type in interface block member");
                synchronize();
                continue;
            }
            
            // Parse member declarators
            do {
                if (!check(GLSLTokenType.IDENTIFIER)) {
                    error(peek(), "Expected member name");
                    break;
                }
                
                String memberName = advance().value;
                GLSLVariableDecl member = new GLSLVariableDecl(memberType, memberName);
                member.setLocation(peek().line, peek().column);
                
                // Check for array specifier
                if (check(GLSLTokenType.LEFT_BRACKET)) {
                    member.type = parseArraySpecifier(memberType);
                }
                
                block.addMember(member);
                
            } while (match(GLSLTokenType.COMMA));
            
            consume(GLSLTokenType.SEMICOLON);
        }
        
        consume(GLSLTokenType.RIGHT_BRACE);
        
        // Optional instance name
        if (check(GLSLTokenType.IDENTIFIER)) {
            block.instanceName = advance().value;
            
            // Optional array specifier
            if (check(GLSLTokenType.LEFT_BRACKET)) {
                ArrayList<Integer> dims = new ArrayList<>();
                while (match(GLSLTokenType.LEFT_BRACKET)) {
                    if (check(GLSLTokenType.RIGHT_BRACKET)) {
                        dims.add(-1);
                    } else {
                        GLSLExpression sizeExpr = parseConditionalExpression();
                        dims.add(evaluateConstantInt(sizeExpr));
                    }
                    consume(GLSLTokenType.RIGHT_BRACKET);
                }
                block.arrayDimensions = dims.stream().mapToInt(i -> i).toArray();
            }
        }
        
        consume(GLSLTokenType.SEMICOLON);
        return block;
    }
    
    private GLSLFunctionDecl parseFunctionDeclaration(GLSLType returnType, String name, GLSLToken start) {
        GLSLFunctionDecl func = new GLSLFunctionDecl(returnType, name);
        func.setLocation(start.line, start.column);
        
        consume(GLSLTokenType.LEFT_PAREN);
        
        // Parse parameters
        if (!check(GLSLTokenType.RIGHT_PAREN)) {
            // Check for void parameter
            if (check(GLSLTokenType.VOID)) {
                advance();
            } else {
                do {
                    GLSLParameterDecl param = parseParameterDeclaration();
                    if (param != null) {
                        func.addParameter(param);
                    }
                } while (match(GLSLTokenType.COMMA));
            }
        }
        
        consume(GLSLTokenType.RIGHT_PAREN);
        
        // Function body or prototype
        if (check(GLSLTokenType.SEMICOLON)) {
            // Prototype
            advance();
            func.isPrototype = true;
        } else if (check(GLSLTokenType.LEFT_BRACE)) {
            // Definition
            boolean prevInFunction = inFunction;
            inFunction = true;
            
            // Create new scope for function body
            GLSLSymbolTable prevScope = currentScope;
            currentScope = new GLSLSymbolTable(currentScope);
            
            // Add parameters to scope
            for (GLSLParameterDecl param : func.parameters) {
                if (param.name != null) {
                    GLSLSymbol paramSymbol = new GLSLSymbol(param.name, GLSLSymbol.Kind.PARAMETER,
                        param.type, param, currentScope.getScopeLevel());
                    currentScope.define(paramSymbol);
                }
            }
            
            func.setBody(parseBlockStatement());
            
            currentScope = prevScope;
            inFunction = prevInFunction;
        } else {
            error(peek(), "Expected ';' or function body");
        }
        
        // Register function in symbol table
        GLSLSymbol symbol = new GLSLSymbol(name, GLSLSymbol.Kind.FUNCTION,
            returnType, func, currentScope.getScopeLevel());
        currentScope.define(symbol);
        func.symbol = symbol;
        
        return func;
    }
    
    private GLSLParameterDecl parseParameterDeclaration() {
        GLSLToken start = peek();
        
        // Parse qualifiers (in, out, inout, const)
        GLSLTypeQualifiers.Storage storage = GLSLTypeQualifiers.Storage.IN;
        
        while (true) {
            if (match(GLSLTokenType.CONST)) {
                // const parameter
            } else if (match(GLSLTokenType.IN)) {
                storage = GLSLTypeQualifiers.Storage.IN;
            } else if (match(GLSLTokenType.OUT)) {
                storage = GLSLTypeQualifiers.Storage.OUT;
            } else if (match(GLSLTokenType.INOUT)) {
                storage = GLSLTypeQualifiers.Storage.INOUT;
            } else if (match(GLSLTokenType.HIGHP) || match(GLSLTokenType.MEDIUMP) || match(GLSLTokenType.LOWP)) {
                // precision qualifier
            } else {
                break;
            }
        }
        
        GLSLType type = parseTypeSpecifier(null);
        if (type == null) {
            error(peek(), "Expected parameter type");
            return null;
        }
        
        GLSLParameterDecl param = new GLSLParameterDecl();
        param.type = type;
        param.qualifier = storage;
        param.setLocation(start.line, start.column);
        
        // Optional parameter name
        if (check(GLSLTokenType.IDENTIFIER)) {
            param.name = advance().value;
            
            // Array specifier on parameter name
            if (check(GLSLTokenType.LEFT_BRACKET)) {
                param.type = parseArraySpecifier(type);
            }
        }
        
        return param;
    }
    
    private GLSLASTNode parseVariableDeclaration(GLSLType type, String firstName, GLSLToken start) {
        GLSLDeclarationStmt stmt = new GLSLDeclarationStmt();
        stmt.setLocation(start.line, start.column);
        
        // First variable
        GLSLVariableDecl firstVar = new GLSLVariableDecl(type, firstName);
        firstVar.setLocation(start.line, start.column);
        
        // Array specifier on variable
        if (check(GLSLTokenType.LEFT_BRACKET)) {
            firstVar.type = parseArraySpecifier(type);
        }
        
        // Initializer
        if (match(GLSLTokenType.ASSIGN)) {
            firstVar.initializer = parseInitializer();
            if (firstVar.initializer != null) {
                firstVar.initializer.parent = firstVar;
            }
        }
        
        stmt.addDeclaration(firstVar);
        registerVariable(firstVar);
        
        // Additional declarators
        while (match(GLSLTokenType.COMMA)) {
            if (!check(GLSLTokenType.IDENTIFIER)) {
                error(peek(), "Expected variable name");
                break;
            }
            
            String name = advance().value;
            GLSLVariableDecl var = new GLSLVariableDecl(type, name);
            var.setLocation(peek().line, peek().column);
            
            // Array specifier
            if (check(GLSLTokenType.LEFT_BRACKET)) {
                var.type = parseArraySpecifier(type);
            }
            
            // Initializer
            if (match(GLSLTokenType.ASSIGN)) {
                var.initializer = parseInitializer();
                if (var.initializer != null) {
                    var.initializer.parent = var;
                }
            }
            
            stmt.addDeclaration(var);
            registerVariable(var);
        }
        
        consume(GLSLTokenType.SEMICOLON);
        
        // If it's a single declaration at global scope, return just the variable decl
        if (stmt.declarations.size() == 1 && currentScope == symbolTable) {
            return stmt.declarations.get(0);
        }
        
        return stmt;
    }
    
    private void registerVariable(GLSLVariableDecl var) {
        GLSLSymbol symbol = new GLSLSymbol(var.name, GLSLSymbol.Kind.VARIABLE,
            var.type, var, currentScope.getScopeLevel());
        currentScope.define(symbol);
        var.symbol = symbol;
    }
    
    private GLSLExpression parseInitializer() {
        if (check(GLSLTokenType.LEFT_BRACE)) {
            return parseInitializerList();
        }
        return parseAssignmentExpression();
    }
    
    private GLSLInitializerListExpr parseInitializerList() {
        GLSLToken start = advance(); // consume '{'
        
        GLSLInitializerListExpr list = new GLSLInitializerListExpr();
        list.setLocation(start.line, start.column);
        
        if (!check(GLSLTokenType.RIGHT_BRACE)) {
            do {
                GLSLExpression elem = parseInitializer();
                if (elem != null) {
                    list.addElement(elem);
                }
            } while (match(GLSLTokenType.COMMA) && !check(GLSLTokenType.RIGHT_BRACE));
        }
        
        consume(GLSLTokenType.RIGHT_BRACE);
        return list;
    }
    
    // ========================================================================
    // STATEMENT PARSING
    // ========================================================================
    
    private GLSLStatement parseStatement() {
        GLSLToken token = peek();
        
        switch (token.type) {
            case LEFT_BRACE:
                return parseBlockStatement();
            case IF:
                return parseIfStatement();
            case FOR:
                return parseForStatement();
            case WHILE:
                return parseWhileStatement();
            case DO:
                return parseDoWhileStatement();
            case SWITCH:
                return parseSwitchStatement();
            case CASE:
            case DEFAULT:
                return parseCaseLabel();
            case RETURN:
                return parseReturnStatement();
            case BREAK:
                return parseBreakStatement();
            case CONTINUE:
                return parseContinueStatement();
            case DISCARD:
                return parseDiscardStatement();
            case SEMICOLON:
                advance();
                return new GLSLExpressionStmt(null);
            default:
                // Could be declaration or expression statement
                return parseDeclarationOrExpressionStatement();
        }
    }
    
    private GLSLBlockStmt parseBlockStatement() {
        GLSLToken start = advance(); // consume '{'
        braceDepth++;
        
        GLSLBlockStmt block = new GLSLBlockStmt();
        block.setLocation(start.line, start.column);
        
        // Create new scope
        GLSLSymbolTable prevScope = currentScope;
        currentScope = new GLSLSymbolTable(currentScope);
        block.localSymbols = currentScope;
        
        while (!check(GLSLTokenType.RIGHT_BRACE) && !isAtEnd()) {
            GLSLStatement stmt = parseStatement();
            if (stmt != null) {
                block.addStatement(stmt);
            }
        }
        
        consume(GLSLTokenType.RIGHT_BRACE);
        braceDepth--;
        
        currentScope = prevScope;
        return block;
    }
    
    private GLSLIfStmt parseIfStatement() {
        GLSLToken start = advance(); // consume 'if'
        
        consume(GLSLTokenType.LEFT_PAREN);
        GLSLExpression condition = parseExpression();
        consume(GLSLTokenType.RIGHT_PAREN);
        
        GLSLStatement thenBranch = parseStatement();
        GLSLStatement elseBranch = null;
        
        if (match(GLSLTokenType.ELSE)) {
            elseBranch = parseStatement();
        }
        
        GLSLIfStmt stmt = new GLSLIfStmt(condition, thenBranch, elseBranch);
        stmt.setLocation(start.line, start.column);
        return stmt;
    }
    
    private GLSLForStmt parseForStatement() {
        GLSLToken start = advance(); // consume 'for'
        
        consume(GLSLTokenType.LEFT_PAREN);
        
        // Create scope for loop variable
        GLSLSymbolTable prevScope = currentScope;
        currentScope = new GLSLSymbolTable(currentScope);
        
        GLSLForStmt stmt = new GLSLForStmt();
        stmt.setLocation(start.line, start.column);
        
        // Init
        if (!check(GLSLTokenType.SEMICOLON)) {
            stmt.init = parseDeclarationOrExpressionStatement();
        } else {
            advance(); // consume ';'
        }
        
        // Condition
        if (!check(GLSLTokenType.SEMICOLON)) {
            stmt.condition = parseExpression();
        }
        consume(GLSLTokenType.SEMICOLON);
        
        // Update
        if (!check(GLSLTokenType.RIGHT_PAREN)) {
            stmt.update = parseExpression();
        }
        
        consume(GLSLTokenType.RIGHT_PAREN);
        
        // Body
        boolean prevInLoop = inLoop;
        inLoop = true;
        stmt.body = parseStatement();
        inLoop = prevInLoop;
        
        if (stmt.init != null) stmt.init.parent = stmt;
        if (stmt.condition != null) stmt.condition.parent = stmt;
        if (stmt.update != null) stmt.update.parent = stmt;
        stmt.body.parent = stmt;
        
        currentScope = prevScope;
        return stmt;
    }
    
    private GLSLWhileStmt parseWhileStatement() {
        GLSLToken start = advance(); // consume 'while'
        
        consume(GLSLTokenType.LEFT_PAREN);
        GLSLExpression condition = parseExpression();
        consume(GLSLTokenType.RIGHT_PAREN);
        
        boolean prevInLoop = inLoop;
        inLoop = true;
        GLSLStatement body = parseStatement();
        inLoop = prevInLoop;
        
        GLSLWhileStmt stmt = new GLSLWhileStmt();
        stmt.condition = condition;
        stmt.body = body;
        condition.parent = stmt;
        body.parent = stmt;
        stmt.setLocation(start.line, start.column);
        return stmt;
    }
    
    private GLSLDoWhileStmt parseDoWhileStatement() {
        GLSLToken start = advance(); // consume 'do'
        
        boolean prevInLoop = inLoop;
        inLoop = true;
        GLSLStatement body = parseStatement();
        inLoop = prevInLoop;
        
        consume(GLSLTokenType.WHILE);
        consume(GLSLTokenType.LEFT_PAREN);
        GLSLExpression condition = parseExpression();
        consume(GLSLTokenType.RIGHT_PAREN);
        consume(GLSLTokenType.SEMICOLON);
        
        GLSLDoWhileStmt stmt = new GLSLDoWhileStmt();
        stmt.body = body;
        stmt.condition = condition;
        body.parent = stmt;
        condition.parent = stmt;
        stmt.setLocation(start.line, start.column);
        return stmt;
    }
    
    private GLSLSwitchStmt parseSwitchStatement() {
        GLSLToken start = advance(); // consume 'switch'
        
        consume(GLSLTokenType.LEFT_PAREN);
        GLSLExpression expression = parseExpression();
        consume(GLSLTokenType.RIGHT_PAREN);
        
        consume(GLSLTokenType.LEFT_BRACE);
        
        GLSLSwitchStmt stmt = new GLSLSwitchStmt();
        stmt.expression = expression;
        expression.parent = stmt;
        stmt.setLocation(start.line, start.column);
        
        boolean prevInSwitch = inSwitch;
        inSwitch = true;
        
        while (!check(GLSLTokenType.RIGHT_BRACE) && !isAtEnd()) {
            if (check(GLSLTokenType.CASE) || check(GLSLTokenType.DEFAULT)) {
                GLSLCaseLabel label = parseCaseLabel();
                stmt.cases.add(label);
                label.parent = stmt;
            } else {
                GLSLStatement s = parseStatement();
                if (s != null) {
                    stmt.statements.add(s);
                    s.parent = stmt;
                }
            }
        }
        
        inSwitch = prevInSwitch;
        
        consume(GLSLTokenType.RIGHT_BRACE);
        return stmt;
    }
    
    private GLSLCaseLabel parseCaseLabel() {
        GLSLToken start = peek();
        GLSLCaseLabel label = new GLSLCaseLabel();
        label.setLocation(start.line, start.column);
        
        if (match(GLSLTokenType.CASE)) {
            label.value = parseExpression();
            label.value.parent = label;
        } else {
            advance(); // consume 'default'
            label.isDefault = true;
        }
        
        consume(GLSLTokenType.COLON);
        return label;
    }
    
    private GLSLReturnStmt parseReturnStatement() {
        GLSLToken start = advance(); // consume 'return'
        
        GLSLReturnStmt stmt = new GLSLReturnStmt();
        stmt.setLocation(start.line, start.column);
        
        if (!check(GLSLTokenType.SEMICOLON)) {
            stmt.value = parseExpression();
            stmt.value.parent = stmt;
        }
        
        consume(GLSLTokenType.SEMICOLON);
        return stmt;
    }
    
    private GLSLBreakStmt parseBreakStatement() {
        GLSLToken start = advance(); // consume 'break'
        
        if (!inLoop && !inSwitch) {
            warning(start, "'break' outside of loop or switch");
        }
        
        consume(GLSLTokenType.SEMICOLON);
        
        GLSLBreakStmt stmt = new GLSLBreakStmt();
        stmt.setLocation(start.line, start.column);
        return stmt;
    }
    
    private GLSLContinueStmt parseContinueStatement() {
        GLSLToken start = advance(); // consume 'continue'
        
        if (!inLoop) {
            warning(start, "'continue' outside of loop");
        }
        
        consume(GLSLTokenType.SEMICOLON);
        
        GLSLContinueStmt stmt = new GLSLContinueStmt();
        stmt.setLocation(start.line, start.column);
        return stmt;
    }
    
    private GLSLDiscardStmt parseDiscardStatement() {
        GLSLToken start = advance(); // consume 'discard'
        
        if (shaderType != GLSLShaderType.FRAGMENT) {
            warning(start, "'discard' only valid in fragment shaders");
        }
        
        consume(GLSLTokenType.SEMICOLON);
        
        GLSLDiscardStmt stmt = new GLSLDiscardStmt();
        stmt.setLocation(start.line, start.column);
        return stmt;
    }
    
    private GLSLStatement parseDeclarationOrExpressionStatement() {
        // Use lookahead to determine if this is a declaration
        if (isDeclarationStart()) {
            return parseLocalDeclaration();
        } else {
            return parseExpressionStatement();
        }
    }
    
    private boolean isDeclarationStart() {
        GLSLToken token = peek();
        
        // Type qualifiers indicate declaration
        switch (token.type) {
            case CONST:
            case HIGHP:
            case MEDIUMP:
            case LOWP:
                return true;
            default:
                break;
        }
        
        // Built-in type keywords
        if (token.type.isTypeKeyword()) {
            return true;
        }
        
        // Struct type name
        if (token.type == GLSLTokenType.IDENTIFIER && knownStructs.contains(token.value)) {
            // Check next token is identifier (variable name)
            GLSLToken next = peek(1);
            return next.type == GLSLTokenType.IDENTIFIER;
        }
        
        return false;
    }
    
    private GLSLStatement parseLocalDeclaration() {
        GLSLToken start = peek();
        
        GLSLTypeQualifiers qualifiers = parseTypeQualifiers();
        GLSLType type = parseTypeSpecifier(qualifiers);
        
        if (type == null) {
            error(peek(), "Expected type");
            return parseExpressionStatement();
        }
        
        if (!check(GLSLTokenType.IDENTIFIER)) {
            error(peek(), "Expected variable name");
            return parseExpressionStatement();
        }
        
        String name = advance().value;
        
        GLSLDeclarationStmt stmt = new GLSLDeclarationStmt();
        stmt.setLocation(start.line, start.column);
        
        // First variable
        GLSLVariableDecl var = new GLSLVariableDecl(type, name);
        var.setLocation(start.line, start.column);
        
        // Array specifier
        if (check(GLSLTokenType.LEFT_BRACKET)) {
            var.type = parseArraySpecifier(type);
        }
        
        // Initializer
        if (match(GLSLTokenType.ASSIGN)) {
            var.initializer = parseInitializer();
            if (var.initializer != null) {
                var.initializer.parent = var;
            }
        }
        
        stmt.addDeclaration(var);
        registerVariable(var);
        
        // Additional declarators
        while (match(GLSLTokenType.COMMA)) {
            if (!check(GLSLTokenType.IDENTIFIER)) {
                error(peek(), "Expected variable name");
                break;
            }
            
            String nextName = advance().value;
            GLSLVariableDecl nextVar = new GLSLVariableDecl(type, nextName);
            nextVar.setLocation(peek().line, peek().column);
            
            if (check(GLSLTokenType.LEFT_BRACKET)) {
                nextVar.type = parseArraySpecifier(type);
            }
            
            if (match(GLSLTokenType.ASSIGN)) {
                nextVar.initializer = parseInitializer();
                if (nextVar.initializer != null) {
                    nextVar.initializer.parent = nextVar;
                }
            }
            
            stmt.addDeclaration(nextVar);
            registerVariable(nextVar);
        }
        
        consume(GLSLTokenType.SEMICOLON);
        return stmt;
    }
    
    private GLSLExpressionStmt parseExpressionStatement() {
        GLSLToken start = peek();
        GLSLExpression expr = parseExpression();
        consume(GLSLTokenType.SEMICOLON);
        
        GLSLExpressionStmt stmt = new GLSLExpressionStmt(expr);
        stmt.setLocation(start.line, start.column);
        return stmt;
    }
    
    // ========================================================================
    // EXPRESSION PARSING (Pratt Parser / Precedence Climbing)
    // ========================================================================
    
    private GLSLExpression parseExpression() {
        return parseCommaExpression();
    }
    
    private GLSLExpression parseCommaExpression() {
        GLSLExpression left = parseAssignmentExpression();
        
        while (match(GLSLTokenType.COMMA)) {
            GLSLExpression right = parseAssignmentExpression();
            left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.COMMA, left, right);
        }
        
        return left;
    }
    
    private GLSLExpression parseAssignmentExpression() {
        GLSLExpression left = parseConditionalExpression();
        
        GLSLBinaryExpr.Operator op = getAssignmentOperator(peek().type);
        if (op != null) {
            GLSLToken opToken = advance();
            GLSLExpression right = parseAssignmentExpression(); // Right associative
            left = new GLSLBinaryExpr(op, left, right);
            left.setLocation(opToken.line, opToken.column);
        }
        
        return left;
    }
    
    private GLSLBinaryExpr.Operator getAssignmentOperator(GLSLTokenType type) {
        switch (type) {
            case ASSIGN: return GLSLBinaryExpr.Operator.ASSIGN;
            case ADD_ASSIGN: return GLSLBinaryExpr.Operator.ADD_ASSIGN;
            case SUB_ASSIGN: return GLSLBinaryExpr.Operator.SUB_ASSIGN;
            case MUL_ASSIGN: return GLSLBinaryExpr.Operator.MUL_ASSIGN;
            case DIV_ASSIGN: return GLSLBinaryExpr.Operator.DIV_ASSIGN;
            case MOD_ASSIGN: return GLSLBinaryExpr.Operator.MOD_ASSIGN;
            case LEFT_SHIFT_ASSIGN: return GLSLBinaryExpr.Operator.LEFT_SHIFT_ASSIGN;
            case RIGHT_SHIFT_ASSIGN: return GLSLBinaryExpr.Operator.RIGHT_SHIFT_ASSIGN;
            case AND_ASSIGN: return GLSLBinaryExpr.Operator.AND_ASSIGN;
            case OR_ASSIGN: return GLSLBinaryExpr.Operator.OR_ASSIGN;
            case XOR_ASSIGN: return GLSLBinaryExpr.Operator.XOR_ASSIGN;
            default: return null;
        }
    }
    
    private GLSLExpression parseConditionalExpression() {
        GLSLExpression condition = parseLogicalOrExpression();
        
        if (match(GLSLTokenType.QUESTION)) {
            GLSLExpression thenExpr = parseExpression();
            consume(GLSLTokenType.COLON);
            GLSLExpression elseExpr = parseConditionalExpression(); // Right associative
            
            return new GLSLTernaryExpr(condition, thenExpr, elseExpr);
        }
        
        return condition;
    }
    
    private GLSLExpression parseLogicalOrExpression() {
        GLSLExpression left = parseLogicalXorExpression();
        
        while (match(GLSLTokenType.OR)) {
            GLSLExpression right = parseLogicalXorExpression();
            left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.OR, left, right);
        }
        
        return left;
    }
    
    private GLSLExpression parseLogicalXorExpression() {
        GLSLExpression left = parseLogicalAndExpression();
        
        while (match(GLSLTokenType.XOR)) {
            GLSLExpression right = parseLogicalAndExpression();
            left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.XOR, left, right);
        }
        
        return left;
    }
    
    private GLSLExpression parseLogicalAndExpression() {
        GLSLExpression left = parseBitwiseOrExpression();
        
        while (match(GLSLTokenType.AND)) {
            GLSLExpression right = parseBitwiseOrExpression();
            left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.AND, left, right);
        }
        
        return left;
    }
    
    private GLSLExpression parseBitwiseOrExpression() {
        GLSLExpression left = parseBitwiseXorExpression();
        
        while (match(GLSLTokenType.PIPE)) {
            GLSLExpression right = parseBitwiseXorExpression();
            left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.BIT_OR, left, right);
        }
        
        return left;
    }
    
    private GLSLExpression parseBitwiseXorExpression() {
        GLSLExpression left = parseBitwiseAndExpression();
        
        while (match(GLSLTokenType.CARET)) {
            GLSLExpression right = parseBitwiseAndExpression();
            left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.BIT_XOR, left, right);
        }
        
        return left;
    }
    
    private GLSLExpression parseBitwiseAndExpression() {
        GLSLExpression left = parseEqualityExpression();
        
        while (match(GLSLTokenType.AMPERSAND)) {
            GLSLExpression right = parseEqualityExpression();
            left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.BIT_AND, left, right);
        }
        
        return left;
    }
    
    private GLSLExpression parseEqualityExpression() {
        GLSLExpression left = parseRelationalExpression();
        
        while (true) {
            if (match(GLSLTokenType.EQ)) {
                GLSLExpression right = parseRelationalExpression();
                left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.EQ, left, right);
            } else if (match(GLSLTokenType.NE)) {
                GLSLExpression right = parseRelationalExpression();
                left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.NE, left, right);
            } else {
                break;
            }
        }
        
        return left;
    }
    
    private GLSLExpression parseRelationalExpression() {
        GLSLExpression left = parseShiftExpression();
        
        while (true) {
            GLSLBinaryExpr.Operator op = null;
            if (match(GLSLTokenType.LT)) op = GLSLBinaryExpr.Operator.LT;
            else if (match(GLSLTokenType.GT)) op = GLSLBinaryExpr.Operator.GT;
            else if (match(GLSLTokenType.LE)) op = GLSLBinaryExpr.Operator.LE;
            else if (match(GLSLTokenType.GE)) op = GLSLBinaryExpr.Operator.GE;
            else break;
            
            GLSLExpression right = parseShiftExpression();
            left = new GLSLBinaryExpr(op, left, right);
        }
        
        return left;
    }
    
    private GLSLExpression parseShiftExpression() {
        GLSLExpression left = parseAdditiveExpression();
        
        while (true) {
            if (match(GLSLTokenType.LEFT_SHIFT)) {
                GLSLExpression right = parseAdditiveExpression();
                left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.LEFT_SHIFT, left, right);
            } else if (match(GLSLTokenType.RIGHT_SHIFT)) {
                GLSLExpression right = parseAdditiveExpression();
                left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.RIGHT_SHIFT, left, right);
            } else {
                break;
            }
        }
        
        return left;
    }
    
    private GLSLExpression parseAdditiveExpression() {
        GLSLExpression left = parseMultiplicativeExpression();
        
        while (true) {
            if (match(GLSLTokenType.PLUS)) {
                GLSLExpression right = parseMultiplicativeExpression();
                left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.ADD, left, right);
            } else if (match(GLSLTokenType.MINUS)) {
                GLSLExpression right = parseMultiplicativeExpression();
                left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.SUB, left, right);
            } else {
                break;
            }
        }
        
        return left;
    }
    
    private GLSLExpression parseMultiplicativeExpression() {
        GLSLExpression left = parseUnaryExpression();
        
        while (true) {
            if (match(GLSLTokenType.STAR)) {
                GLSLExpression right = parseUnaryExpression();
                left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.MUL, left, right);
            } else if (match(GLSLTokenType.SLASH)) {
                GLSLExpression right = parseUnaryExpression();
                left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.DIV, left, right);
            } else if (match(GLSLTokenType.PERCENT)) {
                GLSLExpression right = parseUnaryExpression();
                left = new GLSLBinaryExpr(GLSLBinaryExpr.Operator.MOD, left, right);
            } else {
                break;
            }
        }
        
        return left;
    }
    
    private GLSLExpression parseUnaryExpression() {
        GLSLToken token = peek();
        
        // Prefix operators
        if (match(GLSLTokenType.INCREMENT)) {
            GLSLExpression operand = parseUnaryExpression();
            GLSLUnaryExpr expr = new GLSLUnaryExpr(GLSLUnaryExpr.Operator.PRE_INCREMENT, operand);
            expr.setLocation(token.line, token.column);
            return expr;
        }
        
        if (match(GLSLTokenType.DECREMENT)) {
            GLSLExpression operand = parseUnaryExpression();
            GLSLUnaryExpr expr = new GLSLUnaryExpr(GLSLUnaryExpr.Operator.PRE_DECREMENT, operand);
            expr.setLocation(token.line, token.column);
            return expr;
        }
        
        if (match(GLSLTokenType.PLUS)) {
            GLSLExpression operand = parseUnaryExpression();
            GLSLUnaryExpr expr = new GLSLUnaryExpr(GLSLUnaryExpr.Operator.PLUS, operand);
            expr.setLocation(token.line, token.column);
            return expr;
        }
        
        if (match(GLSLTokenType.MINUS)) {
            GLSLExpression operand = parseUnaryExpression();
            GLSLUnaryExpr expr = new GLSLUnaryExpr(GLSLUnaryExpr.Operator.MINUS, operand);
            expr.setLocation(token.line, token.column);
            return expr;
        }
        
        if (match(GLSLTokenType.NOT)) {
            GLSLExpression operand = parseUnaryExpression();
            GLSLUnaryExpr expr = new GLSLUnaryExpr(GLSLUnaryExpr.Operator.NOT, operand);
            expr.setLocation(token.line, token.column);
            return expr;
        }
        
        if (match(GLSLTokenType.TILDE)) {
            GLSLExpression operand = parseUnaryExpression();
            GLSLUnaryExpr expr = new GLSLUnaryExpr(GLSLUnaryExpr.Operator.BIT_NOT, operand);
            expr.setLocation(token.line, token.column);
            return expr;
        }
        
        return parsePostfixExpression();
    }
    
    private GLSLExpression parsePostfixExpression() {
        GLSLExpression expr = parsePrimaryExpression();
        
        while (true) {
            if (match(GLSLTokenType.LEFT_BRACKET)) {
                // Array subscript
                GLSLExpression index = parseExpression();
                consume(GLSLTokenType.RIGHT_BRACKET);
                expr = new GLSLSubscriptExpr(expr, index);
            } else if (match(GLSLTokenType.DOT)) {
                // Member access or swizzle
                if (!check(GLSLTokenType.IDENTIFIER)) {
                    error(peek(), "Expected member name");
                    break;
                }
                String member = advance().value;
                GLSLMemberExpr memberExpr = new GLSLMemberExpr(expr, member);
                memberExpr.isSwizzle = isSwizzle(member);
                expr = memberExpr;
            } else if (match(GLSLTokenType.INCREMENT)) {
                // Post-increment
                expr = new GLSLUnaryExpr(GLSLUnaryExpr.Operator.POST_INCREMENT, expr);
            } else if (match(GLSLTokenType.DECREMENT)) {
                // Post-decrement
                expr = new GLSLUnaryExpr(GLSLUnaryExpr.Operator.POST_DECREMENT, expr);
            } else if (check(GLSLTokenType.LEFT_PAREN)) {
                // This shouldn't happen for normal postfix - function calls are handled in primary
                break;
            } else {
                break;
            }
        }
        
        return expr;
    }
    
    private boolean isSwizzle(String member) {
        if (member.length() > 4) return false;
        
        for (int i = 0; i < member.length(); i++) {
            char c = member.charAt(i);
            // xyzw, rgba, stpq
            if ("xyzwrgbastpq".indexOf(c) < 0) {
                return false;
            }
        }
        return true;
    }
    
    private GLSLExpression parsePrimaryExpression() {
        GLSLToken token = peek();
        
        // Literals
        if (token.type == GLSLTokenType.INT_LITERAL) {
            advance();
            GLSLLiteralExpr lit = new GLSLLiteralExpr();
            lit.literalType = GLSLLiteralExpr.LiteralType.INT;
            lit.rawValue = token.value;
            lit.intValue = token.asInt();
            lit.isConstant = true;
            lit.setLocation(token.line, token.column);
            return lit;
        }
        
        if (token.type == GLSLTokenType.UINT_LITERAL) {
            advance();
            GLSLLiteralExpr lit = new GLSLLiteralExpr();
            lit.literalType = GLSLLiteralExpr.LiteralType.UINT;
            lit.rawValue = token.value;
            lit.intValue = token.asInt();
            lit.isConstant = true;
            lit.setLocation(token.line, token.column);
            return lit;
        }
        
        if (token.type == GLSLTokenType.FLOAT_LITERAL) {
            advance();
            GLSLLiteralExpr lit = new GLSLLiteralExpr();
            lit.literalType = GLSLLiteralExpr.LiteralType.FLOAT;
            lit.rawValue = token.value;
            lit.floatValue = token.asFloat();
            lit.isConstant = true;
            lit.setLocation(token.line, token.column);
            return lit;
        }
        
        if (token.type == GLSLTokenType.DOUBLE_LITERAL) {
            advance();
            GLSLLiteralExpr lit = new GLSLLiteralExpr();
            lit.literalType = GLSLLiteralExpr.LiteralType.DOUBLE;
            lit.rawValue = token.value;
            lit.floatValue = token.asFloat();
            lit.isConstant = true;
            lit.setLocation(token.line, token.column);
            return lit;
        }
        
        if (token.type == GLSLTokenType.BOOL_LITERAL) {
            advance();
            GLSLLiteralExpr lit = new GLSLLiteralExpr();
            lit.literalType = GLSLLiteralExpr.LiteralType.BOOL;
            lit.rawValue = token.value;
            lit.boolValue = token.asBool();
            lit.isConstant = true;
            lit.setLocation(token.line, token.column);
            return lit;
        }
        
        // Parenthesized expression
        if (match(GLSLTokenType.LEFT_PAREN)) {
            parenDepth++;
            GLSLExpression expr = parseExpression();
            consume(GLSLTokenType.RIGHT_PAREN);
            parenDepth--;
            return expr;
        }
        
        // Type constructor or function call
        if (token.type.isTypeKeyword()) {
            return parseConstructorCall();
        }
        
        // Identifier - could be variable, function call, or struct constructor
        if (token.type == GLSLTokenType.IDENTIFIER) {
            String name = advance().value;
            
            // Function call or struct constructor
            if (check(GLSLTokenType.LEFT_PAREN)) {
                return parseFunctionCall(name, token);
            }
            
            // Variable reference
            GLSLIdentifierExpr id = new GLSLIdentifierExpr(name);
            id.setLocation(token.line, token.column);
            id.isLValue = true;
            
            // Resolve symbol
            GLSLSymbol symbol = currentScope.lookup(name);
            if (symbol != null) {
                id.resolvedSymbol = symbol;
                id.resolvedType = symbol.type;
            }
            
            return id;
        }
        
        // Built-in variables
        if (token.type.isBuiltin()) {
            advance();
            GLSLIdentifierExpr id = new GLSLIdentifierExpr(token.value);
            id.setLocation(token.line, token.column);
            id.isLValue = isBuiltinWritable(token.type);
            return id;
        }
        
        error(token, "Expected expression");
        advance(); // Skip problematic token
        return new GLSLLiteralExpr(0); // Recovery
    }
    
    private GLSLCallExpr parseConstructorCall() {
        GLSLToken token = peek();
        String typeName = token.value;
        advance();
        
        GLSLCallExpr call = new GLSLCallExpr(typeName);
        call.isConstructor = true;
        call.setLocation(token.line, token.column);
        
        consume(GLSLTokenType.LEFT_PAREN);
        
        if (!check(GLSLTokenType.RIGHT_PAREN)) {
            do {
                GLSLExpression arg = parseAssignmentExpression();
                call.addArgument(arg);
            } while (match(GLSLTokenType.COMMA));
        }
        
        consume(GLSLTokenType.RIGHT_PAREN);
        return call;
    }
    
    private GLSLCallExpr parseFunctionCall(String name, GLSLToken nameToken) {
        GLSLCallExpr call = new GLSLCallExpr(name);
        call.setLocation(nameToken.line, nameToken.column);
        
        // Check if it's a struct constructor
        if (knownStructs.contains(name)) {
            call.isConstructor = true;
        }
        
        // Check if it's a built-in function
        call.isBuiltinFunction = isBuiltinFunction(name);
        
        consume(GLSLTokenType.LEFT_PAREN);
        
        if (!check(GLSLTokenType.RIGHT_PAREN)) {
            do {
                GLSLExpression arg = parseAssignmentExpression();
                call.addArgument(arg);
            } while (match(GLSLTokenType.COMMA));
        }
        
        consume(GLSLTokenType.RIGHT_PAREN);
        return call;
    }
    
    private boolean isBuiltinFunction(String name) {
        // Common GLSL built-in functions
        switch (name) {
            // Trigonometric
            case "radians": case "degrees": case "sin": case "cos": case "tan":
            case "asin": case "acos": case "atan": case "sinh": case "cosh": case "tanh":
            case "asinh": case "acosh": case "atanh":
            // Exponential
            case "pow": case "exp": case "log": case "exp2": case "log2":
            case "sqrt": case "inversesqrt":
            // Common
            case "abs": case "sign": case "floor": case "trunc": case "round":
            case "roundEven": case "ceil": case "fract": case "mod": case "modf":
            case "min": case "max": case "clamp": case "mix": case "step":
            case "smoothstep": case "isnan": case "isinf":
            // Geometric
            case "length": case "distance": case "dot": case "cross": case "normalize":
            case "faceforward": case "reflect": case "refract":
            // Matrix
            case "matrixCompMult": case "outerProduct": case "transpose":
            case "determinant": case "inverse":
            // Vector relational
            case "lessThan": case "lessThanEqual": case "greaterThan":
            case "greaterThanEqual": case "equal": case "notEqual":
            case "any": case "all": case "not":
            // Texture
            case "texture": case "textureProj": case "textureLod":
            case "textureOffset": case "texelFetch": case "texelFetchOffset":
            case "textureProjOffset": case "textureLodOffset":
            case "textureProjLod": case "textureProjLodOffset":
            case "textureGrad": case "textureGradOffset":
            case "textureProjGrad": case "textureProjGradOffset":
            case "textureGather": case "textureGatherOffset":
            case "textureSize": case "textureQueryLod": case "textureQueryLevels":
            // Legacy texture
            case "texture1D": case "texture2D": case "texture3D": case "textureCube":
            case "texture1DProj": case "texture2DProj": case "texture3DProj":
            case "texture1DLod": case "texture2DLod": case "texture3DLod":
            case "textureCubeLod":
            case "shadow1D": case "shadow2D": case "shadow1DProj": case "shadow2DProj":
            case "shadow1DLod": case "shadow2DLod":
            // Derivative
            case "dFdx": case "dFdy": case "fwidth":
            case "dFdxCoarse": case "dFdyCoarse": case "fwidthCoarse":
            case "dFdxFine": case "dFdyFine": case "fwidthFine":
            // Noise (deprecated but still parsed)
            case "noise1": case "noise2": case "noise3": case "noise4":
            // Atomic
            case "atomicAdd": case "atomicMin": case "atomicMax":
            case "atomicAnd": case "atomicOr": case "atomicXor":
            case "atomicExchange": case "atomicCompSwap":
            // Image
            case "imageSize": case "imageLoad": case "imageStore":
            case "imageAtomicAdd": case "imageAtomicMin": case "imageAtomicMax":
            case "imageAtomicAnd": case "imageAtomicOr": case "imageAtomicXor":
            case "imageAtomicExchange": case "imageAtomicCompSwap":
            // Barrier
            case "barrier": case "memoryBarrier": case "memoryBarrierAtomicCounter":
            case "memoryBarrierBuffer": case "memoryBarrierShared":
            case "memoryBarrierImage": case "groupMemoryBarrier":
            // Packing
            case "packSnorm2x16": case "unpackSnorm2x16":
            case "packUnorm2x16": case "unpackUnorm2x16":
            case "packSnorm4x8": case "unpackSnorm4x8":
            case "packUnorm4x8": case "unpackUnorm4x8":
            case "packHalf2x16": case "unpackHalf2x16":
            case "packDouble2x32": case "unpackDouble2x32":
            // Integer
            case "bitfieldExtract": case "bitfieldInsert": case "bitfieldReverse":
            case "bitCount": case "findLSB": case "findMSB":
            case "uaddCarry": case "usubBorrow": case "umulExtended": case "imulExtended":
            // Interpolation
            case "interpolateAtCentroid": case "interpolateAtSample": case "interpolateAtOffset":
            // FMA
            case "fma":
            // Fragment processing
            case "EmitVertex": case "EndPrimitive":
            case "EmitStreamVertex": case "EndStreamPrimitive":
                return true;
            default:
                return false;
        }
    }
    
    private boolean isBuiltinWritable(GLSLTokenType type) {
        switch (type) {
            case GL_POSITION:
            case GL_POINTSIZE:
            case GL_CLIPDISTANCE:
            case GL_CULLDISTANCE:
            case GL_LAYER:
            case GL_VIEWPORTINDEX:
            case GL_FRAGDEPTH:
            case GL_SAMPLEMASK:
            case GL_FRAGCOLOR:
            case GL_FRAGDATA:
            case GL_TESSLEVELOUTER:
            case GL_TESSLEVELINNER:
                return true;
            default:
                return false;
        }
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    private GLSLToken peek() {
        return tokens.peek();
    }
    
    private GLSLToken peek(int offset) {
        return tokens.peek(offset);
    }
    
    private GLSLToken advance() {
        return tokens.advance();
    }
    
    private boolean check(GLSLTokenType type) {
        return tokens.check(type);
    }
    
    private boolean match(GLSLTokenType type) {
        return tokens.match(type);
    }
    
    private GLSLToken consume(GLSLTokenType type) {
        if (check(type)) {
            return advance();
        }
        error(peek(), "Expected " + type);
        return peek();
    }
    
    private boolean isAtEnd() {
        return tokens.isAtEnd();
    }
    
    private void error(GLSLToken token, String message) {
        errors.add(new GLSLError(token.line, token.column, message, GLSLErrorType.SYNTAX_ERROR));
    }
    
    private void warning(GLSLToken token, String message) {
        warnings.add("Warning at " + token.line + ":" + token.column + ": " + message);
    }
    
    private void synchronize() {
        advance();
        
        while (!isAtEnd()) {
            // Synchronize on statement boundaries
            if (peek().type == GLSLTokenType.SEMICOLON) {
                advance();
                return;
            }
            
            switch (peek().type) {
                case IF:
                case FOR:
                case WHILE:
                case DO:
                case SWITCH:
                case RETURN:
                case BREAK:
                case CONTINUE:
                case DISCARD:
                case STRUCT:
                case VOID:
                case BOOL:
                case INT:
                case UINT:
                case FLOAT:
                case DOUBLE:
                case VEC2:
                case VEC3:
                case VEC4:
                case MAT2:
                case MAT3:
                case MAT4:
                case UNIFORM:
                case IN:
                case OUT:
                case LAYOUT:
                    return;
                default:
                    advance();
            }
        }
    }
    
    /**
     * Evaluate constant integer expression at compile time.
     * Used for array sizes, layout qualifiers, etc.
     */
    private int evaluateConstantInt(GLSLExpression expr) {
        if (expr instanceof GLSLLiteralExpr) {
            GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
            return (int) lit.intValue;
        }
        
        if (expr instanceof GLSLUnaryExpr) {
            GLSLUnaryExpr unary = (GLSLUnaryExpr) expr;
            int operand = evaluateConstantInt(unary.operand);
            switch (unary.operator) {
                case MINUS: return -operand;
                case PLUS: return operand;
                case BIT_NOT: return ~operand;
                default: break;
            }
        }
        
        if (expr instanceof GLSLBinaryExpr) {
            GLSLBinaryExpr binary = (GLSLBinaryExpr) expr;
            int left = evaluateConstantInt(binary.left);
            int right = evaluateConstantInt(binary.right);
            switch (binary.operator) {
                case ADD: return left + right;
                case SUB: return left - right;
                case MUL: return left * right;
                case DIV: return right != 0 ? left / right : 0;
                case MOD: return right != 0 ? left % right : 0;
                case LEFT_SHIFT: return left << right;
                case RIGHT_SHIFT: return left >> right;
                case BIT_AND: return left & right;
                case BIT_OR: return left | right;
                case BIT_XOR: return left ^ right;
                default: break;
            }
        }
        
        // If we can't evaluate, return -1 (unsized)
        return -1;
    }
    
    /**
     * Reset parser state for reuse.
     */
    void reset() {
        tokens = null;
        shaderType = null;
        detectedVersion = null;
        symbolTable = null;
        currentScope = null;
        errors.clear();
        warnings.clear();
        knownStructs.clear();
        inLoop = false;
        inSwitch = false;
        inFunction = false;
        parenDepth = 0;
        braceDepth = 0;
    }
}

// ============================================================================
// TRANSLATION RULE SYSTEM
// ============================================================================

/**
 * Base interface for all translation rules.
 * Rules are bidirectional and version-aware.
 */
interface GLSLTranslationRule {
    
    /**
     * Check if this rule applies for the given version transition.
     */
    boolean applies(GLSLVersion sourceVersion, GLSLVersion targetVersion, GLSLShaderType shaderType);
    
    /**
     * Get the priority of this rule. Higher priority rules are applied first.
     */
    int getPriority();
    
    /**
     * Get the category of this rule for grouping.
     */
    RuleCategory getCategory();
    
    enum RuleCategory {
        PREPROCESSOR,   // Version, extension directives
        QUALIFIER,      // Storage, interpolation qualifiers
        TYPE,           // Type translations
        FUNCTION,       // Built-in function translations
        VARIABLE,       // Built-in variable translations
        OUTPUT,         // Fragment output handling
        LAYOUT,         // Layout qualifier handling
        FEATURE         // Feature-specific translations
    }
}

/**
 * Rule that transforms AST nodes.
 */
interface GLSLNodeTransformRule extends GLSLTranslationRule {
    
    /**
     * Transform a node. Returns the transformed node or null if no change.
     */
    GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context);
    
    /**
     * Check if this rule can transform the given node type.
     */
    boolean canTransform(Class<? extends GLSLASTNode> nodeType);
}

/**
 * Rule that translates identifiers/names.
 */
interface GLSLNameTranslationRule extends GLSLTranslationRule {
    
    /**
     * Translate a name. Returns translated name or null if no translation.
     */
    String translate(String name, GLSLTranslationContext context);
    
    /**
     * Get all names this rule can translate.
     */
    Set<String> getSourceNames();
}

/**
 * Context passed to translation rules containing all necessary state.
 */
final class GLSLTranslationContext {
    
    final GLSLVersion sourceVersion;
    final GLSLVersion targetVersion;
    final GLSLShaderType shaderType;
    final GLSLShaderAST shader;
    final GLSLMemoryPool pool;
    final GLSLCapabilityRegistry capabilities;
    
    // Collected information during translation
    final Map<String, GLSLType> samplerTypes = new HashMap<>();
    final Map<String, Integer> attributeLocations = new HashMap<>();
    final Map<String, GLSLVariableDecl> globalVariables = new HashMap<>();
    final Set<String> usedOutputNames = new HashSet<>();
    final List<GLSLVariableDecl> generatedOutputs = new ArrayList<>();
    final List<GLSLExtensionDecl> requiredExtensions = new ArrayList<>();
    final List<String> warnings = new ArrayList<>();
    
    // Fragment output tracking
    boolean usesFragColor = false;
    boolean usesFragData = false;
    int maxFragDataIndex = -1;
    String primaryOutputName = "fragColor";
    
    // Translation direction
    final boolean isUpgrade;
    final boolean isDowngrade;
    
    GLSLTranslationContext(GLSLVersion sourceVersion, GLSLVersion targetVersion,
                           GLSLShaderType shaderType, GLSLShaderAST shader,
                           GLSLMemoryPool pool, GLSLCapabilityRegistry capabilities) {
        this.sourceVersion = sourceVersion;
        this.targetVersion = targetVersion;
        this.shaderType = shaderType;
        this.shader = shader;
        this.pool = pool;
        this.capabilities = capabilities;
        this.isUpgrade = targetVersion.versionNumber > sourceVersion.versionNumber;
        this.isDowngrade = targetVersion.versionNumber < sourceVersion.versionNumber;
    }
    
    void registerSamplerType(String name, GLSLType type) {
        samplerTypes.put(name, type);
    }
    
    GLSLType getSamplerType(String name) {
        return samplerTypes.get(name);
    }
    
    void addWarning(String warning) {
        warnings.add(warning);
    }
    
    boolean supportsFeature(GLSLFeature feature) {
        return capabilities.isSupported(feature, targetVersion);
    }
    
    boolean hasExtension(GLSLExtension extension) {
        return capabilities.isExtensionAvailable(extension);
    }
}

// ============================================================================
// TRANSLATION RULE REGISTRY
// ============================================================================

/**
 * Registry of all translation rules, organized for efficient lookup.
 */
final class GLSLTranslationRuleRegistry {
    
    // Rules organized by category and priority
    private final EnumMap<GLSLTranslationRule.RuleCategory, List<GLSLTranslationRule>> rulesByCategory;
    
    // Name translation rules indexed by source name
    private final Map<String, List<GLSLNameTranslationRule>> nameRules = new HashMap<>();
    
    // Node transformation rules indexed by node class
    private final Map<Class<?>, List<GLSLNodeTransformRule>> nodeRules = new IdentityHashMap<>();
    
    // All rules sorted by priority
    private final List<GLSLTranslationRule> allRules = new ArrayList<>();
    
    GLSLTranslationRuleRegistry() {
        rulesByCategory = new EnumMap<>(GLSLTranslationRule.RuleCategory.class);
        for (GLSLTranslationRule.RuleCategory cat : GLSLTranslationRule.RuleCategory.values()) {
            rulesByCategory.put(cat, new ArrayList<>());
        }
        
        // Register all built-in rules
        registerBuiltinRules();
        
        // Sort all rule lists by priority (descending)
        Comparator<GLSLTranslationRule> priorityComparator = 
            (a, b) -> Integer.compare(b.getPriority(), a.getPriority());
        
        for (List<GLSLTranslationRule> rules : rulesByCategory.values()) {
            rules.sort(priorityComparator);
        }
        allRules.sort(priorityComparator);
    }
    
    void register(GLSLTranslationRule rule) {
        allRules.add(rule);
        rulesByCategory.get(rule.getCategory()).add(rule);
        
        if (rule instanceof GLSLNameTranslationRule) {
            GLSLNameTranslationRule nameRule = (GLSLNameTranslationRule) rule;
            for (String name : nameRule.getSourceNames()) {
                nameRules.computeIfAbsent(name, k -> new ArrayList<>()).add(nameRule);
            }
        }
        
        if (rule instanceof GLSLNodeTransformRule) {
            GLSLNodeTransformRule nodeRule = (GLSLNodeTransformRule) rule;
            // Register for common node types
            registerNodeRule(nodeRule, GLSLCallExpr.class);
            registerNodeRule(nodeRule, GLSLIdentifierExpr.class);
            registerNodeRule(nodeRule, GLSLVariableDecl.class);
            registerNodeRule(nodeRule, GLSLFunctionDecl.class);
            registerNodeRule(nodeRule, GLSLMemberExpr.class);
            registerNodeRule(nodeRule, GLSLSubscriptExpr.class);
        }
    }
    
    private void registerNodeRule(GLSLNodeTransformRule rule, Class<? extends GLSLASTNode> nodeClass) {
        if (rule.canTransform(nodeClass)) {
            nodeRules.computeIfAbsent(nodeClass, k -> new ArrayList<>()).add(rule);
        }
    }
    
    List<GLSLNameTranslationRule> getNameRules(String name) {
        return nameRules.getOrDefault(name, Collections.emptyList());
    }
    
    List<GLSLNodeTransformRule> getNodeRules(Class<?> nodeClass) {
        return nodeRules.getOrDefault(nodeClass, Collections.emptyList());
    }
    
    List<GLSLTranslationRule> getRulesByCategory(GLSLTranslationRule.RuleCategory category) {
        return rulesByCategory.get(category);
    }
    
    // ========================================================================
    // BUILT-IN RULE REGISTRATION
    // ========================================================================
    
    private void registerBuiltinRules() {
        // Qualifier translation rules
        register(new AttributeToInRule());
        register(new VaryingToInOutRule());
        register(new InToAttributeRule());
        register(new InOutToVaryingRule());
        
        // Function translation rules - texture functions
        registerTextureFunctionRules();
        
        // Built-in variable rules
        registerBuiltinVariableRules();
        
        // Fragment output rules
        register(new FragColorToOutputRule());
        register(new FragDataToOutputRule());
        register(new OutputToFragColorRule());
        register(new OutputToFragDataRule());
        
        // Layout qualifier rules
        register(new LayoutLocationDowngradeRule());
        register(new LayoutBindingDowngradeRule());
        
        // Type translation rules
        register(new NonSquareMatrixRule());
        register(new DoublePrecisionRule());
        
        // Feature-specific rules
        register(new SwitchStatementRule());
        register(new BitOperationsRule());
    }
    
    private void registerTextureFunctionRules() {
        // Upgrade rules: texture2D -> texture, etc.
        register(new TextureFunctionUpgradeRule("texture1D", "texture", GLSLType.BaseType.SAMPLER_1D));
        register(new TextureFunctionUpgradeRule("texture2D", "texture", GLSLType.BaseType.SAMPLER_2D));
        register(new TextureFunctionUpgradeRule("texture3D", "texture", GLSLType.BaseType.SAMPLER_3D));
        register(new TextureFunctionUpgradeRule("textureCube", "texture", GLSLType.BaseType.SAMPLER_CUBE));
        register(new TextureFunctionUpgradeRule("texture1DProj", "textureProj", GLSLType.BaseType.SAMPLER_1D));
        register(new TextureFunctionUpgradeRule("texture2DProj", "textureProj", GLSLType.BaseType.SAMPLER_2D));
        register(new TextureFunctionUpgradeRule("texture3DProj", "textureProj", GLSLType.BaseType.SAMPLER_3D));
        register(new TextureFunctionUpgradeRule("texture1DLod", "textureLod", GLSLType.BaseType.SAMPLER_1D));
        register(new TextureFunctionUpgradeRule("texture2DLod", "textureLod", GLSLType.BaseType.SAMPLER_2D));
        register(new TextureFunctionUpgradeRule("texture3DLod", "textureLod", GLSLType.BaseType.SAMPLER_3D));
        register(new TextureFunctionUpgradeRule("textureCubeLod", "textureLod", GLSLType.BaseType.SAMPLER_CUBE));
        register(new TextureFunctionUpgradeRule("texture1DProjLod", "textureProjLod", GLSLType.BaseType.SAMPLER_1D));
        register(new TextureFunctionUpgradeRule("texture2DProjLod", "textureProjLod", GLSLType.BaseType.SAMPLER_2D));
        register(new TextureFunctionUpgradeRule("texture3DProjLod", "textureProjLod", GLSLType.BaseType.SAMPLER_3D));
        
        // Shadow texture functions
        register(new TextureFunctionUpgradeRule("shadow1D", "texture", GLSLType.BaseType.SAMPLER_1D_SHADOW));
        register(new TextureFunctionUpgradeRule("shadow2D", "texture", GLSLType.BaseType.SAMPLER_2D_SHADOW));
        register(new TextureFunctionUpgradeRule("shadow1DProj", "textureProj", GLSLType.BaseType.SAMPLER_1D_SHADOW));
        register(new TextureFunctionUpgradeRule("shadow2DProj", "textureProj", GLSLType.BaseType.SAMPLER_2D_SHADOW));
        register(new TextureFunctionUpgradeRule("shadow1DLod", "textureLod", GLSLType.BaseType.SAMPLER_1D_SHADOW));
        register(new TextureFunctionUpgradeRule("shadow2DLod", "textureLod", GLSLType.BaseType.SAMPLER_2D_SHADOW));
        register(new TextureFunctionUpgradeRule("shadow1DProjLod", "textureProjLod", GLSLType.BaseType.SAMPLER_1D_SHADOW));
        register(new TextureFunctionUpgradeRule("shadow2DProjLod", "textureProjLod", GLSLType.BaseType.SAMPLER_2D_SHADOW));
        
        // Downgrade rules: texture -> texture2D, etc.
        register(new TextureFunctionDowngradeRule());
    }
    
    private void registerBuiltinVariableRules() {
        // Legacy vertex attributes (GLSL 1.10/1.20 only)
        register(new LegacyVertexAttributeRule("gl_Vertex", "vec4"));
        register(new LegacyVertexAttributeRule("gl_Normal", "vec3"));
        register(new LegacyVertexAttributeRule("gl_Color", "vec4"));
        register(new LegacyVertexAttributeRule("gl_SecondaryColor", "vec4"));
        register(new LegacyVertexAttributeRule("gl_MultiTexCoord0", "vec4"));
        register(new LegacyVertexAttributeRule("gl_MultiTexCoord1", "vec4"));
        register(new LegacyVertexAttributeRule("gl_MultiTexCoord2", "vec4"));
        register(new LegacyVertexAttributeRule("gl_MultiTexCoord3", "vec4"));
        register(new LegacyVertexAttributeRule("gl_MultiTexCoord4", "vec4"));
        register(new LegacyVertexAttributeRule("gl_MultiTexCoord5", "vec4"));
        register(new LegacyVertexAttributeRule("gl_MultiTexCoord6", "vec4"));
        register(new LegacyVertexAttributeRule("gl_MultiTexCoord7", "vec4"));
        register(new LegacyVertexAttributeRule("gl_FogCoord", "float"));
        
        // Legacy matrices
        register(new LegacyMatrixRule("gl_ModelViewMatrix", "mat4"));
        register(new LegacyMatrixRule("gl_ProjectionMatrix", "mat4"));
        register(new LegacyMatrixRule("gl_ModelViewProjectionMatrix", "mat4"));
        register(new LegacyMatrixRule("gl_TextureMatrix", "mat4")); // Actually an array
        register(new LegacyMatrixRule("gl_NormalMatrix", "mat3"));
        register(new LegacyMatrixRule("gl_ModelViewMatrixInverse", "mat4"));
        register(new LegacyMatrixRule("gl_ProjectionMatrixInverse", "mat4"));
        register(new LegacyMatrixRule("gl_ModelViewProjectionMatrixInverse", "mat4"));
        register(new LegacyMatrixRule("gl_ModelViewMatrixTranspose", "mat4"));
        register(new LegacyMatrixRule("gl_ProjectionMatrixTranspose", "mat4"));
        register(new LegacyMatrixRule("gl_ModelViewProjectionMatrixTranspose", "mat4"));
        register(new LegacyMatrixRule("gl_ModelViewMatrixInverseTranspose", "mat4"));
        register(new LegacyMatrixRule("gl_ProjectionMatrixInverseTranspose", "mat4"));
        register(new LegacyMatrixRule("gl_ModelViewProjectionMatrixInverseTranspose", "mat4"));
        
        // Legacy varying (deprecated in 1.30+)
        register(new LegacyVaryingRule("gl_TexCoord", "vec4")); // Array
        register(new LegacyVaryingRule("gl_FogFragCoord", "float"));
        register(new LegacyVaryingRule("gl_FrontColor", "vec4"));
        register(new LegacyVaryingRule("gl_BackColor", "vec4"));
        register(new LegacyVaryingRule("gl_FrontSecondaryColor", "vec4"));
        register(new LegacyVaryingRule("gl_BackSecondaryColor", "vec4"));
    }
}

// ============================================================================
// QUALIFIER TRANSLATION RULES
// ============================================================================

/**
 * Translates 'attribute' to 'in' for vertex shader inputs (upgrade to 1.30+)
 */
final class AttributeToInRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return shaderType == GLSLShaderType.VERTEX &&
               source.versionNumber < 130 && 
               target.versionNumber >= 130;
    }
    
    @Override
    public int getPriority() { return 100; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.QUALIFIER; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLVariableDecl.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLVariableDecl)) return null;
        
        GLSLVariableDecl decl = (GLSLVariableDecl) node;
        GLSLTypeQualifiers quals = decl.type.qualifiers;
        
        if (quals != null && quals.storage == GLSLTypeQualifiers.Storage.ATTRIBUTE) {
            // Create new qualifiers with IN instead of ATTRIBUTE
            GLSLTypeQualifiers newQuals = quals.copy();
            newQuals.storage = GLSLTypeQualifiers.Storage.IN;
            
            // Create new type with new qualifiers
            decl.type = new GLSLType(decl.type.baseType, decl.type.structName,
                                     decl.type.arrayDimensions, newQuals);
            return decl;
        }
        
        return null;
    }
}

/**
 * Translates 'varying' to 'in'/'out' based on shader stage (upgrade to 1.30+)
 */
final class VaryingToInOutRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return source.versionNumber < 130 && target.versionNumber >= 130;
    }
    
    @Override
    public int getPriority() { return 100; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.QUALIFIER; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLVariableDecl.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLVariableDecl)) return null;
        
        GLSLVariableDecl decl = (GLSLVariableDecl) node;
        GLSLTypeQualifiers quals = decl.type.qualifiers;
        
        if (quals != null && quals.storage == GLSLTypeQualifiers.Storage.VARYING) {
            GLSLTypeQualifiers newQuals = quals.copy();
            
            // In vertex shader: varying -> out
            // In fragment shader: varying -> in
            if (context.shaderType == GLSLShaderType.VERTEX) {
                newQuals.storage = GLSLTypeQualifiers.Storage.OUT;
            } else if (context.shaderType == GLSLShaderType.FRAGMENT) {
                newQuals.storage = GLSLTypeQualifiers.Storage.IN;
            }
            
            decl.type = new GLSLType(decl.type.baseType, decl.type.structName,
                                     decl.type.arrayDimensions, newQuals);
            return decl;
        }
        
        return null;
    }
}

/**
 * Translates 'in' to 'attribute' for vertex shader inputs (downgrade to 1.20 or earlier)
 */
final class InToAttributeRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return shaderType == GLSLShaderType.VERTEX &&
               source.versionNumber >= 130 && 
               target.versionNumber < 130;
    }
    
    @Override
    public int getPriority() { return 100; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.QUALIFIER; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLVariableDecl.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLVariableDecl)) return null;
        
        GLSLVariableDecl decl = (GLSLVariableDecl) node;
        
        // Only transform global declarations (vertex inputs)
        if (decl.parent != context.shader) return null;
        
        GLSLTypeQualifiers quals = decl.type.qualifiers;
        
        if (quals != null && quals.storage == GLSLTypeQualifiers.Storage.IN) {
            GLSLTypeQualifiers newQuals = quals.copy();
            newQuals.storage = GLSLTypeQualifiers.Storage.ATTRIBUTE;
            
            // Remove layout qualifier if present (not supported)
            if (target(context) < 130) {
                newQuals.layout = null;
            }
            
            decl.type = new GLSLType(decl.type.baseType, decl.type.structName,
                                     decl.type.arrayDimensions, newQuals);
            return decl;
        }
        
        return null;
    }
    
    private int target(GLSLTranslationContext ctx) {
        return ctx.targetVersion.versionNumber;
    }
}

/**
 * Translates 'in'/'out' to 'varying' for inter-stage variables (downgrade to 1.20 or earlier)
 */
final class InOutToVaryingRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return source.versionNumber >= 130 && target.versionNumber < 130;
    }
    
    @Override
    public int getPriority() { return 100; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.QUALIFIER; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLVariableDecl.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLVariableDecl)) return null;
        
        GLSLVariableDecl decl = (GLSLVariableDecl) node;
        
        // Only transform global declarations
        if (decl.parent != context.shader) return null;
        
        GLSLTypeQualifiers quals = decl.type.qualifiers;
        if (quals == null) return null;
        
        // Check if this is an inter-stage variable
        boolean shouldTransform = false;
        
        if (context.shaderType == GLSLShaderType.VERTEX && 
            quals.storage == GLSLTypeQualifiers.Storage.OUT) {
            // Vertex output -> varying (but not gl_Position etc.)
            if (!isBuiltinOutput(decl.name)) {
                shouldTransform = true;
            }
        } else if (context.shaderType == GLSLShaderType.FRAGMENT && 
                   quals.storage == GLSLTypeQualifiers.Storage.IN) {
            // Fragment input -> varying (but not gl_FragCoord etc.)
            if (!isBuiltinInput(decl.name)) {
                shouldTransform = true;
            }
        }
        
        if (shouldTransform) {
            GLSLTypeQualifiers newQuals = quals.copy();
            newQuals.storage = GLSLTypeQualifiers.Storage.VARYING;
            newQuals.layout = null; // Remove layout qualifier
            
            // Remove interpolation qualifiers for GLSL 1.10
            if (context.targetVersion.versionNumber < 120) {
                newQuals.interpolation = GLSLTypeQualifiers.Interpolation.NONE;
            }
            
            decl.type = new GLSLType(decl.type.baseType, decl.type.structName,
                                     decl.type.arrayDimensions, newQuals);
            return decl;
        }
        
        return null;
    }
    
    private boolean isBuiltinOutput(String name) {
        return name.startsWith("gl_");
    }
    
    private boolean isBuiltinInput(String name) {
        return name.startsWith("gl_");
    }
}

// ============================================================================
// TEXTURE FUNCTION TRANSLATION RULES
// ============================================================================

/**
 * Upgrades legacy texture functions (texture2D, textureCube, etc.) to generic texture()
 */
final class TextureFunctionUpgradeRule implements GLSLNodeTransformRule {
    
    private final String oldName;
    private final String newName;
    private final GLSLType.BaseType samplerType;
    
    TextureFunctionUpgradeRule(String oldName, String newName, GLSLType.BaseType samplerType) {
        this.oldName = oldName;
        this.newName = newName;
        this.samplerType = samplerType;
    }
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return source.versionNumber < 130 && target.versionNumber >= 130;
    }
    
    @Override
    public int getPriority() { return 90; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.FUNCTION; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLCallExpr.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLCallExpr)) return null;
        
        GLSLCallExpr call = (GLSLCallExpr) node;
        
        if (oldName.equals(call.functionName)) {
            call.functionName = newName;
            call.isBuiltinFunction = true;
            return call;
        }
        
        return null;
    }
}

/**
 * Downgrades generic texture() to legacy texture2D, textureCube, etc.
 * Requires sampler type information from context.
 */
final class TextureFunctionDowngradeRule implements GLSLNodeTransformRule {
    
    // Mapping from sampler type to legacy function name
    private static final Map<GLSLType.BaseType, String> TEXTURE_FUNCTION_MAP = new EnumMap<>(GLSLType.BaseType.class);
    private static final Map<GLSLType.BaseType, String> TEXTURE_PROJ_FUNCTION_MAP = new EnumMap<>(GLSLType.BaseType.class);
    private static final Map<GLSLType.BaseType, String> TEXTURE_LOD_FUNCTION_MAP = new EnumMap<>(GLSLType.BaseType.class);
    
    static {
        TEXTURE_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_1D, "texture1D");
        TEXTURE_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_2D, "texture2D");
        TEXTURE_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_3D, "texture3D");
        TEXTURE_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_CUBE, "textureCube");
        TEXTURE_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_1D_SHADOW, "shadow1D");
        TEXTURE_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_2D_SHADOW, "shadow2D");
        
        TEXTURE_PROJ_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_1D, "texture1DProj");
        TEXTURE_PROJ_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_2D, "texture2DProj");
        TEXTURE_PROJ_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_3D, "texture3DProj");
        TEXTURE_PROJ_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_1D_SHADOW, "shadow1DProj");
        TEXTURE_PROJ_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_2D_SHADOW, "shadow2DProj");
        
        TEXTURE_LOD_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_1D, "texture1DLod");
        TEXTURE_LOD_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_2D, "texture2DLod");
        TEXTURE_LOD_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_3D, "texture3DLod");
        TEXTURE_LOD_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_CUBE, "textureCubeLod");
        TEXTURE_LOD_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_1D_SHADOW, "shadow1DLod");
        TEXTURE_LOD_FUNCTION_MAP.put(GLSLType.BaseType.SAMPLER_2D_SHADOW, "shadow2DLod");
    }
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return source.versionNumber >= 130 && target.versionNumber < 130;
    }
    
    @Override
    public int getPriority() { return 90; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.FUNCTION; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLCallExpr.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLCallExpr)) return null;
        
        GLSLCallExpr call = (GLSLCallExpr) node;
        String funcName = call.functionName;
        
        // Determine which texture function this is
        Map<GLSLType.BaseType, String> funcMap = null;
        
        if ("texture".equals(funcName)) {
            funcMap = TEXTURE_FUNCTION_MAP;
        } else if ("textureProj".equals(funcName)) {
            funcMap = TEXTURE_PROJ_FUNCTION_MAP;
        } else if ("textureLod".equals(funcName)) {
            funcMap = TEXTURE_LOD_FUNCTION_MAP;
        } else if ("textureProjLod".equals(funcName)) {
            // For ProjLod, combine Proj and Lod mappings
            funcMap = new EnumMap<>(GLSLType.BaseType.class);
            funcMap.put(GLSLType.BaseType.SAMPLER_1D, "texture1DProjLod");
            funcMap.put(GLSLType.BaseType.SAMPLER_2D, "texture2DProjLod");
            funcMap.put(GLSLType.BaseType.SAMPLER_3D, "texture3DProjLod");
            funcMap.put(GLSLType.BaseType.SAMPLER_1D_SHADOW, "shadow1DProjLod");
            funcMap.put(GLSLType.BaseType.SAMPLER_2D_SHADOW, "shadow2DProjLod");
        }
        
        if (funcMap == null) return null;
        
        // Get sampler argument
        if (call.arguments.isEmpty()) return null;
        
        GLSLExpression samplerArg = call.arguments.get(0);
        GLSLType samplerType = resolveSamplerType(samplerArg, context);
        
        if (samplerType == null) {
            context.addWarning("Cannot determine sampler type for texture function at line " + call.line);
            return null;
        }
        
        String legacyFunc = funcMap.get(samplerType.baseType);
        if (legacyFunc != null) {
            call.functionName = legacyFunc;
            return call;
        }
        
        return null;
    }
    
    private GLSLType resolveSamplerType(GLSLExpression expr, GLSLTranslationContext context) {
        if (expr instanceof GLSLIdentifierExpr) {
            GLSLIdentifierExpr id = (GLSLIdentifierExpr) expr;
            return context.getSamplerType(id.name);
        }
        return null;
    }
}

// ============================================================================
// FRAGMENT OUTPUT TRANSLATION RULES
// ============================================================================

/**
 * Translates gl_FragColor usage to user-defined output (upgrade to 1.30+)
 */
final class FragColorToOutputRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return shaderType == GLSLShaderType.FRAGMENT &&
               source.versionNumber < 130 && 
               target.versionNumber >= 130;
    }
    
    @Override
    public int getPriority() { return 80; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.OUTPUT; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLIdentifierExpr.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLIdentifierExpr)) return null;
        
        GLSLIdentifierExpr id = (GLSLIdentifierExpr) node;
        
        if ("gl_FragColor".equals(id.name)) {
            context.usesFragColor = true;
            
            // Replace with user-defined output
            String outputName = context.primaryOutputName;
            id.name = outputName;
            context.usedOutputNames.add(outputName);
            
            return id;
        }
        
        return null;
    }
}

/**
 * Translates gl_FragData[n] usage to user-defined outputs (upgrade to 1.30+)
 */
final class FragDataToOutputRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return shaderType == GLSLShaderType.FRAGMENT &&
               source.versionNumber < 130 && 
               target.versionNumber >= 130;
    }
    
    @Override
    public int getPriority() { return 80; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.OUTPUT; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLSubscriptExpr.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLSubscriptExpr)) return null;
        
        GLSLSubscriptExpr subscript = (GLSLSubscriptExpr) node;
        
        if (subscript.array instanceof GLSLIdentifierExpr) {
            GLSLIdentifierExpr arrayId = (GLSLIdentifierExpr) subscript.array;
            
            if ("gl_FragData".equals(arrayId.name)) {
                context.usesFragData = true;
                
                // Determine index
                int index = 0;
                if (subscript.index instanceof GLSLLiteralExpr) {
                    GLSLLiteralExpr lit = (GLSLLiteralExpr) subscript.index;
                    index = (int) lit.intValue;
                }
                
                context.maxFragDataIndex = Math.max(context.maxFragDataIndex, index);
                
                // Replace with user-defined output: fragData_0, fragData_1, etc.
                String outputName = "fragData_" + index;
                context.usedOutputNames.add(outputName);
                
                GLSLIdentifierExpr replacement = new GLSLIdentifierExpr(outputName);
                replacement.setLocation(subscript.line, subscript.column);
                return replacement;
            }
        }
        
        return null;
    }
}

/**
 * Translates user-defined outputs to gl_FragColor (downgrade to 1.20 or earlier)
 */
final class OutputToFragColorRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return shaderType == GLSLShaderType.FRAGMENT &&
               source.versionNumber >= 130 && 
               target.versionNumber < 130;
    }
    
    @Override
    public int getPriority() { return 80; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.OUTPUT; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLIdentifierExpr.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLIdentifierExpr)) return null;
        
        GLSLIdentifierExpr id = (GLSLIdentifierExpr) node;
        
        // Check if this is a fragment output variable
        GLSLVariableDecl outputDecl = context.globalVariables.get(id.name);
        if (outputDecl != null && outputDecl.type.qualifiers != null &&
            outputDecl.type.qualifiers.storage == GLSLTypeQualifiers.Storage.OUT) {
            
            // Check layout location to determine which output
            GLSLLayoutQualifier layout = outputDecl.type.qualifiers.layout;
            int location = layout != null ? layout.location : 0;
            
            if (location == 0) {
                id.name = "gl_FragColor";
            } else {
                // Multiple outputs need gl_FragData
                context.usesFragData = true;
                // This will be handled by creating a subscript expression
                // For now, just warn
                context.addWarning("Multiple render targets require gl_FragData, which may have limited support");
            }
            
            return id;
        }
        
        return null;
    }
}

/**
 * Translates user-defined output array to gl_FragData (downgrade to 1.20 or earlier)
 */
final class OutputToFragDataRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return shaderType == GLSLShaderType.FRAGMENT &&
               source.versionNumber >= 130 && 
               target.versionNumber < 130;
    }
    
    @Override
    public int getPriority() { return 79; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.OUTPUT; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLVariableDecl.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLVariableDecl)) return null;
        
        GLSLVariableDecl decl = (GLSLVariableDecl) node;
        
        // Check if this is a fragment output declaration
        if (decl.type.qualifiers != null &&
            decl.type.qualifiers.storage == GLSLTypeQualifiers.Storage.OUT &&
            context.shaderType == GLSLShaderType.FRAGMENT) {
            
            // Mark this declaration for removal (gl_FragColor/gl_FragData are built-in)
            decl.setFlag(GLSLASTNode.FLAG_DEAD);
            
            // Store mapping for identifier replacement
            context.globalVariables.put(decl.name, decl);
            
            return decl;
        }
        
        return null;
    }
}

// ============================================================================
// LAYOUT QUALIFIER TRANSLATION RULES
// ============================================================================

/**
 * Handles layout(location=N) for versions that don't support it natively.
 * Uses GL_ARB_explicit_attrib_location extension if available.
 */
final class LayoutLocationDowngradeRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return source.versionNumber >= 330 && target.versionNumber < 330;
    }
    
    @Override
    public int getPriority() { return 70; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.LAYOUT; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLVariableDecl.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLVariableDecl)) return null;
        
        GLSLVariableDecl decl = (GLSLVariableDecl) node;
        GLSLTypeQualifiers quals = decl.type.qualifiers;
        
        if (quals == null || quals.layout == null || quals.layout.location < 0) {
            return null;
        }
        
        int location = quals.layout.location;
        
        // Check if we can use extension
        if (context.targetVersion.versionNumber >= 120 &&
            context.hasExtension(GLSLExtension.ARB_EXPLICIT_ATTRIB_LOCATION)) {
            
            // Add extension requirement
            if (!hasExtensionDecl(context, GLSLExtension.ARB_EXPLICIT_ATTRIB_LOCATION)) {
                context.requiredExtensions.add(new GLSLExtensionDecl(
                    GLSLExtension.ARB_EXPLICIT_ATTRIB_LOCATION.name, "require"));
            }
            
            // Keep layout qualifier (extension supports it)
            return null;
        }
        
        // Remove layout qualifier - store for later use by host application
        context.attributeLocations.put(decl.name, location);
        
        GLSLTypeQualifiers newQuals = quals.copy();
        newQuals.layout = null;
        
        decl.type = new GLSLType(decl.type.baseType, decl.type.structName,
                                 decl.type.arrayDimensions, newQuals);
        
        context.addWarning("Layout location for '" + decl.name + 
                          "' must be set via glBindAttribLocation(" + location + ")");
        
        return decl;
    }
    
    private boolean hasExtensionDecl(GLSLTranslationContext context, GLSLExtension ext) {
        for (GLSLExtensionDecl decl : context.requiredExtensions) {
            if (ext.name.equals(decl.extensionName)) {
                return true;
            }
        }
        return false;
    }
}

/**
 * Handles layout(binding=N) for versions that don't support it natively.
 */
final class LayoutBindingDowngradeRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return source.versionNumber >= 420 && target.versionNumber < 420;
    }
    
    @Override
    public int getPriority() { return 70; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.LAYOUT; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLVariableDecl.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLVariableDecl)) return null;
        
        GLSLVariableDecl decl = (GLSLVariableDecl) node;
        GLSLTypeQualifiers quals = decl.type.qualifiers;
        
        if (quals == null || quals.layout == null || quals.layout.binding < 0) {
            return null;
        }
        
        // Try shading_language_420pack extension
        if (context.targetVersion.versionNumber >= 330 &&
            context.hasExtension(GLSLExtension.ARB_SHADING_LANGUAGE_420PACK)) {
            
            if (!hasExtensionDecl(context, GLSLExtension.ARB_SHADING_LANGUAGE_420PACK)) {
                context.requiredExtensions.add(new GLSLExtensionDecl(
                    GLSLExtension.ARB_SHADING_LANGUAGE_420PACK.name, "require"));
            }
            return null;
        }
        
        // Remove binding qualifier - must be set via glUniform1i
        int binding = quals.layout.binding;
        
        GLSLLayoutQualifier newLayout = quals.layout.copy();
        newLayout.binding = -1;
        
        // If layout is now empty, remove it entirely
        if (isLayoutEmpty(newLayout)) {
            GLSLTypeQualifiers newQuals = quals.copy();
            newQuals.layout = null;
            decl.type = new GLSLType(decl.type.baseType, decl.type.structName,
                                     decl.type.arrayDimensions, newQuals);
        } else {
            GLSLTypeQualifiers newQuals = quals.copy();
            newQuals.layout = newLayout;
            decl.type = new GLSLType(decl.type.baseType, decl.type.structName,
                                     decl.type.arrayDimensions, newQuals);
        }
        
        context.addWarning("Binding for '" + decl.name + 
                          "' must be set via glUniform1i to unit " + binding);
        
        return decl;
    }
    
    private boolean isLayoutEmpty(GLSLLayoutQualifier layout) {
        return layout.location < 0 && layout.binding < 0 && layout.offset < 0 &&
               layout.component < 0 && layout.set < 0 &&
               layout.packing == GLSLLayoutQualifier.PackingMode.NONE &&
               layout.matrixLayout == GLSLLayoutQualifier.MatrixLayout.NONE &&
               layout.localSizeX < 0 && layout.localSizeY < 0 && layout.localSizeZ < 0 &&
               layout.primitiveType == null && layout.maxVertices < 0 &&
               layout.imageFormat == null;
    }
    
    private boolean hasExtensionDecl(GLSLTranslationContext context, GLSLExtension ext) {
        for (GLSLExtensionDecl decl : context.requiredExtensions) {
            if (ext.name.equals(decl.extensionName)) {
                return true;
            }
        }
        return false;
    }
}

// ============================================================================
// LEGACY BUILT-IN VARIABLE RULES
// ============================================================================

/**
 * Handles legacy vertex attributes that were removed in GLSL 1.40+
 */
final class LegacyVertexAttributeRule implements GLSLNodeTransformRule {
    
    private final String attributeName;
    private final String attributeType;
    
    LegacyVertexAttributeRule(String name, String type) {
        this.attributeName = name;
        this.attributeType = type;
    }
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        // Upgrading: need to create replacement attribute
        // Downgrading: can use built-in if target supports it
        return shaderType == GLSLShaderType.VERTEX &&
               ((source.versionNumber < 140 && target.versionNumber >= 140) ||
                (source.versionNumber >= 140 && target.versionNumber < 140));
    }
    
    @Override
    public int getPriority() { return 60; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.VARIABLE; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLIdentifierExpr.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLIdentifierExpr)) return null;
        
        GLSLIdentifierExpr id = (GLSLIdentifierExpr) node;
        
        if (!attributeName.equals(id.name)) return null;
        
        if (context.isUpgrade) {
            // Upgrading from legacy - replace with custom attribute
            String newName = attributeName.substring(3); // Remove "gl_" prefix
            newName = Character.toLowerCase(newName.charAt(0)) + newName.substring(1);
            
            id.name = newName;
            
            // Remember to generate attribute declaration
            context.addWarning("Legacy built-in '" + attributeName + 
                              "' replaced with attribute '" + newName + "'");
            
            return id;
        }
        
        // Downgrading - already using built-in name, no change needed
        return null;
    }
}

/**
 * Handles legacy matrix uniforms that were removed in GLSL 1.40+
 */
final class LegacyMatrixRule implements GLSLNodeTransformRule {
    
    private final String matrixName;
    private final String matrixType;
    
    LegacyMatrixRule(String name, String type) {
        this.matrixName = name;
        this.matrixType = type;
    }
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return (source.versionNumber < 140 && target.versionNumber >= 140) ||
               (source.versionNumber >= 140 && target.versionNumber < 140);
    }
    
    @Override
    public int getPriority() { return 60; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.VARIABLE; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLIdentifierExpr.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLIdentifierExpr)) return null;
        
        GLSLIdentifierExpr id = (GLSLIdentifierExpr) node;
        
        if (!matrixName.equals(id.name)) return null;
        
        if (context.isUpgrade) {
            // Create custom uniform name
            String newName = matrixName.substring(3); // Remove "gl_"
            newName = Character.toLowerCase(newName.charAt(0)) + newName.substring(1);
            
            id.name = newName;
            
            context.addWarning("Legacy built-in '" + matrixName + 
                              "' replaced with uniform '" + newName + "'");
            
            return id;
        }
        
        return null;
    }
}

/**
 * Handles legacy varying variables
 */
final class LegacyVaryingRule implements GLSLNodeTransformRule {
    
    private final String varyingName;
    private final String varyingType;
    
    LegacyVaryingRule(String name, String type) {
        this.varyingName = name;
        this.varyingType = type;
    }
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return (source.versionNumber < 130 && target.versionNumber >= 130) ||
               (source.versionNumber >= 130 && target.versionNumber < 130);
    }
    
    @Override
    public int getPriority() { return 60; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.VARIABLE; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLIdentifierExpr.class || 
               nodeType == GLSLSubscriptExpr.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        String name = null;
        
        if (node instanceof GLSLIdentifierExpr) {
            name = ((GLSLIdentifierExpr) node).name;
        } else if (node instanceof GLSLSubscriptExpr) {
            GLSLSubscriptExpr sub = (GLSLSubscriptExpr) node;
            if (sub.array instanceof GLSLIdentifierExpr) {
                name = ((GLSLIdentifierExpr) sub.array).name;
            }
        }
        
        if (!varyingName.equals(name)) return null;
        
        if (context.isUpgrade) {
            // Replace with custom varying
            String newName = varyingName.substring(3);
            newName = Character.toLowerCase(newName.charAt(0)) + newName.substring(1);
            
            if (node instanceof GLSLIdentifierExpr) {
                ((GLSLIdentifierExpr) node).name = newName;
            } else if (node instanceof GLSLSubscriptExpr) {
                GLSLSubscriptExpr sub = (GLSLSubscriptExpr) node;
                ((GLSLIdentifierExpr) sub.array).name = newName;
            }
            
            context.addWarning("Legacy built-in '" + varyingName + 
                              "' replaced with varying '" + newName + "'");
            
            return node;
        }
        
        return null;
    }
}

// ============================================================================
// FEATURE-SPECIFIC RULES
// ============================================================================

/**
 * Handles non-square matrices (mat2x3, mat3x4, etc.) which were added in GLSL 1.20
 */
final class NonSquareMatrixRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        // Only applies when downgrading to 1.10
        return source.versionNumber >= 120 && target.versionNumber < 120;
    }
    
    @Override
    public int getPriority() { return 50; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.TYPE; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLVariableDecl.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLVariableDecl)) return null;
        
        GLSLVariableDecl decl = (GLSLVariableDecl) node;
        GLSLType type = decl.type;
        
        // Check for non-square matrix types
        String typeName = type.baseType.name();
        if (typeName.contains("X") && (typeName.startsWith("MAT") || typeName.startsWith("DMAT"))) {
            context.addWarning("Non-square matrix type '" + typeName + 
                              "' not supported in GLSL 1.10 - translation may be incorrect");
        }
        
        return null;
    }
}

/**
 * Handles double precision types which were added in GLSL 4.00
 */
final class DoublePrecisionRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return source.versionNumber >= 400 && target.versionNumber < 400;
    }
    
    @Override
    public int getPriority() { return 50; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.TYPE; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLVariableDecl.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (!(node instanceof GLSLVariableDecl)) return null;
        
        GLSLVariableDecl decl = (GLSLVariableDecl) node;
        GLSLType type = decl.type;
        
        // Check for double types
        String typeName = type.baseType.name();
        if (typeName.equals("DOUBLE") || typeName.startsWith("DVEC") || typeName.startsWith("DMAT")) {
            // Attempt to downgrade to float
            GLSLType.BaseType newBaseType = downgradeDoubleType(type.baseType);
            
            if (newBaseType != type.baseType) {
                decl.type = new GLSLType(newBaseType, type.structName, 
                                         type.arrayDimensions, type.qualifiers);
                context.addWarning("Double precision type '" + typeName + 
                                  "' downgraded to float - precision may be lost");
                return decl;
            }
        }
        
        return null;
    }
    
    private GLSLType.BaseType downgradeDoubleType(GLSLType.BaseType type) {
        switch (type) {
            case DOUBLE: return GLSLType.BaseType.FLOAT;
            case DVEC2: return GLSLType.BaseType.VEC2;
            case DVEC3: return GLSLType.BaseType.VEC3;
            case DVEC4: return GLSLType.BaseType.VEC4;
            case DMAT2: return GLSLType.BaseType.MAT2;
            case DMAT3: return GLSLType.BaseType.MAT3;
            case DMAT4: return GLSLType.BaseType.MAT4;
            case DMAT2X2: return GLSLType.BaseType.MAT2X2;
            case DMAT2X3: return GLSLType.BaseType.MAT2X3;
            case DMAT2X4: return GLSLType.BaseType.MAT2X4;
            case DMAT3X2: return GLSLType.BaseType.MAT3X2;
            case DMAT3X3: return GLSLType.BaseType.MAT3X3;
            case DMAT3X4: return GLSLType.BaseType.MAT3X4;
            case DMAT4X2: return GLSLType.BaseType.MAT4X2;
            case DMAT4X3: return GLSLType.BaseType.MAT4X3;
            case DMAT4X4: return GLSLType.BaseType.MAT4X4;
            default: return type;
        }
    }
}

/**
 * Handles switch statements which were added in GLSL 1.30
 */
final class SwitchStatementRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return source.versionNumber >= 130 && target.versionNumber < 130;
    }
    
    @Override
    public int getPriority() { return 40; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.FEATURE; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return false; // Handled specially - switch to if-else chain
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        // Switch statement transformation would convert to if-else chain
        // This is complex and would require restructuring the AST
        if (node instanceof GLSLSwitchStmt) {
            context.addWarning("Switch statement not supported in GLSL " + 
                              context.targetVersion.versionString + " - manual conversion required");
        }
        return null;
    }
}

/**
 * Handles bitwise operations which were added in GLSL 1.30
 */
final class BitOperationsRule implements GLSLNodeTransformRule {
    
    @Override
    public boolean applies(GLSLVersion source, GLSLVersion target, GLSLShaderType shaderType) {
        return source.versionNumber >= 130 && target.versionNumber < 130;
    }
    
    @Override
    public int getPriority() { return 40; }
    
    @Override
    public RuleCategory getCategory() { return RuleCategory.FEATURE; }
    
    @Override
    public boolean canTransform(Class<? extends GLSLASTNode> nodeType) {
        return nodeType == GLSLBinaryExpr.class || nodeType == GLSLUnaryExpr.class;
    }
    
    @Override
    public GLSLASTNode transform(GLSLASTNode node, GLSLTranslationContext context) {
        if (node instanceof GLSLBinaryExpr) {
            GLSLBinaryExpr binary = (GLSLBinaryExpr) node;
            switch (binary.operator) {
                case BIT_AND:
                case BIT_OR:
                case BIT_XOR:
                case LEFT_SHIFT:
                case RIGHT_SHIFT:
                    context.addWarning("Bitwise operation '" + binary.operator + 
                                      "' not supported in GLSL " + 
                                      context.targetVersion.versionString);
                    break;
                default:
                    break;
            }
        } else if (node instanceof GLSLUnaryExpr) {
            GLSLUnaryExpr unary = (GLSLUnaryExpr) node;
            if (unary.operator == GLSLUnaryExpr.Operator.BIT_NOT) {
                context.addWarning("Bitwise NOT not supported in GLSL " + 
                                  context.targetVersion.versionString);
            }
        }
        return null;
    }
}

// ============================================================================
// TRANSLATION ENGINE
// ============================================================================

/**
 * Main translation engine that orchestrates the translation process.
 */
final class GLSLTranslationEngine {
    
    private final GLSLCallMapper mapper;
    private final GLSLTranslationRuleRegistry rules;
    
    GLSLTranslationEngine(GLSLCallMapper mapper) {
        this.mapper = mapper;
        this.rules = new GLSLTranslationRuleRegistry();
    }
    
    /**
     * Translate an AST from source version to target version.
     */
    GLSLShaderAST translate(GLSLShaderAST ast, GLSLVersion sourceVersion, 
                            GLSLVersion targetVersion, GLSLShaderType shaderType) {
        
        // No translation needed if versions match
        if (sourceVersion == targetVersion) {
            return ast;
        }
        
        // Create translation context
        GLSLTranslationContext context = new GLSLTranslationContext(
            sourceVersion, targetVersion, shaderType, ast,
            mapper.getMemoryPool(), mapper.getCapabilityRegistry()
        );
        
        // Pre-pass: collect information about the shader
        collectShaderInfo(ast, context);
        
        // Update version
        ast.version = targetVersion;
        
        // Apply translation rules
        GLSLTranslationVisitor visitor = new GLSLTranslationVisitor(context, rules);
        ast.accept(visitor);
        
        // Post-pass: generate required declarations
        generateRequiredDeclarations(ast, context);
        
        // Add extension declarations
        for (GLSLExtensionDecl ext : context.requiredExtensions) {
            ast.extensions.add(0, ext);
            ast.declarations.add(0, ext);
        }
        
        // Copy warnings
        ast.warnings.addAll(context.warnings);
        
        // Add required extensions to shader
        for (GLSLExtensionDecl ext : context.requiredExtensions) {
            GLSLExtension parsed = GLSLExtension.fromString(ext.extensionName);
            if (parsed != null) {
                ast.requiredExtensions.add(parsed);
            }
        }
        
        return ast;
    }
    
    /**
     * Validate an AST against a target version.
     */
    GLSLValidationResult validate(GLSLShaderAST ast, GLSLVersion version, 
                                  GLSLShaderType shaderType) {
        List<GLSLError> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        GLSLValidationVisitor validator = new GLSLValidationVisitor(
            version, shaderType, mapper.getCapabilityRegistry(), errors, warnings
        );
        
        ast.accept(validator);
        
        return new GLSLValidationResult(errors.isEmpty(), errors, warnings);
    }
    
    /**
     * Collect information about the shader for translation.
     */
    private void collectShaderInfo(GLSLShaderAST ast, GLSLTranslationContext context) {
        // Collect sampler types for texture function translation
        for (GLSLVariableDecl var : ast.globalVariables) {
            if (var.type.isSampler()) {
                context.registerSamplerType(var.name, var.type);
            }
            context.globalVariables.put(var.name, var);
        }
        
        // Collect interface block samplers
        for (GLSLInterfaceBlock block : ast.interfaceBlocks) {
            for (GLSLVariableDecl member : block.members) {
                if (member.type.isSampler()) {
                    String fullName = block.instanceName != null ? 
                        block.instanceName + "." + member.name : member.name;
                    context.registerSamplerType(fullName, member.type);
                }
            }
        }
    }
    
    /**
     * Generate declarations required by the translation.
     */
    private void generateRequiredDeclarations(GLSLShaderAST ast, GLSLTranslationContext context) {
        // Generate fragment output declarations for upgrade
        if (context.isUpgrade && context.shaderType == GLSLShaderType.FRAGMENT) {
            if (context.usesFragColor) {
                GLSLVariableDecl output = createFragmentOutput(
                    context.primaryOutputName, 0, context.targetVersion);
                ast.declarations.add(findInsertionPoint(ast), output);
                ast.globalVariables.add(output);
            }
            
            if (context.usesFragData) {
                for (int i = 0; i <= context.maxFragDataIndex; i++) {
                    String outputName = "fragData_" + i;
                    if (!context.usedOutputNames.contains(outputName)) continue;
                    
                    GLSLVariableDecl output = createFragmentOutput(outputName, i, context.targetVersion);
                    ast.declarations.add(findInsertionPoint(ast), output);
                    ast.globalVariables.add(output);
                }
            }
        }
    }
    
    private GLSLVariableDecl createFragmentOutput(String name, int location, GLSLVersion version) {
        GLSLTypeQualifiers quals = new GLSLTypeQualifiers();
        quals.storage = GLSLTypeQualifiers.Storage.OUT;
        
        if (version.versionNumber >= 330) {
            quals.layout = new GLSLLayoutQualifier();
            quals.layout.location = location;
        }
        
        GLSLType type = new GLSLType(GLSLType.BaseType.VEC4, null, null, quals);
        return new GLSLVariableDecl(type, name);
    }
    
    private int findInsertionPoint(GLSLShaderAST ast) {
        // Find position after version, extensions, and precision declarations
        int pos = 0;
        for (GLSLASTNode decl : ast.declarations) {
            if (decl instanceof GLSLExtensionDecl || decl instanceof GLSLPrecisionDecl) {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }
}

/**
 * Visitor that applies translation rules to the AST.
 */
final class GLSLTranslationVisitor extends GLSLASTBaseVisitor {
    
    private final GLSLTranslationContext context;
    private final GLSLTranslationRuleRegistry rules;
    
    GLSLTranslationVisitor(GLSLTranslationContext context, GLSLTranslationRuleRegistry rules) {
        this.context = context;
        this.rules = rules;
    }
    
    @Override
    public void visitVariableDecl(GLSLVariableDecl node) {
        applyNodeRules(node);
        super.visitVariableDecl(node);
    }
    
    @Override
    public void visitCallExpr(GLSLCallExpr node) {
        // First process children
        super.visitCallExpr(node);
        // Then apply rules to this node
        applyNodeRules(node);
    }
    
    @Override
    public void visitIdentifierExpr(GLSLIdentifierExpr node) {
        applyNodeRules(node);
    }
    
    @Override
    public void visitSubscriptExpr(GLSLSubscriptExpr node) {
        // Check if this is gl_FragData access before processing children
        GLSLASTNode result = applyNodeRules(node);
        if (result != node && result != null) {
            // Node was replaced, don't visit children
            return;
        }
        super.visitSubscriptExpr(node);
    }
    
    @Override
    public void visitMemberExpr(GLSLMemberExpr node) {
        super.visitMemberExpr(node);
        applyNodeRules(node);
    }
    
    @Override
    public void visitBinaryExpr(GLSLBinaryExpr node) {
        super.visitBinaryExpr(node);
        applyNodeRules(node);
    }
    
    @Override
    public void visitUnaryExpr(GLSLUnaryExpr node) {
        super.visitUnaryExpr(node);
        applyNodeRules(node);
    }
    
    @Override
    public void visitSwitchStmt(GLSLSwitchStmt node) {
        applyNodeRules(node);
        super.visitSwitchStmt(node);
    }
    
    private GLSLASTNode applyNodeRules(GLSLASTNode node) {
        List<GLSLNodeTransformRule> nodeRules = rules.getNodeRules(node.getClass());
        
        for (GLSLNodeTransformRule rule : nodeRules) {
            if (rule.applies(context.sourceVersion, context.targetVersion, context.shaderType)) {
                GLSLASTNode result = rule.transform(node, context);
                if (result != null && result != node) {
                    // Node was replaced
                    return result;
                }
            }
        }
        
        return node;
    }
}

/**
 * Visitor that validates an AST against a target version.
 */
final class GLSLValidationVisitor extends GLSLASTBaseVisitor {
    
    private final GLSLVersion version;
    private final GLSLShaderType shaderType;
    private final GLSLCapabilityRegistry capabilities;
    private final List<GLSLError> errors;
    private final List<String> warnings;
    
    GLSLValidationVisitor(GLSLVersion version, GLSLShaderType shaderType,
                          GLSLCapabilityRegistry capabilities,
                          List<GLSLError> errors, List<String> warnings) {
        this.version = version;
        this.shaderType = shaderType;
        this.capabilities = capabilities;
        this.errors = errors;
        this.warnings = warnings;
    }
    
    @Override
    public void visitVariableDecl(GLSLVariableDecl node) {
        // Check type compatibility
        validateType(node.type, node.line, node.column);
        
        // Check qualifier compatibility
        if (node.type.qualifiers != null) {
            validateQualifiers(node.type.qualifiers, node.line, node.column);
        }
        
        super.visitVariableDecl(node);
    }
    
    @Override
    public void visitSwitchStmt(GLSLSwitchStmt node) {
        if (!capabilities.isSupported(GLSLFeature.SWITCH_STATEMENT, version)) {
            errors.add(new GLSLError(node.line, node.column,
                "Switch statement not supported in GLSL " + version.versionString,
                GLSLErrorType.UNSUPPORTED_FEATURE));
        }
        super.visitSwitchStmt(node);
    }
    
    @Override
    public void visitBinaryExpr(GLSLBinaryExpr node) {
        // Check bitwise operations
        switch (node.operator) {
            case BIT_AND:
            case BIT_OR:
            case BIT_XOR:
            case LEFT_SHIFT:
            case RIGHT_SHIFT:
                if (!capabilities.isSupported(GLSLFeature.BITWISE_OPERATIONS, version)) {
                    errors.add(new GLSLError(node.line, node.column,
                        "Bitwise operations not supported in GLSL " + version.versionString,
                        GLSLErrorType.UNSUPPORTED_FEATURE));
                }
                break;
            default:
                break;
        }
        super.visitBinaryExpr(node);
    }
    
    private void validateType(GLSLType type, int line, int column) {
        if (type == null) return;
        
        String typeName = type.baseType.name();
        
        // Double precision
        if (typeName.equals("DOUBLE") || typeName.startsWith("DVEC") || typeName.startsWith("DMAT")) {
            if (!capabilities.isSupported(GLSLFeature.DOUBLE_PRECISION, version)) {
                errors.add(new GLSLError(line, column,
                    "Double precision types not supported in GLSL " + version.versionString,
                    GLSLErrorType.UNSUPPORTED_FEATURE));
            }
        }
        
        // Non-square matrices
        if (typeName.contains("X") && (typeName.startsWith("MAT") || typeName.startsWith("DMAT"))) {
            if (!capabilities.isSupported(GLSLFeature.NON_SQUARE_MATRICES, version)) {
                errors.add(new GLSLError(line, column,
                    "Non-square matrices not supported in GLSL " + version.versionString,
                    GLSLErrorType.UNSUPPORTED_FEATURE));
            }
        }
    }
    
    private void validateQualifiers(GLSLTypeQualifiers quals, int line, int column) {
        // Layout qualifiers
        if (quals.layout != null) {
            if (quals.layout.location >= 0 && 
                !capabilities.isSupported(GLSLFeature.LAYOUT_LOCATION_INPUT, version)) {
                warnings.add("Layout location qualifier may require extension in GLSL " + 
                            version.versionString);
            }
            
            if (quals.layout.binding >= 0 &&
                !capabilities.isSupported(GLSLFeature.LAYOUT_BINDING, version)) {
                errors.add(new GLSLError(line, column,
                    "Layout binding qualifier not supported in GLSL " + version.versionString,
                    GLSLErrorType.UNSUPPORTED_FEATURE));
            }
        }
        
        // Storage qualifiers
        if (quals.storage == GLSLTypeQualifiers.Storage.ATTRIBUTE &&
            version.versionNumber >= 130) {
            warnings.add("'attribute' keyword is deprecated in GLSL " + version.versionString);
        }
        
        if (quals.storage == GLSLTypeQualifiers.Storage.VARYING &&
            version.versionNumber >= 130) {
            warnings.add("'varying' keyword is deprecated in GLSL " + version.versionString);
        }
        
        // Interpolation qualifiers
        if (quals.interpolation != GLSLTypeQualifiers.Interpolation.NONE &&
            !capabilities.isSupported(GLSLFeature.FLAT_INTERPOLATION, version)) {
            errors.add(new GLSLError(line, column,
                "Interpolation qualifiers not supported in GLSL " + version.versionString,
                GLSLErrorType.UNSUPPORTED_FEATURE));
        }
    }
}

// ============================================================================
// OPTIMIZATION PASS SYSTEM
// ============================================================================

/**
 * Base interface for optimization passes.
 * Passes are applied in order based on priority and dependencies.
 */
interface GLSLOptimizationPass {
    
    /**
     * Get the name of this pass for debugging/logging.
     */
    String getName();
    
    /**
     * Get the priority of this pass. Higher priority runs first.
     */
    int getPriority();
    
    /**
     * Get the minimum optimization level required for this pass.
     * Level 0 = disabled, 1 = basic, 2 = standard, 3 = aggressive
     */
    int getMinimumLevel();
    
    /**
     * Check if this pass should run.
     */
    boolean shouldRun(GLSLOptimizationContext context);
    
    /**
     * Run the optimization pass on the shader.
     * Returns true if any modifications were made.
     */
    boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context);
}

/**
 * Context for optimization passes containing shared state and utilities.
 */
final class GLSLOptimizationContext {
    
    final GLSLMemoryPool pool;
    final int optimizationLevel;
    final GLSLShaderType shaderType;
    final GLSLVersion targetVersion;
    
    // Usage tracking
    final Map<String, Integer> variableUseCount = new HashMap<>();
    final Map<String, Integer> functionCallCount = new HashMap<>();
    final Set<String> writtenVariables = new HashSet<>();
    final Set<String> readVariables = new HashSet<>();
    
    // Constant values for propagation
    final Map<String, GLSLConstantValue> constantValues = new HashMap<>();
    
    // Statistics
    int constantsFolded = 0;
    int deadCodeRemoved = 0;
    int expressionsSimplified = 0;
    int variablesInlined = 0;
    
    // Iteration control
    int passIterations = 0;
    static final int MAX_ITERATIONS = 10;
    
    GLSLOptimizationContext(GLSLMemoryPool pool, int level, 
                            GLSLShaderType shaderType, GLSLVersion version) {
        this.pool = pool;
        this.optimizationLevel = level;
        this.shaderType = shaderType;
        this.targetVersion = version;
    }
    
    void reset() {
        variableUseCount.clear();
        functionCallCount.clear();
        writtenVariables.clear();
        readVariables.clear();
        constantValues.clear();
    }
    
    void recordVariableUse(String name) {
        variableUseCount.merge(name, 1, Integer::sum);
    }
    
    void recordFunctionCall(String name) {
        functionCallCount.merge(name, 1, Integer::sum);
    }
    
    void recordVariableWrite(String name) {
        writtenVariables.add(name);
    }
    
    void recordVariableRead(String name) {
        readVariables.add(name);
    }
    
    void setConstantValue(String name, GLSLConstantValue value) {
        constantValues.put(name, value);
    }
    
    GLSLConstantValue getConstantValue(String name) {
        return constantValues.get(name);
    }
    
    boolean isUnused(String name) {
        return variableUseCount.getOrDefault(name, 0) == 0;
    }
    
    boolean isWriteOnly(String name) {
        return writtenVariables.contains(name) && !readVariables.contains(name);
    }
    
    boolean shouldContinueIterating() {
        return passIterations++ < MAX_ITERATIONS;
    }
}

/**
 * Represents a constant value that can be propagated.
 */
final class GLSLConstantValue {
    
    enum Type { BOOL, INT, UINT, FLOAT, DOUBLE, VECTOR, MATRIX }
    
    final Type type;
    final int componentCount;
    
    // Storage for different types
    boolean boolValue;
    long intValue;
    double floatValue;
    double[] vectorValues; // For vectors and matrices
    
    private GLSLConstantValue(Type type, int componentCount) {
        this.type = type;
        this.componentCount = componentCount;
    }
    
    static GLSLConstantValue ofBool(boolean value) {
        GLSLConstantValue cv = new GLSLConstantValue(Type.BOOL, 1);
        cv.boolValue = value;
        return cv;
    }
    
    static GLSLConstantValue ofInt(long value) {
        GLSLConstantValue cv = new GLSLConstantValue(Type.INT, 1);
        cv.intValue = value;
        return cv;
    }
    
    static GLSLConstantValue ofFloat(double value) {
        GLSLConstantValue cv = new GLSLConstantValue(Type.FLOAT, 1);
        cv.floatValue = value;
        return cv;
    }
    
    static GLSLConstantValue ofVector(double[] values) {
        GLSLConstantValue cv = new GLSLConstantValue(Type.VECTOR, values.length);
        cv.vectorValues = values.clone();
        return cv;
    }
    
    boolean isZero() {
        switch (type) {
            case BOOL: return !boolValue;
            case INT:
            case UINT: return intValue == 0;
            case FLOAT:
            case DOUBLE: return floatValue == 0.0;
            case VECTOR:
                for (double v : vectorValues) {
                    if (v != 0.0) return false;
                }
                return true;
            default: return false;
        }
    }
    
    boolean isOne() {
        switch (type) {
            case BOOL: return boolValue;
            case INT:
            case UINT: return intValue == 1;
            case FLOAT:
            case DOUBLE: return floatValue == 1.0;
            case VECTOR:
                for (double v : vectorValues) {
                    if (v != 1.0) return false;
                }
                return true;
            default: return false;
        }
    }
    
    GLSLExpression toExpression(GLSLMemoryPool pool) {
        switch (type) {
            case BOOL:
                return new GLSLLiteralExpr(boolValue);
            case INT:
            case UINT:
                return new GLSLLiteralExpr(intValue);
            case FLOAT:
            case DOUBLE:
                return new GLSLLiteralExpr(floatValue);
            default:
                return null; // Vectors handled separately
        }
    }
}

// ============================================================================
// MAIN OPTIMIZER
// ============================================================================

/**
 * Main optimizer that coordinates optimization passes.
 */
final class GLSLOptimizer {
    
    private final GLSLMemoryPool pool;
    private final List<GLSLOptimizationPass> passes;
    
    GLSLOptimizer(GLSLMemoryPool pool) {
        this.pool = pool;
        this.passes = new ArrayList<>();
        
        // Register passes in priority order
        registerPasses();
        
        // Sort by priority (descending)
        passes.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }
    
    private void registerPasses() {
        // Analysis passes (run first)
        passes.add(new UsageAnalysisPass());
        passes.add(new ConstantAnalysisPass());
        
        // Optimization passes
        passes.add(new ConstantFoldingPass());
        passes.add(new ConstantPropagationPass());
        passes.add(new AlgebraicSimplificationPass());
        passes.add(new StrengthReductionPass());
        passes.add(new DeadCodeEliminationPass());
        passes.add(new DeadBranchEliminationPass());
        passes.add(new UnreachableCodeEliminationPass());
        passes.add(new VariableInliningPass());
        passes.add(new CommonSubexpressionPass());
        
        // Shader-specific passes
        passes.add(new PrecisionOptimizationPass());
        passes.add(new SwizzleOptimizationPass());
        passes.add(new VectorConstructorOptimizationPass());
        
        // Cleanup passes (run last)
        passes.add(new EmptyBlockRemovalPass());
        passes.add(new DeclarationCleanupPass());
    }
    
    /**
     * Optimize the shader AST.
     */
    GLSLShaderAST optimize(GLSLShaderAST shader, int level) {
        if (level <= 0) {
            return shader;
        }
        
        GLSLOptimizationContext context = new GLSLOptimizationContext(
            pool, level, shader.shaderType, shader.version
        );
        
        boolean changed;
        do {
            changed = false;
            context.reset();
            
            for (GLSLOptimizationPass pass : passes) {
                if (pass.getMinimumLevel() <= level && pass.shouldRun(context)) {
                    boolean passChanged = pass.optimize(shader, context);
                    changed = changed || passChanged;
                }
            }
            
        } while (changed && context.shouldContinueIterating());
        
        return shader;
    }
}

// ============================================================================
// ANALYSIS PASSES
// ============================================================================

/**
 * Analyzes variable and function usage throughout the shader.
 */
final class UsageAnalysisPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "UsageAnalysis"; }
    @Override public int getPriority() { return 1000; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        UsageVisitor visitor = new UsageVisitor(context);
        shader.accept(visitor);
        return false; // Analysis only, no modifications
    }
    
    private static class UsageVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        private boolean inLValue = false;
        
        UsageVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitIdentifierExpr(GLSLIdentifierExpr node) {
            context.recordVariableUse(node.name);
            if (inLValue) {
                context.recordVariableWrite(node.name);
            } else {
                context.recordVariableRead(node.name);
            }
        }
        
        @Override
        public void visitCallExpr(GLSLCallExpr node) {
            context.recordFunctionCall(node.functionName);
            super.visitCallExpr(node);
        }
        
        @Override
        public void visitBinaryExpr(GLSLBinaryExpr node) {
            // Handle assignment LHS
            if (node.operator.isAssignment()) {
                inLValue = true;
                node.left.accept(this);
                inLValue = false;
                node.right.accept(this);
            } else {
                super.visitBinaryExpr(node);
            }
        }
        
        @Override
        public void visitUnaryExpr(GLSLUnaryExpr node) {
            if (node.operator == GLSLUnaryExpr.Operator.PRE_INCREMENT ||
                node.operator == GLSLUnaryExpr.Operator.PRE_DECREMENT ||
                node.operator == GLSLUnaryExpr.Operator.POST_INCREMENT ||
                node.operator == GLSLUnaryExpr.Operator.POST_DECREMENT) {
                inLValue = true;
                node.operand.accept(this);
                inLValue = false;
                // Also counts as a read
                context.recordVariableRead(getIdentifierName(node.operand));
            } else {
                super.visitUnaryExpr(node);
            }
        }
        
        private String getIdentifierName(GLSLExpression expr) {
            if (expr instanceof GLSLIdentifierExpr) {
                return ((GLSLIdentifierExpr) expr).name;
            }
            return null;
        }
    }
}

/**
 * Analyzes constant values for propagation.
 */
final class ConstantAnalysisPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "ConstantAnalysis"; }
    @Override public int getPriority() { return 999; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        // Find const variables with literal initializers
        for (GLSLVariableDecl var : shader.globalVariables) {
            if (isConstant(var)) {
                GLSLConstantValue value = evaluateConstant(var.initializer);
                if (value != null) {
                    context.setConstantValue(var.name, value);
                }
            }
        }
        
        // Also analyze function-local const variables
        ConstantVisitor visitor = new ConstantVisitor(context);
        shader.accept(visitor);
        
        return false; // Analysis only
    }
    
    private boolean isConstant(GLSLVariableDecl var) {
        return var.type.qualifiers != null && 
               var.type.qualifiers.storage == GLSLTypeQualifiers.Storage.CONST &&
               var.initializer != null;
    }
    
    private GLSLConstantValue evaluateConstant(GLSLExpression expr) {
        if (expr instanceof GLSLLiteralExpr) {
            GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
            switch (lit.literalType) {
                case BOOL: return GLSLConstantValue.ofBool(lit.boolValue);
                case INT:
                case UINT: return GLSLConstantValue.ofInt(lit.intValue);
                case FLOAT:
                case DOUBLE: return GLSLConstantValue.ofFloat(lit.floatValue);
            }
        }
        return null;
    }
    
    private static class ConstantVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        
        ConstantVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitDeclarationStmt(GLSLDeclarationStmt node) {
            for (GLSLVariableDecl decl : node.declarations) {
                if (decl.type.qualifiers != null &&
                    decl.type.qualifiers.storage == GLSLTypeQualifiers.Storage.CONST &&
                    decl.initializer instanceof GLSLLiteralExpr) {
                    
                    GLSLLiteralExpr lit = (GLSLLiteralExpr) decl.initializer;
                    GLSLConstantValue value = null;
                    
                    switch (lit.literalType) {
                        case BOOL: value = GLSLConstantValue.ofBool(lit.boolValue); break;
                        case INT:
                        case UINT: value = GLSLConstantValue.ofInt(lit.intValue); break;
                        case FLOAT:
                        case DOUBLE: value = GLSLConstantValue.ofFloat(lit.floatValue); break;
                    }
                    
                    if (value != null) {
                        context.setConstantValue(decl.name, value);
                    }
                }
            }
            super.visitDeclarationStmt(node);
        }
    }
}

// ============================================================================
// CONSTANT FOLDING PASS
// ============================================================================

/**
 * Evaluates constant expressions at compile time.
 */
final class ConstantFoldingPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "ConstantFolding"; }
    @Override public int getPriority() { return 900; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        ConstantFoldingVisitor visitor = new ConstantFoldingVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class ConstantFoldingVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        ConstantFoldingVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitBinaryExpr(GLSLBinaryExpr node) {
            // First fold children
            super.visitBinaryExpr(node);
            
            // Try to fold this expression
            if (node.left instanceof GLSLLiteralExpr && node.right instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr left = (GLSLLiteralExpr) node.left;
                GLSLLiteralExpr right = (GLSLLiteralExpr) node.right;
                
                GLSLLiteralExpr result = foldBinary(node.operator, left, right);
                if (result != null) {
                    replaceInParent(node, result);
                    context.constantsFolded++;
                    changed = true;
                }
            }
        }
        
        @Override
        public void visitUnaryExpr(GLSLUnaryExpr node) {
            super.visitUnaryExpr(node);
            
            if (node.operand instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr operand = (GLSLLiteralExpr) node.operand;
                
                GLSLLiteralExpr result = foldUnary(node.operator, operand);
                if (result != null) {
                    replaceInParent(node, result);
                    context.constantsFolded++;
                    changed = true;
                }
            }
        }
        
        @Override
        public void visitTernaryExpr(GLSLTernaryExpr node) {
            super.visitTernaryExpr(node);
            
            if (node.condition instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr cond = (GLSLLiteralExpr) node.condition;
                if (cond.literalType == GLSLLiteralExpr.LiteralType.BOOL) {
                    GLSLExpression result = cond.boolValue ? node.thenExpr : node.elseExpr;
                    replaceInParent(node, result);
                    context.constantsFolded++;
                    changed = true;
                }
            }
        }
        
        @Override
        public void visitCallExpr(GLSLCallExpr node) {
            super.visitCallExpr(node);
            
            // Try to fold built-in function calls with constant arguments
            if (node.isBuiltinFunction && allArgsConstant(node)) {
                GLSLLiteralExpr result = foldBuiltinCall(node);
                if (result != null) {
                    replaceInParent(node, result);
                    context.constantsFolded++;
                    changed = true;
                }
            }
        }
        
        private GLSLLiteralExpr foldBinary(GLSLBinaryExpr.Operator op, 
                                           GLSLLiteralExpr left, GLSLLiteralExpr right) {
            // Integer operations
            if (isIntegral(left) && isIntegral(right)) {
                long l = left.intValue;
                long r = right.intValue;
                
                switch (op) {
                    case ADD: return literalInt(l + r);
                    case SUB: return literalInt(l - r);
                    case MUL: return literalInt(l * r);
                    case DIV: return r != 0 ? literalInt(l / r) : null;
                    case MOD: return r != 0 ? literalInt(l % r) : null;
                    case BIT_AND: return literalInt(l & r);
                    case BIT_OR: return literalInt(l | r);
                    case BIT_XOR: return literalInt(l ^ r);
                    case LEFT_SHIFT: return literalInt(l << r);
                    case RIGHT_SHIFT: return literalInt(l >> r);
                    case EQ: return literalBool(l == r);
                    case NE: return literalBool(l != r);
                    case LT: return literalBool(l < r);
                    case GT: return literalBool(l > r);
                    case LE: return literalBool(l <= r);
                    case GE: return literalBool(l >= r);
                    default: break;
                }
            }
            
            // Float operations
            if (isFloating(left) || isFloating(right)) {
                double l = toDouble(left);
                double r = toDouble(right);
                
                switch (op) {
                    case ADD: return literalFloat(l + r);
                    case SUB: return literalFloat(l - r);
                    case MUL: return literalFloat(l * r);
                    case DIV: return r != 0 ? literalFloat(l / r) : null;
                    case EQ: return literalBool(l == r);
                    case NE: return literalBool(l != r);
                    case LT: return literalBool(l < r);
                    case GT: return literalBool(l > r);
                    case LE: return literalBool(l <= r);
                    case GE: return literalBool(l >= r);
                    default: break;
                }
            }
            
            // Boolean operations
            if (isBool(left) && isBool(right)) {
                boolean l = left.boolValue;
                boolean r = right.boolValue;
                
                switch (op) {
                    case AND: return literalBool(l && r);
                    case OR: return literalBool(l || r);
                    case XOR: return literalBool(l ^ r);
                    case EQ: return literalBool(l == r);
                    case NE: return literalBool(l != r);
                    default: break;
                }
            }
            
            return null;
        }
        
        private GLSLLiteralExpr foldUnary(GLSLUnaryExpr.Operator op, GLSLLiteralExpr operand) {
            switch (op) {
                case MINUS:
                    if (isIntegral(operand)) {
                        return literalInt(-operand.intValue);
                    } else if (isFloating(operand)) {
                        return literalFloat(-operand.floatValue);
                    }
                    break;
                    
                case PLUS:
                    return operand; // No change
                    
                case NOT:
                    if (isBool(operand)) {
                        return literalBool(!operand.boolValue);
                    }
                    break;
                    
                case BIT_NOT:
                    if (isIntegral(operand)) {
                        return literalInt(~operand.intValue);
                    }
                    break;
                    
                default:
                    break;
            }
            return null;
        }
        
        private GLSLLiteralExpr foldBuiltinCall(GLSLCallExpr call) {
            String func = call.functionName;
            List<GLSLExpression> args = call.arguments;
            
            // Single-argument math functions
            if (args.size() == 1 && args.get(0) instanceof GLSLLiteralExpr) {
                double x = toDouble((GLSLLiteralExpr) args.get(0));
                
                switch (func) {
                    case "abs": return literalFloat(Math.abs(x));
                    case "sign": return literalFloat(Math.signum(x));
                    case "floor": return literalFloat(Math.floor(x));
                    case "ceil": return literalFloat(Math.ceil(x));
                    case "fract": return literalFloat(x - Math.floor(x));
                    case "round": return literalFloat(Math.round(x));
                    case "trunc": return literalFloat((long) x);
                    case "sin": return literalFloat(Math.sin(x));
                    case "cos": return literalFloat(Math.cos(x));
                    case "tan": return literalFloat(Math.tan(x));
                    case "asin": return literalFloat(Math.asin(x));
                    case "acos": return literalFloat(Math.acos(x));
                    case "atan": return literalFloat(Math.atan(x));
                    case "sinh": return literalFloat(Math.sinh(x));
                    case "cosh": return literalFloat(Math.cosh(x));
                    case "tanh": return literalFloat(Math.tanh(x));
                    case "exp": return literalFloat(Math.exp(x));
                    case "log": return x > 0 ? literalFloat(Math.log(x)) : null;
                    case "exp2": return literalFloat(Math.pow(2, x));
                    case "log2": return x > 0 ? literalFloat(Math.log(x) / Math.log(2)) : null;
                    case "sqrt": return x >= 0 ? literalFloat(Math.sqrt(x)) : null;
                    case "inversesqrt": return x > 0 ? literalFloat(1.0 / Math.sqrt(x)) : null;
                    case "radians": return literalFloat(Math.toRadians(x));
                    case "degrees": return literalFloat(Math.toDegrees(x));
                }
            }
            
            // Two-argument math functions
            if (args.size() == 2 && 
                args.get(0) instanceof GLSLLiteralExpr &&
                args.get(1) instanceof GLSLLiteralExpr) {
                
                double x = toDouble((GLSLLiteralExpr) args.get(0));
                double y = toDouble((GLSLLiteralExpr) args.get(1));
                
                switch (func) {
                    case "pow": return literalFloat(Math.pow(x, y));
                    case "mod": return y != 0 ? literalFloat(x - y * Math.floor(x / y)) : null;
                    case "min": return literalFloat(Math.min(x, y));
                    case "max": return literalFloat(Math.max(x, y));
                    case "atan": return literalFloat(Math.atan2(x, y));
                    case "step": return literalFloat(x >= y ? 1.0 : 0.0);
                    case "distance": return literalFloat(Math.abs(x - y));
                }
            }
            
            // Three-argument math functions
            if (args.size() == 3 &&
                args.get(0) instanceof GLSLLiteralExpr &&
                args.get(1) instanceof GLSLLiteralExpr &&
                args.get(2) instanceof GLSLLiteralExpr) {
                
                double x = toDouble((GLSLLiteralExpr) args.get(0));
                double y = toDouble((GLSLLiteralExpr) args.get(1));
                double z = toDouble((GLSLLiteralExpr) args.get(2));
                
                switch (func) {
                    case "clamp": return literalFloat(Math.max(y, Math.min(z, x)));
                    case "mix": return literalFloat(x * (1 - z) + y * z);
                    case "smoothstep": {
                        double t = Math.max(0, Math.min(1, (z - x) / (y - x)));
                        return literalFloat(t * t * (3 - 2 * t));
                    }
                    case "fma": return literalFloat(x * y + z);
                }
            }
            
            return null;
        }
        
        private boolean allArgsConstant(GLSLCallExpr call) {
            for (GLSLExpression arg : call.arguments) {
                if (!(arg instanceof GLSLLiteralExpr)) {
                    return false;
                }
            }
            return true;
        }
        
        private boolean isIntegral(GLSLLiteralExpr lit) {
            return lit.literalType == GLSLLiteralExpr.LiteralType.INT ||
                   lit.literalType == GLSLLiteralExpr.LiteralType.UINT;
        }
        
        private boolean isFloating(GLSLLiteralExpr lit) {
            return lit.literalType == GLSLLiteralExpr.LiteralType.FLOAT ||
                   lit.literalType == GLSLLiteralExpr.LiteralType.DOUBLE;
        }
        
        private boolean isBool(GLSLLiteralExpr lit) {
            return lit.literalType == GLSLLiteralExpr.LiteralType.BOOL;
        }
        
        private double toDouble(GLSLLiteralExpr lit) {
            if (isIntegral(lit)) return lit.intValue;
            return lit.floatValue;
        }
        
        private GLSLLiteralExpr literalInt(long value) {
            GLSLLiteralExpr lit = new GLSLLiteralExpr(value);
            lit.isConstant = true;
            return lit;
        }
        
        private GLSLLiteralExpr literalFloat(double value) {
            GLSLLiteralExpr lit = new GLSLLiteralExpr(value);
            lit.isConstant = true;
            return lit;
        }
        
        private GLSLLiteralExpr literalBool(boolean value) {
            GLSLLiteralExpr lit = new GLSLLiteralExpr(value);
            lit.isConstant = true;
            return lit;
        }
        
        private void replaceInParent(GLSLASTNode oldNode, GLSLASTNode newNode) {
            GLSLASTNode parent = oldNode.parent;
            if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChild(i) == oldNode) {
                        parent.setChild(i, newNode);
                        break;
                    }
                }
            }
        }
    }
}

// ============================================================================
// CONSTANT PROPAGATION PASS
// ============================================================================

/**
 * Replaces variable references with known constant values.
 */
final class ConstantPropagationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "ConstantPropagation"; }
    @Override public int getPriority() { return 850; }
    @Override public int getMinimumLevel() { return 2; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { 
        return !context.constantValues.isEmpty();
    }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        ConstantPropagationVisitor visitor = new ConstantPropagationVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class ConstantPropagationVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        ConstantPropagationVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitIdentifierExpr(GLSLIdentifierExpr node) {
            GLSLConstantValue value = context.getConstantValue(node.name);
            if (value != null) {
                GLSLExpression replacement = value.toExpression(context.pool);
                if (replacement != null) {
                    replaceInParent(node, replacement);
                    changed = true;
                }
            }
        }
        
        private void replaceInParent(GLSLASTNode oldNode, GLSLASTNode newNode) {
            GLSLASTNode parent = oldNode.parent;
            if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChild(i) == oldNode) {
                        parent.setChild(i, newNode);
                        break;
                    }
                }
            }
        }
    }
}

// ============================================================================
// ALGEBRAIC SIMPLIFICATION PASS
// ============================================================================

/**
 * Simplifies expressions using algebraic identities.
 */
final class AlgebraicSimplificationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "AlgebraicSimplification"; }
    @Override public int getPriority() { return 800; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        AlgebraicVisitor visitor = new AlgebraicVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class AlgebraicVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        AlgebraicVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitBinaryExpr(GLSLBinaryExpr node) {
            super.visitBinaryExpr(node);
            
            GLSLExpression result = simplify(node);
            if (result != null && result != node) {
                replaceInParent(node, result);
                context.expressionsSimplified++;
                changed = true;
            }
        }
        
        @Override
        public void visitUnaryExpr(GLSLUnaryExpr node) {
            super.visitUnaryExpr(node);
            
            // Double negation: --x -> x (for non-increment/decrement)
            if (node.operator == GLSLUnaryExpr.Operator.MINUS) {
                if (node.operand instanceof GLSLUnaryExpr) {
                    GLSLUnaryExpr inner = (GLSLUnaryExpr) node.operand;
                    if (inner.operator == GLSLUnaryExpr.Operator.MINUS) {
                        replaceInParent(node, inner.operand);
                        context.expressionsSimplified++;
                        changed = true;
                    }
                }
            }
            
            // Double logical not: !!x -> x
            if (node.operator == GLSLUnaryExpr.Operator.NOT) {
                if (node.operand instanceof GLSLUnaryExpr) {
                    GLSLUnaryExpr inner = (GLSLUnaryExpr) node.operand;
                    if (inner.operator == GLSLUnaryExpr.Operator.NOT) {
                        replaceInParent(node, inner.operand);
                        context.expressionsSimplified++;
                        changed = true;
                    }
                }
            }
        }
        
        private GLSLExpression simplify(GLSLBinaryExpr expr) {
            boolean leftIsZero = isZero(expr.left);
            boolean rightIsZero = isZero(expr.right);
            boolean leftIsOne = isOne(expr.left);
            boolean rightIsOne = isOne(expr.right);
            
            switch (expr.operator) {
                // Addition identities
                case ADD:
                    if (leftIsZero) return expr.right;  // 0 + x -> x
                    if (rightIsZero) return expr.left;  // x + 0 -> x
                    break;
                
                // Subtraction identities
                case SUB:
                    if (rightIsZero) return expr.left;  // x - 0 -> x
                    if (leftIsZero) {
                        // 0 - x -> -x
                        return new GLSLUnaryExpr(GLSLUnaryExpr.Operator.MINUS, expr.right);
                    }
                    if (areEqual(expr.left, expr.right)) {
                        // x - x -> 0
                        return new GLSLLiteralExpr(0L);
                    }
                    break;
                
                // Multiplication identities
                case MUL:
                    if (leftIsZero || rightIsZero) {
                        // 0 * x or x * 0 -> 0
                        return new GLSLLiteralExpr(0.0);
                    }
                    if (leftIsOne) return expr.right;  // 1 * x -> x
                    if (rightIsOne) return expr.left;  // x * 1 -> x
                    // Check for -1 * x -> -x
                    if (isNegOne(expr.left)) {
                        return new GLSLUnaryExpr(GLSLUnaryExpr.Operator.MINUS, expr.right);
                    }
                    if (isNegOne(expr.right)) {
                        return new GLSLUnaryExpr(GLSLUnaryExpr.Operator.MINUS, expr.left);
                    }
                    break;
                
                // Division identities
                case DIV:
                    if (rightIsOne) return expr.left;  // x / 1 -> x
                    if (leftIsZero) {
                        // 0 / x -> 0 (assuming x != 0)
                        return new GLSLLiteralExpr(0.0);
                    }
                    if (areEqual(expr.left, expr.right)) {
                        // x / x -> 1 (assuming x != 0)
                        return new GLSLLiteralExpr(1.0);
                    }
                    break;
                
                // Modulo identities
                case MOD:
                    if (rightIsOne) {
                        // x % 1 -> 0 (for floats this is fract behavior)
                        return new GLSLLiteralExpr(0.0);
                    }
                    if (leftIsZero) {
                        // 0 % x -> 0
                        return new GLSLLiteralExpr(0.0);
                    }
                    break;
                
                // Logical AND identities
                case AND:
                    if (isFalse(expr.left) || isFalse(expr.right)) {
                        return new GLSLLiteralExpr(false);
                    }
                    if (isTrue(expr.left)) return expr.right;
                    if (isTrue(expr.right)) return expr.left;
                    break;
                
                // Logical OR identities
                case OR:
                    if (isTrue(expr.left) || isTrue(expr.right)) {
                        return new GLSLLiteralExpr(true);
                    }
                    if (isFalse(expr.left)) return expr.right;
                    if (isFalse(expr.right)) return expr.left;
                    break;
                
                // Bitwise AND identities
                case BIT_AND:
                    if (leftIsZero || rightIsZero) {
                        return new GLSLLiteralExpr(0L);
                    }
                    if (isAllOnes(expr.left)) return expr.right;
                    if (isAllOnes(expr.right)) return expr.left;
                    break;
                
                // Bitwise OR identities
                case BIT_OR:
                    if (leftIsZero) return expr.right;
                    if (rightIsZero) return expr.left;
                    break;
                
                // Bitwise XOR identities
                case BIT_XOR:
                    if (leftIsZero) return expr.right;
                    if (rightIsZero) return expr.left;
                    if (areEqual(expr.left, expr.right)) {
                        return new GLSLLiteralExpr(0L);
                    }
                    break;
                
                // Shift identities
                case LEFT_SHIFT:
                case RIGHT_SHIFT:
                    if (rightIsZero) return expr.left;  // x << 0 or x >> 0 -> x
                    if (leftIsZero) {
                        return new GLSLLiteralExpr(0L);  // 0 << n or 0 >> n -> 0
                    }
                    break;
                
                default:
                    break;
            }
            
            return null;
        }
        
        private boolean isZero(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                switch (lit.literalType) {
                    case INT:
                    case UINT: return lit.intValue == 0;
                    case FLOAT:
                    case DOUBLE: return lit.floatValue == 0.0;
                    default: return false;
                }
            }
            return false;
        }
        
        private boolean isOne(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                switch (lit.literalType) {
                    case INT:
                    case UINT: return lit.intValue == 1;
                    case FLOAT:
                    case DOUBLE: return lit.floatValue == 1.0;
                    default: return false;
                }
            }
            return false;
        }
        
        private boolean isNegOne(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                switch (lit.literalType) {
                    case INT: return lit.intValue == -1;
                    case FLOAT:
                    case DOUBLE: return lit.floatValue == -1.0;
                    default: return false;
                }
            }
            return false;
        }
        
        private boolean isTrue(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                return lit.literalType == GLSLLiteralExpr.LiteralType.BOOL && lit.boolValue;
            }
            return false;
        }
        
        private boolean isFalse(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                return lit.literalType == GLSLLiteralExpr.LiteralType.BOOL && !lit.boolValue;
            }
            return false;
        }
        
        private boolean isAllOnes(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                if (lit.literalType == GLSLLiteralExpr.LiteralType.INT ||
                    lit.literalType == GLSLLiteralExpr.LiteralType.UINT) {
                    return lit.intValue == -1L || lit.intValue == 0xFFFFFFFFL;
                }
            }
            return false;
        }
        
        private boolean areEqual(GLSLExpression a, GLSLExpression b) {
            // Simple structural equality check
            if (a instanceof GLSLIdentifierExpr && b instanceof GLSLIdentifierExpr) {
                return ((GLSLIdentifierExpr) a).name.equals(((GLSLIdentifierExpr) b).name);
            }
            return false;
        }
        
        private void replaceInParent(GLSLASTNode oldNode, GLSLASTNode newNode) {
            GLSLASTNode parent = oldNode.parent;
            if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChild(i) == oldNode) {
                        parent.setChild(i, newNode);
                        break;
                    }
                }
            }
        }
    }
}

// ============================================================================
// STRENGTH REDUCTION PASS
// ============================================================================

/**
 * Replaces expensive operations with cheaper equivalents.
 */
final class StrengthReductionPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "StrengthReduction"; }
    @Override public int getPriority() { return 750; }
    @Override public int getMinimumLevel() { return 2; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        StrengthReductionVisitor visitor = new StrengthReductionVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class StrengthReductionVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        StrengthReductionVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitBinaryExpr(GLSLBinaryExpr node) {
            super.visitBinaryExpr(node);
            
            // x * 2 -> x + x (addition is often faster)
            if (node.operator == GLSLBinaryExpr.Operator.MUL) {
                if (isTwo(node.right)) {
                    GLSLBinaryExpr add = new GLSLBinaryExpr(
                        GLSLBinaryExpr.Operator.ADD, 
                        node.left.copy(context.pool), 
                        node.left.copy(context.pool)
                    );
                    replaceInParent(node, add);
                    context.expressionsSimplified++;
                    changed = true;
                    return;
                }
                if (isTwo(node.left)) {
                    GLSLBinaryExpr add = new GLSLBinaryExpr(
                        GLSLBinaryExpr.Operator.ADD, 
                        node.right.copy(context.pool), 
                        node.right.copy(context.pool)
                    );
                    replaceInParent(node, add);
                    context.expressionsSimplified++;
                    changed = true;
                    return;
                }
                
                // x * power-of-2 -> x << log2(power) (for integers)
                int rightShift = getPowerOfTwo(node.right);
                if (rightShift > 0 && isIntegerExpr(node.left)) {
                    GLSLBinaryExpr shift = new GLSLBinaryExpr(
                        GLSLBinaryExpr.Operator.LEFT_SHIFT,
                        node.left,
                        new GLSLLiteralExpr((long) rightShift)
                    );
                    replaceInParent(node, shift);
                    context.expressionsSimplified++;
                    changed = true;
                    return;
                }
            }
            
            // x / power-of-2 -> x >> log2(power) (for integers)
            if (node.operator == GLSLBinaryExpr.Operator.DIV) {
                int rightShift = getPowerOfTwo(node.right);
                if (rightShift > 0 && isIntegerExpr(node.left)) {
                    GLSLBinaryExpr shift = new GLSLBinaryExpr(
                        GLSLBinaryExpr.Operator.RIGHT_SHIFT,
                        node.left,
                        new GLSLLiteralExpr((long) rightShift)
                    );
                    replaceInParent(node, shift);
                    context.expressionsSimplified++;
                    changed = true;
                    return;
                }
                
                // x / c -> x * (1/c) for float constants
                if (isFloatConstant(node.right)) {
                    double c = ((GLSLLiteralExpr) node.right).floatValue;
                    if (c != 0.0) {
                        GLSLBinaryExpr mul = new GLSLBinaryExpr(
                            GLSLBinaryExpr.Operator.MUL,
                            node.left,
                            new GLSLLiteralExpr(1.0 / c)
                        );
                        replaceInParent(node, mul);
                        context.expressionsSimplified++;
                        changed = true;
                    }
                }
            }
        }
        
        @Override
        public void visitCallExpr(GLSLCallExpr node) {
            super.visitCallExpr(node);
            
            // pow(x, 2) -> x * x
            if ("pow".equals(node.functionName) && node.arguments.size() == 2) {
                if (isTwo(node.arguments.get(1))) {
                    GLSLExpression x = node.arguments.get(0);
                    GLSLBinaryExpr mul = new GLSLBinaryExpr(
                        GLSLBinaryExpr.Operator.MUL,
                        x.copy(context.pool),
                        x.copy(context.pool)
                    );
                    replaceInParent(node, mul);
                    context.expressionsSimplified++;
                    changed = true;
                    return;
                }
                
                // pow(x, 0.5) -> sqrt(x)
                if (isHalf(node.arguments.get(1))) {
                    GLSLCallExpr sqrt = new GLSLCallExpr("sqrt");
                    sqrt.addArgument(node.arguments.get(0));
                    sqrt.isBuiltinFunction = true;
                    replaceInParent(node, sqrt);
                    context.expressionsSimplified++;
                    changed = true;
                    return;
                }
            }
            
            // length(v) == 0 comparisons could use dot(v,v) == 0 (avoids sqrt)
            // But this is a semantic change that might not be desired
        }
        
        private boolean isTwo(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                switch (lit.literalType) {
                    case INT:
                    case UINT: return lit.intValue == 2;
                    case FLOAT:
                    case DOUBLE: return lit.floatValue == 2.0;
                    default: return false;
                }
            }
            return false;
        }
        
        private boolean isHalf(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                if (lit.literalType == GLSLLiteralExpr.LiteralType.FLOAT ||
                    lit.literalType == GLSLLiteralExpr.LiteralType.DOUBLE) {
                    return lit.floatValue == 0.5;
                }
            }
            return false;
        }
        
        private int getPowerOfTwo(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                if (lit.literalType == GLSLLiteralExpr.LiteralType.INT ||
                    lit.literalType == GLSLLiteralExpr.LiteralType.UINT) {
                    long val = lit.intValue;
                    if (val > 0 && (val & (val - 1)) == 0) {
                        return Long.numberOfTrailingZeros(val);
                    }
                }
            }
            return -1;
        }
        
        private boolean isIntegerExpr(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                return lit.literalType == GLSLLiteralExpr.LiteralType.INT ||
                       lit.literalType == GLSLLiteralExpr.LiteralType.UINT;
            }
            // Could check resolved type, but for safety assume false
            return false;
        }
        
        private boolean isFloatConstant(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                return lit.literalType == GLSLLiteralExpr.LiteralType.FLOAT ||
                       lit.literalType == GLSLLiteralExpr.LiteralType.DOUBLE;
            }
            return false;
        }
        
        private void replaceInParent(GLSLASTNode oldNode, GLSLASTNode newNode) {
            GLSLASTNode parent = oldNode.parent;
            if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChild(i) == oldNode) {
                        parent.setChild(i, newNode);
                        break;
                    }
                }
            }
        }
    }
}

// ============================================================================
// DEAD CODE ELIMINATION PASSES
// ============================================================================

/**
 * Removes unused variable declarations.
 */
final class DeadCodeEliminationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "DeadCodeElimination"; }
    @Override public int getPriority() { return 700; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        boolean changed = false;
        
        // Remove unused global variables (except uniforms, inputs, outputs)
        Iterator<GLSLVariableDecl> globalIter = shader.globalVariables.iterator();
        while (globalIter.hasNext()) {
            GLSLVariableDecl var = globalIter.next();
            if (canRemove(var, context)) {
                globalIter.remove();
                shader.declarations.remove(var);
                context.deadCodeRemoved++;
                changed = true;
            }
        }
        
        // Remove unused local variables
        DeadLocalVisitor visitor = new DeadLocalVisitor(context);
        shader.accept(visitor);
        changed = changed || visitor.changed;
        
        // Remove unused functions (except main)
        Iterator<GLSLFunctionDecl> funcIter = shader.functions.iterator();
        while (funcIter.hasNext()) {
            GLSLFunctionDecl func = funcIter.next();
            if (!func.isMain() && 
                context.functionCallCount.getOrDefault(func.name, 0) == 0) {
                funcIter.remove();
                shader.declarations.remove(func);
                context.deadCodeRemoved++;
                changed = true;
            }
        }
        
        return changed;
    }
    
    private boolean canRemove(GLSLVariableDecl var, GLSLOptimizationContext context) {
        // Don't remove if used
        if (context.variableUseCount.getOrDefault(var.name, 0) > 0) {
            return false;
        }
        
        // Don't remove uniforms, inputs, outputs, etc.
        if (var.type.qualifiers != null) {
            switch (var.type.qualifiers.storage) {
                case UNIFORM:
                case IN:
                case OUT:
                case ATTRIBUTE:
                case VARYING:
                case BUFFER:
                case SHARED:
                    return false;
                default:
                    break;
            }
        }
        
        // Check for side effects in initializer
        if (var.initializer != null && hasSideEffects(var.initializer)) {
            return false;
        }
        
        return true;
    }
    
    private boolean hasSideEffects(GLSLExpression expr) {
        if (expr instanceof GLSLCallExpr) {
            // Function calls might have side effects
            GLSLCallExpr call = (GLSLCallExpr) expr;
            // Built-in math functions are pure
            if (call.isBuiltinFunction && isPureBuiltin(call.functionName)) {
                // Check arguments
                for (GLSLExpression arg : call.arguments) {
                    if (hasSideEffects(arg)) return true;
                }
                return false;
            }
            return true; // Assume user functions have side effects
        }
        
        if (expr instanceof GLSLBinaryExpr) {
            GLSLBinaryExpr binary = (GLSLBinaryExpr) expr;
            if (binary.operator.isAssignment()) {
                return true;
            }
            return hasSideEffects(binary.left) || hasSideEffects(binary.right);
        }
        
        if (expr instanceof GLSLUnaryExpr) {
            GLSLUnaryExpr unary = (GLSLUnaryExpr) expr;
            if (unary.operator == GLSLUnaryExpr.Operator.PRE_INCREMENT ||
                unary.operator == GLSLUnaryExpr.Operator.PRE_DECREMENT ||
                unary.operator == GLSLUnaryExpr.Operator.POST_INCREMENT ||
                unary.operator == GLSLUnaryExpr.Operator.POST_DECREMENT) {
                return true;
            }
            return hasSideEffects(unary.operand);
        }
        
        return false;
    }
    
    private boolean isPureBuiltin(String name) {
        // All math functions are pure
        switch (name) {
            case "sin": case "cos": case "tan":
            case "asin": case "acos": case "atan":
            case "sinh": case "cosh": case "tanh":
            case "pow": case "exp": case "log": case "exp2": case "log2":
            case "sqrt": case "inversesqrt":
            case "abs": case "sign": case "floor": case "ceil": case "fract":
            case "mod": case "min": case "max": case "clamp": case "mix":
            case "step": case "smoothstep":
            case "length": case "distance": case "dot": case "cross":
            case "normalize": case "reflect": case "refract":
            case "radians": case "degrees":
            case "lessThan": case "lessThanEqual":
            case "greaterThan": case "greaterThanEqual":
            case "equal": case "notEqual":
            case "any": case "all": case "not":
                return true;
            default:
                return false;
        }
    }
    
    private static class DeadLocalVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        DeadLocalVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitDeclarationStmt(GLSLDeclarationStmt node) {
            Iterator<GLSLVariableDecl> iter = node.declarations.iterator();
            while (iter.hasNext()) {
                GLSLVariableDecl decl = iter.next();
                if (context.isUnused(decl.name) && 
                    (decl.initializer == null || !hasEffect(decl.initializer))) {
                    iter.remove();
                    context.deadCodeRemoved++;
                    changed = true;
                }
            }
            
            // If all declarations removed, mark statement for removal
            if (node.declarations.isEmpty()) {
                node.setFlag(GLSLASTNode.FLAG_DEAD);
            }
        }
        
        private boolean hasEffect(GLSLExpression expr) {
            if (expr instanceof GLSLCallExpr) {
                return true; // Assume function calls have effects
            }
            return false;
        }
    }
}

/**
 * Removes dead branches (if(false), if(true), etc.)
 */
final class DeadBranchEliminationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "DeadBranchElimination"; }
    @Override public int getPriority() { return 650; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        DeadBranchVisitor visitor = new DeadBranchVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class DeadBranchVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        DeadBranchVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitIfStmt(GLSLIfStmt node) {
            // First process children
            super.visitIfStmt(node);
            
            // Check for constant condition
            if (node.condition instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) node.condition;
                if (lit.literalType == GLSLLiteralExpr.LiteralType.BOOL) {
                    // Replace if statement with appropriate branch
                    GLSLASTNode parent = node.parent;
                    if (parent instanceof GLSLBlockStmt) {
                        GLSLBlockStmt block = (GLSLBlockStmt) parent;
                        int index = block.statements.indexOf(node);
                        if (index >= 0) {
                            block.statements.remove(index);
                            
                            GLSLStatement replacement = lit.boolValue ? 
                                node.thenBranch : node.elseBranch;
                            
                            if (replacement != null) {
                                // If replacement is a block, inline its statements
                                if (replacement instanceof GLSLBlockStmt) {
                                    GLSLBlockStmt repBlock = (GLSLBlockStmt) replacement;
                                    for (int i = 0; i < repBlock.statements.size(); i++) {
                                        block.statements.add(index + i, repBlock.statements.get(i));
                                    }
                                } else {
                                    block.statements.add(index, replacement);
                                }
                            }
                            
                            context.deadCodeRemoved++;
                            changed = true;
                        }
                    }
                }
            }
        }
        
        @Override
        public void visitTernaryExpr(GLSLTernaryExpr node) {
            super.visitTernaryExpr(node);
            
            if (node.condition instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) node.condition;
                if (lit.literalType == GLSLLiteralExpr.LiteralType.BOOL) {
                    GLSLExpression replacement = lit.boolValue ? 
                        node.thenExpr : node.elseExpr;
                    
                    replaceInParent(node, replacement);
                    context.deadCodeRemoved++;
                    changed = true;
                }
            }
        }
        
        @Override
        public void visitWhileStmt(GLSLWhileStmt node) {
            super.visitWhileStmt(node);
            
            // while(false) -> remove entirely
            if (node.condition instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) node.condition;
                if (lit.literalType == GLSLLiteralExpr.LiteralType.BOOL && !lit.boolValue) {
                    node.setFlag(GLSLASTNode.FLAG_DEAD);
                    context.deadCodeRemoved++;
                    changed = true;
                }
            }
        }
        
        private void replaceInParent(GLSLASTNode oldNode, GLSLASTNode newNode) {
            GLSLASTNode parent = oldNode.parent;
            if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChild(i) == oldNode) {
                        parent.setChild(i, newNode);
                        break;
                    }
                }
            }
        }
    }
}

/**
 * Removes code after return/discard/break/continue.
 */
final class UnreachableCodeEliminationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "UnreachableCodeElimination"; }
    @Override public int getPriority() { return 600; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        UnreachableVisitor visitor = new UnreachableVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class UnreachableVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        UnreachableVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitBlockStmt(GLSLBlockStmt node) {
            boolean foundTerminator = false;
            
            for (int i = 0; i < node.statements.size(); i++) {
                GLSLStatement stmt = node.statements.get(i);
                
                if (foundTerminator) {
                    // This statement is unreachable
                    stmt.setFlag(GLSLASTNode.FLAG_DEAD);
                    context.deadCodeRemoved++;
                    changed = true;
                } else if (isTerminator(stmt)) {
                    foundTerminator = true;
                }
                
                stmt.accept(this);
            }
            
            // Remove dead statements
            node.statements.removeIf(s -> s.hasFlag(GLSLASTNode.FLAG_DEAD));
        }
        
        private boolean isTerminator(GLSLStatement stmt) {
            return stmt instanceof GLSLReturnStmt ||
                   stmt instanceof GLSLDiscardStmt ||
                   stmt instanceof GLSLBreakStmt ||
                   stmt instanceof GLSLContinueStmt;
        }
    }
}

// ============================================================================
// VARIABLE INLINING PASS
// ============================================================================

/**
 * Inlines variables that are only used once.
 */
final class VariableInliningPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "VariableInlining"; }
    @Override public int getPriority() { return 550; }
    @Override public int getMinimumLevel() { return 2; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        // Find variables used exactly once
        Map<String, GLSLExpression> inlineValues = new HashMap<>();
        
        for (GLSLVariableDecl var : shader.globalVariables) {
            if (canInline(var, context)) {
                inlineValues.put(var.name, var.initializer);
            }
        }
        
        if (inlineValues.isEmpty()) {
            return false;
        }
        
        InliningVisitor visitor = new InliningVisitor(inlineValues, context);
        shader.accept(visitor);
        
        // Remove inlined variable declarations
        if (visitor.changed) {
            shader.globalVariables.removeIf(v -> inlineValues.containsKey(v.name));
            shader.declarations.removeIf(d -> 
                d instanceof GLSLVariableDecl && inlineValues.containsKey(((GLSLVariableDecl) d).name)
            );
        }
        
        return visitor.changed;
    }
    
    private boolean canInline(GLSLVariableDecl var, GLSLOptimizationContext context) {
        // Only inline if used exactly once
        int useCount = context.variableUseCount.getOrDefault(var.name, 0);
        if (useCount != 1) return false;
        
        // Must have initializer
        if (var.initializer == null) return false;
        
        // Don't inline uniforms, inputs, outputs
        if (var.type.qualifiers != null) {
            switch (var.type.qualifiers.storage) {
                case UNIFORM:
                case IN:
                case OUT:
                case ATTRIBUTE:
                case VARYING:
                case BUFFER:
                case SHARED:
                    return false;
                default:
                    break;
            }
        }
        
        // Only inline simple expressions to avoid code bloat
        return isSimpleExpression(var.initializer);
    }
    
    private boolean isSimpleExpression(GLSLExpression expr) {
        if (expr instanceof GLSLLiteralExpr) return true;
        if (expr instanceof GLSLIdentifierExpr) return true;
        
        if (expr instanceof GLSLUnaryExpr) {
            return isSimpleExpression(((GLSLUnaryExpr) expr).operand);
        }
        
        if (expr instanceof GLSLBinaryExpr) {
            GLSLBinaryExpr binary = (GLSLBinaryExpr) expr;
            return isSimpleExpression(binary.left) && isSimpleExpression(binary.right);
        }
        
        if (expr instanceof GLSLCallExpr) {
            GLSLCallExpr call = (GLSLCallExpr) expr;
            if (call.isConstructor || call.isBuiltinFunction) {
                for (GLSLExpression arg : call.arguments) {
                    if (!isSimpleExpression(arg)) return false;
                }
                return call.arguments.size() <= 4;
            }
        }
        
        return false;
    }
    
    private static class InliningVisitor extends GLSLASTBaseVisitor {
        private final Map<String, GLSLExpression> inlineValues;
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        InliningVisitor(Map<String, GLSLExpression> inlineValues, GLSLOptimizationContext context) {
            this.inlineValues = inlineValues;
            this.context = context;
        }
        
        @Override
        public void visitIdentifierExpr(GLSLIdentifierExpr node) {
            GLSLExpression replacement = inlineValues.get(node.name);
            if (replacement != null) {
                GLSLExpression copy = (GLSLExpression) replacement.copy(context.pool);
                replaceInParent(node, copy);
                context.variablesInlined++;
                changed = true;
            }
        }
        
        private void replaceInParent(GLSLASTNode oldNode, GLSLASTNode newNode) {
            GLSLASTNode parent = oldNode.parent;
            if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChild(i) == oldNode) {
                        parent.setChild(i, newNode);
                        break;
                    }
                }
            }
        }
    }
}

// ============================================================================
// COMMON SUBEXPRESSION ELIMINATION
// ============================================================================

/**
 * Identifies and caches common subexpressions.
 * Note: This is a simplified version that only identifies, not transforms.
 */
final class CommonSubexpressionPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "CommonSubexpression"; }
    @Override public int getPriority() { return 500; }
    @Override public int getMinimumLevel() { return 3; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        // CSE is complex - for now just identify opportunities
        // Full implementation would create temporary variables
        return false;
    }
}

// ============================================================================
// PRECISION OPTIMIZATION PASS
// ============================================================================

/**
 * Optimizes precision qualifiers for better performance.
 */
final class PrecisionOptimizationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "PrecisionOptimization"; }
    @Override public int getPriority() { return 400; }
    @Override public int getMinimumLevel() { return 2; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) {
        // Only applies to ES or when targeting mobile
        return context.targetVersion.versionNumber >= 300;
    }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        // This would analyze variable usage and potentially downgrade precision
        // For now, just a placeholder
        return false;
    }
}

// ============================================================================
// SWIZZLE OPTIMIZATION PASS
// ============================================================================

/**
 * Optimizes vector swizzle operations.
 */
final class SwizzleOptimizationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "SwizzleOptimization"; }
    @Override public int getPriority() { return 350; }
    @Override public int getMinimumLevel() { return 2; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        SwizzleVisitor visitor = new SwizzleVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class SwizzleVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        SwizzleVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitMemberExpr(GLSLMemberExpr node) {
            super.visitMemberExpr(node);
            
            // Collapse chained swizzles: v.xy.x -> v.x
            if (node.isSwizzle && node.object instanceof GLSLMemberExpr) {
                GLSLMemberExpr inner = (GLSLMemberExpr) node.object;
                if (inner.isSwizzle) {
                    String collapsed = collapseSwizzle(inner.member, node.member);
                    if (collapsed != null) {
                        node.object = inner.object;
                        node.member = collapsed;
                        inner.object.parent = node;
                        context.expressionsSimplified++;
                        changed = true;
                    }
                }
            }
            
            // Identity swizzle: v.xyzw -> v (for vec4)
            // v.xyz -> v (for vec3), etc.
            if (node.isSwizzle && isIdentitySwizzle(node.member)) {
                // Would need type info to safely remove
            }
        }
        
        private String collapseSwizzle(String first, String second) {
            // first is the inner swizzle, second is the outer
            // Example: first = "xy", second = "x" -> result = "x"
            // Example: first = "zw", second = "yx" -> result = "wz"
            
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < second.length(); i++) {
                char c = second.charAt(i);
                int index = "xyzwrgbastpq".indexOf(c) % 4;
                if (index < 0 || index >= first.length()) {
                    return null; // Invalid swizzle
                }
                result.append(first.charAt(index));
            }
            return result.toString();
        }
        
        private boolean isIdentitySwizzle(String swizzle) {
            return swizzle.equals("xyzw") || swizzle.equals("rgba") || swizzle.equals("stpq") ||
                   swizzle.equals("xyz") || swizzle.equals("rgb") || swizzle.equals("stp") ||
                   swizzle.equals("xy") || swizzle.equals("rg") || swizzle.equals("st") ||
                   swizzle.equals("x") || swizzle.equals("r") || swizzle.equals("s");
        }
    }
}

// ============================================================================
// VECTOR CONSTRUCTOR OPTIMIZATION PASS
// ============================================================================

/**
 * Optimizes vector and matrix constructor calls.
 */
final class VectorConstructorOptimizationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "VectorConstructorOptimization"; }
    @Override public int getPriority() { return 300; }
    @Override public int getMinimumLevel() { return 2; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        VectorConstructorVisitor visitor = new VectorConstructorVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class VectorConstructorVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        VectorConstructorVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitCallExpr(GLSLCallExpr node) {
            super.visitCallExpr(node);
            
            if (!node.isConstructor) return;
            
            // vec3(v.x, v.y, v.z) -> v.xyz (if v is vec3 or larger)
            // This needs type information to implement safely
            
            // vec4(1.0, 1.0, 1.0, 1.0) -> vec4(1.0) (splat optimization)
            if (isVectorConstructor(node.functionName) && allSameConstant(node.arguments)) {
                if (node.arguments.size() > 1) {
                    GLSLExpression firstArg = node.arguments.get(0);
                    node.arguments.clear();
                    node.addArgument(firstArg);
                    context.expressionsSimplified++;
                    changed = true;
                }
            }
        }
        
        private boolean isVectorConstructor(String name) {
            return name.matches("(b|i|u|d)?vec[234]");
        }
        
        private boolean allSameConstant(List<GLSLExpression> args) {
            if (args.isEmpty()) return false;
            
            GLSLExpression first = args.get(0);
            if (!(first instanceof GLSLLiteralExpr)) return false;
            
            GLSLLiteralExpr firstLit = (GLSLLiteralExpr) first;
            
            for (int i = 1; i < args.size(); i++) {
                if (!(args.get(i) instanceof GLSLLiteralExpr)) return false;
                GLSLLiteralExpr lit = (GLSLLiteralExpr) args.get(i);
                
                if (firstLit.literalType != lit.literalType) return false;
                
                switch (firstLit.literalType) {
                    case INT:
                    case UINT:
                        if (firstLit.intValue != lit.intValue) return false;
                        break;
                    case FLOAT:
                    case DOUBLE:
                        if (firstLit.floatValue != lit.floatValue) return false;
                        break;
                    case BOOL:
                        if (firstLit.boolValue != lit.boolValue) return false;
                        break;
                }
            }
            
            return true;
        }
    }
}

// ============================================================================
// CLEANUP PASSES
// ============================================================================

/**
 * Removes empty blocks and simplifies nested blocks.
 */
final class EmptyBlockRemovalPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "EmptyBlockRemoval"; }
    @Override public int getPriority() { return 100; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        EmptyBlockVisitor visitor = new EmptyBlockVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class EmptyBlockVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        EmptyBlockVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitBlockStmt(GLSLBlockStmt node) {
            // First process children
            super.visitBlockStmt(node);
            
            // Remove dead statements
            boolean removed = node.statements.removeIf(s -> s.hasFlag(GLSLASTNode.FLAG_DEAD));
            if (removed) {
                changed = true;
            }
            
            // Flatten nested blocks: { { stmt; } } -> { stmt; }
            for (int i = 0; i < node.statements.size(); i++) {
                GLSLStatement stmt = node.statements.get(i);
                if (stmt instanceof GLSLBlockStmt) {
                    GLSLBlockStmt inner = (GLSLBlockStmt) stmt;
                    // Only flatten if inner block has no local declarations
                    if (inner.localSymbols == null || inner.localSymbols.getSymbols().isEmpty()) {
                        node.statements.remove(i);
                        for (int j = 0; j < inner.statements.size(); j++) {
                            node.statements.add(i + j, inner.statements.get(j));
                        }
                        changed = true;
                        i--; // Re-check at same position
                    }
                }
            }
        }
        
        @Override
        public void visitIfStmt(GLSLIfStmt node) {
            super.visitIfStmt(node);
            
            // if(cond) {} else {} -> remove (unless condition has side effects)
            if (isEmpty(node.thenBranch) && isEmpty(node.elseBranch)) {
                node.setFlag(GLSLASTNode.FLAG_DEAD);
                context.deadCodeRemoved++;
                changed = true;
            }
            
            // if(cond) {} else stmt -> if(!cond) stmt
            if (isEmpty(node.thenBranch) && !isEmpty(node.elseBranch)) {
                GLSLUnaryExpr notCond = new GLSLUnaryExpr(
                    GLSLUnaryExpr.Operator.NOT, node.condition);
                node.condition = notCond;
                node.thenBranch = node.elseBranch;
                node.elseBranch = null;
                context.expressionsSimplified++;
                changed = true;
            }
        }
        
        private boolean isEmpty(GLSLStatement stmt) {
            if (stmt == null) return true;
            if (stmt instanceof GLSLBlockStmt) {
                return ((GLSLBlockStmt) stmt).statements.isEmpty();
            }
            if (stmt instanceof GLSLExpressionStmt) {
                return ((GLSLExpressionStmt) stmt).expression == null;
            }
            return false;
        }
    }
}

/**
 * Final cleanup of declarations.
 */
final class DeclarationCleanupPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "DeclarationCleanup"; }
    @Override public int getPriority() { return 50; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        boolean changed = false;
        
        // Remove declarations marked as dead
        changed |= shader.declarations.removeIf(d -> d.hasFlag(GLSLASTNode.FLAG_DEAD));
        
        // Remove empty declaration statements in functions
        for (GLSLFunctionDecl func : shader.functions) {
            if (func.body != null) {
                changed |= cleanupBlock(func.body);
            }
        }
        
        return changed;
    }
    
    private boolean cleanupBlock(GLSLBlockStmt block) {
        boolean changed = block.statements.removeIf(s -> {
            if (s instanceof GLSLDeclarationStmt) {
                return ((GLSLDeclarationStmt) s).declarations.isEmpty();
            }
            return s.hasFlag(GLSLASTNode.FLAG_DEAD);
        });
        
        for (GLSLStatement stmt : block.statements) {
            if (stmt instanceof GLSLBlockStmt) {
                changed |= cleanupBlock((GLSLBlockStmt) stmt);
            } else if (stmt instanceof GLSLIfStmt) {
                GLSLIfStmt ifStmt = (GLSLIfStmt) stmt;
                if (ifStmt.thenBranch instanceof GLSLBlockStmt) {
                    changed |= cleanupBlock((GLSLBlockStmt) ifStmt.thenBranch);
                }
                if (ifStmt.elseBranch instanceof GLSLBlockStmt) {
                    changed |= cleanupBlock((GLSLBlockStmt) ifStmt.elseBranch);
                }
            } else if (stmt instanceof GLSLForStmt) {
                if (((GLSLForStmt) stmt).body instanceof GLSLBlockStmt) {
                    changed |= cleanupBlock((GLSLBlockStmt) ((GLSLForStmt) stmt).body);
                }
            } else if (stmt instanceof GLSLWhileStmt) {
                if (((GLSLWhileStmt) stmt).body instanceof GLSLBlockStmt) {
                    changed |= cleanupBlock((GLSLBlockStmt) ((GLSLWhileStmt) stmt).body);
                }
            }
        }
        
        return changed;
    }
}

// ============================================================================
// OPTIMIZATION PASS SYSTEM
// ============================================================================

/**
 * Base interface for optimization passes.
 * Passes are applied in order based on priority and dependencies.
 */
interface GLSLOptimizationPass {
    
    /**
     * Get the name of this pass for debugging/logging.
     */
    String getName();
    
    /**
     * Get the priority of this pass. Higher priority runs first.
     */
    int getPriority();
    
    /**
     * Get the minimum optimization level required for this pass.
     * Level 0 = disabled, 1 = basic, 2 = standard, 3 = aggressive
     */
    int getMinimumLevel();
    
    /**
     * Check if this pass should run.
     */
    boolean shouldRun(GLSLOptimizationContext context);
    
    /**
     * Run the optimization pass on the shader.
     * Returns true if any modifications were made.
     */
    boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context);
}

/**
 * Context for optimization passes containing shared state and utilities.
 */
final class GLSLOptimizationContext {
    
    final GLSLMemoryPool pool;
    final int optimizationLevel;
    final GLSLShaderType shaderType;
    final GLSLVersion targetVersion;
    
    // Usage tracking
    final Map<String, Integer> variableUseCount = new HashMap<>();
    final Map<String, Integer> functionCallCount = new HashMap<>();
    final Set<String> writtenVariables = new HashSet<>();
    final Set<String> readVariables = new HashSet<>();
    
    // Constant values for propagation
    final Map<String, GLSLConstantValue> constantValues = new HashMap<>();
    
    // Statistics
    int constantsFolded = 0;
    int deadCodeRemoved = 0;
    int expressionsSimplified = 0;
    int variablesInlined = 0;
    
    // Iteration control
    int passIterations = 0;
    static final int MAX_ITERATIONS = 10;
    
    GLSLOptimizationContext(GLSLMemoryPool pool, int level, 
                            GLSLShaderType shaderType, GLSLVersion version) {
        this.pool = pool;
        this.optimizationLevel = level;
        this.shaderType = shaderType;
        this.targetVersion = version;
    }
    
    void reset() {
        variableUseCount.clear();
        functionCallCount.clear();
        writtenVariables.clear();
        readVariables.clear();
        constantValues.clear();
    }
    
    void recordVariableUse(String name) {
        variableUseCount.merge(name, 1, Integer::sum);
    }
    
    void recordFunctionCall(String name) {
        functionCallCount.merge(name, 1, Integer::sum);
    }
    
    void recordVariableWrite(String name) {
        writtenVariables.add(name);
    }
    
    void recordVariableRead(String name) {
        readVariables.add(name);
    }
    
    void setConstantValue(String name, GLSLConstantValue value) {
        constantValues.put(name, value);
    }
    
    GLSLConstantValue getConstantValue(String name) {
        return constantValues.get(name);
    }
    
    boolean isUnused(String name) {
        return variableUseCount.getOrDefault(name, 0) == 0;
    }
    
    boolean isWriteOnly(String name) {
        return writtenVariables.contains(name) && !readVariables.contains(name);
    }
    
    boolean shouldContinueIterating() {
        return passIterations++ < MAX_ITERATIONS;
    }
}

/**
 * Represents a constant value that can be propagated.
 */
final class GLSLConstantValue {
    
    enum Type { BOOL, INT, UINT, FLOAT, DOUBLE, VECTOR, MATRIX }
    
    final Type type;
    final int componentCount;
    
    // Storage for different types
    boolean boolValue;
    long intValue;
    double floatValue;
    double[] vectorValues; // For vectors and matrices
    
    private GLSLConstantValue(Type type, int componentCount) {
        this.type = type;
        this.componentCount = componentCount;
    }
    
    static GLSLConstantValue ofBool(boolean value) {
        GLSLConstantValue cv = new GLSLConstantValue(Type.BOOL, 1);
        cv.boolValue = value;
        return cv;
    }
    
    static GLSLConstantValue ofInt(long value) {
        GLSLConstantValue cv = new GLSLConstantValue(Type.INT, 1);
        cv.intValue = value;
        return cv;
    }
    
    static GLSLConstantValue ofFloat(double value) {
        GLSLConstantValue cv = new GLSLConstantValue(Type.FLOAT, 1);
        cv.floatValue = value;
        return cv;
    }
    
    static GLSLConstantValue ofVector(double[] values) {
        GLSLConstantValue cv = new GLSLConstantValue(Type.VECTOR, values.length);
        cv.vectorValues = values.clone();
        return cv;
    }
    
    boolean isZero() {
        switch (type) {
            case BOOL: return !boolValue;
            case INT:
            case UINT: return intValue == 0;
            case FLOAT:
            case DOUBLE: return floatValue == 0.0;
            case VECTOR:
                for (double v : vectorValues) {
                    if (v != 0.0) return false;
                }
                return true;
            default: return false;
        }
    }
    
    boolean isOne() {
        switch (type) {
            case BOOL: return boolValue;
            case INT:
            case UINT: return intValue == 1;
            case FLOAT:
            case DOUBLE: return floatValue == 1.0;
            case VECTOR:
                for (double v : vectorValues) {
                    if (v != 1.0) return false;
                }
                return true;
            default: return false;
        }
    }
    
    GLSLExpression toExpression(GLSLMemoryPool pool) {
        switch (type) {
            case BOOL:
                return new GLSLLiteralExpr(boolValue);
            case INT:
            case UINT:
                return new GLSLLiteralExpr(intValue);
            case FLOAT:
            case DOUBLE:
                return new GLSLLiteralExpr(floatValue);
            default:
                return null; // Vectors handled separately
        }
    }
}

// ============================================================================
// MAIN OPTIMIZER
// ============================================================================

/**
 * Main optimizer that coordinates optimization passes.
 */
final class GLSLOptimizer {
    
    private final GLSLMemoryPool pool;
    private final List<GLSLOptimizationPass> passes;
    
    GLSLOptimizer(GLSLMemoryPool pool) {
        this.pool = pool;
        this.passes = new ArrayList<>();
        
        // Register passes in priority order
        registerPasses();
        
        // Sort by priority (descending)
        passes.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }
    
    private void registerPasses() {
        // Analysis passes (run first)
        passes.add(new UsageAnalysisPass());
        passes.add(new ConstantAnalysisPass());
        
        // Optimization passes
        passes.add(new ConstantFoldingPass());
        passes.add(new ConstantPropagationPass());
        passes.add(new AlgebraicSimplificationPass());
        passes.add(new StrengthReductionPass());
        passes.add(new DeadCodeEliminationPass());
        passes.add(new DeadBranchEliminationPass());
        passes.add(new UnreachableCodeEliminationPass());
        passes.add(new VariableInliningPass());
        passes.add(new CommonSubexpressionPass());
        
        // Shader-specific passes
        passes.add(new PrecisionOptimizationPass());
        passes.add(new SwizzleOptimizationPass());
        passes.add(new VectorConstructorOptimizationPass());
        
        // Cleanup passes (run last)
        passes.add(new EmptyBlockRemovalPass());
        passes.add(new DeclarationCleanupPass());
    }
    
    /**
     * Optimize the shader AST.
     */
    GLSLShaderAST optimize(GLSLShaderAST shader, int level) {
        if (level <= 0) {
            return shader;
        }
        
        GLSLOptimizationContext context = new GLSLOptimizationContext(
            pool, level, shader.shaderType, shader.version
        );
        
        boolean changed;
        do {
            changed = false;
            context.reset();
            
            for (GLSLOptimizationPass pass : passes) {
                if (pass.getMinimumLevel() <= level && pass.shouldRun(context)) {
                    boolean passChanged = pass.optimize(shader, context);
                    changed = changed || passChanged;
                }
            }
            
        } while (changed && context.shouldContinueIterating());
        
        return shader;
    }
}

// ============================================================================
// ANALYSIS PASSES
// ============================================================================

/**
 * Analyzes variable and function usage throughout the shader.
 */
final class UsageAnalysisPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "UsageAnalysis"; }
    @Override public int getPriority() { return 1000; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        UsageVisitor visitor = new UsageVisitor(context);
        shader.accept(visitor);
        return false; // Analysis only, no modifications
    }
    
    private static class UsageVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        private boolean inLValue = false;
        
        UsageVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitIdentifierExpr(GLSLIdentifierExpr node) {
            context.recordVariableUse(node.name);
            if (inLValue) {
                context.recordVariableWrite(node.name);
            } else {
                context.recordVariableRead(node.name);
            }
        }
        
        @Override
        public void visitCallExpr(GLSLCallExpr node) {
            context.recordFunctionCall(node.functionName);
            super.visitCallExpr(node);
        }
        
        @Override
        public void visitBinaryExpr(GLSLBinaryExpr node) {
            // Handle assignment LHS
            if (node.operator.isAssignment()) {
                inLValue = true;
                node.left.accept(this);
                inLValue = false;
                node.right.accept(this);
            } else {
                super.visitBinaryExpr(node);
            }
        }
        
        @Override
        public void visitUnaryExpr(GLSLUnaryExpr node) {
            if (node.operator == GLSLUnaryExpr.Operator.PRE_INCREMENT ||
                node.operator == GLSLUnaryExpr.Operator.PRE_DECREMENT ||
                node.operator == GLSLUnaryExpr.Operator.POST_INCREMENT ||
                node.operator == GLSLUnaryExpr.Operator.POST_DECREMENT) {
                inLValue = true;
                node.operand.accept(this);
                inLValue = false;
                // Also counts as a read
                context.recordVariableRead(getIdentifierName(node.operand));
            } else {
                super.visitUnaryExpr(node);
            }
        }
        
        private String getIdentifierName(GLSLExpression expr) {
            if (expr instanceof GLSLIdentifierExpr) {
                return ((GLSLIdentifierExpr) expr).name;
            }
            return null;
        }
    }
}

/**
 * Analyzes constant values for propagation.
 */
final class ConstantAnalysisPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "ConstantAnalysis"; }
    @Override public int getPriority() { return 999; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        // Find const variables with literal initializers
        for (GLSLVariableDecl var : shader.globalVariables) {
            if (isConstant(var)) {
                GLSLConstantValue value = evaluateConstant(var.initializer);
                if (value != null) {
                    context.setConstantValue(var.name, value);
                }
            }
        }
        
        // Also analyze function-local const variables
        ConstantVisitor visitor = new ConstantVisitor(context);
        shader.accept(visitor);
        
        return false; // Analysis only
    }
    
    private boolean isConstant(GLSLVariableDecl var) {
        return var.type.qualifiers != null && 
               var.type.qualifiers.storage == GLSLTypeQualifiers.Storage.CONST &&
               var.initializer != null;
    }
    
    private GLSLConstantValue evaluateConstant(GLSLExpression expr) {
        if (expr instanceof GLSLLiteralExpr) {
            GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
            switch (lit.literalType) {
                case BOOL: return GLSLConstantValue.ofBool(lit.boolValue);
                case INT:
                case UINT: return GLSLConstantValue.ofInt(lit.intValue);
                case FLOAT:
                case DOUBLE: return GLSLConstantValue.ofFloat(lit.floatValue);
            }
        }
        return null;
    }
    
    private static class ConstantVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        
        ConstantVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitDeclarationStmt(GLSLDeclarationStmt node) {
            for (GLSLVariableDecl decl : node.declarations) {
                if (decl.type.qualifiers != null &&
                    decl.type.qualifiers.storage == GLSLTypeQualifiers.Storage.CONST &&
                    decl.initializer instanceof GLSLLiteralExpr) {
                    
                    GLSLLiteralExpr lit = (GLSLLiteralExpr) decl.initializer;
                    GLSLConstantValue value = null;
                    
                    switch (lit.literalType) {
                        case BOOL: value = GLSLConstantValue.ofBool(lit.boolValue); break;
                        case INT:
                        case UINT: value = GLSLConstantValue.ofInt(lit.intValue); break;
                        case FLOAT:
                        case DOUBLE: value = GLSLConstantValue.ofFloat(lit.floatValue); break;
                    }
                    
                    if (value != null) {
                        context.setConstantValue(decl.name, value);
                    }
                }
            }
            super.visitDeclarationStmt(node);
        }
    }
}

// ============================================================================
// CONSTANT FOLDING PASS
// ============================================================================

/**
 * Evaluates constant expressions at compile time.
 */
final class ConstantFoldingPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "ConstantFolding"; }
    @Override public int getPriority() { return 900; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        ConstantFoldingVisitor visitor = new ConstantFoldingVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class ConstantFoldingVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        ConstantFoldingVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitBinaryExpr(GLSLBinaryExpr node) {
            // First fold children
            super.visitBinaryExpr(node);
            
            // Try to fold this expression
            if (node.left instanceof GLSLLiteralExpr && node.right instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr left = (GLSLLiteralExpr) node.left;
                GLSLLiteralExpr right = (GLSLLiteralExpr) node.right;
                
                GLSLLiteralExpr result = foldBinary(node.operator, left, right);
                if (result != null) {
                    replaceInParent(node, result);
                    context.constantsFolded++;
                    changed = true;
                }
            }
        }
        
        @Override
        public void visitUnaryExpr(GLSLUnaryExpr node) {
            super.visitUnaryExpr(node);
            
            if (node.operand instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr operand = (GLSLLiteralExpr) node.operand;
                
                GLSLLiteralExpr result = foldUnary(node.operator, operand);
                if (result != null) {
                    replaceInParent(node, result);
                    context.constantsFolded++;
                    changed = true;
                }
            }
        }
        
        @Override
        public void visitTernaryExpr(GLSLTernaryExpr node) {
            super.visitTernaryExpr(node);
            
            if (node.condition instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr cond = (GLSLLiteralExpr) node.condition;
                if (cond.literalType == GLSLLiteralExpr.LiteralType.BOOL) {
                    GLSLExpression result = cond.boolValue ? node.thenExpr : node.elseExpr;
                    replaceInParent(node, result);
                    context.constantsFolded++;
                    changed = true;
                }
            }
        }
        
        @Override
        public void visitCallExpr(GLSLCallExpr node) {
            super.visitCallExpr(node);
            
            // Try to fold built-in function calls with constant arguments
            if (node.isBuiltinFunction && allArgsConstant(node)) {
                GLSLLiteralExpr result = foldBuiltinCall(node);
                if (result != null) {
                    replaceInParent(node, result);
                    context.constantsFolded++;
                    changed = true;
                }
            }
        }
        
        private GLSLLiteralExpr foldBinary(GLSLBinaryExpr.Operator op, 
                                           GLSLLiteralExpr left, GLSLLiteralExpr right) {
            // Integer operations
            if (isIntegral(left) && isIntegral(right)) {
                long l = left.intValue;
                long r = right.intValue;
                
                switch (op) {
                    case ADD: return literalInt(l + r);
                    case SUB: return literalInt(l - r);
                    case MUL: return literalInt(l * r);
                    case DIV: return r != 0 ? literalInt(l / r) : null;
                    case MOD: return r != 0 ? literalInt(l % r) : null;
                    case BIT_AND: return literalInt(l & r);
                    case BIT_OR: return literalInt(l | r);
                    case BIT_XOR: return literalInt(l ^ r);
                    case LEFT_SHIFT: return literalInt(l << r);
                    case RIGHT_SHIFT: return literalInt(l >> r);
                    case EQ: return literalBool(l == r);
                    case NE: return literalBool(l != r);
                    case LT: return literalBool(l < r);
                    case GT: return literalBool(l > r);
                    case LE: return literalBool(l <= r);
                    case GE: return literalBool(l >= r);
                    default: break;
                }
            }
            
            // Float operations
            if (isFloating(left) || isFloating(right)) {
                double l = toDouble(left);
                double r = toDouble(right);
                
                switch (op) {
                    case ADD: return literalFloat(l + r);
                    case SUB: return literalFloat(l - r);
                    case MUL: return literalFloat(l * r);
                    case DIV: return r != 0 ? literalFloat(l / r) : null;
                    case EQ: return literalBool(l == r);
                    case NE: return literalBool(l != r);
                    case LT: return literalBool(l < r);
                    case GT: return literalBool(l > r);
                    case LE: return literalBool(l <= r);
                    case GE: return literalBool(l >= r);
                    default: break;
                }
            }
            
            // Boolean operations
            if (isBool(left) && isBool(right)) {
                boolean l = left.boolValue;
                boolean r = right.boolValue;
                
                switch (op) {
                    case AND: return literalBool(l && r);
                    case OR: return literalBool(l || r);
                    case XOR: return literalBool(l ^ r);
                    case EQ: return literalBool(l == r);
                    case NE: return literalBool(l != r);
                    default: break;
                }
            }
            
            return null;
        }
        
        private GLSLLiteralExpr foldUnary(GLSLUnaryExpr.Operator op, GLSLLiteralExpr operand) {
            switch (op) {
                case MINUS:
                    if (isIntegral(operand)) {
                        return literalInt(-operand.intValue);
                    } else if (isFloating(operand)) {
                        return literalFloat(-operand.floatValue);
                    }
                    break;
                    
                case PLUS:
                    return operand; // No change
                    
                case NOT:
                    if (isBool(operand)) {
                        return literalBool(!operand.boolValue);
                    }
                    break;
                    
                case BIT_NOT:
                    if (isIntegral(operand)) {
                        return literalInt(~operand.intValue);
                    }
                    break;
                    
                default:
                    break;
            }
            return null;
        }
        
        private GLSLLiteralExpr foldBuiltinCall(GLSLCallExpr call) {
            String func = call.functionName;
            List<GLSLExpression> args = call.arguments;
            
            // Single-argument math functions
            if (args.size() == 1 && args.get(0) instanceof GLSLLiteralExpr) {
                double x = toDouble((GLSLLiteralExpr) args.get(0));
                
                switch (func) {
                    case "abs": return literalFloat(Math.abs(x));
                    case "sign": return literalFloat(Math.signum(x));
                    case "floor": return literalFloat(Math.floor(x));
                    case "ceil": return literalFloat(Math.ceil(x));
                    case "fract": return literalFloat(x - Math.floor(x));
                    case "round": return literalFloat(Math.round(x));
                    case "trunc": return literalFloat((long) x);
                    case "sin": return literalFloat(Math.sin(x));
                    case "cos": return literalFloat(Math.cos(x));
                    case "tan": return literalFloat(Math.tan(x));
                    case "asin": return literalFloat(Math.asin(x));
                    case "acos": return literalFloat(Math.acos(x));
                    case "atan": return literalFloat(Math.atan(x));
                    case "sinh": return literalFloat(Math.sinh(x));
                    case "cosh": return literalFloat(Math.cosh(x));
                    case "tanh": return literalFloat(Math.tanh(x));
                    case "exp": return literalFloat(Math.exp(x));
                    case "log": return x > 0 ? literalFloat(Math.log(x)) : null;
                    case "exp2": return literalFloat(Math.pow(2, x));
                    case "log2": return x > 0 ? literalFloat(Math.log(x) / Math.log(2)) : null;
                    case "sqrt": return x >= 0 ? literalFloat(Math.sqrt(x)) : null;
                    case "inversesqrt": return x > 0 ? literalFloat(1.0 / Math.sqrt(x)) : null;
                    case "radians": return literalFloat(Math.toRadians(x));
                    case "degrees": return literalFloat(Math.toDegrees(x));
                }
            }
            
            // Two-argument math functions
            if (args.size() == 2 && 
                args.get(0) instanceof GLSLLiteralExpr &&
                args.get(1) instanceof GLSLLiteralExpr) {
                
                double x = toDouble((GLSLLiteralExpr) args.get(0));
                double y = toDouble((GLSLLiteralExpr) args.get(1));
                
                switch (func) {
                    case "pow": return literalFloat(Math.pow(x, y));
                    case "mod": return y != 0 ? literalFloat(x - y * Math.floor(x / y)) : null;
                    case "min": return literalFloat(Math.min(x, y));
                    case "max": return literalFloat(Math.max(x, y));
                    case "atan": return literalFloat(Math.atan2(x, y));
                    case "step": return literalFloat(x >= y ? 1.0 : 0.0);
                    case "distance": return literalFloat(Math.abs(x - y));
                }
            }
            
            // Three-argument math functions
            if (args.size() == 3 &&
                args.get(0) instanceof GLSLLiteralExpr &&
                args.get(1) instanceof GLSLLiteralExpr &&
                args.get(2) instanceof GLSLLiteralExpr) {
                
                double x = toDouble((GLSLLiteralExpr) args.get(0));
                double y = toDouble((GLSLLiteralExpr) args.get(1));
                double z = toDouble((GLSLLiteralExpr) args.get(2));
                
                switch (func) {
                    case "clamp": return literalFloat(Math.max(y, Math.min(z, x)));
                    case "mix": return literalFloat(x * (1 - z) + y * z);
                    case "smoothstep": {
                        double t = Math.max(0, Math.min(1, (z - x) / (y - x)));
                        return literalFloat(t * t * (3 - 2 * t));
                    }
                    case "fma": return literalFloat(x * y + z);
                }
            }
            
            return null;
        }
        
        private boolean allArgsConstant(GLSLCallExpr call) {
            for (GLSLExpression arg : call.arguments) {
                if (!(arg instanceof GLSLLiteralExpr)) {
                    return false;
                }
            }
            return true;
        }
        
        private boolean isIntegral(GLSLLiteralExpr lit) {
            return lit.literalType == GLSLLiteralExpr.LiteralType.INT ||
                   lit.literalType == GLSLLiteralExpr.LiteralType.UINT;
        }
        
        private boolean isFloating(GLSLLiteralExpr lit) {
            return lit.literalType == GLSLLiteralExpr.LiteralType.FLOAT ||
                   lit.literalType == GLSLLiteralExpr.LiteralType.DOUBLE;
        }
        
        private boolean isBool(GLSLLiteralExpr lit) {
            return lit.literalType == GLSLLiteralExpr.LiteralType.BOOL;
        }
        
        private double toDouble(GLSLLiteralExpr lit) {
            if (isIntegral(lit)) return lit.intValue;
            return lit.floatValue;
        }
        
        private GLSLLiteralExpr literalInt(long value) {
            GLSLLiteralExpr lit = new GLSLLiteralExpr(value);
            lit.isConstant = true;
            return lit;
        }
        
        private GLSLLiteralExpr literalFloat(double value) {
            GLSLLiteralExpr lit = new GLSLLiteralExpr(value);
            lit.isConstant = true;
            return lit;
        }
        
        private GLSLLiteralExpr literalBool(boolean value) {
            GLSLLiteralExpr lit = new GLSLLiteralExpr(value);
            lit.isConstant = true;
            return lit;
        }
        
        private void replaceInParent(GLSLASTNode oldNode, GLSLASTNode newNode) {
            GLSLASTNode parent = oldNode.parent;
            if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChild(i) == oldNode) {
                        parent.setChild(i, newNode);
                        break;
                    }
                }
            }
        }
    }
}

// ============================================================================
// CONSTANT PROPAGATION PASS
// ============================================================================

/**
 * Replaces variable references with known constant values.
 */
final class ConstantPropagationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "ConstantPropagation"; }
    @Override public int getPriority() { return 850; }
    @Override public int getMinimumLevel() { return 2; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { 
        return !context.constantValues.isEmpty();
    }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        ConstantPropagationVisitor visitor = new ConstantPropagationVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class ConstantPropagationVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        ConstantPropagationVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitIdentifierExpr(GLSLIdentifierExpr node) {
            GLSLConstantValue value = context.getConstantValue(node.name);
            if (value != null) {
                GLSLExpression replacement = value.toExpression(context.pool);
                if (replacement != null) {
                    replaceInParent(node, replacement);
                    changed = true;
                }
            }
        }
        
        private void replaceInParent(GLSLASTNode oldNode, GLSLASTNode newNode) {
            GLSLASTNode parent = oldNode.parent;
            if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChild(i) == oldNode) {
                        parent.setChild(i, newNode);
                        break;
                    }
                }
            }
        }
    }
}

// ============================================================================
// ALGEBRAIC SIMPLIFICATION PASS
// ============================================================================

/**
 * Simplifies expressions using algebraic identities.
 */
final class AlgebraicSimplificationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "AlgebraicSimplification"; }
    @Override public int getPriority() { return 800; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        AlgebraicVisitor visitor = new AlgebraicVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class AlgebraicVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        AlgebraicVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitBinaryExpr(GLSLBinaryExpr node) {
            super.visitBinaryExpr(node);
            
            GLSLExpression result = simplify(node);
            if (result != null && result != node) {
                replaceInParent(node, result);
                context.expressionsSimplified++;
                changed = true;
            }
        }
        
        @Override
        public void visitUnaryExpr(GLSLUnaryExpr node) {
            super.visitUnaryExpr(node);
            
            // Double negation: --x -> x (for non-increment/decrement)
            if (node.operator == GLSLUnaryExpr.Operator.MINUS) {
                if (node.operand instanceof GLSLUnaryExpr) {
                    GLSLUnaryExpr inner = (GLSLUnaryExpr) node.operand;
                    if (inner.operator == GLSLUnaryExpr.Operator.MINUS) {
                        replaceInParent(node, inner.operand);
                        context.expressionsSimplified++;
                        changed = true;
                    }
                }
            }
            
            // Double logical not: !!x -> x
            if (node.operator == GLSLUnaryExpr.Operator.NOT) {
                if (node.operand instanceof GLSLUnaryExpr) {
                    GLSLUnaryExpr inner = (GLSLUnaryExpr) node.operand;
                    if (inner.operator == GLSLUnaryExpr.Operator.NOT) {
                        replaceInParent(node, inner.operand);
                        context.expressionsSimplified++;
                        changed = true;
                    }
                }
            }
        }
        
        private GLSLExpression simplify(GLSLBinaryExpr expr) {
            boolean leftIsZero = isZero(expr.left);
            boolean rightIsZero = isZero(expr.right);
            boolean leftIsOne = isOne(expr.left);
            boolean rightIsOne = isOne(expr.right);
            
            switch (expr.operator) {
                // Addition identities
                case ADD:
                    if (leftIsZero) return expr.right;  // 0 + x -> x
                    if (rightIsZero) return expr.left;  // x + 0 -> x
                    break;
                
                // Subtraction identities
                case SUB:
                    if (rightIsZero) return expr.left;  // x - 0 -> x
                    if (leftIsZero) {
                        // 0 - x -> -x
                        return new GLSLUnaryExpr(GLSLUnaryExpr.Operator.MINUS, expr.right);
                    }
                    if (areEqual(expr.left, expr.right)) {
                        // x - x -> 0
                        return new GLSLLiteralExpr(0L);
                    }
                    break;
                
                // Multiplication identities
                case MUL:
                    if (leftIsZero || rightIsZero) {
                        // 0 * x or x * 0 -> 0
                        return new GLSLLiteralExpr(0.0);
                    }
                    if (leftIsOne) return expr.right;  // 1 * x -> x
                    if (rightIsOne) return expr.left;  // x * 1 -> x
                    // Check for -1 * x -> -x
                    if (isNegOne(expr.left)) {
                        return new GLSLUnaryExpr(GLSLUnaryExpr.Operator.MINUS, expr.right);
                    }
                    if (isNegOne(expr.right)) {
                        return new GLSLUnaryExpr(GLSLUnaryExpr.Operator.MINUS, expr.left);
                    }
                    break;
                
                // Division identities
                case DIV:
                    if (rightIsOne) return expr.left;  // x / 1 -> x
                    if (leftIsZero) {
                        // 0 / x -> 0 (assuming x != 0)
                        return new GLSLLiteralExpr(0.0);
                    }
                    if (areEqual(expr.left, expr.right)) {
                        // x / x -> 1 (assuming x != 0)
                        return new GLSLLiteralExpr(1.0);
                    }
                    break;
                
                // Modulo identities
                case MOD:
                    if (rightIsOne) {
                        // x % 1 -> 0 (for floats this is fract behavior)
                        return new GLSLLiteralExpr(0.0);
                    }
                    if (leftIsZero) {
                        // 0 % x -> 0
                        return new GLSLLiteralExpr(0.0);
                    }
                    break;
                
                // Logical AND identities
                case AND:
                    if (isFalse(expr.left) || isFalse(expr.right)) {
                        return new GLSLLiteralExpr(false);
                    }
                    if (isTrue(expr.left)) return expr.right;
                    if (isTrue(expr.right)) return expr.left;
                    break;
                
                // Logical OR identities
                case OR:
                    if (isTrue(expr.left) || isTrue(expr.right)) {
                        return new GLSLLiteralExpr(true);
                    }
                    if (isFalse(expr.left)) return expr.right;
                    if (isFalse(expr.right)) return expr.left;
                    break;
                
                // Bitwise AND identities
                case BIT_AND:
                    if (leftIsZero || rightIsZero) {
                        return new GLSLLiteralExpr(0L);
                    }
                    if (isAllOnes(expr.left)) return expr.right;
                    if (isAllOnes(expr.right)) return expr.left;
                    break;
                
                // Bitwise OR identities
                case BIT_OR:
                    if (leftIsZero) return expr.right;
                    if (rightIsZero) return expr.left;
                    break;
                
                // Bitwise XOR identities
                case BIT_XOR:
                    if (leftIsZero) return expr.right;
                    if (rightIsZero) return expr.left;
                    if (areEqual(expr.left, expr.right)) {
                        return new GLSLLiteralExpr(0L);
                    }
                    break;
                
                // Shift identities
                case LEFT_SHIFT:
                case RIGHT_SHIFT:
                    if (rightIsZero) return expr.left;  // x << 0 or x >> 0 -> x
                    if (leftIsZero) {
                        return new GLSLLiteralExpr(0L);  // 0 << n or 0 >> n -> 0
                    }
                    break;
                
                default:
                    break;
            }
            
            return null;
        }
        
        private boolean isZero(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                switch (lit.literalType) {
                    case INT:
                    case UINT: return lit.intValue == 0;
                    case FLOAT:
                    case DOUBLE: return lit.floatValue == 0.0;
                    default: return false;
                }
            }
            return false;
        }
        
        private boolean isOne(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                switch (lit.literalType) {
                    case INT:
                    case UINT: return lit.intValue == 1;
                    case FLOAT:
                    case DOUBLE: return lit.floatValue == 1.0;
                    default: return false;
                }
            }
            return false;
        }
        
        private boolean isNegOne(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                switch (lit.literalType) {
                    case INT: return lit.intValue == -1;
                    case FLOAT:
                    case DOUBLE: return lit.floatValue == -1.0;
                    default: return false;
                }
            }
            return false;
        }
        
        private boolean isTrue(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                return lit.literalType == GLSLLiteralExpr.LiteralType.BOOL && lit.boolValue;
            }
            return false;
        }
        
        private boolean isFalse(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                return lit.literalType == GLSLLiteralExpr.LiteralType.BOOL && !lit.boolValue;
            }
            return false;
        }
        
        private boolean isAllOnes(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                if (lit.literalType == GLSLLiteralExpr.LiteralType.INT ||
                    lit.literalType == GLSLLiteralExpr.LiteralType.UINT) {
                    return lit.intValue == -1L || lit.intValue == 0xFFFFFFFFL;
                }
            }
            return false;
        }
        
        private boolean areEqual(GLSLExpression a, GLSLExpression b) {
            // Simple structural equality check
            if (a instanceof GLSLIdentifierExpr && b instanceof GLSLIdentifierExpr) {
                return ((GLSLIdentifierExpr) a).name.equals(((GLSLIdentifierExpr) b).name);
            }
            return false;
        }
        
        private void replaceInParent(GLSLASTNode oldNode, GLSLASTNode newNode) {
            GLSLASTNode parent = oldNode.parent;
            if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChild(i) == oldNode) {
                        parent.setChild(i, newNode);
                        break;
                    }
                }
            }
        }
    }
}

// ============================================================================
// STRENGTH REDUCTION PASS
// ============================================================================

/**
 * Replaces expensive operations with cheaper equivalents.
 */
final class StrengthReductionPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "StrengthReduction"; }
    @Override public int getPriority() { return 750; }
    @Override public int getMinimumLevel() { return 2; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        StrengthReductionVisitor visitor = new StrengthReductionVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class StrengthReductionVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        StrengthReductionVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitBinaryExpr(GLSLBinaryExpr node) {
            super.visitBinaryExpr(node);
            
            // x * 2 -> x + x (addition is often faster)
            if (node.operator == GLSLBinaryExpr.Operator.MUL) {
                if (isTwo(node.right)) {
                    GLSLBinaryExpr add = new GLSLBinaryExpr(
                        GLSLBinaryExpr.Operator.ADD, 
                        node.left.copy(context.pool), 
                        node.left.copy(context.pool)
                    );
                    replaceInParent(node, add);
                    context.expressionsSimplified++;
                    changed = true;
                    return;
                }
                if (isTwo(node.left)) {
                    GLSLBinaryExpr add = new GLSLBinaryExpr(
                        GLSLBinaryExpr.Operator.ADD, 
                        node.right.copy(context.pool), 
                        node.right.copy(context.pool)
                    );
                    replaceInParent(node, add);
                    context.expressionsSimplified++;
                    changed = true;
                    return;
                }
                
                // x * power-of-2 -> x << log2(power) (for integers)
                int rightShift = getPowerOfTwo(node.right);
                if (rightShift > 0 && isIntegerExpr(node.left)) {
                    GLSLBinaryExpr shift = new GLSLBinaryExpr(
                        GLSLBinaryExpr.Operator.LEFT_SHIFT,
                        node.left,
                        new GLSLLiteralExpr((long) rightShift)
                    );
                    replaceInParent(node, shift);
                    context.expressionsSimplified++;
                    changed = true;
                    return;
                }
            }
            
            // x / power-of-2 -> x >> log2(power) (for integers)
            if (node.operator == GLSLBinaryExpr.Operator.DIV) {
                int rightShift = getPowerOfTwo(node.right);
                if (rightShift > 0 && isIntegerExpr(node.left)) {
                    GLSLBinaryExpr shift = new GLSLBinaryExpr(
                        GLSLBinaryExpr.Operator.RIGHT_SHIFT,
                        node.left,
                        new GLSLLiteralExpr((long) rightShift)
                    );
                    replaceInParent(node, shift);
                    context.expressionsSimplified++;
                    changed = true;
                    return;
                }
                
                // x / c -> x * (1/c) for float constants
                if (isFloatConstant(node.right)) {
                    double c = ((GLSLLiteralExpr) node.right).floatValue;
                    if (c != 0.0) {
                        GLSLBinaryExpr mul = new GLSLBinaryExpr(
                            GLSLBinaryExpr.Operator.MUL,
                            node.left,
                            new GLSLLiteralExpr(1.0 / c)
                        );
                        replaceInParent(node, mul);
                        context.expressionsSimplified++;
                        changed = true;
                    }
                }
            }
        }
        
        @Override
        public void visitCallExpr(GLSLCallExpr node) {
            super.visitCallExpr(node);
            
            // pow(x, 2) -> x * x
            if ("pow".equals(node.functionName) && node.arguments.size() == 2) {
                if (isTwo(node.arguments.get(1))) {
                    GLSLExpression x = node.arguments.get(0);
                    GLSLBinaryExpr mul = new GLSLBinaryExpr(
                        GLSLBinaryExpr.Operator.MUL,
                        x.copy(context.pool),
                        x.copy(context.pool)
                    );
                    replaceInParent(node, mul);
                    context.expressionsSimplified++;
                    changed = true;
                    return;
                }
                
                // pow(x, 0.5) -> sqrt(x)
                if (isHalf(node.arguments.get(1))) {
                    GLSLCallExpr sqrt = new GLSLCallExpr("sqrt");
                    sqrt.addArgument(node.arguments.get(0));
                    sqrt.isBuiltinFunction = true;
                    replaceInParent(node, sqrt);
                    context.expressionsSimplified++;
                    changed = true;
                    return;
                }
            }
            
            // length(v) == 0 comparisons could use dot(v,v) == 0 (avoids sqrt)
            // But this is a semantic change that might not be desired
        }
        
        private boolean isTwo(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                switch (lit.literalType) {
                    case INT:
                    case UINT: return lit.intValue == 2;
                    case FLOAT:
                    case DOUBLE: return lit.floatValue == 2.0;
                    default: return false;
                }
            }
            return false;
        }
        
        private boolean isHalf(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                if (lit.literalType == GLSLLiteralExpr.LiteralType.FLOAT ||
                    lit.literalType == GLSLLiteralExpr.LiteralType.DOUBLE) {
                    return lit.floatValue == 0.5;
                }
            }
            return false;
        }
        
        private int getPowerOfTwo(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                if (lit.literalType == GLSLLiteralExpr.LiteralType.INT ||
                    lit.literalType == GLSLLiteralExpr.LiteralType.UINT) {
                    long val = lit.intValue;
                    if (val > 0 && (val & (val - 1)) == 0) {
                        return Long.numberOfTrailingZeros(val);
                    }
                }
            }
            return -1;
        }
        
        private boolean isIntegerExpr(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                return lit.literalType == GLSLLiteralExpr.LiteralType.INT ||
                       lit.literalType == GLSLLiteralExpr.LiteralType.UINT;
            }
            // Could check resolved type, but for safety assume false
            return false;
        }
        
        private boolean isFloatConstant(GLSLExpression expr) {
            if (expr instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) expr;
                return lit.literalType == GLSLLiteralExpr.LiteralType.FLOAT ||
                       lit.literalType == GLSLLiteralExpr.LiteralType.DOUBLE;
            }
            return false;
        }
        
        private void replaceInParent(GLSLASTNode oldNode, GLSLASTNode newNode) {
            GLSLASTNode parent = oldNode.parent;
            if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChild(i) == oldNode) {
                        parent.setChild(i, newNode);
                        break;
                    }
                }
            }
        }
    }
}

// ============================================================================
// DEAD CODE ELIMINATION PASSES
// ============================================================================

/**
 * Removes unused variable declarations.
 */
final class DeadCodeEliminationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "DeadCodeElimination"; }
    @Override public int getPriority() { return 700; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        boolean changed = false;
        
        // Remove unused global variables (except uniforms, inputs, outputs)
        Iterator<GLSLVariableDecl> globalIter = shader.globalVariables.iterator();
        while (globalIter.hasNext()) {
            GLSLVariableDecl var = globalIter.next();
            if (canRemove(var, context)) {
                globalIter.remove();
                shader.declarations.remove(var);
                context.deadCodeRemoved++;
                changed = true;
            }
        }
        
        // Remove unused local variables
        DeadLocalVisitor visitor = new DeadLocalVisitor(context);
        shader.accept(visitor);
        changed = changed || visitor.changed;
        
        // Remove unused functions (except main)
        Iterator<GLSLFunctionDecl> funcIter = shader.functions.iterator();
        while (funcIter.hasNext()) {
            GLSLFunctionDecl func = funcIter.next();
            if (!func.isMain() && 
                context.functionCallCount.getOrDefault(func.name, 0) == 0) {
                funcIter.remove();
                shader.declarations.remove(func);
                context.deadCodeRemoved++;
                changed = true;
            }
        }
        
        return changed;
    }
    
    private boolean canRemove(GLSLVariableDecl var, GLSLOptimizationContext context) {
        // Don't remove if used
        if (context.variableUseCount.getOrDefault(var.name, 0) > 0) {
            return false;
        }
        
        // Don't remove uniforms, inputs, outputs, etc.
        if (var.type.qualifiers != null) {
            switch (var.type.qualifiers.storage) {
                case UNIFORM:
                case IN:
                case OUT:
                case ATTRIBUTE:
                case VARYING:
                case BUFFER:
                case SHARED:
                    return false;
                default:
                    break;
            }
        }
        
        // Check for side effects in initializer
        if (var.initializer != null && hasSideEffects(var.initializer)) {
            return false;
        }
        
        return true;
    }
    
    private boolean hasSideEffects(GLSLExpression expr) {
        if (expr instanceof GLSLCallExpr) {
            // Function calls might have side effects
            GLSLCallExpr call = (GLSLCallExpr) expr;
            // Built-in math functions are pure
            if (call.isBuiltinFunction && isPureBuiltin(call.functionName)) {
                // Check arguments
                for (GLSLExpression arg : call.arguments) {
                    if (hasSideEffects(arg)) return true;
                }
                return false;
            }
            return true; // Assume user functions have side effects
        }
        
        if (expr instanceof GLSLBinaryExpr) {
            GLSLBinaryExpr binary = (GLSLBinaryExpr) expr;
            if (binary.operator.isAssignment()) {
                return true;
            }
            return hasSideEffects(binary.left) || hasSideEffects(binary.right);
        }
        
        if (expr instanceof GLSLUnaryExpr) {
            GLSLUnaryExpr unary = (GLSLUnaryExpr) expr;
            if (unary.operator == GLSLUnaryExpr.Operator.PRE_INCREMENT ||
                unary.operator == GLSLUnaryExpr.Operator.PRE_DECREMENT ||
                unary.operator == GLSLUnaryExpr.Operator.POST_INCREMENT ||
                unary.operator == GLSLUnaryExpr.Operator.POST_DECREMENT) {
                return true;
            }
            return hasSideEffects(unary.operand);
        }
        
        return false;
    }
    
    private boolean isPureBuiltin(String name) {
        // All math functions are pure
        switch (name) {
            case "sin": case "cos": case "tan":
            case "asin": case "acos": case "atan":
            case "sinh": case "cosh": case "tanh":
            case "pow": case "exp": case "log": case "exp2": case "log2":
            case "sqrt": case "inversesqrt":
            case "abs": case "sign": case "floor": case "ceil": case "fract":
            case "mod": case "min": case "max": case "clamp": case "mix":
            case "step": case "smoothstep":
            case "length": case "distance": case "dot": case "cross":
            case "normalize": case "reflect": case "refract":
            case "radians": case "degrees":
            case "lessThan": case "lessThanEqual":
            case "greaterThan": case "greaterThanEqual":
            case "equal": case "notEqual":
            case "any": case "all": case "not":
                return true;
            default:
                return false;
        }
    }
    
    private static class DeadLocalVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        DeadLocalVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitDeclarationStmt(GLSLDeclarationStmt node) {
            Iterator<GLSLVariableDecl> iter = node.declarations.iterator();
            while (iter.hasNext()) {
                GLSLVariableDecl decl = iter.next();
                if (context.isUnused(decl.name) && 
                    (decl.initializer == null || !hasEffect(decl.initializer))) {
                    iter.remove();
                    context.deadCodeRemoved++;
                    changed = true;
                }
            }
            
            // If all declarations removed, mark statement for removal
            if (node.declarations.isEmpty()) {
                node.setFlag(GLSLASTNode.FLAG_DEAD);
            }
        }
        
        private boolean hasEffect(GLSLExpression expr) {
            if (expr instanceof GLSLCallExpr) {
                return true; // Assume function calls have effects
            }
            return false;
        }
    }
}

/**
 * Removes dead branches (if(false), if(true), etc.)
 */
final class DeadBranchEliminationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "DeadBranchElimination"; }
    @Override public int getPriority() { return 650; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        DeadBranchVisitor visitor = new DeadBranchVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class DeadBranchVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        DeadBranchVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitIfStmt(GLSLIfStmt node) {
            // First process children
            super.visitIfStmt(node);
            
            // Check for constant condition
            if (node.condition instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) node.condition;
                if (lit.literalType == GLSLLiteralExpr.LiteralType.BOOL) {
                    // Replace if statement with appropriate branch
                    GLSLASTNode parent = node.parent;
                    if (parent instanceof GLSLBlockStmt) {
                        GLSLBlockStmt block = (GLSLBlockStmt) parent;
                        int index = block.statements.indexOf(node);
                        if (index >= 0) {
                            block.statements.remove(index);
                            
                            GLSLStatement replacement = lit.boolValue ? 
                                node.thenBranch : node.elseBranch;
                            
                            if (replacement != null) {
                                // If replacement is a block, inline its statements
                                if (replacement instanceof GLSLBlockStmt) {
                                    GLSLBlockStmt repBlock = (GLSLBlockStmt) replacement;
                                    for (int i = 0; i < repBlock.statements.size(); i++) {
                                        block.statements.add(index + i, repBlock.statements.get(i));
                                    }
                                } else {
                                    block.statements.add(index, replacement);
                                }
                            }
                            
                            context.deadCodeRemoved++;
                            changed = true;
                        }
                    }
                }
            }
        }
        
        @Override
        public void visitTernaryExpr(GLSLTernaryExpr node) {
            super.visitTernaryExpr(node);
            
            if (node.condition instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) node.condition;
                if (lit.literalType == GLSLLiteralExpr.LiteralType.BOOL) {
                    GLSLExpression replacement = lit.boolValue ? 
                        node.thenExpr : node.elseExpr;
                    
                    replaceInParent(node, replacement);
                    context.deadCodeRemoved++;
                    changed = true;
                }
            }
        }
        
        @Override
        public void visitWhileStmt(GLSLWhileStmt node) {
            super.visitWhileStmt(node);
            
            // while(false) -> remove entirely
            if (node.condition instanceof GLSLLiteralExpr) {
                GLSLLiteralExpr lit = (GLSLLiteralExpr) node.condition;
                if (lit.literalType == GLSLLiteralExpr.LiteralType.BOOL && !lit.boolValue) {
                    node.setFlag(GLSLASTNode.FLAG_DEAD);
                    context.deadCodeRemoved++;
                    changed = true;
                }
            }
        }
        
        private void replaceInParent(GLSLASTNode oldNode, GLSLASTNode newNode) {
            GLSLASTNode parent = oldNode.parent;
            if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChild(i) == oldNode) {
                        parent.setChild(i, newNode);
                        break;
                    }
                }
            }
        }
    }
}

/**
 * Removes code after return/discard/break/continue.
 */
final class UnreachableCodeEliminationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "UnreachableCodeElimination"; }
    @Override public int getPriority() { return 600; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        UnreachableVisitor visitor = new UnreachableVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class UnreachableVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        UnreachableVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitBlockStmt(GLSLBlockStmt node) {
            boolean foundTerminator = false;
            
            for (int i = 0; i < node.statements.size(); i++) {
                GLSLStatement stmt = node.statements.get(i);
                
                if (foundTerminator) {
                    // This statement is unreachable
                    stmt.setFlag(GLSLASTNode.FLAG_DEAD);
                    context.deadCodeRemoved++;
                    changed = true;
                } else if (isTerminator(stmt)) {
                    foundTerminator = true;
                }
                
                stmt.accept(this);
            }
            
            // Remove dead statements
            node.statements.removeIf(s -> s.hasFlag(GLSLASTNode.FLAG_DEAD));
        }
        
        private boolean isTerminator(GLSLStatement stmt) {
            return stmt instanceof GLSLReturnStmt ||
                   stmt instanceof GLSLDiscardStmt ||
                   stmt instanceof GLSLBreakStmt ||
                   stmt instanceof GLSLContinueStmt;
        }
    }
}

// ============================================================================
// VARIABLE INLINING PASS
// ============================================================================

/**
 * Inlines variables that are only used once.
 */
final class VariableInliningPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "VariableInlining"; }
    @Override public int getPriority() { return 550; }
    @Override public int getMinimumLevel() { return 2; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        // Find variables used exactly once
        Map<String, GLSLExpression> inlineValues = new HashMap<>();
        
        for (GLSLVariableDecl var : shader.globalVariables) {
            if (canInline(var, context)) {
                inlineValues.put(var.name, var.initializer);
            }
        }
        
        if (inlineValues.isEmpty()) {
            return false;
        }
        
        InliningVisitor visitor = new InliningVisitor(inlineValues, context);
        shader.accept(visitor);
        
        // Remove inlined variable declarations
        if (visitor.changed) {
            shader.globalVariables.removeIf(v -> inlineValues.containsKey(v.name));
            shader.declarations.removeIf(d -> 
                d instanceof GLSLVariableDecl && inlineValues.containsKey(((GLSLVariableDecl) d).name)
            );
        }
        
        return visitor.changed;
    }
    
    private boolean canInline(GLSLVariableDecl var, GLSLOptimizationContext context) {
        // Only inline if used exactly once
        int useCount = context.variableUseCount.getOrDefault(var.name, 0);
        if (useCount != 1) return false;
        
        // Must have initializer
        if (var.initializer == null) return false;
        
        // Don't inline uniforms, inputs, outputs
        if (var.type.qualifiers != null) {
            switch (var.type.qualifiers.storage) {
                case UNIFORM:
                case IN:
                case OUT:
                case ATTRIBUTE:
                case VARYING:
                case BUFFER:
                case SHARED:
                    return false;
                default:
                    break;
            }
        }
        
        // Only inline simple expressions to avoid code bloat
        return isSimpleExpression(var.initializer);
    }
    
    private boolean isSimpleExpression(GLSLExpression expr) {
        if (expr instanceof GLSLLiteralExpr) return true;
        if (expr instanceof GLSLIdentifierExpr) return true;
        
        if (expr instanceof GLSLUnaryExpr) {
            return isSimpleExpression(((GLSLUnaryExpr) expr).operand);
        }
        
        if (expr instanceof GLSLBinaryExpr) {
            GLSLBinaryExpr binary = (GLSLBinaryExpr) expr;
            return isSimpleExpression(binary.left) && isSimpleExpression(binary.right);
        }
        
        if (expr instanceof GLSLCallExpr) {
            GLSLCallExpr call = (GLSLCallExpr) expr;
            if (call.isConstructor || call.isBuiltinFunction) {
                for (GLSLExpression arg : call.arguments) {
                    if (!isSimpleExpression(arg)) return false;
                }
                return call.arguments.size() <= 4;
            }
        }
        
        return false;
    }
    
    private static class InliningVisitor extends GLSLASTBaseVisitor {
        private final Map<String, GLSLExpression> inlineValues;
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        InliningVisitor(Map<String, GLSLExpression> inlineValues, GLSLOptimizationContext context) {
            this.inlineValues = inlineValues;
            this.context = context;
        }
        
        @Override
        public void visitIdentifierExpr(GLSLIdentifierExpr node) {
            GLSLExpression replacement = inlineValues.get(node.name);
            if (replacement != null) {
                GLSLExpression copy = (GLSLExpression) replacement.copy(context.pool);
                replaceInParent(node, copy);
                context.variablesInlined++;
                changed = true;
            }
        }
        
        private void replaceInParent(GLSLASTNode oldNode, GLSLASTNode newNode) {
            GLSLASTNode parent = oldNode.parent;
            if (parent != null) {
                for (int i = 0; i < parent.getChildCount(); i++) {
                    if (parent.getChild(i) == oldNode) {
                        parent.setChild(i, newNode);
                        break;
                    }
                }
            }
        }
    }
}

// ============================================================================
// COMMON SUBEXPRESSION ELIMINATION
// ============================================================================

/**
 * Identifies and caches common subexpressions.
 * Note: This is a simplified version that only identifies, not transforms.
 */
final class CommonSubexpressionPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "CommonSubexpression"; }
    @Override public int getPriority() { return 500; }
    @Override public int getMinimumLevel() { return 3; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        // CSE is complex - for now just identify opportunities
        // Full implementation would create temporary variables
        return false;
    }
}

// ============================================================================
// PRECISION OPTIMIZATION PASS
// ============================================================================

/**
 * Optimizes precision qualifiers for better performance.
 */
final class PrecisionOptimizationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "PrecisionOptimization"; }
    @Override public int getPriority() { return 400; }
    @Override public int getMinimumLevel() { return 2; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) {
        // Only applies to ES or when targeting mobile
        return context.targetVersion.versionNumber >= 300;
    }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        // This would analyze variable usage and potentially downgrade precision
        // For now, just a placeholder
        return false;
    }
}

// ============================================================================
// SWIZZLE OPTIMIZATION PASS
// ============================================================================

/**
 * Optimizes vector swizzle operations.
 */
final class SwizzleOptimizationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "SwizzleOptimization"; }
    @Override public int getPriority() { return 350; }
    @Override public int getMinimumLevel() { return 2; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        SwizzleVisitor visitor = new SwizzleVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class SwizzleVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        SwizzleVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitMemberExpr(GLSLMemberExpr node) {
            super.visitMemberExpr(node);
            
            // Collapse chained swizzles: v.xy.x -> v.x
            if (node.isSwizzle && node.object instanceof GLSLMemberExpr) {
                GLSLMemberExpr inner = (GLSLMemberExpr) node.object;
                if (inner.isSwizzle) {
                    String collapsed = collapseSwizzle(inner.member, node.member);
                    if (collapsed != null) {
                        node.object = inner.object;
                        node.member = collapsed;
                        inner.object.parent = node;
                        context.expressionsSimplified++;
                        changed = true;
                    }
                }
            }
            
            // Identity swizzle: v.xyzw -> v (for vec4)
            // v.xyz -> v (for vec3), etc.
            if (node.isSwizzle && isIdentitySwizzle(node.member)) {
                // Would need type info to safely remove
            }
        }
        
        private String collapseSwizzle(String first, String second) {
            // first is the inner swizzle, second is the outer
            // Example: first = "xy", second = "x" -> result = "x"
            // Example: first = "zw", second = "yx" -> result = "wz"
            
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < second.length(); i++) {
                char c = second.charAt(i);
                int index = "xyzwrgbastpq".indexOf(c) % 4;
                if (index < 0 || index >= first.length()) {
                    return null; // Invalid swizzle
                }
                result.append(first.charAt(index));
            }
            return result.toString();
        }
        
        private boolean isIdentitySwizzle(String swizzle) {
            return swizzle.equals("xyzw") || swizzle.equals("rgba") || swizzle.equals("stpq") ||
                   swizzle.equals("xyz") || swizzle.equals("rgb") || swizzle.equals("stp") ||
                   swizzle.equals("xy") || swizzle.equals("rg") || swizzle.equals("st") ||
                   swizzle.equals("x") || swizzle.equals("r") || swizzle.equals("s");
        }
    }
}

// ============================================================================
// VECTOR CONSTRUCTOR OPTIMIZATION PASS
// ============================================================================

/**
 * Optimizes vector and matrix constructor calls.
 */
final class VectorConstructorOptimizationPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "VectorConstructorOptimization"; }
    @Override public int getPriority() { return 300; }
    @Override public int getMinimumLevel() { return 2; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        VectorConstructorVisitor visitor = new VectorConstructorVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class VectorConstructorVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        VectorConstructorVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitCallExpr(GLSLCallExpr node) {
            super.visitCallExpr(node);
            
            if (!node.isConstructor) return;
            
            // vec3(v.x, v.y, v.z) -> v.xyz (if v is vec3 or larger)
            // This needs type information to implement safely
            
            // vec4(1.0, 1.0, 1.0, 1.0) -> vec4(1.0) (splat optimization)
            if (isVectorConstructor(node.functionName) && allSameConstant(node.arguments)) {
                if (node.arguments.size() > 1) {
                    GLSLExpression firstArg = node.arguments.get(0);
                    node.arguments.clear();
                    node.addArgument(firstArg);
                    context.expressionsSimplified++;
                    changed = true;
                }
            }
        }
        
        private boolean isVectorConstructor(String name) {
            return name.matches("(b|i|u|d)?vec[234]");
        }
        
        private boolean allSameConstant(List<GLSLExpression> args) {
            if (args.isEmpty()) return false;
            
            GLSLExpression first = args.get(0);
            if (!(first instanceof GLSLLiteralExpr)) return false;
            
            GLSLLiteralExpr firstLit = (GLSLLiteralExpr) first;
            
            for (int i = 1; i < args.size(); i++) {
                if (!(args.get(i) instanceof GLSLLiteralExpr)) return false;
                GLSLLiteralExpr lit = (GLSLLiteralExpr) args.get(i);
                
                if (firstLit.literalType != lit.literalType) return false;
                
                switch (firstLit.literalType) {
                    case INT:
                    case UINT:
                        if (firstLit.intValue != lit.intValue) return false;
                        break;
                    case FLOAT:
                    case DOUBLE:
                        if (firstLit.floatValue != lit.floatValue) return false;
                        break;
                    case BOOL:
                        if (firstLit.boolValue != lit.boolValue) return false;
                        break;
                }
            }
            
            return true;
        }
    }
}

// ============================================================================
// CLEANUP PASSES
// ============================================================================

/**
 * Removes empty blocks and simplifies nested blocks.
 */
final class EmptyBlockRemovalPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "EmptyBlockRemoval"; }
    @Override public int getPriority() { return 100; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        EmptyBlockVisitor visitor = new EmptyBlockVisitor(context);
        shader.accept(visitor);
        return visitor.changed;
    }
    
    private static class EmptyBlockVisitor extends GLSLASTBaseVisitor {
        private final GLSLOptimizationContext context;
        boolean changed = false;
        
        EmptyBlockVisitor(GLSLOptimizationContext context) {
            this.context = context;
        }
        
        @Override
        public void visitBlockStmt(GLSLBlockStmt node) {
            // First process children
            super.visitBlockStmt(node);
            
            // Remove dead statements
            boolean removed = node.statements.removeIf(s -> s.hasFlag(GLSLASTNode.FLAG_DEAD));
            if (removed) {
                changed = true;
            }
            
            // Flatten nested blocks: { { stmt; } } -> { stmt; }
            for (int i = 0; i < node.statements.size(); i++) {
                GLSLStatement stmt = node.statements.get(i);
                if (stmt instanceof GLSLBlockStmt) {
                    GLSLBlockStmt inner = (GLSLBlockStmt) stmt;
                    // Only flatten if inner block has no local declarations
                    if (inner.localSymbols == null || inner.localSymbols.getSymbols().isEmpty()) {
                        node.statements.remove(i);
                        for (int j = 0; j < inner.statements.size(); j++) {
                            node.statements.add(i + j, inner.statements.get(j));
                        }
                        changed = true;
                        i--; // Re-check at same position
                    }
                }
            }
        }
        
        @Override
        public void visitIfStmt(GLSLIfStmt node) {
            super.visitIfStmt(node);
            
            // if(cond) {} else {} -> remove (unless condition has side effects)
            if (isEmpty(node.thenBranch) && isEmpty(node.elseBranch)) {
                node.setFlag(GLSLASTNode.FLAG_DEAD);
                context.deadCodeRemoved++;
                changed = true;
            }
            
            // if(cond) {} else stmt -> if(!cond) stmt
            if (isEmpty(node.thenBranch) && !isEmpty(node.elseBranch)) {
                GLSLUnaryExpr notCond = new GLSLUnaryExpr(
                    GLSLUnaryExpr.Operator.NOT, node.condition);
                node.condition = notCond;
                node.thenBranch = node.elseBranch;
                node.elseBranch = null;
                context.expressionsSimplified++;
                changed = true;
            }
        }
        
        private boolean isEmpty(GLSLStatement stmt) {
            if (stmt == null) return true;
            if (stmt instanceof GLSLBlockStmt) {
                return ((GLSLBlockStmt) stmt).statements.isEmpty();
            }
            if (stmt instanceof GLSLExpressionStmt) {
                return ((GLSLExpressionStmt) stmt).expression == null;
            }
            return false;
        }
    }
}

/**
 * Final cleanup of declarations.
 */
final class DeclarationCleanupPass implements GLSLOptimizationPass {
    
    @Override public String getName() { return "DeclarationCleanup"; }
    @Override public int getPriority() { return 50; }
    @Override public int getMinimumLevel() { return 1; }
    @Override public boolean shouldRun(GLSLOptimizationContext context) { return true; }
    
    @Override
    public boolean optimize(GLSLShaderAST shader, GLSLOptimizationContext context) {
        boolean changed = false;
        
        // Remove declarations marked as dead
        changed |= shader.declarations.removeIf(d -> d.hasFlag(GLSLASTNode.FLAG_DEAD));
        
        // Remove empty declaration statements in functions
        for (GLSLFunctionDecl func : shader.functions) {
            if (func.body != null) {
                changed |= cleanupBlock(func.body);
            }
        }
        
        return changed;
    }
    
    private boolean cleanupBlock(GLSLBlockStmt block) {
        boolean changed = block.statements.removeIf(s -> {
            if (s instanceof GLSLDeclarationStmt) {
                return ((GLSLDeclarationStmt) s).declarations.isEmpty();
            }
            return s.hasFlag(GLSLASTNode.FLAG_DEAD);
        });
        
        for (GLSLStatement stmt : block.statements) {
            if (stmt instanceof GLSLBlockStmt) {
                changed |= cleanupBlock((GLSLBlockStmt) stmt);
            } else if (stmt instanceof GLSLIfStmt) {
                GLSLIfStmt ifStmt = (GLSLIfStmt) stmt;
                if (ifStmt.thenBranch instanceof GLSLBlockStmt) {
                    changed |= cleanupBlock((GLSLBlockStmt) ifStmt.thenBranch);
                }
                if (ifStmt.elseBranch instanceof GLSLBlockStmt) {
                    changed |= cleanupBlock((GLSLBlockStmt) ifStmt.elseBranch);
                }
            } else if (stmt instanceof GLSLForStmt) {
                if (((GLSLForStmt) stmt).body instanceof GLSLBlockStmt) {
                    changed |= cleanupBlock((GLSLBlockStmt) ((GLSLForStmt) stmt).body);
                }
            } else if (stmt instanceof GLSLWhileStmt) {
                if (((GLSLWhileStmt) stmt).body instanceof GLSLBlockStmt) {
                    changed |= cleanupBlock((GLSLBlockStmt) ((GLSLWhileStmt) stmt).body);
                }
            }
        }
        
        return changed;
    }
}

// ============================================================================
// GLSL CODE GENERATOR
// ============================================================================

/**
 * High-performance GLSL code generator that converts AST back to source code.
 * Uses pooled StringBuilders and minimizes allocations.
 */
final class GLSLCodeGenerator {
    
    private final GLSLMemoryPool pool;
    
    // Operator precedence for minimal parenthesization
    private static final Map<GLSLBinaryExpr.Operator, Integer> BINARY_PRECEDENCE = new EnumMap<>(GLSLBinaryExpr.Operator.class);
    private static final Map<GLSLBinaryExpr.Operator, Boolean> RIGHT_ASSOCIATIVE = new EnumMap<>(GLSLBinaryExpr.Operator.class);
    
    static {
        // Precedence levels (higher = binds tighter)
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.COMMA, 1);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.ASSIGN, 2);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.ADD_ASSIGN, 2);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.SUB_ASSIGN, 2);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.MUL_ASSIGN, 2);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.DIV_ASSIGN, 2);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.MOD_ASSIGN, 2);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.LEFT_SHIFT_ASSIGN, 2);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.RIGHT_SHIFT_ASSIGN, 2);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.AND_ASSIGN, 2);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.OR_ASSIGN, 2);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.XOR_ASSIGN, 2);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.OR, 4);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.XOR, 5);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.AND, 6);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.BIT_OR, 7);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.BIT_XOR, 8);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.BIT_AND, 9);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.EQ, 10);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.NE, 10);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.LT, 11);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.GT, 11);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.LE, 11);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.GE, 11);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.LEFT_SHIFT, 12);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.RIGHT_SHIFT, 12);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.ADD, 13);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.SUB, 13);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.MUL, 14);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.DIV, 14);
        BINARY_PRECEDENCE.put(GLSLBinaryExpr.Operator.MOD, 14);
        
        // Right associative operators
        for (GLSLBinaryExpr.Operator op : GLSLBinaryExpr.Operator.values()) {
            RIGHT_ASSOCIATIVE.put(op, op.isAssignment());
        }
    }
    
    GLSLCodeGenerator(GLSLMemoryPool pool) {
        this.pool = pool;
    }
    
    /**
     * Generate GLSL source code from AST.
     */
    String generate(GLSLShaderAST ast, GLSLVersion targetVersion) {
        StringBuilder sb = pool.acquireStringBuilder();
        
        try {
            CodeGenContext ctx = new CodeGenContext(sb, targetVersion);
            
            // Version directive
            generateVersionDirective(ast, ctx);
            
            // Extension directives
            for (GLSLExtensionDecl ext : ast.extensions) {
                generateExtension(ext, ctx);
            }
            
            if (!ast.extensions.isEmpty()) {
                ctx.newLine();
            }
            
            // Precision declarations
            for (GLSLPrecisionDecl prec : ast.precisionDecls) {
                generatePrecision(prec, ctx);
            }
            
            if (!ast.precisionDecls.isEmpty()) {
                ctx.newLine();
            }
            
            // Struct declarations
            for (GLSLStructDecl struct : ast.structs) {
                generateStruct(struct, ctx);
                ctx.newLine();
            }
            
            // Interface blocks
            for (GLSLInterfaceBlock block : ast.interfaceBlocks) {
                generateInterfaceBlock(block, ctx);
                ctx.newLine();
            }
            
            // Global variables
            for (GLSLVariableDecl var : ast.globalVariables) {
                generateVariableDecl(var, ctx);
                ctx.append(";").newLine();
            }
            
            if (!ast.globalVariables.isEmpty()) {
                ctx.newLine();
            }
            
            // Functions
            for (int i = 0; i < ast.functions.size(); i++) {
                if (i > 0) ctx.newLine();
                generateFunction(ast.functions.get(i), ctx);
            }
            
            return sb.toString();
            
        } finally {
            pool.releaseStringBuilder(sb);
        }
    }
    
    // ========================================================================
    // VERSION & EXTENSION
    // ========================================================================
    
    private void generateVersionDirective(GLSLShaderAST ast, CodeGenContext ctx) {
        ctx.append("#version ").append(ast.version.versionNumber);
        
        if (ast.profile != null && !ast.profile.isEmpty()) {
            ctx.append(" ").append(ast.profile);
        } else if (ast.version.versionNumber >= 150) {
            // Default to core for 1.50+
            ctx.append(" core");
        }
        
        ctx.newLine();
    }
    
    private void generateExtension(GLSLExtensionDecl ext, CodeGenContext ctx) {
        ctx.append("#extension ").append(ext.extensionName)
           .append(" : ").append(ext.behavior).newLine();
    }
    
    private void generatePrecision(GLSLPrecisionDecl prec, CodeGenContext ctx) {
        ctx.append("precision ");
        
        switch (prec.precision) {
            case HIGHP: ctx.append("highp"); break;
            case MEDIUMP: ctx.append("mediump"); break;
            case LOWP: ctx.append("lowp"); break;
            default: break;
        }
        
        ctx.append(" ");
        generateType(prec.type, ctx, false);
        ctx.append(";").newLine();
    }
    
    // ========================================================================
    // TYPE GENERATION
    // ========================================================================
    
    private void generateType(GLSLType type, CodeGenContext ctx, boolean includeArrayDims) {
        // Qualifiers
        if (type.qualifiers != null) {
            generateQualifiers(type.qualifiers, ctx);
        }
        
        // Base type
        ctx.append(baseTypeToString(type.baseType, type.structName));
        
        // Array dimensions (on type)
        if (includeArrayDims && type.arrayDimensions != null) {
            for (int dim : type.arrayDimensions) {
                ctx.append("[");
                if (dim > 0) ctx.append(dim);
                ctx.append("]");
            }
        }
    }
    
    private void generateQualifiers(GLSLTypeQualifiers quals, CodeGenContext ctx) {
        if (quals.invariant) ctx.append("invariant ");
        if (quals.precise) ctx.append("precise ");
        
        if (quals.layout != null) {
            generateLayout(quals.layout, ctx);
            ctx.append(" ");
        }
        
        if (quals.centroid) ctx.append("centroid ");
        if (quals.sample) ctx.append("sample ");
        if (quals.patch) ctx.append("patch ");
        
        switch (quals.interpolation) {
            case FLAT: ctx.append("flat "); break;
            case SMOOTH: ctx.append("smooth "); break;
            case NOPERSPECTIVE: ctx.append("noperspective "); break;
            default: break;
        }
        
        switch (quals.storage) {
            case CONST: ctx.append("const "); break;
            case IN: ctx.append("in "); break;
            case OUT: ctx.append("out "); break;
            case INOUT: ctx.append("inout "); break;
            case UNIFORM: ctx.append("uniform "); break;
            case BUFFER: ctx.append("buffer "); break;
            case SHARED: ctx.append("shared "); break;
            case ATTRIBUTE: ctx.append("attribute "); break;
            case VARYING: ctx.append("varying "); break;
            default: break;
        }
        
        switch (quals.precision) {
            case HIGHP: ctx.append("highp "); break;
            case MEDIUMP: ctx.append("mediump "); break;
            case LOWP: ctx.append("lowp "); break;
            default: break;
        }
        
        if (quals.coherent) ctx.append("coherent ");
        if (quals.volatileQ) ctx.append("volatile ");
        if (quals.restrict) ctx.append("restrict ");
        if (quals.readonly) ctx.append("readonly ");
        if (quals.writeonly) ctx.append("writeonly ");
    }
    
    private void generateLayout(GLSLLayoutQualifier layout, CodeGenContext ctx) {
        ctx.append("layout(");
        boolean first = true;
        
        if (layout.location >= 0) {
            ctx.append("location = ").append(layout.location);
            first = false;
        }
        
        if (layout.binding >= 0) {
            if (!first) ctx.append(", ");
            ctx.append("binding = ").append(layout.binding);
            first = false;
        }
        
        if (layout.offset >= 0) {
            if (!first) ctx.append(", ");
            ctx.append("offset = ").append(layout.offset);
            first = false;
        }
        
        if (layout.component >= 0) {
            if (!first) ctx.append(", ");
            ctx.append("component = ").append(layout.component);
            first = false;
        }
        
        if (layout.set >= 0) {
            if (!first) ctx.append(", ");
            ctx.append("set = ").append(layout.set);
            first = false;
        }
        
        if (layout.index >= 0) {
            if (!first) ctx.append(", ");
            ctx.append("index = ").append(layout.index);
            first = false;
        }
        
        // Packing
        if (layout.packing != GLSLLayoutQualifier.PackingMode.NONE) {
            if (!first) ctx.append(", ");
            ctx.append(layout.packing.name().toLowerCase());
            first = false;
        }
        
        // Matrix layout
        if (layout.matrixLayout != GLSLLayoutQualifier.MatrixLayout.NONE) {
            if (!first) ctx.append(", ");
            ctx.append(layout.matrixLayout == GLSLLayoutQualifier.MatrixLayout.ROW_MAJOR ? 
                      "row_major" : "column_major");
            first = false;
        }
        
        // Compute shader local size
        if (layout.localSizeX >= 0) {
            if (!first) ctx.append(", ");
            ctx.append("local_size_x = ").append(layout.localSizeX);
            first = false;
        }
        if (layout.localSizeY >= 0) {
            if (!first) ctx.append(", ");
            ctx.append("local_size_y = ").append(layout.localSizeY);
            first = false;
        }
        if (layout.localSizeZ >= 0) {
            if (!first) ctx.append(", ");
            ctx.append("local_size_z = ").append(layout.localSizeZ);
            first = false;
        }
        
        // Geometry/tessellation
        if (layout.primitiveType != null) {
            if (!first) ctx.append(", ");
            ctx.append(layout.primitiveType);
            first = false;
        }
        if (layout.maxVertices >= 0) {
            if (!first) ctx.append(", ");
            ctx.append("max_vertices = ").append(layout.maxVertices);
            first = false;
        }
        if (layout.vertices >= 0) {
            if (!first) ctx.append(", ");
            ctx.append("vertices = ").append(layout.vertices);
            first = false;
        }
        if (layout.invocations >= 0) {
            if (!first) ctx.append(", ");
            ctx.append("invocations = ").append(layout.invocations);
            first = false;
        }
        
        // Fragment
        if (layout.originUpperLeft) {
            if (!first) ctx.append(", ");
            ctx.append("origin_upper_left");
            first = false;
        }
        if (layout.pixelCenterInteger) {
            if (!first) ctx.append(", ");
            ctx.append("pixel_center_integer");
            first = false;
        }
        if (layout.earlyFragmentTests) {
            if (!first) ctx.append(", ");
            ctx.append("early_fragment_tests");
            first = false;
        }
        if (layout.depthAny) {
            if (!first) ctx.append(", ");
            ctx.append("depth_any");
            first = false;
        }
        if (layout.depthGreater) {
            if (!first) ctx.append(", ");
            ctx.append("depth_greater");
            first = false;
        }
        if (layout.depthLess) {
            if (!first) ctx.append(", ");
            ctx.append("depth_less");
            first = false;
        }
        if (layout.depthUnchanged) {
            if (!first) ctx.append(", ");
            ctx.append("depth_unchanged");
            first = false;
        }
        
        // Image format
        if (layout.imageFormat != null) {
            if (!first) ctx.append(", ");
            ctx.append(layout.imageFormat);
            first = false;
        }
        
        // Other parameters
        if (layout.otherParams != null) {
            for (Map.Entry<String, String> entry : layout.otherParams.entrySet()) {
                if (!first) ctx.append(", ");
                ctx.append(entry.getKey());
                if (!entry.getValue().isEmpty()) {
                    ctx.append(" = ").append(entry.getValue());
                }
                first = false;
            }
        }
        
        ctx.append(")");
    }
    
    private String baseTypeToString(GLSLType.BaseType baseType, String structName) {
        if (baseType == GLSLType.BaseType.STRUCT) {
            return structName != null ? structName : "struct";
        }
        
        String name = baseType.name().toLowerCase();
        
        // Handle special naming conventions
        switch (baseType) {
            case SAMPLER_1D: return "sampler1D";
            case SAMPLER_2D: return "sampler2D";
            case SAMPLER_3D: return "sampler3D";
            case SAMPLER_CUBE: return "samplerCube";
            case SAMPLER_1D_SHADOW: return "sampler1DShadow";
            case SAMPLER_2D_SHADOW: return "sampler2DShadow";
            case SAMPLER_CUBE_SHADOW: return "samplerCubeShadow";
            case SAMPLER_1D_ARRAY: return "sampler1DArray";
            case SAMPLER_2D_ARRAY: return "sampler2DArray";
            case SAMPLER_1D_ARRAY_SHADOW: return "sampler1DArrayShadow";
            case SAMPLER_2D_ARRAY_SHADOW: return "sampler2DArrayShadow";
            case SAMPLER_CUBE_ARRAY: return "samplerCubeArray";
            case SAMPLER_CUBE_ARRAY_SHADOW: return "samplerCubeArrayShadow";
            case SAMPLER_2D_RECT: return "sampler2DRect";
            case SAMPLER_2D_RECT_SHADOW: return "sampler2DRectShadow";
            case SAMPLER_BUFFER: return "samplerBuffer";
            case SAMPLER_2D_MS: return "sampler2DMS";
            case SAMPLER_2D_MS_ARRAY: return "sampler2DMSArray";
            case ISAMPLER_1D: return "isampler1D";
            case ISAMPLER_2D: return "isampler2D";
            case ISAMPLER_3D: return "isampler3D";
            case ISAMPLER_CUBE: return "isamplerCube";
            case ISAMPLER_1D_ARRAY: return "isampler1DArray";
            case ISAMPLER_2D_ARRAY: return "isampler2DArray";
            case ISAMPLER_CUBE_ARRAY: return "isamplerCubeArray";
            case ISAMPLER_2D_RECT: return "isampler2DRect";
            case ISAMPLER_BUFFER: return "isamplerBuffer";
            case ISAMPLER_2D_MS: return "isampler2DMS";
            case ISAMPLER_2D_MS_ARRAY: return "isampler2DMSArray";
            case USAMPLER_1D: return "usampler1D";
            case USAMPLER_2D: return "usampler2D";
            case USAMPLER_3D: return "usampler3D";
            case USAMPLER_CUBE: return "usamplerCube";
            case USAMPLER_1D_ARRAY: return "usampler1DArray";
            case USAMPLER_2D_ARRAY: return "usampler2DArray";
            case USAMPLER_CUBE_ARRAY: return "usamplerCubeArray";
            case USAMPLER_2D_RECT: return "usampler2DRect";
            case USAMPLER_BUFFER: return "usamplerBuffer";
            case USAMPLER_2D_MS: return "usampler2DMS";
            case USAMPLER_2D_MS_ARRAY: return "usampler2DMSArray";
            case ATOMIC_UINT: return "atomic_uint";
            case IMAGE_1D: return "image1D";
            case IMAGE_2D: return "image2D";
            case IMAGE_3D: return "image3D";
            case IMAGE_CUBE: return "imageCube";
            case IMAGE_1D_ARRAY: return "image1DArray";
            case IMAGE_2D_ARRAY: return "image2DArray";
            case IMAGE_CUBE_ARRAY: return "imageCubeArray";
            case IMAGE_2D_RECT: return "image2DRect";
            case IMAGE_BUFFER: return "imageBuffer";
            case IMAGE_2D_MS: return "image2DMS";
            case IMAGE_2D_MS_ARRAY: return "image2DMSArray";
            // Integer images
            case IIMAGE_1D: return "iimage1D";
            case IIMAGE_2D: return "iimage2D";
            case IIMAGE_3D: return "iimage3D";
            // Unsigned images
            case UIMAGE_1D: return "uimage1D";
            case UIMAGE_2D: return "uimage2D";
            case UIMAGE_3D: return "uimage3D";
            // Non-square matrices
            case MAT2X2: return "mat2x2";
            case MAT2X3: return "mat2x3";
            case MAT2X4: return "mat2x4";
            case MAT3X2: return "mat3x2";
            case MAT3X3: return "mat3x3";
            case MAT3X4: return "mat3x4";
            case MAT4X2: return "mat4x2";
            case MAT4X3: return "mat4x3";
            case MAT4X4: return "mat4x4";
            case DMAT2X2: return "dmat2x2";
            case DMAT2X3: return "dmat2x3";
            case DMAT2X4: return "dmat2x4";
            case DMAT3X2: return "dmat3x2";
            case DMAT3X3: return "dmat3x3";
            case DMAT3X4: return "dmat3x4";
            case DMAT4X2: return "dmat4x2";
            case DMAT4X3: return "dmat4x3";
            case DMAT4X4: return "dmat4x4";
            default:
                return name;
        }
    }
    
    // ========================================================================
    // DECLARATION GENERATION
    // ========================================================================
    
    private void generateStruct(GLSLStructDecl struct, CodeGenContext ctx) {
        ctx.append("struct ");
        if (struct.name != null) {
            ctx.append(struct.name).append(" ");
        }
        ctx.append("{").newLine();
        ctx.indent();
        
        for (GLSLVariableDecl member : struct.members) {
            ctx.writeIndent();
            generateType(member.type, ctx, true);
            ctx.append(" ").append(member.name);
            generateArrayDims(member.arrayDimensions, ctx);
            ctx.append(";").newLine();
        }
        
        ctx.dedent();
        ctx.writeIndent().append("}");
    }
    
    private void generateInterfaceBlock(GLSLInterfaceBlock block, CodeGenContext ctx) {
        if (block.qualifiers != null) {
            generateQualifiers(block.qualifiers, ctx);
        }
        
        ctx.append(block.blockName).append(" {").newLine();
        ctx.indent();
        
        for (GLSLVariableDecl member : block.members) {
            ctx.writeIndent();
            generateType(member.type, ctx, true);
            ctx.append(" ").append(member.name);
            generateArrayDims(member.arrayDimensions, ctx);
            ctx.append(";").newLine();
        }
        
        ctx.dedent();
        ctx.writeIndent().append("}");
        
        if (block.instanceName != null) {
            ctx.append(" ").append(block.instanceName);
            generateArrayDims(block.arrayDimensions, ctx);
        }
        
        ctx.append(";").newLine();
    }
    
    private void generateVariableDecl(GLSLVariableDecl var, CodeGenContext ctx) {
        generateType(var.type, ctx, false);
        ctx.append(" ").append(var.name);
        
        // Array dimensions on variable
        if (var.type.arrayDimensions != null) {
            for (int dim : var.type.arrayDimensions) {
                ctx.append("[");
                if (dim > 0) ctx.append(dim);
                ctx.append("]");
            }
        }
        generateArrayDims(var.arrayDimensions, ctx);
        
        if (var.initializer != null) {
            ctx.append(" = ");
            generateExpression(var.initializer, ctx, 0);
        }
    }
    
    private void generateArrayDims(int[] dims, CodeGenContext ctx) {
        if (dims != null) {
            for (int dim : dims) {
                ctx.append("[");
                if (dim > 0) ctx.append(dim);
                ctx.append("]");
            }
        }
    }
    
    private void generateFunction(GLSLFunctionDecl func, CodeGenContext ctx) {
        // Return type
        generateType(func.returnType, ctx, false);
        ctx.append(" ").append(func.name).append("(");
        
        // Parameters
        for (int i = 0; i < func.parameters.size(); i++) {
            if (i > 0) ctx.append(", ");
            generateParameter(func.parameters.get(i), ctx);
        }
        
        ctx.append(")");
        
        if (func.isPrototype || func.body == null) {
            ctx.append(";").newLine();
        } else {
            ctx.append(" ");
            generateBlock(func.body, ctx, false);
            ctx.newLine();
        }
    }
    
    private void generateParameter(GLSLParameterDecl param, CodeGenContext ctx) {
        // Qualifier
        switch (param.qualifier) {
            case IN: ctx.append("in "); break;
            case OUT: ctx.append("out "); break;
            case INOUT: ctx.append("inout "); break;
            default: break;
        }
        
        generateType(param.type, ctx, true);
        
        if (param.name != null) {
            ctx.append(" ").append(param.name);
        }
    }
    
    // ========================================================================
    // STATEMENT GENERATION
    // ========================================================================
    
    private void generateStatement(GLSLStatement stmt, CodeGenContext ctx) {
        if (stmt instanceof GLSLBlockStmt) {
            generateBlock((GLSLBlockStmt) stmt, ctx, true);
        } else if (stmt instanceof GLSLExpressionStmt) {
            generateExpressionStmt((GLSLExpressionStmt) stmt, ctx);
        } else if (stmt instanceof GLSLDeclarationStmt) {
            generateDeclarationStmt((GLSLDeclarationStmt) stmt, ctx);
        } else if (stmt instanceof GLSLIfStmt) {
            generateIfStmt((GLSLIfStmt) stmt, ctx);
        } else if (stmt instanceof GLSLForStmt) {
            generateForStmt((GLSLForStmt) stmt, ctx);
        } else if (stmt instanceof GLSLWhileStmt) {
            generateWhileStmt((GLSLWhileStmt) stmt, ctx);
        } else if (stmt instanceof GLSLDoWhileStmt) {
            generateDoWhileStmt((GLSLDoWhileStmt) stmt, ctx);
        } else if (stmt instanceof GLSLSwitchStmt) {
            generateSwitchStmt((GLSLSwitchStmt) stmt, ctx);
        } else if (stmt instanceof GLSLCaseLabel) {
            generateCaseLabel((GLSLCaseLabel) stmt, ctx);
        } else if (stmt instanceof GLSLReturnStmt) {
            generateReturnStmt((GLSLReturnStmt) stmt, ctx);
        } else if (stmt instanceof GLSLBreakStmt) {
            ctx.writeIndent().append("break;").newLine();
        } else if (stmt instanceof GLSLContinueStmt) {
            ctx.writeIndent().append("continue;").newLine();
        } else if (stmt instanceof GLSLDiscardStmt) {
            ctx.writeIndent().append("discard;").newLine();
        }
    }
    
    private void generateBlock(GLSLBlockStmt block, CodeGenContext ctx, boolean indent) {
        ctx.append("{").newLine();
        ctx.indent();
        
        for (GLSLStatement stmt : block.statements) {
            generateStatement(stmt, ctx);
        }
        
        ctx.dedent();
        ctx.writeIndent().append("}");
        
        if (indent) {
            ctx.newLine();
        }
    }
    
    private void generateExpressionStmt(GLSLExpressionStmt stmt, CodeGenContext ctx) {
        ctx.writeIndent();
        if (stmt.expression != null) {
            generateExpression(stmt.expression, ctx, 0);
        }
        ctx.append(";").newLine();
    }
    
    private void generateDeclarationStmt(GLSLDeclarationStmt stmt, CodeGenContext ctx) {
        if (stmt.declarations.isEmpty()) return;
        
        ctx.writeIndent();
        
        // Get type from first declaration
        GLSLVariableDecl first = stmt.declarations.get(0);
        generateType(first.type, ctx, false);
        ctx.append(" ");
        
        for (int i = 0; i < stmt.declarations.size(); i++) {
            if (i > 0) ctx.append(", ");
            GLSLVariableDecl var = stmt.declarations.get(i);
            
            ctx.append(var.name);
            generateArrayDims(var.arrayDimensions, ctx);
            
            if (var.initializer != null) {
                ctx.append(" = ");
                generateExpression(var.initializer, ctx, 0);
            }
        }
        
        ctx.append(";").newLine();
    }
    
    private void generateIfStmt(GLSLIfStmt stmt, CodeGenContext ctx) {
        ctx.writeIndent().append("if (");
        generateExpression(stmt.condition, ctx, 0);
        ctx.append(") ");
        
        if (stmt.thenBranch instanceof GLSLBlockStmt) {
            generateBlock((GLSLBlockStmt) stmt.thenBranch, ctx, false);
        } else {
            ctx.newLine();
            ctx.indent();
            generateStatement(stmt.thenBranch, ctx);
            ctx.dedent();
        }
        
        if (stmt.elseBranch != null) {
            if (stmt.thenBranch instanceof GLSLBlockStmt) {
                ctx.append(" else ");
            } else {
                ctx.writeIndent().append("else ");
            }
            
            if (stmt.elseBranch instanceof GLSLIfStmt) {
                // else if - don't add braces or newline
                generateIfStmt((GLSLIfStmt) stmt.elseBranch, ctx);
                return;
            } else if (stmt.elseBranch instanceof GLSLBlockStmt) {
                generateBlock((GLSLBlockStmt) stmt.elseBranch, ctx, false);
            } else {
                ctx.newLine();
                ctx.indent();
                generateStatement(stmt.elseBranch, ctx);
                ctx.dedent();
                return;
            }
        }
        
        ctx.newLine();
    }
    
    private void generateForStmt(GLSLForStmt stmt, CodeGenContext ctx) {
        ctx.writeIndent().append("for (");
        
        // Init
        if (stmt.init != null) {
            if (stmt.init instanceof GLSLDeclarationStmt) {
                GLSLDeclarationStmt decl = (GLSLDeclarationStmt) stmt.init;
                if (!decl.declarations.isEmpty()) {
                    GLSLVariableDecl first = decl.declarations.get(0);
                    generateType(first.type, ctx, false);
                    ctx.append(" ");
                    for (int i = 0; i < decl.declarations.size(); i++) {
                        if (i > 0) ctx.append(", ");
                        GLSLVariableDecl var = decl.declarations.get(i);
                        ctx.append(var.name);
                        if (var.initializer != null) {
                            ctx.append(" = ");
                            generateExpression(var.initializer, ctx, 0);
                        }
                    }
                }
            } else if (stmt.init instanceof GLSLExpressionStmt) {
                GLSLExpressionStmt expr = (GLSLExpressionStmt) stmt.init;
                if (expr.expression != null) {
                    generateExpression(expr.expression, ctx, 0);
                }
            }
        }
        
        ctx.append("; ");
        
        // Condition
        if (stmt.condition != null) {
            generateExpression(stmt.condition, ctx, 0);
        }
        
        ctx.append("; ");
        
        // Update
        if (stmt.update != null) {
            generateExpression(stmt.update, ctx, 0);
        }
        
        ctx.append(") ");
        
        if (stmt.body instanceof GLSLBlockStmt) {
            generateBlock((GLSLBlockStmt) stmt.body, ctx, true);
        } else {
            ctx.newLine();
            ctx.indent();
            generateStatement(stmt.body, ctx);
            ctx.dedent();
        }
    }
    
    private void generateWhileStmt(GLSLWhileStmt stmt, CodeGenContext ctx) {
        ctx.writeIndent().append("while (");
        generateExpression(stmt.condition, ctx, 0);
        ctx.append(") ");
        
        if (stmt.body instanceof GLSLBlockStmt) {
            generateBlock((GLSLBlockStmt) stmt.body, ctx, true);
        } else {
            ctx.newLine();
            ctx.indent();
            generateStatement(stmt.body, ctx);
            ctx.dedent();
        }
    }
    
    private void generateDoWhileStmt(GLSLDoWhileStmt stmt, CodeGenContext ctx) {
        ctx.writeIndent().append("do ");
        
        if (stmt.body instanceof GLSLBlockStmt) {
            generateBlock((GLSLBlockStmt) stmt.body, ctx, false);
        } else {
            ctx.newLine();
            ctx.indent();
            generateStatement(stmt.body, ctx);
            ctx.dedent();
            ctx.writeIndent();
        }
        
        ctx.append(" while (");
        generateExpression(stmt.condition, ctx, 0);
        ctx.append(");").newLine();
    }
    
    private void generateSwitchStmt(GLSLSwitchStmt stmt, CodeGenContext ctx) {
        ctx.writeIndent().append("switch (");
        generateExpression(stmt.expression, ctx, 0);
        ctx.append(") {").newLine();
        
        for (GLSLCaseLabel caseLabel : stmt.cases) {
            generateCaseLabel(caseLabel, ctx);
        }
        
        ctx.indent();
        for (GLSLStatement s : stmt.statements) {
            generateStatement(s, ctx);
        }
        ctx.dedent();
        
        ctx.writeIndent().append("}").newLine();
    }
    
    private void generateCaseLabel(GLSLCaseLabel label, CodeGenContext ctx) {
        ctx.writeIndent();
        if (label.isDefault) {
            ctx.append("default:");
        } else {
            ctx.append("case ");
            generateExpression(label.value, ctx, 0);
            ctx.append(":");
        }
        ctx.newLine();
    }
    
    private void generateReturnStmt(GLSLReturnStmt stmt, CodeGenContext ctx) {
        ctx.writeIndent().append("return");
        if (stmt.value != null) {
            ctx.append(" ");
            generateExpression(stmt.value, ctx, 0);
        }
        ctx.append(";").newLine();
    }
    
    // ========================================================================
    // EXPRESSION GENERATION
    // ========================================================================
    
    private void generateExpression(GLSLExpression expr, CodeGenContext ctx, int parentPrecedence) {
        if (expr instanceof GLSLLiteralExpr) {
            generateLiteral((GLSLLiteralExpr) expr, ctx);
        } else if (expr instanceof GLSLIdentifierExpr) {
            ctx.append(((GLSLIdentifierExpr) expr).name);
        } else if (expr instanceof GLSLBinaryExpr) {
            generateBinaryExpr((GLSLBinaryExpr) expr, ctx, parentPrecedence);
        } else if (expr instanceof GLSLUnaryExpr) {
            generateUnaryExpr((GLSLUnaryExpr) expr, ctx);
        } else if (expr instanceof GLSLTernaryExpr) {
            generateTernaryExpr((GLSLTernaryExpr) expr, ctx, parentPrecedence);
        } else if (expr instanceof GLSLCallExpr) {
            generateCallExpr((GLSLCallExpr) expr, ctx);
        } else if (expr instanceof GLSLMemberExpr) {
            generateMemberExpr((GLSLMemberExpr) expr, ctx);
        } else if (expr instanceof GLSLSubscriptExpr) {
            generateSubscriptExpr((GLSLSubscriptExpr) expr, ctx);
        } else if (expr instanceof GLSLInitializerListExpr) {
            generateInitializerList((GLSLInitializerListExpr) expr, ctx);
        }
    }
    
    private void generateLiteral(GLSLLiteralExpr lit, CodeGenContext ctx) {
        switch (lit.literalType) {
            case BOOL:
                ctx.append(lit.boolValue ? "true" : "false");
                break;
            case INT:
                ctx.append(lit.intValue);
                break;
            case UINT:
                ctx.append(lit.intValue).append("u");
                break;
            case FLOAT:
                String floatStr = formatFloat(lit.floatValue);
                ctx.append(floatStr);
                break;
            case DOUBLE:
                String doubleStr = formatDouble(lit.floatValue);
                ctx.append(doubleStr);
                break;
        }
    }
    
    private String formatFloat(double value) {
        if (Double.isNaN(value)) return "(0.0/0.0)";
        if (Double.isInfinite(value)) return value > 0 ? "(1.0/0.0)" : "(-1.0/0.0)";
        
        String s = String.valueOf(value);
        // Ensure it has decimal point
        if (!s.contains(".") && !s.contains("e") && !s.contains("E")) {
            s += ".0";
        }
        return s;
    }
    
    private String formatDouble(double value) {
        return formatFloat(value) + "lf";
    }
    
    private void generateBinaryExpr(GLSLBinaryExpr expr, CodeGenContext ctx, int parentPrecedence) {
        int myPrecedence = BINARY_PRECEDENCE.getOrDefault(expr.operator, 0);
        boolean needParens = myPrecedence < parentPrecedence;
        
        if (needParens) ctx.append("(");
        
        // Left operand
        int leftPrec = myPrecedence;
        if (RIGHT_ASSOCIATIVE.getOrDefault(expr.operator, false)) {
            leftPrec = myPrecedence + 1;
        }
        generateExpression(expr.left, ctx, leftPrec);
        
        // Operator
        ctx.append(" ").append(expr.operator.symbol).append(" ");
        
        // Right operand
        int rightPrec = myPrecedence;
        if (!RIGHT_ASSOCIATIVE.getOrDefault(expr.operator, false)) {
            rightPrec = myPrecedence + 1;
        }
        generateExpression(expr.right, ctx, rightPrec);
        
        if (needParens) ctx.append(")");
    }
    
    private void generateUnaryExpr(GLSLUnaryExpr expr, CodeGenContext ctx) {
        boolean isPrefix = expr.operator.isPrefix();
        
        if (isPrefix) {
            ctx.append(expr.operator.symbol);
        }
        
        // Unary has high precedence
        boolean needParens = expr.operand instanceof GLSLBinaryExpr || 
                            expr.operand instanceof GLSLTernaryExpr;
        
        if (needParens) ctx.append("(");
        generateExpression(expr.operand, ctx, 15);
        if (needParens) ctx.append(")");
        
        if (!isPrefix) {
            ctx.append(expr.operator.symbol);
        }
    }
    
    private void generateTernaryExpr(GLSLTernaryExpr expr, CodeGenContext ctx, int parentPrecedence) {
        // Ternary has very low precedence (3)
        boolean needParens = parentPrecedence > 3;
        
        if (needParens) ctx.append("(");
        
        generateExpression(expr.condition, ctx, 3);
        ctx.append(" ? ");
        generateExpression(expr.thenExpr, ctx, 0);
        ctx.append(" : ");
        generateExpression(expr.elseExpr, ctx, 3);
        
        if (needParens) ctx.append(")");
    }
    
    private void generateCallExpr(GLSLCallExpr expr, CodeGenContext ctx) {
        ctx.append(expr.functionName).append("(");
        
        for (int i = 0; i < expr.arguments.size(); i++) {
            if (i > 0) ctx.append(", ");
            generateExpression(expr.arguments.get(i), ctx, 0);
        }
        
        ctx.append(")");
    }
    
    private void generateMemberExpr(GLSLMemberExpr expr, CodeGenContext ctx) {
        generateExpression(expr.object, ctx, 16); // High precedence
        ctx.append(".").append(expr.member);
    }
    
    private void generateSubscriptExpr(GLSLSubscriptExpr expr, CodeGenContext ctx) {
        generateExpression(expr.array, ctx, 16); // High precedence
        ctx.append("[");
        generateExpression(expr.index, ctx, 0);
        ctx.append("]");
    }
    
    private void generateInitializerList(GLSLInitializerListExpr expr, CodeGenContext ctx) {
        ctx.append("{");
        for (int i = 0; i < expr.elements.size(); i++) {
            if (i > 0) ctx.append(", ");
            generateExpression(expr.elements.get(i), ctx, 0);
        }
        ctx.append("}");
    }
    
    // ========================================================================
    // CODE GENERATION CONTEXT
    // ========================================================================
    
    private static class CodeGenContext {
        private final StringBuilder sb;
        private final GLSLVersion version;
        private int indentLevel = 0;
        private static final String INDENT = "    ";
        
        CodeGenContext(StringBuilder sb, GLSLVersion version) {
            this.sb = sb;
            this.version = version;
        }
        
        CodeGenContext append(String s) {
            sb.append(s);
            return this;
        }
        
        CodeGenContext append(int i) {
            sb.append(i);
            return this;
        }
        
        CodeGenContext append(long l) {
            sb.append(l);
            return this;
        }
        
        CodeGenContext append(char c) {
            sb.append(c);
            return this;
        }
        
        CodeGenContext newLine() {
            sb.append('\n');
            return this;
        }
        
        CodeGenContext writeIndent() {
            for (int i = 0; i < indentLevel; i++) {
                sb.append(INDENT);
            }
            return this;
        }
        
        void indent() {
            indentLevel++;
        }
        
        void dedent() {
            if (indentLevel > 0) indentLevel--;
        }
    }
}

// ============================================================================
// SHADER COMPILATION INTEGRATION
// ============================================================================

/**
 * Integrated shader compilation with automatic translation.
 * Wraps OpenGL shader functions with translation support.
 */
final class GLSLShaderCompiler {
    
    private final GLSLCallMapper mapper;
    private final OpenGLCallMapper gl;
    private final GLSLShaderCache cache;
    
    // Active shaders and programs
    private final Map<Integer, CompiledShader> shaders = new ConcurrentHashMap<>();
    private final Map<Integer, ShaderProgram> programs = new ConcurrentHashMap<>();
    
    // ID generators
    private final AtomicInteger nextShaderId = new AtomicInteger(1);
    private final AtomicInteger nextProgramId = new AtomicInteger(1);
    
    GLSLShaderCompiler(GLSLCallMapper mapper, OpenGLCallMapper gl) {
        this.mapper = mapper;
        this.gl = gl;
        this.cache = new GLSLShaderCache(mapper.getMemoryPool());
    }
    
    // ========================================================================
    // SHADER CREATION & COMPILATION
    // ========================================================================
    
    /**
     * Create a shader with automatic version translation.
     */
    public int createShader(int type, String source) {
        GLSLShaderType shaderType = GLSLShaderType.fromGLType(type);
        if (shaderType == null) {
            throw new GLSLException("Invalid shader type: " + type);
        }
        
        // Translate to target version
        GLSLTranslationResult result = mapper.translate(source, shaderType);
        
        // Try to compile
        int glShader = gl.glCreateShader(type);
        if (glShader == 0) {
            throw new GLSLException("Failed to create shader");
        }
        
        gl.glShaderSource(glShader, result.getSource());
        gl.glCompileShader(glShader);
        
        // Check compilation status
        int status = gl.glGetShaderi(glShader, OpenGLCallMapper.GL_COMPILE_STATUS);
        
        CompiledShader shader = new CompiledShader(
            glShader, shaderType, source, result.getSource(),
            result.getSourceVersion(), result.getTargetVersion(),
            status == OpenGLCallMapper.GL_TRUE
        );
        
        if (!shader.compiled) {
            shader.errorLog = gl.glGetShaderInfoLog(glShader);
            shader.errorLog = remapErrorLines(shader.errorLog, source, result.getSource());
        }
        
        shaders.put(glShader, shader);
        return glShader;
    }
    
    /**
     * Create shader with specific target version.
     */
    public int createShader(int type, String source, GLSLVersion targetVersion) {
        GLSLShaderType shaderType = GLSLShaderType.fromGLType(type);
        if (shaderType == null) {
            throw new GLSLException("Invalid shader type: " + type);
        }
        
        // Translate to specific version
        GLSLTranslationResult result = mapper.translate(source, shaderType, null, targetVersion);
        
        int glShader = gl.glCreateShader(type);
        if (glShader == 0) {
            throw new GLSLException("Failed to create shader");
        }
        
        gl.glShaderSource(glShader, result.getSource());
        gl.glCompileShader(glShader);
        
        int status = gl.glGetShaderi(glShader, OpenGLCallMapper.GL_COMPILE_STATUS);
        
        CompiledShader shader = new CompiledShader(
            glShader, shaderType, source, result.getSource(),
            result.getSourceVersion(), targetVersion,
            status == OpenGLCallMapper.GL_TRUE
        );
        
        if (!shader.compiled) {
            shader.errorLog = gl.glGetShaderInfoLog(glShader);
            shader.errorLog = remapErrorLines(shader.errorLog, source, result.getSource());
        }
        
        shaders.put(glShader, shader);
        return glShader;
    }
    
    /**
     * Create shader with fallback versions.
     * Tries target version first, then falls back to lower versions.
     */
    public int createShaderWithFallback(int type, String source, GLSLVersion[] fallbackVersions) {
        GLSLShaderType shaderType = GLSLShaderType.fromGLType(type);
        if (shaderType == null) {
            throw new GLSLException("Invalid shader type: " + type);
        }
        
        GLSLVersion sourceVersion = mapper.getVersionManager().detectVersion(source);
        StringBuilder allErrors = new StringBuilder();
        
        for (GLSLVersion targetVersion : fallbackVersions) {
            if (targetVersion.versionNumber > mapper.getHardwareMaxVersion().versionNumber) {
                continue; // Skip versions not supported by hardware
            }
            
            try {
                GLSLTranslationResult result = mapper.translate(source, shaderType, 
                                                                sourceVersion, targetVersion);
                
                int glShader = gl.glCreateShader(type);
                if (glShader == 0) continue;
                
                gl.glShaderSource(glShader, result.getSource());
                gl.glCompileShader(glShader);
                
                int status = gl.glGetShaderi(glShader, OpenGLCallMapper.GL_COMPILE_STATUS);
                
                if (status == OpenGLCallMapper.GL_TRUE) {
                    CompiledShader shader = new CompiledShader(
                        glShader, shaderType, source, result.getSource(),
                        sourceVersion, targetVersion, true
                    );
                    shaders.put(glShader, shader);
                    return glShader;
                }
                
                // Collect error for reporting
                String error = gl.glGetShaderInfoLog(glShader);
                allErrors.append("GLSL ").append(targetVersion.versionString)
                         .append(": ").append(error).append("\n");
                
                gl.glDeleteShader(glShader);
                
            } catch (GLSLException e) {
                allErrors.append("GLSL ").append(targetVersion.versionString)
                         .append(": ").append(e.getMessage()).append("\n");
            }
        }
        
        throw new GLSLException("Failed to compile shader with any version:\n" + allErrors);
    }
    
    /**
     * Get compilation status.
     */
    public boolean isCompiled(int shader) {
        CompiledShader s = shaders.get(shader);
        return s != null && s.compiled;
    }
    
    /**
     * Get shader info log with remapped line numbers.
     */
    public String getShaderInfoLog(int shader) {
        CompiledShader s = shaders.get(shader);
        return s != null ? s.errorLog : "";
    }
    
    /**
     * Get the translated source.
     */
    public String getTranslatedSource(int shader) {
        CompiledShader s = shaders.get(shader);
        return s != null ? s.translatedSource : null;
    }
    
    /**
     * Delete shader.
     */
    public void deleteShader(int shader) {
        CompiledShader s = shaders.remove(shader);
        if (s != null) {
            gl.glDeleteShader(s.glId);
        }
    }
    
    // ========================================================================
    // PROGRAM MANAGEMENT
    // ========================================================================
    
    /**
     * Create and link a shader program.
     */
    public int createProgram(int... shaderIds) {
        int program = gl.glCreateProgram();
        if (program == 0) {
            throw new GLSLException("Failed to create program");
        }
        
        ShaderProgram prog = new ShaderProgram(program);
        
        for (int shaderId : shaderIds) {
            CompiledShader shader = shaders.get(shaderId);
            if (shader == null) {
                gl.glDeleteProgram(program);
                throw new GLSLException("Invalid shader ID: " + shaderId);
            }
            
            gl.glAttachShader(program, shaderId);
            prog.attachedShaders.add(shaderId);
        }
        
        gl.glLinkProgram(program);
        
        int status = gl.glGetProgrami(program, OpenGLCallMapper.GL_LINK_STATUS);
        prog.linked = status == OpenGLCallMapper.GL_TRUE;
        
        if (!prog.linked) {
            prog.errorLog = gl.glGetProgramInfoLog(program);
        } else {
            // Query active uniforms and attributes
            queryProgramResources(prog);
        }
        
        programs.put(program, prog);
        return program;
    }
    
    /**
     * Use a shader program.
     */
    public void useProgram(int program) {
        gl.glUseProgram(program);
    }
    
    /**
     * Get uniform location with caching.
     */
    public int getUniformLocation(int program, String name) {
        ShaderProgram prog = programs.get(program);
        if (prog == null) return -1;
        
        Integer cached = prog.uniformLocations.get(name);
        if (cached != null) return cached;
        
        int location = gl.glGetUniformLocation(program, name);
        prog.uniformLocations.put(name, location);
        return location;
    }
    
    /**
     * Get attribute location with caching.
     */
    public int getAttribLocation(int program, String name) {
        ShaderProgram prog = programs.get(program);
        if (prog == null) return -1;
        
        Integer cached = prog.attribLocations.get(name);
        if (cached != null) return cached;
        
        int location = gl.glGetAttribLocation(program, name);
        prog.attribLocations.put(name, location);
        return location;
    }
    
    /**
     * Delete program.
     */
    public void deleteProgram(int program) {
        ShaderProgram prog = programs.remove(program);
        if (prog != null) {
            gl.glDeleteProgram(prog.glId);
        }
    }
    
    /**
     * Get program link status.
     */
    public boolean isLinked(int program) {
        ShaderProgram prog = programs.get(program);
        return prog != null && prog.linked;
    }
    
    /**
     * Get program info log.
     */
    public String getProgramInfoLog(int program) {
        ShaderProgram prog = programs.get(program);
        return prog != null ? prog.errorLog : "";
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    private String remapErrorLines(String errorLog, String originalSource, String translatedSource) {
        if (errorLog == null || errorLog.isEmpty()) return errorLog;
        
        // Build line mapping
        int[] originalLines = countLines(originalSource);
        int[] translatedLines = countLines(translatedSource);
        
        // Simple heuristic: try to map line numbers
        // In a real implementation, we'd track line mappings during translation
        
        StringBuilder result = new StringBuilder();
        String[] lines = errorLog.split("\n");
        
        for (String line : lines) {
            // Try to find and remap line numbers in format "0(123)" or "ERROR: 0:123:"
            String remapped = remapLineNumber(line, translatedLines.length, originalLines.length);
            result.append(remapped).append("\n");
        }
        
        return result.toString();
    }
    
    private String remapLineNumber(String errorLine, int translatedLineCount, int originalLineCount) {
        // Simple ratio-based remapping
        // Real implementation would use source maps
        try {
            // Pattern: "0(N)" or "ERROR: 0:N:"
            int start = -1;
            int end = -1;
            
            // Look for patterns like "0(123)" or ":123:"
            for (int i = 0; i < errorLine.length(); i++) {
                char c = errorLine.charAt(i);
                if (c == '(' || (c == ':' && i > 0 && Character.isDigit(errorLine.charAt(i-1)) == false)) {
                    if (i + 1 < errorLine.length() && Character.isDigit(errorLine.charAt(i+1))) {
                        start = i + 1;
                    }
                } else if (start > 0 && !Character.isDigit(c)) {
                    end = i;
                    break;
                }
            }
            
            if (start > 0 && end > start) {
                int lineNum = Integer.parseInt(errorLine.substring(start, end));
                // Scale line number
                int mappedLine = (int) ((double) lineNum / translatedLineCount * originalLineCount);
                mappedLine = Math.max(1, Math.min(mappedLine, originalLineCount));
                
                return errorLine.substring(0, start) + mappedLine + errorLine.substring(end) +
                       " (translated line: " + lineNum + ")";
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
        
        return errorLine;
    }
    
    private int[] countLines(String source) {
        int count = 1;
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n') count++;
        }
        return new int[count];
    }
    
    private void queryProgramResources(ShaderProgram prog) {
        // Query active uniforms
        int numUniforms = gl.glGetProgrami(prog.glId, OpenGLCallMapper.GL_ACTIVE_UNIFORMS);
        for (int i = 0; i < numUniforms; i++) {
            String name = gl.glGetActiveUniform(prog.glId, i);
            if (name != null && !name.startsWith("gl_")) {
                int location = gl.glGetUniformLocation(prog.glId, name);
                prog.uniformLocations.put(name, location);
            }
        }
        
        // Query active attributes
        int numAttribs = gl.glGetProgrami(prog.glId, OpenGLCallMapper.GL_ACTIVE_ATTRIBUTES);
        for (int i = 0; i < numAttribs; i++) {
            String name = gl.glGetActiveAttrib(prog.glId, i);
            if (name != null && !name.startsWith("gl_")) {
                int location = gl.glGetAttribLocation(prog.glId, name);
                prog.attribLocations.put(name, location);
            }
        }
    }
    
    // ========================================================================
    // INTERNAL CLASSES
    // ========================================================================
    
    private static class CompiledShader {
        final int glId;
        final GLSLShaderType type;
        final String originalSource;
        final String translatedSource;
        final GLSLVersion sourceVersion;
        final GLSLVersion targetVersion;
        final boolean compiled;
        String errorLog;
        
        CompiledShader(int glId, GLSLShaderType type, String original, String translated,
                       GLSLVersion sourceVer, GLSLVersion targetVer, boolean compiled) {
            this.glId = glId;
            this.type = type;
            this.originalSource = original;
            this.translatedSource = translated;
            this.sourceVersion = sourceVer;
            this.targetVersion = targetVer;
            this.compiled = compiled;
        }
    }
    
    private static class ShaderProgram {
        final int glId;
        final List<Integer> attachedShaders = new ArrayList<>();
        final Map<String, Integer> uniformLocations = new ConcurrentHashMap<>();
        final Map<String, Integer> attribLocations = new ConcurrentHashMap<>();
        boolean linked;
        String errorLog;
        
        ShaderProgram(int glId) {
            this.glId = glId;
        }
    }
}

// ============================================================================
// SHADER CACHE SYSTEM
// ============================================================================

/**
 * Disk-based cache for translated shaders.
 */
final class GLSLShaderCache {
    
    private final GLSLMemoryPool pool;
    private final Map<Long, CacheEntry> memoryCache = new ConcurrentHashMap<>();
    private Path cacheDirectory;
    private boolean diskCacheEnabled = false;
    
    private static final int MAX_MEMORY_ENTRIES = 128;
    private static final long CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 hours
    
    GLSLShaderCache(GLSLMemoryPool pool) {
        this.pool = pool;
    }
    
    /**
     * Enable disk caching in the specified directory.
     */
    public void enableDiskCache(Path directory) {
        try {
            Files.createDirectories(directory);
            this.cacheDirectory = directory;
            this.diskCacheEnabled = true;
        } catch (IOException e) {
            diskCacheEnabled = false;
        }
    }
    
    /**
     * Get cached translation result.
     */
    public GLSLTranslationResult get(String source, GLSLShaderType type, GLSLVersion targetVersion) {
        long key = computeKey(source, type, targetVersion);
        
        // Check memory cache
        CacheEntry entry = memoryCache.get(key);
        if (entry != null && !entry.isExpired()) {
            entry.accessCount++;
            return entry.result;
        }
        
        // Check disk cache
        if (diskCacheEnabled) {
            GLSLTranslationResult diskResult = loadFromDisk(key);
            if (diskResult != null) {
                // Promote to memory cache
                addToMemoryCache(key, diskResult);
                return diskResult;
            }
        }
        
        return null;
    }
    
    /**
     * Store translation result in cache.
     */
    public void put(String source, GLSLShaderType type, GLSLVersion targetVersion, 
                    GLSLTranslationResult result) {
        long key = computeKey(source, type, targetVersion);
        
        addToMemoryCache(key, result);
        
        if (diskCacheEnabled) {
            saveToDisk(key, result);
        }
    }
    
    /**
     * Clear all caches.
     */
    public void clear() {
        memoryCache.clear();
        
        if (diskCacheEnabled && cacheDirectory != null) {
            try {
                Files.list(cacheDirectory)
                     .filter(p -> p.toString().endsWith(".glslcache"))
                     .forEach(p -> {
                         try { Files.delete(p); } catch (IOException e) {}
                     });
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    /**
     * Remove expired entries.
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        memoryCache.entrySet().removeIf(e -> e.getValue().isExpired());
        
        // Clean disk cache
        if (diskCacheEnabled && cacheDirectory != null) {
            try {
                Files.list(cacheDirectory)
                     .filter(p -> p.toString().endsWith(".glslcache"))
                     .forEach(p -> {
                         try {
                             if (Files.getLastModifiedTime(p).toMillis() + CACHE_EXPIRY_MS < now) {
                                 Files.delete(p);
                             }
                         } catch (IOException e) {}
                     });
            } catch (IOException e) {
                // Ignore
            }
        }
    }
    
    private long computeKey(String source, GLSLShaderType type, GLSLVersion version) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(source.getBytes(StandardCharsets.UTF_8));
            md.update((byte) type.ordinal());
            md.update((byte) version.ordinal());
            byte[] hash = md.digest();
            
            // Use first 8 bytes as long
            long result = 0;
            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (hash[i] & 0xFF);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return source.hashCode() * 31L + type.ordinal() * 17 + version.ordinal();
        }
    }
    
    private void addToMemoryCache(long key, GLSLTranslationResult result) {
        // Evict if full
        if (memoryCache.size() >= MAX_MEMORY_ENTRIES) {
            evictLRU();
        }
        
        memoryCache.put(key, new CacheEntry(result));
    }
    
    private void evictLRU() {
        // Find least accessed entry
        Long lruKey = null;
        int minAccess = Integer.MAX_VALUE;
        
        for (Map.Entry<Long, CacheEntry> entry : memoryCache.entrySet()) {
            if (entry.getValue().accessCount < minAccess) {
                minAccess = entry.getValue().accessCount;
                lruKey = entry.getKey();
            }
        }
        
        if (lruKey != null) {
            memoryCache.remove(lruKey);
        }
    }
    
    private GLSLTranslationResult loadFromDisk(long key) {
        if (cacheDirectory == null) return null;
        
        Path cachePath = cacheDirectory.resolve(Long.toHexString(key) + ".glslcache");
        
        try {
            if (Files.exists(cachePath)) {
                byte[] data = Files.readAllBytes(cachePath);
                return deserialize(data);
            }
        } catch (IOException e) {
            // Ignore
        }
        
        return null;
    }
    
    private void saveToDisk(long key, GLSLTranslationResult result) {
        if (cacheDirectory == null) return;
        
        Path cachePath = cacheDirectory.resolve(Long.toHexString(key) + ".glslcache");
        
        try {
            byte[] data = serialize(result);
            Files.write(cachePath, data);
        } catch (IOException e) {
            // Ignore
        }
    }
    
    private byte[] serialize(GLSLTranslationResult result) {
        // Simple serialization - in production use proper serialization
        StringBuilder sb = new StringBuilder();
        sb.append(result.getSourceVersion().versionNumber).append('\n');
        sb.append(result.getTargetVersion().versionNumber).append('\n');
        sb.append(result.getShaderType().ordinal()).append('\n');
        sb.append(result.getSource().length()).append('\n');
        sb.append(result.getSource());
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    private GLSLTranslationResult deserialize(byte[] data) {
        try {
            String str = new String(data, StandardCharsets.UTF_8);
            String[] lines = str.split("\n", 5);
            
            GLSLVersion sourceVer = GLSLVersion.fromNumber(Integer.parseInt(lines[0]));
            GLSLVersion targetVer = GLSLVersion.fromNumber(Integer.parseInt(lines[1]));
            GLSLShaderType type = GLSLShaderType.values()[Integer.parseInt(lines[2])];
            int sourceLen = Integer.parseInt(lines[3]);
            String source = lines[4].substring(0, sourceLen);
            
            return new GLSLTranslationResult(source, sourceVer, targetVer, type,
                                             null, null, 0);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static class CacheEntry {
        final GLSLTranslationResult result;
        final long createTime;
        int accessCount = 1;
        
        CacheEntry(GLSLTranslationResult result) {
            this.result = result;
            this.createTime = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - createTime > CACHE_EXPIRY_MS;
        }
    }
}

// ============================================================================
// HOT RELOAD SUPPORT
// ============================================================================

/**
 * Shader hot-reload system for development.
 */
final class GLSLHotReloadManager {
    
    private final GLSLShaderCompiler compiler;
    private final Map<Path, WatchedShader> watchedShaders = new ConcurrentHashMap<>();
    private final ScheduledExecutorService watchService;
    private volatile boolean running = false;
    
    GLSLHotReloadManager(GLSLShaderCompiler compiler) {
        this.compiler = compiler;
        this.watchService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GLSL-HotReload");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Start watching for shader file changes.
     */
    public void start() {
        if (running) return;
        running = true;
        
        watchService.scheduleWithFixedDelay(this::checkForChanges, 1, 1, TimeUnit.SECONDS);
    }
    
    /**
     * Stop watching.
     */
    public void stop() {
        running = false;
        watchService.shutdown();
    }
    
    /**
     * Register a shader file for hot-reloading.
     */
    public void watch(Path shaderFile, int shaderType, Consumer<Integer> onReload) {
        try {
            WatchedShader ws = new WatchedShader();
            ws.path = shaderFile;
            ws.shaderType = shaderType;
            ws.lastModified = Files.getLastModifiedTime(shaderFile).toMillis();
            ws.onReload = onReload;
            
            // Initial compile
            String source = new String(Files.readAllBytes(shaderFile), StandardCharsets.UTF_8);
            ws.currentShader = compiler.createShader(shaderType, source);
            
            if (compiler.isCompiled(ws.currentShader)) {
                onReload.accept(ws.currentShader);
            }
            
            watchedShaders.put(shaderFile, ws);
        } catch (IOException e) {
            throw new GLSLException("Failed to watch shader: " + shaderFile, e);
        }
    }
    
    /**
     * Unregister a shader file.
     */
    public void unwatch(Path shaderFile) {
        WatchedShader ws = watchedShaders.remove(shaderFile);
        if (ws != null && ws.currentShader > 0) {
            compiler.deleteShader(ws.currentShader);
        }
    }
    
    private void checkForChanges() {
        for (Map.Entry<Path, WatchedShader> entry : watchedShaders.entrySet()) {
            Path path = entry.getKey();
            WatchedShader ws = entry.getValue();
            
            try {
                long currentModified = Files.getLastModifiedTime(path).toMillis();
                
                if (currentModified > ws.lastModified) {
                    ws.lastModified = currentModified;
                    
                    // Reload shader
                    String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    
                    int oldShader = ws.currentShader;
                    ws.currentShader = compiler.createShader(ws.shaderType, source);
                    
                    if (compiler.isCompiled(ws.currentShader)) {
                        // Delete old shader
                        if (oldShader > 0) {
                            compiler.deleteShader(oldShader);
                        }
                        
                        // Notify callback
                        ws.onReload.accept(ws.currentShader);
                        
                        System.out.println("[GLSL] Hot-reloaded: " + path.getFileName());
                    } else {
                        // Compilation failed - keep old shader
                        System.err.println("[GLSL] Hot-reload failed: " + path.getFileName());
                        System.err.println(compiler.getShaderInfoLog(ws.currentShader));
                        
                        compiler.deleteShader(ws.currentShader);
                        ws.currentShader = oldShader;
                    }
                }
            } catch (IOException e) {
                // File might be temporarily unavailable during save
            }
        }
    }
    
    private static class WatchedShader {
        Path path;
        int shaderType;
        long lastModified;
        int currentShader;
        Consumer<Integer> onReload;
    }
}

// ============================================================================
// COMPLETE INTEGRATION - UPDATED GLSLCallMapper METHODS
// ============================================================================

/**
 * Extension methods for GLSLCallMapper integration.
 * These would be added to the main GLSLCallMapper class.
 */
final class GLSLCallMapperExtensions {
    
    private final GLSLCallMapper mapper;
    private GLSLShaderCompiler compiler;
    private GLSLHotReloadManager hotReload;
    
    GLSLCallMapperExtensions(GLSLCallMapper mapper) {
        this.mapper = mapper;
    }
    
    /**
     * Initialize the shader compilation system.
     */
    public void initializeCompiler(OpenGLCallMapper gl) {
        this.compiler = new GLSLShaderCompiler(mapper, gl);
    }
    
    /**
     * Get the shader compiler.
     */
    public GLSLShaderCompiler getCompiler() {
        return compiler;
    }
    
    /**
     * Initialize hot-reload support.
     */
    public void initializeHotReload() {
        if (compiler == null) {
            throw new IllegalStateException("Compiler must be initialized first");
        }
        this.hotReload = new GLSLHotReloadManager(compiler);
    }
    
    /**
     * Get the hot-reload manager.
     */
    public GLSLHotReloadManager getHotReloadManager() {
        return hotReload;
    }
    
    /**
     * Compile a shader with automatic version translation.
     * Convenience method that uses the integrated compiler.
     */
    public int compileShader(int type, String source) {
        if (compiler == null) {
            throw new IllegalStateException("Compiler not initialized");
        }
        return compiler.createShader(type, source);
    }
    
    /**
     * Compile and link a complete shader program.
     */
    public int compileProgram(String vertexSource, String fragmentSource) {
        if (compiler == null) {
            throw new IllegalStateException("Compiler not initialized");
        }
        
        int vs = compiler.createShader(OpenGLCallMapper.GL_VERTEX_SHADER, vertexSource);
        int fs = compiler.createShader(OpenGLCallMapper.GL_FRAGMENT_SHADER, fragmentSource);
        
        if (!compiler.isCompiled(vs)) {
            throw new GLSLException("Vertex shader compilation failed:\n" + 
                                   compiler.getShaderInfoLog(vs));
        }
        
        if (!compiler.isCompiled(fs)) {
            compiler.deleteShader(vs);
            throw new GLSLException("Fragment shader compilation failed:\n" + 
                                   compiler.getShaderInfoLog(fs));
        }
        
        int program = compiler.createProgram(vs, fs);
        
        if (!compiler.isLinked(program)) {
            compiler.deleteShader(vs);
            compiler.deleteShader(fs);
            throw new GLSLException("Program linking failed:\n" + 
                                   compiler.getProgramInfoLog(program));
        }
        
        return program;
    }
    
    /**
     * Load, translate, and compile shaders from files.
     */
    public int compileProgramFromFiles(Path vertexPath, Path fragmentPath) throws IOException {
        String vertexSource = new String(Files.readAllBytes(vertexPath), StandardCharsets.UTF_8);
        String fragmentSource = new String(Files.readAllBytes(fragmentPath), StandardCharsets.UTF_8);
        return compileProgram(vertexSource, fragmentSource);
    }
}

// ============================================================================
// UTILITY: SHADER PREPROCESSOR
// ============================================================================

/**
 * Simple shader preprocessor for #include directives.
 */
final class GLSLPreprocessor {
    
    private final Map<String, String> includes = new HashMap<>();
    private final Map<String, String> defines = new HashMap<>();
    
    /**
     * Register an includable file.
     */
    public void registerInclude(String name, String content) {
        includes.put(name, content);
    }
    
    /**
     * Add a preprocessor define.
     */
    public void define(String name, String value) {
        defines.put(name, value);
    }
    
    /**
     * Add a preprocessor define without value.
     */
    public void define(String name) {
        defines.put(name, "");
    }
    
    /**
     * Process shader source.
     */
    public String process(String source) {
        StringBuilder result = new StringBuilder();
        String[] lines = source.split("\n");
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.startsWith("#include")) {
                // Process include
                String includeName = extractIncludeName(trimmed);
                if (includeName != null && includes.containsKey(includeName)) {
                    result.append(includes.get(includeName)).append("\n");
                } else {
                    result.append("// Include not found: ").append(includeName).append("\n");
                }
            } else if (trimmed.startsWith("#pragma once")) {
                // Skip pragma once
            } else {
                // Process defines in the line
                String processed = processDefines(line);
                result.append(processed).append("\n");
            }
        }
        
        return result.toString();
    }
    
    private String extractIncludeName(String line) {
        // #include "file" or #include <file>
        int start = line.indexOf('"');
        int end = line.lastIndexOf('"');
        
        if (start < 0) {
            start = line.indexOf('<');
            end = line.lastIndexOf('>');
        }
        
        if (start >= 0 && end > start) {
            return line.substring(start + 1, end);
        }
        
        return null;
    }
    
    private String processDefines(String line) {
        String result = line;
        for (Map.Entry<String, String> def : defines.entrySet()) {
            result = result.replace(def.getKey(), def.getValue());
        }
        return result;
    }
}

// ============================================================================
// SHADER BUILDER - FLUENT API
// ============================================================================

/**
 * Fluent API for building and compiling shaders.
 */
final class GLSLShaderBuilder {
    
    private final GLSLCallMapper mapper;
    private final GLSLPreprocessor preprocessor = new GLSLPreprocessor();
    
    private String vertexSource;
    private String fragmentSource;
    private String geometrySource;
    private String tessControlSource;
    private String tessEvalSource;
    private String computeSource;
    
    private GLSLVersion targetVersion;
    private int optimizationLevel = 2;
    
    GLSLShaderBuilder(GLSLCallMapper mapper) {
        this.mapper = mapper;
        this.targetVersion = mapper.getTargetVersion();
    }
    
    public GLSLShaderBuilder vertex(String source) {
        this.vertexSource = preprocessor.process(source);
        return this;
    }
    
    public GLSLShaderBuilder fragment(String source) {
        this.fragmentSource = preprocessor.process(source);
        return this;
    }
    
    public GLSLShaderBuilder geometry(String source) {
        this.geometrySource = preprocessor.process(source);
        return this;
    }
    
    public GLSLShaderBuilder tessControl(String source) {
        this.tessControlSource = preprocessor.process(source);
        return this;
    }
    
    public GLSLShaderBuilder tessEval(String source) {
        this.tessEvalSource = preprocessor.process(source);
        return this;
    }
    
    public GLSLShaderBuilder compute(String source) {
        this.computeSource = preprocessor.process(source);
        return this;
    }
    
    public GLSLShaderBuilder include(String name, String content) {
        preprocessor.registerInclude(name, content);
        return this;
    }
    
    public GLSLShaderBuilder define(String name, String value) {
        preprocessor.define(name, value);
        return this;
    }
    
    public GLSLShaderBuilder define(String name) {
        preprocessor.define(name);
        return this;
    }
    
    public GLSLShaderBuilder targetVersion(GLSLVersion version) {
        this.targetVersion = version;
        return this;
    }
    
    public GLSLShaderBuilder optimization(int level) {
        this.optimizationLevel = level;
        return this;
    }
    
    public ShaderBuildResult build() {
        ShaderBuildResult result = new ShaderBuildResult();
        
        try {
            int prevOptLevel = mapper.getOptimizationLevel();
            mapper.setOptimizationLevel(optimizationLevel);
            
            if (vertexSource != null) {
                result.vertexResult = mapper.translate(vertexSource, GLSLShaderType.VERTEX, 
                                                       null, targetVersion);
            }
            
            if (fragmentSource != null) {
                result.fragmentResult = mapper.translate(fragmentSource, GLSLShaderType.FRAGMENT,
                                                         null, targetVersion);
            }
            
            if (geometrySource != null) {
                result.geometryResult = mapper.translate(geometrySource, GLSLShaderType.GEOMETRY,
                                                         null, targetVersion);
            }
            
            if (tessControlSource != null) {
                result.tessControlResult = mapper.translate(tessControlSource, 
                                                            GLSLShaderType.TESS_CONTROL,
                                                            null, targetVersion);
            }
            
            if (tessEvalSource != null) {
                result.tessEvalResult = mapper.translate(tessEvalSource,
                                                         GLSLShaderType.TESS_EVALUATION,
                                                         null, targetVersion);
            }
            
            if (computeSource != null) {
                result.computeResult = mapper.translate(computeSource, GLSLShaderType.COMPUTE,
                                                        null, targetVersion);
            }
            
            result.success = true;
            mapper.setOptimizationLevel(prevOptLevel);
            
        } catch (GLSLException e) {
            result.success = false;
            result.error = e.getMessage();
        }
        
        return result;
    }
    
    public static class ShaderBuildResult {
        public boolean success;
        public String error;
        
        public GLSLTranslationResult vertexResult;
        public GLSLTranslationResult fragmentResult;
        public GLSLTranslationResult geometryResult;
        public GLSLTranslationResult tessControlResult;
        public GLSLTranslationResult tessEvalResult;
        public GLSLTranslationResult computeResult;
        
        public String getVertexSource() {
            return vertexResult != null ? vertexResult.getSource() : null;
        }
        
        public String getFragmentSource() {
            return fragmentResult != null ? fragmentResult.getSource() : null;
        }
        
        public String getGeometrySource() {
            return geometryResult != null ? geometryResult.getSource() : null;
        }
        
        public String getComputeSource() {
            return computeResult != null ? computeResult.getSource() : null;
        }
        
        public List<String> getAllWarnings() {
            List<String> warnings = new ArrayList<>();
            if (vertexResult != null) warnings.addAll(vertexResult.getWarnings());
            if (fragmentResult != null) warnings.addAll(fragmentResult.getWarnings());
            if (geometryResult != null) warnings.addAll(geometryResult.getWarnings());
            if (tessControlResult != null) warnings.addAll(tessControlResult.getWarnings());
            if (tessEvalResult != null) warnings.addAll(tessEvalResult.getWarnings());
            if (computeResult != null) warnings.addAll(computeResult.getWarnings());
            return warnings;
        }
    }
}
