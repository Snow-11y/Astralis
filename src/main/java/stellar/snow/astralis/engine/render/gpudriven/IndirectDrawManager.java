package stellar.snow.astralis.engine.render.gpudriven;
import stellar.snow.astralis.engine.gpu.compute.IndirectDrawManager;
import stellar.snow.astralis.engine.gpu.authority.GPUBackend;
import stellar.snow.astralis.engine.gpu.authority.GPUBackendSelector;
/**
 * GPU-driven indirect draw integration wrapper.
 * 
 * This delegates to LO's production-grade IndirectDrawManager instead of
 * reimplementing weak draw logic. IndirectDrawManager provides:
 * - Lock-free instance allocation
 * - GPU-driven culling pipeline
 * - HiZ occlusion culling
 * - Meshlet support
 * - N-buffered resources
 * - Async statistics readback
 * 
 * This wrapper adapts the rendering system to use those capabilities.
 */
    
    private final IndirectDrawManager drawManager;
    private final GPUBackend backend;
    
    public IndirectDrawBridge() {
        this.backend = GPUBackendSelector.selectBackend();
        this.drawManager = new IndirectDrawManager(backend, 3);  // 3 frames in flight
    }
    
    /**
     * Begin a new frame.
     */
    public void beginFrame() {
        drawManager.beginFrame();
    }
    
    /**
     * Add an instance to be rendered.
     */
    public int addInstance(
        int meshId,
        float[] transform,
        int materialId,
        int flags
    ) {
        return drawManager.allocateInstance();
    }
    
    /**
     * Update instance transform.
     */
    public void updateInstanceTransform(int instanceId, float[] transform) {
        drawManager.updateInstanceTransform(instanceId, transform);
    }
    
    /**
     * Set instance flags (visibility, shadow casting, etc).
     */
    public void setInstanceFlags(int instanceId, int flags) {
        drawManager.setInstanceFlags(instanceId, flags);
    }
    
    /**
     * Enable/disable instance.
     */
    public void setInstanceEnabled(int instanceId, boolean enabled) {
        drawManager.setInstanceEnabled(instanceId, enabled);
    }
    
    /**
     * Execute GPU-driven culling and rendering.
     */
    public void executeCullingAndDraw(
        long commandBuffer,
        float[] viewMatrix,
        float[] projMatrix,
        int cullFlags
    ) {
        drawManager.updateCameraData(viewMatrix, projMatrix);
        drawManager.performGPUCulling(cullFlags);
        drawManager.executeIndirectDraw(commandBuffer, false);
    }
    
    /**
     * Execute transparent pass.
     */
    public void executeTransparentDraw(long commandBuffer) {
        drawManager.executeIndirectDraw(commandBuffer, true);
    }
    
    /**
     * End frame and submit GPU work.
     */
    public IndirectDrawManager.FrameData endFrame() {
        return drawManager.endFrame();
    }
    
    /**
     * Set frame fence for synchronization.
     */
    public void setFrameFence(long fence) {
        drawManager.setFrameFence(fence);
    }
    
    /**
     * Get statistics.
     */
    public IndirectDrawManager.Statistics getStatistics() {
        return drawManager.getStatistics();
    }
    
    /**
     * Reset statistics.
     */
    public void resetStatistics() {
        drawManager.resetStatistics();
    }
    
    /**
     * Destroy and cleanup.
     */
    public void destroy() {
        drawManager.destroy();
    }
}
