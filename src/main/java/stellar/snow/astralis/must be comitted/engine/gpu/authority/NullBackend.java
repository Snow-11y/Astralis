package stellar.snow.astralis.engine.gpu.authority;

/**
 * NullBackend - Minimal no-op GPU backend for fallback/testing.
 * 
 * This backend does nothing and is used when:
 * - No real GPU backends can be initialized
 * - Software rendering is needed as last resort
 * - Testing without actual GPU access
 * 
 * All methods are no-ops that return safe default values.
 */
public final class NullBackend implements GPUBackend {
    
    private volatile boolean initialized = false;
    
    public NullBackend() {
        // No-op constructor
    }
    
    /**
     * Initialize - always succeeds as no-op.
     */
    public void initialize() {
        initialized = true;
    }
    
    /**
     * Check if initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Get backend name.
     */
    @Override
    public String getName() {
        return "Null Backend (Software Fallback)";
    }
    
    /**
     * Get backend type.
     */
    public String getType() {
        return "NULL";
    }
    
    /**
     * Get version - always 0.0.0.
     */
    public String getVersion() {
        return "0.0.0";
    }
    
    /**
     * Check capabilities - always returns false (no capabilities).
     */
    public boolean hasCapability(String capability) {
        return false;
    }
    
    /**
     * Get capabilities - empty set.
     */
    public java.util.Set<String> getCapabilities() {
        return java.util.Collections.emptySet();
    }
    
    /**
     * Cleanup - no-op.
     */
    @Override
    public void close() {
        initialized = false;
    }
    
    /**
     * Begin frame - no-op.
     */
    public void beginFrame() {
        // No-op
    }
    
    /**
     * End frame - no-op.
     */
    public void endFrame() {
        // No-op
    }
    
    /**
     * Submit work - no-op.
     */
    public void submit() {
        // No-op
    }
    
    /**
     * Wait idle - no-op.
     */
    public void waitIdle() {
        // No-op
    }
    
    /**
     * Get device info.
     */
    public String getDeviceInfo() {
        return "Null Backend - No Device";
    }
    
    /**
     * Check if supports compute.
     */
    public boolean supportsCompute() {
        return false;
    }
    
    /**
     * Check if supports ray tracing.
     */
    public boolean supportsRayTracing() {
        return false;
    }
    
    /**
     * Check if supports mesh shaders.
     */
    public boolean supportsMeshShaders() {
        return false;
    }
    
    /**
     * Get maximum texture size.
     */
    public int getMaxTextureSize() {
        return 1024; // Minimal safe default
    }
    
    /**
     * Get maximum buffer size.
     */
    public long getMaxBufferSize() {
        return 1024 * 1024; // 1MB minimal
    }
    
    /**
     * Get available memory - always returns 0.
     */
    public long getAvailableMemory() {
        return 0;
    }
    
    /**
     * Get total memory - always returns 0.
     */
    public long getTotalMemory() {
        return 0;
    }
    
    @Override
    public String toString() {
        return "NullBackend{initialized=" + initialized + "}";
    }
}
