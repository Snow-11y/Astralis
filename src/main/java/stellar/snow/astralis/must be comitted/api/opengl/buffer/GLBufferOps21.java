package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps21 - OpenGL 2.1 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║              ★★★ MINECRAFT 1.12.2 MINIMUM REQUIREMENT ★★★                   ║
 * ║                                                                              ║
 * ║  OpenGL 2.1 (2006) Features:                                                 ║
 * ║  • GLSL 1.20                                                                 ║
 * ║  • Non-square matrices in shaders                                            ║
 * ║  • sRGB textures                                                             ║
 * ║  • Pixel buffer objects (PBOs) - async texture transfers                     ║
 * ║                                                                              ║
 * ║  Buffer Operations: Same as GL 2.0                                           ║
 * ║                                                                              ║
 * ║  This is THE critical pipeline for MC 1.12.2 compatibility.                  ║
 * ║  Every optimization here directly impacts the majority of players.           ║
 * ║                                                                              ║
 * ║  EXTREME OPTIMIZATIONS:                                                      ║
 * ║  • Inlined hot paths                                                         ║
 * ║  • Zero-allocation buffer reuse                                              ║
 * ║  • Minimal branching                                                         ║
 * ║  • Cache-friendly access patterns                                            ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 2.1 ★            │
 * │ Color: #FFD700 (Gold) - MC Baseline     │
 * └─────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public final class GLBufferOps21 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 21;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL21;
    public static final String VERSION_NAME = "OpenGL 2.1";
    
    // PBO targets (new in 2.1 era via ARB)
    public static final int GL_PIXEL_PACK_BUFFER = 0x88EB;
    public static final int GL_PIXEL_UNPACK_BUFFER = 0x88EC;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Pre-allocated scratch space - ZERO per-call allocation
    // ─────────────────────────────────────────────────────────────────────────────
    
    // We inline the thread-local access for maximum speed in this critical pipeline
    private static final ThreadLocal<IntBuffer> TL_SINGLE = ThreadLocal.withInitial(() ->
            GLBufferOpsBase.createDirectBuffer(4).asIntBuffer());
    
    private static final ThreadLocal<IntBuffer> TL_BATCH_16 = ThreadLocal.withInitial(() ->
            GLBufferOpsBase.createDirectBuffer(64).asIntBuffer());
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    
    public GLBufferOps21() {
        // Eager warmup - this is THE most used pipeline
        TL_SINGLE.get();
        TL_BATCH_16.get();
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Version Info
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override public int getVersionCode() { return VERSION_CODE; }
    @Override public int getDisplayColor() { return DISPLAY_COLOR; }
    @Override public String getVersionName() { return VERSION_NAME; }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Generation - HYPER-OPTIMIZED
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int genBuffer() {
        // CRITICAL HOT PATH - every frame, multiple times
        IntBuffer b = TL_SINGLE.get();
        b.clear();
        OpenGLCallMapper.glGenBuffers(b);
        return b.get(0);
    }
    
    @Override
    public int[] genBuffers(int count) {
        // Handle edge cases first (predictable branches)
        if (count <= 0) return new int[0];
        if (count == 1) return new int[] { genBuffer() };
        
        // Fast path for small batches (most common)
        if (count <= 16) {
            IntBuffer b = TL_BATCH_16.get();
            b.clear();
            b.limit(count);
            OpenGLCallMapper.glGenBuffers(b);
            int[] r = new int[count];
            b.rewind();
            b.get(r);
            return r;
        }
        
        // Large batch (rare) - use shared pool
        IntBuffer b = GLBufferOpsBase.getBatchBuffer(count);
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
        if (buffer == 0) return; // GL silently ignores 0
        
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
        
        // Fast path for small batches
        if (len <= 16) {
            IntBuffer b = TL_BATCH_16.get();
            b.clear();
            b.put(buffers, 0, len);
            b.flip();
            OpenGLCallMapper.glDeleteBuffers(b);
            return;
        }
        
        // Large batch
        IntBuffer b = GLBufferOpsBase.toDirectBuffer(buffers);
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
    // Data Upload - All paths inline for speed
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
        // ThreadLocal cleanup happens automatically via GC
        // No native resources to explicitly free
    }
}
