package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps43 - OpenGL 4.3 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║          ★★★★★ COMPUTE SHADERS & BUFFER INVALIDATION ★★★★★                 ║
 * ║                                                                              ║
 * ║  OpenGL 4.3 (2012) - THE COMPUTE REVOLUTION:                                 ║
 * ║                                                                              ║
 * ║  COMPUTE SHADERS:                                                            ║
 * ║  • General-purpose GPU compute                                               ║
 * ║  • Work groups & local invocations                                           ║
 * ║  • Shared memory between invocations                                         ║
 * ║  • Compute can write to ANY buffer type                                      ║
 * ║                                                                              ║
 * ║  SHADER STORAGE BUFFER OBJECTS (SSBOs):                                      ║
 * ║  • GL_SHADER_STORAGE_BUFFER target                                           ║
 * ║  • Read/write from ANY shader stage                                          ║
 * ║  • Variable-length arrays in shaders                                         ║
 * ║  • Much larger than UBOs (typically 128MB+)                                  ║
 * ║                                                                              ║
 * ║  BUFFER INVALIDATION - HUGE PERFORMANCE WIN:                                 ║
 * ║  • glInvalidateBufferData - Orphan entire buffer                             ║
 * ║  • glInvalidateBufferSubData - Orphan range                                  ║
 * ║  • Tells driver: "Old data is garbage, don't sync"                           ║
 * ║  • Eliminates implicit GPU-CPU synchronization                               ║
 * ║                                                                              ║
 * ║  MULTI-DRAW INDIRECT:                                                        ║
 * ║  • glMultiDrawArraysIndirect                                                 ║
 * ║  • glMultiDrawElementsIndirect                                               ║
 * ║  • Multiple draws from ONE call                                              ║
 * ║  • GPU-driven rendering at scale                                             ║
 * ║                                                                              ║
 * ║  OTHER FEATURES:                                                             ║
 * ║  • GL_DISPATCH_INDIRECT_BUFFER for compute dispatch                          ║
 * ║  • Debug output (KHR_debug)                                                  ║
 * ║  • Explicit uniform location                                                 ║
 * ║  • Vertex attrib binding                                                     ║
 * ║  • Texture views                                                             ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 4.3              │
 * │ Color: #1E90FF (Dodger Blue)            │
 * └─────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public final class GLBufferOps43 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 43;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL43;
    public static final String VERSION_NAME = "OpenGL 4.3";
    
    // NEW in 4.3: SSBO and dispatch indirect
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    public static final int GL_DISPATCH_INDIRECT_BUFFER = 0x90EE;
    
    // Existing targets
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_DRAW_INDIRECT_BUFFER = 0x8F3F;
    public static final int GL_ATOMIC_COUNTER_BUFFER = 0x92C0;
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
    
    // Memory barrier bits
    public static final int GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT = 0x00000001;
    public static final int GL_ELEMENT_ARRAY_BARRIER_BIT = 0x00000002;
    public static final int GL_UNIFORM_BARRIER_BIT = 0x00000004;
    public static final int GL_COMMAND_BARRIER_BIT = 0x00000040;
    public static final int GL_BUFFER_UPDATE_BARRIER_BIT = 0x00000200;
    public static final int GL_SHADER_STORAGE_BARRIER_BIT = 0x00002000;
    public static final int GL_ATOMIC_COUNTER_BARRIER_BIT = 0x00001000;
    public static final int GL_ALL_BARRIER_BITS = 0xFFFFFFFF;
    
    // Compute dispatch command structure (12 bytes)
    public static final int DISPATCH_INDIRECT_COMMAND_SIZE = 12;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Thread-local pools - optimized for compute workloads
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static final ThreadLocal<IntBuffer> TL_1 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    private static final ThreadLocal<IntBuffer> TL_16 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    private static final ThreadLocal<IntBuffer> TL_64 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(256).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    private static final ThreadLocal<IntBuffer> TL_256 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    // Dispatch command buffer
    private static final ThreadLocal<ByteBuffer> TL_DISPATCH = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(DISPATCH_INDIRECT_COMMAND_SIZE)
                      .order(ByteOrder.nativeOrder()));
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    
    public GLBufferOps43() {
        TL_1.get(); TL_16.get(); TL_64.get(); TL_256.get(); TL_DISPATCH.get();
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Version Info
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override public int getVersionCode() { return VERSION_CODE; }
    @Override public int getDisplayColor() { return DISPLAY_COLOR; }
    @Override public String getVersionName() { return VERSION_NAME; }
    @Override public boolean supportsMapBufferRange() { return true; }
    @Override public boolean supportsCopyBuffer() { return true; }
    @Override public boolean supportsInvalidation() { return true; }
    
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
    // GL 4.3: BUFFER INVALIDATION - CRITICAL FOR PERFORMANCE! ★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Invalidate entire buffer contents.
     * 
     * THIS IS HUGE FOR PERFORMANCE:
     * - Tells the driver previous contents are garbage
     * - Driver can orphan the old storage
     * - Eliminates implicit synchronization
     * - Use BEFORE bufferData/bufferSubData for streaming patterns
     * 
     * Perfect for:
     * - Per-frame dynamic buffers
     * - Triple-buffering without explicit sync
     * - Particle system buffers
     * - Any frequently-updated GPU data
     * 
     * @param buffer Buffer ID (NOT target!)
     */
    @Override
    public void invalidateBufferData(int buffer) {
        OpenGLCallMapper.glInvalidateBufferData(buffer);
    }
    
    /**
     * Invalidate a range of buffer contents.
     * More fine-grained than full invalidation.
     * 
     * Use when you're only updating part of a buffer and
     * want to signal that a specific region is stale.
     * 
     * @param buffer Buffer ID (NOT target!)
     * @param offset Byte offset
     * @param length Byte length
     */
    @Override
    public void invalidateBufferSubData(int buffer, long offset, long length) {
        OpenGLCallMapper.glInvalidateBufferSubData(buffer, offset, length);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // GL 4.3: Shader Storage Buffer Helpers
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Create a Shader Storage Buffer Object.
     * SSBOs can be read/written from any shader stage.
     * 
     * Typical uses:
     * - Compute shader input/output
     * - Large data arrays (particle positions, etc.)
     * - Inter-shader communication
     * 
     * @param size Buffer size in bytes
     * @param usage GL_STATIC_DRAW, GL_DYNAMIC_DRAW, etc.
     * @return Buffer ID
     */
    public int createSSBO(long size, int usage) {
        int buf = genBuffer();
        bindBuffer(GL_SHADER_STORAGE_BUFFER, buf);
        bufferData(GL_SHADER_STORAGE_BUFFER, size, usage);
        return buf;
    }
    
    /**
     * Create SSBO with initial data.
     */
    public int createSSBO(ByteBuffer data, int usage) {
        int buf = genBuffer();
        bindBuffer(GL_SHADER_STORAGE_BUFFER, buf);
        bufferData(GL_SHADER_STORAGE_BUFFER, data, usage);
        return buf;
    }
    
    /**
     * Bind SSBO to indexed binding point.
     * Shaders reference by binding index.
     */
    public void bindSSBO(int index, int buffer) {
        OpenGLCallMapper.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, index, buffer);
    }
    
    /**
     * Bind range of SSBO to indexed binding point.
     */
    public void bindSSBORange(int index, int buffer, long offset, long size) {
        OpenGLCallMapper.glBindBufferRange(GL_SHADER_STORAGE_BUFFER, index, buffer, offset, size);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // GL 4.3: Compute Dispatch Indirect Helpers
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Create a dispatch indirect buffer for compute shaders.
     * 
     * Command structure (12 bytes):
     *   uint num_groups_x;
     *   uint num_groups_y;
     *   uint num_groups_z;
     */
    public int createDispatchIndirectBuffer(int commandCount) {
        int buf = genBuffer();
        bindBuffer(GL_DISPATCH_INDIRECT_BUFFER, buf);
        bufferData(GL_DISPATCH_INDIRECT_BUFFER, 
                   (long) commandCount * DISPATCH_INDIRECT_COMMAND_SIZE,
                   GLBufferOpsBase.GL_DYNAMIC_DRAW);
        return buf;
    }
    
    /**
     * Write dispatch command to thread-local buffer.
     * ZERO ALLOCATION.
     */
    public ByteBuffer writeDispatchCommand(int numGroupsX, int numGroupsY, int numGroupsZ) {
        ByteBuffer cmd = TL_DISPATCH.get();
        cmd.clear();
        cmd.putInt(numGroupsX);
        cmd.putInt(numGroupsY);
        cmd.putInt(numGroupsZ);
        cmd.flip();
        return cmd;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // GL 4.3: Memory Barriers
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Issue memory barrier for shader storage buffer operations.
     * Call after compute shader writes before subsequent reads.
     */
    public void shaderStorageBarrier() {
        OpenGLCallMapper.glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    }
    
    /**
     * Issue memory barrier for indirect command buffers.
     * Call after writing indirect commands before draw/dispatch.
     */
    public void commandBarrier() {
        OpenGLCallMapper.glMemoryBarrier(GL_COMMAND_BARRIER_BIT);
    }
    
    /**
     * Issue full memory barrier.
     * Use sparingly - this is expensive!
     */
    public void fullBarrier() {
        OpenGLCallMapper.glMemoryBarrier(GL_ALL_BARRIER_BITS);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Convenience: Optimized streaming with invalidation
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Stream data to buffer with invalidation.
     * Optimal for per-frame updates.
     * 
     * Pattern:
     * 1. Invalidate buffer (tell driver old data is garbage)
     * 2. Upload new data
     * 3. No sync stall because driver knows old data unused
     */
    public void streamToBuffer(int buffer, int target, ByteBuffer data, int usage) {
        invalidateBufferData(buffer);
        bindBuffer(target, buffer);
        bufferData(target, data, usage);
    }
    
    /**
     * Stream partial data with range invalidation.
     */
    public void streamToBufferRange(int buffer, int target, long offset, ByteBuffer data) {
        invalidateBufferSubData(buffer, offset, data.remaining());
        bindBuffer(target, buffer);
        bufferSubData(target, offset, data);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void shutdown() {}
}
