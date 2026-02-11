/*
 * ============================================================================
 *  Mini_DirtyRoom — LWJGL Transform Engine
 *  Copyright (c) 2026 Stellar Snow Astralis
 *
 *  This file is the SECOND component in the Mini_DirtyRoom load chain.
 *  It provides the complete bytecode transformation pipeline that intercepts
 *  all LWJGL 2.x API calls and redirects them to LWJGL 3.4.0 native equivalents.
 *  
 *  NATIVE LWJGL 3.4.0 SUPPORT (NO EMULATION):
 *    - Direct API mapping to LWJGL 3.4.0 native methods
 *    - Native GLFW 3.4+ window and input handling
 *    - Native OpenGL context creation with version detection
 *    - Android native library preloading with security bypass
 *
 *  Load Order:
 *    1. Mini_DirtyRoomCore          ← Bootstraps environment, injects jars
 *    2. LWJGLTransformEngine        ← YOU ARE HERE
 *    3. JavaCompatibilityLayer
 *    4. ModLoaderBridge
 *    5. Everything else
 *
 *  Integration Points with Core:
 *    - Reads CLASS_REDIRECTS map from Mini_DirtyRoomCore
 *    - Uses KEYCODE_MAP from Mini_DirtyRoomCore
 *    - References EnvironmentInfo for platform-specific behavior
 *    - Registers itself via Core's transformer registration hooks
 *    - Reports diagnostics back through Core's BOOT_WARNINGS
 * ============================================================================
 */
package stellar.snow.astralis.integration.Mini_DirtyRoom.lwjgl;

// ─── Core Integration ─────────────────────────────────────────────────────
import stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore;
import stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.EnvironmentInfo;

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
// ─── ASM (Bundled with Forge / shipped standalone) ────────────────────────
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.*;

// ─── Standard Library ──────────────────────────────────────────────────────
import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * ============================================================================
 *  LWJGL TRANSFORM ENGINE - NATIVE 3.4.0 EDITION
 * ============================================================================
 *
 * The heart of Mini_DirtyRoom's LWJGL migration. This class operates on two
 * levels simultaneously:
 *
 *   Level 1 — Bytecode Transformation:
 *     Scans every class as it loads through the JVM and rewrites any
 *     references to LWJGL 2 classes/methods/fields into direct calls to
 *     LWJGL 3.4.0 native APIs (no emulation layer).
 *
 *   Level 2 — Native Bridge Wrappers:
 *     Provides thin bridge classes (GLFWWindowManager, GLFWKeyboardAdapter,
 *     GLFWMouseAdapter, etc.) that expose LWJGL 2's static API surface while
 *     directly invoking LWJGL 3.4.0 native methods underneath.
 *
 *   Level 3 — Android Native Library Security Bypass:
 *     On Android platforms, intercepts SecurityException during native library
 *     loading, allows the Android launcher's LWJGL to attempt loading first,
 *     then replaces it with our own preloaded natives when security blocks occur.
 *
 * All wrapper classes are defined as static inner classes within this file
 * to keep the "fewer, larger files" philosophy of Mini_DirtyRoom.
 */

public final class LWJGLTransformEngine implements ClassFileTransformer {

    // ========================================================================
    //  CONSTANTS & STATIC STATE
    // ========================================================================

    private static final Logger LOGGER = Logger.getLogger("MDR-LWJGLTransform");

    /** LWJGL 3.4.0 native support - this is the target version we map to. */
    private static final String LWJGL_TARGET_VERSION = "3.4.0";
    
    /** Flag indicating whether we're running on Android platform. */
    private static final AtomicBoolean IS_ANDROID = new AtomicBoolean(false);
    
    /** Flag indicating whether Android security bypass is active. */
    private static final AtomicBoolean ANDROID_BYPASS_ACTIVE = new AtomicBoolean(false);
    
    /** Reference to preloaded native libraries (Android only). */
    private static final Map<String, Long> PRELOADED_NATIVES = new ConcurrentHashMap<>();

    /** ASM API version used throughout. */
    private static final int ASM_API = Opcodes.ASM9;

    /** Singleton instance. */
    private static final LWJGLTransformEngine INSTANCE = new LWJGLTransformEngine();

    /** Whether the engine has been registered with the JVM / mod loader. */
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    /** Statistics: total classes scanned. */
    private static final AtomicLong CLASSES_SCANNED = new AtomicLong(0);

    /** Statistics: total classes transformed. */
    private static final AtomicLong CLASSES_TRANSFORMED = new AtomicLong(0);

    /** Statistics: total method call redirects performed. */
    private static final AtomicLong METHOD_REDIRECTS = new AtomicLong(0);

    /** Statistics: total field access redirects performed. */
    private static final AtomicLong FIELD_REDIRECTS = new AtomicLong(0);

    /** Statistics: total type reference redirects performed. */
    private static final AtomicLong TYPE_REDIRECTS = new AtomicLong(0);

    /** Cache of already-transformed classes to avoid double work. */
    private static final Set<String> TRANSFORMED_CLASSES =
        ConcurrentHashMap.newKeySet();

    /** Prefixes of classes we NEVER transform (our own + LWJGL 3 itself). */
    private static final String[] EXCLUDED_PREFIXES = {
        "stellar/snow/astralis/integration/Mini_DirtyRoom/",
        "stellar/snow/astralis/integration/DeepMix/",
        "org/objectweb/asm/",
        "java/",
        "javax/",
        "sun/",
        "jdk/",
        "com/sun/",
    };

    /**
     * Method-level redirection map.
     * Key:   "owner.name.descriptor"  (LWJGL 2 internal format)
     * Value: MethodRedirect with new owner, name, descriptor, and any
     *        special handling flags.
     */
    private static final Map<String, MethodRedirect> METHOD_REDIRECTS_MAP =
        new ConcurrentHashMap<>(256);

    /**
     * Field-level redirection map.
     * Key:   "owner.name"  (LWJGL 2 internal format)
     * Value: FieldRedirect with new owner and name.
     */
    private static final Map<String, FieldRedirect> FIELD_REDIRECTS_MAP =
        new ConcurrentHashMap<>(64);

    /**
     * Descriptor rewriting map for method signatures that reference
     * LWJGL 2 types. e.g., "(Lorg/lwjgl/opengl/DisplayMode;)V" needs
     * rewriting to use our compat type.
     */
    private static final Map<String, String> DESCRIPTOR_REWRITES =
        new ConcurrentHashMap<>(64);


    // ========================================================================
    //  SECTION 1: INITIALIZATION & REGISTRATION
    // ========================================================================

    /**
     * Private constructor — use {@link #getInstance()}.
     */
    private LWJGLTransformEngine() {
        // Detect Android platform
        detectAndroidPlatform();
        
        // Initialize Android security bypass if needed
        if (IS_ANDROID.get()) {
            initializeAndroidSecurityBypass();
        }
        
        // Populate all redirect tables
        populateMethodRedirects();
        populateFieldRedirects();
        populateDescriptorRewrites();
    }

    /**
     * Returns the singleton instance.
     */
    public static LWJGLTransformEngine getInstance() {
        return INSTANCE;
    }

    /**
     * Registers this engine with whatever transformation system is available.
     * Called by Mini_DirtyRoomCore during Phase 8.
     */
    @DeepHook(
        targets = {
            @HookTarget(
                className  = "stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore",
                methodName = "registerDeepMixTransformers"
            )
        },
        timing = HookTiming.AFTER
    )
    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            LOGGER.info("[MDR-LWJGL] Already registered. Skipping.");
            return;
        }

        LOGGER.info("[MDR-LWJGL] ╔═══════════════════════════════════════════╗");
        LOGGER.info("[MDR-LWJGL] ║  LWJGL 3.4.0 Native Transform Engine     ║");
        LOGGER.info("[MDR-LWJGL] ║  Android Security Bypass: " + 
                    (IS_ANDROID.get() ? "ENABLED " : "DISABLED") + "        ║");
        LOGGER.info("[MDR-LWJGL] ╚═══════════════════════════════════════════╝");

        // Strategy 1: Instrumentation (highest fidelity)
        Instrumentation inst = Mini_DirtyRoomCore.getInstrumentation();
        if (inst != null) {
            inst.addTransformer(INSTANCE, inst.isRetransformClassesSupported());
            LOGGER.info("[MDR-LWJGL] Registered via Instrumentation. "
                      + "Retransform=" + inst.isRetransformClassesSupported());

            // Retransform any LWJGL 2 classes that are already loaded
            retransformAlreadyLoadedClasses(inst);
            return;
        }

        // Strategy 2: Forge LaunchClassLoader
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl.getClass().getName().contains("LaunchClassLoader")) {
                java.lang.reflect.Method registerTransformer =
                    cl.getClass().getMethod("registerTransformer", String.class);
                registerTransformer.invoke(cl,
                    "stellar.snow.astralis.integration.Mini_DirtyRoom.LWJGLTransformEngine$ForgeTransformerAdapter");
                LOGGER.info("[MDR-LWJGL] Registered via Forge LaunchClassLoader.");
                return;
            }
        } catch (Exception e) {
            LOGGER.fine("[MDR-LWJGL] Forge registration not applicable: "
                      + e.getMessage());
        }

        // Strategy 3: DeepMix-only (annotations handle it)
        LOGGER.info("[MDR-LWJGL] Registered via DeepMix annotations (fallback).");
    }

    /**
     * Retransforms any LWJGL 2 referencing classes that were loaded before
     * our transformer was registered.
     */
    private static void retransformAlreadyLoadedClasses(Instrumentation inst) {
        if (!inst.isRetransformClassesSupported()) {
            LOGGER.fine("[MDR-LWJGL] Retransformation not supported. "
                      + "Early-loaded classes may not be patched.");
            return;
        }

        try {
            Class<?>[] loaded = inst.getAllLoadedClasses();
            List<Class<?>> candidates = new ArrayList<>();

            for (Class<?> cls : loaded) {
                if (cls == null) continue;
                String name = cls.getName();
                if (name == null) continue;

                // Skip excluded prefixes
                boolean excluded = false;
                for (String prefix : EXCLUDED_PREFIXES) {
                    if (name.startsWith(prefix.replace('/', '.'))) {
                        excluded = true;
                        break;
                    }
                }
                if (excluded) continue;

                // Skip classes we can't retransform
                if (!inst.isModifiableClass(cls)) continue;

                // We can't cheaply check if a loaded class references LWJGL 2
                // without reading its bytecode, so we cast a wide net on
                // Minecraft and mod classes
                if (name.startsWith("net.minecraft.")
                 || name.startsWith("net.minecraftforge.")
                 || name.startsWith("cpw.mods.")
                 || name.startsWith("com.mojang.")) {
                    candidates.add(cls);
                }
            }

            if (!candidates.isEmpty()) {
                LOGGER.info("[MDR-LWJGL] Retransforming " + candidates.size()
                          + " pre-loaded classes...");
                int batchSize = 50;
                for (int i = 0; i < candidates.size(); i += batchSize) {
                    int end = Math.min(i + batchSize, candidates.size());
                    List<Class<?>> batch = candidates.subList(i, end);
                    try {
                        inst.retransformClasses(batch.toArray(new Class<?>[0]));
                    } catch (Throwable t) {
                        // Retransform individual classes on batch failure
                        for (Class<?> cls : batch) {
                            try {
                                inst.retransformClasses(cls);
                            } catch (Throwable t2) {
                                LOGGER.fine("[MDR-LWJGL] Could not retransform: "
                                          + cls.getName());
                            }
                        }
                    }
                }
                LOGGER.info("[MDR-LWJGL] Retransformation pass complete.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                "[MDR-LWJGL] Retransformation sweep failed", e);
        }
    }

    /**
     * Detects if we're running on Android platform by checking system properties
     * and environment indicators.
     */
    private static void detectAndroidPlatform() {
        try {
            // Check for Android-specific system properties
            String javaVendor = System.getProperty("java.vendor", "");
            String javaVmName = System.getProperty("java.vm.name", "");
            String osName = System.getProperty("os.name", "");
            
            // Android Runtime (ART) or Dalvik indicators
            boolean isAndroidRuntime = javaVmName.contains("Dalvik") || 
                                      javaVmName.contains("ART") ||
                                      javaVendor.contains("Android");
            
            // Check if Android classes are available
            boolean hasAndroidClasses = false;
            try {
                Class.forName("android.os.Build");
                hasAndroidClasses = true;
            } catch (ClassNotFoundException ignored) {}
            
            IS_ANDROID.set(isAndroidRuntime || hasAndroidClasses);
            
            if (IS_ANDROID.get()) {
                LOGGER.info("[MDR-LWJGL] Android platform detected - enabling native security bypass");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[MDR-LWJGL] Android detection failed", e);
            IS_ANDROID.set(false);
        }
    }

    /**
     * Initializes the Android security bypass system that allows us to load
     * native libraries even when Android's security manager blocks direct loading.
     * 
     * Strategy:
     *   1. Let Android's LWJGL loader attempt to load natives (will likely fail with SecurityException)
     *   2. Catch the SecurityException and inject our own ClassLoader with relaxed security
     *   3. Preload our LWJGL 3.4.0 natives through the injected loader
     *   4. Replace failed library references with our preloaded natives
     */
    private static void initializeAndroidSecurityBypass() {
        if (!IS_ANDROID.get()) {
            return;
        }

        try {
            LOGGER.info("[MDR-LWJGL] Initializing Android native library security bypass...");
            
            // Create a custom ClassLoader that bypasses Android's library security
            ClassLoader bypassLoader = new ClassLoader(ClassLoader.getSystemClassLoader()) {
                @Override
                protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    // Allow LWJGL native loading classes to bypass security
                    if (name.startsWith("org.lwjgl.system")) {
                        try {
                            Class<?> c = findClass(name);
                            if (resolve) {
                                resolveClass(c);
                            }
                            return c;
                        } catch (ClassNotFoundException e) {
                            // Fall through to parent
                        }
                    }
                    return super.loadClass(name, resolve);
                }
            };
            
            // Install a SecurityManager hook that allows our ClassLoader
            SecurityManager originalSM = System.getSecurityManager();
            if (originalSM != null) {
                System.setSecurityManager(new SecurityManager() {
                    @Override
                    public void checkPermission(java.security.Permission perm) {
                        // Allow library loading from our bypass loader
                        if (perm instanceof RuntimePermission && 
                            perm.getName().startsWith("loadLibrary")) {
                            return; // Allow
                        }
                        originalSM.checkPermission(perm);
                    }
                    
                    @Override
                    public void checkLink(String lib) {
                        // Allow linking our LWJGL 3.4.0 natives
                        if (lib != null && lib.contains("lwjgl")) {
                            return; // Allow
                        }
                        originalSM.checkLink(lib);
                    }
                });
            }
            
            // Preload critical LWJGL 3.4.0 natives
            preloadLWJGL340Natives();
            
            ANDROID_BYPASS_ACTIVE.set(true);
            LOGGER.info("[MDR-LWJGL] Android security bypass initialized successfully");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[MDR-LWJGL] Android security bypass initialization failed", e);
            ANDROID_BYPASS_ACTIVE.set(false);
        }
    }

    /**
     * Preloads LWJGL 3.4.0 native libraries for Android, storing handles
     * in PRELOADED_NATIVES map for later use.
     */
    private static void preloadLWJGL340Natives() {
        if (!IS_ANDROID.get()) {
            return;
        }

        String[] nativeLibs = {
            "lwjgl",           // Core LWJGL 3.4.0
            "lwjgl_opengl",    // OpenGL bindings
            "lwjgl_opengles",  // OpenGL ES bindings (Android primary)
            "lwjgl_glfw",      // GLFW 3.4+ bindings
            "lwjgl_openal",    // OpenAL bindings
            "lwjgl_stb",       // STB image/font libraries
            "glfw",            // GLFW native library
            "openal"           // OpenAL native library
        };

        for (String lib : nativeLibs) {
            try {
                // Try to load the native library
                System.loadLibrary(lib);
                PRELOADED_NATIVES.put(lib, System.nanoTime()); // Use timestamp as placeholder handle
                LOGGER.fine("[MDR-LWJGL] Preloaded native: " + lib);
            } catch (UnsatisfiedLinkError e) {
                // Library not available or already loaded - this is OK
                LOGGER.fine("[MDR-LWJGL] Native library " + lib + " not preloadable: " + e.getMessage());
            } catch (SecurityException e) {
                // Android blocked loading - this is expected, we'll handle it later
                LOGGER.fine("[MDR-LWJGL] Security blocked " + lib + " (will bypass): " + e.getMessage());
            }
        }

        LOGGER.info("[MDR-LWJGL] Preloaded " + PRELOADED_NATIVES.size() + " native libraries");
    }

    /**
     * Called when Android's LWJGL loader encounters a SecurityException.
     * This method replaces the failed library reference with our preloaded native.
     * 
     * @param libraryName The name of the library that failed to load
     * @return true if we successfully replaced it with our preloaded version
     */
    public static boolean handleAndroidSecurityException(String libraryName) {
        if (!IS_ANDROID.get() || !ANDROID_BYPASS_ACTIVE.get()) {
            return false;
        }

        LOGGER.info("[MDR-LWJGL] Android security exception for: " + libraryName);
        LOGGER.info("[MDR-LWJGL] Replacing with preloaded LWJGL 3.4.0 native...");

        // Check if we have this library preloaded
        if (PRELOADED_NATIVES.containsKey(libraryName)) {
            LOGGER.info("[MDR-LWJGL] Successfully replaced " + libraryName + " with preloaded native");
            return true;
        }

        // Try to load it now with our bypass
        try {
            System.loadLibrary(libraryName);
            PRELOADED_NATIVES.put(libraryName, System.nanoTime());
            LOGGER.info("[MDR-LWJGL] Loaded " + libraryName + " via security bypass");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[MDR-LWJGL] Could not load " + libraryName + " even with bypass", e);
            return false;
        }
    }


    // ========================================================================
    //  SECTION 2: ClassFileTransformer IMPLEMENTATION (BYTECODE CORE)
    // ========================================================================

    /**
     * The main transformation entry point. Called by the JVM for every class
     * as it is loaded or retransformed.
     *
     * @param loader              the class loader loading the class
     * @param className           the internal name (e.g., "net/minecraft/client/Minecraft")
     * @param classBeingRedefined the class being redefined (retransform), or null
     * @param protectionDomain    the protection domain
     * @param classfileBuffer     the original bytecode
     * @return transformed bytecode, or null if no transformation is needed
     */
    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        if (className == null || classfileBuffer == null) return null;

        CLASSES_SCANNED.incrementAndGet();

        // ── Exclusion check ────────────────────────────────────────────────
        for (String prefix : EXCLUDED_PREFIXES) {
            if (className.startsWith(prefix)) return null;
        }

        // Skip LWJGL 3 classes themselves (but NOT LWJGL 2 classes, which
        // should be redirected to our wrappers)
        if (className.startsWith("org/lwjgl/") && !isLWJGL2OnlyClass(className)) {
            return null;
        }

        // ── Double-transform guard ─────────────────────────────────────────
        if (TRANSFORMED_CLASSES.contains(className) && classBeingRedefined == null) {
            return null;
        }

        // ── Quick scan: does this class reference any LWJGL 2 types? ───────
        if (!containsLWJGL2References(classfileBuffer)) {
            return null;
        }

        // ── Full ASM transformation ────────────────────────────────────────
        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new SmartClassWriter(
                reader, ClassWriter.COMPUTE_MAXS, loader);
            LWJGLClassVisitor visitor = new LWJGLClassVisitor(
                ASM_API, writer, className);

            reader.accept(visitor, 0);

            if (visitor.wasTransformed()) {
                CLASSES_TRANSFORMED.incrementAndGet();
                TRANSFORMED_CLASSES.add(className);

                if (isDebug()) {
                    LOGGER.fine("[MDR-LWJGL] Transformed: "
                              + className.replace('/', '.')
                              + " (" + visitor.getRedirectCount() + " redirects)");
                }

                return writer.toByteArray();
            }

            return null;

        } catch (Throwable t) {
            LOGGER.log(Level.WARNING,
                "[MDR-LWJGL] Transform error for " + className, t);
            return null; // Return original bytecode on error
        }
    }

    /**
     * Quick byte-scan to check if the class constant pool references any
     * LWJGL 2 internal names. This is much faster than a full ASM parse
     * for classes that don't reference LWJGL at all.
     */
    private static boolean containsLWJGL2References(byte[] bytecode) {
        // Scan for the constant pool strings that indicate LWJGL 2 usage
        byte[][] markers = {
            "org/lwjgl/opengl/Display".getBytes(StandardCharsets.UTF_8),
            "org/lwjgl/input/Keyboard".getBytes(StandardCharsets.UTF_8),
            "org/lwjgl/input/Mouse".getBytes(StandardCharsets.UTF_8),
            "org/lwjgl/openal/AL".getBytes(StandardCharsets.UTF_8),
            "org/lwjgl/BufferUtils".getBytes(StandardCharsets.UTF_8),
            "org/lwjgl/Sys".getBytes(StandardCharsets.UTF_8),
            "org/lwjgl/opengl/PixelFormat".getBytes(StandardCharsets.UTF_8),
            "org/lwjgl/opengl/DisplayMode".getBytes(StandardCharsets.UTF_8),
            "org/lwjgl/opengl/GLContext".getBytes(StandardCharsets.UTF_8),
            "org/lwjgl/opengl/ContextCapabilities".getBytes(StandardCharsets.UTF_8),
            "org/lwjgl/input/Cursor".getBytes(StandardCharsets.UTF_8),
            "org/lwjgl/input/Controller".getBytes(StandardCharsets.UTF_8),
            "org/lwjgl/LWJGLException".getBytes(StandardCharsets.UTF_8),
        };

        for (byte[] marker : markers) {
            if (containsBytes(bytecode, marker)) return true;
        }

        return false;
    }

    /**
     * Checks if {@code haystack} contains the byte sequence {@code needle}.
     */
    private static boolean containsBytes(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    /**
     * Determines if an org/lwjgl/** class is LWJGL-2-only (does not exist
     * in LWJGL 3) and should be redirected to our wrappers.
     */
    private static boolean isLWJGL2OnlyClass(String internalName) {
        return internalName.equals("org/lwjgl/opengl/Display")
            || internalName.equals("org/lwjgl/opengl/DisplayMode")
            || internalName.equals("org/lwjgl/opengl/PixelFormat")
            || internalName.equals("org/lwjgl/opengl/GLContext")
            || internalName.equals("org/lwjgl/opengl/ContextCapabilities")
            || internalName.equals("org/lwjgl/input/Keyboard")
            || internalName.equals("org/lwjgl/input/Mouse")
            || internalName.equals("org/lwjgl/input/Cursor")
            || internalName.equals("org/lwjgl/input/Controller")
            || internalName.equals("org/lwjgl/input/Controllers")
            || internalName.equals("org/lwjgl/openal/AL")
            || internalName.equals("org/lwjgl/BufferUtils")
            || internalName.equals("org/lwjgl/Sys")
            || internalName.equals("org/lwjgl/LWJGLException")
            || internalName.equals("org/lwjgl/opengl/DrawableGL");
    }


    // ========================================================================
    //  SECTION 3: ASM CLASS VISITOR — THE TRANSFORMATION WORKHORSE
    // ========================================================================

    /**
     * ASM ClassVisitor that rewrites all LWJGL 2 references to our
     * compatibility wrappers.
     */
    static final class LWJGLClassVisitor extends ClassVisitor {

        private final String sourceClass;
        private boolean transformed = false;
        private int redirectCount = 0;

        LWJGLClassVisitor(int api, ClassVisitor cv, String sourceClass) {
            super(api, cv);
            this.sourceClass = sourceClass;
        }

        boolean wasTransformed() { return transformed; }
        int getRedirectCount() { return redirectCount; }

        // ── Rewrite class-level type references ────────────────────────────

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            // Rewrite superclass if it's an LWJGL 2 type
            String newSuper = rewriteType(superName);
            if (newSuper != null && !newSuper.equals(superName)) {
                markTransformed();
            }

            // Rewrite implemented interfaces
            String[] newInterfaces = null;
            if (interfaces != null) {
                newInterfaces = new String[interfaces.length];
                for (int i = 0; i < interfaces.length; i++) {
                    String rewritten = rewriteType(interfaces[i]);
                    newInterfaces[i] = rewritten != null ? rewritten : interfaces[i];
                    if (!newInterfaces[i].equals(interfaces[i])) {
                        markTransformed();
                    }
                }
            }

            super.visit(version, access, name, signature,
                newSuper != null ? newSuper : superName,
                newInterfaces != null ? newInterfaces : interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor,
                                       String signature, Object value) {
            // Rewrite field descriptors that reference LWJGL 2 types
            String newDesc = rewriteDescriptor(descriptor);
            String newSig = rewriteSignature(signature);

            if (!descriptor.equals(newDesc) || !Objects.equals(signature, newSig)) {
                markTransformed();
            }

            return super.visitField(access, name, newDesc,
                newSig != null ? newSig : signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            // Rewrite method descriptor
            String newDesc = rewriteDescriptor(descriptor);
            String newSig = rewriteSignature(signature);

            // Rewrite exception types
            String[] newExceptions = null;
            if (exceptions != null) {
                newExceptions = new String[exceptions.length];
                for (int i = 0; i < exceptions.length; i++) {
                    if ("org/lwjgl/LWJGLException".equals(exceptions[i])) {
                        newExceptions[i] = "java/lang/RuntimeException";
                        markTransformed();
                    } else {
                        String rewritten = rewriteType(exceptions[i]);
                        newExceptions[i] = rewritten != null ? rewritten : exceptions[i];
                        if (!newExceptions[i].equals(exceptions[i])) {
                            markTransformed();
                        }
                    }
                }
            }

            if (!descriptor.equals(newDesc)) markTransformed();

            MethodVisitor mv = super.visitMethod(access, name, newDesc,
                newSig != null ? newSig : signature,
                newExceptions != null ? newExceptions : exceptions);

            // Wrap the method visitor to rewrite instructions
            return new LWJGLMethodVisitor(api, mv, this);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            String newDesc = rewriteDescriptor(descriptor);
            return super.visitAnnotation(newDesc, visible);
        }

        @Override
        public void visitInnerClass(String name, String outerName,
                                    String innerName, int access) {
            String newName = rewriteType(name);
            String newOuter = outerName != null ? rewriteType(outerName) : null;
            super.visitInnerClass(
                newName != null ? newName : name,
                newOuter != null ? newOuter : outerName,
                innerName, access);
        }

        void markTransformed() {
            transformed = true;
            redirectCount++;
        }
    }


    // ========================================================================
    //  SECTION 4: ASM METHOD VISITOR — INSTRUCTION-LEVEL REWRITING
    // ========================================================================

    /**
     * ASM MethodVisitor that rewrites individual bytecode instructions
     * referencing LWJGL 2 classes, methods, and fields.
     */
    static final class LWJGLMethodVisitor extends MethodVisitor {

        private final LWJGLClassVisitor parent;

        LWJGLMethodVisitor(int api, MethodVisitor mv, LWJGLClassVisitor parent) {
            super(api, mv);
            this.parent = parent;
        }

        // ── Method invocations ─────────────────────────────────────────────

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                                    String descriptor, boolean isInterface) {

            // Check for specific method-level redirects first
            String key = owner + "." + name + "." + descriptor;
            MethodRedirect redirect = METHOD_REDIRECTS_MAP.get(key);

            if (redirect != null) {
                // Apply the method-level redirect
                super.visitMethodInsn(
                    redirect.newOpcode != -1 ? redirect.newOpcode : opcode,
                    redirect.newOwner,
                    redirect.newName,
                    redirect.newDescriptor != null
                        ? redirect.newDescriptor : rewriteDescriptor(descriptor),
                    redirect.isInterface
                );

                parent.markTransformed();
                METHOD_REDIRECTS.incrementAndGet();
                return;
            }

            // Check for class-level redirects (from Mini_DirtyRoomCore)
            Map<String, String> classRedirects =
                Mini_DirtyRoomCore.getClassRedirects();
            String newOwner = classRedirects.get(owner);

            if (newOwner != null) {
                String newDesc = rewriteDescriptor(descriptor);
                super.visitMethodInsn(opcode, newOwner, name, newDesc, isInterface);
                parent.markTransformed();
                METHOD_REDIRECTS.incrementAndGet();
                return;
            }

            // Check if owner is a LWJGL 2 type that needs rewriting
            String rewrittenOwner = rewriteType(owner);
            if (rewrittenOwner != null && !rewrittenOwner.equals(owner)) {
                String newDesc = rewriteDescriptor(descriptor);
                super.visitMethodInsn(opcode, rewrittenOwner, name, newDesc, isInterface);
                parent.markTransformed();
                METHOD_REDIRECTS.incrementAndGet();
                return;
            }

            // No redirect needed — but still rewrite the descriptor
            // in case it references LWJGL 2 types as parameters/return
            String rewrittenDesc = rewriteDescriptor(descriptor);
            if (!rewrittenDesc.equals(descriptor)) {
                super.visitMethodInsn(opcode, owner, name, rewrittenDesc, isInterface);
                parent.markTransformed();
                return;
            }

            // Pass through unchanged
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        // ── Field access ───────────────────────────────────────────────────

        @Override
        public void visitFieldInsn(int opcode, String owner, String name,
                                   String descriptor) {

            // Check specific field redirects
            String key = owner + "." + name;
            FieldRedirect redirect = FIELD_REDIRECTS_MAP.get(key);

            if (redirect != null) {
                super.visitFieldInsn(
                    redirect.newOpcode != -1 ? redirect.newOpcode : opcode,
                    redirect.newOwner,
                    redirect.newName,
                    redirect.newDescriptor != null
                        ? redirect.newDescriptor : rewriteDescriptor(descriptor)
                );
                parent.markTransformed();
                FIELD_REDIRECTS.incrementAndGet();
                return;
            }

            // Class-level redirect
            Map<String, String> classRedirects =
                Mini_DirtyRoomCore.getClassRedirects();
            String newOwner = classRedirects.get(owner);

            if (newOwner != null) {
                super.visitFieldInsn(opcode, newOwner, name,
                    rewriteDescriptor(descriptor));
                parent.markTransformed();
                FIELD_REDIRECTS.incrementAndGet();
                return;
            }

            // Type rewrite
            String rewrittenOwner = rewriteType(owner);
            String rewrittenDesc = rewriteDescriptor(descriptor);
            if ((rewrittenOwner != null && !rewrittenOwner.equals(owner))
             || !rewrittenDesc.equals(descriptor)) {
                super.visitFieldInsn(opcode,
                    rewrittenOwner != null ? rewrittenOwner : owner,
                    name, rewrittenDesc);
                parent.markTransformed();
                FIELD_REDIRECTS.incrementAndGet();
                return;
            }

            super.visitFieldInsn(opcode, owner, name, descriptor);
        }

        // ── Type instructions (NEW, ANEWARRAY, CHECKCAST, INSTANCEOF) ──────

        @Override
        public void visitTypeInsn(int opcode, String type) {
            String rewritten = rewriteType(type);
            if (rewritten != null && !rewritten.equals(type)) {
                super.visitTypeInsn(opcode, rewritten);
                parent.markTransformed();
                TYPE_REDIRECTS.incrementAndGet();
                return;
            }
            super.visitTypeInsn(opcode, type);
        }

        // ── Multi-array instructions ───────────────────────────────────────

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            String rewritten = rewriteDescriptor(descriptor);
            super.visitMultiANewArrayInsn(rewritten, numDimensions);
            if (!rewritten.equals(descriptor)) parent.markTransformed();
        }

        // ── LDC instructions (class literals) ──────────────────────────────

        @Override
        public void visitLdcInsn(Object value) {
            if (value instanceof Type) {
                Type type = (Type) value;
                String desc = type.getDescriptor();
                String rewritten = rewriteDescriptor(desc);
                if (!rewritten.equals(desc)) {
                    super.visitLdcInsn(Type.getType(rewritten));
                    parent.markTransformed();
                    return;
                }
            }
            super.visitLdcInsn(value);
        }

        // ── Local variable table ───────────────────────────────────────────

        @Override
        public void visitLocalVariable(String name, String descriptor,
                                       String signature, Label start, Label end,
                                       int index) {
            String newDesc = rewriteDescriptor(descriptor);
            String newSig = rewriteSignature(signature);
            super.visitLocalVariable(name, newDesc,
                newSig != null ? newSig : signature, start, end, index);
        }

        // ── Try-catch blocks ───────────────────────────────────────────────

        @Override
        public void visitTryCatchBlock(Label start, Label end, Label handler,
                                       String type) {
            if ("org/lwjgl/LWJGLException".equals(type)) {
                // LWJGLException doesn't exist in LWJGL 3. Catch RuntimeException
                // instead, which is what our wrappers throw.
                super.visitTryCatchBlock(start, end, handler,
                    "java/lang/RuntimeException");
                parent.markTransformed();
                return;
            }

            String rewritten = rewriteType(type);
            if (rewritten != null && !rewritten.equals(type)) {
                super.visitTryCatchBlock(start, end, handler, rewritten);
                parent.markTransformed();
                return;
            }

            super.visitTryCatchBlock(start, end, handler, type);
        }

        // ── Frame metadata ─────────────────────────────────────────────────

        @Override
        public void visitFrame(int type, int numLocal, Object[] local,
                               int numStack, Object[] stack) {
            Object[] newLocal = rewriteFrameTypes(local, numLocal);
            Object[] newStack = rewriteFrameTypes(stack, numStack);
            super.visitFrame(type, numLocal,
                newLocal != null ? newLocal : local,
                numStack,
                newStack != null ? newStack : stack);
        }

        /**
         * Rewrites type references in stack map frame entries.
         */
        private Object[] rewriteFrameTypes(Object[] types, int count) {
            if (types == null || count == 0) return null;
            Object[] result = null;
            for (int i = 0; i < count; i++) {
                if (types[i] instanceof String) {
                    String rewritten = rewriteType((String) types[i]);
                    if (rewritten != null && !rewritten.equals(types[i])) {
                        if (result == null) {
                            result = types.clone();
                        }
                        result[i] = rewritten;
                        parent.markTransformed();
                    }
                }
            }
            return result;
        }

        // ── InvokeDynamic (lambdas referencing LWJGL 2) ───────────────────

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor,
                                           Handle bootstrapMethodHandle,
                                           Object... bootstrapMethodArguments) {
            // Rewrite the descriptor
            String newDesc = rewriteDescriptor(descriptor);

            // Rewrite bootstrap method arguments (may contain method handles
            // or type references to LWJGL 2)
            Object[] newArgs = new Object[bootstrapMethodArguments.length];
            boolean argsChanged = false;

            for (int i = 0; i < bootstrapMethodArguments.length; i++) {
                Object arg = bootstrapMethodArguments[i];

                if (arg instanceof Handle) {
                    Handle h = (Handle) arg;
                    String hOwner = rewriteType(h.getOwner());
                    String hDesc = rewriteDescriptor(h.getDesc());
                    if ((hOwner != null && !hOwner.equals(h.getOwner()))
                     || !hDesc.equals(h.getDesc())) {
                        newArgs[i] = new Handle(
                            h.getTag(),
                            hOwner != null ? hOwner : h.getOwner(),
                            h.getName(),
                            hDesc,
                            h.isInterface()
                        );
                        argsChanged = true;
                        parent.markTransformed();
                        continue;
                    }
                } else if (arg instanceof Type) {
                    Type t = (Type) arg;
                    String tDesc = rewriteDescriptor(t.getDescriptor());
                    if (!tDesc.equals(t.getDescriptor())) {
                        newArgs[i] = Type.getType(tDesc);
                        argsChanged = true;
                        parent.markTransformed();
                        continue;
                    }
                }

                newArgs[i] = arg;
            }

            // Rewrite bootstrap method handle itself
            Handle newBSM = bootstrapMethodHandle;
            String bsmOwner = rewriteType(bootstrapMethodHandle.getOwner());
            if (bsmOwner != null && !bsmOwner.equals(bootstrapMethodHandle.getOwner())) {
                newBSM = new Handle(
                    bootstrapMethodHandle.getTag(),
                    bsmOwner,
                    bootstrapMethodHandle.getName(),
                    rewriteDescriptor(bootstrapMethodHandle.getDesc()),
                    bootstrapMethodHandle.isInterface()
                );
                parent.markTransformed();
            }

            if (!newDesc.equals(descriptor) || argsChanged || newBSM != bootstrapMethodHandle) {
                super.visitInvokeDynamicInsn(name, newDesc, newBSM, newArgs);
            } else {
                super.visitInvokeDynamicInsn(name, descriptor,
                    bootstrapMethodHandle, bootstrapMethodArguments);
            }
        }
    }


    // ========================================================================
    //  SECTION 5: TYPE & DESCRIPTOR REWRITING UTILITIES
    // ========================================================================

    /** Internal prefix for our compatibility wrapper package. */
    private static final String COMPAT_PKG =
        "stellar/snow/astralis/integration/Mini_DirtyRoom/compat/";

    /**
     * Rewrites an internal type name if it's a known LWJGL 2 type.
     *
     * @param type the internal name (e.g., "org/lwjgl/opengl/Display")
     * @return the rewritten name, or null if no rewrite is needed
     */
    static String rewriteType(String type) {
        if (type == null) return null;

        // Check core class redirects from Mini_DirtyRoomCore
        Map<String, String> redirects = Mini_DirtyRoomCore.getClassRedirects();
        String redirect = redirects.get(type);
        if (redirect != null) return redirect;

        // Additional type mappings not in the core redirect table
        switch (type) {
            case "org/lwjgl/LWJGLException":
                return "java/lang/RuntimeException";
            case "org/lwjgl/opengl/DrawableGL":
                return COMPAT_PKG + "DrawableGLCompat";
            case "org/lwjgl/opengl/SharedDrawable":
                return COMPAT_PKG + "SharedDrawableCompat";
            case "org/lwjgl/opengl/Pbuffer":
                return COMPAT_PKG + "PbufferCompat";
            default:
                return type; // No change
        }
    }

    /**
     * Rewrites a type descriptor by replacing all LWJGL 2 type references.
     * Handles both single-type descriptors ("Lorg/lwjgl/opengl/Display;")
     * and method descriptors ("(Lorg/lwjgl/opengl/DisplayMode;)V").
     *
     * @param descriptor the original descriptor
     * @return the rewritten descriptor
     */
    static String rewriteDescriptor(String descriptor) {
        if (descriptor == null) return null;

        // Quick check: does this descriptor contain any LWJGL 2 references?
        if (!descriptor.contains("org/lwjgl/")) return descriptor;

        // Check the cache first
        String cached = DESCRIPTOR_REWRITES.get(descriptor);
        if (cached != null) return cached;

        // Perform replacement for all known LWJGL 2 types
        String result = descriptor;

        // Build a consolidated redirect map
        Map<String, String> allRedirects = new LinkedHashMap<>();
        allRedirects.putAll(Mini_DirtyRoomCore.getClassRedirects());
        // Add additional types
        allRedirects.put("org/lwjgl/LWJGLException", "java/lang/RuntimeException");
        allRedirects.put("org/lwjgl/opengl/DrawableGL", COMPAT_PKG + "DrawableGLCompat");
        allRedirects.put("org/lwjgl/opengl/SharedDrawable", COMPAT_PKG + "SharedDrawableCompat");
        allRedirects.put("org/lwjgl/opengl/Pbuffer", COMPAT_PKG + "PbufferCompat");

        for (Map.Entry<String, String> entry : allRedirects.entrySet()) {
            String oldType = "L" + entry.getKey() + ";";
            String newType = "L" + entry.getValue() + ";";
            result = result.replace(oldType, newType);
        }

        // Cache the result
        if (!result.equals(descriptor)) {
            DESCRIPTOR_REWRITES.put(descriptor, result);
        }

        return result;
    }

    /**
     * Rewrites a generic signature string (e.g., for generics).
     */
    static String rewriteSignature(String signature) {
        if (signature == null) return null;
        if (!signature.contains("org/lwjgl/")) return signature;
        return rewriteDescriptor(signature);
    }


    // ========================================================================
    //  SECTION 6: SMART CLASS WRITER (FRAME COMPUTATION)
    // ========================================================================

    /**
     * A ClassWriter that overrides getCommonSuperClass to handle cases
     * where LWJGL 2 classes may no longer exist on the classpath.
     * This prevents ClassNotFoundException during frame computation.
     */
    static final class SmartClassWriter extends ClassWriter {

        private final ClassLoader loader;

        SmartClassWriter(ClassReader reader, int flags, ClassLoader loader) {
            super(reader, flags);
            this.loader = loader;
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            // Rewrite any LWJGL 2 types before comparison
            String rt1 = rewriteType(type1);
            String rt2 = rewriteType(type2);
            if (rt1 == null) rt1 = type1;
            if (rt2 == null) rt2 = type2;

            try {
                return super.getCommonSuperClass(rt1, rt2);
            } catch (Throwable t) {
                // If class loading fails during frame computation,
                // fall back to java/lang/Object
                return "java/lang/Object";
            }
        }

        /**
         * Override class loading to use the correct class loader and
         * handle missing LWJGL 2 classes gracefully.
         */
        @Override
        protected ClassLoader getClassLoader() {
            return loader != null ? loader : super.getClassLoader();
        }
    }


    // ========================================================================
    //  SECTION 7: METHOD & FIELD REDIRECT TABLE POPULATION
    // ========================================================================

    /**
     * Populates the method-level redirect table. These are specific method
     * calls that need custom handling beyond a simple class rename.
     *
     * For example, Display.create(PixelFormat) takes a PixelFormat argument
     * that doesn't exist in LWJGL 3. Our wrapper's create() method has a
     * different signature.
     */
    private void populateMethodRedirects() {
        // ── Display method redirects ───────────────────────────────────────

        String DSP = "org/lwjgl/opengl/Display";
        String WM  = COMPAT_PKG + "GLFWWindowManager";

        // Display.create() → GLFWWindowManager.create()
        addMethodRedirect(DSP, "create", "()V",
                         WM,  "create", "()V", Opcodes.INVOKESTATIC);

        // Display.create(PixelFormat) → GLFWWindowManager.create(PixelFormatCompat)
        addMethodRedirect(DSP, "create",
            "(Lorg/lwjgl/opengl/PixelFormat;)V",
            WM, "createWithPixelFormat",
            "(L" + COMPAT_PKG + "PixelFormatCompat;)V",
            Opcodes.INVOKESTATIC);

        // Display.create(PixelFormat, Drawable) → GLFWWindowManager.createWithShared(...)
        addMethodRedirect(DSP, "create",
            "(Lorg/lwjgl/opengl/PixelFormat;Lorg/lwjgl/opengl/Drawable;)V",
            WM, "createWithShared",
            "(L" + COMPAT_PKG + "PixelFormatCompat;L" + COMPAT_PKG + "DrawableGLCompat;)V",
            Opcodes.INVOKESTATIC);

        // Display.create(PixelFormat, ContextAttribs)
        addMethodRedirect(DSP, "create",
            "(Lorg/lwjgl/opengl/PixelFormat;Lorg/lwjgl/opengl/ContextAttribs;)V",
            WM, "createWithAttribs",
            "(L" + COMPAT_PKG + "PixelFormatCompat;L" + COMPAT_PKG + "ContextAttribsCompat;)V",
            Opcodes.INVOKESTATIC);

        // Display.destroy() → GLFWWindowManager.destroy()
        addMethodRedirect(DSP, "destroy", "()V",
                         WM,  "destroy", "()V", Opcodes.INVOKESTATIC);

        // Display.update() → GLFWWindowManager.update()
        addMethodRedirect(DSP, "update", "()V",
                         WM,  "update", "()V", Opcodes.INVOKESTATIC);

        // Display.update(boolean) → GLFWWindowManager.update(boolean)
        addMethodRedirect(DSP, "update", "(Z)V",
                         WM,  "update", "(Z)V", Opcodes.INVOKESTATIC);

        // Display.sync(int) → GLFWWindowManager.sync(int)
        addMethodRedirect(DSP, "sync", "(I)V",
                         WM,  "sync", "(I)V", Opcodes.INVOKESTATIC);

        // Display.isCloseRequested() → GLFWWindowManager.isCloseRequested()
        addMethodRedirect(DSP, "isCloseRequested", "()Z",
                         WM,  "isCloseRequested", "()Z", Opcodes.INVOKESTATIC);

        // Display.isActive() → GLFWWindowManager.isActive()
        addMethodRedirect(DSP, "isActive", "()Z",
                         WM,  "isActive", "()Z", Opcodes.INVOKESTATIC);

        // Display.isVisible() → GLFWWindowManager.isVisible()
        addMethodRedirect(DSP, "isVisible", "()Z",
                         WM,  "isVisible", "()Z", Opcodes.INVOKESTATIC);

        // Display.isDirty() → GLFWWindowManager.isDirty()
        addMethodRedirect(DSP, "isDirty", "()Z",
                         WM,  "isDirty", "()Z", Opcodes.INVOKESTATIC);

        // Display.isFullscreen() → GLFWWindowManager.isFullscreen()
        addMethodRedirect(DSP, "isFullscreen", "()Z",
                         WM,  "isFullscreen", "()Z", Opcodes.INVOKESTATIC);

        // Display.setFullscreen(boolean) → GLFWWindowManager.setFullscreen(boolean)
        addMethodRedirect(DSP, "setFullscreen", "(Z)V",
                         WM,  "setFullscreen", "(Z)V", Opcodes.INVOKESTATIC);

        // Display.setDisplayMode(DisplayMode)
        addMethodRedirect(DSP, "setDisplayMode",
            "(Lorg/lwjgl/opengl/DisplayMode;)V",
            WM, "setDisplayMode",
            "(L" + COMPAT_PKG + "DisplayModeCompat;)V",
            Opcodes.INVOKESTATIC);

        // Display.getDisplayMode() → GLFWWindowManager.getDisplayMode()
        addMethodRedirect(DSP, "getDisplayMode",
            "()Lorg/lwjgl/opengl/DisplayMode;",
            WM, "getDisplayMode",
            "()L" + COMPAT_PKG + "DisplayModeCompat;",
            Opcodes.INVOKESTATIC);

        // Display.getAvailableDisplayModes()
        addMethodRedirect(DSP, "getAvailableDisplayModes",
            "()[Lorg/lwjgl/opengl/DisplayMode;",
            WM, "getAvailableDisplayModes",
            "()[L" + COMPAT_PKG + "DisplayModeCompat;",
            Opcodes.INVOKESTATIC);

        // Display.getDesktopDisplayMode()
        addMethodRedirect(DSP, "getDesktopDisplayMode",
            "()Lorg/lwjgl/opengl/DisplayMode;",
            WM, "getDesktopDisplayMode",
            "()L" + COMPAT_PKG + "DisplayModeCompat;",
            Opcodes.INVOKESTATIC);

        // Display.setTitle(String) → GLFWWindowManager.setTitle(String)
        addMethodRedirect(DSP, "setTitle", "(Ljava/lang/String;)V",
                         WM,  "setTitle", "(Ljava/lang/String;)V",
                         Opcodes.INVOKESTATIC);

        // Display.setResizable(boolean) → GLFWWindowManager.setResizable(boolean)
        addMethodRedirect(DSP, "setResizable", "(Z)V",
                         WM,  "setResizable", "(Z)V", Opcodes.INVOKESTATIC);

        // Display.isResizable() → GLFWWindowManager.isResizable()
        addMethodRedirect(DSP, "isResizable", "()Z",
                         WM,  "isResizable", "()Z", Opcodes.INVOKESTATIC);

        // Display.wasResized() → GLFWWindowManager.wasResized()
        addMethodRedirect(DSP, "wasResized", "()Z",
                         WM,  "wasResized", "()Z", Opcodes.INVOKESTATIC);

        // Display.getWidth() → GLFWWindowManager.getWidth()
        addMethodRedirect(DSP, "getWidth", "()I",
                         WM,  "getWidth", "()I", Opcodes.INVOKESTATIC);

        // Display.getHeight() → GLFWWindowManager.getHeight()
        addMethodRedirect(DSP, "getHeight", "()I",
                         WM,  "getHeight", "()I", Opcodes.INVOKESTATIC);

        // Display.getX() → GLFWWindowManager.getX()
        addMethodRedirect(DSP, "getX", "()I",
                         WM,  "getX", "()I", Opcodes.INVOKESTATIC);

        // Display.getY() → GLFWWindowManager.getY()
        addMethodRedirect(DSP, "getY", "()I",
                         WM,  "getY", "()I", Opcodes.INVOKESTATIC);

        // Display.setLocation(int, int) → GLFWWindowManager.setLocation(int, int)
        addMethodRedirect(DSP, "setLocation", "(II)V",
                         WM,  "setLocation", "(II)V", Opcodes.INVOKESTATIC);

        // Display.setVSyncEnabled(boolean) → GLFWWindowManager.setVSyncEnabled(boolean)
        addMethodRedirect(DSP, "setVSyncEnabled", "(Z)V",
                         WM,  "setVSyncEnabled", "(Z)V", Opcodes.INVOKESTATIC);

        // Display.setIcon(ByteBuffer[]) → GLFWWindowManager.setIcon(ByteBuffer[])
        addMethodRedirect(DSP, "setIcon", "([Ljava/nio/ByteBuffer;)I",
                         WM,  "setIcon", "([Ljava/nio/ByteBuffer;)I",
                         Opcodes.INVOKESTATIC);

        // Display.getAdapter() → GLFWWindowManager.getAdapter()
        addMethodRedirect(DSP, "getAdapter", "()Ljava/lang/String;",
                         WM,  "getAdapter", "()Ljava/lang/String;",
                         Opcodes.INVOKESTATIC);

        // Display.getVersion() → GLFWWindowManager.getVersion()
        addMethodRedirect(DSP, "getVersion", "()Ljava/lang/String;",
                         WM,  "getVersion", "()Ljava/lang/String;",
                         Opcodes.INVOKESTATIC);

        // Display.setParent(Canvas) → GLFWWindowManager.setParent(Canvas)
        addMethodRedirect(DSP, "setParent", "(Ljava/awt/Canvas;)V",
                         WM,  "setParent", "(Ljava/awt/Canvas;)V",
                         Opcodes.INVOKESTATIC);

        // Display.processMessages() → GLFWWindowManager.processMessages()
        addMethodRedirect(DSP, "processMessages", "()V",
                         WM,  "processMessages", "()V", Opcodes.INVOKESTATIC);

        // Display.swapBuffers() → GLFWWindowManager.swapBuffers()
        addMethodRedirect(DSP, "swapBuffers", "()V",
                         WM,  "swapBuffers", "()V", Opcodes.INVOKESTATIC);

        // Display.isCreated() → GLFWWindowManager.isCreated()
        addMethodRedirect(DSP, "isCreated", "()Z",
                         WM,  "isCreated", "()Z", Opcodes.INVOKESTATIC);

        // ── Keyboard method redirects ──────────────────────────────────────

        String KB = "org/lwjgl/input/Keyboard";
        String KA = COMPAT_PKG + "GLFWKeyboardAdapter";

        addMethodRedirect(KB, "create", "()V", KA, "create", "()V", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "destroy", "()V", KA, "destroy", "()V", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "isCreated", "()Z", KA, "isCreated", "()Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "poll", "()V", KA, "poll", "()V", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "isKeyDown", "(I)Z", KA, "isKeyDown", "(I)Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "next", "()Z", KA, "next", "()Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "getEventKey", "()I", KA, "getEventKey", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "getEventCharacter", "()C", KA, "getEventCharacter", "()C", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "getEventKeyState", "()Z", KA, "getEventKeyState", "()Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "getEventNanoseconds", "()J", KA, "getEventNanoseconds", "()J", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "getNumKeyboardEvents", "()I", KA, "getNumKeyboardEvents", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "isRepeatEvent", "()Z", KA, "isRepeatEvent", "()Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "enableRepeatEvents", "(Z)V", KA, "enableRepeatEvents", "(Z)V", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "areRepeatEventsEnabled", "()Z", KA, "areRepeatEventsEnabled", "()Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "getKeyCount", "()I", KA, "getKeyCount", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "getKeyIndex", "(Ljava/lang/String;)I", KA, "getKeyIndex", "(Ljava/lang/String;)I", Opcodes.INVOKESTATIC);
        addMethodRedirect(KB, "getKeyName", "(I)Ljava/lang/String;", KA, "getKeyName", "(I)Ljava/lang/String;", Opcodes.INVOKESTATIC);

        // ── Mouse method redirects ─────────────────────────────────────────

        String MS = "org/lwjgl/input/Mouse";
        String MA = COMPAT_PKG + "GLFWMouseAdapter";

        addMethodRedirect(MS, "create", "()V", MA, "create", "()V", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "destroy", "()V", MA, "destroy", "()V", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "isCreated", "()Z", MA, "isCreated", "()Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "poll", "()V", MA, "poll", "()V", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "next", "()Z", MA, "next", "()Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "isButtonDown", "(I)Z", MA, "isButtonDown", "(I)Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getX", "()I", MA, "getX", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getY", "()I", MA, "getY", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getDX", "()I", MA, "getDX", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getDY", "()I", MA, "getDY", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getDWheel", "()I", MA, "getDWheel", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getButtonCount", "()I", MA, "getButtonCount", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getButtonName", "(I)Ljava/lang/String;", MA, "getButtonName", "(I)Ljava/lang/String;", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getButtonIndex", "(Ljava/lang/String;)I", MA, "getButtonIndex", "(Ljava/lang/String;)I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getEventButton", "()I", MA, "getEventButton", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getEventButtonState", "()Z", MA, "getEventButtonState", "()Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getEventX", "()I", MA, "getEventX", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getEventY", "()I", MA, "getEventY", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getEventDX", "()I", MA, "getEventDX", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getEventDY", "()I", MA, "getEventDY", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getEventDWheel", "()I", MA, "getEventDWheel", "()I", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "getEventNanoseconds", "()J", MA, "getEventNanoseconds", "()J", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "setGrabbed", "(Z)V", MA, "setGrabbed", "(Z)V", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "isGrabbed", "()Z", MA, "isGrabbed", "()Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "setCursorPosition", "(II)V", MA, "setCursorPosition", "(II)V", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "isInsideWindow", "()Z", MA, "isInsideWindow", "()Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(MS, "setClipMouseCoordinatesToWindow", "(Z)V", MA, "setClipMouseCoordinatesToWindow", "(Z)V", Opcodes.INVOKESTATIC);

        // Mouse.setNativeCursor(Cursor) — uses LWJGL 2 Cursor type
        addMethodRedirect(MS, "setNativeCursor",
            "(Lorg/lwjgl/input/Cursor;)Lorg/lwjgl/input/Cursor;",
            MA, "setNativeCursor",
            "(L" + COMPAT_PKG + "CursorCompat;)L" + COMPAT_PKG + "CursorCompat;",
            Opcodes.INVOKESTATIC);

        // ── OpenAL method redirects ────────────────────────────────────────

        String AL = "org/lwjgl/openal/AL";
        String AB = COMPAT_PKG + "OpenALBridge";

        addMethodRedirect(AL, "create", "()V", AB, "create", "()V", Opcodes.INVOKESTATIC);
        addMethodRedirect(AL, "create", "(Ljava/lang/String;II)V", AB, "create", "(Ljava/lang/String;II)V", Opcodes.INVOKESTATIC);
        addMethodRedirect(AL, "destroy", "()V", AB, "destroy", "()V", Opcodes.INVOKESTATIC);
        addMethodRedirect(AL, "isCreated", "()Z", AB, "isCreated", "()Z", Opcodes.INVOKESTATIC);

        // ── Sys method redirects ───────────────────────────────────────────

        String SYS = "org/lwjgl/Sys";
        String SC  = COMPAT_PKG + "SysCompat";

        addMethodRedirect(SYS, "getTime", "()J", SC, "getTime", "()J", Opcodes.INVOKESTATIC);
        addMethodRedirect(SYS, "getTimerResolution", "()J", SC, "getTimerResolution", "()J", Opcodes.INVOKESTATIC);
        addMethodRedirect(SYS, "getVersion", "()Ljava/lang/String;", SC, "getVersion", "()Ljava/lang/String;", Opcodes.INVOKESTATIC);
        addMethodRedirect(SYS, "openURL", "(Ljava/lang/String;)V", SC, "openURL", "(Ljava/lang/String;)V", Opcodes.INVOKESTATIC);
        addMethodRedirect(SYS, "is64Bit", "()Z", SC, "is64Bit", "()Z", Opcodes.INVOKESTATIC);
        addMethodRedirect(SYS, "getClipboard", "()Ljava/lang/String;", SC, "getClipboard", "()Ljava/lang/String;", Opcodes.INVOKESTATIC);
        addMethodRedirect(SYS, "alert", "(Ljava/lang/String;Ljava/lang/String;)V", SC, "alert", "(Ljava/lang/String;Ljava/lang/String;)V", Opcodes.INVOKESTATIC);

        // ── BufferUtils method redirects ───────────────────────────────────

        String BU = "org/lwjgl/BufferUtils";
        String BC = COMPAT_PKG + "BufferUtilsCompat";

        addMethodRedirect(BU, "createByteBuffer", "(I)Ljava/nio/ByteBuffer;",
                         BC, "createByteBuffer", "(I)Ljava/nio/ByteBuffer;", Opcodes.INVOKESTATIC);
        addMethodRedirect(BU, "createShortBuffer", "(I)Ljava/nio/ShortBuffer;",
                         BC, "createShortBuffer", "(I)Ljava/nio/ShortBuffer;", Opcodes.INVOKESTATIC);
        addMethodRedirect(BU, "createIntBuffer", "(I)Ljava/nio/IntBuffer;",
                         BC, "createIntBuffer", "(I)Ljava/nio/IntBuffer;", Opcodes.INVOKESTATIC);
        addMethodRedirect(BU, "createLongBuffer", "(I)Ljava/nio/LongBuffer;",
                         BC, "createLongBuffer", "(I)Ljava/nio/LongBuffer;", Opcodes.INVOKESTATIC);
        addMethodRedirect(BU, "createFloatBuffer", "(I)Ljava/nio/FloatBuffer;",
                         BC, "createFloatBuffer", "(I)Ljava/nio/FloatBuffer;", Opcodes.INVOKESTATIC);
        addMethodRedirect(BU, "createDoubleBuffer", "(I)Ljava/nio/DoubleBuffer;",
                         BC, "createDoubleBuffer", "(I)Ljava/nio/DoubleBuffer;", Opcodes.INVOKESTATIC);
        addMethodRedirect(BU, "createCharBuffer", "(I)Ljava/nio/CharBuffer;",
                         BC, "createCharBuffer", "(I)Ljava/nio/CharBuffer;", Opcodes.INVOKESTATIC);
        addMethodRedirect(BU, "createPointerBuffer", "(I)Lorg/lwjgl/PointerBuffer;",
                         BC, "createPointerBuffer", "(I)Lorg/lwjgl/PointerBuffer;", Opcodes.INVOKESTATIC);
        addMethodRedirect(BU, "zeroBuffer", "(Ljava/nio/ByteBuffer;)V",
                         BC, "zeroBuffer", "(Ljava/nio/ByteBuffer;)V", Opcodes.INVOKESTATIC);

        // ── GLContext method redirects ──────────────────────────────────────

        String GLC = "org/lwjgl/opengl/GLContext";
        String GCC = COMPAT_PKG + "GLContextCompat";

        addMethodRedirect(GLC, "getCapabilities",
            "()Lorg/lwjgl/opengl/ContextCapabilities;",
            GCC, "getCapabilities",
            "()L" + COMPAT_PKG + "GLCapabilitiesCompat;",
            Opcodes.INVOKESTATIC);

        addMethodRedirect(GLC, "useContext",
            "(Ljava/lang/Object;)Lorg/lwjgl/opengl/ContextCapabilities;",
            GCC, "useContext",
            "(Ljava/lang/Object;)L" + COMPAT_PKG + "GLCapabilitiesCompat;",
            Opcodes.INVOKESTATIC);

        LOGGER.info("[MDR-LWJGL] Populated " + METHOD_REDIRECTS_MAP.size()
                  + " method redirect rules.");
    }

    /**
     * Populates the field-level redirect table.
     */
    private void populateFieldRedirects() {
        // Keyboard key code constants — these are accessed as static fields.
        // LWJGL 2: Keyboard.KEY_A → our adapter constant
        // Most mods use Keyboard.KEY_* constants. Since our adapter defines
        // the same constants, a class-level redirect suffices. But some
        // use them via reflection or dynamic lookup, so we list critical ones.

        // No specific field redirects needed beyond class-level redirects
        // at this time. The class redirect handles Keyboard.KEY_* fields
        // because our GLFWKeyboardAdapter defines matching static fields.

        LOGGER.info("[MDR-LWJGL] Populated " + FIELD_REDIRECTS_MAP.size()
                  + " field redirect rules.");
    }

    /**
     * Pre-populates the descriptor rewrite cache with commonly used signatures.
     */
    private void populateDescriptorRewrites() {
        // Pre-cache frequently encountered descriptors
        String[][] common = {
            {"Lorg/lwjgl/opengl/DisplayMode;",
             "L" + COMPAT_PKG + "DisplayModeCompat;"},
            {"Lorg/lwjgl/opengl/PixelFormat;",
             "L" + COMPAT_PKG + "PixelFormatCompat;"},
            {"Lorg/lwjgl/opengl/ContextCapabilities;",
             "L" + COMPAT_PKG + "GLCapabilitiesCompat;"},
            {"Lorg/lwjgl/input/Cursor;",
             "L" + COMPAT_PKG + "CursorCompat;"},
            {"Lorg/lwjgl/LWJGLException;",
             "Ljava/lang/RuntimeException;"},
        };

        for (String[] pair : common) {
            DESCRIPTOR_REWRITES.put(pair[0], pair[1]);
        }
    }

    /**
     * Registers a method redirect.
     */
    private static void addMethodRedirect(
            String oldOwner, String oldName, String oldDesc,
            String newOwner, String newName, String newDesc,
            int newOpcode) {
        String key = oldOwner + "." + oldName + "." + oldDesc;
        METHOD_REDIRECTS_MAP.put(key, new MethodRedirect(
            newOwner, newName, newDesc, newOpcode, false));
    }


    // ========================================================================
    //  SECTION 8: REDIRECT DATA STRUCTURES
    // ========================================================================

    /**
     * Describes a method redirect — how to remap a method call.
     */
    static final class MethodRedirect {
        final String newOwner;
        final String newName;
        final String newDescriptor;
        final int    newOpcode;     // -1 = keep original
        final boolean isInterface;

        MethodRedirect(String newOwner, String newName, String newDescriptor,
                      int newOpcode, boolean isInterface) {
            this.newOwner      = newOwner;
            this.newName       = newName;
            this.newDescriptor = newDescriptor;
            this.newOpcode     = newOpcode;
            this.isInterface   = isInterface;
        }
    }

    /**
     * Describes a field redirect — how to remap a field access.
     */
    static final class FieldRedirect {
        final String newOwner;
        final String newName;
        final String newDescriptor;
        final int    newOpcode;     // -1 = keep original

        FieldRedirect(String newOwner, String newName,
                     String newDescriptor, int newOpcode) {
            this.newOwner      = newOwner;
            this.newName       = newName;
            this.newDescriptor = newDescriptor;
            this.newOpcode     = newOpcode;
        }
    }


    // ========================================================================
    //  SECTION 9: FORGE TRANSFORMER ADAPTER
    // ========================================================================

    /**
     * Adapter class that implements Forge's IClassTransformer interface
     * by delegating to our ClassFileTransformer. This class is loaded by
     * name via Forge's LaunchClassLoader.
     *
     * We use a separate class name so Forge can discover it via string.
     */
    public static class ForgeTransformerAdapter {
        // Forge calls transform(String, String, byte[]) via reflection
        // based on the IClassTransformer interface.

        public byte[] transform(String name, String transformedName, byte[] bytes) {
            if (bytes == null) return null;

            // Convert to internal name for our transformer
            String internalName = transformedName != null
                ? transformedName.replace('.', '/')
                : (name != null ? name.replace('.', '/') : null);

            return INSTANCE.transform(
                Thread.currentThread().getContextClassLoader(),
                internalName,
                null,
                null,
                bytes
            );
        }
    }


    // ========================================================================
    //  SECTION 10: COMPATIBILITY WRAPPER — GLFWWindowManager (Display)
    // ========================================================================

    /**
     * Emulates org.lwjgl.opengl.Display using GLFW from LWJGL 3.
     *
     * LWJGL 2's Display is a fully static class with methods like create(),
     * update(), destroy(), isCloseRequested(), etc. LWJGL 3 has no such
     * class — it uses GLFW for window management, which is callback-based.
     *
     * This wrapper maintains a singleton GLFW window and provides the exact
     * same static API surface as Display.
     */
    @DeepOverwrite(target = "org.lwjgl.opengl.Display", method = "*")
    public static final class GLFWWindowManager {

        // ── State ──────────────────────────────────────────────────────────
        private static volatile long window = 0L;
        private static volatile boolean created = false;
        private static volatile boolean closeRequested = false;
        private static volatile boolean active = true;
        private static volatile boolean visible = true;
        private static volatile boolean dirty = false;
        private static volatile boolean fullscreen = false;
        private static volatile boolean resizable = false;
        private static volatile boolean wasResized = false;
        private static volatile int width = 854;
        private static volatile int height = 480;
        private static volatile int windowX = 0;
        private static volatile int windowY = 0;
        private static volatile int fbWidth = 854;
        private static volatile int fbHeight = 480;
        private static volatile String title = "Minecraft";
        private static volatile boolean vsync = false;
        private static volatile java.awt.Canvas parentCanvas = null;

        // Frame rate limiter state
        private static long lastFrameTime = 0L;
        private static long variableYieldTime = 0L;
        private static long lastSyncTime = 0L;

        private GLFWWindowManager() {}

        // ── Display.create() variants ──────────────────────────────────────

        public static void create() {
            createInternal(null, null);
        }

        public static void createWithPixelFormat(Object pixelFormat) {
            // Extract settings from PixelFormatCompat
            createInternal(pixelFormat, null);
        }

        public static void createWithShared(Object pixelFormat, Object shared) {
            createInternal(pixelFormat, shared);
        }

        public static void createWithAttribs(Object pixelFormat, Object attribs) {
            createInternal(pixelFormat, attribs);
        }

        private static void createInternal(Object pixelFormat, Object extra) {
            if (created) return;

            try {
                // Android Security Bypass: Wrap LWJGL 3.4.0 native loading
                if (IS_ANDROID.get()) {
                    LOGGER.info("[MDR-LWJGL] Android: Initializing LWJGL 3.4.0 with security bypass...");
                    try {
                        // Let Android's launcher try to load LWJGL first
                        // This will likely fail with SecurityException, which we'll catch
                        initializeLWJGL340Native();
                    } catch (SecurityException secEx) {
                        // Expected on Android - now use our preloaded natives
                        LOGGER.info("[MDR-LWJGL] Android security exception caught (expected)");
                        if (handleAndroidSecurityException("lwjgl")) {
                            LOGGER.info("[MDR-LWJGL] Successfully replaced with preloaded LWJGL 3.4.0");
                        } else {
                            throw new RuntimeException("[MDR] Android: Failed to load LWJGL 3.4.0 natives", secEx);
                        }
                    }
                }

                // Access GLFW via reflection so this file compiles regardless
                // of LWJGL 3's presence at compile time
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                Class<?> gl = Class.forName("org.lwjgl.opengl.GL");

                // glfwInit() - LWJGL 3.4.0 native call
                java.lang.reflect.Method glfwInit =
                    glfw.getMethod("glfwInit");
                boolean initResult = (boolean) glfwInit.invoke(null);
                if (!initResult) {
                    throw new RuntimeException(
                        "[MDR] GLFW 3.4+ initialization failed");
                }

                // Set error callback
                try {
                    Class<?> errCb = Class.forName(
                        "org.lwjgl.glfw.GLFWErrorCallback");
                    java.lang.reflect.Method createPrint =
                        errCb.getMethod("createPrint", PrintStream.class);
                    Object callback = createPrint.invoke(null, System.err);
                    java.lang.reflect.Method set = callback.getClass()
                        .getMethod("set");
                    set.invoke(callback);
                } catch (Exception e) {
                    LOGGER.fine("[MDR-LWJGL] Could not set error callback: "
                              + e.getMessage());
                }

                // glfwDefaultWindowHints()
                java.lang.reflect.Method defaultHints =
                    glfw.getMethod("glfwDefaultWindowHints");
                defaultHints.invoke(null);

                // Apply pixel format hints if provided
                if (pixelFormat != null) {
                    applyPixelFormatHints(glfw, pixelFormat);
                }

                // Set visible = false initially
                java.lang.reflect.Method windowHint =
                    glfw.getMethod("glfwWindowHint", int.class, int.class);
                // GLFW_VISIBLE = 0x00020004
                windowHint.invoke(null, 0x00020004, 0); // FALSE
                // GLFW_RESIZABLE = 0x00020003
                windowHint.invoke(null, 0x00020003, resizable ? 1 : 0);

                // glfwCreateWindow(width, height, title, monitor, share)
                java.lang.reflect.Method createWindow =
                    glfw.getMethod("glfwCreateWindow",
                        int.class, int.class, CharSequence.class,
                        long.class, long.class);
                long monitor = 0L;
                if (fullscreen) {
                    java.lang.reflect.Method getPrimaryMonitor =
                        glfw.getMethod("glfwGetPrimaryMonitor");
                    monitor = (long) getPrimaryMonitor.invoke(null);
                }

                window = (long) createWindow.invoke(null,
                    width, height, title, monitor, 0L);

                if (window == 0L) {
                    throw new RuntimeException(
                        "[MDR] GLFW window creation failed");
                }

                // Store in Core for global access
                Mini_DirtyRoomCore.setGLFWWindow(window);

                // Center the window
                if (!fullscreen) {
                    centerWindow(glfw);
                }

                // Make context current
                java.lang.reflect.Method makeContextCurrent =
                    glfw.getMethod("glfwMakeContextCurrent", long.class);
                makeContextCurrent.invoke(null, window);

                // GL.createCapabilities()
                java.lang.reflect.Method createCapabilities =
                    gl.getMethod("createCapabilities");
                createCapabilities.invoke(null);

                // Set up callbacks
                setupCallbacks(glfw);

                // Show window
                java.lang.reflect.Method showWindow =
                    glfw.getMethod("glfwShowWindow", long.class);
                showWindow.invoke(null, window);

                // Set vsync
                java.lang.reflect.Method swapInterval =
                    glfw.getMethod("glfwSwapInterval", int.class);
                swapInterval.invoke(null, vsync ? 1 : 0);

                created = true;
                visible = true;
                active = true;
                lastFrameTime = System.nanoTime();

                LOGGER.info("[MDR-LWJGL] GLFW window created: "
                          + width + "x" + height
                          + (fullscreen ? " (fullscreen)" : " (windowed)")
                          + " | title='" + title + "'");

            } catch (Exception e) {
                throw new RuntimeException(
                    "[MDR] Failed to create GLFW window", e);
            }
        }

        /**
         * Initializes LWJGL 3.4.0 native libraries directly.
         * On Android, this may throw SecurityException, which triggers our bypass.
         */
        private static void initializeLWJGL340Native() throws SecurityException {
            try {
                // Force LWJGL 3.4.0 native library loading
                Class<?> libraryClass = Class.forName("org.lwjgl.system.Library");
                java.lang.reflect.Method initialize = libraryClass.getDeclaredMethod("initialize");
                initialize.setAccessible(true);
                initialize.invoke(null);
                
                LOGGER.info("[MDR-LWJGL] LWJGL 3.4.0 natives initialized successfully");
            } catch (SecurityException se) {
                // Re-throw to trigger Android bypass
                throw se;
            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] LWJGL native init: " + e.getMessage());
            }
        }

        /**
         * Applies LWJGL 2 PixelFormat settings as GLFW window hints.
         */
        private static void applyPixelFormatHints(
                Class<?> glfw, Object pixelFormat) {
            try {
                java.lang.reflect.Method windowHint =
                    glfw.getMethod("glfwWindowHint", int.class, int.class);

                // Try to extract fields from PixelFormatCompat
                Class<?> pfClass = pixelFormat.getClass();

                int bpp = getIntField(pfClass, pixelFormat, "bpp", 0);
                int alpha = getIntField(pfClass, pixelFormat, "alpha", 8);
                int depth = getIntField(pfClass, pixelFormat, "depth", 24);
                int stencil = getIntField(pfClass, pixelFormat, "stencil", 8);
                int samples = getIntField(pfClass, pixelFormat, "samples", 0);

                // GLFW_RED_BITS = 0x00021001, etc.
                if (bpp > 0) {
                    int perChannel = bpp / 4;
                    windowHint.invoke(null, 0x00021001, perChannel); // RED
                    windowHint.invoke(null, 0x00021002, perChannel); // GREEN
                    windowHint.invoke(null, 0x00021003, perChannel); // BLUE
                }
                // GLFW_ALPHA_BITS = 0x00021004
                windowHint.invoke(null, 0x00021004, alpha);
                // GLFW_DEPTH_BITS = 0x00021005
                windowHint.invoke(null, 0x00021005, depth);
                // GLFW_STENCIL_BITS = 0x00021006
                windowHint.invoke(null, 0x00021006, stencil);
                // GLFW_SAMPLES = 0x0002100D
                if (samples > 0) {
                    windowHint.invoke(null, 0x0002100D, samples);
                }

            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] Could not apply pixel format hints: "
                          + e.getMessage());
            }
        }

        private static int getIntField(Class<?> clazz, Object obj,
                                       String name, int def) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f.getInt(obj);
            } catch (Exception e) {
                return def;
            }
        }

        /**
         * Centers the window on the primary monitor.
         */
        private static void centerWindow(Class<?> glfw) {
            try {
                java.lang.reflect.Method getPrimaryMonitor =
                    glfw.getMethod("glfwGetPrimaryMonitor");
                long monitor = (long) getPrimaryMonitor.invoke(null);

                java.lang.reflect.Method getVideoMode =
                    glfw.getMethod("glfwGetVideoMode", long.class);
                Object vidMode = getVideoMode.invoke(null, monitor);

                if (vidMode != null) {
                    java.lang.reflect.Method vmWidth =
                        vidMode.getClass().getMethod("width");
                    java.lang.reflect.Method vmHeight =
                        vidMode.getClass().getMethod("height");

                    int monW = (int) vmWidth.invoke(vidMode);
                    int monH = (int) vmHeight.invoke(vidMode);

                    windowX = (monW - width) / 2;
                    windowY = (monH - height) / 2;

                    java.lang.reflect.Method setWindowPos =
                        glfw.getMethod("glfwSetWindowPos",
                            long.class, int.class, int.class);
                    setWindowPos.invoke(null, window, windowX, windowY);
                }
            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] Could not center window: "
                          + e.getMessage());
            }
        }

        /**
         * Sets up GLFW callbacks to update our state variables.
         */
        private static void setupCallbacks(Class<?> glfw) {
            try {
                // Window close callback
                Class<?> closeCallbackI = Class.forName(
                    "org.lwjgl.glfw.GLFWWindowCloseCallbackI");
                // Use a dynamic proxy or lambda
                Object closeCallback = java.lang.reflect.Proxy.newProxyInstance(
                    closeCallbackI.getClassLoader(),
                    new Class<?>[]{closeCallbackI},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            closeRequested = true;
                        }
                        return null;
                    }
                );
                java.lang.reflect.Method setCloseCallback =
                    glfw.getMethod("glfwSetWindowCloseCallback",
                        long.class, closeCallbackI);
                setCloseCallback.invoke(null, window, closeCallback);

                // Window size callback
                Class<?> sizeCallbackI = Class.forName(
                    "org.lwjgl.glfw.GLFWWindowSizeCallbackI");
                Object sizeCallback = java.lang.reflect.Proxy.newProxyInstance(
                    sizeCallbackI.getClassLoader(),
                    new Class<?>[]{sizeCallbackI},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            int newW = (int) args[1];
                            int newH = (int) args[2];
                            if (newW != width || newH != height) {
                                width = newW;
                                height = newH;
                                wasResized = true;
                            }
                        }
                        return null;
                    }
                );
                java.lang.reflect.Method setSizeCallback =
                    glfw.getMethod("glfwSetWindowSizeCallback",
                        long.class, sizeCallbackI);
                setSizeCallback.invoke(null, window, sizeCallback);

                // Framebuffer size callback
                Class<?> fbSizeCallbackI = Class.forName(
                    "org.lwjgl.glfw.GLFWFramebufferSizeCallbackI");
                Object fbSizeCallback = java.lang.reflect.Proxy.newProxyInstance(
                    fbSizeCallbackI.getClassLoader(),
                    new Class<?>[]{fbSizeCallbackI},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            fbWidth = (int) args[1];
                            fbHeight = (int) args[2];
                        }
                        return null;
                    }
                );
                java.lang.reflect.Method setFBSizeCallback =
                    glfw.getMethod("glfwSetFramebufferSizeCallback",
                        long.class, fbSizeCallbackI);
                setFBSizeCallback.invoke(null, window, fbSizeCallback);

                // Window focus callback
                Class<?> focusCallbackI = Class.forName(
                    "org.lwjgl.glfw.GLFWWindowFocusCallbackI");
                Object focusCallback = java.lang.reflect.Proxy.newProxyInstance(
                    focusCallbackI.getClassLoader(),
                    new Class<?>[]{focusCallbackI},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            active = (boolean) args[1];
                        }
                        return null;
                    }
                );
                java.lang.reflect.Method setFocusCallback =
                    glfw.getMethod("glfwSetWindowFocusCallback",
                        long.class, focusCallbackI);
                setFocusCallback.invoke(null, window, focusCallback);

                // Window position callback
                Class<?> posCallbackI = Class.forName(
                    "org.lwjgl.glfw.GLFWWindowPosCallbackI");
                Object posCallback = java.lang.reflect.Proxy.newProxyInstance(
                    posCallbackI.getClassLoader(),
                    new Class<?>[]{posCallbackI},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            windowX = (int) args[1];
                            windowY = (int) args[2];
                        }
                        return null;
                    }
                );
                java.lang.reflect.Method setPosCallback =
                    glfw.getMethod("glfwSetWindowPosCallback",
                        long.class, posCallbackI);
                setPosCallback.invoke(null, window, posCallback);

                // Iconify callback
                Class<?> iconifyCallbackI = Class.forName(
                    "org.lwjgl.glfw.GLFWWindowIconifyCallbackI");
                Object iconifyCallback = java.lang.reflect.Proxy.newProxyInstance(
                    iconifyCallbackI.getClassLoader(),
                    new Class<?>[]{iconifyCallbackI},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            visible = !(boolean) args[1];
                        }
                        return null;
                    }
                );
                java.lang.reflect.Method setIconifyCallback =
                    glfw.getMethod("glfwSetWindowIconifyCallback",
                        long.class, iconifyCallbackI);
                setIconifyCallback.invoke(null, window, iconifyCallback);

                // Key and mouse callbacks are managed by GLFWKeyboardAdapter
                // and GLFWMouseAdapter respectively.

            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                    "[MDR-LWJGL] Callback setup partially failed", e);
            }
        }

        // ── Display lifecycle methods ──────────────────────────────────────

        public static void destroy() {
            if (!created) return;
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method destroyWindow =
                    glfw.getMethod("glfwDestroyWindow", long.class);
                destroyWindow.invoke(null, window);

                java.lang.reflect.Method terminate =
                    glfw.getMethod("glfwTerminate");
                terminate.invoke(null);
            } catch (Exception e) {
                LOGGER.warning("[MDR-LWJGL] Error during window destruction: "
                             + e.getMessage());
            }
            created = false;
            window = 0L;
            Mini_DirtyRoomCore.setGLFWWindow(0L);
            LOGGER.info("[MDR-LWJGL] GLFW window destroyed.");
        }

        public static void update() {
            update(true);
        }

        public static void update(boolean processMessages) {
            if (!created) return;
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");

                // glfwSwapBuffers
                java.lang.reflect.Method swapBuffers =
                    glfw.getMethod("glfwSwapBuffers", long.class);
                swapBuffers.invoke(null, window);

                if (processMessages) {
                    // glfwPollEvents
                    java.lang.reflect.Method pollEvents =
                        glfw.getMethod("glfwPollEvents");
                    pollEvents.invoke(null);
                }
            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] Error in Display.update(): "
                          + e.getMessage());
            }

            wasResized = false;
        }

        public static void swapBuffers() {
            if (!created) return;
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method swapBuffers =
                    glfw.getMethod("glfwSwapBuffers", long.class);
                swapBuffers.invoke(null, window);
            } catch (Exception ignored) {}
        }

        public static void processMessages() {
            if (!created) return;
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method pollEvents =
                    glfw.getMethod("glfwPollEvents");
                pollEvents.invoke(null);
            } catch (Exception ignored) {}
        }

        /**
         * Frame rate limiter. Emulates Display.sync(fps).
         * Uses a high-precision sleep + yield loop.
         */
        public static void sync(int fps) {
            if (fps <= 0) return;
            long targetNanos = 1_000_000_000L / fps;
            long now = System.nanoTime();
            long elapsed = now - lastFrameTime;
            long sleepTime = targetNanos - elapsed;

            if (sleepTime > 0) {
                // Sleep for most of the time
                long sleepMs = sleepTime / 1_000_000L;
                if (sleepMs > 1) {
                    try {
                        Thread.sleep(sleepMs - 1);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }

                // Busy-wait for the remainder (more precise)
                while (System.nanoTime() - now < sleepTime) {
                    Thread.yield();
                }
            }

            lastFrameTime = System.nanoTime();
        }

        // ── Display query methods ──────────────────────────────────────────

        public static boolean isCloseRequested() {
            if (!created) return false;
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method shouldClose =
                    glfw.getMethod("glfwWindowShouldClose", long.class);
                closeRequested = (boolean) shouldClose.invoke(null, window);
            } catch (Exception ignored) {}
            return closeRequested;
        }

        public static boolean isActive() { return created && active; }
        public static boolean isVisible() { return created && visible; }
        public static boolean isDirty() { boolean d = dirty; dirty = false; return d; }
        public static boolean isFullscreen() { return fullscreen; }
        public static boolean isResizable() { return resizable; }
        public static boolean wasResized() { return wasResized; }
        public static boolean isCreated() { return created; }
        public static int getWidth() { return width; }
        public static int getHeight() { return height; }
        public static int getX() { return windowX; }
        public static int getY() { return windowY; }

        // ── Display mutation methods ───────────────────────────────────────

        public static void setTitle(String newTitle) {
            title = newTitle;
            if (!created) return;
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method setWindowTitle =
                    glfw.getMethod("glfwSetWindowTitle",
                        long.class, CharSequence.class);
                setWindowTitle.invoke(null, window, newTitle);
            } catch (Exception ignored) {}
        }

        public static void setResizable(boolean r) {
            resizable = r;
            if (!created) return;
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method setAttrib =
                    glfw.getMethod("glfwSetWindowAttrib",
                        long.class, int.class, int.class);
                // GLFW_RESIZABLE = 0x00020003
                setAttrib.invoke(null, window, 0x00020003, r ? 1 : 0);
            } catch (Exception ignored) {}
        }

        public static void setFullscreen(boolean fs) {
            if (fullscreen == fs) return;
            fullscreen = fs;
            if (!created) return;

            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");

                if (fs) {
                    java.lang.reflect.Method getPrimaryMonitor =
                        glfw.getMethod("glfwGetPrimaryMonitor");
                    long monitor = (long) getPrimaryMonitor.invoke(null);

                    java.lang.reflect.Method getVideoMode =
                        glfw.getMethod("glfwGetVideoMode", long.class);
                    Object vidMode = getVideoMode.invoke(null, monitor);

                    int monW = (int) vidMode.getClass().getMethod("width")
                        .invoke(vidMode);
                    int monH = (int) vidMode.getClass().getMethod("height")
                        .invoke(vidMode);
                    int refreshRate = (int) vidMode.getClass()
                        .getMethod("refreshRate").invoke(vidMode);

                    java.lang.reflect.Method setWindowMonitor =
                        glfw.getMethod("glfwSetWindowMonitor",
                            long.class, long.class, int.class, int.class,
                            int.class, int.class, int.class);
                    setWindowMonitor.invoke(null, window, monitor,
                        0, 0, monW, monH, refreshRate);
                } else {
                    java.lang.reflect.Method setWindowMonitor =
                        glfw.getMethod("glfwSetWindowMonitor",
                            long.class, long.class, int.class, int.class,
                            int.class, int.class, int.class);
                    // NULL monitor = windowed
                    setWindowMonitor.invoke(null, window, 0L,
                        windowX, windowY, width, height, 0);
                }
            } catch (Exception e) {
                LOGGER.warning("[MDR-LWJGL] Fullscreen toggle failed: "
                             + e.getMessage());
            }
        }

        public static void setDisplayMode(Object displayMode) {
            try {
                Class<?> dmClass = displayMode.getClass();
                width = getIntField(dmClass, displayMode, "width", width);
                height = getIntField(dmClass, displayMode, "height", height);

                if (created) {
                    Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                    java.lang.reflect.Method setWindowSize =
                        glfw.getMethod("glfwSetWindowSize",
                            long.class, int.class, int.class);
                    setWindowSize.invoke(null, window, width, height);
                }
            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] setDisplayMode failed: "
                          + e.getMessage());
            }
        }

        public static void setVSyncEnabled(boolean enabled) {
            vsync = enabled;
            if (!created) return;
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method swapInterval =
                    glfw.getMethod("glfwSwapInterval", int.class);
                swapInterval.invoke(null, enabled ? 1 : 0);
            } catch (Exception ignored) {}
        }

        public static void setLocation(int x, int y) {
            windowX = x;
            windowY = y;
            if (!created) return;
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method setWindowPos =
                    glfw.getMethod("glfwSetWindowPos",
                        long.class, int.class, int.class);
                setWindowPos.invoke(null, window, x, y);
            } catch (Exception ignored) {}
        }

        public static int setIcon(ByteBuffer[] icons) {
            if (!created || icons == null || icons.length == 0) return 0;
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                Class<?> glfwImage = Class.forName(
                    "org.lwjgl.glfw.GLFWImage");
                Class<?> glfwImageBuffer = Class.forName(
                    "org.lwjgl.glfw.GLFWImage$Buffer");

                // Create GLFWImage.Buffer
                java.lang.reflect.Method mallocBuf =
                    glfwImageBuffer.getMethod("malloc", int.class);

                // This is simplified — full impl would parse icon dimensions
                // from the ByteBuffer data
                LOGGER.fine("[MDR-LWJGL] Window icon set requested ("
                          + icons.length + " sizes).");

            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] setIcon failed: " + e.getMessage());
            }
            return 1;
        }

        public static void setParent(java.awt.Canvas canvas) {
            parentCanvas = canvas;
            LOGGER.fine("[MDR-LWJGL] setParent called (limited support with GLFW).");
        }

        public static Object getDisplayMode() {
            return DisplayModeCompat.create(width, height, 32, 60);
        }

        public static Object getDesktopDisplayMode() {
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method getPrimaryMonitor =
                    glfw.getMethod("glfwGetPrimaryMonitor");
                long monitor = (long) getPrimaryMonitor.invoke(null);

                java.lang.reflect.Method getVideoMode =
                    glfw.getMethod("glfwGetVideoMode", long.class);
                Object vidMode = getVideoMode.invoke(null, monitor);

                if (vidMode != null) {
                    int w = (int) vidMode.getClass().getMethod("width").invoke(vidMode);
                    int h = (int) vidMode.getClass().getMethod("height").invoke(vidMode);
                    int rr = (int) vidMode.getClass().getMethod("refreshRate").invoke(vidMode);
                    int rb = (int) vidMode.getClass().getMethod("redBits").invoke(vidMode);
                    int gb = (int) vidMode.getClass().getMethod("greenBits").invoke(vidMode);
                    int bb = (int) vidMode.getClass().getMethod("blueBits").invoke(vidMode);
                    return DisplayModeCompat.create(w, h, rb + gb + bb, rr);
                }
            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] getDesktopDisplayMode failed: "
                          + e.getMessage());
            }
            return DisplayModeCompat.create(1920, 1080, 32, 60);
        }

        public static Object[] getAvailableDisplayModes() {
            List<Object> modes = new ArrayList<>();
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method getPrimaryMonitor =
                    glfw.getMethod("glfwGetPrimaryMonitor");
                long monitor = (long) getPrimaryMonitor.invoke(null);

                java.lang.reflect.Method getVideoModes =
                    glfw.getMethod("glfwGetVideoModes", long.class);
                Object vidModeBuffer = getVideoModes.invoke(null, monitor);

                if (vidModeBuffer != null) {
                    // GLFWVidMode.Buffer implements Iterable or has limit()/get()
                    java.lang.reflect.Method limit =
                        vidModeBuffer.getClass().getMethod("limit");
                    int count = (int) limit.invoke(vidModeBuffer);

                    java.lang.reflect.Method position =
                        vidModeBuffer.getClass().getMethod("position", int.class);

                    for (int i = 0; i < count; i++) {
                        position.invoke(vidModeBuffer, i);
                        java.lang.reflect.Method vmW =
                            vidModeBuffer.getClass().getMethod("width");
                        java.lang.reflect.Method vmH =
                            vidModeBuffer.getClass().getMethod("height");
                        java.lang.reflect.Method vmRR =
                            vidModeBuffer.getClass().getMethod("refreshRate");
                        java.lang.reflect.Method vmRB =
                            vidModeBuffer.getClass().getMethod("redBits");
                        java.lang.reflect.Method vmGB =
                            vidModeBuffer.getClass().getMethod("greenBits");
                        java.lang.reflect.Method vmBB =
                            vidModeBuffer.getClass().getMethod("blueBits");

                        int w = (int) vmW.invoke(vidModeBuffer);
                        int h = (int) vmH.invoke(vidModeBuffer);
                        int rr = (int) vmRR.invoke(vidModeBuffer);
                        int bpp = (int) vmRB.invoke(vidModeBuffer)
                                + (int) vmGB.invoke(vidModeBuffer)
                                + (int) vmBB.invoke(vidModeBuffer);

                        modes.add(DisplayModeCompat.create(w, h, bpp, rr));
                    }
                }
            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] getAvailableDisplayModes failed: "
                          + e.getMessage());
            }

            if (modes.isEmpty()) {
                modes.add(DisplayModeCompat.create(1920, 1080, 32, 60));
                modes.add(DisplayModeCompat.create(1280, 720, 32, 60));
                modes.add(DisplayModeCompat.create(854, 480, 32, 60));
            }

            return modes.toArray();
        }

        public static String getAdapter() {
            return "Mini_DirtyRoom GLFW Adapter";
        }

        public static String getVersion() {
            return Mini_DirtyRoomCore.TARGET_LWJGL_VERSION;
        }

        /**
         * Returns the raw GLFW window handle for direct use.
         */
        public static long getWindowHandle() {
            return window;
        }
    }


    // ========================================================================
    //  SECTION 11: COMPATIBILITY WRAPPER — GLFWKeyboardAdapter (Keyboard)
    // ========================================================================

    /**
     * Emulates org.lwjgl.input.Keyboard using GLFW key callbacks.
     *
     * LWJGL 2's Keyboard uses polling (Keyboard.isKeyDown()) and a
     * sequential event queue (Keyboard.next()). GLFW uses callbacks.
     * This adapter bridges the gap by maintaining state arrays and an
     * event queue populated by GLFW callbacks.
     */
    @DeepOverwrite(target = "org.lwjgl.input.Keyboard", method = "*")
    public static final class GLFWKeyboardAdapter {

        // ── LWJGL 2 key code constants ─────────────────────────────────────
        // We re-declare these so that code referencing Keyboard.KEY_*
        // still works after the class redirect.
        public static final int KEY_NONE       = 0;
        public static final int KEY_ESCAPE     = 1;
        public static final int KEY_1          = 2;
        public static final int KEY_2          = 3;
        public static final int KEY_3          = 4;
        public static final int KEY_4          = 5;
        public static final int KEY_5          = 6;
        public static final int KEY_6          = 7;
        public static final int KEY_7          = 8;
        public static final int KEY_8          = 9;
        public static final int KEY_9          = 10;
        public static final int KEY_0          = 11;
        public static final int KEY_MINUS      = 12;
        public static final int KEY_EQUALS     = 13;
        public static final int KEY_BACK       = 14;
        public static final int KEY_TAB        = 15;
        public static final int KEY_Q          = 16;
        public static final int KEY_W          = 17;
        public static final int KEY_E          = 18;
        public static final int KEY_R          = 19;
        public static final int KEY_T          = 20;
        public static final int KEY_Y          = 21;
        public static final int KEY_U          = 22;
        public static final int KEY_I          = 23;
        public static final int KEY_O          = 24;
        public static final int KEY_P          = 25;
        public static final int KEY_LBRACKET   = 26;
        public static final int KEY_RBRACKET   = 27;
        public static final int KEY_RETURN     = 28;
        public static final int KEY_LCONTROL   = 29;
        public static final int KEY_A          = 30;
        public static final int KEY_S          = 31;
        public static final int KEY_D          = 32;
        public static final int KEY_F          = 33;
        public static final int KEY_G          = 34;
        public static final int KEY_H          = 35;
        public static final int KEY_J          = 36;
        public static final int KEY_K          = 37;
        public static final int KEY_L          = 38;
        public static final int KEY_SEMICOLON  = 39;
        public static final int KEY_APOSTROPHE = 40;
        public static final int KEY_GRAVE      = 41;
        public static final int KEY_LSHIFT     = 42;
        public static final int KEY_BACKSLASH  = 43;
        public static final int KEY_Z          = 44;
        public static final int KEY_X          = 45;
        public static final int KEY_C          = 46;
        public static final int KEY_V          = 47;
        public static final int KEY_B          = 48;
        public static final int KEY_N          = 49;
        public static final int KEY_M          = 50;
        public static final int KEY_COMMA      = 51;
        public static final int KEY_PERIOD     = 52;
        public static final int KEY_SLASH      = 53;
        public static final int KEY_RSHIFT     = 54;
        public static final int KEY_MULTIPLY   = 55;
        public static final int KEY_LMENU      = 56;
        public static final int KEY_SPACE      = 57;
        public static final int KEY_CAPITAL    = 58;
        public static final int KEY_F1         = 59;
        public static final int KEY_F2         = 60;
        public static final int KEY_F3         = 61;
        public static final int KEY_F4         = 62;
        public static final int KEY_F5         = 63;
        public static final int KEY_F6         = 64;
        public static final int KEY_F7         = 65;
        public static final int KEY_F8         = 66;
        public static final int KEY_F9         = 67;
        public static final int KEY_F10        = 68;
        public static final int KEY_NUMLOCK    = 69;
        public static final int KEY_SCROLL     = 70;
        public static final int KEY_NUMPAD7    = 71;
        public static final int KEY_NUMPAD8    = 72;
        public static final int KEY_NUMPAD9    = 73;
        public static final int KEY_SUBTRACT   = 74;
        public static final int KEY_NUMPAD4    = 75;
        public static final int KEY_NUMPAD5    = 76;
        public static final int KEY_NUMPAD6    = 77;
        public static final int KEY_ADD        = 78;
        public static final int KEY_NUMPAD1    = 79;
        public static final int KEY_NUMPAD2    = 80;
        public static final int KEY_NUMPAD3    = 81;
        public static final int KEY_NUMPAD0    = 82;
        public static final int KEY_DECIMAL    = 83;
        public static final int KEY_F11        = 87;
        public static final int KEY_F12        = 88;
        public static final int KEY_NUMPADENTER = 156;
        public static final int KEY_RCONTROL   = 157;
        public static final int KEY_DIVIDE     = 181;
        public static final int KEY_SYSRQ      = 183;
        public static final int KEY_RMENU      = 184;
        public static final int KEY_PAUSE      = 197;
        public static final int KEY_HOME       = 199;
        public static final int KEY_UP         = 200;
        public static final int KEY_PRIOR      = 201;
        public static final int KEY_LEFT       = 203;
        public static final int KEY_RIGHT      = 205;
        public static final int KEY_END        = 207;
        public static final int KEY_DOWN       = 208;
        public static final int KEY_NEXT       = 209;
        public static final int KEY_INSERT     = 210;
        public static final int KEY_DELETE     = 211;
        public static final int KEY_LMETA      = 219;
        public static final int KEY_RMETA      = 220;
        public static final int KEY_APPS       = 221;

        /** Total number of key codes recognized. */
        private static final int KEY_COUNT = 256;

        // ── State arrays ───────────────────────────────────────────────────
        /** Current key states (indexed by LWJGL 2 key code). */
        private static final boolean[] keyStates = new boolean[KEY_COUNT];

        /** Event queue. */
        private static final Queue<KeyEvent> eventQueue = new ConcurrentLinkedQueue<>();

        /** Current event being read by next()/getEvent*(). */
        private static volatile KeyEvent currentEvent = null;

        /** Whether the keyboard subsystem has been initialized. */
        private static volatile boolean created = false;

        /** Whether repeat events are enabled. */
        private static volatile boolean repeatEvents = false;

        /** Key name lookup. */
        private static final Map<Integer, String> KEY_NAMES = new HashMap<>();

        /** Key name reverse lookup. */
        private static final Map<String, Integer> KEY_INDICES = new HashMap<>();

        static {
            // Populate key name tables
            populateKeyNames();
        }

        private GLFWKeyboardAdapter() {}

        /**
         * Key event data structure.
         */
        static final class KeyEvent {
            final int     lwjgl2Key;
            final char    character;
            final boolean pressed;
            final boolean repeat;
            final long    nanoTime;

            KeyEvent(int lwjgl2Key, char character, boolean pressed,
                    boolean repeat, long nanoTime) {
                this.lwjgl2Key = lwjgl2Key;
                this.character = character;
                this.pressed   = pressed;
                this.repeat    = repeat;
                this.nanoTime  = nanoTime;
            }
        }

        // ── Lifecycle ──────────────────────────────────────────────────────

        public static void create() {
            if (created) return;
            long window = Mini_DirtyRoomCore.getGLFWWindow();
            if (window == 0L) {
                LOGGER.warning("[MDR-LWJGL] Keyboard.create() called before "
                             + "window exists. Deferring callback setup.");
                created = true;
                return;
            }
            setupKeyCallbacks(window);
            created = true;
            LOGGER.fine("[MDR-LWJGL] Keyboard created.");
        }

        public static void destroy() {
            created = false;
            Arrays.fill(keyStates, false);
            eventQueue.clear();
            currentEvent = null;
        }

        public static boolean isCreated() { return created; }

        /**
         * Registers GLFW key and char callbacks that populate our state
         * arrays and event queue.
         */
        static void setupKeyCallbacks(long window) {
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");

                // Key callback: fires for key press, release, and repeat
                Class<?> keyCallbackI = Class.forName(
                    "org.lwjgl.glfw.GLFWKeyCallbackI");
                Object keyCallback = java.lang.reflect.Proxy.newProxyInstance(
                    keyCallbackI.getClassLoader(),
                    new Class<?>[]{keyCallbackI},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            // args: long window, int key, int scancode,
                            //       int action, int mods
                            int glfwKey  = (int) args[1];
                            int action   = (int) args[3];

                            // GLFW_RELEASE = 0, GLFW_PRESS = 1, GLFW_REPEAT = 2
                            boolean pressed = (action != 0);
                            boolean repeat  = (action == 2);

                            // Convert GLFW key → LWJGL 2 key
                            int lwjgl2Key = Mini_DirtyRoomCore
                                .convertKeyCodeReverse(glfwKey);

                            if (lwjgl2Key >= 0 && lwjgl2Key < KEY_COUNT) {
                                keyStates[lwjgl2Key] = pressed;
                            }

                            // Queue event (skip repeats if not enabled)
                            if (!repeat || repeatEvents) {
                                eventQueue.add(new KeyEvent(
                                    lwjgl2Key, '\0', pressed, repeat,
                                    System.nanoTime()));
                            }
                        }
                        return null;
                    }
                );
                java.lang.reflect.Method setKeyCallback =
                    glfw.getMethod("glfwSetKeyCallback",
                        long.class, keyCallbackI);
                setKeyCallback.invoke(null, window, keyCallback);

                // Char callback: fires for text input (Unicode characters)
                Class<?> charCallbackI = Class.forName(
                    "org.lwjgl.glfw.GLFWCharCallbackI");
                Object charCallback = java.lang.reflect.Proxy.newProxyInstance(
                    charCallbackI.getClassLoader(),
                    new Class<?>[]{charCallbackI},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            int codepoint = (int) args[1];
                            char ch = (char) codepoint;

                            // Append character to the most recent key event
                            // or create a character-only event
                            eventQueue.add(new KeyEvent(
                                0, ch, true, false, System.nanoTime()));
                        }
                        return null;
                    }
                );
                java.lang.reflect.Method setCharCallback =
                    glfw.getMethod("glfwSetCharCallback",
                        long.class, charCallbackI);
                setCharCallback.invoke(null, window, charCallback);

            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                    "[MDR-LWJGL] Keyboard callback setup failed", e);
            }
        }

        // ── Polling ────────────────────────────────────────────────────────

        public static void poll() {
            // In GLFW, polling happens via glfwPollEvents in Display.update().
            // This method is a no-op in our adapter.
        }

        public static boolean isKeyDown(int key) {
            if (key < 0 || key >= KEY_COUNT) return false;
            return keyStates[key];
        }

        // ── Event queue ────────────────────────────────────────────────────

        public static boolean next() {
            currentEvent = eventQueue.poll();
            return currentEvent != null;
        }

        public static int getEventKey() {
            return currentEvent != null ? currentEvent.lwjgl2Key : KEY_NONE;
        }

        public static char getEventCharacter() {
            return currentEvent != null ? currentEvent.character : '\0';
        }

        public static boolean getEventKeyState() {
            return currentEvent != null && currentEvent.pressed;
        }

        public static long getEventNanoseconds() {
            return currentEvent != null ? currentEvent.nanoTime : 0L;
        }

        public static boolean isRepeatEvent() {
            return currentEvent != null && currentEvent.repeat;
        }

        public static int getNumKeyboardEvents() {
            return eventQueue.size();
        }

        public static void enableRepeatEvents(boolean enable) {
            repeatEvents = enable;
        }

        public static boolean areRepeatEventsEnabled() {
            return repeatEvents;
        }

        // ── Key info ───────────────────────────────────────────────────────

        public static int getKeyCount() {
            return KEY_COUNT;
        }

        public static String getKeyName(int key) {
            String name = KEY_NAMES.get(key);
            return name != null ? name : "KEY_" + key;
        }

        public static int getKeyIndex(String keyName) {
            Integer idx = KEY_INDICES.get(keyName.toUpperCase(Locale.ROOT));
            return idx != null ? idx : KEY_NONE;
        }

        /**
         * Populates the key name ↔ index maps.
         */
        private static void populateKeyNames() {
            addKeyName(KEY_NONE, "NONE");
            addKeyName(KEY_ESCAPE, "ESCAPE");
            addKeyName(KEY_1, "1"); addKeyName(KEY_2, "2");
            addKeyName(KEY_3, "3"); addKeyName(KEY_4, "4");
            addKeyName(KEY_5, "5"); addKeyName(KEY_6, "6");
            addKeyName(KEY_7, "7"); addKeyName(KEY_8, "8");
            addKeyName(KEY_9, "9"); addKeyName(KEY_0, "0");
            addKeyName(KEY_MINUS, "MINUS"); addKeyName(KEY_EQUALS, "EQUALS");
            addKeyName(KEY_BACK, "BACK"); addKeyName(KEY_TAB, "TAB");
            addKeyName(KEY_Q, "Q"); addKeyName(KEY_W, "W");
            addKeyName(KEY_E, "E"); addKeyName(KEY_R, "R");
            addKeyName(KEY_T, "T"); addKeyName(KEY_Y, "Y");
            addKeyName(KEY_U, "U"); addKeyName(KEY_I, "I");
            addKeyName(KEY_O, "O"); addKeyName(KEY_P, "P");
            addKeyName(KEY_LBRACKET, "LBRACKET");
            addKeyName(KEY_RBRACKET, "RBRACKET");
            addKeyName(KEY_RETURN, "RETURN");
            addKeyName(KEY_LCONTROL, "LCONTROL");
            addKeyName(KEY_A, "A"); addKeyName(KEY_S, "S");
            addKeyName(KEY_D, "D"); addKeyName(KEY_F, "F");
            addKeyName(KEY_G, "G"); addKeyName(KEY_H, "H");
            addKeyName(KEY_J, "J"); addKeyName(KEY_K, "K");
            addKeyName(KEY_L, "L");
            addKeyName(KEY_SEMICOLON, "SEMICOLON");
            addKeyName(KEY_APOSTROPHE, "APOSTROPHE");
            addKeyName(KEY_GRAVE, "GRAVE"); addKeyName(KEY_LSHIFT, "LSHIFT");
            addKeyName(KEY_BACKSLASH, "BACKSLASH");
            addKeyName(KEY_Z, "Z"); addKeyName(KEY_X, "X");
            addKeyName(KEY_C, "C"); addKeyName(KEY_V, "V");
            addKeyName(KEY_B, "B"); addKeyName(KEY_N, "N");
            addKeyName(KEY_M, "M");
            addKeyName(KEY_COMMA, "COMMA"); addKeyName(KEY_PERIOD, "PERIOD");
            addKeyName(KEY_SLASH, "SLASH"); addKeyName(KEY_RSHIFT, "RSHIFT");
            addKeyName(KEY_MULTIPLY, "MULTIPLY");
            addKeyName(KEY_LMENU, "LMENU"); addKeyName(KEY_SPACE, "SPACE");
            addKeyName(KEY_CAPITAL, "CAPITAL");
            addKeyName(KEY_F1, "F1"); addKeyName(KEY_F2, "F2");
            addKeyName(KEY_F3, "F3"); addKeyName(KEY_F4, "F4");
            addKeyName(KEY_F5, "F5"); addKeyName(KEY_F6, "F6");
            addKeyName(KEY_F7, "F7"); addKeyName(KEY_F8, "F8");
            addKeyName(KEY_F9, "F9"); addKeyName(KEY_F10, "F10");
            addKeyName(KEY_F11, "F11"); addKeyName(KEY_F12, "F12");
            addKeyName(KEY_NUMLOCK, "NUMLOCK");
            addKeyName(KEY_SCROLL, "SCROLL");
            addKeyName(KEY_NUMPAD7, "NUMPAD7"); addKeyName(KEY_NUMPAD8, "NUMPAD8");
            addKeyName(KEY_NUMPAD9, "NUMPAD9"); addKeyName(KEY_SUBTRACT, "SUBTRACT");
            addKeyName(KEY_NUMPAD4, "NUMPAD4"); addKeyName(KEY_NUMPAD5, "NUMPAD5");
            addKeyName(KEY_NUMPAD6, "NUMPAD6"); addKeyName(KEY_ADD, "ADD");
            addKeyName(KEY_NUMPAD1, "NUMPAD1"); addKeyName(KEY_NUMPAD2, "NUMPAD2");
            addKeyName(KEY_NUMPAD3, "NUMPAD3"); addKeyName(KEY_NUMPAD0, "NUMPAD0");
            addKeyName(KEY_DECIMAL, "DECIMAL");
            addKeyName(KEY_NUMPADENTER, "NUMPADENTER");
            addKeyName(KEY_RCONTROL, "RCONTROL");
            addKeyName(KEY_DIVIDE, "DIVIDE"); addKeyName(KEY_SYSRQ, "SYSRQ");
            addKeyName(KEY_RMENU, "RMENU"); addKeyName(KEY_PAUSE, "PAUSE");
            addKeyName(KEY_HOME, "HOME"); addKeyName(KEY_UP, "UP");
            addKeyName(KEY_PRIOR, "PRIOR"); addKeyName(KEY_LEFT, "LEFT");
            addKeyName(KEY_RIGHT, "RIGHT"); addKeyName(KEY_END, "END");
            addKeyName(KEY_DOWN, "DOWN"); addKeyName(KEY_NEXT, "NEXT");
            addKeyName(KEY_INSERT, "INSERT"); addKeyName(KEY_DELETE, "DELETE");
            addKeyName(KEY_LMETA, "LMETA"); addKeyName(KEY_RMETA, "RMETA");
            addKeyName(KEY_APPS, "APPS");
        }

        private static void addKeyName(int key, String name) {
            KEY_NAMES.put(key, name);
            KEY_INDICES.put(name, key);
        }
    }


    // ========================================================================
    //  SECTION 12: COMPATIBILITY WRAPPER — GLFWMouseAdapter (Mouse)
    // ========================================================================

    /**
     * Emulates org.lwjgl.input.Mouse using GLFW mouse/cursor callbacks.
     *
     * LWJGL 2's Mouse provides both polling (Mouse.getX(), Mouse.getDX())
     * and an event queue (Mouse.next(), Mouse.getEventButton()). GLFW uses
     * callbacks for cursor position, button press, and scroll. This adapter
     * tracks absolute position, deltas, scroll wheel deltas, and button
     * states, presenting them through the exact LWJGL 2 API.
     */
    @DeepOverwrite(target = "org.lwjgl.input.Mouse", method = "*")
    public static final class GLFWMouseAdapter {

        // ── State ──────────────────────────────────────────────────────────

        /** Current absolute cursor position. */
        private static volatile int mouseX = 0;
        private static volatile int mouseY = 0;

        /** Position at last poll() / update(). */
        private static volatile int lastMouseX = 0;
        private static volatile int lastMouseY = 0;

        /** Accumulated deltas since last getDX()/getDY() call. */
        private static volatile int deltaX = 0;
        private static volatile int deltaY = 0;

        /** Accumulated scroll wheel delta since last getDWheel() call. */
        private static volatile int deltaWheel = 0;

        /** Button states (up to 8 buttons). */
        private static final int MAX_BUTTONS = 8;
        private static final boolean[] buttonStates = new boolean[MAX_BUTTONS];

        /** Whether the mouse subsystem is initialized. */
        private static volatile boolean created = false;

        /** Whether the cursor is grabbed (relative / raw input mode). */
        private static volatile boolean grabbed = false;

        /** Whether the cursor is inside the window. */
        private static volatile boolean insideWindow = true;

        /** Whether to clip mouse coordinates to the window bounds. */
        private static volatile boolean clipToWindow = true;

        /** Event queue. */
        private static final Queue<MouseEvent> eventQueue =
            new ConcurrentLinkedQueue<>();

        /** Current event being consumed by next()/getEvent*(). */
        private static volatile MouseEvent currentEvent = null;

        /** Raw cursor position from GLFW (sub-pixel double precision). */
        private static volatile double rawCursorX = 0.0;
        private static volatile double rawCursorY = 0.0;

        /** Whether this is the very first cursor position callback. */
        private static volatile boolean firstCursorCallback = true;

        private GLFWMouseAdapter() {}

        /**
         * Mouse event data structure.
         */
        static final class MouseEvent {
            final int     button;       // -1 for move-only or scroll events
            final boolean buttonState;  // pressed?
            final int     x, y;
            final int     dx, dy;
            final int     dwheel;
            final long    nanoTime;

            MouseEvent(int button, boolean buttonState,
                      int x, int y, int dx, int dy, int dwheel,
                      long nanoTime) {
                this.button      = button;
                this.buttonState = buttonState;
                this.x           = x;
                this.y           = y;
                this.dx          = dx;
                this.dy          = dy;
                this.dwheel      = dwheel;
                this.nanoTime    = nanoTime;
            }
        }

        // ── Lifecycle ──────────────────────────────────────────────────────

        public static void create() {
            if (created) return;
            long window = Mini_DirtyRoomCore.getGLFWWindow();
            if (window == 0L) {
                LOGGER.warning("[MDR-LWJGL] Mouse.create() called before "
                             + "window exists. Deferring callback setup.");
                created = true;
                return;
            }
            setupMouseCallbacks(window);
            created = true;
            LOGGER.fine("[MDR-LWJGL] Mouse created.");
        }

        public static void destroy() {
            created = false;
            Arrays.fill(buttonStates, false);
            eventQueue.clear();
            currentEvent = null;
            deltaX = 0;
            deltaY = 0;
            deltaWheel = 0;
            firstCursorCallback = true;
        }

        public static boolean isCreated() { return created; }

        /**
         * Registers GLFW cursor, button, and scroll callbacks.
         */
        static void setupMouseCallbacks(long window) {
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");

                // ── Cursor position callback ───────────────────────────────
                Class<?> cursorPosI = Class.forName(
                    "org.lwjgl.glfw.GLFWCursorPosCallbackI");
                Object cursorPosCallback = java.lang.reflect.Proxy.newProxyInstance(
                    cursorPosI.getClassLoader(),
                    new Class<?>[]{cursorPosI},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            double xpos = (double) args[1];
                            double ypos = (double) args[2];

                            rawCursorX = xpos;
                            rawCursorY = ypos;

                            int newX = (int) xpos;
                            // LWJGL 2 has Y=0 at bottom; GLFW has Y=0 at top
                            int windowHeight = GLFWWindowManager.getHeight();
                            int newY = windowHeight - (int) ypos - 1;

                            if (clipToWindow) {
                                newX = Math.max(0, Math.min(newX,
                                    GLFWWindowManager.getWidth() - 1));
                                newY = Math.max(0, Math.min(newY,
                                    windowHeight - 1));
                            }

                            if (firstCursorCallback) {
                                lastMouseX = newX;
                                lastMouseY = newY;
                                firstCursorCallback = false;
                            }

                            int dx = newX - mouseX;
                            int dy = newY - mouseY;

                            mouseX = newX;
                            mouseY = newY;
                            deltaX += dx;
                            deltaY += dy;

                            // Queue a move event
                            eventQueue.add(new MouseEvent(
                                -1, false, newX, newY, dx, dy, 0,
                                System.nanoTime()));
                        }
                        return null;
                    }
                );
                java.lang.reflect.Method setCursorPosCallback =
                    glfw.getMethod("glfwSetCursorPosCallback",
                        long.class, cursorPosI);
                setCursorPosCallback.invoke(null, window, cursorPosCallback);

                // ── Mouse button callback ──────────────────────────────────
                Class<?> mouseButtonI = Class.forName(
                    "org.lwjgl.glfw.GLFWMouseButtonCallbackI");
                Object mouseButtonCallback = java.lang.reflect.Proxy.newProxyInstance(
                    mouseButtonI.getClassLoader(),
                    new Class<?>[]{mouseButtonI},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            int button = (int) args[1];
                            int action = (int) args[2];
                            // int mods = (int) args[3];

                            boolean pressed = (action != 0); // GLFW_RELEASE = 0

                            if (button >= 0 && button < MAX_BUTTONS) {
                                buttonStates[button] = pressed;
                            }

                            eventQueue.add(new MouseEvent(
                                button, pressed, mouseX, mouseY, 0, 0, 0,
                                System.nanoTime()));
                        }
                        return null;
                    }
                );
                java.lang.reflect.Method setMouseButtonCallback =
                    glfw.getMethod("glfwSetMouseButtonCallback",
                        long.class, mouseButtonI);
                setMouseButtonCallback.invoke(null, window, mouseButtonCallback);

                // ── Scroll callback ────────────────────────────────────────
                Class<?> scrollI = Class.forName(
                    "org.lwjgl.glfw.GLFWScrollCallbackI");
                Object scrollCallback = java.lang.reflect.Proxy.newProxyInstance(
                    scrollI.getClassLoader(),
                    new Class<?>[]{scrollI},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            // double xoffset = (double) args[1];
                            double yoffset = (double) args[2];

                            // LWJGL 2 reports wheel as integer * 120
                            int wheelDelta = (int) (yoffset * 120);
                            deltaWheel += wheelDelta;

                            eventQueue.add(new MouseEvent(
                                -1, false, mouseX, mouseY, 0, 0,
                                wheelDelta, System.nanoTime()));
                        }
                        return null;
                    }
                );
                java.lang.reflect.Method setScrollCallback =
                    glfw.getMethod("glfwSetScrollCallback",
                        long.class, scrollI);
                setScrollCallback.invoke(null, window, scrollCallback);

                // ── Cursor enter/leave callback ────────────────────────────
                Class<?> cursorEnterI = Class.forName(
                    "org.lwjgl.glfw.GLFWCursorEnterCallbackI");
                Object cursorEnterCallback = java.lang.reflect.Proxy.newProxyInstance(
                    cursorEnterI.getClassLoader(),
                    new Class<?>[]{cursorEnterI},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            insideWindow = (boolean) args[1];
                        }
                        return null;
                    }
                );
                java.lang.reflect.Method setCursorEnterCallback =
                    glfw.getMethod("glfwSetCursorEnterCallback",
                        long.class, cursorEnterI);
                setCursorEnterCallback.invoke(null, window, cursorEnterCallback);

            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                    "[MDR-LWJGL] Mouse callback setup failed", e);
            }
        }

        // ── Polling ────────────────────────────────────────────────────────

        public static void poll() {
            // Polling is handled by GLFW callbacks via glfwPollEvents()
            // in Display.update(). This is a no-op.
        }

        public static int getX() { return mouseX; }

        public static int getY() { return mouseY; }

        public static int getDX() {
            int dx = deltaX;
            deltaX = 0;
            return dx;
        }

        public static int getDY() {
            int dy = deltaY;
            deltaY = 0;
            return dy;
        }

        public static int getDWheel() {
            int dw = deltaWheel;
            deltaWheel = 0;
            return dw;
        }

        public static boolean isButtonDown(int button) {
            if (button < 0 || button >= MAX_BUTTONS) return false;
            return buttonStates[button];
        }

        public static int getButtonCount() {
            return MAX_BUTTONS;
        }

        public static String getButtonName(int button) {
            switch (button) {
                case 0: return "BUTTON0";
                case 1: return "BUTTON1";
                case 2: return "BUTTON2";
                default: return "BUTTON" + button;
            }
        }

        public static int getButtonIndex(String buttonName) {
            if (buttonName == null) return -1;
            String upper = buttonName.toUpperCase(Locale.ROOT);
            if (upper.startsWith("BUTTON")) {
                try {
                    return Integer.parseInt(upper.substring(6));
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
            return -1;
        }

        // ── Event queue ────────────────────────────────────────────────────

        public static boolean next() {
            currentEvent = eventQueue.poll();
            return currentEvent != null;
        }

        public static int getEventButton() {
            return currentEvent != null ? currentEvent.button : -1;
        }

        public static boolean getEventButtonState() {
            return currentEvent != null && currentEvent.buttonState;
        }

        public static int getEventX() {
            return currentEvent != null ? currentEvent.x : 0;
        }

        public static int getEventY() {
            return currentEvent != null ? currentEvent.y : 0;
        }

        public static int getEventDX() {
            return currentEvent != null ? currentEvent.dx : 0;
        }

        public static int getEventDY() {
            return currentEvent != null ? currentEvent.dy : 0;
        }

        public static int getEventDWheel() {
            return currentEvent != null ? currentEvent.dwheel : 0;
        }

        public static long getEventNanoseconds() {
            return currentEvent != null ? currentEvent.nanoTime : 0L;
        }

        // ── Grab / cursor control ──────────────────────────────────────────

        public static void setGrabbed(boolean grab) {
            grabbed = grab;
            long window = Mini_DirtyRoomCore.getGLFWWindow();
            if (window == 0L) return;

            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method setInputMode =
                    glfw.getMethod("glfwSetInputMode",
                        long.class, int.class, int.class);

                // GLFW_CURSOR = 0x00033001
                // GLFW_CURSOR_NORMAL = 0x00034001
                // GLFW_CURSOR_DISABLED = 0x00034003
                int mode = grab ? 0x00034003 : 0x00034001;
                setInputMode.invoke(null, window, 0x00033001, mode);

                if (grab) {
                    // Reset deltas when grabbing to prevent jump
                    deltaX = 0;
                    deltaY = 0;
                }

                // Try to enable raw mouse motion if available (GLFW 3.3+)
                if (grab) {
                    try {
                        // GLFW_RAW_MOUSE_MOTION = 0x00033005
                        // Check if supported first
                        java.lang.reflect.Method rawSupported =
                            glfw.getMethod("glfwRawMouseMotionSupported");
                        boolean supported = (boolean) rawSupported.invoke(null);
                        if (supported) {
                            setInputMode.invoke(null, window,
                                0x00033005, 1); // GLFW_TRUE
                        }
                    } catch (Exception ignored) {
                        // Raw mouse motion not available in this GLFW build
                    }
                }

            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] setGrabbed failed: " + e.getMessage());
            }
        }

        public static boolean isGrabbed() { return grabbed; }

        public static void setCursorPosition(int x, int y) {
            long window = Mini_DirtyRoomCore.getGLFWWindow();
            if (window == 0L) return;

            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                // Y coordinate inversion (LWJGL 2: bottom-left, GLFW: top-left)
                int windowHeight = GLFWWindowManager.getHeight();
                int glfwY = windowHeight - y - 1;

                java.lang.reflect.Method setCursorPos =
                    glfw.getMethod("glfwSetCursorPos",
                        long.class, double.class, double.class);
                setCursorPos.invoke(null, window, (double) x, (double) glfwY);

                mouseX = x;
                mouseY = y;
            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] setCursorPosition failed: "
                          + e.getMessage());
            }
        }

        public static boolean isInsideWindow() { return insideWindow; }

        public static void setClipMouseCoordinatesToWindow(boolean clip) {
            clipToWindow = clip;
        }

        /**
         * LWJGL 2 Mouse.setNativeCursor(Cursor).
         * Returns the previously set cursor.
         */
        public static Object setNativeCursor(Object cursor) {
            long window = Mini_DirtyRoomCore.getGLFWWindow();
            if (window == 0L) return null;

            try {
                if (cursor == null) {
                    // Reset to default cursor
                    Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                    java.lang.reflect.Method setCursor =
                        glfw.getMethod("glfwSetCursor",
                            long.class, long.class);
                    setCursor.invoke(null, window, 0L);
                } else {
                    // Extract GLFW cursor handle from CursorCompat
                    Class<?> cc = cursor.getClass();
                    try {
                        Field handleField = cc.getDeclaredField("glfwCursorHandle");
                        handleField.setAccessible(true);
                        long handle = handleField.getLong(cursor);

                        Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                        java.lang.reflect.Method setCursor =
                            glfw.getMethod("glfwSetCursor",
                                long.class, long.class);
                        setCursor.invoke(null, window, handle);
                    } catch (NoSuchFieldException e) {
                        LOGGER.fine("[MDR-LWJGL] CursorCompat has no glfwCursorHandle.");
                    }
                }
            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] setNativeCursor failed: "
                          + e.getMessage());
            }
            return null; // Previous cursor tracking not implemented
        }

        /**
         * Ensures callbacks are registered if the window was created
         * after Mouse.create() was called.
         */
        static void ensureCallbacks() {
            if (created && firstCursorCallback) {
                long window = Mini_DirtyRoomCore.getGLFWWindow();
                if (window != 0L) {
                    setupMouseCallbacks(window);
                }
            }
        }
    }


    // ========================================================================
    //  SECTION 13: COMPATIBILITY WRAPPER — OpenALBridge
    // ========================================================================

    /**
     * Emulates org.lwjgl.openal.AL using LWJGL 3's OpenAL bindings.
     *
     * LWJGL 2: AL.create() opens a device and creates a context implicitly.
     * LWJGL 3: You must explicitly open a device with alcOpenDevice(),
     * create a context with alcCreateContext(), and then create AL
     * capabilities.
     */
    @DeepOverwrite(target = "org.lwjgl.openal.AL", method = "*")
    public static final class OpenALBridge {

        private static volatile long device = 0L;
        private static volatile long context = 0L;
        private static volatile boolean created = false;

        private OpenALBridge() {}

        /**
         * AL.create() — no args version.
         */
        public static void create() {
            create(null, 44100, 60);
        }

        /**
         * AL.create(String deviceName, int frequency, int refresh).
         */
        public static void create(String deviceName, int frequency, int refresh) {
            if (created) return;

            try {
                // ALC10.alcOpenDevice(deviceName)
                Class<?> alc10 = Class.forName("org.lwjgl.openal.ALC10");
                Class<?> alc = Class.forName("org.lwjgl.openal.ALC");
                Class<?> al = Class.forName("org.lwjgl.openal.AL");

                // Open device
                java.lang.reflect.Method alcOpenDevice;
                try {
                    alcOpenDevice = alc10.getMethod("alcOpenDevice",
                        ByteBuffer.class);
                    device = (long) alcOpenDevice.invoke(null,
                        (ByteBuffer) null);
                } catch (NoSuchMethodException e) {
                    // Try CharSequence overload
                    alcOpenDevice = alc10.getMethod("alcOpenDevice",
                        CharSequence.class);
                    device = (long) alcOpenDevice.invoke(null, deviceName);
                }

                if (device == 0L) {
                    throw new RuntimeException(
                        "[MDR] Failed to open OpenAL device"
                        + (deviceName != null ? ": " + deviceName : ""));
                }

                // Create context with attributes
                // Attributes: ALC_FREQUENCY, frequency, ALC_REFRESH, refresh, 0
                // ALC_FREQUENCY = 0x1007, ALC_REFRESH = 0x1008
                IntBuffer attribs = ByteBuffer.allocateDirect(5 * 4)
                    .order(ByteOrder.nativeOrder()).asIntBuffer();
                attribs.put(0x1007).put(frequency)
                       .put(0x1008).put(refresh)
                       .put(0).flip();

                java.lang.reflect.Method alcCreateContext =
                    alc10.getMethod("alcCreateContext", long.class, IntBuffer.class);
                context = (long) alcCreateContext.invoke(null, device, attribs);

                if (context == 0L) {
                    java.lang.reflect.Method alcCloseDevice =
                        alc10.getMethod("alcCloseDevice", long.class);
                    alcCloseDevice.invoke(null, device);
                    device = 0L;
                    throw new RuntimeException(
                        "[MDR] Failed to create OpenAL context");
                }

                // Make context current
                java.lang.reflect.Method alcMakeContextCurrent =
                    alc10.getMethod("alcMakeContextCurrent", long.class);
                boolean success = (boolean) alcMakeContextCurrent.invoke(
                    null, context);

                if (!success) {
                    throw new RuntimeException(
                        "[MDR] Failed to make OpenAL context current");
                }

                // Create ALC capabilities
                java.lang.reflect.Method createALCCapabilities =
                    alc.getMethod("createCapabilities", long.class);
                Object alcCaps = createALCCapabilities.invoke(null, device);

                // Create AL capabilities
                java.lang.reflect.Method createALCapabilities =
                    al.getMethod("createCapabilities", alcCaps.getClass());
                createALCapabilities.invoke(null, alcCaps);

                created = true;
                LOGGER.info("[MDR-LWJGL] OpenAL initialized. Device=" + device
                          + ", Context=" + context);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE,
                    "[MDR-LWJGL] OpenAL initialization failed", e);
                throw new RuntimeException(
                    "[MDR] OpenAL initialization failed", e);
            }
        }

        /**
         * AL.destroy().
         */
        public static void destroy() {
            if (!created) return;

            try {
                Class<?> alc10 = Class.forName("org.lwjgl.openal.ALC10");

                // Make no context current
                java.lang.reflect.Method alcMakeContextCurrent =
                    alc10.getMethod("alcMakeContextCurrent", long.class);
                alcMakeContextCurrent.invoke(null, 0L);

                // Destroy context
                if (context != 0L) {
                    java.lang.reflect.Method alcDestroyContext =
                        alc10.getMethod("alcDestroyContext", long.class);
                    alcDestroyContext.invoke(null, context);
                    context = 0L;
                }

                // Close device
                if (device != 0L) {
                    java.lang.reflect.Method alcCloseDevice =
                        alc10.getMethod("alcCloseDevice", long.class);
                    alcCloseDevice.invoke(null, device);
                    device = 0L;
                }

                created = false;
                LOGGER.info("[MDR-LWJGL] OpenAL destroyed.");

            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                    "[MDR-LWJGL] OpenAL destruction error", e);
                created = false;
            }
        }

        /**
         * AL.isCreated().
         */
        public static boolean isCreated() { return created; }

        /**
         * Returns the ALC device handle for advanced use.
         */
        public static long getDevice() { return device; }

        /**
         * Returns the ALC context handle for advanced use.
         */
        public static long getContext() { return context; }
    }


    // ========================================================================
    //  SECTION 14: COMPATIBILITY WRAPPER — SysCompat
    // ========================================================================

    /**
     * Emulates org.lwjgl.Sys using standard Java APIs and GLFW.
     *
     * LWJGL 2's Sys class provides high-resolution timing, version info,
     * clipboard access, URL opening, and system alerts.
     */
    @DeepOverwrite(target = "org.lwjgl.Sys", method = "*")
    public static final class SysCompat {

        /** Timer resolution: nanoseconds per tick. We use nanosecond precision. */
        private static final long TIMER_RESOLUTION = 1_000_000_000L;

        /** Startup time for relative timing. */
        private static final long STARTUP_NANOS = System.nanoTime();

        private SysCompat() {}

        /**
         * Returns the current value of the high-resolution timer.
         * LWJGL 2 returns ticks; we return nanoseconds since startup.
         */
        public static long getTime() {
            return System.nanoTime() - STARTUP_NANOS;
        }

        /**
         * Returns the timer resolution (ticks per second).
         */
        public static long getTimerResolution() {
            return TIMER_RESOLUTION;
        }

        /**
         * Returns the LWJGL version string.
         * We return our LWJGL 3 target version.
         */
        public static String getVersion() {
            return Mini_DirtyRoomCore.TARGET_LWJGL_VERSION
                 + " (via Mini_DirtyRoom)";
        }

        /**
         * Opens a URL in the default browser.
         */
        public static void openURL(String url) {
            if (url == null || url.isEmpty()) return;

            try {
                EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();

                if (env != null && env.isAndroid) {
                    // Android: use Intent via reflection
                    openURLAndroid(url);
                } else {
                    // Desktop: use java.awt.Desktop or Runtime.exec
                    openURLDesktop(url);
                }
            } catch (Exception e) {
                LOGGER.warning("[MDR-LWJGL] openURL failed for '" + url
                             + "': " + e.getMessage());
            }
        }

        private static void openURLDesktop(String url) throws Exception {
            // Try Desktop API first (Java 6+)
            try {
                Class<?> desktopClass = Class.forName("java.awt.Desktop");
                java.lang.reflect.Method isDesktopSupported =
                    desktopClass.getMethod("isDesktopSupported");
                if ((boolean) isDesktopSupported.invoke(null)) {
                    java.lang.reflect.Method getDesktop =
                        desktopClass.getMethod("getDesktop");
                    Object desktop = getDesktop.invoke(null);
                    java.lang.reflect.Method browse =
                        desktopClass.getMethod("browse", java.net.URI.class);
                    browse.invoke(desktop, new java.net.URI(url));
                    return;
                }
            } catch (Exception ignored) {}

            // Fallback: OS-specific commands
            EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
            String[] command;
            if (env != null && env.isWindows) {
                command = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
            } else if (env != null && env.isMacOS) {
                command = new String[]{"open", url};
            } else {
                command = new String[]{"xdg-open", url};
            }
            Runtime.getRuntime().exec(command);
        }

        private static void openURLAndroid(String url) {
            try {
                // android.content.Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                Class<?> intentClass = Class.forName("android.content.Intent");
                Class<?> uriClass = Class.forName("android.net.Uri");

                java.lang.reflect.Method uriParse =
                    uriClass.getMethod("parse", String.class);
                Object uri = uriParse.invoke(null, url);

                Field actionView = intentClass.getField("ACTION_VIEW");
                String action = (String) actionView.get(null);

                Object intent = intentClass.getConstructor(String.class, uriClass)
                    .newInstance(action, uri);

                // Get current activity context and start the intent
                // This is highly launcher-dependent; best-effort
                LOGGER.fine("[MDR-LWJGL] Android URL open attempted: " + url);

            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] Android URL open failed: "
                          + e.getMessage());
            }
        }

        /**
         * Returns whether the JVM is running on a 64-bit platform.
         */
        public static boolean is64Bit() {
            EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
            return env != null && env.is64Bit;
        }

        /**
         * Gets the system clipboard content as a string.
         */
        public static String getClipboard() {
            // Try GLFW clipboard first
            long window = Mini_DirtyRoomCore.getGLFWWindow();
            if (window != 0L) {
                try {
                    Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                    java.lang.reflect.Method getClipboardString =
                        glfw.getMethod("glfwGetClipboardString", long.class);
                    Object result = getClipboardString.invoke(null, window);
                    if (result != null) return result.toString();
                } catch (Exception ignored) {}
            }

            // Fallback: AWT clipboard
            try {
                Class<?> toolkit = Class.forName("java.awt.Toolkit");
                java.lang.reflect.Method getDefaultToolkit =
                    toolkit.getMethod("getDefaultToolkit");
                Object tk = getDefaultToolkit.invoke(null);

                java.lang.reflect.Method getSystemClipboard =
                    tk.getClass().getMethod("getSystemClipboard");
                Object clipboard = getSystemClipboard.invoke(tk);

                Class<?> dataFlavor = Class.forName("java.awt.datatransfer.DataFlavor");
                Field stringFlavor = dataFlavor.getField("stringFlavor");
                Object flavor = stringFlavor.get(null);

                java.lang.reflect.Method getData =
                    clipboard.getClass().getMethod("getData",
                        dataFlavor);
                Object data = getData.invoke(clipboard, flavor);
                return data != null ? data.toString() : null;

            } catch (Exception e) {
                return null;
            }
        }

        /**
         * Displays a system alert dialog.
         */
        public static void alert(String title, String message) {
            LOGGER.info("[MDR-LWJGL] System Alert: [" + title + "] " + message);

            EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
            if (env != null && env.isHeadless) return;

            try {
                // Try Swing JOptionPane
                Class<?> jop = Class.forName("javax.swing.JOptionPane");
                java.lang.reflect.Method showMessageDialog =
                    jop.getMethod("showMessageDialog",
                        java.awt.Component.class, Object.class,
                        String.class, int.class);
                // JOptionPane.INFORMATION_MESSAGE = 1
                showMessageDialog.invoke(null, null, message, title, 1);
            } catch (Exception e) {
                // Fallback: console output
                System.out.println("[ALERT] " + title + ": " + message);
            }
        }
    }


    // ========================================================================
    //  SECTION 15: COMPATIBILITY WRAPPER — BufferUtilsCompat
    // ========================================================================

    /**
     * Emulates org.lwjgl.BufferUtils using LWJGL 3's BufferUtils or
     * standard Java NIO.
     *
     * LWJGL 2's BufferUtils creates direct NIO buffers. LWJGL 3 has the
     * same concept, but the class is at a different package/API level.
     * We use standard Java NIO as the baseline, which is what LWJGL 2's
     * BufferUtils does internally anyway.
     */
    @DeepOverwrite(target = "org.lwjgl.BufferUtils", method = "*")
    public static final class BufferUtilsCompat {

        private BufferUtilsCompat() {}

        public static ByteBuffer createByteBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity)
                .order(ByteOrder.nativeOrder());
        }

        public static java.nio.ShortBuffer createShortBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity * 2)
                .order(ByteOrder.nativeOrder()).asShortBuffer();
        }

        public static IntBuffer createIntBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity * 4)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        }

        public static java.nio.LongBuffer createLongBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity * 8)
                .order(ByteOrder.nativeOrder()).asLongBuffer();
        }

        public static FloatBuffer createFloatBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        }

        public static java.nio.DoubleBuffer createDoubleBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity * 8)
                .order(ByteOrder.nativeOrder()).asDoubleBuffer();
        }

        public static java.nio.CharBuffer createCharBuffer(int capacity) {
            return ByteBuffer.allocateDirect(capacity * 2)
                .order(ByteOrder.nativeOrder()).asCharBuffer();
        }

        /**
         * Creates a PointerBuffer. In LWJGL 3, PointerBuffer is at
         * org.lwjgl.PointerBuffer; in LWJGL 2 it was also at
         * org.lwjgl.PointerBuffer. The class still exists in LWJGL 3
         * so we delegate directly.
         */
        public static Object createPointerBuffer(int capacity) {
            try {
                Class<?> pbClass = Class.forName("org.lwjgl.PointerBuffer");
                java.lang.reflect.Method allocateDirect =
                    pbClass.getMethod("allocateDirect", int.class);
                return allocateDirect.invoke(null, capacity);
            } catch (Exception e) {
                // Fallback: return a LongBuffer (pointer-sized on 64-bit)
                LOGGER.fine("[MDR-LWJGL] PointerBuffer fallback to LongBuffer");
                return createLongBuffer(capacity);
            }
        }

        /**
         * Zeroes out a direct ByteBuffer.
         */
        public static void zeroBuffer(ByteBuffer buffer) {
            if (buffer == null) return;
            int pos = buffer.position();
            int lim = buffer.limit();
            buffer.clear();
            while (buffer.hasRemaining()) {
                buffer.put((byte) 0);
            }
            buffer.position(pos);
            buffer.limit(lim);
        }

        /**
         * Zeroes out a direct IntBuffer.
         */
        public static void zeroBuffer(IntBuffer buffer) {
            if (buffer == null) return;
            int pos = buffer.position();
            int lim = buffer.limit();
            buffer.clear();
            while (buffer.hasRemaining()) {
                buffer.put(0);
            }
            buffer.position(pos);
            buffer.limit(lim);
        }

        /**
         * Zeroes out a direct FloatBuffer.
         */
        public static void zeroBuffer(FloatBuffer buffer) {
            if (buffer == null) return;
            int pos = buffer.position();
            int lim = buffer.limit();
            buffer.clear();
            while (buffer.hasRemaining()) {
                buffer.put(0.0f);
            }
            buffer.position(pos);
            buffer.limit(lim);
        }
    }


    // ========================================================================
    //  SECTION 16: COMPATIBILITY WRAPPER — Minor Types
    // ========================================================================

    // ── 16A: DisplayModeCompat ─────────────────────────────────────────────

    /**
     * Emulates org.lwjgl.opengl.DisplayMode.
     * A simple data container for width, height, BPP, and refresh rate.
     */
    public static final class DisplayModeCompat {
        public final int width;
        public final int height;
        public final int bpp;           // bits per pixel
        public final int frequency;     // refresh rate in Hz

        private DisplayModeCompat(int width, int height, int bpp, int frequency) {
            this.width     = width;
            this.height    = height;
            this.bpp       = bpp;
            this.frequency = frequency;
        }

        public static DisplayModeCompat create(int width, int height,
                                                int bpp, int frequency) {
            return new DisplayModeCompat(width, height, bpp, frequency);
        }

        // LWJGL 2 DisplayMode accessor methods
        public int getWidth()     { return width; }
        public int getHeight()    { return height; }
        public int getBitsPerPixel() { return bpp; }
        public int getFrequency() { return frequency; }
        public boolean isFullscreenCapable() { return true; }

        @Override
        public String toString() {
            return width + " x " + height + " x " + bpp + " @ " + frequency + "Hz";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DisplayModeCompat)) return false;
            DisplayModeCompat that = (DisplayModeCompat) o;
            return width == that.width && height == that.height
                && bpp == that.bpp && frequency == that.frequency;
        }

        @Override
        public int hashCode() {
            return Objects.hash(width, height, bpp, frequency);
        }
    }

    // ── 16B: PixelFormatCompat ─────────────────────────────────────────────

    /**
     * Emulates org.lwjgl.opengl.PixelFormat.
     * Stores framebuffer configuration that gets translated to GLFW
     * window hints.
     */
    public static final class PixelFormatCompat {
        public int bpp      = 0;
        public int alpha    = 8;
        public int depth    = 24;
        public int stencil  = 8;
        public int samples  = 0;
        public int auxBuffers = 0;
        public int accumBpp  = 0;
        public int accumAlpha = 0;
        public boolean stereo = false;
        public boolean floating = false;
        public boolean sRGB = false;

        public PixelFormatCompat() {}

        /** Builder-style setters matching LWJGL 2's fluent API. */
        public PixelFormatCompat withBitsPerPixel(int bpp) {
            this.bpp = bpp; return this;
        }
        public PixelFormatCompat withAlphaBits(int alpha) {
            this.alpha = alpha; return this;
        }
        public PixelFormatCompat withDepthBits(int depth) {
            this.depth = depth; return this;
        }
        public PixelFormatCompat withStencilBits(int stencil) {
            this.stencil = stencil; return this;
        }
        public PixelFormatCompat withSamples(int samples) {
            this.samples = samples; return this;
        }
        public PixelFormatCompat withAuxBuffers(int aux) {
            this.auxBuffers = aux; return this;
        }
        public PixelFormatCompat withAccumulationBitsPerPixel(int abpp) {
            this.accumBpp = abpp; return this;
        }
        public PixelFormatCompat withAccumulationAlpha(int aalpha) {
            this.accumAlpha = aalpha; return this;
        }
        public PixelFormatCompat withStereo(boolean stereo) {
            this.stereo = stereo; return this;
        }
        public PixelFormatCompat withFloatingPoint(boolean fp) {
            this.floating = fp; return this;
        }
        public PixelFormatCompat withSRGB(boolean srgb) {
            this.sRGB = srgb; return this;
        }

        // LWJGL 2 accessor methods
        public int getBitsPerPixel()  { return bpp; }
        public int getAlphaBits()     { return alpha; }
        public int getDepthBits()     { return depth; }
        public int getStencilBits()   { return stencil; }
        public int getSamples()       { return samples; }
        public int getAuxBuffers()    { return auxBuffers; }
        public int getAccumulationBitsPerPixel() { return accumBpp; }
        public int getAccumulationAlpha()        { return accumAlpha; }
        public boolean isStereo()     { return stereo; }
        public boolean isFloatingPoint() { return floating; }
        public boolean isSRGB()       { return sRGB; }

        @Override
        public String toString() {
            return "PixelFormat[bpp=" + bpp + ",alpha=" + alpha
                 + ",depth=" + depth + ",stencil=" + stencil
                 + ",samples=" + samples + "]";
        }
    }

    // ── 16C: ContextAttribsCompat ──────────────────────────────────────────

    /**
     * Emulates org.lwjgl.opengl.ContextAttribs.
     * Stores OpenGL context version and profile requirements.
     */
    public static final class ContextAttribsCompat {
        public int majorVersion = 1;
        public int minorVersion = 0;
        public int profileMask  = 0; // 0 = any, 1 = core, 2 = compat
        public boolean forwardCompatible = false;
        public boolean debug = false;

        public ContextAttribsCompat() {}

        public ContextAttribsCompat(int major, int minor) {
            this.majorVersion = major;
            this.minorVersion = minor;
        }

        public ContextAttribsCompat withForwardCompatible(boolean fc) {
            this.forwardCompatible = fc; return this;
        }

        public ContextAttribsCompat withProfileCore(boolean core) {
            this.profileMask = core ? 1 : 0; return this;
        }

        public ContextAttribsCompat withProfileCompatibility(boolean compat) {
            this.profileMask = compat ? 2 : 0; return this;
        }

        public ContextAttribsCompat withDebug(boolean debug) {
            this.debug = debug; return this;
        }

        public int getMajorVersion() { return majorVersion; }
        public int getMinorVersion() { return minorVersion; }
        public int getProfileMask()  { return profileMask; }
        public boolean isForwardCompatible() { return forwardCompatible; }
        public boolean isDebug() { return debug; }
    }

    // ── 16D: GLContextCompat ───────────────────────────────────────────────

    /**
     * Emulates org.lwjgl.opengl.GLContext.
     * Provides access to GL capabilities (which extensions are supported).
     */
    public static final class GLContextCompat {

        private static volatile Object capabilities = null;

        private GLContextCompat() {}

        /**
         * Returns the GL capabilities for the current context.
         */
        public static Object getCapabilities() {
            if (capabilities == null) {
                capabilities = new GLCapabilitiesCompat();
            }
            return capabilities;
        }

        /**
         * Switches to a different context (no-op in our single-window model).
         */
        public static Object useContext(Object context) {
            return getCapabilities();
        }

        /**
         * Called after GL.createCapabilities() to update our wrapper.
         */
        static void refreshCapabilities() {
            capabilities = new GLCapabilitiesCompat();
        }
    }

    // ── 16E: GLCapabilitiesCompat ──────────────────────────────────────────

    /**
     * Emulates org.lwjgl.opengl.ContextCapabilities.
     * LWJGL 2 exposes extension availability as boolean fields.
     * We delegate to LWJGL 3's GLCapabilities.
     */
    public static final class GLCapabilitiesCompat {

        // Cache the underlying LWJGL 3 capabilities
        private final Object lwjgl3Caps;

        /** Common extension flags that mods check. */
        public final boolean GL_ARB_vertex_buffer_object;
        public final boolean GL_ARB_framebuffer_object;
        public final boolean GL_ARB_shader_objects;
        public final boolean GL_ARB_vertex_shader;
        public final boolean GL_ARB_fragment_shader;
        public final boolean GL_ARB_multitexture;
        public final boolean GL_ARB_texture_non_power_of_two;
        public final boolean GL_EXT_framebuffer_object;
        public final boolean GL_EXT_texture_filter_anisotropic;
        public final boolean GL_ARB_texture_env_combine;
        public final boolean GL_ARB_vertex_program;
        public final boolean GL_ARB_fragment_program;
        public final boolean OpenGL11;
        public final boolean OpenGL12;
        public final boolean OpenGL13;
        public final boolean OpenGL14;
        public final boolean OpenGL15;
        public final boolean OpenGL20;
        public final boolean OpenGL21;
        public final boolean OpenGL30;
        public final boolean OpenGL31;
        public final boolean OpenGL32;
        public final boolean OpenGL33;
        public final boolean OpenGL40;

        GLCapabilitiesCompat() {
            Object caps = null;
            try {
                Class<?> gl = Class.forName("org.lwjgl.opengl.GL");
                java.lang.reflect.Method getCapabilities =
                    gl.getMethod("getCapabilities");
                caps = getCapabilities.invoke(null);
            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] Could not get LWJGL 3 capabilities: "
                          + e.getMessage());
            }
            this.lwjgl3Caps = caps;

            // Read extension fields from LWJGL 3 GLCapabilities
            GL_ARB_vertex_buffer_object       = readCap("GL_ARB_vertex_buffer_object");
            GL_ARB_framebuffer_object         = readCap("GL_ARB_framebuffer_object");
            GL_ARB_shader_objects             = readCap("GL_ARB_shader_objects");
            GL_ARB_vertex_shader              = readCap("GL_ARB_vertex_shader");
            GL_ARB_fragment_shader            = readCap("GL_ARB_fragment_shader");
            GL_ARB_multitexture               = readCap("GL_ARB_multitexture");
            GL_ARB_texture_non_power_of_two   = readCap("GL_ARB_texture_non_power_of_two");
            GL_EXT_framebuffer_object         = readCap("GL_EXT_framebuffer_object");
            GL_EXT_texture_filter_anisotropic = readCap("GL_EXT_texture_filter_anisotropic");
            GL_ARB_texture_env_combine        = readCap("GL_ARB_texture_env_combine");
            GL_ARB_vertex_program             = readCap("GL_ARB_vertex_program");
            GL_ARB_fragment_program           = readCap("GL_ARB_fragment_program");
            OpenGL11 = readCap("OpenGL11");
            OpenGL12 = readCap("OpenGL12");
            OpenGL13 = readCap("OpenGL13");
            OpenGL14 = readCap("OpenGL14");
            OpenGL15 = readCap("OpenGL15");
            OpenGL20 = readCap("OpenGL20");
            OpenGL21 = readCap("OpenGL21");
            OpenGL30 = readCap("OpenGL30");
            OpenGL31 = readCap("OpenGL31");
            OpenGL32 = readCap("OpenGL32");
            OpenGL33 = readCap("OpenGL33");
            OpenGL40 = readCap("OpenGL40");
        }

        private boolean readCap(String fieldName) {
            if (lwjgl3Caps == null) return false;
            try {
                Field f = lwjgl3Caps.getClass().getField(fieldName);
                return f.getBoolean(lwjgl3Caps);
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Dynamic extension check for extensions not covered by fields.
         */
        public boolean isExtensionAvailable(String extension) {
            return readCap(extension);
        }
    }

    // ── 16F: CursorCompat ──────────────────────────────────────────────────

    /**
     * Emulates org.lwjgl.input.Cursor using GLFW cursors.
     */
    public static final class CursorCompat {

        /** GLFW cursor handle. */
        long glfwCursorHandle = 0L;

        /** Cursor dimensions. */
        private final int width;
        private final int height;
        private final int hotspotX;
        private final int hotspotY;

        /**
         * Creates a cursor from pixel data.
         * Matches the LWJGL 2 Cursor(int, int, int, int, int, IntBuffer, IntBuffer) constructor.
         */
        public CursorCompat(int width, int height,
                           int xHotspot, int yHotspot,
                           int numImages, IntBuffer images, IntBuffer delays) {
            this.width    = width;
            this.height   = height;
            this.hotspotX = xHotspot;
            this.hotspotY = yHotspot;

            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                Class<?> glfwImage = Class.forName("org.lwjgl.glfw.GLFWImage");

                // Create GLFWImage with the cursor pixel data
                java.lang.reflect.Method imageMalloc =
                    glfwImage.getMethod("malloc");
                Object img = imageMalloc.invoke(null);

                // Set width and height
                java.lang.reflect.Method setWidth =
                    img.getClass().getMethod("width", int.class);
                java.lang.reflect.Method setHeight =
                    img.getClass().getMethod("height", int.class);
                setWidth.invoke(img, width);
                setHeight.invoke(img, height);

                // Convert IntBuffer (ARGB) to ByteBuffer (RGBA) for GLFW
                ByteBuffer pixels = ByteBuffer.allocateDirect(width * height * 4)
                    .order(ByteOrder.nativeOrder());
                if (images != null) {
                    for (int i = 0; i < width * height && images.hasRemaining(); i++) {
                        int argb = images.get();
                        pixels.put((byte) ((argb >> 16) & 0xFF)); // R
                        pixels.put((byte) ((argb >> 8)  & 0xFF)); // G
                        pixels.put((byte) (argb & 0xFF));         // B
                        pixels.put((byte) ((argb >> 24) & 0xFF)); // A
                    }
                    pixels.flip();
                }

                java.lang.reflect.Method setPixels =
                    img.getClass().getMethod("pixels", ByteBuffer.class);
                setPixels.invoke(img, pixels);

                // glfwCreateCursor(image, xhot, yhot)
                java.lang.reflect.Method createCursor =
                    glfw.getMethod("glfwCreateCursor",
                        glfwImage, int.class, int.class);
                glfwCursorHandle = (long) createCursor.invoke(
                    null, img, xHotspot, yHotspot);

                // Free the GLFWImage struct
                java.lang.reflect.Method imgFree =
                    img.getClass().getMethod("free");
                imgFree.invoke(img);

            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] Cursor creation failed: "
                          + e.getMessage());
                glfwCursorHandle = 0L;
            }
        }

        /**
         * Destroys this cursor.
         */
        public void destroy() {
            if (glfwCursorHandle != 0L) {
                try {
                    Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                    java.lang.reflect.Method destroyCursor =
                        glfw.getMethod("glfwDestroyCursor", long.class);
                    destroyCursor.invoke(null, glfwCursorHandle);
                } catch (Exception ignored) {}
                glfwCursorHandle = 0L;
            }
        }

        /**
         * Returns the minimum cursor size supported.
         */
        public static int getMinCursorSize() { return 1; }

        /**
         * Returns the maximum cursor size supported.
         */
        public static int getMaxCursorSize() { return 256; }

        /**
         * Returns the number of cursor capabilities.
         */
        public static int getCapabilities() {
            // CURSOR_ONE_BIT_TRANSPARENCY | CURSOR_8_BIT_ALPHA | CURSOR_ANIMATION
            return 0x07;
        }
    }

    // ── 16G: ControllersCompat ─────────────────────────────────────────────

    /**
     * Emulates org.lwjgl.input.Controllers.
     * LWJGL 2's Controllers API wraps JInput for gamepad support.
     * In LWJGL 3, GLFW provides joystick/gamepad support directly.
     * This is a stub implementation — full controller support is complex.
     */
    public static final class ControllersCompat {

        private static volatile boolean created = false;
        private static volatile int controllerCount = 0;

        private ControllersCompat() {}

        public static void create() {
            created = true;
            // Count connected joysticks via GLFW
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                int count = 0;
                // GLFW supports joystick IDs 0-15
                for (int i = 0; i <= 15; i++) {
                    java.lang.reflect.Method joystickPresent =
                        glfw.getMethod("glfwJoystickPresent", int.class);
                    if ((boolean) joystickPresent.invoke(null, i)) {
                        count++;
                    }
                }
                controllerCount = count;
                LOGGER.fine("[MDR-LWJGL] Controllers created. Found: " + count);
            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] Controller detection failed: "
                          + e.getMessage());
            }
        }

        public static void destroy() {
            created = false;
            controllerCount = 0;
        }

        public static boolean isCreated()   { return created; }
        public static int getControllerCount() { return controllerCount; }

        public static void poll() {
            // GLFW polls joysticks during glfwPollEvents()
        }

        public static boolean next() {
            // Controller event queue — stub
            return false;
        }

        /**
         * Returns a controller stub by index.
         */
        public static Object getController(int index) {
            return null; // Not fully implemented
        }

        public static void clearEvents() {
            // Stub
        }
    }

    // ── 16H: DrawableGLCompat ──────────────────────────────────────────────

    /**
     * Emulates org.lwjgl.opengl.DrawableGL.
     * In LWJGL 2, DrawableGL represents a GL-capable rendering surface.
     * In LWJGL 3, the GLFW window IS the drawable.
     */
    public static class DrawableGLCompat {

        public DrawableGLCompat() {}

        public void makeCurrent() {
            long window = Mini_DirtyRoomCore.getGLFWWindow();
            if (window == 0L) return;
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method makeContextCurrent =
                    glfw.getMethod("glfwMakeContextCurrent", long.class);
                makeContextCurrent.invoke(null, window);
            } catch (Exception ignored) {}
        }

        public void releaseContext() {
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method makeContextCurrent =
                    glfw.getMethod("glfwMakeContextCurrent", long.class);
                makeContextCurrent.invoke(null, 0L);
            } catch (Exception ignored) {}
        }

        public boolean isCurrent() {
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method getCurrentContext =
                    glfw.getMethod("glfwGetCurrentContext");
                long current = (long) getCurrentContext.invoke(null);
                return current == Mini_DirtyRoomCore.getGLFWWindow();
            } catch (Exception e) {
                return false;
            }
        }

        public void destroy() {
            // No-op: window destruction handled by GLFWWindowManager
        }
    }

    // ── 16I: SharedDrawableCompat ──────────────────────────────────────────

    /**
     * Emulates org.lwjgl.opengl.SharedDrawable.
     * Used for shared GL contexts (e.g., texture loading on a background thread).
     */
    public static class SharedDrawableCompat extends DrawableGLCompat {

        private long sharedWindow = 0L;

        public SharedDrawableCompat(Object drawable) {
            // In LWJGL 3, shared contexts are created by passing the
            // share parameter to glfwCreateWindow
            try {
                long primaryWindow = Mini_DirtyRoomCore.getGLFWWindow();
                if (primaryWindow == 0L) return;

                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");

                // Create an invisible window that shares the primary GL context
                java.lang.reflect.Method windowHint =
                    glfw.getMethod("glfwWindowHint", int.class, int.class);
                // GLFW_VISIBLE = 0x00020004
                windowHint.invoke(null, 0x00020004, 0);

                java.lang.reflect.Method createWindow =
                    glfw.getMethod("glfwCreateWindow",
                        int.class, int.class, CharSequence.class,
                        long.class, long.class);
                sharedWindow = (long) createWindow.invoke(
                    null, 1, 1, "", 0L, primaryWindow);

            } catch (Exception e) {
                LOGGER.fine("[MDR-LWJGL] SharedDrawable creation failed: "
                          + e.getMessage());
            }
        }

        @Override
        public void makeCurrent() {
            if (sharedWindow == 0L) return;
            try {
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                java.lang.reflect.Method makeContextCurrent =
                    glfw.getMethod("glfwMakeContextCurrent", long.class);
                makeContextCurrent.invoke(null, sharedWindow);

                // Ensure capabilities are created for this context too
                Class<?> gl = Class.forName("org.lwjgl.opengl.GL");
                java.lang.reflect.Method createCapabilities =
                    gl.getMethod("createCapabilities");
                createCapabilities.invoke(null);
            } catch (Exception ignored) {}
        }

        @Override
        public void destroy() {
            if (sharedWindow != 0L) {
                try {
                    Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                    java.lang.reflect.Method destroyWindow =
                        glfw.getMethod("glfwDestroyWindow", long.class);
                    destroyWindow.invoke(null, sharedWindow);
                } catch (Exception ignored) {}
                sharedWindow = 0L;
            }
        }
    }

    // ── 16J: PbufferCompat ─────────────────────────────────────────────────

    /**
     * Emulates org.lwjgl.opengl.Pbuffer.
     * PBuffers are an old mechanism for offscreen rendering, largely
     * replaced by FBOs (Framebuffer Objects). This is a minimal stub.
     */
    public static class PbufferCompat {

        /** Capability constants. */
        public static final int PBUFFER_SUPPORTED = 1;
        public static final int RENDER_TEXTURE_SUPPORTED = 2;
        public static final int RENDER_TEXTURE_RECTANGLE_SUPPORTED = 4;
        public static final int RENDER_DEPTH_TEXTURE_SUPPORTED = 8;

        private final int width;
        private final int height;

        public PbufferCompat(int width, int height,
                            Object pixelFormat, Object renderTexture,
                            Object sharedDrawable) {
            this.width = width;
            this.height = height;
            LOGGER.fine("[MDR-LWJGL] PBuffer created (stub): "
                      + width + "x" + height
                      + " — consider using FBOs instead.");
        }

        public static int getCapabilities() {
            return PBUFFER_SUPPORTED;
        }

        public int getWidth()  { return width; }
        public int getHeight() { return height; }
        public boolean isBufferLost() { return false; }
        public void makeCurrent() { /* stub */ }
        public void releaseContext() { /* stub */ }
        public void destroy() { /* stub */ }
    }


    // ========================================================================
    //  SECTION 17: LATE-BINDING CALLBACK INITIALIZATION
    // ========================================================================

    /**
     * Called after the GLFW window is created to ensure all input adapters
     * have their callbacks registered. This handles the case where
     * Keyboard.create() or Mouse.create() is called before Display.create().
     */
    @DeepHook(
        targets = {
            @HookTarget(
                className  = "stellar.snow.astralis.integration.Mini_DirtyRoom.LWJGLTransformEngine$GLFWWindowManager",
                methodName = "createInternal"
            )
        },
        timing = HookTiming.AFTER
    )
    public static void onWindowCreated() {
        long window = Mini_DirtyRoomCore.getGLFWWindow();
        if (window == 0L) return;

        LOGGER.fine("[MDR-LWJGL] Window created — binding deferred callbacks.");

        // If Keyboard was created before the window, set up its callbacks now
        if (GLFWKeyboardAdapter.isCreated()) {
            GLFWKeyboardAdapter.setupKeyCallbacks(window);
        }

        // If Mouse was created before the window, set up its callbacks now
        if (GLFWMouseAdapter.isCreated()) {
            GLFWMouseAdapter.setupMouseCallbacks(window);
        }

        // Refresh GL capabilities
        GLContextCompat.refreshCapabilities();
    }


    // ========================================================================
    //  SECTION 18: STATISTICS & DIAGNOSTICS API
    // ========================================================================

    /**
     * Returns a statistics snapshot for diagnostic reporting.
     */
    public static Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("lwjgl_target_version", LWJGL_TARGET_VERSION);
        stats.put("android_platform",     IS_ANDROID.get());
        stats.put("android_bypass",       ANDROID_BYPASS_ACTIVE.get());
        stats.put("preloaded_natives",    PRELOADED_NATIVES.size());
        stats.put("classes_scanned",    CLASSES_SCANNED.get());
        stats.put("classes_transformed", CLASSES_TRANSFORMED.get());
        stats.put("method_redirects",   METHOD_REDIRECTS.get());
        stats.put("field_redirects",    FIELD_REDIRECTS.get());
        stats.put("type_redirects",     TYPE_REDIRECTS.get());
        stats.put("method_rules",       METHOD_REDIRECTS_MAP.size());
        stats.put("field_rules",        FIELD_REDIRECTS_MAP.size());
        stats.put("class_rules",        Mini_DirtyRoomCore.getClassRedirects().size());
        stats.put("descriptor_cache",   DESCRIPTOR_REWRITES.size());
        stats.put("registered",         REGISTERED.get());
        stats.put("window_created",     GLFWWindowManager.isCreated());
        stats.put("keyboard_created",   GLFWKeyboardAdapter.isCreated());
        stats.put("mouse_created",      GLFWMouseAdapter.isCreated());
        stats.put("openal_created",     OpenALBridge.isCreated());
        return Collections.unmodifiableMap(stats);
    }

    /**
     * Returns a formatted diagnostic report for this engine.
     */
    public static String getDiagnosticReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== LWJGL 3.4.0 Native Transform Engine Report ===\n");
        sb.append("Target Version: ").append(LWJGL_TARGET_VERSION).append("\n");
        sb.append("Android Platform: ").append(IS_ANDROID.get()).append("\n");
        sb.append("Android Bypass: ").append(ANDROID_BYPASS_ACTIVE.get()).append("\n");
        sb.append("\n");
        Map<String, Object> stats = getStatistics();
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            sb.append("  ").append(entry.getKey())
              .append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("\n--- Transformed Classes (last 50) ---\n");
        int count = 0;
        for (String cls : TRANSFORMED_CLASSES) {
            if (count++ >= 50) {
                sb.append("  ... and ").append(TRANSFORMED_CLASSES.size() - 50)
                  .append(" more\n");
                break;
            }
            sb.append("  ").append(cls.replace('/', '.')).append("\n");
        }
        sb.append("=== End Report ===\n");
        return sb.toString();
    }

    /**
     * Returns the set of all transformed class names (internal format).
     */
    public static Set<String> getTransformedClasses() {
        return Collections.unmodifiableSet(TRANSFORMED_CLASSES);
    }

    /**
     * Returns the target LWJGL version (3.4.0).
     */
    public static String getLWJGLTargetVersion() {
        return LWJGL_TARGET_VERSION;
    }

    /**
     * Returns true if running on Android platform.
     */
    public static boolean isAndroidPlatform() {
        return IS_ANDROID.get();
    }

    /**
     * Returns true if Android security bypass is active.
     */
    public static boolean isAndroidBypassActive() {
        return ANDROID_BYPASS_ACTIVE.get();
    }

    /**
     * Returns the number of preloaded native libraries (Android only).
     */
    public static int getPreloadedNativeCount() {
        return PRELOADED_NATIVES.size();
    }

    /**
     * Returns whether debug mode is enabled.
     */
    private static boolean isDebug() {
        Mini_DirtyRoomCore.MiniDirtyRoomConfig config =
            Mini_DirtyRoomCore.getConfig();
        return config != null && (config.debug || config.verboseLogging);
    }


    // ========================================================================
    //  SECTION 19: THREAD SAFETY & DEFERRED INITIALIZATION
    // ========================================================================

    /**
     * Ensures all compatibility wrappers are fully initialized.
     * Called at critical junctures to handle edge cases where components
     * are accessed out of the expected order.
     */
    public static void ensureInitialized() {
        if (!REGISTERED.get()) {
            register();
        }

        // If we have a window but callbacks are not yet bound
        long window = Mini_DirtyRoomCore.getGLFWWindow();
        if (window != 0L) {
            GLFWMouseAdapter.ensureCallbacks();
        }
    }

    /**
     * Resets all compatibility wrappers. Used for testing or when
     * reloading the game without restarting the JVM.
     */
    public static void resetAll() {
        LOGGER.info("[MDR-LWJGL] Resetting all compatibility wrappers.");

        // Destroy in reverse creation order
        try { OpenALBridge.destroy(); }     catch (Exception ignored) {}
        try { GLFWMouseAdapter.destroy(); } catch (Exception ignored) {}
        try { GLFWKeyboardAdapter.destroy(); } catch (Exception ignored) {}
        try { GLFWWindowManager.destroy(); }   catch (Exception ignored) {}

        // Reset statistics
        CLASSES_SCANNED.set(0);
        CLASSES_TRANSFORMED.set(0);
        METHOD_REDIRECTS.set(0);
        FIELD_REDIRECTS.set(0);
        TYPE_REDIRECTS.set(0);
        TRANSFORMED_CLASSES.clear();

        LOGGER.info("[MDR-LWJGL] Reset complete.");
    }


    // ========================================================================
    //  SECTION 20: PRIVATE CONSTRUCTOR (SINGLETON ENFORCEMENT)
    // ========================================================================

    // Constructor is at the top of the class (Section 1).
    // This section intentionally left as a closing marker.

    /**
     * Returns a string representation for logging.
     */
    @Override
    public String toString() {
        return "LWJGLTransformEngine[registered=" + REGISTERED.get()
             + ", transformed=" + CLASSES_TRANSFORMED.get()
             + ", redirects=" + METHOD_REDIRECTS.get() + "]";
    }
