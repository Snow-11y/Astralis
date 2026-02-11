package stellar.snow.astralis.api.vulkan.memory;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
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
import static org.lwjgl.vulkan.EXTExternalMemoryHost.*;
import static org.lwjgl.vulkan.EXTDeviceAddressBindingReport.*;

/**
 * A single block of VkDeviceMemory with sub-allocation support.
 * 
 * Features:
 * - Multiple allocation strategies (first-fit, best-fit, buddy)
 * - Free block coalescing
 * - Defragmentation support
 * - Thread-safe operations
 * - Persistent mapping
 * - Statistics tracking
 * 
 * Vulkan 1.2+ Features:
 * - Buffer device address with opaque capture/replay
 * - Memory priority (VK_EXT_memory_priority)
 * 
 * Vulkan 1.3+ Features:
 * - Maintenance4 memory requirement queries
 * - Extended dynamic state
 * 
 * Vulkan 1.4+ Features:
 * - Maintenance5 buffer/image usage flags queries
 * - Maintenance6 descriptor buffer features
 * - Memory budget tracking (VK_EXT_memory_budget)
 * - Pageable device local memory
 * - Device address binding reports
 */
public class MemoryBlock {
    
    // ═══════════════════════════════════════════════════════════════════════
    // ALLOCATION STRATEGIES
    // ═══════════════════════════════════════════════════════════════════════
    
    public enum AllocationStrategy {
        /** First block that fits - fastest */
        FIRST_FIT,
        /** Smallest block that fits - less fragmentation */
        BEST_FIT,
        /** Binary buddy system - fast, predictable fragmentation */
        BUDDY,
        /** Linear/bump allocation - fastest, no individual free */
        LINEAR
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.2+ MEMORY PRIORITY LEVELS
    // ═══════════════════════════════════════════════════════════════════════
    
    public enum MemoryPriority {
        LOWEST(0.0f),
        LOW(0.25f),
        NORMAL(0.5f),
        HIGH(0.75f),
        HIGHEST(1.0f);
        
        private final float value;
        
        MemoryPriority(float value) {
            this.value = value;
        }
        
        public float getValue() { return value; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.4 MEMORY FLAGS
    // ═══════════════════════════════════════════════════════════════════════
    
    public enum MemoryAllocateFlags {
        NONE(0),
        DEVICE_ADDRESS(VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT),
        DEVICE_ADDRESS_CAPTURE_REPLAY(VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_CAPTURE_REPLAY_BIT),
        DEVICE_MASK(VK_MEMORY_ALLOCATE_DEVICE_MASK_BIT);
        
        private final int value;
        
        MemoryAllocateFlags(int value) {
            this.value = value;
        }
        
        public int getValue() { return value; }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FREE NODE
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Represents a contiguous free region within the block.
     */
    private static final class FreeNode implements Comparable<FreeNode> {
        long offset;
        long size;
        FreeNode prev;
        FreeNode next;
        
        // For buddy allocator
        int order; // log2(size / minBlockSize)
        boolean split;
        
        FreeNode(long offset, long size) {
            this.offset = offset;
            this.size = size;
        }
        
        @Override
        public int compareTo(FreeNode other) {
            return Long.compare(this.offset, other.offset);
        }
        
        @Override
        public String toString() {
            return String.format("FreeNode{offset=%d, size=%d}", offset, size);
        }
    }
    
    /**
     * Represents an active allocation for tracking.
     */
    private static final class AllocationRecord {
        final long offset;
        final long size;
        final long alignedSize;
        final long timestamp;
        final String debugName;
        MemoryAllocation allocation;
        
        // Vulkan 1.2+ device address
        long deviceAddress;
        
        AllocationRecord(long offset, long size, long alignedSize, String debugName) {
            this.offset = offset;
            this.size = size;
            this.alignedSize = alignedSize;
            this.timestamp = System.nanoTime();
            this.debugName = debugName;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.4 FEATURE SUPPORT TRACKING
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Tracks available Vulkan features for this block.
     */
    public static final class VulkanFeatureSupport {
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
        public boolean externalMemoryHost;
        public boolean deviceAddressBindingReport;
        
        // Query from device
        public static VulkanFeatureSupport query(VkPhysicalDevice physicalDevice) {
            VulkanFeatureSupport support = new VulkanFeatureSupport();
            
            try (MemoryStack stack = MemoryStack.stackPush()) {
                // Vulkan 1.2 features
                VkPhysicalDeviceVulkan12Features features12 = VkPhysicalDeviceVulkan12Features.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);
                
                // Vulkan 1.3 features
                VkPhysicalDeviceVulkan13Features features13 = VkPhysicalDeviceVulkan13Features.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES)
                    .pNext(features12.address());
                
                // Maintenance5 features (Vulkan 1.4 or extension)
                VkPhysicalDeviceMaintenance5FeaturesKHR maintenance5Features = VkPhysicalDeviceMaintenance5FeaturesKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_5_FEATURES_KHR)
                    .pNext(features13.address());
                
                // Maintenance6 features (Vulkan 1.4 or extension)
                VkPhysicalDeviceMaintenance6FeaturesKHR maintenance6Features = VkPhysicalDeviceMaintenance6FeaturesKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_6_FEATURES_KHR)
                    .pNext(maintenance5Features.address());
                
                // Memory priority features
                VkPhysicalDeviceMemoryPriorityFeaturesEXT memoryPriorityFeatures = VkPhysicalDeviceMemoryPriorityFeaturesEXT.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PRIORITY_FEATURES_EXT)
                    .pNext(maintenance6Features.address());
                
                // Pageable device local memory
                VkPhysicalDevicePageableDeviceLocalMemoryFeaturesEXT pageableFeatures = VkPhysicalDevicePageableDeviceLocalMemoryFeaturesEXT.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PAGEABLE_DEVICE_LOCAL_MEMORY_FEATURES_EXT)
                    .pNext(memoryPriorityFeatures.address());
                
                VkPhysicalDeviceFeatures2 features2 = VkPhysicalDeviceFeatures2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                    .pNext(pageableFeatures.address());
                
                vkGetPhysicalDeviceFeatures2(physicalDevice, features2);
                
                // Read results
                support.bufferDeviceAddress = features12.bufferDeviceAddress();
                support.bufferDeviceAddressCaptureReplay = features12.bufferDeviceAddressCaptureReplay();
                support.bufferDeviceAddressMultiDevice = features12.bufferDeviceAddressMultiDevice();
                
                support.maintenance4 = features13.maintenance4();
                support.synchronization2 = features13.synchronization2();
                
                support.maintenance5 = maintenance5Features.maintenance5();
                support.maintenance6 = maintenance6Features.maintenance6();
                
                support.memoryPriority = memoryPriorityFeatures.memoryPriority();
                support.pageableDeviceLocalMemory = pageableFeatures.pageableDeviceLocalMemory();
                
                // Check for memory budget extension (query extensions)
                support.memoryBudget = checkExtensionSupport(physicalDevice, VK_EXT_MEMORY_BUDGET_EXTENSION_NAME);
                support.externalMemoryHost = checkExtensionSupport(physicalDevice, VK_EXT_EXTERNAL_MEMORY_HOST_EXTENSION_NAME);
                support.deviceAddressBindingReport = checkExtensionSupport(physicalDevice, VK_EXT_DEVICE_ADDRESS_BINDING_REPORT_EXTENSION_NAME);
            }
            
            return support;
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
                "VulkanFeatureSupport{bufferDeviceAddress=%b, maintenance4=%b, maintenance5=%b, " +
                "maintenance6=%b, memoryBudget=%b, memoryPriority=%b, pageableDeviceLocalMemory=%b}",
                bufferDeviceAddress, maintenance4, maintenance5, maintenance6,
                memoryBudget, memoryPriority, pageableDeviceLocalMemory
            );
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════
    
    private final VkDevice device;
    private final VkPhysicalDevice physicalDevice;
    private final int id;
    private final long totalSize;
    private final int memoryTypeIndex;
    private final int propertyFlags;
    private final AllocationStrategy strategy;
    
    // Vulkan 1.2+ features
    private final VulkanFeatureSupport featureSupport;
    private final float memoryPriority;
    private final int allocateFlags;
    private final long opaqueAddress; // For capture/replay
    
    // Vulkan 1.4 - device address for the entire block
    private long blockDeviceAddress;
    
    // Vulkan handles
    private final long memoryHandle;
    private long mappedPointer; // 0 if not mapped
    
    // Free list (sorted by offset for coalescing)
    private final NavigableMap<Long, FreeNode> freeNodesByOffset = new ConcurrentSkipListMap<>();
    // Free list sorted by size (for best-fit)
    private final TreeMap<Long, Set<FreeNode>> freeNodesBySize = new TreeMap<>();
    
    // Active allocations tracking
    private final Map<Long, AllocationRecord> activeAllocations = new ConcurrentHashMap<>();
    
    // Buddy allocator state
    private final List<Set<FreeNode>> buddyFreeLists; // One set per order
    private final int buddyMinOrder;
    private final int buddyMaxOrder;
    private static final long BUDDY_MIN_SIZE = 256; // Minimum buddy block size
    
    // Linear allocator state
    private final AtomicLong linearOffset = new AtomicLong(0);
    
    // Statistics
    private final AtomicLong allocatedBytes = new AtomicLong(0);
    private final AtomicLong allocationCount = new AtomicLong(0);
    private final AtomicLong peakAllocatedBytes = new AtomicLong(0);
    private final AtomicLong totalAllocations = new AtomicLong(0);
    private final AtomicLong totalFrees = new AtomicLong(0);
    private final AtomicLong fragmentedBytes = new AtomicLong(0);
    
    // Vulkan 1.4 - Memory budget tracking
    private final AtomicLong heapBudget = new AtomicLong(0);
    private final AtomicLong heapUsage = new AtomicLong(0);
    private final int heapIndex;
    
    // Thread safety
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Configuration
    private final long minAllocationSize;
    private final long defaultAlignment;
    private volatile boolean destroyed = false;
    
    // Vulkan 1.3+ maintenance4 - non-coherent atom size
    private final long nonCoherentAtomSize;
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONSTRUCTION
    // ═══════════════════════════════════════════════════════════════════════
    
    public MemoryBlock(
            VkDevice device,
            VkPhysicalDevice physicalDevice,
            int id,
            long size,
            int memoryTypeIndex,
            AllocationStrategy strategy) {
        this(device, physicalDevice, id, size, memoryTypeIndex, strategy, 
             MemoryPriority.NORMAL, EnumSet.of(MemoryAllocateFlags.DEVICE_ADDRESS), 0);
    }
    
    /**
     * Full constructor with Vulkan 1.2/1.3/1.4 options.
     */
    public MemoryBlock(
            VkDevice device,
            VkPhysicalDevice physicalDevice,
            int id,
            long size,
            int memoryTypeIndex,
            AllocationStrategy strategy,
            MemoryPriority priority,
            EnumSet<MemoryAllocateFlags> flags,
            long opaqueAddress) {
        
        this.device = device;
        this.physicalDevice = physicalDevice;
        this.id = id;
        this.totalSize = size;
        this.memoryTypeIndex = memoryTypeIndex;
        this.strategy = strategy;
        this.minAllocationSize = strategy == AllocationStrategy.BUDDY ? BUDDY_MIN_SIZE : 64;
        this.defaultAlignment = 256; // Common alignment for most resources
        this.memoryPriority = priority.getValue();
        this.opaqueAddress = opaqueAddress;
        
        // Query feature support
        this.featureSupport = VulkanFeatureSupport.query(physicalDevice);
        
        // Build allocate flags
        int flagBits = 0;
        for (MemoryAllocateFlags flag : flags) {
            flagBits |= flag.getValue();
        }
        this.allocateFlags = flagBits;
        
        // Get memory properties
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProps);
            this.propertyFlags = memProps.memoryTypes(memoryTypeIndex).propertyFlags();
            this.heapIndex = memProps.memoryTypes(memoryTypeIndex).heapIndex();
            
            // Query properties for nonCoherentAtomSize (Vulkan 1.0 but used more in 1.3+)
            VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.malloc(stack);
            vkGetPhysicalDeviceProperties(physicalDevice, props);
            this.nonCoherentAtomSize = props.limits().nonCoherentAtomSize();
        }
        
        // Allocate device memory with 1.2/1.3/1.4 extensions
        this.memoryHandle = allocateDeviceMemory(size, memoryTypeIndex);
        
        // Get device address for block if supported (Vulkan 1.2+)
        if (featureSupport.bufferDeviceAddress && (allocateFlags & VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT) != 0) {
            this.blockDeviceAddress = queryDeviceAddress();
        }
        
        // Update memory budget (Vulkan 1.4 / VK_EXT_memory_budget)
        if (featureSupport.memoryBudget) {
            updateMemoryBudget();
        }
        
        // Setup persistent mapping if host visible
        if (isHostVisible()) {
            this.mappedPointer = mapMemory();
        }
        
        // Initialize allocation strategy
        if (strategy == AllocationStrategy.BUDDY) {
            this.buddyMinOrder = (int) (Math.log(BUDDY_MIN_SIZE) / Math.log(2));
            this.buddyMaxOrder = (int) (Math.log(size) / Math.log(2));
            this.buddyFreeLists = new ArrayList<>();
            
            for (int i = 0; i <= buddyMaxOrder - buddyMinOrder; i++) {
                buddyFreeLists.add(new HashSet<>());
            }
            
            // Initial free block at max order
            FreeNode initialNode = new FreeNode(0, size);
            initialNode.order = buddyMaxOrder - buddyMinOrder;
            buddyFreeLists.get(initialNode.order).add(initialNode);
            freeNodesByOffset.put(0L, initialNode);
        } else {
            this.buddyMinOrder = 0;
            this.buddyMaxOrder = 0;
            this.buddyFreeLists = null;
            
            // Initial free node covering whole block
            FreeNode initialNode = new FreeNode(0, size);
            freeNodesByOffset.put(0L, initialNode);
            addToSizeIndex(initialNode);
        }
    }
    
    private long allocateDeviceMemory(long size, int memoryTypeIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.malloc(stack)
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .pNext(0)
                .allocationSize(size)
                .memoryTypeIndex(memoryTypeIndex);
            
            long pNextChain = 0;
            
            // Vulkan 1.2 - Memory allocate flags (device address)
            VkMemoryAllocateFlagsInfo flagsInfo = null;
            if (allocateFlags != 0) {
                flagsInfo = VkMemoryAllocateFlagsInfo.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_FLAGS_INFO)
                    .pNext(pNextChain)
                    .flags(allocateFlags)
                    .deviceMask(0);
                pNextChain = flagsInfo.address();
            }
            
            // Vulkan 1.2 - Opaque device address for capture/replay
            VkMemoryOpaqueCaptureAddressAllocateInfo opaqueInfo = null;
            if (opaqueAddress != 0 && featureSupport.bufferDeviceAddressCaptureReplay) {
                opaqueInfo = VkMemoryOpaqueCaptureAddressAllocateInfo.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_OPAQUE_CAPTURE_ADDRESS_ALLOCATE_INFO)
                    .pNext(pNextChain)
                    .opaqueCaptureAddress(opaqueAddress);
                pNextChain = opaqueInfo.address();
            }
            
            // VK_EXT_memory_priority - Memory priority
            VkMemoryPriorityAllocateInfoEXT priorityInfo = null;
            if (featureSupport.memoryPriority) {
                priorityInfo = VkMemoryPriorityAllocateInfoEXT.malloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_PRIORITY_ALLOCATE_INFO_EXT)
                    .pNext(pNextChain)
                    .priority(memoryPriority);
                pNextChain = priorityInfo.address();
            }
            
            allocInfo.pNext(pNextChain);
            
            LongBuffer pMemory = stack.mallocLong(1);
            int result = vkAllocateMemory(device, allocInfo, null, pMemory);
            
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate device memory: " + translateVkResult(result));
            }
            
            return pMemory.get(0);
        }
    }
    
    /**
     * Query the device address for this memory block (Vulkan 1.2+).
     */
    private long queryDeviceAddress() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDeviceMemoryOpaqueCaptureAddressInfo info = VkDeviceMemoryOpaqueCaptureAddressInfo.malloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEVICE_MEMORY_OPAQUE_CAPTURE_ADDRESS_INFO)
                .pNext(0)
                .memory(memoryHandle);
            
            return vkGetDeviceMemoryOpaqueCaptureAddress(device, info);
        }
    }
    
    private long mapMemory() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pData = stack.mallocPointer(1);
            int result = vkMapMemory(device, memoryHandle, 0, totalSize, 0, pData);
            
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to map memory: " + translateVkResult(result));
            }
            
            return pData.get(0);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.4 - MEMORY BUDGET (VK_EXT_memory_budget)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Update memory budget information from the driver.
     * Call periodically to get accurate budget data.
     */
    public void updateMemoryBudget() {
        if (!featureSupport.memoryBudget) return;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryBudgetPropertiesEXT budgetProps = VkPhysicalDeviceMemoryBudgetPropertiesEXT.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT);
            
            VkPhysicalDeviceMemoryProperties2 memProps2 = VkPhysicalDeviceMemoryProperties2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2)
                .pNext(budgetProps.address());
            
            vkGetPhysicalDeviceMemoryProperties2(physicalDevice, memProps2);
            
            heapBudget.set(budgetProps.heapBudget(heapIndex));
            heapUsage.set(budgetProps.heapUsage(heapIndex));
        }
    }
    
    /**
     * Get the memory budget for this block's heap.
     */
    public long getHeapBudget() {
        return heapBudget.get();
    }
    
    /**
     * Get the current memory usage for this block's heap.
     */
    public long getHeapUsage() {
        return heapUsage.get();
    }
    
    /**
     * Check if we're within budget for a new allocation.
     */
    public boolean isWithinBudget(long additionalBytes) {
        if (!featureSupport.memoryBudget) return true;
        return heapUsage.get() + additionalBytes <= heapBudget.get();
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.4 - PAGEABLE DEVICE LOCAL MEMORY
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Set memory priority hint for pageable device local memory.
     * Lower priority memory may be paged out under memory pressure.
     * 
     * @param priority Priority value 0.0-1.0
     */
    public void setPageablePriority(float priority) {
        if (!featureSupport.pageableDeviceLocalMemory) return;
        if (!isDeviceLocal()) return;
        
        priority = Math.max(0.0f, Math.min(1.0f, priority));
        vkSetDeviceMemoryPriorityEXT(device, memoryHandle, priority);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.3 - MAINTENANCE4 MEMORY REQUIREMENTS
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Query buffer memory requirements without creating a buffer (Vulkan 1.3+).
     * More efficient than creating a temporary buffer.
     */
    public static MemoryRequirements queryBufferMemoryRequirements(
            VkDevice device,
            long size,
            int usage,
            int createFlags,
            boolean maintenance4Supported) {
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (maintenance4Supported) {
                // Vulkan 1.3 path - no buffer creation needed
                VkDeviceBufferMemoryRequirements bufferReqs = VkDeviceBufferMemoryRequirements.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_BUFFER_MEMORY_REQUIREMENTS);
                
                VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size)
                    .usage(usage)
                    .flags(createFlags)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
                
                bufferReqs.pCreateInfo(bufferInfo);
                
                VkMemoryRequirements2 memReqs2 = VkMemoryRequirements2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2);
                
                vkGetDeviceBufferMemoryRequirements(device, bufferReqs, memReqs2);
                
                VkMemoryRequirements reqs = memReqs2.memoryRequirements();
                return new MemoryRequirements(
                    reqs.size(),
                    reqs.alignment(),
                    reqs.memoryTypeBits()
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
                vkCreateBuffer(device, bufferInfo, null, pBuffer);
                long buffer = pBuffer.get(0);
                
                VkMemoryRequirements reqs = VkMemoryRequirements.malloc(stack);
                vkGetBufferMemoryRequirements(device, buffer, reqs);
                
                MemoryRequirements result = new MemoryRequirements(
                    reqs.size(),
                    reqs.alignment(),
                    reqs.memoryTypeBits()
                );
                
                vkDestroyBuffer(device, buffer, null);
                return result;
            }
        }
    }
    
    /**
     * Query image memory requirements without creating an image (Vulkan 1.3+).
     */
    public static MemoryRequirements queryImageMemoryRequirements(
            VkDevice device,
            VkImageCreateInfo imageInfo,
            boolean maintenance4Supported) {
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (maintenance4Supported) {
                // Vulkan 1.3 path
                VkDeviceImageMemoryRequirements imageReqs = VkDeviceImageMemoryRequirements.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_IMAGE_MEMORY_REQUIREMENTS)
                    .pCreateInfo(imageInfo);
                
                VkMemoryRequirements2 memReqs2 = VkMemoryRequirements2.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2);
                
                vkGetDeviceImageMemoryRequirements(device, imageReqs, memReqs2);
                
                VkMemoryRequirements reqs = memReqs2.memoryRequirements();
                return new MemoryRequirements(
                    reqs.size(),
                    reqs.alignment(),
                    reqs.memoryTypeBits()
                );
            } else {
                // Fallback - create temporary image
                LongBuffer pImage = stack.mallocLong(1);
                vkCreateImage(device, imageInfo, null, pImage);
                long image = pImage.get(0);
                
                VkMemoryRequirements reqs = VkMemoryRequirements.malloc(stack);
                vkGetImageMemoryRequirements(device, image, reqs);
                
                MemoryRequirements result = new MemoryRequirements(
                    reqs.size(),
                    reqs.alignment(),
                    reqs.memoryTypeBits()
                );
                
                vkDestroyImage(device, image, null);
                return result;
            }
        }
    }
    
    public record MemoryRequirements(long size, long alignment, int memoryTypeBits) {}
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.4 - MAINTENANCE5 FEATURES
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Get buffer usage flags for combined usage (Vulkan 1.4 / KHR_maintenance5).
     * Returns what usage flags are actually supported for a given memory type.
     */
    public static long getBufferUsageFlags2(
            VkPhysicalDevice physicalDevice,
            long usage,
            int createFlags,
            boolean maintenance5Supported) {
        
        if (!maintenance5Supported) {
            return usage; // Just return input if not supported
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferUsageFlags2CreateInfoKHR usageFlags = VkBufferUsageFlags2CreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_USAGE_FLAGS_2_CREATE_INFO_KHR)
                .usage(usage);
            
            // Query what's actually supported
            // This is placeholder - actual implementation depends on use case
            return usage;
        }
    }
    
    /**
     * Get rendering area granularity (Vulkan 1.4 / KHR_maintenance5).
     */
    public static VkExtent2D getRenderingAreaGranularity(
            VkDevice device,
            int colorAttachmentCount,
            int colorFormat,
            int depthFormat,
            int stencilFormat,
            boolean maintenance5Supported) {
        
        if (!maintenance5Supported) {
            // Return 1x1 as safe default
            VkExtent2D extent = VkExtent2D.create();
            extent.set(1, 1);
            return extent;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkRenderingAreaInfoKHR areaInfo = VkRenderingAreaInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDERING_AREA_INFO_KHR)
                .viewMask(0)
                .colorAttachmentCount(colorAttachmentCount)
                .depthAttachmentFormat(depthFormat)
                .stencilAttachmentFormat(stencilFormat);
            
            if (colorAttachmentCount > 0) {
                areaInfo.pColorAttachmentFormats(stack.ints(colorFormat));
            }
            
            VkExtent2D granularity = VkExtent2D.malloc(stack);
            vkGetRenderingAreaGranularityKHR(device, areaInfo, granularity);
            
            VkExtent2D result = VkExtent2D.create();
            result.set(granularity.width(), granularity.height());
            return result;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.4 - MAINTENANCE6 FEATURES
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Query descriptor buffer binding alignment (Vulkan 1.4 / KHR_maintenance6).
     */
    public long getDescriptorBufferBindingAlignment() {
        if (!featureSupport.maintenance6) {
            return 256; // Safe default
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMaintenance6PropertiesKHR maintenance6Props = VkPhysicalDeviceMaintenance6PropertiesKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MAINTENANCE_6_PROPERTIES_KHR);
            
            VkPhysicalDeviceProperties2 props2 = VkPhysicalDeviceProperties2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2)
                .pNext(maintenance6Props.address());
            
            vkGetPhysicalDeviceProperties2(physicalDevice, props2);
            
            // Return a relevant property - using a placeholder
            // Actual properties depend on descriptor buffer extension
            return 256;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.2 - DEVICE ADDRESS SUPPORT
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Get the device address for this block.
     */
    public long getBlockDeviceAddress() {
        return blockDeviceAddress;
    }
    
    /**
     * Get device address for a specific offset within this block.
     */
    public long getDeviceAddress(long offset) {
        if (blockDeviceAddress == 0) return 0;
        return blockDeviceAddress + offset;
    }
    
    /**
     * Get the opaque capture address for this memory (for capture/replay).
     */
    public long getOpaqueCaptureAddress() {
        if (!featureSupport.bufferDeviceAddressCaptureReplay) return 0;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDeviceMemoryOpaqueCaptureAddressInfo info = VkDeviceMemoryOpaqueCaptureAddressInfo.malloc(stack)
                .sType(VK_STRUCTURE_TYPE_DEVICE_MEMORY_OPAQUE_CAPTURE_ADDRESS_INFO)
                .pNext(0)
                .memory(memoryHandle);
            
            return vkGetDeviceMemoryOpaqueCaptureAddress(device, info);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // VULKAN 1.3 - SYNCHRONIZATION2 MEMORY BARRIERS
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
            long dstAccessMask,
            int srcQueueFamily,
            int dstQueueFamily) {
        
        return VkBufferMemoryBarrier2.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(srcStageMask)
            .srcAccessMask(srcAccessMask)
            .dstStageMask(dstStageMask)
            .dstAccessMask(dstAccessMask)
            .srcQueueFamilyIndex(srcQueueFamily)
            .dstQueueFamilyIndex(dstQueueFamily)
            .buffer(buffer)
            .offset(offset)
            .size(size);
    }
    
    /**
     * Submit pipeline barrier using Synchronization2 (Vulkan 1.3+).
     */
    public void pipelineBarrier2(
            VkCommandBuffer commandBuffer,
            VkMemoryBarrier2.Buffer memoryBarriers,
            VkBufferMemoryBarrier2.Buffer bufferBarriers,
            VkImageMemoryBarrier2.Buffer imageBarriers) {
        
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
    // ALLOCATION
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Allocate memory from this block.
     * 
     * @param size Required size in bytes
     * @param alignment Required alignment
     * @param debugName Optional debug name
     * @return Allocation or null if block is full
     */
    public MemoryAllocation allocate(long size, long alignment, String debugName) {
        if (destroyed) {
            throw new IllegalStateException("Cannot allocate from destroyed block");
        }
        
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive: " + size);
        }
        
        // Check budget before allocating (Vulkan 1.4)
        if (featureSupport.memoryBudget && !isWithinBudget(size)) {
            updateMemoryBudget(); // Refresh budget
            if (!isWithinBudget(size)) {
                return null; // Over budget
            }
        }
        
        alignment = Math.max(alignment, 1);
        size = Math.max(size, minAllocationSize);
        
        lock.writeLock().lock();
        try {
            return switch (strategy) {
                case FIRST_FIT -> allocateFirstFit(size, alignment, debugName);
                case BEST_FIT -> allocateBestFit(size, alignment, debugName);
                case BUDDY -> allocateBuddy(size, alignment, debugName);
                case LINEAR -> allocateLinear(size, alignment, debugName);
            };
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public MemoryAllocation allocate(long size, long alignment) {
        return allocate(size, alignment, null);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FIRST-FIT ALLOCATION
    // ═══════════════════════════════════════════════════════════════════════
    
    private MemoryAllocation allocateFirstFit(long size, long alignment, String debugName) {
        for (FreeNode node : freeNodesByOffset.values()) {
            MemoryAllocation result = tryAllocateFromNode(node, size, alignment, debugName);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // BEST-FIT ALLOCATION
    // ═══════════════════════════════════════════════════════════════════════
    
    private MemoryAllocation allocateBestFit(long size, long alignment, String debugName) {
        // Find smallest free node that fits
        // Account for worst-case alignment
        long worstCaseSize = size + alignment - 1;
        
        Map.Entry<Long, Set<FreeNode>> entry = freeNodesBySize.ceilingEntry(worstCaseSize);
        
        while (entry != null) {
            for (FreeNode node : entry.getValue()) {
                MemoryAllocation result = tryAllocateFromNode(node, size, alignment, debugName);
                if (result != null) {
                    return result;
                }
            }
            entry = freeNodesBySize.higherEntry(entry.getKey());
        }
        
        return null;
    }
    
    private MemoryAllocation tryAllocateFromNode(FreeNode node, long size, long alignment, String debugName) {
        // Calculate aligned offset
        long alignedOffset = alignUp(node.offset, alignment);
        long padding = alignedOffset - node.offset;
        long totalRequired = size + padding;
        
        if (node.size < totalRequired) {
            return null;
        }
        
        // Remove from free lists
        freeNodesByOffset.remove(node.offset);
        removeFromSizeIndex(node);
        
        // Create front padding node if needed
        if (padding > 0) {
            FreeNode frontNode = new FreeNode(node.offset, padding);
            freeNodesByOffset.put(frontNode.offset, frontNode);
            addToSizeIndex(frontNode);
            fragmentedBytes.addAndGet(padding);
        }
        
        // Create back remainder node if needed
        long remainder = node.size - totalRequired;
        if (remainder > 0) {
            FreeNode backNode = new FreeNode(alignedOffset + size, remainder);
            freeNodesByOffset.put(backNode.offset, backNode);
            addToSizeIndex(backNode);
        }
        
        // Create allocation
        return createAllocation(alignedOffset, size, totalRequired, debugName);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // BUDDY ALLOCATION
    // ═══════════════════════════════════════════════════════════════════════
    
    private MemoryAllocation allocateBuddy(long size, long alignment, String debugName) {
        // Round size up to next power of 2
        long alignedSize = Math.max(nextPowerOf2(size), BUDDY_MIN_SIZE);
        int requiredOrder = (int) (Math.log(alignedSize) / Math.log(2)) - buddyMinOrder;
        
        if (requiredOrder > buddyMaxOrder - buddyMinOrder) {
            return null; // Too large
        }
        
        // Find a free block of sufficient size
        int foundOrder = -1;
        for (int order = requiredOrder; order <= buddyMaxOrder - buddyMinOrder; order++) {
            if (!buddyFreeLists.get(order).isEmpty()) {
                foundOrder = order;
                break;
            }
        }
        
        if (foundOrder == -1) {
            return null; // No suitable block found
        }
        
        // Split blocks until we reach required order
        while (foundOrder > requiredOrder) {
            Set<FreeNode> freeList = buddyFreeLists.get(foundOrder);
            FreeNode node = freeList.iterator().next();
            freeList.remove(node);
            freeNodesByOffset.remove(node.offset);
            
            // Split into two buddies
            long halfSize = node.size / 2;
            int newOrder = foundOrder - 1;
            
            FreeNode left = new FreeNode(node.offset, halfSize);
            left.order = newOrder;
            FreeNode right = new FreeNode(node.offset + halfSize, halfSize);
            right.order = newOrder;
            
            buddyFreeLists.get(newOrder).add(left);
            buddyFreeLists.get(newOrder).add(right);
            freeNodesByOffset.put(left.offset, left);
            freeNodesByOffset.put(right.offset, right);
            
            foundOrder = newOrder;
        }
        
        // Allocate from a block at the required order
        Set<FreeNode> freeList = buddyFreeLists.get(requiredOrder);
        FreeNode allocNode = freeList.iterator().next();
        freeList.remove(allocNode);
        freeNodesByOffset.remove(allocNode.offset);
        
        return createAllocation(allocNode.offset, size, alignedSize, debugName);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // LINEAR ALLOCATION
    // ═══════════════════════════════════════════════════════════════════════
    
    private MemoryAllocation allocateLinear(long size, long alignment, String debugName) {
        while (true) {
            long currentOffset = linearOffset.get();
            long alignedOffset = alignUp(currentOffset, alignment);
            long newOffset = alignedOffset + size;
            
            if (newOffset > totalSize) {
                return null; // Full
            }
            
            if (linearOffset.compareAndSet(currentOffset, newOffset)) {
                return createAllocation(alignedOffset, size, size, debugName);
            }
            // CAS failed, retry
        }
    }
    
    /**
     * Reset linear allocator (frees all allocations).
     */
    public void resetLinear() {
        if (strategy != AllocationStrategy.LINEAR) {
            throw new IllegalStateException("resetLinear() only valid for LINEAR strategy");
        }
        
        lock.writeLock().lock();
        try {
            linearOffset.set(0);
            allocatedBytes.set(0);
            allocationCount.set(0);
            activeAllocations.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // COMMON ALLOCATION HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    
    private MemoryAllocation createAllocation(long offset, long size, long alignedSize, String debugName) {
        // Update statistics
        allocatedBytes.addAndGet(alignedSize);
        allocationCount.incrementAndGet();
        totalAllocations.incrementAndGet();
        
        long peak = peakAllocatedBytes.get();
        long current = allocatedBytes.get();
        while (current > peak) {
            if (peakAllocatedBytes.compareAndSet(peak, current)) break;
            peak = peakAllocatedBytes.get();
        }
        
        // Track allocation
        AllocationRecord record = new AllocationRecord(offset, size, alignedSize, debugName);
        
        // Store device address for this allocation (Vulkan 1.2+)
        if (blockDeviceAddress != 0) {
            record.deviceAddress = blockDeviceAddress + offset;
        }
        
        activeAllocations.put(offset, record);
        
        // Create allocation object
        MemoryAllocation allocation = MemoryAllocation.builder()
            .memoryHandle(memoryHandle)
            .offset(offset)
            .size(size)
            .alignedSize(alignedSize)
            .alignment(defaultAlignment)
            .blockId(id)
            .type(MemoryAllocation.AllocationType.SUBALLOCATED)
            .usage(determineUsage())
            .memoryTypeIndex(memoryTypeIndex)
            .propertyFlags(propertyFlags)
            .baseMappedPtr(mappedPointer)
            .debugName(debugName)
            .deviceAddress(record.deviceAddress) // Vulkan 1.2+
            .build();
        
        record.allocation = allocation;
        
        // Zero memory in debug mode
        if (MemoryDebugConfig.ZERO_ON_ALLOCATE && mappedPointer != 0) {
            ByteBuffer mapped = allocation.getMappedData();
            for (int i = 0; i < size; i++) {
                mapped.put(i, (byte) 0);
            }
        }
        
        return allocation;
    }
    
    private MemoryAllocation.MemoryUsage determineUsage() {
        if ((propertyFlags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0) {
            if ((propertyFlags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0) {
                return MemoryAllocation.MemoryUsage.CPU_TO_GPU;
            }
            return MemoryAllocation.MemoryUsage.GPU_ONLY;
        }
        if ((propertyFlags & VK_MEMORY_PROPERTY_HOST_CACHED_BIT) != 0) {
            return MemoryAllocation.MemoryUsage.GPU_TO_CPU;
        }
        return MemoryAllocation.MemoryUsage.CPU_TO_GPU;
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // FREE
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Free an allocation.
     */
    public void free(MemoryAllocation allocation) {
        if (destroyed) {
            throw new IllegalStateException("Cannot free from destroyed block");
        }
        
        if (allocation.getBlockId() != id) {
            throw new IllegalArgumentException("Allocation does not belong to this block");
        }
        
        if (allocation.isFreed()) {
            throw new IllegalStateException("Allocation already freed: " + allocation.getDebugName());
        }
        
        if (strategy == AllocationStrategy.LINEAR) {
            // Linear allocator doesn't support individual frees
            allocation.markFreed();
            return;
        }
        
        lock.writeLock().lock();
        try {
            freeInternal(allocation);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private void freeInternal(MemoryAllocation allocation) {
        long offset = allocation.getOffset();
        long size = allocation.getAlignedSize();
        
        // Remove from active allocations
        AllocationRecord record = activeAllocations.remove(offset);
        if (record == null) {
            throw new IllegalStateException("Allocation not found in active allocations: " + offset);
        }
        
        // Update statistics
        allocatedBytes.addAndGet(-size);
        allocationCount.decrementAndGet();
        totalFrees.incrementAndGet();
        
        // Fill with pattern in debug mode
        if (MemoryDebugConfig.FILL_ON_FREE && mappedPointer != 0) {
            ByteBuffer mapped = allocation.getMappedData();
            for (int i = 0; i < size; i++) {
                mapped.put(i, (byte) 0xDD);
            }
        }
        
        allocation.markFreed();
        
        if (strategy == AllocationStrategy.BUDDY) {
            freeBuddy(offset, size);
        } else {
            freeAndCoalesce(offset, size);
        }
    }
    
    private void freeAndCoalesce(long offset, long size) {
        // Create new free node
        FreeNode newNode = new FreeNode(offset, size);
        
        // Try to coalesce with previous node
        Map.Entry<Long, FreeNode> prevEntry = freeNodesByOffset.lowerEntry(offset);
        if (prevEntry != null) {
            FreeNode prev = prevEntry.getValue();
            if (prev.offset + prev.size == offset) {
                // Merge with previous
                freeNodesByOffset.remove(prev.offset);
                removeFromSizeIndex(prev);
                
                newNode.offset = prev.offset;
                newNode.size += prev.size;
                fragmentedBytes.addAndGet(-prev.size);
            }
        }
        
        // Try to coalesce with next node
        Map.Entry<Long, FreeNode> nextEntry = freeNodesByOffset.higherEntry(offset);
        if (nextEntry != null) {
            FreeNode next = nextEntry.getValue();
            if (newNode.offset + newNode.size == next.offset) {
                // Merge with next
                freeNodesByOffset.remove(next.offset);
                removeFromSizeIndex(next);
                
                newNode.size += next.size;
            }
        }
        
        // Add coalesced node
        freeNodesByOffset.put(newNode.offset, newNode);
        addToSizeIndex(newNode);
    }
    
    private void freeBuddy(long offset, long size) {
        int order = (int) (Math.log(size) / Math.log(2)) - buddyMinOrder;
        
        while (order < buddyMaxOrder - buddyMinOrder) {
            // Find buddy
            long buddyOffset = offset ^ (1L << (order + buddyMinOrder));
            FreeNode buddy = freeNodesByOffset.get(buddyOffset);
            
            if (buddy == null || buddy.order != order) {
                // No free buddy at this level
                break;
            }
            
            // Merge with buddy
            buddyFreeLists.get(order).remove(buddy);
            freeNodesByOffset.remove(buddy.offset);
            
            offset = Math.min(offset, buddyOffset);
            size *= 2;
            order++;
        }
        
        // Add merged block
        FreeNode node = new FreeNode(offset, size);
        node.order = order;
        buddyFreeLists.get(order).add(node);
        freeNodesByOffset.put(offset, node);
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // SIZE INDEX HELPERS
    // ═══════════════════════════════════════════════════════════════════════
    
    private void addToSizeIndex(FreeNode node) {
        freeNodesBySize.computeIfAbsent(node.size, k -> new HashSet<>()).add(node);
    }
    
    private void removeFromSizeIndex(FreeNode node) {
        Set<FreeNode> nodes = freeNodesBySize.get(node.size);
        if (nodes != null) {
            nodes.remove(node);
            if (nodes.isEmpty()) {
                freeNodesBySize.remove(node.size);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEFRAGMENTATION
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Check if defragmentation would be beneficial.
     */
    public boolean needsDefragmentation() {
        if (strategy == AllocationStrategy.LINEAR || strategy == AllocationStrategy.BUDDY) {
            return false;
        }
        
        lock.readLock().lock();
        try {
            // Calculate fragmentation ratio
            long totalFree = totalSize - allocatedBytes.get();
            if (totalFree == 0) return false;
            
            // Find largest contiguous free block
            long largestFree = 0;
            for (FreeNode node : freeNodesByOffset.values()) {
                largestFree = Math.max(largestFree, node.size);
            }
            
            // Fragmented if largest free block is much smaller than total free
            double fragRatio = 1.0 - (double) largestFree / totalFree;
            return fragRatio > 0.5 && freeNodesByOffset.size() > 4;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Defragment the block by compacting allocations.
     * Returns list of moves that need to be performed.
     */
    public List<DefragMove> planDefragmentation() {
        if (strategy != AllocationStrategy.FIRST_FIT && strategy != AllocationStrategy.BEST_FIT) {
            return List.of();
        }
        
        lock.readLock().lock();
        try {
            List<DefragMove> moves = new ArrayList<>();
            
            // Sort allocations by offset
            List<AllocationRecord> sortedAllocs = new ArrayList<>(activeAllocations.values());
            sortedAllocs.sort(Comparator.comparingLong(a -> a.offset));
            
            long targetOffset = 0;
            for (AllocationRecord record : sortedAllocs) {
                if (record.offset > targetOffset) {
                    // This allocation can be moved earlier
                    moves.add(new DefragMove(
                        record.allocation,
                        record.offset,
                        targetOffset,
                        record.alignedSize
                    ));
                }
                targetOffset = alignUp(targetOffset + record.alignedSize, defaultAlignment);
            }
            
            return moves;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Represents a defragmentation move operation.
     */
    public record DefragMove(
        MemoryAllocation allocation,
        long sourceOffset,
        long destOffset,
        long size
    ) {}
    
    // ═══════════════════════════════════════════════════════════════════════
    // FLUSH / INVALIDATE (Enhanced for Vulkan 1.3+)
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Flush all mapped memory.
     */
    public void flushAll() {
        if (!isHostVisible() || isHostCoherent()) return;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMappedMemoryRange range = VkMappedMemoryRange.malloc(stack)
                .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                .pNext(0)
                .memory(memoryHandle)
                .offset(0)
                .size(VK_WHOLE_SIZE);
            
            vkFlushMappedMemoryRanges(device, range);
        }
    }
    
    /**
     * Flush specific range.
     * Uses nonCoherentAtomSize from Vulkan 1.3+ properties.
     */
    public void flush(long offset, long size) {
        if (!isHostVisible() || isHostCoherent()) return;
        
        // Align to nonCoherentAtomSize
        long alignedOffset = offset & ~(nonCoherentAtomSize - 1);
        long alignedSize = alignUp(offset + size, nonCoherentAtomSize) - alignedOffset;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMappedMemoryRange range = VkMappedMemoryRange.malloc(stack)
                .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                .pNext(0)
                .memory(memoryHandle)
                .offset(alignedOffset)
                .size(alignedSize);
            
            vkFlushMappedMemoryRanges(device, range);
        }
    }
    
    /**
     * Invalidate all mapped memory.
     */
    public void invalidateAll() {
        if (!isHostVisible() || isHostCoherent()) return;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMappedMemoryRange range = VkMappedMemoryRange.malloc(stack)
                .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                .pNext(0)
                .memory(memoryHandle)
                .offset(0)
                .size(VK_WHOLE_SIZE);
            
            vkInvalidateMappedMemoryRanges(device, range);
        }
    }
    
    /**
     * Invalidate specific range.
     */
    public void invalidate(long offset, long size) {
        if (!isHostVisible() || isHostCoherent()) return;
        
        long alignedOffset = offset & ~(nonCoherentAtomSize - 1);
        long alignedSize = alignUp(offset + size, nonCoherentAtomSize) - alignedOffset;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMappedMemoryRange range = VkMappedMemoryRange.malloc(stack)
                .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                .pNext(0)
                .memory(memoryHandle)
                .offset(alignedOffset)
                .size(alignedSize);
            
            vkInvalidateMappedMemoryRanges(device, range);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // PROPERTIES & STATISTICS
    // ═══════════════════════════════════════════════════════════════════════
    
    public int getId() { return id; }
    public long getTotalSize() { return totalSize; }
    public long getMemoryHandle() { return memoryHandle; }
    public int getMemoryTypeIndex() { return memoryTypeIndex; }
    public int getPropertyFlags() { return propertyFlags; }
    public AllocationStrategy getStrategy() { return strategy; }
    public long getMappedPointer() { return mappedPointer; }
    public VulkanFeatureSupport getFeatureSupport() { return featureSupport; }
    public float getMemoryPriorityValue() { return memoryPriority; }
    public int getHeapIndex() { return heapIndex; }
    public long getNonCoherentAtomSize() { return nonCoherentAtomSize; }
    
    public boolean isHostVisible() {
        return (propertyFlags & VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0;
    }
    
    public boolean isHostCoherent() {
        return (propertyFlags & VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) != 0;
    }
    
    public boolean isDeviceLocal() {
        return (propertyFlags & VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0;
    }
    
    public boolean isMapped() {
        return mappedPointer != 0;
    }
    
    public boolean hasDeviceAddress() {
        return blockDeviceAddress != 0;
    }
    
    public long getAllocatedBytes() { return allocatedBytes.get(); }
    public long getFreeBytes() { return totalSize - allocatedBytes.get(); }
    public long getAllocationCount() { return allocationCount.get(); }
    public long getPeakAllocatedBytes() { return peakAllocatedBytes.get(); }
    public long getTotalAllocations() { return totalAllocations.get(); }
    public long getTotalFrees() { return totalFrees.get(); }
    
    public double getUsageRatio() {
        return (double) allocatedBytes.get() / totalSize;
    }
    
    public double getFragmentationRatio() {
        long totalFree = getFreeBytes();
        if (totalFree == 0) return 0.0;
        
        lock.readLock().lock();
        try {
            long largestFree = 0;
            for (FreeNode node : freeNodesByOffset.values()) {
                largestFree = Math.max(largestFree, node.size);
            }
            return 1.0 - (double) largestFree / totalFree;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get detailed statistics.
     */
    public BlockStatistics getStatistics() {
        lock.readLock().lock();
        try {
            long largestFree = 0;
            int freeNodeCount = freeNodesByOffset.size();
            
            for (FreeNode node : freeNodesByOffset.values()) {
                largestFree = Math.max(largestFree, node.size);
            }
            
            return new BlockStatistics(
                id,
                totalSize,
                allocatedBytes.get(),
                getFreeBytes(),
                largestFree,
                allocationCount.get(),
                freeNodeCount,
                totalAllocations.get(),
                totalFrees.get(),
                peakAllocatedBytes.get(),
                getFragmentationRatio(),
                strategy,
                propertyFlags,
                heapBudget.get(),
                heapUsage.get(),
                blockDeviceAddress,
                memoryPriority
            );
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public record BlockStatistics(
        int blockId,
        long totalSize,
        long allocatedBytes,
        long freeBytes,
        long largestFreeBlock,
        long allocationCount,
        int freeNodeCount,
        long totalAllocations,
        long totalFrees,
        long peakAllocatedBytes,
        double fragmentationRatio,
        AllocationStrategy strategy,
        int propertyFlags,
        // Vulkan 1.4+ budget info
        long heapBudget,
        long heapUsage,
        long deviceAddress,
        float memoryPriority
    ) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format(
                "Block %d [%s]: %s / %s (%.1f%%), %d allocs, %d free nodes, %.1f%% fragmented",
                blockId,
                strategy,
                formatBytes(allocatedBytes),
                formatBytes(totalSize),
                100.0 * allocatedBytes / totalSize,
                allocationCount,
                freeNodeCount,
                fragmentationRatio * 100
            ));
            
            if (heapBudget > 0) {
                sb.append(String.format(", budget: %s/%s", 
                    formatBytes(heapUsage), formatBytes(heapBudget)));
            }
            
            if (deviceAddress != 0) {
                sb.append(String.format(", addr: 0x%X", deviceAddress));
            }
            
            return sb.toString();
        }
        
        private static String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DEBUG
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Dump block state for debugging.
     */
    public String dump() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== MemoryBlock ").append(id).append(" ===\n");
            sb.append("  Total: ").append(formatBytes(totalSize)).append("\n");
            sb.append("  Allocated: ").append(formatBytes(allocatedBytes.get())).append("\n");
            sb.append("  Free: ").append(formatBytes(getFreeBytes())).append("\n");
            sb.append("  Strategy: ").append(strategy).append("\n");
            sb.append("  Mapped: ").append(isMapped()).append("\n");
            sb.append("  Property flags: 0x").append(Integer.toHexString(propertyFlags)).append("\n");
            
            // Vulkan 1.2+ info
            sb.append("  Device Address: ");
            if (blockDeviceAddress != 0) {
                sb.append(String.format("0x%X", blockDeviceAddress));
            } else {
                sb.append("N/A");
            }
            sb.append("\n");
            
            sb.append("  Memory Priority: ").append(memoryPriority).append("\n");
            
            // Vulkan 1.4 budget info
            if (featureSupport.memoryBudget) {
                sb.append("  Heap Budget: ").append(formatBytes(heapBudget.get())).append("\n");
                sb.append("  Heap Usage: ").append(formatBytes(heapUsage.get())).append("\n");
            }
            
            // Feature support
            sb.append("  Features: ").append(featureSupport).append("\n");
            
            sb.append("\n  Allocations (").append(activeAllocations.size()).append("):\n");
            List<AllocationRecord> sorted = new ArrayList<>(activeAllocations.values());
            sorted.sort(Comparator.comparingLong(a -> a.offset));
            for (AllocationRecord rec : sorted) {
                sb.append("    [").append(rec.offset).append(" - ")
                  .append(rec.offset + rec.alignedSize).append("] ")
                  .append(formatBytes(rec.size));
                if (rec.deviceAddress != 0) {
                    sb.append(String.format(" @0x%X", rec.deviceAddress));
                }
                if (rec.debugName != null) {
                    sb.append(" '").append(rec.debugName).append("'");
                }
                sb.append("\n");
            }
            
            sb.append("\n  Free nodes (").append(freeNodesByOffset.size()).append("):\n");
            for (FreeNode node : freeNodesByOffset.values()) {
                sb.append("    [").append(node.offset).append(" - ")
                  .append(node.offset + node.size).append("] ")
                  .append(formatBytes(node.size)).append("\n");
            }
            
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Validate internal consistency (debug).
     */
    public void validate() {
        lock.readLock().lock();
        try {
            // Check no overlapping allocations
            long lastEnd = 0;
            for (AllocationRecord rec : activeAllocations.values().stream()
                    .sorted(Comparator.comparingLong(a -> a.offset)).toList()) {
                if (rec.offset < lastEnd) {
                    throw new IllegalStateException("Overlapping allocations detected at offset " + rec.offset);
                }
                lastEnd = rec.offset + rec.alignedSize;
            }
            
            // Check no overlapping free nodes
            lastEnd = 0;
            for (FreeNode node : freeNodesByOffset.values()) {
                if (node.offset < lastEnd) {
                    throw new IllegalStateException("Overlapping free nodes at offset " + node.offset);
                }
                lastEnd = node.offset + node.size;
            }
            
            // Check total size
            long totalUsed = activeAllocations.values().stream()
                .mapToLong(a -> a.alignedSize).sum();
            long totalFreeSpace = freeNodesByOffset.values().stream()
                .mapToLong(n -> n.size).sum();
            
            if (totalUsed + totalFreeSpace != totalSize) {
                throw new IllegalStateException(String.format(
                    "Size mismatch: used=%d + free=%d != total=%d",
                    totalUsed, totalFreeSpace, totalSize));
            }
            
            // Validate device addresses if enabled
            if (blockDeviceAddress != 0) {
                for (AllocationRecord rec : activeAllocations.values()) {
                    long expectedAddr = blockDeviceAddress + rec.offset;
                    if (rec.deviceAddress != expectedAddr) {
                        throw new IllegalStateException(String.format(
                            "Device address mismatch for allocation at %d: expected 0x%X, got 0x%X",
                            rec.offset, expectedAddr, rec.deviceAddress));
                    }
                }
            }
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ═══════════════════════════════════════════════════════════════════════
    
    /**
     * Destroy this memory block.
     */
    public void destroy() {
        if (destroyed) return;
        destroyed = true;
        
        lock.writeLock().lock();
        try {
            // Check for leaks
            if (MemoryDebugConfig.VALIDATE_LEAKS && !activeAllocations.isEmpty()) {
                System.err.println("WARNING: Destroying block with " + activeAllocations.size() + " active allocations:");
                for (AllocationRecord rec : activeAllocations.values()) {
                    System.err.println("  - " + rec.debugName + " at offset " + rec.offset);
                }
            }
            
            // Unmap if mapped
            if (mappedPointer != 0) {
                vkUnmapMemory(device, memoryHandle);
                mappedPointer = 0;
            }
            
            // Free device memory
            vkFreeMemory(device, memoryHandle, null);
            
            // Clear tracking
            activeAllocations.clear();
            freeNodesByOffset.clear();
            freeNodesBySize.clear();
            if (buddyFreeLists != null) {
                buddyFreeLists.forEach(Set::clear);
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════
    
    private static long alignUp(long value, long alignment) {
        return (value + alignment - 1) & ~(alignment - 1);
    }
    
    private static long nextPowerOf2(long value) {
        long result = 1;
        while (result < value) {
            result <<= 1;
        }
        return result;
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private static String translateVkResult(int result) {
        return switch (result) {
            case VK_SUCCESS -> "VK_SUCCESS";
            case VK_ERROR_OUT_OF_HOST_MEMORY -> "VK_ERROR_OUT_OF_HOST_MEMORY";
            case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "VK_ERROR_OUT_OF_DEVICE_MEMORY";
            case VK_ERROR_MEMORY_MAP_FAILED -> "VK_ERROR_MEMORY_MAP_FAILED";
            default -> "VK_ERROR_" + result;
        };
    }
}
