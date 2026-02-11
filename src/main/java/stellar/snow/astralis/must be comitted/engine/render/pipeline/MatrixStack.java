package stellar.snow.astralis.engine.render.pipeline;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.FloatBuffer;

/**
 * MatrixStack - High-performance replacement for OpenGL's fixed-function matrix stack.
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Separate ModelView and Projection stacks (like legacy GL)</li>
 *   <li>JOML-based SIMD-friendly matrix operations</li>
 *   <li>Direct memory upload for Push Constants</li>
 *   <li>Thread-safe per-thread stacks</li>
 * </ul>
 */
public final class MatrixStack {

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    public static final int MAX_MODELVIEW_DEPTH = 32;
    public static final int MAX_PROJECTION_DEPTH = 4;
    public static final int MAX_TEXTURE_DEPTH = 4;

    public static final int MODE_MODELVIEW = 0x1700;
    public static final int MODE_PROJECTION = 0x1701;
    public static final int MODE_TEXTURE = 0x1702;

    // ========================================================================
    // THREAD-LOCAL STACKS
    // ========================================================================

    private static final ThreadLocal<MatrixStack> THREAD_LOCAL = ThreadLocal.withInitial(MatrixStack::new);

    public static MatrixStack get() {
        return THREAD_LOCAL.get();
    }

    // ========================================================================
    // STACK DATA
    // ========================================================================

    private final Matrix4f[] modelViewStack;
    private final Matrix4f[] projectionStack;
    private final Matrix4f[] textureStack;

    private int modelViewPointer = 0;
    private int projectionPointer = 0;
    private int texturePointer = 0;

    private int currentMode = MODE_MODELVIEW;

    // Combined MVP matrix (cached)
    private final Matrix4f mvpMatrix = new Matrix4f();
    private boolean mvpDirty = true;

    // Normal matrix (inverse transpose of modelview 3x3)
    private final Matrix4f normalMatrix = new Matrix4f();
    private boolean normalDirty = true;

    // Upload buffers (off-heap for Vulkan)
    private final Arena arena;
    private final MemorySegment uploadBuffer; // 3 * 16 floats = 192 bytes (MVP + Normal + Texture)
    private final FloatBuffer uploadFloatBuffer;

    // ========================================================================
    // CONSTRUCTION
    // ========================================================================

    public MatrixStack() {
        // Initialize stacks
        this.modelViewStack = new Matrix4f[MAX_MODELVIEW_DEPTH];
        this.projectionStack = new Matrix4f[MAX_PROJECTION_DEPTH];
        this.textureStack = new Matrix4f[MAX_TEXTURE_DEPTH];

        for (int i = 0; i < MAX_MODELVIEW_DEPTH; i++) {
            modelViewStack[i] = new Matrix4f();
        }
        for (int i = 0; i < MAX_PROJECTION_DEPTH; i++) {
            projectionStack[i] = new Matrix4f();
        }
        for (int i = 0; i < MAX_TEXTURE_DEPTH; i++) {
            textureStack[i] = new Matrix4f();
        }

        // Allocate upload buffer
        this.arena = Arena.ofConfined();
        this.uploadBuffer = arena.allocate(192, 16); // 3 matrices, 16-byte aligned
        this.uploadFloatBuffer = uploadBuffer.asByteBuffer().asFloatBuffer();

        // Initialize to identity
        loadIdentity();
    }

    // ========================================================================
    // MODE SELECTION
    // ========================================================================

    public void matrixMode(int mode) {
        this.currentMode = mode;
    }

    public int getMatrixMode() {
        return currentMode;
    }

    // ========================================================================
    // STACK OPERATIONS
    // ========================================================================

    public void pushMatrix() {
        switch (currentMode) {
            case MODE_MODELVIEW -> {
                if (modelViewPointer >= MAX_MODELVIEW_DEPTH - 1) {
                    throw new StackOverflowError("ModelView matrix stack overflow");
                }
                modelViewPointer++;
                modelViewStack[modelViewPointer].set(modelViewStack[modelViewPointer - 1]);
            }
            case MODE_PROJECTION -> {
                if (projectionPointer >= MAX_PROJECTION_DEPTH - 1) {
                    throw new StackOverflowError("Projection matrix stack overflow");
                }
                projectionPointer++;
                projectionStack[projectionPointer].set(projectionStack[projectionPointer - 1]);
            }
            case MODE_TEXTURE -> {
                if (texturePointer >= MAX_TEXTURE_DEPTH - 1) {
                    throw new StackOverflowError("Texture matrix stack overflow");
                }
                texturePointer++;
                textureStack[texturePointer].set(textureStack[texturePointer - 1]);
            }
        }
        invalidateCaches();
    }

    public void popMatrix() {
        switch (currentMode) {
            case MODE_MODELVIEW -> {
                if (modelViewPointer <= 0) {
                    throw new IllegalStateException("ModelView matrix stack underflow");
                }
                modelViewPointer--;
            }
            case MODE_PROJECTION -> {
                if (projectionPointer <= 0) {
                    throw new IllegalStateException("Projection matrix stack underflow");
                }
                projectionPointer--;
            }
            case MODE_TEXTURE -> {
                if (texturePointer <= 0) {
                    throw new IllegalStateException("Texture matrix stack underflow");
                }
                texturePointer--;
            }
        }
        invalidateCaches();
    }

    public void loadIdentity() {
        getCurrentMatrix().identity();
        invalidateCaches();
    }

    public void loadMatrix(Matrix4f matrix) {
        getCurrentMatrix().set(matrix);
        invalidateCaches();
    }

    public void loadMatrix(float[] matrix) {
        getCurrentMatrix().set(matrix);
        invalidateCaches();
    }

    public void multMatrix(Matrix4f matrix) {
        getCurrentMatrix().mul(matrix);
        invalidateCaches();
    }

    public void multMatrix(float[] matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f temp = new Matrix4f().set(matrix);
            getCurrentMatrix().mul(temp);
        }
        invalidateCaches();
    }

    // ========================================================================
    // TRANSFORMATIONS
    // ========================================================================

    public void translate(float x, float y, float z) {
        getCurrentMatrix().translate(x, y, z);
        invalidateCaches();
    }

    public void translate(double x, double y, double z) {
        translate((float) x, (float) y, (float) z);
    }

    public void rotate(float angle, float x, float y, float z) {
        getCurrentMatrix().rotate((float) Math.toRadians(angle), x, y, z);
        invalidateCaches();
    }

    public void rotate(double angle, double x, double y, double z) {
        rotate((float) angle, (float) x, (float) y, (float) z);
    }

    public void scale(float x, float y, float z) {
        getCurrentMatrix().scale(x, y, z);
        invalidateCaches();
    }

    public void scale(double x, double y, double z) {
        scale((float) x, (float) y, (float) z);
    }

    // ========================================================================
    // PROJECTION HELPERS
    // ========================================================================

    public void ortho(float left, float right, float bottom, float top, float zNear, float zFar) {
        getCurrentMatrix().ortho(left, right, bottom, top, zNear, zFar);
        invalidateCaches();
    }

    public void perspective(float fovy, float aspect, float zNear, float zFar) {
        getCurrentMatrix().perspective((float) Math.toRadians(fovy), aspect, zNear, zFar);
        invalidateCaches();
    }

    public void frustum(float left, float right, float bottom, float top, float zNear, float zFar) {
        getCurrentMatrix().frustum(left, right, bottom, top, zNear, zFar);
        invalidateCaches();
    }

    public void lookAt(float eyeX, float eyeY, float eyeZ,
                       float centerX, float centerY, float centerZ,
                       float upX, float upY, float upZ) {
        getCurrentMatrix().lookAt(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        invalidateCaches();
    }

    // ========================================================================
    // MATRIX ACCESS
    // ========================================================================

    public Matrix4f getCurrentMatrix() {
        return switch (currentMode) {
            case MODE_MODELVIEW -> modelViewStack[modelViewPointer];
            case MODE_PROJECTION -> projectionStack[projectionPointer];
            case MODE_TEXTURE -> textureStack[texturePointer];
            default -> modelViewStack[modelViewPointer];
        };
    }

    public Matrix4f getModelViewMatrix() {
        return modelViewStack[modelViewPointer];
    }

    public Matrix4f getProjectionMatrix() {
        return projectionStack[projectionPointer];
    }

    public Matrix4f getTextureMatrix() {
        return textureStack[texturePointer];
    }

    public Matrix4f getMVPMatrix() {
        if (mvpDirty) {
            mvpMatrix.set(projectionStack[projectionPointer]);
            mvpMatrix.mul(modelViewStack[modelViewPointer]);
            mvpDirty = false;
        }
        return mvpMatrix;
    }

    public Matrix4f getNormalMatrix() {
        if (normalDirty) {
            normalMatrix.set(modelViewStack[modelViewPointer]);
            normalMatrix.invert();
            normalMatrix.transpose();
            normalDirty = false;
        }
        return normalMatrix;
    }

    // ========================================================================
    // UPLOAD TO GPU
    // ========================================================================

    /**
     * Uploads all matrices to the internal buffer and returns the segment.
     * Layout: [Projection:64][ModelView:64][Normal:64]
     */
    public MemorySegment getUploadBuffer() {
        // Projection (offset 0)
        projectionStack[projectionPointer].get(uploadFloatBuffer.position(0));

        // ModelView (offset 16 floats = 64 bytes)
        modelViewStack[modelViewPointer].get(uploadFloatBuffer.position(16));

        // Normal (offset 32 floats = 128 bytes)
        getNormalMatrix().get(uploadFloatBuffer.position(32));

        uploadFloatBuffer.position(0);
        return uploadBuffer;
    }

    /**
     * Gets the upload buffer address for direct Vulkan access.
     */
    public long getUploadBufferAddress() {
        return uploadBuffer.address();
    }

    /**
     * Gets the MVP matrix into a provided FloatBuffer.
     */
    public void getMVP(FloatBuffer dest) {
        getMVPMatrix().get(dest);
    }

    /**
     * Gets the ModelView matrix into a provided FloatBuffer.
     */
    public void getModelView(FloatBuffer dest) {
        modelViewStack[modelViewPointer].get(dest);
    }

    /**
     * Gets the Projection matrix into a provided FloatBuffer.
     */
    public void getProjection(FloatBuffer dest) {
        projectionStack[projectionPointer].get(dest);
    }

    // ========================================================================
    // CACHE MANAGEMENT
    // ========================================================================

    private void invalidateCaches() {
        mvpDirty = true;
        normalDirty = true;
    }

    // ========================================================================
    // STACK DEPTH
    // ========================================================================

    public int getModelViewDepth() {
        return modelViewPointer + 1;
    }

    public int getProjectionDepth() {
        return projectionPointer + 1;
    }

    public int getTextureDepth() {
        return texturePointer + 1;
    }

    // ========================================================================
    // RESET
    // ========================================================================

    public void reset() {
        modelViewPointer = 0;
        projectionPointer = 0;
        texturePointer = 0;

        modelViewStack[0].identity();
        projectionStack[0].identity();
        textureStack[0].identity();

        currentMode = MODE_MODELVIEW;
        invalidateCaches();
    }

    // ========================================================================
    // CLEANUP
    // ========================================================================

    public void close() {
        if (arena.scope().isAlive()) {
            arena.close();
        }
    }

    // ========================================================================
    // STATIC CONVENIENCE METHODS
    // ========================================================================

    public static void push() {
        get().pushMatrix();
    }

    public static void pop() {
        get().popMatrix();
    }

    public static void identity() {
        get().loadIdentity();
    }

    public static void mode(int mode) {
        get().matrixMode(mode);
    }

    public static Matrix4f getTop() {
        return get().getCurrentMatrix();
    }

    public static Matrix4f getTopModelView() {
        return get().getModelViewMatrix();
    }

    public static Matrix4f getTopProjection() {
        return get().getProjectionMatrix();
    }
}
