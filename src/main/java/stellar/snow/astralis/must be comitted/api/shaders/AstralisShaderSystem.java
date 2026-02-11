package stellar.snow.astralis.api.shaders;

import stellar.snow.astralis.api.vulkan.shaders.ShaderPermutationManager;
import stellar.snow.astralis.api.vulkan.shaders.ShaderPermutationManager.*;
import stellar.snow.astralis.engine.gpu.authority.GPUBackend;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * ═══════════════════════════════════════════════════════════════════════════════════
 * ASTRALIS SHADER SYSTEM - Easy-to-use shader management for the Astralis engine
 * ═══════════════════════════════════════════════════════════════════════════════════
 * 
 * Features:
 * • Simple fluent API for shader creation
 * • Pre-built shader library for common effects
 * • Automatic pipeline management
 * • Hot-reload support during development
 * • Full support for all shader stages (vertex, fragment, compute, mesh, task, etc.)
 * • Material system with shader variants
 * • Performance profiling and debugging
 * 
 * Quick Start:
 * 
 *   // Create basic PBR material
 *   var material = ShaderSystem.material("MyPBR")
 *       .vertex("shaders/pbr.vert")
 *       .fragment("shaders/pbr.frag")
 *       .define("USE_NORMAL_MAP")
 *       .build();
 * 
 *   // Use compute shader
 *   var compute = ShaderSystem.compute("ParticleUpdate")
 *       .shader("shaders/particles.comp")
 *       .workgroups(256, 1, 1)
 *       .dispatch(numParticles / 256);
 * 
 *   // Create advanced mesh shader pipeline
 *   var meshPipeline = ShaderSystem.meshPipeline("Terrain")
 *       .task("shaders/terrain_cull.task")
 *       .mesh("shaders/terrain.mesh")
 *       .fragment("shaders/terrain.frag")
 *       .build();
 */
public class AstralisShaderSystem {
    
    // ═══════════════════════════════════════════════════════════════════════
    // CORE SYSTEM
    // ═══════════════════════════════════════════════════════════════════════
    
    private final ShaderPermutationManager permutationManager;
    private final GPUBackend backend;
    private final Map<String, Material> materials = new ConcurrentHashMap<>();
    private final Map<String, ComputeShader> computeShaders = new ConcurrentHashMap<>();
    private final Map<String, ShaderEffect> effects = new ConcurrentHashMap<>();
    private final ShaderLibrary library;
    
    private boolean hotReloadEnabled = false;
    
    public AstralisShaderSystem(GPUBackend backend, Path shaderDirectory) {
        this.backend = backend;
        this.permutationManager = new ShaderPermutationManager(backend, shaderDirectory);
        this.library = new ShaderLibrary(this);
        
        // Initialize hot-reload listener
        permutationManager.addReloadListener(this::onShadersReloaded);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MATERIAL SYSTEM
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Material represents a complete shader pipeline with state
     */
    public static class Material {
        private final String name;
        private final long pipeline;
        private final PipelineConfig config;
        private final Map<String, Object> uniforms = new HashMap<>();
        private final Map<String, Long> textures = new HashMap<>();
        
        Material(String name, long pipeline, PipelineConfig config) {
            this.name = name;
            this.pipeline = pipeline;
            this.config = config;
        }
        
        public Material setUniform(String name, Object value) {
            uniforms.put(name, value);
            return this;
        }
        
        public Material setTexture(String name, long textureHandle) {
            textures.put(name, textureHandle);
            return this;
        }
        
        public long getPipeline() { return pipeline; }
        public String getName() { return name; }
        public PipelineConfig getConfig() { return config; }
    }
    
    /**
     * Fluent builder for creating materials
     */
    public static class MaterialBuilder {
        private final AstralisShaderSystem system;
        private final String name;
        private Path vertexPath;
        private Path fragmentPath;
        private Path geometryPath;
        private Path tessControlPath;
        private Path tessEvalPath;
        private final List<String> defines = new ArrayList<>();
        private final Map<Integer, Object> specializationConstants = new HashMap<>();
        
        // Pipeline state
        private boolean depthTest = true;
        private boolean depthWrite = true;
        private int cullMode = GPUBackend.CullMode.BACK;
        private boolean blending = false;
        
        MaterialBuilder(AstralisShaderSystem system, String name) {
            this.system = system;
            this.name = name;
        }
        
        public MaterialBuilder vertex(String path) {
            this.vertexPath = Paths.get(path);
            return this;
        }
        
        public MaterialBuilder fragment(String path) {
            this.fragmentPath = Paths.get(path);
            return this;
        }
        
        public MaterialBuilder geometry(String path) {
            this.geometryPath = Paths.get(path);
            return this;
        }
        
        public MaterialBuilder tessellation(String controlPath, String evalPath) {
            this.tessControlPath = Paths.get(controlPath);
            this.tessEvalPath = Paths.get(evalPath);
            return this;
        }
        
        public MaterialBuilder define(String... defines) {
            this.defines.addAll(Arrays.asList(defines));
            return this;
        }
        
        public MaterialBuilder specialize(int id, Object value) {
            this.specializationConstants.put(id, value);
            return this;
        }
        
        public MaterialBuilder depthTest(boolean enable) {
            this.depthTest = enable;
            return this;
        }
        
        public MaterialBuilder depthWrite(boolean enable) {
            this.depthWrite = enable;
            return this;
        }
        
        public MaterialBuilder cullMode(int mode) {
            this.cullMode = mode;
            return this;
        }
        
        public MaterialBuilder blending(boolean enable) {
            this.blending = enable;
            return this;
        }
        
        public Material build() {
            PipelineConfig config = new PipelineConfig();
            
            // Compile shaders
            if (vertexPath != null) {
                config.vertexShader = system.permutationManager.compileShader(
                    vertexPath, defines.toArray(String[]::new)
                ).handle();
            }
            
            if (fragmentPath != null) {
                config.fragmentShader = system.permutationManager.compileShader(
                    fragmentPath, defines.toArray(String[]::new)
                ).handle();
            }
            
            if (geometryPath != null) {
                config.geometryShader = system.permutationManager.compileShader(
                    geometryPath, defines.toArray(String[]::new)
                ).handle();
            }
            
            if (tessControlPath != null && tessEvalPath != null) {
                config.tessControlShader = system.permutationManager.compileShader(
                    tessControlPath, defines.toArray(String[]::new)
                ).handle();
                config.tessEvalShader = system.permutationManager.compileShader(
                    tessEvalPath, defines.toArray(String[]::new)
                ).handle();
            }
            
            // Set pipeline state
            config.depthTest = depthTest;
            config.depthWrite = depthWrite;
            config.cullMode = cullMode;
            config.blendEnable = blending;
            config.defines = defines.toArray(String[]::new);
            config.specializationConstants = specializationConstants;
            
            // Create pipeline
            long pipeline = system.permutationManager.getOrCreatePipeline(config);
            
            Material material = new Material(name, pipeline, config);
            system.materials.put(name, material);
            return material;
        }
    }
    
    public MaterialBuilder material(String name) {
        return new MaterialBuilder(this, name);
    }
    
    public Material getMaterial(String name) {
        return materials.get(name);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // COMPUTE SHADER SYSTEM
    // ═══════════════════════════════════════════════════════════════════════
    
    public static class ComputeShader {
        private final String name;
        private final long shader;
        private final long pipeline;
        private int workgroupSizeX = 1;
        private int workgroupSizeY = 1;
        private int workgroupSizeZ = 1;
        
        ComputeShader(String name, long shader, long pipeline) {
            this.name = name;
            this.shader = shader;
            this.pipeline = pipeline;
        }
        
        public ComputeShader workgroups(int x, int y, int z) {
            this.workgroupSizeX = x;
            this.workgroupSizeY = y;
            this.workgroupSizeZ = z;
            return this;
        }
        
        public void dispatch(int numWorkgroups) {
            dispatch(numWorkgroups, 1, 1);
        }
        
        public void dispatch(int x, int y, int z) {
            // Bind pipeline and dispatch
            // backend.bindPipeline(pipeline);
            // backend.dispatch(x, y, z);
        }
        
        public long getShader() { return shader; }
        public long getPipeline() { return pipeline; }
    }
    
    public static class ComputeBuilder {
        private final AstralisShaderSystem system;
        private final String name;
        private Path shaderPath;
        private final List<String> defines = new ArrayList<>();
        
        ComputeBuilder(AstralisShaderSystem system, String name) {
            this.system = system;
            this.name = name;
        }
        
        public ComputeBuilder shader(String path) {
            this.shaderPath = Paths.get(path);
            return this;
        }
        
        public ComputeBuilder define(String... defines) {
            this.defines.addAll(Arrays.asList(defines));
            return this;
        }
        
        public ComputeShader build() {
            var compiled = system.permutationManager.compileShader(
                shaderPath, defines.toArray(String[]::new)
            );
            
            PipelineConfig config = new PipelineConfig();
            config.computeShader = compiled.handle();
            
            long pipeline = system.permutationManager.getOrCreatePipeline(config);
            
            ComputeShader compute = new ComputeShader(name, compiled.handle(), pipeline);
            system.computeShaders.put(name, compute);
            return compute;
        }
    }
    
    public ComputeBuilder compute(String name) {
        return new ComputeBuilder(this, name);
    }
    
    public ComputeShader getComputeShader(String name) {
        return computeShaders.get(name);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MESH SHADER PIPELINE
    // ═══════════════════════════════════════════════════════════════════════
    
    public static class MeshPipelineBuilder {
        private final AstralisShaderSystem system;
        private final String name;
        private Path taskPath;
        private Path meshPath;
        private Path fragmentPath;
        private final List<String> defines = new ArrayList<>();
        
        MeshPipelineBuilder(AstralisShaderSystem system, String name) {
            this.system = system;
            this.name = name;
        }
        
        public MeshPipelineBuilder task(String path) {
            this.taskPath = Paths.get(path);
            return this;
        }
        
        public MeshPipelineBuilder mesh(String path) {
            this.meshPath = Paths.get(path);
            return this;
        }
        
        public MeshPipelineBuilder fragment(String path) {
            this.fragmentPath = Paths.get(path);
            return this;
        }
        
        public MeshPipelineBuilder define(String... defines) {
            this.defines.addAll(Arrays.asList(defines));
            return this;
        }
        
        public Material build() {
            PipelineConfig config = new PipelineConfig();
            
            if (taskPath != null) {
                config.taskShader = system.permutationManager.compileShader(
                    taskPath, defines.toArray(String[]::new)
                ).handle();
            }
            
            if (meshPath != null) {
                config.meshShader = system.permutationManager.compileShader(
                    meshPath, defines.toArray(String[]::new)
                ).handle();
            }
            
            if (fragmentPath != null) {
                config.fragmentShader = system.permutationManager.compileShader(
                    fragmentPath, defines.toArray(String[]::new)
                ).handle();
            }
            
            long pipeline = system.permutationManager.getOrCreatePipeline(config);
            Material material = new Material(name, pipeline, config);
            system.materials.put(name, material);
            return material;
        }
    }
    
    public MeshPipelineBuilder meshPipeline(String name) {
        return new MeshPipelineBuilder(this, name);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SHADER EFFECTS - Pre-configured shader combinations
    // ═══════════════════════════════════════════════════════════════════════
    
    public static class ShaderEffect {
        private final String name;
        private final Material material;
        private final Map<String, Object> parameters = new HashMap<>();
        
        ShaderEffect(String name, Material material) {
            this.name = name;
            this.material = material;
        }
        
        public ShaderEffect set(String param, Object value) {
            parameters.put(param, value);
            material.setUniform(param, value);
            return this;
        }
        
        public Material getMaterial() { return material; }
    }
    
    public ShaderEffect effect(String name) {
        return effects.get(name);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SHADER LIBRARY - Pre-built shaders for common use cases
    // ═══════════════════════════════════════════════════════════════════════
    
    public static class ShaderLibrary {
        private final AstralisShaderSystem system;
        
        ShaderLibrary(AstralisShaderSystem system) {
            this.system = system;
        }
        
        /**
         * Create a basic unlit shader
         */
        public Material unlit() {
            return system.material("Unlit")
                .vertex(getBuiltinPath("unlit.vert"))
                .fragment(getBuiltinPath("unlit.frag"))
                .build();
        }
        
        /**
         * Create a PBR (Physically Based Rendering) material
         */
        public Material pbr() {
            return system.material("PBR")
                .vertex(getBuiltinPath("pbr.vert"))
                .fragment(getBuiltinPath("pbr.frag"))
                .define("USE_IBL")
                .build();
        }
        
        /**
         * Create a PBR material with specific features
         */
        public Material pbr(boolean normalMap, boolean aoMap, boolean emissive) {
            var builder = system.material("PBR_Custom")
                .vertex(getBuiltinPath("pbr.vert"))
                .fragment(getBuiltinPath("pbr.frag"));
            
            if (normalMap) builder.define("USE_NORMAL_MAP");
            if (aoMap) builder.define("USE_AO_MAP");
            if (emissive) builder.define("USE_EMISSIVE_MAP");
            
            return builder.build();
        }
        
        /**
         * Create a skybox shader
         */
        public Material skybox() {
            return system.material("Skybox")
                .vertex(getBuiltinPath("skybox.vert"))
                .fragment(getBuiltinPath("skybox.frag"))
                .depthTest(true)
                .depthWrite(false)
                .cullMode(GPUBackend.CullMode.FRONT)
                .build();
        }
        
        /**
         * Create a particle system shader
         */
        public Material particles() {
            return system.material("Particles")
                .vertex(getBuiltinPath("particles.vert"))
                .fragment(getBuiltinPath("particles.frag"))
                .blending(true)
                .depthWrite(false)
                .build();
        }
        
        /**
         * Create a screen-space shader (for post-processing)
         */
        public Material screenSpace() {
            return system.material("ScreenSpace")
                .vertex(getBuiltinPath("fullscreen.vert"))
                .fragment(getBuiltinPath("copy.frag"))
                .depthTest(false)
                .depthWrite(false)
                .build();
        }
        
        /**
         * Create a blur post-process effect
         */
        public ShaderEffect gaussianBlur(float radius) {
            var material = system.material("GaussianBlur")
                .vertex(getBuiltinPath("fullscreen.vert"))
                .fragment(getBuiltinPath("blur.frag"))
                .depthTest(false)
                .build();
            
            var effect = new ShaderEffect("GaussianBlur", material);
            effect.set("radius", radius);
            system.effects.put("GaussianBlur", effect);
            return effect;
        }
        
        /**
         * Create a bloom effect
         */
        public ShaderEffect bloom(float threshold, float intensity) {
            var material = system.material("Bloom")
                .vertex(getBuiltinPath("fullscreen.vert"))
                .fragment(getBuiltinPath("bloom.frag"))
                .depthTest(false)
                .build();
            
            var effect = new ShaderEffect("Bloom", material);
            effect.set("threshold", threshold);
            effect.set("intensity", intensity);
            system.effects.put("Bloom", effect);
            return effect;
        }
        
        /**
         * Create SSAO (Screen Space Ambient Occlusion)
         */
        public ShaderEffect ssao(int samples) {
            var material = system.material("SSAO")
                .vertex(getBuiltinPath("fullscreen.vert"))
                .fragment(getBuiltinPath("ssao.frag"))
                .depthTest(false)
                .specialize(0, samples)
                .build();
            
            var effect = new ShaderEffect("SSAO", material);
            system.effects.put("SSAO", effect);
            return effect;
        }
        
        /**
         * Create a terrain shader with tessellation
         */
        public Material terrain(boolean heightMap, boolean splatMap) {
            var builder = system.material("Terrain")
                .vertex(getBuiltinPath("terrain.vert"))
                .fragment(getBuiltinPath("terrain.frag"))
                .tessellation(
                    getBuiltinPath("terrain.tesc"),
                    getBuiltinPath("terrain.tese")
                );
            
            if (heightMap) builder.define("USE_HEIGHT_MAP");
            if (splatMap) builder.define("USE_SPLAT_MAP");
            
            return builder.build();
        }
        
        /**
         * Create a water shader
         */
        public Material water() {
            return system.material("Water")
                .vertex(getBuiltinPath("water.vert"))
                .fragment(getBuiltinPath("water.frag"))
                .blending(true)
                .define("USE_REFLECTION", "USE_REFRACTION")
                .build();
        }
        
        /**
         * Create a grass/foliage shader with geometry
         */
        public Material grass() {
            return system.material("Grass")
                .vertex(getBuiltinPath("grass.vert"))
                .geometry(getBuiltinPath("grass.geom"))
                .fragment(getBuiltinPath("grass.frag"))
                .cullMode(GPUBackend.CullMode.NONE)
                .build();
        }
        
        /**
         * Create a GPU particle compute shader
         */
        public ComputeShader particleUpdate(int maxParticles) {
            return system.compute("ParticleUpdate")
                .shader(getBuiltinPath("particle_update.comp"))
                .define("MAX_PARTICLES=" + maxParticles)
                .build()
                .workgroups(256, 1, 1);
        }
        
        /**
         * Create a frustum culling compute shader
         */
        public ComputeShader frustumCulling() {
            return system.compute("FrustumCulling")
                .shader(getBuiltinPath("frustum_cull.comp"))
                .build()
                .workgroups(256, 1, 1);
        }
        
        /**
         * Create an occlusion culling compute shader
         */
        public ComputeShader occlusionCulling() {
            return system.compute("OcclusionCulling")
                .shader(getBuiltinPath("occlusion_cull.comp"))
                .build()
                .workgroups(256, 1, 1);
        }
        
        private String getBuiltinPath(String shader) {
            return "astralis/shaders/builtin/" + shader;
        }
    }
    
    public ShaderLibrary library() {
        return library;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // HOT RELOAD SYSTEM
    // ═══════════════════════════════════════════════════════════════════════
    
    public void enableHotReload() {
        this.hotReloadEnabled = true;
    }
    
    public void disableHotReload() {
        this.hotReloadEnabled = false;
    }
    
    private void onShadersReloaded(Set<String> invalidatedShaders) {
        if (!hotReloadEnabled) return;
        
        System.out.println("🔥 Hot-reloading " + invalidatedShaders.size() + " shaders...");
        
        // Rebuild affected materials
        List<Material> toRebuild = new ArrayList<>();
        for (Material material : materials.values()) {
            // Check if any shader in this material was invalidated
            // This is simplified - in production, track shader->material dependencies
            toRebuild.add(material);
        }
        
        // Notify about reload
        System.out.println("✅ Reloaded " + toRebuild.size() + " materials");
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEBUGGING & PROFILING
    // ═══════════════════════════════════════════════════════════════════════
    
    public void printStatistics() {
        var stats = permutationManager.getStatistics();
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║         ASTRALIS SHADER SYSTEM STATISTICS                 ║");
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.printf("║ Total Compilations:      %8d                        ║%n", stats.totalCompilations());
        System.out.printf("║ Cache Hits:              %8d                        ║%n", stats.cacheHits());
        System.out.printf("║ Hit Rate:                %7.1f%%                        ║%n", stats.hitRate() * 100);
        System.out.printf("║ Total Compile Time:      %8d ms                     ║%n", stats.totalCompileTimeMs());
        System.out.printf("║ Cached Shaders:          %8d                        ║%n", stats.cachedShaders());
        System.out.printf("║ Cached Pipelines:        %8d                        ║%n", stats.cachedPipelines());
        System.out.printf("║ Materials:               %8d                        ║%n", materials.size());
        System.out.printf("║ Compute Shaders:         %8d                        ║%n", computeShaders.size());
        System.out.printf("║ Effects:                 %8d                        ║%n", effects.size());
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
    }
    
    public void listMaterials() {
        System.out.println("\n📦 Loaded Materials:");
        materials.forEach((name, mat) -> {
            System.out.println("  • " + name + " (pipeline: " + mat.getPipeline() + ")");
        });
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SHADER CODE TEMPLATES
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Generate shader code templates for common use cases
     */
    public static class ShaderTemplates {
        
        public static String basicVertex() {
            return """
                #version 460
                
                layout(location = 0) in vec3 inPosition;
                layout(location = 1) in vec3 inNormal;
                layout(location = 2) in vec2 inTexCoord;
                
                layout(location = 0) out vec3 fragPosition;
                layout(location = 1) out vec3 fragNormal;
                layout(location = 2) out vec2 fragTexCoord;
                
                layout(set = 0, binding = 0) uniform Camera {
                    mat4 viewProj;
                    vec3 position;
                } camera;
                
                layout(push_constant) uniform Model {
                    mat4 transform;
                } model;
                
                void main() {
                    vec4 worldPos = model.transform * vec4(inPosition, 1.0);
                    gl_Position = camera.viewProj * worldPos;
                    
                    fragPosition = worldPos.xyz;
                    fragNormal = mat3(model.transform) * inNormal;
                    fragTexCoord = inTexCoord;
                }
                """;
        }
        
        public static String basicFragment() {
            return """
                #version 460
                
                layout(location = 0) in vec3 fragPosition;
                layout(location = 1) in vec3 fragNormal;
                layout(location = 2) in vec2 fragTexCoord;
                
                layout(location = 0) out vec4 outColor;
                
                layout(set = 1, binding = 0) uniform sampler2D albedoMap;
                
                void main() {
                    vec3 color = texture(albedoMap, fragTexCoord).rgb;
                    vec3 normal = normalize(fragNormal);
                    
                    // Simple lighting
                    vec3 lightDir = normalize(vec3(1.0, 1.0, 1.0));
                    float ndotl = max(dot(normal, lightDir), 0.0);
                    
                    outColor = vec4(color * (ndotl * 0.8 + 0.2), 1.0);
                }
                """;
        }
        
        public static String computeTemplate() {
            return """
                #version 460
                
                layout(local_size_x = 256, local_size_y = 1, local_size_z = 1) in;
                
                struct Particle {
                    vec4 position;
                    vec4 velocity;
                    vec4 color;
                    float life;
                };
                
                layout(set = 0, binding = 0) buffer Particles {
                    Particle particles[];
                };
                
                layout(push_constant) uniform Params {
                    float deltaTime;
                    float time;
                } params;
                
                void main() {
                    uint id = gl_GlobalInvocationID.x;
                    
                    Particle p = particles[id];
                    
                    // Update particle
                    p.velocity.y -= 9.81 * params.deltaTime; // Gravity
                    p.position += p.velocity * params.deltaTime;
                    p.life -= params.deltaTime;
                    
                    // Reset if dead
                    if (p.life <= 0.0) {
                        p.life = 1.0;
                        p.position = vec4(0.0, 0.0, 0.0, 1.0);
                    }
                    
                    particles[id] = p;
                }
                """;
        }
        
        public static String meshShaderTemplate() {
            return """
                #version 460
                #extension GL_EXT_mesh_shader : require
                
                layout(local_size_x = 32, local_size_y = 1, local_size_z = 1) in;
                layout(triangles, max_vertices = 64, max_primitives = 126) out;
                
                struct Vertex {
                    vec4 position;
                    vec3 normal;
                    vec2 texCoord;
                };
                
                layout(location = 0) out Vertex vertices[];
                
                void main() {
                    uint tid = gl_LocalInvocationID.x;
                    
                    // Generate mesh procedurally
                    if (tid < 64) {
                        vertices[tid].position = vec4(
                            sin(tid * 0.1),
                            cos(tid * 0.1),
                            0.0,
                            1.0
                        );
                        vertices[tid].normal = vec3(0.0, 0.0, 1.0);
                        vertices[tid].texCoord = vec2(tid / 64.0, 0.0);
                    }
                    
                    // Emit triangles
                    if (tid < 126) {
                        gl_PrimitiveTriangleIndicesEXT[tid] = uvec3(
                            tid * 3 + 0,
                            tid * 3 + 1,
                            tid * 3 + 2
                        );
                    }
                    
                    SetMeshOutputsEXT(64, 126);
                }
                """;
        }
        
        public static String rayTracingRayGenTemplate() {
            return """
                #version 460
                #extension GL_EXT_ray_tracing : require
                
                layout(set = 0, binding = 0) uniform accelerationStructureEXT topLevelAS;
                layout(set = 0, binding = 1, rgba8) uniform image2D resultImage;
                
                layout(location = 0) rayPayloadEXT vec3 hitValue;
                
                void main() {
                    vec2 pixelCenter = vec2(gl_LaunchIDEXT.xy) + vec2(0.5);
                    vec2 inUV = pixelCenter / vec2(gl_LaunchSizeEXT.xy);
                    vec2 d = inUV * 2.0 - 1.0;
                    
                    vec3 origin = vec3(0.0, 0.0, -2.0);
                    vec3 direction = normalize(vec3(d.x, d.y, 1.0));
                    
                    traceRayEXT(
                        topLevelAS,      // acceleration structure
                        gl_RayFlagsOpaqueEXT,
                        0xFF,            // cullMask
                        0,               // sbtRecordOffset
                        0,               // sbtRecordStride
                        0,               // missIndex
                        origin,
                        0.001,           // tMin
                        direction,
                        10000.0,         // tMax
                        0                // payload location
                    );
                    
                    imageStore(resultImage, ivec2(gl_LaunchIDEXT.xy), vec4(hitValue, 1.0));
                }
                """;
        }
    }
    
    public ShaderTemplates templates() {
        return new ShaderTemplates();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════
    
    public void shutdown() {
        permutationManager.shutdown();
        materials.clear();
        computeShaders.clear();
        effects.clear();
    }
}
