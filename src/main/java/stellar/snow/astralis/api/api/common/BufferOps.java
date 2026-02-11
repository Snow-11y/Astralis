package stellar.snow.astralis.api.common;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * BufferOps - Common interface for all GL Buffer Operation pipelines.
 * 
 * This interface is OPTIONAL - OpenGLManager uses MethodHandles for dispatch,
 * but implementing this interface provides compile-time safety and documentation.
 * 
 * All implementations MUST follow these rules:
 * - ZERO per-call heap allocation in hot paths
 * - Thread-safe for single render thread usage
 * - Proper resource cleanup in shutdown()
 * - No hidden state mutations
 * 
 * Part of: Astralis / Snowium Render System
 */
public interface BufferOps {
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Version Info (for F3 display)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** GL version code (e.g., 33 for GL 3.3, 46 for GL 4.6) */
    int getVersionCode();
    
    /** Display color for F3 screen (0xRRGGBB format) */
    int getDisplayColor();
    
    /** Human-readable version name */
    String getVersionName();
    
    /** Short name for compact display */
    default String getShortName() {
        int code = getVersionCode();
        if (code == 121) return "1.2.1";
        return (code / 10) + "." + (code % 10);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Core Buffer Operations (Required)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** Generate a single buffer. MUST NOT allocate on heap after warmup. */
    int genBuffer();
    
    /** Generate multiple buffers. Array allocation is unavoidable here. */
    int[] genBuffers(int count);
    
    /** Delete a single buffer. */
    void deleteBuffer(int buffer);
    
    /** Delete multiple buffers. */
    void deleteBuffers(int[] buffers);
    
    /** Bind buffer to target. */
    void bindBuffer(int target, int buffer);
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Data Upload (Required)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** Allocate buffer storage with size only (no data). */
    void bufferData(int target, long size, int usage);
    
    /** Upload ByteBuffer data. */
    void bufferData(int target, ByteBuffer data, int usage);
    
    /** Upload FloatBuffer data. */
    void bufferData(int target, FloatBuffer data, int usage);
    
    /** Upload IntBuffer data. */
    void bufferData(int target, IntBuffer data, int usage);
    
    /** Update portion of buffer with ByteBuffer. */
    void bufferSubData(int target, long offset, ByteBuffer data);
    
    /** Update portion of buffer with FloatBuffer. */
    void bufferSubData(int target, long offset, FloatBuffer data);
    
    /** Update portion of buffer with IntBuffer. */
    void bufferSubData(int target, long offset, IntBuffer data);
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Mapping (Required - may throw on pre-1.5)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** Map entire buffer. Returns null on failure. */
    ByteBuffer mapBuffer(int target, int access);
    
    /** Map entire buffer with length hint. */
    ByteBuffer mapBuffer(int target, int access, long length);
    
    /** Unmap buffer. Returns true on success. */
    boolean unmapBuffer(int target);
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Extended Mapping (Optional - GL 3.0+)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** Map buffer range. Returns null if unsupported. */
    default ByteBuffer mapBufferRange(int target, long offset, long length, int access) {
        return null;
    }
    
    /** Flush mapped range. No-op if unsupported. */
    default void flushMappedBufferRange(int target, long offset, long length) {}
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Copy (Optional - GL 3.1+)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** Copy between buffers. No-op if unsupported. */
    default void copyBufferSubData(int readTarget, int writeTarget, 
                                    long readOffset, long writeOffset, long size) {}
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Invalidation (Optional - GL 4.3+)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** Invalidate entire buffer. No-op if unsupported. */
    default void invalidateBufferData(int buffer) {}
    
    /** Invalidate buffer range. No-op if unsupported. */
    default void invalidateBufferSubData(int buffer, long offset, long length) {}
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Storage (Optional - GL 4.4+)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** Create immutable storage. No-op if unsupported. */
    default void bufferStorage(int target, long size, int flags) {}
    
    /** Create immutable storage with data. No-op if unsupported. */
    default void bufferStorage(int target, ByteBuffer data, int flags) {}
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Query
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** Get buffer parameter. Returns 0 if unsupported. */
    default int getBufferParameteri(int target, int pname) { return 0; }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Capability Queries
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** Does this pipeline support VBOs at all? */
    default boolean supportsVBO() { return getVersionCode() >= 15; }
    
    /** Does this pipeline support mapBufferRange? */
    default boolean supportsMapBufferRange() { return getVersionCode() >= 30; }
    
    /** Does this pipeline support buffer copy? */
    default boolean supportsCopyBuffer() { return getVersionCode() >= 31; }
    
    /** Does this pipeline support buffer invalidation? */
    default boolean supportsInvalidation() { return getVersionCode() >= 43; }
    
    /** Does this pipeline support immutable storage? */
    default boolean supportsBufferStorage() { return getVersionCode() >= 44; }
    
    /** Does this pipeline support DSA? */
    default boolean supportsDSA() { return getVersionCode() >= 45; }
    
    /** Does this pipeline support persistent mapping? */
    default boolean supportsPersistentMapping() { return getVersionCode() >= 44; }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** 
     * Cleanup all resources. Called on shutdown.
     * MUST release all native resources, clear caches, etc.
     */
    void shutdown();
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Convenience: High-level operations (with default implementations)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Create and initialize a buffer in one call.
     * Default implementation: gen + bind + bufferData.
     */
    default int createBuffer(int target, long size, int usage) {
        int buf = genBuffer();
        bindBuffer(target, buf);
        bufferData(target, size, usage);
        return buf;
    }
    
    /**
     * Create and upload data in one call.
     */
    default int createBuffer(int target, ByteBuffer data, int usage) {
        int buf = genBuffer();
        bindBuffer(target, buf);
        bufferData(target, data, usage);
        return buf;
    }
    
    /**
     * Resize a buffer by creating new one and copying data.
     * Returns new buffer ID, deletes old buffer.
     */
    default int resizeBuffer(int target, int oldBuffer, long oldSize, long newSize, int usage) {
        int newBuf = genBuffer();
        bindBuffer(target, newBuf);
        bufferData(target, newSize, usage);
        
        if (supportsCopyBuffer() && oldSize > 0) {
            // Use GPU-side copy
            int copyRead = 0x8F36; // GL_COPY_READ_BUFFER
            bindBuffer(copyRead, oldBuffer);
            copyBufferSubData(copyRead, target, 0, 0, Math.min(oldSize, newSize));
        }
        
        deleteBuffer(oldBuffer);
        return newBuf;
    }
}
