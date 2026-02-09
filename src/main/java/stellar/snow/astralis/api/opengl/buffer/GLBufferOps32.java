package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps32 - OpenGL 3.2 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                      GEOMETRY SHADERS & CORE PROFILE                         ║
 * ║                                                                              ║
 * ║  OpenGL 3.2 (2009) Features:                                                 ║
 * ║  • Geometry shaders (core)                                                   ║
 * ║  • Core/Compatibility profile split                                          ║
 * ║  • Sync objects (fences)                                                     ║
 * ║  • GLSL 1.50                                                                 ║
 * ║  • Depth clamp                                                               ║
 * ║                                                                              ║
 * ║  Buffer operations: Same as 3.1 (copy buffer available)                      ║
 * ║                                                                              ║
 * ║  Note: Geometry shaders don't affect buffer operations directly,             ║
 * ║  but sync objects can be used for advanced buffer streaming patterns.        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 3.2              │
 * │ Color: #2E8B57 (Sea Green)              │
 * └─────────────────────────────────────────┘
 */
public final class GLBufferOps32 implements BufferOps {

    public static final int VERSION_CODE = 32;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL32;
    public static final String VERSION_NAME = "OpenGL 3.2";
    
    public static final int GL_COPY_READ_BUFFER = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    
    private static final ThreadLocal<IntBuffer> TL_SINGLE = ThreadLocal.withInitial(() ->
            GLBufferOpsBase.createDirectBuffer(4).asIntBuffer());
    private static final ThreadLocal<IntBuffer> TL_BATCH = ThreadLocal.withInitial(() ->
            GLBufferOpsBase.createDirectBuffer(256).asIntBuffer());
    
    public GLBufferOps32() { TL_SINGLE.get(); TL_BATCH.get(); }
    
    @Override public int getVersionCode() { return VERSION_CODE; }
    @Override public int getDisplayColor() { return DISPLAY_COLOR; }
    @Override public String getVersionName() { return VERSION_NAME; }
    @Override public boolean supportsMapBufferRange() { return true; }
    @Override public boolean supportsCopyBuffer() { return true; }
    
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
    
    @Override
    public void deleteBuffer(int buffer) {
        if (buffer == 0) return;
        IntBuffer b = TL_SINGLE.get();
        b.clear().put(0, buffer);
        OpenGLCallMapper.glDeleteBuffers(b);
    }
    
    @Override
    public void deleteBuffers(int[] buffers) {
        if (buffers == null || buffers.length == 0) return;
        if (buffers.length == 1) { deleteBuffer(buffers[0]); return; }
        
        IntBuffer b = (buffers.length <= 64) ? TL_BATCH.get() : GLBufferOpsBase.getBatchBuffer(buffers.length);
        b.clear().put(buffers).flip();
        OpenGLCallMapper.glDeleteBuffers(b);
    }
    
    @Override public void bindBuffer(int t, int buf) { OpenGLCallMapper.glBindBuffer(t, buf); }
    
    @Override public void bufferData(int t, long s, int u) { OpenGLCallMapper.glBufferData(t, s, u); }
    @Override public void bufferData(int t, ByteBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    @Override public void bufferData(int t, FloatBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    @Override public void bufferData(int t, IntBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    
    @Override public void bufferSubData(int t, long o, ByteBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    @Override public void bufferSubData(int t, long o, FloatBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    @Override public void bufferSubData(int t, long o, IntBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    
    @Override public ByteBuffer mapBuffer(int t, int a) { return OpenGLCallMapper.glMapBuffer(t, a); }
    @Override public ByteBuffer mapBuffer(int t, int a, long l) { return OpenGLCallMapper.glMapBuffer(t, a, l, null); }
    @Override public ByteBuffer mapBufferRange(int t, long o, long l, int a) { return OpenGLCallMapper.glMapBufferRange(t, o, l, a); }
    @Override public void flushMappedBufferRange(int t, long o, long l) { OpenGLCallMapper.glFlushMappedBufferRange(t, o, l); }
    @Override public boolean unmapBuffer(int t) { return OpenGLCallMapper.glUnmapBuffer(t); }
    
    @Override
    public void copyBufferSubData(int rt, int wt, long ro, long wo, long sz) {
        OpenGLCallMapper.glCopyBufferSubData(rt, wt, ro, wo, sz);
    }
    
    @Override public int getBufferParameteri(int t, int p) { return OpenGLCallMapper.glGetBufferParameteri(t, p); }
    @Override public void shutdown() {}
}
