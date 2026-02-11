package stellar.snow.astralis.bridge;

import stellar.snow.astralis.ecs.Entity;
import stellar.snow.astralis.ecs.World;
import stellar.snow.astralis.ecs.SystemScheduler;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MinecraftECSBridge - Ultra high-performance bridge between Minecraft OOP entities
 * and a Data-Oriented ECS framework.
 *
 * <h2>Architecture Highlights:</h2>
 * <ul>
 *   <li><b>Zero-Allocation Steady State:</b> No GC pressure during normal operation</li>
 *   <li><b>Cache-Optimized Layout:</b> Contiguous memory with cache-line alignment</li>
 *   <li><b>Lock-Free Registration:</b> CAS-based entity lifecycle management</li>
 *   <li><b>SIMD Batch Processing:</b> Vector API for bulk transforms</li>
 *   <li><b>LWJGL Native Interop:</b> Direct buffer sharing with rendering</li>
 *   <li><b>Structured Concurrency:</b> Java 25 virtual thread orchestration</li>
 * </ul>
 *
 * <h2>Memory Layout (per entity = 256 bytes, cache-line aligned):</h2>
 * <pre>
 * [0-63]    Transform (current): x, y, z (f64), yaw, pitch, roll (f32), flags (i32), _pad
 * [64-127]  Transform (previous): same layout for interpolation
 * [128-159] Velocity: vx, vy, vz (f64), speed (f32), _pad
 * [160-191] Acceleration: ax, ay, az (f64), _pad
 * [192-223] Metadata: mcEntityId (i32), ecsEntityId (i32), flags (i64), lastSyncTick (i64)
 * [224-255] Reserved/User Data
 * </pre>
 *
 * @author Astralis Team
 * @version 5.0.0-J25-PERF
 */
public final class MinecraftECSBridge implements AutoCloseable {

    // ========================================================================
    // LOGGING
    // ========================================================================

    private static final Logger LOGGER = Logger.getLogger(MinecraftECSBridge.class.getName());

    // ========================================================================
    // CONFIGURATION CONSTANTS
    // ========================================================================

    /** Maximum supported entities - power of 2 for fast modulo */
    public static final int MAX_ENTITIES = 1 << 17; // 131,072

    /** Mask for fast modulo: slot & SLOT_MASK == slot % MAX_ENTITIES */
    private static final int SLOT_MASK = MAX_ENTITIES - 1;

    /** Size of hash table for open addressing (2x entities for load factor ~0.5) */
    private static final int HASH_TABLE_SIZE = MAX_ENTITIES << 1;
    private static final int HASH_TABLE_MASK = HASH_TABLE_SIZE - 1;

    /** Cache line size for alignment */
    private static final int CACHE_LINE_BYTES = 64;

    /** Entity memory block size (must be multiple of cache line) */
    public static final int ENTITY_BLOCK_SIZE = 256;

    /** Maximum probes for open-addressed hash table */
    private static final int MAX_HASH_PROBES = 32;

    // ========================================================================
    // ENTITY SLOT STATES
    // ========================================================================

    private static final int SLOT_FREE = 0;
    private static final int SLOT_ALLOCATING = 1;
    private static final int SLOT_ACTIVE = 2;
    private static final int SLOT_REMOVING = 3;

    // ========================================================================
    // BRIDGE STATES
    // ========================================================================

    private static final int STATE_UNINITIALIZED = 0;
    private static final int STATE_INITIALIZING = 1;
    private static final int STATE_RUNNING = 2;
    private static final int STATE_PAUSED = 3;
    private static final int STATE_SHUTTING_DOWN = 4;
    private static final int STATE_TERMINATED = 5;

    // ========================================================================
    // VARHANDLE ACCESS (For volatile field operations)
    // ========================================================================

    private static final VarHandle STATE_HANDLE;
    private static final VarHandle TICK_COUNT_HANDLE;
    private static final VarHandle LAST_TICK_NANOS_HANDLE;
    private static final VarHandle ACTIVE_COUNT_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            STATE_HANDLE = lookup.findVarHandle(MinecraftECSBridge.class, "state", int.class);
            TICK_COUNT_HANDLE = lookup.findVarHandle(MinecraftECSBridge.class, "tickCount", long.class);
            LAST_TICK_NANOS_HANDLE = lookup.findVarHandle(MinecraftECSBridge.class, "lastTickNanos", long.class);
            ACTIVE_COUNT_HANDLE = lookup.findVarHandle(MinecraftECSBridge.class, "activeEntityCount", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // ========================================================================
    // SINGLETON
    // ========================================================================

    private static volatile MinecraftECSBridge instance;
    private static final Object INSTANCE_LOCK = new Object();

    /**
     * Gets the singleton instance, creating it if necessary.
     * Uses double-checked locking with volatile for thread safety.
     */
    public static MinecraftECSBridge getInstance() {
        MinecraftECSBridge local = instance;
        if (local == null) {
            synchronized (INSTANCE_LOCK) {
                local = instance;
                if (local == null) {
                    instance = local = new MinecraftECSBridge();
                }
            }
        }
        return local;
    }

    // ========================================================================
    // CACHE-LINE PADDED STATE FIELDS
    // ========================================================================

    // Padding before hot fields
    @SuppressWarnings("unused")
    private long p00, p01, p02, p03, p04, p05, p06, p07;

    /** Bridge state machine */
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int state = STATE_UNINITIALIZED;

    /** Monotonic tick counter */
    @SuppressWarnings("FieldMayBeFinal")
    private volatile long tickCount = 0L;

    /** Last tick timestamp in nanoseconds */
    @SuppressWarnings("FieldMayBeFinal")
    private volatile long lastTickNanos = 0L;

    /** Number of active entities */
    @SuppressWarnings("FieldMayBeFinal")
    private volatile int activeEntityCount = 0;

    // Padding after hot fields
    @SuppressWarnings("unused")
    private long p10, p11, p12, p13, p14, p15, p16, p17;

    // ========================================================================
    // MEMORY MANAGEMENT
    // ========================================================================

    /** Main component memory arena (shared for concurrent access) */
    private final Arena componentArena;

    /** Contiguous memory block for all entity components */
    private final MemorySegment componentMemory;

    /** Native pointer for LWJGL interop */
    private final long componentMemoryAddress;

    /** LWJGL-allocated buffer for GPU uploads */
    private final long gpuStagingBuffer;
    private final int gpuStagingBufferSize;

    // ========================================================================
    // ENTITY TRACKING (Lock-Free Structures)
    // ========================================================================

    /** Slot state array for CAS-based lifecycle management */
    private final AtomicInteger[] slotStates;

    /** Open-addressed hash table: hash -> slot mapping */
    private final int[] hashToSlot;

    /** Reverse mapping: slot -> MC entity ID */
    private final int[] slotToMcId;

    /** Direct references to MC entities (cleaned explicitly) */
    private final net.minecraft.entity.Entity[] mcEntities;

    /** ECS entity handles */
    private final Entity[] ecsEntities;

    /** Lock-free free-list implemented as a stack */
    private final int[] freeStack;
    private final AtomicInteger freeStackTop;

    /** Generation counters for ABA prevention */
    private final int[] slotGenerations;

    // ========================================================================
    // SUBSYSTEMS
    // ========================================================================

    /** ECS World reference */
    private volatile World ecsWorld;

    /** Circuit breaker for fault tolerance */
    private final CircuitBreaker circuitBreaker;

    /** Batch processor for SIMD operations */
    private final BatchProcessor batchProcessor;

    /** Performance metrics collector */
    private final BridgeMetrics metrics;

    /** Thread-local batch buffers */
    private static final ThreadLocal<int[]> BATCH_BUFFER = ThreadLocal.withInitial(() -> new int[4096]);
    private static final ThreadLocal<double[]> INTERPOLATION_OUT = ThreadLocal.withInitial(() -> new double[6]);

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    private MinecraftECSBridge() {
        LOGGER.info("[ECS-Bridge] Allocating memory structures...");

        // 1. Allocate main component memory (off-heap, page-aligned)
        this.componentArena = Arena.ofShared();
        long totalSize = (long) MAX_ENTITIES * ENTITY_BLOCK_SIZE;
        this.componentMemory = componentArena.allocate(totalSize, CACHE_LINE_BYTES);
        this.componentMemoryAddress = componentMemory.address();

        // Zero-initialize memory
        componentMemory.fill((byte) 0);

        // 2. Allocate GPU staging buffer via LWJGL for render uploads
        this.gpuStagingBufferSize = MAX_ENTITIES * BridgeComponents.GPU_TRANSFORM_SIZE;
        this.gpuStagingBuffer = MemoryUtil.nmemAlignedAlloc(CACHE_LINE_BYTES, gpuStagingBufferSize);
        if (gpuStagingBuffer == MemoryUtil.NULL) {
            componentArena.close();
            throw new OutOfMemoryError("Failed to allocate GPU staging buffer");
        }

        // 3. Initialize slot management arrays
        this.slotStates = new AtomicInteger[MAX_ENTITIES];
        this.slotGenerations = new int[MAX_ENTITIES];
        for (int i = 0; i < MAX_ENTITIES; i++) {
            slotStates[i] = new AtomicInteger(SLOT_FREE);
            slotGenerations[i] = 0;
        }

        // 4. Initialize hash table
        this.hashToSlot = new int[HASH_TABLE_SIZE];
        java.util.Arrays.fill(hashToSlot, -1);

        // 5. Initialize reverse mapping
        this.slotToMcId = new int[MAX_ENTITIES];
        java.util.Arrays.fill(slotToMcId, -1);

        // 6. Initialize entity reference arrays
        this.mcEntities = new net.minecraft.entity.Entity[MAX_ENTITIES];
        this.ecsEntities = new Entity[MAX_ENTITIES];

        // 7. Initialize free stack (filled with all slots in reverse order)
        this.freeStack = new int[MAX_ENTITIES];
        for (int i = 0; i < MAX_ENTITIES; i++) {
            freeStack[i] = MAX_ENTITIES - 1 - i;
        }
        this.freeStackTop = new AtomicInteger(MAX_ENTITIES);

        // 8. Initialize subsystems
        this.circuitBreaker = new CircuitBreaker(5, 30_000L, "ECS-Bridge-Main");
        this.batchProcessor = new BatchProcessor(componentMemory, ENTITY_BLOCK_SIZE);
        this.metrics = new BridgeMetrics();

        // 9. Register shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread.ofVirtual().unstarted(this::shutdownHook));

        LOGGER.info("[ECS-Bridge] Memory allocation complete. Capacity: " + MAX_ENTITIES + " entities");
    }

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    /**
     * Initializes the bridge with an ECS world.
     * Must be called from the main thread during mod initialization.
     *
     * @param worldConfig configuration for the ECS world
     * @throws IllegalStateException if already initialized
     */
    public void initialize(World.Config worldConfig) {
        if (!STATE_HANDLE.compareAndSet(this, STATE_UNINITIALIZED, STATE_INITIALIZING)) {
            int current = (int) STATE_HANDLE.get(this);
            if (current >= STATE_RUNNING && current <= STATE_PAUSED) {
                LOGGER.fine("[ECS-Bridge] Already initialized, skipping");
                return;
            }
            throw new IllegalStateException("Cannot initialize from state: " + stateToString(current));
        }

        try {
            LOGGER.info("[ECS-Bridge] Initializing ECS World...");

            // Create ECS world
            this.ecsWorld = new World(worldConfig);

            // Register core systems
            registerCoreSystems();

            // Initialize the world
            ecsWorld.initialize();

            // Record start time
            LAST_TICK_NANOS_HANDLE.setVolatile(this, System.nanoTime());

            // Transition to running
            STATE_HANDLE.setVolatile(this, STATE_RUNNING);

            LOGGER.info("[ECS-Bridge] Initialization complete");

        } catch (Exception e) {
            STATE_HANDLE.setVolatile(this, STATE_TERMINATED);
            LOGGER.log(Level.SEVERE, "[ECS-Bridge] Initialization failed", e);
            throw new RuntimeException("ECS Bridge initialization failed", e);
        }
    }

    /**
     * Convenience method to initialize with default configuration.
     */
    public void initialize() {
        World.Config config = World.Config.builder("MC-Client-ECS")
                .maxEntities(MAX_ENTITIES)
                .parallelism(Math.max(2, Runtime.getRuntime().availableProcessors() - 1))
                .useVirtualThreads(true)
                .useOffHeapStorage(true)
                .enableChangeDetection(true)
                .build();
        initialize(config);
    }

    private void registerCoreSystems() {
        // PRE_UPDATE: Sync MC -> ECS
        ecsWorld.registerSystem(
                new SyncSystems.InboundSyncSystem(this),
                SystemScheduler.Stage.PRE_UPDATE
        );

        // POST_UPDATE: Sync ECS -> MC
        ecsWorld.registerSystem(
                new SyncSystems.OutboundSyncSystem(this),
                SystemScheduler.Stage.POST_UPDATE
        );

        // RENDER: Interpolation
        ecsWorld.registerSystem(
                new InterpolationSystem(this),
                SystemScheduler.Stage.RENDER
        );
    }

    // ========================================================================
    // ENTITY REGISTRATION (Lock-Free)
    // ========================================================================

    /**
     * Registers a Minecraft entity with the bridge.
     * Thread-safe, lock-free implementation using CAS.
     *
     * @param mcEntity the Minecraft entity to register
     * @return the allocated slot index, or -1 on failure
     */
    public int registerEntity(net.minecraft.entity.Entity mcEntity) {
        Objects.requireNonNull(mcEntity, "mcEntity cannot be null");

        if (!isRunning()) return -1;

        int mcId = mcEntity.getEntityId();

        // Fast path: check if already registered
        int existingSlot = lookupSlotByMcId(mcId);
        if (existingSlot >= 0) {
            return existingSlot;
        }

        // Allocate a slot from the free stack
        int slot = popFreeSlot();
        if (slot < 0) {
            LOGGER.warning("[ECS-Bridge] Entity registration failed: no free slots");
            metrics.recordRegistrationFailure();
            return -1;
        }

        // Try to transition slot from FREE to ALLOCATING
        if (!slotStates[slot].compareAndSet(SLOT_FREE, SLOT_ALLOCATING)) {
            // Lost race, return slot and retry
            pushFreeSlot(slot);
            return registerEntity(mcEntity); // Tail call
        }

        try {
            // Increment generation for ABA prevention
            slotGenerations[slot]++;

            // Store mappings
            slotToMcId[slot] = mcId;
            mcEntities[slot] = mcEntity;

            // Create ECS entity
            ecsEntities[slot] = ecsWorld.createEntity();

            // Initialize component memory
            initializeEntityMemory(slot, mcEntity);

            // Register in hash table
            if (!registerInHashTable(mcId, slot)) {
                throw new IllegalStateException("Hash table registration failed");
            }

            // Link MC entity via mixin
            if (mcEntity instanceof BridgeMixinInterface ext) {
                ext.astralis$setBridgeSlot(slot);
                ext.astralis$setEcsEntity(ecsEntities[slot]);
            }

            // Transition to ACTIVE
            slotStates[slot].set(SLOT_ACTIVE);

            // Update active count
            ACTIVE_COUNT_HANDLE.getAndAdd(this, 1);
            metrics.recordRegistration();

            return slot;

        } catch (Exception e) {
            // Rollback on failure
            slotToMcId[slot] = -1;
            mcEntities[slot] = null;
            if (ecsEntities[slot] != null) {
                ecsWorld.destroyEntity(ecsEntities[slot]);
                ecsEntities[slot] = null;
            }
            slotStates[slot].set(SLOT_FREE);
            pushFreeSlot(slot);

            LOGGER.log(Level.WARNING, "[ECS-Bridge] Entity registration failed", e);
            metrics.recordRegistrationFailure();
            return -1;
        }
    }

    /**
     * Unregisters an entity by slot index.
     * Thread-safe, lock-free implementation.
     *
     * @param slot the slot to unregister
     */
    public void unregisterEntity(int slot) {
        if (slot < 0 || slot >= MAX_ENTITIES) return;

        // Transition from ACTIVE to REMOVING
        if (!slotStates[slot].compareAndSet(SLOT_ACTIVE, SLOT_REMOVING)) {
            return; // Already being removed or not active
        }

        int mcId = slotToMcId[slot];

        // Clear MC entity reference
        net.minecraft.entity.Entity mcEntity = mcEntities[slot];
        mcEntities[slot] = null;

        if (mcEntity instanceof BridgeMixinInterface ext) {
            ext.astralis$setBridgeSlot(-1);
            ext.astralis$setEcsEntity(null);
        }

        // Destroy ECS entity
        Entity ecsEntity = ecsEntities[slot];
        ecsEntities[slot] = null;
        if (ecsEntity != null && ecsWorld != null) {
            ecsWorld.destroyEntity(ecsEntity);
        }

        // Clear hash table entry
        unregisterFromHashTable(mcId);

        // Clear slot data
        slotToMcId[slot] = -1;

        // Clear component memory
        clearEntityMemory(slot);

        // Transition to FREE and return to pool
        slotStates[slot].set(SLOT_FREE);
        pushFreeSlot(slot);

        // Update active count
        ACTIVE_COUNT_HANDLE.getAndAdd(this, -1);
        metrics.recordUnregistration();
    }

    /**
     * Unregisters an entity by MC entity reference.
     */
    public void unregisterEntity(net.minecraft.entity.Entity mcEntity) {
        if (mcEntity == null) return;

        // Fast path via mixin
        if (mcEntity instanceof BridgeMixinInterface ext) {
            int slot = ext.astralis$getBridgeSlot();
            if (slot >= 0) {
                unregisterEntity(slot);
                return;
            }
        }

        // Slow path: lookup by ID
        int slot = lookupSlotByMcId(mcEntity.getEntityId());
        if (slot >= 0) {
            unregisterEntity(slot);
        }
    }

    // ========================================================================
    // FREE STACK OPERATIONS (Lock-Free)
    // ========================================================================

    private int popFreeSlot() {
        int top, slot;
        do {
            top = freeStackTop.get();
            if (top <= 0) return -1;
            slot = freeStack[top - 1];
        } while (!freeStackTop.compareAndSet(top, top - 1));
        return slot;
    }

    private void pushFreeSlot(int slot) {
        int top;
        do {
            top = freeStackTop.get();
            if (top >= MAX_ENTITIES) return; // Stack full (shouldn't happen)
        } while (!freeStackTop.compareAndSet(top, top + 1));
        freeStack[top] = slot;
    }

    // ========================================================================
    // HASH TABLE OPERATIONS (Open Addressing with Linear Probing)
    // ========================================================================

    /**
     * Fast hash function using Murmur3 finalizer.
     */
    private static int hashMcId(int mcId) {
        int h = mcId;
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    /**
     * Looks up the slot index for a given MC entity ID.
     *
     * @param mcId the Minecraft entity ID
     * @return slot index, or -1 if not found
     */
    public int lookupSlotByMcId(int mcId) {
        int hash = hashMcId(mcId);
        int idx = hash & HASH_TABLE_MASK;

        for (int probe = 0; probe < MAX_HASH_PROBES; probe++) {
            int slot = hashToSlot[idx];

            if (slot < 0) {
                return -1; // Empty slot = not found
            }

            if (slotToMcId[slot] == mcId && slotStates[slot].get() == SLOT_ACTIVE) {
                return slot;
            }

            idx = (idx + 1) & HASH_TABLE_MASK;
        }

        return -1; // Not found after max probes
    }

    private boolean registerInHashTable(int mcId, int slot) {
        int hash = hashMcId(mcId);
        int idx = hash & HASH_TABLE_MASK;

        for (int probe = 0; probe < MAX_HASH_PROBES; probe++) {
            int existing = hashToSlot[idx];

            if (existing < 0 || slotStates[existing].get() != SLOT_ACTIVE) {
                hashToSlot[idx] = slot;
                return true;
            }

            idx = (idx + 1) & HASH_TABLE_MASK;
        }

        return false; // Table full in this region
    }

    private void unregisterFromHashTable(int mcId) {
        int hash = hashMcId(mcId);
        int idx = hash & HASH_TABLE_MASK;

        for (int probe = 0; probe < MAX_HASH_PROBES; probe++) {
            int slot = hashToSlot[idx];

            if (slot < 0) return;

            if (slotToMcId[slot] == mcId) {
                hashToSlot[idx] = -1;
                return;
            }

            idx = (idx + 1) & HASH_TABLE_MASK;
        }
    }

    // ========================================================================
    // COMPONENT MEMORY ACCESS
    // ========================================================================

    /**
     * Returns the base memory offset for an entity's component block.
     */
    public long getEntityMemoryOffset(int slot) {
        return (long) slot * ENTITY_BLOCK_SIZE;
    }

    /**
     * Returns a slice of the component memory for a specific entity.
     */
    public MemorySegment getEntityMemorySegment(int slot) {
        long offset = getEntityMemoryOffset(slot);
        return componentMemory.asSlice(offset, ENTITY_BLOCK_SIZE);
    }

    /**
     * Returns the raw component memory segment.
     */
    public MemorySegment getComponentMemory() {
        return componentMemory;
    }

    /**
     * Returns the native address of component memory for LWJGL interop.
     */
    public long getComponentMemoryAddress() {
        return componentMemoryAddress;
    }

    /**
     * Returns the GPU staging buffer address.
     */
    public long getGpuStagingBuffer() {
        return gpuStagingBuffer;
    }

    private void initializeEntityMemory(int slot, net.minecraft.entity.Entity mc) {
        long base = getEntityMemoryOffset(slot);

        // Current Transform
        componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_X, mc.posX);
        componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Y, mc.posY);
        componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Z, mc.posZ);
        componentMemory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_YAW, mc.rotationYaw);
        componentMemory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_PITCH, mc.rotationPitch);
        componentMemory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_ROLL, 0.0f);
        componentMemory.set(ValueLayout.JAVA_INT, base + BridgeComponents.TRANSFORM_FLAGS, 0);

        // Previous Transform (copy of current for first frame)
        componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.PREV_TRANSFORM_X, mc.posX);
        componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.PREV_TRANSFORM_Y, mc.posY);
        componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.PREV_TRANSFORM_Z, mc.posZ);
        componentMemory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.PREV_TRANSFORM_YAW, mc.rotationYaw);
        componentMemory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.PREV_TRANSFORM_PITCH, mc.rotationPitch);
        componentMemory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.PREV_TRANSFORM_ROLL, 0.0f);

        // Velocity
        componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_X, mc.motionX);
        componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Y, mc.motionY);
        componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.VELOCITY_Z, mc.motionZ);
        componentMemory.set(ValueLayout.JAVA_FLOAT, base + BridgeComponents.VELOCITY_SPEED, 0.0f);

        // Acceleration
        componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.ACCEL_X, 0.0);
        componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.ACCEL_Y, 0.0);
        componentMemory.set(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.ACCEL_Z, 0.0);

        // Metadata
        componentMemory.set(ValueLayout.JAVA_INT, base + BridgeComponents.META_MC_ID, mc.getEntityId());
        componentMemory.set(ValueLayout.JAVA_INT, base + BridgeComponents.META_ECS_ID, ecsEntities[slot] != null ? ecsEntities[slot].id() : -1);
        componentMemory.set(ValueLayout.JAVA_LONG, base + BridgeComponents.META_FLAGS, 0L);
        componentMemory.set(ValueLayout.JAVA_LONG, base + BridgeComponents.META_LAST_SYNC, (long) TICK_COUNT_HANDLE.get(this));
    }

    private void clearEntityMemory(int slot) {
        long base = getEntityMemoryOffset(slot);
        componentMemory.asSlice(base, ENTITY_BLOCK_SIZE).fill((byte) 0);
    }

    // ========================================================================
    // TICK PROCESSING
    // ========================================================================

    /**
     * Main tick entry point. Called from Minecraft's client tick.
     *
     * @param partialTicks interpolation factor for rendering
     */
    public void onClientTick(float partialTicks) {
        if (!isRunning()) return;

        if (!circuitBreaker.allowRequest()) {
            metrics.recordCircuitBreakerTrip();
            return;
        }

        long now = System.nanoTime();
        long last = (long) LAST_TICK_NANOS_HANDLE.getAndSet(this, now);

        if (last == 0L) return; // First tick

        float deltaTime = (now - last) / 1_000_000_000.0f;
        deltaTime = Math.min(deltaTime, 0.1f); // Clamp to 100ms max

        try {
            executeTickPipeline(deltaTime);
            circuitBreaker.recordSuccess();
            TICK_COUNT_HANDLE.getAndAdd(this, 1L);
            metrics.recordTick(now - last);

        } catch (Exception e) {
            circuitBreaker.recordFailure();
            LOGGER.log(Level.SEVERE, "[ECS-Bridge] Tick failed", e);
            metrics.recordTickFailure();
        }
    }

    private void executeTickPipeline(float deltaTime) {
        // 1. Process command buffer
        ecsWorld.getCommandBuffer().execute(ecsWorld);

        // 2. Collect active slots into batch buffer
        int[] batch = BATCH_BUFFER.get();
        int batchSize = collectActiveSlots(batch);

        // 3. Execute inbound sync (MC -> ECS)
        batchProcessor.syncInbound(batch, batchSize, mcEntities);

        // 4. Execute ECS update
        ecsWorld.update(deltaTime);

        // 5. Execute outbound sync (ECS -> MC)
        batchProcessor.syncOutbound(batch, batchSize, mcEntities);
    }

    private int collectActiveSlots(int[] batch) {
        int count = 0;
        int maxBatch = batch.length;

        for (int slot = 0; slot < MAX_ENTITIES && count < maxBatch; slot++) {
            if (slotStates[slot].get() == SLOT_ACTIVE) {
                batch[count++] = slot;
            }
        }

        return count;
    }

    /**
     * Called during render tick for interpolation.
     *
     * @param partialTicks interpolation factor [0, 1)
     */
    public void onRenderTick(float partialTicks) {
        if (!isRunning()) return;

        try {
            ecsWorld.executeStage(SystemScheduler.Stage.RENDER, partialTicks);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[ECS-Bridge] Render tick failed", e);
        }
    }

    // ========================================================================
    // INTERPOLATION (Zero Allocation)
    // ========================================================================

    /**
     * Gets interpolated transform for rendering.
     * Writes to thread-local array to avoid allocation.
     *
     * @param slot entity slot
     * @param t    interpolation factor [0, 1]
     * @return double array [x, y, z, yaw, pitch, roll] (thread-local, do not store)
     */
    public double[] getInterpolatedTransform(int slot, float t) {
        double[] out = INTERPOLATION_OUT.get();
        getInterpolatedTransform(slot, t, out);
        return out;
    }

    /**
     * Gets interpolated transform into provided array.
     *
     * @param slot entity slot
     * @param t    interpolation factor [0, 1]
     * @param out  output array (length >= 6)
     */
    public void getInterpolatedTransform(int slot, float t, double[] out) {
        long base = getEntityMemoryOffset(slot);

        // Read previous transform
        double px = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.PREV_TRANSFORM_X);
        double py = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.PREV_TRANSFORM_Y);
        double pz = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.PREV_TRANSFORM_Z);
        float pYaw = componentMemory.get(ValueLayout.JAVA_FLOAT, base + BridgeComponents.PREV_TRANSFORM_YAW);
        float pPitch = componentMemory.get(ValueLayout.JAVA_FLOAT, base + BridgeComponents.PREV_TRANSFORM_PITCH);
        float pRoll = componentMemory.get(ValueLayout.JAVA_FLOAT, base + BridgeComponents.PREV_TRANSFORM_ROLL);

        // Read current transform
        double cx = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_X);
        double cy = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Y);
        double cz = componentMemory.get(ValueLayout.JAVA_DOUBLE, base + BridgeComponents.TRANSFORM_Z);
        float cYaw = componentMemory.get(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_YAW);
        float cPitch = componentMemory.get(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_PITCH);
        float cRoll = componentMemory.get(ValueLayout.JAVA_FLOAT, base + BridgeComponents.TRANSFORM_ROLL);

        // Interpolate
        out[0] = px + (cx - px) * t;
        out[1] = py + (cy - py) * t;
        out[2] = pz + (cz - pz) * t;
        out[3] = lerpAngle(pYaw, cYaw, t);
        out[4] = pPitch + (cPitch - pPitch) * t;
        out[5] = pRoll + (cRoll - pRoll) * t;
    }

    private static float lerpAngle(float from, float to, float t) {
        float diff = ((to - from + 540.0f) % 360.0f) - 180.0f;
        return from + diff * t;
    }

    // ========================================================================
    // ACCESSORS
    // ========================================================================

    public boolean isRunning() {
        int s = (int) STATE_HANDLE.get(this);
        return s == STATE_RUNNING;
    }

    public boolean isPaused() {
        return (int) STATE_HANDLE.get(this) == STATE_PAUSED;
    }

    public int getActiveEntityCount() {
        return (int) ACTIVE_COUNT_HANDLE.get(this);
    }

    public long getTickCount() {
        return (long) TICK_COUNT_HANDLE.get(this);
    }

    public World getEcsWorld() {
        return ecsWorld;
    }

    public Entity getEcsEntity(int slot) {
        if (slot < 0 || slot >= MAX_ENTITIES) return null;
        return ecsEntities[slot];
    }

    public net.minecraft.entity.Entity getMcEntity(int slot) {
        if (slot < 0 || slot >= MAX_ENTITIES) return null;
        return mcEntities[slot];
    }

    public BridgeMetrics getMetrics() {
        return metrics;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    // ========================================================================
    // LIFECYCLE CONTROL
    // ========================================================================

    public void pause() {
        STATE_HANDLE.compareAndSet(this, STATE_RUNNING, STATE_PAUSED);
    }

    public void resume() {
        STATE_HANDLE.compareAndSet(this, STATE_PAUSED, STATE_RUNNING);
    }

    private void shutdownHook() {
        try {
            close();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[ECS-Bridge] Error in shutdown hook", e);
        }
    }

    @Override
    public void close() {
        int prev = (int) STATE_HANDLE.getAndSet(this, STATE_SHUTTING_DOWN);
        if (prev == STATE_SHUTTING_DOWN || prev == STATE_TERMINATED) {
            return;
        }

        LOGGER.info("[ECS-Bridge] Shutting down...");

        try {
            // Shutdown ECS world
            if (ecsWorld != null) {
                ecsWorld.shutdown();
            }

            // Clear all entity references
            for (int i = 0; i < MAX_ENTITIES; i++) {
                mcEntities[i] = null;
                ecsEntities[i] = null;
            }

            // Free GPU staging buffer
            if (gpuStagingBuffer != MemoryUtil.NULL) {
                MemoryUtil.nmemAlignedFree(gpuStagingBuffer);
            }

            // Close component arena
            if (componentArena.scope().isAlive()) {
                componentArena.close();
            }

            LOGGER.info("[ECS-Bridge] Shutdown complete");

        } finally {
            STATE_HANDLE.set(this, STATE_TERMINATED);
        }
    }

    // ========================================================================
    // UTILITIES
    // ========================================================================

    private static String stateToString(int state) {
        return switch (state) {
            case STATE_UNINITIALIZED -> "UNINITIALIZED";
            case STATE_INITIALIZING -> "INITIALIZING";
            case STATE_RUNNING -> "RUNNING";
            case STATE_PAUSED -> "PAUSED";
            case STATE_SHUTTING_DOWN -> "SHUTTING_DOWN";
            case STATE_TERMINATED -> "TERMINATED";
            default -> "UNKNOWN(" + state + ")";
        };
    }
}
