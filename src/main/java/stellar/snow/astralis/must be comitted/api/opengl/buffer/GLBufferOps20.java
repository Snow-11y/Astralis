package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps20 - OpenGL 2.0 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                         GLSL SHADERS BECOME CORE                             ║
 * ║                                                                              ║
 * ║  OpenGL 2.0 (2004) Major Features:                                           ║
 * ║  • GLSL 1.10 shaders (vertex & fragment)                                     ║
 * ║  • Multiple render targets                                                   ║
 * ║  • Point sprites                                                             ║
 * ║  • Two-sided stencil                                                         ║
 * ║                                                                              ║
 * ║  Buffer Operations: Same as GL 1.5                                           ║
 * ║  - VBOs fully supported                                                      ║
 * ║  - No mapBufferRange yet (that's GL 3.0)                                     ║
 * ║                                                                              ║
 * ║  Optimizations:                                                              ║
 * ║  • Thread-local scratch buffers (zero allocation)                            ║
 * ║  • Pooled batch buffers                                                      ║
 * ║  • Inline null checks                                                        ║
 * ║  • Branch prediction hints via ordering                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 2.0              │
 * │ Color: #B87333 (Copper)                 │
 * └─────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public final class GLBufferOps20 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 20;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL20;
    public static final String VERSION_NAME = "OpenGL 2.0";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    
    public GLBufferOps20() {
        // Warm up thread-local buffers immediately
        GLBufferOpsBase.getSingleIntBuffer();
        GLBufferOpsBase.getBatchBuffer(16);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Version Info
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override public int getVersionCode() { return VERSION_CODE; }
    @Override public int getDisplayColor() { return DISPLAY_COLOR; }
    @Override public String getVersionName() { return VERSION_NAME; }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Generation - ZERO ALLOCATION
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int genBuffer() {
        // Hot path: single int buffer from thread-local pool
        IntBuffer buf = GLBufferOpsBase.getSingleIntBuffer();
        OpenGLCallMapper.glGenBuffers(buf);
        return buf.get(0);
    }
    
    @Override
    public int[] genBuffers(int count) {
        // Fast path for common cases
        if (count <= 0) return new int[0];
        if (count == 1) return new int[] { genBuffer() };
        
        // Use pooled buffer - zero allocation for count <= 256
        IntBuffer buf = GLBufferOpsBase.getBatchBuffer(count);
        OpenGLCallMapper.glGenBuffers(buf);
        
        // Result array allocation is unavoidable
        int[] result = new int[count];
        buf.rewind();
        buf.get(result);
        return result;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Deletion - ZERO ALLOCATION
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void deleteBuffer(int buffer) {
        // GL spec: 0 is silently ignored - fast exit
        if (buffer == 0) return;
        
        IntBuffer buf = GLBufferOpsBase.getSingleIntBuffer();
        buf.put(0, buffer);
        OpenGLCallMapper.glDeleteBuffers(buf);
    }
    
    @Override
    public void deleteBuffers(int[] buffers) {
        // Null/empty check first
        if (buffers == null || buffers.length == 0) return;
        
        // Single delete optimization
        if (buffers.length == 1) {
            deleteBuffer(buffers[0]);
            return;
        }
        
        // Batch delete
        IntBuffer buf = GLBufferOpsBase.toDirectBuffer(buffers);
        OpenGLCallMapper.glDeleteBuffers(buf);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Binding - Direct passthrough
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
    // Mapping - GL 1.5 style (whole buffer)
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
        // Thread-local cleanup handled by GC
    }
}
