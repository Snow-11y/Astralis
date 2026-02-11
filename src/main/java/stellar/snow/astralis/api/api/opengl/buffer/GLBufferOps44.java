package stellar.snow.astralis.api.opengl.buffer;

import stellar.snow.astralis.api.opengl.mapping.OpenGLCallMapper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GLBufferOps44 - OpenGL 4.4 Pipeline
 * 
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║     ★★★★★★ PERSISTENT MAPPING - THE ULTIMATE STREAMING SOLUTION ★★★★★★     ║
 * ║                                                                              ║
 * ║  OpenGL 4.4 (2013) - GAME CHANGING FEATURES:                                 ║
 * ║                                                                              ║
 * ║  ┌────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │                     IMMUTABLE BUFFER STORAGE                           │  ║
 * ║  │                                                                        │  ║
 * ║  │  glBufferStorage - Create buffer with IMMUTABLE size & flags           │  ║
 * ║  │  • Size CANNOT change after creation (more efficient)                  │  ║
 * ║  │  • Driver knows exact usage pattern upfront                            │  ║
 * ║  │  • Better memory placement decisions                                   │  ║
 * ║  └────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                              ║
 * ║  ┌────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │                     PERSISTENT MAPPING                                 │  ║
 * ║  │                                                                        │  ║
 * ║  │  GL_MAP_PERSISTENT_BIT:                                                │  ║
 * ║  │  • Map buffer ONCE at creation                                         │  ║
 * ║  │  • NEVER unmap (until deletion)                                        │  ║
 * ║  │  • Write directly to pointer every frame                               │  ║
 * ║  │  • ZERO map/unmap overhead!                                            │  ║
 * ║  │                                                                        │  ║
 * ║  │  GL_MAP_COHERENT_BIT:                                                  │  ║
 * ║  │  • Writes immediately visible to GPU                                   │  ║
 * ║  │  • No explicit flush needed                                            │  ║
 * ║  │  • Slight overhead vs non-coherent                                     │  ║
 * ║  │                                                                        │  ║
 * ║  │  Without coherent (manual sync):                                       │  ║
 * ║  │  • Use glMemoryBarrier or glFlushMappedBufferRange                     │  ║
 * ║  │  • More control, potentially faster                                    │  ║
 * ║  └────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                              ║
 * ║  ┌────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │                     TRIPLE BUFFERING PATTERN                           │  ║
 * ║  │                                                                        │  ║
 * ║  │  Frame N:   [Write Region 0] [GPU reads 1] [GPU reads 2]               │  ║
 * ║  │  Frame N+1: [GPU reads 0] [Write Region 1] [GPU reads 2]               │  ║
 * ║  │  Frame N+2: [GPU reads 0] [GPU reads 1] [Write Region 2]               │  ║
 * ║  │                                                                        │  ║
 * ║  │  • Never overwrite data GPU is currently reading                       │  ║
 * ║  │  • Use fences to verify GPU completion                                 │  ║
 * ║  │  • ZERO synchronization stalls!                                        │  ║
 * ║  └────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                              ║
 * ║  OTHER 4.4 FEATURES:                                                         ║
 * ║  • Multi-bind (bind multiple buffers in one call)                           ║
 * ║  • Enhanced layouts for uniform/storage blocks                              ║
 * ║  • Query buffer target                                                       ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 * 
 * ┌─────────────────────────────────────────┐
 * │ Snowium Render: OpenGL 4.4              │
 * │ Color: #4169E1 (Royal Blue)             │
 * └─────────────────────────────────────────┘
 * 
 * Part of: Astralis / Snowium Render System
 */
public final class GLBufferOps44 implements BufferOps {

    // ─────────────────────────────────────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int VERSION_CODE = 44;
    public static final int DISPLAY_COLOR = GLBufferOpsBase.COLOR_GL44;
    public static final String VERSION_NAME = "OpenGL 4.4";
    
    // Buffer targets
    public static final int GL_ARRAY_BUFFER = 0x8892;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    public static final int GL_COPY_READ_BUFFER = 0x8F36;
    public static final int GL_COPY_WRITE_BUFFER = 0x8F37;
    public static final int GL_UNIFORM_BUFFER = 0x8A11;
    public static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    public static final int GL_DRAW_INDIRECT_BUFFER = 0x8F3F;
    public static final int GL_DISPATCH_INDIRECT_BUFFER = 0x90EE;
    public static final int GL_ATOMIC_COUNTER_BUFFER = 0x92C0;
    public static final int GL_QUERY_BUFFER = 0x9192; // NEW in 4.4
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Map Flags (critical for persistent mapping)
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_MAP_READ_BIT = 0x0001;
    public static final int GL_MAP_WRITE_BIT = 0x0002;
    public static final int GL_MAP_INVALIDATE_RANGE_BIT = 0x0004;
    public static final int GL_MAP_INVALIDATE_BUFFER_BIT = 0x0008;
    public static final int GL_MAP_FLUSH_EXPLICIT_BIT = 0x0010;
    public static final int GL_MAP_UNSYNCHRONIZED_BIT = 0x0020;
    
    // NEW in 4.4 - THE BIG ONES
    public static final int GL_MAP_PERSISTENT_BIT = 0x0040;
    public static final int GL_MAP_COHERENT_BIT = 0x0080;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Storage Flags (for glBufferStorage)
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_DYNAMIC_STORAGE_BIT = 0x0100;  // Allow bufferSubData
    public static final int GL_CLIENT_STORAGE_BIT = 0x0200;   // Prefer client-side memory
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Pre-composed flag combinations for common patterns
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Flags for STATIC immutable data (textures, static meshes).
     * Upload once, GPU reads forever, no CPU access after.
     */
    public static final int STATIC_FLAGS = 0; // No flags = read-only by GPU
    
    /**
     * Flags for DYNAMIC data with bufferSubData updates.
     * Traditional update pattern, mutable via subData.
     */
    public static final int DYNAMIC_FLAGS = GL_DYNAMIC_STORAGE_BIT;
    
    /**
     * Flags for STREAMING with persistent coherent mapping.
     * Map once, write every frame, automatic sync.
     * Easiest to use but slight coherency overhead.
     */
    public static final int STREAM_PERSISTENT_COHERENT_FLAGS = 
        GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT;
    
    /**
     * Flags for STREAMING with persistent non-coherent mapping.
     * Map once, write every frame, manual flush.
     * Maximum performance but requires explicit sync.
     */
    public static final int STREAM_PERSISTENT_FLAGS = 
        GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_FLUSH_EXPLICIT_BIT;
    
    /**
     * Flags for READ-BACK with persistent mapping.
     * For GPU->CPU data transfer (occlusion results, etc).
     */
    public static final int READBACK_PERSISTENT_FLAGS = 
        GL_MAP_READ_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT;
    
    /**
     * Flags for BIDIRECTIONAL persistent access.
     * Both CPU and GPU can read/write. Use with care.
     */
    public static final int BIDIRECTIONAL_PERSISTENT_FLAGS = 
        GL_MAP_READ_BIT | GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT;
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Memory Barrier Bits
    // ─────────────────────────────────────────────────────────────────────────────
    
    public static final int GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT = 0x00004000;
    public static final int GL_BUFFER_UPDATE_BARRIER_BIT = 0x00000200;
    public static final int GL_SHADER_STORAGE_BARRIER_BIT = 0x00002000;
    public static final int GL_ALL_BARRIER_BITS = 0xFFFFFFFF;
    
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
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Persistent Mapping Registry
    // Tracks persistently mapped buffers to prevent double-mapping and leaks
    // ─────────────────────────────────────────────────────────────────────────────
    
    private static final ConcurrentHashMap<Integer, PersistentBuffer> PERSISTENT_BUFFERS = 
            new ConcurrentHashMap<>();
    
    /**
     * Metadata for persistently mapped buffers.
     */
    public static final class PersistentBuffer {
        public final int id;
        public final long size;
        public final ByteBuffer mapped;
        public final int flags;
        public final boolean coherent;
        
        PersistentBuffer(int id, long size, ByteBuffer mapped, int flags) {
            this.id = id;
            this.size = size;
            this.mapped = mapped;
            this.flags = flags;
            this.coherent = (flags & GL_MAP_COHERENT_BIT) != 0;
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────────────────────
    
    public GLBufferOps44() {
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
    // Buffer Generation
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int genBuffer() {
        IntBuffer b = TL_1.get();
        b.clear();
        OpenGLCallMapper.glGenBuffers(b);
        return b.get(0);
    }
    
    @Override
    public int[] genBuffers(int count) {
        if (count <= 0) return new int[0];
        if (count == 1) return new int[] { genBuffer() };
        IntBuffer b = pool(count);
        OpenGLCallMapper.glGenBuffers(b);
        int[] r = new int[count];
        b.rewind();
        b.get(r);
        return r;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Buffer Deletion - CRITICAL: Clean up persistent mappings!
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void deleteBuffer(int buffer) {
        if (buffer == 0) return;
        
        // Remove from persistent registry (no need to unmap - deletion handles it)
        PERSISTENT_BUFFERS.remove(buffer);
        
        IntBuffer b = TL_1.get();
        b.clear().put(0, buffer);
        OpenGLCallMapper.glDeleteBuffers(b);
    }
    
    @Override
    public void deleteBuffers(int[] buffers) {
        if (buffers == null || buffers.length == 0) return;
        
        // Clean persistent registry
        for (int buf : buffers) {
            PERSISTENT_BUFFERS.remove(buf);
        }
        
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
    // Binding
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void bindBuffer(int target, int buffer) {
        OpenGLCallMapper.glBindBuffer(target, buffer);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Legacy Data Upload (still works with DYNAMIC_STORAGE_BIT)
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override public void bufferData(int t, long s, int u) { OpenGLCallMapper.glBufferData(t, s, u); }
    @Override public void bufferData(int t, ByteBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    @Override public void bufferData(int t, FloatBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    @Override public void bufferData(int t, IntBuffer d, int u) { OpenGLCallMapper.glBufferData(t, d, u); }
    
    @Override public void bufferSubData(int t, long o, ByteBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    @Override public void bufferSubData(int t, long o, FloatBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    @Override public void bufferSubData(int t, long o, IntBuffer d) { OpenGLCallMapper.glBufferSubData(t, o, d); }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // GL 4.4: IMMUTABLE BUFFER STORAGE ★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Create immutable buffer storage.
     * 
     * CRITICAL NOTES:
     * - Size CANNOT change after this call
     * - Cannot call bufferData() on this buffer ever again
     * - CAN call bufferSubData() IF GL_DYNAMIC_STORAGE_BIT was set
     * - CAN map IF appropriate MAP_*_BIT flags were set
     * 
     * @param target Buffer target (must be bound first)
     * @param size Size in bytes
     * @param flags Combination of storage/map flags
     */
    @Override
    public void bufferStorage(int target, long size, int flags) {
        OpenGLCallMapper.glBufferStorage(target, size, flags);
    }
    
    /**
     * Create immutable buffer storage with initial data.
     */
    @Override
    public void bufferStorage(int target, ByteBuffer data, int flags) {
        OpenGLCallMapper.glBufferStorage(target, data, flags);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Legacy Mapping (still works)
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override public ByteBuffer mapBuffer(int t, int a) { return OpenGLCallMapper.glMapBuffer(t, a); }
    @Override public ByteBuffer mapBuffer(int t, int a, long l) { return OpenGLCallMapper.glMapBuffer(t, a, l, null); }
    @Override public ByteBuffer mapBufferRange(int t, long o, long l, int a) { return OpenGLCallMapper.glMapBufferRange(t, o, l, a); }
    @Override public void flushMappedBufferRange(int t, long o, long l) { OpenGLCallMapper.glFlushMappedBufferRange(t, o, l); }
    @Override public boolean unmapBuffer(int t) { return OpenGLCallMapper.glUnmapBuffer(t); }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Copy & Invalidation
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void copyBufferSubData(int rt, int wt, long ro, long wo, long sz) {
        OpenGLCallMapper.glCopyBufferSubData(rt, wt, ro, wo, sz);
    }
    
    @Override
    public void invalidateBufferData(int buffer) {
        OpenGLCallMapper.glInvalidateBufferData(buffer);
    }
    
    @Override
    public void invalidateBufferSubData(int buffer, long offset, long length) {
        OpenGLCallMapper.glInvalidateBufferSubData(buffer, offset, length);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Query
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public int getBufferParameteri(int target, int pname) {
        return OpenGLCallMapper.glGetBufferParameteri(target, pname);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // GL 4.4: PERSISTENT MAPPING API ★★★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Create a persistently mapped streaming buffer.
     * 
     * This is THE optimal pattern for per-frame dynamic data:
     * - Buffer is mapped ONCE at creation
     * - Pointer remains valid until buffer deletion
     * - Write directly to mapped memory every frame
     * - ZERO map/unmap overhead
     * 
     * Uses coherent mapping for simplicity (writes immediately visible to GPU).
     * 
     * @param target Buffer target
     * @param size Buffer size in bytes
     * @return PersistentBuffer with mapped ByteBuffer
     */
    public PersistentBuffer createPersistentStreamBuffer(int target, long size) {
        int buf = genBuffer();
        bindBuffer(target, buf);
        
        // Create immutable storage with persistent coherent write access
        bufferStorage(target, size, STREAM_PERSISTENT_COHERENT_FLAGS);
        
        // Map persistently - this mapping is valid until buffer deletion
        ByteBuffer mapped = mapBufferRange(target, 0, size, 
            GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
        
        PersistentBuffer pb = new PersistentBuffer(buf, size, mapped, STREAM_PERSISTENT_COHERENT_FLAGS);
        PERSISTENT_BUFFERS.put(buf, pb);
        
        return pb;
    }
    
    /**
     * Create persistent buffer with explicit flush (non-coherent).
     * Higher performance but requires manual synchronization.
     * 
     * After writing, call flushMappedBufferRange() on the written region,
     * then issue a memory barrier before GPU access.
     */
    public PersistentBuffer createPersistentStreamBufferExplicit(int target, long size) {
        int buf = genBuffer();
        bindBuffer(target, buf);
        
        bufferStorage(target, size, STREAM_PERSISTENT_FLAGS);
        
        ByteBuffer mapped = mapBufferRange(target, 0, size,
            GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_FLUSH_EXPLICIT_BIT);
        
        PersistentBuffer pb = new PersistentBuffer(buf, size, mapped, STREAM_PERSISTENT_FLAGS);
        PERSISTENT_BUFFERS.put(buf, pb);
        
        return pb;
    }
    
    /**
     * Create persistent buffer for GPU readback.
     * GPU writes data, CPU reads it persistently.
     */
    public PersistentBuffer createPersistentReadbackBuffer(int target, long size) {
        int buf = genBuffer();
        bindBuffer(target, buf);
        
        bufferStorage(target, size, READBACK_PERSISTENT_FLAGS);
        
        ByteBuffer mapped = mapBufferRange(target, 0, size,
            GL_MAP_READ_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
        
        PersistentBuffer pb = new PersistentBuffer(buf, size, mapped, READBACK_PERSISTENT_FLAGS);
        PERSISTENT_BUFFERS.put(buf, pb);
        
        return pb;
    }
    
    /**
     * Get existing persistent buffer info.
     * Returns null if buffer is not persistently mapped.
     */
    public PersistentBuffer getPersistentBuffer(int buffer) {
        return PERSISTENT_BUFFERS.get(buffer);
    }
    
    /**
     * Check if buffer is persistently mapped.
     */
    public boolean isPersistentlyMapped(int buffer) {
        return PERSISTENT_BUFFERS.containsKey(buffer);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // GL 4.4: Triple Buffer Helper ★★★
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Triple buffer for lock-free streaming.
     * 
     * Contains 3 regions that rotate each frame:
     * - One being written by CPU
     * - One or two being read by GPU
     * - Never any overlap
     */
    public static final class TripleBuffer {
        public final int bufferId;
        public final long regionSize;
        public final long totalSize;
        public final ByteBuffer mapped;
        public final boolean coherent;
        
        private int writeIndex = 0;
        
        // Fence objects for synchronization (GL sync objects)
        private final long[] fences = new long[3];
        
        TripleBuffer(int id, long regionSize, ByteBuffer mapped, boolean coherent) {
            this.bufferId = id;
            this.regionSize = regionSize;
            this.totalSize = regionSize * 3;
            this.mapped = mapped;
            this.coherent = coherent;
        }
        
        /**
         * Get the region to write to this frame.
         * Automatically rotates to next region.
         */
        public ByteBuffer getWriteRegion() {
            int idx = writeIndex;
            writeIndex = (writeIndex + 1) % 3;
            
            long offset = idx * regionSize;
            mapped.position((int) offset);
            mapped.limit((int) (offset + regionSize));
            ByteBuffer slice = mapped.slice();
            mapped.clear();
            
            return slice;
        }
        
        /**
         * Get current write region index (0, 1, or 2).
         */
        public int getCurrentWriteIndex() {
            return writeIndex;
        }
        
        /**
         * Get byte offset of current write region.
         */
        public long getCurrentWriteOffset() {
            return writeIndex * regionSize;
        }
        
        /**
         * Get byte offset for specific region.
         */
        public long getRegionOffset(int index) {
            return index * regionSize;
        }
    }
    
    /**
     * Create a triple buffer for streaming.
     * 
     * @param target Buffer target
     * @param regionSize Size of EACH region (total = 3x this)
     * @param coherent Use coherent mapping (simpler) or explicit flush (faster)
     * @return TripleBuffer helper
     */
    public TripleBuffer createTripleBuffer(int target, long regionSize, boolean coherent) {
        int buf = genBuffer();
        bindBuffer(target, buf);
        
        long totalSize = regionSize * 3;
        int flags = coherent ? STREAM_PERSISTENT_COHERENT_FLAGS : STREAM_PERSISTENT_FLAGS;
        int mapFlags = GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | 
                       (coherent ? GL_MAP_COHERENT_BIT : GL_MAP_FLUSH_EXPLICIT_BIT);
        
        bufferStorage(target, totalSize, flags);
        ByteBuffer mapped = mapBufferRange(target, 0, totalSize, mapFlags);
        
        TripleBuffer tb = new TripleBuffer(buf, regionSize, mapped, coherent);
        
        // Register in persistent buffer map
        PERSISTENT_BUFFERS.put(buf, new PersistentBuffer(buf, totalSize, mapped, flags));
        
        return tb;
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // GL 4.4: Memory Barriers for non-coherent persistent mapping
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Memory barrier for client mapped buffer writes.
     * Call after writing to non-coherent persistent mapping before GPU access.
     */
    public void clientMappedBufferBarrier() {
        OpenGLCallMapper.glMemoryBarrier(GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // GL 4.4: Fence/Sync helpers for triple buffering
    // ─────────────────────────────────────────────────────────────────────────────
    
    /**
     * Create a fence sync object.
     * Insert after draw call that uses a buffer region.
     * 
     * @return Sync object handle (0 on failure)
     */
    public long createFenceSync() {
        return OpenGLCallMapper.glFenceSync(0x9117, 0); // GL_SYNC_GPU_COMMANDS_COMPLETE
    }
    
    /**
     * Wait for fence with timeout.
     * 
     * @param sync Sync object
     * @param timeoutNanos Timeout in nanoseconds (0 = check only, don't wait)
     * @return true if signaled, false if timeout
     */
    public boolean waitSync(long sync, long timeoutNanos) {
        if (sync == 0) return true;
        int result = OpenGLCallMapper.glClientWaitSync(sync, 0x00000001, timeoutNanos);
        // GL_ALREADY_SIGNALED = 0x911A, GL_CONDITION_SATISFIED = 0x911C
        return result == 0x911A || result == 0x911C;
    }
    
    /**
     * Check if fence is signaled (non-blocking).
     */
    public boolean isSyncSignaled(long sync) {
        return waitSync(sync, 0);
    }
    
    /**
     * Delete sync object.
     */
    public void deleteSync(long sync) {
        if (sync != 0) {
            OpenGLCallMapper.glDeleteSync(sync);
        }
    }
    
    // ─────────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────────────────
    
    @Override
    public void shutdown() {
        // Clear persistent buffer registry
        // Actual buffer deletion is done by caller or GL context destruction
        PERSISTENT_BUFFERS.clear();
    }
}
