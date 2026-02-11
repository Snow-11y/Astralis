package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps33 - OpenGL 3.3 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║          ★★★★★ THE UNIVERSAL SWEET SPOT FOR MODERN GAMING ★★★★★            ║
 * ║                                                                              ║
 * ║  OpenGL 3.3 (2010) - The Gold Standard:                                      ║
 * ║  • Virtually ALL modern GPUs support 3.3 (even Intel HD 2000+)               ║
 * ║  • Feature-complete for 99% of rendering needs                               ║
 * ║  • Best compatibility/performance balance                                    ║
 * ║                                                                              ║
 * ║  KEY FEATURES:                                                               ║
 * ║  • Instanced rendering via vertex attrib divisor                             ║
 * ║  • Sampler objects                                                           ║
 * ║  • Timer queries                                                             ║
 * ║  • Explicit attribute locations                                              ║
 * ║  • GLSL 3.30                                                                 ║
 * ║  • All GL 3.0-3.2 features                                                   ║
 * ║                                                                              ║
 * ║  BUFFER FEATURES AVAILABLE:                                                  ║
 * ║  ✓ VBOs (since 1.5)                                                          ║
 * ║  ✓ mapBufferRange (since 3.0)                                                ║
 * ║  ✓ copyBufferSubData (since 3.1)                                             ║
 * ║  ✓ flushMappedBufferRange (since 3.0)                                        ║
 * ║  ✓ Uniform buffer objects (since 3.1)                                        ║
 * ║                                                                              ║
 * ║  THIS IS THE RECOMMENDED BASELINE PIPELINE FOR MC 1.12.2 MODS               ║
 * ║                                                                              ║
 * ║  EXTREME OPTIMIZATIONS:                                                      ║
 * ║  • Multi-tier buffer pooling                                                 ║
 * ║  • Inline everything in hot paths                                            ║
 * ║  • Streaming write patterns                                                  ║
 * ║  • Orphaning support                                                         ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 3.3 ★            │
 * │ Color: #00FF7F (Spring Green)           │
 * └─────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public final class GLBufferOps33 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 33;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL33;
    public static final String VERSION_NAME = "OpenGL 3.3";
    
    // Buffer targets
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_COPY_READ_BUFFER = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    public static final int GL_TEXTURE_BUFFER = 0x8C2A;
    public static final int GL_TRANSFORM_FEEDBACK_BUFFER = 0x8C8E;
    
    // Map access flags
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_MAP_WRITE_BIT = 0x0002;
    public static final int GL_MAP_INVALIDATE_RANGE_BIT = 0x0004;
    public static final int GL_MAP_INVALIDATE_BUFFER_BIT = 0x0008;
    public static final int GL_MAP_FLUSH_EXPLICIT_BIT = 0x0010;
    public static final int GL_MAP_UNSYNCHRONIZED_BIT = 0x0020;
    
    // Optimal flags for common operations
    public static final int STREAM_WRITE_FLAGS = 
        GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT;
    
    public static final int STREAM_WRITE_UNSYNC_FLAGS = 
        GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_RANGE_BIT | GL_MAP_UNSYNCHRONIZED_BIT;
    
    public static final int PARTIAL_UPDATE_FLAGS = 
        GL_MAP_WRITE_BIT | GL_MAP_FLUSH_EXPLICIT_BIT;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Multi-tier buffer pool for ZERO allocation in common cases
    // ─────────────────────────────────────────────────────────────────────────────
    
    // Tier 1: Single operations (most common)
    private static final ThreadLocal<IntBuffer> TL_SINGLE = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    // Tier 2: Small batch (2-16 buffers, very common)
    private static final ThreadLocal<IntBuffer> TL_SMALL = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    // Tier 3: Medium batch (17-64 buffers, occasional)
    private static final ThreadLocal<IntBuffer> TL_MEDIUM = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(256).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    // Tier 4: Large batch (65-256 buffers, rare)
    private static final ThreadLocal<IntBuffer> TL_LARGE = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    
    public GLBufferOps33() {
        // Warm up ALL tiers to avoid first-call allocation
        TL_SINGLE.get();
        TL_SMALL.get();
        TL_MEDIUM.get();
        TL_LARGE.get();
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
    // Buffer Pool Selection - Inlined for speed
    // ─────────────────────────────────────────────────────────────────────────────
    
    private IntBuffer getBuffer(int count) {
        IntBuffer b;
        if (count <= 1) {
            b = TL_SINGLE.get();
        } else if (count <= 16) {
            b = TL_SMALL.get();
        } else if (count <= 64) {
            b = TL_MEDIUM.get();
        } else if (count <= 256) {
            b = TL_LARGE.get();
        } else {
            // Extremely rare: allocate (>256 buffers at once)
            return ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        }
        b.clear();
        b.limit(count);
        return b;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Generation - HYPER-OPTIMIZED
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int genBuffer() {
        // THE most common operation - fully inlined
        IntBuffer b = TL_SINGLE.get();
        b.clear();
        OpenGLCallMapper.glGenBuffers(b);
        return b.get(0);
    }
    
    @Override
    public int[] genBuffers(int count) {
        // Fast exits
        if (count <= 0) return new int[0];
        if (count == 1) return new int[] { genBuffer() };
        
        IntBuffer b = getBuffer(count);
        OpenGLCallMapper.glGenBuffers(b);
        
        int[] r = new int[count];
        b.rewind();
        b.get(r);
        return r;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Deletion - HYPER-OPTIMIZED
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
        if (buffers == null) return;
        final int len = buffers.length;
        if (len == 0) return;
        if (len == 1) { deleteBuffer(buffers[0]); return; }
        
        IntBuffer b = getBuffer(len);
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
    // Data Upload - Direct calls
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
    // Mapping - Full feature set
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
    // Copy
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void copyBufferSubData(int readTarget, int writeTarget, 
                                   long readOffset, long writeOffset, long size) {
        OpenGLCallMapper.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Query
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int getBufferParameteri(int target, int pname) {
        return OpenGLCallMapper.glGetBufferParameteri(target, pname);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Convenience: Streaming patterns
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Map buffer for streaming writes with buffer orphaning.
     * The driver will allocate new storage, old data is discarded.
     * Best for per-frame dynamic data.
     */
    public ByteBuffer mapForStreamingWrite(int target, long size) {
        // First orphan the buffer
        bufferData(target, size, GLBufferOpsBase.GL_STREAM_DRAW);
        // Then map with invalidate
        return mapBufferRange(target, 0, size, STREAM_WRITE_FLAGS);
    }
    
    /**
     * Map buffer range for streaming without sync.
     * USE ONLY when you're certain no GPU reads are pending on this range.
     * Typically used with triple-buffering or manual fencing.
     */
    public ByteBuffer mapRangeUnsync(int target, long offset, long length) {
        return mapBufferRange(target, offset, length, STREAM_WRITE_UNSYNC_FLAGS);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Convenience: Resize with preservation
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int resizeBuffer(int target, int oldBuffer, long oldSize, long newSize, int usage) {
        int newBuf = genBuffer();
        
        // Use dedicated copy targets
        bindBuffer(GL_COPY_READ_BUFFER, oldBuffer);
        bindBuffer(GL_COPY_WRITE_BUFFER, newBuf);
        bufferData(GL_COPY_WRITE_BUFFER, newSize, usage);
        
        if (oldSize > 0) {
            copyBufferSubData(GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER, 
                             0, 0, Math.min(oldSize, newSize));
        }
        
        deleteBuffer(oldBuffer);
        bindBuffer(target, newBuf);
        return newBuf;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void shutdown() {
        // ThreadLocals cleaned by GC when thread dies
    }
}
