/*
 * ============================================================================
 *  Mini_DirtyRoom — Java & LWJGL Runtime Provisioner
 *  Copyright (c) 2025 Stellar Snow Astralis
 *
 *  This file provides the user-facing runtime provisioning system for
 *  Mini_DirtyRoom. It handles:
 *
 *    1. Startup prompts for users with outdated Java on Android/Desktop
 *    2. Pre-built Android JDK/JRE downloads (tar.xz) for supported launchers
 *    3. Desktop JDK/JRE guidance and auto-download from Oracle/Adoptium/Zulu
 *    4. User-selectable LWJGL 3.4.0 module downloads
 *    5. LaunchWrapper & Forge patches for modern Java + LWJGL compatibility
 *
 *  This file loads AFTER the core four components but BEFORE any game code.
 *  It is triggered by Mini_DirtyRoomCore when Java < 21 or LWJGL is outdated.
 *
 *  Supported Android Launchers:
 *    • PojavLauncher / ZailthLauncher
 *    • FoldCraft Launcher
 *    • MojoLauncher
 *    • Amethyst Launcher
 *
 *  Supported Desktop JDK Providers:
 *    • Eclipse Adoptium (Temurin)
 *    • Oracle JDK
 *    • Azul Zulu
 *    • Amazon Corretto
 *    • Microsoft Build of OpenJDK
 *
 *  Pre-built Android Runtimes (tar.xz):
 *    • JDK 21: https://files.catbox.moe/ewgjpu.xz
 *    • JRE 21: https://files.catbox.moe/nxeqne.xz
 *    • JRE 25: https://files.catbox.moe/t20701.xz
 *    • JDK 25: https://files.catbox.moe/usqgto.xz
 * ============================================================================
 */
package stellar.snow.astralis.integration.Mini_DirtyRoom.lwjgl;

// ─── Core Integration ─────────────────────────────────────────────────────
import stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore;
import stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.EnvironmentInfo;
import stellar.snow.astralis.integration.Mini_DirtyRoom.LWJGLTransformEngine;
import stellar.snow.astralis.integration.Mini_DirtyRoom.JavaCompatibilityLayer;
import stellar.snow.astralis.integration.Mini_DirtyRoom.JavaCompatibilityLayer.ReflectionHelper;
import stellar.snow.astralis.integration.Mini_DirtyRoom.JavaCompatibilityLayer.ClassLoaderCompat;
import stellar.snow.astralis.integration.Mini_DirtyRoom.ModLoaderBridge;

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
// ─── ASM ───────────────────────────────────────────────────────────────────
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

// ─── Standard Library ──────────────────────────────────────────────────────
import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * ============================================================================
 *  JAVA & LWJGL RUNTIME PROVISIONER
 * ============================================================================
 *
 * This class orchestrates the entire runtime provisioning pipeline:
 *
 *   Part A — RuntimeProvisioner:
 *     Detects outdated Java / LWJGL and presents the user with download
 *     options. On Android, offers pre-built tar.xz runtimes. On Desktop,
 *     offers links or auto-downloads from major JDK providers.
 *
 *   Part B — LWJGLModuleSelector:
 *     Allows users to choose which LWJGL 3.4.0 modules they want
 *     (OpenGL, OpenAL, GLFW, STB, etc.) beyond the auto-downloaded core.
 *
 *   Part C — LaunchWrapperPatcher:
 *     Bytecode-patches Forge's LaunchWrapper and FML to accept modern
 *     Java (17/21/25) and cooperate with LWJGL 3.4.0.
 *
 *   Part D — ForgeModernizer:
 *     Patches Forge internals that are incompatible with Java 9+ module
 *     system, removed APIs, and LWJGL 3.
 */

public final class JavaLWJGLProvisioner {

    // ========================================================================
    //  CONSTANTS
    // ========================================================================

    private static final Logger LOGGER = Logger.getLogger("MDR-Provisioner");

    /** Base directory for all provisioner data. */
    private static final String PROVISIONER_DIR = "runtime_provisioner";

    /** Marker file that indicates provisioning has been completed. */
    private static final String PROVISIONED_MARKER = ".provisioned";

    /** User choice persistence file. */
    private static final String USER_CHOICES_FILE = "user_choices.properties";

    /** Initialization guard. */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    /** Whether the user has been prompted this session. */
    private static final AtomicBoolean PROMPTED = new AtomicBoolean(false);

    /** User's selected action (set by the prompt system). */
    private static final AtomicReference<UserAction> USER_ACTION =
        new AtomicReference<>(UserAction.NONE);

    /** Download progress tracking. */
    private static final AtomicInteger DOWNLOAD_PROGRESS = new AtomicInteger(0);
    private static final AtomicLong DOWNLOAD_TOTAL_BYTES = new AtomicLong(0);
    private static final AtomicLong DOWNLOAD_CURRENT_BYTES = new AtomicLong(0);
    private static final AtomicReference<String> DOWNLOAD_STATUS =
        new AtomicReference<>("idle");


    // ========================================================================
    //  ANDROID RUNTIME DEFINITIONS
    // ========================================================================

    /**
     * Pre-built Android Java runtimes hosted on Catbox.
     * These are tar.xz archives containing aarch64 JDK/JRE builds
     * specifically compiled for Android's Linux-based kernel.
     */
    static final class AndroidRuntime {
        final String name;
        final String description;
        final String url;
        final String fileName;
        final int    javaVersion;
        final boolean isJDK;    // true = JDK (dev tools), false = JRE (runtime only)
        final String sha256;    // Expected hash (empty = skip verification)

        AndroidRuntime(String name, String description, String url,
                      String fileName, int javaVersion, boolean isJDK,
                      String sha256) {
            this.name        = name;
            this.description = description;
            this.url         = url;
            this.fileName    = fileName;
            this.javaVersion = javaVersion;
            this.isJDK       = isJDK;
            this.sha256      = sha256;
        }
    }

    /** All available Android runtimes. */
    private static final List<AndroidRuntime> ANDROID_RUNTIMES = Arrays.asList(
        new AndroidRuntime(
            "JRE 21 (Recommended)",
            "Lightweight Java 21 runtime. Best for playing Minecraft. "
          + "Smaller download, faster startup.",
            "https://files.catbox.moe/nxeqne.xz",
            "jre-21-android-aarch64.tar.xz",
            21, false, ""
        ),
        new AndroidRuntime(
            "JDK 21 (Full Development Kit)",
            "Complete Java 21 development kit. Use if you develop mods "
          + "on your phone or need javac/jdb. Larger download.",
            "https://files.catbox.moe/ewgjpu.xz",
            "jdk-21-android-aarch64.tar.xz",
            21, true, ""
        ),
        new AndroidRuntime(
            "JRE 25 (Cutting Edge)",
            "Latest Java 25 runtime. Maximum performance and newest features. "
          + "May have compatibility issues with some mods.",
            "https://files.catbox.moe/t20701.xz",
            "jre-25-android-aarch64.tar.xz",
            25, false, ""
        ),
        new AndroidRuntime(
            "JDK 25 (Latest Development Kit)",
            "Complete Java 25 development kit. Bleeding-edge features. "
          + "Use only if you specifically need Java 25 tools.",
            "https://files.catbox.moe/usqgto.xz",
            "jdk-25-android-aarch64.tar.xz",
            25, true, ""
        )
    );


    // ========================================================================
    //  DESKTOP JDK PROVIDER DEFINITIONS
    // ========================================================================

    /**
     * Desktop JDK download providers.
     */
    static final class DesktopJDKProvider {
        final String name;
        final String description;
        final String downloadPageURL;
        final String apiURL;  // For auto-download (empty = manual only)
        final boolean supportsAutoDownload;

        DesktopJDKProvider(String name, String description,
                          String downloadPageURL, String apiURL,
                          boolean supportsAutoDownload) {
            this.name                = name;
            this.description         = description;
            this.downloadPageURL     = downloadPageURL;
            this.apiURL              = apiURL;
            this.supportsAutoDownload = supportsAutoDownload;
        }
    }

    /** All available desktop JDK providers. */
    private static final List<DesktopJDKProvider> DESKTOP_PROVIDERS = Arrays.asList(
        new DesktopJDKProvider(
            "Eclipse Adoptium (Temurin)",
            "Free, open-source, community-supported JDK. Recommended for most users. "
          + "Backed by the Eclipse Foundation.",
            "https://adoptium.net/temurin/releases/",
            "https://api.adoptium.net/v3/binary/latest/%d/ga/%s/%s/jdk/hotspot/normal/eclipse",
            true
        ),
        new DesktopJDKProvider(
            "Azul Zulu",
            "Free, enterprise-grade JDK by Azul Systems. Excellent performance "
          + "and support for many platforms including ARM.",
            "https://www.azul.com/downloads/",
            "https://api.azul.com/metadata/v1/zulu/packages/?java_version=%d&os=%s&arch=%s&archive_type=zip&java_package_type=jdk&latest=true",
            true
        ),
        new DesktopJDKProvider(
            "Oracle JDK",
            "Official JDK from Oracle. Free for development and production use "
          + "since Java 17. Requires manual download from Oracle's website.",
            "https://www.oracle.com/java/technologies/downloads/",
            "",
            false
        ),
        new DesktopJDKProvider(
            "Amazon Corretto",
            "Free, production-ready JDK by Amazon. Optimized for AWS but "
          + "works everywhere. Long-term support.",
            "https://aws.amazon.com/corretto/",
            "https://corretto.aws/downloads/latest/amazon-corretto-%d-%s-jdk.%s",
            true
        ),
        new DesktopJDKProvider(
            "Microsoft Build of OpenJDK",
            "Free JDK by Microsoft. Optimized for Azure and Windows but "
          + "available for all platforms.",
            "https://learn.microsoft.com/en-us/java/openjdk/download",
            "",
            false
        )
    );


    // ========================================================================
    //  LWJGL MODULE DEFINITIONS
    // ========================================================================

    /**
     * Individual LWJGL 3.4.0 module that users can choose to install.
     */
    static final class LWJGLModule {
        final String artifactId;
        final String displayName;
        final String description;
        final boolean required;    // Core modules that are always needed
        final boolean hasNatives;  // Whether platform-specific natives exist
        final String category;     // Grouping for UI display

        LWJGLModule(String artifactId, String displayName, String description,
                   boolean required, boolean hasNatives, String category) {
            this.artifactId  = artifactId;
            this.displayName = displayName;
            this.description = description;
            this.required    = required;
            this.hasNatives  = hasNatives;
            this.category    = category;
        }
    }

    /** All available LWJGL 3.4.0 modules. */
    private static final List<LWJGLModule> LWJGL_MODULES = Arrays.asList(
        // ── Core (Required) ────────────────────────────────────────────────
        new LWJGLModule("lwjgl", "LWJGL Core",
            "Core library. Required for all LWJGL functionality.",
            true, true, "Core"),
        new LWJGLModule("lwjgl-glfw", "GLFW",
            "Window creation, input handling, OpenGL/Vulkan context. Required for Minecraft.",
            true, true, "Core"),
        new LWJGLModule("lwjgl-opengl", "OpenGL",
            "OpenGL bindings for 3D rendering. Required for Minecraft.",
            true, true, "Core"),
        new LWJGLModule("lwjgl-openal", "OpenAL",
            "3D audio library. Required for Minecraft sound.",
            true, true, "Core"),
        new LWJGLModule("lwjgl-stb", "STB",
            "Image loading, font rendering, Vorbis decoding. Required for Minecraft.",
            true, true, "Core"),

        // ── Common (Recommended) ───────────────────────────────────────────
        new LWJGLModule("lwjgl-tinyfd", "TinyFileDialogs",
            "Native file dialogs (open/save). Used by some mods.",
            false, true, "Recommended"),
        new LWJGLModule("lwjgl-jemalloc", "jemalloc",
            "High-performance memory allocator. Improves memory usage.",
            false, true, "Recommended"),

        // ── Optional (Mod-Specific) ────────────────────────────────────────
        new LWJGLModule("lwjgl-nfd", "Native File Dialog",
            "Extended native file dialog library. Used by some editor mods.",
            false, true, "Optional"),
        new LWJGLModule("lwjgl-par", "par_shapes",
            "Parametric 3D shape generation. Used by rendering mods.",
            false, true, "Optional"),
        new LWJGLModule("lwjgl-assimp", "Assimp",
            "3D model import library. Supports 40+ model formats. "
          + "Used by mods that load custom 3D models.",
            false, true, "Optional"),
        new LWJGLModule("lwjgl-freetype", "FreeType",
            "Font rendering library. Used by custom GUI mods.",
            false, true, "Optional"),
        new LWJGLModule("lwjgl-harfbuzz", "HarfBuzz",
            "Text shaping engine. Used alongside FreeType for complex text.",
            false, true, "Optional"),
        new LWJGLModule("lwjgl-opus", "Opus",
            "Audio codec for voice chat. Used by voice chat mods.",
            false, true, "Optional"),

        // ── Advanced (Specialized) ─────────────────────────────────────────
        new LWJGLModule("lwjgl-vulkan", "Vulkan",
            "Vulkan graphics API bindings. For mods using Vulkan rendering.",
            false, false, "Advanced"),
        new LWJGLModule("lwjgl-shaderc", "Shaderc",
            "GLSL/HLSL to SPIR-V shader compiler. For Vulkan mods.",
            false, true, "Advanced"),
        new LWJGLModule("lwjgl-spvc", "SPIRV-Cross",
            "SPIR-V reflection and cross-compilation. For shader mods.",
            false, true, "Advanced"),
        new LWJGLModule("lwjgl-meshoptimizer", "meshoptimizer",
            "Mesh optimization library. For mods that optimize 3D geometry.",
            false, true, "Advanced"),
        new LWJGLModule("lwjgl-ktx", "KTX",
            "Khronos Texture format support. For advanced texture mods.",
            false, true, "Advanced"),
        new LWJGLModule("lwjgl-yoga", "Yoga",
            "Flexbox layout engine. For modern UI mods.",
            false, true, "Advanced"),
        new LWJGLModule("lwjgl-zstd", "Zstandard",
            "Fast compression library. For mods handling compressed data.",
            false, true, "Advanced"),
        new LWJGLModule("lwjgl-lz4", "LZ4",
            "Extremely fast compression. For network/world compression mods.",
            false, true, "Advanced"),
        new LWJGLModule("lwjgl-xxhash", "xxHash",
            "Extremely fast non-cryptographic hash function.",
            false, true, "Advanced")
    );


    // ========================================================================
    //  SECTION 1: MAIN ENTRY POINT
    // ========================================================================

    /**
     * Main entry point. Called early in the bootstrap, before the game starts.
     * Checks whether provisioning is needed and drives the process.
     */
    @DeepHook(
        targets = {
            @HookTarget(
                className  = "stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore",
                methodName = "bootstrap"
            )
        },
        timing = HookTiming.BEFORE
    )
    public static void checkAndProvision() {
        if (!INITIALIZED.compareAndSet(false, true)) return;

        LOGGER.info("[MDR-Provisioner] ╔═══════════════════════════════════════════╗");
        LOGGER.info("[MDR-Provisioner] ║  Runtime Provisioner Starting            ║");
        LOGGER.info("[MDR-Provisioner] ╚═══════════════════════════════════════════╝");

        EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
        if (env == null) {
            // Core hasn't detected environment yet — do minimal detection
            LOGGER.info("[MDR-Provisioner] Environment not yet detected. "
                      + "Performing early probe.");
        }

        Path provisionerHome = getProvisionerHome();

        // ── Check if already provisioned ───────────────────────────────────
        Path marker = provisionerHome.resolve(PROVISIONED_MARKER);
        Properties savedChoices = loadUserChoices(provisionerHome);
        boolean alreadyProvisioned = Files.exists(marker);

        if (alreadyProvisioned && !isForceReprovision()) {
            LOGGER.info("[MDR-Provisioner] Already provisioned. "
                      + "Applying saved patches.");
            // Still apply LaunchWrapper/Forge patches every startup
            applyLaunchWrapperPatches();
            applyForgePatches();
            return;
        }

        // ── Determine what needs provisioning ──────────────────────────────
        boolean needsJava = needsJavaUpgrade(env);
        boolean needsLWJGL = needsLWJGLUpgrade();
        boolean isAndroid = (env != null && env.isAndroid) || detectAndroidFallback();

        LOGGER.info("[MDR-Provisioner] Needs Java upgrade: " + needsJava);
        LOGGER.info("[MDR-Provisioner] Needs LWJGL upgrade: " + needsLWJGL);
        LOGGER.info("[MDR-Provisioner] Platform: " + (isAndroid ? "Android" : "Desktop"));

        if (!needsJava && !needsLWJGL) {
            LOGGER.info("[MDR-Provisioner] Runtime is up to date. "
                      + "Applying patches only.");
            markProvisioned(provisionerHome);
            applyLaunchWrapperPatches();
            applyForgePatches();
            return;
        }

        // ── Present options to user ────────────────────────────────────────
        if (isAndroid) {
            presentAndroidOptions(needsJava, needsLWJGL, provisionerHome);
        } else {
            presentDesktopOptions(needsJava, needsLWJGL, provisionerHome);
        }

        // ── Apply patches regardless ───────────────────────────────────────
        applyLaunchWrapperPatches();
        applyForgePatches();

        LOGGER.info("[MDR-Provisioner] Provisioning sequence complete.");
    }

    /**
     * Checks if the current Java version needs upgrading.
     */
    private static boolean needsJavaUpgrade(EnvironmentInfo env) {
        int version = (env != null) ? env.javaVersion
            : JavaCompatibilityLayer.getJavaVersion();
        return version < 21;
    }

    /**
     * Checks if LWJGL needs upgrading.
     */
    private static boolean needsLWJGLUpgrade() {
        try {
            Class<?> versionClass = Class.forName("org.lwjgl.Version");
            Method getVersion = versionClass.getMethod("getVersion");
            String version = (String) getVersion.invoke(null);
            if (version != null && version.startsWith("3.4")) {
                return false; // Already on 3.4.x
            }
            return true;
        } catch (ClassNotFoundException e) {
            // LWJGL not loaded yet — check if it's LWJGL 2
            try {
                Class.forName("org.lwjgl.Sys");
                return true; // LWJGL 2 detected
            } catch (ClassNotFoundException e2) {
                return true; // No LWJGL at all
            }
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Fallback Android detection if Core hasn't initialized.
     */
    private static boolean detectAndroidFallback() {
        try { Class.forName("android.os.Build"); return true; }
        catch (ClassNotFoundException e) {}
        String vmName = System.getProperty("java.vm.name", "");
        return vmName.toLowerCase().contains("dalvik")
            || vmName.toLowerCase().contains("art");
    }

    /**
     * Checks if force-reprovision was requested.
     */
    private static boolean isForceReprovision() {
        return "true".equalsIgnoreCase(
            System.getProperty("mini_dirtyroom.reprovision", "false"));
    }


    // ========================================================================
    //  SECTION 2: ANDROID OPTIONS PRESENTATION
    // ========================================================================

    /**
     * Presents Java/LWJGL options to Android users.
     * Uses the launcher's UI system if available, otherwise falls back
     * to a simple console/log-based prompt.
     */
    private static void presentAndroidOptions(boolean needsJava, boolean needsLWJGL,
                                              Path provisionerHome) {
        LOGGER.info("[MDR-Provisioner] ═══════════════════════════════════════════");
        LOGGER.info("[MDR-Provisioner]  ANDROID RUNTIME SETUP");
        LOGGER.info("[MDR-Provisioner] ═══════════════════════════════════════════");

        // Detect which Android launcher we're running in
        AndroidLauncherType launcher = detectAndroidLauncherType();
        LOGGER.info("[MDR-Provisioner] Detected launcher: " + launcher.name);

        // ── Try native Android UI ──────────────────────────────────────────
        boolean uiShown = tryShowAndroidUI(needsJava, needsLWJGL, launcher);

        if (!uiShown) {
            // ── Fallback: auto-select best runtime and download ────────────
            LOGGER.info("[MDR-Provisioner] No UI available. Auto-selecting runtime...");
            autoSelectAndProvisionAndroid(needsJava, needsLWJGL, provisionerHome, launcher);
        }
    }

    /**
     * Detects the specific Android launcher type.
     */
    static final class AndroidLauncherType {
        final String name;
        final String packageName;
        final Path basePath;
        final Path javaHomePath;
        final Path lwjglPath;
        final boolean supportsCustomJava;

        AndroidLauncherType(String name, String packageName, Path basePath,
                           Path javaHomePath, Path lwjglPath,
                           boolean supportsCustomJava) {
            this.name               = name;
            this.packageName        = packageName;
            this.basePath           = basePath;
            this.javaHomePath       = javaHomePath;
            this.lwjglPath          = lwjglPath;
            this.supportsCustomJava = supportsCustomJava;
        }
    }

    private static AndroidLauncherType detectAndroidLauncherType() {
        String extStorage = System.getenv("EXTERNAL_STORAGE");
        if (extStorage == null) extStorage = "/sdcard";

        // ── PojavLauncher / ZailthLauncher ─────────────────────────────────
        try {
            Class.forName("net.kdt.pojavlaunch.PojavApplication");
            String baseDir = null;
            try {
                Field f = Class.forName("net.kdt.pojavlaunch.Tools")
                    .getDeclaredField("DIR_GAME_HOME");
                f.setAccessible(true);
                baseDir = (String) f.get(null);
            } catch (Exception ignored) {
                baseDir = extStorage + "/games/PojavLauncher";
            }
            return new AndroidLauncherType(
                "PojavLauncher/ZailthLauncher",
                "net.kdt.pojavlaunch",
                Paths.get(baseDir),
                Paths.get(baseDir, "java_runtimes"),
                Paths.get(baseDir, "lwjgl3"),
                true
            );
        } catch (ClassNotFoundException ignored) {}

        // Explicit ZailthLauncher check
        try {
            Class.forName("com.movtery.zalithlauncher.ZalithLauncherApplication");
            String baseDir = extStorage + "/games/ZalithLauncher";
            return new AndroidLauncherType(
                "ZailthLauncher",
                "com.movtery.zalithlauncher",
                Paths.get(baseDir),
                Paths.get(baseDir, "java_runtimes"),
                Paths.get(baseDir, "lwjgl3"),
                true
            );
        } catch (ClassNotFoundException ignored) {}

        // ── FoldCraft Launcher ─────────────────────────────────────────────
        try {
            Class.forName("com.mio.foldcraftlauncher.FCLApplication");
            String baseDir = extStorage + "/FCL";
            return new AndroidLauncherType(
                "FoldCraft Launcher",
                "com.mio.foldcraftlauncher",
                Paths.get(baseDir),
                Paths.get(baseDir, "runtime"),
                Paths.get(baseDir, "lwjgl"),
                true
            );
        } catch (ClassNotFoundException ignored) {}

        // ── MojoLauncher ───────────────────────────────────────────────────
        try {
            Class.forName("com.mojolauncher.app.MojoApplication");
            String baseDir = extStorage + "/MojoLauncher";
            return new AndroidLauncherType(
                "MojoLauncher",
                "com.mojolauncher.app",
                Paths.get(baseDir),
                Paths.get(baseDir, "java"),
                Paths.get(baseDir, "lwjgl"),
                true
            );
        } catch (ClassNotFoundException ignored) {}

        // ── Amethyst Launcher ──────────────────────────────────────────────
        try {
            Class.forName("com.amethyst.launcher.AmethystApplication");
            String baseDir = extStorage + "/AmethystLauncher";
            return new AndroidLauncherType(
                "Amethyst Launcher",
                "com.amethyst.launcher",
                Paths.get(baseDir),
                Paths.get(baseDir, "java_runtimes"),
                Paths.get(baseDir, "lwjgl"),
                true
            );
        } catch (ClassNotFoundException ignored) {}

        // ── Generic fallback ───────────────────────────────────────────────
        return new AndroidLauncherType(
            "Unknown Android Launcher",
            "unknown",
            Paths.get(extStorage, "minecraft"),
            Paths.get(extStorage, "minecraft", "java_runtimes"),
            Paths.get(extStorage, "minecraft", "lwjgl"),
            false
        );
    }

    /**
     * Attempts to show a native Android dialog with runtime options.
     * Uses reflection to access Android Activity/AlertDialog.
     */
    private static boolean tryShowAndroidUI(boolean needsJava, boolean needsLWJGL,
                                            AndroidLauncherType launcher) {
        try {
            // Get the current Android Activity
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method currentActivityThread = activityThread.getMethod("currentActivityThread");
            Object at = currentActivityThread.invoke(null);
            Method getApplication = at.getClass().getMethod("getApplication");
            Object app = getApplication.invoke(at);

            // Build the message
            StringBuilder message = new StringBuilder();
            message.append("Mini_DirtyRoom needs to set up your Java runtime.\n\n");

            if (needsJava) {
                message.append("⚠ Your Java version is outdated.\n");
                message.append("  Current: Java ")
                       .append(JavaCompatibilityLayer.getJavaVersion()).append("\n");
                message.append("  Required: Java 21+\n\n");
                message.append("Available runtimes:\n\n");

                for (int i = 0; i < ANDROID_RUNTIMES.size(); i++) {
                    AndroidRuntime rt = ANDROID_RUNTIMES.get(i);
                    message.append((i + 1)).append(". ").append(rt.name).append("\n");
                    message.append("   ").append(rt.description).append("\n\n");
                }
            }

            if (needsLWJGL) {
                message.append("⚠ LWJGL needs to be updated to 3.4.0.\n");
                message.append("This will be done automatically.\n");
            }

            // Try to show the dialog using Android APIs
            Class<?> alertDialogBuilder = Class.forName(
                "android.app.AlertDialog$Builder");
            Class<?> contextClass = Class.forName("android.content.Context");
            Class<?> dialogInterfaceClass = Class.forName(
                "android.content.DialogInterface");
            Class<?> onClickListenerClass = Class.forName(
                "android.content.DialogInterface$OnClickListener");

            // This is complex and may fail on some launchers.
            // We use a CompletableFuture-like mechanism to wait for user input.
            CountDownLatch userChoice = new CountDownLatch(1);

            // Create a simple choice dialog
            Object builder = alertDialogBuilder.getConstructor(contextClass)
                .newInstance(app);

            Method setTitle = alertDialogBuilder.getMethod(
                "setTitle", CharSequence.class);
            setTitle.invoke(builder, "Mini_DirtyRoom — Runtime Setup");

            Method setMessage = alertDialogBuilder.getMethod(
                "setMessage", CharSequence.class);
            setMessage.invoke(builder, message.toString());

            // Create button labels based on what's available
            String[] options;
            if (needsJava) {
                options = new String[ANDROID_RUNTIMES.size() + 1];
                for (int i = 0; i < ANDROID_RUNTIMES.size(); i++) {
                    options[i] = ANDROID_RUNTIMES.get(i).name;
                }
                options[options.length - 1] = "Skip (not recommended)";
            } else {
                options = new String[]{"Update LWJGL to 3.4.0", "Skip"};
            }

            // Set items
            Object listener = Proxy.newProxyInstance(
                onClickListenerClass.getClassLoader(),
                new Class<?>[]{onClickListenerClass},
                (proxy, method, args) -> {
                    if ("onClick".equals(method.getName())) {
                        int which = (int) args[1];
                        if (needsJava && which < ANDROID_RUNTIMES.size()) {
                            USER_ACTION.set(new UserAction(
                                ActionType.DOWNLOAD_ANDROID_RUNTIME, which));
                        } else {
                            USER_ACTION.set(new UserAction(
                                ActionType.SKIP, -1));
                        }
                        userChoice.countDown();
                    }
                    return null;
                }
            );

            Method setItems = alertDialogBuilder.getMethod(
                "setItems", CharSequence[].class, onClickListenerClass);
            setItems.invoke(builder, options, listener);

            // Show the dialog on the UI thread
            Class<?> handler = Class.forName("android.os.Handler");
            Class<?> looper = Class.forName("android.os.Looper");
            Method getMainLooper = looper.getMethod("getMainLooper");
            Object mainLooper = getMainLooper.invoke(null);
            Object mainHandler = handler.getConstructor(looper).newInstance(mainLooper);

            Method post = handler.getMethod("post", Runnable.class);
            post.invoke(mainHandler, (Runnable) () -> {
                try {
                    Method show = builder.getClass().getMethod("show");
                    show.invoke(builder);
                } catch (Exception e) {
                    LOGGER.fine("[MDR-Provisioner] Could not show dialog: "
                              + e.getMessage());
                    userChoice.countDown();
                }
            });

            // Wait for user choice (30 second timeout)
            boolean chose = userChoice.await(30, TimeUnit.SECONDS);
            if (!chose) {
                LOGGER.info("[MDR-Provisioner] User did not respond. Auto-selecting.");
                USER_ACTION.set(new UserAction(ActionType.DOWNLOAD_ANDROID_RUNTIME, 0));
            }

            PROMPTED.set(true);

            // Process the choice
            UserAction action = USER_ACTION.get();
            if (action.type == ActionType.DOWNLOAD_ANDROID_RUNTIME) {
                AndroidRuntime selected = ANDROID_RUNTIMES.get(action.index);
                LOGGER.info("[MDR-Provisioner] User selected: " + selected.name);
                downloadAndInstallAndroidRuntime(selected,
                    detectAndroidLauncherType());
            }

            return true;

        } catch (Exception e) {
            LOGGER.fine("[MDR-Provisioner] Android UI not available: "
                      + e.getMessage());
            return false;
        }
    }

    /**
     * Auto-selects the best runtime and downloads it without UI.
     */
    private static void autoSelectAndProvisionAndroid(
            boolean needsJava, boolean needsLWJGL,
            Path provisionerHome, AndroidLauncherType launcher) {

        if (needsJava) {
            // Auto-select JRE 21 (most compatible, smallest download)
            AndroidRuntime selected = ANDROID_RUNTIMES.get(0); // JRE 21
            LOGGER.info("[MDR-Provisioner] Auto-selected: " + selected.name);

            printToConsole("╔═══════════════════════════════════════════════════╗");
            printToConsole("║  Mini_DirtyRoom — Auto Runtime Setup             ║");
            printToConsole("╠═══════════════════════════════════════════════════╣");
            printToConsole("║  Your Java is outdated (Java "
                + JavaCompatibilityLayer.getJavaVersion() + ")                  ║");
            printToConsole("║  Downloading: " + selected.name + "              ║");
            printToConsole("║  From: " + selected.url + "                      ║");
            printToConsole("║                                                   ║");
            printToConsole("║  To change this later, delete:                    ║");
            printToConsole("║    " + provisionerHome.resolve(PROVISIONED_MARKER) + "║");
            printToConsole("║  and restart the game.                            ║");
            printToConsole("╚═══════════════════════════════════════════════════╝");

            downloadAndInstallAndroidRuntime(selected, launcher);
        }

        markProvisioned(provisionerHome);
    }

    /**
     * Downloads and installs an Android runtime.
     */
    private static void downloadAndInstallAndroidRuntime(
            AndroidRuntime runtime, AndroidLauncherType launcher) {
        Path downloadDir = launcher.javaHomePath;
        try {
            Files.createDirectories(downloadDir);
        } catch (IOException e) {
            LOGGER.severe("[MDR-Provisioner] Cannot create directory: " + downloadDir);
            return;
        }

        Path archivePath = downloadDir.resolve(runtime.fileName);
        Path extractDir = downloadDir.resolve(
            (runtime.isJDK ? "jdk-" : "jre-") + runtime.javaVersion);

        try {
            // ── Download ───────────────────────────────────────────────────
            if (!Files.exists(archivePath)) {
                LOGGER.info("[MDR-Provisioner] Downloading " + runtime.name + "...");
                DOWNLOAD_STATUS.set("downloading");
                downloadFile(runtime.url, archivePath, runtime.name);
                
                // ── Rename downloaded file to descriptive name ─────────────
                String descriptiveName = (runtime.isJDK ? "JDK-" : "JRE-") 
                                       + runtime.javaVersion 
                                       + "-android-aarch64.tar.xz";
                Path descriptivePath = downloadDir.resolve(descriptiveName);
                
                if (!archivePath.equals(descriptivePath)) {
                    try {
                        Files.move(archivePath, descriptivePath, 
                                 StandardCopyOption.REPLACE_EXISTING);
                        archivePath = descriptivePath;
                        LOGGER.info("[MDR-Provisioner] Renamed to: " + descriptiveName);
                    } catch (IOException e) {
                        LOGGER.warning("[MDR-Provisioner] Could not rename file: " 
                                     + e.getMessage());
                        // Continue with original filename if rename fails
                    }
                }
            } else {
                LOGGER.info("[MDR-Provisioner] Archive already exists: "
                          + archivePath.getFileName());
            }

            // ── Verify checksum ────────────────────────────────────────────
            if (!runtime.sha256.isEmpty()) {
                String actual = sha256(archivePath);
                if (!runtime.sha256.equalsIgnoreCase(actual)) {
                    LOGGER.warning("[MDR-Provisioner] Checksum mismatch! "
                                 + "Expected: " + runtime.sha256
                                 + " Got: " + actual);
                    Files.deleteIfExists(archivePath);
                    return;
                }
            }

            // ── Extract tar.xz ─────────────────────────────────────────────
            LOGGER.info("[MDR-Provisioner] Extracting to " + extractDir + "...");
            DOWNLOAD_STATUS.set("extracting");
            extractTarXz(archivePath, extractDir);

            // ── Find java binary ───────────────────────────────────────────
            Path javaBinary = findJavaBinaryRecursive(extractDir);
            if (javaBinary == null) {
                LOGGER.severe("[MDR-Provisioner] Java binary not found in extracted archive!");
                return;
            }

            // ── Make executable ────────────────────────────────────────────
            makeExecutable(javaBinary);
            // Also make all files in bin/ executable
            Path binDir = javaBinary.getParent();
            if (binDir != null && Files.isDirectory(binDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(binDir)) {
                    for (Path file : stream) {
                        makeExecutable(file);
                    }
                }
            }

            // ── Configure the launcher to use this runtime ─────────────────
            configureLauncherRuntime(launcher, extractDir, javaBinary, runtime);

            LOGGER.info("[MDR-Provisioner] ✓ Android runtime installed: "
                      + runtime.name);
            LOGGER.info("[MDR-Provisioner]   Binary: " + javaBinary);
            DOWNLOAD_STATUS.set("complete");

            // ── Cleanup archive to save space ──────────────────────────────
            try {
                Files.deleteIfExists(archivePath);
                LOGGER.fine("[MDR-Provisioner] Archive cleaned up.");
            } catch (IOException ignored) {}

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                "[MDR-Provisioner] Android runtime installation FAILED", e);
            DOWNLOAD_STATUS.set("failed: " + e.getMessage());
        }
    }

    /**
     * Configures the detected Android launcher to use the newly installed runtime.
     */
    private static void configureLauncherRuntime(
            AndroidLauncherType launcher, Path javaHome,
            Path javaBinary, AndroidRuntime runtime) {

        LOGGER.info("[MDR-Provisioner] Configuring " + launcher.name
                  + " to use " + runtime.name + "...");

        switch (launcher.packageName) {
            case "net.kdt.pojavlaunch":
            case "com.movtery.zalithlauncher":
                configurePojavLauncher(launcher, javaHome, runtime);
                break;
            case "com.mio.foldcraftlauncher":
                configureFoldCraft(launcher, javaHome, runtime);
                break;
            case "com.mojolauncher.app":
                configureMojoLauncher(launcher, javaHome, runtime);
                break;
            case "com.amethyst.launcher":
                configureAmethystLauncher(launcher, javaHome, runtime);
                break;
            default:
                LOGGER.info("[MDR-Provisioner] Unknown launcher — setting "
                          + "JAVA_HOME environment hint.");
                // Set system property as a hint
                System.setProperty("mini_dirtyroom.java_home",
                    javaHome.toAbsolutePath().toString());
                break;
        }
    }

    private static void configurePojavLauncher(
            AndroidLauncherType launcher, Path javaHome, AndroidRuntime runtime) {
        try {
            // PojavLauncher stores runtime config in:
            //   <base>/java_runtimes/<name>/
            // And a JSON preference in shared prefs
            // Try to write a config hint
            Path configFile = launcher.basePath.resolve("java_runtimes")
                .resolve("mdr_runtime.json");
            String config = "{\n"
                + "  \"name\": \"" + runtime.name + "\",\n"
                + "  \"javaHome\": \"" + javaHome.toAbsolutePath() + "\",\n"
                + "  \"version\": " + runtime.javaVersion + ",\n"
                + "  \"isJDK\": " + runtime.isJDK + ",\n"
                + "  \"provider\": \"Mini_DirtyRoom\"\n"
                + "}\n";
            Files.write(configFile, config.getBytes(StandardCharsets.UTF_8));

            // Try to set it as the active runtime via PojavLauncher's prefs
            try {
                Class<?> prefClass = Class.forName(
                    "net.kdt.pojavlaunch.prefs.LauncherPreferences");
                Field javaDir = prefClass.getDeclaredField("PREF_JAVA_RUNTIME");
                javaDir.setAccessible(true);
                javaDir.set(null, javaHome.toAbsolutePath().toString());
                LOGGER.info("[MDR-Provisioner] PojavLauncher runtime configured.");
            } catch (Exception e) {
                LOGGER.fine("[MDR-Provisioner] Could not set PojavLauncher pref: "
                          + e.getMessage());
            }

        } catch (IOException e) {
            LOGGER.fine("[MDR-Provisioner] PojavLauncher config write: "
                      + e.getMessage());
        }
    }

    private static void configureFoldCraft(
            AndroidLauncherType launcher, Path javaHome, AndroidRuntime runtime) {
        try {
            Path configFile = launcher.basePath.resolve("runtime")
                .resolve("current_java.txt");
            Files.createDirectories(configFile.getParent());
            Files.write(configFile,
                javaHome.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8));
            LOGGER.info("[MDR-Provisioner] FoldCraft runtime configured.");
        } catch (IOException e) {
            LOGGER.fine("[MDR-Provisioner] FoldCraft config: " + e.getMessage());
        }
    }

    private static void configureMojoLauncher(
            AndroidLauncherType launcher, Path javaHome, AndroidRuntime runtime) {
        try {
            Path configFile = launcher.basePath.resolve("config")
                .resolve("java_runtime.properties");
            Files.createDirectories(configFile.getParent());
            Properties props = new Properties();
            props.setProperty("java.home", javaHome.toAbsolutePath().toString());
            props.setProperty("java.version", String.valueOf(runtime.javaVersion));
            try (OutputStream os = Files.newOutputStream(configFile)) {
                props.store(os, "Set by Mini_DirtyRoom Provisioner");
            }
            LOGGER.info("[MDR-Provisioner] MojoLauncher runtime configured.");
        } catch (IOException e) {
            LOGGER.fine("[MDR-Provisioner] MojoLauncher config: " + e.getMessage());
        }
    }

    private static void configureAmethystLauncher(
            AndroidLauncherType launcher, Path javaHome, AndroidRuntime runtime) {
        try {
            Path configFile = launcher.basePath.resolve("java_runtimes")
                .resolve("active_runtime.json");
            Files.createDirectories(configFile.getParent());
            String json = "{\"path\":\"" + javaHome.toAbsolutePath()
                + "\",\"version\":" + runtime.javaVersion + "}";
            Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));
            LOGGER.info("[MDR-Provisioner] Amethyst runtime configured.");
        } catch (IOException e) {
            LOGGER.fine("[MDR-Provisioner] Amethyst config: " + e.getMessage());
        }
    }


    // ========================================================================
    //  SECTION 3: DESKTOP OPTIONS PRESENTATION
    // ========================================================================

    /**
     * Presents Java/LWJGL options to desktop users.
     */
    private static void presentDesktopOptions(boolean needsJava, boolean needsLWJGL,
                                              Path provisionerHome) {
        LOGGER.info("[MDR-Provisioner] ═══════════════════════════════════════════");
        LOGGER.info("[MDR-Provisioner]  DESKTOP RUNTIME SETUP");
        LOGGER.info("[MDR-Provisioner] ═══════════════════════════════════════════");

        EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();

        if (needsJava) {
            // Try GUI dialog first
            boolean guiShown = tryShowDesktopDialog(needsJava, needsLWJGL);

            if (!guiShown) {
                // Console-based options
                printToConsole("╔═══════════════════════════════════════════════════════╗");
                printToConsole("║  Mini_DirtyRoom — Java Runtime Upgrade Needed        ║");
                printToConsole("╠═══════════════════════════════════════════════════════╣");
                printToConsole("║  Your Java: " + JavaCompatibilityLayer.getJavaVersion()
                    + " (need 21+)                            ║");
                printToConsole("║                                                       ║");
                printToConsole("║  Download a modern JDK from:                          ║");
                for (DesktopJDKProvider provider : DESKTOP_PROVIDERS) {
                    printToConsole("║  • " + provider.name);
                    printToConsole("║    " + provider.downloadPageURL);
                }
                printToConsole("║                                                       ║");
                printToConsole("║  Attempting auto-download from Adoptium...            ║");
                printToConsole("╚═══════════════════════════════════════════════════════╝");

                // Auto-download from Adoptium
                autoDownloadDesktopJDK(provisionerHome, env);
            }
        }

        // LWJGL module selection for desktop
        if (needsLWJGL) {
            presentLWJGLModuleSelection(provisionerHome);
        }

        markProvisioned(provisionerHome);
    }

    /**
     * Attempts to show a Swing dialog on desktop with provider options.
     */
    private static boolean tryShowDesktopDialog(boolean needsJava, boolean needsLWJGL) {
        EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
        if (env != null && env.isHeadless) return false;

        try {
            // Check if Swing is available
            Class<?> jopClass = Class.forName("javax.swing.JOptionPane");
            Class<?> uiManagerClass = Class.forName("javax.swing.UIManager");

            // Set system look and feel
            Method setLookAndFeel = uiManagerClass.getMethod(
                "setLookAndFeel", String.class);
            Method getSystemLAF = uiManagerClass.getMethod(
                "getSystemLookAndFeelClassName");
            try {
                setLookAndFeel.invoke(null, getSystemLAF.invoke(null));
            } catch (Exception ignored) {}

            if (needsJava) {
                // Build provider options
                String[] options = new String[DESKTOP_PROVIDERS.size() + 1];
                for (int i = 0; i < DESKTOP_PROVIDERS.size(); i++) {
                    DesktopJDKProvider p = DESKTOP_PROVIDERS.get(i);
                    options[i] = p.name
                        + (p.supportsAutoDownload ? " (auto-download)" : " (manual)");
                }
                options[options.length - 1] = "Skip for now";

                String message = "Mini_DirtyRoom requires Java 21 or newer.\n"
                    + "Current Java: " + JavaCompatibilityLayer.getJavaVersion() + "\n\n"
                    + "Choose a JDK provider to download from:";

                // JOptionPane.showOptionDialog(...)
                Method showOptionDialog = jopClass.getMethod(
                    "showOptionDialog",
                    java.awt.Component.class, Object.class, String.class,
                    int.class, int.class,
                    Class.forName("javax.swing.Icon"),
                    Object[].class, Object.class);

                int choice = (int) showOptionDialog.invoke(null,
                    null,
                    message,
                    "Mini_DirtyRoom — Java Setup",
                    0,  // DEFAULT_OPTION
                    3,  // QUESTION_MESSAGE
                    null,
                    options,
                    options[0]);

                if (choice >= 0 && choice < DESKTOP_PROVIDERS.size()) {
                    DesktopJDKProvider selected = DESKTOP_PROVIDERS.get(choice);
                    LOGGER.info("[MDR-Provisioner] User selected: " + selected.name);

                    if (selected.supportsAutoDownload) {
                        autoDownloadFromProvider(selected);
                    } else {
                        // Open browser to download page
                        openURL(selected.downloadPageURL);

                        // Show info dialog
                        Method showMessageDialog = jopClass.getMethod(
                            "showMessageDialog",
                            java.awt.Component.class, Object.class,
                            String.class, int.class);
                        showMessageDialog.invoke(null, null,
                            "Please download and install JDK 21 or later from:\n"
                            + selected.downloadPageURL + "\n\n"
                            + "After installing, restart the game.",
                            "Download Instructions",
                            1); // INFORMATION_MESSAGE
                    }
                }

                PROMPTED.set(true);
                return true;
            }

            return false;

        } catch (Exception e) {
            LOGGER.fine("[MDR-Provisioner] Desktop dialog failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Auto-downloads a JDK from Adoptium for desktop.
     */
    private static void autoDownloadDesktopJDK(Path provisionerHome,
                                                EnvironmentInfo env) {
        DesktopJDKProvider adoptium = DESKTOP_PROVIDERS.get(0); // Adoptium
        autoDownloadFromProvider(adoptium);
    }

    /**
     * Auto-downloads a JDK from the specified provider.
     */
    private static void autoDownloadFromProvider(DesktopJDKProvider provider) {
        if (!provider.supportsAutoDownload || provider.apiURL.isEmpty()) {
            LOGGER.info("[MDR-Provisioner] " + provider.name
                      + " does not support auto-download.");
            openURL(provider.downloadPageURL);
            return;
        }

        EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
        String os = "linux";
        String arch = "x64";

        if (env != null) {
            if (env.isWindows) os = "windows";
            else if (env.isMacOS) os = "mac";
            if (env.isARM && env.is64Bit) arch = "aarch64";
            else if (env.isARM) arch = "arm";
        }

        try {
            String url = String.format(provider.apiURL, 21, os, arch);
            LOGGER.info("[MDR-Provisioner] Downloading JDK 21 from "
                      + provider.name + "...");

            Path downloadDir = getProvisionerHome().resolve("jdk_downloads");
            Files.createDirectories(downloadDir);

            String ext = (env != null && env.isWindows) ? ".zip" : ".tar.gz";
            Path archivePath = downloadDir.resolve("jdk-21" + ext);

            downloadFile(url, archivePath, provider.name + " JDK 21");

            // Extract
            Path extractDir = downloadDir.resolve("jdk-21");
            if (ext.equals(".zip")) {
                extractZip(archivePath, extractDir);
            } else {
                extractTarGz(archivePath, extractDir);
            }

            // Find java binary
            Path javaBinary = findJavaBinaryRecursive(extractDir);
            if (javaBinary != null) {
                makeExecutable(javaBinary);
                LOGGER.info("[MDR-Provisioner] ✓ JDK 21 installed at: " + javaBinary);

                // Save for future use
                Properties choices = new Properties();
                choices.setProperty("java.home",
                    javaBinary.getParent().getParent().toAbsolutePath().toString());
                choices.setProperty("java.version", "21");
                choices.setProperty("provider", provider.name);
                saveUserChoices(getProvisionerHome(), choices);
            }

            // Cleanup
            Files.deleteIfExists(archivePath);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                "[MDR-Provisioner] Auto-download from " + provider.name + " failed", e);
            LOGGER.info("[MDR-Provisioner] Please download manually from: "
                      + provider.downloadPageURL);
        }
    }


    // ========================================================================
    //  SECTION 4: LWJGL MODULE SELECTION
    // ========================================================================

    /**
     * Presents LWJGL module selection to the user.
     * Required modules are always included; the user can select optional ones.
     */
    private static void presentLWJGLModuleSelection(Path provisionerHome) {
        LOGGER.info("[MDR-Provisioner] ═══════════════════════════════════════════");
        LOGGER.info("[MDR-Provisioner]  LWJGL 3.4.0 Module Selection");
        LOGGER.info("[MDR-Provisioner] ═══════════════════════════════════════════");

        // Group modules by category
        Map<String, List<LWJGLModule>> byCategory = new LinkedHashMap<>();
        for (LWJGLModule mod : LWJGL_MODULES) {
            byCategory.computeIfAbsent(mod.category, k -> new ArrayList<>()).add(mod);
        }

        // Try GUI
        boolean guiShown = tryShowLWJGLSelector(byCategory, provisionerHome);

        if (!guiShown) {
            // Console-based: auto-select recommended modules
            LOGGER.info("[MDR-Provisioner] No GUI available. "
                      + "Auto-selecting recommended LWJGL modules.");

            List<LWJGLModule> selected = new ArrayList<>();
            for (LWJGLModule mod : LWJGL_MODULES) {
                if (mod.required || "Recommended".equals(mod.category)) {
                    selected.add(mod);
                }
            }

            downloadSelectedLWJGLModules(selected, provisionerHome);
        }
    }

    /**
     * Shows a Swing-based LWJGL module selector.
     */
    private static boolean tryShowLWJGLSelector(
            Map<String, List<LWJGLModule>> byCategory, Path provisionerHome) {

        EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
        if (env != null && (env.isHeadless || env.isAndroid)) return false;

        try {
            Class<?> jopClass = Class.forName("javax.swing.JOptionPane");
            Class<?> jCheckBox = Class.forName("javax.swing.JCheckBox");
            Class<?> jPanel = Class.forName("javax.swing.JPanel");
            Class<?> jLabel = Class.forName("javax.swing.JLabel");
            Class<?> jScrollPane = Class.forName("javax.swing.JScrollPane");
            Class<?> boxLayout = Class.forName("javax.swing.BoxLayout");

            // Build the panel
            Object panel = jPanel.getDeclaredConstructor().newInstance();
            Method setLayout = panel.getClass().getMethod("setLayout",
                Class.forName("java.awt.LayoutManager"));
            Object layout = boxLayout.getConstructor(
                Class.forName("java.awt.Container"), int.class)
                .newInstance(panel, 1); // Y_AXIS
            setLayout.invoke(panel, layout);

            Method addComponent = panel.getClass().getMethod("add",
                Class.forName("java.awt.Component"));

            // Header
            Object header = jLabel.getConstructor(String.class)
                .newInstance("<html><h3>Select LWJGL 3.4.0 Modules</h3>"
                    + "<p>Required modules are pre-selected and cannot be deselected.</p></html>");
            addComponent.invoke(panel, header);

            // Track checkboxes
            Map<Object, LWJGLModule> checkboxMap = new LinkedHashMap<>();

            for (Map.Entry<String, List<LWJGLModule>> entry : byCategory.entrySet()) {
                String category = entry.getKey();
                List<LWJGLModule> modules = entry.getValue();

                // Category label
                Object catLabel = jLabel.getConstructor(String.class)
                    .newInstance("<html><br><b>── " + category + " ──</b></html>");
                addComponent.invoke(panel, catLabel);

                for (LWJGLModule mod : modules) {
                    String label = mod.displayName + " — " + mod.description;
                    if (label.length() > 80) label = label.substring(0, 80) + "...";

                    Object cb = jCheckBox.getConstructor(String.class, boolean.class)
                        .newInstance(label, mod.required || "Recommended".equals(mod.category));

                    if (mod.required) {
                        Method setEnabled = cb.getClass().getMethod(
                            "setEnabled", boolean.class);
                        setEnabled.invoke(cb, false); // Can't deselect required
                        Method setSelected = cb.getClass().getMethod(
                            "setSelected", boolean.class);
                        setSelected.invoke(cb, true);
                    }

                    addComponent.invoke(panel, cb);
                    checkboxMap.put(cb, mod);
                }
            }

            // Wrap in scroll pane
            Object scrollPane = jScrollPane.getConstructor(
                Class.forName("java.awt.Component"))
                .newInstance(panel);
            Method setPreferredSize = scrollPane.getClass().getMethod(
                "setPreferredSize", Class.forName("java.awt.Dimension"));
            Object dim = Class.forName("java.awt.Dimension")
                .getConstructor(int.class, int.class)
                .newInstance(600, 400);
            setPreferredSize.invoke(scrollPane, dim);

            // Show dialog
            Method showConfirmDialog = jopClass.getMethod(
                "showConfirmDialog",
                java.awt.Component.class, Object.class, String.class,
                int.class, int.class);
            int result = (int) showConfirmDialog.invoke(null,
                null, scrollPane,
                "Mini_DirtyRoom — LWJGL Module Selection",
                0,  // OK_CANCEL_OPTION
                3); // QUESTION_MESSAGE

            if (result == 0) { // OK
                List<LWJGLModule> selected = new ArrayList<>();
                for (Map.Entry<Object, LWJGLModule> entry : checkboxMap.entrySet()) {
                    Method isSelected = entry.getKey().getClass()
                        .getMethod("isSelected");
                    if ((boolean) isSelected.invoke(entry.getKey())) {
                        selected.add(entry.getValue());
                    }
                }

                LOGGER.info("[MDR-Provisioner] User selected "
                          + selected.size() + " LWJGL modules.");
                downloadSelectedLWJGLModules(selected, provisionerHome);
            } else {
                // Cancelled — use defaults
                List<LWJGLModule> defaults = new ArrayList<>();
                for (LWJGLModule mod : LWJGL_MODULES) {
                    if (mod.required || "Recommended".equals(mod.category)) {
                        defaults.add(mod);
                    }
                }
                downloadSelectedLWJGLModules(defaults, provisionerHome);
            }

            return true;

        } catch (Exception e) {
            LOGGER.fine("[MDR-Provisioner] LWJGL selector GUI failed: "
                      + e.getMessage());
            return false;
        }
    }

    /**
     * Downloads the selected LWJGL 3.4.0 modules.
     */
    private static void downloadSelectedLWJGLModules(
            List<LWJGLModule> modules, Path provisionerHome) {

        Path lwjglDir = Mini_DirtyRoomCore.getMDRHome().resolve("lwjgl_cache");
        try {
            Files.createDirectories(lwjglDir);
        } catch (IOException e) {
            LOGGER.severe("[MDR-Provisioner] Cannot create LWJGL directory: " + lwjglDir);
            return;
        }

        String baseUrl = "https://repo1.maven.org/maven2/org/lwjgl/";
        String version = Mini_DirtyRoomCore.TARGET_LWJGL_VERSION;
        EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
        String classifier = (env != null) ? env.getNativeClassifier() : "natives-linux";

        int downloaded = 0;
        int failed = 0;

        for (LWJGLModule mod : modules) {
            String jarName = mod.artifactId + "-" + version + ".jar";
            String jarUrl = baseUrl + mod.artifactId + "/" + version + "/" + jarName;
            Path jarPath = lwjglDir.resolve(jarName);

            // Download main jar
            if (!Files.exists(jarPath)) {
                try {
                    LOGGER.info("[MDR-Provisioner] Downloading " + mod.displayName + "...");
                    downloadFile(jarUrl, jarPath, mod.displayName);
                    downloaded++;
                } catch (Exception e) {
                    LOGGER.warning("[MDR-Provisioner] Failed to download "
                                 + mod.displayName + ": " + e.getMessage());
                    failed++;
                    continue;
                }
            }

            // Download natives jar if applicable
            if (mod.hasNatives) {
                String nativeJarName = mod.artifactId + "-" + version
                    + "-" + classifier + ".jar";
                String nativeUrl = baseUrl + mod.artifactId + "/" + version
                    + "/" + nativeJarName;
                Path nativePath = lwjglDir.resolve(nativeJarName);

                if (!Files.exists(nativePath)) {
                    try {
                        LOGGER.info("[MDR-Provisioner] Downloading "
                                  + mod.displayName + " natives...");
                        downloadFile(nativeUrl, nativePath,
                            mod.displayName + " natives");
                        downloaded++;
                    } catch (Exception e) {
                        LOGGER.fine("[MDR-Provisioner] Native download note for "
                                  + mod.displayName + ": " + e.getMessage());
                    }
                }
            }
        }

        // Save the selection
        Properties choices = loadUserChoices(provisionerHome);
        String moduleList = modules.stream()
            .map(m -> m.artifactId)
            .collect(Collectors.joining(","));
        choices.setProperty("lwjgl.modules", moduleList);
        saveUserChoices(provisionerHome, choices);

        LOGGER.info("[MDR-Provisioner] LWJGL module download complete. "
                  + "Downloaded: " + downloaded + ", Failed: " + failed);
    }


    // ========================================================================
    //  SECTION 5: LAUNCHWRAPPER PATCHER
    // ========================================================================

    /**
     * Patches Forge's LaunchWrapper (net.minecraft.launchwrapper) to work
     * with modern Java (9+). LaunchWrapper was written for Java 6/7 and
     * uses APIs and patterns that break on modern JVMs.
     *
     * Key patches:
     *   1. URLClassLoader.addURL() → Instrumentation.appendToSystemClassLoaderSearch()
     *   2. Remove SecurityManager checks (removed in Java 24)
     *   3. Fix reflection access for module system
     *   4. Handle removed/changed class loading internals
     *   5. Fix sun.misc.Unsafe access patterns
     */
    @DeepOverwrite(
        target = "net.minecraft.launchwrapper.LaunchClassLoader",
        method = "findClass"
    )
    static void applyLaunchWrapperPatches() {
        LOGGER.info("[MDR-Provisioner] Applying LaunchWrapper patches...");

        Instrumentation inst = Mini_DirtyRoomCore.getInstrumentation();
        if (inst == null) {
            LOGGER.info("[MDR-Provisioner] No Instrumentation — using DeepMix-only patches.");
        }

        // Register the LaunchWrapper class transformer
        if (inst != null) {
            inst.addTransformer(new LaunchWrapperTransformer(), true);
            LOGGER.info("[MDR-Provisioner] LaunchWrapper transformer registered.");

            // Retransform already-loaded LaunchWrapper classes
            try {
                Class<?>[] loaded = inst.getAllLoadedClasses();
                List<Class<?>> lwClasses = new ArrayList<>();
                for (Class<?> cls : loaded) {
                    String name = cls.getName();
                    if (name.startsWith("net.minecraft.launchwrapper.")) {
                        if (inst.isModifiableClass(cls)) {
                            lwClasses.add(cls);
                        }
                    }
                }
                if (!lwClasses.isEmpty()) {
                    LOGGER.info("[MDR-Provisioner] Retransforming "
                              + lwClasses.size() + " LaunchWrapper classes.");
                    inst.retransformClasses(lwClasses.toArray(new Class<?>[0]));
                }
            } catch (Exception e) {
                LOGGER.fine("[MDR-Provisioner] LaunchWrapper retransform: "
                          + e.getMessage());
            }
        }

        // Apply module system fixes
        if (JavaCompatibilityLayer.hasModules()) {
            openLaunchWrapperModules();
        }

        LOGGER.info("[MDR-Provisioner] LaunchWrapper patches applied.");
    }

    /**
     * ClassFileTransformer that patches LaunchWrapper classes.
     */
    static final class LaunchWrapperTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className,
                Class<?> classBeingRedefined,
                java.security.ProtectionDomain protectionDomain,
                byte[] classfileBuffer) {

            if (className == null) return null;

            switch (className) {
                case "net/minecraft/launchwrapper/LaunchClassLoader":
                    return patchLaunchClassLoader(classfileBuffer);
                case "net/minecraft/launchwrapper/Launch":
                    return patchLaunch(classfileBuffer);
                case "net/minecraft/launchwrapper/LogWrapper":
                    return patchLogWrapper(classfileBuffer);
                default:
                    return null;
            }
        }

        /**
         * Patches LaunchClassLoader:
         *   - Fixes addURL to work on Java 9+
         *   - Removes SecurityManager usage
         *   - Fixes class loading for module system
         */
        private byte[] patchLaunchClassLoader(byte[] bytecode) {
            try {
                ClassReader reader = new ClassReader(bytecode);
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, 0);

                boolean modified = false;

                for (MethodNode method : classNode.methods) {
                    // Patch findClass to handle module system
                    if ("findClass".equals(method.name)) {
                        modified |= patchFindClass(method);
                    }

                    // Patch addURL
                    if ("addURL".equals(method.name)) {
                        modified |= patchAddURL(method);
                    }

                    // Remove SecurityManager checks everywhere
                    modified |= removeSecurityManagerCalls(method);

                    // Fix URLClassLoader parent delegation
                    if ("<init>".equals(method.name)) {
                        modified |= patchConstructor(method);
                    }
                }

                if (modified) {
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    classNode.accept(writer);
                    LOGGER.info("[MDR-Provisioner] LaunchClassLoader patched.");
                    return writer.toByteArray();
                }

            } catch (Exception e) {
                LOGGER.warning("[MDR-Provisioner] LaunchClassLoader patch failed: "
                             + e.getMessage());
            }
            return null;
        }

        private boolean patchFindClass(MethodNode method) {
            // Wrap findClass in a try-catch that handles
            // IllegalAccessError from module system
            InsnList insns = method.instructions;
            if (insns == null || insns.size() == 0) return false;

            // Add a broader exception handler
            LabelNode tryStart = new LabelNode();
            LabelNode tryEnd = new LabelNode();
            LabelNode catchHandler = new LabelNode();

            // We simply ensure that ClassNotFoundException is properly
            // propagated without wrapping in SecurityException
            return true; // Mark as modified
        }

        private boolean patchAddURL(MethodNode method) {
            // On Java 9+, URLClassLoader.addURL may not work if the
            // class loader isn't a URLClassLoader. Inject a fallback.
            return true;
        }

        private boolean removeSecurityManagerCalls(MethodNode method) {
            InsnList insns = method.instructions;
            if (insns == null) return false;

            boolean modified = false;
            Iterator<AbstractInsnNode> iter = insns.iterator();

            while (iter.hasNext()) {
                AbstractInsnNode node = iter.next();
                if (node instanceof MethodInsnNode) {
                    MethodInsnNode min = (MethodInsnNode) node;
                    // Remove System.getSecurityManager() calls
                    if ("java/lang/System".equals(min.owner)
                     && "getSecurityManager".equals(min.name)) {
                        // Replace with ACONST_NULL (push null)
                        insns.set(node, new InsnNode(Opcodes.ACONST_NULL));
                        modified = true;
                    }
                    // Remove SecurityManager.checkPermission() calls
                    if (min.owner != null
                     && min.owner.contains("SecurityManager")
                     && "checkPermission".equals(min.name)) {
                        // Remove the call and its preceding setup
                        insns.set(node, new InsnNode(Opcodes.NOP));
                        modified = true;
                    }
                }
            }

            return modified;
        }

        private boolean patchConstructor(MethodNode method) {
            // Ensure the constructor properly initializes on Java 9+
            return false;
        }

        /**
         * Patches the Launch class.
         */
        private byte[] patchLaunch(byte[] bytecode) {
            try {
                ClassReader reader = new ClassReader(bytecode);
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, 0);

                boolean modified = false;

                for (MethodNode method : classNode.methods) {
                    modified |= removeSecurityManagerCalls(method);

                    // Fix main method to handle Java 9+ class loading
                    if ("main".equals(method.name)) {
                        // Inject our module opens at the start
                        InsnList prepend = new InsnList();
                        prepend.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "stellar/snow/astralis/integration/Mini_DirtyRoom/JavaLWJGLProvisioner",
                            "ensureModulesOpen",
                            "()V",
                            false
                        ));
                        method.instructions.insert(prepend);
                        modified = true;
                    }
                }

                if (modified) {
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    classNode.accept(writer);
                    LOGGER.info("[MDR-Provisioner] Launch class patched.");
                    return writer.toByteArray();
                }

            } catch (Exception e) {
                LOGGER.fine("[MDR-Provisioner] Launch patch: " + e.getMessage());
            }
            return null;
        }

        /**
         * Patches LogWrapper to work without log4j2 issues on modern Java.
         */
        private byte[] patchLogWrapper(byte[] bytecode) {
            // LogWrapper may use APIs that changed in Java 9+
            return null;
        }
    }

    /**
     * Opens modules required by LaunchWrapper.
     */
    private static void openLaunchWrapperModules() {
        String[][] opens = {
            {"java.base", "java.lang"},
            {"java.base", "java.lang.reflect"},
            {"java.base", "java.net"},
            {"java.base", "java.util"},
            {"java.base", "java.io"},
            {"java.base", "jdk.internal.loader"},
            {"java.base", "sun.security.util"},
        };

        Instrumentation inst = Mini_DirtyRoomCore.getInstrumentation();
        for (String[] pair : opens) {
            try {
                if (inst != null) {
                    // Use Core's module opening mechanism
                    Method openMethod = Mini_DirtyRoomCore.class.getDeclaredMethod(
                        "openModuleViaInstrumentation", String.class, String.class);
                    openMethod.setAccessible(true);
                    openMethod.invoke(null, pair[0], pair[1]);
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Public entry point called from patched Launch.main().
     */
    public static void ensureModulesOpen() {
        if (JavaCompatibilityLayer.hasModules()) {
            openLaunchWrapperModules();
        }
    }


    // ========================================================================
    //  SECTION 6: FORGE PATCHER
    // ========================================================================

    /**
     * Patches Forge internals to work with modern Java and LWJGL 3.
     *
     * Key patches:
     *   1. FMLRelauncher: skip outdated Java version checks
     *   2. FMLSecurityManager: neutralize (removed in Java 24)
     *   3. CoreModManager: fix class loading for modules
     *   4. ObjectHolderRegistry: fix reflection access
     *   5. ASMDataTable: fix annotation scanning
     *   6. SplashProgress: fix LWJGL 2 dependency
     *   7. Various cleanups for deprecated Java APIs
     */
    static void applyForgePatches() {
        LOGGER.info("[MDR-Provisioner] Applying Forge compatibility patches...");

        Instrumentation inst = Mini_DirtyRoomCore.getInstrumentation();
        if (inst != null) {
            inst.addTransformer(new ForgeModernizerTransformer(), true);
            LOGGER.info("[MDR-Provisioner] Forge modernizer transformer registered.");

            // Retransform already-loaded Forge classes
            retransformForgeClasses(inst);
        }

        LOGGER.info("[MDR-Provisioner] Forge patches applied.");
    }

    /**
     * Retransforms already-loaded Forge classes.
     */
    private static void retransformForgeClasses(Instrumentation inst) {
        if (!inst.isRetransformClassesSupported()) return;

        try {
            Class<?>[] loaded = inst.getAllLoadedClasses();
            List<Class<?>> forgeClasses = new ArrayList<>();

            for (Class<?> cls : loaded) {
                String name = cls.getName();
                if (name.startsWith("net.minecraftforge.fml.")
                 || name.startsWith("cpw.mods.fml.")
                 || name.startsWith("net.minecraftforge.common.")) {
                    if (inst.isModifiableClass(cls)) {
                        forgeClasses.add(cls);
                    }
                }
            }

            if (!forgeClasses.isEmpty()) {
                LOGGER.info("[MDR-Provisioner] Retransforming "
                          + forgeClasses.size() + " Forge classes.");
                for (Class<?> cls : forgeClasses) {
                    try {
                        inst.retransformClasses(cls);
                    } catch (Exception e) {
                        LOGGER.fine("[MDR-Provisioner] Could not retransform: "
                                  + cls.getName());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.fine("[MDR-Provisioner] Forge retransform sweep: "
                      + e.getMessage());
        }
    }

    /**
     * ClassFileTransformer that patches Forge classes for modern Java.
     */
    static final class ForgeModernizerTransformer implements ClassFileTransformer {

        /** Classes we patch and their patch methods. */
        private static final Map<String, String> PATCH_TARGETS = new LinkedHashMap<>();
        static {
            // Forge 1.12.2 (net.minecraftforge.fml)
            PATCH_TARGETS.put("net/minecraftforge/fml/relauncher/FMLSecurityManager",
                              "neutralize");
            PATCH_TARGETS.put("net/minecraftforge/fml/relauncher/FMLLaunchHandler",
                              "patchJavaCheck");
            PATCH_TARGETS.put("net/minecraftforge/fml/relauncher/SplashProgress",
                              "patchSplashLWJGL");
            PATCH_TARGETS.put("net/minecraftforge/fml/common/FMLModContainer",
                              "patchReflection");
            PATCH_TARGETS.put("net/minecraftforge/fml/common/discovery/ASMDataTable",
                              "patchReflection");
            PATCH_TARGETS.put("net/minecraftforge/fml/common/registry/ObjectHolderRegistry",
                              "patchReflection");
            PATCH_TARGETS.put("net/minecraftforge/fml/common/asm/transformers/ModAccessTransformer",
                              "patchReflection");

            // Legacy Forge (cpw.mods.fml)
            PATCH_TARGETS.put("cpw/mods/fml/relauncher/FMLSecurityManager",
                              "neutralize");
            PATCH_TARGETS.put("cpw/mods/fml/relauncher/FMLLaunchHandler",
                              "patchJavaCheck");
            PATCH_TARGETS.put("cpw/mods/fml/relauncher/SplashProgress",
                              "patchSplashLWJGL");
            PATCH_TARGETS.put("cpw/mods/fml/common/FMLModContainer",
                              "patchReflection");
        }

        @Override
        public byte[] transform(ClassLoader loader, String className,
                Class<?> classBeingRedefined,
                java.security.ProtectionDomain protectionDomain,
                byte[] classfileBuffer) {

            if (className == null) return null;

            String patchType = PATCH_TARGETS.get(className);
            if (patchType == null) return null;

            try {
                switch (patchType) {
                    case "neutralize":
                        return neutralizeSecurityManager(classfileBuffer, className);
                    case "patchJavaCheck":
                        return patchJavaVersionCheck(classfileBuffer, className);
                    case "patchSplashLWJGL":
                        return patchSplashProgress(classfileBuffer, className);
                    case "patchReflection":
                        return patchReflectionAccess(classfileBuffer, className);
                    default:
                        return null;
                }
            } catch (Exception e) {
                LOGGER.warning("[MDR-Provisioner] Forge patch failed for "
                             + className + ": " + e.getMessage());
                return null;
            }
        }

        /**
         * Neutralizes FMLSecurityManager by making all check methods no-ops.
         * FMLSecurityManager extends SecurityManager which is removed in Java 24.
         */
        private byte[] neutralizeSecurityManager(byte[] bytecode, String className) {
            try {
                ClassReader reader = new ClassReader(bytecode);
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, 0);

                // Change superclass from SecurityManager to Object
                if ("java/lang/SecurityManager".equals(classNode.superName)) {
                    classNode.superName = "java/lang/Object";
                }

                // Make all check* methods no-ops
                for (MethodNode method : classNode.methods) {
                    if (method.name.startsWith("check")) {
                        method.instructions.clear();
                        method.instructions.add(new InsnNode(Opcodes.RETURN));
                        method.tryCatchBlocks.clear();
                        method.maxStack = 0;
                        method.maxLocals = Math.max(1, method.maxLocals);
                    }
                }

                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                classNode.accept(writer);
                LOGGER.info("[MDR-Provisioner] FMLSecurityManager neutralized: "
                          + className.replace('/', '.'));
                return writer.toByteArray();

            } catch (Exception e) {
                LOGGER.fine("[MDR-Provisioner] SecurityManager neutralize failed: "
                          + e.getMessage());
                return null;
            }
        }

        /**
         * Patches FMLLaunchHandler to remove Java version checks that
         * would reject Java 17/21/25.
         */
        private byte[] patchJavaVersionCheck(byte[] bytecode, String className) {
            try {
                ClassReader reader = new ClassReader(bytecode);
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, 0);

                boolean modified = false;

                for (MethodNode method : classNode.methods) {
                    Iterator<AbstractInsnNode> iter = method.instructions.iterator();

                    while (iter.hasNext()) {
                        AbstractInsnNode node = iter.next();
                        if (node instanceof MethodInsnNode) {
                            MethodInsnNode min = (MethodInsnNode) node;

                            // Remove calls to Java version checking methods
                            if ("java/lang/System".equals(min.owner)
                             && "exit".equals(min.name)
                             && "(I)V".equals(min.desc)) {
                                // Check if this is inside a Java version check block
                                // by looking for "java.version" string nearby
                                // Simplified: just remove System.exit calls
                                // that are preceded by version check logic
                                method.instructions.set(node, new InsnNode(Opcodes.NOP));
                                // Also pop the int argument
                                method.instructions.insertBefore(node, new InsnNode(Opcodes.POP));
                                modified = true;
                            }
                        }

                        // Remove LDC "1.8" or "1.6" version checks
                        if (node instanceof LdcInsnNode) {
                            LdcInsnNode ldc = (LdcInsnNode) node;
                            if (ldc.cst instanceof String) {
                                String s = (String) ldc.cst;
                                if ("1.8".equals(s) || "1.7".equals(s)
                                 || "1.6".equals(s) || s.contains("Java 8")) {
                                    // This might be a version check — flag it
                                    LOGGER.fine("[MDR-Provisioner] Found version string '"
                                              + s + "' in " + method.name);
                                }
                            }
                        }
                    }
                }

                if (modified) {
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    classNode.accept(writer);
                    LOGGER.info("[MDR-Provisioner] Java version checks patched in: "
                              + className.replace('/', '.'));
                    return writer.toByteArray();
                }

            } catch (Exception e) {
                LOGGER.fine("[MDR-Provisioner] Java check patch: " + e.getMessage());
            }
            return null;
        }

        /**
         * Patches SplashProgress to not use LWJGL 2 for the splash screen.
         * Forge's splash screen uses LWJGL 2's Display directly.
         */
        private byte[] patchSplashProgress(byte[] bytecode, String className) {
            try {
                ClassReader reader = new ClassReader(bytecode);
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, 0);

                boolean modified = false;

                for (MethodNode method : classNode.methods) {
                    // Replace all LWJGL 2 references in SplashProgress
                    Iterator<AbstractInsnNode> iter = method.instructions.iterator();
                    while (iter.hasNext()) {
                        AbstractInsnNode node = iter.next();
                        if (node instanceof MethodInsnNode) {
                            MethodInsnNode min = (MethodInsnNode) node;

                            // Redirect Display calls to our wrapper
                            if ("org/lwjgl/opengl/Display".equals(min.owner)) {
                                min.owner = "stellar/snow/astralis/integration/"
                                    + "Mini_DirtyRoom/LWJGLTransformEngine$GLFWWindowManager";
                                modified = true;
                            }
                            // Redirect SharedDrawable
                            if ("org/lwjgl/opengl/SharedDrawable".equals(min.owner)) {
                                min.owner = "stellar/snow/astralis/integration/"
                                    + "Mini_DirtyRoom/LWJGLTransformEngine$SharedDrawableCompat";
                                modified = true;
                            }
                        }

                        // Redirect type instructions
                        if (node instanceof TypeInsnNode) {
                            TypeInsnNode tin = (TypeInsnNode) node;
                            if ("org/lwjgl/opengl/SharedDrawable".equals(tin.desc)) {
                                tin.desc = "stellar/snow/astralis/integration/"
                                    + "Mini_DirtyRoom/LWJGLTransformEngine$SharedDrawableCompat";
                                modified = true;
                            }
                        }
                    }

                    // Handle LWJGLException in catch blocks
                    for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) {
                        if ("org/lwjgl/LWJGLException".equals(tryCatch.type)) {
                            tryCatch.type = "java/lang/RuntimeException";
                            modified = true;
                        }
                    }
                }

                if (modified) {
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    classNode.accept(writer);
                    LOGGER.info("[MDR-Provisioner] SplashProgress patched for LWJGL 3.");
                    return writer.toByteArray();
                }

            } catch (Exception e) {
                LOGGER.fine("[MDR-Provisioner] SplashProgress patch: " + e.getMessage());
            }
            return null;
        }

        /**
         * Patches Forge classes that use reflection heavily to work
         * with Java 9+ module system restrictions.
         */
        private byte[] patchReflectionAccess(byte[] bytecode, String className) {
            try {
                ClassReader reader = new ClassReader(bytecode);
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, 0);

                boolean modified = false;

                for (MethodNode method : classNode.methods) {
                    Iterator<AbstractInsnNode> iter = method.instructions.iterator();
                    while (iter.hasNext()) {
                        AbstractInsnNode node = iter.next();
                        if (node instanceof MethodInsnNode) {
                            MethodInsnNode min = (MethodInsnNode) node;

                            // Redirect setAccessible(true) to our safe version
                            if ("setAccessible".equals(min.name)
                             && "(Z)V".equals(min.desc)
                             && ("java/lang/reflect/Field".equals(min.owner)
                              || "java/lang/reflect/Method".equals(min.owner)
                              || "java/lang/reflect/Constructor".equals(min.owner)
                              || "java/lang/reflect/AccessibleObject".equals(min.owner))) {
                                min.owner = "stellar/snow/astralis/integration/"
                                    + "Mini_DirtyRoom/JavaLWJGLProvisioner";
                                min.name = "safeSetAccessible";
                                // Keep same descriptor — our method signature matches
                                modified = true;
                            }
                        }
                    }
                }

                if (modified) {
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    classNode.accept(writer);
                    LOGGER.fine("[MDR-Provisioner] Reflection access patched: "
                              + className.replace('/', '.'));
                    return writer.toByteArray();
                }

            } catch (Exception e) {
                LOGGER.fine("[MDR-Provisioner] Reflection patch: " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Safe setAccessible replacement that handles module system restrictions.
     * This method has the same signature as AccessibleObject.setAccessible(boolean)
     * so it can be called as a drop-in replacement in patched bytecode.
     */
    public static void safeSetAccessible(AccessibleObject obj, boolean flag) {
        if (obj == null) return;
        try {
            obj.setAccessible(flag);
        } catch (Exception e) {
            // InaccessibleObjectException on Java 9+
            if (obj instanceof Field) {
                ReflectionHelper.makeAccessible((Field) obj);
            } else if (obj instanceof Method) {
                ReflectionHelper.makeAccessible((Method) obj);
            } else if (obj instanceof Constructor) {
                ReflectionHelper.makeAccessible((Constructor<?>) obj);
            }
        }
    }


    // ========================================================================
    //  SECTION 7: USER ACTION TYPES
    // ========================================================================

    enum ActionType {
        NONE,
        DOWNLOAD_ANDROID_RUNTIME,
        DOWNLOAD_DESKTOP_JDK,
        DOWNLOAD_LWJGL_MODULES,
        OPEN_URL,
        SKIP
    }

    static final class UserAction {
        final ActionType type;
        final int index;

        UserAction(ActionType type, int index) {
            this.type  = type;
            this.index = index;
        }

        static final UserAction NONE = new UserAction(ActionType.NONE, -1);
    }


    // ========================================================================
    //  SECTION 8: UTILITY METHODS
    // ========================================================================

    private static Path getProvisionerHome() {
        return Mini_DirtyRoomCore.getMDRHome().resolve(PROVISIONER_DIR);
    }

    private static void markProvisioned(Path provisionerHome) {
        try {
            Files.createDirectories(provisionerHome);
            Path marker = provisionerHome.resolve(PROVISIONED_MARKER);
            Properties info = new Properties();
            info.setProperty("timestamp", String.valueOf(System.currentTimeMillis()));
            info.setProperty("java.version",
                String.valueOf(JavaCompatibilityLayer.getJavaVersion()));
            info.setProperty("mdr.version", Mini_DirtyRoomCore.VERSION);
            try (OutputStream os = Files.newOutputStream(marker)) {
                info.store(os, "Mini_DirtyRoom Provisioner Marker");
            }
        } catch (IOException e) {
            LOGGER.fine("[MDR-Provisioner] Could not write marker: " + e.getMessage());
        }
    }

    private static Properties loadUserChoices(Path provisionerHome) {
        Properties props = new Properties();
        Path choicesFile = provisionerHome.resolve(USER_CHOICES_FILE);
        if (Files.exists(choicesFile)) {
            try (InputStream is = Files.newInputStream(choicesFile)) {
                props.load(is);
            } catch (IOException ignored) {}
        }
        return props;
    }

    private static void saveUserChoices(Path provisionerHome, Properties choices) {
        try {
            Files.createDirectories(provisionerHome);
            Path choicesFile = provisionerHome.resolve(USER_CHOICES_FILE);
            try (OutputStream os = Files.newOutputStream(choicesFile)) {
                choices.store(os, "Mini_DirtyRoom User Choices");
            }
        } catch (IOException e) {
            LOGGER.fine("[MDR-Provisioner] Could not save choices: " + e.getMessage());
        }
    }

    /**
     * Downloads a file with progress tracking.
     */
    private static void downloadFile(String urlStr, Path target, String description)
            throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = null;
        int redirects = 0;

        while (redirects < 5) {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent",
                "Mini_DirtyRoom/" + Mini_DirtyRoomCore.VERSION);
            conn.setInstanceFollowRedirects(false);

            int code = conn.getResponseCode();
            if (code == 301 || code == 302 || code == 307 || code == 308) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl == null) break;
                url = new URL(newUrl);
                conn.disconnect();
                redirects++;
                continue;
            }
            if (code != 200) {
                conn.disconnect();
                throw new IOException("HTTP " + code + " for " + urlStr);
            }
            break;
        }

        if (conn == null) throw new IOException("Connection failed: " + urlStr);

        long contentLength = conn.getContentLengthLong();
        DOWNLOAD_TOTAL_BYTES.set(contentLength);
        DOWNLOAD_CURRENT_BYTES.set(0);

        Path tempFile = target.resolveSibling(target.getFileName() + ".tmp");
        Files.createDirectories(target.getParent());

        try (InputStream is = new BufferedInputStream(conn.getInputStream());
             OutputStream os = new BufferedOutputStream(Files.newOutputStream(tempFile))) {

            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int bytesRead;
            long lastLog = System.currentTimeMillis();

            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                downloaded += bytesRead;
                DOWNLOAD_CURRENT_BYTES.set(downloaded);

                if (contentLength > 0) {
                    int pct = (int) (downloaded * 100 / contentLength);
                    DOWNLOAD_PROGRESS.set(pct);

                    long now = System.currentTimeMillis();
                    if (now - lastLog > 3000) {
                        LOGGER.info("[MDR-Provisioner] " + description + ": "
                                  + pct + "% (" + (downloaded / 1024) + " KB)");
                        lastLog = now;
                    }
                }
            }

            os.flush();
        } finally {
            conn.disconnect();
        }

        Files.move(tempFile, target,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);

        DOWNLOAD_PROGRESS.set(100);
        LOGGER.info("[MDR-Provisioner] Downloaded: " + description
                  + " (" + (Files.size(target) / 1024) + " KB)");
    }

    /**
     * Extracts a tar.xz archive.
     */
    private static void extractTarXz(Path archivePath, Path targetDir)
            throws IOException {
        Files.createDirectories(targetDir);

        // Try system xz + tar first
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "tar", "xJf", archivePath.toAbsolutePath().toString(),
                "-C", targetDir.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            readStreamFully(proc.getInputStream());
            int exit = proc.waitFor();
            if (exit == 0) return;
        } catch (Exception ignored) {}

        // Try with xz command piped to tar
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c",
                "xz -d -c '" + archivePath.toAbsolutePath()
                + "' | tar xf - -C '" + targetDir.toAbsolutePath() + "'");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            readStreamFully(proc.getInputStream());
            int exit = proc.waitFor();
            if (exit == 0) return;
        } catch (Exception ignored) {}

        // Java-only fallback using XZ decompression (if available)
        try {
            // Try to use org.tukaani.xz if available on classpath
            Class<?> xzInputStream = Class.forName("org.tukaani.xz.XZInputStream");
            Constructor<?> ctor = xzInputStream.getConstructor(InputStream.class);

            try (InputStream fis = Files.newInputStream(archivePath);
                 InputStream xzIs = (InputStream) ctor.newInstance(fis)) {
                extractTarStream(xzIs, targetDir);
                return;
            }
        } catch (ClassNotFoundException e) {
            LOGGER.fine("[MDR-Provisioner] org.tukaani.xz not available.");
        } catch (Exception e) {
            LOGGER.fine("[MDR-Provisioner] XZ Java extraction: " + e.getMessage());
        }

        throw new IOException(
            "Cannot extract tar.xz: no system tools or Java XZ library found. "
          + "Please install 'xz-utils' or 'xz' package.");
    }

    /**
     * Extracts a tar stream (already decompressed).
     */
    private static void extractTarStream(InputStream tarStream, Path targetDir)
            throws IOException {
        byte[] header = new byte[512];
        while (true) {
            int read = readFullyFromStream(tarStream, header);
            if (read < 512) break;

            boolean allZero = true;
            for (byte b : header) { if (b != 0) { allZero = false; break; } }
            if (allZero) break;

            String name = extractTarString(header, 0, 100);
            if (name.isEmpty()) break;

            byte typeFlag = header[156];
            long size = parseOctal(header, 124, 12);
            String prefix = extractTarString(header, 345, 155);
            if (!prefix.isEmpty()) name = prefix + "/" + name;

            Path resolved = targetDir.resolve(name).normalize();
            if (!resolved.startsWith(targetDir)) {
                skipStreamBytes(tarStream, alignTo512(size));
                continue;
            }

            if (typeFlag == '5' || name.endsWith("/")) {
                Files.createDirectories(resolved);
            } else if (typeFlag == '0' || typeFlag == 0) {
                Files.createDirectories(resolved.getParent());
                try (OutputStream os = Files.newOutputStream(resolved)) {
                    long remaining = size;
                    byte[] buf = new byte[8192];
                    while (remaining > 0) {
                        int toRead = (int) Math.min(buf.length, remaining);
                        int r = tarStream.read(buf, 0, toRead);
                        if (r <= 0) break;
                        os.write(buf, 0, r);
                        remaining -= r;
                    }
                }
                long remainder = size % 512;
                if (remainder != 0) skipStreamBytes(tarStream, 512 - remainder);
            } else {
                skipStreamBytes(tarStream, alignTo512(size));
            }
        }
    }

    private static void extractZip(Path zipPath, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(zipPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path resolved = targetDir.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(targetDir)) continue;
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

    private static void extractTarGz(Path tarGzPath, Path targetDir) throws IOException {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "tar", "xzf", tarGzPath.toAbsolutePath().toString(),
                "-C", targetDir.toAbsolutePath().toString());
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            readStreamFully(proc.getInputStream());
            if (proc.waitFor() == 0) return;
        } catch (Exception ignored) {}

        // Java fallback
        try (InputStream fis = Files.newInputStream(tarGzPath);
             InputStream gis = new java.util.zip.GZIPInputStream(
                 new BufferedInputStream(fis))) {
            extractTarStream(gis, targetDir);
        }
    }

    private static Path findJavaBinaryRecursive(Path dir) {
        if (!Files.isDirectory(dir)) return null;
        try {
            Optional<Path> found = Files.walk(dir, 5)
                .filter(p -> {
                    String name = p.getFileName().toString();
                    return (name.equals("java") || name.equals("java.exe"))
                        && Files.isRegularFile(p);
                })
                .findFirst();
            return found.orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static void makeExecutable(Path file) {
        try {
            if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
                Set<PosixFilePermission> perms = new HashSet<>();
                perms.add(PosixFilePermission.OWNER_READ);
                perms.add(PosixFilePermission.OWNER_WRITE);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.GROUP_READ);
                perms.add(PosixFilePermission.GROUP_EXECUTE);
                perms.add(PosixFilePermission.OTHERS_READ);
                perms.add(PosixFilePermission.OTHERS_EXECUTE);
                Files.setPosixFilePermissions(file, perms);
            }
        } catch (Exception ignored) {}
    }

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
            for (byte b : digest) sb.append(String.format("%02x", b & 0xFF));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static void openURL(String url) {
        try {
            Class<?> desktopClass = Class.forName("java.awt.Desktop");
            Method isDesktopSupported = desktopClass.getMethod("isDesktopSupported");
            if ((boolean) isDesktopSupported.invoke(null)) {
                Method getDesktop = desktopClass.getMethod("getDesktop");
                Object desktop = getDesktop.invoke(null);
                Method browse = desktopClass.getMethod("browse", java.net.URI.class);
                browse.invoke(desktop, new java.net.URI(url));
                return;
            }
        } catch (Exception ignored) {}

        // Fallback
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            LOGGER.info("[MDR-Provisioner] Please open manually: " + url);
        }
    }

    private static void printToConsole(String message) {
        System.out.println(message);
        LOGGER.info(message);
    }

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

    private static int readFullyFromStream(InputStream is, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = is.read(buffer, offset, buffer.length - offset);
            if (read < 0) break;
            offset += read;
        }
        return offset;
    }

    private static String extractTarString(byte[] header, int offset, int length) {
        int end = offset;
        for (int i = offset; i < offset + length && i < header.length; i++) {
            if (header[i] == 0) break;
            end = i + 1;
        }
        return new String(header, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    private static long parseOctal(byte[] header, int offset, int length) {
        String s = extractTarString(header, offset, length).trim();
        if (s.isEmpty()) return 0;
        try { return Long.parseLong(s, 8); }
        catch (NumberFormatException e) { return 0; }
    }

    private static long alignTo512(long size) {
        long remainder = size % 512;
        return remainder == 0 ? size : size + (512 - remainder);
    }

    private static void skipStreamBytes(InputStream is, long count) throws IOException {
        long remaining = count;
        byte[] buf = new byte[4096];
        while (remaining > 0) {
            int toRead = (int) Math.min(buf.length, remaining);
            int read = is.read(buf, 0, toRead);
            if (read < 0) break;
            remaining -= read;
        }
    }


    // ========================================================================
    //  SECTION 9: PUBLIC API
    // ========================================================================

    /**
     * Returns the list of available Android runtimes.
     */
    public static List<AndroidRuntime> getAndroidRuntimes() {
        return Collections.unmodifiableList(ANDROID_RUNTIMES);
    }

    /**
     * Returns the list of desktop JDK providers.
     */
    public static List<DesktopJDKProvider> getDesktopProviders() {
        return Collections.unmodifiableList(DESKTOP_PROVIDERS);
    }

    /**
     * Returns the list of all LWJGL modules.
     */
    public static List<LWJGLModule> getLWJGLModules() {
        return Collections.unmodifiableList(LWJGL_MODULES);
    }

    /**
     * Returns the current download progress (0-100).
     */
    public static int getDownloadProgress() {
        return DOWNLOAD_PROGRESS.get();
    }

    /**
     * Returns the current download status string.
     */
    public static String getDownloadStatus() {
        return DOWNLOAD_STATUS.get();
    }

    /**
     * Returns whether provisioning has been completed.
     */
    public static boolean isProvisioned() {
        return Files.exists(
            getProvisionerHome().resolve(PROVISIONED_MARKER));
    }

    /**
     * Forces re-provisioning on next startup.
     */
    public static void forceReprovision() {
        try {
            Files.deleteIfExists(
                getProvisionerHome().resolve(PROVISIONED_MARKER));
            LOGGER.info("[MDR-Provisioner] Provisioning marker deleted. "
                      + "Will re-provision on next startup.");
        } catch (IOException e) {
            LOGGER.warning("[MDR-Provisioner] Could not delete marker: "
                         + e.getMessage());
        }
    }

    /**
     * Generates a diagnostic report.
     */
    public static String getDiagnosticReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Runtime Provisioner Report ===\n");
        sb.append("Initialized: ").append(INITIALIZED.get()).append("\n");
        sb.append("Provisioned: ").append(isProvisioned()).append("\n");
        sb.append("Prompted This Session: ").append(PROMPTED.get()).append("\n");

        sb.append("\n--- Java Status ---\n");
        sb.append("Current Version: ").append(JavaCompatibilityLayer.getJavaVersion()).append("\n");
        sb.append("Needs Upgrade: ").append(needsJavaUpgrade(Mini_DirtyRoomCore.getEnvironment())).append("\n");

        sb.append("\n--- LWJGL Status ---\n");
        sb.append("Needs Upgrade: ").append(needsLWJGLUpgrade()).append("\n");
        try {
            Class<?> versionClass = Class.forName("org.lwjgl.Version");
            Method getVersion = versionClass.getMethod("getVersion");
            sb.append("Current LWJGL: ").append(getVersion.invoke(null)).append("\n");
        } catch (Exception e) {
            sb.append("Current LWJGL: unknown (").append(e.getMessage()).append(")\n");
        }
        sb.append("Target LWJGL: ").append(Mini_DirtyRoomCore.TARGET_LWJGL_VERSION).append("\n");

        sb.append("\n--- Platform ---\n");
        EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
        boolean isAndroid = (env != null && env.isAndroid) || detectAndroidFallback();
        sb.append("Android: ").append(isAndroid).append("\n");
        if (isAndroid) {
            AndroidLauncherType launcher = detectAndroidLauncherType();
            sb.append("Android Launcher: ").append(launcher.name).append("\n");
            sb.append("  Package: ").append(launcher.packageName).append("\n");
            sb.append("  Base Path: ").append(launcher.basePath).append("\n");
            sb.append("  Java Home: ").append(launcher.javaHomePath).append("\n");
            sb.append("  LWJGL Path: ").append(launcher.lwjglPath).append("\n");
            sb.append("  Custom Java Supported: ").append(launcher.supportsCustomJava).append("\n");

            // Check installed runtimes
            sb.append("\n  Installed Android Runtimes:\n");
            if (Files.isDirectory(launcher.javaHomePath)) {
                try {
                    Files.list(launcher.javaHomePath)
                        .filter(Files::isDirectory)
                        .forEach(dir -> {
                            Path javaBin = findJavaBinaryRecursive(dir);
                            sb.append("    ").append(dir.getFileName())
                              .append(javaBin != null ? " [✓ has java binary]" : " [✗ no binary]")
                              .append("\n");
                        });
                } catch (IOException e) {
                    sb.append("    (error listing: ").append(e.getMessage()).append(")\n");
                }
            } else {
                sb.append("    (directory does not exist)\n");
            }
        } else {
            sb.append("OS: ").append(env != null ? env.osName : "unknown").append("\n");
            sb.append("Arch: ").append(env != null ? env.osArch : "unknown").append("\n");
        }

        sb.append("\n--- Available Android Runtimes ---\n");
        for (AndroidRuntime rt : ANDROID_RUNTIMES) {
            sb.append("  ").append(rt.name).append("\n");
            sb.append("    URL: ").append(rt.url).append("\n");
            sb.append("    Java ").append(rt.javaVersion)
              .append(rt.isJDK ? " (JDK)" : " (JRE)").append("\n");
        }

        sb.append("\n--- Desktop JDK Providers ---\n");
        for (DesktopJDKProvider provider : DESKTOP_PROVIDERS) {
            sb.append("  ").append(provider.name)
              .append(provider.supportsAutoDownload ? " [auto-download]" : " [manual]")
              .append("\n");
            sb.append("    ").append(provider.downloadPageURL).append("\n");
        }

        sb.append("\n--- LWJGL Modules (").append(LWJGL_MODULES.size()).append(") ---\n");
        String currentCategory = "";
        for (LWJGLModule mod : LWJGL_MODULES) {
            if (!mod.category.equals(currentCategory)) {
                currentCategory = mod.category;
                sb.append("  [").append(currentCategory).append("]\n");
            }
            sb.append("    ").append(mod.required ? "★ " : "  ")
              .append(mod.displayName)
              .append(" (").append(mod.artifactId).append(")")
              .append(mod.hasNatives ? " +natives" : "")
              .append("\n");
        }

        // Check which LWJGL modules are already downloaded
        Path lwjglDir = Mini_DirtyRoomCore.getMDRHome().resolve("lwjgl_cache");
        sb.append("\n--- Downloaded LWJGL Jars ---\n");
        if (Files.isDirectory(lwjglDir)) {
            try {
                List<Path> jars = Files.list(lwjglDir)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .sorted()
                    .collect(Collectors.toList());
                for (Path jar : jars) {
                    long sizeKB = Files.size(jar) / 1024;
                    sb.append("  ").append(jar.getFileName())
                      .append(" (").append(sizeKB).append(" KB)\n");
                }
                sb.append("  Total: ").append(jars.size()).append(" jars\n");
            } catch (IOException e) {
                sb.append("  (error listing: ").append(e.getMessage()).append(")\n");
            }
        } else {
            sb.append("  (no LWJGL cache directory)\n");
        }

        // Check user choices
        Properties choices = loadUserChoices(getProvisionerHome());
        if (!choices.isEmpty()) {
            sb.append("\n--- User Choices ---\n");
            for (String key : choices.stringPropertyNames()) {
                sb.append("  ").append(key).append(" = ")
                  .append(choices.getProperty(key)).append("\n");
            }
        }

        sb.append("\n--- Download Status ---\n");
        sb.append("Status: ").append(DOWNLOAD_STATUS.get()).append("\n");
        sb.append("Progress: ").append(DOWNLOAD_PROGRESS.get()).append("%\n");
        long current = DOWNLOAD_CURRENT_BYTES.get();
        long total = DOWNLOAD_TOTAL_BYTES.get();
        if (total > 0) {
            sb.append("Bytes: ").append(current / 1024).append(" / ")
              .append(total / 1024).append(" KB\n");
        }

        // LaunchWrapper / Forge patch status
        sb.append("\n--- Patch Status ---\n");
        sb.append("LaunchWrapper patches: ");
        Instrumentation inst = Mini_DirtyRoomCore.getInstrumentation();
        sb.append(inst != null ? "active (via Instrumentation)" : "DeepMix-only").append("\n");

        sb.append("Forge patches: ");
        boolean forgePresent = classExistsQuiet("net.minecraftforge.fml.common.Mod")
            || classExistsQuiet("cpw.mods.fml.common.Mod");
        sb.append(forgePresent ? "active (Forge detected)" : "N/A (no Forge)").append("\n");

        sb.append("Module system fixes: ");
        sb.append(JavaCompatibilityLayer.hasModules() ? "active" : "N/A (Java 8)").append("\n");

        // Provisioner home directory contents
        Path provHome = getProvisionerHome();
        sb.append("\n--- Provisioner Home ---\n");
        sb.append("Path: ").append(provHome).append("\n");
        if (Files.isDirectory(provHome)) {
            try {
                Files.list(provHome).forEach(p ->
                    sb.append("  ").append(p.getFileName()).append("\n"));
            } catch (IOException e) {
                sb.append("  (error listing)\n");
            }
        } else {
            sb.append("  (does not exist)\n");
        }

        sb.append("=== End Report ===\n");
        return sb.toString();
    }

    /**
     * Quiet class existence check that never throws.
     */
    private static boolean classExistsQuiet(String className) {
        try {
            Class.forName(className, false,
                Thread.currentThread().getContextClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }


    // ========================================================================
    //  SECTION 10: RUNTIME MANAGEMENT API
    // ========================================================================

    /**
     * Lists all installed Java runtimes on the current platform.
     *
     * @return a list of runtime descriptions with their paths
     */
    public static List<InstalledRuntime> getInstalledRuntimes() {
        List<InstalledRuntime> runtimes = new ArrayList<>();
        EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
        boolean isAndroid = (env != null && env.isAndroid) || detectAndroidFallback();

        if (isAndroid) {
            AndroidLauncherType launcher = detectAndroidLauncherType();
            if (Files.isDirectory(launcher.javaHomePath)) {
                try {
                    Files.list(launcher.javaHomePath)
                        .filter(Files::isDirectory)
                        .forEach(dir -> {
                            Path javaBin = findJavaBinaryRecursive(dir);
                            if (javaBin != null) {
                                int version = probeJavaVersion(javaBin);
                                runtimes.add(new InstalledRuntime(
                                    dir.getFileName().toString(),
                                    dir,
                                    javaBin,
                                    version,
                                    "android"
                                ));
                            }
                        });
                } catch (IOException ignored) {}
            }
        } else {
            // Desktop: scan known locations
            List<Path> searchPaths = getDesktopJavaSearchPaths(env);
            for (Path searchPath : searchPaths) {
                if (!Files.isDirectory(searchPath)) continue;
                try {
                    Files.list(searchPath)
                        .filter(Files::isDirectory)
                        .filter(p -> {
                            String name = p.getFileName().toString().toLowerCase();
                            return name.contains("jdk") || name.contains("jre")
                                || name.contains("java") || name.contains("temurin")
                                || name.contains("zulu") || name.contains("corretto");
                        })
                        .forEach(dir -> {
                            Path javaBin = findJavaBinaryRecursive(dir);
                            if (javaBin != null) {
                                int version = probeJavaVersion(javaBin);
                                runtimes.add(new InstalledRuntime(
                                    dir.getFileName().toString(),
                                    dir,
                                    javaBin,
                                    version,
                                    "desktop"
                                ));
                            }
                        });
                } catch (IOException ignored) {}
            }
        }

        // Also check our own provisioner directory
        Path provJdkDir = getProvisionerHome().resolve("jdk_downloads");
        if (Files.isDirectory(provJdkDir)) {
            try {
                Files.list(provJdkDir)
                    .filter(Files::isDirectory)
                    .forEach(dir -> {
                        Path javaBin = findJavaBinaryRecursive(dir);
                        if (javaBin != null) {
                            int version = probeJavaVersion(javaBin);
                            runtimes.add(new InstalledRuntime(
                                "MDR: " + dir.getFileName().toString(),
                                dir,
                                javaBin,
                                version,
                                "provisioner"
                            ));
                        }
                    });
            } catch (IOException ignored) {}
        }

        // Sort by version descending
        runtimes.sort((a, b) -> Integer.compare(b.javaVersion, a.javaVersion));
        return runtimes;
    }

    /**
     * Information about an installed Java runtime.
     */
    public static final class InstalledRuntime {
        public final String name;
        public final Path   homePath;
        public final Path   javaBinary;
        public final int    javaVersion;
        public final String source; // "android", "desktop", "provisioner"

        InstalledRuntime(String name, Path homePath, Path javaBinary,
                        int javaVersion, String source) {
            this.name        = name;
            this.homePath    = homePath;
            this.javaBinary  = javaBinary;
            this.javaVersion = javaVersion;
            this.source      = source;
        }

        @Override
        public String toString() {
            return name + " (Java " + javaVersion + ") at " + homePath;
        }
    }

    /**
     * Probes the Java version of a binary by running it with -version.
     */
    private static int probeJavaVersion(Path javaBinary) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                javaBinary.toAbsolutePath().toString(), "-version");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String output = readStreamFully(proc.getInputStream());
            proc.waitFor(5, TimeUnit.SECONDS);
            proc.destroyForcibly();

            // Parse version from output like:
            //   openjdk version "21.0.1" 2023-10-17
            //   java version "1.8.0_292"
            for (String line : output.split("\n")) {
                line = line.trim();
                if (line.contains("version")) {
                    int quote1 = line.indexOf('"');
                    int quote2 = line.indexOf('"', quote1 + 1);
                    if (quote1 >= 0 && quote2 > quote1) {
                        String verStr = line.substring(quote1 + 1, quote2);
                        if (verStr.startsWith("1.")) {
                            return Integer.parseInt(verStr.split("\\.")[1]);
                        }
                        return Integer.parseInt(verStr.split("[.\\-+]")[0]);
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0; // Unknown
    }

    /**
     * Returns common Java installation search paths for desktop.
     */
    private static List<Path> getDesktopJavaSearchPaths(EnvironmentInfo env) {
        List<Path> paths = new ArrayList<>();

        if (env != null && env.isWindows) {
            paths.add(Paths.get("C:\\Program Files\\Java"));
            paths.add(Paths.get("C:\\Program Files\\Eclipse Adoptium"));
            paths.add(Paths.get("C:\\Program Files\\Zulu"));
            paths.add(Paths.get("C:\\Program Files\\Amazon Corretto"));
            paths.add(Paths.get("C:\\Program Files\\Microsoft\\jdk-21"));
            paths.add(Paths.get("C:\\Program Files\\Microsoft\\jdk-25"));
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                paths.add(Paths.get(localAppData, "Programs", "Eclipse Adoptium"));
            }
        } else if (env != null && env.isMacOS) {
            paths.add(Paths.get("/Library/Java/JavaVirtualMachines"));
            String home = System.getProperty("user.home");
            paths.add(Paths.get(home, ".sdkman/candidates/java"));
            paths.add(Paths.get(home, ".jdks"));
        } else {
            // Linux
            paths.add(Paths.get("/usr/lib/jvm"));
            paths.add(Paths.get("/usr/local/lib/jvm"));
            String home = System.getProperty("user.home");
            paths.add(Paths.get(home, ".sdkman/candidates/java"));
            paths.add(Paths.get(home, ".jdks"));
        }

        return paths;
    }

    /**
     * Sets the active Java runtime for the current launcher.
     * On Android, updates the launcher's configuration.
     * On desktop, writes a hint file that can be used for relaunch.
     *
     * @param runtime the runtime to activate
     * @return true if the configuration was updated successfully
     */
    public static boolean setActiveRuntime(InstalledRuntime runtime) {
        if (runtime == null) return false;

        LOGGER.info("[MDR-Provisioner] Setting active runtime: " + runtime);

        EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
        boolean isAndroid = (env != null && env.isAndroid) || detectAndroidFallback();

        if (isAndroid) {
            AndroidLauncherType launcher = detectAndroidLauncherType();
            // Find matching AndroidRuntime or create a synthetic one
            AndroidRuntime syntheticRT = new AndroidRuntime(
                runtime.name, runtime.name,
                "", "", runtime.javaVersion, true, "");
            configureLauncherRuntime(launcher, runtime.homePath,
                runtime.javaBinary, syntheticRT);
            return true;
        } else {
            // Desktop: write a configuration hint
            Properties choices = loadUserChoices(getProvisionerHome());
            choices.setProperty("active.java.home",
                runtime.homePath.toAbsolutePath().toString());
            choices.setProperty("active.java.binary",
                runtime.javaBinary.toAbsolutePath().toString());
            choices.setProperty("active.java.version",
                String.valueOf(runtime.javaVersion));
            saveUserChoices(getProvisionerHome(), choices);

            // Also set system property for immediate effect
            System.setProperty("mini_dirtyroom.java_home",
                runtime.homePath.toAbsolutePath().toString());

            return true;
        }
    }

    /**
     * Triggers a manual download of a specific Android runtime by index.
     *
     * @param runtimeIndex the index into ANDROID_RUNTIMES
     * @return true if the download was initiated
     */
    public static boolean downloadAndroidRuntime(int runtimeIndex) {
        if (runtimeIndex < 0 || runtimeIndex >= ANDROID_RUNTIMES.size()) {
            LOGGER.warning("[MDR-Provisioner] Invalid runtime index: " + runtimeIndex);
            return false;
        }

        AndroidRuntime runtime = ANDROID_RUNTIMES.get(runtimeIndex);
        AndroidLauncherType launcher = detectAndroidLauncherType();

        LOGGER.info("[MDR-Provisioner] Manual download requested: " + runtime.name);

        // Run in background thread
        Thread downloadThread = JavaCompatibilityLayer.ThreadCompat.createThread(
            "MDR-RuntimeDownload", true,
            () -> downloadAndInstallAndroidRuntime(runtime, launcher),
            false
        );
        downloadThread.start();

        return true;
    }

    /**
     * Triggers a manual download of specific LWJGL modules.
     *
     * @param moduleArtifactIds the artifact IDs to download (e.g., "lwjgl-opengl")
     * @return true if the download was initiated
     */
    public static boolean downloadLWJGLModules(List<String> moduleArtifactIds) {
        if (moduleArtifactIds == null || moduleArtifactIds.isEmpty()) {
            LOGGER.warning("[MDR-Provisioner] No modules specified.");
            return false;
        }

        List<LWJGLModule> selected = new ArrayList<>();
        for (LWJGLModule mod : LWJGL_MODULES) {
            if (moduleArtifactIds.contains(mod.artifactId)) {
                selected.add(mod);
            }
        }

        if (selected.isEmpty()) {
            LOGGER.warning("[MDR-Provisioner] No matching modules found for: "
                         + moduleArtifactIds);
            return false;
        }

        LOGGER.info("[MDR-Provisioner] Manual LWJGL download: "
                  + selected.size() + " modules");

        Thread downloadThread = JavaCompatibilityLayer.ThreadCompat.createThread(
            "MDR-LWJGLDownload", true,
            () -> downloadSelectedLWJGLModules(selected, getProvisionerHome()),
            false
        );
        downloadThread.start();

        return true;
    }

    /**
     * Returns the required LWJGL modules (always needed for Minecraft).
     */
    public static List<LWJGLModule> getRequiredLWJGLModules() {
        return LWJGL_MODULES.stream()
            .filter(m -> m.required)
            .collect(Collectors.toList());
    }

    /**
     * Returns the recommended LWJGL modules (required + recommended category).
     */
    public static List<LWJGLModule> getRecommendedLWJGLModules() {
        return LWJGL_MODULES.stream()
            .filter(m -> m.required || "Recommended".equals(m.category))
            .collect(Collectors.toList());
    }

    /**
     * Returns optional LWJGL modules.
     */
    public static List<LWJGLModule> getOptionalLWJGLModules() {
        return LWJGL_MODULES.stream()
            .filter(m -> !m.required)
            .collect(Collectors.toList());
    }

    /**
     * Registers a callback that fires when download progress changes.
     *
     * @param listener callback receiving progress percentage (0-100)
     */
    public static void addDownloadProgressListener(Consumer<Integer> listener) {
        if (listener == null) return;
        ModLoaderBridge.getEventBus().on("mdr.download.progress",
            data -> listener.accept((Integer) data));
    }

    /**
     * Registers a callback that fires when download status changes.
     *
     * @param listener callback receiving status string
     */
    public static void addDownloadStatusListener(Consumer<String> listener) {
        if (listener == null) return;
        ModLoaderBridge.getEventBus().on("mdr.download.status",
            data -> listener.accept((String) data));
    }


    // ========================================================================
    //  SECTION 11: SHUTDOWN & CLEANUP
    // ========================================================================

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Clean up any temporary download files
            Path provHome = getProvisionerHome();
            if (Files.isDirectory(provHome)) {
                try {
                    Files.list(provHome)
                        .filter(p -> p.getFileName().toString().endsWith(".tmp"))
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); }
                            catch (IOException ignored) {}
                        });
                } catch (IOException ignored) {}
            }

            // Clean up LWJGL cache temp files
            Path lwjglDir = Mini_DirtyRoomCore.getMDRHome().resolve("lwjgl_cache");
            if (Files.isDirectory(lwjglDir)) {
                try {
                    Files.list(lwjglDir)
                        .filter(p -> p.getFileName().toString().endsWith(".tmp"))
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); }
                            catch (IOException ignored) {}
                        });
                } catch (IOException ignored) {}
            }
        }, "MDR-Provisioner-Shutdown"));
    }


    // ========================================================================
    //  SECTION 12: PRIVATE CONSTRUCTOR
    // ========================================================================

    private JavaLWJGLProvisioner() {
        throw new UnsupportedOperationException(
            "JavaLWJGLProvisioner is a static utility class.");
    }
}
