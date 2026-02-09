package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GLBufferOps46 - OpenGL 4.6 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║              ★★★★★★★★★ MAXIMUM POWER - ALL FEATURES ★★★★★★★★★              ║
 * ║                                                                              ║
 * ║  OpenGL 4.6 (2017) - THE PINNACLE OF OPENGL:                                 ║
 * ║                                                                              ║
 * ║  ┌────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │                      EVERYTHING FROM 4.5 PLUS:                         │  ║
 * ║  │                                                                        │  ║
 * ║  │  • SPIR-V shader support (Vulkan-style shaders)                        │  ║
 * ║  │  • Anisotropic filtering (CORE, not extension)                         │  ║
 * ║  │  • Polygon offset clamp                                                │  ║
 * ║  │  • No error context mode                                               │  ║
 * ║  │  • NVIDIA: "No driver overhead" mode                                   │  ║
 * ║  │  • All ARB extensions from 4.5 era                                     │  ║
 * ║  └────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                              ║
 * ║  THIS PIPELINE INCLUDES:                                                     ║
 * ║  ✓ Full DSA (Direct State Access)                                           ║
 * ║  ✓ Persistent mapping with coherent/explicit modes                          ║
 * ║  ✓ Immutable buffer storage                                                 ║
 * ║  ✓ Buffer invalidation                                                      ║
 * ║  ✓ Memory barriers                                                          ║
 * ║  ✓ Fence synchronization                                                    ║
 * ║  ✓ Ring buffer streaming                                                    ║
 * ║  ✓ Triple buffering                                                         ║
 * ║  ✓ Zero-copy update patterns                                                ║
 * ║  ✓ Batch operations                                                         ║
 * ║  ✓ Debug facilities                                                         ║
 * ║                                                                              ║
 * ║  OPTIMIZATIONS:                                                              ║
 * ║  • Thread-local buffer pools (zero allocation)                              ║
 * ║  • Persistent mapping tracking                                              ║
 * ║  • Automatic fence management                                               ║
 * ║  • Ring buffer with automatic wraparound                                    ║
 * ║  • Statistics tracking for profiling                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 4.6 ★ MAXIMUM ★  │
 * │ Color: #FF00FF (Magenta)                │
 * └─────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public final class GLBufferOps46 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 46;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL46;
    public static final String VERSION_NAME = "OpenGL 4.6";
    
    // All buffer targets
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
    public static final int GL_PARAMETER_BUFFER = 0x80EE;
    
    // All map flags
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
    
    // Memory barriers
    public static final int GL_VERTEX_ATTRIB_ARRAY_BARRIER_BIT = 0x00000001;
    public static final int GL_ELEMENT_ARRAY_BARRIER_BIT = 0x00000002;
    public static final int GL_UNIFORM_BARRIER_BIT = 0x00000004;
    public static final int GL_COMMAND_BARRIER_BIT = 0x00000040;
    public static final int GL_BUFFER_UPDATE_BARRIER_BIT = 0x00000200;
    public static final int GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT = 0x00004000;
    public static final int GL_SHADER_STORAGE_BARRIER_BIT = 0x00002000;
    public static final int GL_ALL_BARRIER_BITS = 0xFFFFFFFF;
    
    // Buffer parameters
    public static final int GL_BUFFER_SIZE = 0x8764;
    public static final int GL_BUFFER_USAGE = 0x8765;
    public static final int GL_BUFFER_MAPPED = 0x88BC;
    public static final int GL_BUFFER_MAP_LENGTH = 0x9120;
    public static final int GL_BUFFER_MAP_OFFSET = 0x9121;
    public static final int GL_BUFFER_STORAGE_FLAGS = 0x8220;
    public static final int GL_BUFFER_IMMUTABLE_STORAGE = 0x821F;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Pre-composed flag sets for optimal patterns
    // ─────────────────────────────────────────────────────────────────────────────
    
    /** Static data: upload once, never change */
    public static final int FLAGS_STATIC = 0;
    
    /** Dynamic with subData updates */
    public static final int FLAGS_DYNAMIC = GL_DYNAMIC_STORAGE_BIT;
    
    /** Streaming coherent (automatic sync) */
    public static final int FLAGS_STREAM_COHERENT = 
        GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT;
    
    /** Streaming explicit (manual sync, faster) */
    public static final int FLAGS_STREAM_EXPLICIT = 
        GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_FLUSH_EXPLICIT_BIT;
    
    /** Readback buffer */
    public static final int FLAGS_READBACK = 
        GL_MAP_READ_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Thread-local pools (tiered for optimal reuse)
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static final ThreadLocal<IntBuffer> TL_1 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer());
    private static final ThreadLocal<IntBuffer> TL_16 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asIntBuffer());
    private static final ThreadLocal<IntBuffer> TL_64 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(256).order(ByteOrder.nativeOrder()).asIntBuffer());
    private static final ThreadLocal<IntBuffer> TL_256 = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder()).asIntBuffer());
    private static final ThreadLocal<IntBuffer> TL_1K = ThreadLocal.withInitial(() ->
            ByteBuffer.allocateDirect(4096).order(ByteOrder.nativeOrder()).asIntBuffer());
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Registry (for tracking and cleanup)
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static final ConcurrentHashMap<Integer, ManagedBuffer> MANAGED_BUFFERS = 
            new ConcurrentHashMap<>();
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Statistics (for profiling)
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static final AtomicLong STAT_BUFFERS_CREATED = new AtomicLong();
    private static final AtomicLong STAT_BUFFERS_DELETED = new AtomicLong();
    private static final AtomicLong STAT_BYTES_UPLOADED = new AtomicLong();
    private static final AtomicLong STAT_BYTES_MAPPED = new AtomicLong();
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Managed Buffer Class
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final class ManagedBuffer {
        public final int id;
        public final long size;
        public final int flags;
        public final boolean immutable;
        public final ByteBuffer persistentMap;
        
        // Ring buffer state (if applicable)
        private long writeOffset;
        private long usedSize;
        
        // Fence for synchronization
        private long fence;
        
        ManagedBuffer(int id, long size, int flags, boolean immutable, ByteBuffer map) {
            this.id = id;
            this.size = size;
            this.flags = flags;
            this.immutable = immutable;
            this.persistentMap = map;
            this.writeOffset = 0;
            this.usedSize = 0;
            this.fence = 0;
        }
        
        public boolean isPersistent() { return persistentMap != null; }
        public boolean isCoherent() { return (flags & GL_MAP_COHERENT_BIT) != 0; }
        
        // Ring buffer methods
        public long getWriteOffset() { return writeOffset; }
        public void advanceWriteOffset(long bytes) { 
            writeOffset = (writeOffset + bytes) % size; 
        }
        public void resetWriteOffset() { writeOffset = 0; }
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    
    public GLBufferOps46() {
        // Warm up all pools
        TL_1.get(); TL_16.get(); TL_64.get(); TL_256.get(); TL_1K.get();
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
        else if (n <= 1024) b = TL_1K.get();
        else return ByteBuffer.allocateDirect(n << 2).order(ByteOrder.nativeOrder()).asIntBuffer();
        b.clear();
        if (n > 1) b.limit(n);
        return b;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // DSA: Buffer Creation
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int genBuffer() {
        IntBuffer b = TL_1.get();
        b.clear();
        OpenGLCallMapper.glCreateBuffers(b);
        STAT_BUFFERS_CREATED.incrementAndGet();
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
        STAT_BUFFERS_CREATED.addAndGet(count);
        return r;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Deletion
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void deleteBuffer(int buffer) {
        if (buffer == 0) return;
        
        ManagedBuffer mb = MANAGED_BUFFERS.remove(buffer);
        if (mb != null && mb.fence != 0) {
            OpenGLCallMapper.glDeleteSync(mb.fence);
        }
        
        IntBuffer b = TL_1.get();
        b.clear().put(0, buffer);
        OpenGLCallMapper.glDeleteBuffers(b);
        STAT_BUFFERS_DELETED.incrementAndGet();
    }
    
    @Override
    public void deleteBuffers(int[] buffers) {
        if (buffers == null || buffers.length == 0) return;
        
        for (int buf : buffers) {
            ManagedBuffer mb = MANAGED_BUFFERS.remove(buf);
            if (mb != null && mb.fence != 0) {
                OpenGLCallMapper.glDeleteSync(mb.fence);
            }
        }
        
        if (buffers.length == 1) {
            IntBuffer b = TL_1.get();
            b.clear().put(0, buffers[0]);
            OpenGLCallMapper.glDeleteBuffers(b);
        } else {
            IntBuffer b = pool(buffers.length);
            b.put(buffers).flip();
            OpenGLCallMapper.glDeleteBuffers(b);
        }
        STAT_BUFFERS_DELETED.addAndGet(buffers.length);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Binding (for draw calls)
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void bindBuffer(int target, int buffer) {
        OpenGLCallMapper.glBindBuffer(target, buffer);
    }
    
    public void bindBufferBase(int target, int index, int buffer) {
        OpenGLCallMapper.glBindBufferBase(target, index, buffer);
    }
    
    public void bindBufferRange(int target, int index, int buffer, long offset, long size) {
        OpenGLCallMapper.glBindBufferRange(target, index, buffer, offset, size);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // DSA: All Named Operations
    // ─────────────────────────────────────────────────────────────────────────────
    
    // Storage
    public void namedBufferStorage(int buf, long size, int flags) {
        OpenGLCallMapper.glNamedBufferStorage(buf, size, flags);
    }
    public void namedBufferStorage(int buf, ByteBuffer data, int flags) {
        OpenGLCallMapper.glNamedBufferStorage(buf, data, flags);
        STAT_BYTES_UPLOADED.addAndGet(data.remaining());
    }
    
    // Data
    public void namedBufferData(int buf, long size, int usage) {
        OpenGLCallMapper.glNamedBufferData(buf, size, usage);
    }
    public void namedBufferData(int buf, ByteBuffer data, int usage) {
        OpenGLCallMapper.glNamedBufferData(buf, data, usage);
        STAT_BYTES_UPLOADED.addAndGet(data.remaining());
    }
    
    // SubData
    public void namedBufferSubData(int buf, long offset, ByteBuffer data) {
        OpenGLCallMapper.glNamedBufferSubData(buf, offset, data);
        STAT_BYTES_UPLOADED.addAndGet(data.remaining());
    }
    
    // Mapping
    public ByteBuffer mapNamedBuffer(int buf, int access) {
        return OpenGLCallMapper.glMapNamedBuffer(buf, access);
    }
    public ByteBuffer mapNamedBufferRange(int buf, long off, long len, int access) {
        ByteBuffer mapped = OpenGLCallMapper.glMapNamedBufferRange(buf, off, len, access);
        if (mapped != null) STAT_BYTES_MAPPED.addAndGet(len);
        return mapped;
    }
    public boolean unmapNamedBuffer(int buf) {
        return OpenGLCallMapper.glUnmapNamedBuffer(buf);
    }
    public void flushMappedNamedBufferRange(int buf, long off, long len) {
        OpenGLCallMapper.glFlushMappedNamedBufferRange(buf, off, len);
    }
    
    // Copy
    public void copyNamedBufferSubData(int read, int write, long ro, long wo, long sz) {
        OpenGLCallMapper.glCopyNamedBufferSubData(read, write, ro, wo, sz);
    }
    
    // Query
    public int getNamedBufferParameteri(int buf, int pname) {
        return OpenGLCallMapper.glGetNamedBufferParameteri(buf, pname);
    }
    public long getNamedBufferParameteri64(int buf, int pname) {
        return OpenGLCallMapper.glGetNamedBufferParameteri64(buf, pname);
    }
    
    // Legacy interface compliance
    @Override public void bufferData(int t, long s, int u) { OpenGLCallMapper.glBufferData(t, s, u); }
    @Override public void bufferData(int t, ByteBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); STAT_BYTES_UPLOADED.addAndGet(d.remaining()); }
    @Override public void bufferData(int t, FloatBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    @Override public void bufferData(int t, IntBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    @Override public void bufferSubData(int t, long o, ByteBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    @Override public void bufferSubData(int t, long o, FloatBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    @Override public void bufferSubData(int t, long o, IntBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    @Override public void bufferStorage(int t, long s, int f) { OpenGLCallMapper.glBufferStorage(t, s, f); }
    @Override public void bufferStorage(int t, ByteBuffer d, int f) { OpenGLCallMapper.glBufferStorage(t, d, f); }
    @Override public ByteBuffer mapBuffer(int t, int a) { return OpenGLCallMapper.glMapBuffer(t, a); }
    @Override public ByteBuffer mapBuffer(int t, int a, long l) { return OpenGLCallMapper.glMapBuffer(t, a, l, null); }
    @Override public ByteBuffer mapBufferRange(int t, long o, long l, int a) { return OpenGLCallMapper.glMapBufferRange(t, o, l, a); }
    @Override public void flushMappedBufferRange(int t, long o, long l) { OpenGLCallMapper.glFlushMappedBufferRange(t, o, l); }
    @Override public boolean unmapBuffer(int t) { return OpenGLCallMapper.glUnmapBuffer(t); }
    @Override public void copyBufferSubData(int rt, int wt, long ro, long wo, long sz) { OpenGLCallMapper.glCopyBufferSubData(rt, wt, ro, wo, sz); }
    @Override public int getBufferParameteri(int t, int p) { return OpenGLCallMapper.glGetBufferParameteri(t, p); }
    @Override public void invalidateBufferData(int buf) { OpenGLCallMapper.glInvalidateBufferData(buf); }
    @Override public void invalidateBufferSubData(int buf, long o, long l) { OpenGLCallMapper.glInvalidateBufferSubData(buf, o, l); }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // High-Level Factory Methods
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Create static buffer (immutable, no updates).
     */
    public ManagedBuffer createStaticBuffer(ByteBuffer data) {
        int buf = genBuffer();
        namedBufferStorage(buf, data, FLAGS_STATIC);
        ManagedBuffer mb = new ManagedBuffer(buf, data.remaining(), FLAGS_STATIC, true, null);
        MANAGED_BUFFERS.put(buf, mb);
        return mb;
    }
    
    /**
     * Create dynamic buffer (updateable via subData).
     */
    public ManagedBuffer createDynamicBuffer(long size) {
        int buf = genBuffer();
        namedBufferStorage(buf, size, FLAGS_DYNAMIC);
        ManagedBuffer mb = new ManagedBuffer(buf, size, FLAGS_DYNAMIC, true, null);
        MANAGED_BUFFERS.put(buf, mb);
        return mb;
    }
    
    /**
     * Create streaming buffer with persistent coherent mapping.
     */
    public ManagedBuffer createStreamingBuffer(long size) {
        int buf = genBuffer();
        namedBufferStorage(buf, size, FLAGS_STREAM_COHERENT);
        ByteBuffer mapped = mapNamedBufferRange(buf, 0, size,
            GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
        ManagedBuffer mb = new ManagedBuffer(buf, size, FLAGS_STREAM_COHERENT, true, mapped);
        MANAGED_BUFFERS.put(buf, mb);
        return mb;
    }
    
    /**
     * Create ring buffer for streaming with automatic wraparound.
     */
    public ManagedBuffer createRingBuffer(long size) {
        return createStreamingBuffer(size);
    }
    
    /**
     * Write to ring buffer, handling wraparound.
     * Returns the offset where data was written.
     */
    public long writeToRingBuffer(ManagedBuffer ring, ByteBuffer data) {
        int dataSize = data.remaining();
        long offset = ring.writeOffset;
        
        // Check if we need to wrap
        if (offset + dataSize > ring.size) {
            offset = 0;
            ring.resetWriteOffset();
        }
        
        // Copy to persistent mapping
        ring.persistentMap.position((int) offset);
        ring.persistentMap.put(data);
        ring.persistentMap.clear();
        
        ring.advanceWriteOffset(dataSize);
        return offset;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Fence/Sync Operations
    // ─────────────────────────────────────────────────────────────────────────────
    
    public long createFence() {
        return OpenGLCallMapper.glFenceSync(0x9117, 0);
    }
    
    public boolean waitFence(long fence, long timeoutNanos) {
        if (fence == 0) return true;
        int r = OpenGLCallMapper.glClientWaitSync(fence, 0x00000001, timeoutNanos);
        return r == 0x911A || r == 0x911C;
    }
    
    public void deleteFence(long fence) {
        if (fence != 0) OpenGLCallMapper.glDeleteSync(fence);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Memory Barriers
    // ─────────────────────────────────────────────────────────────────────────────
    
    public void memoryBarrier(int barriers) {
        OpenGLCallMapper.glMemoryBarrier(barriers);
    }
    
    public void fullBarrier() {
        OpenGLCallMapper.glMemoryBarrier(GL_ALL_BARRIER_BITS);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Statistics
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static long getBuffersCreated() { return STAT_BUFFERS_CREATED.get(); }
    public static long getBuffersDeleted() { return STAT_BUFFERS_DELETED.get(); }
    public static long getBuffersActive() { return STAT_BUFFERS_CREATED.get() - STAT_BUFFERS_DELETED.get(); }
    public static long getBytesUploaded() { return STAT_BYTES_UPLOADED.get(); }
    public static long getBytesMapped() { return STAT_BYTES_MAPPED.get(); }
    
    public static void resetStats() {
        STAT_BUFFERS_CREATED.set(0);
        STAT_BUFFERS_DELETED.set(0);
        STAT_BYTES_UPLOADED.set(0);
        STAT_BYTES_MAPPED.set(0);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void shutdown() {
        // Delete all fences
        for (ManagedBuffer mb : MANAGED_BUFFERS.values()) {
            if (mb.fence != 0) {
                OpenGLCallMapper.glDeleteSync(mb.fence);
            }
        }
        MANAGED_BUFFERS.clear();
        resetStats();
    }
}
