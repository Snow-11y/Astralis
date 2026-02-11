package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps30 - OpenGL 3.0 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                    ★★★ MODERN OPENGL ERA BEGINS ★★★                         ║
 * ║                                                                              ║
 * ║  OpenGL 3.0 (2008) MAJOR FEATURES:                                           ║
 * ║                                                                              ║
 * ║  BUFFER MAPPING REVOLUTION:                                                  ║
 * ║  • glMapBufferRange - Map portions of buffers!                               ║
 * ║  • glFlushMappedBufferRange - Explicit flush control                         ║
 * ║  • Access flags: READ, WRITE, INVALIDATE, UNSYNCHRONIZED                     ║
 * ║                                                                              ║
 * ║  OTHER KEY FEATURES:                                                         ║
 * ║  • Vertex Array Objects (VAOs) - state encapsulation                         ║
 * ║  • Framebuffer Objects (FBOs) become core                                    ║
 * ║  • GLSL 1.30                                                                 ║
 * ║  • Conditional rendering                                                     ║
 * ║  • Transform feedback                                                        ║
 * ║  • Deprecation model begins                                                  ║
 * ║                                                                              ║
 * ║  PERFORMANCE IMPACT:                                                         ║
 * ║  • mapBufferRange allows UNSYNCHRONIZED updates                              ║
 * ║  • Can invalidate ranges before writing (driver hint)                        ║
 * ║  • Partial mapping reduces memory bandwidth                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 3.0              │
 * │ Color: #32CD32 (Lime Green)             │
 * └─────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public final class GLBufferOps30 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 30;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL30;
    public static final String VERSION_NAME = "OpenGL 3.0";
    
    // Map access flags (THE BIG NEW FEATURE)
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_MAP_WRITE_BIT = 0x0002;
    public static final int GL_MAP_INVALIDATE_RANGE_BIT = 0x0004;
    public static final int GL_MAP_INVALIDATE_BUFFER_BIT = 0x0008;
    public static final int GL_MAP_FLUSH_EXPLICIT_BIT = 0x0010;
    public static final int GL_MAP_UNSYNCHRONIZED_BIT = 0x0020;
    
    // Optimized mapping flags for streaming (write, invalidate, unsync)
    public static final int STREAMING_WRITE_FLAGS = 
        GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_RANGE_BIT | GL_MAP_UNSYNCHRONIZED_BIT;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Thread-local buffers
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static final ThreadLocal<IntBuffer> TL_SINGLE = ThreadLocal.withInitial(() ->
            GLBufferOpsBase.createDirectBuffer(4).asIntBuffer());
    
    private static final ThreadLocal<IntBuffer> TL_BATCH = ThreadLocal.withInitial(() ->
            GLBufferOpsBase.createDirectBuffer(256).asIntBuffer());
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    
    public GLBufferOps30() {
        TL_SINGLE.get();
        TL_BATCH.get();
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Version Info
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override public int getVersionCode() { return VERSION_CODE; }
    @Override public int getDisplayColor() { return DISPLAY_COLOR; }
    @Override public String getVersionName() { return VERSION_NAME; }
    @Override public boolean supportsMapBufferRange() { return true; }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Generation
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int genBuffer() {
        IntBuffer b = TL_SINGLE.get();
        b.clear();
        OpenGLCallMapper.glGenBuffers(b);
        return b.get(0);
    }
    
    @Override
    public int[] genBuffers(int count) {
        if (count <= 0) return new int[0];
        if (count == 1) return new int[] { genBuffer() };
        
        IntBuffer b;
        if (count <= 64) {
            b = TL_BATCH.get();
            b.clear();
            b.limit(count);
        } else {
            b = GLBufferOpsBase.getBatchBuffer(count);
        }
        
        OpenGLCallMapper.glGenBuffers(b);
        int[] r = new int[count];
        b.rewind();
        b.get(r);
        return r;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Deletion
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void deleteBuffer(int buffer) {
        if (buffer == 0) return;
        IntBuffer b = TL_SINGLE.get();
        b.clear();
        b.put(0, buffer);
        OpenGLCallMapper.glDeleteBuffers(b);
    }
    
    @Override
    public void deleteBuffers(int[] buffers) {
        if (buffers == null || buffers.length == 0) return;
        if (buffers.length == 1) { deleteBuffer(buffers[0]); return; }
        
        IntBuffer b;
        if (buffers.length <= 64) {
            b = TL_BATCH.get();
            b.clear();
            b.put(buffers);
            b.flip();
        } else {
            b = GLBufferOpsBase.toDirectBuffer(buffers);
        }
        OpenGLCallMapper.glDeleteBuffers(b);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Binding
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void bindBuffer(int target, int buffer) {
        OpenGLCallMapper.glBindBuffer(target, buffer);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Data Upload
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void bufferData(int target, long size, int usage) {
        OpenGLCallMapper.glBufferData(target, size, usage);
    }
    
    @Override
    public void bufferData(int target, ByteBuffer data, int usage) {
        OpenGLCallMapper.glBufferData(target, data, usage);
    }
    
    @Override
    public void bufferData(int target, FloatBuffer data, int usage) {
        OpenGLCallMapper.glBufferData(target, data, usage);
    }
    
    @Override
    public void bufferData(int target, IntBuffer data, int usage) {
        OpenGLCallMapper.glBufferData(target, data, usage);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Partial Update
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void bufferSubData(int target, long offset, ByteBuffer data) {
        OpenGLCallMapper.glBufferSubData(target, offset, data);
    }
    
    @Override
    public void bufferSubData(int target, long offset, FloatBuffer data) {
        OpenGLCallMapper.glBufferSubData(target, offset, data);
    }
    
    @Override
    public void bufferSubData(int target, long offset, IntBuffer data) {
        OpenGLCallMapper.glBufferSubData(target, offset, data);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Mapping - LEGACY (whole buffer)
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public ByteBuffer mapBuffer(int target, int access) {
        return OpenGLCallMapper.glMapBuffer(target, access);
    }
    
    @Override
    public ByteBuffer mapBuffer(int target, int access, long length) {
        return OpenGLCallMapper.glMapBuffer(target, access, length, null);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Mapping - NEW GL 3.0 FEATURES! ★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Map a range of the buffer - THE KEY GL 3.0 FEATURE.
     * 
     * Recommended flag combinations:
     * 
     * FOR READING:
     *   GL_MAP_READ_BIT
     * 
     * FOR STREAMING WRITES (best perf):
     *   GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_RANGE_BIT | GL_MAP_UNSYNCHRONIZED_BIT
     *   - Invalidate tells driver old data is garbage
     *   - Unsynchronized avoids CPU-GPU sync stall
     *   - YOU must ensure no draw calls are using this range!
     * 
     * FOR UPDATING EXISTING DATA:
     *   GL_MAP_WRITE_BIT
     * 
     * FOR PARTIAL FLUSH:
     *   GL_MAP_WRITE_BIT | GL_MAP_FLUSH_EXPLICIT_BIT
     *   - Then call flushMappedBufferRange for each written region
     */
    @Override
    public ByteBuffer mapBufferRange(int target, long offset, long length, int access) {
        return OpenGLCallMapper.glMapBufferRange(target, offset, length, access);
    }
    
    /**
     * Flush a portion of mapped range to GPU.
     * Only valid when mapped with GL_MAP_FLUSH_EXPLICIT_BIT.
     * 
     * Use case: You're writing to a large mapped region but only updating small parts.
     * Flush only what changed to minimize PCIe traffic.
     */
    @Override
    public void flushMappedBufferRange(int target, long offset, long length) {
        OpenGLCallMapper.glFlushMappedBufferRange(target, offset, length);
    }
    
    @Override
    public boolean unmapBuffer(int target) {
        return OpenGLCallMapper.glUnmapBuffer(target);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Query
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int getBufferParameteri(int target, int pname) {
        return OpenGLCallMapper.glGetBufferParameteri(target, pname);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Convenience: Optimized streaming map
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Optimized map for streaming writes.
     * Uses WRITE | INVALIDATE_RANGE | UNSYNCHRONIZED flags.
     * Caller MUST ensure no pending GPU reads from this range!
     */
    public ByteBuffer mapBufferRangeForStreaming(int target, long offset, long length) {
        return OpenGLCallMapper.glMapBufferRange(target, offset, length, STREAMING_WRITE_FLAGS);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void shutdown() {}
}
