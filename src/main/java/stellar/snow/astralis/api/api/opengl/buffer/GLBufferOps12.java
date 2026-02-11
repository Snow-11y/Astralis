package stellar.snow.astralis.api.opengl.buffer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps12 - OpenGL 1.2 Pipeline
 * 
 * OpenGL 1.2 (1998): 3D textures, BGRA format, packed pixels.
 * Still NO Vertex Buffer Objects.
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 1.2 (No VBO)     │
 * │ Color: #777777 (Light Gray)             │
 * └─────────────────────────────────────────┘
 */
public final class GLBufferOps12 implements BufferOps {

    public static final int VERSION_CODE = 12;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL12;
    public static final String VERSION_NAME = "OpenGL 1.2";
    
    private static final String ERR = "[" + GLBufferOpsBase.RENDER_NAME + "] GL 1.2 has no VBO. Need 1.5+";
    
    public GLBufferOps12() {
        System.err.println("[" + GLBufferOpsBase.DEBUG_HEADER + "] WARNING: GL 1.2 - No VBO");
    }
    
    @Override public int getVersionCode() { return VERSION_CODE; }
    @Override public int getDisplayColor() { return DISPLAY_COLOR; }
    @Override public String getVersionName() { return VERSION_NAME; }
    @Override public boolean supportsVBO() { return false; }
    
    @Override public int genBuffer() { throw new UnsupportedOperationException(ERR); }
    @Override public int[] genBuffers(int c) { throw new UnsupportedOperationException(ERR); }
    @Override public void deleteBuffer(int b) { throw new UnsupportedOperationException(ERR); }
    @Override public void deleteBuffers(int[] b) { throw new UnsupportedOperationException(ERR); }
    @Override public void bindBuffer(int t, int b) { throw new UnsupportedOperationException(ERR); }
    @Override public void bufferData(int t, long s, int u) { throw new UnsupportedOperationException(ERR); }
    @Override public void bufferData(int t, ByteBuffer d, int u) { throw new UnsupportedOperationException(ERR); }
    @Override public void bufferData(int t, FloatBuffer d, int u) { throw new UnsupportedOperationException(ERR); }
    @Override public void bufferData(int t, IntBuffer d, int u) { throw new UnsupportedOperationException(ERR); }
    @Override public void bufferSubData(int t, long o, ByteBuffer d) { throw new UnsupportedOperationException(ERR); }
    @Override public void bufferSubData(int t, long o, FloatBuffer d) { throw new UnsupportedOperationException(ERR); }
    @Override public void bufferSubData(int t, long o, IntBuffer d) { throw new UnsupportedOperationException(ERR); }
    @Override public ByteBuffer mapBuffer(int t, int a) { throw new UnsupportedOperationException(ERR); }
    @Override public ByteBuffer mapBuffer(int t, int a, long l) { throw new UnsupportedOperationException(ERR); }
    @Override public boolean unmapBuffer(int t) { throw new UnsupportedOperationException(ERR); }
    @Override public void shutdown() {}
}
