package stellar.snow.astralis.api.opengl.buffer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * GLBufferOps121 - OpenGL 1.2.1 Pipeline
 * 
 * OpenGL 1.2.1: Adds ARB_imaging subset to 1.2.
 * Used as fallback for GL 1.3/1.4 which have multitexture but no VBOs.
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 1.2.1 (No VBO)   │
 * │ Color: #888866 (Tan)                    │
 * └─────────────────────────────────────────┘
 */
public final class GLBufferOps121 implements BufferOps {

    public static final int VERSION_CODE = 121;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL121;
    public static final String VERSION_NAME = "OpenGL 1.2.1";
    
    private static final String ERR = "[" + GLBufferOpsBase.RENDER_NAME + "] GL 1.2.1/1.3/1.4 lacks VBO. Need 1.5+";
    
    public GLBufferOps121() {
        System.err.println("[" + GLBufferOpsBase.DEBUG_HEADER + "] WARNING: GL 1.2.1 fallback - No VBO");
    }
    
    @Override public int getVersionCode() { return VERSION_CODE; }
    @Override public int getDisplayColor() { return DISPLAY_COLOR; }
    @Override public String getVersionName() { return VERSION_NAME; }
    @Override public String getShortName() { return "1.2.1"; }
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
