package stellar.snow.astralis.api.vulkan.memory;

import stellar.snow.astralis.api.vulkan.backend.VulkanContext;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.KHRBufferDeviceAddress.*;
import static org.lwjgl.vulkan.KHRMaintenance4.*;
import static org.lwjgl.vulkan.KHRMaintenance5.*;
import static org.lwjgl.vulkan.KHRMaintenance6.*;
import static org.lwjgl.vulkan.EXTMemoryBudget.*;
import static org.lwjgl.vulkan.EXTMemoryPriority.*;
import static org.lwjgl.vulkan.EXTPageableDeviceLocalMemory.*;

/**
 * Production-grade Vulkan Memory Allocator.
 * 
 * Features:
 * - Automatic memory type selection based on usage
 * - Multiple allocation strategies per pool
 * - Dedicated allocations for large/optimal resources
 * - Linear (bump) allocators for per-frame staging
 * - Buffer device address support for bindless
 * - Memory defragmentation
 * - Statistics and debugging
 * - Thread-safe operations
 * 
 * Vulkan 1.2+ Features:
 * - Buffer device address with capture/replay
 * - Memory opaque capture address
 * - Dedicated allocation requirements query
 * 
 * Vulkan 1.3+ Features:
 * - Maintenance4 memory requirement queries (no resource creation)
 * - Synchronization2 memory barriers
 * 
 * Vulkan 1.4+ Features:
 * - Memory budget tracking (VK_EXT_memory_budget)
 * - Memory priority hints (VK_EXT_memory_priority)
 * - Pageable device local memory (VK_EXT_pageable_device_local_memory)
 * - Maintenance5/6 improvements
 */
public class VulkanMemoryAllocator {
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════════
    
    public static class Config {
        public long defaultBlockSize = 256 * 1024 * 1024L;     // 256 MB
        public long smallBlockSize = 64 * 1024 * 1024L;        // 64 MB for small allocs
        public long largeBlockSize = 512 * 1024 * 1024L;       // 512 MB for large pools
        public long dedicatedThreshold = 64 * 1024 * 1024L;    // 64 MB -> dedicated
        public long smallAllocationThreshold = 4 * 1024L;       // 4 KB
        public long linearBlockSize = 32 * 1024 * 1024L;       // 32 MB per-frame staging
        public int maxFramesInFlight = 3;
        public boolean enableDeviceAddress = true;
        public boolean enableDefragmentation = true;
        public boolean enableDebugMarkers = false;
        public boolean enableMemoryBudget = true;              // Vulkan 1.4
        public boolean enableMemoryPriority = true;            // Vulkan 1.4
        public boolean enableCaptureReplay = false;            // Vulkan 1.2
        public boolean respectBudgetLimits = true;             // Vulkan 1.4
        public float budgetWarningThreshold = 0.9f;            // Warn at 90% budget
        public MemoryBlock.AllocationStrategy defaultStrategy = MemoryBlock.AllocationStrategy.BEST_FIT;
        
        public Config() {}
        
        public static Config highPerformance() {
            Config c = new Config();
            c.defaultBlockSize = 512 * 1024 * 1024L;
            c.linearBlockSize = 64 * 1024 * 1024L;
            return c;
        }
        
        public static Config lowMemory() {
            Config c = new Config();
            c.defaultBlockSize = 64 * 1024 * 1024L;
            c.smallBlockSize = 16 * 1024 * 1024L;
            c.linearBlockSize = 8 * 1024 * 1024L;
            c.dedicatedThreshold = 32 * 1024 * 1024L;
            c.respectBudgetLimits = true;
            return c;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MEMORY USAGE HINTS
    // ═══════════════════════════════════════════════════════════════════════
    
    public enum MemoryUsage {
        /** GPU-only, optimal for render targets and static geometry */
        GPU_ONLY,
        /** CPU writable, GPU readable - dynamic uniforms, staging */
        CPU_TO_GPU,
        /** GPU writable, CPU readable - readback buffers */
        GPU_TO_CPU,
        /** CPU cached for frequent reads */
        CPU_CACHED,
        /** Linear per-frame allocation, auto-reset */
        CPU_TO_GPU_FRAME,
        /** Lazily allocated (mobile/tiled) */
        GPU_LAZY
    }
    
    public enum AllocationFlags {
        NONE(0),
        DEDICATED(1 << 0),              // Force dedicated allocation
        NEVER_DEDICATED(1 << 1),        // Never use dedicated
        MAPPED(1 << 2),                 // Persistently mapped
        HOST_SEQUENTIAL_WRITE(1 << 3),  // Optimize for sequential writes
        HOST_RANDOM_ACCESS(1 << 4),     // Optimize for random access
        CAN_ALIAS(1 << 5),              // Can share memory with other allocations
        STRATEGY_MIN_MEMORY(1 << 6),    // Minimize memory usage
        STRATEGY_MIN_TIME(1 << 7),      // Minimize allocation time
        // Vulkan 1.2+ flags
        DEVICE_ADDRESS(1 << 8),         // Enable buffer device address
        CAPTURE_REPLAY(1 << 9),         // Enable capture/replay
        // Vulkan 1.4+ flags
        HIGH_PRIORITY(1 << 10),         // High memory priority
        LOW_PRIORITY(1 << 11),          // Low memory priority (can be paged)
        PAGEABLE(1 << 12);              // Allow paging for device local memory
        
        public final int bits;
        AllocationFlags(int bits) { this.bits = bits; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN FEATURE SUPPORT
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Tracks available Vulkan memory features.
     */
    public static class VulkanMemoryFeatures {
        // Vulkan 1.2
        public boolean bufferDeviceAddress;
        public boolean bufferDeviceAddressCaptureReplay;
        public boolean bufferDeviceAddressMultiDevice;
        
        // Vulkan 1.3
        public boolean maintenance4;
        public boolean synchronization2;
        
        // Vulkan 1.4 / Extensions
        public boolean maintenance5;
        public boolean maintenance6;
        public boolean memoryBudget;
        public boolean memoryPriority;
        public boolean pageableDeviceLocalMemory;
        
        // Limits
        public long nonCoherentAtomSize;
        public long minMemoryMapAlignment;
        public long maxMemoryAllocationCount;
        public long maxMemoryAllocationSize;
        
        public static VulkanMemoryFeatures query(VkPhysicalDevice physicalDevice) {
            VulkanMemoryFeatures features = new VulkanMemoryFeatures();
            
            try (MemoryStack stack = MemoryStack.stackPush()) {
                // Query features
                VkPhysicalDeviceVulkan12Features features12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);
                
                VkPhysicalDeviceVulkan13Features features13 = VkPhysicalDeviceVulkan13Features.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES)
                    .pNext(features12.address());
                
                VkPhysicalDeviceMaintenance5FeaturesKHR maintenance5Features = VkPhysicalDeviceMaintenance5FeaturesKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_5_FEATURES_KHR)
                    .pNext(features13.address());
                
                VkPhysicalDeviceMaintenance6FeaturesKHR maintenance6Features = VkPhysicalDeviceMaintenance6FeaturesKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_6_FEATURES_KHR)
                    .pNext(maintenance5Features.address());
                
                VkPhysicalDeviceMemoryPriorityFeaturesEXT memoryPriorityFeatures = VkPhysicalDeviceMemoryPriorityFeaturesEXT.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PRIORITY_FEATURES_EXT)
                    .pNext(maintenance6Features.address());
                
                VkPhysicalDevicePageableDeviceLocalMemoryFeaturesEXT pageableFeatures = VkPhysicalDevicePageableDeviceLocalMemoryFeaturesEXT.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PAGEABLE_DEVICE_LOCAL_MEMORY_FEATURES_EXT)
                    .pNext(memoryPriorityFeatures.address());
                
                VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                    .pNext(pageableFeatures.address());
                
                vkGetPhysicalDeviceFeatures2(physicalDevice, features2);
                
                // Read feature results
                features.bufferDeviceAddress = features12.bufferDeviceAddress();
                features.bufferDeviceAddressCaptureReplay = features12.bufferDeviceAddressCaptureReplay();
                features.bufferDeviceAddressMultiDevice = features12.bufferDeviceAddressMultiDevice();
                
                features.maintenance4 = features13.maintenance4();
                features.synchronization2 = features13.synchronization2();
                
                features.maintenance5 = maintenance5Features.maintenance5();
                features.maintenance6 = maintenance6Features.maintenance6();
                
                features.memoryPriority = memoryPriorityFeatures.memoryPriority();
                features.pageableDeviceLocalMemory = pageableFeatures.pageableDeviceLocalMemory();
                
                // Check extensions
                features.memoryBudget = checkExtensionSupport(physicalDevice, VK_EXT_MEMORY_BUDGET_EXTENSION_NAME);
                
                // Query properties
                VkPhysicalDeviceVulkan13Properties props13 = VkPhysicalDeviceVulkan13Properties.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_PROPERTIES);
                
                VkPhysicalDeviceMaintenance4Properties maintenance4Props = VkPhysicalDeviceMaintenance4Properties.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_4_PROPERTIES)
                    .pNext(props13.address());
                
                VkPhysicalDeviceProperties2 props2 = VkPhysicalDeviceProperties2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2)
                    .pNext(maintenance4Props.address());
                
                vkGetPhysicalDeviceProperties2(physicalDevice, props2);
                
                features.nonCoherentAtomSize = props2.properties().limits().nonCoherentAtomSize();
                features.minMemoryMapAlignment = props2.properties().limits().minMemoryMapAlignment();
                features.maxMemoryAllocationCount = props2.properties().limits().maxMemoryAllocationCount();
                
                if (features.maintenance4) {
                    features.maxMemoryAllocationSize = maintenance4Props.maxBufferSize();
                } else {
                    features.maxMemoryAllocationSize = Long.MAX_VALUE;
                }
            }
            
            return features;
        }
        
        private static boolean checkExtensionSupport(VkPhysicalDevice physicalDevice, String extensionName) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                int[] count = new int[1];
                vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer) null, count, null);
                
                VkExtensionProperties.Buffer extensions = VkExtensionProperties.malloc(count[0], stack);
                vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer) null, count, extensions);
                
                for (int i = 0; i < count[0]; i++) {
                    if (extensions.get(i).extensionNameString().equals(extensionName)) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        @Override
        public String toString() {
            return String.format(
                "VulkanMemoryFeatures{deviceAddress=%b, maintenance4=%b, maintenance5=%b, " +
                "memoryBudget=%b, memoryPriority=%b, pageableDeviceLocal=%b}",
                bufferDeviceAddress, maintenance4, maintenance5,
                memoryBudget, memoryPriority, pageableDeviceLocalMemory
            );
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MEMORY BUDGET (Vulkan 1.4 / VK_EXT_memory_budget)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Memory budget information per heap.
     */
    public static class HeapBudget {
        public final int heapIndex;
        public final long heapSize;
        public long budget;        // How much we can use
        public long usage;         // How much is currently used by this process
        public long localUsage;    // Our tracked usage
        
        HeapBudget(int heapIndex, long heapSize) {
            this.heapIndex = heapIndex;
            this.heapSize = heapSize;
            this.budget = heapSize;
            this.usage = 0;
            this.localUsage = 0;
        }
        
        public long getAvailable() {
            return Math.max(0, budget - usage);
        }
        
        public double getUsageRatio() {
            return budget > 0 ? (double) usage / budget : 1.0;
        }
        
        public boolean isOverBudget() {
            return usage > budget;
        }
        
        public boolean isNearBudget(float threshold) {
            return getUsageRatio() >= threshold;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // ALLOCATION REQUEST & INFO
    // ═══════════════════════════════════════════════════════════════════════
    
    public static class AllocationCreateInfo {
        public long size;
        public long alignment;
        public int memoryTypeBits = ~0;           // From VkMemoryRequirements
        public MemoryUsage usage = MemoryUsage.GPU_ONLY;
        public int flags = 0;                      // AllocationFlags
        public int requiredFlags = 0;              // VkMemoryPropertyFlags
        public int preferredFlags = 0;             // VkMemoryPropertyFlags
        public float priority = 0.5f;              // Memory priority (0-1)
        public String debugName;
        // Vulkan 1.2+ capture/replay
        public long opaqueCaptureAddress;          // For replay
        
        public AllocationCreateInfo() {}
        
        public AllocationCreateInfo size(long s) { this.size = s; return this; }
        public AllocationCreateInfo alignment(long a) { this.alignment = a; return this; }
        public AllocationCreateInfo memoryTypeBits(int bits) { this.memoryTypeBits = bits; return this; }
        public AllocationCreateInfo usage(MemoryUsage u) { this.usage = u; return this; }
        public AllocationCreateInfo flags(int f) { this.flags = f; return this; }
        public AllocationCreateInfo dedicated() { this.flags |= AllocationFlags.DEDICATED.bits; return this; }
        public AllocationCreateInfo mapped() { this.flags |= AllocationFlags.MAPPED.bits; return this; }
        public AllocationCreateInfo debugName(String n) { this.debugName = n; return this; }
        public AllocationCreateInfo priority(float p) { this.priority = p; return this; }
        public AllocationCreateInfo highPriority() { this.priority = 1.0f; this.flags |= AllocationFlags.HIGH_PRIORITY.bits; return this; }
        public AllocationCreateInfo lowPriority() { this.priority = 0.0f; this.flags |= AllocationFlags.LOW_PRIORITY.bits; return this; }
        public AllocationCreateInfo captureReplay(long address) { this.opaqueCaptureAddress = address; this.flags |= AllocationFlags.CAPTURE_REPLAY.bits; return this; }
        
        public static AllocationCreateInfo buffer(long size, MemoryUsage usage) {
            return new AllocationCreateInfo().size(size).usage(usage);
        }
        
        public static AllocationCreateInfo image(long size) {
            return new AllocationCreateInfo().size(size).usage(MemoryUsage.GPU_ONLY);
        }
        
        public static AllocationCreateInfo staging(long size) {
            return new AllocationCreateInfo()
                .size(size)
                .usage(MemoryUsage.CPU_TO_GPU)
                .flags(AllocationFlags.HOST_SEQUENTIAL_WRITE.bits);
        }
        
        public static AllocationCreateInfo uniform(long size) {
            return new AllocationCreateInfo()
                .size(size)
                .usage(MemoryUsage.CPU_TO_GPU)
                .mapped();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // MEMORY POOL
    // ═══════════════════════════════════════════════════════════════════════
    
    private class MemoryPool {
        final int memoryTypeIndex;
        final int propertyFlags;
        final long preferredBlockSize;
        final MemoryBlock.AllocationStrategy strategy;
        final List<MemoryBlock> blocks = new CopyOnWriteArrayList<>();
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        
        // Vulkan 1.4 - Memory priority for this pool
        float poolPriority = 0.5f;
        
        // Statistics
        final AtomicLong totalAllocated = new AtomicLong();
        final AtomicLong totalBlockMemory = new AtomicLong();
        final AtomicInteger allocationCount = new AtomicInteger();
        
        MemoryPool(int memoryTypeIndex, int propertyFlags, long blockSize, 
                   MemoryBlock.AllocationStrategy strategy) {
            this.memoryTypeIndex = memoryTypeIndex;
            this.propertyFlags = propertyFlags;
            this.preferredBlockSize = blockSize;
            this.strategy = strategy;
        }
        
        MemoryAllocation allocate(long size, long alignment, String debugName, float priority) {
            // Check budget before allocating (Vulkan 1.4)
            if (config.respectBudgetLimits && vulkanFeatures.memoryBudget) {
                int heapIndex = memoryTypeToHeap[memoryTypeIndex];
                HeapBudget budget = heapBudgets[heapIndex];
                if (budget.getAvailable() < size) {
                    updateMemoryBudget(); // Refresh
                    if (budget.getAvailable() < size) {
                        return null; // Over budget
                    }
                }
            }
            
            // Try existing blocks first (read lock)
            lock.readLock().lock();
            try {
                for (MemoryBlock block : blocks) {
                    MemoryAllocation alloc = block.allocate(size, alignment, debugName);
                    if (alloc != null) {
                        updateStats(alloc, true);
                        return alloc;
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
            
            // Need new block (write lock)
            lock.writeLock().lock();
            try {
                // Double-check after acquiring write lock
                for (MemoryBlock block : blocks) {
                    MemoryAllocation alloc = block.allocate(size, alignment, debugName);
                    if (alloc != null) {
                        updateStats(alloc, true);
                        return alloc;
                    }
                }
                
                // Create new block with priority support
                long blockSize = calculateBlockSize(size);
                MemoryBlock newBlock = createMemoryBlock(blockSize, priority);
                blocks.add(newBlock);
                totalBlockMemory.addAndGet(blockSize);
                
                MemoryAllocation alloc = newBlock.allocate(size, alignment, debugName);
                if (alloc != null) {
                    updateStats(alloc, true);
                }
                return alloc;
                
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        MemoryAllocation allocate(long size, long alignment, String debugName) {
            return allocate(size, alignment, debugName, poolPriority);
        }
        
        private MemoryBlock createMemoryBlock(long blockSize, float priority) {
            MemoryBlock.MemoryPriority blockPriority = MemoryBlock.MemoryPriority.NORMAL;
            if (priority >= 0.75f) blockPriority = MemoryBlock.MemoryPriority.HIGH;
            else if (priority >= 0.9f) blockPriority = MemoryBlock.MemoryPriority.HIGHEST;
            else if (priority <= 0.25f) blockPriority = MemoryBlock.MemoryPriority.LOW;
            else if (priority <= 0.1f) blockPriority = MemoryBlock.MemoryPriority.LOWEST;
            
            EnumSet<MemoryBlock.MemoryAllocateFlags> allocFlags = EnumSet.noneOf(MemoryBlock.MemoryAllocateFlags.class);
            if (config.enableDeviceAddress && vulkanFeatures.bufferDeviceAddress) {
                allocFlags.add(MemoryBlock.MemoryAllocateFlags.DEVICE_ADDRESS);
            }
            if (config.enableCaptureReplay && vulkanFeatures.bufferDeviceAddressCaptureReplay) {
                allocFlags.add(MemoryBlock.MemoryAllocateFlags.DEVICE_ADDRESS_CAPTURE_REPLAY);
            }
            
            return new MemoryBlock(
                context.device,
                context.physicalDevice,
                nextBlockId.getAndIncrement(),
                blockSize,
                memoryTypeIndex,
                strategy,
                blockPriority,
                allocFlags,
                0
            );
        }
        
        void free(MemoryAllocation allocation) {
            for (MemoryBlock block : blocks) {
                if (block.getId() == allocation.getBlockId()) {
                    block.free(allocation);
                    updateStats(allocation, false);
                    return;
                }
            }
        }
        
        private void updateStats(MemoryAllocation alloc, boolean isAlloc) {
            if (isAlloc) {
                totalAllocated.addAndGet(alloc.getAlignedSize());
                allocationCount.incrementAndGet();
            } else {
                totalAllocated.addAndGet(-alloc.getAlignedSize());
                allocationCount.decrementAndGet();
            }
        }
        
        private long calculateBlockSize(long requiredSize) {
            if (requiredSize > preferredBlockSize) {
                // Round up to next power of 2
                return Long.highestOneBit(requiredSize - 1) << 1;
            }
            return preferredBlockSize;
        }
        
        void defragment() {
            lock.writeLock().lock();
            try {
                // Remove empty blocks (keep at least one)
                if (blocks.size() > 1) {
                    blocks.removeIf(block -> {
                        if (block.getAllocationCount() == 0) {
                            totalBlockMemory.addAndGet(-block.getTotalSize());
                            block.destroy();
                            return true;
                        }
                        return false;
                    });
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        // Vulkan 1.4 - Update priority for pageable memory
        void updatePriority(float newPriority) {
            if (!vulkanFeatures.pageableDeviceLocalMemory) return;
            
            poolPriority = newPriority;
            for (MemoryBlock block : blocks) {
                block.setPageablePriority(newPriority);
            }
        }
        
        void destroy() {
            lock.writeLock().lock();
            try {
                for (MemoryBlock block : blocks) {
                    block.destroy();
                }
                blocks.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        PoolStats getStats() {
            return new PoolStats(
                memoryTypeIndex,
                propertyFlags,
                blocks.size(),
                totalBlockMemory.get(),
                totalAllocated.get(),
                allocationCount.get(),
                poolPriority
            );
        }
    }
    
    public record PoolStats(
        int memoryTypeIndex,
        int propertyFlags,
        int blockCount,
        long totalMemory,
        long allocatedMemory,
        int allocationCount,
        float priority
    ) {}
    
    // ═══════════════════════════════════════════════════════════════════════
    // LINEAR ALLOCATOR (Per-Frame Staging)
    // ═══════════════════════════════════════════════════════════════════════
    
    private class LinearAllocator {
        final int memoryTypeIndex;
        final MemoryBlock[] frameBlocks;
        final AtomicLong[] frameOffsets;
        int currentFrame = 0;
        
        LinearAllocator(int memoryTypeIndex, int frameCount, long blockSize) {
            this.memoryTypeIndex = memoryTypeIndex;
            this.frameBlocks = new MemoryBlock[frameCount];
            this.frameOffsets = new AtomicLong[frameCount];
            
            for (int i = 0; i < frameCount; i++) {
                frameBlocks[i] = new MemoryBlock(
                    context.device,
                    context.physicalDevice,
                    nextBlockId.getAndIncrement(),
                    blockSize,
                    memoryTypeIndex,
                    MemoryBlock.AllocationStrategy.LINEAR,
                    MemoryBlock.MemoryPriority.NORMAL,
                    config.enableDeviceAddress && vulkanFeatures.bufferDeviceAddress
                        ? EnumSet.of(MemoryBlock.MemoryAllocateFlags.DEVICE_ADDRESS)
                        : EnumSet.noneOf(MemoryBlock.MemoryAllocateFlags.class),
                    0
                );
                frameOffsets[i] = new AtomicLong(0);
            }
        }
        
        MemoryAllocation allocate(long size, long alignment, String debugName) {
            return frameBlocks[currentFrame].allocate(size, alignment, debugName);
        }
        
        void nextFrame() {
            currentFrame = (currentFrame + 1) % frameBlocks.length;
            frameBlocks[currentFrame].resetLinear();
            frameOffsets[currentFrame].set(0);
        }
        
        void destroy() {
            for (MemoryBlock block : frameBlocks) {
                if (block != null) {
                    block.destroy();
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEDICATED ALLOCATION TRACKING
    // ═══════════════════════════════════════════════════════════════════════
    
    private static class DedicatedAllocation {
        final long memoryHandle;
        final long size;
        final int memoryTypeIndex;
        final long mappedPointer;
        final MemoryAllocation allocation;
        // Vulkan 1.2+ capture/replay
        final long opaqueCaptureAddress;
        
        DedicatedAllocation(long memoryHandle, long size, int memoryTypeIndex, 
                           long mappedPointer, MemoryAllocation allocation, long opaqueCaptureAddress) {
            this.memoryHandle = memoryHandle;
            this.size = size;
            this.memoryTypeIndex = memoryTypeIndex;
            this.mappedPointer = mappedPointer;
            this.allocation = allocation;
            this.opaqueCaptureAddress = opaqueCaptureAddress;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════
    
    private final VulkanContext context;
    private final Config config;
    
    // Vulkan 1.2+ feature support
    private final VulkanMemoryFeatures vulkanFeatures;
    
    // Memory properties
    private final VkPhysicalDeviceMemoryProperties memoryProperties;
    private final int memoryTypeCount;
    private final int memoryHeapCount;
    private final long[] heapSizes;
    private final AtomicLong[] heapUsage;
    private final int[] memoryTypeToHeap;
    
    // Vulkan 1.4 - Memory budgets
    private final HeapBudget[] heapBudgets;
    private volatile long lastBudgetUpdate;
    private static final long BUDGET_UPDATE_INTERVAL_NS = 100_000_000L; // 100ms
    
    // Pools by memory type
    private final Map<Integer, MemoryPool> pools = new ConcurrentHashMap<>();
    
    // Linear allocators for per-frame staging
    private final Map<Integer, LinearAllocator> linearAllocators = new ConcurrentHashMap<>();
    
    // Dedicated allocations
    private final Map<Long, DedicatedAllocation> dedicatedAllocations = new ConcurrentHashMap<>();
    
    // Counters
    private final AtomicInteger nextBlockId = new AtomicInteger(0);
    private final AtomicLong frameCounter = new AtomicLong(0);
    
    // Global statistics
    private final AtomicLong totalAllocatedBytes = new AtomicLong(0);
    private final AtomicLong totalAllocationCount = new AtomicLong(0);
    private final AtomicLong dedicatedAllocationBytes = new AtomicLong(0);
    private final AtomicInteger dedicatedAllocationCount = new AtomicInteger(0);
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTION
    // ═══════════════════════════════════════════════════════════════════════
    
    public VulkanMemoryAllocator(VulkanContext context) {
        this(context, new Config());
    }
    
    public VulkanMemoryAllocator(VulkanContext context, Config config) {
        this.context = context;
        this.config = config;
        
        // Query Vulkan features
        this.vulkanFeatures = VulkanMemoryFeatures.query(context.physicalDevice);
        
        // Query memory properties
        this.memoryProperties = VkPhysicalDeviceMemoryProperties.calloc();
        vkGetPhysicalDeviceMemoryProperties(context.physicalDevice, memoryProperties);
        
        this.memoryTypeCount = memoryProperties.memoryTypeCount();
        this.memoryHeapCount = memoryProperties.memoryHeapCount();
        
        this.heapSizes = new long[memoryHeapCount];
        this.heapUsage = new AtomicLong[memoryHeapCount];
        this.heapBudgets = new HeapBudget[memoryHeapCount];
        
        for (int i = 0; i < memoryHeapCount; i++) {
            heapSizes[i] = memoryProperties.memoryHeaps(i).size();
            heapUsage[i] = new AtomicLong(0);
            heapBudgets[i] = new HeapBudget(i, heapSizes[i]);
        }
        
        this.memoryTypeToHeap = new int[memoryTypeCount];
        for (int i = 0; i < memoryTypeCount; i++) {
            memoryTypeToHeap[i] = memoryProperties.memoryTypes(i).heapIndex();
        }
        
        // Initial budget update
        if (vulkanFeatures.memoryBudget) {
            updateMemoryBudget();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.4 - MEMORY BUDGET
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Update memory budget information from the driver.
     */
    public void updateMemoryBudget() {
        if (!vulkanFeatures.memoryBudget) return;
        
        long now = System.nanoTime();
        if (now - lastBudgetUpdate < BUDGET_UPDATE_INTERVAL_NS) return;
        lastBudgetUpdate = now;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryBudgetPropertiesEXT budgetProps = VkPhysicalDeviceMemoryBudgetPropertiesEXT.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT);
            
            VkPhysicalDeviceMemoryProperties2 memProps2 = VkPhysicalDeviceMemoryProperties2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2)
                .pNext(budgetProps.address());
            
            vkGetPhysicalDeviceMemoryProperties2(context.physicalDevice, memProps2);
            
            for (int i = 0; i < memoryHeapCount; i++) {
                heapBudgets[i].budget = budgetProps.heapBudget(i);
                heapBudgets[i].usage = budgetProps.heapUsage(i);
                heapBudgets[i].localUsage = heapUsage[i].get();
            }
        }
    }
    
    /**
     * Get budget for a specific heap.
     */
    public HeapBudget getHeapBudget(int heapIndex) {
        if (heapIndex < 0 || heapIndex >= memoryHeapCount) {
            throw new IllegalArgumentException("Invalid heap index: " + heapIndex);
        }
        return heapBudgets[heapIndex];
    }
    
    /**
     * Check if any heap is over budget.
     */
    public boolean isAnyHeapOverBudget() {
        updateMemoryBudget();
        for (HeapBudget budget : heapBudgets) {
            if (budget.isOverBudget()) return true;
        }
        return false;
    }
    
    /**
     * Check if any heap is near budget.
     */
    public boolean isAnyHeapNearBudget() {
        updateMemoryBudget();
        for (HeapBudget budget : heapBudgets) {
            if (budget.isNearBudget(config.budgetWarningThreshold)) return true;
        }
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.3 - MAINTENANCE4 MEMORY REQUIREMENTS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Query buffer memory requirements without creating a buffer (Vulkan 1.3+).
     */
    public MemoryRequirementsInfo queryBufferMemoryRequirements(long size, int usage, int createFlags) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (vulkanFeatures.maintenance4) {
                // Vulkan 1.3 path - no buffer creation needed
                VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size)
                    .usage(usage)
                    .flags(createFlags)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
                
                VkDeviceBufferMemoryRequirements deviceReqs = VkDeviceBufferMemoryRequirements.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_BUFFER_MEMORY_REQUIREMENTS)
                    .pCreateInfo(bufferInfo);
                
                VkMemoryDedicatedRequirements dedicatedReqs = VkMemoryDedicatedRequirements.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_REQUIREMENTS);
                
                VkMemoryRequirements2 memReqs2 = VkMemoryRequirements2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2)
                    .pNext(dedicatedReqs.address());
                
                vkGetDeviceBufferMemoryRequirements(context.device, deviceReqs, memReqs2);
                
                VkMemoryRequirements reqs = memReqs2.memoryRequirements();
                return new MemoryRequirementsInfo(
                    reqs.size(),
                    reqs.alignment(),
                    reqs.memoryTypeBits(),
                    dedicatedReqs.prefersDedicatedAllocation(),
                    dedicatedReqs.requiresDedicatedAllocation()
                );
            } else {
                // Fallback - create temporary buffer
                VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size)
                    .usage(usage)
                    .flags(createFlags)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
                
                LongBuffer pBuffer = stack.mallocLong(1);
                vkCreateBuffer(context.device, bufferInfo, null, pBuffer);
                long buffer = pBuffer.get(0);
                
                VkMemoryRequirements reqs = VkMemoryRequirements.malloc(stack);
                vkGetBufferMemoryRequirements(context.device, buffer, reqs);
                
                MemoryRequirementsInfo result = new MemoryRequirementsInfo(
                    reqs.size(),
                    reqs.alignment(),
                    reqs.memoryTypeBits(),
                    false,
                    false
                );
                
                vkDestroyBuffer(context.device, buffer, null);
                return result;
            }
        }
    }
    
    /**
     * Query image memory requirements without creating an image (Vulkan 1.3+).
     */
    public MemoryRequirementsInfo queryImageMemoryRequirements(VkImageCreateInfo imageInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (vulkanFeatures.maintenance4) {
                // Vulkan 1.3 path
                VkDeviceImageMemoryRequirements deviceReqs = VkDeviceImageMemoryRequirements.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_IMAGE_MEMORY_REQUIREMENTS)
                    .pCreateInfo(imageInfo);
                
                VkMemoryDedicatedRequirements dedicatedReqs = VkMemoryDedicatedRequirements.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_REQUIREMENTS);
                
                VkMemoryRequirements2 memReqs2 = VkMemoryRequirements2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2)
                    .pNext(dedicatedReqs.address());
                
                vkGetDeviceImageMemoryRequirements(context.device, deviceReqs, memReqs2);
                
                VkMemoryRequirements reqs = memReqs2.memoryRequirements();
                return new MemoryRequirementsInfo(
                    reqs.size(),
                    reqs.alignment(),
                    reqs.memoryTypeBits(),
                    dedicatedReqs.prefersDedicatedAllocation(),
                    dedicatedReqs.requiresDedicatedAllocation()
                );
            } else {
                // Fallback - create temporary image
                LongBuffer pImage = stack.mallocLong(1);
                vkCreateImage(context.device, imageInfo, null, pImage);
                long image = pImage.get(0);
                
                VkMemoryRequirements reqs = VkMemoryRequirements.malloc(stack);
                vkGetImageMemoryRequirements(context.device, image, reqs);
                
                MemoryRequirementsInfo result = new MemoryRequirementsInfo(
                    reqs.size(),
                    reqs.alignment(),
                    reqs.memoryTypeBits(),
                    false,
                    false
                );
                
                vkDestroyImage(context.device, image, null);
                return result;
            }
        }
    }
    
    public record MemoryRequirementsInfo(
        long size,
        long alignment,
        int memoryTypeBits,
        boolean prefersDedicatedAllocation,
        boolean requiresDedicatedAllocation
    ) {}
    
    // ═══════════════════════════════════════════════════════════════════════
    // MEMORY TYPE SELECTION
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Find optimal memory type for the given requirements.
     */
    public int findMemoryType(int memoryTypeBits, MemoryUsage usage) {
        int required = 0;
        int preferred = 0;
        
        switch (usage) {
            case GPU_ONLY -> {
                required = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
            }
            case CPU_TO_GPU, CPU_TO_GPU_FRAME -> {
                required = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
                preferred = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT | 
                           VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
            }
            case GPU_TO_CPU -> {
                required = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
                preferred = VK_MEMORY_PROPERTY_HOST_CACHED_BIT |
                           VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
            }
            case CPU_CACHED -> {
                required = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
                          VK_MEMORY_PROPERTY_HOST_CACHED_BIT;
                preferred = VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
            }
            case GPU_LAZY -> {
                required = VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT;
                preferred = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
            }
        }
        
        return findMemoryType(memoryTypeBits, required, preferred);
    }
    
    public int findMemoryType(int memoryTypeBits, int requiredFlags, int preferredFlags) {
        int bestType = -1;
        int bestScore = -1;
        
        // Update budget info for better selection (Vulkan 1.4)
        if (vulkanFeatures.memoryBudget) {
            updateMemoryBudget();
        }
        
        for (int i = 0; i < memoryTypeCount; i++) {
            // Check if this type is allowed
            if ((memoryTypeBits & (1 << i)) == 0) continue;
            
            int flags = memoryProperties.memoryTypes(i).propertyFlags();
            
            // Must have all required flags
            if ((flags & requiredFlags) != requiredFlags) continue;
            
            // Score based on preferred flags and heap availability
            int score = Integer.bitCount(flags & preferredFlags) * 100;
            
            // Prefer device-local
            if ((flags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0) {
                score += 1000;
            }
            
            // Consider heap budget (Vulkan 1.4)
            int heapIndex = memoryTypeToHeap[i];
            if (vulkanFeatures.memoryBudget) {
                HeapBudget budget = heapBudgets[heapIndex];
                if (budget.isOverBudget()) {
                    score -= 500; // Penalize over-budget heaps
                } else {
                    // Prefer heaps with more available budget
                    double availRatio = (double) budget.getAvailable() / budget.heapSize;
                    score += (int) (availRatio * 100);
                }
            } else {
                // Fallback: prefer heaps with more available space
                long available = heapSizes[heapIndex] - heapUsage[heapIndex].get();
                score += (int) Math.min(available / (1024 * 1024), 100);
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestType = i;
            }
        }
        
        if (bestType == -1) {
            // Fallback: find any type with required flags
            for (int i = 0; i < memoryTypeCount; i++) {
                if ((memoryTypeBits & (1 << i)) == 0) continue;
                int flags = memoryProperties.memoryTypes(i).propertyFlags();
                if ((flags & requiredFlags) == requiredFlags) {
                    return i;
                }
            }
        }
        
        return bestType;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // ALLOCATION
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Allocate memory for any usage.
     */
    public MemoryAllocation allocate(AllocationCreateInfo info) {
        // Find memory type
        int memoryType = findMemoryType(info.memoryTypeBits, info.usage);
        if (memoryType < 0) {
            memoryType = findMemoryType(info.memoryTypeBits, info.requiredFlags, info.preferredFlags);
        }
        if (memoryType < 0) {
            throw new RuntimeException("No suitable memory type found for: " + info.debugName);
        }
        
        // Check against max allocation size (Vulkan 1.3+)
        if (vulkanFeatures.maintenance4 && info.size > vulkanFeatures.maxMemoryAllocationSize) {
            throw new RuntimeException("Allocation size " + info.size + 
                " exceeds maximum " + vulkanFeatures.maxMemoryAllocationSize);
        }
        
        // Determine allocation strategy
        boolean useDedicated = (info.flags & AllocationFlags.DEDICATED.bits) != 0 ||
            (info.size >= config.dedicatedThreshold && 
             (info.flags & AllocationFlags.NEVER_DEDICATED.bits) == 0);
        
        boolean useLinear = info.usage == MemoryUsage.CPU_TO_GPU_FRAME;
        
        // Allocate
        MemoryAllocation allocation;
        
        if (useDedicated) {
            allocation = allocateDedicated(info.size, memoryType, info.debugName, info.priority, info.opaqueCaptureAddress);
        } else if (useLinear) {
            allocation = allocateLinear(info.size, info.alignment, memoryType, info.debugName);
        } else {
            allocation = allocateFromPool(info.size, info.alignment, memoryType, info.debugName, info.priority);
        }
        
        if (allocation == null) {
            throw new RuntimeException("Failed to allocate " + info.size + " bytes: " + info.debugName);
        }
        
        // Update heap usage
        int heapIndex = memoryTypeToHeap[allocation.getMemoryTypeIndex()];
        heapUsage[heapIndex].addAndGet(allocation.getAlignedSize());
        heapBudgets[heapIndex].localUsage = heapUsage[heapIndex].get();
        totalAllocatedBytes.addAndGet(allocation.getAlignedSize());
        totalAllocationCount.incrementAndGet();
        
        return allocation;
    }
    
    /**
     * Allocate memory for a buffer with automatic binding.
     */
    public MemoryAllocation allocateForBuffer(long buffer, MemoryUsage usage, String debugName) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(context.device, buffer, memReqs);
            
            AllocationCreateInfo info = new AllocationCreateInfo()
                .size(memReqs.size())
                .alignment(memReqs.alignment())
                .memoryTypeBits((int) memReqs.memoryTypeBits())
                .usage(usage)
                .debugName(debugName);
            
            // Check for dedicated allocation requirement (Vulkan 1.1+)
            if (context.supportsVK12()) {
                VkMemoryDedicatedRequirements dedicatedReqs = VkMemoryDedicatedRequirements.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_REQUIREMENTS);
                
                VkMemoryRequirements2 memReqs2 = VkMemoryRequirements2.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2)
                    .pNext(dedicatedReqs.address());
                
                VkBufferMemoryRequirementsInfo2 bufferInfo = VkBufferMemoryRequirementsInfo2.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_REQUIREMENTS_INFO_2)
                    .buffer(buffer);
                
                vkGetBufferMemoryRequirements2(context.device, bufferInfo, memReqs2);
                
                if (dedicatedReqs.requiresDedicatedAllocation()) {
                    info.flags |= AllocationFlags.DEDICATED.bits;
                }
            }
            
            MemoryAllocation allocation = allocate(info);
            
            // Bind buffer to memory
            int result = vkBindBufferMemory(context.device, buffer,
                allocation.getMemoryHandle(), allocation.getOffset());
            
            if (result != VK_SUCCESS) {
                free(allocation);
                throw new RuntimeException("Failed to bind buffer memory: " + result);
            }
            
            // Get buffer device address (Vulkan 1.2+)
            if (config.enableDeviceAddress && vulkanFeatures.bufferDeviceAddress) {
                VkBufferDeviceAddressInfo addressInfo = VkBufferDeviceAddressInfo.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_DEVICE_ADDRESS_INFO)
                    .buffer(buffer);
                
                long deviceAddress = vkGetBufferDeviceAddress(context.device, addressInfo);
                allocation.setBufferInfo(buffer, deviceAddress);
            }
            
            return allocation;
        }
    }
    
    /**
     * Allocate memory for an image with automatic binding.
     */
    public MemoryAllocation allocateForImage(long image, String debugName) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryRequirements memReqs = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(context.device, image, memReqs);
            
            AllocationCreateInfo info = new AllocationCreateInfo()
                .size(memReqs.size())
                .alignment(memReqs.alignment())
                .memoryTypeBits((int) memReqs.memoryTypeBits())
                .usage(MemoryUsage.GPU_ONLY)
                .debugName(debugName);
            
            // Check for dedicated allocation (Vulkan 1.1+)
            if (context.supportsVK12()) {
                VkMemoryDedicatedRequirements dedicatedReqs = VkMemoryDedicatedRequirements.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_REQUIREMENTS);
                
                VkMemoryRequirements2 memReqs2 = VkMemoryRequirements2.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2)
                    .pNext(dedicatedReqs.address());
                
                VkImageMemoryRequirementsInfo2 imageInfo = VkImageMemoryRequirementsInfo2.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_REQUIREMENTS_INFO_2)
                    .image(image);
                
                vkGetImageMemoryRequirements2(context.device, imageInfo, memReqs2);
                
                if (dedicatedReqs.requiresDedicatedAllocation() || 
                    dedicatedReqs.prefersDedicatedAllocation()) {
                    info.flags |= AllocationFlags.DEDICATED.bits;
                }
            }
            
            MemoryAllocation allocation = allocate(info);
            
            // Bind image to memory
            int result = vkBindImageMemory(context.device, image,
                allocation.getMemoryHandle(), allocation.getOffset());
            
            if (result != VK_SUCCESS) {
                free(allocation);
                throw new RuntimeException("Failed to bind image memory: " + result);
            }
            
            return allocation;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // INTERNAL ALLOCATION METHODS
    // ═══════════════════════════════════════════════════════════════════════
    
    private MemoryAllocation allocateFromPool(long size, long alignment, int memoryType, String debugName, float priority) {
        MemoryPool pool = getOrCreatePool(memoryType);
        return pool.allocate(size, alignment > 0 ? alignment : 256, debugName, priority);
    }
    
    private MemoryAllocation allocateFromPool(long size, long alignment, int memoryType, String debugName) {
        return allocateFromPool(size, alignment, memoryType, debugName, 0.5f);
    }
    
    private MemoryAllocation allocateLinear(long size, long alignment, int memoryType, String debugName) {
        LinearAllocator allocator = linearAllocators.computeIfAbsent(memoryType, 
            mt -> new LinearAllocator(mt, config.maxFramesInFlight, config.linearBlockSize));
        return allocator.allocate(size, alignment > 0 ? alignment : 256, debugName);
    }
    
    private MemoryAllocation allocateDedicated(long size, int memoryType, String debugName, float priority, long opaqueCaptureAddress) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long pNextChain = 0;
            
            // Vulkan 1.2 - Device address flags
            VkMemoryAllocateFlagsInfo flagsInfo = null;
            if (config.enableDeviceAddress && vulkanFeatures.bufferDeviceAddress) {
                int allocFlags = VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT;
                if (config.enableCaptureReplay && vulkanFeatures.bufferDeviceAddressCaptureReplay) {
                    allocFlags |= VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_CAPTURE_REPLAY_BIT;
                }
                
                flagsInfo = VkMemoryAllocateFlagsInfo.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_FLAGS_INFO)
                    .pNext(pNextChain)
                    .flags(allocFlags)
                    .deviceMask(0);
                pNextChain = flagsInfo.address();
            }
            
            // Vulkan 1.2 - Opaque capture address for replay
            VkMemoryOpaqueCaptureAddressAllocateInfo opaqueInfo = null;
            if (opaqueCaptureAddress != 0 && vulkanFeatures.bufferDeviceAddressCaptureReplay) {
                opaqueInfo = VkMemoryOpaqueCaptureAddressAllocateInfo.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_OPAQUE_CAPTURE_ADDRESS_ALLOCATE_INFO)
                    .pNext(pNextChain)
                    .opaqueCaptureAddress(opaqueCaptureAddress);
                pNextChain = opaqueInfo.address();
            }
            
            // Vulkan 1.4 - Memory priority
            VkMemoryPriorityAllocateInfoEXT priorityInfo = null;
            if (vulkanFeatures.memoryPriority && config.enableMemoryPriority) {
                priorityInfo = VkMemoryPriorityAllocateInfoEXT.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_PRIORITY_ALLOCATE_INFO_EXT)
                    .pNext(pNextChain)
                    .priority(priority);
                pNextChain = priorityInfo.address();
            }
            
            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.malloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .pNext(pNextChain)
                .allocationSize(size)
                .memoryTypeIndex(memoryType);
            
            LongBuffer pMemory = stack.mallocLong(1);
            int result = vkAllocateMemory(context.device, allocInfo, null, pMemory);
            
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate dedicated memory: " + translateVkResult(result));
            }
            
            long memoryHandle = pMemory.get(0);
            int propertyFlags = memoryProperties.memoryTypes(memoryType).propertyFlags();
            
            // Get opaque capture address for this allocation (Vulkan 1.2+)
            long capturedAddress = 0;
            if (vulkanFeatures.bufferDeviceAddressCaptureReplay && config.enableCaptureReplay) {
                VkDeviceMemoryOpaqueCaptureAddressInfo captureInfo = VkDeviceMemoryOpaqueCaptureAddressInfo.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_MEMORY_OPAQUE_CAPTURE_ADDRESS_INFO)
                    .pNext(0)
                    .memory(memoryHandle);
                capturedAddress = vkGetDeviceMemoryOpaqueCaptureAddress(context.device, captureInfo);
            }
            
            // Map if host visible
            long mappedPtr = 0;
            if ((propertyFlags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) {
                PointerBuffer pData = stack.mallocPointer(1);
                result = vkMapMemory(context.device, memoryHandle, 0, size, 0, pData);
                if (result == VK_SUCCESS) {
                    mappedPtr = pData.get(0);
                }
            }
            
            MemoryAllocation allocation = MemoryAllocation.builder()
                .memoryHandle(memoryHandle)
                .offset(0)
                .size(size)
                .alignedSize(size)
                .blockId(-1)
                .type(MemoryAllocation.AllocationType.DEDICATED)
                .memoryTypeIndex(memoryType)
                .propertyFlags(propertyFlags)
                .baseMappedPtr(mappedPtr)
                .ownsMapping(true)
                .debugName(debugName)
                .allocationFrame(frameCounter.get())
                .opaqueCaptureAddress(capturedAddress)
                .build();
            
            DedicatedAllocation dedicated = new DedicatedAllocation(
                memoryHandle, size, memoryType, mappedPtr, allocation, capturedAddress);
            dedicatedAllocations.put(memoryHandle, dedicated);
            
            dedicatedAllocationBytes.addAndGet(size);
            dedicatedAllocationCount.incrementAndGet();
            
            // Set pageable priority if supported (Vulkan 1.4)
            if (vulkanFeatures.pageableDeviceLocalMemory && 
                (propertyFlags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0) {
                vkSetDeviceMemoryPriorityEXT(context.device, memoryHandle, priority);
            }
            
            return allocation;
        }
    }
    
    private MemoryPool getOrCreatePool(int memoryType) {
        return pools.computeIfAbsent(memoryType, mt -> {
            int flags = memoryProperties.memoryTypes(mt).propertyFlags();
            long blockSize = config.defaultBlockSize;
            
            // Use smaller blocks for host-visible memory (often limited)
            if ((flags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) == 0) {
                blockSize = config.smallBlockSize;
            }
            
            return new MemoryPool(mt, flags, blockSize, config.defaultStrategy);
        });
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FREE
    // ═══════════════════════════════════════════════════════════════════════
    
    public void free(MemoryAllocation allocation) {
        if (allocation == null || allocation.isFreed()) return;
        
        // Update heap usage
        int heapIndex = memoryTypeToHeap[allocation.getMemoryTypeIndex()];
        heapUsage[heapIndex].addAndGet(-allocation.getAlignedSize());
        heapBudgets[heapIndex].localUsage = heapUsage[heapIndex].get();
        totalAllocatedBytes.addAndGet(-allocation.getAlignedSize());
        totalAllocationCount.decrementAndGet();
        
        if (allocation.isDedicated()) {
            freeDedicated(allocation);
        } else if (allocation.getType() == MemoryAllocation.AllocationType.LINEAR) {
            // Linear allocations are freed in bulk during nextFrame()
            allocation.markFreed();
        } else {
            MemoryPool pool = pools.get(allocation.getMemoryTypeIndex());
            if (pool != null) {
                pool.free(allocation);
            }
        }
    }
    
    private void freeDedicated(MemoryAllocation allocation) {
        DedicatedAllocation dedicated = dedicatedAllocations.remove(allocation.getMemoryHandle());
        if (dedicated == null) return;
        
        if (dedicated.mappedPointer != 0) {
            vkUnmapMemory(context.device, dedicated.memoryHandle);
        }
        
        vkFreeMemory(context.device, dedicated.memoryHandle, null);
        allocation.markFreed();
        
        dedicatedAllocationBytes.addAndGet(-dedicated.size);
        dedicatedAllocationCount.decrementAndGet();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.4 - PAGEABLE MEMORY PRIORITY
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Update priority for all allocations of a specific memory type.
     * Lower priority allocations may be paged out under memory pressure.
     */
    public void setMemoryTypePriority(int memoryType, float priority) {
        if (!vulkanFeatures.pageableDeviceLocalMemory) return;
        
        priority = Math.max(0.0f, Math.min(1.0f, priority));
        
        MemoryPool pool = pools.get(memoryType);
        if (pool != null) {
            pool.updatePriority(priority);
        }
    }
    
    /**
     * Reduce priority of less important allocations when under memory pressure.
     */
    public void handleMemoryPressure() {
        if (!vulkanFeatures.memoryBudget) return;
        
        updateMemoryBudget();
        
        for (int heapIndex = 0; heapIndex < memoryHeapCount; heapIndex++) {
            HeapBudget budget = heapBudgets[heapIndex];
            if (budget.isNearBudget(config.budgetWarningThreshold)) {
                // Find memory types using this heap and reduce their priority
                for (int mt = 0; mt < memoryTypeCount; mt++) {
                    if (memoryTypeToHeap[mt] == heapIndex) {
                        setMemoryTypePriority(mt, 0.25f); // Lower priority
                    }
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.3 - SYNCHRONIZATION2 HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Create a memory barrier using Synchronization2 (Vulkan 1.3+).
     */
    public VkMemoryBarrier2 createMemoryBarrier2(
            MemoryStack stack,
            long srcStageMask,
            long srcAccessMask,
            long dstStageMask,
            long dstAccessMask) {
        
        return VkMemoryBarrier2.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER_2)
            .srcStageMask(srcStageMask)
            .srcAccessMask(srcAccessMask)
            .dstStageMask(dstStageMask)
            .dstAccessMask(dstAccessMask);
    }
    
    /**
     * Create a buffer memory barrier using Synchronization2 (Vulkan 1.3+).
     */
    public VkBufferMemoryBarrier2 createBufferMemoryBarrier2(
            MemoryStack stack,
            long buffer,
            long offset,
            long size,
            long srcStageMask,
            long srcAccessMask,
            long dstStageMask,
            long dstAccessMask) {
        
        return VkBufferMemoryBarrier2.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(srcStageMask)
            .srcAccessMask(srcAccessMask)
            .dstStageMask(dstStageMask)
            .dstAccessMask(dstAccessMask)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .buffer(buffer)
            .offset(offset)
            .size(size);
    }
    
    /**
     * Submit a pipeline barrier using Synchronization2 (Vulkan 1.3+).
     */
    public void pipelineBarrier2(
            VkCommandBuffer commandBuffer,
            VkMemoryBarrier2.Buffer memoryBarriers,
            VkBufferMemoryBarrier2.Buffer bufferBarriers,
            VkImageMemoryBarrier2.Buffer imageBarriers) {
        
        if (!vulkanFeatures.synchronization2) {
            throw new UnsupportedOperationException("Synchronization2 not supported");
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDependencyInfo dependencyInfo = VkDependencyInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO)
                .pMemoryBarriers(memoryBarriers)
                .pBufferMemoryBarriers(bufferBarriers)
                .pImageMemoryBarriers(imageBarriers);
            
            vkCmdPipelineBarrier2(commandBuffer, dependencyInfo);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FRAME MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Call at the start of each frame to reset per-frame allocators.
     */
    public void beginFrame() {
        frameCounter.incrementAndGet();
        
        // Reset linear allocators
        for (LinearAllocator allocator : linearAllocators.values()) {
            allocator.nextFrame();
        }
        
        // Periodically update budget (Vulkan 1.4)
        if (vulkanFeatures.memoryBudget && (frameCounter.get() % 60) == 0) {
            updateMemoryBudget();
        }
    }
    
    public long getCurrentFrame() {
        return frameCounter.get();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEFRAGMENTATION
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Defragment memory pools by removing empty blocks.
     */
    public void defragment() {
        if (!config.enableDefragmentation) return;
        
        for (MemoryPool pool : pools.values()) {
            pool.defragment();
        }
    }
    
    /**
     * Get defragmentation statistics.
     */
    public DefragStats getDefragmentationStats() {
        long totalFragmented = 0;
        long totalWasted = 0;
        
        for (MemoryPool pool : pools.values()) {
            for (MemoryBlock block : pool.blocks) {
                long blockFree = block.getFreeBytes();
                if (block.getAllocationCount() > 0) {
                    totalFragmented += blockFree;
                } else {
                    totalWasted += block.getTotalSize();
                }
            }
        }
        
        return new DefragStats(totalFragmented, totalWasted, 
            totalFragmented + totalWasted > 0);
    }
    
    public record DefragStats(long fragmentedBytes, long wastedBytes, boolean recommended) {}
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════════════════
    
    public VulkanMemoryFeatures getVulkanFeatures() {
        return vulkanFeatures;
    }
    
    public AllocatorStats getStatistics() {
        List<PoolStats> poolStats = new ArrayList<>();
        for (MemoryPool pool : pools.values()) {
            poolStats.add(pool.getStats());
        }
        
        long[] heapUsed = new long[heapUsage.length];
        long[] heapBudgetValues = new long[heapUsage.length];
        for (int i = 0; i < heapUsage.length; i++) {
            heapUsed[i] = heapUsage[i].get();
            heapBudgetValues[i] = heapBudgets[i].budget;
        }
        
        return new AllocatorStats(
            totalAllocatedBytes.get(),
            totalAllocationCount.get(),
            dedicatedAllocationBytes.get(),
            dedicatedAllocationCount.get(),
            pools.size(),
            poolStats,
            heapSizes.clone(),
            heapUsed,
            heapBudgetValues,
            vulkanFeatures.memoryBudget
        );
    }
    
    public record AllocatorStats(
        long totalAllocatedBytes,
        long totalAllocationCount,
        long dedicatedBytes,
        int dedicatedCount,
        int poolCount,
        List<PoolStats> pools,
        long[] heapSizes,
        long[] heapUsage,
        long[] heapBudgets,
        boolean budgetAvailable
    ) {
        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Vulkan Memory Allocator ===\n");
            sb.append(String.format("Total: %s in %d allocations\n",
                formatBytes(totalAllocatedBytes), totalAllocationCount));
            sb.append(String.format("Dedicated: %s in %d allocations\n",
                formatBytes(dedicatedBytes), dedicatedCount));
            
            sb.append("\nHeaps:\n");
            for (int i = 0; i < heapSizes.length; i++) {
                if (budgetAvailable) {
                    double pct = 100.0 * heapUsage[i] / heapBudgets[i];
                    sb.append(String.format("  %d: %s / %s budget (%s total) (%.1f%%)\n",
                        i, formatBytes(heapUsage[i]), formatBytes(heapBudgets[i]), 
                        formatBytes(heapSizes[i]), pct));
                } else {
                    double pct = 100.0 * heapUsage[i] / heapSizes[i];
                    sb.append(String.format("  %d: %s / %s (%.1f%%)\n",
                        i, formatBytes(heapUsage[i]), formatBytes(heapSizes[i]), pct));
                }
            }
            
            sb.append("\nPools:\n");
            for (PoolStats ps : pools) {
                sb.append(String.format("  Type %d: %d blocks, %s in %d allocs (priority: %.2f)\n",
                    ps.memoryTypeIndex, ps.blockCount,
                    formatBytes(ps.allocatedMemory), ps.allocationCount, ps.priority));
            }
            
            return sb.toString();
        }
        
        private static String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════
    
    public void shutdown() {
        // Destroy pools
        for (MemoryPool pool : pools.values()) {
            pool.destroy();
        }
        pools.clear();
        
        // Destroy linear allocators
        for (LinearAllocator allocator : linearAllocators.values()) {
            allocator.destroy();
        }
        linearAllocators.clear();
        
        // Free dedicated allocations
        for (DedicatedAllocation dedicated : dedicatedAllocations.values()) {
            if (dedicated.mappedPointer != 0) {
                vkUnmapMemory(context.device, dedicated.memoryHandle);
            }
            vkFreeMemory(context.device, dedicated.memoryHandle, null);
        }
        dedicatedAllocations.clear();
        
        memoryProperties.free();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════
    
    private static String translateVkResult(int result) {
        return switch (result) {
            case VK_SUCCESS -> "VK_SUCCESS";
            case VK_ERROR_OUT_OF_HOST_MEMORY -> "VK_ERROR_OUT_OF_HOST_MEMORY";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "VK_ERROR_OUT_OF_DEVICE_MEMORY";
            case VK_ERROR_TOO_MANY_OBJECTS -> "VK_ERROR_TOO_MANY_OBJECTS";
            case VK_ERROR_MEMORY_MAP_FAILED -> "VK_ERROR_MEMORY_MAP_FAILED";
            default -> "VK_ERROR_" + result;
        };
    }
}
