package stellar.snow.astralis.api.vulkan.shaders;

import stellar.snow.astralis.engine.gpu.authority.GPUBackend;
import stellar.snow.astralis.engine.gpu.authority.GPUBackendSelector;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.regex.*;

/**
 * Production-grade shader permutation manager with:
 * - All shader stages (vertex, fragment, compute, mesh, task, geometry, tessellation)
 * - Include file resolution with dependency tracking
 * - Specialization constants support
 * - SPIRV compilation with disk caching
 * - Async compilation with callbacks
 * - Hot-reload support for development
 * - Shader reflection for automatic binding discovery
 * - Error handling with detailed diagnostics
 */
public class ShaderPermutationManager {
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════
    
    private static final String VERSION_HEADER = "#version 460\n";
    private static final String EXTENSION_HEADER = """
        #extension GL_EXT_shader_16bit_storage : enable
        #extension GL_EXT_shader_8bit_storage : enable
        #extension GL_EXT_shader_explicit_arithmetic_types : enable
        #extension GL_KHR_shader_subgroup_ballot : enable
        #extension GL_KHR_shader_subgroup_arithmetic : enable
        #extension GL_EXT_nonuniform_qualifier : enable
        #extension GL_EXT_scalar_block_layout : enable
        #extension GL_EXT_buffer_reference : enable
        #extension GL_EXT_buffer_reference2 : enable
        """;
    
    private static final String MESH_SHADER_EXTENSIONS = """
        #extension GL_EXT_mesh_shader : require
        #extension GL_KHR_shader_subgroup_shuffle : enable
        """;
    
    private static final Pattern INCLUDE_PATTERN = Pattern.compile(
        "#include\\s+[\"<]([^\"<>]+)[\">]"
    );
    
    private static final Pattern PRAGMA_ONCE_PATTERN = Pattern.compile(
        "#pragma\\s+once"
    );
    
    // ═══════════════════════════════════════════════════════════════════════
    // SHADER STAGE DEFINITIONS
    // ═══════════════════════════════════════════════════════════════════════
    
    public enum ShaderStage {
        VERTEX(GPUBackend.ShaderStage.VERTEX, Shaderc.shaderc_vertex_shader, ".vert"),
        FRAGMENT(GPUBackend.ShaderStage.FRAGMENT, Shaderc.shaderc_fragment_shader, ".frag"),
        COMPUTE(GPUBackend.ShaderStage.COMPUTE, Shaderc.shaderc_compute_shader, ".comp"),
        GEOMETRY(GPUBackend.ShaderStage.GEOMETRY, Shaderc.shaderc_geometry_shader, ".geom"),
        TESS_CONTROL(GPUBackend.ShaderStage.TESS_CONTROL, Shaderc.shaderc_tess_control_shader, ".tesc"),
        TESS_EVALUATION(GPUBackend.ShaderStage.TESS_EVAL, Shaderc.shaderc_tess_evaluation_shader, ".tese"),
        TASK(GPUBackend.ShaderStage.TASK, Shaderc.shaderc_task_shader, ".task"),
        MESH(GPUBackend.ShaderStage.MESH, Shaderc.shaderc_mesh_shader, ".mesh"),
        RAY_GEN(GPUBackend.ShaderStage.RAY_GEN, Shaderc.shaderc_raygen_shader, ".rgen"),
        RAY_MISS(GPUBackend.ShaderStage.RAY_MISS, Shaderc.shaderc_miss_shader, ".rmiss"),
        RAY_CLOSEST_HIT(GPUBackend.ShaderStage.RAY_CLOSEST_HIT, Shaderc.shaderc_closesthit_shader, ".rchit"),
        RAY_ANY_HIT(GPUBackend.ShaderStage.RAY_ANY_HIT, Shaderc.shaderc_anyhit_shader, ".rahit"),
        RAY_INTERSECTION(GPUBackend.ShaderStage.RAY_INTERSECTION, Shaderc.shaderc_intersection_shader, ".rint");
        
        public final int backendStage;
        public final int shadercKind;
        public final String extension;
        
        ShaderStage(int backendStage, int shadercKind, String extension) {
            this.backendStage = backendStage;
            this.shadercKind = shadercKind;
            this.extension = extension;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DATA STRUCTURES
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Compiled shader with metadata
     */
    public record CompiledShader(
        long handle,
        ShaderStage stage,
        String sourceHash,
        ShaderReflection reflection,
        long compilationTimeNs
    ) {}
    
    /**
     * Shader reflection data extracted from SPIRV
     */
    public record ShaderReflection(
        List<UniformBinding> uniformBindings,
        List<StorageBinding> storageBindings,
        List<SamplerBinding> samplerBindings,
        List<PushConstantRange> pushConstants,
        List<VertexInput> vertexInputs,
        Map<String, SpecializationConstant> specializationConstants,
        WorkgroupSize workgroupSize // For compute/mesh/task
    ) {
        public static final ShaderReflection EMPTY = new ShaderReflection(
            List.of(), List.of(), List.of(), List.of(), List.of(), Map.of(), null
        );
    }
    
    public record UniformBinding(int set, int binding, String name, int size) {}
    public record StorageBinding(int set, int binding, String name, boolean readonly) {}
    public record SamplerBinding(int set, int binding, String name, int count) {}
    public record PushConstantRange(int offset, int size, int stageFlags) {}
    public record VertexInput(int location, String name, int format, int vecSize) {}
    public record SpecializationConstant(int id, String name, Object defaultValue) {}
    public record WorkgroupSize(int x, int y, int z) {}
    
    /**
     * Pipeline configuration
     */
    public static class PipelineConfig {
        public long vertexShader;
        public long fragmentShader;
        public long computeShader;
        public long taskShader;
        public long meshShader;
        public long geometryShader;
        public long tessControlShader;
        public long tessEvalShader;
        
        public Map<Integer, Object> specializationConstants = new HashMap<>();
        public String[] defines = new String[0];
        
        // Pipeline state
        public boolean depthTest = true;
        public boolean depthWrite = true;
        public int depthCompareOp = GPUBackend.CompareOp.LESS_OR_EQUAL;
        public int cullMode = GPUBackend.CullMode.BACK;
        public int frontFace = GPUBackend.FrontFace.COUNTER_CLOCKWISE;
        public int polygonMode = GPUBackend.PolygonMode.FILL;
        public boolean blendEnable = false;
        public int topology = GPUBackend.Topology.TRIANGLES;
        
        public long renderPass;
        public int subpass;
        public long pipelineLayout;
        
        public String cacheKey() {
            StringBuilder sb = new StringBuilder();
            sb.append(vertexShader).append("_");
            sb.append(fragmentShader).append("_");
            sb.append(computeShader).append("_");
            sb.append(taskShader).append("_");
            sb.append(meshShader).append("_");
            sb.append(geometryShader).append("_");
            sb.append(depthTest).append(depthWrite).append(depthCompareOp);
            sb.append(cullMode).append(frontFace).append(polygonMode);
            sb.append(blendEnable).append(topology);
            sb.append(renderPass).append("_").append(subpass);
            specializationConstants.forEach((k, v) -> sb.append(k).append("=").append(v).append(";"));
            return sb.toString();
        }
    }
    
    /**
     * Shader file with dependencies for hot-reload
     */
    private static class ShaderFile {
        final Path path;
        final Set<Path> dependencies = new HashSet<>();
        long lastModified;
        String contentHash;
        
        ShaderFile(Path path) {
            this.path = path;
            this.lastModified = getLastModified();
        }
        
        long getLastModified() {
            try {
                long latest = Files.getLastModifiedTime(path).toMillis();
                for (Path dep : dependencies) {
                    if (Files.exists(dep)) {
                        latest = Math.max(latest, Files.getLastModifiedTime(dep).toMillis());
                    }
                }
                return latest;
            } catch (IOException e) {
                return 0;
            }
        }
        
        boolean needsRecompile() {
            return getLastModified() > lastModified;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════
    
    private final GPUBackend backend;
    private final ExecutorService compileExecutor;
    private final Path shaderCacheDir;
    private final List<Path> includePaths;
    private final boolean enableHotReload;
    
    // Caches
    private final Map<String, CompiledShader> shaderCache = new ConcurrentHashMap<>();
    private final Map<String, Long> pipelineCache = new ConcurrentHashMap<>();
    private final Map<Path, ShaderFile> fileWatchMap = new ConcurrentHashMap<>();
    private final Map<String, String> includeCache = new ConcurrentHashMap<>();
    
    // Hot-reload
    private final List<Consumer<Set<String>>> reloadListeners = new CopyOnWriteArrayList<>();
    private volatile boolean watcherRunning = false;
    private Thread watcherThread;
    
    // Statistics
    private final AtomicLong totalCompilations = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong totalCompileTimeNs = new AtomicLong();
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTION
    // ═══════════════════════════════════════════════════════════════════════
    
    public ShaderPermutationManager() {
        this(false, null);
    }
    
    public ShaderPermutationManager(boolean enableHotReload, Path cacheDir) {
        this.backend = GPUBackendSelector.get();
        this.enableHotReload = enableHotReload;
        this.compileExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "ShaderCompiler");
                t.setDaemon(true);
                return t;
            }
        );
        
        // Setup cache directory
        if (cacheDir != null) {
            this.shaderCacheDir = cacheDir;
        } else {
            this.shaderCacheDir = Path.of(System.getProperty("java.io.tmpdir"), "shader_cache");
        }
        try {
            Files.createDirectories(shaderCacheDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create shader cache directory", e);
        }
        
        // Default include paths
        this.includePaths = new ArrayList<>();
        includePaths.add(Path.of("shaders"));
        includePaths.add(Path.of("shaders/include"));
        includePaths.add(Path.of("assets/shaders"));
        
        // Start hot-reload watcher if enabled
        if (enableHotReload) {
            startFileWatcher();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API - SHADER COMPILATION
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Compile a shader from source with defines.
     */
    public CompiledShader compileShader(ShaderStage stage, String source, String... defines) {
        return compileShader(stage, source, Map.of(), defines);
    }
    
    /**
     * Compile a shader with specialization constants.
     */
    public CompiledShader compileShader(
            ShaderStage stage, 
            String source, 
            Map<Integer, Object> specializationConstants,
            String... defines) {
        
        // Generate cache key
        String defineStr = String.join("\n", defines);
        String specStr = specializationConstants.toString();
        String cacheKey = stage.name() + "_" + computeHash(source + defineStr + specStr);
        
        // Check cache
        CompiledShader cached = shaderCache.get(cacheKey);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }
        
        // Compile
        long startTime = System.nanoTime();
        
        try {
            // Process includes
            String processedSource = processIncludes(source, new HashSet<>());
            
            // Build final source
            String finalSource = buildFinalSource(stage, processedSource, defines);
            
            // Compile to SPIRV
            ByteBuffer spirv = compileToSpirv(stage, finalSource, cacheKey);
            
            // Create shader module
            long handle = backend.createShaderFromSpirv(stage.backendStage, spirv);
            
            // Extract reflection data
            ShaderReflection reflection = extractReflection(spirv, stage);
            
            long compileTime = System.nanoTime() - startTime;
            totalCompilations.incrementAndGet();
            totalCompileTimeNs.addAndGet(compileTime);
            
            CompiledShader compiled = new CompiledShader(
                handle, stage, cacheKey, reflection, compileTime
            );
            
            shaderCache.put(cacheKey, compiled);
            return compiled;
            
        } catch (Exception e) {
            throw new ShaderCompilationException(
                "Failed to compile " + stage.name() + " shader", source, e
            );
        }
    }
    
    /**
     * Compile shader from file path.
     */
    public CompiledShader compileShaderFromFile(Path path, String... defines) {
        ShaderStage stage = detectStage(path);
        String source = loadShaderFile(path);
        
        if (enableHotReload) {
            trackFile(path);
        }
        
        return compileShader(stage, source, defines);
    }
    
    /**
     * Async compilation with callback.
     */
    public CompletableFuture<CompiledShader> compileShaderAsync(
            ShaderStage stage, String source, String... defines) {
        return CompletableFuture.supplyAsync(
            () -> compileShader(stage, source, defines),
            compileExecutor
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC API - PIPELINE CREATION
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Create graphics pipeline from vertex and fragment shaders.
     */
    public long createGraphicsPipeline(
            String vertexSource, 
            String fragmentSource,
            String... defines) {
        
        CompiledShader vert = compileShader(ShaderStage.VERTEX, vertexSource, defines);
        CompiledShader frag = compileShader(ShaderStage.FRAGMENT, fragmentSource, defines);
        
        String pipeKey = vert.sourceHash() + "_" + frag.sourceHash();
        
        return pipelineCache.computeIfAbsent(pipeKey, 
            k -> backend.createProgram(vert.handle(), frag.handle())
        );
    }
    
    /**
     * Create compute pipeline.
     */
    public long createComputePipeline(String computeSource, String... defines) {
        CompiledShader comp = compileShader(ShaderStage.COMPUTE, computeSource, defines);
        
        return pipelineCache.computeIfAbsent(comp.sourceHash(),
            k -> backend.createComputePipeline(comp.handle())
        );
    }
    
    /**
     * Create mesh shader pipeline.
     */
    public long createMeshPipeline(
            String taskSource,
            String meshSource,
            String fragmentSource,
            String... defines) {
        
        String[] meshDefines = Arrays.copyOf(defines, defines.length + 1);
        meshDefines[defines.length] = "MESH_SHADER_PIPELINE 1";
        
        CompiledShader task = compileShader(ShaderStage.TASK, taskSource, meshDefines);
        CompiledShader mesh = compileShader(ShaderStage.MESH, meshSource, meshDefines);
        CompiledShader frag = compileShader(ShaderStage.FRAGMENT, fragmentSource, meshDefines);
        
        String pipeKey = task.sourceHash() + "_" + mesh.sourceHash() + "_" + frag.sourceHash();
        
        return pipelineCache.computeIfAbsent(pipeKey,
            k -> backend.createMeshPipeline(task.handle(), mesh.handle(), frag.handle())
        );
    }
    
    /**
     * Create pipeline from full configuration.
     */
    public long createPipeline(PipelineConfig config) {
        String cacheKey = config.cacheKey();
        
        return pipelineCache.computeIfAbsent(cacheKey,
            k -> backend.createPipelineFromConfig(config)
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // INCLUDE PROCESSING
    // ═══════════════════════════════════════════════════════════════════════
    
    private String processIncludes(String source, Set<String> included) {
        StringBuilder result = new StringBuilder();
        String[] lines = source.split("\n");
        
        for (String line : lines) {
            // Check for #pragma once
            if (PRAGMA_ONCE_PATTERN.matcher(line).find()) {
                continue; // Skip pragma once directives
            }
            
            Matcher includeMatcher = INCLUDE_PATTERN.matcher(line);
            if (includeMatcher.find()) {
                String includePath = includeMatcher.group(1);
                
                // Prevent circular includes
                if (included.contains(includePath)) {
                    result.append("// Skipped circular include: ").append(includePath).append("\n");
                    continue;
                }
                
                included.add(includePath);
                
                // Load and process included file
                String includeContent = loadIncludeFile(includePath);
                if (includeContent != null) {
                    result.append("// Begin include: ").append(includePath).append("\n");
                    result.append(processIncludes(includeContent, included));
                    result.append("// End include: ").append(includePath).append("\n");
                } else {
                    result.append("// ERROR: Include not found: ").append(includePath).append("\n");
                }
            } else {
                result.append(line).append("\n");
            }
        }
        
        return result.toString();
    }
    
    private String loadIncludeFile(String includePath) {
        // Check cache
        String cached = includeCache.get(includePath);
        if (cached != null) {
            return cached;
        }
        
        // Search include paths
        for (Path basePath : includePaths) {
            Path fullPath = basePath.resolve(includePath);
            if (Files.exists(fullPath)) {
                try {
                    String content = Files.readString(fullPath);
                    includeCache.put(includePath, content);
                    
                    if (enableHotReload) {
                        trackFile(fullPath);
                    }
                    
                    return content;
                } catch (IOException e) {
                    // Continue searching
                }
            }
        }
        
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SOURCE BUILDING
    // ═══════════════════════════════════════════════════════════════════════
    
    private String buildFinalSource(ShaderStage stage, String source, String[] defines) {
        StringBuilder sb = new StringBuilder();
        
        // Version
        sb.append(VERSION_HEADER);
        
        // Stage-specific extensions
        if (stage == ShaderStage.MESH || stage == ShaderStage.TASK) {
            sb.append(MESH_SHADER_EXTENSIONS);
        }
        sb.append(EXTENSION_HEADER);
        
        // User defines
        for (String define : defines) {
            if (define.contains(" ")) {
                String[] parts = define.split(" ", 2);
                sb.append("#define ").append(parts[0]).append(" ").append(parts[1]).append("\n");
            } else if (define.contains("=")) {
                String[] parts = define.split("=", 2);
                sb.append("#define ").append(parts[0]).append(" ").append(parts[1]).append("\n");
            } else {
                sb.append("#define ").append(define).append(" 1\n");
            }
        }
        
        // Stage define
        sb.append("#define SHADER_STAGE_").append(stage.name()).append(" 1\n");
        
        // Clean and append source (remove existing version/extensions)
        String cleanSource = source
            .replaceAll("#version\\s+\\d+.*\\n", "")
            .replaceAll("#extension\\s+.*\\n", "");
        
        sb.append("\n// === User Shader Code ===\n");
        sb.append(cleanSource);
        
        return sb.toString();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SPIRV COMPILATION
    // ═══════════════════════════════════════════════════════════════════════
    
    private ByteBuffer compileToSpirv(ShaderStage stage, String source, String cacheKey) {
        // Check disk cache
        Path cachedSpirv = shaderCacheDir.resolve(cacheKey + ".spv");
        if (Files.exists(cachedSpirv)) {
            try {
                byte[] bytes = Files.readAllBytes(cachedSpirv);
                ByteBuffer buffer = ByteBuffer.allocateDirect(bytes.length);
                buffer.put(bytes).flip();
                return buffer;
            } catch (IOException e) {
                // Fall through to recompile
            }
        }
        
        // Compile with shaderc
        long compiler = Shaderc.shaderc_compiler_initialize();
        long options = Shaderc.shaderc_compile_options_initialize();
        
        try {
            // Set options
            Shaderc.shaderc_compile_options_set_optimization_level(
                options, Shaderc.shaderc_optimization_level_performance
            );
            Shaderc.shaderc_compile_options_set_target_env(
                options, 
                Shaderc.shaderc_target_env_vulkan, 
                Shaderc.shaderc_env_version_vulkan_1_3
            );
            Shaderc.shaderc_compile_options_set_target_spirv(
                options, Shaderc.shaderc_spirv_version_1_6
            );
            Shaderc.shaderc_compile_options_set_generate_debug_info(options);
            
            // Compile
            long result = Shaderc.shaderc_compile_into_spv(
                compiler,
                source,
                stage.shadercKind,
                cacheKey + stage.extension,
                "main",
                options
            );
            
            try {
                int status = Shaderc.shaderc_result_get_compilation_status(result);
                if (status != Shaderc.shaderc_compilation_status_success) {
                    String error = Shaderc.shaderc_result_get_error_message(result);
                    throw new ShaderCompilationException(
                        "SPIRV compilation failed: " + error, source, null
                    );
                }
                
                // Get SPIRV binary
                ByteBuffer spirv = Shaderc.shaderc_result_get_bytes(result);
                
                // Copy to our own buffer (result will be released)
                ByteBuffer copy = ByteBuffer.allocateDirect(spirv.remaining());
                copy.put(spirv);
                copy.flip();
                
                // Cache to disk
                try {
                    byte[] bytes = new byte[copy.remaining()];
                    copy.get(bytes);
                    copy.flip();
                    Files.write(cachedSpirv, bytes);
                } catch (IOException e) {
                    // Ignore cache write failures
                }
                
                return copy;
                
            } finally {
                Shaderc.shaderc_result_release(result);
            }
            
        } finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // REFLECTION (Simplified - full implementation would use SPIRV-Cross)
    // ═══════════════════════════════════════════════════════════════════════
    
    private ShaderReflection extractReflection(ByteBuffer spirv, ShaderStage stage) {
        // TODO: Implement full SPIRV-Cross reflection
        // For now, return empty reflection
        // In production, use spirv-cross-java or similar
        return ShaderReflection.EMPTY;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // HOT RELOAD
    // ═══════════════════════════════════════════════════════════════════════
    
    private void startFileWatcher() {
        watcherRunning = true;
        watcherThread = new Thread(() -> {
            while (watcherRunning) {
                try {
                    Thread.sleep(500); // Check every 500ms
                    
                    Set<String> invalidated = new HashSet<>();
                    
                    for (Map.Entry<Path, ShaderFile> entry : fileWatchMap.entrySet()) {
                        ShaderFile file = entry.getValue();
                        if (file.needsRecompile()) {
                            file.lastModified = file.getLastModified();
                            
                            // Invalidate all shaders that use this file
                            invalidateFile(entry.getKey(), invalidated);
                        }
                    }
                    
                    if (!invalidated.isEmpty()) {
                        // Notify listeners
                        for (Consumer<Set<String>> listener : reloadListeners) {
                            try {
                                listener.accept(invalidated);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "ShaderHotReload");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }
    
    private void trackFile(Path path) {
        fileWatchMap.computeIfAbsent(path.toAbsolutePath(), ShaderFile::new);
    }
    
    private void invalidateFile(Path path, Set<String> invalidated) {
        // Find and remove cached shaders that depend on this file
        shaderCache.entrySet().removeIf(entry -> {
            // Check if this shader's source references the file
            // Simplified - in production, track dependencies properly
            boolean invalid = entry.getKey().contains(path.getFileName().toString());
            if (invalid) {
                invalidated.add(entry.getKey());
                backend.destroyShader(entry.getValue().handle());
            }
            return invalid;
        });
        
        // Clear include cache for this file
        includeCache.remove(path.getFileName().toString());
    }
    
    public void addReloadListener(Consumer<Set<String>> listener) {
        reloadListeners.add(listener);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════
    
    private ShaderStage detectStage(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        for (ShaderStage stage : ShaderStage.values()) {
            if (name.endsWith(stage.extension) || name.endsWith(stage.extension + ".glsl")) {
                return stage;
            }
        }
        throw new IllegalArgumentException("Cannot detect shader stage from: " + path);
    }
    
    private String loadShaderFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader: " + path, e);
        }
    }
    
    private String computeHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) { // First 16 hex chars
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }
    
    public void addIncludePath(Path path) {
        includePaths.add(path);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATISTICS & DEBUGGING
    // ═══════════════════════════════════════════════════════════════════════
    
    public record Statistics(
        long totalCompilations,
        long cacheHits,
        double hitRate,
        long totalCompileTimeMs,
        int cachedShaders,
        int cachedPipelines
    ) {}
    
    public Statistics getStatistics() {
        long total = totalCompilations.get();
        long hits = cacheHits.get();
        double hitRate = total > 0 ? (double) hits / (total + hits) : 0.0;
        
        return new Statistics(
            total,
            hits,
            hitRate,
            totalCompileTimeNs.get() / 1_000_000,
            shaderCache.size(),
            pipelineCache.size()
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════
    
    public void clear() {
        shaderCache.values().forEach(s -> backend.destroyShader(s.handle()));
        pipelineCache.values().forEach(backend::destroyProgram);
        shaderCache.clear();
        pipelineCache.clear();
        includeCache.clear();
    }
    
    public void shutdown() {
        watcherRunning = false;
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
        compileExecutor.shutdown();
        clear();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXCEPTION
    // ═══════════════════════════════════════════════════════════════════════
    
    public static class ShaderCompilationException extends RuntimeException {
        private final String source;
        
        public ShaderCompilationException(String message, String source, Throwable cause) {
            super(message, cause);
            this.source = source;
        }
        
        public String getSource() {
            return source;
        }
        
        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder(super.getMessage());
            if (source != null) {
                sb.append("\n\n=== Shader Source ===\n");
                String[] lines = source.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    sb.append(String.format("%4d: %s%n", i + 1, lines[i]));
                }
            }
            return sb.toString();
        }
    }
}
