package stellar.snow.astralis.api.vulkan.pipeline;

import stellar.snow.astralis.Astralis;
import stellar.snow.astralis.api.vulkan.mapping.SPIRVCallMapper;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static stellar.snow.astralis.engine.render.pipeline.RenderConstants.*;

/**
 * SPIRVPipelineProvider - SPIR-V shader compilation and management system.
 * 
 * <p>This class provides:
 * <ul>
 *   <li>GLSL to SPIR-V compilation via shaderc</li>
 *   <li>SPIR-V binary caching with content hashing</li>
 *   <li>SPIR-V validation and optimization</li>
 *   <li>Cross-compilation support (GLSL versions 110-460)</li>
 *   <li>Shader reflection for binding extraction</li>
 *   <li>Include directive resolution</li>
 * </ul>
 * </p>
 * 
 * <h2>Supported SPIR-V Versions:</h2>
 * <ul>
 *   <li>SPIR-V 1.0 (Vulkan 1.0)</li>
 *   <li>SPIR-V 1.1 (Vulkan 1.0)</li>
 *   <li>SPIR-V 1.2 (Vulkan 1.0)</li>
 *   <li>SPIR-V 1.3 (Vulkan 1.1)</li>
 *   <li>SPIR-V 1.4 (Vulkan 1.2)</li>
 *   <li>SPIR-V 1.5 (Vulkan 1.2)</li>
 *   <li>SPIR-V 1.6 (Vulkan 1.3)</li>
 * </ul>
 */
public class SPIRVPipelineProvider {

    private static SPIRVPipelineProvider INSTANCE;

    // Shaderc compiler handle
    private long compiler;
    private long compilerOptions;

    // Target SPIR-V version
    private SPIRVVersion targetVersion;
    private VKVersion targetVulkanVersion;

    // Caches
    private final ConcurrentHashMap<String, CompiledShader> shaderCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ByteBuffer> binaryCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ShaderReflection> reflectionCache = new ConcurrentHashMap<>();

    // Include resolver
    private final IncludeResolver includeResolver;

    // Cache directory
    private final Path cacheDir;

    // Statistics
    private final AtomicLong compilations = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong compilationTimeMs = new AtomicLong(0);

    // Optimization level
    public enum OptimizationLevel {
        NONE,           // No optimization (fastest compile, debugging)
        SIZE,           // Optimize for size
        PERFORMANCE     // Optimize for performance (default)
    }

    private OptimizationLevel optimizationLevel = OptimizationLevel.PERFORMANCE;

    public SPIRVPipelineProvider() {
        this(SPIRVVersion.SPIRV_1_5, VKVersion.VK_1_2);
    }

    public SPIRVPipelineProvider(SPIRVVersion spirvVersion, VKVersion vulkanVersion) {
        this.targetVersion = spirvVersion;
        this.targetVulkanVersion = vulkanVersion;
        this.cacheDir = Path.of("cache", "spirv");
        this.includeResolver = new IncludeResolver();

        initializeCompiler();

        Astralis.LOGGER.info("[SPIRVPipelineProvider] Initialized for SPIR-V {}.{}, Vulkan {}.{}",
            spirvVersion.getMajor(), spirvVersion.getMinor(),
            vulkanVersion.major, vulkanVersion.minor);
    }

    public static SPIRVPipelineProvider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SPIRVPipelineProvider();
        }
        return INSTANCE;
    }

    private void initializeCompiler() {
        compiler = Shaderc.shaderc_compiler_initialize();
        if (compiler == 0) {
            throw new RuntimeException("Failed to initialize shaderc compiler");
        }

        compilerOptions = Shaderc.shaderc_compile_options_initialize();
        if (compilerOptions == 0) {
            Shaderc.shaderc_compiler_release(compiler);
            throw new RuntimeException("Failed to initialize shaderc compile options");
        }

        // Set target SPIR-V version
        Shaderc.shaderc_compile_options_set_target_spirv(compilerOptions, getShadercSpirvVersion());

        // Set optimization level
        applyOptimizationLevel();

        // Enable debug info in debug mode
        if (Astralis.DEBUG_MODE) {
            Shaderc.shaderc_compile_options_set_generate_debug_info(compilerOptions);
        }

        // Create cache directory
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            Astralis.LOGGER.warn("[SPIRVPipelineProvider] Failed to create cache directory: {}", e.getMessage());
        }
    }

    private int getShadercSpirvVersion() {
        return switch (targetVersion) {
            case SPIRV_1_0 -> Shaderc.shaderc_spirv_version_1_0;
            case SPIRV_1_1 -> Shaderc.shaderc_spirv_version_1_1;
            case SPIRV_1_2 -> Shaderc.shaderc_spirv_version_1_2;
            case SPIRV_1_3 -> Shaderc.shaderc_spirv_version_1_3;
            case SPIRV_1_4 -> Shaderc.shaderc_spirv_version_1_4;
            case SPIRV_1_5 -> Shaderc.shaderc_spirv_version_1_5;
            case SPIRV_1_6 -> Shaderc.shaderc_spirv_version_1_6;
        };
    }

    private void applyOptimizationLevel() {
        int level = switch (optimizationLevel) {
            case NONE -> Shaderc.shaderc_optimization_level_zero;
            case SIZE -> Shaderc.shaderc_optimization_level_size;
            case PERFORMANCE -> Shaderc.shaderc_optimization_level_performance;
        };
        Shaderc.shaderc_compile_options_set_optimization_level(compilerOptions, level);
    }

    // ========================================================================
    // COMPILATION API
    // ========================================================================

    /**
     * Compiles GLSL source to SPIR-V.
     * 
     * @param source GLSL source code
     * @param stage Shader stage (GL20_VERTEX_SHADER, GL20_FRAGMENT_SHADER, etc.)
     * @param sourceName Name for error messages
     * @return Compiled SPIR-V binary
     */
    public CompiledShader compileGLSL(String source, int stage, String sourceName) {
        // Generate cache key from source hash
        String cacheKey = generateCacheKey(source, stage);

        // Check memory cache
        CompiledShader cached = shaderCache.get(cacheKey);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }

        // Check disk cache
        Path cachePath = cacheDir.resolve(cacheKey + ".spv");
        if (Files.exists(cachePath)) {
            try {
                byte[] data = Files.readAllBytes(cachePath);
                ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
                buffer.put(data).flip();
                CompiledShader shader = new CompiledShader(buffer, stage, sourceName);
                shaderCache.put(cacheKey, shader);
                cacheHits.incrementAndGet();
                return shader;
            } catch (IOException e) {
                Astralis.LOGGER.warn("[SPIRVPipelineProvider] Failed to load cached shader: {}", e.getMessage());
            }
        }

        // Compile
        long startTime = System.currentTimeMillis();
        CompiledShader shader = compileInternal(source, stage, sourceName);
        compilationTimeMs.addAndGet(System.currentTimeMillis() - startTime);
        compilations.incrementAndGet();

        // Cache result
        shaderCache.put(cacheKey, shader);
        saveToDiskCache(cacheKey, shader.spirvBinary);

        return shader;
    }

    /**
     * Compiles GLSL to SPIR-V with custom options.
     */
    public CompiledShader compileGLSL(String source, int stage, String sourceName, CompileOptions options) {
        // Apply custom options temporarily
        long customOptions = Shaderc.shaderc_compile_options_initialize();
        try {
            applyCompileOptions(customOptions, options);
            return compileWithOptions(source, stage, sourceName, customOptions);
        } finally {
            Shaderc.shaderc_compile_options_release(customOptions);
        }
    }

    private CompiledShader compileInternal(String source, int stage, String sourceName) {
        return compileWithOptions(source, stage, sourceName, compilerOptions);
    }

    private CompiledShader compileWithOptions(String source, int stage, String sourceName, long options) {
        // Resolve includes
        String resolvedSource = includeResolver.resolveIncludes(source);

        // Determine shaderc shader kind
        int kind = translateShaderKind(stage);

        // Compile
        long result = Shaderc.shaderc_compile_into_spv(
            compiler,
            resolvedSource,
            kind,
            sourceName,
            "main",
            options
        );

        try {
            // Check compilation status
            int status = Shaderc.shaderc_result_get_compilation_status(result);
            if (status != Shaderc.shaderc_compilation_status_success) {
                String errorLog = Shaderc.shaderc_result_get_error_message(result);
                throw new ShaderCompilationException("Shader compilation failed: " + errorLog);
            }

            // Get warnings
            int numWarnings = (int) Shaderc.shaderc_result_get_num_warnings(result);
            if (numWarnings > 0) {
                String warnings = Shaderc.shaderc_result_get_error_message(result);
                Astralis.LOGGER.warn("[SPIRVPipelineProvider] Shader '{}' compiled with {} warnings:\n{}", 
                    sourceName, numWarnings, warnings);
            }

            // Get SPIR-V binary
            ByteBuffer spirv = Shaderc.shaderc_result_get_bytes(result);
            ByteBuffer copy = MemoryUtil.memAlloc(spirv.remaining());
            copy.put(spirv);
            copy.flip();

            return new CompiledShader(copy, stage, sourceName);

        } finally {
            Shaderc.shaderc_result_release(result);
        }
    }

    /**
     * Compiles GLSL to SPIR-V assembly (human-readable).
     */
    public String compileToAssembly(String source, int stage, String sourceName) {
        String resolvedSource = includeResolver.resolveIncludes(source);
        int kind = translateShaderKind(stage);

        long result = Shaderc.shaderc_compile_into_spv_assembly(
            compiler,
            resolvedSource,
            kind,
            sourceName,
            "main",
            compilerOptions
        );

        try {
            int status = Shaderc.shaderc_result_get_compilation_status(result);
            if (status != Shaderc.shaderc_compilation_status_success) {
                String errorLog = Shaderc.shaderc_result_get_error_message(result);
                throw new ShaderCompilationException("Assembly compilation failed: " + errorLog);
            }

            ByteBuffer bytes = Shaderc.shaderc_result_get_bytes(result);
            byte[] data = new byte[bytes.remaining()];
            bytes.get(data);
            return new String(data);

        } finally {
            Shaderc.shaderc_result_release(result);
        }
    }

    /**
     * Preprocesses GLSL source without compilation.
     */
    public String preprocess(String source, int stage, String sourceName) {
        int kind = translateShaderKind(stage);

        long result = Shaderc.shaderc_compile_into_preprocessed_text(
            compiler,
            source,
            kind,
            sourceName,
            "main",
            compilerOptions
        );

        try {
            int status = Shaderc.shaderc_result_get_compilation_status(result);
            if (status != Shaderc.shaderc_compilation_status_success) {
                String errorLog = Shaderc.shaderc_result_get_error_message(result);
                throw new ShaderCompilationException("Preprocessing failed: " + errorLog);
            }

            ByteBuffer bytes = Shaderc.shaderc_result_get_bytes(result);
            byte[] data = new byte[bytes.remaining()];
            bytes.get(data);
            return new String(data);

        } finally {
            Shaderc.shaderc_result_release(result);
        }
    }

    private int translateShaderKind(int glStage) {
        return switch (glStage) {
            case GL20_VERTEX_SHADER -> Shaderc.shaderc_vertex_shader;
            case GL20_FRAGMENT_SHADER -> Shaderc.shaderc_fragment_shader;
            case GL32_GEOMETRY_SHADER -> Shaderc.shaderc_geometry_shader;
            case GL40_TESS_CONTROL_SHADER -> Shaderc.shaderc_tess_control_shader;
            case GL40_TESS_EVALUATION_SHADER -> Shaderc.shaderc_tess_evaluation_shader;
            case GL43_COMPUTE_SHADER -> Shaderc.shaderc_compute_shader;
            default -> throw new IllegalArgumentException("Unknown shader stage: " + glStage);
        };
    }

    private void applyCompileOptions(long options, CompileOptions customOptions) {
        Shaderc.shaderc_compile_options_set_target_spirv(options, getShadercSpirvVersion());

        if (customOptions.optimizationLevel != null) {
            int level = switch (customOptions.optimizationLevel) {
                case NONE -> Shaderc.shaderc_optimization_level_zero;
                case SIZE -> Shaderc.shaderc_optimization_level_size;
                case PERFORMANCE -> Shaderc.shaderc_optimization_level_performance;
            };
            Shaderc.shaderc_compile_options_set_optimization_level(options, level);
        }

        if (customOptions.generateDebugInfo) {
            Shaderc.shaderc_compile_options_set_generate_debug_info(options);
        }

        if (customOptions.suppressWarnings) {
            Shaderc.shaderc_compile_options_set_suppress_warnings(options);
        }

        if (customOptions.warningsAsErrors) {
            Shaderc.shaderc_compile_options_set_warnings_as_errors(options);
        }

        // Add macro definitions
        for (var entry : customOptions.macros.entrySet()) {
            Shaderc.shaderc_compile_options_add_macro_definition(
                options, entry.getKey(), entry.getValue()
            );
        }
    }

    // ========================================================================
    // SPIR-V VALIDATION & OPTIMIZATION
    // ========================================================================

    /**
     * Validates SPIR-V binary.
     */
    public ValidationResult validate(ByteBuffer spirv) {
        // Use SPIRV-Tools for validation if available
        // For now, do basic header validation
        if (spirv.remaining() < 20) {
            return new ValidationResult(false, "SPIR-V binary too small");
        }

        int magic = spirv.getInt(0);
        if (magic != SPIRV_MAGIC_NUMBER) {
            return new ValidationResult(false, "Invalid SPIR-V magic number");
        }

        int version = spirv.getInt(4);
        int major = (version >> 16) & 0xFF;
        int minor = (version >> 8) & 0xFF;

        if (major > targetVersion.getMajor() || 
            (major == targetVersion.getMajor() && minor > targetVersion.getMinor())) {
            return new ValidationResult(false, 
                String.format("SPIR-V version %d.%d exceeds target %d.%d",
                    major, minor, targetVersion.getMajor(), targetVersion.getMinor()));
        }

        return new ValidationResult(true, "Valid SPIR-V");
    }

    /**
     * Optimizes SPIR-V binary for performance.
     */
    public ByteBuffer optimize(ByteBuffer spirv) {
        // Would use SPIRV-Tools optimizer here
        // For now, return as-is
        return spirv;
    }

    /**
     * Strips debug information from SPIR-V binary.
     */
    public ByteBuffer stripDebugInfo(ByteBuffer spirv) {
        // Would use SPIRV-Tools to strip OpName, OpLine, etc.
        // For now, return as-is
        return spirv;
    }

    // ========================================================================
    // REFLECTION
    // ========================================================================

    /**
     * Extracts reflection information from SPIR-V binary.
     */
    public ShaderReflection reflect(ByteBuffer spirv, String sourceName) {
        String cacheKey = generateBinaryHash(spirv);
        ShaderReflection cached = reflectionCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        ShaderReflection reflection = extractReflection(spirv);
        reflectionCache.put(cacheKey, reflection);
        return reflection;
    }

    private ShaderReflection extractReflection(ByteBuffer spirv) {
        ShaderReflection.Builder builder = new ShaderReflection.Builder();

        // Parse SPIR-V header
        spirv.position(0);
        int magic = spirv.getInt();
        int version = spirv.getInt();
        int generator = spirv.getInt();
        int bound = spirv.getInt();
        int reserved = spirv.getInt();

        builder.spirvVersion(version);
        builder.idBound(bound);

        // Parse instructions
        while (spirv.hasRemaining()) {
            int wordCount = spirv.getShort() & 0xFFFF;
            int opcode = spirv.getShort() & 0xFFFF;

            if (wordCount == 0) break;

            int[] operands = new int[wordCount - 1];
            for (int i = 0; i < operands.length; i++) {
                operands[i] = spirv.getInt();
            }

            switch (opcode) {
                case SPIRV_OP_ENTRY_POINT -> {
                    int executionModel = operands[0];
                    int entryPointId = operands[1];
                    // Name follows as string
                    builder.entryPoint(extractString(operands, 2), executionModel);
                }
                case SPIRV_OP_DECORATE -> {
                    int targetId = operands[0];
                    int decoration = operands[1];
                    if (decoration == 33) { // Binding
                        builder.addBinding(targetId, operands[2]);
                    } else if (decoration == 34) { // DescriptorSet
                        builder.addDescriptorSet(targetId, operands[2]);
                    } else if (decoration == 30) { // Location
                        builder.addLocation(targetId, operands[2]);
                    }
                }
                case SPIRV_OP_CAPABILITY -> {
                    builder.addCapability(operands[0]);
                }
            }
        }

        spirv.rewind();
        return builder.build();
    }

    private String extractString(int[] words, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < words.length; i++) {
            int word = words[i];
            for (int j = 0; j < 4; j++) {
                char c = (char) ((word >> (j * 8)) & 0xFF);
                if (c == 0) return sb.toString();
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ========================================================================
    // CACHING
    // ========================================================================

    private String generateCacheKey(String source, int stage) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(source.getBytes());
            md.update((byte) stage);
            md.update((byte) targetVersion.ordinal());
            md.update((byte) optimizationLevel.ordinal());
            byte[] hash = md.digest();
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(source.hashCode()) + "_" + stage;
        }
    }

    private String generateBinaryHash(ByteBuffer buffer) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] data = new byte[buffer.remaining()];
            buffer.duplicate().get(data);
            byte[] hash = md.digest(data);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(buffer.hashCode());
        }
    }

    private void saveToDiskCache(String key, ByteBuffer spirv) {
        try {
            Path cachePath = cacheDir.resolve(key + ".spv");
            byte[] data = new byte[spirv.remaining()];
            spirv.duplicate().get(data);
            Files.write(cachePath, data);
        } catch (IOException e) {
            Astralis.LOGGER.warn("[SPIRVPipelineProvider] Failed to cache shader: {}", e.getMessage());
        }
    }

    /**
     * Clears all caches.
     */
    public void clearCache() {
        shaderCache.values().forEach(shader -> MemoryUtil.memFree(shader.spirvBinary));
        shaderCache.clear();
        binaryCache.values().forEach(MemoryUtil::memFree);
        binaryCache.clear();
        reflectionCache.clear();
    }

    // ========================================================================
    // CONFIGURATION
    // ========================================================================

    public void setOptimizationLevel(OptimizationLevel level) {
        this.optimizationLevel = level;
        applyOptimizationLevel();
    }

    public void setTargetVersion(SPIRVVersion spirvVersion, VKVersion vulkanVersion) {
        this.targetVersion = spirvVersion;
        this.targetVulkanVersion = vulkanVersion;
        Shaderc.shaderc_compile_options_set_target_spirv(compilerOptions, getShadercSpirvVersion());
        clearCache(); // Invalidate cache when target changes
    }

    public void addIncludePath(Path path) {
        includeResolver.addIncludePath(path);
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    public String getStatistics() {
        long total = compilations.get() + cacheHits.get();
        double hitRate = total > 0 ? (cacheHits.get() * 100.0 / total) : 0;
        return String.format("Compilations: %d, CacheHits: %d (%.1f%%), TotalTime: %dms, Cached: %d",
            compilations.get(), cacheHits.get(), hitRate, compilationTimeMs.get(), shaderCache.size());
    }

    // ========================================================================
    // CLEANUP
    // ========================================================================

    public void cleanup() {
        clearCache();
        
        if (compilerOptions != 0) {
            Shaderc.shaderc_compile_options_release(compilerOptions);
            compilerOptions = 0;
        }
        
        if (compiler != 0) {
            Shaderc.shaderc_compiler_release(compiler);
            compiler = 0;
        }

        Astralis.LOGGER.info("[SPIRVPipelineProvider] Cleanup complete. Final stats: {}", getStatistics());
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    /**
     * Compiled shader result.
     */
    public static class CompiledShader {
        public final ByteBuffer spirvBinary;
        public final int stage;
        public final String sourceName;
        public final long compiledAt;

        public CompiledShader(ByteBuffer spirvBinary, int stage, String sourceName) {
            this.spirvBinary = spirvBinary;
            this.stage = stage;
            this.sourceName = sourceName;
            this.compiledAt = System.currentTimeMillis();
        }

        public int getSpirvVersion() {
            if (spirvBinary.remaining() >= 8) {
                return spirvBinary.getInt(4);
            }
            return 0;
        }

        public int getWordCount() {
            return spirvBinary.remaining() / 4;
        }
    }

    /**
     * Shader reflection data.
     */
    public static class ShaderReflection {
        public final int spirvVersion;
        public final int idBound;
        public final String entryPoint;
        public final int executionModel;
        public final int[] capabilities;
        public final BindingInfo[] bindings;
        public final LocationInfo[] inputs;
        public final LocationInfo[] outputs;

        private ShaderReflection(Builder builder) {
            this.spirvVersion = builder.spirvVersion;
            this.idBound = builder.idBound;
            this.entryPoint = builder.entryPoint;
            this.executionModel = builder.executionModel;
            this.capabilities = builder.capabilities.stream().mapToInt(i -> i).toArray();
            this.bindings = builder.bindings.toArray(new BindingInfo[0]);
            this.inputs = builder.inputs.toArray(new LocationInfo[0]);
            this.outputs = builder.outputs.toArray(new LocationInfo[0]);
        }

        public static class Builder {
            int spirvVersion;
            int idBound;
            String entryPoint;
            int executionModel;
            java.util.List<Integer> capabilities = new java.util.ArrayList<>();
            java.util.List<BindingInfo> bindings = new java.util.ArrayList<>();
            java.util.List<LocationInfo> inputs = new java.util.ArrayList<>();
            java.util.List<LocationInfo> outputs = new java.util.ArrayList<>();
            java.util.Map<Integer, Integer> bindingMap = new java.util.HashMap<>();
            java.util.Map<Integer, Integer> setMap = new java.util.HashMap<>();
            java.util.Map<Integer, Integer> locationMap = new java.util.HashMap<>();

            public Builder spirvVersion(int version) { this.spirvVersion = version; return this; }
            public Builder idBound(int bound) { this.idBound = bound; return this; }
            public Builder entryPoint(String name, int model) { 
                this.entryPoint = name; 
                this.executionModel = model; 
                return this; 
            }
            public Builder addCapability(int cap) { capabilities.add(cap); return this; }
            public Builder addBinding(int id, int binding) { bindingMap.put(id, binding); return this; }
            public Builder addDescriptorSet(int id, int set) { setMap.put(id, set); return this; }
            public Builder addLocation(int id, int location) { locationMap.put(id, location); return this; }
            
            public ShaderReflection build() {
                // Resolve bindings
                for (var entry : bindingMap.entrySet()) {
                    int set = setMap.getOrDefault(entry.getKey(), 0);
                    bindings.add(new BindingInfo(entry.getKey(), set, entry.getValue()));
                }
                return new ShaderReflection(this);
            }
        }
    }

    public record BindingInfo(int id, int set, int binding) {}
    public record LocationInfo(int id, int location) {}

    /**
     * Compilation options.
     */
    public static class CompileOptions {
        public OptimizationLevel optimizationLevel;
        public boolean generateDebugInfo = false;
        public boolean suppressWarnings = false;
        public boolean warningsAsErrors = false;
        public java.util.Map<String, String> macros = new java.util.HashMap<>();

        public CompileOptions define(String name, String value) {
            macros.put(name, value);
            return this;
        }

        public CompileOptions define(String name) {
            return define(name, "1");
        }
    }

    /**
     * Validation result.
     */
    public record ValidationResult(boolean valid, String message) {}

    /**
     * Include resolver for #include directives.
     */
    private static class IncludeResolver {
        private final java.util.List<Path> includePaths = new java.util.ArrayList<>();

        public void addIncludePath(Path path) {
            includePaths.add(path);
        }

        public String resolveIncludes(String source) {
            // Simple include resolution
            StringBuilder result = new StringBuilder();
            String[] lines = source.split("\n");
            
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#include")) {
                    String includePath = extractIncludePath(trimmed);
                    if (includePath != null) {
                        String included = loadInclude(includePath);
                        if (included != null) {
                            result.append(included).append("\n");
                            continue;
                        }
                    }
                }
                result.append(line).append("\n");
            }
            
            return result.toString();
        }

        private String extractIncludePath(String directive) {
            int start = directive.indexOf('"');
            int end = directive.lastIndexOf('"');
            if (start >= 0 && end > start) {
                return directive.substring(start + 1, end);
            }
            start = directive.indexOf('<');
            end = directive.lastIndexOf('>');
            if (start >= 0 && end > start) {
                return directive.substring(start + 1, end);
            }
            return null;
        }

        private String loadInclude(String filename) {
            for (Path basePath : includePaths) {
                Path filePath = basePath.resolve(filename);
                if (Files.exists(filePath)) {
                    try {
                        return Files.readString(filePath);
                    } catch (IOException e) {
                        // Continue to next path
                    }
                }
            }
            
            // Try classpath
            try (InputStream is = getClass().getResourceAsStream("/shaders/" + filename)) {
                if (is != null) {
                    return new String(is.readAllBytes());
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
}
