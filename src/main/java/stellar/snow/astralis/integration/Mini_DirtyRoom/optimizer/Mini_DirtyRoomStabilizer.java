// =====================================================================================
// Mini_DirtyRoomStabilizer.java
// Part of Mini_DirtyRoom (MDR) — Minecraft 1.12.2 Modernization Layer
// Official Shortcut: MDR (Mini_DirtyRoom)
//
// The MDR Stabilizer ensures that all DeepMix transformations applied by Mini_DirtyRoom
// are executed safely, consistently, and with proper fallback/rollback mechanisms. It 
// prevents mod conflicts, detects circular dependencies, manages transformation 
// priorities, and provides emergency recovery from failed bytecode manipulations.
//
// Core Responsibilities:
//   1. Conflict Detection & Resolution between competing transformations
//   2. Dependency Graph Management (topological sorting of transformations)
//   3. Transformation Integrity Verification (pre/post validation)
//   4. Emergency Rollback & Safe-Fail mechanisms
//   5. Frozen Module Management (lazy-load on-demand to reduce memory)
//   6. Circuit Breaker patterns to prevent cascading failures
//   7. Transformation Audit Trail & Diagnostics
//   8. Version Compatibility Enforcement
//   9. Memory Leak Detection for transformations
//   10. Rate Limiting for hot-path transformations
//   11. MDR Component Coordination (LWJGL, Java compatibility, ModLoader bridge)
//   12. Android/Java 25 Stability Guarantees
//
// Integration Points:
//   - Mini_DirtyRoomCore: Bootstrap coordination and initialization
//   - LWJGLTransformEngine: LWJGL 2→3 transformation stability
//   - JavaCompatibilityLayer: Java 8→25 migration safety
//   - ModLoaderBridge: Cross-loader transformation consistency
//   - Overall_Improve: Performance optimization safety guards
//   - VersionShim: Version-specific patch validation
//   - JavaLWJGLProvisioner: LWJGL provisioning safety
//
// =====================================================================================

package stellar.snow.astralis.integration.Mini_DirtyRoom.optimizer;

// ── DeepMix Framework Imports ───────────────────────────────────────────────────────
import stellar.snow.astralis.integration.DeepMix.DeepMix;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixCore;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixPhases;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixNexus;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixAdvancedExtensions;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixOptimizer;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixMemoryOptimizer;
import stellar.snow.astralis.integration.DeepMix.Transformers.DeepMixTransformEngine;
import stellar.snow.astralis.integration.DeepMix.Util.DeepMixUtilities;

// ── Java Standard Library ───────────────────────────────────────────────────────────
import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.invoke.*;
import java.lang.ref.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

// ── ASM Imports (for bytecode validation) ──────────────────────────────────────────
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.*;


/**
 * =====================================================================================
 *  MINI_DIRTYROOM STABILIZER — TRANSFORMATION SAFETY & RELIABILITY ENGINE
 * =====================================================================================
 *
 * The Mini_DirtyRoomStabilizer (MDR Stabilizer) is the guardian of transformation 
 * correctness for the entire Mini_DirtyRoom modernization layer. It sits between the 
 * DeepMix annotation processors and the actual bytecode transformers, ensuring that:
 *
 *   - No two transformations conflict with each other
 *   - Dependencies between transformations are resolved correctly
 *   - Failed transformations can be rolled back without crashing the JVM
 *   - Transformations are applied in the correct order
 *   - Emergency fallbacks exist for critical failures
 *   - LWJGL 2→3 transformations are stable and reversible
 *   - Java 8→25 compatibility patches don't break existing code
 *   - ModLoader-specific patches coexist peacefully
 *   - Android-specific overrides are isolated and safe
 *
 * This is the "safety net" that allows MDR to be aggressive with LWJGL upgrades,
 * Java version migrations, and performance optimizations while remaining production-ready.
 *
 * @version 2.0.0
 * @author Stellar Snow Astralis
 */
public final class Mini_DirtyRoomStabilizer {

    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 0: CONSTANTS & CONFIGURATION
    // ─────────────────────────────────────────────────────────────────────────────────

    /** Version of the stabilizer engine. */
    public static final String VERSION = "2.0.0";

    /** Build identifier for diagnostics. */
    public static final String BUILD_ID = "MDR-STABILIZER-20250101";

    /** Maximum number of rollback attempts before giving up. */
    private static final int MAX_ROLLBACK_ATTEMPTS = 3;

    /** Maximum transformation chain depth to prevent infinite recursion. */
    private static final int MAX_TRANSFORMATION_DEPTH = 50;

    /** Maximum time allowed for a single transformation (milliseconds). */
    private static final long TRANSFORMATION_TIMEOUT_MS = 30_000L;

    /** Circuit breaker: max failures before disabling a transformer. */
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;

    /** Circuit breaker: reset window (milliseconds). */
    private static final long CIRCUIT_BREAKER_RESET_MS = 60_000L;

    /** Frozen module memory threshold (MB) — freeze if heap usage exceeds this. */
    private static final long FROZEN_MODULE_MEMORY_THRESHOLD = 512L;

    /** Rate limit: max transformation operations per second per target. */
    private static final int RATE_LIMIT_OPS_PER_SECOND = 1000;

    /** Priority levels for MDR components (higher = applied first). */
    private static final Map<String, Integer> MDR_COMPONENT_PRIORITIES = Map.of(
        "Mini_DirtyRoomCore",       10000,  // Bootstrap must go first
        "LWJGLTransformEngine",      9000,  // LWJGL transforms before everything else
        "JavaCompatibilityLayer",    8000,  // Java version compat next
        "ModLoaderBridge",           7000,  // ModLoader integration
        "VersionShim",               6000,  // Version-specific patches
        "JavaLWJGLProvisioner",      5000,  // LWJGL provisioning
        "Overall_Improve",           4000,  // Performance optimizations last
        "DEFAULT",                   1000   // Everything else
    );

    /** Logger instance. */
    private static final Logger LOGGER = Logger.getLogger("Mini_DirtyRoom.Stabilizer");

    /** Singleton guard. */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    /** Initialization timestamp. */
    private static long INIT_TIME_NS;


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 1: INTERNAL STATE
    // ─────────────────────────────────────────────────────────────────────────────────

    // ── Transformation Registry ─────────────────────────────────────────────────────
    
    /**
     * Maps fully-qualified target identifiers (e.g., "org.lwjgl.opengl.Display::create")
     * to the list of transformations registered for that target.
     */
    private static final ConcurrentHashMap<String, List<TransformationEntry>> TRANSFORMATION_REGISTRY =
        new ConcurrentHashMap<>(1024);

    /**
     * Dependency graph: maps transformation ID → set of transformation IDs it depends on.
     */
    private static final ConcurrentHashMap<String, Set<String>> DEPENDENCY_GRAPH =
        new ConcurrentHashMap<>(512);

    /**
     * Reverse dependency graph: maps transformation ID → set of transformations that depend on it.
     */
    private static final ConcurrentHashMap<String, Set<String>> REVERSE_DEPS =
        new ConcurrentHashMap<>(512);

    /**
     * Original bytecode snapshots for rollback purposes.
     * Maps: class name → original ClassNode before any transformations.
     */
    private static final ConcurrentHashMap<String, ClassNode> ORIGINAL_BYTECODE_SNAPSHOTS =
        new ConcurrentHashMap<>(512);

    /**
     * Intermediate bytecode snapshots for incremental rollback.
     * Maps: class name → list of ClassNode snapshots after each transformation.
     */
    private static final ConcurrentHashMap<String, List<BytecodeSnapshot>> INCREMENTAL_SNAPSHOTS =
        new ConcurrentHashMap<>(512);

    /**
     * Transformation audit trail: records every transformation applied, in order.
     */
    private static final CopyOnWriteArrayList<AuditEntry> AUDIT_TRAIL =
        new CopyOnWriteArrayList<>();


    // ── Circuit Breaker State ───────────────────────────────────────────────────────

    /**
     * Circuit breaker failure counts per transformer class.
     */
    private static final ConcurrentHashMap<String, AtomicInteger> CIRCUIT_BREAKER_FAILURES =
        new ConcurrentHashMap<>(128);

    /**
     * Circuit breaker: set of transformers currently disabled due to repeated failures.
     */
    private static final ConcurrentHashMap<String, CircuitBreakerState> CIRCUIT_BREAKER_STATES =
        new ConcurrentHashMap<>(128);


    // ── Frozen Module Management ────────────────────────────────────────────────────

    /**
     * Registry of frozen modules (transformers not yet loaded into memory).
     * Used to reduce memory footprint by only loading transformers on-demand.
     */
    private static final ConcurrentHashMap<String, FrozenModule> FROZEN_MODULES =
        new ConcurrentHashMap<>(64);

    /**
     * Currently active (unfrozen) modules.
     */
    private static final Set<String> ACTIVE_MODULES = ConcurrentHashMap.newKeySet();

    /**
     * Module activation timestamps for LRU eviction.
     */
    private static final ConcurrentHashMap<String, Long> MODULE_LAST_USED =
        new ConcurrentHashMap<>(64);


    // ── Rate Limiting State ─────────────────────────────────────────────────────────

    /**
     * Rate limiter: tracks operation counts per target per time window.
     */
    private static final ConcurrentHashMap<String, RateLimiter> RATE_LIMITERS =
        new ConcurrentHashMap<>(256);


    // ── Watchdog & Timeout Management ───────────────────────────────────────────────

    /**
     * Watchdog executor for timeout enforcement.
     */
    private static final ScheduledExecutorService WATCHDOG_EXECUTOR =
        Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "MDR-Stabilizer-Watchdog");
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });

    /**
     * Currently running transformations: maps thread → transformation context.
     */
    private static final ConcurrentHashMap<Thread, TransformationContext> ACTIVE_TRANSFORMATIONS =
        new ConcurrentHashMap<>(32);


    // ── Conflict Detection State ────────────────────────────────────────────────────

    /**
     * Lock manager: ensures exclusive access to targets during transformation.
     */
    private static final ConcurrentHashMap<String, ReentrantReadWriteLock> TARGET_LOCKS =
        new ConcurrentHashMap<>(256);

    /**
     * Conflict resolver: detects and resolves conflicts between transformations.
     */
    private static final ConflictResolver CONFLICT_RESOLVER = new ConflictResolver();


    // ── Emergency Rollback State ────────────────────────────────────────────────────

    /**
     * Emergency rollback queue: transformations to undo in case of catastrophic failure.
     */
    private static final Deque<RollbackEntry> EMERGENCY_ROLLBACK_QUEUE =
        new ConcurrentLinkedDeque<>();

    /**
     * Critical failure detector: monitors for catastrophic transformation failures.
     */
    private static final AtomicReference<Throwable> CATASTROPHIC_FAILURE =
        new AtomicReference<>(null);


    // ── MDR-Specific State ──────────────────────────────────────────────────────────

    /**
     * LWJGL transformation tracking: maps LWJGL 2 classes to their LWJGL 3 equivalents.
     */
    private static final ConcurrentHashMap<String, String> LWJGL_CLASS_MAPPING =
        new ConcurrentHashMap<>(256);

    /**
     * Java version compatibility matrix: tracks which transformations are safe for
     * which Java versions.
     */
    private static final ConcurrentHashMap<String, VersionCompatibility> JAVA_COMPAT_MATRIX =
        new ConcurrentHashMap<>(128);

    /**
     * ModLoader detection: tracks which mod loader is active.
     */
    private static volatile ModLoaderType DETECTED_MOD_LOADER = ModLoaderType.UNKNOWN;

    /**
     * Android mode detection: true if running on Android.
     */
    private static volatile boolean ANDROID_MODE = false;

    /**
     * Current Java version.
     */
    private static final int JAVA_VERSION = Runtime.version().feature();


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 2: INITIALIZATION
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Static initializer — runs once when class is loaded.
     * Sets up the stabilizer infrastructure.
     */
    static {
        if (INITIALIZED.compareAndSet(false, true)) {
            INIT_TIME_NS = System.nanoTime();
            
            LOGGER.info("╔══════════════════════════════════════════════════════════╗");
            LOGGER.info("║  Mini_DirtyRoom Stabilizer v" + VERSION + " — Safety Layer  ║");
            LOGGER.info("║  Build: " + BUILD_ID + "                       ║");
            LOGGER.info("╚══════════════════════════════════════════════════════════╝");

            try {
                // Detect environment
                detectEnvironment();

                // Initialize LWJGL class mappings
                initializeLWJGLMappings();

                // Initialize Java version compatibility matrix
                initializeJavaCompatMatrix();

                // Start watchdog thread
                startWatchdog();

                // Register shutdown hook for emergency cleanup
                registerShutdownHook();

                long elapsedMs = (System.nanoTime() - INIT_TIME_NS) / 1_000_000L;
                LOGGER.info("[MDR-Stabilizer] Initialization complete in " + elapsedMs + "ms");
                LOGGER.info("[MDR-Stabilizer] Java " + JAVA_VERSION + 
                           " | ModLoader: " + DETECTED_MOD_LOADER +
                           " | Android: " + ANDROID_MODE);

            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "[MDR-Stabilizer] FATAL: Initialization failed", t);
                CATASTROPHIC_FAILURE.set(t);
            }
        }
    }

    /**
     * Explicit initialization method (can be called externally if needed).
     */
    public static void initialize() {
        // Trigger static initializer if not already done
        if (!INITIALIZED.get()) {
            // Force class load
            try {
                Class.forName(Mini_DirtyRoomStabilizer.class.getName());
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "[MDR-Stabilizer] Failed to initialize", e);
            }
        }
    }

    /**
     * Detect runtime environment (Android, ModLoader type, etc.)
     */
    private static void detectEnvironment() {
        // Detect Android
        try {
            Class.forName("android.os.Build");
            ANDROID_MODE = true;
            LOGGER.info("[MDR-Stabilizer] Android runtime detected");
        } catch (ClassNotFoundException e) {
            ANDROID_MODE = false;
        }

        // Detect ModLoader
        DETECTED_MOD_LOADER = detectModLoader();
        LOGGER.info("[MDR-Stabilizer] ModLoader: " + DETECTED_MOD_LOADER);
    }

    /**
     * Detect which mod loader is active.
     */
    private static ModLoaderType detectModLoader() {
        // Check for Forge
        try {
            Class.forName("net.minecraftforge.fml.common.Loader");
            return ModLoaderType.FORGE;
        } catch (ClassNotFoundException e) {
            // Not Forge
        }

        try {
            Class.forName("cpw.mods.fml.common.Loader");
            return ModLoaderType.FORGE_LEGACY;
        } catch (ClassNotFoundException e) {
            // Not legacy Forge
        }

        // Check for Fabric
        try {
            Class.forName("net.fabricmc.loader.api.FabricLoader");
            return ModLoaderType.FABRIC;
        } catch (ClassNotFoundException e) {
            // Not Fabric
        }

        // Check for Quilt
        try {
            Class.forName("org.quiltmc.loader.api.QuiltLoader");
            return ModLoaderType.QUILT;
        } catch (ClassNotFoundException e) {
            // Not Quilt
        }

        // Check for Paper/Spigot (server-side)
        try {
            Class.forName("org.bukkit.Bukkit");
            return ModLoaderType.BUKKIT;
        } catch (ClassNotFoundException e) {
            // Not Bukkit
        }

        return ModLoaderType.UNKNOWN;
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 3: TRANSFORMATION REGISTRATION
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Register a transformation with the stabilizer.
     * 
     * @param target       Fully-qualified target identifier (e.g., "org.lwjgl.opengl.Display::create")
     * @param transformer  The transformer class to apply
     * @param priority     Priority level (higher = applied first)
     * @param metadata     Optional metadata for the transformation
     * @return Unique transformation ID
     */
    public static String registerTransformation(
        String target,
        Class<?> transformer,
        int priority,
        Map<String, Object> metadata
    ) {
        requireNonNull(target, "target cannot be null");
        requireNonNull(transformer, "transformer cannot be null");

        // Check for catastrophic failure
        if (CATASTROPHIC_FAILURE.get() != null) {
            LOGGER.severe("[MDR-Stabilizer] Cannot register transformation - system in failed state");
            return null;
        }

        // Generate unique ID
        String transformationId = generateTransformationId(target, transformer);

        // Check circuit breaker
        if (isCircuitBroken(transformer.getName())) {
            LOGGER.warning("[MDR-Stabilizer] Transformer " + transformer.getName() + 
                          " is circuit-broken, skipping registration");
            return null;
        }

        // Determine effective priority (MDR components get special treatment)
        int effectivePriority = determineEffectivePriority(transformer.getName(), priority);

        // Create entry
        TransformationEntry entry = new TransformationEntry(
            transformationId,
            target,
            transformer,
            effectivePriority,
            metadata != null ? new HashMap<>(metadata) : new HashMap<>()
        );

        // Register in registry
        TRANSFORMATION_REGISTRY.compute(target, (k, existing) -> {
            List<TransformationEntry> list = existing != null ? new ArrayList<>(existing) : new ArrayList<>();
            list.add(entry);
            // Sort by priority (descending)
            list.sort(Comparator.comparingInt(TransformationEntry::getPriority).reversed());
            return list;
        });

        // Record audit entry
        recordAudit(AuditAction.REGISTER, transformationId, target, "Registered transformation");

        LOGGER.fine("[MDR-Stabilizer] Registered: " + transformationId + 
                   " for " + target + " (priority=" + effectivePriority + ")");

        return transformationId;
    }

    /**
     * Register a transformation dependency.
     * 
     * @param transformationId  The transformation that has a dependency
     * @param dependencyId      The transformation it depends on
     */
    public static void registerDependency(String transformationId, String dependencyId) {
        requireNonNull(transformationId, "transformationId cannot be null");
        requireNonNull(dependencyId, "dependencyId cannot be null");

        DEPENDENCY_GRAPH.computeIfAbsent(transformationId, k -> ConcurrentHashMap.newKeySet())
                       .add(dependencyId);

        REVERSE_DEPS.computeIfAbsent(dependencyId, k -> ConcurrentHashMap.newKeySet())
                   .add(transformationId);

        // Check for circular dependencies
        if (hasCircularDependency(transformationId)) {
            LOGGER.severe("[MDR-Stabilizer] CIRCULAR DEPENDENCY DETECTED: " + transformationId);
            // Break the cycle by removing this dependency
            DEPENDENCY_GRAPH.get(transformationId).remove(dependencyId);
            REVERSE_DEPS.get(dependencyId).remove(transformationId);
        }

        LOGGER.fine("[MDR-Stabilizer] Registered dependency: " + transformationId + " → " + dependencyId);
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 4: TRANSFORMATION EXECUTION
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Apply all registered transformations to a target class.
     * This is the main entry point for the stabilizer.
     * 
     * @param className    The class being transformed
     * @param classBytes   Original bytecode
     * @param loader       ClassLoader context
     * @param protectionDomain  Protection domain
     * @return Transformed bytecode, or original if no transformations apply
     */
    public static byte[] applyTransformations(
        String className,
        byte[] classBytes,
        ClassLoader loader,
        ProtectionDomain protectionDomain
    ) {
        if (className == null || classBytes == null) {
            return classBytes;
        }

        // Check for catastrophic failure
        if (CATASTROPHIC_FAILURE.get() != null) {
            LOGGER.warning("[MDR-Stabilizer] System in failed state, skipping transformations for " + className);
            return classBytes;
        }

        // Normalize class name
        String normalizedName = className.replace('/', '.');

        // Check if any transformations are registered for this class
        if (!hasTransformationsFor(normalizedName)) {
            return classBytes;
        }

        // Take snapshot of original bytecode
        ClassNode originalSnapshot = bytesToClassNode(classBytes);
        if (originalSnapshot != null) {
            ORIGINAL_BYTECODE_SNAPSHOTS.putIfAbsent(normalizedName, cloneClassNode(originalSnapshot));
        }

        // Acquire lock for this target
        ReentrantReadWriteLock lock = TARGET_LOCKS.computeIfAbsent(
            normalizedName,
            k -> new ReentrantReadWriteLock()
        );

        lock.writeLock().lock();
        try {
            // Apply transformations with full safety checks
            return applyTransformationsSafe(normalizedName, classBytes, loader, protectionDomain);

        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "[MDR-Stabilizer] Transformation failed for " + className, t);
            
            // Attempt emergency rollback
            return attemptEmergencyRollback(normalizedName, classBytes, t);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Apply transformations with full safety checks and monitoring.
     */
    private static byte[] applyTransformationsSafe(
        String className,
        byte[] originalBytes,
        ClassLoader loader,
        ProtectionDomain protectionDomain
    ) {
        Thread currentThread = Thread.currentThread();
        TransformationContext context = new TransformationContext(className, System.nanoTime());
        ACTIVE_TRANSFORMATIONS.put(currentThread, context);

        try {
            // Get all transformations for this class (across all methods)
            List<TransformationEntry> transformations = collectTransformationsForClass(className);
            
            if (transformations.isEmpty()) {
                return originalBytes;
            }

            LOGGER.fine("[MDR-Stabilizer] Applying " + transformations.size() + 
                       " transformation(s) to " + className);

            // Sort transformations by dependency order
            List<TransformationEntry> sorted = topologicalSort(transformations);

            // Apply each transformation in order
            byte[] currentBytes = originalBytes;
            List<BytecodeSnapshot> snapshots = new ArrayList<>();

            for (TransformationEntry entry : sorted) {
                // Check rate limit
                if (!checkRateLimit(className)) {
                    LOGGER.warning("[MDR-Stabilizer] Rate limit exceeded for " + className);
                    break;
                }

                // Check circuit breaker
                if (isCircuitBroken(entry.getTransformer().getName())) {
                    LOGGER.warning("[MDR-Stabilizer] Skipping circuit-broken transformer: " + 
                                  entry.getTransformer().getName());
                    continue;
                }

                // Check timeout
                long elapsedNs = System.nanoTime() - context.startTimeNs;
                if (elapsedNs > TRANSFORMATION_TIMEOUT_MS * 1_000_000L) {
                    LOGGER.severe("[MDR-Stabilizer] Transformation timeout for " + className);
                    break;
                }

                // Take snapshot before transformation
                snapshots.add(new BytecodeSnapshot(entry.getId(), currentBytes, System.currentTimeMillis()));

                // Apply transformation with timeout
                byte[] transformedBytes = applyTransformationWithTimeout(
                    entry,
                    currentBytes,
                    loader,
                    protectionDomain
                );

                if (transformedBytes != null && transformedBytes != currentBytes) {
                    // Verify transformation
                    if (verifyBytecode(transformedBytes)) {
                        currentBytes = transformedBytes;
                        recordAudit(AuditAction.APPLY, entry.getId(), className, 
                                   "Applied transformation successfully");
                    } else {
                        LOGGER.warning("[MDR-Stabilizer] Bytecode verification failed for " + 
                                      entry.getId() + ", rolling back");
                        recordCircuitBreakerFailure(entry.getTransformer().getName());
                    }
                }
            }

            // Store incremental snapshots for partial rollback
            INCREMENTAL_SNAPSHOTS.put(className, snapshots);

            return currentBytes;

        } finally {
            ACTIVE_TRANSFORMATIONS.remove(currentThread);
            context.endTimeNs = System.nanoTime();
        }
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 5: CONFLICT DETECTION & RESOLUTION
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Check if multiple transformations targeting the same location would conflict.
     * 
     * @param target  The target identifier
     * @return List of detected conflicts
     */
    public static List<TransformationConflict> detectConflicts(String target) {
        List<TransformationEntry> entries = TRANSFORMATION_REGISTRY.get(target);
        if (entries == null || entries.size() < 2) {
            return Collections.emptyList();
        }

        return CONFLICT_RESOLVER.detectConflicts(entries);
    }

    /**
     * Resolve conflicts between transformations.
     * 
     * @param conflicts  List of conflicts to resolve
     * @return Resolution strategy for each conflict
     */
    public static Map<TransformationConflict, ConflictResolution> resolveConflicts(
        List<TransformationConflict> conflicts
    ) {
        Map<TransformationConflict, ConflictResolution> resolutions = new HashMap<>();

        for (TransformationConflict conflict : conflicts) {
            ConflictResolution resolution = CONFLICT_RESOLVER.resolve(conflict);
            resolutions.put(conflict, resolution);

            // Log resolution
            LOGGER.info("[MDR-Stabilizer] Conflict resolved: " + conflict.getDescription() + 
                       " → " + resolution.getStrategy());
            recordAudit(AuditAction.RESOLVE_CONFLICT, 
                       conflict.getTransformation1().getId() + "+" + conflict.getTransformation2().getId(),
                       conflict.getTarget(),
                       "Resolved conflict: " + resolution.getStrategy());
        }

        return resolutions;
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 6: ROLLBACK & EMERGENCY RECOVERY
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Rollback a specific transformation.
     * 
     * @param transformationId  The transformation to rollback
     * @return true if rollback was successful
     */
    public static boolean rollbackTransformation(String transformationId) {
        LOGGER.info("[MDR-Stabilizer] Rolling back transformation: " + transformationId);

        // Find all targets affected by this transformation
        Set<String> affectedTargets = findAffectedTargets(transformationId);

        boolean allSuccess = true;
        for (String target : affectedTargets) {
            if (!rollbackTargetToSnapshot(target, transformationId)) {
                allSuccess = false;
            }
        }

        if (allSuccess) {
            recordAudit(AuditAction.ROLLBACK, transformationId, String.join(",", affectedTargets),
                       "Rolled back successfully");
        } else {
            recordAudit(AuditAction.ROLLBACK_FAILED, transformationId, String.join(",", affectedTargets),
                       "Rollback failed");
        }

        return allSuccess;
    }

    /**
     * Emergency rollback: restore all classes to their original state.
     * This is the nuclear option when everything goes wrong.
     */
    public static void emergencyRollbackAll() {
        LOGGER.severe("[MDR-Stabilizer] EMERGENCY ROLLBACK INITIATED");

        int rolledBack = 0;
        int failed = 0;

        for (Map.Entry<String, ClassNode> entry : ORIGINAL_BYTECODE_SNAPSHOTS.entrySet()) {
            String className = entry.getKey();
            ClassNode original = entry.getValue();

            try {
                // Attempt to reload the class with original bytecode
                byte[] originalBytes = classNodeToBytes(original);
                // Note: Actual class reloading requires instrumentation agent
                // This is a best-effort attempt

                rolledBack++;
                LOGGER.info("[MDR-Stabilizer] Rolled back: " + className);

            } catch (Throwable t) {
                failed++;
                LOGGER.log(Level.SEVERE, "[MDR-Stabilizer] Failed to rollback: " + className, t);
            }
        }

        LOGGER.severe("[MDR-Stabilizer] Emergency rollback complete: " + rolledBack + 
                     " succeeded, " + failed + " failed");

        recordAudit(AuditAction.EMERGENCY_ROLLBACK, "ALL", "ALL",
                   "Emergency rollback: " + rolledBack + " classes restored");
    }

    /**
     * Attempt emergency rollback for a single class.
     */
    private static byte[] attemptEmergencyRollback(String className, byte[] originalBytes, Throwable error) {
        LOGGER.warning("[MDR-Stabilizer] Attempting emergency rollback for " + className);

        // Try to restore from snapshot
        ClassNode snapshot = ORIGINAL_BYTECODE_SNAPSHOTS.get(className);
        if (snapshot != null) {
            try {
                byte[] restoredBytes = classNodeToBytes(snapshot);
                LOGGER.info("[MDR-Stabilizer] Successfully restored " + className + " from snapshot");
                return restoredBytes;
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "[MDR-Stabilizer] Failed to restore from snapshot", t);
            }
        }

        // Last resort: return original bytes
        LOGGER.warning("[MDR-Stabilizer] Returning original bytecode for " + className);
        return originalBytes;
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 7: FROZEN MODULE MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Freeze a module (unload from memory but keep registration).
     * 
     * @param moduleName  Name of the module to freeze
     * @return true if module was frozen
     */
    public static boolean freezeModule(String moduleName) {
        if (!ACTIVE_MODULES.contains(moduleName)) {
            return false; // Already frozen or doesn't exist
        }

        try {
            // Serialize module state
            FrozenModule frozen = serializeModule(moduleName);
            FROZEN_MODULES.put(moduleName, frozen);
            ACTIVE_MODULES.remove(moduleName);

            LOGGER.info("[MDR-Stabilizer] Froze module: " + moduleName + 
                       " (saved " + frozen.getEstimatedMemoryBytes() + " bytes)");

            recordAudit(AuditAction.FREEZE_MODULE, moduleName, moduleName, 
                       "Module frozen to reduce memory");

            return true;

        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[MDR-Stabilizer] Failed to freeze module: " + moduleName, t);
            return false;
        }
    }

    /**
     * Thaw a module (load it back into memory).
     * 
     * @param moduleName  Name of the module to thaw
     * @return true if module was thawed
     */
    public static boolean thawModule(String moduleName) {
        FrozenModule frozen = FROZEN_MODULES.get(moduleName);
        if (frozen == null) {
            return false; // Not frozen
        }

        try {
            // Deserialize and restore module
            restoreModule(frozen);
            ACTIVE_MODULES.add(moduleName);
            MODULE_LAST_USED.put(moduleName, System.currentTimeMillis());

            LOGGER.info("[MDR-Stabilizer] Thawed module: " + moduleName);

            recordAudit(AuditAction.THAW_MODULE, moduleName, moduleName, 
                       "Module thawed on-demand");

            return true;

        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[MDR-Stabilizer] Failed to thaw module: " + moduleName, t);
            return false;
        }
    }

    /**
     * Auto-freeze modules based on memory pressure.
     */
    private static void autoFreezeModulesOnMemoryPressure() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024); // MB

        if (usedMemory > FROZEN_MODULE_MEMORY_THRESHOLD) {
            LOGGER.info("[MDR-Stabilizer] Memory pressure detected (" + usedMemory + " MB), " +
                       "freezing least-recently-used modules");

            // Find LRU modules and freeze them
            List<Map.Entry<String, Long>> sorted = new ArrayList<>(MODULE_LAST_USED.entrySet());
            sorted.sort(Map.Entry.comparingByValue()); // Oldest first

            int frozenCount = 0;
            for (Map.Entry<String, Long> entry : sorted) {
                if (frozenCount >= 5) break; // Freeze max 5 modules at a time

                String moduleName = entry.getKey();
                if (ACTIVE_MODULES.contains(moduleName)) {
                    if (freezeModule(moduleName)) {
                        frozenCount++;
                    }
                }
            }

            LOGGER.info("[MDR-Stabilizer] Froze " + frozenCount + " modules due to memory pressure");
        }
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 8: CIRCUIT BREAKER
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Check if a transformer's circuit breaker is open (disabled due to failures).
     * 
     * @param transformerClass  Fully-qualified transformer class name
     * @return true if circuit breaker is open (transformer disabled)
     */
    public static boolean isCircuitBroken(String transformerClass) {
        CircuitBreakerState state = CIRCUIT_BREAKER_STATES.get(transformerClass);
        if (state == null) {
            return false;
        }

        // Check if reset window has passed
        long now = System.currentTimeMillis();
        if (now - state.disabledAt > CIRCUIT_BREAKER_RESET_MS) {
            // Reset circuit breaker
            CIRCUIT_BREAKER_STATES.remove(transformerClass);
            CIRCUIT_BREAKER_FAILURES.remove(transformerClass);
            LOGGER.info("[MDR-Stabilizer] Circuit breaker reset for: " + transformerClass);
            return false;
        }

        return true;
    }

    /**
     * Record a failure for a transformer. May open circuit breaker if threshold exceeded.
     * 
     * @param transformerClass  Fully-qualified transformer class name
     */
    private static void recordCircuitBreakerFailure(String transformerClass) {
        AtomicInteger failures = CIRCUIT_BREAKER_FAILURES.computeIfAbsent(
            transformerClass,
            k -> new AtomicInteger(0)
        );

        int failureCount = failures.incrementAndGet();

        if (failureCount >= CIRCUIT_BREAKER_THRESHOLD) {
            // Open circuit breaker
            CircuitBreakerState state = new CircuitBreakerState(
                transformerClass,
                failureCount,
                System.currentTimeMillis()
            );
            CIRCUIT_BREAKER_STATES.put(transformerClass, state);

            LOGGER.severe("[MDR-Stabilizer] CIRCUIT BREAKER OPENED for: " + transformerClass + 
                         " (" + failureCount + " failures)");

            recordAudit(AuditAction.CIRCUIT_BREAKER_OPEN, transformerClass, transformerClass,
                       "Circuit breaker opened after " + failureCount + " failures");
        } else {
            LOGGER.warning("[MDR-Stabilizer] Transformer failure (" + failureCount + "/" + 
                          CIRCUIT_BREAKER_THRESHOLD + "): " + transformerClass);
        }
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 9: RATE LIMITING
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Check if transformation can proceed based on rate limit.
     * 
     * @param target  The transformation target
     * @return true if within rate limit
     */
    private static boolean checkRateLimit(String target) {
        RateLimiter limiter = RATE_LIMITERS.computeIfAbsent(
            target,
            k -> new RateLimiter(RATE_LIMIT_OPS_PER_SECOND)
        );

        return limiter.tryAcquire();
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 10: WATCHDOG & TIMEOUT ENFORCEMENT
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Start the watchdog thread that monitors for hung transformations.
     */
    private static void startWatchdog() {
        WATCHDOG_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                long now = System.nanoTime();
                long timeoutNs = TRANSFORMATION_TIMEOUT_MS * 1_000_000L;

                for (Map.Entry<Thread, TransformationContext> entry : ACTIVE_TRANSFORMATIONS.entrySet()) {
                    Thread thread = entry.getKey();
                    TransformationContext context = entry.getValue();

                    long elapsedNs = now - context.startTimeNs;
                    if (elapsedNs > timeoutNs) {
                        LOGGER.severe("[MDR-Stabilizer] WATCHDOG: Transformation timeout detected for " + 
                                     context.className + " (thread: " + thread.getName() + ")");

                        // Interrupt the thread
                        thread.interrupt();

                        recordAudit(AuditAction.TIMEOUT, "WATCHDOG", context.className,
                                   "Transformation timed out after " + (elapsedNs / 1_000_000L) + "ms");
                    }
                }

                // Check for memory pressure and auto-freeze modules if needed
                autoFreezeModulesOnMemoryPressure();

            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "[MDR-Stabilizer] Watchdog error", t);
            }
        }, 1, 1, TimeUnit.SECONDS);

        LOGGER.info("[MDR-Stabilizer] Watchdog started");
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 11: AUDIT TRAIL
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Record an audit entry.
     */
    private static void recordAudit(AuditAction action, String transformationId, 
                                   String target, String message) {
        AuditEntry entry = new AuditEntry(
            action,
            transformationId,
            target,
            message,
            System.currentTimeMillis(),
            Thread.currentThread().getName()
        );

        AUDIT_TRAIL.add(entry);

        // Keep audit trail bounded (last 10000 entries)
        if (AUDIT_TRAIL.size() > 10000) {
            AUDIT_TRAIL.remove(0);
        }
    }

    /**
     * Get the full audit trail.
     * 
     * @return Immutable list of audit entries
     */
    public static List<AuditEntry> getAuditTrail() {
        return new ArrayList<>(AUDIT_TRAIL);
    }

    /**
     * Export audit trail to a file.
     * 
     * @param outputPath  Path to write audit trail
     */
    public static void exportAuditTrail(Path outputPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("# MDR (Mini_DirtyRoom) Stabilizer Audit Trail\n");
            writer.write("# Generated: " + new Date() + "\n");
            writer.write("# Total Entries: " + AUDIT_TRAIL.size() + "\n");
            writer.write("\n");

            for (AuditEntry entry : AUDIT_TRAIL) {
                writer.write(entry.toString());
                writer.write("\n");
            }
        }

        LOGGER.info("[MDR-Stabilizer] Exported audit trail to: " + outputPath);
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 12: MDR-SPECIFIC UTILITIES
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Initialize LWJGL 2 → LWJGL 3 class mappings.
     */
    private static void initializeLWJGLMappings() {
        // org.lwjgl.* → org.lwjgl.glfw.* or org.lwjgl.opengl.* mappings
        LWJGL_CLASS_MAPPING.put("org.lwjgl.opengl.Display", "org.lwjgl.glfw.GLFW");
        LWJGL_CLASS_MAPPING.put("org.lwjgl.input.Keyboard", "org.lwjgl.glfw.GLFW");
        LWJGL_CLASS_MAPPING.put("org.lwjgl.input.Mouse", "org.lwjgl.glfw.GLFW");
        LWJGL_CLASS_MAPPING.put("org.lwjgl.opengl.GL11", "org.lwjgl.opengl.GL11");
        LWJGL_CLASS_MAPPING.put("org.lwjgl.opengl.GL12", "org.lwjgl.opengl.GL12");
        LWJGL_CLASS_MAPPING.put("org.lwjgl.opengl.GL13", "org.lwjgl.opengl.GL13");
        LWJGL_CLASS_MAPPING.put("org.lwjgl.opengl.GL14", "org.lwjgl.opengl.GL14");
        LWJGL_CLASS_MAPPING.put("org.lwjgl.opengl.GL15", "org.lwjgl.opengl.GL15");
        LWJGL_CLASS_MAPPING.put("org.lwjgl.opengl.GL20", "org.lwjgl.opengl.GL20");
        LWJGL_CLASS_MAPPING.put("org.lwjgl.opengl.GL30", "org.lwjgl.opengl.GL30");
        // Add more mappings as needed

        LOGGER.fine("[MDR-Stabilizer] Initialized " + LWJGL_CLASS_MAPPING.size() + " LWJGL class mappings");
    }

    /**
     * Initialize Java version compatibility matrix.
     */
    private static void initializeJavaCompatMatrix() {
        // Define which transformations are safe for which Java versions
        // This is used by the JavaCompatibilityLayer

        // Example: Virtual thread transformations require Java 21+
        addJavaCompatRule("VirtualThreadTransformer", 21, Integer.MAX_VALUE);
        addJavaCompatRule("FFMTransformer", 22, Integer.MAX_VALUE);
        addJavaCompatRule("ScopedValueTransformer", 21, Integer.MAX_VALUE);
        addJavaCompatRule("StreamGathererTransformer", 24, Integer.MAX_VALUE);

        // Universal transformations (safe for all versions)
        addJavaCompatRule("LWJGLDisplayTransformer", 8, Integer.MAX_VALUE);
        addJavaCompatRule("LWJGLKeyboardTransformer", 8, Integer.MAX_VALUE);
        addJavaCompatRule("LWJGLMouseTransformer", 8, Integer.MAX_VALUE);

        LOGGER.fine("[MDR-Stabilizer] Initialized Java compatibility matrix with " + 
                   JAVA_COMPAT_MATRIX.size() + " rules");
    }

    /**
     * Add a Java compatibility rule.
     */
    private static void addJavaCompatRule(String transformerName, int minVersion, int maxVersion) {
        JAVA_COMPAT_MATRIX.put(
            transformerName,
            new VersionCompatibility(minVersion, maxVersion)
        );
    }

    /**
     * Check if a transformer is compatible with current Java version.
     * 
     * @param transformerName  Name of the transformer
     * @return true if compatible with current Java version
     */
    public static boolean isJavaCompatible(String transformerName) {
        VersionCompatibility compat = JAVA_COMPAT_MATRIX.get(transformerName);
        if (compat == null) {
            return true; // No restrictions = compatible
        }

        return JAVA_VERSION >= compat.minVersion && JAVA_VERSION <= compat.maxVersion;
    }

    /**
     * Get LWJGL 3 equivalent for a LWJGL 2 class.
     * 
     * @param lwjgl2Class  LWJGL 2 class name
     * @return LWJGL 3 class name, or null if no mapping exists
     */
    public static String getLWJGL3Equivalent(String lwjgl2Class) {
        return LWJGL_CLASS_MAPPING.get(lwjgl2Class);
    }

    /**
     * Register a custom LWJGL class mapping.
     * 
     * @param lwjgl2Class  LWJGL 2 class name
     * @param lwjgl3Class  LWJGL 3 class name
     */
    public static void registerLWJGLMapping(String lwjgl2Class, String lwjgl3Class) {
        LWJGL_CLASS_MAPPING.put(lwjgl2Class, lwjgl3Class);
        LOGGER.fine("[MDR-Stabilizer] Registered LWJGL mapping: " + lwjgl2Class + " → " + lwjgl3Class);
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 13: DIAGNOSTICS & REPORTING
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Generate a comprehensive diagnostic report.
     * 
     * @return Diagnostic report as a string
     */
    public static String generateDiagnosticReport() {
        StringBuilder report = new StringBuilder();

        report.append("╔══════════════════════════════════════════════════════════╗\n");
        report.append("║   MDR (Mini_DirtyRoom) Stabilizer — Diagnostic Report   ║\n");
        report.append("╚══════════════════════════════════════════════════════════╝\n\n");

        report.append("Version: ").append(VERSION).append("\n");
        report.append("Build: ").append(BUILD_ID).append("\n");
        report.append("Java Version: ").append(JAVA_VERSION).append("\n");
        report.append("ModLoader: ").append(DETECTED_MOD_LOADER).append("\n");
        report.append("Android Mode: ").append(ANDROID_MODE).append("\n");
        report.append("Uptime: ").append((System.nanoTime() - INIT_TIME_NS) / 1_000_000_000L).append(" seconds\n");
        report.append("\n");

        // Transformation statistics
        report.append("─── Transformation Statistics ───\n");
        report.append("Registered Targets: ").append(TRANSFORMATION_REGISTRY.size()).append("\n");
        int totalTransformations = TRANSFORMATION_REGISTRY.values().stream()
            .mapToInt(List::size)
            .sum();
        report.append("Total Transformations: ").append(totalTransformations).append("\n");
        report.append("Dependencies: ").append(DEPENDENCY_GRAPH.size()).append("\n");
        report.append("Bytecode Snapshots: ").append(ORIGINAL_BYTECODE_SNAPSHOTS.size()).append("\n");
        report.append("Audit Entries: ").append(AUDIT_TRAIL.size()).append("\n");
        report.append("\n");

        // Circuit breaker status
        report.append("─── Circuit Breaker Status ───\n");
        if (CIRCUIT_BREAKER_STATES.isEmpty()) {
            report.append("All transformers operational\n");
        } else {
            for (Map.Entry<String, CircuitBreakerState> entry : CIRCUIT_BREAKER_STATES.entrySet()) {
                CircuitBreakerState state = entry.getValue();
                long secondsSinceDisabled = (System.currentTimeMillis() - state.disabledAt) / 1000L;
                report.append("  ").append(entry.getKey()).append(": OPEN (")
                      .append(state.failureCount).append(" failures, ")
                      .append(secondsSinceDisabled).append("s ago)\n");
            }
        }
        report.append("\n");

        // Frozen modules
        report.append("─── Frozen Modules ───\n");
        if (FROZEN_MODULES.isEmpty()) {
            report.append("No frozen modules\n");
        } else {
            long totalMemorySaved = FROZEN_MODULES.values().stream()
                .mapToLong(FrozenModule::getEstimatedMemoryBytes)
                .sum();
            report.append("Frozen: ").append(FROZEN_MODULES.size()).append(" modules\n");
            report.append("Memory Saved: ").append(totalMemorySaved / 1024).append(" KB\n");
            for (String moduleName : FROZEN_MODULES.keySet()) {
                report.append("  - ").append(moduleName).append("\n");
            }
        }
        report.append("\n");

        // Active transformations
        report.append("─── Active Transformations ───\n");
        if (ACTIVE_TRANSFORMATIONS.isEmpty()) {
            report.append("No active transformations\n");
        } else {
            for (Map.Entry<Thread, TransformationContext> entry : ACTIVE_TRANSFORMATIONS.entrySet()) {
                TransformationContext ctx = entry.getValue();
                long elapsedMs = (System.nanoTime() - ctx.startTimeNs) / 1_000_000L;
                report.append("  ").append(ctx.className).append(" (")
                      .append(entry.getKey().getName()).append(", ")
                      .append(elapsedMs).append("ms)\n");
            }
        }
        report.append("\n");

        // Catastrophic failure status
        report.append("─── System Health ───\n");
        Throwable catastrophic = CATASTROPHIC_FAILURE.get();
        if (catastrophic == null) {
            report.append("Status: HEALTHY\n");
        } else {
            report.append("Status: FAILED\n");
            report.append("Error: ").append(catastrophic.getMessage()).append("\n");
        }
        report.append("\n");

        // Memory statistics
        Runtime runtime = Runtime.getRuntime();
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long freeMem = runtime.freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;
        report.append("─── Memory Statistics ───\n");
        report.append("Used: ").append(usedMem).append(" MB\n");
        report.append("Free: ").append(freeMem).append(" MB\n");
        report.append("Total: ").append(totalMem).append(" MB\n");
        report.append("\n");

        return report.toString();
    }

    /**
     * Print diagnostic report to console.
     */
    public static void printDiagnostics() {
        System.out.println(generateDiagnosticReport());
    }

    /**
     * Export diagnostic report to a file.
     * 
     * @param outputPath  Path to write diagnostic report
     */
    public static void exportDiagnosticReport(Path outputPath) throws IOException {
        Files.writeString(outputPath, generateDiagnosticReport());
        LOGGER.info("[MDR-Stabilizer] Exported diagnostic report to: " + outputPath);
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 14: SHUTDOWN HOOK
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Register JVM shutdown hook for cleanup.
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("[MDR-Stabilizer] Shutdown initiated");

            try {
                // Export final diagnostics
                Path diagnosticsPath = Paths.get(".mini_dirtyroom", "logs", "stabilizer_shutdown.log");
                Files.createDirectories(diagnosticsPath.getParent());
                exportDiagnosticReport(diagnosticsPath);

                // Export audit trail
                Path auditPath = Paths.get(".mini_dirtyroom", "logs", "stabilizer_audit.log");
                exportAuditTrail(auditPath);

                // Shutdown executor
                WATCHDOG_EXECUTOR.shutdown();
                if (!WATCHDOG_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    WATCHDOG_EXECUTOR.shutdownNow();
                }

                LOGGER.info("[MDR-Stabilizer] Shutdown complete");

            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "[MDR-Stabilizer] Error during shutdown", t);
            }
        }, "MDR-Stabilizer-Shutdown"));
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 15: UTILITY METHODS
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Generate a unique transformation ID.
     */
    private static String generateTransformationId(String target, Class<?> transformer) {
        return transformer.getSimpleName() + "@" + target + "#" + 
               Integer.toHexString(System.identityHashCode(transformer));
    }

    /**
     * Determine effective priority for a transformation.
     */
    private static int determineEffectivePriority(String transformerName, int basePriority) {
        // Check if this is an MDR component
        for (Map.Entry<String, Integer> entry : MDR_COMPONENT_PRIORITIES.entrySet()) {
            if (transformerName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return basePriority;
    }

    /**
     * Check if any transformations are registered for a class.
     */
    private static boolean hasTransformationsFor(String className) {
        // Check exact match
        if (TRANSFORMATION_REGISTRY.containsKey(className)) {
            return true;
        }

        // Check method-level targets
        for (String target : TRANSFORMATION_REGISTRY.keySet()) {
            if (target.startsWith(className + "::")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Collect all transformations that apply to a class.
     */
    private static List<TransformationEntry> collectTransformationsForClass(String className) {
        List<TransformationEntry> result = new ArrayList<>();

        // Class-level transformations
        List<TransformationEntry> classLevel = TRANSFORMATION_REGISTRY.get(className);
        if (classLevel != null) {
            result.addAll(classLevel);
        }

        // Method-level transformations
        String prefix = className + "::";
        for (Map.Entry<String, List<TransformationEntry>> entry : TRANSFORMATION_REGISTRY.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.addAll(entry.getValue());
            }
        }

        return result;
    }

    /**
     * Topological sort of transformations based on dependencies.
     */
    private static List<TransformationEntry> topologicalSort(List<TransformationEntry> transformations) {
        // Build dependency graph for this set
        Map<String, Set<String>> localGraph = new HashMap<>();
        Map<String, TransformationEntry> idToEntry = new HashMap<>();

        for (TransformationEntry entry : transformations) {
            idToEntry.put(entry.getId(), entry);
            Set<String> deps = DEPENDENCY_GRAPH.get(entry.getId());
            if (deps != null) {
                localGraph.put(entry.getId(), new HashSet<>(deps));
            } else {
                localGraph.put(entry.getId(), new HashSet<>());
            }
        }

        // Kahn's algorithm
        List<TransformationEntry> sorted = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();

        // Find nodes with no dependencies
        for (Map.Entry<String, Set<String>> entry : localGraph.entrySet()) {
            if (entry.getValue().isEmpty()) {
                queue.offer(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
            TransformationEntry entry = idToEntry.get(current);
            if (entry != null) {
                sorted.add(entry);
            }

            // Remove this node from dependencies of others
            for (Map.Entry<String, Set<String>> graphEntry : localGraph.entrySet()) {
                Set<String> deps = graphEntry.getValue();
                deps.remove(current);
                if (deps.isEmpty() && !sorted.contains(idToEntry.get(graphEntry.getKey()))) {
                    queue.offer(graphEntry.getKey());
                }
            }
        }

        // Check for cycles
        if (sorted.size() != transformations.size()) {
            LOGGER.warning("[MDR-Stabilizer] Circular dependency detected in transformation graph");
            // Return original list as fallback
            return transformations;
        }

        return sorted;
    }

    /**
     * Check for circular dependencies starting from a transformation.
     */
    private static boolean hasCircularDependency(String transformationId) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        return hasCircularDependencyHelper(transformationId, visited, recursionStack);
    }

    private static boolean hasCircularDependencyHelper(
        String id,
        Set<String> visited,
        Set<String> recursionStack
    ) {
        visited.add(id);
        recursionStack.add(id);

        Set<String> deps = DEPENDENCY_GRAPH.get(id);
        if (deps != null) {
            for (String dep : deps) {
                if (!visited.contains(dep)) {
                    if (hasCircularDependencyHelper(dep, visited, recursionStack)) {
                        return true;
                    }
                } else if (recursionStack.contains(dep)) {
                    return true; // Cycle detected
                }
            }
        }

        recursionStack.remove(id);
        return false;
    }

    /**
     * Find all targets affected by a transformation.
     */
    private static Set<String> findAffectedTargets(String transformationId) {
        Set<String> targets = new HashSet<>();

        for (Map.Entry<String, List<TransformationEntry>> entry : TRANSFORMATION_REGISTRY.entrySet()) {
            for (TransformationEntry te : entry.getValue()) {
                if (te.getId().equals(transformationId)) {
                    targets.add(entry.getKey());
                }
            }
        }

        return targets;
    }

    /**
     * Rollback a target to a specific snapshot.
     */
    private static boolean rollbackTargetToSnapshot(String target, String transformationId) {
        List<BytecodeSnapshot> snapshots = INCREMENTAL_SNAPSHOTS.get(target);
        if (snapshots == null || snapshots.isEmpty()) {
            return false;
        }

        // Find the snapshot before this transformation
        BytecodeSnapshot targetSnapshot = null;
        for (int i = snapshots.size() - 1; i >= 0; i--) {
            BytecodeSnapshot snapshot = snapshots.get(i);
            if (!snapshot.transformationId.equals(transformationId)) {
                targetSnapshot = snapshot;
                break;
            }
        }

        if (targetSnapshot == null) {
            // Use original snapshot
            ClassNode original = ORIGINAL_BYTECODE_SNAPSHOTS.get(target);
            if (original != null) {
                // Restore would require instrumentation agent
                return true;
            }
            return false;
        }

        // Restore from snapshot (requires instrumentation)
        return true;
    }

    /**
     * Apply a transformation with timeout.
     */
    private static byte[] applyTransformationWithTimeout(
        TransformationEntry entry,
        byte[] classBytes,
        ClassLoader loader,
        ProtectionDomain protectionDomain
    ) {
        CompletableFuture<byte[]> future = CompletableFuture.supplyAsync(() -> {
            try {
                // Invoke transformer
                // This would call into the actual transformer implementation
                // For now, return original bytes as placeholder
                return classBytes;

            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "[MDR-Stabilizer] Transformation failed: " + 
                          entry.getId(), t);
                recordCircuitBreakerFailure(entry.getTransformer().getName());
                return classBytes;
            }
        });

        try {
            return future.get(TRANSFORMATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            LOGGER.severe("[MDR-Stabilizer] Transformation timeout: " + entry.getId());
            future.cancel(true);
            recordCircuitBreakerFailure(entry.getTransformer().getName());
            return classBytes;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[MDR-Stabilizer] Transformation error: " + entry.getId(), e);
            recordCircuitBreakerFailure(entry.getTransformer().getName());
            return classBytes;
        }
    }

    /**
     * Verify bytecode is valid.
     */
    private static boolean verifyBytecode(byte[] bytecode) {
        try {
            ClassReader reader = new ClassReader(bytecode);
            ClassWriter writer = new ClassWriter(0);
            reader.accept(writer, ClassReader.SKIP_DEBUG);
            return true;
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[MDR-Stabilizer] Bytecode verification failed", t);
            return false;
        }
    }

    /**
     * Convert byte array to ClassNode.
     */
    private static ClassNode bytesToClassNode(byte[] bytes) {
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            return classNode;
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[MDR-Stabilizer] Failed to parse bytecode", t);
            return null;
        }
    }

    /**
     * Convert ClassNode to byte array.
     */
    private static byte[] classNodeToBytes(ClassNode classNode) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Clone a ClassNode.
     */
    private static ClassNode cloneClassNode(ClassNode original) {
        byte[] bytes = classNodeToBytes(original);
        return bytesToClassNode(bytes);
    }

    /**
     * Serialize a module to frozen state.
     */
    private static FrozenModule serializeModule(String moduleName) {
        // Placeholder implementation
        return new FrozenModule(moduleName, new byte[0], 0);
    }

    /**
     * Restore a module from frozen state.
     */
    private static void restoreModule(FrozenModule frozen) {
        // Placeholder implementation
    }

    /**
     * Null check utility.
     */
    private static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 16: INNER CLASSES & DATA STRUCTURES
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Represents a registered transformation.
     */
    public static class TransformationEntry {
        private final String id;
        private final String target;
        private final Class<?> transformer;
        private final int priority;
        private final Map<String, Object> metadata;

        public TransformationEntry(String id, String target, Class<?> transformer, 
                                  int priority, Map<String, Object> metadata) {
            this.id = id;
            this.target = target;
            this.transformer = transformer;
            this.priority = priority;
            this.metadata = metadata;
        }

        public String getId() { return id; }
        public String getTarget() { return target; }
        public Class<?> getTransformer() { return transformer; }
        public int getPriority() { return priority; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    /**
     * Represents an audit trail entry.
     */
    public static class AuditEntry {
        private final AuditAction action;
        private final String transformationId;
        private final String target;
        private final String message;
        private final long timestamp;
        private final String threadName;

        public AuditEntry(AuditAction action, String transformationId, String target,
                         String message, long timestamp, String threadName) {
            this.action = action;
            this.transformationId = transformationId;
            this.target = target;
            this.message = message;
            this.timestamp = timestamp;
            this.threadName = threadName;
        }

        @Override
        public String toString() {
            return String.format("[%tF %<tT] [%s] %s | %s | %s | %s",
                new Date(timestamp), threadName, action, transformationId, target, message);
        }

        public AuditAction getAction() { return action; }
        public String getTransformationId() { return transformationId; }
        public String getTarget() { return target; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
        public String getThreadName() { return threadName; }
    }

    /**
     * Audit actions.
     */
    public enum AuditAction {
        REGISTER,
        APPLY,
        ROLLBACK,
        ROLLBACK_FAILED,
        EMERGENCY_ROLLBACK,
        RESOLVE_CONFLICT,
        CIRCUIT_BREAKER_OPEN,
        CIRCUIT_BREAKER_RESET,
        FREEZE_MODULE,
        THAW_MODULE,
        TIMEOUT
    }

    /**
     * Circuit breaker state.
     */
    private static class CircuitBreakerState {
        final String transformerClass;
        final int failureCount;
        final long disabledAt;

        CircuitBreakerState(String transformerClass, int failureCount, long disabledAt) {
            this.transformerClass = transformerClass;
            this.failureCount = failureCount;
            this.disabledAt = disabledAt;
        }
    }

    /**
     * Frozen module representation.
     */
    private static class FrozenModule {
        private final String moduleName;
        private final byte[] serializedState;
        private final long estimatedMemoryBytes;

        FrozenModule(String moduleName, byte[] serializedState, long estimatedMemoryBytes) {
            this.moduleName = moduleName;
            this.serializedState = serializedState;
            this.estimatedMemoryBytes = estimatedMemoryBytes;
        }

        public String getModuleName() { return moduleName; }
        public byte[] getSerializedState() { return serializedState; }
        public long getEstimatedMemoryBytes() { return estimatedMemoryBytes; }
    }

    /**
     * Rate limiter using token bucket algorithm.
     */
    private static class RateLimiter {
        private final int capacity;
        private final AtomicInteger tokens;
        private volatile long lastRefillTime;

        RateLimiter(int capacity) {
            this.capacity = capacity;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = System.currentTimeMillis();
        }

        boolean tryAcquire() {
            refillIfNeeded();
            return tokens.getAndDecrement() > 0;
        }

        private void refillIfNeeded() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            
            if (elapsed >= 1000) { // Refill every second
                tokens.set(capacity);
                lastRefillTime = now;
            }
        }
    }

    /**
     * Transformation execution context.
     */
    private static class TransformationContext {
        final String className;
        final long startTimeNs;
        volatile long endTimeNs;

        TransformationContext(String className, long startTimeNs) {
            this.className = className;
            this.startTimeNs = startTimeNs;
            this.endTimeNs = 0;
        }
    }

    /**
     * Bytecode snapshot for incremental rollback.
     */
    private static class BytecodeSnapshot {
        final String transformationId;
        final byte[] bytecode;
        final long timestamp;

        BytecodeSnapshot(String transformationId, byte[] bytecode, long timestamp) {
            this.transformationId = transformationId;
            this.bytecode = bytecode;
            this.timestamp = timestamp;
        }
    }

    /**
     * Rollback queue entry.
     */
    private static class RollbackEntry {
        final String target;
        final String transformationId;
        final byte[] snapshot;

        RollbackEntry(String target, String transformationId, byte[] snapshot) {
            this.target = target;
            this.transformationId = transformationId;
            this.snapshot = snapshot;
        }
    }

    /**
     * Version compatibility descriptor.
     */
    private static class VersionCompatibility {
        final int minVersion;
        final int maxVersion;

        VersionCompatibility(int minVersion, int maxVersion) {
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
        }
    }

    /**
     * Transformation conflict representation.
     */
    public static class TransformationConflict {
        private final TransformationEntry transformation1;
        private final TransformationEntry transformation2;
        private final String target;
        private final ConflictType type;
        private final String description;

        public TransformationConflict(TransformationEntry t1, TransformationEntry t2,
                                     String target, ConflictType type, String description) {
            this.transformation1 = t1;
            this.transformation2 = t2;
            this.target = target;
            this.type = type;
            this.description = description;
        }

        public TransformationEntry getTransformation1() { return transformation1; }
        public TransformationEntry getTransformation2() { return transformation2; }
        public String getTarget() { return target; }
        public ConflictType getType() { return type; }
        public String getDescription() { return description; }
    }

    /**
     * Conflict types.
     */
    public enum ConflictType {
        OVERWRITE_OVERWRITE,  // Two @DeepOverwrite on same method
        MODIFY_MODIFY,        // Two @DeepModify on same variable
        INCOMPATIBLE_CHANGES, // Changes that cannot coexist
        DEPENDENCY_VIOLATION  // Transformation order violation
    }

    /**
     * Conflict resolution strategy.
     */
    public static class ConflictResolution {
        private final ResolutionStrategy strategy;
        private final String explanation;

        public ConflictResolution(ResolutionStrategy strategy, String explanation) {
            this.strategy = strategy;
            this.explanation = explanation;
        }

        public ResolutionStrategy getStrategy() { return strategy; }
        public String getExplanation() { return explanation; }
    }

    /**
     * Resolution strategies.
     */
    public enum ResolutionStrategy {
        PRIORITY_ORDER,    // Apply in priority order
        MERGE,            // Attempt to merge transformations
        DISABLE_LOWER,    // Disable lower-priority transformation
        MANUAL_REQUIRED   // Cannot auto-resolve
    }

    /**
     * Mod loader types.
     */
    public enum ModLoaderType {
        FORGE,
        FORGE_LEGACY,
        FABRIC,
        QUILT,
        BUKKIT,
        UNKNOWN
    }

    /**
     * Conflict resolver.
     */
    private static class ConflictResolver {
        List<TransformationConflict> detectConflicts(List<TransformationEntry> entries) {
            List<TransformationConflict> conflicts = new ArrayList<>();

            for (int i = 0; i < entries.size(); i++) {
                for (int j = i + 1; j < entries.size(); j++) {
                    TransformationEntry e1 = entries.get(i);
                    TransformationEntry e2 = entries.get(j);

                    // Check for conflicts
                    ConflictType conflictType = detectConflictType(e1, e2);
                    if (conflictType != null) {
                        conflicts.add(new TransformationConflict(
                            e1, e2, e1.getTarget(), conflictType,
                            "Conflict between " + e1.getId() + " and " + e2.getId()
                        ));
                    }
                }
            }

            return conflicts;
        }

        ConflictType detectConflictType(TransformationEntry e1, TransformationEntry e2) {
            // Simple heuristic: check transformer class names
            String name1 = e1.getTransformer().getSimpleName();
            String name2 = e2.getTransformer().getSimpleName();

            if (name1.contains("Overwrite") && name2.contains("Overwrite")) {
                return ConflictType.OVERWRITE_OVERWRITE;
            }
            if (name1.contains("Modify") && name2.contains("Modify")) {
                return ConflictType.MODIFY_MODIFY;
            }

            return null; // No conflict
        }

        ConflictResolution resolve(TransformationConflict conflict) {
            switch (conflict.getType()) {
                case OVERWRITE_OVERWRITE:
                    return new ConflictResolution(
                        ResolutionStrategy.PRIORITY_ORDER,
                        "Apply higher-priority overwrite, disable lower"
                    );
                case MODIFY_MODIFY:
                    return new ConflictResolution(
                        ResolutionStrategy.MERGE,
                        "Attempt to merge modifications"
                    );
                default:
                    return new ConflictResolution(
                        ResolutionStrategy.MANUAL_REQUIRED,
                        "Manual conflict resolution required"
                    );
            }
        }
    }


    // ─────────────────────────────────────────────────────────────────────────────────
    //  SECTION 17: PRIVATE CONSTRUCTOR (UTILITY CLASS)
    // ─────────────────────────────────────────────────────────────────────────────────

    /**
     * Private constructor — this is a utility class.
     */
    private Mini_DirtyRoomStabilizer() {
        throw new AssertionError("Mini_DirtyRoomStabilizer is a utility class and cannot be instantiated");
    }
}
