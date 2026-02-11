package stellar.snow.astralis.api.opengl.buffer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps11 - OpenGL 1.1 Pipeline
 * 
 * OpenGL 1.1 (1997): Texture objects, polygon offset, vertex arrays (client-side).
 * Still NO Vertex Buffer Objects.
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 1.1 (No VBO)     │
 * │ Color: #666666 (Gray)                   │
 * └─────────────────────────────────────────┘
 */
public final class GLBufferOps11 implements BufferOps {

    public static final int VERSION_CODE = 11;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL11;
    public static final String VERSION_NAME = "OpenGL 1.1";
    
    private static final String ERR = "[" + GLBufferOpsBase.RENDER_NAME + "] GL 1.1 has no VBO support. Need GL 1.5+";
    
    public GLBufferOps11() {
        System.err.println("[" + GLBufferOpsBase.DEBUG_HEADER + "] WARNING: GL 1.1 - No VBO support");
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
