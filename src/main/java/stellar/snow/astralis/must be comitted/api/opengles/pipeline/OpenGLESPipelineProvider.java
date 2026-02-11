package stellar.snow.astralis.api.opengles.pipeline;

import stellar.snow.astralis.api.opengles.managers.OpenGLESManager;
import stellar.snow.astralis.api.opengles.mapping.OpenGLESCallMapper;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;


import org.apache.logging.log4j.Logger;/**
 * ╔══════════════════════════════════════════════════════════════════════════════════╗
 * ║                     OPENGL ES UNIFIED PIPELINE PROVIDER                          ║
 * ║                                                                                  ║
 * ║  Senior Architect Grade Implementation v2.0                                      ║
 * ║  Java 25 | Safety Critical | Performance First | Legacy & Modern GLES Support    ║
 * ║                                                                                  ║
 * ║  ARCHITECTURE OVERVIEW:                                                          ║
 * ║  ┌────────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │ Application Layer (Game Loop)                                              │  ║
 * ║  └──────┬──────────────────────────────────────────────────────────────┬──────┘  ║
 * ║         │                                                              │         ║
 * ║         ▼                                                              ▼         ║
 * ║  ┌──────────────┐                                               ┌──────────────┐ ║
 * ║  │ Resource     │                                               │ Frame        │ ║
 * ║  │ Orchestrator │<─────────────────────────────────────────────>│ Scheduler    │ ║
 * ║  │ (Pooling)    │                                               │ (Sync/Fence) │ ║
 * ║  └──────┬───────┘                                               └──────┬───────┘ ║
 * ║         │                                                              │         ║
 * ║         ▼                                                              ▼         ║
 * ║  ┌──────────────┐         ┌──────────────────────────┐          ┌──────────────┐ ║
 * ║  │ Memory       │         │ GLSLESPipelineProvider   │          │ State        │ ║
 * ║  │ Sentinel     │<───────>│ (Shader/Program Mgmt)    │<────────>│ Tracker      │ ║
 * ║  └──────┬───────┘         └──────────────────────────┘          └──────┬───────┘ ║
 * ║         │                                                              │         ║
 * ║         ▼                                                              ▼         ║
 * ║  ┌────────────────────────────────────────────────────────────────────────────┐  ║
 * ║  │                           OpenGLESManager (Backend)                        │  ║
 * ║  └────────────────────────────────────────────────────────────────────────────┘  ║
 * ║                                                                                  ║
 * ║  DESIGN PHILOSOPHY:                                                              ║
 * ║  1. Virtual Command Buffers: Abstracts GLES immediate mode into a deferred,      ║
 * ║     batched execution model to minimize JNI crossing overhead.                   ║
 * ║  2. Strict Thread Affinity: Enforces all GL execution on the designated render   ║
 * ║     thread to prevent context corruption.                                        ║
 * ║  3. State Tracking: Eliminates redundant GL state changes via shadow state.      ║
 * ║  4. Deferred Deletion: Resources deleted after GPU is done using them.           ║
 * ║  5. Object Pooling: Minimizes allocations in hot paths.                          ║
 * ║  6. Fence-based Sync: Proper CPU-GPU synchronization without stalls.             ║
 * ║                                                                                  ║
 * ║  PERFORMANCE CHARACTERISTICS:                                                    ║
 * ║  - State change: O(1) with branch for redundancy check                           ║
 * ║  - Resource creation: Pooled descriptors, zero alloc on hot path                 ║
 * ║  - Frame boundary: Fence-based, no glFinish stalls                               ║
 * ║  - Memory tracking: Lock-free primitive maps                                     ║
 * ║                                                                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════════════╝
 */
public final class OpenGLESPipelineProvider implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenGLESPipelineProvider.class);

    // ════════════════════════════════════════════════════════════════════════════
    // CONSTANTS
    // ════════════════════════════════════════════════════════════════════════════
    
    /** Maximum frames that can be in flight simultaneously */
    private static final int MAX_FRAMES_IN_FLIGHT = 3;
    
    /** Default command pool size */
    private static final int DEFAULT_COMMAND_POOL_SIZE = 64;
    
    /** Deferred deletion queue processing batch size */
    private static final int DELETION_BATCH_SIZE = 32;
    
    // GL Constants (to avoid magic numbers)
    private static final int GL_ARRAY_BUFFER = 0x8892;
    private static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    private static final int GL_UNIFORM_BUFFER = 0x8A11;
    private static final int GL_SHADER_STORAGE_BUFFER = 0x90D2;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_TEXTURE_CUBE_MAP = 0x8513;
    private static final int GL_TEXTURE_2D_ARRAY = 0x8C1A;
    private static final int GL_TEXTURE_3D = 0x806F;
    private static final int GL_FRAMEBUFFER = 0x8D40;
    private static final int GL_RENDERBUFFER = 0x8D41;
    private static final int GL_BUFFER = 0x82E0;
    private static final int GL_TEXTURE = 0x1702;
    private static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117;
    private static final int GL_SYNC_FLUSH_COMMANDS_BIT = 0x00000001;
    private static final long GL_TIMEOUT_IGNORED = 0xFFFFFFFFFFFFFFFFL;

    // ════════════════════════════════════════════════════════════════════════════
    // SINGLETON INFRASTRUCTURE (Holder Pattern)
    // ════════════════════════════════════════════════════════════════════════════

    private static final class InstanceHolder {
        private static volatile OpenGLESPipelineProvider instance;
        private static final Object lock = new Object();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // LIFECYCLE STATE
    // ════════════════════════════════════════════════════════════════════════════

    private static final int STATE_UNINITIALIZED = 0;
    private static final int STATE_INITIALIZING = 1;
    private static final int STATE_READY = 2;
    private static final int STATE_FRAME_ACTIVE = 3;
    private static final int STATE_SHUTTING_DOWN = 4;
    private static final int STATE_SHUTDOWN = 5;

    private static final VarHandle STATE_HANDLE;
    static {
        try {
            STATE_HANDLE = MethodHandles.lookup()
                .findVarHandle(OpenGLESPipelineProvider.class, "state", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int state = STATE_UNINITIALIZED;
    
    private final Thread renderThread;
    private final long renderThreadId;

    // ════════════════════════════════════════════════════════════════════════════
    // CORE DEPENDENCIES
    // ════════════════════════════════════════════════════════════════════════════

    private final OpenGLESManager manager;
    private final GLSLESPipelineProvider shaderProvider;
    private final Configuration configuration;

    // ════════════════════════════════════════════════════════════════════════════
    // SUBSYSTEMS
    // ════════════════════════════════════════════════════════════════════════════

    private final ResourceOrchestrator resourceOrchestrator;
    private final FrameScheduler frameScheduler;
    private final MemorySentinel memorySentinel;
    private final StateTracker stateTracker;
    private final DeferredDeletionQueue deletionQueue;
    private final Telemetry telemetry;
    private final CallDescriptorPool descriptorPool;

    // ════════════════════════════════════════════════════════════════════════════
    // FRAME STATE
    // ════════════════════════════════════════════════════════════════════════════

    private volatile OpenGLESManager.CommandBuffer currentFrameCommandBuffer;
    private volatile long currentFrameIndex = 0;

    // ════════════════════════════════════════════════════════════════════════════
    // CONSTRUCTION
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Private constructor - use {@link #initialize(Configuration)} to create instance.
     */
    private OpenGLESPipelineProvider(Configuration config, Thread creatingThread) {
        this.configuration = config;
        this.renderThread = creatingThread;
        this.renderThreadId = creatingThread.threadId();
        
        LOGGER.info("╔══════════════════════════════════════════════════════════════╗");
        LOGGER.info("║         Bootstrapping OpenGLESPipelineProvider...            ║");
        LOGGER.info("╚══════════════════════════════════════════════════════════════╝");
        
        // 1. Initialize Telemetry first (needed by other subsystems)
        this.telemetry = new Telemetry();
        
        // 2. Initialize Backend Manager
        OpenGLESManager.Builder builder = OpenGLESManager.builder()
            .withCommandPoolSize(config.commandPoolSize())
            .withMaxFramesInFlight(config.maxFramesInFlight());
        
        if (config.capabilities() != null) {
            builder.withCapabilities(config.capabilities());
        }
        if (config.debugCallback() != null) {
            builder.withDebugCallback(config.debugCallback());
        }
        
        this.manager = builder.build();
        
        // Log device info
        OpenGLESManager.DeviceCapabilities caps = this.manager.getCapabilities();
        logDeviceCapabilities(caps);
        
        // 3. Initialize subsystems
        this.descriptorPool = new CallDescriptorPool(config.descriptorPoolSize());
        this.memorySentinel = new MemorySentinel(telemetry);
        this.stateTracker = new StateTracker(caps);
        this.deletionQueue = new DeferredDeletionQueue(manager, memorySentinel);
        this.resourceOrchestrator = new ResourceOrchestrator(
            manager, memorySentinel, stateTracker, deletionQueue, descriptorPool, caps
        );
        this.frameScheduler = new FrameScheduler(
            manager, telemetry, deletionQueue, config.maxFramesInFlight()
        );
        
        // 4. Initialize Shader Provider
        this.shaderProvider = new GLSLESPipelineProvider(this);
        
        // 5. Mark as ready
        STATE_HANDLE.setRelease(this, STATE_READY);
        
        LOGGER.info("╔══════════════════════════════════════════════════════════════╗");
        LOGGER.info("║       OpenGLESPipelineProvider Initialization Complete       ║");
        LOGGER.info("╚══════════════════════════════════════════════════════════════╝");
    }
    
    private void logDeviceCapabilities(OpenGLESManager.DeviceCapabilities caps) {
        LOGGER.info("┌──────────────────────────────────────────────────────────────┐");
        LOGGER.info("│                    Device Capabilities                       │");
        LOGGER.info("├──────────────────────────────────────────────────────────────┤");
        LOGGER.info("│  Vendor: {}", padRight(caps.vendor(), 49) + "│");
        LOGGER.info("│  Renderer: {}", padRight(caps.rendererString(), 47) + "│");
        LOGGER.info("│  Version: {}", padRight(caps.versionString(), 48) + "│");
        LOGGER.info("│  GLES Version: {}", padRight(caps.majorVersion() + "." + caps.minorVersion(), 43) + "│");
        LOGGER.info("│  Max Texture Size: {}", padRight(String.valueOf(caps.maxTextureSize()), 39) + "│");
        LOGGER.info("│  Max Texture Units: {}", padRight(String.valueOf(caps.maxTextureUnits()), 38) + "│");
        LOGGER.info("│  Max Vertex Attribs: {}", padRight(String.valueOf(caps.maxVertexAttributes()), 37) + "│");
        LOGGER.info("│  Max UBO Size: {}", padRight(caps.maxUniformBlockSize() + " bytes", 43) + "│");
        LOGGER.info("│  Debug Output: {}", padRight(caps.supportsDebugOutput() ? "YES" : "NO", 43) + "│");
        LOGGER.info("│  Compute Shaders: {}", padRight(caps.supportsComputeShaders() ? "YES" : "NO", 40) + "│");
        LOGGER.info("└──────────────────────────────────────────────────────────────┘");
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SINGLETON ACCESS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Initializes the singleton instance.
     * 
     * <p><b>IMPORTANT:</b> Must be called from the render thread. The calling thread
     * becomes the designated render thread for all subsequent operations.
     *
     * @param config The configuration object.
     * @return The singleton instance.
     * @throws IllegalStateException if already initialized or called from wrong thread
     */
    public static OpenGLESPipelineProvider initialize(Configuration config) {
        Objects.requireNonNull(config, "Configuration cannot be null");
        
        if (InstanceHolder.instance != null) {
            throw new IllegalStateException(
                "OpenGLESPipelineProvider already initialized. Use getInstance() or shutdown first.");
        }
        
        synchronized (InstanceHolder.lock) {
            if (InstanceHolder.instance != null) {
                throw new IllegalStateException("OpenGLESPipelineProvider already initialized");
            }
            
            Thread currentThread = Thread.currentThread();
            LOGGER.info("Initializing OpenGLESPipelineProvider on thread: {} (ID: {})",
                currentThread.getName(), currentThread.threadId());
            
            InstanceHolder.instance = new OpenGLESPipelineProvider(config, currentThread);
            return InstanceHolder.instance;
        }
    }
    
    /**
     * Retrieves the singleton instance.
     *
     * @return The provider instance.
     * @throws IllegalStateException if not initialized.
     */
    public static OpenGLESPipelineProvider getInstance() {
        OpenGLESPipelineProvider inst = InstanceHolder.instance;
        if (inst == null) {
            throw new IllegalStateException(
                "OpenGLESPipelineProvider has not been initialized. Call initialize() first.");
        }
        return inst;
    }
    
    /**
     * Checks if the provider has been initialized.
     */
    public static boolean isInitialized() {
        return InstanceHolder.instance != null;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // FRAME LIFECYCLE
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Begins a new frame.
     * 
     * <p>This method:
     * <ul>
     *   <li>Waits for the oldest in-flight frame if needed</li>
     *   <li>Processes deferred deletions for completed frames</li>
     *   <li>Acquires a fresh command buffer for the frame</li>
     *   <li>Resets frame-local telemetry</li>
     * </ul>
     *
     * @throws IllegalStateException if called out of sequence or from wrong thread
     */
    public void beginFrame() {
        enforceRenderThread("beginFrame");
        
        int currentState = (int) STATE_HANDLE.getAcquire(this);
        if (currentState == STATE_FRAME_ACTIVE) {
            throw new IllegalStateException("beginFrame() called while frame already active. Call endFrame() first.");
        }
        if (currentState != STATE_READY) {
            throw new IllegalStateException("Invalid state for beginFrame(): " + stateToString(currentState));
        }
        
        // Transition to frame active
        if (!STATE_HANDLE.compareAndSet(this, STATE_READY, STATE_FRAME_ACTIVE)) {
            throw new IllegalStateException("Concurrent state modification detected");
        }
        
        // Increment frame index
        currentFrameIndex++;
        
        // Frame scheduler handles fence waiting and deletion processing
        frameScheduler.beginFrame(currentFrameIndex);
        
        // Reset frame telemetry
        telemetry.beginFrame(currentFrameIndex);
        
        // Acquire command buffer for this frame
        currentFrameCommandBuffer = manager.acquireCommandBuffer();
        
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Frame {} begun", currentFrameIndex);
        }
    }

    /**
     * Ends the current frame.
     * 
     * <p>This method:
     * <ul>
     *   <li>Submits the frame's command buffer to the GPU</li>
     *   <li>Inserts a fence for frame completion tracking</li>
     *   <li>Updates telemetry</li>
     * </ul>
     *
     * @throws IllegalStateException if called out of sequence or from wrong thread
     */
    public void endFrame() {
        enforceRenderThread("endFrame");
        
        int currentState = (int) STATE_HANDLE.getAcquire(this);
        if (currentState != STATE_FRAME_ACTIVE) {
            throw new IllegalStateException("endFrame() called without beginFrame(). Current state: " + 
                stateToString(currentState));
        }
        
        OpenGLESManager.CommandBuffer buffer = currentFrameCommandBuffer;
        if (buffer == null) {
            throw new IllegalStateException("Frame command buffer is null - internal error");
        }
        
        // Submit the frame's commands
        submitInternal(buffer, true);
        
        // Frame scheduler inserts fence and manages in-flight tracking
        frameScheduler.endFrame(currentFrameIndex);
        
        // Update telemetry
        telemetry.endFrame();
        
        // Clear frame buffer reference
        currentFrameCommandBuffer = null;
        
        // Transition back to ready
        STATE_HANDLE.setRelease(this, STATE_READY);
        
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Frame {} ended", currentFrameIndex);
        }
    }

    /**
     * Acquires a secondary command buffer for parallel command recording.
     * 
     * <p>Note: OpenGL ES is single-threaded. These buffers allow building command
     * lists on worker threads, but must still be submitted on the render thread.
     *
     * @return A clean CommandBuffer for recording.
     */
    public OpenGLESManager.CommandBuffer acquireSecondaryCommandBuffer() {
        validateOperationalState("acquireSecondaryCommandBuffer");
        return manager.acquireCommandBuffer();
    }

    /**
     * Submits a command buffer to the GPU for execution.
     *
     * @param buffer The buffer to execute.
     * @throws IllegalStateException if called from wrong thread
     */
    public void submit(OpenGLESManager.CommandBuffer buffer) {
        enforceRenderThread("submit");
        validateOperationalState("submit");
        Objects.requireNonNull(buffer, "Command buffer cannot be null");
        
        submitInternal(buffer, false);
    }
    
    private void submitInternal(OpenGLESManager.CommandBuffer buffer, boolean isFrameBuffer) {
        long submitStartNs = System.nanoTime();
        
        // Execute the batch
        manager.executeBatch(buffer);
        
        // Return to pool
        manager.releaseCommandBuffer(buffer);
        
        // Update telemetry
        long submitDurationNs = System.nanoTime() - submitStartNs;
        telemetry.recordSubmission(submitDurationNs);
    }

    /**
     * Gets the current frame's command buffer for direct command recording.
     * 
     * @return The active frame's command buffer.
     * @throws IllegalStateException if not within a frame
     */
    public OpenGLESManager.CommandBuffer getCurrentFrameCommandBuffer() {
        int currentState = (int) STATE_HANDLE.getAcquire(this);
        if (currentState != STATE_FRAME_ACTIVE) {
            throw new IllegalStateException("No active frame. Call beginFrame() first.");
        }
        return currentFrameCommandBuffer;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // LIFECYCLE MANAGEMENT
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Shuts down the provider and releases all resources.
     */
    @Override
    public void close() {
        int currentState = (int) STATE_HANDLE.getAcquire(this);
        
        // Handle case where we're in a frame
        if (currentState == STATE_FRAME_ACTIVE) {
            LOGGER.warn("Shutdown called during active frame - forcing frame end");
            try {
                endFrame();
            } catch (Exception e) {
                LOGGER.error("Error ending frame during shutdown", e);
            }
            currentState = (int) STATE_HANDLE.getAcquire(this);
        }
        
        // Try to transition to shutting down
        if (!STATE_HANDLE.compareAndSet(this, currentState, STATE_SHUTTING_DOWN)) {
            // Already shutting down or shutdown
            int newState = (int) STATE_HANDLE.getAcquire(this);
            if (newState == STATE_SHUTTING_DOWN || newState == STATE_SHUTDOWN) {
                return;
            }
        }
        
        LOGGER.info("╔══════════════════════════════════════════════════════════════╗");
        LOGGER.info("║          Shutting down OpenGLESPipelineProvider...           ║");
        LOGGER.info("╚══════════════════════════════════════════════════════════════╝");
        
        try {
            // 1. Wait for GPU to complete all work
            frameScheduler.waitForAllFrames();
            
            // 2. Process all pending deletions
            deletionQueue.processAll();
            
            // 3. Shutdown subsystems in order
            if (shaderProvider != null) {
                try {
                    shaderProvider.close();
                } catch (Exception e) {
                    LOGGER.error("Error closing shader provider", e);
                }
            }
            
            if (resourceOrchestrator != null) {
                resourceOrchestrator.shutdown();
            }
            
            // 4. Report any leaks
            if (memorySentinel != null) {
                memorySentinel.reportLeaks();
            }
            
            // 5. Log final telemetry
            if (telemetry != null) {
                telemetry.logFinalReport();
            }
            
            // 6. Shutdown manager
            if (manager != null) {
                manager.shutdown();
            }
            
        } finally {
            // Clear singleton reference
            synchronized (InstanceHolder.lock) {
                if (InstanceHolder.instance == this) {
                    InstanceHolder.instance = null;
                }
            }
            
            STATE_HANDLE.setRelease(this, STATE_SHUTDOWN);
            
            LOGGER.info("╔══════════════════════════════════════════════════════════════╗");
            LOGGER.info("║         OpenGLESPipelineProvider shutdown complete           ║");
            LOGGER.info("╚══════════════════════════════════════════════════════════════╝");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SUBSYSTEM ACCESSORS
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Gets the shader pipeline provider for shader/program management.
     */
    public GLSLESPipelineProvider getShaderProvider() {
        return shaderProvider;
    }

    /**
     * Gets the resource orchestrator for buffer/texture/etc. management.
     */
    public ResourceOrchestrator getResources() {
        return resourceOrchestrator;
    }

    /**
     * Gets the underlying OpenGLES manager for low-level access.
     */
    public OpenGLESManager getManager() {
        return manager;
    }

    /**
     * Gets the telemetry subsystem for performance monitoring.
     */
    public Telemetry getTelemetry() {
        return telemetry;
    }
    
    /**
     * Gets the state tracker for checking/setting GL state.
     */
    public StateTracker getStateTracker() {
        return stateTracker;
    }
    
    /**
     * Gets the current frame index.
     */
    public long getCurrentFrameIndex() {
        return currentFrameIndex;
    }
    
    /**
     * Gets the configuration used to initialize this provider.
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // VALIDATION & SAFETY
    // ════════════════════════════════════════════════════════════════════════════

    private void enforceRenderThread(String operation) {
        if (Thread.currentThread().threadId() != renderThreadId) {
            throw new IllegalStateException(
                "Thread Affinity Violation: Operation '" + operation + "' called from thread '" + 
                Thread.currentThread().getName() + "' (ID: " + Thread.currentThread().threadId() + 
                "), but provider requires render thread '" + renderThread.getName() + 
                "' (ID: " + renderThreadId + ")");
        }
    }
    
    private void validateOperationalState(String operation) {
        int currentState = (int) STATE_HANDLE.getAcquire(this);
        if (currentState == STATE_SHUTDOWN || currentState == STATE_SHUTTING_DOWN) {
            throw new IllegalStateException(
                "Operation '" + operation + "' failed: Provider is " + stateToString(currentState));
        }
        if (currentState == STATE_UNINITIALIZED || currentState == STATE_INITIALIZING) {
            throw new IllegalStateException(
                "Operation '" + operation + "' failed: Provider not ready. State: " + stateToString(currentState));
        }
    }
    
    private static String stateToString(int state) {
        return switch (state) {
            case STATE_UNINITIALIZED -> "UNINITIALIZED";
            case STATE_INITIALIZING -> "INITIALIZING";
            case STATE_READY -> "READY";
            case STATE_FRAME_ACTIVE -> "FRAME_ACTIVE";
            case STATE_SHUTTING_DOWN -> "SHUTTING_DOWN";
            case STATE_SHUTDOWN -> "SHUTDOWN";
            default -> "UNKNOWN(" + state + ")";
        };
    }
    
    private static String padRight(String s, int n) {
        if (s == null) s = "null";
        if (s.length() >= n) return s.substring(0, n);
        return s + " ".repeat(n - s.length());
    }

    // ════════════════════════════════════════════════════════════════════════════
    // RESOURCE ORCHESTRATOR
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Manages GPU resource creation, tracking, and destruction.
     * 
     * <p>Features:
     * <ul>
     *   <li>Automatic resource tracking via MemorySentinel</li>
     *   <li>Deferred deletion for in-flight resources</li>
     *   <li>State tracking to avoid redundant binds</li>
     *   <li>Pooled descriptors for zero-alloc hot paths</li>
     * </ul>
     */
    public static final class ResourceOrchestrator {
        
        private final OpenGLESManager manager;
        private final MemorySentinel sentinel;
        private final StateTracker stateTracker;
        private final DeferredDeletionQueue deletionQueue;
        private final CallDescriptorPool descriptorPool;
        private final OpenGLESManager.DeviceCapabilities caps;
        private final StampedLock lock = new StampedLock();
        
        private volatile boolean shutdown = false;

        ResourceOrchestrator(
                OpenGLESManager manager,
                MemorySentinel sentinel,
                StateTracker stateTracker,
                DeferredDeletionQueue deletionQueue,
                CallDescriptorPool descriptorPool,
                OpenGLESManager.DeviceCapabilities caps) {
            this.manager = manager;
            this.sentinel = sentinel;
            this.stateTracker = stateTracker;
            this.deletionQueue = deletionQueue;
            this.descriptorPool = descriptorPool;
            this.caps = caps;
        }

        // ════════════════════════════════════════════════════════════════════
        // BUFFER MANAGEMENT
        // ════════════════════════════════════════════════════════════════════

        /**
         * Creates a GPU buffer (VBO, IBO, UBO, SSBO).
         *
         * @param target The binding target (GL_ARRAY_BUFFER, etc.).
         * @param size Size in bytes.
         * @param usage Usage hint (GL_STATIC_DRAW, etc.).
         * @param label Debug label (optional, may be null).
         * @return The OpenGL buffer ID.
         * @throws OutOfMemoryError if allocation fails
         */
        public int createBuffer(int target, long size, int usage, String label) {
            validateNotShutdown();
            
            if (size <= 0) {
                throw new IllegalArgumentException("Buffer size must be positive: " + size);
            }
            if (size > Integer.MAX_VALUE && !caps.supports64BitBuffers()) {
                throw new IllegalArgumentException("Buffer size exceeds 32-bit limit: " + size);
            }
            
            long stamp = lock.writeLock();
            try {
                // 1. Generate buffer ID
                OpenGLESManager.CallDescriptor genCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.GEN_BUFFERS)
                    .addInt(1)
                    .seal();
                
                OpenGLESManager.MappingResult genResult = manager.mapCall(genCall);
                descriptorPool.release(genCall);
                
                if (!genResult.isSuccess()) {
                    throw new RuntimeException("Failed to generate buffer: " + genResult.message());
                }
                
                int bufferId = genResult.nativeResult();
                
                // 2. Bind buffer (updates state tracker)
                bindBufferInternal(target, bufferId);
                
                // 3. Allocate storage
                OpenGLESManager.CallDescriptor dataCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.BUFFER_DATA)
                    .addInt(target)
                    .addLong(size)
                    .addInt(usage)
                    .addObject(null) // No initial data
                    .seal();
                
                OpenGLESManager.MappingResult allocResult = manager.mapCall(dataCall);
                descriptorPool.release(dataCall);
                
                if (!allocResult.isSuccess()) {
                    // Cleanup on failure
                    deleteBufferImmediate(bufferId);
                    throw new OutOfMemoryError("Failed to allocate buffer storage (" + size + " bytes): " + 
                        allocResult.message());
                }
                
                // 4. Set debug label if supported
                if (label != null && caps.supportsDebugOutput()) {
                    setObjectLabel(GL_BUFFER, bufferId, label);
                }
                
                // 5. Track allocation
                String effectiveLabel = label != null ? label : "Buffer_" + bufferId;
                sentinel.track(bufferId, size, effectiveLabel, ResourceType.BUFFER);
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created buffer {} ({} bytes, target=0x{}, usage=0x{}): '{}'",
                        bufferId, size, Integer.toHexString(target), Integer.toHexString(usage), effectiveLabel);
                }
                
                return bufferId;
                
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Creates a buffer with initial data.
         */
        public int createBuffer(int target, ByteBuffer data, int usage, String label) {
            validateNotShutdown();
            Objects.requireNonNull(data, "Data buffer cannot be null");
            
            long size = data.remaining();
            if (size <= 0) {
                throw new IllegalArgumentException("Data buffer is empty");
            }
            
            long stamp = lock.writeLock();
            try {
                // Generate and bind
                int bufferId = generateBufferId();
                bindBufferInternal(target, bufferId);
                
                // Allocate with data
                OpenGLESManager.CallDescriptor dataCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.BUFFER_DATA)
                    .addInt(target)
                    .addLong(size)
                    .addInt(usage)
                    .addObject(data)
                    .seal();
                
                OpenGLESManager.MappingResult result = manager.mapCall(dataCall);
                descriptorPool.release(dataCall);
                
                if (!result.isSuccess()) {
                    deleteBufferImmediate(bufferId);
                    throw new OutOfMemoryError("Failed to allocate buffer with data: " + result.message());
                }
                
                if (label != null && caps.supportsDebugOutput()) {
                    setObjectLabel(GL_BUFFER, bufferId, label);
                }
                
                String effectiveLabel = label != null ? label : "Buffer_" + bufferId;
                sentinel.track(bufferId, size, effectiveLabel, ResourceType.BUFFER);
                
                return bufferId;
                
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Updates a subregion of a buffer.
         */
        public void updateBuffer(int target, int bufferId, long offset, ByteBuffer data) {
            validateNotShutdown();
            Objects.requireNonNull(data, "Data buffer cannot be null");
            
            long stamp = lock.readLock();
            try {
                bindBufferInternal(target, bufferId);
                
                OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.BUFFER_SUB_DATA)
                    .addInt(target)
                    .addLong(offset)
                    .addLong(data.remaining())
                    .addObject(data)
                    .seal();
                
                manager.mapCall(call);
                descriptorPool.release(call);
                
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        /**
         * Destroys a buffer (deferred until GPU is done with it).
         */
        public void destroyBuffer(int bufferId) {
            if (bufferId == 0) return;
            deletionQueue.queueDeletion(ResourceType.BUFFER, bufferId);
        }
        
        /**
         * Destroys a buffer immediately (use only when GPU is definitely not using it).
         */
        public void destroyBufferImmediate(int bufferId) {
            if (bufferId == 0) return;
            deleteBufferImmediate(bufferId);
            sentinel.untrack(bufferId);
        }
        
        private void deleteBufferImmediate(int bufferId) {
            // Unbind from all targets if bound
            stateTracker.unbindBuffer(bufferId);
            
            OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                .reset(OpenGLESManager.GLESCallType.DELETE_BUFFERS)
                .addInt(1)
                .addInt(bufferId)
                .seal();
            
            manager.mapCall(call);
            descriptorPool.release(call);
        }
        
        private int generateBufferId() {
            OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                .reset(OpenGLESManager.GLESCallType.GEN_BUFFERS)
                .addInt(1)
                .seal();
            
            OpenGLESManager.MappingResult result = manager.mapCall(call);
            descriptorPool.release(call);
            
            if (!result.isSuccess()) {
                throw new RuntimeException("Failed to generate buffer: " + result.message());
            }
            return result.nativeResult();
        }
        
        private void bindBufferInternal(int target, int bufferId) {
            // Check state tracker to avoid redundant bind
            if (stateTracker.setBufferBinding(target, bufferId)) {
                OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.BIND_BUFFER)
                    .addInt(target)
                    .addInt(bufferId)
                    .seal();
                
                manager.mapCall(call);
                descriptorPool.release(call);
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // TEXTURE MANAGEMENT
        // ════════════════════════════════════════════════════════════════════

        /**
         * Creates a 2D texture.
         *
         * @param width Width in pixels.
         * @param height Height in pixels.
         * @param internalFormat Internal format (GL_RGBA8, etc.).
         * @param format Data format (GL_RGBA, etc.).
         * @param type Data type (GL_UNSIGNED_BYTE, etc.).
         * @param label Debug label.
         * @return The OpenGL texture ID.
         */
        public int createTexture2D(int width, int height, int internalFormat, 
                                   int format, int type, String label) {
            validateNotShutdown();
            validateTextureDimensions(width, height);
            
            long stamp = lock.writeLock();
            try {
                int texId = generateTextureId();
                bindTextureInternal(GL_TEXTURE_2D, 0, texId);
                
                // Allocate storage
                OpenGLESManager.CallDescriptor imgCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.TEX_IMAGE_2D)
                    .addInt(GL_TEXTURE_2D)
                    .addInt(0) // Level
                    .addInt(internalFormat)
                    .addInt(width)
                    .addInt(height)
                    .addInt(0) // Border
                    .addInt(format)
                    .addInt(type)
                    .addObject(null) // No data
                    .seal();
                
                OpenGLESManager.MappingResult result = manager.mapCall(imgCall);
                descriptorPool.release(imgCall);
                
                if (!result.isSuccess()) {
                    deleteTextureImmediate(texId);
                    throw new RuntimeException("Failed to allocate texture storage: " + result.message());
                }
                
                // Set default sampling parameters
                setDefaultTextureParameters(GL_TEXTURE_2D);
                
                if (label != null && caps.supportsDebugOutput()) {
                    setObjectLabel(GL_TEXTURE, texId, label);
                }
                
                long estimatedSize = estimateTextureSize(width, height, 1, internalFormat);
                String effectiveLabel = label != null ? label : "Texture2D_" + texId;
                sentinel.track(texId, estimatedSize, effectiveLabel, ResourceType.TEXTURE);
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Created Texture2D {} ({}x{}, format=0x{}): '{}'",
                        texId, width, height, Integer.toHexString(internalFormat), effectiveLabel);
                }
                
                return texId;
                
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Creates a 2D texture with initial data.
         */
        public int createTexture2D(int width, int height, int internalFormat,
                                   int format, int type, ByteBuffer data, String label) {
            validateNotShutdown();
            validateTextureDimensions(width, height);
            Objects.requireNonNull(data, "Data buffer cannot be null");
            
            long stamp = lock.writeLock();
            try {
                int texId = generateTextureId();
                bindTextureInternal(GL_TEXTURE_2D, 0, texId);
                
                OpenGLESManager.CallDescriptor imgCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.TEX_IMAGE_2D)
                    .addInt(GL_TEXTURE_2D)
                    .addInt(0)
                    .addInt(internalFormat)
                    .addInt(width)
                    .addInt(height)
                    .addInt(0)
                    .addInt(format)
                    .addInt(type)
                    .addObject(data)
                    .seal();
                
                OpenGLESManager.MappingResult result = manager.mapCall(imgCall);
                descriptorPool.release(imgCall);
                
                if (!result.isSuccess()) {
                    deleteTextureImmediate(texId);
                    throw new RuntimeException("Failed to upload texture data: " + result.message());
                }
                
                setDefaultTextureParameters(GL_TEXTURE_2D);
                
                if (label != null && caps.supportsDebugOutput()) {
                    setObjectLabel(GL_TEXTURE, texId, label);
                }
                
                long estimatedSize = estimateTextureSize(width, height, 1, internalFormat);
                sentinel.track(texId, estimatedSize, label != null ? label : "Texture2D_" + texId, ResourceType.TEXTURE);
                
                return texId;
                
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Creates a cubemap texture.
         */
        public int createTextureCubeMap(int size, int internalFormat, int format, int type, String label) {
            validateNotShutdown();
            validateTextureDimensions(size, size);
            
            long stamp = lock.writeLock();
            try {
                int texId = generateTextureId();
                bindTextureInternal(GL_TEXTURE_CUBE_MAP, 0, texId);
                
                // Allocate all 6 faces
                int[] faces = {
                    0x8515, // GL_TEXTURE_CUBE_MAP_POSITIVE_X
                    0x8516, // GL_TEXTURE_CUBE_MAP_NEGATIVE_X
                    0x8517, // GL_TEXTURE_CUBE_MAP_POSITIVE_Y
                    0x8518, // GL_TEXTURE_CUBE_MAP_NEGATIVE_Y
                    0x8519, // GL_TEXTURE_CUBE_MAP_POSITIVE_Z
                    0x851A  // GL_TEXTURE_CUBE_MAP_NEGATIVE_Z
                };
                
                for (int face : faces) {
                    OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                        .reset(OpenGLESManager.GLESCallType.TEX_IMAGE_2D)
                        .addInt(face)
                        .addInt(0)
                        .addInt(internalFormat)
                        .addInt(size)
                        .addInt(size)
                        .addInt(0)
                        .addInt(format)
                        .addInt(type)
                        .addObject(null)
                        .seal();
                    
                    OpenGLESManager.MappingResult result = manager.mapCall(call);
                    descriptorPool.release(call);
                    
                    if (!result.isSuccess()) {
                        deleteTextureImmediate(texId);
                        throw new RuntimeException("Failed to allocate cubemap face: " + result.message());
                    }
                }
                
                setDefaultTextureParameters(GL_TEXTURE_CUBE_MAP);
                
                if (label != null && caps.supportsDebugOutput()) {
                    setObjectLabel(GL_TEXTURE, texId, label);
                }
                
                long estimatedSize = estimateTextureSize(size, size, 6, internalFormat);
                sentinel.track(texId, estimatedSize, label != null ? label : "CubeMap_" + texId, ResourceType.TEXTURE);
                
                return texId;
                
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Destroys a texture (deferred).
         */
        public void destroyTexture(int textureId) {
            if (textureId == 0) return;
            deletionQueue.queueDeletion(ResourceType.TEXTURE, textureId);
        }
        
        /**
         * Destroys a texture immediately.
         */
        public void destroyTextureImmediate(int textureId) {
            if (textureId == 0) return;
            deleteTextureImmediate(textureId);
            sentinel.untrack(textureId);
        }
        
        private void deleteTextureImmediate(int textureId) {
            stateTracker.unbindTexture(textureId);
            
            OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                .reset(OpenGLESManager.GLESCallType.DELETE_TEXTURES)
                .addInt(1)
                .addInt(textureId)
                .seal();
            
            manager.mapCall(call);
            descriptorPool.release(call);
        }
        
        private int generateTextureId() {
            OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                .reset(OpenGLESManager.GLESCallType.GEN_TEXTURES)
                .addInt(1)
                .seal();
            
            OpenGLESManager.MappingResult result = manager.mapCall(call);
            descriptorPool.release(call);
            
            if (!result.isSuccess()) {
                throw new RuntimeException("Failed to generate texture: " + result.message());
            }
            return result.nativeResult();
        }
        
        private void bindTextureInternal(int target, int unit, int textureId) {
            if (stateTracker.setTextureBinding(unit, target, textureId)) {
                // Activate unit if needed
                if (stateTracker.setActiveTexture(unit)) {
                    OpenGLESManager.CallDescriptor activeCall = descriptorPool.acquire()
                        .reset(OpenGLESManager.GLESCallType.ACTIVE_TEXTURE)
                        .addInt(0x84C0 + unit) // GL_TEXTURE0 + unit
                        .seal();
                    manager.mapCall(activeCall);
                    descriptorPool.release(activeCall);
                }
                
                OpenGLESManager.CallDescriptor bindCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.BIND_TEXTURE)
                    .addInt(target)
                    .addInt(textureId)
                    .seal();
                manager.mapCall(bindCall);
                descriptorPool.release(bindCall);
            }
        }
        
        private void setDefaultTextureParameters(int target) {
            // GL_TEXTURE_MIN_FILTER = 0x2801, GL_TEXTURE_MAG_FILTER = 0x2800
            // GL_LINEAR = 0x2601, GL_LINEAR_MIPMAP_LINEAR = 0x2703
            // GL_TEXTURE_WRAP_S = 0x2802, GL_TEXTURE_WRAP_T = 0x2803
            // GL_CLAMP_TO_EDGE = 0x812F
            
            setTextureParameter(target, 0x2801, 0x2601); // MIN_FILTER = LINEAR
            setTextureParameter(target, 0x2800, 0x2601); // MAG_FILTER = LINEAR
            setTextureParameter(target, 0x2802, 0x812F); // WRAP_S = CLAMP_TO_EDGE
            setTextureParameter(target, 0x2803, 0x812F); // WRAP_T = CLAMP_TO_EDGE
        }
        
        private void setTextureParameter(int target, int pname, int param) {
            OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                .reset(OpenGLESManager.GLESCallType.TEX_PARAMETER_I)
                .addInt(target)
                .addInt(pname)
                .addInt(param)
                .seal();
            manager.mapCall(call);
            descriptorPool.release(call);
        }
        
        private void validateTextureDimensions(int width, int height) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Invalid texture dimensions: " + width + "x" + height);
            }
            int maxSize = caps.maxTextureSize();
            if (width > maxSize || height > maxSize) {
                throw new IllegalArgumentException(
                    "Texture dimensions exceed device limit: " + width + "x" + height + " > " + maxSize);
            }
        }
        
        private long estimateTextureSize(int width, int height, int layers, int internalFormat) {
            int bytesPerPixel = switch (internalFormat) {
                case 0x8058 -> 4; // GL_RGBA8
                case 0x8051 -> 3; // GL_RGB8
                case 0x8D62 -> 2; // GL_RGB565
                case 0x8C3A -> 2; // GL_R11F_G11F_B10F
                case 0x8C43 -> 4; // GL_SRGB8_ALPHA8
                case 0x8CAC -> 4; // GL_DEPTH_COMPONENT32F
                case 0x81A6 -> 3; // GL_DEPTH_COMPONENT24
                case 0x81A5 -> 2; // GL_DEPTH_COMPONENT16
                default -> 4;
            };
            return (long) width * height * layers * bytesPerPixel;
        }

        // ════════════════════════════════════════════════════════════════════
        // FRAMEBUFFER MANAGEMENT
        // ════════════════════════════════════════════════════════════════════

        /**
         * Creates a framebuffer object.
         */
        public int createFramebuffer(String label) {
            validateNotShutdown();
            
            long stamp = lock.writeLock();
            try {
                OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.GEN_FRAMEBUFFERS)
                    .addInt(1)
                    .seal();
                
                OpenGLESManager.MappingResult result = manager.mapCall(call);
                descriptorPool.release(call);
                
                if (!result.isSuccess()) {
                    throw new RuntimeException("Failed to generate framebuffer: " + result.message());
                }
                
                int fboId = result.nativeResult();
                
                if (label != null && caps.supportsDebugOutput()) {
                    setObjectLabel(GL_FRAMEBUFFER, fboId, label);
                }
                
                sentinel.track(fboId, 0, label != null ? label : "FBO_" + fboId, ResourceType.FRAMEBUFFER);
                
                return fboId;
                
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Attaches a texture to a framebuffer.
         */
        public void framebufferTexture2D(int framebuffer, int attachment, int textureTarget, 
                                         int texture, int level) {
            validateNotShutdown();
            
            long stamp = lock.readLock();
            try {
                bindFramebufferInternal(GL_FRAMEBUFFER, framebuffer);
                
                OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.FRAMEBUFFER_TEXTURE_2D)
                    .addInt(GL_FRAMEBUFFER)
                    .addInt(attachment)
                    .addInt(textureTarget)
                    .addInt(texture)
                    .addInt(level)
                    .seal();
                
                manager.mapCall(call);
                descriptorPool.release(call);
                
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        /**
         * Attaches a renderbuffer to a framebuffer.
         */
        public void framebufferRenderbuffer(int framebuffer, int attachment, int renderbuffer) {
            validateNotShutdown();
            
            long stamp = lock.readLock();
            try {
                bindFramebufferInternal(GL_FRAMEBUFFER, framebuffer);
                
                OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.FRAMEBUFFER_RENDERBUFFER)
                    .addInt(GL_FRAMEBUFFER)
                    .addInt(attachment)
                    .addInt(GL_RENDERBUFFER)
                    .addInt(renderbuffer)
                    .seal();
                
                manager.mapCall(call);
                descriptorPool.release(call);
                
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        /**
         * Checks framebuffer completeness status.
         */
        public int checkFramebufferStatus(int framebuffer) {
            long stamp = lock.readLock();
            try {
                bindFramebufferInternal(GL_FRAMEBUFFER, framebuffer);
                
                OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.CHECK_FRAMEBUFFER_STATUS)
                    .addInt(GL_FRAMEBUFFER)
                    .seal();
                
                OpenGLESManager.MappingResult result = manager.mapCall(call);
                descriptorPool.release(call);
                
                return result.nativeResult();
                
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        /**
         * Destroys a framebuffer (deferred).
         */
        public void destroyFramebuffer(int framebufferId) {
            if (framebufferId == 0) return;
            deletionQueue.queueDeletion(ResourceType.FRAMEBUFFER, framebufferId);
        }
        
        private void bindFramebufferInternal(int target, int framebuffer) {
            if (stateTracker.setFramebufferBinding(target, framebuffer)) {
                OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.BIND_FRAMEBUFFER)
                    .addInt(target)
                    .addInt(framebuffer)
                    .seal();
                manager.mapCall(call);
                descriptorPool.release(call);
            }
        }

        // ════════════════════════════════════════════════════════════════════
        // RENDERBUFFER MANAGEMENT
        // ════════════════════════════════════════════════════════════════════

        /**
         * Creates a renderbuffer with specified format and dimensions.
         */
        public int createRenderbuffer(int internalFormat, int width, int height, String label) {
            validateNotShutdown();
            
            long stamp = lock.writeLock();
            try {
                OpenGLESManager.CallDescriptor genCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.GEN_RENDERBUFFERS)
                    .addInt(1)
                    .seal();
                
                OpenGLESManager.MappingResult result = manager.mapCall(genCall);
                descriptorPool.release(genCall);
                
                if (!result.isSuccess()) {
                    throw new RuntimeException("Failed to generate renderbuffer: " + result.message());
                }
                
                int rboId = result.nativeResult();
                
                // Bind
                OpenGLESManager.CallDescriptor bindCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.BIND_RENDERBUFFER)
                    .addInt(GL_RENDERBUFFER)
                    .addInt(rboId)
                    .seal();
                manager.mapCall(bindCall);
                descriptorPool.release(bindCall);
                
                // Allocate storage
                OpenGLESManager.CallDescriptor storageCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.RENDERBUFFER_STORAGE)
                    .addInt(GL_RENDERBUFFER)
                    .addInt(internalFormat)
                    .addInt(width)
                    .addInt(height)
                    .seal();
                
                OpenGLESManager.MappingResult storageResult = manager.mapCall(storageCall);
                descriptorPool.release(storageCall);
                
                if (!storageResult.isSuccess()) {
                    destroyRenderbufferImmediate(rboId);
                    throw new RuntimeException("Failed to allocate renderbuffer storage: " + storageResult.message());
                }
                
                if (label != null && caps.supportsDebugOutput()) {
                    setObjectLabel(GL_RENDERBUFFER, rboId, label);
                }
                
                long size = estimateTextureSize(width, height, 1, internalFormat);
                sentinel.track(rboId, size, label != null ? label : "RBO_" + rboId, ResourceType.RENDERBUFFER);
                
                return rboId;
                
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Creates a multisampled renderbuffer.
         */
        public int createRenderbufferMultisample(int samples, int internalFormat, 
                                                  int width, int height, String label) {
            validateNotShutdown();
            
            if (!caps.supportsMultisampledRenderbuffers()) {
                throw new UnsupportedOperationException("Device does not support multisampled renderbuffers");
            }
            
            long stamp = lock.writeLock();
            try {
                OpenGLESManager.CallDescriptor genCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.GEN_RENDERBUFFERS)
                    .addInt(1)
                    .seal();
                
                int rboId = manager.mapCall(genCall).nativeResult();
                descriptorPool.release(genCall);
                
                OpenGLESManager.CallDescriptor bindCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.BIND_RENDERBUFFER)
                    .addInt(GL_RENDERBUFFER)
                    .addInt(rboId)
                    .seal();
                manager.mapCall(bindCall);
                descriptorPool.release(bindCall);
                
                OpenGLESManager.CallDescriptor storageCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.RENDERBUFFER_STORAGE_MULTISAMPLE)
                    .addInt(GL_RENDERBUFFER)
                    .addInt(samples)
                    .addInt(internalFormat)
                    .addInt(width)
                    .addInt(height)
                    .seal();
                
                OpenGLESManager.MappingResult result = manager.mapCall(storageCall);
                descriptorPool.release(storageCall);
                
                if (!result.isSuccess()) {
                    destroyRenderbufferImmediate(rboId);
                    throw new RuntimeException("Failed to create multisampled renderbuffer: " + result.message());
                }
                
                if (label != null && caps.supportsDebugOutput()) {
                    setObjectLabel(GL_RENDERBUFFER, rboId, label);
                }
                
                long size = estimateTextureSize(width, height, samples, internalFormat);
                sentinel.track(rboId, size, label != null ? label : "MSAA_RBO_" + rboId, ResourceType.RENDERBUFFER);
                
                return rboId;
                
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Destroys a renderbuffer (deferred).
         */
        public void destroyRenderbuffer(int renderbufferId) {
            if (renderbufferId == 0) return;
            deletionQueue.queueDeletion(ResourceType.RENDERBUFFER, renderbufferId);
        }
        
        private void destroyRenderbufferImmediate(int renderbufferId) {
            OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                .reset(OpenGLESManager.GLESCallType.DELETE_RENDERBUFFERS)
                .addInt(1)
                .addInt(renderbufferId)
                .seal();
            manager.mapCall(call);
            descriptorPool.release(call);
        }

        // ════════════════════════════════════════════════════════════════════
        // VAO MANAGEMENT
        // ════════════════════════════════════════════════════════════════════

        /**
         * Creates a Vertex Array Object.
         */
        public int createVertexArray(String label) {
            validateNotShutdown();
            
            if (!caps.supportsVertexArrayObjects()) {
                throw new UnsupportedOperationException("Device does not support Vertex Array Objects");
            }
            
            long stamp = lock.writeLock();
            try {
                OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.GEN_VERTEX_ARRAYS)
                    .addInt(1)
                    .seal();
                
                OpenGLESManager.MappingResult result = manager.mapCall(call);
                descriptorPool.release(call);
                
                if (!result.isSuccess()) {
                    throw new RuntimeException("Failed to generate VAO: " + result.message());
                }
                
                int vaoId = result.nativeResult();
                
                if (label != null && caps.supportsDebugOutput()) {
                    setObjectLabel(0x8074, vaoId, label); // GL_VERTEX_ARRAY
                }
                
                sentinel.track(vaoId, 0, label != null ? label : "VAO_" + vaoId, ResourceType.VERTEX_ARRAY);
                
                return vaoId;
                
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Binds a Vertex Array Object.
         */
        public void bindVertexArray(int vaoId) {
            if (stateTracker.setVertexArrayBinding(vaoId)) {
                OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.BIND_VERTEX_ARRAY)
                    .addInt(vaoId)
                    .seal();
                manager.mapCall(call);
                descriptorPool.release(call);
            }
        }
        
        /**
         * Destroys a Vertex Array Object (deferred).
         */
        public void destroyVertexArray(int vaoId) {
            if (vaoId == 0) return;
            deletionQueue.queueDeletion(ResourceType.VERTEX_ARRAY, vaoId);
        }

        // ════════════════════════════════════════════════════════════════════
        // SAMPLER MANAGEMENT
        // ════════════════════════════════════════════════════════════════════

        /**
         * Creates a sampler object.
         */
        public int createSampler(SamplerConfig config, String label) {
            validateNotShutdown();
            
            if (!caps.supportsSamplerObjects()) {
                throw new UnsupportedOperationException("Device does not support Sampler Objects");
            }
            
            long stamp = lock.writeLock();
            try {
                OpenGLESManager.CallDescriptor genCall = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.GEN_SAMPLERS)
                    .addInt(1)
                    .seal();
                
                int samplerId = manager.mapCall(genCall).nativeResult();
                descriptorPool.release(genCall);
                
                // Set parameters
                setSamplerParameter(samplerId, 0x2801, config.minFilter()); // MIN_FILTER
                setSamplerParameter(samplerId, 0x2800, config.magFilter()); // MAG_FILTER
                setSamplerParameter(samplerId, 0x2802, config.wrapS());     // WRAP_S
                setSamplerParameter(samplerId, 0x2803, config.wrapT());     // WRAP_T
                setSamplerParameter(samplerId, 0x8072, config.wrapR());     // WRAP_R
                
                if (config.anisotropy() > 1.0f && caps.supportsAnisotropicFiltering()) {
                    setSamplerParameterf(samplerId, 0x84FE, config.anisotropy()); // TEXTURE_MAX_ANISOTROPY
                }
                
                if (label != null && caps.supportsDebugOutput()) {
                    setObjectLabel(0x82E6, samplerId, label); // GL_SAMPLER
                }
                
                sentinel.track(samplerId, 0, label != null ? label : "Sampler_" + samplerId, ResourceType.SAMPLER);
                
                return samplerId;
                
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Binds a sampler to a texture unit.
         */
        public void bindSampler(int unit, int samplerId) {
            if (stateTracker.setSamplerBinding(unit, samplerId)) {
                OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                    .reset(OpenGLESManager.GLESCallType.BIND_SAMPLER)
                    .addInt(unit)
                    .addInt(samplerId)
                    .seal();
                manager.mapCall(call);
                descriptorPool.release(call);
            }
        }
        
        /**
         * Destroys a sampler (deferred).
         */
        public void destroySampler(int samplerId) {
            if (samplerId == 0) return;
            deletionQueue.queueDeletion(ResourceType.SAMPLER, samplerId);
        }
        
        private void setSamplerParameter(int sampler, int pname, int param) {
            OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                .reset(OpenGLESManager.GLESCallType.SAMPLER_PARAMETER_I)
                .addInt(sampler)
                .addInt(pname)
                .addInt(param)
                .seal();
            manager.mapCall(call);
            descriptorPool.release(call);
        }
        
        private void setSamplerParameterf(int sampler, int pname, float param) {
            OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                .reset(OpenGLESManager.GLESCallType.SAMPLER_PARAMETER_F)
                .addInt(sampler)
                .addInt(pname)
                .addFloat(param)
                .seal();
            manager.mapCall(call);
            descriptorPool.release(call);
        }

        // ════════════════════════════════════════════════════════════════════
        // HELPERS
        // ════════════════════════════════════════════════════════════════════

        private void setObjectLabel(int identifier, int name, String label) {
            if (label == null || label.isEmpty()) return;
            
            OpenGLESManager.CallDescriptor call = descriptorPool.acquire()
                .reset(OpenGLESManager.GLESCallType.OBJECT_LABEL)
                .addInt(identifier)
                .addInt(name)
                .addInt(label.length())
                .addObject(label)
                .seal();
            manager.mapCall(call);
            descriptorPool.release(call);
        }
        
        private void validateNotShutdown() {
            if (shutdown) {
                throw new IllegalStateException("ResourceOrchestrator has been shutdown");
            }
        }
        
        void shutdown() {
            shutdown = true;
            LOGGER.info("ResourceOrchestrator shutdown");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // STATE TRACKER
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Tracks OpenGL state to eliminate redundant state changes.
     * 
     * <p>Each setter returns true if the state actually changed (requiring GL call),
     * false if the state was already set (skip GL call).
     */
    public static final class StateTracker {
        
        // Buffer bindings by target
        private final Int2ObjectMap<Integer> bufferBindings = new Int2ObjectOpenHashMap<>();
        
        // Texture bindings: unit -> (target -> textureId)
        private final Int2ObjectMap<Int2ObjectMap<Integer>> textureBindings = new Int2ObjectOpenHashMap<>();
        
        // Sampler bindings: unit -> samplerId
        private final Int2ObjectMap<Integer> samplerBindings = new Int2ObjectOpenHashMap<>();
        
        // Other bindings
        private int currentProgram = 0;
        private int currentVAO = 0;
        private int currentDrawFramebuffer = 0;
        private int currentReadFramebuffer = 0;
        private int activeTextureUnit = 0;
        
        // Render state
        private boolean depthTestEnabled = false;
        private boolean depthWriteEnabled = true;
        private int depthFunc = 0x0201; // GL_LESS
        private boolean blendEnabled = false;
        private int blendSrcRGB = 1, blendDstRGB = 0;
        private int blendSrcAlpha = 1, blendDstAlpha = 0;
        private boolean cullFaceEnabled = false;
        private int cullFaceMode = 0x0405; // GL_BACK
        private int frontFace = 0x0901; // GL_CCW
        
        // Viewport/Scissor
        private int viewportX, viewportY, viewportWidth, viewportHeight;
        private boolean scissorEnabled = false;
        private int scissorX, scissorY, scissorWidth, scissorHeight;
        
        private final StampedLock lock = new StampedLock();
        private final OpenGLESManager.DeviceCapabilities caps;
        
        StateTracker(OpenGLESManager.DeviceCapabilities caps) {
            this.caps = caps;
        }
        
        // ════════════════════════════════════════════════════════════════════
        // BUFFER STATE
        // ════════════════════════════════════════════════════════════════════
        
        /**
         * Sets buffer binding for a target.
         * @return true if state changed, false if already bound
         */
        public boolean setBufferBinding(int target, int bufferId) {
            long stamp = lock.writeLock();
            try {
                Integer current = bufferBindings.get(target);
                if (current != null && current == bufferId) {
                    return false;
                }
                bufferBindings.put(target, bufferId);
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public int getBufferBinding(int target) {
            long stamp = lock.tryOptimisticRead();
            Integer binding = bufferBindings.get(target);
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    binding = bufferBindings.get(target);
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            return binding != null ? binding : 0;
        }
        
        void unbindBuffer(int bufferId) {
            long stamp = lock.writeLock();
            try {
                bufferBindings.int2ObjectEntrySet().removeIf(e -> e.getValue() == bufferId);
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        // ════════════════════════════════════════════════════════════════════
        // TEXTURE STATE
        // ════════════════════════════════════════════════════════════════════
        
        public boolean setActiveTexture(int unit) {
            long stamp = lock.writeLock();
            try {
                if (activeTextureUnit == unit) {
                    return false;
                }
                activeTextureUnit = unit;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setTextureBinding(int unit, int target, int textureId) {
            long stamp = lock.writeLock();
            try {
                Int2ObjectMap<Integer> unitBindings = textureBindings.computeIfAbsent(
                    unit, k -> new Int2ObjectOpenHashMap<>());
                Integer current = unitBindings.get(target);
                if (current != null && current == textureId) {
                    return false;
                }
                unitBindings.put(target, textureId);
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        void unbindTexture(int textureId) {
            long stamp = lock.writeLock();
            try {
                for (Int2ObjectMap<Integer> unitBindings : textureBindings.values()) {
                    unitBindings.int2ObjectEntrySet().removeIf(e -> e.getValue() == textureId);
                }
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        // ════════════════════════════════════════════════════════════════════
        // SAMPLER STATE
        // ════════════════════════════════════════════════════════════════════
        
        public boolean setSamplerBinding(int unit, int samplerId) {
            long stamp = lock.writeLock();
            try {
                Integer current = samplerBindings.get(unit);
                if (current != null && current == samplerId) {
                    return false;
                }
                samplerBindings.put(unit, samplerId);
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        // ════════════════════════════════════════════════════════════════════
        // PROGRAM/VAO/FBO STATE
        // ════════════════════════════════════════════════════════════════════
        
        public boolean setProgramBinding(int programId) {
            long stamp = lock.writeLock();
            try {
                if (currentProgram == programId) {
                    return false;
                }
                currentProgram = programId;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setVertexArrayBinding(int vaoId) {
            long stamp = lock.writeLock();
            try {
                if (currentVAO == vaoId) {
                    return false;
                }
                currentVAO = vaoId;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setFramebufferBinding(int target, int fboId) {
            long stamp = lock.writeLock();
            try {
                if (target == GL_FRAMEBUFFER || target == 0x8CA9) { // GL_DRAW_FRAMEBUFFER
                    if (currentDrawFramebuffer == fboId) {
                        return false;
                    }
                    currentDrawFramebuffer = fboId;
                    if (target == GL_FRAMEBUFFER) {
                        currentReadFramebuffer = fboId;
                    }
                } else if (target == 0x8CA8) { // GL_READ_FRAMEBUFFER
                    if (currentReadFramebuffer == fboId) {
                        return false;
                    }
                    currentReadFramebuffer = fboId;
                }
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        // ════════════════════════════════════════════════════════════════════
        // RENDER STATE
        // ════════════════════════════════════════════════════════════════════
        
        public boolean setDepthTest(boolean enabled) {
            long stamp = lock.writeLock();
            try {
                if (depthTestEnabled == enabled) return false;
                depthTestEnabled = enabled;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setDepthWrite(boolean enabled) {
            long stamp = lock.writeLock();
            try {
                if (depthWriteEnabled == enabled) return false;
                depthWriteEnabled = enabled;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setDepthFunc(int func) {
            long stamp = lock.writeLock();
            try {
                if (depthFunc == func) return false;
                depthFunc = func;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setBlend(boolean enabled) {
            long stamp = lock.writeLock();
            try {
                if (blendEnabled == enabled) return false;
                blendEnabled = enabled;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setBlendFunc(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
            long stamp = lock.writeLock();
            try {
                if (blendSrcRGB == srcRGB && blendDstRGB == dstRGB && 
                    blendSrcAlpha == srcAlpha && blendDstAlpha == dstAlpha) {
                    return false;
                }
                blendSrcRGB = srcRGB;
                blendDstRGB = dstRGB;
                blendSrcAlpha = srcAlpha;
                blendDstAlpha = dstAlpha;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setCullFace(boolean enabled) {
            long stamp = lock.writeLock();
            try {
                if (cullFaceEnabled == enabled) return false;
                cullFaceEnabled = enabled;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setCullFaceMode(int mode) {
            long stamp = lock.writeLock();
            try {
                if (cullFaceMode == mode) return false;
                cullFaceMode = mode;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setFrontFace(int face) {
            long stamp = lock.writeLock();
            try {
                if (frontFace == face) return false;
                frontFace = face;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setViewport(int x, int y, int width, int height) {
            long stamp = lock.writeLock();
            try {
                if (viewportX == x && viewportY == y && 
                    viewportWidth == width && viewportHeight == height) {
                    return false;
                }
                viewportX = x;
                viewportY = y;
                viewportWidth = width;
                viewportHeight = height;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setScissorTest(boolean enabled) {
            long stamp = lock.writeLock();
            try {
                if (scissorEnabled == enabled) return false;
                scissorEnabled = enabled;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        public boolean setScissor(int x, int y, int width, int height) {
            long stamp = lock.writeLock();
            try {
                if (scissorX == x && scissorY == y && 
                    scissorWidth == width && scissorHeight == height) {
                    return false;
                }
                scissorX = x;
                scissorY = y;
                scissorWidth = width;
                scissorHeight = height;
                return true;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        /**
         * Resets all tracked state (call after context loss/reset).
         */
        public void reset() {
            long stamp = lock.writeLock();
            try {
                bufferBindings.clear();
                textureBindings.clear();
                samplerBindings.clear();
                currentProgram = 0;
                currentVAO = 0;
                currentDrawFramebuffer = 0;
                currentReadFramebuffer = 0;
                activeTextureUnit = 0;
                depthTestEnabled = false;
                depthWriteEnabled = true;
                depthFunc = 0x0201;
                blendEnabled = false;
                blendSrcRGB = 1;
                blendDstRGB = 0;
                blendSrcAlpha = 1;
                blendDstAlpha = 0;
                cullFaceEnabled = false;
                cullFaceMode = 0x0405;
                frontFace = 0x0901;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // DEFERRED DELETION QUEUE
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Queue for resources pending deletion after GPU finishes using them.
     */
    private static final class DeferredDeletionQueue {
        
        private final OpenGLESManager manager;
        private final MemorySentinel sentinel;
        
        // Frame index -> list of pending deletions
        private final Long2ObjectMap<ObjectArrayList<PendingDeletion>> pendingByFrame = 
            new Long2ObjectOpenHashMap<>();
        
        private final StampedLock lock = new StampedLock();
        
        record PendingDeletion(ResourceType type, int id) {}
        
        DeferredDeletionQueue(OpenGLESManager manager, MemorySentinel sentinel) {
            this.manager = manager;
            this.sentinel = sentinel;
        }
        
        void queueDeletion(ResourceType type, int id) {
            long currentFrame = OpenGLESPipelineProvider.isInitialized() 
                ? getInstance().currentFrameIndex 
                : 0;
            
            long stamp = lock.writeLock();
            try {
                ObjectArrayList<PendingDeletion> frameList = pendingByFrame.computeIfAbsent(
                    currentFrame, k -> new ObjectArrayList<>());
                frameList.add(new PendingDeletion(type, id));
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        void processCompletedFrame(long frameIndex) {
            ObjectArrayList<PendingDeletion> deletions;
            
            long stamp = lock.writeLock();
            try {
                deletions = pendingByFrame.remove(frameIndex);
            } finally {
                lock.unlockWrite(stamp);
            }
            
            if (deletions != null && !deletions.isEmpty()) {
                for (PendingDeletion deletion : deletions) {
                    executeDelete(deletion.type, deletion.id);
                    sentinel.untrack(deletion.id);
                }
                
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Processed {} deferred deletions for frame {}", 
                        deletions.size(), frameIndex);
                }
            }
        }
        
        void processAll() {
            Long2ObjectMap<ObjectArrayList<PendingDeletion>> allPending;
            
            long stamp = lock.writeLock();
            try {
                allPending = new Long2ObjectOpenHashMap<>(pendingByFrame);
                pendingByFrame.clear();
            } finally {
                lock.unlockWrite(stamp);
            }
            
            int total = 0;
            for (ObjectArrayList<PendingDeletion> frameList : allPending.values()) {
                for (PendingDeletion deletion : frameList) {
                    executeDelete(deletion.type, deletion.id);
                    sentinel.untrack(deletion.id);
                    total++;
                }
            }
            
            if (total > 0) {
                LOGGER.debug("Processed {} total deferred deletions", total);
            }
        }
        
        private void executeDelete(ResourceType type, int id) {
            OpenGLESManager.GLESCallType callType = switch (type) {
                case BUFFER -> OpenGLESManager.GLESCallType.DELETE_BUFFERS;
                case TEXTURE -> OpenGLESManager.GLESCallType.DELETE_TEXTURES;
                case FRAMEBUFFER -> OpenGLESManager.GLESCallType.DELETE_FRAMEBUFFERS;
                case RENDERBUFFER -> OpenGLESManager.GLESCallType.DELETE_RENDERBUFFERS;
                case VERTEX_ARRAY -> OpenGLESManager.GLESCallType.DELETE_VERTEX_ARRAYS;
                case SAMPLER -> OpenGLESManager.GLESCallType.DELETE_SAMPLERS;
                case PROGRAM -> OpenGLESManager.GLESCallType.DELETE_PROGRAM;
                case SHADER -> OpenGLESManager.GLESCallType.DELETE_SHADER;
                case QUERY -> OpenGLESManager.GLESCallType.DELETE_QUERIES;
            };
            
            OpenGLESManager.CallDescriptor call = OpenGLESManager.CallDescriptor.builder()
                .withType(callType)
                .addInt(1)
                .addInt(id)
                .build();
            
            manager.mapCall(call);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // FRAME SCHEDULER
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Manages frame timing, fence synchronization, and in-flight frame tracking.
     */
    private static final class FrameScheduler {
        
        private final OpenGLESManager manager;
        private final Telemetry telemetry;
        private final DeferredDeletionQueue deletionQueue;
        private final int maxFramesInFlight;
        
        // Circular buffer of frame fences
        private final long[] frameFences;
        private final long[] frameIndices;
        private int headIndex = 0;
        private int tailIndex = 0;
        private int inFlightCount = 0;
        
        private final StampedLock lock = new StampedLock();
        
        FrameScheduler(OpenGLESManager manager, Telemetry telemetry, 
                       DeferredDeletionQueue deletionQueue, int maxFramesInFlight) {
            this.manager = manager;
            this.telemetry = telemetry;
            this.deletionQueue = deletionQueue;
            this.maxFramesInFlight = maxFramesInFlight;
            this.frameFences = new long[maxFramesInFlight];
            this.frameIndices = new long[maxFramesInFlight];
        }
        
        void beginFrame(long frameIndex) {
            // If we have max frames in flight, wait for oldest to complete
            if (inFlightCount >= maxFramesInFlight) {
                waitForOldestFrame();
            }
        }
        
        void endFrame(long frameIndex) {
            // Insert fence for this frame
            long fence = createFence();
            
            long stamp = lock.writeLock();
            try {
                frameFences[headIndex] = fence;
                frameIndices[headIndex] = frameIndex;
                headIndex = (headIndex + 1) % maxFramesInFlight;
                inFlightCount++;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        private void waitForOldestFrame() {
            long fence;
            long frameIndex;
            
            long stamp = lock.writeLock();
            try {
                if (inFlightCount == 0) return;
                
                fence = frameFences[tailIndex];
                frameIndex = frameIndices[tailIndex];
            } finally {
                lock.unlockWrite(stamp);
            }
            
            // Wait for fence
            waitForFence(fence);
            deleteFence(fence);
            
            // Process deferred deletions for completed frame
            deletionQueue.processCompletedFrame(frameIndex);
            
            stamp = lock.writeLock();
            try {
                tailIndex = (tailIndex + 1) % maxFramesInFlight;
                inFlightCount--;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
        
        void waitForAllFrames() {
            while (inFlightCount > 0) {
                waitForOldestFrame();
            }
            
            // Also issue glFinish for good measure
            manager.mapCall(OpenGLESManager.CallDescriptor.builder()
                .withType(OpenGLESManager.GLESCallType.FINISH)
                .build());
        }
        
        private long createFence() {
            OpenGLESManager.CallDescriptor call = OpenGLESManager.CallDescriptor.builder()
                .withType(OpenGLESManager.GLESCallType.FENCE_SYNC)
                .addInt(GL_SYNC_GPU_COMMANDS_COMPLETE)
                .addInt(0)
                .build();
            
            OpenGLESManager.MappingResult result = manager.mapCall(call);
            return result.nativeResultLong();
        }
        
        private void waitForFence(long fence) {
            if (fence == 0) return;
            
            OpenGLESManager.CallDescriptor call = OpenGLESManager.CallDescriptor.builder()
                .withType(OpenGLESManager.GLESCallType.CLIENT_WAIT_SYNC)
                .addLong(fence)
                .addInt(GL_SYNC_FLUSH_COMMANDS_BIT)
                .addLong(GL_TIMEOUT_IGNORED)
                .build();
            
            manager.mapCall(call);
        }
        
        private void deleteFence(long fence) {
            if (fence == 0) return;
            
            OpenGLESManager.CallDescriptor call = OpenGLESManager.CallDescriptor.builder()
                .withType(OpenGLESManager.GLESCallType.DELETE_SYNC)
                .addLong(fence)
                .build();
            
            manager.mapCall(call);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MEMORY SENTINEL
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Tracks GPU memory allocations and detects resource leaks.
     */
    private static final class MemorySentinel {
        
        private final Int2ObjectMap<AllocationRecord> allocations = new Int2ObjectOpenHashMap<>();
        private final LongAdder totalBytesAllocated = new LongAdder();
        private final LongAdder currentAllocationCount = new LongAdder();
        private final LongAdder peakAllocationCount = new LongAdder();
        private final LongAdder peakBytesAllocated = new LongAdder();
        private final Telemetry telemetry;
        private final StampedLock lock = new StampedLock();
        
        record AllocationRecord(
            String name, 
            long size, 
            long timestamp, 
            ResourceType type,
            StackTraceElement[] allocationSite
        ) {}
        
        MemorySentinel(Telemetry telemetry) {
            this.telemetry = telemetry;
        }
        
        void track(int id, long size, String name, ResourceType type) {
            if (id == 0) return;
            
            StackTraceElement[] site = LOGGER.isDebugEnabled() 
                ? Thread.currentThread().getStackTrace() 
                : null;
            
            AllocationRecord record = new AllocationRecord(
                name, size, System.currentTimeMillis(), type, site);
            
            long stamp = lock.writeLock();
            try {
                allocations.put(id, record);
            } finally {
                lock.unlockWrite(stamp);
            }
            
            totalBytesAllocated.add(size);
            currentAllocationCount.increment();
            
            // Track peaks
            long currentCount = currentAllocationCount.sum();
            long currentPeak = peakAllocationCount.sum();
            if (currentCount > currentPeak) {
                peakAllocationCount.reset();
                peakAllocationCount.add(currentCount);
            }
            
            long currentBytes = totalBytesAllocated.sum();
            long peakBytes = peakBytesAllocated.sum();
            if (currentBytes > peakBytes) {
                peakBytesAllocated.reset();
                peakBytesAllocated.add(currentBytes);
            }
            
            telemetry.recordAllocation(size);
        }
        
        void untrack(int id) {
            if (id == 0) return;
            
            AllocationRecord record;
            long stamp = lock.writeLock();
            try {
                record = allocations.remove(id);
            } finally {
                lock.unlockWrite(stamp);
            }
            
            if (record != null) {
                totalBytesAllocated.add(-record.size());
                currentAllocationCount.decrement();
                telemetry.recordDeallocation(record.size());
            }
        }
        
        void reportLeaks() {
            Int2ObjectMap<AllocationRecord> leaked;
            
            long stamp = lock.readLock();
            try {
                if (allocations.isEmpty()) {
                    LOGGER.info("╔══════════════════════════════════════════════════════════════╗");
                    LOGGER.info("║         MemorySentinel: No resource leaks detected          ║");
                    LOGGER.info("╚══════════════════════════════════════════════════════════════╝");
                    return;
                }
                leaked = new Int2ObjectOpenHashMap<>(allocations);
            } finally {
                lock.unlockRead(stamp);
            }
            
            long totalLeakedBytes = 0;
            for (AllocationRecord record : leaked.values()) {
                totalLeakedBytes += record.size();
            }
            
            LOGGER.error("╔══════════════════════════════════════════════════════════════╗");
            LOGGER.error("║              !!! GLES MEMORY LEAK DETECTED !!!               ║");
            LOGGER.error("╠══════════════════════════════════════════════════════════════╣");
            LOGGER.error("║  Leaked Resources: {}", padRight(String.valueOf(leaked.size()), 39) + "║");
            LOGGER.error("║  Leaked Memory: {}", padRight(formatBytes(totalLeakedBytes), 42) + "║");
            LOGGER.error("╠══════════════════════════════════════════════════════════════╣");
            
            for (Int2ObjectMap.Entry<AllocationRecord> entry : leaked.int2ObjectEntrySet()) {
                AllocationRecord record = entry.getValue();
                LOGGER.error("║  [{} ID:{}] '{}' ({} bytes)", 
                    record.type(), entry.getIntKey(), record.name(), record.size());
                
                if (record.allocationSite() != null && record.allocationSite().length > 3) {
                    // Skip first 3 frames (getStackTrace, track, createXXX)
                    for (int i = 3; i < Math.min(record.allocationSite().length, 6); i++) {
                        LOGGER.error("║      at {}", record.allocationSite()[i]);
                    }
                }
            }
            
            LOGGER.error("╚══════════════════════════════════════════════════════════════╝");
        }
        
        private static String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CALL DESCRIPTOR POOL
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Object pool for CallDescriptors to eliminate allocations in hot paths.
     */
    private static final class CallDescriptorPool {
        
        private final ConcurrentLinkedQueue<OpenGLESManager.CallDescriptor.Builder> pool;
        private final int maxSize;
        private final LongAdder acquireCount = new LongAdder();
        private final LongAdder releaseCount = new LongAdder();
        private final LongAdder missCount = new LongAdder();
        
        CallDescriptorPool(int maxSize) {
            this.maxSize = maxSize;
            this.pool = new ConcurrentLinkedQueue<>();
            
            // Pre-populate pool
            for (int i = 0; i < maxSize / 2; i++) {
                pool.offer(OpenGLESManager.CallDescriptor.builder());
            }
        }
        
        OpenGLESManager.CallDescriptor.Builder acquire() {
            acquireCount.increment();
            
            OpenGLESManager.CallDescriptor.Builder builder = pool.poll();
            if (builder != null) {
                return builder;
            }
            
            missCount.increment();
            return OpenGLESManager.CallDescriptor.builder();
        }
        
        void release(OpenGLESManager.CallDescriptor descriptor) {
            releaseCount.increment();
            
            // Return builder to pool if under capacity
            if (pool.size() < maxSize) {
                pool.offer(OpenGLESManager.CallDescriptor.builder());
            }
        }
        
        double getHitRate() {
            long acquires = acquireCount.sum();
            long misses = missCount.sum();
            return acquires > 0 ? ((acquires - misses) * 100.0 / acquires) : 100.0;
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TELEMETRY
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Comprehensive telemetry for performance monitoring.
     */
    public static final class Telemetry {
        
        // Frame metrics
        private final LongAdder totalFrames = new LongAdder();
        private volatile long currentFrameIndex = 0;
        private volatile long frameStartTimeNs = 0;
        
        // Memory metrics
        private final LongAdder totalBytesAllocated = new LongAdder();
        private final LongAdder totalBytesDeallocated = new LongAdder();
        private final LongAdder allocationCount = new LongAdder();
        private final LongAdder deallocationCount = new LongAdder();
        
        // Submission metrics
        private final LongAdder totalSubmissions = new LongAdder();
        private final LongAdder frameSubmissions = new LongAdder();
        private final LongAdder totalSubmitTimeNs = new LongAdder();
        
        // Frame time tracking (rolling window)
        private static final int FRAME_TIME_WINDOW = 120;
        private final long[] frameTimesNs = new long[FRAME_TIME_WINDOW];
        private int frameTimeIndex = 0;
        
        // State change metrics
        private final LongAdder stateChanges = new LongAdder();
        private final LongAdder redundantStateChangesAvoided = new LongAdder();
        
        void beginFrame(long frameIndex) {
            currentFrameIndex = frameIndex;
            frameStartTimeNs = System.nanoTime();
            frameSubmissions.reset();
        }
        
        void endFrame() {
            long frameTimeNs = System.nanoTime() - frameStartTimeNs;
            
            // Store in rolling window
            frameTimesNs[frameTimeIndex] = frameTimeNs;
            frameTimeIndex = (frameTimeIndex + 1) % FRAME_TIME_WINDOW;
            
            totalFrames.increment();
        }
        
        void recordAllocation(long bytes) {
            totalBytesAllocated.add(bytes);
            allocationCount.increment();
        }
        
        void recordDeallocation(long bytes) {
            totalBytesDeallocated.add(bytes);
            deallocationCount.increment();
        }
        
        void recordSubmission(long durationNs) {
            totalSubmissions.increment();
            frameSubmissions.increment();
            totalSubmitTimeNs.add(durationNs);
        }
        
        void recordStateChange() {
            stateChanges.increment();
        }
        
        void recordRedundantStateChangeAvoided() {
            redundantStateChangesAvoided.increment();
        }
        
        // ════════════════════════════════════════════════════════════════════
        // PUBLIC GETTERS
        // ════════════════════════════════════════════════════════════════════
        
        public long getTotalFrames() {
            return totalFrames.sum();
        }
        
        public long getCurrentFrameIndex() {
            return currentFrameIndex;
        }
        
        public long getNetMemoryAllocated() {
            return totalBytesAllocated.sum() - totalBytesDeallocated.sum();
        }
        
        public long getTotalAllocations() {
            return allocationCount.sum();
        }
        
        public long getTotalSubmissions() {
            return totalSubmissions.sum();
        }
        
        public double getAverageFrameTimeMs() {
            long sum = 0;
            int count = 0;
            for (long timeNs : frameTimesNs) {
                if (timeNs > 0) {
                    sum += timeNs;
                    count++;
                }
            }
            return count > 0 ? (sum / 1_000_000.0 / count) : 0.0;
        }
        
        public double getAverageFPS() {
            double avgFrameTimeMs = getAverageFrameTimeMs();
            return avgFrameTimeMs > 0 ? (1000.0 / avgFrameTimeMs) : 0.0;
        }
        
        public double getStateChangeEfficiency() {
            long total = stateChanges.sum() + redundantStateChangesAvoided.sum();
            return total > 0 ? (redundantStateChangesAvoided.sum() * 100.0 / total) : 0.0;
        }
        
        public void logFinalReport() {
            LOGGER.info("╔══════════════════════════════════════════════════════════════╗");
            LOGGER.info("║                   Final Telemetry Report                     ║");
            LOGGER.info("╠══════════════════════════════════════════════════════════════╣");
            LOGGER.info("║  Total Frames: {}", padRight(String.valueOf(totalFrames.sum()), 43) + "║");
            LOGGER.info("║  Average FPS: {}", padRight(String.format("%.1f", getAverageFPS()), 44) + "║");
            LOGGER.info("║  Avg Frame Time: {}", padRight(String.format("%.2f ms", getAverageFrameTimeMs()), 41) + "║");
            LOGGER.info("╠══════════════════════════════════════════════════════════════╣");
            LOGGER.info("║  Total Submissions: {}", padRight(String.valueOf(totalSubmissions.sum()), 38) + "║");
            LOGGER.info("║  Total Allocations: {}", padRight(String.valueOf(allocationCount.sum()), 38) + "║");
            LOGGER.info("║  Net Memory: {}", padRight(formatBytes(getNetMemoryAllocated()), 45) + "║");
            LOGGER.info("╠══════════════════════════════════════════════════════════════╣");
            LOGGER.info("║  State Changes: {}", padRight(String.valueOf(stateChanges.sum()), 42) + "║");
            LOGGER.info("║  Redundant Avoided: {}", padRight(String.valueOf(redundantStateChangesAvoided.sum()), 38) + "║");
            LOGGER.info("║  Efficiency: {}", padRight(String.format("%.1f%%", getStateChangeEfficiency()), 45) + "║");
            LOGGER.info("╚══════════════════════════════════════════════════════════════╝");
        }
        
        private static String formatBytes(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // PUBLIC TYPES
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * Resource types for tracking and deletion.
     */
    public enum ResourceType {
        BUFFER,
        TEXTURE,
        FRAMEBUFFER,
        RENDERBUFFER,
        VERTEX_ARRAY,
        SAMPLER,
        PROGRAM,
        SHADER,
        QUERY
    }

    /**
     * Sampler configuration.
     */
    public record SamplerConfig(
        int minFilter,
        int magFilter,
        int wrapS,
        int wrapT,
        int wrapR,
        float anisotropy
    ) {
        public static SamplerConfig linear() {
            return new SamplerConfig(0x2601, 0x2601, 0x812F, 0x812F, 0x812F, 1.0f);
        }
        
        public static SamplerConfig linearMipmap() {
            return new SamplerConfig(0x2703, 0x2601, 0x812F, 0x812F, 0x812F, 1.0f);
        }
        
        public static SamplerConfig nearest() {
            return new SamplerConfig(0x2600, 0x2600, 0x812F, 0x812F, 0x812F, 1.0f);
        }
        
        public static SamplerConfig anisotropic(float anisotropy) {
            return new SamplerConfig(0x2703, 0x2601, 0x2901, 0x2901, 0x2901, anisotropy);
        }
    }

    /**
     * Immutable configuration for the pipeline provider.
     */
    public record Configuration(
        OpenGLESManager.DeviceCapabilities capabilities,
        int commandPoolSize,
        int descriptorPoolSize,
        int maxFramesInFlight,
        OpenGLESManager.DebugCallback debugCallback
    ) {
        public Configuration {
            if (commandPoolSize <= 0) commandPoolSize = DEFAULT_COMMAND_POOL_SIZE;
            if (descriptorPoolSize <= 0) descriptorPoolSize = 256;
            if (maxFramesInFlight <= 0 || maxFramesInFlight > 4) maxFramesInFlight = MAX_FRAMES_IN_FLIGHT;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static final class Builder {
            private OpenGLESManager.DeviceCapabilities capabilities;
            private int commandPoolSize = DEFAULT_COMMAND_POOL_SIZE;
            private int descriptorPoolSize = 256;
            private int maxFramesInFlight = MAX_FRAMES_IN_FLIGHT;
            private OpenGLESManager.DebugCallback debugCallback;
            
            public Builder capabilities(OpenGLESManager.DeviceCapabilities caps) {
                this.capabilities = caps;
                return this;
            }
            
            public Builder commandPoolSize(int size) {
                this.commandPoolSize = size;
                return this;
            }
            
            public Builder descriptorPoolSize(int size) {
                this.descriptorPoolSize = size;
                return this;
            }
            
            public Builder maxFramesInFlight(int count) {
                this.maxFramesInFlight = count;
                return this;
            }
            
            public Builder debugCallback(OpenGLESManager.DebugCallback callback) {
                this.debugCallback = callback;
                return this;
            }
            
            public Configuration build() {
                return new Configuration(
                    capabilities, commandPoolSize, descriptorPoolSize, 
                    maxFramesInFlight, debugCallback
                );
            }
        }
    }
}
