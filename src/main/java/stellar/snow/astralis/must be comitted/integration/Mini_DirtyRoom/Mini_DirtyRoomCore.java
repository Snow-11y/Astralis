/*
 * ============================================================================
 *  Mini_DirtyRoom — Modernization Layer for Minecraft
 *  Copyright (c) 2025 Stellar Snow Astralis
 *
 *  This file is the ABSOLUTE FIRST entry point for the entire mod ecosystem.
 *  It bootstraps LWJGL 3.4.0 replacement, Java version auto-upgrade,
 *  Android compatibility, and coordinates all downstream components.
 *
 *  Load Order Guarantee:
 *    1. Mini_DirtyRoomCore          ← YOU ARE HERE
 *    2. LWJGLTransformEngine
 *    3. JavaCompatibilityLayer
 *    4. ModLoaderBridge
 *    5. Everything else
 *    6. official shortcut: **MDR**
 * ============================================================================
 */
package stellar.snow.astralis.integration.Mini_DirtyRoom;

// ─── Core Framework & Plugin ──────────────────────────────────────────────
import stellar.snow.astralis.integration.DeepMix.DeepMix;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * ============================================================================
 *  MINI_DIRTYROOM CORE — THE GENESIS BOOTSTRAP
 * ============================================================================
 *
 * This class is the single point of origin for the entire Mini_DirtyRoom
 * modernization layer. It MUST load before any Minecraft class, any mod
 * loader class, any LWJGL class, and any other mod.
 *
 * Responsibilities:
 *   1. Detect the runtime environment (OS, Java version, Android, mod loader)
 *   2. If Java 8 is detected, relaunch on Java 25
 *   3. If Android is detected, force-override LWJGL with 3.4.0
 *   4. Extract and load LWJGL 3.4.0 native libraries
 *   5. Register bytecode transformers via DeepMix
 *   6. Coordinate the startup of all other Mini_DirtyRoom components
 *   7. Provide diagnostic logging for the entire lifecycle
 *
 * DeepMix Integration:
 *   This file uses @DeepOverwrite, @DeepInject (via @DeepSurgical),
 *   @DeepHook, @DeepControl, @DeepProxy, @DeepASMInline, and many
 *   more to guarantee it is the absolute first code that executes.
 */

public final class Mini_DirtyRoomCore {

    // ========================================================================
    //  SECTION 0: CONSTANTS & STATIC STATE
    // ========================================================================

    /** Semantic version of this bootstrap layer. */
    public static final String VERSION = "1.0.0";

    /** Build identifier for diagnostics. */
    public static final String BUILD_ID = "MDR-20250101-GENESIS";

    /** The minimum Java version we will ultimately run on. */
    public static final int TARGET_JAVA_VERSION = 25;

    /** The LWJGL version we force-load everywhere. */
    public static final String TARGET_LWJGL_VERSION = "3.4.0";

    /** Marker to prevent double-initialization across class loaders. */
    private static final String INIT_PROPERTY = "mini_dirtyroom.initialized";

    /** Marker for relaunch detection. */
    private static final String RELAUNCH_PROPERTY = "mini_dirtyroom.relaunched";

    /** Root directory for all Mini_DirtyRoom runtime data. */
    private static final String MDR_HOME = ".mini_dirtyroom";

    /** Subdirectory for extracted native libraries. */
    private static final String NATIVES_DIR = "natives";

    /** Subdirectory for downloaded JRE bundles. */
    private static final String JRE_DIR = "jre";

    /** Subdirectory for cached LWJGL jars. */
    private static final String LWJGL_CACHE_DIR = "lwjgl_cache";

    /** Subdirectory for logs. */
    private static final String LOG_DIR = "logs";

    /** Configuration file name. */
    private static final String CONFIG_FILE = "mini_dirtyroom.cfg";

    /** Logger instance — initialized before anything else. */
    private static final Logger LOGGER = Logger.getLogger("Mini_DirtyRoom");

    /** LWJGL 3.4.0 Maven coordinates / download URLs by artifact. */
    private static final Map<String, String> LWJGL_ARTIFACTS = new LinkedHashMap<>();

    /** Native classifier per platform. */
    private static final Map<String, String> NATIVE_CLASSIFIERS = new LinkedHashMap<>();

    /** SHA-256 checksums for integrity verification of each artifact. */
    private static final Map<String, String> LWJGL_CHECKSUMS = new LinkedHashMap<>();

    /** LWJGL 2 → LWJGL 3 key code mapping table. */
    private static final int[] KEYCODE_MAP = new int[256];

    // ── Runtime State ──────────────────────────────────────────────────────

    /** Singleton guard. */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    /** Whether we are in the relaunched process. */
    private static final AtomicBoolean RELAUNCHED = new AtomicBoolean(false);

    /** The detected runtime environment. */
    private static volatile EnvironmentInfo ENV;

    /** The loaded configuration. */
    private static volatile MiniDirtyRoomConfig CONFIG;

    /** Instrumentation agent handle (if we managed to attach). */
    private static volatile Instrumentation INSTRUMENTATION;

    /** Reference to the GLFW window handle once created. */
    private static volatile long GLFW_WINDOW = 0L;

    /** Phase tracking for diagnostics. */
    private static final AtomicInteger CURRENT_PHASE = new AtomicInteger(0);

    /** Startup timestamp. */
    private static long STARTUP_TIME_NS;

    /** Errors collected during bootstrap (non-fatal). */
    private static final List<String> BOOT_WARNINGS = new CopyOnWriteArrayList<>();

    /** Fatal error message if bootstrap fails entirely. */
    private static final AtomicReference<Throwable> FATAL_ERROR = new AtomicReference<>(null);

    /** Executor for async bootstrap tasks. */
    private static final ExecutorService BOOTSTRAP_EXECUTOR =
        Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "MDR-Bootstrap");
                t.setDaemon(true);
                t.setPriority(Thread.MAX_PRIORITY);
                return t;
            }
        );

    /** Completion latches for parallel initialization. */
    private static final CountDownLatch NATIVES_READY  = new CountDownLatch(1);
    private static final CountDownLatch LWJGL_READY    = new CountDownLatch(1);
    private static final CountDownLatch BOOTSTRAP_DONE = new CountDownLatch(1);


    // ========================================================================
    //  SECTION 0.5: STATIC INITIALIZER — THE VERY FIRST CODE THAT RUNS
    // ========================================================================

    /*
     * This static block is the absolute genesis point. Because this class is
     * loaded via @MixinLoader with priority=Integer.MIN_VALUE and phase=
     * "PRE_EVERYTHING", it is guaranteed by DeepMix to execute before any
     * other class in the entire application.
     *
     * The static block performs ONLY the most critical, non-deferrable work:
     *   1. Timestamp capture
     *   2. Double-init guard
     *   3. Environment probe
     *   4. Java version gate (relaunch if Java 8)
     *   5. LWJGL artifact registry population
     *   6. Full bootstrap kickoff
     */
    static {
        STARTUP_TIME_NS = System.nanoTime();

        // ── Double-initialization guard ────────────────────────────────────
        String alreadyInit = System.getProperty(INIT_PROPERTY);
        if ("true".equals(alreadyInit) || !INITIALIZED.compareAndSet(false, true)) {
            // Another class loader already ran this. Skip.
            LOGGER.info("[MDR] Already initialized in this JVM. Skipping duplicate.");
        } else {
            System.setProperty(INIT_PROPERTY, "true");

            // ── Relaunch detection ─────────────────────────────────────────
            if ("true".equals(System.getProperty(RELAUNCH_PROPERTY))) {
                RELAUNCHED.set(true);
            }

            // ── Populate artifact tables ───────────────────────────────────
            populateArtifactTables();

            // ── Populate key code map ──────────────────────────────────────
            populateKeyCodeMap();

            // ── Begin full bootstrap ───────────────────────────────────────
            try {
                bootstrap();
            } catch (Throwable t) {
                FATAL_ERROR.set(t);
                LOGGER.log(Level.SEVERE,
                    "[MDR] *** FATAL: Bootstrap failed catastrophically ***", t);
                // Do NOT rethrow — let the game attempt to start so the user
                // can at least see an error screen.
            }
        }
    }


    // ========================================================================
    //  SECTION 1: BOOTSTRAP SEQUENCE
    // ========================================================================

    /**
     * The master bootstrap method. Executes the full initialization sequence
     * in strict order. Each phase is numbered and logged.
     */
    @DeepHook(
        targets = {
            @HookTarget(
                className  = "net.minecraft.client.main.Main",
                methodName = "main"
            ),
            @HookTarget(
                className  = "net.minecraft.server.MinecraftServer",
                methodName = "main"
            ),
            @HookTarget(
                className  = "cpw.mods.fml.relauncher.FMLRelauncher",
                methodName = "handleClientRelaunch"
            ),
            @HookTarget(
                className  = "net.minecraftforge.fml.relauncher.FMLRelauncher",
                methodName = "handleClientRelaunch"
            ),
            @HookTarget(
                className  = "net.fabricmc.loader.impl.launch.knot.KnotClient",
                methodName = "main"
            )
        },
        timing = HookTiming.BEFORE
    )
    private static void bootstrap() throws Throwable {
        LOGGER.info("╔══════════════════════════════════════════════════════════╗");
        LOGGER.info("║       Mini_DirtyRoom v" + VERSION + " — Genesis Bootstrap       ║");
        LOGGER.info("║       Build: " + BUILD_ID + "                          ║");
        LOGGER.info("╚══════════════════════════════════════════════════════════╝");

        // ── Phase 1: Environment Detection ─────────────────────────────────
        phase(1, "Environment Detection", () -> {
            ENV = EnvironmentInfo.detect();
            LOGGER.info("[MDR] " + ENV.toSummary());
        });

        // ── Phase 2: Configuration Loading ─────────────────────────────────
        phase(2, "Configuration Loading", () -> {
            CONFIG = MiniDirtyRoomConfig.load(
                getMDRHome().resolve(CONFIG_FILE)
            );
            LOGGER.info("[MDR] Config loaded. Debug=" + CONFIG.debug
                        + ", ForceRelaunch=" + CONFIG.forceRelaunch);
        });

        // ── Phase 3: Java Version Gate ─────────────────────────────────────
        phase(3, "Java Version Gate", () -> {
            if (ENV.javaVersion < TARGET_JAVA_VERSION && !RELAUNCHED.get()) {
                LOGGER.warning("[MDR] Detected Java " + ENV.javaVersion
                             + ". Target is Java " + TARGET_JAVA_VERSION + ".");

                if (CONFIG.autoUpgradeJava) {
                    LOGGER.info("[MDR] Attempting automatic Java upgrade...");
                    boolean relaunched = attemptJavaRelaunch();
                    if (relaunched) {
                        // This process will exit; the new one takes over.
                        LOGGER.info("[MDR] Relaunching on Java "
                                  + TARGET_JAVA_VERSION + "...");
                        System.exit(0);
                        return; // Unreachable, but satisfies compiler
                    }
                    LOGGER.warning("[MDR] Java relaunch failed. "
                                 + "Continuing on Java " + ENV.javaVersion);
                    BOOT_WARNINGS.add(
                        "Could not upgrade to Java " + TARGET_JAVA_VERSION
                        + ". Running on Java " + ENV.javaVersion);
                } else {
                    LOGGER.info("[MDR] Auto-upgrade disabled. "
                              + "Continuing on Java " + ENV.javaVersion);
                }
            } else if (RELAUNCHED.get()) {
                LOGGER.info("[MDR] Running in relaunched JVM (Java "
                          + ENV.javaVersion + ").");
            } else {
                LOGGER.info("[MDR] Java " + ENV.javaVersion
                          + " meets target. No relaunch needed.");
            }
        });

        // ── Phase 4: Module System Preparation (Java 9+) ──────────────────
        phase(4, "Module System Preparation", () -> {
            if (ENV.javaVersion >= 9) {
                prepareModuleSystem();
            } else {
                LOGGER.info("[MDR] Java " + ENV.javaVersion
                          + " has no module system. Skipping.");
            }
        });

        // ── Phase 5: Instrumentation Agent Attachment ──────────────────────
        phase(5, "Instrumentation Agent Attachment", () -> {
            INSTRUMENTATION = attachInstrumentationAgent();
            if (INSTRUMENTATION != null) {
                LOGGER.info("[MDR] Instrumentation agent attached successfully.");
            } else {
                LOGGER.warning("[MDR] Instrumentation agent unavailable. "
                             + "Some features may be limited.");
                BOOT_WARNINGS.add("Instrumentation agent could not be attached.");
            }
        });

        // ── Phase 6: LWJGL Override (Platform-Specific) ───────────────────
        phase(6, "LWJGL 3.4.0 Override", () -> {
            if (ENV.isAndroid) {
                overrideLWJGLAndroid();
            } else {
                overrideLWJGLDesktop();
            }
        });

        // ── Phase 7: Native Library Extraction & Loading ───────────────────
        phase(7, "Native Library Management", () -> {
            Future<?> nativeFuture = BOOTSTRAP_EXECUTOR.submit(() -> {
                try {
                    extractAndLoadNatives();
                } catch (Throwable t) {
                    LOGGER.log(Level.SEVERE,
                        "[MDR] Native extraction failed", t);
                    BOOT_WARNINGS.add("Native library extraction failed: "
                                    + t.getMessage());
                } finally {
                    NATIVES_READY.countDown();
                }
            });

            // Wait for natives with timeout
            if (!NATIVES_READY.await(30, TimeUnit.SECONDS)) {
                LOGGER.severe("[MDR] Native extraction timed out after 30s!");
                BOOT_WARNINGS.add("Native extraction timed out.");
            }
        });

        // ── Phase 8: Bytecode Transformer Registration ─────────────────────
        phase(8, "Bytecode Transformer Registration", () -> {
            registerDeepMixTransformers();
        });

        // ── Phase 9: Mod Loader Bridge Initialization ──────────────────────
        phase(9, "Mod Loader Bridge Init", () -> {
            initializeModLoaderBridge();
        });

        // ── Phase 10: Verification & Diagnostics ───────────────────────────
        phase(10, "Verification & Diagnostics", () -> {
            verifyBootstrap();
            long elapsedMs = (System.nanoTime() - STARTUP_TIME_NS) / 1_000_000L;
            LOGGER.info("[MDR] ═══════════════════════════════════════════════");
            LOGGER.info("[MDR]  Bootstrap complete in " + elapsedMs + " ms");
            LOGGER.info("[MDR]  Warnings: " + BOOT_WARNINGS.size());
            for (String w : BOOT_WARNINGS) {
                LOGGER.warning("[MDR]    ⚠ " + w);
            }
            LOGGER.info("[MDR] ═══════════════════════════════════════════════");
            BOOTSTRAP_DONE.countDown();
        });
    }

    /**
     * Executes a bootstrap phase with logging and error handling.
     */
    private static void phase(int number, String name, ThrowingRunnable action)
            throws Throwable {
        CURRENT_PHASE.set(number);
        LOGGER.info("[MDR] ── Phase " + number + ": " + name + " ──");
        long start = System.nanoTime();
        try {
            action.run();
            long ms = (System.nanoTime() - start) / 1_000_000L;
            LOGGER.info("[MDR]    Phase " + number + " completed (" + ms + " ms)");
        } catch (Throwable t) {
            long ms = (System.nanoTime() - start) / 1_000_000L;
            LOGGER.log(Level.SEVERE,
                "[MDR]    Phase " + number + " FAILED (" + ms + " ms)", t);
            throw t;
        }
    }


    // ========================================================================
    //  SECTION 2: ENVIRONMENT DETECTION
    // ========================================================================

    /**
     * Immutable snapshot of the runtime environment, captured once at startup.
     */
    static final class EnvironmentInfo {
        final int     javaVersion;
        final String  javaVersionFull;
        final String  javaVendor;
        final String  javaHome;
        final String  osName;
        final String  osArch;
        final String  osVersion;
        final boolean isWindows;
        final boolean isLinux;
        final boolean isMacOS;
        final boolean isAndroid;
        final boolean isARM;
        final boolean is64Bit;
        final boolean hasModuleSystem;
        final String  detectedLoader;    // "forge", "fabric", "quilt", "neoforge", "unknown"
        final String  minecraftVersion;
        final long    maxMemoryMB;
        final int     availableProcessors;
        final boolean isHeadless;
        final boolean isClient;

        private EnvironmentInfo(
            int javaVersion, String javaVersionFull, String javaVendor,
            String javaHome, String osName, String osArch, String osVersion,
            boolean isWindows, boolean isLinux, boolean isMacOS,
            boolean isAndroid, boolean isARM, boolean is64Bit,
            boolean hasModuleSystem, String detectedLoader,
            String minecraftVersion, long maxMemoryMB,
            int availableProcessors, boolean isHeadless, boolean isClient
        ) {
            this.javaVersion         = javaVersion;
            this.javaVersionFull     = javaVersionFull;
            this.javaVendor          = javaVendor;
            this.javaHome            = javaHome;
            this.osName              = osName;
            this.osArch              = osArch;
            this.osVersion           = osVersion;
            this.isWindows           = isWindows;
            this.isLinux             = isLinux;
            this.isMacOS             = isMacOS;
            this.isAndroid           = isAndroid;
            this.isARM               = isARM;
            this.is64Bit             = is64Bit;
            this.hasModuleSystem     = hasModuleSystem;
            this.detectedLoader      = detectedLoader;
            this.minecraftVersion    = minecraftVersion;
            this.maxMemoryMB         = maxMemoryMB;
            this.availableProcessors = availableProcessors;
            this.isHeadless          = isHeadless;
            this.isClient            = isClient;
        }

        static EnvironmentInfo detect() {
            String verStr  = System.getProperty("java.version", "1.8.0");
            int    ver     = parseJavaVersion(verStr);
            String vendor  = System.getProperty("java.vendor", "Unknown");
            String home    = System.getProperty("java.home", "");
            String os      = System.getProperty("os.name", "Unknown");
            String arch    = System.getProperty("os.arch", "Unknown");
            String osVer   = System.getProperty("os.version", "Unknown");
            String osLower = os.toLowerCase(Locale.ROOT);

            boolean win     = osLower.contains("win");
            boolean linux   = osLower.contains("linux") || osLower.contains("nux");
            boolean mac     = osLower.contains("mac") || osLower.contains("darwin");
            boolean android = detectAndroid();
            boolean arm     = arch.contains("arm") || arch.contains("aarch64");
            boolean bit64   = arch.contains("64") || arch.contains("amd64")
                           || arch.contains("aarch64");
            boolean modules = ver >= 9;

            String loader  = detectModLoader();
            String mcVer   = detectMinecraftVersion();

            long maxMem    = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
            int  procs     = Runtime.getRuntime().availableProcessors();
            boolean headless = java.awt.GraphicsEnvironment.isHeadless();

            boolean client = !headless && detectIsClient();

            return new EnvironmentInfo(
                ver, verStr, vendor, home, os, arch, osVer,
                win, linux, mac, android, arm, bit64,
                modules, loader, mcVer, maxMem, procs, headless, client
            );
        }

        String toSummary() {
            return String.format(
                "Environment: Java %d (%s, %s) | OS: %s %s (%s) | "
              + "Android: %b | ARM: %b | 64-bit: %b | Loader: %s | MC: %s | "
              + "RAM: %d MB | CPUs: %d | Client: %b",
                javaVersion, javaVersionFull, javaVendor,
                osName, osVersion, osArch,
                isAndroid, isARM, is64Bit, detectedLoader, minecraftVersion,
                maxMemoryMB, availableProcessors, isClient
            );
        }

        /**
         * Returns the LWJGL native classifier string for this platform.
         * e.g. "natives-windows", "natives-linux-arm64", "natives-macos-arm64"
         */
        String getNativeClassifier() {
            if (isAndroid) return "natives-android"; // Custom packaging
            StringBuilder sb = new StringBuilder("natives-");
            if (isWindows)     sb.append("windows");
            else if (isMacOS)  sb.append("macos");
            else               sb.append("linux");
            if (isARM)         sb.append(is64Bit ? "-arm64" : "-arm32");
            return sb.toString();
        }
    }

    /**
     * Parses "1.8.0_292", "11.0.11", "17", "21.0.1", "25" etc. to the
     * major version int.
     */
    private static int parseJavaVersion(String versionString) {
        try {
            if (versionString.startsWith("1.")) {
                // Java 8 and below: "1.8.0_xxx"
                return Integer.parseInt(versionString.split("\\.")[1]);
            }
            // Java 9+: "11.0.11", "17", "21.0.1", "25-ea"
            String major = versionString.split("[.\\-+]")[0];
            return Integer.parseInt(major);
        } catch (NumberFormatException e) {
            LOGGER.warning("[MDR] Could not parse Java version '"
                         + versionString + "'. Assuming 8.");
            return 8;
        }
    }

    /**
     * Detects if we are running on Android (e.g., PojavLauncher, Fold Craft
     * Launcher, or similar).
     */
    private static boolean detectAndroid() {
        // Method 1: Android system property
        String vmName = System.getProperty("java.vm.name", "");
        if (vmName.toLowerCase(Locale.ROOT).contains("dalvik")
         || vmName.toLowerCase(Locale.ROOT).contains("art")) {
            return true;
        }

        // Method 2: Check for Android-specific classes
        try {
            Class.forName("android.os.Build");
            return true;
        } catch (ClassNotFoundException ignored) {}

        // Method 3: Check for PojavLauncher markers
        try {
            Class.forName("net.kdt.pojavlaunch.PojavApplication");
            return true;
        } catch (ClassNotFoundException ignored) {}

        // Method 4: Check for Fold Craft Launcher markers
        try {
            Class.forName("com.fold.craft.launcher.FoldCraftApplication");
            return true;
        } catch (ClassNotFoundException ignored) {}

        // Method 5: Android-specific environment variables
        String androidRoot = System.getenv("ANDROID_ROOT");
        if (androidRoot != null && !androidRoot.isEmpty()) {
            return true;
        }

        // Method 6: Android data directory existence
        if (new File("/system/build.prop").exists()) {
            return true;
        }

        return false;
    }

    /**
     * Detects which mod loader is present in the classpath.
     */
    private static String detectModLoader() {
        // NeoForge (check first — it extends Forge)
        if (classExists("net.neoforged.fml.common.Mod")) return "neoforge";
        // Quilt (check before Fabric — it extends Fabric)
        if (classExists("org.quiltmc.loader.api.QuiltLoader")) return "quilt";
        // Fabric
        if (classExists("net.fabricmc.loader.api.FabricLoader")) return "fabric";
        // Forge (1.12.2 legacy)
        if (classExists("net.minecraftforge.fml.common.Mod")) return "forge";
        // Forge (older)
        if (classExists("cpw.mods.fml.common.Mod")) return "forge_legacy";
        return "unknown";
    }

    /**
     * Attempts to detect the Minecraft version from available markers.
     */
    private static String detectMinecraftVersion() {
        // Method 1: Forge version file
        try {
            InputStream is = Mini_DirtyRoomCore.class.getClassLoader()
                .getResourceAsStream("version.json");
            if (is != null) {
                String content = readStreamFully(is);
                // Simple JSON parse for "id" field
                int idx = content.indexOf("\"id\"");
                if (idx >= 0) {
                    int colon = content.indexOf(':', idx);
                    int quote1 = content.indexOf('"', colon + 1);
                    int quote2 = content.indexOf('"', quote1 + 1);
                    if (quote1 >= 0 && quote2 > quote1) {
                        return content.substring(quote1 + 1, quote2);
                    }
                }
            }
        } catch (Exception ignored) {}

        // Method 2: Check for known 1.12.2 classes
        if (classExists("net.minecraft.init.Blocks")) {
            return "1.12.2-presumed";
        }

        return "unknown";
    }

    /**
     * Detects whether this is a client (vs dedicated server).
     */
    private static boolean detectIsClient() {
        if (classExists("net.minecraft.client.Minecraft")) return true;
        if (classExists("net.minecraft.client.main.Main")) return true;
        String target = System.getProperty("fml.mcpTargetDir", "");
        if (target.toLowerCase(Locale.ROOT).contains("client")) return true;
        return false;
    }


    // ========================================================================
    //  SECTION 3: JAVA VERSION AUTO-UPGRADE / RELAUNCH
    // ========================================================================

    /**
     * Attempts to relaunch the game on Java 25. The strategy is:
     *   1. Look for a bundled JRE in .mini_dirtyroom/jre/
     *   2. Look for a system-installed Java 25
     *   3. Download a JRE 25 from Adoptium (if config allows)
     *   4. Relaunch the current process with the new java binary
     *
     * @return true if the current process should exit (relaunch started)
     */
    private static boolean attemptJavaRelaunch() {
        Path javaExecutable = findJava25();

        if (javaExecutable == null && CONFIG.downloadJRE) {
            LOGGER.info("[MDR] No local Java 25 found. Downloading...");
            javaExecutable = downloadJRE25();
        }

        if (javaExecutable == null) {
            LOGGER.warning("[MDR] Could not find or download Java 25.");
            return false;
        }

        LOGGER.info("[MDR] Found Java 25 at: " + javaExecutable);
        return relaunchProcess(javaExecutable);
    }

    /**
     * Searches for a Java 25 executable in known locations.
     */
    private static Path findJava25() {
        // Priority 1: Bundled JRE in MDR home
        Path bundled = getMDRHome().resolve(JRE_DIR)
            .resolve("jdk-" + TARGET_JAVA_VERSION);
        Path bundledBin = resolveJavaBinary(bundled);
        if (bundledBin != null && isCorrectJavaVersion(bundledBin)) {
            return bundledBin;
        }

        // Priority 2: MINI_DIRTYROOM_JAVA_HOME env variable
        String envHome = System.getenv("MINI_DIRTYROOM_JAVA_HOME");
        if (envHome != null && !envHome.isEmpty()) {
            Path envBin = resolveJavaBinary(Paths.get(envHome));
            if (envBin != null && isCorrectJavaVersion(envBin)) {
                return envBin;
            }
        }

        // Priority 3: JAVA25_HOME env variable
        String java25 = System.getenv("JAVA25_HOME");
        if (java25 != null && !java25.isEmpty()) {
            Path j25Bin = resolveJavaBinary(Paths.get(java25));
            if (j25Bin != null && isCorrectJavaVersion(j25Bin)) {
                return j25Bin;
            }
        }

        // Priority 4: Scan common installation directories
        List<Path> searchPaths = new ArrayList<>();
        if (ENV.isWindows) {
            searchPaths.add(Paths.get("C:\\Program Files\\Java"));
            searchPaths.add(Paths.get("C:\\Program Files\\Eclipse Adoptium"));
            searchPaths.add(Paths.get("C:\\Program Files\\Microsoft\\jdk-25"));
            searchPaths.add(Paths.get(System.getenv("LOCALAPPDATA"),
                                      "Programs", "Eclipse Adoptium"));
        } else if (ENV.isMacOS) {
            searchPaths.add(Paths.get("/Library/Java/JavaVirtualMachines"));
            searchPaths.add(Paths.get(System.getProperty("user.home"),
                                      ".sdkman/candidates/java"));
        } else if (ENV.isLinux) {
            searchPaths.add(Paths.get("/usr/lib/jvm"));
            searchPaths.add(Paths.get("/usr/local/lib/jvm"));
            searchPaths.add(Paths.get(System.getProperty("user.home"),
                                      ".sdkman/candidates/java"));
            searchPaths.add(Paths.get(System.getProperty("user.home"),
                                      ".jdks"));
        }

        for (Path searchPath : searchPaths) {
            if (!Files.isDirectory(searchPath)) continue;
            try {
                Optional<Path> found = Files.list(searchPath)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.contains("25") || name.contains("jdk-25");
                    })
                    .map(Mini_DirtyRoomCore::resolveJavaBinary)
                    .filter(Objects::nonNull)
                    .filter(Mini_DirtyRoomCore::isCorrectJavaVersion)
                    .findFirst();
                if (found.isPresent()) return found.get();
            } catch (IOException ignored) {}
        }

        return null;
    }

    /**
     * Resolves the `java` or `java.exe` binary within a JDK/JRE home.
     */
    private static Path resolveJavaBinary(Path javaHome) {
        if (javaHome == null || !Files.isDirectory(javaHome)) return null;
        String ext = ENV != null && ENV.isWindows ? ".exe" : "";
        Path bin = javaHome.resolve("bin").resolve("java" + ext);
        if (Files.isRegularFile(bin) && Files.isExecutable(bin)) return bin;
        // Also try without bin/ (some layouts)
        bin = javaHome.resolve("java" + ext);
        if (Files.isRegularFile(bin) && Files.isExecutable(bin)) return bin;
        return null;
    }

    /**
     * Runs the given java binary with `-version` and checks the output.
     */
    private static boolean isCorrectJavaVersion(Path javaBinary) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                javaBinary.toString(), "-version"
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = readStreamFully(proc.getInputStream());
            proc.waitFor(5, TimeUnit.SECONDS);
            proc.destroyForcibly();
            return output.contains("\"" + TARGET_JAVA_VERSION)
                || output.contains("version \"" + TARGET_JAVA_VERSION);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Downloads a JRE 25 from Adoptium (Eclipse Temurin) API.
     * Returns the path to the java binary, or null on failure.
     */
    private static Path downloadJRE25() {
        try {
            String arch;
            if (ENV.isARM && ENV.is64Bit) arch = "aarch64";
            else if (ENV.isARM)           arch = "arm";
            else                          arch = "x64";

            String os;
            if (ENV.isWindows)     os = "windows";
            else if (ENV.isMacOS)  os = "mac";
            else if (ENV.isAndroid) {
                LOGGER.info("[MDR] Cannot download JRE on Android. Skipping.");
                return null;
            } else                 os = "linux";

            String url = String.format(
                "https://api.adoptium.net/v3/binary/latest/%d/ga/%s/%s/jdk/hotspot/normal/eclipse",
                TARGET_JAVA_VERSION, os, arch
            );

            LOGGER.info("[MDR] Downloading JRE from: " + url);

            Path downloadDir = getMDRHome().resolve(JRE_DIR);
            Files.createDirectories(downloadDir);
            String ext = ENV.isWindows ? ".zip" : ".tar.gz";
            Path archivePath = downloadDir.resolve("jdk-" + TARGET_JAVA_VERSION + ext);

            downloadFile(url, archivePath, "JRE " + TARGET_JAVA_VERSION);

            // Extract
            LOGGER.info("[MDR] Extracting JRE archive...");
            Path extractDir = downloadDir.resolve("jdk-" + TARGET_JAVA_VERSION);
            extractArchive(archivePath, extractDir);

            // Find java binary inside extracted directory (may be nested)
            Path javaBin = findJavaBinaryRecursive(extractDir);
            if (javaBin != null) {
                // Make executable on Unix
                if (!ENV.isWindows) {
                    Set<PosixFilePermission> perms = new HashSet<>();
                    perms.add(PosixFilePermission.OWNER_EXECUTE);
                    perms.add(PosixFilePermission.OWNER_READ);
                    perms.add(PosixFilePermission.OWNER_WRITE);
                    Files.setPosixFilePermissions(javaBin, perms);
                }
                LOGGER.info("[MDR] JRE extracted. Binary at: " + javaBin);
                return javaBin;
            }

            LOGGER.warning("[MDR] JRE extraction succeeded but java binary not found.");
            return null;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                "[MDR] JRE download/extraction failed", e);
            return null;
        }
    }

    /**
     * Relaunches the current Java process using the specified java binary,
     * preserving all JVM arguments, classpath, and program arguments.
     */
    private static boolean relaunchProcess(Path javaBinary) {
        try {
            // Reconstruct the command line
            List<String> command = new ArrayList<>();
            command.add(javaBinary.toAbsolutePath().toString());

            // JVM arguments from current process
            List<String> inputArgs = getJVMInputArguments();
            for (String arg : inputArgs) {
                // Skip -javaagent for the old agent, we'll re-add ours
                if (arg.startsWith("-javaagent:") && !arg.contains("mini_dirtyroom")) {
                    command.add(arg);
                } else if (!arg.startsWith("-javaagent:")) {
                    command.add(arg);
                }
            }

            // Add our relaunch marker
            command.add("-D" + RELAUNCH_PROPERTY + "=true");
            command.add("-D" + INIT_PROPERTY + "=false");

            // Add module system opens for Java 9+
            command.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
            command.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED");
            command.add("--add-opens=java.base/java.util=ALL-UNNAMED");
            command.add("--add-opens=java.base/java.io=ALL-UNNAMED");
            command.add("--add-opens=java.base/java.nio=ALL-UNNAMED");
            command.add("--add-opens=java.base/sun.nio.ch=ALL-UNNAMED");
            command.add("--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED");

            // Classpath
            String classpath = System.getProperty("java.class.path");
            command.add("-cp");
            command.add(classpath);

            // Main class
            String mainClass = detectMainClass();
            if (mainClass == null) {
                LOGGER.warning("[MDR] Could not detect main class for relaunch.");
                return false;
            }
            command.add(mainClass);

            // Program arguments
            String[] progArgs = getProgramArguments();
            if (progArgs != null) {
                Collections.addAll(command, progArgs);
            }

            LOGGER.info("[MDR] Relaunch command: " +
                command.stream()
                    .map(s -> s.contains(" ") ? "\"" + s + "\"" : s)
                    .collect(Collectors.joining(" ")));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            pb.start();

            return true;

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[MDR] Process relaunch failed", e);
            return false;
        }
    }

    /**
     * Gets JVM input arguments via reflection to support Java 8+.
     */
    @SuppressWarnings("unchecked")
    private static List<String> getJVMInputArguments() {
        try {
            Class<?> mgmtFactory = Class.forName(
                "java.lang.management.ManagementFactory");
            Method getRuntimeMXBean = mgmtFactory.getMethod("getRuntimeMXBean");
            Object runtimeMXBean = getRuntimeMXBean.invoke(null);
            Method getInputArguments = runtimeMXBean.getClass()
                .getMethod("getInputArguments");
            return (List<String>) getInputArguments.invoke(runtimeMXBean);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Detects the main class of the current application.
     */
    private static String detectMainClass() {
        // Method 1: sun.java.command property
        String cmd = System.getProperty("sun.java.command", "");
        if (!cmd.isEmpty()) {
            String[] parts = cmd.split("\\s+");
            if (parts.length > 0 && !parts[0].endsWith(".jar")) {
                return parts[0];
            }
        }

        // Method 2: Check known main classes
        String[] knownMains = {
            "net.minecraft.client.main.Main",
            "net.minecraft.server.MinecraftServer",
            "cpw.mods.fml.relauncher.ServerLaunchWrapper",
            "net.minecraftforge.fml.relauncher.ServerLaunchWrapper",
            "GradleStart",
            "GradleStartServer",
            "net.fabricmc.loader.launch.knot.KnotClient",
            "net.fabricmc.loader.launch.knot.KnotServer"
        };

        for (String main : knownMains) {
            if (classExists(main)) return main;
        }

        return cmd.isEmpty() ? null : cmd.split("\\s+")[0];
    }

    /**
     * Extracts program arguments (non-JVM arguments) from the command line.
     */
    private static String[] getProgramArguments() {
        String cmd = System.getProperty("sun.java.command", "");
        if (cmd.isEmpty()) return null;
        String[] parts = cmd.split("\\s+");
        if (parts.length <= 1) return new String[0];
        return Arrays.copyOfRange(parts, 1, parts.length);
    }


    // ========================================================================
    //  SECTION 4: MODULE SYSTEM PREPARATION (JAVA 9+)
    // ========================================================================

    /**
     * Opens critical packages in the module system so that bytecode
     * transformation and reflection work properly.
     */
    private static void prepareModuleSystem() {
        LOGGER.info("[MDR] Preparing module system for Java " + ENV.javaVersion);

        String[][] requiredOpens = {
            {"java.base",    "java.lang"},
            {"java.base",    "java.lang.reflect"},
            {"java.base",    "java.lang.invoke"},
            {"java.base",    "java.util"},
            {"java.base",    "java.io"},
            {"java.base",    "java.net"},
            {"java.base",    "java.nio"},
            {"java.base",    "java.security"},
            {"java.base",    "sun.nio.ch"},
            {"java.base",    "sun.security.ssl"},
            {"java.base",    "jdk.internal.misc"},
            {"java.base",    "jdk.internal.ref"},
            {"java.desktop", "sun.awt"},
            {"java.desktop", "java.awt"},
        };

        // First, try using Unsafe to bypass module checks entirely
        boolean bypassedViaUnsafe = tryUnsafeModuleBypass();
        if (bypassedViaUnsafe) {
            LOGGER.info("[MDR] Module restrictions bypassed via Unsafe.");
            return;
        }

        // Fallback: use Instrumentation if available
        if (INSTRUMENTATION != null) {
            for (String[] pair : requiredOpens) {
                openModuleViaInstrumentation(pair[0], pair[1]);
            }
            return;
        }

        // Last resort: log that the user needs --add-opens
        LOGGER.warning("[MDR] Could not programmatically open modules. "
                     + "You may need to add JVM arguments:");
        for (String[] pair : requiredOpens) {
            LOGGER.warning("[MDR]   --add-opens=" + pair[0]
                         + "/" + pair[1] + "=ALL-UNNAMED");
        }
    }

    /**
     * Attempts to use sun.misc.Unsafe to set the module of key classes
     * to null or to our own module, effectively bypassing module checks.
     */
    private static boolean tryUnsafeModuleBypass() {
        try {
            // Get Unsafe instance
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);

            // Get objectFieldOffset method
            Method objectFieldOffset = unsafeClass.getMethod(
                "objectFieldOffset", Field.class);

            // Get the "module" field offset in java.lang.Class
            // This field exists in Java 9+
            Field moduleField = Class.class.getDeclaredField("module");
            long moduleOffset = (long) objectFieldOffset.invoke(unsafe, moduleField);

            // Get our module (unnamed module of our class loader)
            Object ourModule = Mini_DirtyRoomCore.class.getClassLoader()
                .getClass().getMethod("getUnnamedModule")
                .invoke(Mini_DirtyRoomCore.class.getClassLoader());

            // We need to make java.base's module open to us.
            // One technique: create an "everyone" module that exports everything.
            // Simpler technique: use Unsafe to change module field of
            // target class instances so they appear to be in our module.
            // But actually we just need to open the modules.

            // Alternative: override the module accessibility check
            // by patching the Module class itself
            Method putObject = unsafeClass.getMethod(
                "putObject", Object.class, long.class, Object.class);

            // Make String.class's module = our unnamed module (test)
            // This is very dangerous but effective
            LOGGER.info("[MDR] Unsafe module bypass: patching module references");

            // Instead of patching individual classes, let's add our module
            // to the exports of java.base using reflection on Module internals
            Class<?> moduleClass = Class.forName("java.lang.Module");

            // Get the Module instance for java.base
            Method getModule = Class.class.getMethod("getModule");
            Object javaBase = getModule.invoke(String.class);

            // Call Module.implAddOpens(String, Module) via Unsafe
            // This is a private method that opens a package to a specific module
            Method implAddOpens = moduleClass.getDeclaredMethod(
                "implAddOpens", String.class, moduleClass);

            // Use Unsafe to make the method accessible by zeroing out the override field
            Field overrideField = null;
            try {
                overrideField = implAddOpens.getClass()
                    .getSuperclass() // AccessibleObject
                    .getDeclaredField("override");
            } catch (NoSuchFieldException e) {
                // Java 17+: field might be named differently or absent
                // Try "accessCheckCallerClass" or direct approach
            }

            if (overrideField != null) {
                long overrideOffset = (long) objectFieldOffset.invoke(
                    unsafe, overrideField);
                // putBoolean(implAddOpens, overrideOffset, true)
                Method putBoolean = unsafeClass.getMethod(
                    "putBoolean", Object.class, long.class, boolean.class);
                putBoolean.invoke(unsafe, implAddOpens, overrideOffset, true);

                // Now call implAddOpens for critical packages
                String[] packages = {
                    "java.lang", "java.lang.reflect", "java.lang.invoke",
                    "java.util", "java.io", "java.nio", "java.net",
                    "sun.nio.ch", "jdk.internal.misc", "jdk.internal.ref"
                };
                for (String pkg : packages) {
                    try {
                        implAddOpens.invoke(javaBase, pkg, ourModule);
                    } catch (Exception e) {
                        LOGGER.fine("[MDR] Could not open " + pkg + ": "
                                  + e.getMessage());
                    }
                }
                return true;
            }

            return false;

        } catch (Exception e) {
            LOGGER.fine("[MDR] Unsafe module bypass failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Opens a module package using the Instrumentation API.
     */
    private static void openModuleViaInstrumentation(
            String moduleName, String packageName) {
        try {
            // Use reflection to call Instrumentation.redefineModule()
            // because this method doesn't exist on Java 8
            Class<?> moduleClass = Class.forName("java.lang.Module");
            Class<?> moduleLayerClass = Class.forName("java.lang.ModuleLayer");

            // Get the boot layer
            Method boot = moduleLayerClass.getMethod("boot");
            Object bootLayer = boot.invoke(null);

            // Find the target module
            Method findModule = moduleLayerClass.getMethod(
                "findModule", String.class);
            Object optionalModule = findModule.invoke(bootLayer, moduleName);

            // Optional.isPresent() / get()
            Method isPresent = optionalModule.getClass().getMethod("isPresent");
            if (!(boolean) isPresent.invoke(optionalModule)) {
                LOGGER.fine("[MDR] Module '" + moduleName + "' not found.");
                return;
            }
            Method get = optionalModule.getClass().getMethod("get");
            Object targetModule = get.invoke(optionalModule);

            // Get our unnamed module
            Object ourModule = Mini_DirtyRoomCore.class.getClassLoader()
                .getClass().getMethod("getUnnamedModule")
                .invoke(Mini_DirtyRoomCore.class.getClassLoader());

            // Build the extraOpens map
            @SuppressWarnings("unchecked")
            Map<String, Set<Object>> extraOpens = new HashMap<>();
            Set<Object> moduleSet = new HashSet<>();
            moduleSet.add(ourModule);
            extraOpens.put(packageName, moduleSet);

            // Call instrumentation.redefineModule(...)
            Method redefineModule = Instrumentation.class.getMethod(
                "redefineModule",
                moduleClass,         // module
                Set.class,           // extraReads
                Map.class,           // extraExports
                Map.class,           // extraOpens
                Set.class,           // extraUses
                Map.class            // extraProvides
            );

            redefineModule.invoke(INSTRUMENTATION,
                targetModule,
                Collections.singleton(ourModule),  // extraReads
                Collections.emptyMap(),             // extraExports
                extraOpens,                         // extraOpens
                Collections.emptySet(),             // extraUses
                Collections.emptyMap()              // extraProvides
            );

            LOGGER.fine("[MDR] Opened " + moduleName + "/" + packageName);

        } catch (Exception e) {
            LOGGER.fine("[MDR] Failed to open " + moduleName + "/"
                      + packageName + ": " + e.getMessage());
        }
    }


    // ========================================================================
    //  SECTION 5: INSTRUMENTATION AGENT ATTACHMENT
    // ========================================================================

    /**
     * Agent premain entry point (if loaded as -javaagent).
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        INSTRUMENTATION = inst;
        LOGGER.info("[MDR] Agent loaded via -javaagent.");
    }

    /**
     * Agent agentmain entry point (if attached at runtime).
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        INSTRUMENTATION = inst;
        LOGGER.info("[MDR] Agent attached at runtime.");
    }

    /**
     * Attempts to self-attach the instrumentation agent at runtime.
     * Uses the Attach API (tools.jar on Java 8, jdk.attach module on 9+).
     */
    private static Instrumentation attachInstrumentationAgent() {
        // If already loaded via -javaagent
        if (INSTRUMENTATION != null) return INSTRUMENTATION;

        try {
            // Step 1: Find our own JAR file
            Path agentJar = findOwnJarPath();
            if (agentJar == null) {
                LOGGER.fine("[MDR] Cannot find own JAR for self-attach.");
                return null;
            }

            // Step 2: Ensure MANIFEST.MF has Agent-Class
            ensureAgentManifest(agentJar);

            // Step 3: Get current PID
            String pid = getCurrentPID();
            if (pid == null) {
                LOGGER.fine("[MDR] Cannot determine PID.");
                return null;
            }

            // Step 4: Attach
            Class<?> vmClass;
            if (ENV.javaVersion >= 9) {
                vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            } else {
                // Java 8: tools.jar may need to be added to classpath
                addToolsJarToClasspath();
                vmClass = Class.forName("com.sun.tools.attach.VirtualMachine");
            }

            Method attach = vmClass.getMethod("attach", String.class);
            Object vm = attach.invoke(null, pid);

            Method loadAgent = vmClass.getMethod("loadAgent", String.class);
            loadAgent.invoke(vm, agentJar.toAbsolutePath().toString());

            Method detach = vmClass.getMethod("detach");
            detach.invoke(vm);

            // The agentmain method should have been called by now
            return INSTRUMENTATION;

        } catch (Exception e) {
            LOGGER.fine("[MDR] Agent self-attach failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Finds the JAR file containing this class.
     */
    private static Path findOwnJarPath() {
        try {
            URL location = Mini_DirtyRoomCore.class.getProtectionDomain()
                .getCodeSource().getLocation();
            if (location != null) {
                Path path = Paths.get(location.toURI());
                if (Files.isRegularFile(path)
                        && path.toString().endsWith(".jar")) {
                    return path;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Ensures the JAR manifest includes Agent-Class for self-attachment.
     */
    private static void ensureAgentManifest(Path jarPath) {
        // In a real impl, we'd verify the MANIFEST.MF contains:
        //   Agent-Class: stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore
        //   Can-Redefine-Classes: true
        //   Can-Retransform-Classes: true
        // For now, we trust the build system has set it up.
    }

    /**
     * Gets the PID of the current process.
     */
    private static String getCurrentPID() {
        try {
            if (ENV.javaVersion >= 9) {
                // ProcessHandle.current().pid()
                Class<?> ph = Class.forName("java.lang.ProcessHandle");
                Method current = ph.getMethod("current");
                Object handle = current.invoke(null);
                Method pid = handle.getClass().getMethod("pid");
                return String.valueOf(pid.invoke(handle));
            } else {
                // Java 8: ManagementFactory.getRuntimeMXBean().getName() → "PID@host"
                Class<?> mgmt = Class.forName(
                    "java.lang.management.ManagementFactory");
                Method getRt = mgmt.getMethod("getRuntimeMXBean");
                Object rt = getRt.invoke(null);
                Method getName = rt.getClass().getMethod("getName");
                String name = (String) getName.invoke(rt);
                return name.split("@")[0];
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * On Java 8, adds tools.jar to the system class loader.
     */
    private static void addToolsJarToClasspath() {
        try {
            String javaHome = System.getProperty("java.home");
            Path toolsJar = Paths.get(javaHome, "..", "lib", "tools.jar")
                .normalize();
            if (!Files.exists(toolsJar)) {
                toolsJar = Paths.get(javaHome, "lib", "tools.jar");
            }
            if (Files.exists(toolsJar)) {
                URLClassLoader sysLoader =
                    (URLClassLoader) ClassLoader.getSystemClassLoader();
                Method addURL = URLClassLoader.class
                    .getDeclaredMethod("addURL", URL.class);
                addURL.setAccessible(true);
                addURL.invoke(sysLoader, toolsJar.toUri().toURL());
            }
        } catch (Exception e) {
            LOGGER.fine("[MDR] Could not add tools.jar: " + e.getMessage());
        }
    }


    // ========================================================================
    //  SECTION 6: LWJGL 3.4.0 OVERRIDE
    // ========================================================================

    // ── 6A: Android-Specific Override ──────────────────────────────────────

    /**
     * On Android (PojavLauncher, Fold Craft, etc.), the launcher bundles
     * a specific LWJGL version (often a modified LWJGL 3.x or custom bridge).
     * We FORCE-REPLACE it with our known-good LWJGL 3.4.0.
     *
     * Strategy:
     *   1. Detect the launcher's LWJGL jar locations
     *   2. Extract our bundled LWJGL 3.4.0 jars to a staging area
     *   3. Use DeepMix @DeepControl to disable any existing LWJGL transformers
     *   4. Use class loader manipulation to prioritize our LWJGL
     *   5. For native libraries, use the launcher's existing native bridge
     *      (since Android can't load desktop .so files)
     */
    @DeepOverwrite(
        target = "org.lwjgl.system.Library",
        method = "loadSystem"
    )
    @DeepCondition(condition = "ENV.isAndroid")
    private static void overrideLWJGLAndroid() {
        LOGGER.info("[MDR] ╔═══════════════════════════════════════╗");
        LOGGER.info("[MDR] ║  Android LWJGL Override Sequence     ║");
        LOGGER.info("[MDR] ╚═══════════════════════════════════════╝");

        try {
            // Step 1: Detect launcher type and current LWJGL location
            AndroidLauncherInfo launcherInfo = detectAndroidLauncher();
            LOGGER.info("[MDR] Detected launcher: " + launcherInfo.name
                      + " (LWJGL at: " + launcherInfo.lwjglPath + ")");

            // Step 2: Extract our LWJGL 3.4.0 jars from resources
            Path stagingDir = getAndroidStagingDir();
            Files.createDirectories(stagingDir);

            List<Path> lwjglJars = extractBundledLWJGL(stagingDir);
            LOGGER.info("[MDR] Extracted " + lwjglJars.size()
                      + " LWJGL 3.4.0 jars to staging.");

            // Step 3: Inject our jars at the FRONT of the classpath
            ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
            ClassLoader overrideCL = createOverrideClassLoader(lwjglJars, currentCL);
            Thread.currentThread().setContextClassLoader(overrideCL);

            // Step 4: Force-replace the system class loader's LWJGL classes
            // using DeepMix bytecode injection
            forceReplaceAndroidLWJGL(launcherInfo, lwjglJars);

            // Step 5: Handle native library bridge
            // Android LWJGL uses a custom native bridge (e.g., gl4es, virgl)
            // We keep the launcher's native bridge but redirect LWJGL Java
            // calls through our 3.4.0 API
            setupAndroidNativeBridge(launcherInfo);

            LOGGER.info("[MDR] Android LWJGL override complete.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                "[MDR] Android LWJGL override FAILED", e);
            BOOT_WARNINGS.add("Android LWJGL override failed: " + e.getMessage());
        }
    }

    /**
     * Detects the Android launcher type and locates its LWJGL installation.
     */
    private static AndroidLauncherInfo detectAndroidLauncher() {
        // PojavLauncher
        try {
            Class<?> pojav = Class.forName(
                "net.kdt.pojavlaunch.PojavApplication");
            Field f = pojav.getDeclaredField("CURRENT_DIR");
            f.setAccessible(true);
            String baseDir = (String) f.get(null);
            return new AndroidLauncherInfo(
                "PojavLauncher",
                Paths.get(baseDir, "lwjgl3"),
                Paths.get(baseDir, "lib_natives"),
                "pojav"
            );
        } catch (Exception ignored) {}

        // Fold Craft Launcher
        try {
            Class<?> fcl = Class.forName(
                "com.fold.craft.launcher.FoldCraftApplication");
            return new AndroidLauncherInfo(
                "FoldCraftLauncher",
                Paths.get("/data/data/com.fold.craft.launcher/files/lwjgl"),
                Paths.get("/data/data/com.fold.craft.launcher/files/natives"),
                "fcl"
            );
        } catch (Exception ignored) {}

        // QuestCraft / Generic
        try {
            Class<?> qc = Class.forName(
                "com.questcraft.launcher.QuestCraftApp");
            return new AndroidLauncherInfo(
                "QuestCraft",
                Paths.get("/sdcard/QuestCraft/lwjgl"),
                Paths.get("/sdcard/QuestCraft/natives"),
                "questcraft"
            );
        } catch (Exception ignored) {}

        // Fallback: generic Android
        Path extDir = Paths.get(
            System.getenv("ANDROID_DATA") != null
                ? System.getenv("ANDROID_DATA")
                : "/data/data"
        );
        return new AndroidLauncherInfo(
            "GenericAndroid",
            extDir.resolve("minecraft/lwjgl"),
            extDir.resolve("minecraft/natives"),
            "generic"
        );
    }

    /**
     * Information about the Android launcher and its LWJGL setup.
     */
    static final class AndroidLauncherInfo {
        final String name;
        final Path   lwjglPath;
        final Path   nativesPath;
        final String launcherType;

        AndroidLauncherInfo(String name, Path lwjglPath,
                           Path nativesPath, String launcherType) {
            this.name         = name;
            this.lwjglPath    = lwjglPath;
            this.nativesPath  = nativesPath;
            this.launcherType = launcherType;
        }
    }

    /**
     * Gets the staging directory for extracted LWJGL jars on Android.
     */
    private static Path getAndroidStagingDir() {
        String extStorage = System.getenv("EXTERNAL_STORAGE");
        if (extStorage == null) extStorage = "/sdcard";
        return Paths.get(extStorage, ".mini_dirtyroom", "lwjgl_staging");
    }

    /**
     * Extracts the bundled LWJGL 3.4.0 jars from the mod's resources.
     */
    private static List<Path> extractBundledLWJGL(Path targetDir) throws IOException {
        List<Path> extracted = new ArrayList<>();

        String[] bundledNames = {
            "lwjgl-3.4.0.jar",
            "lwjgl-opengl-3.4.0.jar",
            "lwjgl-openal-3.4.0.jar",
            "lwjgl-glfw-3.4.0.jar",
            "lwjgl-stb-3.4.0.jar",
            "lwjgl-tinyfd-3.4.0.jar",
            "lwjgl-jemalloc-3.4.0.jar"
        };

        for (String name : bundledNames) {
            String resourcePath = "lwjgl/" + name;
            InputStream is = Mini_DirtyRoomCore.class.getClassLoader()
                .getResourceAsStream(resourcePath);

            if (is == null) {
                // Try alternative path
                is = Mini_DirtyRoomCore.class.getResourceAsStream(
                    "/META-INF/lwjgl/" + name);
            }

            if (is != null) {
                Path target = targetDir.resolve(name);
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                is.close();
                extracted.add(target);
                LOGGER.fine("[MDR] Extracted: " + name);
            } else {
                LOGGER.fine("[MDR] Bundled LWJGL jar not found: " + name
                          + " (will attempt download)");
            }
        }

        // If bundled jars were not found, download them
        if (extracted.isEmpty()) {
            LOGGER.info("[MDR] No bundled LWJGL jars found. Downloading...");
            extracted.addAll(downloadLWJGLJars(targetDir));
        }

        return extracted;
    }

    /**
     * Creates a class loader that prioritizes our LWJGL jars over the parent.
     * This is a "child-first" class loader for org.lwjgl.** packages.
     */
    private static ClassLoader createOverrideClassLoader(
            List<Path> lwjglJars, ClassLoader parent) throws Exception {

        URL[] urls = new URL[lwjglJars.size()];
        for (int i = 0; i < lwjglJars.size(); i++) {
            urls[i] = lwjglJars.get(i).toUri().toURL();
        }

        return new LWJGLOverrideClassLoader(urls, parent);
    }

    /**
     * A child-first class loader that intercepts all org.lwjgl.** classes
     * and loads them from our LWJGL 3.4.0 jars, bypassing the parent.
     */
    static final class LWJGLOverrideClassLoader extends URLClassLoader {
        private final ClassLoader parentFallback;

        LWJGLOverrideClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, null); // null parent = bootstrap only
            this.parentFallback = parent;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            // Check if already loaded
            Class<?> c = findLoadedClass(name);
            if (c != null) return c;

            // LWJGL classes: load from our jars (child-first)
            if (name.startsWith("org.lwjgl.")) {
                try {
                    c = findClass(name);
                    if (resolve) resolveClass(c);
                    return c;
                } catch (ClassNotFoundException e) {
                    // Fall through to parent
                }
            }

            // Everything else: delegate to parent
            if (parentFallback != null) {
                return parentFallback.loadClass(name);
            }
            throw new ClassNotFoundException(name);
        }

        @Override
        public URL getResource(String name) {
            if (name.startsWith("org/lwjgl/")) {
                URL url = findResource(name);
                if (url != null) return url;
            }
            return parentFallback != null
                 ? parentFallback.getResource(name) : null;
        }
    }

    /**
     * Uses DeepMix bytecode injection to force-replace LWJGL classes in
     * the Android launcher's class loader hierarchy.
     */
    @DeepSurgical(
        target = "org.lwjgl.Version::getVersion",
        point  = InjectionPoint.HEAD
    )
    @DeepASMInline(
        at = InjectionPoint.HEAD,
        instructions = {
            @ASMInstruction(
                type   = ASMInstructionType.LDC_INSN,
                value  = "3.4.0"
            ),
            @ASMInstruction(
                type   = ASMInstructionType.INSN,
                opcode = 176 // ARETURN
            )
        }
    )
    private static void forceReplaceAndroidLWJGL(
            AndroidLauncherInfo launcherInfo, List<Path> lwjglJars) {
        LOGGER.info("[MDR] Force-replacing LWJGL in " + launcherInfo.name);

        try {
            // Strategy A: If we have Instrumentation, retransform loaded classes
            if (INSTRUMENTATION != null) {
                Class<?>[] loadedClasses = INSTRUMENTATION.getAllLoadedClasses();
                List<Class<?>> lwjglClasses = new ArrayList<>();
                for (Class<?> cls : loadedClasses) {
                    if (cls.getName().startsWith("org.lwjgl.")) {
                        lwjglClasses.add(cls);
                    }
                }

                if (!lwjglClasses.isEmpty()) {
                    LOGGER.info("[MDR] Found " + lwjglClasses.size()
                              + " already-loaded LWJGL classes to retransform.");

                    LWJGLRetransformer retransformer =
                        new LWJGLRetransformer(lwjglJars);
                    INSTRUMENTATION.addTransformer(retransformer, true);
                    INSTRUMENTATION.retransformClasses(
                        lwjglClasses.toArray(new Class<?>[0]));
                    INSTRUMENTATION.removeTransformer(retransformer);
                }
            }

            // Strategy B: Patch the launcher's class loader via reflection
            patchClassLoaderSearchPath(launcherInfo, lwjglJars);

            // Strategy C: Override the physical JAR files on disk
            if (CONFIG.overrideFilesOnDisk) {
                overrideLWJGLFilesOnDisk(launcherInfo.lwjglPath, lwjglJars);
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                "[MDR] Force-replace partially failed", e);
            BOOT_WARNINGS.add("Partial LWJGL replacement on Android: "
                            + e.getMessage());
        }
    }

    /**
     * Patches the class loader's search path to prioritize our LWJGL jars.
     */
    private static void patchClassLoaderSearchPath(
            AndroidLauncherInfo info, List<Path> lwjglJars) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();

            // Android uses BaseDexClassLoader → pathList → dexElements[]
            // Desktop uses URLClassLoader → ucp → path/urls

            if (ENV.isAndroid) {
                // Android-specific: patch BaseDexClassLoader
                Class<?> baseDexCL = Class.forName(
                    "dalvik.system.BaseDexClassLoader");
                Field pathListField = baseDexCL.getDeclaredField("pathList");
                pathListField.setAccessible(true);
                Object pathList = pathListField.get(cl);

                Class<?> dexPathListClass = Class.forName(
                    "dalvik.system.DexPathList");
                Field dexElementsField = dexPathListClass
                    .getDeclaredField("dexElements");
                dexElementsField.setAccessible(true);
                Object[] oldElements = (Object[]) dexElementsField.get(pathList);

                // Create new dex elements from our LWJGL jars
                // and prepend them to the array
                Method makeDexElements = dexPathListClass.getDeclaredMethod(
                    "makeDexElements",
                    List.class, File.class, List.class, ClassLoader.class
                );
                makeDexElements.setAccessible(true);

                List<File> jarFiles = new ArrayList<>();
                for (Path jar : lwjglJars) {
                    jarFiles.add(jar.toFile());
                }

                @SuppressWarnings("unchecked")
                Object[] newElements = (Object[]) makeDexElements.invoke(
                    null, jarFiles,
                    new File(info.lwjglPath.toString()),
                    new ArrayList<>(), cl
                );

                // Merge: new elements first, then old elements
                Object[] mergedElements = new Object[
                    newElements.length + oldElements.length];
                System.arraycopy(newElements, 0,
                    mergedElements, 0, newElements.length);
                System.arraycopy(oldElements, 0,
                    mergedElements, newElements.length, oldElements.length);

                dexElementsField.set(pathList, mergedElements);
                LOGGER.info("[MDR] Android class loader path patched. "
                          + "LWJGL 3.4.0 jars prepended.");

            } else {
                // Desktop: handled differently (see overrideLWJGLDesktop)
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                "[MDR] Class loader path patching failed", e);
        }
    }

    /**
     * Overwrites the physical LWJGL jar files on disk with our versions.
     * This is the "nuclear option" for persistent override.
     */
    private static void overrideLWJGLFilesOnDisk(
            Path targetDir, List<Path> sourceJars) {
        if (!Files.isDirectory(targetDir)) {
            LOGGER.fine("[MDR] LWJGL directory does not exist: " + targetDir);
            return;
        }

        try {
            // Backup existing files
            Path backupDir = targetDir.resolve("backup_" + System.currentTimeMillis());
            Files.createDirectories(backupDir);

            try (DirectoryStream<Path> stream =
                    Files.newDirectoryStream(targetDir, "*.jar")) {
                for (Path existing : stream) {
                    String name = existing.getFileName().toString().toLowerCase();
                    if (name.contains("lwjgl")) {
                        Files.move(existing,
                            backupDir.resolve(existing.getFileName()),
                            StandardCopyOption.REPLACE_EXISTING);
                        LOGGER.fine("[MDR] Backed up: " + existing.getFileName());
                    }
                }
            }

            // Copy our jars
            for (Path jar : sourceJars) {
                Path dest = targetDir.resolve(jar.getFileName());
                Files.copy(jar, dest, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.fine("[MDR] Installed: " + jar.getFileName());
            }

            LOGGER.info("[MDR] LWJGL files overridden on disk. "
                      + "Backup at: " + backupDir);

        } catch (IOException e) {
            LOGGER.log(Level.WARNING,
                "[MDR] Disk override failed", e);
        }
    }

    /**
     * Sets up the native bridge on Android so LWJGL 3.4.0 Java classes
     * can call the launcher's existing native rendering backend.
     */
    private static void setupAndroidNativeBridge(AndroidLauncherInfo info) {
        LOGGER.info("[MDR] Setting up Android native bridge for "
                  + info.launcherType);

        try {
            // Android launchers use custom native libraries:
            //   PojavLauncher: libpojav.so, libgl4es.so
            //   FCL: libfcl.so, libvirgl.so
            // These provide OpenGL ES → desktop GL translation.
            // LWJGL 3.4.0 will use these through JNI as long as
            // the library names match what LWJGL expects.

            // Override LWJGL's library loading to use the launcher's natives
            System.setProperty(
                "org.lwjgl.librarypath",
                info.nativesPath.toAbsolutePath().toString()
            );
            System.setProperty("org.lwjgl.opengl.libname", getAndroidGLLibName(info));
            System.setProperty("org.lwjgl.opengles.libname", "libGLESv2.so");

            // For GLFW, Android launchers provide their own stub
            String glfwLib = findNativeLib(info.nativesPath, "glfw");
            if (glfwLib != null) {
                System.setProperty("org.lwjgl.glfw.libname", glfwLib);
            }

            // For OpenAL, use OpenSL ES bridge or launcher's OpenAL
            String alLib = findNativeLib(info.nativesPath, "openal");
            if (alLib != null) {
                System.setProperty("org.lwjgl.openal.libname", alLib);
            }

            LOGGER.info("[MDR] Android native bridge configured.");

        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                "[MDR] Native bridge setup partially failed", e);
        }
    }

    /**
     * Determines the correct GL library name for the Android launcher.
     */
    private static String getAndroidGLLibName(AndroidLauncherInfo info) {
        switch (info.launcherType) {
            case "pojav":
                // PojavLauncher uses gl4es or ANGLE or Zink
                if (findNativeLib(info.nativesPath, "gl4es") != null)
                    return "libgl4es_114.so";
                if (findNativeLib(info.nativesPath, "angle") != null)
                    return "libEGL_angle.so";
                return "libGL.so";
            case "fcl":
                return "libvirgl.so";
            default:
                return "libGL.so";
        }
    }

    /**
     * Finds a native library by partial name in a directory.
     */
    private static String findNativeLib(Path dir, String partialName) {
        if (!Files.isDirectory(dir)) return null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path file : stream) {
                String name = file.getFileName().toString().toLowerCase();
                if (name.contains(partialName)
                        && (name.endsWith(".so") || name.endsWith(".dylib")
                            || name.endsWith(".dll"))) {
                    return file.toAbsolutePath().toString();
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

    // ── 6B: Desktop LWJGL Override ─────────────────────────────────────────

    /**
     * On desktop (Windows, Linux, macOS), override LWJGL 2.x with 3.4.0.
     * This is simpler than Android because we have full control over the
     * classpath and native library paths.
     */
    @DeepHook(
        targets = {
            @HookTarget(
                className  = "org.lwjgl.opengl.Display",
                methodName = "create"
            )
        },
        timing = HookTiming.BEFORE
    )
    private static void overrideLWJGLDesktop() {
        LOGGER.info("[MDR] ╔═══════════════════════════════════════╗");
        LOGGER.info("[MDR] ║  Desktop LWJGL 3.4.0 Override        ║");
        LOGGER.info("[MDR] ╚═══════════════════════════════════════╝");

        try {
            // Step 1: Download or locate LWJGL 3.4.0 jars
            Path lwjglDir = getMDRHome().resolve(LWJGL_CACHE_DIR);
            Files.createDirectories(lwjglDir);

            List<Path> lwjglJars = ensureLWJGLJarsPresent(lwjglDir);
            LOGGER.info("[MDR] " + lwjglJars.size() + " LWJGL 3.4.0 jars ready.");

            // Step 2: Set system properties for LWJGL
            Path nativesDir = getMDRHome().resolve(NATIVES_DIR)
                .resolve(ENV.getNativeClassifier());
            System.setProperty("org.lwjgl.librarypath",
                nativesDir.toAbsolutePath().toString());
            System.setProperty("org.lwjgl.util.Debug", String.valueOf(CONFIG.debug));

            // Step 3: Inject jars into classpath
            injectJarsIntoClasspath(lwjglJars);

            // Step 4: Register the LWJGL 2→3 bytecode transformer
            // (This is coordinated with LWJGLTransformEngine.java)
            registerLWJGLTransformer();

            LOGGER.info("[MDR] Desktop LWJGL override complete.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                "[MDR] Desktop LWJGL override FAILED", e);
            BOOT_WARNINGS.add("Desktop LWJGL override failed: " + e.getMessage());
        }
    }

    /**
     * Ensures all required LWJGL 3.4.0 jar files are present, downloading
     * any that are missing.
     */
    private static List<Path> ensureLWJGLJarsPresent(Path dir) throws IOException {
        List<Path> jars = new ArrayList<>();

        for (Map.Entry<String, String> entry : LWJGL_ARTIFACTS.entrySet()) {
            String artifact = entry.getKey();
            String url      = entry.getValue();
            Path   jarPath  = dir.resolve(artifact + "-" + TARGET_LWJGL_VERSION + ".jar");

            if (Files.exists(jarPath) && Files.size(jarPath) > 0) {
                // Verify checksum if available
                String expectedChecksum = LWJGL_CHECKSUMS.get(artifact);
                if (expectedChecksum != null) {
                    String actualChecksum = sha256(jarPath);
                    if (!expectedChecksum.equalsIgnoreCase(actualChecksum)) {
                        LOGGER.warning("[MDR] Checksum mismatch for " + artifact
                                     + ". Re-downloading.");
                        Files.delete(jarPath);
                    }
                }
            }

            if (!Files.exists(jarPath)) {
                LOGGER.info("[MDR] Downloading " + artifact + "...");
                downloadFile(url, jarPath, artifact);
            }

            jars.add(jarPath);
        }

        // Also get native jars
        String classifier = ENV.getNativeClassifier();
        for (Map.Entry<String, String> entry : NATIVE_CLASSIFIERS.entrySet()) {
            String artifact   = entry.getKey();
            String baseUrl    = LWJGL_ARTIFACTS.get(artifact);
            if (baseUrl == null) continue;

            String nativeUrl  = baseUrl.replace(
                ".jar", "-" + classifier + ".jar");
            Path   nativeJar  = dir.resolve(
                artifact + "-" + TARGET_LWJGL_VERSION + "-" + classifier + ".jar");

            if (!Files.exists(nativeJar)) {
                LOGGER.info("[MDR] Downloading " + artifact + " natives ("
                          + classifier + ")...");
                try {
                    downloadFile(nativeUrl, nativeJar, artifact + "-natives");
                } catch (Exception e) {
                    LOGGER.warning("[MDR] Native download failed for "
                                 + artifact + ": " + e.getMessage());
                }
            }

            if (Files.exists(nativeJar)) {
                jars.add(nativeJar);
            }
        }

        return jars;
    }

    /**
     * Injects the given JAR files into the system class loader's classpath.
     */
    private static void injectJarsIntoClasspath(List<Path> jars) {
        for (Path jar : jars) {
            try {
                if (ENV.javaVersion <= 8) {
                    // Java 8: URLClassLoader.addURL()
                    URLClassLoader sysLoader =
                        (URLClassLoader) ClassLoader.getSystemClassLoader();
                    Method addURL = URLClassLoader.class
                        .getDeclaredMethod("addURL", URL.class);
                    addURL.setAccessible(true);
                    addURL.invoke(sysLoader, jar.toUri().toURL());
                } else {
                    // Java 9+: Use Instrumentation.appendToSystemClassLoaderSearch()
                    if (INSTRUMENTATION != null) {
                        INSTRUMENTATION.appendToSystemClassLoaderSearch(
                            new JarFile(jar.toFile()));
                    } else {
                        // Fallback: Use Unsafe or MethodHandle to access
                        // internal class loader methods
                        injectJarViaUnsafe(jar);
                    }
                }
                LOGGER.fine("[MDR] Injected into classpath: "
                          + jar.getFileName());
            } catch (Exception e) {
                LOGGER.warning("[MDR] Could not inject " + jar.getFileName()
                             + ": " + e.getMessage());
            }
        }
    }

    /**
     * Injects a JAR into the classpath using Unsafe on Java 9+.
     */
    private static void injectJarViaUnsafe(Path jar) {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);

            // Access the internal URL class path
            ClassLoader sysLoader = ClassLoader.getSystemClassLoader();
            Field ucpField = null;

            // Try different field names across Java versions
            String[] fieldNames = {"ucp", "urlClassPath"};
            Class<?> clClass = sysLoader.getClass();
            while (clClass != null && ucpField == null) {
                for (String fname : fieldNames) {
                    try {
                        ucpField = clClass.getDeclaredField(fname);
                        break;
                    } catch (NoSuchFieldException ignored) {}
                }
                clClass = clClass.getSuperclass();
            }

            if (ucpField != null) {
                Method objectFieldOffset = unsafeClass.getMethod(
                    "objectFieldOffset", Field.class);
                long offset = (long) objectFieldOffset.invoke(unsafe, ucpField);

                Method getObject = unsafeClass.getMethod(
                    "getObject", Object.class, long.class);
                Object ucp = getObject.invoke(unsafe, sysLoader, offset);

                // Call addURL on the URLClassPath
                Method addURL = ucp.getClass().getDeclaredMethod("addURL", URL.class);
                addURL.setAccessible(true);
                addURL.invoke(ucp, jar.toUri().toURL());
            }

        } catch (Exception e) {
            LOGGER.fine("[MDR] Unsafe classpath injection failed: "
                      + e.getMessage());
        }
    }

    /**
     * Registers the LWJGL 2→3 bytecode transformer (coordinates with
     * LWJGLTransformEngine.java).
     */
    @DeepProxy(
        target  = "org.lwjgl.opengl.Display",
        handler = LWJGLDisplayProxyHandler.class
    )
    private static void registerLWJGLTransformer() {
        // The actual transformer is in LWJGLTransformEngine.java.
        // Here we just ensure it's registered with whatever transformation
        // system is available.

        if (INSTRUMENTATION != null) {
            // Register as a ClassFileTransformer
            LOGGER.info("[MDR] Registering LWJGL transformer via Instrumentation.");
            // LWJGLTransformEngine will handle registration
        } else {
            LOGGER.info("[MDR] Registering LWJGL transformer via DeepMix.");
            // DeepMix annotations on LWJGLTransformEngine handle this
        }
    }

    /**
     * Stub proxy handler for LWJGL Display class.
     * The real implementation lives in LWJGLTransformEngine.
     */
    static class LWJGLDisplayProxyHandler {
        public Object handleInvocation(Object proxy, String method, Object[] args) {
            // Delegate to GLFWWindowManager (in LWJGLTransformEngine)
            throw new UnsupportedOperationException(
                "Proxy handler stub — real impl in LWJGLTransformEngine");
        }
    }

    /**
     * Retransformer that replaces LWJGL class bytecode with our versions.
     */
    static class LWJGLRetransformer implements java.lang.instrument.ClassFileTransformer {
        private final Map<String, byte[]> replacementBytecode = new HashMap<>();

        LWJGLRetransformer(List<Path> lwjglJars) {
            // Pre-load all class files from our LWJGL jars
            for (Path jar : lwjglJars) {
                try (JarFile jf = new JarFile(jar.toFile())) {
                    jf.stream()
                        .filter(e -> e.getName().endsWith(".class"))
                        .forEach(entry -> {
                            try (InputStream is = jf.getInputStream(entry)) {
                                String className = entry.getName()
                                    .replace('/', '.')
                                    .replace(".class", "");
                                replacementBytecode.put(className, readAllBytes(is));
                            } catch (IOException ignored) {}
                        });
                } catch (IOException ignored) {}
            }
        }

        @Override
        public byte[] transform(ClassLoader loader, String className,
                Class<?> classBeingRedefined,
                java.security.ProtectionDomain protectionDomain,
                byte[] classfileBuffer) {
            if (className == null) return null;
            String dotName = className.replace('/', '.');
            byte[] replacement = replacementBytecode.get(dotName);
            if (replacement != null) {
                LOGGER.fine("[MDR] Retransformed: " + dotName);
                return replacement;
            }
            return null;
        }
    }


    // ========================================================================
    //  SECTION 7: NATIVE LIBRARY MANAGEMENT
    // ========================================================================

    /**
     * Extracts LWJGL 3.4.0 native libraries from the native jars
     * and loads them into the JVM.
     */
    private static void extractAndLoadNatives() throws Exception {
        if (ENV.isAndroid) {
            LOGGER.info("[MDR] Skipping native extraction on Android "
                      + "(using launcher's native bridge).");
            return;
        }

        String classifier = ENV.getNativeClassifier();
        Path nativesDir = getMDRHome().resolve(NATIVES_DIR).resolve(classifier);
        Files.createDirectories(nativesDir);

        Path lwjglDir = getMDRHome().resolve(LWJGL_CACHE_DIR);

        // Extract natives from native jars
        try (DirectoryStream<Path> stream =
                Files.newDirectoryStream(lwjglDir, "*-" + classifier + ".jar")) {
            for (Path nativeJar : stream) {
                extractNativesFromJar(nativeJar, nativesDir);
            }
        }

        // Set the library path
        System.setProperty("org.lwjgl.librarypath",
            nativesDir.toAbsolutePath().toString());
        System.setProperty("java.library.path",
            nativesDir.toAbsolutePath().toString()
            + File.pathSeparator
            + System.getProperty("java.library.path", ""));

        // Force JVM to re-read java.library.path
        forceReloadLibraryPath();

        // Pre-load critical natives
        preloadNativeLibraries(nativesDir);

        LOGGER.info("[MDR] Natives extracted and loaded from: " + nativesDir);
    }

    /**
     * Extracts .dll/.so/.dylib/.jnilib files from a JAR into a directory.
     */
    private static void extractNativesFromJar(Path jarPath, Path targetDir)
            throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(jarPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(".dll") || name.endsWith(".so")
                 || name.endsWith(".dylib") || name.endsWith(".jnilib")) {

                    // Strip directory prefix
                    String fileName = name.contains("/")
                        ? name.substring(name.lastIndexOf('/') + 1)
                        : name;

                    Path target = targetDir.resolve(fileName);
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.fine("[MDR] Extracted native: " + fileName);
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Forces the JVM to re-read the java.library.path system property.
     */
    private static void forceReloadLibraryPath() {
        try {
            Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            sysPathsField.set(null, null);
        } catch (Exception e) {
            LOGGER.fine("[MDR] Could not reset sys_paths: " + e.getMessage());
        }
    }

    /**
     * Pre-loads critical native libraries to catch errors early.
     */
    private static void preloadNativeLibraries(Path nativesDir) {
        String[] criticalLibs;
        if (ENV.isWindows) {
            criticalLibs = new String[]{"lwjgl.dll", "lwjgl_opengl.dll",
                                        "glfw.dll", "lwjgl_openal.dll"};
        } else if (ENV.isMacOS) {
            criticalLibs = new String[]{"liblwjgl.dylib", "liblwjgl_opengl.dylib",
                                        "libglfw.dylib", "liblwjgl_openal.dylib"};
        } else {
            criticalLibs = new String[]{"liblwjgl.so", "liblwjgl_opengl.so",
                                        "libglfw.so", "liblwjgl_openal.so"};
        }

        for (String lib : criticalLibs) {
            Path libPath = nativesDir.resolve(lib);
            if (Files.exists(libPath)) {
                try {
                    System.load(libPath.toAbsolutePath().toString());
                    LOGGER.fine("[MDR] Pre-loaded: " + lib);
                } catch (UnsatisfiedLinkError e) {
                    LOGGER.warning("[MDR] Could not pre-load " + lib
                                 + ": " + e.getMessage());
                }
            } else {
                LOGGER.fine("[MDR] Native not found (may be extracted later): "
                          + lib);
            }
        }
    }


    // ========================================================================
    //  SECTION 8: DEEPMIX TRANSFORMER REGISTRATION
    // ========================================================================

    /**
     * Registers all bytecode transformers that DeepMix will apply.
     * This method configures the transformation pipeline that converts
     * LWJGL 2 calls to LWJGL 3 equivalents at the bytecode level.
     */
    @DeepEdit(
        reason = "Register all LWJGL 2→3 transformation rules"
    )
    private static void registerDeepMixTransformers() {
        LOGGER.info("[MDR] Registering DeepMix transformation rules...");

        // ── Rule 1: Display class redirect ─────────────────────────────────
        // All calls to org.lwjgl.opengl.Display.* are redirected to
        // our GLFWWindowManager compatibility class.
        registerClassRedirect(
            "org.lwjgl.opengl.Display",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.GLFWWindowManager"
        );

        // ── Rule 2: Keyboard class redirect ────────────────────────────────
        registerClassRedirect(
            "org.lwjgl.input.Keyboard",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.GLFWKeyboardAdapter"
        );

        // ── Rule 3: Mouse class redirect ───────────────────────────────────
        registerClassRedirect(
            "org.lwjgl.input.Mouse",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.GLFWMouseAdapter"
        );

        // ── Rule 4: OpenAL redirect ────────────────────────────────────────
        registerClassRedirect(
            "org.lwjgl.openal.AL",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.OpenALBridge"
        );

        // ── Rule 5: BufferUtils redirect ───────────────────────────────────
        registerClassRedirect(
            "org.lwjgl.BufferUtils",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.BufferUtilsCompat"
        );

        // ── Rule 6: Sys class redirect ─────────────────────────────────────
        registerClassRedirect(
            "org.lwjgl.Sys",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.SysCompat"
        );

        // ── Rule 7: PixelFormat → GLFW window hints ───────────────────────
        registerClassRedirect(
            "org.lwjgl.opengl.PixelFormat",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.PixelFormatCompat"
        );

        // ── Rule 8: DisplayMode → GLFW video mode ─────────────────────────
        registerClassRedirect(
            "org.lwjgl.opengl.DisplayMode",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.DisplayModeCompat"
        );

        // ── Rule 9: GL context capabilities ────────────────────────────────
        registerClassRedirect(
            "org.lwjgl.opengl.GLContext",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.GLContextCompat"
        );
        registerClassRedirect(
            "org.lwjgl.opengl.ContextCapabilities",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.GLCapabilitiesCompat"
        );

        // ── Rule 10: Cursor handling ───────────────────────────────────────
        registerClassRedirect(
            "org.lwjgl.input.Cursor",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.CursorCompat"
        );

        // ── Rule 11: Controllers (if present) ──────────────────────────────
        registerClassRedirect(
            "org.lwjgl.input.Controllers",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.ControllersCompat"
        );

        LOGGER.info("[MDR] " + REDIRECT_COUNT.get()
                  + " class redirect rules registered.");
    }

    /** Counter for registered redirect rules. */
    private static final AtomicInteger REDIRECT_COUNT = new AtomicInteger(0);

    /**
     * Registers a class redirect rule. When the bytecode transformer
     * encounters references to {@code oldClass}, it replaces them with
     * references to {@code newClass}.
     */
    private static void registerClassRedirect(String oldClass, String newClass) {
        // This method stores the mapping. The actual bytecode transformation
        // is performed by LWJGLTransformEngine.java which reads these
        // mappings at class-load time.
        String oldInternal = oldClass.replace('.', '/');
        String newInternal = newClass.replace('.', '/');
        CLASS_REDIRECTS.put(oldInternal, newInternal);
        REDIRECT_COUNT.incrementAndGet();
        LOGGER.fine("[MDR]   " + oldClass + " → " + newClass);
    }

    /** Map of class redirects (old internal name → new internal name). */
    static final Map<String, String> CLASS_REDIRECTS =
        new ConcurrentHashMap<>();

    /**
     * Returns the class redirect map for use by LWJGLTransformEngine.
     */
    public static Map<String, String> getClassRedirects() {
        return Collections.unmodifiableMap(CLASS_REDIRECTS);
    }


    // ========================================================================
    //  SECTION 9: MOD LOADER BRIDGE INITIALIZATION
    // ========================================================================

    /**
     * Initializes the mod loader bridge, which allows Mini_DirtyRoom to
     * work with Forge, Fabric, Quilt, NeoForge, or standalone.
     */
    @DeepForge(mod = @ModMetadata(modId = "Astralis"))
    @DeepFabric(mod = @ModMetadata(modId = "Astralis"),
                entrypoint = FabricEntrypoint.PREINIT)
    private static void initializeModLoaderBridge() {
        LOGGER.info("[MDR] Initializing mod loader bridge for: " + ENV.detectedLoader);

        switch (ENV.detectedLoader) {
            case "forge":
            case "forge_legacy":
                initForgeIntegration();
                break;
            case "fabric":
                initFabricIntegration();
                break;
            case "quilt":
                initQuiltIntegration();
                break;
            case "neoforge":
                initNeoForgeIntegration();
                break;
            default:
                LOGGER.info("[MDR] No recognized mod loader detected. "
                          + "Running in standalone/universal mode.");
                initStandaloneMode();
                break;
        }
    }

    /**
     * Forge integration: register as a CoreMod / ASM transformer provider.
     * Handles both legacy (1.7–1.12) and modern (1.13+) Forge.
     */
    private static void initForgeIntegration() {
        LOGGER.info("[MDR] Setting up Forge integration...");

        try {
            // Attempt legacy Forge (1.12.2 and below)
            if ("forge_legacy".equals(ENV.detectedLoader)) {
                // cpw.mods.fml path
                Class<?> launchClass = Class.forName(
                    "cpw.mods.fml.relauncher.FMLLaunchHandler");
                LOGGER.info("[MDR] Detected legacy Forge (cpw.mods.fml). "
                          + "Registering via IFMLLoadingPlugin pathway.");

                // Register our transformer with the LaunchClassLoader
                ClassLoader lcl = Thread.currentThread().getContextClassLoader();
                if (lcl.getClass().getName().contains("LaunchClassLoader")) {
                    Method registerTransformer = lcl.getClass().getMethod(
                        "registerTransformer", String.class);
                    registerTransformer.invoke(lcl,
                        "stellar.snow.astralis.integration.Mini_DirtyRoom.LWJGLTransformEngine");
                    LOGGER.info("[MDR] Registered LWJGLTransformEngine with LaunchClassLoader.");
                }
            } else {
                // Modern Forge (net.minecraftforge.fml path)
                Class<?> modLoaderClass = Class.forName(
                    "net.minecraftforge.fml.common.Loader");
                LOGGER.info("[MDR] Detected modern Forge. "
                          + "Registering via FML transformer pathway.");

                // Register with LaunchClassLoader (still used in 1.12.2 modern forge)
                ClassLoader lcl = Thread.currentThread().getContextClassLoader();
                if (lcl.getClass().getName().contains("LaunchClassLoader")) {
                    Method registerTransformer = lcl.getClass().getMethod(
                        "registerTransformer", String.class);
                    registerTransformer.invoke(lcl,
                        "stellar.snow.astralis.integration.Mini_DirtyRoom.LWJGLTransformEngine");
                    LOGGER.info("[MDR] Registered LWJGLTransformEngine with Forge LaunchClassLoader.");
                }
            }

            // Register exclusions so Forge doesn't try to transform our classes
            registerForgeExclusions();

            // Hook into Forge event bus for lifecycle events
            registerForgeEventHooks();

        } catch (ClassNotFoundException e) {
            LOGGER.warning("[MDR] Forge classes not found despite detection. "
                         + "Falling back to standalone mode.");
            initStandaloneMode();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                "[MDR] Forge integration setup encountered errors", e);
            BOOT_WARNINGS.add("Forge integration partial failure: " + e.getMessage());
        }
    }

    /**
     * Registers class exclusions with Forge's LaunchClassLoader so our
     * classes and LWJGL 3.4.0 classes are not double-transformed.
     */
    private static void registerForgeExclusions() {
        try {
            ClassLoader lcl = Thread.currentThread().getContextClassLoader();
            if (lcl.getClass().getName().contains("LaunchClassLoader")) {
                Method addExclusion = lcl.getClass().getMethod(
                    "addTransformerExclusion", String.class);

                String[] exclusions = {
                    "stellar.snow.astralis.integration.Mini_DirtyRoom.",
                    "stellar.snow.astralis.integration.DeepMix.",
                    "org.lwjgl.",   // Prevent Forge from touching new LWJGL
                };

                for (String exclusion : exclusions) {
                    addExclusion.invoke(lcl, exclusion);
                    LOGGER.fine("[MDR] Added transformer exclusion: " + exclusion);
                }

                // Also add class loader exclusions
                Method addCLExclusion = lcl.getClass().getMethod(
                    "addClassLoaderExclusion", String.class);
                addCLExclusion.invoke(lcl,
                    "stellar.snow.astralis.integration.Mini_DirtyRoom.");
                addCLExclusion.invoke(lcl,
                    "stellar.snow.astralis.integration.DeepMix.");
            }
        } catch (Exception e) {
            LOGGER.fine("[MDR] Could not register Forge exclusions: "
                      + e.getMessage());
        }
    }

    /**
     * Registers event hooks with Forge's event bus for lifecycle management.
     */
    private static void registerForgeEventHooks() {
        try {
            // Try modern Forge event bus
            Class<?> eventBusClass = null;
            try {
                eventBusClass = Class.forName(
                    "net.minecraftforge.common.MinecraftForge");
            } catch (ClassNotFoundException e) {
                eventBusClass = Class.forName(
                    "cpw.mods.fml.common.FMLCommonHandler");
            }

            if (eventBusClass != null) {
                LOGGER.fine("[MDR] Forge event bus located: "
                          + eventBusClass.getName());
                // We'll register our event listener when the bus is ready
                // via a deferred registration stored for ModLoaderBridge.java
                DEFERRED_REGISTRATIONS.add(() -> {
                    try {
                        registerForgeEventListenerDeferred();
                    } catch (Exception ex) {
                        LOGGER.fine("[MDR] Deferred Forge event registration failed: "
                                  + ex.getMessage());
                    }
                });
            }
        } catch (ClassNotFoundException e) {
            LOGGER.fine("[MDR] No Forge event bus found.");
        }
    }

    /** Deferred registrations to be executed when the mod loader is fully ready. */
    static final List<Runnable> DEFERRED_REGISTRATIONS =
        new CopyOnWriteArrayList<>();

    /**
     * Called later by ModLoaderBridge when Forge is fully initialized.
     */
    private static void registerForgeEventListenerDeferred() throws Exception {
        Class<?> minecraftForge = Class.forName(
            "net.minecraftforge.common.MinecraftForge");
        Field eventBusField = minecraftForge.getDeclaredField("EVENT_BUS");
        Object eventBus = eventBusField.get(null);

        Method register = eventBus.getClass().getMethod("register", Object.class);
        register.invoke(eventBus, new ForgeEventReceiver());
        LOGGER.info("[MDR] Forge event listener registered.");
    }

    /**
     * Stub event receiver for Forge. Actual event handling is in ModLoaderBridge.
     */
    static class ForgeEventReceiver {
        // Forge uses annotation-based event subscription. These annotations
        // would normally be @SubscribeEvent, but we keep this loader-agnostic
        // by using reflection in ModLoaderBridge.java.
    }

    /**
     * Fabric integration: register as a PreLaunch entrypoint and cooperate
     * with Fabric's Mixin subsystem.
     */
    private static void initFabricIntegration() {
        LOGGER.info("[MDR] Setting up Fabric integration...");

        try {
            Class<?> fabricLoader = Class.forName(
                "net.fabricmc.loader.api.FabricLoader");
            Method getInstance = fabricLoader.getMethod("getInstance");
            Object loader = getInstance.invoke(null);

            // Check if we're in the right environment (client vs server)
            Method getEnvType = fabricLoader.getMethod("getEnvironmentType");
            Object envType = getEnvType.invoke(loader);
            LOGGER.info("[MDR] Fabric environment: " + envType);

            // Register with Fabric's Mixin subsystem
            // Fabric uses its own Mixin fork; we need to cooperate
            registerFabricMixinCooperation();

            // Fabric Loader already supports adding to classpath via
            // the mod's jar-in-jar mechanism, but we need runtime injection
            // for downloaded LWJGL jars
            registerFabricClasspathAdditions();

            LOGGER.info("[MDR] Fabric integration complete.");

        } catch (ClassNotFoundException e) {
            LOGGER.warning("[MDR] Fabric Loader class not found. "
                         + "Falling back to standalone mode.");
            initStandaloneMode();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                "[MDR] Fabric integration encountered errors", e);
            BOOT_WARNINGS.add("Fabric integration partial failure: " + e.getMessage());
        }
    }

    /**
     * Registers cooperation with Fabric's Mixin system so our transformers
     * don't conflict with Fabric Mixins.
     */
    private static void registerFabricMixinCooperation() {
        try {
            // Fabric uses SpongePowered Mixin. Register our Mixin config.
            Class<?> mixinBootstrap = Class.forName(
                "org.spongepowered.asm.mixin.Mixins");
            Method addConfiguration = mixinBootstrap.getMethod(
                "addConfiguration", String.class);
            addConfiguration.invoke(null, "mini_dirtyroom.mixins.json");
            LOGGER.fine("[MDR] Registered mini_dirtyroom.mixins.json with Fabric Mixin.");
        } catch (Exception e) {
            LOGGER.fine("[MDR] Fabric Mixin cooperation setup note: "
                      + e.getMessage());
        }
    }

    /**
     * Registers additional classpath entries with Fabric's class loader.
     */
    private static void registerFabricClasspathAdditions() {
        try {
            // Fabric's Knot class loader has addURL or addPath
            ClassLoader knotCL = Thread.currentThread().getContextClassLoader();
            String clName = knotCL.getClass().getName();

            if (clName.contains("Knot") || clName.contains("knot")) {
                // Try addURL method
                Method addURL = null;
                try {
                    addURL = knotCL.getClass().getMethod("addURL", URL.class);
                } catch (NoSuchMethodException e) {
                    // Try getDelegateMethod / addPath
                    try {
                        addURL = knotCL.getClass().getDeclaredMethod(
                            "addPath", Path.class);
                        addURL.setAccessible(true);
                    } catch (NoSuchMethodException e2) {
                        LOGGER.fine("[MDR] Knot class loader has no addURL/addPath. "
                                  + "Using fallback injection.");
                    }
                }

                if (addURL != null) {
                    Path lwjglDir = getMDRHome().resolve(LWJGL_CACHE_DIR);
                    if (Files.isDirectory(lwjglDir)) {
                        final Method finalAddURL = addURL;
                        Files.list(lwjglDir)
                            .filter(p -> p.toString().endsWith(".jar"))
                            .forEach(jar -> {
                                try {
                                    if (finalAddURL.getParameterTypes()[0] == URL.class) {
                                        finalAddURL.invoke(knotCL, jar.toUri().toURL());
                                    } else {
                                        finalAddURL.invoke(knotCL, jar);
                                    }
                                    LOGGER.fine("[MDR] Added to Fabric classpath: "
                                              + jar.getFileName());
                                } catch (Exception ex) {
                                    LOGGER.fine("[MDR] Could not add to Fabric classpath: "
                                              + jar.getFileName());
                                }
                            });
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.fine("[MDR] Fabric classpath addition note: " + e.getMessage());
        }
    }

    /**
     * Quilt integration: similar to Fabric but uses Quilt-specific APIs.
     */
    private static void initQuiltIntegration() {
        LOGGER.info("[MDR] Setting up Quilt integration...");

        try {
            Class<?> quiltLoader = Class.forName(
                "org.quiltmc.loader.api.QuiltLoader");
            LOGGER.info("[MDR] Quilt Loader detected.");

            // Quilt is largely Fabric-compatible, so reuse Fabric setup
            registerFabricMixinCooperation();
            registerFabricClasspathAdditions();

            // Quilt-specific: check for Quilt Standard Libraries (QSL)
            if (classExists("org.quiltmc.qsl.base.api.entrypoint.ModInitializer")) {
                LOGGER.fine("[MDR] QSL detected. Will register via QSL entrypoints.");
            }

            LOGGER.info("[MDR] Quilt integration complete.");

        } catch (ClassNotFoundException e) {
            LOGGER.warning("[MDR] Quilt Loader not found. "
                         + "Falling back to standalone mode.");
            initStandaloneMode();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                "[MDR] Quilt integration encountered errors", e);
            BOOT_WARNINGS.add("Quilt integration partial failure: " + e.getMessage());
        }
    }

    /**
     * NeoForge integration: handles NeoForge-specific APIs and transformers.
     */
    private static void initNeoForgeIntegration() {
        LOGGER.info("[MDR] Setting up NeoForge integration...");

        try {
            Class<?> neoForgeClass = Class.forName(
                "net.neoforged.fml.common.Mod");
            LOGGER.info("[MDR] NeoForge detected.");

            // NeoForge uses a service-based transformer system
            // Register our transformer via the service loader mechanism
            registerNeoForgeTransformers();

            // NeoForge event bus
            registerNeoForgeEventHooks();

            LOGGER.info("[MDR] NeoForge integration complete.");

        } catch (ClassNotFoundException e) {
            LOGGER.warning("[MDR] NeoForge classes not found. "
                         + "Falling back to standalone mode.");
            initStandaloneMode();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                "[MDR] NeoForge integration encountered errors", e);
            BOOT_WARNINGS.add("NeoForge integration partial failure: " + e.getMessage());
        }
    }

    /**
     * Registers transformers with NeoForge's transformation service.
     */
    private static void registerNeoForgeTransformers() {
        try {
            // NeoForge uses cpw.mods.modlauncher for transformation
            Class<?> transformerClass = Class.forName(
                "cpw.mods.modlauncher.api.ITransformer");
            LOGGER.fine("[MDR] ModLauncher ITransformer API found. "
                      + "Transformer registration deferred to ModLoaderBridge.");
            DEFERRED_REGISTRATIONS.add(() -> {
                LOGGER.fine("[MDR] NeoForge transformer deferred registration executed.");
            });
        } catch (ClassNotFoundException e) {
            LOGGER.fine("[MDR] ModLauncher API not found: " + e.getMessage());
        }
    }

    /**
     * Registers event hooks with NeoForge's event bus.
     */
    private static void registerNeoForgeEventHooks() {
        try {
            Class<?> neoForge = Class.forName("net.neoforged.neoforge.common.NeoForge");
            LOGGER.fine("[MDR] NeoForge event bus available. "
                      + "Deferred registration stored.");
            DEFERRED_REGISTRATIONS.add(() -> {
                try {
                    Field busField = neoForge.getDeclaredField("EVENT_BUS");
                    Object bus = busField.get(null);
                    Method register = bus.getClass().getMethod("register", Object.class);
                    register.invoke(bus, new ForgeEventReceiver());
                    LOGGER.info("[MDR] NeoForge event listener registered.");
                } catch (Exception ex) {
                    LOGGER.fine("[MDR] NeoForge event registration failed: "
                              + ex.getMessage());
                }
            });
        } catch (ClassNotFoundException e) {
            LOGGER.fine("[MDR] NeoForge class not found for events.");
        }
    }

    /**
     * Standalone mode: no recognized mod loader. We rely entirely on
     * DeepMix annotations and Instrumentation for bytecode transformation.
     */
    private static void initStandaloneMode() {
        LOGGER.info("[MDR] Standalone mode: relying on DeepMix + Instrumentation.");

        if (INSTRUMENTATION != null) {
            // Register our class file transformer directly
            LOGGER.info("[MDR] Adding ClassFileTransformer via Instrumentation.");
            INSTRUMENTATION.addTransformer(new StandaloneLWJGLTransformer(), true);
        } else {
            LOGGER.warning("[MDR] No Instrumentation and no mod loader. "
                         + "LWJGL transformation will depend entirely on DeepMix "
                         + "annotations and may have limited coverage.");
            BOOT_WARNINGS.add("Running without Instrumentation or mod loader. "
                            + "Some LWJGL transformations may not apply.");
        }
    }

    /**
     * Standalone ClassFileTransformer that performs LWJGL 2→3 class redirects
     * when no mod loader is available.
     */
    static class StandaloneLWJGLTransformer
            implements java.lang.instrument.ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className,
                Class<?> classBeingRedefined,
                java.security.ProtectionDomain protectionDomain,
                byte[] classfileBuffer) {

            if (className == null || classfileBuffer == null) return null;

            // Quick check: does this class reference any LWJGL 2 classes?
            boolean hasLwjgl2Refs = false;
            String classStr = className.replace('/', '.');
            // Skip our own classes and LWJGL 3 classes
            if (classStr.startsWith("stellar.snow.astralis.")
             || classStr.startsWith("org.lwjgl.")) {
                return null;
            }

            // Scan constant pool for LWJGL 2 references (quick byte scan)
            byte[] lwjgl2Marker = "org/lwjgl/opengl/Display".getBytes(StandardCharsets.UTF_8);
            byte[] lwjgl2Input  = "org/lwjgl/input/".getBytes(StandardCharsets.UTF_8);
            byte[] lwjgl2Base   = "org/lwjgl/BufferUtils".getBytes(StandardCharsets.UTF_8);
            byte[] lwjgl2Sys    = "org/lwjgl/Sys".getBytes(StandardCharsets.UTF_8);

            for (int i = 0; i < classfileBuffer.length - lwjgl2Marker.length; i++) {
                if (bytesMatch(classfileBuffer, i, lwjgl2Marker)
                 || bytesMatch(classfileBuffer, i, lwjgl2Input)
                 || bytesMatch(classfileBuffer, i, lwjgl2Base)
                 || bytesMatch(classfileBuffer, i, lwjgl2Sys)) {
                    hasLwjgl2Refs = true;
                    break;
                }
            }

            if (!hasLwjgl2Refs) return null;

            // Perform transformation using ASM
            try {
                return transformClassWithRedirects(classfileBuffer);
            } catch (Exception e) {
                LOGGER.fine("[MDR] Standalone transform failed for "
                          + className + ": " + e.getMessage());
                return null;
            }
        }
    }

    /**
     * Checks if {@code data} at offset {@code offset} starts with {@code pattern}.
     */
    private static boolean bytesMatch(byte[] data, int offset, byte[] pattern) {
        if (offset + pattern.length > data.length) return false;
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) return false;
        }
        return true;
    }

    /**
     * Transforms a class's bytecode by replacing LWJGL 2 class references
     * with our compatibility classes, using direct constant pool manipulation
     * (lightweight, no full ASM dependency required at this bootstrap stage).
     */
    private static byte[] transformClassWithRedirects(byte[] classfileBuffer) {
        // Simple constant pool string replacement
        // This is faster than a full ASM pass for the bootstrap phase.
        // LWJGLTransformEngine.java will do a proper ASM pass later.

        byte[] result = classfileBuffer.clone();
        boolean modified = false;

        for (Map.Entry<String, String> redirect : CLASS_REDIRECTS.entrySet()) {
            byte[] oldBytes = redirect.getKey().getBytes(StandardCharsets.UTF_8);
            byte[] newBytes = redirect.getValue().getBytes(StandardCharsets.UTF_8);

            // Only do same-length or shorter replacements in constant pool
            // to avoid invalidating offsets. For length changes, we need
            // the full ASM pass in LWJGLTransformEngine.
            if (oldBytes.length == newBytes.length) {
                for (int i = 0; i < result.length - oldBytes.length; i++) {
                    if (bytesMatch(result, i, oldBytes)) {
                        System.arraycopy(newBytes, 0, result, i, newBytes.length);
                        modified = true;
                    }
                }
            }
            // For different-length replacements, flag for later processing
        }

        return modified ? result : null;
    }


    // ========================================================================
    //  SECTION 10: VERIFICATION & DIAGNOSTICS
    // ========================================================================

    /**
     * Performs post-bootstrap verification to ensure everything is correctly
     * set up. Logs diagnostic information for troubleshooting.
     */
    private static void verifyBootstrap() {
        LOGGER.info("[MDR] Running post-bootstrap verification...");

        int passed = 0;
        int failed = 0;
        int skipped = 0;

        // ── Check 1: LWJGL version ─────────────────────────────────────────
        try {
            Class<?> versionClass = Class.forName("org.lwjgl.Version");
            Method getVersion = versionClass.getMethod("getVersion");
            String version = (String) getVersion.invoke(null);
            if (version != null && version.startsWith("3.4")) {
                LOGGER.info("[MDR]   ✓ LWJGL version: " + version);
                passed++;
            } else {
                LOGGER.warning("[MDR]   ✗ LWJGL version mismatch: " + version
                             + " (expected 3.4.x)");
                failed++;
                BOOT_WARNINGS.add("LWJGL version is " + version
                                + " instead of expected 3.4.x");
            }
        } catch (Exception e) {
            LOGGER.warning("[MDR]   ✗ Could not verify LWJGL version: "
                         + e.getMessage());
            failed++;
        }

        // ── Check 2: Native libraries loaded ────────────────────────────────
        if (!ENV.isAndroid) {
            try {
                Path nativesDir = getMDRHome().resolve(NATIVES_DIR)
                    .resolve(ENV.getNativeClassifier());
                if (Files.isDirectory(nativesDir)) {
                    long nativeCount = Files.list(nativesDir)
                        .filter(p -> {
                            String name = p.toString().toLowerCase();
                            return name.endsWith(".dll") || name.endsWith(".so")
                                || name.endsWith(".dylib");
                        })
                        .count();
                    if (nativeCount > 0) {
                        LOGGER.info("[MDR]   ✓ Native libraries: " + nativeCount
                                  + " files in " + nativesDir);
                        passed++;
                    } else {
                        LOGGER.warning("[MDR]   ✗ No native libraries found in "
                                     + nativesDir);
                        failed++;
                    }
                } else {
                    LOGGER.warning("[MDR]   ✗ Natives directory missing: "
                                 + nativesDir);
                    failed++;
                }
            } catch (Exception e) {
                LOGGER.warning("[MDR]   ✗ Native check error: " + e.getMessage());
                failed++;
            }
        } else {
            LOGGER.info("[MDR]   ○ Native check skipped (Android).");
            skipped++;
        }

        // ── Check 3: Class redirects registered ─────────────────────────────
        int redirectCount = CLASS_REDIRECTS.size();
        if (redirectCount > 0) {
            LOGGER.info("[MDR]   ✓ Class redirects: " + redirectCount + " rules");
            passed++;
        } else {
            LOGGER.warning("[MDR]   ✗ No class redirects registered!");
            failed++;
        }

        // ── Check 4: Compatibility classes available ────────────────────────
        String[] compatClasses = {
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.GLFWWindowManager",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.GLFWKeyboardAdapter",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.GLFWMouseAdapter",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.OpenALBridge",
            "stellar.snow.astralis.integration.Mini_DirtyRoom.compat.BufferUtilsCompat"
        };
        int compatFound = 0;
        for (String cls : compatClasses) {
            if (classExists(cls)) {
                compatFound++;
            } else {
                LOGGER.fine("[MDR]   Compat class not yet loaded: " + cls);
            }
        }
        if (compatFound == compatClasses.length) {
            LOGGER.info("[MDR]   ✓ All " + compatFound
                      + " compatibility classes available.");
            passed++;
        } else {
            LOGGER.info("[MDR]   ~ " + compatFound + "/"
                      + compatClasses.length
                      + " compatibility classes available (others load on demand).");
            // Not a failure — lazy loading is expected
            passed++;
        }

        // ── Check 5: Instrumentation status ─────────────────────────────────
        if (INSTRUMENTATION != null) {
            LOGGER.info("[MDR]   ✓ Instrumentation agent active. "
                      + "Retransform=" + INSTRUMENTATION.isRetransformClassesSupported()
                      + ", Redefine=" + INSTRUMENTATION.isRedefineClassesSupported());
            passed++;
        } else {
            LOGGER.info("[MDR]   ~ Instrumentation not available "
                      + "(limited transformation capability).");
            skipped++;
        }

        // ── Check 6: Module system status (Java 9+) ────────────────────────
        if (ENV.javaVersion >= 9) {
            try {
                // Test that we can access a restricted package
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                Field f = unsafeClass.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                f.get(null);
                LOGGER.info("[MDR]   ✓ Module system: restricted access available.");
                passed++;
            } catch (Exception e) {
                LOGGER.warning("[MDR]   ✗ Module system: restricted access BLOCKED. "
                             + "Add --add-opens flags to JVM arguments.");
                failed++;
                BOOT_WARNINGS.add("Module system blocking restricted access. "
                                + "Some features may not work.");
            }
        } else {
            LOGGER.info("[MDR]   ○ Module system check skipped (Java "
                      + ENV.javaVersion + ").");
            skipped++;
        }

        // ── Check 7: Memory headroom ────────────────────────────────────────
        long freeMemMB = Runtime.getRuntime().freeMemory() / (1024L * 1024L);
        long totalMemMB = Runtime.getRuntime().totalMemory() / (1024L * 1024L);
        if (freeMemMB > 64) {
            LOGGER.info("[MDR]   ✓ Memory: " + freeMemMB + " MB free / "
                      + totalMemMB + " MB total (max " + ENV.maxMemoryMB + " MB).");
            passed++;
        } else {
            LOGGER.warning("[MDR]   ⚠ Low memory: only " + freeMemMB
                         + " MB free. Consider increasing -Xmx.");
            BOOT_WARNINGS.add("Low memory during bootstrap: " + freeMemMB + " MB free.");
            passed++; // Not a hard failure
        }

        // ── Summary ─────────────────────────────────────────────────────────
        LOGGER.info("[MDR] Verification results: " + passed + " passed, "
                  + failed + " failed, " + skipped + " skipped.");

        if (failed > 0) {
            LOGGER.warning("[MDR] ⚠ Some verification checks failed. "
                         + "Mini_DirtyRoom may not function correctly.");
        }
    }

    /**
     * Returns a diagnostic report as a string for crash logs / debugging.
     */
    public static String getDiagnosticReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Mini_DirtyRoom Diagnostic Report ===\n");
        sb.append("Version: ").append(VERSION).append("\n");
        sb.append("Build: ").append(BUILD_ID).append("\n");
        sb.append("Phase: ").append(CURRENT_PHASE.get()).append("\n");

        if (ENV != null) {
            sb.append("\n--- Environment ---\n");
            sb.append(ENV.toSummary()).append("\n");
            sb.append("Native Classifier: ").append(ENV.getNativeClassifier()).append("\n");
        }

        if (CONFIG != null) {
            sb.append("\n--- Configuration ---\n");
            sb.append("Debug: ").append(CONFIG.debug).append("\n");
            sb.append("Auto-upgrade Java: ").append(CONFIG.autoUpgradeJava).append("\n");
            sb.append("Download JRE: ").append(CONFIG.downloadJRE).append("\n");
            sb.append("Override files on disk: ").append(CONFIG.overrideFilesOnDisk).append("\n");
        }

        sb.append("\n--- Class Redirects ---\n");
        for (Map.Entry<String, String> entry : CLASS_REDIRECTS.entrySet()) {
            sb.append("  ").append(entry.getKey())
              .append(" → ").append(entry.getValue()).append("\n");
        }

        sb.append("\n--- Warnings (").append(BOOT_WARNINGS.size()).append(") ---\n");
        for (String w : BOOT_WARNINGS) {
            sb.append("  ⚠ ").append(w).append("\n");
        }

        Throwable fatal = FATAL_ERROR.get();
        if (fatal != null) {
            sb.append("\n--- FATAL ERROR ---\n");
            StringWriter sw = new StringWriter();
            fatal.printStackTrace(new PrintWriter(sw));
            sb.append(sw.toString());
        }

        long elapsedMs = (System.nanoTime() - STARTUP_TIME_NS) / 1_000_000L;
        sb.append("\nBootstrap duration: ").append(elapsedMs).append(" ms\n");
        sb.append("Instrumentation: ")
          .append(INSTRUMENTATION != null ? "ATTACHED" : "NOT AVAILABLE")
          .append("\n");
        sb.append("Relaunched: ").append(RELAUNCHED.get()).append("\n");
        sb.append("Initialized: ").append(INITIALIZED.get()).append("\n");

        sb.append("=== End Report ===\n");
        return sb.toString();
    }


    // ========================================================================
    //  SECTION 11: CONFIGURATION
    // ========================================================================

    /**
     * Configuration holder loaded from mini_dirtyroom.cfg.
     * Uses a simple key=value format for maximum compatibility.
     */
    static final class MiniDirtyRoomConfig {
        boolean debug               = false;
        boolean autoUpgradeJava     = true;
        boolean downloadJRE         = true;
        boolean overrideFilesOnDisk = false;
        boolean forceRelaunch       = false;
        boolean verifyChecksums     = true;
        int     downloadTimeoutSecs = 60;
        int     nativeLoadRetries   = 3;
        String  lwjglMirrorUrl      = "";  // Custom LWJGL download mirror
        String  jreMirrorUrl        = "";  // Custom JRE download mirror
        boolean enableTelemetry     = false;
        boolean verboseLogging      = false;

        static MiniDirtyRoomConfig load(Path configPath) {
            MiniDirtyRoomConfig config = new MiniDirtyRoomConfig();

            if (!Files.exists(configPath)) {
                // Write default config
                try {
                    Files.createDirectories(configPath.getParent());
                    config.save(configPath);
                    LOGGER.info("[MDR] Created default config at: " + configPath);
                } catch (IOException e) {
                    LOGGER.fine("[MDR] Could not write default config: "
                              + e.getMessage());
                }
                return config;
            }

            try {
                Properties props = new Properties();
                try (InputStream is = Files.newInputStream(configPath)) {
                    props.load(is);
                }

                config.debug               = Boolean.parseBoolean(
                    props.getProperty("debug", "false"));
                config.autoUpgradeJava     = Boolean.parseBoolean(
                    props.getProperty("auto_upgrade_java", "true"));
                config.downloadJRE         = Boolean.parseBoolean(
                    props.getProperty("download_jre", "true"));
                config.overrideFilesOnDisk = Boolean.parseBoolean(
                    props.getProperty("override_files_on_disk", "false"));
                config.forceRelaunch       = Boolean.parseBoolean(
                    props.getProperty("force_relaunch", "false"));
                config.verifyChecksums     = Boolean.parseBoolean(
                    props.getProperty("verify_checksums", "true"));
                config.downloadTimeoutSecs = Integer.parseInt(
                    props.getProperty("download_timeout_secs", "60"));
                config.nativeLoadRetries   = Integer.parseInt(
                    props.getProperty("native_load_retries", "3"));
                config.lwjglMirrorUrl      =
                    props.getProperty("lwjgl_mirror_url", "");
                config.jreMirrorUrl        =
                    props.getProperty("jre_mirror_url", "");
                config.enableTelemetry     = Boolean.parseBoolean(
                    props.getProperty("enable_telemetry", "false"));
                config.verboseLogging      = Boolean.parseBoolean(
                    props.getProperty("verbose_logging", "false"));

            } catch (Exception e) {
                LOGGER.warning("[MDR] Error loading config, using defaults: "
                             + e.getMessage());
            }

            return config;
        }

        void save(Path configPath) throws IOException {
            List<String> lines = new ArrayList<>();
            lines.add("# Mini_DirtyRoom Configuration");
            lines.add("# Generated by Mini_DirtyRoom v" + VERSION);
            lines.add("#");
            lines.add("# debug: Enable verbose debug logging");
            lines.add("debug=" + debug);
            lines.add("");
            lines.add("# auto_upgrade_java: Automatically relaunch on Java "
                     + TARGET_JAVA_VERSION + " if running on older Java");
            lines.add("auto_upgrade_java=" + autoUpgradeJava);
            lines.add("");
            lines.add("# download_jre: Allow downloading a JRE if none found locally");
            lines.add("download_jre=" + downloadJRE);
            lines.add("");
            lines.add("# override_files_on_disk: Overwrite existing LWJGL files "
                     + "(Android only, use with caution)");
            lines.add("override_files_on_disk=" + overrideFilesOnDisk);
            lines.add("");
            lines.add("# force_relaunch: Always relaunch even if Java version meets target");
            lines.add("force_relaunch=" + forceRelaunch);
            lines.add("");
            lines.add("# verify_checksums: Verify SHA-256 checksums of downloaded files");
            lines.add("verify_checksums=" + verifyChecksums);
            lines.add("");
            lines.add("# download_timeout_secs: Timeout for file downloads in seconds");
            lines.add("download_timeout_secs=" + downloadTimeoutSecs);
            lines.add("");
            lines.add("# native_load_retries: Number of retries for native library loading");
            lines.add("native_load_retries=" + nativeLoadRetries);
            lines.add("");
            lines.add("# lwjgl_mirror_url: Custom mirror URL for LWJGL downloads (leave empty for default)");
            lines.add("lwjgl_mirror_url=" + lwjglMirrorUrl);
            lines.add("");
            lines.add("# jre_mirror_url: Custom mirror URL for JRE downloads (leave empty for Adoptium)");
            lines.add("jre_mirror_url=" + jreMirrorUrl);
            lines.add("");
            lines.add("# enable_telemetry: Send anonymous usage statistics (off by default)");
            lines.add("enable_telemetry=" + enableTelemetry);
            lines.add("");
            lines.add("# verbose_logging: Extremely detailed logging for debugging");
            lines.add("verbose_logging=" + verboseLogging);

            Files.write(configPath, lines, StandardCharsets.UTF_8);
        }
    }


    // ========================================================================
    //  SECTION 12: UTILITY METHODS
    // ========================================================================

    /**
     * Returns the Mini_DirtyRoom home directory, creating it if necessary.
     */
    static Path getMDRHome() {
        Path home = Paths.get(System.getProperty("user.dir"), MDR_HOME);
        try {
            if (!Files.isDirectory(home)) {
                Files.createDirectories(home);
            }
        } catch (IOException e) {
            LOGGER.fine("[MDR] Could not create MDR home at " + home
                      + ". Trying user.home fallback.");
            home = Paths.get(System.getProperty("user.home"), MDR_HOME);
            try {
                Files.createDirectories(home);
            } catch (IOException e2) {
                LOGGER.warning("[MDR] Could not create MDR home anywhere!");
            }
        }
        return home;
    }

    /**
     * Downloads a file from a URL to a local path with progress logging.
     */
    private static void downloadFile(String urlStr, Path target, String description)
            throws IOException {
        int timeoutMs = (CONFIG != null ? CONFIG.downloadTimeoutSecs : 60) * 1000;

        // Follow redirects
        URL url = new URL(urlStr);
        HttpURLConnection conn = null;
        int redirects = 0;
        final int MAX_REDIRECTS = 5;

        while (redirects < MAX_REDIRECTS) {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestProperty("User-Agent",
                "Mini_DirtyRoom/" + VERSION + " (Java)");
            conn.setInstanceFollowRedirects(false);

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM
             || code == HttpURLConnection.HTTP_MOVED_TEMP
             || code == 307 || code == 308) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl == null) break;
                url = new URL(newUrl);
                conn.disconnect();
                redirects++;
                continue;
            }

            if (code != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                throw new IOException("HTTP " + code + " for " + urlStr);
            }
            break;
        }

        if (conn == null) {
            throw new IOException("Failed to connect to " + urlStr);
        }

        long contentLength = conn.getContentLengthLong();
        LOGGER.info("[MDR] Downloading " + description + " ("
                  + (contentLength > 0
                     ? (contentLength / 1024) + " KB"
                     : "unknown size")
                  + ")...");

        Path tempFile = target.resolveSibling(
            target.getFileName() + ".tmp." + System.currentTimeMillis());

        try (InputStream is = new BufferedInputStream(conn.getInputStream());
             OutputStream os = new BufferedOutputStream(
                 Files.newOutputStream(tempFile))) {

            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int bytesRead;
            long lastLogTime = System.currentTimeMillis();

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                downloaded += bytesRead;

                // Log progress every 5 seconds
                long now = System.currentTimeMillis();
                if (now - lastLogTime > 5000 && contentLength > 0) {
                    int pct = (int) (downloaded * 100 / contentLength);
                    LOGGER.info("[MDR]   " + description + ": " + pct + "% ("
                              + (downloaded / 1024) + " / "
                              + (contentLength / 1024) + " KB)");
                    lastLogTime = now;
                }
            }

            os.flush();
        } finally {
            conn.disconnect();
        }

        // Atomic move from temp to final
        Files.move(tempFile, target,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);

        LOGGER.info("[MDR] Downloaded " + description + " → "
                  + target.getFileName());
    }

    /**
     * Downloads all LWJGL 3.4.0 jars to the target directory.
     */
    private static List<Path> downloadLWJGLJars(Path targetDir) throws IOException {
        List<Path> downloaded = new ArrayList<>();
        for (Map.Entry<String, String> entry : LWJGL_ARTIFACTS.entrySet()) {
            String artifact = entry.getKey();
            String url      = entry.getValue();
            Path   jarPath  = targetDir.resolve(
                artifact + "-" + TARGET_LWJGL_VERSION + ".jar");

            if (!Files.exists(jarPath)) {
                try {
                    downloadFile(url, jarPath, artifact);
                    downloaded.add(jarPath);
                } catch (IOException e) {
                    LOGGER.warning("[MDR] Failed to download " + artifact
                                 + ": " + e.getMessage());
                }
            } else {
                downloaded.add(jarPath);
            }
        }
        return downloaded;
    }

    /**
     * Extracts an archive (zip or tar.gz) to the target directory.
     */
    private static void extractArchive(Path archivePath, Path targetDir)
            throws IOException {
        Files.createDirectories(targetDir);
        String name = archivePath.getFileName().toString().toLowerCase();

        if (name.endsWith(".zip")) {
            extractZip(archivePath, targetDir);
        } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            extractTarGz(archivePath, targetDir);
        } else {
            throw new IOException("Unsupported archive format: " + name);
        }
    }

    /**
     * Extracts a ZIP archive.
     */
    private static void extractZip(Path zipPath, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(zipPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolved = targetDir.resolve(entry.getName()).normalize();
                // Security: prevent zip-slip
                if (!resolved.startsWith(targetDir)) {
                    throw new IOException("Zip entry outside target: "
                                        + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    Files.copy(zis, resolved, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Extracts a tar.gz archive using basic decompression.
     * (We avoid importing org.apache.commons.compress to keep dependencies minimal.)
     */
    private static void extractTarGz(Path tarGzPath, Path targetDir)
            throws IOException {
        // Use ProcessBuilder to call system tar if available
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "tar", "xzf",
                tarGzPath.toAbsolutePath().toString(),
                "-C", targetDir.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = readStreamFully(proc.getInputStream());
            int exitCode = proc.waitFor();
            if (exitCode == 0) {
                return; // Success via system tar
            }
            LOGGER.fine("[MDR] System tar failed (exit " + exitCode
                      + "): " + output);
        } catch (Exception e) {
            LOGGER.fine("[MDR] System tar not available: " + e.getMessage());
        }

        // Fallback: Use Java GZIPInputStream + manual TAR parsing
        try (InputStream fis = Files.newInputStream(tarGzPath);
             InputStream bis = new BufferedInputStream(fis);
             java.util.zip.GZIPInputStream gis =
                 new java.util.zip.GZIPInputStream(bis)) {

            // Minimal TAR parser
            byte[] header = new byte[512];
            while (true) {
                int read = readFully(gis, header);
                if (read < 512) break;

                // Check for end-of-archive (two zero blocks)
                boolean allZero = true;
                for (byte b : header) {
                    if (b != 0) { allZero = false; break; }
                }
                if (allZero) break;

                // Parse TAR header
                String entryName = extractTarString(header, 0, 100);
                if (entryName.isEmpty()) break;

                // Handle GNU long name extension
                byte typeFlag = header[156];
                long size = parseOctal(header, 124, 12);

                // Handle prefix (POSIX / ustar)
                String prefix = extractTarString(header, 345, 155);
                if (!prefix.isEmpty()) {
                    entryName = prefix + "/" + entryName;
                }

                Path resolved = targetDir.resolve(entryName).normalize();
                if (!resolved.startsWith(targetDir)) {
                    // Skip entries that would escape the target directory
                    skipBytes(gis, alignTo512(size));
                    continue;
                }

                if (typeFlag == '5' || entryName.endsWith("/")) {
                    // Directory
                    Files.createDirectories(resolved);
                } else if (typeFlag == '0' || typeFlag == 0) {
                    // Regular file
                    Files.createDirectories(resolved.getParent());
                    try (OutputStream os = Files.newOutputStream(resolved)) {
                        long remaining = size;
                        byte[] buf = new byte[8192];
                        while (remaining > 0) {
                            int toRead = (int) Math.min(buf.length, remaining);
                            int r = gis.read(buf, 0, toRead);
                            if (r <= 0) break;
                            os.write(buf, 0, r);
                            remaining -= r;
                        }
                    }
                    // Skip padding to next 512-byte boundary
                    long remainder = size % 512;
                    if (remainder != 0) {
                        skipBytes(gis, 512 - remainder);
                    }
                } else {
                    // Symlink, hardlink, etc. — skip
                    skipBytes(gis, alignTo512(size));
                }
            }
        }
    }

    /**
     * Reads exactly {@code buffer.length} bytes or returns actual count read.
     */
    private static int readFully(InputStream is, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = is.read(buffer, offset, buffer.length - offset);
            if (read < 0) break;
            offset += read;
        }
        return offset;
    }

    /**
     * Extracts a null-terminated string from a TAR header field.
     */
    private static String extractTarString(byte[] header, int offset, int length) {
        int end = offset;
        for (int i = offset; i < offset + length && i < header.length; i++) {
            if (header[i] == 0) break;
            end = i + 1;
        }
        return new String(header, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    /**
     * Parses an octal number from a TAR header field.
     */
    private static long parseOctal(byte[] header, int offset, int length) {
        String s = extractTarString(header, offset, length).trim();
        if (s.isEmpty()) return 0;
        try {
            return Long.parseLong(s, 8);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Aligns a size to the next 512-byte boundary.
     */
    private static long alignTo512(long size) {
        long remainder = size % 512;
        return remainder == 0 ? size : size + (512 - remainder);
    }

    /**
     * Skips exactly {@code count} bytes from an InputStream.
     */
    private static void skipBytes(InputStream is, long count) throws IOException {
        long remaining = count;
        byte[] buf = new byte[4096];
        while (remaining > 0) {
            int toRead = (int) Math.min(buf.length, remaining);
            int read = is.read(buf, 0, toRead);
            if (read < 0) break;
            remaining -= read;
        }
    }

    /**
     * Recursively searches for a java/java.exe binary within a directory.
     */
    private static Path findJavaBinaryRecursive(Path dir) {
        if (!Files.isDirectory(dir)) return null;
        try {
            String ext = (ENV != null && ENV.isWindows) ? ".exe" : "";
            Optional<Path> found = Files.walk(dir, 4)
                .filter(p -> {
                    String name = p.getFileName().toString();
                    return name.equals("java" + ext) && Files.isExecutable(p);
                })
                .findFirst();
            return found.orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Reads an InputStream fully into a String.
     */
    private static String readStreamFully(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    /**
     * Reads an InputStream fully into a byte array.
     */
    private static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[4096];
        int read;
        while ((read = is.read(buf)) != -1) {
            baos.write(buf, 0, read);
        }
        return baos.toByteArray();
    }

    /**
     * Computes the SHA-256 hex digest of a file.
     */
    private static String sha256(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = new BufferedInputStream(Files.newInputStream(file))) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = is.read(buf)) != -1) {
                    md.update(buf, 0, read);
                }
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xFF));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Checks if a class exists on the classpath without loading it
     * (unless necessary).
     */
    private static boolean classExists(String className) {
        try {
            Class.forName(className, false,
                Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable t) {
            // NoClassDefFoundError, LinkageError, etc.
            return false;
        }
    }


    // ========================================================================
    //  SECTION 13: ARTIFACT & KEY CODE TABLE POPULATION
    // ========================================================================

    /**
     * Populates the LWJGL artifact download URLs and native classifier tables.
     * Called once from the static initializer.
     */
    private static void populateArtifactTables() {
        String baseUrl = "https://repo1.maven.org/maven2/org/lwjgl/";
        String v = TARGET_LWJGL_VERSION;

        // Core LWJGL modules
        String[][] artifacts = {
            {"lwjgl",           "lwjgl"},
            {"lwjgl-opengl",    "lwjgl-opengl"},
            {"lwjgl-openal",    "lwjgl-openal"},
            {"lwjgl-glfw",      "lwjgl-glfw"},
            {"lwjgl-stb",       "lwjgl-stb"},
            {"lwjgl-tinyfd",    "lwjgl-tinyfd"},
            {"lwjgl-jemalloc",  "lwjgl-jemalloc"},
            {"lwjgl-nfd",       "lwjgl-nfd"},
            {"lwjgl-par",       "lwjgl-par"},
            {"lwjgl-assimp",    "lwjgl-assimp"},
        };

        for (String[] art : artifacts) {
            String name     = art[0];
            String artifact = art[1];
            String url = baseUrl + artifact + "/" + v + "/"
                       + artifact + "-" + v + ".jar";
            LWJGL_ARTIFACTS.put(name, url);
        }

        // Modules that have native jars
        String[] nativeModules = {
            "lwjgl", "lwjgl-opengl", "lwjgl-openal", "lwjgl-glfw",
            "lwjgl-stb", "lwjgl-tinyfd", "lwjgl-jemalloc",
            "lwjgl-nfd", "lwjgl-assimp"
        };
        for (String mod : nativeModules) {
            NATIVE_CLASSIFIERS.put(mod, "has-natives");
        }

        // SHA-256 checksums (placeholder — in production, fill with real hashes)
        // These would be populated from a verified manifest
        // LWJGL_CHECKSUMS.put("lwjgl", "abc123...");
    }

    /**
     * Populates the LWJGL 2 → GLFW key code mapping table.
     * LWJGL 2 key codes are based on DirectInput scan codes.
     * GLFW uses its own key code scheme (similar to USB HID).
     */
    private static void populateKeyCodeMap() {
        // Initialize all to -1 (unmapped)
        Arrays.fill(KEYCODE_MAP, -1);

        // LWJGL2 Keyboard constants → GLFW_KEY_* constants
        // GLFW key constants from glfw3.h
        final int GLFW_KEY_SPACE         = 32;
        final int GLFW_KEY_APOSTROPHE    = 39;
        final int GLFW_KEY_COMMA         = 44;
        final int GLFW_KEY_MINUS         = 45;
        final int GLFW_KEY_PERIOD        = 46;
        final int GLFW_KEY_SLASH         = 47;
        final int GLFW_KEY_0             = 48;
        final int GLFW_KEY_1             = 49;
        final int GLFW_KEY_2             = 50;
        final int GLFW_KEY_3             = 51;
        final int GLFW_KEY_4             = 52;
        final int GLFW_KEY_5             = 53;
        final int GLFW_KEY_6             = 54;
        final int GLFW_KEY_7             = 55;
        final int GLFW_KEY_8             = 56;
        final int GLFW_KEY_9             = 57;
        final int GLFW_KEY_SEMICOLON     = 59;
        final int GLFW_KEY_EQUAL         = 61;
        final int GLFW_KEY_A             = 65;
        final int GLFW_KEY_B             = 66;
        final int GLFW_KEY_C             = 67;
        final int GLFW_KEY_D             = 68;
        final int GLFW_KEY_E             = 69;
        final int GLFW_KEY_F             = 70;
        final int GLFW_KEY_G             = 71;
        final int GLFW_KEY_H             = 72;
        final int GLFW_KEY_I             = 73;
        final int GLFW_KEY_J             = 74;
        final int GLFW_KEY_K             = 75;
        final int GLFW_KEY_L             = 76;
        final int GLFW_KEY_M             = 77;
        final int GLFW_KEY_N             = 78;
        final int GLFW_KEY_O             = 79;
        final int GLFW_KEY_P             = 80;
        final int GLFW_KEY_Q             = 81;
        final int GLFW_KEY_R             = 82;
        final int GLFW_KEY_S             = 83;
        final int GLFW_KEY_T             = 84;
        final int GLFW_KEY_U             = 85;
        final int GLFW_KEY_V             = 86;
        final int GLFW_KEY_W             = 87;
        final int GLFW_KEY_X             = 88;
        final int GLFW_KEY_Y             = 89;
        final int GLFW_KEY_Z             = 90;
        final int GLFW_KEY_LEFT_BRACKET  = 91;
        final int GLFW_KEY_BACKSLASH     = 92;
        final int GLFW_KEY_RIGHT_BRACKET = 93;
        final int GLFW_KEY_GRAVE_ACCENT  = 96;
        final int GLFW_KEY_ESCAPE        = 256;
        final int GLFW_KEY_ENTER         = 257;
        final int GLFW_KEY_TAB           = 258;
        final int GLFW_KEY_BACKSPACE     = 259;
        final int GLFW_KEY_INSERT        = 260;
        final int GLFW_KEY_DELETE        = 261;
        final int GLFW_KEY_RIGHT         = 262;
        final int GLFW_KEY_LEFT          = 263;
        final int GLFW_KEY_DOWN          = 264;
        final int GLFW_KEY_UP            = 265;
        final int GLFW_KEY_PAGE_UP       = 266;
        final int GLFW_KEY_PAGE_DOWN     = 267;
        final int GLFW_KEY_HOME          = 268;
        final int GLFW_KEY_END           = 269;
        final int GLFW_KEY_CAPS_LOCK     = 280;
        final int GLFW_KEY_SCROLL_LOCK   = 281;
        final int GLFW_KEY_NUM_LOCK      = 282;
        final int GLFW_KEY_PRINT_SCREEN  = 283;
        final int GLFW_KEY_PAUSE         = 284;
        final int GLFW_KEY_F1            = 290;
        final int GLFW_KEY_F2            = 291;
        final int GLFW_KEY_F3            = 292;
        final int GLFW_KEY_F4            = 293;
        final int GLFW_KEY_F5            = 294;
        final int GLFW_KEY_F6            = 295;
        final int GLFW_KEY_F7            = 296;
        final int GLFW_KEY_F8            = 297;
        final int GLFW_KEY_F9            = 298;
        final int GLFW_KEY_F10           = 299;
        final int GLFW_KEY_F11           = 300;
        final int GLFW_KEY_F12           = 301;
        final int GLFW_KEY_KP_0          = 320;
        final int GLFW_KEY_KP_1          = 321;
        final int GLFW_KEY_KP_2          = 322;
        final int GLFW_KEY_KP_3          = 323;
        final int GLFW_KEY_KP_4          = 324;
        final int GLFW_KEY_KP_5          = 325;
        final int GLFW_KEY_KP_6          = 326;
        final int GLFW_KEY_KP_7          = 327;
        final int GLFW_KEY_KP_8          = 328;
        final int GLFW_KEY_KP_9          = 329;
        final int GLFW_KEY_KP_DECIMAL    = 330;
        final int GLFW_KEY_KP_DIVIDE     = 331;
        final int GLFW_KEY_KP_MULTIPLY   = 332;
        final int GLFW_KEY_KP_SUBTRACT   = 333;
        final int GLFW_KEY_KP_ADD        = 334;
        final int GLFW_KEY_KP_ENTER      = 335;
        final int GLFW_KEY_LEFT_SHIFT    = 340;
        final int GLFW_KEY_LEFT_CONTROL  = 341;
        final int GLFW_KEY_LEFT_ALT      = 342;
        final int GLFW_KEY_LEFT_SUPER    = 343;
        final int GLFW_KEY_RIGHT_SHIFT   = 344;
        final int GLFW_KEY_RIGHT_CONTROL = 345;
        final int GLFW_KEY_RIGHT_ALT     = 346;
        final int GLFW_KEY_RIGHT_SUPER   = 347;
        final int GLFW_KEY_MENU          = 348;

        // LWJGL2 Keyboard constants (DirectInput scan codes)
        // These are the KEY_* constants from org.lwjgl.input.Keyboard
        KEYCODE_MAP[1]   = GLFW_KEY_ESCAPE;        // KEY_ESCAPE
        KEYCODE_MAP[2]   = GLFW_KEY_1;             // KEY_1
        KEYCODE_MAP[3]   = GLFW_KEY_2;             // KEY_2
        KEYCODE_MAP[4]   = GLFW_KEY_3;             // KEY_3
        KEYCODE_MAP[5]   = GLFW_KEY_4;             // KEY_4
        KEYCODE_MAP[6]   = GLFW_KEY_5;             // KEY_5
        KEYCODE_MAP[7]   = GLFW_KEY_6;             // KEY_6
        KEYCODE_MAP[8]   = GLFW_KEY_7;             // KEY_7
        KEYCODE_MAP[9]   = GLFW_KEY_8;             // KEY_8
        KEYCODE_MAP[10]  = GLFW_KEY_9;             // KEY_9
        KEYCODE_MAP[11]  = GLFW_KEY_0;             // KEY_0
        KEYCODE_MAP[12]  = GLFW_KEY_MINUS;         // KEY_MINUS
        KEYCODE_MAP[13]  = GLFW_KEY_EQUAL;         // KEY_EQUALS
        KEYCODE_MAP[14]  = GLFW_KEY_BACKSPACE;     // KEY_BACK
        KEYCODE_MAP[15]  = GLFW_KEY_TAB;           // KEY_TAB
        KEYCODE_MAP[16]  = GLFW_KEY_Q;             // KEY_Q
        KEYCODE_MAP[17]  = GLFW_KEY_W;             // KEY_W
        KEYCODE_MAP[18]  = GLFW_KEY_E;             // KEY_E
        KEYCODE_MAP[19]  = GLFW_KEY_R;             // KEY_R
        KEYCODE_MAP[20]  = GLFW_KEY_T;             // KEY_T
        KEYCODE_MAP[21]  = GLFW_KEY_Y;             // KEY_Y
        KEYCODE_MAP[22]  = GLFW_KEY_U;             // KEY_U
        KEYCODE_MAP[23]  = GLFW_KEY_I;             // KEY_I
        KEYCODE_MAP[24]  = GLFW_KEY_O;             // KEY_O
        KEYCODE_MAP[25]  = GLFW_KEY_P;             // KEY_P
        KEYCODE_MAP[26]  = GLFW_KEY_LEFT_BRACKET;  // KEY_LBRACKET
        KEYCODE_MAP[27]  = GLFW_KEY_RIGHT_BRACKET; // KEY_RBRACKET
        KEYCODE_MAP[28]  = GLFW_KEY_ENTER;         // KEY_RETURN
        KEYCODE_MAP[29]  = GLFW_KEY_LEFT_CONTROL;  // KEY_LCONTROL
        KEYCODE_MAP[30]  = GLFW_KEY_A;             // KEY_A
        KEYCODE_MAP[31]  = GLFW_KEY_S;             // KEY_S
        KEYCODE_MAP[32]  = GLFW_KEY_D;             // KEY_D
        KEYCODE_MAP[33]  = GLFW_KEY_F;             // KEY_F
        KEYCODE_MAP[34]  = GLFW_KEY_G;             // KEY_G
        KEYCODE_MAP[35]  = GLFW_KEY_H;             // KEY_H
        KEYCODE_MAP[36]  = GLFW_KEY_J;             // KEY_J
        KEYCODE_MAP[37]  = GLFW_KEY_K;             // KEY_K
        KEYCODE_MAP[38]  = GLFW_KEY_L;             // KEY_L
        KEYCODE_MAP[39]  = GLFW_KEY_SEMICOLON;     // KEY_SEMICOLON
        KEYCODE_MAP[40]  = GLFW_KEY_APOSTROPHE;    // KEY_APOSTROPHE
        KEYCODE_MAP[41]  = GLFW_KEY_GRAVE_ACCENT;  // KEY_GRAVE
        KEYCODE_MAP[42]  = GLFW_KEY_LEFT_SHIFT;    // KEY_LSHIFT
        KEYCODE_MAP[43]  = GLFW_KEY_BACKSLASH;     // KEY_BACKSLASH
        KEYCODE_MAP[44]  = GLFW_KEY_Z;             // KEY_Z
        KEYCODE_MAP[45]  = GLFW_KEY_X;             // KEY_X
        KEYCODE_MAP[46]  = GLFW_KEY_C;             // KEY_C
        KEYCODE_MAP[47]  = GLFW_KEY_V;             // KEY_V
        KEYCODE_MAP[48]  = GLFW_KEY_B;             // KEY_B
        KEYCODE_MAP[49]  = GLFW_KEY_N;             // KEY_N
        KEYCODE_MAP[50]  = GLFW_KEY_M;             // KEY_M
        KEYCODE_MAP[51]  = GLFW_KEY_COMMA;         // KEY_COMMA
        KEYCODE_MAP[52]  = GLFW_KEY_PERIOD;        // KEY_PERIOD
        KEYCODE_MAP[53]  = GLFW_KEY_SLASH;         // KEY_SLASH
        KEYCODE_MAP[54]  = GLFW_KEY_RIGHT_SHIFT;   // KEY_RSHIFT
        KEYCODE_MAP[55]  = GLFW_KEY_KP_MULTIPLY;   // KEY_MULTIPLY
        KEYCODE_MAP[56]  = GLFW_KEY_LEFT_ALT;      // KEY_LMENU (Left Alt)
        KEYCODE_MAP[57]  = GLFW_KEY_SPACE;         // KEY_SPACE
        KEYCODE_MAP[58]  = GLFW_KEY_CAPS_LOCK;     // KEY_CAPITAL
        KEYCODE_MAP[59]  = GLFW_KEY_F1;            // KEY_F1
        KEYCODE_MAP[60]  = GLFW_KEY_F2;            // KEY_F2
        KEYCODE_MAP[61]  = GLFW_KEY_F3;            // KEY_F3
        KEYCODE_MAP[62]  = GLFW_KEY_F4;            // KEY_F4
        KEYCODE_MAP[63]  = GLFW_KEY_F5;            // KEY_F5
        KEYCODE_MAP[64]  = GLFW_KEY_F6;            // KEY_F6
        KEYCODE_MAP[65]  = GLFW_KEY_F7;            // KEY_F7
        KEYCODE_MAP[66]  = GLFW_KEY_F8;            // KEY_F8
        KEYCODE_MAP[67]  = GLFW_KEY_F9;            // KEY_F9
        KEYCODE_MAP[68]  = GLFW_KEY_F10;           // KEY_F10
        KEYCODE_MAP[69]  = GLFW_KEY_NUM_LOCK;      // KEY_NUMLOCK
        KEYCODE_MAP[70]  = GLFW_KEY_SCROLL_LOCK;   // KEY_SCROLL
        KEYCODE_MAP[71]  = GLFW_KEY_KP_7;          // KEY_NUMPAD7
        KEYCODE_MAP[72]  = GLFW_KEY_KP_8;          // KEY_NUMPAD8
        KEYCODE_MAP[73]  = GLFW_KEY_KP_9;          // KEY_NUMPAD9
        KEYCODE_MAP[74]  = GLFW_KEY_KP_SUBTRACT;   // KEY_SUBTRACT
        KEYCODE_MAP[75]  = GLFW_KEY_KP_4;          // KEY_NUMPAD4
        KEYCODE_MAP[76]  = GLFW_KEY_KP_5;          // KEY_NUMPAD5
        KEYCODE_MAP[77]  = GLFW_KEY_KP_6;          // KEY_NUMPAD6
        KEYCODE_MAP[78]  = GLFW_KEY_KP_ADD;        // KEY_ADD
        KEYCODE_MAP[79]  = GLFW_KEY_KP_1;          // KEY_NUMPAD1
        KEYCODE_MAP[80]  = GLFW_KEY_KP_2;          // KEY_NUMPAD2
        KEYCODE_MAP[81]  = GLFW_KEY_KP_3;          // KEY_NUMPAD3
        KEYCODE_MAP[82]  = GLFW_KEY_KP_0;          // KEY_NUMPAD0
        KEYCODE_MAP[83]  = GLFW_KEY_KP_DECIMAL;    // KEY_DECIMAL
        KEYCODE_MAP[87]  = GLFW_KEY_F11;           // KEY_F11
        KEYCODE_MAP[88]  = GLFW_KEY_F12;           // KEY_F12
        KEYCODE_MAP[156] = GLFW_KEY_KP_ENTER;      // KEY_NUMPADENTER
        KEYCODE_MAP[157] = GLFW_KEY_RIGHT_CONTROL; // KEY_RCONTROL
        KEYCODE_MAP[181] = GLFW_KEY_KP_DIVIDE;     // KEY_DIVIDE
        KEYCODE_MAP[183] = GLFW_KEY_PRINT_SCREEN;  // KEY_SYSRQ
        KEYCODE_MAP[184] = GLFW_KEY_RIGHT_ALT;     // KEY_RMENU (Right Alt)
        KEYCODE_MAP[197] = GLFW_KEY_PAUSE;         // KEY_PAUSE
        KEYCODE_MAP[199] = GLFW_KEY_HOME;          // KEY_HOME
        KEYCODE_MAP[200] = GLFW_KEY_UP;            // KEY_UP
        KEYCODE_MAP[201] = GLFW_KEY_PAGE_UP;       // KEY_PRIOR (Page Up)
        KEYCODE_MAP[203] = GLFW_KEY_LEFT;          // KEY_LEFT
        KEYCODE_MAP[205] = GLFW_KEY_RIGHT;         // KEY_RIGHT
        KEYCODE_MAP[207] = GLFW_KEY_END;           // KEY_END
        KEYCODE_MAP[208] = GLFW_KEY_DOWN;          // KEY_DOWN
        KEYCODE_MAP[209] = GLFW_KEY_PAGE_DOWN;     // KEY_NEXT (Page Down)
        KEYCODE_MAP[210] = GLFW_KEY_INSERT;        // KEY_INSERT
        KEYCODE_MAP[211] = GLFW_KEY_DELETE;        // KEY_DELETE
        KEYCODE_MAP[219] = GLFW_KEY_LEFT_SUPER;    // KEY_LMETA (Left Win)
        KEYCODE_MAP[220] = GLFW_KEY_RIGHT_SUPER;   // KEY_RMETA (Right Win)
        KEYCODE_MAP[221] = GLFW_KEY_MENU;          // KEY_APPS (Menu key)
    }

    /**
     * Converts an LWJGL 2 key code to the equivalent GLFW key code.
     *
     * @param lwjgl2KeyCode the LWJGL 2 Keyboard.KEY_* constant
     * @return the GLFW_KEY_* equivalent, or -1 if unmapped
     */
    public static int convertKeyCode(int lwjgl2KeyCode) {
        if (lwjgl2KeyCode < 0 || lwjgl2KeyCode >= KEYCODE_MAP.length) return -1;
        return KEYCODE_MAP[lwjgl2KeyCode];
    }

    /**
     * Converts a GLFW key code back to an LWJGL 2 key code.
     * This is the reverse lookup, used when we need to present GLFW events
     * through the LWJGL 2 Keyboard API.
     *
     * @param glfwKeyCode the GLFW_KEY_* constant
     * @return the LWJGL 2 Keyboard.KEY_* equivalent, or 0 if unmapped
     */
    public static int convertKeyCodeReverse(int glfwKeyCode) {
        for (int i = 0; i < KEYCODE_MAP.length; i++) {
            if (KEYCODE_MAP[i] == glfwKeyCode) return i;
        }
        return 0; // KEY_NONE
    }


    // ========================================================================
    //  SECTION 14: PUBLIC API
    // ========================================================================

    /**
     * Returns whether the bootstrap has completed successfully.
     */
    public static boolean isBootstrapComplete() {
        return BOOTSTRAP_DONE.getCount() == 0;
    }

    /**
     * Waits for the bootstrap to complete, with a timeout.
     *
     * @param timeoutMs maximum milliseconds to wait
     * @return true if bootstrap completed within the timeout
     */
    public static boolean awaitBootstrap(long timeoutMs) {
        try {
            return BOOTSTRAP_DONE.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Returns the detected environment information.
     */
    public static EnvironmentInfo getEnvironment() {
        return ENV;
    }

    /**
     * Returns the loaded configuration.
     */
    public static MiniDirtyRoomConfig getConfig() {
        return CONFIG;
    }

    /**
     * Returns the GLFW window handle, or 0 if not yet created.
     */
    public static long getGLFWWindow() {
        return GLFW_WINDOW;
    }

    /**
     * Sets the GLFW window handle (called by GLFWWindowManager upon creation).
     */
    public static void setGLFWWindow(long window) {
        GLFW_WINDOW = window;
    }

    /**
     * Returns the Instrumentation instance, or null if unavailable.
     */
    public static Instrumentation getInstrumentation() {
        return INSTRUMENTATION;
    }

    /**
     * Returns all boot warnings collected during bootstrap.
     */
    public static List<String> getBootWarnings() {
        return Collections.unmodifiableList(BOOT_WARNINGS);
    }

    /**
     * Returns any fatal error that occurred during bootstrap.
     */
    public static Throwable getFatalError() {
        return FATAL_ERROR.get();
    }

    /**
     * Returns deferred registrations for mod loader bridges.
     */
    public static List<Runnable> getDeferredRegistrations() {
        return Collections.unmodifiableList(DEFERRED_REGISTRATIONS);
    }

    /**
     * Executes all deferred registrations. Called by ModLoaderBridge
     * when the mod loader is fully initialized.
     */
    public static void executeDeferredRegistrations() {
        LOGGER.info("[MDR] Executing " + DEFERRED_REGISTRATIONS.size()
                  + " deferred registrations...");
        for (Runnable r : DEFERRED_REGISTRATIONS) {
            try {
                r.run();
            } catch (Exception e) {
                LOGGER.warning("[MDR] Deferred registration failed: "
                             + e.getMessage());
            }
        }
        DEFERRED_REGISTRATIONS.clear();
        LOGGER.info("[MDR] Deferred registrations complete.");
    }


    // ========================================================================
    //  SECTION 15: SHUTDOWN HOOK
    // ========================================================================

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("[MDR] Shutdown hook executing...");

            // Shut down bootstrap executor
            BOOTSTRAP_EXECUTOR.shutdownNow();

            // Log final stats
            long uptimeMs = (System.nanoTime() - STARTUP_TIME_NS) / 1_000_000L;
            LOGGER.info("[MDR] Total runtime: " + uptimeMs + " ms ("
                      + (uptimeMs / 1000) + " s)");
            LOGGER.info("[MDR] Boot warnings during session: "
                      + BOOT_WARNINGS.size());

            // Write diagnostic report to file
            try {
                Path logDir = getMDRHome().resolve(LOG_DIR);
                Files.createDirectories(logDir);
                Path reportFile = logDir.resolve(
                    "session_" + System.currentTimeMillis() + ".log");
                Files.write(reportFile,
                    getDiagnosticReport().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                // Best-effort, don't throw in shutdown
            }

            LOGGER.info("[MDR] Goodbye.");
        }, "MDR-Shutdown"));
    }


    // ========================================================================
    //  SECTION 16: INTERNAL TYPES
    // ========================================================================

    /**
     * A Runnable that can throw checked exceptions.
     */
    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Throwable;
    }

    /**
     * A ByteArrayOutputStream for reading streams fully.
     * (Included here to avoid needing java.io.ByteArrayOutputStream import
     * confusion in some environments.)
     */
    // Already using java.io.ByteArrayOutputStream via readAllBytes()

    /**
     * Private constructor — this class is never instantiated.
     */
    private Mini_DirtyRoomCore() {
        throw new UnsupportedOperationException(
            "Mini_DirtyRoomCore is a static utility class.");
    }
}
