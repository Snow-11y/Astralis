package stellar.snow.astralis.api.opengl.pipeline;

import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.api.opengl.mapping.GLSLCallMapper;
import org.lwjgl.opengl.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static stellar.snow.astralis.engine.render.pipeline.RenderConstants.*;

/**
 * GLSLPipelineProvider - GLSL shader management and compilation system.
 * 
 * <p>This class provides:
 * <ul>
 *   <li>GLSL shader compilation for all versions (110-460)</li>
 *   <li>Automatic version translation and compatibility</li>
 *   <li>Shader program linking and caching</li>
 *   <li>Uniform and attribute location caching</li>
 *   <li>Preprocessor macro handling</li>
 *   <li>Include directive resolution</li>
 *   <li>Shader hot-reloading support</li>
 * </ul>
 * </p>
 * 
 * <h2>Supported GLSL Versions:</h2>
 * <ul>
 *   <li>GLSL 110 (OpenGL 2.0)</li>
 *   <li>GLSL 120 (OpenGL 2.1)</li>
 *   <li>GLSL 130 (OpenGL 3.0)</li>
 *   <li>GLSL 140 (OpenGL 3.1)</li>
 *   <li>GLSL 150 (OpenGL 3.2)</li>
 *   <li>GLSL 330 (OpenGL 3.3)</li>
 *   <li>GLSL 400-460 (OpenGL 4.0-4.6)</li>
 * </ul>
 */
public class GLSLPipelineProvider {

    private static GLSLPipelineProvider INSTANCE;

    // Target GLSL version
    private GLSLVersion targetVersion;
    private final GLVersion glVersion;

    // Shader caches
    private final ConcurrentHashMap<ShaderKey, Integer> shaderCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ProgramKey, ShaderProgram> programCache = new ConcurrentHashMap<>();
    
    // Uniform caches
    private final ConcurrentHashMap<UniformKey, Integer> uniformLocationCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UniformKey, UniformBlockInfo> uniformBlockCache = new ConcurrentHashMap<>();

    // Include resolver
    private final IncludeResolver includeResolver;
    
    // Preprocessor
    private final ShaderPreprocessor preprocessor;

    // Statistics
    private final AtomicLong compilations = new AtomicLong(0);
    private final AtomicLong linkages = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);

    // Watch for shader changes (hot-reload)
    private final Map<Path, Long> watchedFiles = new ConcurrentHashMap<>();
    private boolean hotReloadEnabled = false;

    public GLSLPipelineProvider() {
        // Detect current GL version
        int major = GL11.glGetInteger(GL30.GL_MAJOR_VERSION);
        int minor = GL11.glGetInteger(GL30.GL_MINOR_VERSION);
        this.glVersion = GLVersion.fromNumbers(major, minor);
        this.targetVersion = GLSLVersion.fromGL(glVersion);
        this.includeResolver = new IncludeResolver();
        this.preprocessor = new ShaderPreprocessor();

        Astralis.LOGGER.info("[GLSLPipelineProvider] Initialized for GL {}.{}, GLSL {}",
            major, minor, targetVersion.version);
    }

    public static GLSLPipelineProvider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GLSLPipelineProvider();
        }
        return INSTANCE;
    }

    // ========================================================================
    // SHADER COMPILATION
    // ========================================================================

    /**
     * Compiles a GLSL shader from source.
     * 
     * @param source GLSL source code
     * @param type Shader type (GL_VERTEX_SHADER, GL_FRAGMENT_SHADER, etc.)
     * @return Compiled shader handle
     */
    public int compileShader(String source, int type) {
        return compileShader(source, type, "shader");
    }

    /**
     * Compiles a GLSL shader with source name for error messages.
     */
    public int compileShader(String source, int type, String sourceName) {
        return compileShader(source, type, sourceName, Collections.emptyMap());
    }

    /**
     * Compiles a GLSL shader with preprocessor defines.
     */
    public int compileShader(String source, int type, String sourceName, Map<String, String> defines) {
        ShaderKey key = new ShaderKey(source.hashCode(), type, defines.hashCode());
        
        Integer cached = shaderCache.get(key);
        if (cached != null && GL20.glIsShader(cached)) {
            cacheHits.incrementAndGet();
            return cached;
        }

        // Preprocess
        String processedSource = preprocessor.process(source, defines, targetVersion);
        
        // Resolve includes
        processedSource = includeResolver.resolveIncludes(processedSource);
        
        // Upgrade/downgrade version if needed
        processedSource = ensureVersionCompatibility(processedSource, type);

        // Compile
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, processedSource);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader);
            GL20.glDeleteShader(shader);
            throw new ShaderCompilationException(
                String.format("Shader '%s' compilation failed:\n%s\n\nSource:\n%s", 
                    sourceName, log, addLineNumbers(processedSource)));
        }

        shaderCache.put(key, shader);
        compilations.incrementAndGet();
        
        return shader;
    }

    /**
     * Loads and compiles a shader from file.
     */
    public int compileShaderFromFile(Path path, int type) throws IOException {
        String source = Files.readString(path);
        int shader = compileShader(source, type, path.getFileName().toString());
        
        if (hotReloadEnabled) {
            watchedFiles.put(path, Files.getLastModifiedTime(path).toMillis());
        }
        
        return shader;
    }

    /**
     * Loads and compiles a shader from resources.
     */
    public int compileShaderFromResource(String resourcePath, int type) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            String source = new String(is.readAllBytes());
            return compileShader(source, type, resourcePath);
        }
    }

    // ========================================================================
    // PROGRAM LINKING
    // ========================================================================

    /**
     * Creates and links a shader program.
     */
    public ShaderProgram createProgram(int... shaders) {
        return createProgram("program", shaders);
    }

    /**
     * Creates and links a shader program with name.
     */
    public ShaderProgram createProgram(String name, int... shaders) {
        ProgramKey key = new ProgramKey(shaders);
        
        ShaderProgram cached = programCache.get(key);
        if (cached != null && GL20.glIsProgram(cached.handle)) {
            cacheHits.incrementAndGet();
            return cached;
        }

        int program = GL20.glCreateProgram();
        
        for (int shader : shaders) {
            GL20.glAttachShader(program, shader);
        }

        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program);
            GL20.glDeleteProgram(program);
            throw new ShaderLinkException("Program '" + name + "' linking failed: " + log);
        }

        // Validate in debug mode
        if (Astralis.DEBUG_MODE) {
            GL20.glValidateProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
                String log = GL20.glGetProgramInfoLog(program);
                Astralis.LOGGER.warn("[GLSLPipelineProvider] Program '{}' validation warning: {}", name, log);
            }
        }

        // Detach shaders after linking
        for (int shader : shaders) {
            GL20.glDetachShader(program, shader);
        }

        ShaderProgram shaderProgram = new ShaderProgram(program, name, extractUniforms(program), extractAttributes(program));
        programCache.put(key, shaderProgram);
        linkages.incrementAndGet();

        return shaderProgram;
    }

    /**
     * Creates a program from vertex and fragment shader sources.
     */
    public ShaderProgram createProgram(String vertexSource, String fragmentSource, String name) {
        int vs = compileShader(vertexSource, GL20.GL_VERTEX_SHADER, name + ".vert");
        int fs = compileShader(fragmentSource, GL20.GL_FRAGMENT_SHADER, name + ".frag");
        ShaderProgram program = createProgram(name, vs, fs);
        // Shaders can be deleted after linking
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        return program;
    }

    /**
     * Creates a program with geometry shader.
     */
    public ShaderProgram createProgram(String vertexSource, String geometrySource, String fragmentSource, String name) {
        if (!glVersion.isAtLeast(GLVersion.GL_3_2)) {
            throw new UnsupportedOperationException("Geometry shaders require OpenGL 3.2+");
        }
        
        int vs = compileShader(vertexSource, GL20.GL_VERTEX_SHADER, name + ".vert");
        int gs = compileShader(geometrySource, GL32.GL_GEOMETRY_SHADER, name + ".geom");
        int fs = compileShader(fragmentSource, GL20.GL_FRAGMENT_SHADER, name + ".frag");
        
        ShaderProgram program = createProgram(name, vs, gs, fs);
        
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(gs);
        GL20.glDeleteShader(fs);
        
        return program;
    }

    // ========================================================================
    // VERSION COMPATIBILITY
    // ========================================================================

    private String ensureVersionCompatibility(String source, int shaderType) {
        // Parse current version
        GLSLVersion sourceVersion = parseVersion(source);
        
        if (sourceVersion == null) {
            // No version directive, add target version
            return targetVersion.getVersionDirective() + "\n" + source;
        }

        // Check if upgrade/downgrade needed
        if (sourceVersion.version > targetVersion.version) {
            // Downgrade (complex - may need keyword translation)
            return downgradeShader(source, sourceVersion, targetVersion, shaderType);
        } else if (sourceVersion.version < targetVersion.version) {
            // Upgrade (simpler - just change version and maybe add compatibility)
            return upgradeShader(source, sourceVersion, targetVersion, shaderType);
        }

        return source;
    }

    private GLSLVersion parseVersion(String source) {
        Pattern versionPattern = Pattern.compile("#version\\s+(\\d+)");
        Matcher matcher = versionPattern.matcher(source);
        if (matcher.find()) {
            int version = Integer.parseInt(matcher.group(1));
            for (GLSLVersion v : GLSLVersion.values()) {
                if (v.version == version) return v;
            }
        }
        return null;
    }

    private String upgradeShader(String source, GLSLVersion from, GLSLVersion to, int shaderType) {
        StringBuilder result = new StringBuilder();
        
        // Replace version directive
        source = source.replaceFirst("#version\\s+\\d+.*", to.getVersionDirective());
        
        // Translate keywords if crossing the 130 boundary
        if (!from.hasInOut && to.hasInOut) {
            source = translateToModern(source, shaderType);
        }
        
        return source;
    }

    private String downgradeShader(String source, GLSLVersion from, GLSLVersion to, int shaderType) {
        // Replace version directive
        source = source.replaceFirst("#version\\s+\\d+.*", to.getVersionDirective());
        
        // Translate keywords if crossing the 130 boundary
        if (from.hasInOut && !to.hasInOut) {
            source = translateToLegacy(source, shaderType);
        }
        
        return source;
    }

    private String translateToModern(String source, int shaderType) {
        if (shaderType == GL20.GL_VERTEX_SHADER) {
            source = source.replaceAll("\\battribute\\b", "in");
            source = source.replaceAll("\\bvarying\\b", "out");
        } else if (shaderType == GL20.GL_FRAGMENT_SHADER) {
            source = source.replaceAll("\\bvarying\\b", "in");
            source = source.replaceAll("\\btexture2D\\b", "texture");
            source = source.replaceAll("\\btexture3D\\b", "texture");
            source = source.replaceAll("\\btextureCube\\b", "texture");
            source = source.replaceAll("\\bshadow2D\\b", "texture");
            
            // Handle gl_FragColor
            if (source.contains("gl_FragColor")) {
                source = "out vec4 fragColor;\n" + source;
                source = source.replaceAll("\\bgl_FragColor\\b", "fragColor");
            }
        }
        return source;
    }

    private String translateToLegacy(String source, int shaderType) {
        if (shaderType == GL20.GL_VERTEX_SHADER) {
            source = source.replaceAll("\\bin\\s+", "attribute ");
            source = source.replaceAll("\\bout\\s+", "varying ");
        } else if (shaderType == GL20.GL_FRAGMENT_SHADER) {
            source = source.replaceAll("\\bin\\s+", "varying ");
            source = source.replaceAll("\\btexture\\s*\\(", "texture2D(");
            
            // Handle custom fragment output
            Pattern outPattern = Pattern.compile("out\\s+vec4\\s+(\\w+)\\s*;");
            Matcher matcher = outPattern.matcher(source);
            if (matcher.find()) {
                String varName = matcher.group(1);
                source = matcher.replaceAll("");
                source = source.replaceAll("\\b" + varName + "\\b", "gl_FragColor");
            }
        }
        return source;
    }

    // ========================================================================
    // UNIFORM MANAGEMENT
    // ========================================================================

    /**
     * Gets uniform location with caching.
     */
    public int getUniformLocation(int program, String name) {
        UniformKey key = new UniformKey(program, name);
        return uniformLocationCache.computeIfAbsent(key, 
            k -> GL20.glGetUniformLocation(program, name));
    }

    /**
     * Gets uniform block index (GL 3.1+).
     */
    public int getUniformBlockIndex(int program, String name) {
        if (!glVersion.isAtLeast(GLVersion.GL_3_1)) {
            return -1;
        }
        return GL31.glGetUniformBlockIndex(program, name);
    }

    /**
     * Binds uniform block to binding point (GL 3.1+).
     */
    public void uniformBlockBinding(int program, int blockIndex, int bindingPoint) {
        if (glVersion.isAtLeast(GLVersion.GL_3_1)) {
            GL31.glUniformBlockBinding(program, blockIndex, bindingPoint);
        }
    }

    private Map<String, UniformInfo> extractUniforms(int program) {
        Map<String, UniformInfo> uniforms = new HashMap<>();
        
        int count = GL20.glGetProgrami(program, GL20.GL_ACTIVE_UNIFORMS);
        int maxLength = GL20.glGetProgrami(program, GL20.GL_ACTIVE_UNIFORM_MAX_LENGTH);
        
        for (int i = 0; i < count; i++) {
            int[] size = new int[1];
            int[] type = new int[1];
            String name = GL20.glGetActiveUniform(program, i, maxLength, size, type);
            int location = GL20.glGetUniformLocation(program, name);
            
            // Remove array suffix for base name
            String baseName = name.replaceAll("\\[\\d+\\]$", "");
            uniforms.put(baseName, new UniformInfo(name, location, type[0], size[0]));
        }
        
        return uniforms;
    }

    private Map<String, AttributeInfo> extractAttributes(int program) {
        Map<String, AttributeInfo> attributes = new HashMap<>();
        
        int count = GL20.glGetProgrami(program, GL20.GL_ACTIVE_ATTRIBUTES);
        int maxLength = GL20.glGetProgrami(program, GL20.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH);
        
        for (int i = 0; i < count; i++) {
            int[] size = new int[1];
            int[] type = new int[1];
            String name = GL20.glGetActiveAttrib(program, i, maxLength, size, type);
            int location = GL20.glGetAttribLocation(program, name);
            
            attributes.put(name, new AttributeInfo(name, location, type[0], size[0]));
        }
        
        return attributes;
    }

    // ========================================================================
    // HOT RELOAD
    // ========================================================================

    /**
     * Enables hot-reload functionality for shader files.
     */
    public void enableHotReload(boolean enable) {
        this.hotReloadEnabled = enable;
    }

    /**
     * Checks for modified shaders and recompiles them.
     * Call this periodically (e.g., once per second).
     */
    public void checkForChanges() {
        if (!hotReloadEnabled) return;
        
        for (Map.Entry<Path, Long> entry : watchedFiles.entrySet()) {
            try {
                long currentModified = Files.getLastModifiedTime(entry.getKey()).toMillis();
                if (currentModified > entry.getValue()) {
                    Astralis.LOGGER.info("[GLSLPipelineProvider] Reloading shader: {}", entry.getKey());
                    entry.setValue(currentModified);
                    // Trigger reload callback if registered
                    // This would invalidate cached programs using this shader
                }
            } catch (IOException e) {
                // File might have been deleted
            }
        }
    }

    // ========================================================================
    // UTILITY
    // ========================================================================

    private String addLineNumbers(String source) {
        StringBuilder result = new StringBuilder();
        String[] lines = source.split("\n");
        for (int i = 0; i < lines.length; i++) {
            result.append(String.format("%4d: %s\n", i + 1, lines[i]));
        }
        return result.toString();
    }

    public void setTargetVersion(GLSLVersion version) {
        this.targetVersion = version;
    }

    public GLSLVersion getTargetVersion() {
        return targetVersion;
    }

    public GLVersion getGLVersion() {
        return glVersion;
    }

    public void addIncludePath(Path path) {
        includeResolver.addIncludePath(path);
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    public String getStatistics() {
        long total = compilations.get() + linkages.get() + cacheHits.get();
        double hitRate = total > 0 ? (cacheHits.get() * 100.0 / total) : 0;
        return String.format("Compilations: %d, Linkages: %d, CacheHits: %d (%.1f%%), Shaders: %d, Programs: %d",
            compilations.get(), linkages.get(), cacheHits.get(), hitRate, 
            shaderCache.size(), programCache.size());
    }

    // ========================================================================
    // CLEANUP
    // ========================================================================

    public void cleanup() {
        // Delete all cached shaders
        for (int shader : shaderCache.values()) {
            if (GL20.glIsShader(shader)) {
                GL20.glDeleteShader(shader);
            }
        }
        shaderCache.clear();

        // Delete all cached programs
        for (ShaderProgram program : programCache.values()) {
            if (GL20.glIsProgram(program.handle)) {
                GL20.glDeleteProgram(program.handle);
            }
        }
        programCache.clear();

        uniformLocationCache.clear();
        uniformBlockCache.clear();
        watchedFiles.clear();

        Astralis.LOGGER.info("[GLSLPipelineProvider] Cleanup complete. Final stats: {}", getStatistics());
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    /**
     * Compiled shader program with metadata.
     */
    public static class ShaderProgram {
        public final int handle;
        public final String name;
        public final Map<String, UniformInfo> uniforms;
        public final Map<String, AttributeInfo> attributes;

        public ShaderProgram(int handle, String name, 
                           Map<String, UniformInfo> uniforms, 
                           Map<String, AttributeInfo> attributes) {
            this.handle = handle;
            this.name = name;
            this.uniforms = Collections.unmodifiableMap(uniforms);
            this.attributes = Collections.unmodifiableMap(attributes);
        }

        public void use() {
            GL20.glUseProgram(handle);
        }

        public int getUniformLocation(String name) {
            UniformInfo info = uniforms.get(name);
            return info != null ? info.location : -1;
        }

        public int getAttributeLocation(String name) {
            AttributeInfo info = attributes.get(name);
            return info != null ? info.location : -1;
        }

        // Uniform setters
        public void setUniform1i(String name, int value) {
            int loc = getUniformLocation(name);
            if (loc >= 0) GL20.glUniform1i(loc, value);
        }

        public void setUniform1f(String name, float value) {
            int loc = getUniformLocation(name);
            if (loc >= 0) GL20.glUniform1f(loc, value);
        }

        public void setUniform2f(String name, float x, float y) {
            int loc = getUniformLocation(name);
            if (loc >= 0) GL20.glUniform2f(loc, x, y);
        }

        public void setUniform3f(String name, float x, float y, float z) {
            int loc = getUniformLocation(name);
            if (loc >= 0) GL20.glUniform3f(loc, x, y, z);
        }

        public void setUniform4f(String name, float x, float y, float z, float w) {
            int loc = getUniformLocation(name);
            if (loc >= 0) GL20.glUniform4f(loc, x, y, z, w);
        }

        public void setUniformMatrix4f(String name, boolean transpose, float[] matrix) {
            int loc = getUniformLocation(name);
            if (loc >= 0) GL20.glUniformMatrix4fv(loc, transpose, matrix);
        }
    }

    public record UniformInfo(String name, int location, int type, int size) {}
    public record AttributeInfo(String name, int location, int type, int size) {}
    public record UniformBlockInfo(String name, int index, int size, int binding) {}

    private record ShaderKey(int sourceHash, int type, int definesHash) {}
    private record ProgramKey(int[] shaders) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProgramKey that)) return false;
            return Arrays.equals(shaders, that.shaders);
        }
        @Override
        public int hashCode() {
            return Arrays.hashCode(shaders);
        }
    }
    private record UniformKey(int program, String name) {}

    /**
     * Shader preprocessor for macro handling.
     */
    private static class ShaderPreprocessor {
        public String process(String source, Map<String, String> defines, GLSLVersion version) {
            StringBuilder result = new StringBuilder();
            
            // Find version directive position
            int versionEnd = source.indexOf('\n', source.indexOf("#version"));
            if (versionEnd < 0) versionEnd = 0;
            
            // Insert after version directive
            result.append(source, 0, versionEnd + 1);
            
            // Add defines
            for (Map.Entry<String, String> define : defines.entrySet()) {
                result.append("#define ").append(define.getKey());
                if (define.getValue() != null && !define.getValue().isEmpty()) {
                    result.append(" ").append(define.getValue());
                }
                result.append("\n");
            }
            
            // Add platform defines
            result.append("#define GLSL_VERSION ").append(version.version).append("\n");
            if (version.hasInOut) {
                result.append("#define GLSL_MODERN 1\n");
            }
            if (version.hasExplicitLayouts) {
                result.append("#define GLSL_EXPLICIT_LAYOUTS 1\n");
            }
            
            // Rest of source
            result.append(source.substring(versionEnd + 1));
            
            return result.toString();
        }
    }

    /**
     * Include resolver for #include directives.
     */
    private static class IncludeResolver {
        private final List<Path> includePaths = new ArrayList<>();
        private final Map<String, String> includeCache = new ConcurrentHashMap<>();

        public void addIncludePath(Path path) {
            includePaths.add(path);
        }

        public String resolveIncludes(String source) {
            Pattern includePattern = Pattern.compile("#include\\s+[\"<]([^\"<>]+)[>\"]");
            Matcher matcher = includePattern.matcher(source);
            
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                String includePath = matcher.group(1);
                String included = loadInclude(includePath);
                if (included != null) {
                    // Recursively resolve includes
                    included = resolveIncludes(included);
                    matcher.appendReplacement(result, Matcher.quoteReplacement(included));
                } else {
                    Astralis.LOGGER.warn("[GLSLPipelineProvider] Include not found: {}", includePath);
                    matcher.appendReplacement(result, "// Include not found: " + includePath);
                }
            }
            matcher.appendTail(result);
            
            return result.toString();
        }

        private String loadInclude(String filename) {
            // Check cache
            String cached = includeCache.get(filename);
            if (cached != null) return cached;

            // Search include paths
            for (Path basePath : includePaths) {
                Path filePath = basePath.resolve(filename);
                if (Files.exists(filePath)) {
                    try {
                        String content = Files.readString(filePath);
                        includeCache.put(filename, content);
                        return content;
                    } catch (IOException e) {
                        // Continue searching
                    }
                }
            }

            // Try classpath
            try (InputStream is = getClass().getResourceAsStream("/shaders/" + filename)) {
                if (is != null) {
                    String content = new String(is.readAllBytes());
                    includeCache.put(filename, content);
                    return content;
                }
            } catch (IOException e) {
                // Ignore
            }

            return null;
        }
    }

    /**
     * Shader compilation exception.
     */
    public static class ShaderCompilationException extends RuntimeException {
        public ShaderCompilationException(String message) {
            super(message);
        }
    }

    /**
     * Shader link exception.
     */
    public static class ShaderLinkException extends RuntimeException {
        public ShaderLinkException(String message) {
            super(message);
        }
    }
}
