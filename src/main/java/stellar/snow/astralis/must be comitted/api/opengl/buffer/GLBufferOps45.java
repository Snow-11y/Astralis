package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GLBufferOps45 - OpenGL 4.5 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║     ★★★★★★★ DIRECT STATE ACCESS - THE BINDING-FREE REVOLUTION ★★★★★★★      ║
 * ║                                                                              ║
 * ║  OpenGL 4.5 (2014) - DSA TRANSFORMS EVERYTHING:                              ║
 * ║                                                                              ║
 * ║  ┌────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │                         OLD WAY (BIND-TO-EDIT)                         │  ║
 * ║  │                                                                        │  ║
 * ║  │  glGenBuffers(&buf);           // Generate name                        │  ║
 * ║  │  glBindBuffer(GL_ARRAY, buf);  // Bind to edit                         │  ║
 * ║  │  glBufferData(GL_ARRAY, ...);  // Upload (uses bound buffer)           │  ║
 * ║  │  glBindBuffer(GL_ARRAY, 0);    // Unbind (good practice)               │  ║
 * ║  │                                                                        │  ║
 * ║  │  • Hidden global state mutations                                       │  ║
 * ║  │  • Easy to corrupt bindings                                            │  ║
 * ║  │  • Driver must track current bindings                                  │  ║
 * ║  └────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                              ║
 * ║  ┌────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │                         NEW WAY (DSA)                                  │  ║
 * ║  │                                                                        │  ║
 * ║  │  glCreateBuffers(1, &buf);         // Create AND initialize            │  ║
 * ║  │  glNamedBufferStorage(buf, ...);   // Upload directly by name          │  ║
 * ║  │                                                                        │  ║
 * ║  │  • NO binding required for setup                                       │  ║
 * ║  │  • NO hidden state changes                                             │  ║
 * ║  │  • Clearer intent                                                      │  ║
 * ║  │  • Better driver optimization                                          │  ║
 * ║  │  • Thread-friendlier                                                   │  ║
 * ║  └────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                              ║
 * ║  DSA FUNCTIONS FOR BUFFERS:                                                  ║
 * ║  • glCreateBuffers         (replaces glGenBuffers + implicit init)           ║
 * ║  • glNamedBufferStorage    (replaces bind + glBufferStorage)                 ║
 * ║  • glNamedBufferData       (replaces bind + glBufferData)                    ║
 * ║  • glNamedBufferSubData    (replaces bind + glBufferSubData)                 ║
 * ║  • glMapNamedBuffer        (replaces bind + glMapBuffer)                     ║
 * ║  • glMapNamedBufferRange   (replaces bind + glMapBufferRange)                ║
 * ║  • glUnmapNamedBuffer      (replaces bind + glUnmapBuffer)                   ║
 * ║  • glCopyNamedBufferSubData(replaces bind + glCopyBufferSubData)             ║
 * ║  • glGetNamedBufferParameteriv (replaces bind + query)                       ║
 * ║                                                                              ║
 * ║  PERFORMANCE BENEFITS:                                                       ║
 * ║  • Fewer GL calls (no bind/unbind)                                           ║
 * ║  • No binding point pollution                                                ║
 * ║  • Driver can batch operations better                                        ║
 * ║  • No accidental binding overwrites                                          ║
 * ║                                                                              ║
 * ║  OTHER 4.5 FEATURES:                                                         ║
 * ║  • Robustness guarantees                                                     ║
 * ║  • Flush control                                                             ║
 * ║  • Clip control                                                              ║
 * ║  • Cull distance                                                             ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 4.5 (DSA)        │
 * │ Color: #8A2BE2 (Blue Violet)            │
 * └─────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public final class GLBufferOps45 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 45;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL45;
    public static final String VERSION_NAME = "OpenGL 4.5 (DSA)";
    
    // All buffer targets (for compatibility methods)
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_COPY_READ_BUFFER = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    public static final int GL_DRAW_INDIRECT_BUFFER = 0x8F3F;
    public static final int GL_DISPATCH_INDIRECT_BUFFER = 0x90EE;
    public static final int GL_ATOMIC_COUNTER_BUFFER = 0x92C0;
    public static final int GL_QUERY_BUFFER = 0x9192;
    
    // Map flags (all from 4.4)
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_MAP_WRITE_BIT = 0x0002;
    public static final int GL_MAP_INVALIDATE_RANGE_BIT = 0x0004;
    public static final int GL_MAP_INVALIDATE_BUFFER_BIT = 0x0008;
    public static final int GL_MAP_FLUSH_EXPLICIT_BIT = 0x0010;
    public static final int GL_MAP_UNSYNCHRONIZED_BIT = 0x0020;
    public static final int GL_MAP_PERSISTENT_BIT = 0x0040;
    public static final int GL_MAP_COHERENT_BIT = 0x0080;
    
    // Storage flags
    public static final int GL_DYNAMIC_STORAGE_BIT = 0x0100;
    public static final int GL_CLIENT_STORAGE_BIT = 0x0200;
    
    // Pre-composed flags (same as 4.4)
    public static final int STATIC_FLAGS = 0;
    public static final int DYNAMIC_FLAGS = GL_DYNAMIC_STORAGE_BIT;
    public static final int STREAM_PERSISTENT_COHERENT = 
        GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT;
    public static final int STREAM_PERSISTENT_EXPLICIT = 
        GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_FLUSH_EXPLICIT_BIT;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Thread-local pools
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static final ThreadLocal<IntBuffer> TL_1 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer());
    private static final ThreadLocal<IntBuffer> TL_16 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer());
    private static final ThreadLocal<IntBuffer> TL_64 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(256).order(ByteOrder.nativeOrder()).asIntBuffer());
    private static final ThreadLocal<IntBuffer> TL_256 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    // Persistent buffer tracking
    private static final ConcurrentHashMap<Integer, DSABuffer> BUFFERS = new ConcurrentHashMap<>();
    
    /**
     * Buffer metadata for DSA operations.
     */
    public static final class DSABuffer {
        public final int id;
        public final long size;
        public final int storageFlags;
        public final ByteBuffer persistentMap; // null if not persistently mapped
        
        DSABuffer(int id, long size, int flags, ByteBuffer map) {
            this.id = id;
            this.size = size;
            this.storageFlags = flags;
            this.persistentMap = map;
        }
        
        public boolean isPersistent() { return persistentMap != null; }
        public boolean isCoherent() { return (storageFlags & GL_MAP_COHERENT_BIT) != 0; }
        public boolean isDynamic() { return (storageFlags & GL_DYNAMIC_STORAGE_BIT) != 0; }
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    
    public GLBufferOps45() {
        TL_1.get(); TL_16.get(); TL_64.get(); TL_256.get();
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Version Info
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override public int getVersionCode() { return VERSION_CODE; }
    @Override public int getDisplayColor() { return DISPLAY_COLOR; }
    @Override public String getVersionName() { return VERSION_NAME; }
    @Override public boolean supportsMapBufferRange() { return true; }
    @Override public boolean supportsCopyBuffer() { return true; }
    @Override public boolean supportsInvalidation() { return true; }
    @Override public boolean supportsBufferStorage() { return true; }
    @Override public boolean supportsPersistentMapping() { return true; }
    @Override public boolean supportsDSA() { return true; }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Pool Selection
    // ─────────────────────────────────────────────────────────────────────────────
    
    private IntBuffer pool(int n) {
        IntBuffer b;
        if (n <= 1) b = TL_1.get();
        else if (n <= 16) b = TL_16.get();
        else if (n <= 64) b = TL_64.get();
        else if (n <= 256) b = TL_256.get();
        else return ByteBuffer.allocateDirect(n << 2).order(ByteOrder.nativeOrder()).asIntBuffer();
        b.clear();
        if (n > 1) b.limit(n);
        return b;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // DSA: Buffer Creation - glCreateBuffers ★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Create buffer using DSA (glCreateBuffers).
     * 
     * Unlike glGenBuffers which just reserves a name,
     * glCreateBuffers creates AND initializes the buffer object.
     * The buffer is immediately ready for use with Named* functions.
     */
    @Override
    public int genBuffer() {
        IntBuffer b = TL_1.get();
        b.clear();
        OpenGLCallMapper.glCreateBuffers(b);
        return b.get(0);
    }
    
    @Override
    public int[] genBuffers(int count) {
        if (count <= 0) return new int[0];
        if (count == 1) return new int[] { genBuffer() };
        IntBuffer b = pool(count);
        OpenGLCallMapper.glCreateBuffers(b);
        int[] r = new int[count];
        b.rewind();
        b.get(r);
        return r;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Deletion
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void deleteBuffer(int buffer) {
        if (buffer == 0) return;
        BUFFERS.remove(buffer);
        IntBuffer b = TL_1.get();
        b.clear().put(0, buffer);
        OpenGLCallMapper.glDeleteBuffers(b);
    }
    
    @Override
    public void deleteBuffers(int[] buffers) {
        if (buffers == null || buffers.length == 0) return;
        for (int buf : buffers) BUFFERS.remove(buf);
        if (buffers.length == 1) { 
            IntBuffer b = TL_1.get();
            b.clear().put(0, buffers[0]);
            OpenGLCallMapper.glDeleteBuffers(b);
            return;
        }
        IntBuffer b = pool(buffers.length);
        b.put(buffers).flip();
        OpenGLCallMapper.glDeleteBuffers(b);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Binding (still needed for draw calls, VAO setup)
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void bindBuffer(int target, int buffer) {
        OpenGLCallMapper.glBindBuffer(target, buffer);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // DSA: Named Buffer Data ★★★ (NO BINDING REQUIRED!)
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Upload data to buffer WITHOUT binding.
     * This is the DSA equivalent of bind + bufferData.
     */
    public void namedBufferData(int buffer, long size, int usage) {
        OpenGLCallMapper.glNamedBufferData(buffer, size, usage);
    }
    
    public void namedBufferData(int buffer, ByteBuffer data, int usage) {
        OpenGLCallMapper.glNamedBufferData(buffer, data, usage);
    }
    
    public void namedBufferData(int buffer, FloatBuffer data, int usage) {
        OpenGLCallMapper.glNamedBufferData(buffer, data, usage);
    }
    
    public void namedBufferData(int buffer, IntBuffer data, int usage) {
        OpenGLCallMapper.glNamedBufferData(buffer, data, usage);
    }
    
    // Legacy bind-based versions (for interface compliance)
    @Override public void bufferData(int t, long s, int u) { OpenGLCallMapper.glBufferData(t, s, u); }
    @Override public void bufferData(int t, ByteBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    @Override public void bufferData(int t, FloatBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    @Override public void bufferData(int t, IntBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // DSA: Named Buffer SubData ★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    public void namedBufferSubData(int buffer, long offset, ByteBuffer data) {
        OpenGLCallMapper.glNamedBufferSubData(buffer, offset, data);
    }
    
    public void namedBufferSubData(int buffer, long offset, FloatBuffer data) {
        OpenGLCallMapper.glNamedBufferSubData(buffer, offset, data);
    }
    
    public void namedBufferSubData(int buffer, long offset, IntBuffer data) {
        OpenGLCallMapper.glNamedBufferSubData(buffer, offset, data);
    }
    
    // Legacy
    @Override public void bufferSubData(int t, long o, ByteBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    @Override public void bufferSubData(int t, long o, FloatBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    @Override public void bufferSubData(int t, long o, IntBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // DSA: Named Buffer Storage ★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    public void namedBufferStorage(int buffer, long size, int flags) {
        OpenGLCallMapper.glNamedBufferStorage(buffer, size, flags);
    }
    
    public void namedBufferStorage(int buffer, ByteBuffer data, int flags) {
        OpenGLCallMapper.glNamedBufferStorage(buffer, data, flags);
    }
    
    // Legacy bind-based
    @Override public void bufferStorage(int t, long s, int f) { OpenGLCallMapper.glBufferStorage(t, s, f); }
    @Override public void bufferStorage(int t, ByteBuffer d, int f) { OpenGLCallMapper.glBufferStorage(t, d, f); }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // DSA: Named Buffer Mapping ★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Map buffer by name (no binding).
     */
    public ByteBuffer mapNamedBuffer(int buffer, int access) {
        return OpenGLCallMapper.glMapNamedBuffer(buffer, access);
    }
    
    /**
     * Map buffer range by name (no binding).
     */
    public ByteBuffer mapNamedBufferRange(int buffer, long offset, long length, int access) {
        return OpenGLCallMapper.glMapNamedBufferRange(buffer, offset, length, access);
    }
    
    /**
     * Unmap buffer by name.
     */
    public boolean unmapNamedBuffer(int buffer) {
        return OpenGLCallMapper.glUnmapNamedBuffer(buffer);
    }
    
    /**
     * Flush mapped range by name.
     */
    public void flushMappedNamedBufferRange(int buffer, long offset, long length) {
        OpenGLCallMapper.glFlushMappedNamedBufferRange(buffer, offset, length);
    }
    
    // Legacy bind-based
    @Override public ByteBuffer mapBuffer(int t, int a) { return OpenGLCallMapper.glMapBuffer(t, a); }
    @Override public ByteBuffer mapBuffer(int t, int a, long l) { return OpenGLCallMapper.glMapBuffer(t, a, l, null); }
    @Override public ByteBuffer mapBufferRange(int t, long o, long l, int a) { return OpenGLCallMapper.glMapBufferRange(t, o, l, a); }
    @Override public void flushMappedBufferRange(int t, long o, long l) { OpenGLCallMapper.glFlushMappedBufferRange(t, o, l); }
    @Override public boolean unmapBuffer(int t) { return OpenGLCallMapper.glUnmapBuffer(t); }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // DSA: Named Buffer Copy ★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Copy between buffers by name.
     * No need to bind to GL_COPY_READ/WRITE_BUFFER!
     */
    public void copyNamedBufferSubData(int readBuffer, int writeBuffer, 
                                        long readOffset, long writeOffset, long size) {
        OpenGLCallMapper.glCopyNamedBufferSubData(readBuffer, writeBuffer, readOffset, writeOffset, size);
    }
    
    // Legacy
    @Override
    public void copyBufferSubData(int rt, int wt, long ro, long wo, long sz) {
        OpenGLCallMapper.glCopyBufferSubData(rt, wt, ro, wo, sz);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // DSA: Named Buffer Query ★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    public int getNamedBufferParameteri(int buffer, int pname) {
        return OpenGLCallMapper.glGetNamedBufferParameteri(buffer, pname);
    }
    
    public long getNamedBufferParameteri64(int buffer, int pname) {
        return OpenGLCallMapper.glGetNamedBufferParameteri64(buffer, pname);
    }
    
    // Legacy
    @Override
    public int getBufferParameteri(int target, int pname) {
        return OpenGLCallMapper.glGetBufferParameteri(target, pname);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Invalidation
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void invalidateBufferData(int buffer) {
        OpenGLCallMapper.glInvalidateBufferData(buffer);
    }
    
    @Override
    public void invalidateBufferSubData(int buffer, long offset, long length) {
        OpenGLCallMapper.glInvalidateBufferSubData(buffer, offset, length);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // DSA: High-Level Convenience Methods ★★★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Create a static immutable buffer with initial data.
     * Optimal for geometry that never changes.
     * PURE DSA - no binding involved.
     */
    public DSABuffer createStaticBuffer(ByteBuffer data) {
        int buf = genBuffer();
        namedBufferStorage(buf, data, STATIC_FLAGS);
        DSABuffer db = new DSABuffer(buf, data.remaining(), STATIC_FLAGS, null);
        BUFFERS.put(buf, db);
        return db;
    }
    
    /**
     * Create a dynamic buffer that can be updated with namedBufferSubData.
     * PURE DSA.
     */
    public DSABuffer createDynamicBuffer(long size) {
        int buf = genBuffer();
        namedBufferStorage(buf, size, DYNAMIC_FLAGS);
        DSABuffer db = new DSABuffer(buf, size, DYNAMIC_FLAGS, null);
        BUFFERS.put(buf, db);
        return db;
    }
    
    /**
     * Create a persistently mapped streaming buffer.
     * PURE DSA.
     */
    public DSABuffer createPersistentBuffer(long size, boolean coherent) {
        int buf = genBuffer();
        int flags = coherent ? STREAM_PERSISTENT_COHERENT : STREAM_PERSISTENT_EXPLICIT;
        int mapFlags = GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | 
                       (coherent ? GL_MAP_COHERENT_BIT : GL_MAP_FLUSH_EXPLICIT_BIT);
        
        namedBufferStorage(buf, size, flags);
        ByteBuffer mapped = mapNamedBufferRange(buf, 0, size, mapFlags);
        
        DSABuffer db = new DSABuffer(buf, size, flags, mapped);
        BUFFERS.put(buf, db);
        return db;
    }
    
    /**
     * Get buffer info.
     */
    public DSABuffer getBuffer(int buffer) {
        return BUFFERS.get(buffer);
    }
    
    /**
     * Resize buffer, preserving data.
     * Creates new buffer, copies data, deletes old.
     * PURE DSA.
     */
    public DSABuffer resizeBuffer(int oldBuffer, long newSize, int flags) {
        DSABuffer old = BUFFERS.get(oldBuffer);
        long oldSize = old != null ? old.size : getNamedBufferParameteri64(oldBuffer, 0x8764);
        
        int newBuf = genBuffer();
        namedBufferStorage(newBuf, newSize, flags);
        
        if (oldSize > 0) {
            copyNamedBufferSubData(oldBuffer, newBuf, 0, 0, Math.min(oldSize, newSize));
        }
        
        deleteBuffer(oldBuffer);
        
        DSABuffer db = new DSABuffer(newBuf, newSize, flags, null);
        BUFFERS.put(newBuf, db);
        return db;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void shutdown() {
        BUFFERS.clear();
    }
}
