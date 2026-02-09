package stellar.snow.astralis.api.vulkan.memory;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a single allocation within a memory block.
 * Supports:
 * - Buffer device addresses for bindless
 * - Mapped memory access with safety checks
 * - Reference counting for safe deallocation
 * - Debug tracking and validation
 */
public final class MemoryAllocation {
    
    // ═══════════════════════════════════════════════════════════════════════
    // ALLOCATION TYPES
    // ═══════════════════════════════════════════════════════════════════════
    
    public enum AllocationType {
        /** Standard sub-allocation from a block */
        SUBALLOCATED,
        /** Dedicated allocation (own VkDeviceMemory) */
        DEDICATED,
        /** Linear/bump allocation (fast, no individual free) */
        LINEAR,
        /** Pool allocation (fixed-size blocks) */
        POOL,
        /** External memory import */
        EXTERNAL
    }
    
    public enum MemoryUsage {
        /** GPU-only, fastest for rendering */
        GPU_ONLY,
        /** CPU to GPU transfers (staging, dynamic uniforms) */
        CPU_TO_GPU,
        /** GPU to CPU readback */
        GPU_TO_CPU,
        /** CPU access, cached for frequent reads */
        CPU_CACHED,
        /** Lazy allocation (mobile/tiled GPUs) */
        GPU_LAZILY_ALLOCATED
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CORE DATA
    // ═══════════════════════════════════════════════════════════════════════
    
    /** The underlying VkDeviceMemory handle */
    private final long memoryHandle;
    
    /** Offset within the memory block */
    private final long offset;
    
    /** Size of this allocation in bytes */
    private final long size;
    
    /** Actual size including alignment padding */
    private final long alignedSize;
    
    /** Required alignment for this allocation */
    private final long alignment;
    
    /** Parent block ID (-1 for dedicated allocations) */
    private final int blockId;
    
    /** Allocation type */
    private final AllocationType type;
    
    /** Memory usage hint */
    private final MemoryUsage usage;
    
    /** Memory type index */
    private final int memoryTypeIndex;
    
    /** Memory property flags */
    private final int propertyFlags;
    
    // ═══════════════════════════════════════════════════════════════════════
    // MAPPING
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Persistently mapped pointer (null if not mapped) */
    private volatile ByteBuffer mappedData;
    
    /** Base mapped pointer of the block (for calculating our slice) */
    private final long baseMappedPtr;
    
    /** Whether this allocation owns the mapping */
    private final boolean ownsMapping;
    
    // ═══════════════════════════════════════════════════════════════════════
    // BUFFER DEVICE ADDRESS (Bindless)
    // ═══════════════════════════════════════════════════════════════════════
    
    /** VkBuffer handle if this allocation backs a buffer */
    private long bufferHandle;
    
    /** Buffer device address for bindless access */
    private long deviceAddress;
    
    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE & DEBUG
    // ═══════════════════════════════════════════════════════════════════════
    
    /** Reference count for safe deallocation */
    private final AtomicInteger refCount = new AtomicInteger(1);
    
    /** Whether this allocation has been freed */
    private final AtomicBoolean freed = new AtomicBoolean(false);
    
    /** Frame when this allocation was made */
    private final long allocationFrame;
    
    /** Debug name for identification */
    private String debugName;
    
    /** Stack trace of allocation (debug builds only) */
    private StackTraceElement[] allocationStackTrace;
    
    /** User data attachment */
    private Object userData;
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTION
    // ═══════════════════════════════════════════════════════════════════════
    
    private MemoryAllocation(Builder builder) {
        this.memoryHandle = builder.memoryHandle;
        this.offset = builder.offset;
        this.size = builder.size;
        this.alignedSize = builder.alignedSize;
        this.alignment = builder.alignment;
        this.blockId = builder.blockId;
        this.type = builder.type;
        this.usage = builder.usage;
        this.memoryTypeIndex = builder.memoryTypeIndex;
        this.propertyFlags = builder.propertyFlags;
        this.baseMappedPtr = builder.baseMappedPtr;
        this.ownsMapping = builder.ownsMapping;
        this.allocationFrame = builder.allocationFrame;
        this.debugName = builder.debugName;
        
        // Create mapped slice if base is mapped
        if (baseMappedPtr != 0) {
            this.mappedData = createMappedSlice();
        }
        
        // Capture stack trace in debug mode
        if (MemoryDebugConfig.TRACK_ALLOCATIONS) {
            this.allocationStackTrace = Thread.currentThread().getStackTrace();
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private long memoryHandle;
        private long offset;
        private long size;
        private long alignedSize;
        private long alignment = 1;
        private int blockId = -1;
        private AllocationType type = AllocationType.SUBALLOCATED;
        private MemoryUsage usage = MemoryUsage.GPU_ONLY;
        private int memoryTypeIndex;
        private int propertyFlags;
        private long baseMappedPtr;
        private boolean ownsMapping;
        private long allocationFrame;
        private String debugName;
        
        public Builder memoryHandle(long handle) { this.memoryHandle = handle; return this; }
        public Builder offset(long offset) { this.offset = offset; return this; }
        public Builder size(long size) { this.size = size; return this; }
        public Builder alignedSize(long size) { this.alignedSize = size; return this; }
        public Builder alignment(long alignment) { this.alignment = alignment; return this; }
        public Builder blockId(int id) { this.blockId = id; return this; }
        public Builder type(AllocationType type) { this.type = type; return this; }
        public Builder usage(MemoryUsage usage) { this.usage = usage; return this; }
        public Builder memoryTypeIndex(int index) { this.memoryTypeIndex = index; return this; }
        public Builder propertyFlags(int flags) { this.propertyFlags = flags; return this; }
        public Builder baseMappedPtr(long ptr) { this.baseMappedPtr = ptr; return this; }
        public Builder ownsMapping(boolean owns) { this.ownsMapping = owns; return this; }
        public Builder allocationFrame(long frame) { this.allocationFrame = frame; return this; }
        public Builder debugName(String name) { this.debugName = name; return this; }
        
        public MemoryAllocation build() {
            if (alignedSize == 0) alignedSize = size;
            return new MemoryAllocation(this);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // ACCESSORS
    // ═══════════════════════════════════════════════════════════════════════
    
    public long getMemoryHandle() { return memoryHandle; }
    public long getOffset() { return offset; }
    public long getSize() { return size; }
    public long getAlignedSize() { return alignedSize; }
    public long getAlignment() { return alignment; }
    public int getBlockId() { return blockId; }
    public AllocationType getType() { return type; }
    public MemoryUsage getUsage() { return usage; }
    public int getMemoryTypeIndex() { return memoryTypeIndex; }
    public int getPropertyFlags() { return propertyFlags; }
    public long getAllocationFrame() { return allocationFrame; }
    public String getDebugName() { return debugName; }
    public void setDebugName(String name) { this.debugName = name; }
    public Object getUserData() { return userData; }
    public void setUserData(Object data) { this.userData = data; }
    
    public boolean isDedicated() { return type == AllocationType.DEDICATED; }
    public boolean isHostVisible() { return (propertyFlags & 0x02) != 0; } // VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
    public boolean isHostCoherent() { return (propertyFlags & 0x04) != 0; } // VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
    public boolean isDeviceLocal() { return (propertyFlags & 0x01) != 0; } // VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
    
    // ═══════════════════════════════════════════════════════════════════════
    // BUFFER DEVICE ADDRESS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Get the buffer device address for bindless access.
     * Only valid if a buffer has been bound to this allocation.
     */
    public long getDeviceAddress() {
        validateNotFreed();
        if (deviceAddress == 0) {
            throw new IllegalStateException("No buffer device address set. Bind a buffer first.");
        }
        return deviceAddress;
    }
    
    /**
     * Get device address with internal offset for bindless array access.
     */
    public long getDeviceAddress(long internalOffset) {
        return getDeviceAddress() + internalOffset;
    }
    
    /**
     * Set the buffer handle and device address (called by buffer creation).
     */
    public void setBufferInfo(long bufferHandle, long deviceAddress) {
        this.bufferHandle = bufferHandle;
        this.deviceAddress = deviceAddress;
    }
    
    public long getBufferHandle() {
        return bufferHandle;
    }
    
    public boolean hasDeviceAddress() {
        return deviceAddress != 0;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MAPPED MEMORY ACCESS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Check if this allocation is currently mapped.
     */
    public boolean isMapped() {
        return mappedData != null;
    }
    
    /**
     * Get the mapped ByteBuffer for direct CPU access.
     * The buffer's position is at the start of this allocation,
     * and its limit is at the end.
     */
    public ByteBuffer getMappedData() {
        validateNotFreed();
        if (mappedData == null) {
            throw new IllegalStateException("Memory is not mapped. Call map() first or use host-visible memory.");
        }
        return mappedData.duplicate(); // Return duplicate to preserve position/limit
    }
    
    /**
     * Get mapped data with bounds checking.
     */
    public ByteBuffer getMappedData(long internalOffset, long length) {
        validateNotFreed();
        if (mappedData == null) {
            throw new IllegalStateException("Memory is not mapped");
        }
        if (internalOffset < 0 || length < 0 || internalOffset + length > size) {
            throw new IndexOutOfBoundsException(
                String.format("Invalid range [%d, %d) for allocation of size %d", 
                    internalOffset, internalOffset + length, size)
            );
        }
        
        ByteBuffer slice = mappedData.duplicate();
        slice.position((int) internalOffset);
        slice.limit((int) (internalOffset + length));
        return slice.slice();
    }
    
    private ByteBuffer createMappedSlice() {
        if (baseMappedPtr == 0) return null;
        
        // Create a ByteBuffer view of our portion
        return org.lwjgl.system.MemoryUtil.memByteBuffer(
            baseMappedPtr + offset, 
            (int) size
        );
    }
    
    /**
     * Flush mapped memory range (required for non-coherent memory).
     */
    public void flush() {
        flush(0, size);
    }
    
    /**
     * Flush a range of mapped memory.
     */
    public void flush(long flushOffset, long flushSize) {
        validateNotFreed();
        if (!isMapped()) {
            throw new IllegalStateException("Cannot flush unmapped memory");
        }
        if (!isHostCoherent()) {
            // Actual flush would call vkFlushMappedMemoryRanges
            // This is typically handled by the allocator
        }
    }
    
    /**
     * Invalidate mapped memory range (required before reading non-coherent memory).
     */
    public void invalidate() {
        invalidate(0, size);
    }
    
    public void invalidate(long invOffset, long invSize) {
        validateNotFreed();
        if (!isMapped()) {
            throw new IllegalStateException("Cannot invalidate unmapped memory");
        }
        if (!isHostCoherent()) {
            // Actual invalidate would call vkInvalidateMappedMemoryRanges
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DATA WRITE HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Write float array to mapped memory.
     */
    public void writeFloats(long internalOffset, float[] data) {
        ByteBuffer buffer = getMappedData(internalOffset, (long) data.length * 4);
        buffer.asFloatBuffer().put(data);
    }
    
    /**
     * Write int array to mapped memory.
     */
    public void writeInts(long internalOffset, int[] data) {
        ByteBuffer buffer = getMappedData(internalOffset, (long) data.length * 4);
        buffer.asIntBuffer().put(data);
    }
    
    /**
     * Write byte array to mapped memory.
     */
    public void writeBytes(long internalOffset, byte[] data) {
        ByteBuffer buffer = getMappedData(internalOffset, data.length);
        buffer.put(data);
    }
    
    /**
     * Write another ByteBuffer to mapped memory.
     */
    public void writeBuffer(long internalOffset, ByteBuffer src) {
        ByteBuffer buffer = getMappedData(internalOffset, src.remaining());
        buffer.put(src);
    }
    
    /**
     * Zero-fill the allocation.
     */
    public void zero() {
        ByteBuffer buffer = getMappedData();
        for (int i = 0; i < size; i++) {
            buffer.put(i, (byte) 0);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // REFERENCE COUNTING
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Increment reference count.
     */
    public MemoryAllocation addRef() {
        validateNotFreed();
        refCount.incrementAndGet();
        return this;
    }
    
    /**
     * Decrement reference count. Returns true if this was the last reference.
     */
    public boolean release() {
        int remaining = refCount.decrementAndGet();
        if (remaining < 0) {
            throw new IllegalStateException("Reference count went negative for: " + debugName);
        }
        return remaining == 0;
    }
    
    public int getRefCount() {
        return refCount.get();
    }
    
    /**
     * Mark as freed (called by allocator).
     */
    void markFreed() {
        freed.set(true);
        mappedData = null;
    }
    
    public boolean isFreed() {
        return freed.get();
    }
    
    private void validateNotFreed() {
        if (freed.get()) {
            String msg = "Attempting to use freed allocation";
            if (debugName != null) {
                msg += ": " + debugName;
            }
            if (allocationStackTrace != null) {
                msg += "\nAllocated at:\n";
                for (StackTraceElement elem : allocationStackTrace) {
                    msg += "  " + elem + "\n";
                }
            }
            throw new IllegalStateException(msg);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEBUG & STRING
    // ═══════════════════════════════════════════════════════════════════════
    
    @Override
    public String toString() {
        return String.format(
            "MemoryAllocation{name='%s', block=%d, offset=%d, size=%d, type=%s, usage=%s, refs=%d, freed=%s}",
            debugName, blockId, offset, size, type, usage, refCount.get(), freed.get()
        );
    }
    
    /**
     * Get detailed debug info.
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MemoryAllocation {\n");
        sb.append("  name: ").append(debugName).append("\n");
        sb.append("  handle: 0x").append(Long.toHexString(memoryHandle)).append("\n");
        sb.append("  offset: ").append(offset).append(" (0x").append(Long.toHexString(offset)).append(")\n");
        sb.append("  size: ").append(size).append(" (").append(formatBytes(size)).append(")\n");
        sb.append("  alignedSize: ").append(alignedSize).append("\n");
        sb.append("  alignment: ").append(alignment).append("\n");
        sb.append("  blockId: ").append(blockId).append("\n");
        sb.append("  type: ").append(type).append("\n");
        sb.append("  usage: ").append(usage).append("\n");
        sb.append("  memoryTypeIndex: ").append(memoryTypeIndex).append("\n");
        sb.append("  propertyFlags: 0x").append(Integer.toHexString(propertyFlags)).append("\n");
        sb.append("  mapped: ").append(isMapped()).append("\n");
        sb.append("  deviceAddress: 0x").append(Long.toHexString(deviceAddress)).append("\n");
        sb.append("  refCount: ").append(refCount.get()).append("\n");
        sb.append("  freed: ").append(freed.get()).append("\n");
        sb.append("  allocationFrame: ").append(allocationFrame).append("\n");
        sb.append("}");
        return sb.toString();
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

/**
 * Debug configuration for memory tracking.
 */
class MemoryDebugConfig {
    static final boolean TRACK_ALLOCATIONS = Boolean.getBoolean("vulkan.memory.trackAllocations");
    static final boolean VALIDATE_LEAKS = Boolean.getBoolean("vulkan.memory.validateLeaks");
    static final boolean ZERO_ON_ALLOCATE = Boolean.getBoolean("vulkan.memory.zeroOnAllocate");
    static final boolean FILL_ON_FREE = Boolean.getBoolean("vulkan.memory.fillOnFree");
}
