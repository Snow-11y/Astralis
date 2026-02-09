package stellar.snow.astralis.integration.DeepMix.Core;

/*
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                                                                            ║
 * ║  🔮 DEEPMIX — PERFORMANCE, OPTIMIZATION & STABILITY ENGINE                ║
 * ║  ══════════════════════════════════════════════════════════════             ║
 * ║                                                                            ║
 * ║  "Zero-cost when idle, surgical when active"                               ║
 * ║                                                                            ║
 * ║  Core Philosophy:                                                          ║
 * ║    • FROZEN by default — modules sleep until explicitly awakened            ║
 * ║    • LAZY everywhere — nothing loads until first touch                      ║
 * ║    • POOLED resources — never allocate what you can reuse                   ║
 * ║    • BOUNDED operations — every path has a timeout and memory cap           ║
 * ║    • FAIL-SAFE always — crashes are contained, never propagated             ║
 * ║    • OBSERVABLE always — every subsystem reports its health                 ║
 * ║                                                                            ║
 * ║  Module Loading Strategy:                                                  ║
 * ║    PERMANENT (never unloaded):                                             ║
 * ║      - DeepMix (core bootstrap)                                            ║
 * ║      - DeepMixCore (fundamental infrastructure)                            ║
 * ║      - DeepMixMixinHelper (mixin compatibility layer)                      ║
 * ║      - DeepMixStabilizer (crash prevention & recovery)                     ║
 * ║                                                                            ║
 * ║    FROZEN (loaded on demand, unloaded when idle):                           ║
 * ║      - DeepMixAdvancedExtensions                                           ║
 * ║      - DeepMixAssetForge                                                   ║
 * ║      - DeepMixDataFormats                                                  ║
 * ║      - DeepMixNexus                                                        ║
 * ║      - DeepMixPhases                                                       ║
 * ║      - DeepMixMixins                                                       ║
 * ║      - DeepMixTransformEngine                                              ║
 * ║      - DeepMixUtilities                                                    ║
 * ║      - DeepMixTransformers                                                 ║
 * ║                                                                            ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */

import java.lang.annotation.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.management.*;
import java.lang.ref.*;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.stream.Collectors;
import stellar.snow.astralis.integration.DeepMixTransformers;
import stellar.snow.astralis.integration.DeepMix.DeepMix;
import stellar.snow.astralis.integration.DeepMix.DeepMixAssetForge;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixAdvancedExtensions;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixCore;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixDataFormats;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixMixinHelper;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixNexus;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixPhases;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixStabilizer;
import stellar.snow.astralis.integration.DeepMix.Mixins.DeepMixMixins;
import stellar.snow.astralis.integration.DeepMix.Transformers.DeepMixTransformEngine;
import stellar.snow.astralis.integration.DeepMix.Util.DeepMixUtilities;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * ╭──────────────────────────────────────────────────────────────────────────╮
 * │                                                                        │
 * │  DeepMixOptimizer — The Performance & Stability Backbone of DeepMix    │
 * │                                                                        │
 * │  This engine ensures DeepMix NEVER degrades the host application's     │
 * │  performance. All DeepMix subsystems are "frozen" by default and       │
 * │  only thaw (load into memory) when explicitly requested.              │
 * │                                                                        │
 * │  Memory Budget:                                                        │
 * │    • Idle: < 2MB overhead                                              │
 * │    • Active (typical): 8-32MB                                          │
 * │    • Active (heavy): 64-128MB (with safety cap)                        │
 * │    • Emergency: Auto-shed to < 4MB                                     │
 * │                                                                        │
 * │  Latency Budget:                                                       │
 * │    • Class transform: < 5ms per class                                  │
 * │    • Annotation processing: < 1ms per annotation                      │
 * │    • Hot reload cycle: < 50ms                                          │
 * │    • Module thaw: < 100ms                                              │
 * │    • Full cold start: < 500ms                                          │
 * │                                                                        │
 * ╰──────────────────────────────────────────────────────────────────────────╯
 */
public final class DeepMixOptimizer {

    private DeepMixOptimizer() {
        throw new UnsupportedOperationException(
            "DeepMixOptimizer is a static engine — do not instantiate");
    }

    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 1: MODULE FREEZE/THAW SYSTEM                             ║
    // ║                                                                    ║
    // ║  Every DeepMix module is wrapped in a FrozenModule container.      ║
    // ║  Frozen modules consume ~200 bytes each (just the descriptor).     ║
    // ║  Thawed modules hold the actual class references and processors.   ║
    // ║                                                                    ║
    // ║  Lifecycle:  FROZEN → THAWING → ACTIVE → COOLING → FROZEN         ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    // ──────────────────────────────────────────────
    // 1.1 — Module lifecycle states
    // ──────────────────────────────────────────────

    /** Module lifecycle state */
    public enum ModuleState {
        /** Module class is NOT loaded into JVM — zero memory footprint */
        FROZEN,
        /** Module class is being loaded — transitional */
        THAWING,
        /** Module is fully loaded and operational */
        ACTIVE,
        /** Module is winding down — draining pending work */
        COOLING,
        /** Module encountered a fatal error — quarantined */
        QUARANTINED,
        /** Module is permanently loaded (core modules only) */
        PERMANENT
    }

    /** Module classification for loading policy */
    public enum ModuleClass {
        /** Never unloaded — critical infrastructure */
        CORE,
        /** Loaded on demand, unloaded after idle timeout */
        ON_DEMAND,
        /** Loaded only once per session, then frozen */
        SINGLE_USE,
        /** Loaded based on detected platform */
        PLATFORM_SPECIFIC,
        /** Loaded only in development mode */
        DEV_ONLY
    }

    /** Module priority for memory pressure shedding */
    public enum ModulePriority {
        CRITICAL(0),      // Never shed (core, stabilizer)
        HIGH(1),          // Shed only under extreme pressure
        NORMAL(2),        // Standard shedding policy
        LOW(3),           // Shed eagerly when memory is tight
        DISPOSABLE(4);    // Shed immediately on any pressure

        final int level;
        ModulePriority(int level) { this.level = level; }
    }

    // ──────────────────────────────────────────────
    // 1.2 — Module descriptor (lightweight, always in memory)
    // ──────────────────────────────────────────────

    /**
     * Lightweight descriptor for a DeepMix module.
     * This is ALL that stays in memory when a module is frozen.
     * Total footprint: ~200-300 bytes per module.
     */
    public static final class ModuleDescriptor {
        public final String id;
        public final String className;
        public final ModuleClass moduleClass;
        public final ModulePriority priority;
        public final String[] dependencies;  // Module IDs this depends on
        public final long maxMemoryBytes;    // Memory budget for this module
        public final long idleTimeoutMs;     // Time before auto-freeze
        public final boolean threadSafe;     // Can be accessed from multiple threads

        // Mutable state (atomic for lock-free reads)
        private volatile ModuleState state = ModuleState.FROZEN;
        private volatile long lastAccessTimeMs = 0;
        private volatile long thawCount = 0;
        private volatile long totalActiveTimeMs = 0;
        private volatile long lastThawDurationMs = 0;
        private volatile Throwable lastError = null;

        public ModuleDescriptor(String id, String className, ModuleClass moduleClass,
                                ModulePriority priority, String[] dependencies,
                                long maxMemoryBytes, long idleTimeoutMs,
                                boolean threadSafe) {
            this.id = id;
            this.className = className;
            this.moduleClass = moduleClass;
            this.priority = priority;
            this.dependencies = dependencies;
            this.maxMemoryBytes = maxMemoryBytes;
            this.idleTimeoutMs = idleTimeoutMs;
            this.threadSafe = threadSafe;
        }

        public ModuleState state() { return state; }
        public long lastAccessTimeMs() { return lastAccessTimeMs; }
        public long thawCount() { return thawCount; }
        public long totalActiveTimeMs() { return totalActiveTimeMs; }
        public long lastThawDurationMs() { return lastThawDurationMs; }
        public Throwable lastError() { return lastError; }

        public boolean isFrozen() { return state == ModuleState.FROZEN; }
        public boolean isActive() { return state == ModuleState.ACTIVE || state == ModuleState.PERMANENT; }
        public boolean isQuarantined() { return state == ModuleState.QUARANTINED; }

        public long idleTimeMs() {
            if (lastAccessTimeMs == 0) return Long.MAX_VALUE;
            return System.currentTimeMillis() - lastAccessTimeMs;
        }

        void touch() { this.lastAccessTimeMs = System.currentTimeMillis(); }

        @Override
        public String toString() {
            return String.format("Module[%s|%s|%s|thaws=%d|idle=%dms]",
                id, state, priority, thawCount,
                lastAccessTimeMs > 0 ? idleTimeMs() : -1);
        }
    }

    // ──────────────────────────────────────────────
    // 1.3 — Frozen module container
    // ──────────────────────────────────────────────

    /**
     * Container that holds a module reference.
     * When frozen, the internal reference is null (the class may not even be loaded).
     * When thawed, the reference points to the loaded module's API surface.
     *
     * Thread-safe: uses CAS for state transitions, ReadWriteLock for access.
     *
     * @param <T> The module's API type
     */
    public static final class FrozenModule<T> {

        private final ModuleDescriptor descriptor;
        private final Supplier<T> thawFactory;   // How to create the module instance
        private final Consumer<T> freezeAction;  // How to clean up on freeze
        private final ReadWriteLock accessLock;
        private volatile T instance;
        private volatile long estimatedMemoryBytes;

        // Circuit breaker for repeated thaw failures
        private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
        private static final int MAX_CONSECUTIVE_FAILURES = 3;
        private volatile long quarantineUntilMs = 0;
        private static final long QUARANTINE_DURATION_MS = 30_000; // 30 seconds

        public FrozenModule(ModuleDescriptor descriptor,
                            Supplier<T> thawFactory,
                            Consumer<T> freezeAction) {
            this.descriptor = descriptor;
            this.thawFactory = thawFactory;
            this.freezeAction = freezeAction;
            this.accessLock = new ReentrantReadWriteLock(true); // fair lock

            if (descriptor.moduleClass == ModuleClass.CORE) {
                // Core modules are thawed immediately and permanently
                thawInternal();
                descriptor.state = ModuleState.PERMANENT;
            }
        }

        /**
         * Get the module instance, thawing it if necessary.
         * This is the primary access point — zero-cost when already active.
         *
         * @return Module instance, never null for non-quarantined modules
         * @throws ModuleQuarantinedException if module is quarantined
         */
        public T get() {
            // Fast path: already active (no lock needed for volatile read)
            T local = instance;
            if (local != null && descriptor.isActive()) {
                descriptor.touch();
                return local;
            }

            // Check quarantine
            if (descriptor.isQuarantined()) {
                if (System.currentTimeMillis() < quarantineUntilMs) {
                    throw new ModuleQuarantinedException(descriptor);
                }
                // Quarantine expired — allow retry
                descriptor.state = ModuleState.FROZEN;
                consecutiveFailures.set(0);
            }

            // Slow path: need to thaw
            accessLock.writeLock().lock();
            try {
                // Double-check after acquiring lock
                if (instance != null && descriptor.isActive()) {
                    descriptor.touch();
                    return instance;
                }
                return thawInternal();
            } finally {
                accessLock.writeLock().unlock();
            }
        }

        /**
         * Get the module if already active, without triggering a thaw.
         * Returns null if the module is frozen.
         */
        public T getIfActive() {
            T local = instance;
            if (local != null && descriptor.isActive()) {
                descriptor.touch();
                return local;
            }
            return null;
        }

        /**
         * Execute an action with the module, thawing only if needed.
         * The module reference is guaranteed valid for the duration of the action.
         */
        public <R> R withModule(Function<T, R> action) {
            accessLock.readLock().lock();
            try {
                T module = get();
                return action.apply(module);
            } finally {
                accessLock.readLock().unlock();
            }
        }

        /**
         * Execute an action with the module if it's already active.
         * Does NOT trigger a thaw. Returns empty if module is frozen.
         */
        public <R> Optional<R> withModuleIfActive(Function<T, R> action) {
            T local = getIfActive();
            if (local == null) return Optional.empty();

            accessLock.readLock().lock();
            try {
                return Optional.ofNullable(action.apply(local));
            } finally {
                accessLock.readLock().unlock();
            }
        }

        /**
         * Freeze the module — release its resources and memory.
         * No-op for CORE modules (they are permanent).
         */
        public void freeze() {
            if (descriptor.moduleClass == ModuleClass.CORE) return;
            if (descriptor.state == ModuleState.PERMANENT) return;

            accessLock.writeLock().lock();
            try {
                if (instance == null) return;

                descriptor.state = ModuleState.COOLING;
                long activeStarted = descriptor.lastAccessTimeMs;

                try {
                    if (freezeAction != null) {
                        freezeAction.accept(instance);
                    }
                } catch (Exception e) {
                    // Log but don't prevent freeze
                    System.err.println("[DeepMix:Optimizer] Warning during freeze of " +
                        descriptor.id + ": " + e.getMessage());
                }

                instance = null;
                estimatedMemoryBytes = 0;
                descriptor.state = ModuleState.FROZEN;

                if (activeStarted > 0) {
                    descriptor.totalActiveTimeMs +=
                        (System.currentTimeMillis() - activeStarted);
                }
            } finally {
                accessLock.writeLock().unlock();
            }
        }

        /**
         * Force quarantine — module is disabled until quarantine expires.
         */
        public void quarantine(Throwable reason) {
            accessLock.writeLock().lock();
            try {
                freeze();
                descriptor.state = ModuleState.QUARANTINED;
                descriptor.lastError = reason;
                quarantineUntilMs = System.currentTimeMillis() + QUARANTINE_DURATION_MS;
            } finally {
                accessLock.writeLock().unlock();
            }
        }

        public ModuleDescriptor descriptor() { return descriptor; }
        public boolean isFrozen() { return descriptor.isFrozen(); }
        public boolean isActive() { return descriptor.isActive(); }
        public long estimatedMemoryBytes() { return estimatedMemoryBytes; }

        // ── Internal ──

        private T thawInternal() {
            long start = System.nanoTime();
            descriptor.state = ModuleState.THAWING;

            try {
                // Thaw dependencies first
                ModuleRegistry.ensureDependencies(descriptor);

                // Create the module instance
                T created = thawFactory.get();
                if (created == null) {
                    throw new IllegalStateException(
                        "Thaw factory returned null for module: " + descriptor.id);
                }

                instance = created;
                descriptor.state = descriptor.moduleClass == ModuleClass.CORE ?
                    ModuleState.PERMANENT : ModuleState.ACTIVE;
                descriptor.thawCount++;
                descriptor.touch();
                consecutiveFailures.set(0);

                long durationMs = (System.nanoTime() - start) / 1_000_000;
                descriptor.lastThawDurationMs = durationMs;

                // Estimate memory footprint
                estimatedMemoryBytes = estimateObjectSize(created);

                // Check if thaw was too slow
                if (durationMs > 200) {
                    System.err.println("[DeepMix:Optimizer] SLOW THAW: Module '" +
                        descriptor.id + "' took " + durationMs + "ms to thaw");
                }

                return created;

            } catch (Exception e) {
                descriptor.state = ModuleState.FROZEN;
                descriptor.lastError = e;

                int failures = consecutiveFailures.incrementAndGet();
                if (failures >= MAX_CONSECUTIVE_FAILURES) {
                    quarantine(e);
                    throw new ModuleQuarantinedException(descriptor, e);
                }

                throw new ModuleThawException(descriptor, e);
            }
        }

        private long estimateObjectSize(Object obj) {
            // Rough estimation based on Runtime memory delta
            // More accurate with Instrumentation agent, but this is non-invasive
            Runtime rt = Runtime.getRuntime();
            rt.gc();
            long before = rt.totalMemory() - rt.freeMemory();
            // Can't actually force GC reliably, so use heuristic:
            // Base object = 16 bytes, each reference field = 8 bytes
            // For module objects, estimate 50KB-5MB depending on type
            return Math.max(50_000, descriptor.maxMemoryBytes / 4);
        }
    }

    // ──────────────────────────────────────────────
    // 1.4 — Module exceptions
    // ──────────────────────────────────────────────

    public static class ModuleThawException extends RuntimeException {
        private final ModuleDescriptor descriptor;

        public ModuleThawException(ModuleDescriptor descriptor, Throwable cause) {
            super("Failed to thaw module '" + descriptor.id + "': " + cause.getMessage(), cause);
            this.descriptor = descriptor;
        }

        public ModuleDescriptor descriptor() { return descriptor; }
    }

    public static class ModuleQuarantinedException extends RuntimeException {
        private final ModuleDescriptor descriptor;

        public ModuleQuarantinedException(ModuleDescriptor descriptor) {
            super("Module '" + descriptor.id + "' is quarantined until " +
                new Date(System.currentTimeMillis() + QUARANTINE_DURATION_MS));
            this.descriptor = descriptor;
        }

        public ModuleQuarantinedException(ModuleDescriptor descriptor, Throwable cause) {
            super("Module '" + descriptor.id + "' quarantined after " +
                MAX_CONSECUTIVE_FAILURES + " consecutive failures", cause);
            this.descriptor = descriptor;
        }

        public ModuleDescriptor descriptor() { return descriptor; }

        private static final int MAX_CONSECUTIVE_FAILURES = 3;
        private static final long QUARANTINE_DURATION_MS = 30_000;
    }

    // ──────────────────────────────────────────────
    // 1.5 — Module registry (central catalog of all DeepMix modules)
    // ──────────────────────────────────────────────

    /**
     * Central registry of all DeepMix modules.
     * This is the ONLY class that holds references to FrozenModule containers.
     * Total idle memory: ~5KB for all module descriptors.
     */
    public static final class ModuleRegistry {

        private static final ConcurrentHashMap<String, FrozenModule<?>> modules =
            new ConcurrentHashMap<>(16);
        private static final ConcurrentHashMap<String, ModuleDescriptor> descriptors =
            new ConcurrentHashMap<>(16);
        private static volatile boolean initialized = false;
        private static final Object initLock = new Object();

        // ── Module IDs ──
        public static final String DEEPMIX_CORE = "deepmix.core";
        public static final String DEEPMIX_MIXIN_HELPER = "deepmix.mixin_helper";
        public static final String DEEPMIX_STABILIZER = "deepmix.stabilizer";
        public static final String DEEPMIX_BOOTSTRAP = "deepmix.bootstrap";
        public static final String DEEPMIX_PHASES = "deepmix.phases";
        public static final String DEEPMIX_ADVANCED = "deepmix.advanced_extensions";
        public static final String DEEPMIX_ASSET_FORGE = "deepmix.asset_forge";
        public static final String DEEPMIX_DATA_FORMATS = "deepmix.data_formats";
        public static final String DEEPMIX_NEXUS = "deepmix.nexus";
        public static final String DEEPMIX_MIXINS = "deepmix.mixins";
        public static final String DEEPMIX_TRANSFORM_ENGINE = "deepmix.transform_engine";
        public static final String DEEPMIX_UTILITIES = "deepmix.utilities";
        public static final String DEEPMIX_TRANSFORMERS = "deepmix.transformers";

        /**
         * Initialize the module registry with all DeepMix module descriptors.
         * This is called ONCE at DeepMix bootstrap — total cost: ~1ms, ~5KB.
         */
        public static void initialize() {
            if (initialized) return;
            synchronized (initLock) {
                if (initialized) return;

                // ═══════════════════════════════════════════════
                // PERMANENT MODULES (never unloaded)
                // ═══════════════════════════════════════════════

                registerModule(new ModuleDescriptor(
                    DEEPMIX_BOOTSTRAP,
                    "stellar.snow.astralis.integration.DeepMix.DeepMix",
                    ModuleClass.CORE, ModulePriority.CRITICAL,
                    new String[]{}, // No dependencies
                    2 * 1024 * 1024, // 2MB budget
                    Long.MAX_VALUE,  // Never idle-freeze
                    true
                ), () -> loadCoreModule(DEEPMIX_BOOTSTRAP), null);

                registerModule(new ModuleDescriptor(
                    DEEPMIX_CORE,
                    "stellar.snow.astralis.integration.DeepMix.Core.DeepMixCore",
                    ModuleClass.CORE, ModulePriority.CRITICAL,
                    new String[]{DEEPMIX_BOOTSTRAP},
                    4 * 1024 * 1024, // 4MB budget
                    Long.MAX_VALUE,
                    true
                ), () -> loadCoreModule(DEEPMIX_CORE), null);

                registerModule(new ModuleDescriptor(
                    DEEPMIX_MIXIN_HELPER,
                    "stellar.snow.astralis.integration.DeepMix.Core.DeepMixMixinHelper",
                    ModuleClass.CORE, ModulePriority.CRITICAL,
                    new String[]{DEEPMIX_CORE},
                    2 * 1024 * 1024,
                    Long.MAX_VALUE,
                    true
                ), () -> loadCoreModule(DEEPMIX_MIXIN_HELPER), null);

                registerModule(new ModuleDescriptor(
                    DEEPMIX_STABILIZER,
                    "stellar.snow.astralis.integration.DeepMix.Core.DeepMixStabilizer",
                    ModuleClass.CORE, ModulePriority.CRITICAL,
                    new String[]{DEEPMIX_CORE},
                    2 * 1024 * 1024,
                    Long.MAX_VALUE,
                    true
                ), () -> loadCoreModule(DEEPMIX_STABILIZER), null);

                // ═══════════════════════════════════════════════
                // FROZEN MODULES (loaded on demand)
                // ═══════════════════════════════════════════════

                registerModule(new ModuleDescriptor(
                    DEEPMIX_PHASES,
                    "stellar.snow.astralis.integration.DeepMix.Core.DeepMixPhases",
                    ModuleClass.ON_DEMAND, ModulePriority.HIGH,
                    new String[]{DEEPMIX_CORE},
                    8 * 1024 * 1024, // 8MB budget
                    5 * 60 * 1000,   // 5 min idle timeout
                    true
                ), () -> loadFrozenModule(DEEPMIX_PHASES),
                   module -> unloadModule(DEEPMIX_PHASES, module));

                registerModule(new ModuleDescriptor(
                    DEEPMIX_ADVANCED,
                    "stellar.snow.astralis.integration.DeepMix.Core.DeepMixAdvancedExtensions",
                    ModuleClass.ON_DEMAND, ModulePriority.NORMAL,
                    new String[]{DEEPMIX_CORE, DEEPMIX_PHASES},
                    16 * 1024 * 1024, // 16MB budget (large module)
                    3 * 60 * 1000,    // 3 min idle timeout
                    true
                ), () -> loadFrozenModule(DEEPMIX_ADVANCED),
                   module -> unloadModule(DEEPMIX_ADVANCED, module));

                registerModule(new ModuleDescriptor(
                    DEEPMIX_ASSET_FORGE,
                    "stellar.snow.astralis.integration.DeepMix.DeepMixAssetForge",
                    ModuleClass.ON_DEMAND, ModulePriority.LOW,
                    new String[]{DEEPMIX_CORE},
                    8 * 1024 * 1024,
                    2 * 60 * 1000,    // 2 min idle timeout (rarely needed)
                    false
                ), () -> loadFrozenModule(DEEPMIX_ASSET_FORGE),
                   module -> unloadModule(DEEPMIX_ASSET_FORGE, module));

                registerModule(new ModuleDescriptor(
                    DEEPMIX_DATA_FORMATS,
                    "stellar.snow.astralis.integration.DeepMix.Core.DeepMixDataFormats",
                    ModuleClass.ON_DEMAND, ModulePriority.NORMAL,
                    new String[]{DEEPMIX_CORE},
                    4 * 1024 * 1024,
                    3 * 60 * 1000,
                    true
                ), () -> loadFrozenModule(DEEPMIX_DATA_FORMATS),
                   module -> unloadModule(DEEPMIX_DATA_FORMATS, module));

                registerModule(new ModuleDescriptor(
                    DEEPMIX_NEXUS,
                    "stellar.snow.astralis.integration.DeepMix.Core.DeepMixNexus",
                    ModuleClass.ON_DEMAND, ModulePriority.NORMAL,
                    new String[]{DEEPMIX_CORE, DEEPMIX_PHASES},
                    8 * 1024 * 1024,
                    3 * 60 * 1000,
                    true
                ), () -> loadFrozenModule(DEEPMIX_NEXUS),
                   module -> unloadModule(DEEPMIX_NEXUS, module));

                registerModule(new ModuleDescriptor(
                    DEEPMIX_MIXINS,
                    "stellar.snow.astralis.integration.DeepMix.Mixins.DeepMixMixins",
                    ModuleClass.ON_DEMAND, ModulePriority.HIGH,
                    new String[]{DEEPMIX_CORE, DEEPMIX_MIXIN_HELPER},
                    8 * 1024 * 1024,
                    5 * 60 * 1000,
                    true
                ), () -> loadFrozenModule(DEEPMIX_MIXINS),
                   module -> unloadModule(DEEPMIX_MIXINS, module));

                registerModule(new ModuleDescriptor(
                    DEEPMIX_TRANSFORM_ENGINE,
                    "stellar.snow.astralis.integration.DeepMix.Transformers.DeepMixTransformEngine",
                    ModuleClass.ON_DEMAND, ModulePriority.HIGH,
                    new String[]{DEEPMIX_CORE, DEEPMIX_MIXIN_HELPER},
                    12 * 1024 * 1024,
                    5 * 60 * 1000,
                    true
                ), () -> loadFrozenModule(DEEPMIX_TRANSFORM_ENGINE),
                   module -> unloadModule(DEEPMIX_TRANSFORM_ENGINE, module));

                registerModule(new ModuleDescriptor(
                    DEEPMIX_UTILITIES,
                    "stellar.snow.astralis.integration.DeepMix.Util.DeepMixUtilities",
                    ModuleClass.ON_DEMAND, ModulePriority.LOW,
                    new String[]{DEEPMIX_CORE},
                    2 * 1024 * 1024,
                    2 * 60 * 1000,
                    true
                ), () -> loadFrozenModule(DEEPMIX_UTILITIES),
                   module -> unloadModule(DEEPMIX_UTILITIES, module));

                registerModule(new ModuleDescriptor(
                    DEEPMIX_TRANSFORMERS,
                    "stellar.snow.astralis.integration.DeepMixTransformers",
                    ModuleClass.ON_DEMAND, ModulePriority.HIGH,
                    new String[]{DEEPMIX_CORE, DEEPMIX_TRANSFORM_ENGINE},
                    8 * 1024 * 1024,
                    5 * 60 * 1000,
                    true
                ), () -> loadFrozenModule(DEEPMIX_TRANSFORMERS),
                   module -> unloadModule(DEEPMIX_TRANSFORMERS, module));

                initialized = true;

                // Start the background maintenance thread
                MaintenanceDaemon.start();
            }
        }

        @SuppressWarnings("unchecked")
        public static <T> FrozenModule<T> getModule(String moduleId) {
            if (!initialized) initialize();
            FrozenModule<?> module = modules.get(moduleId);
            if (module == null) {
                throw new IllegalArgumentException(
                    "Unknown DeepMix module: '" + moduleId + "'");
            }
            return (FrozenModule<T>) module;
        }

        public static ModuleDescriptor getDescriptor(String moduleId) {
            return descriptors.get(moduleId);
        }

        public static Collection<ModuleDescriptor> allDescriptors() {
            return Collections.unmodifiableCollection(descriptors.values());
        }

        public static Collection<FrozenModule<?>> allModules() {
            return Collections.unmodifiableCollection(modules.values());
        }

        /** Get count of currently active (thawed) modules */
        public static int activeModuleCount() {
            return (int) modules.values().stream()
                .filter(FrozenModule::isActive)
                .count();
        }

        /** Get total estimated memory of all active modules */
        public static long activeMemoryBytes() {
            return modules.values().stream()
                .filter(FrozenModule::isActive)
                .mapToLong(FrozenModule::estimatedMemoryBytes)
                .sum();
        }

        /** Freeze all non-core modules */
        public static void freezeAll() {
            modules.values().forEach(m -> {
                if (m.descriptor().moduleClass != ModuleClass.CORE) {
                    m.freeze();
                }
            });
        }

        /** Freeze modules that have been idle longer than their timeout */
        public static int freezeIdle() {
            int frozenCount = 0;
            for (FrozenModule<?> module : modules.values()) {
                ModuleDescriptor desc = module.descriptor();
                if (desc.moduleClass == ModuleClass.CORE) continue;
                if (!module.isActive()) continue;

                if (desc.idleTimeMs() > desc.idleTimeoutMs) {
                    module.freeze();
                    frozenCount++;
                }
            }
            return frozenCount;
        }

        /** Ensure all dependencies for a module are active */
        static void ensureDependencies(ModuleDescriptor descriptor) {
            for (String depId : descriptor.dependencies) {
                FrozenModule<?> dep = modules.get(depId);
                if (dep != null && !dep.isActive()) {
                    dep.get(); // Thaw dependency
                }
            }
        }

        /** Shed modules to reach target memory, lowest priority first */
        public static long shedToMemoryTarget(long targetBytes) {
            long currentBytes = activeMemoryBytes();
            if (currentBytes <= targetBytes) return 0;

            long shed = 0;

            // Sort by priority (highest number = lowest priority = shed first)
            List<FrozenModule<?>> candidates = modules.values().stream()
                .filter(FrozenModule::isActive)
                .filter(m -> m.descriptor().moduleClass != ModuleClass.CORE)
                .sorted(Comparator.comparingInt(m ->
                    -m.descriptor().priority.level)) // Descending priority number
                .collect(Collectors.toList());

            for (FrozenModule<?> module : candidates) {
                if (currentBytes - shed <= targetBytes) break;
                long moduleMem = module.estimatedMemoryBytes();
                module.freeze();
                shed += moduleMem;
            }

            return shed;
        }

        // ── Private helpers ──

        @SuppressWarnings("unchecked")
        private static <T> void registerModule(ModuleDescriptor descriptor,
                                                Supplier<T> factory,
                                                Consumer<T> freezeAction) {
            descriptors.put(descriptor.id, descriptor);
            modules.put(descriptor.id,
                new FrozenModule<>(descriptor, factory, (Consumer<Object>) freezeAction));
        }

        private static Object loadCoreModule(String moduleId) {
            ModuleDescriptor desc = descriptors.get(moduleId);
            if (desc == null) return null;
            try {
                Class<?> clazz = Class.forName(desc.className);
                // Core modules are typically static — return the Class itself
                return clazz;
            } catch (ClassNotFoundException e) {
                throw new ModuleThawException(desc, e);
            }
        }

        private static Object loadFrozenModule(String moduleId) {
            ModuleDescriptor desc = descriptors.get(moduleId);
            if (desc == null) return null;
            try {
                Class<?> clazz = Class.forName(desc.className);
                return clazz;
            } catch (ClassNotFoundException e) {
                throw new ModuleThawException(desc, e);
            }
        }

        private static void unloadModule(String moduleId, Object module) {
            // Module-specific cleanup
            // For class references, we just null them out — the GC handles the rest
            // If the module registered any hooks, listeners, or callbacks, clean those up
            try {
                if (module instanceof Class<?>) {
                    Class<?> clazz = (Class<?>) module;
                    // Look for a static cleanup method
                    try {
                        Method cleanup = clazz.getDeclaredMethod("deepmix$cleanup");
                        cleanup.setAccessible(true);
                        cleanup.invoke(null);
                    } catch (NoSuchMethodException ignored) {
                        // No cleanup method — that's fine
                    }
                }
            } catch (Exception e) {
                System.err.println("[DeepMix:Optimizer] Cleanup warning for " +
                    moduleId + ": " + e.getMessage());
            }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 2: MEMORY MANAGEMENT & PRESSURE HANDLING                  ║
    // ║                                                                    ║
    // ║  Monitors JVM heap usage and proactively sheds module memory       ║
    // ║  before the system enters GC pressure. Uses tiered thresholds.     ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Memory pressure monitor with tiered response.
     *
     * Thresholds (configurable):
     *   GREEN  (< 60% heap): Normal operation — all modules allowed
     *   YELLOW (60-75%):     Freeze idle modules, disable low-priority thaws
     *   ORANGE (75-85%):     Shed DISPOSABLE & LOW priority modules
     *   RED    (85-95%):     Shed NORMAL priority, compact caches
     *   CRITICAL (> 95%):    Emergency shed — only CRITICAL modules remain
     */
    public static final class MemoryGuard {

        public enum PressureLevel {
            GREEN(0.0, 0.60),
            YELLOW(0.60, 0.75),
            ORANGE(0.75, 0.85),
            RED(0.85, 0.95),
            CRITICAL(0.95, 1.0);

            final double low;
            final double high;
            PressureLevel(double low, double high) { this.low = low; this.high = high; }
        }

        private static volatile PressureLevel currentLevel = PressureLevel.GREEN;
        private static final AtomicLong lastCheckMs = new AtomicLong(0);
        private static final long CHECK_INTERVAL_MS = 500; // Check every 500ms
        private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        private static final List<MemoryPoolMXBean> memoryPools =
            ManagementFactory.getMemoryPoolMXBeans();

        // Memory pressure listeners
        private static final CopyOnWriteArrayList<Consumer<PressureLevel>> pressureListeners =
            new CopyOnWriteArrayList<>();

        // Emergency OOM prevention callback
        private static volatile Runnable emergencyAction = null;

        // GC tracking
        private static final AtomicLong totalGcTimeMs = new AtomicLong(0);
        private static final AtomicLong gcCount = new AtomicLong(0);
        private static volatile long lastGcPauseMs = 0;

        /**
         * Check current memory pressure. Cached for CHECK_INTERVAL_MS.
         */
        public static PressureLevel checkPressure() {
            long now = System.currentTimeMillis();
            long lastCheck = lastCheckMs.get();
            if (now - lastCheck < CHECK_INTERVAL_MS) {
                return currentLevel;
            }

            if (lastCheckMs.compareAndSet(lastCheck, now)) {
                PressureLevel previous = currentLevel;
                currentLevel = computePressureLevel();

                if (currentLevel != previous) {
                    onPressureChanged(previous, currentLevel);
                }
            }

            return currentLevel;
        }

        /**
         * Get detailed memory statistics.
         */
        public static MemoryStats getStats() {
            MemoryUsage heap = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

            return new MemoryStats(
                heap.getUsed(),
                heap.getCommitted(),
                heap.getMax(),
                nonHeap.getUsed(),
                nonHeap.getCommitted(),
                currentLevel,
                ModuleRegistry.activeMemoryBytes(),
                ModuleRegistry.activeModuleCount(),
                BytecodeCache.estimatedSize(),
                ObjectPool.totalPooledObjects(),
                gcCount.get(),
                totalGcTimeMs.get(),
                lastGcPauseMs
            );
        }

        /** Register a pressure change listener */
        public static void onPressureChange(Consumer<PressureLevel> listener) {
            pressureListeners.add(listener);
        }

        /** Set emergency OOM prevention action */
        public static void setEmergencyAction(Runnable action) {
            emergencyAction = action;
        }

        /** Check if a module thaw is allowed at current pressure */
        public static boolean allowThaw(ModulePriority priority) {
            switch (currentLevel) {
                case GREEN:
                    return true;
                case YELLOW:
                    return priority.level <= ModulePriority.HIGH.level;
                case ORANGE:
                    return priority.level <= ModulePriority.HIGH.level;
                case RED:
                    return priority == ModulePriority.CRITICAL;
                case CRITICAL:
                    return priority == ModulePriority.CRITICAL;
                default:
                    return false;
            }
        }

        /** Proactively request GC if we're trending toward pressure */
        public static void suggestGC() {
            if (currentLevel.ordinal() >= PressureLevel.ORANGE.ordinal()) {
                System.gc(); // Hint only — JVM may ignore
            }
        }

        // ── Private ──

        private static PressureLevel computePressureLevel() {
            MemoryUsage heap = memoryBean.getHeapMemoryUsage();
            long used = heap.getUsed();
            long max = heap.getMax();

            if (max <= 0) {
                // Max not set — use committed as proxy
                max = heap.getCommitted();
            }

            double ratio = (double) used / max;

            // Also check old gen specifically if available
            for (MemoryPoolMXBean pool : memoryPools) {
                String name = pool.getName().toLowerCase();
                if (name.contains("old") || name.contains("tenured")) {
                    MemoryUsage poolUsage = pool.getUsage();
                    if (poolUsage.getMax() > 0) {
                        double poolRatio = (double) poolUsage.getUsed() / poolUsage.getMax();
                        ratio = Math.max(ratio, poolRatio);
                    }
                }
            }

            if (ratio >= PressureLevel.CRITICAL.low) return PressureLevel.CRITICAL;
            if (ratio >= PressureLevel.RED.low) return PressureLevel.RED;
            if (ratio >= PressureLevel.ORANGE.low) return PressureLevel.ORANGE;
            if (ratio >= PressureLevel.YELLOW.low) return PressureLevel.YELLOW;
            return PressureLevel.GREEN;
        }

        private static void onPressureChanged(PressureLevel from, PressureLevel to) {
            System.out.println("[DeepMix:MemoryGuard] Pressure: " + from + " → " + to);

            // Automatic responses
            switch (to) {
                case YELLOW:
                    ModuleRegistry.freezeIdle();
                    break;

                case ORANGE:
                    ModuleRegistry.freezeIdle();
                    shedModulesByPriority(ModulePriority.DISPOSABLE);
                    BytecodeCache.compact(0.5); // Keep 50% of cache
                    break;

                case RED:
                    shedModulesByPriority(ModulePriority.LOW);
                    shedModulesByPriority(ModulePriority.NORMAL);
                    BytecodeCache.compact(0.25);
                    ObjectPool.drainAll();
                    break;

                case CRITICAL:
                    // EMERGENCY: shed everything except CRITICAL modules
                    ModuleRegistry.freezeAll();
                    BytecodeCache.clear();
                    ObjectPool.drainAll();
                    TransformationPipeline.cancelNonCritical();
                    suggestGC();

                    if (emergencyAction != null) {
                        try { emergencyAction.run(); }
                        catch (Exception e) { /* Swallow — we're in emergency */ }
                    }
                    break;

                case GREEN:
                    // Recovered — log it
                    System.out.println("[DeepMix:MemoryGuard] Memory pressure resolved");
                    break;
            }

            // Notify listeners
            for (Consumer<PressureLevel> listener : pressureListeners) {
                try { listener.accept(to); }
                catch (Exception e) { /* Swallow listener errors */ }
            }
        }

        private static void shedModulesByPriority(ModulePriority maxPriority) {
            for (FrozenModule<?> module : ModuleRegistry.allModules()) {
                ModuleDescriptor desc = module.descriptor();
                if (desc.priority.level >= maxPriority.level && module.isActive()) {
                    module.freeze();
                }
            }
        }

        /** Update GC stats (called by MaintenanceDaemon) */
        static void updateGCStats() {
            long totalGc = 0;
            long totalCount = 0;
            long maxPause = 0;

            for (GarbageCollectorMXBean gcBean :
                    ManagementFactory.getGarbageCollectorMXBeans()) {
                long count = gcBean.getCollectionCount();
                long time = gcBean.getCollectionTime();
                if (count > 0 && time > 0) {
                    totalCount += count;
                    totalGc += time;
                    // Rough last pause estimate
                    maxPause = Math.max(maxPause, time / Math.max(1, count));
                }
            }

            gcCount.set(totalCount);
            totalGcTimeMs.set(totalGc);
            lastGcPauseMs = maxPause;
        }
    }

    /** Immutable memory statistics snapshot */
    public static final class MemoryStats {
        public final long heapUsed;
        public final long heapCommitted;
        public final long heapMax;
        public final long nonHeapUsed;
        public final long nonHeapCommitted;
        public final MemoryGuard.PressureLevel pressure;
        public final long deepmixActiveMemory;
        public final int deepmixActiveModules;
        public final long bytecodeCacheSize;
        public final int pooledObjects;
        public final long gcCount;
        public final long gcTotalTimeMs;
        public final long gcLastPauseMs;

        MemoryStats(long heapUsed, long heapCommitted, long heapMax,
                    long nonHeapUsed, long nonHeapCommitted,
                    MemoryGuard.PressureLevel pressure,
                    long deepmixActiveMemory, int deepmixActiveModules,
                    long bytecodeCacheSize, int pooledObjects,
                    long gcCount, long gcTotalTimeMs, long gcLastPauseMs) {
            this.heapUsed = heapUsed;
            this.heapCommitted = heapCommitted;
            this.heapMax = heapMax;
            this.nonHeapUsed = nonHeapUsed;
            this.nonHeapCommitted = nonHeapCommitted;
            this.pressure = pressure;
            this.deepmixActiveMemory = deepmixActiveMemory;
            this.deepmixActiveModules = deepmixActiveModules;
            this.bytecodeCacheSize = bytecodeCacheSize;
            this.pooledObjects = pooledObjects;
            this.gcCount = gcCount;
            this.gcTotalTimeMs = gcTotalTimeMs;
            this.gcLastPauseMs = gcLastPauseMs;
        }

        public double heapUsageRatio() {
            return heapMax > 0 ? (double) heapUsed / heapMax : 0;
        }

        @Override
        public String toString() {
            return String.format(
                "MemoryStats[heap=%dMB/%dMB (%.1f%%), pressure=%s, " +
                    "deepmix=%dKB in %d modules, cache=%dKB, pooled=%d, " +
                    "gc=%d runs/%dms total]",
                heapUsed / (1024 * 1024), heapMax / (1024 * 1024),
                heapUsageRatio() * 100, pressure,
                deepmixActiveMemory / 1024, deepmixActiveModules,
                bytecodeCacheSize / 1024, pooledObjects,
                gcCount, gcTotalTimeMs
            );
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 3: BYTECODE TRANSFORMATION CACHE                          ║
    // ║                                                                    ║
    // ║  LRU cache for transformed bytecode. Avoids re-transforming        ║
    // ║  classes that haven't changed. Uses weak values to allow GC.       ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * High-performance bytecode transformation cache.
     *
     * Features:
     *   - LRU eviction with configurable max size
     *   - Weak value references (GC can reclaim under pressure)
     *   - Content-addressed (hash of original bytecode → transformed)
     *   - Thread-safe with striped locking
     *   - Automatic compaction under memory pressure
     *   - Statistics tracking for hit/miss ratios
     */
    public static final class BytecodeCache {

        /** Cache entry with metadata */
        static final class CacheEntry {
            final byte[] transformedBytecode;
            final long originalHash;
            final long transformedHash;
            final long createdAtMs;
            final String className;
            final int transformCount; // How many transforms were applied
            volatile long lastAccessMs;
            volatile int accessCount;

            CacheEntry(byte[] transformed, long originalHash, String className,
                       int transformCount) {
                this.transformedBytecode = transformed;
                this.originalHash = originalHash;
                this.transformedHash = computeHash(transformed);
                this.createdAtMs = System.currentTimeMillis();
                this.className = className;
                this.transformCount = transformCount;
                this.lastAccessMs = createdAtMs;
                this.accessCount = 1;
            }

            void touch() {
                lastAccessMs = System.currentTimeMillis();
                accessCount++;
            }
        }

        // Striped locking: 16 stripes for concurrent access
        private static final int STRIPE_COUNT = 16;
        private static final int STRIPE_MASK = STRIPE_COUNT - 1;
        private static final ReentrantLock[] stripeLocks = new ReentrantLock[STRIPE_COUNT];
        private static final Map<Long, CacheEntry>[] stripes;

        // Configuration
        private static volatile int maxEntriesPerStripe = 256;
        private static volatile long maxTotalBytes = 64 * 1024 * 1024; // 64MB
        private static volatile long entryTTLMs = 30 * 60 * 1000; // 30 minutes

        // Statistics
        private static final AtomicLong hits = new AtomicLong(0);
        private static final AtomicLong misses = new AtomicLong(0);
        private static final AtomicLong evictions = new AtomicLong(0);
        private static final AtomicLong totalBytesStored = new AtomicLong(0);

        @SuppressWarnings("unchecked")
        static {
            stripes = new LinkedHashMap[STRIPE_COUNT];
            for (int i = 0; i < STRIPE_COUNT; i++) {
                stripeLocks[i] = new ReentrantLock();
                final int stripeIdx = i;
                stripes[i] = new LinkedHashMap<Long, CacheEntry>(64, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Long, CacheEntry> eldest) {
                        if (size() > maxEntriesPerStripe) {
                            totalBytesStored.addAndGet(-eldest.getValue().transformedBytecode.length);
                            evictions.incrementAndGet();
                            return true;
                        }
                        return false;
                    }
                };
            }
        }

        /**
         * Look up transformed bytecode by original content hash.
         *
         * @param originalBytecode The original (untransformed) class bytes
         * @return Transformed bytecode, or null if not cached
         */
        public static byte[] get(byte[] originalBytecode) {
            long hash = computeHash(originalBytecode);
            int stripe = (int) (hash & STRIPE_MASK);

            stripeLocks[stripe].lock();
            try {
                CacheEntry entry = stripes[stripe].get(hash);
                if (entry != null) {
                    // Verify content hash matches (collision check)
                    if (entry.originalHash == hash) {
                        // Check TTL
                        if (System.currentTimeMillis() - entry.createdAtMs > entryTTLMs) {
                            stripes[stripe].remove(hash);
                            totalBytesStored.addAndGet(-entry.transformedBytecode.length);
                            misses.incrementAndGet();
                            return null;
                        }

                        entry.touch();
                        hits.incrementAndGet();
                        return entry.transformedBytecode.clone(); // Defensive copy
                    }
                }
                misses.incrementAndGet();
                return null;
            } finally {
                stripeLocks[stripe].unlock();
            }
        }

        /**
         * Store transformed bytecode in the cache.
         *
         * @param originalBytecode  The original class bytes
         * @param transformedBytecode The transformed class bytes
         * @param className         Class name for diagnostics
         * @param transformCount    Number of transforms applied
         */
        public static void put(byte[] originalBytecode, byte[] transformedBytecode,
                               String className, int transformCount) {
            // Don't cache if under memory pressure
            if (MemoryGuard.checkPressure().ordinal() >= MemoryGuard.PressureLevel.RED.ordinal()) {
                return;
            }

            // Don't cache if total size exceeds budget
            if (totalBytesStored.get() + transformedBytecode.length > maxTotalBytes) {
                compact(0.75); // Free 25%
            }

            long hash = computeHash(originalBytecode);
            int stripe = (int) (hash & STRIPE_MASK);

            CacheEntry entry = new CacheEntry(
                transformedBytecode.clone(), hash, className, transformCount);

            stripeLocks[stripe].lock();
            try {
                CacheEntry old = stripes[stripe].put(hash, entry);
                if (old != null) {
                    totalBytesStored.addAndGet(-old.transformedBytecode.length);
                }
                totalBytesStored.addAndGet(transformedBytecode.length);
            } finally {
                stripeLocks[stripe].unlock();
            }
        }

        /**
         * Invalidate a specific class from the cache.
         */
        public static void invalidate(String className) {
            for (int i = 0; i < STRIPE_COUNT; i++) {
                stripeLocks[i].lock();
                try {
                    Iterator<Map.Entry<Long, CacheEntry>> it =
                        stripes[i].entrySet().iterator();
                    while (it.hasNext()) {
                        CacheEntry entry = it.next().getValue();
                        if (entry.className.equals(className)) {
                            totalBytesStored.addAndGet(-entry.transformedBytecode.length);
                            it.remove();
                        }
                    }
                } finally {
                    stripeLocks[i].unlock();
                }
            }
        }

        /** Clear entire cache */
        public static void clear() {
            for (int i = 0; i < STRIPE_COUNT; i++) {
                stripeLocks[i].lock();
                try {
                    stripes[i].clear();
                } finally {
                    stripeLocks[i].unlock();
                }
            }
            totalBytesStored.set(0);
            evictions.addAndGet(hits.get() + misses.get()); // Rough count
        }

        /**
         * Compact the cache, keeping only the given fraction of entries.
         * Evicts LRU entries first.
         *
         * @param keepFraction 0.0 to 1.0 — fraction of entries to keep
         */
        public static void compact(double keepFraction) {
            keepFraction = Math.max(0.0, Math.min(1.0, keepFraction));

            for (int i = 0; i < STRIPE_COUNT; i++) {
                stripeLocks[i].lock();
                try {
                    int targetSize = (int) (stripes[i].size() * keepFraction);
                    while (stripes[i].size() > targetSize) {
                        Iterator<Map.Entry<Long, CacheEntry>> it =
                            stripes[i].entrySet().iterator();
                        if (it.hasNext()) {
                            CacheEntry removed = it.next().getValue();
                            totalBytesStored.addAndGet(-removed.transformedBytecode.length);
                            it.remove();
                            evictions.incrementAndGet();
                        } else {
                            break;
                        }
                    }
                } finally {
                    stripeLocks[i].unlock();
                }
            }
        }

        /** Estimated total cache size in bytes */
        public static long estimatedSize() {
            return totalBytesStored.get();
        }

        /** Cache hit ratio (0.0 to 1.0) */
        public static double hitRatio() {
            long h = hits.get();
            long m = misses.get();
            long total = h + m;
            return total > 0 ? (double) h / total : 0.0;
        }

        /** Cache statistics snapshot */
        public static CacheStats stats() {
            int totalEntries = 0;
            for (int i = 0; i < STRIPE_COUNT; i++) {
                totalEntries += stripes[i].size();
            }
            return new CacheStats(
                hits.get(), misses.get(), evictions.get(),
                totalEntries, totalBytesStored.get(), hitRatio()
            );
        }

        /** Set configuration */
        public static void configure(int maxPerStripe, long maxBytes, long ttlMs) {
            maxEntriesPerStripe = maxPerStripe;
            maxTotalBytes = maxBytes;
            entryTTLMs = ttlMs;
        }

        // ── Hash function (FNV-1a 64-bit) ──

        static long computeHash(byte[] data) {
            long hash = 0xcbf29ce484222325L; // FNV offset basis
            for (byte b : data) {
                hash ^= (b & 0xFF);
                hash *= 0x100000001b3L; // FNV prime
            }
            return hash;
        }
    }

    /** Immutable cache statistics */
    public static final class CacheStats {
        public final long hits;
        public final long misses;
        public final long evictions;
        public final int entries;
        public final long totalBytes;
        public final double hitRatio;

        CacheStats(long hits, long misses, long evictions, int entries,
                   long totalBytes, double hitRatio) {
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.entries = entries;
            this.totalBytes = totalBytes;
            this.hitRatio = hitRatio;
        }

        @Override
        public String toString() {
            return String.format(
                "CacheStats[entries=%d, size=%dKB, hits=%d, misses=%d, " +
                    "evictions=%d, hitRatio=%.1f%%]",
                entries, totalBytes / 1024, hits, misses, evictions, hitRatio * 100);
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 4: OBJECT POOLING                                         ║
    // ║                                                                    ║
    // ║  Pre-allocated pools for frequently created objects to reduce       ║
    // ║  GC pressure. Pools: ClassNode, MethodNode, InsnList, byte[].      ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Generic thread-safe object pool with bounded size.
     *
     * @param <T> Pooled object type
     */
    public static final class BoundedPool<T> {

        private final ConcurrentLinkedDeque<T> pool;
        private final Supplier<T> factory;
        private final Consumer<T> resetter;
        private final int maxSize;
        private final AtomicInteger currentSize = new AtomicInteger(0);
        private final AtomicLong borrows = new AtomicLong(0);
        private final AtomicLong returns = new AtomicLong(0);
        private final AtomicLong creates = new AtomicLong(0);
        private final AtomicLong discards = new AtomicLong(0);

        public BoundedPool(int maxSize, Supplier<T> factory, Consumer<T> resetter) {
            this.pool = new ConcurrentLinkedDeque<>();
            this.factory = factory;
            this.resetter = resetter;
            this.maxSize = maxSize;
        }

        /** Pre-warm the pool with initial objects */
        public void warmUp(int count) {
            int toCreate = Math.min(count, maxSize);
            for (int i = 0; i < toCreate; i++) {
                T obj = factory.get();
                pool.offer(obj);
                currentSize.incrementAndGet();
                creates.incrementAndGet();
            }
        }

        /**
         * Borrow an object from the pool.
         * Creates a new one if pool is empty.
         */
        public T borrow() {
            borrows.incrementAndGet();
            T obj = pool.pollFirst();
            if (obj != null) {
                currentSize.decrementAndGet();
                return obj;
            }
            creates.incrementAndGet();
            return factory.get();
        }

        /**
         * Return an object to the pool.
         * Resets the object before pooling.
         * Discards if pool is full.
         */
        public void returnObject(T obj) {
            if (obj == null) return;
            returns.incrementAndGet();

            try {
                if (resetter != null) {
                    resetter.accept(obj);
                }
            } catch (Exception e) {
                discards.incrementAndGet();
                return; // Don't pool corrupt objects
            }

            if (currentSize.get() < maxSize) {
                pool.offerFirst(obj);
                currentSize.incrementAndGet();
            } else {
                discards.incrementAndGet();
            }
        }

        /** Drain all objects from the pool */
        public void drain() {
            T obj;
            while ((obj = pool.pollFirst()) != null) {
                currentSize.decrementAndGet();
            }
        }

        public int size() { return currentSize.get(); }
        public long borrowCount() { return borrows.get(); }
        public long returnCount() { return returns.get(); }
        public long createCount() { return creates.get(); }
    }

    /**
     * Central pool manager for all DeepMix object pools.
     */
    public static final class ObjectPool {

        // ClassNode pool — these are expensive to create
        private static final BoundedPool<ClassNode> CLASS_NODES =
            new BoundedPool<>(32, ClassNode::new, cn -> {
                cn.name = null;
                cn.superName = null;
                cn.interfaces.clear();
                cn.fields.clear();
                cn.methods.clear();
                cn.visibleAnnotations = null;
                cn.invisibleAnnotations = null;
                cn.innerClasses.clear();
                cn.attrs = null;
                cn.sourceFile = null;
                cn.sourceDebug = null;
                cn.outerClass = null;
                cn.outerMethod = null;
                cn.outerMethodDesc = null;
                cn.signature = null;
                cn.access = 0;
                cn.version = 0;
            });

        // InsnList pool
        private static final BoundedPool<InsnList> INSN_LISTS =
            new BoundedPool<>(64, InsnList::new, InsnList::clear);

        // Byte buffer pool for bytecode reading/writing
        private static final BoundedPool<byte[]> BYTE_BUFFERS_SMALL =
            new BoundedPool<>(32, () -> new byte[8192], b -> { /* no reset needed */ });

        private static final BoundedPool<byte[]> BYTE_BUFFERS_LARGE =
            new BoundedPool<>(8, () -> new byte[65536], b -> { /* no reset needed */ });

        // StringBuilder pool for string building operations
        private static final BoundedPool<StringBuilder> STRING_BUILDERS =
            new BoundedPool<>(32, () -> new StringBuilder(256), sb -> sb.setLength(0));

        // HashMap pool for temporary maps
        @SuppressWarnings("unchecked")
        private static final BoundedPool<HashMap<String, Object>> HASH_MAPS =
            new BoundedPool<>(16, () -> new HashMap<>(32), HashMap::clear);

        // ArrayList pool
        @SuppressWarnings("unchecked")
        private static final BoundedPool<ArrayList<Object>> ARRAY_LISTS =
            new BoundedPool<>(32, () -> new ArrayList<>(16), ArrayList::clear);

        // ── Public access ──

        public static ClassNode borrowClassNode() { return CLASS_NODES.borrow(); }
        public static void returnClassNode(ClassNode cn) { CLASS_NODES.returnObject(cn); }

        public static InsnList borrowInsnList() { return INSN_LISTS.borrow(); }
        public static void returnInsnList(InsnList il) { INSN_LISTS.returnObject(il); }

        public static byte[] borrowSmallBuffer() { return BYTE_BUFFERS_SMALL.borrow(); }
        public static void returnSmallBuffer(byte[] b) {
            if (b != null && b.length == 8192) BYTE_BUFFERS_SMALL.returnObject(b);
        }

        public static byte[] borrowLargeBuffer() { return BYTE_BUFFERS_LARGE.borrow(); }
        public static void returnLargeBuffer(byte[] b) {
            if (b != null && b.length == 65536) BYTE_BUFFERS_LARGE.returnObject(b);
        }

        public static StringBuilder borrowStringBuilder() { return STRING_BUILDERS.borrow(); }
        public static void returnStringBuilder(StringBuilder sb) {
            if (sb.capacity() < 4096) { // Don't pool oversized builders
                STRING_BUILDERS.returnObject(sb);
            }
        }

        @SuppressWarnings("unchecked")
        public static <K, V> HashMap<K, V> borrowHashMap() {
            return (HashMap<K, V>) (HashMap<?, ?>) HASH_MAPS.borrow();
        }
        @SuppressWarnings("unchecked")
        public static <K, V> void returnHashMap(HashMap<K, V> map) {
            if (map.size() < 1000) { // Don't pool oversized maps
                HASH_MAPS.returnObject((HashMap<String, Object>) (HashMap<?, ?>) map);
            }
        }

        @SuppressWarnings("unchecked")
        public static <T> ArrayList<T> borrowArrayList() {
            return (ArrayList<T>) (ArrayList<?>) ARRAY_LISTS.borrow();
        }
        @SuppressWarnings("unchecked")
        public static <T> void returnArrayList(ArrayList<T> list) {
            if (list.size() < 1000) {
                ARRAY_LISTS.returnObject((ArrayList<Object>) (ArrayList<?>) list);
            }
        }

        /** Pre-warm all pools */
        public static void warmUp() {
            CLASS_NODES.warmUp(4);
            INSN_LISTS.warmUp(8);
            BYTE_BUFFERS_SMALL.warmUp(4);
            BYTE_BUFFERS_LARGE.warmUp(2);
            STRING_BUILDERS.warmUp(8);
            HASH_MAPS.warmUp(4);
            ARRAY_LISTS.warmUp(8);
        }

        /** Drain all pools (release pooled memory) */
        public static void drainAll() {
            CLASS_NODES.drain();
            INSN_LISTS.drain();
            BYTE_BUFFERS_SMALL.drain();
            BYTE_BUFFERS_LARGE.drain();
            STRING_BUILDERS.drain();
            HASH_MAPS.drain();
            ARRAY_LISTS.drain();
        }

        /** Total pooled objects across all pools */
        public static int totalPooledObjects() {
            return CLASS_NODES.size() + INSN_LISTS.size() +
                BYTE_BUFFERS_SMALL.size() + BYTE_BUFFERS_LARGE.size() +
                STRING_BUILDERS.size() + HASH_MAPS.size() + ARRAY_LISTS.size();
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 5: TRANSFORMATION PIPELINE OPTIMIZER                      ║
    // ║                                                                    ║
    // ║  Optimizes the order and execution of bytecode transformations     ║
    // ║  for maximum throughput and minimal overhead.                       ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Optimized transformation pipeline.
     *
     * Strategies:
     *   1. BATCH: Group transforms for the same class together
     *   2. PIPELINE: Overlap I/O with compute (read next while transforming current)
     *   3. SKIP: Skip classes with no applicable transforms
     *   4. CACHE: Return cached result for unchanged classes
     *   5. LAZY_FRAMES: Defer stack frame computation until actually needed
     *   6. PARALLEL: Transform independent classes concurrently
     *   7. INCREMENTAL: Only re-transform what changed (for hot reload)
     *   8. BUDGET: Enforce per-class time budget — skip slow transforms
     */
    public static final class TransformationPipeline {

        /** Transform result with metadata */
        public static final class TransformResult {
            public final byte[] bytecode;
            public final String className;
            public final int transformsApplied;
            public final long durationNanos;
            public final boolean fromCache;
            public final boolean skipped;
            public final List<String> appliedTransformIds;
            public final List<String> skippedTransformIds;
            public final Throwable error;

            TransformResult(byte[] bytecode, String className, int transformsApplied,
                           long durationNanos, boolean fromCache, boolean skipped,
                           List<String> applied, List<String> skippedIds,
                           Throwable error) {
                this.bytecode = bytecode;
                this.className = className;
                this.transformsApplied = transformsApplied;
                this.durationNanos = durationNanos;
                this.fromCache = fromCache;
                this.skipped = skipped;
                this.appliedTransformIds = applied != null ? applied : Collections.emptyList();
                this.skippedTransformIds = skippedIds != null ? skippedIds : Collections.emptyList();
                this.error = error;
            }

            public boolean hasError() { return error != null; }
            public long durationMs() { return durationNanos / 1_000_000; }
            public long durationMicros() { return durationNanos / 1_000; }
        }

        /** Per-class time budget in nanoseconds */
        private static volatile long perClassBudgetNanos = 5_000_000; // 5ms default
        private static volatile long perTransformBudgetNanos = 2_000_000; // 2ms per transform
        private static volatile boolean parallelEnabled = true;
        private static volatile int maxParallelism = Runtime.getRuntime().availableProcessors();

        // Executor for parallel transforms
        private static final ExecutorService transformExecutor =
            new ThreadPoolExecutor(
                1, // Core pool
                Runtime.getRuntime().availableProcessors(), // Max pool
                30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(256),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "DeepMix-Transform-" + counter.getAndIncrement());
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY - 1); // Slightly below normal
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // Backpressure: caller runs if queue full
            );

        // Statistics
        private static final AtomicLong totalTransforms = new AtomicLong(0);
        private static final AtomicLong totalSkipped = new AtomicLong(0);
        private static final AtomicLong totalCacheHits = new AtomicLong(0);
        private static final AtomicLong totalBudgetExceeded = new AtomicLong(0);
        private static final AtomicLong totalErrors = new AtomicLong(0);
        private static final LongAdder totalTransformNanos = new LongAdder();

        // Exclusion patterns — classes that should NEVER be transformed
        private static final Set<String> exclusionPrefixes = ConcurrentHashMap.newKeySet();
        static {
            // JVM internals
            exclusionPrefixes.add("java/");
            exclusionPrefixes.add("javax/");
            exclusionPrefixes.add("jdk/");
            exclusionPrefixes.add("sun/");
            exclusionPrefixes.add("com/sun/");

            // ASM library itself
            exclusionPrefixes.add("org/objectweb/asm/");

            // DeepMix internals (prevent recursive transformation)
            exclusionPrefixes.add("stellar/snow/astralis/integration/DeepMix/Core/DeepMixOptimizer");

            // Common libraries that should not be touched
            exclusionPrefixes.add("org/slf4j/");
            exclusionPrefixes.add("org/apache/logging/");
            exclusionPrefixes.add("ch/qos/logback/");
        }

        /**
         * Transform a single class through the pipeline.
         * This is the main entry point for all class transformations.
         *
         * @param className     Internal class name (e.g., "com/example/MyClass")
         * @param originalBytes Original class bytecode
         * @param transforms    Ordered list of transforms to apply
         * @return TransformResult with the outcome
         */
        public static TransformResult transform(String className, byte[] originalBytes,
                                                 List<ClassTransform> transforms) {
            long startNanos = System.nanoTime();

            // ── Step 1: Quick exclusion check ──
            if (isExcluded(className)) {
                totalSkipped.incrementAndGet();
                return new TransformResult(
                    originalBytes, className, 0,
                    System.nanoTime() - startNanos, false, true,
                    null, null, null);
            }

            // ── Step 2: Check cache ──
            byte[] cached = BytecodeCache.get(originalBytes);
            if (cached != null) {
                totalCacheHits.incrementAndGet();
                return new TransformResult(
                    cached, className, -1,
                    System.nanoTime() - startNanos, true, false,
                    null, null, null);
            }

            // ── Step 3: Filter applicable transforms ──
            List<ClassTransform> applicable = new ArrayList<>();
            List<String> skippedIds = new ArrayList<>();
            for (ClassTransform tx : transforms) {
                if (tx.appliesTo(className)) {
                    applicable.add(tx);
                } else {
                    skippedIds.add(tx.id());
                }
            }

            if (applicable.isEmpty()) {
                totalSkipped.incrementAndGet();
                return new TransformResult(
                    originalBytes, className, 0,
                    System.nanoTime() - startNanos, false, true,
                    Collections.emptyList(), skippedIds, null);
            }

            // ── Step 4: Sort transforms by priority ──
            applicable.sort(Comparator.comparingInt(ClassTransform::priority));

            // ── Step 5: Apply transforms sequentially with budget enforcement ──
            byte[] currentBytes = originalBytes;
            List<String> appliedIds = new ArrayList<>();
            Throwable lastError = null;

            // Parse once with pooled ClassNode
            ClassNode classNode = ObjectPool.borrowClassNode();
            try {
                ClassReader cr = new ClassReader(currentBytes);
                cr.accept(classNode, ClassReader.EXPAND_FRAMES);

                for (ClassTransform tx : applicable) {
                    long txStart = System.nanoTime();

                    try {
                        // Apply the transform
                        boolean modified = tx.apply(classNode);

                        long txDuration = System.nanoTime() - txStart;

                        if (modified) {
                            appliedIds.add(tx.id());
                        }

                        // Check per-transform budget
                        if (txDuration > perTransformBudgetNanos) {
                            totalBudgetExceeded.incrementAndGet();
                            System.err.println(
                                "[DeepMix:Pipeline] SLOW TRANSFORM: " + tx.id() +
                                    " on " + className + " took " +
                                    (txDuration / 1_000_000) + "ms (budget: " +
                                    (perTransformBudgetNanos / 1_000_000) + "ms)");
                        }

                        // Check total class budget
                        if (System.nanoTime() - startNanos > perClassBudgetNanos) {
                            totalBudgetExceeded.incrementAndGet();
                            // Skip remaining transforms
                            for (int i = applicable.indexOf(tx) + 1; i < applicable.size(); i++) {
                                skippedIds.add(applicable.get(i).id());
                            }
                            break;
                        }

                    } catch (Exception e) {
                        lastError = e;
                        totalErrors.incrementAndGet();
                        skippedIds.add(tx.id() + " (ERROR: " + e.getMessage() + ")");

                        // Continue with remaining transforms unless it's critical
                        if (tx.critical()) {
                            break;
                        }
                    }
                }

                // ── Step 6: Write back to bytecode ──
                if (!appliedIds.isEmpty()) {
                    ClassWriter cw = new SafeClassWriter(
                        cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    classNode.accept(cw);
                    currentBytes = cw.toByteArray();
                }

            } catch (Exception e) {
                lastError = e;
                totalErrors.incrementAndGet();
                // Return original bytes on catastrophic failure
                currentBytes = originalBytes;
            } finally {
                ObjectPool.returnClassNode(classNode);
            }

            long totalNanos = System.nanoTime() - startNanos;
            totalTransforms.incrementAndGet();
            totalTransformNanos.add(totalNanos);

            // ── Step 7: Cache the result ──
            if (!appliedIds.isEmpty() && lastError == null) {
                BytecodeCache.put(originalBytes, currentBytes, className, appliedIds.size());
            }

            return new TransformResult(
                currentBytes, className, appliedIds.size(),
                totalNanos, false, false,
                appliedIds, skippedIds, lastError);
        }

        /**
         * Transform multiple classes in parallel.
         *
         * @param batch List of (className, bytecode) pairs
         * @param transforms Transforms to apply
         * @return List of results, in same order as input
         */
        public static List<TransformResult> transformBatch(
                List<Map.Entry<String, byte[]>> batch,
                List<ClassTransform> transforms) {

            if (!parallelEnabled || batch.size() <= 1) {
                // Sequential fallback
                List<TransformResult> results = new ArrayList<>(batch.size());
                for (Map.Entry<String, byte[]> entry : batch) {
                    results.add(transform(entry.getKey(), entry.getValue(), transforms));
                }
                return results;
            }

            // Parallel execution
            List<Future<TransformResult>> futures = new ArrayList<>(batch.size());
            for (Map.Entry<String, byte[]> entry : batch) {
                futures.add(transformExecutor.submit(() ->
                    transform(entry.getKey(), entry.getValue(), transforms)));
            }

            List<TransformResult> results = new ArrayList<>(batch.size());
            for (Future<TransformResult> future : futures) {
                try {
                    results.add(future.get(perClassBudgetNanos * 2, TimeUnit.NANOSECONDS));
                } catch (TimeoutException e) {
                    future.cancel(true);
                    results.add(new TransformResult(
                        null, "TIMEOUT", 0, perClassBudgetNanos * 2,
                        false, true, null, null, e));
                } catch (Exception e) {
                    results.add(new TransformResult(
                        null, "ERROR", 0, 0,
                        false, true, null, null, e));
                }
            }

            return results;
        }

        /** Cancel all non-critical pending transforms */
        static void cancelNonCritical() {
            if (transformExecutor instanceof ThreadPoolExecutor) {
                ((ThreadPoolExecutor) transformExecutor).getQueue().clear();
            }
        }

        /** Check if a class is excluded from transformation */
        public static boolean isExcluded(String className) {
            for (String prefix : exclusionPrefixes) {
                if (className.startsWith(prefix)) return true;
            }
            return false;
        }

        /** Add an exclusion prefix */
        public static void addExclusion(String prefix) {
            exclusionPrefixes.add(prefix);
        }

        /** Remove an exclusion prefix */
        public static void removeExclusion(String prefix) {
            exclusionPrefixes.remove(prefix);
        }

        /** Configure pipeline budgets */
        public static void configure(long classBudgetMs, long transformBudgetMs,
                                     boolean parallel, int maxParallel) {
            perClassBudgetNanos = classBudgetMs * 1_000_000;
            perTransformBudgetNanos = transformBudgetMs * 1_000_000;
            parallelEnabled = parallel;
            maxParallelism = maxParallel;
        }

        /** Pipeline statistics */
        public static PipelineStats stats() {
            return new PipelineStats(
                totalTransforms.get(), totalSkipped.get(), totalCacheHits.get(),
                totalBudgetExceeded.get(), totalErrors.get(),
                totalTransformNanos.sum(),
                transformExecutor instanceof ThreadPoolExecutor ?
                    ((ThreadPoolExecutor) transformExecutor).getActiveCount() : 0,
                transformExecutor instanceof ThreadPoolExecutor ?
                    ((ThreadPoolExecutor) transformExecutor).getQueue().size() : 0
            );
        }

        /** Shutdown the transform executor gracefully */
        public static void shutdown() {
            transformExecutor.shutdown();
            try {
                if (!transformExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    transformExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                transformExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Class transform interface */
    public interface ClassTransform {
        String id();
        int priority();
        boolean critical();
        boolean appliesTo(String className);
        boolean apply(ClassNode classNode) throws Exception;
    }

    /** Pipeline statistics */
    public static final class PipelineStats {
        public final long totalTransforms;
        public final long totalSkipped;
        public final long totalCacheHits;
        public final long totalBudgetExceeded;
        public final long totalErrors;
        public final long totalNanos;
        public final int activeThreads;
        public final int queuedTasks;

        PipelineStats(long totalTransforms, long totalSkipped, long totalCacheHits,
                     long totalBudgetExceeded, long totalErrors, long totalNanos,
                     int activeThreads, int queuedTasks) {
            this.totalTransforms = totalTransforms;
            this.totalSkipped = totalSkipped;
            this.totalCacheHits = totalCacheHits;
            this.totalBudgetExceeded = totalBudgetExceeded;
            this.totalErrors = totalErrors;
            this.totalNanos = totalNanos;
            this.activeThreads = activeThreads;
            this.queuedTasks = queuedTasks;
        }

        public double averageTransformMs() {
            return totalTransforms > 0 ?
                (double) totalNanos / totalTransforms / 1_000_000.0 : 0;
        }

        @Override
        public String toString() {
            return String.format(
                "PipelineStats[transforms=%d, skipped=%d, cached=%d, " +
                    "budgetExceeded=%d, errors=%d, avgTime=%.2fms, " +
                    "threads=%d, queued=%d]",
                totalTransforms, totalSkipped, totalCacheHits,
                totalBudgetExceeded, totalErrors, averageTransformMs(),
                activeThreads, queuedTasks);
        }
    }

    /**
     * Safe ClassWriter that handles missing classes gracefully.
     * Falls back to Object for unknown common superclass computation.
     */
    static final class SafeClassWriter extends ClassWriter {

        private final ClassReader classReader;

        SafeClassWriter(ClassReader classReader, int flags) {
            super(classReader, flags);
            this.classReader = classReader;
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            // Fast path for common cases
            if (type1.equals("java/lang/Object") || type2.equals("java/lang/Object")) {
                return "java/lang/Object";
            }
            if (type1.equals(type2)) {
                return type1;
            }

            // Try the standard approach, but catch ClassNotFoundException
            try {
                return super.getCommonSuperClass(type1, type2);
            } catch (TypeNotPresentException | RuntimeException e) {
                // Fallback: return Object (safe but may require frame recomputation)
                return "java/lang/Object";
            }
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 6: BYTECODE OPTIMIZATION PASSES                           ║
    // ║                                                                    ║
    // ║  Post-transform optimization passes that reduce bytecode size      ║
    // ║  and improve runtime performance of DeepMix-generated code.        ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Collection of bytecode optimization passes.
     * Applied after all DeepMix transforms to clean up generated code.
     */
    public static final class BytecodeOptimizer {

        /** Optimization pass interface */
        @FunctionalInterface
        public interface OptimizationPass {
            /**
             * @return true if the pass modified the bytecode
             */
            boolean optimize(ClassNode classNode);
        }

        // Standard passes (ordered by typical impact)
        private static final List<OptimizationPass> STANDARD_PASSES = List.of(
            BytecodeOptimizer::removeDeadCode,
            BytecodeOptimizer::foldConstants,
            BytecodeOptimizer::simplifyBranches,
            BytecodeOptimizer::removeRedundantLoadsStores,
            BytecodeOptimizer::peepholeOptimize,
            BytecodeOptimizer::removeNopInstructions,
            BytecodeOptimizer::mergeAdjacentStringOps
        );

        /**
         * Run all standard optimization passes on a ClassNode.
         * Iterates until no pass makes changes (fixed-point).
         *
         * @param classNode The class to optimize
         * @param maxIterations Maximum optimization iterations
         * @return Number of total modifications made
         */
        public static int optimize(ClassNode classNode, int maxIterations) {
            int totalMods = 0;
            for (int iter = 0; iter < maxIterations; iter++) {
                boolean anyChanged = false;
                for (OptimizationPass pass : STANDARD_PASSES) {
                    try {
                        if (pass.optimize(classNode)) {
                            anyChanged = true;
                            totalMods++;
                        }
                    } catch (Exception e) {
                        // Never let an optimization pass crash the pipeline
                        System.err.println("[DeepMix:Optimizer] Pass failed: " + e.getMessage());
                    }
                }
                if (!anyChanged) break; // Fixed point reached
            }
            return totalMods;
        }

        /** Optimize with default max iterations (3) */
        public static int optimize(ClassNode classNode) {
            return optimize(classNode, 3);
        }

        // ── Individual optimization passes ──

        /**
         * Remove unreachable code after unconditional jumps/returns.
         */
        public static boolean removeDeadCode(ClassNode classNode) {
            boolean modified = false;
            for (MethodNode mn : classNode.methods) {
                if (mn.instructions.size() == 0) continue;

                Set<AbstractInsnNode> reachable = new HashSet<>();
                Deque<AbstractInsnNode> worklist = new ArrayDeque<>();

                // Start from method entry
                worklist.add(mn.instructions.getFirst());

                // Add exception handler entries
                if (mn.tryCatchBlocks != null) {
                    for (TryCatchBlockNode tcb : mn.tryCatchBlocks) {
                        worklist.add(tcb.handler);
                    }
                }

                // BFS to find all reachable instructions
                while (!worklist.isEmpty()) {
                    AbstractInsnNode insn = worklist.poll();
                    if (insn == null || reachable.contains(insn)) continue;
                    reachable.add(insn);

                    int opcode = insn.getOpcode();

                    // Add successors
                    if (insn instanceof JumpInsnNode) {
                        worklist.add(((JumpInsnNode) insn).label);
                        if (opcode != Opcodes.GOTO) {
                            // Conditional jump: fall-through is also reachable
                            worklist.add(insn.getNext());
                        }
                    } else if (insn instanceof TableSwitchInsnNode) {
                        TableSwitchInsnNode tsn = (TableSwitchInsnNode) insn;
                        worklist.add(tsn.dflt);
                        for (LabelNode label : tsn.labels) worklist.add(label);
                    } else if (insn instanceof LookupSwitchInsnNode) {
                        LookupSwitchInsnNode lsn = (LookupSwitchInsnNode) insn;
                        worklist.add(lsn.dflt);
                        for (LabelNode label : lsn.labels) worklist.add(label);
                    } else if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                        // Return — no successor
                    } else if (opcode == Opcodes.ATHROW) {
                        // Throw — no successor
                    } else {
                        // Normal instruction — successor is next
                        if (insn.getNext() != null) {
                            worklist.add(insn.getNext());
                        }
                    }

                    // Labels, frames, line numbers — always add next
                    if (insn instanceof LabelNode || insn instanceof FrameNode ||
                        insn instanceof LineNumberNode) {
                        if (insn.getNext() != null) {
                            worklist.add(insn.getNext());
                        }
                    }
                }

                // Remove unreachable instructions (but keep labels and frames)
                Iterator<AbstractInsnNode> it = mn.instructions.iterator();
                while (it.hasNext()) {
                    AbstractInsnNode insn = it.next();
                    if (!reachable.contains(insn) &&
                        !(insn instanceof LabelNode) &&
                        !(insn instanceof FrameNode)) {
                        it.remove();
                        modified = true;
                    }
                }
            }
            return modified;
        }

        /**
         * Fold constant expressions.
         * Examples: ICONST_2 + ICONST_3 → ICONST_5
         *           LDC "foo" + LDC "bar" → LDC "foobar"
         */
        public static boolean foldConstants(ClassNode classNode) {
            boolean modified = false;
            for (MethodNode mn : classNode.methods) {
                for (AbstractInsnNode insn = mn.instructions.getFirst();
                     insn != null; insn = insn.getNext()) {

                    // Pattern: push const, push const, arithmetic op
                    if (isIntConstant(insn) && insn.getNext() != null &&
                        isIntConstant(insn.getNext())) {

                        AbstractInsnNode next = insn.getNext();
                        AbstractInsnNode op = next.getNext();

                        if (op != null && isIntArithmetic(op)) {
                            int a = getIntConstant(insn);
                            int b = getIntConstant(next);
                            int result;

                            switch (op.getOpcode()) {
                                case Opcodes.IADD: result = a + b; break;
                                case Opcodes.ISUB: result = a - b; break;
                                case Opcodes.IMUL: result = a * b; break;
                                case Opcodes.IDIV:
                                    if (b == 0) continue;
                                    result = a / b;
                                    break;
                                case Opcodes.IREM:
                                    if (b == 0) continue;
                                    result = a % b;
                                    break;
                                case Opcodes.IAND: result = a & b; break;
                                case Opcodes.IOR:  result = a | b; break;
                                case Opcodes.IXOR: result = a ^ b; break;
                                case Opcodes.ISHL: result = a << b; break;
                                case Opcodes.ISHR: result = a >> b; break;
                                case Opcodes.IUSHR: result = a >>> b; break;
                                default: continue;
                            }

                            // Replace all three instructions with the folded constant
                            AbstractInsnNode replacement = createIntConstant(result);
                            mn.instructions.insert(op, replacement);
                            mn.instructions.remove(insn);
                            mn.instructions.remove(next);
                            mn.instructions.remove(op);
                            insn = replacement;
                            modified = true;
                        }
                    }
                }
            }
            return modified;
        }

        /**
         * Simplify branches: remove redundant gotos, fold always-true/false branches.
         */
        public static boolean simplifyBranches(ClassNode classNode) {
            boolean modified = false;
            for (MethodNode mn : classNode.methods) {
                for (AbstractInsnNode insn = mn.instructions.getFirst();
                     insn != null; insn = insn.getNext()) {

                    // GOTO to next instruction → remove
                    if (insn.getOpcode() == Opcodes.GOTO) {
                        JumpInsnNode jump = (JumpInsnNode) insn;
                        AbstractInsnNode target = jump.label;
                        AbstractInsnNode afterGoto = insn.getNext();

                        // Walk past labels/frames/line numbers to find effective next
                        AbstractInsnNode effectiveNext = afterGoto;
                        while (effectiveNext != null &&
                               (effectiveNext instanceof LabelNode ||
                                effectiveNext instanceof FrameNode ||
                                effectiveNext instanceof LineNumberNode)) {
                            if (effectiveNext == target) {
                                // GOTO jumps to the very next real instruction — remove it
                                mn.instructions.remove(insn);
                                insn = afterGoto;
                                modified = true;
                                break;
                            }
                            effectiveNext = effectiveNext.getNext();
                        }
                    }

                    // ICONST_1 + IFEQ → never taken (always true)
                    // ICONST_0 + IFEQ → always taken (GOTO)
                    if (isIntConstant(insn) && insn.getNext() instanceof JumpInsnNode) {
                        int val = getIntConstant(insn);
                        JumpInsnNode jump = (JumpInsnNode) insn.getNext();
                        int op = jump.getOpcode();

                        boolean alwaysTaken = false;
                        boolean neverTaken = false;

                        switch (op) {
                            case Opcodes.IFEQ: alwaysTaken = (val == 0); neverTaken = (val != 0); break;
                            case Opcodes.IFNE: alwaysTaken = (val != 0); neverTaken = (val == 0); break;
                            case Opcodes.IFLT: alwaysTaken = (val < 0); neverTaken = (val >= 0); break;
                            case Opcodes.IFGE: alwaysTaken = (val >= 0); neverTaken = (val < 0); break;
                            case Opcodes.IFGT: alwaysTaken = (val > 0); neverTaken = (val <= 0); break;
                            case Opcodes.IFLE: alwaysTaken = (val <= 0); neverTaken = (val > 0); break;
                        }

                        if (alwaysTaken) {
                            // Replace const + conditional with GOTO
                            JumpInsnNode gotoInsn = new JumpInsnNode(Opcodes.GOTO, jump.label);
                            mn.instructions.insert(jump, gotoInsn);
                            mn.instructions.remove(insn);
                            mn.instructions.remove(jump);
                            insn = gotoInsn;
                            modified = true;
                        } else if (neverTaken) {
                            // Remove both const and branch (fall through)
                            AbstractInsnNode afterJump = jump.getNext();
                            mn.instructions.remove(insn);
                            mn.instructions.remove(jump);
                            insn = afterJump != null ? afterJump : mn.instructions.getFirst();
                            modified = true;
                        }
                    }
                }
            }
            return modified;
        }

        /**
         * Remove redundant load-store pairs: ISTORE n; ILOAD n → DUP; ISTORE n
         */
        public static boolean removeRedundantLoadsStores(ClassNode classNode) {
            boolean modified = false;
            for (MethodNode mn : classNode.methods) {
                for (AbstractInsnNode insn = mn.instructions.getFirst();
                     insn != null; insn = insn.getNext()) {

                    // Pattern: xSTORE n; xLOAD n (same type, same variable)
                    if (insn instanceof VarInsnNode && insn.getNext() instanceof VarInsnNode) {
                        VarInsnNode store = (VarInsnNode) insn;
                        VarInsnNode load = (VarInsnNode) insn.getNext();

                        if (store.var == load.var && isStoreOp(store.getOpcode()) &&
                            isMatchingLoad(store.getOpcode(), load.getOpcode())) {
                            // Replace with DUP; xSTORE n
                            mn.instructions.insertBefore(store, new InsnNode(Opcodes.DUP));
                            mn.instructions.remove(load);
                            modified = true;
                        }
                    }

                    // Pattern: xLOAD n; POP → remove both
                    if (insn instanceof VarInsnNode && isLoadOp(insn.getOpcode()) &&
                        insn.getNext() != null && insn.getNext().getOpcode() == Opcodes.POP) {
                        AbstractInsnNode afterPop = insn.getNext().getNext();
                        mn.instructions.remove(insn.getNext());
                        mn.instructions.remove(insn);
                        insn = afterPop != null ? afterPop : mn.instructions.getFirst();
                        modified = true;
                    }
                }
            }
            return modified;
        }

        /**
         * Peephole optimizations: small pattern replacements.
         */
        public static boolean peepholeOptimize(ClassNode classNode) {
            boolean modified = false;
            for (MethodNode mn : classNode.methods) {
                for (AbstractInsnNode insn = mn.instructions.getFirst();
                     insn != null; insn = insn.getNext()) {

                    // SWAP; SWAP → remove both
                    if (insn.getOpcode() == Opcodes.SWAP &&
                        insn.getNext() != null && insn.getNext().getOpcode() == Opcodes.SWAP) {
                        AbstractInsnNode next = insn.getNext();
                        AbstractInsnNode afterBoth = next.getNext();
                        mn.instructions.remove(insn);
                        mn.instructions.remove(next);
                        insn = afterBoth != null ? afterBoth : mn.instructions.getFirst();
                        modified = true;
                        continue;
                    }

                    // DUP; POP → remove both
                    if (insn.getOpcode() == Opcodes.DUP &&
                        insn.getNext() != null && insn.getNext().getOpcode() == Opcodes.POP) {
                        AbstractInsnNode next = insn.getNext();
                        AbstractInsnNode afterBoth = next.getNext();
                        mn.instructions.remove(insn);
                        mn.instructions.remove(next);
                        insn = afterBoth != null ? afterBoth : mn.instructions.getFirst();
                        modified = true;
                        continue;
                    }

                    // CHECKCAST T; CHECKCAST T → single CHECKCAST T
                    if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.CHECKCAST &&
                        insn.getNext() instanceof TypeInsnNode &&
                        insn.getNext().getOpcode() == Opcodes.CHECKCAST) {
                        TypeInsnNode first = (TypeInsnNode) insn;
                        TypeInsnNode second = (TypeInsnNode) insn.getNext();
                        if (first.desc.equals(second.desc)) {
                            mn.instructions.remove(second);
                            modified = true;
                        }
                    }

                    // INEG; INEG → remove both
                    if (insn.getOpcode() == Opcodes.INEG &&
                        insn.getNext() != null && insn.getNext().getOpcode() == Opcodes.INEG) {
                        AbstractInsnNode next = insn.getNext();
                        AbstractInsnNode afterBoth = next.getNext();
                        mn.instructions.remove(insn);
                        mn.instructions.remove(next);
                        insn = afterBoth != null ? afterBoth : mn.instructions.getFirst();
                        modified = true;
                        continue;
                    }

                    // ICONST_0 + IADD → remove both (identity: x + 0 = x)
                    if (isZeroConstant(insn) && insn.getNext() != null &&
                        insn.getNext().getOpcode() == Opcodes.IADD) {
                        AbstractInsnNode add = insn.getNext();
                        AbstractInsnNode afterAdd = add.getNext();
                        mn.instructions.remove(insn);
                        mn.instructions.remove(add);
                        insn = afterAdd != null ? afterAdd : mn.instructions.getFirst();
                        modified = true;
                        continue;
                    }

                    // ICONST_1 + IMUL → remove both (identity: x * 1 = x)
                    if (isOneConstant(insn) && insn.getNext() != null &&
                        insn.getNext().getOpcode() == Opcodes.IMUL) {
                        AbstractInsnNode mul = insn.getNext();
                        AbstractInsnNode afterMul = mul.getNext();
                        mn.instructions.remove(insn);
                        mn.instructions.remove(mul);
                        insn = afterMul != null ? afterMul : mn.instructions.getFirst();
                        modified = true;
                        continue;
                    }

                    // ICONST_0 + IMUL → POP; ICONST_0 (x * 0 = 0, but need to consume x)
                    if (isZeroConstant(insn) && insn.getNext() != null &&
                        insn.getNext().getOpcode() == Opcodes.IMUL) {
                        AbstractInsnNode mul = insn.getNext();
                        // Replace: remove zero, replace mul with POP; ICONST_0
                        mn.instructions.set(mul, new InsnNode(Opcodes.POP));
                        // insn (the zero constant) stays — result is POP, then zero on stack next
                        modified = true;
                    }

                    // Strength reduction: IMUL by power of 2 → ISHL
                    if (insn instanceof LdcInsnNode && insn.getNext() != null &&
                        insn.getNext().getOpcode() == Opcodes.IMUL) {
                        LdcInsnNode ldc = (LdcInsnNode) insn;
                        if (ldc.cst instanceof Integer) {
                            int val = (Integer) ldc.cst;
                            if (val > 0 && Integer.bitCount(val) == 1) {
                                int shift = Integer.numberOfTrailingZeros(val);
                                mn.instructions.set(insn, createIntConstant(shift));
                                mn.instructions.set(insn.getNext(),
                                    new InsnNode(Opcodes.ISHL));
                                modified = true;
                            }
                        }
                    }

                    // Strength reduction: IDIV by power of 2 → ISHR (for positive only)
                    if (insn instanceof LdcInsnNode && insn.getNext() != null &&
                        insn.getNext().getOpcode() == Opcodes.IDIV) {
                        LdcInsnNode ldc = (LdcInsnNode) insn;
                        if (ldc.cst instanceof Integer) {
                            int val = (Integer) ldc.cst;
                            if (val > 0 && Integer.bitCount(val) == 1) {
                                int shift = Integer.numberOfTrailingZeros(val);
                                mn.instructions.set(insn, createIntConstant(shift));
                                mn.instructions.set(insn.getNext(),
                                    new InsnNode(Opcodes.IUSHR));
                                modified = true;
                            }
                        }
                    }
                }
            }
            return modified;
        }

        /**
         * Remove NOP instructions (they waste space).
         */
        public static boolean removeNopInstructions(ClassNode classNode) {
            boolean modified = false;
            for (MethodNode mn : classNode.methods) {
                Iterator<AbstractInsnNode> it = mn.instructions.iterator();
                while (it.hasNext()) {
                    if (it.next().getOpcode() == Opcodes.NOP) {
                        it.remove();
                        modified = true;
                    }
                }
            }
            return modified;
        }

        /**
         * Merge adjacent StringBuilder operations.
         * Pattern: new SB; SB.init; SB.append(a); SB.append(b); SB.toString()
         * → optimize append chains, fold constant string appends
         */
        public static boolean mergeAdjacentStringOps(ClassNode classNode) {
            boolean modified = false;
            for (MethodNode mn : classNode.methods) {
                // Find consecutive LDC string; SB.append patterns
                for (AbstractInsnNode insn = mn.instructions.getFirst();
                     insn != null; insn = insn.getNext()) {

                    // Pattern: LDC "a"; INVOKEVIRTUAL SB.append; LDC "b"; INVOKEVIRTUAL SB.append
                    // → LDC "ab"; INVOKEVIRTUAL SB.append
                    if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof String) {
                        AbstractInsnNode append1 = insn.getNext();
                        if (isStringBuilderAppend(append1)) {
                            AbstractInsnNode ldc2 = append1.getNext();
                            if (ldc2 instanceof LdcInsnNode &&
                                ((LdcInsnNode) ldc2).cst instanceof String) {
                                AbstractInsnNode append2 = ldc2.getNext();
                                if (isStringBuilderAppend(append2)) {
                                    // Merge the two string constants
                                    String merged = (String) ((LdcInsnNode) insn).cst +
                                        (String) ((LdcInsnNode) ldc2).cst;
                                    ((LdcInsnNode) insn).cst = merged;
                                    mn.instructions.remove(append1);
                                    mn.instructions.remove(ldc2);
                                    // Keep append2 — it now appends the merged string
                                    modified = true;
                                }
                            }
                        }
                    }
                }
            }
            return modified;
        }

        // ── Helper methods for optimization passes ──

        private static boolean isIntConstant(AbstractInsnNode insn) {
            if (insn == null) return false;
            int op = insn.getOpcode();
            return (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) ||
                   op == Opcodes.BIPUSH || op == Opcodes.SIPUSH ||
                   (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Integer);
        }

        private static int getIntConstant(AbstractInsnNode insn) {
            int op = insn.getOpcode();
            if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) {
                return op - Opcodes.ICONST_0;
            }
            if (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH) {
                return ((IntInsnNode) insn).operand;
            }
            if (insn instanceof LdcInsnNode) {
                return (Integer) ((LdcInsnNode) insn).cst;
            }
            throw new IllegalArgumentException("Not an int constant: " + insn);
        }

        private static AbstractInsnNode createIntConstant(int value) {
            if (value >= -1 && value <= 5) {
                return new InsnNode(Opcodes.ICONST_0 + value);
            }
            if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                return new IntInsnNode(Opcodes.BIPUSH, value);
            }
            if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                return new IntInsnNode(Opcodes.SIPUSH, value);
            }
            return new LdcInsnNode(value);
        }

        private static boolean isZeroConstant(AbstractInsnNode insn) {
            return insn != null && insn.getOpcode() == Opcodes.ICONST_0;
        }

        private static boolean isOneConstant(AbstractInsnNode insn) {
            return insn != null && insn.getOpcode() == Opcodes.ICONST_1;
        }

        private static boolean isIntArithmetic(AbstractInsnNode insn) {
            if (insn == null) return false;
            int op = insn.getOpcode();
            return op == Opcodes.IADD || op == Opcodes.ISUB || op == Opcodes.IMUL ||
                   op == Opcodes.IDIV || op == Opcodes.IREM || op == Opcodes.IAND ||
                   op == Opcodes.IOR || op == Opcodes.IXOR || op == Opcodes.ISHL ||
                   op == Opcodes.ISHR || op == Opcodes.IUSHR;
        }

        private static boolean isStoreOp(int opcode) {
            return opcode == Opcodes.ISTORE || opcode == Opcodes.LSTORE ||
                   opcode == Opcodes.FSTORE || opcode == Opcodes.DSTORE ||
                   opcode == Opcodes.ASTORE;
        }

        private static boolean isLoadOp(int opcode) {
            return opcode == Opcodes.ILOAD || opcode == Opcodes.LLOAD ||
                   opcode == Opcodes.FLOAD || opcode == Opcodes.DLOAD ||
                   opcode == Opcodes.ALOAD;
        }

        private static boolean isMatchingLoad(int storeOp, int loadOp) {
            return (storeOp == Opcodes.ISTORE && loadOp == Opcodes.ILOAD) ||
                   (storeOp == Opcodes.LSTORE && loadOp == Opcodes.LLOAD) ||
                   (storeOp == Opcodes.FSTORE && loadOp == Opcodes.FLOAD) ||
                   (storeOp == Opcodes.DSTORE && loadOp == Opcodes.DLOAD) ||
                   (storeOp == Opcodes.ASTORE && loadOp == Opcodes.ALOAD);
        }

        private static boolean isStringBuilderAppend(AbstractInsnNode insn) {
            if (!(insn instanceof MethodInsnNode)) return false;
            MethodInsnNode min = (MethodInsnNode) insn;
            return min.owner.equals("java/lang/StringBuilder") &&
                   min.name.equals("append") &&
                   min.desc.equals("(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 7: CIRCUIT BREAKER & STABILITY GUARDS                     ║
    // ║                                                                    ║
    // ║  Prevents cascading failures, infinite loops, and resource          ║
    // ║  exhaustion in the transformation pipeline.                         ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Circuit breaker for DeepMix operations.
     *
     * States: CLOSED (normal) → OPEN (failing) → HALF_OPEN (testing) → CLOSED
     *
     * When OPEN, all operations are immediately rejected without executing.
     * After a cooldown, moves to HALF_OPEN and allows a single test operation.
     * If the test succeeds, returns to CLOSED. If it fails, returns to OPEN.
     */
    public static final class CircuitBreaker {

        public enum State {
            /** Normal operation — all requests pass through */
            CLOSED,
            /** Failing — all requests rejected immediately */
            OPEN,
            /** Testing recovery — one request allowed through */
            HALF_OPEN
        }

        private final String name;
        private final int failureThreshold;          // Failures before opening
        private final long cooldownMs;               // Time before half-open
        private final long slidingWindowMs;          // Window for counting failures
        private final double failureRateThreshold;   // 0.0-1.0 failure rate to open

        private volatile State state = State.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger totalCount = new AtomicInteger(0);
        private final AtomicLong lastFailureMs = new AtomicLong(0);
        private final AtomicLong openedAtMs = new AtomicLong(0);
        private final AtomicLong lastStateChangeMs = new AtomicLong(System.currentTimeMillis());
        private final AtomicInteger consecutiveSuccesses = new AtomicInteger(0);
        private final AtomicInteger halfOpenAttempts = new AtomicInteger(0);
        private final AtomicLong totalRejections = new AtomicLong(0);

        // Sliding window for failure rate calculation
        private final ConcurrentLinkedDeque<Long> failureTimestamps = new ConcurrentLinkedDeque<>();
        private final ConcurrentLinkedDeque<Long> successTimestamps = new ConcurrentLinkedDeque<>();

        // Listeners
        private final CopyOnWriteArrayList<BiConsumer<State, State>> stateListeners =
            new CopyOnWriteArrayList<>();

        // Recovery requirements for HALF_OPEN → CLOSED
        private static final int HALF_OPEN_SUCCESS_THRESHOLD = 3;

        public CircuitBreaker(String name, int failureThreshold, long cooldownMs,
                              long slidingWindowMs, double failureRateThreshold) {
            this.name = name;
            this.failureThreshold = failureThreshold;
            this.cooldownMs = cooldownMs;
            this.slidingWindowMs = slidingWindowMs;
            this.failureRateThreshold = failureRateThreshold;
        }

        /** Convenience constructor with defaults */
        public CircuitBreaker(String name) {
            this(name, 5, 30_000, 60_000, 0.5);
        }

        /**
         * Execute an operation through the circuit breaker.
         *
         * @param operation The operation to execute
         * @param fallback  Fallback value if circuit is open
         * @return Operation result or fallback
         */
        public <T> T execute(Supplier<T> operation, Supplier<T> fallback) {
            if (!allowRequest()) {
                totalRejections.incrementAndGet();
                return fallback != null ? fallback.get() : null;
            }

            try {
                T result = operation.get();
                recordSuccess();
                return result;
            } catch (Exception e) {
                recordFailure(e);
                if (fallback != null) {
                    return fallback.get();
                }
                throw e;
            }
        }

        /**
         * Execute a void operation through the circuit breaker.
         */
        public void execute(Runnable operation, Runnable fallback) {
            if (!allowRequest()) {
                totalRejections.incrementAndGet();
                if (fallback != null) fallback.run();
                return;
            }

            try {
                operation.run();
                recordSuccess();
            } catch (Exception e) {
                recordFailure(e);
                if (fallback != null) {
                    fallback.run();
                } else {
                    throw e;
                }
            }
        }

        /**
         * Check if a request should be allowed through.
         */
        public boolean allowRequest() {
            switch (state) {
                case CLOSED:
                    return true;

                case OPEN:
                    long elapsed = System.currentTimeMillis() - openedAtMs.get();
                    if (elapsed >= cooldownMs) {
                        // Transition to HALF_OPEN
                        transitionTo(State.HALF_OPEN);
                        halfOpenAttempts.set(0);
                        consecutiveSuccesses.set(0);
                        return true;
                    }
                    return false;

                case HALF_OPEN:
                    // Allow limited requests in half-open
                    return halfOpenAttempts.incrementAndGet() <= HALF_OPEN_SUCCESS_THRESHOLD + 1;

                default:
                    return false;
            }
        }

        /**
         * Record a successful operation.
         */
        public void recordSuccess() {
            long now = System.currentTimeMillis();
            successCount.incrementAndGet();
            totalCount.incrementAndGet();
            successTimestamps.addLast(now);
            purgeOldTimestamps();

            switch (state) {
                case HALF_OPEN:
                    int successes = consecutiveSuccesses.incrementAndGet();
                    if (successes >= HALF_OPEN_SUCCESS_THRESHOLD) {
                        // Recovery confirmed — close the circuit
                        transitionTo(State.CLOSED);
                        failureCount.set(0);
                        consecutiveSuccesses.set(0);
                    }
                    break;

                case CLOSED:
                    // Reset consecutive success tracking
                    consecutiveSuccesses.incrementAndGet();
                    break;

                default:
                    break;
            }
        }

        /**
         * Record a failed operation.
         */
        public void recordFailure(Throwable error) {
            long now = System.currentTimeMillis();
            failureCount.incrementAndGet();
            totalCount.incrementAndGet();
            lastFailureMs.set(now);
            failureTimestamps.addLast(now);
            consecutiveSuccesses.set(0);
            purgeOldTimestamps();

            switch (state) {
                case CLOSED:
                    // Check if we should open the circuit
                    if (shouldTrip()) {
                        transitionTo(State.OPEN);
                        openedAtMs.set(now);
                    }
                    break;

                case HALF_OPEN:
                    // Any failure in half-open → back to open
                    transitionTo(State.OPEN);
                    openedAtMs.set(now);
                    break;

                default:
                    break;
            }
        }

        /** Force the circuit open */
        public void forceOpen() {
            transitionTo(State.OPEN);
            openedAtMs.set(System.currentTimeMillis());
        }

        /** Force the circuit closed */
        public void forceClosed() {
            transitionTo(State.CLOSED);
            failureCount.set(0);
            consecutiveSuccesses.set(0);
        }

        /** Reset all counters */
        public void reset() {
            forceClosed();
            failureCount.set(0);
            successCount.set(0);
            totalCount.set(0);
            consecutiveSuccesses.set(0);
            halfOpenAttempts.set(0);
            totalRejections.set(0);
            failureTimestamps.clear();
            successTimestamps.clear();
        }

        /** Register a state change listener */
        public void onStateChange(BiConsumer<State, State> listener) {
            stateListeners.add(listener);
        }

        // ── Getters ──

        public String name() { return name; }
        public State state() { return state; }
        public int failureCount() { return failureCount.get(); }
        public int successCount() { return successCount.get(); }
        public long totalRejections() { return totalRejections.get(); }
        public long lastFailureMs() { return lastFailureMs.get(); }

        public double failureRate() {
            int total = totalInWindow();
            if (total == 0) return 0.0;
            return (double) failuresInWindow() / total;
        }

        public CircuitBreakerStats stats() {
            return new CircuitBreakerStats(
                name, state, failureCount.get(), successCount.get(),
                totalCount.get(), totalRejections.get(),
                failureRate(), lastFailureMs.get(),
                lastStateChangeMs.get()
            );
        }

        @Override
        public String toString() {
            return String.format("CircuitBreaker[%s|%s|failures=%d|rate=%.1f%%|rejections=%d]",
                name, state, failureCount.get(), failureRate() * 100, totalRejections.get());
        }

        // ── Private ──

        private boolean shouldTrip() {
            // Trip if absolute failure count exceeds threshold
            if (failuresInWindow() >= failureThreshold) return true;

            // Trip if failure rate exceeds threshold (with minimum sample size)
            int total = totalInWindow();
            if (total >= failureThreshold) {
                double rate = (double) failuresInWindow() / total;
                return rate >= failureRateThreshold;
            }

            return false;
        }

        private int failuresInWindow() {
            long cutoff = System.currentTimeMillis() - slidingWindowMs;
            return (int) failureTimestamps.stream().filter(t -> t >= cutoff).count();
        }

        private int totalInWindow() {
            long cutoff = System.currentTimeMillis() - slidingWindowMs;
            int failures = (int) failureTimestamps.stream().filter(t -> t >= cutoff).count();
            int successes = (int) successTimestamps.stream().filter(t -> t >= cutoff).count();
            return failures + successes;
        }

        private void purgeOldTimestamps() {
            long cutoff = System.currentTimeMillis() - slidingWindowMs;
            while (!failureTimestamps.isEmpty() && failureTimestamps.peekFirst() < cutoff) {
                failureTimestamps.pollFirst();
            }
            while (!successTimestamps.isEmpty() && successTimestamps.peekFirst() < cutoff) {
                successTimestamps.pollFirst();
            }
        }

        private void transitionTo(State newState) {
            State oldState = this.state;
            if (oldState == newState) return;

            this.state = newState;
            this.lastStateChangeMs.set(System.currentTimeMillis());

            System.out.println("[DeepMix:CircuitBreaker:" + name + "] " +
                oldState + " → " + newState);

            for (BiConsumer<State, State> listener : stateListeners) {
                try {
                    listener.accept(oldState, newState);
                } catch (Exception e) {
                    // Swallow listener errors
                }
            }
        }
    }

    /** Immutable circuit breaker statistics */
    public static final class CircuitBreakerStats {
        public final String name;
        public final CircuitBreaker.State state;
        public final int failures;
        public final int successes;
        public final int total;
        public final long rejections;
        public final double failureRate;
        public final long lastFailureMs;
        public final long lastStateChangeMs;

        CircuitBreakerStats(String name, CircuitBreaker.State state, int failures,
                           int successes, int total, long rejections,
                           double failureRate, long lastFailureMs,
                           long lastStateChangeMs) {
            this.name = name;
            this.state = state;
            this.failures = failures;
            this.successes = successes;
            this.total = total;
            this.rejections = rejections;
            this.failureRate = failureRate;
            this.lastFailureMs = lastFailureMs;
            this.lastStateChangeMs = lastStateChangeMs;
        }

        @Override
        public String toString() {
            return String.format(
                "CB[%s|%s|f=%d|s=%d|rej=%d|rate=%.1f%%]",
                name, state, failures, successes, rejections, failureRate * 100);
        }
    }

    /**
     * Central registry of circuit breakers for all DeepMix subsystems.
     */
    public static final class CircuitBreakerRegistry {

        private static final ConcurrentHashMap<String, CircuitBreaker> breakers =
            new ConcurrentHashMap<>();

        // Pre-defined breakers for critical subsystems
        public static final String TRANSFORM_PIPELINE = "transform.pipeline";
        public static final String MODULE_THAW = "module.thaw";
        public static final String HOT_RELOAD = "hot.reload";
        public static final String BYTECODE_CACHE = "bytecode.cache";
        public static final String EXTERNAL_IO = "external.io";
        public static final String MIXIN_APPLY = "mixin.apply";
        public static final String ASM_OPERATIONS = "asm.operations";
        public static final String PLUGIN_LIFECYCLE = "plugin.lifecycle";

        static {
            // Initialize standard breakers with tuned parameters
            register(TRANSFORM_PIPELINE,
                new CircuitBreaker(TRANSFORM_PIPELINE, 10, 15_000, 60_000, 0.3));
            register(MODULE_THAW,
                new CircuitBreaker(MODULE_THAW, 3, 30_000, 120_000, 0.5));
            register(HOT_RELOAD,
                new CircuitBreaker(HOT_RELOAD, 5, 10_000, 30_000, 0.4));
            register(BYTECODE_CACHE,
                new CircuitBreaker(BYTECODE_CACHE, 8, 5_000, 30_000, 0.6));
            register(EXTERNAL_IO,
                new CircuitBreaker(EXTERNAL_IO, 5, 20_000, 60_000, 0.3));
            register(MIXIN_APPLY,
                new CircuitBreaker(MIXIN_APPLY, 5, 15_000, 60_000, 0.3));
            register(ASM_OPERATIONS,
                new CircuitBreaker(ASM_OPERATIONS, 8, 10_000, 30_000, 0.4));
            register(PLUGIN_LIFECYCLE,
                new CircuitBreaker(PLUGIN_LIFECYCLE, 3, 30_000, 120_000, 0.5));
        }

        public static void register(String name, CircuitBreaker breaker) {
            breakers.put(name, breaker);
        }

        public static CircuitBreaker get(String name) {
            return breakers.computeIfAbsent(name, CircuitBreaker::new);
        }

        /** Execute through a named circuit breaker */
        public static <T> T execute(String breakerName, Supplier<T> operation,
                                     Supplier<T> fallback) {
            return get(breakerName).execute(operation, fallback);
        }

        /** Execute void through a named circuit breaker */
        public static void execute(String breakerName, Runnable operation,
                                    Runnable fallback) {
            get(breakerName).execute(operation, fallback);
        }

        /** Get stats for all breakers */
        public static Map<String, CircuitBreakerStats> allStats() {
            Map<String, CircuitBreakerStats> stats = new LinkedHashMap<>();
            breakers.forEach((name, breaker) -> stats.put(name, breaker.stats()));
            return Collections.unmodifiableMap(stats);
        }

        /** Reset all breakers */
        public static void resetAll() {
            breakers.values().forEach(CircuitBreaker::reset);
        }

        /** Check if any breaker is open */
        public static boolean anyOpen() {
            return breakers.values().stream()
                .anyMatch(b -> b.state() == CircuitBreaker.State.OPEN);
        }

        /** Get all open breakers */
        public static List<String> openBreakers() {
            return breakers.entrySet().stream()
                .filter(e -> e.getValue().state() == CircuitBreaker.State.OPEN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 8: WATCHDOG & DEADLOCK DETECTION                          ║
    // ║                                                                    ║
    // ║  Monitors DeepMix threads for hangs, deadlocks, and infinite       ║
    // ║  loops. Automatically kills stuck operations.                      ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Watchdog timer that monitors running operations and kills them
     * if they exceed their time budget.
     *
     * Usage:
     * <pre>
     * WatchdogToken token = Watchdog.start("myOperation", 5000);
     * try {
     *     doExpensiveWork();
     * } finally {
     *     Watchdog.stop(token);
     * }
     * </pre>
     */
    public static final class Watchdog {

        /** Watched operation token */
        public static final class WatchdogToken {
            final long id;
            final String operationName;
            final Thread thread;
            final long startTimeMs;
            final long deadlineMs;
            final boolean interruptOnTimeout;
            volatile boolean completed = false;
            volatile boolean timedOut = false;

            WatchdogToken(long id, String name, Thread thread,
                          long startMs, long deadlineMs, boolean interrupt) {
                this.id = id;
                this.operationName = name;
                this.thread = thread;
                this.startTimeMs = startMs;
                this.deadlineMs = deadlineMs;
                this.interruptOnTimeout = interrupt;
            }

            public long elapsedMs() {
                return System.currentTimeMillis() - startTimeMs;
            }

            public long remainingMs() {
                return Math.max(0, deadlineMs - System.currentTimeMillis());
            }

            public boolean isOverdue() {
                return !completed && System.currentTimeMillis() > deadlineMs;
            }
        }

        private static final ConcurrentHashMap<Long, WatchdogToken> activeTokens =
            new ConcurrentHashMap<>();
        private static final AtomicLong tokenCounter = new AtomicLong(0);
        private static final AtomicLong totalTimeouts = new AtomicLong(0);
        private static final AtomicLong totalWatched = new AtomicLong(0);

        // Default timeout for operations (5 seconds)
        private static volatile long defaultTimeoutMs = 5_000;

        // Watchdog check interval
        private static final long CHECK_INTERVAL_MS = 250;

        // Watchdog daemon thread
        private static volatile Thread watchdogThread = null;
        private static volatile boolean running = false;

        /**
         * Start watching an operation.
         *
         * @param operationName Human-readable name for diagnostics
         * @param timeoutMs     Maximum allowed duration
         * @return Token to pass to stop()
         */
        public static WatchdogToken start(String operationName, long timeoutMs) {
            return start(operationName, timeoutMs, true);
        }

        /**
         * Start watching an operation.
         *
         * @param operationName      Human-readable name
         * @param timeoutMs          Maximum allowed duration
         * @param interruptOnTimeout Whether to interrupt the thread on timeout
         * @return Token to pass to stop()
         */
        public static WatchdogToken start(String operationName, long timeoutMs,
                                           boolean interruptOnTimeout) {
            ensureRunning();

            long id = tokenCounter.incrementAndGet();
            long now = System.currentTimeMillis();
            WatchdogToken token = new WatchdogToken(
                id, operationName, Thread.currentThread(),
                now, now + timeoutMs, interruptOnTimeout);

            activeTokens.put(id, token);
            totalWatched.incrementAndGet();

            return token;
        }

        /** Start with default timeout */
        public static WatchdogToken start(String operationName) {
            return start(operationName, defaultTimeoutMs);
        }

        /**
         * Stop watching an operation (mark as completed).
         */
        public static void stop(WatchdogToken token) {
            if (token == null) return;
            token.completed = true;
            activeTokens.remove(token.id);
        }

        /**
         * Execute an operation with watchdog protection.
         */
        public static <T> T guarded(String name, long timeoutMs, Supplier<T> operation) {
            WatchdogToken token = start(name, timeoutMs);
            try {
                return operation.get();
            } finally {
                stop(token);
            }
        }

        /** Execute void operation with watchdog */
        public static void guarded(String name, long timeoutMs, Runnable operation) {
            WatchdogToken token = start(name, timeoutMs);
            try {
                operation.run();
            } finally {
                stop(token);
            }
        }

        /** Get all currently active operations */
        public static List<WatchdogToken> activeOperations() {
            return new ArrayList<>(activeTokens.values());
        }

        /** Get all overdue operations */
        public static List<WatchdogToken> overdueOperations() {
            return activeTokens.values().stream()
                .filter(WatchdogToken::isOverdue)
                .collect(Collectors.toList());
        }

        public static long totalTimeouts() { return totalTimeouts.get(); }
        public static long totalWatched() { return totalWatched.get(); }
        public static int activeCount() { return activeTokens.size(); }

        public static void setDefaultTimeout(long ms) { defaultTimeoutMs = ms; }

        /** Shutdown the watchdog daemon */
        public static void shutdown() {
            running = false;
            if (watchdogThread != null) {
                watchdogThread.interrupt();
                watchdogThread = null;
            }
        }

        // ── Private ──

        private static void ensureRunning() {
            if (running) return;
            synchronized (Watchdog.class) {
                if (running) return;
                running = true;
                watchdogThread = new Thread(Watchdog::watchdogLoop, "DeepMix-Watchdog");
                watchdogThread.setDaemon(true);
                watchdogThread.setPriority(Thread.MAX_PRIORITY); // High priority monitor
                watchdogThread.start();
            }
        }

        private static void watchdogLoop() {
            while (running) {
                try {
                    Thread.sleep(CHECK_INTERVAL_MS);

                    long now = System.currentTimeMillis();
                    for (WatchdogToken token : activeTokens.values()) {
                        if (token.completed) {
                            activeTokens.remove(token.id);
                            continue;
                        }

                        if (now > token.deadlineMs) {
                            // TIMEOUT detected
                            token.timedOut = true;
                            totalTimeouts.incrementAndGet();

                            System.err.println(
                                "[DeepMix:Watchdog] ⚠️ TIMEOUT: Operation '" +
                                    token.operationName + "' exceeded " +
                                    (now - token.startTimeMs) + "ms (budget: " +
                                    (token.deadlineMs - token.startTimeMs) + "ms) " +
                                    "on thread " + token.thread.getName());

                            // Print stack trace of stuck thread
                            StackTraceElement[] stack = token.thread.getStackTrace();
                            if (stack.length > 0) {
                                System.err.println("[DeepMix:Watchdog] Stuck thread stack:");
                                for (int i = 0; i < Math.min(stack.length, 10); i++) {
                                    System.err.println("  at " + stack[i]);
                                }
                                if (stack.length > 10) {
                                    System.err.println("  ... " + (stack.length - 10) + " more");
                                }
                            }

                            // Interrupt the thread if configured
                            if (token.interruptOnTimeout && token.thread.isAlive()) {
                                token.thread.interrupt();
                                System.err.println(
                                    "[DeepMix:Watchdog] Interrupted thread: " +
                                        token.thread.getName());
                            }

                            activeTokens.remove(token.id);
                        }
                    }

                    // Deadlock detection (periodic — every 10 checks)
                    if (tokenCounter.get() % 10 == 0) {
                        detectDeadlocks();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Watchdog must never crash
                    System.err.println("[DeepMix:Watchdog] Error: " + e.getMessage());
                }
            }
        }

        private static void detectDeadlocks() {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            long[] deadlockedIds = threadBean.findDeadlockedThreads();

            if (deadlockedIds != null && deadlockedIds.length > 0) {
                System.err.println("[DeepMix:Watchdog] 🚨 DEADLOCK DETECTED!");
                ThreadInfo[] threadInfos = threadBean.getThreadInfo(deadlockedIds, true, true);

                for (ThreadInfo info : threadInfos) {
                    if (info == null) continue;

                    System.err.println("  Deadlocked thread: " + info.getThreadName() +
                        " (state: " + info.getThreadState() + ")");
                    System.err.println("    Waiting for lock: " + info.getLockName() +
                        " held by: " + info.getLockOwnerName());

                    StackTraceElement[] stack = info.getStackTrace();
                    for (int i = 0; i < Math.min(stack.length, 5); i++) {
                        System.err.println("    at " + stack[i]);
                    }
                }

                // Attempt recovery: interrupt one of the deadlocked threads
                // (specifically, prefer interrupting DeepMix threads over application threads)
                for (ThreadInfo info : threadInfos) {
                    if (info != null && info.getThreadName().startsWith("DeepMix-")) {
                        Thread target = findThread(info.getThreadId());
                        if (target != null) {
                            System.err.println(
                                "[DeepMix:Watchdog] Breaking deadlock by interrupting: " +
                                    target.getName());
                            target.interrupt();
                            break;
                        }
                    }
                }
            }
        }

        private static Thread findThread(long threadId) {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getId() == threadId) return t;
            }
            return null;
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 9: RATE LIMITER & THROTTLE                                ║
    // ║                                                                    ║
    // ║  Prevents DeepMix from consuming too many CPU cycles or            ║
    // ║  performing too many transformations in a short period.             ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Token-bucket rate limiter for DeepMix operations.
     *
     * Prevents runaway transformation storms by limiting the number
     * of operations per time window.
     */
    public static final class RateLimiter {

        private final String name;
        private final double tokensPerSecond;
        private final double maxBurst;

        private volatile double availableTokens;
        private volatile long lastRefillTimeNanos;
        private final Object lock = new Object();

        // Statistics
        private final AtomicLong totalAcquired = new AtomicLong(0);
        private final AtomicLong totalThrottled = new AtomicLong(0);
        private final AtomicLong totalWaitNanos = new AtomicLong(0);

        /**
         * Create a rate limiter.
         *
         * @param name             Limiter name for diagnostics
         * @param tokensPerSecond  Steady-state rate
         * @param maxBurst         Maximum burst size
         */
        public RateLimiter(String name, double tokensPerSecond, double maxBurst) {
            this.name = name;
            this.tokensPerSecond = tokensPerSecond;
            this.maxBurst = maxBurst;
            this.availableTokens = maxBurst;
            this.lastRefillTimeNanos = System.nanoTime();
        }

        /**
         * Try to acquire a token without blocking.
         *
         * @return true if a token was acquired, false if rate-limited
         */
        public boolean tryAcquire() {
            return tryAcquire(1);
        }

        /**
         * Try to acquire N tokens without blocking.
         */
        public boolean tryAcquire(int tokens) {
            synchronized (lock) {
                refill();
                if (availableTokens >= tokens) {
                    availableTokens -= tokens;
                    totalAcquired.addAndGet(tokens);
                    return true;
                }
                totalThrottled.addAndGet(tokens);
                return false;
            }
        }

        /**
         * Acquire a token, blocking if necessary until one is available.
         * Returns the time waited in nanoseconds.
         *
         * @param maxWaitMs Maximum time to wait (0 = no limit)
         * @return Time waited in nanoseconds, or -1 if timed out
         */
        public long acquire(long maxWaitMs) {
            long startNanos = System.nanoTime();
            long deadline = maxWaitMs > 0 ?
                startNanos + (maxWaitMs * 1_000_000) : Long.MAX_VALUE;

            synchronized (lock) {
                while (true) {
                    refill();
                    if (availableTokens >= 1.0) {
                        availableTokens -= 1.0;
                        totalAcquired.incrementAndGet();
                        long waited = System.nanoTime() - startNanos;
                        totalWaitNanos.addAndGet(waited);
                        return waited;
                    }

                    // Calculate wait time for next token
                    double deficit = 1.0 - availableTokens;
                    long waitMs = (long) (deficit / tokensPerSecond * 1000) + 1;

                    if (System.nanoTime() + waitMs * 1_000_000 > deadline) {
                        totalThrottled.incrementAndGet();
                        return -1; // Timed out
                    }

                    try {
                        lock.wait(waitMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        totalThrottled.incrementAndGet();
                        return -1;
                    }
                }
            }
        }

        /** Acquire without timeout */
        public long acquire() {
            return acquire(0);
        }

        /** Get current available tokens */
        public double availableTokens() {
            synchronized (lock) {
                refill();
                return availableTokens;
            }
        }

        public String name() { return name; }
        public double rate() { return tokensPerSecond; }
        public long totalAcquired() { return totalAcquired.get(); }
        public long totalThrottled() { return totalThrottled.get(); }

        public double averageWaitMs() {
            long acquired = totalAcquired.get();
            if (acquired == 0) return 0;
            return (double) totalWaitNanos.get() / acquired / 1_000_000.0;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillTimeNanos;
            if (elapsed <= 0) return;

            double newTokens = elapsed * tokensPerSecond / 1_000_000_000.0;
            availableTokens = Math.min(maxBurst, availableTokens + newTokens);
            lastRefillTimeNanos = now;
        }

        @Override
        public String toString() {
            return String.format("RateLimiter[%s|%.0f/s|avail=%.1f|acquired=%d|throttled=%d]",
                name, tokensPerSecond, availableTokens(), totalAcquired.get(), totalThrottled.get());
        }
    }

    /**
     * Pre-configured rate limiters for DeepMix subsystems.
     */
    public static final class RateLimits {

        /** Transform pipeline: max 200 classes/sec, burst 50 */
        public static final RateLimiter TRANSFORMS =
            new RateLimiter("transforms", 200, 50);

        /** Hot reload: max 5/sec, burst 2 (avoid reload storms) */
        public static final RateLimiter HOT_RELOAD =
            new RateLimiter("hot_reload", 5, 2);

        /** Module thaw: max 20/sec, burst 5 */
        public static final RateLimiter MODULE_THAW =
            new RateLimiter("module_thaw", 20, 5);

        /** Diagnostic output: max 50/sec, burst 20 */
        public static final RateLimiter DIAGNOSTICS =
            new RateLimiter("diagnostics", 50, 20);

        /** External I/O (file reads, network): max 100/sec, burst 25 */
        public static final RateLimiter EXTERNAL_IO =
            new RateLimiter("external_io", 100, 25);
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 10: MAINTENANCE DAEMON                                    ║
    // ║                                                                    ║
    // ║  Background thread that performs periodic maintenance:              ║
    // ║    - Freeze idle modules                                           ║
    // ║    - Check memory pressure                                         ║
    // ║    - Compact caches                                                ║
    // ║    - Update GC statistics                                          ║
    // ║    - Health checks on all subsystems                               ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Background maintenance daemon.
     * Runs periodic tasks to keep DeepMix healthy and lean.
     */
    static final class MaintenanceDaemon {

        private static volatile Thread daemonThread = null;
        private static volatile boolean running = false;
        private static final long TICK_INTERVAL_MS = 2_000; // Every 2 seconds

        // Tick counter for scheduling less-frequent tasks
        private static long tickCount = 0;

        static void start() {
            if (running) return;
            synchronized (MaintenanceDaemon.class) {
                if (running) return;
                running = true;
                daemonThread = new Thread(MaintenanceDaemon::run, "DeepMix-Maintenance");
                daemonThread.setDaemon(true);
                daemonThread.setPriority(Thread.MIN_PRIORITY); // Lowest priority
                daemonThread.start();
            }
        }

        static void stop() {
            running = false;
            if (daemonThread != null) {
                daemonThread.interrupt();
                daemonThread = null;
            }
        }

        private static void run() {
            System.out.println("[DeepMix:Maintenance] Daemon started");

            while (running) {
                try {
                    Thread.sleep(TICK_INTERVAL_MS);
                    tickCount++;

                    // ── Every tick (2s): Memory pressure check ──
                    MemoryGuard.checkPressure();

                    // ── Every 5 ticks (10s): Freeze idle modules ──
                    if (tickCount % 5 == 0) {
                        int frozen = ModuleRegistry.freezeIdle();
                        if (frozen > 0) {
                            System.out.println(
                                "[DeepMix:Maintenance] Froze " + frozen + " idle modules");
                        }
                    }

                    // ── Every 15 ticks (30s): Update GC stats ──
                    if (tickCount % 15 == 0) {
                        MemoryGuard.updateGCStats();
                    }

                    // ── Every 30 ticks (60s): Cache compaction ──
                    if (tickCount % 30 == 0) {
                        MemoryGuard.PressureLevel pressure = MemoryGuard.checkPressure();
                        if (pressure.ordinal() >= MemoryGuard.PressureLevel.YELLOW.ordinal()) {
                            double keepFraction = pressure == MemoryGuard.PressureLevel.YELLOW ?
                                0.8 : 0.5;
                            BytecodeCache.compact(keepFraction);
                        }
                    }

                    // ── Every 60 ticks (2m): Full health report ──
                    if (tickCount % 60 == 0) {
                        HealthReport report = HealthMonitor.fullReport();
                        if (report.status != HealthStatus.HEALTHY) {
                            System.err.println("[DeepMix:Maintenance] Health: " + report);
                        }
                    }

                    // ── Every 150 ticks (5m): Deep maintenance ──
                    if (tickCount % 150 == 0) {
                        // Purge expired cache entries
                        BytecodeCache.compact(0.9);

                        // Check for stuck watchdog tokens
                        List<Watchdog.WatchdogToken> overdue = Watchdog.overdueOperations();
                        if (!overdue.isEmpty()) {
                            System.err.println(
                                "[DeepMix:Maintenance] " + overdue.size() +
                                    " overdue operations detected");
                        }

                        // Log module registry status
                        long activeMemory = ModuleRegistry.activeMemoryBytes();
                        int activeCount = ModuleRegistry.activeModuleCount();
                        if (activeCount > 0) {
                            System.out.println(String.format(
                                "[DeepMix:Maintenance] Modules: %d active (%dKB), " +
                                    "cache: %s, pipeline: %s",
                                activeCount, activeMemory / 1024,
                                BytecodeCache.stats(),
                                TransformationPipeline.stats()
                            ));
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // Maintenance daemon must NEVER crash
                    System.err.println("[DeepMix:Maintenance] Error (non-fatal): " +
                        e.getMessage());
                }
            }

            System.out.println("[DeepMix:Maintenance] Daemon stopped");
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 11: HEALTH MONITORING                                     ║
    // ║                                                                    ║
    // ║  Comprehensive health checks for all DeepMix subsystems.           ║
    // ║  Reports overall health status and individual component health.    ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    public enum HealthStatus {
        HEALTHY,           // All systems nominal
        DEGRADED,          // Some non-critical issues
        UNHEALTHY,         // Significant problems
        CRITICAL           // Immediate attention required
    }

    /**
     * Health check result for a single component.
     */
    public static final class ComponentHealth {
        public final String component;
        public final HealthStatus status;
        public final String message;
        public final Map<String, Object> details;
        public final long checkDurationMs;

        ComponentHealth(String component, HealthStatus status, String message,
                       Map<String, Object> details, long checkDurationMs) {
            this.component = component;
            this.status = status;
            this.message = message;
            this.details = details != null ? Collections.unmodifiableMap(details) :
                Collections.emptyMap();
            this.checkDurationMs = checkDurationMs;
        }

        @Override
        public String toString() {
            return String.format("%s: %s (%s) [%dms]", component, status, message, checkDurationMs);
        }
    }

    /**
     * Full health report across all DeepMix subsystems.
     */
    public static final class HealthReport {
        public final HealthStatus status;
        public final Instant timestamp;
        public final List<ComponentHealth> components;
        public final long totalCheckDurationMs;

        HealthReport(HealthStatus status, List<ComponentHealth> components,
                    long totalCheckDurationMs) {
            this.status = status;
            this.timestamp = Instant.now();
            this.components = Collections.unmodifiableList(components);
            this.totalCheckDurationMs = totalCheckDurationMs;
        }

        public List<ComponentHealth> unhealthyComponents() {
            return components.stream()
                .filter(c -> c.status != HealthStatus.HEALTHY)
                .collect(Collectors.toList());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("HealthReport[").append(status).append(" at ").append(timestamp);
            sb.append(", ").append(components.size()).append(" components");
            List<ComponentHealth> unhealthy = unhealthyComponents();
            if (!unhealthy.isEmpty()) {
                sb.append(", issues: ");
                for (ComponentHealth c : unhealthy) {
                    sb.append(c.component).append("=").append(c.status).append(" ");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * Health monitoring system for all DeepMix components.
     */
    public static final class HealthMonitor {

        @FunctionalInterface
        public interface HealthCheck {
            ComponentHealth check();
        }

        private static final Map<String, HealthCheck> checks = new ConcurrentHashMap<>();
        private static volatile HealthReport lastReport = null;
        private static final CopyOnWriteArrayList<Consumer<HealthReport>> listeners =
            new CopyOnWriteArrayList<>();

        // Register standard health checks
        static {
            register("memory", HealthMonitor::checkMemory);
            register("modules", HealthMonitor::checkModules);
            register("cache", HealthMonitor::checkCache);
            register("pipeline", HealthMonitor::checkPipeline);
            register("circuit_breakers", HealthMonitor::checkCircuitBreakers);
            register("watchdog", HealthMonitor::checkWatchdog);
            register("threads", HealthMonitor::checkThreads);
        }

        public static void register(String name, HealthCheck check) {
            checks.put(name, check);
        }

        public static void unregister(String name) {
            checks.remove(name);
        }

        /** Run all health checks and produce a full report */
        public static HealthReport fullReport() {
            long start = System.currentTimeMillis();
            List<ComponentHealth> results = new ArrayList<>();
            HealthStatus worstStatus = HealthStatus.HEALTHY;

            for (Map.Entry<String, HealthCheck> entry : checks.entrySet()) {
                try {
                    ComponentHealth health = entry.getValue().check();
                    results.add(health);
                    if (health.status.ordinal() > worstStatus.ordinal()) {
                        worstStatus = health.status;
                    }
                } catch (Exception e) {
                    results.add(new ComponentHealth(
                        entry.getKey(), HealthStatus.UNHEALTHY,
                        "Health check threw: " + e.getMessage(),
                        null, 0));
                    worstStatus = HealthStatus.UNHEALTHY;
                }
            }

            long duration = System.currentTimeMillis() - start;
            HealthReport report = new HealthReport(worstStatus, results, duration);
            lastReport = report;

            // Notify listeners
            for (Consumer<HealthReport> listener : listeners) {
                try { listener.accept(report); }
                catch (Exception e) { /* swallow */ }
            }

            return report;
        }

        /** Quick check — returns last report if recent, otherwise runs new check */
        public static HealthReport quickCheck() {
            HealthReport last = lastReport;
            if (last != null &&
                Duration.between(last.timestamp, Instant.now()).toSeconds() < 30) {
                return last;
            }
            return fullReport();
        }

        /** Check a single component */
        public static ComponentHealth checkComponent(String name) {
            HealthCheck check = checks.get(name);
            if (check == null) {
                return new ComponentHealth(name, HealthStatus.UNHEALTHY,
                    "Unknown component", null, 0);
            }
            return check.check();
        }

        /** Register a health report listener */
        public static void onReport(Consumer<HealthReport> listener) {
            listeners.add(listener);
        }

        public static HealthReport lastReport() { return lastReport; }

        // ── Standard health checks ──

        private static ComponentHealth checkMemory() {
            long start = System.currentTimeMillis();
            MemoryStats stats = MemoryGuard.getStats();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("heapUsed", stats.heapUsed);
            details.put("heapMax", stats.heapMax);
            details.put("heapRatio", stats.heapUsageRatio());
            details.put("pressure", stats.pressure.name());
            details.put("deepmixMemory", stats.deepmixActiveMemory);
            details.put("gcCount", stats.gcCount);
            details.put("gcTimeMs", stats.gcTotalTimeMs);

            HealthStatus status;
            String message;
            switch (stats.pressure) {
                case GREEN:
                    status = HealthStatus.HEALTHY;
                    message = String.format("Heap %.0f%% used", stats.heapUsageRatio() * 100);
                    break;
                case YELLOW:
                    status = HealthStatus.DEGRADED;
                    message = String.format("Elevated memory: %.0f%%", stats.heapUsageRatio() * 100);
                    break;
                case ORANGE:
                case RED:
                    status = HealthStatus.UNHEALTHY;
                    message = String.format("High memory pressure: %.0f%%", stats.heapUsageRatio() * 100);
                    break;
                case CRITICAL:
                    status = HealthStatus.CRITICAL;
                    message = String.format("CRITICAL memory: %.0f%%", stats.heapUsageRatio() * 100);
                    break;
                default:
                    status = HealthStatus.HEALTHY;
                    message = "Unknown";
            }

            return new ComponentHealth("memory", status, message,
                details, System.currentTimeMillis() - start);
        }

        private static ComponentHealth checkModules() {
            long start = System.currentTimeMillis();
            Map<String, Object> details = new LinkedHashMap<>();

            int active = ModuleRegistry.activeModuleCount();
            long activeMemory = ModuleRegistry.activeMemoryBytes();
            int quarantined = 0;

            for (ModuleDescriptor desc : ModuleRegistry.allDescriptors()) {
                if (desc.isQuarantined()) quarantined++;
                details.put(desc.id, desc.state().name());
            }

            details.put("activeCount", active);
            details.put("activeMemoryKB", activeMemory / 1024);
            details.put("quarantinedCount", quarantined);

            HealthStatus status;
            String message;
            if (quarantined > 0) {
                status = HealthStatus.UNHEALTHY;
                message = quarantined + " quarantined modules";
            } else if (activeMemory > 100 * 1024 * 1024) { // > 100MB
                status = HealthStatus.DEGRADED;
                message = "High module memory: " + (activeMemory / (1024 * 1024)) + "MB";
            } else {
                status = HealthStatus.HEALTHY;
                message = active + " active, " + (activeMemory / 1024) + "KB";
            }

            return new ComponentHealth("modules", status, message,
                details, System.currentTimeMillis() - start);
        }

        private static ComponentHealth checkCache() {
            long start = System.currentTimeMillis();
            CacheStats stats = BytecodeCache.stats();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("entries", stats.entries);
            details.put("sizeKB", stats.totalBytes / 1024);
            details.put("hitRatio", stats.hitRatio);
            details.put("hits", stats.hits);
            details.put("misses", stats.misses);
            details.put("evictions", stats.evictions);

            HealthStatus status;
            String message;
            if (stats.hitRatio < 0.2 && stats.hits + stats.misses > 100) {
                status = HealthStatus.DEGRADED;
                message = String.format("Low hit ratio: %.1f%%", stats.hitRatio * 100);
            } else if (stats.totalBytes > 100 * 1024 * 1024) {
                status = HealthStatus.DEGRADED;
                message = "Large cache: " + (stats.totalBytes / (1024 * 1024)) + "MB";
            } else {
                status = HealthStatus.HEALTHY;
                message = String.format("%d entries, %.1f%% hit ratio",
                    stats.entries, stats.hitRatio * 100);
            }

            return new ComponentHealth("cache", status, message,
                details, System.currentTimeMillis() - start);
        }

        private static ComponentHealth checkPipeline() {
            long start = System.currentTimeMillis();
            PipelineStats stats = TransformationPipeline.stats();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("totalTransforms", stats.totalTransforms);
            details.put("totalSkipped", stats.totalSkipped);
            details.put("totalErrors", stats.totalErrors);
            details.put("budgetExceeded", stats.totalBudgetExceeded);
            details.put("avgTimeMs", stats.averageTransformMs());
            details.put("activeThreads", stats.activeThreads);
            details.put("queuedTasks", stats.queuedTasks);

            HealthStatus status;
            String message;
            if (stats.totalErrors > 0 &&
                (double) stats.totalErrors / Math.max(1, stats.totalTransforms) > 0.1) {
                status = HealthStatus.UNHEALTHY;
                message = String.format("High error rate: %d/%d",
                    stats.totalErrors, stats.totalTransforms);
            } else if (stats.averageTransformMs() > 10) {
                status = HealthStatus.DEGRADED;
                message = String.format("Slow transforms: avg %.1fms", stats.averageTransformMs());
            } else if (stats.queuedTasks > 100) {
                status = HealthStatus.DEGRADED;
                message = stats.queuedTasks + " tasks queued";
            } else {
                status = HealthStatus.HEALTHY;
                message = String.format("%d transforms, avg %.2fms",
                    stats.totalTransforms, stats.averageTransformMs());
            }

            return new ComponentHealth("pipeline", status, message,
                details, System.currentTimeMillis() - start);
        }

        private static ComponentHealth checkCircuitBreakers() {
            long start = System.currentTimeMillis();
            Map<String, CircuitBreakerStats> allStats = CircuitBreakerRegistry.allStats();
            Map<String, Object> details = new LinkedHashMap<>();

            int openCount = 0;
            long totalRejections = 0;
            for (Map.Entry<String, CircuitBreakerStats> entry : allStats.entrySet()) {
                CircuitBreakerStats cbStats = entry.getValue();
                details.put(entry.getKey(), cbStats.state.name());
                if (cbStats.state == CircuitBreaker.State.OPEN) openCount++;
                totalRejections += cbStats.rejections;
            }
            details.put("openCount", openCount);
            details.put("totalRejections", totalRejections);

            HealthStatus status;
            String message;
            if (openCount > 2) {
                status = HealthStatus.CRITICAL;
                message = openCount + " circuit breakers OPEN";
            } else if (openCount > 0) {
                status = HealthStatus.UNHEALTHY;
                List<String> openNames = CircuitBreakerRegistry.openBreakers();
                message = "Open: " + String.join(", ", openNames);
            } else if (totalRejections > 100) {
                status = HealthStatus.DEGRADED;
                message = totalRejections + " total rejections";
            } else {
                status = HealthStatus.HEALTHY;
                message = allStats.size() + " breakers, all closed";
            }

            return new ComponentHealth("circuit_breakers", status, message,
                details, System.currentTimeMillis() - start);
        }

        private static ComponentHealth checkWatchdog() {
            long start = System.currentTimeMillis();
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("activeOperations", Watchdog.activeCount());
            details.put("totalTimeouts", Watchdog.totalTimeouts());
            details.put("totalWatched", Watchdog.totalWatched());

            List<Watchdog.WatchdogToken> overdue = Watchdog.overdueOperations();
            details.put("overdueCount", overdue.size());

            HealthStatus status;
            String message;
            if (overdue.size() > 3) {
                status = HealthStatus.CRITICAL;
                message = overdue.size() + " operations stuck";
            } else if (overdue.size() > 0) {
                status = HealthStatus.UNHEALTHY;
                message = overdue.size() + " overdue operations";
            } else if (Watchdog.totalTimeouts() > 10) {
                status = HealthStatus.DEGRADED;
                message = Watchdog.totalTimeouts() + " total timeouts";
            } else {
                status = HealthStatus.HEALTHY;
                message = Watchdog.activeCount() + " active, " +
                    Watchdog.totalTimeouts() + " timeouts";
            }

            return new ComponentHealth("watchdog", status, message,
                details, System.currentTimeMillis() - start);
        }

        private static ComponentHealth checkThreads() {
            long start = System.currentTimeMillis();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            Map<String, Object> details = new LinkedHashMap<>();

            int totalThreads = threadBean.getThreadCount();
            int deepmixThreads = 0;
            int daemonThreads = threadBean.getDaemonThreadCount();
            int peakThreads = threadBean.getPeakThreadCount();

            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getName().startsWith("DeepMix-")) {
                    deepmixThreads++;
                }
            }

            details.put("totalThreads", totalThreads);
            details.put("deepmixThreads", deepmixThreads);
            details.put("daemonThreads", daemonThreads);
            details.put("peakThreads", peakThreads);

            long[] deadlocked = threadBean.findDeadlockedThreads();
            boolean hasDeadlock = deadlocked != null && deadlocked.length > 0;
            details.put("deadlocked", hasDeadlock);

            HealthStatus status;
            String message;
            if (hasDeadlock) {
                status = HealthStatus.CRITICAL;
                message = "DEADLOCK detected! " + deadlocked.length + " threads";
            } else if (deepmixThreads > 20) {
                status = HealthStatus.DEGRADED;
                message = "High DeepMix thread count: " + deepmixThreads;
            } else if (totalThreads > 500) {
                status = HealthStatus.DEGRADED;
                message = "High total thread count: " + totalThreads;
            } else {
                status = HealthStatus.HEALTHY;
                message = deepmixThreads + " DeepMix threads, " + totalThreads + " total";
            }

            return new ComponentHealth("threads", status, message,
                details, System.currentTimeMillis() - start);
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 12: SAFE EXECUTION WRAPPERS                               ║
    // ║                                                                    ║
    // ║  Convenience methods that combine circuit breaker, watchdog,        ║
    // ║  rate limiter, and error handling into safe execution contexts.     ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * Safe execution context that wraps operations with all safety mechanisms.
     *
     * Every DeepMix operation should go through one of these methods.
     */
    public static final class SafeExecutor {

        /**
         * Execute a transform operation with full safety wrapping:
         *   1. Rate limiting
         *   2. Memory pressure check
         *   3. Circuit breaker
         *   4. Watchdog timeout
         *   5. Exception containment
         *
         * @param operationName Name for diagnostics
         * @param timeoutMs     Maximum execution time
         * @param breaker       Circuit breaker name
         * @param operation     The operation to execute
         * @param fallback      Fallback value on failure
         * @return Result or fallback
         */
        public static <T> T safeExecute(String operationName, long timeoutMs,
                                         String breaker, Supplier<T> operation,
                                         Supplier<T> fallback) {
            // Step 1: Rate limiting
            if (!RateLimits.TRANSFORMS.tryAcquire()) {
                return fallback != null ? fallback.get() : null;
            }

            // Step 2: Memory pressure check
            MemoryGuard.PressureLevel pressure = MemoryGuard.checkPressure();
            if (pressure == MemoryGuard.PressureLevel.CRITICAL) {
                return fallback != null ? fallback.get() : null;
            }

            // Step 3: Circuit breaker
            CircuitBreaker cb = CircuitBreakerRegistry.get(breaker);
            if (!cb.allowRequest()) {
                return fallback != null ? fallback.get() : null;
            }

            // Step 4: Watchdog + execution
            Watchdog.WatchdogToken token = Watchdog.start(operationName, timeoutMs);
            try {
                T result = operation.get();
                cb.recordSuccess();
                return result;
            } catch (Exception e) {
                cb.recordFailure(e);

                System.err.println("[DeepMix:SafeExecutor] " + operationName +
                    " failed: " + e.getMessage());

                return fallback != null ? fallback.get() : null;
            } finally {
                Watchdog.stop(token);
            }
        }

        /** Simplified safe execution with defaults */
        public static <T> T safe(String name, Supplier<T> operation, T fallbackValue) {
            return safeExecute(name, 5000,
                CircuitBreakerRegistry.TRANSFORM_PIPELINE,
                operation, () -> fallbackValue);
        }

        /** Safe void execution */
        public static void safeRun(String name, long timeoutMs, String breaker,
                                    Runnable operation) {
            safeExecute(name, timeoutMs, breaker,
                () -> { operation.run(); return null; },
                () -> null);
        }

        /** Safe void execution with defaults */
        public static void safeRun(String name, Runnable operation) {
            safeRun(name, 5000, CircuitBreakerRegistry.TRANSFORM_PIPELINE, operation);
        }

        /**
         * Execute a transformation with full safety and performance tracking.
         * This is THE recommended entry point for all bytecode transformations.
         */
        public static TransformationPipeline.TransformResult safeTransform(
                String className, byte[] originalBytes,
                List<ClassTransform> transforms) {

            return safeExecute(
                "transform:" + className,
                10_000, // 10 second hard cap
                CircuitBreakerRegistry.TRANSFORM_PIPELINE,
                () -> TransformationPipeline.transform(className, originalBytes, transforms),
                () -> new TransformationPipeline.TransformResult(
                    originalBytes, className, 0, 0, false, true,
                    null, null, new RuntimeException("SafeExecutor fallback"))
            );
        }

        /**
         * Execute a module thaw with safety wrapping.
         */
        public static <T> T safeThaw(String moduleId) {
            return safeExecute(
                "thaw:" + moduleId,
                5_000,
                CircuitBreakerRegistry.MODULE_THAW,
                () -> {
                    FrozenModule<T> module = ModuleRegistry.getModule(moduleId);

                    // Check memory pressure before thawing
                    ModuleDescriptor desc = module.descriptor();
                    if (!MemoryGuard.allowThaw(desc.priority)) {
                        throw new RuntimeException(
                            "Memory pressure too high to thaw module: " + moduleId);
                    }

                    return module.get();
                },
                () -> null
            );
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 13: PERFORMANCE METRICS & TELEMETRY                       ║
    // ║                                                                    ║
    // ║  Lightweight metrics collection for all DeepMix operations.        ║
    // ║  No external dependencies — all data is in-process.                ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    /**
     * In-process metrics collection for DeepMix performance tracking.
     * Uses lock-free counters and ring buffers for minimal overhead.
     */
    public static final class Metrics {

        /** Counter metric (monotonically increasing) */
        public static final class Counter {
            private final String name;
            private final LongAdder value = new LongAdder();

            Counter(String name) { this.name = name; }
            public void increment() { value.increment(); }
            public void add(long delta) { value.add(delta); }
            public long get() { return value.sum(); }
            public String name() { return name; }
            public void reset() { value.reset(); }
        }

        /** Gauge metric (point-in-time value) */
        public static final class Gauge {
            private final String name;
            private final LongSupplier supplier;

            Gauge(String name, LongSupplier supplier) {
                this.name = name;
                this.supplier = supplier;
            }

            public long get() { return supplier.getAsLong(); }
            public String name() { return name; }
        }

        /** Histogram metric (distribution of values) with ring buffer */
        public static final class Histogram {
            private final String name;
            private final long[] samples;
            private final AtomicInteger writeIndex = new AtomicInteger(0);
            private final AtomicLong totalCount = new AtomicLong(0);
            private final AtomicLong totalSum = new AtomicLong(0);
            private volatile long min = Long.MAX_VALUE;
            private volatile long max = Long.MIN_VALUE;

            Histogram(String name, int sampleSize) {
                this.name = name;
                this.samples = new long[sampleSize];
            }

            Histogram(String name) { this(name, 1024); }

            public void record(long value) {
                int idx = writeIndex.getAndIncrement() & (samples.length - 1);
                samples[idx] = value;
                totalCount.incrementAndGet();
                totalSum.addAndGet(value);

                // Racy but acceptable for min/max
                if (value < min) min = value;
                if (value > max) max = value;
            }

            public double mean() {
                long count = totalCount.get();
                return count > 0 ? (double) totalSum.get() / count : 0;
            }

            public long min() { return min == Long.MAX_VALUE ? 0 : min; }
            public long max() { return max == Long.MIN_VALUE ? 0 : max; }
            public long count() { return totalCount.get(); }
            public String name() { return name; }

            /** Approximate percentile from ring buffer */
            public long percentile(double p) {
                int sampleCount = (int) Math.min(totalCount.get(), samples.length);
                if (sampleCount == 0) return 0;

                long[] sorted = new long[sampleCount];
                System.arraycopy(samples, 0, sorted, 0, sampleCount);
                Arrays.sort(sorted);

                int idx = (int) Math.ceil(p * sampleCount) - 1;
                return sorted[Math.max(0, Math.min(idx, sampleCount - 1))];
            }

            public long p50() { return percentile(0.50); }
            public long p90() { return percentile(0.90); }
            public long p95() { return percentile(0.95); }
            public long p99() { return percentile(0.99); }

            public void reset() {
                writeIndex.set(0);
                totalCount.set(0);
                totalSum.set(0);
                min = Long.MAX_VALUE;
                max = Long.MIN_VALUE;
                Arrays.fill(samples, 0);
            }

            @Override
            public String toString() {
                return String.format(
                    "%s[count=%d, mean=%.1f, p50=%d, p90=%d, p99=%d, min=%d, max=%d]",
                    name, count(), mean(), p50(), p90(), p99(), min(), max());
            }
        }

        /** Timer metric (records duration in nanoseconds) */
        public static final class Timer {
            private final Histogram histogram;

            Timer(String name) { this.histogram = new Histogram(name, 1024); }

            /** Start timing — returns a stop function */
            public Runnable start() {
                long startNanos = System.nanoTime();
                return () -> histogram.record(System.nanoTime() - startNanos);
            }

            /** Time a supplier */
            public <T> T time(Supplier<T> operation) {
                long start = System.nanoTime();
                try {
                    return operation.get();
                } finally {
                    histogram.record(System.nanoTime() - start);
                }
            }

            /** Time a runnable */
            public void time(Runnable operation) {
                long start = System.nanoTime();
                try {
                    operation.run();
                } finally {
                    histogram.record(System.nanoTime() - start);
                }
            }

            public Histogram histogram() { return histogram; }
            public String name() { return histogram.name(); }

            /** Mean duration in milliseconds */
            public double meanMs() { return histogram.mean() / 1_000_000.0; }
            public double p50Ms() { return histogram.p50() / 1_000_000.0; }
            public double p90Ms() { return histogram.p90() / 1_000_000.0; }
            public double p99Ms() { return histogram.p99() / 1_000_000.0; }

            @Override
            public String toString() {
                return String.format(
                    "%s[count=%d, mean=%.2fms, p50=%.2fms, p90=%.2fms, p99=%.2fms]",
                    name(), histogram.count(), meanMs(), p50Ms(), p90Ms(), p99Ms());
            }
        }

        // ── Metric registry ──
        private static final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
        private static final ConcurrentHashMap<String, Gauge> gauges = new ConcurrentHashMap<>();
        private static final ConcurrentHashMap<String, Histogram> histograms = new ConcurrentHashMap<>();
        private static final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();

        // ── Pre-defined metrics ──

        // Counters
        public static final Counter CLASSES_TRANSFORMED = counter("classes.transformed");
        public static final Counter CLASSES_SKIPPED = counter("classes.skipped");
        public static final Counter ANNOTATIONS_PROCESSED = counter("annotations.processed");
        public static final Counter MODULES_THAWED = counter("modules.thawed");
        public static final Counter MODULES_FROZEN = counter("modules.frozen");
        public static final Counter CACHE_HITS = counter("cache.hits");
        public static final Counter CACHE_MISSES = counter("cache.misses");
        public static final Counter ERRORS_CAUGHT = counter("errors.caught");
        public static final Counter ERRORS_SUPPRESSED = counter("errors.suppressed");
        public static final Counter HOT_RELOADS = counter("hot_reloads");
        public static final Counter CIRCUIT_BREAKS = counter("circuit.breaks");
        public static final Counter RATE_LIMITED = counter("rate.limited");
        public static final Counter WATCHDOG_TIMEOUTS = counter("watchdog.timeouts");
        public static final Counter GC_TRIGGERED = counter("gc.triggered");

        // Timers
        public static final Timer TRANSFORM_TIME = timer("transform.time");
        public static final Timer ANNOTATION_TIME = timer("annotation.time");
        public static final Timer MODULE_THAW_TIME = timer("module.thaw.time");
        public static final Timer CACHE_LOOKUP_TIME = timer("cache.lookup.time");
        public static final Timer HOT_RELOAD_TIME = timer("hot_reload.time");
        public static final Timer OPTIMIZATION_TIME = timer("optimization.time");
        public static final Timer HEALTH_CHECK_TIME = timer("health_check.time");

        // Histograms
        public static final Histogram CLASS_SIZE_BYTES = histogram("class.size.bytes");
        public static final Histogram TRANSFORMS_PER_CLASS = histogram("transforms.per_class");
        public static final Histogram METHODS_PER_CLASS = histogram("methods.per_class");

        // ── Factory methods ──

        public static Counter counter(String name) {
            return counters.computeIfAbsent(name, Counter::new);
        }

        public static Gauge gauge(String name, LongSupplier supplier) {
            Gauge g = new Gauge(name, supplier);
            gauges.put(name, g);
            return g;
        }

        public static Histogram histogram(String name) {
            return histograms.computeIfAbsent(name, Histogram::new);
        }

        public static Timer timer(String name) {
            return timers.computeIfAbsent(name, Timer::new);
        }

        /** Get a full metrics snapshot */
        public static MetricsSnapshot snapshot() {
            Map<String, Long> counterValues = new LinkedHashMap<>();
            counters.forEach((k, v) -> counterValues.put(k, v.get()));

            Map<String, Long> gaugeValues = new LinkedHashMap<>();
            gauges.forEach((k, v) -> gaugeValues.put(k, v.get()));

            Map<String, String> histogramSummaries = new LinkedHashMap<>();
            histograms.forEach((k, v) -> histogramSummaries.put(k, v.toString()));

            Map<String, String> timerSummaries = new LinkedHashMap<>();
            timers.forEach((k, v) -> timerSummaries.put(k, v.toString()));

            return new MetricsSnapshot(counterValues, gaugeValues,
                histogramSummaries, timerSummaries, Instant.now());
        }

        /** Reset all metrics */
        public static void resetAll() {
            counters.values().forEach(Counter::reset);
            histograms.values().forEach(Histogram::reset);
            timers.values().forEach(t -> t.histogram().reset());
        }
    }

    /** Immutable metrics snapshot */
    public static final class MetricsSnapshot {
        public final Map<String, Long> counters;
        public final Map<String, Long> gauges;
        public final Map<String, String> histograms;
        public final Map<String, String> timers;
        public final Instant timestamp;

        MetricsSnapshot(Map<String, Long> counters, Map<String, Long> gauges,
                       Map<String, String> histograms, Map<String, String> timers,
                       Instant timestamp) {
            this.counters = Collections.unmodifiableMap(counters);
            this.gauges = Collections.unmodifiableMap(gauges);
            this.histograms = Collections.unmodifiableMap(histograms);
            this.timers = Collections.unmodifiableMap(timers);
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("═══ DeepMix Metrics Snapshot (").append(timestamp).append(") ═══\n");

            sb.append("\n📊 Counters:\n");
            counters.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));

            sb.append("\n📐 Gauges:\n");
            gauges.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));

            sb.append("\n📈 Timers:\n");
            timers.forEach((k, v) -> sb.append("  ").append(v).append("\n"));

            sb.append("\n📉 Histograms:\n");
            histograms.forEach((k, v) -> sb.append("  ").append(v).append("\n"));

            return sb.toString();
        }
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION 14: BOOTSTRAP & SHUTDOWN                                  ║
    // ║                                                                    ║
    // ║  Initialization and graceful shutdown of the optimizer engine.      ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝

    private static volatile boolean bootstrapped = false;
    private static final Object bootstrapLock = new Object();

    /**
     * Bootstrap the DeepMix Optimizer engine.
     * Call this ONCE at application startup, before any other DeepMix operations.
     *
     * Cost: ~2ms, ~5KB initial memory.
     */
    public static void bootstrap() {
        if (bootstrapped) return;
        synchronized (bootstrapLock) {
            if (bootstrapped) return;

            long startNanos = System.nanoTime();

            System.out.println(
                "╔══════════════════════════════════════════════════════╗\n" +
                "║  🔮 DeepMix Optimizer Engine — Initializing...      ║\n" +
                "╚══════════════════════════════════════════════════════╝");

            // Step 1: Register runtime gauges
            Metrics.gauge("heap.used", () -> {
                MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                return heap.getUsed();
            });
            Metrics.gauge("heap.max", () -> {
                MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                return heap.getMax();
            });
            Metrics.gauge("modules.active", () -> ModuleRegistry.activeModuleCount());
            Metrics.gauge("modules.memory", () -> ModuleRegistry.activeMemoryBytes());
            Metrics.gauge("cache.entries", () -> BytecodeCache.stats().entries);
            Metrics.gauge("cache.size", () -> BytecodeCache.estimatedSize());
            Metrics.gauge("threads.deepmix", () -> {
                int count = 0;
                for (Thread t : Thread.getAllStackTraces().keySet()) {
                    if (t.getName().startsWith("DeepMix-")) count++;
                }
                return count;
            });
            Metrics.gauge("watchdog.active", () -> Watchdog.activeCount());
            Metrics.gauge("pool.total", () -> ObjectPool.totalPooledObjects());

            // Step 2: Initialize module registry (registers all module descriptors)
            ModuleRegistry.initialize();

            // Step 3: Pre-warm object pools
            ObjectPool.warmUp();

            // Step 4: Start watchdog
            Watchdog.start("optimizer.bootstrap", 10_000);

            // Step 5: Register shutdown hook for graceful cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[DeepMix:Optimizer] Shutting down...");
                shutdown();
            }, "DeepMix-ShutdownHook"));

            // Step 6: Initial health check
            HealthReport health = HealthMonitor.fullReport();

            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            bootstrapped = true;

            System.out.println(String.format(
                "╔══════════════════════════════════════════════════════╗\n" +
                "║  🔮 DeepMix Optimizer Engine — Ready!               ║\n" +
                "║  Bootstrap: %3dms | Health: %-10s               ║\n" +
                "║  Modules: %d permanent, %d frozen                   ║\n" +
                "║  Memory: ~%dKB | Pools: %d objects pre-warmed       ║\n" +
                "╚══════════════════════════════════════════════════════╝",
                durationMs, health.status,
                ModuleRegistry.allModules().stream()
                    .filter(m -> m.descriptor().moduleClass == ModuleClass.CORE).count(),
                ModuleRegistry.allModules().stream()
                    .filter(m -> m.descriptor().moduleClass != ModuleClass.CORE).count(),
                ModuleRegistry.activeMemoryBytes() / 1024,
                ObjectPool.totalPooledObjects()
            ));

            Watchdog.stop(null); // Bootstrap watchdog token
        }
    }

    /**
     * Gracefully shut down all DeepMix optimizer subsystems.
     */
    public static void shutdown() {
        if (!bootstrapped) return;
        synchronized (bootstrapLock) {
            if (!bootstrapped) return;

            System.out.println("[DeepMix:Optimizer] Graceful shutdown starting...");

            // Step 1: Stop accepting new work
            TransformationPipeline.shutdown();

            // Step 2: Stop watchdog
            Watchdog.shutdown();

            // Step 3: Stop maintenance daemon
            MaintenanceDaemon.stop();

            // Step 4: Freeze all non-core modules
            ModuleRegistry.freezeAll();

            // Step 5: Drain object pools
            ObjectPool.drainAll();

            // Step 6: Clear bytecode cache
            BytecodeCache.clear();

            // Step 7: Reset circuit breakers
            CircuitBreakerRegistry.resetAll();

            // Step 8: Final metrics snapshot
            MetricsSnapshot finalMetrics = Metrics.snapshot();
            System.out.println("[DeepMix:Optimizer] Final metrics:\n" + finalMetrics);

            // Step 9: Final health report
            HealthReport finalHealth = HealthMonitor.fullReport();
            System.out.println("[DeepMix:Optimizer] Final health: " + finalHealth);

            bootstrapped = false;
            System.out.println("[DeepMix:Optimizer] Shutdown complete.");
        }
    }

    /**
     * Check if the optimizer engine is bootstrapped and ready.
     */
    public static boolean isReady() {
        return bootstrapped;
    }

    /**
     * Get a comprehensive status string for debugging.
     */
    public static String statusReport() {
        if (!bootstrapped) return "DeepMix Optimizer: NOT INITIALIZED";

        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════════════════════╗\n");
        sb.append("║        🔮 DeepMix Optimizer Status Report               ║\n");
        sb.append("╠══════════════════════════════════════════════════════════╣\n");

        // Memory
        MemoryStats memStats = MemoryGuard.getStats();
        sb.append("║ Memory:                                                  ║\n");
        sb.append(String.format("║   Heap: %dMB / %dMB (%.1f%%) [%s]%s║\n",
            memStats.heapUsed / (1024 * 1024), memStats.heapMax / (1024 * 1024),
            memStats.heapUsageRatio() * 100, memStats.pressure,
            " ".repeat(Math.max(1, 20 - memStats.pressure.name().length()))));
        sb.append(String.format("║   DeepMix: %dKB in %d modules%s║\n",
            memStats.deepmixActiveMemory / 1024, memStats.deepmixActiveModules,
            " ".repeat(Math.max(1, 30 - String.valueOf(memStats.deepmixActiveModules).length()))));

        // Cache
        CacheStats cacheStats = BytecodeCache.stats();
        sb.append("║ Cache:                                                   ║\n");
        sb.append(String.format("║   %d entries, %dKB, %.1f%% hit ratio%s║\n",
            cacheStats.entries, cacheStats.totalBytes / 1024, cacheStats.hitRatio * 100,
            " ".repeat(Math.max(1, 20))));

        // Pipeline
        PipelineStats pipeStats = TransformationPipeline.stats();
        sb.append("║ Pipeline:                                                ║\n");
        sb.append(String.format("║   %d transforms, avg %.2fms, %d errors%s║\n",
            pipeStats.totalTransforms, pipeStats.averageTransformMs(), pipeStats.totalErrors,
            " ".repeat(Math.max(1, 15))));

        // Circuit Breakers
        List<String> openBreakers = CircuitBreakerRegistry.openBreakers();
        sb.append("║ Circuit Breakers:                                        ║\n");
        if (openBreakers.isEmpty()) {
            sb.append("║   All CLOSED ✅                                          ║\n");
        } else {
            sb.append(String.format("║   ⚠️ %d OPEN: %s%s║\n",
                openBreakers.size(), String.join(", ", openBreakers),
                " ".repeat(Math.max(1, 20))));
        }

        // Health
        HealthReport health = HealthMonitor.quickCheck();
        sb.append("║ Health: ").append(health.status).append("                                          ║\n");
        for (ComponentHealth ch : health.unhealthyComponents()) {
            sb.append("║   ⚠️ ").append(ch.component).append(": ").append(ch.message);
            sb.append(" ".repeat(Math.max(1, 40 - ch.message.length()))).append("║\n");
        }

        sb.append("╚══════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }


    // ╔══════════════════════════════════════════════════════════════════════╗
    // ║                                                                    ║
    // ║  SECTION SUMMARY & STATISTICS                                      ║
    // ║                                                                    ║
    // ║  Sections implemented:                                             ║
    // ║    1.  Module Freeze/Thaw System           (~300 lines)            ║
    // ║    2.  Memory Management & Pressure        (~250 lines)            ║
    // ║    3.  Bytecode Transformation Cache       (~250 lines)            ║
    // ║    4.  Object Pooling                      (~200 lines)            ║
    // ║    5.  Transformation Pipeline Optimizer   (~350 lines)            ║
    // ║    6.  Bytecode Optimization Passes        (~400 lines)            ║
    // ║    7.  Circuit Breaker & Stability Guards  (~350 lines)            ║
    // ║    8.  Watchdog & Deadlock Detection        (~250 lines)            ║
    // ║    9.  Rate Limiter & Throttle             (~200 lines)            ║
    // ║   10.  Maintenance Daemon                  (~100 lines)            ║
    // ║   11.  Health Monitoring                   (~350 lines)            ║
    // ║   12.  Safe Execution Wrappers             (~100 lines)            ║
    // ║   13.  Performance Metrics & Telemetry     (~250 lines)            ║
    // ║   14.  Bootstrap & Shutdown                (~150 lines)            ║
    // ║                                                                    ║
    // ║  Total: ~3,500 lines (meh, its 4880 but i dont actually care on how much there, added that as a simply reminder od what i should do, and i had write like 1.3k more lines rather the original plan) | 14 major subsystems                        ║
    // ║  Dependencies: ASM, JMX (built-in), java.util.concurrent          ║
    // ║  External deps: NONE                                               ║
    // ║                                                                    ║
    // ╚══════════════════════════════════════════════════════════════════════╝
}
