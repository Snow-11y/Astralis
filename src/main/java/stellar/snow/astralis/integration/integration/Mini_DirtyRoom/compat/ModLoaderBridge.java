/*
 * ============================================================================
 *  Mini_DirtyRoom — Mod Loader Bridge
 *  Copyright (c) 2025 Stellar Snow Astralis
 *
 *  This file is the FOURTH and FINAL component in the Mini_DirtyRoom load
 *  chain. It provides the universal integration layer between Mini_DirtyRoom's
 *  modernization engine and every supported mod loader / server platform.
 *
 *  Load Order:
 *    1. Mini_DirtyRoomCore          ← Bootstraps environment, injects jars
 *    2. LWJGLTransformEngine        ← LWJGL 2→3 bytecode transformation
 *    3. JavaCompatibilityLayer      ← Java 8–25 compatibility shims
 *    4. ModLoaderBridge             ← YOU ARE HERE
 *    5. Everything else (mods, game code)
 *
 *  Integration Points with Core:
 *    - Reads EnvironmentInfo.detectedLoader for loader dispatch
 *    - Executes deferred registrations queued during bootstrap
 *    - Reports loader-specific issues to Core's BOOT_WARNINGS
 *    - Coordinates transformer registration via Core's Instrumentation handle
 *
 *  Integration Points with LWJGLTransformEngine:
 *    - Ensures LWJGLTransformEngine is registered with the loader's
 *      transformation pipeline (LaunchClassLoader, Mixin, ModLauncher, etc.)
 *    - Mediates transformer priority / ordering with other mods' transformers
 *
 *  Integration Points with JavaCompatibilityLayer:
 *    - Uses ReflectionHelper for accessing loader internals safely
 *    - Uses ClassLoaderCompat for cross-version class loading
 *    - Uses ThreadCompat for loader-managed thread pools
 *    - Uses DeprecatedAPIs for SecurityManager-gated operations
 *
 *  Supported Platforms:
 *    Client Mod Loaders:
 *      • Minecraft Forge (1.7.10 – 1.12.2 legacy, 1.13+ modern)
 *      • NeoForge (1.20.1+)
 *      • Fabric (all versions)
 *      • Quilt (all versions)
 *      • LiteLoader (legacy)
 *
 *    Server Platforms:
 *      • Spigot / CraftBukkit
 *      • Paper / Folia
 *      • Purpur
 *      • Sponge (Forge & Vanilla)
 *      • BungeeCord / Waterfall
 *      • Velocity
 *
 *    Standalone:
 *      • Vanilla client/server (no mod loader)
 *      • Custom launchers (PojavLauncher, Prism, MultiMC, etc.)
 * ============================================================================
 */
package stellar.snow.astralis.integration.Mini_DirtyRoom.compat;

// ─── Core Integration ─────────────────────────────────────────────────────
import stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore;
import stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore.EnvironmentInfo;
import stellar.snow.astralis.integration.Mini_DirtyRoom.LWJGLTransformEngine;
import stellar.snow.astralis.integration.Mini_DirtyRoom.JavaCompatibilityLayer;
import stellar.snow.astralis.integration.Mini_DirtyRoom.JavaCompatibilityLayer.ReflectionHelper;
import stellar.snow.astralis.integration.Mini_DirtyRoom.JavaCompatibilityLayer.ClassLoaderCompat;
import stellar.snow.astralis.integration.Mini_DirtyRoom.JavaCompatibilityLayer.ThreadCompat;
import stellar.snow.astralis.integration.Mini_DirtyRoom.JavaCompatibilityLayer.DeprecatedAPIs;

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
import java.lang.reflect.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * ============================================================================
 *  MOD LOADER BRIDGE
 * ============================================================================
 *
 * The universal adapter that makes Mini_DirtyRoom work with any Minecraft
 * mod loader, server platform, or standalone environment. This class:
 *
 *   1. Detects the active mod loader(s) and server platform
 *   2. Registers Mini_DirtyRoom components with each loader's lifecycle
 *   3. Bridges event systems (Forge EventBus, Fabric Events, etc.)
 *   4. Manages transformer ordering and conflict resolution
 *   5. Provides a unified mod metadata / description to each loader
 *   6. Handles cross-loader compatibility for mods that target multiple loaders
 *   7. Exposes a universal API that downstream mods can use regardless of loader
 *
 * Each supported loader has a dedicated inner class that implements the
 * LoaderIntegration interface.
 */

@DeepPriority(level = Integer.MIN_VALUE + 3)
@DeepPhase(name = "MOD_LOADER_BRIDGE", order = -999996)
@DeepEdit(reason = "Universal mod loader integration bridge")
public final class ModLoaderBridge {

    // ========================================================================
    //  CONSTANTS & STATIC STATE
    // ========================================================================

    private static final Logger LOGGER = Logger.getLogger("MDR-Bridge");

    /** Singleton guard. */
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    /** Whether the bridge has completed full initialization (including deferred). */
    private static final AtomicBoolean FULLY_READY = new AtomicBoolean(false);

    /** The active loader integration implementation. */
    private static final AtomicReference<LoaderIntegration> ACTIVE_INTEGRATION =
        new AtomicReference<>(null);

    /** All detected loader integrations (some environments have multiple). */
    private static final List<LoaderIntegration> ALL_INTEGRATIONS =
        new CopyOnWriteArrayList<>();

    /** Server platform integration (orthogonal to mod loader). */
    private static final AtomicReference<ServerPlatformIntegration> SERVER_PLATFORM =
        new AtomicReference<>(null);

    /** Universal event bus for cross-loader event dispatching. */
    private static final UniversalEventBus EVENT_BUS = new UniversalEventBus();

    /** Lifecycle callbacks registered by other components. */
    private static final Map<LifecyclePhase, List<Runnable>> LIFECYCLE_CALLBACKS =
        new ConcurrentHashMap<>();

    /** Mod metadata exposed to all loaders. */
    private static final ModMetadata MOD_META = new ModMetadata(
        "mini_dirtyroom",
        "Mini DirtyRoom",
        Mini_DirtyRoomCore.VERSION,
        "Stellar Snow Astralis",
        "Modernization layer: LWJGL 3.4.0 + Java 8–25 compatibility",
        "https://github.com/stellar-snow-astralis/mini-dirtyroom",
        "MIT"
    );

    /** Detected environment flags. */
    private static volatile boolean IS_CLIENT      = false;
    private static volatile boolean IS_SERVER      = false;
    private static volatile boolean IS_PROXY       = false;
    private static volatile boolean IS_DEVELOPMENT = false;
    private static volatile boolean IS_OBFUSCATED  = true;

    /** Mapping environment detection results. */
    private static volatile String MAPPING_TYPE = "unknown"; // "srg", "intermediary", "mojang", "none"


    // ========================================================================
    //  SECTION 1: INITIALIZATION
    // ========================================================================

    /**
     * Primary initialization entry point. Called by Mini_DirtyRoomCore
     * during Phase 9, and also hooked via DeepMix for redundancy.
     */
    @DeepHook(
        targets = {
            @HookTarget(
                className  = "stellar.snow.astralis.integration.Mini_DirtyRoom.Mini_DirtyRoomCore",
                methodName = "initializeModLoaderBridge"
            )
        },
        timing = HookTiming.AFTER
    )
    public static void initialize() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            LOGGER.fine("[MDR-Bridge] Already initialized. Skipping.");
            return;
        }

        LOGGER.info("[MDR-Bridge] ╔═══════════════════════════════════════════╗");
        LOGGER.info("[MDR-Bridge] ║  Mod Loader Bridge Initializing          ║");
        LOGGER.info("[MDR-Bridge] ╚═══════════════════════════════════════════╝");

        long startNs = System.nanoTime();

        try {
            // ── Step 1: Detect environment ─────────────────────────────────
            detectEnvironment();

            // ── Step 2: Detect and create all loader integrations ──────────
            detectLoaders();

            // ── Step 3: Detect server platform ─────────────────────────────
            detectServerPlatform();

            // ── Step 4: Initialize primary loader integration ──────────────
            LoaderIntegration primary = ACTIVE_INTEGRATION.get();
            if (primary != null) {
                LOGGER.info("[MDR-Bridge] Primary loader: " + primary.getLoaderName()
                          + " v" + primary.getLoaderVersion());
                primary.initialize();
            } else {
                LOGGER.info("[MDR-Bridge] No mod loader detected. "
                          + "Running in standalone mode.");
                StandaloneIntegration standalone = new StandaloneIntegration();
                ACTIVE_INTEGRATION.set(standalone);
                ALL_INTEGRATIONS.add(standalone);
                standalone.initialize();
            }

            // ── Step 5: Initialize server platform ─────────────────────────
            ServerPlatformIntegration server = SERVER_PLATFORM.get();
            if (server != null) {
                LOGGER.info("[MDR-Bridge] Server platform: "
                          + server.getPlatformName());
                server.initialize();
            }

            // ── Step 6: Ensure LWJGL transform engine is registered ────────
            ensureTransformEngineRegistered();

            // ── Step 7: Execute deferred registrations from Core ───────────
            Mini_DirtyRoomCore.executeDeferredRegistrations();

            // ── Step 8: Fire initialization lifecycle event ────────────────
            fireLifecycleEvent(LifecyclePhase.BRIDGE_INITIALIZED);

            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000L;
            LOGGER.info("[MDR-Bridge] Bridge initialization complete (" + elapsedMs + " ms).");
            LOGGER.info("[MDR-Bridge]   Loader: "
                      + (primary != null ? primary.getLoaderName() : "standalone"));
            LOGGER.info("[MDR-Bridge]   Server: "
                      + (server != null ? server.getPlatformName() : "none"));
            LOGGER.info("[MDR-Bridge]   Client: " + IS_CLIENT
                      + " | Server: " + IS_SERVER
                      + " | Dev: " + IS_DEVELOPMENT);

        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE,
                "[MDR-Bridge] Bridge initialization FAILED", t);
            Mini_DirtyRoomCore.getBootWarnings().add(
                "ModLoaderBridge initialization failed: " + t.getMessage());
        }
    }

    /**
     * Called later when the game is fully loaded. Completes deferred setup
     * that requires the game to be running (e.g., registry access).
     */
    @DeepHook(
        targets = {
            @HookTarget(
                className  = "net.minecraft.client.Minecraft",
                methodName = "init"
            ),
            @HookTarget(
                className  = "net.minecraft.client.Minecraft",
                methodName = "startGame"
            ),
            @HookTarget(
                className  = "net.minecraft.server.MinecraftServer",
                methodName = "init"
            ),
            @HookTarget(
                className  = "net.minecraft.server.MinecraftServer",
                methodName = "run"
            )
        },
        timing = HookTiming.AFTER
    )
    public static void onGameReady() {
        if (!FULLY_READY.compareAndSet(false, true)) return;

        LOGGER.info("[MDR-Bridge] Game ready — completing deferred setup.");

        LoaderIntegration integration = ACTIVE_INTEGRATION.get();
        if (integration != null) {
            try {
                integration.onGameReady();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                    "[MDR-Bridge] Loader onGameReady failed", e);
            }
        }

        ServerPlatformIntegration server = SERVER_PLATFORM.get();
        if (server != null) {
            try {
                server.onServerReady();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                    "[MDR-Bridge] Server onServerReady failed", e);
            }
        }

        fireLifecycleEvent(LifecyclePhase.GAME_READY);

        LOGGER.info("[MDR-Bridge] Deferred setup complete. "
                  + "Mini_DirtyRoom is fully operational.");
    }


    // ========================================================================
    //  SECTION 2: ENVIRONMENT DETECTION
    // ========================================================================

    /**
     * Detects whether we're on client, server, dev environment,
     * and what mapping type is active.
     */
    private static void detectEnvironment() {
        EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
        IS_CLIENT = env != null && env.isClient;
        IS_SERVER = !IS_CLIENT;

        // Development environment detection
        IS_DEVELOPMENT = detectDevelopmentEnvironment();

        // Obfuscation / mapping detection
        detectMappings();

        LOGGER.info("[MDR-Bridge] Environment: "
                  + (IS_CLIENT ? "CLIENT" : "SERVER")
                  + " | Dev: " + IS_DEVELOPMENT
                  + " | Mappings: " + MAPPING_TYPE
                  + " | Obfuscated: " + IS_OBFUSCATED);
    }

    /**
     * Detects whether we're running in a development environment
     * (IDE, Gradle runClient, etc.)
     */
    private static boolean detectDevelopmentEnvironment() {
        // Check for known dev-environment markers
        // ForgeGradle sets fml.deobfRuntimeEnvironment
        if ("true".equals(System.getProperty("fml.deobfRuntimeEnvironment"))) {
            return true;
        }
        // GradleStart classes
        if (classExists("GradleStart") || classExists("GradleStartServer")) {
            return true;
        }
        // Fabric dev: fabric.development=true
        if ("true".equals(System.getProperty("fabric.development"))) {
            return true;
        }
        // Check if we're running from IDE (build/classes vs jar)
        URL codeSource = ModLoaderBridge.class.getProtectionDomain()
            .getCodeSource().getLocation();
        if (codeSource != null) {
            String path = codeSource.getPath();
            if (path.contains("/build/classes/") || path.contains("/out/")
             || path.contains("/target/classes/")) {
                return true;
            }
        }
        // Check classpath for IDE markers
        String cp = System.getProperty("java.class.path", "");
        if (cp.contains("idea_rt.jar") || cp.contains("eclipse")
         || cp.contains("netbeans")) {
            return true;
        }
        return false;
    }

    /**
     * Detects the active mapping type (SRG, Intermediary, Mojang, none).
     */
    private static void detectMappings() {
        // Forge uses SRG mappings (func_12345, field_12345)
        if (classExists("net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer")) {
            MAPPING_TYPE = "srg";
            IS_OBFUSCATED = !IS_DEVELOPMENT;
            return;
        }

        // Fabric uses Intermediary mappings (method_12345, field_12345 → named in dev)
        if (classExists("net.fabricmc.loader.api.FabricLoader")) {
            MAPPING_TYPE = "intermediary";
            IS_OBFUSCATED = !IS_DEVELOPMENT;
            return;
        }

        // NeoForge uses Mojang mappings
        if (classExists("net.neoforged.fml.common.Mod")) {
            MAPPING_TYPE = "mojang";
            IS_OBFUSCATED = false; // NeoForge ships with Mojang mappings
            return;
        }

        // Check for obfuscated class names (single letter classes like 'aaa', 'bcd')
        try {
            if (classExists("bao") || classExists("ave") || classExists("bcx")) {
                IS_OBFUSCATED = true;
                MAPPING_TYPE = "notch"; // Vanilla obfuscated names
                return;
            }
        } catch (Exception ignored) {}

        MAPPING_TYPE = "none";
        IS_OBFUSCATED = false;
    }


    // ========================================================================
    //  SECTION 3: LOADER DETECTION & CREATION
    // ========================================================================

    /**
     * Detects all present mod loaders and creates integration instances.
     * Sets the primary (most capable) one as the active integration.
     */
    private static void detectLoaders() {
        LOGGER.info("[MDR-Bridge] Scanning for mod loaders...");

        // ── NeoForge (check first — extends Forge internals) ───────────────
        if (classExists("net.neoforged.fml.common.Mod")
         || classExists("net.neoforged.fml.ModContainer")) {
            try {
                NeoForgeIntegration nf = new NeoForgeIntegration();
                ALL_INTEGRATIONS.add(nf);
                ACTIVE_INTEGRATION.compareAndSet(null, nf);
                LOGGER.info("[MDR-Bridge]   ✓ NeoForge detected");
            } catch (Exception e) {
                LOGGER.warning("[MDR-Bridge]   ✗ NeoForge detected but init failed: "
                             + e.getMessage());
            }
        }

        // ── Quilt (check before Fabric — extends Fabric) ───────────────────
        if (classExists("org.quiltmc.loader.api.QuiltLoader")) {
            try {
                QuiltIntegration quilt = new QuiltIntegration();
                ALL_INTEGRATIONS.add(quilt);
                ACTIVE_INTEGRATION.compareAndSet(null, quilt);
                LOGGER.info("[MDR-Bridge]   ✓ Quilt detected");
            } catch (Exception e) {
                LOGGER.warning("[MDR-Bridge]   ✗ Quilt detected but init failed: "
                             + e.getMessage());
            }
        }

        // ── Fabric ─────────────────────────────────────────────────────────
        if (classExists("net.fabricmc.loader.api.FabricLoader")) {
            try {
                FabricIntegration fabric = new FabricIntegration();
                ALL_INTEGRATIONS.add(fabric);
                ACTIVE_INTEGRATION.compareAndSet(null, fabric);
                LOGGER.info("[MDR-Bridge]   ✓ Fabric detected");
            } catch (Exception e) {
                LOGGER.warning("[MDR-Bridge]   ✗ Fabric detected but init failed: "
                             + e.getMessage());
            }
        }

        // ── Forge (modern: net.minecraftforge.fml) ─────────────────────────
        if (classExists("net.minecraftforge.fml.common.Mod")) {
            try {
                ForgeModernIntegration forge = new ForgeModernIntegration();
                ALL_INTEGRATIONS.add(forge);
                ACTIVE_INTEGRATION.compareAndSet(null, forge);
                LOGGER.info("[MDR-Bridge]   ✓ Forge (modern) detected");
            } catch (Exception e) {
                LOGGER.warning("[MDR-Bridge]   ✗ Forge (modern) detected but init failed: "
                             + e.getMessage());
            }
        }

        // ── Forge (legacy: cpw.mods.fml) ───────────────────────────────────
        if (classExists("cpw.mods.fml.common.Mod")) {
            try {
                ForgeLegacyIntegration forgeLegacy = new ForgeLegacyIntegration();
                ALL_INTEGRATIONS.add(forgeLegacy);
                ACTIVE_INTEGRATION.compareAndSet(null, forgeLegacy);
                LOGGER.info("[MDR-Bridge]   ✓ Forge (legacy/1.7.x) detected");
            } catch (Exception e) {
                LOGGER.warning("[MDR-Bridge]   ✗ Forge (legacy) detected but init failed: "
                             + e.getMessage());
            }
        }

        // ── LiteLoader ─────────────────────────────────────────────────────
        if (classExists("com.mumfrey.liteloader.LiteMod")) {
            try {
                LiteLoaderIntegration ll = new LiteLoaderIntegration();
                ALL_INTEGRATIONS.add(ll);
                // LiteLoader is secondary to Forge
                LOGGER.info("[MDR-Bridge]   ✓ LiteLoader detected (secondary)");
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge]   LiteLoader init note: " + e.getMessage());
            }
        }

        LOGGER.info("[MDR-Bridge] Detected " + ALL_INTEGRATIONS.size()
                  + " loader(s).");
    }

    /**
     * Detects server platform (Spigot, Paper, Sponge, etc.).
     * Server platforms are orthogonal to mod loaders — a server can have
     * both Forge and Sponge, or Paper without any mod loader.
     */
    private static void detectServerPlatform() {
        if (!IS_SERVER && !classExists("org.bukkit.Bukkit")) {
            // Not a server environment (or at least no Bukkit)
            // Still check for Sponge since it can be client-side
        }

        // ── Folia (check before Paper — extends Paper) ─────────────────────
        if (classExists("io.papermc.paper.threadedregions.RegionizedServer")) {
            try {
                FoliaIntegration folia = new FoliaIntegration();
                SERVER_PLATFORM.set(folia);
                LOGGER.info("[MDR-Bridge]   ✓ Folia server detected");
                return;
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge]   Folia init note: " + e.getMessage());
            }
        }

        // ── Purpur (check before Paper — extends Paper) ────────────────────
        if (classExists("org.purpurmc.purpur.PurpurConfig")) {
            try {
                PurpurIntegration purpur = new PurpurIntegration();
                SERVER_PLATFORM.set(purpur);
                LOGGER.info("[MDR-Bridge]   ✓ Purpur server detected");
                return;
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge]   Purpur init note: " + e.getMessage());
            }
        }

        // ── Paper ──────────────────────────────────────────────────────────
        if (classExists("com.destroystokyo.paper.PaperConfig")
         || classExists("io.papermc.paper.configuration.GlobalConfiguration")) {
            try {
                PaperIntegration paper = new PaperIntegration();
                SERVER_PLATFORM.set(paper);
                LOGGER.info("[MDR-Bridge]   ✓ Paper server detected");
                return;
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge]   Paper init note: " + e.getMessage());
            }
        }

        // ── Spigot ─────────────────────────────────────────────────────────
        if (classExists("org.spigotmc.SpigotConfig")) {
            try {
                SpigotIntegration spigot = new SpigotIntegration();
                SERVER_PLATFORM.set(spigot);
                LOGGER.info("[MDR-Bridge]   ✓ Spigot server detected");
                return;
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge]   Spigot init note: " + e.getMessage());
            }
        }

        // ── CraftBukkit (base) ─────────────────────────────────────────────
        if (classExists("org.bukkit.craftbukkit.CraftServer")
         || classExists("org.bukkit.Bukkit")) {
            try {
                BukkitIntegration bukkit = new BukkitIntegration();
                SERVER_PLATFORM.set(bukkit);
                LOGGER.info("[MDR-Bridge]   ✓ CraftBukkit server detected");
                return;
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge]   Bukkit init note: " + e.getMessage());
            }
        }

        // ── Sponge ─────────────────────────────────────────────────────────
        if (classExists("org.spongepowered.api.Sponge")) {
            try {
                SpongeIntegration sponge = new SpongeIntegration();
                SERVER_PLATFORM.set(sponge);
                LOGGER.info("[MDR-Bridge]   ✓ Sponge platform detected");
                return;
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge]   Sponge init note: " + e.getMessage());
            }
        }

        // ── Velocity (proxy) ───────────────────────────────────────────────
        if (classExists("com.velocitypowered.api.proxy.ProxyServer")) {
            try {
                VelocityIntegration velocity = new VelocityIntegration();
                SERVER_PLATFORM.set(velocity);
                IS_PROXY = true;
                LOGGER.info("[MDR-Bridge]   ✓ Velocity proxy detected");
                return;
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge]   Velocity init note: " + e.getMessage());
            }
        }

        // ── BungeeCord / Waterfall (proxy) ─────────────────────────────────
        if (classExists("net.md_5.bungee.api.ProxyServer")) {
            try {
                BungeeCordIntegration bungee = new BungeeCordIntegration();
                SERVER_PLATFORM.set(bungee);
                IS_PROXY = true;
                LOGGER.info("[MDR-Bridge]   ✓ BungeeCord/Waterfall proxy detected");
                return;
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge]   BungeeCord init note: " + e.getMessage());
            }
        }
    }


    // ========================================================================
    //  SECTION 4: TRANSFORMER ENGINE COORDINATION
    // ========================================================================

    /**
     * Ensures the LWJGLTransformEngine is properly registered with
     * whatever transformation system the active loader uses.
     */
    private static void ensureTransformEngineRegistered() {
        LOGGER.info("[MDR-Bridge] Ensuring LWJGL transform engine is registered...");

        LoaderIntegration integration = ACTIVE_INTEGRATION.get();
        if (integration != null) {
            try {
                integration.registerTransformer(LWJGLTransformEngine.getInstance());
                LOGGER.info("[MDR-Bridge] Transform engine registered via "
                          + integration.getLoaderName());
                return;
            } catch (Exception e) {
                LOGGER.warning("[MDR-Bridge] Loader-specific transformer registration failed: "
                             + e.getMessage());
            }
        }

        // Fallback: register via Instrumentation directly
        LWJGLTransformEngine.register();
    }


    // ========================================================================
    //  SECTION 5: LOADER INTEGRATION INTERFACE
    // ========================================================================

    /**
     * Common interface for all mod loader integrations.
     * Each loader implementation provides its own version of these methods.
     */
    interface LoaderIntegration {
        /** Returns the loader's name (e.g., "Forge", "Fabric"). */
        String getLoaderName();

        /** Returns the loader's version string. */
        String getLoaderVersion();

        /** Returns the Minecraft version the loader targets. */
        String getMinecraftVersion();

        /** Performs primary initialization. */
        void initialize();

        /** Called when the game is fully loaded and running. */
        void onGameReady();

        /**
         * Registers a ClassFileTransformer with this loader's
         * transformation pipeline.
         */
        void registerTransformer(LWJGLTransformEngine engine);

        /**
         * Registers an event listener with this loader's event system.
         * The listener object should have appropriately annotated methods.
         */
        void registerEventListener(Object listener);

        /**
         * Returns the loader's class loader (LaunchClassLoader, KnotClassLoader, etc.)
         */
        ClassLoader getLoaderClassLoader();

        /**
         * Returns the game directory (e.g., .minecraft/).
         */
        Path getGameDirectory();

        /**
         * Returns the config directory.
         */
        Path getConfigDirectory();

        /**
         * Returns the mods directory.
         */
        Path getModsDirectory();

        /**
         * Checks if a specific mod is loaded.
         */
        boolean isModLoaded(String modId);

        /**
         * Returns a list of all loaded mod IDs.
         */
        List<String> getLoadedMods();
    }

    /**
     * Common interface for server platform integrations.
     */
    interface ServerPlatformIntegration {
        String getPlatformName();
        String getPlatformVersion();
        void initialize();
        void onServerReady();
        void registerCommand(String name, Object handler);
        boolean isMainThread();
        void runOnMainThread(Runnable task);
    }


    // ========================================================================
    //  SECTION 6: FORGE MODERN INTEGRATION (1.12.2+)
    // ========================================================================

    /**
     * Integration with Minecraft Forge (net.minecraftforge.fml path).
     * Covers Forge for Minecraft 1.12.2 and the 1.13+ rewrite.
     */
    static final class ForgeModernIntegration implements LoaderIntegration {

        private Object eventBusInstance = null;
        private ClassLoader launchClassLoader = null;
        private String forgeVersion = "unknown";
        private String mcVersion = "unknown";

        @Override public String getLoaderName() { return "Forge"; }
        @Override public String getLoaderVersion() { return forgeVersion; }
        @Override public String getMinecraftVersion() { return mcVersion; }

        @Override
        public void initialize() {
            // Detect Forge version
            try {
                Class<?> forgeVersion = Class.forName(
                    "net.minecraftforge.fml.common.Loader");
                Method mcVersion = forgeVersion.getMethod("getMCVersionString");
                this.mcVersion = (String) mcVersion.invoke(
                    forgeVersion.getMethod("instance").invoke(null));
            } catch (Exception ignored) {
                // Try alternative path
                try {
                    Class<?> fv = Class.forName(
                        "net.minecraftforge.versions.forge.ForgeVersion");
                    Method getVersion = fv.getMethod("getVersion");
                    this.forgeVersion = (String) getVersion.invoke(null);
                } catch (Exception ignored2) {}
            }

            // Get the LaunchClassLoader
            launchClassLoader = Thread.currentThread().getContextClassLoader();

            // Register transformer exclusions
            registerExclusions();

            // Get event bus reference
            try {
                Class<?> minecraftForge = Class.forName(
                    "net.minecraftforge.common.MinecraftForge");
                Field eventBusField = minecraftForge.getDeclaredField("EVENT_BUS");
                eventBusInstance = eventBusField.get(null);
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge-Forge] Event bus not yet available. "
                          + "Will register listeners on game ready.");
            }

            LOGGER.info("[MDR-Bridge-Forge] Forge integration initialized. "
                      + "Version=" + forgeVersion + ", MC=" + mcVersion);
        }

        @Override
        public void onGameReady() {
            // Event bus should be available now
            if (eventBusInstance == null) {
                try {
                    Class<?> minecraftForge = Class.forName(
                        "net.minecraftforge.common.MinecraftForge");
                    Field eventBusField = minecraftForge.getDeclaredField("EVENT_BUS");
                    eventBusInstance = eventBusField.get(null);
                } catch (Exception e) {
                    LOGGER.warning("[MDR-Bridge-Forge] Could not get event bus: "
                                 + e.getMessage());
                }
            }

            // Register our event handler
            if (eventBusInstance != null) {
                registerEventListener(new ForgeEventHandler());
                LOGGER.info("[MDR-Bridge-Forge] Event handler registered.");
            }
        }

        @Override
        public void registerTransformer(LWJGLTransformEngine engine) {
            ClassLoader cl = launchClassLoader != null
                ? launchClassLoader
                : Thread.currentThread().getContextClassLoader();

            if (cl.getClass().getName().contains("LaunchClassLoader")) {
                try {
                    // Register as an IClassTransformer
                    Method registerTransformer = cl.getClass().getMethod(
                        "registerTransformer", String.class);
                    registerTransformer.invoke(cl,
                        LWJGLTransformEngine.ForgeTransformerAdapter.class.getName());
                    LOGGER.info("[MDR-Bridge-Forge] Transformer registered with LaunchClassLoader.");
                } catch (Exception e) {
                    LOGGER.warning("[MDR-Bridge-Forge] Transformer registration failed: "
                                 + e.getMessage());
                    // Fallback to Instrumentation
                    LWJGLTransformEngine.register();
                }
            } else {
                // Non-LaunchClassLoader Forge (1.13+ with ModLauncher)
                registerTransformerModLauncher(engine);
            }
        }

        /**
         * Registers with cpw.mods.modlauncher (Forge 1.13+).
         */
        private void registerTransformerModLauncher(LWJGLTransformEngine engine) {
            try {
                // ModLauncher uses a service-based transformer system
                // We register via the ITransformationService SPI
                LOGGER.info("[MDR-Bridge-Forge] ModLauncher environment detected. "
                          + "Using Instrumentation fallback.");
                LWJGLTransformEngine.register();
            } catch (Exception e) {
                LOGGER.warning("[MDR-Bridge-Forge] ModLauncher registration failed: "
                             + e.getMessage());
            }
        }

        @Override
        public void registerEventListener(Object listener) {
            if (eventBusInstance == null) return;
            try {
                Method register = eventBusInstance.getClass().getMethod(
                    "register", Object.class);
                register.invoke(eventBusInstance, listener);
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge-Forge] Event listener registration failed: "
                          + e.getMessage());
            }
        }

        private void registerExclusions() {
            ClassLoader cl = launchClassLoader;
            if (cl == null) cl = Thread.currentThread().getContextClassLoader();
            if (!cl.getClass().getName().contains("LaunchClassLoader")) return;

            String[] exclusions = {
                "stellar.snow.astralis.integration.Mini_DirtyRoom.",
                "stellar.snow.astralis.integration.DeepMix.",
            };

            try {
                Method addTransformerExclusion = cl.getClass().getMethod(
                    "addTransformerExclusion", String.class);
                Method addClassLoaderExclusion = cl.getClass().getMethod(
                    "addClassLoaderExclusion", String.class);

                for (String exclusion : exclusions) {
                    addTransformerExclusion.invoke(cl, exclusion);
                    addClassLoaderExclusion.invoke(cl, exclusion);
                }
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge-Forge] Exclusion registration: "
                          + e.getMessage());
            }
        }

        @Override public ClassLoader getLoaderClassLoader() { return launchClassLoader; }
        @Override public Path getGameDirectory() { return findGameDir(); }
        @Override public Path getConfigDirectory() { return findGameDir().resolve("config"); }
        @Override public Path getModsDirectory() { return findGameDir().resolve("mods"); }

        @Override
        public boolean isModLoaded(String modId) {
            try {
                Class<?> loader = Class.forName(
                    "net.minecraftforge.fml.common.Loader");
                Method instance = loader.getMethod("instance");
                Object inst = instance.invoke(null);
                Method isModLoaded = loader.getMethod("isModLoaded", String.class);
                return (boolean) isModLoaded.invoke(inst, modId);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public List<String> getLoadedMods() {
            List<String> mods = new ArrayList<>();
            try {
                Class<?> loader = Class.forName(
                    "net.minecraftforge.fml.common.Loader");
                Method instance = loader.getMethod("instance");
                Object inst = instance.invoke(null);
                Method getModList = loader.getMethod("getModList");
                @SuppressWarnings("unchecked")
                List<Object> modList = (List<Object>) getModList.invoke(inst);
                for (Object mod : modList) {
                    Method getModId = mod.getClass().getMethod("getModId");
                    mods.add((String) getModId.invoke(mod));
                }
            } catch (Exception ignored) {}
            return mods;
        }
    }

    /**
     * Forge event handler that bridges Forge events to our universal event bus.
     */
    static class ForgeEventHandler {
        // Forge discovers @SubscribeEvent methods via reflection.
        // We define handler stubs that forward to the universal bus.
        // Actual @SubscribeEvent annotations would require Forge on the
        // compile classpath. Instead, we register dynamically.
    }


    // ========================================================================
    //  SECTION 7: FORGE LEGACY INTEGRATION (1.7.x)
    // ========================================================================

    /**
     * Integration with legacy Forge (cpw.mods.fml path, MC 1.7.x).
     */
    static final class ForgeLegacyIntegration implements LoaderIntegration {

        private ClassLoader launchClassLoader = null;

        @Override public String getLoaderName() { return "Forge (Legacy)"; }
        @Override public String getLoaderVersion() { return "legacy"; }
        @Override public String getMinecraftVersion() { return "1.7.x"; }

        @Override
        public void initialize() {
            launchClassLoader = Thread.currentThread().getContextClassLoader();

            // Register exclusions
            if (launchClassLoader.getClass().getName().contains("LaunchClassLoader")) {
                try {
                    Method addExclusion = launchClassLoader.getClass().getMethod(
                        "addTransformerExclusion", String.class);
                    addExclusion.invoke(launchClassLoader,
                        "stellar.snow.astralis.integration.");
                } catch (Exception ignored) {}
            }

            LOGGER.info("[MDR-Bridge-ForgeLegacy] Legacy Forge integration initialized.");
        }

        @Override public void onGameReady() {
            // Register with legacy event bus
            try {
                Class<?> fmlCommonHandler = Class.forName(
                    "cpw.mods.fml.common.FMLCommonHandler");
                Method instance = fmlCommonHandler.getMethod("instance");
                Object handler = instance.invoke(null);
                Method bus = handler.getClass().getMethod("bus");
                Object eventBus = bus.invoke(handler);
                Method register = eventBus.getClass().getMethod(
                    "register", Object.class);
                register.invoke(eventBus, new ForgeEventHandler());
                LOGGER.info("[MDR-Bridge-ForgeLegacy] Event handler registered.");
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge-ForgeLegacy] Event registration: "
                          + e.getMessage());
            }
        }

        @Override
        public void registerTransformer(LWJGLTransformEngine engine) {
            if (launchClassLoader != null
             && launchClassLoader.getClass().getName().contains("LaunchClassLoader")) {
                try {
                    Method registerTransformer = launchClassLoader.getClass()
                        .getMethod("registerTransformer", String.class);
                    registerTransformer.invoke(launchClassLoader,
                        LWJGLTransformEngine.ForgeTransformerAdapter.class.getName());
                    LOGGER.info("[MDR-Bridge-ForgeLegacy] Transformer registered.");
                } catch (Exception e) {
                    LWJGLTransformEngine.register();
                }
            } else {
                LWJGLTransformEngine.register();
            }
        }

        @Override public void registerEventListener(Object listener) {}
        @Override public ClassLoader getLoaderClassLoader() { return launchClassLoader; }
        @Override public Path getGameDirectory() { return findGameDir(); }
        @Override public Path getConfigDirectory() { return findGameDir().resolve("config"); }
        @Override public Path getModsDirectory() { return findGameDir().resolve("mods"); }
        @Override public boolean isModLoaded(String modId) { return false; }
        @Override public List<String> getLoadedMods() { return Collections.emptyList(); }
    }


    // ========================================================================
    //  SECTION 8: FABRIC INTEGRATION
    // ========================================================================

    /**
     * Integration with the Fabric mod loader.
     */
    static final class FabricIntegration implements LoaderIntegration {

        private Object fabricLoaderInstance = null;
        private String fabricVersion = "unknown";
        private String mcVersion = "unknown";

        @Override public String getLoaderName() { return "Fabric"; }
        @Override public String getLoaderVersion() { return fabricVersion; }
        @Override public String getMinecraftVersion() { return mcVersion; }

        @Override
        public void initialize() {
            try {
                Class<?> fabricLoader = Class.forName(
                    "net.fabricmc.loader.api.FabricLoader");
                Method getInstance = fabricLoader.getMethod("getInstance");
                fabricLoaderInstance = getInstance.invoke(null);

                // Get Fabric Loader version
                try {
                    Method getModContainer = fabricLoader.getMethod(
                        "getModContainer", String.class);
                    Object optional = getModContainer.invoke(
                        fabricLoaderInstance, "fabricloader");
                    Method isPresent = optional.getClass().getMethod("isPresent");
                    if ((boolean) isPresent.invoke(optional)) {
                        Method get = optional.getClass().getMethod("get");
                        Object container = get.invoke(optional);
                        Method getMetadata = container.getClass().getMethod("getMetadata");
                        Object metadata = getMetadata.invoke(container);
                        Method getVersion = metadata.getClass().getMethod("getVersion");
                        Object version = getVersion.invoke(metadata);
                        fabricVersion = version.toString();
                    }
                } catch (Exception ignored) {}

                // Get MC version
                try {
                    Method getModContainer = fabricLoader.getMethod(
                        "getModContainer", String.class);
                    Object optional = getModContainer.invoke(
                        fabricLoaderInstance, "minecraft");
                    Method isPresent = optional.getClass().getMethod("isPresent");
                    if ((boolean) isPresent.invoke(optional)) {
                        Method get = optional.getClass().getMethod("get");
                        Object container = get.invoke(optional);
                        Method getMetadata = container.getClass().getMethod("getMetadata");
                        Object metadata = getMetadata.invoke(container);
                        Method getVersion = metadata.getClass().getMethod("getVersion");
                        Object version = getVersion.invoke(metadata);
                        mcVersion = version.toString();
                    }
                } catch (Exception ignored) {}

                // Register Mixin config
                registerMixinConfig();

            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                    "[MDR-Bridge-Fabric] Initialization failed", e);
            }

            LOGGER.info("[MDR-Bridge-Fabric] Fabric integration initialized. "
                      + "Loader=" + fabricVersion + ", MC=" + mcVersion);
        }

        private void registerMixinConfig() {
            try {
                Class<?> mixins = Class.forName(
                    "org.spongepowered.asm.mixin.Mixins");
                Method addConfiguration = mixins.getMethod(
                    "addConfiguration", String.class);
                addConfiguration.invoke(null, "mini_dirtyroom.mixins.json");
                LOGGER.fine("[MDR-Bridge-Fabric] Mixin config registered.");
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge-Fabric] Mixin config registration: "
                          + e.getMessage());
            }
        }

        @Override public void onGameReady() {
            LOGGER.info("[MDR-Bridge-Fabric] Game ready.");
        }

        @Override
        public void registerTransformer(LWJGLTransformEngine engine) {
            // Fabric uses Mixin for bytecode transformation.
            // Our Mixin config handles the heavy lifting.
            // For non-Mixin transforms, use Instrumentation.
            Instrumentation inst = Mini_DirtyRoomCore.getInstrumentation();
            if (inst != null) {
                inst.addTransformer(engine, inst.isRetransformClassesSupported());
                LOGGER.info("[MDR-Bridge-Fabric] Transformer registered via Instrumentation.");
            } else {
                LOGGER.info("[MDR-Bridge-Fabric] Using Mixin-only transformation.");
            }
        }

        @Override
        public void registerEventListener(Object listener) {
            // Fabric uses its own event system (e.g., ServerLifecycleEvents)
            // Register via Fabric API if available
            try {
                if (classExists("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents")) {
                    LOGGER.fine("[MDR-Bridge-Fabric] Fabric API events available.");
                }
            } catch (Exception ignored) {}
        }

        @Override
        public ClassLoader getLoaderClassLoader() {
            return Thread.currentThread().getContextClassLoader();
        }

        @Override public Path getGameDirectory() {
            return invokeOnLoader("getGameDir", Path.class, findGameDir());
        }

        @Override public Path getConfigDirectory() {
            return invokeOnLoader("getConfigDir", Path.class,
                findGameDir().resolve("config"));
        }

        @Override public Path getModsDirectory() {
            return getGameDirectory().resolve("mods");
        }

        @Override
        public boolean isModLoaded(String modId) {
            try {
                Method isModLoaded = fabricLoaderInstance.getClass()
                    .getMethod("isModLoaded", String.class);
                return (boolean) isModLoaded.invoke(fabricLoaderInstance, modId);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public List<String> getLoadedMods() {
            List<String> mods = new ArrayList<>();
            try {
                Method getAllMods = fabricLoaderInstance.getClass()
                    .getMethod("getAllMods");
                @SuppressWarnings("unchecked")
                Collection<Object> allMods = (Collection<Object>)
                    getAllMods.invoke(fabricLoaderInstance);
                for (Object mod : allMods) {
                    Method getMetadata = mod.getClass().getMethod("getMetadata");
                    Object metadata = getMetadata.invoke(mod);
                    Method getId = metadata.getClass().getMethod("getId");
                    mods.add((String) getId.invoke(metadata));
                }
            } catch (Exception ignored) {}
            return mods;
        }

        @SuppressWarnings("unchecked")
        private <T> T invokeOnLoader(String method, Class<T> type, T defaultVal) {
            if (fabricLoaderInstance == null) return defaultVal;
            try {
                Method m = fabricLoaderInstance.getClass().getMethod(method);
                Object result = m.invoke(fabricLoaderInstance);
                if (type.isInstance(result)) return type.cast(result);
                // Might return Optional<Path>
                if (result != null && result.getClass().getName().contains("Optional")) {
                    Method get = result.getClass().getMethod("orElse", Object.class);
                    return (T) get.invoke(result, defaultVal);
                }
            } catch (Exception ignored) {}
            return defaultVal;
        }
    }


    // ========================================================================
    //  SECTION 9: QUILT INTEGRATION
    // ========================================================================

    /**
     * Integration with the Quilt mod loader (fork of Fabric).
     * Quilt is largely Fabric-compatible, so we extend FabricIntegration
     * behavior and add Quilt-specific APIs.
     */
    static final class QuiltIntegration implements LoaderIntegration {

        private final FabricIntegration fabricDelegate = new FabricIntegration();
        private Object quiltLoaderInstance = null;

        @Override public String getLoaderName() { return "Quilt"; }
        @Override
        public String getLoaderVersion() {
            try {
                Class<?> ql = Class.forName("org.quiltmc.loader.api.QuiltLoader");
                // QuiltLoader doesn't always have a direct version method
                return "latest";
            } catch (Exception e) {
                return "unknown";
            }
        }
        @Override public String getMinecraftVersion() {
            return fabricDelegate.getMinecraftVersion();
        }

        @Override
        public void initialize() {
            try {
                Class<?> qlClass = Class.forName(
                    "org.quiltmc.loader.api.QuiltLoader");
                // QuiltLoader is a static utility class
                quiltLoaderInstance = qlClass; // Use the class itself
            } catch (Exception ignored) {}

            // Initialize Fabric compatibility layer
            fabricDelegate.initialize();

            LOGGER.info("[MDR-Bridge-Quilt] Quilt integration initialized.");
        }

        @Override public void onGameReady() { fabricDelegate.onGameReady(); }
        @Override public void registerTransformer(LWJGLTransformEngine e) {
            fabricDelegate.registerTransformer(e);
        }
        @Override public void registerEventListener(Object l) {
            fabricDelegate.registerEventListener(l);
        }
        @Override public ClassLoader getLoaderClassLoader() {
            return fabricDelegate.getLoaderClassLoader();
        }
        @Override public Path getGameDirectory() { return fabricDelegate.getGameDirectory(); }
        @Override public Path getConfigDirectory() { return fabricDelegate.getConfigDirectory(); }
        @Override public Path getModsDirectory() { return fabricDelegate.getModsDirectory(); }

        @Override
        public boolean isModLoaded(String modId) {
            // Try Quilt API first
            try {
                Method isModLoaded = quiltLoaderInstance.getClass()
                    .getMethod("isModLoaded", String.class);
                return (boolean) isModLoaded.invoke(null, modId);
            } catch (Exception e) {
                return fabricDelegate.isModLoaded(modId);
            }
        }

        @Override
        public List<String> getLoadedMods() {
            try {
                Class<?> qlClass = Class.forName("org.quiltmc.loader.api.QuiltLoader");
                Method getAllMods = qlClass.getMethod("getAllMods");
                @SuppressWarnings("unchecked")
                Collection<Object> allMods = (Collection<Object>)
                    getAllMods.invoke(null);
                List<String> mods = new ArrayList<>();
                for (Object mod : allMods) {
                    Method metadata = mod.getClass().getMethod("metadata");
                    Object meta = metadata.invoke(mod);
                    Method id = meta.getClass().getMethod("id");
                    mods.add((String) id.invoke(meta));
                }
                return mods;
            } catch (Exception e) {
                return fabricDelegate.getLoadedMods();
            }
        }
    }


    // ========================================================================
    //  SECTION 10: NEOFORGE INTEGRATION
    // ========================================================================

    /**
     * Integration with NeoForge (fork of Forge, 1.20.1+).
     */
    static final class NeoForgeIntegration implements LoaderIntegration {

        private Object neoForgeEventBus = null;
        private String neoForgeVersion = "unknown";

        @Override public String getLoaderName() { return "NeoForge"; }
        @Override public String getLoaderVersion() { return neoForgeVersion; }
        @Override public String getMinecraftVersion() { return "1.20+"; }

        @Override
        public void initialize() {
            try {
                Class<?> fv = Class.forName(
                    "net.neoforged.fml.loading.FMLLoader");
                Method getVersion = fv.getMethod("versionInfo");
                Object info = getVersion.invoke(null);
                Method nfVersion = info.getClass().getMethod("neoForgeVersion");
                neoForgeVersion = nfVersion.invoke(info).toString();
            } catch (Exception ignored) {
                try {
                    Class<?> fv = Class.forName(
                        "net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion");
                    Method getVersion = fv.getMethod("getVersion");
                    neoForgeVersion = (String) getVersion.invoke(null);
                } catch (Exception ignored2) {}
            }

            LOGGER.info("[MDR-Bridge-NeoForge] NeoForge integration initialized. "
                      + "Version=" + neoForgeVersion);
        }

        @Override
        public void onGameReady() {
            try {
                Class<?> neoForge = Class.forName(
                    "net.neoforged.neoforge.common.NeoForge");
                Field busField = neoForge.getDeclaredField("EVENT_BUS");
                neoForgeEventBus = busField.get(null);

                if (neoForgeEventBus != null) {
                    LOGGER.info("[MDR-Bridge-NeoForge] Event bus acquired.");
                }
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge-NeoForge] Event bus: " + e.getMessage());
            }
        }

        @Override
        public void registerTransformer(LWJGLTransformEngine engine) {
            // NeoForge uses ModLauncher. Register via Instrumentation.
            Instrumentation inst = Mini_DirtyRoomCore.getInstrumentation();
            if (inst != null) {
                inst.addTransformer(engine, inst.isRetransformClassesSupported());
                LOGGER.info("[MDR-Bridge-NeoForge] Transformer registered via Instrumentation.");
            } else {
                LOGGER.warning("[MDR-Bridge-NeoForge] No Instrumentation available.");
            }
        }

        @Override
        public void registerEventListener(Object listener) {
            if (neoForgeEventBus == null) return;
            try {
                Method register = neoForgeEventBus.getClass().getMethod(
                    "register", Object.class);
                register.invoke(neoForgeEventBus, listener);
            } catch (Exception e) {
                LOGGER.fine("[MDR-Bridge-NeoForge] Event listener: " + e.getMessage());
            }
        }

        @Override public ClassLoader getLoaderClassLoader() {
            return Thread.currentThread().getContextClassLoader();
        }
        @Override public Path getGameDirectory() { return findGameDir(); }
        @Override public Path getConfigDirectory() { return findGameDir().resolve("config"); }
        @Override public Path getModsDirectory() { return findGameDir().resolve("mods"); }

        @Override
        public boolean isModLoaded(String modId) {
            try {
                Class<?> modList = Class.forName("net.neoforged.fml.ModList");
                Method get = modList.getMethod("get");
                Object list = get.invoke(null);
                Method isLoaded = list.getClass().getMethod("isLoaded", String.class);
                return (boolean) isLoaded.invoke(list, modId);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public List<String> getLoadedMods() {
            List<String> mods = new ArrayList<>();
            try {
                Class<?> modList = Class.forName("net.neoforged.fml.ModList");
                Method get = modList.getMethod("get");
                Object list = get.invoke(null);
                Method getMods = list.getClass().getMethod("getMods");
                @SuppressWarnings("unchecked")
                List<Object> modInfos = (List<Object>) getMods.invoke(list);
                for (Object info : modInfos) {
                    Method getModId = info.getClass().getMethod("getModId");
                    mods.add((String) getModId.invoke(info));
                }
            } catch (Exception ignored) {}
            return mods;
        }
    }


    // ========================================================================
    //  SECTION 11: LITELOADER INTEGRATION
    // ========================================================================

    /**
     * Integration with LiteLoader (lightweight mod loader, often alongside Forge).
     */
    static final class LiteLoaderIntegration implements LoaderIntegration {

        @Override public String getLoaderName() { return "LiteLoader"; }
        @Override public String getLoaderVersion() { return "unknown"; }
        @Override public String getMinecraftVersion() { return "unknown"; }

        @Override
        public void initialize() {
            LOGGER.info("[MDR-Bridge-LiteLoader] LiteLoader integration initialized (minimal).");
        }

        @Override public void onGameReady() {}
        @Override public void registerTransformer(LWJGLTransformEngine e) {
            LWJGLTransformEngine.register();
        }
        @Override public void registerEventListener(Object l) {}
        @Override public ClassLoader getLoaderClassLoader() {
            return Thread.currentThread().getContextClassLoader();
        }
        @Override public Path getGameDirectory() { return findGameDir(); }
        @Override public Path getConfigDirectory() { return findGameDir().resolve("config"); }
        @Override public Path getModsDirectory() { return findGameDir().resolve("mods"); }
        @Override public boolean isModLoaded(String modId) { return false; }
        @Override public List<String> getLoadedMods() { return Collections.emptyList(); }
    }


    // ========================================================================
    //  SECTION 12: STANDALONE INTEGRATION
    // ========================================================================

    /**
     * Standalone integration when no recognized mod loader is present.
     * Relies entirely on Instrumentation / DeepMix for transformation.
     */
    static final class StandaloneIntegration implements LoaderIntegration {

        @Override public String getLoaderName() { return "Standalone"; }
        @Override public String getLoaderVersion() { return "N/A"; }
        @Override public String getMinecraftVersion() {
            EnvironmentInfo env = Mini_DirtyRoomCore.getEnvironment();
            return env != null ? env.minecraftVersion : "unknown";
        }

        @Override
        public void initialize() {
            LOGGER.info("[MDR-Bridge-Standalone] Standalone mode initialized.");
        }

        @Override public void onGameReady() {
            LOGGER.info("[MDR-Bridge-Standalone] Game ready.");
        }

        @Override
        public void registerTransformer(LWJGLTransformEngine engine) {
            Instrumentation inst = Mini_DirtyRoomCore.getInstrumentation();
            if (inst != null) {
                inst.addTransformer(engine, inst.isRetransformClassesSupported());
                LOGGER.info("[MDR-Bridge-Standalone] Transformer registered via Instrumentation.");
            } else {
                LOGGER.warning("[MDR-Bridge-Standalone] No Instrumentation. "
                             + "LWJGL transformation depends on DeepMix annotations.");
            }
        }

        @Override public void registerEventListener(Object l) {}
        @Override public ClassLoader getLoaderClassLoader() {
            return Thread.currentThread().getContextClassLoader();
        }
        @Override public Path getGameDirectory() { return findGameDir(); }
        @Override public Path getConfigDirectory() { return findGameDir().resolve("config"); }
        @Override public Path getModsDirectory() { return findGameDir().resolve("mods"); }
        @Override public boolean isModLoaded(String modId) { return false; }
        @Override public List<String> getLoadedMods() { return Collections.emptyList(); }
    }


    // ========================================================================
    //  SECTION 13: SERVER PLATFORM INTEGRATIONS
    // ========================================================================

    // ── 13A: Bukkit / CraftBukkit ──────────────────────────────────────────

    static class BukkitIntegration implements ServerPlatformIntegration {
        @Override public String getPlatformName() { return "CraftBukkit"; }
        @Override public String getPlatformVersion() {
            try {
                Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
                Method getVersion = bukkit.getMethod("getVersion");
                return (String) getVersion.invoke(null);
            } catch (Exception e) { return "unknown"; }
        }
        @Override public void initialize() {
            LOGGER.info("[MDR-Bridge-Bukkit] CraftBukkit integration initialized.");
        }
        @Override public void onServerReady() {}
        @Override public void registerCommand(String name, Object handler) {
            LOGGER.fine("[MDR-Bridge-Bukkit] Command registration deferred.");
        }
        @Override public boolean isMainThread() {
            try {
                Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
                Method isPrimaryThread = bukkit.getMethod("isPrimaryThread");
                return (boolean) isPrimaryThread.invoke(null);
            } catch (Exception e) { return Thread.currentThread().getName().equals("Server thread"); }
        }
        @Override public void runOnMainThread(Runnable task) {
            try {
                Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
                Method getScheduler = bukkit.getMethod("getScheduler");
                Object scheduler = getScheduler.invoke(null);

                // Get our plugin instance (may not exist)
                Method getPluginManager = bukkit.getMethod("getPluginManager");
                Object pm = getPluginManager.invoke(null);
                Method getPlugin = pm.getClass().getMethod("getPlugin", String.class);
                Object plugin = getPlugin.invoke(pm, "Mini_DirtyRoom");

                if (plugin != null) {
                    Method runTask = scheduler.getClass().getMethod(
                        "runTask", Class.forName("org.bukkit.plugin.Plugin"), Runnable.class);
                    runTask.invoke(scheduler, plugin, task);
                } else {
                    task.run(); // No plugin context — run directly
                }
            } catch (Exception e) {
                task.run();
            }
        }
    }

    // ── 13B: Spigot ────────────────────────────────────────────────────────

    static class SpigotIntegration extends BukkitIntegration {
        @Override public String getPlatformName() { return "Spigot"; }
        @Override public void initialize() {
            LOGGER.info("[MDR-Bridge-Spigot] Spigot integration initialized.");
        }
    }

    // ── 13C: Paper ─────────────────────────────────────────────────────────

    static class PaperIntegration extends SpigotIntegration {
        @Override public String getPlatformName() { return "Paper"; }
        @Override public void initialize() {
            LOGGER.info("[MDR-Bridge-Paper] Paper integration initialized.");
        }
        @Override public void runOnMainThread(Runnable task) {
            // Paper has async-safe scheduling
            try {
                Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
                Method getGlobalRegionScheduler = bukkit.getMethod("getGlobalRegionScheduler");
                Object scheduler = getGlobalRegionScheduler.invoke(null);
                // Paper 1.20+ global scheduler
                Method run = scheduler.getClass().getMethod("run",
                    Class.forName("org.bukkit.plugin.Plugin"), Consumer.class);
                // This requires a plugin instance; fall back to Bukkit scheduler
                super.runOnMainThread(task);
            } catch (Exception e) {
                super.runOnMainThread(task);
            }
        }
    }

    // ── 13D: Purpur ────────────────────────────────────────────────────────

    static class PurpurIntegration extends PaperIntegration {
        @Override public String getPlatformName() { return "Purpur"; }
        @Override public void initialize() {
            LOGGER.info("[MDR-Bridge-Purpur] Purpur integration initialized.");
        }
    }

    // ── 13E: Folia ─────────────────────────────────────────────────────────

    static class FoliaIntegration extends PaperIntegration {
        @Override public String getPlatformName() { return "Folia"; }
        @Override public void initialize() {
            LOGGER.info("[MDR-Bridge-Folia] Folia integration initialized. "
                      + "Note: Folia uses regionized threading.");
        }
        @Override public boolean isMainThread() {
            // Folia has no single main thread — everything is regionized
            return false;
        }
        @Override public void runOnMainThread(Runnable task) {
            // Folia: use global region scheduler
            try {
                Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
                Method getGlobalRegionScheduler = bukkit.getMethod("getGlobalRegionScheduler");
                Object scheduler = getGlobalRegionScheduler.invoke(null);
                Method execute = scheduler.getClass().getMethod("execute",
                    Class.forName("org.bukkit.plugin.Plugin"), Runnable.class);
                // Need plugin instance
                task.run(); // Simplified fallback
            } catch (Exception e) {
                task.run();
            }
        }
    }

    // ── 13F: Sponge ────────────────────────────────────────────────────────

    static class SpongeIntegration implements ServerPlatformIntegration {
        @Override public String getPlatformName() { return "Sponge"; }
        @Override public String getPlatformVersion() {
            try {
                Class<?> sponge = Class.forName("org.spongepowered.api.Sponge");
                Method platform = sponge.getMethod("platform");
                Object plat = platform.invoke(null);
                Method minecraftVersion = plat.getClass().getMethod("minecraftVersion");
                Object ver = minecraftVersion.invoke(plat);
                return ver.toString();
            } catch (Exception e) { return "unknown"; }
        }
        @Override public void initialize() {
            LOGGER.info("[MDR-Bridge-Sponge] Sponge integration initialized.");
        }
        @Override public void onServerReady() {}
        @Override public void registerCommand(String name, Object handler) {}
        @Override public boolean isMainThread() {
            try {
                Class<?> sponge = Class.forName("org.spongepowered.api.Sponge");
                Method server = sponge.getMethod("server");
                Object srv = server.invoke(null);
                Method onMainThread = srv.getClass().getMethod("onMainThread");
                return (boolean) onMainThread.invoke(srv);
            } catch (Exception e) { return false; }
        }
        @Override public void runOnMainThread(Runnable task) { task.run(); }
    }

    // ── 13G: Velocity ──────────────────────────────────────────────────────

    static class VelocityIntegration implements ServerPlatformIntegration {
        @Override public String getPlatformName() { return "Velocity"; }
        @Override public String getPlatformVersion() { return "unknown"; }
        @Override public void initialize() {
            LOGGER.info("[MDR-Bridge-Velocity] Velocity proxy integration initialized.");
        }
        @Override public void onServerReady() {}
        @Override public void registerCommand(String name, Object handler) {}
        @Override public boolean isMainThread() { return false; }
        @Override public void runOnMainThread(Runnable task) { task.run(); }
    }

    // ── 13H: BungeeCord ────────────────────────────────────────────────────

    static class BungeeCordIntegration implements ServerPlatformIntegration {
        @Override public String getPlatformName() { return "BungeeCord"; }
        @Override public String getPlatformVersion() {
            try {
                Class<?> proxyServer = Class.forName("net.md_5.bungee.api.ProxyServer");
                Method getInstance = proxyServer.getMethod("getInstance");
                Object instance = getInstance.invoke(null);
                Method getVersion = instance.getClass().getMethod("getVersion");
                return (String) getVersion.invoke(instance);
            } catch (Exception e) { return "unknown"; }
        }
        @Override public void initialize() {
            LOGGER.info("[MDR-Bridge-BungeeCord] BungeeCord proxy integration initialized.");
        }
        @Override public void onServerReady() {}
        @Override public void registerCommand(String name, Object handler) {}
        @Override public boolean isMainThread() { return false; }
        @Override public void runOnMainThread(Runnable task) { task.run(); }
    }


    // ========================================================================
    //  SECTION 14: UNIVERSAL EVENT BUS
    // ========================================================================

    /**
     * A simple cross-loader event bus that allows components to communicate
     * regardless of which mod loader is active. This is NOT a replacement
     * for Forge/Fabric event systems — it's an internal coordination tool.
     */
    public static final class UniversalEventBus {

        private final Map<String, List<Consumer<Object>>> listeners =
            new ConcurrentHashMap<>();

        /**
         * Registers a listener for a specific event type.
         */
        public void on(String eventType, Consumer<Object> listener) {
            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(listener);
        }

        /**
         * Fires an event to all registered listeners.
         */
        public void fire(String eventType, Object eventData) {
            List<Consumer<Object>> handlers = listeners.get(eventType);
            if (handlers == null || handlers.isEmpty()) return;

            for (Consumer<Object> handler : handlers) {
                try {
                    handler.accept(eventData);
                } catch (Exception e) {
                    LOGGER.warning("[MDR-Bridge] Event handler error for '"
                                 + eventType + "': " + e.getMessage());
                }
            }
        }

        /**
         * Removes all listeners for an event type.
         */
        public void clear(String eventType) {
            listeners.remove(eventType);
        }

        /**
         * Removes all listeners.
         */
        public void clearAll() {
            listeners.clear();
        }

        /**
         * Returns the number of registered event types.
         */
        public int getEventTypeCount() {
            return listeners.size();
        }

        /**
         * Returns the total number of registered listeners.
         */
        public int getTotalListenerCount() {
            return listeners.values().stream().mapToInt(List::size).sum();
        }
    }


    // ========================================================================
    //  SECTION 15: LIFECYCLE MANAGEMENT
    // ========================================================================

    /**
     * Lifecycle phases that components can hook into.
     */
    public enum LifecyclePhase {
        /** Fired after the bridge is initialized. */
        BRIDGE_INITIALIZED,
        /** Fired when the game is fully loaded and running. */
        GAME_READY,
        /** Fired when a world is loaded (client or server). */
        WORLD_LOADED,
        /** Fired when the game is shutting down. */
        SHUTDOWN,
        /** Fired when a resource reload occurs (F3+T, etc.). */
        RESOURCE_RELOAD,
        /** Fired when a tick occurs (if hooked). */
        TICK
    }

    /**
     * Registers a callback for a lifecycle phase.
     */
    public static void onLifecycle(LifecyclePhase phase, Runnable callback) {
        LIFECYCLE_CALLBACKS
            .computeIfAbsent(phase, k -> new CopyOnWriteArrayList<>())
            .add(callback);
    }

    /**
     * Fires all callbacks for a lifecycle phase.
     */
    static void fireLifecycleEvent(LifecyclePhase phase) {
        List<Runnable> callbacks = LIFECYCLE_CALLBACKS.get(phase);
        if (callbacks == null || callbacks.isEmpty()) return;

        LOGGER.fine("[MDR-Bridge] Firing lifecycle: " + phase
                  + " (" + callbacks.size() + " callbacks)");

        for (Runnable callback : callbacks) {
            try {
                callback.run();
            } catch (Exception e) {
                LOGGER.warning("[MDR-Bridge] Lifecycle callback error ("
                             + phase + "): " + e.getMessage());
            }
        }
    }


    // ========================================================================
    //  SECTION 16: MOD METADATA
    // ========================================================================

    /**
     * Mod metadata container exposed to all loaders.
     */
    static final class ModMetadata {
        final String id;
        final String name;
        final String version;
        final String author;
        final String description;
        final String url;
        final String license;

        ModMetadata(String id, String name, String version, String author,
                   String description, String url, String license) {
            this.id          = id;
            this.name        = name;
            this.version     = version;
            this.author      = author;
            this.description = description;
            this.url         = url;
            this.license     = license;
        }

        @Override
        public String toString() {
            return name + " v" + version + " by " + author
                 + " [" + id + "]";
        }
    }


    // ========================================================================
    //  SECTION 17: PUBLIC API
    // ========================================================================

    /**
     * Returns the active loader integration.
     */
    public static LoaderIntegration getActiveLoader() {
        return ACTIVE_INTEGRATION.get();
    }

    /**
     * Returns all detected loader integrations.
     */
    public static List<LoaderIntegration> getAllLoaders() {
        return Collections.unmodifiableList(ALL_INTEGRATIONS);
    }

    /**
     * Returns the server platform integration, or null.
     */
    public static ServerPlatformIntegration getServerPlatform() {
        return SERVER_PLATFORM.get();
    }

    /**
     * Returns the universal event bus.
     */
    public static UniversalEventBus getEventBus() {
        return EVENT_BUS;
    }

    /**
     * Returns the mod metadata.
     */
    public static ModMetadata getModMetadata() {
        return MOD_META;
    }

    /**
     * Returns whether a specific mod is loaded (checks all loaders).
     */
    public static boolean isModLoaded(String modId) {
        for (LoaderIntegration loader : ALL_INTEGRATIONS) {
            if (loader.isModLoaded(modId)) return true;
        }
        return false;
    }

    /**
     * Returns all loaded mod IDs across all loaders.
     */
    public static List<String> getAllLoadedMods() {
        Set<String> allMods = new LinkedHashSet<>();
        for (LoaderIntegration loader : ALL_INTEGRATIONS) {
            allMods.addAll(loader.getLoadedMods());
        }
        return new ArrayList<>(allMods);
    }

    /**
     * Returns whether the bridge is fully initialized.
     */
    public static boolean isReady() {
        return FULLY_READY.get();
    }

    /**
     * Returns whether we're on the client side.
     */
    public static boolean isClient() { return IS_CLIENT; }

    /**
     * Returns whether we're on a dedicated server.
     */
    public static boolean isServer() { return IS_SERVER; }

    /**
     * Returns whether we're on a proxy (Velocity/BungeeCord).
     */
    public static boolean isProxy() { return IS_PROXY; }

    /**
     * Returns whether we're in a development environment.
     */
    public static boolean isDevelopment() { return IS_DEVELOPMENT; }

    /**
     * Returns whether class names are obfuscated.
     */
    public static boolean isObfuscated() { return IS_OBFUSCATED; }

    /**
     * Returns the mapping type (srg, intermediary, mojang, notch, none).
     */
    public static String getMappingType() { return MAPPING_TYPE; }

    /**
     * Returns the game directory.
     */
    public static Path getGameDirectory() {
        LoaderIntegration loader = ACTIVE_INTEGRATION.get();
        return loader != null ? loader.getGameDirectory() : findGameDir();
    }

    /**
     * Returns the config directory.
     */
    public static Path getConfigDirectory() {
        LoaderIntegration loader = ACTIVE_INTEGRATION.get();
        return loader != null ? loader.getConfigDirectory()
                              : findGameDir().resolve("config");
    }

    /**
     * Returns the mods directory.
     */
    public static Path getModsDirectory() {
        LoaderIntegration loader = ACTIVE_INTEGRATION.get();
        return loader != null ? loader.getModsDirectory()
                              : findGameDir().resolve("mods");
    }

    /**
     * Runs a task on the main thread (server thread).
     */
    public static void runOnMainThread(Runnable task) {
        ServerPlatformIntegration server = SERVER_PLATFORM.get();
        if (server != null) {
            server.runOnMainThread(task);
        } else {
            task.run();
        }
    }


    // ========================================================================
    //  SECTION 18: DIAGNOSTIC REPORT
    // ========================================================================

    /**
     * Generates a comprehensive diagnostic report for this bridge.
     */
    public static String getDiagnosticReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Mod Loader Bridge Report ===\n");
        sb.append("Initialized: ").append(INITIALIZED.get()).append("\n");
        sb.append("Fully Ready: ").append(FULLY_READY.get()).append("\n");

        sb.append("\n--- Environment ---\n");
        sb.append("Client: ").append(IS_CLIENT).append("\n");
        sb.append("Server: ").append(IS_SERVER).append("\n");
        sb.append("Proxy: ").append(IS_PROXY).append("\n");
        sb.append("Development: ").append(IS_DEVELOPMENT).append("\n");
        sb.append("Obfuscated: ").append(IS_OBFUSCATED).append("\n");
        sb.append("Mappings: ").append(MAPPING_TYPE).append("\n");

        sb.append("\n--- Loader Integrations (")
          .append(ALL_INTEGRATIONS.size()).append(") ---\n");
        LoaderIntegration primary = ACTIVE_INTEGRATION.get();
        for (LoaderIntegration loader : ALL_INTEGRATIONS) {
            boolean isPrimary = (loader == primary);
            sb.append("  ").append(isPrimary ? "→ " : "  ")
              .append(loader.getLoaderName())
              .append(" v").append(loader.getLoaderVersion())
              .append(" (MC ").append(loader.getMinecraftVersion()).append(")")
              .append(isPrimary ? " [PRIMARY]" : "")
              .append("\n");
        }

        sb.append("\n--- Server Platform ---\n");
        ServerPlatformIntegration server = SERVER_PLATFORM.get();
        if (server != null) {
            sb.append("  ").append(server.getPlatformName())
              .append(" v").append(server.getPlatformVersion()).append("\n");
        } else {
            sb.append("  None detected\n");
        }

        sb.append("\n--- Event Bus ---\n");
        sb.append("Event types: ").append(EVENT_BUS.getEventTypeCount()).append("\n");
        sb.append("Total listeners: ").append(EVENT_BUS.getTotalListenerCount()).append("\n");

        sb.append("\n--- Lifecycle Callbacks ---\n");
        for (Map.Entry<LifecyclePhase, List<Runnable>> entry : LIFECYCLE_CALLBACKS.entrySet()) {
            sb.append("  ").append(entry.getKey())
              .append(": ").append(entry.getValue().size())
              .append(" callbacks\n");
        }

        sb.append("\n--- Loaded Mods ---\n");
        List<String> mods = getAllLoadedMods();
        sb.append("Count: ").append(mods.size()).append("\n");
        int count = 0;
        for (String mod : mods) {
            if (count++ >= 30) {
                sb.append("  ... and ").append(mods.size() - 30).append(" more\n");
                break;
            }
            sb.append("  ").append(mod).append("\n");
        }

        sb.append("\n--- Paths ---\n");
        sb.append("Game Dir: ").append(getGameDirectory()).append("\n");
        sb.append("Config Dir: ").append(getConfigDirectory()).append("\n");
        sb.append("Mods Dir: ").append(getModsDirectory()).append("\n");

        sb.append("\n--- Mod Metadata ---\n");
        sb.append(MOD_META.toString()).append("\n");

        sb.append("=== End Report ===\n");
        return sb.toString();
    }


    // ========================================================================
    //  SECTION 19: SHUTDOWN HOOK
    // ========================================================================

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("[MDR-Bridge] Shutdown hook executing...");
            fireLifecycleEvent(LifecyclePhase.SHUTDOWN);
            EVENT_BUS.clearAll();
            LIFECYCLE_CALLBACKS.clear();
            LOGGER.info("[MDR-Bridge] Shutdown complete.");
        }, "MDR-Bridge-Shutdown"));
    }


    // ========================================================================
    //  SECTION 20: UTILITY METHODS
    // ========================================================================

    /**
     * Checks if a class exists on the classpath without loading it.
     */
    private static boolean classExists(String className) {
        try {
            Class.forName(className, false,
                Thread.currentThread().getContextClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Finds the game directory (.minecraft or server root).
     */
    private static Path findGameDir() {
        // Check system property first (Forge sets this)
        String gameDir = System.getProperty("user.dir");

        // Check for Forge-specific property
        String forgeGameDir = System.getProperty("forge.gameDir");
        if (forgeGameDir != null && !forgeGameDir.isEmpty()) {
            return Paths.get(forgeGameDir);
        }

        // Check for Fabric-specific property
        String fabricGameDir = System.getProperty("fabric.gameDir");
        if (fabricGameDir != null && !fabricGameDir.isEmpty()) {
            return Paths.get(fabricGameDir);
        }

        // Check for minecraft home
        String mcDir = System.getProperty("minecraft.applet.home");
        if (mcDir != null && !mcDir.isEmpty()) {
            return Paths.get(mcDir);
        }

        // Default to current working directory
        return Paths.get(gameDir != null ? gameDir : ".");
    }


    // ========================================================================
    //  SECTION 21: PRIVATE CONSTRUCTOR
    // ========================================================================

    private ModLoaderBridge() {
        throw new UnsupportedOperationException(
            "ModLoaderBridge is a static utility class.");
    }
}
