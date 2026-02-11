package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps40 - OpenGL 4.0 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                     TESSELLATION & INDIRECT DRAWING                          ║
 * ║                                                                              ║
 * ║  OpenGL 4.0 (2010) MAJOR FEATURES:                                           ║
 * ║                                                                              ║
 * ║  TESSELLATION:                                                               ║
 * ║  • Tessellation control & evaluation shaders                                 ║
 * ║  • GPU-driven geometry amplification                                         ║
 * ║  • Adaptive level-of-detail                                                  ║
 * ║                                                                              ║
 * ║  INDIRECT DRAWING - THE BIG ONE FOR BUFFERS:                                 ║
 * ║  • glDrawArraysIndirect, glDrawElementsIndirect                              ║
 * ║  • GL_DRAW_INDIRECT_BUFFER target                                            ║
 * ║  • Draw parameters stored IN GPU BUFFERS                                     ║
 * ║  • CPU doesn't need to know draw counts!                                     ║
 * ║                                                                              ║
 * ║  OTHER FEATURES:                                                             ║
 * ║  • Shader subroutines                                                        ║
 * ║  • Transform feedback improvements                                           ║
 * ║  • 64-bit double precision in shaders                                        ║
 * ║  • Cube map arrays                                                           ║
 * ║                                                                              ║
 * ║  PERFORMANCE IMPACT:                                                         ║
 * ║  • Indirect draw = GPU-driven rendering                                      ║
 * ║  • Compute shader (4.3) can write draw commands                              ║
 * ║  • Eliminates CPU readback for culling                                       ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 4.0              │
 * │ Color: #00FFFF (Cyan)                   │
 * └─────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public final class GLBufferOps40 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 40;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL40;
    public static final String VERSION_NAME = "OpenGL 4.0";
    
    // NEW in 4.0: Indirect draw buffer
    public static final int GL_DRAW_INDIRECT_BUFFER = 0x8F3F;
    
    // Existing targets
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_COPY_READ_BUFFER = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    public static final int GL_TRANSFORM_FEEDBACK_BUFFER = 0x8C8E;
    
    // Map flags
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_MAP_WRITE_BIT = 0x0002;
    public static final int GL_MAP_INVALIDATE_RANGE_BIT = 0x0004;
    public static final int GL_MAP_INVALIDATE_BUFFER_BIT = 0x0008;
    public static final int GL_MAP_FLUSH_EXPLICIT_BIT = 0x0010;
    public static final int GL_MAP_UNSYNCHRONIZED_BIT = 0x0020;
    
    // Indirect draw command structure sizes (in bytes)
    public static final int DRAW_ARRAYS_INDIRECT_COMMAND_SIZE = 16;   // 4 uints
    public static final int DRAW_ELEMENTS_INDIRECT_COMMAND_SIZE = 20; // 5 uints
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Thread-local buffers - 4-tier pool
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static final ThreadLocal<IntBuffer> TL_1 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    private static final ThreadLocal<IntBuffer> TL_16 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    private static final ThreadLocal<IntBuffer> TL_64 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(256).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    private static final ThreadLocal<IntBuffer> TL_256 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    // Thread-local for indirect command building
    private static final ThreadLocal<ByteBuffer> TL_INDIRECT_CMD = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(DRAW_ELEMENTS_INDIRECT_COMMAND_SIZE)
                      .order(ByteOrder.nativeOrder()));
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    
    public GLBufferOps40() {
        // Warm up all pools
        TL_1.get();
        TL_16.get();
        TL_64.get();
        TL_256.get();
        TL_INDIRECT_CMD.get();
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
        b.clear();
        b.put(0, buffer);
        OpenGLCallMapper.glDeleteBuffers(b);
    }
    
    @Override
    public void deleteBuffers(int[] buffers) {
        if (buffers == null || buffers.length == 0) return;
        if (buffers.length == 1) { deleteBuffer(buffers[0]); return; }
        
        IntBuffer b = pool(buffers.length);
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
    
    @Override public ByteBuffer mapBuffer(int t, int a) { return OpenGLCallMapper.glMapBuffer(t, a); }
    @Override public ByteBuffer mapBuffer(int t, int a, long l) { return OpenGLCallMapper.glMapBuffer(t, a, l, null); }
    @Override public ByteBuffer mapBufferRange(int t, long o, long l, int a) { return OpenGLCallMapper.glMapBufferRange(t, o, l, a); }
    @Override public void flushMappedBufferRange(int t, long o, long l) { OpenGLCallMapper.glFlushMappedBufferRange(t, o, l); }
    @Override public boolean unmapBuffer(int t) { return OpenGLCallMapper.glUnmapBuffer(t); }
    
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
    // GL 4.0 Convenience: Indirect Draw Command Helpers
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Create an indirect draw buffer for glDrawArraysIndirect.
     * 
     * Command structure (16 bytes):
     *   uint count;        // Vertex count
     *   uint instanceCount;// Instance count
     *   uint first;        // First vertex
     *   uint baseInstance; // Base instance (requires GL 4.2 to work)
     * 
     * @param commandCount Number of draw commands
     * @return Buffer ID ready for GL_DRAW_INDIRECT_BUFFER binding
     */
    public int createDrawArraysIndirectBuffer(int commandCount) {
        int buf = genBuffer();
        bindBuffer(GL_DRAW_INDIRECT_BUFFER, buf);
        bufferData(GL_DRAW_INDIRECT_BUFFER, 
                   (long) commandCount * DRAW_ARRAYS_INDIRECT_COMMAND_SIZE,
                   GLBufferOpsBase.GL_DYNAMIC_DRAW);
        return buf;
    }
    
    /**
     * Create an indirect draw buffer for glDrawElementsIndirect.
     * 
     * Command structure (20 bytes):
     *   uint count;        // Index count
     *   uint instanceCount;// Instance count  
     *   uint firstIndex;   // First index
     *   int  baseVertex;   // Base vertex (signed!)
     *   uint baseInstance; // Base instance
     */
    public int createDrawElementsIndirectBuffer(int commandCount) {
        int buf = genBuffer();
        bindBuffer(GL_DRAW_INDIRECT_BUFFER, buf);
        bufferData(GL_DRAW_INDIRECT_BUFFER,
                   (long) commandCount * DRAW_ELEMENTS_INDIRECT_COMMAND_SIZE,
                   GLBufferOpsBase.GL_DYNAMIC_DRAW);
        return buf;
    }
    
    /**
     * Write a single DrawArraysIndirect command to the thread-local buffer.
     * Returns a ByteBuffer ready for upload via bufferSubData.
     * ZERO ALLOCATION.
     */
    public ByteBuffer writeDrawArraysCommand(int vertexCount, int instanceCount, 
                                              int firstVertex, int baseInstance) {
        ByteBuffer cmd = TL_INDIRECT_CMD.get();
        cmd.clear();
        cmd.putInt(vertexCount);
        cmd.putInt(instanceCount);
        cmd.putInt(firstVertex);
        cmd.putInt(baseInstance);
        cmd.flip();
        return cmd;
    }
    
    /**
     * Write a single DrawElementsIndirect command to thread-local buffer.
     */
    public ByteBuffer writeDrawElementsCommand(int indexCount, int instanceCount,
                                                int firstIndex, int baseVertex,
                                                int baseInstance) {
        ByteBuffer cmd = TL_INDIRECT_CMD.get();
        cmd.clear();
        cmd.putInt(indexCount);
        cmd.putInt(instanceCount);
        cmd.putInt(firstIndex);
        cmd.putInt(baseVertex);
        cmd.putInt(baseInstance);
        cmd.flip();
        return cmd;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void shutdown() {}
}
