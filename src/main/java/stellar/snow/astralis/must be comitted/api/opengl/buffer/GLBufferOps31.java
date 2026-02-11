package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps31 - OpenGL 3.1 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                       GPU-TO-GPU BUFFER COPY                                 ║
 * ║                                                                              ║
 * ║  OpenGL 3.1 (2009) KEY FEATURES:                                             ║
 * ║                                                                              ║
 * ║  BUFFER COPY:                                                                ║
 * ║  • glCopyBufferSubData - Copy between buffers ON THE GPU!                    ║
 * ║  • GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER targets                         ║
 * ║  • No CPU round-trip needed for buffer-to-buffer operations                  ║
 * ║                                                                              ║
 * ║  OTHER FEATURES:                                                             ║
 * ║  • Uniform Buffer Objects (UBOs) - shared uniforms                           ║
 * ║  • Texture buffer objects                                                    ║
 * ║  • Primitive restart                                                         ║
 * ║  • GLSL 1.40                                                                 ║
 * ║  • Fixed-function removal in core profile                                    ║
 * ║                                                                              ║
 * ║  PERFORMANCE IMPACT:                                                         ║
 * ║  • Buffer defragmentation without CPU                                        ║
 * ║  • Buffer resizing with data preservation                                    ║
 * ║  • GPU-accelerated data movement                                             ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 3.1              │
 * │ Color: #3CB371 (Medium Sea Green)       │
 * └─────────────────────────────────────────┘
 */
public final class GLBufferOps31 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 31;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL31;
    public static final String VERSION_NAME = "OpenGL 3.1";
    
    // Copy buffer targets (new in 3.1)
    public static final int GL_COPY_READ_BUFFER = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    
    // Uniform buffer target (new in 3.1)
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    
    // Map flags (from 3.0)
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_MAP_WRITE_BIT = 0x0002;
    public static final int GL_MAP_INVALIDATE_RANGE_BIT = 0x0004;
    public static final int GL_MAP_INVALIDATE_BUFFER_BIT = 0x0008;
    public static final int GL_MAP_FLUSH_EXPLICIT_BIT = 0x0010;
    public static final int GL_MAP_UNSYNCHRONIZED_BIT = 0x0020;
    
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
    
    public GLBufferOps31() {
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
    @Override public boolean supportsCopyBuffer() { return true; }
    
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
        
        IntBuffer b = (count <= 64) ? TL_BATCH.get() : GLBufferOpsBase.getBatchBuffer(count);
        b.clear();
        if (count <= 64) b.limit(count);
        
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
        
        IntBuffer b = (buffers.length <= 64) ? TL_BATCH.get() : GLBufferOpsBase.getBatchBuffer(buffers.length);
        b.clear();
        b.put(buffers);
        b.flip();
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
    
    @Override public void bufferData(int t, long s, int u) { OpenGLCallMapper.glBufferData(t, s, u); }
    @Override public void bufferData(int t, ByteBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    @Override public void bufferData(int t, FloatBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    @Override public void bufferData(int t, IntBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    
    @Override public void bufferSubData(int t, long o, ByteBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    @Override public void bufferSubData(int t, long o, FloatBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    @Override public void bufferSubData(int t, long o, IntBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Mapping
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public ByteBuffer mapBuffer(int target, int access) {
        return OpenGLCallMapper.glMapBuffer(target, access);
    }
    
    @Override
    public ByteBuffer mapBuffer(int target, int access, long length) {
        return OpenGLCallMapper.glMapBuffer(target, access, length, null);
    }
    
    @Override
    public ByteBuffer mapBufferRange(int target, long offset, long length, int access) {
        return OpenGLCallMapper.glMapBufferRange(target, offset, length, access);
    }
    
    @Override
    public void flushMappedBufferRange(int target, long offset, long length) {
        OpenGLCallMapper.glFlushMappedBufferRange(target, offset, length);
    }
    
    @Override
    public boolean unmapBuffer(int target) {
        return OpenGLCallMapper.glUnmapBuffer(target);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Copy - NEW IN GL 3.1! ★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Copy data between buffers ON THE GPU.
     * 
     * This is a GAME CHANGER for:
     * • Buffer defragmentation
     * • Resizing with data preservation
     * • Duplicating buffer contents
     * • Moving data without CPU involvement
     * 
     * Use GL_COPY_READ_BUFFER and GL_COPY_WRITE_BUFFER targets to avoid
     * disturbing other bindings.
     * 
     * @param readTarget  Source buffer target (or GL_COPY_READ_BUFFER)
     * @param writeTarget Destination buffer target (or GL_COPY_WRITE_BUFFER)
     * @param readOffset  Byte offset in source
     * @param writeOffset Byte offset in destination
     * @param size        Bytes to copy
     */
    @Override
    public void copyBufferSubData(int readTarget, int writeTarget, 
                                   long readOffset, long writeOffset, long size) {
        OpenGLCallMapper.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Convenience: Buffer resize with data preservation
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Resize buffer, preserving existing data.
     * Uses GPU-side copy for efficiency.
     * 
     * @return New buffer ID (old buffer is deleted)
     */
    @Override
    public int resizeBuffer(int target, int oldBuffer, long oldSize, long newSize, int usage) {
        // Create new buffer
        int newBuf = genBuffer();
        
        // Bind to copy targets to preserve other bindings
        bindBuffer(GL_COPY_READ_BUFFER, oldBuffer);
        bindBuffer(GL_COPY_WRITE_BUFFER, newBuf);
        
        // Allocate new buffer
        bufferData(GL_COPY_WRITE_BUFFER, newSize, usage);
        
        // Copy data (only what fits)
        if (oldSize > 0) {
            long copySize = Math.min(oldSize, newSize);
            copyBufferSubData(GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER, 0, 0, copySize);
        }
        
        // Cleanup old buffer
        deleteBuffer(oldBuffer);
        
        // Rebind new buffer to original target
        bindBuffer(target, newBuf);
        
        return newBuf;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Query
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int getBufferParameteri(int target, int pname) {
        return OpenGLCallMapper.glGetBufferParameteri(target, pname);
    }
    
    @Override
    public void shutdown() {}
}
