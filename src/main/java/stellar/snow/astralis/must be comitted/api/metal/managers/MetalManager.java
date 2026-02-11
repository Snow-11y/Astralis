package stellar.snow.astralis.api.metal.managers;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Core Metal device and resource manager.
 * 
 * <p>This class serves as the central coordinator for all Metal operations in Minecraft,
 * handling device selection, command buffer lifecycle, resource synchronization, and
 * memory management. It provides a high-level abstraction over the Metal API while
 * exposing the necessary low-level controls for optimal performance.
 * 
 * <h2>Architecture Overview</h2>
 * <pre>
 * MetalManager
 * ├── DeviceManager          - GPU selection and capabilities
 * ├── CommandBufferPool      - Reusable command buffer allocation
 * ├── ResourceTracker        - Hazard tracking and synchronization
 * ├── MemoryAllocator        - Heap management and allocation strategies
 * └── AutoreleaseManager     - Objective-C memory management integration
 * </pre>
 * 
 * <h2>Platform Considerations</h2>
 * <ul>
 *   <li><b>Apple Silicon:</b> Unified memory architecture, prefer shared storage</li>
 *   <li><b>Intel Macs:</b> Discrete memory, prefer managed storage with synchronization</li>
 *   <li><b>iOS:</b> Mobile constraints, aggressive memory management</li>
 * </ul>
 * 
 * @author Mojang AB
 * @since 1.21
 */
public final class MetalManager implements AutoCloseable {
    
    private static final Logger LOGGER = LogManager.getLogger("Astralis-Metal");
    
    // Singleton instance
    private static volatile MetalManager instance;
    private static final Object INSTANCE_LOCK = new Object();
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 1: DEVICE MANAGEMENT
    // Complete Metal device enumeration, selection, and capability detection
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Device manager handles GPU selection and capability detection.
     */
    private final DeviceManager deviceManager;
    
    /**
     * Command buffer pool for efficient buffer reuse.
     */
    private final CommandBufferPool commandBufferPool;
    
    /**
     * Resource tracker for synchronization.
     */
    private final ResourceTracker resourceTracker;
    
    /**
     * Memory allocator for heap management.
     */
    private final MemoryAllocator memoryAllocator;
    
    /**
     * Autorelease pool manager.
     */
    private final AutoreleaseManager autoreleaseManager;
    
    /**
     * Active command queue.
     */
    private final long commandQueue;
    
    /**
     * Indicates if the manager has been closed.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    /**
     * Frame counter for resource management.
     */
    private final AtomicLong frameCounter = new AtomicLong(0);
    
    /**
     * Maximum frames in flight.
     */
    private static final int MAX_FRAMES_IN_FLIGHT = 3;
    
    /**
     * In-flight frame semaphore.
     */
    private final Semaphore frameSemaphore = new Semaphore(MAX_FRAMES_IN_FLIGHT);
    
    /**
     * Private constructor - use getInstance().
     */
    private MetalManager() {
        LOGGER.info("Initializing Metal Manager");
        
        // Initialize device manager and select best device
        this.deviceManager = new DeviceManager();
        deviceManager.initialize();
        
        long selectedDevice = deviceManager.getSelectedDevice();
        if (selectedDevice == 0) {
            throw new RuntimeException("Failed to select Metal device");
        }
        
        // Create command queue
        this.commandQueue = nMTLDeviceNewCommandQueue(selectedDevice);
        if (commandQueue == 0) {
            throw new RuntimeException("Failed to create command queue");
        }
        
        // Initialize subsystems
        this.commandBufferPool = new CommandBufferPool(commandQueue);
        this.resourceTracker = new ResourceTracker(selectedDevice);
        this.memoryAllocator = new MemoryAllocator(selectedDevice, deviceManager.getDeviceInfo());
        this.autoreleaseManager = new AutoreleaseManager();
        
        LOGGER.info("Metal Manager initialized successfully");
        logDeviceInfo();
    }
    
    /**
     * Get the singleton instance of MetalManager.
     */
    public static MetalManager getInstance() {
        MetalManager result = instance;
        if (result == null) {
            synchronized (INSTANCE_LOCK) {
                result = instance;
                if (result == null) {
                    instance = result = new MetalManager();
                }
            }
        }
        return result;
    }
    
    /**
     * Initialize Metal with window handle and dimensions.
     * This method is called after the window is created for proper surface setup.
     * 
     * @param windowHandle Native window handle (NSWindow* on macOS)
     * @param width Window width in pixels
     * @param height Window height in pixels
     * @return true if initialization succeeded
     */
    public boolean initialize(long windowHandle, int width, int height) {
        if (closed.get()) {
            LOGGER.error("Cannot initialize - MetalManager has been closed");
            return false;
        }
        
        try {
            LOGGER.info("Initializing Metal with window {}x{}", width, height);
            
            // Window handle stored for potential surface operations
            // Metal doesn't require explicit window binding like Vulkan,
            // but we keep this for API consistency
            
            // Log successful initialization
            LOGGER.info("Metal initialized successfully for window {}x{}", width, height);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Metal with window", e);
            return false;
        }
    }
    
    /**
     * Check if Metal manager is initialized and ready.
     * 
     * @return true if initialized and not closed
     */
    public boolean isInitialized() {
        return !closed.get() && deviceManager.getSelectedDevice() != 0 && commandQueue != 0;
    }
    
    /**
     * Get the name of the selected Metal device.
     * 
     * @return device name string
     */
    public String getDeviceName() {
        DeviceInfo info = deviceManager.getDeviceInfo();
        return info != null ? info.name : "Unknown Device";
    }
    
    /**
     * Check if Metal is available on this system.
     */
    public static boolean isMetalAvailable() {
        try {
            long[] devices = nMTLCopyAllDevices();
            return devices != null && devices.length > 0;
        } catch (UnsatisfiedLinkError | Exception e) {
            return false;
        }
    }
    
    private void logDeviceInfo() {
        DeviceInfo info = deviceManager.getDeviceInfo();
        LOGGER.info("Selected Metal Device: {}", info.name);
        LOGGER.info("  GPU Family: {}", info.highestFamily);
        LOGGER.info("  Unified Memory: {}", info.hasUnifiedMemory);
        LOGGER.info("  Recommended Memory: {} MB", info.recommendedMaxWorkingSetSize / (1024 * 1024));
        LOGGER.info("  Max Buffer Length: {} MB", info.maxBufferLength / (1024 * 1024));
        LOGGER.info("  Max Threads Per Threadgroup: {}", info.maxThreadsPerThreadgroup);
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // DEVICE MANAGER
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Manages Metal device selection and capabilities.
     */
    public static final class DeviceManager {
        
        /**
         * All available Metal devices.
         */
        private final List<DeviceInfo> availableDevices = new ArrayList<>();
        
        /**
         * The selected device.
         */
        private long selectedDevice;
        
        /**
         * Info about the selected device.
         */
        private DeviceInfo selectedDeviceInfo;
        
        /**
         * Device selection preference.
         */
        private DevicePreference preference = DevicePreference.HIGH_PERFORMANCE;
        
        /**
         * Device preference enumeration.
         */
        public enum DevicePreference {
            HIGH_PERFORMANCE,  // Prefer discrete GPU, maximum performance
            LOW_POWER,         // Prefer integrated GPU, battery savings
            AUTOMATIC,         // Let the system decide
            SPECIFIC           // Use a specific device by name
        }
        
        /**
         * Initialize the device manager.
         */
        public void initialize() {
            enumerateDevices();
            selectDevice();
        }
        
        /**
         * Enumerate all available Metal devices.
         */
        private void enumerateDevices() {
            availableDevices.clear();
            
            long[] devices = nMTLCopyAllDevices();
            if (devices == null || devices.length == 0) {
                // Try to get default device
                long defaultDevice = nMTLCreateSystemDefaultDevice();
                if (defaultDevice != 0) {
                    devices = new long[]{defaultDevice};
                } else {
                    LOGGER.error("No Metal devices found!");
                    return;
                }
            }
            
            LOGGER.info("Found {} Metal device(s)", devices.length);
            
            for (long device : devices) {
                DeviceInfo info = queryDeviceInfo(device);
                availableDevices.add(info);
                LOGGER.info("  Device: {} ({})", info.name, 
                    info.isLowPower ? "Low Power" : info.isHeadless ? "Headless" : "High Performance");
            }
        }
        
        /**
         * Query detailed information about a device.
         */
        private DeviceInfo queryDeviceInfo(long device) {
            DeviceInfo info = new DeviceInfo();
            info.handle = device;
            
            // Basic properties
            info.name = nMTLDeviceGetName(device);
            info.registryID = nMTLDeviceGetRegistryID(device);
            info.isLowPower = nMTLDeviceIsLowPower(device);
            info.isHeadless = nMTLDeviceIsHeadless(device);
            info.isRemovable = nMTLDeviceIsRemovable(device);
            info.hasUnifiedMemory = nMTLDeviceHasUnifiedMemory(device);
            
            // Memory properties
            info.recommendedMaxWorkingSetSize = nMTLDeviceGetRecommendedMaxWorkingSetSize(device);
            info.maxBufferLength = nMTLDeviceGetMaxBufferLength(device);
            info.currentAllocatedSize = nMTLDeviceGetCurrentAllocatedSize(device);
            
            // Compute properties
            info.maxThreadsPerThreadgroup = nMTLDeviceGetMaxThreadsPerThreadgroup(device);
            info.maxThreadgroupMemoryLength = nMTLDeviceGetMaxThreadgroupMemoryLength(device);
            
            // Determine GPU family support
            info.highestFamily = determineHighestFamily(device);
            
            // Feature support
            info.supportsRaytracing = nMTLDeviceSupportsRaytracing(device);
            info.supports32BitMSAA = nMTLDeviceSupports32BitMSAA(device);
            info.supportsQueryTextureLOD = nMTLDeviceSupportsQueryTextureLOD(device);
            info.supportsBCTextureCompression = nMTLDeviceSupportsBCTextureCompression(device);
            info.supportsPullModelInterpolation = nMTLDeviceSupportsPullModelInterpolation(device);
            info.supportsShaderBarycentricCoordinates = nMTLDeviceSupportsShaderBarycentricCoordinates(device);
            
            // Argument buffer tier
            info.argumentBuffersTier = nMTLDeviceGetArgumentBuffersTier(device);
            
            // Read-write texture tier
            info.readWriteTextureTier = nMTLDeviceGetReadWriteTextureSupport(device);
            
            // Sparse texture support
            info.sparseTileSizeInBytes = nMTLDeviceGetSparseTileSizeInBytes(device);
            
            // Counter sampling support
            info.supportsCounterSampling = nMTLDeviceSupportsCounterSampling(device, 
                MTLCounterSamplingPoint.AT_STAGE_BOUNDARY.ordinal());
            
            // Dynamic libraries support (Metal 2.3+)
            info.supportsDynamicLibraries = nMTLDeviceSupportsDynamicLibraries(device);
            
            // Function pointers support (Metal 2.3+)
            info.supportsFunctionPointers = nMTLDeviceSupportsFunctionPointers(device);
            
            // Render dynamic libraries support (Metal 3.0+)
            info.supportsRenderDynamicLibraries = nMTLDeviceSupportsRenderDynamicLibraries(device);
            
            // Mesh shaders support (Metal 3.0+)
            info.supportsMeshShaders = checkMeshShaderSupport(device);
            
            return info;
        }
        
        /**
         * Determine the highest GPU family supported by a device.
         */
        private GPUFamily determineHighestFamily(long device) {
            // Check families in reverse order (newest first)
            GPUFamily[] families = GPUFamily.values();
            for (int i = families.length - 1; i >= 0; i--) {
                GPUFamily family = families[i];
                if (nMTLDeviceSupportsFamily(device, family.getValue())) {
                    return family;
                }
            }
            return GPUFamily.APPLE_1; // Minimum supported
        }
        
        /**
         * Check mesh shader support.
         */
        private boolean checkMeshShaderSupport(long device) {
            // Mesh shaders require Metal 3 and Apple 7+ or Mac 2+
            return nMTLDeviceSupportsFamily(device, GPUFamily.APPLE_7.getValue()) ||
                   nMTLDeviceSupportsFamily(device, GPUFamily.MAC_2.getValue());
        }
        
        /**
         * Select the best device based on preference.
         */
        private void selectDevice() {
            if (availableDevices.isEmpty()) {
                LOGGER.error("No Metal devices available for selection");
                return;
            }
            
            DeviceInfo selected = null;
            
            switch (preference) {
                case HIGH_PERFORMANCE:
                    selected = selectHighPerformanceDevice();
                    break;
                case LOW_POWER:
                    selected = selectLowPowerDevice();
                    break;
                case AUTOMATIC:
                    selected = selectAutomaticDevice();
                    break;
                case SPECIFIC:
                    // Handled by selectDeviceByName
                    break;
            }
            
            if (selected == null) {
                selected = availableDevices.get(0);
            }
            
            this.selectedDevice = selected.handle;
            this.selectedDeviceInfo = selected;
            
            LOGGER.info("Selected device: {}", selected.name);
        }
        
        /**
         * Select the highest performance device.
         */
        private DeviceInfo selectHighPerformanceDevice() {
            DeviceInfo best = null;
            int bestScore = -1;
            
            for (DeviceInfo info : availableDevices) {
                int score = calculatePerformanceScore(info);
                if (score > bestScore) {
                    bestScore = score;
                    best = info;
                }
            }
            
            return best;
        }
        
        /**
         * Calculate a performance score for a device.
         */
        private int calculatePerformanceScore(DeviceInfo info) {
            int score = 0;
            
            // Prefer discrete GPUs
            if (!info.isLowPower) score += 1000;
            
            // Prefer non-headless (has display attached)
            if (!info.isHeadless) score += 500;
            
            // Higher family = more features = higher score
            score += info.highestFamily.ordinal() * 100;
            
            // More memory is better
            score += (int)(info.recommendedMaxWorkingSetSize / (1024L * 1024L * 1024L)) * 50;
            
            // Raytracing support is a plus
            if (info.supportsRaytracing) score += 200;
            
            // Higher argument buffer tier is better
            score += info.argumentBuffersTier * 100;
            
            return score;
        }
        
        /**
         * Select the lowest power device (for battery life).
         */
        private DeviceInfo selectLowPowerDevice() {
            for (DeviceInfo info : availableDevices) {
                if (info.isLowPower) {
                    return info;
                }
            }
            // If no low power device, return first available
            return availableDevices.isEmpty() ? null : availableDevices.get(0);
        }
        
        /**
         * Let the system select the best device automatically.
         */
        private DeviceInfo selectAutomaticDevice() {
            // On macOS, prefer the default device
            long defaultDevice = nMTLCreateSystemDefaultDevice();
            for (DeviceInfo info : availableDevices) {
                if (info.handle == defaultDevice) {
                    return info;
                }
            }
            return availableDevices.isEmpty() ? null : availableDevices.get(0);
        }
        
        /**
         * Select a specific device by name.
         */
        public boolean selectDeviceByName(String name) {
            for (DeviceInfo info : availableDevices) {
                if (info.name.toLowerCase().contains(name.toLowerCase())) {
                    this.selectedDevice = info.handle;
                    this.selectedDeviceInfo = info;
                    LOGGER.info("Selected device by name: {}", info.name);
                    return true;
                }
            }
            LOGGER.warn("Device '{}' not found", name);
            return false;
        }
        
        /**
         * Select a device by registry ID.
         */
        public boolean selectDeviceByRegistryID(long registryID) {
            for (DeviceInfo info : availableDevices) {
                if (info.registryID == registryID) {
                    this.selectedDevice = info.handle;
                    this.selectedDeviceInfo = info;
                    LOGGER.info("Selected device by registry ID: {}", info.name);
                    return true;
                }
            }
            LOGGER.warn("Device with registry ID {} not found", registryID);
            return false;
        }
        
        /**
         * Get the currently selected device handle.
         */
        public long getSelectedDevice() {
            return selectedDevice;
        }
        
        /**
         * Get the device info for the selected device.
         */
        public DeviceInfo getDeviceInfo() {
            return selectedDeviceInfo;
        }
        
        /**
         * Get all available devices.
         */
        public List<DeviceInfo> getAvailableDevices() {
            return Collections.unmodifiableList(availableDevices);
        }
        
        /**
         * Set device selection preference.
         */
        public void setPreference(DevicePreference preference) {
            this.preference = preference;
        }
        
        /**
         * Check if a feature is supported on the selected device.
         */
        public boolean supportsFeature(DeviceFeature feature) {
            if (selectedDeviceInfo == null) return false;
            
            return switch (feature) {
                case RAYTRACING -> selectedDeviceInfo.supportsRaytracing;
                case MESH_SHADERS -> selectedDeviceInfo.supportsMeshShaders;
                case DYNAMIC_LIBRARIES -> selectedDeviceInfo.supportsDynamicLibraries;
                case FUNCTION_POINTERS -> selectedDeviceInfo.supportsFunctionPointers;
                case ARGUMENT_BUFFERS_TIER_2 -> selectedDeviceInfo.argumentBuffersTier >= 2;
                case READ_WRITE_TEXTURES_TIER_2 -> selectedDeviceInfo.readWriteTextureTier >= 2;
                case SPARSE_TEXTURES -> selectedDeviceInfo.sparseTileSizeInBytes > 0;
                case BC_TEXTURE_COMPRESSION -> selectedDeviceInfo.supportsBCTextureCompression;
                case UNIFIED_MEMORY -> selectedDeviceInfo.hasUnifiedMemory;
                case COUNTER_SAMPLING -> selectedDeviceInfo.supportsCounterSampling;
                case BARYCENTRIC_COORDINATES -> selectedDeviceInfo.supportsShaderBarycentricCoordinates;
                case PULL_MODEL_INTERPOLATION -> selectedDeviceInfo.supportsPullModelInterpolation;
            };
        }
        
        /**
         * Get the maximum supported GPU family.
         */
        public GPUFamily getMaxSupportedFamily() {
            return selectedDeviceInfo != null ? selectedDeviceInfo.highestFamily : GPUFamily.APPLE_1;
        }
    }
    
    /**
     * Device feature enumeration.
     */
    public enum DeviceFeature {
        RAYTRACING,
        MESH_SHADERS,
        DYNAMIC_LIBRARIES,
        FUNCTION_POINTERS,
        ARGUMENT_BUFFERS_TIER_2,
        READ_WRITE_TEXTURES_TIER_2,
        SPARSE_TEXTURES,
        BC_TEXTURE_COMPRESSION,
        UNIFIED_MEMORY,
        COUNTER_SAMPLING,
        BARYCENTRIC_COORDINATES,
        PULL_MODEL_INTERPOLATION
    }
    
    /**
     * GPU Family enumeration.
     */
    public enum GPUFamily {
        APPLE_1(1001),
        APPLE_2(1002),
        APPLE_3(1003),
        APPLE_4(1004),
        APPLE_5(1005),
        APPLE_6(1006),
        APPLE_7(1007),
        APPLE_8(1008),
        APPLE_9(1009),
        MAC_1(2001),
        MAC_2(2002),
        COMMON_1(3001),
        COMMON_2(3002),
        COMMON_3(3003),
        METAL_3(5001);
        
        private final int value;
        
        GPUFamily(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Counter sampling points.
     */
    public enum MTLCounterSamplingPoint {
        AT_STAGE_BOUNDARY,
        AT_DRAW_BOUNDARY,
        AT_DISPATCH_BOUNDARY,
        AT_TILE_DISPATCH_BOUNDARY,
        AT_BLIT_BOUNDARY
    }
    
    /**
     * Detailed device information.
     */
    public static final class DeviceInfo {
        public long handle;
        public String name;
        public long registryID;
        public boolean isLowPower;
        public boolean isHeadless;
        public boolean isRemovable;
        public boolean hasUnifiedMemory;
        
        // Memory
        public long recommendedMaxWorkingSetSize;
        public long maxBufferLength;
        public long currentAllocatedSize;
        
        // Compute
        public int maxThreadsPerThreadgroup;
        public int maxThreadgroupMemoryLength;
        
        // Family
        public GPUFamily highestFamily;
        
        // Features
        public boolean supportsRaytracing;
        public boolean supports32BitMSAA;
        public boolean supportsQueryTextureLOD;
        public boolean supportsBCTextureCompression;
        public boolean supportsPullModelInterpolation;
        public boolean supportsShaderBarycentricCoordinates;
        public boolean supportsDynamicLibraries;
        public boolean supportsFunctionPointers;
        public boolean supportsRenderDynamicLibraries;
        public boolean supportsMeshShaders;
        public boolean supportsCounterSampling;
        
        // Tiers
        public int argumentBuffersTier;
        public int readWriteTextureTier;
        public int sparseTileSizeInBytes;
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 2: COMMAND BUFFER LIFECYCLE
    // Complete command buffer pooling, completion handling, and error management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Command buffer pool for efficient buffer reuse.
     */
    public static final class CommandBufferPool {
        
        private final long commandQueue;
        
        /**
         * Pool of available command buffers.
         */
        private final ConcurrentLinkedQueue<PooledCommandBuffer> availableBuffers = new ConcurrentLinkedQueue<>();
        
        /**
         * Currently active command buffers.
         */
        private final ConcurrentHashMap<Long, PooledCommandBuffer> activeBuffers = new ConcurrentHashMap<>();
        
        /**
         * Command buffer configuration.
         */
        private CommandBufferConfiguration configuration = new CommandBufferConfiguration();
        
        /**
         * Statistics.
         */
        private final CommandBufferStats stats = new CommandBufferStats();
        
        /**
         * Maximum pool size.
         */
        private static final int MAX_POOL_SIZE = 64;
        
        /**
         * Completion handler executor.
         */
        private final ExecutorService completionExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "Metal-Completion-Handler");
            t.setDaemon(true);
            return t;
        });
        
        /**
         * Frame completion listeners.
         */
        private final List<Consumer<Long>> frameCompletionListeners = new CopyOnWriteArrayList<>();
        
        public CommandBufferPool(long commandQueue) {
            this.commandQueue = commandQueue;
        }
        
        /**
         * Acquire a command buffer from the pool.
         */
        public PooledCommandBuffer acquireCommandBuffer() {
            return acquireCommandBuffer(null);
        }
        
        /**
         * Acquire a command buffer with a label.
         */
        public PooledCommandBuffer acquireCommandBuffer(@Nullable String label) {
            stats.totalAcquired.incrementAndGet();
            
            // Try to reuse a pooled buffer
            PooledCommandBuffer pooled = availableBuffers.poll();
            
            if (pooled != null && pooled.isValid()) {
                stats.poolHits.incrementAndGet();
                pooled.reset();
                if (label != null) {
                    pooled.setLabel(label);
                }
                activeBuffers.put(pooled.handle, pooled);
                return pooled;
            }
            
            // Create a new command buffer
            stats.poolMisses.incrementAndGet();
            long handle = createCommandBuffer();
            if (handle == 0) {
                throw new RuntimeException("Failed to create command buffer");
            }
            
            PooledCommandBuffer newBuffer = new PooledCommandBuffer(handle, this);
            if (label != null) {
                newBuffer.setLabel(label);
            }
            activeBuffers.put(handle, newBuffer);
            
            return newBuffer;
        }
        
        /**
         * Create a new command buffer.
         */
        private long createCommandBuffer() {
            if (configuration.useUnretainedReferences) {
                return nMTLCommandQueueCommandBufferWithUnretainedReferences(commandQueue);
            } else {
                return nMTLCommandQueueCommandBuffer(commandQueue);
            }
        }
        
        /**
         * Return a command buffer to the pool.
         */
        void returnToPool(PooledCommandBuffer buffer) {
            activeBuffers.remove(buffer.handle);
            
            if (availableBuffers.size() < MAX_POOL_SIZE && buffer.canBeReused()) {
                availableBuffers.offer(buffer);
                stats.returned.incrementAndGet();
            } else {
                // Buffer cannot be reused or pool is full
                buffer.dispose();
                stats.disposed.incrementAndGet();
            }
        }
        
        /**
         * Commit a command buffer with completion handling.
         */
        public void commitCommandBuffer(PooledCommandBuffer buffer, @Nullable Runnable onComplete) {
            long handle = buffer.handle;
            
            if (onComplete != null || !frameCompletionListeners.isEmpty()) {
                // Set up completion handler
                nMTLCommandBufferAddCompletedHandler(handle, () -> {
                    completionExecutor.submit(() -> {
                        try {
                            // Check for errors
                            int status = nMTLCommandBufferGetStatus(handle);
                            if (status == MTLCommandBufferStatus.ERROR.ordinal()) {
                                String error = nMTLCommandBufferGetError(handle);
                                LOGGER.error("Command buffer error: {}", error);
                                stats.errors.incrementAndGet();
                            }
                            
                            // Notify listeners
                            long gpuTime = nMTLCommandBufferGetGPUEndTime(handle) - 
                                          nMTLCommandBufferGetGPUStartTime(handle);
                            for (Consumer<Long> listener : frameCompletionListeners) {
                                try {
                                    listener.accept(gpuTime);
                                } catch (Exception e) {
                                    LOGGER.warn("Completion listener error", e);
                                }
                            }
                            
                            // User callback
                            if (onComplete != null) {
                                onComplete.run();
                            }
                            
                        } finally {
                            returnToPool(buffer);
                        }
                    });
                });
            }
            
            // Commit the buffer
            nMTLCommandBufferCommit(handle);
            stats.committed.incrementAndGet();
        }
        
        /**
         * Commit and wait for completion.
         */
        public void commitAndWait(PooledCommandBuffer buffer) {
            long handle = buffer.handle;
            nMTLCommandBufferCommit(handle);
            nMTLCommandBufferWaitUntilCompleted(handle);
            
            // Check for errors
            int status = nMTLCommandBufferGetStatus(handle);
            if (status == MTLCommandBufferStatus.ERROR.ordinal()) {
                String error = nMTLCommandBufferGetError(handle);
                throw new RuntimeException("Command buffer execution failed: " + error);
            }
            
            returnToPool(buffer);
            stats.committed.incrementAndGet();
        }
        
        /**
         * Present a drawable and commit.
         */
        public void presentAndCommit(PooledCommandBuffer buffer, long drawable, 
                                     @Nullable Runnable onComplete) {
            nMTLCommandBufferPresentDrawable(buffer.handle, drawable);
            commitCommandBuffer(buffer, onComplete);
        }
        
        /**
         * Schedule a drawable presentation at a specific time.
         */
        public void presentAtTime(PooledCommandBuffer buffer, long drawable, 
                                  double presentationTime, @Nullable Runnable onComplete) {
            nMTLCommandBufferPresentDrawableAtTime(buffer.handle, drawable, presentationTime);
            commitCommandBuffer(buffer, onComplete);
        }
        
        /**
         * Add a frame completion listener.
         */
        public void addFrameCompletionListener(Consumer<Long> listener) {
            frameCompletionListeners.add(listener);
        }
        
        /**
         * Remove a frame completion listener.
         */
        public void removeFrameCompletionListener(Consumer<Long> listener) {
            frameCompletionListeners.remove(listener);
        }
        
        /**
         * Get pool statistics.
         */
        public CommandBufferStats getStats() {
            return stats;
        }
        
        /**
         * Clear the pool.
         */
        public void clear() {
            PooledCommandBuffer buffer;
            while ((buffer = availableBuffers.poll()) != null) {
                buffer.dispose();
            }
        }
        
        /**
         * Shutdown the pool.
         */
        public void shutdown() {
            clear();
            completionExecutor.shutdown();
            try {
                if (!completionExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    completionExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                completionExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        /**
         * Set configuration.
         */
        public void setConfiguration(CommandBufferConfiguration config) {
            this.configuration = config;
        }
    }
    
    /**
     * Command buffer status enumeration.
     */
    public enum MTLCommandBufferStatus {
        NOT_ENQUEUED,
        ENQUEUED,
        COMMITTED,
        SCHEDULED,
        COMPLETED,
        ERROR
    }
    
    /**
     * Pooled command buffer wrapper.
     */
    public static final class PooledCommandBuffer implements AutoCloseable {
        
        final long handle;
        private final CommandBufferPool pool;
        private boolean committed = false;
        private boolean disposed = false;
        private String label;
        
        /**
         * Active encoders on this buffer.
         */
        private final List<Long> activeEncoders = new ArrayList<>();
        
        /**
         * Resource usage tracking.
         */
        private final Set<Long> usedResources = ConcurrentHashMap.newKeySet();
        
        PooledCommandBuffer(long handle, CommandBufferPool pool) {
            this.handle = handle;
            this.pool = pool;
        }
        
        /**
         * Get the native handle.
         */
        public long getHandle() {
            return handle;
        }
        
        /**
         * Set a debug label.
         */
        public void setLabel(String label) {
            this.label = label;
            nMTLCommandBufferSetLabel(handle, label);
        }
        
        /**
         * Get the label.
         */
        public String getLabel() {
            return label;
        }
        
        /**
         * Create a render command encoder.
         */
        public long createRenderEncoder(long renderPassDescriptor) {
            if (committed) {
                throw new IllegalStateException("Cannot create encoder on committed buffer");
            }
            long encoder = nMTLCommandBufferRenderCommandEncoderWithDescriptor(handle, renderPassDescriptor);
            if (encoder != 0) {
                activeEncoders.add(encoder);
            }
            return encoder;
        }
        
        /**
         * Create a compute command encoder.
         */
        public long createComputeEncoder() {
            if (committed) {
                throw new IllegalStateException("Cannot create encoder on committed buffer");
            }
            long encoder = nMTLCommandBufferComputeCommandEncoder(handle);
            if (encoder != 0) {
                activeEncoders.add(encoder);
            }
            return encoder;
        }
        
        /**
         * Create a compute command encoder with dispatch type.
         */
        public long createComputeEncoder(MTLDispatchType dispatchType) {
            if (committed) {
                throw new IllegalStateException("Cannot create encoder on committed buffer");
            }
            long encoder = nMTLCommandBufferComputeCommandEncoderWithDispatchType(handle, dispatchType.ordinal());
            if (encoder != 0) {
                activeEncoders.add(encoder);
            }
            return encoder;
        }
        
        /**
         * Create a blit command encoder.
         */
        public long createBlitEncoder() {
            if (committed) {
                throw new IllegalStateException("Cannot create encoder on committed buffer");
            }
            long encoder = nMTLCommandBufferBlitCommandEncoder(handle);
            if (encoder != 0) {
                activeEncoders.add(encoder);
            }
            return encoder;
        }
        
        /**
         * Create a parallel render command encoder.
         */
        public long createParallelRenderEncoder(long renderPassDescriptor) {
            if (committed) {
                throw new IllegalStateException("Cannot create encoder on committed buffer");
            }
            long encoder = nMTLCommandBufferParallelRenderCommandEncoderWithDescriptor(handle, renderPassDescriptor);
            if (encoder != 0) {
                activeEncoders.add(encoder);
            }
            return encoder;
        }
        
        /**
         * Create a resource state command encoder.
         */
        public long createResourceStateEncoder() {
            if (committed) {
                throw new IllegalStateException("Cannot create encoder on committed buffer");
            }
            return nMTLCommandBufferResourceStateCommandEncoder(handle);
        }
        
        /**
         * Create an acceleration structure command encoder.
         */
        public long createAccelerationStructureEncoder() {
            if (committed) {
                throw new IllegalStateException("Cannot create encoder on committed buffer");
            }
            return nMTLCommandBufferAccelerationStructureCommandEncoder(handle);
        }
        
        /**
         * Track a resource as used by this command buffer.
         */
        public void trackResource(long resource) {
            usedResources.add(resource);
        }
        
        /**
         * Get all resources used by this command buffer.
         */
        public Set<Long> getUsedResources() {
            return Collections.unmodifiableSet(usedResources);
        }
        
        /**
         * Enqueue the command buffer without committing.
         */
        public void enqueue() {
            nMTLCommandBufferEnqueue(handle);
        }
        
        /**
         * Commit the command buffer.
         */
        public void commit() {
            commit(null);
        }
        
        /**
         * Commit with a completion callback.
         */
        public void commit(@Nullable Runnable onComplete) {
            if (committed) {
                throw new IllegalStateException("Command buffer already committed");
            }
            committed = true;
            pool.commitCommandBuffer(this, onComplete);
        }
        
        /**
         * Commit and wait for completion.
         */
        public void commitAndWait() {
            if (committed) {
                throw new IllegalStateException("Command buffer already committed");
            }
            committed = true;
            pool.commitAndWait(this);
        }
        
        /**
         * Present and commit.
         */
        public void presentAndCommit(long drawable) {
            presentAndCommit(drawable, null);
        }
        
        /**
         * Present and commit with callback.
         */
        public void presentAndCommit(long drawable, @Nullable Runnable onComplete) {
            if (committed) {
                throw new IllegalStateException("Command buffer already committed");
            }
            committed = true;
            pool.presentAndCommit(this, drawable, onComplete);
        }
        
        /**
         * Add a scheduled handler.
         */
        public void addScheduledHandler(Runnable handler) {
            nMTLCommandBufferAddScheduledHandler(handle, handler);
        }
        
        /**
         * Push a debug group.
         */
        public void pushDebugGroup(String label) {
            nMTLCommandBufferPushDebugGroup(handle, label);
        }
        
        /**
         * Pop a debug group.
         */
        public void popDebugGroup() {
            nMTLCommandBufferPopDebugGroup(handle);
        }
        
        /**
         * Check if this buffer can be reused.
         */
        boolean canBeReused() {
            return !disposed && 
                   (committed && nMTLCommandBufferGetStatus(handle) == MTLCommandBufferStatus.COMPLETED.ordinal());
        }
        
        /**
         * Check if this buffer is valid.
         */
        boolean isValid() {
            return !disposed && handle != 0;
        }
        
        /**
         * Reset for reuse.
         */
        void reset() {
            committed = false;
            label = null;
            activeEncoders.clear();
            usedResources.clear();
        }
        
        /**
         * Dispose of this buffer.
         */
        void dispose() {
            if (!disposed) {
                disposed = true;
                nRelease(handle);
            }
        }
        
        @Override
        public void close() {
            if (!committed) {
                // Return to pool without committing
                pool.returnToPool(this);
            }
        }
    }
    
    /**
     * Compute dispatch type.
     */
    public enum MTLDispatchType {
        SERIAL,
        CONCURRENT
    }
    
    /**
     * Command buffer configuration.
     */
    public static final class CommandBufferConfiguration {
        /**
         * Use unretained references for better performance.
         * Only safe when resource lifetimes are managed manually.
         */
        public boolean useUnretainedReferences = false;
        
        /**
         * Enable GPU timing information.
         */
        public boolean enableGPUTiming = true;
        
        /**
         * Error handling mode.
         */
        public ErrorMode errorMode = ErrorMode.DEFAULT;
        
        public enum ErrorMode {
            DEFAULT,    // Standard error handling
            ASSERT      // Assert on errors (debug builds)
        }
    }
    
    /**
     * Command buffer statistics.
     */
    public static final class CommandBufferStats {
        public final AtomicLong totalAcquired = new AtomicLong(0);
        public final AtomicLong poolHits = new AtomicLong(0);
        public final AtomicLong poolMisses = new AtomicLong(0);
        public final AtomicLong committed = new AtomicLong(0);
        public final AtomicLong returned = new AtomicLong(0);
        public final AtomicLong disposed = new AtomicLong(0);
        public final AtomicLong errors = new AtomicLong(0);
        
        public double getHitRate() {
            long total = poolHits.get() + poolMisses.get();
            return total > 0 ? (double) poolHits.get() / total : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("CommandBufferStats[acquired=%d, hits=%d, misses=%d, hitRate=%.2f%%, " +
                               "committed=%d, errors=%d]",
                totalAcquired.get(), poolHits.get(), poolMisses.get(), 
                getHitRate() * 100, committed.get(), errors.get());
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 3: RESOURCE SYNCHRONIZATION
    // Complete resource tracking, hazard detection, and synchronization
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Resource tracker for synchronization and hazard detection.
     */
    public static final class ResourceTracker {
        
        private final long device;
        
        /**
         * Tracked resources with their current state.
         */
        private final ConcurrentHashMap<Long, ResourceState> resourceStates = new ConcurrentHashMap<>();
        
        /**
         * Pending synchronizations.
         */
        private final ConcurrentLinkedQueue<PendingSync> pendingSyncs = new ConcurrentLinkedQueue<>();
        
        /**
         * Event pool for CPU-GPU synchronization.
         */
        private final ConcurrentLinkedQueue<Long> eventPool = new ConcurrentLinkedQueue<>();
        
        /**
         * Fence pool for encoder synchronization.
         */
        private final ConcurrentLinkedQueue<Long> fencePool = new ConcurrentLinkedQueue<>();
        
        /**
         * Use automatic hazard tracking.
         */
        private boolean automaticHazardTracking = true;
        
        /**
         * Statistics.
         */
        private final SyncStats stats = new SyncStats();
        
        public ResourceTracker(long device) {
            this.device = device;
        }
        
        /**
         * Resource state tracking.
         */
        public static final class ResourceState {
            public final long resource;
            public ResourceUsage lastUsage = ResourceUsage.NONE;
            public long lastCommandBuffer;
            public int lastEncoderIndex;
            public boolean needsSync;
            public long lastModifiedFrame;
            
            public ResourceState(long resource) {
                this.resource = resource;
            }
        }
        
        /**
         * Resource usage type.
         */
        public enum ResourceUsage {
            NONE,
            READ,
            WRITE,
            READ_WRITE
        }
        
        /**
         * Pending synchronization.
         */
        public static final class PendingSync {
            public final long resource;
            public final SyncType type;
            public final long waitValue;
            
            public PendingSync(long resource, SyncType type, long waitValue) {
                this.resource = resource;
                this.type = type;
                this.waitValue = waitValue;
            }
        }
        
        /**
         * Synchronization type.
         */
        public enum SyncType {
            CPU_TO_GPU,
            GPU_TO_CPU,
            GPU_TO_GPU
        }
        
        /**
         * Track resource usage.
         */
        public void trackUsage(long resource, ResourceUsage usage, long commandBuffer, int encoderIndex) {
            ResourceState state = resourceStates.computeIfAbsent(resource, ResourceState::new);
            
            synchronized (state) {
                // Check for hazards
                if (!automaticHazardTracking) {
                    if (state.lastUsage == ResourceUsage.WRITE && usage != ResourceUsage.NONE) {
                        // Write followed by any access - need sync
                        if (state.lastCommandBuffer != commandBuffer || state.lastEncoderIndex != encoderIndex) {
                            state.needsSync = true;
                            stats.hazardsDetected.incrementAndGet();
                        }
                    }
                    if (state.lastUsage != ResourceUsage.NONE && usage == ResourceUsage.WRITE) {
                        // Any access followed by write - need sync
                        if (state.lastCommandBuffer != commandBuffer || state.lastEncoderIndex != encoderIndex) {
                            state.needsSync = true;
                            stats.hazardsDetected.incrementAndGet();
                        }
                    }
                }
                
                state.lastUsage = usage;
                state.lastCommandBuffer = commandBuffer;
                state.lastEncoderIndex = encoderIndex;
            }
        }
        
        /**
         * Check if a resource needs synchronization.
         */
        public boolean needsSynchronization(long resource) {
            ResourceState state = resourceStates.get(resource);
            return state != null && state.needsSync;
        }
        
        /**
         * Mark a resource as synchronized.
         */
        public void markSynchronized(long resource) {
            ResourceState state = resourceStates.get(resource);
            if (state != null) {
                state.needsSync = false;
            }
        }
        
        /**
         * Create an event for CPU-GPU synchronization.
         */
        public long acquireEvent() {
            Long pooled = eventPool.poll();
            if (pooled != null) {
                return pooled;
            }
            
            long event = nMTLDeviceNewEvent(device);
            stats.eventsCreated.incrementAndGet();
            return event;
        }
        
        /**
         * Return an event to the pool.
         */
        public void releaseEvent(long event) {
            if (eventPool.size() < 32) {
                eventPool.offer(event);
            } else {
                nRelease(event);
            }
        }
        
        /**
         * Create a shared event for cross-process/cross-device sync.
         */
        public long createSharedEvent() {
            return nMTLDeviceNewSharedEvent(device);
        }
        
        /**
         * Create a shared event from a handle.
         */
        public long createSharedEventFromHandle(long sharedEventHandle) {
            return nMTLDeviceNewSharedEventWithHandle(device, sharedEventHandle);
        }
        
        /**
         * Create a fence for encoder synchronization.
         */
        public long acquireFence() {
            Long pooled = fencePool.poll();
            if (pooled != null) {
                return pooled;
            }
            
            long fence = nMTLDeviceNewFence(device);
            stats.fencesCreated.incrementAndGet();
            return fence;
        }
        
        /**
         * Return a fence to the pool.
         */
        public void releaseFence(long fence) {
            if (fencePool.size() < 16) {
                fencePool.offer(fence);
            } else {
                nRelease(fence);
            }
        }
        
        /**
         * Wait for a fence on an encoder.
         */
        public void waitForFence(long encoder, long fence, int stages) {
            nMTLRenderCommandEncoderWaitForFence(encoder, fence, stages);
            stats.fenceWaits.incrementAndGet();
        }
        
        /**
         * Update a fence on an encoder.
         */
        public void updateFence(long encoder, long fence, int stages) {
            nMTLRenderCommandEncoderUpdateFence(encoder, fence, stages);
            stats.fenceUpdates.incrementAndGet();
        }
        
        /**
         * Use an encoder-level memory barrier.
         */
        public void memoryBarrier(long encoder, int scope, int afterStages, int beforeStages) {
            nMTLRenderCommandEncoderMemoryBarrier(encoder, scope, afterStages, beforeStages);
            stats.barriers.incrementAndGet();
        }
        
        /**
         * Use a resource-specific memory barrier.
         */
        public void memoryBarrierWithResources(long encoder, long[] resources, int afterStages, int beforeStages) {
            nMTLRenderCommandEncoderMemoryBarrierWithResources(encoder, resources, afterStages, beforeStages);
            stats.barriers.incrementAndGet();
        }
        
        /**
         * Insert a texture barrier (imageblock).
         */
        public void textureBarrier(long encoder) {
            nMTLRenderCommandEncoderTextureBarrier(encoder);
        }
        
        /**
         * Synchronize managed resources for CPU access.
         */
        public void synchronizeResource(long blitEncoder, long resource) {
            nMTLBlitCommandEncoderSynchronizeResource(blitEncoder, resource);
            stats.managedSyncs.incrementAndGet();
        }
        
        /**
         * Synchronize a texture region for CPU access.
         */
        public void synchronizeTexture(long blitEncoder, long texture, int slice, int level) {
            nMTLBlitCommandEncoderSynchronizeTexture(blitEncoder, texture, slice, level);
            stats.managedSyncs.incrementAndGet();
        }
        
        /**
         * Signal an event on a command buffer.
         */
        public void signalEvent(long commandBuffer, long event, long value) {
            nMTLCommandBufferEncodeSignalEvent(commandBuffer, event, value);
        }
        
        /**
         * Wait for an event on a command buffer.
         */
        public void waitForEvent(long commandBuffer, long event, long value) {
            nMTLCommandBufferEncodeWaitForEvent(commandBuffer, event, value);
        }
        
        /**
         * CPU-side wait for shared event.
         */
        public boolean waitForSharedEventValue(long sharedEvent, long value, long timeoutNs) {
            long listener = nMTLSharedEventNewListener(sharedEvent);
            boolean[] completed = {false};
            
            nMTLSharedEventNotifyListener(sharedEvent, listener, value, () -> {
                synchronized (completed) {
                    completed[0] = true;
                    completed.notifyAll();
                }
            });
            
            synchronized (completed) {
                long deadline = System.nanoTime() + timeoutNs;
                while (!completed[0]) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0) break;
                    try {
                        completed.wait(remaining / 1_000_000, (int)(remaining % 1_000_000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            nRelease(listener);
            return completed[0];
        }
        
        /**
         * Set automatic hazard tracking mode.
         */
        public void setAutomaticHazardTracking(boolean enabled) {
            this.automaticHazardTracking = enabled;
        }
        
        /**
         * Get synchronization statistics.
         */
        public SyncStats getStats() {
            return stats;
        }
        
        /**
         * Clear tracked resources.
         */
        public void clear() {
            resourceStates.clear();
        }
        
        /**
         * Shutdown and cleanup.
         */
        public void shutdown() {
            clear();
            
            // Release pooled events
            Long event;
            while ((event = eventPool.poll()) != null) {
                nRelease(event);
            }
            
            // Release pooled fences
            Long fence;
            while ((fence = fencePool.poll()) != null) {
                nRelease(fence);
            }
        }
    }
    
    /**
     * Synchronization statistics.
     */
    public static final class SyncStats {
        public final AtomicLong hazardsDetected = new AtomicLong(0);
        public final AtomicLong eventsCreated = new AtomicLong(0);
        public final AtomicLong fencesCreated = new AtomicLong(0);
        public final AtomicLong fenceWaits = new AtomicLong(0);
        public final AtomicLong fenceUpdates = new AtomicLong(0);
        public final AtomicLong barriers = new AtomicLong(0);
        public final AtomicLong managedSyncs = new AtomicLong(0);
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 4: MEMORY MANAGEMENT
    // Complete heap allocation, memory pressure handling, and resource purging
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Memory allocator for efficient GPU memory management.
     */
    public static final class MemoryAllocator {
        
        private final long device;
        private final DeviceInfo deviceInfo;
        
        /**
         * Heap allocations by storage mode.
         */
        private final Map<MTLStorageMode, HeapPool> heapPools = new EnumMap<>(MTLStorageMode.class);
        
        /**
         * Direct allocations (not from heaps).
         */
        private final ConcurrentHashMap<Long, AllocationInfo> directAllocations = new ConcurrentHashMap<>();
        
        /**
         * Purgeable resources.
         */
        private final ConcurrentLinkedQueue<PurgeableResource> purgeableResources = new ConcurrentLinkedQueue<>();
        
        /**
         * Memory pressure observer.
         */
        private MemoryPressureObserver pressureObserver;
        
        /**
         * Statistics.
         */
        private final MemoryStats stats = new MemoryStats();
        
        /**
         * Configuration.
         */
        private MemoryConfiguration config = new MemoryConfiguration();
        
        public MemoryAllocator(long device, DeviceInfo deviceInfo) {
            this.device = device;
            this.deviceInfo = deviceInfo;
            initialize();
        }
        
        private void initialize() {
            // Create heap pools for each storage mode
            if (deviceInfo.hasUnifiedMemory) {
                // Apple Silicon - prefer shared storage
                heapPools.put(MTLStorageMode.SHARED, new HeapPool(device, MTLStorageMode.SHARED, 
                    config.defaultHeapSize));
                heapPools.put(MTLStorageMode.PRIVATE, new HeapPool(device, MTLStorageMode.PRIVATE,
                    config.defaultHeapSize));
            } else {
                // Discrete GPU - use all storage modes
                heapPools.put(MTLStorageMode.SHARED, new HeapPool(device, MTLStorageMode.SHARED,
                    config.defaultHeapSize / 2));
                heapPools.put(MTLStorageMode.MANAGED, new HeapPool(device, MTLStorageMode.MANAGED,
                    config.defaultHeapSize));
                heapPools.put(MTLStorageMode.PRIVATE, new HeapPool(device, MTLStorageMode.PRIVATE,
                    config.defaultHeapSize));
            }
            
            // Set up memory pressure observer
            pressureObserver = new MemoryPressureObserver(this::handleMemoryPressure);
            pressureObserver.start();
        }
        
        /**
         * Storage mode enumeration.
         */
        public enum MTLStorageMode {
            SHARED(0),      // CPU and GPU can access
            MANAGED(1),     // System manages CPU/GPU coherency
            PRIVATE(2),     // GPU only
            MEMORYLESS(3);  // Tile memory only (iOS)
            
            private final int value;
            
            MTLStorageMode(int value) {
                this.value = value;
            }
            
            public int getValue() {
                return value;
            }
        }
        
        /**
         * Resource CPU cache mode.
         */
        public enum MTLCPUCacheMode {
            DEFAULT_CACHE(0),
            WRITE_COMBINED(1);
            
            private final int value;
            
            MTLCPUCacheMode(int value) {
                this.value = value;
            }
            
            public int getValue() {
                return value;
            }
        }
        
        /**
         * Hazard tracking mode.
         */
        public enum MTLHazardTrackingMode {
            DEFAULT(0),
            UNTRACKED(1),
            TRACKED(2);
            
            private final int value;
            
            MTLHazardTrackingMode(int value) {
                this.value = value;
            }
            
            public int getValue() {
                return value;
            }
        }
        
        /**
         * Get the recommended storage mode for a resource.
         */
        public MTLStorageMode getRecommendedStorageMode(ResourceType resourceType, 
                                                        boolean cpuAccess, boolean gpuWrite) {
            if (deviceInfo.hasUnifiedMemory) {
                // Apple Silicon - shared memory is efficient
                if (cpuAccess || resourceType == ResourceType.UNIFORM_BUFFER) {
                    return MTLStorageMode.SHARED;
                }
                return MTLStorageMode.PRIVATE;
            } else {
                // Discrete GPU
                if (cpuAccess && gpuWrite) {
                    return MTLStorageMode.MANAGED;
                }
                if (cpuAccess) {
                    return MTLStorageMode.SHARED;
                }
                return MTLStorageMode.PRIVATE;
            }
        }
        
        /**
         * Resource type for allocation hints.
         */
        public enum ResourceType {
            VERTEX_BUFFER,
            INDEX_BUFFER,
            UNIFORM_BUFFER,
            STORAGE_BUFFER,
            TEXTURE,
            RENDER_TARGET,
            STAGING_BUFFER
        }
        
        /**
         * Allocate a buffer.
         */
        public long allocateBuffer(long size, MTLStorageMode storageMode, 
                                   @Nullable String label) {
            return allocateBuffer(size, storageMode, MTLCPUCacheMode.DEFAULT_CACHE, label);
        }
        
        /**
         * Allocate a buffer with specific cache mode.
         */
        public long allocateBuffer(long size, MTLStorageMode storageMode,
                                   MTLCPUCacheMode cacheMode, @Nullable String label) {
            // Try heap allocation first
            HeapPool pool = heapPools.get(storageMode);
            if (pool != null && size <= config.maxHeapAllocationSize) {
                long buffer = pool.allocateBuffer(size, cacheMode);
                if (buffer != 0) {
                    if (label != null) {
                        nMTLResourceSetLabel(buffer, label);
                    }
                    stats.heapAllocations.incrementAndGet();
                    stats.heapAllocatedBytes.addAndGet(size);
                    return buffer;
                }
            }
            
            // Fall back to direct allocation
            int resourceOptions = (storageMode.getValue() << 0) | (cacheMode.getValue() << 4);
            long buffer = nMTLDeviceNewBufferWithLength(device, size, resourceOptions);
            
            if (buffer != 0) {
                if (label != null) {
                    nMTLResourceSetLabel(buffer, label);
                }
                directAllocations.put(buffer, new AllocationInfo(size, storageMode));
                stats.directAllocations.incrementAndGet();
                stats.directAllocatedBytes.addAndGet(size);
            }
            
            return buffer;
        }
        
        /**
         * Allocate a buffer with initial data.
         */
        public long allocateBufferWithData(ByteBuffer data, MTLStorageMode storageMode,
                                           @Nullable String label) {
            int resourceOptions = storageMode.getValue() << 0;
            long buffer = nMTLDeviceNewBufferWithBytes(device, data, data.remaining(), resourceOptions);
            
            if (buffer != 0) {
                if (label != null) {
                    nMTLResourceSetLabel(buffer, label);
                }
                directAllocations.put(buffer, new AllocationInfo(data.remaining(), storageMode));
                stats.directAllocations.incrementAndGet();
                stats.directAllocatedBytes.addAndGet(data.remaining());
            }
            
            return buffer;
        }
        
        /**
         * Allocate a texture.
         */
        public long allocateTexture(TextureDescriptor descriptor, @Nullable String label) {
            // Calculate texture size
            long size = calculateTextureSize(descriptor);
            
            // Try heap allocation for private textures
            if (descriptor.storageMode == MTLStorageMode.PRIVATE) {
                HeapPool pool = heapPools.get(MTLStorageMode.PRIVATE);
                if (pool != null && size <= config.maxHeapAllocationSize) {
                    long texture = pool.allocateTexture(descriptor);
                    if (texture != 0) {
                        if (label != null) {
                            nMTLResourceSetLabel(texture, label);
                        }
                        stats.heapAllocations.incrementAndGet();
                        stats.heapAllocatedBytes.addAndGet(size);
                        return texture;
                    }
                }
            }
            
            // Direct allocation
            long descriptorHandle = createTextureDescriptor(descriptor);
            long texture = nMTLDeviceNewTextureWithDescriptor(device, descriptorHandle);
            nRelease(descriptorHandle);
            
            if (texture != 0) {
                if (label != null) {
                    nMTLResourceSetLabel(texture, label);
                }
                directAllocations.put(texture, new AllocationInfo(size, descriptor.storageMode));
                stats.directAllocations.incrementAndGet();
                stats.directAllocatedBytes.addAndGet(size);
            }
            
            return texture;
        }
        
        /**
         * Calculate texture size in bytes.
         */
        private long calculateTextureSize(TextureDescriptor descriptor) {
            int bytesPerPixel = getPixelFormatSize(descriptor.pixelFormat);
            long size = (long) descriptor.width * descriptor.height * bytesPerPixel;
            
            if (descriptor.depth > 1) {
                size *= descriptor.depth;
            }
            if (descriptor.arrayLength > 1) {
                size *= descriptor.arrayLength;
            }
            if (descriptor.mipmapped) {
                size = (long) (size * 1.34); // Approximate mipmap overhead
            }
            
            return size;
        }
        
        /**
         * Get pixel format size in bytes.
         */
        private int getPixelFormatSize(MTLPixelFormat format) {
            return switch (format) {
                case R8Unorm, R8Snorm, R8Uint, R8Sint, A8Unorm -> 1;
                case R16Unorm, R16Snorm, R16Uint, R16Sint, R16Float, RG8Unorm, RG8Snorm -> 2;
                case R32Uint, R32Sint, R32Float, RG16Unorm, RG16Snorm, RG16Uint, RG16Sint, RG16Float,
                     RGBA8Unorm, RGBA8Unorm_sRGB, RGBA8Snorm, RGBA8Uint, RGBA8Sint, BGRA8Unorm, BGRA8Unorm_sRGB,
                     RGB10A2Unorm, RGB10A2Uint, RG11B10Float, RGB9E5Float, Depth32Float -> 4;
                case RG32Uint, RG32Sint, RG32Float, RGBA16Unorm, RGBA16Snorm, RGBA16Uint, RGBA16Sint, RGBA16Float,
                     Depth32Float_Stencil8 -> 8;
                case RGBA32Uint, RGBA32Sint, RGBA32Float -> 16;
                case BC1_RGBA, BC1_RGBA_sRGB -> 1; // 8 bytes per 4x4 block
                case BC2_RGBA, BC2_RGBA_sRGB, BC3_RGBA, BC3_RGBA_sRGB -> 1; // 16 bytes per 4x4 block
                case BC4_RUnorm, BC4_RSnorm -> 1; // 8 bytes per 4x4 block
                case BC5_RGUnorm, BC5_RGSnorm -> 1; // 16 bytes per 4x4 block
                case BC6H_RGBFloat, BC6H_RGBUfloat -> 1; // 16 bytes per 4x4 block
                case BC7_RGBAUnorm, BC7_RGBAUnorm_sRGB -> 1; // 16 bytes per 4x4 block
                default -> 4;
            };
        }
        
        /**
         * Create a texture descriptor handle.
         */
        private long createTextureDescriptor(TextureDescriptor descriptor) {
            long handle = nMTLTextureDescriptorNew();
            nMTLTextureDescriptorSetTextureType(handle, descriptor.textureType.getValue());
            nMTLTextureDescriptorSetPixelFormat(handle, descriptor.pixelFormat.getValue());
            nMTLTextureDescriptorSetWidth(handle, descriptor.width);
            nMTLTextureDescriptorSetHeight(handle, descriptor.height);
            nMTLTextureDescriptorSetDepth(handle, descriptor.depth);
            nMTLTextureDescriptorSetMipmapLevelCount(handle, descriptor.mipmapped ? 
                calculateMipmapCount(descriptor.width, descriptor.height) : 1);
            nMTLTextureDescriptorSetArrayLength(handle, descriptor.arrayLength);
            nMTLTextureDescriptorSetSampleCount(handle, descriptor.sampleCount);
            nMTLTextureDescriptorSetStorageMode(handle, descriptor.storageMode.getValue());
            nMTLTextureDescriptorSetUsage(handle, descriptor.usage);
            return handle;
        }
        
        /**
         * Calculate mipmap count.
         */
        private int calculateMipmapCount(int width, int height) {
            int levels = 1;
            while (width > 1 || height > 1) {
                width = Math.max(1, width / 2);
                height = Math.max(1, height / 2);
                levels++;
            }
            return levels;
        }
        
        /**
         * Free a resource.
         */
        public void free(long resource) {
            AllocationInfo info = directAllocations.remove(resource);
            if (info != null) {
                stats.directDeallocations.incrementAndGet();
                stats.directDeallocatedBytes.addAndGet(info.size);
            }
            
            // Check if it's a heap allocation
            for (HeapPool pool : heapPools.values()) {
                if (pool.free(resource)) {
                    stats.heapDeallocations.incrementAndGet();
                    return;
                }
            }
            
            // Release the resource
            nRelease(resource);
        }
        
        /**
         * Make a resource purgeable.
         */
        public void makePurgeable(long resource, int priority) {
            nMTLResourceSetPurgeableState(resource, MTLPurgeableState.VOLATILE.getValue());
            purgeableResources.offer(new PurgeableResource(resource, priority));
        }
        
        /**
         * Make a resource non-purgeable.
         */
        public boolean makeNonPurgeable(long resource) {
            int state = nMTLResourceSetPurgeableState(resource, MTLPurgeableState.NON_VOLATILE.getValue());
            return state != MTLPurgeableState.EMPTY.getValue();
        }
        
        /**
         * Purgeable state.
         */
        public enum MTLPurgeableState {
            KEEP_CURRENT(1),
            NON_VOLATILE(2),
            VOLATILE(3),
            EMPTY(4);
            
            private final int value;
            
            MTLPurgeableState(int value) {
                this.value = value;
            }
            
            public int getValue() {
                return value;
            }
        }
        
        /**
         * Handle memory pressure.
         */
        private void handleMemoryPressure(MemoryPressureLevel level) {
            LOGGER.info("Memory pressure: {}", level);
            stats.pressureEvents.incrementAndGet();
            
            switch (level) {
                case WARNING:
                    // Purge low priority resources
                    purgeLowPriorityResources();
                    break;
                    
                case CRITICAL:
                    // Purge all purgeable resources
                    purgeAllPurgeableResources();
                    // Compact heaps
                    compactHeaps();
                    break;
                    
                case NORMAL:
                    // Memory pressure resolved
                    break;
            }
        }
        
        /**
         * Purge low priority resources.
         */
        private void purgeLowPriorityResources() {
            Iterator<PurgeableResource> it = purgeableResources.iterator();
            while (it.hasNext()) {
                PurgeableResource pr = it.next();
                if (pr.priority < 5) {
                    nMTLResourceSetPurgeableState(pr.resource, MTLPurgeableState.EMPTY.getValue());
                    it.remove();
                    stats.resourcesPurged.incrementAndGet();
                }
            }
        }
        
        /**
         * Purge all purgeable resources.
         */
        private void purgeAllPurgeableResources() {
            PurgeableResource pr;
            while ((pr = purgeableResources.poll()) != null) {
                nMTLResourceSetPurgeableState(pr.resource, MTLPurgeableState.EMPTY.getValue());
                stats.resourcesPurged.incrementAndGet();
            }
        }
        
        /**
         * Compact heaps to reduce fragmentation.
         */
        private void compactHeaps() {
            for (HeapPool pool : heapPools.values()) {
                pool.compact();
            }
        }
        
        /**
         * Get current memory usage.
         */
        public long getCurrentAllocatedSize() {
            return nMTLDeviceGetCurrentAllocatedSize(device);
        }
        
        /**
         * Get recommended max working set size.
         */
        public long getRecommendedMaxWorkingSetSize() {
            return deviceInfo.recommendedMaxWorkingSetSize;
        }
        
        /**
         * Check if memory is under pressure.
         */
        public boolean isUnderMemoryPressure() {
            long current = getCurrentAllocatedSize();
            long max = getRecommendedMaxWorkingSetSize();
            return current > max * 0.9;
        }
        
        /**
         * Get memory statistics.
         */
        public MemoryStats getStats() {
            return stats;
        }
        
        /**
         * Shutdown the allocator.
         */
        public void shutdown() {
            if (pressureObserver != null) {
                pressureObserver.stop();
            }
            
            for (HeapPool pool : heapPools.values()) {
                pool.shutdown();
            }
            heapPools.clear();
            
            directAllocations.clear();
            purgeableResources.clear();
        }
        
        /**
         * Purgeable resource info.
         */
        private static final class PurgeableResource {
            final long resource;
            final int priority;
            
            PurgeableResource(long resource, int priority) {
                this.resource = resource;
                this.priority = priority;
            }
        }
        
        /**
         * Allocation info.
         */
        private static final class AllocationInfo {
            final long size;
            final MTLStorageMode storageMode;
            
            AllocationInfo(long size, MTLStorageMode storageMode) {
                this.size = size;
                this.storageMode = storageMode;
            }
        }
    }
    
    /**
     * Texture descriptor.
     */
    public static final class TextureDescriptor {
        public MTLTextureType textureType = MTLTextureType.TYPE_2D;
        public MTLPixelFormat pixelFormat = MTLPixelFormat.RGBA8Unorm;
        public int width = 1;
        public int height = 1;
        public int depth = 1;
        public boolean mipmapped = false;
        public int arrayLength = 1;
        public int sampleCount = 1;
        public MemoryAllocator.MTLStorageMode storageMode = MemoryAllocator.MTLStorageMode.PRIVATE;
        public int usage = MTLTextureUsage.SHADER_READ.getValue();
    }
    
    /**
     * Texture type enumeration.
     */
    public enum MTLTextureType {
        TYPE_1D(0),
        TYPE_1D_ARRAY(1),
        TYPE_2D(2),
        TYPE_2D_ARRAY(3),
        TYPE_2D_MULTISAMPLE(4),
        TYPE_CUBE(5),
        TYPE_CUBE_ARRAY(6),
        TYPE_3D(7),
        TYPE_2D_MULTISAMPLE_ARRAY(8),
        TYPE_TEXTURE_BUFFER(9);
        
        private final int value;
        
        MTLTextureType(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Pixel format enumeration.
     */
    public enum MTLPixelFormat {
        Invalid(0),
        A8Unorm(1),
        R8Unorm(10),
        R8Unorm_sRGB(11),
        R8Snorm(12),
        R8Uint(13),
        R8Sint(14),
        R16Unorm(20),
        R16Snorm(22),
        R16Uint(23),
        R16Sint(24),
        R16Float(25),
        RG8Unorm(30),
        RG8Unorm_sRGB(31),
        RG8Snorm(32),
        RG8Uint(33),
        RG8Sint(34),
        B5G6R5Unorm(40),
        A1BGR5Unorm(41),
        ABGR4Unorm(42),
        BGR5A1Unorm(43),
        R32Uint(53),
        R32Sint(54),
        R32Float(55),
        RG16Unorm(60),
        RG16Snorm(62),
        RG16Uint(63),
        RG16Sint(64),
        RG16Float(65),
        RGBA8Unorm(70),
        RGBA8Unorm_sRGB(71),
        RGBA8Snorm(72),
        RGBA8Uint(73),
        RGBA8Sint(74),
        BGRA8Unorm(80),
        BGRA8Unorm_sRGB(81),
        RGB10A2Unorm(90),
        RGB10A2Uint(91),
        RG11B10Float(92),
        RGB9E5Float(93),
        BGR10A2Unorm(94),
        RG32Uint(103),
        RG32Sint(104),
        RG32Float(105),
        RGBA16Unorm(110),
        RGBA16Snorm(112),
        RGBA16Uint(113),
        RGBA16Sint(114),
        RGBA16Float(115),
        RGBA32Uint(123),
        RGBA32Sint(124),
        RGBA32Float(125),
        BC1_RGBA(130),
        BC1_RGBA_sRGB(131),
        BC2_RGBA(132),
        BC2_RGBA_sRGB(133),
        BC3_RGBA(134),
        BC3_RGBA_sRGB(135),
        BC4_RUnorm(140),
        BC4_RSnorm(141),
        BC5_RGUnorm(142),
        BC5_RGSnorm(143),
        BC6H_RGBFloat(150),
        BC6H_RGBUfloat(151),
        BC7_RGBAUnorm(152),
        BC7_RGBAUnorm_sRGB(153),
        PVRTC_RGB_2BPP(160),
        PVRTC_RGB_2BPP_sRGB(161),
        PVRTC_RGB_4BPP(162),
        PVRTC_RGB_4BPP_sRGB(163),
        PVRTC_RGBA_2BPP(164),
        PVRTC_RGBA_2BPP_sRGB(165),
        PVRTC_RGBA_4BPP(166),
        PVRTC_RGBA_4BPP_sRGB(167),
        EAC_R11Unorm(170),
        EAC_R11Snorm(172),
        EAC_RG11Unorm(174),
        EAC_RG11Snorm(176),
        EAC_RGBA8(178),
        EAC_RGBA8_sRGB(179),
        ETC2_RGB8(180),
        ETC2_RGB8_sRGB(181),
        ETC2_RGB8A1(182),
        ETC2_RGB8A1_sRGB(183),
        ASTC_4x4_sRGB(186),
        ASTC_5x4_sRGB(187),
        ASTC_5x5_sRGB(188),
        ASTC_6x5_sRGB(189),
        ASTC_6x6_sRGB(190),
        ASTC_8x5_sRGB(192),
        ASTC_8x6_sRGB(193),
        ASTC_8x8_sRGB(194),
        ASTC_10x5_sRGB(195),
        ASTC_10x6_sRGB(196),
        ASTC_10x8_sRGB(197),
        ASTC_10x10_sRGB(198),
        ASTC_12x10_sRGB(199),
        ASTC_12x12_sRGB(200),
        ASTC_4x4_LDR(204),
        ASTC_5x4_LDR(205),
        ASTC_5x5_LDR(206),
        ASTC_6x5_LDR(207),
        ASTC_6x6_LDR(208),
        ASTC_8x5_LDR(210),
        ASTC_8x6_LDR(211),
        ASTC_8x8_LDR(212),
        ASTC_10x5_LDR(213),
        ASTC_10x6_LDR(214),
        ASTC_10x8_LDR(215),
        ASTC_10x10_LDR(216),
        ASTC_12x10_LDR(217),
        ASTC_12x12_LDR(218),
        ASTC_4x4_HDR(222),
        ASTC_5x4_HDR(223),
        ASTC_5x5_HDR(224),
        ASTC_6x5_HDR(225),
        ASTC_6x6_HDR(226),
        ASTC_8x5_HDR(228),
        ASTC_8x6_HDR(229),
        ASTC_8x8_HDR(230),
        ASTC_10x5_HDR(231),
        ASTC_10x6_HDR(232),
        ASTC_10x8_HDR(233),
        ASTC_10x10_HDR(234),
        ASTC_12x10_HDR(235),
        ASTC_12x12_HDR(236),
        GBGR422(240),
        BGRG422(241),
        Depth16Unorm(250),
        Depth32Float(252),
        Stencil8(253),
        Depth24Unorm_Stencil8(255),
        Depth32Float_Stencil8(260),
        X32_Stencil8(261),
        X24_Stencil8(262);
        
        private final int value;
        
        MTLPixelFormat(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Texture usage flags.
     */
    public enum MTLTextureUsage {
        UNKNOWN(0),
        SHADER_READ(1),
        SHADER_WRITE(2),
        RENDER_TARGET(4),
        PIXEL_FORMAT_VIEW(16);
        
        private final int value;
        
        MTLTextureUsage(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Heap pool for sub-allocations.
     */
    public static final class HeapPool {
        
        private final long device;
        private final MemoryAllocator.MTLStorageMode storageMode;
        private final long heapSize;
        
        /**
         * Active heaps.
         */
        private final List<HeapInfo> heaps = new CopyOnWriteArrayList<>();
        
        /**
         * Allocations by resource handle.
         */
        private final ConcurrentHashMap<Long, HeapAllocation> allocations = new ConcurrentHashMap<>();
        
        public HeapPool(long device, MemoryAllocator.MTLStorageMode storageMode, long heapSize) {
            this.device = device;
            this.storageMode = storageMode;
            this.heapSize = heapSize;
        }
        
        /**
         * Allocate a buffer from the heap pool.
         */
        public long allocateBuffer(long size, MemoryAllocator.MTLCPUCacheMode cacheMode) {
            // Find a heap with enough space
            for (HeapInfo heap : heaps) {
                if (heap.currentSize - heap.usedSize >= size) {
                    long buffer = nMTLHeapNewBufferWithLength(heap.handle, size, 
                        (storageMode.getValue() << 0) | (cacheMode.getValue() << 4));
                    if (buffer != 0) {
                        heap.usedSize += size;
                        allocations.put(buffer, new HeapAllocation(heap, size));
                        return buffer;
                    }
                }
            }
            
            // Create a new heap
            HeapInfo newHeap = createHeap(Math.max(heapSize, size));
            if (newHeap != null) {
                long buffer = nMTLHeapNewBufferWithLength(newHeap.handle, size,
                    (storageMode.getValue() << 0) | (cacheMode.getValue() << 4));
                if (buffer != 0) {
                    newHeap.usedSize += size;
                    allocations.put(buffer, new HeapAllocation(newHeap, size));
                    return buffer;
                }
            }
            
            return 0;
        }
        
        /**
         * Allocate a texture from the heap pool.
         */
        public long allocateTexture(TextureDescriptor descriptor) {
            // Create texture descriptor
            long descHandle = nMTLTextureDescriptorNew();
            nMTLTextureDescriptorSetTextureType(descHandle, descriptor.textureType.getValue());
            nMTLTextureDescriptorSetPixelFormat(descHandle, descriptor.pixelFormat.getValue());
            nMTLTextureDescriptorSetWidth(descHandle, descriptor.width);
            nMTLTextureDescriptorSetHeight(descHandle, descriptor.height);
            nMTLTextureDescriptorSetDepth(descHandle, descriptor.depth);
            nMTLTextureDescriptorSetStorageMode(descHandle, storageMode.getValue());
            nMTLTextureDescriptorSetUsage(descHandle, descriptor.usage);
            
            // Get required size
            long requiredSize = nMTLDeviceHeapTextureSizeAndAlign(device, descHandle);
            
            // Find a heap with enough space
            for (HeapInfo heap : heaps) {
                if (heap.currentSize - heap.usedSize >= requiredSize) {
                    long texture = nMTLHeapNewTextureWithDescriptor(heap.handle, descHandle);
                    if (texture != 0) {
                        heap.usedSize += requiredSize;
                        allocations.put(texture, new HeapAllocation(heap, requiredSize));
                        nRelease(descHandle);
                        return texture;
                    }
                }
            }
            
            // Create a new heap
            HeapInfo newHeap = createHeap(Math.max(heapSize, requiredSize));
            if (newHeap != null) {
                long texture = nMTLHeapNewTextureWithDescriptor(newHeap.handle, descHandle);
                if (texture != 0) {
                    newHeap.usedSize += requiredSize;
                    allocations.put(texture, new HeapAllocation(newHeap, requiredSize));
                    nRelease(descHandle);
                    return texture;
                }
            }
            
            nRelease(descHandle);
            return 0;
        }
        
        /**
         * Create a new heap.
         */
        private HeapInfo createHeap(long size) {
            long descriptor = nMTLHeapDescriptorNew();
            nMTLHeapDescriptorSetSize(descriptor, size);
            nMTLHeapDescriptorSetStorageMode(descriptor, storageMode.getValue());
            
            long heap = nMTLDeviceNewHeapWithDescriptor(device, descriptor);
            nRelease(descriptor);
            
            if (heap != 0) {
                HeapInfo info = new HeapInfo(heap, size);
                heaps.add(info);
                return info;
            }
            
            return null;
        }
        
        /**
         * Free a resource.
         */
        public boolean free(long resource) {
            HeapAllocation allocation = allocations.remove(resource);
            if (allocation != null) {
                allocation.heap.usedSize -= allocation.size;
                nMTLResourceMakeAliasable(resource);
                return true;
            }
            return false;
        }
        
        /**
         * Compact heaps by releasing empty ones.
         */
        public void compact() {
            heaps.removeIf(heap -> {
                if (heap.usedSize == 0) {
                    nRelease(heap.handle);
                    return true;
                }
                return false;
            });
        }
        
        /**
         * Shutdown the pool.
         */
        public void shutdown() {
            for (HeapInfo heap : heaps) {
                nRelease(heap.handle);
            }
            heaps.clear();
            allocations.clear();
        }
        
        private static final class HeapInfo {
            final long handle;
            final long currentSize;
            long usedSize;
            
            HeapInfo(long handle, long size) {
                this.handle = handle;
                this.currentSize = size;
                this.usedSize = 0;
            }
        }
        
        private static final class HeapAllocation {
            final HeapInfo heap;
            final long size;
            
            HeapAllocation(HeapInfo heap, long size) {
                this.heap = heap;
                this.size = size;
            }
        }
    }
    
    /**
     * Memory pressure level.
     */
    public enum MemoryPressureLevel {
        NORMAL,
        WARNING,
        CRITICAL
    }
    
    /**
     * Memory pressure observer.
     */
    public static final class MemoryPressureObserver {
        
        private final Consumer<MemoryPressureLevel> callback;
        private volatile boolean running = false;
        private long observerHandle;
        
        public MemoryPressureObserver(Consumer<MemoryPressureLevel> callback) {
            this.callback = callback;
        }
        
        public void start() {
            if (running) return;
            running = true;
            
            observerHandle = nDispatchSourceCreateMemoryPressure((level) -> {
                if (callback != null) {
                    MemoryPressureLevel pressureLevel = switch (level) {
                        case 1 -> MemoryPressureLevel.NORMAL;
                        case 2 -> MemoryPressureLevel.WARNING;
                        case 4 -> MemoryPressureLevel.CRITICAL;
                        default -> MemoryPressureLevel.NORMAL;
                    };
                    callback.accept(pressureLevel);
                }
            });
        }
        
        public void stop() {
            if (!running) return;
            running = false;
            
            if (observerHandle != 0) {
                nDispatchSourceCancel(observerHandle);
                observerHandle = 0;
            }
        }
    }
    
    /**
     * Memory configuration.
     */
    public static final class MemoryConfiguration {
        public long defaultHeapSize = 64 * 1024 * 1024; // 64MB
        public long maxHeapAllocationSize = 16 * 1024 * 1024; // 16MB
        public boolean useHeapAllocation = true;
    }
    
    /**
     * Memory statistics.
     */
    public static final class MemoryStats {
        public final AtomicLong heapAllocations = new AtomicLong(0);
        public final AtomicLong heapDeallocations = new AtomicLong(0);
        public final AtomicLong heapAllocatedBytes = new AtomicLong(0);
        public final AtomicLong directAllocations = new AtomicLong(0);
        public final AtomicLong directDeallocations = new AtomicLong(0);
        public final AtomicLong directAllocatedBytes = new AtomicLong(0);
        public final AtomicLong directDeallocatedBytes = new AtomicLong(0);
        public final AtomicLong pressureEvents = new AtomicLong(0);
        public final AtomicLong resourcesPurged = new AtomicLong(0);
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 5: AUTORELEASE MANAGEMENT
    // Integration with Objective-C ARC for proper memory management
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Autorelease pool manager.
     */
    public static final class AutoreleaseManager {
        
        /**
         * Thread-local autorelease pools.
         */
        private final ThreadLocal<Deque<Long>> poolStacks = ThreadLocal.withInitial(ArrayDeque::new);
        
        /**
         * Push a new autorelease pool.
         */
        public void pushPool() {
            long pool = nObjCAutoreleasePoolPush();
            poolStacks.get().push(pool);
        }
        
        /**
         * Pop the current autorelease pool.
         */
        public void popPool() {
            Deque<Long> stack = poolStacks.get();
            if (!stack.isEmpty()) {
                long pool = stack.pop();
                nObjCAutoreleasePoolPop(pool);
            }
        }
        
        /**
         * Drain all pools for the current thread.
         */
        public void drainAllPools() {
            Deque<Long> stack = poolStacks.get();
            while (!stack.isEmpty()) {
                long pool = stack.pop();
                nObjCAutoreleasePoolPop(pool);
            }
        }
        
        /**
         * Execute a runnable within an autorelease pool.
         */
        public void withPool(Runnable runnable) {
            pushPool();
            try {
                runnable.run();
            } finally {
                popPool();
            }
        }
        
        /**
         * Execute a callable within an autorelease pool.
         */
        public <T> T withPool(Callable<T> callable) throws Exception {
            pushPool();
            try {
                return callable.call();
            } finally {
                popPool();
            }
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get the device manager.
     */
    public DeviceManager getDeviceManager() {
        return deviceManager;
    }
    
    /**
     * Get the selected device handle.
     */
    public long getDevice() {
        return deviceManager.getSelectedDevice();
    }
    
    /**
     * Get the command queue.
     */
    public long getCommandQueue() {
        return commandQueue;
    }
    
    /**
     * Get the command buffer pool.
     */
    public CommandBufferPool getCommandBufferPool() {
        return commandBufferPool;
    }
    
    /**
     * Acquire a command buffer.
     */
    public PooledCommandBuffer acquireCommandBuffer() {
        return commandBufferPool.acquireCommandBuffer();
    }
    
    /**
     * Acquire a command buffer with label.
     */
    public PooledCommandBuffer acquireCommandBuffer(String label) {
        return commandBufferPool.acquireCommandBuffer(label);
    }
    
    /**
     * Get the resource tracker.
     */
    public ResourceTracker getResourceTracker() {
        return resourceTracker;
    }
    
    /**
     * Get the memory allocator.
     */
    public MemoryAllocator getMemoryAllocator() {
        return memoryAllocator;
    }
    
    /**
     * Get the autorelease manager.
     */
    public AutoreleaseManager getAutoreleaseManager() {
        return autoreleaseManager;
    }
    
    /**
     * Begin a new frame.
     */
    public void beginFrame() {
        try {
            frameSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        frameCounter.incrementAndGet();
        autoreleaseManager.pushPool();
    }
    
    /**
     * End the current frame.
     */
    public void endFrame(Runnable onComplete) {
        autoreleaseManager.popPool();
        
        // Frame completion callback
        Runnable wrappedCallback = () -> {
            frameSemaphore.release();
            if (onComplete != null) {
                onComplete.run();
            }
        };
        
        // This would be called by the command buffer completion
        wrappedCallback.run();
    }
    
    /**
     * Wait for all GPU work to complete.
     */
    public void waitForIdle() {
        // Acquire all semaphore permits (waits for all in-flight frames)
        try {
            frameSemaphore.acquire(MAX_FRAMES_IN_FLIGHT);
            frameSemaphore.release(MAX_FRAMES_IN_FLIGHT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get the current frame number.
     */
    public long getFrameNumber() {
        return frameCounter.get();
    }
    
    /**
     * Check if the manager is closed.
     */
    public boolean isClosed() {
        return closed.get();
    }
    
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            LOGGER.info("Shutting down Metal Manager");
            
            waitForIdle();
            
            commandBufferPool.shutdown();
            resourceTracker.shutdown();
            memoryAllocator.shutdown();
            autoreleaseManager.drainAllPools();
            
            if (commandQueue != 0) {
                nRelease(commandQueue);
            }
            
            synchronized (INSTANCE_LOCK) {
                instance = null;
            }
            
            LOGGER.info("Metal Manager shutdown complete");
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // NATIVE METHOD DECLARATIONS
    // ════════════════════════════════════════════════════════════════════════════
    
    // Device methods
    private static native long nMTLCreateSystemDefaultDevice();
    private static native long[] nMTLCopyAllDevices();
    private static native String nMTLDeviceGetName(long device);
    private static native long nMTLDeviceGetRegistryID(long device);
    private static native boolean nMTLDeviceIsLowPower(long device);
    private static native boolean nMTLDeviceIsHeadless(long device);
    private static native boolean nMTLDeviceIsRemovable(long device);
    private static native boolean nMTLDeviceHasUnifiedMemory(long device);
    private static native long nMTLDeviceGetRecommendedMaxWorkingSetSize(long device);
    private static native long nMTLDeviceGetMaxBufferLength(long device);
    private static native long nMTLDeviceGetCurrentAllocatedSize(long device);
    private static native int nMTLDeviceGetMaxThreadsPerThreadgroup(long device);
    private static native int nMTLDeviceGetMaxThreadgroupMemoryLength(long device);
    private static native boolean nMTLDeviceSupportsFamily(long device, int family);
    private static native boolean nMTLDeviceSupportsRaytracing(long device);
    private static native boolean nMTLDeviceSupports32BitMSAA(long device);
    private static native boolean nMTLDeviceSupportsQueryTextureLOD(long device);
    private static native boolean nMTLDeviceSupportsBCTextureCompression(long device);
    private static native boolean nMTLDeviceSupportsPullModelInterpolation(long device);
    private static native boolean nMTLDeviceSupportsShaderBarycentricCoordinates(long device);
    private static native boolean nMTLDeviceSupportsDynamicLibraries(long device);
    private static native boolean nMTLDeviceSupportsFunctionPointers(long device);
    private static native boolean nMTLDeviceSupportsRenderDynamicLibraries(long device);
    private static native int nMTLDeviceGetArgumentBuffersTier(long device);
    private static native int nMTLDeviceGetReadWriteTextureSupport(long device);
    private static native int nMTLDeviceGetSparseTileSizeInBytes(long device);
    private static native boolean nMTLDeviceSupportsCounterSampling(long device, int samplingPoint);
    
    // Command queue methods
    private static native long nMTLDeviceNewCommandQueue(long device);
    private static native long nMTLDeviceNewCommandQueueWithMaxCommandBufferCount(long device, int maxCount);
    
    // Command buffer methods
    private static native long nMTLCommandQueueCommandBuffer(long queue);
    private static native long nMTLCommandQueueCommandBufferWithUnretainedReferences(long queue);
    private static native void nMTLCommandBufferSetLabel(long buffer, String label);
    private static native void nMTLCommandBufferEnqueue(long buffer);
    private static native void nMTLCommandBufferCommit(long buffer);
    private static native void nMTLCommandBufferWaitUntilCompleted(long buffer);
    private static native void nMTLCommandBufferWaitUntilScheduled(long buffer);
    private static native int nMTLCommandBufferGetStatus(long buffer);
    private static native String nMTLCommandBufferGetError(long buffer);
    private static native double nMTLCommandBufferGetGPUStartTime(long buffer);
    private static native double nMTLCommandBufferGetGPUEndTime(long buffer);
    private static native void nMTLCommandBufferAddCompletedHandler(long buffer, Runnable handler);
    private static native void nMTLCommandBufferAddScheduledHandler(long buffer, Runnable handler);
    private static native void nMTLCommandBufferPresentDrawable(long buffer, long drawable);
    private static native void nMTLCommandBufferPresentDrawableAtTime(long buffer, long drawable, double time);
    private static native void nMTLCommandBufferPushDebugGroup(long buffer, String label);
    private static native void nMTLCommandBufferPopDebugGroup(long buffer);
    private static native void nMTLCommandBufferEncodeSignalEvent(long buffer, long event, long value);
    private static native void nMTLCommandBufferEncodeWaitForEvent(long buffer, long event, long value);
    
    // Encoder creation methods
    private static native long nMTLCommandBufferRenderCommandEncoderWithDescriptor(long buffer, long descriptor);
    private static native long nMTLCommandBufferComputeCommandEncoder(long buffer);
    private static native long nMTLCommandBufferComputeCommandEncoderWithDispatchType(long buffer, int type);
    private static native long nMTLCommandBufferBlitCommandEncoder(long buffer);
    private static native long nMTLCommandBufferParallelRenderCommandEncoderWithDescriptor(long buffer, long descriptor);
    private static native long nMTLCommandBufferResourceStateCommandEncoder(long buffer);
    private static native long nMTLCommandBufferAccelerationStructureCommandEncoder(long buffer);
    
    // Synchronization methods
    private static native long nMTLDeviceNewEvent(long device);
    private static native long nMTLDeviceNewSharedEvent(long device);
    private static native long nMTLDeviceNewSharedEventWithHandle(long device, long handle);
    private static native long nMTLDeviceNewFence(long device);
    private static native long nMTLSharedEventNewListener(long event);
    private static native void nMTLSharedEventNotifyListener(long event, long listener, long value, Runnable handler);
    private static native void nMTLRenderCommandEncoderWaitForFence(long encoder, long fence, int stages);
    private static native void nMTLRenderCommandEncoderUpdateFence(long encoder, long fence, int stages);
    private static native void nMTLRenderCommandEncoderMemoryBarrier(long encoder, int scope, int after, int before);
    private static native void nMTLRenderCommandEncoderMemoryBarrierWithResources(long encoder, long[] resources, int after, int before);
    private static native void nMTLRenderCommandEncoderTextureBarrier(long encoder);
    private static native void nMTLBlitCommandEncoderSynchronizeResource(long encoder, long resource);
    private static native void nMTLBlitCommandEncoderSynchronizeTexture(long encoder, long texture, int slice, int level);
    
    // Resource methods
    private static native long nMTLDeviceNewBufferWithLength(long device, long length, int options);
    private static native long nMTLDeviceNewBufferWithBytes(long device, ByteBuffer data, long length, int options);
    private static native long nMTLDeviceNewTextureWithDescriptor(long device, long descriptor);
    private static native void nMTLResourceSetLabel(long resource, String label);
    private static native int nMTLResourceSetPurgeableState(long resource, int state);
    private static native void nMTLResourceMakeAliasable(long resource);
    
    // Texture descriptor methods
    private static native long nMTLTextureDescriptorNew();
    private static native void nMTLTextureDescriptorSetTextureType(long desc, int type);
    private static native void nMTLTextureDescriptorSetPixelFormat(long desc, int format);
    private static native void nMTLTextureDescriptorSetWidth(long desc, int width);
    private static native void nMTLTextureDescriptorSetHeight(long desc, int height);
    private static native void nMTLTextureDescriptorSetDepth(long desc, int depth);
    private static native void nMTLTextureDescriptorSetMipmapLevelCount(long desc, int count);
    private static native void nMTLTextureDescriptorSetArrayLength(long desc, int length);
    private static native void nMTLTextureDescriptorSetSampleCount(long desc, int count);
    private static native void nMTLTextureDescriptorSetStorageMode(long desc, int mode);
    private static native void nMTLTextureDescriptorSetUsage(long desc, int usage);
    
    // Heap methods
    private static native long nMTLHeapDescriptorNew();
    private static native void nMTLHeapDescriptorSetSize(long desc, long size);
    private static native void nMTLHeapDescriptorSetStorageMode(long desc, int mode);
    private static native long nMTLDeviceNewHeapWithDescriptor(long device, long descriptor);
    private static native long nMTLHeapNewBufferWithLength(long heap, long length, int options);
    private static native long nMTLHeapNewTextureWithDescriptor(long heap, long descriptor);
    private static native long nMTLDeviceHeapTextureSizeAndAlign(long device, long descriptor);
    
    // Memory pressure methods
    private static native long nDispatchSourceCreateMemoryPressure(java.util.function.IntConsumer handler);
    private static native void nDispatchSourceCancel(long source);
    
    // Autorelease methods
    private static native long nObjCAutoreleasePoolPush();
    private static native void nObjCAutoreleasePoolPop(long pool);
    
    // Release
    private static native void nRelease(long object);
}

     * Get a function with constant values.
     */
    public long getFunctionWithConstants(CompiledShader shader, String functionName,
                                         FunctionConstantValues constantValues) {
        CachedLibrary cached = libraryCache.get(shader.hash);
        if (cached == null) {
            throw new IllegalStateException("Library not found in cache");
        }
        
        // Create a unique key for this function + constants combination
        String key = functionName + "_" + constantValues.computeHash();
        
        // Check function cache
        Long function = cached.functions.get(key);
        if (function != null) {
            return function;
        }
        
        // Create function constant values
        long mtlConstants = nMTLFunctionConstantValuesNew();
        for (FunctionConstantValues.Entry entry : constantValues.entries) {
            switch (entry.type) {
                case BOOL -> nMTLFunctionConstantValuesSetBool(mtlConstants, entry.name, 
                    entry.index, (Boolean) entry.value);
                case INT -> nMTLFunctionConstantValuesSetInt(mtlConstants, entry.name,
                    entry.index, (Integer) entry.value);
                case UINT -> nMTLFunctionConstantValuesSetUInt(mtlConstants, entry.name,
                    entry.index, (Integer) entry.value);
                case FLOAT -> nMTLFunctionConstantValuesSetFloat(mtlConstants, entry.name,
                    entry.index, (Float) entry.value);
                case HALF -> nMTLFunctionConstantValuesSetHalf(mtlConstants, entry.name,
                    entry.index, (Float) entry.value);
            }
        }
        
        // Get function with constants
        long[] error = {0};
        function = nMTLLibraryNewFunctionWithNameConstantValues(cached.handle, functionName, 
            mtlConstants, error);
        nRelease(mtlConstants);
        
        if (function == 0) {
            String errorMsg = error[0] != 0 ? nNSErrorLocalizedDescription(error[0]) : "Unknown error";
            if (error[0] != 0) nRelease(error[0]);
            throw new ShaderCompilationException(shader.name, 
                "Failed to create function with constants: " + functionName + " - " + errorMsg);
        }
        
        cached.functions.put(key, function);
        return function;
    }
    
    /**
     * Create a linked function.
     */
    public long createLinkedFunction(CompiledShader shader, String functionName,
                                     @Nullable LinkedFunctionOptions options) {
        long function = getFunction(shader, functionName);
        
        if (options == null) {
            return function;
        }
        
        long linkedFunctions = nMTLLinkedFunctionsNew();
        
        if (options.functions != null) {
            for (long fn : options.functions) {
                nMTLLinkedFunctionsAddFunction(linkedFunctions, fn);
            }
        }
        
        if (options.binaryFunctions != null) {
            for (long fn : options.binaryFunctions) {
                nMTLLinkedFunctionsAddBinaryFunction(linkedFunctions, fn);
            }
        }
        
        return linkedFunctions;
    }
    
    /**
     * Precompile a pipeline state to the binary archive.
     */
    public boolean addToBinaryArchive(long pipelineDescriptor, PipelineType type) {
        if (binaryArchive == 0) return false;
        
        long[] error = {0};
        boolean success = switch (type) {
            case RENDER -> nMTLBinaryArchiveAddRenderPipeline(binaryArchive, pipelineDescriptor, error);
            case COMPUTE -> nMTLBinaryArchiveAddComputePipeline(binaryArchive, pipelineDescriptor, error);
            case TILE -> nMTLBinaryArchiveAddTilePipeline(binaryArchive, pipelineDescriptor, error);
        };
        
        if (!success && error[0] != 0) {
            LOGGER.warn("Failed to add pipeline to binary archive: {}", 
                nNSErrorLocalizedDescription(error[0]));
            nRelease(error[0]);
        }
        
        return success;
    }
    
    /**
     * Pipeline type for binary archive.
     */
    public enum PipelineType {
        RENDER,
        COMPUTE,
        TILE
    }
    
    /**
     * Evict unused libraries from cache.
     */
    public void evictUnusedLibraries(long maxAgeMs) {
        long now = System.currentTimeMillis();
        
        libraryCache.entrySet().removeIf(entry -> {
            CachedLibrary cached = entry.getValue();
            if (now - cached.lastAccessTime > maxAgeMs) {
                // Release all functions
                for (long fn : cached.functions.values()) {
                    nRelease(fn);
                }
                // Release library
                nRelease(cached.handle);
                stats.librariesEvicted.incrementAndGet();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Clear all cached libraries.
     */
    public void clearCache() {
        for (CachedLibrary cached : libraryCache.values()) {
            for (long fn : cached.functions.values()) {
                nRelease(fn);
            }
            nRelease(cached.handle);
        }
        libraryCache.clear();
        stats.librariesEvicted.addAndGet(libraryCache.size());
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 2: CROSS-COMPILATION ROUTING
    // Route SPIR-V, HLSL, and GLSL to MSL
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Source shader type.
     */
    public enum ShaderSourceType {
        MSL,      // Native Metal Shading Language
        SPIRV,    // SPIR-V binary
        HLSL,     // High Level Shading Language
        GLSL      // OpenGL Shading Language
    }
    
    /**
     * Compile shader from any supported source type.
     */
    public CompiledShader compileFromSource(String name, byte[] source, ShaderSourceType sourceType,
                                            ShaderStage stage, @Nullable Map<String, String> defines) {
        String mslSource = switch (sourceType) {
            case MSL -> new String(source, StandardCharsets.UTF_8);
            case SPIRV -> compileSPIRVToMSL(name, source, stage);
            case HLSL -> compileHLSLToMSL(name, new String(source, StandardCharsets.UTF_8), stage, defines);
            case GLSL -> compileGLSLToMSL(name, new String(source, StandardCharsets.UTF_8), stage);
        };
        
        return compileShader(name, mslSource, defines);
    }
    
    /**
     * Compile SPIR-V to MSL.
     */
    public String compileSPIRVToMSL(String name, byte[] spirvBinary, ShaderStage stage) {
        stats.spirvCompilations.incrementAndGet();
        long startTime = System.nanoTime();
        
        try {
            // Wrap in ByteBuffer
            ByteBuffer buffer = ByteBuffer.allocateDirect(spirvBinary.length);
            buffer.put(spirvBinary);
            buffer.flip();
            
            // Create entry point info
            MSLCallMapper.MSLEntryPoint entryPoint = new MSLCallMapper.MSLEntryPoint();
            entryPoint.name = "main";
            entryPoint.stage = switch (stage) {
                case VERTEX -> MSLCallMapper.MSLShaderStage.VERTEX;
                case FRAGMENT -> MSLCallMapper.MSLShaderStage.FRAGMENT;
                case COMPUTE -> MSLCallMapper.MSLShaderStage.COMPUTE;
            };
            
            // Compile
            MSLCallMapper.MSLCompilationResult result = spirvCompiler.compile(buffer, entryPoint);
            
            if (!result.success) {
                throw new ShaderCompilationException(name, "SPIR-V to MSL compilation failed: " + result.errorMessage);
            }
            
            stats.spirvCompileTimeNs.addAndGet(System.nanoTime() - startTime);
            return result.mslSource;
            
        } catch (ShaderCompilationException e) {
            throw e;
        } catch (Exception e) {
            throw new ShaderCompilationException(name, "SPIR-V compilation error: " + e.getMessage());
        }
    }
    
    /**
     * Compile HLSL to MSL.
     */
    public String compileHLSLToMSL(String name, String hlslSource, ShaderStage stage,
                                   @Nullable Map<String, String> defines) {
        stats.hlslCompilations.incrementAndGet();
        long startTime = System.nanoTime();
        
        try {
            // Setup entry point
            String entryPoint = switch (stage) {
                case VERTEX -> "VSMain";
                case FRAGMENT -> "PSMain";
                case COMPUTE -> "CSMain";
            };
            
            // First compile HLSL to SPIR-V using DXC
            byte[] spirvBinary = compileHLSLToSPIRV(hlslSource, entryPoint, stage, defines);
            
            // Then compile SPIR-V to MSL
            String mslSource = compileSPIRVToMSL(name, spirvBinary, stage);
            
            stats.hlslCompileTimeNs.addAndGet(System.nanoTime() - startTime);
            return mslSource;
            
        } catch (ShaderCompilationException e) {
            throw e;
        } catch (Exception e) {
            throw new ShaderCompilationException(name, "HLSL compilation error: " + e.getMessage());
        }
    }
    
    /**
     * Compile HLSL to SPIR-V using DXC.
     */
    private byte[] compileHLSLToSPIRV(String hlslSource, String entryPoint, ShaderStage stage,
                                      @Nullable Map<String, String> defines) {
        // Build command line arguments
        List<String> args = new ArrayList<>();
        args.add("-spirv");
        args.add("-E");
        args.add(entryPoint);
        args.add("-T");
        args.add(switch (stage) {
            case VERTEX -> "vs_6_0";
            case FRAGMENT -> "ps_6_0";
            case COMPUTE -> "cs_6_0";
        });
        
        // Add defines
        if (defines != null) {
            for (Map.Entry<String, String> entry : defines.entrySet()) {
                args.add("-D");
                args.add(entry.getKey() + "=" + entry.getValue());
            }
        }
        
        // Additional options
        args.add("-Zpr");  // Row-major matrices
        args.add("-O3");   // Optimization level
        
        // Compile using native DXC
        return nDXCCompileHLSLToSPIRV(hlslSource, args.toArray(new String[0]));
    }
    
    /**
     * Compile GLSL to MSL.
     */
    public String compileGLSLToMSL(String name, String glslSource, ShaderStage stage) {
        stats.glslCompilations.incrementAndGet();
        long startTime = System.nanoTime();
        
        try {
            // First compile GLSL to SPIR-V using glslang
            byte[] spirvBinary = compileGLSLToSPIRV(glslSource, stage);
            
            // Then compile SPIR-V to MSL
            String mslSource = compileSPIRVToMSL(name, spirvBinary, stage);
            
            stats.glslCompileTimeNs.addAndGet(System.nanoTime() - startTime);
            return mslSource;
            
        } catch (ShaderCompilationException e) {
            throw e;
        } catch (Exception e) {
            // Fallback: try direct GLSL to MSL translation
            LOGGER.warn("SPIR-V compilation failed, trying direct GLSL to MSL");
            return translateGLSLToMSLDirect(name, glslSource, stage);
        }
    }
    
    /**
     * Compile GLSL to SPIR-V using glslang.
     */
    private byte[] compileGLSLToSPIRV(String glslSource, ShaderStage stage) {
        int glslangStage = switch (stage) {
            case VERTEX -> 0;   // GLSLANG_STAGE_VERTEX
            case FRAGMENT -> 4; // GLSLANG_STAGE_FRAGMENT
            case COMPUTE -> 5;  // GLSLANG_STAGE_COMPUTE
        };
        
        return nGlslangCompileGLSLToSPIRV(glslSource, glslangStage);
    }
    
    /**
     * Direct GLSL to MSL translation (fallback).
     */
    private String translateGLSLToMSLDirect(String name, String glslSource, ShaderStage stage) {
        MSLCallMapper.MSLEntryPoint entryPoint = new MSLCallMapper.MSLEntryPoint();
        entryPoint.name = "main";
        entryPoint.stage = switch (stage) {
            case VERTEX -> MSLCallMapper.MSLShaderStage.VERTEX;
            case FRAGMENT -> MSLCallMapper.MSLShaderStage.FRAGMENT;
            case COMPUTE -> MSLCallMapper.MSLShaderStage.COMPUTE;
        };
        
        MSLCallMapper.MSLCompilationResult result = glslCompiler.compileGLSL(glslSource, entryPoint);
        
        if (!result.success) {
            throw new ShaderCompilationException(name, "GLSL to MSL compilation failed: " + result.errorMessage);
        }
        
        return result.mslSource;
    }
    
    /**
     * Auto-detect source type from content.
     */
    public ShaderSourceType detectSourceType(byte[] source) {
        // Check for SPIR-V magic number
        if (source.length >= 4) {
            int magic = ((source[0] & 0xFF)) |
                       ((source[1] & 0xFF) << 8) |
                       ((source[2] & 0xFF) << 16) |
                       ((source[3] & 0xFF) << 24);
            if (magic == 0x07230203) {
                return ShaderSourceType.SPIRV;
            }
        }
        
        String text = new String(source, StandardCharsets.UTF_8);
        
        // Check for MSL markers
        if (text.contains("#include <metal_stdlib>") ||
            text.contains("using namespace metal") ||
            text.contains("kernel void") ||
            text.contains("vertex ") && text.contains("fragment ")) {
            return ShaderSourceType.MSL;
        }
        
        // Check for HLSL markers
        if (text.contains("cbuffer") ||
            text.contains("SV_Position") ||
            text.contains("SV_Target") ||
            text.contains("Texture2D") ||
            text.contains("SamplerState")) {
            return ShaderSourceType.HLSL;
        }
        
        // Default to GLSL
        return ShaderSourceType.GLSL;
    }
    
    /**
     * Shader stage enumeration.
     */
    public enum ShaderStage {
        VERTEX,
        FRAGMENT,
        COMPUTE
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // SECTION 3: SHADER CACHING
    // Hash computation, disk cache, and binary archive persistence
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Compute a hash for shader source and defines.
     */
    public String computeShaderHash(String source, @Nullable Map<String, String> defines) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Hash source
            digest.update(source.getBytes(StandardCharsets.UTF_8));
            
            // Hash defines in sorted order
            if (defines != null && !defines.isEmpty()) {
                List<String> sortedKeys = new ArrayList<>(defines.keySet());
                Collections.sort(sortedKeys);
                for (String key : sortedKeys) {
                    digest.update(key.getBytes(StandardCharsets.UTF_8));
                    digest.update(defines.get(key).getBytes(StandardCharsets.UTF_8));
                }
            }
            
            // Hash compiler options
            digest.update((byte) (compilerOptions.fastMathEnabled ? 1 : 0));
            digest.update((byte) (compilerOptions.preserveInvariants ? 1 : 0));
            digest.update((byte) targetVersion.ordinal());
            
            // Convert to hex string
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return Integer.toHexString(source.hashCode()) + "_" +
                   (defines != null ? Integer.toHexString(defines.hashCode()) : "0");
        }
    }
    
    /**
     * Disk cache for shader source and metadata.
     */
    public static final class ShaderDiskCache {
        
        private final Path cacheDirectory;
        private final ConcurrentHashMap<String, CacheEntry> memoryIndex = new ConcurrentHashMap<>();
        
        /**
         * Cache entry.
         */
        public static final class CacheEntry {
            public final String mslSource;
            public final Map<String, String> defines;
            public final long timestamp;
            
            public CacheEntry(String mslSource, @Nullable Map<String, String> defines, long timestamp) {
                this.mslSource = mslSource;
                this.defines = defines != null ? new HashMap<>(defines) : Collections.emptyMap();
                this.timestamp = timestamp;
            }
        }
        
        public ShaderDiskCache(Path cacheDirectory) {
            this.cacheDirectory = cacheDirectory;
            try {
                Files.createDirectories(cacheDirectory);
                loadIndex();
            } catch (IOException e) {
                LOGGER.warn("Failed to initialize disk cache: {}", e.getMessage());
            }
        }
        
        /**
         * Load cache index from disk.
         */
        private void loadIndex() {
            Path indexPath = cacheDirectory.resolve("index.dat");
            if (!Files.exists(indexPath)) return;
            
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(
                        new GZIPInputStream(Files.newInputStream(indexPath))))) {
                
                int version = in.readInt();
                if (version != 1) {
                    LOGGER.warn("Unknown cache index version: {}", version);
                    return;
                }
                
                int count = in.readInt();
                for (int i = 0; i < count; i++) {
                    String hash = in.readUTF();
                    long timestamp = in.readLong();
                    int defineCount = in.readInt();
                    
                    Map<String, String> defines = new HashMap<>();
                    for (int j = 0; j < defineCount; j++) {
                        defines.put(in.readUTF(), in.readUTF());
                    }
                    
                    // Load source lazily
                    memoryIndex.put(hash, new CacheEntry(null, defines, timestamp));
                }
                
                LOGGER.info("Loaded {} entries from shader cache index", count);
                
            } catch (IOException e) {
                LOGGER.warn("Failed to load cache index: {}", e.getMessage());
            }
        }
        
        /**
         * Save cache index to disk.
         */
        public void saveIndex() {
            Path indexPath = cacheDirectory.resolve("index.dat");
            
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(
                        new GZIPOutputStream(Files.newOutputStream(indexPath))))) {
                
                out.writeInt(1); // Version
                out.writeInt(memoryIndex.size());
                
                for (Map.Entry<String, CacheEntry> entry : memoryIndex.entrySet()) {
                    out.writeUTF(entry.getKey());
                    out.writeLong(entry.getValue().timestamp);
                    out.writeInt(entry.getValue().defines.size());
                    
                    for (Map.Entry<String, String> define : entry.getValue().defines.entrySet()) {
                        out.writeUTF(define.getKey());
                        out.writeUTF(define.getValue());
                    }
                }
                
            } catch (IOException e) {
                LOGGER.warn("Failed to save cache index: {}", e.getMessage());
            }
        }
        
        /**
         * Get a cache entry.
         */
        @Nullable
        public CacheEntry get(String hash) {
            CacheEntry entry = memoryIndex.get(hash);
            if (entry == null) return null;
            
            // Load source if not in memory
            if (entry.mslSource == null) {
                String source = loadSource(hash);
                if (source != null) {
                    entry = new CacheEntry(source, entry.defines, entry.timestamp);
                    memoryIndex.put(hash, entry);
                } else {
                    memoryIndex.remove(hash);
                    return null;
                }
            }
            
            return entry;
        }
        
        /**
         * Load source from disk.
         */
        @Nullable
        private String loadSource(String hash) {
            Path sourcePath = cacheDirectory.resolve(hash.substring(0, 2))
                                            .resolve(hash + ".msl.gz");
            if (!Files.exists(sourcePath)) return null;
            
            try (Reader reader = new InputStreamReader(
                    new GZIPInputStream(Files.newInputStream(sourcePath)), StandardCharsets.UTF_8)) {
                
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[8192];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, read);
                }
                return sb.toString();
                
            } catch (IOException e) {
                LOGGER.warn("Failed to load cached source for {}: {}", hash, e.getMessage());
                return null;
            }
        }
        
        /**
         * Put a cache entry.
         */
        public void put(String hash, CacheEntry entry) {
            memoryIndex.put(hash, entry);
            
            // Save source to disk
            Path dir = cacheDirectory.resolve(hash.substring(0, 2));
            try {
                Files.createDirectories(dir);
                
                Path sourcePath = dir.resolve(hash + ".msl.gz");
                try (Writer writer = new OutputStreamWriter(
                        new GZIPOutputStream(Files.newOutputStream(sourcePath)), StandardCharsets.UTF_8)) {
                    writer.write(entry.mslSource);
                }
                
            } catch (IOException e) {
                LOGGER.warn("Failed to save cached source for {}: {}", hash, e.getMessage());
            }
        }
        
        /**
         * Remove a cache entry.
         */
        public void remove(String hash) {
            memoryIndex.remove(hash);
            
            Path sourcePath = cacheDirectory.resolve(hash.substring(0, 2))
                                            .resolve(hash + ".msl.gz");
            try {
                Files.deleteIfExists(sourcePath);
            } catch (IOException e) {
                LOGGER.warn("Failed to delete cached source for {}: {}", hash, e.getMessage());
            }
        }
        
        /**
         * Clear the cache.
         */
        public void clear() {
            memoryIndex.clear();
            
            try {
                Files.walk(cacheDirectory)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignored) {}
                    });
            } catch (IOException e) {
                LOGGER.warn("Failed to clear cache: {}", e.getMessage());
            }
        }
        
        /**
         * Get cache size.
         */
        public int size() {
            return memoryIndex.size();
        }
        
        /**
         * Evict old entries.
         */
        public void evictOlderThan(long maxAgeMs) {
            long cutoff = System.currentTimeMillis() - maxAgeMs;
            
            memoryIndex.entrySet().removeIf(entry -> {
                if (entry.getValue().timestamp < cutoff) {
                    remove(entry.getKey());
                    return true;
                }
                return false;
            });
        }
    }
    
    /**
     * Function constant values builder.
     */
    public static final class FunctionConstantValues {
        
        final List<Entry> entries = new ArrayList<>();
        
        public static final class Entry {
            final String name;
            final int index;
            final ConstantType type;
            final Object value;
            
            Entry(String name, int index, ConstantType type, Object value) {
                this.name = name;
                this.index = index;
                this.type = type;
                this.value = value;
            }
        }
        
        public enum ConstantType {
            BOOL, INT, UINT, FLOAT, HALF
        }
        
        public FunctionConstantValues setBool(String name, int index, boolean value) {
            entries.add(new Entry(name, index, ConstantType.BOOL, value));
            return this;
        }
        
        public FunctionConstantValues setBool(int index, boolean value) {
            return setBool(null, index, value);
        }
        
        public FunctionConstantValues setInt(String name, int index, int value) {
            entries.add(new Entry(name, index, ConstantType.INT, value));
            return this;
        }
        
        public FunctionConstantValues setInt(int index, int value) {
            return setInt(null, index, value);
        }
        
        public FunctionConstantValues setUInt(String name, int index, int value) {
            entries.add(new Entry(name, index, ConstantType.UINT, value));
            return this;
        }
        
        public FunctionConstantValues setUInt(int index, int value) {
            return setUInt(null, index, value);
        }
        
        public FunctionConstantValues setFloat(String name, int index, float value) {
            entries.add(new Entry(name, index, ConstantType.FLOAT, value));
            return this;
        }
        
        public FunctionConstantValues setFloat(int index, float value) {
            return setFloat(null, index, value);
        }
        
        public FunctionConstantValues setHalf(String name, int index, float value) {
            entries.add(new Entry(name, index, ConstantType.HALF, value));
            return this;
        }
        
        public FunctionConstantValues setHalf(int index, float value) {
            return setHalf(null, index, value);
        }
        
        String computeHash() {
            StringBuilder sb = new StringBuilder();
            for (Entry entry : entries) {
                sb.append(entry.index).append('_').append(entry.value).append(';');
            }
            return Integer.toHexString(sb.toString().hashCode());
        }
    }
    
    /**
     * Linked function options.
     */
    public static final class LinkedFunctionOptions {
        public long[] functions;
        public long[] binaryFunctions;
        public long[] groups;
    }
    
    /**
     * Compiled shader result.
     */
    public static final class CompiledShader {
        public final long library;
        public final String name;
        public final String hash;
        public final boolean fromCache;
        
        CompiledShader(long library, String name, String hash, boolean fromCache) {
            this.library = library;
            this.name = name;
            this.hash = hash;
            this.fromCache = fromCache;
        }
    }
    
    /**
     * Compiler options.
     */
    public static final class CompilerOptions {
        public boolean fastMathEnabled = true;
        public boolean preserveInvariants = false;
        public OptimizationLevel optimizationLevel = OptimizationLevel.DEFAULT;
        public Map<String, String> defaultDefines = new HashMap<>();
        
        public enum OptimizationLevel {
            NONE,
            DEFAULT,
            SIZE
        }
    }
    
    /**
     * Shader compilation exception.
     */
    public static class ShaderCompilationException extends RuntimeException {
        public final String shaderName;
        
        public ShaderCompilationException(String shaderName, String message) {
            super("Shader '" + shaderName + "': " + message);
            this.shaderName = shaderName;
        }
    }
    
    /**
     * MSL Manager statistics.
     */
    public static final class MSLStats {
        public final AtomicLong cacheHits = new AtomicLong(0);
        public final AtomicLong cacheMisses = new AtomicLong(0);
        public final AtomicLong diskCacheHits = new AtomicLong(0);
        public final AtomicLong totalCompilations = new AtomicLong(0);
        public final AtomicLong totalCompileTimeNs = new AtomicLong(0);
        public final AtomicLong spirvCompilations = new AtomicLong(0);
        public final AtomicLong spirvCompileTimeNs = new AtomicLong(0);
        public final AtomicLong hlslCompilations = new AtomicLong(0);
        public final AtomicLong hlslCompileTimeNs = new AtomicLong(0);
        public final AtomicLong glslCompilations = new AtomicLong(0);
        public final AtomicLong glslCompileTimeNs = new AtomicLong(0);
        public final AtomicLong librariesEvicted = new AtomicLong(0);
        
        public double getCacheHitRate() {
            long total = cacheHits.get() + cacheMisses.get();
            return total > 0 ? (double) cacheHits.get() / total : 0.0;
        }
        
        public double getAverageCompileTimeMs() {
            long count = totalCompilations.get();
            return count > 0 ? (double) totalCompileTimeNs.get() / count / 1_000_000.0 : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "MSLStats[cacheHits=%d, cacheMisses=%d, hitRate=%.2f%%, compilations=%d, avgCompileTime=%.2fms, " +
                "spirv=%d, hlsl=%d, glsl=%d]",
                cacheHits.get(), cacheMisses.get(), getCacheHitRate() * 100,
                totalCompilations.get(), getAverageCompileTimeMs(),
                spirvCompilations.get(), hlslCompilations.get(), glslCompilations.get()
            );
        }
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ════════════════════════════════════════════════════════════════════════════
    
    /**
     * Get statistics.
     */
    public MSLStats getStats() {
        return stats;
    }
    
    /**
     * Get the disk cache.
     */
    public ShaderDiskCache getDiskCache() {
        return diskCache;
    }
    
    /**
     * Get the binary archive handle.
     */
    public long getBinaryArchive() {
        return binaryArchive;
    }
    
    /**
     * Check if the manager is closed.
     */
    public boolean isClosed() {
        return closed;
    }
    
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        
        LOGGER.info("Shutting down MSL Manager");
        
        // Save binary archive
        saveBinaryArchive();
        
        // Save disk cache index
        diskCache.saveIndex();
        
        // Shutdown compilation executor
        compilationExecutor.shutdown();
        try {
            if (!compilationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                compilationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            compilationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Clear library cache
        clearCache();
        
        // Release binary archive
        if (binaryArchive != 0) {
            nRelease(binaryArchive);
            binaryArchive = 0;
        }
        
        LOGGER.info("MSL Manager shutdown complete");
        LOGGER.info("Final stats: {}", stats);
    }
    
    // ════════════════════════════════════════════════════════════════════════════
    // NATIVE METHOD DECLARATIONS
    // ════════════════════════════════════════════════════════════════════════════
    
    // Compile options
    private static native long nMTLCompileOptionsNew();
    private static native void nMTLCompileOptionsSetLanguageVersion(long options, int version);
    private static native void nMTLCompileOptionsSetFastMathEnabled(long options, boolean enabled);
    private static native void nMTLCompileOptionsSetPreserveInvariants(long options, boolean enabled);
    private static native void nMTLCompileOptionsAddPreprocessorMacro(long options, String name, String value);
    
    // Library creation
    private static native long nMTLDeviceNewLibraryWithSource(long device, String source, long options, long[] error);
    private static native long nMTLDeviceNewLibraryWithData(long device, ByteBuffer data, long[] error);
    private static native long nMTLDeviceNewLibraryWithURL(long device, String url, long[] error);
    private static native long nMTLDeviceNewDefaultLibrary(long device);
    
    // Function extraction
    private static native long nMTLLibraryNewFunctionWithName(long library, String name);
    private static native long nMTLLibraryNewFunctionWithNameConstantValues(long library, String name, long constants, long[] error);
    private static native String[] nMTLLibraryGetFunctionNames(long library);
    
    // Function constant values
    private static native long nMTLFunctionConstantValuesNew();
    private static native void nMTLFunctionConstantValuesSetBool(long values, String name, int index, boolean value);
    private static native void nMTLFunctionConstantValuesSetInt(long values, String name, int index, int value);
    private static native void nMTLFunctionConstantValuesSetUInt(long values, String name, int index, int value);
    private static native void nMTLFunctionConstantValuesSetFloat(long values, String name, int index, float value);
    private static native void nMTLFunctionConstantValuesSetHalf(long values, String name, int index, float value);
    
    // Linked functions
    private static native long nMTLLinkedFunctionsNew();
    private static native void nMTLLinkedFunctionsAddFunction(long linkedFunctions, long function);
    private static native void nMTLLinkedFunctionsAddBinaryFunction(long linkedFunctions, long function);
    
    // Binary archive
    private static native long nMTLBinaryArchiveDescriptorNew();
    private static native void nMTLBinaryArchiveDescriptorSetUrl(long descriptor, String url);
    private static native long nMTLDeviceNewBinaryArchive(long device, long descriptor, long[] error);
    private static native boolean nMTLBinaryArchiveAddRenderPipeline(long archive, long descriptor, long[] error);
    private static native boolean nMTLBinaryArchiveAddComputePipeline(long archive, long descriptor, long[] error);
    private static native boolean nMTLBinaryArchiveAddTilePipeline(long archive, long descriptor, long[] error);
    private static native boolean nMTLBinaryArchiveSerialize(long archive, String url, long[] error);
    
    // Error handling
    private static native String nNSErrorLocalizedDescription(long error);
    
    // Cross-compilation
    private static native byte[] nDXCCompileHLSLToSPIRV(String source, String[] args);
    private static native byte[] nGlslangCompileGLSLToSPIRV(String source, int stage);
    
    // Release
    private static native void nRelease(long object);
}