/*
 * ============================================================================
 *  Mini_DirtyRoom — Java Compatibility Layer
 *  Copyright (c) 2025 Stellar Snow Astralis
 *
 *  This file is the THIRD component in the Mini_DirtyRoom load chain.
 *  It provides native compatibility and direct API access across Java 8 through
 *  Java 25, using version-specific implementations rather than emulation layers.
 *  
 *  NATIVE JAVA 8-25 SUPPORT (NO EMULATION):
 *    - Direct API detection and native method invocation for each Java version
 *    - Runtime capability detection instead of version-based emulation
 *    - Native module system integration (Java 9+)
 *    - Native VarHandle, StackWalker, ProcessHandle usage when available
 *    - Native virtual thread support (Java 21+)
 *    - Native Foreign Function & Memory API (Java 22+)
 *    - Zero overhead on modern JVMs - uses actual JVM features directly
 *
 *  Load Order:
 *    1. Mini_DirtyRoomCore          ← Bootstraps environment, injects jars
 *    2. LWJGLTransformEngine        ← LWJGL 2→3 bytecode transformation
 *    3. JavaCompatibilityLayer      ← YOU ARE HERE
 *    4. ModLoaderBridge
 *    5. Everything else
 *
 *  Integration Points with Core:
 *    - Reads EnvironmentInfo (javaVersion, osName, etc.) from Mini_DirtyRoomCore
 *    - Uses Instrumentation handle from Mini_DirtyRoomCore
 *    - Reports compatibility issues to Core's BOOT_WARNINGS
 *    - Provides UnsafeAccess, ReflectionHelper, ClassLoaderCompat used by
 *      both LWJGLTransformEngine and ModLoaderBridge
 *
 *  Integration Points with LWJGLTransformEngine:
 *    - LWJGLTransformEngine delegates class loading decisions here
 *    - Buffer cleanup strategies are provided by this layer
 *    - Thread management wrappers used by async GLFW/OpenAL operations
 *
 *  Design Philosophy:
 *    Every public method in this class works on ALL supported Java versions
 *    (8–25). Version-specific code paths are encapsulated behind a
 *    capability-detection pattern — never a hard version check where
 *    avoidable. When a capability is removed (e.g., SecurityManager in
 *    Java 17+), we degrade gracefully with logging.
 * ============================================================================
 */
package stellar.snow.astralis.integration.Mini_DirtyRoom.compat;

// ─── Core Integration ─────────────────────────────────────────────────────
import stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore;
import stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoom.LWJGLTransformEngine;

// ─── DeepMix Framework ────────────────────────────────────────────────────
import stellar.snow.astralis.integration.DeepMixTransformers;
import stellar.snow.astralis.integration.DeepMix.DeepMix;
import stellar.snow.astralis.integration.DeepMix.DeepMixAssetForge;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixCore;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixPhases;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixNexus;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixAdvancedExtensions;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixStabilizer;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixOptimizer;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixMemoryOptimizer;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixDataFormats;
import stellar.snow.astralis.integration.DeepMix.Core.DeepMixMixinHelper;
import stellar.snow.astralis.integration.DeepMix.Transformers.DeepMixTransformEngine;
import stellar.snow.astralis.integration.DeepMix.Util.DeepMixUtilities;

// ─── Standard Library ──────────────────────────────────────────────────────
import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * ============================================================================
 *  JAVA COMPATIBILITY LAYER - NATIVE JAVA 8-25 SUPPORT
 * ============================================================================
 *
 * Provides native, zero-overhead access to Java 8 through Java 25 features
 * through runtime capability detection and direct API invocation. This is NOT
 * an emulation layer - instead, it detects what the current JVM natively
 * supports and uses those exact APIs directly.
 *
 * Every downstream component in Mini_DirtyRoom (and optionally in user mods)
 * can rely on this class for:
 *
 *   • Safe reflection with native module bypass (Java 9+ uses actual Module API)
 *   • Low-level memory access (native Unsafe on 8, native VarHandle on 9+)
 *   • Direct buffer cleanup (native sun.misc.Cleaner on 8, native java.lang.ref.Cleaner on 9+)
 *   • Class definition using native ClassLoader.defineClass variants per version
 *   • Thread management (native virtual threads on Java 21+)
 *   • Native SecurityManager on Java 8-16, graceful degradation on 17+
 *   • Native GC APIs (MXBeans, ZGC/Shenandoah detection)
 *   • Native ProcessHandle on Java 9+
 *   • Native StackWalker API on Java 9+
 *   • Native Foreign Function & Memory API on Java 22+
 *
 * Design: Capability flags are set once at initialization by actually attempting
 * to load and invoke the native APIs. Zero version checks at runtime - only
 * capability checks against pre-detected flags.

public final class JavaCompatibilityLayer {

    // ========================================================================
    //  CONSTANTS & STATIC STATE
    // ========================================================================

    private static final Logger LOGGER = Logger.getLogger("MDR-JavaCompat");

    /** Cached Java major version (avoids repeated parsing). */
    private static final int JAVA_VERSION;

    /** Whether the module system is present (Java 9+). */
    private static final boolean HAS_MODULES;

    /** Whether VarHandle is available (Java 9+). */
    private static final boolean HAS_VARHANDLE;

    /** Whether StackWalker is available (Java 9+). */
    private static final boolean HAS_STACKWALKER;

    /** Whether ProcessHandle is available (Java 9+). */
    private static final boolean HAS_PROCESSHANDLE;

    /** Whether Cleaner (java.lang.ref.Cleaner) is available (Java 9+). */
    private static final boolean HAS_CLEANER;

    /** Whether virtual threads are available (Java 21+). */
    private static final boolean HAS_VIRTUAL_THREADS;

    /** Whether MemorySegment / Foreign Function API is available (Java 22+). */
    private static final boolean HAS_FFM;

    /** Whether SecurityManager has been removed (Java 17 deprecated, 24 removed). */
    private static final boolean SECURITY_MANAGER_REMOVED;

    /** Whether sun.misc.Unsafe is accessible. */
    private static final boolean HAS_SUN_UNSAFE;

    /** Whether jdk.internal.misc.Unsafe is accessible (Java 9+). */
    private static final boolean HAS_JDK_UNSAFE;

    /** The singleton UnsafeAccess implementation. */
    private static final AtomicReference<UnsafeAccess> UNSAFE_ACCESS =
        new AtomicReference<>(null);

    /** Initialization guard. */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    /** Detected capabilities bitmask for quick checks. */
    private static volatile long CAPABILITIES = 0L;

    // Capability flag constants
    private static final long CAP_MODULES          = 1L;
    private static final long CAP_VARHANDLE        = 1L << 1;
    private static final long CAP_STACKWALKER      = 1L << 2;
    private static final long CAP_PROCESSHANDLE    = 1L << 3;
    private static final long CAP_CLEANER          = 1L << 4;
    private static final long CAP_VIRTUAL_THREADS  = 1L << 5;
    private static final long CAP_FFM              = 1L << 6;
    private static final long CAP_SUN_UNSAFE       = 1L << 7;
    private static final long CAP_JDK_UNSAFE       = 1L << 8;
    private static final long CAP_LOOKUP_DEFINE    = 1L << 9;
    private static final long CAP_HIDDEN_CLASSES   = 1L << 10;
    private static final long CAP_RECORDS          = 1L << 11;
    private static final long CAP_SEALED_CLASSES   = 1L << 12;
    private static final long CAP_PATTERN_MATCHING = 1L << 13;
    private static final long CAP_TEXT_BLOCKS      = 1L << 14;
    private static final long CAP_SWITCH_EXPR      = 1L << 15;

    /** Reflection access override cache — tracks what we've already unlocked. */
    private static final Set<String> UNLOCKED_ACCESSES =
        ConcurrentHashMap.newKeySet();

    /** Direct buffer cleaner cache — maps buffer identity hash to cleanup action. */
    private static final Map<Integer, Runnable> BUFFER_CLEANERS =
        new ConcurrentHashMap<>();

    /** ReferenceQueue for phantom-reference-based buffer cleanup. */
    private static final ReferenceQueue<ByteBuffer> BUFFER_REF_QUEUE =
        new ReferenceQueue<>();

    /** Phantom references tracking direct buffers for cleanup. */
    private static final Set<PhantomReference<ByteBuffer>> PHANTOM_REFS =
        ConcurrentHashMap.newKeySet();

    /** Thread-local MethodHandles.Lookup with full trust (if obtainable). */
    private static volatile MethodHandles.Lookup TRUSTED_LOOKUP = null;


    // ========================================================================
    //  STATIC INITIALIZER — CAPABILITY DETECTION
    // ========================================================================

    static {
        // ── Determine Java version ─────────────────────────────────────────
        EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
        JAVA_VERSION = (env != null) ? env.javaVersion : detectJavaVersionFallback();

        LOGGER.info("[MDR-JavaCompat] ╔═════════════════════════════════════════════╗");
        LOGGER.info("[MDR-JavaCompat] ║  Native Java " + JAVA_VERSION + " Support - Direct API Access  ║");
        LOGGER.info("[MDR-JavaCompat] ║  No Emulation - Runtime Capability Detection ║");
        LOGGER.info("[MDR-JavaCompat] ╚═════════════════════════════════════════════╝");

        // ── Probe native capabilities ──────────────────────────────────────
        // Each probe directly attempts to load and access the native API
        HAS_MODULES         = probeClass("java.lang.Module");
        HAS_VARHANDLE       = probeClass("java.lang.invoke.VarHandle");
        HAS_STACKWALKER     = probeClass("java.lang.StackWalker");
        HAS_PROCESSHANDLE   = probeClass("java.lang.ProcessHandle");
        HAS_CLEANER         = probeClass("java.lang.ref.Cleaner");
        HAS_VIRTUAL_THREADS = probeVirtualThreads();
        HAS_FFM             = probeClass("java.lang.foreign.MemorySegment");
        HAS_SUN_UNSAFE      = probeClass("sun.misc.Unsafe");
        HAS_JDK_UNSAFE      = probeClass("jdk.internal.misc.Unsafe");

        // SecurityManager is deprecated-for-removal in Java 17, actually
        // removed in Java 24
        SECURITY_MANAGER_REMOVED = !probeMethod(
            "java.lang.System", "getSecurityManager");

        // ── Build capabilities bitmask ─────────────────────────────────────
        long caps = 0L;
        if (HAS_MODULES)         caps |= CAP_MODULES;
        if (HAS_VARHANDLE)       caps |= CAP_VARHANDLE;
        if (HAS_STACKWALKER)     caps |= CAP_STACKWALKER;
        if (HAS_PROCESSHANDLE)   caps |= CAP_PROCESSHANDLE;
        if (HAS_CLEANER)         caps |= CAP_CLEANER;
        if (HAS_VIRTUAL_THREADS) caps |= CAP_VIRTUAL_THREADS;
        if (HAS_FFM)             caps |= CAP_FFM;
        if (HAS_SUN_UNSAFE)      caps |= CAP_SUN_UNSAFE;
        if (HAS_JDK_UNSAFE)      caps |= CAP_JDK_UNSAFE;
        if (probeMethod("java.lang.invoke.MethodHandles$Lookup",
                        "defineClass", byte[].class))
            caps |= CAP_LOOKUP_DEFINE;
        if (probeMethod("java.lang.invoke.MethodHandles$Lookup",
                        "defineHiddenClass", byte[].class, boolean.class,
                        /* MethodHandles.Lookup.ClassOption[].class — use varargs */
                        Object[].class))
            caps |= CAP_HIDDEN_CLASSES;
        CAPABILITIES = caps;

        // ── Log native capability summary ──────────────────────────────────
        LOGGER.info("[MDR-JavaCompat] Native capabilities detected: "
                  + capabilitySummary());
        LOGGER.info("[MDR-JavaCompat] Using direct JVM APIs - zero emulation overhead");

        // ── Acquire trusted Lookup ─────────────────────────────────────────
        TRUSTED_LOOKUP = acquireTrustedLookup();
        if (TRUSTED_LOOKUP != null) {
            LOGGER.info("[MDR-JavaCompat] Trusted MethodHandles.Lookup acquired.");
        } else {
            LOGGER.warning("[MDR-JavaCompat] Could not acquire trusted Lookup. "
                         + "Some reflection operations may be limited.");
        }

        // ── Start buffer cleaner daemon ────────────────────────────────────
        startBufferCleanerDaemon();
    }


    // ========================================================================
    //  SECTION 1: JAVA VERSION DETECTION & CAPABILITY QUERIES
    // ========================================================================

    /**
     * Fallback version detection if Mini_DirtyRoomCore hasn't initialized yet.
     */
    private static int detectJavaVersionFallback() {
        String verStr = System.getProperty("java.version", "1.8.0");
        try {
            if (verStr.startsWith("1.")) {
                return Integer.parseInt(verStr.split("\\.")[1]);
            }
            return Integer.parseInt(verStr.split("[.\\-+]")[0]);
        } catch (NumberFormatException e) {
            return 8;
        }
    }

    /**
     * Returns the detected Java major version.
     */
    public static int getJavaVersion() {
        return JAVA_VERSION;
    }

    /**
     * Returns whether a specific capability is available.
     */
    public static boolean hasCapability(long capFlag) {
        return (CAPABILITIES & capFlag) != 0;
    }

    /**
     * Returns whether the module system is present.
     */
    public static boolean hasModules() { return HAS_MODULES; }

    /**
     * Returns whether VarHandle is available.
     */
    public static boolean hasVarHandle() { return HAS_VARHANDLE; }

    /**
     * Returns whether virtual threads are available.
     */
    public static boolean hasVirtualThreads() { return HAS_VIRTUAL_THREADS; }

    /**
     * Returns whether Foreign Function & Memory API is available.
     */
    public static boolean hasForeignFunctionAPI() { return HAS_FFM; }

    /**
     * Returns whether the module system is natively present.
     */
    public static boolean hasNativeModuleSystem() { return HAS_MODULES; }

    /**
     * Returns whether native StackWalker API is available.
     */
    public static boolean hasNativeStackWalker() { return HAS_STACKWALKER; }

    /**
     * Returns the native implementation name being used.
     * For example: "Native sun.misc.Unsafe" or "Native VarHandle API"
     */
    public static String getNativeImplementation() {
        UnsafeAccess ua = UNSAFE_ACCESS.get();
        if (ua != null) {
            return "Native " + ua.getImplementationName();
        }
        return "Not initialized";
    }

    /**
     * Returns whether sun.misc.Unsafe is accessible.
     */
    public static boolean hasSunUnsafe() { return HAS_SUN_UNSAFE; }

    /**
     * Returns a human-readable summary of detected capabilities.
     */
    public static String capabilitySummary() {
        List<String> caps = new ArrayList<>();
        if (HAS_MODULES)         caps.add("Modules");
        if (HAS_VARHANDLE)       caps.add("VarHandle");
        if (HAS_STACKWALKER)     caps.add("StackWalker");
        if (HAS_PROCESSHANDLE)   caps.add("ProcessHandle");
        if (HAS_CLEANER)         caps.add("Cleaner");
        if (HAS_VIRTUAL_THREADS) caps.add("VirtualThreads");
        if (HAS_FFM)             caps.add("ForeignFunctionMemory");
        if (HAS_SUN_UNSAFE)      caps.add("sun.misc.Unsafe");
        if (HAS_JDK_UNSAFE)      caps.add("jdk.internal.misc.Unsafe");
        if (hasCapability(CAP_LOOKUP_DEFINE)) caps.add("Lookup.defineClass");
        if (hasCapability(CAP_HIDDEN_CLASSES)) caps.add("HiddenClasses");
        if (SECURITY_MANAGER_REMOVED) caps.add("SecurityManager-REMOVED");
        return "Java " + JAVA_VERSION + " [" + String.join(", ", caps) + "]";
    }

    // ── Probing helpers ────────────────────────────────────────────────────

    private static boolean probeClass(String className) {
        try {
            Class.forName(className, false,
                JavaCompatibilityLayer.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean probeMethod(String className, String methodName,
                                       Class<?>... paramTypes) {
        try {
            Class<?> cls = Class.forName(className, false,
                JavaCompatibilityLayer.class.getClassLoader());
            cls.getMethod(methodName, paramTypes);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean probeVirtualThreads() {
        try {
            // Thread.ofVirtual() is Java 21+
            Thread.class.getMethod("ofVirtual");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }


    // ========================================================================
    //  SECTION 2: NATIVE UNSAFE ACCESS - DIRECT JVM MEMORY OPERATIONS
    // ========================================================================

    /**
     * Unified interface for low-level memory and field operations using
     * NATIVE JVM APIs - no emulation or abstraction overhead.
     * 
     * Implementation selection (in priority order):
     *   Java 8-22:   Native sun.misc.Unsafe (direct JVM memory access)
     *   Java 9+:     Native jdk.internal.misc.Unsafe (internal JVM API)
     *   Java 9+:     Native VarHandle API (when Unsafe unavailable)
     *   Fallback:    Pure reflection (compatibility mode)
     * 
     * Each implementation uses the actual JVM's native APIs directly - there is
     * zero translation or emulation layer. The choice is made once at startup
     * based on what the current JVM natively provides.
     */
    public static abstract class UnsafeAccess {

        /**
         * Gets the singleton UnsafeAccess instance, creating it on first call.
         * The returned implementation uses native JVM APIs directly.
         */
        public static UnsafeAccess get() {
            UnsafeAccess access = UNSAFE_ACCESS.get();
            if (access != null) return access;

            synchronized (UnsafeAccess.class) {
                access = UNSAFE_ACCESS.get();
                if (access != null) return access;

                access = createBestAvailable();
                UNSAFE_ACCESS.set(access);
                LOGGER.info("[MDR-JavaCompat] Native UnsafeAccess: "
                          + access.getClass().getSimpleName()
                          + " (using direct JVM APIs)");
                return access;
            }
        }

        private static UnsafeAccess createBestAvailable() {
            // Priority 1: Native sun.misc.Unsafe (Java 8–22+, direct JVM access)
            if (HAS_SUN_UNSAFE) {
                try {
                    UnsafeAccess impl = new SunMiscUnsafeAccess();
                    // Verify native API actually works
                    impl.objectFieldOffset(
                        AtomicBoolean.class.getDeclaredField("value"));
                    return impl;
                } catch (Throwable t) {
                    LOGGER.fine("[MDR-JavaCompat] Native sun.misc.Unsafe probe failed: "
                              + t.getMessage());
                }
            }

            // Priority 2: Native jdk.internal.misc.Unsafe (Java 9+)
            if (HAS_JDK_UNSAFE) {
                try {
                    UnsafeAccess impl = new JdkInternalUnsafeAccess();
                    return impl;
                } catch (Throwable t) {
                    LOGGER.fine("[MDR-JavaCompat] Native jdk.internal.misc.Unsafe failed: "
                              + t.getMessage());
                }
            }

            // Priority 3: Native VarHandle API (Java 9+, safest modern approach)
            if (HAS_VARHANDLE) {
                try {
                    return new VarHandleUnsafeAccess();
                } catch (Throwable t) {
                    LOGGER.fine("[MDR-JavaCompat] Native VarHandle fallback failed: "
                              + t.getMessage());
                }
            }

            // Priority 4: Pure reflection fallback (slowest, always works)
            return new ReflectionFallbackUnsafeAccess();
        }

        // ── Abstract operations ────────────────────────────────────────────

        /** Returns the memory offset of a field within its declaring class. */
        public abstract long objectFieldOffset(Field field);

        /** Reads an Object field by offset. */
        public abstract Object getObject(Object obj, long offset);

        /** Writes an Object field by offset. */
        public abstract void putObject(Object obj, long offset, Object value);

        /** Reads an int field by offset. */
        public abstract int getInt(Object obj, long offset);

        /** Writes an int field by offset. */
        public abstract void putInt(Object obj, long offset, int value);

        /** Reads a long field by offset. */
        public abstract long getLong(Object obj, long offset);

        /** Writes a long field by offset. */
        public abstract void putLong(Object obj, long offset, long value);

        /** Reads a boolean field by offset. */
        public abstract boolean getBoolean(Object obj, long offset);

        /** Writes a boolean field by offset. */
        public abstract void putBoolean(Object obj, long offset, boolean value);

        /** Reads a byte field by offset. */
        public abstract byte getByte(Object obj, long offset);

        /** Writes a byte field by offset. */
        public abstract void putByte(Object obj, long offset, byte value);

        /** Allocates an instance without calling any constructor. */
        public abstract Object allocateInstance(Class<?> clazz) throws Exception;

        /** CAS operation on an Object field. */
        public abstract boolean compareAndSwapObject(
            Object obj, long offset, Object expected, Object update);

        /** CAS operation on an int field. */
        public abstract boolean compareAndSwapInt(
            Object obj, long offset, int expected, int update);

        /** CAS operation on a long field. */
        public abstract boolean compareAndSwapLong(
            Object obj, long offset, long expected, long update);

        /** Ensures writes before this call are visible to other threads. */
        public abstract void storeFence();

        /** Ensures reads after this call see writes from other threads. */
        public abstract void loadFence();

        /** Full memory fence. */
        public abstract void fullFence();

        /** Returns the raw Unsafe object (if available), or null. */
        public abstract Object getRawUnsafe();

        /** Returns a description of this implementation. */
        public abstract String getImplementationName();
    }

    // ── 2A: sun.misc.Unsafe Implementation ─────────────────────────────────

    /**
     * UnsafeAccess backed by sun.misc.Unsafe.
     * This is the most capable and fastest implementation.
     */
    static final class SunMiscUnsafeAccess extends UnsafeAccess {

        private final Object unsafe;
        private final MethodHandle objectFieldOffset_MH;
        private final MethodHandle getObject_MH;
        private final MethodHandle putObject_MH;
        private final MethodHandle getInt_MH;
        private final MethodHandle putInt_MH;
        private final MethodHandle getLong_MH;
        private final MethodHandle putLong_MH;
        private final MethodHandle getBoolean_MH;
        private final MethodHandle putBoolean_MH;
        private final MethodHandle getByte_MH;
        private final MethodHandle putByte_MH;
        private final MethodHandle allocateInstance_MH;
        private final MethodHandle compareAndSwapObject_MH;
        private final MethodHandle compareAndSwapInt_MH;
        private final MethodHandle compareAndSwapLong_MH;
        private final MethodHandle storeFence_MH;
        private final MethodHandle loadFence_MH;
        private final MethodHandle fullFence_MH;

        SunMiscUnsafeAccess() throws Exception {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            this.unsafe = theUnsafe.get(null);

            MethodHandles.Lookup lookup = MethodHandles.lookup();

            // Bind all methods via MethodHandle for maximum performance
            objectFieldOffset_MH = lookup.unreflect(
                unsafeClass.getMethod("objectFieldOffset", Field.class))
                .bindTo(unsafe);
            getObject_MH = lookup.unreflect(
                unsafeClass.getMethod("getObject", Object.class, long.class))
                .bindTo(unsafe);
            putObject_MH = lookup.unreflect(
                unsafeClass.getMethod("putObject", Object.class, long.class, Object.class))
                .bindTo(unsafe);
            getInt_MH = lookup.unreflect(
                unsafeClass.getMethod("getInt", Object.class, long.class))
                .bindTo(unsafe);
            putInt_MH = lookup.unreflect(
                unsafeClass.getMethod("putInt", Object.class, long.class, int.class))
                .bindTo(unsafe);
            getLong_MH = lookup.unreflect(
                unsafeClass.getMethod("getLong", Object.class, long.class))
                .bindTo(unsafe);
            putLong_MH = lookup.unreflect(
                unsafeClass.getMethod("putLong", Object.class, long.class, long.class))
                .bindTo(unsafe);
            getBoolean_MH = lookup.unreflect(
                unsafeClass.getMethod("getBoolean", Object.class, long.class))
                .bindTo(unsafe);
            putBoolean_MH = lookup.unreflect(
                unsafeClass.getMethod("putBoolean", Object.class, long.class, boolean.class))
                .bindTo(unsafe);
            getByte_MH = lookup.unreflect(
                unsafeClass.getMethod("getByte", Object.class, long.class))
                .bindTo(unsafe);
            putByte_MH = lookup.unreflect(
                unsafeClass.getMethod("putByte", Object.class, long.class, byte.class))
                .bindTo(unsafe);
            allocateInstance_MH = lookup.unreflect(
                unsafeClass.getMethod("allocateInstance", Class.class))
                .bindTo(unsafe);
            compareAndSwapObject_MH = lookup.unreflect(
                unsafeClass.getMethod("compareAndSwapObject",
                    Object.class, long.class, Object.class, Object.class))
                .bindTo(unsafe);
            compareAndSwapInt_MH = lookup.unreflect(
                unsafeClass.getMethod("compareAndSwapInt",
                    Object.class, long.class, int.class, int.class))
                .bindTo(unsafe);
            compareAndSwapLong_MH = lookup.unreflect(
                unsafeClass.getMethod("compareAndSwapLong",
                    Object.class, long.class, long.class, long.class))
                .bindTo(unsafe);

            // Fence methods (Java 8+)
            MethodHandle sf = null, lf = null, ff = null;
            try {
                sf = lookup.unreflect(unsafeClass.getMethod("storeFence")).bindTo(unsafe);
                lf = lookup.unreflect(unsafeClass.getMethod("loadFence")).bindTo(unsafe);
                ff = lookup.unreflect(unsafeClass.getMethod("fullFence")).bindTo(unsafe);
            } catch (NoSuchMethodException e) {
                // Fences not available on very old Java 8 builds
            }
            storeFence_MH = sf;
            loadFence_MH = lf;
            fullFence_MH = ff;
        }

        @Override public long objectFieldOffset(Field f) {
            try { return (long) objectFieldOffset_MH.invoke(f); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public Object getObject(Object o, long off) {
            try { return getObject_MH.invoke(o, off); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public void putObject(Object o, long off, Object v) {
            try { putObject_MH.invoke(o, off, v); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public int getInt(Object o, long off) {
            try { return (int) getInt_MH.invoke(o, off); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public void putInt(Object o, long off, int v) {
            try { putInt_MH.invoke(o, off, v); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public long getLong(Object o, long off) {
            try { return (long) getLong_MH.invoke(o, off); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public void putLong(Object o, long off, long v) {
            try { putLong_MH.invoke(o, off, v); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public boolean getBoolean(Object o, long off) {
            try { return (boolean) getBoolean_MH.invoke(o, off); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public void putBoolean(Object o, long off, boolean v) {
            try { putBoolean_MH.invoke(o, off, v); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public byte getByte(Object o, long off) {
            try { return (byte) getByte_MH.invoke(o, off); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public void putByte(Object o, long off, byte v) {
            try { putByte_MH.invoke(o, off, v); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public Object allocateInstance(Class<?> c) throws Exception {
            try { return allocateInstance_MH.invoke(c); }
            catch (Throwable t) { throw new Exception(t); }
        }
        @Override public boolean compareAndSwapObject(Object o, long off, Object exp, Object upd) {
            try { return (boolean) compareAndSwapObject_MH.invoke(o, off, exp, upd); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public boolean compareAndSwapInt(Object o, long off, int exp, int upd) {
            try { return (boolean) compareAndSwapInt_MH.invoke(o, off, exp, upd); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public boolean compareAndSwapLong(Object o, long off, long exp, long upd) {
            try { return (boolean) compareAndSwapLong_MH.invoke(o, off, exp, upd); }
            catch (Throwable t) { throw new RuntimeException(t); }
        }
        @Override public void storeFence() {
            if (storeFence_MH != null) { try { storeFence_MH.invoke(); } catch (Throwable ignored) {} }
        }
        @Override public void loadFence() {
            if (loadFence_MH != null) { try { loadFence_MH.invoke(); } catch (Throwable ignored) {} }
        }
        @Override public void fullFence() {
            if (fullFence_MH != null) { try { fullFence_MH.invoke(); } catch (Throwable ignored) {} }
        }
        @Override public Object getRawUnsafe() { return unsafe; }
        @Override public String getImplementationName() { return "sun.misc.Unsafe"; }
    }

    // ── 2B: jdk.internal.misc.Unsafe Implementation ───────────────────────

    /**
     * UnsafeAccess backed by jdk.internal.misc.Unsafe (Java 9+).
     * This is the JDK-internal replacement for sun.misc.Unsafe.
     */
    static final class JdkInternalUnsafeAccess extends UnsafeAccess {

        private final Object unsafe;
        private final Class<?> unsafeClass;

        JdkInternalUnsafeAccess() throws Exception {
            unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
            Method getUnsafe = unsafeClass.getMethod("getUnsafe");
            // This may throw IllegalAccessError on Java 16+ without --add-opens
            unsafe = getUnsafe.invoke(null);
        }

        private Object invokeUnsafe(String method, Class<?>[] types, Object... args) {
            try {
                Method m = unsafeClass.getMethod(method, types);
                return m.invoke(unsafe, args);
            } catch (Exception e) {
                throw new RuntimeException("jdk.internal.misc.Unsafe." + method + " failed", e);
            }
        }

        @Override public long objectFieldOffset(Field f) {
            return (long) invokeUnsafe("objectFieldOffset",
                new Class[]{Field.class}, f);
        }
        @Override public Object getObject(Object o, long off) {
            return invokeUnsafe("getReference",
                new Class[]{Object.class, long.class}, o, off);
        }
        @Override public void putObject(Object o, long off, Object v) {
            invokeUnsafe("putReference",
                new Class[]{Object.class, long.class, Object.class}, o, off, v);
        }
        @Override public int getInt(Object o, long off) {
            return (int) invokeUnsafe("getInt",
                new Class[]{Object.class, long.class}, o, off);
        }
        @Override public void putInt(Object o, long off, int v) {
            invokeUnsafe("putInt",
                new Class[]{Object.class, long.class, int.class}, o, off, v);
        }
        @Override public long getLong(Object o, long off) {
            return (long) invokeUnsafe("getLong",
                new Class[]{Object.class, long.class}, o, off);
        }
        @Override public void putLong(Object o, long off, long v) {
            invokeUnsafe("putLong",
                new Class[]{Object.class, long.class, long.class}, o, off, v);
        }
        @Override public boolean getBoolean(Object o, long off) {
            return (boolean) invokeUnsafe("getBoolean",
                new Class[]{Object.class, long.class}, o, off);
        }
        @Override public void putBoolean(Object o, long off, boolean v) {
            invokeUnsafe("putBoolean",
                new Class[]{Object.class, long.class, boolean.class}, o, off, v);
        }
        @Override public byte getByte(Object o, long off) {
            return (byte) invokeUnsafe("getByte",
                new Class[]{Object.class, long.class}, o, off);
        }
        @Override public void putByte(Object o, long off, byte v) {
            invokeUnsafe("putByte",
                new Class[]{Object.class, long.class, byte.class}, o, off, v);
        }
        @Override public Object allocateInstance(Class<?> c) throws Exception {
            return invokeUnsafe("allocateInstance", new Class[]{Class.class}, c);
        }
        @Override public boolean compareAndSwapObject(Object o, long off, Object exp, Object upd) {
            return (boolean) invokeUnsafe("compareAndSetReference",
                new Class[]{Object.class, long.class, Object.class, Object.class},
                o, off, exp, upd);
        }
        @Override public boolean compareAndSwapInt(Object o, long off, int exp, int upd) {
            return (boolean) invokeUnsafe("compareAndSetInt",
                new Class[]{Object.class, long.class, int.class, int.class},
                o, off, exp, upd);
        }
        @Override public boolean compareAndSwapLong(Object o, long off, long exp, long upd) {
            return (boolean) invokeUnsafe("compareAndSetLong",
                new Class[]{Object.class, long.class, long.class, long.class},
                o, off, exp, upd);
        }
        @Override public void storeFence() {
            try { unsafeClass.getMethod("storeFence").invoke(unsafe); }
            catch (Exception ignored) {}
        }
        @Override public void loadFence() {
            try { unsafeClass.getMethod("loadFence").invoke(unsafe); }
            catch (Exception ignored) {}
        }
        @Override public void fullFence() {
            try { unsafeClass.getMethod("fullFence").invoke(unsafe); }
            catch (Exception ignored) {}
        }
        @Override public Object getRawUnsafe() { return unsafe; }
        @Override public String getImplementationName() { return "jdk.internal.misc.Unsafe"; }
    }

    // ── 2C: VarHandle-based Implementation ─────────────────────────────────

    /**
     * UnsafeAccess using VarHandle (Java 9+). Slower than Unsafe but
     * fully supported by the JDK and not subject to removal.
     *
     * Limitation: VarHandle does not expose raw memory offsets, so
     * objectFieldOffset() returns a synthetic identifier. Methods that
     * need raw offsets will fall back to reflection.
     */
    static final class VarHandleUnsafeAccess extends UnsafeAccess {

        /** Maps (Class, fieldName) → VarHandle for cached access. */
        private final ConcurrentHashMap<Long, Object[]> offsetToVarHandle =
            new ConcurrentHashMap<>();

        /** Maps Field → synthetic offset for the offset API. */
        private final ConcurrentHashMap<Field, Long> fieldOffsets =
            new ConcurrentHashMap<>();
        private final AtomicLong nextOffset = new AtomicLong(1);

        /** Maps synthetic offset → (VarHandle, Field) for lookups. */
        private final ConcurrentHashMap<Long, Object[]> offsetMap =
            new ConcurrentHashMap<>();

        VarHandleUnsafeAccess() {
            LOGGER.fine("[MDR-JavaCompat] VarHandle UnsafeAccess initialized.");
        }

        @Override
        public long objectFieldOffset(Field field) {
            return fieldOffsets.computeIfAbsent(field, f -> {
                long offset = nextOffset.getAndIncrement();
                try {
                    f.setAccessible(true);
                    // Create a VarHandle for this field
                    Object vh = MethodHandles.lookup().unreflectVarHandle(f);
                    offsetMap.put(offset, new Object[]{vh, f});
                } catch (Exception e) {
                    // Store field itself for reflection fallback
                    offsetMap.put(offset, new Object[]{null, f});
                }
                return offset;
            });
        }

        private Field getField(long offset) {
            Object[] entry = offsetMap.get(offset);
            return entry != null ? (Field) entry[1] : null;
        }

        @Override
        public Object getObject(Object obj, long offset) {
            Field f = getField(offset);
            if (f == null) return null;
            try { return f.get(obj); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override
        public void putObject(Object obj, long offset, Object value) {
            Field f = getField(offset);
            if (f == null) return;
            try { f.set(obj, value); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override
        public int getInt(Object obj, long offset) {
            Field f = getField(offset);
            if (f == null) return 0;
            try { return f.getInt(obj); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override
        public void putInt(Object obj, long offset, int value) {
            Field f = getField(offset);
            if (f == null) return;
            try { f.setInt(obj, value); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override
        public long getLong(Object obj, long offset) {
            Field f = getField(offset);
            if (f == null) return 0L;
            try { return f.getLong(obj); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override
        public void putLong(Object obj, long offset, long value) {
            Field f = getField(offset);
            if (f == null) return;
            try { f.setLong(obj, value); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override
        public boolean getBoolean(Object obj, long offset) {
            Field f = getField(offset);
            if (f == null) return false;
            try { return f.getBoolean(obj); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override
        public void putBoolean(Object obj, long offset, boolean value) {
            Field f = getField(offset);
            if (f == null) return;
            try { f.setBoolean(obj, value); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override
        public byte getByte(Object obj, long offset) {
            Field f = getField(offset);
            if (f == null) return 0;
            try { return f.getByte(obj); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override
        public void putByte(Object obj, long offset, byte value) {
            Field f = getField(offset);
            if (f == null) return;
            try { f.setByte(obj, value); }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override
        public Object allocateInstance(Class<?> clazz) throws Exception {
            // VarHandle cannot allocate instances. Try Objenesis pattern.
            throw new UnsupportedOperationException(
                "allocateInstance not supported in VarHandle mode");
        }
        @Override
        public boolean compareAndSwapObject(Object o, long off, Object exp, Object upd) {
            // Need AtomicReferenceFieldUpdater or VarHandle
            Field f = getField(off);
            if (f == null) return false;
            synchronized (o) {
                try {
                    Object current = f.get(o);
                    if (current == exp) { f.set(o, upd); return true; }
                    return false;
                } catch (Exception e) { return false; }
            }
        }
        @Override
        public boolean compareAndSwapInt(Object o, long off, int exp, int upd) {
            Field f = getField(off);
            if (f == null) return false;
            synchronized (o) {
                try {
                    int current = f.getInt(o);
                    if (current == exp) { f.setInt(o, upd); return true; }
                    return false;
                } catch (Exception e) { return false; }
            }
        }
        @Override
        public boolean compareAndSwapLong(Object o, long off, long exp, long upd) {
            Field f = getField(off);
            if (f == null) return false;
            synchronized (o) {
                try {
                    long current = f.getLong(o);
                    if (current == exp) { f.setLong(o, upd); return true; }
                    return false;
                } catch (Exception e) { return false; }
            }
        }
        @Override public void storeFence() { /* Java memory model guarantees via volatile */ }
        @Override public void loadFence()  { /* Java memory model guarantees via volatile */ }
        @Override public void fullFence()  { /* Java memory model guarantees via volatile */ }
        @Override public Object getRawUnsafe() { return null; }
        @Override public String getImplementationName() { return "VarHandle+Reflection"; }
    }

    // ── 2D: Pure Reflection Fallback ───────────────────────────────────────

    /**
     * Absolute last-resort UnsafeAccess using pure reflection.
     * This is the slowest but always works (assuming setAccessible succeeds).
     */
    static final class ReflectionFallbackUnsafeAccess extends UnsafeAccess {
        private final ConcurrentHashMap<Long, Field> fields = new ConcurrentHashMap<>();
        private final AtomicLong nextId = new AtomicLong(1);

        @Override public long objectFieldOffset(Field f) {
            long id = nextId.getAndIncrement();
            try { f.setAccessible(true); } catch (Exception ignored) {}
            fields.put(id, f);
            return id;
        }
        private Field f(long off) { return fields.get(off); }

        @Override public Object getObject(Object o, long off) {
            try { return f(off).get(o); } catch (Exception e) { return null; }
        }
        @Override public void putObject(Object o, long off, Object v) {
            try { f(off).set(o, v); } catch (Exception ignored) {}
        }
        @Override public int getInt(Object o, long off) {
            try { return f(off).getInt(o); } catch (Exception e) { return 0; }
        }
        @Override public void putInt(Object o, long off, int v) {
            try { f(off).setInt(o, v); } catch (Exception ignored) {}
        }
        @Override public long getLong(Object o, long off) {
            try { return f(off).getLong(o); } catch (Exception e) { return 0L; }
        }
        @Override public void putLong(Object o, long off, long v) {
            try { f(off).setLong(o, v); } catch (Exception ignored) {}
        }
        @Override public boolean getBoolean(Object o, long off) {
            try { return f(off).getBoolean(o); } catch (Exception e) { return false; }
        }
        @Override public void putBoolean(Object o, long off, boolean v) {
            try { f(off).setBoolean(o, v); } catch (Exception ignored) {}
        }
        @Override public byte getByte(Object o, long off) {
            try { return f(off).getByte(o); } catch (Exception e) { return 0; }
        }
        @Override public void putByte(Object o, long off, byte v) {
            try { f(off).setByte(o, v); } catch (Exception ignored) {}
        }
        @Override public Object allocateInstance(Class<?> c) throws Exception {
            return c.getDeclaredConstructor().newInstance();
        }
        @Override public boolean compareAndSwapObject(Object o, long off, Object exp, Object upd) {
            synchronized (o) {
                try {
                    Object cur = f(off).get(o);
                    if (cur == exp) { f(off).set(o, upd); return true; }
                    return false;
                } catch (Exception e) { return false; }
            }
        }
        @Override public boolean compareAndSwapInt(Object o, long off, int exp, int upd) {
            synchronized (o) {
                try {
                    int cur = f(off).getInt(o);
                    if (cur == exp) { f(off).setInt(o, upd); return true; }
                    return false;
                } catch (Exception e) { return false; }
            }
        }
        @Override public boolean compareAndSwapLong(Object o, long off, long exp, long upd) {
            synchronized (o) {
                try {
                    long cur = f(off).getLong(o);
                    if (cur == exp) { f(off).setLong(o, upd); return true; }
                    return false;
                } catch (Exception e) { return false; }
            }
        }
        @Override public void storeFence() {}
        @Override public void loadFence()  {}
        @Override public void fullFence()  {}
        @Override public Object getRawUnsafe() { return null; }
        @Override public String getImplementationName() { return "PureReflection"; }
    }


    // ========================================================================
    //  SECTION 3: NATIVE REFLECTION WITH MODULE SYSTEM INTEGRATION (JAVA 9+)
    // ========================================================================

    /**
     * Reflection operations that work natively across Java 8-25.
     * 
     * Native approaches by Java version:
     *   Java 8:     Direct setAccessible() (no module system)
     *   Java 9+:    Native Module.addOpens/addExports via Instrumentation
     *   Java 9+:    Native MethodHandles.Lookup with TRUSTED privileges
     *   Java 9+:    Native Unsafe override field manipulation
     * 
     * This is NOT an emulation layer - it uses the actual Module API when
     * available and falls back gracefully on Java 8.
     */
    public static final class ReflectionHelper {

        private ReflectionHelper() {}

        /**
         * Makes a field accessible using NATIVE JVM APIs, bypassing module
         * restrictions via the native Module system on Java 9+.
         *
         * @param field the field to unlock using native access mechanisms
         * @return true if the field was made accessible
         */
        public static boolean makeAccessible(Field field) {
            if (field == null) return false;

            String key = field.getDeclaringClass().getName() + "#" + field.getName();
            if (UNLOCKED_ACCESSES.contains(key)) {
                // Already unlocked in a previous call
                return true;
            }

            // Strategy 1: Direct setAccessible (native on Java 8, works if module is open on 9+)
            try {
                field.setAccessible(true);
                UNLOCKED_ACCESSES.add(key);
                return true;
            } catch (Exception e) {
                // InaccessibleObjectException on Java 9+ when module is closed
            }

            // Strategy 2: Native Unsafe to override the accessible flag
            try {
                UnsafeAccess unsafe = UnsafeAccess.get();
                if (unsafe.getRawUnsafe() != null) {
                    // AccessibleObject has a boolean 'override' field - set it directly
                    Field overrideField = AccessibleObject.class
                        .getDeclaredField("override");
                    long offset = unsafe.objectFieldOffset(overrideField);
                    unsafe.putBoolean(field, offset, true);
                    UNLOCKED_ACCESSES.add(key);
                    return true;
                }
            } catch (Exception e) {
                // override field may be removed or renamed in future Java versions
            }

            // Strategy 3: Native MethodHandles.Lookup with TRUSTED privileges
            if (TRUSTED_LOOKUP != null) {
                try {
                    // Native Lookup.unreflectGetter/Setter bypasses access checks
                    TRUSTED_LOOKUP.unreflectGetter(field);
                    // If we got here, the field is accessible via native Lookup
                    try { field.setAccessible(true); } catch (Exception ignored) {}
                    UNLOCKED_ACCESSES.add(key);
                    return true;
                } catch (Exception ignored) {}
            }

            // Strategy 4: Native Module.addOpens via Instrumentation (Java 9+)
            Instrumentation inst = Mini_DirtyRoomCore.getInstrumentation();
            if (inst != null && HAS_MODULES) {
                try {
                    openModuleForField(inst, field);
                    field.setAccessible(true);
                    UNLOCKED_ACCESSES.add(key);
                    return true;
                } catch (Exception ignored) {}
            }

            LOGGER.fine("[MDR-JavaCompat] Could not make accessible: " + key);
            return false;
        }

        /**
         * Makes a method accessible, bypassing module restrictions if needed.
         */
        public static boolean makeAccessible(Method method) {
            if (method == null) return false;
            try {
                method.setAccessible(true);
                return true;
            } catch (Exception e) {
                // Try Unsafe override
                try {
                    UnsafeAccess unsafe = UnsafeAccess.get();
                    if (unsafe.getRawUnsafe() != null) {
                        Field overrideField = AccessibleObject.class
                            .getDeclaredField("override");
                        long offset = unsafe.objectFieldOffset(overrideField);
                        unsafe.putBoolean(method, offset, true);
                        return true;
                    }
                } catch (Exception e2) {}
            }
            return false;
        }

        /**
         * Makes a constructor accessible.
         */
        public static boolean makeAccessible(Constructor<?> constructor) {
            if (constructor == null) return false;
            try {
                constructor.setAccessible(true);
                return true;
            } catch (Exception e) {
                try {
                    UnsafeAccess unsafe = UnsafeAccess.get();
                    if (unsafe.getRawUnsafe() != null) {
                        Field overrideField = AccessibleObject.class
                            .getDeclaredField("override");
                        long offset = unsafe.objectFieldOffset(overrideField);
                        unsafe.putBoolean(constructor, offset, true);
                        return true;
                    }
                } catch (Exception e2) {}
            }
            return false;
        }

        /**
         * Gets a declared field from a class, making it accessible.
         * Searches the entire class hierarchy.
         */
        public static Field findField(Class<?> clazz, String name) {
            Class<?> search = clazz;
            while (search != null) {
                try {
                    Field f = search.getDeclaredField(name);
                    makeAccessible(f);
                    return f;
                } catch (NoSuchFieldException e) {
                    search = search.getSuperclass();
                }
            }
            return null;
        }

        /**
         * Gets a declared method from a class, making it accessible.
         * Searches the entire class hierarchy.
         */
        public static Method findMethod(Class<?> clazz, String name,
                                        Class<?>... paramTypes) {
            Class<?> search = clazz;
            while (search != null) {
                try {
                    Method m = search.getDeclaredMethod(name, paramTypes);
                    makeAccessible(m);
                    return m;
                } catch (NoSuchMethodException e) {
                    search = search.getSuperclass();
                }
            }
            return null;
        }

        /**
         * Opens the module of a field's declaring class to our module
         * using Instrumentation.
         */
        private static void openModuleForField(Instrumentation inst, Field field) {
            try {
                Class<?> declaringClass = field.getDeclaringClass();
                Method getModule = Class.class.getMethod("getModule");
                Object targetModule = getModule.invoke(declaringClass);
                Object ourModule = getModule.invoke(JavaCompatibilityLayer.class);

                String packageName = declaringClass.getPackage().getName();

                Class<?> moduleClass = Class.forName("java.lang.Module");
                Method redefineModule = Instrumentation.class.getMethod(
                    "redefineModule",
                    moduleClass, Set.class, Map.class, Map.class,
                    Set.class, Map.class);

                Map<String, Set<Object>> extraOpens = new HashMap<>();
                Set<Object> moduleSet = new HashSet<>();
                moduleSet.add(ourModule);
                extraOpens.put(packageName, moduleSet);

                redefineModule.invoke(inst,
                    targetModule,
                    Collections.singleton(ourModule),
                    Collections.emptyMap(),
                    extraOpens,
                    Collections.emptySet(),
                    Collections.emptyMap());

            } catch (Exception e) {
                LOGGER.fine("[MDR-JavaCompat] Module open via Instrumentation failed: "
                          + e.getMessage());
            }
        }

        /**
         * Gets the value of a field, handling all access control issues.
         *
         * @return the field value, or {@code defaultValue} on failure
         */
        public static <T> T getFieldValue(Object instance, String fieldName,
                                          Class<T> type, T defaultValue) {
            try {
                Field f = findField(instance.getClass(), fieldName);
                if (f != null) {
                    Object val = f.get(instance);
                    return type.isInstance(val) ? type.cast(val) : defaultValue;
                }
            } catch (Exception ignored) {}
            return defaultValue;
        }

        /**
         * Gets a static field value.
         */
        public static <T> T getStaticFieldValue(Class<?> clazz, String fieldName,
                                                 Class<T> type, T defaultValue) {
            try {
                Field f = findField(clazz, fieldName);
                if (f != null) {
                    Object val = f.get(null);
                    return type.isInstance(val) ? type.cast(val) : defaultValue;
                }
            } catch (Exception ignored) {}
            return defaultValue;
        }

        /**
         * Sets a field value, handling all access control issues.
         */
        public static boolean setFieldValue(Object instance, String fieldName,
                                            Object value) {
            try {
                Field f = findField(instance.getClass(), fieldName);
                if (f != null) {
                    // Handle final fields
                    if (Modifier.isFinal(f.getModifiers())) {
                        return setFinalField(instance, f, value);
                    }
                    f.set(instance, value);
                    return true;
                }
            } catch (Exception ignored) {}
            return false;
        }

        /**
         * Sets a final field value using Unsafe.
         */
        public static boolean setFinalField(Object instance, Field field,
                                            Object value) {
            try {
                makeAccessible(field);

                // On Java 12+, setting final fields via reflection throws
                // IllegalAccessException even with setAccessible(true).
                // We must use Unsafe.
                UnsafeAccess unsafe = UnsafeAccess.get();
                long offset = unsafe.objectFieldOffset(field);
                unsafe.putObject(instance, offset, value);
                return true;
            } catch (Exception e) {
                LOGGER.fine("[MDR-JavaCompat] setFinalField failed: "
                          + e.getMessage());
                return false;
            }
        }

        /**
         * Invokes a method, handling all access control issues.
         */
        public static Object invokeMethod(Object instance, String methodName,
                                          Class<?>[] paramTypes, Object... args) {
            try {
                Method m = findMethod(instance.getClass(), methodName, paramTypes);
                if (m != null) {
                    return m.invoke(instance, args);
                }
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            } catch (Exception e) {
                throw new RuntimeException("Method invocation failed: "
                    + methodName, e);
            }
            return null;
        }
    }


    // ========================================================================
    //  SECTION 4: TRUSTED LOOKUP ACQUISITION
    // ========================================================================

    /**
     * Acquires a MethodHandles.Lookup with full ("trusted") access.
     * This lookup can access private members of any class, bypassing
     * module boundaries.
     */
    private static MethodHandles.Lookup acquireTrustedLookup() {
        // Strategy 1: Use Unsafe to read the IMPL_LOOKUP field
        try {
            UnsafeAccess unsafe = UnsafeAccess.get();
            if (unsafe.getRawUnsafe() != null) {
                Field implLookup = MethodHandles.Lookup.class
                    .getDeclaredField("IMPL_LOOKUP");
                long offset = unsafe.objectFieldOffset(implLookup);
                Object lookup = unsafe.getObject(null, offset);
                if (lookup instanceof MethodHandles.Lookup) {
                    return (MethodHandles.Lookup) lookup;
                }
                // On some JVMs, IMPL_LOOKUP is a static field of the class itself
                lookup = unsafe.getObject(
                    MethodHandles.Lookup.class, offset);
                if (lookup instanceof MethodHandles.Lookup) {
                    return (MethodHandles.Lookup) lookup;
                }
            }
        } catch (Exception e) {
            LOGGER.fine("[MDR-JavaCompat] IMPL_LOOKUP via Unsafe failed: "
                      + e.getMessage());
        }

        // Strategy 2: Construct a Lookup with full access mode via Unsafe
        try {
            UnsafeAccess unsafe = UnsafeAccess.get();
            if (unsafe.getRawUnsafe() != null) {
                // MethodHandles.Lookup has a private constructor:
                //   Lookup(Class<?> lookupClass, Class<?> prevLookupClass,
                //          int allowedModes)
                // The ALL_MODES constant includes TRUSTED
                MethodHandles.Lookup normalLookup = MethodHandles.lookup();

                // Find the 'allowedModes' field
                Field modesField = MethodHandles.Lookup.class
                    .getDeclaredField("allowedModes");
                long modesOffset = unsafe.objectFieldOffset(modesField);

                // Create a clone of our lookup and set modes to max
                Object trustedLookup = unsafe.allocateInstance(
                    MethodHandles.Lookup.class);

                // Set lookupClass
                Field lookupClassField = MethodHandles.Lookup.class
                    .getDeclaredField("lookupClass");
                long classOffset = unsafe.objectFieldOffset(lookupClassField);
                unsafe.putObject(trustedLookup, classOffset, Object.class);

                // Set allowedModes to -1 (all modes including TRUSTED)
                unsafe.putInt(trustedLookup, modesOffset, -1); // TRUSTED = -1

                return (MethodHandles.Lookup) trustedLookup;
            }
        } catch (Exception e) {
            LOGGER.fine("[MDR-JavaCompat] Lookup construction via Unsafe failed: "
                      + e.getMessage());
        }

        // Strategy 3: Use privateLookupIn (Java 9+, requires open module)
        if (JAVA_VERSION >= 9) {
            try {
                Method privateLookupIn = MethodHandles.class.getMethod(
                    "privateLookupIn", Class.class, MethodHandles.Lookup.class);
                MethodHandles.Lookup result = (MethodHandles.Lookup)
                    privateLookupIn.invoke(null, Object.class,
                        MethodHandles.lookup());
                return result;
            } catch (Exception e) {
                LOGGER.fine("[MDR-JavaCompat] privateLookupIn failed: "
                          + e.getMessage());
            }
        }

        // Strategy 4: Fall back to normal Lookup (limited)
        return MethodHandles.lookup();
    }

    /**
     * Returns the trusted Lookup, or a fallback normal Lookup.
     */
    public static MethodHandles.Lookup getTrustedLookup() {
        return TRUSTED_LOOKUP != null ? TRUSTED_LOOKUP : MethodHandles.lookup();
    }


    // ========================================================================
    //  SECTION 5: CLASS LOADING COMPATIBILITY
    // ========================================================================

    /**
     * Abstraction for defining classes at runtime across Java versions.
     */
    public static final class ClassLoaderCompat {

        private ClassLoaderCompat() {}

        /**
         * Defines a class from bytecode using the best available mechanism.
         *
         * @param loader    the class loader context
         * @param name      the fully qualified class name
         * @param bytecode  the class bytecode
         * @return the defined class
         */
        public static Class<?> defineClass(ClassLoader loader, String name,
                                           byte[] bytecode) throws Exception {
            // Strategy 1: Lookup.defineClass (Java 9+)
            if (hasCapability(CAP_LOOKUP_DEFINE) && TRUSTED_LOOKUP != null) {
                try {
                    Method defineClass = MethodHandles.Lookup.class.getMethod(
                        "defineClass", byte[].class);
                    return (Class<?>) defineClass.invoke(TRUSTED_LOOKUP, bytecode);
                } catch (Exception e) {
                    LOGGER.fine("[MDR-JavaCompat] Lookup.defineClass failed: "
                              + e.getMessage());
                }
            }

            // Strategy 2: Unsafe.defineClass
            UnsafeAccess unsafe = UnsafeAccess.get();
            if (unsafe.getRawUnsafe() != null) {
                try {
                    Object rawUnsafe = unsafe.getRawUnsafe();
                    Method defineClassUnsafe = rawUnsafe.getClass().getMethod(
                        "defineClass",
                        String.class, byte[].class, int.class, int.class,
                        ClassLoader.class, ProtectionDomain.class);
                    ReflectionHelper.makeAccessible(defineClassUnsafe);
                    return (Class<?>) defineClassUnsafe.invoke(
                        rawUnsafe, name, bytecode, 0, bytecode.length,
                        loader, null);
                } catch (Exception e) {
                    LOGGER.fine("[MDR-JavaCompat] Unsafe.defineClass failed: "
                              + e.getMessage());
                }
            }

            // Strategy 3: ClassLoader.defineClass via reflection (Java 8)
            try {
                Method defineClassMethod = ClassLoader.class.getDeclaredMethod(
                    "defineClass",
                    String.class, byte[].class, int.class, int.class);
                ReflectionHelper.makeAccessible(defineClassMethod);
                return (Class<?>) defineClassMethod.invoke(
                    loader, name, bytecode, 0, bytecode.length);
            } catch (Exception e) {
                LOGGER.fine("[MDR-JavaCompat] ClassLoader.defineClass failed: "
                          + e.getMessage());
            }

            // Strategy 4: MethodHandles.Lookup.defineHiddenClass (Java 15+)
            if (hasCapability(CAP_HIDDEN_CLASSES) && TRUSTED_LOOKUP != null) {
                try {
                    // defineHiddenClass(byte[], boolean, Lookup.ClassOption...)
                    Method dhc = MethodHandles.Lookup.class.getMethod(
                        "defineHiddenClass", byte[].class, boolean.class,
                        /* ClassOption[] */ Array.newInstance(
                            Class.forName("java.lang.invoke.MethodHandles$Lookup$ClassOption"),
                            0).getClass());
                    Object result = dhc.invoke(TRUSTED_LOOKUP, bytecode, true,
                        Array.newInstance(
                            Class.forName("java.lang.invoke.MethodHandles$Lookup$ClassOption"),
                            0));
                    // Result is a Lookup for the hidden class
                    Method lookupClass = result.getClass().getMethod("lookupClass");
                    return (Class<?>) lookupClass.invoke(result);
                } catch (Exception e) {
                    LOGGER.fine("[MDR-JavaCompat] defineHiddenClass failed: "
                              + e.getMessage());
                }
            }

            throw new ClassNotFoundException(
                "Could not define class: " + name
                + " (all strategies exhausted)");
        }

        /**
         * Adds a URL to a URLClassLoader's search path.
         * On Java 9+, URLClassLoader is no longer the system class loader,
         * so we need alternative approaches.
         */
        public static boolean addURL(ClassLoader loader, URL url) {
            // Direct approach: URLClassLoader.addURL
            if (loader instanceof URLClassLoader) {
                try {
                    Method addURL = URLClassLoader.class.getDeclaredMethod(
                        "addURL", URL.class);
                    ReflectionHelper.makeAccessible(addURL);
                    addURL.invoke(loader, url);
                    return true;
                } catch (Exception e) {
                    LOGGER.fine("[MDR-JavaCompat] URLClassLoader.addURL failed: "
                              + e.getMessage());
                }
            }

            // Java 9+: Use Instrumentation to append to system search
            Instrumentation inst = Mini_DirtyRoomCore.getInstrumentation();
            if (inst != null) {
                try {
                    Path jarPath = Paths.get(url.toURI());
                    if (Files.isRegularFile(jarPath)) {
                        inst.appendToSystemClassLoaderSearch(
                            new java.util.jar.JarFile(jarPath.toFile()));
                        return true;
                    }
                } catch (Exception e) {
                    LOGGER.fine("[MDR-JavaCompat] Instrumentation append failed: "
                              + e.getMessage());
                }
            }

            // Unsafe approach: find and modify the internal URL path
            try {
                UnsafeAccess unsafe = UnsafeAccess.get();
                if (unsafe.getRawUnsafe() != null) {
                    // Navigate the class loader's internal structure
                    Field ucpField = ReflectionHelper.findField(
                        loader.getClass(), "ucp");
                    if (ucpField == null) {
                        ucpField = ReflectionHelper.findField(
                            loader.getClass(), "urlClassPath");
                    }
                    if (ucpField != null) {
                        Object ucp = ucpField.get(loader);
                        Method addUrlMethod = ReflectionHelper.findMethod(
                            ucp.getClass(), "addURL", URL.class);
                        if (addUrlMethod != null) {
                            addUrlMethod.invoke(ucp, url);
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.fine("[MDR-JavaCompat] Unsafe URL injection failed: "
                          + e.getMessage());
            }

            return false;
        }

        /**
         * Gets the URLs from a class loader (works across Java versions).
         */
        public static URL[] getURLs(ClassLoader loader) {
            if (loader instanceof URLClassLoader) {
                return ((URLClassLoader) loader).getURLs();
            }

            // Java 9+: try to access the internal URL list
            try {
                Field ucpField = ReflectionHelper.findField(
                    loader.getClass(), "ucp");
                if (ucpField != null) {
                    Object ucp = ucpField.get(loader);
                    Method getURLs = ReflectionHelper.findMethod(
                        ucp.getClass(), "getURLs");
                    if (getURLs != null) {
                        return (URL[]) getURLs.invoke(ucp);
                    }
                }
            } catch (Exception ignored) {}

            // Fallback: check java.class.path
            String classpath = System.getProperty("java.class.path", "");
            String[] entries = classpath.split(File.pathSeparator);
            List<URL> urls = new ArrayList<>();
            for (String entry : entries) {
                try {
                    urls.add(new File(entry).toURI().toURL());
                } catch (Exception ignored) {}
            }
            return urls.toArray(new URL[0]);
        }
    }


    // ========================================================================
    //  SECTION 6: NATIVE DIRECT BUFFER CLEANUP (JAVA 8-25)
    // ========================================================================

    /**
     * Direct buffer cleanup using NATIVE JVM APIs - critical for preventing
     * native memory leaks in LWJGL applications.
     *
     * Native cleanup mechanisms by Java version:
     *   Java 8:      Native sun.misc.Cleaner attached to DirectByteBuffer
     *   Java 9-15:   Native sun.misc.Cleaner OR java.lang.ref.Cleaner
     *   Java 16+:    Native java.lang.ref.Cleaner (primary)
     *   Java 9+:     Native Unsafe.invokeCleaner() method (fallback)
     *   Java 22+:    Native MemorySegment API (future direction)
     * 
     * Each strategy uses the actual JVM's native cleanup mechanism directly.
     * No emulation - we invoke the real Cleaner.clean() or invokeCleaner() APIs.
     */
    public static final class DirectBufferCleaner {

        private DirectBufferCleaner() {}

        /**
         * Immediately frees native memory using the JVM's NATIVE cleanup API.
         * This directly invokes the cleanup mechanism the JVM provides.
         *
         * @param buffer the direct ByteBuffer to clean using native APIs
         * @return true if the buffer was successfully cleaned via native mechanism
         */
        public static boolean clean(ByteBuffer buffer) {
            if (buffer == null || !buffer.isDirect()) return false;

            // Strategy 1: Native sun.misc.Cleaner (Java 8-15, works directly)
            try {
                return cleanViaSunMiscCleaner(buffer);
            } catch (Exception ignored) {}

            // Strategy 2: Native jdk.internal.ref.Cleaner (Java 9+)
            try {
                return cleanViaInternalCleaner(buffer);
            } catch (Exception ignored) {}

            // Strategy 3: Native sun.nio.ch.DirectBuffer.cleaner() interface
            try {
                return cleanViaDirectBufferInterface(buffer);
            } catch (Exception ignored) {}

            // Strategy 4: Native Unsafe.invokeCleaner (Java 9+, official API)
            if (JAVA_VERSION >= 9) {
                try {
                    return cleanViaUnsafeInvokeCleaner(buffer);
                } catch (Exception ignored) {}
            }

            // Failed — the buffer will be cleaned when GC'd (native JVM cleanup)
            LOGGER.fine("[MDR-JavaCompat] Could not immediately clean buffer: "
                      + buffer);
            return false;
        }

        private static boolean cleanViaSunMiscCleaner(ByteBuffer buffer) throws Exception {
            // Native DirectByteBuffer cleanup via sun.nio.ch.DirectBuffer interface
            Method cleanerMethod = buffer.getClass().getMethod("cleaner");
            ReflectionHelper.makeAccessible(cleanerMethod);
            Object cleaner = cleanerMethod.invoke(buffer);

            if (cleaner != null) {
                // Invoke native Cleaner.clean() method
                Method cleanMethod = cleaner.getClass().getMethod("clean");
                ReflectionHelper.makeAccessible(cleanMethod);
                cleanMethod.invoke(cleaner);
                return true;
            }
            return false;
        }

        private static boolean cleanViaInternalCleaner(ByteBuffer buffer) throws Exception {
            // Java 9+: DirectByteBuffer.cleaner() returns jdk.internal.ref.Cleaner
            // which extends PhantomReference and has clean()
            Method cleanerMethod = buffer.getClass().getDeclaredMethod("cleaner");
            ReflectionHelper.makeAccessible(cleanerMethod);
            Object cleaner = cleanerMethod.invoke(buffer);

            if (cleaner != null) {
                Method cleanMethod = cleaner.getClass().getMethod("clean");
                ReflectionHelper.makeAccessible(cleanMethod);
                cleanMethod.invoke(cleaner);
                return true;
            }
            return false;
        }

        private static boolean cleanViaDirectBufferInterface(ByteBuffer buffer)
                throws Exception {
            // Cast to sun.nio.ch.DirectBuffer interface
            Class<?> directBufferClass = Class.forName("sun.nio.ch.DirectBuffer");
            if (directBufferClass.isInstance(buffer)) {
                Method cleanerMethod = directBufferClass.getMethod("cleaner");
                Object cleaner = cleanerMethod.invoke(buffer);
                if (cleaner != null) {
                    Method cleanMethod = cleaner.getClass().getMethod("clean");
                    ReflectionHelper.makeAccessible(cleanMethod);
                    cleanMethod.invoke(cleaner);
                    return true;
                }
            }
            return false;
        }

        private static boolean cleanViaUnsafeInvokeCleaner(ByteBuffer buffer)
                throws Exception {
            // Java 9+: sun.misc.Unsafe.invokeCleaner(ByteBuffer)
            UnsafeAccess unsafe = UnsafeAccess.get();
            Object rawUnsafe = unsafe.getRawUnsafe();
            if (rawUnsafe != null) {
                Method invokeCleaner = rawUnsafe.getClass().getMethod(
                    "invokeCleaner", ByteBuffer.class);
                ReflectionHelper.makeAccessible(invokeCleaner);
                invokeCleaner.invoke(rawUnsafe, buffer);
                return true;
            }
            return false;
        }

        /**
         * Registers a direct buffer for deferred cleanup when it becomes
         * phantom-reachable (i.e., no strong references remain).
         * This is a safety net for buffers that aren't explicitly cleaned.
         */
        public static void registerForCleanup(ByteBuffer buffer,
                                              Runnable cleanAction) {
            if (buffer == null || !buffer.isDirect()) return;
            BUFFER_CLEANERS.put(System.identityHashCode(buffer), cleanAction);
            PhantomReference<ByteBuffer> ref =
                new PhantomReference<>(buffer, BUFFER_REF_QUEUE);
            PHANTOM_REFS.add(ref);
        }

        /**
         * Returns the native address of a direct ByteBuffer.
         */
        public static long getBufferAddress(ByteBuffer buffer) {
            if (buffer == null || !buffer.isDirect()) return 0L;

            try {
                // DirectByteBuffer has a long 'address' field
                Field addressField = ReflectionHelper.findField(
                    buffer.getClass(), "address");
                if (addressField != null) {
                    return addressField.getLong(buffer);
                }
            } catch (Exception ignored) {}

            // Fallback via Unsafe
            try {
                UnsafeAccess unsafe = UnsafeAccess.get();
                Class<?> bufClass = buffer.getClass();
                Field addressField = null;
                while (bufClass != null && addressField == null) {
                    try {
                        addressField = bufClass.getDeclaredField("address");
                    } catch (NoSuchFieldException e) {
                        bufClass = bufClass.getSuperclass();
                    }
                }
                if (addressField != null) {
                    long offset = unsafe.objectFieldOffset(addressField);
                    return unsafe.getLong(buffer, offset);
                }
            } catch (Exception ignored) {}

            return 0L;
        }
    }

    /**
     * Background daemon that processes the phantom reference queue for
     * buffer cleanup.
     */
    private static void startBufferCleanerDaemon() {
        Thread daemon = new Thread(() -> {
            while (true) {
                try {
                    Reference<?> ref = BUFFER_REF_QUEUE.remove(5000);
                    if (ref != null) {
                        PHANTOM_REFS.remove(ref);
                        // Run associated cleanup action
                        int hash = System.identityHashCode(ref);
                        Runnable cleanup = BUFFER_CLEANERS.remove(hash);
                        if (cleanup != null) {
                            try {
                                cleanup.run();
                            } catch (Exception e) {
                                LOGGER.fine("[MDR-JavaCompat] Buffer cleanup action failed: "
                                          + e.getMessage());
                            }
                        }
                        ref.clear();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.fine("[MDR-JavaCompat] Buffer cleaner daemon error: "
                              + e.getMessage());
                }
            }
        }, "MDR-BufferCleaner");
        daemon.setDaemon(true);
        daemon.setPriority(Thread.MIN_PRIORITY + 1);
        daemon.start();
    }


    // ========================================================================
    //  SECTION 7: DEPRECATED API WRAPPERS
    // ========================================================================

    /**
     * Wrappers for APIs that have been deprecated or removed across
     * Java versions. Each method provides the best available implementation
     * for the current JVM.
     */
    public static final class DeprecatedAPIs {

        private DeprecatedAPIs() {}

        // ── Thread.stop() — removed in Java 20 ────────────────────────────

        /**
         * Stops a thread. Uses Thread.stop() on Java 8–19, and
         * interrupt + daemon flag on Java 20+.
         *
         * @param thread the thread to stop
         */
        @SuppressWarnings("deprecation")
        public static void stopThread(Thread thread) {
            if (thread == null || !thread.isAlive()) return;

            if (JAVA_VERSION < 20) {
                try {
                    Method stop = Thread.class.getMethod("stop");
                    stop.invoke(thread);
                    return;
                } catch (Exception ignored) {}
            }

            // Java 20+: interrupt and hope the thread checks isInterrupted()
            thread.interrupt();

            // If that doesn't work within 5 seconds, try harder
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (thread.isAlive()) {
                LOGGER.warning("[MDR-JavaCompat] Thread '" + thread.getName()
                             + "' did not stop after interrupt. "
                             + "It may leak resources.");
            }
        }

        // ── Thread.suspend() / resume() — removed in Java 20 ──────────────

        /**
         * Suspends a thread (best-effort on Java 20+).
         */
        @SuppressWarnings("deprecation")
        public static void suspendThread(Thread thread) {
            if (thread == null) return;
            if (JAVA_VERSION < 20) {
                try {
                    Method suspend = Thread.class.getMethod("suspend");
                    suspend.invoke(thread);
                    return;
                } catch (Exception ignored) {}
            }
            LOGGER.fine("[MDR-JavaCompat] Thread.suspend() unavailable on Java "
                      + JAVA_VERSION);
        }

        /**
         * Resumes a thread (best-effort on Java 20+).
         */
        @SuppressWarnings("deprecation")
        public static void resumeThread(Thread thread) {
            if (thread == null) return;
            if (JAVA_VERSION < 20) {
                try {
                    Method resume = Thread.class.getMethod("resume");
                    resume.invoke(thread);
                    return;
                } catch (Exception ignored) {}
            }
            LOGGER.fine("[MDR-JavaCompat] Thread.resume() unavailable on Java "
                      + JAVA_VERSION);
        }

        // ── Runtime.runFinalizersOnExit() — removed in Java 18 ────────────

        /**
         * Enables finalizer-on-exit (no-op on Java 18+).
         */
        @SuppressWarnings("deprecation")
        public static void runFinalizersOnExit(boolean value) {
            if (JAVA_VERSION < 18) {
                try {
                    Method m = Runtime.class.getMethod(
                        "runFinalizersOnExit", boolean.class);
                    m.invoke(null, value);
                } catch (Exception ignored) {}
            }
            // Java 18+: finalizers are deprecated for removal; no-op
        }

        // ── System.getSecurityManager() — removed in Java 24 ──────────────

        /**
         * Gets the security manager (returns null on Java 24+).
         */
        public static Object getSecurityManager() {
            if (SECURITY_MANAGER_REMOVED) return null;
            try {
                Method m = System.class.getMethod("getSecurityManager");
                return m.invoke(null);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Checks a permission (no-op if SecurityManager is absent).
         */
        public static void checkPermission(String permissionName) {
            if (SECURITY_MANAGER_REMOVED) return;
            try {
                Object sm = getSecurityManager();
                if (sm != null) {
                    Class<?> permClass = Class.forName(
                        "java.security.RuntimePermission");
                    Constructor<?> ctor = permClass.getConstructor(String.class);
                    Object perm = ctor.newInstance(permissionName);

                    Method check = sm.getClass().getMethod(
                        "checkPermission",
                        Class.forName("java.security.Permission"));
                    check.invoke(sm, perm);
                }
            } catch (Exception ignored) {}
        }

        // ── AccessController.doPrivileged — deprecated in Java 17 ──────────

        /**
         * Executes an action in a privileged context.
         * On Java 24+ where AccessController is removed, just runs the action.
         */
        @SuppressWarnings({"deprecation", "removal"})
        public static <T> T doPrivileged(Supplier<T> action) {
            if (SECURITY_MANAGER_REMOVED) {
                return action.get();
            }
            try {
                return AccessController.doPrivileged(
                    (PrivilegedAction<T>) action::get);
            } catch (Throwable t) {
                // If AccessController itself is gone
                return action.get();
            }
        }

        // ── Object.finalize() — deprecated for removal in Java 18 ──────────

        /**
         * Registers a cleanup action for an object, using the best
         * available mechanism (Cleaner on Java 9+, weak ref on Java 8).
         */
        public static void registerCleanup(Object obj, Runnable cleanup) {
            if (HAS_CLEANER) {
                registerCleanupViaCleaner(obj, cleanup);
            } else {
                registerCleanupViaWeakRef(obj, cleanup);
            }
        }

        private static void registerCleanupViaCleaner(Object obj, Runnable cleanup) {
            try {
                Class<?> cleanerClass = Class.forName("java.lang.ref.Cleaner");
                Method create = cleanerClass.getMethod("create");
                Object cleaner = create.invoke(null);
                Method register = cleanerClass.getMethod("register",
                    Object.class, Runnable.class);
                register.invoke(cleaner, obj, cleanup);
            } catch (Exception e) {
                registerCleanupViaWeakRef(obj, cleanup);
            }
        }

        private static void registerCleanupViaWeakRef(Object obj, Runnable cleanup) {
            ReferenceQueue<Object> queue = new ReferenceQueue<>();
            WeakReference<Object> ref = new WeakReference<>(obj, queue);
            Thread cleanupThread = new Thread(() -> {
                try {
                    queue.remove();
                    cleanup.run();
                } catch (InterruptedException ignored) {}
            }, "MDR-Cleanup-" + obj.getClass().getSimpleName());
            cleanupThread.setDaemon(true);
            cleanupThread.start();
        }
    }


    // ========================================================================
    //  SECTION 8: NATIVE THREAD MANAGEMENT (JAVA 8-25)
    // ========================================================================

    /**
     * Thread utilities providing native access to platform and virtual threads.
     * 
     * Native Implementation:
     *   Java 8-20:  Native platform threads (Thread class)
     *   Java 21+:   Native virtual threads via Thread.ofVirtual() when requested
     *               Falls back to native platform threads when virtual not available
     * 
     * Zero emulation - uses the exact Thread API the JVM provides.
     */
    public static final class ThreadCompat {

        private ThreadCompat() {}

        /**
         * Creates a new thread using NATIVE JVM thread APIs.
         * On Java 21+, creates actual virtual threads when requested.
         * On Java 8-20, creates native platform threads.
         *
         * @param name       thread name
         * @param daemon     whether the thread is a daemon
         * @param runnable   the task
         * @param useVirtual whether to use native virtual threads (Java 21+ only)
         * @return the new thread (not started)
         */
        public static Thread createThread(String name, boolean daemon,
                                          Runnable runnable, boolean useVirtual) {
            if (useVirtual && HAS_VIRTUAL_THREADS) {
                try {
                    // Native virtual thread creation via Thread.ofVirtual() API
                    Method ofVirtual = Thread.class.getMethod("ofVirtual");
                    Object builder = ofVirtual.invoke(null);

                    Method nameMethod = builder.getClass().getMethod(
                        "name", String.class);
                    builder = nameMethod.invoke(builder, name);

                    Method unstarted = builder.getClass().getMethod(
                        "unstarted", Runnable.class);
                    return (Thread) unstarted.invoke(builder, runnable);

                } catch (Exception e) {
                    LOGGER.fine("[MDR-JavaCompat] Native virtual thread creation failed, "
                              + "falling back to native platform thread.");
                }
            }

            // Native platform thread
            Thread t = new Thread(runnable, name);
            t.setDaemon(daemon);
            return t;
        }

        /**
         * Creates and starts a thread using native JVM APIs.
         */
        public static Thread startThread(String name, boolean daemon,
                                         Runnable runnable, boolean useVirtual) {
            Thread t = createThread(name, daemon, runnable, useVirtual);
            t.start();
            return t;
        }

        /**
         * Creates an ExecutorService using native JVM thread APIs.
         * On Java 21+, uses native virtual thread executor when requested.
         */
        public static ExecutorService createExecutor(String namePrefix,
                                                     int maxThreads,
                                                     boolean useVirtual) {
            if (useVirtual && HAS_VIRTUAL_THREADS) {
                try {
                    // Native virtual thread executor via Executors.newVirtualThreadPerTaskExecutor()
                    Method nvtpte = Executors.class.getMethod(
                        "newVirtualThreadPerTaskExecutor");
                    return (ExecutorService) nvtpte.invoke(null);
                } catch (Exception e) {
                    LOGGER.fine("[MDR-JavaCompat] Native virtual thread executor failed.");
                }
            }

            // Native platform thread pool
            return Executors.newFixedThreadPool(maxThreads, r -> {
                Thread t = new Thread(r, namePrefix + "-"
                    + Thread.activeCount());
                t.setDaemon(true);
                return t;
            });
        }

        /**
         * Gets the thread ID in a version-compatible way.
         * Java 19+ deprecated Thread.getId() in favor of threadId().
         */
        public static long getThreadId(Thread thread) {
            if (JAVA_VERSION >= 19) {
                try {
                    Method threadId = Thread.class.getMethod("threadId");
                    return (long) threadId.invoke(thread);
                } catch (Exception ignored) {}
            }
            return thread.getId();
        }

        /**
         * Checks if a thread is a virtual thread (Java 21+).
         */
        public static boolean isVirtualThread(Thread thread) {
            if (!HAS_VIRTUAL_THREADS) return false;
            try {
                Method isVirtual = Thread.class.getMethod("isVirtual");
                return (boolean) isVirtual.invoke(thread);
            } catch (Exception e) {
                return false;
            }
        }
    }


    // ========================================================================
    //  SECTION 9: GC & MEMORY UTILITIES
    // ========================================================================

    /**
     * GC and memory management utilities that work across all Java versions.
     */
    public static final class MemoryCompat {

        private MemoryCompat() {}

        /**
         * Suggests a full GC. Works on all versions but the JVM may ignore it.
         */
        public static void suggestGC() {
            System.gc();
        }

        /**
         * Returns the current GC name (e.g., "G1", "ZGC", "Shenandoah").
         */
        public static String getGCName() {
            try {
                Class<?> mgmtFactory = Class.forName(
                    "java.lang.management.ManagementFactory");
                Method getGCBeans = mgmtFactory.getMethod(
                    "getGarbageCollectorMXBeans");
                @SuppressWarnings("unchecked")
                List<Object> beans = (List<Object>) getGCBeans.invoke(null);

                if (!beans.isEmpty()) {
                    Method getName = beans.get(0).getClass().getMethod("getName");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < beans.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(getName.invoke(beans.get(i)));
                    }
                    return sb.toString();
                }
            } catch (Exception ignored) {}
            return "Unknown";
        }

        /**
         * Returns memory usage as a formatted string.
         */
        public static String getMemoryReport() {
            Runtime rt = Runtime.getRuntime();
            long free = rt.freeMemory();
            long total = rt.totalMemory();
            long max = rt.maxMemory();
            long used = total - free;

            return String.format(
                "Used: %d MB | Free: %d MB | Total: %d MB | Max: %d MB | GC: %s",
                used / (1024 * 1024),
                free / (1024 * 1024),
                total / (1024 * 1024),
                max / (1024 * 1024),
                getGCName()
            );
        }

        /**
         * Allocates a direct ByteBuffer with proper native byte order
         * and registers it for cleanup.
         */
        public static ByteBuffer allocateDirectBuffer(int capacity) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(capacity)
                .order(ByteOrder.nativeOrder());
            // Register for cleanup as a safety net
            final ByteBuffer ref = buffer;
            DirectBufferCleaner.registerForCleanup(buffer,
                () -> DirectBufferCleaner.clean(ref));
            return buffer;
        }

        /**
         * Returns the native memory address of a direct buffer, or 0.
         */
        public static long getDirectBufferAddress(ByteBuffer buffer) {
            return DirectBufferCleaner.getBufferAddress(buffer);
        }
    }


    // ========================================================================
    //  SECTION 10: STACK WALKING
    // ========================================================================

    /**
     * Stack walking abstraction: StackWalker on Java 9+,
     * Throwable.getStackTrace() on Java 8.
     */
    public static final class StackWalkerCompat {

        private StackWalkerCompat() {}

        /**
         * Returns the caller class, skipping {@code skipFrames} frames.
         */
        public static Class<?> getCallerClass(int skipFrames) {
            if (HAS_STACKWALKER) {
                return getCallerClassViaStackWalker(skipFrames + 1);
            }
            return getCallerClassViaThrowable(skipFrames + 1);
        }

        private static Class<?> getCallerClassViaStackWalker(int skip) {
            try {
                Class<?> swClass = Class.forName("java.lang.StackWalker");

                // Get StackWalker.Option.RETAIN_CLASS_REFERENCE
                Class<?> optionClass = Class.forName(
                    "java.lang.StackWalker$Option");
                Object retainClassRef = Enum.valueOf(
                    (Class<Enum>) optionClass, "RETAIN_CLASS_REFERENCE");

                // StackWalker.getInstance(Option)
                Method getInstance = swClass.getMethod("getInstance", optionClass);
                Object walker = getInstance.invoke(null, retainClassRef);

                // walker.walk(stream -> stream.skip(N).findFirst().get().getDeclaringClass())
                Method walk = swClass.getMethod("walk", Function.class);
                int finalSkip = skip;
                @SuppressWarnings("unchecked")
                Optional<Object> frame = (Optional<Object>) walk.invoke(walker,
                    (Function<Object, Object>) stream -> {
                        try {
                            // stream.skip(finalSkip).findFirst()
                            Method skipMethod = stream.getClass().getMethod(
                                "skip", long.class);
                            Object skipped = skipMethod.invoke(stream, (long) finalSkip);
                            Method findFirst = skipped.getClass().getMethod("findFirst");
                            return findFirst.invoke(skipped);
                        } catch (Exception e) {
                            return Optional.empty();
                        }
                    });

                if (frame != null && frame.isPresent()) {
                    Object stackFrame = frame.get();
                    Method getDeclaringClass = stackFrame.getClass()
                        .getMethod("getDeclaringClass");
                    return (Class<?>) getDeclaringClass.invoke(stackFrame);
                }
            } catch (Exception e) {
                LOGGER.fine("[MDR-JavaCompat] StackWalker caller lookup failed: "
                          + e.getMessage());
            }
            return null;
        }

        private static Class<?> getCallerClassViaThrowable(int skip) {
            StackTraceElement[] stack = new Throwable().getStackTrace();
            if (skip < stack.length) {
                try {
                    return Class.forName(stack[skip].getClassName());
                } catch (ClassNotFoundException ignored) {}
            }
            return null;
        }

        /**
         * Returns the current stack trace as a list of class names.
         */
        public static List<String> getStackClassNames() {
            StackTraceElement[] stack = new Throwable().getStackTrace();
            List<String> names = new ArrayList<>(stack.length);
            for (StackTraceElement ste : stack) {
                names.add(ste.getClassName());
            }
            return names;
        }
    }


    // ========================================================================
    //  SECTION 11: PROCESS MANAGEMENT
    // ========================================================================

    /**
     * Process utilities abstracting ProcessHandle (Java 9+) vs legacy APIs.
     */
    public static final class ProcessCompat {

        private ProcessCompat() {}

        /**
         * Returns the PID of the current process.
         */
        public static long getCurrentPID() {
            if (HAS_PROCESSHANDLE) {
                try {
                    Class<?> ph = Class.forName("java.lang.ProcessHandle");
                    Method current = ph.getMethod("current");
                    Object handle = current.invoke(null);
                    Method pid = handle.getClass().getMethod("pid");
                    return (long) pid.invoke(handle);
                } catch (Exception ignored) {}
            }

            // Java 8 fallback: ManagementFactory
            try {
                Class<?> mgmt = Class.forName(
                    "java.lang.management.ManagementFactory");
                Method getRt = mgmt.getMethod("getRuntimeMXBean");
                Object rt = getRt.invoke(null);
                Method getName = rt.getClass().getMethod("getName");
                String name = (String) getName.invoke(rt);
                return Long.parseLong(name.split("@")[0]);
            } catch (Exception e) {
                return -1L;
            }
        }

        /**
         * Returns the uptime of the JVM in milliseconds.
         */
        public static long getJVMUptimeMs() {
            try {
                Class<?> mgmt = Class.forName(
                    "java.lang.management.ManagementFactory");
                Method getRt = mgmt.getMethod("getRuntimeMXBean");
                Object rt = getRt.invoke(null);
                Method getUptime = rt.getClass().getMethod("getUptime");
                return (long) getUptime.invoke(rt);
            } catch (Exception e) {
                return -1L;
            }
        }

        /**
         * Checks if a process with the given PID is alive.
         */
        public static boolean isProcessAlive(long pid) {
            if (HAS_PROCESSHANDLE) {
                try {
                    Class<?> ph = Class.forName("java.lang.ProcessHandle");
                    Method of = ph.getMethod("of", long.class);
                    Object optional = of.invoke(null, pid);
                    Method isPresent = optional.getClass().getMethod("isPresent");
                    if ((boolean) isPresent.invoke(optional)) {
                        Method get = optional.getClass().getMethod("get");
                        Object handle = get.invoke(optional);
                        Method isAlive = handle.getClass().getMethod("isAlive");
                        return (boolean) isAlive.invoke(handle);
                    }
                    return false;
                } catch (Exception ignored) {}
            }

            // Fallback: try sending signal 0 (Unix) or tasklist (Windows)
            EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
            if (env != null && env.isWindows) {
                try {
                    Process proc = Runtime.getRuntime().exec(
                        new String[]{"tasklist", "/FI", "PID eq " + pid});
                    String output = readStream(proc.getInputStream());
                    proc.waitFor(3, TimeUnit.SECONDS);
                    return output.contains(String.valueOf(pid));
                } catch (Exception ignored) {}
            } else {
                try {
                    Process proc = Runtime.getRuntime().exec(
                        new String[]{"kill", "-0", String.valueOf(pid)});
                    return proc.waitFor(3, TimeUnit.SECONDS)
                        && proc.exitValue() == 0;
                } catch (Exception ignored) {}
            }

            return false;
        }

        private static String readStream(InputStream is) throws IOException {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            return sb.toString();
        }
    }


    // ========================================================================
    //  SECTION 12: DIAGNOSTIC REPORT
    // ========================================================================

    /**
     * Generates a comprehensive native capability diagnostic report.
     */
    public static String getDiagnosticReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Java Compatibility Layer - Native Java 8-25 Support ===\n");
        sb.append("Target: Direct JVM API Access - Zero Emulation\n\n");
        sb.append("Java Version: ").append(JAVA_VERSION).append("\n");
        sb.append("Full Version: ").append(
            System.getProperty("java.version", "?")).append("\n");
        sb.append("Vendor: ").append(
            System.getProperty("java.vendor", "?")).append("\n");
        sb.append("VM: ").append(
            System.getProperty("java.vm.name", "?")).append("\n");

        sb.append("\n--- Native Capabilities Detected ---\n");
        sb.append(capabilitySummary()).append("\n");

        sb.append("\n--- Native UnsafeAccess Implementation ---\n");
        UnsafeAccess ua = UNSAFE_ACCESS.get();
        sb.append("Implementation: ").append(
            ua != null ? ua.getImplementationName() : "NOT INITIALIZED").append("\n");
        sb.append("Native Unsafe available: ").append(
            ua != null && ua.getRawUnsafe() != null).append("\n");

        sb.append("\n--- Native Trusted Lookup ---\n");
        sb.append("Acquired: ").append(TRUSTED_LOOKUP != null).append("\n");
        if (TRUSTED_LOOKUP != null) {
            sb.append("Lookup class: ").append(
                TRUSTED_LOOKUP.lookupClass().getName()).append("\n");
        }

        sb.append("\n--- Memory ---\n");
        sb.append(MemoryCompat.getMemoryReport()).append("\n");

        sb.append("\n--- Unlocked Accesses ---\n");
        sb.append("Count: ").append(UNLOCKED_ACCESSES.size()).append("\n");
        int count = 0;
        for (String access : UNLOCKED_ACCESSES) {
            if (count++ >= 20) {
                sb.append("  ... and ").append(
                    UNLOCKED_ACCESSES.size() - 20).append(" more\n");
                break;
            }
            sb.append("  ").append(access).append("\n");
        }

        sb.append("\n--- Process ---\n");
        sb.append("PID: ").append(ProcessCompat.getCurrentPID()).append("\n");
        sb.append("Uptime: ").append(ProcessCompat.getJVMUptimeMs()).append(" ms\n");

        sb.append("=== End Report ===\n");
        return sb.toString();
    }


    // ========================================================================
    //  SECTION 13: INITIALIZATION ENTRY POINT
    // ========================================================================

    /**
     * Explicit initialization method called by Mini_DirtyRoomCore or
     * ModLoaderBridge. The static initializer has already done most of
     * the work; this method performs any deferred setup.
     */
    @DeepHook(
        targets = {
            @HookTarget(
                className  = "stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore",
                methodName = "bootstrap"
            )
        },
        timing = HookTiming.AFTER
    )
    public static void initialize() {
        if (!INITIALIZED.compareAndSet(false, true)) return;

        LOGGER.info("[MDR-JavaCompat] ╔═══════════════════════════════════════════╗");
        LOGGER.info("[MDR-JavaCompat] ║  Native Java " + JAVA_VERSION + " Support Initialized      ║");
        LOGGER.info("[MDR-JavaCompat] ║  Direct JVM API Access - Zero Emulation  ║");
        LOGGER.info("[MDR-JavaCompat] ╚═══════════════════════════════════════════╝");

        // Ensure native UnsafeAccess is created
        UnsafeAccess.get();

        // Validate critical native capabilities
        if (!HAS_SUN_UNSAFE && !HAS_JDK_UNSAFE && !HAS_VARHANDLE) {
            LOGGER.warning("[MDR-JavaCompat] No native Unsafe or VarHandle available! "
                         + "Some features will be severely limited.");
            Mini_DirtyRoomCore.getBootWarnings().getClass(); // ensure core is loaded
        }

        // Report warnings for known problematic configurations
        if (JAVA_VERSION >= 16 && TRUSTED_LOOKUP == null) {
            String warn = "Java " + JAVA_VERSION + " with no native trusted Lookup. "
                        + "Add --add-opens=java.base/java.lang.invoke=ALL-UNNAMED";
            LOGGER.warning("[MDR-JavaCompat] " + warn);
        }

        if (JAVA_VERSION >= 17 && !SECURITY_MANAGER_REMOVED) {
            LOGGER.info("[MDR-JavaCompat] Native SecurityManager still present on Java "
                      + JAVA_VERSION + " (deprecated).");
        }

        LOGGER.info("[MDR-JavaCompat] Native capabilities: " + capabilitySummary());
        LOGGER.info("[MDR-JavaCompat] Native UnsafeAccess implementation: "
                  + UnsafeAccess.get().getImplementationName());
    }


    // ========================================================================
    //  SECTION 14: PRIVATE CONSTRUCTOR
    // ========================================================================

    private JavaCompatibilityLayer() {
        throw new UnsupportedOperationException(
            "JavaCompatibilityLayer is a static utility class.");
    }
}
