package stellar.snow.astralis.api.opengl.buffer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;


import net.minecraft.client.Minecraft;/**
 * GLBufferOps10 - OpenGL 1.0 Pipeline
 * 
 * OpenGL 1.0 predates VBOs by many years.
 * All buffer operations throw UnsupportedOperationException.
 * 
 * This pipeline exists for:
 * - Proper error messaging
 * - Complete version coverage
 * - Graceful degradation detection
 * 
 * Hardware running GL 1.0 is 25+ years old and CANNOT run Minecraft.
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 1.0 (No VBO)     │
 * │ Color: #555555 (Dark Gray)              │
 * └─────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public final class GLBufferOps10 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 10;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL10;
    public static final String VERSION_NAME = "OpenGL 1.0";
    
    private static final String ERR_NO_VBO = 
        "[" + GLBufferOpsBase.RENDER_NAME + "] FATAL: OpenGL 1.0 does not support Vertex Buffer Objects.\n" +
        "Minimum requirement: OpenGL 1.5 (released 2003).\n" +
        "Please update your graphics drivers or use newer hardware.";
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    
    public GLBufferOps10() {
        System.err.println("[" + GLBufferOpsBase.DEBUG_HEADER + "] WARNING: GL 1.0 selected - NO VBO SUPPORT!");
        System.err.println("[" + GLBufferOpsBase.DEBUG_HEADER + "] This indicates extremely old/broken graphics drivers.");
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Version Info
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override public int getVersionCode() { return VERSION_CODE; }
    @Override public int getDisplayColor() { return DISPLAY_COLOR; }
    @Override public String getVersionName() { return VERSION_NAME; }
    @Override public boolean supportsVBO() { return false; }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // All operations throw - NO VBO SUPPORT
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override public int genBuffer() { throw new UnsupportedOperationException(ERR_NO_VBO); }
    @Override public int[] genBuffers(int count) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    @Override public void deleteBuffer(int buffer) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    @Override public void deleteBuffers(int[] buffers) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    @Override public void bindBuffer(int target, int buffer) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    
    @Override public void bufferData(int target, long size, int usage) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    @Override public void bufferData(int target, ByteBuffer data, int usage) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    @Override public void bufferData(int target, FloatBuffer data, int usage) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    @Override public void bufferData(int target, IntBuffer data, int usage) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    
    @Override public void bufferSubData(int target, long offset, ByteBuffer data) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    @Override public void bufferSubData(int target, long offset, FloatBuffer data) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    @Override public void bufferSubData(int target, long offset, IntBuffer data) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    
    @Override public ByteBuffer mapBuffer(int target, int access) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    @Override public ByteBuffer mapBuffer(int target, int access, long length) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    @Override public boolean unmapBuffer(int target) { throw new UnsupportedOperationException(ERR_NO_VBO); }
    
    @Override public void shutdown() { /* Nothing to clean */ }
}
