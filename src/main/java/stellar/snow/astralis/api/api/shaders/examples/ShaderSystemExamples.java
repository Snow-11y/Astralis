package stellar.snow.astralis.api.shaders.examples;

import stellar.snow.astralis.api.shaders.AstralisShaderSystem;
import stellar.snow.astralis.api.shaders.AstralisShaderSystem.*;
import stellar.snow.astralis.engine.gpu.authority.GPUBackend;

import java.nio.file.Paths;

/**
 * ═══════════════════════════════════════════════════════════════════════════════════
 * ASTRALIS SHADER SYSTEM - USAGE EXAMPLES
 * ═══════════════════════════════════════════════════════════════════════════════════
 * 
 * This file demonstrates how to use the Astralis Shader System for common
 * rendering tasks. The system supports all modern GPU features across multiple
 * backends (Vulkan, OpenGL, Metal, DirectX 12).
 */
public class ShaderSystemExamples {
    
    private AstralisShaderSystem shaders;
    private GPUBackend gpu;
    
    public void initialize(GPUBackend gpu) {
        this.gpu = gpu;
        this.shaders = new AstralisShaderSystem(gpu, Paths.get("shaders"));
        
        // Enable hot-reload for development
        shaders.enableHotReload();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 1: Basic Material Creation
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example1_BasicMaterial() {
        // Create a simple material with vertex and fragment shaders
        Material material = shaders.material("BasicMaterial")
            .vertex("shaders/basic.vert")
            .fragment("shaders/basic.frag")
            .build();
        
        // Set uniform values
        material.setUniform("color", new float[]{1.0f, 0.0f, 0.0f, 1.0f});
        material.setTexture("diffuse", getTextureHandle());
        
        // Use the material
        long pipeline = material.getPipeline();
        // gpu.bindPipeline(pipeline);
        // ... draw calls ...
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 2: PBR Material with Options
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example2_PBRMaterial() {
        // Method 1: Use built-in PBR from library
        Material pbrMaterial = shaders.library().pbr();
        
        // Method 2: Create custom PBR with specific features
        Material customPBR = shaders.library()
            .pbr(
                true,  // Use normal mapping
                true,  // Use ambient occlusion
                false  // No emissive
            );
        
        // Method 3: Create PBR from scratch with defines
        Material advancedPBR = shaders.material("AdvancedPBR")
            .vertex("shaders/pbr.vert")
            .fragment("shaders/pbr.frag")
            .define("USE_NORMAL_MAP")
            .define("USE_METALLIC_ROUGHNESS")
            .define("USE_AO_MAP")
            .define("USE_IBL")
            .define("USE_SHADOW_MAPS")
            .build();
        
        // Set PBR textures
        advancedPBR.setTexture("albedoMap", getTextureHandle());
        advancedPBR.setTexture("normalMap", getTextureHandle());
        advancedPBR.setTexture("metallicRoughnessMap", getTextureHandle());
        advancedPBR.setTexture("aoMap", getTextureHandle());
        
        // Set PBR parameters
        advancedPBR.setUniform("metallic", 0.8f);
        advancedPBR.setUniform("roughness", 0.3f);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 3: Compute Shaders for GPU Particles
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example3_GPUParticles() {
        int maxParticles = 1_000_000;
        
        // Create particle update compute shader
        ComputeShader particleUpdate = shaders.library()
            .particleUpdate(maxParticles);
        
        // Or create custom compute shader
        ComputeShader customCompute = shaders.compute("CustomParticles")
            .shader("shaders/particles.comp")
            .define("MAX_PARTICLES=" + maxParticles)
            .define("USE_GRAVITY")
            .define("USE_COLLISION")
            .build();
        
        // Set workgroup size and dispatch
        customCompute
            .workgroups(256, 1, 1)
            .dispatch(maxParticles / 256);
        
        // Create rendering shader for particles
        Material particleMaterial = shaders.library().particles();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 4: Post-Processing Effects
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example4_PostProcessing() {
        // Bloom effect
        ShaderEffect bloom = shaders.library().bloom(
            0.8f,  // threshold
            1.5f   // intensity
        );
        bloom.set("threshold", 0.9f);
        bloom.set("intensity", 2.0f);
        
        // Gaussian blur
        ShaderEffect blur = shaders.library().gaussianBlur(5.0f);
        blur.set("radius", 10.0f);
        
        // SSAO (Screen Space Ambient Occlusion)
        ShaderEffect ssao = shaders.library().ssao(32); // 32 samples
        
        // Custom post-process effect
        Material customFX = shaders.material("ChromaticAberration")
            .vertex("shaders/fullscreen.vert")
            .fragment("shaders/chromatic_aberration.frag")
            .depthTest(false)
            .depthWrite(false)
            .build();
        
        customFX.setUniform("aberrationStrength", 0.01f);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 5: Terrain with Tessellation
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example5_Terrain() {
        // Use built-in terrain shader
        Material terrain = shaders.library().terrain(
            true,  // Use height map
            true   // Use splat map
        );
        
        // Or create custom terrain with tessellation
        Material customTerrain = shaders.material("CustomTerrain")
            .vertex("shaders/terrain.vert")
            .tessellation(
                "shaders/terrain.tesc",  // Control shader
                "shaders/terrain.tese"   // Evaluation shader
            )
            .fragment("shaders/terrain.frag")
            .define("USE_HEIGHT_MAP")
            .define("USE_NORMAL_MAP")
            .define("USE_SPLAT_MAP")
            .define("NUM_LAYERS=4")
            .build();
        
        // Set terrain textures
        customTerrain.setTexture("heightMap", getTextureHandle());
        customTerrain.setTexture("splatMap", getTextureHandle());
        customTerrain.setTexture("layerTextures", getTextureArrayHandle());
        
        // Set tessellation parameters
        customTerrain.setUniform("tessellationLevel", 32);
        customTerrain.setUniform("heightScale", 100.0f);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 6: Water Rendering
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example6_Water() {
        // Built-in water shader
        Material water = shaders.library().water();
        
        water.setTexture("reflectionMap", getTextureHandle());
        water.setTexture("refractionMap", getTextureHandle());
        water.setTexture("normalMap", getTextureHandle());
        water.setTexture("foamMap", getTextureHandle());
        
        water.setUniform("time", getCurrentTime());
        water.setUniform("waveSpeed", 0.05f);
        water.setUniform("waveStrength", 0.02f);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 7: Mesh Shaders (Modern GPU Feature)
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example7_MeshShaders() {
        // Create mesh shader pipeline for LOD terrain
        Material meshTerrain = shaders.meshPipeline("MeshTerrain")
            .task("shaders/terrain_cull.task")    // Optional culling
            .mesh("shaders/terrain.mesh")         // Mesh generation
            .fragment("shaders/terrain.frag")     // Fragment shading
            .define("USE_LOD")
            .build();
        
        // Create mesh shader for procedural grass
        Material meshGrass = shaders.meshPipeline("MeshGrass")
            .mesh("shaders/grass.mesh")
            .fragment("shaders/grass.frag")
            .cullMode(GPUBackend.CullMode.NONE)
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 8: Grass/Foliage with Geometry Shaders
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example8_Grass() {
        // Built-in grass shader
        Material grass = shaders.library().grass();
        
        // Custom grass with wind animation
        Material customGrass = shaders.material("WindGrass")
            .vertex("shaders/grass.vert")
            .geometry("shaders/grass.geom")      // Generates grass blades
            .fragment("shaders/grass.frag")
            .cullMode(GPUBackend.CullMode.NONE)
            .define("USE_WIND")
            .define("BLADE_SEGMENTS=3")
            .build();
        
        customGrass.setUniform("windStrength", 1.0f);
        customGrass.setUniform("windDirection", new float[]{1.0f, 0.0f, 0.3f});
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 9: Skybox
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example9_Skybox() {
        // Create skybox material
        Material skybox = shaders.library().skybox();
        
        skybox.setTexture("cubemap", getCubemapHandle());
        
        // Or create custom skybox with atmospheric scattering
        Material atmosphericSky = shaders.material("AtmosphericSky")
            .vertex("shaders/sky.vert")
            .fragment("shaders/atmospheric_scattering.frag")
            .depthTest(true)
            .depthWrite(false)
            .cullMode(GPUBackend.CullMode.FRONT)
            .build();
        
        atmosphericSky.setUniform("sunDirection", new float[]{0.5f, 0.8f, 0.3f});
        atmosphericSky.setUniform("rayleighCoefficient", 5.5e-6f);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 10: Culling Systems
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example10_Culling() {
        // Frustum culling compute shader
        ComputeShader frustumCull = shaders.library().frustumCulling();
        
        // Occlusion culling
        ComputeShader occlusionCull = shaders.library().occlusionCulling();
        
        // Custom hierarchical Z-buffer culling
        ComputeShader hiZCull = shaders.compute("HiZCulling")
            .shader("shaders/hiz_culling.comp")
            .define("TILE_SIZE=16")
            .build()
            .workgroups(256, 1, 1);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 11: Material Variants with Specialization Constants
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example11_MaterialVariants() {
        // Create multiple variants with different quality settings
        
        // Ultra quality
        Material ultraPBR = shaders.material("PBR_Ultra")
            .vertex("shaders/pbr.vert")
            .fragment("shaders/pbr.frag")
            .define("USE_ALL_MAPS")
            .specialize(0, 128)  // Shadow map resolution
            .specialize(1, 64)   // IBL mip levels
            .build();
        
        // High quality
        Material highPBR = shaders.material("PBR_High")
            .vertex("shaders/pbr.vert")
            .fragment("shaders/pbr.frag")
            .define("USE_NORMAL_MAP", "USE_ROUGHNESS_MAP")
            .specialize(0, 64)
            .specialize(1, 32)
            .build();
        
        // Low quality (mobile)
        Material lowPBR = shaders.material("PBR_Low")
            .vertex("shaders/pbr.vert")
            .fragment("shaders/pbr.frag")
            .specialize(0, 32)
            .specialize(1, 16)
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 12: Shadow Mapping
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example12_Shadows() {
        // Shadow map generation pass
        Material shadowGen = shaders.material("ShadowGen")
            .vertex("shaders/shadow.vert")
            .fragment("shaders/shadow.frag")  // Optional for alpha test
            .depthTest(true)
            .depthWrite(true)
            .cullMode(GPUBackend.CullMode.FRONT)  // Prevent peter panning
            .build();
        
        // Main rendering with shadows
        Material litWithShadows = shaders.material("LitWithShadows")
            .vertex("shaders/lit.vert")
            .fragment("shaders/lit_shadows.frag")
            .define("USE_PCF_FILTERING")
            .define("SHADOW_MAP_SIZE=2048")
            .build();
        
        litWithShadows.setTexture("shadowMap", getShadowMapHandle());
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 13: Custom Pipeline State
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example13_CustomPipelineState() {
        // Alpha blended particles with custom blend mode
        Material alphaParticles = shaders.material("AlphaParticles")
            .vertex("shaders/particles.vert")
            .fragment("shaders/particles.frag")
            .depthTest(true)
            .depthWrite(false)
            .blending(true)
            .cullMode(GPUBackend.CullMode.NONE)
            .build();
        
        // Wireframe rendering
        Material wireframe = shaders.material("Wireframe")
            .vertex("shaders/basic.vert")
            .fragment("shaders/solid_color.frag")
            .cullMode(GPUBackend.CullMode.NONE)
            .build();
        // Note: Wireframe mode is set via backend.setPolygonMode()
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 14: Using Shader Templates
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example14_ShaderTemplates() {
        // Get shader code templates
        String vertexCode = ShaderTemplates.basicVertex();
        String fragmentCode = ShaderTemplates.basicFragment();
        String computeCode = ShaderTemplates.computeTemplate();
        String meshCode = ShaderTemplates.meshShaderTemplate();
        String rayGenCode = ShaderTemplates.rayTracingRayGenTemplate();
        
        // Write templates to files and use them
        writeShaderFile("shaders/generated/basic.vert", vertexCode);
        writeShaderFile("shaders/generated/basic.frag", fragmentCode);
        
        Material generatedMaterial = shaders.material("Generated")
            .vertex("shaders/generated/basic.vert")
            .fragment("shaders/generated/basic.frag")
            .build();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 15: Complete Rendering Pipeline
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example15_CompleteRenderingPipeline() {
        // 1. Shadow pass
        Material shadowMaterial = shaders.getMaterial("ShadowGen");
        // renderShadows(shadowMaterial);
        
        // 2. Main geometry pass
        Material pbrMaterial = shaders.getMaterial("PBR_Ultra");
        // renderGeometry(pbrMaterial);
        
        // 3. Skybox
        Material skybox = shaders.getMaterial("Skybox");
        // renderSkybox(skybox);
        
        // 4. Transparent objects
        Material water = shaders.getMaterial("Water");
        Material particles = shaders.getMaterial("AlphaParticles");
        // renderTransparent(water, particles);
        
        // 5. Post-processing
        ShaderEffect ssao = shaders.effect("SSAO");
        ShaderEffect bloom = shaders.effect("Bloom");
        ShaderEffect blur = shaders.effect("GaussianBlur");
        
        // applyPostFX(ssao);
        // applyPostFX(bloom);
        // applyPostFX(blur);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 16: Hot Reload During Development
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example16_HotReload() {
        // Hot reload is automatically enabled in initialize()
        // When you edit shader files, they'll automatically recompile
        
        // You can also manually trigger recompilation
        shaders.enableHotReload();
        
        // Print statistics to see cache performance
        shaders.printStatistics();
        
        // List all loaded materials
        shaders.listMaterials();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EXAMPLE 17: Ray Tracing (if supported)
    // ═══════════════════════════════════════════════════════════════════════
    
    public void example17_RayTracing() {
        // Note: Ray tracing support depends on GPU backend
        // Vulkan supports ray tracing on RTX and RDNA2+ cards
        
        // Create ray tracing pipeline would go through
        // the ShaderPermutationManager with ShaderStage.RAY_GEN, etc.
        
        // Example of what the API might look like:
        /*
        Material rayTracedMaterial = shaders.material("RayTraced")
            .rayGen("shaders/raytrace.rgen")
            .closestHit("shaders/raytrace.rchit")
            .miss("shaders/raytrace.rmiss")
            .build();
        */
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════
    
    private long getTextureHandle() {
        // Return actual texture handle from your texture manager
        return 0L;
    }
    
    private long getTextureArrayHandle() {
        return 0L;
    }
    
    private long getCubemapHandle() {
        return 0L;
    }
    
    private long getShadowMapHandle() {
        return 0L;
    }
    
    private float getCurrentTime() {
        return System.nanoTime() / 1_000_000_000.0f;
    }
    
    private void writeShaderFile(String path, String content) {
        try {
            java.nio.file.Files.writeString(
                java.nio.file.Paths.get(path),
                content
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════
    
    public void cleanup() {
        shaders.shutdown();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MAIN - Run Examples
    // ═══════════════════════════════════════════════════════════════════════
    
    public static void main(String[] args) {
        // Initialize GPU backend (Vulkan, OpenGL, etc.)
        GPUBackend gpu = null; // Initialize your backend here
        
        ShaderSystemExamples examples = new ShaderSystemExamples();
        examples.initialize(gpu);
        
        // Run examples
        System.out.println("🎨 Running Astralis Shader System Examples...\n");
        
        examples.example1_BasicMaterial();
        examples.example2_PBRMaterial();
        examples.example3_GPUParticles();
        examples.example4_PostProcessing();
        examples.example5_Terrain();
        examples.example6_Water();
        examples.example7_MeshShaders();
        examples.example8_Grass();
        examples.example9_Skybox();
        examples.example10_Culling();
        examples.example11_MaterialVariants();
        examples.example12_Shadows();
        examples.example13_CustomPipelineState();
        examples.example14_ShaderTemplates();
        examples.example15_CompleteRenderingPipeline();
        examples.example16_HotReload();
        
        System.out.println("\n✅ All examples completed!\n");
        
        // Print statistics
        examples.shaders.printStatistics();
        examples.shaders.listMaterials();
        
        // Cleanup
        examples.cleanup();
    }
}
