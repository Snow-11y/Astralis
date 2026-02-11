package stellar.snow.astralis.api.common;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * GLBufferOpsBase - Shared utilities for all buffer operations pipelines.
 * 
 * Contains:
 * - Zero-allocation buffer pools
 * - Thread-local scratch buffers
 * - Memory management utilities
 * - Common constants
 * 
 * Part of: Astralis / Snowium Render System
 */
public final class GLBufferOpsBase {
    
    private GLBufferOpsBase() {} // No instantiation
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Snowium Branding Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final String MOD_NAME = "Astralis";
    public static final String MOD_SHORT = "FF";
    public static final String RENDER_NAME = "Snowium";
    public static final String DEBUG_HEADER = "Snowium Render";
    
    // Snowium brand color: Icy cyan-white glow
    public static final int SNOWIUM_COLOR = 0x7DF9FF; // Electric Ice
    public static final int SNOWIUM_GLOW_COLOR = 0xB0FFFF; // Bright glow
    public static final int ASTRALIS_COLOR = 0xFF6B35; // Energetic orange
    
    // ─────────────────────────────────────────────────────────────────────────────
    // GL Constants (no LWJGL dependency)
    // ─────────────────────────────────────────────────────────────────────────────
    
    // Buffer targets
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_COPY_READ_BUFFER = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    public static final int GL_DRAW_INDIRECT_BUFFER = 0x8F3F;
    
    // Usage hints
    public static final int GL_STATIC_DRAW = 0x88E4;
    public static final int GL_DYNAMIC_DRAW = 0x88E8;
    public static final int GL_STREAM_DRAW = 0x88E0;
    
    // Access modes
    public static final int GL_READ_ONLY = 0x88B8;
    public static final int GL_WRITE_ONLY = 0x88B9;
    public static final int GL_READ_WRITE = 0x88BA;
    
    // Map access bits (GL 3.0+)
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_MAP_WRITE_BIT = 0x0002;
    public static final int GL_MAP_INVALIDATE_RANGE_BIT = 0x0004;
    public static final int GL_MAP_INVALIDATE_BUFFER_BIT = 0x0008;
    public static final int GL_MAP_FLUSH_EXPLICIT_BIT = 0x0010;
    public static final int GL_MAP_UNSYNCHRONIZED_BIT = 0x0020;
    public static final int GL_MAP_PERSISTENT_BIT = 0x0040;
    public static final int GL_MAP_COHERENT_BIT = 0x0080;
    
    // Storage flags (GL 4.4+)
    public static final int GL_DYNAMIC_STORAGE_BIT = 0x0100;
    public static final int GL_CLIENT_STORAGE_BIT = 0x0200;
    
    // Buffer parameters
    public static final int GL_BUFFER_SIZE = 0x8764;
    public static final int GL_BUFFER_USAGE = 0x8765;
    public static final int GL_BUFFER_MAPPED = 0x88BC;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Thread-Local Scratch Buffers (ZERO ALLOCATION after warmup)
    // ─────────────────────────────────────────────────────────────────────────────
    
    // Single int buffer for gen/delete single operations
    private static final ThreadLocal<IntBuffer> TL_SINGLE_INT = ThreadLocal.withInitial(() -> {
        ByteBuffer bb = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        return bb.asIntBuffer();
    });
    
    // Small batch buffer (16 ints = 64 bytes) for common batch sizes
    private static final ThreadLocal<IntBuffer> TL_SMALL_BATCH = ThreadLocal.withInitial(() -> {
        ByteBuffer bb = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
        return bb.asIntBuffer();
    });
    
    // Medium batch buffer (256 ints = 1KB) for larger batches
    private static final ThreadLocal<IntBuffer> TL_MEDIUM_BATCH = ThreadLocal.withInitial(() -> {
        ByteBuffer bb = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder());
        return bb.asIntBuffer();
    });
    
    /**
     * Get thread-local single int buffer. 
     * ALWAYS call clear() before use.
     * NEVER store reference - use immediately.
     */
    public static IntBuffer getSingleIntBuffer() {
        IntBuffer buf = TL_SINGLE_INT.get();
        buf.clear();
        return buf;
    }
    
    /**
     * Get optimal batch buffer for count.
     * Returns thread-local buffer if count fits, otherwise allocates.
     * Caller MUST NOT store reference to returned buffer.
     */
    public static IntBuffer getBatchBuffer(int count) {
        if (count <= 0) return getSingleIntBuffer();
        
        if (count == 1) {
            return getSingleIntBuffer();
        }
        
        if (count <= 16) {
            IntBuffer buf = TL_SMALL_BATCH.get();
            buf.clear();
            buf.limit(count);
            return buf;
        }
        
        if (count <= 256) {
            IntBuffer buf = TL_MEDIUM_BATCH.get();
            buf.clear();
            buf.limit(count);
            return buf;
        }
        
        // Large batch: must allocate (rare case)
        ByteBuffer bb = ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder());
        return bb.asIntBuffer();
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Direct ByteBuffer Pool (for mapping operations)
    // ─────────────────────────────────────────────────────────────────────────────
    
    // Common sizes for streaming buffers
    private static final int[] POOL_SIZES = {
        4 * 1024,      // 4KB
        16 * 1024,     // 16KB
        64 * 1024,     // 64KB
        256 * 1024,    // 256KB
        1024 * 1024    // 1MB
    };
    
    @SuppressWarnings("unchecked")
    private static final ThreadLocal<ByteBuffer>[] TL_POOLS = new ThreadLocal[POOL_SIZES.length];
    
    static {
        for (int i = 0; i < POOL_SIZES.length; i++) {
            final int size = POOL_SIZES[i];
            TL_POOLS[i] = ThreadLocal.withInitial(() -> 
                ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
            );
        }
    }
    
    /**
     * Get a pooled direct ByteBuffer of at least the requested size.
     * Returns null if size is too large (caller should allocate directly).
     */
    public static ByteBuffer getPooledBuffer(int minSize) {
        for (int i = 0; i < POOL_SIZES.length; i++) {
            if (POOL_SIZES[i] >= minSize) {
                ByteBuffer buf = TL_POOLS[i].get();
                buf.clear();
                buf.limit(minSize);
                return buf;
            }
        }
        return null; // Too large, caller allocates
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Utility Methods
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Convert int array to direct IntBuffer (for batch operations).
     * Uses pooled buffer if possible.
     */
    public static IntBuffer toDirectBuffer(int[] values) {
        if (values == null || values.length == 0) return getSingleIntBuffer();
        
        IntBuffer buf = getBatchBuffer(values.length);
        buf.put(values);
        buf.flip();
        return buf;
    }
    
    /**
     * Extract int array from IntBuffer.
     */
    public static int[] toArray(IntBuffer buf, int count) {
        int[] result = new int[count];
        buf.get(result);
        return result;
    }
    
    /**
     * Safe direct buffer creation with native byte order.
     */
    public static ByteBuffer createDirectBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }
    
    /**
     * Validate buffer is direct and has native order.
     * Returns false if buffer is unsuitable for GL operations.
     */
    public static boolean isValidGLBuffer(ByteBuffer buf) {
        return buf != null && buf.isDirect() && buf.order() == ByteOrder.nativeOrder();
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // F3 Display Color Palette
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int COLOR_GL10 = 0x555555;  // Dark Gray - No VBO
    public static final int COLOR_GL11 = 0x666666;  // Gray - No VBO
    public static final int COLOR_GL12 = 0x777777;  // Light Gray - No VBO
    public static final int COLOR_GL121 = 0x888866; // Tan - No VBO
    public static final int COLOR_GL15 = 0xCD7F32;  // Bronze - First VBO
    public static final int COLOR_GL20 = 0xB87333;  // Copper - GLSL
    public static final int COLOR_GL21 = 0xFFD700;  // Gold - MC Minimum
    public static final int COLOR_GL30 = 0x32CD32;  // Lime - Modern Era
    public static final int COLOR_GL31 = 0x3CB371;  // Medium Sea Green
    public static final int COLOR_GL32 = 0x2E8B57;  // Sea Green
    public static final int COLOR_GL33 = 0x00FF7F;  // Spring Green - Sweet Spot
    public static final int COLOR_GL40 = 0x00FFFF;  // Cyan - Tessellation
    public static final int COLOR_GL41 = 0x00CED1;  // Dark Turquoise
    public static final int COLOR_GL42 = 0x00BFFF;  // Deep Sky Blue - Atomics
    public static final int COLOR_GL43 = 0x1E90FF;  // Dodger Blue - Compute
    public static final int COLOR_GL44 = 0x4169E1;  // Royal Blue - Persistent
    public static final int COLOR_GL45 = 0x8A2BE2;  // Blue Violet - DSA
    public static final int COLOR_GL46 = 0xFF00FF;  // Magenta - MAXIMUM
    
    /**
     * Get display color for GL version code.
     */
    public static int getColorForVersion(int versionCode) {
        switch (versionCode) {
            case 10: return COLOR_GL10;
            case 11: return COLOR_GL11;
            case 12: return COLOR_GL12;
            case 121: return COLOR_GL121;
            case 15: return COLOR_GL15;
            case 20: return COLOR_GL20;
            case 21: return COLOR_GL21;
            case 30: return COLOR_GL30;
            case 31: return COLOR_GL31;
            case 32: return COLOR_GL32;
            case 33: return COLOR_GL33;
            case 40: return COLOR_GL40;
            case 41: return COLOR_GL41;
            case 42: return COLOR_GL42;
            case 43: return COLOR_GL43;
            case 44: return COLOR_GL44;
            case 45: return COLOR_GL45;
            case 46: return COLOR_GL46;
            default: return 0xFFFFFF;
        }
    }
}
