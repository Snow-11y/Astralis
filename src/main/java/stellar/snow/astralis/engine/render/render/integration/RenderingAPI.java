package stellar.snow.astralis.engine.render.integration;

import stellar.snow.astralis.engine.gpu.compute.CullingManager;
import stellar.snow.astralis.engine.gpu.compute.IndirectDrawManager;
import stellar.snow.astralis.engine.render.gpudriven.CullingSystem;
import stellar.snow.astralis.engine.render.gpudriven.IndirectDrawBridge;
import stellar.snow.astralis.engine.render.debug.DeveloperDebugSystem;
import stellar.snow.astralis.engine.render.stability.StabilityValidator;

import net.minecraft.world.World;
import net.minecraft.entity.Entity;

import java.util.*;

/**
 * Modder-Friendly Rendering API
 * 
 * CLEAN ABSTRACTION LAYER for extending the rendering engine:
 * - Simple, documented API surface
 * - Reasonable defaults
 * - Clear extension points
 * - No VarHandles or MemorySegments in public API
 * - Comprehensive examples
 * 
 * This is what Kirino does well - being accessible to mid-level developers.
 * The complex systems (Arena, DAG, etc) are INTERNAL IMPLEMENTATION.
 * The PUBLIC API is simple and safe.
 */
public final class RenderingAPI {
    
    private final CullingSystem cullingSystem;
    private final IndirectDrawBridge drawBridge;
    private final DeveloperDebugSystem debugSystem;
    private final StabilityValidator validator;
    
    // Extension points
    private final List<RenderingExtension> extensions = new ArrayList<>();
    
    /**
     * Create rendering API with default configuration.
     * This is the simple entry point for modders.
     */
    public RenderingAPI() {
        this(new Configuration());
    }
    
    /**
     * Create rendering API with custom configuration.
     */
    public RenderingAPI(Configuration config) {
        this.cullingSystem = new CullingSystem();
        this.drawBridge = new IndirectDrawBridge();
        this.debugSystem = new DeveloperDebugSystem(cullingSystem, drawBridge);
        this.validator = new StabilityValidator(config.validationLevel);
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // SIMPLE RENDERING API
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * Render a frame.
     * 
     * This handles all the complex stuff automatically:
     * - Entity culling
     * - GPU-driven rendering
     * - Debug visualization
     * - Validation
     * 
     * Example:
     * ```java
     * api.renderFrame(world, entities, partialTicks);
     * ```
     */
    public void renderFrame(World world, List<Entity> entities, float partialTicks) {
        validator.checkNotNull(world, "world");
        validator.checkNotNull(entities, "entities");
        
        // Begin frame
        drawBridge.beginFrame();
        debugSystem.getProfiler().startFrame();
        
        // Call extensions (pre-render)
        for (RenderingExtension ext : extensions) {
            if (ext.isEnabled()) {
                ext.onPreRender(world, entities, partialTicks);
            }
        }
        
        // Perform culling
        var cullResult = cullingSystem.cullEntities(world, entities);
        
        // Setup rendering
        setupRendering(cullResult);
        
        // Execute GPU rendering
        executeGPURendering();
        
        // Call extensions (post-render)
        for (RenderingExtension ext : extensions) {
            if (ext.isEnabled()) {
                ext.onPostRender();
            }
        }
        
        // End frame
        drawBridge.endFrame();
        debugSystem.getProfiler().endFrame();
        debugSystem.update(partialTicks);
    }
    
    /**
     * Setup rendering state from culling results.
     */
    private void setupRendering(CullingSystem.CullingResult cullResult) {
        // Add full detail entities
        for (Entity entity : cullResult.fullDetail) {
            addEntityInstance(entity, DetailLevel.FULL);
        }
        
        // Add reduced detail entities
        for (Entity entity : cullResult.reducedDetail) {
            addEntityInstance(entity, DetailLevel.REDUCED);
        }
        
        // Add minimal detail entities
        for (Entity entity : cullResult.minimal) {
            addEntityInstance(entity, DetailLevel.MINIMAL);
        }
    }
    
    /**
     * Add entity instance with detail level.
     */
    private void addEntityInstance(Entity entity, DetailLevel detailLevel) {
        // Transform calculation (simplified)
        float[] transform = new float[16];
        // ... calculate transform matrix ...
        
        int flags = 0;
        if (detailLevel == DetailLevel.FULL) {
            flags |= IndirectDrawBridge.INSTANCE_FLAG_ENABLED;
            flags |= IndirectDrawBridge.INSTANCE_FLAG_CAST_SHADOW;
        }
        
        drawBridge.addInstance(
            0,  // mesh ID
            transform,
            0,  // material ID
            flags
        );
    }
    
    /**
     * Execute GPU-driven rendering.
     */
    private void executeGPURendering() {
        float[] viewMatrix = new float[16];
        float[] projMatrix = new float[16];
        // ... get matrices from camera ...
        
        int cullFlags = CullingManager.CULL_FLAG_FRUSTUM |
                       CullingManager.CULL_FLAG_HIZ_OCCLUSION;
        
        drawBridge.executeCullingAndDraw(
            0,  // command buffer
            viewMatrix,
            projMatrix,
            cullFlags
        );
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // EXTENSION SYSTEM
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * Register a rendering extension.
     * 
     * Example:
     * ```java
     * api.registerExtension(new MyCustomRenderPass());
     * ```
     */
    public void registerExtension(RenderingExtension extension) {
        validator.checkNotNull(extension, "extension");
        extensions.add(extension);
    }
    
    /**
     * Unregister an extension.
     */
    public void unregisterExtension(RenderingExtension extension) {
        extensions.remove(extension);
    }
    
    /**
     * Base class for rendering extensions.
     * 
     * Extend this to add custom rendering passes, effects, etc.
     * 
     * Example:
     * ```java
     * public class MyWaterEffect extends RenderingExtension {
     *     @Override
     *     public void onPreRender(World world, List<Entity> entities, float partialTicks) {
     *         // Setup water rendering
     *     }
     *     
     *     @Override
     *     public void onPostRender() {
     *         // Cleanup
     *     }
     * }
     * ```
     */
    public static abstract class RenderingExtension {
        private boolean enabled = true;
        
        /**
         * Called before main rendering.
         */
        public void onPreRender(World world, List<Entity> entities, float partialTicks) {
            // Override in subclass
        }
        
        /**
         * Called after main rendering.
         */
        public void onPostRender() {
            // Override in subclass
        }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // DEBUG API
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * Get debug system for visualization and profiling.
     * 
     * Example:
     * ```java
     * var debug = api.getDebugSystem();
     * debug.getGizmosManager().drawSphere(pos, radius, color);
     * debug.getConsole().executeCommand("stats");
     * ```
     */
    public DeveloperDebugSystem getDebugSystem() {
        return debugSystem;
    }
    
    /**
     * Print comprehensive statistics.
     */
    public void printStatistics() {
        System.out.println("=== Rendering Statistics ===");
        
        // Culling stats
        var cullStats = cullingSystem.getStatistics();
        System.out.printf("Culling Cache Hit Rate: %.1f%%\n", 
            cullStats.hitRate() * 100);
        
        // Draw stats
        var drawStats = drawBridge.getStatistics();
        System.out.printf("Active Instances: %,d\n", 
            drawStats.activeInstances());
        System.out.printf("Average Cull Rate: %.1f%%\n", 
            drawStats.getAverageCullRate() * 100);
        
        // Validation stats
        var validStats = validator.getStatistics();
        System.out.printf("Validation Failures: %,d / %,d (%.4f%%)\n",
            validStats.totalFailures(),
            validStats.totalChecks(),
            validStats.failureRate() * 100);
    }
    
    /**
     * Generate full diagnostic report.
     */
    public String generateDiagnosticReport() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== Astralis Rendering Engine Diagnostic Report ===\n\n");
        
        // Profiler report
        sb.append(debugSystem.getProfiler().generateReport());
        sb.append("\n");
        
        // Validation report
        sb.append(validator.generateReport());
        sb.append("\n");
        
        return sb.toString();
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ════════════════════════════════════════════════════════════════════════
    
    /**
     * Configuration for rendering API.
     * 
     * Use builder pattern for easy configuration:
     * ```java
     * var config = new Configuration.Builder()
     *     .validationLevel(ValidationLevel.NORMAL)
     *     .enableDebugHUD(true)
     *     .build();
     * ```
     */
    public static class Configuration {
        public final StabilityValidator.ValidationLevel validationLevel;
        public final boolean enableDebugHUD;
        public final boolean enableProfiling;
        public final boolean enableAnomalyDetection;
        
        public Configuration() {
            this(new Builder());
        }
        
        private Configuration(Builder builder) {
            this.validationLevel = builder.validationLevel;
            this.enableDebugHUD = builder.enableDebugHUD;
            this.enableProfiling = builder.enableProfiling;
            this.enableAnomalyDetection = builder.enableAnomalyDetection;
        }
        
        public static class Builder {
            private StabilityValidator.ValidationLevel validationLevel = 
                StabilityValidator.ValidationLevel.NORMAL;
            private boolean enableDebugHUD = true;
            private boolean enableProfiling = false;
            private boolean enableAnomalyDetection = true;
            
            public Builder validationLevel(StabilityValidator.ValidationLevel level) {
                this.validationLevel = level;
                return this;
            }
            
            public Builder enableDebugHUD(boolean enable) {
                this.enableDebugHUD = enable;
                return this;
            }
            
            public Builder enableProfiling(boolean enable) {
                this.enableProfiling = enable;
                return this;
            }
            
            public Builder enableAnomalyDetection(boolean enable) {
                this.enableAnomalyDetection = enable;
                return this;
            }
            
            public Configuration build() {
                return new Configuration(this);
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════
    // DETAIL LEVEL
    // ════════════════════════════════════════════════════════════════════════
    
    public enum DetailLevel {
        FULL,       // Full detail, all features
        REDUCED,    // Reduced detail, some features disabled
        MINIMAL     // Minimal detail, only essential geometry
    }
    
    /**
     * Constant flags from IndirectDrawBridge.
     * Re-exported here for convenience.
     */
    public static final class InstanceFlags {
        public static final int ENABLED = 1 << 0;
        public static final int CAST_SHADOW = 1 << 1;
        public static final int STATIC = 1 << 2;
        public static final int SKINNED = 1 << 8;
        public static final int WIND_AFFECTED = 1 << 9;
    }
}
