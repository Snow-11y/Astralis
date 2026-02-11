package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps42 - OpenGL 4.2 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                    ATOMIC COUNTERS & IMAGE LOAD/STORE                        ║
 * ║                                                                              ║
 * ║  OpenGL 4.2 (2011) MAJOR FEATURES:                                           ║
 * ║                                                                              ║
 * ║  ATOMIC COUNTER BUFFERS:                                                     ║
 * ║  • GL_ATOMIC_COUNTER_BUFFER target                                           ║
 * ║  • Shader atomic operations on buffer values                                 ║
 * ║  • GPU-driven counting without CPU sync                                      ║
 * ║  • Perfect for particle counts, visibility, occlusion                        ║
 * ║                                                                              ║
 * ║  IMAGE LOAD/STORE:                                                           ║
 * ║  • Direct texture read/write from shaders                                    ║
 * ║  • Arbitrary texture access patterns                                         ║
 * ║  • Memory barriers for synchronization                                       ║
 * ║                                                                              ║
 * ║  OTHER FEATURES:                                                             ║
 * ║  • Immutable texture storage                                                 ║
 * ║  • Transform feedback instanced                                              ║
 * ║  • Shader packing functions                                                  ║
 * ║                                                                              ║
 * ║  PERFORMANCE IMPACT:                                                         ║
 * ║  • Atomic counters eliminate CPU readbacks                                   ║
 * ║  • GPU can maintain its own counters                                         ║
 * ║  • Enables GPU-driven culling pipelines                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 4.2              │
 * │ Color: #00BFFF (Deep Sky Blue)          │
 * └─────────────────────────────────────────┘
 */
public final class GLBufferOps42 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 42;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL42;
    public static final String VERSION_NAME = "OpenGL 4.2";
    
    // NEW in 4.2: Atomic counter buffer
    public static final int GL_ATOMIC_COUNTER_BUFFER = 0x92C0;
    
    // Existing targets
    public static final int GL_DRAW_INDIRECT_BUFFER = 0x8F3F;
    public static final int GL_COPY_READ_BUFFER = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    
    // Map flags
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_MAP_WRITE_BIT = 0x0002;
    public static final int GL_MAP_INVALIDATE_RANGE_BIT = 0x0004;
    public static final int GL_MAP_INVALIDATE_BUFFER_BIT = 0x0008;
    public static final int GL_MAP_FLUSH_EXPLICIT_BIT = 0x0010;
    public static final int GL_MAP_UNSYNCHRONIZED_BIT = 0x0020;
    
    // Memory barrier bits (for image load/store sync)
    public static final int GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT = 0x00000001;
    public static final int GL_ELEMENT_ARRAY_BARRIER_BIT = 0x00000002;
    public static final int GL_UNIFORM_BARRIER_BIT = 0x00000004;
    public static final int GL_BUFFER_UPDATE_BARRIER_BIT = 0x00000200;
    public static final int GL_ATOMIC_COUNTER_BARRIER_BIT = 0x00001000;
    public static final int GL_ALL_BARRIER_BITS = 0xFFFFFFFF;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Thread-local pools
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static final ThreadLocal<IntBuffer> TL_1 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer());
    private static final ThreadLocal<IntBuffer> TL_16 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer());
    private static final ThreadLocal<IntBuffer> TL_64 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(256).order(ByteOrder.nativeOrder()).asIntBuffer());
    private static final ThreadLocal<IntBuffer> TL_256 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    // Atomic counter specific buffer (commonly need to reset counters)
    private static final ThreadLocal<IntBuffer> TL_ATOMIC = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer()); // 16 counters
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    
    public GLBufferOps42() {
        TL_1.get(); TL_16.get(); TL_64.get(); TL_256.get(); TL_ATOMIC.get();
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
    // Pool Selection
    // ─────────────────────────────────────────────────────────────────────────────
    
    private IntBuffer pool(int n) {
        IntBuffer b;
        if (n <= 1) b = TL_1.get();
        else if (n <= 16) b = TL_16.get();
        else if (n <= 64) b = TL_64.get();
        else if (n <= 256) b = TL_256.get();
        else return ByteBuffer.allocateDirect(n << 2).order(ByteOrder.nativeOrder()).asIntBuffer();
        b.clear();
        if (n > 1) b.limit(n);
        return b;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Generation
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int genBuffer() {
        IntBuffer b = TL_1.get();
        b.clear();
        OpenGLCallMapper.glGenBuffers(b);
        return b.get(0);
    }
    
    @Override
    public int[] genBuffers(int count) {
        if (count <= 0) return new int[0];
        if (count == 1) return new int[] { genBuffer() };
        IntBuffer b = pool(count);
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
        IntBuffer b = TL_1.get();
        b.clear().put(0, buffer);
        OpenGLCallMapper.glDeleteBuffers(b);
    }
    
    @Override
    public void deleteBuffers(int[] buffers) {
        if (buffers == null || buffers.length == 0) return;
        if (buffers.length == 1) { deleteBuffer(buffers[0]); return; }
        IntBuffer b = pool(buffers.length);
        b.put(buffers).flip();
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
    // Data Operations
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
    
    @Override public ByteBuffer mapBuffer(int t, int a) { return OpenGLCallMapper.glMapBuffer(t, a); }
    @Override public ByteBuffer mapBuffer(int t, int a, long l) { return OpenGLCallMapper.glMapBuffer(t, a, l, null); }
    @Override public ByteBuffer mapBufferRange(int t, long o, long l, int a) { return OpenGLCallMapper.glMapBufferRange(t, o, l, a); }
    @Override public void flushMappedBufferRange(int t, long o, long l) { OpenGLCallMapper.glFlushMappedBufferRange(t, o, l); }
    @Override public boolean unmapBuffer(int t) { return OpenGLCallMapper.glUnmapBuffer(t); }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Copy
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void copyBufferSubData(int rt, int wt, long ro, long wo, long sz) {
        OpenGLCallMapper.glCopyBufferSubData(rt, wt, ro, wo, sz);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Query
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int getBufferParameteri(int target, int pname) {
        return OpenGLCallMapper.glGetBufferParameteri(target, pname);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // GL 4.2 Convenience: Atomic Counter Helpers
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Create an atomic counter buffer.
     * Each counter is a uint (4 bytes).
     * 
     * Usage pattern:
     * 1. Create buffer with N counters
     * 2. Reset counters to 0 before use
     * 3. Bind to GL_ATOMIC_COUNTER_BUFFER with index
     * 4. Shader increments/decrements atomically
     * 5. Read back or use in subsequent passes
     * 
     * @param counterCount Number of atomic counters
     * @return Buffer ID
     */
    public int createAtomicCounterBuffer(int counterCount) {
        int buf = genBuffer();
        bindBuffer(GL_ATOMIC_COUNTER_BUFFER, buf);
        bufferData(GL_ATOMIC_COUNTER_BUFFER, counterCount * 4L, GLBufferOpsBase.GL_DYNAMIC_DRAW);
        return buf;
    }
    
    /**
     * Reset atomic counters to zero.
     * ZERO ALLOCATION for up to 16 counters.
     */
    public void resetAtomicCounters(int counterCount) {
        if (counterCount <= 0) return;
        
        IntBuffer zeros;
        if (counterCount <= 16) {
            zeros = TL_ATOMIC.get();
            zeros.clear();
            for (int i = 0; i < counterCount; i++) zeros.put(0);
            zeros.flip();
        } else {
            zeros = ByteBuffer.allocateDirect(counterCount * 4)
                              .order(ByteOrder.nativeOrder())
                              .asIntBuffer();
            for (int i = 0; i < counterCount; i++) zeros.put(0);
            zeros.flip();
        }
        
        bufferSubData(GL_ATOMIC_COUNTER_BUFFER, 0, zeros);
    }
    
    /**
     * Reset a single atomic counter to zero at given index.
     */
    public void resetAtomicCounter(int index) {
        IntBuffer zero = TL_1.get();
        zero.clear();
        zero.put(0, 0);
        bufferSubData(GL_ATOMIC_COUNTER_BUFFER, index * 4L, zero);
    }
    
    /**
     * Set atomic counter to specific value.
     */
    public void setAtomicCounter(int index, int value) {
        IntBuffer val = TL_1.get();
        val.clear();
        val.put(0, value);
        bufferSubData(GL_ATOMIC_COUNTER_BUFFER, index * 4L, val);
    }
    
    /**
     * Issue memory barrier for atomic counter operations.
     * Call after shader writes before reading counters.
     */
    public void atomicCounterBarrier() {
        OpenGLCallMapper.glMemoryBarrier(GL_ATOMIC_COUNTER_BARRIER_BIT);
    }
    
    /**
     * Issue memory barrier for buffer updates.
     */
    public void bufferBarrier() {
        OpenGLCallMapper.glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void shutdown() {}
}
