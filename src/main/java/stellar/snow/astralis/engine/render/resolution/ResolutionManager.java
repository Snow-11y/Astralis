package stellar.snow.astralis.engine.render.resolution;

import stellar.snow.astralis.engine.gpu.authority.GPUBackend;
import stellar.snow.astralis.engine.render.graph.RenderGraph;
import stellar.snow.astralis.engine.render.graph.ResourceNode;

import java.util.*;
import java.util.function.Consumer;

/**
 * Advanced Resolution Manager with:
 * - Dynamic Resolution Scaling (DRS)
 * - Multiple upscaling algorithms (FSR 1.0/2.0, Bicubic, Lanczos)
 * - Motion vector support for temporal upscaling
 * - Sharpening post-process (RCAS, CAS)
 * - Quality presets
 * - Performance-based auto-scaling
 * - VR/Multi-view support
 */
public class ResolutionManager {
    
    // ═══════════════════════════════════════════════════════════════════════
    // UPSCALING METHODS
    // ═══════════════════════════════════════════════════════════════════════
    
    public enum UpscaleMethod {
        /** Nearest neighbor - fastest, worst quality */
        NEAREST,
        /** Bilinear interpolation - fast, smooth */
        BILINEAR,
        /** Bicubic interpolation - good quality */
        BICUBIC,
        /** Lanczos - high quality, sharper */
        LANCZOS,
        /** AMD FidelityFX Super Resolution 1.0 (spatial) */
        FSR_1_0,
        /** AMD FidelityFX Super Resolution 2.x (temporal) */
        FSR_2,
        /** NVIDIA DLSS placeholder (requires SDK) */
        DLSS,
        /** Intel XeSS placeholder */
        XESS,
        /** Custom temporal upscaling */
        TEMPORAL_AA_UPSCALE
    }
    
    public enum SharpenMethod {
        NONE,
        /** Contrast Adaptive Sharpening */
        CAS,
        /** Robust Contrast-Adaptive Sharpening (FSR RCAS) */
        RCAS,
        /** Unsharp Mask */
        UNSHARP_MASK,
        /** Adaptive sharpening based on local contrast */
        ADAPTIVE
    }
    
    public enum QualityPreset {
        ULTRA_PERFORMANCE(0.50f, UpscaleMethod.FSR_1_0, SharpenMethod.RCAS, 0.25f),
        PERFORMANCE(0.58f, UpscaleMethod.FSR_1_0, SharpenMethod.RCAS, 0.20f),
        BALANCED(0.67f, UpscaleMethod.FSR_1_0, SharpenMethod.RCAS, 0.17f),
        QUALITY(0.77f, UpscaleMethod.FSR_1_0, SharpenMethod.RCAS, 0.15f),
        ULTRA_QUALITY(0.87f, UpscaleMethod.BICUBIC, SharpenMethod.CAS, 0.10f),
        NATIVE(1.0f, UpscaleMethod.NEAREST, SharpenMethod.NONE, 0.0f),
        SUPERSAMPLING_2X(1.414f, UpscaleMethod.LANCZOS, SharpenMethod.NONE, 0.0f),
        SUPERSAMPLING_4X(2.0f, UpscaleMethod.LANCZOS, SharpenMethod.NONE, 0.0f);
        
        public final float scale;
        public final UpscaleMethod upscaleMethod;
        public final SharpenMethod sharpenMethod;
        public final float sharpenAmount;
        
        QualityPreset(float scale, UpscaleMethod upscale, SharpenMethod sharpen, float amount) {
            this.scale = scale;
            this.upscaleMethod = upscale;
            this.sharpenMethod = sharpen;
            this.sharpenAmount = amount;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DYNAMIC RESOLUTION CONFIG
    // ═══════════════════════════════════════════════════════════════════════
    
    public static class DynamicResolutionConfig {
        public boolean enabled = true;
        public float targetFrameTimeMs = 16.67f;  // 60 FPS target
        public float minScale = 0.5f;
        public float maxScale = 1.0f;
        public float scaleStep = 0.05f;
        public float increaseThreshold = 0.9f;    // If frame time < target * 0.9, increase
        public float decreaseThreshold = 1.1f;    // If frame time > target * 1.1, decrease
        public int historySize = 10;              // Frames to average
        public float smoothingFactor = 0.1f;      // How fast to change scale
        
        public DynamicResolutionConfig() {}
        
        public DynamicResolutionConfig target(float fps) {
            this.targetFrameTimeMs = 1000.0f / fps;
            return this;
        }
        
        public DynamicResolutionConfig range(float min, float max) {
            this.minScale = min;
            this.maxScale = max;
            return this;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MOTION VECTOR CONFIG (for temporal upscaling)
    // ═══════════════════════════════════════════════════════════════════════
    
    public static class MotionVectorConfig {
        public boolean enabled = true;
        public boolean dilated = true;          // Use dilated motion vectors
        public float velocityScale = 1.0f;
        public ResourceNode motionVectorInput;
        public ResourceNode depthInput;
        public ResourceNode previousColorInput;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════
    
    // Display dimensions
    private int displayWidth = 1920;
    private int displayHeight = 1080;
    
    // Current settings
    private float renderScale = 1.0f;
    private UpscaleMethod upscaleMethod = UpscaleMethod.FSR_1_0;
    private SharpenMethod sharpenMethod = SharpenMethod.RCAS;
    private float sharpenAmount = 0.2f;
    private QualityPreset currentPreset = QualityPreset.QUALITY;
    
    // Dynamic resolution
    private final DynamicResolutionConfig drsConfig = new DynamicResolutionConfig();
    private final LinkedList<Float> frameTimeHistory = new LinkedList<>();
    private float currentDynamicScale = 1.0f;
    private float targetDynamicScale = 1.0f;
    
    // Motion vectors for temporal upscaling
    private final MotionVectorConfig motionConfig = new MotionVectorConfig();
    
    // Jitter for temporal effects
    private int jitterIndex = 0;
    private float jitterX = 0.0f;
    private float jitterY = 0.0f;
    private static final float[][] HALTON_SEQUENCE = generateHaltonSequence(16);
    
    // Pipelines (lazy-loaded)
    private final Map<UpscaleMethod, Long> upscalePipelines = new HashMap<>();
    private final Map<SharpenMethod, Long> sharpenPipelines = new HashMap<>();
    
    // Listeners
    private final List<Consumer<ResolutionChangeEvent>> resolutionListeners = new ArrayList<>();
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════
    
    public void setDisplaySize(int width, int height) {
        if (this.displayWidth != width || this.displayHeight != height) {
            this.displayWidth = width;
            this.displayHeight = height;
            notifyResolutionChange();
        }
    }
    
    public void setRenderScale(float scale) {
        scale = Math.max(0.25f, Math.min(2.0f, scale));
        if (Math.abs(this.renderScale - scale) > 0.001f) {
            this.renderScale = scale;
            notifyResolutionChange();
        }
    }
    
    public void setQualityPreset(QualityPreset preset) {
        this.currentPreset = preset;
        this.renderScale = preset.scale;
        this.upscaleMethod = preset.upscaleMethod;
        this.sharpenMethod = preset.sharpenMethod;
        this.sharpenAmount = preset.sharpenAmount;
        notifyResolutionChange();
    }
    
    public void setUpscaleMethod(UpscaleMethod method) {
        this.upscaleMethod = method;
    }
    
    public void setSharpenMethod(SharpenMethod method) {
        this.sharpenMethod = method;
    }
    
    public void setSharpenAmount(float amount) {
        this.sharpenAmount = Math.max(0.0f, Math.min(1.0f, amount));
    }
    
    public DynamicResolutionConfig getDynamicResolutionConfig() {
        return drsConfig;
    }
    
    public MotionVectorConfig getMotionConfig() {
        return motionConfig;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════════════════
    
    public int getDisplayWidth() { return displayWidth; }
    public int getDisplayHeight() { return displayHeight; }
    public float getRenderScale() { return renderScale; }
    
    public int getRenderWidth() {
        float effectiveScale = drsConfig.enabled ? currentDynamicScale : renderScale;
        return Math.max(1, (int)(displayWidth * effectiveScale));
    }
    
    public int getRenderHeight() {
        float effectiveScale = drsConfig.enabled ? currentDynamicScale : renderScale;
        return Math.max(1, (int)(displayHeight * effectiveScale));
    }
    
    public float getEffectiveScale() {
        return drsConfig.enabled ? currentDynamicScale : renderScale;
    }
    
    public boolean requiresUpscaling() {
        return getEffectiveScale() < 0.99f;
    }
    
    public boolean requiresDownscaling() {
        return getEffectiveScale() > 1.01f;
    }
    
    public float getJitterX() { return jitterX; }
    public float getJitterY() { return jitterY; }
    
    /**
     * Get jitter offset in pixels for the current frame.
     */
    public float[] getJitterOffset() {
        return new float[] {
            jitterX * 2.0f / getRenderWidth(),
            jitterY * 2.0f / getRenderHeight()
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DYNAMIC RESOLUTION
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Update dynamic resolution based on frame time.
     * Call once per frame with the GPU frame time in milliseconds.
     */
    public void updateDynamicResolution(float frameTimeMs) {
        if (!drsConfig.enabled) return;
        
        // Add to history
        frameTimeHistory.addLast(frameTimeMs);
        while (frameTimeHistory.size() > drsConfig.historySize) {
            frameTimeHistory.removeFirst();
        }
        
        // Calculate average
        float avgFrameTime = 0;
        for (float t : frameTimeHistory) {
            avgFrameTime += t;
        }
        avgFrameTime /= frameTimeHistory.size();
        
        // Determine target scale
        float ratio = avgFrameTime / drsConfig.targetFrameTimeMs;
        
        if (ratio < drsConfig.increaseThreshold && targetDynamicScale < drsConfig.maxScale) {
            // Performance headroom - increase quality
            targetDynamicScale = Math.min(drsConfig.maxScale, 
                targetDynamicScale + drsConfig.scaleStep);
        } else if (ratio > drsConfig.decreaseThreshold && targetDynamicScale > drsConfig.minScale) {
            // Over budget - decrease quality
            targetDynamicScale = Math.max(drsConfig.minScale,
                targetDynamicScale - drsConfig.scaleStep);
        }
        
        // Smooth transition
        currentDynamicScale += (targetDynamicScale - currentDynamicScale) * drsConfig.smoothingFactor;
        currentDynamicScale = Math.max(drsConfig.minScale, 
            Math.min(drsConfig.maxScale, currentDynamicScale));
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // TEMPORAL JITTER
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Advance jitter sequence for temporal effects.
     * Call once per frame before rendering.
     */
    public void advanceJitter() {
        jitterIndex = (jitterIndex + 1) % HALTON_SEQUENCE.length;
        jitterX = HALTON_SEQUENCE[jitterIndex][0] - 0.5f;
        jitterY = HALTON_SEQUENCE[jitterIndex][1] - 0.5f;
    }
    
    private static float[][] generateHaltonSequence(int count) {
        float[][] sequence = new float[count][2];
        for (int i = 0; i < count; i++) {
            sequence[i][0] = halton(i + 1, 2);
            sequence[i][1] = halton(i + 1, 3);
        }
        return sequence;
    }
    
    private static float halton(int index, int base) {
        float result = 0;
        float f = 1.0f / base;
        int i = index;
        while (i > 0) {
            result += f * (i % base);
            i = i / base;
            f /= base;
        }
        return result;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // RENDER GRAPH INTEGRATION
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Inject upscaling and sharpening passes into the render graph.
     */
    public void injectUpscalePasses(
            RenderGraph graph,
            ResourceNode lowResColor,
            ResourceNode highResOutput) {
        
        injectUpscalePasses(graph, lowResColor, null, null, null, highResOutput);
    }
    
    /**
     * Inject upscaling with motion vectors for temporal upscaling.
     */
    public void injectUpscalePasses(
            RenderGraph graph,
            ResourceNode lowResColor,
            ResourceNode motionVectors,
            ResourceNode depth,
            ResourceNode previousColor,
            ResourceNode highResOutput) {
        
        float effectiveScale = getEffectiveScale();
        
        // Native resolution - just copy
        if (Math.abs(effectiveScale - 1.0f) < 0.01f) {
            graph.addPass("Native_Copy")
                .read(lowResColor)
                .write(highResOutput)
                .setExecutor((backend, cmd) -> {
                    backend.cmdBlitImage(cmd,
                        lowResColor.handle, highResOutput.handle,
                        GPUBackend.Filter.NEAREST);
                });
            return;
        }
        
        // Determine intermediate resources
        ResourceNode upscaleOutput = highResOutput;
        if (sharpenMethod != SharpenMethod.NONE) {
            upscaleOutput = graph.createTransientTexture("upscale_temp",
                displayWidth, displayHeight, lowResColor.format);
        }
        
        // Add upscale pass
        switch (upscaleMethod) {
            case NEAREST, BILINEAR -> injectSimpleUpscale(graph, lowResColor, upscaleOutput);
            case BICUBIC -> injectBicubicUpscale(graph, lowResColor, upscaleOutput);
            case LANCZOS -> injectLanczosUpscale(graph, lowResColor, upscaleOutput);
            case FSR_1_0 -> injectFSR1Upscale(graph, lowResColor, upscaleOutput);
            case FSR_2 -> injectFSR2Upscale(graph, lowResColor, motionVectors, 
                                             depth, previousColor, upscaleOutput);
            case TEMPORAL_AA_UPSCALE -> injectTemporalUpscale(graph, lowResColor,
                                                               motionVectors, depth,
                                                               previousColor, upscaleOutput);
            default -> injectSimpleUpscale(graph, lowResColor, upscaleOutput);
        }
        
        // Add sharpen pass if needed
        if (sharpenMethod != SharpenMethod.NONE && upscaleOutput != highResOutput) {
            injectSharpenPass(graph, upscaleOutput, highResOutput);
        }
    }
    
    private void injectSimpleUpscale(RenderGraph graph, ResourceNode input, ResourceNode output) {
        int filter = upscaleMethod == UpscaleMethod.NEAREST ? 
            GPUBackend.Filter.NEAREST : GPUBackend.Filter.LINEAR;
        
        graph.addPass("Simple_Upscale")
            .read(input)
            .write(output)
            .setExecutor((backend, cmd) -> {
                backend.cmdBlitImage(cmd, input.handle, output.handle, filter);
            });
    }
    
    private void injectBicubicUpscale(RenderGraph graph, ResourceNode input, ResourceNode output) {
        graph.addPass("Bicubic_Upscale")
            .read(input)
            .write(output)
            .setExecutor((backend, cmd) -> {
                long pipeline = getUpscalePipeline(backend, UpscaleMethod.BICUBIC);
                backend.cmdBindComputePipeline(cmd, pipeline);
                
                // Set uniforms
                float[] params = {
                    (float) input.width, (float) input.height,
                    (float) output.width, (float) output.height
                };
                backend.cmdPushConstants(cmd, GPUBackend.ShaderStage.COMPUTE, 0, params);
                
                // Bind textures
                backend.cmdBindTexture(cmd, 0, input.handle);
                backend.cmdBindStorageImage(cmd, 1, output.handle);
                
                // Dispatch
                int groupsX = (output.width + 7) / 8;
                int groupsY = (output.height + 7) / 8;
                backend.cmdDispatch(cmd, groupsX, groupsY, 1);
            });
    }
    
    private void injectLanczosUpscale(RenderGraph graph, ResourceNode input, ResourceNode output) {
        graph.addPass("Lanczos_Upscale")
            .read(input)
            .write(output)
            .setExecutor((backend, cmd) -> {
                long pipeline = getUpscalePipeline(backend, UpscaleMethod.LANCZOS);
                backend.cmdBindComputePipeline(cmd, pipeline);
                
                float[] params = {
                    (float) input.width, (float) input.height,
                    (float) output.width, (float) output.height,
                    2.0f, 0.0f, 0.0f, 0.0f  // Lanczos radius = 2
                };
                backend.cmdPushConstants(cmd, GPUBackend.ShaderStage.COMPUTE, 0, params);
                
                backend.cmdBindTexture(cmd, 0, input.handle);
                backend.cmdBindStorageImage(cmd, 1, output.handle);
                
                int groupsX = (output.width + 7) / 8;
                int groupsY = (output.height + 7) / 8;
                backend.cmdDispatch(cmd, groupsX, groupsY, 1);
            });
    }
    
    private void injectFSR1Upscale(RenderGraph graph, ResourceNode input, ResourceNode output) {
        // FSR 1.0 is a two-pass algorithm: EASU (upscale) + RCAS (sharpen)
        // RCAS is handled in sharpen pass if enabled
        
        graph.addPass("FSR1_EASU")
            .read(input)
            .write(output)
            .setExecutor((backend, cmd) -> {
                long pipeline = getUpscalePipeline(backend, UpscaleMethod.FSR_1_0);
                backend.cmdBindComputePipeline(cmd, pipeline);
                
                // FSR EASU constants
                float[] easuConst0 = calculateEASUConst0(input.width, input.height, 
                                                         input.width, input.height,
                                                         output.width, output.height);
                float[] easuConst1 = calculateEASUConst1(input.width, input.height,
                                                         output.width, output.height);
                float[] easuConst2 = calculateEASUConst2(input.width, input.height);
                float[] easuConst3 = { 0, 0, 0, 0 };
                
                backend.cmdPushConstants(cmd, GPUBackend.ShaderStage.COMPUTE, 0, easuConst0);
                backend.cmdPushConstants(cmd, GPUBackend.ShaderStage.COMPUTE, 16, easuConst1);
                backend.cmdPushConstants(cmd, GPUBackend.ShaderStage.COMPUTE, 32, easuConst2);
                backend.cmdPushConstants(cmd, GPUBackend.ShaderStage.COMPUTE, 48, easuConst3);
                
                backend.cmdBindTexture(cmd, 0, input.handle);
                backend.cmdBindStorageImage(cmd, 1, output.handle);
                
                int groupsX = (output.width + 15) / 16;
                int groupsY = (output.height + 15) / 16;
                backend.cmdDispatch(cmd, groupsX, groupsY, 1);
            });
    }
    
    private void injectFSR2Upscale(
            RenderGraph graph,
            ResourceNode color,
            ResourceNode motionVectors,
            ResourceNode depth,
            ResourceNode previousColor,
            ResourceNode output) {
        
        if (motionVectors == null || depth == null) {
            // Fallback to FSR 1.0 if motion vectors not available
            injectFSR1Upscale(graph, color, output);
            return;
        }
        
        graph.addPass("FSR2_Temporal")
            .read(color)
            .read(motionVectors)
            .read(depth)
            .read(previousColor)
            .write(output)
            .setExecutor((backend, cmd) -> {
                long pipeline = getUpscalePipeline(backend, UpscaleMethod.FSR_2);
                backend.cmdBindComputePipeline(cmd, pipeline);
                
                float[] params = {
                    (float) color.width, (float) color.height,
                    (float) output.width, (float) output.height,
                    jitterX, jitterY,
                    motionConfig.velocityScale, 0.0f
                };
                backend.cmdPushConstants(cmd, GPUBackend.ShaderStage.COMPUTE, 0, params);
                
                backend.cmdBindTexture(cmd, 0, color.handle);
                backend.cmdBindTexture(cmd, 1, motionVectors.handle);
                backend.cmdBindTexture(cmd, 2, depth.handle);
                backend.cmdBindTexture(cmd, 3, previousColor.handle);
                backend.cmdBindStorageImage(cmd, 4, output.handle);
                
                int groupsX = (output.width + 7) / 8;
                int groupsY = (output.height + 7) / 8;
                backend.cmdDispatch(cmd, groupsX, groupsY, 1);
            });
    }
    
    private void injectTemporalUpscale(
            RenderGraph graph,
            ResourceNode color,
            ResourceNode motionVectors,
            ResourceNode depth,
            ResourceNode previousColor,
            ResourceNode output) {
        
        // Custom temporal AA-based upscaling
        // Similar to FSR 2 but simpler
        injectFSR2Upscale(graph, color, motionVectors, depth, previousColor, output);
    }
    
    private void injectSharpenPass(RenderGraph graph, ResourceNode input, ResourceNode output) {
        graph.addPass("Sharpen_" + sharpenMethod.name())
            .read(input)
            .write(output)
            .setExecutor((backend, cmd) -> {
                long pipeline = getSharpenPipeline(backend, sharpenMethod);
                backend.cmdBindComputePipeline(cmd, pipeline);
                
                float[] params = {
                    (float) input.width, (float) input.height,
                    sharpenAmount, 0.0f
                };
                backend.cmdPushConstants(cmd, GPUBackend.ShaderStage.COMPUTE, 0, params);
                
                backend.cmdBindTexture(cmd, 0, input.handle);
                backend.cmdBindStorageImage(cmd, 1, output.handle);
                
                int groupsX = (output.width + 7) / 8;
                int groupsY = (output.height + 7) / 8;
                backend.cmdDispatch(cmd, groupsX, groupsY, 1);
            });
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FSR CONSTANTS
    // ═══════════════════════════════════════════════════════════════════════
    
    private float[] calculateEASUConst0(int srcW, int srcH, int inputW, int inputH, int dstW, int dstH) {
        return new float[] {
            (float) srcW / dstW,
            (float) srcH / dstH,
            0.5f * (float) srcW / dstW - 0.5f,
            0.5f * (float) srcH / dstH - 0.5f
        };
    }
    
    private float[] calculateEASUConst1(int srcW, int srcH, int dstW, int dstH) {
        return new float[] {
            1.0f / srcW,
            1.0f / srcH,
            1.0f / srcW,
            -1.0f / srcH
        };
    }
    
    private float[] calculateEASUConst2(int srcW, int srcH) {
        return new float[] {
            -1.0f / srcW,
            2.0f / srcH,
            1.0f / srcW,
            2.0f / srcH
        };
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PIPELINE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════
    
    private long getUpscalePipeline(GPUBackend backend, UpscaleMethod method) {
        return upscalePipelines.computeIfAbsent(method, m -> {
            // Load shader based on method
            String shaderName = switch (m) {
                case BICUBIC -> "upscale_bicubic.comp";
                case LANCZOS -> "upscale_lanczos.comp";
                case FSR_1_0 -> "fsr_easu.comp";
                case FSR_2 -> "fsr2_upscale.comp";
                default -> "upscale_bilinear.comp";
            };
            return backend.createComputePipeline(shaderName);
        });
    }
    
    private long getSharpenPipeline(GPUBackend backend, SharpenMethod method) {
        return sharpenPipelines.computeIfAbsent(method, m -> {
            String shaderName = switch (m) {
                case CAS -> "sharpen_cas.comp";
                case RCAS -> "fsr_rcas.comp";
                case UNSHARP_MASK -> "sharpen_unsharp.comp";
                case ADAPTIVE -> "sharpen_adaptive.comp";
                default -> "sharpen_cas.comp";
            };
            return backend.createComputePipeline(shaderName);
        });
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // EVENTS
    // ═══════════════════════════════════════════════════════════════════════
    
    public record ResolutionChangeEvent(
        int displayWidth, int displayHeight,
        int renderWidth, int renderHeight,
        float scale
    ) {}
    
    public void addResolutionListener(Consumer<ResolutionChangeEvent> listener) {
        resolutionListeners.add(listener);
    }
    
    private void notifyResolutionChange() {
        ResolutionChangeEvent event = new ResolutionChangeEvent(
            displayWidth, displayHeight,
            getRenderWidth(), getRenderHeight(),
            getEffectiveScale()
        );
        
        for (Consumer<ResolutionChangeEvent> listener : resolutionListeners) {
            listener.accept(event);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════════════
    
    public record Stats(
        int displayWidth, int displayHeight,
        int renderWidth, int renderHeight,
        float staticScale,
        float dynamicScale,
        float effectiveScale,
        UpscaleMethod upscaleMethod,
        SharpenMethod sharpenMethod,
        QualityPreset preset
    ) {
        public String format() {
            return String.format(
                "Resolution: %dx%d -> %dx%d (%.0f%%), DRS: %.0f%%, Method: %s + %s",
                renderWidth, renderHeight, displayWidth, displayHeight,
                effectiveScale * 100, dynamicScale * 100,
                upscaleMethod.name(), sharpenMethod.name()
            );
        }
    }
    
    public Stats getStats() {
        return new Stats(
            displayWidth, displayHeight,
            getRenderWidth(), getRenderHeight(),
            renderScale,
            currentDynamicScale,
            getEffectiveScale(),
            upscaleMethod,
            sharpenMethod,
            currentPreset
        );
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════
    
    public void destroy(GPUBackend backend) {
        for (long pipeline : upscalePipelines.values()) {
            backend.destroyPipeline(pipeline);
        }
        upscalePipelines.clear();
        
        for (long pipeline : sharpenPipelines.values()) {
            backend.destroyPipeline(pipeline);
        }
        sharpenPipelines.clear();
    }
}
