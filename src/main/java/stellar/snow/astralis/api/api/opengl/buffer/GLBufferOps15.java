package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps15 - OpenGL 1.5 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                    FIRST VERSION WITH VBO SUPPORT!                          ║
 * ║                                                                              ║
 * ║  Core Features:                                                              ║
 * ║  • glGenBuffers, glDeleteBuffers                                             ║
 * ║  • glBindBuffer                                                              ║
 * ║  • glBufferData, glBufferSubData                                             ║
 * ║  • glMapBuffer, glUnmapBuffer                                                ║
 * ║  • glGetBufferParameteriv                                                    ║
 * ║                                                                              ║
 * ║  Optimizations:                                                              ║
 * ║  • Thread-local IntBuffer for zero-allocation gen/delete                     ║
 * ║  • Pooled buffers for batch operations                                       ║
 * ║  • Direct mapper calls (no abstraction overhead)                             ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 1.5              │
 * │ Color: #CD7F32 (Bronze)                 │
 * └─────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public final class GLBufferOps15 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 15;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL15;
    public static final String VERSION_NAME = "OpenGL 1.5";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────────────────────────────────────
    
    private volatile boolean shutdown = false;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    
    public GLBufferOps15() {
        // Warm up thread-local buffers on render thread
        GLBufferOpsBase.getSingleIntBuffer();
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Version Info
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override public int getVersionCode() { return VERSION_CODE; }
    @Override public int getDisplayColor() { return DISPLAY_COLOR; }
    @Override public String getVersionName() { return VERSION_NAME; }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Generation - ZERO ALLOCATION for single buffer
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int genBuffer() {
        IntBuffer buf = GLBufferOpsBase.getSingleIntBuffer();
        OpenGLCallMapper.glGenBuffers(buf);
        return buf.get(0);
    }
    
    @Override
    public int[] genBuffers(int count) {
        if (count <= 0) return new int[0];
        if (count == 1) return new int[] { genBuffer() };
        
        // Use pooled buffer if possible
        IntBuffer buf = GLBufferOpsBase.getBatchBuffer(count);
        OpenGLCallMapper.glGenBuffers(buf);
        return GLBufferOpsBase.toArray(buf, count);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Deletion - ZERO ALLOCATION for single buffer
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void deleteBuffer(int buffer) {
        if (buffer == 0) return; // GL spec: 0 is silently ignored
        IntBuffer buf = GLBufferOpsBase.getSingleIntBuffer();
        buf.put(0, buffer);
        OpenGLCallMapper.glDeleteBuffers(buf);
    }
    
    @Override
    public void deleteBuffers(int[] buffers) {
        if (buffers == null || buffers.length == 0) return;
        if (buffers.length == 1) {
            deleteBuffer(buffers[0]);
            return;
        }
        
        IntBuffer buf = GLBufferOpsBase.toDirectBuffer(buffers);
        OpenGLCallMapper.glDeleteBuffers(buf);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Binding - Direct pass-through (caching done by manager)
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void bindBuffer(int target, int buffer) {
        OpenGLCallMapper.glBindBuffer(target, buffer);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Data Upload - All variations
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
    // Mapping - GL 1.5 style (whole buffer only)
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public ByteBuffer mapBuffer(int target, int access) {
        return OpenGLCallMapper.glMapBuffer(target, access);
    }
    
    @Override
    public ByteBuffer mapBuffer(int target, int access, long length) {
        // GL 1.5 LWJGL2 might need length hint, LWJGL3 signature varies
        return OpenGLCallMapper.glMapBuffer(target, access, length, null);
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
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void shutdown() {
        shutdown = true;
        // ThreadLocal buffers are cleaned by GC when thread dies
        // No explicit cleanup needed for pooled direct buffers
    }
}
